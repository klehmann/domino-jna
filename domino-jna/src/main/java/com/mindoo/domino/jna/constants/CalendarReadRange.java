package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Flags that control behavior of the calendar APIs that return iCalendar data for an entry or notice
 * 
 * @author Karsten Lehmann
 */
public enum CalendarReadRange {

	DTSTART (1, NotesConstants.READ_RANGE_MASK_DTSTART),
	DTEND (1, NotesConstants.READ_RANGE_MASK_DTEND),
	DTSTAMP (1, NotesConstants.READ_RANGE_MASK_DTSTAMP),
	SUMMARY (1, NotesConstants.READ_RANGE_MASK_SUMMARY),
	CLASS (1, NotesConstants.READ_RANGE_MASK_CLASS),
	PRIORITY (1, NotesConstants.READ_RANGE_MASK_PRIORITY),
	RECURRENCE_ID (1, NotesConstants.READ_RANGE_MASK_RECURRENCE_ID),
	SEQUENCE (1, NotesConstants.READ_RANGE_MASK_SEQUENCE),
	LOCATION (1, NotesConstants.READ_RANGE_MASK_LOCATION),
	TRANSP (1, NotesConstants.READ_RANGE_MASK_TRANSP),
	CATEGORY (1, NotesConstants.READ_RANGE_MASK_CATEGORY),
	APPTTYPE (1, NotesConstants.READ_RANGE_MASK_APPTTYPE),
	NOTICETYPE (1, NotesConstants.READ_RANGE_MASK_NOTICETYPE),
	STATUS (1, NotesConstants.READ_RANGE_MASK_STATUS),
	/** Includes online meeting URL as well as any online meeting password or conf ID */
	ONLINE_URL (1, NotesConstants.READ_RANGE_MASK_ONLINE_URL),
	/** Note: For performance reasons, the organizer may not be stored in
	 * ORGANIZER but rather in X-LOTUS-ORGANIZER to avoid lookups necessary
	 * to get the internet address. */
	NOTESORGANIZER (1, NotesConstants.READ_RANGE_MASK_NOTESORGANIZER),
	/** Note: For performance reasons, the organizer may not be stored in PARTICIPANT but
	 * rather in X-LOTUS-ROOM to avoid lookups necessary to get the internet address. */
	NOTESROOM (1, NotesConstants.READ_RANGE_MASK_NOTESROOM),
	/** Output alarm information for this entry */
	ALARM (1, NotesConstants.READ_RANGE_MASK_ALARM),

	/** X-LOTUS-HASATTACH is set to 1 if there are any file attachments for this entry */
	HASATTACH (2, NotesConstants.READ_RANGE_MASK2_HASATTACH),
	/**
	 * X-LOTUS-UNID will always be set for notices (as it is used as the identifier for
	 * a notice), but setting this flag will also set X-LOTUS-UNID for calendar entries,
	 * where this will be set with the UNID of the note that currently contains this
	 * instance (can be used to construct a URL to open the instance in Notes, for instance)
	 */
	UNID (2, NotesConstants.READ_RANGE_MASK2_UNID);

	private int m_maskNr;
	private int m_val;

	CalendarReadRange(int maskNr, int val) {
		m_maskNr = maskNr;
		m_val = val;
	}

	public int getMaskNr() {
		return m_maskNr;
	}

	public int getValue() {
		return m_val;
	}

	public static int toBitMask(EnumSet<CalendarReadRange> flagSet) {
		int result = 0;
		if (flagSet!=null) {
			for (CalendarReadRange currFlag : values()) {
				if (currFlag.getMaskNr()==1) {
					if (flagSet.contains(currFlag)) {
						result = result | currFlag.getValue();
					}
				}
			}
		}
		return result;
	}

	public static int toBitMask2(EnumSet<CalendarReadRange> flagSet) {
		int result = 0;
		if (flagSet!=null) {
			for (CalendarReadRange currFlag : values()) {
				if (currFlag.getMaskNr()==2) {
					if (flagSet.contains(currFlag)) {
						result = result | currFlag.getValue();
					}
				}
			}
		}
		return result;
	}

}
