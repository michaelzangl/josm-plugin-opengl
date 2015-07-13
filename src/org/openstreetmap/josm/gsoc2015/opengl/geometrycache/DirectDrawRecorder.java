package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import javax.media.opengl.GL2;

/**
 * This is a recorder that does not record the draw commands but displays them immediately instead.
 * @author michael
 *
 */
public class DirectDrawRecorder implements Recorder {
	private GL2 gl;

	public DirectDrawRecorder(GL2 gl) {
		this.gl = gl;
	}
	
	@Override
	public void recordGeometry(RecordedGeometry cachedGeometry) {
		System.out.println("Drawing geometry..." + cachedGeometry);
		cachedGeometry.draw(gl);
	}
}
