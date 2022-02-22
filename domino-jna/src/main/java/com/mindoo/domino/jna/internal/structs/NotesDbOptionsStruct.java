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
 * JNA class for the DbOptions type
 * 
 * @author Karsten Lehmann
 */
public class NotesDbOptionsStruct extends BaseStructure implements Serializable, IAdaptable {
	private static final long serialVersionUID = -496925509459204819L;
	public int options1;
	public int options2;
	public int options3;
	public int options4;
	
	public static NotesDbOptionsStruct newInstance() {
		return AccessController.doPrivileged((PrivilegedAction<NotesDbOptionsStruct>) () -> new NotesDbOptionsStruct());
	}

	public static NotesDbOptionsStruct.ByValue newInstanceByVal() {
		return AccessController.doPrivileged((PrivilegedAction<ByValue>) () -> new NotesDbOptionsStruct.ByValue());
	}
	
	public static NotesDbOptionsStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged((PrivilegedAction<NotesDbOptionsStruct>) () -> {
			NotesDbOptionsStruct newObj = new NotesDbOptionsStruct(peer);
			newObj.read();
			return newObj;
		});
	}

	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	@Deprecated
	public NotesDbOptionsStruct() {
		super();
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("options1", "options2", "options3", "options4"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	@Deprecated
	public NotesDbOptionsStruct(Pointer peer) {
		super(peer);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesDbOptionsStruct.class) {
			return (T) this;
		}
		else if (clazz == Pointer.class) {
			return (T) getPointer();
		}
		return null;
	}
	
	public static class ByReference extends NotesDbOptionsStruct implements Structure.ByReference {
		private static final long serialVersionUID = -2958581285484373942L;
		
	};
	public static class ByValue extends NotesDbOptionsStruct implements Structure.ByValue {
		private static final long serialVersionUID = -6538673668884547829L;
		
	};
	
}
