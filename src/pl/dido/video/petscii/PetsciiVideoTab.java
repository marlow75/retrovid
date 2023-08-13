package pl.dido.video.petscii;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.SystemColor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;

import pl.dido.video.utils.GuiUtils;
import pl.dido.video.utils.VideoCanvas;

public class PetsciiVideoTab extends PetsciiGui {
	
	public static VideoCanvas movie;
	public static JSlider sldFrame;
	
	public static Button btnPlay;
	public static Button btnRecord;
	
	public static JProgressBar progressBar;

	public static JPanel petsciiTab(final PetsciiVideoConfig config) {
		final JPanel petscii = PetsciiGui.petsciiTab(config);
		movie = new VideoCanvas();
		
		movie.setBounds(320, 7, 320, 200);
		movie.setBackground(Color.black);
		
		petscii.add(movie);	
		
		final JLabel sizeLabel = new JLabel("select start position (sec)");
		sizeLabel.setFont(GuiUtils.bold);
		sizeLabel.setBounds(320, 215, 200, 20);
		petscii.add(sizeLabel);

		sldFrame = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		sldFrame.setBounds(350, 235, 280, 35);
		sldFrame.setFont(GuiUtils.std);
		sldFrame.setEnabled(false);

		sldFrame.setPaintLabels(true);
		sldFrame.setMinorTickSpacing(10);
		sldFrame.setSnapToTicks(false);
		sldFrame.setPaintLabels(true);
		
		petscii.add(sldFrame);
		
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
		
		petscii.add(btnPlay);
		petscii.add(btnRecord);
		
		return petscii;
	}
}