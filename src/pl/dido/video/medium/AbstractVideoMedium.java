package pl.dido.video.medium;

import java.io.BufferedInputStream;
import java.io.IOException;

import pl.dido.image.utils.Utils;
import pl.dido.video.compression.Compression;
import pl.dido.video.utils.MarkableByteArrayOutputStream;

public abstract class AbstractVideoMedium implements VideoMedium {
	protected final MarkableByteArrayOutputStream mediumStream;

	protected final Compression compression;
	protected int mediumSize;

	protected int framesCounterMark;
	protected int streamAddressMark;

	protected int grabbedFrames;
	protected String fileName;

	public AbstractVideoMedium(final Compression compression) throws IOException {
		this.compression = compression;

		mediumStream = new MarkableByteArrayOutputStream(256 * 1024);
		mediumSize += savePlayer();

		// update stream address
		final int address = mediumSize + getStreamBase();
		mediumStream.setShortAtMarkedPosition(streamAddressMark, address);

		mediumStream.flush();
	}

	public int savePlayer() throws IOException {
		int sum = 0, data;
		BufferedInputStream in = null;

		try {
			in = new BufferedInputStream(Utils.getResourceAsStream(getPlayerFileName(), AbstractVideoMedium.class),
					8192);

			in.read(); // skip loading address
			in.read();

			while ((data = in.read()) != -1) {
				mediumStream.write(data);
				sum++;
			}

			sum += reserveSpaceForFramesCounter();
			sum += reserveSpaceForStreamAddress();
		} finally {
			in.close();
		}

		return sum;
	}

	public void createMedium(final String fileName) {
		this.fileName = fileName;
		
		// update frames counter
		mediumStream.setByteAtMarkedPosition(framesCounterMark, (byte) (grabbedFrames - 1));
		writeVideoStream(fileName);
	}

	protected int reserveSpaceForStreamAddress() {
		streamAddressMark = mediumStream.size();

		mediumStream.write(0x0);
		mediumStream.write(0x0);

		return 2;
	}

	public void saveKeyFrame(final int background, final int[] screen, final int[] nibble) {
		final int frameHeaderSize = writeFrameSound();
		mediumStream.write(background); // background color

		// screen
		for (int i = 0; i < 1000; i++)
			mediumStream.write(screen[i]);

		// nibbles
		final int bytes[] = compression.packNibble(nibble);
		for (int i = 0; i < 500; i++)
			mediumStream.write(bytes[i]);

		mediumSize += 1500 + frameHeaderSize;
		grabbedFrames++;
	}

	public boolean saveFrame(int background, int[] oldScreen, int[] oldNibble, int[] screen, int[] nibble) {
		final int compressedScreen[] = compression.compress(oldScreen, oldNibble, screen, nibble);

		final int compressedLen = compressedScreen.length;
		final int len = compressedLen + getFrameSoundSize() + 1; // +1 background

		if (mediumSize + len > getMaxSize())
			return false;

		if (!compression.checkSize(compressedScreen))
			throw new RuntimeException("Invalid compression");

		// check & verify compression
		compression.decompress(oldScreen, oldNibble, compressedScreen);
		for (int i = 0; i < 1000; i++)
			if (oldScreen[i] != screen[i])
				throw new RuntimeException("Invalid compression");

		writeFrameSound();
		mediumStream.write(background);

		for (int i = 0; i < compressedLen; i++)
			mediumStream.write(compressedScreen[i]);

		mediumSize += len;
		grabbedFrames++;

		return true;
	}

	protected abstract int getFrameSoundSize();

	protected abstract int writeFrameSound();

	protected abstract int reserveSpaceForFramesCounter();

	protected abstract int reserveSpaceForAudio();

	protected abstract String getPlayerFileName();

	protected abstract int getStreamBase();

	protected abstract String getMediumName(final String fileName);

	protected abstract void writeVideoStream(final String fileName);
}