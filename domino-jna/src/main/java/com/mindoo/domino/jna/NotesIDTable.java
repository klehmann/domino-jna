package com.mindoo.domino.jna;

import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.mindoo.domino.jna.NotesIDTable.IEnumerateCallback.Action;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesCAPI.IdEnumerateProc;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder.ItemTableData;
import com.mindoo.domino.jna.internal.WinNotesCAPI;
import com.mindoo.domino.jna.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

/**
 * Class wrapping the basic functionality of Notes C API ID tables
 * 
 * @author Karsten Lehmann
 */
public class NotesIDTable implements IRecyclableNotesObject {
	private int m_idTableHandle32;
	private long m_idTableHandle64;
	private boolean m_isRecycled;
	private boolean m_noRecycle;
	
	/**
	 * Creates a new ID table
	 */
	public NotesIDTable() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			LongByReference idTableHandle64 = new LongByReference();
			short noteIdLength = notesAPI.ODSLength((short) 1); //_NOTEID
			
			result = notesAPI.b64_IDCreateTable(noteIdLength, idTableHandle64);
			NotesErrorUtils.checkResult(result);
			m_idTableHandle64 = idTableHandle64.getValue();
			if (m_idTableHandle64==0) {
				throw new NotesError(0, "Null handle received for id table");
			}

		}
		else {
			IntByReference idTableHandle32 = new IntByReference();
			short noteIdLength = notesAPI.ODSLength((short) 1); //_NOTEID
			
			result = notesAPI.b32_IDCreateTable(noteIdLength, idTableHandle32);
			NotesErrorUtils.checkResult(result);
			m_idTableHandle32 = idTableHandle32.getValue();
			if (m_idTableHandle32==0) {
				throw new NotesError(0, "Null handle received for id table");
			}
		}
		NotesGC.__objectCreated(NotesIDTable.class, this);

		m_noRecycle=false;
	}

	/**
	 * Creates a new ID table and adds a list of note Ids.<br>
	 * <br>
	 * Uses a highly optimized C call internally which inserts
	 * ranges of IDs. The ID list does not have to be sorted, this is
	 * checked and done (if required) internally.
	 * 
	 * @param ids IDs to add
	 */
	public NotesIDTable(Collection<Integer> ids) {
		this();
		addNotes(ids, true);
	}
	
	/**
	 * Creates a new ID table and adds a list of note Ids.<br>
	 * <br>
	 * Uses a highly optimized C call internally which inserts
	 * ranges of IDs. The ID list does not have to be sorted, this is
	 * checked and done (if required) internally.
	 * 
	 * @param ids IDs to add
	 */
	public NotesIDTable(int [] ids) {
		this();
		List<Integer> idsAsList = new ArrayList<Integer>(ids.length);
		for (int i=0; i<ids.length; i++) {
			idsAsList.add(Integer.valueOf(ids[i]));
		}
		addNotes(idsAsList, true);
	}
	
	/**
	 * Wraps an existing ID table, 32 bit mode
	 * 
	 * @param hTable ID table handle
	 * @param noRecycle true to prevent auto-recycling (e.g. because the C API owns this id table)
	 */
	NotesIDTable(int hTable, boolean noRecycle) {
		if (NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		m_idTableHandle32 = hTable;
		m_noRecycle=noRecycle;
		if (!noRecycle) {
			NotesGC.__objectCreated(NotesIDTable.class, this);
		}
	}

	/**
	 * Wraps an existing ID table, 64 bit mode
	 * 
	 * @param hTable ID table handle
	 * @param noRecycle true to prevent auto-recycling (e.g. because the C API owns this id table)
	 */
	NotesIDTable(long hTable, boolean noRecycle) {
		if (!NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_idTableHandle64 = hTable;
		m_noRecycle=noRecycle;
		if (!noRecycle) {
			NotesGC.__objectCreated(NotesIDTable.class, this);
		}
	}
	
	public boolean isRecycled() {
		if (NotesJNAContext.is64Bit()) {
			return m_idTableHandle64==0;
		}
		else {
			return m_idTableHandle32==0;
		}
	}
	
	/**
	 * Compares this table to another table
	 * 
	 * @param table other table
	 * @return true if equal
	 */
	public boolean equalsTable(NotesIDTable table) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			return notesAPI.b64_IDAreTablesEqual(m_idTableHandle64, table.getHandle64());
		}
		else {
			return notesAPI.b32_IDAreTablesEqual(m_idTableHandle32, table.getHandle32());
		}
	}
	
	public void recycle() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (!m_noRecycle) {
			if (m_isRecycled)
				return;
			
			if (NotesJNAContext.is64Bit()) {
				if (m_idTableHandle64!=0) {
					short result = notesAPI.b64_IDDestroyTable(m_idTableHandle64);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectBeeingBeRecycled(NotesIDTable.class, this);
					m_idTableHandle64=0;
					m_isRecycled = true;
				}
			}
			else {
				if (m_idTableHandle32!=0) {
					short result = notesAPI.b32_IDDestroyTable(m_idTableHandle32);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectBeeingBeRecycled(NotesIDTable.class, this);
					m_idTableHandle32=0;
					m_isRecycled = true;
				}
			}
		}
	}
	
	public void setNoRecycle() {
		m_noRecycle=true;
	}
	
	@Override
	public boolean isNoRecycle() {
		return m_noRecycle;
	}
	
	public int getHandle32() {
		return m_idTableHandle32;
	}

	public long getHandle64() {
		return m_idTableHandle64;
	}

	private void checkHandle() {
		if (NotesJNAContext.is64Bit()) {
			if (m_idTableHandle64==0)
				throw new RuntimeException("ID table already recycled");
			if (!m_noRecycle)
				NotesGC.__b64_checkValidObjectHandle(NotesIDTable.class, m_idTableHandle64);
		}
		else {
			if (m_idTableHandle32==0)
				throw new RuntimeException("ID table already recycled");
			if (!m_noRecycle)
				NotesGC.__b32_checkValidObjectHandle(NotesIDTable.class, m_idTableHandle32);
		}
	}
	
	/**
	 * Adds a single note id to the table
	 * 
	 * @param noteId note id
	 * @return true if added
	 */
	public boolean addNote(int noteId) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		IntByReference retInserted = new IntByReference();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_IDInsert(m_idTableHandle64, noteId, retInserted);
		}
		else {
			result = notesAPI.b32_IDInsert(m_idTableHandle32, noteId, retInserted);
		}
		NotesErrorUtils.checkResult(result);
		int retInsertedAsInt = retInserted.getValue();
		return retInsertedAsInt != 0;
	}

	/**
	 * Adds the note ids of another id table to this table
	 * 
	 * @param otherTable other table to add
	 */
	public void addTable(NotesIDTable otherTable) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_IDInsertTable(m_idTableHandle64, otherTable.getHandle64());
		}
		else {
			result = notesAPI.b32_IDInsertTable(m_idTableHandle32, otherTable.getHandle32());
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * Adds a set of note ids to this id table
	 * 
	 * @param noteIds ids to add
	 */
	public void addNotes(Collection<Integer> noteIds) {
		boolean addToEnd = false;
		if (getCount()==0) {
			addToEnd = true;
		}
		
		addNotes(noteIds, addToEnd);
	}

	/**
	 * Method to add a list of note ids. Method is private to prevent
	 * wrong usage by setting <i>addToEnd</i> to true when it's not ok.
	 * 
	 * @param noteIds ids to add
	 * @param addToEnd set to true if we can <b>guarantee</b> that the ids we add are higher that the highest IDs in the table
	 */
	private void addNotes(Collection<Integer> noteIds, boolean addToEnd) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		//check if Set is already sorted
		Integer lastVal = null;
		Iterator<Integer> idsIt = noteIds.iterator();
		boolean isSorted = true;
		while (idsIt.hasNext()) {
			Integer currVal = idsIt.next();
			if (lastVal!=null && currVal!=null) {
				if (lastVal.intValue() > currVal.intValue()) {
					isSorted = false;
					break;
				}

			}
			lastVal = currVal;
		}
		Integer[] noteIdsArr = noteIds.toArray(new Integer[noteIds.size()]);
		if (!isSorted) {
			Arrays.sort(noteIdsArr);
		}
		
		LinkedList<Integer> currIdRange = new LinkedList<Integer>();
		
		//find consecutive id ranges
		for (int i=0; i<noteIdsArr.length; i++) {
			int currNoteId = noteIdsArr[i];
			if (currIdRange.isEmpty()) {
				currIdRange.add(currNoteId);
			}
			else {
				Integer highestRangeId = currIdRange.getLast();
				if (currNoteId == (highestRangeId.intValue() + 4)) {
					currIdRange.add(currNoteId);
				}
				else {
					if (currIdRange.size()==1) {
						addNote(currIdRange.getFirst());
					}
					else {
						short result;
						
						if (NotesJNAContext.is64Bit()) {
							result = notesAPI.b64_IDInsertRange(m_idTableHandle64, currIdRange.getFirst(), currIdRange.getLast(), addToEnd);
							
						}
						else {
							result = notesAPI.b32_IDInsertRange(m_idTableHandle32, currIdRange.getFirst(), currIdRange.getLast(), addToEnd);
						}
						
						NotesErrorUtils.checkResult(result);
					}
					//flush range list
					currIdRange.clear();
					currIdRange.add(currNoteId);
				}
			}
			
		}
		
		if (!currIdRange.isEmpty()) {
			if (currIdRange.size()==1) {
				addNote(currIdRange.getFirst());
			}
			else {
				short result;
				
				if (NotesJNAContext.is64Bit()) {
					result = notesAPI.b64_IDInsertRange(m_idTableHandle64, currIdRange.getFirst(), currIdRange.getLast(), addToEnd);
					
				}
				else {
					result = notesAPI.b32_IDInsertRange(m_idTableHandle32, currIdRange.getFirst(), currIdRange.getLast(), addToEnd);
				}
				
				NotesErrorUtils.checkResult(result);
			}
		}
	}
	
	/**
	 * Removes a set of note ids from this id table
	 * 
	 * @param noteIds ids to remove
	 */
	public void removeNotes(Collection<Integer> noteIds) {
		for (Integer currNoteId : noteIds) {
			removeNote(currNoteId.intValue());
		}
	}

	/**
	 * Removes the note ids of another id table from this table
	 * 
	 * @param otherTable other table to remove
	 */
	public void removeTable(NotesIDTable otherTable) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_IDDeleteTable(m_idTableHandle64, otherTable.getHandle64());
		}
		else {
			result = notesAPI.b32_IDDeleteTable(m_idTableHandle32, otherTable.getHandle32());
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * Removes a single note id from this table
	 * 
	 * @param noteId note id to remove
	 * @return true if removed
	 */
	public boolean removeNote(int noteId) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		IntByReference retDeleted = new IntByReference();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_IDDelete(m_idTableHandle64, noteId, retDeleted);
		}
		else {
			result = notesAPI.b32_IDDelete(m_idTableHandle32, noteId, retDeleted);
		}
		NotesErrorUtils.checkResult(result);
		int retDeletedAsInt = retDeleted.getValue();
		return retDeletedAsInt != 0;
	}

	/**
	 * Deletes all IDs from an ID table
	 */
	public void clear() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_IDDeleteAll(m_idTableHandle64);
		}
		else {
			result = notesAPI.b32_IDDeleteAll(m_idTableHandle32);
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * Fast replacement of this table's content with the content of another table
	 * 
	 * @param otherTable table to get the IDs from
	 * @param saveIDTableHeader true to save the ID table header (e.g. the timedate)
	 */
	public void replaceWith(NotesIDTable otherTable, boolean saveIDTableHeader) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_IDTableReplaceExtended(otherTable.getHandle64(), m_idTableHandle64, saveIDTableHeader ? NotesCAPI.IDREPLACE_SAVEDEST : 0);
		}
		else {
			result = notesAPI.b32_IDTableReplaceExtended(otherTable.getHandle32(), m_idTableHandle32, saveIDTableHeader ? NotesCAPI.IDREPLACE_SAVEDEST : 0);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Returns the this of this table in memory
	 * 
	 * @return size in bytes
	 */
	public int sizeInBytes() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			int size = notesAPI.b64_IDTableSize(m_idTableHandle64);
			return size;
		}
		else {
			int size = notesAPI.b32_IDTableSize(m_idTableHandle32);
			return size;
		}
	}
	
	@Override
	public String toString() {
		if (isRecycled()) {
			return "NotesIDTable [recycled]";
		}
		else {
			return "NotesIDTable [handle="+(NotesJNAContext.is64Bit() ? m_idTableHandle64 : m_idTableHandle32)+", "+getCount()+" entries]";
		}
	}
	
	/**
	 * Method to check whether the ID table is empty
	 * 
	 * @return true if empty
	 */
	public boolean isEmpty() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		IntByReference retID = new IntByReference();
		boolean first = true;
		boolean hasData;
		if (NotesJNAContext.is64Bit()) {
			hasData = notesAPI.b64_IDScan(m_idTableHandle64, first, retID) && retID.getValue()!=0;
		}
		else {
			hasData = notesAPI.b32_IDScan(m_idTableHandle32, first, retID) && retID.getValue()!=0;
		}
		return !hasData;
	}
	
	/**
	 * Returns the number of entries in this table
	 * 
	 * @return count
	 */
	public int getCount() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			int entries = notesAPI.b64_IDEntries(m_idTableHandle64);
			return entries;
		}
		else {
			int entries = notesAPI.b32_IDEntries(m_idTableHandle32);
			return entries;
		}
	}
	
	/**
	 * This function sets the {@link NotesTimeDate} structure that is stored in an ID Table.
	 * It is a convenience function that converts the {@link Calendar} value to {@link NotesTimeDate}
	 * and calls {@link #setTime(NotesTimeDate)}
	 * 
	 * @param cal new time
	 */
	public void setTime(Calendar cal) {
		NotesTimeDate time = NotesDateTimeUtils.calendarToTimeDate(cal);
		setTime(time);
	}
	
	/**
	 * This function sets the {@link NotesTimeDate} structure that is stored in an ID Table.
	 * This storage is reserved for the caller usage and can be used to date an ID Table for
	 * later comparison with other versions of the same ID Table.
	 * 
	 * @param time new time
	 */
	public void setTime(NotesTimeDate time) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		NotesTimeDateStruct timeStruct = time==null ? null : time.getAdapter(NotesTimeDateStruct.class);
		
		Pointer ptr;
		if (NotesJNAContext.is64Bit()) {
			ptr = notesAPI.b64_OSLockObject(m_idTableHandle64);
		}
		else {
			ptr = notesAPI.b32_OSLockObject(m_idTableHandle32);
		}
		
		try {
			int sizeInBytes;
			if (NotesJNAContext.is64Bit()) {
				sizeInBytes = notesAPI.b64_IDTableSizeP(ptr);
			}
			else {
				sizeInBytes = notesAPI.b32_IDTableSizeP(ptr);
			}
			ByteBuffer buf = ptr.getByteBuffer(0, sizeInBytes);
			notesAPI.IDTableSetTime(buf, timeStruct);
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(m_idTableHandle64);
			}
			else {
				notesAPI.b32_OSUnlockObject(m_idTableHandle32);
			}
		}
	}

	/**
	 * This function returns the {@link NotesTimeDate} structure that is stored in an ID Table,
	 * converted to {@link Calendar}
	 * 
	 * @return time
	 */
	public Calendar getTimeAsCalendar() {
		NotesTimeDate time = getTime();
		Calendar cal = time==null ? null : NotesDateTimeUtils.timeDateToCalendar(NotesDateTimeUtils.isDaylightTime(), NotesDateTimeUtils.getGMTOffset(), time);
		return cal;
	}
	
	/**
	 * This function returns the {@link NotesTimeDate} structure that is stored in an ID Table.
	 * This storage is reserved for the caller usage and can be used to date an ID Table for later
	 * comparison with other versions of the same ID Table.
	 * 
	 * @return time
	 */
	public NotesTimeDate getTime() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		Pointer ptr;
		if (NotesJNAContext.is64Bit()) {
			ptr = notesAPI.b64_OSLockObject(m_idTableHandle64);
		}
		else {
			ptr = notesAPI.b32_OSLockObject(m_idTableHandle32);
		}
		
		try {
			int sizeInBytes;
			if (NotesJNAContext.is64Bit()) {
				sizeInBytes = notesAPI.b64_IDTableSizeP(ptr);
			}
			else {
				sizeInBytes = notesAPI.b32_IDTableSizeP(ptr);
			}
			
			ByteBuffer buf = ptr.getByteBuffer(0, sizeInBytes);
			
			NotesTimeDateStruct timeStruct = notesAPI.IDTableTime(buf);
			return timeStruct==null ? null : new NotesTimeDate(timeStruct);
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(m_idTableHandle64);
			}
			else {
				notesAPI.b32_OSUnlockObject(m_idTableHandle32);
			}
		}
	}
	/**
	 * Returns the modified flag ({@link #setModified(boolean)})
	 * 
	 * @return modified flag
	 */
	public boolean isModified() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		Pointer ptr;
		if (NotesJNAContext.is64Bit()) {
			ptr = notesAPI.b64_OSLockObject(m_idTableHandle64);
		}
		else {
			ptr = notesAPI.b32_OSLockObject(m_idTableHandle32);
		}
		
		try {
			int sizeInBytes;
			if (NotesJNAContext.is64Bit()) {
				sizeInBytes = notesAPI.b64_IDTableSizeP(ptr);
			}
			else {
				sizeInBytes = notesAPI.b32_IDTableSizeP(ptr);
			}
			
			ByteBuffer buf = ptr.getByteBuffer(0, sizeInBytes);
			short flags = notesAPI.IDTableFlags(buf);
			if ((flags & NotesCAPI.IDTABLE_MODIFIED)==NotesCAPI.IDTABLE_MODIFIED) {
				return true;
			}
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(m_idTableHandle64);
			}
			else {
				notesAPI.b32_OSUnlockObject(m_idTableHandle32);
			}
		}

		return false;
	}
	
	/**
	 * Checks if this id table should be inverted (e.g. when reading from a view - all
	 * entries but the ones with ids in the table)
	 * 
	 * @return true if inverted
	 */
	public boolean isInverted() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		Pointer ptr;
		if (NotesJNAContext.is64Bit()) {
			ptr = notesAPI.b64_OSLockObject(m_idTableHandle64);
		}
		else {
			ptr = notesAPI.b32_OSLockObject(m_idTableHandle32);
		}
		
		try {
			int sizeInBytes;
			if (NotesJNAContext.is64Bit()) {
				sizeInBytes = notesAPI.b64_IDTableSizeP(ptr);
			}
			else {
				sizeInBytes = notesAPI.b32_IDTableSizeP(ptr);
			}
			
			ByteBuffer buf = ptr.getByteBuffer(0, sizeInBytes);
			short flags = notesAPI.IDTableFlags(buf);
			if ((flags & NotesCAPI.IDTABLE_INVERTED)==NotesCAPI.IDTABLE_INVERTED) {
				return true;
			}
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(m_idTableHandle64);
			}
			else {
				notesAPI.b32_OSUnlockObject(m_idTableHandle32);
			}
		}

		return false;
	}
	
	/**
	 * Sets the modified flag
	 * 
	 * @param modified true if modified
	 */
	public void setModified(boolean modified) {
		checkHandle();
		short newFlags = (short) ((isInverted() ? NotesCAPI.IDTABLE_INVERTED : 0) + (modified ? NotesCAPI.IDTABLE_MODIFIED : 0));
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Pointer ptr;
		if (NotesJNAContext.is64Bit()) {
			ptr = notesAPI.b64_OSLockObject(m_idTableHandle64);
		}
		else {
			ptr = notesAPI.b32_OSLockObject(m_idTableHandle32);
		}
		
		try {
			int sizeInBytes;
			if (NotesJNAContext.is64Bit()) {
				sizeInBytes = notesAPI.b64_IDTableSizeP(ptr);
			}
			else {
				sizeInBytes = notesAPI.b32_IDTableSizeP(ptr);
			}
			ByteBuffer buf = ptr.getByteBuffer(0, sizeInBytes);
			notesAPI.IDTableSetFlags(buf, newFlags);
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(m_idTableHandle64);
			}
			else {
				notesAPI.b32_OSUnlockObject(m_idTableHandle32);
			}
		}
	}
	
	/**
	 * Sets the inverted flag (e.g. when reading from a view - all
	 * entries but the ones with ids in the table)
	 * 
	 * @param inverted true if inverted
	 */
	public void setInverted(boolean inverted) {
		checkHandle();
		short newFlags = (short) ((isModified() ? NotesCAPI.IDTABLE_MODIFIED : 0) + (inverted ? NotesCAPI.IDTABLE_INVERTED : 0));
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Pointer ptr;
		if (NotesJNAContext.is64Bit()) {
			ptr = notesAPI.b64_OSLockObject(m_idTableHandle64);
		}
		else {
			ptr = notesAPI.b32_OSLockObject(m_idTableHandle32);
		}
		try {
			int sizeInBytes;
			if (NotesJNAContext.is64Bit()) {
				sizeInBytes = notesAPI.b64_IDTableSizeP(ptr);
			}
			else {
				sizeInBytes = notesAPI.b32_IDTableSizeP(ptr);
			}
			
			ByteBuffer buf = ptr.getByteBuffer(0, sizeInBytes);
			notesAPI.IDTableSetFlags(buf, newFlags);
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(m_idTableHandle64);
			}
			else {
				notesAPI.b32_OSUnlockObject(m_idTableHandle32);
			}
		}
	}
	
	/**
	 * Converts the content of this id table to a list of Integer
	 * 
	 * @return list
	 */
	public List<Integer> toList() {
		final List<Integer> idsAsList = new ArrayList<Integer>();
		
		enumerate(new IEnumerateCallback() {

			@Override
			public Action noteVisited(int noteId) {
				idsAsList.add(noteId);
				return Action.Continue;
			}
		});
		
		return idsAsList;
	}
	
	/**
	 * Converts the content of this id table to an array of int
	 * 
	 * @return int array
	 */
	public int[] toArray() {
		List<Integer> idsAsList = toList();
		int[] idsArr = new int[idsAsList.size()];
		
		for (int i=0; i<idsAsList.size(); i++) {
			idsArr[i] = idsAsList.get(i).intValue();
		}
		return idsArr;
	}

	/**
	 * Callback interface for ID table scanning
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IEnumerateCallback {
		public static enum Action {Continue, Stop};
		
		/**
		 * Method is called for each ID in the table
		 * 
		 * @param noteId not id
		 * @return either {@link Action#Continue} to go on scanning or {@link Action#Stop}
		 */
		public Action noteVisited(int noteId);
		
	}
	
	/**
	 * Traverses the ID table
	 * 
	 * @param callback callback is called for each ID
	 */
	public void enumerate(final IEnumerateCallback callback) {
		checkHandle();
		
		final NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		final IdEnumerateProc proc;
		if (notesAPI instanceof WinNotesCAPI) {
			proc = new WinNotesCAPI.IdEnumerateProcWin() {

				@Override
				public short invoke(Pointer parameter, int noteId) {
					Action result = callback.noteVisited(noteId);
					if (result==Action.Stop) {
						return INotesErrorConstants.ERR_CANCEL;
					}
					return 0;
				}
				
			};
		}
		else {
			proc = new IdEnumerateProc() {

				@Override
				public short invoke(Pointer parameter, int noteId) {
					Action result = callback.noteVisited(noteId);
					if (result==Action.Stop) {
						return INotesErrorConstants.ERR_CANCEL;
					}
					return 0;
				}
				
			};
		}
		
		try {
			//AccessController call required to prevent SecurityException when running in XPages
			AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {

				@Override
				public Object run() throws Exception {
					if (NotesJNAContext.is64Bit()) {
						short result = notesAPI.b64_IDEnumerate(m_idTableHandle64, proc, null);
						if (result!=INotesErrorConstants.ERR_CANCEL) {
							NotesErrorUtils.checkResult(result);
						}
					}
					else {
						short result = notesAPI.b32_IDEnumerate(m_idTableHandle32, proc, null);
						if (result!=INotesErrorConstants.ERR_CANCEL) {
							NotesErrorUtils.checkResult(result);
						}
					}
					return null;
				}
			});
		} catch (PrivilegedActionException e) {
			if (e.getCause() instanceof RuntimeException) 
				throw (RuntimeException) e.getCause();
			else
				throw new NotesError(0, "Error enumerating ID table", e);
		}
	}
	
	/**
	 * Traverses the ID table in reverse order
	 * 
	 * @param callback callback is called for each ID
	 */
	public void enumerateBackwards(IEnumerateCallback callback) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		IntByReference retID = new IntByReference();
		boolean last = true;

		if (NotesJNAContext.is64Bit()) {
			while (notesAPI.b64_IDScanBack(m_idTableHandle64, last, retID)) {
				last=false;
				Action result = callback.noteVisited(retID.getValue());
				if (result==Action.Stop) {
					return;
				}
			}
		}
		else {
			while (notesAPI.b32_IDScanBack(m_idTableHandle32, last, retID)) {
				last=false;
				Action result = callback.noteVisited(retID.getValue());
				if (result==Action.Stop) {
					return;
				}
			}
		}
	}
	
	/**
	 * Returns the last ID in the table
	 * 
	 * @return ID
	 * @throws NotesError with {@link INotesErrorConstants#ERR_IDTABLE_LENGTH_MISMATCH} if ID table is empty
	 */
	public int getLastId() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		IntByReference retID = new IntByReference();
		if (NotesJNAContext.is64Bit()) {
			if (notesAPI.b64_IDScanBack(m_idTableHandle64, true, retID)) {
				return retID.getValue();
			}
			else {
				throw new NotesError(INotesErrorConstants.ERR_IDTABLE_LENGTH_MISMATCH, "ID table is empty");
			}
		}
		else {
			if (notesAPI.b32_IDScanBack(m_idTableHandle32, true, retID)) {
				return retID.getValue();
			}
			else {
				throw new NotesError(INotesErrorConstants.ERR_IDTABLE_LENGTH_MISMATCH, "ID table is empty");
			}
		}
	}
	
	/**
	 * Returns the first ID in the table
	 * 
	 * @return ID
	 * @throws NotesError with {@link INotesErrorConstants#ERR_IDTABLE_LENGTH_MISMATCH} if ID table is empty
	 */
	public int getFirstId() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		IntByReference retID = new IntByReference();
		if (NotesJNAContext.is64Bit()) {
			if (notesAPI.b64_IDScan(m_idTableHandle64, true, retID)) {
				return retID.getValue();
			}
			else {
				throw new NotesError(INotesErrorConstants.ERR_IDTABLE_LENGTH_MISMATCH, "ID table is empty");
			}
		}
		else {
			if (notesAPI.b32_IDScan(m_idTableHandle32, true, retID)) {
				return retID.getValue();
			}
			else {
				throw new NotesError(INotesErrorConstants.ERR_IDTABLE_LENGTH_MISMATCH, "ID table is empty");
			}
		}
	}
	
	/**
	 * Checks if the table contains a note ids
	 * 
	 * @param noteId note id
	 * @return true if id exists in the table
	 */
	public boolean contains(int noteId) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			return notesAPI.b64_IDIsPresent(m_idTableHandle64, noteId);
		}
		else {
			return notesAPI.b32_IDIsPresent(m_idTableHandle32, noteId);
		}
	}
	
	/**
	 * This function creates the intersection of two ID Tables.
	 * The resulting table contains those IDs that are common to both source tables.
	 * 
	 * @param otherTable other table to intersect
	 * @return table with common note ids
	 */
	public NotesIDTable intersect(NotesIDTable otherTable) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			LongByReference retTableHandle = new LongByReference();
			result = notesAPI.b64_IDTableIntersect(m_idTableHandle64, otherTable.getHandle64(), retTableHandle);
			NotesErrorUtils.checkResult(result);
			NotesIDTable retTable = new NotesIDTable(retTableHandle.getValue(), false);
			return retTable;
		}
		else {
			IntByReference retTableHandle = new IntByReference();
			result = notesAPI.b32_IDTableIntersect(m_idTableHandle32, otherTable.getHandle32(), retTableHandle);
			NotesErrorUtils.checkResult(result);
			NotesIDTable retTable = new NotesIDTable(retTableHandle.getValue(), false);
			return retTable;
		}
	}
	
	/**
	 * This function creates the intersection of two ID Tables.
	 * The resulting table contains those IDs that are common to both source tables.
	 * 
	 * @param otherTable other table to intersect
	 * @param targetTable resulting table that will receive the ids that both tables have in common
	 */
	public void intersect(NotesIDTable otherTable, NotesIDTable targetTable) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			LongByReference retTableHandle = new LongByReference();
			retTableHandle.setValue(targetTable.getHandle64());
			result = notesAPI.b64_IDTableIntersect(m_idTableHandle64, otherTable.getHandle64(), retTableHandle);
			NotesErrorUtils.checkResult(result);
		}
		else {
			IntByReference retTableHandle = new IntByReference();
			retTableHandle.setValue(targetTable.getHandle32());
			result = notesAPI.b32_IDTableIntersect(m_idTableHandle32, otherTable.getHandle32(), retTableHandle);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * Container object with the comparison result of {@link NotesIDTable#findDifferences(NotesIDTable)}
	 * 
	 * @author Karsten Lehmann
	 */
	public static class ComparisonResult {
		private NotesIDTable m_tableAdds;
		private NotesIDTable m_tableDeletes;
		private NotesIDTable m_tableSame;
		
		public ComparisonResult(NotesIDTable tableAdds, NotesIDTable tableDeletes, NotesIDTable tableSame) {
			m_tableAdds = tableAdds;
			m_tableDeletes = tableDeletes;
			m_tableSame = tableSame;
		}
		
		/**
		 * Returns the ID table of adds
		 * 
		 * @return table
		 */
		public NotesIDTable getTableAdds() {
			return m_tableAdds;
		}

		/**
		 * Returns the ID table of deletes
		 * 
		 * @return table
		 */
		public NotesIDTable getTableDeletes() {
			return m_tableDeletes;
		}

		/**
		 * Returns the ID table of IDs that exist in both compared tables
		 * 
		 * @return table
		 */
		public NotesIDTable getTableSame() {
			return m_tableSame;
		}
		
		/**
		 * Returns if all tables are recycled
		 * 
		 * @return true if recycled
		 */
		public boolean isRecycled() {
			return m_tableAdds.isRecycled() && m_tableDeletes.isRecycled() && m_tableSame.isRecycled();
		}
		
		/**
		 * Recycles all tables. Does nothing if already recycled
		 */
		public void recycle() {
			if (!m_tableAdds.isRecycled())
				m_tableAdds.recycle();
			if (!m_tableDeletes.isRecycled())
				m_tableDeletes.recycle();
			if (!m_tableSame.isRecycled())
				m_tableSame.recycle();
		}
	}
	
	/**
	 * Compares this ID table to another one and returns which note ids need to be added or deleted and which
	 * IDs are the same in both tables
	 * 
	 * @param otherTable table to compare with
	 * @return result object with ID tables for adds, deletes and same
	 */
	public ComparisonResult findDifferences(NotesIDTable otherTable) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			LongByReference retTableAddsHandle = new LongByReference();
			LongByReference retTableDeletesHandle = new LongByReference();
			LongByReference retTableSameHandle = new LongByReference();

			result = notesAPI.b64_IDTableDifferences(m_idTableHandle64, otherTable.getHandle64(), retTableAddsHandle, retTableDeletesHandle, retTableSameHandle);
			NotesErrorUtils.checkResult(result);
			
			long hTableAdds = retTableAddsHandle.getValue();
			long hTableDeletes = retTableDeletesHandle.getValue();
			long hTableSame = retTableSameHandle.getValue();
			
			ComparisonResult compResult = new ComparisonResult(new NotesIDTable(hTableAdds, false),
					new NotesIDTable(hTableDeletes, false), new NotesIDTable(hTableSame, false));
			return compResult;
		}
		else {
			IntByReference retTableAddsHandle = new IntByReference();
			IntByReference retTableDeletesHandle = new IntByReference();
			IntByReference retTableSameHandle = new IntByReference();

			result = notesAPI.b32_IDTableDifferences(m_idTableHandle32, otherTable.getHandle32(), retTableAddsHandle, retTableDeletesHandle, retTableSameHandle);
			NotesErrorUtils.checkResult(result);
			
			int hTableAdds = retTableAddsHandle.getValue();
			int hTableDeletes = retTableDeletesHandle.getValue();
			int hTableSame = retTableSameHandle.getValue();
			
			ComparisonResult compResult = new ComparisonResult(new NotesIDTable(hTableAdds, false),
					new NotesIDTable(hTableDeletes, false), new NotesIDTable(hTableSame, false));
			return compResult;
		}
	}
	
	/**
	 * Creates a copy of this table
	 * 
	 * @return clone
	 */
	public Object clone() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		NotesIDTable clonedTable;
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethTable = new LongByReference();
			short result = notesAPI.b64_IDTableCopy(m_idTableHandle64, rethTable);
			NotesErrorUtils.checkResult(result);
			clonedTable = new NotesIDTable(rethTable.getValue(), false);
		}
		else {
			IntByReference rethTable = new IntByReference();
			short result = notesAPI.b32_IDTableCopy(m_idTableHandle32, rethTable);
			NotesErrorUtils.checkResult(result);
			clonedTable = new NotesIDTable(rethTable.getValue(), false);
		}
		return clonedTable;
	}
	
	/**
	 * Creates a new ID table with the IDs of this table, but with high order
	 * bit set (0x80000000L).
	 * 
	 * @return ID table
	 */
	public NotesIDTable withHighOrderBit() {
		List<Integer> ids = toList();
		
		for (int i=0; i<ids.size(); i++) {
			long currId = ids.get(i) | NotesCAPI.NOTEID_RESERVED;
			ids.set(i, (int) (currId & 0xffffffffL));
		}
		
		return new NotesIDTable(ids);
	}
	
	/**
	 * Filters the ID table by applying the specified selection formula on each note.<br>
	 * 
	 * @param db database to load the notes
	 * @param formula selection formula, e.g. SELECT Form="Person"
	 * @return new ID table with filter result
	 */
	public NotesIDTable filter(final NotesDatabase db, final String formula) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesIDTable>() {

			@Override
			public NotesIDTable run() {
				final Set<Integer> retIds = new TreeSet<Integer>();
				
				NotesSearch.search(db, this, formula, "-", EnumSet.of(Search.SESSION_USERNAME),
						EnumSet.of(NoteClass.DOCUMENT), null, new NotesSearch.ISearchCallback() {

					@Override
					public Action noteFound(NotesDatabase parentDb, int noteId, EnumSet<NoteClass> noteClass, NotesTimeDate dbCreated,
							NotesTimeDate noteModified, ItemTableData summaryBufferData) {
						retIds.add(noteId);
						return Action.Continue;
					}
				});
				
				NotesIDTable retIDTable = new NotesIDTable(retIds);
				return retIDTable;
			}
		});
	}
	
	/**
	 * Filters the ID table by applying the specified selection formula on each note.
	 * In constrast to {@link #filter(NotesDatabase, String)}, this method does not
	 * create a new ID table instance, but replaces the IDs in this table with the
	 * selection result.
	 * 
	 * @param db database to load the notes
	 * @param formula selection formula, e.g. SELECT Form="Person"
	 */
	public void filterInPlace(final NotesDatabase db, final String formula) {
		if (formula==null)
			return;
		
		final Set<Integer> retIds = new TreeSet<Integer>();

		AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				NotesSearch.search(db, this, formula, "", EnumSet.of(Search.SESSION_USERNAME), EnumSet.of(NoteClass.DOCUMENT), null, new NotesSearch.ISearchCallback() {

					@Override
					public Action noteFound(NotesDatabase parentDb, int noteId, EnumSet<NoteClass> noteClass, NotesTimeDate dbCreated,
							NotesTimeDate noteModified, ItemTableData summaryBufferData) {
						retIds.add(noteId);
						return Action.Continue;
					}
				});
				return null;
			}
		});
		
		clear();
		addNotes(retIds, true);
	}
	
	/**
	 * This function takes the item values in the given note and stores (stamps) them in all the notes
	 * specified by this ID Table.<br>
	 * It overwrites the current item values, thereby updating the last modified time/date of
	 * each Note processed.<br>
	 * If the item does not exist or is of a different data type, this method will create/delete the item
	 * and append the new value as required.<br>
	 * <br>
	 * If you do not have the proper access to modify any one of the notes in the ID Table,
	 * {@link INotesErrorConstants#ERR_NOACCESS} will be returned.<br>
	 * Only those documents you are able to modify have been stamped.
	 * 
	 * @param db database to load and change the notes
	 * @param templateNote note containing specified item values to stamp to the set of notes contained in the table
	 */
	public void stampAllMultiItem(NotesDatabase db, NotesNote templateNote) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (db.isRecycled())
			throw new NotesError(0, "Database already recycled");
		if (templateNote.isRecycled())
			throw new NotesError(0, "Template note already recycled");
		
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFDbStampNotesMultiItem(db.getHandle64(), m_idTableHandle64, templateNote.getHandle64());
		}
		else {
			result = notesAPI.b32_NSFDbStampNotesMultiItem(db.getHandle32(), m_idTableHandle32, templateNote.getHandle32());
		}
		NotesErrorUtils.checkResult(result);
	}
}
