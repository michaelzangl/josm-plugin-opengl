package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class DrawStats {
	private static Hashtable<Integer, List<Integer>> drawSizes = new Hashtable<>();
	private static int colorChanges = 0;

	public static void drawArrays(int drawMode, int points) {
		List<Integer> list = drawSizes.get(drawMode);
		if (list == null) {
			list = new ArrayList<>();
			drawSizes.put(drawMode, list);
		}
		list.add(points);
	}

	public static void setColor(int rgba) {
		colorChanges++;
	}

	public static void printStats() {
		for (Integer drawMode : drawSizes.keySet()) {
			printStats(drawMode);
		}
	}

	private static void printStats(Integer drawMode) {
		List<Integer> list = drawSizes.get(drawMode);
		int shortDraws = 0;
		for (Integer i : list) {
			if (i < 15) {
				shortDraws++;
			}
		}
		System.out.println(String.format(
				"   drawMode = %02d, draws: %05d, short: %05d", drawMode,
				list.size(), shortDraws));
	}

	public static void reset() {
		drawSizes.clear();
		colorChanges = 0;
	}
}
