package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

/**
 * These flags control the manner in which Domino and Notes updates or deletes the on-disk copy of a note.<br>
 * The extended (32-bit) options are ignored if passed to NSFNoteUpdate or NSFNoteDelete;<br>
 * these options can only be passed to NSFNoteUpdatedExtended, NSFNoteDeleteExtended and NSFDbCopyNoteExt.
 * 
 * @author Karsten Lehmann
 */
public enum UpdateNote {
	
	/** Do an update or delete even if some other user has updated the note between the time the
	 * note was read into memory and the time we try to write it. This flag is appropriate
	 * for both updating and deleting a note. */
	FORCE(0x0001),
	
	/** Give an error if the updated note contains a new field name that wasn't already defined
	 * in one of the forms in the database. This flag is appropriate for updating a note only. */
	NAME_KEY_WARNING(0x0002),
	
	/** Do not flush all data to disk after the update. The note will be updated by the system,
	 * but non-summary data may not be immediately written to disk. This may improve the
	 * speed of NSFNoteUpdate if many operations are to be done sequentially, but risks
	 * loss of data if the machine crashes before data can be flushed to the disk. Summary
	 * data is always written to disk regardless of whether this flag is set or clear.
	 * This flag is appropriate for both updating and deleting a note. */
	NOCOMMIT(0x0004),
	
	/** do not update seq/sequence time on update (used when updating notes in the replicator) */
	REPLICA(0x0008),

	/** Do not maintain revision history. This flag is appropriate for updating a note only. */
	NOREVISION(0x0100),
	
	/** Leave no trace of the note in the database if the note is deleted.
	 * This flag is only appropriate for deleting a note. You can use this flag on a
	 * deletion stub by removing the RRV_DELETED flag from the note id of the deletion
	 * stub before calling NSFNoteDelete or NSFNoteDeleteExtended. Once the deletion stub
	 * is removed, the deletion of the note will not be replicated. Otherwise, NOSTUB
	 * is only useful for applications that do NOT use views, and do not replicate.<br>
	 * For example, a gateway may periodically examine a mail.box file, using NSFSearch
	 * and simply delete a document (using UPDATE_NOSTUB) when it's done with it.
	 * The database does not replicate, nor does it need views. UPDATE_NOSTUB ensures
	 * that there is minimum waste space allocated to stubs that are never needed in a
	 * highly volatile request-style database. Documents that are deleted with
	 * NOSTUB will still appear in the Domino views of the database and may cause
	 * problems when replicated. The documents themselves will not exist in the
	 * database and therefore cannot be opened. */
	NOSTUB(0x0200),
	
	/** avoid queuing the update to the real time replicator and the streaming cluster replicator */
	RTR(0x0800),
	
	/** Compute incremental note info. */
	INCREMENTAL(0x4000),
	
	/** The current update is a step in the process of deleting a note.
	 * API programs only use this flag in the context of a database hook driver.
	 * See Data Type DBHOOKVEC. Normally, API programs do not set this flag in
	 * calls to NSFNoteUpdate or NSFNoteDelete. NSFNoteDelete specifies this
	 * flag when it calls NSFNoteUpdate in the process of writing the deletion stub to the disk. */
	DELETED(0x8000),
	
	/** Obsolete. Allow duplicate items of the same name. This flag is appropriate for updating a note only. */
	DUPLICATE(0),
	
	/** Extended (32-bit DWORD) option: Split the second update of this note with the Note Object Store. */
	SHARE_SECOND(0x00200000),
	
	/** Extended (32-bit DWORD) option: Share only objects with the Note Object Store, not the summary information. */
	SHARE_OBJECTS(0x00400000);
	

	private int m_val;

	UpdateNote(int val) {
		m_val = val;
	}

	public int getValue() {
		return m_val;
	}

	public static short toBitMaskForUpdate(EnumSet<UpdateNote> updateFlagSet) {
		int result = 0;
		if (updateFlagSet != null) {
			for (UpdateNote currNav : values()) {
				if (currNav.getValue() > 0xffff) {
					//skip ext flags
					continue;
				}
				
				if (updateFlagSet.contains(currNav)) {
					result = result | currNav.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}

	public static int toBitMaskForUpdateExt(EnumSet<UpdateNote> updateFlagSet) {
		int result = 0;
		if (updateFlagSet != null) {
			for (UpdateNote currNav : values()) {
				if (updateFlagSet.contains(currNav)) {
					result = result | currNav.getValue();
				}
			}
		}
		return result;
	}

}
