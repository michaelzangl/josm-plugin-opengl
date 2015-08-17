package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class HashGeometryMerger extends GeometryMerger  {
	private static final int ACTIVE_GROUPS = 16;
	private static final boolean DEBUG = false;
	private MergeGroup[] activeMergeGroups = new MergeGroup[ACTIVE_GROUPS];
	private int[] mergeGroupHashCount = new int[ACTIVE_GROUPS];
	private Hashtable<Integer, byte[]> hashUses = new Hashtable<>();

	private int getBestMergeables(Collection<RecordedOsmGeometries> geos) {
		int[] hashUsedCount = new int[ACTIVE_GROUPS];
		for (RecordedOsmGeometries g : geos) {
			for (int h : g.getCombineHashes()) {
				byte[] hashUsed = hashUses.get(h);
				if (hashUsed == null) {
					continue;
				}
				for (int i = 0; i < ACTIVE_GROUPS; i++) {
					hashUsedCount[i] += hashUsed[i] & 0xff;
				}
			}
		}

		// Now do a fun trick: We place the hash used count in the upper half of
		// the int, the slot index in the lower half.

		for (int i = 0; i < ACTIVE_GROUPS; i++) {
			int weightedCount = hashUsedCount[i] * 256 / (mergeGroupHashCount[i] + 1);
			if (DEBUG) {
				System.out.println("Rated for slot " + i + ": " + weightedCount);
			}
			hashUsedCount[i] = 0x7fffffff & (weightedCount << 8) + i;
		}
		Arrays.sort(hashUsedCount);

		for (int i = ACTIVE_GROUPS - 1; i > ACTIVE_GROUPS - 6; i--) {
			int slotToUse = hashUsedCount[i] & ((1 << 8) - 1);
			MergeGroup group = activeMergeGroups[slotToUse];
			if (group != null) {
				return slotToUse;
			}
		}
		return -1;
	}
	
	private int replacementClock = 0;

	@Override
	public synchronized void addMergeables(OsmPrimitive primitive,
			Collection<RecordedOsmGeometries> geometries) {
		if (DEBUG) {
			System.out.println("SEARCHING FOR " + geometries);
			dump();
		}
		int groupIndex = getBestMergeables(geometries);
		if (groupIndex < 0) {
			MergeGroup group = new MergeGroup();
			// now let's add a new group
			for (byte[] v : hashUses.values()) {
				v[replacementClock] = 0;
			}
			activeMergeGroups[replacementClock] = group;
			int hashes = 0;
			for (RecordedOsmGeometries g :geometries) {
				int[] combineHashes = g.getCombineHashes();
				for (int h : combineHashes) {
					byte[] uses = hashUses.get(h);
					if (uses == null) {
						uses = new byte[ACTIVE_GROUPS];
						hashUses.put(h, uses);
					}
					uses[replacementClock]++;
				}
				hashes += combineHashes.length;
			}
			
			mergeGroupHashCount[replacementClock] = hashes;
			group.merge(primitive, geometries);
			
			replacementClock++;
			if (replacementClock >= ACTIVE_GROUPS) {
				// TODO: Do a clean of unused hashUses.
				replacementClock = 0;
			}
			mergeGroups.add(group);
		} else {
			MergeGroup group = activeMergeGroups[groupIndex];
			group.merge(primitive, geometries);
			if (!group.moreMergesRecommended()) {
				activeMergeGroups[groupIndex] = null;
				// rates this group really low.
				mergeGroupHashCount[groupIndex] = Integer.MAX_VALUE - 1;
			} else {
				//TODO: Add the new hashes to the old group.
			}
		}
	}

	private void dump() {
		for (int i = 0; i < ACTIVE_GROUPS; i++) {
			System.out.println("Group " + i + ": " + activeMergeGroups[i]);
			System.out.print("    Hashes:");
			for (Entry<Integer, byte[]> h : hashUses.entrySet()) {
				if (h.getValue()[i] > 0) {
					System.out.print(" " + Integer.toHexString(h.getKey()) + "x" + h.getValue()[i]);
				}
			}
			System.out.println();
			System.out.println("    Total hash count: " + mergeGroupHashCount[i]);
		}
	}

}
