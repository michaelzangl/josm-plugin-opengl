package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import javax.media.opengl.GL;

import org.jogamp.glg2d.impl.AbstractTesselatorVisitor;
import org.openstreetmap.josm.gsoc2015.opengl.pool.VertexBufferProvider;
import org.openstreetmap.josm.gsoc2015.opengl.pool.VertexBufferProvider.ReleasableVertexBuffer;

public class RecordingTesselatorVisitor extends AbstractTesselatorVisitor {
	private static final int DEFAULT_BUFFER_SIZE = 256;
	private final VertexBufferProvider VERTEX_BUFFER_PROVIDER = VertexBufferProvider.DEFAULT;
	private final RecordingColorHelper colorRecorder;
	private final Recorder recorder;

	public RecordingTesselatorVisitor(RecordingColorHelper colorHelper,
			Recorder recorder) {
		if (recorder == null) {
			throw new NullPointerException("recorder is null.");
		}
		colorRecorder = colorHelper;
		this.recorder = recorder;
		vBuffer = VERTEX_BUFFER_PROVIDER.getVertexBuffer(DEFAULT_BUFFER_SIZE);
	}

	@Override
	public void setGLContext(GL context) {
		// we don't perform any GL calls, so we don't need that context.
	}

	@Override
	protected void endTess() {
		recorder.recordGeometry(new RecordedGeometry(drawMode,
				(ReleasableVertexBuffer) vBuffer, colorRecorder
				.getActiveColor()));
		// For the next recording call.
		vBuffer = VERTEX_BUFFER_PROVIDER.getVertexBuffer(DEFAULT_BUFFER_SIZE);
	}

	public void dispose() {
		((ReleasableVertexBuffer) vBuffer).release();
	}

}
