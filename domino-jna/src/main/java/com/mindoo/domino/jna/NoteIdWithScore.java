package com.mindoo.domino.jna;

/**
 * Container for one FT search result entry, containing a note id and the search score (or 0 if
 * no scores have been collected).
 * 
 * @author Karsten Lehmann
 */
public class NoteIdWithScore {
	private int m_noteId;
	private int m_score;
	
	public NoteIdWithScore(int noteId, int score) {
		m_noteId = noteId;
		m_score = score;
	}
	
	public int getNoteId() {
		return m_noteId;
	}
	
	public int getScore() {
		return m_score;
	}

	@Override
	public String toString() {
		return "NoteIdWithScore [m_noteId=" + m_noteId + ", m_score=" + m_score + "]";
	}
	
}
