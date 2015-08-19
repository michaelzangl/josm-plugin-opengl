package org.openstreetmap.josm.gsoc2015.opengl;

import java.util.LinkedList;

/**
 * This class handles the global paint mode of the map view. It can change
 * between OpenGL and Java2D.
 * <p>
 * Global change listeners can be registered.
 *
 * @author Michael Zangl
 *
 */
public final class MapViewPaintModeState {

	/**
	 * The supported paint modes.
	 *
	 * @author Michael Zangl
	 */
	public enum PaintMode {
		JAVA2D, OPENGL
	}

	/**
	 * A listener that listens for paint mode changes.
	 *
	 * @author Michael Zangl
	 *
	 */
	public interface PaintModeListener {
		void paintModeChanged(MapViewPaintModeState.PaintMode newMode);
	}

	private static MapViewPaintModeState instance = new MapViewPaintModeState();

	private MapViewPaintModeState.PaintMode currentPaintMode = PaintMode.JAVA2D;

	private final LinkedList<MapViewPaintModeState.PaintModeListener> paintModeListeners = new LinkedList<>();

	private MapViewPaintModeState() {
	}

	/**
	 * Get the current paint mode.
	 *
	 * @return The paint mode that was set last.
	 */
	public synchronized PaintMode getCurrentPaintMode() {
		return currentPaintMode;
	}

	/**
	 * Changes the paint mode and fires the change listeners if it was changed.
	 *
	 * @param currentPaintMode
	 *            The paint mode.
	 */
	public synchronized void setCurrentPaintMode(
			MapViewPaintModeState.PaintMode currentPaintMode) {
		if (currentPaintMode == null) {
			throw new NullPointerException("Paint mode may not be null.");
		}
		if (this.currentPaintMode != currentPaintMode) {
			this.currentPaintMode = currentPaintMode;
			System.out.println("Setting paint mode: " + currentPaintMode);
			for (final MapViewPaintModeState.PaintModeListener l : paintModeListeners) {
				l.paintModeChanged(currentPaintMode);
			}
		}
	}

	/**
	 * Add a new paint mode listener.
	 *
	 * @param l
	 *            The listener
	 * @param fireNow
	 *            <code>true</code> if the listener should be fired now.
	 * @see #removePaintModeListener(PaintModeListener)
	 */
	public synchronized void addPaintModeListener(
			MapViewPaintModeState.PaintModeListener l, boolean fireNow) {
		paintModeListeners.add(l);
		if (fireNow) {
			l.paintModeChanged(getCurrentPaintMode());
		}
	}

	/**
	 * Removes a paint mode listener.
	 *
	 * @param l
	 *            The listener
	 * @see #addPaintModeListener(PaintModeListener, boolean)
	 */
	public synchronized void removePaintModeListener(
			MapViewPaintModeState.PaintModeListener l) {
		paintModeListeners.remove(l);
	}

	/**
	 * Gets the one single {@link MapViewPaintModeState} object.
	 *
	 * @return
	 */
	public static MapViewPaintModeState getInstance() {
		return instance;
	}
}