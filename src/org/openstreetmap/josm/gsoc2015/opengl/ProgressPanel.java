package org.openstreetmap.josm.gsoc2015.opengl;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class ProgressPanel extends JPanel {

	private JProgressBar progress = new JProgressBar();

	private JLabel text = new JLabel();

	public ProgressPanel() {
		add(progress);
		add(text);
	}

	public void progressChanged(float progress, String message) {
		System.out.println("JOGL Progress (" + progress + "): " + message);
		this.progress.setValue((int) (progress * 100));
		text.setText(message);
	}
}
