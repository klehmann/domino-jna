
package com.mindoo.domino.jna.utils;

import java.io.File;

import com.mindoo.domino.jna.constants.OSDirectory;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.sun.jna.Memory;

/**
 * Utility functions for the Domino platform and its OS platform
 */
public class PlatformUtils {

	/**
	 * This function returns the path specification of the local Domino or Notes
	 * executable / data / temp directory.<br>
	 * <br>
	 * Author: Ulrich Krause
	 * 
	 * @param osDirectory
	 *            {@link OSDirectory}
	 * @return path
	 */
	public static String getOsDirectory(OSDirectory osDirectory) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		String strReturn = "";
		Memory retPathName = new Memory(NotesCAPI.MAXPATH);
		switch (osDirectory) {
		case EXECUTABLE:
			notesAPI.OSGetExecutableDirectory(retPathName);
			break;
		case DATA:
			notesAPI.OSGetDataDirectory(retPathName);
			break;
		case TEMP:
			notesAPI.OSGetSystemTempDirectory(retPathName, NotesCAPI.MAXPATH);
			break;
		case VIEWREBUILD:
			notesAPI.NIFGetViewRebuildDir(retPathName, NotesCAPI.MAXPATH);
			break;
		case DAOS:
			notesAPI.DAOSGetBaseStoragePath(retPathName, NotesCAPI.MAXPATH);
			break;
		default:
			throw new IllegalArgumentException("Unsupported directory type: "+osDirectory);
		}
		notesAPI.OSPathAddTrailingPathSep(retPathName);
		strReturn = NotesStringUtils.fromLMBCS(retPathName, NotesStringUtils.getNullTerminatedLength(retPathName));

		File file = new File(strReturn);
		if (!file.exists()) {
			strReturn = "";
		}
		
		return strReturn;
	}

}