package org.openstreetmap.josm.gsoc2015.opengl;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;

import org.openstreetmap.josm.gui.MapView;

/**
 * This panel intercepts all mouse events and forwards them to the hidden map
 * view. Other plugins and JOSM core register all map events a slisteners on the
 * MapView. This makes it impossible for us to know which ones they registered
 * and thus we need to simulate a event on the map view every time it occurs on
 * the GL view.
 * 
 * @author Michael Zangl
 *
 */
public class InterceptMouseEvents extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -9017627618812451008L;

	/**
	 * Just forwards all events to the MapView and lets the map view.
	 * 
	 * @author Michael Zangl
	 *
	 */
	private class MouseEventForwarder implements MouseListener,
			MouseMotionListener, MouseWheelListener {
	
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			for (MouseWheelListener l : forwardTo
					.getListeners(MouseWheelListener.class)) {
				l.mouseWheelMoved(e);
			}
		}
	
		@Override
		public void mouseDragged(MouseEvent e) {
			for (MouseMotionListener l : forwardTo
					.getListeners(MouseMotionListener.class)) {
				l.mouseDragged(e);
			}
		}
	
		@Override
		public void mouseMoved(MouseEvent e) {
			for (MouseMotionListener l : forwardTo
					.getListeners(MouseMotionListener.class)) {
				l.mouseMoved(e);
			}
		}
	
		@Override
		public void mouseClicked(MouseEvent e) {
			for (MouseListener l : forwardTo.getListeners(MouseListener.class)) {
				l.mouseClicked(e);
			}
		}
	
		@Override
		public void mousePressed(MouseEvent e) {
			for (MouseListener l : forwardTo.getListeners(MouseListener.class)) {
				l.mousePressed(e);
			}
		}
	
		@Override
		public void mouseReleased(MouseEvent e) {
			for (MouseListener l : forwardTo.getListeners(MouseListener.class)) {
				l.mouseReleased(e);
			}
		}
	
		@Override
		public void mouseEntered(MouseEvent e) {
			for (MouseListener l : forwardTo.getListeners(MouseListener.class)) {
				l.mouseEntered(e);
			}
		}
	
		@Override
		public void mouseExited(MouseEvent e) {
			for (MouseListener l : forwardTo.getListeners(MouseListener.class)) {
				l.mouseExited(e);
			}
		}
	}

	private MapView forwardTo;
	
	private MouseEventForwarder forwarder = new MouseEventForwarder();

	public InterceptMouseEvents(final MapView forwardTo) {
		this.forwardTo = forwardTo;
		addMouseListener(forwarder);
		addMouseMotionListener(forwarder);
		addMouseWheelListener(forwarder);
		forwardTo.getCursorManager().addComponent(this);;
		// MapMover mover = new MapMover(forwardTo.getNavigationModel(),
		// cursorManager, null);
		// mover.registerMouseEvents(this);
		// this.addMouseMotionListener(new MouseMotionListener() {
		// @Override
		// public void mouseDragged(MouseEvent e) {
		// mouseMoved(e);
		// }
		//
		// @Override
		// public void mouseMoved(MouseEvent e) {
		// forwardTo.lastMEvent = e;
		// }
		// });
		// this.addMouseListener(new MouseAdapter() {
		// @Override
		// public void mousePressed(MouseEvent me) {
		// // focus the MapView component when mouse is pressed inside it
		// forwardTo.requestFocus();
		// }
		// });
		setOpaque(false);

	}

	@Override
	public void paint(Graphics g) {
		// transparent.
	}

}
