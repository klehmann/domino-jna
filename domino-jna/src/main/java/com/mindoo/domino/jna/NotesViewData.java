package com.mindoo.domino.jna;

import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.structs.NotesCollectionStats;

public class NotesViewData {
	private NotesCollectionStats m_stats;
	private List<NotesViewEntryData> m_entries;
	private int m_numEntriesReturned;
	private int m_numEntriesSkipped;
	private short m_signalFlags;
	
	public NotesViewData(NotesCollectionStats stats, List<NotesViewEntryData> entries, int numEntriesSkipped, int numEntriesReturned, short signalFlags) {
		m_stats = stats;
		m_entries = entries;
		m_numEntriesSkipped = numEntriesSkipped;
		m_numEntriesReturned = numEntriesReturned;
		m_signalFlags = signalFlags;
	}

	public void reverseEntries() {
		if (m_entries!=null)
			Collections.reverse(m_entries);
	}
	
	/**
	 * Returns view statistics, if they have been requested via the read mask {@link NotesCAPI#READ_MASK_COLLECTIONSTATS}
	 * 
	 * @return statistics or null
	 */
	public NotesCollectionStats getStats() {
		return m_stats;
	}

	/**
	 * Returns the number of view entries skipped
	 * 
	 * @return skip count
	 */
	public int getSkipCount() {
		return m_numEntriesSkipped;
	}
	
	/**
	 * Returns the number of view entries read
	 * 
	 * @return return count
	 */
	public int getReturnCount() {
		return m_numEntriesReturned;
	}
	
	/**
	 * Returns the view entry data
	 * 
	 * @return list of view entry data
	 */
	public List<NotesViewEntryData> getEntries() {
		return m_entries;
	}

	/**
	 * End of collection has not been reached because the return buffer is too full.
	 * The NIFReadEntries call should be repeated to continue reading the desired entries.
	 * 
	 * @return true if more to do
	 */
	public boolean hasMoreToDo() {
		return (m_signalFlags & NotesCAPI.SIGNAL_MORE_TO_DO) == NotesCAPI.SIGNAL_MORE_TO_DO;
	}
	
	/**
	 * Collection is not up to date.
	 *  
	 * @return true if database was modified
	 */
	public boolean isDatabaseModified() {
		return (m_signalFlags & NotesCAPI.SIGNAL_DATABASE_MODIFIED) == NotesCAPI.SIGNAL_DATABASE_MODIFIED;
	}
	
	/**
	 * At least one of the "definition" view items (Selection formula or sorting rules) has been
	 * modified by another user since the last NIFReadEntries. Upon receipt, you may wish to
	 * re-read the view note if up-to-date copies of these items are needed. You also may wish
	 * to re-synchronize your index position and re-read the rebuilt index.
	 * This signal is returned only ONCE per detection.
	 * 
	 * @return true if modified
	 */
	public boolean isViewDesignModified() {
		return (m_signalFlags & NotesCAPI.SIGNAL_DEFN_ITEM_MODIFIED) == NotesCAPI.SIGNAL_DEFN_ITEM_MODIFIED;
	}

	/**
	 * The collection index has been modified by another user since the last NIFReadEntries.
	 * Upon receipt, you may wish to re-synchronize your index position and re-read the modified index.
	 * This signal is returned only ONCE per detection.
	 * 
	 * @return true if modified
	 */
	public boolean isViewIndexModified() {
		return (m_signalFlags & NotesCAPI.SIGNAL_INDEX_MODIFIED) == NotesCAPI.SIGNAL_INDEX_MODIFIED;
	}

	/**
	 * Use this method to tell whether the collection contains a time-relative formula (e.g., @Now) and
	 * will EVER be up-to-date since time-relative views, by definition, are NEVER up-to-date.
	 * 
	 * @return true if time relative
	 */
	public boolean isTimeRelative() {
		return (m_signalFlags & NotesCAPI.SIGNAL_VIEW_TIME_RELATIVE) == NotesCAPI.SIGNAL_VIEW_TIME_RELATIVE;
	}
}