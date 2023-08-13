package pl.dido.video.utils;

import java.io.ByteArrayOutputStream;

public class MarkableByteArrayOutputStream extends ByteArrayOutputStream {
	
	private int markPosition;
	
	public MarkableByteArrayOutputStream(final int iSize) {
		super(iSize);
	}

	public void mark() {
		markPosition = this.size(); 
	}

	public void setByteAtMarkedPosition(final byte b) {
		this.buf[markPosition] = b;
	}
	
	public void setShortAtMarkedPosition(final short b) {
		final byte hi = (byte) (b / 256);
		final byte lo = (byte) (b - hi * 256);
		
		buf[markPosition] = lo;
		buf[markPosition + 1] = hi;
	}

	public void rollback(final int rolbackBytes) {
		this.count -= rolbackBytes;
	}
}
