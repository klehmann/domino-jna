package com.mindoo.domino.jna.indexing.cqengine;

import com.mindoo.domino.jna.NotesTimeDate;

/**
 * Base class for objects that we store in CQEngine. Provides access to the UNID / seq / sequence time
 * information which we need to incremental indexing.
 * 
 * @author Karsten Lehmann
 */
public class BaseIndexObject {
	private String m_unid;
	private int m_sequence;
	private NotesTimeDate m_sequenceTime;

	public BaseIndexObject(String unid, int sequence, NotesTimeDate sequenceTime) {
		m_unid = unid;
		m_sequence = sequence;
		m_sequenceTime = sequenceTime;
	}
	
	public String getUNID() {
		return m_unid;
	}
	
	public int getSequence() {
		return m_sequence;
	}
	
	public NotesTimeDate getSequenceTime() {
		return m_sequenceTime;
	}

}
