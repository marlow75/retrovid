package pl.dido.video.medium;

import java.io.BufferedInputStream;
import java.io.IOException;

import pl.dido.image.utils.Utils;
import pl.dido.video.compression.Compression;

public class GSVideoCharsetCartridge extends GSVideoCartridge {
	
	protected byte charset[];
	protected int charsetMark;

	public GSVideoCharsetCartridge(final Compression compression, final byte[] charset) throws IOException {
		super(compression);
		
		this.charset = charset;
		saveCharset();
	}

	public void saveCharset() {
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
		return "cart-loader-charset.prg";
	}
	
	@Override
	protected String getExtendedFileName() {
		return "cart-player-charset.prg";
	}
}