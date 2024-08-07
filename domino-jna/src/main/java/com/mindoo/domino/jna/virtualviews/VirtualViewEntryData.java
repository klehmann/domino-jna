package com.mindoo.domino.jna.virtualviews;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.mindoo.domino.jna.IViewEntryData;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.TypedItemAccess;

/**
 * Entry in a {@link VirtualView}, representing a document or category.
 */
public class VirtualViewEntryData extends TypedItemAccess implements IViewEntryData {
	//properties for the position in the view
	private VirtualView parentView;
	private VirtualViewEntryData parent;
	
	private String origin;
	private int noteId;
	private String unid;
	private int siblingIndex;
	private int level = Integer.MIN_VALUE;
	private int indentLevels;
	
	private int[] pos;
	private String posStr;
	
	private ViewEntrySortKey sortKey;	
	private Map<String,Object> columnValues;
	
	private ConcurrentSkipListMap<ViewEntrySortKey,VirtualViewEntryData> childEntriesBySortKey;
	private Comparator<ViewEntrySortKey> childrenComparator;
	
	/** this is updated by the VirtualView when child elements are added/removed */
	AtomicInteger childCount;
	AtomicInteger childCategoryCount;
	AtomicInteger childDocumentCount;
	
	AtomicInteger descendantCount;
	AtomicInteger descendantDocumentCount;
	AtomicInteger descendantCategoryCount;
	
	public VirtualViewEntryData(VirtualView parentView, VirtualViewEntryData parent, String origin, int noteId, String unid,
			ViewEntrySortKey sortKey, Comparator<ViewEntrySortKey> childrenComparator) {
		this.parentView = parentView;
		this.parent = parent;
		this.origin = origin;
		this.noteId = noteId;
		this.unid = unid;
		this.sortKey = sortKey;
		
		this.childCount = new AtomicInteger();
		this.childCategoryCount = new AtomicInteger();
		this.childDocumentCount = new AtomicInteger();

		this.descendantCount = new AtomicInteger();
		this.descendantDocumentCount = new AtomicInteger();
		this.descendantCategoryCount = new AtomicInteger();
		
		Objects.requireNonNull(childrenComparator);
		this.childrenComparator = childrenComparator;
		this.childEntriesBySortKey = new ConcurrentSkipListMap<>(childrenComparator);
	}
	
	public VirtualView getParentView() {
		return parentView;
	}
	
	public VirtualViewEntryData getParent() {
		return parent;
	}

	public Comparator<?> getChildrenComparator() {
		return childrenComparator;
	}
	
	@Override
	public int getChildCount() {
		return childCount.get();
	}
	
	public int getChildCategoryCount() {
		return childCategoryCount.get();
	}
	
	public int getChildDocumentCount() {
		return childDocumentCount.get();
	}
	
	@Override
	public int getDescendantCount() {
		return descendantCount.get();
	}
	
	public int getDescendantCountWithoutReaders() {
		return descendantCountWithoutReaders.get();
	}
	
	public int getDescendantDocumentCount() {
        return descendantDocumentCount.get();
	}
	
	public int getDescendantCategoryCount() {
		return descendantCategoryCount.get();
	}
	
	@Override
	public int getSiblingCount() {
		if (parent != null) {
			return parent.getChildCount();
		} else {
			return 0;
		}
	}
	
	public String getOrigin() {
		return origin;
	}
	
	ViewEntrySortKey getSortKey() {
		return sortKey;
	}
	
	@Override
	public int getNoteId() {
		return noteId;
	}
	
	@Override
	public String getNoteIdAsHex() {
		return Integer.toString(noteId, 16);
	}
	
	@Override
	public String getUNID() {
		return unid;
	}
	
	public Object getCategoryValue() {
		if (isCategory()) {
			return getSortKey().getValues().get(0);
		}
		else {
			return null;
		}
	}
	
	@Override
	public Object get(String itemName) {
		return columnValues==null ? null : columnValues.get(itemName);
	}
	
	/**
	 * Returns the item names of the column values
	 * 
	 * @return item names
	 */
	public Iterator<String> getItemNames() {
		return columnValues == null ? Collections.emptyIterator() : columnValues.keySet().iterator();
	}
	
	/**
	 * Returns the column values of the entry as a map. Use
	 * 
	 * @return column values
	 */
	public Map<String,Object> getColumnValues() {
		return columnValues;
	}
	
	void setColumnValues(Map<String,Object> columnValues) {
		this.columnValues = columnValues;
	}
	
