package com.mindoo.domino.jna.mime.internal;

import java.io.IOException;
import java.io.InputStream;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;

import com.mindoo.domino.jna.mime.attachments.IMimeAttachment;

/**
 * Implementation of {@link IMimeAttachment} that reads its
 * data from a javax.mail {@link BodyPart}.
 * 
 * @author Karsten Lehmann
 */
public class JavaxMailMimeBodyPartAttachment implements IMimeAttachment {
	private Part m_bodyPart;
	
	public JavaxMailMimeBodyPartAttachment(Part bodyPart) {
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

}
