package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.util.List;

import javax.media.opengl.GL2;

import org.jogamp.glg2d.GLGraphics2D;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.DrawStats;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.GLState;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordedOsmGeometries;
import org.openstreetmap.josm.gsoc2015.opengl.osm.StyledGeometryGenerator.ChacheDataSupplier;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;

/**
 * This is our implementaion of the map renderer. It renders a MapCSS styled map
 * using OpenGL and a geometry cache.
 *
 * @author Michael Zangl
 *
 */
public class OpenGLStyledMapRenderer extends StyledMapRenderer {

	/**
	 * Create a new renderer.
	 *
	 * @param g
	 *            The graphics.
	 * @param nc
	 *            The map view
	 * @param isInactiveMode
	 *            If we are in inactive mode.
	 */
	public OpenGLStyledMapRenderer(GLGraphics2D g, NavigatableComponent nc,
			boolean isInactiveMode) {
		super(g, nc, isInactiveMode);
	}

	private final class RepaintListener implements FullRepaintListener {
		private boolean repaintRequested;

		@Override
		public synchronized void requestRepaint() {
			// only call nc.repaint() once.
			if (!repaintRequested) {
				repaintRequested = true;
				nc.repaint();
			}
		}

		/**
		 * Called on every paint call to clear the repaint request flag.
		 */
		public synchronized void paining() {
			repaintRequested = false;
		}
	}

	/**
	 * This class is a snapshot of the current MapView for which we are
	 * rendering styles.
	 * <p>
	 * This allows us to do the rendering while the map view state still
	 * changes.
	 * <p>
	 * The style generation / point computation does not use this state. Changes
	 * to JOSM are required to allow this.
	 *
	 * @author Michael Zangl
	 */
	public static class StyleGenerationState implements ChacheDataSupplier {
		/**
		 * Maximum time to run in Milliseconds. After this time, no new
		 * geometries are generated in this frame and we just collect old ones.
		 */
		public static int MAX_TIME = 50;
		/**
		 * MAximum number of geometries to immediately generate on each frame.
		 */
		public static int MAX_GEOMETRIES_GENERATED = 5000;
		/**
		 * The current circum.
		 *
		 * @see MapView#getDist100Pixel()
		 */
		private final double circum;
		/**
		 * The current view position.
		 */
		private final ViewPosition viewPosition;

		private int geometriesGenerated = 0;

		private long startTime = 0;

		private boolean enoughGometriesGenerated;

		private final boolean renderVirtualNodes, isInactiveMode;

		private final NavigatableComponent cacheKey;

		public StyleGenerationState(double circum, ViewPosition viewPosition,
				boolean renderVirtualNodes, boolean isInactiveMode,
				NavigatableComponent cacheKey) {
			super();
			this.circum = circum;
			this.viewPosition = viewPosition;
			this.renderVirtualNodes = renderVirtualNodes;
			this.isInactiveMode = isInactiveMode;
			this.cacheKey = cacheKey;
			startTime = System.currentTimeMillis();
		}

		@Override
		public NavigatableComponent getCacheKey() {
			return cacheKey;
		}

		public boolean renderVirtualNodes() {
			return renderVirtualNodes;
		}

		public boolean isInactiveMode() {
			return isInactiveMode;
		}

		@Override
		public double getCircum() {
			return circum;
		}

		/**
		 * Get the current view position used for rendering.
		 *
		 * @return The {@link ViewPosition}
		 */
		public ViewPosition getViewPosition() {
			return viewPosition;
		}

		/**
		 * Tests if we should generate one more geometry.
		 *
		 * @return <code>true</code> if the new geometry should be generated
		 *         now.
		 */
		public synchronized boolean shouldGenerateGeometry() {
			return !enoughGometriesGenerated;
		}

		/**
		 * This should be called on every generation of a new geometry.
		 */
		public synchronized void incrementGenerationCounter() {
			geometriesGenerated++;
			if (geometriesGenerated % 5 == 0) {
				if (startTime + MAX_TIME < System.currentTimeMillis()) {
					enoughGometriesGenerated = true;
				}
			}

			if (geometriesGenerated > MAX_GEOMETRIES_GENERATED) {
				enoughGometriesGenerated = true;
			}
		}
	}

	private final RepaintListener repaintListener = new RepaintListener();

	/**
	 * The style manager that does the management of the cache and all styles to
	 * draw.
	 */
	private StyleGenerationManager manager = null;

	@Override
	public synchronized void render(final DataSet data,
			boolean renderVirtualNodes, Bounds bounds) {
		if (manager == null) {
			manager = new StyleGenerationManager(data, repaintListener);
		} else if (!manager.usesDataSet(data)) {
			throw new IllegalArgumentException("Wrong DataSet provided.");
		}

		repaintListener.paining();

		final BBox bbox = bounds.toBBox();
		getSettings(renderVirtualNodes);
		data.getReadLock().lock();
		try {
			final long time1 = System.currentTimeMillis();
			// StyleWorkQueue styleWorkQueue = new StyleWorkQueue(data);
			// styleWorkQueue.setArea(bbox);
			final StyleGenerationState sgs = new StyleGenerationState(getCircum(),
					ViewPosition.from(nc), renderVirtualNodes, isInactiveMode,
					nc);
			final List<RecordedOsmGeometries> geometries = manager.getDrawGeometries(
					bbox, sgs);
			final long time2 = System.currentTimeMillis();

			DrawStats.reset();

			final GL2 gl = ((GLGraphics2D) g).getGLContext().getGL().getGL2();
			final GLState state = new GLState(gl);
			state.initialize(ViewPosition.from(nc));
			for (final RecordedOsmGeometries s : geometries) {
				s.draw(gl, state);
			}
			state.done();
			final long time4 = System.currentTimeMillis();

			drawVirtualNodes(data, bbox);
			final long time5 = System.currentTimeMillis();
			System.out.println("Create styles: " + (time2 - time1)
					+ "ms, draw: " + (time4 - time2) + "ms, draw virtual: "
					+ (time5 - time4) + "ms; total: " + (time5 - time1) + "ms");
			DrawStats.printStats();

		} finally {
			data.getReadLock().unlock();
		}
	}

	/**
	 * Frees all resources used by this renderer.
	 */
	public void dispose() {
		manager.dispose();
	}

}
