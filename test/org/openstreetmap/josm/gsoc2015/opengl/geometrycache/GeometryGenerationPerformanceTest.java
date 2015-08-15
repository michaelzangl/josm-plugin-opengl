package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.imageio.ImageIO;
import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;
import javax.swing.SwingUtilities;

import org.jogamp.glg2d.GLGraphics2D;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.PerformanceTestUtils.PerformanceTestTimer;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmDataGenerator;
import org.openstreetmap.josm.data.osm.OsmDataGenerator.DataGenerator;
import org.openstreetmap.josm.data.osm.OsmDataGenerator.NodeDataGenerator;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OsmLayerDrawer;
import org.openstreetmap.josm.gsoc2015.opengl.osm.OpenGLStyledMapRenderer.StyleGenerationState;
import org.openstreetmap.josm.gsoc2015.opengl.pool.SimpleBufferPool;
import org.openstreetmap.josm.gsoc2015.opengl.pool.VertexBufferPool;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.SetGlobalStyle;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.util.GuiHelper;

import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

/**
 * Tests the performance of geometry generation.
 * <p>
 * Each test simulates a full draw.
 * 
 * @author Michael Zangl
 *
 */
public class GeometryGenerationPerformanceTest {
	/**
	 * If we should also run it using java2d to see the difference.
	 */
	private static final boolean RUN_REFERENCE = true;

	private static class GeometryGenerationRunner implements Runnable {

		private static final int WIDTH = 500;
		private static final int HEIGHT = 500;
		private String description;
		private String css;
		private DataGenerator dataGenerator;

		public GeometryGenerationRunner(String description, String css,
				DataGenerator dataGenerator) {
			this.description = description;
			this.css = css;
			this.dataGenerator = dataGenerator;
		}

		@Override
		public void run() {
			System.out.println(description + "...");
			StyleGenerationState.MAX_GEOMETRIES_GENERATED = Integer.MAX_VALUE;
			MapCSSStyleSource source = new MapCSSStyleSource(css);
			PerformanceTestTimer timer = PerformanceTestUtils
					.startTimer("MapCSSStyleSource#loadStyleSource(...) for "
							+ description);
			source.loadStyleSource();
			SetGlobalStyle.setGlobalStyle(source);
			timer.done();

			GLAutoDrawable drawable = createGLContext();

			timer = PerformanceTestUtils.startTimer("Generate test data for "
					+ description);
			OsmDataLayer osmLayer = dataGenerator.createDataLayer();
			OsmLayerDrawer drawer = new OsmLayerDrawer(osmLayer);

			MapView mapView = Main.map.mapView;
			mapView.addLayer(osmLayer);
			mapView.setBounds(new Rectangle(WIDTH, HEIGHT));
			mapView.zoomTo(new ProjectionBounds(new EastNorth(0, 0),
					new EastNorth(1, 1)));
			// mapView.zoomTo(new Bounds(new LatLon(0, 0), new LatLon(1, 1)));
			Bounds box = mapView.getLatLonBounds(new Rectangle(mapView
					.getSize()));
			timer.done();

			GLGraphics2D g2d = new GLGraphics2D();
			g2d.prePaint(drawable.getContext());
			g2d.setColor(Color.WHITE);
			g2d.fillRect(0, 0, WIDTH, HEIGHT);
			timer = PerformanceTestUtils.startTimer("first drawLayer() for "
					+ description);
			drawer.drawLayer(g2d, mapView, box);
			timer.done();

			g2d.setColor(Color.WHITE);
			g2d.fillRect(0, 0, WIDTH, HEIGHT);
			timer = PerformanceTestUtils.startTimer("second drawLayer() for "
					+ description);
			drawer.drawLayer(g2d, mapView, box);
			timer.done();

			BufferedImage im = new AWTGLReadBufferUtil(drawable.getGLProfile(),
					true).readPixelsToBufferedImage(drawable.getGL(), 0, 0,
					WIDTH, HEIGHT, true);

			try {
				File dir = new File("test_data/results");
				dir.mkdirs();
				ImageIO.write(im, "png", new File(dir, "test result for "
						+ description + ".png"));
				// Generate reference image...
				if (RUN_REFERENCE) {
					MapPaintStyles.getStyles().clearCached();
					BufferedImage expected = new BufferedImage(WIDTH, HEIGHT,
							BufferedImage.TYPE_3BYTE_BGR);
					Graphics2D g = expected.createGraphics();
					g.setClip(0, 0, WIDTH, HEIGHT);
					timer = PerformanceTestUtils
							.startTimer("first Java2D paint() for "
									+ description);
					paintOnImage(osmLayer, mapView, box, g);
					timer.done();
					timer = PerformanceTestUtils
							.startTimer("second Java2D paint() for "
									+ description);
					paintOnImage(osmLayer, mapView, box, g);
					timer.done();
					ImageIO.write(expected, "png", new File(dir,
							"expected for " + description + ".png"));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			drawable.destroy();

			System.out.println();
		}

		private void paintOnImage(OsmDataLayer osmLayer, MapView mapView,
				Bounds box, Graphics2D g) {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, WIDTH, HEIGHT);
			osmLayer.paint(g, mapView, box);
		}

		private GLAutoDrawable createGLContext() {
			GLProfile glp = GLProfile.getDefault();
			GLCapabilities caps = new GLCapabilities(glp);
			caps.setHardwareAccelerated(true);
			caps.setDoubleBuffered(false);
			caps.setAlphaBits(8);
			caps.setRedBits(8);
			caps.setBlueBits(8);
			caps.setGreenBits(8);
			caps.setOnscreen(false);
			GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);

			GLAutoDrawable drawable = factory.createOffscreenAutoDrawable(
					factory.getDefaultDevice(), caps,
					new DefaultGLCapabilitiesChooser(), WIDTH, HEIGHT);
			drawable.display();
			drawable.getContext().makeCurrent();
			return drawable;
		}

	}

