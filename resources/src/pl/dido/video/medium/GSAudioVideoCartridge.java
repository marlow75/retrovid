package pl.dido.video.medium;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import pl.dido.video.compression.Compression;

public class GSAudioVideoCartridge extends GSVideoCartridge implements AudioMedium {

	private Logger log = Logger.getLogger(GSAudioVideoCartridge.class.getCanonicalName());
	private final int AUDIO_FRAME_LENGTH = 184; // bytes per audio frame 4,4kHz

	protected ByteArrayOutputStream audioStream = new ByteArrayOutputStream();
	protected ArrayList<Integer> audioFrames = new ArrayList<Integer>();

	public GSAudioVideoCartridge(final Compression compression) throws IOException {
		super(compression);
		audioFrames = new ArrayList<Integer>();
	}

	@Override
	public void createMedium(final String fileName) throws IOException {
		this.fileName = fileName;
		
		updateAudio(); // fill audio buffers for every frame
		final BufferedOutputStream cart = new BufferedOutputStream(
				new FileOutputStream(new File(getMediumName(fileName))), 16384);
		try {
			byte bank = 0;
			out.setShortAtMarkedPosition(framesCounterMark, (short) (grabbedFrames - 1));
			out.flush();
			
			final byte bytes[] = out.toByteArray();
			final int dataLen = bytes.length;

			final byte[] header = getHeader();
			cart.write(header);

			int i = 0;
			while (i < dataLen) {
				if (i % BANK_SIZE == 0) {
					log.fine("bank: " + bank);
					cart.write(this.getChipHeader(bank++));
				}
				
				cart.write(bytes[i++]);
			}
			
			// fill up whole BANK to match declared chip size
			while (i++ % BANK_SIZE != 0)
				cart.write(-1);

		} finally {
			cart.flush();
			cart.close();
		}
	}

	protected void updateAudio() {
		final byte[] samples = audioStream.toByteArray();
		int b = 0, len = samples.length;

		for (final int position : audioFrames) {
			final int e = (b + AUDIO_FRAME_LENGTH <= len) ? AUDIO_FRAME_LENGTH : len - b;
			final byte[] data = Arrays.copyOfRange(samples, b, b + e);

			out.fill(position, data);
			b += e;
		}
	}

	protected int reserveSpaceForAudio() {
		final int mark = out.size();
		for (int i = 0; i < AUDIO_FRAME_LENGTH; i++)
			out.write(0x0);

		return mark;
	}

	@Override
	protected String getLoaderName() {
		return "cart-loader-sound.prg";
	}
	
	@Override
	protected int writeFrameHeader() {
		audioFrames.add(reserveSpaceForAudio());
		return AUDIO_FRAME_LENGTH;
	}
	
	@Override
	protected int getFrameHeaderSize() {
		return AUDIO_FRAME_LENGTH;
	}

	@Override
	public void saveAudioBuffer(final byte[] audio) {
		audioStream.writeBytes(audio);
	}
}