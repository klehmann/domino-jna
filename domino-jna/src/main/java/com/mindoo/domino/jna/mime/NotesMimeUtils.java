package com.mindoo.domino.jna.mime;

import java.util.Iterator;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
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

	private static IMimeDataAccessService getAccessService() {
		IMimeDataAccessService service = null;
		int servicePrio = Integer.MAX_VALUE;
		
		Iterator<IMimeDataAccessService> implementations = PlatformUtils.getService(IMimeDataAccessService.class);
		while (implementations.hasNext()) {
			IMimeDataAccessService currService = implementations.next();
			int currPrio = currService.getPriority();
			//pick service with lowest priority value
			if (service==null || currPrio<servicePrio) {
				service = currService;
				servicePrio = currPrio;
			}
		}
		
		if(service==null) {
			throw new IllegalStateException("Unable to locate implementation of " + IMimeDataAccessService.class.getName());
		}
		return service;
	}

	/**
	 * Reads {@link MIMEData} from a note
	 * 
	 * @param note note
	 * @param itemName MIME item name
	 * @return MIME data or null if item could not be found
	 */
	public static MIMEData getMimeData(NotesNote note, String itemName) {
		return getAccessService().getMimeData(note, itemName);
	}

	/**
	 * Writes {@link MIMEData} to a note
	 * 
	 * @param note note
	 * @param itemName MIME item name
	 * @param mimeData MIME data to write
	 */
	public static void setMimeData(NotesNote note, String itemName, MIMEData mimeData) {
		getAccessService().setMimeData(note, itemName, mimeData);
	}
}
