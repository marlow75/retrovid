package pl.dido.video.charset;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import pl.dido.video.petscii.PetsciiGrabberTask;
import pl.dido.video.petscii.PetsciiVideoConfig;
import pl.dido.video.petscii.PetsciiVideoConfig.COMPRESSION;
import pl.dido.video.petscii.PetsciiVideoConfig.MEDIUM_TYPE;
import pl.dido.video.utils.GuiUtils;
import pl.dido.video.utils.VideoGui;

public class CharsetVideoGui extends VideoGui {
	
	public CharsetVideoGui(final JFrame frame) {
		super(frame, new PetsciiVideoConfig());
	} 
	
	@Override
	public JPanel getTab() {
		final JPanel panel = super.getTab();
		
		final JLabel lblmediumLabel = new JLabel("Medium:");
		lblmediumLabel.setFont(GuiUtils.bold);
		lblmediumLabel.setBounds(20, 10, 169, 14);
		panel.add(lblmediumLabel);

		final JRadioButton rdbtnCRTVideoButton = new JRadioButton("CRT");
		rdbtnCRTVideoButton.setToolTipText("Pack movie into GS cartridge file without audio - 512kb");
		rdbtnCRTVideoButton.setFont(GuiUtils.std);
		rdbtnCRTVideoButton.setBounds(46, 30, 50, 23);
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
		rdbtnCRTAudioButton.setBounds(100, 30, 100, 23);
		rdbtnCRTAudioButton.setSelected(config.mediumType == MEDIUM_TYPE.CRT_SND);
		rdbtnCRTAudioButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.mediumType = MEDIUM_TYPE.CRT_SND;
			}
		});

		panel.add(rdbtnCRTAudioButton);
		
		final ButtonGroup groupCompression = new ButtonGroup();
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
				
		GuiUtils.addContrastControls(panel, config);
		return panel;
	}

	@Override
	protected PetsciiGrabberTask getGrabberTask() {
		return new CharsetGrabberTask(config);
	}
}