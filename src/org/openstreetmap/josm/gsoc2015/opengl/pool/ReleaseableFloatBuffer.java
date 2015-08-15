package org.openstreetmap.josm.gsoc2015.opengl.pool;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;

public class ReleaseableFloatBuffer implements Releaseable {

	private final FloatBuffer buffer;

	public ReleaseableFloatBuffer(FloatBuffer buffer) {
		this.buffer = buffer;
	}

	public FloatBuffer getBuffer() {
		return buffer;
	}

	public void release() {
		// This is silently ignored. We have GC for this.
	}

	public static ReleaseableFloatBuffer allocateDirect(int numFLoats) {
		return new ReleaseableFloatBuffer(
				Buffers.newDirectFloatBuffer(numFLoats));
	}
}
