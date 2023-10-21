package pl.dido.video.utils;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pl.dido.image.utils.Config;

public class GuiUtils {

	public final static Font std = new Font("Tahoma", Font.BOLD, 10);
	public final static Font bold = new Font("Tahoma", Font.BOLD, 10);
		
	public static final void addContrastControls(final JPanel panel, final Config config) {
		final JLabel contrastLabel = new JLabel("Contrast processing:");
		contrastLabel.setFont(bold);
		contrastLabel.setBounds(20, 190, 300, 20);
		panel.add(contrastLabel);
		
		final JLabel brightLabel = new JLabel("details");
		brightLabel.setFont(bold);
		brightLabel.setBounds(180, 235, 120, 20);
		panel.add(brightLabel);

		final JSlider sldBrightness = new JSlider(JSlider.HORIZONTAL, 1, 5, config.details);
		sldBrightness.setEnabled(config.highContrast == Config.HIGH_CONTRAST.SWAHE || config.highContrast == Config.HIGH_CONTRAST.CLAHE);
		sldBrightness.setFont(GuiUtils.std);
		sldBrightness.setBounds(180, 255, 120, 35);
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
		rdbtnNoContrastExpanderButton.setBounds(46, 213, 50, 20);
		rdbtnNoContrastExpanderButton.setSelected(config.highContrast == Config.HIGH_CONTRAST.NONE);

		rdbtnNoContrastExpanderButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.highContrast = Config.HIGH_CONTRAST.NONE;
				sldBrightness.setEnabled(false);
			}
		});

		panel.add(rdbtnNoContrastExpanderButton);

		final JRadioButton rdbtnHEButton = new JRadioButton("HE");
		rdbtnHEButton.setToolTipText("Histogram Equalizer");
		rdbtnHEButton.setFont(std);
		rdbtnHEButton.setBounds(116, 213, 50, 20);
		rdbtnHEButton.setSelected(config.highContrast == Config.HIGH_CONTRAST.HE);

		rdbtnHEButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.highContrast = Config.HIGH_CONTRAST.HE;
				sldBrightness.setEnabled(false);
			}
		});

		panel.add(rdbtnHEButton);
		
		final JRadioButton rdbtnCLAHEButton = new JRadioButton("CLAHE");
		rdbtnCLAHEButton.setToolTipText("Clipped Adaptive Histogram Equalizer");
		rdbtnCLAHEButton.setFont(std);
		rdbtnCLAHEButton.setBounds(186, 213, 70, 20);
		rdbtnCLAHEButton.setSelected(config.highContrast == Config.HIGH_CONTRAST.CLAHE);

		rdbtnCLAHEButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				config.highContrast = Config.HIGH_CONTRAST.CLAHE;
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
