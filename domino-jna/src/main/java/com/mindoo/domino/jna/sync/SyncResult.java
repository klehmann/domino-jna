package com.mindoo.domino.jna.sync;

import com.mindoo.domino.jna.NotesTimeDate;

/**
 * Sync result class providing the amount of notes matching/not matching the
 * selection formula and the amount of deleted notes since the last sync run.
 * 
 * @author Karsten Lehmann
 */
public class SyncResult {
	private int m_syncDurationMS;
	
	private NotesTimeDate m_prevSyncStart;
	private NotesTimeDate m_nextSyncStart;
	
	private boolean m_replicaIdChanged;
	private boolean m_selectionFormulaChanged;
	
	private int m_addedToTarget;
	private int m_updatedInTarget;
	private int m_removedFromTarget;
	
	private int m_notesMatchingFormula;
	private int m_notesNotMatchingFormula;
	private int m_notesDeleted;
	
	public SyncResult(int syncDurationMS,
			boolean replicaIdChanged, boolean selectionFormulaChanged,
			NotesTimeDate prevSyncStart, NotesTimeDate nextSyncStart,
			int addedToTarget, int updatedInTarget, int removedFromTarget,
			int notesMatchingFormula, int notesNotMatchingFormula, int notesDeleted) {
		
		m_syncDurationMS = syncDurationMS;
		
		m_replicaIdChanged = replicaIdChanged;
		m_selectionFormulaChanged = selectionFormulaChanged;
		
		m_prevSyncStart = prevSyncStart;
		m_nextSyncStart = nextSyncStart;
		
		m_addedToTarget = addedToTarget;
		m_updatedInTarget = updatedInTarget;
		m_removedFromTarget = removedFromTarget;
		
		m_notesMatchingFormula = notesMatchingFormula;
		m_notesNotMatchingFormula = notesNotMatchingFormula;
		m_notesDeleted = notesDeleted;
	}
	
	public int getSyncDurationInMs() {
		return m_syncDurationMS;
	}
	
	public boolean isReplicaIdChanged() {
		return m_replicaIdChanged;
	}
	
	public boolean isSelectionFormulaChanged() {
		return m_selectionFormulaChanged;
	}
	
	public NotesTimeDate getPrevSince() {
		return m_prevSyncStart;
	}
	
	public NotesTimeDate getNextSince() {
		return m_nextSyncStart;
	}
	
	public int getAddedToTarget() {
		return m_addedToTarget;
	}
	
	public int getUpdatedInTarget() {
		return m_updatedInTarget;
	}
	
	public int getRemovedFromTarget() {
		return m_removedFromTarget;
	}
	
	public int getNoteCountMatchingFormula() {
		return m_notesMatchingFormula;
	}
	
	public int getNoteCountNotMatchingFormula() {
		return m_notesNotMatchingFormula;
	}
	
	public int getNoteCountDeleted() {
		return m_notesDeleted;
	}
	
	public String toString() {
		return "SyncResult [addedtotarget="+m_addedToTarget+", updatedintarget="+m_updatedInTarget+", removedfromtarget="+m_removedFromTarget+
				", prevsyncstart="+m_prevSyncStart+", nextsyncstart="+m_nextSyncStart+
				", durationinms="+m_syncDurationMS+
				", replicaidchanged="+m_replicaIdChanged+", selectionchanged="+m_selectionFormulaChanged+
				", matchessincelastsync="+m_notesMatchingFormula+", non-matchessincelastsync="+m_notesNotMatchingFormula+
				", deletionssincelastsync="+m_notesDeleted+"]";
	};
	
}
