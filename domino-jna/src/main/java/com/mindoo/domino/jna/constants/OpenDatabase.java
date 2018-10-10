package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Database open options controlling Domino based file locking (scan lock), the removal of deleted note stubs,
 * the removal of documents whose last modified date is past the replication cutoff date (and whose REPLFLG_CUTOFF_DELETE
 * replication flag is set), the forcing of an Access Control List (ACL) check during local database access, and
 * initiation of a database fixup covering various levels of detail.<br>
 * <br>
 * These flags can only be specified with NSFDbOpenExtended, and not with NSFDbOpen.<br>
 * Open Flag Definitions.<br>
 * These flags are passed to NSFNoteOpen.
 * 
 * @author Karsten Lehmann
 */
public enum OpenDatabase {
	
	/**
	 * When opening the database, purge any deleted document place-holders (deleted note stubs)
	 * that are older than the DBREPLICAINFO.Cutoff date and, if the REPLFLG_CUTOFF_DELETE
	 * DBREPLICAINFO.Flag is set, purge documents that have not been modified since the DBREPLICAINFO.Cutoff date.
	 * This flag will prevent the replicator from opening the specified database.
	 */
	PURGE(NotesConstants.DBOPEN_PURGE),
	
	/**
	 * Force a database fixup, even if the file was properly closed previously. This flag is not
	 * necessary if the database was improperly closed, since Domino and Notes will automatically
	 * verify the database contents of improperly closed databases. This process involves three steps:<br>
	 * 1) Perform a consistancy check that compares the database's header information against the on-disk
	 * image of the database and if possible, repair any discrepancies found.<br>
	 * 2) Perform a document by document consistancy check of the entire database, that compares each
	 * note's header information against its on-disk image and if possible, repair any discrepancies found.<br>
	 * 3) Delete all bad documents/notes that could not be corrected during the consistancy check.
	 * NSFDbOpenExtended with {@link #FORCE_FIXUP} will not succeed if db_name specifies a directory.
	 * This flag will prevent the replicator from opening the specified database.
	 */
	FORCE_FIXUP(NotesConstants.DBOPEN_FORCE_FIXUP),
	
	/**
	 * Scan all notes and all items for validity.<br>
	 * Note: NSFDbOpenExtended with {@link #FIXUP_FULL_NOTE_SCAN} will not succeed if db_name specifies a directory.
	 */
	FIXUP_FULL_NOTE_SCAN(NotesConstants.DBOPEN_FIXUP_FULL_NOTE_SCAN),
	
	/**
	 * Do not delete bad notes during note scan (skip step 4.3).<br>
	 * Note: NSFDbOpenExtended with {@link #FIXUP_NO_NOTE_DELETE} will not succeed if db_name specifies a directory.
	 */
	FIXUP_NO_NOTE_DELETE(NotesConstants.DBOPEN_FIXUP_NO_NOTE_DELETE),
	
	/**
	 * If open fails, failover to another server in the same cluster that has a replica copy of this database.
	 * If the input server is not a member of a cluster or if the database is not replicated on other
	 * servers in the cluster, then this flag will have no effect.
	 */
	CLUSTER_FAILOVER(NotesConstants.DBOPEN_CLUSTER_FAILOVER),
	
	FULL_ACCESS(1048576);

	private int m_val;

	OpenDatabase(int val) {
		m_val = val;
	}

	public int getValue() {
		return m_val;
	}

	public static short toBitMaskForOpen(EnumSet<OpenDatabase> openFlagSet) {
		int result = 0;
		if (openFlagSet != null) {
			for (OpenDatabase currNav : values()) {
				if (((int)(currNav.getValue() & 0xffff)) > 0xffff) {
					//skip ext flags
					continue;
				}
				
				if (openFlagSet.contains(currNav)) {
					result = result | currNav.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}

}
