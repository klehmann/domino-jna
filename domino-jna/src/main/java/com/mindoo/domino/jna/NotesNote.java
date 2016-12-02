package com.mindoo.domino.jna;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import com.mindoo.domino.jna.NotesNote.IItemCallback.Action;
import com.mindoo.domino.jna.constants.Compression;
import com.mindoo.domino.jna.constants.UpdateNote;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.errors.UnsupportedItemValueError;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesCAPI.b32_CWFErrorProc;
import com.mindoo.domino.jna.internal.NotesCAPI.b64_CWFErrorProc;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.internal.WinNotesCAPI;
import com.mindoo.domino.jna.structs.NotesBlockId;
import com.mindoo.domino.jna.structs.NotesCDField;
import com.mindoo.domino.jna.structs.NotesFileObject;
import com.mindoo.domino.jna.structs.NotesObjectDescriptor;
import com.mindoo.domino.jna.structs.NotesOriginatorId;
import com.mindoo.domino.jna.structs.NotesTimeDate;
import com.mindoo.domino.jna.utils.LegacyAPIUtils;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

import lotus.domino.Database;
import lotus.domino.Document;

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
	 * Converts a legacy {@link lotus.domino.Document} to a
	 * {@link NotesNote}.
	 * 
	 * @param parentDb parent database
	 * @param doc document to convert
	 * @return note
	 */
	public static NotesNote toNote(NotesDatabase parentDb, Document doc) {
		long handle = LegacyAPIUtils.getHandle(doc);
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
		NotesOriginatorId oid = new NotesOriginatorId(retOid);
		oid.read();
		String unid = oid.getUNIDAsString();
		return unid;
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
		NotesTimeDate td = new NotesTimeDate(retModified);
		td.read();
		Calendar cal = td.toCalendar();
		return cal;
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
		NotesTimeDate td = new NotesTimeDate(retModified);
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
		NotesTimeDate td = new NotesTimeDate(retModified);
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
	public void updateExtended(EnumSet<UpdateNote> updateFlags) {
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
	 * This function only supports the set of 16-bit WORD options described in the entry {@link UpdateNote};
	 * to use the extended 32-bit DWORD options, use the function {@link #updateExtended(EnumSet)}.<br>
	 * <br>
	 * You should also consider updating the collections associated with other Views in a database
	 * via the function {@link NotesDatabase#openCollection(String, int, EnumSet)}
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
	 * 	Note: if you have enabled IMAP on a database, in the case of the
	 * special NoteID "NOTEID_ADD_OR_REPLACE", a new note is always created.
	 * 
	 * @param updateFlags flags
	 */
	public void update(EnumSet<UpdateNote> updateFlags) {
		checkHandle();
		
		short updateFlagsBitmask = UpdateNote.toBitMaskForUpdate(updateFlags);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFNoteUpdate(m_hNote64, updateFlagsBitmask);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFNoteUpdate(m_hNote32, updateFlagsBitmask);
			NotesErrorUtils.checkResult(result);
		}
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
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFItemSetTime(m_hNote64, itemNameMem, timeDate);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFItemSetTime(m_hNote32, itemNameMem, timeDate);
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
	 * @return double value
	 */
	public long getItemValueLong(String itemName) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, true);
		
		LongByReference number_item_default = new LongByReference();
		number_item_default.setValue(0);
		
		if (NotesJNAContext.is64Bit()) {
			long longVal = notesAPI.b64_NSFItemGetLong(m_hNote64, itemNameMem, number_item_default);
			return longVal;
		}
		else {
			long longVal = notesAPI.b32_NSFItemGetLong(m_hNote32, itemNameMem, number_item_default);
			return longVal;
		}
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
		
		NotesTimeDate td_item_value = new NotesTimeDate();
		
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
	List<Object> getItemValue(String itemName, NotesBlockId itemBlockId, NotesBlockId valueBlockId, int valueLength) {
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
	 * @param NotesBlockId itemBlockId item block id
	 * @param valuePtr pointer to the item value
	 * @param valueLength item value length plus 2 bytes for the data type WORD
	 * @return item value as list
	 */
	List<Object> getItemValue(String itemName, NotesBlockId itemBlockId, Pointer valuePtr, int valueLength) {
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
			List<Object> textList = ItemDecoder.decodeTextListValue(notesAPI, valueDataPtr, valueDataLength, false);
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
			
			List<Object> calendarValues = ItemDecoder.decodeTimeDateList(notesAPI, valueDataPtr, valueDataLength, useDayLight, gmtOffset);
			return calendarValues==null ? Collections.emptyList() : calendarValues;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_OBJECT) {
			NotesObjectDescriptor objDescriptor = new NotesObjectDescriptor(valueDataPtr);
			objDescriptor.read();
			
			if (objDescriptor.ObjectType == NotesCAPI.OBJECT_FILE) {
				Pointer fileObjectPtr = valueDataPtr;
				
				NotesFileObject fileObject = new NotesFileObject(fileObjectPtr);
				fileObject.read();
				
				short compressionType = fileObject.CompressionType;
				NotesTimeDate fileCreated = fileObject.FileCreated;
				NotesTimeDate fileModified = fileObject.FileModified;
				short fileNameLength = fileObject.FileNameLength;
				int fileSize = fileObject.FileSize;
				short flags = fileObject.Flags;
				fileObject.HostType = fileObject.HostType;
				
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
						fileCreated, fileModified, this,
						itemBlockId);
				
				return Arrays.asList((Object) attInfo);
			}
			//TODO add support for other object types
			return Arrays.asList((Object) objDescriptor);
		}
		else {
			throw new UnsupportedItemValueError("Data type for value of item "+itemName+" is currently unsupported: "+dataTypeAsInt);
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
		NotesBlockId valueBlockId = item.getValueBlockId();
		
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
		
		NotesBlockId.ByReference itemBlockId = new NotesBlockId.ByReference();
		NotesBlockId.ByReference valueBlockId = new NotesBlockId.ByReference();
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
		
		NotesBlockId itemBlockIdClone = new NotesBlockId();
		itemBlockIdClone.pool = itemBlockId.pool;
		itemBlockIdClone.block = itemBlockId.block;
		itemBlockIdClone.write();
		
		NotesBlockId valueBlockIdClone = new NotesBlockId();
		valueBlockIdClone.pool = valueBlockId.pool;
		valueBlockIdClone.block = valueBlockId.block;
		valueBlockIdClone.write();
		
		int dataType = retDataType.getValue();
		
		NotesItem itemInfo = new NotesItem(this, itemBlockIdClone, dataType,
				valueBlockIdClone, retValueLen.getValue());
		
		Action action = callback.itemFound(itemInfo);
		if (action != Action.Continue) {
			return;
		}
		
		while (true) {
			IntByReference retNextValueLen = new IntByReference();
			
			NotesBlockId.ByValue itemBlockIdByVal = new NotesBlockId.ByValue();
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

			itemBlockIdClone = new NotesBlockId();
			itemBlockIdClone.pool = itemBlockId.pool;
			itemBlockIdClone.block = itemBlockId.block;
			itemBlockIdClone.write();
			
			valueBlockIdClone = new NotesBlockId();
			valueBlockIdClone.pool = valueBlockId.pool;
			valueBlockIdClone.block = valueBlockId.block;
			valueBlockIdClone.write();
			
			dataType = retDataType.getValue();

			itemInfo = new NotesItem(this, itemBlockIdClone, dataType,
					valueBlockIdClone, retValueLen.getValue());
			
			action = callback.itemFound(itemInfo);
			if (action != Action.Continue) {
				return;
			}
		}
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
			byte sealed = signed_flag_ptr.getValue();
			return sealed == 1;
		}
		else {
			notesAPI.b32_NSFNoteIsSignedOrSealed(m_hNote32, signed_flag_ptr, sealed_flag_ptr);
			byte sealed = signed_flag_ptr.getValue();
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
		NotesCDField cdField = new NotesCDField(ptrCDField);
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
	
	private void computeWithForm(boolean continueOnError, final ComputeWithFormCallback callback) {
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
}
