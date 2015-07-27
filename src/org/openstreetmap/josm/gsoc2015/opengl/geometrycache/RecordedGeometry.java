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
	private int vbo = -1;
	private final int color;
	private final int drawMode;
	private final TextureEntry texture;
	private final int points;

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

	public void draw(GL2 gl) {
		if (points == 0) {
			// nop
		} else if (vbo >= 0) {

		} else {
			int rgb = color;
			gl.glColor4ub((byte) (rgb >> 16 & 0xFF), (byte) (rgb >> 8 & 0xFF),
					(byte) (rgb & 0xFF), (byte) (rgb >> 24 & 0xFF));

			coordinates.rewind();

			int stride = (texture != null ? 4 : 2) * Buffers.SIZEOF_FLOAT;
			gl.glVertexPointer(2, GL.GL_FLOAT, stride,
					coordinates);
			gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);

			if (texture != null) {
			    gl.glTexEnvi(GL2ES1.GL_TEXTURE_ENV, GL2ES1.GL_TEXTURE_ENV_MODE, GL2ES1.GL_MODULATE);
			    gl.glTexParameterf(GL2ES1.GL_TEXTURE_ENV, GL2ES1.GL_TEXTURE_ENV_MODE, GL.GL_BLEND);
				float[] dst = new float[16];
				coordinates.get(dst);
				System.out.println("Coordinates: " + Arrays.toString(dst) + ", texture " + texture.getTexture(gl).getTextureObject(gl));
				
				texture.getTexture(gl).enable(gl);
				texture.getTexture(gl).bind(gl);
				coordinates.position(2);
				gl.glTexCoordPointer(2, GL.GL_FLOAT, stride, coordinates);
				gl.glEnableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
			}

			gl.glDrawArrays(drawMode, 0, points);

			if (texture != null) {
				texture.getTexture(gl).disable(gl);
				gl.glDisableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
			}

			gl.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
		}
	}

	@Override
	public String toString() {
		return "RecordedGeometry ["
				+ (vbo < 0 ? "coordinates=" + coordinates : "vbo=" + vbo)
				+ ", color=" + Integer.toHexString(color) + ", drawMode="
				+ drawMode + "]";
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
