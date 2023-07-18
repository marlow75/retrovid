package pl.dido.video.compression;

import java.util.Arrays;

public class Compression2 extends Compression {
	
	public static final int[] compress(final int o[], final int p[], final int c[]) {
		final int out[] = new int[2000];
		int index = 0, address = 0;
		
		for (int i = 0, j = 0; i < o.length; i++) {
			if (o[i] != p[i]) {
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
	
	public static final int[] decompress(final int s[], final int c[], final int changes[]) {
		int address = 0;
		for (int i = 0; i < changes.length - 1; i += 2) {
			address += changes[i] & 0xf;
			c[address] = (changes[i] & 0xf0) >> 4;
			s[address] = changes[i + 1];
		}
				
		return s;
	}
//	
//	public static final boolean checkAddress(final int changes[]) {
//		int address = 0;
//		for (int i = 0; i < changes.length; i += 2)
//			address += changes[i] & 0xf;
//				
//		return address != 999;
//	}
}
