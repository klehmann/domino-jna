package com.mindoo.domino.jna.mime.attachments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public interface IMimeAttachment {
	
	public String getFileName() throws IOException;
	
	public String getContentType() throws IOException;
	
	public InputStream getInputStream() throws IOException;
	
	public long getFileSize() throws IOException;
	
	/**
	 * Returns the attachment content as byte array
	 * 
	 * @return byte array
	 * @throws IOException in case of I/O errors
	 */
	default byte[] readAsBytes() throws IOException {
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		try (InputStream in = getInputStream()) {
			int len;
			byte[] buf = new byte[16384];
			
			while ((len = in.read(buf)) > 0) {
				bOut.write(buf, 0, len);
			}
		}
		return bOut.toByteArray();
	}
	
}