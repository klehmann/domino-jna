package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Specifies the structure of {@link NotesTimeDate} value when converted to a string
 * 
 * @author Karsten Lehmann
 */
public enum DateTimeStructure {
	
	/** DATE */
	DATE(NotesConstants.TSFMT_DATE),
	/** TIME */
	TIME(NotesConstants.TSFMT_TIME),
	/** DATE TIME */
	DATETIME(NotesConstants.TSFMT_DATETIME),
	/** DATE TIME or TIME Today or TIME Yesterday */
	CDATETIME(NotesConstants.TSFMT_CDATETIME),
	/** DATE, Today or Yesterday */ 
	CDATE(NotesConstants.TSFMT_CDATE);
	
	private byte m_val;

	DateTimeStructure(byte val) {
		m_val = val;
	}
	
	public byte getValue() {
		return m_val;
	}

}
