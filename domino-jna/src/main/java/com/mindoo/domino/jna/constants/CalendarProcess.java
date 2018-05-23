package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.NotesCalendarActionData;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * {@link CalendarProcess} values are used to define the action taken taken on Calendar notices and entries
 * 
 * @author Karsten Lehmann
 */
public enum CalendarProcess {
	/** Accept (regardless of conflicts)<br>
	 * For Information update notices or confirm notices, this will apply the changes to the relavent
	 * calendar entry.<br>
	 * Used by the organizer to accept a counter proposal.
	 */
	ACCEPT(NotesConstants.CAL_PROCESS_ACCEPT),
	/** Tentatively accept (regardless of conflicts) */
	TENTATIVE(NotesConstants.CAL_PROCESS_TENTATIVE),
	/** Decline<br>
	 * Can be used by the organizer to decline a counter if done from a counter notice */
	DECLINE(NotesConstants.CAL_PROCESS_DECLINE),
	/** Delegate to {@link NotesCalendarActionData#setDelegateTo(String)} */
	DELEGATE(NotesConstants.CAL_PROCESS_DELEGATE),
	/** Counter to a new time (requires populating {@link NotesCalendarActionData#setChangeToStart(com.mindoo.domino.jna.NotesTimeDate)} / {@link NotesCalendarActionData#setChangeToEnd(com.mindoo.domino.jna.NotesTimeDate)} values) */
	COUNTER(NotesConstants.CAL_PROCESS_COUNTER),
	/** Request updated information from the organizer for this meeting.
	 * Also used by the organizer to respond to a request for updated info. */
	REQUESTINFO(NotesConstants.CAL_PROCESS_REQUESTINFO),
	/** This will process a cancelation notice, removing the meeting from the calendar */
	REMOVECANCEL(NotesConstants.CAL_PROCESS_REMOVECANCEL),
	/** This will physically delete a meeting from the calendar.  This will NOT send notices out */
	DELETE(NotesConstants.CAL_PROCESS_DELETE),
	/** This will remove the meeting or appointment from the calendar and send notices if 
	 * necessary.<br>
	 * It is treated as a {@link #CANCEL} if the entry is a meeting the mailfile 
	 * owner is the organizer of.<br>
	 * It is treated as a {@link #DECLINE} if the entry is a meeting that the mailfile
	 * owner is not the organizer of except when the entry is a broadcast.  In that case it
	 * is treated as a {@link #DELETE}.<br>
	 * It is treated as a {@link #DELETE} if the entry is a non-meeting */
	SMARTREMOVE(NotesConstants.CAL_PROCESS_SMARTREMOVE),
	/** This will cancel a meeting that the mailfile owner is the organizer of */
	CANCEL(NotesConstants.CAL_PROCESS_CANCEL),
	/** This will update the invitee lists on the specified entry (or entries) to include or remove
	 * those users specified in lists contained in the {@link NotesCalendarActionData#setAddNamesReq(java.util.List)} etc. and 
	 * {@link NotesCalendarActionData#setRemoveNames(java.util.List)} values */
	UPDATEINVITEES(NotesConstants.CAL_PROCESS_UPDATEINVITEES);

	private int m_val;
	
	CalendarProcess(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMask(EnumSet<CalendarProcess> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (CalendarProcess currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
