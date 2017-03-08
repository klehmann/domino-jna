package com.mindoo.domino.jna.structs;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.structs.BaseStructure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class to the NAMES_LIST type on Windows 64 bit platforms
 * 
 * @author Karsten Lehmann
 */
public class WinNotesNamesListHeader64Struct extends BaseStructure {
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
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public WinNotesNamesListHeader64Struct() {
		super();
		setAlignType(Structure.ALIGN_DEFAULT);
	}
	
	public static WinNotesNamesListHeader64Struct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<WinNotesNamesListHeader64Struct>() {

			@Override
			public WinNotesNamesListHeader64Struct run() {
				return new WinNotesNamesListHeader64Struct();
			}});
	}
	
	@Override
	protected List<String> getFieldOrder() {
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
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public WinNotesNamesListHeader64Struct(short numNames, byte id[], byte product, byte check[], short authenticated) {
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
	
	public static WinNotesNamesListHeader64Struct newInstance(final short numNames, final byte id[], final byte product, final byte check[], final short authenticated) {
		return AccessController.doPrivileged(new PrivilegedAction<WinNotesNamesListHeader64Struct>() {

			@Override
			public WinNotesNamesListHeader64Struct run() {
				return new WinNotesNamesListHeader64Struct(numNames, id, product, check, authenticated);
			}});
	}

	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public WinNotesNamesListHeader64Struct(Pointer peer) {
		super(peer);
		setAlignType(Structure.ALIGN_DEFAULT);
	}
	
	public static WinNotesNamesListHeader64Struct newInstance(final Pointer p) {
		return AccessController.doPrivileged(new PrivilegedAction<WinNotesNamesListHeader64Struct>() {

			@Override
			public WinNotesNamesListHeader64Struct run() {
				return new WinNotesNamesListHeader64Struct(p);
			}});
	}
	
	public static class ByReference extends WinNotesNamesListHeader64Struct implements Structure.ByReference {
		
	};
	public static class ByValue extends WinNotesNamesListHeader64Struct implements Structure.ByValue {
		
	};
}
