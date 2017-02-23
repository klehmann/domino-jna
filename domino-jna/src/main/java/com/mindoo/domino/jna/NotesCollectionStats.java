package com.mindoo.domino.jna;

/**
 * If requested, this structure is returned by {@link NotesCollection#readEntries(com.mindoo.domino.jna.NotesCollectionPosition, java.util.EnumSet, int, java.util.EnumSet, int, java.util.EnumSet)}
 * at the front of the returned information buffer.<br>
 * The structure describes statistics about the overall collection.
 * 
 * @author Karsten Lehmann
 */
public class NotesCollectionStats {
	private int m_topLevelEntries;
	private int m_lastModifiedTime;
	
	public NotesCollectionStats(int topLevelEntries, int lastModifiedTime) {
		m_topLevelEntries = topLevelEntries;
		m_lastModifiedTime = lastModifiedTime;
	}
	
	/**
	 * # top level entries (level 0)
	 * 
	 * @return entries
	 */
	public int getTopLevelEntries() {
		return m_topLevelEntries;
	}
	
	/**
	 * Currently not used in the C API
	 * 
	 * @return 0
	 */
	public int getLastModifiedTime() {
		return m_lastModifiedTime;
	}
}
