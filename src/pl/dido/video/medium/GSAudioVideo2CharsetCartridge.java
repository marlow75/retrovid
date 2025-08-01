package pl.dido.video.medium;

import java.io.BufferedInputStream;
import java.io.IOException;

import pl.dido.image.utils.Utils;
import pl.dido.video.compression.Compression;

public class GSAudioVideo2CharsetCartridge extends GSAudioVideoCharsetCartridge {

	public GSAudioVideo2CharsetCartridge(final Compression compression, final byte charset1[], final byte charset2[]) throws IOException {
		super(compression, charset1);		
		save2Charset(charset2);
	}
	
	public void save2Charset(final byte charset2[]) {
		mediumStream.fill(charsetMark + 2048, charset2);	
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
			for (int i = 0; i < 4096; i++)
				mediumStream.write(0x0);
			
			sum += 4096;
			
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
		return "cart-loader-sound-charset2.prg";
	}

	@Override
	protected String getExtendedFileName() {
		return "cart-player-sound-charset2.prg";
	}
}
