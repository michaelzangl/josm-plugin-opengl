package org.openstreetmap.josm.gsoc2015.opengl.pool;

import java.nio.FloatBuffer;

import org.jogamp.glg2d.VertexBuffer;

/**
 * This is a base class that provides new vertex buffers.
 * 
 * @author Michael Zangl
 */
public abstract class VertexBufferProvider {
	public static VertexBufferProvider DEFAULT = new SimpleBufferPool();
	
	public abstract class ReleaseableVertexBuffer extends VertexBuffer
			implements Releaseable {
		protected ReleaseableVertexBuffer(FloatBuffer buffer) {
			super(buffer);
		}

		public ReleaseableVertexBuffer(int size) {
			this(null);
			buffer = allocateBuffer(size * 2);
		}

		@Override
		protected void ensureCapacity(int numNewFloats) {
			int newSize = buffer.position() + numNewFloats;
			if (buffer.capacity() <= newSize) {
				FloatBuffer larger = allocateBuffer(Math.max(
						buffer.position() * 2, newSize));
				buffer.flip();
				larger.put(buffer);
				releaseBuffer(buffer);
				buffer = larger;
			}
		}

		@Override
		public FloatBuffer getBuffer() {
			if (buffer == null) {
				throw new IllegalStateException("already released");
			}
			return super.getBuffer();
		}

		@Override
		public void release() {
			if (buffer == null) {
				throw new IllegalStateException("already released");
			}
			releaseBuffer(buffer);
			buffer = null;
		}

		protected abstract FloatBuffer allocateBuffer(int capacity);

		protected abstract void releaseBuffer(FloatBuffer buffer);

	}

	public abstract ReleaseableVertexBuffer getVertexBuffer(int minimumSize);

	public abstract Releaseable getBuffer(int numFloats);
}
