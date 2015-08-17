package org.openstreetmap.josm.gsoc2015.opengl.jogl;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;

import org.openstreetmap.josm.gsoc2015.opengl.MapViewPaintModeState;
import org.openstreetmap.josm.gsoc2015.opengl.MapViewPaintModeState.PaintMode;
import org.openstreetmap.josm.gsoc2015.opengl.MapViewPaintModeState.PaintModeListener;
import org.openstreetmap.josm.gsoc2015.opengl.temp.MapViewportObserver;
import org.openstreetmap.josm.gsoc2015.opengl.temp.MapViewportObserver.MapViewportListener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.RepaintListener;

import com.jogamp.opengl.util.Animator;

/**
 * This is our replacement for the {@link MapView}. This panel draws the current
 * map state.
 * 
 * @author Michael Zangl
 *
 */
public class MapPanel extends GLJPanel implements RepaintListener {

	private final class GLJPanelListener implements PaintModeListener,
			MapViewportListener {
		@Override
		public void paintModeChanged(PaintMode newMode) {
			if (activeDrawer != null) {
				removeGLEventListener(activeDrawer);
				activeDrawer.dispose(MapPanel.this);
				activeDrawer = null;
			}
			if (newMode == PaintMode.OPENGL) {
				activeDrawer = new GLDrawer(mapView);
				addGLEventListener(activeDrawer);
				repaint();
			}
		}

		@Override
		public void mapViewportChanged() {
			repaint();
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 7479651466293194041L;
	private final Animator animator;

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

	private final MapView mapView;

	private GLDrawer activeDrawer = null;

	public MapPanel(MapView mapView) {
		super(getDefaultCapabalities());
		this.mapView = mapView;

		mapView.addRepaintListener(this);

		// When to repaint...
		animator = new Animator(this);
		animator.setRunAsFastAsPossible(false);
		GLJPanelListener l = new GLJPanelListener();
		new MapViewportObserver(mapView).addMapViewportListener(l);

		MapViewPaintModeState.getInstance().addPaintModeListener(l, true);
	}
}
