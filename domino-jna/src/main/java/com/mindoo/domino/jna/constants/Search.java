package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Use these flags in the search_flags parameter to
 * {@link NotesDatabase#search(String, String, EnumSet, EnumSet, com.mindoo.domino.jna.NotesTimeDate, com.mindoo.domino.jna.NotesDatabase.SearchCallback)}
 * to control what the function searches for and what information it returns. These values can be bitwise
 * ORed together to combine functionality.
 * 
 * @author Karsten Lehmann
 */
public enum Search {
	/** Include deleted and non-matching notes in search (ALWAYS "ON" in partial searches, which are searches using a since date!) */
	ALL_VERSIONS(NotesConstants.SEARCH_ALL_VERSIONS),
	/** TRUE to return summary buffer with each match */
	SUMMARY(NotesConstants.SEARCH_SUMMARY),
	/** For directory mode file type filtering. If set, "NoteClassMask" is treated as a FILE_xxx mask for directory filtering */
	FILETYPE(NotesConstants.SEARCH_FILETYPE),
	/** Set {@link NotesConstants#NOTE_CLASS_NOTIFYDELETION} bit of NoteClass for deleted notes */
	NOTIFYDELETIONS(NotesConstants.SEARCH_NOTIFYDELETIONS),
	/** return error if we don't have full privileges */
	ALLPRIVS(NotesConstants.SEARCH_ALLPRIVS),
	/** Use current session's user name, not server's */
	SESSION_USERNAME(NotesConstants.SEARCH_SESSION_USERNAME),
	/** Filter out "Truncated" documents */
	NOABSTRACTS(NotesConstants.SEARCH_NOABSTRACTS),
	/** Search formula applies only to data notes, i.e., others match */
	DATAONLY_FORMULA(NotesConstants.SEARCH_DATAONLY_FORMULA),
	/** INCLUDE notes with non-replicatable OID flag */
	NONREPLICATABLE(NotesConstants.SEARCH_NONREPLICATABLE),
	/** Full search (as if Since was "1") but exclude DATA notes prior to passed-in Since time */
	FULL_DATACUTOFF(NotesConstants.SEARCH_FULL_DATACUTOFF),
	
	/** Allow search to return id's only i.e. no summary buffer */
	NOPRIVCHECK(NotesConstants.SEARCH_NOPRIVCHECK),
	
	/** Search includes all children of matching documents. */
	ALLCHILDREN(NotesConstants.SEARCH_ALLCHILDREN),
	
	/** Search includes all descendants of matching documents. */
	ALLDESCENDANTS(NotesConstants.SEARCH_ALLDESCENDANTS),
	
	/**
	 * Include *** ALL *** named ghost notes in the search (profile docs,
	 * xACL's, etc). Note: use SEARCH1_PROFILE_DOCS, etc., introduced in R6, for
	 * finer control
	 */
	NAMED_GHOSTS(NotesConstants.SEARCH_NAMED_GHOSTS),
	
	/** Return only docs with protection fields (BS_PROTECTED set in note header) */
	ONLYPROTECTED(NotesConstants.SEARCH_ONLYPROTECTED),
	
	/** Return soft deleted documents */
	SOFTDELETIONS(NotesConstants.SEARCH_SOFTDELETIONS),
	
	// search1 flags
	
	/** flag to let the selection formula be run against profile documents; must
	 * be used together with {@link #PROFILE_DOCS} */
	SELECT_NAMED_GHOSTS(NotesConstants.SEARCH1_SELECT_NAMED_GHOSTS),
	
	/**
	 * Include profile documents (a specific type of named ghost note) in the
	 * search Note: set {@link #SELECT_NAMED_GHOSTS}, too, if you want the
	 * selection formula to be applied to the profile docs (so as not to get
	 * them all back as matches).
	 */
	PROFILE_DOCS(NotesConstants.SEARCH1_PROFILE_DOCS);
	
	private static EnumSet<Search> SEARCH1_FLAGS = EnumSet.of(SELECT_NAMED_GHOSTS, PROFILE_DOCS);
	
	private int m_val;
	
	Search(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static short toBitMaskSearch1Flags(EnumSet<Search> searchFlagSet) {
		int result = 0;
		if (searchFlagSet!=null) {
			for (Search currFlag : values()) {
				if (SEARCH1_FLAGS.contains(currFlag) && searchFlagSet.contains(currFlag)) {
					result = result | currFlag.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}

	public static int toBitMaskStdFlagsInt(EnumSet<Search> searchFlagSet) {
		int result = 0;
		if (searchFlagSet!=null) {
			for (Search currFlag : values()) {
				if (!SEARCH1_FLAGS.contains(currFlag) && searchFlagSet.contains(currFlag)) {
					result = result | currFlag.getValue();
				}
			}
		}
		return result;
	}
}
