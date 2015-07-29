package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.awt.Dimension;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.NavigatableComponent;

public class ViewPosition {

    private final double scale;
    /**
     * Center n/e coordinate of the screen corner.
     */
    private final EastNorth upperLeft;
    
    private final Dimension windowSize;
    
	public ViewPosition(double scale, EastNorth upperLeft, Dimension windowSize) {
		super();
		this.scale = scale;
		this.upperLeft = upperLeft;
		this.windowSize = windowSize;
	}

	public static ViewPosition from(NavigatableComponent nc) {
		return new ViewPosition(nc.getScale(), nc.getEastNorth(0, 0), nc.getSize());
	}

	public double translateXTo(ViewPosition other) {
		double dEast = other.upperLeft.east() - this.upperLeft.east();
		return dEast / scale;
	}

	public double translateYTo(ViewPosition other) {
		double dEast = other.upperLeft.north() - this.upperLeft.north();
		return -dEast / scale;
	}

	public double translateScaleTo(ViewPosition other) {
		return other.scale / this.scale;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(scale);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((upperLeft == null) ? 0 : upperLeft.hashCode());
		result = prime * result
				+ ((windowSize == null) ? 0 : windowSize.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ViewPosition other = (ViewPosition) obj;
		if (Double.doubleToLongBits(scale) != Double
				.doubleToLongBits(other.scale))
			return false;
		if (upperLeft == null) {
			if (other.upperLeft != null)
				return false;
		} else if (!upperLeft.equals(other.upperLeft))
			return false;
		if (windowSize == null) {
			if (other.windowSize != null)
				return false;
		} else if (!windowSize.equals(other.windowSize))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ViewPosition [scale=" + scale + ", upperLeft=" + upperLeft
				+ ", windowSize=" + windowSize + "]";
	}
	
}
