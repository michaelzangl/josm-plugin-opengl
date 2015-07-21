package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.ArrayList;
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
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.OsmPrimitiveRecorder;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.OsmPrimitiveRecorder.RecordedPrimitiveReceiver;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordedOsmGeometries;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.StyleCache.StyleList;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;

public class OpenGLStyledMapRenderer extends StyledMapRenderer {

	// private RecordingStyledMapRenderer recordingRenderer;
	// private OsmPrimitiveRecorder recorder;

	public OpenGLStyledMapRenderer(GLGraphics2D g, NavigatableComponent nc,
			boolean isInactiveMode, PrimitiveManager cache) {
		super(g, nc, isInactiveMode);
		// recorder = new OsmPrimitiveRecorder();
		// recordingRenderer = new RecordingStyledMapRenderer(
		// recorder, nc,
		// isInactiveMode);
	}

	/**
	 * This class computes the styles for a given array of elements.
	 * 
	 * @author michael
	 *
	 * @param <T>
	 */
	private class StyledGeometryGenerator<T extends OsmPrimitive> implements
			Runnable {
		private final ElemStyles styles = MapPaintStyles.getStyles();
		private Iterable<T> primitives;

		public StyledGeometryGenerator(Iterable<T> primitives) {
			this.primitives = primitives;
		}

		@Override
		public void run() {
			MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
			try {
				for (T p : primitives) {
					if (p.isDrawable()) {
						StyleList sl = styles.get(p, getCircum(), nc);

						// TODO: parallel recordingRenderer
						for (ElemStyle s : sl) {
							new StyleGenerationRequest<T>(s, p).run();
						}
					}
				}
			} finally {
				MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
			}
		}
	}

	public class StyleMap<T extends OsmPrimitive> {
		private Hashtable<T, List<RecordedOsmGeometries<T>>> recordedStyles = new Hashtable<>();

		public void add(T primitive, RecordedOsmGeometries<T> geometry) {
			List<RecordedOsmGeometries<T>> list = recordedStyles.get(primitive);
			if (list == null) {
				list = new ArrayList<>();
				recordedStyles.put(primitive, list);
			}
			list.add(geometry);
		}

		public ArrayList<RecordedOsmGeometries<T>> getOrderedStyles() {
			ArrayList<RecordedOsmGeometries<T>> list = new ArrayList<>();
			for (List<RecordedOsmGeometries<T>> v : recordedStyles.values()) {
				list.addAll(v);
			}
			// TODO Collections.sort(list);
			return list;
		}
	}

	// TEMP
	private StyleMap<Node> nodeStyleMap = new StyleMap<>();
	private StyleMap<Way> wayStyleMap = new StyleMap<>();
	private boolean renderVirtualNodes;

	public class StyleGenerationRequest<T extends OsmPrimitive> implements
			Runnable, RecordedPrimitiveReceiver {

		private ElemStyle s;
		private T primitive;
		private int flags = 0; // <- TODO

		public StyleGenerationRequest(ElemStyle s, T primitive) {
			this.s = s;
			this.primitive = primitive;
		}

		@Override
		public void run() {
			OsmPrimitiveRecorder recorder = new OsmPrimitiveRecorder(this);
			RecordingStyledMapRenderer recordingRenderer = new RecordingStyledMapRenderer(
					recorder, nc, isInactiveMode);
			recordingRenderer.getSettings(renderVirtualNodes);
			recordingRenderer.setClipBounds(g.getClipBounds());
			recorder.start(primitive);
			s.paintPrimitive(primitive, paintSettings, recordingRenderer,
					(flags & FLAG_SELECTED) != 0,
					(flags & FLAG_OUTERMEMBER_OF_SELECTED) != 0,
					(flags & FLAG_MEMBER_OF_SELECTED) != 0);
			recorder.end();
		}

		@Override
		public void receiveForNode(RecordedOsmGeometries<Node> geometry) {
			nodeStyleMap.add((Node) primitive, geometry);
		}

		@Override
		public void receiveForWay(RecordedOsmGeometries<Way> geometry) {
			wayStyleMap.add((Way) primitive, geometry);

		}

		@Override
		public void receiveForRelation(RecordedOsmGeometries<Relation> geometry) {
			// TODO Auto-generated method stub

		}

	}

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
			new ElementSearcher<Node>(new StyleGeometryScheduler<Node>()) {
				@Override
				public List<Node> searchElements() {
					return data.searchNodes(bbox);
				}
			}.run();
			new ElementSearcher<Way>(new StyleGeometryScheduler<Way>()) {
				@Override
				public List<Way> searchElements() {
					return data.searchWays(bbox);
				}
			}.run();
		}
	}
	
	/**
	 * Schedules the generation of a bunch of styles/geometries.
	 * @author michael
	 *
	 * @param <T>
	 */
	private class StyleGeometryScheduler<T extends OsmPrimitive> implements OsmPrimitiveHandler<T> {
		public void receivePrimitives(java.util.List<T> primitives) {
			// We could now split this into bulks.
			//TODO: Use different threads.
			new StyledGeometryGenerator<>(primitives).run();
		}
	}
	
	private interface OsmPrimitiveHandler<T extends OsmPrimitive> {
		public void receivePrimitives(List<T> primitives);
	}

	private abstract class ElementSearcher<T extends OsmPrimitive> {
		private OsmPrimitiveHandler<T> handler;

		public ElementSearcher(OsmPrimitiveHandler<T> handler) {
			this.handler = handler;
		}

		public abstract List<T> searchElements();

		public void run() {
			List<T> primitives = searchElements();
			handler.receivePrimitives(primitives);
		}
	}

	@Override
	public void render(final DataSet data, boolean renderVirtualNodes,
			Bounds bounds) {
		this.renderVirtualNodes = renderVirtualNodes;
		BBox bbox = bounds.toBBox();
		getSettings(renderVirtualNodes);
		data.getReadLock().lock();
		try {

			StyleWorkQueue styleWorkQueue = new StyleWorkQueue(data);
			styleWorkQueue.setArea(bbox);

			for (RecordedOsmGeometries<Node> s : nodeStyleMap
					.getOrderedStyles()) {
				System.out.println("Draw node " + s);
				s.draw(((GLGraphics2D) g).getGLContext().getGL().getGL2());
			}
			for (RecordedOsmGeometries<Way> s : wayStyleMap
					.getOrderedStyles()) {
				System.out.println("Draw way " + s);
				s.draw(((GLGraphics2D) g).getGLContext().getGL().getGL2());
			}

			drawVirtualNodes(data, bbox);

		} finally {
			data.getReadLock().unlock();
		}
	}

}
