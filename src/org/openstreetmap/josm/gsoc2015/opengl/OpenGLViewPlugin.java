package org.openstreetmap.josm.gsoc2015.opengl;

import java.awt.CardLayout;
import java.awt.Container;
import java.util.Arrays;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
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

/**
 * This is the OpenGL plugin entry class.
 *
 * @author Michael Zangl
 *
 */
public class OpenGLViewPlugin extends Plugin {

	private class OpenGLSwitchPanel extends JPanel implements PaintModeListener {
		/**
		 *
		 */
		private static final long serialVersionUID = -5859334937863977150L;

		public OpenGLSwitchPanel(JComponent java2dPanel, JComponent openGLPanel) {
			super(new CardLayout());
			add(java2dPanel, PaintMode.JAVA2D.toString());
			add(openGLPanel, PaintMode.OPENGL.toString());
			final MapViewPaintModeState state = MapViewPaintModeState.getInstance();
			state.addPaintModeListener(this, true);
		}

		@Override
		public void paintModeChanged(PaintMode newMode) {
			final CardLayout cl = (CardLayout) getLayout();
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
			final MapView mapView = newFrame.mapView;
			addOpenglView(mapView);
		}
	}

	private class PaintModeMenuButton extends JCheckBoxMenuItem {
		/**
		 *
		 */
		private static final long serialVersionUID = 3141819668890426424L;

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
		final JMenu menu = Main.main.menu.viewMenu;
		menu.add(new PaintModeMenuButton(), 1);
	}

	private void addToolbarButton() {
		final ToolbarPreferences.ActionParser actionParser = new ToolbarPreferences.ActionParser(
				null);
		final String action = actionParser
				.saveAction(new ToolbarPreferences.ActionDefinition(
						ChangePaintModeAction.getInstance()));
		System.out.println("Action: " + action);
		Main.toolbar.addCustomButton(action, -1, false);
	}

	private OpenGLSwitchPanel addOpenglView(MapView mapView) {
		final Container mapViewOuter = mapView.getParent();
		final int index = Arrays.asList(mapViewOuter.getComponents())
				.indexOf(mapView);
		mapViewOuter.remove(mapView);

		final JComponent openGLView = new OpenGLMapView(mapView,
				getPluginInformation());
		final OpenGLSwitchPanel mapViewContainer = new OpenGLSwitchPanel(mapView,
				openGLView);
		mapViewOuter.add(mapViewContainer, GBC.std().fill(), index);
		mapViewOuter.revalidate();

		return mapViewContainer;
	}
}
