package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.internal.NotesCAPI;

/**
 * These are the options used when calling {@link NotesDatabase#replicateDbsWithServer(String, EnumSet, java.util.List, int)}
 * 
 * @author Karsten Lehmann
 */
public enum ReplicateOption {
	
	/** Receive notes from server (pull) */
	RECEIVE_NOTES(NotesCAPI.REPL_OPTION_RCV_NOTES),
	
	/** Send notes to server (push) */
	SEND_NOTES(NotesCAPI.REPL_OPTION_SEND_NOTES),
	
	/** Replicate all database files */
	ALL_DBS(NotesCAPI.REPL_OPTION_ALL_DBS),
	
	/** Close sessions when done */
	CLOSE_SESSION(NotesCAPI.REPL_OPTION_CLOSE_SESS),
	
	/** Replicate NTFs as well */
	ALL_NTFS(NotesCAPI.REPL_OPTION_ALL_NTFS),
	
	/** Low, Medium, &amp; High priority databases*/
	PRIO_LOW(NotesCAPI.REPL_OPTION_PRI_LOW),
	
	/** Medium &amp; High priority databases only*/
	PRIO_MEDIUM(NotesCAPI.REPL_OPTION_PRI_MED),
	
	/** High priority databases only */
	PRIO_HIGH(NotesCAPI.REPL_OPTION_PRI_HI),
	
	/** Abstract/truncate docs to summary data and first RTF field. (~40K) */
	ABSTRACT_RTF(NotesCAPI.REPL_OPTION_ABSTRACT_RTF),
	
	/** Abstract/truncate docs to summary only data. */
	ABSTRACT_SUMMARYONLY(NotesCAPI.REPL_OPTION_ABSTRACT_SMRY),
	
	/** Replicate private documents even if not selected by default. */
	PRIVATE(NotesCAPI.REPL_OPTION_PRIVATE),
	
	ALL_FILES(NotesCAPI.REPL_OPTION_ALL_DBS | NotesCAPI.REPL_OPTION_ALL_NTFS);

	private int m_val;
	
	ReplicateOption(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static int toBitMaskInt(EnumSet<ReplicateOption> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (ReplicateOption currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
