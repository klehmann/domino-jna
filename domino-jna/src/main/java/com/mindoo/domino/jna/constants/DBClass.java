package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * These symbolic constants define the various classes that can be specified when a database is created.
 * 
 * @author Karsten Lehmann
 */
public enum DBClass {

	/** The type of the database is determined by the filename extension.<br>
	 * The extensions and their database classes are .NSX (NSFTESTFILE), .NSF (NOTEFILE), .DSK (DESKTOP),
	 * .NCF (NOTECLIPBOARD), .NTF (TEMPLATEFILE), .NSG (GIANTNOTEFILE), .NSH (HUGENOTEFILE), NTD (ONEDOCFILE),
	 * NS2 (V2NOTEFILE), NTM (ENCAPSMAILFILE). */
	BY_EXTENSION(NotesConstants.DBCLASS_BY_EXTENSION),
	
	/** A test database. */
	NSFTESTFILE(NotesConstants.DBCLASS_NSFTESTFILE),
	
	/** A standard Domino database. */
	NOTEFILE(NotesConstants.DBCLASS_NOTEFILE),
	
	/** A Notes desktop (folders, icons, etc.). */
	DESKTOP(NotesConstants.DBCLASS_DESKTOP),
	
	/** A Notes clipboard (used for cutting and pasting). */
	NOTECLIPBOARD(NotesConstants.DBCLASS_NOTECLIPBOARD),
	
	/** A database that contains every type of note (forms, views, ACL, icon, etc.) except data notes. */
	TEMPLATEFILE(NotesConstants.DBCLASS_TEMPLATEFILE),
	
	/** A standard Domino database, with size up to 1 GB. This was used
	 * in Notes Release 3 when the size of a previous version of a database had been limited to 200 MB.
	 */
	GIANTNOTEFILE(NotesConstants.DBCLASS_GIANTNOTEFILE),
	
	/**  A standard Domino database, with size up to 1 GB. This was used in Notes Release
	 * 3 when the size of a previous version of a database had been limited to 300 MB.
	 */
	HUGENOTEFILE(NotesConstants.DBCLASS_HUGENOTEFILE),
	
	/** One document database with size up to 10MB. Specifically used by alternate
	 * mail to create an encapsulated database. Components of the document are
	 * further limited in size. It is not recommended that you use this database
	 * class with NSFDbCreate. If you do, and you get an error when saving the document,
	 * you will need to re-create the database using DBCLASS_NOTEFILE. */
	ONEDOCFILE(NotesConstants.DBCLASS_ONEDOCFILE),
	
	/** Database was created as a Notes Release 2 database. */
	DBCLASS_ONEDOCFILE(NotesConstants.DBCLASS_V2NOTEFILE),
	
	/** One document database with size up to 5MB. Specifically used by alternate mail
	 * to create an encapsulated database. Components of the document are further
	 * limited in size. It is not recommended that you use this database class with
	 * NSFDbCreate. If you do, and you get an error when saving the document, you will
	 * need to re-create the database using DBCLASS_NOTEFILE. */
	ENCAPSMAILFILE(NotesConstants.DBCLASS_ENCAPSMAILFILE),
	
	/** Specifically used by alternate mail. Not recomended for use with NSFDbCreate. */
	LRGENCAPSMAILFILE(NotesConstants.DBCLASS_LRGENCAPSMAILFILE),
	
	/** Database was created as a Notes Release 3 database. */
	V3NOTEFILE(NotesConstants.DBCLASS_V3NOTEFILE),
	
	/** Object store. */
	OBJSTORE(NotesConstants.DBCLASS_OBJSTORE),
	
	/**  One document database with size up to 10MB. Specifically used by Notes Release 3
	 * alternate mail to create an encapsulated database. Not recomended for use
	 * with NSFDbCreate. */
	V3ONEDOCFILE(NotesConstants.DBCLASS_V3ONEDOCFILE),
	
	/** Database was created specifically for Domino and Notes Release 4. */
	V4NOTEFILE(NotesConstants.DBCLASS_V4NOTEFILE),
	
	/** Database was created specifically for Domino and Notes Release 5. */
	V5NOTEFILE(NotesConstants.DBCLASS_V5NOTEFILE),
	
	/** Database was created specifically for Domino and Notes Release Notes/Domino 6. */
	V6NOTEFILE(NotesConstants.DBCLASS_V6NOTEFILE),
	
	/** Database was created specifically for Domino and Notes Release Notes/Domino 8. */
	V8NOTEFILE(NotesConstants.DBCLASS_V8NOTEFILE),
	
	/** Database was created specifically for Domino and Notes Release Notes/Domino 8.5. */
	V85NOTEFILE(NotesConstants.DBCLASS_V85NOTEFILE),
	
	/** Database was created specifically for Domino and Notes Release Notes/Domino 9. */
	V9NOTEFILE(NotesConstants.DBCLASS_V9NOTEFILE),

	/** Database was created specifically for Domino and Notes Release Notes/Domino 10. */
	V10NOTEFILE(NotesConstants.DBCLASS_V10NOTEFILE),

	/** Database was created specifically for Domino and Notes Release Notes/Domino 12. */
	V12NOTEFILE(NotesConstants.DBCLASS_V12NOTEFILE);

	private short m_val;
	
	DBClass(short val) {
		m_val = val;
	}
	
	/**
	 * Returns the numeric constant for the db class
	 * 
	 * @return constant
	 */
	public short getValue() {
		return m_val;
	}

	/**
	 * Converts a numeric constant to a db class
	 * 
	 * @param value constant
	 * @return db class
	 */
	public static DBClass toType(int value) {
		short valueShort = (short) (value & 0xffff);
		
		for (DBClass currClass : values()) {
			if (valueShort == currClass.getValue())
				return currClass;
		}
		throw new IllegalArgumentException("Unknown constant: "+value);
	}
}
