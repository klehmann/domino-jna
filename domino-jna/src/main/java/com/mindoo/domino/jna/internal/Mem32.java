package com.mindoo.domino.jna.internal;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * Memory management utility for 32 bit
 * 
 * @author Karsten Lehmann
 */
public class Mem32 {

	public static Pointer OSLockObject(int handle) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be locked");
		return NotesNativeAPI32.get().OSLockObject(handle);
	}
	
	public static boolean OSUnlockObject(int handle) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be unlocked");
		return NotesNativeAPI32.get().OSUnlockObject(handle);
	}
	
	public static short OSMemFree(int handle) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be freed");
		return NotesNativeAPI32.get().OSMemFree(handle);
	}
	
	public static short OSMemGetSize(int handle, IntByReference retSize) {
		if (handle==0)
			throw new IllegalArgumentException("Size cannot be read for null handle");
		return NotesNativeAPI32.get().OSMemGetSize(handle, retSize);
	}

	public static int OSMemoryGetSize(int handle) {
		if (handle==0)
			throw new IllegalArgumentException("Size cannot be read for null handle");
		return NotesNativeAPI32.get().OSMemoryGetSize(handle);
	}

	public static void OSMemoryFree(int handle) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be freed");
		NotesNativeAPI32.get().OSMemoryFree(handle);
	}
	
	public static short OSMemoryReallocate(int handle, int size) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be reallocated");
		return NotesNativeAPI32.get().OSMemoryReallocate(handle, size);
	}

	public static Pointer OSMemoryLock(int handle) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be locked");
		return NotesNativeAPI32.get().OSMemoryLock(handle);
	}

	public static boolean OSMemoryUnlock(int handle) {
		if (handle==0)
			throw new IllegalArgumentException("Null handle cannot be unlocked");
		return NotesNativeAPI32.get().OSMemoryUnlock(handle);
	}

	public static short OSMemAlloc(
			short  BlkType,
			int  dwSize,
			IntByReference retHandle) {
		return NotesNativeAPI32.get().OSMemAlloc(BlkType, dwSize, retHandle);
	}

}
