package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.Color;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.fixedfunc.GLPointerFunc;

import org.openstreetmap.josm.gsoc2015.opengl.osm.ViewPosition;

import com.jogamp.common.nio.Buffers;

/**
 * This is a state cache for OpenGL. All color changes need to be passed through
 * here.
 * 
 * @author michael
 *
 */
public class GLState {

	private GL2 gl;

	public GLState(GL2 gl) {
		this.gl = gl;
	}

	private int activeColor;
	private boolean texCoordActive;
	private ViewPosition currentViewPosition;
	private FloatBuffer oldModelview = Buffers.newDirectFloatBuffer(16);
	private GLLineStrippleDefinition oldLineStripple;

	public void initialize(ViewPosition viewPosition) {
		ckeckGLError(false);
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, oldModelview);
		ckeckGLError(true);

		currentViewPosition = viewPosition;
		activeColor = Color.white.getRGB();
		setColorImpl(activeColor);
		ckeckGLError(true);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
		ckeckGLError(true);

		// always active.
		gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
		ckeckGLError(true);

		gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_FASTEST);
		ckeckGLError(true);
	}

	private void ckeckGLError(boolean fromMe) {
		while (true) {
			int error = gl.glGetError();
			if (error == GL.GL_NO_ERROR) {
				break;
			}
			System.err.println(String.format(
					(fromMe ? "GLState made an GL error: "
							: "Pending error code:") + " 0x%04x", error));
		}
	}

	public void setTexCoordActive(boolean active) {
		ckeckGLError(false);
		if (texCoordActive != active) {
			if (active) {
				gl.glEnableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
			} else {
				gl.glDisableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
			}
			texCoordActive = active;
		}
		ckeckGLError(true);
	}

	public void done() {
		ckeckGLError(false);
		gl.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
		gl.glLoadMatrixf(oldModelview);
		ckeckGLError(true);
	}

	public void setColor(int rgba) {
		ckeckGLError(false);
		if (activeColor != rgba) {
			setColorImpl(rgba);
			DrawStats.setColor(rgba);
			activeColor = rgba;
		}
		ckeckGLError(true);
	}

	private void setColorImpl(int rgba) {
		gl.glColor4ub((byte) (rgba >> 16 & 0xFF), (byte) (rgba >> 8 & 0xFF),
				(byte) (rgba & 0xFF), (byte) (rgba >> 24 & 0xFF));
	}

	public void toViewPosition(ViewPosition viewPosition) {
		ckeckGLError(false);
		if (!viewPosition.equals(currentViewPosition)) {
			// convert the current matrix to the new view position.
			float[] m = new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0,
					0, 1 };
			float scale = (float) currentViewPosition
					.translateScaleTo(viewPosition);
			m[0] = scale;
			m[5] = scale;
			m[12] = (float) currentViewPosition.translateXTo(viewPosition);
			m[13] = (float) currentViewPosition.translateYTo(viewPosition);
			gl.glMultMatrixf(m, 0);
			// gl.glLoadMatrixf(m, 0);
			currentViewPosition = viewPosition;
		}
		ckeckGLError(true);
	}

	public void setLineStripple(GLLineStrippleDefinition lineStripple) {
		ckeckGLError(false);
		if (lineStripple == null) {
			// non-line drawing is not affected. We can safely ignore this.
			return;
		}
		if (lineStripple.equals(oldLineStripple)) {
			// no change required.
			return;
		}
		lineStripple.activate(gl);

		this.oldLineStripple = lineStripple;
		ckeckGLError(true);
	}
}
