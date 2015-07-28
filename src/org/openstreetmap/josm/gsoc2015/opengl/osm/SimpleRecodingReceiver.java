package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.List;

import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordedOsmGeometries;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.OsmPrimitiveRecorder.RecordedPrimitiveReceiver;

class SimpleRecodingReceiver implements
		RecordedPrimitiveReceiver {

	private List<RecordedOsmGeometries> recorded;

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