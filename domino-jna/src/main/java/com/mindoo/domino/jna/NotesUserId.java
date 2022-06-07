package com.mindoo.domino.jna;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.DHANDLE32;
import com.mindoo.domino.jna.internal.handles.DHANDLE64;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;

/**
 * Container for an in-memory user ID fetched from the ID vault
 * 
 * @author Karsten Lehmann
 */
public class NotesUserId  {
	private long m_hKFC64;
	private int m_hKFC32;
	
	public NotesUserId(IAdaptable adaptable) {
		DHANDLE hdl = adaptable.getAdapter(DHANDLE.class);
		if (hdl!=null) {
			if (PlatformUtils.is64Bit() && hdl instanceof DHANDLE64) {
				m_hKFC64 = ((DHANDLE64)hdl).hdl;
				return;
			}
			else if (hdl instanceof DHANDLE32) {
				m_hKFC32 = ((DHANDLE32)hdl).hdl;
				return;
			}
		}
		throw new NotesError(0, "Unsupported adaptable parameter");
	}
	
	public String getUsername() {
		DisposableMemory retUsernameMem = new DisposableMemory(NotesConstants.MAXUSERNAME);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().SECKFMAccess((short) 32, m_hKFC64, retUsernameMem, null);
		}
		else {
			result = NotesNativeAPI32.get().SECKFMAccess((short) 32, m_hKFC32, retUsernameMem, null);
		}
		NotesErrorUtils.checkResult(result);
		
		String username = NotesStringUtils.fromLMBCS(retUsernameMem, -1);
		return username;
	}
	
	/**
	 * Writes a safe copy of the ID to disk
	 * 
	 * @param targetFilePath filepath to write the safe.id
	 */
	public void makeSafeCopy(String targetFilePath) {
		Memory targetFilePathMem = NotesStringUtils.toLMBCS(targetFilePath, true);

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().SECKFMMakeSafeCopy(m_hKFC64, NotesConstants.KFM_safecopy_Standard,
					(short) 0, targetFilePathMem);
		}
		else {
			result = NotesNativeAPI32.get().SECKFMMakeSafeCopy(m_hKFC32, NotesConstants.KFM_safecopy_Standard,
					(short) 0, targetFilePathMem);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Returns the handle to the in-memory ID for 32 bit
	 * 
	 * @return handle
	 */
	public int getHandle32() {
		return m_hKFC32;
	}
	
	/**
	 * Returns the handle to the in-memory ID for 64 bit
	 * 
	 * @return handle
	 */
	public long getHandle64() {
		return m_hKFC64;
	}

}
