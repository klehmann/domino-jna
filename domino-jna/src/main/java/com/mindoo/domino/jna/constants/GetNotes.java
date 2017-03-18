package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

/**
 * Control flags for NSFDbGetNotes.
 * 
 * @author Karsten Lehmann
 */
public enum GetNotes {
	
	/** Preserve order of notes in NoteID list */
	PRESERVE_ORDER(0x00000001),
	
	/** Send (copiable) objects along with note */
	SEND_OBJECTS(0x00000002),
	
	/** Order returned notes by (approximate) ascending size */
	ORDER_BY_SIZE(0x00000004),
	
	/** Continue to next on list if error encountered */
	CONTINUE_ON_ERROR(0x00000008),
	
	/** Enable folder-add callback function after the note-level callback */
	GET_FOLDER_ADDS(0x00000010),
	
	/** Apply folder ops directly - don't bother using callback */
	APPLY_FOLDER_ADDS(0x00000020),

	/** Don't stream - used primarily for testing purposes */
	NO_STREAMING(0x00000040);
	

	
	private int m_val;
	
	GetNotes(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMask(EnumSet<GetNotes> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (GetNotes currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
