package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.geom.Point2D;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.IdentityHashMap;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

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
	/**
	 * Up to how many triangle fan points is conversion to a triangle strip
	 * good?
	 */
	private static final int MAX_CONVERT_POINTS = 50;

	private static IdentityHashMap<VertexBuffer, VertexBuffer> used = new IdentityHashMap<>();

	protected FloatBuffer coordinates;
	protected int points;
	private int vbo = -1;
	protected final int color;
	protected int drawMode;
	protected final TextureEntry texture;

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
	private RecordedGeometry(int drawMode, FloatBuffer coordinates, int color,
			TextureEntry texture) {
		if (!coordinates.isDirect()) {
			throw new IllegalArgumentException(
					"Can only handle direct float buffers.");
		}
		this.drawMode = drawMode;
		this.coordinates = coordinates;
		this.color = color;
		this.texture = texture;
		this.points = coordinates.position() / getBufferEntriesForPoint();
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
	public synchronized void dispose(GL2 gl) {
		// TODO: Do this, but delay deletion.
		
		// This is to avoid more draw calls.
		points = 0;
	}

	public synchronized void draw(GL2 gl, GLState state) {
		if (points == 0) {
			// nop
			return;
		}

		if (vbo < 0 && points > 50) {
			// convert to VBO

			int[] vboIds = new int[1];
			gl.glGenBuffers(1, vboIds, 0);
			vbo = vboIds[0];
			if (vbo == 0) {
				Main.warn("Could not generate VBO object.");
				vbo = -1;
			} else {
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo);
				coordinates.rewind();
				gl.glBufferData(GL2.GL_ARRAY_BUFFER, coordinates.remaining()
						* Buffers.SIZEOF_FLOAT, coordinates, GL.GL_STATIC_DRAW);
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

		DrawStats.drawArrays(drawMode, points);
		gl.glDrawArrays(drawMode, 0, points);
//		for (int i = points; drawMode == GL.GL_TRIANGLES && i > 0 && points < 20 * 3; i -=3) {
//			gl.glDrawArrays(drawMode, 0, i);
//		}
		//gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, points);

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
		result = prime * result + getBestCombineDrawMode();
		result = prime * result + ((texture == null) ? 0 : texture.hashCode());
		return result;
	}

	/**
	 * Get the draw mode that should be used when combing this buffer. In some
	 * draw modes, putting multiple geometries in one buffer is not possible.
	 * Those are converted to a geometry that is suited for combining.
	 * 
	 * @return The draw mode to use after combining geometries.
	 */
	private int getBestCombineDrawMode() {
		if (shouldConvertToTriangles()) {
			return GL2.GL_TRIANGLES;
		}
		return drawMode;
	}

	private boolean shouldConvertToTriangles() {
		return (drawMode == GL2.GL_TRIANGLE_STRIP || drawMode == GL2.GL_TRIANGLE_FAN)
				&& points < MAX_CONVERT_POINTS;
	}

	/**
	 * Checks if two geomtries could be combined. The result is symetric.
	 * 
	 * @param other
	 * @return
	 */
	public synchronized boolean couldCombineWith(RecordedGeometry other) {
		return other.color == color && other.texture == texture
				&& other.getBestCombineDrawMode() == getBestCombineDrawMode()
				&& hasMergeableDrawMode() && !other.usesVBO() && !usesVBO();
	}

	private boolean hasMergeableDrawMode() {
		int drawMode = getBestCombineDrawMode();
		return drawMode == GL2.GL_QUADS || drawMode == GL2.GL_LINES
				|| drawMode == GL2.GL_TRIANGLES || drawMode == GL2.GL_POINTS;
	}

	public synchronized boolean attemptCombineWith(RecordedGeometry other) {
		if (!couldCombineWith(other)) {
			return false;
		}
		if (this == other) {
			throw new IllegalArgumentException("Cannot combine with myself.");
		}

		System.out.println("Combine " + drawMode + " with " + other.drawMode);
		int newPoints = getPointsAfterConversion()
				+ other.getPointsAfterConversion();
		FloatBuffer newCoordinates;

		// Create coordinates array if required.
		int newBufferSize = newPoints * getBufferEntriesForPoint();
		if (newBufferSize <= coordinates.capacity()) {
			newCoordinates = coordinates;
			coordinates.rewind();
		} else {
			newCoordinates = Buffers.newDirectFloatBuffer(newBufferSize);
		}
		transferToBuffer(newCoordinates);

		other.transferToBuffer(newCoordinates);

		coordinates = newCoordinates;
		points = newPoints;
		drawMode = getBestCombineDrawMode();
		return true;
	}

	private int getPointsAfterConversion() {
		if (shouldConvertToTriangles()) {
			return (points - 2) * 3;
		} else {
			return points;
		}
	}

	/**
	 * Transfers my coordinates to the given buffer, converting if needed.
	 * <p>
	 * Positions the cursor at the end of the data.
	 * 
	 * @param to
	 * @return
	 */
	private void transferToBuffer(FloatBuffer to) {
		coordinates.rewind();
		if (shouldConvertToTriangles()) {
			transferAndConvertToTriangles(coordinates, to, points, drawMode, getBufferEntriesForPoint());
		} else {
			int entries = points * getBufferEntriesForPoint();
			transfer(coordinates, to, entries);
		}
	}

	/**
	 * Transfers between two buffers converting them between draw modes on the
	 * fly.
	 * <p>
	 * For a triangle fan, we convert to triangles with the points (1,2,3),
	 * (1,3,4), (1,4,5), ...
	 * <p>
	 * For a triangle strip, we convert to triangles with the points (1,2,3),
	 * (3,2,4), (3,4,5) ...
	 * 
	 * @param from
	 *            The buffer to take from.
	 * @param to
	 *            A buffer to write to. May be the same as the buffer to take
	 *            from but then it should be at the same position. Needs at
	 *            least (oldEntries - 2) * 3 places.
	 * @param oldPoints
	 *            Old number of stored points.
	 * @param oldDrawMode
	 */
	private static void transferAndConvertToTriangles(FloatBuffer from,
			FloatBuffer to, int oldPoints, int oldDrawMode, int pointSize) {
		int triangles = (oldPoints - 2);
		int fromOffset = from.position();
		int toOffset = to.position();
		System.out.println("Converting " + triangles + " triangles: from@" + fromOffset + " of " + from.capacity() + ", to@"+toOffset + " of " + to.capacity());

		if (oldDrawMode == GL2.GL_TRIANGLE_STRIP) {
		} else if (oldDrawMode == GL2.GL_TRIANGLE_FAN) {
		} else {
			throw new IllegalArgumentException("Cannot convert to triangles: "
					+ oldDrawMode);
		}
		
		FloatBuffer toPut = null;
		if (to == from) {
			toPut = to;
			to = Buffers.newDirectFloatBuffer(triangles * 3 * pointSize);
		}
		FloatBuffer fromCopy = from.duplicate();

		// Go backward to support in-buffer conversion.
		for (int triangle = 0; triangle < triangles; triangle++) {
			int vertex1, vertex2;
			if (oldDrawMode == GL2.GL_TRIANGLE_STRIP) {
				int odd = triangle % 2; // 0 or 1
				// We could reduce number of gets here by storing the result.
				vertex1 = triangle + odd;
				vertex2 = triangle + 1 - odd;
			} else {
				vertex1 = 0;
				vertex2 = triangle + 1;
			}
			int vertex3 = triangle + 2;
			selectVertexAndPut(fromCopy, fromOffset, vertex1, pointSize, to);
			selectVertexAndPut(fromCopy, fromOffset, vertex2, pointSize, to);
			selectVertexAndPut(fromCopy, fromOffset, vertex3, pointSize, to);
		}
		
		if (toPut != null) {
			to.rewind();
			toPut.put(to);
		}
	}

	private static void selectVertexAndPut(FloatBuffer fromCopy,
			int fromOffset, int vertexN, int pointSize, FloatBuffer to) {
		selectVertex(fromCopy, fromOffset, vertexN, pointSize);
		to.put(fromCopy);
	}

	private static void selectVertex(FloatBuffer fromCopy, int fromOffset,
			int vertexN, int pointSize) {
		try {
			fromCopy.limit(fromOffset + (vertexN + 1) * pointSize);
			fromCopy.position(fromOffset + vertexN * pointSize);
		} catch (IllegalArgumentException e) {
			System.err.println("Position: " + (fromOffset + vertexN * pointSize));
			throw e;
		}
	}


	private static void transfer(FloatBuffer from, FloatBuffer to, int entries) {
		if (from == to) {
			from.position(from.position() + entries);
		} else if (from.remaining() == entries) {
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
