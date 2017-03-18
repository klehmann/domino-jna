package com.mindoo.domino.jna;

import java.util.Calendar;
import java.util.List;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.NotesBlockIdStruct;
import com.mindoo.domino.jna.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Container for note item data
 * 
 * @author Karsten Lehmann
 */
public class NotesItem {
	/*  All datatypes below are passed to NSF in either host (machine-specific
	byte ordering and padding) or canonical form (Intel 86 packed form).
	The format of each datatype, as it is passed to and from NSF functions,
	is listed below in the comment field next to each of the data types.
	(This host/canonical issue is NOT applicable to Intel86 machines,
	because on that machine, they are the same and no conversion is required).
	On all other machines, use the ODS subroutine package to perform
	conversions of those datatypes in canonical format before they can
	be interpreted. */

	/*	"Computable" Data Types */

	public static final int TYPE_ERROR = (int)(0 + (1 << 8));
	public static final int TYPE_UNAVAILABLE = (int)(0 + (2 << 8));
	public static final int TYPE_TEXT = (int)(0 + (5 << 8));
	public static final int TYPE_TEXT_LIST = (int)(1 + (5 << 8));
	public static final int TYPE_NUMBER = (int)(0 + (3 << 8));
	public static final int TYPE_NUMBER_RANGE = (int)(1 + (3 << 8));
	public static final int TYPE_TIME = (int)(0 + (4 << 8));
	public static final int TYPE_TIME_RANGE = (int)(1 + (4 << 8));
	public static final int TYPE_FORMULA = (int)(0 + (6 << 8));
	public static final int TYPE_USERID = (int)(0 + (7 << 8));

	/*	"Non-Computable" Data Types */

	public static final int TYPE_SIGNATURE = (int)(8 + (0 << 8));
	public static final int TYPE_ACTION = (int)(16 + (0 << 8));
	public static final int TYPE_WORKSHEET_DATA = (int)(13 + (0 << 8));
	public static final int TYPE_VIEWMAP_LAYOUT = (int)(19 + (0 << 8));
	public static final int TYPE_SEAL2 = (int)(31 + (0 << 8));
	public static final int TYPE_LSOBJECT = (int)(20 + (0 << 8));
	public static final int TYPE_ICON = (int)(6 + (0 << 8));
	public static final int TYPE_VIEW_FORMAT = (int)(5 + (0 << 8));
	public static final int TYPE_SCHED_LIST = (int)(22 + (0 << 8));
	public static final int TYPE_VIEWMAP_DATASET = (int)(18 + (0 << 8));
	public static final int TYPE_SEAL = (int)(9 + (0 << 8));
	public static final int TYPE_MIME_PART = (int)(25 + (0 << 8));
	public static final int TYPE_SEALDATA = (int)(10 + (0 << 8));
	public static final int TYPE_NOTELINK_LIST = (int)(7 + (0 << 8));
	public static final int TYPE_COLLATION = (int)(2 + (0 << 8));
	public static final int TYPE_RFC822_TEXT = (int)(2 + (5 << 8));
	public static final int TYPE_COMPOSITE = (int)(1 + (0 << 8));
	public static final int TYPE_OBJECT = (int)(3 + (0 << 8));
	public static final int TYPE_HTML = (int)(21 + (0 << 8));
	public static final int TYPE_ASSISTANT_INFO = (int)(17 + (0 << 8));
	public static final int TYPE_HIGHLIGHTS = (int)(12 + (0 << 8));
	public static final int TYPE_NOTEREF_LIST = (int)(4 + (0 << 8));
	public static final int TYPE_QUERY = (int)(15 + (0 << 8));
	public static final int TYPE_USERDATA = (int)(14 + (0 << 8));
	public static final int TYPE_INVALID_OR_UNKNOWN = (int)(0 + (0 << 8));
	public static final int TYPE_SEAL_LIST = (int)(11 + (0 << 8));
	public static final int TYPE_CALENDAR_FORMAT = (int)(24 + (0 << 8));

