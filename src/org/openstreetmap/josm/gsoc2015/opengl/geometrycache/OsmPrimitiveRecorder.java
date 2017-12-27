package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.OsmPrimitiveVisitor;
import org.openstreetmap.josm.gsoc2015.opengl.osm.ViewPosition;
import org.openstreetmap.josm.tools.Logging;

/**
 * A recorder that records the geometry for one OSM primitive.
 *
 * @author michael
 */
public class OsmPrimitiveRecorder implements Recorder, OsmPrimitiveVisitor {

	private final List<RecordedGeometry> geometries = new ArrayList<>();
	private OsmPrimitive activePrimitive;
	private final RecordedPrimitiveReceiver receiver;
	private long activeOrderIndex;
	private ViewPosition viewPosition;

	/**
	 * Classes implementing this may receive geometries for {@link OsmPrimitive}
	 * s
	 * 
	 * @author Michael Zangl
	 */
	public interface RecordedPrimitiveReceiver {
		/**
		 * Called whenever a geometry for a node was generated. May be called
		 * multiple times per node.
		 * 
		 * @param geometry
		 *            The recorded geometry objects. You can assume that
		 *            {@link RecordedOsmGeometries#getPrimitives()} only returns
		 *            one single primitive.
		 */
		void receiveForNode(RecordedOsmGeometries geometry);

		/**
		 * Called whenever a geometry for a way was generated. May be called
		 * multiple times per way.
		 * 
		 * @param geometry
		 *            The recorded geometry objects. You can assume that
		 *            {@link RecordedOsmGeometries#getPrimitives()} only returns
		 *            one single primitive.
		 */
		void receiveForWay(RecordedOsmGeometries geometry);

		/**
		 * Called whenever a geometry for a relation was generated. May be called
		 * multiple times per relation.
		 * 
		 * @param geometry
		 *            The recorded geometry objects. You can assume that
		 *            {@link RecordedOsmGeometries#getPrimitives()} only returns
		 *            one single primitive.
		 */
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
			Logging.warn("Skipping a nop geometry");
			cachedGeometry.dispose();
		}
	}

	private void reset() {
		geometries.clear();
	}

	/**
	 * Starts recording geometries for the given primitive. All draw calls until
	 * the next end() are recorded.
	 *
	 * @param p
	 *            The primitive to record for.
	 * @param viewPosition
	 *            The current view position on screen.
	 * @param orderIndex
	 *            The order index to support z-index.
	 */
	public void start(OsmPrimitive p, ViewPosition viewPosition, long orderIndex) {
		if (activePrimitive != null) {
			throw new IllegalStateException("start() called without a end().");
		}
		reset();
		activeOrderIndex = orderIndex;
		this.viewPosition = viewPosition;
		activePrimitive = p;
	}

	/**
	 * Ends recording for the current primitive.
	 */
	public void end() {
		if (activePrimitive == null) {
			throw new IllegalStateException("end() called without a start().");
		}
		activePrimitive.accept(this);
		activePrimitive = null;
	}

	@Override
	public void visit(Node n) {
		receiver.receiveForNode(new RecordedOsmGeometries(geometries, n,
				activeOrderIndex, viewPosition));
	}

	@Override
	public void visit(Way w) {
		receiver.receiveForWay(new RecordedOsmGeometries(geometries, w,
				activeOrderIndex, viewPosition));
	}

	@Override
	public void visit(Relation r) {
		receiver.receiveForRelation(new RecordedOsmGeometries(geometries, r,
				activeOrderIndex, viewPosition));
	}
}
