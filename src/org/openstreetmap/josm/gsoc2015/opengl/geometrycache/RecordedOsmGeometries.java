package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.media.opengl.GL2;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gsoc2015.opengl.osm.ViewPosition;
import org.openstreetmap.josm.gsoc2015.opengl.util.DebugUtils;
import org.openstreetmap.josm.tools.Pair;

/**
 * This is a list of geometries that are recorded to draw the given geometries.
 * 
 * @author michael
 *
 */
public class RecordedOsmGeometries implements Comparable<RecordedOsmGeometries> {
	private static final GeometryComperator COMPERATOR = new GeometryComperator();

	private static final class GeometryComperator implements
			Comparator<RecordedGeometry> {
		@Override
		public int compare(RecordedGeometry o1, RecordedGeometry o2) {
			return Integer.compare(o1.getCombineHash(), o2.getCombineHash());
		}
	}

	/**
	 * The geometries in the order in which they need to be drawn.
	 */
	private List<RecordedGeometry> geometries;
	/**
	 * A list of primitives this geometry is for.
	 */
	private final Set<OsmPrimitive> primitives = new HashSet<>();

	/**
	 * The zoom and viewport that was used to create this geometry.
	 */
	private final ViewPosition viewPosition;

	private long orderIndex;
	private int[] hashes;

	//XXX TEMP
	public MergeGroup mergeGroup;
	
	/**
	 * 
	 * @param geometries
	 *            The geometries. A copy is created.
	 * @param primitive
	 *            The initial primitive this record is for.
	 */
	public RecordedOsmGeometries(List<RecordedGeometry> geometries,
			OsmPrimitive primitive, long orderIndex, ViewPosition viewPosition) {
		this.orderIndex = orderIndex;
		this.viewPosition = viewPosition;
		this.geometries = new ArrayList<>(geometries);
		this.primitives.add(primitive);
	}

	/**
	 * Disposes the underlying buffer and frees all allocated resources. The
	 * object may not be used afterwards.
	 */
	public void dispose() {
		for (RecordedGeometry g : geometries) {
			g.dispose();
		}
	}

	public void draw(GL2 gl, GLState state) {
		state.toViewPosition(viewPosition);
		for (RecordedGeometry g : geometries) {
			g.draw(gl, state);
		}
	}

	public Set<OsmPrimitive> getPrimitives() {
		return primitives;
	}

	/**
	 * Gets a list of hashes that suggest the combination of two geometries if
	 * most of their hashes are the same.
	 * 
	 * @return The combine hashes.
	 */
	public int[] getCombineHashes() {
		if (hashes == null) {
			hashes = getUsedHashes(geometries);
			Arrays.sort(hashes);
			// now remove duplicates from that array.
			int newIndex = 1;
			for (int i = 1; i < hashes.length; i++) {
				if (hashes[i] != hashes[i-1]) {
					hashes[newIndex++] = hashes[i];
				}
			}
			if (newIndex < hashes.length) {
				hashes = Arrays.copyOf(hashes, newIndex);
			}
		}
		return hashes;
	}

	private static int[] getUsedHashes(List<RecordedGeometry> geometries) {
		int[] hashes = new int[geometries.size()];
		for (int i = 0; i < hashes.length; i++) {
			hashes[i] = geometries.get(i).getCombineHash();
		}
		return hashes;
	}

	/**
	 * Merges an other geometry in this geometry.
	 * <p>
	 * This method is not thread safe.
	 * 
	 * @param other
	 *            The other geometry to merge.
	 * @return <code>true</code> if the merge was successful.
	 */
	public boolean mergeWith(RecordedOsmGeometries other) {
		if (!isMergeable(other)) {
			return false;
		}
		
		primitives.addAll(other.primitives);
		geometries = merge(geometries, other.geometries);
		
		// Update the hashes.
		HashSet<Integer> newHashes = new HashSet<>();
		for (RecordedGeometry geometry : other.geometries) {
			int hash = geometry.getCombineHash();
			if (Arrays.binarySearch(hashes, hash) < 0) {
				newHashes.add(hash);
			}
		}
		
		if (newHashes.size() > 0) {
			int length = hashes.length;
			hashes = Arrays.copyOf(hashes, length + newHashes.size());
			for (int hash : newHashes) {
				hashes[length++] = hash;
			}
			Arrays.sort(hashes);
		}

		return true;
	}

	private boolean isMergeable(RecordedOsmGeometries other) {
		return other.orderIndex == this.orderIndex && viewPosition.equals(other.viewPosition);
	}

