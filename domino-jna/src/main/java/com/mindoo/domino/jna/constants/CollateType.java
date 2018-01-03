package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.NotesCollateDescriptor;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * These are the possible values for the  keytype member of the {@link NotesCollateDescriptor} data structure.<br>
 * The keytype structure member specifies the type of sorting that is done in the specified column in a view.
 * 
 * @author Karsten Lehmann
 */
public enum CollateType {

	/** Collate by key in summary buffer (requires key name string) */
	KEY (NotesConstants.COLLATE_TYPE_KEY),
	/** Collate by note ID */
	NOTEID(NotesConstants.COLLATE_TYPE_NOTEID),
	/** Collate by "tumbler" summary key (requires key name string) */
	TUMBLER(NotesConstants.COLLATE_TYPE_TUMBLER),
	/** Collate by "category" summary key (requires key name string) */
	CATEGORY(NotesConstants.COLLATE_TYPE_CATEGORY);

	private int m_val;
	
	CollateType(int val) {
		m_val = val;
	}
	
	/**
	 * Returns the numeric constant for the collate type
	 * 
	 * @return constant
	 */
	public int getValue() {
		return m_val;
	}

	/**
	 * Converts a numeric constant to a collate type
	 * 
	 * @param value constant
	 * @return collate type
	 */
	public static CollateType toType(int value) {
		if (value == NotesConstants.COLLATE_TYPE_KEY) {
			return CollateType.KEY;
		}
		else if (value == NotesConstants.COLLATE_TYPE_NOTEID) {
			return CollateType.NOTEID;
		}
		else if (value == NotesConstants.COLLATE_TYPE_TUMBLER) {
			return CollateType.TUMBLER;
		}
		else if (value == NotesConstants.COLLATE_TYPE_CATEGORY) {
			return CollateType.CATEGORY;
		}
		else
			throw new IllegalArgumentException("Unknown type constant: "+value);
	}
}
