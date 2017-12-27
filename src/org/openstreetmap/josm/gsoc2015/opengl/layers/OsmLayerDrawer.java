package org.openstreetmap.josm.gsoc2015.opengl.layers;

import java.awt.Graphics2D;

import org.jogamp.glg2d.GLGraphics2D;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.paint.MapRendererFactory;
import org.openstreetmap.josm.data.osm.visitor.paint.Rendering;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OpenGLStyledMapRenderer;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This class handles the drawing of a OSM data layer.
 *
 * @author Michael Zangl
 */
public class OsmLayerDrawer extends LayerDrawer {
	/**
	 * The layer we draw.
	 */
	private final OsmDataLayer osmLayer;
	/**
	 * A flag to clear the cache when the inactive flag changed.
	 */
	private boolean cachedIsInactive;

	/**
	 * The renderer to draw the map. It is preserved between frames to allow it
	 * to cache geometries.
	 */
	private OpenGLStyledMapRenderer cachedRenderer;

	/**
	 * Create a new {@link OsmLayerDrawer}
	 *
	 * @param osmLayer
	 *            The layer we draw.
	 */
	public OsmLayerDrawer(OsmDataLayer osmLayer) {
		super(osmLayer);
		this.osmLayer = osmLayer;
	}

	@Override
	public void drawLayer(Graphics2D g2d, MapView mv, Bounds box) {
		final boolean active = MainApplication.getLayerManager().getActiveLayer() == osmLayer;
		final boolean inactive = !active
				&& Main.pref.getBoolean("draw.data.inactive_color", true);
		final boolean virtual = !inactive && mv.isVirtualNodesEnabled();

		Rendering painter = MapRendererFactory.getInstance()
				.createActiveRenderer(g2d, mv, inactive);
		if (painter instanceof StyledMapRenderer) {
			if (cachedRenderer == null || cachedIsInactive != inactive) {
				disposeCachedRenderer();
				cachedRenderer = new OpenGLStyledMapRenderer(
						(GLGraphics2D) g2d, mv, inactive);
				cachedIsInactive = inactive;
				// We could also only invalidate on inactive change...
			}
			painter = cachedRenderer;
		} else {
			disposeCachedRenderer();
		}
		painter.render(osmLayer.data, virtual, box);

		// This should be a temporary layer. Or even a full
		// layer. What about sublayers?
		MainApplication.getMap().conflictDialog.paintConflicts(g2d, mv);
	}

	private void disposeCachedRenderer() {
		if (cachedRenderer != null) {
			cachedRenderer.dispose();
			cachedRenderer = null;
		}
	}

	@Override
	public void dispose() {
		disposeCachedRenderer();
		super.dispose();
	}

}
