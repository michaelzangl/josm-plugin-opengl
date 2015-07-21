package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import javax.media.opengl.GL;

import org.jogamp.glg2d.impl.BasicStrokeLineVisitor;

public class RecordingStrokeLineVisitor extends BasicStrokeLineVisitor {

	private RecordingColorHelper colorHelper;
	private Recorder recorder;

	public RecordingStrokeLineVisitor(RecordingColorHelper colorHelper,
			Recorder recorder) {
		this.colorHelper = colorHelper;
		this.recorder = recorder;
	}

	@Override
	public void setGLContext(GL context) {
		// not used.
	}

	@Override
	protected void drawBuffer() {
		recorder.recordGeometry(new RecordedGeometry(GL.GL_TRIANGLE_STRIP,
				vBuffer, colorHelper.getActiveColor()));
	}

}
