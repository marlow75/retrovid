package pl.dido.video.medium;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import pl.dido.image.utils.Utils;
import pl.dido.video.compression.Compression;
import pl.dido.video.utils.MarkableByteArrayOutputStream;

public class PCFile implements VideoMedium, Medium {
	protected final MarkableByteArrayOutputStream mediumStream;

	protected final Compression compression;
	protected int mediumSize;

	protected int framesCounterMark;
	protected int streamAddressMark;

	protected int grabbedFrames;
	protected String fileName;
	
	protected int charsetMark;

	public PCFile(final String mediumName, final Compression compression, final byte charset[]) throws IOException {
		savePlayer();
		
		this.compression = compression;
		this.fileName = mediumName;

		mediumStream = new MarkableByteArrayOutputStream(256 * 1024);
		mediumStream.fill(0, charset);

		mediumStream.flush();
	}
	
	public void createMedium() {
		// update frames counter
		mediumStream.setByteAtMarkedPosition(framesCounterMark, (byte) (grabbedFrames - 1));
		writeVideoStream(fileName);
	}
	
	
	public void savePlayer() {
		int data;
		
		BufferedInputStream in = null;
		BufferedOutputStream out = null;

		try {
			final int index = fileName.lastIndexOf("\\");
			
			final String playerDirectory = fileName.substring(0, index);
			final String shortFileName = fileName.substring(index + 1);
					
			in = new BufferedInputStream(Utils.getResourceAsStream("player.com", AbstractVideoMedium.class), 8192);
			out = new BufferedOutputStream(new FileOutputStream(playerDirectory + "\\player.com"), 8192);

			while ((data = in.read()) != -1)
				out.write(data);
			
			final byte shortFileNameBytes[] = shortFileName.getBytes();
			for (int i = 0; i < 8; i++)
				out.write(shortFileNameBytes[i]);
			
			out.write(".dat".getBytes());
			out.write(0);
			
		} catch (final IOException e) {
			System.out.println("Can't prepare player !!!");
		} finally {
			try {
				in.close();
				out.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void saveKeyFrame(final int background, final int[] screen, final int[] colors) {
		mediumStream.write(background); // background color

		// screen
		for (int i = 0; i < screen.length; i++) {
			mediumStream.write(screen[i]);
			mediumStream.write(colors[i]);
		}

		mediumSize += 2 * screen.length;
		grabbedFrames++;
	}


	public void writeVideoStream(final String fileName) {
		final byte bytes[] = mediumStream.toByteArray();
		final int dataLen = bytes.length;
		
		savePlayer();
		BufferedOutputStream file = null;
		
		try {
			file = new BufferedOutputStream(new FileOutputStream(new File(getMediumName(fileName))), 8192);
			for (int i = 0; i < dataLen; i++)
				file.write(bytes[i]);

		} catch (final IOException ex) {
			System.out.println("Can't create movie file !!!");
		} finally {
			if (file != null)
				try {
					file.flush();
					file.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	public boolean saveFrame(int background, int[] oldScreen, int[] oldNibble, int[] screen, int[] nibble) {
		final int compressedScreen[] = compression.compress(oldScreen, oldNibble, screen, nibble);

		final int compressedLen = compressedScreen.length;
		final int len = compressedLen + 1; // +1 background

		if (!compression.checkSize(compressedScreen, screen.length - 1))
			throw new RuntimeException("Invalid compression");

		// check & verify compression
		compression.decompress(oldScreen, oldNibble, compressedScreen);
		for (int i = 0; i < screen.length; i++)
			if (oldScreen[i] != screen[i])
				throw new RuntimeException("Invalid compression");

		mediumStream.write(background);

		for (int i = 0; i < compressedLen; i++)
			mediumStream.write(compressedScreen[i]);

		mediumSize += len;
		grabbedFrames++;

		return true;
	}

	protected String getMediumName(final String fileName) {
		return fileName + ".dat";
	}

	@Override
	public int getCurrentSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxSize() {
		// TODO Auto-generated method stub
		return 0;
	}
}