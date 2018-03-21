package com.mindoo.domino.jna;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.utils.LMBCSString;

/**
 * This is the structure used for item (field) summary buffers, received from NSF searches
 * if {@link Search#SUMMARY} is used and for view read operations with flag
 * {@link ReadMask#SUMMARY}.<br>
 * <br>
 * The table contains a list of item names with their data type and value.
 * 
 * @author Karsten Lehmann
 */
public interface IItemTableData extends IItemValueTableData {

	/**
	 * Method to check whether the table contains the specified item
	 * 
	 * @param itemName item name
	 * @return true if item exists
	 */
	public boolean has(String itemName);
	
	/**
	 * Returns the names of the decoded items (programmatic column names in case of collection data)
	 * 
	 * @return names
	 */
	public String[] getItemNames();
	
	/**
	 * Returns a single value by its programmatic column name
	 * 
	 * @param itemName item name, case insensitive
	 * @return value or null
	 */
	public Object get(String itemName);
	
	/**
	 * Convenience function that converts a summary value to a string
	 * 
	 * @param itemName item name, case insensitive
	 * @param defaultValue default value if value is empty or is not a string
	 * @return string value or null
	 */
	public String getAsString(String itemName, String defaultValue);
	
	/**
	 * Convenience function that converts a summary value to an abbreviated name
	 * 
	 * @param itemName item name, case insensitive
	 * @return name or null
	 */
	public String getAsNameAbbreviated(String itemName);
	
	/**
	 * Convenience function that converts a summary value to an abbreviated name
	 * 
	 * @param itemName item name, case insensitive
	 * @param defaultValue value to be used of item not found
	 * @return name or default value
	 */
	public String getAsNameAbbreviated(String itemName, String defaultValue);
	
	/**
	 * Convenience function that converts a summary value to a list of abbreviated names
	 * 
	 * @param itemName item name, case insensitive
	 * @return names or null
	 */
	public List<String> getAsNamesListAbbreviated(String itemName);
	
	/**
	 * Convenience function that converts a summary value to a list of abbreviated names
	 * 
	 * @param itemName item name, case insensitive
	 * @param defaultValue default value if column is empty or is not a string or string list
	 * @return names or default value if not found
	 */
	public List<String> getAsNamesListAbbreviated(String itemName, List<String> defaultValue);
	
	/**
	 * Convenience function that converts a summary value to a string list
	 * 
	 * @param itemName item name, case insensitive
	 * @param defaultValue default value if column is empty or is not a string or string list
	 * @return string list value or null
	 */
	public List<String> getAsStringList(String itemName, List<String> defaultValue);
	
	/**
	 * Convenience function that converts a summary value to a {@link Calendar}
	 * 
	 * @param itemName item name, case insensitive
	 * @param defaultValue default value if column is empty or is not a Calendar
	 * @return calendar value or null
	 */
	public Calendar getAsCalendar(String itemName, Calendar defaultValue);
	
	/**
	 * Convenience function that converts a summary value to a {@link NotesTimeDate}
	 * 
	 * @param itemName item name, case insensitive
	 * @param defaultValue default value if column is empty or is not a NotesTimeDate
	 * @return NotesTimeDate value or null
	 */
	public NotesTimeDate getAsTimeDate(String itemName, NotesTimeDate defaultValue);
	
	/**
	 * Convenience function that converts a summary value to a {@link Calendar} list
	 * 
	 * @param itemName item name, case insensitive
	 * @param defaultValue default value if column is empty or is not a Calendar
	 * @return calendar list value or null
	 */
	public List<Calendar> getAsCalendarList(String itemName, List<Calendar> defaultValue);
	
	/**
	 * Convenience function that converts a summary value to a {@link NotesTimeDate} list
	 * 
	 * @param itemName item name, case insensitive
	 * @param defaultValue default value if column is empty or is not a NotesTimeDate list
	 * @return NotesTimeDate list value or null
	 */
	public List<NotesTimeDate> getAsTimeDateList(String itemName, List<NotesTimeDate> defaultValue);
	
	/**
	 * Convenience function that converts a summary value to a double
	 * 
	 * @param itemName item name, case insensitive
	 * @param defaultValue default value if column is empty or is not a number
	 * @return double
	 */
	public Double getAsDouble(String itemName, Double defaultValue);
	
	/**
	 * Convenience function that converts a summary value to a double
	 * 
	 * @param itemName item name, case insensitive
	 * @param defaultValue default value if column is empty or is not a number
	 * @return integer
	 */
	public Integer getAsInteger(String itemName, Integer defaultValue);
	
	/**
	 * Convenience function that converts a summary value to a double list
	 * 
	 * @param itemName item name, case insensitive
	 * @param defaultValue default value if column is empty or is not a number
	 * @return double list
	 */
	public List<Double> getAsDoubleList(String itemName, List<Double> defaultValue);
	
	/**
	 * Convenience function that converts a summary value to a integer list
	 * 
	 * @param itemName item name, case insensitive
	 * @param defaultValue default value if column is empty or is not a number
	 * @return integer list
	 */
	public List<Integer> getAsIntegerList(String itemName, List<Integer> defaultValue);
	
	/**
	 * Converts the values to a Java {@link Map}
	 * 
	 * @return data as map
	 */
	public Map<String,Object> asMap();
	
	/**
	 * Converts the values to a Java {@link Map}
	 * 
	 * @param decodeLMBCS true to convert {@link LMBCSString} objects and lists to Java Strings
	 * @return data as map
	 */
	public Map<String,Object> asMap(boolean decodeLMBCS);
	
	/**
	 * @deprecated internal method, no need to call this in client code
	 */
	@Deprecated
	public void free();
	
	/**
	 * @deprecated internal method, no need to call this in client code
	 * 
	 * @return true if freed
	 */
	@Deprecated
	public boolean isFreed();
	
}
