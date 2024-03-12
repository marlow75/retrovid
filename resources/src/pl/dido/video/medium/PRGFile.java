package pl.dido.video.medium;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import pl.dido.video.compression.Compression;

public class PRGFile extends AbstractVideoMedium {

	public PRGFile(final Compression compression) throws IOException {
		super(compression);
	}

	@Override
	public void createMedium(final String fileName) throws IOException {
		this.fileName = fileName;
		
		final BufferedOutputStream prg = new BufferedOutputStream(
				new FileOutputStream(new File(getMediumName(fileName))), 8192);
		try {
			out.setByteAtMarkedPosition(framesCounterMark, (byte) (grabbedFrames - 1));
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

	protected int reserveSpaceForFramesCounter() {
		framesCounterMark = out.size(); // frames 0-255
		out.write(0x0);
		
		return 1;
	}

	protected String getMediumName(final String fileName) {
		return fileName + ".prg";
	}
	
	protected String getLoaderName() {
		return "prg-player.prg";
	}

	public void setFrames(final int frames) {
		out.setByteAtMarkedPosition(framesCounterMark, (byte) frames);
	}

	@Override
	public int getCurrentSize() {
		return size;
	}

	@Override
	public int getMaxSize() {
		return 0xc7fe;
	}

	@Override
	protected int writeFrameHeader() {
		return 0;
	}

	@Override
	protected int getFrameHeaderSize() {
		return 0;
	}
}