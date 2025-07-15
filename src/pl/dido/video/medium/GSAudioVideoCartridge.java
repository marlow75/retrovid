package pl.dido.video.medium;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import pl.dido.video.compression.Compression;

public class GSAudioVideoCartridge extends GSVideoCartridge implements AudioMedium {

	//private Logger log = Logger.getLogger(GSAudioVideoCartridge.class.getCanonicalName());
	private final int AUDIO_FRAME_LENGTH = 184; // bytes per audio frame 4,41kHz - 4-bits

	protected ByteArrayOutputStream audioStream = new ByteArrayOutputStream();
	protected ArrayList<Integer> audioFrames = new ArrayList<Integer>();

	public GSAudioVideoCartridge(final Compression compression) throws IOException {
		super(compression);
		audioFrames = new ArrayList<Integer>();
	}

	@Override
	public void writeVideoStream(final BufferedOutputStream prg) throws IOException {	
		updateAudio(); // fill audio buffers for every frame
		super.writeVideoStream(prg);
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