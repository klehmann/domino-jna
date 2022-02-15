package com.mindoo.domino.jna.constants;

import java.util.Set;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * More flags to control compaction
 */
public enum DBCompact2 {

	/** Retain bodyheader values for imap enabled databases. */
	KEEP_IMAP_ITEMS(NotesConstants.DBCOMPACT2_KEEP_IMAP_ITEMS),
	/** Upgrade to LZ1 attachments for entire db. */
	LZ1_UPGRADE(NotesConstants.DBCOMPACT2_LZ1_UPGRADE),
	/**
	 * Archive delete only! Must specify {@link DBCompact#ARCHIVE}
	 */
	ARCHIVE_JUST_DELETE(NotesConstants.DBCOMPACT2_ARCHIVE_JUST_DELETE),
	/** Force the compact to open the DB with the O_SYNC flag on
	 * this is for large db's on systems with huge amounts of memory
	 */
	SYNC_OPEN(NotesConstants.DBCOMPACT2_SYNC_OPEN),
	/** Convert the source database to an NSFDB2 database. */
	CONVERT_TO_NSFDB2(NotesConstants.DBCOMPACT2_CONVERT_TO_NSFDB2),
	/** Fixup busted LZ1 attachments (really huffman) for entire db. */
	LZ1_FIXUP(NotesConstants.DBCOMPACT2_LZ1_FIXUP),
	/** Check busted LZ1 attachments (really huffman) for entire db. */
	LZ1_CHECK(NotesConstants.DBCOMPACT2_LZ1_CHECK),
	/** Downgrade attachments to huffman for entire db. */
	LZ1_DOWNGRADE(NotesConstants.DBCOMPACT2_LZ1_DOWNGRADE),
	/** skip NSFDB2 databases found while traversing files index */
	SKIP_NSFDB2(NotesConstants.DBCOMPACT2_SKIP_NSFDB2),
	/** skip NSF databases while processing NSFDB2 databases */
	SKIP_NSF(NotesConstants.DBCOMPACT2_SKIP_NSF),
	
	/** TRUE if design note non-summary should be compressed */
	COMPRESS_DESIGN_NS(NotesConstants.DBCOMPACT2_COMPRESS_DESIGN_NS),
	/** TRUE if design note non-summary should be uncompressed */
	UNCOMPRESS_DESIGN_NS(NotesConstants.DBCOMPACT2_UNCOMPRESS_DESIGN_NS),
	
	/** if TRUE, do db2 group compression for group associated with this nsf */
	DB2_ASSOCGRP_COMPACT(NotesConstants.DBCOMPACT2_DB2_ASSOCGRP_COMPACT),
	/** if TRUE, do db2 group compression directly on group*/
	DB2_GROUP_COMPACT(NotesConstants.DBCOMPACT2_DB2_GROUP_COMPACT),
	
	/** TRUE if all data note non-summary should be compressed */
	COMPRESS_DATA_DOCS(NotesConstants.DBCOMPACT2_COMPRESS_DATA_DOCS),
	/** TRUE if all data note non-summary should be uncompressed */
	UNCOMPRESS_DATA_DOCS(NotesConstants.DBCOMPACT2_UNCOMPRESS_DATA_DOCS),
	
	/** TRUE if return file sizes should be in granules to handle large file sizes */
	STATS_IN_GRANULES(NotesConstants.DBCOMPACT2_STATS_IN_GRANULES),
	
	/** enable compact TO DAOS */
	FORCE_DAOS_ON(NotesConstants.DBCOMPACT2_FORCE_DAOS_ON),
	/** enable compact FROM DAOS */
	FORCE_DAOS_OFF(NotesConstants.DBCOMPACT2_FORCE_DAOS_OFF),
	
	/** revert one ods based on current ods of the database */
	REVERT_ONE_ODS(NotesConstants.DBCOMPACT2_REVERT_ONE_ODS),
	/** Process attachments inplace for entire db. */
	LZ1_INPLACE(NotesConstants.DBCOMPACT2_LZ1_INPLACE),
	/** If ODS is lower than desired ODS based on INI settings, compact it to upgrade ODS */
	ODS_DEFAULT_UPGRADE(NotesConstants.DBCOMPACT2_ODS_DEFAULT_UPGRADE),
	/** split NIF containers out to their own database */
	SPLIT_NIF_DATA(NotesConstants.DBCOMPACT2_SPLIT_NIF_DATA),
	/** see above, but off */
	UNSPLIT_NIF_DATA(NotesConstants.DBCOMPACT2_UNSPLIT_NIF_DATA),
	
	/** enable compact with PIRC */
	FORCE_PIRC_ON(NotesConstants.DBCOMPACT2_FORCE_PIRC_ON),
	/** enable compact without PIRC */
	FORCE_PIRC_OFF(NotesConstants.DBCOMPACT2_FORCE_PIRC_OFF),
	
	/** SaaS option, enable advanced property override */
	ADV_OPT_OVERRIDE_ON(NotesConstants.DBCOMPACT2_ADV_OPT_OVERRIDE_ON),
	/** SaaS option, disable advanced property override */
	ADV_OPT_OVERRIDE_OFF(NotesConstants.DBCOMPACT2_ADV_OPT_OVERRIDE_OFF),

	/** compact is running as DataBaseMaintenanceTool */
	DBMT(NotesConstants.DBCOMPACT2_DBMT),
	/** Take database offline for compact */
	FORCE(NotesConstants.DBCOMPACT2_FORCE),
	/** for copy style compaction, force "new" target to be encrypted even if source db is not */
	ENABLE_ENCRYPTION(NotesConstants.DBCOMPACT2_ENABLE_ENCRYPTION),
	/** Upgrade previous DBCLASS_V*NOTEFILE classes to DBCLASS_NOTEFILE */
	DBCLASS_UPGRADE(NotesConstants.DBCOMPACT2_DBCLASS_UPGRADE),
	/** Create a new replica in the copy style compact */
	COPY_REPLICA(NotesConstants.DBCOMPACT2_COPY_REPLICA);

	private final int value;
	
	private DBCompact2(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}

	public static int toBitMask(Set<DBCompact2> flags) {
		int result = 0;
		if (flags!=null) {
			for (DBCompact2 currFlag : values()) {
				if (flags.contains(currFlag)) {
					result = result | currFlag.getValue();
				}
			}
		}
		return (int) (result & 0xffff);
	}
}
