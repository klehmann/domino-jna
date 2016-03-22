package com.mindoo.domino.jna.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class to the NAMES_LIST type on 64 bit platforms
 * 
 * @author Karsten Lehmann
 */
public class NotesNamesList64 extends Structure {
	/** Number of names in list */
	public int NumNames;

	/**
	 * User's license - now obsolete<br>
	 * C type : LICENSEID
	 */
	
	/**
	 * license number<br>
	 * C type : BYTE[5]
	 */
	public byte[] ID = new byte[5];
	/** product code, mfgr-specific */
	public byte Product;
	/**
	 * validity check field, mfgr-specific<br>
	 * C type : BYTE[2]
	 */
	public byte[] Check = new byte[2];

	
	public int Authenticated;
	
	public NotesNamesList64() {
		super();
		//set ALIGN to NONE, because the NAMES_LIST structure is directly followed by the usernames and wildcards in memory
		setAlignType(ALIGN_NONE);
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("NumNames", "ID", "Product", "Check", "Authenticated");
	}
	/**
	 * @param NumNames Number of names in list<br>
	 * @param License User's license - now obsolete<br>
	 * C type : LICENSEID
	 */
	public NotesNamesList64(short NumNames, byte ID[], byte Product, byte Check[], short Authenticated) {
		super();
		//set ALIGN to NONE, because the NAMES_LIST structure is directly followed by the usernames and wildcards in memory
		setAlignType(ALIGN_NONE);
		this.NumNames = NumNames;
		if ((ID.length != this.ID.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.ID = ID;
		this.Product = Product;
		if ((Check.length != this.Check.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.Check = Check;
		this.Authenticated = Authenticated;
	}
	public NotesNamesList64(Pointer peer) {
		super(peer);
		setAlignType(ALIGN_NONE);
	}
	public static class ByReference extends NotesNamesList32 implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesNamesList32 implements Structure.ByValue {
		
	};
}
