package org.openstreetmap.josm.gsoc2015.opengl.osm;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OpenGLStyledMapRenderer.StyleGenerationState;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.StyleCache.StyleList;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;

/**
 * This class computes the styles for a given array of elements.
 * 
 * @author michael
 *
 * @param <T>
 */
public class StyledGeometryGenerator<T extends OsmPrimitive>
		implements Runnable {
	/**
	 * A class implementing this interface provides all state data to access
	 * the object style cache.
	 * 
	 * @author Michael Zangl
	 */
	interface ChacheDataSupplier {
		public NavigatableComponent getCacheKey();

		public double getCircum();
	}

	private final ElemStyles styles = MapPaintStyles.getStyles();
	private final Iterable<T> primitives;
	private final StyleGenerationState sgs;

	public StyledGeometryGenerator(Iterable<T> primitives,
			StyleGenerationState sgs) {
		this.primitives = primitives;
		this.sgs = sgs;
	}

	@Override
	public void run() {
		MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
		try {
			for (T p : primitives) {
				if (p.isDrawable()) {
					StyleList sl = styles.get(p,
							sgs.getCircum(),
							sgs.getCacheKey());

					// TODO: parallel recordingRenderer
					for (ElemStyle s : sl) {
						new StyleGenerationRequest<T>(s, p, sgs).run();
					}
				}
			}
		} finally {
			MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
		}
	}
}