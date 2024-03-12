package pl.dido.video.compression;

import java.util.Arrays;

public class Compression {
	// o - old screen, n - old nibble
	public int[] compress(final int o[], final int n[], final int screen[], final int nibble[]) {
		final int out[] = new int[2000];

		int index = 0;
		int address = 0;

		for (int i = 0, j = 0; i < o.length; i++) {
			if (o[i] != screen[i]) {
				out[index++] = j;
				out[index++] = screen[i];

				address += j;
				j = 0;
			}

			if (j == 255) {
				out[index++] = 255;
				out[index++] = screen[i];

				address += j;
				j = 0;
			}

			j++;
		}

		if (address < 999) {
			out[index++] = (999 - address);
			out[index++] = screen[999];
		}

		return Arrays.copyOf(out, index);
	}

	public void decompress(final int s[], final int c[], final int changes[]) {
		int index = 0;
		for (int i = 0; i < changes.length; i += 2) {
			index += changes[i];
			s[index] = changes[i + 1];
		}
	}

	public boolean checkSize(final int changes[]) {
		int size = 0;
		for (int i = 0; i < changes.length; i += 2)
			size += changes[i];

		return size == 999;
	}

	public int[] packNibble(final int[] nibble) {
		final int result[] = new int[nibble.length / 2];

		for (int i = 0, j = 0; i < nibble.length; i += 2, j++)
			result[j] = (nibble[i] & 0xf) | ((nibble[i + 1] & 0xf) << 4);

		return result;
	}
}
