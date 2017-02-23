package com.mindoo.domino.jna;

import java.util.EnumSet;
import java.util.List;

import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.utils.StringUtil;

/**
 * Utility method to easily scan a {@link NotesCollection} and read its data
 * 
 * @deprecated should by replaced by {@link NotesCollection#getAllEntries(String, int, EnumSet, int, EnumSet, com.mindoo.domino.jna.NotesCollection.ViewLookupCallback)}, because this implementation does not handle view index changes
 * 
 * @author Karsten Lehmann
 */
public abstract class NotesCollectionReader {
	/** Available actions during collection scan */
	public static enum Action {Stop, Continue};
	
	private NotesCollection m_col;
	private NotesCollectionPosition m_pos;
	private String m_posStr;
	private int m_skipCount;
	
	private EnumSet<Navigate> m_skipNav;
	private EnumSet<Navigate> m_returnNav;
	private int m_bufferSize;
	private EnumSet<ReadMask> m_returnMask;
	private boolean m_descending;
	
	/**
	 * Creates a new instance
	 * 
	 * @param col collection to scan
	 * @param startPos start position or null to start in the first row
	 * @param skipCount number of entries to skip
	 * @param skipNavigator navigator type that defines which collection entries should be skipped
	 * @param returnNavigator navigator type that defines which collection entries should be read
	 * @param bufferSize number of entries to read in one API call (used to improve performance when reading a lot of data)
	 * @param returnMask bitmask of view data to be returned
	 */
	public NotesCollectionReader(NotesCollection col, String startPos, int skipCount, EnumSet<Navigate> skipNavigator,
			EnumSet<Navigate> returnNavigator, int bufferSize, EnumSet<ReadMask> returnMask) {
		this(col, startPos, skipCount, skipNavigator, returnNavigator, bufferSize, returnMask,  null);
	}
	
	/**
	 * Creates a new instance
	 * 
	 * @param col collection to scan
	 * @param startPos start position or null to start in the first row
	 * @param skipCount number of entries to skip
	 * @param skipNavigator navigator type that defines which collection entries should be skipped
	 * @param returnNavigator navigator type that defines which collection entries should be read
	 * @param bufferSize number of entries to read in one API call (used to improve performance when reading a lot of data)
	 * @param returnMask bitmask of view data to be returned
	 * @param decodeColumns optional array to only decode specific view columns
	 */
	public NotesCollectionReader(NotesCollection col, String startPos, int skipCount, EnumSet<Navigate> skipNavigator,
			EnumSet<Navigate> returnNavigator, int bufferSize, EnumSet<ReadMask> returnMask, boolean[] decodeColumns) {
		m_col = col;
		m_pos = StringUtil.isEmpty(startPos) ? new NotesCollectionPosition("0") : new NotesCollectionPosition(startPos);
		m_posStr = startPos;
		m_skipCount = skipCount;
		m_skipNav = skipNavigator;
		m_returnNav = returnNavigator;
		m_bufferSize = bufferSize;
		m_returnMask = returnMask;
		m_descending = NotesCollection.isDescendingNav(m_returnNav);
	}
	
