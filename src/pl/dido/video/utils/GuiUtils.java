package pl.dido.video.utils;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pl.dido.image.petscii.PetsciiConfig;
import pl.dido.image.utils.Config;
import pl.dido.image.utils.Config.FILTER;
import pl.dido.video.petscii.PetsciiVideoConfig;
import pl.dido.video.petscii.PetsciiVideoConfig.COMPRESSION;
import pl.dido.video.utils.VideoConfig.PDF;

public class GuiUtils {

	public final static Font std = new Font("Tahoma", Font.PLAIN, 12);
	public final static Font bold = new Font("Tahoma", Font.BOLD, 10);
	
	public static final void addFilterControls(final JPanel panel, final PetsciiConfig petsciiConfig) {
		final JLabel lblVideoLabel = new JLabel("Video filters:");
		lblVideoLabel.setFont(GuiUtils.bold);
		lblVideoLabel.setBounds(20, 130, 169, 14);
		panel.add(lblVideoLabel);
		
		final JCheckBox chkLowpassFilterButton = new JCheckBox("lowpass");
		chkLowpassFilterButton.setToolTipText("Apply lowpass filter (blur)");
		chkLowpassFilterButton.setFont(GuiUtils.std);
		chkLowpassFilterButton.setBounds(46, 150, 80, 23);
		chkLowpassFilterButton.setSelected(petsciiConfig.filter == FILTER.LOWPASS);
		chkLowpassFilterButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final JCheckBox source = (JCheckBox) e.getSource();
				if (source.isSelected())
					petsciiConfig.filter = FILTER.LOWPASS;
			}
		});

		panel.add(chkLowpassFilterButton);

		final JCheckBox chkEdgeFilterButton = new JCheckBox("edge");
		chkEdgeFilterButton.setToolTipText("Edge filter, edges would be more visible");
		chkEdgeFilterButton.setFont(GuiUtils.std);
		chkEdgeFilterButton.setBounds(150, 150, 80, 23);
		chkEdgeFilterButton.setSelected(petsciiConfig.filter == FILTER.EDGES_BLEND);
		chkEdgeFilterButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final JCheckBox source = (JCheckBox) e.getSource();
				if (source.isSelected())
					petsciiConfig.filter = FILTER.EDGES_BLEND;
			}
		});
		
		panel.add(chkEdgeFilterButton);
		
		final JSlider sldDetect = new JSlider(JSlider.HORIZONTAL, 0, 4, (int) petsciiConfig.lowpass_gain);
		sldDetect.setBounds(40, 186, 100, 35);
		sldDetect.setFont(GuiUtils.std);
		sldDetect.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent e) {
				final JSlider source = (JSlider) e.getSource();

				if (!source.getValueIsAdjusting()) {
					final int value = source.getValue();
					if (value != 0)
						petsciiConfig.lowpass_gain = value;
				}
			}
		});
		
		sldDetect.setMajorTickSpacing(2);
		sldDetect.setPaintLabels(true);
		panel.add(sldDetect);
		
		final JCheckBox chckbxDenoiseCheckBox = new JCheckBox("denoising filter");
		chckbxDenoiseCheckBox.setToolTipText("Neural denoise filter (simple autoencoder)");
		chckbxDenoiseCheckBox.setFont(GuiUtils.std);
		chckbxDenoiseCheckBox.setBounds(150, 196, 150, 20);
		chckbxDenoiseCheckBox.setSelected(petsciiConfig.denoise);

		chckbxDenoiseCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				petsciiConfig.denoise = !petsciiConfig.denoise;
			}
		});

		panel.add(chckbxDenoiseCheckBox);
	}
	
	public static final void addCompressionSoundControls(final JPanel panel, final PetsciiVideoConfig config) {
		final JLabel lblCompLabel = new JLabel("Compression mode:");
		lblCompLabel.setFont(GuiUtils.bold);
		lblCompLabel.setBounds(20, 60, 169, 14);
		panel.add(lblCompLabel);

		final JRadioButton rdbtColorButton = new JRadioButton("Code and color");
		rdbtColorButton.setToolTipText("Preserves colors, shorten clips");
		rdbtColorButton.setFont(GuiUtils.std);
		rdbtColorButton.setBounds(46, 80, 150, 23);
		rdbtColorButton.setSelected(config.compression == COMPRESSION.CODES_COLOR);
		rdbtColorButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.compression = COMPRESSION.CODES_COLOR;
			}
		});

		panel.add(rdbtColorButton);

		final JRadioButton rdbtnCodesButton = new JRadioButton("Only codes");
		rdbtnCodesButton.setToolTipText("Exact codes, but lazy colors, longer clips");
		rdbtnCodesButton.setFont(GuiUtils.std);
		rdbtnCodesButton.setBounds(46, 100, 150, 23);
		rdbtnCodesButton.setSelected(config.compression == COMPRESSION.CODES);
		rdbtnCodesButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.compression = COMPRESSION.CODES;
			}
		});

		final ButtonGroup groupMedium = new ButtonGroup();
		groupMedium.add(rdbtColorButton);
		groupMedium.add(rdbtnCodesButton);
		panel.add(rdbtnCodesButton);
		
		final JLabel lblSoundLabel = new JLabel("Sound options:");
		lblSoundLabel.setFont(GuiUtils.bold);
		lblSoundLabel.setBounds(174, 60, 169, 14);
		panel.add(lblSoundLabel);

		final JCheckBox chkFilterButton = new JCheckBox("lowpass filter");
		chkFilterButton.setToolTipText("Apply low pass filter with cutoff at 5512Hz");
		chkFilterButton.setFont(GuiUtils.std);
		chkFilterButton.setBounds(200, 80, 150, 23);
		chkFilterButton.setSelected(config.lowpassFilter == true);
		chkFilterButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.lowpassFilter = !config.lowpassFilter;
			}
		});

		panel.add(chkFilterButton);

		final JCheckBox chkTPDFButton = new JCheckBox("TPDF");
		chkTPDFButton.setToolTipText("Dithering triangle propability distribution function enabler");
		chkTPDFButton.setFont(GuiUtils.std);
		chkTPDFButton.setBounds(200, 100, 150, 23);
		chkTPDFButton.setSelected(config.ditherPDF == PDF.TPDF);
		chkTPDFButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				switch (config.ditherPDF) {
				case TPDF:
					config.ditherPDF = PDF.WHITE_NOISE;
					break;
				default:
					config.ditherPDF = PDF.TPDF;
					break;
				
				}
			}
		});

		panel.add(chkTPDFButton);
	}
		
	public static final void addContrastControls(final JPanel panel, final Config config) {
		final JLabel contrastLabel = new JLabel("Contrast processing:");
		contrastLabel.setFont(bold);
		contrastLabel.setBounds(20, 220, 300, 20);
		panel.add(contrastLabel);
		
		final JLabel brightLabel = new JLabel("details");
		brightLabel.setFont(bold);
		brightLabel.setBounds(180, 265, 120, 20);
		panel.add(brightLabel);

		final JSlider sldBrightness = new JSlider(JSlider.HORIZONTAL, 1, 5, config.details);
		sldBrightness.setEnabled(config.high_contrast == Config.HIGH_CONTRAST.SWAHE || config.high_contrast == Config.HIGH_CONTRAST.CLAHE);
		sldBrightness.setFont(GuiUtils.std);
		sldBrightness.setBounds(180, 285, 120, 35);
		sldBrightness.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent e) {
				final JSlider source = (JSlider) e.getSource();

				if (!source.getValueIsAdjusting())
					config.details = source.getValue();
			}
		});

		sldBrightness.setPaintLabels(true);

		// create the label table
		final Hashtable<Integer, JLabel> labelTable2 = new Hashtable<Integer, JLabel>();
		labelTable2.put(1, new JLabel("1.0"));
		labelTable2.put(2, new JLabel("2.0"));
		labelTable2.put(3, new JLabel("3.0"));
		labelTable2.put(4, new JLabel("4.0"));
		labelTable2.put(5, new JLabel("5.0"));
		sldBrightness.setLabelTable(labelTable2);

		panel.add(sldBrightness);
		
		final JRadioButton rdbtnNoContrastExpanderButton = new JRadioButton("none");
		rdbtnNoContrastExpanderButton.setToolTipText("No contrast processing");
		rdbtnNoContrastExpanderButton.setFont(std);
		rdbtnNoContrastExpanderButton.setBounds(46, 243, 50, 20);
		rdbtnNoContrastExpanderButton.setSelected(config.high_contrast == Config.HIGH_CONTRAST.NONE);

		rdbtnNoContrastExpanderButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.high_contrast = Config.HIGH_CONTRAST.NONE;
				sldBrightness.setEnabled(false);
			}
		});

		panel.add(rdbtnNoContrastExpanderButton);

		final JRadioButton rdbtnHEButton = new JRadioButton("HE");
		rdbtnHEButton.setToolTipText("Histogram Equalizer");
		rdbtnHEButton.setFont(std);
		rdbtnHEButton.setBounds(106, 243, 50, 20);
		rdbtnHEButton.setSelected(config.high_contrast == Config.HIGH_CONTRAST.HE);

		rdbtnHEButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.high_contrast = Config.HIGH_CONTRAST.HE;
				sldBrightness.setEnabled(false);
			}
		});

		panel.add(rdbtnHEButton);
		
		final JRadioButton rdbtnCLAHEButton = new JRadioButton("CLAHE");
		rdbtnCLAHEButton.setToolTipText("Clipped Adaptive Histogram Equalizer");
		rdbtnCLAHEButton.setFont(std);
		rdbtnCLAHEButton.setBounds(166, 243, 70, 20);
		rdbtnCLAHEButton.setSelected(config.high_contrast == Config.HIGH_CONTRAST.CLAHE);

		rdbtnCLAHEButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.high_contrast = Config.HIGH_CONTRAST.CLAHE;
				sldBrightness.setEnabled(true);
			}
		});

		panel.add(rdbtnCLAHEButton);
		
		final ButtonGroup groupContrast = new ButtonGroup();
		groupContrast.add(rdbtnNoContrastExpanderButton);
		groupContrast.add(rdbtnHEButton);
		groupContrast.add(rdbtnCLAHEButton);
	}
}
