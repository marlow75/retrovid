package pl.dido.video.medium;

import java.io.IOException;

public interface Medium {
	void createMedium() throws IOException;
	
	int getCurrentSize();
	int getMaxSize();
}
