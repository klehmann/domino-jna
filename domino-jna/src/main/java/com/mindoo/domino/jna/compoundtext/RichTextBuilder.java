package com.mindoo.domino.jna.compoundtext;

import java.util.HashMap;
import java.util.Map;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.structs.compoundtext.NotesCompoundStyleStruct;
import com.mindoo.domino.jna.structs.compoundtext.NotesFontIDFieldsStruct;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * This class can be used to create basic richtext content. It uses C API methods to create
 * "CompoundText", which provide some high-level methods for richtext creation, e.g.
 * to add text, doclinks, render notes as richtext or append other richtext items.<br>
 * <br>
 * <b>After calling the available methods in the returned {@link RichTextBuilder}, you must
 * call {@link RichTextBuilder#close()} to write your changes into the note. Otherwise
 * it is discarded.</b>
 * 
 * @author Karsten Lehmann
 */
public class RichTextBuilder implements IRecyclableNotesObject {
	private NotesNote m_parentNote;
	private long m_handle64;
	private int m_handle32;
	private Map<Integer,Integer> m_definedStyleId;

	public RichTextBuilder(NotesNote parentNote, long handle) {
		if (!NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_parentNote = parentNote;
		m_handle64 = handle;
		m_definedStyleId = new HashMap<Integer, Integer>();
	}

	public RichTextBuilder(NotesNote parentNote, int handle) {
		if (NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		m_parentNote = parentNote;
		m_handle32 = handle;
		m_definedStyleId = new HashMap<Integer, Integer>();
	}

	void checkHandle() {
		if (m_parentNote.isRecycled())
			throw new NotesError(0, "Parent note already recycled");

		if (NotesJNAContext.is64Bit()) {
			if (m_handle64==0)
				throw new NotesError(0, "Compound text already recycled");
			NotesGC.__b64_checkValidObjectHandle(RichTextBuilder.class, m_handle64);
		}
		else {
			if (m_handle32==0)
				throw new NotesError(0, "Compound text already recycled");
			NotesGC.__b32_checkValidObjectHandle(RichTextBuilder.class, m_handle32);
		}
	}

	private int getDefaultFontId() {
		return getFontId(NotesCAPI.FONT_FACE_SWISS, (byte) 0, (byte) 0, (byte) 10);
	}

	private int getFontId(byte face, byte attrib, byte color, byte pointSize) {
		NotesFontIDFieldsStruct fontIdStruct = NotesFontIDFieldsStruct.newInstance();
		fontIdStruct.Face = face;
		fontIdStruct.Attrib = attrib;
		fontIdStruct.Color = color;
		fontIdStruct.PointSize = pointSize;
		fontIdStruct.write();

		int fontId = fontIdStruct.getPointer().getInt(0);
		return fontId;
	}

	/**
	 * This function inserts a DocLink for the specified {@link NotesNote}.
	 * 
	 * @param note note to create the link
	 * @param comment This string appears when the DocLink is selected (clicked on).
	 */
	public void addDocLink(NotesNote note, String comment) {
		if (note.isRecycled())
			throw new NotesError(0, "Provided note is already recycled");

		NotesDatabase parentDb = note.getParent();
		String replicaId = parentDb.getReplicaID();
		String noteUnid = note.getUNID();
		NotesCollection defaultCollection = parentDb.openDefaultCollection();
		addDocLink(replicaId, defaultCollection.getUNID(), noteUnid, comment);
	}

	/**
	 * This function inserts a DocLink using manual values.
	 * 
	 * @param dbReplicaId Replica ID of the database that contains the note (document) pointed to by the DocLink.
	 * @param viewUnid UNID of the view that contains the note (document) pointed to by the DocLink.
	 * @param noteUNID UNID of the note (document) pointed to by the DocLink.
	 * @param comment This string appears when the DocLink is selected (clicked on).
	 */
	public void addDocLink(String dbReplicaId, String viewUnid, String noteUNID, String comment) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory commentMem = NotesStringUtils.toLMBCS(comment, true);
		short result;
		NotesUniversalNoteIdStruct viewUNIDStruct = NotesUniversalNoteIdStruct.fromString(viewUnid);
		NotesUniversalNoteIdStruct noteUNIDStruct = NotesUniversalNoteIdStruct.fromString(noteUNID);

		NotesUniversalNoteIdStruct.ByValue viewUNIDStructByVal = NotesUniversalNoteIdStruct.ByValue.newInstance();
		viewUNIDStructByVal.File = viewUNIDStruct.File;
		viewUNIDStructByVal.Note = viewUNIDStruct.Note;

		NotesUniversalNoteIdStruct.ByValue noteUNIDStructByVal = NotesUniversalNoteIdStruct.ByValue.newInstance();
		noteUNIDStructByVal.File = noteUNIDStruct.File;
		noteUNIDStructByVal.Note = noteUNIDStruct.Note;

		int[] dbReplicaIdInnards = NotesStringUtils.replicaIdToInnards(dbReplicaId);
		NotesTimeDateStruct.ByValue dbReplicaIdStructByVal = NotesTimeDateStruct.ByValue.newInstance();
		dbReplicaIdStructByVal.Innards[0] = dbReplicaIdInnards[0];
		dbReplicaIdStructByVal.Innards[1] = dbReplicaIdInnards[1];

		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_CompoundTextAddDocLink(m_handle64, dbReplicaIdStructByVal, viewUNIDStructByVal, noteUNIDStructByVal, commentMem, 0);
		}
		else {
			result = notesAPI.b32_CompoundTextAddDocLink(m_handle32, dbReplicaIdStructByVal, viewUNIDStructByVal, noteUNIDStructByVal, commentMem, 0);
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * The function will render the specified note in Composite Data format (i.e., richtext) and add the rendered
	 * note to the data created by this builder.<br>
	 * This allows an editable copy of an entire note to be embedded in another note.
	 * 
	 * @param note note to render
	 */
	public void addRenderedNote(NotesNote note) {
		String formName = note.getItemValueString("Form");

		addRenderedNote(note, formName);
	}

	/**
	 * The function will render the specified note in Composite Data format (i.e., richtext) and add the rendered
	 * note to the data created by this builder.<br>
	 * This allows an editable copy of an entire note to be embedded in another note.
	 * 
	 * @param note note to render
	 * @param form name of form used to render the note, null for default form
	 */
	public void addRenderedNote(NotesNote note, String form) {
		if (note.isRecycled())
			throw new NotesError(0, "Provided note is already recycled");

		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		NotesDatabase db = note.getParent();
		NotesNote formNote = StringUtil.isEmpty(form) ? null : db.findDesignNote(form, NoteClass.FORM);

		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_CompoundTextAddRenderedNote(m_handle64, note.getHandle64(), formNote==null ? 0 : formNote.getHandle64(), 0);
		}
		else {
			result = notesAPI.b64_CompoundTextAddRenderedNote(m_handle32, note.getHandle32(), formNote==null ? 0 : formNote.getHandle32(), 0);
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * Adds text with default text and font style
	 * 
	 * @param txt text to add
	 */
	public void addText(String txt) {
		addText(txt, (TextStyle) null, (FontStyle) null);
	}

	/**
	 * Converts the {@link TextStyle} to a style id, reusing already defined
	 * styles if all attributes are matching
	 * 
	 * @param style text style
	 * @return style id
	 */
	private int getStyleId(TextStyle style) {
		int styleHash = style.hashCode();
		Integer styleId = m_definedStyleId.get(styleHash);
		if (styleId==null) {
			checkHandle();
			NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
			IntByReference retStyleId = new IntByReference();

			NotesCompoundStyleStruct styleStruct = NotesCompoundStyleStruct.newInstance();
			notesAPI.CompoundTextInitStyle(styleStruct);
			styleStruct.read();
			styleStruct.JustifyMode = style.m_justifyMode;
			styleStruct.LineSpacing = style.m_lineSpacing;
			styleStruct.ParagraphSpacingBefore = style.m_paragraphSpacingBefore;
			styleStruct.ParagraphSpacingAfter = style.m_paragraphSpacingAfter;
			styleStruct.LeftMargin = style.m_leftMargin;
			styleStruct.RightMargin = style.m_rightMargin;
			styleStruct.FirstLineLeftMargin = style.m_firstLineLeftMargin;
			styleStruct.Tabs = style.m_tabs;
			styleStruct.Tab = style.m_tab==null ? null : style.m_tab.clone();
			styleStruct.Flags = style.m_flags;
			styleStruct.write();

			Memory styleNameMem = NotesStringUtils.toLMBCS(style.m_styleName, true);

			short result;
			if (NotesJNAContext.is64Bit()) {
				result = notesAPI.b64_CompoundTextDefineStyle(m_handle64, styleNameMem, styleStruct, retStyleId);
			}
			else {
				result = notesAPI.b32_CompoundTextDefineStyle(m_handle32, styleNameMem, styleStruct, retStyleId);

			}
			NotesErrorUtils.checkResult(result);
			styleId = retStyleId.getValue();

			m_definedStyleId.put(styleHash, styleId);
		}
		return styleId;
	}

	/**
	 * Adds text with the specified text and font style
	 * 
	 * @param txt text to add
	 * @param textStyle text style
	 * @param fontStyle font style
	 */
	public void addText(String txt, TextStyle textStyle, FontStyle fontStyle) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory txtMem = NotesStringUtils.toLMBCS(txt, false);
		Memory lineDelimMem = new Memory(3);
		lineDelimMem.setByte(0, (byte) '\r'); 
		lineDelimMem.setByte(1, (byte) '\n'); 
		lineDelimMem.setByte(2, (byte) 0);

		int fontId = fontStyle==null ? getDefaultFontId() : fontStyle.getFontId();
		int dwStyleID = textStyle==null ? NotesCAPI.STYLE_ID_SAMEASPREV : getStyleId(textStyle);

		Pointer nlsInfoPtr = notesAPI.OSGetLMBCSCLS();
		short result;
		int dwFlags = NotesCAPI.COMP_PRESERVE_LINES | NotesCAPI.COMP_PARA_LINE | NotesCAPI.COMP_PARA_BLANK_LINE;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_CompoundTextAddTextExt(m_handle64, dwStyleID, fontId, txtMem, (int) txtMem.size(),
					lineDelimMem, dwFlags, nlsInfoPtr);
		}
		else {
			result = notesAPI.b32_CompoundTextAddTextExt(m_handle32, dwStyleID, fontId, txtMem, (int) txtMem.size(),
					lineDelimMem, dwFlags, nlsInfoPtr);
		}
		NotesErrorUtils.checkResult(result);
	}

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
	public void addCompoundText(NotesNote otherNote, String itemName) {
		if (otherNote.isRecycled())
			throw new NotesError(0, "Provided note is already recycled");

		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);

		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_CompoundTextAssimilateItem(m_handle64, otherNote.getHandle64(), itemNameMem, 0);
		}
		else {
			result = notesAPI.b32_CompoundTextAssimilateItem(m_handle32, otherNote.getHandle32(), itemNameMem, 0);
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * This routine closes the build process. Use {@link NotesNote#update()} 
	 * after {@link #close()} to update and save the document.
	 */
	public void close() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		short result;
		if (NotesJNAContext.is64Bit()) {
			NotesGC.__objectBeeingBeRecycled(RichTextBuilder.class, this);
			result = notesAPI.b64_CompoundTextClose(m_handle64, null, null, null, (short) 0);
			m_handle64 = 0;
		}
		else {
			NotesGC.__objectBeeingBeRecycled(RichTextBuilder.class, this);
			result = notesAPI.b32_CompoundTextClose(m_handle32, null, null, null, (short) 0);
			m_handle32 = 0;
		}
		NotesErrorUtils.checkResult(result);
	}

	@Override
	public void recycle() {
		if (isRecycled())
			return;

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			if (m_handle64!=0) {
				notesAPI.b64_CompoundTextDiscard(m_handle64);
				NotesGC.__objectBeeingBeRecycled(RichTextBuilder.class, this);
				m_handle64=0;
			}
		}
		else {
			if (m_handle32!=0) {
				notesAPI.b32_CompoundTextDiscard(m_handle32);
				NotesGC.__objectBeeingBeRecycled(RichTextBuilder.class, this);
				m_handle32=0;
			}
		}
	}

	@Override
	public boolean isRecycled() {
		if (NotesJNAContext.is64Bit()) {
			return m_handle64==0;
		}
		else {
			return m_handle32==0;
		}
	}

	@Override
	public boolean isNoRecycle() {
		return false;
	}

	@Override
	public int getHandle32() {
		return m_handle32;
	}

	@Override
	public long getHandle64() {
		return m_handle64;
	}
}
