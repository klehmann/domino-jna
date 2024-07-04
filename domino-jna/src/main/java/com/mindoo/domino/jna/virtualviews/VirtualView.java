package com.mindoo.domino.jna.virtualviews;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.mindoo.domino.jna.IViewColumn.ColumnSort;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.TypedItemAccess;
import com.mindoo.domino.jna.utils.IDUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.mindoo.domino.jna.virtualviews.VirtualViewColumn.Total;
import com.mindoo.domino.jna.virtualviews.VirtualViewDataChange.EntryData;
import com.mindoo.domino.jna.virtualviews.VirtualViewNavigator.WithCategories;
import com.mindoo.domino.jna.virtualviews.VirtualViewNavigator.WithDocuments;
import com.mindoo.domino.jna.virtualviews.dataprovider.IVirtualViewDataProvider;
import com.mindoo.domino.jna.virtualviews.security.ViewEntryAccessCheck;

/**
 * This class represents a virtual view that is built from data changes
 * provided by different data providers. The view is built from a list of
 * {@link VirtualViewColumn} objects that define the columns of the view.
 * The columns can be categorized and sorted.
 */
public class VirtualView {
	static final String ORIGIN_VIRTUALVIEW = "virtualview";
	
	/** during a view update, we use this map to remember which sibiling indexes to recompute */
	private Map<ScopedNoteId,List<VirtualViewEntryData>> pendingSiblingIndexFlush = new ConcurrentHashMap<>();
	/** lock to coordinate r/w access on the view */
	private ReadWriteLock viewChangeLock = new ReentrantReadWriteLock();

	//data for serialization

	private VirtualViewEntryData rootEntry;
	private int rootEntryNoteId;
	
	private List<VirtualViewColumn> columns;
	private List<VirtualViewColumn> categoryColumns;
	private List<VirtualViewColumn> sortColumns;
	private List<VirtualViewColumn> totalColumns;
	private List<VirtualViewColumn> valueFunctionColumns;
	private boolean[] docOrderDescending;
	private boolean viewHasTotalColumns;
	private AtomicLong categoryNoteId = new AtomicLong(4);

	/** contains the occurences of a note id in the view */
	private Map<ScopedNoteId,List<VirtualViewEntryData>> entriesByNoteId;
	
	private LinkedHashMap<String, IVirtualViewDataProvider> dataProviderByOrigin;
	
	/**
	 * Creates a new virtual view
	 * 
	 * @param columnsParam columns of the view
	 */
	public VirtualView(VirtualViewColumn...columnsParam ) {
		this(Arrays.asList(columnsParam));
	}
	
