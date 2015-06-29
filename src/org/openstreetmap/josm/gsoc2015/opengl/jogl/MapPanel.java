package org.openstreetmap.josm.gsoc2015.opengl.jogl;

import java.awt.AlphaComposite;
import java.awt.Rectangle;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import org.jogamp.glg2d.GLGraphics2D;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.gsoc2015.opengl.MapViewPaintModeState;
import org.openstreetmap.josm.gsoc2015.opengl.MapViewPaintModeState.PaintMode;
import org.openstreetmap.josm.gsoc2015.opengl.MapViewPaintModeState.PaintModeListener;
import org.openstreetmap.josm.gsoc2015.opengl.temp.MapViewportObserver;
import org.openstreetmap.josm.gsoc2015.opengl.temp.MapViewportObserver.MapViewportListener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.RepaintListener;

import com.jogamp.opengl.util.Animator;

public class MapPanel extends GLJPanel implements RepaintListener {
	private MapView mapView;
	private Animator animator;

	/**
	 * Draws the layers of the {@link #mapView} to the panel.
	 * 
	 * @author michael
	 *
	 */
	private final class GLDrawer implements GLEventListener {
		protected GLGraphics2D g2d;
		
		private LayerDrawManager layerDrawer = new LayerDrawManager();
		private LayerDrawManager temporaryLayerDrawer = new LayerDrawManager();

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

		private double animateStep = 0;

		@Override
		public void display(GLAutoDrawable drawable) {
			System.out.println("BEGIN display()");
			drawable.getGL().glViewport(0, 0, drawable.getSurfaceWidth(),
					drawable.getSurfaceHeight());
			long time = System.currentTimeMillis();
			
			try {
//			drawTestScene(gl);
//			drawMapView(drawable);
			drawAllLayers(drawable);
			} catch (Throwable t) {
				System.err.println(t);
				t.printStackTrace();
			}
			System.out.println(String.format("END display(), t = %d ms", System.currentTimeMillis() - time));
		}

		private void drawTestScene(GL2 gl) {
			int[] viewportDimensions = new int[4];
			gl.glGetIntegerv(GL.GL_VIEWPORT, viewportDimensions, 0);
			int width = viewportDimensions[2];
			int height = viewportDimensions[3];

			gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glOrtho(0, width, 0, height, -1, 1);

			// the MODELVIEW matrix will get adjusted later

			gl.glMatrixMode(GL.GL_TEXTURE);
			gl.glLoadIdentity();

			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();

			gl.glColor3f(1, 1, 1);
			gl.glBegin(GL2.GL_TRIANGLES); // draw using triangles
			animateStep += .4;
			gl.glVertex3f(200.0f, 300.0f, 0.0f);
			gl.glVertex3f(100.0f + 20f * (float) Math.cos(animateStep),
					100.0f + 20f * (float) Math.sin(animateStep), 0.0f);
			gl.glVertex3f(300.0f, 100.0f, 0.0f);
			gl.glEnd();
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
			
			// Set alpha to full. Otherwise, we will get annoying effects the next time we draw.
			gl.glColorMask(false, false, false, true);
			gl.glDisable(GL.GL_SCISSOR_TEST);
			gl.glDisable(GL.GL_DITHER);
			gl.glClearColor(0, 0, 0, 1.0f);
			gl.glClear(GL2ES1.GL_COLOR_BUFFER_BIT);
			gl.glColorMask(true, true, true, true);
		}

	}

	public static GLCapabilities getDefaultCapabalities() {
		GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES1());
		caps.setRedBits(8);
		caps.setGreenBits(8);
		caps.setBlueBits(8);
		caps.setAlphaBits(8);
		caps.setDoubleBuffered(true);
		caps.setHardwareAccelerated(true);
		caps.setNumSamples(4);
		caps.setBackgroundOpaque(false);
		caps.setSampleBuffers(true);
		return caps;
	}

	public MapPanel(MapView mapView) {
		super(getDefaultCapabalities());
		this.mapView = mapView;
		
		addGLEventListener(new GLDrawer());
		
		mapView.addRepaintListener(this);

		// When to repaint...
		animator = new Animator(this);
		animator.setRunAsFastAsPossible(false);
		new MapViewportObserver(mapView).addMapViewportListener(new MapViewportListener() {
			@Override
			public void mapViewportChanged() {
				repaint();
			}
		});
		
		MapViewPaintModeState.getInstance().addPaintModeListener(new PaintModeListener() {
			@Override
			public void paintModeChanged(PaintMode newMode) {
				if (newMode == PaintMode.OPENGL) {
					repaint();
				}
			}
		}, true);
	}
}
