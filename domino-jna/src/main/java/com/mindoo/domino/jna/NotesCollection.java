package com.mindoo.domino.jna;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.joda.time.Interval;

import com.mindoo.domino.jna.constants.FTSearch;
import com.mindoo.domino.jna.constants.Find;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.structs.NotesCollectionPosition;
import com.mindoo.domino.jna.structs.NotesTimeDate;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

import lotus.domino.NotesException;
import lotus.domino.View;
import lotus.domino.ViewColumn;

/**
 * A collection represents a list of Notes, comparable to the {@link View} object
 * 
 * @author Karsten Lehmann
 */
public class NotesCollection implements IRecyclableNotesObject {
	private int m_hDB32;
	private long m_hDB64;
	private int m_hCollection32;
	private long m_hCollection64;
	private String m_name;
	private NotesIDTable m_collapsedList;
	private NotesIDTable m_selectedList;
	private String m_viewUNID;
	private boolean m_noRecycle;
	private int m_viewNoteId;
	private IntByReference m_activeFTSearchHandle32;
	private LongByReference m_activeFTSearchHandle64;
	private NotesIDTable m_unreadTable;
	private String m_asUserCanonical;
	private NotesDatabase m_parentDb;
	
	/**
	 * Creates a new instance, 32 bit mode
	 * 
	 * @param hDB database handle
	 * @param hCollection collection handle
	 * @param name collection name
	 * @param viewNoteId view note id
	 * @param viewUNID view UNID
	 * @param collapsedList id table for the collapsed list
	 * @param selectedList id table for the selected list
	 * @param unreadTable id table for the unread list
	 * @param asUserCanonical user used to read the collection data
	 */
	public NotesCollection(NotesDatabase parentDb, int hCollection, String name, int viewNoteId, String viewUNID, NotesIDTable collapsedList, NotesIDTable selectedList, NotesIDTable unreadTable, String asUserCanonical) {
		if (NotesContext.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		m_asUserCanonical = asUserCanonical;
		m_parentDb = parentDb;
		m_hDB32 = parentDb.getHandle32();
		m_hCollection32 = hCollection;
		m_name = name;
		m_viewNoteId = viewNoteId;
		m_viewUNID = viewUNID;
		m_collapsedList = collapsedList;
		m_selectedList = selectedList;
		m_unreadTable = unreadTable;
	}

	/**
	 * Creates a new instance, 64 bit mode
	 * 
	 * @param hDB database handle
	 * @param hCollection collection handle
	 * @param name collection name
	 * @param viewNoteId view note id
	 * @param viewUNID view UNID
	 * @param collapsedList id table for the collapsed list
	 * @param selectedList id table for the selected list
	 * @param unreadTable id table for the unread list
	 * @param asUserCanonical user used to read the collection data
	 */
	public NotesCollection(NotesDatabase parentDb, long hCollection, String name, int viewNoteId, String viewUNID, NotesIDTable collapsedList, NotesIDTable selectedList, NotesIDTable unreadTable, String asUser) {
		if (!NotesContext.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_asUserCanonical = asUser;
		m_parentDb = parentDb;
		m_hDB64 = parentDb.getHandle64();
		m_hCollection64 = hCollection;
		m_name = name;
		m_viewNoteId = viewNoteId;
		m_viewUNID = viewUNID;
		m_collapsedList = collapsedList;
		m_selectedList = selectedList;
		m_unreadTable = unreadTable;
	}

	/**
	 * Returns the name of the collection
	 * 
	 * @return name
	 */
	public String getName() {
		return m_name;
	}
	
	/**
	 * Returns the parent database of this collation
	 * 
	 * @return database
	 */
	public NotesDatabase getParent() {
		return m_parentDb;
	}
	
	/**
	 * Each time the number of documents in a collection is modified, a sequence number
	 * is incremented.  This function will return the modification sequence number, which
	 * may then be compared to a previous value (also obtained by calling
	 * NIFGetLastModifiedTime()) to determine whether or not the number of documents in the
	 * collection has been changed.<br>
	 * <br>Note that the TIMEDATE value returned by this function is not an actual time.
	 * 
	 * @return time date
	 */
	public NotesTimeDate getLastModifiedTime() {
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		NotesTimeDate retLastModifiedTime = new NotesTimeDate();
		
		if (NotesContext.is64Bit()) {
			notesAPI.b64_NIFGetLastModifiedTime(m_hCollection64, retLastModifiedTime);
		}
		else {
			notesAPI.b32_NIFGetLastModifiedTime(m_hCollection32, retLastModifiedTime);
		}
		return retLastModifiedTime;
	}
	
	public NotesIDTable getIDTableForFolder(boolean validateIds) {
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		
		if (NotesContext.is64Bit()) {
			LongByReference hTable = new LongByReference();
			short result = notesAPI.b64_NSFFolderGetIDTable(m_hDB64, m_hDB64, m_viewNoteId, validateIds ? NotesCAPI.DB_GETIDTABLE_VALIDATE : 0, hTable);
			NotesErrorUtils.checkResult(result);
			return new NotesIDTable(hTable);
		}
		else {
			IntByReference hTable = new IntByReference();
			short result = notesAPI.b32_NSFFolderGetIDTable(m_hDB32, m_hDB32, m_viewNoteId, validateIds ? NotesCAPI.DB_GETIDTABLE_VALIDATE : 0, hTable);
			NotesErrorUtils.checkResult(result);
			return new NotesIDTable(hTable);
		}
	}
	
	/**
	 * Method to check whether a skip or return navigator returns view data from last to first entry
	 * 
	 * @param nav navigator mode
	 * @return true if descending
	 */
	public static boolean isDescendingNav(EnumSet<Navigate> nav) {
		boolean descending = nav.contains(Navigate.PREV) ||
				nav.contains(Navigate.PREV_CATEGORY) ||
				nav.contains(Navigate.PREV_EXP_NONCATEGORY) ||
				nav.contains(Navigate.PREV_EXPANDED) ||
				nav.contains(Navigate.PREV_EXPANDED_CATEGORY) ||
				nav.contains(Navigate.PREV_EXPANDED_SELECTED) ||
				nav.contains(Navigate.PREV_EXPANDED_UNREAD) ||
				nav.contains(Navigate.PREV_HIT) ||
				nav.contains(Navigate.PREV_MAIN) ||
				nav.contains(Navigate.PREV_NONCATEGORY) ||
				nav.contains(Navigate.PREV_PARENT) ||
				nav.contains(Navigate.PREV_PEER) ||
				nav.contains(Navigate.PREV_SELECTED) ||
				nav.contains(Navigate.PREV_SELECTED_HIT) ||
				nav.contains(Navigate.PREV_SELECTED_MAIN) ||
				nav.contains(Navigate.PREV_UNREAD) ||
				nav.contains(Navigate.PREV_UNREAD_HIT) ||
				nav.contains(Navigate.PREV_UNREAD_MAIN) ||
				nav.contains(Navigate.PARENT);
	
		return descending;
	}
	
	/**
	 * Method to reverse the traversal order, e.g. from {@link NotesCAPI#NAVIGATE_NEXT} to
	 * {@link NotesCAPI#NAVIGATE_PREV}.
	 * 
	 * @param nav nav constant
	 * @return reversed contant
	 */
	public static Navigate reverseNav(Navigate nav) {
		switch (nav) {
		case PARENT:
			return Navigate.CHILD;
		case CHILD:
			return Navigate.PARENT;
		case NEXT_PEER:
			return Navigate.PREV_PEER;
		case PREV_PEER:
			return Navigate.NEXT_PEER;
		case FIRST_PEER:
			return Navigate.LAST_PEER;
		case LAST_PEER:
			return Navigate.FIRST_PEER;
		case NEXT_MAIN:
			return Navigate.PREV_MAIN;
		case PREV_MAIN:
			return Navigate.NEXT_MAIN;
		case NEXT_PARENT:
			return Navigate.PREV_PARENT;
		case PREV_PARENT:
			return Navigate.NEXT_PARENT;
		case NEXT:
			return Navigate.PREV;
		case NEXT_UNREAD:
			return Navigate.PREV_UNREAD;
		case NEXT_UNREAD_MAIN:
			return Navigate.PREV_UNREAD_MAIN;
		case PREV_UNREAD_MAIN:
			return Navigate.NEXT_UNREAD_MAIN;
		case PREV_UNREAD:
			return Navigate.NEXT_UNREAD;
		case NEXT_SELECTED:
			return Navigate.PREV_SELECTED;
		case PREV_SELECTED:
			return Navigate.NEXT_SELECTED;
		case NEXT_SELECTED_MAIN:
			return Navigate.PREV_SELECTED_MAIN;
		case PREV_SELECTED_MAIN:
			return Navigate.NEXT_SELECTED_MAIN;
		case NEXT_EXPANDED:
			return Navigate.PREV_EXPANDED;
		case PREV_EXPANDED:
			return Navigate.NEXT_EXPANDED;
		case NEXT_EXPANDED_UNREAD:
			return Navigate.PREV_EXPANDED_UNREAD;
		case PREV_EXPANDED_UNREAD:
			return Navigate.NEXT_EXPANDED_UNREAD;
		case NEXT_EXPANDED_SELECTED:
			return Navigate.PREV_EXPANDED_SELECTED;
		case PREV_EXPANDED_SELECTED:
			return Navigate.NEXT_EXPANDED_SELECTED;
		case NEXT_EXPANDED_CATEGORY:
			return Navigate.PREV_EXPANDED_CATEGORY;
		case PREV_EXPANDED_CATEGORY:
			return Navigate.NEXT_EXPANDED_CATEGORY;
		case NEXT_EXP_NONCATEGORY:
			return Navigate.PREV_EXP_NONCATEGORY;
		case PREV_EXP_NONCATEGORY:
			return Navigate.NEXT_EXP_NONCATEGORY;
		case NEXT_HIT:
			return Navigate.PREV_HIT;
		case PREV_HIT:
			return Navigate.NEXT_HIT;
		case NEXT_SELECTED_HIT:
			return Navigate.PREV_SELECTED_HIT;
		case PREV_SELECTED_HIT:
			return Navigate.NEXT_SELECTED_HIT;
		case NEXT_UNREAD_HIT:
			return Navigate.PREV_UNREAD_HIT;
		case PREV_UNREAD_HIT:
			return Navigate.NEXT_UNREAD_HIT;
		case NEXT_CATEGORY:
			return Navigate.PREV_CATEGORY;
		case PREV_CATEGORY:
			return Navigate.NEXT_CATEGORY;
		case NEXT_NONCATEGORY:
			return Navigate.PREV_NONCATEGORY;
		case PREV_NONCATEGORY:
			return Navigate.NEXT_NONCATEGORY;
		}

		return nav;
	}
	
	@Override
	protected void finalize() throws Throwable {
		recycle();
	}
	
	public boolean isRecycled() {
		if (NotesContext.is64Bit()) {
			return m_hCollection64==0;
		}
		else {
			return m_hCollection32==0;
		}
	}
	
	public void recycle() {
		if (!m_noRecycle) {
			boolean bHandleIsNull = false;
			if (NotesContext.is64Bit()) {
				bHandleIsNull = m_hCollection64==0;
			}
			else {
				bHandleIsNull = m_hCollection32==0;
			}
			
			if (!bHandleIsNull) {
				clearSearch();
				
				if (m_unreadTable!=null && !m_unreadTable.isRecycled()) {
					m_unreadTable.recycle();
				}
				
				NotesCAPI notesAPI = NotesContext.getNotesAPI();
				short result;
				if (NotesContext.is64Bit()) {
					result = notesAPI.b64_NIFCloseCollection(m_hCollection64);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectRecycled(this);
					m_hCollection64=0;
				}
				else {
					result = notesAPI.b32_NIFCloseCollection(m_hCollection32);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectRecycled(this);
					m_hCollection32=0;
				}
				
			}
		}
	}

	public void setNoRecycle() {
		m_noRecycle=true;
	}
	
	private void checkHandle() {
		if (NotesContext.is64Bit()) {
			if (m_hCollection64==0)
				throw new NotesError(0, "Collection already recycled");
		}
		else {
			if (m_hCollection32==0)
				throw new NotesError(0, "Collection already recycled");
		}
	}

	public int getNoteId() {
		return m_viewNoteId;
	}
	
	public String getUNID() {
		return m_viewUNID;
	}
	
	public int getHandle32() {
		return m_hCollection32;
	}

	public long getHandle64() {
		return m_hCollection64;
	}

	public String getContextUser() {
		return m_asUserCanonical;
	}
	
	public NotesIDTable getUnreadTable() {
		return m_unreadTable;
	}
	
	public NotesIDTable getCollapsedList() {
		return m_collapsedList;
	}
	
	public NotesIDTable getSelectedList() {
		return m_selectedList;
	}
	
	/**
	 * Performs a fulltext search in the collection
	 * 
	 * @param query fulltext query
	 * @param limit max entries to return or 0 to get all
	 * @param options FTSearch flags
	 * @param filterIDTable optional ID table to refine the search
	 * @return search result
	 */
	public SearchResult ftSearch(String query, short limit, EnumSet<FTSearch> options, NotesIDTable filterIDTable) {
		clearSearch();
		
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		short result;
		if (NotesContext.is64Bit()) {
			LongByReference rethSearch = new LongByReference();
			result = notesAPI.b64_FTOpenSearch(rethSearch);
			NotesErrorUtils.checkResult(result);
			m_activeFTSearchHandle64 = rethSearch;
		}
		else {
			IntByReference rethSearch = new IntByReference();
			result = notesAPI.b32_FTOpenSearch(rethSearch);
			NotesErrorUtils.checkResult(result);
			m_activeFTSearchHandle32 = rethSearch;
		}
		
		
		Memory queryLMBCS = NotesStringUtils.toLMBCS(query);
		IntByReference retNumDocs = new IntByReference();
		
		//always filter view data
		EnumSet<FTSearch> optionsWithView = options.clone();
		optionsWithView.add(FTSearch.SET_COLL);
		int optionsWithViewBitMask = FTSearch.toBitMask(optionsWithView);
		
		if (NotesContext.is64Bit()) {
			LongByReference rethResults = new LongByReference();
			result = notesAPI.b64_FTSearch(
					m_hDB64,
					m_activeFTSearchHandle64,
					m_hCollection64,
					queryLMBCS,
					optionsWithViewBitMask,
					limit,
					filterIDTable==null ? 0 : filterIDTable.getHandle64(),
					retNumDocs,
					new Memory(Pointer.SIZE), // Reserved field
					rethResults);
			if (result == 3874) {
				//handle special error code: no documents found
				return new SearchResult(null, 0);
			}
			NotesErrorUtils.checkResult(result);
			
			return new SearchResult(rethResults.getValue()==0 ? null : new NotesIDTable(rethResults), retNumDocs.getValue());
		}
		else {
			IntByReference rethResults = new IntByReference();
			result = notesAPI.b32_FTSearch(
					m_hDB32,
					m_activeFTSearchHandle32,
					m_hCollection32,
					queryLMBCS,
					optionsWithViewBitMask,
					limit,
					filterIDTable==null ? 0 : filterIDTable.getHandle32(),
					retNumDocs,
					new Memory(Pointer.SIZE), // Reserved field
					rethResults);
			
			if (result == 3874) {
				//handle special error code: no documents found
				return new SearchResult(null, 0);
			}
			NotesErrorUtils.checkResult(result);
			
			return new SearchResult(rethResults.getValue()==0 ? null : new NotesIDTable(rethResults), retNumDocs.getValue());
		}
	}
	
	/**
	 * Container for a FT search result
	 * 
	 * @author Karsten Lehmann
	 */
	public static class SearchResult {
		private NotesIDTable m_matchesIDTable;
		private int m_numDocs;
		
		public SearchResult(NotesIDTable matchesIDTable, int numDocs) {
			m_matchesIDTable = matchesIDTable;
			m_numDocs = numDocs;
		}
		
		public int getNumDocs() {
			return m_numDocs;
		}
		
		public NotesIDTable getMatches() {
			return m_matchesIDTable;
		}
		
	}
	
	/**
	 * Resets an active filtering cause by a FT search
	 */
	public void clearSearch() {
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		
		if (NotesContext.is64Bit()) {
			if (m_activeFTSearchHandle64!=null) {
				short result = notesAPI.b64_FTCloseSearch(m_activeFTSearchHandle64.getValue());
				NotesErrorUtils.checkResult(result);
				m_activeFTSearchHandle64=null;
			}
		}
		else {
			if (m_activeFTSearchHandle32!=null) {
				short result = notesAPI.b64_FTCloseSearch(m_activeFTSearchHandle32.getValue());
				NotesErrorUtils.checkResult(result);
				m_activeFTSearchHandle32=null;
			}
		}
	}
	
	/**
	 * Locates a note in the collection
	 * 
	 * @param noteId note id
	 * @return collection position
	 * @throws NotesError if not found
	 */
	public String locateNote(int noteId) {
		checkHandle();

		NotesCollectionPosition foundPos = new NotesCollectionPosition();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		short result;
		if (NotesContext.is64Bit()) {
			result = notesAPI.b64_NIFLocateNote(m_hCollection64, foundPos, noteId);
		}
		else {
			result = notesAPI.b32_NIFLocateNote(m_hCollection32, foundPos, noteId);
		}
		NotesErrorUtils.checkResult(result);
		return foundPos.toPosString();
	}

	/**
	 * Locates a note in the collection
	 * 
	 * @param noteId note id as hex string
	 * @return collection position
	 * @throws NotesError if not found
	 */
	public String locateNote(String noteId) {
		return locateNote(Integer.parseInt(noteId, 16));
	}

	/**
	 * This function searches through a collection for the first note whose sort
	 * column values match the given search keys.<br>
	 * <br>
	 * The search key consists of an array containing one or several values.
	 * This function matches each value in the
	 * search key against the corresponding sorted column of the view or folder.<br>
	 * <br>
	 * Only sorted columns are used. The values in the search key
	 * must be specified in the same order as the sorted columns in the view
	 * or folder, from left to right.  Other unsorted columns may lie between
	 * the sorted columns to be searched.<br>
	 * <br>
	 * For example, suppose view columns 1, 3, 4 and 5 are sorted.<br>
	 * The key buffer may contain search keys for: just column 1; columns 1
	 * and 3; or for columns 1, 3, and 4.<br>
	 * <br>
	 * This function yields the COLLECTIONPOSITION of the first note in the
	 * collection that matches the keys. It also yields a count of the number
	 * of notes that match the keys. Since all notes that match the keys
	 * appear contiguously in the view or folder, you may pass the resulting
	 * COLLECTIONPOSITION and match count as inputs to
	 * {@link NotesCollection#readEntries(NotesCollectionPosition, short, int, short, int, int)}
	 * to read all the entries in the collection that match the keys.<br>
	 * <br>
	 * If multiple notes match the specified (partial) keys, and
	 * {@link Find#FIRST_EQUAL} (the default flag) is specified, 
	 * hen the position
	 * of the first matching note is returned ("first" is defined by the
	 * note which collates before all the others in the view).<br>
	 * <br>
	 * The position of the last matching note is returned if {@link Find#LAST_EQUAL}
	 * is specified.  If {@link Find#LESS_THAN} is specified,
	 * then the last note
	 * with a key value less than the specified key is returned.<br>
	 * <br>
	 * If {@link Find#GREATER_THAN} is specified, then the first
	 * note with a key
	 * value greater than the specified key is returned.<br>
	 * <br>
	 * This routine cannot be used to locate notes that are categorized
	 * under multiple categories (the resulting position is unpredictable),
	 * and also cannot be used to locate responses.<br>
	 * <br>
	 * This routine is usually not appropriate for equality searches of key
	 * values of {@link Calendar}.<br>
	 * <br>
	 * A match will only be found if the key value is
	 * as precise as and is equal to the internally stored data.<br>
	 * <br>
	 * {@link Calendar} data is displayed with less precision than what is stored
	 * internally.  Use inequality searches, such as {@link Find#GREATER_THAN} or
	 * {@link Find#LESS_THAN}, for {@link Calendar} key values
	 * to avoid having to find
	 * an exact match of the specified value.  If the precise key value
	 * is known, however, equality searches of {@link Calendar} values are supported.<br>
	 * <br>
	 * Returning the number of matches on an inequality search is not supported.<br>
	 * <br>
	 * In other words, if you specify any one of the following for the FindFlags argument:<br>
	 * {@link Find#LESS_THAN}<br>
	 * {@link Find#LESS_THAN} | {@link Find#EQUAL}<br>
	 * {@link Find#GREATER_THAN}<br>
	 * {@link Find#GREATER_THAN} | {@link Find#EQUAL}<br>
	 * <br>
	 * this function cannot determine the number of notes that match the search
	 * condition (use {@link #canFindExactNumberOfMatches(short)} to check
	 * whether a combination of find flags can return the exact number of matches).<br>
	 * If we cannot determine the number of notes, the function will return 1 for the count
	 * value returned by {@link FindResult#getEntriesFound()}.

	 * @param findFlags {@see Find}
	 * @param keys lookup keys, can be {@link String}, double / {@link Double}, int / {@link Integer}, {@link Date}, {@link Calendar}, {@link Date}[] or {@link Calendar}[] with two elements or {@link Interval} for date ranges
	 * @return result
	 */
	public FindResult findByKey(EnumSet<Find> findFlags, Object... keys) {
		checkHandle();
		
		if (keys==null || keys.length==0)
			throw new IllegalArgumentException("No search keys specified");
		
		IntByReference retNumMatches = new IntByReference();
		NotesCollectionPosition retIndexPos = new NotesCollectionPosition();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		short findFlagsBitMask = Find.toBitMask(findFlags);
		short result;
		if (NotesContext.is64Bit()) {
			Memory keyBuffer;
			try {
				keyBuffer = NotesSearchKeyEncoder.b64_encodeKeys(keys);
			} catch (Throwable e) {
				throw new NotesError(0, "Could not encode search keys", e);
			}
			result = notesAPI.b64_NIFFindByKey(m_hCollection64, keyBuffer, findFlagsBitMask, retIndexPos, retNumMatches);
		}
		else {
			Memory keyBuffer;
			try {
				keyBuffer = NotesSearchKeyEncoder.b32_encodeKeys(keys);
			} catch (Throwable e) {
				throw new NotesError(0, "Could not encode search keys", e);
			}
			result = notesAPI.b32_NIFFindByKey(m_hCollection32, keyBuffer, findFlagsBitMask, retIndexPos, retNumMatches);
		}
		if (result == 1028 || result == 17412) {
			return new FindResult("", 0, canFindExactNumberOfMatches(findFlags));
		}
		
		NotesErrorUtils.checkResult(result);
		
		int nMatchesFound = retNumMatches.getValue();

		int[] retTumbler = retIndexPos.Tumbler;
		short retLevel = retIndexPos.Level;
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<=retLevel; i++) {
			if (sb.length()>0)
				sb.append(".");

			sb.append(retTumbler[i]);
		}

		String firstMatchPos = sb.toString();
		return new FindResult(firstMatchPos, nMatchesFound, canFindExactNumberOfMatches(findFlags));
	}
	
	/**
	 * This function searches through a collection for notes whose primary sort
	 * key matches a given string. The primary sort key for a given note is the 
	 * value displayed for that note in the leftmost sorted column in the view
	 * or folder. Use this function only when the leftmost sorted column of
	 * the view or folder is a string.<br>
	 * <br>
	 * This function yields the COLLECTIONPOSITION of the first note in the
	 * collection that matches the string. It also yields a count of the number
	 * of notes that match the string.<br>
	 * <br>
	 * With views that are not categorized, all notes with primary sort keys that
	 * match the string appear contiguously in the view or folder.<br>
	 * <br>
	 * This means you may pass the resulting COLLECTIONPOSITION and match count
	 * as inputs to {@link #readEntries(NotesCollectionPosition, short, int, short, int, int)}
	 * to read all the entries in the collection that match the string.<br>
	 * <br>
	 * This routine returns limited results if the view is categorized.<br>
	 * <br>
	 * Views that are categorized do not necessarily list all notes whose<br>
	 * sort keys match the string contiguously; such as in the case where
	 * the category note intervenes.<br>
	 * Likewise, this routine cannot be used to locate notes that are
	 * categorized under multiple categories (the resulting position is unpredictable),
	 * and also cannot be used to locate responses.<br>
	 * <br>
	 * Use {@link #findByKey(short, Object...)} if the leftmost sorted column
	 * is a number or a time/date.<br>
	 * <br>
	 * Returning the number of matches on an inequality search is not supported.<br>
	 * <br>
	 * In other words, if you specify any one of the following for the FindFlags argument:<br>
	 * <br>
	 * {@link Find#LESS_THAN}<br>
	 * {@link Find#LESS_THAN} | {@link Find#EQUAL}<br>
	 * {@link Find#GREATER_THAN}<br>
	 * {@link Find#GREATER_THAN} | {@link Find#EQUAL}<br>
	 * <br>
	 * this function cannot determine the number of notes that match the search
	 * condition (use {@link #canFindExactNumberOfMatches(short)} to check
	 * whether a combination of find flags can return the exact number of matches).<br>
	 * If we cannot determine the number of notes, the function will return 1 for the count
	 * value returned by {@link FindResult#getEntriesFound()}.
	 * 
	 * @param name name to look for
	 * @param findFlags find flags, see {@link Find}
	 * @return result
	 */
	public FindResult findByName(String name, EnumSet<Find> findFlags) {
		checkHandle();
		
		Memory nameLMBCS = NotesStringUtils.toLMBCS(name);

		IntByReference retNumMatches = new IntByReference();
		NotesCollectionPosition retIndexPos = new NotesCollectionPosition();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		short findFlagsBitMask = Find.toBitMask(findFlags);
		short result;
		if (NotesContext.is64Bit()) {
			result = notesAPI.b64_NIFFindByName(m_hCollection64, nameLMBCS, findFlagsBitMask, retIndexPos, retNumMatches);
		}
		else {
			result = notesAPI.b32_NIFFindByName(m_hCollection32, nameLMBCS, findFlagsBitMask, retIndexPos, retNumMatches);
		}
		if (result == 1028 || result == 17412) {
			return new FindResult("", 0, canFindExactNumberOfMatches(findFlags));
		}

		NotesErrorUtils.checkResult(result);

		int nMatchesFound = retNumMatches.getValue();

		int[] retTumbler = retIndexPos.Tumbler;
		short retLevel = retIndexPos.Level;
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<=retLevel; i++) {
			if (sb.length()>0)
				sb.append(".");

			sb.append(retTumbler[i]);
		}

		String firstMatchPos = sb.toString();
		return new FindResult(firstMatchPos, nMatchesFound, canFindExactNumberOfMatches(findFlags));
	}

