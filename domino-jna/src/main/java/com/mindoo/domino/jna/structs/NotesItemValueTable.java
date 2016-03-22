package com.mindoo.domino.jna.structs;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * JNA class for the ITEM_VALUE_TABLE type
 * 
 * @author Karsten Lehmann
 */
public class NotesItemValueTable extends Structure {
	/** total length of this buffer */
	public short Length;
	/** number of items in the table */
	public short Items;
	
	public NotesItemValueTable() {
		super();
	}
	
	protected List<? > getFieldOrder() {
		return Arrays.asList("Length", "Items");
	}
	
	/**
	 * @param Length total length of this buffer<br>
	 * @param Items number of items in the table
	 */
	public NotesItemValueTable(short Length, short Items) {
		super();
		this.Length = Length;
		this.Items = Items;
	}
	
	public NotesItemValueTable(Pointer peer) {
		super(peer);
	}
	
	public static class ByReference extends NotesItemValueTable implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesItemValueTable implements Structure.ByValue {
		
	};
	
	public int getLengthAsInt() {
		return Length  & 0xffff;
	}

	public int getItemsAsInt() {
		return Items  & 0xffff;
	}

}
