package pl.dido.video.supercpu;

import at.fhtw.ai.nn.utils.Network;
import pl.dido.image.petscii.PetsciiRenderer;
import pl.dido.image.utils.Config;

public class SupercpuRenderer extends PetsciiRenderer {

	public SupercpuRenderer(final Config config) {
		super(config);
	}

	public void setNeural(final Network neural) {
		this.neural = neural;
	}

	public void setupPalette() {
		super.setupPalette();
	}
}