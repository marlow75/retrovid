package pl.dido.video.petscii;

import java.io.File;

import pl.dido.image.petscii.PetsciiConfig;
import pl.dido.image.utils.Config.DITHERING;
import pl.dido.image.utils.Config.FILTER;
import pl.dido.video.utils.VideoConfig;

public class PetsciiVideoConfig extends VideoConfig {
	
	public enum MEDIUM_TYPE { CRT, CRT_SND, PRG };
	
	public MEDIUM_TYPE mediumType;

	public File selectedFile;	
	public int startFrame;
	
	public PetsciiVideoConfig() {
		super(new PetsciiConfig());
		mediumType = MEDIUM_TYPE.CRT;
		
		config.dither_alg = DITHERING.NONE;
		config.pal_view = false;
		
		config.filter = FILTER.EDGES_BLEND;
	}
}
