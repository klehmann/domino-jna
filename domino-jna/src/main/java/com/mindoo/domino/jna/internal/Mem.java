package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.DHANDLE32;
import com.mindoo.domino.jna.internal.handles.DHANDLE64;
import com.mindoo.domino.jna.internal.structs.NotesBlockIdStruct;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

public class Mem {

	public static Pointer OSLockObject(DHANDLE handle) {
		if (handle==null || handle.isNull())
			throw new IllegalArgumentException("Null handle cannot be locked");
		else if (handle instanceof DHANDLE64) {
			return Mem64.OSLockObject(((DHANDLE64) handle).getValue());
		}
		else if (handle instanceof DHANDLE32) {
			return Mem32.OSLockObject(((DHANDLE32) handle).getValue());
		}
		else {
			throw new IllegalArgumentException("Unsupported handle type: "+handle.getClass().getName());
		}
	}
	
	public static boolean OSUnlockObject(DHANDLE handle) {
		if (handle==null || handle.isNull())
			throw new IllegalArgumentException("Null handle cannot be unlocked");
		else if (handle instanceof DHANDLE64) {
			return Mem64.OSUnlockObject(((DHANDLE64) handle).getValue());
		}
		else if (handle instanceof DHANDLE32) {
			return Mem32.OSUnlockObject(((DHANDLE32) handle).getValue());
		}
		else {
			throw new IllegalArgumentException("Unsupported handle type: "+handle.getClass().getName());
		}
	}
	
	public static short OSMemFree(DHANDLE handle) {
		if (handle==null || handle.isNull())
			throw new IllegalArgumentException("Memory for null handle cannot be freed");
		else if (handle instanceof DHANDLE64) {
			short result = Mem64.OSMemFree(((DHANDLE64) handle).getValue());
			if (result==0) {
				handle.setDisposed();
			}
			return result;
		}
		else if (handle instanceof DHANDLE32) {
			short result = Mem32.OSMemFree(((DHANDLE32) handle).getValue());
			if (result==0) {
				handle.setDisposed();
			}
			return result;
		}
		else {
			throw new IllegalArgumentException("Unsupported handle type: "+handle.getClass().getName());
		}
	}
	
	public static short OSMemGetSize(DHANDLE handle, IntByReference retSize) {
		if (handle==null || handle.isNull())
			throw new IllegalArgumentException("Size of memory with null handle cannot be read");
		else if (handle instanceof DHANDLE64) {
			return Mem64.OSMemGetSize(((DHANDLE64) handle).getValue(), retSize);
		}
		else if (handle instanceof DHANDLE32) {
			return Mem32.OSMemGetSize(((DHANDLE32) handle).getValue(), retSize);
		}
		else {
			throw new IllegalArgumentException("Unsupported handle type: "+handle.getClass().getName());
		}
	}

	public static int OSMemoryGetSize(DHANDLE handle) {
		if (handle==null || handle.isNull())
			throw new IllegalArgumentException("Size of memory with null handle cannot be read");
		else if (handle instanceof DHANDLE64) {
			return Mem64.OSMemoryGetSize(((DHANDLE64) handle).getValue());
		}
		else if (handle instanceof DHANDLE32) {
			return Mem32.OSMemoryGetSize(((DHANDLE32) handle).getValue());
		}
		else {
			throw new IllegalArgumentException("Unsupported handle type: "+handle.getClass().getName());
		}
	}

	public static void OSMemoryFree(DHANDLE handle) {
		if (handle==null || handle.isNull())
			throw new IllegalArgumentException("Memory with null handle cannot be freed");
		else if (handle instanceof DHANDLE64) {
			Mem64.OSMemoryFree(((DHANDLE64) handle).getValue());
			handle.setDisposed();
		}
		else if (handle instanceof DHANDLE32) {
			Mem32.OSMemoryFree(((DHANDLE32) handle).getValue());
			handle.setDisposed();
		}
		else {
			throw new IllegalArgumentException("Unsupported handle type: "+handle.getClass().getName());
		}
	}
	
	public static short OSMemoryReallocate(DHANDLE handle, int size) {
		if (handle==null || handle.isNull())
			throw new IllegalArgumentException("Reallocation of memory with null handle is not possible");
		else if (handle instanceof DHANDLE64) {
			return Mem64.OSMemoryReallocate(((DHANDLE64) handle).getValue(), size);
		}
		else if (handle instanceof DHANDLE32) {
			return Mem32.OSMemoryReallocate(((DHANDLE32) handle).getValue(), size);
		}
		else {
			throw new IllegalArgumentException("Unsupported handle type: "+handle.getClass().getName());
		}
	}

