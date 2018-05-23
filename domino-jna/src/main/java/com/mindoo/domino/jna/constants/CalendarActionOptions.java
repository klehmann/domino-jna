package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * {@link CalendarActionOptions} values are used to provide additional processing control to some
 * actions taken on Calendar notices and entries
 * 
 * @author Karsten Lehmann
 */
public enum CalendarActionOptions {
	/** Indicates that a check should be performed when processing the action to determine 
	* if an overwrite of invitee changes to the entry will occur. */
	DO_OVERWRITE_CHECK(NotesConstants.CAL_ACTION_DO_OVERWRITE_CHECK),
	
	/** New in 9.01 release.  Used to indicate that current entry participants should be notified of changes
	 * to the participant list in addition to those being added or removed. */
	UPDATE_ALL_PARTICIPANTS(NotesConstants.CAL_ACTION_UPDATE_ALL_PARTICIPANTS);

	private int m_val;
	
	CalendarActionOptions(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMask(EnumSet<CalendarActionOptions> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (CalendarActionOptions currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
