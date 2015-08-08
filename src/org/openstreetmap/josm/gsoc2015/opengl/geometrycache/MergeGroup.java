package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.StyleCache;

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
	
	// TODO: This should be temporary.
	private HashMap<OsmPrimitive, StyleCache> primitiveStyleUsed = new HashMap<>();
	private HashMap<OsmPrimitive, Boolean> primitiveStyleHighlighted = new HashMap<>();
	
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
		//XXX This can be removed if performance is an issue.
		for (RecordedOsmGeometries m : geometries) {
			if (this.geometries.contains(m)) {
				throw new IllegalArgumentException("Attemt to merge a geometry that is already in this group.");
			}
			if (m.mergeGroup != null) {
				throw new IllegalArgumentException("Got a geometry that already has a merge group.");
			}
		}
		
		primitives.add(p);
		StyleCache style = p.mappaintStyle;
		if (style != null) {
			primitiveStyleUsed.put(p, style);
		} else {
			Main.warn("No style set for " + p);
		}
		primitiveStyleHighlighted.put(p, p.isHighlighted());
		for (RecordedOsmGeometries m : geometries) {
			boolean merged = false;
			for (RecordedOsmGeometries g : this.geometries) {
				if (g.mergeWith(m)) {
					merged = true;
					break;
				}
			}
			if (!merged) {
				m.mergeGroup = this;
				this.geometries.add(m);
			}
		}
	}
	
	public boolean moreMergesRecommended() {
		return geometries.size() < 30 && primitives.size() < 80; 
	}

	public ArrayList<RecordedOsmGeometries> getGeometries() {
		return geometries;
	}
	
	public ArrayList<OsmPrimitive> getPrimitives() {
		return primitives;
	}

	public synchronized StyleCache getStyleCacheUsed(OsmPrimitive primitive) {
		return primitiveStyleUsed.get(primitive);
	}

	public boolean getStyleCacheUsedHighlighted(OsmPrimitive primitive) {
		return primitiveStyleHighlighted.get(primitive);
	}
}
