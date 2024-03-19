package pl.dido.video.charset;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import cern.colt.bitvector.BitVector;
import pl.dido.image.utils.Config.NEAREST_COLOR;
import pl.dido.image.utils.Gfx;

public class SOMTrainer {
	private final static int colors[] = new int[] { 0, 0xFFFFFF, 0x68372B, 0x70A4B2, 0x6F3D86, 0x588D43, 0x352879,
			0xB8C76F, 0x6F4F25, 0x433900, 0x9A6759, 0x444444, 0x6C6C6C, 0x9AD284, 0x6C5EB5, 0x959595 };

	private static final int MAX_FRAMES = 10;

	protected int palette[][] = new int[16][3];
	protected final SOMCharsetNetwork net = new SOMCharsetNetwork(16, 16);

	public int backgroundColor = 0;
	
	protected void setupPalette(final int colorModel) {
		switch (colorModel) {
		case BufferedImage.TYPE_3BYTE_BGR:
			for (int i = 0; i < colors.length; i++) {
				palette[i][0] = colors[i] & 0x0000ff; // blue
				palette[i][1] = (colors[i] & 0x00ff00) >> 8; // green
				palette[i][2] = (colors[i] & 0xff0000) >> 16; // red
			}
			break;
		case BufferedImage.TYPE_INT_RGB:
			for (int i = 0; i < colors.length; i++) {
				palette[i][0] = (colors[i] & 0xff0000) >> 16; // red
				palette[i][1] = (colors[i] & 0x00ff00) >> 8; // green
				palette[i][2] = colors[i] & 0x0000ff; // blue
			}
			break;
		default:
			throw new RuntimeException("Unsupported Pixel format !!!");
		}
	}

	protected SOMDataset getChars(final byte pixels[], final NEAREST_COLOR colorAlg, final int colorModel) throws IOException {
		final SOMDataset dataset = new SOMDataset();

		// tiles screen and pattern
		final int work[] = new int[64 * 3];

		// calculate average
		int nr = 0, ng = 0, nb = 0, count = 0;
		final int occurrence[] = new int[16];

		for (int i = 0; i < pixels.length; i += 3) {
			nr = pixels[i] & 0xff;
			ng = pixels[i + 1] & 0xff;
			nb = pixels[i + 2] & 0xff;

			// dimmer better
			occurrence[Gfx.getColorIndex(colorAlg, palette, nr, ng, nb)] += (255
					- Gfx.getLuma(nr, ng, nb));
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
		backgroundColor = k;

		nr = palette[k][0];
		ng = palette[k][1];
		nb = palette[k][2];

		final float backLuma = Gfx.getLuma(nr, ng, nb);

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

						final float distance = Math.abs(Gfx.getLuma(r, g, b) - backLuma);
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
				
				final BitVector vec = new BitVector(64);

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

				dataset.add(vec);
			}
		}

		return dataset;
	}
	
	protected SOMDataset grabVideo(final String fileName, final FFmpegFrameGrabber frameGrabber) throws IOException {
		final SOMDataset dataset = new SOMDataset();

		final Java2DFrameConverter con = new Java2DFrameConverter();
		int frames = 0, colorModel = 0;
		
		boolean firstFrame = true;
		while (frames < MAX_FRAMES) {
			Frame frame = frameGrabber.grabFrame(false, true, true, true);

			if (frame == null)
				break; // end of file

			if (frame.type == Frame.Type.VIDEO) {
				final BufferedImage img = Gfx.scaleWithStretching(con.getBufferedImage(frame), 320, 200);
				ImageIO.write(img, "jpg", new File(fileName + frames + ".jpg"));
				
				final byte pixels[] = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
				if (firstFrame) {
					colorModel = img.getType();
					setupPalette(colorModel);
					firstFrame = false;
				}
				
				dataset.addAll(getChars(pixels, NEAREST_COLOR.PERCEPTED, colorModel));
				frames++;
			}

			frame.close();
			con.close();
		}
		
		System.out.println("Frames: " + frames);
		return dataset;
	}

	public final static void main(final String args[]) throws IOException {
		final SOMTrainer trainer = new SOMTrainer();
		
		final FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(args[0]);
		frameGrabber.start();
		
		final SOMDataset dataset = trainer.grabVideo(args[0], frameGrabber);
		final SOMCharsetNetwork som = new SOMCharsetNetwork(16, 16);
		final byte charset[] = som.train(dataset);

		final Path path = Paths.get("c:/temp/charset.bin");
		Files.write(path, charset);
	}
}
