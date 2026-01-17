package pl.dido.video.utils;

import java.io.File;

import pl.dido.image.utils.Config;

public class VideoConfig {
	
	public enum SOUND_NORMALIZATION { LIGHT, AGRESSIVE };
	public enum DITHERING_PDF { WHITE_NOISE, TPDF };
	public enum COMPRESSION { CODES_COLOR, CODES };

	public File selectedFile;
	public Config config;
	
	public COMPRESSION compression = COMPRESSION.CODES_COLOR;
	
	// starting conversion frame
	public int startVideoFrame;
	// frame rate of converted video
	public double frameRate;
	// video denoiser
	public boolean denoise = false;
	// sound dithering probability function
	public DITHERING_PDF ditherPDF = DITHERING_PDF.TPDF;
	// sound low pass filter
	public boolean lowpassFilter = true;
	// sound filter type
	public SOUND_NORMALIZATION soundNormalization = SOUND_NORMALIZATION.LIGHT;
	// frame rate after conversion
	protected static final int desiredFramerate = 12;
	
	public VideoConfig(final Config config) {
		this.config = config;
	}

	public int getSkipFrameRate() {
		return (int)Math.round(frameRate / desiredFramerate);
	}
}