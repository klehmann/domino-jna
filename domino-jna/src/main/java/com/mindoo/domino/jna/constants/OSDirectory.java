package com.mindoo.domino.jna.constants;

/**
 * Constants to read platform directories
 */
public enum OSDirectory {
    /** DAOS Base Directory */
	DAOS,
	/** Temporary folder used for view rebuilds */
	VIEWREBUILD,
	/** Directory of Domino executable */
	EXECUTABLE,
	/** Domino data directory */
	DATA,
	/** Domino temp directory */
	TEMP;
}