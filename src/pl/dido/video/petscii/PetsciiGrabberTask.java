package pl.dido.video.petscii;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import pl.dido.image.petscii.PetsciiRenderer;
import pl.dido.image.utils.Config;
import pl.dido.image.utils.Utils;
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
	private static final int c64SampleRate = 4410;
	
	public PetsciiGrabberTask(final PetsciiVideoConfig config) {
		super(config);
	}

	protected PetsciiRenderer getRenderer() {
		// grabbing with denoiser
		try {
			final Config cfg = (Config) config.config.clone();
			cfg.denoise = config.denoise;

			return new PetsciiRenderer(cfg);
		} catch (final Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public int convert() {
		setProgress(0);

		try (final FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(config.selectedFile)) {
			final String dir = config.selectedFile.getParent() + File.separator + "MOVIES";
			String fileName = config.selectedFile.getName();

			fileName = dir + File.separator + fileName.substring(0, fileName.length() > 8 ? 8 : fileName.length());
			Utils.createDirectory(dir);

			frameGrabber.start();
			frameGrabber.setFrameNumber(config.startVideoFrame);

			final VideoMedium medium = (VideoMedium) getMedium();
			final int grabbedFrames = grabVideo(fileName, frameGrabber, medium);
			frameGrabber.stop();
			
			if (medium instanceof AudioMedium) {
				frameGrabber.start();
				
				final int timestamp = (int) (config.startVideoFrame / frameGrabber.getVideoFrameRate());
				final int startAudioFrame = (int) (timestamp * frameGrabber.getAudioFrameRate());
				
				frameGrabber.setAudioFrameNumber(startAudioFrame);
				grabAudio(frameGrabber, medium);
				
				frameGrabber.stop();
			}

			log.info("Grabbed: " + grabbedFrames + " frames");

			medium.createMedium(fileName + config.startVideoFrame);
			setProgress(100);
		} catch (final IOException e) {
			setProgress(100); // done
			return IO_ERROR;
		} catch (final Exception e) {
			setProgress(100); // done
			return ERROR;
		}

		return OK;
	}

	protected void grabAudio(final FFmpegFrameGrabber frameGrabber, final VideoMedium medium) throws Exception {
		int avg = 0;
		boolean first = true, nibble = false;
		
		short data; // data - low and high nibble
		int currentTime = (int) firstFrameTime;
		
		final int sound8Leap = (int)(1f / c64SampleRate * 1_000_000);
		while (!isCancelled()) {
			final Frame frame = frameGrabber.grab();

			if (frame == null)
				break; // end of file

			if (frame.type == Frame.Type.AUDIO) {
				// check frame time
				final long frameTime = frame.timestamp;  // microseconds
				final long timeDelta = frameTime - currentTime;
				
				final int sampleRate = frame.sampleRate;
				final ShortBuffer buffer = (ShortBuffer) frame.samples[0]; // get first

				buffer.rewind();
				data = 0;
				
				final int frameLength = Math.round(buffer.capacity() / (1f * frame.audioChannels * sampleRate) * 1_000_000); // frame duration in microseconds
				final int bit4BufferSize = frameLength / sound8Leap / 2; // 4 bit sound buffer for single frame
				
				final ByteArrayOutputStream samples = new ByteArrayOutputStream(2 * bit4BufferSize); // 10% margin for blanks
				
				// to soon so play empty bytes
				if (timeDelta > 0) {
					final int mutes = timeDelta / sound8Leap + Math.random() > 0.5f ? 1 : 0;
					for (int i = 0; i < mutes; i++) {
						if (nibble)
							samples.write(0x0);
						else
							data = 0;
						
						nibble = !nibble;
					}
					
					currentTime += mutes * sound8Leap;
				}
				
				int i = 0;
				while (buffer.position() < buffer.capacity()) {
					// two channels so get the average for mono
					int sample = ((buffer.get() + buffer.get()) / 2) & 0xffff;
					if (first) {
						avg = sample;
						first = false;

						continue;
					}

					avg = SoundUtils.lowPass(avg, sample);
					if (i++ % 10 == 0) { // every AUDIO_SKIP bytes 44,1 kHz
						short p = (short) Math.round(0.000228882f * (avg + (2 * SoundUtils.triangularDistribution(0f, 0.5f, 1f) - 1f))); // sound scaling 16->8 & dithering
						p = p > 15 ? 15 : p;
						
						if (nibble) {
							data |= p << 4; // high nibble
							samples.write(data);
						} else
							data = p; // low nibble
						
						nibble = !nibble;
					}
				}
				
				currentTime += frameLength;
				final byte samplesBuffer[] = samples.toByteArray();
				
				((AudioMedium) medium).saveAudioBuffer(samplesBuffer);
			}
			
			frame.close();
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