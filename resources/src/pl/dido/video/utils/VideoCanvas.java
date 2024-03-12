package pl.dido.video.utils;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class VideoCanvas extends Canvas {
	
	private static final long serialVersionUID = 7126069230112530254L;
	private BufferedImage img;
	
	public void setImage(final BufferedImage img) {
		this.img = img;
		createBufferStrategy(2);
	}
	
	public void showImage() {
		final Graphics gfx = getBufferStrategy().getDrawGraphics();

		gfx.drawImage(img, 0, 0, this.getWidth(), this.getHeight(), 0, 0, this.getWidth(), this.getHeight(), null);
		gfx.dispose();

		getBufferStrategy().show();
	}
	
	@Override
	public void paint(final Graphics g) {
		super.paint(g);

		if (img != null) {
			final int x = (getWidth() - img.getWidth()) / 2;
			final int y = (getHeight() - img.getHeight()) / 2;
			g.drawImage(img, x, y, this);
		}
	}
}