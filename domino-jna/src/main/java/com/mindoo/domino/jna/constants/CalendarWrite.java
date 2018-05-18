package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Flags that control behavior of the calendar APIs - Used when APIS take iCalendar input to modify calendar data
 * 
 * @author Karsten Lehmann
 */
public enum CalendarWrite {
	
	/**
	 * Used when APIs modify entry data via CalUpdateEntry.<br>
	 * This flag means that NO data is preserved from the original entry and the resulting entry is 100%
	 * a product of the iCalendar passed in.<br>
	 * NOTE: When this flag is NOT used, some content may be preserved during an update if that particular
	 * content was not included in the iCalendar input.<br>
	 * This includes:<br>
	 * <ul>
	 * <li>Body</li>
	 * <li>Attachments</li>
	 * <li>Custom data properties as specified in $CSCopyItems</li>
	 * </ul>
	 */
	COMPLETEREPLACE(NotesConstants.CAL_WRITE_COMPLETE_REPLACE),
	
	/**
	 * Used when APIs create or modify calendar entries where the organizer is the mailfile owner.<br>
	 * When a calendar entry is modified with {@link #DISABLE_IMPLICIT_SCHEDULING} set, no notices
	 * are sent (invites, updates, reschedules, cancels, etc)<br>
	 * <br>
	 * Note: This is not intended for cases where you are saving a meeting as a draft (since there
	 * is currently not a capability to then send it later.  It will also not allow some notices to
	 * go out but other notices not to go out (such as, send invites to added invitees but dont send
	 * updates to existing invitees).<br>
	 * Rather, this is targeted at callers that prefer to be responsible for sending out notices themselves
	 * through a separate mechanism
	 */
	DISABLE_IMPLICIT_SCHEDULING(NotesConstants.CAL_WRITE_DISABLE_IMPLICIT_SCHEDULING),
	
	/**
	 * Used when APIs create or modify entries on the calendar<br>
	 * This will allow creation/modification of calendar entries, even if the database is not a mailfile
	 */
	IGNORE_VERIFY_DB(NotesConstants.CAL_WRITE_IGNORE_VERIFY_DB),
	
	/**
	 * By default, alarms will be created on calendar entries based on VALARM content of iCalendar input.<br>
	 * Use of this flag will disregard VALARM information in the iCalendar and use the user's default
	 * alarm settings for created or updated entries.
	 */
	USE_ALARM_DEFAULTS(NotesConstants.CAL_WRITE_USE_ALARM_DEFAULTS);
	
	private int m_val;
	
	CalendarWrite(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMask(EnumSet<CalendarWrite> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (CalendarWrite currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
