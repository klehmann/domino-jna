package com.mindoo.domino.jna.gc;

/**
 * Interface of memory allocated by/for the C API that requires manual releasing of its handle
 * 
 * @author Karsten Lehmann
 */
public interface IAllocatedMemory {

	/**
	 * Frees the memory, if not already done
	 */
	public void free();
	
	/**
	 * Checks if this memory has already been freed
	 * 
	 * @return true if freed
	 */
	public boolean isFreed();
	
	/**
	 * Returns the main memory handle for 32 bit, used to hash the memory
	 * 
	 * @return handle
	 */
	public int getHandle32();

	/**
	 * Returns the main memory handle for 64 bit, used to hash the memory
	 * 
	 * @return handle
	 */
	public long getHandle64();

}
