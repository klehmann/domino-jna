package com.mindoo.domino.jna;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mindoo.domino.jna.NotesNote.IItemCallback.Action;
import com.mindoo.domino.jna.compoundtext.RichTextBuilder;
import com.mindoo.domino.jna.constants.CDRecord;
import com.mindoo.domino.jna.constants.Compression;
import com.mindoo.domino.jna.constants.ItemType;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.OpenNote;
import com.mindoo.domino.jna.constants.UpdateNote;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.errors.UnsupportedItemValueError;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.html.CommandId;
import com.mindoo.domino.jna.html.IHtmlApiReference;
import com.mindoo.domino.jna.html.IHtmlApiUrlTargetComponent;
import com.mindoo.domino.jna.html.IHtmlConversionResult;
import com.mindoo.domino.jna.html.IHtmlImageRef;
import com.mindoo.domino.jna.html.ReferenceType;
import com.mindoo.domino.jna.html.TargetType;
import com.mindoo.domino.jna.internal.CollationDecoder;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesCAPI.b32_CWFErrorProc;
import com.mindoo.domino.jna.internal.NotesCAPI.b64_CWFErrorProc;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.internal.ViewFormatDecoder;
import com.mindoo.domino.jna.internal.WinNotesCAPI;
import com.mindoo.domino.jna.structs.NoteIdStruct;
import com.mindoo.domino.jna.structs.NotesBlockIdStruct;
import com.mindoo.domino.jna.structs.NotesCDFieldStruct;
import com.mindoo.domino.jna.structs.NotesFileObjectStruct;
import com.mindoo.domino.jna.structs.NotesNumberPairStruct;
import com.mindoo.domino.jna.structs.NotesObjectDescriptorStruct;
import com.mindoo.domino.jna.structs.NotesOriginatorIdStruct;
import com.mindoo.domino.jna.structs.NotesRangeStruct;
import com.mindoo.domino.jna.structs.NotesTimeDatePairStruct;
import com.mindoo.domino.jna.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.structs.html.HTMLAPIReference32Struct;
import com.mindoo.domino.jna.structs.html.HTMLAPIReference64Struct;
import com.mindoo.domino.jna.structs.html.HtmlApi_UrlTargetComponentStruct;
import com.mindoo.domino.jna.utils.LegacyAPIUtils;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;

/**
 * Object wrapping a Notes document / note
 * 
 * @author Karsten Lehmann
 */
public class NotesNote implements IRecyclableNotesObject {
	private int m_hNote32;
	private long m_hNote64;
	private boolean m_noRecycle;
	private NotesDatabase m_parentDb;
	private Document m_legacyDocRef;
	private EnumSet<NoteClass> m_noteClass;
	
	/**
	 * Creates a new instance
	 * 
	 * @param parentDb parent database
	 * @param hNote handle
	 */
	NotesNote(NotesDatabase parentDb, int hNote) {
		if (NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		m_parentDb = parentDb;
		m_hNote32 = hNote;
	}

	/**
	 * Creates a new instance
	 * 
	 * @param parentDb parent database
	 * @param hNote handle
	 */
	NotesNote(NotesDatabase parentDb, long hNote) {
		if (!NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_parentDb = parentDb;
		m_hNote64 = hNote;
	}

	/**
	 * Creates a new NotesDatabase
	 * 
	 * @param adaptable adaptable providing enough information to create the database
	 */
	public NotesNote(IAdaptable adaptable) {
		Document legacyDoc = adaptable.getAdapter(Document.class);
		if (legacyDoc!=null) {
			if (isRecycled(legacyDoc))
				throw new NotesError(0, "Legacy database already recycled");
			
			long docHandle = LegacyAPIUtils.getDocHandle(legacyDoc);
			if (docHandle==0)
				throw new NotesError(0, "Could not read db handle");
			
			if (NotesJNAContext.is64Bit()) {
				m_hNote64 = docHandle;
			}
			else {
				m_hNote32 = (int) docHandle;
			}
			NotesGC.__objectCreated(NotesNote.class, this);
			setNoRecycle();
			m_legacyDocRef = legacyDoc;
			
			Database legacyDb;
			try {
				legacyDb = legacyDoc.getParentDatabase();
			} catch (NotesException e1) {
				throw new NotesError(0, "Could not read parent legacy db from document", e1);
			}
			long dbHandle = LegacyAPIUtils.getDBHandle(legacyDb);
			try {
				if (NotesJNAContext.is64Bit()) {
					m_parentDb = (NotesDatabase) NotesGC.__b64_checkValidObjectHandle(NotesDatabase.class, dbHandle);
				}
				else {
					m_parentDb = (NotesDatabase) NotesGC.__b32_checkValidObjectHandle(NotesDatabase.class, (int) dbHandle);
				}
			} catch (NotesError e) {
				m_parentDb = LegacyAPIUtils.toNotesDatabase(legacyDb);
			}
		}
		else {
			throw new NotesError(0, "Unsupported adaptable parameter");
		}
	}

	private boolean isRecycled(Document doc) {
		try {
			//call any method to check recycled state
			doc.hasItem("~-~-~-~-~-~");
		}
		catch (NotesException e) {
			if (e.id==4376 || e.id==4466)
				return true;
		}
		return false;
	}

	/**
	 * Converts a legacy {@link lotus.domino.Document} to a
	 * {@link NotesNote}.
	 * 
	 * @param parentDb parent database
	 * @param doc document to convert
	 * @return note
	 */
	public static NotesNote toNote(NotesDatabase parentDb, Document doc) {
		long handle = LegacyAPIUtils.getDocHandle(doc);
		NotesNote note;
		if (NotesJNAContext.is64Bit()) {
			note = new NotesNote(parentDb, handle);
		}
		else {
			note = new NotesNote(parentDb, (int) handle);
		}
		note.setNoRecycle();
		return note;
	}
	
	/**
	 * Converts this note to a legacy {@link Document}
	 * 
	 * @param db parent database
	 * @return document
	 */
	public Document toDocument(Database db) {
		if (NotesJNAContext.is64Bit()) {
			return LegacyAPIUtils.createDocument(db, m_hNote64);
		}
		else {
			return LegacyAPIUtils.createDocument(db, m_hNote32);
		}
	}
	
	/**
	 * Returns the parent database
	 * 
	 * @return database
	 */
	public NotesDatabase getParent() {
		return m_parentDb;
	}
	
	/**
	 * Returns the note id of the note
	 * 
	 * @return note id
	 */
	public int getNoteId() {
		checkHandle();
		
		Memory retNoteId = new Memory(4);
		retNoteId.clear();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			notesAPI.b64_NSFNoteGetInfo(m_hNote64, NotesCAPI._NOTE_ID, retNoteId);
		}
		else {
			notesAPI.b32_NSFNoteGetInfo(m_hNote32, NotesCAPI._NOTE_ID, retNoteId);
		}
		return retNoteId.getInt(0);
	}
	
	/**
	 * Returns the note class of the note
	 * 
	 * @return note class
	 */
	public EnumSet<NoteClass> getNoteClass() {
		if (m_noteClass==null) {
			checkHandle();
			
			Memory retNoteClass = new Memory(2);
			retNoteClass.clear();
			
			NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_NSFNoteGetInfo(m_hNote64, NotesCAPI._NOTE_CLASS, retNoteClass);
			}
			else {
				notesAPI.b32_NSFNoteGetInfo(m_hNote32, NotesCAPI._NOTE_CLASS, retNoteClass);
			}
			int noteClassMask = retNoteClass.getShort(0);
			m_noteClass = NoteClass.toNoteClasses(noteClassMask);
		}
		return m_noteClass;
	}
	
	/**
	 * Converts the value of {@link #getNoteId()} to a hex string in uppercase
	 * format
	 * 
	 * @return note id as hex string
	 */
	public String getNoteIdAsString() {
		return Integer.toString(getNoteId(), 16).toUpperCase();
	}
	
	@Override
	public String toString() {
		if (isRecycled()) {
			return "NotesNote [recycled]";
		}
		else {
			return "NotesNote [handle="+(NotesJNAContext.is64Bit() ? m_hNote64 : m_hNote32)+", noteid="+getNoteId()+"]";
		}
	}
	
	/**
	 * Returns the UNID of the note
	 * 
	 * @return UNID
	 */
	public String getUNID() {
		NotesOriginatorIdStruct oid = getOIDStruct();
		String unid = oid.getUNIDAsString();
		return unid;
	}

	/**
	 * Internal method to get the populated {@link NotesOriginatorIdStruct} object
	 * for this note
	 * 
	 * @return oid structure
	 */
	private NotesOriginatorIdStruct getOIDStruct() {
		checkHandle();
		
		Memory retOid = new Memory(NotesCAPI.oidSize);
		retOid.clear();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			notesAPI.b64_NSFNoteGetInfo(m_hNote64, NotesCAPI._NOTE_OID, retOid);
		}
		else {
			notesAPI.b32_NSFNoteGetInfo(m_hNote32, NotesCAPI._NOTE_OID, retOid);
		}
		NotesOriginatorIdStruct oidStruct = NotesOriginatorIdStruct.newInstance(retOid);
		oidStruct.read();
		
