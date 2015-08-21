package org.openstreetmap.josm.gsoc2015.opengl.jogl;

import java.util.List;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JComponent;

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
		private final List<? extends JComponent> navigationComponents;

		/**
		 * Creates a new paint mode listener
		 * 
		 * @param navigationComponents
		 *            Components to paint over the map.
		 */
		public GLJPanelListener(List<? extends JComponent> navigationComponents) {
			this.navigationComponents = navigationComponents;
		}

		@Override
		public void paintModeChanged(PaintMode newMode) {
			if (activeDrawer != null) {
				disposeGLEventListener(activeDrawer, true);
				activeDrawer = null;
			}
			if (newMode == PaintMode.OPENGL) {
				activeDrawer = new GLDrawer(mapView, navigationComponents);
				addGLEventListener(activeDrawer);
				repaint();
			}
		}

		@Override
		public void mapViewportChanged() {
			repaint();
		}
	}

	private static final long serialVersionUID = 7479651466293194041L;
	private final Animator animator;

	public static GLCapabilities getDefaultCapabalities() {
		final GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES1());
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

	/**
	 * Creates a new map panel that mirrors the given map view.
	 *
	 * @param mapView
	 *            The map view.
	 * @param navigationComponents
	 *            Components to paint over the map.
	 */
	public MapPanel(MapView mapView,
			List<? extends JComponent> navigationComponents) {
		super(getDefaultCapabalities());
		this.mapView = mapView;

		mapView.addRepaintListener(this);

		// When to repaint...
		animator = new Animator(this);
		animator.setRunAsFastAsPossible(false);
		final GLJPanelListener l = new GLJPanelListener(navigationComponents);
		new MapViewportObserver(mapView).addMapViewportListener(l);

		MapViewPaintModeState.getInstance().addPaintModeListener(l, true);
	}
}
