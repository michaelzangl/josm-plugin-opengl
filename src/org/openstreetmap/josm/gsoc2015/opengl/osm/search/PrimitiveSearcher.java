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
public abstract class PrimitiveSearcher<T extends OsmPrimitive> implements
Runnable {

	private final OsmPrimitiveHandler<T> handler;
	protected final BBox bbox;
	protected final DataSet data;

	/**
	 * Creates a new primitive searcher
	 *
	 * @param handler
	 *            The handler to send the found primitives to. It may be invoked
	 *            multiple times in the future.
	 * @param data
	 *            The data to search in.
	 * @param bbox
	 *            The bounding box to search.
	 */
	public PrimitiveSearcher(OsmPrimitiveHandler<T> handler, DataSet data,
			BBox bbox) {
		this.handler = handler;
		this.data = data;
		this.bbox = bbox;
	}

	/**
	 * Runs the search for the elements.
	 *
	 * @return The list of elements in that area.
	 */
	public abstract List<T> searchElements();

	@Override
	public void run() {
		final List<T> primitives = searchElements();
		handler.receivePrimitives(primitives);
	}
}