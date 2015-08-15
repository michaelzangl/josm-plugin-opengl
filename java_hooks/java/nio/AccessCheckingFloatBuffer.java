package java.nio;

import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class AccessCheckingFloatBuffer extends HeapFloatBuffer {
	private final FloatBuffer buffer;
	private boolean wasReleased;
	
	public AccessCheckingFloatBuffer(FloatBuffer buffer) {
		super(null, 0, 0, 0, 0, 0);
		this.buffer = buffer;
	}

	public FloatBuffer slice() {
		checkAccess();
		return buffer.slice();
	}

	public FloatBuffer duplicate() {
		checkAccess();
		return buffer.duplicate();
	}

	public FloatBuffer asReadOnlyBuffer() {
		checkAccess();
		return buffer.asReadOnlyBuffer();
	}

	public float get() {
		checkAccess();
		return buffer.get();
	}

	public FloatBuffer put(float f) {
		checkAccess();
		return buffer.put(f);
	}

	public float get(int index) {
		checkAccess();
		return buffer.get(index);
	}

	public FloatBuffer put(int index, float f) {
		checkAccess();
		return buffer.put(index, f);
	}

	public FloatBuffer get(float[] dst, int offset, int length) {
		checkAccess();
		return buffer.get(dst, offset, length);
	}

	public boolean isReadOnly() {
		checkAccess();
		return buffer.isReadOnly();
	}

	public FloatBuffer get(float[] dst) {
		checkAccess();
		return buffer.get(dst);
	}

	public FloatBuffer put(FloatBuffer src) {
		checkAccess();
		return buffer.put(src);
	}

	public FloatBuffer put(float[] src, int offset, int length) {
		checkAccess();
		return buffer.put(src, offset, length);
	}

	public FloatBuffer compact() {
		checkAccess();
		return buffer.compact();
	}

	public boolean isDirect() {
		checkAccess();
		return buffer.isDirect();
	}

	public int hashCode() {
		checkAccess();
		return buffer.hashCode();
	}

	public boolean equals(Object ob) {
		checkAccess();
		return buffer.equals(ob);
	}

	public int compareTo(FloatBuffer that) {
		checkAccess();
		return buffer.compareTo(that);
	}

	public ByteOrder order() {
		checkAccess();
		return buffer.order();
	}

	protected void checkAccess() {
		if (wasReleased) {
			illegalAccess();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		if (!wasReleased) {
			notGarbageCollected();
		}
		super.finalize();
	}

	protected void notGarbageCollected() {
		System.err
				.println("This buffer was garbage collected but not released.");
	}

	protected void illegalAccess() {
		throw new IllegalStateException(
				"Buffer was used after it was released.");
	}

	public void markAsReleased() {
		checkAccess();
		wasReleased = true;
	}
}