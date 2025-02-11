package pl.dido.video.ascii;

import java.io.File;

import pl.dido.image.petscii.PetsciiConfig;
import pl.dido.image.utils.Config.DITHERING;
import pl.dido.video.utils.VideoConfig;

public class AsciiVideoConfig extends VideoConfig {
	
	public enum COMPRESSION { CODES_COLOR, CODES };

	public File selectedFile;	
	public int startFrame;
	public int frameRate;
	
	public AsciiVideoConfig() {
		super(new PetsciiConfig());
		
		config.dither_alg = DITHERING.NONE;
		config.pal_view = false;
		
		config.low_pass_filter = false;
	}
}
