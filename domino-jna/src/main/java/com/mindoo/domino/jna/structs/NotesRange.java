package com.mindoo.domino.jna.structs;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * JNA class for the RANGE type
 * 
 * @author Karsten Lehmann
 */
public class NotesRange extends Structure {
	/** list entries following */
	public short ListEntries;
	/** range entries following */
	public short RangeEntries;
	public NotesRange() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("ListEntries", "RangeEntries");
	}
	/**
	 * @param ListEntries list entries following
	 * @param RangeEntries range entries following
	 */
	public NotesRange(short ListEntries, short RangeEntries) {
		super();
		this.ListEntries = ListEntries;
		this.RangeEntries = RangeEntries;
	}
	public NotesRange(Pointer peer) {
		super(peer);
	}
	public static class ByReference extends NotesRange implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesRange implements Structure.ByValue {
		
	};
}
