package com.mindoo.domino.jna.structs;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.structs.BaseStructure;
import com.mindoo.domino.jna.structs.NotesTimeDateStruct;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class for the TIMEDATE_PAIR type
 * 
 * @author Karsten Lehmann
 */
public class NotesTimeDatePairStruct extends BaseStructure {
	/** C type : TIMEDATE */
	public NotesTimeDateStruct Lower;
	/** C type : TIMEDATE */
	public NotesTimeDateStruct Upper;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesTimeDatePairStruct() {
		super();
	}
	
	public static NotesTimeDatePairStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesTimeDatePairStruct>() {

			@Override
			public NotesTimeDatePairStruct run() {
				return new NotesTimeDatePairStruct();
			}
		});
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("Lower", "Upper");
	}
	
	/**
	 * @param Lower C type : TIMEDATE<br>
	 * @param Upper C type : TIMEDATE
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesTimeDatePairStruct(NotesTimeDateStruct Lower, NotesTimeDateStruct Upper) {
		super();
		this.Lower = Lower;
		this.Upper = Upper;
	}
	
	public static NotesTimeDatePairStruct newInstance(final NotesTimeDateStruct Lower, final NotesTimeDateStruct Upper) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesTimeDatePairStruct>() {

			@Override
			public NotesTimeDatePairStruct run() {
				return new NotesTimeDatePairStruct(Lower, Upper);
			}
		});
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesTimeDatePairStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesTimeDatePairStruct newInstance(final Pointer p) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesTimeDatePairStruct>() {

			@Override
			public NotesTimeDatePairStruct run() {
				return new NotesTimeDatePairStruct(p);
			}
		});
	}
	
	public static class ByReference extends NotesTimeDatePairStruct implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesTimeDatePairStruct implements Structure.ByValue {
		
	};
}
