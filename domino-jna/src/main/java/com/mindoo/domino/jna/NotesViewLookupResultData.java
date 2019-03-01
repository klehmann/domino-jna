package com.mindoo.domino.jna;

import java.util.List;

import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Container class for a lookup result in a collection/view
 * 
 * @author Karsten Lehmann
 */
public class NotesViewLookupResultData {
	private NotesCollectionStats m_stats;
	private List<NotesViewEntryData> m_entries;
	private int m_numEntriesReturned;
	private int m_numEntriesSkipped;
	private short m_signalFlags;
	private String m_pos;
	private int m_indexModifiedSequenceNo;
	private NotesTimeDate m_retDiffTime;
	
	/**
	 * Creates a new instance
	 * 
	 * @param stats collection statistics
	 * @param entries entries read from the buffer
	 * @param numEntriesSkipped number of skipped entries
	 * @param numEntriesReturned number of returned entries
	 * @param signalFlags signal flags indicating view index changes and other stuff
	 * @param pos first matching position
	 * @param indexModifiedSequenceNo index modified sequence number
	 * @param retDiffTime only set in {@link NotesCollection#readEntriesExt(NotesCollectionPosition, java.util.EnumSet, int, java.util.EnumSet, int, java.util.EnumSet, NotesTimeDate, NotesIDTable, Integer)}
	 */
	public NotesViewLookupResultData(NotesCollectionStats stats, List<NotesViewEntryData> entries, int numEntriesSkipped, int numEntriesReturned, short signalFlags, String pos, int indexModifiedSequenceNo, NotesTimeDate retDiffTime) {
		m_stats = stats;
		m_entries = entries;
		m_numEntriesSkipped = numEntriesSkipped;
		m_numEntriesReturned = numEntriesReturned;
		m_signalFlags = signalFlags;
		m_pos = pos;
		m_indexModifiedSequenceNo = indexModifiedSequenceNo;
		m_retDiffTime = retDiffTime;
	}

	/**
	 * For differential view reading via {@link NotesCollection#readEntriesExt(NotesCollectionPosition, java.util.EnumSet, int, java.util.EnumSet, int, java.util.EnumSet, NotesTimeDate, NotesIDTable, Integer)},
	 * this method returns the returned diff time that can be passed in subsequent read calls to
	 * get incremental view updates
	 * 
	 * @return diff time or null
	 */
	public NotesTimeDate getReturnedDiffTime() {
		return m_retDiffTime;
	}
	
	/**
	 * Returns the index modified sequence number, which is increased on every index change.<br>
	 * 
	 * @return number
	 */
	public int getIndexModifiedSequenceNo() {
		return m_indexModifiedSequenceNo;
	}
	
	/**
	 * If multiple index entries match the specified lookup key (especially if<br>
	 * not enough key items were specified), then the index position of<br>
	 * the FIRST matching entry is returned ("first" is defined by the<br>
	 * entry which collates before all others in the collated index).<br>
	 * 
	 * Will only be set when {@link NotesCollection#findByKeyExtended2(java.util.EnumSet, java.util.EnumSet, Object...)}
	 * is called.
	 * 
	 * @return position or null
	 */
	public String getPosition() {
		return m_pos;
	}
	
	/**
	 * Returns view statistics, if they have been requested via the
	 * read mask {@link ReadMask#COLLECTIONSTATS}
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
		return (m_signalFlags & NotesConstants.SIGNAL_MORE_TO_DO) == NotesConstants.SIGNAL_MORE_TO_DO;
	}
	
	/**
	 * Collection is not up to date.
	 *  
	 * @return true if database was modified
	 */
	public boolean isDatabaseModified() {
		return (m_signalFlags & NotesConstants.SIGNAL_DATABASE_MODIFIED) == NotesConstants.SIGNAL_DATABASE_MODIFIED;
	}
	
	/**
	 * At least one of the "definition" view items (Selection formula or sorting rules) has been
	 * modified by another user since the last NIFReadEntries. Upon receipt, you may wish to
	 * re-read the view note if up-to-date copies of these items are needed. You also may wish
	 * to re-synchronize your index position and re-read the rebuilt index.<br>
	 * <br>
	 * This signal is returned only ONCE per detection.
	 * 
	 * @return true if modified
	 */
	public boolean isViewDefiningItemModified() {
		return (m_signalFlags & NotesConstants.SIGNAL_DEFN_ITEM_MODIFIED) == NotesConstants.SIGNAL_DEFN_ITEM_MODIFIED;
	}

	/**
	 * At least one of the non-"definition" view items ($TITLE,etc) has been
	 * modified since last ReadEntries.
	 * Upon receipt, you may wish to re-read the view note if up-to-date copies of these
	 * items are needed.<br>
	 * <br>
	 * Signal returned only ONCE per detection
	 * 
	 * @return true if modified
	 */
	public boolean isViewOtherItemModified() {
		return (m_signalFlags & NotesConstants.SIGNAL_VIEW_ITEM_MODIFIED) == NotesConstants.SIGNAL_VIEW_ITEM_MODIFIED;
	}
	
	/**
	 * The collection index has been modified by another user since the last NIFReadEntries.
	 * Upon receipt, you may wish to re-synchronize your index position and re-read the modified index.
	 * This signal is returned only ONCE per detection.
	 * 
	 * @return true if modified
	 */
	public boolean isViewIndexModified() {
		return (m_signalFlags & NotesConstants.SIGNAL_INDEX_MODIFIED) == NotesConstants.SIGNAL_INDEX_MODIFIED;
	}

	/**
	 * Use this method to tell whether the collection contains a time-relative formula (e.g., @ Now) and
	 * will EVER be up-to-date since time-relative views, by definition, are NEVER up-to-date.
	 * 
	 * @return true if time relative
	 */
	public boolean isViewTimeRelative() {
		return (m_signalFlags & NotesConstants.SIGNAL_VIEW_TIME_RELATIVE) == NotesConstants.SIGNAL_VIEW_TIME_RELATIVE;
	}
	
	/**
	 * Returns whether the view contains documents with reader fields
	 * 
	 * @return true if reader fields
	 */
	public boolean hasDocsWithReaderFields() {
		return (m_signalFlags & NotesConstants.SIGNAL_VIEW_HASPRIVS) == NotesConstants.SIGNAL_VIEW_HASPRIVS;
	}
	
	/**	
	 * Mask that defines all "sharing conflicts" except for {@link #isDatabaseModified()}.
	 * This can be used in combination with {@link #isViewTimeRelative()} to tell if
	 * the database or collection has truly changed out from under the user or if the
	 * view is a time-relative view which will NEVER be up-to-date. {@link #isDatabaseModified()}
	 * is always returned for a time-relative view to indicate that it is never up-to-date.
	 * 
	 *  @return true if we have conflicts
	 */
	public boolean hasAnyNonDataConflicts() {
		return (m_signalFlags & NotesConstants.SIGNAL_ANY_NONDATA_CONFLICT) != 0;
	}
}