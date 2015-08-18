package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OpenGLStyledMapRenderer.StyleGenerationState;

public class QueryCachePrimitive<T extends OsmPrimitive> implements Runnable {
	
	private final List<T> primitives;
	private final int min;
	private final int max;
	private final StyleGenerationState sgs;
	private final StyleGeometryCache cache;
	private final Class<T> primitiveType;

	public QueryCachePrimitive(List<T> primitives, int min, int max,
			StyleGenerationState sgs, StyleGeometryCache cache, Class<T> type) {
		this.primitives = primitives;
		this.min = min;
		this.max = max;
		this.sgs = sgs;
		this.cache = cache;
		this.primitiveType = type;
	}

	@Override
	public void run() {
		long start = System.currentTimeMillis();
		StyledGeometryGenerator<T> gen = StyledGeometryGenerator.forType(sgs, primitiveType);
		gen.startRunning();
		try {
			for (int i = min; i < max; i++) {
				long time = System.currentTimeMillis();
				T primitive = primitives.get(i);
				cache.requestOrCreateGeometry(primitive, sgs.shouldGenerateGeometry() ? gen : null);
				if (System.currentTimeMillis() - time > 5) {
					System.err.println("Slow primitive (" + (System.currentTimeMillis() - time) + "ms) " + primitive);
				}
			}
		} finally {
			gen.endRunning();
		}
	}

}
