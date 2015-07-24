package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.jogamp.glg2d.GLGraphics2D;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.DirectDrawRecorder;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.OsmPrimitiveRecorder;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.OsmPrimitiveRecorder.RecordedPrimitiveReceiver;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordedOsmGeometries;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.NodeElemStyle;
import org.openstreetmap.josm.gui.mappaint.StyleCache.StyleList;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
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

	public class StyleGenerationRequest<T extends OsmPrimitive> implements
			Runnable, RecordedPrimitiveReceiver {

		private ElemStyle s;
		private T primitive;
		
	    /**
	     * A primitive with {@link OsmPrimitive#isDisabled()}
	     * <p>
	     * This is the sign bit. So primitives with this set appear on the bottom.
	     */
	    private static final long FLAG_DISABLED = (1 << 63);
	    
	    private static final long STATE_NORMAL = 0;
	    /**
	     * A primitive with {@link OsmPrimitive#isMemberOfSelected()}
	     */
	    private static final long STATE_MEMBER_OF_SELECTED = (1 << 25);
	    /**
	     * A primitive with {@link OsmPrimitive#isSelected()}
	     */
	    private static final long STATE_SELECTED = (2 << 25);
	    /**
	     * A primitive with {@link OsmPrimitive#isOuterMemberOfSelected()}
	     */
	    private static final long STATE_OUTERMEMBER_OF_SELECTED = (3 << 25);

		public StyleGenerationRequest(ElemStyle s, T primitive) {
			this.s = s;
			this.primitive = primitive;
		}

		@Override
		public void run() {
			OsmPrimitiveRecorder recorder = new OsmPrimitiveRecorder(this);
			// For testing:
			// DirectDrawRecorder rec = new DirectDrawRecorder(((GLGraphics2D)g).getGLContext().getGL().getGL2());
			RecordingStyledMapRenderer recordingRenderer = new RecordingStyledMapRenderer(
					recorder, nc, isInactiveMode);
			recordingRenderer.getSettings(renderVirtualNodes);
			recordingRenderer.setClipBounds(g.getClipBounds());
			long state = getState();
			recorder.start(primitive, getOrderIndex(state));
			s.paintPrimitive(primitive, MapPaintSettings.INSTANCE, recordingRenderer,
					state == STATE_SELECTED,
					state == STATE_OUTERMEMBER_OF_SELECTED,
					state == STATE_MEMBER_OF_SELECTED);
			recorder.end();
		}

		private long getState() {
			if (primitive.isSelected()) {
				return STATE_SELECTED;
			} else if (!(primitive instanceof Node)
					&& primitive.isOuterMemberOfSelected()) {
				return STATE_OUTERMEMBER_OF_SELECTED;
			} else if (primitive.isMemberOfSelected()) {
				return STATE_MEMBER_OF_SELECTED;
			} else {
				return STATE_NORMAL;
			}
		}

		/**
		 * Converts a float to a normal integer so that the order stays the same.
		 * @param number
		 * @param totalBits Total number of bits. 1 sign bit, then the bits before the decimal point, then those after.
		 * @param afterSignBits Number of fixed bits after the decimal point.
		 * @return The float converted to an integer.
		 */
		private long floatToFixed(double number, int totalBits,
				int afterSignBits) {
			long value = (long) (number * (1l << afterSignBits));
			long highestBitMask = 1l << (totalBits - 1);
			long valueMask = (highestBitMask - 1);
			long signBit = number < 0 ? 0 : highestBitMask;
			return signBit | (value & valueMask);
		}

		/**
		 * Compute the order index.
		 * <ul>
		 * <li>Bit 63: always 0 (sign bit).
		 * <li>Bit 62: FLAG_DISABLED.
		 * <li>Bit 32 - 55: Major z index
		 * <li>Bit 25 - 28: flags
		 * <li>Bit 1 - 24: minor Z index
		 * <li>Bit 0: SIMPLE_NODE_ELEMSTYLE
		 * </ul>
		 * 
		 * @return
		 */
		private long getOrderIndex(long state) {
			int MAJOR_SHIFT = 32;
			int MINOR_SHIFT = 1;
			long orderIndex = 0;
			if (primitive.isDisabled()) {
				orderIndex |= FLAG_DISABLED;
	        }
			
			orderIndex |= floatToFixed(s.majorZIndex, 24, 8) << MAJOR_SHIFT;
			orderIndex |= state;
			orderIndex |= floatToFixed(s.zIndex, 24, 8) << MINOR_SHIFT;
			orderIndex |= s == NodeElemStyle.SIMPLE_NODE_ELEMSTYLE ? 1 : 0;
			
			System.out.println("0major: " + s.majorZIndex + ", minor: " + s.zIndex + ", order " + Long.toHexString(orderIndex));
			return orderIndex;
		}

		@Override
		public void receiveForNode(RecordedOsmGeometries geometry) {
			styleMap.add((Node) primitive, geometry);
		}

		@Override
		public void receiveForWay(RecordedOsmGeometries geometry) {
			styleMap.add((Way) primitive, geometry);

		}

		@Override
		public void receiveForRelation(RecordedOsmGeometries geometry) {
			styleMap.add((Relation) primitive, geometry);
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
	 * 
	 * @author michael
	 *
	 * @param <T>
	 */
	private class StyleGeometryScheduler<T extends OsmPrimitive> implements
			OsmPrimitiveHandler<T> {
		public void receivePrimitives(java.util.List<T> primitives) {
			// We could now split this into bulks.
			// TODO: Use different threads.
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

		makeStyleZIndexUniform(MapPaintStyles.getStyles());

		this.renderVirtualNodes = renderVirtualNodes;
		BBox bbox = bounds.toBBox();
		getSettings(renderVirtualNodes);
		data.getReadLock().lock();
		try {

			StyleWorkQueue styleWorkQueue = new StyleWorkQueue(data);
			styleWorkQueue.setArea(bbox);

			for (RecordedOsmGeometries s : styleMap.getOrderedStyles()) {
				System.out.println("Draw something: " + s);
				s.draw(((GLGraphics2D) g).getGLContext().getGL().getGL2());
			}

			drawVirtualNodes(data, bbox);

		} finally {
			data.getReadLock().unlock();
		}
	}

	private void makeStyleZIndexUniform(ElemStyles styles) {
		for (StyleSource s : styles.getStyleSources()) {
			if (s instanceof MapCSSStyleSource) {
				MapCSSStyleSource source = (MapCSSStyleSource) s;
				// TODO: We might analyze the z index here.
			} else {
				Main.warn("Cannot ahndle major/minor zindex of " + s
						+ ": unknown class.");
			}
		}
	}

}
