package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.GeometryMerger;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.HashGeometryMerger;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.MergeGroup;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordedOsmGeometries;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.NavigatableComponent.ZoomChangeListener;
import org.openstreetmap.josm.tools.Pair;

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
	private static final int BACKGROUND_BULK_SIZE = 30;

	private final class ZoomListener implements ZoomChangeListener {
		private static final double MIN_CHANGE = .9;
		private double currentScale;
		private final MapView mapView;

		public ZoomListener(MapView mapView) {
			this.mapView = mapView;
			currentScale = mapView.getScale();
		}

		@Override
		public void zoomChanged() {
			final double newScale = mapView.getScale();
			final double change = newScale / currentScale;
			if (change < MIN_CHANGE || change > 1 / MIN_CHANGE) {
				invalidateAllLater();
				currentScale = newScale;
			}
		}
	}

	/**
	 * This class listens to changes of the {@link DataSet} and invalidates
	 * cache entries if their style stays the same but the way they are rendered
	 * might have changed.
	 *
	 * @author Michael Zangl
	 *
	 */
	private final class GeometryChangeObserver implements DataSetListener {
		@Override
		public void wayNodesChanged(WayNodesChangedEvent event) {
			// All node changes invoke clearCachedStyle().
		}

		@Override
		public void tagsChanged(TagsChangedEvent event) {
			// All tag changes invoke clearCachedStyle().
		}

		@Override
		public void relationMembersChanged(RelationMembersChangedEvent event) {
			// the changed members get a clearCachedStyle(), but not the
			// relation itself.
			invalidateGeometry(event.getRelation());
		}

		@Override
		public void primitivesRemoved(PrimitivesRemovedEvent event) {
			for (final OsmPrimitive p : event.getPrimitives()) {
				invalidateGeometry(p);
				// we ignore child nodes/...
			}
		}

		@Override
		public void primitivesAdded(PrimitivesAddedEvent event) {
			// no need to handle, get detected automatically.
		}

		@Override
		public void otherDatasetChange(AbstractDatasetChangedEvent event) {
			// we don't know what happened. Just ignore.
		}

		@Override
		public void nodeMoved(NodeMovedEvent event) {
			invalidateGeometry(event.getNode());
			for (final OsmPrimitive ref : event.getNode().getReferrers()) {
				invalidateGeometry(ref);
			}
		}

		@Override
		public void dataChanged(DataChangedEvent event) {
			// Ignored for now.
		}
	}

	private final class SelectionObserver implements SelectionChangedListener {
		private final DataSet data;

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
			final Collection<OsmPrimitive> newSelection = data.getAllSelected();
			if (newSelection == selected) {
				return;
			}
			// Note: This was replaced with the clearStyleCache() function.
			// We should use this to handle those geometries with higher
			// priority.
			for (final OsmPrimitive s : selected) {
				if (!newSelection.contains(s)) {
					// Give a priority when invalidating.
					// invalidateGeometry(s);
				}
			}
			for (final OsmPrimitive s : newSelection) {
				if (!selected.contains(s)) {
					// De-selected geometries seem not to get a notification.
					invalidateGeometry(s);
				}
			}

			selected = newSelection;
		}
	}

	/**
	 * Stores the merge group in which that primitive is. We always draw the
	 * whole merge group whenever that primitive is drawn.
	 */
	private final Hashtable<OsmPrimitive, MergeGroup> recordedForPrimitive = new Hashtable<>();

	/**
	 * A list of geometries that were created in this frame and are not yet in
	 * the cache.
	 */
	private GeometryMerger collectedForFrameMerger;

	/**
	 * This listener watches for selection changes and forwards them to us.
	 */
	private SelectionChangedListener selectionListener;

	/**
	 * This listeners watches for changes to the dataset and forwards them to
	 * us.
	 */
	private final GeometryChangeObserver changeObserver = new GeometryChangeObserver();

	/**
	 * A regenerator that (re)generates geometries in background if needed.
	 */
	private final BackgroundGemometryRegenerator regenerator;

	/**
	 * The repaint listener to notify whenever geometries have changed and a
	 * repaint is required.
	 */
	private final FullRepaintListener repaintListener;

	/**
	 * When this flag is set, all geometries need to be regenerated.
	 */
	private boolean invalidateAll;

	/**
	 * Our zoom listener.
	 */
	private ZoomListener zoomListener;

	/**
	 * A list of merge groups that should be drawn this frame.
	 */
	private final HashSet<MergeGroup> groupsForThisFrame = new HashSet<>();

	/**
	 * A set of primitives that are/will be in the current merger. This prevents
	 * us from adding one twice.
	 */
	private final HashSet<OsmPrimitive> primitivesRegenerated = new HashSet<>();

	/**
	 * A fake, empty merge group that is used while the real geometry is
	 * generated in a background thread.
	 */
	private MergeGroup scheduledForBackground = null;

	/**
	 * Locks access to {@link #scheduledForBackground}
	 */
	private final Object scheduledForBackgroundMutex = new Object();

	public StyleGeometryCache(FullRepaintListener repaintListener) {
		super();
		this.repaintListener = repaintListener;
		regenerator = new BackgroundGemometryRegenerator(repaintListener);
	}

	/**
	 * Invalidates the geometry for a given primitive.
	 *
	 * @param s
	 *            The primitive.
	 */
	public synchronized void invalidateGeometry(OsmPrimitive s) {
		final MergeGroup recorded = recordedForPrimitive.get(s);
		if (recorded != null) {
			invalidate(recorded);
		}
	}

	/**
	 * Invalidates a given merge group. This is called whenever drawing that
	 * merge group would give wrong results (e.g. the geometry changed, objects
	 * in the group are deleted, ...
	 *
	 * @param mergeGroup
	 *            The merge group to regenerate.
	 */
	private void invalidate(MergeGroup mergeGroup) {
		removeCacheEntries(mergeGroup);
	}

	/**
	 * Prepares the cache for collecting geometries for the next frame.
	 */
	public void startFrame() {
		reset();
		collectedForFrameMerger = new HashGeometryMerger();

		final Pair<Collection<OsmPrimitive>, Collection<MergeGroup>> groupsAndPrimitives = regenerator
				.takeNewMergeGroups();
		for (final OsmPrimitive p : groupsAndPrimitives.a) {
			removeCacheEntry(p);
		}
		updateMergeGroups(groupsAndPrimitives.b);

		if (invalidateAll) {
			for (final MergeGroup m : recordedForPrimitive.values()) {
				invalidateLater(m);
			}
			invalidateAll = false;
		}
	}

	/**
	 * Invalidates a merge group for later invalidation.
	 *
	 * @param mergeGroup
	 */
	private void invalidateLater(MergeGroup mergeGroup) {
		regenerator.schedule(mergeGroup);
	}

	/**
	 * Mark all entries for invalidation. The current entries are still in use
	 * until the new ones are generated.
	 */
	protected void invalidateAllLater() {
		invalidateAll = true;
		// Note: Geometries are invalidated on next repaint and take one frame
		// before starting to arrive.
		repaintListener.requestRepaint();
	}

	/**
	 * Ends recording for the current frame and returns an unordered list of
	 * geometries to draw the current frame.
	 *
	 * @return An unordered list of geometries.
	 * @see #startFrame()
	 */
	public ArrayList<RecordedOsmGeometries> endFrame() {
		synchronized (scheduledForBackgroundMutex) {
			if (scheduledForBackground != null) {
				scheduleForBackground();
			}
		}

		// get all geometries from merger.
		final ArrayList<RecordedOsmGeometries> list = new ArrayList<>();
		for (final MergeGroup g : groupsForThisFrame) {
			list.addAll(g.getGeometries());
		}
		final ArrayList<RecordedOsmGeometries> fromMerger = collectedForFrameMerger
				.getGeometries();
		System.out.println("Received " + list.size()
				+ " geometries from cache and " + fromMerger.size()
				+ " generated this frame.");

		updateMergeGroups(collectedForFrameMerger.getMergeGroups());
		list.addAll(fromMerger);
		collectedForFrameMerger = null;
		reset();
		return list;
	}

	/**
	 * Resets part of our frame state.
	 */
	private void reset() {
		groupsForThisFrame.clear();
		primitivesRegenerated.clear();
	}

	private void updateMergeGroups(Collection<MergeGroup> mergeGroups) {
		for (final MergeGroup mergeGroup : mergeGroups) {
			for (final OsmPrimitive p : mergeGroup.getPrimitives()) {
				invalidateGeometry(p);
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
		final MergeGroup g = recordedForPrimitive.get(p);
		if (g != null) {
			removeCacheEntries(g);
		}
	}

	private void removeCacheEntries(MergeGroup g) {
		for (final OsmPrimitive inGroup : g.getPrimitives()) {
			recordedForPrimitive.remove(inGroup);
		}
		for (final RecordedOsmGeometries geo : g.getGeometries()) {
			geo.dispose();
		}
	}

	/**
	 * Adds a new primitive to be drawn. This is called by the draw collection
	 * threads for every primitive in view.
	 * <p>
	 * This may only be called for each primitive once in every frame.
	 *
	 * @param primitive
	 *            The primitive
	 * @param generator
	 *            The generator that can be used to generate a new geometry if
	 *            the current one was invalidated.
	 */
	public <T extends OsmPrimitive> void requestOrCreateGeometry(
			OsmPrimitive primitive, StyledGeometryGenerator generator) {
		Collection<OsmPrimitive> regenerate = null;
		synchronized (this) {
			final MergeGroup group = recordedForPrimitive.get(primitive);
			// TODO: Listen to cache invalidation events.
			if (group != null
					&& (primitive.mappaintStyle == null || primitive.mappaintStyle != group
					.getStyleCacheUsed(primitive) || primitive
					.isHighlighted() != group
					.getStyleCacheUsedHighlighted(primitive))) {
				groupsForThisFrame.remove(group);
				invalidate(group);
				regenerate = new ArrayList<>();
				for (final OsmPrimitive p : group.getPrimitives()) {
					if (primitivesRegenerated.add(p)) {
						regenerate.add(p);
					}
				}
			} else if (group == null) {
				if (primitivesRegenerated.add(primitive)) {
					regenerate = Collections.singleton(primitive);
				}

			} else {
				// -- add the geometries to collectedForFrame
				groupsForThisFrame.add(group);
			}
		}

		// Now regenerate those geometries outside of lock.
		if (regenerate != null) {
			for (final OsmPrimitive p : regenerate) {
				regenerateGeometryFor(p, generator);
			}
		}
	}

	private void regenerateGeometryFor(OsmPrimitive primitive,
			StyledGeometryGenerator generator) {
		if (generator != null) {
			final List<RecordedOsmGeometries> geometries = generator
					.runFor(primitive);
			collectedForFrameMerger.addMergeables(primitive, geometries);
		} else {
			// Schedule for background rendering.
			synchronized (scheduledForBackgroundMutex) {
				if (scheduledForBackground == null) {
					scheduledForBackground = new MergeGroup();
				}
				scheduledForBackground.merge(primitive,
						Collections.<RecordedOsmGeometries> emptyList());
				synchronized (this) {
					recordedForPrimitive.put(primitive, scheduledForBackground);
				}
				if (scheduledForBackground.getPrimitives().size() >= BACKGROUND_BULK_SIZE) {
					scheduleForBackground();
				}
			}
		}
	}

	private void scheduleForBackground() {
		regenerator.schedule(scheduledForBackground);
		scheduledForBackground = null;
	}

	/**
	 * Adds invalidation listeners to the dataset so that the cache gets
	 * invalidated when the data changes.
	 *
	 * @param data
	 *            The {@link DataSet}
	 */
	public void addListeners(DataSet data, MapView mapView) {
		selectionListener = new SelectionObserver(data);
		// Note: We cannot register this on a specific data set.
		DataSet.addSelectionListener(selectionListener);
		data.addDataSetListener(changeObserver);
		zoomListener = new ZoomListener(mapView);
		NavigatableComponent.addZoomChangeListener(zoomListener);
	}

	/**
	 * Deallocates all resources used for this cache.
	 */
	public void dispose() {
		invalidateAll();
	}

	/**
	 * Invalidates all entries of this cache immediately so that they need to be
	 * re-created on the next draw call.
	 *
	 * @see #invalidateAllLater()
	 */
	public void invalidateAll() {
		while (!recordedForPrimitive.isEmpty()) {
			final MergeGroup first = recordedForPrimitive.values().iterator().next();
			// this might also remove some other primitives form the cache.
			invalidate(first);
		}
	}

	/**
	 * Removes the listeners added by {@link #addListeners(DataSet)}
	 *
	 * @param data
	 *            The {@link DataSet}
	 */
	public void removeListeners(DataSet data) {
		DataSet.removeSelectionListener(selectionListener);
		data.removeDataSetListener(changeObserver);
		NavigatableComponent.removeZoomChangeListener(zoomListener);
	}

	public BackgroundGemometryRegenerator getRegenerator() {
		return regenerator;
	}
}
