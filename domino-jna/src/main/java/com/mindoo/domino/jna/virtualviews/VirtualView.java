package com.mindoo.domino.jna.virtualviews;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.TypedItemAccess;
import com.mindoo.domino.jna.virtualviews.VirtualViewColumn.ColumnSort;
import com.mindoo.domino.jna.virtualviews.VirtualViewDataChange.EntryData;

/**
 * This class represents a virtual view that is built from data changes
 * provided by different data providers. The view is built from a list of
 * {@link VirtualViewColumn} objects that define the columns of the view.
 * The columns can be categorized and sorted.
 */
public class VirtualView {
	static final String ORIGIN_VIRTUALVIEW = "virtualview";
	
	private VirtualViewEntry rootEntry;
	private int rootEntryNoteId;
	
	private List<VirtualViewColumn> columns;
	private List<VirtualViewColumn> categoryColumns;
	private List<VirtualViewColumn> sortColumns;
	private List<VirtualViewColumn> valueFunctionColumns;
	private boolean[] docOrderDescending;
	
	/** contains the occurences of a note id in the view */
	private Map<ScopedNoteId,List<VirtualViewEntry>> entriesByNoteId;
	
	/** during a view update, we use this map to remember which sibiling indexes to recompute */
	private Map<ScopedNoteId,List<VirtualViewEntry>> pendingSiblingIndexFlush = new ConcurrentHashMap<>();
	/** lock to coordinate r/w access on the view */
	private ReadWriteLock viewChangeLock = new ReentrantReadWriteLock();
	
	
	public VirtualView(VirtualViewColumn...columnsParam ) {
		this(Arrays.asList(columnsParam));
	}
	
	public VirtualView(List<VirtualViewColumn> columnsParam) {
		this.columns = new ArrayList<>();
		this.categoryColumns = new ArrayList<>();
		this.sortColumns = new ArrayList<>();
		this.valueFunctionColumns = new ArrayList<>();
		
		//split columns into category and sort columns
		for (VirtualViewColumn currColumn : columnsParam) {
			columns.add(currColumn);
			
			if (currColumn.isCategory()) {
				this.categoryColumns.add(currColumn);
			}
			
			if (!currColumn.isCategory() && currColumn.getSorting() != VirtualViewColumn.ColumnSort.NONE) {
				this.sortColumns.add(currColumn);
			}

			if (currColumn.getValueFunction() != null) {
				this.valueFunctionColumns.add(currColumn);
			}
		}

		docOrderDescending = new boolean[sortColumns.size()];
		for (int i = 0; i < docOrderDescending.length; i++) {
			docOrderDescending[i] = sortColumns.get(i).getSorting() == VirtualViewColumn.ColumnSort.DESCENDING;
		}
		
		ViewEntrySortKeyComparator rootChildEntryComparator;
		if (this.categoryColumns.isEmpty()) {
			rootChildEntryComparator = new ViewEntrySortKeyComparator(
					false, this.docOrderDescending);
		}
		else {
			rootChildEntryComparator = new ViewEntrySortKeyComparator(
					this.categoryColumns.get(0).getSorting() == ColumnSort.DESCENDING, this.docOrderDescending);
		}
		
		ViewEntrySortKey rootSortKey = ViewEntrySortKey.createSortKey(true, Collections.emptyList(), ORIGIN_VIRTUALVIEW, 0);
		this.rootEntryNoteId = createNewCategoryNoteId();
		this.rootEntry = new VirtualViewEntry(this, null, ORIGIN_VIRTUALVIEW,
				rootEntryNoteId, "", rootSortKey, rootChildEntryComparator);
		this.rootEntry.setSiblingIndex(0);
		
		this.entriesByNoteId = new ConcurrentHashMap<>();
	}
	
	private AtomicLong categoryNoteId = new AtomicLong(4);

	private int createNewCategoryNoteId() {
		int newId = (int) categoryNoteId.addAndGet(4);
		return (int) ((NotesConstants.RRV_DELETED | newId) & 0xFFFFFFFF);
	}
	
