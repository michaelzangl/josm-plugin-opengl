package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.StyleSource;

public class SetGlobalStyle {
	public static void setGlobalStyle(StyleSource s) {
        MapPaintStyles.getStyles().clear();
        MapPaintStyles.getStyles().add(s);
	}
}
