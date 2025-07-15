package pl.dido.video.utils;

import java.io.ByteArrayOutputStream;

public class MarkableByteArrayOutputStream extends ByteArrayOutputStream {
	
	public MarkableByteArrayOutputStream(final int iSize) {
		super(iSize);
	}
	
	public void fill(final int position, final byte[] data) {
		int len = position + data.length;
		len = len > buf.length ? buf.length : len;  
		
		for (int i = position, j = 0; i < len; i++, j++)
			buf[i] = data[j];
	}

	public void setByteAtMarkedPosition(final int position, final byte b) {
		buf[position] = b;
	}
	
	public void setShortAtMarkedPosition(final int position, final int b) {
		final byte hi = (byte) (b / 256);
		final byte lo = (byte) (b - hi * 256);
		
		buf[position] = lo;
		buf[position + 1] = hi;
	}

	public void rollback(final int rolbackBytes) {
		count -= rolbackBytes;
	}
	
	public void toHexString(final int skip) {
		int hex = 0;
		String line = "";
		
		for (int i = skip; i < count; i++) {
			line += "$" + Integer.toHexString(buf[i] & 0xff);
			
			if (hex % 16 == 15) {
				System.out.println("\tbyte " + line);
				line = "";
			} else
				line += ", ";
			
			hex += 1;
		}
	}
}
