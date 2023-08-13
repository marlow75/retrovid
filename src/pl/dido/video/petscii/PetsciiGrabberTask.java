package pl.dido.video.petscii;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

import org.bytedeco.javacpp.tools.Logger;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import pl.dido.image.petscii.PetsciiRenderer;
import pl.dido.image.utils.Utils;
import pl.dido.video.compression.CodesCompression;
import pl.dido.video.compression.ColorsCodesCompression;
import pl.dido.video.compression.Compression;
import pl.dido.video.utils.GSCartridge;
import pl.dido.video.utils.PRGFile;
import pl.dido.video.utils.VideoMedium;

public class PetsciiGrabberTask extends SwingWorker<Integer, Void> {
	private static final Logger log = Logger.create(PetsciiGrabberTask.class);

	public static int OK = 0;
	public static int IO_ERROR = 2;
	
	public static int ERROR = 3;
	public static int CANCELLED = 4;

	protected PetsciiVideoConfig config;
	protected final Java2DFrameConverter con;

	public PetsciiGrabberTask(final PetsciiVideoConfig config) {
		this.config = config;
		con = new Java2DFrameConverter();
	}

	public int convert() {
		setProgress(0);

		try (final FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(config.selectedFile)) {
			final String dir = config.selectedFile.getParent() + File.separator + "MOVIES";
			String fileName = config.selectedFile.getName();
			
			fileName = dir + File.separator + fileName.substring(0, fileName.length() > 8 ? 8 : fileName.length());

			frameGrabber.start();
			frameGrabber.setFrameNumber(config.startFrame);

			final int lastFrame = frameGrabber.getLengthInVideoFrames();
			
			Utils.createDirectory(dir);
			int oldScreen[] = null, oldNibble[] = null;

			final VideoMedium medium = getMedium();
			final PetsciiRenderer petscii = new PetsciiRenderer(config);

			Frame frame = frameGrabber.grabFrame(false, true, true, false);
			petscii.setImage(con.convert(frame));
			frame.close();

			petscii.imageProcess();
			medium.saveKeyFrame(petscii.backgroundColor, petscii.screen, petscii.nibble);

			oldScreen = petscii.screen.clone();
			oldNibble = petscii.nibble.clone();

			ImageIO.write(petscii.getImage(), "jpg", new File(dir + File.separatorChar + "0.jpg"));

			int frames = 1, grabbedFrames = 1;
			log.debug("Total frames: " + lastFrame);

			while (!isCancelled()) {
				frame = frameGrabber.grab();
				
				if (frame == null)
					break;

				if (frame.type == Frame.Type.VIDEO) {
					if (frames % config.skip == 0) {
						setProgress(grabbedFrames % 100);
						
						petscii.setImage(con.convert(frame));
						petscii.imageProcess();

						if (!medium.saveFrame(petscii.backgroundColor, oldScreen, oldNibble, petscii.screen,
								petscii.nibble)) {

							frame.close();
							break;
						}

						oldScreen = petscii.screen.clone();
						oldNibble = petscii.nibble.clone();

						ImageIO.write(petscii.getImage(), "jpg",
								new File(dir + File.separatorChar + String.format("%2d", grabbedFrames) + ".jpg"));

						grabbedFrames++;
					}

					frames++;
				}
				
				frame.close();
			}

			log.debug("Frame: " + frames);
			log.debug("Grabbed: " + grabbedFrames + " frames");

			medium.setFrames((short) (grabbedFrames - 1));
			setProgress(100);

			frameGrabber.stop();
			medium.createMedium(fileName + config.startFrame);
		} catch (final IOException e) {
			setProgress(100); // done
			return IO_ERROR;
		} catch (final Exception e) {
			setProgress(100); // done
			return ERROR;
		}

		return OK;
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

	private VideoMedium getMedium() throws IOException {
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
			return new GSCartridge(compression);
		default: // PRG
			return new PRGFile(compression);
		}
	}
}