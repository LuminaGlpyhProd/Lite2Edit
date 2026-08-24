package goldendelicios.lite2edit;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

public class Lite2Edit {
	private static final String LITEMATIC_EXT = ".litematic";
	private static final String SCHEM_EXT = ".schem";
	private static File dir = new File(System.getProperty("user.dir"));
	private static File lastOutputDir;
	private static PrintStream errorFile;

	public static void main(String[] args) {
		try {
			Files.deleteIfExists(Paths.get("errors.log"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (args.length == 0)
			openGUI();
		else if (args[0].equals("--convert")) {
			int start = 1;
			String target = Converter.LEGACY_LABEL;
			if (args.length > 2 && args[1].equals("--target")) {
				target = args[2];
				start = 3;
				if (!Converter.supportedVersionLabels().contains(target)) {
					System.err.println("Error: unknown --target version '" + target + "'");
					System.err.println("Supported versions: " + String.join(", ", Converter.supportedVersionLabels()));
					return;
				}
			}

			File[] files = new File[args.length - start];
			for (int i = start; i < args.length; i++) {
				String filename = args[i];
				File file = new File(filename);
				if (!(isLitematic(filename) || isSchem(filename)) || !file.isFile()) {
					System.err.println("Error: '" + filename + "' is not a valid .litematic or .schem file");
					return;
				}
				files[i - start] = file;
			}
			convertCli(files, target);
		}
		else {
			System.err.println("Invalid arguments.");
			System.err.println("Correct usage: `java -jar Lite2Edit.jar` (Opens GUI)");
			System.err.println("Correct usage: `java -jar Lite2Edit.jar --convert [--target <version>] [Path to file 1] [Path to file 2]...`");
			System.err.println("Supported --target versions: " + String.join(", ", Converter.supportedVersionLabels()));
		}
	}

	private static boolean isLitematic(String filename) {
		return filename.endsWith(LITEMATIC_EXT);
	}

	private static boolean isSchem(String filename) {
		return filename.endsWith(SCHEM_EXT);
	}

	private static void openGUI() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) {}

		JFrame frame = new JFrame("Lite2Edit");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setMinimumSize(new Dimension(480, 380));
		frame.setSize(560, 460);
		frame.setLocationRelativeTo(null);

		JLabel title = new JLabel("Lite2Edit");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
		title.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

		JLabel subtitle = new JLabel(
			"<html>Converts <b>.litematic</b> and <b>.schem</b> files, retargeted to the Minecraft version you pick below.<br>"
			+ "Pick one or more files below — the source file type is detected automatically.</html>");
		subtitle.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));
		headerPanel.add(title);
		headerPanel.add(Box.createVerticalStrut(4));
		headerPanel.add(subtitle);

		JLabel versionLabel = new JLabel("Target version:");
		JComboBox<String> versionCombo = new JComboBox<>(Converter.supportedVersionLabels().toArray(new String[0]));
		versionCombo.setSelectedItem(Converter.LEGACY_LABEL);

		JButton browse = new JButton("Browse files...");
		JButton clearLog = new JButton("Clear log");
		JButton openOutput = new JButton("Open output folder");
		openOutput.setEnabled(false);

		JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 4));
		buttonPanel.add(versionLabel);
		buttonPanel.add(versionCombo);
		buttonPanel.add(browse);
		buttonPanel.add(clearLog);
		buttonPanel.add(openOutput);

		JLabel statusLabel = new JLabel("Status: Idle");
		JProgressBar progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);

		JPanel statusPanel = new JPanel(new GridBagLayout());
		statusPanel.setBorder(BorderFactory.createEmptyBorder(2, 14, 6, 14));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(0, 0, 4, 0);
		statusPanel.add(statusLabel, gbc);
		gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
		statusPanel.add(progressBar, gbc);

		JTextArea logArea = new JTextArea();
		logArea.setEditable(false);
		logArea.setLineWrap(true);
		logArea.setWrapStyleWord(true);
		logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		JScrollPane logScroll = new JScrollPane(logArea);
		logScroll.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createEmptyBorder(0, 14, 12, 14),
			BorderFactory.createTitledBorder("Log")));

		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(headerPanel, BorderLayout.NORTH);
		topPanel.add(buttonPanel, BorderLayout.CENTER);
		topPanel.add(statusPanel, BorderLayout.SOUTH);

		frame.getContentPane().add(BorderLayout.NORTH, topPanel);
		frame.getContentPane().add(BorderLayout.CENTER, logScroll);

		appendLog(logArea, "Ready. Choose files to begin.");

		clearLog.addActionListener(e -> logArea.setText(""));
		openOutput.addActionListener(e -> {
			if (lastOutputDir != null && lastOutputDir.isDirectory()) {
				try {
					Desktop.getDesktop().open(lastOutputDir);
				} catch (IOException ex) {
					appendLog(logArea, "Could not open output folder: " + ex.getMessage());
				}
			}
		});

		browse.addActionListener(event -> {
			JFileChooser fc = new JFileChooser(dir);
			fc.setMultiSelectionEnabled(true);
			fc.setFileFilter(new ConvertibleFileFilter());

			int value = fc.showOpenDialog(frame);
			if (value != JFileChooser.APPROVE_OPTION) return;

			File[] inputs = fc.getSelectedFiles();
			String target = (String) versionCombo.getSelectedItem();
			browse.setEnabled(false);
			progressBar.setValue(0);
			progressBar.setString("0 / " + inputs.length);
			statusLabel.setText("Status: Working...");

			new ConversionWorker(inputs, target, logArea, progressBar, statusLabel, browse, openOutput).execute();
		});

		frame.setVisible(true);
	}

	private static final class ConversionWorker extends SwingWorker<Void, String> {
		private final File[] inputs;
		private final String target;
		private final JTextArea logArea;
		private final JProgressBar progressBar;
		private final JLabel statusLabel;
		private final JButton browse;
		private final JButton openOutput;
		private int successCount = 0;
		private int errorCount = 0;

		ConversionWorker(File[] inputs, String target, JTextArea logArea, JProgressBar progressBar, JLabel statusLabel, JButton browse, JButton openOutput) {
			this.inputs = inputs;
			this.target = target;
			this.logArea = logArea;
			this.progressBar = progressBar;
			this.statusLabel = statusLabel;
			this.browse = browse;
			this.openOutput = openOutput;
		}

		@Override
		protected Void doInBackground() {
			Converter.Logger logger = this::publish;
			for (int i = 0; i < inputs.length; i++) {
				File input = inputs[i];
				publish("--- " + input.getName() + " -> " + target + " (" + (i + 1) + "/" + inputs.length + ") ---");
				long start = System.currentTimeMillis();
				try {
					File parent = input.getAbsoluteFile().getParentFile();
					dir = parent;
					lastOutputDir = parent;
					List<File> outputs = Converter.convertToVersion(input, parent, target, logger);

					if (outputs.isEmpty()) {
						publish(input.getName() + " is not a valid file");
						errorCount++;
					}
					else {
						successCount++;
					}
					long time = System.currentTimeMillis() - start;
					publish("Done in " + time + "ms");
				} catch (Throwable e) {
					publish("Error while converting " + input.getName() + ": " + e);
					handleException(e);
					errorCount++;
				}
				int completed = i + 1;
				SwingUtilities.invokeLater(() -> {
					progressBar.setValue(completed * 100 / inputs.length);
					progressBar.setString(completed + " / " + inputs.length);
				});
			}
			return null;
		}

		@Override
		protected void process(List<String> chunks) {
			for (String line : chunks) appendLog(logArea, line);
		}

		@Override
		protected void done() {
			progressBar.setValue(100);
			progressBar.setString(successCount + " / " + inputs.length + " succeeded");
			String summary = errorCount == 0
				? "Status: Done — " + successCount + " file(s) converted"
				: "Status: Done with " + errorCount + " error(s) — " + successCount + " succeeded";
			statusLabel.setText(summary);
			appendLog(logArea, summary);
			browse.setEnabled(true);
			openOutput.setEnabled(lastOutputDir != null);
		}
	}

	private static void convertCli(File[] inputs, String target) {
		Converter.Logger logger = System.out::println;
		int successCount = 0, errorCount = 0;
		for (int i = 0; i < inputs.length; i++) {
			File input = inputs[i];
			System.out.println("--- " + input.getName() + " -> " + target + " (" + (i + 1) + "/" + inputs.length + ") ---");
			try {
				File parent = input.getAbsoluteFile().getParentFile();
				List<File> outputs = Converter.convertToVersion(input, parent, target, logger);

				if (outputs.isEmpty()) {
					System.out.println(input.getName() + " is not a valid file");
					errorCount++;
				}
				else {
					successCount++;
				}
			} catch (Throwable e) {
				System.out.println("Error while converting " + input.getName() + ": " + e);
				handleException(e);
				errorCount++;
			}
		}
		System.out.println("Finished: " + successCount + " succeeded, " + errorCount + " failed");
	}

	private static void handleException(Throwable e) {
		e.printStackTrace();
		if (errorFile == null) {
			try {
				errorFile = new PrintStream("errors.log");
			} catch (Exception e2) {
				System.err.println("Failed to write to errors.log");
				e2.printStackTrace();
				return;
			}
		}
		e.printStackTrace(errorFile);
		errorFile.flush();
	}

	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

	private static void appendLog(JTextArea area, String message) {
		Runnable r = () -> {
			area.append("[" + TIME_FORMAT.format(new Date()) + "] " + message + "\n");
			area.setCaretPosition(area.getDocument().getLength());
		};
		if (SwingUtilities.isEventDispatchThread()) r.run();
		else SwingUtilities.invokeLater(r);
	}

	private static final class ConvertibleFileFilter extends FileFilter {
		@Override
		public String getDescription() {
			return "Litematics and Schematics (*.litematic, *.schem)";
		}

		@Override
		public boolean accept(File f) {
			return f.isDirectory() || isLitematic(f.getName()) || isSchem(f.getName());
		}
	}

}
