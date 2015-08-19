package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.awt.Dimension;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;

/**
 * This class describes the current position of the map view.
 *
 * @author Michael Zangl
 */
public class ViewPosition {

	/**
	 * The scale.
	 *
	 * @see MapView#getScale()
	 */
	private final double scale;
	/**
	 * Current n/e coordinate of the screen corner.
	 */
	private final EastNorth upperLeft;

	/**
	 * The size of the map view.
	 */
	private final Dimension windowSize;

	/**
	 * Creates a new view position.
	 *
	 * @param scale
	 *            The scale
	 * @param upperLeft
	 *            The upper left corner position
	 * @param windowSize
	 *            The size of the map view.
	 */
	public ViewPosition(double scale, EastNorth upperLeft, Dimension windowSize) {
		super();
		this.scale = scale;
		this.upperLeft = upperLeft;
		this.windowSize = windowSize;
	}

	/**
	 * Creates a new {@link ViewPosition} for the current map state.
	 *
	 * @param nc
	 *            The map view to take the position from
	 * @return The view position for that map view.
	 */
	public static ViewPosition from(NavigatableComponent nc) {
		return new ViewPosition(nc.getScale(), nc.getEastNorth(0, 0),
				nc.getSize());
	}

	/**
	 * Gets the translation matrix value to translate between this and the other
	 * view position.
	 *
	 * @param other
	 *            The other view position.
	 * @return The translation vector x coordinate.
	 */
	public double translateXTo(ViewPosition other) {
		final double dEast = other.upperLeft.east() - upperLeft.east();
		return dEast / scale;
	}

	/**
	 * Gets the translation matrix value to translate between this and the other
	 * view position.
	 *
	 * @param other
	 *            The other view position.
	 * @return The translation vector y coordinate.
	 */
	public double translateYTo(ViewPosition other) {
		final double dEast = other.upperLeft.north() - upperLeft.north();
		return -dEast / scale;
	}

	/**
	 * Gets the translation matrix value to scale between this and the other
	 * view position.
	 *
	 * @param other
	 *            The other view position.
	 * @return The scale difference.
	 */
	public double translateScaleTo(ViewPosition other) {
		return other.scale / scale;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(scale);
		result = prime * result + (int) (temp ^ temp >>> 32);
		result = prime * result
				+ (upperLeft == null ? 0 : upperLeft.hashCode());
		result = prime * result
				+ (windowSize == null ? 0 : windowSize.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ViewPosition other = (ViewPosition) obj;
		if (Double.doubleToLongBits(scale) != Double
				.doubleToLongBits(other.scale)) {
			return false;
		}
		if (upperLeft == null) {
			if (other.upperLeft != null) {
				return false;
			}
		} else if (!upperLeft.equals(other.upperLeft)) {
			return false;
		}
		if (windowSize == null) {
			if (other.windowSize != null) {
				return false;
			}
		} else if (!windowSize.equals(other.windowSize)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ViewPosition [scale=" + scale + ", upperLeft=" + upperLeft
				+ ", windowSize=" + windowSize + "]";
	}

}