	@Override
	public boolean isDocument() {
		return !isCategory() && !isTotal();
	}
	
	@Override
	public boolean isTotal() {
		return noteId != -1 && (noteId & NotesConstants.NOTEID_CATEGORY_TOTAL) == NotesConstants.NOTEID_CATEGORY_TOTAL;
	}
	
	@Override
	public boolean isCategory() {
		return noteId != -1 && ((noteId & NotesConstants.NOTEID_CATEGORY) == NotesConstants.NOTEID_CATEGORY);
	}
	
	/**
	 * Returns the child view entries sorted by their sort key
	 * 
	 * @return child entries
	 */
	ConcurrentNavigableMap<ViewEntrySortKey,VirtualViewEntryData> getChildEntriesAsMap() {
		return childEntriesBySortKey;
	}
	
	final static Object LOW_SORTVAL = new Object();
	final static Object HIGH_SORTVAL = new Object();
	
	final static String LOW_ORIGIN = "~~LOW~";
	final static String HIGH_ORIGIN = "~~HIGH~";
	
	/**
	 * Returns the child categories of this entry
	 * 
	 * @param descending whether to return the categories in descending order
	 * @return child categories
	 */
	ConcurrentNavigableMap<ViewEntrySortKey, VirtualViewEntryData> getChildCategoriesAsMap() {
		ViewEntrySortKey lowCategorySearchKey = ViewEntrySortKey.createScanKey(true, Arrays.asList(new Object[] {LOW_SORTVAL}),
				LOW_ORIGIN,
				0);
		ViewEntrySortKey highCategorySearchKey = ViewEntrySortKey.createScanKey(true, Arrays.asList(new Object[] {HIGH_SORTVAL}),
				HIGH_ORIGIN,
				Integer.MAX_VALUE);
		
		return childEntriesBySortKey.subMap(lowCategorySearchKey, false, highCategorySearchKey, false);
	}

	/**
	 * Returns the child documents of this entry
	 * 
	 * @param descending whether to return the documents in descending order
	 * @return child documents
	 */
	ConcurrentNavigableMap<ViewEntrySortKey, VirtualViewEntryData> getChildDocumentsAsMap() {
		ViewEntrySortKey lowCategorySearchKey = ViewEntrySortKey.createScanKey(false, Arrays.asList(new Object[] {LOW_SORTVAL}),
				LOW_ORIGIN,
				0);
		ViewEntrySortKey highCategorySearchKey = ViewEntrySortKey.createScanKey(false, Arrays.asList(new Object[] {HIGH_SORTVAL}),
				HIGH_ORIGIN,
				Integer.MAX_VALUE);
		
		return childEntriesBySortKey.subMap(lowCategorySearchKey, false, highCategorySearchKey, false);
	}
	
	/**
	 * Returns a list of readers that are allowed to see a document entry
	 * 
	 * @return readers list or null if the entry is visible to everyone
	 */
	public Collection<String> getDocReadersList() {
		if (isCategory()) {
			return categoryReadersList.keySet();
		}
		else {
		    return getAsStringList("$C1$", null);			
		}
	}
	
	/**
	 * For category entries, we collect the readers lists of all descendants to do
	 * quick read access checks. This method returns the names of all origins that
	 * have readers lists for this category entry.
	 * 
	 * @return origins
	 */
	public Set<String> getCategoryReadersListOrigins() {
		return categoryReadersList.keySet();
	}
	
	/**
	 * For category entries, we collect the readers lists of all descendants to do
	 * quick read access checks. This method returns the readers list for a specific origin.
	 * 
	 * @param origin origin
	 * @return readers list or null if no readers list is stored for this origin
	 */
	public Set<String> getCategoryReadersList(String origin) {
		return categoryReadersList.getOrDefault(origin, Collections.emptyMap()).keySet();
	}
	
	int getSiblingIndex() {
		return siblingIndex;
	}
	
	void setSiblingIndex(int idx) {
		if (siblingIndex != idx) {
			siblingIndex = idx;
			//reset cached values, because our index has changed
			pos = null;
			posStr = null;			
		}
	}
	
	void setIndentLevels(int level) {
		indentLevels = level;
	}
	
	@Override
	public int getIndentLevels() {
		return indentLevels;
	}
	
