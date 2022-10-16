package com.mindoo.domino.jna.mime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.mime.attachments.ByteArrayMimeAttachment;
import com.mindoo.domino.jna.mime.attachments.IMimeAttachment;
import com.mindoo.domino.jna.mime.attachments.LocalFileMimeAttachment;
import com.mindoo.domino.jna.mime.attachments.UrlMimeAttachment;

/**
 * Container for text and binary data of an item of type {@link NotesItem#TYPE_MIME_PART}.
 */
public class MIMEData {
	private static final String MIMETYPE_TEXT_PLAIN = "text/plain";
	private static final String MIMETYPE_TEXT_HTML = "text/html";
	
	private Map<String,String> m_mailBodyByContentType;
	private Map<String,IMimeAttachment> m_embeds;
	private List<IMimeAttachment> m_attachments;
	private int m_uniqueCidCounter=1;
	
	private String m_toString;
	
	public MIMEData() {
		this("", "", null, null);
	}
	
	public MIMEData(String html, String text,
			Map<String,IMimeAttachment> embeds, List<IMimeAttachment> attachments) {
		
		m_mailBodyByContentType = new HashMap<>();
		setHtml(html);
		setPlainText(text);
		
		m_embeds = embeds==null ? new HashMap<>() : new HashMap<>(embeds);
		m_attachments = attachments==null ? new ArrayList<>() : new ArrayList<>(attachments);
	}
	
	/**
	 * Returns the html content
	 * 
	 * @return HTML, not null
	 */
	public String getHtml() {
		return m_mailBodyByContentType.getOrDefault(MIMETYPE_TEXT_HTML, "");
	}
	
	/**
	 * Sets the HTML content
	 * 
	 * @param html html
	 */
	public void setHtml(String html) {
		if (html==null) {
			m_mailBodyByContentType.remove(MIMETYPE_TEXT_HTML);
		}
		else {
			m_mailBodyByContentType.put(MIMETYPE_TEXT_HTML, html);
		}
		m_toString = null;
	}

	/**
	 * Returns alternative plaintext content
	 * 
	 * @return plaintext content or empty string
	 */
	public String getPlainText() {
		return m_mailBodyByContentType.getOrDefault(MIMETYPE_TEXT_PLAIN, "");
	}
	
	/**
	 * Sets the alternative plaintext content
	 * 
	 * @param text plaintext
	 */
	public void setPlainText(String text) {
		if (text==null) {
			m_mailBodyByContentType.remove(MIMETYPE_TEXT_PLAIN);
		}
		else {
			m_mailBodyByContentType.put(MIMETYPE_TEXT_PLAIN, text);
		}
		m_toString = null;
	}
	
	/**
	 * Sets mail body content of a specific type
	 * 
	 * @param contentType content type, e.g. "text/html" or "text/plain"
	 * @param content body content
	 */
	public void setBodyContent(String contentType, String content) {
		if (content==null) {
			m_mailBodyByContentType.remove(contentType);
		}
		else {
			m_mailBodyByContentType.put(contentType, content);
		}
		m_toString = null;
	}
	
	/**
	 * Returns mail body content of a specific type
	 * 
	 * @param contentType content type
	 * @return mail body content
	 */
	public Optional<String> getBodyContent(String contentType) {
		if (m_mailBodyByContentType.containsKey(contentType)) {
			return Optional.of(m_mailBodyByContentType.get(contentType));
		}
		else {
			return Optional.empty();
		}
	}
	
	/**
	 * Returns all defined content types for the mail body
	 * 
	 * @return content types, e.g. "text/html" or "text/plain"
	 */
	public Iterable<String> getBodyContentTypes() {
		return m_mailBodyByContentType.keySet();
	}
	
	/**
	 * Returns an attachment for a content id
	 * 
	 * @param cid content id
	 * @return attachment or null if not found
	 */
	public IMimeAttachment getEmbed(String cid) {
		return m_embeds.get(cid);
	}
	
	/**
	 * Adds an inline file
	 * 
	 * @param attachment attachment
	 * @return unique content id
	 */
	public String embed(IMimeAttachment attachment) {
		if (attachment==null) {
			throw new IllegalArgumentException("Attachment cannot be null");
		}

		//find a unique content id
		String cid;
		do {
			cid = "att_"+(m_uniqueCidCounter++)+"@jnxdoc";
		}
		while (m_embeds.containsKey(cid));
		
		m_embeds.put(cid, attachment);
		m_toString = null;

		return cid;
	}
	
	/**
	 * Adds/changes an inline file with a given content id
	 * 
	 * @param cid content id
	 * @param attachment attachment
	 */
	public void embed(String cid, IMimeAttachment attachment) {
		if (attachment==null) {
			m_embeds.remove(cid);
		}
		else {
			m_embeds.put(cid, attachment);
		}
		m_toString = null;
	}
	
	/**
	 * Removes an inline file
	 * 
	 * @param cid content id
	 */
	public void removeEmbed(String cid) {
		m_embeds.remove(cid);
		m_toString = null;
	}
	
	/**
	 * Method to check if this object contains embedded MIME resources
	 * 
	 * @return true if we have embeds
	 */
	public boolean hasEmbeds() {
		return !m_embeds.isEmpty();
	}
	
	/**
	 * Returns the content ids for all inline files.
	 * 
	 * @return content ids
	 */
	public Iterable<String> getContentIds() {
		return m_embeds.keySet();
	}

	/**
	 * Attaches a file. We provide several implementations for
	 * {@link IMimeAttachment}, e.g. {@link LocalFileMimeAttachment},
	 * {@link ByteArrayMimeAttachment} or {@link UrlMimeAttachment} or
	 * you can add your own implementation. When reading {@link MIMEData}
	 * from a document, we transform the attachment to an internal class
	 * that returns the same filename, content type and binary content.
	 * 
	 * @param attachment attachment
	 */
	public void attach(IMimeAttachment attachment) {
		m_attachments.add(attachment);
		m_toString = null;
	}
	
	public List<IMimeAttachment> getAttachments() {
		return Collections.unmodifiableList(m_attachments);
	}

	public boolean hasAttachments() {
		return !m_attachments.isEmpty();
	}
	
	/**
	 * Removes an attachment from the MIME data
	 * 
	 * @param attachment attachment to remove
	 */
	public void removeAttachment(IMimeAttachment attachment) {
		m_attachments.remove(attachment);
		m_toString = null;
	}

	public String toString() {
		if (m_toString==null) {
			m_toString = "MimeData [contentTypes="+m_mailBodyByContentType.keySet().stream().collect(Collectors.toList())
					+ ", embeds="+m_embeds.keySet()
					+ ", attachments="+
					m_attachments.stream().map((att) -> {
						try {
							return att.getFileName();
						} catch (IOException e) {
							return "-error-";
						}
					}).collect(Collectors.toList())+"]";
		}
		return m_toString;
	}
}
