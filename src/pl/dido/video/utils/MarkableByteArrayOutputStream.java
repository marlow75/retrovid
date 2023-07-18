package pl.dido.video.utils;

import java.io.ByteArrayOutputStream;

public class MarkableByteArrayOutputStream extends ByteArrayOutputStream {
	
	private int markPosition;
	
	public MarkableByteArrayOutputStream(final int size) {
		super(size);
	}

	public void mark() {
		markPosition = this.size(); 
	}

	public void setByteAtMarkedPosition(final byte b) {
		this.buf[markPosition] = b;
	}

	public void rollback(final int rolbackBytes) {
		this.count -= rolbackBytes;
	}
}