	public List<VirtualViewColumn> getColumns() {
		return columns;
	}
	
	/**
	 * Override this method to not add certain entries to the view
	 * 
	 * @param origin origin of entry
	 * @param noteId note id of the document
	 * @param unid UNID of the document
	 * @param columnValues column values of the document
	 * @return true to add the entry, false to skip it (true by default)
	 */
	protected boolean isAccepted(String origin, int noteId, String unid,
			Map<String, Object> columnValues) {
		return true;
	}
	
	/**
	 * Modifies the view structure based on data changes. The method uses
	 * a write lock to ensure that it is not called concurrently.
	 * 
	 * @param change data change
	 */
	public void applyChanges(VirtualViewDataChange change) {
		viewChangeLock.writeLock().lock();
		try {
			String origin = change.getOrigin();
			List<VirtualViewEntry> categoryEntriesToCheck = new ArrayList<>();
			
			//apply removals
			
			for (int currNoteId : change.getRemovals()) {
				ScopedNoteId scopedNoteId = new ScopedNoteId(origin, currNoteId);
				List<VirtualViewEntry> entries = entriesByNoteId.remove(scopedNoteId);
				if (entries != null) {
					for (VirtualViewEntry currEntry : entries) {
						if (ORIGIN_VIRTUALVIEW.equals(currEntry.getOrigin())) {
							// don't remove our own entries
							continue;
						}
						
						VirtualViewEntry parentEntry = currEntry.getParent();
						if (parentEntry.getChildEntries().remove(currEntry.getSortKey()) != null) {
						    parentEntry.childCount.decrementAndGet();
						    if (currEntry.isCategory()) {
						    	parentEntry.childCategoryCount.decrementAndGet();
						    }
						    else if (currEntry.isDocument()) {
						    	parentEntry.childDocumentCount.decrementAndGet();
						    }
						    //remember to assign new sibling indexes
						    markEntryForSiblingIndexFlush(parentEntry);
						}
						
						if (parentEntry.isCategory()) {
							//check later if this category is now empty
							categoryEntriesToCheck.add(parentEntry);
						}
					}
				}
			}
			
			//apply additions
			
			for (Entry<Integer,EntryData> currEntry : change.getAdditions().entrySet()) {
				int currNoteId = currEntry.getKey();
				EntryData currData = currEntry.getValue();
				String unid = currData.getUnid();
				Map<String,Object> columnValues = currData.getValues();

				List<VirtualViewEntry> addedViewEntries = addEntry(origin, currNoteId, unid, columnValues,
						rootEntry, this.categoryColumns, true);
				ScopedNoteId scopedNoteId = new ScopedNoteId(origin, currNoteId);
                entriesByNoteId.put(scopedNoteId, addedViewEntries);
			}
			
			//clean up category entries that are now empty
			
			for (VirtualViewEntry currCategoryEntry : categoryEntriesToCheck) {
				if (currCategoryEntry.getChildEntries().isEmpty()) {
					removeChildFromParent(currCategoryEntry);
				}
			}
			
			//assign new sibling indexes
			processPendingSiblingIndexUpdates();
			
		} finally {
			viewChangeLock.writeLock().unlock();
		}
	}
	
	/**
	 * Marks an entry to be reprocessed for sibling index assignment so that its children have an ascending sibling index
	 * starting from 1
	 * 
	 * @param ve entry to reprocess
	 */
	private void markEntryForSiblingIndexFlush(VirtualViewEntry ve) {
		List<VirtualViewEntry> entries = pendingSiblingIndexFlush.get(new ScopedNoteId(ve.getOrigin(), ve.getNoteId()));
		if (entries == null) {
			entries = new ArrayList<>();
			pendingSiblingIndexFlush.put(new ScopedNoteId(ve.getOrigin(), ve.getNoteId()), entries);
		}
		if (!entries.contains(ve)) {
			entries.add(ve);
		}
	}

