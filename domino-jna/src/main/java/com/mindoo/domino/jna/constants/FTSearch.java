package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.NotesFTSearchResult;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesOriginatorId;

/**
 * These values define the options you may specify in the Options
 * parameter to FTSearch or FTSearchExt when performing a full text
 * search in a database or the domain search against a Domain catalog.
 * These options may be combined by bitwise-ORing them together.
 * 
 * @author Karsten Lehmann
 */
public enum FTSearch {
	/** Store search results in NIF collections; Don't return them to caller */
	SET_COLL(0x00000001),
	/** Return # hits only; not the documents */
	NUMDOCS_ONLY(0x00000002),
	/** Refine the query using the IDTABLE */
	REFINE(0x00000004),
	/** Return document scores (default sort) */
	SCORES(0x00000008),
	/** Return ID table, can be read via {@link NotesFTSearchResult} */
	RET_IDTABLE(0x00000010),
	
	/** Use Limit arg. to return only top scores */
	TOP_SCORES(0x00000080),
	/** Stem words in this query */
	STEM_WORDS(0x00000200),
	/** Thesaurus words in this query */
	THESAURUS_WORDS(0x00000400),
	/** Search w/o index, requires a {@link NotesIDTable} to specify
	 * the docs to create a temporary index. By default, not more
	 * than 5000 docs can be specified here. See this technote
	 * to increase this limit: 
	 * <a href="https://www.ibm.com/support/pages/error-maximum-allowable-documents-exceeded-temporary-index-log">Error: '...Maximum allowable documents exceeded for a temporary index' in log</a> */ 
	NOINDEX(0x00000800),
	/** set if fuzzy search wanted */
	FUZZY(0x00004000),
	/** Return url-based results (FTSearchExt only) */
	EXT_RET_URL(0x00008000),
	/** this is a domain search */
	EXT_DOMAIN(0x00040000),
	/** search the filesystem index (Domain Search only) */
	EXT_FILESYSTEM(0x00100000),
	/** search the database index (Domain Search only) */
	EXT_DATABASE(0x00200000),
	/** return highlight strings */ 
	EXT_RET_HL(0x00080000),
	
	// sort options; if SORT_DATE or SORT_DATE_CREATED are not specified and we retrieved SCORES,
	// results are sorted by scores in descending order. Use SORT_ASCEND to reverse the sort order
	
	/** Sort results by last modified date (modified in this replica, returned by
	 * {@link NotesOriginatorId#getSequenceTime()} in the OID read via {@link NotesNote#getOID()}  */
	SORT_DATE(0x00000020),
	/** Sort by created date (default is to sort by modified date) */
	SORT_DATE_CREATED(0x00010000),
	/** Sort in ascending order (e.g. ascending score when combined with {@link #SCORES} ) */
	SORT_ASCEND(0x00000040);

	private int m_val;
	
	FTSearch(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMask(EnumSet<FTSearch> ftSearchSet) {
		int result = 0;
		if (ftSearchSet!=null) {
			for (FTSearch currFind : values()) {
				if (ftSearchSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}
}
