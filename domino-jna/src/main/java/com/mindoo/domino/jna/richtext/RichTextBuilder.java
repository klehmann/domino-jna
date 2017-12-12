package com.mindoo.domino.jna.richtext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.NotesAttachment;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.internal.CompoundTextWriter;

/**
 * This class can be used to create basic richtext content. It uses C API methods to create
 * "CompoundText", which provide some high-level methods for richtext creation, e.g.
 * to add text, doclinks, render notes as richtext or append other richtext items.<br>
 * <br>
 * Use {@link NotesNote#createRichTextItem(String)} to start composing a new richtext
 * item and get a {@link RichTextBuilder}.<br>
 * <b>After calling the available methods, you must
 * call {@link RichTextBuilder#close()} to write your changes into the note. Otherwise
 * it is discarded.</b>
 * 
 * @author Karsten Lehmann
 */
public class RichTextBuilder implements IRecyclableNotesObject, ICompoundText, IAdaptable {
	private NotesNote m_parentNote;
	private CompoundTextWriter m_compoundText;
	
	public RichTextBuilder(NotesNote parentNote, CompoundTextWriter compoundText) {
		m_parentNote = parentNote;
		m_compoundText = compoundText;
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz==NotesNote.class) {
			return (T) m_parentNote;
		}
		else if (clazz==CompoundTextWriter.class) {
			return (T) m_compoundText;
		}
		else
			return null;
	}
	
	void checkHandle() {
		if (m_parentNote.isRecycled())
			throw new NotesError(0, "Parent note already recycled");

		m_compoundText.checkHandle();
	}

	@Override
	public void addDocLink(NotesNote note, String comment) {
		checkHandle();
		m_compoundText.addDocLink(note, comment);
	}

	@Override
	public void addDocLink(String dbReplicaId, String viewUnid, String noteUNID, String comment) {
		checkHandle();
		m_compoundText.addDocLink(dbReplicaId, viewUnid, noteUNID, comment);
	}

	@Override
	public void addRenderedNote(NotesNote note) {
		checkHandle();
		m_compoundText.addRenderedNote(note);
	}

	@Override
	public void addRenderedNote(NotesNote note, String form) {
		checkHandle();
		m_compoundText.addRenderedNote(note, form);
	}

	@Override
	public void addText(String txt) {
		checkHandle();
		m_compoundText.addText(txt);
	}

	@Override
	public void addText(String txt, TextStyle textStyle, FontStyle fontStyle) {
		checkHandle();
		m_compoundText.addText(txt, textStyle, fontStyle);
	}

	@Override
	public void addText(String txt, TextStyle textStyle, FontStyle fontStyle, boolean createParagraphOnLinebreak) {
		checkHandle();
		m_compoundText.addText(txt, textStyle, fontStyle, createParagraphOnLinebreak);
	}
	
	@Override
	public void addRichTextItem(NotesNote otherNote, String itemName) {
		checkHandle();
		m_compoundText.addRichTextItem(otherNote, itemName);
	}
	
	@Override
	public void addImage(File f) throws IOException {
		checkHandle();
		m_compoundText.addImage(f);
	}
	
	@Override
	public void addImage(int resizeToWidth, int resizeToHeight, File f) throws IOException {
		checkHandle();
		m_compoundText.addImage(resizeToWidth, resizeToHeight, f);
	}
	
	@Override
	public void addImage(int fileSize, InputStream imageData) throws IOException {
		checkHandle();
		m_compoundText.addImage(fileSize, imageData);
	}
	
	@Override
	public void addImage(int resizeToWidth, int resizeToHeight, int fileSize, InputStream imageData) throws IOException {
		checkHandle();
		m_compoundText.addImage(resizeToWidth, resizeToHeight, fileSize, imageData);
	}
	
	@Override
	public void addFileHotspot(NotesAttachment attachment, String filenameToDisplay) {
		m_compoundText.addFileHotspot(attachment, filenameToDisplay);
	}
	
	@Override
	public void addFileHotspot(NotesAttachment attachment, String filenameToDisplay, String captionText, File image) throws IOException {
		checkHandle();
		m_compoundText.addFileHotspot(attachment, filenameToDisplay, captionText, image);
	}
	
	@Override
	public void addFileHotspot(NotesAttachment attachment, String filenameToDisplay, String captionText, FontStyle captionStyle,
			CaptionPosition captionPos, int captionColorRed, int captionColorGreen, int captionColorBlue,
			int resizeToWidth, int resizeToHeight, int fileSize, InputStream imageData) throws IOException {
		
		checkHandle();
		m_compoundText.addFileHotspot(attachment, filenameToDisplay, captionText, captionStyle, captionPos,
				captionColorRed, captionColorGreen, captionColorBlue, resizeToWidth, resizeToHeight, fileSize, imageData);
	}
	
	@Override
	public void addClosedStandaloneRichText(StandaloneRichText rt) {
		checkHandle();
		m_compoundText.addClosedStandaloneRichText(rt);
	}

	/**
	 * This routine closes the build process. Use {@link NotesNote#update()} 
	 * after {@link #close()} to update and save the document.
	 */
	public void close() {
		checkHandle();
		m_compoundText.closeItemContext();
	}

	@Override
	public void recycle() {
		if (isRecycled())
			return;

		m_compoundText.recycle();
	}

	@Override
	public boolean isRecycled() {
		return m_compoundText.isRecycled();
	}

	@Override
	public boolean isNoRecycle() {
		return m_compoundText.isNoRecycle();
	}

	@Override
	public int getHandle32() {
		return m_compoundText.getHandle32();
	}

	@Override
	public long getHandle64() {
		return m_compoundText.getHandle64();
	}

	public boolean hasData() {
		return m_compoundText.hasData();
	}

}
