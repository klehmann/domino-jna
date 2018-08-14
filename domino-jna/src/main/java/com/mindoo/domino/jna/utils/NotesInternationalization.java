package com.mindoo.domino.jna.utils;

import java.util.ArrayList;
import java.util.List;

import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;

/**
 * Utility to read translated calendar strings
 * 
 * @author Karsten Lehmann
 */
public class NotesInternationalization {
	
	/**
	 * Request for AM String
	 * 
	 * @return AM string
	 */
	public static String getAMString() {
		return getExtIntlFormat(NotesConstants.EXT_AM_STRING, 1).get(0);
	}

	/**
	 * Request for PM String
	 * 
	 * @return PM string
	 */
	public static String getPMString() {
		return getExtIntlFormat(NotesConstants.EXT_PM_STRING, 1).get(0);
	}

	/**
	 * Request for Currency String
	 * 
	 * @return currency string
	 */
	public static String getCurrencyString() {
		return getExtIntlFormat(NotesConstants.EXT_CURRENCY_STRING, 1).get(0);
	}
	
	/**
	 * Request for Month Name
	 * 
	 * @return month names
	 */
	public static List<String> getMonthNames() {
		return getExtIntlFormat(NotesConstants.MONTH_NAMES, 12);
	}

	/**
	 * Request for abbreviated month names
	 * 
	 * @return month names
	 */
	public static List<String> getAbbrMonthNames() {
		return getExtIntlFormat(NotesConstants.ABBR_MONTH_NAMES, 12);
	}

	/**
	 * Request for weekday names
	 * 
	 * @return weekday names
	 */
	public static List<String> getWeekdayNames() {
		return getExtIntlFormat(NotesConstants.WEEKDAY_NAMES, 7);
	}

	/**
	 * Request for abbreviated weekday names
	 * 
	 * @return abbr weekday names
	 */
	public static List<String> getAbbrWeekdayNames() {
		return getExtIntlFormat(NotesConstants.ABBR_WEEKDAY_NAMES, 7);
	}

	private static List<String> getExtIntlFormat(byte item, int itemCount) {
		List<String> values = new ArrayList<String>(itemCount);
		DisposableMemory mem = new DisposableMemory(256);
		try {
			for (byte b=1; b<=itemCount; b++) {
				short txtLen = NotesNativeAPI.get().OSGetExtIntlFormat(item, b, mem, (short)  256);
				values.add(NotesStringUtils.fromLMBCS(mem, txtLen));
			}
			return values;
		}
		finally {
			mem.dispose();
		}
	}
}
