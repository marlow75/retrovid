package pl.dido.video.utils;

import java.io.IOException;

public interface VideoMedium {
	void saveKeyFrame(int background, int[] screen, int[] nibble);
	// true is frame fits medium
	boolean saveFrame(int background, int[] oldScreen, int[] oldNibble, int[] screen, int[] nibble) throws Exception;
	
	void rewind(int lastFrameSize);
	void createMedium(String fileName) throws IOException;
	
	void setFrames(short frames);
	int getCurrentSize();
	
	int getMaxSize();
}
