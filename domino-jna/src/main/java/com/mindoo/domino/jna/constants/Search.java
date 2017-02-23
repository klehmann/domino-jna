package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.internal.NotesCAPI;

/**
 * Use these flags in the search_flags parameter to
 * {@link NotesDatabase#search(String, String, EnumSet, int, com.mindoo.domino.jna.structs.NotesTimeDateWrap, com.mindoo.domino.jna.NotesDatabase.ISearchCallback)}
 * to control what the function searches for and what information it returns. These values can be bitwise
 * ORed together to combine functionality.
 * 
 * @author Karsten Lehmann
 */
public enum Search {
	/** Include deleted and non-matching notes in search (ALWAYS "ON" in partial searches!) */
	ALL_VERSIONS(0x0001),
	/** TRUE to return summary buffer with each match */
	SUMMARY(0x0002),
	/** For directory mode file type filtering. If set, "NoteClassMask" is treated as a FILE_xxx mask for directory filtering */
	FILETYPE(0x0004),
	/** Set {@link NotesCAPI#NOTE_CLASS_NOTIFYDELETION} bit of NoteClass for deleted notes */
	NOTIFYDELETIONS(0x0010),
	/** return error if we don't have full privileges */
	ALLPRIVS(0x0040),
	/** Use current session's user name, not server's */
	SESSION_USERNAME(0x0400),
	/** Filter out "Truncated" documents */
	NOABSTRACTS(0x1000),
	/** Search formula applies only to data notes, i.e., others match */
	DATAONLY_FORMULA(0x4000),
	
	/** Full search (as if Since was "1") but exclude DATA notes prior to passed-in Since time */
	FULL_DATACUTOFF(0x02000000),
	
	/** Allow search to return id's only i.e. no summary buffer */
	NOPRIVCHECK(0x0800);
	
	private int m_val;
	
	Search(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static short toBitMask(EnumSet<Search> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (Search currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}
	
	public static int toBitMaskInt(EnumSet<Search> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (Search currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}
}
