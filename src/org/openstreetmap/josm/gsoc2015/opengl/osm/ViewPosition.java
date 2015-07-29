package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.awt.Dimension;

import org.openstreetmap.josm.Main;
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
	
}
