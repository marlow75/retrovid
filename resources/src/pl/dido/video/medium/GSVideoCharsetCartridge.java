package pl.dido.video.medium;

import java.io.IOException;

import pl.dido.video.compression.Compression;

public class GSVideoCharsetCartridge extends GSVideoCartridge {
	
	protected byte charset[];

	public GSVideoCharsetCartridge(final Compression compression, final byte[] charset) throws IOException {
		super(compression);
		
		this.charset = charset;
		saveCharset();
	}

	public void saveCharset() {
		for (int i = 0; i < 2048; i++) {
			out.write(charset[i]);
			size++;
		}
	}

	@Override
	protected String getLoaderName() {
		return "cart-loader-charset.prg";
	}
}
