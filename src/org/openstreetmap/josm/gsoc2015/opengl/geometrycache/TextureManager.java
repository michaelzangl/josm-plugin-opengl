package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.VolatileImage;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import javax.media.opengl.GL2;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

/**
 * This is the texture manager.
 * 
 * @author michael
 *
 */
public class TextureManager {

	public static class TextureEntry {
		Texture texture = null;

		BufferedImage imageToLoad;

		public TextureEntry(Image image) {
			imageToLoad = toBufferedImage(image);
		}

		protected BufferedImage toBufferedImage(Image image) {
			if (image instanceof BufferedImage && ((BufferedImage) image).getType() != BufferedImage.TYPE_CUSTOM) {
				return (BufferedImage) image;
			}
			if (image instanceof VolatileImage) {
				return ((VolatileImage) image).getSnapshot();
			}

			int width = image.getWidth(null);
			int height = image.getHeight(null);
			if (width < 0 || height < 0) {
				return null;
			}

			BufferedImage bufferedImage = new BufferedImage(width, height,
					BufferedImage.TYPE_4BYTE_ABGR);
			bufferedImage.createGraphics().drawImage(image, null, null);
			return bufferedImage;
		}

		/**
		 * Only call this in the GL thread.
		 * 
		 * @param gl
		 * @return 
		 */
		public Texture getTexture(GL2 gl) {
			if (imageToLoad != null) {
				// we'll assume the image is complete and can be rendered
				texture = AWTTextureIO.newTexture(gl.getGLProfile(),
						imageToLoad, false);

				imageToLoad = null;
			}
			return texture;
		}
	}

	private HashMap<WeakIdentityKey<Image>, TextureEntry> textures = new HashMap<>();

	public synchronized TextureEntry getTexture(Image image, ImageObserver observer) {
		// TODO: Use a queue to garbage collect old entries.
		WeakIdentityKey<Image> key = new WeakIdentityKey<Image>(image, null);
		TextureEntry texture = textures.get(key);
		if (texture == null) {
			texture = new TextureEntry(image);
			textures.put(key, texture);
		}
		return texture;
	}

	public synchronized void clearAll() {
		// TODO: Remove all images.
	}

	private static class WeakIdentityKey<T> extends WeakReference<T> {
		private final int hash;

		public WeakIdentityKey(T value, ReferenceQueue<T> queue) {
			super(value, queue);
			hash = value.hashCode();
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof WeakIdentityKey) {
				WeakIdentityKey<?> other = (WeakIdentityKey<?>) obj;
				return other.hash == hash && get() == other.get();
			} else {
				return false;
			}
		}
	}
}
