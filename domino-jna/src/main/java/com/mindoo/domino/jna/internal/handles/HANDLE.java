package com.mindoo.domino.jna.internal.handles;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public interface HANDLE extends IAdaptable, IHANDLEBase<HANDLE,HANDLE.ByValue> {
	
	/**
	 * Returns whether the handle is disposed
	 * 
	 * @return true if disposed
	 */
	boolean isDisposed();
	
	/**
	 * Marks the handle as disposed
	 */
	void setDisposed();

	/**
	 * Fill handle with a null value
	 */
	void clear();

	/**
	 * Throws a {@link DominoException} if the handle is marked as disposed
	 */
	@Override
	default void checkDisposed() {
		if (isDisposed()) {
			throw new NotesError("Handle is already disposed");
		}
	}

	@SuppressWarnings("deprecation")
	static HANDLE newInstance(Pointer peer) {
		return AccessController.doPrivileged((PrivilegedAction<HANDLE>) () -> {
			if (PlatformUtils.is64Bit()) {
				return new HANDLE64(peer);
				
			}
			else {
				return new HANDLE32(peer);
				
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	static HANDLE newInstance() {
		return AccessController.doPrivileged((PrivilegedAction<HANDLE>) () -> {
			if (PlatformUtils.is64Bit()) {
				return new HANDLE64();
				
			}
			else {
				return new HANDLE32();
				
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	static HANDLE.ByReference newInstanceByReference(Pointer peer) {
		return AccessController.doPrivileged((PrivilegedAction<ByReference>) () -> {
			if (PlatformUtils.is64Bit()) {
				return new HANDLE64.ByReference(peer);
				
			}
			else {
				return new HANDLE32.ByReference(peer);
				
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	static HANDLE.ByReference newInstanceByReference() {
		return AccessController.doPrivileged((PrivilegedAction<ByReference>) () -> {
			if (PlatformUtils.is64Bit()) {
				return new HANDLE64.ByReference();
				
			}
			else {
				return new HANDLE32.ByReference();
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	static HANDLE.ByReference newInstanceByReference(HANDLE hdlToCopy) {
		return AccessController.doPrivileged((PrivilegedAction<ByReference>) () -> {
			if (PlatformUtils.is64Bit()) {
				HANDLE64.ByReference newHdl = new HANDLE64.ByReference();
				newHdl.hdl = ((HANDLE64)hdlToCopy).hdl;
				newHdl.write();
				return newHdl;
			}
			else {
				HANDLE32.ByReference newHdl = new HANDLE32.ByReference();
				newHdl.hdl = ((HANDLE32)hdlToCopy).hdl;
				newHdl.write();
				return newHdl;
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	static ByValue newInstanceByValue() {
		return AccessController.doPrivileged((PrivilegedAction<ByValue>) () -> {
			if (PlatformUtils.is64Bit()) {
				return new HANDLE64.ByValue();
				
			}
			else {
				return new HANDLE32.ByValue();
				
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	static ByValue newInstanceByValue(Pointer peer) {
		return AccessController.doPrivileged((PrivilegedAction<ByValue>) () -> {
			if (PlatformUtils.is64Bit()) {
				return new HANDLE64.ByValue(peer);
				
			}
			else {
				return new HANDLE32.ByValue(peer);
				
			}
		});
	}
	
	public interface ByReference extends HANDLE, Structure.ByReference {
	
		public ByValue getByValue();

	}

	public interface ByValue extends HANDLE, Structure.ByValue {
		
	}

	@Override boolean isNull();
	
}
