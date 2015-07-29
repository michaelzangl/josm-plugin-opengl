package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.media.opengl.GL2;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gsoc2015.opengl.osm.ViewPosition;

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
	 * 
	 * @param gl
	 */
	public void dispose(GL2 gl) {
		for (RecordedGeometry g : geometries) {
			g.dispose(gl);
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
			hashes = new int[geometries.size()];
			for (int i = 0; i < hashes.length; i++) {
				hashes[i] = geometries.get(i).getCombineHash();
			}
			Arrays.sort(hashes);
		}
		return hashes;
	}

	/**
	 * Merges an other geometry in this geometry.
	 * 
	 * @param other
	 *            The other geometry to merge.
	 * @return <code>true</code> if the merge was successful.
	 */
	public boolean mergeWith(RecordedOsmGeometries other) {
		if (other.orderIndex != this.orderIndex || !viewPosition.equals(other.viewPosition)) {
			return false;
		}
		hashes = null;
		primitives.addAll(other.primitives);
		geometries = merge(geometries, other.geometries);
		
		return true;
	}

	private List<RecordedGeometry> merge(List<RecordedGeometry> geometries1,
			List<RecordedGeometry> geometries2) {
		RecordedGeometry[] toMerge = combineLists(geometries1, geometries2);
		Arrays.sort(toMerge, COMPERATOR);
		RecordedGeometry last = null;
		List<RecordedGeometry> ret = new ArrayList<>();
		for (RecordedGeometry current : toMerge) {
			if (last != null && last.attemptCombineWith(current)) {
				// all good, we combined this one.
			} else {
				ret.add(current);
				last = current;
			}
		}
				
		return ret;
	}

	private RecordedGeometry[] combineLists(List<RecordedGeometry> geometries1,
			List<RecordedGeometry> geometries2) {
		RecordedGeometry[] toMerge = new RecordedGeometry[geometries1.size() + geometries2.size()];
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
		return 1;
	}

}
