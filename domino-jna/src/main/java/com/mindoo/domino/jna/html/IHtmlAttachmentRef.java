package com.mindoo.domino.jna.html;

import java.util.Optional;

import com.mindoo.domino.jna.NotesAttachment;

/**
 * Wraps relevant information about links in HTML that point to attachments within the same note.
 * 
 * @author Karsten Lehmann
 */
public interface IHtmlAttachmentRef {

	/**
	 * Results the whole img tag URL (src attribute value)
	 * 
	 * @return relative image URL
	 */
	public String getReferenceText();

	/**
	 * Returns the filename of the referenced attachment in the same document
	 * 
	 * @return filename
	 */
	public String getFileName();
	
	/**
	 * Tries to find the attachment in the current note
	 * 
	 * @return attachment if found
	 */
	public Optional<NotesAttachment> findAttachment();
	
	/**
	 * Returns the conversion properties used for the
	 * richtext-html conversion
	 * 
	 * @return conversion properties
	 */
	public HtmlConvertProperties getProperties();
}
