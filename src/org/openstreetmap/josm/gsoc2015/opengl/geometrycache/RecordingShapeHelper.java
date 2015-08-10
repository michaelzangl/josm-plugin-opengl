package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;

import org.jogamp.glg2d.VertexBuffer;
import org.jogamp.glg2d.impl.AbstractShapeHelper;
import org.jogamp.glg2d.impl.SimplePathVisitor;

public class RecordingShapeHelper extends AbstractShapeHelper {

	private final RecordingStrokeLineVisitor lineVisitor;
	private final RecordingStarOrTesselatorVisitor fillVisitor;

	private enum Clockwise {
		CLOCKWISE, COUNTER_CLOCKWISE, NOT_SURE;

		public static Clockwise isClockwise(float x1, float y1, float x2,
				float y2, float x3, float y3) {
			float area = (x2 - x1) * (y1 + y2) + (x3 - x2) * (y2 + y3)
					+ (x1 - x3) * (y1 + y3);
			if (area > .00001) {
				return COUNTER_CLOCKWISE;
			} else if (area < .00001) {
				return CLOCKWISE;
			} else {
				return NOT_SURE;
			}
		}
	}

	/**
	 * This visitor assumes that the current shape has a star form around the
	 * first vertex. This is the case e.g. for all convex shapes.
	 * <p>
	 * It does not record a triangle fan but single triangles instead.
	 * <p>
	 * Let v_1, ... , v_n be the vertexes. We then assume v1 to be the center
	 * point of our start
	 * <p>
	 * First, we add a triangle v1, v2, v3. For each new vertex, we add the
	 * triangle v1, v_(i-1), v_i
	 * <p>
	 * As long as all outer vertexes form a circular shape, we are good. This is
	 * the case if and only if all triangles have the same orientation (either
	 * clockwise or counter-clockwise)
	 * 
	 * 
	 * @author Michael Zangl
	 *
	 */
	private static class RecordingStarOrTesselatorVisitor extends
			SimplePathVisitor {
		/**
		 * Most simple shapes should not have more than 16 corners. XXX:
		 * Confirm.
		 */
		private static final int DEFAULT_SIZE = 16 * 3;
		private final RecordingTesselatorVisitor fallback;
		private final Recorder recorder;
		private final RecordingColorHelper colorRecorder;
		private float startPointX;
		private float startPointY;
		private float lastPointX;
		private float lastPointY;
		/**
		 * Drawing the current polygon has failed and is using the fallback.
		 */
		private boolean failed;
		/**
		 * This flag is set as soon as we should start drawing.
		 */
		private boolean inDraw = false;

		private Clockwise isClockwise = Clockwise.NOT_SURE;

		private VertexBuffer vBuffer = new VertexBuffer(DEFAULT_SIZE);

		public RecordingStarOrTesselatorVisitor(
				RecordingColorHelper colorHelper, Recorder recorder) {
			this.colorRecorder = colorHelper;
			this.recorder = recorder;
			this.fallback = new RecordingTesselatorVisitor(colorHelper,
					recorder);
		}

		@Override
		public void setGLContext(GL context) {
			// unused
		}

		@Override
		public void setStroke(BasicStroke stroke) {
			// only supports filling.
		}

		@Override
		public void moveTo(float[] vertex) {
			if (failed) {
				// We send a moveTo to force closing the current loop.
				fallback.moveTo(vertex);
			}
			commitIfRequired();
			startPointX = lastPointX = vertex[0];
			startPointY = lastPointY = vertex[1];
			inDraw = false;
			isClockwise = Clockwise.NOT_SURE;
		}

		@Override
		public void lineTo(float[] vertex) {
			if (failed) {
				fallback.lineTo(vertex);
				return;
			}
			if (closeTo(lastPointX, vertex[0])
					&& closeTo(lastPointY, vertex[1])) {
				// The user does not see a difference but we save a triangle.
				return;
			}

			if (!inDraw) {
				inDraw = true;
			} else {
				vBuffer.addVertex(startPointX, startPointY);
				vBuffer.addVertex(lastPointX, lastPointY);
				vBuffer.addVertex(vertex[0], vertex[1]);
				Clockwise cw = Clockwise.isClockwise(startPointX, startPointY,
						lastPointX, lastPointY, vertex[0], vertex[1]);
				if (cw != Clockwise.NOT_SURE
						&& isClockwise != Clockwise.NOT_SURE
						&& cw != isClockwise) {
					switchToFallback();
				}
				isClockwise = cw;
			}
			lastPointX = vertex[0];
			lastPointY = vertex[1];
		}

		private void switchToFallback() {
			// replay the last steps.
			FloatBuffer buffer = vBuffer.getBuffer();
			int count = buffer.position();
			if (count < 2 * 3) {
				throw new IllegalStateException(
						"vBuffer was not filled enough.");
			}
			buffer.rewind();
			float[] vertex = new float[2];
			buffer.get(vertex);
			fallback.moveTo(vertex);
			buffer.get(vertex);
			fallback.lineTo(vertex);
			for (int vertexPosition = 4; vertexPosition < count; vertexPosition += 6) {
				buffer.position(vertexPosition);
				buffer.get(vertex);
				fallback.lineTo(vertex);
			}
			buffer.rewind();
			failed = true;
		}

		private boolean closeTo(float d1, float d2) {
			return Math.abs(d1 - d2) < .1;
		}

		@Override
		public void closeLine() {
			if (failed) {
				fallback.closeLine();
			}
			commitIfRequired();
			// triangle is auto-closed.
		}

		@Override
		public void beginPoly(int windingRule) {
			fallback.beginPoly(windingRule);
		}

		@Override
		public void endPoly() {
			fallback.endPoly();
			commitIfRequired();
		}

		private void commitIfRequired() {
			if (!failed && vBuffer.getBuffer().position() > 0) {
				recorder.recordGeometry(new RecordedGeometry(GL.GL_TRIANGLES,
						vBuffer, colorRecorder.getActiveColor()));
				vBuffer = new VertexBuffer(DEFAULT_SIZE);
			}
			inDraw = false;
			isClockwise = Clockwise.NOT_SURE;
			failed = false;
		}
	}

