package pl.dido.video.petscii;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import pl.dido.image.petscii.PetsciiRenderer;
import pl.dido.image.renderer.AbstractRenderer;
import pl.dido.image.utils.Config;
import pl.dido.video.compression.Compression;
import pl.dido.video.compression.PETSCIICodesCompression;
import pl.dido.video.compression.PETSCIIColorsCodesCompression;
import pl.dido.video.medium.AudioMedium;
import pl.dido.video.medium.GSAudioVideoCartridge;
import pl.dido.video.medium.GSVideoCartridge;
import pl.dido.video.medium.Medium;
import pl.dido.video.medium.PRGFile;
import pl.dido.video.medium.VideoMedium;
import pl.dido.video.utils.GrabberTask;
import pl.dido.video.utils.SoundUtils;
import pl.dido.video.utils.VideoConfig.DITHERING_PDF;

public class PetsciiGrabberTask extends GrabberTask {

	private static final Logger log = Logger.getLogger(PetsciiGrabberTask.class.getCanonicalName());
	private static final int c64SampleRate = 5512;

	public PetsciiGrabberTask(final PetsciiVideoConfig config) {
		super(config);

		//log.setLevel(Level.FINEST);
	}

	protected AbstractRenderer getRenderer() {
		// grabbing with denoiser
		try {
			final Config cfg = (Config) config.config.clone();
			cfg.denoise = config.denoise;

			return new PetsciiRenderer(cfg);
		} catch (final Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	protected void grabAudio(final FFmpegFrameGrabber frameGrabber, final AudioMedium medium, final long stopTime)
			throws Exception {

		float e = 0f;
		int grabbedFrames = 1;

		long currentTime = 0;
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream(256 * 1024);

		final int soundLeap = Math.round(1_000_000f / c64SampleRate);
		boolean first = true, nibble = false;

		// just grab raw sound data
		while (!isCancelled()) {
			
			final Frame frame = frameGrabber.grab();
			if (frame == null)
				break; // end of file

			if (first) {
				currentTime = frame.timestamp;
				first = false;
			}

			if (frame.type == Frame.Type.AUDIO) {
				setProgress(grabbedFrames % 100);
				
				// check frame time
				final long frameTime = frame.timestamp; // microseconds
				final long timeDelta = frameTime - currentTime;

				final ShortBuffer shortBuffer = (ShortBuffer) frame.samples[0];
				shortBuffer.rewind();

				final int bufLen = shortBuffer.capacity();
				final int frameLength = Math.round(bufLen / (1f * frame.audioChannels * frame.sampleRate) * 1_000_000);

				// to soon so play empty bytes
				if (timeDelta > 0) {
					final int mutes = Math.round(timeDelta / soundLeap);

					for (int i = 0; i < mutes; i++) {
						bytes.write(0x0);
						bytes.write(0x0);
					}

					currentTime += mutes * soundLeap;
				}

				int prev = 0;
				while (shortBuffer.position() < bufLen) {
					int data = (shortBuffer.get() + shortBuffer.get()) / 2;

					if (config.lowpassFilter)
						prev = SoundUtils.lowPass(prev, data);
					else
						prev = data;

					data = prev & 0xffff;
					final int hi = data / 256;
					final int lo = data - hi * 256;

					bytes.write(lo);
					bytes.write(hi);
				}

				currentTime += frameLength;
				if (currentTime >= stopTime)
					break;
				
				grabbedFrames++;
			}

			frame.close();
		}

		final byte byteData[] = bytes.toByteArray();
		final int byteLen = byteData.length;

		final int bufLen = byteLen / 2;
		short shortData[] = new short[bufLen];

		int j = 0;
		for (int i = 0; i < byteLen; i += 2) {
			final int lo = byteData[i + 0] & 0xff;
			final int hi = byteData[i + 1] & 0xff;

			shortData[j++] = (short) (hi * 256 + lo);
		}

		float avg = 0;
		int data = 0;

		// log peak/RMS BEFORE compress/normalize
		if (log.isLoggable(Level.FINEST)) {
			float prePeak = 0f;
			double preRmsAcc = 0.0;
			
			for (short sv : shortData) {
				final float abs = Math.abs(sv);
				
				if (abs > prePeak)
					prePeak = abs;
				
				preRmsAcc += sv * (long) sv;
			}
			
			final double preRms = Math.sqrt(preRmsAcc / (double) shortData.length);
			log.finest("Audio BEFORE compressAndNormalize: peak=" + prePeak + " rms=" + preRms);
		}

		shortData = SoundUtils.compressAndNormalize(shortData);

		// compute dynamic scale s (map peak -> ~7) and choose gain (tunable)
		float postPeak = 1f;
		double postRmsAcc = 0.0;
		
		for (short sv : shortData) {
			final float abs = Math.abs(sv);
			
			if (abs > postPeak)
				postPeak = abs;
			
			postRmsAcc += sv * (long) sv;
		}
		
		final double postRms = Math.sqrt(postRmsAcc / (double) shortData.length);
		
		if (log.isLoggable(Level.FINEST))
			log.finest("Audio AFTER compressAndNormalize: peak = " + postPeak + " rms = " + postRms);

		final float s = Math.max(1f, postPeak / 7f); // dynamic scaling: peak -> ~7
		final float gain = 3.0f; // tunable: try 1.0, 1.5, 2.0, ... (higher -> louder but more clipping)

		final ByteArrayOutputStream debug = log.isLoggable(Level.FINEST) ? new ByteArrayOutputStream(128 * 1024) : null;
		final ByteArrayOutputStream samples4bit = new ByteArrayOutputStream(128 * 1024);

		// conversion
		boolean firstSample = true; // do not reuse 'first' which was used above for timestamp
		for (int i = 0; i < bufLen; i++) {
			
			final short sample = shortData[i];
			if (firstSample) {
				
				avg = sample;
				firstSample = false;
				
				continue;
			}

			avg += sample;
			if ((i % 8) == 7) { // every 8
				avg /= 8;

				final float scaledFloat = (avg * gain) / s; // apply gain after dynamic scaling
				final float dither;

				if (config.ditherPDF == DITHERING_PDF.TPDF)
					dither = SoundUtils.triangularDistribution(-0.5f, 0, 0.5f);
				else
					dither = (float) ((Math.random() - Math.random()) * 0.5f);

				final float shaped = scaledFloat + dither + 0.8f * e; // np. 0.8f
				int q = Math.round(shaped);

				e = shaped - q;
				// clamp i mapowanie do 0..15
				if (q > 7)
					q = 7;
				else if (q < -8)
					q = -8;

				final int p = q + 8; // 0..15
				if (log.isLoggable(Level.FINEST))
					debug.write(p * 16);

				if (nibble) {
					data |= p << 4; // high nibble
					samples4bit.write(data);
				} else
					data = p; // low nibble

				nibble = !nibble;
				avg = 0;
			}
		}

		if (nibble)
			samples4bit.write(data & 0x0F);

		final byte samplesBuffer[] = samples4bit.toByteArray();
		((AudioMedium) medium).saveAudioBuffer(samplesBuffer);

		if (debug != null) {
			final AudioFormat format = new AudioFormat((float) 5512, 8, 1, false, true);

			final byte buf[] = debug.toByteArray();
			final ByteArrayInputStream bais = new ByteArrayInputStream(buf);

			final AudioInputStream audiois = new AudioInputStream(bais, format, buf.length);
			final File outFile = new File("debug.wav");

			AudioSystem.write(audiois, AudioFileFormat.Type.WAVE, outFile);
			audiois.close();
		}
	}

	protected int grabVideo(final String fileName, final FFmpegFrameGrabber frameGrabber, final VideoMedium medium)
			throws Exception {
		int oldScreen[] = null, oldNibble[] = null;
		Frame frame = frameGrabber.grabFrame(false, true, true, false);

		firstFrameTime = frame.timestamp;
		final PetsciiRenderer petscii = (PetsciiRenderer) renderer;

		petscii.setImage(con.convert(frame));
		frame.close();

		renderer.imageProcess();
		medium.saveKeyFrame(petscii.getBackgroundColor(), petscii.getScreen(), petscii.getNibble());

		oldScreen = petscii.getScreen().clone();
		oldNibble = petscii.getNibble().clone();

		ImageIO.write(petscii.getImage(), "jpg", new File(fileName + "0.jpg"));

		int frames = 1, grabbedFrames = 1, skipVideo = config.getSkipFrameRate();
		final int lastFrame = frameGrabber.getLengthInVideoFrames();

		log.info("Total frames: " + lastFrame);
		while (!isCancelled()) {
			frame = frameGrabber.grab();

			if (frame == null)
				break; // end of file

			if (frame.type == Frame.Type.VIDEO) {
				if (frames % skipVideo == 0) {
					setProgress(grabbedFrames % 100);

					petscii.setImage(con.convert(frame));
					petscii.imageProcess();

					if (!medium.saveFrame(petscii.getBackgroundColor(), oldScreen, oldNibble, petscii.getScreen(),
							petscii.getNibble()))
						break; // end of medium space

					oldScreen = petscii.getScreen().clone();
					oldNibble = petscii.getNibble().clone();

					grabbedFrames++;
				}

				frames++;
			}

			frame.close();
		}

		log.info("Frame: " + frames);
		return grabbedFrames;
	}

	protected Medium getMedium(final String mediumName) throws IOException {
		final Compression compression;
		final PetsciiVideoConfig petsciiVideoConfig = (PetsciiVideoConfig) config;

		switch (petsciiVideoConfig.compression) {
		default:
			compression = new PETSCIICodesCompression();
			break;
		case CODES_COLOR:
			compression = new PETSCIIColorsCodesCompression();
			break;
		}

		switch (petsciiVideoConfig.mediumType) {
		case CRT:
			return new GSVideoCartridge(mediumName, compression);
		case CRT_SND:
			return new GSAudioVideoCartridge(mediumName, compression);
		default: // PRG
			return new PRGFile(mediumName, compression);
		}
	}
}
