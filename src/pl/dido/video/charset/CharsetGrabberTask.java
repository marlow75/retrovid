package pl.dido.video.charset;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import cern.colt.bitvector.BitVector;
import pl.dido.image.petscii.PetsciiRenderer;
import pl.dido.image.utils.Config.NEAREST_COLOR;
import pl.dido.image.utils.Gfx;
import pl.dido.image.utils.ProgressListener;
import pl.dido.image.utils.neural.Dataset;
import pl.dido.image.utils.neural.NNUtils;
import pl.dido.image.utils.neural.Network;
import pl.dido.video.compression.CodesCompression;
import pl.dido.video.compression.ColorsCodesCompression;
import pl.dido.video.compression.Compression;
import pl.dido.video.medium.GSAudioVideoCharsetCartridge;
import pl.dido.video.medium.GSVideoCharsetCartridge;
import pl.dido.video.medium.Medium;
import pl.dido.video.petscii.PetsciiGrabberTask;
import pl.dido.video.petscii.PetsciiVideoConfig;

public class CharsetGrabberTask extends PetsciiGrabberTask implements ProgressListener {

	private static final Logger log = Logger.getLogger(CharsetGrabberTask.class.getName());
	private static final int MAX_FRAMES = 30;

	protected final SOMCharsetNetwork som;
	protected Network neural;
	
	protected int progress;
	protected byte charset[];

	public CharsetGrabberTask(final PetsciiVideoConfig config) {
		super(config);

		som = new SOMCharsetNetwork(16, 16);
		som.addProgressListener(this);

		neural = new HL1SoftmaxNetwork(64, 128, 256);
		neural.addProgressListener(this);
		
		progress = 0;
	}
	
	protected PetsciiRenderer getRenderer() {
		return new CharsetRenderer(config);
	}

	protected SOMDataset getChars(final byte pixels[], final NEAREST_COLOR colorAlg)
			throws IOException {
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
			occurrence[Gfx.getColorIndex(colorAlg, petscii.palette, nr, ng, nb)] += (255
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
		nr = petscii.palette[k][0];
		ng = petscii.palette[k][1];
		nb = petscii.palette[k][2];

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
							f = Gfx.getColorIndex(colorAlg, petscii.palette, r, g, b);
						}
					}
				}

				// foreground color
				final int cf[] = petscii.palette[f];
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

	protected SOMDataset grabKeyFrames(final String fileName, final FFmpegFrameGrabber frameGrabber)
			throws IOException {
		final SOMDataset dataset = new SOMDataset();
		final Java2DFrameConverter con = new Java2DFrameConverter();
		int frames = 0;

		while (frames < MAX_FRAMES) {
			Frame frame = frameGrabber.grabFrame(false, true, true, true);

			if (frame == null)
				break; // end of file

			if (frame.type == Frame.Type.VIDEO) {
				petscii.setImage(con.convert(frame));
				((CharsetRenderer) petscii).setupPalette();

				final BufferedImage img = petscii.getImage();
				dataset.addAll(getChars(((DataBufferByte) img.getRaster().getDataBuffer()).getData(),
						NEAREST_COLOR.PERCEPTED));

				frames++;
				setProgress(progress++ % 100);
			}

			frame.close();
		}

		con.close();
		log.info("Frames: " + frames);

		return dataset;
	}

	@Override
	protected Medium getMedium() throws IOException {
		final Compression compression;

		switch (config.compression) {
		default:
			compression = new CodesCompression();
			break;
		case CODES_COLOR:
			compression = new ColorsCodesCompression();
			break;
		}

		switch (config.mediumType) {
		case CRT_SND:
			return new GSAudioVideoCharsetCartridge(compression, charset);
		default:
			return new GSVideoCharsetCartridge(compression, charset);
		}
	}

	public void prepareCharset() throws IOException {
		setProgress(0);

		final FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(config.selectedFile);
		frameGrabber.start();

		frameGrabber.setFrameNumber(config.startFrame);
		final SOMDataset dataset = grabKeyFrames(config.selectedFile.getName(), frameGrabber);
		charset = som.train(dataset);
		
		Files.write(Path.of("c:/temp/charset.bin"), charset);
		//((CharsetRenderer) petscii).replaceCharacters(charset, 87, 169);
		//charset = petscii.getCharset();
		
		if (log.isLoggable(Level.FINEST)) {
			Files.write(Path.of("charset.bin"), charset);
			log.finest("Charset saved.");
		}
		
		final Vector<Dataset> samples = NNUtils.loadData(new ByteArrayInputStream(charset));
		neural.train(samples);
		((CharsetRenderer) petscii).setNeural(neural);
	}

	@Override
	public Integer doInBackground() {
		try {
			Thread.sleep(500); // wait for gui frame
			prepareCharset();

			return convert();
		} catch (final InterruptedException e) {
			return CANCELLED;
		} catch (final IOException e) {
			e.printStackTrace();
			return CANCELLED;
		}
	}

	@Override
	public void notifyProgress() {
		setProgress(progress++ % 100);
	}
}