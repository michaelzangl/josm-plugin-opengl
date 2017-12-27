package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.StyleCache;
import org.openstreetmap.josm.tools.Logging;

/**
 * This is a set of primitives and their merged geometries. Once the geometries
 * are merged, it is not possible to invalidate one single element contained by
 * them. This is why we need to invalidate all geometries that have merge
 * relations to the primitive. Those are therefore stored in the same merge
 * group.
 * <p>
 * Every primitive is contained in at most one merge group.
 * <p>
 * When generating the geometries, a merge group is generated for each
 * primitive. Then, this group is merged with other groups to increase
 * performance.
 * <p>
 * For the following example, a {@link DataSet} of 3 ways (ignoring
 * nodes/relations) is used.
 * <p>
 * <img src="doc-files/example-scene.png"/>
 * <p>
 * You can see 3 buildings, of which two can be merged completely and all
 * outlines can be merged, and two ways.
 * <p>
 * The ways consist of two lines on different layers, the buildings consist of
 * an area and a line on the same layer but drawn in a specific order.
 * Geometries with different Z-Index are stored in different
 * {@link RecordedOsmGeometries}. For the same z-index, we can use one
 * {@link RecordedOsmGeometries} and store multiple, ordered
 * {@link RecordedGeometry} in it.
 * <p>
 * <img src="doc-files/initial-groups.png"/>
 * <p>
 * Then, those groups are merged. Some OpenGL primitives (line strips) cannot be
 * appended. This is why we convert a line strip to single line segments. They
 * can be appended and we can merge two {@link RecordedGeometry}s into a single,
 * longer geometry using
 * {@link RecordedGeometry#attemptCombineWith(RecordedGeometry)}.
 * <p>
 * <img src="doc-files/final-groups.png"/>
 *
 *
 * @author Michael Zangl.
 */
public class MergeGroup {
	/**
	 * A list of geometries that are in this merge group.
	 */
	private final ArrayList<RecordedOsmGeometries> geometries = new ArrayList<>();
	/**
	 * All primitives this group is for.
	 */
	private final ArrayList<OsmPrimitive> primitives = new ArrayList<>();

	// This should be temporary and could be replaced with better style
	// handling.
	private final HashMap<OsmPrimitive, StyleCache> primitiveStyleUsed = new HashMap<>();
	private final HashMap<OsmPrimitive, Boolean> primitiveStyleHighlighted = new HashMap<>();
	private int usedFlag = -1;

	/**
	 * This method rates how good this primitive with the given geometries could
	 * be merged in this merge group.
	 * 
	 * @param p
	 *            The primitive to merge.
	 * @param geometries
	 *            The geometries, assuming they are only for the primitive (that
	 *            means, {@link RecordedOsmGeometries#getPrimitives()} only
	 *            returns this single geometry. May be empty.
	 * @return A float value, the greater the better.
	 */
	public float getMergeRating(OsmPrimitive p,
			Collection<RecordedOsmGeometries> geometries) {
		float m = 0;
		if (moreMergesRecommended()) {
			for (final RecordedOsmGeometries g : geometries) {
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
		for (final RecordedOsmGeometries inGroupGeometry : geometries) {
			r = Math.max(r, g.getCombineRating(inGroupGeometry));
		}
		return r;
	}

	/**
	 * Merges a primitive into this merge group.
	 * 
	 * @param p
	 *            The primitive to merge.
	 * @param geometries
	 *            The geometries, assuming they are only for the primitive (that
	 *            means, {@link RecordedOsmGeometries#getPrimitives()} only
	 *            returns this single geometry. May be empty.
	 */
	public void merge(OsmPrimitive p,
			Collection<RecordedOsmGeometries> geometries) {
		// XXX This can be removed if performance is an issue.
		for (final RecordedOsmGeometries m : geometries) {
			if (this.geometries.contains(m)) {
				throw new IllegalArgumentException(
						"Attemt to merge a geometry that is already in this group.");
			}
			if (m.mergeGroup != null) {
				throw new IllegalArgumentException(
						"Got a geometry that already has a merge group.");
			}
		}

		primitives.add(p);
		final StyleCache style = p.mappaintStyle;
		if (style != null) {
			primitiveStyleUsed.put(p, style);
		} else {
			Logging.warn("No style set for " + p);
		}
		primitiveStyleHighlighted.put(p, p.isHighlighted());
		for (final RecordedOsmGeometries m : geometries) {
			boolean merged = false;
			for (final RecordedOsmGeometries g : this.geometries) {
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

	/**
	 * Tests if more primitives should be merged in this group.
	 * 
	 * @return <code>false</code> if you should stop merging.
	 */
	public boolean moreMergesRecommended() {
		return geometries.size() < 30 && primitives.size() < 80;
	}

	/**
	 * Gets all geometries to draw this group.
	 * 
	 * @return The unordered List of geometries.
	 */
	public ArrayList<RecordedOsmGeometries> getGeometries() {
		return geometries;
	}

	/**
	 * Gets all primitives for this group.
	 * 
	 * @return All primitives added by {@link #merge(OsmPrimitive, Collection)}
	 */
	public ArrayList<OsmPrimitive> getPrimitives() {
		return primitives;
	}

	/**
	 * Temporary. May be replaced with better style handling.
	 * 
	 * @param primitive
	 * @return
	 */
	public synchronized StyleCache getStyleCacheUsed(OsmPrimitive primitive) {
		return primitiveStyleUsed.get(primitive);
	}

	/**
	 * Temporary. May be replaced with better style handling.
	 * 
	 * @param primitive
	 * @return
	 */
	public boolean getStyleCacheUsedHighlighted(OsmPrimitive primitive) {
		return primitiveStyleHighlighted.get(primitive);
	}

	/**
	 * Sets the used flag for this merge group.
	 * @param usedFlag the flag to set. Used by the cache.
	 */
	public void setUsedFlag(int usedFlag) {
		this.usedFlag = usedFlag;
	}
	
	/**
	 * Gets the used flag.
	 * @return The used flag as set with {@link #getUsedFlag()}
	 */
	public int getUsedFlag() {
		return usedFlag;
	}
}