	private NotesNote m_parentNote;
	private boolean m_itemFlagsLoaded;
	private int m_itemFlags;
	private byte m_seq;
	private byte m_dupItemId;
	private int m_dataType;
	private int m_valueLength;
	private String m_itemName;
	
	private NotesBlockIdStruct m_itemBlockId;
	private NotesBlockIdStruct m_valueBlockId;
	
	/**
	 * Creates a new item object
	 * 
	 * @param parentNote parent note
	 * @param itemBlockId item block id
	 * @param dataType data type
	 * @param valueBlockId value block id
	 * @param valueLength value length in bytes
	 */
	NotesItem(NotesNote parentNote, NotesBlockIdStruct itemBlockId, int dataType,
			NotesBlockIdStruct valueBlockId, int valueLength) {
		m_parentNote = parentNote;
		m_itemBlockId = itemBlockId;
		m_dataType = dataType;
		m_valueBlockId = valueBlockId;
		m_valueLength = valueLength;
	}

	/**
	 * Returns the parent note for this item
	 * 
	 * @return note
	 */
	public NotesNote getParent() {
		return m_parentNote;
	}
	
	/**
	 * Returns the item block id to read item meta data
	 * 
	 * @return item block id
	 */
	NotesBlockIdStruct getItemBlockId() {
		return m_itemBlockId;
	}
	
	/**
	 * Returns the value block id to lock the value in memory and decode it
	 * 
	 * @return value block id
	 */
	NotesBlockIdStruct getValueBlockId() {
		return m_valueBlockId;
	}
	
	/**
	 * Returns the value length in bytes
	 * 
	 * @return length
	 */
	int getValueLength() {
		return m_valueLength;
	}
	
	/**
	 * Returns the item name
	 * 
	 * @return item name
	 */
	public String getName() {
		loadItemNameAndFlags();
		
		return m_itemName;
	}
	
	/**
	 * Returns the data type, e.g. {@link #TYPE_TEXT} or {@link #TYPE_NUMBER}
	 * 
	 * @return type
	 */
	public int getType() {
		return m_dataType;
	}
	
	/**
	 * Returns false if the item's type is {@link #TYPE_UNAVAILABLE}
	 * 
	 * @return true if available
	 */
	public boolean isAvailable() {
		return m_dataType!=TYPE_UNAVAILABLE;
	}
	
	/**
	 * Returns the sequence number of the item
	 * 
	 * @return sequence number
	 */
	public int getSeq() {
		loadItemNameAndFlags();
		
		return m_seq;
	}
	
	/**
	 * Returna the Duplicate item ID
	 * 
	 * @return id
	 */
	public int getDupItemId() {
		loadItemNameAndFlags();
		
		return m_dupItemId;
	}
	
	/**
	 * Decodes the item value(s). The data is always returned as a list even though
	 * the list may contain only one element (e.g. for {@link #TYPE_TEXT}.
	 * <br>
	 * We currently support the following data types:<br>
	 * <ul>
	 * <li>{@link #TYPE_TEXT} - List with String object</li>
	 * <li>{@link #TYPE_TEXT_LIST} - List of String objects</li>
	 * <li>{@link #TYPE_NUMBER} - List with Double object</li>
	 * <li>{@link #TYPE_NUMBER_RANGE} - List of Double objects</li>
	 * <li>{@link #TYPE_TIME} - List with Calendar object</li>
	 * <li>{@link #TYPE_TIME_RANGE} - List of Calendar objects</li>
	 * <li>{@link #TYPE_OBJECT} with the subtype Attachment (e.g. $File items) - List with {@link NotesAttachment} object</li>
	 * <li>{@link #TYPE_NOTEREF_LIST} - List with one {@link NotesUniversalNoteId} object</li>
	 * <li>{@link #TYPE_UNAVAILABLE} - returns an empty list; might e.g. be returned by {@link NotesDatabase#getNotes(int[], java.util.EnumSet[], int[], java.util.EnumSet, NotesDatabase, com.mindoo.domino.jna.NotesDatabase.IGetNotesCallback, com.mindoo.domino.jna.NotesDatabase.INoteOpenCallback, com.mindoo.domino.jna.NotesDatabase.IObjectAllocCallback, com.mindoo.domino.jna.NotesDatabase.IObjectWriteCallback, NotesTimeDate, com.mindoo.domino.jna.NotesDatabase.IFolderAddCallback)} with to high sequence numbers</li>
	 * </ul>
	 * Other data types may be read via {@link #getValueAsText(char)} or native support may be added at
	 * a later time.
	 * 
	 * @return item value(s)
	 */
	public List<Object> getValues() {
		loadItemNameAndFlags();
		
		List<Object> values = m_parentNote.getItemValue(m_itemName, m_itemBlockId, m_valueBlockId, m_valueLength);
		return values;
	}