	/**
	 * Starts the scan process
	 */
	public void scan() {
		boolean hasMoreToDo = true;
		boolean firstRun = true;
		
		boolean startsWithLast = false;
		boolean startsWithFirst = false;
		boolean endsWithLast = false;
		boolean endsWithFirst = false;
		
		while (hasMoreToDo) {
			//TODO check for SIGNAL_ANY_CONFLICT and update the collection
			boolean hasFirst = false;
			boolean hasLast = false;

			NotesViewLookupResultData viewData;
			if (StringUtil.isEmpty(m_posStr) || "0".equals(m_posStr) || "first".equals(m_posStr)) {
				if (firstRun) {
					hasFirst=true;
					//skip 1 entry and start reading from the first relevant entry
					m_pos = new NotesCollectionPosition("0");
					viewData = m_col.readEntries(m_pos, m_skipNav, 1 + m_skipCount, m_returnNav, m_bufferSize, m_returnMask);
				}
				else {
					viewData = m_col.readEntries(m_pos, EnumSet.of(Navigate.CURRENT), 0, m_returnNav, m_bufferSize, m_returnMask);
				}
			}
			else if ("last".equals(m_posStr)) {
				//start reading from the end of the view
				if (firstRun) {
					//move all the way to the end of the view and start reading from there
					m_pos = new NotesCollectionPosition("0");
					hasLast=true;
					
					viewData = m_col.readEntries(m_pos, EnumSet.of(Navigate.NEXT, Navigate.CONTINUE), Integer.MAX_VALUE, m_returnNav, m_bufferSize, m_returnMask);
				}
				else {
					viewData = m_col.readEntries(m_pos, EnumSet.of(Navigate.CURRENT), 0, m_returnNav, m_bufferSize, m_returnMask);
				}
			}
			else {
				//start reading from the specified position
				if (firstRun) {
					if (m_descending) {
						//read the last view entry position to see where start here
						m_pos = new NotesCollectionPosition("0");
						
						viewData = m_col.readEntries(m_pos, EnumSet.of(Navigate.NEXT, Navigate.CONTINUE), Integer.MAX_VALUE, m_returnNav, 1, EnumSet.of(ReadMask.INDEXPOSITION));
						List<NotesViewEntryData> entries = viewData.getEntries();
						if (entries.size()>0) {
							NotesViewEntryData lastEntryData = entries.get(0);
							String comparePos = lastEntryData.getPositionStr();
							hasLast = comparePos.equals(m_posStr);
						}
					}
					else {
						//read the first view entry position to see if we start here
						m_pos = new NotesCollectionPosition("0");
						
						viewData = m_col.readEntries(m_pos, m_skipNav, 1, m_returnNav, 1, EnumSet.of(ReadMask.INDEXPOSITION));
						List<NotesViewEntryData> entries = viewData.getEntries();
						if (entries.size()>0) {
							NotesViewEntryData lastEntryData = entries.get(0);
							String comparePos = lastEntryData.getPositionStr();
							hasFirst = comparePos.equals(m_posStr);
						}
					}
					
					m_pos = new NotesCollectionPosition(m_posStr);
				}
				viewData = m_col.readEntries(m_pos, EnumSet.of(Navigate.CURRENT), 0, m_returnNav, m_bufferSize, m_returnMask);
			}
			
//			if (m_descending) {
//				viewData.reverseEntries();
//			}
			
			if (viewData.getReturnCount() < m_bufferSize) {
				if (m_descending) {
					hasFirst=true;
				}
				else {
					hasLast=true;
				}
			}
			
			if (m_descending) {
				if (hasLast) {
					startsWithLast=true;
				}
				if (hasFirst) {
					endsWithFirst=true;
				}
			}
			else {
				if (hasLast) {
					endsWithLast=true;
				}
				if (hasFirst) {
					startsWithFirst=true;
				}
			}
			
			Action action = viewDataRead(viewData, startsWithLast, startsWithFirst, endsWithLast, endsWithFirst);
			if (action == Action.Stop) {
				break;
			}
			
			firstRun = false;
			hasMoreToDo = viewData.hasMoreToDo();
		}
		
		done();
	}
	
	/**
	 * Internal method that is called with a list of read view entries
	 * 
	 * @param viewData view data
	 * @param startsWithLast true if the data starts with the last view entry
	 * @param startsWithFirst true if the data starts with the first view entry
	 * @param endsWithLast true if the data ends with the last view entry
	 * @param endsWithFirst true if the data ends with the first view entry
	 * @return action, either {@link Action#Continue} or {@link Action#Stop}
	 */
	private Action viewDataRead(NotesViewLookupResultData viewData, boolean startsWithLast, boolean startsWithFirst, boolean endsWithLast, boolean endsWithFirst) {
		List<NotesViewEntryData> entries = viewData.getEntries();
		for (int i=0; i<entries.size(); i++) {
			NotesViewEntryData currEntry = entries.get(i);
			boolean isFirst = (startsWithFirst && i==0) || (endsWithFirst && i==(entries.size()-1));
			boolean isLast = (startsWithLast && i==0) || (endsWithLast && i==(entries.size()-1));
			
			Action action = entryRead(currEntry, isFirst, isLast);
			if (action == Action.Stop)
				return action;
		}
		return Action.Continue;
	}

	/**
	 * Method is called when the scan is done
	 */
	protected void done() {
	}
	
	/**
	 * Implement this method to receive each view row
	 * 
	 * @param entryData row data
	 * @param isFirst true if row is first in the view
	 * @param isLast true if row is last in the view
	 * @return action, either {@link Action#Continue} or {@link Action#Stop}
	 */
	protected abstract Action entryRead(NotesViewEntryData entryData, boolean isFirst, boolean isLast);
}
