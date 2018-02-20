package com.mindoo.domino.jna.indexing.cqengine;

/**
 * Base class for objects that we store in CQEngine. Provides access to the UNID / seq / sequence time
 * information which we need to incremental indexing.
 * 
 * @author Karsten Lehmann
 */
public class BaseIndexObject {
	private String m_unid;
	private int m_sequence;
	private int[] m_sequenceTimeInnards;

	public BaseIndexObject(String unid, int sequence, int[] sequenceTimeInnards) {
		m_unid = unid;
		m_sequence = sequence;
		m_sequenceTimeInnards = sequenceTimeInnards;
	}
	
	public String getUNID() {
		return m_unid;
	}
	
	public int getSequence() {
		return m_sequence;
	}
	
	public int[] getSequenceTimeInnards() {
		return m_sequenceTimeInnards;
	}
}
