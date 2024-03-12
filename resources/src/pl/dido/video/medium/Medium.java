package pl.dido.video.medium;

import java.io.IOException;

public interface Medium {
	void createMedium(String fileName) throws IOException;
	
	int getCurrentSize();
	int getMaxSize();
}
