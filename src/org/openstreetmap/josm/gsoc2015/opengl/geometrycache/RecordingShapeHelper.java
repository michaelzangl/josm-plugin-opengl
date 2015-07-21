package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;

import org.jogamp.glg2d.impl.AbstractShapeHelper;
import org.jogamp.glg2d.impl.gl2.FastLineVisitor;

public class RecordingShapeHelper extends AbstractShapeHelper {

	private final RecordingTesselatorVisitor fillVisitor;
	private final RecordingStrokeLineVisitor lineVisitor;
	private RecordingColorHelper colorHelper;

	public RecordingShapeHelper(RecordingColorHelper colorHelper,
			Recorder recorder) {
		this.colorHelper = colorHelper;
		fillVisitor = new RecordingTesselatorVisitor(colorHelper, recorder);
		lineVisitor = new RecordingStrokeLineVisitor(colorHelper, recorder);
	}

	@Override
	public void draw(Shape shape) {
		Stroke stroke = getStroke();
		if (stroke instanceof BasicStroke) {
			BasicStroke basicStroke = (BasicStroke) stroke;
			if (basicStroke.getDashArray() == null) {
				lineVisitor.setStroke(basicStroke);
				traceShape(shape, lineVisitor);
				return;
			}
		}
		fill(stroke.createStrokedShape(shape));
	}

	@Override
	protected void fill(Shape shape, boolean isDefinitelySimpleConvex) {
		traceShape(shape, fillVisitor);
	}

}
