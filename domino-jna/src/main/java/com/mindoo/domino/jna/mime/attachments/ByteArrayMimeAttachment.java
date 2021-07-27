package com.mindoo.domino.jna.mime.attachments;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of {@link IMimeAttachment} to use a byte array
 * as MIME attachment.
 * 
 * @author Karsten Lehmann
 */
public class ByteArrayMimeAttachment implements IMimeAttachment {
	private byte[] m_data;
	private String m_fileName;
	private String m_contentType;

	public ByteArrayMimeAttachment(byte[] data, String fileName) {
		this(data, fileName, null);
	}
	
	public ByteArrayMimeAttachment(byte[] data, String fileName, String contentType) {
		m_data = data;
		m_fileName = fileName;
		m_contentType = contentType;
	}
	
	@Override
	public String getFileName() throws IOException {
		return m_fileName;
	}

	@Override
	public String getContentType() throws IOException {
		return m_contentType;
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(m_data);
	}

	@Override
	public long getFileSize() throws IOException {
		return m_data.length;
	}

}
