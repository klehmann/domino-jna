package com.mindoo.domino.jna.gc;

/**
 * Interface of C API objects that require manual releasing of their handle
 * 
 * @author Karsten Lehmann
 */
public interface IRecyclableNotesObject {

	/**
	 * Recycles the object, if not already done
	 */
	public void recycle();
	
	/**
	 * Checks if this object has already been recycled
	 * 
	 * @return true if recycled
	 */
	public boolean isRecycled();
	
	/**
	 * Returns the main object handle for 32 bit, used to hash the object
	 * 
	 * @return handle
	 */
	public int getHandle32();

	/**
	 * Returns the main object handle for 64 bit, used to hash the object
	 * 
	 * @return handle
	 */
	public long getHandle64();

}
