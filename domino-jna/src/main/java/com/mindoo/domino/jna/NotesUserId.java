package com.mindoo.domino.jna;

/**
 * Container for an in-memory user ID fetched from the ID vault
 * 
 * @author Karsten Lehmann
 */
public class NotesUserId  {
	private long m_memHandle;
	
	/**
	 * Creates a new instance
	 * 
	 * @param hKFC id handle
	 */
	public NotesUserId(long hKFC) {
		m_memHandle = hKFC;
	}

	/**
	 * Returns the handle to the in-memory ID
	 * 
	 * @return handle
	 */
	public long getKFCHandle() {
		return m_memHandle;
	}
}
