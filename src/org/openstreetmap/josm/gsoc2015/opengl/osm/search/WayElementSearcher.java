package org.openstreetmap.josm.gsoc2015.opengl.osm.search;

import java.util.List;

import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;

/**
 * This class searches for ways in a given bounding box.
 * @author Michael Zangl
 */
public class WayElementSearcher extends ElementSearcher<Way> {

	public WayElementSearcher(OsmPrimitiveHandler<Way> handler,
			DataSet data, BBox bbox) {
		super(handler, data, bbox);
	}

	@Override
	public List<Way> searchElements() {
		return data.searchWays(bbox);
	}
}