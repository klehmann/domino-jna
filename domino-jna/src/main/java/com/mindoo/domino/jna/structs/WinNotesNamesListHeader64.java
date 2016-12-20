package com.mindoo.domino.jna.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class to the NAMES_LIST type on Windows 64 bit platforms
 * 
 * @author Karsten Lehmann
 */
public class WinNotesNamesListHeader64 extends BaseStructure {
	/** Number of names in list */
	public short NumNames;

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

	/**
	 * Flag to mark the user as already authenticated, e.g. via web server
	 */
	public int Authenticated;
	
	public WinNotesNamesListHeader64() {
		super();
		setAlignType(Structure.ALIGN_DEFAULT);
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("NumNames", "ID", "Product", "Check", "Authenticated");
	}
	
	/**
	 * Creates a new instance
	 * 
	 * @param numNames number of names in the list
	 * @param id info from LICENSEID, should be empty
	 * @param product info from LICENSEID, should be empty
	 * @param check info from LICENSEID, should be empty
	 * @param authenticated  Flag to mark the user as already authenticated, e.g. via web server
	 */
	public WinNotesNamesListHeader64(short numNames, byte id[], byte product, byte check[], short authenticated) {
		super();
		this.NumNames = numNames;
		if ((id.length != this.ID.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.ID = id;
		this.Product = product;
		if ((check.length != this.Check.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.Check = check;
		this.Authenticated = authenticated;
		setAlignType(Structure.ALIGN_DEFAULT);
	}
	public WinNotesNamesListHeader64(Pointer peer) {
		super(peer);
		setAlignType(Structure.ALIGN_DEFAULT);
	}
	public static class ByReference extends WinNotesNamesListHeader64 implements Structure.ByReference {
		
	};
	public static class ByValue extends WinNotesNamesListHeader64 implements Structure.ByValue {
		
	};
}
