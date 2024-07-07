package com.mindoo.domino.jna;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mindoo.domino.jna.NotesItem.ICompositeCallbackDirect.Action;
import com.mindoo.domino.jna.constants.CDRecordType;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.Mem;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.structs.NotesBlockIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.richtext.RichTextBuilder;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
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
	 * @param valueBlockId value block id or null if not retrieved yet
	 * @param valueLength value length in bytes
	 */
	NotesItem(NotesNote parentNote, NotesBlockIdStruct itemBlockId, int dataType,
			NotesBlockIdStruct valueBlockId) {
		m_parentNote = parentNote;
		m_itemBlockId = itemBlockId;
		m_dataType = dataType;
		m_valueBlockId = valueBlockId;
	}

	/**
	 * This function converts the input {@link NotesItem#TYPE_RFC822_TEXT} item in an open
	 * note to its pre-V5 equivalent; i.e. to {@link NotesItem#TYPE_TEXT}, {@link NotesItem#TYPE_TEXT_LIST},
	 * or {@link NotesItem#TYPE_TIME}.<br>
	 * <br>
	 * It does not update the Domino database; to update the database, call NSFNoteUpdate.<br>
	 * <br>
	 * convertRFC822TextItem converts the named input item to the appropriate pre-V5 item type.<br>
	 * <br>
	 * For example, we convert the PostedDate {@link NotesItem#TYPE_RFC822_TEXT} item to a
	 * {@link NotesItem#TYPE_TIME} item.
	 */
	public void convertRFC822TextItem() {
		m_parentNote.checkHandle();

		NotesBlockIdStruct.ByValue itemBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		itemBlockIdByVal.pool = m_itemBlockId.pool;
		itemBlockIdByVal.block = m_itemBlockId.block;

		NotesBlockIdStruct.ByValue valueBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		valueBlockIdByVal.pool = m_valueBlockId.pool;
		valueBlockIdByVal.block = m_valueBlockId.block;

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().MIMEConvertRFC822TextItemByBLOCKID(m_parentNote.getHandle64(), itemBlockIdByVal,
					valueBlockIdByVal);
		}
		else {
			result = NotesNativeAPI32.get().MIMEConvertRFC822TextItemByBLOCKID(m_parentNote.getHandle32(), itemBlockIdByVal,
					valueBlockIdByVal);
		}
		NotesErrorUtils.checkResult(result);

		//force datatype and seq number reload
		m_itemFlagsLoaded = false;
		loadItemNameAndFlags();
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
	public int getValueLength() {
		loadItemNameAndFlags();

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
	 * Returns a copy of the item raw value
	 * 
	 * @param prefixDataType true to keep the datatype WORD as prefix
	 * @return item value
	 */
	public DisposableMemory getValueRaw(boolean prefixDataType) {
		loadItemNameAndFlags();
		NotesBlockIdStruct valueBlockId = getValueBlockId();
		
		DisposableMemory mem = new DisposableMemory(prefixDataType ? m_valueLength : m_valueLength-2);
		Pointer valuePtr = Mem.OSLockObject(valueBlockId);
		try {
			byte[] valueArr = prefixDataType ? valuePtr.getByteArray(0, m_valueLength) : valuePtr.getByteArray(2, m_valueLength-2);
			mem.write(0, valueArr, 0, valueArr.length);
			return mem;
		}
		finally {
			Mem.OSUnlockObject(valueBlockId);
		}
	}
	
	/**
	 * Changes the item type.<br>
	 * <br>
	 * <b>Should only be used if you really know what you
	 * are doing</b>, e.g. when writing CD records into design elements
	 * with our {@link RichTextBuilder}
	 * to change the item type afterwards from {@link NotesItem#TYPE_COMPOSITE}
	 * to {@link NotesItem#TYPE_QUERY} or {@link NotesItem#TYPE_ACTION}
	 * 
	 * @param newType new type
	 */
	public void setItemType(int newType) {
		m_parentNote.checkHandle();

		loadItemNameAndFlags();

		NotesBlockIdStruct.ByValue itemBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		itemBlockIdByVal.pool = m_itemBlockId.pool;
		itemBlockIdByVal.block = m_itemBlockId.block;

		DisposableMemory itemValue = getValueRaw(false);
		try {
			DHANDLE docHandle = m_parentNote.getHandle();
			
			short result = NotesNativeAPI.get().NSFItemModifyValue(docHandle.getByValue(),
					itemBlockIdByVal, 
					(short) (m_itemFlags & 0xffff), (short) (newType & 0xffff), 
					itemValue, (int) itemValue.size());
			NotesErrorUtils.checkResult(result);
		}
		finally {
			itemValue.dispose();
		}
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
	 * @return sequence number (byte)
	 */
	public int getSeq() {
		loadItemNameAndFlags();

		return (int) (m_seq & 0xff);
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

		if (getType()==TYPE_COMPOSITE) {
			//Calling getValues() on a single item of TYPE_COMPOSITE does not make much sense,
			//because it may contain just a part of the whole richtext content (in
			//case the data exceeds the max segment size of a single item).
			//So here we return a Navigator for the whole richtext item instead.
			return Arrays.asList(m_parentNote.getRichtextNavigator(m_itemName));
		}
		
		int valueLength = getValueLength();
		List<Object> values = m_parentNote.getItemValue(m_itemName, m_itemBlockId, m_valueBlockId, valueLength);
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

		NotesBlockIdStruct.ByValue valueBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		valueBlockIdByVal.pool = m_valueBlockId.pool;
		valueBlockIdByVal.block = m_valueBlockId.block;

		synchronized (MAX_TEXT_ITEM_VALUE) {
			try {
				//API docs: The maximum buffer size allowed is 60K. 
				short retBufferSize = (short) (Math.min(60*1024, MAX_TEXT_ITEM_VALUE.size()) & 0xffff);

				short length;
				int valueLength = getValueLength();

				if (PlatformUtils.is64Bit()) {
					length = NotesNativeAPI64.get().NSFItemConvertValueToText((short) (m_dataType & 0xffff), valueBlockIdByVal, valueLength, MAX_TEXT_ITEM_VALUE, retBufferSize, separator);
				}
				else {
					length = NotesNativeAPI32.get().NSFItemConvertValueToText((short) (m_dataType & 0xffff), valueBlockIdByVal, valueLength, MAX_TEXT_ITEM_VALUE, retBufferSize, separator);
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

		ByteByReference retSeqByte = new ByteByReference();
		ByteByReference retDupItemID = new ByteByReference();

		Memory item_name = new Memory(NotesConstants.MAXUSERNAME);
		ShortByReference retName_len = new ShortByReference();
		ShortByReference retItem_flags = new ShortByReference();
		ShortByReference retDataType = new ShortByReference();
		IntByReference retValueLen = new IntByReference();

		NotesBlockIdStruct.ByValue itemBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		itemBlockIdByVal.pool = m_itemBlockId.pool;
		itemBlockIdByVal.block = m_itemBlockId.block;

		NotesBlockIdStruct.ByReference retValueBid = NotesBlockIdStruct.ByReference.newInstance();
		
		if (PlatformUtils.is64Bit()) {
			NotesNativeAPI64.get().NSFItemQueryEx(m_parentNote.getHandle64(),
					itemBlockIdByVal, item_name, (short) (item_name.size() & 0xffff), retName_len,
					retItem_flags, retDataType, retValueBid, retValueLen, retSeqByte, retDupItemID);
		}
		else {
			NotesNativeAPI32.get().NSFItemQueryEx(m_parentNote.getHandle32(),
					itemBlockIdByVal, item_name, (short) (item_name.size() & 0xffff), retName_len,
					retItem_flags, retDataType, retValueBid, retValueLen, retSeqByte, retDupItemID);
		}
		m_valueBlockId = retValueBid;


		m_dataType = retDataType.getValue();
		m_seq = retSeqByte.getValue();
		m_dupItemId = retDupItemID.getValue();
		m_itemFlags = (int) (retItem_flags.getValue() & 0xffff);
		m_itemName = NotesStringUtils.fromLMBCS(item_name, (int) (retName_len.getValue() & 0xffff));
		m_valueLength = retValueLen.getValue();
		m_itemFlagsLoaded = true;
	}

	public boolean isSigned() {
		loadItemNameAndFlags();

		return (m_itemFlags & NotesConstants.ITEM_SIGN) == NotesConstants.ITEM_SIGN;
	}

	public boolean isSealed() {
		loadItemNameAndFlags();

		return (m_itemFlags & NotesConstants.ITEM_SEAL) == NotesConstants.ITEM_SEAL;
	}

	public boolean isSummary() {
		loadItemNameAndFlags();

		return (m_itemFlags & NotesConstants.ITEM_SUMMARY) == NotesConstants.ITEM_SUMMARY;
	}

	public boolean isSaveToDisk() {
		loadItemNameAndFlags();

		return (m_itemFlags & NotesConstants.ITEM_NOUPDATE) == 0;
	}

	public void setSummary(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isSummary()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesConstants.ITEM_SUMMARY;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isSummary()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesConstants.ITEM_SUMMARY;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}

	public void setSaveToDisk(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isSaveToDisk()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesConstants.ITEM_NOUPDATE;
				setItemFlags((short) (newFlagsAsInt & 0xffff));

			}
		}
		else {
			if (isSaveToDisk()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesConstants.ITEM_NOUPDATE;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}

	public void setSealed(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isSealed()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesConstants.ITEM_SEAL;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isSealed()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesConstants.ITEM_SEAL;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}

	public void setSigned(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isSigned()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesConstants.ITEM_SIGN;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isSigned()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesConstants.ITEM_SIGN;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}

	public void setNames(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isNames()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesConstants.ITEM_NAMES;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isNames()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesConstants.ITEM_NAMES;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}

	public void setPlaceholder(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isPlaceholder()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesConstants.ITEM_PLACEHOLDER;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isPlaceholder()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesConstants.ITEM_PLACEHOLDER;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}

	public void setAuthors(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isAuthors()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesConstants.ITEM_READWRITERS;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isAuthors()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesConstants.ITEM_READWRITERS;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}

	public void setReaders(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isReaders()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesConstants.ITEM_READERS;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isReaders()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesConstants.ITEM_READERS;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}

	public void setProtected(boolean flag) {
		loadItemNameAndFlags();
		if (flag) {
			if (!isProtected()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt | NotesConstants.ITEM_PROTECTED;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
		else {
			if (isProtected()) {
				int flagsAsInt = m_itemFlags & 0xffff;
				int newFlagsAsInt = flagsAsInt & ~NotesConstants.ITEM_PROTECTED;
				setItemFlags((short) (newFlagsAsInt & 0xffff));
			}
		}
	}

	private void setItemFlags(short newFlags) {
		m_parentNote.checkHandle();

		loadItemNameAndFlags();

		NotesBlockIdStruct.ByValue itemBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		itemBlockIdByVal.pool = m_itemBlockId.pool;
		itemBlockIdByVal.block = m_itemBlockId.block;

		Pointer poolPtr;
		if (PlatformUtils.is64Bit()) {
			poolPtr = Mem64.OSLockObject((long) m_itemBlockId.pool);
		}
		else {
			poolPtr = Mem32.OSLockObject(m_itemBlockId.pool);
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
			if (PlatformUtils.is64Bit()) {
				Mem64.OSUnlockObject((long) m_itemBlockId.pool);
			}
			else {
				Mem32.OSUnlockObject(m_itemBlockId.pool);
			}
		}
	}

	public boolean isReadWriters() {
		loadItemNameAndFlags();

		return (m_itemFlags & NotesConstants.ITEM_READWRITERS) == NotesConstants.ITEM_READWRITERS;
	}

	public boolean isNames() {
		loadItemNameAndFlags();

		return (m_itemFlags & NotesConstants.ITEM_NAMES) == NotesConstants.ITEM_NAMES;
	}

	public boolean isPlaceholder() {
		loadItemNameAndFlags();

		return (m_itemFlags & NotesConstants.ITEM_PLACEHOLDER) == NotesConstants.ITEM_PLACEHOLDER;
	}

	public boolean isProtected() {
		loadItemNameAndFlags();

		return (m_itemFlags & NotesConstants.ITEM_PROTECTED) == NotesConstants.ITEM_PROTECTED;
	}

	public boolean isReaders() {
		loadItemNameAndFlags();

		return (m_itemFlags & NotesConstants.ITEM_READERS) == NotesConstants.ITEM_READERS;
	}

	public boolean isAuthors() {
		loadItemNameAndFlags();

		return (m_itemFlags & NotesConstants.ITEM_READWRITERS) == NotesConstants.ITEM_READWRITERS;
	}

	public boolean isUnchanged() {
		loadItemNameAndFlags();

		return (m_itemFlags & NotesConstants.ITEM_UNCHANGED) == NotesConstants.ITEM_UNCHANGED;
	}

	/**
	 * Searches all {@link #TYPE_MIME_PART} items on the note to see if
	 * the given $File item contains MIME part data.
	 * 
	 * @return true if item contains MIME part data
	 */
	public boolean isMimePart() {
		getParent().checkHandle();

		NotesBlockIdStruct.ByValue blockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		blockIdByVal.block = m_itemBlockId.block;
		blockIdByVal.pool = m_itemBlockId.pool;

		if (PlatformUtils.is64Bit()) {
			return NotesNativeAPI64.get().NSFIsFileItemMimePart(getParent().getHandle64(), blockIdByVal) == 1;
		}
		else {
			return NotesNativeAPI32.get().NSFIsFileItemMimePart(getParent().getHandle32(), blockIdByVal) == 1;
		}
	}

	/**
	 * Given a MIME part item, check it to see if its content is in a file.<br>
	 * If so, return the filename or null otherwise.
	 * 
	 * @return filename or null
	 */
	public String getFilenameIfMimePartInFile() {
		getParent().checkHandle();

		NotesBlockIdStruct.ByValue blockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		blockIdByVal.block = m_itemBlockId.block;
		blockIdByVal.pool = m_itemBlockId.pool;

		DisposableMemory retFilenameMem = new DisposableMemory(NotesConstants.MAXPATH);
		try {
			if (PlatformUtils.is64Bit()) {
				if (NotesNativeAPI64.get().NSFIsMimePartInFile(getParent().getHandle64(), blockIdByVal,
						retFilenameMem, (short) (NotesConstants.MAXPATH & 0xffff)) == 1) {

					String fileName = NotesStringUtils.fromLMBCS(retFilenameMem, -1);
					return fileName;
				}
				else {
					return null;
				}
			}
			else {
				if (NotesNativeAPI32.get().NSFIsMimePartInFile(getParent().getHandle32(), blockIdByVal,
						retFilenameMem, (short) (NotesConstants.MAXPATH & 0xffff)) == 1) {

					String fileName = NotesStringUtils.fromLMBCS(retFilenameMem, -1);
					return fileName;
				}
				else {
					return null;
				}
			}
		}
		finally {
			retFilenameMem.dispose();
		}
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

		NotesTimeDateStruct retTime = NotesTimeDateStruct.newInstance();

		NotesBlockIdStruct.ByValue itemIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		itemIdByVal.pool = m_itemBlockId.pool;
		itemIdByVal.block = m_itemBlockId.block;

		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFItemGetModifiedTimeByBLOCKID(m_parentNote.getHandle64(),
					itemIdByVal, 0, retTime);

			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFItemGetModifiedTimeByBLOCKID(m_parentNote.getHandle32(),
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
		NotesBlockIdStruct.ByValue itemBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		itemBlockIdByVal.pool = m_itemBlockId.pool;
		itemBlockIdByVal.block = m_itemBlockId.block;

		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFItemCopy(targetNote.getHandle64(), itemBlockIdByVal);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFItemCopy(targetNote.getHandle32(), itemBlockIdByVal);
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * Copies this item to another note and renames it.
	 * 
	 * @param targetNote target note
	 * @param newItemName name of the item copy
	 * @param overwrite true to overwrite an existing item in the target note; otherwise, the target note may contain multiple items with the same name
	 */
	public void copyToNote(NotesNote targetNote, String newItemName, boolean overwrite) {
		m_parentNote.checkHandle();
		targetNote.checkHandle();

		if (overwrite) {
			if (targetNote.hasItem(newItemName)) {
				targetNote.removeItem(newItemName);
			}
		}
		Memory newItemNameMem = NotesStringUtils.toLMBCS(newItemName, true);

		NotesBlockIdStruct.ByValue itemBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		itemBlockIdByVal.pool = m_itemBlockId.pool;
		itemBlockIdByVal.block = m_itemBlockId.block;

		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFItemCopyAndRename(targetNote.getHandle64(),
					itemBlockIdByVal, newItemNameMem);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFItemCopyAndRename(targetNote.getHandle32(),
					itemBlockIdByVal, newItemNameMem);
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * Removes the item from the parent note
	 */
	public void remove() {
		m_parentNote.checkHandle();

		NotesBlockIdStruct.ByValue itemBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		itemBlockIdByVal.pool = m_itemBlockId.pool;
		itemBlockIdByVal.block = m_itemBlockId.block;

		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFItemDeleteByBLOCKID(m_parentNote.getHandle64(), itemBlockIdByVal);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFItemDeleteByBLOCKID(m_parentNote.getHandle32(), itemBlockIdByVal);
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * Enumerates all CD records if a richtext item (TYPE_COMPOSITE).
	 * 
	 * @param callback callback with direct memory access via pointer
	 * @throws UnsupportedOperationException if item has the wrong type
	 */
	void enumerateCDRecords(final ICompositeCallbackDirect callback) {
		if (getType() != TYPE_COMPOSITE)
			throw new UnsupportedOperationException("Item is not of type TYPE_COMPOSITE (type found: "+getType());

		Pointer poolPtr;
		if (PlatformUtils.is64Bit()) {
			poolPtr = Mem64.OSLockObject((long) m_valueBlockId.pool);
		}
		else {
			poolPtr = Mem32.OSLockObject(m_valueBlockId.pool);
		}

		int block = (m_valueBlockId.block & 0xffff);
		long poolPtrLong = Pointer.nativeValue(poolPtr) + block;
		Pointer valuePtr = new Pointer(poolPtrLong);

		try {
			int fixedSize;

			int dwFileSize = getValueLength() - 2; //2 -> subtract data type WORD
			int dwFileOffset = 0;

			boolean aborted = false;

			while (dwFileSize>0) {
				Pointer cdRecordPtr = valuePtr.share(2 + dwFileOffset); //2 -> skip data type WORD

				//read signature WORD
				short recordType = cdRecordPtr.getShort(0);

				int dwLength;

				/* structures used to define and read the signatures 

					 0		   1
				+---------+---------+
				|   Sig   |  Length	|						Byte signature
				+---------+---------+

					 0		   1        2         3
				+---------+---------+---------+---------+
				|   Sig   |   ff    |		Length	   |		Word signature
				+---------+---------+---------+---------+

					 0		   1        2         3          4         5
				+---------+---------+---------+---------+---------+---------+
				|   Sig   |   00	    |                 Length		           | DWord signature
				+---------+---------+---------+---------+---------+---------+

				 */

				short highOrderByte = (short) (recordType & 0xFF00);

				switch (highOrderByte) {
				case NotesConstants.LONGRECORDLENGTH:      /* LSIG */
					dwLength = cdRecordPtr.share(2).getInt(0);

					fixedSize = 6; //sizeof(LSIG);

					break;

				case NotesConstants.WORDRECORDLENGTH:      /* WSIG */
					dwLength = (int) (cdRecordPtr.share(2).getShort(0) & 0xffff);

					fixedSize = 4; //sizeof(WSIG);

					break;

				default:                    /* BSIG */
					dwLength = (int) ((recordType >> 8) & 0x00ff);
					recordType &= 0x00FF; /* Length not part of signature */
					fixedSize = 2; //sizeof(BSIG);
				}

				//give direct pointer access (internal only)
				if (callback!=null) {
					if (callback.recordVisited(cdRecordPtr.share(fixedSize), recordType, dwLength-fixedSize, cdRecordPtr, dwLength) == Action.Stop) {
						aborted=true;
						break;
					}
				}

				if (dwLength>0) {
					dwFileSize -= dwLength;
					dwFileOffset += dwLength;
				}
				else {
					dwFileSize -= fixedSize;
					dwFileOffset += fixedSize;
				}

				/* If we are at an odd file offset, ignore the filler byte */

				if ((dwFileOffset & 1L)==1 && (dwFileSize > 0) ) {
					dwFileSize -= 1;            
					dwFileOffset += 1;
				}
			}

			if (!aborted && dwFileSize>0) {
				//should not happen :-)
				System.out.println("WARNING: Remaining "+dwFileSize+" bytes found at the end of the CD record item "+getName()+" of document with UNID "+m_parentNote.getUNID());
			}
		}
		finally {
			if (PlatformUtils.is64Bit()) {
				Mem64.OSUnlockObject((long) m_valueBlockId.pool);
			}
			else {
				Mem32.OSUnlockObject(m_valueBlockId.pool);
			}
		}
	}

	@Override
	public String toString() {
		if (m_parentNote.isRecycled()) {
			return "NotesItem [recycled]";
		}
		else {
			return "NotesItem [name="+getName()+", type="+getType()+", isauthors="+isAuthors()+", isreaders="+isReaders()+", isnames="+isNames()+
					", issummary="+isSummary()+"]";
		}
	}

	/**
	 * Extracts all text from the item
	 * 
	 * @return text
	 */
	String getAllCompositeTextContent() {
		StringWriter sWriter = new StringWriter();
		getAllCompositeTextContent(sWriter);
		return sWriter.toString();
	}

	/**
	 * Extracts all text from the item
	 * 
	 * @param writer writer is used to write extracted text
	 */
	void getAllCompositeTextContent(final Writer writer) {
		final boolean useOSLineBreaks = NotesStringUtils.isUseOSLineDelimiter();

		AtomicBoolean skippedFirstParagraph = new AtomicBoolean();
		
		enumerateCDRecords(new ICompositeCallbackDirect() {

			@Override
			public Action recordVisited(Pointer dataPtr, short signature, int dataLength, Pointer cdRecordPtr, int cdRecordLength) {
				if (CDRecordType.TEXT.getConstant() == signature) {
					Pointer txtPtr = dataPtr.share(4);
					int txtMemLength = dataLength-4;
					if (txtMemLength>0) {
						String txt = NotesStringUtils.fromLMBCS(txtPtr, txtMemLength);
						if (txt.length()>0) {
							try {
								writer.write(txt);
							} catch (IOException e) {
								throw new RuntimeException("Error writing composite text", e);
							}
						}
					}
				}
				else if (CDRecordType.PARAGRAPH.getConstant() == signature) {
					try {
						if (skippedFirstParagraph.get()) {
							if (PlatformUtils.isWindows() && useOSLineBreaks) {
								writer.write("\r\n");
							}
							else {
								writer.write("\n");
							}
						}
						else {
							skippedFirstParagraph.set(true);
						}
					} catch (IOException e) {
						throw new RuntimeException("Error writing composite text", e);
					}
				}
				return Action.Continue;
			}
		});
		
		try {
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException("Error writing composite text", e);
		}
	}

	/**
	 * Internal interface for direct access to memory structures
	 * 
	 * @author Karsten Lehmann
	 */
	static interface ICompositeCallbackDirect {
		public enum Action {Continue, Stop}

		/**
		 * Method is called for all CD records in this item
		 * 
		 * @param dataPtr pointer to access data
		 * @param parsedSignature list of enum values matching the signature WORD (that number is not unique, e.g. SIG_CD_VMTEXTBOX and SIG_CD_PABHIDE have the same value)
		 * @param signature signature WORD for the record type, use {@link CDRecordType#getRecordTypeForConstant(short, com.mindoo.domino.jna.constants.CDRecordType.Area)} to get an enum value for the data you are processing
		 * @param dataLength length of data to read
		 * @param cdRecordPtr pointer to CD record (header + data)
		 * @param cdRecordLength total length of CD record (BSIG/WSIG/LSIG header plus <code>dataLength</code>
		 * @return action value to continue or stop
		 */
		public Action recordVisited(Pointer dataPtr, short signature, int dataLength, Pointer cdRecordPtr, int cdRecordLength);

	}
}