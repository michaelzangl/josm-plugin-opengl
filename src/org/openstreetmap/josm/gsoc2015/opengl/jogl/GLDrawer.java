package org.openstreetmap.josm.gsoc2015.opengl.jogl;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.swing.JComponent;

import org.jogamp.glg2d.GLGraphics2D;
import org.openstreetmap.josm.Main;
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

	private final LayerDrawManager layerDrawer = new LayerDrawManager();
	private final LayerDrawManager temporaryLayerDrawer = new LayerDrawManager();
	private final List<? extends JComponent> navigationComponents;

	/**
	 * Creates a new {@link GLDrawer}
	 * 
	 * @param mapView
	 *            The map view to draw for.
	 * @param navigationComponents
	 *            Components to draw over the view.
	 */
	public GLDrawer(MapView mapView,
			List<? extends JComponent> navigationComponents) {
		super();
		this.mapView = mapView;
		this.navigationComponents = navigationComponents;
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
		// long time = System.currentTimeMillis();

		try {
			drawAllLayers(drawable);
		} catch (final Throwable t) {
			System.err.println(t);
			t.printStackTrace();
		}
		// System.out.println(String.format("END display(), t = %d ms",
		// System.currentTimeMillis() - time));
	}

	private void drawAllLayers(GLAutoDrawable drawable) {
		final GL2 gl = drawable.getGL().getGL2();
		// gl.glTranslatef(-.5f, -.5f, 0);
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
		layerDrawer.setLayersToDraw(Main.getLayerManager().getVisibleLayersInZOrder());
		temporaryLayerDrawer.setLayersToDraw(mapView.getTemporaryLayers());

		final Bounds box = mapView.getLatLonBounds(new Rectangle(drawable
				.getSurfaceWidth(), drawable.getSurfaceHeight()));
		layerDrawer.draw(g2d, mapView, box);
		temporaryLayerDrawer.draw(g2d, mapView, box);
		paintComponents(g2d);
		g2d.postPaint();

		// Set alpha to full. Otherwise, we will get annoying effects the
		// next time we draw.
		gl.glColorMask(false, false, false, true);
		gl.glDisable(GL.GL_SCISSOR_TEST);
		gl.glDisable(GL.GL_DITHER);
		gl.glClearColor(0, 0, 0, 1.0f);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		gl.glColorMask(true, true, true, true);
	}

	private void paintComponents(GLGraphics2D g) {
		for (JComponent c : navigationComponents) {
			Rectangle cr = c.getBounds();
			System.out.println(cr);
            Graphics cg = g.create(cr.x, cr.y, cr.width,
                    cr.height);
            cg.setColor(c.getForeground());
            cg.setFont(c.getFont());
			c.paint(g);
			cg.dispose();
		}
	}

}