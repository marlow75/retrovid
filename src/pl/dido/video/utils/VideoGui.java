package pl.dido.video.utils;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.ProgressMonitor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import pl.dido.image.renderer.AbstractRenderer;
import pl.dido.image.utils.Gfx;
import pl.dido.video.petscii.PetsciiGrabberTask;

public abstract class VideoGui implements ActionListener, PropertyChangeListener, VideoPanel {
	
	protected FFmpegFrameGrabber grabber;
	
	protected VideoCanvas movie;
	protected JSlider sldFrame;
	
	protected Button btnPlay;
	protected Button btnRecord;
	
	protected JProgressBar progressBar;
	protected VideoConfig config;
	
	protected ProgressMonitor progressMonitor;
	protected JFrame frame;
	
	protected GrabberTask task;
	protected AbstractRenderer renderer;
	
	public VideoGui(final JFrame frame, final AbstractRenderer renderer, final VideoConfig config) {
		this.renderer = renderer;
		this.frame = frame;
		
		this.config = config;
	}

	public JPanel getTab() {
		final JPanel panel = new JPanel();
		panel.setLayout(null);
		
		movie = new VideoCanvas();
		
		movie.setBounds(320, 7, 320, 200);
		movie.setBackground(Color.black);
		
		panel.add(movie);	
		
		final JLabel sizeLabel = new JLabel("select start position (sec)");
		sizeLabel.setFont(GuiUtils.bold);
		sizeLabel.setBounds(320, 215, 200, 20);
		panel.add(sizeLabel);

		sldFrame = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		sldFrame.setBounds(350, 235, 280, 35);
		sldFrame.setFont(GuiUtils.std);
		sldFrame.setEnabled(false);

		sldFrame.setPaintLabels(true);
		sldFrame.setMinorTickSpacing(10);
		sldFrame.setSnapToTicks(false);
		sldFrame.setPaintLabels(true);
		
		panel.add(sldFrame);
		
		btnPlay = new Button("Prev");
		btnPlay.setBackground(new Color(128, 128, 255));
		btnPlay.setForeground(SystemColor.text);
		btnPlay.setPreferredSize(new Dimension(68, 20));
		btnPlay.setBounds(350, 270, 68, 20);
		btnPlay.setEnabled(false);

		btnRecord = new Button("Grab");
		btnRecord.setBackground(new Color(128, 0, 64));
		btnRecord.setForeground(SystemColor.text);
		btnRecord.setPreferredSize(new Dimension(68, 20));
		btnRecord.setBounds(430, 270, 68, 20);
		btnRecord.setEnabled(false);
		
		panel.add(btnPlay);
		panel.add(btnRecord);
		
		sldFrame.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent e) {
				final JSlider source = (JSlider) e.getSource();

				if (!source.getValueIsAdjusting())
					config.startFrame = source.getValue() * config.frameRate;

				displaySingleFrame();
			}
		});

		btnPlay.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				playFragment(2); // 2 seconds
			}
		});

		btnRecord.addActionListener(this);
		return panel;
	}
	
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
					converterButtons(true);
				}
			}
		}
	}
	
	public void actionPerformed(final ActionEvent event) {		
		converterButtons(false);

		progressMonitor = new ProgressMonitor(frame, "Converting", "", 0, 100);
		progressMonitor.setProgress(0);
		
		task = getGrabberTask();
		
		task.addPropertyChangeListener(this);
		task.execute();
	}
	
	protected abstract GrabberTask getGrabberTask();

	public void displaySingleFrame() {
		if (config.selectedFile != null)
			try {
				grabber.close();
				grabber.start();
				grabber.setFrameNumber(config.startFrame);
				
				frame2ascii(renderer);
			} catch (final Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(null, "ERROR", "Can't display single fragment !!!",
						JOptionPane.ERROR_MESSAGE);
			}
	}

	private void playFragment(final int time) {
		try {
			converterButtons(false);
			grabber.close();
			grabber.start();
			
			final int start = config.startFrame;
			int end = (int) (start + time * config.frameRate);
			final int len = grabber.getLengthInFrames();

			end = end > len ? len : end;
			grabber.setFrameNumber(start);
			
			for (int i = start; i < end; i++)
				frame2ascii(renderer); // slide show

		} catch (final Exception ex) {
			JOptionPane.showMessageDialog(null, "ERROR", "Can't play fragment !!!", JOptionPane.ERROR_MESSAGE);
		} finally {
			converterButtons(true);
		}
	}

	private void converterButtons(final boolean p) {
		btnPlay.setEnabled(p);
		btnRecord.setEnabled(p);
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
	
	private void frame2ascii(final AbstractRenderer renderer) {
		Frame frame = getFrame();

		try (final Java2DFrameConverter conv = new Java2DFrameConverter()) {
			
			renderer.setImage(Gfx.scaleWithStretching(conv.convert(frame), config.config.getScreenWidth(), config.config.getScreenHeight()));
			renderer.imageProcess();

			movie.setImage(Gfx.scaleWithStretching(renderer.getImage(), 320, 200));
			movie.showImage();
		}

		frame.close();
	}

	public void setSlider(final int end, final Hashtable<Integer, JLabel> labelTable) {
		sldFrame.setValue(0);
		sldFrame.setEnabled(true);
		sldFrame.setMaximum(end);
		sldFrame.setLabelTable(labelTable);
	}

	public void enablePlay(final boolean b) {
		btnPlay.setEnabled(b);
	}

	public void enableRecord(boolean b) {
		btnRecord.setEnabled(b);
	}

	public FFmpegFrameGrabber getNewGrabber() {
		grabber = new FFmpegFrameGrabber(config.selectedFile);
		return grabber;
	}

	public VideoConfig getConfig() {
		return config;
	}
}