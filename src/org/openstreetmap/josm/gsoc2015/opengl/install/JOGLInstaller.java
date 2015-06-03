package org.openstreetmap.josm.gsoc2015.opengl.install;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.SwingWorker;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gsoc2015.opengl.install.JOGLInstaller.ProgressStatus;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginHandler.DynamicURLClassLoader;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Utils;

/**
 * This class attempts to install JOGL.
 * 
 * @author michael
 */
public class JOGLInstaller extends SwingWorker<Void, ProgressStatus> {

	public static class ProgressStatus {
		private final float progress;
		private final String message;

		public ProgressStatus(float progress, String message) {
			super();
			this.progress = progress;
			this.message = message;
		}

		public float getProgress() {
			return progress;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return "ProgressStatus [progress=" + progress + ", message="
					+ message + "]";
		}

	}

	public interface JOGLInstallProgress {
		/**
		 * Called whenever the installation progress changed.
		 * 
		 * @param progress
		 * @param message
		 */
		void progressChanged(float progress, String message);

		/**
		 * Called when jogl is installed.
		 */
		void joglInstalled();
	}

	private static final JOGLInstallationSync installationSync = new JOGLInstallationSync();
	/**
	 * Just for the progress bar, does not have to be exact.
	 */
	private static final int LIB_COUNT_MAX = 22;
	private JOGLInstallProgress progressMonitor;
	private PluginInformation pluginInformation;

	/**
	 * Synchronizes the installation of jogl.
	 * 
	 * @author michael
	 *
	 */
	private static class JOGLInstallationSync {
		private JOGLInstaller currentInstaller;

		public synchronized void start(JOGLInstaller installer) {
			while (currentInstaller != null) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
			currentInstaller = installer;
		}

		public synchronized void done(JOGLInstaller installer) {
			if (currentInstaller != installer) {
				throw new IllegalStateException(
						"Installer is done but was not started.");
			}
			notifyAll();
		}
	}

	private JOGLInstaller(JOGLInstallProgress progressMonitor,
			PluginInformation pluginInformation) {
		this.progressMonitor = progressMonitor;
		this.pluginInformation = pluginInformation;
	}

	@Override
	protected Void doInBackground() throws Exception {
		try {
			installationSync.start(this);
			if (!isInstalledAndLoaded) {
				attemptToExtract();
			}
			isInstalledAndLoaded = true;
			return null;
		} finally {
			installationSync.done(this);
		}
	}

	@Override
	protected void done() {
		try {
			get();
			progressMonitor.joglInstalled();
		} catch (InterruptedException e) {
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void process(List<ProgressStatus> chunks) {
		for (ProgressStatus chunk : chunks) {
			progressMonitor.progressChanged(chunk.getProgress(),
					chunk.getMessage());
		}
	}

	private void attemptToExtract() throws IOException, ClassNotFoundException {
		File pluginsDirectory = Main.pref.getPluginsDirectory();
		String name = pluginInformation.getName();
		File pluginDir = new File(pluginsDirectory, name);
		pluginDir.mkdirs();

		File versionFile = new File(pluginDir, "jogl-last-update");
		String versionOnDisk = getVersionFileContent(versionFile);
		String currentVersion = pluginInformation.version;

		File joglDir = new File(pluginDir, "lib");

		// If the version file is correct, we assume the right version of jogl
		// is installed correctly.
		if (!versionOnDisk.equals(currentVersion)) {
			File pluginJar = new File(pluginsDirectory, name + ".jar");
			extractJogl(pluginJar, joglDir);
			writeVersionFile(versionFile, currentVersion);
		}

		load(joglDir);

		test();
	}

	private void test() throws ClassNotFoundException {
		Class.forName("javax.media.opengl.awt.GLCanvas", true,
				PluginHandler.getPluginClassLoader());
		Class.forName("javax.media.opengl.awt.GLCanvas", true, getClass()
				.getClassLoader());
	}

	private void load(File joglDir) {
		DynamicURLClassLoader loader = PluginHandler.getPluginClassLoader();

		File joglJar = new File(joglDir, "jogl-all.jar");
		System.out.println("Adding " + joglJar);
		loader.addURL(Utils.fileToURL(joglJar));
		File gluegenJar = new File(joglDir, "gluegen-rt.jar");
		System.out.println("Adding " + gluegenJar);
		loader.addURL(Utils.fileToURL(gluegenJar));
	}

	private void extractJogl(File pluginJar, File joglDir) throws IOException {
		joglDir.mkdirs();

		// get the zip file content
		ZipInputStream zis = new ZipInputStream(new FileInputStream(pluginJar));
		// get the zipped file list entry
		ZipEntry ze = zis.getNextEntry();
		Pattern libPattern = Pattern.compile("resources/lib/([\\w-]+.jar)");
		int libCount = 0;

		while (ze != null) {
			Matcher matcher = libPattern.matcher(ze.getName());
			if (matcher.matches()) {
				// TODO tr
				String name = matcher.group(1);
				progressMonitor.progressChanged((float) libCount
						/ LIB_COUNT_MAX, "Extracting " + name);
				extractTo(zis, new File(joglDir, name));
			}
			ze = zis.getNextEntry();
		}

		zis.closeEntry();
		zis.close();
	}

	private void extractTo(ZipInputStream zis, File file)
			throws FileNotFoundException, IOException {
		try (FileOutputStream out = new FileOutputStream(file)) {
			byte[] buffer = new byte[8192];
			int len;
			while ((len = zis.read(buffer)) != -1) {
				out.write(buffer, 0, len);
			}
			out.close();
		}
	}

	private void writeVersionFile(File versionFile, String currentVersion)
			throws IOException {
		try (FileWriter writer = new FileWriter(versionFile)) {
			writer.write(currentVersion);
		}
	}

	private String getVersionFileContent(File versionFile) throws IOException {
		if (versionFile.exists()) {
			List<String> lines = Files.readAllLines(versionFile.toPath(),
					StandardCharsets.UTF_8);
			if (lines.size() > 0)
				return lines.get(0);
		}
		return "";
	}

	private static boolean isInstalledAndLoaded = false;

	/**
	 * Forces JOGL to be installed and initialized. As soon as it is, the
	 * {@link JOGLInstallProgress#joglInstalled()} method is called.
	 * 
	 * @param progressMonitor
	 * @param pluginInformation
	 */
	public static synchronized void requireJOGLInstallation(
			JOGLInstallProgress progressMonitor,
			PluginInformation pluginInformation) {
		if (isInstalledAndLoaded) {
			progressMonitor.joglInstalled();
		} else {
			// TODO tr
			progressMonitor.progressChanged(0, "Preparing JOGL.");
			new Thread(new JOGLInstaller(progressMonitor, pluginInformation),
					"Install JOGL").start();
		}
	}
}
