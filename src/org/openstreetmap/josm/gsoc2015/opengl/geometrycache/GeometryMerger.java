package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * This class merges a list of geometries into a new list.
 * 
 * @author Michael Zangl
 */
public class GeometryMerger {
	private HashMap<Integer, List<RecordedOsmGeometries>> combineMap = new HashMap<>();

	private ArrayList<RecordedOsmGeometries> geometries = new ArrayList<>();

	public GeometryMerger() {
	}

	public synchronized void addMergeable(RecordedOsmGeometries geometry) {
		if (geometries.contains(geometry)) {
			throw new IllegalArgumentException(
					"Attempted to merge in a geometry that is already merged.");
		}

		for (int hash : geometry.getCombineHashes()) {
			List<RecordedOsmGeometries> list = combineMap.get(hash);
			if (list != null) {
				for (RecordedOsmGeometries r : list) {
					if (attemptMerge(geometry, r)) {
						return;
					}
				}
			}
		}

		geometries.add(geometry);
		addHashes(geometry, geometry.getCombineHashes(), new int[0]);
	}

	private boolean attemptMerge(RecordedOsmGeometries geometry,
			RecordedOsmGeometries mergeInto) {
		if (mergeInto.getCombineRating(geometry) > 0) {
			int[] oldHashes = mergeInto.getCombineHashes();
			if (mergeInto.mergeWith(geometry)) {
				addHashes(mergeInto, mergeInto.getCombineHashes(), oldHashes);
				return true;
			}
		}
		return false;
	}

	private void addHashes(RecordedOsmGeometries geometry, int[] combineHashes,
			int[] except) {
		for (int c : combineHashes) {
			if (Arrays.binarySearch(except, c) < 0) {
				List<RecordedOsmGeometries> list = combineMap.get(c);
				if (list == null) {
					list = new ArrayList<>();
				}
				list.add(geometry);
				combineMap.put(c, list);
			}
		}
	}

	public void addMergeables(Collection<RecordedOsmGeometries> geometries) {
		for (RecordedOsmGeometries g : geometries) {
			addMergeable(g);
		}
	}

	/**
	 * Gets all geometries that have been added so far. Some of them may have
	 * been merged, so this list is shorter than the list of provided
	 * geometries.
	 * 
	 * @return The merged geometries.
	 */
	public ArrayList<RecordedOsmGeometries> getGeometries() {
		return geometries;
	}
}
