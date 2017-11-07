package com.mindoo.domino.jna;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import com.mindoo.domino.jna.CollectionDataCache.CacheState;
import com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action;
import com.mindoo.domino.jna.NotesViewEntryData.CacheableViewEntryData;
import com.mindoo.domino.jna.constants.FTSearch;
import com.mindoo.domino.jna.constants.Find;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.constants.UpdateCollectionFilters;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder.ItemTableData;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder.ItemValueTableData;
import com.mindoo.domino.jna.internal.NotesSearchKeyEncoder;
import com.mindoo.domino.jna.queries.condition.Selection;
import com.mindoo.domino.jna.structs.NotesCollectionDataStruct;
import com.mindoo.domino.jna.structs.NotesCollectionPositionStruct;
import com.mindoo.domino.jna.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

import lotus.domino.Database;
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
	private boolean m_autoUpdate;
	private CollationInfo m_collationInfo;
	private Map<String, Integer> m_columnIndicesByItemName;
	private Map<String, Integer> m_columnIndicesByTitle;
	private Map<Integer, String> m_columnNamesByIndex;
	private Map<Integer, Boolean> m_columnIsCategoryByIndex;
	private Map<Integer, String> m_columnTitlesLCByIndex;
	private Map<Integer, String> m_columnTitlesByIndex;
	private NotesNote m_viewNote;
	private NotesViewFormat m_viewFormat;
	
	/**
	 * Creates a new instance, 32 bit mode
	 * 
	 * @param parentDb parent database
	 * @param hCollection collection handle
	 * @param name collection name
	 * @param viewNoteId view note id
	 * @param viewUNID view UNID
	 * @param collapsedList id table for the collapsed list
	 * @param selectedList id table for the selected list
	 * @param unreadTable id table for the unread list
	 * @param asUserCanonical user used to read the collection data
	 */
	public NotesCollection(NotesDatabase parentDb, int hCollection, String name, int viewNoteId, String viewUNID,
			NotesIDTable collapsedList, NotesIDTable selectedList, NotesIDTable unreadTable, String asUserCanonical) {
		if (NotesJNAContext.is64Bit())
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
		
		m_autoUpdate = true;
	}

	/**
	 * Creates a new instance, 64 bit mode
	 * 
	 * @param parentDb parent database
	 * @param hCollection collection handle
	 * @param name collection name
	 * @param viewNoteId view note id
	 * @param viewUNID view UNID
	 * @param collapsedList id table for the collapsed list
	 * @param selectedList id table for the selected list
	 * @param unreadTable id table for the unread list
	 * @param asUserCanonical user used to read the collection data
	 */
	public NotesCollection(NotesDatabase parentDb, long hCollection, String name, int viewNoteId, String viewUNID,
			NotesIDTable collapsedList, NotesIDTable selectedList, NotesIDTable unreadTable, String asUserCanonical) {
		if (!NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_asUserCanonical = asUserCanonical;
		m_parentDb = parentDb;
		m_hDB64 = parentDb.getHandle64();
		m_hCollection64 = hCollection;
		m_name = name;
		m_viewNoteId = viewNoteId;
		m_viewUNID = viewUNID;
		m_collapsedList = collapsedList;
		m_selectedList = selectedList;
		m_unreadTable = unreadTable;
		
		m_autoUpdate = true;
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
	 * Method to check whether a collection column contains a category
	 * 
	 * @param columnName programmatic column name
	 * @return true if category, false otherwise
	 */
	public boolean isCategoryColumn(String columnName) {
		if (m_columnIsCategoryByIndex==null) {
			scanColumns();
		}
		int colValuesIndex = getColumnValuesIndex(columnName);
		Boolean isCategory = m_columnIsCategoryByIndex.get(colValuesIndex);
		return Boolean.TRUE.equals(isCategory);
	}
	
	/**
	 * Returns the column values index for the specified programmatic column name
	 * or column title
	 * 
	 * @param columnNameOrTitle programmatic column name or title, case insensitive
	 * @return index or -1 for unknown columns; returns 65535 for static column values that are not returned as column values
	 */
	public int getColumnValuesIndex(String columnNameOrTitle) {
		if (m_columnIndicesByItemName==null) {
			scanColumns();
		}
		Integer idx = m_columnIndicesByItemName.get(columnNameOrTitle.toLowerCase());
		if (idx==null) {
			idx = m_columnIndicesByTitle.get(columnNameOrTitle.toLowerCase());
		}
		return idx==null ? -1 : idx.intValue();
	}
	
	/**
	 * Returns whether the view automatically handles view index updates while reading from the view.<br>
	 * <br>
	 * This flag is used by the methods<br>
	 * <br>
	 * <ul>
	 * <li>{@link #getAllEntries(String, int, EnumSet, int, EnumSet, ViewLookupCallback)}</li>
	 * <li>{@link #getAllEntriesByKey(EnumSet, EnumSet, ViewLookupCallback, Object...)}</li>
	 * <li>{@link #getAllIds(Navigate)}</li>
	 * <li>{@link #getAllIdsByKey(EnumSet, Object...)}</li>
	 * </ul>
	 * @return true if auto update
	 */
	public boolean isAutoUpdate() {
		return m_autoUpdate;
	}
	
	/**
	 * Changes the auto update flag, which indicates whether the view automatically handles view index
	 * updates while reading from the view.<br>
	 * <br>
	 * This flag is used by the methods<br>
	 * <br>
	 * <ul>
	 * <li>{@link #getAllEntries(String, int, EnumSet, int, EnumSet, ViewLookupCallback)}</li>
	 * <li>{@link #getAllEntriesByKey(EnumSet, EnumSet, ViewLookupCallback, Object...)}</li>
	 * <li>{@link #getAllIds(Navigate)}</li>
	 * <li>{@link #getAllIdsByKey(EnumSet, Object...)}</li>
	 * </ul>
	 * @param update true to activate auto update
	 */
	public void setAutoUpdate(boolean update) {
		m_autoUpdate = update;
	}
	
	/**
	 * Returns the index modified sequence number that can be used to track view changes.
	 * The method calls {@link #getLastModifiedTime()} and returns part of the result (Innards[0]).
	 * We found out by testing that this value is the same that NIFFindByKeyExtended2 returns.
	 * 
	 * @return index modified sequence number
	 */
	public int getIndexModifiedSequenceNo() {
		NotesTimeDate ndtModified = getLastModifiedTime();
		return ndtModified.getAdapter(NotesTimeDateStruct.class).Innards[0];
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
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		NotesTimeDateStruct retLastModifiedTime = NotesTimeDateStruct.newInstance();
		
		if (NotesJNAContext.is64Bit()) {
			notesAPI.b64_NIFGetLastModifiedTime(m_hCollection64, retLastModifiedTime);
		}
		else {
			notesAPI.b32_NIFGetLastModifiedTime(m_hCollection32, retLastModifiedTime);
		}
		return new NotesTimeDate(retLastModifiedTime);
	}

	/**
	 * This function adds the document(s) specified in an ID Table to a folder.
	 * 
	 * @param idTable id table
	 */
	public void addToFolder(NotesIDTable idTable) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_FolderDocAdd(m_hDB64, 0, m_viewNoteId, idTable.getHandle64(), 0);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_FolderDocAdd(m_hDB32, 0, m_viewNoteId, idTable.getHandle32(), 0);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function adds the document(s) specified as note id set to a folder
	 * 
	 * @param noteIds ids of notes to add
	 */
	public void addToFolder(Set<Integer> noteIds) {
		NotesIDTable idTable = new NotesIDTable();
		try {
			idTable.addNotes(noteIds);
			addToFolder(idTable);
		}
		finally {
			idTable.recycle();
		}
	}

	/**
	 * This function removes the document(s) specified in an ID Table from a folder.
	 * 
	 * @param idTable id table
	 */
	public void removeFromFolder(NotesIDTable idTable) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_FolderDocRemove(m_hDB64, 0, m_viewNoteId, idTable.getHandle64(), 0);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_FolderDocRemove(m_hDB32, 0, m_viewNoteId, idTable.getHandle32(), 0);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function removes the document(s) specified as note id set from a folder.
	 * 
	 * @param noteIds ids of notes to remove
	 */
	public void removeFromFolder(Set<Integer> noteIds) {
		NotesIDTable idTable = new NotesIDTable();
		try {
			idTable.addNotes(noteIds);
			removeFromFolder(idTable);
		}
		finally {
			idTable.recycle();
		}
	}
	
	/**
	 * This function removes all documents from a specified folder.<br>
	 * <br>
	 * Subfolders and documents within the subfolders are not removed.
	 */
	public void removeAllFromFolder() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_FolderDocRemoveAll(m_hDB64, 0, m_viewNoteId, 0);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_FolderDocRemoveAll(m_hDB32, 0, m_viewNoteId, 0);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function moves the specified folder under a given parent folder.<br>
	 * <br>
	 * If the parent folder is a shared folder, then the child folder must be a shared folder.<br>
	 * If the parent folder is a private folder, then the child folder must be a private folder.
	 * 
	 * @param newParentFolder parent folder
	 */
	public void moveFolder(NotesCollection newParentFolder) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_FolderMove(m_hDB64, 0, m_viewNoteId, 0, newParentFolder.getNoteId(), 0);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_FolderMove(m_hDB32, 0, m_viewNoteId, 0, newParentFolder.getNoteId(), 0);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function renames the specified folder and its subfolders.
	 * 
	 * @param name new folder name
	 */
	public void renameFolder(String name) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		Memory pszName = NotesStringUtils.toLMBCS(name, false);
		if (pszName.size() > NotesCAPI.DESIGN_FOLDER_MAX_NAME) {
			throw new IllegalArgumentException("Folder name too long (max "+NotesCAPI.DESIGN_FOLDER_MAX_NAME+" bytes, found "+pszName.size()+" bytes)");
		}
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_FolderRename(m_hDB64, 0, m_viewNoteId, pszName, (short) pszName.size(), 0);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_FolderRename(m_hDB32, 0, m_viewNoteId, pszName, (short) pszName.size(), 0);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function returns the number of entries in the specified folder's index.<br>
	 * <br>
	 * This is the number of documents plus the number of cateogories (if any) in the folder.<br>
	 * <br>
	 * Subfolders and documents in subfolders are not included in the count.
	 * 
	 * @return count
	 */
	public long getFolderDocCount() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		if (NotesJNAContext.is64Bit()) {
			LongByReference pdwNumDocs = new LongByReference();
			short result = notesAPI.b64_FolderDocCount(m_hDB64, 0, m_viewNoteId, 0, pdwNumDocs);
			NotesErrorUtils.checkResult(result);
			return pdwNumDocs.getValue();
		}
		else {
			LongByReference pdwNumDocs = new LongByReference();
			short result = notesAPI.b32_FolderDocCount(m_hDB32, 0, m_viewNoteId, 0, pdwNumDocs);
			NotesErrorUtils.checkResult(result);
			return pdwNumDocs.getValue();
		}
	}
	
	/**
	 * Returns an id table of the folder content
	 * 
	 * @param validateIds If set, return only "validated" noteIDs
	 * @return id table
	 */
	public NotesIDTable getIDTableForFolder(boolean validateIds) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			LongByReference hTable = new LongByReference();
			short result = notesAPI.b64_NSFFolderGetIDTable(m_hDB64, m_hDB64, m_viewNoteId, validateIds ? NotesCAPI.DB_GETIDTABLE_VALIDATE : 0, hTable);
			NotesErrorUtils.checkResult(result);
			//don't auto-gc the ID table, since there is no remark in the C API that we are responsible
			return new NotesIDTable(hTable.getValue(), true);
		}
		else {
			IntByReference hTable = new IntByReference();
			short result = notesAPI.b32_NSFFolderGetIDTable(m_hDB32, m_hDB32, m_viewNoteId, validateIds ? NotesCAPI.DB_GETIDTABLE_VALIDATE : 0, hTable);
			NotesErrorUtils.checkResult(result);
			//don't auto-gc the ID table, since there is no remark in the C API that we are responsible
			return new NotesIDTable(hTable.getValue(), true);
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
	 * Method to reverse the traversal order, e.g. from {@link Navigate#NEXT} to
	 * {@link Navigate#PREV}.
	 * 
	 * @param nav nav constant
	 * @return reversed constant
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
		case PREV:
			return Navigate.NEXT;
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
		default:
			return nav;
		}
	}
	
	public boolean isRecycled() {
		if (NotesJNAContext.is64Bit()) {
			return m_hCollection64==0;
		}
		else {
			return m_hCollection32==0;
		}
	}
	
	public void recycle() {
		if (!m_noRecycle) {
			boolean bHandleIsNull = false;
			if (NotesJNAContext.is64Bit()) {
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
				
				NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
				short result;
				if (NotesJNAContext.is64Bit()) {
					result = notesAPI.b64_NIFCloseCollection(m_hCollection64);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectBeeingBeRecycled(NotesCollection.class, this);
					m_hCollection64=0;
				}
				else {
					result = notesAPI.b32_NIFCloseCollection(m_hCollection32);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectBeeingBeRecycled(NotesCollection.class, this);
					m_hCollection32=0;
				}
				
			}
		}
	}

	public void setNoRecycle() {
		m_noRecycle=true;
	}
	
	@Override
	public boolean isNoRecycle() {
		return m_noRecycle;
	}
	
	private void checkHandle() {
		if (NotesJNAContext.is64Bit()) {
			if (m_hCollection64==0)
				throw new NotesError(0, "Collection already recycled");
			NotesGC.__b64_checkValidObjectHandle(NotesCollection.class, m_hCollection64);
		}
		else {
			if (m_hCollection32==0)
				throw new NotesError(0, "Collection already recycled");
			NotesGC.__b32_checkValidObjectHandle(NotesCollection.class, m_hCollection32);
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

	/**
	 * Returns the user for which the collation returns the data
	 * 
	 * @return null for server
	 */
	public String getContextUser() {
		return m_asUserCanonical;
	}
	
	/**
	 * Returns the unread table.<br>Adding note ids
	 * to this table causes the notes to be found in view lookups using {@link Navigate#NEXT_UNREAD}
	 * and similar flags.<br>
	 * <br>
	 * For remote databases, do not forget to call {@link #updateFilters(EnumSet)}
	 * after you are done modifying the table to re-send the list to the server.
	 * 
	 * @return unread table
	 */
	public NotesIDTable getUnreadTable() {
		return m_unreadTable;
	}
	
	/**
	 * Returns the collapsed list. Can be used to tell Domino which categories are
	 * expanded/collapsed.<br> Adding note ids
	 * to this table causes the notes to be found in view lookups using {@link Navigate#NEXT_EXPANDED},
	 * {@link Navigate#NEXT_EXPANDED_CATEGORY}, {@link Navigate#NEXT_EXPANDED_SELECTED} or
	 * {@link Navigate#NEXT_EXPANDED_UNREAD}.<br>
	 * <br>
	 * For remote databases, do not forget to call {@link #updateFilters(EnumSet)}
	 * after you are done modifying the table to re-send the list to the server.
	 * 
	 * @return collapsed list
	 */
	public NotesIDTable getCollapsedList() {
		return m_collapsedList;
	}
	
	/**
	 * Returns an id table of "selected" note ids; adding note ids
	 * to this table causes the notes to be found in view lookups using {@link Navigate#NEXT_SELECTED}
	 * and similar flags.<br>
	 * <br>
	 * For remote databases, do not forget to call {@link #updateFilters(EnumSet)}
	 * after you are done modifying the table to re-send the list to the server.
	 * 
	 * @return selected list
	 */
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
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
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
		
		
		Memory queryLMBCS = NotesStringUtils.toLMBCS(query, true);
		IntByReference retNumDocs = new IntByReference();
		
		//always filter view data
		EnumSet<FTSearch> optionsWithView = options.clone();
		optionsWithView.add(FTSearch.SET_COLL);
		int optionsWithViewBitMask = FTSearch.toBitMask(optionsWithView);
		
		if (NotesJNAContext.is64Bit()) {
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
			
			NotesIDTable resultsIdTable = null;

			long hResults = rethResults.getValue();
			if (hResults!=0) {
				resultsIdTable = new NotesIDTable(rethResults.getValue(), false);
			}
			if (options.contains(FTSearch.RET_IDTABLE) && resultsIdTable==null) {
				resultsIdTable = new NotesIDTable();
			}

			return new SearchResult(resultsIdTable, retNumDocs.getValue());
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
			
			NotesIDTable resultsIdTable = null;
			
			int hResults = rethResults.getValue();
			if (hResults!=0) {
				resultsIdTable = new NotesIDTable(rethResults.getValue(), false);
			}
			if (options.contains(FTSearch.RET_IDTABLE) && resultsIdTable==null) {
				resultsIdTable = new NotesIDTable();
			}

			return new SearchResult(resultsIdTable, retNumDocs.getValue());
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
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			if (m_activeFTSearchHandle64!=null) {
				short result = notesAPI.b64_FTCloseSearch(m_activeFTSearchHandle64.getValue());
				NotesErrorUtils.checkResult(result);
				m_activeFTSearchHandle64=null;
			}
		}
		else {
			if (m_activeFTSearchHandle32!=null) {
				short result = notesAPI.b32_FTCloseSearch(m_activeFTSearchHandle32.getValue());
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

		NotesCollectionPositionStruct foundPos = NotesCollectionPositionStruct.newInstance();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
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
	 * Convenience function that returns a sorted set of note ids of documents
	 * matching the specified search key(s) in the collection
	 * 
	 * @param findFlags find flags, see {@link Find}
	 * @param keys lookup keys
	 * @return note ids
	 */
	public LinkedHashSet<Integer> getAllIdsByKey(EnumSet<Find> findFlags, Object... keys) {
		LinkedHashSet<Integer> noteIds = getAllEntriesByKey(findFlags, EnumSet.of(ReadMask.NOTEID), 
				new ViewLookupCallback<LinkedHashSet<Integer>>() {

			@Override
			public LinkedHashSet<Integer> startingLookup() {
				return new LinkedHashSet<Integer>();
			}

			@Override
			public com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action entryRead(
					LinkedHashSet<Integer> result, NotesViewEntryData entryData) {
				
				result.add(entryData.getNoteId());
				return Action.Continue;
			}
			
			@Override
			public LinkedHashSet<Integer> lookupDone(LinkedHashSet<Integer> result) {
				return result;
			}

		}, keys);
		return noteIds;
	}
	
	/**
	 * Method to check whether an optimized  view lookup method can be used for
	 * a set of find/return flags and the current Domino version
	 * 
	 * @param findFlags find flags
	 * @param returnMask return flags
	 * @param keys lookup keys
	 * @return true if method can be used
	 */
	private boolean canUseOptimizedLookupForKeyLookup(EnumSet<Find> findFlags, EnumSet<ReadMask> returnMask, Object... keys) {
		if (findFlags.contains(Find.GREATER_THAN) || findFlags.contains(Find.LESS_THAN)) {
			//TODO check this with IBM dev; we had crashes like "[0A0F:0002-21A00] PANIC: LookupHandle: null handle" using NIFFindByKeyExtended2
			return false;
		}
		{
			//we had "ERR 774: Unsupported return flag(s)" errors when using the optimized lookup
			//method with other return values other than note id
			boolean unsupportedValuesFound = false;
			for (ReadMask currReadMaskValues: returnMask) {
				if ((currReadMaskValues != ReadMask.NOTEID) && (currReadMaskValues != ReadMask.SUMMARY)) {
					unsupportedValuesFound = true;
					break;
				}
			}

			if (unsupportedValuesFound) {
				return false;
			}
		}
		
		{
			//check for R9 and flag compatibility
			short buildVersion = m_parentDb.getParentServerBuildVersion();
			if (buildVersion < 400) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Callback base class used to process collection lookup results
	 * 
	 * @author Karsten Lehmann
	 */
	public static abstract class ViewLookupCallback<T> {
		public enum Action {Continue, Stop};
		private NotesTimeDate m_newDiffTime;
		
		/**
		 * The method is called when the view lookup is (re-)started. If the view
		 * index is modified while reading, the view read operation restarts from
		 * the beginning.
		 * 
		 * @return result object that is passed to {@link #entryRead(Object, NotesViewEntryData)}
		 */
		public abstract T startingLookup();
		
		/**
		 * Override this method to return the programmatic name of a collection column. If
		 * a non-null value is returned, we use an optimized lookup method to read the data,
		 * resulting in much better performance (working like the formula @DbColumn)
		 * 
		 * @return programmatic column name or null
		 */
		public String getNameForSingleColumnRead() {
			return null;
		}
		
		/**
		 * Implement this method to process a read entry directly or add it to a result object.<br>
		 * Please note: If you process the entry directly, keep in mind that the lookup
		 * may restart when a view index change is detected.
		 * 
		 * @param result context
		 * @param entryData entry data
		 * @return action (whether the lookup should continue)
		 */
		public abstract Action entryRead(T result, NotesViewEntryData entryData);
		
		/**
		 * Override this empty method to get notified about view index changes
		 */
		public void viewIndexChangeDetected() {
		}
		
		/**
		 * The method is called when differential view reading is used to return the {@link NotesTimeDate}
		 * to be used for the next lookups
		 * 
		 * @param newDiffTime new diff time
		 */
		public void setNewDiffTime(NotesTimeDate newDiffTime) {
			m_newDiffTime = newDiffTime;
		}
		
		/**
		 * Use this method to read the {@link NotesTimeDate} to be used for the next lookups when using differential view
		 * reads
		 * 
		 * @return diff time or null
		 */
		public NotesTimeDate getNewDiffTime() {
			return m_newDiffTime;
		}
		
		/**
		 * Override this method to return an optional {@link CollectionDataCache} to speed up view reading.
		 * The returned cache instance is shared for all calls done with this callback implementation.<br>
		 * <br>
		 * Please note that according to IBM dev, this optimized view reading (differential view reads) does
		 * only work in views that are not permuted (where documents do not appear multiple times, because
		 * "Show multiple values as separate entries" has been set on any view column).
		 * 
		 * @return cache or null (default value)
		 */
		public CollectionDataCache createDataCache() {
			return null;
		}
		
		private CollectionDataCache m_cacheInstance;
		
		/**
		 * Standard implementation of this method calls {@link #createDataCache()} once
		 * and stores the object instance in a member variable for later reuse.<br>
		 * Can be overridden in case you need to store the cache somewhere else,
		 * e.g. to reuse it later on.
		 * 
		 * @return cache
		 */
		public CollectionDataCache getDataCache() {
			if (m_cacheInstance==null) {
				m_cacheInstance = createDataCache();
			}
			return m_cacheInstance;
		}
		
		/**
		 * Method is called when the lookup process is done
		 * 
		 * @param result result object
		 * @return result or transformed result
		 */
		public abstract T lookupDone(T result);
	}

	/**
	 * Subclass of {@link ViewLookupCallback} that wraps any methods and forwards all calls
	 * the another {@link ViewLookupCallback}.
	 * 
	 * @author Karsten Lehmann
	 */
	public static class ViewLookupCallbackWrapper<T> extends ViewLookupCallback<T> {
		private ViewLookupCallback<T> m_innerCallback;
		
		public ViewLookupCallbackWrapper(ViewLookupCallback<T> innerCallback) {
			m_innerCallback = innerCallback;
		}

		@Override
		public String getNameForSingleColumnRead() {
			return m_innerCallback.getNameForSingleColumnRead();
		}
		
		@Override
		public T startingLookup() {
			return m_innerCallback.startingLookup();
		}

		@Override
		public com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action entryRead(T result,
				NotesViewEntryData entryData) {
			return m_innerCallback.entryRead(result, entryData);
		}

		@Override
		public CollectionDataCache createDataCache() {
			return m_innerCallback.createDataCache();
		}
		
		@Override
		public NotesTimeDate getNewDiffTime() {
			return m_innerCallback.getNewDiffTime();
		}
		
		@Override
		public void setNewDiffTime(NotesTimeDate newDiffTime) {
			m_innerCallback.setNewDiffTime(newDiffTime);
		}
		
		@Override
		public T lookupDone(T result) {
			return m_innerCallback.lookupDone(result);
		}
		
		@Override
		public void viewIndexChangeDetected() {
			m_innerCallback.viewIndexChangeDetected();
		}
	}
	
	/**
	 * Subclass of {@link ViewLookupCallback} that uses an optimized view lookup to
	 * only read the value of a single collection column. This results in much
	 * better performance, because the 64K summary buffer is not polluted with irrelevant data.<br>
	 * <br>
	 * Please make sure to pass either {@link ReadMask#SUMMARYVALUES} or {@link ReadMask#SUMMARY},
	 * preferably {@link ReadMask#SUMMARYVALUES}.
	 * 
	 * @author Karsten Lehmann
	 */
	public static class ReadSingleColumnValues extends ViewLookupCallback<Set<String>> {
		private String m_columnName;
		private Locale m_sortLocale;
		
		/**
		 * Creates a new instance
		 * 
		 * @param columnName programmatic column name
		 * @param sortLocale optional sort locale used to sort the result
		 */
		public ReadSingleColumnValues(String columnName, Locale sortLocale) {
			m_columnName = columnName;
			m_sortLocale = sortLocale;
		}

		@Override
		public String getNameForSingleColumnRead() {
			return m_columnName;
		}
		
		@Override
		public Set<String> startingLookup() {
			Collator collator = Collator.getInstance(m_sortLocale==null ? Locale.getDefault() : m_sortLocale);
			return new TreeSet<String>(collator);
		}

		@Override
		public com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action entryRead(Set<String> result,
				NotesViewEntryData entryData) {
			String colValue = entryData.getAsString(m_columnName, null);
			if (colValue!=null) {
				result.add(colValue);
			}
			return com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action.Continue;
		}

		@Override
		public Set<String> lookupDone(Set<String> result) {
			return result;
		}
	}
	
	/**
	 * Subclass of {@link ViewLookupCallback} that stores the data of read collection entries
	 * in a {@link List}.
	 * 
	 * @author Karsten Lehmann
	 */
	public static class EntriesAsListCallback extends ViewLookupCallback<List<NotesViewEntryData>> {
		private int m_maxEntries;
		
		/**
		 * Creates a new instance
		 * 
		 * @param maxEntries maximum entries to return
		 */
		public EntriesAsListCallback(int maxEntries) {
			m_maxEntries = maxEntries;
		}
		
		@Override
		public List<NotesViewEntryData> startingLookup() {
			return new ArrayList<NotesViewEntryData>();
		}

		@Override
		public com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action entryRead(
				List<NotesViewEntryData> result, NotesViewEntryData entryData) {
			
			if (m_maxEntries==0) {
				return Action.Stop;
			}

			if (!isAccepted(entryData)) {
				//ignore this entry
				return Action.Continue;
			}

			//add entry to result list
			result.add(entryData);
			
			if (result.size() >= m_maxEntries) {
				//stop the lookup, we have enough data
				return Action.Stop;
			}
			else {
				//go on reading the view
				return Action.Continue;
			}
		}

		/**
		 * Override this method to filter entries
		 * 
		 * @param entryData current entry
		 * @return true if entry should be added to the result
		 */
		protected boolean isAccepted(NotesViewEntryData entryData) {
			return true;
		}
		
		@Override
		public List<NotesViewEntryData> lookupDone(List<NotesViewEntryData> result) {
			return result;
		}
	}

	/**
	 * Very fast scan function that populates a {@link NotesIDTable} with note ids in the
	 * collection. Uses an undocumented C API call internally. Since the {@link NotesIDTable}
	 * is sorted in ascending note id order, this method does not keep the original view order.
	 * Use {@link NotesCollection#getAllIds(Navigate)} to get an ID list sorted in view order.
	 * 
	 * @param navigator use {@link Navigate#NEXT} to read documents and categories, {@link Navigate#NEXT_CATEGORY} to only read categories and {@link Navigate#NEXT_NONCATEGORY} to only read documents
	 * @param filterTable true to filter the ID table to entries visible for the current user
	 * @param idTable table to populate with note ids
	 */
	public void getAllIds(Navigate navigator, boolean filterTable, NotesIDTable idTable) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NIFGetIDTableExtended(m_hCollection64, Navigate.toBitMask(EnumSet.of(navigator)),
					(short) (filterTable ? 0 : 1), idTable.getHandle64());
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NIFGetIDTableExtended(m_hCollection32, Navigate.toBitMask(EnumSet.of(navigator)),
					(short) (filterTable ? 0 : 1), idTable.getHandle32());
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * Convenience method that collects all note ids in the view, in the sort order of the current collation
	 * 
	 * @param navigator use {@link Navigate#NEXT} to read documents and categories, {@link Navigate#NEXT_CATEGORY} to only read categories and {@link Navigate#NEXT_NONCATEGORY} to only read documents
	 * @return set of note ids, sorted by occurence in the collection
	 */
	public LinkedHashSet<Integer> getAllIds(Navigate navigator) {
		LinkedHashSet<Integer> ids = getAllEntries("0", 1, EnumSet.of(navigator), Integer.MAX_VALUE, EnumSet.of(ReadMask.NOTEID), new ViewLookupCallback<LinkedHashSet<Integer>>() {

			@Override
			public LinkedHashSet<Integer> startingLookup() {
				return new LinkedHashSet<Integer>();
			}

			@Override
			public com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action entryRead(
					LinkedHashSet<Integer> ctx, NotesViewEntryData entryData) {
				
				ctx.add(entryData.getNoteId());
				return Action.Continue;
			}
			
			@Override
			public LinkedHashSet<Integer> lookupDone(LinkedHashSet<Integer> result) {
				return result;
			}
			
		});
		return ids;
	}
	
	/**
	 * Reads all values of a collection column
	 * 
	 * @param columnName programmatic column name
	 * @param sortLocale optional sort locale to sort the values; if null, we use the locale returned by {@link Locale#getDefault()}
	 * @return column values
	 */
	public Set<String> getColumnValues(String columnName, Locale sortLocale) {
		boolean isCategory = isCategoryColumn(columnName);
		Navigate nav = isCategory ? Navigate.NEXT_CATEGORY : Navigate.NEXT_NONCATEGORY;
		
		return getAllEntries("0", 1, EnumSet.of(nav), Integer.MAX_VALUE, EnumSet.of(ReadMask.SUMMARYVALUES), new ReadSingleColumnValues(columnName, sortLocale));
	}
	
	/**
	 * The method reads a number of entries located under a specified category from the collection/view.
	 * It internally takes care of view index changes while reading view data and restarts reading
	 * if such a change has been detected.
	 * 
	 * @param <T> result data type
	 * 
	 * @param category category or catlevel1\catlevel2 structure
	 * @param skipCount number of entries to skip
	 * @param returnNav navigator to specify how to move in the collection
	 * @param preloadEntryCount amount of entries that is read from the view; if a filter is specified, this should be higher than returnCount
	 * @param returnMask values to extract
	 * @param callback callback that is called for each entry read from the collection
	 * @return lookup result
	 */
	
	public <T> T getAllEntriesInCategory(String category, int skipCount, EnumSet<Navigate> returnNav,
			int preloadEntryCount, EnumSet<ReadMask> returnMask,
			final ViewLookupCallback<T> callback) {
		
		return getAllEntriesInCategory(category, skipCount, returnNav, null, null, preloadEntryCount, returnMask, callback);
	}

	/**
	 * Convenience method that reads all note ids located under a category
	 * 
	 * @param category category
	 * @param returnNav navigator to be used to scan for collection entries
	 * @return ids in view order
	 */
	public LinkedHashSet<Integer> getAllIdsInCategory(String category, EnumSet<Navigate> returnNav) {
		return getAllEntriesInCategory(category, 0, returnNav, Integer.MAX_VALUE, EnumSet.of(ReadMask.NOTEID),
				new ViewLookupCallback<LinkedHashSet<Integer>>() {

			@Override
			public LinkedHashSet<Integer> startingLookup() {
				return new LinkedHashSet<Integer>();
			}

			@Override
			public com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action entryRead(
					LinkedHashSet<Integer> ctx, NotesViewEntryData entryData) {

				ctx.add(entryData.getNoteId());
				return Action.Continue;
			}

			@Override
			public LinkedHashSet<Integer> lookupDone(LinkedHashSet<Integer> result) {
				return result;
			}

		});
	}

	/**
	 * The method reads a number of entries located under a specified category from the collection/view.
	 * It internally takes care of view index changes while reading view data and restarts reading
	 * if such a change has been detected.
	 * 
	 * @param <T> result data type
	 * 
	 * @param category category or catlevel1\catlevel2 structure
	 * @param skipCount number of entries to skip
	 * @param returnNav navigator to specify how to move in the collection
	 * @param diffTime If non-null, this is a "differential view read" meaning that the caller wants
	 * 				us to optimize things by only returning full information for notes which have
	 * 				changed (or are new) in the view, return just NoteIDs for notes which haven't
	 * 				changed since this time and return a deleted ID table for notes which may be
	 * 				known by the caller and have been deleted since DiffTime.
	 * 				<b>Please note that "differential view reads" do only work in views without permutations (no columns with "show multiple values as separate entries" set) according to IBM. Otherwise, all the view data is always returned.</b>
	 * @param diffIDTable If DiffTime is non-null and DiffIDTable is not null it provides a
	 * 				list of notes which the caller has current information on.  We use this to
	 * 				know which notes we can return shortened information for (i.e., just the NoteID)
	 * 				and what notes we might have to include in the returned DelNoteIDTable.
	 * @param preloadEntryCount amount of entries that is read from the view; if a filter is specified, this should be higher than returnCount
	 * @param returnMask values to extract
	 * @param callback callback that is called for each entry read from the collection
	 * @return lookup result
	 */
	public <T> T getAllEntriesInCategory(final String category, int skipCount, EnumSet<Navigate> returnNav,
			NotesTimeDate diffTime, NotesIDTable diffIDTable, int preloadEntryCount, EnumSet<ReadMask> returnMask,
			final ViewLookupCallback<T> callback) {
		
		final String[] categoryPos = new String[1];
		
		IStartPositionRetriever catPosRetriever = new IStartPositionRetriever() {

			@Override
			public String getStartPosition() {
				//find category entry using NIFFindByKeyExtended2 with flag FIND_CATEGORY_MATCH
				NotesViewLookupResultData catLkResult = findByKeyExtended2(EnumSet.of(Find.MATCH_CATEGORYORLEAF,
						Find.REFRESH_FIRST, Find.RETURN_DWORD, Find.AND_READ_MATCHES),
						EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARY), category);
				
				if (catLkResult.getReturnCount()==0) {
					//category not found
					return null;
				}
				
				categoryPos[0] = catLkResult.getPosition();
				if (StringUtil.isEmpty(categoryPos[0])) {
					//category not found
					return null;
				}
				else {
					return categoryPos[0];
				}
			}
		};
		
		EnumSet<ReadMask> useReturnMask = returnMask.clone();
		//make sure that we get the entry position for the range check
		useReturnMask.add(ReadMask.INDEXPOSITION);

		return getAllEntries(catPosRetriever, skipCount, returnNav,
				preloadEntryCount, useReturnMask, new ViewLookupCallbackWrapper<T>(callback) {
			
			@Override
			public com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action entryRead(T result,
					NotesViewEntryData entryData) {
				
				//check if this entry is still one of the descendants of the category entry
				String entryPos = entryData.getPositionStr();
				if (entryPos.equals(categoryPos[0])) {
					//skip category entry
					return Action.Continue;
				}
				else if (entryPos.startsWith(categoryPos[0])) {
					return super.entryRead(result, entryData);
				}
				else {
					return Action.Stop;
				}
			}
		});
	}

	/**
	 * The method reads a number of entries from the collection/view, starting an a row
	 * with the specified note id. The method internally takes care
	 * of view index changes while reading view data and restarts reading if such a change has been
	 * detected.
	 * 
	 * @param noteId note id to start reading
	 * @param skipCount number entries to skip before reading
	 * @param returnNav navigator to specify how to move in the collection
	 * @param preloadEntryCount amount of entries that is read from the view; if a filter is specified, this should be higher than returnCount
	 * @param returnMask values to extract
	 * @param callback callback that is called for each entry read from the collection
	 * @return lookup result
	 * 
	 * @param <T> type of lookup result object
	 */
	public <T> T getAllEntriesStartingAtNoteId(int noteId, int skipCount, EnumSet<Navigate> returnNav,
			int preloadEntryCount,
			EnumSet<ReadMask> returnMask, ViewLookupCallback<T> callback) {
		
		String fakePos = Integer.toString(noteId);
		EnumSet<ReadMask> useReturnMask = returnMask.clone();
		useReturnMask.add(ReadMask.INIT_POS_NOTEID);
		
		T entries = getAllEntries(fakePos, skipCount, returnNav, preloadEntryCount, useReturnMask, callback);
		return entries;
	}

	/**
	 * The method reads a number of entries from the collection/view. It internally takes care
	 * of view index changes while reading view data and restarts reading if such a change has been
	 * detected.
	 * 
	 * @param startPosStr start position; use "0" or null to start before the first entry
	 * @param skipCount number entries to skip before reading
	 * @param returnNav navigator to specify how to move in the collection
	 * @param preloadEntryCount amount of entries that is read from the view; if a filter is specified, this should be higher than returnCount
	 * @param returnMask values to extract
	 * @param callback callback that is called for each entry read from the collection
	 * @return lookup result
	 * 
	 * @param <T> type of lookup result object
	 */
	public <T> T getAllEntries(final String startPosStr, int skipCount, EnumSet<Navigate> returnNav,
			int preloadEntryCount,
			EnumSet<ReadMask> returnMask, ViewLookupCallback<T> callback) {
		
		return getAllEntries(new IStartPositionRetriever() {

			@Override
			public String getStartPosition() {
				return startPosStr;
			}
			
		}, skipCount, returnNav, preloadEntryCount, returnMask, callback);
	}
	
	/**
	 * Callback to dynamically locate the start position of a collection scan, e.g.
	 * the position of a category entry. We use a callback to be able to react on
	 * view index updates. Since a lookup may be repeated when the view index changes,
	 * this callback may be called multiple times to return a fresh starting position
	 * for the lookup.
	 * 
	 * @author Karsten Lehmann
	 */
	private static interface IStartPositionRetriever {
		
		/**
		 * Implement this method to find the lookup start position
		 * 
		 * @return start position or null if not found
		 */
		public String getStartPosition();
		
	}
	
	/**
	 * The method reads a number of entries from the collection/view. It internally takes care
	 * of view index changes while reading view data and restarts reading if such a change has been
	 * detected.
	 * 
	 * @param startPosRetriever callback to find the start position to read
	 * @param skipCount number entries to skip before reading
	 * @param returnNav navigator to specify how to move in the collection
	 * @param preloadEntryCount amount of entries that is read from the view; if a filter is specified, this should be higher than returnCount
	 * @param returnMask values to extract
	 * @param callback callback that is called for each entry read from the collection
	 * @return lookup result
	 * 
	 * @param <T> type of lookup result object
	 */
	private <T> T getAllEntries(IStartPositionRetriever startPosRetriever, int skipCount, EnumSet<Navigate> returnNav,
			int preloadEntryCount,
			EnumSet<ReadMask> returnMask, ViewLookupCallback<T> callback) {
		
		EnumSet<ReadMask> useReturnMask = returnMask;

		//decide whether we need to use the undocumented NIFReadEntriesExt
		String readSingleColumnName = callback.getNameForSingleColumnRead();
		if (readSingleColumnName!=null) {
			//make sure that we actually read any column values
			if (!useReturnMask.contains(ReadMask.SUMMARY) && !useReturnMask.contains(ReadMask.SUMMARYVALUES)) {
				useReturnMask.add(ReadMask.SUMMARYVALUES);
			}
		}
		
		CollectionDataCache dataCache = callback.getDataCache();
		if (useReturnMask.equals(EnumSet.of(ReadMask.NOTEID))) {
			//disable cache if all we need to read is the note id
			dataCache = null;
		}
		
		Integer readSingleColumnIndex = readSingleColumnName==null ? null : getColumnValuesIndex(readSingleColumnName);
		
		if (readSingleColumnName!=null) {
			//TODO view row caching currently disabled for single column reads, needs more work
			dataCache = null;
		}

		if (dataCache!=null) {
			//if caching is used, make sure that we read the note id, because that's how we hash our data
			if (!useReturnMask.contains(ReadMask.NOTEID) && !useReturnMask.contains(ReadMask.NOTEID)) {
				useReturnMask = useReturnMask.clone();
				useReturnMask.add(ReadMask.NOTEID);
			}
		}

		while (true) {
			int indexModifiedBeforeGettingStartPos = getIndexModifiedSequenceNo();
			
			String startPosStr = startPosRetriever.getStartPosition();
			if (StringUtil.isEmpty(startPosStr)) {
				T result = callback.startingLookup();
				result = callback.lookupDone(result);
				return result;
			}
			
			int indexModifiedAfterGettingStartPos = getIndexModifiedSequenceNo();

			if (indexModifiedBeforeGettingStartPos != indexModifiedAfterGettingStartPos) {
				//view index was changed while reading; restart scan
				callback.viewIndexChangeDetected();
				update();
				continue;
			}
			
			NotesCollectionPositionStruct pos = NotesCollectionPositionStruct.toPosition(("last".equalsIgnoreCase(startPosStr) || startPosStr==null) ? "0" : startPosStr);
			NotesCollectionPosition posWrap = new NotesCollectionPosition(pos);

			T result = callback.startingLookup();
			
			if (preloadEntryCount==0) {
				//nothing to do
				result = callback.lookupDone(result);
				return result;
			}
			
			boolean viewModified = false;
			boolean firstLoopRun = true;
			
			NotesTimeDate retDiffTime = null;
			
			NotesTimeDate diffTime = null;
			NotesIDTable diffIDTable = null;
			
			if (dataCache!=null) {
				CacheState cacheState = dataCache.getCacheState();
				
				//only use cache content if read masks are compatible
				Map<Integer,CacheableViewEntryData> cacheEntries = cacheState.getCacheEntries();
				if (cacheEntries!=null && !cacheEntries.isEmpty()) {
					EnumSet<ReadMask> cacheReadMask = cacheState.getReadMask();
					if (useReturnMask.equals(cacheReadMask)) {
						diffTime = cacheState.getDiffTime();

						diffIDTable = new NotesIDTable();
						diffIDTable.addNotes(cacheEntries.keySet());
					}
				}
			}
			
			List<NotesViewEntryData> entriesToUpdateCache = dataCache==null ? null : new ArrayList<NotesViewEntryData>();
			
			while (true) {
				if (preloadEntryCount==0) {
					break;
				}

				int useSkipCount;
				if (firstLoopRun) {
					if ("last".equalsIgnoreCase(startPosStr)) {
						//TODO make "last" work when called from getAllEntriesInCategory
						
						//first jump to the end of the view
						useSkipCount = Integer.MAX_VALUE;
					}
					else {
						useSkipCount = skipCount;
					}
				}
				else {
					//just skip the last entry that we returned on the last NIFReadEntries call
					useSkipCount = 1;
				}
				EnumSet<Navigate> skipNav = returnNav.clone();
				if (firstLoopRun) {
					if ("last".equalsIgnoreCase(startPosStr)) {
						//compute the skipNav by reversing the returnNav; e.g. for startPos="last"
						//and returnNav=Navigate.PREV_SELECTED, we first jump to the end of the view
						//with skipCount=INTEGER.MAX_VALUE Navigate.NEXT_SELECTED.
						//Then we start reading n entries with Navigate.PREV_SELECTED,
						//effectively returning the last n selected entries of the view
						skipNav = EnumSet.noneOf(Navigate.class);
						for (Navigate currNav : returnNav) {
							skipNav.add(reverseNav(currNav));
						}
						//set NAVIGATE_CONTINUE to stop skipping on the last view element and not return an error
						skipNav.add(Navigate.CONTINUE);
					}
					else {
						skipNav = returnNav;
					}
				}
				else {
					skipNav = returnNav;
				}
				NotesViewLookupResultData data;
				data = readEntriesExt(posWrap, skipNav, useSkipCount, returnNav, preloadEntryCount, useReturnMask,
						diffTime, diffIDTable, readSingleColumnIndex);
				
				int indexModifiedAfterDataLookup = getIndexModifiedSequenceNo();

				if (indexModifiedAfterGettingStartPos != indexModifiedAfterDataLookup) {
					//view index was changed while reading; restart scan
					callback.viewIndexChangeDetected();
					update();
					continue;
				}

				if (useReturnMask.contains(ReadMask.INIT_POS_NOTEID)) {
					//make sure to only use this flag on the first lookup call
					useReturnMask.remove(ReadMask.INIT_POS_NOTEID);
				}
				
				retDiffTime = data.getReturnedDiffTime();
				
				if (dataCache!=null) {
					//if data cache is used, we fill in missing gaps in cases where NIF skipped producing
					//the summary data, because the corresponding cache entry was already
					//up to date
					List<NotesViewEntryData> entries = data.getEntries();
					dataCache.populateEntryStubsWithData(entries);
					
					entriesToUpdateCache.addAll(entries);
				}

				if (data.getReturnCount()==0) {
					//no more data found
					result = callback.lookupDone(result);
					
					if (dataCache!=null && retDiffTime!=null) {
						if (!entriesToUpdateCache.isEmpty()) {
							dataCache.addCacheValues(useReturnMask, retDiffTime, entriesToUpdateCache);
						}
						callback.setNewDiffTime(retDiffTime);
					}

					return result;
				}
				
				firstLoopRun = false;
				
				if (isAutoUpdate()) {
					if (data.hasAnyNonDataConflicts()) {
						//refresh the view and restart the lookup
						viewModified=true;
						break;
					}
				}
				
				List<NotesViewEntryData> entries = data.getEntries();
				for (NotesViewEntryData currEntry : entries) {
					Action action = callback.entryRead(result, currEntry);
					if (action==Action.Stop) {
						result = callback.lookupDone(result);
						
						if (dataCache!=null && retDiffTime!=null) {
							if (!entriesToUpdateCache.isEmpty()) {
								dataCache.addCacheValues(useReturnMask, retDiffTime, entriesToUpdateCache);
							}
							callback.setNewDiffTime(retDiffTime);
						}
						return result;
					}
				}
			}

			if (dataCache!=null && retDiffTime!=null) {
				if (!entriesToUpdateCache.isEmpty()) {
					dataCache.addCacheValues(useReturnMask, retDiffTime, entriesToUpdateCache);
				}
				callback.setNewDiffTime(retDiffTime);
			}

			if (diffIDTable!=null) {
				diffIDTable.recycle();
			}
			
			if (viewModified) {
				//view index was changed while reading; restart scan
				callback.viewIndexChangeDetected();
				update();
				continue;
			}
			
			return result;
		}
	}
	
	/**
	 * Returns all view entries matching the specified search key(s) in the collection.
	 * It internally takes care of view index changes while reading view data and restarts
	 * reading if such a change has been detected.
	 * 
	 * @param findFlags find flags, see {@link Find}
	 * @param returnMask values to be returned
	 * @param callback lookup callback
	 * @param keys lookup keys
	 * @return lookup result
	 * 
	 * @param <T> type of lookup result object
	 */
	public <T> T getAllEntriesByKey(EnumSet<Find> findFlags, EnumSet<ReadMask> returnMask, ViewLookupCallback<T> callback, Object... keys) {
		//we are leaving the loop when there is no more data to be read;
		//while(true) is here to rerun the query in case of view index changes while reading
		
		while (true) {
			T result = callback.startingLookup();

			NotesViewLookupResultData data;
			//position of first match
			String firstMatchPosStr;
			int remainingEntries;
			
			int entriesToSkipOnFirstLoopRun = 0;

			if (canUseOptimizedLookupForKeyLookup(findFlags, returnMask, keys)) {
				//do the first lookup and read operation atomically; uses a large buffer for local calls
				EnumSet<Find> findFlagsWithExtraBits = findFlags.clone();
				findFlagsWithExtraBits.add(Find.AND_READ_MATCHES);
				findFlagsWithExtraBits.add(Find.RETURN_DWORD);
				
				data = findByKeyExtended2(findFlagsWithExtraBits, returnMask, keys);
				
				int numEntriesFound = data.getReturnCount();
				if (numEntriesFound!=-1) {
					if (isAutoUpdate()) {
						//check for view index or design change
						if (data.hasAnyNonDataConflicts()) {
							//refresh the view and restart the lookup
							callback.viewIndexChangeDetected();
							update();
							continue;
						}
					}
					
					//copy the data we have read
					List<NotesViewEntryData> entries = data.getEntries();
					for (NotesViewEntryData currEntryData : entries) {
						Action action = callback.entryRead(result, currEntryData);
						if (action==Action.Stop) {
							result = callback.lookupDone(result);
							return result;
						}
					}
					entriesToSkipOnFirstLoopRun = entries.size();
					
					if (!data.hasMoreToDo()) {
						//we are done
						result = callback.lookupDone(result);
						return result;
					}

					//compute what we have left
					int entriesReadOnFirstLookup = entries.size();
					remainingEntries = numEntriesFound - entriesReadOnFirstLookup;
					firstMatchPosStr = data.getPosition();
				}
				else {
					//workaround for a bug where the method NIFFindByKeyExtended2 returns -1 as numEntriesFound
					//and no buffer data
					//
					//fallback to classic lookup until this is fixed/commented by IBM dev:
					FindResult findResult = findByKey(findFlags, keys);
					remainingEntries = findResult.getEntriesFound();
					if (remainingEntries==0) {
						return result;
					}
					firstMatchPosStr = findResult.getPosition();
				}
			}
			else {
				//first find the start position to read data
				FindResult findResult = findByKey(findFlags, keys);
				remainingEntries = findResult.getEntriesFound();
				if (remainingEntries==0) {
					return result;
				}
				firstMatchPosStr = findResult.getPosition();
			}

			if (!canFindExactNumberOfMatches(findFlags)) {
				Direction currSortDirection = getCurrentSortDirection();
				if (currSortDirection!=null) {
					//handle special case for inquality search where column sort order matches the find flag,
					//so we can read all view entries after findResult.getPosition()
					
					if (currSortDirection==Direction.Ascending && findFlags.contains(Find.GREATER_THAN)) {
						//read all entries after findResult.getPosition()
						remainingEntries = Integer.MAX_VALUE;
					}
					else if (currSortDirection==Direction.Descending && findFlags.contains(Find.LESS_THAN)) {
						//read all entries after findResult.getPosition()
						remainingEntries = Integer.MAX_VALUE;
					}
				}
			}

			if (firstMatchPosStr!=null) {
				//position of the first match; we skip (entries.size()) to read the remaining entries
				boolean isFirstLookup = true;
				
				NotesCollectionPositionStruct lookupPos = NotesCollectionPositionStruct.toPosition(firstMatchPosStr);
				NotesCollectionPosition lookupPosWrap = new NotesCollectionPosition(lookupPos);
				
				boolean viewModified = false;
				
				while (remainingEntries>0) {
					//on first lookup, start at "posStr" and skip the amount of already read entries
					data = readEntries(lookupPosWrap, EnumSet.of(Navigate.NEXT_NONCATEGORY), isFirstLookup ? entriesToSkipOnFirstLoopRun : 1, EnumSet.of(Navigate.NEXT_NONCATEGORY), remainingEntries, returnMask);

					if (isFirstLookup || isAutoUpdate()) {
						//for the first lookup, make sure we start at the right position
						if (data.hasAnyNonDataConflicts()) {
							//set viewModified to true and leave the inner loop; we will refresh the view and restart the lookup
							viewModified=true;
							break;
						}
					}
					isFirstLookup=false;
					
					List<NotesViewEntryData> entries = data.getEntries();
					if (entries.isEmpty()) {
						//looks like we don't have any more data in the view
						break;
					}
					
					for (NotesViewEntryData currEntryData : entries) {
						Action action = callback.entryRead(result, currEntryData);
						if (action==Action.Stop) {
							result = callback.lookupDone(result);
							return result;
						}
					}
					remainingEntries = remainingEntries - entries.size();
				}
				
				if (viewModified) {
					//refresh view and redo the whole lookup
					callback.viewIndexChangeDetected();
					update();
					continue;
				}
			}
			
			result = callback.lookupDone(result);
			return result;
		}
	}
	
	/**
	 * This method is in essense a combo NIFFindKey/NIFReadEntries API. It leverages
	 * the C API method NIFFindByKeyExtended2 internally which was introduced in Domino R9<br>
	 * <br>
	 * The purpose of this method is to provide a mechanism to position into a
	 * collection and read the associated entries in an atomic manner.<br>
	 * <br>
	 * More specifically, the key provided is positioned to and the entries from
	 * the collection are read while the collection is read locked so that no other updates can occur.<br>
	 * <br>
	 * 1)  This avoids the possibility of the initial collection position shifting
	 * due to an insert/delete/update in and/or around the logical key value that
	 * would result in an ordinal change to the position.<br>
	 * <br>
	 * This a classic problem when doing a NIFFindKey, getting the position returned,
	 * and then doing a NIFReadEntries following.<br>
	 * <br>
	 * 2) The API improves the ability to read all the entries that are associated
	 * with the key position atomically.<br>
	 * <br>
	 * This can be done depending on the size of the data being returned.<br>
	 * <br>
	 * If all the data fits into the limitation (64K) of the return buffer, then
	 * it will be done atomically in 1 call.<br>
	 * Otherwise subsequent NIFReadEntries will need to be called, which will be non-atomic.<br>
	 * <br>
	 * The 64K limit only changes behavior to NIFFindByKey/NIFReadEntries when the call is client/server.
	 * Locally there is no limit.
	 * <hr>
	 * Original documentation of C API method NIFFindByKeyExtended2:<br>
	 * <br>
	 * NIFFindByKeyExtended2 - Lookup index entry by "key"<br>
	 * <br>
	 *	Given a "key" buffer in the standard format of a summary buffer,<br>
	 *	locate the entry which matches the given key(s).  Supply as many<br>
	 *	"key" summary items as required to correspond to the way the index<br>
	 *	collates, in order to uniquely find an entry.<br>
	 * <br>
	 *	If multiple index entries match the specified key (especially if<br>
	 *	not enough key items were specified), then the index position of<br>
	 *	the FIRST matching entry is returned ("first" is defined by the<br>
	 *	entry which collates before all others in the collated index).<br>
	 * <br>
	 *	Note that the more explicitly an entry can be specified (by<br>
	 *	specifying as many keys as possible), then the faster the lookup<br>
	 *	can be performed, since the "key" lookup is very fast, but a<br>
	 *	sequential search is performed to locate the "first" entry when<br>
	 *	multiple entries match.<br>
	 * <br>
	 *	This routine can only be used when dealing with notes that do not<br>
	 *	have multiple permutations, and cannot be used to locate response<br>
	 *	notes.
	 * 
	 * @param findFlags find flags ({@link Find})
	 * @param returnMask mask specifying what information is to be returned on each entry ({link ReadMask})
	 * @param keys lookup keys
	 * @return lookup result
	 */
	public NotesViewLookupResultData findByKeyExtended2(EnumSet<Find> findFlags, EnumSet<ReadMask> returnMask, Object... keys) {
		checkHandle();
		
		if (keys==null || keys.length==0)
			throw new IllegalArgumentException("No search keys specified");
		
//		if (!canUseOptimizedLookupForKeyLookup(findFlags, returnMask, keys)) {
//			throw new UnsupportedOperationException("This method cannot be used for the specified arguments (only noteids) or the current platform (only R9 and above)");
//		}
		
		IntByReference retNumMatches = new IntByReference();
		NotesCollectionPositionStruct retIndexPos = NotesCollectionPositionStruct.newInstance();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		int findFlagsBitMask = Find.toBitMaskInt(findFlags);
		short result;
		int returnMaskBitMask = ReadMask.toBitMask(returnMask);
		
		ShortByReference retSignalFlags = new ShortByReference();
		
		if (NotesJNAContext.is64Bit()) {
			Memory keyBuffer;
			try {
				keyBuffer = NotesSearchKeyEncoder.b64_encodeKeys(keys);
			} catch (Throwable e) {
				throw new NotesError(0, "Could not encode search keys", e);
			}
			
			LongByReference retBuffer = new LongByReference();
			IntByReference retSequence = new IntByReference();
			
			result = notesAPI.b64_NIFFindByKeyExtended2(m_hCollection64, keyBuffer, findFlagsBitMask, returnMaskBitMask, retIndexPos, retNumMatches, retSignalFlags, retBuffer, retSequence);
			
			if (result == 1028 || result == 17412) {
				return new NotesViewLookupResultData(null, new ArrayList<NotesViewEntryData>(0), 0, 0, retSignalFlags.getValue(), null, retSequence.getValue(), null);
			}
			NotesErrorUtils.checkResult(result);

			if (retNumMatches.getValue()==0) {
				return new NotesViewLookupResultData(null, new ArrayList<NotesViewEntryData>(0), 0, 0, retSignalFlags.getValue(), null, retSequence.getValue(), null);
			}
			else {
				if (retBuffer.getValue()==0) {
					return new NotesViewLookupResultData(null, new ArrayList<NotesViewEntryData>(0), 0, retNumMatches.getValue(), retSignalFlags.getValue(), retIndexPos.toPosString(), retSequence.getValue(), null);
				}
				else {
					boolean convertStringsLazily = true;
					NotesViewLookupResultData viewData = NotesLookupResultBufferDecoder.b64_decodeCollectionLookupResultBuffer(this, retBuffer.getValue(),
							0, retNumMatches.getValue(), returnMask, retSignalFlags.getValue(), retIndexPos.toPosString(), retSequence.getValue(), null,
							convertStringsLazily, null);
					return viewData;
				}
			}
		}
		else {
			Memory keyBuffer;
			try {
				keyBuffer = NotesSearchKeyEncoder.b32_encodeKeys(keys);
			} catch (Throwable e) {
				throw new NotesError(0, "Could not encode search keys", e);
			}
			
			IntByReference retBuffer = new IntByReference();
			IntByReference retSequence = new IntByReference();
			
			result = notesAPI.b32_NIFFindByKeyExtended2(m_hCollection32, keyBuffer, findFlagsBitMask, returnMaskBitMask, retIndexPos, retNumMatches, retSignalFlags, retBuffer, retSequence);
			if (result == 1028 || result == 17412) {
				return new NotesViewLookupResultData(null, new ArrayList<NotesViewEntryData>(0), 0, 0, retSignalFlags.getValue(), null, retSequence.getValue(), null);
			}
			NotesErrorUtils.checkResult(result);

			if (retNumMatches.getValue()==0) {
				return new NotesViewLookupResultData(null, new ArrayList<NotesViewEntryData>(0), 0, 0, retSignalFlags.getValue(), null, retSequence.getValue(), null);
			}
			else {
				if (retBuffer.getValue()==0) {
					return new NotesViewLookupResultData(null, new ArrayList<NotesViewEntryData>(0), 0, retNumMatches.getValue(), retSignalFlags.getValue(), retIndexPos.toPosString(), retSequence.getValue(), null);
				}
				else {
					boolean convertStringsLazily = true;
					NotesViewLookupResultData viewData = NotesLookupResultBufferDecoder.b32_decodeCollectionLookupResultBuffer(this, retBuffer.getValue(),
							0, retNumMatches.getValue(), returnMask, retSignalFlags.getValue(), retIndexPos.toPosString(), retSequence.getValue(), null,
							convertStringsLazily, null);
					return viewData;
				}
			}
		}
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
	 * This function yields the {@link NotesCollectionPosition} of the first note in the
	 * collection that matches the keys. It also yields a count of the number
	 * of notes that match the keys. Since all notes that match the keys
	 * appear contiguously in the view or folder, you may pass the resulting
	 * {@link NotesCollectionPosition} and match count as inputs to
	 * {@link #readEntries(NotesCollectionPosition, EnumSet, int, EnumSet, int, EnumSet)}
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
	 * condition (use {@link #canFindExactNumberOfMatches(EnumSet)} to check
	 * whether a combination of find flags can return the exact number of matches).<br>
	 * If we cannot determine the number of notes, the function will return 1 for the count
	 * value returned by {@link FindResult#getEntriesFound()}.

	 * @param findFlags {@link Find}
	 * @param keys lookup keys, can be {@link String}, double / {@link Double}, int / {@link Integer}, {@link Date}, {@link Calendar}, {@link Date}[] or {@link Calendar}[] with two elements for date ranges
	 * @return result
	 */
	public FindResult findByKey(EnumSet<Find> findFlags, Object... keys) {
		checkHandle();
		
		if (keys==null || keys.length==0)
			throw new IllegalArgumentException("No search keys specified");
		
		IntByReference retNumMatches = new IntByReference();
		NotesCollectionPositionStruct retIndexPos = NotesCollectionPositionStruct.newInstance();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short findFlagsBitMask = Find.toBitMask(findFlags);
		short result;
		if (NotesJNAContext.is64Bit()) {
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
	 * This function yields the {@link NotesCollectionPosition} of the first note in the
	 * collection that matches the string. It also yields a count of the number
	 * of notes that match the string.<br>
	 * <br>
	 * With views that are not categorized, all notes with primary sort keys that
	 * match the string appear contiguously in the view or folder.<br>
	 * <br>
	 * This means you may pass the resulting {@link NotesCollectionPosition} and match count
	 * as inputs to {@link #readEntries(NotesCollectionPosition, EnumSet, int, EnumSet, int, EnumSet)}
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
	 * Use {@link #findByKey(EnumSet, Object...)} if the leftmost sorted column
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
	 * condition (use {@link #canFindExactNumberOfMatches(EnumSet)} to check
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
		
		Memory nameLMBCS = NotesStringUtils.toLMBCS(name, true);

		IntByReference retNumMatches = new IntByReference();
		NotesCollectionPositionStruct retIndexPos = NotesCollectionPositionStruct.newInstance();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short findFlagsBitMask = Find.toBitMask(findFlags);
		short result;
		if (NotesJNAContext.is64Bit()) {
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
		
		/**
		 * Creates a new instance
		 * 
		 * @param position position of the first match
		 * @param entriesFound number of entries found or 1 if hasExactNumberOfMatches is <code>false</code>
		 * @param hasExactNumberOfMatches true if Notes was able to count the number of matches (e.g. for string key lookups with full or partial matches)
		 */
		public FindResult(String position, int entriesFound, boolean hasExactNumberOfMatches) {
			m_position = position;
			m_entriesFound = entriesFound;
			m_hasExactNumberOfMatches = hasExactNumberOfMatches;
		}

		/**
		 * Returns the number of entries found or 1 if hasExactNumberOfMatches is <code>false</code>
		 * and any matches were found
		 * 
		 * @return count
		 */
		public int getEntriesFound() {
			return m_entriesFound;
		}

		/**
		 * Returns the position of the first match
		 * 
		 * @return position
		 */
		public String getPosition() {
			return m_position;
		}
		
		/**
		 * Use this method to check whether Notes was able to count the number of matches
		 * (e.g. for string key lookups with full or partial matches)
		 * 
		 * @return true if we have an exact match count
		 */
		public boolean hasExactNumberOfMatches() {
			return m_hasExactNumberOfMatches;
		}
	}

	/**
	 * Returns the number of top level entries in the view
	 * 
	 * @return top level entries
	 */
	public int getTopLevelEntries() {
		NotesViewLookupResultData lkData = readEntries(new NotesCollectionPosition("0"), EnumSet.of(Navigate.CURRENT), 0, EnumSet.of(Navigate.CURRENT), 0, EnumSet.of(ReadMask.COLLECTIONSTATS));
		return lkData.getStats().getTopLevelEntries();
	}
	
	/**
	 * Reads collection entries (using NIFReadEntries method).<br>
	 * <br>
	 * This method provides low-level API access. In general, it is safer to use high-level functions like
	 * {@link #getAllEntries(String, int, EnumSet, int, EnumSet, ViewLookupCallback)} instead because
	 * they handle view index update while reading.
	 * 
	 * @param startPos start position for the scan; will be modified by the method to reflect the current position
	 * @param skipNavigator navigator to use for the skip operation
	 * @param skipCount number of entries to skip
	 * @param returnNavigator navigator to use for the read operation
	 * @param returnCount number of entries to read
	 * @param returnMask bitmask of data to read
	 * @return read data
	 */
	public NotesViewLookupResultData readEntries(NotesCollectionPosition startPos, EnumSet<Navigate> skipNavigator, int skipCount, EnumSet<Navigate> returnNavigator, int returnCount, EnumSet<ReadMask> returnMask) {
		checkHandle();

		IntByReference retNumEntriesSkipped = new IntByReference();
		IntByReference retNumEntriesReturned = new IntByReference();
		ShortByReference retSignalFlags = new ShortByReference();
		ShortByReference retBufferLength = new ShortByReference();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short skipNavBitMask = Navigate.toBitMask(skipNavigator);
		short returnNavBitMask = Navigate.toBitMask(returnNavigator);
		int readMaskBitMask = ReadMask.toBitMask(returnMask);
		
		NotesCollectionPositionStruct startPosStruct = startPos==null ? null : startPos.getAdapter(NotesCollectionPositionStruct.class);
		
		short result;
		if (NotesJNAContext.is64Bit()) {
			LongByReference retBuffer = new LongByReference();
			result = notesAPI.b64_NIFReadEntries(m_hCollection64, // hCollection
					startPosStruct, // IndexPos
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
			
			int indexModifiedSequenceNo = getIndexModifiedSequenceNo();
			
			int iBufLength = (int) (retBufferLength.getValue() & 0xffff);
			if (iBufLength==0) {
				return new NotesViewLookupResultData(null, new ArrayList<NotesViewEntryData>(0), retNumEntriesSkipped.getValue(), retNumEntriesReturned.getValue(), retSignalFlags.getValue(), null, indexModifiedSequenceNo, null);
			}
			else {
				boolean convertStringsLazily = true;
				NotesViewLookupResultData viewData = NotesLookupResultBufferDecoder.b64_decodeCollectionLookupResultBuffer(this, retBuffer.getValue(),
						retNumEntriesSkipped.getValue(), retNumEntriesReturned.getValue(), returnMask, retSignalFlags.getValue(), null,
						indexModifiedSequenceNo, null, convertStringsLazily, null);
				return viewData;
			}
		}
		else {
			IntByReference retBuffer = new IntByReference();
			result = notesAPI.b32_NIFReadEntries(m_hCollection32, // hCollection
					startPosStruct, // IndexPos
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
			
			int indexModifiedSequenceNo = getIndexModifiedSequenceNo();

			int iBufLength = (int) (retBufferLength.getValue() & 0xffff);
			if (iBufLength==0) {
				return new NotesViewLookupResultData(null, new ArrayList<NotesViewEntryData>(0), retNumEntriesSkipped.getValue(), retNumEntriesReturned.getValue(), retSignalFlags.getValue(), null, indexModifiedSequenceNo, null);
			}
			else {
				boolean convertStringsLazily = true;

				NotesViewLookupResultData viewData = NotesLookupResultBufferDecoder.b32_decodeCollectionLookupResultBuffer(this, retBuffer.getValue(),
						retNumEntriesSkipped.getValue(), retNumEntriesReturned.getValue(), returnMask, retSignalFlags.getValue(), null,
						indexModifiedSequenceNo, null, convertStringsLazily, null);
				return viewData;
			}
		}
	}

	/**
	 * Reads collection entries with extended funcionality (using undocumented NIFReadEntriesExt method).<br>
	 * <br>
	 * This method provides low-level API access. In general, it is safer to use high-level functions like
	 * {@link #getAllEntries(String, int, EnumSet, int, EnumSet, ViewLookupCallback)} instead because
	 * they handle view index update while reading.
	 * 
	 * @param startPos start position for the scan; will be modified by the method to reflect the current position
	 * @param skipNavigator navigator to use for the skip operation
	 * @param skipCount number of entries to skip
	 * @param returnNavigator navigator to use for the read operation
	 * @param returnCount number of entries to read
	 * @param returnMask bitmask of data to read
	 * @param diffTime If non-null, this is a "differential view read" meaning that the caller wants
	 * 				us to optimize things by only returning full information for notes which have
	 * 				changed (or are new) in the view, return just NoteIDs for notes which haven't
	 * 				changed since this time and return a deleted ID table for notes which may be
	 * 				known by the caller and have been deleted since DiffTime. <b>Please note that "differential view reads" do only work in views without permutations (no columns with "show multiple values as separate entries" set) according to IBM. Otherwise, all the view data is always returned.</b>
	 * @param diffIDTable If DiffTime is non-null and DiffIDTable is not null it provides a
	 * 				list of notes which the caller has current information on.  We use this to
	 * 				know which notes we can return shortened information for (i.e., just the NoteID)
	 * 				and what notes we might have to include in the returned DelNoteIDTable.
	 * @param columnNumber If not null, number of single column to return value for (0-based)
	 * @return read data
	 */
	public NotesViewLookupResultData readEntriesExt(NotesCollectionPosition startPos,
			EnumSet<Navigate> skipNavigator, int skipCount, EnumSet<Navigate> returnNavigator,
			int returnCount, EnumSet<ReadMask> returnMask, NotesTimeDate diffTime,
			NotesIDTable diffIDTable,
			Integer columnNumber) {
		checkHandle();

		IntByReference retNumEntriesSkipped = new IntByReference();
		IntByReference retNumEntriesReturned = new IntByReference();
		ShortByReference retSignalFlags = new ShortByReference();
		ShortByReference retBufferLength = new ShortByReference();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short skipNavBitMask = Navigate.toBitMask(skipNavigator);
		short returnNavBitMask = Navigate.toBitMask(returnNavigator);
		int readMaskBitMask = ReadMask.toBitMask(returnMask);
		NotesCollectionPositionStruct startPosStruct = startPos==null ? null : startPos.getAdapter(NotesCollectionPositionStruct.class);
		
		int flags = 0;
		
		NotesTimeDateStruct retDiffTimeStruct = NotesTimeDateStruct.newInstance();
		NotesTimeDateStruct retModifiedTimeStruct = NotesTimeDateStruct.newInstance();
		IntByReference retSequence = new IntByReference();

		String singleColumnLookupName = columnNumber == null ? null : getColumnName(columnNumber);
		
		NotesTimeDateStruct diffTimeStruct = diffTime==null ? null : diffTime.getAdapter(NotesTimeDateStruct.class);
		
		short result;
		if (NotesJNAContext.is64Bit()) {
			LongByReference retBuffer = new LongByReference();
			result = notesAPI.b64_NIFReadEntriesExt(m_hCollection64, startPosStruct,
					skipNavBitMask,
					skipCount, returnNavBitMask, returnCount, readMaskBitMask,
					diffTimeStruct, diffIDTable==null ? 0 : diffIDTable.getHandle64(), columnNumber==null ? NotesCAPI.MAXDWORD : columnNumber, flags, retBuffer, retBufferLength,
					retNumEntriesSkipped, retNumEntriesReturned, retSignalFlags,
					retDiffTimeStruct, retModifiedTimeStruct, retSequence);
			
			NotesErrorUtils.checkResult(result);
			
			int indexModifiedSequenceNo = retModifiedTimeStruct.Innards[0]; //getIndexModifiedSequenceNo();
			
			NotesTimeDate retDiffTimeWrap = new NotesTimeDate(retDiffTimeStruct);

			int iBufLength = (int) (retBufferLength.getValue() & 0xffff);
			if (iBufLength==0) {
				return new NotesViewLookupResultData(null, new ArrayList<NotesViewEntryData>(0),
						retNumEntriesSkipped.getValue(), retNumEntriesReturned.getValue(),
						retSignalFlags.getValue(), null, indexModifiedSequenceNo, new NotesTimeDate(retDiffTimeStruct));
			}
			else {
				boolean convertStringsLazily = true;
				NotesViewLookupResultData viewData = NotesLookupResultBufferDecoder.b64_decodeCollectionLookupResultBuffer(this, retBuffer.getValue(),
						retNumEntriesSkipped.getValue(), retNumEntriesReturned.getValue(), returnMask, retSignalFlags.getValue(), null,
						indexModifiedSequenceNo, retDiffTimeWrap, convertStringsLazily, singleColumnLookupName);
				return viewData;
			}
		}
		else {
			IntByReference retBuffer = new IntByReference();
			result = notesAPI.b32_NIFReadEntriesExt(m_hCollection32, startPosStruct,
					skipNavBitMask,
					skipCount, returnNavBitMask, returnCount, readMaskBitMask,
					diffTimeStruct, diffIDTable==null ? 0 : diffIDTable.getHandle32(), columnNumber==null ? NotesCAPI.MAXDWORD : columnNumber, flags, retBuffer, retBufferLength,
					retNumEntriesSkipped, retNumEntriesReturned, retSignalFlags,
					retDiffTimeStruct, retModifiedTimeStruct, retSequence);
			NotesErrorUtils.checkResult(result);
			
			int indexModifiedSequenceNo = retModifiedTimeStruct.Innards[0]; //getIndexModifiedSequenceNo();

			NotesTimeDate retDiffTimeWrap = new NotesTimeDate(retDiffTimeStruct);
			
			if (retBufferLength.getValue()==0) {
				return new NotesViewLookupResultData(null, new ArrayList<NotesViewEntryData>(0),
						retNumEntriesSkipped.getValue(), retNumEntriesReturned.getValue(),
						retSignalFlags.getValue(), null, indexModifiedSequenceNo, retDiffTimeWrap);
			}
			else {
				boolean convertStringsLazily = true;

				NotesViewLookupResultData viewData = NotesLookupResultBufferDecoder.b32_decodeCollectionLookupResultBuffer(this, retBuffer.getValue(),
						retNumEntriesSkipped.getValue(), retNumEntriesReturned.getValue(), returnMask, retSignalFlags.getValue(), null,
						indexModifiedSequenceNo, retDiffTimeWrap, convertStringsLazily, singleColumnLookupName);
				return viewData;
			}
		}
	}

	/**
	 * Updates the view to reflect the current database content (using NIFUpdateCollection method)
	 */
	public void update() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NIFUpdateCollection(m_hCollection64);
		}
		else {
			result = notesAPI.b32_NIFUpdateCollection(m_hCollection32);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Returns the programmatic name of the column that has last been used to resort
	 * the view
	 * 
	 * @return column name or null if view has not been resorted
	 */
	public String getCurrentSortColumnName() {
		short collation = getCollation();
		if (collation==0)
			return null;
		
		CollationInfo colInfo = getCollationsInfo();
		return colInfo.getSortItem(collation);
	}
	
	/**
	 * Returns the sort direction that has last been used to resort the view
	 * 
	 * @return direction or null
	 */
	public Direction getCurrentSortDirection() {
		short collation = getCollation();
		if (collation==0)
			return null;
		CollationInfo colInfo = getCollationsInfo();
		return colInfo.getSortDirection(collation);
	}
	
	/**
	 * Returns the currently active collation
	 * 
	 * @return collation
	 */
	private short getCollation() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		ShortByReference retCollationNum = new ShortByReference();
		
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NIFGetCollation(m_hCollection64, retCollationNum);
		}
		else {
			result = notesAPI.b32_NIFGetCollation(m_hCollection32, retCollationNum);
		}
		NotesErrorUtils.checkResult(result);
		return retCollationNum.getValue();
	}
	
	/**
	 * Changes the collation to sort the collection by the specified column and direction
	 * 
	 * @param progColumnName programmatic column name
	 * @param direction sort direction
	 */
	public void resortView(String progColumnName, Direction direction) {
		short collation = findCollation(progColumnName, direction);
		if (collation==-1) {
			throw new NotesError(0, "Column "+progColumnName+" does not exist in view "+getName()+" or is not sortable in "+direction+" direction");
		}
		setCollation(collation);
	}
	
	/**
	 * Resets the view sorting to the default (collation=0). Only needs to be called if
	 * view had been resorted via {@link #resortView(String, Direction)}
	 */
	public void resetViewSortingToDefault() {
		setCollation((short) 0);
	}
	
	/**
	 * Sets the active collation (collection column sorting)
	 * 
	 * @param collation collation
	 */
	private void setCollation(short collation) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NIFSetCollation(m_hCollection64, collation);
		}
		else {
			result = notesAPI.b32_NIFSetCollation(m_hCollection32, collation);
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * Returns programmatic names and sorting of sortable columns
	 * 
	 * @return info object with collation info
	 */
	private CollationInfo getCollationsInfo() {
		if (m_collationInfo==null) {
			scanColumns();
		}
		return m_collationInfo;
	}
	
	/**
	 * Returns an iterator of all available columns for which we can read column values
	 * (e.g. does not return static column names)
	 * 
	 * @return programmatic column names converted to lowercase in the order they appear in the view
	 */
	public Iterator<String> getColumnNames() {
		if (m_columnIndicesByItemName==null) {
			scanColumns();
		}
		return m_columnIndicesByItemName.keySet().iterator();
	}
	
	/**
	 * Returns the programmatic column name for a column index
	 * 
	 * @param index index
	 * @return column name or null if index is unknown / invalid
	 */
	public String getColumnName(int index) {
		if (m_columnNamesByIndex==null) {
			scanColumns();
		}
		String colName = m_columnNamesByIndex.get(index);
		return colName;
	}
	
	/**
	 * Returns the number of columns for which we can read column data (e.g. does not count columns
	 * with static values)
	 * 
	 * @return number of columns
	 */
	public int getNumberOfColumns() {
		if (m_columnIndicesByItemName==null) {
			scanColumns();
		}
		return m_columnIndicesByItemName.size();
	}
	
	/**
	 * Returns the column title for a column
	 * 
	 * @param columnIndex column index
	 * @return title
	 */
	public String getColumnTitle(int columnIndex) {
		if (m_columnTitlesByIndex==null) {
			scanColumns();
		}
		return m_columnTitlesByIndex.get(columnIndex);
	}

	private void scanColumns() {
		scanColumnsNew();
	}

	/**
	 * New method to read information about view columns and sortings using C methods
	 */
	private void scanColumnsNew() {
		m_columnIndicesByItemName = new LinkedHashMap<String, Integer>();
		m_columnIndicesByTitle = new LinkedHashMap<String, Integer>();
		m_columnNamesByIndex = new TreeMap<Integer, String>();
		m_columnIsCategoryByIndex = new TreeMap<Integer, Boolean>();
		m_columnTitlesLCByIndex = new TreeMap<Integer, String>();
		m_columnTitlesByIndex = new TreeMap<Integer, String>();

		m_viewNote = m_parentDb.openNoteByUnid(m_viewUNID);
		
		//read collations
		CollationInfo collationInfo = new CollationInfo();

		int colNo = 0;
		boolean readCollations = false;
		
		while (m_viewNote.hasItem("$Collation"+(colNo==0 ? "" : colNo))) {
			List<Object> collationInfoList = m_viewNote.getItemValue("$Collation"+(colNo==0 ? "" : colNo));
			if (collationInfoList!=null && !collationInfoList.isEmpty()) {
				readCollations = true;
				
				NotesCollationInfo colInfo = (NotesCollationInfo) collationInfoList.get(0);
				
				List<NotesCollateDescriptor> collateDescList = colInfo.getDescriptors();
				NotesCollateDescriptor firstCollateDesc = collateDescList.get(0);
				String currItemName = firstCollateDesc.getName();
				Direction currDirection = firstCollateDesc.getDirection();
				
				collationInfo.addCollation((short) colNo, currItemName, currDirection);
			}
			colNo++;
		}
		
		m_collationInfo = collationInfo;

		if (!readCollations) {
			throw new AssertionError("View note with UNID "+m_viewUNID+" contains collations");
		}
		
		
		//read view columns
		List<Object> viewFormatList = m_viewNote.getItemValue("$VIEWFORMAT");
		if (!(viewFormatList!=null && !viewFormatList.isEmpty()))
			throw new AssertionError("View note with UNID "+m_viewUNID+" has item $VIEWFORMAT");
		
		NotesViewFormat format = (NotesViewFormat) viewFormatList.get(0);
		m_viewFormat = format;
		List<NotesViewColumn> columns = format.getColumns();
		
		for (int i=0; i<columns.size(); i++) {
			NotesViewColumn currCol = columns.get(i);
			String currItemName = currCol.getItemName();
			String currItemNameLC = currItemName.toLowerCase(Locale.ENGLISH);
			String currTitle = currCol.getTitle();
			String currTitleLC = currTitle.toLowerCase(Locale.ENGLISH);
			
			int currColumnValuesIndex = currCol.getColumnValuesIndex();
			
			m_columnIndicesByItemName.put(currItemNameLC, currColumnValuesIndex);
			m_columnIndicesByTitle.put(currTitleLC, currColumnValuesIndex);

			if (!currCol.isConstant()) {
				m_columnNamesByIndex.put(currColumnValuesIndex, currItemNameLC);
				m_columnTitlesLCByIndex.put(currColumnValuesIndex, currTitleLC);
				m_columnTitlesByIndex.put(currColumnValuesIndex, currTitle);
				
				boolean isCategory = currCol.isCategory();
				m_columnIsCategoryByIndex.put(currColumnValuesIndex, isCategory);
			}
		}
	}
	
	/**
	 * Returns design information about the view
	 * 
	 * @return view columns
	 */
	public List<NotesViewColumn> getColumns() {
		if (m_viewFormat==null) {
			scanColumnsNew();
		}
		return Collections.unmodifiableList(m_viewFormat.getColumns());
	}
	
	/**
	 * Old (unused) method to read information about view columns and sortings using the legacy API
	 */
	private void scanColumnsOld() {
		m_columnIndicesByItemName = new LinkedHashMap<String, Integer>();
		m_columnIndicesByTitle = new LinkedHashMap<String, Integer>();
		m_columnNamesByIndex = new TreeMap<Integer, String>();
		m_columnIsCategoryByIndex = new TreeMap<Integer, Boolean>();
		m_columnTitlesLCByIndex = new TreeMap<Integer, String>();
		m_columnTitlesByIndex = new TreeMap<Integer, String>();
		
		try {
			//TODO implement this in pure JNA code
			Database db = m_parentDb.getSession().getDatabase(m_parentDb.getServer(), m_parentDb.getRelativeFilePath());
			View view = db.getView(getName());
			if (view==null) {
				throw new NotesError(0, "View "+getName()+" not found using legacy API");
			}

			CollationInfo collationInfo = new CollationInfo();

			Vector<?> columns = view.getColumns();
			try {
				short collation = 1;
				for (int i=0; i<columns.size(); i++) {
					ViewColumn currCol = (ViewColumn) columns.get(i);
					
					String currItemName = currCol.getItemName();
					String currItemNameLC = currItemName.toLowerCase();
					
					String currTitle = currCol.getTitle();
					String currTitleLC = currTitle.toLowerCase();
					
					int currColumnValuesIndex = currCol.getColumnValuesIndex();
					m_columnIndicesByItemName.put(currItemNameLC, currColumnValuesIndex);
					m_columnIndicesByTitle.put(currTitleLC, currColumnValuesIndex);
					
					if (currColumnValuesIndex != ViewColumn.VC_NOT_PRESENT) {
						m_columnNamesByIndex.put(currColumnValuesIndex, currItemNameLC);
						m_columnTitlesLCByIndex.put(currColumnValuesIndex, currTitleLC);
						m_columnTitlesByIndex.put(currColumnValuesIndex, currTitle);
						
						boolean isCategory = currCol.isCategory();
						m_columnIsCategoryByIndex.put(currColumnValuesIndex, isCategory);
					}
					
					boolean isResortAscending = currCol.isResortAscending();
					boolean isResortDescending = currCol.isResortDescending();
					
					if (isResortAscending || isResortDescending) {

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

				m_collationInfo = collationInfo;
			}
			finally {
				view.recycle(columns);
			}
		}
		catch (Throwable t) {
			throw new NotesError(0, "Could not read collation information for view "+getName(), t);
		}
	}
	
	/**
	 * Container class with view collation information (collation index vs. sort item name and sort direction)
	 * 
	 * @author Karsten Lehmann
	 */
	private static class CollationInfo {
		private Map<String,Short> m_ascendingLookup;
		private Map<String,Short> m_descendingLookup;
		private Map<Short,String> m_collationSortItem;
		private Map<Short,Direction> m_collationSorting;
		private int m_nrOfCollations;
		
		/**
		 * Creates a new instance
		 */
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
			String itemNameLC = itemName.toLowerCase();
			if (direction == Direction.Ascending) {
				m_ascendingLookup.put(itemNameLC, Short.valueOf(collation));
			}
			else if (direction == Direction.Descending) {
				m_descendingLookup.put(itemNameLC, Short.valueOf(collation));
			}
			m_nrOfCollations = Math.max(m_nrOfCollations, collation);
			m_collationSorting.put(collation, direction);
			m_collationSortItem.put(collation, itemNameLC);
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
			String itemNameLC = sortItem.toLowerCase();
			if (direction==Direction.Ascending) {
				Short collation = m_ascendingLookup.get(itemNameLC);
				return collation==null ? -1 : collation.shortValue();
			}
			else {
				Short collation = m_descendingLookup.get(itemNameLC);
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
			
			String sortItem = m_collationSortItem.get(Short.valueOf((short)collation));
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
			
			Direction direction = m_collationSorting.get(Short.valueOf((short)collation));
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
	 */
	private short findCollation(String columnName, Direction direction) {
		return getCollationsInfo().findCollation(columnName, direction);
	}
	
	/**
	 * Method to check if a note is visible in a view for the current user
	 * 
	 * @param noteId note id
	 * @return true if visible
	 */
	public boolean isNoteInView(int noteId) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		
		IntByReference retIsInView = new IntByReference();
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NIFIsNoteInView(m_hCollection64, noteId, retIsInView);
		}
		else {
			result = notesAPI.b32_NIFIsNoteInView(m_hCollection32, noteId, retIsInView);
		}
		NotesErrorUtils.checkResult(result);
		boolean isInView = retIsInView.getValue()==1;
		return isInView;
	}
	
	/**
	 * Method to check whether a view is time variant and has to be rebuilt on each db open
	 * 
	 * @return true if time variant
	 */
	public boolean isTimeVariantView() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		if (NotesJNAContext.is64Bit()) {
			return notesAPI.b64_NIFIsTimeVariantView(m_hCollection64);
		}
		else {
			return notesAPI.b32_NIFIsTimeVariantView(m_hCollection32);
		}
	}
	
	/**
	 * Method to check if the view index is up to date or if any note has changed in
	 * the database since the last view index update.
	 * 
	 * @return true if up to date
	 */
	public boolean isUpToDate() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			return notesAPI.b64_NIFCollectionUpToDate(m_hCollection64);
		}
		else {
			return notesAPI.b32_NIFCollectionUpToDate(m_hCollection32);
		}
	}

	/**
	 * Check if the collection is currently being updated
	 * 
	 * @return true if being updated
	 */
	public boolean isUpdateInProgress() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			return notesAPI.b64_NIFIsUpdateInProgress(m_hCollection64);
		}
		else {
			return notesAPI.b32_NIFIsUpdateInProgress(m_hCollection32);
		}
	}
	
	/**
	 * Notify of modification to per-user index filters<br>
	 * <br>
	 * This routine must be called by application code when it makes changes
	 * to any of the index filters (unread list, expand/collapse list,
	 * selected list).  No handles to the lists are necessary as input
	 * to this function, since the collection context block remembers the
	 * filter handles that were originally specified to the OpenCollection
	 * call.<br>
	 * If the collection is open on a remote server, then the newly
	 * modified lists are re-sent over to the server. If the collection
	 * is "local", then nothing is done.
	 * 
	 * @param flags Flags indicating which filters were modified
	 */
	public void updateFilters(EnumSet<UpdateCollectionFilters> flags) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		short flagsBitmask = UpdateCollectionFilters.toBitMask(flags);
		
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NIFUpdateFilters(m_hCollection64, flagsBitmask);
			
		}
		else {
			result = notesAPI.b32_NIFUpdateFilters(m_hCollection32, flagsBitmask);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Unfinished alternative lookup method
	 * 
	 * @param column first column to return
	 * @param columns other columns to return
	 * @deprecated not ready for prime time, does nothing
	 * @return selection
	 */
	public Selection select(String column, String... columns) {
		List<String> columnsList = new ArrayList<String>();
		columnsList.add(column);
		
		if (columns!=null) {
			for (String currCol : columns) {
				columnsList.add(currCol);
			}
		}
	
		String[] columnsArr = columnsList.toArray(new String[columnsList.size()]);
		return new Selection(columnsArr);
	}

	@Override
	public String toString() {
		if (isRecycled()) {
			return "NotesCollection [recycled]";
		}
		else {
			return "NotesCollection [handle="+(NotesJNAContext.is64Bit() ? m_hCollection64 : m_hCollection32)+", noteid="+getNoteId()+"]";
		}
	}

	/**
	 * Resets the selected list ID table
	 */
	public void clearSelection() {
		m_selectedList.clear();
	}
	
	/**
	 * This method adds a list of note ids to the selected list
	 * 
	 * @param noteIds note ids to add
	 * @param clearPrevSelection true to clear the previous selection
	 */
	public void select(Collection<Integer> noteIds, boolean clearPrevSelection) {
		NotesIDTable selectedList = getSelectedList();
		
		if (clearPrevSelection) {
			selectedList.clear();
		}
		selectedList.addNotes(noteIds);
		
		//push selection changes to remote servers
		updateFilters(EnumSet.of(UpdateCollectionFilters.FILTER_SELECTED));
	}
	
	/**
	 * This function runs a selection formula on every document of this collection.
	 * Documents matching the selection formula get added to the selected list.<br>
	 * After calling this method, the selected documents can then be read via
	 * {@link #getAllEntries(String, int, EnumSet, int, EnumSet, ViewLookupCallback)}
	 * with the navigator {@link Navigate#NEXT_SELECTED}.
	 * 
	 * @param formula selection formula, e.g. SELECT form="Person"
	 * @param clearPrevSelection true to clear the previous selection
	 */
	public void select(String formula, boolean clearPrevSelection) {
		NotesIDTable idTable = new NotesIDTable();
		try {
			//collect all ids of this collection
			getAllIds(Navigate.NEXT_NONCATEGORY, true, idTable);
			
			final Set<Integer> retIds = new TreeSet<Integer>();
			
			NotesSearch.search(m_parentDb, idTable, formula, "-",
					EnumSet.of(Search.SESSION_USERNAME), EnumSet.of(NoteClass.DOCUMENT), null, new NotesSearch.ISearchCallback() {
						
						@Override
						public void noteFound(NotesDatabase parentDb, int noteId, EnumSet<NoteClass> noteClass, NotesTimeDate dbCreated,
								NotesTimeDate noteModified, ItemTableData summaryBufferData) {
							retIds.add(noteId);
						}
					});
			
			NotesIDTable selectedList = getSelectedList();
			if (clearPrevSelection) {
				selectedList.clear();
			}
			
			if (!retIds.isEmpty())
				selectedList.addNotes(retIds);
			
			//push selection changes to remote servers
			updateFilters(EnumSet.of(UpdateCollectionFilters.FILTER_SELECTED));
		}
		finally {
			idTable.recycle();
		}
	}
	
	/**
	 * Returns the total number of documents in the view
	 * 
	 * @return document count
	 */
	public int getDocumentCount() {
		return getCollectionData().getDocCount();
	}
	
	/**
	 * Retrieve detailed information about the collection itself, such as the number of documents
	 * in the collection and the total size of the document entries in the collection.<br>
	 * This function returns a {@link NotesCollectionData} object.<br>
	 * 
	 * It is only useful for providing information about a collection which is not categorized.<br>
	 * The index of a categorized collection is implemented using nested B-Trees, and it is not possible to provide
	 * useful information about all of the B-Trees which make up the index.<br>
	 * If this method is called for a categorized collection, the data that is returned pertains
	 * only to the top-level category entries, and not to the main notes in the collection.
	 * 
	 * @return collection data
	 */
	public NotesCollectionData getCollectionData() {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		NotesCollectionDataStruct struct;
		ItemValueTableData[] itemValueTables = new ItemValueTableData[NotesCAPI.PERCENTILE_COUNT];
		
		short result;
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethCollData = new LongByReference();
			result = notesAPI.b64_NIFGetCollectionData(m_hCollection64, rethCollData);
			NotesErrorUtils.checkResult(result);
			
			long hCollData = rethCollData.getValue();
			Pointer ptrCollectionData = notesAPI.b64_OSLockObject(hCollData);
			try {
				struct = NotesCollectionDataStruct.newInstance(ptrCollectionData);
				struct.read();
				
				int gmtOffset = NotesDateTimeUtils.getGMTOffset();
				boolean useDayLight = NotesDateTimeUtils.isDaylightTime();
				boolean convertStringsLazily = false;
				
				for (int i=0; i<NotesCAPI.PERCENTILE_COUNT; i++) {
					Pointer ptrItemTable = ptrCollectionData.share(struct.keyOffset[i]);
					itemValueTables[i] = NotesLookupResultBufferDecoder.decodeItemValueTable(ptrItemTable, gmtOffset, useDayLight, convertStringsLazily);
				}
				
				NotesCollectionData data = new NotesCollectionData(struct.docCount, struct.docTotalSize,
						struct.btreeLeafNodes, struct.btreeDepth, itemValueTables);
				return data;
			}
			finally {
				notesAPI.b64_OSUnlockObject(hCollData);
				notesAPI.b64_OSMemFree(hCollData);
			}
		}
		else {
			IntByReference rethCollData = new IntByReference();
			result = notesAPI.b32_NIFGetCollectionData(m_hCollection32, rethCollData);
			NotesErrorUtils.checkResult(result);
			
			int hCollData = rethCollData.getValue();
			Pointer ptrCollectionData = notesAPI.b32_OSLockObject(hCollData);
			try {
				struct = NotesCollectionDataStruct.newInstance(ptrCollectionData);
				struct.read();
				
				int gmtOffset = NotesDateTimeUtils.getGMTOffset();
				boolean useDayLight = NotesDateTimeUtils.isDaylightTime();
				boolean convertStringsLazily = false;
				
				for (int i=0; i<NotesCAPI.PERCENTILE_COUNT; i++) {
					Pointer ptrItemTable = ptrCollectionData.share(struct.keyOffset[i]);
					itemValueTables[i] = NotesLookupResultBufferDecoder.decodeItemValueTable(ptrItemTable, gmtOffset, useDayLight, convertStringsLazily);
				}
				
				NotesCollectionData data = new NotesCollectionData(struct.docCount, struct.docTotalSize,
						struct.btreeLeafNodes, struct.btreeDepth, itemValueTables);
				return data;
			}
			finally {
				notesAPI.b32_OSUnlockObject(hCollData);
				notesAPI.b32_OSMemFree(hCollData);
			}
		}
	}
}
