package com.mindoo.domino.jna.internal;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

/**
 * Memory management utility for 64 bit
 * 
 * @author Karsten Lehmann
 */
public class Mem64 {

	public static Pointer OSLockObject(long handle) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be locked");
		return NotesNativeAPI64.get().OSLockObject(handle);
	}
	
	public static boolean OSUnlockObject(long handle) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be unlocked");
		return NotesNativeAPI64.get().OSUnlockObject(handle);
	}
	
	public static short OSMemFree(long handle) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be freed");
		return NotesNativeAPI64.get().OSMemFree(handle);
	}
	
	public static short OSMemGetSize(long handle, IntByReference retSize) {
		if (handle==0)
			throw new IllegalArgumentException("Size cannot be read for null handle");
		return NotesNativeAPI64.get().OSMemGetSize(handle, retSize);
	}

	public static int OSMemoryGetSize(long handle) {
		if (handle==0)
			throw new IllegalArgumentException("Size cannot be read for null handle");
		return NotesNativeAPI64.get().OSMemoryGetSize(handle);
	}

	public static void OSMemoryFree(long handle) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be freed");
		NotesNativeAPI64.get().OSMemoryFree(handle);
	}
	
	public static short OSMemoryReallocate(long handle, int size) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be reallocated");
		return NotesNativeAPI64.get().OSMemoryReallocate(handle, size);
	}

	public static Pointer OSMemoryLock(long handle) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be locked");
		return NotesNativeAPI64.get().OSMemoryLock(handle);
	}

	public static boolean OSMemoryUnlock(long handle) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be unlocked");
		return NotesNativeAPI64.get().OSMemoryUnlock(handle);
	}
	
	public static short OSMemAlloc(
			short  BlkType,
			int  dwSize,
			LongByReference retHandle) {
		return NotesNativeAPI64.get().OSMemAlloc(BlkType, dwSize, retHandle);
	}

	public static short OSMemGetTyoe(long handle) {
		return NotesNativeAPI64.get().OSMemGetType(handle);
	}

}
