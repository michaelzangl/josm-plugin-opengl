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
import org.openstreetmap.josm.gsoc2015.opengl.util.DebugUtils;
import org.openstreetmap.josm.gui.NavigatableComponent;

public class OpenGLStyledMapRenderer extends StyledMapRenderer {

	public OpenGLStyledMapRenderer(GLGraphics2D g, NavigatableComponent nc,
			boolean isInactiveMode) {
		super(g, nc, isInactiveMode);
	}

	private final class RepaintListener implements FullRepaintListener {
		private boolean repaintRequested;

		@Override
		public synchronized void requestRepaint() {
			if (!repaintRequested) {
				repaintRequested = true;
				System.out.println("Repaint: " + DebugUtils.getStackTrace());
				nc.repaint();
			}
		}

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
		public static int MAX_GEOMETRIES_GENERATED = 5000;
		private final double circum;
		private final ViewPosition viewPosition;

		int geometriesGenerated = 0;
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

		public ViewPosition getViewPosition() {
			return viewPosition;
		}

		/**
		 * Tests if we should generate one more geometry.
		 * 
		 * @return
		 */
		public synchronized boolean shouldGenerateGeometry() {
			return !enoughGometriesGenerated;
		}

		public synchronized void incrementDrawCounter() {
			geometriesGenerated++;
			if (geometriesGenerated > MAX_GEOMETRIES_GENERATED) {
				enoughGometriesGenerated = true;
			}
		}
	}

	private final RepaintListener repaintListener = new RepaintListener();
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

		BBox bbox = bounds.toBBox();
		getSettings(renderVirtualNodes);
		data.getReadLock().lock();
		try {
			long time1 = System.currentTimeMillis();
			// StyleWorkQueue styleWorkQueue = new StyleWorkQueue(data);
			// styleWorkQueue.setArea(bbox);
			StyleGenerationState sgs = new StyleGenerationState(getCircum(),
					ViewPosition.from(nc), renderVirtualNodes, isInactiveMode,
					nc);
			List<RecordedOsmGeometries> geometries = manager.getDrawGeometries(
					bbox, sgs);
			long time2 = System.currentTimeMillis();

			DrawStats.reset();

			GL2 gl = ((GLGraphics2D) g).getGLContext().getGL().getGL2();
			GLState state = new GLState(gl);
			state.initialize(ViewPosition.from(nc));
			for (RecordedOsmGeometries s : geometries) {
				s.draw(gl, state);
			}
			state.done();
			long time4 = System.currentTimeMillis();

			drawVirtualNodes(data, bbox);
			long time5 = System.currentTimeMillis();
			System.out.println("Create styles: " + (time2 - time1)
					+ "ms, draw: " + (time4 - time2) + "ms, draw virtual: "
					+ (time5 - time4) + "ms; total: " + (time5 - time1) + "ms");
			DrawStats.printStats();

		} finally {
			data.getReadLock().unlock();
		}
	}

	public void dispose() {
		manager.dispose();
	}

}
