package pl.dido.video.compression;

import java.util.Arrays;

public class Compression {

	public static int[] compress(final int o[], final int p[]) {
		final int out[] = new int[2000];

		int index = 0;
		int address = 0;

		for (int i = 0, j = 0; i < o.length; i++) {
			if (o[i] != p[i]) {
				out[index++] = j;
				out[index++] = p[i];

				address += j;
				j = 0;
			}

			if (j == 255) {
				out[index++] = 255;
				out[index++] = p[i];

				address += j;
				j = 0;
			}

			j++;
		}

		if (address < 999) {
			out[index++] = 999 - address;
			out[index++] = p[999];
		}

		return Arrays.copyOf(out, index);
	}

	public static int[] decompress(final int prev[], final int changes[]) {
		int index = 0;
		for (int i = 0; i < changes.length; i += 2) {
			index += changes[i];
			prev[index] = changes[i + 1];
		}

		return prev;
	}

	public static boolean checkAddress(final int changes[]) {
		int address = 0;
		for (int i = 0; i < changes.length; i += 2)
			address += changes[i];

		return address == 999;
	}

	public static int[] packNibble(final int[] nibble) {
		final int result[] = new int[nibble.length / 2];

		for (int i = 0, j = 0; i < nibble.length; i += 2, j++)
			result[j] = (nibble[i] & 0xf) | ((nibble[i + 1] & 0xf) << 4);

		return result;
	}
}
