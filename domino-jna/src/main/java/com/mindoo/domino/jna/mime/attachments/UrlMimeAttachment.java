package com.mindoo.domino.jna.mime.attachments;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Implementation of {@link IMimeAttachment} to use a {@link URL}
 * as MIME attachment.
 * 
 * @author Karsten Lehmann
 */
public class UrlMimeAttachment implements IMimeAttachment {
	private URL m_url;
	private String m_fileName;
	private String m_contentType;
	private Long m_fileSize;
	
	/**
	 * Creates a new instance
	 * 
	 * @param url url to read the attachment content. We also try to read the content type and filename.
	 */
	public UrlMimeAttachment(URL url) {
		this(url, null, null);
	}
	
	/**
	 * Creates a new instance
	 * 
	 * @param url url to read the attachment content. We also try to read the content type.
	 * @param fileName attachment filename
	 */
	public UrlMimeAttachment(URL url, String fileName) {
		this(url, fileName, null);
	}
	
	/**
	 * Creates a new instance
	 * 
	 * @param url url to read the attachment content
	 * @param fileName attachment filename
	 * @param contentType mime type
	 */
	public UrlMimeAttachment(URL url, String fileName, String contentType) {
		m_url = url;
		m_fileName = fileName;
		m_contentType = contentType;
	}
	
	@Override
	public String getFileName() throws IOException {
		if (m_fileName==null || m_fileName.length()==0) {
			URLConnection conn = m_url.openConnection();
			
			//try to read "Content-Disposition" header in case this is a http url
			//example: "attachment; filename=myfile.png"
			String disposition = conn.getHeaderField("Content-Disposition");
			
			if (disposition!=null && disposition.length()>0) {
				int iPos = disposition.indexOf('=');
				
				if (iPos != -1) {
					m_fileName = disposition.substring(iPos+1);
				}
			}
			
			if (m_fileName==null || m_fileName.length()==0) {
				//grab last part of filename
				String urlPath = m_url.getPath();
				while (urlPath.endsWith("/")) {
					urlPath = urlPath.substring(0, urlPath.length()-1);
				}
				
				int iPos = urlPath.lastIndexOf('/');
				if (iPos!=-1) {
					m_fileName = urlPath.substring(iPos+1, urlPath.length());
				}
			}
			
			if (m_fileName==null || m_fileName.length()==0) {
				//use a dummy fallback filename
				m_fileName = "attachment.bin";
			}
		}
		return m_fileName;
	}

	@Override
	public String getContentType() throws IOException {
		if (m_contentType==null || m_contentType.length()==0) {
			URLConnection conn = m_url.openConnection();
			m_contentType = conn.getContentType();

			if (m_contentType==null || m_contentType.length()==0) {
				m_contentType = "application/octet-stream";
			}
		}
		return m_contentType;
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return m_url.openStream();
	}

	@Override
	public long getFileSize() throws IOException {
		if (m_fileSize==null) {
			URLConnection conn = m_url.openConnection();
			m_fileSize = Long.parseLong(conn.getHeaderField("content-length"));
		}
		return m_fileSize;
	}
	
}
