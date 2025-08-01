package pl.dido.video.utils;

import java.io.File;

import pl.dido.image.utils.Config;

public class VideoConfig {
	
	public enum PDF { WHITE_NOISE, TPDF };

	public File selectedFile;
	public Config petsciiConfig;
	
	// starting conversion frame
	public int startVideoFrame;
	// frame rate of converted video
	public int frameRate;
	// video denoiser
	public boolean denoise = false;
	// dithering probability function
	public PDF ditherPDF = PDF.TPDF;
	// low pass filter
	public boolean lowpassFilter = true;
	// frame rate after conversion
	protected static final int desiredFramerate = 12;
	
	public VideoConfig(final Config config) {
		this.petsciiConfig = config;
		denoise = false;
	}

	public int getSkipFrameRate() {
		return frameRate / desiredFramerate;
	}
}