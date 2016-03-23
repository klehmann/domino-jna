package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

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
	/** Return ID table */
	RET_IDTABLE(0x00000010),
	/** Sort results by date */
	SORT_DATE(0x00000020),
	/** Sort in ascending order */
	SORT_ASCEND(0x00000040),
	/** Use Limit arg. to return only top scores */
	TOP_SCORES(0x00000080),
	/** Stem words in this query */
	STEM_WORDS(0x00000200),
	/** Thesaurus words in this query */
	THESAURUS_WORDS(0x00000400),
	/** set if fuzzy search wanted */
	FUZZY(0x00004000),
	/** Return url-based results (FTSearchExt only) */
	EXT_RET_URL(0x00008000),
	/** Sort by created date (default is to sort by modified date) */
	SORT_DATE_CREATED(0x00010000),
	/** this is a domain search */
	EXT_DOMAIN(0x00040000),
	/** search the filesystem index (Domain Search only) */
	EXT_FILESYSTEM(0x00100000),
	/** search the database index (Domain Search only) */
	EXT_DATABASE(0x00200000);
	
	private int m_val;
	
	FTSearch(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMask(EnumSet<FTSearch> findSet) {
		int result = 0;
		for (FTSearch currFind : values()) {
			if (findSet.contains(currFind)) {
				result = result | currFind.getValue();
			}
		}
		return result;
	}
}
