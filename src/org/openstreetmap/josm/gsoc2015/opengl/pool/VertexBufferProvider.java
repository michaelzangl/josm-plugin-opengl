package org.openstreetmap.josm.gsoc2015.opengl.pool;

import java.nio.FloatBuffer;

import org.jogamp.glg2d.VertexBuffer;

/**
 * This is a base class that provides new vertex buffers.
 *
 * @author Michael Zangl
 */
public abstract class VertexBufferProvider {
	/**
	 * This is the default pool you can use whenever you need some buffers.
	 */
	public static VertexBufferProvider DEFAULT = new SimpleBufferPool();

	/**
	 * This is a vertex buffer that can be released back to the pool
	 *
	 * @author Michael Zangl
	 */
	public abstract class ReleasableVertexBuffer extends VertexBuffer implements
	Releasable {
		protected ReleasableVertexBuffer(FloatBuffer buffer) {
			super(buffer);
		}

		public ReleasableVertexBuffer(int size) {
			this(null);
			buffer = allocateBuffer(size * 2);
		}

		@Override
		protected void ensureCapacity(int numNewFloats) {
			final int newSize = buffer.position() + numNewFloats;
			if (buffer.capacity() <= newSize) {
				final FloatBuffer larger = allocateBuffer(Math.max(
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

		/**
		 * Allocates a new buffer.
		 *
		 * @param capacity
		 *            The number of floats required.
		 * @return The buffer.
		 */
		protected abstract FloatBuffer allocateBuffer(int capacity);

		/**
		 * Releases a buffer
		 *
		 * @param buffer
		 *            The buffer that is not needed any more.
		 */
		protected abstract void releaseBuffer(FloatBuffer buffer);

	}

	/**
	 * Gets a vertex buffer.
	 *
	 * @param minimumSize
	 *            The minimum number of vertices it should initially hold.
	 * @return The vertex buffer
	 */
	public abstract ReleasableVertexBuffer getVertexBuffer(int minimumSize);

	/**
	 * Gets a new float buffer.
	 *
	 * @param numFloats
	 *            The number of floats that buffer needs to hold at least.
	 * @return The releasable float buffer.
	 */
	public abstract Releasable getBuffer(int numFloats);
}
