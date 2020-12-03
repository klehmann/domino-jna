package com.mindoo.domino.jna.mime;

import java.util.Iterator;

import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.utils.PlatformUtils;

/**
 * Simple API to read/write MIME data from/to a {@link NotesNote}. We
 * support HTML, plaintext, embedded images and attachments.<br>
 * <br>
 * To use this class, you either need to include a dependency for
 * "domino-jna-mime-jakartamail" or "domino-jna-mime-javaxmail" which
 * contain implementations for the service {@link IMimeDataAccessService}
 * using the new Jakarta Mail API or the old JavaX Mail API.
 * 
 * @author Karsten Lehmann
 */
public class MIMEUtils {

	private static IMimeDataAccessService getAccessService() {
		Iterator<IMimeDataAccessService> implementations = PlatformUtils.getService(IMimeDataAccessService.class);
		if(!implementations.hasNext()) {
			throw new IllegalStateException("Unable to locate implementation of " + IMimeDataAccessService.class.getName());
		}
		return implementations.next();
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
