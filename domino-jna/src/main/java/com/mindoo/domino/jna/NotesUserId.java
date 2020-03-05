package com.mindoo.domino.jna;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.Handle;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;

/**
 * Container for an in-memory user ID fetched from the ID vault
 * 
 * @author Karsten Lehmann
 */
public class NotesUserId  {
	private long m_memHandle64;
	private int m_memHandle32;
	
	public NotesUserId(IAdaptable adaptable) {
		Handle hdl = adaptable.getAdapter(Handle.class);
		if (hdl!=null) {
			if (PlatformUtils.is64Bit()) {
				m_memHandle64 = hdl.getHandle64();
			}
			else {
				m_memHandle32 = hdl.getHandle32();
			}
			return;
		}
		throw new NotesError(0, "Unsupported adaptable parameter");
	}
	
	public String getUsername() {
		DisposableMemory retUsernameMem = new DisposableMemory(NotesConstants.MAXUSERNAME);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().SECKFMAccess((short) 32, m_memHandle64, retUsernameMem, null);
		}
		else {
			result = NotesNativeAPI32.get().SECKFMAccess((short) 32, m_memHandle32, retUsernameMem, null);
		}
		NotesErrorUtils.checkResult(result);
		
		String username = NotesStringUtils.fromLMBCS(retUsernameMem, -1);
		return username;
	}
	
	/**
	 * Returns the handle to the in-memory ID for 32 bit
	 * 
	 * @return handle
	 */
	public int getHandle32() {
		return m_memHandle32;
	}
	
	/**
	 * Returns the handle to the in-memory ID for 64 bit
	 * 
	 * @return handle
	 */
	public long getHandle64() {
		return m_memHandle64;
	}

}
