package pl.dido.video.petscii;

import pl.dido.image.petscii.PetsciiConfig;

public class PetsciiVideoConfig extends PetsciiConfig {
	
	public enum COMPRESSION { CODES_COLOR, CODES };
	
	public int startFrame;
	public COMPRESSION compression;
	
	public PetsciiVideoConfig() {
		super();
		compression = COMPRESSION.CODES_COLOR;
	}
}
