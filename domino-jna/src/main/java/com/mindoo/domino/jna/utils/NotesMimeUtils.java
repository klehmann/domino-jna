package com.mindoo.domino.jna.utils;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.sun.jna.Memory;

public class NotesMimeUtils {

	/**
	 * Exports a MIME email to a file in EML format on disk
	 * 
	 * @param note note to export
	 * @param exportFilePath target path
	 */
	public static void exportMIMEToEML(NotesNote note, String exportFilePath) {
		NotesDatabase db = note.getParent();
		exportMIMEToEML(db.getServer(), db.getRelativeFilePath(), note.getNoteId(), exportFilePath);
	}

	/**
	 * Exports a MIME email to a file in EML format on disk
	 * 
	 * @param dbServer database server
	 * @param dbFilePath database filepath
	 * @param noteID note id of note to export
	 * @param exportFilePath target path
	 */
	public static void exportMIMEToEML(String dbServer, String dbFilePath, int noteID, String exportFilePath) {
		Memory dbServerLMBCS = NotesStringUtils.toLMBCS(dbServer, true);
		Memory dbFilePathLMBCS = NotesStringUtils.toLMBCS(dbFilePath, true);
		Memory retFullNetPath = new Memory(NotesConstants.MAXPATH);

		short result = NotesNativeAPI.get().OSPathNetConstruct(null, dbServerLMBCS, dbFilePathLMBCS, retFullNetPath);
		NotesErrorUtils.checkResult(result);

		Memory exportFilePathMem = NotesStringUtils.toLMBCS(exportFilePath, true);
		
		result = NotesNativeAPI.get().MIMEEMLExport(retFullNetPath, noteID, exportFilePathMem);
		NotesErrorUtils.checkResult(result);
	}

}
