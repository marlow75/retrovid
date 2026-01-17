package pl.dido.video.supercpu;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import pl.dido.image.renderer.AbstractRenderer;
import pl.dido.image.utils.BitVector;
import pl.dido.image.utils.Config;
import pl.dido.image.utils.Config.NEAREST_COLOR;
import pl.dido.image.utils.Gfx;
import pl.dido.image.utils.neural.SOMCharsetNetwork;
import pl.dido.image.utils.neural.SOMDataset;
import pl.dido.video.compression.Compression;
import pl.dido.video.compression.PETSCIICodesCompression;
import pl.dido.video.compression.PETSCIIColorsCodesCompression;
import pl.dido.video.medium.GSAudioVideo2CharsetCartridge;
import pl.dido.video.medium.GSVideo2CharsetCartridge;
import pl.dido.video.medium.Medium;
import pl.dido.video.petscii.PetsciiVideoConfig;

public class SupercpuGrabberTask2 extends SupercpuGrabberTask {

	protected static final Logger log = Logger.getLogger(SupercpuGrabberTask.class.getName());
	protected static final int MAX_FRAMES = 30;

	protected final SOMCharsetNetwork som2;
	protected byte charset2[];

	protected int progress;

	public SupercpuGrabberTask2(final PetsciiVideoConfig config) {
		super(config);

		som2 = new SOMCharsetNetwork(16, 16);
		som2.addProgressListener(this);
	}

	@Override
	protected AbstractRenderer getRenderer() {
		try {
			final Config cfg = (Config) config.config.clone();
			return new SupercpuRenderer2(cfg);
		} catch (final Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	protected Medium getMedium(final String mediumName) throws IOException {
		final Compression compression;
		SupercpuVideoConfig charsetVideoConfig = (SupercpuVideoConfig) config;

		switch (charsetVideoConfig.compression) {
		default:
			compression = new PETSCIICodesCompression();
			break;
		case CODES_COLOR:
			compression = new PETSCIIColorsCodesCompression();
			break;
		}

		switch (charsetVideoConfig.mediumType) {
		case CRT_SND:
			return new GSAudioVideo2CharsetCartridge(mediumName, compression, charset, charset2);
		default:
			return new GSVideo2CharsetCartridge(mediumName, compression, charset, charset2);
		}
	}
	
	protected SOMDataset<BitVector>[] getCharacterset2(final byte pixels[], final NEAREST_COLOR colorAlg) throws IOException {
		@SuppressWarnings("unchecked")
		final SOMDataset<BitVector> dataset[] = new SOMDataset[2];
		
		dataset[0] = new SOMDataset<BitVector>();
		dataset[1] = new SOMDataset<BitVector>();

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
			occurrence[Gfx.getColorIndex(colorAlg, renderer.palette, nr, ng, nb)] += (255 - Gfx.getLuma(nr, ng, nb));
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
		nr = renderer.palette[k][0];
		ng = renderer.palette[k][1];
		nb = renderer.palette[k][2];

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
							f = Gfx.getColorIndex(colorAlg, renderer.palette, r, g, b);
						}
					}
				}

				// foreground color
				final int cf[] = renderer.palette[f];
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

				if (y < 100)
					dataset[0].add(vec);
				else
					dataset[1].add(vec);
			}
		}
		
		return dataset;
	}
	
	protected SOMDataset<BitVector>[] grabKeyFrames2(final String fileName, final FFmpegFrameGrabber frameGrabber)
			throws IOException {
		@SuppressWarnings("unchecked")
		final SOMDataset<BitVector> dataset[] = new SOMDataset[2];
		
		dataset[0] = new SOMDataset<BitVector>();
		dataset[1] = new SOMDataset<BitVector>();

		final Java2DFrameConverter con = new Java2DFrameConverter();
		
		int frames = 0;
		while (frames < MAX_FRAMES) {
			Frame frame = frameGrabber.grabFrame(false, true, true, true);

			if (frame == null)
				break; // end of file

			if (frame.type == Frame.Type.VIDEO) {
				renderer.setImage(con.convert(frame));
				((SupercpuRenderer2) renderer).setupPalette();

				final BufferedImage img = renderer.getImage();
				final SOMDataset<BitVector> result[] = getCharacterset2(((DataBufferByte) img.getRaster().getDataBuffer()).getData(),
						NEAREST_COLOR.PERCEPTED);
				
				dataset[0].addAll(result[0]);
				dataset[1].addAll(result[1]);
				
				frames++;
				setProgress(progress++ % 100);
			}

			frame.close();
		}

		con.close();
		log.info("Frames: " + frames);

		return dataset;
	}

	public void prepareCharset() throws IOException {
		setProgress(0);

		final FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(config.selectedFile);
		frameGrabber.start();

		frameGrabber.setFrameNumber(config.startVideoFrame);
		final SOMDataset<BitVector> datasets[] = grabKeyFrames2(config.selectedFile.getName(), frameGrabber);
		
		charset = som.train(datasets[0]);
		charset2 = som2.train(datasets[1]);

		if (log.isLoggable(Level.FINEST)) {
			Files.write(Path.of("charset1.bin"), charset);
			Files.write(Path.of("charset2.bin"), charset2);
			log.finest("Charsets saved.");
		}

		((SupercpuRenderer2) renderer).setSoms(som, som2);
		((SupercpuRenderer2) renderer).setCharsets(charset, charset2);
	}
}