package pl.dido.video;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import pl.dido.image.AboutGui;
import pl.dido.image.petscii.PetsciiRenderer;
import pl.dido.image.utils.Gfx;
import pl.dido.image.utils.Utils;
import pl.dido.video.petscii.PetsciiGrabberTask;
import pl.dido.video.petscii.PetsciiVideoConfig;
import pl.dido.video.petscii.PetsciiVideoTab;

public class RetroVID implements ActionListener, PropertyChangeListener {
	private final static int FRAMES_PER_SECOND = 10;

	protected final PetsciiVideoConfig petsciiVideoConfig = new PetsciiVideoConfig();
	protected FFmpegFrameGrabber grabber;

	protected String default_path;
	protected PetsciiGrabberTask task;

	protected ProgressMonitor progressMonitor;
	protected JFrame frame;

	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					final RetroVID app = new RetroVID();
					app.frame.setVisible(true);
					app.frame.setLocationRelativeTo(null);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public RetroVID() {
		frame = new JFrame("RetroVID");
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(Utils.getResourceAsURL("retro.png")));
		frame.setResizable(false);
		frame.setBounds(0, 0, 670, 450);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());

		final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setFont(new Font("Tahoma", Font.PLAIN, 12));

		final Button btnLoad = new Button("Load file...");

		frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.addTab("PETSCII", null, PetsciiVideoTab.petsciiTab(petsciiVideoConfig), null);
		tabbedPane.addTab("About", null, AboutGui.aboutTab(), null);

		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent changeEvent) {
				final JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent.getSource();
				final int index = sourceTabbedPane.getSelectedIndex();
				
				btnLoad.setVisible(!"About".equals(tabbedPane.getTitleAt(index)));
			}
		});

		PetsciiVideoTab.sldFrame.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent e) {
				final JSlider source = (JSlider) e.getSource();

				if (!source.getValueIsAdjusting())
					petsciiVideoConfig.startFrame = source.getValue() * petsciiVideoConfig.frameRate;

				displaySingleFrame();
			}
		});

		PetsciiVideoTab.btnPlay.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				playFragment(20); // 20 seconds
			}
		});

		PetsciiVideoTab.btnRecord.addActionListener(this);

		btnLoad.setBackground(new Color(0, 128, 128));
		btnLoad.setFont(new Font("Dialog", Font.BOLD, 12));
		btnLoad.setForeground(new Color(255, 255, 255));
		btnLoad.setPreferredSize(new Dimension(143, 34));

		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final JFileChooser fc = new JFileChooser(default_path);
				final FileFilter filter = new FileNameExtensionFilter("Choose movie", "mp4");

				fc.setFileFilter(filter);
				final int returnVal = fc.showOpenDialog(frame);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {
						if (grabber != null) {
							grabber.stop();
							grabber.close();
						}

						petsciiVideoConfig.selectedFile = fc.getSelectedFile();
						grabber = new FFmpegFrameGrabber(petsciiVideoConfig.selectedFile);
						grabber.start();

						default_path = petsciiVideoConfig.selectedFile.getAbsolutePath();
						petsciiVideoConfig.frameRate = (int) grabber.getFrameRate();

						final int end = grabber.getLengthInFrames() / petsciiVideoConfig.frameRate;
						final int mid = end / 2;

						final Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
						labelTable.put(0, new JLabel("0"));
						labelTable.put(mid, new JLabel(Integer.toString(mid) + "s"));
						labelTable.put(end, new JLabel(Integer.toString(end) + "s"));

						petsciiVideoConfig.skip = petsciiVideoConfig.frameRate / FRAMES_PER_SECOND;
						petsciiVideoConfig.startFrame = 0;

						PetsciiVideoTab.sldFrame.setValue(0);
						PetsciiVideoTab.sldFrame.setEnabled(true);
						PetsciiVideoTab.sldFrame.setMaximum(end);
						PetsciiVideoTab.sldFrame.setLabelTable(labelTable);

						PetsciiVideoTab.btnPlay.setEnabled(true);
						PetsciiVideoTab.btnRecord.setEnabled(true);

						displaySingleFrame();
					} catch (final Exception ex) {
						// TODO: Nothing
					}
				}
			}
		});

		final Button btnClose = new Button("Close");
		btnClose.setBackground(new Color(128, 0, 64));
		btnClose.setForeground(SystemColor.text);
		btnClose.setPreferredSize(new Dimension(67, 34));

		final JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS));
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		buttonsPanel.add(Box.createHorizontalGlue());
		buttonsPanel.add(btnLoad);
		buttonsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonsPanel.add(btnClose);

		frame.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				System.exit(0);
			}
		});
	}

	public void actionPerformed(final ActionEvent event) {
		converterButtons(false);

		progressMonitor = new ProgressMonitor(this.frame, "Converting", "", 0, 100);
		progressMonitor.setProgress(0);
		
		task = new PetsciiGrabberTask(petsciiVideoConfig);
		task.addPropertyChangeListener(this);
		task.execute();
	}

	private void displaySingleFrame() {
		if (petsciiVideoConfig.selectedFile != null)
			try {
				grabber.setFrameNumber(petsciiVideoConfig.startFrame);
				frame2petscii(new PetsciiRenderer(petsciiVideoConfig), false);
			} catch (final Exception ex) {
				JOptionPane.showMessageDialog(null, "ERROR", "Can't display single fragment !!!",
						JOptionPane.ERROR_MESSAGE);
			}
	}

	private void playFragment(final float time) {
		try {
			converterButtons(false);
			final int start = petsciiVideoConfig.startFrame;
			int end = (int) (start + time * petsciiVideoConfig.frameRate);

			end = end > grabber.getLengthInFrames() ? grabber.getLengthInFrames() : end;
			final PetsciiRenderer petscii = new PetsciiRenderer(petsciiVideoConfig);

			grabber.setFrameNumber(start);
			for (int i = start; i < end; i++)
				frame2petscii(petscii, i % 3 == 0); // slide show

		} catch (final Exception ex) {
			JOptionPane.showMessageDialog(null, "ERROR", "Can't play fragment !!!", JOptionPane.ERROR_MESSAGE);
		} finally {
			converterButtons(true);
		}
	}

	private void converterButtons(final boolean p) {
		PetsciiVideoTab.btnPlay.setEnabled(p);
		PetsciiVideoTab.btnRecord.setEnabled(p);
	}

	private Frame getFrame() {
		Frame frame = null;

		try {
			frame = grabber.grabFrame();
			if (frame == null)
				return null;

			while (frame.image == null) {
				frame.close();
				frame = grabber.grabFrame();
			}
		} catch (final Exception e) {
			return null;
		}

		return frame;
	}
	
	private void frame2petscii(final PetsciiRenderer petscii, final boolean skip) {
		Frame frame = getFrame();
		if (!skip)
			try (final Java2DFrameConverter conv = new Java2DFrameConverter()) {

				petscii.setImage(Gfx.scaleWithStretching(conv.convert(frame), 320, 200));
				petscii.imageProcess();

				PetsciiVideoTab.movie.setImage(petscii.getImage());
				PetsciiVideoTab.movie.showImage();
			}

		frame.close();
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if ("progress" == evt.getPropertyName()) {
			final int progress = (Integer) evt.getNewValue();
			progressMonitor.setProgress(progress);

			if (progressMonitor.isCanceled()) {
				task.cancel(true);
				converterButtons(true);
				
				return;
			}
			
			if (task.isDone()) {
				try {
					final Integer result = task.get();

					if (result == PetsciiGrabberTask.ERROR) 	
						JOptionPane.showMessageDialog(null, "ERROR", "Unexpected error !!!", JOptionPane.ERROR_MESSAGE);
					else
					if (result == PetsciiGrabberTask.IO_ERROR) 	
						JOptionPane.showMessageDialog(null, "ERROR", "IO error !!!", JOptionPane.ERROR_MESSAGE);
					
				} catch (final Exception e) {
					e.printStackTrace();
				} finally {
					this.converterButtons(true);
				}
			}
		}
	}
}