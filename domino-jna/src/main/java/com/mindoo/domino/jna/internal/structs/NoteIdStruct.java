package com.mindoo.domino.jna.internal.structs;

import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * JNA class for the NOTEID type
 * 
 * @author Karsten Lehmann
 */
public class NoteIdStruct extends BaseStructure implements Serializable, IAdaptable {
	public int nid;
	
	public static NoteIdStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NoteIdStruct>() {

			@Override
			public NoteIdStruct run() {
				return new NoteIdStruct();
			}
		});
	}
	
	public static NoteIdStruct newInstance(final int nid) {
		return AccessController.doPrivileged(new PrivilegedAction<NoteIdStruct>() {

			@Override
			public NoteIdStruct run() {
				return new NoteIdStruct(nid);
			}
		});
	}
	
	public static NoteIdStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NoteIdStruct>() {

			@Override
			public NoteIdStruct run() {
				NoteIdStruct newObj = new NoteIdStruct(peer);
				newObj.read();
				return newObj;
			}
		});
	}

	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NoteIdStruct() {
		super();
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("nid");
	}
		
	/**
	 * Creates a new instance
	 * 
	 * @param nid note id
	 */
	public NoteIdStruct(int nid) {
		super();
		this.nid = nid;
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NoteIdStruct(Pointer peer) {
		super(peer);
	}
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NoteIdStruct.class) {
			return (T) this;
		}
		else if (clazz == Pointer.class) {
			return (T) getPointer();
		}
		return null;
	}
	
	public static class ByReference extends NoteIdStruct implements Structure.ByReference {
		private static final long serialVersionUID = -3097461571616131768L;
		
	};
	public static class ByValue extends NoteIdStruct implements Structure.ByValue {
		private static final long serialVersionUID = -5045877402293096954L;
		
	};
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof NoteIdStruct) {
			return this.nid == ((NoteIdStruct)o).nid;
		}
		return false;
	}
	
	/**
	 * Creates a new {@link NoteIdStruct} instance with the same data as this one
	 */
	public NoteIdStruct clone() {
		NoteIdStruct clone = new NoteIdStruct();
		clone.nid = this.nid;
		clone.write();
		return clone;
	}
	
}
