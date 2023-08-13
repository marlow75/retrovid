package pl.dido.video.petscii;

import java.io.File;

import pl.dido.image.petscii.PetsciiConfig;

public class PetsciiVideoConfig extends PetsciiConfig {
	
	public enum COMPRESSION { CODES_COLOR, CODES };
	public enum MEDIUM_TYPE { CRT, PRG };
	
	public int startFrame;
	public COMPRESSION compression;
	
	public File selectedFile;
	public int skip;
	
	public int frameRate;
	public MEDIUM_TYPE mediumType;
	
	public PetsciiVideoConfig() {
		super();
		compression = COMPRESSION.CODES_COLOR;
		mediumType = MEDIUM_TYPE.CRT;
	}
}
