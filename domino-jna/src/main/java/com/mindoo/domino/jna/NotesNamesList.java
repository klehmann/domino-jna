package com.mindoo.domino.jna;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.Handle;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
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

	public NotesNamesList(IAdaptable adaptable) {
		Handle hdl = adaptable.getAdapter(Handle.class);
		if (hdl!=null) {
			if (PlatformUtils.is64Bit()) {
				m_handle64 = hdl.getHandle64();
			}
			else {
				m_handle32 = hdl.getHandle32();
			}
			return;
		}
		throw new NotesError(0, "Unsupported adaptable parameter");
	}

	public void setNoRecycle() {
		m_noRecycle=true;
	}

	@Override
	public void free() {
		if (m_noRecycle || isFreed())
			return;

		if (PlatformUtils.is64Bit()) {
			NotesGC.__memoryBeeingFreed(this);
			short result = Mem64.OSMemFree(m_handle64);
			NotesErrorUtils.checkResult(result);
			m_handle64=0;
		}
		else {
			NotesGC.__memoryBeeingFreed(this);
			short result = Mem32.OSMemFree(m_handle32);
			NotesErrorUtils.checkResult(result);
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
				Pointer ptr = Mem64.OSLockObject(m_handle64);
				try {
					m_names = readNamesList(ptr);
				}
				finally {
					Mem64.OSUnlockObject(m_handle64);
				}
			}
			else {
				Pointer ptr = Mem32.OSLockObject(m_handle32);
				try {
					m_names = readNamesList(ptr);
				}
				finally {
					Mem32.OSUnlockObject(m_handle32);
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