	/**
	 * Assigns new sibling indexes to entries that were marked for reprocessing
	 */
	private void processPendingSiblingIndexUpdates() {
		for (Entry<ScopedNoteId, List<VirtualViewEntry>> currMapEntry : pendingSiblingIndexFlush.entrySet()) {
			for (VirtualViewEntry currViewEntry : currMapEntry.getValue()) {
				int[] pos = new int[] {1};
				
				if (currViewEntry.getChildCount() > 0) {
					currViewEntry.getChildEntries().entrySet().forEach((currChild) -> {
						currChild.getValue().setSiblingIndex(pos[0]++);
					});					
				}
			}
		}
		pendingSiblingIndexFlush.clear();
	}
	
	private Object getFirstListValue(Object value) {
		if (value instanceof List) {
			List<?> valueList = (List<?>) value;
			if (valueList.isEmpty()) {
				return null;
			} else {
				return valueList.get(0);
			}
		} else {
			return value;
		}
	}
	
	/**
	 * Adds a new entry to the view
	 * 
	 * @param origin       origin of the data change (ID of the data provider)
	 * @param noteId       note id of the document
	 * @param unid         UNID of the document
	 * @param columnValues column values of the document, value types: String, Number, NotesTimeDate, List&lt;String&gt;, List&lt;Number&gt, List&lt;NotesTimeDate&gt
	 * @param targetParent parent entry under which the new entry should be added (changed during recursion)
	 * @param remainingCategoryColumns remaining category columns to process (changed during recursion)
	 * @return list of created view entries for the document
	 */
	private List<VirtualViewEntry> addEntry(String origin, int noteId, String unid,
			Map<String, Object> columnValues, VirtualViewEntry targetParent,
			List<VirtualViewColumn> remainingCategoryColumns, boolean firstCall) {
		
		List<VirtualViewEntry> createdChildEntriesForDocument = new ArrayList<>();
		
		//compute additional values provided via function
		for (VirtualViewColumn currValueFunctionColumn : this.valueFunctionColumns) {
			String itemName = currValueFunctionColumn.getItemName();
			Object value = currValueFunctionColumn.getValueFunction().getValue(origin, itemName, new TypedItemAccess() {

				@Override
				public Object get(String itemName) {
					return columnValues.get(itemName);
				}				
			});
			
			columnValues.put(itemName, value);
		}

		if (firstCall && !isAccepted(origin, noteId, unid, columnValues)) {
			return Collections.emptyList();
		}
		
		if (remainingCategoryColumns.isEmpty()) {
			//insert as document entry at the right position under targetParent
			List<Object> docSortValues = new ArrayList<>();
			
			for (VirtualViewColumn currSortColumn : this.sortColumns) {
				String currSortItemName = currSortColumn.getItemName();
				Object currSortValues = columnValues.get(currSortItemName);
				//if sort collumn formulas contain value lists, just sort by the first value
				Object currSortFirstValue = getFirstListValue(currSortValues);
				
				docSortValues.add(currSortFirstValue);
			}
			
			ViewEntrySortKey sortKey = ViewEntrySortKey.createSortKey(false, docSortValues, origin, noteId);

			ViewEntrySortKeyComparator childEntryComparator = new ViewEntrySortKeyComparator(
					false, this.docOrderDescending);

			VirtualViewEntry newChild = new VirtualViewEntry(this,
					targetParent, origin, noteId,
					unid, sortKey,
					childEntryComparator);
			//TODO add support for permuted columns (multiple rows for one doc)

			newChild.setColumnValues(columnValues);
			if (targetParent.getChildEntries().put(sortKey, newChild) == null) {
				targetParent.childCount.incrementAndGet();
				targetParent.childDocumentCount.incrementAndGet();
			}
		    //remember to assign new sibling indexes
			markEntryForSiblingIndexFlush(targetParent);

			createdChildEntriesForDocument.add(newChild);			
			return createdChildEntriesForDocument;
		}
		
		VirtualViewColumn currCategoryColumn = remainingCategoryColumns.get(0);
		List<VirtualViewColumn> remainingColumnsForNextIteration = remainingCategoryColumns.size() == 1 ? Collections.emptyList() : remainingCategoryColumns.subList(1, remainingCategoryColumns.size());
		
		ViewEntrySortKeyComparator childEntryComparator = new ViewEntrySortKeyComparator(
				currCategoryColumn.getSorting() == ColumnSort.DESCENDING, this.docOrderDescending);
		
		String itemName = currCategoryColumn.getItemName();
		Object valuesForColumn = columnValues.get(itemName);
		
		List<Object> multipleCategoryValues;
		
		//special case: empty or null category value
		if (valuesForColumn == null) {
			multipleCategoryValues = Arrays.asList(new Object[] {null} );
		}
		else if ("".equals(valuesForColumn)) {
			multipleCategoryValues = Arrays.asList(new Object[] {null} );
		}
		else if (valuesForColumn instanceof List && ((List) valuesForColumn).isEmpty()) {
			multipleCategoryValues = Arrays.asList(new Object[] {null} );
		}
		else if (valuesForColumn instanceof List &&
				((List) valuesForColumn).size() ==1 &&
				"".equals(((List) valuesForColumn).get(0))) {
			multipleCategoryValues = Arrays.asList(new Object[] {null} );
		}
		
		//make sure this is a list
		if (valuesForColumn instanceof List) {
			multipleCategoryValues = (List<Object>) valuesForColumn;
		} else {
			multipleCategoryValues = Arrays.asList(valuesForColumn);
		}

		//we can insert the document entry in multiple categories:
		for (Object currCategoryValue : multipleCategoryValues) {
			if (currCategoryValue instanceof String && ((String)currCategoryValue).contains("\\")) {
				//special case, span category value across multiple tree levels
				String[] parts = ((String)currCategoryValue).split("\\\\", -1);
				
				VirtualViewEntry currentSubCatParent = targetParent;
				
				for (String currSubCat : parts) {
					Object currSubCatObj = "".equals(currSubCat) ? null : currSubCat;
					
					ViewEntrySortKey categorySortKey = ViewEntrySortKey.createSortKey(true, Arrays.asList(new Object[] { currSubCatObj }),
							ORIGIN_VIRTUALVIEW, 0);
					
					VirtualViewEntry entryWithSortKey = currentSubCatParent.getChildEntries().get(categorySortKey);
					if (entryWithSortKey == null) {
						int newCategoryNoteId = createNewCategoryNoteId();
						entryWithSortKey = new VirtualViewEntry(this, currentSubCatParent, ORIGIN_VIRTUALVIEW,
								newCategoryNoteId, "", categorySortKey,
								childEntryComparator);
						if (currentSubCatParent.getChildEntries().put(categorySortKey, entryWithSortKey) == null) {
							currentSubCatParent.childCount.incrementAndGet();
							currentSubCatParent.childCategoryCount.incrementAndGet();
						}
						entriesByNoteId.put(new ScopedNoteId(ORIGIN_VIRTUALVIEW, newCategoryNoteId), Arrays.asList(entryWithSortKey));
						
					    //remember to assign new sibling indexes
						markEntryForSiblingIndexFlush(currentSubCatParent);
					}
					
					currentSubCatParent = entryWithSortKey;
				}
				
				//go on with the remaining categories
				List<VirtualViewEntry> addedEntries = addEntry(origin, noteId, unid, columnValues, currentSubCatParent,
						remainingColumnsForNextIteration, false);
				createdChildEntriesForDocument.addAll(addedEntries);
			}
			else {
				ViewEntrySortKey categorySortKey = ViewEntrySortKey.createSortKey(true, Arrays.asList(new Object[] { currCategoryValue }),
						ORIGIN_VIRTUALVIEW,
						0);
				
				VirtualViewEntry entryWithSortKey = targetParent.getChildEntries().get(categorySortKey);
				if (entryWithSortKey == null) {
					int newCategoryNoteId = createNewCategoryNoteId();
					entryWithSortKey = new VirtualViewEntry(this, targetParent, ORIGIN_VIRTUALVIEW,
							newCategoryNoteId, "", categorySortKey,
							childEntryComparator);
					if (targetParent.getChildEntries().put(categorySortKey, entryWithSortKey) == null) {
						targetParent.childCount.incrementAndGet();
						targetParent.childCategoryCount.incrementAndGet();
					}
					entriesByNoteId.put(new ScopedNoteId(ORIGIN_VIRTUALVIEW, newCategoryNoteId), Arrays.asList(entryWithSortKey));
					
				    //remember to assign new sibling indexes
					markEntryForSiblingIndexFlush(targetParent);
				}

				//go on with the remaining categories
				List<VirtualViewEntry> addedEntries = addEntry(origin, noteId, unid, columnValues,
						entryWithSortKey,
						remainingColumnsForNextIteration, false);
				createdChildEntriesForDocument.addAll(addedEntries);
			}			
		}
	
		return createdChildEntriesForDocument;
	}

