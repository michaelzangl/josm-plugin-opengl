package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.awt.Rectangle;

import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.Recorder;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordingGraphics2D;
import org.openstreetmap.josm.gui.NavigatableComponent;

/**
 * This is a map renderer that caches the resoults fo the render call instead of displaying them.
 * @author michael
 *
 */
public class RecordingStyledMapRenderer extends StyledMapRenderer {
	public RecordingStyledMapRenderer(Recorder recorder, NavigatableComponent nc,
			boolean isInactiveMode) {
		super(new RecordingGraphics2D(recorder), nc, isInactiveMode);
	}

	public void flush() {
	}

	public void setClipBounds(Rectangle clipBounds) {
		g.setClip(clipBounds);
	}

	public void dispose() {
		((RecordingGraphics2D) g).dispose();
	}
}
