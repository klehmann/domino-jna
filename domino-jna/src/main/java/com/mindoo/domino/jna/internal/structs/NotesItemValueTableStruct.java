package com.mindoo.domino.jna.internal.structs;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.internal.structs.BaseStructure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class for the ITEM_VALUE_TABLE type
 * 
 * @author Karsten Lehmann
 */
public class NotesItemValueTableStruct extends BaseStructure {
	/** total length of this buffer */
	public short Length;
	/** number of items in the table */
	public short Items;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesItemValueTableStruct() {
		super();
	}
	
	public static NotesItemValueTableStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesItemValueTableStruct>() {

			@Override
			public NotesItemValueTableStruct run() {
				return new NotesItemValueTableStruct();
			}
		});
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("Length", "Items");
	}
	
	/**
	 * @param length total length of this buffer<br>
	 * @param items number of items in the table
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesItemValueTableStruct(short length, short items) {
		super();
		this.Length = length;
		this.Items = items;
	}
	
	public static NotesItemValueTableStruct newInstance(final short length, final short items) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesItemValueTableStruct>() {

			@Override
			public NotesItemValueTableStruct run() {
				return new NotesItemValueTableStruct(length, items);
			}
		});
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesItemValueTableStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesItemValueTableStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesItemValueTableStruct>() {

			@Override
			public NotesItemValueTableStruct run() {
				return new NotesItemValueTableStruct(peer);
			}
		});
	}

	public static class ByReference extends NotesItemValueTableStruct implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesItemValueTableStruct implements Structure.ByValue {
		
	};
	
	public int getLengthAsInt() {
		return Length  & 0xffff;
	}

	public int getItemsAsInt() {
		return Items  & 0xffff;
	}

}
