package pl.dido.video.ascii;

import javax.swing.JFrame;
import javax.swing.JPanel;

import pl.dido.image.petscii.PetsciiRenderer;
import pl.dido.video.utils.GrabberTask;
import pl.dido.video.utils.GuiUtils;
import pl.dido.video.utils.VideoGui;

public class AsciiVideoGui extends VideoGui {
	
	public AsciiVideoGui(final JFrame frame, final AsciiVideoConfig config) {
		super(frame, new PetsciiRenderer(config.config), config);
	} 

	@Override
	public JPanel getTab() {
		final JPanel panel = super.getTab();
		final AsciiVideoConfig asciiVideoConfig = (AsciiVideoConfig) config;

		GuiUtils.addContrastControls(panel, asciiVideoConfig.config);
		return panel;
	}

	@Override
	protected GrabberTask getGrabberTask() {
		return new AsciiGrabberTask((AsciiVideoConfig)config);
	}
}