package com.mindoo.domino.jna.structs;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class for the TIMEDATE type
 * 
 * @author Karsten Lehmann
 */
public class NotesTimeDate extends Structure {
	/** C type : DWORD[2] */
	public int[] Innards = new int[2];
	public NotesTimeDate() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("Innards");
	}
	/** @param Innards C type : DWORD[2] */
	public NotesTimeDate(int Innards[]) {
		super();
		if ((Innards.length != this.Innards.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.Innards = Innards;
	}
	public NotesTimeDate(Pointer peer) {
		super(peer);
	}
	
	@Override
	public String toString() {
		return NotesDateTimeUtils.toString(this);
	}
	
	public static class ByReference extends NotesTimeDate implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesTimeDate implements Structure.ByValue {
		
	};
	
	/**
	 * Checks whether the timedate has a date portion
	 * 
	 * @return true if date part exists
	 */
	public boolean hasDate() {
        boolean hasDate=(Innards[1]!=0 && Innards[1]!=NotesCAPI.ANYDAY);
		return hasDate;
	}
	
	/**
	 * Checks whether the timedate has a time portion
	 * 
	 * @return true if time part exists
	 */
	public boolean hasTime() {
        boolean hasDate=(Innards[0]!=0 && Innards[0]!=NotesCAPI.ALLDAY);
		return hasDate;
	}
	
	/**
	 * Converts the time date to a calendar
	 * 
	 * @return calendar or null if data is invalid
	 */
	public Calendar toCalendar() {
		return NotesDateTimeUtils.innardsToCalendar(NotesDateTimeUtils.isDaylightTime(), NotesDateTimeUtils.getGMTOffset(), this.Innards);
	}
	
	/**
	 * Converts the time date to a Java {@link Date}
	 * 
	 * @return date or null if data is invalid
	 */
	public Date toDate() {
		Calendar cal = toCalendar();
		return cal==null ? null : cal.getTime();
	}
}