	/**
	 * If the specified find flag uses an inequality search like {@link Find#LESS_THAN}
	 * or {@link Find#GREATER_THAN}, this method returns true, meaning that
	 * the Notes API cannot return an exact number of matches.
	 * 
	 * @param findFlags find flags
	 * @return true if exact number of matches can be returned
	 */
	public boolean canFindExactNumberOfMatches(EnumSet<Find> findFlags) {
		if (findFlags.contains(Find.LESS_THAN)) {
			return false;
		}
		else if (findFlags.contains(Find.GREATER_THAN)) {
			return false;
		}
		else {
			return true;
		}
	}
	
	/**
	 * Container object for a collection lookup result
	 * 
	 * @author Karsten Lehmann
	 */
	public static class FindResult {
		private String m_position;
		private int m_entriesFound;
		private boolean m_hasExactNumberOfMatches;
		
		public FindResult(String position, int entriesFound, boolean hasExactNumberOfMatches) {
			m_position = position;
			m_entriesFound = entriesFound;
			m_hasExactNumberOfMatches = hasExactNumberOfMatches;
		}

		public int getEntriesFound() {
			return m_entriesFound;
		}

		public String getPosition() {
			return m_position;
		}
		
		public boolean hasExactNumberOfMatches() {
			return m_hasExactNumberOfMatches;
		}
	}
	
