package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Flags for {@link NotesItem#TYPE_MIME_PART} items
 * 
 * @author Karsten Lehmann
 */
public enum MimePartOptions {
	
	/** Mime part has boundary. */
	HAS_BOUNDARY (NotesConstants.MIME_PART_HAS_BOUNDARY),
	
	/** Mime part has headers. */
	HAS_HEADERS (NotesConstants.MIME_PART_HAS_HEADERS),

	/** Mime part has body in database object. */
	BODY_IN_DBOBJECT (NotesConstants.MIME_PART_BODY_IN_DBOBJECT),
	
	/** Mime part has shared database object. Used only with MIME_PART_BODY_IN_DBOBJECT. */
	SHARED_DBOBJECT (NotesConstants.MIME_PART_SHARED_DBOBJECT),

	/** Skip for conversion, only used during MIME-&gt;CD conversion. */
	SKIP_FOR_CONVERSION (NotesConstants.MIME_PART_SKIP_FOR_CONVERSION);
	
	private int m_val;
	
	MimePartOptions(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static short toBitMask(EnumSet<MimePartOptions> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (MimePartOptions currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}
	
	public static int toBitMaskInt(EnumSet<MimePartOptions> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (MimePartOptions currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
