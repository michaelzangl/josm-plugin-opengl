package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.jogamp.glg2d.GLGraphics2D;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordedOsmGeometries;
import org.openstreetmap.josm.gsoc2015.opengl.osm.StyledGeometryGenerator.ChacheDataSupplier;
import org.openstreetmap.josm.gsoc2015.opengl.osm.search.NodeElementSearcher;
import org.openstreetmap.josm.gsoc2015.opengl.osm.search.OsmPrimitiveHandler;
import org.openstreetmap.josm.gsoc2015.opengl.osm.search.RelationElementSearcher;
import org.openstreetmap.josm.gsoc2015.opengl.osm.search.WayElementSearcher;
import org.openstreetmap.josm.gui.NavigatableComponent;

public class OpenGLStyledMapRenderer extends StyledMapRenderer {

	public OpenGLStyledMapRenderer(GLGraphics2D g, NavigatableComponent nc,
			boolean isInactiveMode) {
		super(g, nc, isInactiveMode);
	}

	public class StyleMap {
		private Hashtable<OsmPrimitive, List<RecordedOsmGeometries>> recordedStyles = new Hashtable<>();

		public synchronized <T extends OsmPrimitive> void add(T primitive,
				RecordedOsmGeometries geometry) {
			// For testing
			// geometry.draw(((GLGraphics2D)g).getGLContext().getGL().getGL2());

			List<RecordedOsmGeometries> list = recordedStyles.get(primitive);
			if (list == null) {
				list = new ArrayList<>();
				recordedStyles.put(primitive, list);
			}
			list.add((RecordedOsmGeometries) geometry);
		}

		public synchronized ArrayList<RecordedOsmGeometries> getOrderedStyles() {
			ArrayList<RecordedOsmGeometries> list = new ArrayList<>();
			for (List<RecordedOsmGeometries> v : recordedStyles.values()) {
				list.addAll(v);
			}
			Collections.sort(list);
			return list;
		}
	}

	// TEMP
	private StyleMap styleMap = new StyleMap();
	private boolean renderVirtualNodes;

	/**
	 * This queue recieves an area to process.
	 * 
	 * @author michael
	 *
	 */
	private class StyleWorkQueue {

		int activeWorkCycle = 0;

		DataSet data;

		public StyleWorkQueue(DataSet data) {
			this.data = data;
		}

		public void setArea(BBox bbox) {

			activeWorkCycle++;
			addArea(bbox);
		}

		public void addArea(final BBox bbox) {

			StyleGenerationState sgs = new StyleGenerationState();
			new NodeElementSearcher(new StyleGeometryScheduler<Node>(sgs),
					data, bbox).run();
			new WayElementSearcher(new StyleGeometryScheduler<Way>(sgs), data,
					bbox).run();
			new RelationElementSearcher(new StyleGeometryScheduler<Relation>(
					sgs), data, bbox).run();
		}
	}

	/**
	 * TODO: Static, unchangebale.
	 * @author michael
	 *
	 */
	public class StyleGenerationState implements ChacheDataSupplier {
		@Override
		public NavigatableComponent getCacheKey() {
			return nc;
		}

		public StyleMap getStyleReceiver() {
			return styleMap;
		}

		public Rectangle getClipBounds() {
			return g.getClipBounds();
		}

		public boolean renderVirtualNodes() {
			return renderVirtualNodes;
		}

		public boolean isInactiveMode() {
			return isInactiveMode;
		}

		@Override
		public double getCircum() {
			return OpenGLStyledMapRenderer.this.getCircum();
		}
	}

	/**
	 * Schedules the generation of a bunch of styles/geometries.
	 * 
	 * @author michael
	 *
	 * @param <T>
	 */
	private static class StyleGeometryScheduler<T extends OsmPrimitive>
			implements OsmPrimitiveHandler<T> {
		private StyleGenerationState sgs;

		public StyleGeometryScheduler(StyleGenerationState sgs) {
			this.sgs = sgs;
		}

		public void receivePrimitives(java.util.List<T> primitives) {
			// We could now split this into bulks.
			// TODO: Use different threads.
			//new StyledGeometryGenerator<>(primitives, sgs).run();
		}
	}

	private StyleGenerationManager manager = null;
	
	@Override
	public synchronized void render(final DataSet data, boolean renderVirtualNodes,
			Bounds bounds) {
		if (manager == null) {
			manager = new StyleGenerationManager(data);
			//TODO: check manager data set.
		}

		this.renderVirtualNodes = renderVirtualNodes;
		BBox bbox = bounds.toBBox();
		getSettings(renderVirtualNodes);
		data.getReadLock().lock();
		try {
			long time1 = System.currentTimeMillis();
//			StyleWorkQueue styleWorkQueue = new StyleWorkQueue(data);
//			styleWorkQueue.setArea(bbox);
			List<RecordedOsmGeometries> geometries = manager.getDrawGeometries(bbox, new StyleGenerationState());
			long time2 = System.currentTimeMillis();

			for (RecordedOsmGeometries s : geometries) {
				s.draw(((GLGraphics2D) g).getGLContext().getGL().getGL2());
			}
			long time3 = System.currentTimeMillis();

			drawVirtualNodes(data, bbox);
			long time4 = System.currentTimeMillis();
			System.out.println("Create styles: " + (time2 - time1) + ", draw: "
					+ (time3 - time2) + ", draw virtual: " + (time4 - time3));

		} finally {
			data.getReadLock().unlock();
		}
	}

}
