package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import static org.jogamp.glg2d.impl.AbstractShapeHelper.visitShape;

import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.text.AttributedCharacterIterator;

import org.jogamp.glg2d.G2DDrawingHelper;
import org.jogamp.glg2d.GLG2DColorHelper;
import org.jogamp.glg2d.GLG2DImageHelper;
import org.jogamp.glg2d.GLG2DShapeHelper;
import org.jogamp.glg2d.GLG2DTextHelper;
import org.jogamp.glg2d.GLG2DTransformHelper;
import org.jogamp.glg2d.GLGraphics2D;
import org.jogamp.glg2d.PathVisitor;
import org.jogamp.glg2d.impl.AbstractMatrixHelper;
import org.jogamp.glg2d.impl.AbstractShapeHelper;
import org.jogamp.glg2d.impl.AbstractTextDrawer;
import org.jogamp.glg2d.impl.shader.text.CollectingTesselator;
import org.jogamp.glg2d.impl.shader.text.CollectingTesselator.Triangles;

import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * This is a graphics2D implementation that sends all OpenGL calls to a given
 * recorder.
 * 
 * @author michael
 *
 */
public class RecordingGraphics2D extends GLGraphics2D {
	private final class RecordingStringHelper extends AbstractTextDrawer {
		private RecordingTesselatorVisitor fillVisitor;

		public RecordingStringHelper(RecordingColorHelper colorHelper,
				Recorder recorder) {
			fillVisitor = new RecordingTesselatorVisitor(colorHelper, recorder);
		}

		@Override
		public void dispose() {
			// TODO Auto-generated method stub

		}

		@Override
		public void drawString(String string, int x, int y) {
			drawString(string, (float) x, (float) y);
		}

		@Override
		public void drawString(String string, float x, float y) {
			// pipeline.setColor(gl,
			// g2d.getUniformsObject().colorHook.getRGBA());
			// pipeline.setTransform(gl,
			// g2d.getUniformsObject().transformHook.getGLMatrixData());

			GlyphVector glyphs = getFont().createGlyphVector(
					getFontRenderContext(), string);
			for (int i = 0; i < string.length(); i++) {
				Shape outline = glyphs.getGlyphOutline(i, x, y);
				AbstractShapeHelper.visitShape(outline, fillVisitor);
			}
		}

		protected Triangles getTesselatedGlyph(char c) {
			GlyphVector glyphVect = getFont().createGlyphVector(
					getFontRenderContext(), new char[] { c });
			Shape s = glyphVect.getGlyphOutline(0);

			CollectingTesselator tess = new CollectingTesselator();
			visitShape(s, tess);
			Triangles triangles = tess.getTesselated();

			return triangles;
		}

		@Override
		public void drawString(AttributedCharacterIterator iterator, float x,
				float y) {
			StringBuilder builder = new StringBuilder(iterator.getEndIndex()
					- iterator.getBeginIndex());
			while (iterator.next() != AttributedCharacterIterator.DONE) {
				builder.append(iterator.current());
			}

			drawString(builder.toString(), x, y);
		}

		@Override
		public void drawString(AttributedCharacterIterator iterator, int x,
				int y) {
			drawString(iterator, (float) x, (float) y);
		}
	}

	private Recorder recorder;
	private RecordingColorHelper createdColorHelper;

	public RecordingGraphics2D(Recorder recorder) {
		super();
		this.recorder = recorder;

		realCreateDrawingHelpers();
		// We don't have a context, so we need to initialize the heplers
		// ourselves.
		for (G2DDrawingHelper helper : helpers) {
			helper.setG2D(this);
		}
	}

	@Override
	protected void createDrawingHelpers() {
		// This is called in super constructor. We ignore it.
	}

	protected void realCreateDrawingHelpers() {
		// This is called in our constructor
		super.createDrawingHelpers();
	}

	@Override
	protected GLG2DTransformHelper createTransformHelper() {
		return new AbstractMatrixHelper() {
			@Override
			protected void flushTransformToOpenGL() {
			}
		};
	}

	@Override
	protected GLG2DTextHelper createTextHelper() {
		// colorHelper is not created here.
		return new RecordingStringHelper(getOrCreateColorHelper(),
				recorder);
	}

	@Override
	protected GLG2DImageHelper createImageHelper() {
		return new RecordingImageHelper(recorder);
	}

	private RecordingColorHelper getOrCreateColorHelper() {
		// Used in super constructor, so we cannot use field initialization here.
		if (createdColorHelper == null) {
			createdColorHelper = new RecordingColorHelper();
		}
		return createdColorHelper;
	}
	
	@Override
	protected GLG2DColorHelper createColorHelper() {
		return getOrCreateColorHelper();
	}

	@Override
	protected GLG2DShapeHelper createShapeHelper() {
		return new RecordingShapeHelper(getOrCreateColorHelper(),
				recorder);
	}

	@Override
	protected void scissor(boolean enable) {
		// unsupported. But we need to support storing the clip bounds.
	}
}
