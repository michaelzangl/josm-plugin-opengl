package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.OsmPrimitiveRecorder.RecordedPrimitiveReceiver;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OpenGLStyledMapRenderer.StyleGenerationState;

public class QueryCachePrimitive<T extends OsmPrimitive> implements Runnable {

	private final List<T> primitives;
	private final int min;
	private final int max;
	private final StyleGenerationState sgs;
	private final RecordedPrimitiveReceiver receiver;

	public QueryCachePrimitive(List<T> primitives, int min, int max,
			StyleGenerationState sgs, RecordedPrimitiveReceiver receiver) {
		this.primitives = primitives;
		this.min = min;
		this.max = max;
		this.sgs = sgs;
		this.receiver = receiver;
	}

	@Override
	public void run() {
		long time = System.currentTimeMillis();
		StyledGeometryGenerator<T> gen = new StyledGeometryGenerator<>(sgs,	receiver);
		gen.startRunning();
		try {
			for (int i = min; i < max; i++) {
				time = System.currentTimeMillis();
				gen.runFor(primitives.get(i));
				if (System.currentTimeMillis() - time > 5) {
					System.err.println("Slow primitive (" + (System.currentTimeMillis() - time) + "ms) " + primitives.get(i));
				}
			}
		} finally {
			gen.endRunning();
		}
		System.out.println("Got primitives to query: " + primitives.size() + " from "
				+ min + " to " + max + ", time: " + (System.currentTimeMillis() - time));
	}

}
