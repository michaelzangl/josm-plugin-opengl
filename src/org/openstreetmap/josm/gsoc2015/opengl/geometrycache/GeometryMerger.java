package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * This class merges a list of geometries into a new list.
 *
 * @author Michael Zangl
 * @see MergeGroup
 */
public abstract class GeometryMerger {

	/**
	 * The list of merge groups that are currently in this merger.
	 */
	protected ArrayList<MergeGroup> mergeGroups = new ArrayList<>();

	public GeometryMerger() {
	}

	public abstract void addMergeables(OsmPrimitive primitive,
			Collection<RecordedOsmGeometries> geometries);

	/**
	 * Gets all geometries that have been added so far. Some of them may have
	 * been merged, so this list is shorter than the list of provided
	 * geometries.
	 *
	 * @return The merged geometries.
	 */
	public ArrayList<RecordedOsmGeometries> getGeometries() {
		final ArrayList<RecordedOsmGeometries> geometries = new ArrayList<>();
		for (final MergeGroup m : mergeGroups) {
			geometries.addAll(m.getGeometries());
		}
		return geometries;
	}

	/**
	 * Gets all merge groups produced by this merger.
	 *
	 * @return The result of the merge.
	 */
	public ArrayList<MergeGroup> getMergeGroups() {
		return mergeGroups;
	}
}
