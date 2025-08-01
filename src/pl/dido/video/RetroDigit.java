package pl.dido.video;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import pl.dido.video.utils.SoundUtils;

public class RetroDigit {
	
	private static final int c64SampleRate = 5512;

	public static void main(final String args[]) throws Exception {
		boolean first = true;
		
		try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("c:/temp/adele.mp4")) {

			float s = 65536 / 16;
			float d = 0, e = 0f, pd = 0;

			int currentTime = 0;
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream(256 * 1024);

			final int soundLeap = (int) (1f / c64SampleRate * 1_000_000);
			grabber.start();
			// just grab raw sound data
			while (true) {
				final Frame frame = grabber.grab();

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
						avg = SoundUtils.lowPass(avg, data);

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
			
			float avg = 0;
			final ByteArrayOutputStream debug = new ByteArrayOutputStream(128 * 1024);
			
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
					
					debug.write(p * 16);
				}
			}

			final AudioFormat format = new AudioFormat((float) 5512, 8, 1, false, true);
			final byte buf[] = debug.toByteArray();

			final ByteArrayInputStream bais = new ByteArrayInputStream(buf);

			final AudioInputStream audiois = new AudioInputStream(bais, format, buf.length);
			final File outFile = new File("c:/temp/debug.wav");

			AudioSystem.write(audiois, AudioFileFormat.Type.WAVE, outFile);
			audiois.close();

			grabber.stop();
			grabber.release();

		} catch (final Exception exception) {
			System.out.println(exception.getLocalizedMessage());
			System.exit(1);
		}
	}
}