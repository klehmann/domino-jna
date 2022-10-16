package com.mindoo.domino.jna.mime.attachments;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.mindoo.domino.jna.utils.ByteBufferInputStream;

/**
 * Implementation of {@link IMimeAttachment} to use a byte array
 * as MIME attachment.
 * 
 * @author Karsten Lehmann
 */
public class ByteBufferMimeAttachment implements IMimeAttachment {
	private ByteBuffer m_data;
	private String m_fileName;
	private int m_fileSize;
	private String m_contentType;

	public ByteBufferMimeAttachment(ByteBuffer data, String fileName) {
		this(data, fileName, null);
	}
	
	public ByteBufferMimeAttachment(ByteBuffer data, String fileName, String contentType) {
		m_data = data.slice();
		m_fileSize = m_data.remaining();
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
		return new ByteBufferInputStream(m_data.slice());
	}

	@Override
	public long getFileSize() throws IOException {
		return m_fileSize;
	}

}
