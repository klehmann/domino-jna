package com.mindoo.domino.jna.html;

import java.util.List;

import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.mime.MIMEData;

/**
 * Container for a richtext-HTML conversion result, returning the HTML source code and information
 * about contained references to external targets (e.g. links / images)
 * 
 * @author Karsten Lehmann
 */
public interface IHtmlConversionResult {

	/**
	 * Returns the HTML code of the conversion result
	 * 
	 * @return html
	 */
	String getText();
	
	/**
	 * Method to access all external references located in the HTML code (e.g. links or img tags)
	 * 
	 * @return references
	 */
	List<IHtmlApiReference> getReferences();
	
	/**
	 * Convenience method that calls {@link #getReferences()} and extracts the relevant
	 * data for all embedded img tags
	 * 
	 * @return embedded images
	 */
	List<IHtmlImageRef> getImages();

	/**
	 * Convenience method that calls {@link #getReferences()} and extracts the relevant
	 * data for all embedded attachment links
	 * 
	 * @return attachment links
	 */
	List<IHtmlAttachmentRef> getAttachments();
	
	/**
	 * Converts the HTML conversion result to a {@link MIMEData} object that can be
	 * written to a new item of type {@link NotesItem#TYPE_MIME_PART} via
	 * {@link NotesNote#replaceItemValue(String, Object)}.
	 * 
	 * @return MIME
	 */
	MIMEData toMIME();
	
}
