package pl.dido.video.petscii;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import pl.dido.image.petscii.PetsciiConfig.NETWORK;
import pl.dido.video.petscii.PetsciiVideoConfig.COMPRESSION;
import pl.dido.video.petscii.PetsciiVideoConfig.MEDIUM_TYPE;
import pl.dido.video.utils.GuiUtils;
import pl.dido.video.utils.VideoGui;

public class PetsciiVideoGui extends VideoGui {
	
	public PetsciiVideoGui(final JFrame frame) {
		super(frame, new PetsciiVideoConfig());
	} 

	@Override
	public JPanel getTab() {
		final JPanel panel = super.getTab();

		final JLabel lblmediumLabel = new JLabel("Medium:");
		lblmediumLabel.setFont(GuiUtils.bold);
		lblmediumLabel.setBounds(20, 10, 169, 14);
		panel.add(lblmediumLabel);

		final JRadioButton rdbtnPRGButton = new JRadioButton("PRG");
		rdbtnPRGButton.setToolTipText("Pack movie into prg file - 50kb");
		rdbtnPRGButton.setFont(GuiUtils.std);
		rdbtnPRGButton.setBounds(46, 30, 50, 23);
		rdbtnPRGButton.setSelected(config.mediumType == MEDIUM_TYPE.PRG);
		rdbtnPRGButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.mediumType = MEDIUM_TYPE.PRG;
			}
		});

		panel.add(rdbtnPRGButton);

		final JRadioButton rdbtnCRTVideoButton = new JRadioButton("CRT");
		rdbtnCRTVideoButton.setToolTipText("Pack movie into GS cartridge file without audio - 512kb");
		rdbtnCRTVideoButton.setFont(GuiUtils.std);
		rdbtnCRTVideoButton.setBounds(100, 30, 50, 23);
		rdbtnCRTVideoButton.setSelected(config.mediumType == MEDIUM_TYPE.CRT);
		rdbtnCRTVideoButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.mediumType = MEDIUM_TYPE.CRT;
			}
		});

		panel.add(rdbtnCRTVideoButton);

		final JRadioButton rdbtnCRTAudioButton = new JRadioButton("CRT audio");
		rdbtnCRTAudioButton.setToolTipText("Pack movie into GS cartridge file with audio - 512kb");
		rdbtnCRTAudioButton.setFont(GuiUtils.std);
		rdbtnCRTAudioButton.setBounds(154, 30, 100, 23);
		rdbtnCRTAudioButton.setSelected(config.mediumType == MEDIUM_TYPE.CRT_SND);
		rdbtnCRTAudioButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.mediumType = MEDIUM_TYPE.CRT_SND;
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

		final JLabel lblConvertLabel = new JLabel("Converter mode:");
		lblConvertLabel.setFont(GuiUtils.bold);
		lblConvertLabel.setBounds(20, 125, 169, 14);
		panel.add(lblConvertLabel);

		final JRadioButton rdbtnL1Button = new JRadioButton("Semigraphics");
		rdbtnL1Button.setToolTipText("Prefers semigraphic");
		rdbtnL1Button.setFont(GuiUtils.std);
		rdbtnL1Button.setBounds(46, 142, 150, 23);
		rdbtnL1Button.setSelected(config.network == NETWORK.L1);
		rdbtnL1Button.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.network = NETWORK.L1;
			}
		});

		panel.add(rdbtnL1Button);

		final JRadioButton rdbtnL2Button = new JRadioButton("Characters");
		rdbtnL2Button.setToolTipText("Prefers characters");
		rdbtnL2Button.setFont(GuiUtils.std);
		rdbtnL2Button.setBounds(46, 162, 150, 23);
		rdbtnL2Button.setSelected(config.network == NETWORK.L2);
		rdbtnL2Button.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.network = NETWORK.L2;
			}
		});

		panel.add(rdbtnL2Button);

		final ButtonGroup groupResolution = new ButtonGroup();
		groupResolution.add(rdbtnL1Button);
		groupResolution.add(rdbtnL2Button);

		GuiUtils.addContrastControls(panel, config);
		return panel;
	}

	@Override
	protected PetsciiGrabberTask getGrabberTask() {
		return new PetsciiGrabberTask(config);
	}
}