	/**
	 * Reads collection entries (using NIFReadEntries method)
	 * 
	 * @param startPos start position for the scan; will be modified by the method to reflect the current position
	 * @param skipNavigator navigator to use for the skip operation
	 * @param skipCount number of entries to skip
	 * @param returnNavigator navigator to use for the read operation
	 * @param returnCount number of entries to read
	 * @param returnMask bitmask of data to read
	 * @return read data
	 */
	public NotesViewData readEntries(NotesCollectionPosition startPos, EnumSet<Navigate> skipNavigator, int skipCount, EnumSet<Navigate> returnNavigator, int returnCount, EnumSet<ReadMask> returnMask) {
		return readEntries(startPos, skipNavigator, skipCount, returnNavigator, returnCount, returnMask, null);
	}
	
	/**
	 * Reads collection entries (using NIFReadEntries method)
	 * 
	 * @param startPos start position for the scan; will be modified by the method to reflect the current position
	 * @param skipNavigator navigator to use for the skip operation
	 * @param skipCount number of entries to skip
	 * @param returnNavigator navigator to use for the read operation
	 * @param returnCount number of entries to read
	 * @param returnMask bitmask of data to read
	 * @param decodeColumns optional array with columns to be decoded
	 * @return read data
	 */
	public NotesViewData readEntries(NotesCollectionPosition startPos, EnumSet<Navigate> skipNavigator, int skipCount, EnumSet<Navigate> returnNavigator, int returnCount, EnumSet<ReadMask> returnMask, boolean[] decodeColumns) {
		checkHandle();

		IntByReference retNumEntriesSkipped = new IntByReference();
		IntByReference retNumEntriesReturned = new IntByReference();
		ShortByReference retSignalFlags = new ShortByReference();
		ShortByReference retBufferLength = new ShortByReference();

		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		short skipNavBitMask = Navigate.toBitMask(skipNavigator);
		short returnNavBitMask = Navigate.toBitMask(returnNavigator);
		int readMaskBitMask = ReadMask.toBitMask(returnMask);
		
		short result;
		if (NotesContext.is64Bit()) {
			LongByReference retBuffer = new LongByReference();
			result = notesAPI.b64_NIFReadEntries(m_hCollection64, // hCollection
					startPos, // IndexPos
					skipNavBitMask, // SkipNavigator
					skipCount, // SkipCount
					returnNavBitMask, // ReturnNavigator
					returnCount, // ReturnCount
					readMaskBitMask, // Return mask
					retBuffer, // rethBuffer
					retBufferLength, // retBufferLength
					retNumEntriesSkipped, // retNumEntriesSkipped
					retNumEntriesReturned, // retNumEntriesReturned
					retSignalFlags // retSignalFlags
					);
			NotesErrorUtils.checkResult(result);
			
			if (retBufferLength.getValue()==0) {
				return new NotesViewData(null, new ArrayList<NotesViewEntryData>(0), retNumEntriesSkipped.getValue(), retNumEntriesReturned.getValue(), retSignalFlags.getValue());
			}
			else {
				NotesViewData viewData = NotesSummaryBufferDecoder.b64_decodeBuffer(retBuffer.getValue(), retNumEntriesSkipped.getValue(), retNumEntriesReturned.getValue(), returnMask, retSignalFlags.getValue(), decodeColumns);
				return viewData;
			}
		}
		else {
			IntByReference retBuffer = new IntByReference();
			result = notesAPI.b32_NIFReadEntries(m_hCollection32, // hCollection
					startPos, // IndexPos
					skipNavBitMask, // SkipNavigator
					skipCount, // SkipCount
					returnNavBitMask, // ReturnNavigator
					returnCount, // ReturnCount
					readMaskBitMask, // Return mask
					retBuffer, // rethBuffer
					retBufferLength, // retBufferLength
					retNumEntriesSkipped, // retNumEntriesSkipped
					retNumEntriesReturned, // retNumEntriesReturned
					retSignalFlags // retSignalFlags
					);
			NotesErrorUtils.checkResult(result);
			
			if (retBufferLength.getValue()==0) {
				return new NotesViewData(null, new ArrayList<NotesViewEntryData>(0), retNumEntriesSkipped.getValue(), retNumEntriesReturned.getValue(), retSignalFlags.getValue());
			}
			else {
				NotesViewData viewData = NotesSummaryBufferDecoder.b32_decodeBuffer(retBuffer.getValue(), retNumEntriesSkipped.getValue(), retNumEntriesReturned.getValue(), returnMask, retSignalFlags.getValue(), decodeColumns);
				return viewData;
			}
		}
	}

