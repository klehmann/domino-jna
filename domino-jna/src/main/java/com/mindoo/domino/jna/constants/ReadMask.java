package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.NotesSummaryBufferDecoder;
import com.mindoo.domino.jna.structs.NotesCollectionStats;

/**
 * These flags control what information is returned by NIFReadEntries
 * for each note that is found. All of the information requested is returned in the
 * buffer that NIFReadEntries creates.
 * 
 * @author Karsten Lehmann
 */
public enum ReadMask {
	/** NOTEID of note */
	NOTEID(0x00000001),
	/** UNID of note */
	NOTEUNID(0x00000002),
	/** Note class of note */
	NOTECLASS(0x00000004),
	/** Number of siblings in view or folder */
	INDEXSIBLINGS(0x00000008),
	/** Number of immediate children in view or folder. Subcategories are included in the count */
	INDEXCHILDREN(0x00000010),
	/** Number of descendents in view or folder. Subcategories are not included in the count */
	INDEXDESCENDANTS(0x00000020),
	/** TRUE if unread (or any descendents unread), FALSE otherwise */
	INDEXANYUNREAD(0x00000040),
	/** Number of levels that this entry should be indented in a formatted view or folder */
	INDENTLEVELS(0x00000080),
	/** Relevancy score of an entry. Occupies one WORD in the buffer.
	 * FTSearch must be called prior to NIFReadEntries. The FT_SEARCH_SET_COLL
	 * search option or'd with the FT_SEARCH_SCORES search option must be specified in the call to FTSearch. */
	SCORE(0x00000200),
	/** TRUE if this entry is unread, FALSE otherwise */
	INDEXUNREAD(0x00000400),
	/** Collection statistics (as a {@link NotesCollectionStats} structure) */
	COLLECTIONSTATS(0x00000100),
	/** Return the position of an entry in the collection */
	INDEXPOSITION(0x00004000),
	/** Return the column values of the entry in the collection */
	SUMMARYVALUES(0x00002000),
	/** @deprecated not yet implemented by {@link NotesSummaryBufferDecoder} */
	SUMMARY(0x00008000);

	
	private int m_val;
	
	ReadMask(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMask(EnumSet<ReadMask> findSet) {
		int result = 0;
		for (ReadMask currNav : values()) {
			if (findSet.contains(currNav)) {
				result = result | currNav.getValue();
			}
		}
		return result;
	}

}
