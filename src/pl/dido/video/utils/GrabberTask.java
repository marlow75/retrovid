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

	protected VideoConfig config;
	protected final Java2DFrameConverter con;

	protected AbstractRenderer renderer;
	protected float firstFrameTime;

	public GrabberTask(final VideoConfig config) {
		this.config = config;
		
		con = new Java2DFrameConverter();
		renderer = getRenderer();
	}

	public int convert() {
		setProgress(0);

		try (final FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(config.selectedFile)) {
			final String dir = config.selectedFile.getParent() + File.separator + "MOVIES";
			String fileName = config.selectedFile.getName() + config.startVideoFrame;
			
			final int frameLength = String.valueOf(config.startVideoFrame).length();
			if (frameLength < 8) {
				fileName = fileName.substring(0, 8 - frameLength) + config.startVideoFrame;
			} else {
				fileName = String.valueOf(config.startVideoFrame);
				fileName = fileName.substring(0, 8) + config.startVideoFrame;
			}
			
			fileName = dir + File.separator + fileName;
			
			Utils.createDirectory(dir);
			frameGrabber.start();
			
			frameGrabber.setFrameNumber(config.startVideoFrame);
			final Medium medium = getMedium(fileName);
			
			final int grabbedFrames = grabVideo(fileName, frameGrabber, (VideoMedium) medium);
			frameGrabber.stop();
			
			if (medium instanceof AudioMedium) {
				frameGrabber.start();
				
				final int startTime = (int) (config.startVideoFrame / frameGrabber.getVideoFrameRate()); // in sec
				final int stopTime = (int) (startTime + (grabbedFrames * config.getSkipFrameRate()) / frameGrabber.getVideoFrameRate()); // in sec
				
				final int startAudioFrame = (int) (startTime * frameGrabber.getAudioFrameRate());

				frameGrabber.setAudioFrameNumber(startAudioFrame);
				grabAudio(frameGrabber, (AudioMedium) medium, stopTime * 1_000_000);
				
				frameGrabber.stop();
			}

			log.info("Grabbed: " + grabbedFrames + " frames");

			medium.createMedium();
			setProgress(100);
		} catch (final IOException e) {
			e.printStackTrace();
			setProgress(100); // done
			return IO_ERROR;
		} catch (final Exception e) {
			e.printStackTrace();
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

	protected abstract void grabAudio(final FFmpegFrameGrabber frameGrabber, final AudioMedium medium, final long stopAudioTime) throws Exception;
	protected abstract int grabVideo(final String fileName, final FFmpegFrameGrabber frameGrabber, final VideoMedium medium) throws Exception;

	protected abstract Medium getMedium(final String mediumName) throws IOException;
	protected abstract AbstractRenderer getRenderer();
}