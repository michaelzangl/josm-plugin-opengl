package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

/**
 * A recorder that passes record-calls on to an other recorder.
 *
 * @author michael
 *
 */
public class ProxyRecorder {

	private Recorder recorder = Recorder.NOP_INSTANCE;

	public void recordGeometry(RecordedGeometry cachedGeometry) {
		recorder.recordGeometry(cachedGeometry);
	}

	public Recorder getRecorder() {
		return recorder;
	}

	public void setRecorder(Recorder recorder) {
		this.recorder = recorder;
	}

}
