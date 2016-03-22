package com.mindoo.domino.jna.constants;

/**
 * These values define the options you may specify in the Options
 * parameter to FTSearch or FTSearchExt when performing a full text
 * search in a database or the domain search against a Domain catalog.
 * These options may be combined by bitwise-ORing them together.
 * 
 * @author Karsten Lehmann
 */
public interface IFTSearchConstants {
	
	/** Store search results in NIF collections; Don't return them to caller */
	public static int FT_SEARCH_SET_COLL = 0x00000001;
	/** Return # hits only; not the documents */
	public static int FT_SEARCH_NUMDOCS_ONLY = 0x00000002;
	/** Refine the query using the IDTABLE */
	public static int FT_SEARCH_REFINE = 0x00000004;
	/** Return document scores (default sort) */
	public static int FT_SEARCH_SCORES = 0x00000008;
	/** Return ID table */
	public static int FT_SEARCH_RET_IDTABLE = 0x00000010;
	/** Sort results by date */
	public static int FT_SEARCH_SORT_DATE = 0x00000020;
	/** Sort in ascending order */
	public static int FT_SEARCH_SORT_ASCEND	= 0x00000040;
	/** Use Limit arg. to return only top scores */
	public static int FT_SEARCH_TOP_SCORES = 0x00000080;
	/** Stem words in this query */
	public static int FT_SEARCH_STEM_WORDS = 0x00000200;
	/** Thesaurus words in this query */
	public static int FT_SEARCH_THESAURUS_WORDS = 0x00000400;
	/** set if fuzzy search wanted */
	public static int FT_SEARCH_FUZZY = 0x00004000;
	/** Return url-based results (FTSearchExt only) */
	public static int FT_SEARCH_EXT_RET_URL = 0x00008000;
	/** Sort by created date (default is to sort by modified date) */
	public static int FT_SEARCH_SORT_DATE_CREATED = 0x00010000;
	/** this is a domain search */
	public static int FT_SEARCH_EXT_DOMAIN = 0x00040000;
	/** search the filesystem index (Domain Search only) */
	public static int FT_SEARCH_EXT_FILESYSTEM = 0x00100000;
	/** search the database index (Domain Search only) */
	public static int FT_SEARCH_EXT_DATABASE = 0x00200000;
	
}
