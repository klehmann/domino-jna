package com.mindoo.domino.jna.mime.internal;

import java.io.IOException;
import java.io.InputStream;

import com.mindoo.domino.jna.mime.attachments.IMimeAttachment;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;

/**
 * Implementation of {@link IMimeAttachment} that reads its
 * data from a Jakarta {@link BodyPart}.
 * 
 * @author Karsten Lehmann
 */
public class JakartaMailMimeBodyPartAttachment implements IMimeAttachment {
	private Part m_bodyPart;
	
	public JakartaMailMimeBodyPartAttachment(Part bodyPart) {
		m_bodyPart = bodyPart;
	}
	
	@Override
	public String getFileName() throws IOException {
		try {
			return m_bodyPart.getFileName();
		} catch (MessagingException e) {
			throw new IOException("Error accessing MIME body part", e);
		}
	}

	@Override
	public String getContentType() throws IOException {
		try {
			return m_bodyPart.getContentType();
		} catch (MessagingException e) {
			throw new IOException("Error accessing MIME body part", e);
		}
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		try {
			return m_bodyPart.getInputStream();
		} catch (MessagingException e) {
			throw new IOException("Error accessing MIME body part", e);
		}
	}

	@Override
	public long getFileSize() throws IOException {
		try {
			return m_bodyPart.getSize();
		} catch (MessagingException e) {
			throw new IOException("Error accessing MIME body part", e);
		}
	}
	
}
