package pl.dido.video.supercpu;

import at.fhtw.ai.nn.utils.Network;
import pl.dido.image.petscii.PetsciiRenderer2;
import pl.dido.image.utils.Config;

public class SupercpuRenderer2 extends PetsciiRenderer2 {
	
	public SupercpuRenderer2(final Config config) {
		super(config);
	}

	public void setNeurals(final Network neural1, final Network neural2) {
		this.neural = neural1;
		this.neural2 = neural2;
	}

	public void setupPalette() {
		super.setupPalette();
	}

	public void setCharsets(final byte charset1[], final byte charset2[]) {
		this.charset = charset1;
		this.charset2 = charset2;
	}
}