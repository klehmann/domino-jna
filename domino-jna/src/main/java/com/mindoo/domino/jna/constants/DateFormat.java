package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Specifies how to format the date part of a {@link NotesTimeDate} to a string.
 * 
 * @author Karsten Lehmann
 */
public enum DateFormat {
	
	/** year, month, and day */
	FULL(NotesConstants.TDFMT_FULL),
	/** month and day, year if not this year */
	CPARTIAL(NotesConstants.TDFMT_CPARTIAL),
	/** month and day */
	PARTIAL(NotesConstants.TDFMT_PARTIAL),
	/** year and month */
	DPARTIAL(NotesConstants.TDFMT_DPARTIAL),
	/** year(4digit), month, and day */
	FULL4(NotesConstants.TDFMT_FULL4),
	/** month and day, year(4digit) if not this year */
	CPARTIAL4(NotesConstants.TDFMT_CPARTIAL4),
	/** year(4digit) and month */
	DPARTIAL4(NotesConstants.TDFMT_DPARTIAL4);

	private byte m_val;

	DateFormat(byte val) {
		m_val = val;
	}
	
	public byte getValue() {
		return m_val;
	}

}
