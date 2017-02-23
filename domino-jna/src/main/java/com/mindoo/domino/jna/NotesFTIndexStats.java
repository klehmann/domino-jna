package com.mindoo.domino.jna;

/**
 * This class contains statistics information that is returned when indexing a database for full
 * text searching capabilities with {@link NotesDatabase#FTIndex(java.util.EnumSet)}
 * 
 * @author Karsten Lehmann
 */
public class NotesFTIndexStats {
	private int m_docsAdded;
	private int m_docsUpdated;
	private int m_docsDeleted;
	private int m_bytesIndexed;
	
	public NotesFTIndexStats(int docsAdded, int docsUpdated, int docsDeleted, int bytesIndexed) {
		m_docsAdded = docsAdded;
		m_docsUpdated = docsUpdated;
		m_docsDeleted = docsDeleted;
		m_bytesIndexed = bytesIndexed;
	}
	
	/**
	 * # of new documents
	 * 
	 * @return count
	 */
	public int getDocsAdded() {
		return m_docsAdded;
	}
	
	/**
	 * # of revised documents
	 * 
	 * @return count
	 */
	public int getDocsUpdated() {
		return m_docsUpdated;
	}
	
	/**
	 * # of deleted documents
	 * 
	 * @return count
	 */
	public int getDocsDeleted() {
		return m_docsDeleted;
	}
	
	/**
	 * # of bytes indexed
	 * 
	 * @return bytes
	 */
	public int getBytesIndexed() {
		return m_bytesIndexed;
	}
	
}
