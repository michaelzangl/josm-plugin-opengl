package org.openstreetmap.josm.gsoc2015.opengl.pool;

import java.nio.AccessCheckingFloatBuffer;
import java.nio.FloatBuffer;

import org.openstreetmap.josm.gsoc2015.opengl.util.DebugUtils;

import com.jogamp.common.nio.Buffers;

public class ErrorCheckingVertexBufferPool extends VertexBufferProvider {

	public static final ErrorCheckingVertexBufferPool INSTANCE = new ErrorCheckingVertexBufferPool();

	@Override
	public ReleaseableVertexBuffer getVertexBuffer(int minimumSize) {
		final String creationPoint = DebugUtils.getStackTrace();
		return new ReleaseableVertexBuffer(minimumSize) {
			@Override
			protected FloatBuffer allocateBuffer(int capacity) {
				FloatBuffer buffer = Buffers.newDirectFloatBuffer(capacity);
				return new AccessCheckingFloatBuffer(buffer) {
					private String lastAccess;

					@Override
					protected void illegalAccess() {
						System.err.println("Buffer created at: \n"
								+ creationPoint);
						System.err.println("Buffer last accessed at: \n"
								+ lastAccess);
						super.illegalAccess();
					};

					@Override
					protected void notGarbageCollected() {
						System.err.println("Buffer created at: \n"
								+ creationPoint);
						System.err.println("Buffer last accessed at: \n"
								+ lastAccess);
						super.notGarbageCollected();
					};

					@Override
					protected void checkAccess() {
						super.checkAccess();
						lastAccess = DebugUtils.getStackTrace();
					}
				};
			}

			@Override
			protected void releaseBuffer(FloatBuffer buffer) {
				((AccessCheckingFloatBuffer) buffer).markAsReleased();
			}
		};
	}

	@Override
	public Releaseable getBuffer(int numFloats) {
		return getVertexBuffer(numFloats / 2);
	}
}
