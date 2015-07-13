package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import javax.media.opengl.GL;

import org.jogamp.glg2d.impl.AbstractTesselatorVisitor;

public class RecordingTesselatorVisitor extends AbstractTesselatorVisitor {
	private RecordingColorHelper colorRecorder;
	private Recorder recorder;

	public RecordingTesselatorVisitor(RecordingColorHelper colorHelper, Recorder recorder) {
		if (recorder == null) {
			throw new NullPointerException("recorder is null.");
		}
		this.colorRecorder = colorHelper;
		this.recorder = recorder;
	}
	
	@Override
	public void setGLContext(GL context) {
		// we don't perform any GL calls, so we don't need that context.
	}

	@Override
	protected void endTess() {
		recorder.recordGeometry(new RecordedGeometry(drawMode, vBuffer, colorRecorder.getActiveColor()));
	}

}
