package pl.dido.video.compression;

import java.util.Arrays;

// compress only character changes with max 15 relative positions
public class ColorsCodesCompression extends CodesCompression {
	
	@Override
	public int[] compress(final int o[], final int n[], final int screen[], final int nibble[]) {
		final int out[] = new int[2000];
		int index = 0, address = 0;
		
		for (int i = 0, j = 0; i < o.length; i++) {
			if (o[i] != screen[i] || n[i] != nibble[i]) {
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
}
