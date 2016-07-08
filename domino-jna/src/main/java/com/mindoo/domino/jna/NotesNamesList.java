package com.mindoo.domino.jna;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.NotesNamesListHeader;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * NAMES_LIST structure wrapper that wraps a user names list stored in memory
 * 
 * @author Karsten Lehmann
 */
public class NotesNamesList implements IRecyclableNotesObject {
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
	public void recycle() {
		if (m_noRecycle || isRecycled())
			return;

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_OSMemFree(m_handle64);
			NotesErrorUtils.checkResult(result);
			NotesGC.__objectRecycled(this);
			m_handle64=0;
		}
		else {
			short result = notesAPI.b32_OSMemFree(m_handle32);
			NotesErrorUtils.checkResult(result);
			NotesGC.__objectRecycled(this);
			m_handle32=0;
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
	public int getHandle32() {
		return m_handle32;
	}

	@Override
	public long getHandle64() {
		return m_handle64;
	}

	@Override
	public String toString() {
		if (isRecycled()) {
			return "NotesNamesList [recycled]";
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
					notesAPI.b64_OSUnlockObject(m_handle32);
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
		NotesNamesListHeader namesList = new NotesNamesListHeader(namesListBufferPtr);
		namesList.read();

		List<String> names = new ArrayList<String>(namesList.NumNames);

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();

		long offset = namesList.size();

		while (names.size() < namesList.NumNames) {
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
