package pl.dido.video.compression;

import java.util.Arrays;

// compress only character changes with max 15 relative positions
public class VGACodesCompression extends Compression {
	
	public int[] compress(final int o[], final int n[], final int screen[], final int nibble[]) {
		final int out[] = new int[4000];
		int index = 0, address = 0;
		
		for (int i = 0, j = 0; i < o.length; i++) {
			if (o[i] != screen[i]) {
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
	
	@Override
	public void decompress(final int s[], final int c[], final int changes[]) {
		int address = 0;
		for (int i = 0; i < changes.length - 1; i += 3) {
			address += changes[i];
			c[address] = changes[i + 1];
			s[address] = changes[i + 2];
		}
	}

	@Override
	public boolean checkSize(final int changes[], final int size) {
		int s = 0;
		for (int i = 0; i < changes.length; i += 3)
			s += changes[i];

		return s == size;
	}
}
