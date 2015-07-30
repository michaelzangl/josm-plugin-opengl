package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OpenGLStyledMapRenderer.StyleGenerationState;

public class QueryCachePrimitive<T extends OsmPrimitive> implements Runnable {

	public static AtomicInteger GENERATE = new AtomicInteger();
	
	private final List<T> primitives;
	private final int min;
	private final int max;
	private final StyleGenerationState sgs;
	private final StyleGeometryCache cache;

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
		long start = System.currentTimeMillis();
		StyledGeometryGenerator<OsmPrimitive> gen = new StyledGeometryGenerator<>(sgs);
		gen.startRunning();
		try {
			for (int i = min; i < max; i++) {
				long time = System.currentTimeMillis();
				T primitive = primitives.get(i);
				cache.requestOrCreateGeometry(primitive, GENERATE.decrementAndGet() > 0 ? gen : null);
				if (System.currentTimeMillis() - time > 5) {
					System.err.println("Slow primitive (" + (System.currentTimeMillis() - time) + "ms) " + primitive);
				}
			}
		} finally {
			gen.endRunning();
		}
		System.out.println("Got primitives to query: " + primitives.size() + " from "
				+ min + " to " + max + ", time: " + (System.currentTimeMillis() - start));
	}

}
