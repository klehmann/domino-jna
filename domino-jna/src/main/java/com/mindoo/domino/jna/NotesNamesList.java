package com.mindoo.domino.jna;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.structs.LinuxNotesNamesListHeader64Struct;
import com.mindoo.domino.jna.internal.structs.MacNotesNamesListHeader64Struct;
import com.mindoo.domino.jna.internal.structs.NotesNamesListHeader32Struct;
import com.mindoo.domino.jna.internal.structs.WinNotesNamesListHeader32Struct;
import com.mindoo.domino.jna.internal.structs.WinNotesNamesListHeader64Struct;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
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
		if (PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor not available in 64 bit");
		
		m_handle32 = handle;
	}

	public NotesNamesList(long handle) {
		if (!PlatformUtils.is64Bit())
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

		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().OSMemFree(m_handle64);
			NotesErrorUtils.checkResult(result);
			NotesGC.__memoryBeeingFreed(this);
			m_handle64=0;
		}
		else {
			short result = NotesNativeAPI32.get().OSMemFree(m_handle32);
			NotesErrorUtils.checkResult(result);
			NotesGC.__memoryBeeingFreed(this);
			m_handle32=0;
		}
	}

	@Override
	public boolean isFreed() {
		if (PlatformUtils.is64Bit()) {
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
		if (PlatformUtils.is64Bit()) {
			if (m_handle64==0)
				throw new NotesError(0, "Memory already freed");
			NotesGC.__b64_checkValidMemHandle(getClass(), m_handle64);
		}
		else {
			if (m_handle32==0)
				throw new NotesError(0, "Memory already freed");
			NotesGC.__b32_checkValidMemHandle(getClass(), m_handle32);
		}
	}

	@Override
	public String toString() {
		if (isFreed()) {
			return "NotesNamesList [freed]";
		}
		else {
			return "NotesNamesList [handle="+(PlatformUtils.is64Bit() ? m_handle64 : m_handle32)+", values="+getNames()+"]";
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
			if (PlatformUtils.is64Bit()) {
				Pointer ptr = NotesNativeAPI64.get().OSLockObject(m_handle64);
				try {
					m_names = readNamesList(ptr);
				}
				finally {
					NotesNativeAPI64.get().OSUnlockObject(m_handle64);
				}
			}
			else {
				Pointer ptr = NotesNativeAPI32.get().OSLockObject(m_handle32);
				try {
					m_names = readNamesList(ptr);
				}
				finally {
					NotesNativeAPI32.get().OSUnlockObject(m_handle32);
				}
			}
		}
		return new ArrayList<String>(m_names);
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

		if (PlatformUtils.is64Bit()) {
			if (PlatformUtils.isWindows()) {
				WinNotesNamesListHeader64Struct namesList = WinNotesNamesListHeader64Struct.newInstance(namesListBufferPtr);
				namesList.read();
				
				names = new ArrayList<String>(namesList.NumNames);

				offset = namesList.size();
				numNames = (int) (namesList.NumNames & 0xffff);
			}
			else if (PlatformUtils.isMac()) {
				MacNotesNamesListHeader64Struct namesList = MacNotesNamesListHeader64Struct.newInstance(namesListBufferPtr);
				namesList.read();

				names = new ArrayList<String>(namesList.NumNames);

				offset = namesList.size();
				numNames = (int) (namesList.NumNames & 0xffff);
			}
			else {
				LinuxNotesNamesListHeader64Struct namesList = LinuxNotesNamesListHeader64Struct.newInstance(namesListBufferPtr);
				namesList.read();

				names = new ArrayList<String>(namesList.NumNames);

				offset = namesList.size();
				numNames = (int) (namesList.NumNames & 0xffff);
			}
		}
		else {
			if (PlatformUtils.isWindows()) {
				WinNotesNamesListHeader32Struct namesList = WinNotesNamesListHeader32Struct.newInstance(namesListBufferPtr);
				namesList.read();

				names = new ArrayList<String>(namesList.NumNames);

				offset = namesList.size();
				numNames = (int) (namesList.NumNames & 0xffff);
			}
			else {
				NotesNamesListHeader32Struct namesList = NotesNamesListHeader32Struct.newInstance(namesListBufferPtr);
				namesList.read();

				names = new ArrayList<String>(namesList.NumNames);

				offset = namesList.size();
				numNames = (int) (namesList.NumNames & 0xffff);

			}
		}
		
		if (numNames==0)
			return Collections.emptyList();

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
