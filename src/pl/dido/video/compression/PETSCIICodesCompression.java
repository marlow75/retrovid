package pl.dido.video.compression;

import java.util.Arrays;

// compress only character changes with max 15 relative positions
public class PETSCIICodesCompression extends Compression {
	
	public int[] compress(final int o[], final int n[], final int screen[], final int nibble[]) {
		final int out[] = new int[2000];
		int index = 0, address = 0;
		
		for (int i = 0, j = 0; i < o.length; i++) {
			if (o[i] != screen[i]) {
				final int a = i - j;
				out[index++] = a | (nibble[i] << 4);
				out[index++] = screen[i];
				
				address += a;
				j = i;
				continue;
			}
			
			if (i - j == 15) {
				out[index++] = 15 | (nibble[i] << 4);
				out[index++] = screen[i];

				address += 15;
				j = i;
				continue;
			}
		}
		
		int rest = 999 - address;
		while (rest > 0) {
			int a = rest < 15 ? rest : 15;
			address += a;
			
			out[index++] = a | (nibble[address] << 4);
			out[index++] = screen[address];
			
			rest -= a;
		}
		
		return Arrays.copyOf(out, index);
	}
	
	@Override
	public void decompress(final int s[], final int c[], final int changes[]) {
		int address = 0;
		for (int i = 0; i < changes.length - 1; i += 2) {
			address += changes[i] & 0xf;
			c[address] = (byte) ((changes[i] & 0xf0) >> 4);
			s[address] = changes[i + 1];
		}
	}

	@Override
	public boolean checkSize(final int changes[], final int size) {
		int s = 0;
		for (int i = 0; i < changes.length; i += 2)
			s += changes[i] & 0xf;

		return s == size;
	}

}
