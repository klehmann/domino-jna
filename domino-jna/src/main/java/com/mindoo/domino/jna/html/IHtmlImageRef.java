package com.mindoo.domino.jna.html;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.mindoo.domino.jna.NotesNote.IHtmlItemImageConversionCallback;

/**
 * Convenience interface to access an image that is referenced from HTML as a result
 * of a richtext-html conversion
 * 
 * @author Karsten Lehmann
 */
public interface IHtmlImageRef {

	/**
	 * Results the whole img tag URL (src attribute value)
	 * 
	 * @return relative image URL
	 */
	public String getReferenceText();
	
	/**
	 * Returns the parsed item name that contains the image
	 * 
	 * @return item name
	 */
	public String getItemName();
	
	/**
	 * Returns the item index to extract the image data from the item
	 * 
	 * @return index
	 */
	public int getItemIndex();
	
	/**
	 * Returns the item offset to extract the image data from the item
	 * 
	 * @return offset
	 */
	public int getItemOffset();
	
	/**
	 * Returns the conversion properties used for the
	 * richtext-html conversion
	 * 
	 * @return conversion properties
	 */
	public HtmlConvertProperties getProperties();
	
	/**
	 * Method to directly access the image data
	 * 
	 * @param callback callback to receive data
	 */
	public void readImage(IHtmlItemImageConversionCallback callback);
	
	/**
	 * Convenience method to write the whole image to a file on disk
	 * 
	 * @param f file
	 * @throws IOException on case of I/O errors
	 */
	public void writeImage(File f) throws IOException;
	
	/**
	 * Convenience method to write the whole image to an output stream
	 * 
	 * @param out stream
	 * @throws IOException on case of I/O errors
	 */
	public void writeImage(OutputStream out) throws IOException;

	/**
	 * Returns the image format, either "gif" or "jpg"
	 * 
	 * @return format
	 */
	public String getFormat();
}
