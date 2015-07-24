package org.openstreetmap.josm.gsoc2015.opengl.osm.search;

import java.util.List;

import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * This class is a runnable that searches through a Dataset and finds the
 * primitives the search implementation returns.
 * 
 * @author Michael Zangl
 *
 * @param <T>
 */
public abstract class ElementSearcher<T extends OsmPrimitive> implements Runnable {

	private final OsmPrimitiveHandler<T> handler;
	protected final BBox bbox;
	protected final DataSet data;

	public ElementSearcher(OsmPrimitiveHandler<T> handler, DataSet data,
			BBox bbox) {
		this.handler = handler;
		this.data = data;
		this.bbox = bbox;
	}

	public abstract List<T> searchElements();

	public void run() {
		long time = System.currentTimeMillis();
		List<T> primitives = searchElements();
		System.out.println("Search took " + (System.currentTimeMillis() - time) + " ms and returned " + primitives.size() + " results ");
		handler.receivePrimitives(primitives);
	}
}