	/**
	 * Creates a new virtual view
	 * 
	 * @param columnsParam columns of the view
	 */
	public VirtualView(List<VirtualViewColumn> columnsParam) {
		this.dataProviderByOrigin = new LinkedHashMap<>();
		this.columns = new ArrayList<>();
		this.categoryColumns = new ArrayList<>();
		this.sortColumns = new ArrayList<>();
		this.totalColumns = new ArrayList<>();
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

			if (currColumn.getFunction() != null) {
				this.valueFunctionColumns.add(currColumn);
			}
			
			if (currColumn.getTotalMode() != VirtualViewColumn.Total.NONE) {
				this.totalColumns.add(currColumn);
				viewHasTotalColumns = true;
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
		this.rootEntry = new VirtualViewEntryData(this, null, ORIGIN_VIRTUALVIEW,
				rootEntryNoteId, "", rootSortKey, rootChildEntryComparator);
		this.rootEntry.setColumnValues(new ConcurrentHashMap<>());
		this.rootEntry.setSiblingIndex(0);
		
		this.entriesByNoteId = new ConcurrentHashMap<>();
	}
	
	/**
	 * Adds a data provider to the view
	 * 
	 * @param provider data provider
	 */
	public void addDataProvider(IVirtualViewDataProvider provider) {
		String origin = provider.getOrigin();
		if (this.dataProviderByOrigin.containsKey(origin)) {
			throw new IllegalArgumentException("Data provider with origin '" + origin + "' already added");
		}
		this.dataProviderByOrigin.put(origin, provider);
	}
	
	/**
	 * Returns the data providers
	 * 
	 * @return data providers
	 */
	public Iterator<IVirtualViewDataProvider> getDataProviders() {
		return this.dataProviderByOrigin.values().iterator();
	}
	
	/**
	 * Updates the data for all data providers
	 */
	public void update() {
		for (IVirtualViewDataProvider currProvider : this.dataProviderByOrigin.values()) {
			currProvider.update();
		}
	}
	
	/**
	 * Updates the data for a specific data provider
	 * 
	 * @param origin origin of the data provider
	 */
	public void update(String origin) {
		IVirtualViewDataProvider provider = this.dataProviderByOrigin.get(origin);
		if (provider != null) {
			provider.update();
		}
	}
	
	private int createNewCategoryNoteId() {
		int newId = (int) categoryNoteId.addAndGet(4);
		return (int) ((NotesConstants.RRV_DELETED | newId) & 0xFFFFFFFF);
	}
	
	public List<VirtualViewColumn> getColumns() {
		return columns;
	}
	
	public class VirtualViewNavigatorBuilder {
		private VirtualView view;
		private String effectiveUserName;
		private WithCategories cats = null;
		private WithDocuments docs = null;
		ViewEntryAccessCheck accessCheck;
		
		private VirtualViewNavigatorBuilder(VirtualView view) {
			this.view = view;
		}
		
		/**
		 * By default, both categories and documents are included in the view navigator. If you call this method, only categories will be included.
		 * 
		 * @return builder
		 */
		public VirtualViewNavigatorBuilder withCategories() {
			cats = WithCategories.YES;
			return this;
		}

		/**
		 * By default, both categories and documents are included in the view navigator. If you call this method, only documents will be included.
		 * 
		 * @return builder
		 */
		public VirtualViewNavigatorBuilder withDocuments() {
			docs = WithDocuments.YES;
			return this;
		}
		
		private void fillWithCategoriesAndDocumentsWithDefaults() {
			if (cats == null && docs == null) {
				//if nothing is set, show all entries
				cats = WithCategories.YES;
				docs = WithDocuments.YES;
			}
			if (cats == null) {
				cats = WithCategories.NO;
			}
			if (docs == null) {
				docs = WithDocuments.NO;
			}
		}
		
		/**
		 * Sets the effective user name to use for view entry access checks. By default, the current ID username is used.
		 * 
		 * @param effectiveUserName effective user name
		 * @return builder
		 */
		public VirtualViewNavigatorBuilder withEffectiveUserName(String effectiveUserName) {
			this.effectiveUserName = effectiveUserName;
			return this;
		}
		
		private ViewEntryAccessCheck createAccessCheck() {
			ViewEntryAccessCheck accessCheck = ViewEntryAccessCheck
					.forUser(VirtualView.this, StringUtil.isEmpty(effectiveUserName) ? IDUtils.getIdUsername() : effectiveUserName);
			return accessCheck;
		}
		
		/**
		 * Creates a new view navigator for the whole view
		 * 
		 * @return navigator
		 */
		public VirtualViewNavigator build() {
			fillWithCategoriesAndDocumentsWithDefaults();
			return new VirtualViewNavigator(this.view, this.view.getRoot(), cats, docs, createAccessCheck());
		}
		
		/**
		 * Creates a new view navigator for a subtree of the view
		 * 
		 * @param topEntry top entry of the subtree (navigator contains all descendants of this entry)
		 * @return navigator
		 */
		public VirtualViewNavigator buildFromDescendants(VirtualViewEntryData topEntry) {
			fillWithCategoriesAndDocumentsWithDefaults();
			return new VirtualViewNavigator(this.view, topEntry, cats, docs, createAccessCheck());
		}
		
		/**
		 * Creates a new view navigator that starts at a specific category (containing all descendants of the category entry)
		 * 
		 * @param category category name (e.g. "Sales\\2017")
		 * @return navigator
		 */
		public VirtualViewNavigator buildFromCategory(String category) {
			fillWithCategoriesAndDocumentsWithDefaults();
			ViewEntryAccessCheck accessCheck = createAccessCheck();

			String[] categoryParts = category.split("\\\\", -1);
			
			VirtualViewNavigator findCategoryNav = new VirtualViewNavigator(this.view, getRoot(), 
					WithCategories.YES, WithDocuments.NO, accessCheck);
			
			VirtualViewEntryData currCategoryEntry = getRoot();
			for (String currPart : categoryParts) {
				VirtualViewEntryData matchingSubCategories = findCategoryNav
						.childCategoriesByKey(currCategoryEntry, currPart, true, false)
						.findFirst()	
						.orElse(null);
				
				if (matchingSubCategories == null) {
					// category not found
					currCategoryEntry = null;
					break;
				}
				else {
					currCategoryEntry = matchingSubCategories;
				}
			}
			
			VirtualViewNavigator nav = new VirtualViewNavigator(this.view, currCategoryEntry, cats, docs,
					accessCheck);
			return nav;
		}
		
		/**
		 * Creates a new view navigator that starts at a specific category (containing all descendants of the category entry)
		 * 
		 * @param categoryLevels category levels (e.g. Arrays.asList("Sales", "2017"))
		 * @return navigator
		 */
		public VirtualViewNavigator buildFromCategory(List<Object> categoryLevels) {
			fillWithCategoriesAndDocumentsWithDefaults();
			ViewEntryAccessCheck accessCheck = createAccessCheck();

			VirtualViewNavigator findCategoryNav = new VirtualViewNavigator(this.view, getRoot(), WithCategories.YES,
					WithDocuments.NO, accessCheck);

			VirtualViewEntryData currCategoryEntry = getRoot();
			for (Object currPart : categoryLevels) {
				VirtualViewEntryData matchingSubCategories = findCategoryNav
						.childCategoriesBetween(currCategoryEntry, currPart, currPart, false).findFirst().orElse(null);

				if (matchingSubCategories == null) {
					// category not found
					currCategoryEntry = null;
					break;
				} else {
					currCategoryEntry = matchingSubCategories;
				}
			}

			VirtualViewNavigator nav = new VirtualViewNavigator(this.view, currCategoryEntry, cats, docs,
					accessCheck);
			return nav;
		}
	}
	
	/**
	 * Creates a new view navigator to traverse the view structure
	 * 
	 * @return builder
	 */
	public VirtualViewNavigatorBuilder createViewNav() {
		return new VirtualViewNavigatorBuilder(this);
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
			List<VirtualViewEntryData> categoryEntriesToCheck = new ArrayList<>();
			
			//apply removals
			
			for (int currNoteId : change.getRemovals()) {
				ScopedNoteId scopedNoteId = new ScopedNoteId(origin, currNoteId);
				List<VirtualViewEntryData> entries = entriesByNoteId.remove(scopedNoteId);
				if (entries != null) {
					for (VirtualViewEntryData currEntry : entries) {
						if (currEntry.isCategory() || ORIGIN_VIRTUALVIEW.equals(currEntry.getOrigin())) {
							// don't remove our own entries or categories
							continue;
						}
						
						VirtualViewEntryData parentEntry = currEntry.getParent();
						if (parentEntry.getChildEntriesAsMap().remove(currEntry.getSortKey()) != null) {
						    parentEntry.childCount.decrementAndGet();
					    	parentEntry.childDocumentCount.decrementAndGet();

						    reduceDescendantCountAndTotalValuesOfParents(currEntry);
						    
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

				List<VirtualViewEntryData> addedViewEntries = addEntry(origin, currNoteId, unid, columnValues,
						rootEntry, this.categoryColumns, true);
				ScopedNoteId scopedNoteId = new ScopedNoteId(origin, currNoteId);
                entriesByNoteId.put(scopedNoteId, addedViewEntries);
			}
			
			//clean up category entries that are now empty
			
			for (VirtualViewEntryData currCategoryEntry : categoryEntriesToCheck) {
				if (currCategoryEntry.getChildEntriesAsMap().isEmpty()) {
					removeCategoryFromParent(currCategoryEntry);
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
	private void markEntryForSiblingIndexFlush(VirtualViewEntryData ve) {
		List<VirtualViewEntryData> entries = pendingSiblingIndexFlush.get(new ScopedNoteId(ve.getOrigin(), ve.getNoteId()));
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
		for (Entry<ScopedNoteId, List<VirtualViewEntryData>> currMapEntry : pendingSiblingIndexFlush.entrySet()) {
			for (VirtualViewEntryData currViewEntry : currMapEntry.getValue()) {
				int[] pos = new int[] {1};
				
				if (currViewEntry.getChildCount() > 0) {
					currViewEntry.getChildEntriesAsMap().entrySet().forEach((currChild) -> {
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
	private List<VirtualViewEntryData> addEntry(String origin, int noteId, String unid,
			Map<String, Object> columnValues, VirtualViewEntryData targetParent,
			List<VirtualViewColumn> remainingCategoryColumns, boolean firstCall) {
		
		List<VirtualViewEntryData> createdChildEntriesForDocument = new ArrayList<>();
		
		//compute additional values provided via function
		for (VirtualViewColumn currValueFunctionColumn : this.valueFunctionColumns) {
			String itemName = currValueFunctionColumn.getItemName();
			Object value = currValueFunctionColumn.getFunction().getValue(origin, itemName, new TypedItemAccess() {

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

			VirtualViewEntryData newDocChild = new VirtualViewEntryData(this,
					targetParent, origin, noteId,
					unid, sortKey,
					childEntryComparator);
			//TODO add support for permuted columns (multiple rows for one doc)

			newDocChild.setColumnValues(columnValues);
			if (targetParent.getChildEntriesAsMap().put(sortKey, newDocChild) == null) {
				targetParent.childCount.incrementAndGet();
				targetParent.childDocumentCount.incrementAndGet();
				increaseDescendantCountAndTotalValuesOfParents(newDocChild);
			}
		    //remember to assign new sibling indexes
			markEntryForSiblingIndexFlush(targetParent);

			createdChildEntriesForDocument.add(newDocChild);			
			return createdChildEntriesForDocument;
		}
		
		VirtualViewColumn currCategoryColumn = remainingCategoryColumns.get(0);
		List<VirtualViewColumn> remainingColumnsForNextIteration = remainingCategoryColumns.size() == 1 ? Collections.emptyList() : remainingCategoryColumns.subList(1, remainingCategoryColumns.size());
		
		
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
				
				VirtualViewEntryData currentSubCatParent = targetParent;
				
				for (int i=0; i<parts.length; i++) {
					String currSubCat = parts[i];
					boolean isLastPart = i == parts.length-1;
					
					Object currSubCatObj = "".equals(currSubCat) ? null : currSubCat;
					
					ViewEntrySortKey categorySortKey = ViewEntrySortKey.createSortKey(true, Arrays.asList(new Object[] { currSubCatObj }),
							ORIGIN_VIRTUALVIEW, 0);
					
					VirtualViewEntryData entryWithSortKey = currentSubCatParent.getChildEntriesAsMap().get(categorySortKey);
					if (entryWithSortKey == null) {
						boolean childCategoryOrderingDescending;
						if (!isLastPart) {
							//sort the subcategories like the current column, e.g. for "2024\03", sort the "03" like the "2024"
							childCategoryOrderingDescending = currCategoryColumn.getSorting() == ColumnSort.DESCENDING;
						}
						else {
							//for the last part, sort the categories like the next category column
							VirtualViewColumn nextCategoryColumn = remainingColumnsForNextIteration.isEmpty() ? null : remainingColumnsForNextIteration.get(0);
							
							if (nextCategoryColumn != null && nextCategoryColumn.getSorting() == ColumnSort.DESCENDING) {
								childCategoryOrderingDescending = true;
							}
							else {
								childCategoryOrderingDescending = false;
							}
						}
						ViewEntrySortKeyComparator childEntryComparator = new ViewEntrySortKeyComparator(childCategoryOrderingDescending, this.docOrderDescending);

						
						int newCategoryNoteId = createNewCategoryNoteId();
						entryWithSortKey = new VirtualViewEntryData(this, currentSubCatParent, ORIGIN_VIRTUALVIEW,
								newCategoryNoteId, "", categorySortKey,
								childEntryComparator);
						Map<String,Object> categoryColValues = new ConcurrentHashMap<>();
						if (currSubCatObj != null) {
							categoryColValues.put(itemName, currSubCatObj);
						}
						entryWithSortKey.setColumnValues(categoryColValues);
						
						if (currentSubCatParent.getChildEntriesAsMap().put(categorySortKey, entryWithSortKey) == null) {
							currentSubCatParent.childCount.incrementAndGet();
							currentSubCatParent.childCategoryCount.incrementAndGet();
							
							//bubble up the descendant count
							VirtualViewEntryData currParent = currentSubCatParent;
							while (currParent != null) {
								currParent.descendantCategoryCount.incrementAndGet();
								currParent.descendantCount.incrementAndGet();
								currParent = currParent.getParent();
							}
						}
						entriesByNoteId.put(new ScopedNoteId(ORIGIN_VIRTUALVIEW, newCategoryNoteId), Arrays.asList(entryWithSortKey));
						
					    //remember to assign new sibling indexes
						markEntryForSiblingIndexFlush(currentSubCatParent);
					}
					
					currentSubCatParent = entryWithSortKey;
				}
				
				//go on with the remaining categories
				List<VirtualViewEntryData> addedEntries = addEntry(origin, noteId, unid, columnValues, currentSubCatParent,
						remainingColumnsForNextIteration, false);
				createdChildEntriesForDocument.addAll(addedEntries);
			}
			else {
				ViewEntrySortKey categorySortKey = ViewEntrySortKey.createSortKey(true, Arrays.asList(new Object[] { currCategoryValue }),
						ORIGIN_VIRTUALVIEW,
						0);
				
				VirtualViewEntryData entryWithSortKey = targetParent.getChildEntriesAsMap().get(categorySortKey);
				if (entryWithSortKey == null) {
					VirtualViewColumn nextCategoryColumn = remainingColumnsForNextIteration.isEmpty() ? null : remainingColumnsForNextIteration.get(0);
					ColumnSort nextCategoryColumnSort = ColumnSort.ASCENDING;
					if (nextCategoryColumn != null && nextCategoryColumn.getSorting() == ColumnSort.DESCENDING) {
						nextCategoryColumnSort = ColumnSort.DESCENDING;
					}
					ViewEntrySortKeyComparator childEntryComparator = new ViewEntrySortKeyComparator(
							nextCategoryColumnSort == ColumnSort.DESCENDING, this.docOrderDescending);

					int newCategoryNoteId = createNewCategoryNoteId();
					entryWithSortKey = new VirtualViewEntryData(this, targetParent, ORIGIN_VIRTUALVIEW,
							newCategoryNoteId, "", categorySortKey,
							childEntryComparator);
					Map<String,Object> categoryColValues = new ConcurrentHashMap<>();
					if (currCategoryValue != null) {
						categoryColValues.put(itemName, currCategoryValue);
					}
					entryWithSortKey.setColumnValues(categoryColValues);

					if (targetParent.getChildEntriesAsMap().put(categorySortKey, entryWithSortKey) == null) {
						targetParent.childCount.incrementAndGet();
						targetParent.childCategoryCount.incrementAndGet();
						
						//bubble up the descendant count
						VirtualViewEntryData currParent = targetParent;
						while (currParent != null) {
							currParent.descendantCategoryCount.incrementAndGet();
							currParent.descendantCount.incrementAndGet();
							currParent = currParent.getParent();
						}
					}
					entriesByNoteId.put(new ScopedNoteId(ORIGIN_VIRTUALVIEW, newCategoryNoteId), Arrays.asList(entryWithSortKey));
					
				    //remember to assign new sibling indexes
					markEntryForSiblingIndexFlush(targetParent);
				}

				//go on with the remaining categories
				List<VirtualViewEntryData> addedEntries = addEntry(origin, noteId, unid, columnValues,
						entryWithSortKey,
						remainingColumnsForNextIteration, false);
				createdChildEntriesForDocument.addAll(addedEntries);
			}			
		}
	
		return createdChildEntriesForDocument;
	}

	private void increaseDescendantCountAndTotalValuesOfParents(VirtualViewEntryData docEntry) {
		Map<String,Double> docTotalValues = null;
		if (viewHasTotalColumns) {
			docTotalValues = new HashMap<>();
			for (VirtualViewColumn currTotalColumn : totalColumns) {
				String itemName = currTotalColumn.getItemName();
				Double docVal = docEntry.getAsDouble(itemName, null);
				if (docVal != null) {
					docTotalValues.put(itemName, docVal);
				}
			}			
		}
		
		VirtualViewEntryData currParent = docEntry.getParent();
		while (currParent != null) {
			currParent.descendantDocumentCount.incrementAndGet();
			currParent.descendantCount.incrementAndGet();
			
			if (docTotalValues != null) {
				for (Entry<String,Double> currDocTotalValue : docTotalValues.entrySet()) {
	                String itemName = currDocTotalValue.getKey();
	                Double dblVal = currDocTotalValue.getValue();
	                
	                currParent.addAndGetTotalValue(itemName, dblVal);
	            }
				
				computeTotalColumnValues(currParent);				
			}

			currParent = currParent.getParent();
		}
	}
	
	private void reduceDescendantCountAndTotalValuesOfParents(VirtualViewEntryData docEntry) {
		Map<String,Double> docTotalValues = null;
		if (viewHasTotalColumns) {
			docTotalValues = new HashMap<>();
			for (VirtualViewColumn currTotalColumn : totalColumns) {
				String itemName = currTotalColumn.getItemName();
				Double docVal = docEntry.getAsDouble(itemName, null);
				if (docVal != null) {
					docTotalValues.put(itemName, docVal);
				}
			}			
		}

		VirtualViewEntryData currParent = docEntry.getParent();
		while (currParent != null) {
			currParent.descendantDocumentCount.decrementAndGet();
			currParent.descendantCount.decrementAndGet();
			
			if (docTotalValues != null) {
				for (Entry<String,Double> currDocTotalValue : docTotalValues.entrySet()) {
	                String itemName = currDocTotalValue.getKey();
	                Double dblVal = currDocTotalValue.getValue();
	                
	                currParent.addAndGetTotalValue(itemName, -1 * dblVal);
	            }				
				
				computeTotalColumnValues(currParent);				
			}
			
			currParent = currParent.getParent();
		}
	}
	
	private void computeTotalColumnValues(VirtualViewEntryData catEntry) {
		for (VirtualViewColumn currTotalColumn : totalColumns) {
			String itemName = currTotalColumn.getItemName();
			Double dblVal = catEntry.getTotalValue(itemName);
			
			if (dblVal == null) {
				catEntry.getColumnValues().remove(itemName);
			}
			else {
				if (currTotalColumn.getTotalMode() == Total.SUM) {
					catEntry.getColumnValues().put(itemName, dblVal);
				}
				else if (currTotalColumn.getTotalMode() == Total.AVERAGE) {
					int docCount = catEntry.getDescendantDocumentCount(); // computeDescendantDocCount(catEntry);
					if (docCount == 0) {
						catEntry.getColumnValues().remove(itemName);
					}
					else {
						catEntry.getColumnValues().put(itemName, dblVal / docCount);
					}
				}				
			}
		}
	}
	
	/**
	 * Removes an entry from the tree structure. If the parent of the entry is a category and
	 * it has no more children, it will be removed too.
	 * 
	 * @param entry entry to remove
	 */
	private void removeCategoryFromParent(VirtualViewEntryData entry) {
		VirtualViewEntryData parentEntry = entry.getParent();
		if (parentEntry != null) {
			if (parentEntry.getChildEntriesAsMap().remove(entry.getSortKey()) != null) {
				parentEntry.childCount.decrementAndGet();
				if (entry.isCategory()) {
					parentEntry.childCategoryCount.decrementAndGet();
				} else if (entry.isDocument()) {
					parentEntry.childDocumentCount.decrementAndGet();
				}
			    //remember to assign new sibling indexes
				markEntryForSiblingIndexFlush(parentEntry);

			    //cleanup entry from selected and expanded entries
			    ScopedNoteId scopedNoteId = new ScopedNoteId(entry.getOrigin(), entry.getNoteId());
			    entriesByNoteId.remove(scopedNoteId);
			}
			
			if (parentEntry.isCategory()) {
				if (parentEntry.getChildEntriesAsMap().isEmpty()) {
					removeCategoryFromParent(parentEntry);
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
	public VirtualViewEntryData getRoot() {
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
	public List<VirtualViewEntryData> findEntries(String origin, int noteId) {
		return Collections.unmodifiableList(entriesByNoteId.get(new ScopedNoteId(origin, noteId)));
	}
}
