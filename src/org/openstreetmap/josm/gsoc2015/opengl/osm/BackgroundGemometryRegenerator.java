package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.HashGeometryMerger;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.MergeGroup;
import org.openstreetmap.josm.tools.Pair;

/**
 * This generator generates geometries in the background.
 * <p>
 * It works in rounds. Each round is ended by calling
 * {@link #takeNewMergeGroups()}.
 * <p>
 * During the round, you can call {@link #schedule(MergeGroup)} and
 * {@link #abortSchedule(MergeGroup)} to add/remove merge groups to be
 * regenerated.
 * 
 * @author Michael Zangl
 */
public class BackgroundGemometryRegenerator {

	private final class RegenerationTask implements Runnable {
		private ArrayList<MergeGroup> groupsToUse;

		private HashGeometryMerger merger = new HashGeometryMerger();
		private boolean aborted = false;

		private List<MergeGroup> newGroups;
		private List<OsmPrimitive> removedPrimitives = new ArrayList<>();

		public RegenerationTask(ArrayList<MergeGroup> groupsToUse) {
			super();
			this.groupsToUse = groupsToUse;
		}

		@Override
		public void run() {
			for (MergeGroup g : groupsToUse) {
				for (OsmPrimitive p : g.getPrimitives()) {
					if (aborted) {
						return;
					}
					generateGeometries(p);
				}
			}
			if (aborted) {
				return;
			}
			newGroups = merger.getMergeGroups();
			merger = null;
			doneWithRegenerationTask(this);
		}

		private void generateGeometries(OsmPrimitive p) {
			removedPrimitives.add(p);
			// TODO Auto-generated method stub

		}

		/**
		 * Suggests to abort this task. The aborted flag is set and the task may
		 * be aborted.
		 */
		public void abort() {
			aborted = true;
		}

		public boolean isAborted() {
			return aborted;
		}
	}

	/**
	 * This is a list of merge groups that are invalidated and should be
	 * regenerated if there is time. They can still be used and are regenerated
	 * by background tasks.
	 * <p>
	 * We assume none of them share the same geometry.
	 */
	private final LinkedHashSet<MergeGroup> invalidatedMergeGroups = new LinkedHashSet<>();

	/**
	 * Old merge groups that are currently regenerated or that have been
	 * regenerated and are done.
	 */
	private final HashMap<MergeGroup, RegenerationTask> runningMergeGroups = new HashMap<>();

	/**
	 * A list of tasks that are done and ready to be used.
	 */
	private final LinkedList<RegenerationTask> doneRegenerationTasks = new LinkedList<>();

	private final Object regnenerationMutex = new Object();

	private FullRepaintListener repaintListener;

	public BackgroundGemometryRegenerator(FullRepaintListener repaintListener) {
		this.repaintListener = repaintListener;
	}

	public void schedule(MergeGroup mergeGroup) {
		synchronized (regnenerationMutex) {
			abortSchedule(mergeGroup); // <- might be improved.
			invalidatedMergeGroups.add(mergeGroup);

			regnenerationMutex.notify();
		}
	}

	protected RegenerationTask generateRegenerationTask() {
		synchronized (regnenerationMutex) {
			if (invalidatedMergeGroups.isEmpty()) {
				return null;
			}
			ArrayList<MergeGroup> groups = new ArrayList<>();
			for (MergeGroup g : invalidatedMergeGroups) {
				groups.add(g);
				if (groups.size() > 5) {
					break;
				}
			}
			invalidatedMergeGroups.removeAll(groups);
			return new RegenerationTask(groups);
		}
	}

	protected void waitForNewWork() {
		synchronized (regnenerationMutex) {
			if (invalidatedMergeGroups.isEmpty()) {
				try {
					regnenerationMutex.wait();
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public void resumeAllWaiting() {
		synchronized (regnenerationMutex) {
			regnenerationMutex.notifyAll();
		}
	}

	/**
	 * Called when geometries have been regenerated.
	 * 
	 * @param regenerationTask
	 */
	protected void doneWithRegenerationTask(RegenerationTask regenerationTask) {
		synchronized (regnenerationMutex) {
			if (!regenerationTask.isAborted()) {
				doneRegenerationTasks.add(regenerationTask);
				repaintListener.requestRepaint();
			}
		}
	}

	/**
	 * Aborts the regeneration of that merge group.
	 * <p>
	 * The merge group will not be in the result of the next
	 * {@link #takeNewMergeGroups()} call.
	 * 
	 * @param mergeGroup
	 */
	public void abortSchedule(MergeGroup mergeGroup) {
		synchronized (regnenerationMutex) {
			invalidatedMergeGroups.remove(mergeGroup);
			RegenerationTask running = runningMergeGroups.get(mergeGroup);
			if (running != null) {
				running.abort();
				doneRegenerationTasks.remove(running);
				removeRunning(running);
				// now re-schedule all other merge groups
				for (MergeGroup m : running.groupsToUse) {
					if (!m.equals(mergeGroup)) {
						invalidatedMergeGroups.add(m);
					}
				}
			}
		}
	}

	private void removeRunning(RegenerationTask running) {
		for (MergeGroup m : running.groupsToUse) {
			runningMergeGroups.remove(m);
		}
	}

	/**
	 * Retrieves a list of merge groups that were regenerated. The groups were
	 * generated so that they replace an exact set of original merge groups.
	 * 
	 * @return The primitives for which the new geometry was generated and the
	 *         merge groups. Mind that some primitives may have lost all
	 *         geometries and are in no merge group any more.
	 */
	public Pair<Collection<OsmPrimitive>, Collection<MergeGroup>> takeNewMergeGroups() {
		synchronized (regnenerationMutex) {
			ArrayList<MergeGroup> groups = new ArrayList<>();
			ArrayList<OsmPrimitive> primitives = new ArrayList<>();
			for (RegenerationTask d : doneRegenerationTasks) {
				groups.addAll(d.newGroups);
				primitives.addAll(d.removedPrimitives);
				removeRunning(d);
			}
			doneRegenerationTasks.clear();
			System.out.println("Regenerated " + groups.size()
					+ " merge groups for " + primitives.size() + " primitives");
			return new Pair<Collection<OsmPrimitive>, Collection<MergeGroup>>(
					primitives, groups);
		}
	}
}
