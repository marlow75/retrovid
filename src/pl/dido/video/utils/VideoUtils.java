package pl.dido.video.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bytedeco.javacpp.tools.Logger;

import pl.dido.image.utils.Utils;

public class VideoUtils {

	private static final Logger log = Logger.create(VideoUtils.class);

	public static void printArray(final int a[]) {
		final StringBuffer stb = new StringBuffer();

		stb.append(";*************************************\n");

		for (int i = 0; i < a.length; i++) {
			if (i % 8 == 0)
				stb.append("\tbyte ");

			stb.append(a[i] & 0xff);

			if (i % 8 == 7)
				stb.append('\n');
			else if (i != a.length - 1)
				stb.append(",");
		}

		stb.append('\n');
		log.debug(stb.toString());
	}

	public static int saveHeader(final MarkableByteArrayOutputStream out, final int back, final int screen[],
			final int nibble[]) {
		int sum = 0;
		try {
			final BufferedInputStream in = new BufferedInputStream(
					Utils.getResourceAsStream("player.prg", VideoUtils.class), 8192);

			// loading address BASIC
			out.write(0x01);
			out.write(0x08);
			sum += 2;

			int data;
			in.read(); // skip loading address
			in.read();

			while ((data = in.read()) != -1) {
				out.write(data);
				sum++;
			}

			in.close();

			log.debug("Frame counter position: " + sum + " byte from begining of file");
			out.mark();
			
			out.write(0); // frames placeholder
			out.write(back & 0xf); // background color
			sum++;

			log.debug("\tbyte " + back);

			// screen
			for (int i = 0; i < screen.length; i++) {
				out.write(screen[i] & 0xff);
				sum++;
			}

			printArray(screen);

			// color nibbles
			for (int i = 0; i < nibble.length; i++) {
				out.write(nibble[i] & 0xff);
				sum++;
			}

			printArray(nibble);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return sum;
	}

	public static int saveScreen(final ByteArrayOutputStream out, final int back, final int screen[],
			final int nibble[]) {

		int sum = 0;
		// screen
		for (int i = 0; i < screen.length; i++) {
			out.write(screen[i] & 0xff);
			sum++;
		}

		printArray(screen);

		// first background color
		out.write(back & 0xf);
		log.debug("\tbyte " + back);
		sum++;

		// color nibbles
		for (int i = 0; i < nibble.length; i++) {
			out.write(nibble[i] & 0xff);
			sum++;
		}

		printArray(nibble);

		return sum;
	}

	public static int saveScreen(final ByteArrayOutputStream out, final int back, final int screen[]) {
		int sum = 0;

		log.debug("\tbyte " + back);

		// first background color
		out.write(back & 0xf);
		sum++;

		// screen
		for (int i = 0; i < screen.length; i++) {
			out.write(screen[i] & 0xff);
			sum++;
		}

		printArray(screen);

		return sum;
	}
}