	/**
	 * Removes an entry from the tree structure. If the parent of the entry is a category and
	 * it has no more children, it will be removed too.
	 * 
	 * @param entry entry to remove
	 */
	private void removeChildFromParent(VirtualViewEntry entry) {
		VirtualViewEntry parentEntry = entry.getParent();
		if (parentEntry != null) {
			if (parentEntry.getChildEntries().remove(entry.getSortKey()) != null) {
				parentEntry.childCount.decrementAndGet();
			    //remember to assign new sibling indexes
				markEntryForSiblingIndexFlush(parentEntry);

			    //cleanup entry from selected and expanded entries
			    ScopedNoteId scopedNoteId = new ScopedNoteId(entry.getOrigin(), entry.getNoteId());
			    entriesByNoteId.remove(scopedNoteId);
			}
			
			if (parentEntry.isCategory()) {
				if (parentEntry.getChildEntries().isEmpty()) {
					removeChildFromParent(parentEntry);
				}
			}
		}
	}
	
	/**
	 * Access the view structure with a read lock to ensure that it is not modified
	 * while the code is running.
	 * 
	 * @param runnable code to run
	 */
	public void accessWithReadLock(Runnable runnable) {
		viewChangeLock.readLock().lock();
		try {
			runnable.run();
		} finally {
			viewChangeLock.readLock().unlock();
		}
	}

