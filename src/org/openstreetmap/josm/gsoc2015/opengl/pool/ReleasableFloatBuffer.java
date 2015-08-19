package org.openstreetmap.josm.gsoc2015.opengl.pool;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;

public class ReleasableFloatBuffer implements Releasable {

	private final FloatBuffer buffer;

	public ReleasableFloatBuffer(FloatBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public FloatBuffer getBuffer() {
		return buffer;
	}

	@Override
	public void release() {
		// This is silently ignored. We have GC for this.
	}

	public static ReleasableFloatBuffer allocateDirect(int numFLoats) {
		return new ReleasableFloatBuffer(
				Buffers.newDirectFloatBuffer(numFLoats));
	}
}
