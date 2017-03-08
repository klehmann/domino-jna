package com.mindoo.domino.jna.structs;

import com.mindoo.domino.jna.structs.BaseStructure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

/**
 * JNA class for the RANGE type
 * 
 * @author Karsten Lehmann
 */
public class NotesRangeStruct extends BaseStructure {
	/** list entries following */
	public short ListEntries;
	/** range entries following */
	public short RangeEntries;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesRangeStruct() {
		super();
	}
	
	public static NotesRangeStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesRangeStruct>() {

			@Override
			public NotesRangeStruct run() {
				return new NotesRangeStruct();
			}
		});
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("ListEntries", "RangeEntries");
	}
	
	/**
	 * @param ListEntries list entries following
	 * @param RangeEntries range entries following
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesRangeStruct(short ListEntries, short RangeEntries) {
		super();
		this.ListEntries = ListEntries;
		this.RangeEntries = RangeEntries;
	}
	
	public static NotesRangeStruct newInstance(final short ListEntries, final short RangeEntries) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesRangeStruct>() {

			@Override
			public NotesRangeStruct run() {
				return new NotesRangeStruct(ListEntries, RangeEntries);
			}
		});
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesRangeStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesRangeStruct newInstance(final Pointer p) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesRangeStruct>() {

			@Override
			public NotesRangeStruct run() {
				return new NotesRangeStruct(p);
			}
		});
	}
	
	public static class ByReference extends NotesRangeStruct implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesRangeStruct implements Structure.ByValue {
		
	};
}
