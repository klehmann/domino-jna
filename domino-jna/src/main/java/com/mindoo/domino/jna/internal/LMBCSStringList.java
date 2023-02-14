package com.mindoo.domino.jna.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.DHANDLE32;
import com.mindoo.domino.jna.internal.handles.DHANDLE64;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.ptr.ShortByReference;

/**
 * A textlist implementation that stores the values as LMBCS encoded strings
 * 
 * @author Karsten Lehmann
 */
public class LMBCSStringList implements IAllocatedMemory, Iterable<String> {
	private List<String> m_values;
	private boolean m_prefixDataType;
	private DHANDLE m_handle;
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
		
		DHANDLE.ByReference rethList = DHANDLE.newInstanceByReference();
		ShortByReference retListSize = new ShortByReference();

		result = NotesNativeAPI.get().ListAllocate((short) 0, 
				(short) 0,
				m_prefixDataType ? 1 : 0, rethList, null, retListSize);
		
		NotesErrorUtils.checkResult(result);

		m_handle = rethList;
		Mem.OSUnlockObject(m_handle);
		
		m_listSizeBytes = retListSize.getValue() & 0xffff;
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
		
		short result = NotesNativeAPI.get().ListRemoveAllEntries(m_handle.getByValue(), m_prefixDataType ? 1 : 0, retListSize);
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
		ShortByReference retListSize = new ShortByReference();
		retListSize.setValue((short) (m_listSizeBytes & 0xffff));

		for (int i=0; i<newValues.size(); i++) {
			String currStr = newValues.get(i);
			Memory currStrMem = NotesStringUtils.toLMBCS(currStr, false);
			if (currStrMem!=null && currStrMem.size() > 65535) {
				throw new NotesError(MessageFormat.format("List item at position {0} exceeds max lengths of 65535 bytes", i));
			}

			char textSize = currStrMem==null ? 0 : (char) currStrMem.size();

			result = NotesNativeAPI.get().ListAddEntry(m_handle.getByValue(),
					m_prefixDataType ? 1 : 0,
							retListSize,
							(char) i,
							currStrMem,
							textSize);
			NotesErrorUtils.checkResult(result);
		}
		
		m_listSizeBytes = retListSize.getValue() & 0xffff;
		
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
		
		short result = Mem.OSMemFree(m_handle.getByValue());
		NotesErrorUtils.checkResult(result);
		m_handle=null;
	}

	@Override
	public boolean isFreed() {
		return m_handle==null || m_handle.isNull();
	}

	private void checkHandle() {
		if (isFreed())
			throw new NotesError(0, "List already freed");
		
		if (PlatformUtils.is64Bit()) {
			NotesGC.__b64_checkValidMemHandle(LMBCSStringList.class, ((DHANDLE64)m_handle).hdl);
		}
		else {
			NotesGC.__b32_checkValidMemHandle(LMBCSStringList.class, ((DHANDLE32)m_handle).hdl);
		}
	}

	@Override
	public int getHandle32() {
		return ((DHANDLE32)m_handle).hdl;
	}

	@Override
	public long getHandle64() {
		return ((DHANDLE64)m_handle).hdl;
	}

	public DHANDLE getHandle() {
		return m_handle;
	}
	
	@Override
	public Iterator<String> iterator() {
		return m_values.iterator();
	}
	
	public int getSize() {
		return m_values.size();
	}
	
}
