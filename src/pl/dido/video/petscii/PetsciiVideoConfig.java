package pl.dido.video.petscii;

import java.io.File;

import pl.dido.image.petscii.PetsciiConfig;
import pl.dido.image.utils.Config.DITHERING;
import pl.dido.video.utils.VideoConfig;

public class PetsciiVideoConfig extends VideoConfig {
	
	public enum COMPRESSION { CODES_COLOR, CODES };
	public enum MEDIUM_TYPE { CRT, CRT_SND, PRG };
	
	public MEDIUM_TYPE mediumType;
	public COMPRESSION compression;

	public File selectedFile;	
	public int startFrame;
	public int frameRate;
	
	public PetsciiVideoConfig() {
		super(new PetsciiConfig());
		
		compression = COMPRESSION.CODES_COLOR;
		mediumType = MEDIUM_TYPE.CRT_SND;
		
		this.config.dither_alg = DITHERING.NONE;
		this.config.pal_view = false;
		
		this.config.low_pass_filter = true;
	}
}
