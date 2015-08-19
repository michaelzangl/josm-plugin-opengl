package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * This is a simple class that keeps track of the number of draw calls used for
 * the current frame.
 *
 * @author Michael Zangl
 *
 */
public class DrawStats {
	private static Hashtable<Integer, List<Integer>> drawSizes = new Hashtable<>();
	private static int colorChanges = 0;

	/**
	 * Register a draw arrays call.
	 *
	 * @param drawMode
	 *            The draw mode.
	 * @param points
	 *            The number of points drawn.
	 */
	public static void drawArrays(int drawMode, int points) {
		List<Integer> list = drawSizes.get(drawMode);
		if (list == null) {
			list = new ArrayList<>();
			drawSizes.put(drawMode, list);
		}
		list.add(points);
	}

	/**
	 * Register a set color call.
	 *
	 * @param rgba
	 *            The new color.
	 */
	public static void setColor(int rgba) {
		colorChanges++;
	}

	/**
	 * Print the draw statistics to the console.
	 */
	public static void printStats() {
		for (final Integer drawMode : drawSizes.keySet()) {
			printStats(drawMode);
		}
		System.out.println(String
				.format("   color changes: %02d", colorChanges));
	}

	private static void printStats(Integer drawMode) {
		final List<Integer> list = drawSizes.get(drawMode);
		int shortDraws = 0;
		for (final Integer i : list) {
			if (i < 15) {
				shortDraws++;
			}
		}
		System.out.println(String.format(
				"   drawMode = %02d, draws: %05d, short: %05d", drawMode,
				list.size(), shortDraws));
	}

	/**
	 * reset the statistics.
	 */
	public static void reset() {
		drawSizes.clear();
		colorChanges = 0;
	}
}
