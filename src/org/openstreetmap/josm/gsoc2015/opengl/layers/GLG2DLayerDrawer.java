package org.openstreetmap.josm.gsoc2015.opengl.layers;

import java.awt.Graphics2D;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;

/**
 * This is a layer drawer that uses GLG2D to paint the layer. This is not
 * optimized for OpenGL but should render all normal layers just fine.
 * 
 * @author Michael Zangl
 *
 */
public class GLG2DLayerDrawer extends LayerDrawer {

	public GLG2DLayerDrawer(MapViewPaintable layer) {
		super(layer);
	}

	@Override
	public void drawLayer(Graphics2D g2d, MapView mv, Bounds box) {
		getLayer().paint(g2d, mv, box);
	}
}