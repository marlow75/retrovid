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
import pl.dido.video.compression.CodesCompression;
import pl.dido.video.compression.ColorsCodesCompression;
import pl.dido.video.compression.Compression;
import pl.dido.video.medium.AudioMedium;
import pl.dido.video.medium.GSAudioVideoCartridge;
import pl.dido.video.medium.GSVideoCartridge;
import pl.dido.video.medium.Medium;
import pl.dido.video.medium.PRGFile;
import pl.dido.video.medium.VideoMedium;
import pl.dido.video.utils.GrabberTask;
import pl.dido.video.utils.SoundUtils;

public class PetsciiGrabberTask extends GrabberTask {
	private static final Logger log = Logger.getLogger(PetsciiGrabberTask.class.getCanonicalName());
	private static final int c64SampleRate = 5512; // 4410;

	public PetsciiGrabberTask(final PetsciiVideoConfig config) {
		super(config);
	}

	protected AbstractRenderer getRenderer() {
		// grabbing with denoiser
		try {
			final Config cfg = (Config) config.petsciiConfig.clone();
			cfg.denoise = config.denoise;

			return new PetsciiRenderer(cfg);
		} catch (final Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	protected void grabAudio(final FFmpegFrameGrabber frameGrabber, final VideoMedium medium, final int stopAudioFrame)
			throws Exception {
		
		float s = 65536 / 16;
		float d = 0, e = 0f, pd = 0;

		int currentTime = 0;
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream(256 * 1024);

		final int soundLeap = (int) (1f / c64SampleRate * 1_000_000);
		boolean first = true, nibble = false;
		
		// just grab raw sound data
		while (!isCancelled()) {
			final Frame frame = frameGrabber.grab();

			if (frame == null)
				break; // end of file

			if (frame.type == Frame.Type.AUDIO) {
				// check frame time
				final long frameTime = frame.timestamp; // microseconds
				final long timeDelta = frameTime - currentTime;

				final int sampleRate = frame.sampleRate;
				final ShortBuffer shortBuffer = (ShortBuffer) frame.samples[0];

				shortBuffer.rewind();

				final int bufLen = shortBuffer.capacity();
				// frame duration in microseconds
				final int frameLength = Math.round(bufLen / (1f * frame.audioChannels * sampleRate) * 1_000_000);

				// to soon so play empty bytes
				if (timeDelta > 0) {
					final int mutes = timeDelta / soundLeap + Math.random() > 0.5f ? 1 : 0;
					for (int i = 0; i < mutes; i++) {
						bytes.write(0x0);
						bytes.write(0x0);
					}

					currentTime += mutes * soundLeap;
				}

				int avg = 0;
				while (shortBuffer.position() < bufLen) {
					int data = (shortBuffer.get() + shortBuffer.get()) / 2;
					
					if (config.lowpassFilter)
						avg = SoundUtils.lowPass(avg, data);
					else
						avg = data;

					data = avg & 0xffff;
					final int hi = data / 256;
					final int lo = data - hi * 256;
					
					bytes.write(lo);
					bytes.write(hi);
				}
				
				currentTime += frameLength;
			}

			frame.close();
		}
		
		final byte byteData[] = bytes.toByteArray();
		final int byteLen = byteData.length;
		
		final int bufLen = byteLen / 2;
		final short shortData[] = new short[bufLen];
		
		int j = 0;
		for (int i = 0; i < byteLen; i += 2) {
			final int lo = byteData[i + 0] & 0xff;
			final int hi = byteData[i + 1] & 0xff;
	
			shortData[j++] = (short) (hi * 256 + lo);
		}
		
		SoundUtils.peekNormalization(shortData);
		
		float avg = 0; int data = 0;
		
		final ByteArrayOutputStream debug = log.isLoggable(Level.FINEST) ? new ByteArrayOutputStream(128 * 1024) : null;
		final ByteArrayOutputStream samples4bit = new ByteArrayOutputStream(128 * 1024);
		
		// conversion
		for (int i = 0; i < bufLen; i++) {
			final short sample = shortData[i];
			if (first) {
				avg = sample;
				first = false;

				continue;
			}

			avg += sample;
			if (i % 8 == 0) { // every AUDIO_SKIP bytes 44,1 kHz
				avg /= 8;
				
				pd = d;
				d = (float) (2 * SoundUtils.triangularDistribution(0, 0.5f, 1) - 1);

				// scaling + dithering
				final int scaled = (int) (avg / s);
				final float result = scaled + d + 0.8f * e * (d - pd);

				// downsampling
				int p = (int) (result);
				e = scaled - p;
				
				// saturation
				p = p > 8 ? 8 : p < -7 ? -7 : p;
				p += 8;
				
				if (log.isLoggable(Level.FINEST)) 
					debug.write(p * 16);
				
				if (nibble) {
					data |= p << 4; // high nibble
					samples4bit.write(data);
				} else
					data = p; // low nibble

				nibble = !nibble;
			}
		}

		final byte samplesBuffer[] = samples4bit.toByteArray();
		((AudioMedium) medium).saveAudioBuffer(samplesBuffer);
		
		if (log.isLoggable(Level.FINEST)) {
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

					if (log.isLoggable(Level.FINE))
						ImageIO.write(petscii.getImage(), "jpg",
								new File(fileName + String.format("%2d", grabbedFrames) + ".jpg"));

					grabbedFrames++;
				}

				frames++;
			}

			frame.close();
		}

		log.info("Frame: " + frames);
		return grabbedFrames;
	}

	protected Medium getMedium() throws IOException {
		final Compression compression;
		final PetsciiVideoConfig petsciiVideoConfig = (PetsciiVideoConfig) config;

		switch (petsciiVideoConfig.compression) {
		default:
			compression = new CodesCompression();
			break;
		case CODES_COLOR:
			compression = new ColorsCodesCompression();
			break;
		}

		switch (petsciiVideoConfig.mediumType) {
		case CRT:
			return new GSVideoCartridge(compression);
		case CRT_SND:
			return new GSAudioVideoCartridge(compression);
		default: // PRG
			return new PRGFile(compression);
		}
	}
}