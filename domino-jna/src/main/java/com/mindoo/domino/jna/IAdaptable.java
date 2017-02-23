package com.mindoo.domino.jna;

/**
 * Interface to access internal object structures and get adapter classes
 * 
 * @author Karsten Lehmann
 */
public interface IAdaptable {

	/**
	 * Method to get an adapter for a class
	 * 
	 * @param <T> adapter type
	 * 
	 * @param clazz class
	 * @return adapter or null if not supported
	 */
	public <T> T getAdapter(Class<T> clazz);
	
}
