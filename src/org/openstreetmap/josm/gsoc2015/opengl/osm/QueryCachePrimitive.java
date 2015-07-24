package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordedOsmGeometries;

public class QueryCachePrimitive<T extends OsmPrimitive> implements Runnable {

	private final List<T> primitives;
	private final int min;
	private final int max;
	private final ArrayList<RecordedOsmGeometries> recorded;

	public QueryCachePrimitive(List<T> primitives, int min, int max, ArrayList<RecordedOsmGeometries> recorded) {
		this.primitives = primitives;
		this.min = min;
		this.max = max;
		this.recorded = recorded;
	}

	@Override
	public void run() {
		System.out.println("Got primitives to query: " + primitives + " from "
				+ min + " to " + max);
	}

}
