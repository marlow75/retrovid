package pl.dido.video.compression;

import java.util.Arrays;

public class Compression3 extends Compression2 {
	
	public static final int[] compress(final int o[], final int p[], final int oc[], final int c[]) {
		final int out[] = new int[2000];
		int index = 0, address = 0;
		
		for (int i = 0, j = 0; i < oc.length; i++) {
			if (o[i] != p[i] || oc[i] != c[i]) {
				final int a = i - j;
				out[index++] = a | (c[i] << 4);
				out[index++] = p[i];
				
				address += a;
				j = i;
				continue;
			}
			
			if (i - j == 15) {
				out[index++] = 15 | (c[i] << 4);
				out[index++] = p[i];

				address += 15;
				j = i;
				continue;
			}
		}
		
		int rest = 999 - address;
		while (rest > 0) {
			int a = rest < 15 ? rest : 15;
			address += a;
			
			out[index++] = a | (c[address] << 4);
			out[index++] = p[address];
			
			rest -= a;
		}
		
		return Arrays.copyOf(out, index);
	}
}