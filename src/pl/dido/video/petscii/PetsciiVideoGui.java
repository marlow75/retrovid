package pl.dido.video.petscii;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pl.dido.image.petscii.PetsciiConfig;
import pl.dido.image.petscii.PetsciiRenderer;
import pl.dido.image.utils.Config.FILTER;
import pl.dido.video.petscii.PetsciiVideoConfig.COMPRESSION;
import pl.dido.video.petscii.PetsciiVideoConfig.MEDIUM_TYPE;
import pl.dido.video.utils.GuiUtils;
import pl.dido.video.utils.VideoGui;

public class PetsciiVideoGui extends VideoGui {
	
	public PetsciiVideoGui(final JFrame frame, final PetsciiVideoConfig config) {
		super(frame, new PetsciiRenderer(config.config), config);
	} 

	@Override
	public JPanel getTab() {
		final JPanel panel = super.getTab();
		final PetsciiVideoConfig petsciiVideoConfig = (PetsciiVideoConfig) config;

		final JLabel lblmediumLabel = new JLabel("Medium:");
		lblmediumLabel.setFont(GuiUtils.bold);
		lblmediumLabel.setBounds(20, 10, 169, 14);
		panel.add(lblmediumLabel);

		final JRadioButton rdbtnPRGButton = new JRadioButton("PRG");
		rdbtnPRGButton.setToolTipText("Pack movie into prg file - 50kb");
		rdbtnPRGButton.setFont(GuiUtils.std);
		rdbtnPRGButton.setBounds(46, 30, 50, 23);
		rdbtnPRGButton.setSelected(petsciiVideoConfig.mediumType == MEDIUM_TYPE.PRG);
		rdbtnPRGButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				petsciiVideoConfig.mediumType = MEDIUM_TYPE.PRG;
			}
		});

		panel.add(rdbtnPRGButton);

		final JRadioButton rdbtnCRTVideoButton = new JRadioButton("CRT");
		rdbtnCRTVideoButton.setToolTipText("Pack movie into GS cartridge file without audio - 512kb");
		rdbtnCRTVideoButton.setFont(GuiUtils.std);
		rdbtnCRTVideoButton.setBounds(100, 30, 50, 23);
		rdbtnCRTVideoButton.setSelected(petsciiVideoConfig.mediumType == MEDIUM_TYPE.CRT);
		rdbtnCRTVideoButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				petsciiVideoConfig.mediumType = MEDIUM_TYPE.CRT;
			}
		});

		panel.add(rdbtnCRTVideoButton);

		final JRadioButton rdbtnCRTAudioButton = new JRadioButton("CRT audio");
		rdbtnCRTAudioButton.setToolTipText("Pack movie into GS cartridge file with audio - 512kb");
		rdbtnCRTAudioButton.setFont(GuiUtils.std);
		rdbtnCRTAudioButton.setBounds(154, 30, 100, 23);
		rdbtnCRTAudioButton.setSelected(petsciiVideoConfig.mediumType == MEDIUM_TYPE.CRT_SND);
		rdbtnCRTAudioButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				petsciiVideoConfig.mediumType = MEDIUM_TYPE.CRT_SND;
			}
		});

		panel.add(rdbtnCRTAudioButton);

		final ButtonGroup groupCompression = new ButtonGroup();
		groupCompression.add(rdbtnPRGButton);
		groupCompression.add(rdbtnCRTVideoButton);
		groupCompression.add(rdbtnCRTAudioButton);

		final JLabel lblCompLabel = new JLabel("Compression mode:");
		lblCompLabel.setFont(GuiUtils.bold);
		lblCompLabel.setBounds(20, 60, 169, 14);
		panel.add(lblCompLabel);

		final JRadioButton rdbtColorButton = new JRadioButton("Code and color");
		rdbtColorButton.setToolTipText("Preserves colors, shorten clips");
		rdbtColorButton.setFont(GuiUtils.std);
		rdbtColorButton.setBounds(46, 80, 150, 23);
		rdbtColorButton.setSelected(petsciiVideoConfig.compression == COMPRESSION.CODES_COLOR);
		rdbtColorButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				petsciiVideoConfig.compression = COMPRESSION.CODES_COLOR;
			}
		});

		panel.add(rdbtColorButton);

		final JRadioButton rdbtnCodesButton = new JRadioButton("Only codes");
		rdbtnCodesButton.setToolTipText("Exact codes, but lazy colors, longer clips");
		rdbtnCodesButton.setFont(GuiUtils.std);
		rdbtnCodesButton.setBounds(46, 100, 150, 23);
		rdbtnCodesButton.setSelected(petsciiVideoConfig.compression == COMPRESSION.CODES);
		rdbtnCodesButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				petsciiVideoConfig.compression = COMPRESSION.CODES;
			}
		});

		final ButtonGroup groupMedium = new ButtonGroup();
		groupMedium.add(rdbtColorButton);
		groupMedium.add(rdbtnCodesButton);
		panel.add(rdbtnCodesButton);

		final PetsciiConfig petsciiConfig = ((PetsciiConfig) petsciiVideoConfig.config);
		
		final JLabel lblConvertLabel = new JLabel("lowpass threshold:");
		lblConvertLabel.setFont(GuiUtils.bold);
		lblConvertLabel.setBounds(20, 125, 100, 14);
		panel.add(lblConvertLabel);
		
		final JSlider sldDetect = new JSlider(JSlider.HORIZONTAL, 0, 4, (int) petsciiConfig.lowpass_gain);
		sldDetect.setBounds(40, 146, 100, 35);
		sldDetect.setFont(GuiUtils.std);
		sldDetect.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent e) {
				final JSlider source = (JSlider) e.getSource();

				if (!source.getValueIsAdjusting()) {
					final int value = source.getValue();
					if (value != 0) {
					    petsciiConfig.filter = FILTER.LOWPASS;
						petsciiConfig.lowpass_gain = value;
					} else
						petsciiConfig.filter = FILTER.NONE;
				}
			}
		});
		
		sldDetect.setMajorTickSpacing(2);
		sldDetect.setPaintLabels(true);
		panel.add(sldDetect);
		
		final JCheckBox chckbxDenoiseCheckBox = new JCheckBox("denoising filter");
		chckbxDenoiseCheckBox.setToolTipText("Neural denoise filter (simple autoencoder)");
		chckbxDenoiseCheckBox.setFont(GuiUtils.std);
		chckbxDenoiseCheckBox.setBounds(150, 156, 150, 20);
		chckbxDenoiseCheckBox.setSelected(petsciiConfig.denoise);

		chckbxDenoiseCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.denoise = !config.denoise;
			}
		});

		panel.add(chckbxDenoiseCheckBox);

		GuiUtils.addContrastControls(panel, petsciiVideoConfig.config);
		return panel;
	}

	@Override
	protected PetsciiGrabberTask getGrabberTask() {
		return new PetsciiGrabberTask((PetsciiVideoConfig)config);
	}
}