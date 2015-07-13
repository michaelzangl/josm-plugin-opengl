package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;


import javax.media.opengl.GL2;

import org.jogamp.glg2d.VertexBuffer;

public class RecordedGeometry {
	private VertexBuffer vBuffer;
	private int vbo = -1;
	private int color;
	private int drawMode;

	public RecordedGeometry(int drawMode, VertexBuffer vBuffer, int color) {
		this.drawMode = drawMode;
		this.vBuffer = vBuffer;
		this.color = color;
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
		if (vbo >= 0) {

		} else {
			int rgb = color;
			gl.glColor4ub((byte) (rgb >> 16 & 0xFF), (byte) (rgb >> 8 & 0xFF),
					(byte) (rgb & 0xFF), (byte) (rgb >> 24 & 0xFF));
			vBuffer.drawBuffer(gl, drawMode);
		}
	}

	@Override
	public String toString() {
		return "RecordedGeometry ["
				+ (vbo < 0 ? "vBuffer=" + vBuffer : "vbo=" + vbo) + ", color="
				+ Integer.toHexString(color) + ", drawMode=" + drawMode + "]";
	}

}
