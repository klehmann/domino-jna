package com.mindoo.domino.jna.html;

import java.util.HashMap;
import java.util.Map;

import com.mindoo.domino.jna.internal.NotesConstants;

public enum ReferenceType {

	/**
	 * unknown purpose
	 */
	UNKNOWN(NotesConstants.HTMLAPI_REF_UNKNOWN),
	
	/**
	 * A tag HREF= value
	 */
	HREF(NotesConstants.HTMLAPI_REF_HREF),
	
	/**
	 * IMG tag SRC= value
	 */
	IMG(NotesConstants.HTMLAPI_REF_IMG),

	/**
	 * (I)FRAME tag SRC= value
	 */
	FRAME(NotesConstants.HTMLAPI_REF_FRAME),
	
	/**
	 * Java applet reference
	 */
	APPLET(NotesConstants.HTMLAPI_REF_APPLET),

	/**
	 * plugin SRC= reference
	 */
	EMBED(NotesConstants.HTMLAPI_REF_EMBED),
	
	/**
	 * active object DATA= referendce
	 */
	OBJECT(NotesConstants.HTMLAPI_REF_OBJECT),

	/**
	 * BASE tag value
	 */
	BASE(NotesConstants.HTMLAPI_REF_BASE),
	
	/**
	 * BODY BACKGROUND
	 */
	BACKGROUND(NotesConstants.HTMLAPI_REF_BACKGROUND),
	
	/**
	 * IMG SRC= value from MIME message
	 */
	CID(NotesConstants.HTMLAPI_REF_CID);
	
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
