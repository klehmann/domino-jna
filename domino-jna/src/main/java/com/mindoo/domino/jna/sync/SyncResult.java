package com.mindoo.domino.jna.sync;

/**
 * Sync result class providing the amount of notes matching/not matching the
 * selection formula and the amount of deleted notes since the last sync run.
 * 
 * @author Karsten Lehmann
 */
public class SyncResult {
	private int m_notesMatchingFormula;
	private int m_notesNotMatchingFormula;
	private int m_notesDeleted;
	
	public SyncResult(int notesMatchingFormula, int notesNotMatchingFormula, int notesDeleted) {
		m_notesMatchingFormula = notesMatchingFormula;
		m_notesNotMatchingFormula = notesNotMatchingFormula;
		m_notesDeleted = notesDeleted;
	}
	
	public int getNoteCountMatchngFormula() {
		return m_notesMatchingFormula;
	}
	
	public int getNoteCountNotMatchingFormula() {
		return m_notesNotMatchingFormula;
	}
	
	public int getNoteCountDeleted() {
		return m_notesDeleted;
	}
	
	public String toString() {
		return "SyncResult [matches="+m_notesMatchingFormula+", non-matches="+m_notesNotMatchingFormula+", deletions="+m_notesDeleted+"]";
	};
	
}
