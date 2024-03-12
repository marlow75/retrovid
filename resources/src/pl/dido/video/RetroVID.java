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
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.bytedeco.javacv.FFmpegFrameGrabber;

import pl.dido.image.utils.Utils;
import pl.dido.video.charset.CharsetVideoGui;
import pl.dido.video.petscii.PetsciiGrabberTask;
import pl.dido.video.petscii.PetsciiVideoConfig;
import pl.dido.video.petscii.PetsciiVideoGui;
import pl.dido.video.utils.GuiUtils;
import pl.dido.video.utils.VideoGui;
import pl.dido.video.utils.VideoPanel;

public class RetroVID {
	protected String default_path;
	protected PetsciiGrabberTask task;

	protected JFrame frame;
	
	protected int currentTabIndex = 0;
	protected VideoPanel tabs[];

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
		tabbedPane.setFont(GuiUtils.std);
		tabs = new VideoPanel[3];
		
		final Button btnLoad = new Button("Load file...");
		tabs[0] = new PetsciiVideoGui(frame);
		tabs[1] = new CharsetVideoGui(frame);
		tabs[2] = new AboutVideoGui();
		
		frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.addTab("C64 PETSCII", null, tabs[0].getTab(), null);
		tabbedPane.addTab("Super CPU CHARSET", null, tabs[1].getTab(), null);
		tabbedPane.addTab("About", null, tabs[2].getTab(), null);

		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent changeEvent) {
				final JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent.getSource();
				currentTabIndex = sourceTabbedPane.getSelectedIndex();
				btnLoad.setVisible(!"About".equals(tabbedPane.getTitleAt(currentTabIndex)));
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
					final VideoGui tab = (VideoGui) tabs[currentTabIndex];
					final PetsciiVideoConfig config = (PetsciiVideoConfig) tab.getConfig();
					
					config.selectedFile = fc.getSelectedFile();

					try (final FFmpegFrameGrabber grabber = tab.getNewGrabber()) {
						grabber.start();

						default_path = config.selectedFile.getAbsolutePath();
						config.frameRate = (int) grabber.getFrameRate();

						final int end = grabber.getLengthInFrames() / config.frameRate;
						final int mid = end / 2;

						final Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
						labelTable.put(0, new JLabel("0"));
						labelTable.put(mid, new JLabel(Integer.toString(mid) + "s"));
						labelTable.put(end, new JLabel(Integer.toString(end) + "s"));

						config.startFrame = 0;

						tab.setSlider(end, labelTable);
						tab.enablePlay(true);
						tab.enableRecord(true);

						tab.displaySingleFrame();
					} catch (final Exception ex) {
						ex.printStackTrace();
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
}