package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Flags that control behavior of the calendar APIs that return iCalendar data for an entry or notice
 * 
 * @author Karsten Lehmann
 */
public enum CalendarRangeRepeat {
	/** Modifying just this instance */
	CURRENT(NotesConstants.RANGE_REPEAT_CURRENT),
	/** Modifying all instances */
	ALL(NotesConstants.RANGE_REPEAT_ALL),
	/** Modifying current + previous */
	PREV(NotesConstants.RANGE_REPEAT_PREV),
	/** Modifying current + future */
	FUTURE(NotesConstants.RANGE_REPEAT_FUT);
	
	private int m_val;
	
	CalendarRangeRepeat(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}

}
