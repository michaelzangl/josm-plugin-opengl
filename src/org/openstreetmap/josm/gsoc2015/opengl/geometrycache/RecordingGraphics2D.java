package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.text.AttributedCharacterIterator;

import org.jogamp.glg2d.G2DDrawingHelper;
import org.jogamp.glg2d.GLG2DColorHelper;
import org.jogamp.glg2d.GLG2DImageHelper;
import org.jogamp.glg2d.GLG2DShapeHelper;
import org.jogamp.glg2d.GLG2DTextHelper;
import org.jogamp.glg2d.GLG2DTransformHelper;
import org.jogamp.glg2d.GLGraphics2D;
import org.jogamp.glg2d.impl.AbstractMatrixHelper;
import org.jogamp.glg2d.impl.AbstractTextDrawer;

/**
 * This is a graphics2D implementation that sends all OpenGL calls to a given
 * recorder.
 * 
 * @author michael
 *
 */
public class RecordingGraphics2D extends GLGraphics2D {
	private Recorder recorder;

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
		return new AbstractTextDrawer() {
			@Override
			public void dispose() {
				// TODO Auto-generated method stub

			}

			@Override
			public void drawString(String string, int x, int y) {
				// TODO Auto-generated method stub

			}

			@Override
			public void drawString(String string, float x, float y) {
				// TODO Auto-generated method stub

			}

			@Override
			public void drawString(AttributedCharacterIterator iterator,
					float x, float y) {
				// TODO Auto-generated method stub

			}

			@Override
			public void drawString(AttributedCharacterIterator iterator, int x,
					int y) {
				// TODO Auto-generated method stub

			}
		};
	}

	@Override
	protected GLG2DImageHelper createImageHelper() {
		return new RecordingImageHelper(recorder);
	}

	@Override
	protected GLG2DColorHelper createColorHelper() {
		return new RecordingColorHelper();
	}

	@Override
	protected GLG2DShapeHelper createShapeHelper() {
		// colorHelper is already created here.
		return new RecordingShapeHelper((RecordingColorHelper) colorHelper,
				recorder);
	}

	@Override
	protected void scissor(boolean enable) {
		// unsupported. But we need to support storing the clip bounds.
	}
}
