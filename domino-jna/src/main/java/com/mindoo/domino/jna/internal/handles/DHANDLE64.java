package com.mindoo.domino.jna.internal.handles;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.internal.structs.BaseStructure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * DHANDLE on 64 bit systems
 * 
 * @author Karsten Lehmann
 */
public class DHANDLE64 extends BaseStructure implements DHANDLE {
	public long hdl;
	private boolean disposed;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	@Deprecated
	public DHANDLE64() {
		super();
		//set ALIGN to NONE, because the NAMES_LIST structure is directly followed by the usernames and wildcards in memory
		setAlignType(ALIGN_DEFAULT);
	}

	public long getValue() {
		return hdl;
	}
	
	public static DHANDLE64 newInstance(long hdl) {
		return AccessController.doPrivileged((PrivilegedAction<DHANDLE64>) () -> new DHANDLE64(hdl));
	}

	@Override
	public void clear() {
		hdl = 0;
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}

	@Override
	public void setDisposed() {
		disposed = true;
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("hdl"); //$NON-NLS-1$
	}

	/**
	 * Creates a new instance
	 * 
	 * @param hdl handle value
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	@Deprecated
	public DHANDLE64(long hdl) {
		super();
		setAlignType(ALIGN_DEFAULT);
		this.hdl = hdl;
	}

	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	@Deprecated
	public DHANDLE64(Pointer peer) {
		super(peer);
		setAlignType(ALIGN_DEFAULT);
	}

	public static DHANDLE64 newInstance(final Pointer peer) {
		return AccessController.doPrivileged((PrivilegedAction<DHANDLE64>) () -> new DHANDLE64(peer));
	}

	public static class ByReference extends DHANDLE64 implements Structure.ByReference, DHANDLE.ByReference {
		/**
		 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
		 */
		@Deprecated
		public ByReference() {
			super();
		}
		
		/**
		 * @param peer memory pointer
		 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
		 */
		@Deprecated
		public ByReference(Pointer peer) {
			super(peer);
		}
		
		@Override
		public com.mindoo.domino.jna.internal.handles.DHANDLE.ByValue getByValue() {
			return AccessController.doPrivileged((PrivilegedAction<com.mindoo.domino.jna.internal.handles.DHANDLE.ByValue>) () -> {
				DHANDLE64.ByValue byVal = new DHANDLE64.ByValue();
				byVal.hdl = this.hdl;
				return byVal;
			});
		}

	};
	public static class ByValue extends DHANDLE64 implements Structure.ByValue, DHANDLE.ByValue {
		/**
		 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
		 */
		@Deprecated
		public ByValue() {
			super();
		}
		
		/**
		 * @param peer memory pointer
		 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
		 */
		@Deprecated
		public ByValue(Pointer peer) {
			super(peer);
		}
	};
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == DHANDLE.class || clazz == DHANDLE64.class) {
			return (T) this;
		}
		else if(clazz == Structure.class) {
			return (T)this;
		}
		else if(clazz == Pointer.class) {
			return (T)getPointer();
		}
		
		return null;
	}
	
	@Override
	public boolean isNull() {
		return hdl==0;
	}

	@Override
	public String toString() {
		return MessageFormat.format("DHANDLE64 [handle={0}]", hdl); //$NON-NLS-1$
	}

	@Override
	public DHANDLE.ByValue getByValue() {
		DHANDLE64.ByValue newHdl = new DHANDLE64.ByValue();
		newHdl.hdl = this.hdl;
		return newHdl;
	}

}
