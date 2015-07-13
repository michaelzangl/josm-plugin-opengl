package org.openstreetmap.josm.gsoc2015.opengl.osm;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.util.List;

import org.jogamp.glg2d.GLGraphics2D;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.RecordedGeometry;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.DirectDrawRecorder;
import org.openstreetmap.josm.gsoc2015.opengl.geometrycache.Recorder;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.LineElemStyle;

public class OpenGLStyledMapRenderer extends StyledMapRenderer {

	private RecordingStyledMapRenderer recordingRenderer;

	public OpenGLStyledMapRenderer(GLGraphics2D g, NavigatableComponent nc,
			boolean isInactiveMode, PrimitiveManager cache) {
		super(g, nc, isInactiveMode);
		recordingRenderer = new RecordingStyledMapRenderer(
				new DirectDrawRecorder(g.getGLContext().getGL().getGL2()), nc,
				isInactiveMode);
	}

	@Override
	public void render(DataSet data, boolean renderVirtualNodes, Bounds bounds) {
		recordingRenderer.getSettings(renderVirtualNodes);
		super.render(data, renderVirtualNodes, bounds);
	}

	@Override
	protected void renderStyled(List<StyleRecord> allStyleElems) {
		float lastIndex = Float.NEGATIVE_INFINITY;
		for (StyleRecord r : allStyleElems) {
			float index = r.style.zIndex;
			if (lastIndex > index) {
				recordingRenderer.flush();
			}

			//
			// if (r.style instanceof LineElemStyle) {
			r.style.paintPrimitive(r.osm, paintSettings, recordingRenderer,
					(r.flags & FLAG_SELECTED) != 0,
					(r.flags & FLAG_OUTERMEMBER_OF_SELECTED) != 0,
					(r.flags & FLAG_MEMBER_OF_SELECTED) != 0);
			// } else {
			// cachingRenderer.flush();
			// r.style.paintPrimitive(
			// r.osm,
			// paintSettings,
			// this,
			// (r.flags & FLAG_SELECTED) != 0,
			// (r.flags & FLAG_OUTERMEMBER_OF_SELECTED) != 0,
			// (r.flags & FLAG_MEMBER_OF_SELECTED) != 0
			// );
			// }
		}
		recordingRenderer.flush();
	}

}
