package com.mindoo.domino.jna.dxl;

import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.Handle;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.LMBCSStringList;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

public abstract class AbstractDXLTransfer implements IAllocatedMemory {

	protected abstract short getProperty(int index, Memory m);
	
	protected abstract short setProperty(int prop, Memory propValue);
	
	protected abstract void checkHandle();
	
	@SuppressWarnings("unchecked")
	protected List<String> getStringList(int index) {
		checkHandle();

		DisposableMemory m = new DisposableMemory(8);
		m.clear();
		try {
			short result = getProperty(index, m);
			NotesErrorUtils.checkResult(result);
			
			int handle = m.getInt(0);
			if (handle==0) {
				return Collections.emptyList();
			}
			
			if (PlatformUtils.is64Bit()) {
				Pointer pList = Mem64.OSLockObject(handle);
				try {
					@SuppressWarnings("rawtypes")
					List list = ItemDecoder.decodeTextListValue(pList, false);
					return list;
				}
				finally {
					Mem64.OSUnlockObject(handle);
					result = Mem64.OSMemFree(handle);
					NotesErrorUtils.checkResult(result);
				}
			}
			else {
				Pointer pList = Mem32.OSLockObject(handle);
				try {
					@SuppressWarnings("rawtypes")
					List list = ItemDecoder.decodeTextListValue(pList, false);
					return list;
				}
				finally {
					Mem32.OSUnlockObject(handle);
					result = Mem32.OSMemFree(handle);
					NotesErrorUtils.checkResult(result);
				}
			}
		}
		finally {
			m.dispose();
		}
	}

	protected String getStringFromMemhandle(int index) {
		checkHandle();

		DisposableMemory m = new DisposableMemory(8);
		m.clear();
		try {
			short result = getProperty(index, m);
			NotesErrorUtils.checkResult(result);
			
			int memHandle = m.getInt(0);
			if (memHandle==0) {
				return "";
			}
			
			if (PlatformUtils.is64Bit()) {
				Pointer ptr = Mem64.OSMemoryLock(memHandle);
				try {
					String str = NotesStringUtils.fromLMBCS(ptr, -1);
					return str;
				}
				finally {
					Mem64.OSMemoryUnlock(memHandle);
					Mem64.OSMemoryFree(memHandle);
				}
			}
			else {
				Pointer ptr = Mem32.OSMemoryLock(memHandle);
				try {
					String str = NotesStringUtils.fromLMBCS(ptr, -1);
					return str;
				}
				finally {
					Mem32.OSMemoryUnlock(memHandle);
					Mem32.OSMemoryFree(memHandle);
				}
			}
		}
		finally {
			m.dispose();
		}
	}
	
	protected NotesIDTable getIDTableFromHandle(int index) {
		checkHandle();

		if (PlatformUtils.is64Bit()) {
			DisposableMemory m = new DisposableMemory(8);
			m.clear();
			try {
				short result = getProperty(index, m);
				NotesErrorUtils.checkResult(result);

				long handle = m.getLong(0);
				if (handle==0) {
					return null;
				}
				
				NotesIDTable idTable = new NotesIDTable(new Handle(handle), true);
				//register IDTable for later disposal
				NotesGC.__objectCreated(NotesIDTable.class, idTable);

				return idTable;
			}
			finally {
				m.dispose();
			}

		}
		else {
			DisposableMemory m = new DisposableMemory(8);
			m.clear();
			try {
				short result = getProperty(index, m);
				NotesErrorUtils.checkResult(result);

				int handle = m.getInt(0);
				if (handle==0) {
					return null;
				}
				
				NotesIDTable idTable = new NotesIDTable(new Handle(handle), true);
				//register IDTable for later disposal
				NotesGC.__objectCreated(NotesIDTable.class, idTable);

				return idTable;
			}
			finally {
				m.dispose();
			}
		}
	}
	
	protected boolean getBooleanProperty(int index) {
		checkHandle();

		DisposableMemory m = new DisposableMemory(8);
		m.clear();
		try {
			short result = getProperty(index, m);
			NotesErrorUtils.checkResult(result);
			short boolAsShort = m.getShort(0);
			return boolAsShort != 0;
		}
		finally {
			m.dispose();
		}
	}
	
	protected void setBooleanProperty(int index, boolean value) {
		checkHandle();

		DisposableMemory m = new DisposableMemory(8);
		m.clear();
		try {
			m.setByte(0, (byte) (value ? 1 : 0));
			short result = setProperty(index, m);
			NotesErrorUtils.checkResult(result);
		}
		finally {
			m.dispose();
		}
	}
	
	protected int getInt(int index) {
		checkHandle();
		
		DisposableMemory m = new DisposableMemory(8);
		m.clear();
		try {
			short result = getProperty(index, m);
			NotesErrorUtils.checkResult(result);

			return m.getInt(0);
		}
		finally {
			m.dispose();
		}
	}

	protected long getLong(int index) {
		checkHandle();
		
		DisposableMemory m = new DisposableMemory(8);
		m.clear();
		try {
			short result = getProperty(index, m);
			NotesErrorUtils.checkResult(result);

			return m.getLong(0);
		}
		finally {
			m.dispose();
		}
	}

	protected void setStringList(int index, List<String> values) {
		LMBCSStringList lmbcsStrList = new LMBCSStringList(values, false);
		try {
			if (PlatformUtils.is64Bit()) {
				DisposableMemory m = new DisposableMemory(8);
				m.clear();
				try {
					m.setLong(0, lmbcsStrList.getHandle64());
					short result = setProperty(index, m);
					NotesErrorUtils.checkResult(result);
				}
				finally {
					m.dispose();
				}
			}
			else {
				DisposableMemory m = new DisposableMemory(8);
				m.clear();
				try {
					m.setInt(0, lmbcsStrList.getHandle32());
					short result = setProperty(index, m);
					NotesErrorUtils.checkResult(result);
				}
				finally {
					m.dispose();
				}
			}
		}
		finally {
			lmbcsStrList.free();
		}
	}
	
	protected void setInt(int index, int value) {
		checkHandle();
		
		DisposableMemory m = new DisposableMemory(8);
		m.clear();
		try {
			m.setInt(0, value);

			short result = setProperty(index, m);
			NotesErrorUtils.checkResult(result);
		}
		finally {
			m.dispose();
		}
	}

	protected void setLong(int index, long value) {
		checkHandle();
		
		DisposableMemory m = new DisposableMemory(8);
		m.clear();
		try {
			m.clear();
			m.setLong(0, value);

			short result = setProperty(index, m);
			NotesErrorUtils.checkResult(result);
		}
		finally {
			m.dispose();
		}
	}

	protected void setStringProperty(int index, String str) {
		checkHandle();
		
		if (str==null) {
			str = "";
		}
		
		Memory strAsLMBCs = NotesStringUtils.toLMBCS(str, true);
		short result = setProperty(index, strAsLMBCs);
		NotesErrorUtils.checkResult(result);
	}

	protected short getShort(int index) {
		checkHandle();
		
		DisposableMemory m = new DisposableMemory(8);
		m.clear();
		
		try {
			short result = getProperty(index, m);
			NotesErrorUtils.checkResult(result);
			
			return m.getShort(0);
		}
		finally {
			m.dispose();
		}
	}

	protected void setShort(int index, short value) {
		checkHandle();
		
		DisposableMemory m = new DisposableMemory(8);
		m.clear();
		
		try {
			m.clear();
			m.setShort(0, value);

			short result = setProperty(index, m);
			NotesErrorUtils.checkResult(result);
		}
		finally {
			m.dispose();
		}
	}
}
