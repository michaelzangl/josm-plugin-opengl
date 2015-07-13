package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

public interface Recorder {

	public final Recorder NOP_INSTANCE = new Recorder() {
		@Override
		public void recordGeometry(RecordedGeometry cachedGeometry) {
		}
	};

	void recordGeometry(RecordedGeometry cachedGeometry);
}
