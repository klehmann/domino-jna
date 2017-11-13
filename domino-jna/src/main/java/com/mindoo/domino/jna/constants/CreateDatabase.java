package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.internal.NotesCAPI;

/**
 * Database creation options.
 * 
 * @author Karsten Lehmann
 */
public enum CreateDatabase {

	/** Create a locally encrypted database. */
	LOCALSECURITY(NotesCAPI.DBCREATE_LOCALSECURITY),
	
	/** NSFNoteUpdate will not use an object store for notes in the database. */
	OBJSTORE_NEVER(NotesCAPI.DBCREATE_OBJSTORE_NEVER),
	
	/** The maximum database length is specified in bytes in NSFDbCreateExtended. */
	MAX_SPECIFIED(NotesCAPI.DBCREATE_MAX_SPECIFIED),
	
	/** Don't support note hierarchy - ODS21 and up only */
	NORESPONSE_INFO(NotesCAPI.DBCREATE_NORESPONSE_INFO),
	
	/** Don't maintain unread lists for this DB */
	NOUNREAD(NotesCAPI.DBCREATE_NOUNREAD),
	
	/** Skip overwriting freed disk buffer space */
	NO_FREE_OVERWRITE(NotesCAPI.DBCREATE_NO_FREE_OVERWRITE),
	
	/** Maintain form/bucket bitmap */
	FORM_BUCKET_OPT(NotesCAPI.DBCREATE_FORM_BUCKET_OPT),
	
	/** Disable transaction logging for this database if specified */
	DISABLE_TXN_LOGGING(NotesCAPI.DBCREATE_DISABLE_TXN_LOGGING),
	
	/** Enable maintaining last accessed time */
	MAINTAIN_LAST_ACCESSED(NotesCAPI.DBCREATE_MAINTAIN_LAST_ACCESSED),
	
	/** TRUE if database is a mail[n].box database */
	IS_MAILBOX(NotesCAPI.DBCREATE_IS_MAILBOX),
	
	/** TRUE if database should allow "large" (&lt;64K bytes) UNK table */
	LARGE_UNKTABLE(NotesCAPI.DBCREATE_LARGE_UNKTABLE);

	private short m_val;
	
	CreateDatabase(short val) {
		m_val = val;
	}
	
	/**
	 * Returns the numeric constant for the db creation flag
	 * 
	 * @return constant
	 */
	public short getValue() {
		return m_val;
	}

	/**
	 * Converts a numeric constant to a db creation flag
	 * 
	 * @param value constant
	 * @return db creation flag
	 */
	public static CreateDatabase toType(int value) {
		short valueShort = (short) (value & 0xffff);
		
		for (CreateDatabase currClass : values()) {
			if (valueShort == currClass.getValue())
				return currClass;
		}
		throw new IllegalArgumentException("Unknown constant: "+value);
	}
	
	public static short toBitMask(EnumSet<CreateDatabase> flags) {
		int result = 0;
		if (flags!=null) {
			for (CreateDatabase currFlag : values()) {
				if (flags.contains(currFlag)) {
					result = result | currFlag.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}
}
