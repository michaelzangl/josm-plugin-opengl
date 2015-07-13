package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

import org.jogamp.glg2d.GLGraphics2D;
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

	@Override
	protected void displaySegments(GeneralPath path,
			GeneralPath orientationArrows, GeneralPath onewayArrows,
			GeneralPath onewayArrowsCasing, Color color, BasicStroke line,
			BasicStroke dashes, Color dashedColor) {
//		color = isInactiveMode ? inactiveColor : color;
//		g.setStroke(line);
//		g.draw(path);
//		System.out.println("Got segments...");
		super.displaySegments(path, orientationArrows, onewayArrows,
				onewayArrowsCasing, color, line, dashes, dashedColor);
	}
	
	public void flush() {
	}
}
