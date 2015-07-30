package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.geom.Point2D;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.IdentityHashMap;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.fixedfunc.GLPointerFunc;

import org.jogamp.glg2d.VertexBuffer;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.TextureManager.TextureEntry;

import com.jogamp.common.nio.Buffers;

/**
 * This is a recorded geometry data. Each geometry consists of a direct
 * {@link FloatBuffer} that holds x/y coordinates and optionally U7V coordinates
 * for the texture.
 * 
 * @author michael
 *
 */
public class RecordedGeometry {
	private static IdentityHashMap<VertexBuffer, VertexBuffer> used = new IdentityHashMap<>();

	private FloatBuffer coordinates;
	private int points;
	private int vbo = -1;
	private final int color;
	private final int drawMode;
	private final TextureEntry texture;

	/**
	 * Creates a new recorded geometry.
	 * 
	 * @param drawMode
	 * @param coordinates
	 *            The coordinates array. The length is queried by using the
	 *            {@link Buffer#position()} method and computing the number of
	 *            vertices until then.
	 * @param color
	 * @param texture
	 */
	public RecordedGeometry(int drawMode, FloatBuffer coordinates, int color,
			TextureEntry texture) {
		if (!coordinates.isDirect()) {
			throw new IllegalArgumentException(
					"Can only handle direct float buffers.");
		}
		this.drawMode = drawMode;
		this.coordinates = coordinates;
		this.color = color;
		this.texture = texture;
		this.points = coordinates.position() / (texture != null ? 4 : 2);
	}

	public RecordedGeometry(int drawMode, VertexBuffer vBuffer, int color) {
		// For testing
		// if (used.containsKey(vBuffer)) {
		// throw new IllegalArgumentException("Got VBuffer twice.");
		// }
		// used.put(vBuffer, vBuffer);
		// Test code end
		this(drawMode, vBuffer.getBuffer(), color, null);
	}

	/**
	 * Disposes the underlying buffer and frees all allocated resources. The
	 * object may not be used afterwards.
	 * 
	 * @param gl
	 */
	public void dispose(GL2 gl) {

	}

	public void draw(GL2 gl, GLState state) {
		if (points == 0) {
			// nop
			return;
		}
		
		if (vbo < 0 && points > 50) {
			// convert to VBO
			
			int[] vboIds = new int[1];
			gl.glGenBuffers(1, vboIds,0 );
			vbo = vboIds[0];
			if (vbo == 0) {
				Main.warn("Could not generate VBO object.");
				vbo = -1;
			} else {
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo);
				coordinates.rewind();
				gl.glBufferData(GL2.GL_ARRAY_BUFFER, coordinates.remaining() * Buffers.SIZEOF_FLOAT, coordinates, GL.GL_STATIC_DRAW);
				// we don't need this any more
				coordinates = null;
			}
		}
		
		state.setColor(color);

		int stride = getBufferEntriesForPoint() * Buffers.SIZEOF_FLOAT;

