package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Define the MIME stream itemize options
 * 
 * @author Karsten Lehmann
 */
public enum MimeStreamItemizeOptions {
	
	/** don't delete attachment during itemization. */
	NO_DELETE_ATTACHMENTS (NotesConstants.MIME_STREAM_NO_DELETE_ATTACHMENTS),
	
	/** create items for part headers (only for initial RFC822 headers, not MIME part headers) */
	ITEMIZE_HEADERS (NotesConstants.MIME_STREAM_ITEMIZE_HEADERS),

	/** create items for part bodies */
	ITEMIZE_BODY (NotesConstants.MIME_STREAM_ITEMIZE_BODY);
	
	private int m_val;
	
	MimeStreamItemizeOptions(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMaskInt(EnumSet<MimeStreamItemizeOptions> flags) {
		int result = 0;
		if (flags!=null) {
			for (MimeStreamItemizeOptions currFind : values()) {
				if (flags.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
