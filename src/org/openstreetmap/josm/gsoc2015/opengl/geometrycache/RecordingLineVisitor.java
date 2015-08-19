package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.BasicStroke;

import javax.media.opengl.GL;

import org.jogamp.glg2d.impl.SimplePathVisitor;
import org.openstreetmap.josm.gsoc2015.opengl.pool.VertexBufferProvider;
import org.openstreetmap.josm.gsoc2015.opengl.pool.VertexBufferProvider.ReleasableVertexBuffer;

/**
 * This visitor records a line as {@link GL#GL_LINE_LOOP} or
 * {@link GL#GL_LINE_STRIP}
 * <p>
 * This is only possible if the line style is supported by OpenGL.
 *
 * @see GLLineStippleDefinition
 * @author Michael Zangl
 */
public class RecordingLineVisitor extends SimplePathVisitor {

	/**
	 * Maximum number of points that most lines have. We reserve a buffer this
	 * big for each line.
	 */
	private static final int DEFAULT_LINE_LENGTH = 32;

	private GLLineStippleDefinition stripple;

	private ReleasableVertexBuffer vBuffer;

	private final RecordingColorHelper colorHelper;

	private final Recorder recorder;

	private final VertexBufferProvider VERTEX_BUFFER_PROVIDER = VertexBufferProvider.DEFAULT;

	public RecordingLineVisitor(RecordingColorHelper colorHelper,
			Recorder recorder) {
		this.colorHelper = colorHelper;
		this.recorder = recorder;
		resetVBuffer();
	}

	public void setStripple(GLLineStippleDefinition stripple) {
		this.stripple = stripple;
	}

	@Override
	public void setGLContext(GL context) {
	}

	@Override
	public void setStroke(BasicStroke stroke) {
	}

	@Override
	public void moveTo(float[] vertex) {
		commitIfRequired(false);
		vBuffer.addVertex(vertex[0], vertex[1]);
	}

	private void commitIfRequired(boolean closedLine) {
		if (vBuffer.getBuffer().position() > 2) {
			recorder.recordGeometry(RecordedGeometry.generateLine(stripple,
					colorHelper.getActiveColor(), vBuffer, closedLine));
			resetVBuffer();
		} else {
			vBuffer.getBuffer().rewind();
		}
	}

	private void resetVBuffer() {
		vBuffer = VERTEX_BUFFER_PROVIDER.getVertexBuffer(DEFAULT_LINE_LENGTH);
	}

	@Override
	public void lineTo(float[] vertex) {
		vBuffer.addVertex(vertex[0], vertex[1]);
	}

	@Override
	public void closeLine() {
		commitIfRequired(true);
	}

	@Override
	public void beginPoly(int windingRule) {
	}

	@Override
	public void endPoly() {
		commitIfRequired(false);
	}

	public void dispose() {
		vBuffer.release();
	}

}