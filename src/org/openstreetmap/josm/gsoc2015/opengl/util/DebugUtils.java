package org.openstreetmap.josm.gsoc2015.opengl.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;

public class DebugUtils {
	public static String getStackTrace() {
		final StringWriter sw = new StringWriter();
		new Exception("Stack trace").printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	public static <T> Collection<T> findDuplicates(Collection<T> geometries) {
		final HashSet<T> foundOnce = new HashSet<>();
		final HashSet<T> foundMultipleTimes = new HashSet<>();
		for (final T g : geometries) {
			if (!foundOnce.add(g)) {
				foundMultipleTimes.add(g);
				System.out.println("found duplicate: " + g);
			}
		}
		return foundMultipleTimes;
	}
}
