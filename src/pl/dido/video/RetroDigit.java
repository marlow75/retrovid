package pl.dido.video;

import java.nio.ShortBuffer;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Frame.Type;

public class RetroDigit {
	
	public static void main(final String args[]) throws Exception {
		try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("c:/temp/commando.mp4")) {
			grabber.start();

			boolean stopped = false;
				
			int position = 0;
			int bytes = 0;
			
			boolean hi = false;
			short data = 0;
			int line = 0;

			while (!Thread.interrupted() && !stopped) {
				Frame frame = grabber.grab();

				if (frame == null)
					break;

				if (frame.type == Type.AUDIO) {
					final ShortBuffer buf = (ShortBuffer) frame.samples[0];
					buf.rewind();
					float avg = 0, sample = 0;
					
					while (buf.position() < buf.capacity()) {
						// two channels skip right
						sample = (((buf.get() + buf.get()) / 2) & 0xffff);
						//avg = ((sample + 2 * avg) / 3); // sample low pass filter
						avg = sample * 0.1f + 0.9f * avg;
						
						if (position++ % 10 == 9) { // every 8 bytes 44,1 kHz / 8 = 5,5 kHz or 10 bytes = 4,4 kHz
							//sample = (short) ((avg & 0xf000) >> 12);
							sample = Math.round(avg * 0.000228882 + Math.random());
							sample = sample > 15 ? 15: sample;
							
							if (hi) {
								data |= ((short) sample) << 4;
								
								hi = false;
								line = line % 16;

								String val = Integer.toHexString(Short.toUnsignedInt(data));
								switch (line) {
								case 0:
									System.out.print("\tbyte $" + val);
									break;
								case 15:
									System.out.println(",$" + val);
									break;
								default:
									System.out.print(",$" + val);
								}

								line++;
								bytes++;
							} else {
								data = (short) sample;
								hi = true;
							}
						}

						if (position > 36 * 1024 * 16) {
							stopped = true;
							break;
						}
					}
				}
			}

			System.out.println("\n\rbytes: " + bytes);
			
			grabber.stop();
			grabber.release();

		} catch (final Exception exception) {
			System.out.println(exception.getLocalizedMessage());
			System.exit(1);
		}
	}
}