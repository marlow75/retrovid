package pl.dido.video;

import javax.swing.JPanel;

import pl.dido.image.AboutGui;
import pl.dido.video.utils.VideoPanel;

public class AboutVideoGui implements VideoPanel {

	@Override
	public JPanel getTab() {
		return AboutGui.aboutTab();
	}
}
