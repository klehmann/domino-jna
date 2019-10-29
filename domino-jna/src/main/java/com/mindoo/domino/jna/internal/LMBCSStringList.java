package com.mindoo.domino.jna.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * A textlist implementation that stores the values as LMBCS encoded strings
 * 
 * @author Karsten Lehmann
 */
public class LMBCSStringList implements IAllocatedMemory, Iterable<String> {
	private List<String> m_values;
	private boolean m_prefixDataType;
	private int m_handle32;
	private long m_handle64;
	private int m_listSizeBytes;
	private boolean m_noFree;

	@SuppressWarnings("unchecked")
	public LMBCSStringList(boolean prefixDataType) {
		this(Collections.EMPTY_LIST, prefixDataType);
	}
	
	public LMBCSStringList(List<String> values, boolean prefixDataType) {
		if (values==null) {
			values = Collections.emptyList();
		}
		
		m_values = new ArrayList<String>();
		m_prefixDataType = prefixDataType;
		
		allocate();
		NotesGC.__memoryAllocated(this);
		
		addAll(values);
	}

	public boolean isPrefixDataType() {
		return m_prefixDataType;
	}
	
	public int getListSizeInBytes() {
		return m_listSizeBytes;
	}
	
	private void allocate() {
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference rethList = new LongByReference();
			ShortByReference retListSize = new ShortByReference();

			result = NotesNativeAPI64.get().ListAllocate((short) 0, 
					(short) 0,
					m_prefixDataType ? 1 : 0, rethList, null, retListSize);
			
			NotesErrorUtils.checkResult(result);

			m_handle64 = rethList.getValue();
			Mem64.OSUnlockObject(m_handle64);
			
			m_listSizeBytes = retListSize.getValue() & 0xffff;
		}
		else {
			IntByReference rethList = new IntByReference();
			ShortByReference retListSize = new ShortByReference();

			result = NotesNativeAPI32.get().ListAllocate((short) 0, 
					(short) 0,
					m_prefixDataType ? 1 : 0, rethList, null, retListSize);
			
			NotesErrorUtils.checkResult(result);

			m_handle32 = rethList.getValue();
			Mem32.OSUnlockObject(m_handle32);
			
			m_listSizeBytes = retListSize.getValue() & 0xffff;
		}
	}

	/**
	 * Removes all entries from the list
	 */
	public void clear() {
		if (m_values.isEmpty()) {
			return;
		}
		
		checkHandle();
		
		ShortByReference retListSize = new ShortByReference();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().ListRemoveAllEntries(m_handle64, m_prefixDataType ? 1 : 0, retListSize);
		}
		else {
			result = NotesNativeAPI32.get().ListRemoveAllEntries(m_handle32, m_prefixDataType ? 1 : 0, retListSize);
		}
		NotesErrorUtils.checkResult(result);
		
		m_listSizeBytes = (int) (retListSize.getValue() & 0xffff);
		m_values.clear();
	}
	
	/**
	 * Adds a value to the list
	 * 
	 * @param value value to add
	 */
	public void add(String value) {
		addAll(Arrays.asList(value));
	}
	
	/**
	 * Adds values to the list
	 * 
	 * @param newValues values to add
	 */
	public void addAll(List<String> newValues) {
		if (newValues.isEmpty()) {
			return;
		}
		
		checkHandle();
		
		if ((m_values.size() + newValues.size())> 65535) {
			throw new IllegalArgumentException("String list size must fit in a WORD ("+m_values.size()+">65535)");
		}
		
		short result;
		if (PlatformUtils.is64Bit()) {
			ShortByReference retListSize = new ShortByReference();
			retListSize.setValue((short) (m_listSizeBytes & 0xffff));

			for (int i=0; i<newValues.size(); i++) {
				String currStr = newValues.get(i);
				Memory currStrMem = NotesStringUtils.toLMBCS(currStr, false);

				short entryNo = (short) (m_values.size() & 0xffff);
				
				result = NotesNativeAPI64.get().ListAddEntry(m_handle64, m_prefixDataType ? 1 : 0, retListSize, entryNo, currStrMem,
						(short) (currStrMem==null ? 0 : (currStrMem.size() & 0xffff)));
				NotesErrorUtils.checkResult(result);
			}
			
			m_listSizeBytes = retListSize.getValue() & 0xffff;
		}
		else {
			ShortByReference retListSize = new ShortByReference();
			retListSize.setValue((short) (m_listSizeBytes & 0xffff));
			
			for (int i=0; i<newValues.size(); i++) {
				String currStr = newValues.get(i);
				Memory currStrMem = NotesStringUtils.toLMBCS(currStr, false);

				short entryNo = (short) (m_values.size() & 0xffff);

				result = NotesNativeAPI32.get().ListAddEntry(m_handle32, m_prefixDataType ? 1 : 0, retListSize, entryNo, currStrMem,
						(short) (currStrMem==null ? 0 : (currStrMem.size() & 0xffff)));
				NotesErrorUtils.checkResult(result);
			}
			
			m_listSizeBytes = retListSize.getValue() & 0xffff;
		}
		
		m_values.addAll(newValues);
	}
	
	/**
	 * Call this method when the ownership of the list memory gets transferred and
	 * we should not free the handle anymore.
	 */
	public void setNoFree() {
		m_noFree = true;
	}
	
	@Override
	public void free() {
		if (isFreed() || m_noFree) {
			return;
		}
		
		if (PlatformUtils.is64Bit()) {
			short result = Mem64.OSMemFree(m_handle64);
			NotesErrorUtils.checkResult(result);
			m_handle64=0;
		}
		else {
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

	private void checkHandle() {
		if (isFreed())
			throw new NotesError(0, "List already freed");
		
		if (PlatformUtils.is64Bit()) {
			NotesGC.__b64_checkValidMemHandle(LMBCSStringList.class, m_handle64);
		}
		else {
			NotesGC.__b32_checkValidMemHandle(LMBCSStringList.class, m_handle32);
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
	public Iterator<String> iterator() {
		return m_values.iterator();
	}
	
	public int getSize() {
		return m_values.size();
	}
	
}
