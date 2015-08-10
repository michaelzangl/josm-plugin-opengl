package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.GeometryMerger;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.MergeGroup;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordedOsmGeometries;

/**
 * This is a cache of style geometries.
 * <p>
 * It allows you to query the geometries for a given primitive.
 * <p>
 * This cache is optimized for beeing accessed during one draw phase. It allows
 * asynchronous geometzry generation but geometry fetching should be done in
 * frames.
 * <p>
 * Each frame is initiated by calling {@link #startFrame()}. The cache is then
 * prepared to receive multiple request calls.
 * <p>
 * On each request call, the geometries for this frame can be scheduled for
 * collection using
 * {@link #requestOrCreateGeometry(OsmPrimitive, StyledGeometryGenerator)}
 * <p>
 * When the frame has ended, {@link #endFrame()} returns a list of all received
 * geometries.
 * 
 * 
 * @author Michael Zangl
 *
 */
public class StyleGeometryCache {
	private final class SelectionObserver implements SelectionChangedListener {
		private DataSet data;

		private Collection<OsmPrimitive> selected;

		public SelectionObserver(DataSet data) {
			super();
			this.data = data;
			selected = data.getAllSelected();
		}

		@Override
		public void selectionChanged(
				Collection<? extends OsmPrimitive> newSelectionInAnyDataSet) {
			// JOSM fires for all data sets. I cannot filter this.
			Collection<OsmPrimitive> newSelection = data.getAllSelected();
			if (newSelection == selected) {
				return;
			}
			// TODO: Use sets here.
			for (OsmPrimitive s : selected) {
				if (!newSelection.contains(s)) {
					// TODO: Give a priority when invalidating.
					// XXX invalidateGeometry(s);
				}
			}
			for (OsmPrimitive s : newSelection) {
				if (!selected.contains(s)) {
					// XXX invalidateGeometry(s);
				}
			}

			selected = newSelection;
		}
	}

	private Hashtable<OsmPrimitive, MergeGroup> recordedForPrimitive = new Hashtable<>();

	private Set<RecordedOsmGeometries> collectedForFrame = Collections
			.synchronizedSet(new HashSet<RecordedOsmGeometries>());

	private GeometryMerger collectedForFrameMerger;

	private SelectionChangedListener selectionListener;

	public void invalidateAll() {
		recordedForPrimitive.clear();
	}

	public synchronized void invalidateGeometry(OsmPrimitive s) {
		MergeGroup recorded = recordedForPrimitive.get(s);
		if (recorded != null) {
			invalidate(recorded);
		}
	}

	private void invalidate(MergeGroup mergeGroup) {
		// TODO Only schedule deletion, dispose, regenerate the other geometries
		// affected.
		removeCacheEntries(mergeGroup);
	}

	public void startFrame() {
		collectedForFrame.clear();
		collectedForFrameMerger = new GeometryMerger();
	}

	public ArrayList<RecordedOsmGeometries> endFrame() {
		// get all geometries from merger.
		ArrayList<RecordedOsmGeometries> list = new ArrayList<>(
				collectedForFrame);
		ArrayList<RecordedOsmGeometries> fromMerger = collectedForFrameMerger
				.getGeometries();
		System.out.println("Received " + list.size()
				+ " geometries from cache and " + fromMerger.size()
				+ " generated this frame.");

		updateMergeGroups(collectedForFrameMerger.getMergeGroups());
		list.addAll(fromMerger);
		collectedForFrameMerger = null;
		collectedForFrame.clear();
		return list;
	}

	private void updateMergeGroups(ArrayList<MergeGroup> mergeGroups) {
		for (MergeGroup mergeGroup : mergeGroups) {
			for (OsmPrimitive p : mergeGroup.getPrimitives()) {
				removeCacheEntry(p);
				recordedForPrimitive.put(p, mergeGroup);
			}
		}

	}

	/**
	 * Removes the cached entry for the primitive p and all related primitives
	 * immediately.
	 * 
	 * @param p
	 */
	private void removeCacheEntry(OsmPrimitive p) {
		MergeGroup g = recordedForPrimitive.get(p);
		if (g != null) {
			removeCacheEntries(g);
		}
	}

	private void removeCacheEntries(MergeGroup g) {
		for (OsmPrimitive inGroup : g.getPrimitives()) {
			recordedForPrimitive.remove(inGroup);
		}
		for (RecordedOsmGeometries geo : g.getGeometries()) {
			// TODO: geo.dispose(gl);
		}
	}

	/**
	 * <p>
	 * This may only be called for each primitive once in every frame.
	 * 
	 * @param primitive
	 * @param generator
	 * @return
	 */
	public void requestOrCreateGeometry(OsmPrimitive primitive,
			StyledGeometryGenerator<OsmPrimitive> generator) {
		// query primitive
		MergeGroup list = recordedForPrimitive.get(primitive);
		// style change. TODO: Handle zoom changes.
		// TODO: Listen to cache invalidation events.
		if (primitive.mappaintStyle == null
				|| (list != null && (primitive.mappaintStyle != list
						.getStyleCacheUsed(primitive) || primitive
						.isHighlighted() != list
						.getStyleCacheUsedHighlighted(primitive)))) {
			System.out.println("Mappaint style change found.");
			invalidateGeometry(primitive);
			list = recordedForPrimitive.get(primitive);
		}
		// if not exists and generator is set
		if (list == null) {
			if (generator != null) {
				// -- call generator. Generator returns list of geometries
				// -- pass on to merger
				List<RecordedOsmGeometries> geometries = generator
						.runFor(primitive);
				long time = System.currentTimeMillis();
				collectedForFrameMerger.addMergeables(primitive, geometries);
				System.out.println("Merge time: " + (System.currentTimeMillis() - time) + "ms");
			} else {
				// if not exists and no generator is set
				// -- schedule for background generation
			}
		} else {
			// -- add the geometries to collectedForFrame
			collectedForFrame.addAll(list.getGeometries());
		}

	}

	// private void putGeometries(Collection<RecordedOsmGeometries> geometries)
	// {
	// System.out.println("There are " + recordedForPrimitive.size() +
	// " geos in cache");
	// for (RecordedOsmGeometries g : geometries) {
	// putGeometry(g);
	// }
	// System.out.println("There are " + recordedForPrimitive.size() +
	// " geos in cache");
	// }
	//
	// private synchronized void putGeometry(RecordedOsmGeometries geometry) {
	// for (OsmPrimitive primitive : geometry.getPrimitives()) {
	// List<RecordedOsmGeometries> list = recordedForPrimitive
	// .get(primitive);
	// if (list == null) {
	// list = new ArrayList<>();
	// recordedForPrimitive.put(primitive, list);
	// }
	// list.add(geometry);
	// }
	// }

	public void addListeners(DataSet data) {
		selectionListener = new SelectionObserver(data);
		data.addSelectionListener(selectionListener);
	}
}
