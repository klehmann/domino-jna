package com.mindoo.domino.jna.constants;

import java.util.Set;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Flags to control DB compaction
 */
public enum DBCompact {

	/** Don't preserve view indexes */
	NO_INDEXES(NotesConstants.DBCOMPACT_NO_INDEXES),
	/** Don't lock out database users */
	NO_LOCKOUT(NotesConstants.DBCOMPACT_NO_LOCKOUT),
	/** Revert current ODS to the previous ODS version */
	REVERT_ODS(NotesConstants.DBCOMPACT_REVERT_ODS),
	
	/** Indicate we are encrypting database */
	FOR_ENCRYPT(NotesConstants.DBCOMPACT_FOR_ENCRYPT),
	/** Indicate we are decrypting database */
	FOR_DECRYPT(NotesConstants.DBCOMPACT_FOR_DECRYPT),
	
	/** Create new file with 4GB file size limit */
	MAX_4GB(NotesConstants.DBCOMPACT_MAX_4GB),
	
	/** This note should be updated as a ghost note */
	GHOST_NOTE(NotesConstants.DBCOMPACT_GHOST_NOTE),
	
	/** Compact XXXX.BOX for mail router and other MTAs */
	MAILBOX(NotesConstants.DBCOMPACT_MAILBOX),
	/** Don't do in-place compaction */
	NO_INPLACE(NotesConstants.DBCOMPACT_NO_INPLACE),
	
	ENCRYPT_DEFAULT(NotesConstants.DBCOMPACT_ENCRYPT_DEFAULT),
	
	/** Disable unread marks in destination database */
	DISABLE_UNREAD(NotesConstants.DBCOMPACT_DISABLE_UNREAD),
	/** Reenable unread marks in destination database (default) */
	ENABLE_UNREAD(NotesConstants.DBCOMPACT_ENABLE_UNREAD),
	/** Disable response info in resulting database */
	DISABLE_RESPONSE_INFO(NotesConstants.DBCOMPACT_DISABLE_RESPONSE_INFO),
	/** Disable response info in resulting database (default) */
	ENABLE_RESPONSE_INFO(NotesConstants.DBCOMPACT_ENABLE_RESPONSE_INFO),
	/** Enable form/bucket bitmap optimization */
	ENABLE_FORM_BKT_OPT(NotesConstants.DBCOMPACT_ENABLE_FORM_BKT_OPT),
	/** Diable form/bucket bitmap optimization (default) */
	DISABLE_FORM_BKT_OPT(NotesConstants.DBCOMPACT_DISABLE_FORM_BKT_OPT),
	/** Ignore errors encountered during compaction.
	 * That is, make best effort to get something at the end */
	IGNORE_ERRORS(NotesConstants.DBCOMPACT_IGNORE_ERRORS),
	
	/** If set, disable transaction logging for new database */
	DISABLE_TXN_LOGGING(NotesConstants.DBCOMPACT_DISABLE_TXN_LOGGING),
	/** If set, enable transaction logging for new database */
	ENABLE_TXN_LOGGING(NotesConstants.DBCOMPACT_ENABLE_TXN_LOGGING),
	
	/** If set, do only bitmap correction if in-place can be done */
	RECOVER_SPACE_ONLY(NotesConstants.DBCOMPACT_RECOVER_SPACE_ONLY),
	/** Archive/delete, then compact the database */
	ARCHIVE(NotesConstants.DBCOMPACT_ARCHIVE),
	/** Just archive/delete, no need to compact */
	ARCHIVE_ONLY(NotesConstants.DBCOMPACT_ARCHIVE_ONLY),
	
	/** Just check object size and position fidelity - looking for overlap */
	VERIFY_NOOVERLAP(NotesConstants.DBCOMPACT_VERIFY_NOOVERLAP),
	
	/** If set, always do full space recovery compaction */
	RECOVER_ALL_SPACE(NotesConstants.DBCOMPACT_RECOVER_ALL_SPACE),
	
	/** If set and inplace is possible, just dump space map - don't compact */
	DUMP_SPACE_MAP_ONLY(NotesConstants.DBCOMPACT_DUMP_SPACE_MAP_ONLY),
	/** Disable large UNK table in destination database (default) */
	DISABLE_LARGE_UNKTBL(NotesConstants.DBCOMPACT_DISABLE_LARGE_UNKTBL),
	/** Enable large UNK table in destination database */
	ENABLE_LARGE_UNKTBL(NotesConstants.DBCOMPACT_ENABLE_LARGE_UNKTBL),
	/** Only do compaction if it can be done inplace - error return otherwise */
	ONLY_IF_INPLACE(NotesConstants.DBCOMPACT_ONLY_IF_INPLACE),
	/** Recursively explore subdirectores during NSFSearchExtended */
	RECURSE_SUBDIRECTORIES(NotesConstants.DBCOMPACT_RECURSE_SUBDIRECTORIES);

	private final int value;
	
	private DBCompact(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}

	public static int toBitMask(Set<DBCompact> flags) {
		int result = 0;
		if (flags!=null) {
			for (DBCompact currFlag : values()) {
				if (flags.contains(currFlag)) {
					result = result | currFlag.getValue();
				}
			}
		}
		return (int) (result & 0xffff);
	}
}
