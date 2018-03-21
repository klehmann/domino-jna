package com.mindoo.domino.jna;

import java.util.Calendar;
import java.util.List;

import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.constants.Search;

/**
 * This is the structure received from NSF searches
 * if both {@link Search#NOITEMNAMES} and {@link Search#SUMMARY} are set 
 * and for view read operations with flag
 * {@link ReadMask#SUMMARYVALUES}.<br>
 * <br>
 * The table contains a list of item data types and value. In contrast to
 * {@link IItemTableData}, the {@link IItemValueTableData} does not contain
 * column item names.
 * 
 * @author Karsten Lehmann
 */
public interface IItemValueTableData {

	/**
	 * Returns the decoded item value, with the following types:<br>
	 * <ul>
	 * <li>{@link NotesItem#TYPE_TEXT} - {@link String}</li>
	 * <li>{@link NotesItem#TYPE_TEXT_LIST} - {@link List} of {@link String}</li>
	 * <li>{@link NotesItem#TYPE_NUMBER} - {@link Double}</li>
	 * <li>{@link NotesItem#TYPE_NUMBER_RANGE} - {@link List} with {@link Double} values for number lists or double[] values for number ranges (not sure if Notes views really supports them)</li>
	 * <li>{@link NotesItem#TYPE_TIME} - {@link Calendar}</li>
	 * <li>{@link NotesItem#TYPE_TIME_RANGE} - {@link List} with {@link Calendar} values for number lists or Calendar[] values for datetime ranges</li>
	 * </ul>
	 * 
	 * @param index item index between 0 and {@link #getItemsCount()}
	 * @return value or null if unknown type
	 */
	public Object getItemValue(int index);
	
	/**
	 * Returns the data type of an item value by its index, e.g. {@link NotesItem#TYPE_TEXT},
	 * {@link NotesItem#TYPE_TEXT_LIST}, {@link NotesItem#TYPE_NUMBER},
	 * {@link NotesItem#TYPE_NUMBER_RANGE}
	 * 
	 * @param index item index between 0 and {@link #getItemsCount()}
	 * @return data type
	 */
	public int getItemDataType(int index);
	
	/**
	 * Returns the number of decoded items
	 * 
	 * @return number
	 */
	public int getItemsCount();
	
	/**
	 * Sets whether methods like {@link #getItemValue(int)} should return {@link NotesTimeDate}
	 * instead of {@link Calendar}.
	 * 
	 * @param b true to prefer NotesTimeDate (false by default)
	 */
	public void setPreferNotesTimeDates(boolean b);
	
	/**
	 * Returns whether methods like {@link #getItemValue(int)} should return {@link NotesTimeDate}
	 * instead of {@link Calendar}.
	 * 
	 * @return true to prefer NotesTimeDate
	 */
	public boolean isPreferNotesTimeDates();
}
