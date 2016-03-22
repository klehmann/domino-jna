package com.mindoo.domino.jna.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class for the TIMEDATE_PAIR type
 * 
 * @author Karsten Lehmann
 */
public class NotesTimeDatePair extends Structure {
	/** C type : TIMEDATE */
	public NotesTimeDate Lower;
	/** C type : TIMEDATE */
	public NotesTimeDate Upper;
	
	public NotesTimeDatePair() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("Lower", "Upper");
	}
	/**
	 * @param Lower C type : TIMEDATE<br>
	 * @param Upper C type : TIMEDATE
	 */
	public NotesTimeDatePair(NotesTimeDate Lower, NotesTimeDate Upper) {
		super();
		this.Lower = Lower;
		this.Upper = Upper;
	}
	public NotesTimeDatePair(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends NotesTimeDatePair implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesTimeDatePair implements Structure.ByValue {
		
	};
}
