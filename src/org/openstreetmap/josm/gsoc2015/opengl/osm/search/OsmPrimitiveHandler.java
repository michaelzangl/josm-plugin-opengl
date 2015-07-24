package org.openstreetmap.josm.gsoc2015.opengl.osm.search;

import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * This is a receiver for lists of OSM primitives.
 * @author Michael Zangl
 *
 * @param <T> The type of primitives to receive.
 */
public interface OsmPrimitiveHandler<T extends OsmPrimitive> {
	/**
	 * Receives a list of primitives, e.g. the result of a search query.
	 * @param primitives The primitives.
	 */
	public void receivePrimitives(List<T> primitives);
}