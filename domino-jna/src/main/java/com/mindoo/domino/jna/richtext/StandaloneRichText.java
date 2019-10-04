package com.mindoo.domino.jna.richtext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.NotesAttachment;
import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesNote.IItemCallback;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.CDFileRichTextNavigator;
import com.mindoo.domino.jna.internal.CompoundTextWriter;
import com.mindoo.domino.jna.internal.CompoundTextWriter.CloseResult;
import com.mindoo.domino.jna.internal.CompoundTextWriter.CloseResultType;
import com.mindoo.domino.jna.internal.CompoundTextWriter.CompoundTextStandaloneBuffer;
import com.mindoo.domino.jna.internal.CompoundTextWriter.CompoundTextStandaloneBuffer.FileInfo;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

/**
 * Helper class to construct richtext content without a {@link NotesNote} as container.
 * The main purpose of this class is to have a temporary buffer to compose one item
 * out of multiple others and transfer the result to a {@link NotesNote} at the end via
 * {@link #closeAndCopyToNote(NotesNote, String)}.
 * 
 * @author Karsten Lehmann
 */
public class StandaloneRichText implements IRecyclableNotesObject, ICompoundText, IAdaptable {
	private CompoundTextWriter m_compoundText;
	
	/**
	 * Creates a new standalone richtext item
	 */
	public StandaloneRichText() {
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference rethCompound = new LongByReference();
			result = NotesNativeAPI64.get().CompoundTextCreate((long)0, null, rethCompound);
			NotesErrorUtils.checkResult(result);
			long hCompound = rethCompound.getValue();
			CompoundTextWriter ct = new CompoundTextWriter(hCompound, true);
			NotesGC.__objectCreated(CompoundTextWriter.class, ct);
			m_compoundText = ct;
		}
		else {
			IntByReference rethCompound = new IntByReference();
			result = NotesNativeAPI32.get().CompoundTextCreate((int)0, null, rethCompound);
			NotesErrorUtils.checkResult(result);
			int hCompound = rethCompound.getValue();
			CompoundTextWriter ct = new CompoundTextWriter(hCompound, true);
			NotesGC.__objectCreated(CompoundTextWriter.class, ct);
			m_compoundText = ct;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz==CompoundTextWriter.class) {
			return (T) m_compoundText;
		}
		else
			return null;
	}
	
	@Override
	public void addDocLink(NotesNote note, String comment) {
		m_compoundText.addDocLink(note, comment);
	}

	@Override
	public void addDocLink(String dbReplicaId, String viewUnid, String noteUNID, String comment) {
		m_compoundText.addDocLink(dbReplicaId, viewUnid, noteUNID, comment);
	}

	@Override
	public void addRenderedNote(NotesNote note) {
		m_compoundText.addRenderedNote(note);
	}

	@Override
	public void addRenderedNote(NotesNote note, String form) {
		m_compoundText.addRenderedNote(note, form);
	}

	@Override
	public void addText(String txt) {
		m_compoundText.addText(txt);
	}

	@Override
	public void addText(String txt, TextStyle textStyle, FontStyle fontStyle) {
		m_compoundText.addText(txt, textStyle, fontStyle);
	}

	@Override
	public void addText(String txt, TextStyle textStyle, FontStyle fontStyle, boolean createParagraphOnLinebreak) {
		m_compoundText.addText(txt, textStyle, fontStyle, createParagraphOnLinebreak);
	}

	@Override
	public void addRichTextItem(NotesNote otherNote, String itemName) {
		m_compoundText.addRichTextItem(otherNote, itemName);
	}

	@Override
	public void addImage(File f) throws IOException {
		m_compoundText.addImage(f);
	}

	@Override
	public void addImage(int resizeToWidth, int resizeToHeight, File f) throws IOException {
		m_compoundText.addImage(resizeToWidth, resizeToHeight, f);
	}

	@Override
	public void addImage(int fileSize, InputStream imageData) throws IOException {
		m_compoundText.addImage(fileSize, imageData);
	}

	@Override
	public void addImage(int resizeToWidth, int resizeToHeight, int fileSize, InputStream imageData)
			throws IOException {
		m_compoundText.addImage(resizeToWidth, resizeToHeight, fileSize, imageData);
	}

	@Override
	public void addFileHotspot(NotesAttachment attachment, String filenameToDisplay) {
		m_compoundText.addFileHotspot(attachment, filenameToDisplay);
	}
	
	@Override
	public void addFileHotspot(NotesAttachment attachment, String filenameToDisplay, String captionText, File image)
			throws IOException {
		m_compoundText.addFileHotspot(attachment, filenameToDisplay, captionText, image);
	}

	@Override
	public void addFileHotspot(NotesAttachment attachment, String filenameToDisplay, String captionText,
			FontStyle captionStyle, CaptionPosition captionPos, int captionColorRed, int captionColorGreen,
			int captionColorBlue, int resizeToWidth, int resizeToHeight, int fileSize, InputStream imageData)
			throws IOException {
		m_compoundText.addFileHotspot(attachment, filenameToDisplay, captionText, captionStyle, captionPos, captionColorRed, captionColorGreen, captionColorBlue, resizeToWidth, resizeToHeight, fileSize, imageData);
	}

	/**
	 * Closes the specified {@link StandaloneRichText} and appends its content, renumbering
	 * paragraph ids.
	 * 
	 * @param rt standalone richtext
	 */
	@Override
	public void addClosedStandaloneRichText(StandaloneRichText rt) {
		m_compoundText.addClosedStandaloneRichText(rt);
	}
	
	@Override
	public void addCDRecords(Memory cdRecordMem) {
		m_compoundText.addCDRecords(cdRecordMem);
	}
	
	@Override
	public void recycle() {
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
	
	/**
	 * Closes this standalone richtext (so no more additions are allowed) and copies its content
	 * to a note. This method may be called multiple times if you need to copy the content
	 * to more than one note.
	 * 
	 * @param note target note
	 * @param richTextItemName name of richtext item in target note, existing richtext items with this name will be removed
	 */
	public void closeAndCopyToNote(final NotesNote note, String richTextItemName) {
		CloseResult result = m_compoundText.closeStandaloneContext();
		if (result.getType()!=CloseResultType.Buffer && result.getType()!=CloseResultType.File) {
			//should not happen
			throw new IllegalStateException("Unexpected close type received from compound text: "+result.getType());
		}
		
		//collect old composite items to prepare later deletion
		final LinkedList<NotesItem> items = new LinkedList<NotesItem>();
		
		note.getItems(richTextItemName, new IItemCallback() {

			@Override
			public void itemNotFound() {
			}

			@Override
			public Action itemFound(NotesItem item) {
				if (item.getType()==NotesItem.TYPE_COMPOSITE) {
					items.add(item);
				}
				return Action.Continue;
			}
		});
		
		//build new compound text
		RichTextBuilder rt = note.createRichTextItem(richTextItemName);
		CompoundTextWriter ctWriter = rt.getAdapter(CompoundTextWriter.class);
		if (ctWriter==null) {
			throw new NotesError(0, "Could not get "+CompoundTextWriter.class.getSimpleName()+" instance");
		}
		
		//and transfer the CD records from this compound text
		if (result.getType()==CloseResultType.Buffer) {
			final CompoundTextStandaloneBuffer buffer = result.getBuffer();
			FileInfo fileInfo;
			try {
				fileInfo = buffer.asFileOnDisk();
			} catch (FileNotFoundException e1) {
				throw new NotesError(0, "Could not extract compound text buffer to disk: "+buffer, e1);
			}
			ctWriter.addCompoundTextFromFile(fileInfo.getFilePath());
		}
		else {
			//Domino created a temp file
			String filePath = result.getFilePath();
			ctWriter.addCompoundTextFromFile(filePath);
		}
		rt.close();
		
		//cleanup obsolete items read earlier
		for (NotesItem currOldItem : items) {
			currOldItem.remove();
		}
	}
	
	public IRichTextNavigator closeAndGetRichTextNavigator() {
		CloseResult result = m_compoundText.closeStandaloneContext();
		FileInputStream fIn;
		String filePath;
		long fileSize;
		
		if (result.getType()==CloseResultType.Buffer) {
			final CompoundTextStandaloneBuffer buffer = result.getBuffer();
			try {
				FileInfo fileInfo = buffer.asFileOnDisk();
				
				fIn = fileInfo.getStream();
				filePath = fileInfo.getFilePath();
				fileSize = fileInfo.getFileSize();
			} catch (FileNotFoundException e) {
				throw new NotesError(0, "Error opening file stream for standalone compound text", e);
			}
		}
		else {
			//Domino created a temp file
			filePath = result.getFilePath();
			File tmpFile = new File(filePath);
			try {
				fileSize = tmpFile.length();
				fIn = new FileInputStream(tmpFile);
			} catch (FileNotFoundException e) {
				throw new NotesError(0, "Error opening file stream for standalone compound text", e);
			}
		}
		try {
			return new CDFileRichTextNavigator(fIn, filePath, fileSize);
		} catch (IOException e) {
			throw new NotesError(0, "Error creating richtext navigator for file stream", e);
		}
	}
}