	/**
	 * Returns the position of the entry in the view
	 * 
	 * @return position string, e.g. "1.2.3"
	 */
	@Override
	public String getPositionStr() {
		if (posStr == null) {
			int[] pos = getPosition();
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<pos.length; i++) {
				if (sb.length() > 0) {
					sb.append('.');
				}

				sb.append(pos[i]);
			}
			posStr = sb.toString();
		}
		return posStr;
	}
	
	@Override
	public int getLevel() {
		if (level == Integer.MIN_VALUE) {
			level = -1;
			VirtualViewEntryData parentEntry = getParent();
			while (parentEntry != null) {
				level++;
				parentEntry = parentEntry.getParent();
			}
		}
		return level;
	}

	/**
	 * Returns the position of the entry in the view
	 * 
	 * @return position array, e.g. [1,2,3]
	 */
	@Override
	public int[] getPosition() {
		if (pos == null) {
			if (parentView.getRoot().equals(this)) {
				pos = new int[] { 0 };
			}
			else {
				LinkedList<Integer> posList = new LinkedList<>();
				posList.add(getSiblingIndex());
				
				VirtualViewEntryData parentEntry = getParent();
				while (parentEntry != null) {
					int parentSiblingIdx = parentEntry.getSiblingIndex();
					
					parentEntry = parentEntry.getParent();
					if (parentEntry != null) {
						//ignore root sibling position
						posList.addFirst(parentSiblingIdx);
					}
				}
				
				pos = new int[posList.size()];
				int idx = 0;
				for (Integer currPos : posList) {
					pos[idx++] = currPos.intValue();
				}
			}
		}
		return pos;
	}
	
	private ConcurrentHashMap<String,Double> totalValues = new ConcurrentHashMap<>();
	
	private ConcurrentHashMap<String,Map<String,Integer>> categoryReadersList = new ConcurrentHashMap<>();
	/** number of descendant entries that do not have reader items or just "*" */
	AtomicInteger descendantCountWithoutReaders = new AtomicInteger();
	
	/**
	 * Adds a value to a total value and returns the new total
	 * 
	 * @param itemName item name
	 * @param val value to add
	 * @return new total value
	 */
	double addAndGetTotalValue(String itemName, double val) {
		String itemNameLC = itemName.toLowerCase();
		
		return totalValues.compute(itemNameLC, (key, oldVal) -> {
			if (oldVal == null) {
				return Double.valueOf(val);
            }
            else {
            	return Double.valueOf(oldVal.doubleValue() + val);
            }
        }).doubleValue();
	}
	
	/**
	 * Returns the total value for a specific item
	 * 
	 * @param itemName item name
	 * @return total value or null if no total value is stored
	 */
	public Double getTotalValue(String itemName) {
		String itemNameLC = itemName.toLowerCase();
		
		return totalValues.getOrDefault(itemName, null);
	}
	
	int increaseAndGetReader(String origin, String reader) {
		String readerLC = reader.toLowerCase();
		
		return categoryReadersList.computeIfAbsent(origin, (key) -> {
			return new ConcurrentHashMap<>();
		})
		.compute(readerLC, (key, oldVal) -> {
			if (oldVal == null) {
				return 1;
			} else {
				return oldVal + 1;
			}
		});
	}
	
	int decreaseAndGetReader(String origin, String reader) {
		String readerLC = reader.toLowerCase();
		
		Integer newVal = categoryReadersList.computeIfAbsent(origin, (key) -> {
			return new ConcurrentHashMap<>();
		})
		.compute(readerLC, (key, oldVal) -> {
			if (oldVal == null) {
				//should not happen
				return -1;
			} else if (oldVal == 1) {
				//remove reader from map
				return null;
			} else {
				return oldVal - 1;
			}
		});
		
		return newVal == null ? 0 : newVal.intValue();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("VirtualViewEntry [");
		sb.append("pos=").append(Arrays.toString(getPosition()));
		sb.append(", level=").append(getLevel());
		sb.append(", siblingIndex=").append(getSiblingIndex());
		sb.append(", type=").append(isDocument() ? "document" : isCategory() ? "category" : isTotal() ? "total" : "unknown");
		sb.append(", sortKey=").append(sortKey);
		sb.append(", origin=").append(origin);
		sb.append(", noteId=").append(noteId);
	    sb.append(", unid=").append(unid);
	    sb.append(", columnValues=").append(columnValues);
	    sb.append(", childCount=").append(childCount);
	    sb.append(", childDocCount=").append(childDocumentCount);
	    sb.append(", childCatCount=").append(childCategoryCount);
	    sb.append(", descendantCount=").append(descendantCount);
	    sb.append(", descendantDocCount=").append(descendantDocumentCount);
	    sb.append(", descendantCatCount=").append(descendantCategoryCount);
	    sb.append("]");
	    return sb.toString();
	}
	
}
