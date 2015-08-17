package org.openstreetmap.josm.gsoc2015.opengl.jogl;

import java.awt.AlphaComposite;
import java.awt.Rectangle;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import org.jogamp.glg2d.GLGraphics2D;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.gsoc2015.opengl.layers.LayerDrawManager;
import org.openstreetmap.josm.gui.MapView;

/**
 * Draws the layers of the {@link #mapView} to the panel.
 * 
 * @author Michael Zangl
 *
 */
final class GLDrawer implements GLEventListener {
	protected GLGraphics2D g2d;
	final MapView mapView;

	private LayerDrawManager layerDrawer = new LayerDrawManager();
	private LayerDrawManager temporaryLayerDrawer = new LayerDrawManager();

	public GLDrawer(MapView mapView) {
		super();
		this.mapView = mapView;
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		g2d = new GLGraphics2D();
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		if (g2d != null) {
			g2d.glDispose();
			g2d = null;
		}

		layerDrawer.dispose();
		temporaryLayerDrawer.dispose();
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		// System.out.println("BEGIN display()");
		drawable.getGL().glViewport(0, 0, drawable.getSurfaceWidth(),
				drawable.getSurfaceHeight());
		long time = System.currentTimeMillis();

		try {
			drawAllLayers(drawable);
		} catch (Throwable t) {
			System.err.println(t);
			t.printStackTrace();
		}
		// System.out.println(String.format("END display(), t = %d ms",
		// System.currentTimeMillis() - time));
	}

	private void drawAllLayers(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		g2d.prePaint(drawable.getContext());

		// Draw the window background
		g2d.setClip(null);
		g2d.setColor(PaintColors.getBackgroundColor());
		g2d.setComposite(AlphaComposite.Src);
		g2d.fillRect(0, 0, drawable.getSurfaceWidth(),
				drawable.getSurfaceHeight());
		g2d.setComposite(AlphaComposite.SrcOver);

		// clip to only the component we're painting
		g2d.clipRect(0, 0, drawable.getSurfaceWidth(),
				drawable.getSurfaceHeight());

		// reload the layers (we might make this event based)
		layerDrawer.setLayersToDraw(mapView.getVisibleLayersInZOrder());
		temporaryLayerDrawer.setLayersToDraw(mapView.getTemporaryLayers());

		Bounds box = mapView.getLatLonBounds(new Rectangle(drawable
				.getSurfaceWidth(), drawable.getSurfaceHeight()));
		layerDrawer.draw(g2d, mapView, box);
		temporaryLayerDrawer.draw(g2d, mapView, box);
		g2d.postPaint();

		// Set alpha to full. Otherwise, we will get annoying effects the
		// next time we draw.
		gl.glColorMask(false, false, false, true);
		gl.glDisable(GL.GL_SCISSOR_TEST);
		gl.glDisable(GL.GL_DITHER);
		gl.glClearColor(0, 0, 0, 1.0f);
		gl.glClear(GL2ES1.GL_COLOR_BUFFER_BIT);
		gl.glColorMask(true, true, true, true);
	}

}