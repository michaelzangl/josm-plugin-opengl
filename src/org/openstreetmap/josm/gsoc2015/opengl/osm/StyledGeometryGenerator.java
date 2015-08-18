package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.OsmPrimitiveRecorder;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordedOsmGeometries;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OpenGLStyledMapRenderer.StyleGenerationState;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.AreaElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.NodeElemStyle;
import org.openstreetmap.josm.gui.mappaint.StyleCache.StyleList;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;

/**
 * This class computes the styles for a given array of elements. This class only
 * allows one thread to send requests.
 * 
 * @author michael
 *
 * @param <T>
 */
public class StyledGeometryGenerator implements Visitor {
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

	/**
	 * A class implementing this interface provides all state data to access the
	 * MapCSS style cache.
	 * 
	 * @author Michael Zangl
	 */
	interface ChacheDataSupplier {
		public NavigatableComponent getCacheKey();

		public double getCircum();
	}

	private final ElemStyles styles = MapPaintStyles.getStyles();
	private final StyleGenerationState sgs;

	private final OsmPrimitiveRecorder recorder;

	private RecordingStyledMapRenderer recordingRenderer;

	/**
	 * This field stores the active thread for debugging/state-checking.
	 */
	private Thread activeThread;

	/**
	 * A list of geometries that were generated.
	 */
	private ArrayList<RecordedOsmGeometries> recorded = new ArrayList<>();

	protected final boolean drawArea;
	protected final boolean drawMultipolygon;
	protected final boolean drawRestriction = Main.pref.getBoolean(
			"mappaint.restriction", true);

	protected StyledGeometryGenerator(StyleGenerationState sgs) {
		this.sgs = sgs;
		recorder = new OsmPrimitiveRecorder(
				new SimpleRecodingReceiver(recorded));
		drawArea = sgs.getCircum() <= Main.pref.getInteger(
				"mappaint.fillareas", 10000000);
		drawMultipolygon = drawArea
				&& Main.pref.getBoolean("mappaint.multipolygon", true);
	}

	/**
	 * Call {@link #endRunning()} afterwards, preferably in a finally-block.
	 */
	public void startRunning() {
		if (activeThread != null) {
			throw new IllegalStateException("startRunning() already called.");
		}
		recordingRenderer = new RecordingStyledMapRenderer(recorder,
				sgs.getCacheKey(), sgs.isInactiveMode());
		recordingRenderer.getSettings(sgs.renderVirtualNodes());
		// We don't really clip since we need the whole geometry for our cache.
		recordingRenderer.setClipBounds(new Rectangle(-1000000, -1000000,
				2000000, 2000000));
		MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
		activeThread = Thread.currentThread();
	}

	/**
	 * Releases all locks held and frees resources that are not needed any more.
	 */
	public void endRunning() {
		if (activeThread != Thread.currentThread()) {
			throw new IllegalStateException(
					"endRunning() called in the wrong thread.");
		}
		recordingRenderer.dispose();
		MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
		activeThread = null;
	}

	/**
	 * Generates the geometries for a single primitive.
	 * 
	 * @param primitive
	 *            The primitive to run for.
	 * @return The list of geometries for that primitive.
	 */
	public List<RecordedOsmGeometries> runFor(OsmPrimitive primitive) {
		sgs.incrementDrawCounter();
		if (primitive.isDrawable()) {
			if (activeThread != Thread.currentThread()) {
				throw new IllegalStateException(
						"endRunning() called in the wrong thread.");
			}

			runForStyles(primitive);
		}
		ArrayList<RecordedOsmGeometries> received = new ArrayList<>(recorded);
		for (RecordedOsmGeometries r : received) {
			r.mergeChildren();
		}
		recorded.clear();
		return received;
	}

	/**
	 * Generates the styles for a single primitive.
	 * <p>
	 * Since the style cache is not synchronized, we need to ensure that we do
	 * not generate a primitive in background and in foreground on the same
	 * time.
	 * 
	 * @param primitive
	 *            The primitive.
	 * @see StyledMapRenderer.ComputeStyleListWorker#add()
	 */
	protected void runForStyles(OsmPrimitive primitive) {
		synchronized (primitive) {
			primitive.accept(this);
		}
	}

	@Override
	public void visit(Node n) {
		StyleList sl = styles.get(n, sgs.getCircum(), sgs.getCacheKey());
		long state = getState(n);
		for (ElemStyle s : sl) {
			runForStyle(n, s, state);
		}
	}

	@Override
	public void visit(Way w) {
		StyleList sl = styles.get(w, sgs.getCircum(), sgs.getCacheKey());
		long state = getState(w);
		for (ElemStyle s : sl) {
			if (!(drawArea && !w.isDisabled()) && s instanceof AreaElemStyle) {
				continue;
			}
			runForStyle(w, s, state);
		}
	}

	@Override
	public void visit(Relation r) {
		StyleList sl = styles.get(r, sgs.getCircum(), sgs.getCacheKey());
		long state = getState(r);
		for (ElemStyle s : sl) {
			if (drawMultipolygon && drawArea && s instanceof AreaElemStyle
					&& !r.isDisabled()) {
				runForStyle(r, s, state);
			} else if (drawRestriction && s instanceof NodeElemStyle) {
				runForStyle(r, s, state);
			}
		}
	}

	@Override
	public void visit(Changeset cs) {
		throw new UnsupportedOperationException();
	}

	protected void runForStyle(OsmPrimitive primitive, ElemStyle s, long state) {
		recorder.start(primitive, sgs.getViewPosition(),
				getOrderIndex(primitive, s, state));
		s.paintPrimitive(primitive, MapPaintSettings.INSTANCE,
				recordingRenderer, state == STATE_SELECTED,
				state == STATE_OUTERMEMBER_OF_SELECTED,
				state == STATE_MEMBER_OF_SELECTED);
		recorder.end();
	}

	/**
	 * Compute the order index. Pre-computing this and then only sorting by a
	 * long value increases speed.
	 * <ul>
	 * <li>Bit 63: always 0 (sign bit).
	 * <li>Bit 62: FLAG_DISABLED.
	 * <li>Bit 32 - 55: Major z index
	 * <li>Bit 25 - 28: flags
	 * <li>Bit 1 - 24: minor Z index
	 * <li>Bit 0: SIMPLE_NODE_ELEMSTYLE
	 * </ul>
	 * 
	 * @return A long that has the same order as if the style was sorted by the
	 *         Java2D implementation.
	 */
	private static long getOrderIndex(OsmPrimitive primitive, ElemStyle s,
			long state) {
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

		return orderIndex;
	}

	private static long getState(OsmPrimitive primitive) {
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
	 * 
	 * @param number
	 * @param totalBits
	 *            Total number of bits. 1 sign bit, then the bits before the
	 *            decimal point, then those after.
	 * @param afterSignBits
	 *            Number of fixed bits after the decimal point.
	 * @return The float converted to an integer.
	 */
	private static long floatToFixed(double number, int totalBits,
			int afterSignBits) {
		long value = (long) (number * (1l << afterSignBits));
		long highestBitMask = 1l << (totalBits - 1);
		long valueMask = (highestBitMask - 1);
		long signBit = number < 0 ? 0 : highestBitMask;
		return signBit | (value & valueMask);
	}
}