	/**
	 * Prepare the test.
	 */
	@BeforeClass
	public static void createJOSMFixture() {
		new JOSMFixture("test_data/performance-josm.home").init(true);
	}

	@Test
	public void testEmpty() throws Exception {
		DataGenerator generator = new DataGenerator("empty") {
			@Override
			protected void fillData(DataSet ds) {
			}
		};
		runTest(new GeometryGenerationRunner("empty", "", generator));
	}

	@Test
	public void testTriangles() throws Exception {
		runTest(new GeometryGenerationRunner(
				"tiangles",
				"node { fill-color: blue; symbol-size: 20; symbol-shape: triangle }",
				getRandomNodeGenerator()));
	}

	@Test
	public void testOctagons() throws Exception {
		// Should be merged and drawn with fast drawer,
		runTest(new GeometryGenerationRunner(
				"octagons",
				"node {symbol-fill-color: red; symbol-size: 20; symbol-shape: octagon }",
				getRandomNodeGenerator()));
	}

	@Test
	public void testMultipleOctagons() throws Exception {
		// Should be merged and drawn with fast drawer,
		String css = "";
		for (int i = 1; i <= 20; i++) {
			int c = new Color(i / 20f, 1 - i / 20f, 0).getRGB() & 0xffffff;
			css += String
					.format("node::l%1$d {symbol-fill-color: #%2$06x; symbol-size: %1$d; symbol-shape: octagon; major-z-index: %3$d }\n",
							i, c, 20 - i);
		}
		runTest(new GeometryGenerationRunner("multiple ocatagons", css,
				getRandomNodeGenerator()));
	}

	@Test
	public void testOctagonsWithBorder() throws Exception {
		runTest(new GeometryGenerationRunner(
				"octagons with border",
				"node {symbol-fill-color: red; symbol-size: 20; symbol-stroke-width: 2; symbol-stroke-opacity: 0.5; symbol-stroke-color: blue; symbol-shape: octagon }",
				getRandomNodeGenerator()));
		System.out.println(SimpleBufferPool.miss.getAndSet(0));
		System.out.println(SimpleBufferPool.hit.getAndSet(0));
	}

	private NodeDataGenerator getRandomNodeGenerator() {
		return new OsmDataGenerator.NodeDataGenerator("many-nodes", 1000000) {
		};
	}

	private void runTest(GeometryGenerationRunner geometryGenerationRunner)
			throws Exception {
		SwingUtilities.invokeAndWait(geometryGenerationRunner);
	}
}
