package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * This is a set of primitives and their merged geomtries. Once the geometries
 * are merged, it is not possible to invalidate one single element contained by
 * them. This is why we need to invalidate all geometries that have merge
 * relations to the primitive. Those are therefore stored in the same merge
 * group.
 * 
 * @author Michael Zangl.
 *
 */
public class MergeGroup {

	private ArrayList<RecordedOsmGeometries> geometries = new ArrayList<>();
	private ArrayList<OsmPrimitive> primitives = new ArrayList<>();
	
	public float getMergeRating(OsmPrimitive p, Collection<RecordedOsmGeometries> geometries) {
		float m = 0;
		if (moreMergesRecommended()) {
			for (RecordedOsmGeometries g : geometries) {
				m += getMergeRating(g);
			}
		}
		return m;
	}
	
	private float getMergeRating(RecordedOsmGeometries g) {
		if (g == null) {
			throw new NullPointerException("Got a null geometry.");
		}
		
		float r = 0;
		for (RecordedOsmGeometries inGroupGeometry : geometries) {
			r = Math.max(r, g.getCombineRating(inGroupGeometry));
		}
		return r;
	}

	public void merge(OsmPrimitive p, Collection<RecordedOsmGeometries> geometries) {
		primitives.add(p);
		// TODO: Real merge.
		this.geometries.addAll(geometries);
	}
	
	public boolean moreMergesRecommended() {
		return geometries.size() < 20 && primitives.size() < 40; 
	}

	public ArrayList<RecordedOsmGeometries> getGeometries() {
		return geometries;
	}
}
