package com.mindoo.domino.jna.mime;

import com.mindoo.domino.jna.NotesNote;

public interface IMimeDataAccessService {

	/**
	 * Reads {@link MIMEData} from a note
	 * 
	 * @param note note
	 * @param itemName MIME item name
	 * @return MIME data or null if item could not be found
	 */
	public MIMEData getMimeData(NotesNote note, String itemName);
	
	/**
	 * Writes {@link MIMEData} to a note
	 * 
	 * @param note note
	 * @param itemName MIME item name
	 * @param mimeData MIME data to write
	 */
	public void setMimeData(NotesNote note, String itemName, MIMEData mimeData);
	
}