	/**
	 * Returns the root entry of the view. The root is an artifical entry that
	 * is automatically expanded and contains the top level of the view as
	 * children.
	 * 
	 * @return root entry
	 */
	public VirtualViewEntry getRoot() {
		return rootEntry;
	}

	/**
	 * A note id with an origin to be used as hash key
	 */
	public static class ScopedNoteId {
		private String origin;
		private int noteId;
		private int hashCode;
		
		public ScopedNoteId(String origin, int noteId) {
			this.origin = origin;
			this.noteId = noteId;
			this.hashCode = Objects.hash(noteId, origin);
		}
		
		public String getOrigin() {
			return origin;
		}
		
		public int getNoteId() {
			return noteId;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ScopedNoteId other = (ScopedNoteId) obj;
			return noteId == other.noteId && Objects.equals(origin, other.origin);
		}
		
		@Override
		public String toString() {
			return origin + ":" + noteId;
		}
		
	}
	
	/**
	 * Returns all occurrences of a note id in the view
	 * 
	 * @param origin origin of the entry
	 * @param noteId note id of the entry
	 * @return list of entries
	 */
	public List<VirtualViewEntry> findEntries(String origin, int noteId) {
		return Collections.unmodifiableList(entriesByNoteId.get(new ScopedNoteId(origin, noteId)));
	}
}
