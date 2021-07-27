package com.mindoo.domino.jna.mime.attachments;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implementation of {@link IMimeAttachment} to use a local file
 * as MIME attachment.
 * 
 * @author Karsten Lehmann
 */
public class LocalFileMimeAttachment implements IMimeAttachment {
	private Path m_filePathOnDisk;
	private String m_fileName;
	private String m_contentType;

	public LocalFileMimeAttachment(Path filePathOnDisk) {
		this(filePathOnDisk, filePathOnDisk.getFileName().toString(), null);
	}

	public LocalFileMimeAttachment(Path filePathOnDisk, String fileName) {
		this(filePathOnDisk, fileName, null);
	}
	
	public LocalFileMimeAttachment(Path filePathOnDisk, String fileName, String contentType) {
		m_filePathOnDisk = filePathOnDisk;
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
		return Files.newInputStream(m_filePathOnDisk);
	}

	@Override
	public long getFileSize() throws IOException {
		return Files.size(m_filePathOnDisk);
	}

}
