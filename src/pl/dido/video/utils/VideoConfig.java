package pl.dido.video.utils;

import java.io.File;

import pl.dido.image.utils.Config;

public class VideoConfig {

	public File selectedFile;
	public Config config;
	
	public int startVideoFrame;
	public int frameRate;
	
	public boolean denoise = false;
	private static final int desiredFramerate = 12;
	
	public VideoConfig(final Config config) {
		this.config = config;
		
		denoise = false;
	}
	
	public Config getConfig() {
		return config;
	}	

	public int getSkipFrameRate() {
		return frameRate / desiredFramerate;
	}
}
