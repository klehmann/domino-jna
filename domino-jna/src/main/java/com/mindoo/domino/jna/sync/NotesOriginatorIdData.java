package com.mindoo.domino.jna.sync;

import com.mindoo.domino.jna.NotesOriginatorId;
import com.mindoo.domino.jna.NotesTimeDate;

/**
 * Data container for the {@link NotesOriginatorId} fields used
 * for the sync. Use to speed up the sync process, avoiding unnecessary
 * JNA structures.
 * 
 * Karsten Lehmann
 */
public class NotesOriginatorIdData {
	private String m_unid;
	private int m_seq;
	private NotesTimeDate m_seqTime;
	
	public NotesOriginatorIdData(String unid, int seq, NotesTimeDate seqTime) {
		m_unid = unid;
		m_seq = seq;
		m_seqTime = seqTime;
	}
	
	public NotesOriginatorIdData(NotesOriginatorId oid) {
		m_unid = oid.getUNIDAsString();
		m_seq = oid.getSequence();
		m_seqTime = oid.getSequenceTime();
	}
	
	public String getUNID() {
		return m_unid;
	}
	
	public int getSequence() {
		return m_seq;
	}
	
	public NotesTimeDate getSequenceTime() {
		return m_seqTime;
	}
}
