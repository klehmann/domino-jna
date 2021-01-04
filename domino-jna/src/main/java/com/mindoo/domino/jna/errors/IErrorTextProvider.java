package com.mindoo.domino.jna.errors;

import java.util.Map;

/**
 * Interface to provide fallback error texts
 * 
 * @author Karsten Lehmann
 */
public interface IErrorTextProvider {

	/**
	 * Providers are sorted in ascending priority order in case
	 * this interface will ever be implemented by someone else.
	 * 
	 * @return priority
	 */
	public int getPriority();
	
	/**
	 * Implement this method and return the error texts hashed
	 * by status code. The result will be cached by the caller.
	 * 
	 * @return error texts
	 */
	public Map<Short,String> getErrorTexts();
	
}
