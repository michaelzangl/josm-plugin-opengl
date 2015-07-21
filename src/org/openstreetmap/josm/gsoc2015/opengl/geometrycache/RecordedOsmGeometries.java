package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.media.opengl.GL2;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class RecordedOsmGeometries<T extends OsmPrimitive> {
	/**
	 * The geometries in the order in which they need to be drawn.
	 */
	private List<RecordedGeometry> geometries;
	/**
	 * A list of primitives this geometry is for.
	 */
	private Set<T> primitives = new HashSet<>();

	/**
	 * 
	 * @param geometries
	 *            The geometires. A copy is created.
	 * @param primitives
	 */
	public RecordedOsmGeometries(List<RecordedGeometry> geometries, T primitive) {
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

	public void draw(GL2 gl) {
		for (RecordedGeometry g : geometries) {
			g.draw(gl);
		}
	}

	@Override
	public String toString() {
		return "RecordedOsmGeometries [geometries=" + geometries
				+ ", primitives=" + primitives + "]";
	}

}