		if (vbo >= 0) {
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo);
			gl.glVertexPointer(2, GL.GL_FLOAT, stride, 0);
		} else {
			coordinates.rewind();
			gl.glVertexPointer(2, GL.GL_FLOAT, stride, coordinates);
		}

		state.setTexCoordActive(texture != null);
		if (texture != null) {
			texture.getTexture(gl).enable(gl);
			texture.getTexture(gl).bind(gl);
			if (vbo >= 0) {
				gl.glTexCoordPointer(2, GL.GL_FLOAT, stride,
						2 * Buffers.SIZEOF_FLOAT);
			} else {
				coordinates.position(2);
				gl.glTexCoordPointer(2, GL.GL_FLOAT, stride, coordinates);
			}
		}

		gl.glDrawArrays(drawMode, 0, points);

		if (texture != null) {
			texture.getTexture(gl).disable(gl);
		}

		if (vbo >= 0) {
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
		}
	}

	private int getBufferEntriesForPoint() {
		return texture != null ? 4 : 2;
	}

	/**
	 * Check whether this draw command has no influence on the output.
	 * 
	 * @return <code>true</code> if there is no influence.
	 */
	public boolean isNop() {
		return points == 0;
	}

	/**
	 * Creates a hash that suggests which geometries could be combined.
	 * 
	 * @return The hash.
	 */
	public int getCombineHash() {
		final int prime = 31;
		int result = 1;
		result = prime * result + color;
		result = prime * result + drawMode;
		result = prime * result + ((texture == null) ? 0 : texture.hashCode());
		return result;
	}

	/**
	 * Checks if two geomtries could be combined. The result is symetric.
	 * @param other
	 * @return
	 */
	public boolean couldCombineWith(RecordedGeometry other) {
		return other.color == color && other.texture == texture
				&& other.drawMode == drawMode && hasMergeableDrawMode()
				&& !other.usesVBO() && !usesVBO();
	}

	private boolean hasMergeableDrawMode() {
		return drawMode == GL2.GL_QUADS || drawMode == GL2.GL_LINES
				|| drawMode == GL2.GL_TRIANGLES || drawMode == GL2.GL_POINTS;
	}

	public boolean attemptCombineWith(RecordedGeometry other) {
		if (!couldCombineWith(other)) {
			return false;
		}
		if (this == other) {
			throw new IllegalArgumentException("Cannot combine with myself.");
		}

		int newPoints = points + other.points;
		FloatBuffer newCoordinates;
		int entries = points * getBufferEntriesForPoint();
		int otherEntries = other.points * getBufferEntriesForPoint();

		// Create coordinates array if required.
		if (entries + otherEntries <= coordinates.capacity()) {
			newCoordinates = coordinates;
			newCoordinates.position(entries);
		} else {
			newCoordinates = Buffers.newDirectFloatBuffer(entries
					+ otherEntries);
			coordinates.rewind();
			transfer(coordinates, newCoordinates, entries);
		}

		other.coordinates.rewind();
		transfer(other.coordinates, newCoordinates, otherEntries);

		coordinates = newCoordinates;
		points = newPoints;
		return true;
	}

	private void transfer(FloatBuffer from, FloatBuffer to, int entries) {
		if (from.remaining() == entries) {
			to.put(from);
		} else {
			for (int i = 0; i < entries; i++) {
				to.put(from.get());
			}
		}
	}

	@Override
	public String toString() {
		return "RecordedGeometry ["
				+ (usesVBO() ? "vbo=" + vbo : "coordinates=" + coordinates)
				+ ", color=" + Integer.toHexString(color) + ", drawMode="
				+ drawMode + "]";
	}

	private boolean usesVBO() {
		return vbo >= 0;
	}

	public static RecordedGeometry generateTexture(TextureEntry texture,
			int color, Point2D sw, Point2D se, Point2D ne, Point2D nw,
			float relSx1, float relSy1, float relSx2, float relSy2) {
		float[] corners = new float[] { (float) sw.getX(), (float) sw.getY(),
				(float) se.getX(), (float) se.getY(), (float) ne.getX(),
				(float) ne.getY(), (float) nw.getX(), (float) nw.getY(), };
		return generateTexture(texture, color, corners, relSx1, relSy1, relSx2,
				relSy2);
	}

	public static RecordedGeometry generateTexture(TextureEntry texture,
			int color, float[] corners, float relSx1, float relSy1,
			float relSx2, float relSy2) {
		FloatBuffer cornerBuffer = Buffers.newDirectFloatBuffer(4 * 4);
		// SW
		cornerBuffer.put(corners, 0, 2);
		cornerBuffer.put(relSx1);
		cornerBuffer.put(relSy2);
		// SE
		cornerBuffer.put(corners, 2, 2);
		cornerBuffer.put(relSx2);
		cornerBuffer.put(relSy2);
		// NE
		cornerBuffer.put(corners, 4, 2);
		cornerBuffer.put(relSx2);
		cornerBuffer.put(relSy1);
		// NW
		cornerBuffer.put(corners, 6, 2);
		cornerBuffer.put(relSx1);
		cornerBuffer.put(relSy1);
		return new RecordedGeometry(GL2.GL_QUADS, cornerBuffer, color, texture);
	}
}
