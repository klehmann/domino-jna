package com.mindoo.domino.jna.html;

import java.util.HashMap;
import java.util.Map;

import com.mindoo.domino.jna.internal.NotesCAPI;

/**
 * This can be used to determine the URL address type, some operation on the generated HTML
 * text should be done according to the URL type. For more information, please refer the
 * samples released with the C API toolkit
 * 
 * @author Karsten Lehmann
 */
public enum TargetType {
	NONE(NotesCAPI.UAT_None),
	SERVER(NotesCAPI.UAT_Server),
	DATABASE(NotesCAPI.UAT_Database),
	VIEW(NotesCAPI.UAT_View),
	FORM(NotesCAPI.UAT_Form),
	NAVIGATOR(NotesCAPI.UAT_Navigator),
	AGENT(NotesCAPI.UAT_Agent),
	DOCUMENT(NotesCAPI.UAT_Document),
	/** internal filename of attachment */
	FILENAME(NotesCAPI.UAT_Filename),
	/** external filename of attachment if different */
	ACTUALFILENAME(NotesCAPI.UAT_ActualFilename),
	FIELD(NotesCAPI.UAT_Field),
	FIELDOFFSET(NotesCAPI.UAT_FieldOffset),
	FIELDSUBOFFSET(NotesCAPI.UAT_FieldSuboffset),
	PAGE(NotesCAPI.UAT_Page),
	FRAMESET(NotesCAPI.UAT_FrameSet),
	IMAGERESOURCE(NotesCAPI.UAT_ImageResource),
	CSSRESOURCE(NotesCAPI.UAT_CssResource),
	JAVASCRIPTLIB(NotesCAPI.UAT_JavascriptLib),
	FILERESOURCE(NotesCAPI.UAT_FileResource),
	ABOUT(NotesCAPI.UAT_About),
	HELP(NotesCAPI.UAT_Help),
	ICON(NotesCAPI.UAT_Icon),
	SEARCHFORM(NotesCAPI.UAT_SearchForm),
	SEARCHSITEFORM(NotesCAPI.UAT_SearchSiteForm),
	OUTLINE(NotesCAPI.UAT_Outline);

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