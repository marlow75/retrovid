package pl.dido.video.compression;

import java.util.Arrays;

public class VGAColorsCodesCompression extends VGACodesCompression {

	// o - old screen, n - old nibble
	public int[] compress(final int o[], final int n[], final int screen[], final int nibble[]) {
		final int out[] = new int[8000];
		int index = 0, address = 0;
		
		for (int i = 0, j = 0; i < o.length; i++) {
			if (o[i] != screen[i] || n[i] != nibble[i]) {
				final int a = i - j;
				out[index++] = a;
				out[index++] = nibble[i];
				out[index++] = screen[i];
				
				address += a;
				j = i;
				continue;
			}
			
			if (i - j == 255) {
				out[index++] = 255;
				out[index++] = nibble[i];
				out[index++] = screen[i];

				address += 255;
				j = i;
				continue;
			}
		}
		
		int rest = 1999 - address;
		while (rest > 0) {
			int a = rest < 255 ? rest : 255;
			address += a;
			
			out[index++] = a;
			out[index++] = nibble[address];
			out[index++] = screen[address];
			
			rest -= a;
		}

		return Arrays.copyOf(out, index);
	}
}