	/**
	 * Updates the view to reflect the current database content (using NIFUpdateCollection method)
	 */
	public void refresh() {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		short result;
		if (NotesContext.is64Bit()) {
			result = notesAPI.b64_NIFUpdateCollection(m_hCollection64);
		}
		else {
			result = notesAPI.b32_NIFUpdateCollection(m_hCollection32);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Returns the currently active collation
	 * 
	 * @return collation
	 */
	public short getCollation() {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		short result;
		ShortByReference retCollationNum = new ShortByReference();
		
		if (NotesContext.is64Bit()) {
			result = notesAPI.b64_NIFGetCollation(m_hCollection64, retCollationNum);
		}
		else {
			result = notesAPI.b32_NIFGetCollation(m_hCollection32, retCollationNum);
		}
		NotesErrorUtils.checkResult(result);
		return retCollationNum.getValue();
	}
	
	/**
	 * Sets the active collation (collection column sorting)
	 * 
	 * @param collation collation
	 */
	public void setCollation(short collation) {
		checkHandle();
		NotesCAPI notesAPI = NotesContext.getNotesAPI();
		short result;
		
		if (NotesContext.is64Bit()) {
			result = notesAPI.b64_NIFSetCollation(m_hCollection64, collation);
		}
		else {
			result = notesAPI.b32_NIFSetCollation(m_hCollection32, collation);
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * Scans the columns of the specified {@link View} and computes the collation indices
	 * 
	 * @param view view to scan
	 * @return info object with hashed collation indices
	 * @throws NotesException
	 */
	public CollationInfo hashCollations(View view) throws NotesException {
		CollationInfo collationInfo = new CollationInfo();
		
		Vector<?> columns = view.getColumns();
		try {
			short collation = 1;
			for (int i=0; i<columns.size(); i++) {
				ViewColumn currCol = (ViewColumn) columns.get(i);
				boolean isResortAscending = currCol.isResortAscending();
				boolean isResortDescending = currCol.isResortDescending();
				
				if (isResortAscending || isResortDescending) {
					String currItemName = currCol.getItemName();
					
					if (isResortAscending) {
						collationInfo.addCollation(collation, currItemName, Direction.Ascending);
						collation++;
					}
					if (isResortDescending) {
						collationInfo.addCollation(collation, currItemName, Direction.Descending);
						collation++;
					}
				}
			}
			
			return collationInfo;
		}
		finally {
			view.recycle(columns);
		}
	}
	
	/**
	 * Container class with view collation information (collation index vs. sort item name and sort direction)
	 * 
	 * @author Karsten Lehmann
	 */
	public static class CollationInfo {
		private Map<String,Short> m_ascendingLookup;
		private Map<String,Short> m_descendingLookup;
		private Map<Short,String> m_collationSortItem;
		private Map<Short,Direction> m_collationSorting;
		private int m_nrOfCollations;
		
		public CollationInfo() {
			m_ascendingLookup = new HashMap<String,Short>();
			m_descendingLookup = new HashMap<String,Short>();
			m_collationSortItem = new HashMap<Short, String>();
			m_collationSorting = new HashMap<Short, NotesCollection.Direction>();
		}
		
		/**
		 * Internal method to populate the maps
		 * 
		 * @param collation collation index
		 * @param itemName sort item name
		 * @param direction sort direction
		 */
		void addCollation(short collation, String itemName, Direction direction) {
			if (direction == Direction.Ascending) {
				m_ascendingLookup.put(itemName.toLowerCase(), Short.valueOf(collation));
			}
			else if (direction == Direction.Descending) {
				m_descendingLookup.put(itemName.toLowerCase(), Short.valueOf(collation));
			}
			m_nrOfCollations = Math.max(m_nrOfCollations, collation);
		}
		
		/**
		 * Returns the total number of collations
		 * 
		 * @return number
		 */
		public int getNumberOfCollations() {
			return m_nrOfCollations;
		}
		
		/**
		 * Finds a collation index
		 * 
		 * @param sortItem sort item name
		 * @param direction sort direction
		 * @return collation index or -1 if not found
		 */
		public short findCollation(String sortItem, Direction direction) {
			if (direction==Direction.Ascending) {
				Short collation = m_ascendingLookup.get(sortItem.toLowerCase());
				return collation==null ? -1 : collation.shortValue();
			}
			else {
				Short collation = m_descendingLookup.get(sortItem.toLowerCase());
				return collation==null ? -1 : collation.shortValue();
			}
		}
		
		/**
		 * Returns the sort item name of a collation
		 * 
		 * @param collation collation index
		 * @return sort item name
		 */
		public String getSortItem(int collation) {
			if (collation > m_nrOfCollations)
				throw new IndexOutOfBoundsException("Unknown collation index (max value: "+m_nrOfCollations+")");
			
			String sortItem = m_collationSortItem.get(Integer.valueOf(collation));
			return sortItem;
		}
		
		/**
		 * Returns the sort direction of a collation
		 * 
		 * @param collation collation index
		 * @return sort direction
		 */
		public Direction getSortDirection(int collation) {
			if (collation > m_nrOfCollations)
				throw new IndexOutOfBoundsException("Unknown collation index (max value: "+m_nrOfCollations+")");
			
			Direction direction = m_collationSorting.get(Integer.valueOf(collation));
			return direction;
		}
	}
	
	/** Available column sort directions */
	public static enum Direction {Ascending, Descending};
	
	/**
	 * Finds the matching collation nunber for the specified sort column and direction
	 * Convenience method that calls {@link #hashCollations(View)} and {@link CollationInfo#findCollation(String, Direction)}
	 * 
	 * @param view view view to search for the collation
	 * @param columnName sort column name
	 * @param direction sort direction
	 * @return collation number or -1 if not found
	 * @throws NotesException
	 */
	public short findCollation(View view, String columnName, Direction direction) throws NotesException {
		return hashCollations(view).findCollation(columnName, direction);
	}
}
