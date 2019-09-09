package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Specifies how to format the time part of a {@link NotesTimeDate} to a string.
 * 
 * @author Karsten Lehmann
 */
public enum TimeFormat {
	
	/** hour, minute, and second */
	FULL(NotesConstants.TTFMT_FULL),
	/** hour and minute */
	PARTIAL(NotesConstants.TTFMT_PARTIAL),
	/** hour */
	HOUR(NotesConstants.TTFMT_HOUR),
	/** hour, minute, second, hundredths (max resolution). This currently works only for time-to-text conversion! */
	FULL_MAX(NotesConstants.TTFMT_FULL_MAX);

	private byte m_val;

	TimeFormat(byte val) {
		m_val = val;
	}
	
	public byte getValue() {
		return m_val;
	}

}
