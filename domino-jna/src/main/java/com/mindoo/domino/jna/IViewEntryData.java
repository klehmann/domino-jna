package com.mindoo.domino.jna;

import com.mindoo.domino.jna.constants.ReadMask;

/**
 * Interface to access data of a view entry
 */
public interface IViewEntryData extends INoteSummary {

	int getNoteId();
	
	String getNoteIdAsHex();
	
	String getUNID();
	
	int getSiblingCount();
	
	int getChildCount();
	
	int getDescendantCount();
	
	String getPositionStr();
	
	int[] getPosition();
	
	/**
	 * Method to check whether the entry is a document. Only returns a value if {@link ReadMask#NOTEID}
	 * is used for the lookup
	 * 
	 * @return true if document
	 */
	boolean isDocument();
	
	/**
	 * Method to check whether the entry is a category. Only returns a value if {@link ReadMask#NOTEID}
	 * is used for the lookup
	 * 
	 * @return true if category
	 */
	boolean isCategory();
	
	/**
	 * Method to check whether the entry is a total value. Only returns a value if {@link ReadMask#NOTEID}
	 * is used for the lookup
	 * 
	 * @return true if total
	 */
	boolean isTotal();
	
	Object get(String columnNameOrTitle);
	
	/**
	 * Returns the level of the entry in the view (0 for first level)
	 * 
	 * @return level
	 */
	int getLevel();
	
	/**
	 * For category entries where the category contains a "\" character, this method returns the
	 * index of the category entry (e.g. 1 for "level2" in the string "level1\level2").
	 * 
	 * @return indent level
	 */
	int getIndentLevels();
	
}
