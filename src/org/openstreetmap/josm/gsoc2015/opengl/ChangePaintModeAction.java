package org.openstreetmap.josm.gsoc2015.opengl;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ToggleAction;
import org.openstreetmap.josm.gsoc2015.opengl.MapViewPaintModeState.PaintMode;
import org.openstreetmap.josm.tools.Shortcut;

final class ChangePaintModeAction extends ToggleAction
		implements MapViewPaintModeState.PaintModeListener {
	private static final ChangePaintModeAction instance = new ChangePaintModeAction();

	public static ChangePaintModeAction getInstance() {
		return instance;
	}
	
	private ChangePaintModeAction() {
		super("opengl-toggle (TODO)", null, "", Shortcut.registerShortcut(
				"menu:view:opengl", tr("Toggle OpenGL view"),
				KeyEvent.VK_W, Shortcut.ALT_CTRL_SHIFT), true);
		putValue("toolbar", "toggle-opengl");
		Main.toolbar.register(this);
		MapViewPaintModeState.getInstance()
				.addPaintModeListener(this, true);
	}

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.main.hasEditLayer());
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		MapViewPaintModeState.PaintMode mode = MapViewPaintModeState.getInstance()
				.getCurrentPaintMode();
		MapViewPaintModeState.PaintMode[] modes = PaintMode.values();
		MapViewPaintModeState.PaintMode nextMode = modes[(mode.ordinal() + 1) % modes.length];
		MapViewPaintModeState.getInstance().setCurrentPaintMode(nextMode);
	}

	@Override
	public void paintModeChanged(MapViewPaintModeState.PaintMode newMode) {
		setSelected(newMode == PaintMode.OPENGL);
		notifySelectedState();
	}
}