package com.mindoo.domino.jna.internal.structs;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.internal.structs.BaseStructure;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class for the TIME type
 * 
 * @author Karsten Lehmann
 */
public class NotesTimeStruct extends BaseStructure {
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
	public NotesTimeDateStruct GM;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesTimeStruct() {
		super();
	}
	
	public static NotesTimeStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesTimeStruct>() {

			@Override
			public NotesTimeStruct run() {
				return new NotesTimeStruct();
			}
		});
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("year", "month", "day", "weekday", "hour", "minute", "second", "hundredth", "dst", "zone", "GM");
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesTimeStruct(Pointer peer) {
		super(peer);
	}

	public static NotesTimeStruct newInstance(final Pointer p) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesTimeStruct>() {

			@Override
			public NotesTimeStruct run() {
				return new NotesTimeStruct(p);
			}
		});
	}

	public static class ByReference extends NotesTimeStruct implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesTimeStruct implements Structure.ByValue {
		
	};
}
