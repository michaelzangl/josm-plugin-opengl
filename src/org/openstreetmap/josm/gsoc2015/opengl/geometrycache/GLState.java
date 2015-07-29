package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.Color;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.fixedfunc.GLPointerFunc;

import org.openstreetmap.josm.gsoc2015.opengl.osm.ViewPosition;

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

	public void initialize(ViewPosition viewPosition) {
		currentViewPosition = viewPosition;
		activeColor = Color.white.getRGB();
		setColorImpl(activeColor);
		gl.glTexEnvi(GL2ES1.GL_TEXTURE_ENV, GL2ES1.GL_TEXTURE_ENV_MODE,
				GL2ES1.GL_MODULATE);
		gl.glTexParameterf(GL2ES1.GL_TEXTURE_ENV, GL2ES1.GL_TEXTURE_ENV_MODE,
				GL.GL_BLEND);

		// always active.
		gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
	}

	public void setTexCoordActive(boolean active) {
		if (texCoordActive != active) {
			if (active) {
				gl.glEnableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
			} else {
				gl.glDisableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
			}
			texCoordActive = active;
		}
	}

	public void done() {
		gl.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
	}

	public void setColor(int rgba) {
		if (activeColor != rgba) {
			setColorImpl(rgba);
			activeColor = rgba;
		}
	}

	private void setColorImpl(int rgba) {
		gl.glColor4ub((byte) (rgba >> 16 & 0xFF), (byte) (rgba >> 8 & 0xFF),
				(byte) (rgba & 0xFF), (byte) (rgba >> 24 & 0xFF));
	}

	public void toViewPosition(ViewPosition viewPosition) {
		// convert the current matrix to the new view position.
		float[] m = new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0,
				1 };
		float scale = (float) currentViewPosition.translateScaleTo(viewPosition);
		m[0] = scale;
		m[5] = scale;
		m[12] = (float) currentViewPosition.translateXTo(viewPosition);
		m[13] = (float) currentViewPosition.translateYTo(viewPosition);
		gl.glMultMatrixf(m, 0);
		//gl.glLoadMatrixf(m, 0);
		currentViewPosition = viewPosition;
	}
}
