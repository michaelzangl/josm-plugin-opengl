package org.openstreetmap.josm.gsoc2015.opengl.layers;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This class manages the drawing of the single layers.
 * <p>
 * To use it, you need to call {@link #setLayersToDraw(List)}. Then, you can
 * draw all layers using {@link #draw(Graphics2D, MapView, Bounds)}.
 *
 * @author Michael Zangl
 *
 */
public class LayerDrawManager {

	private final ArrayList<LayerDrawer> layerDrawers = new ArrayList<>();

	public void setLayersToDraw(List<? extends MapViewPaintable> list) {
		final HashMap<MapViewPaintable, LayerDrawer> oldLayerDrawers = new HashMap<>();
		for (final LayerDrawer d : layerDrawers) {
			oldLayerDrawers.put(d.getLayer(), d);
		}

		layerDrawers.clear();
		for (final MapViewPaintable layer : list) {
			LayerDrawer drawer = oldLayerDrawers.remove(layer);
			if (drawer == null) {
				drawer = createDrawerForLayer(layer);
			}
			layerDrawers.add(drawer);
		}

		// Now tell the remaining drawers that we don't need them any more.
		for (final LayerDrawer drawer : oldLayerDrawers.values()) {
			drawer.dispose();
		}
	}

	private LayerDrawer createDrawerForLayer(MapViewPaintable l) {
		if (l instanceof OsmDataLayer) {
			return new OsmLayerDrawer((OsmDataLayer) l);
		} else {
			return new GLG2DLayerDrawer(l);
		}
	}

	public void draw(Graphics2D g2d, MapView mv, Bounds box) {
		for (final LayerDrawer ld : layerDrawers) {
			ld.drawLayer(g2d, mv, box);
		}
	}

	public void dispose() {
		setLayersToDraw(Collections.<MapViewPaintable>emptyList());
	}
}
