package pl.dido.video.medium;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import pl.dido.video.compression.Compression;

public class PRGFile extends AbstractVideoMedium {

	public PRGFile(final String mediumName, final Compression compression) throws IOException {
		super(mediumName, compression);
	}

	@Override
	public void writeVideoStream(final String fileName) {
		final byte bytes[] = mediumStream.toByteArray();
		final int dataLen = bytes.length;

		BufferedOutputStream prg = null;
		try {
			prg = new BufferedOutputStream(new FileOutputStream(new File(getMediumName(fileName))), 8192);
			prg.write(0x01); // prg start header
			prg.write(0x08);

			for (int i = 0; i < dataLen; i++)
				prg.write(bytes[i]);

		} catch (final IOException ex) {
			System.out.println("Can't create PRG file !!!");
		} finally {
			if (prg != null)
				try {
					prg.flush();
					prg.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
		}
	}

	protected int reserveSpaceForFramesCounter() {
		framesCounterMark = mediumStream.size(); // frames
		mediumStream.write(0x0);

		return 1;
	}

	protected String getMediumName(final String fileName) {
		return fileName + ".prg";
	}

	@Override
	protected String getPlayerFileName() {
		return "prg-player.prg";
	}

	@Override
	public int getCurrentSize() {
		return mediumSize;
	}

	@Override
	public int getMaxSize() {
		return 0xc7fe;
	}

	@Override
	protected int writeFrameSound() {
		return 0;
	}

	@Override
	protected int getFrameSoundSize() {
		return 0;
	}

	@Override
	protected int reserveSpaceForAudio() {
		return 0;
	}

	@Override
	protected int getStreamBase() {
		return 0x0801;
	}
}