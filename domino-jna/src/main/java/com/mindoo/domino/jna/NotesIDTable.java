package com.mindoo.domino.jna;

import java.nio.ByteBuffer;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
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
	private boolean m_noRecycle;
	
	/**
	 * Creates a new ID table
	 */
	public NotesIDTable() {
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		short result;
		if (NotesContext.is64Bit()) {
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
		if (NotesContext.is64Bit())
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
		if (!NotesContext.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_idTableHandle64 = hTable;
		m_noRecycle=true;
	}
	
	public boolean isRecycled() {
		if (NotesContext.is64Bit()) {
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
	
	public boolean equalTable(NotesIDTable table) {
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		if (NotesContext.is64Bit()) {
			return notesAPI.b64_IDAreTablesEqual(m_idTableHandle64.getValue(), table.getHandle64());
		}
		else {
			return notesAPI.b64_IDAreTablesEqual(m_idTableHandle32.getValue(), table.getHandle32());
		}
	}
	
	public void recycle() {
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		if (!m_noRecycle) {
			if (NotesContext.is64Bit()) {
				long hTable=m_idTableHandle64.getValue();
				if (hTable!=0) {
					short result = notesAPI.b64_IDDestroyTable(hTable);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectRecycled(this);
					m_idTableHandle64.setValue(0);
				}
			}
			else {
				int hTable=m_idTableHandle32.getValue();
				if (hTable!=0) {
					short result = notesAPI.b32_IDDestroyTable(hTable);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectRecycled(this);
					m_idTableHandle32.setValue(0);
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
		if (NotesContext.is64Bit()) {
			if (m_idTableHandle64.getValue()==0)
				throw new RuntimeException("ID table already recycled");
		}
		else {
			if (m_idTableHandle32.getValue()==0)
				throw new RuntimeException("ID table already recycled");
		}
	}
	
	public boolean addNote(int noteId) {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		IntByReference retInserted = new IntByReference();
		short result;
		if (NotesContext.is64Bit()) {
			result = notesAPI.b64_IDInsert(m_idTableHandle64.getValue(), noteId, retInserted);
		}
		else {
			result = notesAPI.b32_IDInsert(m_idTableHandle32.getValue(), noteId, retInserted);
		}
		NotesErrorUtils.checkResult(result);
		int retInsertedAsInt = retInserted.getValue();
		return retInsertedAsInt != 0;
	}

	public boolean removeNote(int noteId) {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		IntByReference retDeleted = new IntByReference();
		short result;
		if (NotesContext.is64Bit()) {
			result = notesAPI.b64_IDDelete(m_idTableHandle64.getValue(), noteId, retDeleted);
		}
		else {
			result = notesAPI.b32_IDDelete(m_idTableHandle32.getValue(), noteId, retDeleted);
		}
		NotesErrorUtils.checkResult(result);
		int retDeletedAsInt = retDeleted.getValue();
		return retDeletedAsInt != 0;
	}
	
	public int sizeInBytes() {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		if (NotesContext.is64Bit()) {
			int size = notesAPI.b64_IDTableSize(m_idTableHandle64.getValue());
			return size;
		}
		else {
			int size = notesAPI.b32_IDTableSize(m_idTableHandle32.getValue());
			return size;
		}
	}
	
	public int count() {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		if (NotesContext.is64Bit()) {
			int entries = notesAPI.b64_IDEntries(m_idTableHandle64.getValue());
			return entries;
		}
		else {
			int entries = notesAPI.b32_IDEntries(m_idTableHandle32.getValue());
			return entries;
		}
	}
	
	public boolean isModified() {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		
		int sizeBytes = sizeInBytes();
		Pointer ptr;
		if (NotesContext.is64Bit()) {
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
			if (NotesContext.is64Bit()) {
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
	
	public boolean isInverted() {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		
		int sizeBytes = sizeInBytes();
		Pointer ptr;
		if (NotesContext.is64Bit()) {
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
			if (NotesContext.is64Bit()) {
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
	
	public void setModified(boolean modified) {
		checkHandle();
		short newFlags = (short) ((isInverted() ? NotesCAPI.IDTABLE_INVERTED : 0) + (modified ? NotesCAPI.IDTABLE_MODIFIED : 0));
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		int sizeBytes = sizeInBytes();
		Pointer ptr;
		if (NotesContext.is64Bit()) {
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
			if (NotesContext.is64Bit()) {
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
	
	public void setInverted(boolean inverted) {
		checkHandle();
		short newFlags = (short) ((isModified() ? NotesCAPI.IDTABLE_MODIFIED : 0) + (inverted ? NotesCAPI.IDTABLE_INVERTED : 0));
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		int sizeBytes = sizeInBytes();
		Pointer ptr;
		if (NotesContext.is64Bit()) {
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
			if (NotesContext.is64Bit()) {
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
	
	public int[] toArray() {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		int[] ids = new int[count()];
		IntByReference retID = new IntByReference();
		boolean first = true;
		int cnt = 0;
		if (NotesContext.is64Bit()) {
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
	
	public void subtract(NotesIDTable otherTable) {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		if (NotesContext.is64Bit()) {
			short result = notesAPI.b64_IDDeleteTable(m_idTableHandle64.getValue(), otherTable.m_idTableHandle64.getValue());
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_IDDeleteTable(m_idTableHandle32.getValue(), otherTable.m_idTableHandle32.getValue());
			NotesErrorUtils.checkResult(result);
		}
	}
	
	public void merge(NotesIDTable otherTable) {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		if (NotesContext.is64Bit()) {
			short result = notesAPI.b64_IDInsertTable(m_idTableHandle64.getValue(), otherTable.m_idTableHandle64.getValue());
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_IDInsertTable(m_idTableHandle32.getValue(), otherTable.m_idTableHandle32.getValue());
			NotesErrorUtils.checkResult(result);
		}
	}
	
	public boolean contains(int noteId) {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		if (NotesContext.is64Bit()) {
			return notesAPI.b64_IDIsPresent(m_idTableHandle64.getValue(), noteId);
		}
		else {
			return notesAPI.b64_IDIsPresent(m_idTableHandle32.getValue(), noteId);
		}
	}
	
	public Object clone() {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		if (NotesContext.is64Bit()) {
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
