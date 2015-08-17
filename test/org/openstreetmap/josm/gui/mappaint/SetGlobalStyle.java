package org.openstreetmap.josm.gui.mappaint;


public class SetGlobalStyle {
	public static void setGlobalStyle(StyleSource s) {
        MapPaintStyles.getStyles().clear();
        MapPaintStyles.getStyles().add(s);
	}
}
