package com.mindoo.domino.jna.structs;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class for the ITEM_TABLE type
 * 
 * @author Karsten Lehmann
 */
public class NotesItemTable extends BaseStructure {
	/** total length of this buffer */
	public short Length;
	/** number of items in the table */
	public short Items;
	
	public NotesItemTable() {
		super();
	}
	
	protected List<? > getFieldOrder() {
		return Arrays.asList("Length", "Items");
	}
	
	/**
	 * @param Length total length of this buffer<br>
	 * @param Items number of items in the table
	 */
	public NotesItemTable(short Length, short Items) {
		super();
		this.Length = Length;
		this.Items = Items;
	}
	
	public NotesItemTable(Pointer peer) {
		super(peer);
	}
	
	public static class ByReference extends NotesItemTable implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesItemTable implements Structure.ByValue {
		
	};
	
	public int getLengthAsInt() {
		return Length  & 0xffff;
	}

	public int getItemsAsInt() {
		return Items  & 0xffff;
	}

}
