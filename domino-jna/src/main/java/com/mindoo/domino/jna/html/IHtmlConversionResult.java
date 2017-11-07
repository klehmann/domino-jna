package com.mindoo.domino.jna.html;

import java.util.List;

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
	public String getText();
	
	/**
	 * Method to access all external references located in the HTML code (e.g. links or img tags)
	 * 
	 * @return references
	 */
	public List<IHtmlApiReference> getReferences();
	
	/**
	 * Convenience method that calls {@link #getReferences()} and extracts the relevant
	 * data for all embedded img tags
	 * 
	 * @return embedded images
	 */
	public List<IHtmlImageRef> getImages();

}
