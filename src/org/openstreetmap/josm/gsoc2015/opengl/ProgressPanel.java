package org.openstreetmap.josm.gsoc2015.opengl;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * This is a simple progress panel.
 *
 * @author Michael Zangl
 *
 */
public class ProgressPanel extends JPanel {

	private static final long serialVersionUID = -429949250617089564L;

	private final JProgressBar progress = new JProgressBar();

	private final JLabel text = new JLabel();

	public ProgressPanel() {
		add(progress);
		add(text);
	}

	/**
	 * Displays the new progress.
	 *
	 * @param progress
	 *            The progress in range 0..1
	 * @param message
	 *            The message.
	 */
	public void progressChanged(float progress, String message) {
		System.out.println("JOGL Progress (" + progress + "): " + message);
		this.progress.setValue((int) (progress * 100));
		text.setText(message);
	}
}
