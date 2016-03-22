package com.mindoo.domino.jna.structs;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * JNA class for the ITEM type
 * 
 * @author Karsten Lehmann
 */
public class NotesItem extends Structure {
	/** Length of Item Name following this struct. may be zero (0) if not required by func(s)*/
	public short NameLength;
	/** Length of Item Value following this struct, incl. Notes data type.        */
	public short ValueLength;
	
	public NotesItem() {
		super();
	}
	
	protected List<? > getFieldOrder() {
		return Arrays.asList("NameLength", "ValueLength");
	}
	
	/**
	 * @param NameLength length of item name
	 * @param ValueLength length of item value
	 */
	public NotesItem(short NameLength, short ValueLength) {
		super();
		this.NameLength = NameLength;
		this.ValueLength = ValueLength;
	}
	
	public NotesItem(Pointer peer) {
		super(peer);
	}
	
	public static class ByReference extends NotesItem implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesItem implements Structure.ByValue {
		
	};
	
	public int getNameLengthAsInt() {
		return NameLength & 0xffff;
	}

	public int getValueLengthAsInt() {
		return ValueLength & 0xffff;
	}

}
