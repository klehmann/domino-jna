package com.mindoo.domino.jna.richtext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.NotesAttachment;
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.sun.jna.Memory;

/**
 * Interface for CompoundText, a high level C API to produce richtext
 * 
 * @author Karsten Lehmann
 */
public interface ICompoundText<T extends ICompoundText<?>> extends IAdaptable {

	/**
	 * This function inserts a DocLink for the specified {@link NotesDatabase}.
	 * 
	 * @param db database to create the link
	 * @param comment This string appears when the DocLink is selected (clicked on).
	 */
	public T addDatabaseLink(NotesDatabase db, String comment);

	/**
	 * This function inserts a DocLink for the specified {@link NotesCollection}.
	 * 
	 * @param collection collection to create the link
	 * @param comment This string appears when the DocLink is selected (clicked on).
	 */
	public T addCollectionLink(NotesCollection collection, String comment);

	/**
	 * This function inserts a DocLink for the specified {@link NotesNote}.
	 * 
	 * @param note note to create the link
	 * @param comment This string appears when the DocLink is selected (clicked on).
	 */
	public T addDocLink(NotesNote note, String comment);
	
	/**
	 * This function inserts a DocLink using manual values.
	 * 
	 * @param dbReplicaId Replica ID of the database that contains the note (document) pointed to by the DocLink.
	 * @param viewUnid UNID of the view that contains the note (document) pointed to by the DocLink; empty/null to create a database link
	 * @param noteUNID UNID of the note (document) pointed to by the DocLink; empty/null to create a collection link
	 * @param comment This string appears when the DocLink is selected (clicked on).
	 */
	public T addDocLink(String dbReplicaId, String viewUnid, String noteUNID, String comment);
	
	/**
	 * The function will render the specified note in Composite Data format (i.e., richtext) and add the rendered
	 * note to the data created by this builder.<br>
	 * This allows an editable copy of an entire note to be embedded in another note.
	 * 
	 * @param note note to render
	 */
	public T addRenderedNote(NotesNote note);
	
	/**
	 * The function will render the specified note in Composite Data format (i.e., richtext) and add the rendered
	 * note to the data created by this builder.<br>
	 * This allows an editable copy of an entire note to be embedded in another note.
	 * 
	 * @param note note to render
	 * @param form name of form used to render the note, null for default form
	 */
	public T addRenderedNote(NotesNote note, String form);
	
	/**
	 * Adds text with default text and font style
	 * 
	 * @param txt text to add
	 */
	public T addText(String txt);
	
	/**
	 * Adds text with the specified text and font style. Creates a paragraph for each linebreak found in the text
	 * 
	 * @param txt text to add
	 * @param textStyle text style
	 * @param fontStyle font style
	 */
	public T addText(String txt, TextStyle textStyle, FontStyle fontStyle);
	
	/**
	 * Adds text with the specified text and font style
	 * 
	 * @param txt text to add
	 * @param textStyle text style
	 * @param fontStyle font style
	 * @param createParagraphForLinebreak true to create a paragraph for each linebreak found in the text
	 */
	public T addText(String txt, TextStyle textStyle, FontStyle fontStyle, boolean createParagraphForLinebreak);
	
	/**
	 * This routine assimilates the contents of one or more named richtext items (type TYPE_COMPOSITE) from a document
	 * into the item to be created by this class.<br>
	 * <br>
	 * The contents are assimilated in that PABIDs and styles are fixed up (renumbered/renamed as needed)
	 * before they are appended to the CompoundText context.<br>
	 * <br>
	 * Note: This function does not handle features of rich text that depend on special items that reside outside
	 * the rich text field specified by the "itemName" parameter.<br>
	 * Two such features of rich text include doc links that depend on the special $Links item, and font faces
	 * that depend on the special $Fonts item. These special items in the note reside outside the
	 * specified rich text field.<br>
	 * Therefore, when this method merges a rich text field containing doc links consisting of CDLINK2 records
	 * into a Compound Text context, the doc link in the resulting compound text does not function.
	 * A work-around to this problem is demonstrated by sample program HISTORY in the RICHTEXT directory.<br>
	 * <br>
	 * Also, when this method merges a rich text field containing font faces that require a font
	 * table (a $Fonts item in the note)  the resulting compound text does not reflect the font face
	 * used in the original field.
	 * 
	 * @param otherNote note containing the item to append
	 * @param itemName name of item to append
	 */
	public T addRichTextItem(NotesNote otherNote, String itemName);
	
	/**
	 * Adds an image to the richtext item
	 * 
	 * @param f image file
	 * @throws IOException if data cannot be read
	 */
	public T addImage(File f) throws IOException;
	
	/**
	 * Adds an image to the richtext item. We support GIF, JPEG and BMP files.
	 * 
	 * @param resizeToWidth if not -1, resize the image to this width
	 * @param resizeToHeight if not -1, resize the image to this width
	 * @param f image file
	 * @throws IOException if data cannot be read
	 */
	public T addImage(int resizeToWidth, int resizeToHeight, File f) throws IOException;
	
	/**
	 * Adds an image to the richtext item
	 * 
	 * @param fileSize total size of image data
	 * @param imageData image data as bytestream
	 * @throws IOException if data cannot be read
	 */
	public T addImage(int fileSize, InputStream imageData) throws IOException;
	
