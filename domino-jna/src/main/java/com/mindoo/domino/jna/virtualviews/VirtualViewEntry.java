package com.mindoo.domino.jna.virtualviews;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.TypedItemAccess;

/**
 * Entry in a {@link VirtualView}, representing a document or category.
 */
public class VirtualViewEntry extends TypedItemAccess {
	//properties for the position in the view
	private VirtualView parentView;
	private VirtualViewEntry parent;
	
	private String origin;
	private int noteId;
	private String unid;
	private int siblingIndex;
	private int level;
	
	private ViewEntrySortKey sortKey;	
	private Map<String,Object> columnValues;
	
	private ConcurrentSkipListMap<ViewEntrySortKey,VirtualViewEntry> childEntriesBySortKey;
	/** this is updated by the VirtualView when child elements are added/removed */
	AtomicInteger childCount;
	
	public VirtualViewEntry(VirtualView parentView, VirtualViewEntry parent, String origin, int noteId, String unid,
			ViewEntrySortKey sortKey, Comparator<ViewEntrySortKey> childrenComparator) {
		this.parentView = parentView;
		this.parent = parent;
		this.origin = origin;
		this.noteId = noteId;
		this.unid = unid;
		this.sortKey = sortKey;
		this.childCount = new AtomicInteger();
		Objects.requireNonNull(childrenComparator);
		this.childEntriesBySortKey = new ConcurrentSkipListMap<>(childrenComparator);
	}
	
	public VirtualView getParentView() {
		return parentView;
	}
	
	public VirtualViewEntry getParent() {
		return parent;
	}

	public int getChildCount() {
		return childCount.get();
	}
	
	public int getDescendantCount() {
		int count = getChildCount();
		for (VirtualViewEntry child : childEntriesBySortKey.values()) {
			count += child.getDescendantCount();
		}
		return count;
	}
	
	public String getOrigin() {
		return origin;
	}
	
	public ViewEntrySortKey getSortKey() {
		return sortKey;
	}
	
	public int getNoteId() {
		return noteId;
	}
	
