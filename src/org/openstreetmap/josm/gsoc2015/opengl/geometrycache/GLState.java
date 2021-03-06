package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.fixedfunc.GLPointerFunc;

import org.openstreetmap.josm.gsoc2015.opengl.osm.ViewPosition;

import com.jogamp.common.nio.Buffers;

/**
 * This is a state cache for OpenGL. All color changes and other state changes
 * need to be passed through here. This class optimizes those state changes to
 * minimize the number of native calls required.
 *
 * @author michael
 *
 */
public class GLState implements GeometryDisposer {

	private final GL2 gl;

	public GLState(GL2 gl) {
		this.gl = gl;
	}

	private int activeColor;
	private boolean texCoordActive;
	private ViewPosition currentViewPosition;
	private final FloatBuffer oldModelview = Buffers.newDirectFloatBuffer(16);
	private GLLineStippleDefinition oldLineStripple;

	private final ArrayList<Integer> vboToDelete = new ArrayList<>();
	private final Object vboToDeleteMutex = new Object();

	/**
	 * Starts with the default opengl state.
	 *
	 * @param viewPosition
	 *            The initial view position.
	 */
	public void initialize(ViewPosition viewPosition) {
		deleteVBOs();
		ckeckGLError(false);
		gl.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, oldModelview);
		ckeckGLError(true);

		currentViewPosition = viewPosition;
		activeColor = Color.white.getRGB();
		setColorImpl(activeColor);
		ckeckGLError(true);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
				GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
				GL.GL_NEAREST);
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

	private void deleteVBOs() {
		synchronized (vboToDeleteMutex) {
			if (!vboToDelete.isEmpty()) {
				int[] vbos = new int[vboToDelete.size()];
				for (int i = 0; i < vbos.length; i++) {
					vbos[i] = vboToDelete.get(i);
				}
				gl.glDeleteVertexArrays(vbos.length, vbos, 0);
				vboToDelete.clear();
			}
		}
	}

	private void ckeckGLError(boolean fromMe) {
		while (true) {
			final int error = gl.glGetError();
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

	/**
	 * Resets the OpenGL state to the state assumed by GLG2D.
	 */
	public void done() {
		deleteVBOs();
		ckeckGLError(false);
		gl.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
		gl.glLoadMatrixf(oldModelview);
		ckeckGLError(true);
	}

	/**
	 * Sets the color.
	 *
	 * @param rgba
	 *            The color as rgba value.
	 * @see Color#getRGB()
	 */
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

	/**
	 * Sets the current view position. Adjusts the transform matrix accordingly.
	 *
	 * @param viewPosition
	 *            The new view position.
	 */
	public void toViewPosition(ViewPosition viewPosition) {
		ckeckGLError(false);
		if (!viewPosition.equals(currentViewPosition)) {
			// convert the current matrix to the new view position.
			final float[] m = new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0,
					0, 0, 0, 1 };
			final float scale = (float) currentViewPosition
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

	/**
	 * Sets the style for drawing lines.
	 * 
	 * @param lineStripple
	 *            The line style.
	 */
	public void setLineStripple(GLLineStippleDefinition lineStripple) {
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

		oldLineStripple = lineStripple;
		ckeckGLError(true);
	}

	@Override
	public void disposeVBO(int vboId) {
		synchronized (vboToDeleteMutex) {
			vboToDelete.add(vboId);
		}
	}
}
