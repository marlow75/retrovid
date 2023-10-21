package pl.dido.video.medium;

public interface VideoMedium extends Medium {
	
	void saveKeyFrame(int background, int[] screen, int[] nibble);
	boolean saveFrame(int background, int[] oldScreen, int[] oldNibble, int[] screen, int[] nibble);
}
