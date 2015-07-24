package org.openstreetmap.josm.gsoc2015.opengl.jogl;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OsmLayerDrawer;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This class manages the drawing of the single layers.
 * @author michael
 *
 */
public class LayerDrawManager {

	public static abstract class LayerDrawer {
		private final MapViewPaintable layer;
		
		public LayerDrawer(MapViewPaintable layer) {
			this.layer = layer;
			
		}
		
		public abstract void drawLayer(Graphics2D g2d, MapView mv, Bounds box);

		
		public MapViewPaintable getLayer() {
			return layer;
		}
	}
	
	public static class GLG2DLayerDrawer extends LayerDrawer {

		public GLG2DLayerDrawer(MapViewPaintable layer) {
			super(layer);
		}
		
		@Override
		public void drawLayer(Graphics2D g2d, MapView mv, Bounds box) {
			getLayer().paint(g2d, mv, box);
		}
	}
	
	private List<LayerDrawer> layerDrawers = Collections.emptyList();
	
	public void setLayersToDraw(List<? extends MapViewPaintable> list) {
		List<LayerDrawer> oldLayerDrawers = layerDrawers;
		
		layerDrawers = new ArrayList<>();
		for (MapViewPaintable l : list) {
			LayerDrawer drawer = findDrawerInList(oldLayerDrawers, l);
			if (drawer == null) {
				drawer = createDrawerForLayer(l);
			}
			layerDrawers.add(drawer);
		}
	}

	private LayerDrawer createDrawerForLayer(MapViewPaintable l) {
		if (l instanceof OsmDataLayer) {
			return new OsmLayerDrawer((OsmDataLayer) l);
		} else {
			return new GLG2DLayerDrawer(l);
		}
	}

	private LayerDrawer findDrawerInList(List<LayerDrawer> layerDrawers, MapViewPaintable forLayer) {
		for (LayerDrawer ld : layerDrawers) {
			if (ld.getLayer() == forLayer) {
				return ld;
			}
		}
		return null;
	}
	
	public void draw(Graphics2D g2d, MapView mv, Bounds box) {
		for (LayerDrawer ld : layerDrawers) {
			ld.drawLayer(g2d, mv, box);
		}
	}
	
	public void dispose() {
		layerDrawers = Collections.emptyList();
	}
}
