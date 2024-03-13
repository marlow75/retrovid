package pl.dido.video.medium;

import java.io.BufferedInputStream;
import java.io.IOException;

import pl.dido.image.utils.Utils;
import pl.dido.video.compression.Compression;
import pl.dido.video.utils.MarkableByteArrayOutputStream;

public abstract class AbstractVideoMedium implements VideoMedium {
	
	protected final MarkableByteArrayOutputStream out;

	protected final Compression compression;
	protected int size;

	protected int framesCounterMark;
	protected int grabbedFrames;
	
	protected String fileName;
	
	public AbstractVideoMedium(final Compression compression) throws IOException {
		this.compression = compression;
		out = new MarkableByteArrayOutputStream(256 * 1024);

		size += saveLoader(getLoaderName());
	}
	
	public int saveLoader(final String fileName) throws IOException {
		int sum = 0;
		BufferedInputStream in = null;

		try {
			in = new BufferedInputStream(Utils.getResourceAsStream(fileName, AbstractVideoMedium.class), 8192);

			int data;
			in.read(); // skip loading address
			in.read();

			while ((data = in.read()) != -1) {
				out.write(data);
				sum++;
			}

			reserveSpaceForFramesCounter();
			sum += 2;
		} finally {
			in.close();
		}

		return sum;
	}
	
	public void saveKeyFrame(final int background, final int[] screen, final int[] nibble) {
		final int frameHeaderSize = writeFrameHeader();
		out.write(background); // background color

		// screen
		for (int i = 0; i < 1000; i++)
			out.write(screen[i]);

		// nibbles
		final int bytes[] = compression.packNibble(nibble);
		for (int i = 0; i < 500; i++)
			out.write(bytes[i]);

		size += 1500 + frameHeaderSize;
		grabbedFrames++;
	}
	
	public boolean saveFrame(int background, int[] oldScreen, int[] oldNibble, int[] screen, int[] nibble) {
		final int compressedScreen[] = compression.compress(oldScreen, oldNibble, screen, nibble);
		
		final int compressedLen = compressedScreen.length;
		final int len = compressedLen + getFrameHeaderSize() + 1;

		if (size + len > getMaxSize()) // +1 background
			return false;

		if (!compression.checkSize(compressedScreen))
			throw new RuntimeException("Invalid compression");

		// check & verify compression
		compression.decompress(oldScreen, oldNibble, compressedScreen);
		for (int i = 0; i < 1000; i++)
			if (oldScreen[i] != screen[i])
				throw new RuntimeException("Invalid compression");
		
		writeFrameHeader();
		out.write(background);
		
		for (int i = 0; i < compressedLen; i++)
			out.write(compressedScreen[i]);

		size += len;
		grabbedFrames++;
		
		return true;
	}

	protected abstract int getFrameHeaderSize();
	protected abstract int writeFrameHeader();
	protected abstract int reserveSpaceForFramesCounter();
	protected abstract String getLoaderName();
}