package com.mindoo.domino.jna.structs;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.structs.BaseStructure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class for the NUMBER_PAIR type
 * 
 * @author Karsten Lehmann
 */
public class NotesNumberPairStruct extends BaseStructure {
	/** C type : NUMBER */
	public double Lower;
	/** C type : NUMBER */
	public double Upper;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesNumberPairStruct() {
		super();
	}
	
	public static NotesNumberPairStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesNumberPairStruct>() {

			@Override
			public NotesNumberPairStruct run() {
				return new NotesNumberPairStruct();
			}
		});
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("Lower", "Upper");
	}
	
	/**
	 * @param Lower C type : NUMBER<br>
	 * @param Upper C type : NUMBER
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesNumberPairStruct(double Lower, double Upper) {
		super();
		this.Lower = Lower;
		this.Upper = Upper;
	}
	
	public static NotesNumberPairStruct newInstance(final double Lower, final double Upper) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesNumberPairStruct>() {

			@Override
			public NotesNumberPairStruct run() {
				return new NotesNumberPairStruct(Lower, Upper);
			}
		});
	}

	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesNumberPairStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesNumberPairStruct newInstance(final Pointer p) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesNumberPairStruct>() {

			@Override
			public NotesNumberPairStruct run() {
				return new NotesNumberPairStruct(p);
			}
		});
	}
	
	public static class ByReference extends NotesNumberPairStruct implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesNumberPairStruct implements Structure.ByValue {
		
	};
}
