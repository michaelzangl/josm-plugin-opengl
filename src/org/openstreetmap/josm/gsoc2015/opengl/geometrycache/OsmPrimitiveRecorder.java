package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.List;
import java.util.ArrayList;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * A recorder that records the geometry for one OSM primitive.
 * @author michael
 */
public class OsmPrimitiveRecorder implements Recorder, Visitor {
	
	private List<RecordedGeometry> geometries = new ArrayList<>();
	private OsmPrimitive activePrimitive;
	private RecordedPrimitiveReceiver receiver ;
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
		void receiveForNode(RecordedOsmGeometries<Node> geometry);
		void receiveForWay(RecordedOsmGeometries<Way> geometry);
		void receiveForRelation(RecordedOsmGeometries<Relation> geometry);
	}
	
	public OsmPrimitiveRecorder(RecordedPrimitiveReceiver receiver) {
		this.receiver = receiver;
	}
	
	@Override
	public void recordGeometry(RecordedGeometry cachedGeometry) {
		geometries.add(cachedGeometry);
	}
	
	private void reset() {
		geometries.clear();
	}
	
	public void start(OsmPrimitive p) {
		if (activePrimitive != null) {
			throw new IllegalStateException("start() called without a end().");
		}
		reset();
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
		receiver.receiveForNode(new RecordedOsmGeometries<Node>(geometries, n));
	}

	@Override
	public void visit(Way w) {
		receiver.receiveForWay(new RecordedOsmGeometries<Way>(geometries, w));
	}

	@Override
	public void visit(Relation r) {
		receiver.receiveForRelation(new RecordedOsmGeometries<Relation>(geometries, r));
	}

	@Override
	public void visit(Changeset cs) {
	}
}
