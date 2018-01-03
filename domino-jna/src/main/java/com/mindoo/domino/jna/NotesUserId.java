package com.mindoo.domino.jna;

import com.mindoo.domino.jna.utils.PlatformUtils;

/**
 * Container for an in-memory user ID fetched from the ID vault
 * 
 * @author Karsten Lehmann
 */
public class NotesUserId  {
	private long m_memHandle64;
	private int m_memHandle32;
	
	/**
	 * Creates a new instance
	 * 
	 * @param hKFC id handle
	 */
	public NotesUserId(long hKFC) {
		if (!PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_memHandle64 = hKFC;
	}

	/**
	 * Creates a new instance
	 * 
	 * @param hKFC id handle
	 */
	public NotesUserId(int hKFC) {
		if (PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		m_memHandle32 = hKFC;
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
