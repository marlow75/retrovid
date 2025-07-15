package pl.dido.video.medium;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import pl.dido.image.utils.Utils;
import pl.dido.video.compression.Compression;

public class GSVideoCartridge extends PRGFile {
	protected final int BANK_SIZE = 8192;
	
	public GSVideoCartridge(final Compression compression) throws IOException {
		super(compression);
	}
	
	@Override
	public void createMedium(final String fileName) {
		this.fileName = fileName;
		BufferedOutputStream prg = null;
		
		try {
			prg = new BufferedOutputStream(new FileOutputStream(new File(getMediumName(fileName))), 8192);
			// update frames counter
			mediumStream.setShortAtMarkedPosition(framesCounterMark, (short) (grabbedFrames - 1));
			writeVideoStream(prg);
		} catch(final IOException ex) {
			System.out.println("Can't create CRT file !!!");
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
	
	@Override
	public void writeVideoStream(final BufferedOutputStream prg) throws IOException {
		final byte bytes[] = mediumStream.toByteArray();
		final int dataLen = bytes.length;
		
		prg.write(getHeader());
		
		int i = 0;
		byte bank = 0;
		
		for (i = 0; i < dataLen; i++) {
			if (i % BANK_SIZE == 0)
				// block size reached
				prg.write(getChipHeader(bank++));

			prg.write(bytes[i]);
		}

		while (i++ % BANK_SIZE != 0)
			prg.write(-1);
	}
	
	@Override
	public int savePlayer() throws IOException {
		int sum = 0, data;
		BufferedInputStream in = null;

		try {
			try {
				in = new BufferedInputStream(Utils.getResourceAsStream(getPlayerFileName(), AbstractVideoMedium.class), 8192);

				in.read(); // skip loading address
				in.read();
				
				while ((data = in.read()) != -1) {
					mediumStream.write(data);
					sum++;
				}
				
			} finally {
				in.close();
			}
			
			in = new BufferedInputStream(Utils.getResourceAsStream(getExtendedFileName(), AbstractVideoMedium.class), 8192);
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

	protected static byte[] getChipHeader(final byte bank) {
		return new byte[] { 0x43, 0x48, 0x49, 0x50, // CHIP
				0x00, 0x00, 0x20, 0x10, // PACKET LENGTH 0x2010
				0x00, 0x00, // ROM TYPE
				0x00, bank, // BANK NUMBER 0-63
				(byte) 0x80, 0x00, // STARTING LOAD ADDRESS
				0x20, 0x00 }; // ROM SIZE
	}

	protected static byte[] getHeader() {
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

	private static byte[] stringToPetscii(String string) {
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
		framesCounterMark = mediumStream.size(); // frames 0-65535
		mediumStream.write(0x0);
		mediumStream.write(0x0);
		
		return 2;
	}
	
	@Override
	protected String getPlayerFileName() {
		return "cart-loader.prg";
	}
	
	protected String getExtendedFileName() {
		return "cart-player.prg";
	}
	
	@Override
	public int getMaxSize() {
		return 512 * 1024; // 512kb
	}
	
	@Override
	protected String getMediumName(final String fileName) {
		return fileName + ".crt";
	}
	
	@Override
	protected int getStreamBase() {
		return 0x8000;
	}
}