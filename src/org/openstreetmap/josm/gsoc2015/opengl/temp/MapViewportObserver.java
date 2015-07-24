package org.openstreetmap.josm.gsoc2015.opengl.temp;

import java.awt.Dimension;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.MapView;

/**
 * We should use events for this.
 * 
 * @author michael
 *
 */
public class MapViewportObserver {
	private MapView mapView;
	private List<MapViewportListener> mapViewportListeners = new CopyOnWriteArrayList<>();

	public interface MapViewportListener {

		void mapViewportChanged();

	}

	public static class MapViewportPosition {
		private final EastNorth center;
		private final double scale;
		private final Dimension size;

		public MapViewportPosition(MapView mapView) {
			center = mapView.getCenter();
			scale = mapView.getScale();
			size = mapView.getSize();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((center == null) ? 0 : center.hashCode());
			long temp;
			temp = Double.doubleToLongBits(scale);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((size == null) ? 0 : size.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MapViewportPosition other = (MapViewportPosition) obj;
			if (center == null) {
				if (other.center != null)
					return false;
			} else if (!center.equals(other.center))
				return false;
			if (Double.doubleToLongBits(scale) != Double
					.doubleToLongBits(other.scale))
				return false;
			if (size == null) {
				if (other.size != null)
					return false;
			} else if (!size.equals(other.size))
				return false;
			return true;
		}
	}

	private MapViewportPosition lastPos;

	public MapViewportObserver(MapView mapView) {
		this.mapView = mapView;
		lastPos = new MapViewportPosition(mapView);

		startTimer();
	}

	private void startTimer() {
		new Timer("MapViewportObserver").schedule(new TimerTask() {
			@Override
			public void run() {
				MapViewportPosition currentPos = new MapViewportPosition(
						mapView);
				if (!currentPos.equals(lastPos)) {
					fireMapViewportChanged();
					System.out.println("Map viewport changed.");
					lastPos = currentPos;
				}
			}
		}, 100, 20);
	}

	public void addMapViewportListener(MapViewportListener l) {
		mapViewportListeners.add(l);
	}

	public void removeMapViewportListener(MapViewportListener l) {
		mapViewportListeners.add(l);
	}

	protected void fireMapViewportChanged() {
		for (MapViewportListener l : mapViewportListeners) {
			l.mapViewportChanged();
		}
	}

}
