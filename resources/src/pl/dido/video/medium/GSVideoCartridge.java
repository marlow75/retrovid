package pl.dido.video.medium;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import pl.dido.video.compression.Compression;

public class GSVideoCartridge extends PRGFile {
	protected final int BANK_SIZE = 8192;
	
	public GSVideoCartridge(final Compression compression) throws IOException {
		super(compression);
	}

	@Override
	public void createMedium(final String fileName) throws IOException {
		this.fileName = fileName;
		
		final BufferedOutputStream cart = new BufferedOutputStream(new FileOutputStream(new File(getMediumName(fileName))), 16384);
		try {
			byte bank = 0;
			out.setShortAtMarkedPosition(framesCounterMark, (short) (grabbedFrames - 1));
			out.flush();
			
			byte bytes[] = out.toByteArray();
			final int dataLen = bytes.length;

			final byte[] header = getHeader();
			cart.write(header);

			int i = 0;
			for (i = 0; i < dataLen; i++) {
				if (i % BANK_SIZE == 0)
					// block size reached
					cart.write(getChipHeader(bank++));

				cart.write(bytes[i]);
			}

			while (i++ % BANK_SIZE != 0)
				cart.write(-1);

		} finally {
			cart.flush();
			cart.close();
		}
	}

	protected byte[] getChipHeader(final byte bank) {
		return new byte[] { 0x43, 0x48, 0x49, 0x50, // CHIP
				0x00, 0x00, 0x20, 0x10, // PACKET LENGTH 0x2010
				0x00, 0x00, // ROM TYPE
				0x00, bank, // BANK NUMBER 0-63
				(byte) 0x80, 0x00, // STARTING LOAD ADDRESS
				0x20, 0x00 }; // ROM SIZE
	}

	protected byte[] getHeader() {
		final byte p[] = new byte[64];
		final byte b[] = new byte[] { 0x43, 0x36, 0x34, 0x20, 0x43, 0x41, 0x52, 0x54, 0x52, 0x49, 0x44, 0x47, 0x45,
				0x20, 0x20, 0x20, // C64 CARTRIDGE
				0x00, 0x00, 0x00, 0x40, // HEADER LENGTH
				0x01, 0x00, // VERSION
				0x00, 0x0f, // GAME SYSTEM
				0x00, // EXROM LINE
				0x01, // GAME LINE
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }; // FUTURE USE

		System.arraycopy(b, 0, p, 0, 32);
		System.arraycopy(stringToPetscii("RETROVID PLAYER"), 0, p, 32, 32);

		return p;
	}

	private byte[] stringToPetscii(String string) {
		string = string.toUpperCase();
		final byte p[] = new byte[32];

		int l = string.length();
		l = l > 32 ? 32 : l;

		int i;
		for (i = 0; i < l; i++)
			p[i] = (byte) string.charAt(i);

		for (int j = i; j < 32; j++)
			p[j] = 0;

		return p;
	}
	
	@Override
	protected int reserveSpaceForFramesCounter() {
		framesCounterMark = out.size(); // frames 0-65535
		out.write(0x0);
		out.write(0x0);
		
		return 2;
	}
	
	@Override
	protected String getLoaderName() {
		return "cart-loader.prg";
	}
	
	public int getMaxSize() {
		return 512 * 1024; // 512kb
	}
	
	@Override
	protected String getMediumName(final String fileName) {
		return fileName + ".crt";
	}
}