package com.mindoo.domino.jna.virtualviews;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Entry in a {@link VirtualView}, representing a document or category.
 */
public class VirtualViewEntry {
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
	
	public Map<String,Object> getColumnValues() {
		return columnValues;
	}
	
	public void setColumnValues(Map<String,Object> columnValues) {
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
	
	public ConcurrentNavigableMap<ViewEntrySortKey,VirtualViewEntry> getCategories() {
		ViewEntrySortKey lowCategorySearchKey = ViewEntrySortKey.createScanKey(true, Arrays.asList(new Object[] {LOW_SORTVAL}),
				LOW_ORIGIN,
				0);
		ViewEntrySortKey highCategorySearchKey = ViewEntrySortKey.createScanKey(true, Arrays.asList(new Object[] {HIGH_SORTVAL}),
				HIGH_ORIGIN,
				Integer.MAX_VALUE);
		
		return childEntriesBySortKey.subMap(lowCategorySearchKey, false, highCategorySearchKey, false);
	}
	
	public ConcurrentNavigableMap<ViewEntrySortKey, VirtualViewEntry> getCategories(Object startKey, boolean startInclusive, Object endKey, boolean endInclusive) {
		ViewEntrySortKey lowCategorySortKey = ViewEntrySortKey.createScanKey(true, Arrays.asList(new Object[] {startKey, LOW_SORTVAL}),
				LOW_ORIGIN,
				0);
		ViewEntrySortKey highCategorySortKey = ViewEntrySortKey.createScanKey(true, Arrays.asList(new Object[] {endKey, HIGH_SORTVAL}),
				HIGH_ORIGIN,
				Integer.MAX_VALUE);
		
		return childEntriesBySortKey.subMap(lowCategorySortKey, startInclusive, highCategorySortKey, endInclusive);
	}
	
	public ConcurrentNavigableMap<ViewEntrySortKey,VirtualViewEntry> getDocuments() {
		ViewEntrySortKey lowCategorySearchKey = ViewEntrySortKey.createScanKey(false, Arrays.asList(new Object[] {LOW_SORTVAL}),
				LOW_ORIGIN,
				0);
		ViewEntrySortKey highCategorySearchKey = ViewEntrySortKey.createScanKey(false, Arrays.asList(new Object[] {HIGH_SORTVAL}),
				HIGH_ORIGIN,
				Integer.MAX_VALUE);
		
		return childEntriesBySortKey.subMap(lowCategorySearchKey, false, highCategorySearchKey, false);		
	}

	public ConcurrentNavigableMap<ViewEntrySortKey,VirtualViewEntry> getDocuments(Object startKey, boolean startInclusive, Object endKey, boolean endInclusive) {
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
	    return getAsStringList("$C1$").orElse(null);
	}
	
	/**
	 * Returns a column value as string
	 * 
	 * @param itemName item name
	 * @return value or empty
	 */
	public Optional<String> getAsString(String itemName) {
		if (columnValues.containsKey(itemName)) {
			Object value = columnValues.get(itemName);
			if (value instanceof String) {
				return Optional.of((String) value);
			} else {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}
	
	/**
	 * Returns a column value as number
	 * 
	 * @param itemName item name
	 * @return value or empty
	 */
	public Optional<Number> getAsNumber(String itemName) {
		if (columnValues.containsKey(itemName)) {
			Object value = columnValues.get(itemName);
			if (value instanceof Number) {
				return Optional.of((Number) value);
			} else {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}
	
	/**
	 * Returns a column value as string list
	 * 
	 * @param itemName item name
	 * @return value or empty
	 */
	public Optional<List<String>> getAsStringList(String itemName) {
		if (columnValues.containsKey(itemName)) {
			Object value = columnValues.get(itemName);
			if (value instanceof List && !((List)value).isEmpty() && ((List)value).get(0) instanceof String) {
				return Optional.of((List<String>) value);
			} else if (value instanceof String) {
				return Optional.of(Arrays.asList((String) value));
			} else {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}
	
	/**
	 * Returns a column value as number list
	 * 
	 * @param itemName item name
	 * @return value or empty
	 */
	public Optional<List<Number>> getAsNumberList(String itemName) {
		if (columnValues.containsKey(itemName)) {
			Object value = columnValues.get(itemName);
			if (value instanceof List && !((List) value).isEmpty() && ((List) value).get(0) instanceof Number) {
				return Optional.of((List<Number>) value);
			} else if (value instanceof Number) {
				return Optional.of(Arrays.asList((Number) value));
			} else {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}
	
	/**
	 * Returns a column value as {@link NotesTimeDate}
	 * 
	 * @param itemName item name
	 * @return value or empty
	 */
	public Optional<NotesTimeDate> getAsTimeDate(String itemName) {
		if (columnValues.containsKey(itemName)) {
			Object value = columnValues.get(itemName);
			if (value instanceof NotesTimeDate) {
				return Optional.of((NotesTimeDate) value);
			} else {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}
	
	/**
	 * Returns a column value as {@link NotesTimeDate} list
	 * 
	 * @param itemName item name
	 * @return value or empty
	 */
	public Optional<List<NotesTimeDate>> getAsTimeDateList(String itemName) {
		if (columnValues.containsKey(itemName)) {
			Object value = columnValues.get(itemName);
			if (value instanceof List && !((List) value).isEmpty() && ((List) value).get(0) instanceof NotesTimeDate) {
				return Optional.of((List<NotesTimeDate>) value);
			} else if (value instanceof NotesTimeDate) {
				return Optional.of(Arrays.asList((NotesTimeDate) value));
			} else {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}
	
	int getSiblingIndex() {
		return siblingIndex;
	}
	
	void setSiblingIndex(int idx) {
		siblingIndex = idx;
	}
	
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
	
	public int getLevel() {
		if (level == 0) {
			VirtualViewEntry parentEntry = getParent();
			while (parentEntry != null) {
				int parentSiblingIdx = parentEntry.getSiblingIndex();
				
				parentEntry = parentEntry.getParent();
				if (parentEntry != null) {
					//ignore root sibling position
					level++;
				}
			}
		}
		return level;
	}

	public int[] getPosition() {
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
		return "VirtualViewEntry [pos=" + Arrays.toString(getPosition())+", type=" + (isDocument() ? "document" : isCategory() ? "category" : "") +
				", sortKey=" + sortKey +
				", origin=" + origin + ", noteId=" + noteId + ", unid=" + unid +
				", columnValues=" + columnValues + ", childCount=" + childCount + "]";
	}
	
	
}
