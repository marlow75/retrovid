package pl.dido.video.supercpu;

import pl.dido.image.petscii.PetsciiRenderer2;
import pl.dido.image.utils.BitVector;
import pl.dido.image.utils.Config;
import pl.dido.image.utils.Gfx;
import pl.dido.image.utils.neural.Position;
import pl.dido.image.utils.neural.SOMCharsetNetwork;

public class SupercpuRenderer2 extends PetsciiRenderer2 {
	
	protected SOMCharsetNetwork som = null, som2 = null;
	
	public SupercpuRenderer2(final Config config) {
		super(config);
	}

	public void setupPalette() {
		super.setupPalette();
	}

	public void setCharsets(final byte charset1[], final byte charset2[]) {
		this.charset = charset1;
		this.charset2 = charset2;
	}

	public void setSoms(final SOMCharsetNetwork som, final SOMCharsetNetwork som2) {
		this.som = som;
		this.som2 = som2;
	}
	
	@Override
	protected void imagePostproces() {
		if (som == null) {
			super.imagePostproces();
			return;
		}
		
		// tiles screen and pattern
		final int work[] = new int[64 * 3];
		
		int nr = 0, ng = 0, nb = 0, count = 0;
		final int occurrence[] = new int[16];

		//byte charset[];

		for (int i = 0; i < pixels.length; i += 3) {
			nr = pixels[i] & 0xff;
			ng = pixels[i + 1] & 0xff;
			nb = pixels[i + 2] & 0xff;

			// dimmer better
			occurrence[Gfx.getColorIndex(colorAlg, palette, nr, ng, nb)] += (255 - Gfx.getLuma(nr, ng, nb));
		}

		// get background color with maximum occurrence
		int k = 0;
		for (int i = 0; i < 16; i++) {
			final int o = occurrence[i];
			if (count < o) {
				count = o;
				k = i;
			}
		}

		// most occurrence color as background
		setBackgroundColor(k);

		nr = palette[k][0];
		ng = palette[k][1];
		nb = palette[k][2];

		final float back_luma = Gfx.getLuma(nr, ng, nb);
		final BitVector vec = new BitVector(64);
		
		byte charset[];

		for (int y = 0; y < 200; y += 8) {
			final int p = y * 320 * 3;

			for (int x = 0; x < 320; x += 8) {
				final int offset = p + x * 3;

				int index = 0, f = 0;
				float max_distance = 0;

				// pickup brightest color in 8x8 tile
				for (int y0 = 0; y0 < 8; y0++) {
					for (int x0 = 0; x0 < 24; x0 += 3) {
						final int position = offset + y0 * 320 * 3 + x0;

						final int r = pixels[position] & 0xff;
						final int g = pixels[position + 1] & 0xff;
						final int b = pixels[position + 2] & 0xff;

						work[index++] = r;
						work[index++] = g;
						work[index++] = b;

						final float distance = Gfx.getLuma(r, g, b) - back_luma;
						if (max_distance < distance) {
							max_distance = distance;
							f = Gfx.getColorIndex(colorAlg, palette, r, g, b);
						}
					}
				}

				// foreground color
				final int cf[] = palette[f];
				final int fr = cf[0];
				final int fg = cf[1];
				final int fb = cf[2];
				
				vec.clear();

				for (int y0 = 0; y0 < 8; y0++)
					for (int x0 = 0; x0 < 8; x0++) {
						final int pyx0 = y0 * 24 + x0 * 3;

						final int r = work[pyx0];
						final int g = work[pyx0 + 1];
						final int b = work[pyx0 + 2];

						// fore or background color?
						final float df = Gfx.getDistance(colorAlg, r, g, b, fr, fg, fb);
						final float db = Gfx.getDistance(colorAlg, r, g, b, nr, ng, nb);

						// ones as color of the bright pixels
						if (df <= db)
							vec.set(y0 * 8 + x0);
					}

				final Position pos;
				
				if (y < 100) {
					pos = som.getBMU(vec);
					charset = this.charset;
				} else {
					pos = som2.getBMU(vec);
					charset = this.charset2;
				}
				
				final int code = pos.y * 16 + pos.x;

				// colors
				final int address = (y >> 3) * 40 + (x >> 3);
				nibble[address] = f;
				screen[address] = code;

				// draw character
				for (int y0 = 0; y0 < 8; y0++) {
					final int charset_pos = code * 8 + y0;
					final int charByte = charset[charset_pos];

					for (int x0 = 0; x0 < 8; x0++) {
						final int bitValue = power2[x0];
						final int screen_pos = offset + y0 * 320 * 3 + x0 * 3;

						if ((charByte & bitValue) == bitValue) {
							pixels[screen_pos] = (byte) fr;
							pixels[screen_pos + 1] = (byte) fg;
							pixels[screen_pos + 2] = (byte) fb;
						} else {
							pixels[screen_pos] = (byte) nr;
							pixels[screen_pos + 1] = (byte) ng;
							pixels[screen_pos + 2] = (byte) nb;
						}
					}
				}
			}
		}
	}
}