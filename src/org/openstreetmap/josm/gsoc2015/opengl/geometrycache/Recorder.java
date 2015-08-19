package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

/**
 * Classes implementing this can receive recorded geometries.
 *
 * @author Michael Zangl
 *
 */
public interface Recorder {

	/**
	 * A NOP-Recorder that discards all recorded geometries.
	 */
	public final Recorder NOP_INSTANCE = new Recorder() {
		@Override
		public void recordGeometry(RecordedGeometry cachedGeometry) {
		}
	};

	/**
	 * Receive a new recorded geometry.
	 *
	 * @param cachedGeometry
	 *            The geometry.
	 */
	void recordGeometry(RecordedGeometry cachedGeometry);
}
