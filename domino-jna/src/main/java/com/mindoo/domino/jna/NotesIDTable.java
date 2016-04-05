package com.mindoo.domino.jna;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Set;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.NotesTimeDate;
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
	private IntByReference m_idTableHandle32;
	private LongByReference m_idTableHandle64;
	private boolean m_isRecycled;
	private boolean m_noRecycle;
	
	/**
	 * Creates a new ID table
	 */
	public NotesIDTable() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			m_idTableHandle64 = new LongByReference();
			result = notesAPI.b64_IDCreateTable(0, m_idTableHandle64);
			NotesErrorUtils.checkResult(result);
		}
		else {
			m_idTableHandle32 = new IntByReference();
			result = notesAPI.b32_IDCreateTable(0, m_idTableHandle32);
			NotesErrorUtils.checkResult(result);
		}
		NotesGC.__objectCreated(this);
		
		m_noRecycle=false;
	}

	/**
	 * Wraps an existing ID table, 32 bit mode
	 * 
	 * @param hTable ID table handle
	 */
	public NotesIDTable(IntByReference hTable) {
		if (NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		m_idTableHandle32 = hTable;
		m_noRecycle=true;
	}

	/**
	 * Wraps an existing ID table, 64 bit mode
	 * 
	 * @param hTable ID table handle
	 */
	public NotesIDTable(LongByReference hTable) {
		if (!NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_idTableHandle64 = hTable;
		m_noRecycle=true;
	}
	
	public boolean isRecycled() {
		if (NotesJNAContext.is64Bit()) {
			return m_idTableHandle64==null || m_idTableHandle64.getValue()==0;
		}
		else {
			return m_idTableHandle32==null || m_idTableHandle32.getValue()==0;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		recycle();
	}
	
	/**
	 * Compares this table to another table
	 * 
	 * @param table other table
	 * @return true if equal
	 */
	public boolean equalTable(NotesIDTable table) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			return notesAPI.b64_IDAreTablesEqual(m_idTableHandle64.getValue(), table.getHandle64());
		}
		else {
			return notesAPI.b64_IDAreTablesEqual(m_idTableHandle32.getValue(), table.getHandle32());
		}
	}
	
	public void recycle() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (!m_noRecycle) {
			if (m_isRecycled)
				return;
			
			if (NotesJNAContext.is64Bit()) {
				long hTable=m_idTableHandle64.getValue();
				if (hTable!=0) {
					short result = notesAPI.b64_IDDestroyTable(hTable);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectRecycled(this);
					m_idTableHandle64.setValue(0);
					m_isRecycled = true;
				}
			}
			else {
				int hTable=m_idTableHandle32.getValue();
				if (hTable!=0) {
					short result = notesAPI.b32_IDDestroyTable(hTable);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectRecycled(this);
					m_idTableHandle32.setValue(0);
					m_isRecycled = true;
				}
			}
		}
	}
	
	public void setNoRecycle() {
		m_noRecycle=true;
	}
	
	public int getHandle32() {
		return m_idTableHandle32==null ? 0 : m_idTableHandle32.getValue();
	}

	public long getHandle64() {
		return m_idTableHandle64==null ? 0 : m_idTableHandle64.getValue();
	}

	private void checkHandle() {
		if (NotesJNAContext.is64Bit()) {
			if (m_idTableHandle64.getValue()==0)
				throw new RuntimeException("ID table already recycled");
			if (!m_noRecycle)
				NotesGC.__b64_checkValidHandle(getHandle64());
		}
		else {
			if (m_idTableHandle32.getValue()==0)
				throw new RuntimeException("ID table already recycled");
			if (!m_noRecycle)
				NotesGC.__b32_checkValidHandle(getHandle32());
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
			result = notesAPI.b64_IDInsert(m_idTableHandle64.getValue(), noteId, retInserted);
		}
		else {
			result = notesAPI.b32_IDInsert(m_idTableHandle32.getValue(), noteId, retInserted);
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
			result = notesAPI.b64_IDInsertTable(m_idTableHandle64.getValue(), otherTable.getHandle64());
		}
		else {
			result = notesAPI.b32_IDInsertTable(m_idTableHandle32.getValue(), otherTable.getHandle32());
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * Adds a set of note ids to this id table
	 * 
	 * @param noteIds ids to add
	 */
	public void addNotes(Set<Integer> noteIds) {
		for (Integer currNoteId : noteIds) {
			addNote(currNoteId.intValue());
		}
	}

	/**
	 * Removes a set of note ids from this id table
	 * 
	 * @param noteIds ids to remove
	 */
	public void removeNotes(Set<Integer> noteIds) {
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
			result = notesAPI.b64_IDDeleteTable(m_idTableHandle64.getValue(), otherTable.getHandle64());
		}
		else {
			result = notesAPI.b32_IDDeleteTable(m_idTableHandle32.getValue(), otherTable.getHandle32());
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
			result = notesAPI.b64_IDDelete(m_idTableHandle64.getValue(), noteId, retDeleted);
		}
		else {
			result = notesAPI.b32_IDDelete(m_idTableHandle32.getValue(), noteId, retDeleted);
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
			result = notesAPI.b64_IDDeleteAll(m_idTableHandle64.getValue());
		}
		else {
			result = notesAPI.b32_IDDeleteAll(m_idTableHandle32.getValue());
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
			int size = notesAPI.b64_IDTableSize(m_idTableHandle64.getValue());
			return size;
		}
		else {
			int size = notesAPI.b32_IDTableSize(m_idTableHandle32.getValue());
			return size;
		}
	}
	
	/**
	 * Returns the number of entries in this table
	 * 
	 * @return count
	 */
	public int count() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			int entries = notesAPI.b64_IDEntries(m_idTableHandle64.getValue());
			return entries;
		}
		else {
			int entries = notesAPI.b32_IDEntries(m_idTableHandle32.getValue());
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
		
		int sizeBytes = sizeInBytes();
		Pointer ptr;
		if (NotesJNAContext.is64Bit()) {
			ptr = notesAPI.b64_OSLockObject(m_idTableHandle64.getValue());
		}
		else {
			ptr = notesAPI.b32_OSLockObject(m_idTableHandle32.getValue());
		}
		
		try {
			ByteBuffer buf = ptr.getByteBuffer(0, sizeBytes);
			notesAPI.IDTableSetTime(buf, time);
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(m_idTableHandle64.getValue());
				if (!m_noRecycle) {
					notesAPI.b64_OSMemFree(m_idTableHandle64.getValue());
				}
			}
			else {
				notesAPI.b32_OSUnlockObject(m_idTableHandle32.getValue());
				if (!m_noRecycle) {
					notesAPI.b32_OSMemFree(m_idTableHandle32.getValue());
				}
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
		
		int sizeBytes = sizeInBytes();
		Pointer ptr;
		if (NotesJNAContext.is64Bit()) {
			ptr = notesAPI.b64_OSLockObject(m_idTableHandle64.getValue());
		}
		else {
			ptr = notesAPI.b32_OSLockObject(m_idTableHandle32.getValue());
		}
		
		try {
			ByteBuffer buf = ptr.getByteBuffer(0, sizeBytes);
			NotesTimeDate time = notesAPI.IDTableTime(buf);
			return time;
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(m_idTableHandle64.getValue());
				if (!m_noRecycle) {
					notesAPI.b64_OSMemFree(m_idTableHandle64.getValue());
				}
			}
			else {
				notesAPI.b32_OSUnlockObject(m_idTableHandle32.getValue());
				if (!m_noRecycle) {
					notesAPI.b32_OSMemFree(m_idTableHandle32.getValue());
				}
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
		
		int sizeBytes = sizeInBytes();
		Pointer ptr;
		if (NotesJNAContext.is64Bit()) {
			ptr = notesAPI.b64_OSLockObject(m_idTableHandle64.getValue());
		}
		else {
			ptr = notesAPI.b32_OSLockObject(m_idTableHandle32.getValue());
		}
		
		try {
			ByteBuffer buf = ptr.getByteBuffer(0, sizeBytes);
			short flags = notesAPI.IDTableFlags(buf);
			if ((flags & NotesCAPI.IDTABLE_MODIFIED)==NotesCAPI.IDTABLE_MODIFIED) {
				return true;
			}
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(m_idTableHandle64.getValue());
				if (!m_noRecycle) {
					notesAPI.b64_OSMemFree(m_idTableHandle64.getValue());
				}
			}
			else {
				notesAPI.b32_OSUnlockObject(m_idTableHandle32.getValue());
				if (!m_noRecycle) {
					notesAPI.b32_OSMemFree(m_idTableHandle32.getValue());
				}
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
		
		int sizeBytes = sizeInBytes();
		Pointer ptr;
		if (NotesJNAContext.is64Bit()) {
			ptr = notesAPI.b64_OSLockObject(m_idTableHandle64.getValue());
		}
		else {
			ptr = notesAPI.b32_OSLockObject(m_idTableHandle32.getValue());
		}
		
		try {
			ByteBuffer buf = ptr.getByteBuffer(0, sizeBytes);
			short flags = notesAPI.IDTableFlags(buf);
			if ((flags & NotesCAPI.IDTABLE_INVERTED)==NotesCAPI.IDTABLE_INVERTED) {
				return true;
			}
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(m_idTableHandle64.getValue());
				if (!m_noRecycle)
					notesAPI.b64_OSMemFree(m_idTableHandle64.getValue());
			}
			else {
				notesAPI.b32_OSUnlockObject(m_idTableHandle32.getValue());
				if (!m_noRecycle)
					notesAPI.b32_OSMemFree(m_idTableHandle32.getValue());
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
		int sizeBytes = sizeInBytes();
		Pointer ptr;
		if (NotesJNAContext.is64Bit()) {
			ptr = notesAPI.b64_OSLockObject(m_idTableHandle64.getValue());
		}
		else {
			ptr = notesAPI.b32_OSLockObject(m_idTableHandle32.getValue());
		}
		
		try {
			ByteBuffer buf = ptr.getByteBuffer(0, sizeBytes);
			notesAPI.IDTableSetFlags(buf, newFlags);
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(m_idTableHandle64.getValue());
				if (!m_noRecycle)
					notesAPI.b64_OSMemFree(m_idTableHandle64.getValue());
			}
			else {
				notesAPI.b32_OSUnlockObject(m_idTableHandle32.getValue());
				if (!m_noRecycle)
					notesAPI.b32_OSMemFree(m_idTableHandle32.getValue());
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
		int sizeBytes = sizeInBytes();
		Pointer ptr;
		if (NotesJNAContext.is64Bit()) {
			ptr = notesAPI.b64_OSLockObject(m_idTableHandle64.getValue());
		}
		else {
			ptr = notesAPI.b32_OSLockObject(m_idTableHandle32.getValue());
		}
		try {
			ByteBuffer buf = ptr.getByteBuffer(0, sizeBytes);
			notesAPI.IDTableSetFlags(buf, newFlags);
		}
		finally {
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_OSUnlockObject(m_idTableHandle64.getValue());
				if (!m_noRecycle)
					notesAPI.b64_OSMemFree(m_idTableHandle64.getValue());
			}
			else {
				notesAPI.b32_OSUnlockObject(m_idTableHandle32.getValue());
				if (!m_noRecycle)
					notesAPI.b32_OSMemFree(m_idTableHandle32.getValue());
			}
		}
	}
	
	/**
	 * Converts the content of this id table to an array of int
	 * 
	 * @return int array
	 */
	public int[] toArray() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		int[] ids = new int[count()];
		IntByReference retID = new IntByReference();
		boolean first = true;
		int cnt = 0;
		if (NotesJNAContext.is64Bit()) {
			while (notesAPI.b64_IDScan(m_idTableHandle64.getValue(), first, retID)) {
				first=false;
				ids[cnt] = retID.getValue();
				cnt++;
			}
		}
		else {
			while (notesAPI.b32_IDScan(m_idTableHandle32.getValue(), first, retID)) {
				first=false;
				ids[cnt] = retID.getValue();
				cnt++;
			}
		}
		return ids;
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
			return notesAPI.b64_IDIsPresent(m_idTableHandle64.getValue(), noteId);
		}
		else {
			return notesAPI.b64_IDIsPresent(m_idTableHandle32.getValue(), noteId);
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
			result = notesAPI.b64_IDTableIntersect(m_idTableHandle64.getValue(), otherTable.getHandle64(), retTableHandle);
			NotesErrorUtils.checkResult(result);
			NotesIDTable retTable = new NotesIDTable(retTableHandle);
			return retTable;
		}
		else {
			IntByReference retTableHandle = new IntByReference();
			result = notesAPI.b32_IDTableIntersect(m_idTableHandle32.getValue(), otherTable.getHandle32(), retTableHandle);
			NotesErrorUtils.checkResult(result);
			NotesIDTable retTable = new NotesIDTable(retTableHandle);
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
			result = notesAPI.b64_IDTableIntersect(m_idTableHandle64.getValue(), otherTable.getHandle64(), retTableHandle);
			NotesErrorUtils.checkResult(result);
		}
		else {
			IntByReference retTableHandle = new IntByReference();
			retTableHandle.setValue(targetTable.getHandle32());
			result = notesAPI.b32_IDTableIntersect(m_idTableHandle32.getValue(), otherTable.getHandle32(), retTableHandle);
			NotesErrorUtils.checkResult(result);
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
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethTable = new LongByReference();
			short result = notesAPI.b64_IDTableCopy(m_idTableHandle64.getValue(), rethTable);
			NotesErrorUtils.checkResult(result);
			return new NotesIDTable(rethTable);
		}
		else {
			IntByReference rethTable = new IntByReference();
			short result = notesAPI.b32_IDTableCopy(m_idTableHandle32.getValue(), rethTable);
			NotesErrorUtils.checkResult(result);
			return new NotesIDTable(rethTable);
		}
	}
}
