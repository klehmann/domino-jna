package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * 
 * These symbols represent access level modifier flags in access control lists.<br>
 * <br>
 * Each access level taken by itself implies a certain set of immutable capabilities.<br>
 * Each access level has a different set of access modifier bits that are relevant for that level.<br>
 * <br>
 * All of the other bits that are returned in the Access Flag parameter of C API functions are
 * irrelevant and are unpredictable.<br>
 * <br><br>
 * <table summary="The table depicts which Access Level Modifier Flags are applicable to the Access Levels" width="100%" border="1" cellspacing="0">
 * <caption>The table depicts which Access Level Modifier Flags ({@link AclFlag}) are applicable to the Access Levels ({@link AclLevel})</caption>
 * <tr>
 * <th>{@link AclLevel}</th><th><th>{@link AclFlag} Applicable to {@link AclLevel}</th>
 * </tr>
 * <tr>
 * <td>{@link AclLevel#MANAGER}</td><td>{@link #NODELETE}<br>{@link #PERSON}<br>{@link #GROUP}<br>{@link #SERVER}</td>
 * </tr>
 * <tr>
 * <td>{@link AclLevel#DESIGNER}</td><td>{@link #NODELETE}<br>{@link #CREATE_LOTUSSCRIPT}<br>{@link #PERSON}<br>{@link #GROUP}<br>{@link #SERVER}</td>
 * </tr>
 * <tr>
 * <td>{@link AclLevel#EDITOR}</td><td>{@link #NODELETE}<br>{@link #CREATE_PRAGENT}<br>{@link #CREATE_PRFOLDER}<br>{@link #CREATE_FOLDER}<br>{@link #CREATE_LOTUSSCRIPT}<br>{@link #PERSON}<br>{@link #GROUP}<br>{@link #SERVER}</td>
 * </tr>
 * <tr>
 * <td>{@link AclLevel#AUTHOR}</td><td>{@link #AUTHOR_NOCREATE}<br>{@link #NODELETE}<br>{@link #CREATE_PRAGENT}<br>{@link #CREATE_PRFOLDER}<br>{@link #CREATE_LOTUSSCRIPT}<br>{@link #PUBLICWRITER}<br>{@link #PERSON}<br>{@link #GROUP}<br>{@link #SERVER}</td>
 * </tr>
 * <tr>
 * <td>{@link AclLevel#READER}</td><td>{@link #CREATE_PRAGENT}<br>{@link #CREATE_PRFOLDER}<br>{@link #CREATE_LOTUSSCRIPT}<br>{@link #PUBLICWRITER}<br>{@link #PERSON}<br>{@link #GROUP}<br>{@link #SERVER}</td>
 * </tr>
 * <tr>
 * <td>{@link AclLevel#DEPOSITOR}</td><td>{@link #PUBLICREADER}<br>{@link #PUBLICWRITER}<br>{@link #PERSON}<br>{@link #GROUP}<br>{@link #SERVER}</td>
 * </tr>
 * <tr>
 * <td>{@link AclLevel#NOACCESS}</td><td>{@link #PUBLICREADER}<br>{@link #PUBLICWRITER}<br>{@link #PERSON}<br>{@link #GROUP}<br>{@link #SERVER}</td>
 * </tr>
 * </table>
 * @author Karsten Lehmann
 */
public enum AclFlag {
	
	/** Authors can't create new notes (only edit existing ones) */
	AUTHOR_NOCREATE(NotesConstants.ACL_FLAG_AUTHOR_NOCREATE),
	
	/** Entry represents a Server (V4) */
	SERVER(NotesConstants.ACL_FLAG_SERVER),
	
	/** User cannot delete notes */
	NODELETE(NotesConstants.ACL_FLAG_NODELETE),
	
	/** User can create personal agents (V4) */
	CREATE_PRAGENT(NotesConstants.ACL_FLAG_CREATE_PRAGENT),
	
	/** User can create personal folders (V4) */
	CREATE_PRFOLDER(NotesConstants.ACL_FLAG_CREATE_PRFOLDER),
	
	/** Entry represents a Person (V4) */
	PERSON(NotesConstants.ACL_FLAG_PERSON),

	/** Entry represents a group (V4) */
	GROUP(NotesConstants.ACL_FLAG_GROUP),
	
	/** User can create and update shared views &amp; folders (V4)<br>
This allows an Editor to assume some Designer-level access */
	CREATE_FOLDER(NotesConstants.ACL_FLAG_CREATE_FOLDER),
	
	/** User can create LotusScript */
	CREATE_LOTUSSCRIPT(NotesConstants.ACL_FLAG_CREATE_LOTUSSCRIPT),
	
	/** User can read public notes */
	PUBLICREADER(NotesConstants.ACL_FLAG_PUBLICREADER),
	
	/** User can write public notes */
	PUBLICWRITER(NotesConstants.ACL_FLAG_PUBLICWRITER),
	
	/** User CANNOT register monitors for this database */
	MONITORS_DISALLOWED(NotesConstants.ACL_FLAG_MONITORS_DISALLOWED),
	
	/** User cannot replicate or copy this database */
	NOREPLICATE(NotesConstants.ACL_FLAG_NOREPLICATE),
	
	/** Admin server can modify reader and author fields in db */
	ADMIN_READERAUTHOR(NotesConstants.ACL_FLAG_ADMIN_READERAUTHOR),
	
	/** Entry is administration server (V4) */
	ADMIN_SERVER(NotesConstants.ACL_FLAG_ADMIN_SERVER);

	private int m_val;
	
	AclFlag(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static short toBitMask(EnumSet<AclFlag> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (AclFlag currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}
	
	public static int toBitMaskInt(EnumSet<AclFlag> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (AclFlag currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
