package com.mindoo.domino.jna.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class for the NUMBER_PAIR type
 * 
 * @author Karsten Lehmann
 */
public class NotesNumberPair extends BaseStructure {
	/** C type : NUMBER */
	public double Lower;
	/** C type : NUMBER */
	public double Upper;
	public NotesNumberPair() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("Lower", "Upper");
	}
	/**
	 * @param Lower C type : NUMBER<br>
	 * @param Upper C type : NUMBER
	 */
	public NotesNumberPair(double Lower, double Upper) {
		super();
		this.Lower = Lower;
		this.Upper = Upper;
	}
	public NotesNumberPair(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends NotesNumberPair implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesNumberPair implements Structure.ByValue {
		
	};
}
