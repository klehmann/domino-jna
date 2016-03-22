package com.mindoo.domino.jna.constants;

/**
 * These values define the options used when creating a full text
 * index for a database.<br>
 * These options may be combined by bitwise OR-ing them together.<br>
 * However, FT_INDEX_AUTOOPTIONS will ignore all other indexing
 * options and therefore should not be OR-ed with any of the other
 * indexing options.
 * 
 * @author Karsten Lehmann
 */
public interface IFTIndexConstants {

	/*	Define Indexing options */

	/** Re-index from scratch */
	public static final short FT_INDEX_REINDEX = 0x0002;
	/** Build case sensitive index */
	public static final short FT_INDEX_CASE_SENS = 0x0004;
	/** Build stem index */
	public static final short FT_INDEX_STEM_INDEX = 0x0008;
	/** Index paragraph & sentence breaks */
	public static final short FT_INDEX_PSW = 0x0010;
	/** Optimize index (e.g. for CDROM) (Not used) */
	public static final short FT_INDEX_OPTIMIZE = 0x0020;
	/** Index Attachments */
	public static final short FT_INDEX_ATT = 0x0040;
	/** Index Encrypted Fields */
	public static final short FT_INDEX_ENCRYPTED_FIELDS = 0x0080;
	/** Get options from database */
	public static final short FT_INDEX_AUTOOPTIONS = 0x0100;
	/** Index summary data only */
	public static final short FT_INDEX_SUMMARY_ONLY = 0x0200;
	/** Index all attachments including BINARY formats */
	public static final short FT_INDEX_ATT_BINARY = 0x1000;
	
}
