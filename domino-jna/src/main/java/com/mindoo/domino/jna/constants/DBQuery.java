package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.NotesDatabase;

/**
 * Flags to query {@link NotesDatabase} data using DQL
 * 
 * @author Karsten Lehmann
 */
public enum DBQuery {
	/** Explain only mode, only plan and return the explain output */
	NO_EXEC(0x00000001),
	/** produce debugging output (notes.ini setting is independent of this) */
	DEBUG(0x00000002),
	/** refresh all views when they are opened(default is NO_UPDATE) */
	VIEWREFRESH(0x00000004),
	/** to check for syntax only - stops short of planning */
	PARSEONLY(0x00000008),
	/** Governs producing Explain output */
	EXPLAIN(0x00000010),
	/** NSF scans only */
	NOVIEWS(0x00000020);

	private int m_val;
	
	DBQuery(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMask(EnumSet<DBQuery> queryParamSet) {
		int result = 0;
		if (queryParamSet!=null) {
			for (DBQuery currFind : values()) {
				if (queryParamSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