		return oidStruct;
	}
	
	/**
	 * Returns the {@link NotesOriginatorId} for this note
	 * 
	 * @return oid
	 */
	public NotesOriginatorId getOID() {
		NotesOriginatorIdStruct oidStruct = getOIDStruct();
		NotesOriginatorId oid = new NotesOriginatorId(oidStruct);
		return oid;
	}
	
	/**
	 * Sets a new UNID in the {@link NotesOriginatorId} for this note
	 * 
	 * @param newUnid new universal id
	 */
	public void setUNID(String newUnid) {
		checkHandle();
		
		Memory retOid = new Memory(NotesCAPI.oidSize);
		retOid.clear();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			notesAPI.b64_NSFNoteGetInfo(m_hNote64, NotesCAPI._NOTE_OID, retOid);
		}
		else {
			notesAPI.b32_NSFNoteGetInfo(m_hNote32, NotesCAPI._NOTE_OID, retOid);
		}
		NotesOriginatorIdStruct oidStruct = NotesOriginatorIdStruct.newInstance(retOid);
		oidStruct.read();
		oidStruct.setUNID(newUnid);
		if (NotesJNAContext.is64Bit()) {
			notesAPI.b64_NSFNoteSetInfo(m_hNote64, NotesCAPI._NOTE_OID, retOid);
		}
		else {
			notesAPI.b32_NSFNoteSetInfo(m_hNote32, NotesCAPI._NOTE_OID, retOid);
		}
	}
	
	/**
	 * Returns the last modified date of the note
	 * 
	 * @return last modified date
	 */
	public Calendar getLastModified() {
		checkHandle();
		
		Memory retModified = new Memory(NotesCAPI.timeDateSize);
		retModified.clear();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			notesAPI.b64_NSFNoteGetInfo(m_hNote64, NotesCAPI._NOTE_MODIFIED, retModified);
		}
		else {
			notesAPI.b32_NSFNoteGetInfo(m_hNote32, NotesCAPI._NOTE_MODIFIED, retModified);
		}
		NotesTimeDateStruct td = NotesTimeDateStruct.newInstance(retModified);
		td.read();
		Calendar cal = td.toCalendar();
		return cal;
	}

	/**
	 * Returns the creation date of this note
	 * 
	 * @return creation date
	 */
	public Calendar getCreationDate() {
		checkHandle();
		
		Calendar creationDate = getItemValueDateTime("$CREATED");
		if (creationDate!=null) {
			return creationDate;
		}
		
		NotesOriginatorIdStruct oidStruct = getOIDStruct();
		NotesTimeDateStruct creationDateStruct = oidStruct.Note;
		return creationDateStruct.toCalendar();
	}
	
	/**
	 * Returns the last access date of the note
	 * 
	 * @return last access date
	 */
	public Calendar getLastAccessed() {
		checkHandle();
		
		Memory retModified = new Memory(NotesCAPI.timeDateSize);
		retModified.clear();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			notesAPI.b64_NSFNoteGetInfo(m_hNote64, NotesCAPI._NOTE_ACCESSED, retModified);
		}
		else {
			notesAPI.b32_NSFNoteGetInfo(m_hNote32, NotesCAPI._NOTE_ACCESSED, retModified);
		}
		NotesTimeDateStruct td = NotesTimeDateStruct.newInstance(retModified);
		td.read();
		Calendar cal = td.toCalendar();
		return cal;
	}

	/**
	 * Returns the last access date of the note
	 * 
	 * @return last access date
	 */
	public Calendar getAddedToFileTime() {
		checkHandle();
		
		Memory retModified = new Memory(NotesCAPI.timeDateSize);
		retModified.clear();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			notesAPI.b64_NSFNoteGetInfo(m_hNote64, NotesCAPI._NOTE_ADDED_TO_FILE, retModified);
		}
		else {
			notesAPI.b32_NSFNoteGetInfo(m_hNote32, NotesCAPI._NOTE_ADDED_TO_FILE, retModified);
		}
		NotesTimeDateStruct td = NotesTimeDateStruct.newInstance(retModified);
		td.read();
		Calendar cal = td.toCalendar();
		return cal;
	}

	/**
	 * The NOTE_FLAG_READONLY bit indicates that the note is Read-Only for the current user.<br>
	 * If a note contains an author names field, and the name of the user opening the
	 * document is not included in the author names field list, then  the NOTE_FLAG_READONLY
	 * bit is set in the note header data when that user opens the note.
	 * 
	 * @return TRUE if document cannot be updated
	 */
	public boolean isReadOnly() {
		int flags = getFlags();
		return (flags & NotesCAPI.NOTE_FLAG_READONLY) == NotesCAPI.NOTE_FLAG_READONLY;
	}
	
	/**
	 * The NOTE_FLAG_ABSTRACTED bit indicates that the note has been abstracted (truncated).<br>
	 * This bit may be set if the database containing the note has replication settings set to
	 * "Truncate large documents and remove attachments".
	 * 
	 * @return true if truncated
	 */
	public boolean isTruncated() {
		int flags = getFlags();
		return (flags & NotesCAPI.NOTE_FLAG_ABSTRACTED) == NotesCAPI.NOTE_FLAG_ABSTRACTED;
	}
	
	/**
	 * Reads the note flags (e.g. {@link NotesCAPI#NOTE_FLAG_READONLY})
	 * 
	 * @return flags
	 */
	private short getFlags() {
		checkHandle();
		
		Memory retFlags = new Memory(2);
		retFlags.clear();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			notesAPI.b64_NSFNoteGetInfo(m_hNote64, NotesCAPI._NOTE_FLAGS, retFlags);
		}
		else {
			notesAPI.b32_NSFNoteGetInfo(m_hNote32, NotesCAPI._NOTE_FLAGS, retFlags);
		}
		short flags = retFlags.getShort(0);
		return flags;
	}
	
	public void setNoRecycle() {
		m_noRecycle=true;
	}
	
	@Override
	public boolean isNoRecycle() {
		return m_noRecycle;
	}
	
	@Override
	public void recycle() {
		if (m_noRecycle || isRecycled())
			return;

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFNoteClose(m_hNote64);
			NotesErrorUtils.checkResult(result);
			NotesGC.__objectBeeingBeRecycled(NotesNote.class, this);
			m_hNote64=0;
		}
		else {
			result = notesAPI.b32_NSFNoteClose(m_hNote32);
			NotesErrorUtils.checkResult(result);
			NotesGC.__objectBeeingBeRecycled(NotesNote.class, this);
			m_hNote32=0;
		}
	}

	@Override
	public boolean isRecycled() {
		if (NotesJNAContext.is64Bit()) {
			return m_hNote64==0;
		}
		else {
			return m_hNote32==0;
		}
	}

	@Override
	public int getHandle32() {
		return m_hNote32;
	}

	@Override
	public long getHandle64() {
		return m_hNote64;
	}

	void checkHandle() {
		if (m_legacyDocRef!=null && isRecycled(m_legacyDocRef))
			throw new NotesError(0, "Wrapped legacy document already recycled");
		
		if (NotesJNAContext.is64Bit()) {
			if (m_hNote64==0)
				throw new NotesError(0, "Note already recycled");
			NotesGC.__b64_checkValidObjectHandle(NotesNote.class, m_hNote64);
		}
		else {
			if (m_hNote32==0)
				throw new NotesError(0, "Note already recycled");
			NotesGC.__b32_checkValidObjectHandle(NotesNote.class, m_hNote32);
		}
	}

	/**
	 * Unsigns the note. This function removes the $Signature item from the note.
	 */
	public void unsign() {
		checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFNoteUnsign(m_hNote64);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFNoteUnsign(m_hNote32);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function writes the in-memory version of a note to its database.<br>
	 * <br>
	 * Prior to issuing this call, a new note (or changes to a note) are not a part
	 * of the on-disk database.<br>
	 * <br>
	 * This function allows using extended 32-bit DWORD update options, as described
	 * in the entry {@link UpdateNote}.<br>
	 * <br>
	 * You should also consider updating the collections associated with other Views
	 * in a database via the function {@link NotesCollection#update()},
	 * if you have added and/or deleted a substantial number of documents.<br>
	 * <br>
	 * If the Server's Indexer Task does not rebuild the collections associated with the database's Views,
	 * the first user to access a View in the modified database might experience an inordinant
	 * delay, while the collection is rebuilt by the Notes Workstation (locally) or
	 * Server Application (remotely).

	 * @param updateFlags flags
	 */
	public void update(EnumSet<UpdateNote> updateFlags) {
		checkHandle();
		
		int updateFlagsBitmask = UpdateNote.toBitMaskForUpdateExt(updateFlags);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFNoteUpdateExtended(m_hNote64, updateFlagsBitmask);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFNoteUpdateExtended(m_hNote32, updateFlagsBitmask);
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * This function writes the in-memory version of a note to its database.<br>
	 * Prior to issuing this call, a new note (or changes to a note) are not a part of
	 * the on-disk database.<br>
	 * <br>
	 * You should also consider updating the collections associated with other Views in a database
	 * via the function
	 * {@link NotesCollection#update()}, if you have added and/or deleted a substantial number of documents.<br>
	 * If the Server's Indexer Task does not rebuild the collections associated with
	 * the database's Views, the first user to access a View in the modified database
	 * might experience an inordinant delay, while the collection is rebuilt by the
	 * Notes Workstation (locally) or Server Application (remotely).<br>
	 * <br>
	 * Do not update notes to disk that contain invalid items.<br>
	 * An example of an invalid item is a view design note that has a $Collation item
	 * whose BufferSize member is set to zero.<br>
	 * This update method may return an error for an invalid item that was not caught
	 * in a previous release of Domino or Notes.<br>
	 * Note: if you have enabled IMAP on a database, in the case of the
	 * special NoteID "NOTEID_ADD_OR_REPLACE", a new note is always created.
	 * 
	 */
	public void update() {
		update(EnumSet.noneOf(UpdateNote.class));
	}

	/**
	 * The method checks whether an item exists
	 * 
	 * @param itemName item name
	 * @return true if the item exists
	 */
	public boolean hasItem(String itemName) {
		checkHandle();
		
		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, false);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFItemInfo(m_hNote64, itemNameMem, (short) (itemNameMem.size() & 0xffff), null, null, null, null);
		}
		else {
			result = notesAPI.b32_NSFItemInfo(m_hNote32, itemNameMem, (short) (itemNameMem.size() & 0xffff), null, null, null, null);
		}
		return result == 0;	
	}
	
	/**
	 * The function NSFNoteHasComposite returns TRUE if the given note contains any TYPE_COMPOSITE items.
	 * 
	 * @return true if composite
	 */
	public boolean hasComposite() {
		checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			return notesAPI.b64_NSFNoteHasComposite(m_hNote64) == 1;
		}
		else {
			return notesAPI.b32_NSFNoteHasComposite(m_hNote32) == 1;
		}
	}
	
	/**
	 * The function NSFNoteHasMIME returns TRUE if the given note contains either TYPE_RFC822_TEXT
	 * items or TYPE_MIME_PART items.
	 * 
	 * @return true if mime
	 */
	public boolean hasMIME() {
		checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			return notesAPI.b64_NSFNoteHasMIME(m_hNote64) == 1;
		}
		else {
			return notesAPI.b32_NSFNoteHasMIME(m_hNote32) == 1;
		}
	}
	
	/**
	 * The function NSFNoteHasMIMEPart returns TRUE if the given note contains any TYPE_MIME_PART items.
	 * 
	 * @return true if mime part
	 */
	public boolean hasMIMEPart() {
		checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			return notesAPI.b64_NSFNoteHasMIMEPart(m_hNote64) == 1;
		}
		else {
			return notesAPI.b32_NSFNoteHasMIMEPart(m_hNote32) == 1;
		}
	}
	
	/**
	 * This function deletes this note from the specified database with default flags (0).<br>
	 * <br>
	 * This function allows using extended 32-bit DWORD update options, as described in the entry {@link UpdateNote}.<br>
	 * <br>
	 * It deletes the specified note by updating it with a nil body, and marking the note as a deletion stub.<br>
	 * The deletion stub identifies the deleted note to other replica copies of the database.<br>
	 * This allows the replicator to delete copies of the note from replica databases.
	 * <br>
	 * The deleted note may be of any NOTE_CLASS_xxx.  The active user ID must have sufficient user access
	 * in the databases's Access Control List (ACL) to carry out a deletion on the note or the function
	 * will return an error code.
	 */
	public void delete() {
		delete(EnumSet.noneOf(UpdateNote.class));
	}
	
	/**
	 * This function deletes this note from the specified database.<br>
	 * <br>
	 * This function allows using extended 32-bit DWORD update options, as described in the entry {@link UpdateNote}.<br>
	 * <br>
	 * It deletes the specified note by updating it with a nil body, and marking the note as a deletion stub.<br>
	 * The deletion stub identifies the deleted note to other replica copies of the database.<br>
	 * This allows the replicator to delete copies of the note from replica databases.
	 * <br>
	 * The deleted note may be of any NOTE_CLASS_xxx.  The active user ID must have sufficient user access
	 * in the databases's Access Control List (ACL) to carry out a deletion on the note or the function
	 * will return an error code.
	 * 
	 * @param flags flags
	 */
	public void delete(EnumSet<UpdateNote> flags) {
		checkHandle();
		
		if (m_parentDb.isRecycled())
			throw new NotesError(0, "Parent database already recycled");
		
		int flagsAsInt = UpdateNote.toBitMaskForUpdateExt(flags);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFNoteDeleteExtended(m_parentDb.getHandle64(), getNoteId(), flagsAsInt);
		}
		else {
			result = notesAPI.b32_NSFNoteDeleteExtended(m_parentDb.getHandle32(), getNoteId(), flagsAsInt);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Method to remove an item from a note
	 * 
	 * @param itemName item name
	 */
	public void removeItem(String itemName) {
		checkHandle();
		
		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, false);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFItemDelete(m_hNote64, itemNameMem, (short) (itemNameMem.size() & 0xffff));
		}
		else {
			result = notesAPI.b32_NSFItemDelete(m_hNote32, itemNameMem, (short) (itemNameMem.size() & 0xffff));
		}
		if (result==INotesErrorConstants.ERR_ITEM_NOT_FOUND) {
			return;
		}
		NotesErrorUtils.checkResult(result);
	}
	
	//shared memory buffer for text item values
	private static Memory MAX_TEXT_ITEM_VALUE = new Memory(65535);
	static {
		MAX_TEXT_ITEM_VALUE.clear();
	}
	
	/**
	 * Use this function to read the value of a text item.<br>
	 * <br>
	 * If the item does not exist, the method returns an empty string. Use {@link #hasItem(String)}
	 * to check for item existence.
	 * 
	 * @param itemName item name
	 * @return text value
	 */
	public String getItemValueString(String itemName) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		
		synchronized (MAX_TEXT_ITEM_VALUE) {
			try {
				short length;
				if (NotesJNAContext.is64Bit()) {
					length = notesAPI.b64_NSFItemGetText(m_hNote64, itemNameMem, MAX_TEXT_ITEM_VALUE, (short) (MAX_TEXT_ITEM_VALUE.size() & 0xffff));
				}
				else {
					length = notesAPI.b32_NSFItemGetText(m_hNote32, itemNameMem, MAX_TEXT_ITEM_VALUE, (short) (MAX_TEXT_ITEM_VALUE.size() & 0xffff));
				}
				int lengthAsInt = (int) length & 0xffff;
				if (lengthAsInt==0) {
					return "";
				}
				String strVal = NotesStringUtils.fromLMBCS(MAX_TEXT_ITEM_VALUE, lengthAsInt);
				return strVal;
			}
			finally {
				MAX_TEXT_ITEM_VALUE.clear();
			}
		}
	}
	
	/**
	 * This function writes an item of type TEXT to the note.<br>
	 * If an item of that name already exists, it deletes the existing item first,
	 * then appends the new item.<br>
	 * <br>
	 * Note 1: Use \n as line break in your string. The method internally converts these line breaks
	 * to null characters ('\0'), because that's what the Notes API expects as line break delimiter.<br>
	 * <br>
	 * Note 2: If the Summary parameter of is set to TRUE, the ITEM_SUMMARY flag in the item will be set.
	 * Items with the ITEM_SUMMARY flag set are stored in the note's summary buffer. These items may
	 * be used in view columns,  selection formulas, and @-functions. The maximum size of the summary
	 * buffer is 32K per note.<br>
	 * If you append more than 32K bytes of data in items that have the ITEM_SUMMARY flag set,
	 * this method call will succeed, but {@link #update(EnumSet)} will return ERR_SUMMARY_TOO_BIG (ERR 561).
	 * To avoid this, decide which fields are not used in view columns, selection formulas,
	 * or @-functions. For these "non-computed" fields, use this method with the Summary parameter set to FALSE.<br>
	 * <br>
	 * API program may read, modify, and write items that do not have the summary flag set, but they
	 * must open the note first. Items that do not have the summary flag set can not be accessed
	 * in the summary buffer.<br>
	 * 
	 * @param itemName item name
	 * @param itemValue item value
	 * @param isSummary true to set summary flag
	 */
	public void setItemValueString(String itemName, String itemValue, boolean isSummary) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		Memory itemValueMem = NotesStringUtils.toLMBCS(itemValue, false);
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFItemSetTextSummary(m_hNote64, itemNameMem, itemValueMem, itemValueMem==null ? 0 : ((short) (itemValueMem.size() & 0xffff)), isSummary);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFItemSetTextSummary(m_hNote32, itemNameMem, itemValueMem, itemValueMem==null ? 0 : ((short) (itemValueMem.size() & 0xffff)), isSummary);
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * This function writes a number to an item in the note.
	 * 
	 * @param itemName item name
	 * @param value new value
	 */
	public void setItemValueDouble(String itemName, double value) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		
		Memory doubleMem = new Memory(Native.getNativeSize(Double.TYPE));
		doubleMem.setDouble(0, value);
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFItemSetNumber(m_hNote64, itemNameMem, doubleMem);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFItemSetNumber(m_hNote32, itemNameMem, doubleMem);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * Writes a {@link Calendar} value in an item
	 * 
	 * @param itemName item name
	 * @param cal new value
	 */
	public void setItemValueDateTime(String itemName, Calendar cal) {
		if (cal==null) {
			removeItem(itemName);
			return;
		}
		
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);

		NotesTimeDate timeDate = NotesDateTimeUtils.calendarToTimeDate(cal);
		NotesTimeDateStruct timeDateStruct = timeDate==null ? null : timeDate.getAdapter(NotesTimeDateStruct.class);
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFItemSetTime(m_hNote64, itemNameMem, timeDateStruct);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFItemSetTime(m_hNote32, itemNameMem, timeDateStruct);
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * Writes a {@link Date} value in an item
	 * 
	 * @param itemName item name
	 * @param dt new value
	 * @param hasDate true to save the date of the specified {@link Date} object
	 * @param hasTime true to save the time of the specified {@link Date} object
	 */
	public void setItemValueDateTime(String itemName, Date dt, boolean hasDate, boolean hasTime) {
		if (dt==null) {
			removeItem(itemName);
			return;
		}
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(dt);
		if (!hasDate) {
			NotesDateTimeUtils.setAnyDate(cal);
		}
		if (!hasTime) {
			NotesDateTimeUtils.setAnyTime(cal);
		}
		setItemValueDateTime(itemName, cal);
	}

	/**
	 * Reads the value of a text list item
	 * 
	 * @param itemName item name
	 * @return list of strings; empty if item does not exist
	 */
	public List<String> getItemValueStringList(String itemName) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		
		synchronized (MAX_TEXT_ITEM_VALUE) {
			try {
				short nrOfValues;
				if (NotesJNAContext.is64Bit()) {
					nrOfValues = notesAPI.b64_NSFItemGetTextListEntries(m_hNote64, itemNameMem);
					
				}
				else {
					nrOfValues = notesAPI.b32_NSFItemGetTextListEntries(m_hNote32, itemNameMem);
				}
				
				int nrOfValuesAsInt = (int) (nrOfValues & 0xffff);
				
				if (nrOfValuesAsInt==0) {
					return Collections.emptyList();
				}
				
				List<String> strList = new ArrayList<String>(nrOfValuesAsInt);
				
				short retBufferSize = (short) (MAX_TEXT_ITEM_VALUE.size() & 0xffff);
				
				for (int i=0; i<nrOfValuesAsInt; i++) {
					short length;
					if (NotesJNAContext.is64Bit()) {
						length = notesAPI.b64_NSFItemGetTextListEntry(m_hNote64, itemNameMem, (short) (i & 0xffff), MAX_TEXT_ITEM_VALUE, retBufferSize);
					}
					else {
						length = notesAPI.b32_NSFItemGetTextListEntry(m_hNote32, itemNameMem, (short) (i & 0xffff), MAX_TEXT_ITEM_VALUE, retBufferSize);
					}
					
					int lengthAsInt = (int) length & 0xffff;
					if (lengthAsInt==0) {
						strList.add("");
					}
					String strVal = NotesStringUtils.fromLMBCS(MAX_TEXT_ITEM_VALUE, lengthAsInt);
					strList.add(strVal);
				}
				
				return strList;
			}
			finally {
				MAX_TEXT_ITEM_VALUE.clear();
			}
		}

	}
	
	/**
	 * This is a very powerful function that converts many kinds of Domino fields (items) into
	 * text strings.<br>
	 * 
	 * If there is more than one item with the same name, this function will always return the first of these.
	 * This function, therefore, is not useful if you want to retrieve multiple instances of the same
	 * field name. For these situations, use NSFItemConvertValueToText.<br>
	 * <br>
	 * The item value may be any one of these supported Domino data types:<br>
	 * <ul>
	 * <li>TYPE_TEXT - Text is returned unmodified.</li>
	 * <li>TYPE_TEXT_LIST - A text list items will merged into a single text string, with the separator between them.</li>
	 * <li>TYPE_NUMBER - the FLOAT number will be converted to text.</li>
	 * <li>TYPE_NUMBER_RANGE -- the FLOAT numbers will be converted to text, with the separator between them.</li>
	 * <li>TYPE_TIME - the binary Time/Date will be converted to text.</li>
	 * <li>TYPE_TIME_RANGE -- the binary Time/Date values will be converted to text, with the separator between them.</li>
	 * <li>TYPE_COMPOSITE - The text portion of the rich text field will be returned.</li>
	 * <li>TYPE_USERID - The user name portion will be converted to text.</li>
	 * <li>TYPE_ERROR - the binary Error value will be converted to "ERROR: ".</li>
	 * <li>TYPE_UNAVAILABLE - the binary Unavailable value will be converted to "UNAVAILABLE: ".</li>
	 * </ul>
	 * 
	 * @param itemName item name
	 * @param multiValueDelimiter delimiter character for value lists; should be an ASCII character, since no encoding is done
	 * @return string value
	 */
	public String getItemValueAsText(String itemName, char multiValueDelimiter) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);

		synchronized (MAX_TEXT_ITEM_VALUE) {
			try {
				//API docs: The maximum buffer size allowed is 60K. 
				short retBufferSize = (short) (Math.min(60*1024, MAX_TEXT_ITEM_VALUE.size()) & 0xffff);

				short length;
				if (NotesJNAContext.is64Bit()) {
					length = notesAPI.b64_NSFItemConvertToText(m_hNote64, itemNameMem, MAX_TEXT_ITEM_VALUE, retBufferSize, multiValueDelimiter);
				}
				else {
					length = notesAPI.b32_NSFItemConvertToText(m_hNote32, itemNameMem, MAX_TEXT_ITEM_VALUE, retBufferSize, multiValueDelimiter);
				}
				int lengthAsInt = (int) length & 0xffff;
				if (lengthAsInt==0) {
					return "";
				}
				for (int i=0; i<lengthAsInt; i++) {
					//replace null bytes with newlines
					byte currByte = MAX_TEXT_ITEM_VALUE.getByte(i);
					if (currByte==0) {
						MAX_TEXT_ITEM_VALUE.setByte(i, (byte) '\n');
					}
				}
				String strVal = NotesStringUtils.fromLMBCS(MAX_TEXT_ITEM_VALUE, lengthAsInt);
				return strVal;
			}
			finally {
				MAX_TEXT_ITEM_VALUE.clear();
			}
		}

	}
	
	/**
	 * Use this function to read the value of a number item as double.<br>
	 * <br>
	 * If the item does not exist, the method returns 0. Use {@link #hasItem(String)}
	 * to check for item existence.
	 * 
	 * @param itemName item name
	 * @return double value
	 */
	public double getItemValueDouble(String itemName) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		
		DoubleByReference retNumber = new DoubleByReference();
		
		if (NotesJNAContext.is64Bit()) {
			boolean exists = notesAPI.b64_NSFItemGetNumber(m_hNote64, itemNameMem, retNumber);
			if (!exists) {
				return 0;
			}
		}
		else {
			boolean exists = notesAPI.b32_NSFItemGetNumber(m_hNote32, itemNameMem, retNumber);
			if (!exists) {
				return 0;
			}
		}
		
		return retNumber.getValue();
	}

	/**
	 * Use this function to read the value of a number item as long.<br>
	 * <br>
	 * If the item does not exist, the method returns 0. Use {@link #hasItem(String)}
	 * to check for item existence.
	 * 
	 * @param itemName item name
	 * @return long value
	 */
	public long getItemValueLong(String itemName) {
		List<?> values = getItemValue(itemName);
		if (values.size()==0)
			return 0;
		Object firstVal = values.get(0);
		if (firstVal instanceof Number) {
			return ((Number)firstVal).longValue();
		}
		return 0;
	}

	/**
	 * Use this function to read the value of a number item as integer.<br>
	 * <br>
	 * If the item does not exist, the method returns 0. Use {@link #hasItem(String)}
	 * to check for item existence.
	 * 
	 * @param itemName item name
	 * @return int value
	 */
	public int getItemValueInteger(String itemName) {
		List<?> values = getItemValue(itemName);
		if (values.size()==0)
			return 0;
		Object firstVal = values.get(0);
		if (firstVal instanceof Number) {
			return ((Number)firstVal).intValue();
		}
		return 0;
	}
	
	/**
	 * Use this function to read the value of a timedate item as {@link Calendar}.<br>
	 * <br>
	 * If the item does not exist, the method returns null.
	 * 
	 * @param itemName item name
	 * @return time date value or null
	 */
	public Calendar getItemValueDateTime(String itemName) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		
		NotesTimeDateStruct td_item_value = NotesTimeDateStruct.newInstance();
		
		if (NotesJNAContext.is64Bit()) {
			boolean exists = notesAPI.b64_NSFItemGetTime(m_hNote64, itemNameMem, td_item_value);
			if (!exists) {
				return null;
			}
		}
		else {
			boolean exists = notesAPI.b32_NSFItemGetTime(m_hNote32, itemNameMem, td_item_value);
			if (!exists) {
				return null;
			}
		}
		return td_item_value.toCalendar();
	}

	/**
	 * Decodes an item value
	 * 
	 * @param itemName item name (for logging purpose)
	 * @param valueBlockId value block id
	 * @param valueLength item value length plus 2 bytes for the data type WORD
	 * @return item value as list
	 */
	List<Object> getItemValue(String itemName, NotesBlockIdStruct itemBlockId, NotesBlockIdStruct valueBlockId, int valueLength) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		Pointer poolPtr;
		if (NotesJNAContext.is64Bit()) {
			poolPtr = notesAPI.b64_OSLockObject((long) valueBlockId.pool);
		}
		else {
			poolPtr = notesAPI.b32_OSLockObject(valueBlockId.pool);
		}
		
		int block = (valueBlockId.block & 0xffff);
		long poolPtrLong = Pointer.nativeValue(poolPtr) + block;
		Pointer valuePtr = new Pointer(poolPtrLong);
		
		try {
			List<Object> values = getItemValue(itemName, itemBlockId, valuePtr, valueLength);
			return values;
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject((long) valueBlockId.pool);
				
			}
			else {
				notesAPI.b32_OSUnlockObject(valueBlockId.pool);
			}
		}
	}
	
	/**
	 * Decodes an item value
	 * 
	 * @param notesAPI Notes API
	 * @param itemName item name (for logging purpose)
	 * @param NotesBlockIdStruct itemBlockId item block id
	 * @param valuePtr pointer to the item value
	 * @param valueLength item value length plus 2 bytes for the data type WORD
	 * @return item value as list
	 */
	List<Object> getItemValue(String itemName, NotesBlockIdStruct itemBlockId, Pointer valuePtr, int valueLength) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		short dataType = valuePtr.getShort(0);
		int dataTypeAsInt = (int) (dataType & 0xffff);
		
		boolean supportedType = false;
		if (dataTypeAsInt == NotesItem.TYPE_TEXT) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TEXT_LIST) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER_RANGE) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME_RANGE) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_OBJECT) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NOTEREF_LIST) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_COLLATION) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_VIEW_FORMAT) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_FORMULA) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_UNAVAILABLE) {
			supportedType = true;
		}
		
		if (!supportedType) {
			throw new UnsupportedItemValueError("Data type for value of item "+itemName+" is currently unsupported: "+dataTypeAsInt);
		}

		int checkDataType = valuePtr.getShort(0) & 0xffff;
		Pointer valueDataPtr = valuePtr.share(2);
		int valueDataLength = valueLength - 2;
		
		if (checkDataType!=dataTypeAsInt) {
			throw new IllegalStateException("Value data type does not meet expected date type: found "+checkDataType+", expected "+dataTypeAsInt);
		}
		if (dataTypeAsInt == NotesItem.TYPE_TEXT) {
			String txtVal = (String) ItemDecoder.decodeTextValue(notesAPI, valueDataPtr, valueDataLength, false);
			return txtVal==null ? Collections.emptyList() : Arrays.asList((Object) txtVal);
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TEXT_LIST) {
			List<Object> textList = valueDataLength==0 ? Collections.emptyList() : ItemDecoder.decodeTextListValue(notesAPI, valueDataPtr, false);
			return textList==null ? Collections.emptyList() : textList;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER) {
			double numVal = ItemDecoder.decodeNumber(notesAPI, valueDataPtr, valueDataLength);
			return Arrays.asList((Object) Double.valueOf(numVal));
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER_RANGE) {
			List<Object> numberList = ItemDecoder.decodeNumberList(notesAPI, valueDataPtr, valueDataLength);
			return numberList==null ? Collections.emptyList() : numberList;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME) {
			boolean useDayLight = NotesDateTimeUtils.isDaylightTime();
			int gmtOffset = NotesDateTimeUtils.getGMTOffset();
			
			Calendar cal = ItemDecoder.decodeTimeDate(notesAPI, valueDataPtr, valueDataLength, useDayLight, gmtOffset);
			return cal==null ? Collections.emptyList() : Arrays.asList((Object) cal);
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME_RANGE) {
			boolean useDayLight = NotesDateTimeUtils.isDaylightTime();
			int gmtOffset = NotesDateTimeUtils.getGMTOffset();
			
			List<Object> calendarValues = ItemDecoder.decodeTimeDateList(notesAPI, valueDataPtr, useDayLight, gmtOffset);
			return calendarValues==null ? Collections.emptyList() : calendarValues;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_OBJECT) {
			NotesObjectDescriptorStruct objDescriptor = NotesObjectDescriptorStruct.newInstance(valueDataPtr);
			objDescriptor.read();
			
			int rrv = objDescriptor.RRV;
			
			if (objDescriptor.ObjectType == NotesCAPI.OBJECT_FILE) {
				Pointer fileObjectPtr = valueDataPtr;
				
				NotesFileObjectStruct fileObject = NotesFileObjectStruct.newInstance(fileObjectPtr);
				fileObject.read();
				
				short compressionType = fileObject.CompressionType;
				NotesTimeDateStruct fileCreated = fileObject.FileCreated;
				NotesTimeDateStruct fileModified = fileObject.FileModified;
				NotesTimeDate fileCreatedWrap = fileCreated==null ? null : new NotesTimeDate(fileCreated);
				NotesTimeDate fileModifiedWrap = fileModified==null ? null : new NotesTimeDate(fileModified);
				
				short fileNameLength = fileObject.FileNameLength;
				int fileSize = fileObject.FileSize;
				short flags = fileObject.Flags;
				
				Compression compression = null;
				for (Compression currComp : Compression.values()) {
					if (compressionType == currComp.getValue()) {
						compression = currComp;
						break;
					}
				}
				
				Pointer fileNamePtr = fileObjectPtr.share(NotesCAPI.fileObjectSize);
				String fileName = NotesStringUtils.fromLMBCS(fileNamePtr, fileNameLength);
				
				NotesAttachment attInfo = new NotesAttachment(fileName, compression, flags, fileSize,
						fileCreatedWrap, fileModifiedWrap, this,
						itemBlockId, rrv);
				
				return Arrays.asList((Object) attInfo);
			}
			//TODO add support for other object types
			return Arrays.asList((Object) objDescriptor);
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NOTEREF_LIST) {
			//skip LIST structure
			NotesUniversalNoteIdStruct unidStruct = NotesUniversalNoteIdStruct.newInstance(valueDataPtr.share(2));
			NotesUniversalNoteId unid = new NotesUniversalNoteId(unidStruct);
			return Arrays.asList((Object) unid);
		}
		else if (dataTypeAsInt == NotesItem.TYPE_COLLATION) {
			NotesCollationInfo colInfo = CollationDecoder.decodeCollation(valueDataPtr);
			return Arrays.asList((Object) colInfo);
		}
		else if (dataTypeAsInt == NotesItem.TYPE_VIEW_FORMAT) {
			NotesViewFormat viewFormatInfo = ViewFormatDecoder.decodeViewFormat(valueDataPtr,  valueDataLength);
			return Arrays.asList((Object) viewFormatInfo);
		}
		else if (dataTypeAsInt == NotesItem.TYPE_FORMULA) {
			boolean isSelectionFormula = "$FORMULA".equalsIgnoreCase(itemName) && getNoteClass().contains(NoteClass.VIEW);
			
			if (NotesJNAContext.is64Bit()) {
				LongByReference rethFormulaText = new LongByReference();
				ShortByReference retFormulaTextLength = new ShortByReference();
				short result = notesAPI.b64_NSFFormulaDecompile(valueDataPtr, isSelectionFormula, rethFormulaText, retFormulaTextLength);
				NotesErrorUtils.checkResult(result);

				Pointer formulaPtr = notesAPI.b64_OSLockObject(rethFormulaText.getValue());
				try {
					int textLen = (int) (retFormulaTextLength.getValue() & 0xffff);
					String formula = NotesStringUtils.fromLMBCS(formulaPtr, textLen);
					return Arrays.asList((Object) formula);
				}
				finally {
					notesAPI.b64_OSUnlockObject(rethFormulaText.getValue());
					notesAPI.b64_OSMemFree(rethFormulaText.getValue());
				}
			}
			else {
				IntByReference rethFormulaText = new IntByReference();
				ShortByReference retFormulaTextLength = new ShortByReference();
				short result = notesAPI.b32_NSFFormulaDecompile(valueDataPtr, isSelectionFormula, rethFormulaText, retFormulaTextLength);
				NotesErrorUtils.checkResult(result);
				
				Pointer formulaPtr = notesAPI.b32_OSLockObject(rethFormulaText.getValue());
				try {
					int textLen = (int) (retFormulaTextLength.getValue() & 0xffff);
					String formula = NotesStringUtils.fromLMBCS(formulaPtr, textLen);
					return Arrays.asList((Object) formula);
				}
				finally {
					notesAPI.b32_OSUnlockObject(rethFormulaText.getValue());
					notesAPI.b32_OSMemFree(rethFormulaText.getValue());
				}
			}
		}
		else if (dataTypeAsInt == NotesItem.TYPE_UNAVAILABLE) {
			return Collections.emptyList();
		}
		else {
			throw new UnsupportedItemValueError("Data type for value of item "+itemName+" is currently unsupported: "+dataTypeAsInt);
		}
	}

	/**
	 * Attaches a disk file to a note.<br>
	 * <br>
	 * To accomplish this, the function creates an item of TYPE_OBJECT, sub-category OBJECT_FILE,
	 * whose ITEM_xxx flag(s) are set to ITEM_SIGN | ITEM_SEAL.<br>
	 * The item that is built by NSFNoteAttachFile contains all relevant file information and
	 * the compressed file itself.<br>
	 * Since the Item APIs offer no means of dealing with signed, sealed, or compressed item values,
	 * the File Attachment API NSFNoteDetachFile must be used exclusively to access these items.
	 * 
	 * @param filePathOnDisk fully qualified file path specification for file being attached
	 * @param fileNameInNote filename that will be stored internally with the attachment, displayed with the attachment icon when the document is viewed in the Notes user interface, and subsequently used when selecting which attachment to Extract or Detach and what path to create for an Extracted file.  Note that these operations may be carried out both from the workstation application Attachments dialog box and programmatically, so try to choose meaningful filenames as opposed to attach.001, attach002, etc., whenever possible.  If attaching mulitiple files that have the same filename but different content to a single document, make sure this variable is unique in each call to NSFNoteAttachFile().
	 * @param compression compression to use
	 */
	public void attachFile(String filePathOnDisk, String fileNameInNote, Compression compression) {
		checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory $fileItemName = NotesStringUtils.toLMBCS("$FILE", true);
		Memory filePathOnDiskMem = NotesStringUtils.toLMBCS(filePathOnDisk, true);
		Memory fileNameInNoteMem = NotesStringUtils.toLMBCS(fileNameInNote, true);
		short compressionAsShort = (short) (compression.getValue() & 0xffff);
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFNoteAttachFile(m_hNote64, $fileItemName, (short) (($fileItemName.size()-1) & 0xffff), filePathOnDiskMem, fileNameInNoteMem, compressionAsShort);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFNoteAttachFile(m_hNote32, $fileItemName, (short) (($fileItemName.size()-1) & 0xffff), filePathOnDiskMem, fileNameInNoteMem, compressionAsShort);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * The method searches for a note attachment with the specified filename
	 * 
	 * @param fileName filename to search for (case-insensitive)
	 * @return attachment or null if not found
	 */
	public NotesAttachment getAttachment(final String fileName) {
		final NotesAttachment[] foundAttInfo = new NotesAttachment[1];
		
		getItems("$file", new IItemCallback() {
			
			@Override
			public void itemNotFound() {
			}
			
			@Override
			public Action itemFound(NotesItem item) {
				List<Object> values = item.getValues();
				if (values!=null && !values.isEmpty() && values.get(0) instanceof NotesAttachment) {
					NotesAttachment attInfo = (NotesAttachment) values.get(0);
					if (attInfo.getFileName().equalsIgnoreCase(fileName)) {
						foundAttInfo[0] = attInfo;
						return Action.Stop;
					}
				}
				return Action.Continue;
			}
		});
		return foundAttInfo[0];
	}
	
	/**
	 * Decodes the value(s) of the first item with the specified item name<br>
	 * <br>
	 * The supported data types are documented here: {@link NotesItem#getValues()}
	 * 
	 * @param itemName item name
	 * @return value(s) as list, not null
	 * @throws UnsupportedItemValueError if item type is not supported yet
	 */
	public List<Object> getItemValue(String itemName) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		NotesItem item = getFirstItem(itemName);
		if (item==null) {
			return Collections.emptyList();
		}
		
		int valueLength = item.getValueLength();

		Pointer valuePtr;
		
		//lock and decode value
		NotesBlockIdStruct valueBlockId = item.getValueBlockId();
		
		Pointer poolPtr;
		if (NotesJNAContext.is64Bit()) {
			poolPtr = notesAPI.b64_OSLockObject((long) valueBlockId.pool);
		}
		else {
			poolPtr = notesAPI.b32_OSLockObject(valueBlockId.pool);
		}
		
		int block = (int) (valueBlockId.block & 0xffff);
		long poolPtrLong = Pointer.nativeValue(poolPtr) + block;
		valuePtr = new Pointer(poolPtrLong);
		
		try {
			List<Object> values = getItemValue(itemName, item.getItemBlockId(), valuePtr, valueLength);
			return values;
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject((long) valueBlockId.pool);
				
			}
			else {
				notesAPI.b32_OSUnlockObject(valueBlockId.pool);
			}
		}
	}
	
	/**
	 * Callback interface for {@link NotesNote#getItems(IItemCallback)}
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IItemCallback {
		public static enum Action {Continue, Stop};
		
		/**
		 * Method is called when an item could not be found in the note
		 */
		public void itemNotFound();
		
		/**
		 * Method is called for each item in the note. A note may contain the same item name
		 * multiple times. In this case, the method is called for each item instance
		 * 
		 * @param item item object with meta data and access method to decode item value
		 * @return next action, either continue or stop scan
		 */
		public Action itemFound(NotesItem item);
	}
	
	/**
	 * Utility class to decode the item flag bitmask
	 * 
	 * @author Karsten Lehmann
	 */
	public static class ItemFlags {
		private int m_itemFlags;
		
		public ItemFlags(int itemFlags) {
			m_itemFlags = itemFlags;
		}
		
		public int getBitMask() {
			return m_itemFlags;
		}
		
		public boolean isSigned() {
			return (m_itemFlags & NotesCAPI.ITEM_SIGN) == NotesCAPI.ITEM_SIGN;
		}
		
		public boolean isSealed() {
			return (m_itemFlags & NotesCAPI.ITEM_SEAL) == NotesCAPI.ITEM_SEAL;
		}
		
		public boolean isSummary() {
			return (m_itemFlags & NotesCAPI.ITEM_SUMMARY) == NotesCAPI.ITEM_SUMMARY;
		}
		
		public boolean isReadWriters() {
			return (m_itemFlags & NotesCAPI.ITEM_READWRITERS) == NotesCAPI.ITEM_READWRITERS;
		}
		
		public boolean isNames() {
			return (m_itemFlags & NotesCAPI.ITEM_NAMES) == NotesCAPI.ITEM_NAMES;
		}
		
		public boolean isPlaceholder() {
			return (m_itemFlags & NotesCAPI.ITEM_PLACEHOLDER) == NotesCAPI.ITEM_PLACEHOLDER;
		}
		
		public boolean isProtected() {
			return (m_itemFlags & NotesCAPI.ITEM_PROTECTED) == NotesCAPI.ITEM_PROTECTED;
		}
		
		public boolean isReaders() {
			return (m_itemFlags & NotesCAPI.ITEM_READERS) == NotesCAPI.ITEM_READERS;
		}
		
		public boolean isUnchanged() {
			return (m_itemFlags & NotesCAPI.ITEM_UNCHANGED) == NotesCAPI.ITEM_UNCHANGED;
		}

	}
	
	/**
	 * Scans through all items of this note
	 * 
	 * @param callback callback is called for each item found
	 */
	public void getItems(final IItemCallback callback) {
		getItems((String) null, callback);
	}
	
	/**
	 * Scans through all items of this note that have the specified name
	 * 
	 * @param searchForItemName item name to search for or null to scan through all items
	 * @param callback callback is called for each scan result
	 */
	public void getItems(final String searchForItemName, final IItemCallback callback) {
		checkHandle();
		
		final NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		Memory itemNameMem = StringUtil.isEmpty(searchForItemName) ? null : NotesStringUtils.toLMBCS(searchForItemName, false);
		
		NotesBlockIdStruct.ByReference itemBlockId = NotesBlockIdStruct.ByReference.newInstance();
		NotesBlockIdStruct.ByReference valueBlockId = NotesBlockIdStruct.ByReference.newInstance();
		ShortByReference retDataType = new ShortByReference();
		IntByReference retValueLen = new IntByReference();
		
		short result;
		
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFItemInfo(m_hNote64, itemNameMem, itemNameMem==null ? 0 : (short) (itemNameMem.size() & 0xffff),
					itemBlockId, retDataType, valueBlockId, retValueLen);
		}
		else {
			result = notesAPI.b32_NSFItemInfo(m_hNote32, itemNameMem, itemNameMem==null ? 0 : (short) (itemNameMem.size() & 0xffff),
					itemBlockId, retDataType, valueBlockId, retValueLen);
		}
		
		if (result == INotesErrorConstants.ERR_ITEM_NOT_FOUND) {
			callback.itemNotFound();
			return;
		}

		NotesErrorUtils.checkResult(result);
		
		NotesBlockIdStruct itemBlockIdClone = new NotesBlockIdStruct();
		itemBlockIdClone.pool = itemBlockId.pool;
		itemBlockIdClone.block = itemBlockId.block;
		itemBlockIdClone.write();
		
		NotesBlockIdStruct valueBlockIdClone = new NotesBlockIdStruct();
		valueBlockIdClone.pool = valueBlockId.pool;
		valueBlockIdClone.block = valueBlockId.block;
		valueBlockIdClone.write();
		
		int dataType = retDataType.getValue();
		
		NotesItem itemInfo = new NotesItem(this, itemBlockIdClone, dataType,
				valueBlockIdClone);
		
		Action action = callback.itemFound(itemInfo);
		if (action != Action.Continue) {
			return;
		}
		
		while (true) {
			IntByReference retNextValueLen = new IntByReference();
			
			NotesBlockIdStruct.ByValue itemBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
			itemBlockIdByVal.pool = itemBlockId.pool;
			itemBlockIdByVal.block = itemBlockId.block;
			
			if (NotesJNAContext.is64Bit()) {
				result = notesAPI.b64_NSFItemInfoNext(m_hNote64, itemBlockIdByVal,
						itemNameMem, itemNameMem==null ? 0 : (short) (itemNameMem.size() & 0xffff), itemBlockId, retDataType,
						valueBlockId, retNextValueLen);
			}
			else {
				result = notesAPI.b32_NSFItemInfoNext(m_hNote32, itemBlockIdByVal,
						itemNameMem, itemNameMem==null ? 0 : (short) (itemNameMem.size() & 0xffff), itemBlockId, retDataType,
						valueBlockId, retNextValueLen);
			}

			if (result == INotesErrorConstants.ERR_ITEM_NOT_FOUND) {
				return;
			}

			NotesErrorUtils.checkResult(result);

			itemBlockIdClone = new NotesBlockIdStruct();
			itemBlockIdClone.pool = itemBlockId.pool;
			itemBlockIdClone.block = itemBlockId.block;
			itemBlockIdClone.write();
			
			valueBlockIdClone = new NotesBlockIdStruct();
			valueBlockIdClone.pool = valueBlockId.pool;
			valueBlockIdClone.block = valueBlockId.block;
			valueBlockIdClone.write();
			
			dataType = retDataType.getValue();

			itemInfo = new NotesItem(this, itemBlockIdClone, dataType,
					valueBlockIdClone);
			
			action = callback.itemFound(itemInfo);
			if (action != Action.Continue) {
				return;
			}
		}
	}

	/**
	 * Method to enumerate all CD records of a richtext item with the specified name.
	 * If Domino splits the richtext data into multiple items (because it does not fit into 64K),
	 * the method will process all available items.
	 * 
	 * @param richTextItemName richtext item name
	 * @param callback callback to call for each record
	 */
	public void enumerateRichTextCDRecords(String richTextItemName, final ICDRecordCallback callback) {
		final boolean[] aborted = new boolean[1];
		
		getItems(richTextItemName, new IItemCallback() {

			@Override
			public void itemNotFound() {
			}

			@Override
			public Action itemFound(NotesItem item) {
				if (item.getType()==NotesItem.TYPE_COMPOSITE) {
					item.enumerateRichTextCDRecords(new ICDRecordCallback() {

						@Override
						public Action recordVisited(ByteBuffer data, CDRecord parsedSignature, short signature,
								int dataLength, int cdRecordLength) {
							Action action = callback.recordVisited(data, parsedSignature, signature, dataLength, cdRecordLength);
							if (action==Action.Stop) {
								aborted[0] = true;
							}
							return action;
						}
					});
					if (aborted[0]) {
						return Action.Stop;
					}
				}
				return Action.Continue;
			}
		});
	}
	
	/**
	 * Extracts all text from a richtext item
	 * 
	 * @param itemName item name
	 * @return text content
	 */
	public String getRichtextContentAsText(String itemName) {
		final StringWriter sWriter = new StringWriter();
		
		//find all items with this name in case the content got to big to fit into one item alone
		getItems(itemName, new IItemCallback() {

			@Override
			public void itemNotFound() {
			}

			@Override
			public Action itemFound(NotesItem item) {
				if (item.getType()==NotesItem.TYPE_COMPOSITE) {
					item.getAllCompositeTextContent(sWriter);
				}
				return Action.Continue;
			}
		});
		
		return sWriter.toString();
	}
	
	
	/**
	 * Returns the first item with the specified name from the note
	 * 
	 * @param itemName item name
	 * @return item data or null if not found
	 */
	public NotesItem getFirstItem(String itemName) {
		if (itemName==null) {
			throw new IllegalArgumentException("Item name cannot be null. Use getItems() instead.");
		}
		
		final NotesItem[] retItem = new NotesItem[1];
		
		getItems(itemName, new IItemCallback() {

			@Override
			public void itemNotFound() {
				retItem[0]=null;
			}

			@Override
			public Action itemFound(NotesItem itemInfo) {
				retItem[0] = itemInfo;
				return Action.Stop;
			}
		});
		
		return retItem[0];
	}
	
	/**
	 * Checks whether this note is signed
	 * 
	 * @return true if signed
	 */
	public boolean isSigned() {
		checkHandle();
		
		ByteByReference signed_flag_ptr = new ByteByReference();
		ByteByReference sealed_flag_ptr = new ByteByReference();
		
		final NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			notesAPI.b64_NSFNoteIsSignedOrSealed(m_hNote64, signed_flag_ptr, sealed_flag_ptr);
			byte signed = signed_flag_ptr.getValue();
			return signed == 1;
		}
		else {
			notesAPI.b32_NSFNoteIsSignedOrSealed(m_hNote32, signed_flag_ptr, sealed_flag_ptr);
			byte signed = signed_flag_ptr.getValue();
			return signed == 1;
		}
	}
	
	/**
	 * Checks whether this note is sealed
	 * 
	 * @return true if sealed
	 */
	public boolean isSealed() {
		checkHandle();
		
		ByteByReference signed_flag_ptr = new ByteByReference();
		ByteByReference sealed_flag_ptr = new ByteByReference();
		
		final NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			notesAPI.b64_NSFNoteIsSignedOrSealed(m_hNote64, signed_flag_ptr, sealed_flag_ptr);
			byte sealed = sealed_flag_ptr.getValue();
			return sealed == 1;
		}
		else {
			notesAPI.b32_NSFNoteIsSignedOrSealed(m_hNote32, signed_flag_ptr, sealed_flag_ptr);
			byte sealed = sealed_flag_ptr.getValue();
			return sealed == 1;
		}
	}

	/** Possible validation phases for {@link NotesNote#computeWithForm(boolean, ComputeWithFormCallback)}  */
	public static enum ValidationPhase {
		/** Error occurred when processing the Default Value formula. */
		CWF_DV_FORMULA,
		/** Error occurred when processing the Translation formula. */
		CWF_IT_FORMULA,
		/**  Error occurred when processing the Validation formula. */
		CWF_IV_FORMULA,
		/** Error occurred when processing the computed field Value formula. */
		CWF_COMPUTED_FORMULA,
		/** Error occurred when verifying the data type for the field. */
		CWF_DATATYPE_CONVERSION,
		/** Error occurred when processing the computed field Value formula, during the "load" pass. */
		CWF_COMPUTED_FORMULA_LOAD,
		/** Error occurred when processing the computed field Value formula, during the "save" pass. */
		CWF_COMPUTED_FORMULA_SAVE
	};

	/* 	Possible return values from the callback routine specified in
	NSFNoteComputeWithForm() */
	public static enum CWF_Action {
		/** End all processing by NSFNoteComputeWithForm() and return the error status to the caller. */
		CWF_ABORT((short) 1),
		/** End validation of the current field and go on to the next. */
		CWF_NEXT_FIELD((short) 2),
		/** Begin the validation process for this field over again. */
		CWF_RECHECK_FIELD((short) 3);
		
		short actionVal;
		
		CWF_Action(short val) {
			this.actionVal = val;
		}
		
		public short getShortVal() {
			return actionVal;
		}

	}
	
	private ValidationPhase decodeValidationPhase(short phase) {
		ValidationPhase phaseEnum = null;
		if (phase == NotesCAPI.CWF_DV_FORMULA) {
			phaseEnum = ValidationPhase.CWF_DV_FORMULA;
		}
		else if (phase == NotesCAPI.CWF_IT_FORMULA) {
			phaseEnum = ValidationPhase.CWF_IT_FORMULA;
		}
		else if (phase == NotesCAPI.CWF_IV_FORMULA) {
			phaseEnum = ValidationPhase.CWF_IV_FORMULA;
		}
		else if (phase == NotesCAPI.CWF_COMPUTED_FORMULA) {
			phaseEnum = ValidationPhase.CWF_COMPUTED_FORMULA;
		}
		else if (phase == NotesCAPI.CWF_DATATYPE_CONVERSION) {
			phaseEnum = ValidationPhase.CWF_DATATYPE_CONVERSION;
		}
		else if (phase == NotesCAPI.CWF_COMPUTED_FORMULA_LOAD) {
			phaseEnum = ValidationPhase.CWF_COMPUTED_FORMULA_LOAD;
		}
		else if (phase == NotesCAPI.CWF_COMPUTED_FORMULA_SAVE) {
			phaseEnum = ValidationPhase.CWF_COMPUTED_FORMULA_SAVE;
		}

		return phaseEnum;
	}
	
	private FieldInfo readCDFieldInfo(Pointer ptrCDField) {
		NotesCDFieldStruct cdField = NotesCDFieldStruct.newInstance(ptrCDField);
		cdField.read();
		
		Pointer defaultValueFormulaPtr = ptrCDField.share(NotesCAPI.cdFieldSize);
		Pointer inputTranslationFormulaPtr = defaultValueFormulaPtr.share(cdField.DVLength & 0xffff);
		Pointer inputValidityCheckFormulaPtr = inputTranslationFormulaPtr.share((cdField.ITLength &0xffff) +
				(cdField.TabOrder & 0xffff));
//		Pointer namePtr = inputValidityCheckFormulaPtr.share(cdField.IVLength & 0xffff);
		
//		field.DVLength + field.ITLength + field.IVLength,
//        field.NameLength
        
		Pointer namePtr = ptrCDField.share((cdField.DVLength & 0xffff) + (cdField.ITLength & 0xffff) +
				(cdField.IVLength & 0xffff));
		Pointer descriptionPtr = namePtr.share(cdField.NameLength & 0xffff);
		
		String defaultValueFormula = NotesStringUtils.fromLMBCS(defaultValueFormulaPtr, cdField.DVLength & 0xffff);
		String inputTranslationFormula = NotesStringUtils.fromLMBCS(inputTranslationFormulaPtr, cdField.ITLength & 0xffff);
		String inputValidityCheckFormula = NotesStringUtils.fromLMBCS(inputValidityCheckFormulaPtr, cdField.IVLength & 0xffff);
		String name = NotesStringUtils.fromLMBCS(namePtr, cdField.NameLength & 0xffff);
		String description = NotesStringUtils.fromLMBCS(descriptionPtr, cdField.DescLength & 0xffff);
		
		return new FieldInfo(defaultValueFormula, inputTranslationFormula, inputValidityCheckFormula,
				name, description);
	}
	
	public static class FieldInfo {
		private String m_defaultValueFormula;
		private String m_inputTranslationFormula;
		private String m_inputValidityCheckFormula;
		private String m_name;
		private String m_description;
		
		public FieldInfo(String defaultValueFormula, String inputTranslationFormula, String inputValidityCheckFormula,
				String name, String description) {
			m_defaultValueFormula = defaultValueFormula;
			m_inputTranslationFormula = inputTranslationFormula;
			m_inputValidityCheckFormula = inputValidityCheckFormula;
			m_name = name;
			m_description = description;
		}
		
		public String getDefaultValueFormula() {
			return m_defaultValueFormula;
		}
		
		public String getInputTranslationFormula() {
			return m_inputTranslationFormula;
		}
		
		public String getInputValidityCheckFormula() {
			return m_inputValidityCheckFormula;
		}
		
		public String getName() {
			return m_name;
		}
		
		public String getDescription() {
			return m_description;
		}
		
		@Override
		public String toString() {
			return "FieldInfo [name="+getName()+", description="+getDescription()+", default="+getDefaultValueFormula()+
					", inputtranslation="+getInputTranslationFormula()+", validation="+getInputValidityCheckFormula()+"]";

		}

	}
	
	public void computeWithForm(boolean continueOnError, final ComputeWithFormCallback callback) {
		checkHandle();

		int dwFlags = 0;
		if (continueOnError) {
			dwFlags = NotesCAPI.CWF_CONTINUE_ON_ERROR;
		}
		
		final NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			b64_CWFErrorProc errorProc;
			if (notesAPI instanceof WinNotesCAPI) {
				errorProc = new WinNotesCAPI.b64_CWFErrorProcWin() {

					@Override
					public short invoke(Pointer pCDField, short phase, short error, long hErrorText,
							short wErrorTextSize, Pointer ctx) {
						
						String errorTxt;
						if (hErrorText==0) {
							errorTxt = "";
						}
						else {
							Pointer errorTextPtr = notesAPI.b64_OSLockObject(hErrorText);
							
							try {
								//TODO find out where this offset 6 comes from
								errorTxt = NotesStringUtils.fromLMBCS(errorTextPtr.share(6), (wErrorTextSize & 0xffff)-6);
							}
							finally {
								notesAPI.b64_OSUnlockObject(hErrorText);
							}
						}

						FieldInfo fieldInfo = readCDFieldInfo(pCDField);
						ValidationPhase phaseEnum = decodeValidationPhase(phase);

						CWF_Action action;
						if (callback==null) {
							action = CWF_Action.CWF_ABORT;
						}
						else {
							action = callback.errorRaised(fieldInfo, phaseEnum, errorTxt, hErrorText);
						}
						return action==null ? CWF_Action.CWF_ABORT.getShortVal() : action.getShortVal();
					}
					
				};
			}
			else {
				errorProc = new b64_CWFErrorProc() {

					@Override
					public short invoke(Pointer pCDField, short phase, short error, long hErrorText,
							short wErrorTextSize, Pointer ctx) {
						
						String errorTxt;
						if (hErrorText==0) {
							errorTxt = "";
						}
						else {
							Pointer errorTextPtr = notesAPI.b64_OSLockObject(hErrorText);
							System.out.println("ErrorTextPtr: "+errorTextPtr.dump(0, (int) (wErrorTextSize & 0xffff)));
							try {
								//TODO find out where this offset 6 comes from
								errorTxt = NotesStringUtils.fromLMBCS(errorTextPtr.share(6), (wErrorTextSize & 0xffff)-6);
							}
							finally {
								notesAPI.b64_OSUnlockObject(hErrorText);
							}
						}

						FieldInfo fieldInfo = readCDFieldInfo(pCDField);
						ValidationPhase phaseEnum = decodeValidationPhase(phase);

						CWF_Action action;
						if (callback==null) {
							action = CWF_Action.CWF_ABORT;
						}
						else {
							action = callback.errorRaised(fieldInfo, phaseEnum, errorTxt, hErrorText);
						}
						return action==null ? CWF_Action.CWF_ABORT.getShortVal() : action.getShortVal();
					}
					
				};
			}
			short result = notesAPI.b64_NSFNoteComputeWithForm(m_hNote64, 0, dwFlags, errorProc, null);
			NotesErrorUtils.checkResult(result);
		}
		else {
			b32_CWFErrorProc errorProc;
			if (notesAPI instanceof WinNotesCAPI) {
				errorProc = new WinNotesCAPI.b32_CWFErrorProcWin() {

					@Override
					public short invoke(Pointer pCDField, short phase, short error, int hErrorText,
							short wErrorTextSize, Pointer ctx) {
						
						String errorTxt;
						if (hErrorText==0) {
							errorTxt = "";
						}
						else {
							Pointer errorTextPtr = notesAPI.b32_OSLockObject(hErrorText);
							try {
								//TODO find out where this offset 6 comes from
								errorTxt = NotesStringUtils.fromLMBCS(errorTextPtr.share(6), (wErrorTextSize & 0xffff)-6);
							}
							finally {
								notesAPI.b32_OSUnlockObject(hErrorText);
							}
						}

						FieldInfo fieldInfo = readCDFieldInfo(pCDField);
						ValidationPhase phaseEnum = decodeValidationPhase(phase);

						CWF_Action action;
						if (callback==null) {
							action = CWF_Action.CWF_ABORT;
						}
						else {
							action = callback.errorRaised(fieldInfo, phaseEnum, errorTxt, hErrorText);
						}
						return action==null ? CWF_Action.CWF_ABORT.getShortVal() : action.getShortVal();
					}

				};
			}
			else {
				errorProc = new b32_CWFErrorProc() {

					@Override
					public short invoke(Pointer pCDField, short phase, short error, int hErrorText,
							short wErrorTextSize, Pointer ctx) {
						
						String errorTxt;
						if (hErrorText==0) {
							errorTxt = "";
						}
						else {
							Pointer errorTextPtr = notesAPI.b32_OSLockObject(hErrorText);
							try {
								//TODO find out where this offset 6 comes from
								errorTxt = NotesStringUtils.fromLMBCS(errorTextPtr.share(6), (wErrorTextSize & 0xffff)-6);
							}
							finally {
								notesAPI.b32_OSUnlockObject(hErrorText);
							}
						}

						FieldInfo fieldInfo = readCDFieldInfo(pCDField);
						ValidationPhase phaseEnum = decodeValidationPhase(phase);

						CWF_Action action;
						if (callback==null) {
							action = CWF_Action.CWF_ABORT;
						}
						else {
							action = callback.errorRaised(fieldInfo, phaseEnum, errorTxt, hErrorText);
						}
						return action==null ? CWF_Action.CWF_ABORT.getShortVal() : action.getShortVal();
					}

				};
			}
			short result = notesAPI.b32_NSFNoteComputeWithForm(m_hNote32, 0, dwFlags, errorProc, null);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	public static interface ComputeWithFormCallback {
		
		public CWF_Action errorRaised(FieldInfo fieldInfo, ValidationPhase phase, String errorTxt, long errCode);
	}
	
	public static enum EncryptionMode {
		/** Encrypt the message with the key in the user's ID. This flag is not for outgoing mail
		 * messages because recipients, other than the sender, will not be able to decrypt
		 * the message. This flag can be useful to encrypt documents in a local database to
		 * keep them secure or to encrypt documents that can only be decrypted by the same user. */
		ENCRYPT_WITH_USER_PUBLIC_KEY (NotesCAPI.ENCRYPT_WITH_USER_PUBLIC_KEY),
		
		/**
		 * Encrypt SMIME if MIME present
		 */
		ENCRYPT_SMIME_IF_MIME_PRESENT (NotesCAPI.ENCRYPT_SMIME_IF_MIME_PRESENT),
		
		/**
		 * Encrypt SMIME no sender.
		 */
		ENCRYPT_SMIME_NO_SENDER (NotesCAPI.ENCRYPT_SMIME_NO_SENDER),
		
		/**
		 * Encrypt SMIME trusting all certificates.
		 */
		ENCRYPT_SMIME_TRUST_ALL_CERTS(NotesCAPI.ENCRYPT_SMIME_TRUST_ALL_CERTS);
		
		private int m_mode;
		
		private EncryptionMode(int mode) {
			m_mode = mode;
		}
		
		public int getMode() {
			return m_mode;
		}
	};

	/**
	 * This function copies and encrypts (seals) the encryption enabled fields in a note
	 * (including the note's file objects), using the current ID file.<br>
	 * <br>
	 * It can encrypt a note in several ways -- by using the Domino public key of the caller,
	 * by using specified secret encryption keys stored in the caller's ID, or by using the
	 * Domino public keys of specified users, if the note does not have any mime parts.<br>
	 * <br>
	 * The method decides which type of encryption to do based upon the setting of the flag
	 * passed to it in its <code>encryptionMode</code> argument.<br>
	 * <br>
	 * If the {@link EncryptionMode#ENCRYPT_WITH_USER_PUBLIC_KEY} flag is set, it uses the
	 * caller's public ID to encrypt the note.<br>
	 * In this case, only the user who encodes the note can decrypt it.<br>
	 * This feature allows an individual to protect information from anyone else.<br>
	 * <br>
	 * If, instead, the {@link EncryptionMode#ENCRYPT_WITH_USER_PUBLIC_KEY} flag is not set,
	 * then the function expects the note to contain  a field named "SecretEncryptionKeys"
	 * a field named "PublicEncryptionKeys", or both.<br>
	 * Each field is either a TYPE_TEXT or TYPE_TEXT_LIST field.<br>
	 * <br>
	 * "SecretEncryptionKeys" contains the name(s) of the secret encryption keys in the
	 * calling user's ID to be used to encrypt the note.<br>
	 * This feature is intended to allow a group to encrypt some of the notes in a single
	 * database in a way that only they can decrypt them -- they must share the secret encryption
	 * keys among themselves first for this to work.<br>
	 * <br>
	 * "PublicEncryptionKeys" contains the name(s) of  users, in canonical format.<br>
	 * The note will be encrypted with each user's Domino public key.<br>
	 * The user can then decrypt the note with the private key in the user's ID.<br>
	 * This feature provides a way to encrypt documents, such as mail documents, for another user.<br>
	 * <br>
	 * The note must contain at least one encryption enabled item (an item with the ITEM_SEAL flag set)
	 * in order to be encrypted.<br>
	 * If the note has mime parts and flag {@link EncryptionMode#ENCRYPT_SMIME_IF_MIME_PRESENT}
	 * is set, then it is SMIME encrypted.<br>
	 * <br>
	 * If the document is to be signed as well as encrypted, you must sign the document
	 * before using this method.

	 * @param encryptionMode encryption mode
	 * @return encrypted note copy
	 */
	public NotesNote copyAndEncrypt(EnumSet<EncryptionMode> encryptionMode) {
		return copyAndEncrypt(null, encryptionMode);
	}
		
	/**
	 * This function copies and encrypts (seals) the encryption enabled fields in a note
	 * (including the note's file objects), using a handle to an ID file.<br>
	 * <br>
	 * It can encrypt a note in several ways -- by using the Domino public key of the caller,
	 * by using specified secret encryption keys stored in the caller's ID, or by using the
	 * Domino public keys of specified users, if the note does not have any mime parts.<br>
	 * <br>
	 * The method decides which type of encryption to do based upon the setting of the flag
	 * passed to it in its <code>encryptionMode</code> argument.<br>
	 * <br>
	 * If the {@link EncryptionMode#ENCRYPT_WITH_USER_PUBLIC_KEY} flag is set, it uses the
	 * caller's public ID to encrypt the note.<br>
	 * In this case, only the user who encodes the note can decrypt it.<br>
	 * This feature allows an individual to protect information from anyone else.<br>
	 * <br>
	 * If, instead, the {@link EncryptionMode#ENCRYPT_WITH_USER_PUBLIC_KEY} flag is not set,
	 * then the function expects the note to contain  a field named "SecretEncryptionKeys"
	 * a field named "PublicEncryptionKeys", or both.<br>
	 * Each field is either a TYPE_TEXT or TYPE_TEXT_LIST field.<br>
	 * <br>
	 * "SecretEncryptionKeys" contains the name(s) of the secret encryption keys in the
	 * calling user's ID to be used to encrypt the note.<br>
	 * This feature is intended to allow a group to encrypt some of the notes in a single
	 * database in a way that only they can decrypt them -- they must share the secret encryption
	 * keys among themselves first for this to work.<br>
	 * <br>
	 * "PublicEncryptionKeys" contains the name(s) of  users, in canonical format.<br>
	 * The note will be encrypted with each user's Domino public key.<br>
	 * The user can then decrypt the note with the private key in the user's ID.<br>
	 * This feature provides a way to encrypt documents, such as mail documents, for another user.<br>
	 * <br>
	 * The note must contain at least one encryption enabled item (an item with the ITEM_SEAL flag set)
	 * in order to be encrypted.<br>
	 * If the note has mime parts and flag {@link EncryptionMode#ENCRYPT_SMIME_IF_MIME_PRESENT}
	 * is set, then it is SMIME encrypted.<br>
	 * <br>
	 * If the document is to be signed as well as encrypted, you must sign the document
	 * before using this method.

	 * @param id user id to be used for encryption, use null for current id
	 * @param encryptionMode encryption mode
	 * @return encrypted note copy
	 */
	public NotesNote copyAndEncrypt(NotesUserId id, EnumSet<EncryptionMode> encryptionMode) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		int flags = 0;
		for (EncryptionMode currMode : encryptionMode) {
			flags = flags | currMode.getMode();
		}
		
		short flagsShort = (short) (flags & 0xffff);
		
		short result;
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethDstNote = new LongByReference();
			result = notesAPI.b64_NSFNoteCopyAndEncryptExt2(m_hNote64, id==null ? 0 : id.getHandle64(), flagsShort, rethDstNote, 0, null);
			NotesErrorUtils.checkResult(result);
			
			NotesNote copyNote = new NotesNote(m_parentDb, rethDstNote.getValue());
			NotesGC.__objectCreated(NotesNote.class, copyNote);
			return copyNote;
		}
		else {
			IntByReference rethDstNote = new IntByReference();
			result = notesAPI.b32_NSFNoteCopyAndEncryptExt2(m_hNote32, id==null ? 0 : id.getHandle32(), flagsShort, rethDstNote, 0, null);
			NotesErrorUtils.checkResult(result);
			
			NotesNote copyNote = new NotesNote(m_parentDb, rethDstNote.getValue());
			NotesGC.__objectCreated(NotesNote.class, copyNote);
			return copyNote;
		}
	}
	
	public NotesNote copyToDatabase(NotesDatabase targetDb) {
		checkHandle();

		if (targetDb.isRecycled()) {
			throw new NotesError(0, "Target database already recycled");
		}
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			LongByReference note_handle_dst = new LongByReference();
			short result = notesAPI.b64_NSFNoteCopy(m_hNote64, note_handle_dst);
			NotesErrorUtils.checkResult(result);
			
			NotesNote copyNote = new NotesNote(targetDb, note_handle_dst.getValue());
			
			NotesOriginatorId newOid = targetDb.generateOID();
			NotesOriginatorIdStruct newOidStruct = newOid.getAdapter(NotesOriginatorIdStruct.class);
			
			notesAPI.b64_NSFNoteSetInfo(copyNote.getHandle64(), NotesCAPI._NOTE_ID, null);
			notesAPI.b64_NSFNoteSetInfo(copyNote.getHandle64(), NotesCAPI._NOTE_OID, newOidStruct.getPointer());
			
			LongByReference targetDbHandle = new LongByReference();
			targetDbHandle.setValue(targetDb.getHandle64());
			notesAPI.b64_NSFNoteSetInfo(copyNote.getHandle64(), NotesCAPI._NOTE_DB, targetDbHandle.getPointer());
			
			NotesGC.__objectCreated(NotesNote.class, copyNote);
			return copyNote;
		}
		else {
			IntByReference note_handle_dst = new IntByReference();
			short result = notesAPI.b32_NSFNoteCopy(m_hNote32, note_handle_dst);
			NotesErrorUtils.checkResult(result);
			
			NotesNote copyNote = new NotesNote(targetDb, note_handle_dst.getValue());
			
			NotesOriginatorId newOid = targetDb.generateOID();
			NotesOriginatorIdStruct newOidStruct = newOid.getAdapter(NotesOriginatorIdStruct.class);
			
			notesAPI.b32_NSFNoteSetInfo(copyNote.getHandle32(), NotesCAPI._NOTE_ID, null);
			notesAPI.b32_NSFNoteSetInfo(copyNote.getHandle32(), NotesCAPI._NOTE_OID, newOidStruct.getPointer());
			
			IntByReference targetDbHandle = new IntByReference();
			targetDbHandle.setValue(targetDb.getHandle32());
			notesAPI.b32_NSFNoteSetInfo(copyNote.getHandle32(), NotesCAPI._NOTE_DB, targetDbHandle.getPointer());
			
			NotesGC.__objectCreated(NotesNote.class, copyNote);
			return copyNote;
		}
	}
	
	/**
	 * This function decrypts an encrypted note, using the current user's ID file.<br>
	 * If the user does not have the appropriate encryption key to decrypt the note, an error is returned.<br>
	 * <br>
	 * This function supports new cryptographic keys and algorithms introduced in Release 8.0.1 as
	 * well as any from releases prior to 8.0.1.
	 * <br>
	 * The current implementation of this function automatically decrypts attachments as well.
	 */
	public void decrypt() {
		decrypt(null);
	}
	
	/**
	 * This function decrypts an encrypted note, using the appropriate encryption key stored
	 * in the user's ID file.<br>
	 * If the user does not have the appropriate encryption key to decrypt the note, an error is returned.<br>
	 * <br>
	 * This function supports new cryptographic keys and algorithms introduced in Release 8.0.1 as
	 * well as any from releases prior to 8.0.1.
	 * <br>
	 * The current implementation of this function automatically decrypts attachments as well.
	 * 
	 * @param id user id used for decryption, use null for current id
	 */
	public void decrypt(NotesUserId id) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		short decryptFlags = NotesCAPI.DECRYPT_ATTACHMENTS_IN_PLACE;
		
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFNoteCipherDecrypt(m_hNote64, id==null ? 0 : id.getHandle64(), decryptFlags,
					null, 0, null);
		}
		else {
			result = notesAPI.b32_NSFNoteCipherDecrypt(m_hNote32, id==null ? 0 : id.getHandle32(), decryptFlags,
					null, 0, null);
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * Removes any existing field with the specified name and creates a new one with the specified value, setting the {@link ItemType#SUMMARY}<br>
	 * We support the following value types:
	 * <ul>
	 * <li>String and List&lt;String&gt; (max. 65535 entries)</li>
	 * <li>Double, double, Integer, int, Long, long, Float, float for numbers (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number types for multiple numbers (max. 65535 entries)</li>
	 * <li>Double[], double[], Integer[], int[], Long[], long[], Float[], float[] with 2 elements (lower/upper) for number ranges (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number range types for multiple number ranges</li>
	 * <li>Calendar, Date, NotesTimeDate</li>
	 * <li>{@link List} of date types for multiple dates</li>
	 * <li>Calendar[], Date[], NotesTimeDate[] with 2 elements (lower/upper) for date ranges</li>
	 * <li>{@link List} of date range types for multiple date ranges (max. 65535 entries)</li>
	 * </ul>
	 * 
	 * @param itemName item name
	 * @param value item value, see method comment for allowed types
	 * @return created item
	 */
	public NotesItem replaceItemValue(String itemName, Object value) {
		return replaceItemValue(itemName, EnumSet.of(ItemType.SUMMARY), value);
	}

	/**
	 * Removes any existing field with the specified name and creates a new one with the specified value.<br>
	 * We support the following value types:
	 * <ul>
	 * <li>String and List&lt;String&gt; (max. 65535 entries)</li>
	 * <li>Double, double, Integer, int, Long, long, Float, float for numbers (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number types for multiple numbers (max. 65535 entries)</li>
	 * <li>Double[], double[], Integer[], int[], Long[], long[], Float[], float[] with 2 elements (lower/upper) for number ranges (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number range types for multiple number ranges</li>
	 * <li>Calendar, Date, NotesTimeDate</li>
	 * <li>{@link List} of date types for multiple dates</li>
	 * <li>Calendar[], Date[], NotesTimeDate[] with 2 elements (lower/upper) for date ranges</li>
	 * <li>{@link List} of date range types for multiple date ranges (max. 65535 entries)</li>
	 * </ul>
	 * 
	 * @param itemName item name
	 * @param flags item flags, e.g. {@link ItemType#SUMMARY}
	 * @param value item value, see method comment for allowed types
	 * @return created item
	 */
	public NotesItem replaceItemValue(String itemName, EnumSet<ItemType> flags, Object value) {
		if (!hasSupportedItemObjectType(value)) {
			throw new IllegalArgumentException("Unsupported value type: "+(value==null ? "null" : value.getClass().getName()));
		}
		
		while (hasItem(itemName)) {
			removeItem(itemName);
		}
		return appendItemValue(itemName, flags, value);
	}
	
	@SuppressWarnings("rawtypes")
	private boolean hasSupportedItemObjectType(Object value) {
		if (value instanceof String) {
			return true;
		}
		else if (value instanceof Number) {
			return true;
		}
		else if (value instanceof Calendar || value instanceof NotesTimeDate || value instanceof Date) {
			return true;
		}
		else if (value instanceof List && ((List)value).isEmpty()) {
			return true;
		}
		else if (value instanceof List && isStringList((List) value)) {
			return true;
		}
		else if (value instanceof List && isNumberOrNumberArrayList((List) value)) {
			return true;
		}
		else if (value instanceof List && isCalendarOrCalendarArrayList((List) value)) {
			return true;
		}
		else if (value instanceof Calendar[] && ((Calendar[])value).length==2) {
			return true;
		}
		else if (value instanceof NotesTimeDate[] && ((NotesTimeDate[])value).length==2) {
			return true;
		}
		else if (value instanceof Date[] && ((Date[])value).length==2) {
			return true;
		}
		else if (value instanceof Number[] && ((Number[])value).length==2) {
			return true;
		}
		else if (value instanceof Double[] && ((Double[])value).length==2) {
			return true;
		}
		else if (value instanceof Integer[] && ((Integer[])value).length==2) {
			return true;
		}
		else if (value instanceof Long[] && ((Long[])value).length==2) {
			return true;
		}
		else if (value instanceof Float[] && ((Float[])value).length==2) {
			return true;
		}
		else if (value instanceof double[] && ((double[])value).length==2) {
			return true;
		}
		else if (value instanceof int[] && ((int[])value).length==2) {
			return true;
		}
		else if (value instanceof long[] && ((long[])value).length==2) {
			return true;
		}
		else if (value instanceof float[] && ((float[])value).length==2) {
			return true;
		}
		else if (value instanceof NotesUniversalNoteId) {
			return true;
		}
		return false;
	}
	
	/**
	 * Creates a new item with the specified value, setting the {@link ItemType#SUMMARY} (does not overwrite items with the same name)<br>
	 * We support the following value types:
	 * <ul>
	 * <li>String and List&lt;String&gt; (max. 65535 entries)</li>
	 * <li>Double, double, Integer, int, Long, long, Float, float for numbers (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number types for multiple numbers (max. 65535 entries)</li>
	 * <li>Double[], double[], Integer[], int[], Long[], long[], Float[], float[] with 2 elements (lower/upper) for number ranges (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number range types for multiple number ranges</li>
	 * <li>Calendar, Date, NotesTimeDate</li>
	 * <li>{@link List} of date types for multiple dates</li>
	 * <li>Calendar[], Date[], NotesTimeDate[] with 2 elements (lower/upper) for date ranges</li>
	 * <li>{@link List} of date range types for multiple date ranges (max. 65535 entries)</li>
	 * </ul>
	 * 
	 * @param itemName item name
	 * @param value item value, see method comment for allowed types
	 * @return created item
	 */
	public NotesItem appendItemValue(String itemName, Object value) {
		return appendItemValue(itemName, EnumSet.of(ItemType.SUMMARY), value);
	}
	
	/**
	 * Creates a new item with the specified item flags and value<br>
	 * We support the following value types:
	 * <ul>
	 * <li>String and List&lt;String&gt; (max. 65535 entries)</li>
	 * <li>Double, double, Integer, int, Long, long, Float, float for numbers (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number types for multiple numbers (max. 65535 entries)</li>
	 * <li>Double[], double[], Integer[], int[], Long[], long[], Float[], float[] with 2 elements (lower/upper) for number ranges (will all be converted to double, because that's how Domino stores them)</li>
	 * <li>{@link List} of number range types for multiple number ranges</li>
	 * <li>Calendar, Date, NotesTimeDate</li>
	 * <li>{@link List} of date types for multiple dates</li>
	 * <li>Calendar[], Date[], NotesTimeDate[] with 2 elements (lower/upper) for date ranges</li>
	 * <li>{@link List} of date range types for multiple date ranges (max. 65535 entries)</li>
	 * <li>{@link NotesUniversalNoteId} for $REF like items</li>
	 * </ul>
	 * 
	 * @param itemName item name
	 * @param flags item flags
	 * @param value item value, see method comment for allowed types
	 * @return created item
	 */
	public NotesItem appendItemValue(String itemName, EnumSet<ItemType> flags, Object value) {
		checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		if (value instanceof String) {
			Memory strValueMem = NotesStringUtils.toLMBCS((String)value, false);

			int valueSize = (int) (2 + (strValueMem==null ? 0 : strValueMem.size()));
			
			if (NotesJNAContext.is64Bit()) {
				LongByReference rethItem = new LongByReference();
				short result = notesAPI.b64_OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = notesAPI.b64_OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_TEXT);
					valuePtr = valuePtr.share(2);
					if (strValueMem!=null) {
						valuePtr.write(0, strValueMem.getByteArray(0, (int) strValueMem.size()), 0, (int) strValueMem.size());
					}
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TEXT, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					notesAPI.b64_OSUnlockObject(rethItem.getValue());
				}
			}
			else {
				IntByReference rethItem = new IntByReference();
				short result = notesAPI.b32_OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = notesAPI.b32_OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_TEXT);
					valuePtr = valuePtr.share(2);
					if (strValueMem!=null) {
						valuePtr.write(0, strValueMem.getByteArray(0, (int) strValueMem.size()), 0, (int) strValueMem.size());
					}
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TEXT, rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					notesAPI.b32_OSUnlockObject(rethItem.getValue());
				}
			}
		
		}
		else if (value instanceof Number) {
			int valueSize = 2 + 8;
			
			if (NotesJNAContext.is64Bit()) {
				LongByReference rethItem = new LongByReference();
				short result = notesAPI.b64_OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = notesAPI.b64_OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_NUMBER);
					valuePtr = valuePtr.share(2);
					valuePtr.setDouble(0, ((Number)value).doubleValue());
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_NUMBER, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					notesAPI.b64_OSUnlockObject(rethItem.getValue());
				}
			}
			else {
				IntByReference rethItem = new IntByReference();
				short result = notesAPI.b32_OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = notesAPI.b32_OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_NUMBER);
					valuePtr = valuePtr.share(2);
					valuePtr.setDouble(0, ((Number)value).doubleValue());
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_NUMBER, rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					notesAPI.b32_OSUnlockObject(rethItem.getValue());
				}
			}
		}
		else if (value instanceof Calendar || value instanceof NotesTimeDate || value instanceof Date) {
			Calendar calValue;
			if (value instanceof Calendar) {
				calValue = (Calendar) value;
			}
			else if (value instanceof NotesTimeDate) {
				calValue = ((NotesTimeDate)value).getTimeAsCalendar();
			}
			else if (value instanceof Date) {
				calValue = Calendar.getInstance();
				calValue.setTime((Date) value);
			}
			else {
				throw new IllegalArgumentException("Unsupported date value type: "+(value==null ? "null" : value.getClass().getName()));
			}
			
			boolean hasDate = NotesDateTimeUtils.hasDate(calValue);
			boolean hasTime = NotesDateTimeUtils.hasTime(calValue);
			int[] innards = NotesDateTimeUtils.calendarToInnards(calValue, hasDate, hasTime);

			int valueSize = 2 + 8;
			
			if (NotesJNAContext.is64Bit()) {
				LongByReference rethItem = new LongByReference();
				short result = notesAPI.b64_OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = notesAPI.b64_OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_TIME);
					valuePtr = valuePtr.share(2);

					NotesTimeDateStruct timeDate = NotesTimeDateStruct.newInstance(valuePtr);
					timeDate.Innards[0] = innards[0];
					timeDate.Innards[1] = innards[1];
					timeDate.write();

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TIME, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					notesAPI.b64_OSUnlockObject(rethItem.getValue());
				}
			}
			else {
				IntByReference rethItem = new IntByReference();
				short result = notesAPI.b32_OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = notesAPI.b32_OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_TIME);
					valuePtr = valuePtr.share(2);

					NotesTimeDateStruct timeDate = NotesTimeDateStruct.newInstance(valuePtr);
					timeDate.Innards[0] = innards[0];
					timeDate.Innards[1] = innards[1];
					timeDate.write();

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TIME, rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					notesAPI.b32_OSUnlockObject(rethItem.getValue());
				}
			}
		}
		else if (value instanceof List && (((List)value).isEmpty() || isStringList((List) value))) {
			List<String> strList = (List<String>) value;
			
			if (strList.size()> 65535) {
				throw new IllegalArgumentException("String list size must fit in a WORD ("+strList.size()+">65535)");
			}
			
			short result;
			if (NotesJNAContext.is64Bit()) {
				LongByReference rethList = new LongByReference();
				ShortByReference retListSize = new ShortByReference();

				result = notesAPI.b64_ListAllocate((short) 0, 
						(short) 0,
						1, rethList, null, retListSize);
				
				NotesErrorUtils.checkResult(result);

				long hList = rethList.getValue();
				notesAPI.b64_OSUnlockObject(hList);
				
				for (int i=0; i<strList.size(); i++) {
					String currStr = strList.get(i);
					Memory currStrMem = NotesStringUtils.toLMBCS(currStr, false);

					result = notesAPI.b64_ListAddEntry(hList, 1, retListSize, (short) (i & 0xffff), currStrMem,
							(short) (currStrMem==null ? 0 : (currStrMem.size() & 0xffff)));
					NotesErrorUtils.checkResult(result);
				}
				
				int listSize = retListSize.getValue() & 0xffff;
				
				@SuppressWarnings("unused")
				Pointer valuePtr = notesAPI.b64_OSLockObject(hList);
				try {
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TEXT_LIST, (int) hList, listSize);
					return item;
				}
				finally {
					notesAPI.b64_OSUnlockObject(hList);
				}
			}
			else {
				IntByReference rethList = new IntByReference();
				ShortByReference retListSize = new ShortByReference();

				result = notesAPI.b32_ListAllocate((short) 0, 
						(short) 0,
						1, rethList, null, retListSize);
				
				NotesErrorUtils.checkResult(result);

				int hList = rethList.getValue();
				notesAPI.b32_OSUnlockObject(hList);
				
				for (int i=0; i<strList.size(); i++) {
					String currStr = strList.get(i);
					Memory currStrMem = NotesStringUtils.toLMBCS(currStr, false);

					result = notesAPI.b32_ListAddEntry(hList, 1, retListSize, (short) (i & 0xffff), currStrMem,
							(short) (currStrMem==null ? 0 : (currStrMem.size() & 0xffff)));
					NotesErrorUtils.checkResult(result);
				}
				
				int listSize = retListSize.getValue() & 0xffff;
				
				@SuppressWarnings("unused")
				Pointer valuePtr = notesAPI.b32_OSLockObject(hList);
				try {
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TEXT_LIST, (int) hList, listSize);
					return item;
				}
				finally {
					notesAPI.b32_OSUnlockObject(hList);
				}
			}
		}
		else if (value instanceof List && isNumberOrNumberArrayList((List) value)) {
			List<?> numberOrNumberArrList = toNumberOrNumberArrayList((List<?>) value);
			
			List<Number> numberList = new ArrayList<Number>();
			List<double[]> numberArrList = new ArrayList<double[]>();
			
			for (int i=0; i<numberOrNumberArrList.size(); i++) {
				Object currObj = numberOrNumberArrList.get(i);
				if (currObj instanceof Double) {
					numberList.add((Number) currObj);
				}
				else if (currObj instanceof double[]) {
					numberArrList.add((double[])currObj);
				}
			}
			
			if (numberList.size()> 65535) {
				throw new IllegalArgumentException("Number list size must fit in a WORD ("+numberList.size()+">65535)");
			}

			if (numberArrList.size()> 65535) {
				throw new IllegalArgumentException("Number range list size must fit in a WORD ("+numberList.size()+">65535)");
			}

			int valueSize = 2 + NotesCAPI.rangeSize + 
					8 * numberList.size() +
					NotesCAPI.numberPairSize * numberArrList.size();

			if (NotesJNAContext.is64Bit()) {
				LongByReference rethItem = new LongByReference();
				short result = notesAPI.b64_OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = notesAPI.b64_OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_NUMBER_RANGE);
					valuePtr = valuePtr.share(2);
					
					Pointer rangePtr = valuePtr;
					NotesRangeStruct range = NotesRangeStruct.newInstance(rangePtr);
					range.ListEntries = (short) (numberList.size() & 0xffff);
					range.RangeEntries = (short) (numberArrList.size() & 0xffff);
					range.write();

					Pointer doubleListPtr = rangePtr.share(NotesCAPI.rangeSize);
					
					for (int i=0; i<numberList.size(); i++) {
						doubleListPtr.setDouble(0, numberList.get(i).doubleValue());
						doubleListPtr = doubleListPtr.share(8);
					}

					Pointer doubleArrListPtr = doubleListPtr;
					
					for (int i=0; i<numberArrList.size(); i++) {
						double[] currNumberArr = numberArrList.get(i);
						
						NotesNumberPairStruct numberPair = NotesNumberPairStruct.newInstance(doubleArrListPtr);
						numberPair.Lower = currNumberArr[0];
						numberPair.Upper = currNumberArr[1];
						numberPair.write();

						doubleArrListPtr = doubleArrListPtr.share(NotesCAPI.numberPairSize);
					}
					
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_NUMBER_RANGE, (int) rethItem.getValue(),
							valueSize);
					return item;
				}
				finally {
					notesAPI.b64_OSUnlockObject(rethItem.getValue());
				}
			}
			else {
				IntByReference rethItem = new IntByReference();
				short result = notesAPI.b32_OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = notesAPI.b32_OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_NUMBER_RANGE);
					valuePtr = valuePtr.share(2);
					
					Pointer rangePtr = valuePtr;
					NotesRangeStruct range = NotesRangeStruct.newInstance(rangePtr);
					range.ListEntries = (short) (numberList.size() & 0xffff);
					range.RangeEntries = (short) (numberArrList.size() & 0xffff);
					range.write();

					Pointer doubleListPtr = rangePtr.share(NotesCAPI.rangeSize);
					
					for (int i=0; i<numberList.size(); i++) {
						doubleListPtr.setDouble(0, numberList.get(i).doubleValue());
						doubleListPtr = doubleListPtr.share(8);
					}

					Pointer doubleArrListPtr = doubleListPtr;
					
					for (int i=0; i<numberArrList.size(); i++) {
						double[] currNumberArr = numberArrList.get(i);
						
						NotesNumberPairStruct numberPair = NotesNumberPairStruct.newInstance(doubleArrListPtr);
						numberPair.Lower = currNumberArr[0];
						numberPair.Upper = currNumberArr[1];
						numberPair.write();

						doubleArrListPtr = doubleArrListPtr.share(NotesCAPI.numberPairSize);
					}
					
					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_NUMBER_RANGE, rethItem.getValue(),
							valueSize);
					return item;
				}
				finally {
					notesAPI.b32_OSUnlockObject(rethItem.getValue());
				}
			}
		}
		else if (value instanceof Calendar[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof Date[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof NotesTimeDate[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof List && isCalendarOrCalendarArrayList((List) value)) {
			List<?> calendarOrCalendarArrList = toCalendarOrCalendarArrayList((List<?>) value);
			
			List<Calendar> calendarList = new ArrayList<Calendar>();
			List<Calendar[]> calendarArrList = new ArrayList<Calendar[]>();
			
			for (int i=0; i<calendarOrCalendarArrList.size(); i++) {
				Object currObj = calendarOrCalendarArrList.get(i);
				if (currObj instanceof Calendar) {
					calendarList.add((Calendar) currObj);
				}
				else if (currObj instanceof Calendar[]) {
					calendarArrList.add((Calendar[]) currObj);
				}
			}
			
			if (calendarList.size() > 65535) {
				throw new IllegalArgumentException("Date list size must fit in a WORD ("+calendarList.size()+">65535)");
			}
			if (calendarArrList.size() > 65535) {
				throw new IllegalArgumentException("Date range list size must fit in a WORD ("+calendarArrList.size()+">65535)");
			}

			int valueSize = 2 + NotesCAPI.rangeSize + 
					8 * calendarList.size() +
					NotesCAPI.timeDatePairSize * calendarArrList.size();
			
			if (NotesJNAContext.is64Bit()) {
				LongByReference rethItem = new LongByReference();
				short result = notesAPI.b64_OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = notesAPI.b64_OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_TIME_RANGE);
					valuePtr = valuePtr.share(2);
					
					Pointer rangePtr = valuePtr;
					NotesRangeStruct range = NotesRangeStruct.newInstance(rangePtr);
					range.ListEntries = (short) (calendarList.size() & 0xffff);
					range.RangeEntries = (short) (calendarArrList.size() & 0xffff);
					range.write();

					Pointer dateListPtr = rangePtr.share(NotesCAPI.rangeSize);
					
					for (Calendar currCal : calendarList) {
						boolean hasDate = NotesDateTimeUtils.hasDate(currCal);
						boolean hasTime = NotesDateTimeUtils.hasTime(currCal);
						int[] innards = NotesDateTimeUtils.calendarToInnards(currCal, hasDate, hasTime);

						dateListPtr.setInt(0, innards[0]);
						dateListPtr = dateListPtr.share(4);
						dateListPtr.setInt(0, innards[1]);
						dateListPtr = dateListPtr.share(4);
					}
					
					Pointer rangeListPtr = dateListPtr;
					
					for (int i=0; i<calendarArrList.size(); i++) {
						Calendar[] currRangeVal = calendarArrList.get(i);
						
						boolean hasDateStart = NotesDateTimeUtils.hasDate(currRangeVal[0]);
						boolean hasTimeStart = NotesDateTimeUtils.hasTime(currRangeVal[0]);
						int[] innardsStart = NotesDateTimeUtils.calendarToInnards(currRangeVal[0], hasDateStart, hasTimeStart);

						boolean hasDateEnd = NotesDateTimeUtils.hasDate(currRangeVal[1]);
						boolean hasTimeEnd = NotesDateTimeUtils.hasTime(currRangeVal[1]);
						int[] innardsEnd = NotesDateTimeUtils.calendarToInnards(currRangeVal[1], hasDateEnd, hasTimeEnd);

						NotesTimeDateStruct timeDateStart = NotesTimeDateStruct.newInstance(innardsStart);
						NotesTimeDateStruct timeDateEnd = NotesTimeDateStruct.newInstance(innardsEnd);
						
						NotesTimeDatePairStruct timeDatePair = NotesTimeDatePairStruct.newInstance(rangeListPtr);
						timeDatePair.Lower = timeDateStart;
						timeDatePair.Upper = timeDateEnd;
						timeDatePair.write();

						rangeListPtr = rangeListPtr.share(NotesCAPI.timeDatePairSize);
					}

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TIME_RANGE, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					notesAPI.b64_OSUnlockObject(rethItem.getValue());
				}
			}
			else {
				IntByReference rethItem = new IntByReference();
				short result = notesAPI.b32_OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = notesAPI.b32_OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_TIME_RANGE);
					valuePtr = valuePtr.share(2);
					
					Pointer rangePtr = valuePtr;
					NotesRangeStruct range = NotesRangeStruct.newInstance(rangePtr);
					range.ListEntries = (short) (calendarList.size() & 0xffff);
					range.RangeEntries = (short) (calendarArrList.size() & 0xffff);
					range.write();

					Pointer dateListPtr = rangePtr.share(NotesCAPI.rangeSize);
					
					for (Calendar currCal : calendarList) {
						boolean hasDate = NotesDateTimeUtils.hasDate(currCal);
						boolean hasTime = NotesDateTimeUtils.hasTime(currCal);
						int[] innards = NotesDateTimeUtils.calendarToInnards(currCal, hasDate, hasTime);

						dateListPtr.setInt(0, innards[0]);
						dateListPtr = dateListPtr.share(4);
						dateListPtr.setInt(0, innards[1]);
						dateListPtr = dateListPtr.share(4);
					}
					
					Pointer rangeListPtr = dateListPtr;
					
					for (int i=0; i<calendarArrList.size(); i++) {
						Calendar[] currRangeVal = calendarArrList.get(i);
						
						boolean hasDateStart = NotesDateTimeUtils.hasDate(currRangeVal[0]);
						boolean hasTimeStart = NotesDateTimeUtils.hasTime(currRangeVal[0]);
						int[] innardsStart = NotesDateTimeUtils.calendarToInnards(currRangeVal[0], hasDateStart, hasTimeStart);

						boolean hasDateEnd = NotesDateTimeUtils.hasDate(currRangeVal[1]);
						boolean hasTimeEnd = NotesDateTimeUtils.hasTime(currRangeVal[1]);
						int[] innardsEnd = NotesDateTimeUtils.calendarToInnards(currRangeVal[1], hasDateEnd, hasTimeEnd);

						NotesTimeDateStruct timeDateStart = NotesTimeDateStruct.newInstance(innardsStart);
						NotesTimeDateStruct timeDateEnd = NotesTimeDateStruct.newInstance(innardsEnd);
						
						NotesTimeDatePairStruct timeDatePair = NotesTimeDatePairStruct.newInstance(rangeListPtr);
						timeDatePair.Lower = timeDateStart;
						timeDatePair.Upper = timeDateEnd;
						timeDatePair.write();

						rangeListPtr = rangeListPtr.share(NotesCAPI.timeDatePairSize);
					}

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_TIME_RANGE, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					notesAPI.b32_OSUnlockObject(rethItem.getValue());
				}
			}
		}
		else if (value instanceof double[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof int[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof float[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof long[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof Number[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof Double[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof Integer[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof Float[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof Long[]) {
			return appendItemValue(itemName, flags, Arrays.asList(value));
		}
		else if (value instanceof NotesUniversalNoteId) {
			NotesUniversalNoteIdStruct struct = ((NotesUniversalNoteId)value).getAdapter(NotesUniversalNoteIdStruct.class);

			//date type + LIST structure + UNIVERSALNOTEID
			int valueSize = 2 + 2 + 2 * NotesCAPI.timeDateSize;
			
			if (NotesJNAContext.is64Bit()) {
				LongByReference rethItem = new LongByReference();
				short result = notesAPI.b64_OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = notesAPI.b64_OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_NOTEREF_LIST);
					valuePtr = valuePtr.share(2);
					
					//LIST structure
					valuePtr.setShort(0, (short) 1);
					valuePtr = valuePtr.share(2);
					
					struct.write();
					valuePtr.write(0, struct.getAdapter(Pointer.class).getByteArray(0, 2*NotesCAPI.timeDateSize), 0, 2*NotesCAPI.timeDateSize);

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_NOTEREF_LIST, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					notesAPI.b64_OSUnlockObject(rethItem.getValue());
				}
			}
			else {
				IntByReference rethItem = new IntByReference();
				short result = notesAPI.b32_OSMemAlloc((short) 0, valueSize, rethItem);
				NotesErrorUtils.checkResult(result);
				
				Pointer valuePtr = notesAPI.b32_OSLockObject(rethItem.getValue());
				
				try {
					valuePtr.setShort(0, (short) NotesItem.TYPE_NOTEREF_LIST);
					valuePtr = valuePtr.share(2);
					
					//LIST structure
					valuePtr.setShort(0, (short) 1);
					valuePtr = valuePtr.share(2);
					
					struct.write();
					valuePtr.write(0, struct.getAdapter(Pointer.class).getByteArray(0, 2*NotesCAPI.timeDateSize), 0, 2*NotesCAPI.timeDateSize);

					NotesItem item = appendItemValue(itemName, flags, NotesItem.TYPE_NOTEREF_LIST, (int) rethItem.getValue(), valueSize);
					return item;
				}
				finally {
					notesAPI.b32_OSUnlockObject(rethItem.getValue());
				}
			}
		}
		else {
			throw new IllegalArgumentException("Unsupported value type: "+(value==null ? "null" : value.getClass().getName()));
		}
	}

	private List<?> toNumberOrNumberArrayList(List<?> list) {
		boolean allNumbers = true;
		for (int i=0; i<list.size(); i++) {
			if (!(list.get(i) instanceof double[]) && !(list.get(i) instanceof Double)) {
				allNumbers = false;
				break;
			}
		}
		
		if (allNumbers)
			return (List<?>) list;
		
		List convertedList = new ArrayList();
		for (int i=0; i<list.size(); i++) {
			if (list.get(i) instanceof Number) {
				//ok
				convertedList.add(((Number)list.get(i)).doubleValue());
			}
			else if (list.get(i) instanceof double[]) {
				if (((double[])list.get(i)).length!=2) {
					throw new IllegalArgumentException("Length of double array entry must be 2 for number ranges");
				}
				//ok
				convertedList.add((double[]) list.get(i));
			}
			else if (list.get(i) instanceof Number[]) {
				Number[] numberArr = (Number[]) list.get(i);
				if (numberArr.length!=2) {
					throw new IllegalArgumentException("Length of Number array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						numberArr[0].doubleValue(),
						numberArr[1].doubleValue()
				});
			}
			else if (list.get(i) instanceof Double[]) {
				Double[] doubleArr = (Double[]) list.get(i);
				if (doubleArr.length!=2) {
					throw new IllegalArgumentException("Length of Number array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						doubleArr[0],
						doubleArr[1]
				});
			}
			else if (list.get(i) instanceof Integer[]) {
				Integer[] integerArr = (Integer[]) list.get(i);
				if (integerArr.length!=2) {
					throw new IllegalArgumentException("Length of Integer array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						integerArr[0].doubleValue(),
						integerArr[1].doubleValue()
				});
			}
			else if (list.get(i) instanceof Long[]) {
				Long[] longArr = (Long[]) list.get(i);
				if (longArr.length!=2) {
					throw new IllegalArgumentException("Length of Long array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						longArr[0].doubleValue(),
						longArr[1].doubleValue()
				});
			}
			else if (list.get(i) instanceof Float[]) {
				Float[] floatArr = (Float[]) list.get(i);
				if (floatArr.length!=2) {
					throw new IllegalArgumentException("Length of Float array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						floatArr[0].doubleValue(),
						floatArr[1].doubleValue()
				});
			}
			else if (list.get(i) instanceof int[]) {
				int[] intArr = (int[]) list.get(i);
				if (intArr.length!=2) {
					throw new IllegalArgumentException("Length of int array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						intArr[0],
						intArr[1]
				});
			}
			else if (list.get(i) instanceof long[]) {
				long[] longArr = (long[]) list.get(i);
				if (longArr.length!=2) {
					throw new IllegalArgumentException("Length of long array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						longArr[0],
						longArr[1]
				});
			}
			else if (list.get(i) instanceof float[]) {
				float[] floatArr = (float[]) list.get(i);
				if (floatArr.length!=2) {
					throw new IllegalArgumentException("Length of float array entry must be 2 for number ranges");
				}
				
				convertedList.add(new double[] {
						floatArr[0],
						floatArr[1]
				});
			}
			else {
				throw new IllegalArgumentException("Unsupported date format found in list: "+(list.get(i)==null ? "null" : list.get(i).getClass().getName()));
			}
		}
		return convertedList;
	}
	private List<?> toCalendarOrCalendarArrayList(List<?> list) {
		boolean allCalendar = true;
		for (int i=0; i<list.size(); i++) {
			if (!(list.get(i) instanceof Calendar[]) && !(list.get(i) instanceof Calendar)) {
				allCalendar = false;
				break;
			}
		}
		
		if (allCalendar)
			return (List<?>) list;
		
		List convertedList = new ArrayList();
		for (int i=0; i<list.size(); i++) {
			if (list.get(i) instanceof Calendar) {
				//ok
				convertedList.add(list.get(i));
			}
			else if (list.get(i) instanceof Calendar[]) {
				//ok
				convertedList.add((Calendar[]) list.get(i));
			}
			else if (list.get(i) instanceof Date) {
				Calendar cal = Calendar.getInstance();
				cal.setTime((Date) list.get(i));
				convertedList.add(cal);
			}
			else if (list.get(i) instanceof NotesTimeDate) {
				Calendar cal = ((NotesTimeDate)list.get(i)).toCalendar();
				convertedList.add(cal);
			}
			else if (list.get(i) instanceof Date[]) {
				Date[] dateArr = (Date[]) list.get(i);
				if (dateArr.length!=2) {
					throw new IllegalArgumentException("Length of Date array entry must be 2 for date ranges");
				}
				Calendar val1 = Calendar.getInstance();
				val1.setTime(dateArr[0]);

				Calendar val2 = Calendar.getInstance();
				val2.setTime(dateArr[1]);

				convertedList.add(new Calendar[] {val1, val2});
			}
			else if (list.get(i) instanceof NotesTimeDate[]) {
				NotesTimeDate[] ntdArr = (NotesTimeDate[]) list.get(i);
				if (ntdArr.length!=2) {
					throw new IllegalArgumentException("Length of NotesTimeDate array entry must be 2 for date ranges");
				}
				Calendar val1 = ntdArr[0].toCalendar();
				Calendar val2 = ntdArr[1].toCalendar();
				
				convertedList.add(new Calendar[] {val1, val2});
			}
			else {
				throw new IllegalArgumentException("Unsupported date format found in list: "+(list.get(i)==null ? "null" : list.get(i).getClass().getName()));
			}
		}
		return convertedList;
	}
	
	private boolean isStringList(List<?> list) {
		if (list==null || list.isEmpty()) {
			return false;
		}
		for (int i=0; i<list.size(); i++) {
			if (!(list.get(i) instanceof String)) {
				return false;
			}
		}
		return true;
	}

	private boolean isCalendarOrCalendarArrayList(List<?> list) {
		if (list==null || list.isEmpty()) {
			return false;
		}
		for (int i=0; i<list.size(); i++) {
			boolean isAccepted=false;
			
			Object currObj = list.get(i);
			
			if (currObj instanceof Calendar[]) {
				Calendar[] calArr = (Calendar[]) currObj;
				if (calArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Date[]) {
				Date[] dateArr = (Date[]) currObj;
				if (dateArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof NotesTimeDate[]) {
				NotesTimeDate[] ndtArr = (NotesTimeDate[]) currObj;
				if (ndtArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Calendar) {
				isAccepted = true;
			}
			else if (currObj instanceof Date) {
				isAccepted = true;
			}
			else if (currObj instanceof NotesTimeDate) {
				isAccepted = true;
			}
			
			if (!isAccepted) {
				return false;
			}
		}
		return true;
	}
	
	private boolean isNumberOrNumberArrayList(List<?> list) {
		if (list==null || list.isEmpty()) {
			return false;
		}
		for (int i=0; i<list.size(); i++) {
			boolean isAccepted=false;
			
			Object currObj = list.get(i);
			
			if (currObj instanceof double[]) {
				double[] valArr = (double[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			if (currObj instanceof int[]) {
				int[] valArr = (int[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			if (currObj instanceof long[]) {
				long[] valArr = (long[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			if (currObj instanceof float[]) {
				float[] valArr = (float[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Number[]) {
				Number[] valArr = (Number[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Integer[]) {
				Integer[] valArr = (Integer[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Long[]) {
				Long[] valArr = (Long[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Double[]) {
				Double[] valArr = (Double[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Float[]) {
				Float[] valArr = (Float[]) currObj;
				if (valArr.length==2) {
					isAccepted = true;
				}
			}
			else if (currObj instanceof Number) {
				isAccepted = true;
			}
			
			if (!isAccepted) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Internal method that calls the C API method to write the item
	 * 
	 * @param itemName item name
	 * @param flags item flags
	 * @param itemType item type
	 * @param hItemValue handle to memory block with item value
	 * @param valueLength length of binary item value (without data type short)
	 */
	private NotesItem appendItemValue(String itemName, EnumSet<ItemType> flags, int itemType, int hItemValue, int valueLength) {
		checkHandle();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, false);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short flagsShort = ItemType.toBitMask(flags);
		
		NotesBlockIdStruct.ByValue valueBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		valueBlockIdByVal.pool = hItemValue;
		valueBlockIdByVal.block = 0;
		valueBlockIdByVal.write();
		
		NotesBlockIdStruct retItemBlockId = NotesBlockIdStruct.newInstance();
		retItemBlockId.pool = 0;
		retItemBlockId.block = 0;
		retItemBlockId.write();
		
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFItemAppendByBLOCKID(m_hNote64, flagsShort, itemNameMem,
					(short) (itemNameMem==null ? 0 : itemNameMem.size()), valueBlockIdByVal,
					valueLength, retItemBlockId);
		}
		else {
			result = notesAPI.b32_NSFItemAppendByBLOCKID(m_hNote32, flagsShort, itemNameMem,
					(short) (itemNameMem==null ? 0 : itemNameMem.size()), valueBlockIdByVal,
					valueLength, retItemBlockId);
		}
		NotesErrorUtils.checkResult(result);
		
		NotesItem item = new NotesItem(this, retItemBlockId, itemType, valueBlockIdByVal);
		return item;
	}
	
	/**
	 * Internal method that calls the C API method to write the item
	 * 
	 * @param itemName item name
	 * @param flags item flags
	 * @param itemType item type
	 * @param itemValue binary item value
	 * @param valueLength length of binary item value (without data type short)
	 */
//	private void appendItemx(String itemName, EnumSet<ItemType> flags, int itemType, Pointer itemValue, int valueLength) {
//		checkHandle();
//
//		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, false);
//		
//		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
//		short flagsShort = ItemType.toBitMask(flags);
//		
//		short result;
//		if (NotesJNAContext.is64Bit()) {
//			result = notesAPI.b64_NSFItemAppend(m_hNote64, flagsShort, itemNameMem, (short) 
//					(itemNameMem==null ? 0 : itemNameMem.size()), (short) (itemType & 0xffff), itemValue, valueLength);
//		}
//		else {
//			result = notesAPI.b32_NSFItemAppend(m_hNote32, flagsShort, itemNameMem, (short) 
//					(itemNameMem==null ? 0 : itemNameMem.size()), (short) (itemType & 0xffff), itemValue, valueLength);
//		}
//		NotesErrorUtils.checkResult(result);
//	}
	
	/**
	 * This function signs a document by creating a unique electronic signature and appending this
	 * signature to the note.<br>
	 * <br>
	 * A signature constitutes proof of the user's identity and serves to assure the reader that
	 * the user was the real author of the document.<br>
	 * <br>
	 * The signature is derived from the User ID. A signature item has data type {@link NotesItem#TYPE_SIGNATURE}
	 * and item flags {@link NotesCAPI#ITEM_SEAL}. The data value of the signature item is a digest
	 * of the data stored in items in the note, signed with the user's private key.<br>
	 * <br>
	 * This method signs entire document. It creates a digest of all the items in the note, and
	 * appends a signature item with field name $Signature (ITEM_NAME_NOTE_SIGNATURE).<br>
	 * <br>
	 * If the document to be signed is encrypted, this function will attempt to decrypt the
	 * document in order to generate a valid signature.<br>
	 * If you want the document to be signed and encrypted, you must sign the document
	 * {@link #copyAndEncrypt(NotesUserId, EnumSet)}.<br>
	 * <br>
	 * Note:  When the Notes user interface opens a note, it always uses the {@link OpenNote#EXPAND}
	 * option to promote items of type number to number list, and items of type text to text list.<br>
	 */
	public void sign() {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFNoteExpand(m_hNote64);
			NotesErrorUtils.checkResult(result);
			
			result = notesAPI.b64_NSFNoteSign(m_hNote64);
			NotesErrorUtils.checkResult(result);
			
			result = notesAPI.b64_NSFNoteContract(m_hNote64);
			NotesErrorUtils.checkResult(result);
			
		}
		else {
			result = notesAPI.b32_NSFNoteExpand(m_hNote32);
			NotesErrorUtils.checkResult(result);
			
			result = notesAPI.b32_NSFNoteSign(m_hNote32);
			NotesErrorUtils.checkResult(result);
			
			result = notesAPI.b32_NSFNoteContract(m_hNote32);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function signs the note using the specified ID.
	 * It allows you to pass a flag to determine how MIME parts will be signed.<br>
	 * <br>
	 * The used C method NSFNoteSignExt3 generates a signature item with the specified name and
	 * appends this signature item to the note. A signature item has data type
	 * TYPE_SIGNATURE and item flags ITEM_SEAL. The data value of the signature item is a
	 * digest of the data stored in items in the note, signed with the user's private key.<br>
	 * <br>
	 * If the note has MIME parts and the flag <code>signNotesIfMimePresent</code> is true,
	 * it will be SMIME signed, if not set it will be Notes signed.
	 * <br>
	 * If the document to be signed is encrypted, this function will attempt to decrypt the
	 * document in order to generate a valid signature.<br>
	 * If you want the document to be signed and encrypted, you must sign the document
	 * before using {@link #copyAndEncrypt(NotesUserId, EnumSet)}.<br>
	 * <br>
	 * <b>Please note:<br>
	 * As described <a href="http://www.eknori.de/2015-01-31/serious-issue-when-signing-a-database-in-the-background-on-the-server/">here</a>,
	 * it is not possible to use this method to sign XPages design elements on a server with
	 * an ID other than the server id.</b>
	 * 
	 * @param id user id to use for signing or null for current id
	 * @param signNotesIfMimePresent If the note has MIME parts and this flag is true it will be SMIME signed, if not set it will be Notes signed.
	 */
	public void sign(NotesUserId id, boolean signNotesIfMimePresent) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFNoteExpand(m_hNote64);
			NotesErrorUtils.checkResult(result);
			
			result = notesAPI.b64_NSFNoteSignExt3(m_hNote64, id==null ? 0 : id.getHandle64(), null, NotesCAPI.MAXWORD, 0, signNotesIfMimePresent ? NotesCAPI.SIGN_NOTES_IF_MIME_PRESENT : 0, 0, null);
			NotesErrorUtils.checkResult(result);
			
			result = notesAPI.b64_NSFNoteContract(m_hNote64);
			NotesErrorUtils.checkResult(result);
			
			//verify signature
			NotesTimeDateStruct retWhenSigned = NotesTimeDateStruct.newInstance();
			Memory retSigner = new Memory(NotesCAPI.MAXUSERNAME);
			Memory retCertifier = new Memory(NotesCAPI.MAXUSERNAME);

			result = notesAPI.b64_NSFNoteVerifySignature (m_hNote64, null, retWhenSigned, retSigner, retCertifier);
			NotesErrorUtils.checkResult(result);
		}
		else {
			result = notesAPI.b32_NSFNoteExpand(m_hNote32);
			NotesErrorUtils.checkResult(result);

			result = notesAPI.b32_NSFNoteSignExt3(m_hNote32, id==null ? 0 : id.getHandle32(), null, NotesCAPI.MAXWORD, 0, signNotesIfMimePresent ? NotesCAPI.SIGN_NOTES_IF_MIME_PRESENT : 0, 0, null);
			NotesErrorUtils.checkResult(result);

			result = notesAPI.b32_NSFNoteContract(m_hNote32);
			NotesErrorUtils.checkResult(result);
			
			//verify signature
			NotesTimeDateStruct retWhenSigned = NotesTimeDateStruct.newInstance();
			Memory retSigner = new Memory(NotesCAPI.MAXUSERNAME);
			Memory retCertifier = new Memory(NotesCAPI.MAXUSERNAME);

			result = notesAPI.b32_NSFNoteVerifySignature (m_hNote32, null, retWhenSigned, retSigner, retCertifier);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * Container for note signature data
	 * 
	 * @author Karsten Lehmann
	 */
	public static class SignatureData {
		private NotesTimeDate m_whenSigned;
		private String m_signer;
		private String m_certifier;
		
		private SignatureData(NotesTimeDate whenSigned, String signer, String certifier) {
			m_whenSigned = whenSigned;
			m_signer = signer;
			m_certifier = certifier;
		}
		
		public NotesTimeDate getWhenSigned() {
			return m_whenSigned;
		}
		
		public String getSigner() {
			return m_signer;
		}
		
		public String getCertifier() {
			return m_certifier;
		}
	}
	
	/**
	 * Returns the signer of a note
	 * 
	 * @return signer or empty string if not signed
	 */
	public String getSigner() {
		try {
			SignatureData signatureData = verifySignature();
			
			return signatureData.getSigner();
		}
		catch (NotesError e) {
			if (e.getId()==INotesErrorConstants.ERR_NOTE_NOT_SIGNED) {
				return "";
			}
			else {
				throw e;
			}
		}
	}
	
	/**
	 * This function verifies a signature on a note or section(s) within a note.<br>
	 * It returns an error if a signature did not verify.<br>
	 * <br>

	 * @return signer data
	 */
	public SignatureData verifySignature() {
		checkHandle();
		
		NotesTimeDateStruct retWhenSigned = NotesTimeDateStruct.newInstance();
		Memory retSigner = new Memory(NotesCAPI.MAXUSERNAME);
		Memory retCertifier = new Memory(NotesCAPI.MAXUSERNAME);

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFNoteExpand(m_hNote64);
			NotesErrorUtils.checkResult(result);

			result = notesAPI.b64_NSFNoteVerifySignature (m_hNote64, null, retWhenSigned, retSigner, retCertifier);
			NotesErrorUtils.checkResult(result);
			
			result = notesAPI.b64_NSFNoteContract(m_hNote64);
			NotesErrorUtils.checkResult(result);
		}
		else {
			result = notesAPI.b32_NSFNoteExpand(m_hNote32);
			NotesErrorUtils.checkResult(result);
			
			result = notesAPI.b32_NSFNoteVerifySignature (m_hNote32, null, retWhenSigned, retSigner, retCertifier);
			NotesErrorUtils.checkResult(result);
			
			result = notesAPI.b32_NSFNoteContract(m_hNote32);
			NotesErrorUtils.checkResult(result);
		}

		String signer = NotesStringUtils.fromLMBCS(retSigner, NotesStringUtils.getNullTerminatedLength(retSigner));
		String certifier = NotesStringUtils.fromLMBCS(retCertifier, NotesStringUtils.getNullTerminatedLength(retCertifier));
		SignatureData data = new SignatureData(new NotesTimeDate(retWhenSigned), signer, certifier);
		return data;
	}
	
	/**
	 * Creates/overwrites a $REF item pointing to another {@link NotesNote}
	 * 
	 * @param note $REF target note
	 */
	public void makeResponse(NotesNote note) {
		makeResponse(note.getUNID());
	}
	
	/**
	 * Creates/overwrites a $REF item pointing to a UNID
	 * 
	 * @param targetUnid $REF target UNID
	 */
	public void makeResponse(String targetUnid) {
		replaceItemValue("$REF", EnumSet.of(ItemType.SUMMARY), new NotesUniversalNoteId(targetUnid));
	}

	/**
	 * Callback interface that receives data of images embedded in a HTML conversion result
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IHtmlItemImageConversionCallback {
		public static enum Action {Continue, Stop};
		
		/**
		 * Reports the size of the image
		 * 
		 * @param size size
		 * @return return how many bytes to skip before reading
		 */
		public int setSize(int size);
		
		/**
		 * Implement this method to receive element data
		 * 
		 * @param data data
		 * @return action, either Continue or Stop
		 */
		public Action read(byte[] data);
	}
	
	public enum HtmlConvertOption {
		ForceSectionExpand,
		RowAtATimeTableAlt,
		ForceOutlineExpand;
		
		public static String[] toStringArray(EnumSet<HtmlConvertOption> options) {
			List<String> optionsAsStrList = new ArrayList<String>(options.size());
			for (HtmlConvertOption currOption : options) {
				optionsAsStrList.add(currOption.toString()+"=1");
			}
			return optionsAsStrList.toArray(new String[optionsAsStrList.size()]);
		}
	}
	
	/**
	 * Convenience method to read the binary data of a {@link IHtmlImageRef}
	 * 
	 * @param image image reference
	 * @param callback callback to receive the data
	 */
	public void convertHtmlElement(IHtmlImageRef image, IHtmlItemImageConversionCallback callback) {
		String itemName = image.getItemName();
		int itemIndex = image.getItemIndex();
		int itemOffset = image.getItemOffset();
		EnumSet<HtmlConvertOption> options = image.getOptions();
		
		convertHtmlElement(itemName, options, itemIndex, itemOffset, callback);
	}
	
	/**
	 * Method to access images embedded in HTML conversion result. Compute index and offset parameters
	 * from the img tag path like this: 1.3E =&gt; index=1, offset=63
	 * 
	 * @param itemName  rich text field which is being converted
	 * @param options conversion options
	 * @param itemIndex the relative item index -- if there is more than one, Item with the same pszItemName, then this indicates which one (zero relative)
	 * @param itemOffset byte offset in the Item where the element starts
	 * @param callback callback to receive the data
	 */
	public void convertHtmlElement(String itemName, EnumSet<HtmlConvertOption> options, int itemIndex, int itemOffset, IHtmlItemImageConversionCallback callback) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		IntByReference phHTML = new IntByReference();
		short result = notesAPI.HTMLCreateConverter(phHTML);
		NotesErrorUtils.checkResult(result);
		
		int hHTML = phHTML.getValue();
		
		try {
			if (!options.isEmpty()) {
				result = notesAPI.HTMLSetHTMLOptions(hHTML, new StringArray(HtmlConvertOption.toStringArray(options)));
				NotesErrorUtils.checkResult(result);
			}

			Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);

			int totalLen;
			
			int skip;
			
			if (NotesJNAContext.is64Bit()) {
				result = notesAPI.b64_HTMLConvertElement(hHTML, getParent().getHandle64(), m_hNote64, itemNameMem, itemIndex, itemOffset);
				NotesErrorUtils.checkResult(result);
				
				Memory tLenMem = new Memory(4);
				result = notesAPI.b64_HTMLGetProperty(hHTML, (long) NotesCAPI.HTMLAPI_PROP_TEXTLENGTH, tLenMem);
				NotesErrorUtils.checkResult(result);
				totalLen = tLenMem.getInt(0);
				skip = callback.setSize(totalLen);
			}
			else {
				result = notesAPI.b32_HTMLConvertElement(hHTML, getParent().getHandle32(), m_hNote32, itemNameMem, itemIndex, itemOffset);
				NotesErrorUtils.checkResult(result);
				
				Memory tLenMem = new Memory(4);
				result = notesAPI.b32_HTMLGetProperty(hHTML, (int) NotesCAPI.HTMLAPI_PROP_TEXTLENGTH, tLenMem);
				NotesErrorUtils.checkResult(result);
				totalLen = tLenMem.getInt(0);
				skip = callback.setSize(totalLen);
			}

			if (skip > totalLen)
				throw new IllegalArgumentException("Skip value cannot be greater than size: "+skip+" > "+totalLen);
			
			IntByReference len = new IntByReference();
			len.setValue(NotesCAPI.MAXPATH);
			int startOffset=skip;
			Memory bufMem = new Memory(NotesCAPI.MAXPATH+1);
			
			while (result==0 && len.getValue()>0 && startOffset<totalLen) {
				len.setValue(NotesCAPI.MAXPATH);
				
				result = notesAPI.HTMLGetText(hHTML, startOffset, len, bufMem);
				NotesErrorUtils.checkResult(result);
				
				byte[] data = bufMem.getByteArray(0, len.getValue());
				IHtmlItemImageConversionCallback.Action action = callback.read(data);
				if (action == IHtmlItemImageConversionCallback.Action.Stop)
					break;
				
				startOffset += len.getValue();
			}
		}
		finally {
			result = notesAPI.HTMLDestroyConverter(hHTML);
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * Method to convert the whole note to HTML
	 * 
	 * @param options conversion options
	 * @return conversion result
	 */
	public IHtmlConversionResult convertNoteToHtml(EnumSet<HtmlConvertOption> options) {
		return convertNoteToHtml(options, (EnumSet<ReferenceType>) null, (Map<ReferenceType,EnumSet<TargetType>>) null);
	}

	/**
	 * Method to convert the whole note to HTML with additional filters for the
	 * data returned in the conversion result
	 * 
	 * @param options conversion options
	 * @param refTypeFilter optional filter for ref types to be returned or null for no filter
	 * @param targetTypeFilter optional filter for target types to be returned or null for no filter
	 * @return conversion result
	 */
	public IHtmlConversionResult convertNoteToHtml(EnumSet<HtmlConvertOption> options,
			EnumSet<ReferenceType> refTypeFilter,
			Map<ReferenceType,EnumSet<TargetType>> targetTypeFilter) {
		return internalConvertItemToHtml(null, options, refTypeFilter, targetTypeFilter);
	}
	
	/**
	 * Method to convert a single item of this note to HTML
	 * 
	 * @param itemName item name
	 * @param options conversion options
	 * @return conversion result
	 */
	public IHtmlConversionResult convertItemToHtml(String itemName, EnumSet<HtmlConvertOption> options) {
		return convertItemToHtml(itemName, options, (EnumSet<ReferenceType>) null, (Map<ReferenceType,EnumSet<TargetType>>) null);
	}
	
	/**
	 * Method to convert a single item of this note to HTML with additional filters for the
	 * data returned in the conversion result
	 * 
	 * @param itemName item name
	 * @param options conversion options
	 * @param refTypeFilter optional filter for ref types to be returned or null for no filter
	 * @param targetTypeFilter optional filter for target types to be returned or null for no filter
	 * @return conversion result
	 */
	public IHtmlConversionResult convertItemToHtml(String itemName, EnumSet<HtmlConvertOption> options,
			EnumSet<ReferenceType> refTypeFilter,
			Map<ReferenceType,EnumSet<TargetType>> targetTypeFilter) {
		if (StringUtil.isEmpty(itemName))
			throw new NullPointerException("Item name cannot be null");
		
		return internalConvertItemToHtml(itemName, options, refTypeFilter, targetTypeFilter);
	}

	/**
	 * Implementation of {@link IHtmlConversionResult} that contains the HTML conversion result
	 * 
	 * @author Karsten Lehmann
	 */
	private class HtmlConversionResult implements IHtmlConversionResult {
		private String m_html;
		private List<IHtmlApiReference> m_references;
		private EnumSet<HtmlConvertOption> m_options;
		
		private HtmlConversionResult(String html, List<IHtmlApiReference> references, EnumSet<HtmlConvertOption> options) {
			m_html = html;
			m_references = references;
			m_options = options;
		}
		
		@Override
		public String getText() {
			return m_html;
		}

		@Override
		public List<IHtmlApiReference> getReferences() {
			return m_references;
		}
		
		private IHtmlImageRef createImageRef(final String refText, final String fieldName, final int itemIndex,
				final int itemOffset, final String format) {
			return new IHtmlImageRef() {
				
				@Override
				public void readImage(IHtmlItemImageConversionCallback callback) {
					NotesNote.this.convertHtmlElement(this, callback);
				}
				
				@Override
				public void writeImage(File f) throws IOException {
					if (f.exists() && !f.delete())
						throw new IOException("Cannot delete existing file "+f.getAbsolutePath());
						
					final FileOutputStream fOut = new FileOutputStream(f);
					final IOException[] ex = new IOException[1];
					try {
						NotesNote.this.convertHtmlElement(this, new IHtmlItemImageConversionCallback() {

							@Override
							public int setSize(int size) {
								return 0;
							}

							@Override
							public Action read(byte[] data) {
								try {
									fOut.write(data);
									return Action.Continue;
								} catch (IOException e) {
									ex[0] = e;
									return Action.Stop;
								}
							}
						});
						
						if (ex[0]!=null)
							throw ex[0];
					}
					finally {
						fOut.close();
					}
				}
				
				@Override
				public void writeImage(final OutputStream out) throws IOException {
					final IOException[] ex = new IOException[1];

					NotesNote.this.convertHtmlElement(this, new IHtmlItemImageConversionCallback() {

						@Override
						public int setSize(int size) {
							return 0;
						}

						@Override
						public Action read(byte[] data) {
							try {
								out.write(data);
								return Action.Continue;
							} catch (IOException e) {
								ex[0] = e;
								return Action.Stop;
							}
						}
					});
					
					if (ex[0]!=null)
						throw ex[0];
					
					out.flush();
				}
				
				@Override
				public String getReferenceText() {
					return refText;
				}
				
				@Override
				public EnumSet<HtmlConvertOption> getOptions() {
					return m_options;
				}
				
				@Override
				public int getItemOffset() {
					return itemOffset;
				}
				
				@Override
				public String getItemName() {
					return fieldName;
				}
				
				@Override
				public int getItemIndex() {
					return itemIndex;
				}
				
				@Override
				public String getFormat() {
					return format;
				}
			};
		}
		
		public java.util.List<com.mindoo.domino.jna.html.IHtmlImageRef> getImages() {
			List<IHtmlImageRef> imageRefs = new ArrayList<IHtmlImageRef>();
			
			for (IHtmlApiReference currRef : m_references) {
				if (currRef.getType() == ReferenceType.IMG) {
					String refText = currRef.getReferenceText();
					String format = "gif";
					int iFormatPos = refText.indexOf("FieldElemFormat=");
					if (iFormatPos!=-1) {
						String remainder = refText.substring(iFormatPos + "FieldElemFormat=".length());
						int iNextDelim = remainder.indexOf('&');
						if (iNextDelim==-1) {
							format = remainder;
						}
						else {
							format = remainder.substring(0, iNextDelim);
						}
					}
					
					IHtmlApiUrlTargetComponent<?> fieldOffsetTarget = currRef.getTargetByType(TargetType.FIELDOFFSET);
					if (fieldOffsetTarget!=null) {
						Object fieldOffsetObj = fieldOffsetTarget.getValue();
						if (fieldOffsetObj instanceof String) {
							String fieldOffset = (String) fieldOffsetObj;
							// 1.3E -> index=1, offset=63
							int iPos = fieldOffset.indexOf('.');
							if (iPos!=-1) {
								String indexStr = fieldOffset.substring(0, iPos);
								String offsetStr = fieldOffset.substring(iPos+1);
								
								int itemIndex = Integer.parseInt(indexStr, 16);
								int itemOffset = Integer.parseInt(offsetStr, 16);
								
								IHtmlApiUrlTargetComponent<?> fieldTarget = currRef.getTargetByType(TargetType.FIELD);
								if (fieldTarget!=null) {
									Object fieldNameObj = fieldTarget.getValue();
									String fieldName = (fieldNameObj instanceof String) ? (String) fieldNameObj : null;
									
									IHtmlImageRef newImgRef = createImageRef(refText, fieldName, itemIndex, itemOffset, format);
									imageRefs.add(newImgRef);
								}
							}
							
						}
					}
				}
			}
			
			return imageRefs;
		};
		
	}

	private static class HTMLApiReference implements IHtmlApiReference {
		private ReferenceType m_type;
		private String m_refText;
		private String m_fragment;
		private CommandId m_commandId;
		private List<IHtmlApiUrlTargetComponent<?>> m_targets;
		private Map<TargetType, IHtmlApiUrlTargetComponent<?>> m_targetByType;
		
		private HTMLApiReference(ReferenceType type, String refText, String fragment, CommandId commandId,
				List<IHtmlApiUrlTargetComponent<?>> targets) {
			m_type = type;
			m_refText = refText;
			m_fragment = fragment;
			m_commandId = commandId;
			m_targets = targets;
		}
		
		@Override
		public ReferenceType getType() {
			return m_type;
		}

		@Override
		public String getReferenceText() {
			return m_refText;
		}

		@Override
		public String getFragment() {
			return m_fragment;
		}

		@Override
		public CommandId getCommandId() {
			return m_commandId;
		}

		@Override
		public List<IHtmlApiUrlTargetComponent<?>> getTargets() {
			return m_targets;
		}

		@Override
		public IHtmlApiUrlTargetComponent<?> getTargetByType(TargetType type) {
			if (m_targetByType==null) {
				m_targetByType = new HashMap<TargetType, IHtmlApiUrlTargetComponent<?>>();
				if (m_targets!=null && !m_targets.isEmpty()) {
					for (IHtmlApiUrlTargetComponent<?> currTarget : m_targets) {
						m_targetByType.put(currTarget.getType(), currTarget);
					}
				}
			}
			return m_targetByType.get(type);
		}
	}
	
	private static class HtmlApiUrlTargetComponent<T> implements IHtmlApiUrlTargetComponent<T> {
		private TargetType m_type;
		private Class<T> m_valueClazz;
		private T m_value;
		
		private HtmlApiUrlTargetComponent(TargetType type, Class<T> valueClazz, T value) {
			m_type = type;
			m_valueClazz = valueClazz;
			m_value = value;
		}
		
		@Override
		public TargetType getType() {
			return m_type;
		}

		@Override
		public Class<T> getValueClass() {
			return m_valueClazz;
		}

		@Override
		public T getValue() {
			return m_value;
		}
	}

	/**
	 * Internal method doing the HTML conversion work
	 * 
	 * @param itemName item name to be converted or null for whole note
	 * @param options conversion options
	 * @param refTypeFilter optional filter for ref types to be returned or null for no filter
	 * @param targetTypeFilter optional filter for target types to be returned or null for no filter
	 * @return conversion result
	 */
	private IHtmlConversionResult internalConvertItemToHtml(String itemName,
			EnumSet<HtmlConvertOption> options, EnumSet<ReferenceType> refTypeFilter,
			Map<ReferenceType,EnumSet<TargetType>> targetTypeFilter) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		IntByReference phHTML = new IntByReference();
		short result = notesAPI.HTMLCreateConverter(phHTML);
		NotesErrorUtils.checkResult(result);
		
		int hHTML = phHTML.getValue();
		
		try {
			if (!options.isEmpty()) {
				result = notesAPI.HTMLSetHTMLOptions(hHTML, new StringArray(HtmlConvertOption.toStringArray(options)));
				NotesErrorUtils.checkResult(result);
			}

			Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
			
			int totalLen;
			
			if (NotesJNAContext.is64Bit()) {
				if (itemName==null) {
					result = notesAPI.b64_HTMLConvertNote(hHTML, getParent().getHandle64(), m_hNote64, 0, null);
					NotesErrorUtils.checkResult(result);
				}
				else {
					result = notesAPI.b64_HTMLConvertItem(hHTML, getParent().getHandle64(), m_hNote64, itemNameMem);
					NotesErrorUtils.checkResult(result);
				}
				
				Memory tLenMem = new Memory(4);
				result = notesAPI.b64_HTMLGetProperty(hHTML, (long) NotesCAPI.HTMLAPI_PROP_TEXTLENGTH, tLenMem);
				NotesErrorUtils.checkResult(result);
				totalLen = tLenMem.getInt(0);
			}
			else {
				if (itemName==null) {
					result = notesAPI.b32_HTMLConvertNote(hHTML, getParent().getHandle32(), m_hNote32, 0, null);
					NotesErrorUtils.checkResult(result);
				}
				else {
					result = notesAPI.b32_HTMLConvertItem(hHTML, getParent().getHandle32(), m_hNote32, itemNameMem);
					NotesErrorUtils.checkResult(result);
					
				}

				Memory tLenMem = new Memory(4);
				result = notesAPI.b32_HTMLGetProperty(hHTML, NotesCAPI.HTMLAPI_PROP_TEXTLENGTH, tLenMem);
				NotesErrorUtils.checkResult(result);
				totalLen = tLenMem.getInt(0);

			}
			
			IntByReference len = new IntByReference();
			len.setValue(NotesCAPI.MAXPATH);
			int startOffset=0;
			Memory textMem = new Memory(NotesCAPI.MAXPATH+1);
			
			StringBuilder htmlText = new StringBuilder();
			
			while (result==0 && len.getValue()>0 && startOffset<totalLen) {
				len.setValue(NotesCAPI.MAXPATH);
				textMem.setByte(0, (byte) 0);
				
				result = notesAPI.HTMLGetText(hHTML, startOffset, len, textMem);
				NotesErrorUtils.checkResult(result);
				
				if (result == 0) {
					textMem.setByte(len.getValue(), (byte) 0);
					
					String currText = NotesStringUtils.fromLMBCS(textMem, -1);
					htmlText.append(currText);
					
					startOffset += len.getValue();
				}
			}
			
			Memory refCount = new Memory(4);
			
			if (NotesJNAContext.is64Bit()) {
				result=notesAPI.b64_HTMLGetProperty(hHTML, NotesCAPI.HTMLAPI_PROP_NUMREFS, refCount);
			}
			else {
				result=notesAPI.b32_HTMLGetProperty(hHTML, NotesCAPI.HTMLAPI_PROP_NUMREFS, refCount);
			}
			NotesErrorUtils.checkResult(result);
			
			int iRefCount = refCount.getInt(0);

			List<IHtmlApiReference> references = new ArrayList<IHtmlApiReference>();
			
			for (int i=0; i<iRefCount; i++) {
				IntByReference phRef = new IntByReference();
				
				result = notesAPI.HTMLGetReference(hHTML, i, phRef);
				NotesErrorUtils.checkResult(result);
				
				Memory ppRef = new Memory(Pointer.SIZE);
				int hRef = phRef.getValue();
				
				result = notesAPI.HTMLLockAndFixupReference(hRef, ppRef);
				NotesErrorUtils.checkResult(result);
				try {
					int iRefType;
					Pointer pRefText;
					Pointer pFragment;
					int iCmdId;
					int nTargets;
					Pointer pTargets;
					
					//use separate structs for 64/32, because RefType uses 8 bytes on 64 and 4 bytes on 32 bit
					if (NotesJNAContext.is64Bit()) {
						HTMLAPIReference64Struct htmlApiRef = HTMLAPIReference64Struct.newInstance(ppRef.getPointer(0));
						htmlApiRef.read();
						iRefType = (int) htmlApiRef.RefType;
						pRefText = htmlApiRef.pRefText;
						pFragment = htmlApiRef.pFragment;
						iCmdId = (int) htmlApiRef.CommandId;
						nTargets = htmlApiRef.NumTargets;
						pTargets = htmlApiRef.pTargets;
					}
					else {
						HTMLAPIReference32Struct htmlApiRef = HTMLAPIReference32Struct.newInstance(ppRef.getPointer(0));
						htmlApiRef.read();
						iRefType = htmlApiRef.RefType;
						pRefText = htmlApiRef.pRefText;
						pFragment = htmlApiRef.pFragment;
						iCmdId = htmlApiRef.CommandId;
						nTargets = htmlApiRef.NumTargets;
						pTargets = htmlApiRef.pTargets;
					}

					ReferenceType refType = ReferenceType.getType((int) iRefType);
					
					if (refTypeFilter==null || refTypeFilter.contains(refType)) {
						String refText = NotesStringUtils.fromLMBCS(pRefText, -1);
						String fragment = NotesStringUtils.fromLMBCS(pFragment, -1);
						
						CommandId cmdId = CommandId.getCommandId(iCmdId);
						
						List<IHtmlApiUrlTargetComponent<?>> targets = new ArrayList<IHtmlApiUrlTargetComponent<?>>(nTargets);
						
						for (int t=0; t<nTargets; t++) {
							Pointer pCurrTarget = pTargets.share(t * NotesCAPI.htmlApiUrlComponentSize);
							HtmlApi_UrlTargetComponentStruct currTarget = HtmlApi_UrlTargetComponentStruct.newInstance(pCurrTarget);
							currTarget.read();
							
							int iTargetType = currTarget.AddressableType;
							TargetType targetType = TargetType.getType(iTargetType);
							
							EnumSet<TargetType> targetTypeFilterForRefType = targetTypeFilter==null ? null : targetTypeFilter.get(refType);
							
							if (targetTypeFilterForRefType==null || targetTypeFilterForRefType.contains(targetType)) {
								switch (currTarget.ReferenceType) {
								case NotesCAPI.URT_Name:
									currTarget.Value.setType(Pointer.class);
									currTarget.Value.read();
									String name = NotesStringUtils.fromLMBCS(currTarget.Value.name, -1);
									targets.add(new HtmlApiUrlTargetComponent(targetType, String.class, name));
									break;
								case NotesCAPI.URT_NoteId:
									currTarget.Value.setType(NoteIdStruct.class);
									currTarget.Value.read();
									NoteIdStruct noteIdStruct = currTarget.Value.nid;
									int iNoteId = noteIdStruct.nid;
									targets.add(new HtmlApiUrlTargetComponent(targetType, Integer.class, iNoteId));
									break;
								case NotesCAPI.URT_Unid:
									currTarget.Value.setType(NotesUniversalNoteIdStruct.class);
									currTarget.Value.read();
									NotesUniversalNoteIdStruct unidStruct = currTarget.Value.unid;
									unidStruct.read();
									String unid = unidStruct.toString();
									targets.add(new HtmlApiUrlTargetComponent(targetType, String.class, unid));
									break;
								case NotesCAPI.URT_None:
									targets.add(new HtmlApiUrlTargetComponent(targetType, Object.class, null));
									break;
								case NotesCAPI.URT_RepId:
									//TODO find out how to decode this one
									break;
								case NotesCAPI.URT_Special:
									//TODO find out how to decode this one
									break;
								}
							}
						}
						
						IHtmlApiReference newRef = new HTMLApiReference(refType, refText, fragment,
								cmdId, targets);
						references.add(newRef);
					}
				}
				finally {
					if (hRef!=0) {
						notesAPI.OSMemoryUnlock(hRef);
						notesAPI.OSMemoryFree(hRef);
					}
				}
			}
			
			return new HtmlConversionResult(htmlText.toString(), references, options);
		}
		finally {
			result = notesAPI.HTMLDestroyConverter(hHTML);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function can be used to create basic richtext content. It uses C API methods to create
	 * "CompoundText", which provide some high-level methods for richtext creation, e.g.
	 * to add text, doclinks, render notes as richtext or append other richtext items.<br>
	 * <br>
	 * <b>After calling the available methods in the returned {@link RichTextBuilder}, you must
	 * call {@link RichTextBuilder#close()} to write your changes into the note. Otherwise
	 * it is discarded.</b>
	 * 
	 * @param itemName item name
	 * @return richtext builder
	 */
	public RichTextBuilder createRichTextItem(String itemName) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		
		short result;
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethCompound = new LongByReference();
			result = notesAPI.b64_CompoundTextCreate(m_hNote64, itemNameMem, rethCompound);
			NotesErrorUtils.checkResult(result);
			long hCompound = rethCompound.getValue();
			RichTextBuilder ct = new RichTextBuilder(this, hCompound);
			NotesGC.__objectCreated(RichTextBuilder.class, ct);
			return ct;
		}
		else {
			IntByReference rethCompound = new IntByReference();
			result = notesAPI.b32_CompoundTextCreate(m_hNote32, itemNameMem, rethCompound);
			NotesErrorUtils.checkResult(result);
			int hCompound = rethCompound.getValue();
			RichTextBuilder ct = new RichTextBuilder(this, hCompound);
			NotesGC.__objectCreated(RichTextBuilder.class, ct);
			return ct;
		}
	}
	
	/**
	 * Callback interface to read CD record data
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface ICDRecordCallback {
		public enum Action {Continue, Stop}
		
		/**
		 * Method is called for all CD records in this item
		 * 
		 * @param data read-only bytebuffer to access data
		 * @param parsedSignature enum with converted signature WORD
		 * @param signature signature WORD for the record type
		 * @param dataLength length of data to read
		 * @param cdRecordLength total length of CD record (BSIG/WSIG/LSIG header plus <code>dataLength</code>
		 * @return action value to continue or stop
		 */
		public Action recordVisited(ByteBuffer data, CDRecord parsedSignature, short signature, int dataLength, int cdRecordLength);
		
	}
}
