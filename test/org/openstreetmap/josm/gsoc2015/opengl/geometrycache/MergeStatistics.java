package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.PerformanceTestUtils.PerformanceTestTimer;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OpenGLStyledMapRenderer.StyleGenerationState;
import org.openstreetmap.josm.gsoc2015.opengl.osm.StyleGenerationManager;
import org.openstreetmap.josm.gsoc2015.opengl.osm.ViewPosition;
import org.openstreetmap.josm.gsoc2015.opengl.util.DebugUtils;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;

public class MergeStatistics {
	/*
	 * ------------------------ configuration section
	 * ----------------------------
	 */
	/**
	 * The path to the style file used for rendering.
	 */
	public static String STYLE_FILE = "styles/standard/elemstyles.mapcss";

	@BeforeClass
	public static void createJOSMFixture() {
		JOSMFixture.createPerformanceTestFixture().init(true);
	}

	@Test
	public void testMergeability() throws NoSuchMethodException,
			SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException,
			IllegalDataException, IOException {
		loadStyle(STYLE_FILE);
		DataSet ds = loadData("../josm-plugin-opengl/data_nodist/city.osm");

		MapView mv = Main.map.mapView;
		mv.addLayer(new OsmDataLayer(ds, "test", new File(STYLE_FILE)));

		mv.zoomTo(ds.getDataSourceBoundingBox());

		StyleGenerationManager manager = new StyleGenerationManager(ds);
		PerformanceTestTimer timer = PerformanceTestUtils
				.startTimer("generate geometries");

		List<RecordedOsmGeometries> geometries = manager.getDrawGeometries(
				Main.getProjection().getWorldBoundsLatLon().toBBox(),
				new StyleGenerationState(mv.getDist100Pixel(), ViewPosition
						.from(mv), true, false, mv) {
					@Override
					public synchronized boolean hasGeneratedAllGeometries() {
						return true;
					}

					@Override
					public synchronized boolean shouldGenerateGeometry() {
						return true;
					}
				});
		timer.done();

		System.out.println("We got " + geometries.size() + " geometries.");

		assertEquals("Geometries have dupplicates", 0,
				DebugUtils.findDuplicates(geometries).size());

		System.out.println("Of those " + countMergeables(geometries, .99)
				+ " pairs would be mergeable.");
		System.out.println("Of those " + countMergeables(geometries, .6)
				+ " pairs might be mergeable.");
	}

	private String countMergeables(List<RecordedOsmGeometries> geometries,
			double minMergeRating) {
		int count = 0;
		int inSameMergeGroup = 0;
		for (int i = 0; i < geometries.size(); i++) {
			RecordedOsmGeometries g = geometries.get(i);
			for (int j = i + 1; j < geometries.size(); j++) {
				RecordedOsmGeometries g2 = geometries.get(j);
				float rating = g.getCombineRating(g2);
				if (rating > minMergeRating) {
					count++;
					if (g.mergeGroup == g2.mergeGroup) {
						inSameMergeGroup++;
					}
				}
			}
		}
		return count + " (" + inSameMergeGroup + " in the same merge group)";
	}

	private void loadStyle(String file) throws NoSuchMethodException,
			SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		MapCSSStyleSource source = new MapCSSStyleSource(new SourceEntry(file,
				"test style", "a test style", true // active
				));
		source.loadStyleSource();
		if (!source.getErrors().isEmpty()) {
			fail("Failed to load style file ''${STYLE_FILE}''. Errors: ${source.errors}");
		}
		Method method = ElemStyles.class.getDeclaredMethod("setStyleSources",
				Collection.class);
		method.setAccessible(true);
		method.invoke(MapPaintStyles.getStyles(), Collections.singleton(source));
	}

	private DataSet loadData(String file) throws IllegalDataException,
			IOException {
		return OsmReader.parseDataSet(
				Compression.getUncompressedFileInputStream(new File(file)),
				null);
	}
}
