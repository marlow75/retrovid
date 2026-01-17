package pl.dido.video.compression;

public abstract class Compression {

	public void decompress(final int s[], final int c[], final int changes[]) {
		int index = 0;
		for (int i = 0; i < changes.length; i += 2) {
			index += changes[i];
			s[index] = changes[i + 1];
		}
	}

	public boolean checkSize(final int changes[], final int size) {
		int s = 0;
		for (int i = 0; i < changes.length; i += 2)
			s += changes[i];

		return size == s;
	}

	public int[] packNibble(final int[] nibble) {
		final int result[] = new int[nibble.length / 2];

		for (int i = 0, j = 0; i < nibble.length; i += 2, j++)
			result[j] = (nibble[i] & 0xf) | ((nibble[i + 1] & 0xf) << 4);

		return result;
	}

	public abstract int[] compress(int[] oldScreen, int[] oldNibble, int[] screen, int[] nibble);
}
