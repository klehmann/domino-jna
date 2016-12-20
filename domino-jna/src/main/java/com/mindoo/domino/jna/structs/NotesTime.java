package com.mindoo.domino.jna.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class for the TIME type
 * 
 * @author Karsten Lehmann
 */
public class NotesTime extends BaseStructure {
	/** 1-32767 */
	public int year;
	/** 1-12 */
	public int month;
	/** 1-31 */
	public int day;
	/** 1-7, Sunday is 1 */
	public int weekday;
	/** 0-23 */
	public int hour;
	/** 0-59 */
	public int minute;
	/** 0-59 */
	public int second;
	/** 0-99 */
	public int hundredth;
	/** FALSE or TRUE */
	public int dst;
	/** -11 to +11 */
	public int zone;
//	public long timeDatePtr;
//	public int Innard1;
//	public int Innard2;
	
//	public int[] Innards = new int[2];
	public NotesTimeDate GM;
	public NotesTime() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("year", "month", "day", "weekday", "hour", "minute", "second", "hundredth", "dst", "zone", "GM");
	}
	public NotesTime(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends NotesTime implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesTime implements Structure.ByValue {
		
	};
}
