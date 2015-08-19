package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OpenGLStyledMapRenderer.StyleGenerationState;

/**
 * Queries a range of geometries for primitives from the cache.
 * @author Michael Zangl
 *
 * @param <T> The primitive type.
 */
public class QueryCachePrimitive<T extends OsmPrimitive> implements Runnable {

	private static final boolean DEBUG = false;
	private final List<T> primitives;
	private final int min;
	private final int max;
	private final StyleGenerationState sgs;
	private final StyleGeometryCache cache;

	/**
	 * Creates a new {@link QueryCachePrimitive}
	 * @param primitives The list of primitives to query.
	 * @param min The start list index
	 * @param max One after the last list element to use.
	 * @param sgs The current style state.
	 * @param cache The cache to use.
	 */
	public QueryCachePrimitive(List<T> primitives, int min, int max,
			StyleGenerationState sgs, StyleGeometryCache cache) {
		this.primitives = primitives;
		this.min = min;
		this.max = max;
		this.sgs = sgs;
		this.cache = cache;
	}

	@Override
	public void run() {
		final StyledGeometryGenerator gen = new StyledGeometryGenerator(sgs);
		gen.startRunning();
		try {
			for (int i = min; i < max; i++) {
				long time = 0;
				if (DEBUG) {
					time = System.currentTimeMillis();
				}
				final T primitive = primitives.get(i);
				cache.requestOrCreateGeometry(primitive,
						sgs.shouldGenerateGeometry() ? gen : null);
				if (DEBUG && System.currentTimeMillis() - time > 5) {
					System.err.println("Slow primitive ("
							+ (System.currentTimeMillis() - time) + "ms) "
							+ primitive);
				}
			}
		} finally {
			gen.endRunning();
		}
	}

}
