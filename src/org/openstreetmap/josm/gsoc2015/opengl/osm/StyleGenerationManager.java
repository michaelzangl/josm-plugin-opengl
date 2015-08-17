package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordedOsmGeometries;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OpenGLStyledMapRenderer.StyleGenerationState;
import org.openstreetmap.josm.gsoc2015.opengl.osm.search.NodeSearcher;
import org.openstreetmap.josm.gsoc2015.opengl.osm.search.OsmPrimitiveHandler;
import org.openstreetmap.josm.gsoc2015.opengl.osm.search.RelationSearcher;
import org.openstreetmap.josm.gsoc2015.opengl.osm.search.WaySearcher;

/**
 * This class manages a bunch of threads that search primitives, generate styles
 * and return the results.
 * 
 * <p>
 * There are three phases of style generation:
 * <ol>
 * <li>Search the nodes/ways/relations to generate styles for
 * <li>Generate the styles for all primitives found
 * <li>Generate the OpenGL geometries
 * </ol>
 * <p>
 * A cache is used to store already computed geometries. Each cache entry can
 * cache geometrires for multiple primitives, but each primitive can only have
 * at most one cache entry.
 * <p>
 * To allow for parallel geometry (re)generation and drawing of already
 * generated geometries, this generator allows for processing bulks of data.
 * 
 * <h2>Draw process</h2>
 * <p>
 * All entries in the cache are marked as unused.
 * <p>
 * Whenever a new frame is to be rendered, this manager schedules a search of
 * the nodes/ways/relations in the bbox that needs to be drawn.
 * <p>
 * We go through the list of found elements and request the element from the
 * cache. There are multiple cases here:
 * <ol>
 * <li>The cache can provide an up to date entry for the element.
 * <ol>
 * <li>That entry has not yet been scheduled for drawing: Mark it to be drawn
 * <li>That entry has been scheduled for drawing: We are done, no more work.
 * </ol>
 * <li>The cache can provide an outdated entry for that element.
 * <ol>
 * <li>Mark the element to be re-calculated and add it to be drawn as in case 1.
 * </ol>
 * <li>The cache does not contain an entry. We mark the geometry to be created
 * in a new way.
 * </ol>
 * 
 * All entries marked for drawing are then displayed. As soon as new geometries
 * are generated, a redraw is scheduled (TODO: delayed).
 * <p>
 * TODO: Wait a bit here.
 * 
 * 
 * <h2>Cache cleaning</h2>
 * <p>
 * We need to force a clean from the cache:
 * <ul>
 * <li>Nodes (and related ways/relations) of which the geometry changed
 * <li>Ways of which the node list was changed
 * <li>Relations when children were changed.
 * </ul>
 * We should clean:
 * <ul>
 * <li>Primitives with tag changes
 * </ul>
 * 
 * @author Michael Zangl
 *
 */
public class StyleGenerationManager {
	private final DataSet data;

	private ExecutorService geometryGenerationThreads = Executors
			.newFixedThreadPool(4);

	private DrawThreadPool drawThreadPool = new DrawThreadPool();

	private final StyleGeometryCache cache = new StyleGeometryCache();
	
	public StyleGenerationManager(DataSet data) {
		this.data = data;
		cache.addListeners(data);
	}

	/**
	 * This is a thread pool for drawing. It handles multiple threads and allows
	 * you to await the termination of all tasks scheduled on the threads.
	 * 
	 * @author michael
	 *
	 */
	private static class DrawThreadPool {
		private final ExecutorService drawThreads = Executors.newFixedThreadPool(4);
		// private final ExecutorService drawThreads = Executors.newFixedThreadPool(1);
		private final LinkedList<Future<?>> futuresToAwait = new LinkedList<>();

		public void scheduleTask(Runnable r) {
			futuresToAwait.add(drawThreads.submit(r));
		}

		/**
		 * Awaits the termination of all scheduled tasks.
		 */
		public void finish() {
			Future<?> f;
			while ((f = futuresToAwait.pollFirst()) != null) {
				try {
					f.get();
				} catch (InterruptedException | ExecutionException e) {
					// TODO: Error handling
					e.printStackTrace();
				}
			}
		}

		/**
		 * Stops this thread pool.
		 */
		public void stop() {
			drawThreads.shutdown();
		}
	}

	private class PrimitiveForDrawSearcher<T extends OsmPrimitive> implements
			OsmPrimitiveHandler<T> {
		//TODO: Adaptive for nodes/rest
		private static final int BULK_SIZE = 5000;
		private final StyleGenerationState sgs;
		private final StyleGeometryCache cache;

		public PrimitiveForDrawSearcher(StyleGenerationState sgs,
				StyleGeometryCache cache) {
			this.sgs = sgs;
			this.cache = cache;
		}

		@Override
		public void receivePrimitives(List<T> primitives) {
			for (int i = 0; i < primitives.size(); i += BULK_SIZE) {
				drawThreadPool.scheduleTask(new QueryCachePrimitive<T>(
						primitives, i, Math.min(primitives.size(), i + BULK_SIZE), sgs,
						cache));
			}
		}
	}

	/**
	 * Gets a list of geometries to draw the current frame.
	 * 
	 * @param bbox
	 *            The bbox.
	 * @return
	 */
	public List<RecordedOsmGeometries> getDrawGeometries(BBox bbox,
			StyleGenerationState sgs) {
		long time1 = System.currentTimeMillis();
		cache.startFrame();
		drawThreadPool.scheduleTask(new NodeSearcher(
				new PrimitiveForDrawSearcher<Node>(sgs, cache), data, bbox));
		drawThreadPool.scheduleTask(new WaySearcher(
				new PrimitiveForDrawSearcher<Way>(sgs, cache), data, bbox));
		drawThreadPool.scheduleTask(new RelationSearcher(
				new PrimitiveForDrawSearcher<Relation>(sgs, cache), data,
				bbox));
		drawThreadPool.finish();
		List<RecordedOsmGeometries> recorded = cache.endFrame();
		long time2 = System.currentTimeMillis();
		Collections.sort(recorded);
		System.out.println("STYLE GEN in getDrawGeometries()" + (time2 - time1));
		System.out.println("SORTING in getDrawGeometries(): " + (System.currentTimeMillis() - time2));
		return recorded;
	}

	public boolean usesDataSet(DataSet data2) {
		return data == data2;
	}

	/**
	 * Stops all threads allocated and deallocates the whole cache.
	 */
	public void dispose() {
		cache.removeListeners(data);
		drawThreadPool.scheduleTask(new Runnable() {
			@Override
			public void run() {
				cache.dispose();
			}
		});
		drawThreadPool.stop();
	}
}
