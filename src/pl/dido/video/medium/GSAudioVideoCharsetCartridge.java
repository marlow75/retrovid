package pl.dido.video.medium;

import java.io.BufferedInputStream;
import java.io.IOException;

import pl.dido.image.utils.Utils;
import pl.dido.video.compression.Compression;

public class GSAudioVideoCharsetCartridge extends GSAudioVideoCartridge {
	
	protected int charsetMark;

	public GSAudioVideoCharsetCartridge(final Compression compression, final byte charset[]) throws IOException {
		super(compression);	
		saveCharset(charset);
	}
	
	public void saveCharset(final byte charset[]) {
		mediumStream.fill(charsetMark, charset);
	}
	
	@Override
	public int savePlayer() throws IOException {
		int sum = 0, data;
		BufferedInputStream in = null;

		try {
			// loader
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
			
			// reserve bytes for character set
			charsetMark = mediumStream.size();
			for (int i = 0; i < 2048; i++)
				mediumStream.write(0x0);
			
			sum += 2048;
			
			// player
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

	@Override
	protected String getPlayerFileName() {
		return "cart-loader-sound-charset.prg";
	}
	
	@Override
	protected String getExtendedFileName() {
		return "cart-player-sound-charset.prg";
	}
}