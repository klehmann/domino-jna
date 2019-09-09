package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Specifies how to format the timezone part of a {@link NotesTimeDate} to a string.
 * 
 * @author Karsten Lehmann
 */
public enum ZoneFormat {
	
	/** all times converted to THIS zone */
	NEVER(NotesConstants.TZFMT_NEVER),
	/** show only when outside this zone */
	SOMETIMES(NotesConstants.TZFMT_SOMETIMES),
	/** show on all times, regardless */
	ALWAYS(NotesConstants.TZFMT_ALWAYS);

	private byte m_val;

	ZoneFormat(byte val) {
		m_val = val;
	}
	
	public byte getValue() {
		return m_val;
	}

}
