package pl.dido.video.charset;

import pl.dido.image.petscii.PetsciiConfig;
import pl.dido.image.petscii.PetsciiRenderer;
import pl.dido.image.utils.Utils;
import pl.dido.image.utils.neural.Network;

public class CharsetRenderer extends PetsciiRenderer {

	public CharsetRenderer(final PetsciiConfig config) {
		super(config);
	}

	@Override
	protected void initialize() {
		palette = new int[16][3];
		charset = new byte[2048];

		try {
			final byte commons[] = Utils
					.loadCharset(Utils.getResourceAsStream("common-charset.bin", CharsetRenderer.class));
			replaceCharacters(commons, 0, 87);
		} catch (final Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public void setNeural(final Network neural) {
		this.neural = neural;
	}

	public void replaceCharacters(final byte[] charset, final int charPosition, final int charCount) {
		final int b = charPosition * 8;

		for (int i = b, j = 0; i < b + charCount * 8; i++, j++)
			this.charset[i] = charset[j];
	}

	public void setupPalette() {
		super.setupPalette();
	}
}