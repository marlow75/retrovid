package pl.dido.video;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;

import javax.imageio.ImageIO;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.bytedeco.javacpp.tools.Logger;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import pl.dido.image.petscii.PetsciiRenderer;
import pl.dido.image.utils.Gfx;
import pl.dido.image.utils.Utils;
import pl.dido.video.compression.Compression2;
import pl.dido.video.compression.Compression3;
import pl.dido.video.petscii.PetsciiVideoConfig;
import pl.dido.video.petscii.PetsciiVideoTab;
import pl.dido.video.utils.MarkableByteArrayOutputStream;
import pl.dido.video.utils.VideoUtils;

public class RetroVID {
	private static final Logger log = Logger.create(RetroVID.class);
	private static String BAD_COMPRESSION = "Bad compression !!!";

	private final static int FRAMES_PER_SECOND = 10;
	private int skip;

	protected JFrame frame;
	protected final PetsciiVideoConfig petsciiVideoConfig = new PetsciiVideoConfig();

	protected File selectedFile;
	protected FFmpegFrameGrabber grabber;

	protected int frameRate;
	protected final Java2DFrameConverter conv = new Java2DFrameConverter();

	protected String default_path;

	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					final RetroVID video = new RetroVID();
					video.frame.setVisible(true);
					video.frame.setLocationRelativeTo(null);
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
					petsciiVideoConfig.startFrame = source.getValue();

