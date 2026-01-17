package pl.dido.video.petscii;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import pl.dido.image.petscii.PetsciiConfig;
import pl.dido.image.renderer.AbstractRenderer;
import pl.dido.video.petscii.PetsciiVideoConfig.MEDIUM_TYPE;
import pl.dido.video.utils.GuiUtils;
import pl.dido.video.utils.VideoGui;

public class PetsciiVideoGui extends VideoGui {
	
	public PetsciiVideoGui(final JFrame frame, AbstractRenderer renderer, final PetsciiVideoConfig config) {
		super(frame, renderer, config);
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

		final ButtonGroup groupMedium = new ButtonGroup();
		groupMedium.add(rdbtnPRGButton);
		groupMedium.add(rdbtnCRTVideoButton);
		groupMedium.add(rdbtnCRTAudioButton);

		GuiUtils.addCompressionSoundControls(panel, petsciiVideoConfig);
		GuiUtils.addVideoFilterControls(panel, (PetsciiConfig) petsciiVideoConfig.config);
		GuiUtils.addContrastControls(panel, petsciiVideoConfig.config);
		
		return panel;
	}

	@Override
	protected PetsciiGrabberTask getGrabberTask() {
		return new PetsciiGrabberTask((PetsciiVideoConfig)config);
	}
}