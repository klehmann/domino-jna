package com.mindoo.domino.jna.structs;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Internal class to decode the SSO_TOKEN structure values
 * 
 * @author Karsten Lehmann
 */
public class NotesSSOTokenStruct64 extends BaseStructure {
	public int mhName;
	public int mhDomainList;
	public short wNumDomains;
	public int bSecureOnly;
	public int mhData;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesSSOTokenStruct64() {
		super();
	}
	
	public static NotesSSOTokenStruct64 newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesSSOTokenStruct64>() {

			@Override
			public NotesSSOTokenStruct64 run() {
				return new NotesSSOTokenStruct64();
			}
		});
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("mhName", "mhDomainList", "wNumDomains", "bSecureOnly", "mhData");
	}
	
	/**
	 * Creates a new instance
	 * 
	 * @param mhName name for the token when set as a cookie
	 * @param mhDomainList list of DNS domains for the token when set as a cookie
	 * @param wNumDomains Total number of domains contained in the mhDomainList member
	 * @param bSecureOnly BOOL recommending that the token only be set on a secure connection.
	 * @param mhData MEMHANDLE to a the null-terminated token data.
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesSSOTokenStruct64(int mhName, int mhDomainList, short wNumDomains, int bSecureOnly, int mhData) {
		super();
		this.mhName = mhName;
		this.mhDomainList = mhDomainList;
		this.wNumDomains = wNumDomains;
		this.bSecureOnly = bSecureOnly;
		this.mhData = mhData;
	}
	
	public static NotesSSOTokenStruct64 newInstance(final int mhName, final int mhDomainList, final short wNumDomains, final int bSecureOnly, final int mhData) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesSSOTokenStruct64>() {

			@Override
			public NotesSSOTokenStruct64 run() {
				return new NotesSSOTokenStruct64(mhName, mhDomainList, wNumDomains, bSecureOnly, mhData);
			}
		});
	}

	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesSSOTokenStruct64(Pointer peer) {
		super(peer);
	}
	
	public static NotesSSOTokenStruct64 newInstance(final Pointer p) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesSSOTokenStruct64>() {

			@Override
			public NotesSSOTokenStruct64 run() {
				return new NotesSSOTokenStruct64(p);
			}
		});
	}

	public boolean isSecureOnly() {
		return bSecureOnly!=0;
	}

	public static class ByReference extends NotesSSOTokenStruct64 implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesSSOTokenStruct64 implements Structure.ByValue {
		
	};
}
