package org.openstreetmap.josm.gsoc2015.opengl.osm;

/**
 * This listener gets informed whenever a full repaint is suggested.
 * @author Michael Zangl
 *
 */
public interface FullRepaintListener {

	/**
	 * Request a full repaint.
	 */
	public void requestRepaint();
}