	/**
	 * Adds an image to the richtext item
	 * 
	 * @param resizeToWidth if not -1, resize the image to this width
	 * @param resizeToHeight if not -1, resize the image to this width
	 * @param fileSize total size of image data
	 * @param imageData image data as bytestream
	 * @throws IOException if data cannot be read
	 */
	public T addImage(int resizeToWidth, int resizeToHeight, int fileSize, InputStream imageData) throws IOException;
	
	/**
	 * Adds a file hotspot to the richtext item containing a default file icon
	 * @param attachment attachment to open when the hotspot is clicked
	 * @param filenameToDisplay filename to display in the hotspot properties
	 */
	public T addFileHotspot(NotesAttachment attachment, String filenameToDisplay);
	
	/**
	 * Adds a file hotspot to the richtext item with a custom icon
	 * 
	 * @param attachment attachment to open when the hotspot is clicked
	 * @param filenameToDisplay filename to display in the hotspot properties
	 * @param captionText caption text to display below the hotspot
	 * @param image image file on disk to use for the file hotspot
	 * @throws IOException in case of I/O errors
	 */
	public T addFileHotspot(NotesAttachment attachment, String filenameToDisplay, String captionText, File image) throws IOException;
	
	/**
	 * Adds a file hotspot to the richtext item with a custom icon
	 * 
	 * @param attachment attachment to open when the hotspot is clicked
	 * @param filenameToDisplay filename to display in the hotspot properties
	 * @param captionText caption text to display below the hotspot
	 * @param captionStyle font style used for the caption
	 * @param captionPos caption position
	 * @param captionColorRed red-part of the caption text color
	 * @param captionColorGreen green-part of the caption text color
	 * @param captionColorBlue blue-part of the caption text color
	 * @param resizeToWidth optional resize width to be used if image should be resized, or -1 to not resize
	 * @param resizeToHeight optional resize height to be used if image should be resized, or -1 to not resize
	 * @param fileSize length of the data to be read from <code>imageData</code> stream
	 * @param imageData stream with image data
	 * @throws IOException in case of I/O errors
	 */
	public T addFileHotspot(NotesAttachment attachment, String filenameToDisplay, String captionText, FontStyle captionStyle,
			CaptionPosition captionPos, int captionColorRed, int captionColorGreen, int captionColorBlue,
			int resizeToWidth, int resizeToHeight, int fileSize, InputStream imageData) throws IOException;

	/**
	 * Adds a file hotspot to the richtext item containing a default file icon
	 * 
	 * @param attachmentProgrammaticName name returned by {@link NotesAttachment#getFileName()}
	 * @param filenameToDisplay filename to display in the hotspot properties
	 */
	public T addFileHotspot(String attachmentProgrammaticName, String filenameToDisplay);
	
	/**
	 * Adds a file hotspot to the richtext item with a custom icon
	 * 
	 * @param attachmentProgrammaticName name returned by {@link NotesAttachment#getFileName()}
	 * @param filenameToDisplay filename to display in the hotspot properties
	 * @param captionText caption text to display below the hotspot
	 * @param image image file on disk to use for the file hotspot
	 * @throws IOException in case of I/O errors
	 */
	public T addFileHotspot(String attachmentProgrammaticName, String filenameToDisplay, String captionText, File image) throws IOException;
	
	/**
	 * Adds a file hotspot to the richtext item with a custom icon
	 * 
	 * @param attachmentProgrammaticName name returned by {@link NotesAttachment#getFileName()}
	 * @param filenameToDisplay filename to display in the hotspot properties
	 * @param captionText caption text to display below the hotspot
	 * @param captionStyle font style used for the caption
	 * @param captionPos caption position
	 * @param captionColorRed red-part of the caption text color
	 * @param captionColorGreen green-part of the caption text color
	 * @param captionColorBlue blue-part of the caption text color
	 * @param resizeToWidth optional resize width to be used if image should be resized, or -1 to not resize
	 * @param resizeToHeight optional resize height to be used if image should be resized, or -1 to not resize
	 * @param fileSize length of the data to be read from <code>imageData</code> stream
	 * @param imageData stream with image data
	 * @throws IOException in case of I/O errors
	 */
	public T addFileHotspot(String attachmentProgrammaticName, String filenameToDisplay, String captionText, FontStyle captionStyle,
			CaptionPosition captionPos, int captionColorRed, int captionColorGreen, int captionColorBlue,
			int resizeToWidth, int resizeToHeight, int fileSize, InputStream imageData) throws IOException;

	/**
	 * Closes the specified {@link StandaloneRichText} and appends its content, merging and renumbering
	 * paragraph definitions.
	 * 
	 * @param rt standalone richtext
	 */
	public T addClosedStandaloneRichText(StandaloneRichText rt);
	
	/**
	 * Method to add raw CD record data to the compound text.<br>
	 * <br>
	 * <b>Please note:<br>
	 * Only use this method if you really know what you are doing. You can severe
	 * mess up your richtext data by adding the wrong record content. We just added
	 * this method here because we needed a way to copy modified richtext data
	 * between documents (e.g. changing record type from TABLEBEGIN to NESTEDTABLEBEGIN while
	 * keeping the rest of the record data).</b>
	 * 
	 * @param cdRecordMem CD record data including BSIG/WSIG/LSIG prefix
	 * @param cdRecordMem CD record data to add including BSIG/WSIG/LSIG prefix (one or more records, calls the C API method CompoundTextAddCDRecords internally)
	 */
	public T addCDRecords(Memory cdRecordMem);

	public boolean isRecycled();
	
}
