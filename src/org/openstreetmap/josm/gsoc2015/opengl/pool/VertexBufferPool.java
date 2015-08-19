package org.openstreetmap.josm.gsoc2015.opengl.pool;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gsoc2015.opengl.util.DebugUtils;

import com.jogamp.common.nio.Buffers;

/**
 * This is a more complex vertex buffer pool implementation. It did not yield
 * the expected results, so it is not used.
 *
 * @see SimpleBufferPool
 *
 * @author Michael Zangl
 *
 */
public class VertexBufferPool extends VertexBufferProvider {
	public class PooledVertexBuffer extends ReleasableVertexBuffer {
		protected PooledVertexBuffer(int capacity) {
			super(capacity);
		}

		protected PooledVertexBuffer(FloatBuffer buffer) {
			super(buffer);
		}

		@Override
		public String toString() {
			return "PooledVertexBuffer [pool=" + VertexBufferPool.this + "]";
		}

		@Override
		protected FloatBuffer allocateBuffer(int capacity) {
			return getBufferImpl(capacity);
		}

		@Override
		protected void releaseBuffer(FloatBuffer buffer) {
			markAsReleased(buffer);
		}
	}

	protected class PooledFloatBuffer implements Releasable {
		private FloatBuffer buffer;

		public PooledFloatBuffer(FloatBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public FloatBuffer getBuffer() {
			return buffer;
		}

		@Override
		public void release() {
			if (buffer == null) {
				throw new IllegalStateException();
			}
			markAsReleased(buffer);
			buffer = null;
		}

		@Override
		protected void finalize() throws Throwable {
			if (buffer != null) {
				Main.warn("Leaking float buffer.");
			}
			super.finalize();
		}
	}

	private static final int MAX_KEEP = 32;
	public static final VertexBufferPool DEFAULT_POOL = new VertexBufferPool(
			MAX_KEEP);

	private static final int MAX_TAKEBACK_SIZE = 8 * 1024;

	private static final float MIN_LOAD_FACTOR = .5f;

	public int buffersTooBig = 0;
	public int buffersReused = 0;
	public int buffersCreated = 0;
	public int buffersForReuse = 0;
	public int buffersReplaced = 0;

	private final IdentityHashMap<FloatBuffer, String> addedBy = new IdentityHashMap<>();
	private final IdentityHashMap<FloatBuffer, String> receivedBy = new IdentityHashMap<>();

	private final ArrayList<FloatBuffer> available = new ArrayList<>();

	private final Object availableMutex = new Object();

	private int clockCounter = 0;

	private final int maxKeep;

	public VertexBufferPool(int maxKeep) {
		this.maxKeep = maxKeep;
	}

	@Override
	public PooledVertexBuffer getVertexBuffer(int minimumSize) {
		return new PooledVertexBuffer(getBufferImpl(minimumSize * 2));
	}

	@Override
	public Releasable getBuffer(int numFloats) {
		if (numFloats <= MAX_TAKEBACK_SIZE) {
			return new PooledFloatBuffer(getBufferImpl(numFloats));
		} else {
			return ReleasableFloatBuffer.allocateDirect(numFloats);
		}
	}

	public boolean hasSize(FloatBuffer buffer, int minimumPoints) {
		return buffer.capacity() / 2 >= minimumPoints;
	}

	protected void markAsReleased(FloatBuffer buffer) {
		if (hasSize(buffer, MAX_TAKEBACK_SIZE + 1)) {
			// don't take back too big buffers.
			// System.out.println("Too big to take: " + buffer.capacity());
			buffersTooBig++;
			return;
		}
		// reset before adding to pool.
		buffer.rewind();
		buffer.limit(buffer.capacity());
		synchronized (availableMutex) {
			addedBy.put(buffer, DebugUtils.getStackTrace());
			if (maxKeep == 0) {
				// nop
			} else if (available.size() < maxKeep) {
				available.add(buffer);
				buffersForReuse++;
			} else {
				available.set(clockCounter, buffer);
				clockCounter++;
				if (clockCounter >= maxKeep) {
					clockCounter = 0;
				}
				buffersReplaced++;
			}
		}

	}

	private FloatBuffer getBufferImpl(int numFloats) {
		synchronized (availableMutex) {
			final int size = available.size();
			int bestSizeDiff = (int) (numFloats / MIN_LOAD_FACTOR);
			int bestSizeAt = -1;
			for (int i = 0; i < size; i++) {
				final FloatBuffer candidate = available.get(i);
				final int sizeDiff = candidate.capacity() - numFloats;
				if (sizeDiff >= 0 && sizeDiff < bestSizeDiff) {
					bestSizeDiff = sizeDiff;
					bestSizeAt = i;
				}
			}
			if (bestSizeAt >= 0) {
				final FloatBuffer best = available.get(bestSizeAt);
				if (bestSizeAt < size - 1) {
					available.set(bestSizeAt, available.remove(size - 1));
				}
				buffersReused++;
				if (best.position() != 0) {
					throw new IllegalStateException(
							"The buffer was used after adding it to the free pool.");
				}
				receivedBy.put(best, DebugUtils.getStackTrace());
				return best;
			}
			buffersCreated++;
		}

		return Buffers.newDirectFloatBuffer(numFloats);
	}
}
