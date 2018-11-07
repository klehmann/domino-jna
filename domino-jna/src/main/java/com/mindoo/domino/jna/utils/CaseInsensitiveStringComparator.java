package com.mindoo.domino.jna.utils;

import java.util.Comparator;

/**
 * {@link Comparator} that compares two strings without case
 * 
 * @author Karsten Lehmann
 */
public class CaseInsensitiveStringComparator implements Comparator<String> {

	@Override
	public int compare(String o1, String o2) {
		return o1.compareToIgnoreCase(o2);
	}

}
