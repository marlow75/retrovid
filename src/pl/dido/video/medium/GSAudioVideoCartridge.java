package pl.dido.video.medium;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import pl.dido.video.compression.Compression;

public class GSAudioVideoCartridge extends GSVideoCartridge implements AudioMedium, Medium {
	private final int AUDIO_FRAME_LENGTH = 230; // bytes per audio frame 5512 Hz - 4-bits
	protected ArrayList<Integer> audioFrames = new ArrayList<Integer>();
	
	protected ByteArrayOutputStream audioStream = new ByteArrayOutputStream();

	public GSAudioVideoCartridge(final String mediumName, final Compression compression) throws IOException {
		super(mediumName, compression);
		audioFrames = new ArrayList<Integer>();
	}

	@Override
	public void writeVideoStream(final String fileName) {	
		updateAudio(); // fill audio buffers for every frame
		super.writeVideoStream(fileName);
	}

	protected void updateAudio() {
		final byte[] samples = audioStream.toByteArray();
		int b = 0, len = samples.length;

		for (final int position : audioFrames) {
			final int e = (b + AUDIO_FRAME_LENGTH <= len) ? AUDIO_FRAME_LENGTH : len - b;
			final byte[] data = Arrays.copyOfRange(samples, b, b + e);
			
			mediumStream.fill(position, data);
			b += e;
		}
	}

	@Override
	protected int reserveSpaceForAudio() {
		final int mark = mediumStream.size();
		for (int i = 0; i < AUDIO_FRAME_LENGTH; i++)
			mediumStream.write(0x0);

		return mark;
	}

	@Override
	protected String getPlayerFileName() {
		return "cart-loader-sound.prg";
	}

	@Override
	protected String getExtendedFileName() {
		return "cart-player-sound.prg";
	}
	
	@Override
	protected int writeFrameSound() {
		audioFrames.add(reserveSpaceForAudio());
		return AUDIO_FRAME_LENGTH;
	}
	
	@Override
	protected int getFrameSoundSize() {
		return AUDIO_FRAME_LENGTH;
	}

	@Override
	public void saveAudioBuffer(final byte[] audio) {
		audioStream.writeBytes(audio);
	}
}