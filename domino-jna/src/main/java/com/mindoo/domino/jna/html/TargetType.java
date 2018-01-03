package com.mindoo.domino.jna.html;

import java.util.HashMap;
import java.util.Map;

import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * This can be used to determine the URL address type, some operation on the generated HTML
 * text should be done according to the URL type. For more information, please refer the
 * samples released with the C API toolkit
 * 
 * @author Karsten Lehmann
 */
public enum TargetType {
	NONE(NotesConstants.UAT_None),
	SERVER(NotesConstants.UAT_Server),
	DATABASE(NotesConstants.UAT_Database),
	VIEW(NotesConstants.UAT_View),
	FORM(NotesConstants.UAT_Form),
	NAVIGATOR(NotesConstants.UAT_Navigator),
	AGENT(NotesConstants.UAT_Agent),
	DOCUMENT(NotesConstants.UAT_Document),
	/** internal filename of attachment */
	FILENAME(NotesConstants.UAT_Filename),
	/** external filename of attachment if different */
	ACTUALFILENAME(NotesConstants.UAT_ActualFilename),
	FIELD(NotesConstants.UAT_Field),
	FIELDOFFSET(NotesConstants.UAT_FieldOffset),
	FIELDSUBOFFSET(NotesConstants.UAT_FieldSuboffset),
	PAGE(NotesConstants.UAT_Page),
	FRAMESET(NotesConstants.UAT_FrameSet),
	IMAGERESOURCE(NotesConstants.UAT_ImageResource),
	CSSRESOURCE(NotesConstants.UAT_CssResource),
	JAVASCRIPTLIB(NotesConstants.UAT_JavascriptLib),
	FILERESOURCE(NotesConstants.UAT_FileResource),
	ABOUT(NotesConstants.UAT_About),
	HELP(NotesConstants.UAT_Help),
	ICON(NotesConstants.UAT_Icon),
	SEARCHFORM(NotesConstants.UAT_SearchForm),
	SEARCHSITEFORM(NotesConstants.UAT_SearchSiteForm),
	OUTLINE(NotesConstants.UAT_Outline);

	int m_type;

	private static Map<Integer, TargetType> typesByIntValue = new HashMap<Integer, TargetType>();

	static {
		for (TargetType currType : TargetType.values()) {
			typesByIntValue.put(currType.m_type, currType);
		}
	}

	TargetType(int type) {
		m_type = type;
	}

	public int getValue() {
		return m_type;
	}

	public static TargetType getType(int intVal) {
		TargetType type = typesByIntValue.get(intVal);
		if (type==null)
			throw new IllegalArgumentException("Unknown int value: "+intVal);
		return type;
	}
};