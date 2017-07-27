package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.internal.NotesCAPI;

/**
 * Access Control Level symbols used to qualify user or server access to a given Domino database.
 * 
 * @author Karsten Lehmann
 */
public enum AclLevel {
	
	/** User or Server has no access to the database. */
	NOACCESS(NotesCAPI.ACL_LEVEL_NOACCESS),
	
	/** User or Server can add new data documents to a database, but cannot examine the new document or the database. */
	DEPOSITOR(NotesCAPI.ACL_LEVEL_DEPOSITOR),
	
	/** User or Server can only view data documents in the database. */
	READER(NotesCAPI.ACL_LEVEL_READER),
	
	/** User or Server can create and/or edit their own data documents and examine existing ones in the database. */
	AUTHOR(NotesCAPI.ACL_LEVEL_AUTHOR),
	
	/** User or Server can create and/or edit any data document. */
	EDITOR(NotesCAPI.ACL_LEVEL_EDITOR),
	
	/** User or Server can create and/or edit any data document and/or design document. */
	DESIGNER(NotesCAPI.ACL_LEVEL_DESIGNER),
	
	/** User or Server can create and/or maintain any type of database or document, including the ACL. */
	MANAGER(NotesCAPI.ACL_LEVEL_MANAGER);

	private int m_val;
	
	AclLevel(int val) {
		m_val = val;
	}
	
	/**
	 * Returns the numeric constant for the access level
	 * 
	 * @return constant
	 */
	public int getValue() {
		return m_val;
	}

	/**
	 * Converts a numeric constant to an access level
	 * 
	 * @param value constant
	 * @return access level
	 */
	public static AclLevel toLevel(int value) {
		if (value == NotesCAPI.ACL_LEVEL_NOACCESS) {
			return AclLevel.NOACCESS;
		}
		else if (value == NotesCAPI.ACL_LEVEL_DEPOSITOR) {
			return AclLevel.DEPOSITOR;
		}
		else if (value == NotesCAPI.ACL_LEVEL_READER) {
			return AclLevel.READER;
		}
		else if (value == NotesCAPI.ACL_LEVEL_AUTHOR) {
			return AclLevel.AUTHOR;
		}
		else if (value == NotesCAPI.ACL_LEVEL_EDITOR) {
			return AclLevel.EDITOR;
		}
		else if (value == NotesCAPI.ACL_LEVEL_DESIGNER) {
			return AclLevel.DESIGNER;
		}
		else if (value == NotesCAPI.ACL_LEVEL_MANAGER) {
			return AclLevel.MANAGER;
		}
		else
			throw new IllegalArgumentException("Unknown level constant: "+value);
	}
}
