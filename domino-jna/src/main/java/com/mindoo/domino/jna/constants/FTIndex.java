package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

/**
 * These values define the options used when creating a full text
 * index for a database.<br>
 * These options may be combined as an {@link EnumSet}.<br>
 * However, {@link #AUTOOPTIONS} will ignore all other indexing
 * options and therefore should not be OR-ed with any of the other
 * indexing options.
 * 
 * @author Karsten Lehmann
 */
public enum FTIndex {
	/*	Define Indexing options */

	/** Re-index from scratch */
	REINDEX(0x0002),
	/** Build case sensitive index */
	CASE_SENS(0x0004),
	/** Build stem index */
	STEM_INDEX(0x0008),
	/** Index paragraph & sentence breaks */
	PSW(0x0010),
	/** Optimize index (e.g. for CDROM) (Not used) */
	OPTIMIZE(0x0020),
	/** Index Attachments */
	ATT(0x0040),
	/** Index Encrypted Fields */
	ENCRYPTED_FIELDS(0x0080),
	/** Get options from database */
	AUTOOPTIONS(0x0100),
	/** Index summary data only */
	SUMMARY_ONLY(0x0200),
	/** Index all attachments including BINARY formats */
	ATT_BINARY(0x1000);

	private int m_val;
	
	FTIndex(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static short toBitMask(EnumSet<FTIndex> optionSet) {
		int result = 0;
		if (optionSet!=null) {
			for (FTIndex currFind : values()) {
				if (optionSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}
}