	public String getUnid() {
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
	
	public boolean isDocument() {
		return !isCategory() && !isTotal();
	}
	
	public boolean isTotal() {
		return noteId != -1 && (noteId & NotesConstants.NOTEID_CATEGORY_TOTAL) == NotesConstants.NOTEID_CATEGORY_TOTAL;
	}
	
	public boolean isCategory() {
		return noteId != -1 && ((noteId & NotesConstants.NOTEID_CATEGORY) == NotesConstants.NOTEID_CATEGORY);
	}
	
	/**
	 * Returns the child view entries sorted by their sort key
	 * 
	 * @return child entries
	 */
	public ConcurrentNavigableMap<ViewEntrySortKey,VirtualViewEntry> getChildEntries() {
		return childEntriesBySortKey;
	}

	final static Object LOW_SORTVAL = new Object();
	final static Object HIGH_SORTVAL = new Object();
	
	final static String LOW_ORIGIN = "~~LOW~";
	final static String HIGH_ORIGIN = "~~HIGH~";
	
	/**
	 * Returns the child categories of this entry
	 * 
	 * @return child categories
	 */
	public ConcurrentNavigableMap<ViewEntrySortKey,VirtualViewEntry> getChildCategories() {
		ViewEntrySortKey lowCategorySearchKey = ViewEntrySortKey.createScanKey(true, Arrays.asList(new Object[] {LOW_SORTVAL}),
				LOW_ORIGIN,
				0);
		ViewEntrySortKey highCategorySearchKey = ViewEntrySortKey.createScanKey(true, Arrays.asList(new Object[] {HIGH_SORTVAL}),
				HIGH_ORIGIN,
				Integer.MAX_VALUE);
		
		return childEntriesBySortKey.subMap(lowCategorySearchKey, false, highCategorySearchKey, false);
	}
	
	/**
	 * Returns the child categories of this entry in a specific range
	 * 
	 * @param startKey the start key
	 * @param startInclusive whether to include the start key
	 * @param endKey the end key
	 * @param endInclusive whether to include the end key
	 * @return child categories
	 */
	public ConcurrentNavigableMap<ViewEntrySortKey, VirtualViewEntry> getChildCategories(Object startKey, boolean startInclusive, Object endKey, boolean endInclusive) {
		ViewEntrySortKey lowCategorySortKey = ViewEntrySortKey.createScanKey(true, Arrays.asList(new Object[] {startKey, LOW_SORTVAL}),
				LOW_ORIGIN,
				0);
		ViewEntrySortKey highCategorySortKey = ViewEntrySortKey.createScanKey(true, Arrays.asList(new Object[] {endKey, HIGH_SORTVAL}),
				HIGH_ORIGIN,
				Integer.MAX_VALUE);
		
		return childEntriesBySortKey.subMap(lowCategorySortKey, startInclusive, highCategorySortKey, endInclusive);
	}
	
	/**
	 * Returns the child documents of this entry
	 * 
	 * @return child documents
	 */
	public ConcurrentNavigableMap<ViewEntrySortKey,VirtualViewEntry> getChildDocuments() {
		ViewEntrySortKey lowCategorySearchKey = ViewEntrySortKey.createScanKey(false, Arrays.asList(new Object[] {LOW_SORTVAL}),
				LOW_ORIGIN,
				0);
		ViewEntrySortKey highCategorySearchKey = ViewEntrySortKey.createScanKey(false, Arrays.asList(new Object[] {HIGH_SORTVAL}),
				HIGH_ORIGIN,
				Integer.MAX_VALUE);
		
		return childEntriesBySortKey.subMap(lowCategorySearchKey, false, highCategorySearchKey, false);		
	}

	/**
	 * Returns the child documents of this entry in a specific range
	 * 
	 * @param startKey the start key
	 * @param startInclusive whether to include the start key
	 * @param endKey the end key
	 * @param endInclusive whether to include the end key
	 * @return child documents
	 */
	public ConcurrentNavigableMap<ViewEntrySortKey,VirtualViewEntry> getChildDocuments(Object startKey, boolean startInclusive, Object endKey, boolean endInclusive) {
		ViewEntrySortKey lowCategorySortKey = ViewEntrySortKey.createScanKey(false, Arrays.asList(new Object[] {startKey, LOW_SORTVAL}),
				LOW_ORIGIN,
				0);
		ViewEntrySortKey highCategorySortKey = ViewEntrySortKey.createScanKey(false, Arrays.asList(new Object[] {endKey, HIGH_SORTVAL}),
				HIGH_ORIGIN,
				Integer.MAX_VALUE);
		
		return childEntriesBySortKey.subMap(lowCategorySortKey, startInclusive, highCategorySortKey, endInclusive);
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
	 * @param c separator character
	 * @return position string, e.g. "1.2.3"
	 */
	public String getPosition(char c) {
		int[] pos = getPosition();
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<pos.length; i++) {
			if (sb.length() > 0) {
				sb.append(c);
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
			VirtualViewEntry parentEntry = getParent();
			while (parentEntry != null) {
				parentEntry = parentEntry.getParent();
//				if (parentEntry != null) {
//					//ignore root sibling position
//				}
				level++;
			}
		}
		return level;
	}

	/**
	 * Returns the position of the entry in the view
	 * 
	 * @return position array, e.g. [1,2,3]
	 */
	public int[] getPosition() {
		if (parentView.getRoot().equals(this)) {
			return new int[] { 0 };
		}
		
		LinkedList<Integer> pos = new LinkedList<>();
		pos.add(getSiblingIndex());
		
		VirtualViewEntry parentEntry = getParent();
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
	
	@Override
	public String toString() {
		return "VirtualViewEntry [pos=" + Arrays.toString(getPosition())+", level="+getLevel()+", siblingIndex=" + getSiblingIndex() + ", type=" + (isDocument() ? "document" : isCategory() ? "category" : "") +
				", sortKey=" + sortKey +
				", origin=" + origin + ", noteId=" + noteId + ", unid=" + unid +
				", columnValues=" + columnValues + ", childCount=" + childCount + "]";
	}
	
}
