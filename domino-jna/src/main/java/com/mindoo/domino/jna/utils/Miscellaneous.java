
package com.mindoo.domino.jna.utils;

import com.mindoo.domino.jna.constants.OSDirectory;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.sun.jna.Memory;

public class Miscellaneous {

	/**
	 * Given the address of a text buffer, this function returns the path
	 * specification of the local Domino or Notes executable / data / temp
	 * directory,
	 * 
	 * @author Ulrich
	 * @param osDirectory
	 *            {@link OSDirectory}
	 * @return The address of a text buffer dimensioned with MAXPATH, in which a
	 *         null-terminated string containing the full path specification. OS
	 *         specific trailing separator is added if not present
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
		default:
		}
		notesAPI.OSPathAddTrailingPathSep(retPathName);
		strReturn = NotesStringUtils.fromLMBCS(retPathName, NotesStringUtils.getNullTerminatedLength(retPathName));

		return strReturn;
	}

}
