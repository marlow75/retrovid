package pl.dido.video.petscii;

import java.io.File;

import pl.dido.image.petscii.PetsciiConfig;

public class PetsciiVideoConfig extends PetsciiConfig {
	
	public enum COMPRESSION { CODES_COLOR, CODES };
	public enum MEDIUM_TYPE { CRT, CRT_SND, PRG };
	
	public MEDIUM_TYPE mediumType;
	public COMPRESSION compression;

	public File selectedFile;	
	public int startFrame;
	public int frameRate;
	
	public PetsciiVideoConfig() {
		super();
		
		compression = COMPRESSION.CODES_COLOR;
		mediumType = MEDIUM_TYPE.CRT_SND;
	}
	
	public int getFrameRate() {
		switch (mediumType) {
		case CRT_SND: 
			return 10;
		default:
			return 12;
		}
	}

	public int getSkipFrameRate() {
		return frameRate / getFrameRate();
	}
}
