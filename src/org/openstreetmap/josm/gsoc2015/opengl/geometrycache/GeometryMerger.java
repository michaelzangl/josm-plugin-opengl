package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class GeometryMerger {
	private HashMap<Integer, List<RecordedOsmGeometries>> combineMap = new HashMap<>();

	private ArrayList<RecordedOsmGeometries> geometreis = new ArrayList<>();

	public GeometryMerger() {
	}

	public void addMergeable(RecordedOsmGeometries geometry) {
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

		geometreis.add(geometry);
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
	
	public ArrayList<RecordedOsmGeometries> getGeometries() {
		return geometreis;
	}
}
