package pl.dido.video;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Frame.Type;

public class RetroDigit2 {
	
	public static void main(final String args[]) throws Exception {
		try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("c:/temp/for-a-few-dollars-more-final-duel-hd.mp4")) {
			grabber.start();
			boolean stopped = false;
			
			long time = 0;
			final float sound8Leap = 1f / 4400 * 1_000_000;

			while (!Thread.interrupted() && !stopped) {
				Frame frame = grabber.grab();

				if (frame == null)
					break;

				if (frame.type == Type.AUDIO) {
					System.out.println("\ncurrent time: " + time);
					long delta = frame.timestamp - time;
					if (delta > 0)
						time += ((int)(delta / sound8Leap) * sound8Leap) + Math.random() > 0.5f ? 1 : 0;
					
					System.out.println("timestamp: " + frame.timestamp);
					System.out.println("channels: " + frame.audioChannels);
					System.out.println("sample rate: " + frame.sampleRate);
					System.out.println("stream: " + frame.streamIndex);
					System.out.println("buffers: " + frame.samples.length);
					
					int size = (int)(frame.samples[0].capacity() / (1f * frame.audioChannels * frame.sampleRate) * 1_000_000);
					time += size;
					
					System.out.println("length: " + size);
				}
			}
			
			grabber.stop();
			grabber.release();

		} catch (final Exception exception) {
			System.out.println(exception.getLocalizedMessage());
			System.exit(1);
		}
	}
}