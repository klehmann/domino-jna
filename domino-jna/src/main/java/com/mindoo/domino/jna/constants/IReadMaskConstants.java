package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.NotesSummaryBufferDecoder;
import com.mindoo.domino.jna.structs.NotesCollectionStats;

/**
 * These flags control what information is returned by NIFReadEntries
 * for each note that is found. All of the information requested is returned in the
 * buffer that NIFReadEntries creates.
 * 
 * @author Karsten Lehmann
 */
public interface IReadMaskConstants {
	
	/** NOTEID of note */
	public static final int READ_MASK_NOTEID = 0x00000001;
	/** UNID of note */
	public static final int READ_MASK_NOTEUNID = 0x00000002;
	/** Note class of note */
	public static final int READ_MASK_NOTECLASS = 0x00000004;
	/** Number of siblings in view or folder */
	public static final int READ_MASK_INDEXSIBLINGS = 0x00000008;
	/** Number of immediate children in view or folder. Subcategories are included in the count */
	public static final int READ_MASK_INDEXCHILDREN = 0x00000010;
	/** Number of descendents in view or folder. Subcategories are not included in the count */
	public static final int READ_MASK_INDEXDESCENDANTS = 0x00000020;
	/** TRUE if unread (or any descendents unread), FALSE otherwise */
	public static final int READ_MASK_INDEXANYUNREAD = 0x00000040;
	/** Number of levels that this entry should be indented in a formatted view or folder */
	public static final int READ_MASK_INDENTLEVELS = 0x00000080;
	/** Relevancy score of an entry. Occupies one WORD in the buffer.
	 * FTSearch must be called prior to NIFReadEntries. The FT_SEARCH_SET_COLL
	 * search option or'd with the FT_SEARCH_SCORES search option must be specified in the call to FTSearch. */
	public static final int READ_MASK_SCORE = 0x00000200;
	/** TRUE if this entry is unread, FALSE otherwise */
	public static final int READ_MASK_INDEXUNREAD = 0x00000400;
	/** Collection statistics (as a {@link NotesCollectionStats} structure) */
	public static final int READ_MASK_COLLECTIONSTATS = 0x00000100;
	/** Return the position of an entry in the collection */
	public static final int READ_MASK_INDEXPOSITION = 0x00004000;
	/** Return the column values of the entry in the collection */
	public static final int READ_MASK_SUMMARYVALUES = 0x00002000;
	/** @deprecated not yet implemented by {@link NotesSummaryBufferDecoder} */
	public static final int READ_MASK_SUMMARY = 0x00008000;

}