	//shared memory buffer for text item values
	private static Memory MAX_TEXT_ITEM_VALUE = new Memory(65535);
	static {
		MAX_TEXT_ITEM_VALUE.clear();
	}

	/**
	 * Converts the value of the item to a string
	 * 
	 * @param separator separator character for multivalue fields
	 * @return text, not null
	 */
	public String getValueAsText(char separator) {
		m_parentNote.checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		NotesBlockIdStruct.ByValue valueBlockIdByVal = new NotesBlockIdStruct.ByValue();
		valueBlockIdByVal.pool = m_valueBlockId.pool;
		valueBlockIdByVal.block = m_valueBlockId.block;

		synchronized (MAX_TEXT_ITEM_VALUE) {
			try {
				//API docs: The maximum buffer size allowed is 60K. 
				short retBufferSize = (short) (Math.min(60*1024, MAX_TEXT_ITEM_VALUE.size()) & 0xffff);

				short length;

				if (NotesJNAContext.is64Bit()) {
					length = notesAPI.b64_NSFItemConvertValueToText((short) (m_dataType & 0xffff), valueBlockIdByVal, m_valueLength, MAX_TEXT_ITEM_VALUE, retBufferSize, separator);
				}
				else {
					length = notesAPI.b32_NSFItemConvertValueToText((short) (m_dataType & 0xffff), valueBlockIdByVal, m_valueLength, MAX_TEXT_ITEM_VALUE, retBufferSize, separator);
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
	 * Calls NSFItemQueryEx to read the items name and data
	 */
	private void loadItemNameAndFlags() {
		if (m_itemFlagsLoaded) {
			return;
		}
		
		m_parentNote.checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		ByteByReference retSeqByte = new ByteByReference();
		ByteByReference retDupItemID = new ByteByReference();
		
		Memory item_name = new Memory(NotesCAPI.MAXUSERNAME);
		ShortByReference retName_len = new ShortByReference();
		ShortByReference retItem_flags = new ShortByReference();
		ShortByReference retDataType = new ShortByReference();
		IntByReference retValueLen = new IntByReference();
		
		NotesBlockIdStruct.ByValue itemBlockIdByVal = new NotesBlockIdStruct.ByValue();
		itemBlockIdByVal.pool = m_itemBlockId.pool;
		itemBlockIdByVal.block = m_itemBlockId.block;
		
		if (NotesJNAContext.is64Bit()) {
			NotesBlockIdStruct retValueBid = new NotesBlockIdStruct();
			
			notesAPI.b64_NSFItemQueryEx(m_parentNote.getHandle64(),
					itemBlockIdByVal, item_name, (short) (item_name.size() & 0xffff), retName_len,
					retItem_flags, retDataType, retValueBid, retValueLen, retSeqByte, retDupItemID);
			
			
		}
		else {
			NotesBlockIdStruct retValueBid = new NotesBlockIdStruct();

			notesAPI.b32_NSFItemQueryEx(m_parentNote.getHandle32(),
					itemBlockIdByVal, item_name, (short) (item_name.size() & 0xffff), retName_len,
					retItem_flags, retDataType, retValueBid, retValueLen, retSeqByte, retDupItemID);
		}
		
		m_dataType = retDataType.getValue();
		m_seq = retSeqByte.getValue();
		m_dupItemId = retDupItemID.getValue();
		m_itemFlags = (int) (retItem_flags.getValue() & 0xffff);
		m_itemName = NotesStringUtils.fromLMBCS(item_name, (int) (retName_len.getValue() & 0xffff));
		m_itemFlagsLoaded = true;
	}
	
	public boolean isSigned() {
		loadItemNameAndFlags();
		
		return (m_itemFlags & NotesCAPI.ITEM_SIGN) == NotesCAPI.ITEM_SIGN;
	}
	
	public boolean isSealed() {
		loadItemNameAndFlags();
		
		return (m_itemFlags & NotesCAPI.ITEM_SEAL) == NotesCAPI.ITEM_SEAL;
	}
	
	public boolean isSummary() {
		loadItemNameAndFlags();
		
		return (m_itemFlags & NotesCAPI.ITEM_SUMMARY) == NotesCAPI.ITEM_SUMMARY;
	}
	
	public void setSummary(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isSummary()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesCAPI.ITEM_SUMMARY;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isSummary()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesCAPI.ITEM_SUMMARY;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}
	
	public void setSealed(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isSealed()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesCAPI.ITEM_SEAL;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isSealed()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesCAPI.ITEM_SEAL;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}
	
	public void setSigned(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isSigned()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesCAPI.ITEM_SIGN;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isSigned()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesCAPI.ITEM_SIGN;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}

	public void setNames(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isNames()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesCAPI.ITEM_NAMES;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isNames()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesCAPI.ITEM_NAMES;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}

	public void setPlaceholder(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isPlaceholder()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesCAPI.ITEM_PLACEHOLDER;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isPlaceholder()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesCAPI.ITEM_PLACEHOLDER;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}
	
	public void setAuthors(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isAuthors()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesCAPI.ITEM_READWRITERS;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isAuthors()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesCAPI.ITEM_READWRITERS;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}
	
	public void setReaders(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isReaders()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesCAPI.ITEM_READERS;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isReaders()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesCAPI.ITEM_READERS;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}

	public void setProtected(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isProtected()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesCAPI.ITEM_PROTECTED;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isProtected()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesCAPI.ITEM_PROTECTED;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}
	
	private void setItemFlags(short newFlags) {
		m_parentNote.checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		loadItemNameAndFlags();

		NotesBlockIdStruct.ByValue itemBlockIdByVal = new NotesBlockIdStruct.ByValue();
		itemBlockIdByVal.pool = m_itemBlockId.pool;
		itemBlockIdByVal.block = m_itemBlockId.block;

		Pointer poolPtr;
		if (NotesJNAContext.is64Bit()) {
			poolPtr = notesAPI.b64_OSLockObject((long) m_itemBlockId.pool);
		}
		else {
			poolPtr = notesAPI.b32_OSLockObject(m_itemBlockId.pool);
		}
		
		int block = (int) (m_itemBlockId.block & 0xffff);
		long poolPtrLong = Pointer.nativeValue(poolPtr) + block;
		Pointer itemFlagsPtr = new Pointer(poolPtrLong).share(16);
		
		try {
			short oldFlags = itemFlagsPtr.getShort(0);
			if (oldFlags==m_itemFlags) {
				itemFlagsPtr.setShort(0, newFlags);
				m_itemFlagsLoaded = false;
			}
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject((long) m_itemBlockId.pool);
			}
			else {
				notesAPI.b32_OSUnlockObject(m_itemBlockId.pool);
			}
		}
	}
	
	public boolean isReadWriters() {
		loadItemNameAndFlags();
		
		return (m_itemFlags & NotesCAPI.ITEM_READWRITERS) == NotesCAPI.ITEM_READWRITERS;
	}
	
	public boolean isNames() {
		loadItemNameAndFlags();
		
		return (m_itemFlags & NotesCAPI.ITEM_NAMES) == NotesCAPI.ITEM_NAMES;
	}
	
	public boolean isPlaceholder() {
		loadItemNameAndFlags();
		
		return (m_itemFlags & NotesCAPI.ITEM_PLACEHOLDER) == NotesCAPI.ITEM_PLACEHOLDER;
	}
	
	public boolean isProtected() {
		loadItemNameAndFlags();
		
		return (m_itemFlags & NotesCAPI.ITEM_PROTECTED) == NotesCAPI.ITEM_PROTECTED;
	}
	
	public boolean isReaders() {
		loadItemNameAndFlags();
		
		return (m_itemFlags & NotesCAPI.ITEM_READERS) == NotesCAPI.ITEM_READERS;
	}
	
	public boolean isAuthors() {
		loadItemNameAndFlags();
		
		return (m_itemFlags & NotesCAPI.ITEM_READWRITERS) == NotesCAPI.ITEM_READWRITERS;
	}
	
	public boolean isUnchanged() {
		loadItemNameAndFlags();
		
		return (m_itemFlags & NotesCAPI.ITEM_UNCHANGED) == NotesCAPI.ITEM_UNCHANGED;
	}
	
	/**
	 * Use this function to read the last date/time this item was modified as {@link Calendar}.<br>
	 * <br>
	 * If the item has not been modified since creation, the method returns null.
	 * 
	 * @return time date value or null
	 * @throws NotesError with error code 546 in case the item does not exist
	 */
	public Calendar getModifiedDateTime() {
		m_parentNote.checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		NotesTimeDateStruct retTime = NotesTimeDateStruct.newInstance();
		
		NotesBlockIdStruct.ByValue itemIdByVal = new NotesBlockIdStruct.ByValue();
		itemIdByVal.pool = m_itemBlockId.pool;
		itemIdByVal.block = m_itemBlockId.block;
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFItemGetModifiedTimeByBLOCKID(m_parentNote.getHandle64(),
					itemIdByVal, 0, retTime);
			
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFItemGetModifiedTimeByBLOCKID(m_parentNote.getHandle32(),
					itemIdByVal, 0, retTime);
			
			NotesErrorUtils.checkResult(result);
		}
		retTime.read();
		return retTime.toCalendar();
	}
	
	/**
	 * Copies this item to another note
	 * 
	 * @param targetNote target note
	 * @param overwrite true to overwrite an existing item in the target note; otherwise, the target note may contain multiple items with the same name
	 */
	public void copyToNote(NotesNote targetNote, boolean overwrite) {
		m_parentNote.checkHandle();
		targetNote.checkHandle();

		if (overwrite) {
			String itemName = getName();
			if (targetNote.hasItem(itemName)) {
				targetNote.removeItem(itemName);
			}
		}
		NotesBlockIdStruct.ByValue itemBlockIdByVal = new NotesBlockIdStruct.ByValue();
		itemBlockIdByVal.pool = m_itemBlockId.pool;
		itemBlockIdByVal.block = m_itemBlockId.block;
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFItemCopy(targetNote.getHandle64(), itemBlockIdByVal);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFItemCopy(targetNote.getHandle32(), itemBlockIdByVal);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * Removes the item from the parent note
	 */
	public void remove() {
		m_parentNote.checkHandle();
		
		NotesBlockIdStruct.ByValue itemBlockIdByVal = new NotesBlockIdStruct.ByValue();
		itemBlockIdByVal.pool = m_itemBlockId.pool;
		itemBlockIdByVal.block = m_itemBlockId.block;

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFItemDeleteByBLOCKID(m_parentNote.getHandle64(), itemBlockIdByVal);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFItemDeleteByBLOCKID(m_parentNote.getHandle32(), itemBlockIdByVal);
			NotesErrorUtils.checkResult(result);
		}
	}
}