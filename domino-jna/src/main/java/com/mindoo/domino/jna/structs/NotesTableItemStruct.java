package com.mindoo.domino.jna.structs;
import com.mindoo.domino.jna.structs.BaseStructure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

/**
 * JNA class for the ITEM type
 * 
 * @author Karsten Lehmann
 */
public class NotesTableItemStruct extends BaseStructure {
	/** Length of Item Name following this struct. may be zero (0) if not required by func(s)*/
	public short NameLength;
	/** Length of Item Value following this struct, incl. Notes data type.        */
	public short ValueLength;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesTableItemStruct() {
		super();
	}
	
	public static NotesTableItemStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesTableItemStruct>() {

			@Override
			public NotesTableItemStruct run() {
				return new NotesTableItemStruct();
			}});
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("NameLength", "ValueLength");
	}
	
	/**
	 * @param NameLength length of item name
	 * @param ValueLength length of item value
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesTableItemStruct(short NameLength, short ValueLength) {
		super();
		this.NameLength = NameLength;
		this.ValueLength = ValueLength;
	}
	
	public static NotesTableItemStruct newInstance(final short NameLength, final short ValueLength) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesTableItemStruct>() {

			@Override
			public NotesTableItemStruct run() {
				return new NotesTableItemStruct(NameLength, ValueLength);
			}});
	}

	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesTableItemStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesTableItemStruct newInstance(final Pointer p) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesTableItemStruct>() {

			@Override
			public NotesTableItemStruct run() {
				return new NotesTableItemStruct(p);
			}});
	}
	
	public static class ByReference extends NotesTableItemStruct implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesTableItemStruct implements Structure.ByValue {
		
	};
	
	public int getNameLengthAsInt() {
		return NameLength & 0xffff;
	}

	public int getValueLengthAsInt() {
		return ValueLength & 0xffff;
	}

}
