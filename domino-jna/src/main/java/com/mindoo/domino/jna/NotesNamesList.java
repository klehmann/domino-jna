package com.mindoo.domino.jna;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.internal.WinNotesCAPI;
import com.mindoo.domino.jna.structs.NotesNamesListHeader32;
import com.mindoo.domino.jna.structs.NotesNamesListHeader64;
import com.mindoo.domino.jna.structs.WinNotesNamesListHeader64;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * NAMES_LIST structure wrapper that wraps a user names list stored in memory
 * 
 * @author Karsten Lehmann
 */
public class NotesNamesList implements IAllocatedMemory {
	private int m_handle32;
	private long m_handle64;
	private List<String> m_names;
	private boolean m_noRecycle;

	public NotesNamesList(int handle) {
		if (NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor not available in 64 bit");
		
		m_handle32 = handle;
	}

	public NotesNamesList(long handle) {
		if (!NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor not available in 32 bit");
		
		m_handle64 = handle;
	}

	public void setNoRecycle() {
		m_noRecycle=true;
	}

	@Override
	public void free() {
		if (m_noRecycle || isFreed())
			return;

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_OSMemFree(m_handle64);
			NotesErrorUtils.checkResult(result);
			NotesGC.__memoryBeeingFreed(this);
			m_handle64=0;
		}
		else {
			short result = notesAPI.b32_OSMemFree(m_handle32);
			NotesErrorUtils.checkResult(result);
			NotesGC.__memoryBeeingFreed(this);
			m_handle32=0;
		}
	}

	@Override
	public boolean isFreed() {
		if (NotesJNAContext.is64Bit()) {
			return m_handle64==0;
		}
		else {
			return m_handle32==0;
		}
	}

	@Override
	public int getHandle32() {
		return m_handle32;
	}

	@Override
	public long getHandle64() {
		return m_handle64;
	}

	/**
	 * Checks if the database is already recycled
	 */
	private void checkHandle() {
		if (NotesJNAContext.is64Bit()) {
			if (m_handle64==0)
				throw new NotesError(0, "Memory already freed");
			NotesGC.__b64_checkValidMemHandle(getClass(), m_handle64);
		}
		else {
			if (m_handle32==0)
				throw new NotesError(0, "Memory already freed");
			NotesGC.__b64_checkValidMemHandle(getClass(), m_handle32);
		}
	}

	@Override
	public String toString() {
		if (isFreed()) {
			return "NotesNamesList [freed]";
		}
		else {
			return "NotesNamesList [handle="+(NotesJNAContext.is64Bit() ? m_handle64 : m_handle32)+", values="+getNames()+"]";
		}
	}
	
	/**
	 * Returns the names contained in this names list
	 * 
	 * @return names
	 */
	public List<String> getNames() {
		checkHandle();
		
		if (m_names==null) {
			NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

			if (NotesJNAContext.is64Bit()) {
				Pointer ptr = notesAPI.b64_OSLockObject(m_handle64);
				try {
					m_names = readNamesList(ptr);
				}
				finally {
					notesAPI.b64_OSUnlockObject(m_handle64);
				}
			}
			else {
				Pointer ptr = notesAPI.b32_OSLockObject(m_handle32);
				try {
					m_names = readNamesList(ptr);
				}
				finally {
					notesAPI.b32_OSUnlockObject(m_handle32);
				}
			}
		}
		return m_names;
	}
	
	/**
	 * Decodes a usernames list stored in memory
	 * 
	 * @param namesListBufferPtr Pointer to user names list
	 * @return usernames list
	 */
	private static List<String> readNamesList(Pointer namesListBufferPtr) {
		long offset;
		int numNames;
		List<String> names;

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		if (NotesJNAContext.is64Bit()) {
			if (notesAPI instanceof WinNotesCAPI) {
				WinNotesNamesListHeader64 namesList = new WinNotesNamesListHeader64(namesListBufferPtr);
				namesList.read();

				names = new ArrayList<String>(namesList.NumNames);

				offset = NotesCAPI.winNamesListHeaderSize64;
				numNames = (int) (namesList.NumNames & 0xffff);
			}
			else {
				NotesNamesListHeader64 namesList = new NotesNamesListHeader64(namesListBufferPtr);
				namesList.read();

				names = new ArrayList<String>(namesList.NumNames);

				offset = NotesCAPI.namesListHeaderSize64;
				numNames = (int) (namesList.NumNames & 0xffff);
				
			}
		}
		else {
			NotesNamesListHeader32 namesList = new NotesNamesListHeader32(namesListBufferPtr);
			namesList.read();

			names = new ArrayList<String>(namesList.NumNames);

			offset = NotesCAPI.namesListHeaderSize32;
			numNames = (int) (namesList.NumNames & 0xffff);
		}
		

		while (names.size() < numNames) {
			byte b = namesListBufferPtr.getByte(offset);

			if (b == 0) {
				Memory mem = new Memory(bOut.size());
				mem.write(0, bOut.toByteArray(), 0, bOut.size());
				String currUserName = NotesStringUtils.fromLMBCS(mem, bOut.size());
				names.add(currUserName);
				bOut.reset();
			}
			else {
				bOut.write(b);
			}
			offset++;
		}

		return names;
	}

}
