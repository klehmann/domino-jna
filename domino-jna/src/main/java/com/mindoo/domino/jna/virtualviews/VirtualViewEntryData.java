package com.mindoo.domino.jna.virtualviews;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
	private int level;
	
	private ViewEntrySortKey sortKey;	
	private Map<String,Object> columnValues;
	
	private ConcurrentSkipListMap<ViewEntrySortKey,VirtualViewEntryData> childEntriesBySortKey;
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
		this.childEntriesBySortKey = new ConcurrentSkipListMap<>(childrenComparator);
	}
	
	public VirtualView getParentView() {
		return parentView;
	}
	
	public VirtualViewEntryData getParent() {
		return parent;
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
	 * Returns a list of readers that are allowed to see the entry
	 * 
	 * @return readers list or null if the entry is visible to everyone
	 */
	public List<String> getReadersList() {
	    return getAsStringList("$C1$", null);
	}
	
	int getSiblingIndex() {
		return siblingIndex;
	}
	
	void setSiblingIndex(int idx) {
		siblingIndex = idx;
	}
	
	/**
	 * Returns the position of the entry in the view
	 * 
	 * @return position string, e.g. "1.2.3"
	 */
	@Override
	public String getPositionStr() {
		int[] pos = getPosition();
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<pos.length; i++) {
			if (sb.length() > 0) {
				sb.append('.');
			}
			
			sb.append(pos[i]);
		}
		return sb.toString();
	}
	
	/**
	 * Returns the level of the entry in the view (0 for root of virtual view, 1 for first level, ...)
	 * 
	 * @return level
	 */
	public int getLevel() {
		if (parentView.getRoot().equals(this)) {
			return 0;
		}
		
		if (level == 0) {
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
		if (parentView.getRoot().equals(this)) {
			return new int[] { 0 };
		}
		
		LinkedList<Integer> pos = new LinkedList<>();
		pos.add(getSiblingIndex());
		
		VirtualViewEntryData parentEntry = getParent();
		while (parentEntry != null) {
			int parentSiblingIdx = parentEntry.getSiblingIndex();
			
			parentEntry = parentEntry.getParent();
			if (parentEntry != null) {
				//ignore root sibling position
				pos.addFirst(parentSiblingIdx);
			}
		}
		
		int[] posArr = new int[pos.size()];
		int idx = 0;
		for (Integer currPos : pos) {
			posArr[idx++] = currPos.intValue();
		}
		return posArr;
	}
	
	private ConcurrentHashMap<String,AtomicLong> totalValues = new ConcurrentHashMap<>();
	
	/**
	 * Adds a value to a total value and returns the new total
	 * 
	 * @param itemName item name
	 * @param val value to add
	 * @return new total value
	 */
	double addAndGetTotalValue(String itemName, double val) {
		return totalValues.computeIfAbsent(itemName, (key) -> new AtomicLong()).updateAndGet((oldVal) -> {
			double dbl = Double.longBitsToDouble(oldVal);
			dbl += val;
			return Double.doubleToLongBits(dbl);
		});
	}
	
	/**
	 * Returns the total value for a specific item
	 * 
	 * @param itemName item name
	 * @return total value or null if no total value is stored
	 */
	public Double getTotalValue(String itemName) {
		AtomicLong x = totalValues.get(itemName);
		if (x == null) {
			return null;
		}
		return Double.longBitsToDouble(x.get());
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
