package com.mindoo.domino.jna.html;

import java.util.HashMap;
import java.util.Map;

import com.mindoo.domino.jna.internal.NotesCAPI;

public enum ReferenceType {

	/**
	 * unknown purpose
	 */
	UNKNOWN(NotesCAPI.HTMLAPI_REF_UNKNOWN),
	
	/**
	 * A tag HREF= value
	 */
	HREF(NotesCAPI.HTMLAPI_REF_HREF),
	
	/**
	 * IMG tag SRC= value
	 */
	IMG(NotesCAPI.HTMLAPI_REF_IMG),

	/**
	 * (I)FRAME tag SRC= value
	 */
	FRAME(NotesCAPI.HTMLAPI_REF_FRAME),
	
	/**
	 * Java applet reference
	 */
	APPLET(NotesCAPI.HTMLAPI_REF_APPLET),

	/**
	 * plugin SRC= reference
	 */
	EMBED(NotesCAPI.HTMLAPI_REF_EMBED),
	
	/**
	 * active object DATA= referendce
	 */
	OBJECT(NotesCAPI.HTMLAPI_REF_OBJECT),

	/**
	 * BASE tag value
	 */
	BASE(NotesCAPI.HTMLAPI_REF_BASE),
	
	/**
	 * BODY BACKGROUND
	 */
	BACKGROUND(NotesCAPI.HTMLAPI_REF_BACKGROUND),
	
	/**
	 * IMG SRC= value from MIME message
	 */
	CID(NotesCAPI.HTMLAPI_REF_CID);
	
	int m_type;
	
	private static Map<Integer, ReferenceType> typesByIntValue = new HashMap<Integer, ReferenceType>();

    static {
        for (ReferenceType currType : ReferenceType.values()) {
            typesByIntValue.put(currType.m_type, currType);
        }
    }
    
	ReferenceType(int type) {
		m_type = type;
	}
	
	public int getValue() {
		return m_type;
	}
	
	public static ReferenceType getType(int intVal) {
		ReferenceType type = typesByIntValue.get(intVal);
		if (type==null)
			throw new IllegalArgumentException("Unknown int value: "+intVal);
		return type;
	}
}