	public RecordingShapeHelper(RecordingColorHelper colorHelper,
			Recorder recorder) {
		fillVisitor = new RecordingStarOrTesselatorVisitor(colorHelper,
				recorder);
		lineVisitor = new RecordingStrokeLineVisitor(colorHelper, recorder);
	}

	@Override
	public void draw(Shape shape) {
		Stroke stroke = getStroke();
		// long time1 = System.currentTimeMillis();
		if (stroke instanceof BasicStroke) {
			BasicStroke basicStroke = (BasicStroke) stroke;
			if (basicStroke.getDashArray() == null) {
				lineVisitor.setStroke(basicStroke);
				// System.out.println("Slow line beign? lineVisitor");
				traceShape(shape, lineVisitor);
				// System.out.println("Slow line end? lineVisitor "
				// + (System.currentTimeMillis() - time1) + "ms ");
				return;
			}
		}
		// System.out.println("Slow line beign?");
		Shape strokedShape = stroke.createStrokedShape(shape);
		// long time2 = System.currentTimeMillis();
		fill(strokedShape);
		// System.out.println("Slow line end? " + (time2 - time1) + "ms, "
		// + (System.currentTimeMillis() - time2) + "ms, ");
	}

	@Override
	protected void fill(Shape shape, boolean isDefinitelySimpleConvex) {
		// System.out.println("Slow fill beign? " + shape);
		// long time1 = System.currentTimeMillis();
		traceShape(shape, fillVisitor);
		// System.out.println("Slow fill end?"
		// + (System.currentTimeMillis() - time1) + "ms, ");
	}

}
