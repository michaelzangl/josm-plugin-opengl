package org.openstreetmap.josm.gsoc2015.opengl.osm;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.OsmPrimitiveRecorder;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.OsmPrimitiveRecorder.RecordedPrimitiveReceiver;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OpenGLStyledMapRenderer.StyleGenerationState;
import org.openstreetmap.josm.gui.NavigatableComponent;
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
public class StyledGeometryGenerator<T extends OsmPrimitive> {

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

	private final RecordingStyledMapRenderer recordingRenderer;

	public StyledGeometryGenerator(StyleGenerationState sgs,
			RecordedPrimitiveReceiver receiver) {
		this.sgs = sgs;
		recorder = new OsmPrimitiveRecorder(receiver);
		recordingRenderer = new RecordingStyledMapRenderer(recorder,
				sgs.getCacheKey(), sgs.isInactiveMode());
		recordingRenderer.getSettings(sgs.renderVirtualNodes());
		recordingRenderer.setClipBounds(sgs.getClipBounds());
	}

	/**
	 * Call {@link #endRunning()} afterwards, preferably in a finally-block.
	 */
	public void startRunning() {
		MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
	}

	/**
	 * Call
	 */
	public void endRunning() {
		MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
	}

	void runFor(T primitive) {
		if (primitive instanceof Relation) {
			return; //XXX Temp bug?
		}
		if (primitive.isDrawable()) {
			StyleList sl = styles.get(primitive, sgs.getCircum(),
					sgs.getCacheKey());

			for (ElemStyle s : sl) {
				long state = getState(primitive);
				runForStyle(primitive, s, state);
			}
		}
	}

	private void runForStyle(T primitive, ElemStyle s, long state) {
		recorder.start(primitive, getOrderIndex(primitive, s, state));
		s.paintPrimitive(primitive, MapPaintSettings.INSTANCE,
				recordingRenderer, state == STATE_SELECTED,
				state == STATE_OUTERMEMBER_OF_SELECTED,
				state == STATE_MEMBER_OF_SELECTED);
		recorder.end();
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