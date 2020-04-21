package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.mime.MIMEStream;

/**
 * Flags and return values used by {@link MIMEStream} APIs.
 * 
 * @author Karsten Lehmann
 */
public enum MimeStreamOpenOptions {
	
	/** Include MIME Headers */
	MIME_INCLUDE_HEADERS (NotesConstants.MIME_STREAM_MIME_INCLUDE_HEADERS),
	
	/** Include RFC822 Headers */
	RFC2822_INCLUDE_HEADERS (NotesConstants.MIME_STREAM_RFC2822_INCLUDE_HEADERS);
	
	
	private int m_val;
	
	MimeStreamOpenOptions(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMaskInt(EnumSet<MimeStreamOpenOptions> flags) {
		int result = 0;
		if (flags!=null) {
			for (MimeStreamOpenOptions currFind : values()) {
				if (flags.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
