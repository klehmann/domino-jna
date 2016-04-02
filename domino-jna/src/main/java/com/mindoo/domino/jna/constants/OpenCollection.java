package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

/**
 * These flags control the manner in which NIFOpenCollection opens a collection
 * of notes.
 * 
 * @author Karsten Lehmann
 */
public enum OpenCollection {

	/**
	 * Throw away existing index and rebuild it from scratch
	 */
	REBUILD_INDEX((short) 0x0001),

	/**
	 * Do not update index or unread list as part of open (usually set by server
	 * when it does it incrementally instead).
	 */
	NOUPDATE((short) 0x0002),

	/**
	 * If collection object has not yet been created, do NOT create it
	 * automatically, but instead return a special internal error called
	 * ERR_COLLECTION_NOT_CREATED
	 */
	OPEN_DO_NOT_CREATE((short) 0x0004),

	/**
	 * Tells NIF to "own" the view note (which gets read while opening the
	 * collection) in memory, rather than the caller "owning" the view note by
	 * default. If this flag is specified on subsequent opens, and NIF currently
	 * owns a copy of the view note, it will just pass back the view note handle
	 * rather than re-reading it from disk/network. If specified, the the caller
	 * does NOT have to close the handle. If not specified, the caller gets a
	 * separate copy, and has to NSFNoteClose the handle when its done with it.
	 */
	OPEN_SHARED_VIEW_NOTE((short) 0x0010),

	/**
	 * Force re-open of collection and thus, re-read of view note. Also
	 * implicitly prevents sharing of collection handle, and thus prevents
	 * any sharing of associated structures such as unread lists, etc
	 */
	OPEN_REOPEN_COLLECTION((short) 0x0020);

	private short m_val;

	OpenCollection(short val) {
		m_val = val;
	}

	public int getValue() {
		return m_val;
	}

	public static short toBitMask(EnumSet<OpenCollection> openFlagSet) {
		int result = 0;
		if (openFlagSet != null) {
			for (OpenCollection currNav : values()) {
				if (openFlagSet.contains(currNav)) {
					result = result | currNav.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}

}
