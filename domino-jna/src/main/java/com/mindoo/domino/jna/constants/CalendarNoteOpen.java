package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Flags that control behavior of the calendar APIs - Used when opening a note handle for calendar data.
 * 
 * @author Karsten Lehmann
 */
public enum CalendarNoteOpen {
	
	/**
	 * Used when getting a handle via CalOpenNoteHandle (Handy for read-only cases)<br>
	 * When a specific instance of a recurring entry is requested, the underlying note may
	 * represent multiple instances.<br>
	 * <br>
	 * Default behavior makes appropriate modifications so that the returned handle represents
	 * a single instance (but this might cause notes to be created or modified as a side effect).<br>
	 * <br>
	 * Using {@link #HANDLE_NOSPLIT} will bypass any note creations or modifications and return a
	 * note handle that may represent more than a single instance on the calendar.
	 */
	HANDLE_NOSPLIT(NotesConstants.CAL_NOTEOPEN_HANDLE_NOSPLIT);
	
	private int m_val;
	
	CalendarNoteOpen(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMask(EnumSet<CalendarNoteOpen> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (CalendarNoteOpen currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
