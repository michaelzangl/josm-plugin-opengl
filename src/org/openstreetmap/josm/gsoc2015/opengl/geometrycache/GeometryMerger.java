package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
/**
 * This class merges a list of geometries into a new list.
 * 
 * @author Michael Zangl
 */
public class GeometryMerger {
//	private HashMap<Integer, List<RecordedOsmGeometries>> combineMap = new HashMap<>();

	private HashSet<RecordedOsmGeometries> geometries = new HashSet<>();

	private static final int ACTIVE_MERGE_GROUPS = 5000;
	private LinkedHashSet<MergeGroup> openMergeGroups = new LinkedHashSet<>();
	private ArrayList<MergeGroup> mergeGroups = new ArrayList<>();

	public GeometryMerger() {
	}

//	public synchronized void addMergeable(RecordedOsmGeometries geometry) {
//		if (geometries.contains(geometry)) {
//			throw new IllegalArgumentException(
//					"Attempted to merge in a geometry that is already merged.");
//		}
//
//		for (int hash : geometry.getCombineHashes()) {
//			List<RecordedOsmGeometries> list = combineMap.get(hash);
//			if (list != null) {
//				for (RecordedOsmGeometries r : list) {
//					if (attemptMerge(geometry, r)) {
//						return;
//					}
//				}
//			}
//		}
//
//		geometries.add(geometry);
//		addHashes(geometry, geometry.getCombineHashes(), new int[0]);
//	}

//	private boolean attemptMerge(RecordedOsmGeometries geometry,
//			RecordedOsmGeometries mergeInto) {
//		if (mergeInto.getCombineRating(geometry) > 0) {
//			int[] oldHashes = mergeInto.getCombineHashes();
//			if (mergeInto.mergeWith(geometry)) {
//				addHashes(mergeInto, mergeInto.getCombineHashes(), oldHashes);
//				return true;
//			}
//		}
//		return false;
//	}

//	private void addHashes(RecordedOsmGeometries geometry, int[] combineHashes,
//			int[] except) {
//		for (int c : combineHashes) {
//			if (Arrays.binarySearch(except, c) < 0) {
//				List<RecordedOsmGeometries> list = combineMap.get(c);
//				if (list == null) {
//					list = new ArrayList<>();
//				}
//				list.add(geometry);
//				combineMap.put(c, list);
//			}
//		}
//	}

	/**
	 * Adds the geometries.
	 * @param primitive The primitive to add the geometries for.
	 * @param geometries All geometries for that primitive.
	 */
	public synchronized void addMergeables(OsmPrimitive primitive, Collection<RecordedOsmGeometries> geometries) {
		if (findDuplicates(geometries).size() > 0) {
			throw new RuntimeException("Duplicates while adding :-(" + geometries);
		}
		for (RecordedOsmGeometries g : geometries) {
			if (this.geometries.contains(g)) {
				throw new IllegalArgumentException("Attempt to add twice: " + g);
			}
		}
		
		MergeGroup maxMergeRated = null;
		float maxMergeRating = .3f; // <- Minimum rating to merge
		for (MergeGroup g : openMergeGroups) {
			float mergeRating = g.getMergeRating(primitive, geometries);
			if (mergeRating > maxMergeRating) {
				maxMergeRated = g;
				maxMergeRating = mergeRating;
			}
		}
		if (maxMergeRated != null) {
		} else {
			MergeGroup group = new MergeGroup();
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

	/**
	 * Gets all geometries that have been added so far. Some of them may have
	 * been merged, so this list is shorter than the list of provided
	 * geometries.
	 * 
	 * @return The merged geometries.
	 */
	public ArrayList<RecordedOsmGeometries> getGeometries() {
		ArrayList<RecordedOsmGeometries> geometries = new ArrayList<>();
		for (MergeGroup m : mergeGroups) {
			geometries.addAll(m.getGeometries());
		}
		return geometries ;
	}
	
	public ArrayList<MergeGroup> getMergeGroups() {
		return mergeGroups;
	}
}
