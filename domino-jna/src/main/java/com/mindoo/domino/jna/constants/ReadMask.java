package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.NotesCollectionStats;

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
	/** unknown */
	SERETFLAGS(0x800),
	/** Collection statistics (as a {@link NotesCollectionStats} object) */
	COLLECTIONSTATS(0x00000100),
	/** Return SIBLINGS, CHILDREN, DESCENDANTS, COLLECTIONSTATS, and COLLECTIONPOSITION in DWORDs */
	RETURN_DWORD(0x00001000),
	/** Return the position of an entry in the collection */
	INDEXPOSITION(0x00004000),
	/** Return "short" (build 110 or earlier) COLLECTIONPOSITIONS */ 
	SHORT_COLPOS(0x00010000),
	/** IndexPos.Tumbler[0] is a NOTEID for initial position */ 
	INIT_POS_NOTEID(0x00020000),
	/** Return the column values of the entry in the collection */
	SUMMARYVALUES(0x00002000),
	/** Return the summary buffer data for collection entries */
	SUMMARY(0x00008000),
	/** Permuted summary buffer with item names */ 
	SUMMARY_PERMUTED(0x00040000),
	/** Don't return subtotals */ 
	NO_SUBTOTALS(0x00080000),
	/** In single-column read mode, don't return entries with column value empty */ 
	NO_EMPTY_VALUES(0x00100000),
	/** Do DbColumn logic - if categories have non-empty values for
    single-column reading mode, just read the categories */ 
	CATS_ONLY_FOR_COLUMN(0x00200000),
	/** DWORD/WORD of # direct children of entry - not done for categories */ 
	INDEXCHILDREN_NOCATS(0x00400000),
	/** DWORD/WORD of # descendants below entry - not done for categories */ 
	INDEXDESCENDANTS_NOCATS(0x00800000),
	/** Return the readers list field */ 
	RETURN_READERSLIST(0x01000000),
	/** Return only entries which hNames would disallow (requires full access set) */ 
	PRIVATE_ONLY(0x02000000),
	/** If ColumnNumber specifies a valid value, return all column values up to and
    including that column rather than just that column's values */ 
	ALL_TO_COLUMN(0x04000000),
	/** Exclude all columns that have been programmatically generated ( eg $1 ) */
	EXCLUDE_LEADING_PROGRAMMATIC_COLUMNS(0x08000000),
	/** Exclude internal entries - readers list field, $REF, $CONFLICT */ 
	NO_INTERNAL_ENTRIES(0x10000000),
	/** Compute subtotals */ 
	COMPUTE_SUBTOTALS(0x20000000),
	/** add WORD (bool) set to 1=ghost entry, 0=true entry */ 
	IS_GHOST_ENTRY(0x40000000),
	/** unknown */
	ALL(0xE7FF);
	
	private int m_val;
	
	ReadMask(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMask(EnumSet<ReadMask> readMaskSet) {
		int result = 0;
		if (readMaskSet!=null) {
			for (ReadMask currNav : values()) {
				if (readMaskSet.contains(currNav)) {
					result = result | currNav.getValue();
				}
			}
		}
		return result;
	}

}
