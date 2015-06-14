package org.openstreetmap.josm.gsoc2015.opengl;

import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

import org.openstreetmap.josm.gui.MapView;

public class InterceptMouseEvents extends JPanel {

	private MapView forwardTo;

	public InterceptMouseEvents(final MapView forwardTo) {
		this.forwardTo = forwardTo;
		this.addMouseListener(forwardTo.mapMover);
		this.addMouseMotionListener(forwardTo.mapMover);
		this.addMouseWheelListener(forwardTo.mapMover);
		this.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				mouseMoved(e);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				System.out.println("Intercept mouse move...");
				forwardTo.lastMEvent = e;
			}
		});
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent me) {
				// focus the MapView component when mouse is pressed inside it
				forwardTo.requestFocus();
			}
		});
		setOpaque(false);

	}
	
	@Override
	public void paint(Graphics g) {
	}

}
