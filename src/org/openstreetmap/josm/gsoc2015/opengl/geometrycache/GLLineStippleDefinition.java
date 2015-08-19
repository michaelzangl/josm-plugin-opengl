package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.BasicStroke;

import javax.media.opengl.GL2;

/**
 * This class defines a line style that can be drawn using gl line stipple.
 *
 * @author Michael Zangl
 *
 */
public class GLLineStippleDefinition {
	private final float width;
	/**
	 * Only values 1...256 are allowed.
	 */
	private final int factor;
	private final int pattern;

	/**
	 * Create a new stipple definition
	 *
	 * @param width
	 *            The parameter for {@link GL2#glLineWidth(float)}
	 * @param factor
	 *            The parameter for {@link GL2#glLineStipple(int, short)}
	 * @param pattern
	 *            The parameter for {@link GL2#glLineStipple(int, short)}
	 */
	public GLLineStippleDefinition(float width, int factor, int pattern) {
		super();
		this.width = width;
		this.factor = factor;
		this.pattern = pattern;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + factor;
		result = prime * result + pattern;
		result = prime * result + Float.floatToIntBits(width);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final GLLineStippleDefinition other = (GLLineStippleDefinition) obj;
		if (factor != other.factor) {
			return false;
		}
		if (pattern != other.pattern) {
			return false;
		}
		if (Float.floatToIntBits(width) != Float.floatToIntBits(other.width)) {
			return false;
		}
		return true;
	}

	/**
	 * Generates the best matching stipple definition for that stroke
	 *
	 * @param stroke
	 *            The stroke to convert
	 * @return The stipple definition or <code>null</code> if conversion was
	 *         not possible.
	 */
	public static GLLineStippleDefinition generate(BasicStroke stroke) {
		final float[] dashes = stroke.getDashArray();
		final float width = stroke.getLineWidth();
		if (width < .5 || width > 5) {
			// TODO: Query those values from OpenGL.
			return null;
		}
		int factor;
		int pattern;
		if (dashes == null) {
			// simple...
			factor = 1;
			pattern = 0xffff;
		} else {
			factor = findBestFactor(dashes);

			final float phase = stroke.getDashPhase();
			// Now fill using the dashes array.
			pattern = 0;
			for (int i = 0; i < 16; i++) {
				final boolean isDash = getInDashArray(dashes, phase + (i + .5f)
						* factor);
				// System.out.println("For " + i + " testing "
				// + (phase + (i + .5f) * factor) + " in "
				// + Arrays.toString(dashes) + " => " + isDash);
				if (isDash) {
					pattern |= 1 << i;
				}
			}
		}

		return new GLLineStippleDefinition(width, factor, pattern);
	}

	private static boolean getInDashArray(float[] dashes, float distance) {
		float distanceWalked = 0;
		boolean inDash = false;
		for (int i = 0; i < 100; i++) {
			for (final float d : dashes) {
				distanceWalked += d;
				inDash ^= true;
				if (distanceWalked >= distance) {
					return inDash;
				}
			}
		}
		return false;
	}

	private static int findBestFactor(float[] dashes) {
		// we attempt to find the best factor that fits our dash array.
		// the end should match as good as possible.
		final float dashesLength = sum(dashes);
		float bestDifference = Float.POSITIVE_INFINITY;
		int bestFactor = 1;
		for (int factor = 1; factor < 20; factor++) {
			final int length = factor * (1 << 16);
			final int dashesRepeated = Math.round(length / dashesLength);
			if (dashesRepeated == 0) {
				continue;
			}
			float difference = Math.abs((float) length / dashesRepeated
					- dashesLength);

			float distanceWalked = 0;
			for (int i = 0; i < dashes.length; i++) {
				distanceWalked += dashes[i];
				if (dashes[i] < factor) {
					// bad...
					difference += factor;
				} else {
					difference += distanceToNextInt(distanceWalked / factor);
				}
			}

			if (difference < bestDifference) {
				bestFactor = factor;
				bestDifference = difference;
			}
		}
		return bestFactor;
	}

	private static float distanceToNextInt(float f) {
		return Math.abs(f - Math.round(f));
	}

	private static float sum(float[] floats) {
		float sum = 0;
		for (final float f : floats) {
			sum += f;
		}
		return sum;
	}

	public void activate(GL2 gl) {
		if (pattern == 0xffff) {
			gl.glDisable(GL2.GL_LINE_STIPPLE);
		} else {
			gl.glEnable(GL2.GL_LINE_STIPPLE);
			gl.glLineStipple(factor, (short) pattern);
		}
		gl.glLineWidth(width);
	}
}