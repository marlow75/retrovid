package pl.dido.video.utils;

import java.io.File;

import pl.dido.image.utils.Config;

public class VideoConfig {

	public File selectedFile;
	public Config config;
	
	public int startFrame;
	public int frameRate;
	
	public VideoConfig(final Config config) {
		this.config = config;
	}
	
	public Config getConfig() {
		return config;
	}	
	
	public int getFrameRate() {
		return 10;
	}

	public int getSkipFrameRate() {
		return frameRate / getFrameRate();
	}
}
