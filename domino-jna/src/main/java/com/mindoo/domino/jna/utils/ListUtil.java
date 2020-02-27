package com.mindoo.domino.jna.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ListUtil {

	/**
	 * Returns the element at list position <code>list</code> or <code>null</code>,
	 * if the list is not big enough.
	 * 
	 * @param <T> list element type
	 * @param list list
	 * @param index element position
	 * @return element or <code>null</code>
	 */
	public static <T> T getNth(List<T> list, int index) {
		if (list==null)
			return null;
		else if (list.size()>index) {
			T element=list.get(index);
			return element;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Method to return a sublist from a list based on the start index <code>start</code> and
	 * the number of entries to return <code>count</code>. In constrast to {@link List#subList(int, int)},
	 * this implementation is forgiving in case <code>start</code> or <code>start+count</code> is higher than
	 * the actual number of list entries.
	 * 
	 * @param <T> list type
	 * @param list list
	 * @param start start index
	 * @param count number of entries to return
	 * @return sublist, backed by the original list; see {@link List#subList(int, int)} for details
	 */
	public static <T> List<T> subListChecked(List<T> list, int start, int count) {
		if (start > list.size())
			return Collections.emptyList();
		else {
			long startLong = start;
			long countLong = count;
			//make sure we do not exceed Integer.MAX_VALUE
			long sum = Math.min(Integer.MAX_VALUE, startLong+countLong);
			
			return list.subList(start, Math.min(list.size(), (int) sum));
		}
	}

	/**
	 * Checks if a list contains a value ignoring the case
	 * 
	 * @param list list of strings
	 * @param value value to search for
	 * @return true if list contains value
	 */
	public static boolean containsIgnoreCase(List<String> list, String value) {
		if (list==null)
			return false;
		
		for (String currStr : list) {
			if (currStr.equalsIgnoreCase(value))
				return true;
		}
		return false;
	}
	
	/**
	 * Removes empty strings from the list and trims each list entry
	 * 
	 * @param list list
	 * @return trimmed list
	 */
	public static List<String> fullTrim(List<String> list) {
		if (list==null)
			return list;
		
		boolean isRequired = false;
		for (String currStr : list) {
			if ("".equals(currStr) || currStr.startsWith(" ") || currStr.endsWith(" ")) {
				isRequired = true;
				break;
			}
		}
		
		if (!isRequired)
			return list;
		else {
			List<String> trimmedList = new ArrayList<String>(list.size());
			for (String currStr : list) {
				currStr = currStr.trim();
				if (currStr.length()>0) {
					trimmedList.add(currStr);
				}
			}
			return trimmedList;
		}
	}
	
	/**
	 * Removes empty strings from the list
	 * 
	 * @param list list
	 * @return trimmed list
	 */
	public static List<String> trim(List<String> list) {
		if (list==null)
			return list;
		
		boolean isRequired = false;
		for (String currStr : list) {
			if ("".equals(currStr)) {
				isRequired = true;
				break;
			}
		}
		
		if (!isRequired)
			return list;
		else {
			List<String> trimmedList = new ArrayList<String>(list.size()-1);
			for (String currStr : list) {
				if (currStr.length()>0) {
					trimmedList.add(currStr);
				}
			}
			return trimmedList;
		}
	}
	
	/**
	 * Converts a list with strings to lowercase format
	 * 
	 * @param values list with strings
	 * @param loc locale to use for conversion
	 * @return list in lowercase
	 */
	public static List<String> toLowerCase(List<String> values, Locale loc) {
		if (values==null) {
			return null;
		}
		List<String> listInLowercase = new ArrayList<>(values.size());
		for (String currValue : values) {
			listInLowercase.add(currValue.toLowerCase(loc));
		}
		return listInLowercase;
	}
}
