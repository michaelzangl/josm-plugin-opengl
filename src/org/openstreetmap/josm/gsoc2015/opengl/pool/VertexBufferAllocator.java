package org.openstreetmap.josm.gsoc2015.opengl.pool;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;

/**
 * This class allocates a new buffer on each request and lets the garbage
 * collector handle collecting.
 *
 * @author Michael Zangl
 *
 */
public class VertexBufferAllocator extends VertexBufferProvider {

	public static VertexBufferAllocator INSTANCE = new VertexBufferAllocator();

	public VertexBufferAllocator() {
		super();
	}

	@Override
	public ReleasableVertexBuffer getVertexBuffer(int minimumSize) {
		return new ReleasableVertexBuffer(minimumSize) {
			@Override
			protected void releaseBuffer(FloatBuffer buffer) {
			}

			@Override
			protected FloatBuffer allocateBuffer(int capacity) {
				return Buffers.newDirectFloatBuffer(capacity);
			}
		};
	}

	@Override
	public Releasable getBuffer(int numFloats) {
		return ReleasableFloatBuffer.allocateDirect(numFloats);
	}

}
