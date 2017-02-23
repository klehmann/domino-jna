package com.mindoo.domino.jna.structs;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.structs.BaseStructure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class for the ITEM_TABLE type
 * 
 * @author Karsten Lehmann
 */
public class NotesItemTableStruct extends BaseStructure {
	/** total length of this buffer */
	public short Length;
	/** number of items in the table */
	public short Items;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesItemTableStruct() {
		super();
	}
	
	public static NotesItemTableStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesItemTableStruct>() {

			@Override
			public NotesItemTableStruct run() {
				return new NotesItemTableStruct();
			}});
	};
	
	protected List<? > getFieldOrder() {
		return Arrays.asList("Length", "Items");
	}
	
	/**
	 * @param length total length of this buffer<br>
	 * @param items number of items in the table
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesItemTableStruct(short length, short items) {
		super();
		this.Length = length;
		this.Items = items;
	}
	
	public static NotesItemTableStruct newInstance(final short length, final short items) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesItemTableStruct>() {

			@Override
			public NotesItemTableStruct run() {
				return new NotesItemTableStruct(length, items);
			}});
	};

	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesItemTableStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesItemTableStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesItemTableStruct>() {

			@Override
			public NotesItemTableStruct run() {
				return new NotesItemTableStruct(peer);
			}});
	};
	
	public static class ByReference extends NotesItemTableStruct implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesItemTableStruct implements Structure.ByValue {
		
	};
	
	public int getLengthAsInt() {
		return Length  & 0xffff;
	}

	public int getItemsAsInt() {
		return Items  & 0xffff;
	}

}
