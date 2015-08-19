package org.openstreetmap.josm.gsoc2015.opengl.pool;

import java.nio.FloatBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.jogamp.common.nio.Buffers;

/**
 * This is a fast and simple implementation of a vertex buffer pool.
 * @author Michael Zangl
 */
public class SimpleBufferPool extends VertexBufferProvider {

	private final ConcurrentLinkedQueue<FloatBuffer> shortBuffers = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<FloatBuffer> mediumBuffers = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<FloatBuffer> longBuffers = new ConcurrentLinkedQueue<>();

	public static final AtomicInteger miss = new AtomicInteger();
	public static final AtomicInteger hit = new AtomicInteger();

	@Override
	public ReleasableVertexBuffer getVertexBuffer(int minimumSize) {
		return get(minimumSize * 2);
	}

	private ConcurrentLinkedQueue<FloatBuffer> getBuffers(int size) {
		if (size <= 128) {
			return shortBuffers;
		} else if (size <= 2048) {
			return mediumBuffers;
		} else {
			return longBuffers;
		}
	}

	private ReleasableVertexBuffer get(int numFloats) {
		return new ReleasableVertexBuffer(numFloats) {
			@Override
			protected void releaseBuffer(FloatBuffer buffer) {
				final ConcurrentLinkedQueue<FloatBuffer> buffers = getBuffers(buffer.capacity());
				if (buffers.size() < 30) {
					// concurrency does not matter here.
					buffers.add(buffer);
				}
			}

			@Override
			protected FloatBuffer allocateBuffer(int capacity) {
				final ConcurrentLinkedQueue<FloatBuffer> buffers = getBuffers(capacity);
				final FloatBuffer candidate = buffers.poll();
				if (candidate != null && candidate.capacity() >= capacity) {
					candidate.limit(candidate.capacity());
					candidate.rewind();
					hit.incrementAndGet();
					return candidate;
				} else {
					miss.incrementAndGet();
					return Buffers.newDirectFloatBuffer(capacity);
				}
			}
		};
	}

	@Override
	public Releasable getBuffer(int numFloats) {
		return get(numFloats);
	}

}
