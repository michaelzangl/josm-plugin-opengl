package org.openstreetmap.josm.gsoc2015.opengl.geometrycache;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;

import org.jogamp.glg2d.impl.AbstractColorHelper;

public class RecordingColorHelper extends AbstractColorHelper {
	private int activeColor;

	@Override
	public void setColorNoRespectComposite(Color c) {
	    setActiveColor(c, 1);
	}

	@Override
	public void setColorRespectComposite(Color c) {
	    float preMultiplyAlpha;
	    Composite composite = getComposite();
	    if (composite instanceof AlphaComposite) {
	      preMultiplyAlpha = ((AlphaComposite) composite).getAlpha();
	    } else {
	    	preMultiplyAlpha = 1;
	    }

	    setActiveColor(c, preMultiplyAlpha);
	}
	
	private void setActiveColor(Color c, float preMultiplyAlpha) {
	    int rgb = c.getRGB();
	    int alpha = (rgb >> 24 & 0xFF);
	    if (preMultiplyAlpha < .997f) {
	    	alpha *= preMultiplyAlpha;
	    }
	    activeColor = (rgb & 0xffffff) | (alpha << 24);
	}

	public int getActiveColor() {
		return activeColor;
	}

	@Override
	public void setPaintMode() {
	}

	@Override
	public void setXORMode(Color c) {
	}

	@Override
	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
		throw new UnsupportedOperationException("Not suported...");
	}
}
