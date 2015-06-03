package org.openstreetmap.josm.gsoc2015.opengl.jogl;

import java.awt.Component;

import org.openstreetmap.josm.gui.MapView;

/**
 * Removes all class dependencies to JOGL.
 * @author michael
 *
 */
public class MapPanelFactory {
	public static Component getMapRenderer(MapView mapView) {
		return new MapPanel(mapView);
	}
}
