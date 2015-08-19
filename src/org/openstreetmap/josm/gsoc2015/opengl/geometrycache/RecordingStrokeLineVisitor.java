package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import javax.media.opengl.GL;

import org.jogamp.glg2d.impl.BasicStrokeLineVisitor;
import org.openstreetmap.josm.gsoc2015.opengl.pool.VertexBufferProvider;
import org.openstreetmap.josm.gsoc2015.opengl.pool.VertexBufferProvider.ReleasableVertexBuffer;

public class RecordingStrokeLineVisitor extends BasicStrokeLineVisitor {

	private static final VertexBufferProvider VERTEX_BUFFER_PROVIDER = VertexBufferProvider.DEFAULT;
	private static final int DEFAULT_BUFFER_SIZE = 256;
	private final RecordingColorHelper colorHelper;
	private final Recorder recorder;

	public RecordingStrokeLineVisitor(RecordingColorHelper colorHelper,
			Recorder recorder) {
		this.colorHelper = colorHelper;
		this.recorder = recorder;
		vBuffer = VERTEX_BUFFER_PROVIDER.getVertexBuffer(DEFAULT_BUFFER_SIZE);
	}

	@Override
	public void setGLContext(GL context) {
		// not used.
	}

	@Override
	protected void drawBuffer() {
		recorder.recordGeometry(new RecordedGeometry(GL.GL_TRIANGLE_STRIP,
				(ReleasableVertexBuffer) vBuffer, colorHelper.getActiveColor()));
		// For the next run.
		vBuffer = VERTEX_BUFFER_PROVIDER.getVertexBuffer(DEFAULT_BUFFER_SIZE);
	}

	public void dispose() {
		((ReleasableVertexBuffer) vBuffer).release();
		vBuffer = null;
	}
}
