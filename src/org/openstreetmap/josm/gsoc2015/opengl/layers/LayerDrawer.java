package org.openstreetmap.josm.gsoc2015.opengl.layers;

import java.awt.Graphics2D;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;

/**
 * This class handles drawing of a single layer.
 * 
 * @author Michael Zangl
 */
public abstract class LayerDrawer {
	private final MapViewPaintable layer;

	public LayerDrawer(MapViewPaintable layer) {
		this.layer = layer;

	}

	public abstract void drawLayer(Graphics2D g2d, MapView mv, Bounds box);

	public MapViewPaintable getLayer() {
		return layer;
	}

	/**
	 * Tells this drawer that it is not needed any more and that it should
	 * release als resources it holds.
	 */
	public void dispose() {
	}
}