				displaySingleFrame();
			}
		});

		PetsciiVideoTab.btnPlay.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				try {
					frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					playFragment(20); // 20 seconds
				} finally {
					frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
		});

		PetsciiVideoTab.btnRecord.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {				
				try {
					frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					convertFragment(); // as much frames as can be
				} finally {
					frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
		});

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

						selectedFile = fc.getSelectedFile();

						grabber = new FFmpegFrameGrabber(selectedFile);
						grabber.start();

						default_path = selectedFile.getAbsolutePath();
						frameRate = (int) grabber.getFrameRate();

						final int end = grabber.getLengthInFrames() / frameRate;
						final int mid = end / 2;

						final Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
						labelTable.put(0, new JLabel("0"));
						labelTable.put(mid, new JLabel(Integer.toString(mid) + "s"));
						labelTable.put(end, new JLabel(Integer.toString(end) + "s"));

						skip = frameRate / FRAMES_PER_SECOND;

						petsciiVideoConfig.startFrame = 0;
						PetsciiVideoTab.sldFrame.setValue(0);

						PetsciiVideoTab.sldFrame.setEnabled(true);
						PetsciiVideoTab.sldFrame.setMaximum(end);
						PetsciiVideoTab.sldFrame.setLabelTable(labelTable);

						PetsciiVideoTab.btnPlay.setEnabled(true);
						PetsciiVideoTab.btnRecord.setEnabled(true);

						displaySingleFrame();
					} catch (final Exception ex) {
						// TODO:
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

	private void displaySingleFrame() {
		if (selectedFile != null)
			try {
				grabber.setFrameNumber(petsciiVideoConfig.startFrame * frameRate);
				frame2petscii(new PetsciiRenderer(petsciiVideoConfig), false);
			} catch (final Exception ex) {
				JOptionPane.showMessageDialog(null, "ERROR", "Can't display single fragment !!!",
						JOptionPane.ERROR_MESSAGE);
			}
	}

	private void playFragment(final float time) {
		final int start = petsciiVideoConfig.startFrame * frameRate;
		int end = (int) (start + time * frameRate);

		end = end > grabber.getLengthInFrames() ? grabber.getLengthInFrames() : end;
		final PetsciiRenderer petscii = new PetsciiRenderer(petsciiVideoConfig);
		try {
			grabber.setFrameNumber(start);

			for (int i = start; i < end; i++)
				frame2petscii(petscii, i % 2 == 0); // slide show
		} catch (final Exception ex) {
			JOptionPane.showMessageDialog(null, "ERROR", "Can't play fragment !!!", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void convertFragment() {
		convert2petscii(selectedFile, petsciiVideoConfig.startFrame * frameRate, skip);
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
		if (skip)
			return;

		petscii.setImage(Gfx.scaleWithStretching(conv.convert(frame), 320, 200));
		petscii.imageProcess();

		PetsciiVideoTab.movie.setImage(petscii.getImage());
		PetsciiVideoTab.movie.showImage();

		frame.close();
	}

	public void convert2petscii(final File selectedFile, final int frameNumber, final int skip) {
		try (final FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(selectedFile)) {
			int frames = 0;

			final String dir = selectedFile.getParent() + File.separator + "PICS";
			Utils.createDirectory(dir);

			frameGrabber.start();
			frameGrabber.setFrameNumber(frameNumber);

			Frame frame = frameGrabber.grabFrame();

			final PetsciiRenderer petscii = new PetsciiRenderer(petsciiVideoConfig);
			final MarkableByteArrayOutputStream out = new MarkableByteArrayOutputStream(64 * 1024);

			boolean first = true;
			int oldScreen[] = null, oldNibble[] = null;

			try {
				int i = 0, j = 0, sum = 0, headerSize = 0, lastFrameSize = 0;

				while (true) {
					if (frame.image != null) {
						if (i % skip == 0) {
							petscii.setImage(conv.convert(frame));
							petscii.imageProcess();

							if (first) {
								headerSize = VideoUtils.saveHeader(out, petscii.backgroundColor, petscii.screen,
										Compression2.packNibble(petscii.nibble));

								first = false;
								sum += headerSize;
							} else {
								int compressedScreen[] = null;
								int p[] = null;

								switch (petsciiVideoConfig.compression) {
								case CODES:
									compressedScreen = Compression2.compress(oldScreen, petscii.screen, petscii.nibble);

									if (Compression2.checkAddress(compressedScreen)) {
										JOptionPane.showMessageDialog(null, "ERROR", BAD_COMPRESSION,
												JOptionPane.ERROR_MESSAGE);
										break;
									}

									// check & verify compression
									p = Compression2.decompress(oldScreen, oldNibble, compressedScreen);
									for (int k = 0; k < 1000; k++)
										if (p[k] != petscii.screen[k]) {
											JOptionPane.showMessageDialog(null, "ERROR", BAD_COMPRESSION,
													JOptionPane.ERROR_MESSAGE);
											break;
										}
									break;
								case CODES_COLOR:
									compressedScreen = Compression3.compress(oldScreen, petscii.screen, oldNibble,
											petscii.nibble);

									if (Compression3.checkAddress(compressedScreen)) {
										JOptionPane.showMessageDialog(null, "ERROR", BAD_COMPRESSION,
												JOptionPane.ERROR_MESSAGE);
										break;
									}

									// check & verify compression
									p = Compression3.decompress(oldScreen, oldNibble, compressedScreen);
									for (int k = 0; k < 1000; k++)
										if (p[k] != petscii.screen[k]) {
											JOptionPane.showMessageDialog(null, "ERROR", BAD_COMPRESSION,
													JOptionPane.ERROR_MESSAGE);
											break;
										}
								}

								lastFrameSize = VideoUtils.saveScreen(out, petscii.backgroundColor, compressedScreen);
								sum += lastFrameSize;

								frames++;
							}

							oldScreen = petscii.screen.clone();
							oldNibble = petscii.nibble.clone();

							ImageIO.write(petscii.getImage(), "jpg",
									new File(dir + File.separatorChar + String.format("%2d", j++) + ".jpg"));

							if (sum > 0xc7fe) { // max C64 file size
								out.rollback(lastFrameSize);

								log.debug("Grabbed: " + --frames + " frames");
								break;
							}
						}

						i += 1;
					}

					frame = frameGrabber.grabImage();
				}

				out.setByteAtMarkedPosition((byte) frames);

				frameGrabber.stop();
				frameGrabber.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}

			out.flush();
			out.close();

			final String fileName = dir + File.separator + selectedFile.getName().substring(0, 7) + "F"
					+ petsciiVideoConfig.startFrame + ".prg";
			final BufferedOutputStream fs = new BufferedOutputStream(new FileOutputStream(new File(fileName)), 8192);

			fs.write(out.toByteArray());
			fs.close();
		} catch (final IOException e) {
			JOptionPane.showMessageDialog(null, "ERROR", "IOError !!!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
}