	/**
	 * Merges two list of geometries. The order of geomtries is respected and
	 * stays the same after each merge.
	 * 
	 * @param geometries1
	 * @param geometries2
	 * @return A new, merged list of geometries.
	 */
	private List<RecordedGeometry> merge(List<RecordedGeometry> geometries1,
			List<RecordedGeometry> geometries2) {
		List<RecordedGeometry> ordered = new ArrayList<>();
		List<RecordedGeometry> ret = new ArrayList<>();

		LinkedList<Pair<Integer, Integer>> pairs = firstMergePairs(geometries1,
				geometries2);
		ArrayList<Pair<Integer, Integer>> filtered = removeCrossingPairs(pairs);

		// Sort pairs by a
		Collections.sort(filtered, new Comparator<Pair<Integer, Integer>>() {
			@Override
			public int compare(Pair<Integer, Integer> o1,
					Pair<Integer, Integer> o2) {
				return Integer.compare(o1.a, o2.a);
			}
		});
		// Since all crossing pairs are removed, we can be sure that paris are
		// ordered by a and by b.

		int geometry1Index = 0, geometry2Index = 0;
		for (Pair<Integer, Integer> p : filtered) {
			for (; geometry1Index < p.a; geometry1Index++) {
				ordered.add(geometries1.get(geometry1Index));
			}
			for (; geometry2Index < p.b; geometry2Index++) {
				ordered.add(geometries2.get(geometry2Index));
			}
		}
		for (; geometry1Index < geometries1.size(); geometry1Index++) {
			ordered.add(geometries1.get(geometry1Index));
		}
		for (; geometry2Index < geometries2.size(); geometry2Index++) {
			ordered.add(geometries2.get(geometry2Index));
		}

		RecordedGeometry last = null;
		Iterator<RecordedGeometry> iterator = ordered.iterator();
		while (iterator.hasNext()) {
			RecordedGeometry current = iterator.next();
			if (last != null && last.attemptCombineWith(current)) {
				// all good, we combined this one.
			} else {
				last = current;
				ret.add(current);
			}
		}

		return ret;
	}

	private ArrayList<Pair<Integer, Integer>> removeCrossingPairs(
			LinkedList<Pair<Integer, Integer>> pairs) {
		// TODO: Test if sorting by |pair.a - pair.b| helps.
		ArrayList<Pair<Integer, Integer>> filtered = new ArrayList<>();
		while (!pairs.isEmpty()) {
			Pair<Integer, Integer> p = pairs.pollFirst();
			removeAllCrossingPairs(pairs, p);
			filtered.add(p);
		}
		return filtered;
	}

	private void removeAllCrossingPairs(
			LinkedList<Pair<Integer, Integer>> pairs,
			Pair<Integer, Integer> crossingWith) {
		Iterator<Pair<Integer, Integer>> iterator = pairs.iterator();
		while (iterator.hasNext()) {
			Pair<Integer, Integer> pair = iterator.next();
			if (pair.a <= crossingWith.a && pair.b >= crossingWith.b
					|| pair.a >= crossingWith.a && pair.b <= crossingWith.b) {
				iterator.remove();
			}
		}
	}

	private LinkedList<Pair<Integer, Integer>> firstMergePairs(
			List<RecordedGeometry> geometries1,
			List<RecordedGeometry> geometries2) {
		LinkedList<Pair<Integer, Integer>> mergePairs = new LinkedList<>();
		for (int i = 0; i < geometries1.size(); i++) {
			RecordedGeometry g1 = geometries1.get(i);
			for (int j = 0; j < geometries2.size(); j++) {
				RecordedGeometry g2 = geometries1.get(i);
				if (g1.couldCombineWith(g2)) {
					mergePairs.add(new Pair<>(i, j));
				}
			}
		}
		return mergePairs;
	}

	private RecordedGeometry[] combineLists(List<RecordedGeometry> geometries1,
			List<RecordedGeometry> geometries2) {
		RecordedGeometry[] toMerge = new RecordedGeometry[geometries1.size()
				+ geometries2.size()];
		int i = 0;
		for (RecordedGeometry g : geometries1) {
			toMerge[i++] = g;
		}
		for (RecordedGeometry g : geometries2) {
			toMerge[i++] = g;
		}
		return toMerge;
	}

	@Override
	public String toString() {
		return "RecordedOsmGeometries [geometries=" + geometries
				+ ", primitives=" + primitives + "]";
	}

	@Override
	public int compareTo(RecordedOsmGeometries o) {
		return Long.compare(orderIndex, o.orderIndex);
	}

	/**
	 * A rating how useful it would be to combine those two geometries. Range
	 * 0..1
	 * 
	 * @param geometry
	 * @return
	 */
	public float getCombineRating(RecordedOsmGeometries geometry) {
		if (!isMergeable(geometry)) {
			return 0;
		}
		int commonHashes = 0;
		int[] otherHashes = geometry.getCombineHashes();
		int[] myHashes = getCombineHashes();
		for (int h : myHashes) {
			int inOtherHashes = Arrays.binarySearch(otherHashes, h);
			if (inOtherHashes >= 0) {
				commonHashes++;
			}
		}
		int totalHashes = otherHashes.length + myHashes.length - commonHashes;
		
		return (float) commonHashes / totalHashes;
	}

	/**
	 * Attempts to merge all stored {@link RecordedGeometry}s
	 */
	public void mergeChildren() {
		ArrayList<RecordedGeometry> storedGeometries = new ArrayList<>(geometries);
		geometries.clear();
		RecordedGeometry last = null;
		for (RecordedGeometry r : storedGeometries) {
			if (last != null && last.attemptCombineWith(r)) {
				//pass
			} else {
				geometries.add(r);
				last = r;
			}
		}
	}

}
