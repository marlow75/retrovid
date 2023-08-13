package pl.dido.video;

import java.nio.ShortBuffer;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Frame.Type;

public class RetroDigit {
	
	private static short lowPass(short p, short o) {
		short delta = (short) (p - o);
		if (delta < 0)
			return (short) (o + (delta <= -2 ? -2 : delta));
		else
			return (short) (o + (delta >= 2 ? 2 : delta));
	}
	
	private static short lowPass2(short p, short o) {
		return (short) ((p + o * 3) / 4);
	}
	
	private static short highPass(short p, short o) {
		short delta = (short) (p - o);
		if (delta < 0)
			return (short) (o + (delta >= -6 ? -6 : delta));
		else
			return (short) (o + (delta <= 6 ? 6 : delta));
	}

	public static void main(final String args[]) throws Exception {
		try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(
				"C:\\Users\\dydorafa\\Documents\\commando-don-t-disturb-my-friend.he-s-dead.mp4")) {
				//"C:\\Users\\dydorafa\\Documents\\aha_Trim.mp4")) {

			grabber.start();

			boolean stopped = false;
			boolean first = true;

			int position = 0;
			
			int avg = 0;
			boolean hi = false;
			
			short data = 0;
			short oldSample = 0;

			while (!Thread.interrupted() && !stopped) {
				Frame frame = grabber.grab();

				if (frame == null)
					break;			

				if (frame.type == Type.AUDIO) {
					final ShortBuffer buf = (ShortBuffer) frame.samples[0];
					buf.rewind();

					int line = 0;
					while (buf.position() < buf.capacity()) {
						// two channels skip right
						short sample = (short) ((buf.get() + buf.get()) / 2);
						
						if (first && sample == 0)
							continue;

						first = false;
						avg += sample;
						
						if (position++ % 8 == 7) { // every 8 bytes 44,1 kHz / 8 = 5,5 kHz
							sample = (short) (avg / 8);
							
							avg = 0;
							
							if (hi) {
								short p = lowPass2((short)((sample & 0xf000) >> 12), oldSample);
								data |= p << 4;
								oldSample = p;

								hi = false;
								line = line % 16;

								String val = Integer.toHexString(Short.toUnsignedInt(data));

								switch (line) {
								case 0:
									System.out.print("byte $" + val);
									break;
								case 15:
									System.out.println(",$" + val);
									break;
								default:
									System.out.print(",$" + val);
								}

								line++;
							} else {
								short p = lowPass2((short)((sample & 0xf000) >> 12), oldSample);
								data = p;
								oldSample = p;

								hi = true;
							}
						}

						if (position > 36 * 1024 * 16) {
							stopped = true;
							break;
						}
					}

					if (!first && line != 15)
						System.out.println();
				}
			}

			grabber.stop();
			grabber.release();

		} catch (final Exception exception) {
			System.exit(1);
		}
	}
	
//	public static void main(final String args[]) throws Exception {
//		try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(
//				"C:\\Users\\dydorafa\\Documents\\commando-don-t-disturb-my-friend.he-s-dead.mp4")) {
//				//"C:\\Users\\dydorafa\\Documents\\aha_Trim.mp4")) {
//
//			grabber.start();
//
//			boolean stopped = false;
//			boolean first = true;
//
//			int position = 0;
//			short data = 0;
//			
//			int avg = 0;
//			boolean hi = false;
//
//			while (!Thread.interrupted() && !stopped) {
//				Frame frame = grabber.grab();
//
//				if (frame == null) {
//					break;
//				}
//
//				if (frame.type == Type.AUDIO) {
//					final ShortBuffer buf = (ShortBuffer) frame.samples[0];
//					buf.rewind();
//
//					int line = 0;
//					while (buf.position() < buf.capacity()) {
//						// two channels skip right
//						short sample = (short) ((buf.get() + buf.get()) / 2);
//						
//						if (first && sample == 0)
//							continue;
//
//						first = false;
//						avg += sample;
//						
//						if (position++ % 8 == 7) { // every 8 bytes 44,1 kHz / 8 = 5,5 kHz
//							sample = (short) (avg / 8);
//							avg = 0;
//							
//							if (hi) {
//								data |= (sample & 0xf000) >> 8;
//
//								hi = false;
//								line = line % 16;
//
//								String val = Integer.toHexString(Short.toUnsignedInt(data));
//
//								switch (line) {
//								case 0:
//									System.out.print("byte $" + val);
//									break;
//								case 15:
//									System.out.println(",$" + val);
//									break;
//								default:
//									System.out.print(",$" + val);
//								}
//
//								line++;
//							} else {
//								data = (short) ((sample & 0xf000) >> 12);
//								hi = true;
//							}
//						}
//
//						if (position > 36 * 1024 * 16) {
//							stopped = true;
//							break;
//						}
//					}
//
//					if (!first && line != 15)
//						System.out.println();
//				}
//			}
//
//			grabber.stop();
//			grabber.release();
//
//		} catch (final Exception exception) {
//			System.exit(1);
//		}
//	}
}