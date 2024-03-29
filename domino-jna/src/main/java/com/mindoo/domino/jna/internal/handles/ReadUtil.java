package com.mindoo.domino.jna.internal.handles;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.Mem;
import com.sun.jna.Pointer;

/**
 * Utility methods to access memory associated with a handle.
 * 
 * @author Tammo Riedinger
 */
public class ReadUtil {

	/**
	 * Method to lock the given handle and retrieve the memory pointer to access the data.
	 * The pointer will be handed to the callback which can then extract the data safely.
	 *  
	 * @param <R>		the type of result to be generated by the callback
	 * @param handle	the memory-handle
	 * @param autoFree	true, if the memory held by the handle should be freed after this operation
	 * @param callback		the callback to read the data
	 * @return			the data as created by the callback
	 */
	public static <R> R accessMemory(DHANDLE handle, boolean autoFree, final MemoryAccess<R> callback) {
		if (!handle.isNull()) {
			Pointer ptr = Mem.OSLockObject(handle);
			try {
				return callback.access(ptr);
			}
			finally {
				Mem.OSUnlockObject(handle);
				
				if (autoFree) {
					NotesErrorUtils.checkResult(
						Mem.OSMemFree(handle.getByValue())
					);
				}
			}
		}
		return callback.handleIsNull();
	}
	
	/**
	 * Callback interface to ensure exclusive handle access across threads.
	 * 
	 * @author Tammo Riedinger
	 *
	 * @param <R> result type
	 */
	public interface MemoryAccess<R> {

		/**
		 * Implement this method with code reading data from the memory pointer
		 * 
		 * @param ptr 	the memory pointer
		 * @return result
		 */
		R access(Pointer ptr);
		
		/**
		 * This method is called, when the given handle contained a null value.
		 * Defaults to null
		 * 
		 * @return		the result
		 */
		default R handleIsNull() {
			return null;
		}
	}

}
