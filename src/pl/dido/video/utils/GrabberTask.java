package pl.dido.video.utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import pl.dido.image.renderer.AbstractRenderer;
import pl.dido.image.utils.Utils;
import pl.dido.video.medium.AudioMedium;
import pl.dido.video.medium.Medium;
import pl.dido.video.medium.VideoMedium;

public abstract class GrabberTask extends SwingWorker<Integer, Void> {
	private static final Logger log = Logger.getLogger(GrabberTask.class.getCanonicalName());

	public static int OK = 0;
	public static int IO_ERROR = 2;

	public static int ERROR = 3;
	public static int CANCELLED = 4;

	protected static int AUDIO_SKIP = 10;

	protected VideoConfig config;
	protected final Java2DFrameConverter con;

	protected AbstractRenderer renderer;
	protected float firstFrameTime;

	public GrabberTask(final VideoConfig config) {
		this.config = config;
		con = new Java2DFrameConverter();
		renderer = getRenderer();
	}

	protected abstract AbstractRenderer getRenderer();

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

	@Override
	public Integer doInBackground() {
		try {
			Thread.sleep(500); // wait for gui frame
			return convert();
		} catch (final InterruptedException e) {
			return CANCELLED;
		}
	}

	protected abstract void grabAudio(final FFmpegFrameGrabber frameGrabber, final VideoMedium medium) throws Exception;
	protected abstract int grabVideo(final String fileName, final FFmpegFrameGrabber frameGrabber,
			final VideoMedium medium) throws Exception;

	protected abstract Medium getMedium() throws IOException;
}