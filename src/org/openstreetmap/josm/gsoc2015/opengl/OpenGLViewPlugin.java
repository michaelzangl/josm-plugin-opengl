package org.openstreetmap.josm.gsoc2015.opengl;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gsoc2015.opengl.MapViewPaintModeState.PaintMode;
import org.openstreetmap.josm.gsoc2015.opengl.MapViewPaintModeState.PaintModeListener;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.GBC;

public class OpenGLViewPlugin extends Plugin {

	private class OpenGLSwitchPanel extends JPanel implements PaintModeListener {
		private Component java2dPanel;
		private Component openGLPanel;

		public OpenGLSwitchPanel(Component java2dPanel, Component openGLPanel) {
			super(new CardLayout());
			this.java2dPanel = java2dPanel;
			this.openGLPanel = openGLPanel;
			add(java2dPanel, PaintMode.JAVA2D.toString());
			add(openGLPanel, PaintMode.OPENGL.toString());
			MapViewPaintModeState state = MapViewPaintModeState.getInstance();
			state.addPaintModeListener(this, true);
			setBorder(BorderFactory.createLineBorder(Color.red));
		}

		@Override
		public void paintModeChanged(PaintMode newMode) {
			CardLayout cl = (CardLayout) (getLayout());
			cl.show(this, newMode.toString());
		}
	}

	public OpenGLViewPlugin(PluginInformation info) {
		super(info);
		System.out.println("OpenGLViewPlugin()");
		addToolbarButton();

		addMenuButton();
	}

	@Override
	public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
		super.mapFrameInitialized(oldFrame, newFrame);
		System.out.println("mapFrameInitialized(" + oldFrame + ", " + newFrame
				+ ")");
		if (newFrame != null) {
			MapView mapView = newFrame.mapView;
			OpenGLSwitchPanel openGlContainer = addOpenglView(mapView);

		}
	}

	private class PaintModeMenuButton extends JCheckBoxMenuItem {
		public PaintModeMenuButton() {
			this(ChangePaintModeAction.getInstance());
		}

		private PaintModeMenuButton(ChangePaintModeAction action) {
			super(action);
			setAccelerator(action.getShortcut().getKeyStroke());
			action.addButtonModel(getModel());
		}
	}

	private void addMenuButton() {
		JMenu menu = Main.main.menu.viewMenu;
		menu.add(new PaintModeMenuButton(), 1);
	}

	private void addToolbarButton() {
		ToolbarPreferences.ActionParser actionParser = new ToolbarPreferences.ActionParser(
				null);
		String action = actionParser
				.saveAction(new ToolbarPreferences.ActionDefinition(
						ChangePaintModeAction.getInstance()));
		System.out.println("Action: " + action);
		Main.toolbar.addCustomButton(action, -1, false);
	}

	private OpenGLSwitchPanel addOpenglView(MapView mapView) {
		Container mapViewOuter = mapView.getParent();
		int index = Arrays.asList(mapViewOuter.getComponents())
				.indexOf(mapView);
		mapViewOuter.remove(mapView);

		Component openGLView = new OpenGLMapView(mapView, getPluginInformation());
		OpenGLSwitchPanel mapViewContainer = new OpenGLSwitchPanel(mapView,
				openGLView);
		mapViewOuter.add(mapViewContainer, GBC.std().fill(), index);
		mapViewOuter.revalidate();

		return mapViewContainer;
	}
}
