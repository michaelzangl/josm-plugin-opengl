package org.openstreetmap.josm.gsoc2015.opengl.layer;

import java.awt.Graphics2D;

import org.jogamp.glg2d.GLGraphics2D;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.paint.MapRendererFactory;
import org.openstreetmap.josm.data.osm.visitor.paint.Rendering;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gsoc2015.opengl.layers.LayerDrawer;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OpenGLStyledMapRenderer;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class OsmLayerDrawer extends LayerDrawer {
	private final OsmDataLayer osmLayer;
	private boolean cachedIsInactive;
	private OpenGLStyledMapRenderer cachedRenderer;

	public OsmLayerDrawer(OsmDataLayer osmLayer) {
		super(osmLayer);
		this.osmLayer = osmLayer;
	}

	@Override
	public void drawLayer(Graphics2D g2d, MapView mv, Bounds box) {
		boolean active = mv.getActiveLayer() == osmLayer;
		boolean inactive = !active
				&& Main.pref.getBoolean("draw.data.inactive_color", true);
		boolean virtual = !inactive && mv.isVirtualNodesEnabled();

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
		Main.map.conflictDialog.paintConflicts(g2d, mv);
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
