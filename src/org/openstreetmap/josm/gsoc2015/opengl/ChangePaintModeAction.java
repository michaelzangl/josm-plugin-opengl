package org.openstreetmap.josm.gsoc2015.opengl;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.actions.ToggleAction;
import org.openstreetmap.josm.gsoc2015.opengl.MapViewPaintModeState.PaintMode;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This action allows you to change the paint mode between OpenGL and normal
 * Java2D.
 *
 * @author Michael Zangl
 */
final class ChangePaintModeAction extends ToggleAction implements
MapViewPaintModeState.PaintModeListener {
	/**
	 *
	 */
	private static final long serialVersionUID = -5075624956921762216L;
	private static final ChangePaintModeAction instance = new ChangePaintModeAction();

	public static ChangePaintModeAction getInstance() {
		return instance;
	}

	private ChangePaintModeAction() {
		super("Use OpenGL", null, "", Shortcut.registerShortcut(
				"menu:view:opengl", tr("Toggle OpenGL view"), KeyEvent.VK_W,
				Shortcut.ALT_CTRL_SHIFT), true);
		putValue("toolbar", "toggle-opengl");
		MainApplication.getToolbar().register(this);
		MapViewPaintModeState.getInstance().addPaintModeListener(this, true);
	}

	@Override
	protected void updateEnabledState() {
		setEnabled(MainApplication.getLayerManager().getEditLayer() != null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final MapViewPaintModeState.PaintMode mode = MapViewPaintModeState
				.getInstance().getCurrentPaintMode();
		final MapViewPaintModeState.PaintMode[] modes = PaintMode.values();
		final MapViewPaintModeState.PaintMode nextMode = modes[(mode.ordinal() + 1)
		                                                       % modes.length];
		MapViewPaintModeState.getInstance().setCurrentPaintMode(nextMode);
	}

	@Override
	public void paintModeChanged(MapViewPaintModeState.PaintMode newMode) {
		setSelected(newMode == PaintMode.OPENGL);
		notifySelectedState();
	}
}