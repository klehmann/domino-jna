package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.utils.NotesCalendarUtils;

/**
 * Flags that control behavior of the calendar APIs that return iCalendar data for an entry or notice
 * 
 * @author Karsten Lehmann
 */
public enum CalendarRead {
	
	/**
	 * Used when APIs generate iCalendar<br>
	 * <br>
	 * By default, some X-LOTUS properties and parameters will be included in iCalendar data
	 * returned by these APIs.<br>
	 * {@link #HIDE_X_LOTUS} causes all X-LOTUS properties and parameters to be removed from
	 * the generated iCalendar data.<br>
	 * <br>Note: This overrides {@link #INCLUDE_X_LOTUS}
	 */
	HIDE_X_LOTUS(NotesConstants.CAL_READ_HIDE_X_LOTUS),
	
	/**
	 * Used when APIs generate iCalendar<br>
	 * <br>
	 * Include all Lotus specific properties like X-LOTUS-UPDATE-SEQ, X-LOTUS-UPDATE_WISL, etc
	 * in the generated iCalendar data.<br>
	 * These properties are NOT included by default in any iCalendar data returned by the APIs.<br>
	 * <br>
	 * Caution: Unless the caller knows how to use these it can be dangerous since their
	 * presence will be honored and can cause issues if not updated properly.<br>
	 * Ignored if {@link #HIDE_X_LOTUS} is also specified.
	 */
	INCLUDE_X_LOTUS(NotesConstants.CAL_READ_INCLUDE_X_LOTUS),
	
	/**
	 * RESERVED: This functionality is not currently in plan<br>
	 * When generating ATTENDEE info in {@link NotesCalendarUtils#readCalendarEntry(com.mindoo.domino.jna.NotesDatabase, String, String, EnumSet)},
	 * determine and populate response Status (which might be a performance hit)
	 */
	SKIP_RESPONSE_DATA(NotesConstants.CAL_READ_SKIP_RESPONSE_DATA);
	
	private int m_val;
	
	CalendarRead(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMask(EnumSet<CalendarRead> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (CalendarRead currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