	public static Pointer OSMemoryLock(DHANDLE handle) {
		if (handle==null || handle.isNull())
			throw new IllegalArgumentException("Locking of memory with null handle is not possible");
		else if (handle instanceof DHANDLE64) {
			return Mem64.OSMemoryLock(((DHANDLE64) handle).getValue());
		}
		else if (handle instanceof DHANDLE32) {
			return Mem32.OSMemoryLock(((DHANDLE32) handle).getValue());
		}
		else {
			throw new IllegalArgumentException("Unsupported handle type: "+handle.getClass().getName());
		}
	}

	public static boolean OSMemoryUnlock(DHANDLE handle) {
		if (handle==null || handle.isNull())
			throw new IllegalArgumentException("Unlocking of memory with null handle is not possible");
		else if (handle instanceof DHANDLE64) {
			return Mem64.OSMemoryUnlock(((DHANDLE64) handle).getValue());
		}
		else if (handle instanceof DHANDLE32) {
			return Mem32.OSMemoryUnlock(((DHANDLE32) handle).getValue());
		}
		else {
			throw new IllegalArgumentException("Unsupported handle type: "+handle.getClass().getName());
		}
	}
	
	public static short OSMemAlloc(
			short  BlkType,
			int  dwSize,
			DHANDLE.ByReference retHandle) {
		if (retHandle instanceof DHANDLE64) {
			LongByReference retHandle64 = new LongByReference();
			short status = Mem64.OSMemAlloc(BlkType, dwSize, retHandle64);
			((DHANDLE64)retHandle).hdl = retHandle64.getValue();
			return status;
		}
		else if (retHandle instanceof DHANDLE32) {
			IntByReference retHandle32 = new IntByReference();
			short status = Mem32.OSMemAlloc(BlkType, dwSize, retHandle32);
			((DHANDLE32)retHandle).hdl = retHandle32.getValue();
			return status;
		}
		else {
			throw new IllegalArgumentException("Unsupported return handle type: "+retHandle==null ? "null" : retHandle.getClass().getName());
		}
	}

	public static short OSMemGetTyoe(DHANDLE handle) {
		if (handle==null || handle.isNull())
			throw new IllegalArgumentException("Type of memory with null handle cannot be read");
		else if (handle instanceof DHANDLE64) {
			return Mem64.OSMemGetTyoe(((DHANDLE64) handle).getValue());
		}
		else if (handle instanceof DHANDLE32) {
			return Mem32.OSMemGetTyoe(((DHANDLE32) handle).getValue());
		}
		else {
			throw new IllegalArgumentException("Unsupported handle type: "+handle.getClass().getName());
		}
	}
	
	public static short OSMemoryAllocate(int dwtype, int size, DHANDLE.ByReference retHandle) {
		if (retHandle instanceof DHANDLE64) {
			LongByReference retHandle64 = new LongByReference();
			short status = Mem64.OSMemoryAllocate(dwtype, size, retHandle64);
			((DHANDLE64)retHandle).hdl = retHandle64.getValue();
			return status;
		}
		else if (retHandle instanceof DHANDLE32) {
			IntByReference retHandle32 = new IntByReference();
			short status = Mem32.OSMemoryAllocate(dwtype, size, retHandle32);
			((DHANDLE32)retHandle).hdl = retHandle32.getValue();
			return status;
		}
		else {
			throw new IllegalArgumentException("Unsupported return handle type: "+retHandle==null ? "null" : retHandle.getClass().getName());
		}
	}
	
	public static Pointer OSLockObject(NotesBlockIdStruct blockId) {
		DHANDLE.ByValue hdlByVal = DHANDLE.newInstanceByValue();
		
		if (PlatformUtils.is64Bit()) {
			((DHANDLE64.ByValue)hdlByVal).hdl = blockId.pool;
		}
		else {
			((DHANDLE32.ByValue)hdlByVal).hdl = blockId.pool;
		}
		
		if (hdlByVal.isNull()) {
			throw new IllegalArgumentException("Null handle cannot be unlocked");
		}
		
		Pointer poolPtr = NotesNativeAPI.get().OSLockObject(hdlByVal);

		int block = blockId.block & 0xffff;
		long poolPtrLong = Pointer.nativeValue(poolPtr) + block;
		return new Pointer(poolPtrLong);
	}

	public static boolean OSUnlockObject(NotesBlockIdStruct blockId) {
		DHANDLE.ByValue hdlByVal = DHANDLE.newInstanceByValue();
		
		if (PlatformUtils.is64Bit()) {
			((DHANDLE64.ByValue)hdlByVal).hdl = blockId.pool;
		}
		else {
			((DHANDLE32.ByValue)hdlByVal).hdl = blockId.pool;
		}
		
		if (hdlByVal.isNull()) {
			throw new IllegalArgumentException("Null handle cannot be unlocked");
		}
		
		return NotesNativeAPI.get().OSUnlockObject(hdlByVal);
	}
	
}
