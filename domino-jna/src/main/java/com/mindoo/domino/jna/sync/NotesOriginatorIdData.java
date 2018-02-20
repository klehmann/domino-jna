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
	private int[] m_seqTimeInnards;
	
	public NotesOriginatorIdData(String unid, int seq, int[] seqTimeInnards) {
		m_unid = unid;
		m_seq = seq;
		m_seqTimeInnards = seqTimeInnards;
	}
	
	public NotesOriginatorIdData(NotesOriginatorId oid) {
		m_unid = oid.getUNIDAsString();
		m_seq = oid.getSequence();
		m_seqTimeInnards = oid.getSequenceTime().getInnards();
	}
	
	public String getUNID() {
		return m_unid;
	}
	
	public int getSequence() {
		return m_seq;
	}
	
	public int[] getSequenceTimeInnards() {
		return m_seqTimeInnards;
	}
}
