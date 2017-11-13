package com.mindoo.domino.jna.html;

/**
 * Returns information about one component of a URL found in the richtext-HTML conversion result.
 * 
 * @author Karsten Lehmann
 *
 * @param <T> value type
 */
public interface IHtmlApiUrlTargetComponent<T> {

	/**
	 * Returns the type of url component
	 * 
	 * @return type
	 */
	public TargetType getType();
	
	/**
	 * Returns the class of the value returned by {@link #getValue()}, e.g. String for
	 * {@link TargetType#FIELD} or {@link TargetType#FIELDOFFSET}
	 * 
	 * @return value class
	 */
	public Class<T> getValueClass();
	
	/**
	 * Returns the value of the URL component
	 * 
	 * @return value
	 */
	public T getValue();
	
}
