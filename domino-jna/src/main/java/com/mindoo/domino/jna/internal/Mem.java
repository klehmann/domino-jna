package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.DHANDLE32;
import com.mindoo.domino.jna.internal.handles.DHANDLE64;
import com.mindoo.domino.jna.internal.handles.HANDLE;
import com.mindoo.domino.jna.internal.handles.HANDLE32;
import com.mindoo.domino.jna.internal.handles.HANDLE64;
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
	
	public static short OSMemFree(DHANDLE.ByValue hdl) {
		if (hdl==null || hdl.isNull()) {
			throw new IllegalArgumentException("Null handle cannot be freed");
		}
		short result = NotesNativeAPI.get().OSMemFree(hdl);
		if (result==0) {
			hdl.setDisposed();
		}
		return result;
	}

	public static short OSMemFree(HANDLE.ByValue hdl) {
		if (hdl==null || hdl.isNull()) {
			throw new IllegalArgumentException("Null handle cannot be freed");
		}
		
		DHANDLE.ByValue hdlByVal = DHANDLE.newInstanceByValue();
		
		if (PlatformUtils.is64Bit()) {
			((DHANDLE64.ByValue)hdlByVal).hdl = ((HANDLE64.ByValue)hdl).hdl;
		}
		else {
			((DHANDLE32.ByValue)hdlByVal).hdl = ((HANDLE32.ByValue)hdl).hdl;
		}
		
		short result = NotesNativeAPI.get().OSMemFree(hdlByVal);
		if (result==0) {
			hdlByVal.setDisposed();
		}
		return result;
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
	
	public static void OSMemoryAllocate(int dwtype, int size, IntByReference retHandle) {
		short status = NotesNativeAPI.get().OSMemoryAllocate(dwtype, size, retHandle);
		NotesErrorUtils.checkResult(status);
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

	public static LockedMemory OSMemoryLock(int handle) { 
		return OSMemoryLock(handle, false);
	}

	public static LockedMemory OSMemoryLock(int handle, boolean freeAfterClose) {
		return new LockedMemory32(NotesNativeAPI.get().OSMemoryLock(handle), handle, freeAfterClose);
	}
	
	public static LockedMemory OSMemoryLock(long handle) { 
		return OSMemoryLock(handle, false);
	}
	
	public static LockedMemory OSMemoryLock(long handle, boolean freeAfterClose) {
		return new LockedMemory64(NotesNativeAPI.get().OSMemoryLock(handle), handle, freeAfterClose);
	}

	public interface LockedMemory extends AutoCloseable {
		Pointer getPointer();
		long getSize();
		@Override void close();
	}
	
	public static class NullLockedMemory implements LockedMemory {
		@Override
		public Pointer getPointer() {
			return null;
		}
		
		@Override
		public long getSize() {
			return 0;
		}
		
		@Override
		public void close() {
			// nothing to close
		}
	}
	
	private static class LockedMemory32 implements LockedMemory {
		private final Pointer pointer;
		private final int handle;
		private final boolean freeAfterClose;
		
		public LockedMemory32(Pointer pointer, int handle, boolean freeAfterClose) {
			this.pointer = pointer;
			this.handle = handle;
			this.freeAfterClose=freeAfterClose;
		}
		
		@Override
		public Pointer getPointer() {
			return pointer;
		}
		
		@Override
		public long getSize() {
			return NotesNativeAPI.get().OSMemoryGetSize(handle);
		}
		
		@Override
		public void close() {
			try {
				NotesNativeAPI.get().OSMemoryUnlock(handle);
			}
			finally {
				if (freeAfterClose) {
					NotesNativeAPI.get().OSMemoryFree(handle);
				}
			}
		}
	}
	
	private static class LockedMemory64 implements LockedMemory {
		private final Pointer pointer;
		private final long handle;
		private final boolean freeAfterClose;
		
		public LockedMemory64(Pointer pointer, long handle, boolean freeAfterClose) {
			this.pointer = pointer;
			this.handle = handle;
			this.freeAfterClose=freeAfterClose;
		}
		
		@Override
		public Pointer getPointer() {
			return pointer;
		}
		
		@Override
		public long getSize() {
			return NotesNativeAPI.get().OSMemoryGetSize(handle);
		}

		@Override
		public void close() {
			try {
				NotesNativeAPI.get().OSMemoryUnlock(handle);
			}
			finally {
				if (freeAfterClose) {
					NotesNativeAPI.get().OSMemoryFree(handle);
				}
			}
		}
	}
	
	public static LockedMemory OSMemoryLock(DHANDLE.ByReference hdl, boolean freeAfterClose) {
		DHANDLE.ByValue hdlByVal = hdl.getByValue();
		if (!hdlByVal.isNull()) {
			if (PlatformUtils.is64Bit()) {
				return OSMemoryLock(((DHANDLE64.ByValue)hdlByVal).hdl, freeAfterClose);
			}
			else {
				return OSMemoryLock(((DHANDLE32.ByValue)hdlByVal).hdl, freeAfterClose);
			}
		}
		else {
			return new NullLockedMemory();
		}
	}

	public static boolean OSMemoryUnlock(int handle) {
		return NotesNativeAPI.get().OSMemoryUnlock(handle);
	}
	
	public static boolean OSMemoryUnlock(long handle) {
		return NotesNativeAPI.get().OSMemoryUnlock(handle);
	}
	
	public static void OSMemoryFree(int handle) {
		NotesNativeAPI.get().OSMemoryFree(handle);
	}
	
	public static void OSMemoryFree(long handle) {
		NotesNativeAPI.get().OSMemoryFree(handle);
	}

	public static int OSMemoryGetSize(int handle) {
		return NotesNativeAPI.get().OSMemoryGetSize(handle);
	}
	
	public static int OSMemoryGetSize(long handle) {
		return NotesNativeAPI.get().OSMemoryGetSize(handle);
	}

	public static short OSMemRealloc(DHANDLE.ByValue handle, int newSize) {
		return NotesNativeAPI.get().OSMemRealloc(handle, newSize);
	}

	public static LockedMemory OSMemoryLock(DHANDLE.ByReference hdl) {
		return OSMemoryLock(hdl, false);
	}
	
}
