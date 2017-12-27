package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.geom.Point2D;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.IdentityHashMap;
import java.util.Objects;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;

import org.jogamp.glg2d.VertexBuffer;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.TextureManager.TextureEntry;
import org.openstreetmap.josm.gsoc2015.opengl.pool.Releasable;
import org.openstreetmap.josm.gsoc2015.opengl.pool.VertexBufferProvider;
import org.openstreetmap.josm.gsoc2015.opengl.pool.VertexBufferProvider.ReleasableVertexBuffer;
import org.openstreetmap.josm.tools.Logging;

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

	private static final int MIN_VBO_POINTS = 2000;

	private static final VertexBufferProvider VERTEX_BUFFER_PROVIDER = VertexBufferProvider.DEFAULT;

	private static IdentityHashMap<VertexBuffer, VertexBuffer> used = new IdentityHashMap<>();

	protected Releasable coordinates;
	protected int points;
	private int vbo = -1;
	protected final int color;
	protected int drawMode;
	protected final TextureEntry texture;

	private final GLLineStippleDefinition lineStripple;

	private GeometryDisposer vboDisposer;

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
	private RecordedGeometry(int drawMode, Releasable coordinates, int color,
			TextureEntry texture, GLLineStippleDefinition lineStripple) {
		if (!coordinates.getBuffer().isDirect()) {
			throw new IllegalArgumentException(
					"Can only handle direct float buffers.");
		}
		if (lineStripple != null && drawMode != GL.GL_LINE_LOOP
				&& drawMode != GL.GL_LINE_STRIP && drawMode != GL.GL_LINES) {
			throw new IllegalArgumentException(
					"line stripple given in wrong draw mode.");
		}
		this.drawMode = drawMode;
		this.coordinates = coordinates;
		this.color = color;
		this.texture = texture;
		points = coordinates.getBuffer().position()
				/ getBufferEntriesForPoint();
		this.lineStripple = lineStripple;
	}

	public RecordedGeometry(int drawMode, Releasable vBuffer, int color) {
		this(drawMode, vBuffer, color, null, null);
	}

	/**
	 * Disposes the underlying buffer and frees all allocated resources. The
	 * object may not be used afterwards.
	 *
	 * @param gl
	 */
	public synchronized void dispose() {
		if (vbo >= 0) {
			vboDisposer.disposeVBO(vbo);
		}

		if (coordinates != null) {
			coordinates.release();
			coordinates = null;
		}
		// This is to avoid more draw calls.
		points = 0;
	}

	@Override
	protected void finalize() throws Throwable {
		if (points > 0) {
			Logging.warn("RecordedGeometry was finalized but is missing dispose()");
		}
	};

	public synchronized void draw(GL2 gl, GLState state) {
		if (points == 0) {
			// nop
			return;
		}

		if (vbo < 0 && points > MIN_VBO_POINTS) {
			final FloatBuffer buffer = coordinates.getBuffer();
			// convert to VBO

			final int[] vboIds = new int[1];
			gl.glGenBuffers(1, vboIds, 0);
			vbo = vboIds[0];
			if (vbo == 0) {
				Logging.warn("Could not generate VBO object.");
				vbo = -1;
			} else {
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
				buffer.rewind();
				gl.glBufferData(GL.GL_ARRAY_BUFFER, buffer.remaining()
						* Buffers.SIZEOF_FLOAT, buffer, GL.GL_STATIC_DRAW);
				// we don't need this any more
				coordinates.release();
				coordinates = null;
				vboDisposer = state;
			}
		}

		state.setColor(color);
		state.setLineStripple(lineStripple);

		final int stride = getBufferEntriesForPoint() * Buffers.SIZEOF_FLOAT;
		FloatBuffer buffer = null;
		if (vbo >= 0) {
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
			gl.glVertexPointer(2, GL.GL_FLOAT, stride, 0);
		} else {
			buffer = coordinates.getBuffer();
			buffer.rewind();
			gl.glVertexPointer(2, GL.GL_FLOAT, stride, buffer);
		}

		state.setTexCoordActive(texture != null);
		if (texture != null) {
			texture.getTexture(gl).enable(gl);
			texture.getTexture(gl).bind(gl);
			if (vbo >= 0) {
				gl.glTexCoordPointer(2, GL.GL_FLOAT, stride,
						2 * Buffers.SIZEOF_FLOAT);
			} else {
				buffer.position(2);
				gl.glTexCoordPointer(2, GL.GL_FLOAT, stride, buffer);
			}
		}

		DrawStats.drawArrays(drawMode, points);
		gl.glDrawArrays(drawMode, 0, points);
		// for (int i = points; drawMode == GL.GL_TRIANGLES && i > 0 && points <
		// 20 * 3; i -=3) {
		// gl.glDrawArrays(drawMode, 0, i);
		// }
		// gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, points);

		if (texture != null) {
			texture.getTexture(gl).disable(gl);
		}

		if (vbo >= 0) {
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
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
		result = prime * result + (texture == null ? 0 : texture.hashCode());
		result = prime * result + (lineStripple == null ? 0 : lineStripple.hashCode());
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
			return GL.GL_TRIANGLES;
		} else if (shouldConvertToLines()) {
			return GL.GL_LINES;
		}
		return drawMode;
	}

	private boolean shouldConvertToTriangles() {
		return (drawMode == GL.GL_TRIANGLE_STRIP || drawMode == GL.GL_TRIANGLE_FAN)
				&& points < MAX_CONVERT_POINTS;
	}

	private boolean shouldConvertToLines() {
		return (drawMode == GL.GL_LINE_STRIP || drawMode == GL.GL_LINE_LOOP)
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
				&& Objects.equals(lineStripple, other.lineStripple)
				&& hasMergeableDrawMode() && !other.usesVBO() && !usesVBO();
	}

	private boolean hasMergeableDrawMode() {
		final int drawMode = getBestCombineDrawMode();
		return drawMode == GL2GL3.GL_QUADS || drawMode == GL.GL_LINES
				|| drawMode == GL.GL_TRIANGLES || drawMode == GL.GL_POINTS;
	}

	/**
	 * Attempts to merge two recorded geometries.
	 * <p>
	 * If the merge is successful, drawing this geometry has the same effect as
	 * a drwa of this and the odert geometry would have before the merge. The
	 * other geometry is automatically disposed.
	 *
	 * @param other
	 * @return
	 */
	public synchronized boolean attemptCombineWith(RecordedGeometry other) {
		if (!couldCombineWith(other)) {
			return false;
		}
		if (this == other) {
			throw new IllegalArgumentException("Cannot combine with myself.");
		}

		final int newPoints = getPointsAfterConversion()
				+ other.getPointsAfterConversion();
		Releasable newCoordinates;

		final int newDrawMode = getBestCombineDrawMode();

		// Create coordinates array if required.
		int newBufferSize = newPoints * getBufferEntriesForPoint();
		if (newBufferSize <= coordinates.getBuffer().capacity()) {
			newCoordinates = coordinates;
			coordinates.getBuffer().rewind();
		} else {
			newBufferSize = Math.max(newBufferSize, coordinates.getBuffer()
					.capacity() * 3 / 2);
			newCoordinates = VERTEX_BUFFER_PROVIDER
					.getBuffer(newBufferSize);
		}

		transferToBuffer(newCoordinates.getBuffer());
		other.transferToBuffer(newCoordinates.getBuffer());

		other.dispose();
		if (newCoordinates != coordinates) {
			coordinates.release();
			coordinates = newCoordinates;
		}
		points = newPoints;
		drawMode = newDrawMode;

		return true;
	}

	private int getPointsAfterConversion() {
		if (shouldConvertToTriangles()) {
			return (points - 2) * 3;
		} else if (shouldConvertToLines()) {
			return getLinesPointCount(points, drawMode);
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
		final FloatBuffer coordBuffer = coordinates.getBuffer();
		coordBuffer.rewind();
		if (shouldConvertToTriangles()) {
			transferAndConvertToTriangles(coordBuffer, to, points, drawMode,
					getBufferEntriesForPoint());
		} else if (shouldConvertToLines()) {
			transferAndConvertToLines(coordBuffer, to, points, drawMode,
					getBufferEntriesForPoint());
		} else {
			final int entries = points * getBufferEntriesForPoint();
			transfer(coordBuffer, to, entries);
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
		final int triangles = oldPoints - 2;
		final int fromOffset = from.position();
		// int toOffset = to.position();
		// System.out.println("Converting " + triangles + " triangles: from@"
		// + fromOffset + " of " + from.capacity() + ", to@" + toOffset
		// + " of " + to.capacity());

		if (oldDrawMode == GL.GL_TRIANGLE_STRIP) {
		} else if (oldDrawMode == GL.GL_TRIANGLE_FAN) {
		} else {
			throw new IllegalArgumentException("Cannot convert to triangles: "
					+ oldDrawMode);
		}

		FloatBuffer toPut = null;
		if (to == from) {
			toPut = to;
			to = Buffers.newDirectFloatBuffer(triangles * 3 * pointSize);
		}
		final FloatBuffer fromCopy = from.duplicate();

		// Go backward to support in-buffer conversion.
		for (int triangle = 0; triangle < triangles; triangle++) {
			int vertex1, vertex2;
			if (oldDrawMode == GL.GL_TRIANGLE_STRIP) {
				final int odd = triangle % 2; // 0 or 1
				// We could reduce number of gets here by storing the result.
				vertex1 = triangle + odd;
				vertex2 = triangle + 1 - odd;
			} else {
				vertex1 = 0;
				vertex2 = triangle + 1;
			}
			final int vertex3 = triangle + 2;
			selectVertexAndPut(fromCopy, fromOffset, vertex1, pointSize, to);
			selectVertexAndPut(fromCopy, fromOffset, vertex2, pointSize, to);
			selectVertexAndPut(fromCopy, fromOffset, vertex3, pointSize, to);
		}

		if (toPut != null) {
			to.rewind();
			toPut.put(to);
		}
	}

	private static void transferAndConvertToLines(FloatBuffer from,
			FloatBuffer to, int oldPoints, int oldDrawMode, int pointSize) {
		final int linesPoints = getLinesPointCount(oldPoints, oldDrawMode);
		final int fromOffset = from.position();
		// int toOffset = to.position();
		// System.out.println("Converting " + (linesPoints / 2) +
		// " lines: from@"
		// + fromOffset + " of " + from.capacity() + ", to@" + toOffset
		// + " of " + to.capacity());
		FloatBuffer toPut = null;
		if (to == from) {
			toPut = to;
			to = Buffers.newDirectFloatBuffer(linesPoints * pointSize);
		}
		final FloatBuffer fromCopy = from.duplicate();

		// Go backward to support in-buffer conversion.
		for (int point = 0; point < linesPoints / 2; point++) {
			int vertex1, vertex2;
			vertex1 = point;
			vertex2 = (point + 1) % oldPoints;
			selectVertexAndPut(fromCopy, fromOffset, vertex1, pointSize, to);
			selectVertexAndPut(fromCopy, fromOffset, vertex2, pointSize, to);
		}

		if (toPut != null) {
			to.rewind();
			toPut.put(to);
		}
	}

	private static int getLinesPointCount(int oldPoints, int oldDrawMode) {
		int lineSegments;
		if (oldDrawMode == GL.GL_LINE_STRIP) {
			lineSegments = (oldPoints - 1) * 2;
		} else if (oldDrawMode == GL.GL_LINE_LOOP) {
			lineSegments = oldPoints * 2;
		} else {
			throw new IllegalArgumentException("Cannot convert to triangles: "
					+ oldDrawMode);
		}
		return lineSegments;
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
		} catch (final IllegalArgumentException e) {
			System.err.println("Position: "
					+ (fromOffset + vertexN * pointSize));
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
		final float[] corners = new float[] { (float) sw.getX(), (float) sw.getY(),
				(float) se.getX(), (float) se.getY(), (float) ne.getX(),
				(float) ne.getY(), (float) nw.getX(), (float) nw.getY(), };
		return generateTexture(texture, color, corners, relSx1, relSy1, relSx2,
				relSy2);
	}

	public static RecordedGeometry generateTexture(TextureEntry texture,
			int color, float[] corners, float relSx1, float relSy1,
			float relSx2, float relSy2) {
		final Releasable releasable = VERTEX_BUFFER_PROVIDER.getBuffer(4 * 4);
		final FloatBuffer cornerBuffer = releasable.getBuffer();
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
		return new RecordedGeometry(GL2GL3.GL_QUADS, releasable, color, texture,
				null);
	}

	public static RecordedGeometry generateLine(
			GLLineStippleDefinition stripple, int color,
			ReleasableVertexBuffer floatBuffer, boolean closedLine) {
		int mode;
		if (floatBuffer.getBuffer().position() == 4) {
			mode = GL.GL_LINES;
		} else {
			mode = closedLine ? GL.GL_LINE_LOOP : GL.GL_LINE_STRIP;
		}
		return new RecordedGeometry(mode, floatBuffer, color, null, stripple);
	}
}
