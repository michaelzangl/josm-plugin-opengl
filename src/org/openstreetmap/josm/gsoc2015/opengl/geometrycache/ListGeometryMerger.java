package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class ListGeometryMerger extends GeometryMerger {

	private static final int ACTIVE_MERGE_GROUPS = 100;
	private final LinkedHashSet<MergeGroup> openMergeGroups = new LinkedHashSet<>();

	protected HashSet<RecordedOsmGeometries> geometries = new HashSet<>();

	/**
	 * Adds the geometries.
	 * @param primitive The primitive to add the geometries for.
	 * @param geometries All geometries for that primitive.
	 */
	@Override
	public synchronized void addMergeables(OsmPrimitive primitive, Collection<RecordedOsmGeometries> geometries) {
		for (final RecordedOsmGeometries g : geometries) {
			if (this.geometries.contains(g)) {
				throw new IllegalArgumentException("Attempt to add twice: " + g);
			}
		}

		MergeGroup maxMergeRated = null;
		float maxMergeRating = .3f; // <- Minimum rating to merge
		for (final MergeGroup g : openMergeGroups) {
			final float mergeRating = g.getMergeRating(primitive, geometries);
			if (mergeRating > maxMergeRating) {
				maxMergeRated = g;
				maxMergeRating = mergeRating;
			}
		}
		if (maxMergeRated != null) {
		} else {
			final MergeGroup group = new MergeGroup();
			mergeGroups.add(group);
			openMergeGroups.add(group);
			if (openMergeGroups.size() > ACTIVE_MERGE_GROUPS) {
				openMergeGroups.remove(openMergeGroups.iterator().next());
			}
			maxMergeRated = group;
		}
		maxMergeRated.merge(primitive, geometries);
		if (!maxMergeRated.moreMergesRecommended()) {
			openMergeGroups.remove(maxMergeRated);
		}
		this.geometries.addAll(maxMergeRated.getGeometries());
	}

}
