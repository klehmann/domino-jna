package com.mindoo.domino.jna.internal.handles;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public interface DHANDLE extends IAdaptable, IHANDLEBase<DHANDLE,DHANDLE.ByValue> {

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
	static DHANDLE newInstance(Pointer peer) {
		return AccessController.doPrivileged((PrivilegedAction<DHANDLE>) () -> {
			if (PlatformUtils.is64Bit()) {
				return new DHANDLE64(peer);
				
			}
			else {
				return new DHANDLE32(peer);
				
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	static DHANDLE newInstance() {
		return AccessController.doPrivileged((PrivilegedAction<DHANDLE>) () -> {
			if (PlatformUtils.is64Bit()) {
				return new DHANDLE64();
				
			}
			else {
				return new DHANDLE32();
				
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	static DHANDLE.ByReference newInstanceByReference(Pointer peer) {
		return AccessController.doPrivileged((PrivilegedAction<ByReference>) () -> {
			if (PlatformUtils.is64Bit()) {
				return new DHANDLE64.ByReference(peer);
				
			}
			else {
				return new DHANDLE32.ByReference(peer);
				
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	static DHANDLE.ByReference newInstanceByReference() {
		return AccessController.doPrivileged((PrivilegedAction<ByReference>) () -> {
			if (PlatformUtils.is64Bit()) {
				return new DHANDLE64.ByReference();
				
			}
			else {
				return new DHANDLE32.ByReference();
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	static ByValue newInstanceByValue() {
		return AccessController.doPrivileged((PrivilegedAction<ByValue>) () -> {
			if (PlatformUtils.is64Bit()) {
				return new DHANDLE64.ByValue();
				
			}
			else {
				return new DHANDLE32.ByValue();
				
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	static ByValue newInstanceByValue(DHANDLE copyHandleValueFrom) {
		return AccessController.doPrivileged((PrivilegedAction<ByValue>) () -> {
			if (PlatformUtils.is64Bit()) {
				DHANDLE64.ByValue newHdl1 = new DHANDLE64.ByValue();
				newHdl1.hdl = ((DHANDLE64)copyHandleValueFrom).hdl;
				return newHdl1;
			}
			else {
				DHANDLE32.ByValue newHdl2 = new DHANDLE32.ByValue();
				newHdl2.hdl = ((DHANDLE32)copyHandleValueFrom).hdl;
				return newHdl2;
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	static ByValue newInstanceByValue(Pointer peer) {
		return AccessController.doPrivileged((PrivilegedAction<ByValue>) () -> {
			if (PlatformUtils.is64Bit()) {
				return new DHANDLE64.ByValue(peer);
				
			}
			else {
				return new DHANDLE32.ByValue(peer);
				
			}
		});
	}
	
	public interface ByReference extends DHANDLE, Structure.ByReference {
		
		public ByValue getByValue();
		
	}

	public interface ByValue extends DHANDLE, Structure.ByValue {
		
	}

	@Override boolean isNull();
	
}
