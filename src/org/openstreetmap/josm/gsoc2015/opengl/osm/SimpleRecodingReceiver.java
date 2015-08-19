package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.List;

import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.OsmPrimitiveRecorder.RecordedPrimitiveReceiver;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordedOsmGeometries;

/**
 * This geometry receiver simply stores them in the privided list.
 *
 * @author Michael Zangl
 */
class SimpleRecodingReceiver implements RecordedPrimitiveReceiver {

	private final List<RecordedOsmGeometries> recorded;

	/**
	 * Creates a new receiver.
	 *
	 * @param recorded
	 *            The list to store received geometries in.
	 */
	public SimpleRecodingReceiver(List<RecordedOsmGeometries> recorded) {
		this.recorded = recorded;
	}

	@Override
	public void receiveForNode(RecordedOsmGeometries geometry) {
		recorded.add(geometry);
	}

	@Override
	public void receiveForWay(RecordedOsmGeometries geometry) {
		recorded.add(geometry);
	}

	@Override
	public void receiveForRelation(RecordedOsmGeometries geometry) {
		recorded.add(geometry);
	}

}