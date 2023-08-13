package pl.dido.video.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import pl.dido.image.utils.Utils;
import pl.dido.video.compression.Compression;

public class PRGFile implements VideoMedium {
	protected final MarkableByteArrayOutputStream out;

	protected final Compression compression;
	protected int size;

	public PRGFile(final Compression compression) throws IOException {
		this.compression = compression;

		out = new MarkableByteArrayOutputStream(256 * 1024);
		size = saveLoader(getLoaderName());
	}

	public int saveLoader(final String fileName) throws IOException {
		int sum = 0;
		BufferedInputStream in = null;

		try {
			in = new BufferedInputStream(Utils.getResourceAsStream(fileName, PRGFile.class), 8192);

			int data;
			in.read(); // skip loading address
			in.read();

			while ((data = in.read()) != -1) {
				out.write(data);
				sum++;
			}

			reserveSpaceForFramesCounter();
		} finally {
			in.close();
		}

		return sum;
	}

	protected void reserveSpaceForFramesCounter() {
		out.mark(); // frames 0-255
		out.write(0x0);
	}

	@Override
	public void saveKeyFrame(final int background, final int[] screen, final int[] nibble) {
		out.write(background); // background color

		// screen
		for (int i = 0; i < 1000; i++)
			out.write(screen[i]);

		// nibbles
		final int bytes[] = compression.packNibble(nibble);
		for (int i = 0; i < 500; i++)
			out.write(bytes[i]);

		size += 1500;
	}

	@Override
	public boolean saveFrame(final int background, final int[] oldScreen, final int[] oldNibble, final int[] screen,
			final int[] nibble) throws Exception {
		
		final int compressedScreen[] = compression.compress(oldScreen, oldNibble, screen, nibble);
		final int len = compressedScreen.length;
		
		if (size + len + 1> getMaxSize()) // +1 background 
			return false;
		
		if (!compression.checkSize(compressedScreen))
			throw new Exception("Compression exception");
		
		// check & verify compression
		compression.decompress(oldScreen, oldNibble, compressedScreen);
		for (int i = 0; i < 1000; i++)
			if (oldScreen[i] != screen[i])
				throw new Exception("Compression exception");

		out.write(background);
		for (int i = 0; i < len; i++)
			out.write(compressedScreen[i]);

		size += len + 1;
		return true;
	}
	
	protected String getMediumName(final String fileName) {
		return fileName + ".prg";
	}

	@Override
	public void rewind(final int lastFrameSize) {
		out.rollback(lastFrameSize);
	}

	@Override
	public void createMedium(final String fileName) throws IOException {
		final BufferedOutputStream prg = new BufferedOutputStream(new FileOutputStream(new File(getMediumName(fileName))), 8192);
		try {
			out.flush();

			final byte bytes[] = out.toByteArray();
			final int dataLen = bytes.length;

			prg.write(0x01); // prg start header
			prg.write(0x08);

			for (int i = 0; i < dataLen; i++)
				prg.write(bytes[i]);
			
		} finally {
			prg.flush();
			prg.close();
		}
	}

	protected String getLoaderName() {
		return "prg-player.prg";
	}

	@Override
	public void setFrames(final short frames) {
		out.setByteAtMarkedPosition((byte) frames);
	}

	@Override
	public int getCurrentSize() {
		return size;
	}
	
	@Override
	public int getMaxSize() {
		return 0xc7fe;
	}
}
