package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.Color;
import java.awt.Image;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;

import org.jogamp.glg2d.GLG2DImageHelper;
import org.jogamp.glg2d.GLGraphics2D;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.TextureManager.TextureEntry;

class RecordingImageHelper implements GLG2DImageHelper {
		TextureManager textureCache = new TextureManager();
		private Recorder recorder;

		public RecordingImageHelper(Recorder recorder) {
			this.recorder = recorder;
		}

		@Override
		public void setG2D(GLGraphics2D g2d) {
		}

		@Override
		public void push(GLGraphics2D newG2d) {
		}

		@Override
		public void pop(GLGraphics2D parentG2d) {
		}

		@Override
		public void setHint(Key key, Object value) {
		}

		@Override
		public void resetHints() {
		}

		@Override
		public void dispose() {
			textureCache.clearAll();
		}

		@Override
		public boolean drawImage(Image img, int x, int y, Color bgcolor,
				ImageObserver observer) {
			return drawImage(img,
					AffineTransform.getTranslateInstance(x, y), bgcolor,
					observer);
		}

		@Override
		public boolean drawImage(Image img, AffineTransform xform,
				ImageObserver observer) {
			return drawImage(img, xform, (Color) null, observer);
		}

		@Override
		public boolean drawImage(Image img, int x, int y, int width,
				int height, Color bgcolor, ImageObserver observer) {
			double imgHeight = img.getHeight(null);
			double imgWidth = img.getWidth(null);

			if (imgHeight < 0 || imgWidth < 0) {
				return false;
			}

			AffineTransform transform = AffineTransform
					.getTranslateInstance(x, y);
			transform.scale(width / imgWidth, height / imgHeight);
			return drawImage(img, transform, bgcolor, observer);
		}

		@Override
		public boolean drawImage(Image img, int dx1, int dy1, int dx2,
				int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor,
				ImageObserver observer) {
			TextureEntry texture = textureCache.getTexture(img, observer);

			// float height = img.getHeight(observer);
			// float width = img.getWidth(observer);
			// if (width < 0 || height < 0) {
			// return false;
			// }
			// applyTexture(texture, bgcolor, dx1, dy1, dx2, dy2, sx1 /
			// width, sy1
			// / height, sx2 / width, sy2 / height);
			// TODO
//				RecordedGeometry rec = RecordedGeometry.generateTexture(
//						texture, bgcolor.getRGB(), points, 0, 0, 1, 1);

			System.err.println("Got unsupported draw call.");
			
			return true;
		}

		protected boolean drawImage(Image img, AffineTransform xform,
				Color color, ImageObserver observer) {
			if (color == null) {
				color = Color.white;
			}
			TextureEntry texture = textureCache.getTexture(img, observer);

			float height = img.getHeight(observer);
			float width = img.getWidth(observer);
			if (width < 0 || height < 0) {
				return false;
			}
			float[] points = new float[] {
					0, width, // <-se
					height, width, // <-ne
					height, 0, // <-nw
					0, 0, // <-sw
			};
			xform.transform(points, 0, points, 0, 4);

			RecordedGeometry rec = RecordedGeometry.generateTexture(
					texture, color.getRGB(), points, 0, 0, 1, 1);

			recorder.recordGeometry(rec);
			return true;
		}

		@Override
		public void drawImage(BufferedImage img, BufferedImageOp op, int x,
				int y) {
			notImplemented();
		}

		@Override
		public void drawImage(RenderedImage img, AffineTransform xform) {
			notImplemented();
		}

		@Override
		public void drawImage(RenderableImage img, AffineTransform xform) {
			notImplemented();
		}

		private void notImplemented() {
			System.err.println("Image operation not implemented.");
			Thread.dumpStack();
		}
	}