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
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordedOsmGeometries;
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
 * <p>
 * There need to be worker threads allocated that execute the
 * {@link RegenerationTask}s provided by {@link #generateRegenerationTask()}.
 * They can wait for new tasks to be ready using {@link #waitForNewWork()}
 *
 * @author Michael Zangl
 */
public class BackgroundGemometryRegenerator {

	/**
	 * This is a single regeneration bulk task.
	 * <p>
	 * It re-generates the geometry for a list of merge groups.
	 *
	 * @author Michael Zangl
	 *
	 */
	public final class RegenerationTask {
		private final ArrayList<MergeGroup> groupsToUse;

		private HashGeometryMerger merger = new HashGeometryMerger();
		private boolean aborted = false;

		private List<MergeGroup> newGroups;
		private final List<OsmPrimitive> removedPrimitives = new ArrayList<>();

		/**
		 * Creates a new {@link RegenerationTask}
		 *
		 * @param groupsToUse
		 *            The groups that should be regenerated.
		 */
		public RegenerationTask(ArrayList<MergeGroup> groupsToUse) {
			super();
			this.groupsToUse = groupsToUse;
		}

		/**
		 * Runs the regeneration of the merge groups.
		 *
		 * @param generator
		 *            The generator to use.
		 */
		public void runWithGenerator(StyledGeometryGenerator generator) {
			generator.startRunning();
			try {
				for (final MergeGroup g : groupsToUse) {
					for (final OsmPrimitive p : g.getPrimitives()) {
						if (aborted) {
							return;
						}
						generateGeometries(p, generator);
					}
				}
				if (aborted) {
					return;
				}
				newGroups = merger.getMergeGroups();
				merger = null;
				doneWithRegenerationTask(this);
			} finally {
				generator.endRunning();
			}
		}

		private void generateGeometries(OsmPrimitive p,
				StyledGeometryGenerator generator) {
			removedPrimitives.add(p);
			final List<RecordedOsmGeometries> geometries = generator.runFor(p);
			merger.addMergeables(p, geometries);
		}

		/**
		 * Suggests to abort this task. The aborted flag is set and the task may
		 * be aborted.
		 */
		public void abort() {
			aborted = true;
		}

		/**
		 * Tests if the abort flag was set
		 *
		 * @return The abort flag.
		 */
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

	/**
	 * The repaint listener to notify when new geometries are computed.
	 */
	private final FullRepaintListener repaintListener;

	/**
	 * Creates a new background repainter.
	 *
	 * @param repaintListener
	 *            The repaint listener to notify when new geometries are
	 *            computed.
	 */
	public BackgroundGemometryRegenerator(FullRepaintListener repaintListener) {
		this.repaintListener = repaintListener;
	}

	/**
	 * Schedule a new group to be regenerated.
	 *
	 * @param mergeGroup
	 *            The group to regenerate the geometry for.
	 */
	public void schedule(MergeGroup mergeGroup) {
		synchronized (regnenerationMutex) {
			abortSchedule(mergeGroup); // <- might be improved.
			invalidatedMergeGroups.add(mergeGroup);

			regnenerationMutex.notify();
		}
	}

	/**
	 * Creates a new {@link RegenerationTask} that regenerates some geometries
	 * when run.
	 *
	 * @return The task or <code>null</code> if we do not have more work.
	 */
	public RegenerationTask generateRegenerationTask() {
		synchronized (regnenerationMutex) {
			if (invalidatedMergeGroups.isEmpty()) {
				return null;
			}
			final ArrayList<MergeGroup> groups = new ArrayList<>();
			for (final MergeGroup g : invalidatedMergeGroups) {
				groups.add(g);
				if (groups.size() > 5) {
					break;
				}
			}
			invalidatedMergeGroups.removeAll(groups);
			return new RegenerationTask(groups);
		}
	}

	/**
	 * Blocks until new work appears.
	 */
	protected void waitForNewWork() {
		synchronized (regnenerationMutex) {
			if (invalidatedMergeGroups.isEmpty()) {
				try {
					regnenerationMutex.wait();
				} catch (final InterruptedException e) {
				}
			}
		}
	}

	/**
	 * Resumes all threads that are blocked and waiting for new work.
	 */
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
			final RegenerationTask running = runningMergeGroups.get(mergeGroup);
			if (running != null) {
				running.abort();
				doneRegenerationTasks.remove(running);
				removeRunning(running);
				// now re-schedule all other merge groups
				for (final MergeGroup m : running.groupsToUse) {
					if (!m.equals(mergeGroup)) {
						invalidatedMergeGroups.add(m);
					}
				}
			}
		}
	}

	private void removeRunning(RegenerationTask running) {
		for (final MergeGroup m : running.groupsToUse) {
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
			final ArrayList<MergeGroup> groups = new ArrayList<>();
			final ArrayList<OsmPrimitive> primitives = new ArrayList<>();
			for (final RegenerationTask d : doneRegenerationTasks) {
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
