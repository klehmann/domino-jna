package com.mindoo.domino.jna.richtext.conversion;

import com.mindoo.domino.jna.richtext.ICompoundText;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;

/**
 * Interface for a conversion class that transforms richtext structures
 * 
 * @author Karsten Lehmann
 */
public interface IRichTextConversion {

	
	/**
	 * Method is called before starting the CD record traversal. Can be used
	 * to reset variables, e.g. to track if we are within a BEGIN/END block.
	 */
	public void richtextNavigationStart();
	
	/**
	 * Method to check whether the richtext item actually requires a conversion
	 * 
	 * @param nav richtext navigator
	 * @return true if conversion is required
	 */
	public boolean isMatch(IRichTextNavigator nav);
	
	/**
	 * Method to do the actual conversion, e.g. traversing the CD records of the specified
	 * {@link IRichTextNavigator} and writing the resulting output to the <code>target</code>.
	 * 
	 * @param source source richtext navigator
	 * @param target target to write conversion result
	 */
	public void convert(IRichTextNavigator source, ICompoundText<?> target);
	
	/**
	 * Method is called when the richtext navigation is done
	 */
	public void richtextNavigationEnd();

}
