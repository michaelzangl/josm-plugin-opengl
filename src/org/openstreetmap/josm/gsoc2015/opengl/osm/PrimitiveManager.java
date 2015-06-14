package org.openstreetmap.josm.gsoc2015.opengl.osm;

import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This class manages all primitives of a given data layer. It tracks which of
 * them are in the view area.
 * 
 * @author michael
 *
 */
public class PrimitiveManager {
	private final OsmDataLayer layer;

	public PrimitiveManager(OsmDataLayer layer) {
		this.layer = layer;
	}
	
	
}
