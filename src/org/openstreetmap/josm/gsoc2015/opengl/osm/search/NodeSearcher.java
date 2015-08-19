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

	/**
	 * Creates a new node searcher
	 *
	 * @param handler
	 *            The handler to send the found primitives to. It may be invoked
	 *            multiple times in the future.
	 * @param data
	 *            The data to search in.
	 * @param bbox
	 *            The bounding box to search.
	 */
	public NodeSearcher(OsmPrimitiveHandler<Node> handler,
			DataSet data, BBox bbox) {
		super(handler, data, bbox);
	}

	@Override
	public List<Node> searchElements() {
		return data.searchNodes(bbox);
	}
}