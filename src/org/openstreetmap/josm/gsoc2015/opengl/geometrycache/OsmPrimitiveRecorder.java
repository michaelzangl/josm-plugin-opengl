package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gsoc2015.opengl.osm.ViewPosition;

/**
 * A recorder that records the geometry for one OSM primitive.
 * @author michael
 */
public class OsmPrimitiveRecorder implements Recorder, Visitor {
	
	private List<RecordedGeometry> geometries = new ArrayList<>();
	private OsmPrimitive activePrimitive;
	private RecordedPrimitiveReceiver receiver ;
	private long activeOrderIndex;
	private ViewPosition viewPosition;
	
//			new RecordedPrimitiveReceiver() {
//		
//		@Override
//		public void receiveForWay(RecordedOsmGeometries<Way> geometry) {
//			System.out.println("Received way: " + geometry.toString());
//		}
//		
//		@Override
//		public void receiveForRelation(RecordedOsmGeometries<Relation> geometry) {
//			// TODO Auto-generated method stub
//			
//		}
//		
//		@Override
//		public void receiveForNode(RecordedOsmGeometries<Node> geometry) {
//			// TODO Auto-generated method stub
//			
//		}
//	};

	public interface RecordedPrimitiveReceiver {
		void receiveForNode(RecordedOsmGeometries geometry);
		void receiveForWay(RecordedOsmGeometries geometry);
		void receiveForRelation(RecordedOsmGeometries geometry);
	}
	
	public OsmPrimitiveRecorder(RecordedPrimitiveReceiver receiver) {
		this.receiver = receiver;
	}
	
	@Override
	public void recordGeometry(RecordedGeometry cachedGeometry) {
		if (!cachedGeometry.isNop()) {
			geometries.add(cachedGeometry);
		} else {
			Main.warn("Skipping a nop geometry");
			cachedGeometry.dispose();
		}
	}
	
	private void reset() {
		geometries.clear();
	}
	
	public void start(OsmPrimitive p, ViewPosition viewPosition, long orderIndex) {
		if (activePrimitive != null) {
			throw new IllegalStateException("start() called without a end().");
		}
		reset();
		this.activeOrderIndex = orderIndex;
		this.viewPosition = viewPosition;
		activePrimitive = p;
	}
	
	public void end() {
		if (activePrimitive == null) {
			throw new IllegalStateException("end() called without a start().");
		}
		activePrimitive.accept(this);
		activePrimitive = null;
	}

	@Override
	public void visit(Node n) {
		receiver.receiveForNode(new RecordedOsmGeometries(geometries, n, activeOrderIndex, viewPosition));
	}

	@Override
	public void visit(Way w) {
		receiver.receiveForWay(new RecordedOsmGeometries(geometries, w, activeOrderIndex, viewPosition));
	}

	@Override
	public void visit(Relation r) {
		receiver.receiveForRelation(new RecordedOsmGeometries(geometries, r, activeOrderIndex, viewPosition));
	}

	@Override
	public void visit(Changeset cs) {
		throw new UnsupportedOperationException();
	}
}
