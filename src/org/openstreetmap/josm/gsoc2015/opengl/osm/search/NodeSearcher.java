package org.openstreetmap.josm.gsoc2015.opengl.osm.search;

import java.util.List;

import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;

/**
 * This class searches for nodes in a given bounding box.
 * @author Michael Zangl
 */
public class NodeSearcher extends PrimitiveSearcher<Node> {

	public NodeSearcher(OsmPrimitiveHandler<Node> handler,
			DataSet data, BBox bbox) {
		super(handler, data, bbox);
	}

	@Override
	public List<Node> searchElements() {
		return data.searchNodes(bbox);
	}
}