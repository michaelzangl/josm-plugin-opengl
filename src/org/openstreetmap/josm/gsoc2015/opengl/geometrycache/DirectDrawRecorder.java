package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import javax.media.opengl.GL2;

import org.openstreetmap.josm.gsoc2015.opengl.osm.ViewPosition;

/**
 * This is a recorder that does not record the draw commands but displays them immediately instead.
 * @author michael
 *
 */
public class DirectDrawRecorder implements Recorder {
	private final GL2 gl;
	private final ViewPosition viewPosition;

	public DirectDrawRecorder(GL2 gl, ViewPosition viewPosition) {
		this.gl = gl;
		this.viewPosition = viewPosition;
	}

	@Override
	public void recordGeometry(RecordedGeometry cachedGeometry) {
		// System.out.println("Drawing geometry..." + cachedGeometry);
		final GLState state = new GLState(gl);
		state.initialize(viewPosition);
		cachedGeometry.draw(gl, state);
		state.done();
	}
}
