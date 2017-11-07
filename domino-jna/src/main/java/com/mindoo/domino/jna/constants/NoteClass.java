package com.mindoo.domino.jna.constants;

import java.util.EnumSet;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;

import com.mindoo.domino.jna.internal.NotesCAPI;

/**
 * These bit masks define the types of notes in a database. The bit masks may be or'ed together
 * to specify more than one type of note.
 * 
 * @author Karsten Lehmann
 */
public enum NoteClass {
	
	/** old name for document note */
	DATA(NotesCAPI.NOTE_CLASS_DOCUMENT),

	/** document note */
	DOCUMENT(NotesCAPI.NOTE_CLASS_DOCUMENT),

	/** notefile info (help-about) note */
	INFO(NotesCAPI.NOTE_CLASS_INFO),
	
	/** form note */
	FORM(NotesCAPI.NOTE_CLASS_FORM),

	/** view note */
	VIEW(NotesCAPI.NOTE_CLASS_VIEW),
	
	/** icon note */
	ICON(NotesCAPI.NOTE_CLASS_ICON),
	
	/** design note collection */
	DESIGN(NotesCAPI.NOTE_CLASS_DESIGN),
	
	/** acl note */
	ACL(NotesCAPI.NOTE_CLASS_ACL),

	/** Notes product help index note */
	HELP_INDEX(NotesCAPI.NOTE_CLASS_HELP_INDEX),

	/** designer's help note */
	HELP(NotesCAPI.NOTE_CLASS_HELP),
	
	/** filter note */
	FILTER(NotesCAPI.NOTE_CLASS_FILTER),
	
	/** field note */
	FIELD(NotesCAPI.NOTE_CLASS_FIELD),
	
	/** replication formula */
	REPLFORMULA(NotesCAPI.NOTE_CLASS_REPLFORMULA),

	/** Private design note, use $PrivateDesign view to locate/classify */
	PRIVATE(NotesCAPI.NOTE_CLASS_PRIVATE),

	/** MODIFIER - default version of each */
	DEFAULT(NotesCAPI.NOTE_CLASS_DEFAULT),
	
	/** see {@link Search#NOTIFYDELETIONS} */
	NOTIFYDELETION(NotesCAPI.NOTE_CLASS_NOTIFYDELETION),
	
	/** all note types */
	ALL(NotesCAPI.NOTE_CLASS_ALL),
	
	/** all non-data notes */
	ALLNONDATA(NotesCAPI.NOTE_CLASS_ALLNONDATA),
	
	/** no notes */
	NONE(NotesCAPI.NOTE_CLASS_NONE),
	
	/** Define symbol for those note classes that allow only one such in a file */
	SINGLE_INSTANCE(NotesCAPI.NOTE_CLASS_SINGLE_INSTANCE);

	private int m_val;
	private static Map<Integer,NoteClass> classesByValue = new HashedMap<Integer, NoteClass>();
	static {
		for (NoteClass currClass : values()) {
			classesByValue.put(currClass.getValue(), currClass);
		}
	}
	
	public static NoteClass toNoteClass(int val) {
		return classesByValue.get(val);
	}
	
	NoteClass(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static EnumSet<NoteClass> toNoteClasses(int bitMask) {
		EnumSet<NoteClass> set = EnumSet.noneOf(NoteClass.class);
		if (bitMask==0) {
			set.add(NoteClass.NONE);
		}
		else {
			for (NoteClass currClass : values()) {
				if (currClass.getValue()!=0) {
					if ((bitMask & currClass.getValue()) == currClass.getValue()) {
						set.add(currClass);
					}
				}
			}
		}
		return set;
	}
	
	public static short toBitMask(EnumSet<NoteClass> noteClassSet) {
		int result = 0;
		if (noteClassSet!=null) {
			for (NoteClass currFind : values()) {
				if (noteClassSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}
	
	public static int toBitMaskInt(EnumSet<NoteClass> noteClassSet) {
		int result = 0;
		if (noteClassSet!=null) {
			for (NoteClass currFind : values()) {
				if (noteClassSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
