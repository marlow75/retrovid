package pl.dido.video.petscii;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import pl.dido.image.petscii.PetsciiRenderer;
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
import pl.dido.video.utils.SoundUtils;

public class PetsciiGrabberTask extends SwingWorker<Integer, Void> {
	private static final Logger log = Logger.getLogger(PetsciiGrabberTask.class.getCanonicalName());

	public static int OK = 0;
	public static int IO_ERROR = 2;

	public static int ERROR = 3;
	public static int CANCELLED = 4;

	protected static int AUDIO_SKIP = 10;

	protected PetsciiVideoConfig config;
	protected final Java2DFrameConverter con;
	
	protected PetsciiRenderer petscii;
	protected float firstFrameTime;
	
	public PetsciiGrabberTask(final PetsciiVideoConfig config) {
		this.config = config;
		con = new Java2DFrameConverter();
		petscii = getRenderer();
	}

	protected PetsciiRenderer getRenderer() {
		return new PetsciiRenderer(config);
	}

	public int convert() {
		setProgress(0);

		try (final FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(config.selectedFile)) {
			final String dir = config.selectedFile.getParent() + File.separator + "MOVIES";
			String fileName = config.selectedFile.getName();

			fileName = dir + File.separator + fileName.substring(0, fileName.length() > 8 ? 8 : fileName.length());
			Utils.createDirectory(dir);

			frameGrabber.start();
			frameGrabber.setFrameNumber(config.startFrame);

			final VideoMedium medium = (VideoMedium) getMedium();
			final int grabbedFrames = grabVideo(fileName, frameGrabber, medium);
			frameGrabber.stop();
			
			if (medium instanceof AudioMedium) {
				frameGrabber.start();
				frameGrabber.setAudioFrameNumber(config.startFrame);

				grabAudio(frameGrabber, medium);
				frameGrabber.stop();
			}

			log.info("Grabbed: " + grabbedFrames + " frames");

			medium.createMedium(fileName + config.startFrame);
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
		float avg = 0;
		boolean first = true, hiNibble = false;

		short data = 0; // data - low and high nibble

		float currentTime = firstFrameTime;
		final float timeQuantum = 1f / 4400 * 1_000_000; // microseconds
		
		while (!isCancelled()) {
			final Frame frame = frameGrabber.grab();

			if (frame == null)
				break; // end of file

			if (frame.type == Frame.Type.AUDIO) {
				final ShortBuffer sampleBuffer = (ShortBuffer) frame.samples[0]; // get first
				int bufferLen = sampleBuffer.capacity() / (2 * frame.audioChannels * AUDIO_SKIP);
				
				final ByteArrayOutputStream c64Samples = new ByteArrayOutputStream(bufferLen);
				sampleBuffer.rewind();
				
				// check frame time
				final float frameTime = frame.timestamp;
				final float delta = frameTime - currentTime;
				
				// blank bytes
				if (delta > 0) {
					final int mutes = (int) (Math.round(delta / timeQuantum) + Math.random());
					
					for (int i = 0; i < mutes; i++) {
						if (hiNibble)
							c64Samples.write(0x0);
						
						hiNibble = !hiNibble;
					}
				}
				
				currentTime += delta + ((sampleBuffer.capacity() / frame.audioChannels / 44_100f) * 1_000_000);
				int i = 0;

				while (sampleBuffer.position() < sampleBuffer.capacity()) {
					// two channels so get the average for mono
					float sample = ((sampleBuffer.get() + sampleBuffer.get()) / 2) & 0xffff;

					if (first) {
						avg = sample;
						first = false;

						continue;
					}

					avg = SoundUtils.lowPass(avg, sample);
					
					if (i++ % AUDIO_SKIP == (AUDIO_SKIP - 1)) { // every AUDIO_SKIP bytes 44,1 kHz
						sample = Math.round(avg * 0.000228882 + Math.random()); // sound scaling 16->8 & dithering
						final short p = (short) (sample > 15 ? 15: sample);
						
						if (hiNibble) {
							data |= p << 4; // high nibble
							c64Samples.write(data);
						} else
							data = p; // low nibble
						
						hiNibble = !hiNibble;
					}
				}

				((AudioMedium) medium).saveAudioBuffer(c64Samples.toByteArray());
			}

			frame.close();
		}
	}

	protected int grabVideo(final String fileName, final FFmpegFrameGrabber frameGrabber, final VideoMedium medium)
			throws Exception {
		int oldScreen[] = null, oldNibble[] = null;

		Frame frame = frameGrabber.grabFrame(false, true, true, false);
		firstFrameTime = frame.timestamp;
		
		petscii.setImage(con.convert(frame));
		frame.close();

		petscii.imageProcess();
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

	@Override
	public Integer doInBackground() {
		try {
			Thread.sleep(500); // wait for gui frame
			return convert();
		} catch (final InterruptedException e) {
			return CANCELLED;
		}
	}

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
		case CRT:
			return new GSVideoCartridge(compression);
		case CRT_SND:
			return new GSAudioVideoCartridge(compression);
		default: // PRG
			return new PRGFile(compression);
		}
	}
}