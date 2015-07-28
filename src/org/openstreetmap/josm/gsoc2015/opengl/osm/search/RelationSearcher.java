package org.openstreetmap.josm.gsoc2015.opengl.osm.search;

import java.util.List;

import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;

/**
 * This class searches for relations in a given bounding box.
 * @author Michael Zangl
 */
public class RelationSearcher extends PrimitiveSearcher<Relation> {

	public RelationSearcher(OsmPrimitiveHandler<Relation> handler,
			DataSet data, BBox bbox) {
		super(handler, data, bbox);
	}

	@Override
	public List<Relation> searchElements() {
		return data.searchRelations(bbox);
	}
}