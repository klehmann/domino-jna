package com.mindoo.domino.jna.virtualviews;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.mindoo.domino.jna.utils.EmptyIterator;
import com.mindoo.domino.jna.virtualviews.VirtualView.ScopedNoteId;
import com.mindoo.domino.jna.virtualviews.security.IViewEntryAccessCheck;

/**
 * Notes ViewNavigator like implementation for the {@link VirtualView} to navigate in
 * the view structure and handle expanded/collapsed states of categorized views.<br>
 * <br>
 * The navigator supports the following features:
 * <ul>
 * <li>Reads category, document entries or both</li>
 * <li>Skips view entries that are not accessible for the current user (checks readers list)</li>
 * <li>Navigation to a specific position in the view (e.g. "1.2.3")</li>
 * <li>Navigation to the first/last entry</li>
 * <li>Navigation to the next/previous entry according to the expanded/collapsed categories ({@link #gotoNext()} / {@link #gotoPrev()})</li>
 * <li>Navigation to the parent entry ({@link #gotoParent()})</li>
 * <li>Navigation to the next/previous sibling entry ({@link #gotoPrevSibling()} / {@link #gotoNextSibling()})</li>
 * <li>Navigation to the first/last child entry ({@link #gotoFirstChild()} / {@link #gotoLastChild()})</li>
 * <li>Reduce the view to selected entries</li>
 * <li>Expansion/collapse of entries (e.g. {@link #expandAll()} or {@link #expand(int[])})</li>
 * </ul>
 * <br>
 * Those navigation methods move the cursor position within the tree structure.<br>
 * Use the method {@link #getCurrentEntry()} to read the view entry at this position.
 */
public class VirtualViewNavigator {
	public enum WithCategories { YES, NO };
	public enum WithDocuments { YES, NO };
	public enum SelectedOnly { YES, NO };
	
	private VirtualView view;
	private boolean withCategories;
	private boolean withDocuments;
	private IViewEntryAccessCheck viewEntryAccessCheck;
	
	/** if selectAll==true, this set is treated as deselected list */
	private Set<ScopedNoteId> selectedOrDeselectedEntries = ConcurrentHashMap.newKeySet();
	private boolean selectAll = false;
	
	/** if expandAll==true, this set is treated as collapsed list */
	private Set<ScopedNoteId> expandedOrCollapsedEntries = ConcurrentHashMap.newKeySet();
	private boolean expandAllByDefault = false;
	private int expandLevel;
	
	private Stack<TraversalInfo> currentEntryStack;
	
	/**
	 * Creates a new view navigator
	 * 
	 * @param view view
	 * @param cats whether to include category entries
	 * @param docs whether to include document entries
	 * @param viewEntryAccessCheck class to check {@link VirtualViewEntryData} visibility for a specific user
	 */
	public VirtualViewNavigator(VirtualView view, WithCategories cats, WithDocuments docs,
			IViewEntryAccessCheck viewEntryAccessCheck) {
		this(view, view.getRoot(), cats, docs, viewEntryAccessCheck);
	}
	
	/**
	 * Creates a new view navigator
	 * 
	 * @param view view
	 * @param topEntry top entry of the navigator (e.g. {@link VirtualView#getRoot()} or a different entry to reduce the view to a subtree)
	 * @param cats whether to include category entries
	 * @param docs whether to include document entries
	 * @param viewEntryAccessCheck class to check {@link VirtualViewEntryData} visibility for a specific user
	 */
	public VirtualViewNavigator(VirtualView view, VirtualViewEntryData topEntry, WithCategories cats, WithDocuments docs,
			IViewEntryAccessCheck viewEntryAccessCheck) {
		this.view = view;
		this.withCategories = cats == WithCategories.YES;
		this.withDocuments = docs == WithDocuments.YES;
		if (!withCategories && !withDocuments) {
			throw new IllegalArgumentException("The view navigator must contain categories, documents or both");
		}
		this.viewEntryAccessCheck = viewEntryAccessCheck;
		
		//set up the initial traversal info (top level if the accessible entries)
		this.currentEntryStack = new Stack<>();
		if (topEntry != null) {
			TraversalInfo traversalInfo = new TraversalInfo(topEntry, withCategories, withDocuments);
			this.currentEntryStack.push(traversalInfo);			
		}
	}
	
	public VirtualView getView() {
		return view;
	}
	
	/**
	 * Moves the top element of this view navigator to the specified position, resulting in a subtree ("restrict to category")
	 * 
	 * @param newRoot new root element
	 * @return this navigator
	 */
	public VirtualViewNavigator setRoot(VirtualViewEntryData newRoot) {
		TraversalInfo traversalInfo = new TraversalInfo(newRoot, withCategories, withDocuments);
		this.currentEntryStack = new Stack<>();
		this.currentEntryStack.push(traversalInfo);
		return this;
	}
	
	/**
	 * Moves the cursor to a position in the view
	 * 
	 * @param posStr position string e.g. "1.2.3"
	 * @return true if position could be found
	 */
	public boolean gotoPos(String posStr) {
		int[] pos = toPositionArray(posStr, '.');
		return gotoPos(pos);
	}

	/**
	 * Moves the cursor to a position in the view
	 * 
	 * @param pos new position
	 * @return true if position could be found
	 */
	public boolean gotoPos(int[] pos) {
		Optional<VirtualViewEntryData> entry = getPos(pos, true);
		return entry.isPresent();
	}
	
	/**
	 * Returns the view entry at the specified position
	 * 
	 * @param posStr position string e.g. "1.2.3"
	 * @return entry if found or empty
	 */
	public Optional<VirtualViewEntryData> getPos(String posStr) {
		int[] pos = toPositionArray(posStr, '.');
		return getPos(pos);
	}
	
	/**
	 * Returns the view entry at the specified position
	 * 
	 * @param pos new position, e.g. [1,2,3]
	 * @return entry if found or empty
	 */
	public Optional<VirtualViewEntryData> getPos(int[] pos) {
		return getPos(pos, false);
	}
	
	/**
	 * Moves the cursor to a position in the view
	 * 
	 * @param pos new position
	 * @param moveCursor true to move the cursor to the new position
	 * @return entry if found
	 */
	private Optional<VirtualViewEntryData> getPos(int[] pos, boolean moveCursor) {
		Stack<TraversalInfo> newCurrentEntryStack = new Stack<>();
		
		VirtualViewEntryData parentEntry = view.getRoot();
		TraversalInfo traversalInfo = new TraversalInfo(parentEntry, withCategories, withDocuments);
		newCurrentEntryStack.push(traversalInfo);
		
		for (int i=0; i<pos.length; i++) {						
			if (traversalInfo.gotoFirst()) {
				VirtualViewEntryData matchingEntry = null;
				do {
					VirtualViewEntryData currSearchEntry = traversalInfo.getCurrentEntry();
					if (currSearchEntry.getSiblingIndex() == pos[i]) {
						matchingEntry = currSearchEntry;
						break;
					}
				}
				while (traversalInfo.gotoNextSibling());
				
				if (matchingEntry != null) {
					if ((i+1) < pos.length) {
						//more to do, scan the next level
						traversalInfo = new TraversalInfo(matchingEntry, withCategories, withDocuments);
						newCurrentEntryStack.push(traversalInfo);
						parentEntry = matchingEntry;
						continue;
					}
					else {
						//we are done
						if (moveCursor) {
							currentEntryStack = newCurrentEntryStack;
						}
						return Optional.of(matchingEntry);
					}
				}
				else {
					//no match found
					return Optional.empty();
				}
			}
			else {
				//no match found
				return Optional.empty();
			}
		}
		
		return Optional.empty();
	}
	
	/**
	 * Navigates to the first child of the current entry
	 * 
	 * @return true if successful, false if the current entry has no children (then we don't change the cursor position)
	 */
	public boolean gotoFirstChild() {
		VirtualViewEntryData currEntry = getCurrentEntry();
		if (currEntry == null) {
			return false;
		}
		TraversalInfo traversalInfo = new TraversalInfo(currEntry, withCategories, withDocuments);
		boolean success = traversalInfo.gotoFirst();
		if (success) {
			currentEntryStack.push(traversalInfo);
			return traversalInfo.gotoFirst();			
		}
		else {
			return false;
		}
	}
	
	/**
	 * Navigates to the last child of the current entry
	 * 
	 * @return true if successful, false if the current entry has no children (then we don't change the cursor position)
	 */
	public boolean gotoLastChild() {
		VirtualViewEntryData currEntry = getCurrentEntry();
		if (currEntry == null) {
			return false;
		}
		TraversalInfo traversalInfo = new TraversalInfo(currEntry, withCategories, withDocuments);
		boolean success = traversalInfo.gotoLast();
		if (success) {
			currentEntryStack.push(traversalInfo);
			return traversalInfo.gotoLast();
		} else {
			return false;
		}
	}
	
	/**
	 * Moves the cursor to the parent entry
	 * 
	 * @return true if successful, false if the cursor is already at the top of the view (then we don't change the cursor position)
	 */
	public boolean gotoParent() {
		if (currentEntryStack.size() > 1) {
			currentEntryStack.pop();
			return true;
		} else {
			return false;
        }
	}
	
	/**
	 * Navigates to the next selected entry in the view, taking the expand states into account
	 * 
	 * @return true if successful
	 * @see #select(String, int, boolean)
	 */
	public boolean gotoNextSelected() {
		while (gotoNext()) {
			VirtualViewEntryData currEntry = getCurrentEntry();

			if (isSelected(currEntry.getOrigin(), currEntry.getNoteId())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Navigates to the previous selected entry in the view, taking the expand states into account
	 * 
	 * @return true if successful
	 * @see #select(String, int, boolean)
	 */
	public boolean gotoPrevSelected() {
		while (gotoPrev()) {
			VirtualViewEntryData currEntry = getCurrentEntry();

			if (isSelected(currEntry.getOrigin(), currEntry.getNoteId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Moves the cursor to the top of the view ({@link #gotoFirst()}) and
	 * then navigates through the view with {@link #gotoNext()}, returning
	 * the entries as a stream. Takes the expand states into account.
	 * 
	 * @param selectedOnly true to return only selected entries
	 * @return stream of entries
	 * @see #select(String, int, boolean)
	 */
	public Stream<VirtualViewEntryData> entriesForward(SelectedOnly selectedOnly) {
		if (!gotoFirst()) {
			return Stream.empty();
		}
		
		return StreamSupport
				.stream(new Spliterators.AbstractSpliterator<VirtualViewEntryData>(Long.MAX_VALUE, Spliterator.ORDERED) {
					@Override
					public boolean tryAdvance(Consumer<? super VirtualViewEntryData> action) {
						VirtualViewEntryData entry = getCurrentEntry();
						if (selectedOnly == SelectedOnly.NO ||
								(selectedOnly == SelectedOnly.YES && isSelected(entry.getOrigin(), entry.getNoteId()))) {
							action.accept(getCurrentEntry());
						}
						return selectedOnly == SelectedOnly.YES ? gotoNextSelected() : gotoNext();
					}
				}, false);			
	
	}
	
	/**
	 * Moves the cursor to the end of the view ({@link #gotoLast()}) and
	 * then navigates through the view with {@link #gotoPrev()}, returning
	 * the entries as a stream. Takes the expand states into account.
	 * 
	 * @param selectedOnly true to return only selected entries
	 * @return stream of entries
	 */
	public Stream<VirtualViewEntryData> entriesBackward(SelectedOnly selectedOnly) {
		if (!gotoLast()) {
			return Stream.empty();
		}
		
		return StreamSupport
				.stream(new Spliterators.AbstractSpliterator<VirtualViewEntryData>(Long.MAX_VALUE, Spliterator.ORDERED) {
					@Override
					public boolean tryAdvance(Consumer<? super VirtualViewEntryData> action) {
						VirtualViewEntryData entry = getCurrentEntry();
						if (selectedOnly == SelectedOnly.NO ||
								(selectedOnly == SelectedOnly.YES && isSelected(entry.getOrigin(), entry.getNoteId()))) {
							action.accept(getCurrentEntry());
						}
						return selectedOnly == SelectedOnly.YES ? gotoPrevSelected() : gotoNext();
					}
				}, false);
	}
	
	/**
	 * Returns the child documents of this entry in a specific range from the first lookup key
	 * until the last lookup key (inclusive)
	 * 
	 * @param entry the parent entry
	 * @param startKey the start key
	 * @param endKey the end key
	 * @param descending whether to return the documents in descending order
	 * @return child documents as stream
	 */
	public Stream<VirtualViewEntryData> childDocumentsBetween(VirtualViewEntryData entry, Object startKey, Object endKey, boolean descending) {
		ViewEntrySortKey lowCategorySortKey = ViewEntrySortKey.createScanKey(false, Arrays.asList(new Object[] {startKey, VirtualViewEntryData.LOW_SORTVAL}),
				VirtualViewEntryData.LOW_ORIGIN,
				0);
		ViewEntrySortKey highCategorySortKey = ViewEntrySortKey.createScanKey(false, Arrays.asList(new Object[] {endKey, VirtualViewEntryData.HIGH_SORTVAL}),
				VirtualViewEntryData.HIGH_ORIGIN,
				Integer.MAX_VALUE);
		
		ConcurrentNavigableMap<ViewEntrySortKey, VirtualViewEntryData> map = entry.getChildEntriesAsMap()
				.subMap(lowCategorySortKey, false, highCategorySortKey, false);
		if (descending) {
			map = map.descendingMap();
		}
		return map
				.values()
				.stream()
				.filter((currEntry) -> {
					return viewEntryAccessCheck.isVisible(currEntry);
				});
	}
	
	/**
	 * Returns the child documents of this entry with a specific key
	 * 
	 * @param entry the parent entry
	 * @param key key to search for
	 * @param isExact whether to search for an exact match or prefix match
	 * @param descending whether to return the documents in descending order
	 * @return child documents as stream
	 */
	public Stream<VirtualViewEntryData> childDocumentsByKey(VirtualViewEntryData entry, String key, boolean isExact, boolean descending) {		
		String startKey = key;
		String endKey;
		if (isExact) {
			endKey = key;
		}
		else {
			endKey = key + Character.MAX_VALUE;
		}
		
		return childDocumentsBetween(entry, startKey, endKey, descending);		
	}
	

	/**
	 * Returns the child documents of this entry
	 * 
	 * @param descending whether to return the documents in descending order
	 * @return child documents as stream
	 */
	public Stream<VirtualViewEntryData> childDocuments(VirtualViewEntryData entry, boolean descending) {
		ConcurrentNavigableMap<ViewEntrySortKey, VirtualViewEntryData> map = entry.getChildDocumentsAsMap();
		if (descending) {
			map = map.descendingMap();
		}
		return map
				.values()
				.stream()
				.filter((currEntry) -> {
					return viewEntryAccessCheck.isVisible(currEntry);
				});
	}
	
	
	/**
	 * Returns the child categories of this entry in a specific range from the first lookup key
	 * until the last lookup key (inclusive)
	 * 
	 * @param startKey the start key
	 * @param endKey the end key
	 * @param descending whether to return the categories in descending order
	 * @return child categories as stream
	 */
	public Stream<VirtualViewEntryData> childCategoriesBetween(VirtualViewEntryData entry, Object startKey, Object endKey, boolean descending) {
		ViewEntrySortKey lowCategorySortKey = ViewEntrySortKey.createScanKey(true, Arrays.asList(new Object[] {startKey, VirtualViewEntryData.LOW_SORTVAL}),
				VirtualViewEntryData.LOW_ORIGIN,
				0);
		ViewEntrySortKey highCategorySortKey = ViewEntrySortKey.createScanKey(true, Arrays.asList(new Object[] {endKey, VirtualViewEntryData.HIGH_SORTVAL}),
				VirtualViewEntryData.HIGH_ORIGIN,
				Integer.MAX_VALUE);
		
		ConcurrentNavigableMap<ViewEntrySortKey, VirtualViewEntryData> map = entry.getChildEntriesAsMap()
				.subMap(lowCategorySortKey, false, highCategorySortKey, false);
		if (descending) {
			map = map.descendingMap();
		}
		return map
				.values()
				.stream()
				.filter((currEntry) -> {
					return viewEntryAccessCheck.isVisible(currEntry);
				});
	}
	
	/**
	 * Returns the child documents of this entry with a specific key
	 * 
	 * @param entry the parent entry
	 * @param key key to search for
	 * @param isExact whether to search for an exact match or prefix match
	 * @param descending whether to return the categories in descending order
	 * @return child documents as stream
	 */
	public Stream<VirtualViewEntryData> childCategoriesByKey(VirtualViewEntryData entry, String key, boolean isExact, boolean descending) {		
		String startKey = key;
		String endKey;
		if (isExact) {
			endKey = key;
		}
		else {
			endKey = key + Character.MAX_VALUE;
		}
		
		return childCategoriesBetween(entry, startKey, endKey, descending);		
	}
	
	/**
	 * Returns the child categories of this entry
	 * 
	 * @param entry the parent entry
	 * @param descending whether to return the categories in descending order
	 * @return child categories as stream
	 */
	public Stream<VirtualViewEntryData> childCategories(VirtualViewEntryData entry, boolean descending) {
		ConcurrentNavigableMap<ViewEntrySortKey, VirtualViewEntryData> map = entry.getChildCategoriesAsMap();
		if (descending) {
			map = map.descendingMap();
		}
		return map
				.values()
				.stream()
				.filter((currEntry) -> {
					return viewEntryAccessCheck.isVisible(currEntry);
				});
	}
	
	/**
	 * Returns the child view entries in sorted order
	 * 
	 * @param descending whether to return the entries in descending order
	 * @return child entries as stream
	 */
	public Stream<VirtualViewEntryData> childEntries(VirtualViewEntryData entry, boolean descending) {
		ConcurrentNavigableMap<ViewEntrySortKey, VirtualViewEntryData> map = entry.getChildEntriesAsMap();
		if (descending) {
			map = map.descendingMap();
		}
		return map
				.values()
				.stream()
				.filter((currEntry) -> {
					return viewEntryAccessCheck.isVisible(currEntry);
				});
	}
	
	/**
	 * Navigates to the next entry in the view, taking the expand states into account
	 * 
	 * @return true if successful
	 */
	public boolean gotoNext() {
		VirtualViewEntryData currEntry = getCurrentEntry();
		if (currEntry == null) {
			return false;
		}
		boolean isExpanded = isExpanded(currEntry);
		int childCount = currEntry.getChildCount();
		
		if (isExpanded && childCount > 0) {
			//try to navigate to the first child of the current entry
			TraversalInfo traversalInfo = new TraversalInfo(currEntry, withCategories, withDocuments);
			if (traversalInfo.gotoFirst()) {
				//success
				currentEntryStack.push(traversalInfo);
				return true;
			}
		}

		//try to go to the next sibling
		if (gotoNextSibling()) {
			return true;
		}
		
		while (gotoParent()) {
			// go up one level, go to next sibling
			if (gotoNextSibling()) {
				return true;			
			}
			
			//repeat for cases where we are deeper in the tree
			//Entry #1
			//  Entry #1.1
			//    Entry #1.1.1
			//      Entry #1.1.1.1   <--
			//Entry #2
		}
		
		//no more entries in the view
		return false;
	}

	/**
	 * Navigates to the previous entry in the view, taking the expand states into account
	 * 
	 * @return true if successful
	 */
	public boolean gotoPrev() {
		VirtualViewEntryData currEntry = getCurrentEntry();
		if (currEntry == null) {
			return false;
		}
		
		if (gotoPrevSibling()) {
			//navigate to the deepest expanded descendant entry of the prev sibling
			VirtualViewEntryData prevSiblingEntry = getCurrentEntry();
			gotoDeepestExpandedDescendant(prevSiblingEntry);
			return true;
		}

		//navigate to parent
		if (gotoParent()) {
			return true;
		}
		
		return false;
	}

	/**
	 * Navigates to the deepest expanded descendant of a parent entry
	 * 
	 * @param entry parent entry
	 */
	private void gotoDeepestExpandedDescendant(VirtualViewEntryData entry) {
		if (entry.getChildCount() > 0 && isExpanded(entry)) {
			TraversalInfo traversalInfo = new TraversalInfo(entry, withCategories, withDocuments);
            if (traversalInfo.gotoLast()) {
                currentEntryStack.push(traversalInfo);
                gotoDeepestExpandedDescendant(traversalInfo.getCurrentEntry());
            }
		}
	}
	
	/**
	 * Moves the cursor to the next sibling in the view
	 * 
	 * @return true if we have a next sibling
	 */
	public boolean gotoNextSibling() {
		return currentEntryStack.peek().gotoNextSibling();
	}

	/**
	 * Moves the cursor to the previous sibling in the view
	 * 
	 * @return true if we have a next sibling
	 */
	public boolean gotoPrevSibling() {
		return currentEntryStack.peek().gotoPrevSibling();
	}

	/**
	 * Moves the cursor to the first entry of the view
	 * 
	 * @return true if successful, false if the view is empty
	 */
	public boolean gotoFirst() {
		if (currentEntryStack.isEmpty()) {
			return false;
		}
		
		//back to top level
		while (currentEntryStack.size() > 1) {
			currentEntryStack.pop();
		}
		return currentEntryStack.peek().gotoFirst();
	}
	
	/**
	 * Moves the cursor to the last entry of the view, taking the expand states into account
	 * 
	 * @return true if successful, false if the view is empty
	 */
	public boolean gotoLast() {
		if (currentEntryStack.isEmpty()) {
			return false;
		}
		
		//back to top level
		while (currentEntryStack.size() > 1) {
			currentEntryStack.pop();
		}
		
		if (currentEntryStack.peek().gotoLast()) {
			VirtualViewEntryData lastTopLevelEntry = getCurrentEntry();
			gotoDeepestExpandedDescendant(lastTopLevelEntry);

			return true;
		}
		
		return false;
	}
	
	/**
	 * Compares two position arrays like [1,2], [1,2,3] and [1,2,4]
	 */
	private static final Comparator<int[]> positionArrayComparator = new Comparator<int[]>() {
		@Override
		public int compare(int[] o1, int[] o2) {
			int len = Math.min(o1.length, o2.length);
			for (int i = 0; i < len; i++) {
				if (o1[i] != o2[i]) {
					return o1[i] - o2[i];
				}
			}
			return o1.length - o2.length;
		}
	};
	
	/**
	 * Returns all occurrences of a note id in the view in ascending order (sorted by position)
	 * 
	 * @param origin origin of the entry
	 * @param noteId note id of the entry
	 * @return stream of entries
	 */
	public Stream<VirtualViewEntryData> getSortedEntries(String origin, int noteId) {
		return view
				.getEntries(origin, noteId)
				.stream()
				.sorted((entry1, entry2) -> {
					int[] pos1 = entry1.getPosition();
					int[] pos2 = entry2.getPosition();
					
					return positionArrayComparator.compare(pos1, pos2);
				})
				.filter((currEntry) -> {
					return viewEntryAccessCheck.isVisible(currEntry);
				});
	}

	/**
	 * Returns all occurrences of a note id in the view in ascending order (sorted by position)
	 * 
	 * @param origin origin
	 * @param noteIds set of note ids
	 * @return stream of entries
	 */
	public Stream<VirtualViewEntryData> getSortedEntries(String origin, Set<Integer> noteIds) {
		//same as other getSortedEntries method, but for a single origin
		return noteIds
				.stream()
				.flatMap((noteId) -> {
					return view.getEntries(origin, noteId).stream();
				})
				.sorted((entry1, entry2) -> {
					int[] pos1 = entry1.getPosition();
					int[] pos2 = entry2.getPosition();
					
					return positionArrayComparator.compare(pos1, pos2);
				})
				.filter((currEntry) -> {
					return viewEntryAccessCheck.isVisible(currEntry);
				});
	}

	/**
	 * Sorts the note ids by position and returns them as a linked hash set
	 * 
	 * @param origin origin
	 * @param noteIds set of note ids
	 * @return linked hash set of note ids sorted by position
	 */
	public LinkedHashSet<Integer> getSortedNoteIds(String origin, Set<Integer> noteIds) {
		return noteIds
				.stream()
				.flatMap((noteId) -> {
					return view.getEntries(origin, noteId).stream();
				})
				.sorted((entry1, entry2) -> {
					int[] pos1 = entry1.getPosition();
					int[] pos2 = entry2.getPosition();

					return positionArrayComparator.compare(pos1, pos2);
				})
				.filter((currEntry) -> {
					return viewEntryAccessCheck.isVisible(currEntry);
				})
				.map((entry) -> {
					return entry.getNoteId();
				})
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}
	
	/**
	 * Sorts the note ids by position and returns them as a linked hash set
	 * 
	 * @param noteIds set of note ids with origin
	 * @return linked hash set of note ids sorted by position
	 */
	public LinkedHashSet<ScopedNoteId> getSortedNoteIds(Set<ScopedNoteId> noteIds) {
		return noteIds
				.stream()
				.flatMap((scopedNoteId) -> {
					return view.getEntries(scopedNoteId.getOrigin(), scopedNoteId.getNoteId()).stream();
				})
				.sorted((entry1, entry2) -> {
					int[] pos1 = entry1.getPosition();
					int[] pos2 = entry2.getPosition();

					return positionArrayComparator.compare(pos1, pos2);
				})
				.filter((currEntry) -> {
					return viewEntryAccessCheck.isVisible(currEntry);
				})
				.map((entry) -> {
					return new ScopedNoteId(entry.getOrigin(), entry.getNoteId());
				})
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Returns all occurrences of the specified note ids in the view in ascending order (sorted
	 * by position)
	 * 
	 * @param noteIds set of note ids with origin
	 * @return stream of entries
	 */
	public Stream<VirtualViewEntryData> getSortedEntries(Set<ScopedNoteId> noteIds) {
		//return Stream of VirtualViewEntryData sorted by VirtualViewEntryData.getPositionStr()
		return noteIds
				.stream()
				.flatMap((scopedNoteId) -> {
					return view.getEntries(scopedNoteId.getOrigin(), scopedNoteId.getNoteId()).stream();
				})
				.sorted((entry1, entry2) -> {
					int[] pos1 = entry1.getPosition();
					int[] pos2 = entry2.getPosition();
					
					return positionArrayComparator.compare(pos1, pos2);
				})
				.filter((currEntry) -> {
					return viewEntryAccessCheck.isVisible(currEntry);
				});
	}

	/**
	 * Returns the view entry at the cursor position
	 * 
	 * @return view entry or null if the cursor is offroad
	 */
	public VirtualViewEntryData getCurrentEntry() {
		if (currentEntryStack.isEmpty()) {
			return null;
		}
		else {
			VirtualViewEntryData currEntry = currentEntryStack.peek().getCurrentEntry();
			if (currEntry != null) {
				if (currEntry.isCategory() && withCategories) {
					return currEntry;
				}
				else if (currEntry.isDocument() && withDocuments) {
					return currEntry;
				}
			}

			return null;				
		}
	}

	/**
	 * Selects a view entry and optionally all parent categories
	 * 
	 * @param origin origin of the entry
	 * @param noteId note id of the entry
	 * @param selectParentCategories true to select all parent categories
	 * @return this navigator
	 */
	public VirtualViewNavigator select(String origin, int noteId, boolean selectParentCategories) {
		ScopedNoteId scopedNoteId = new ScopedNoteId(origin, noteId);
		if (selectAll) {
			//make sure this entry is not deselected
			selectedOrDeselectedEntries.remove(scopedNoteId);			
		}
		else {
			selectedOrDeselectedEntries.add(scopedNoteId);
		}
		
		if (selectParentCategories) {
			List<VirtualViewEntryData> entries = view.getEntries(origin, noteId);
			if (entries != null) {
				VirtualViewEntryData rootEntry = view.getRoot();
				
				for (VirtualViewEntryData currEntry : entries) {
					VirtualViewEntryData parent = currEntry.getParent();
					while (parent != null && !rootEntry.equals(parent)) {
						if (selectAll) {
							selectedOrDeselectedEntries.remove(new ScopedNoteId(parent.getOrigin(), parent.getNoteId()));
						}
						else {
							selectedOrDeselectedEntries.add(new ScopedNoteId(parent.getOrigin(), parent.getNoteId()));
						}
						parent = parent.getParent();
					}
				}
			}
		}
		return this;
	}
	
	/**
	 * Selects all view entries by default. Use {@link #deselect(String, int)} to remove
	 * entries from the selection
	 * 
	 * @return this navigator
	 */
	public VirtualViewNavigator selectAll() {
		selectedOrDeselectedEntries.clear();
		selectAll = true;
		return this;
	}

	/**
	 * Returns whether all entries are selected by default (after {@link #selectAll()} has been called)
	 * 
	 * @return true if all entries are selected by default
	 */
	public boolean isSelectAllByDefault() {
		return selectAll;
	}
	
	/**
	 * Returns whether all entries are deselected by default (after {@link #deselectAll()} has been called)
	 * 
	 * @return true if all entries are deselected by default
	 */
	public boolean isDeselectAllByDefault() {
		return !selectAll;
	}
	
	/**
	 * Clears the set of selection entries
	 * 
	 * @return this navigator
	 */
	public VirtualViewNavigator deselectAll() {
		selectedOrDeselectedEntries.clear();
		selectAll = false;
		return this;
	}
	
	/**
	 * Deselects a view entry
	 * 
	 * @param origin origin of the entry
	 * @param noteId note id of the entry
	 * @return this navigator
	 */
	public VirtualViewNavigator deselect(String origin, int noteId) {
		if (selectAll) {
			selectedOrDeselectedEntries.add(new ScopedNoteId(origin, noteId));			
		}
		else {
			selectedOrDeselectedEntries.remove(new ScopedNoteId(origin, noteId));
		}
		return this;
    }
	
	/**
	 * Returns a copy of the internal structure for selected entries or deselected entries if {@link #selectAll()} has been called
	 * 
	 * @return set of selected entries
	 */
	public Set<ScopedNoteId> getSelectedOrDeselectedEntries() {
		return new HashSet<>(selectedOrDeselectedEntries);
	}

	/**
	 * Bulk function to set selected / deselected entries
	 * 
	 * @param ids set of selected entries
	 * @return this navigator
	 */
	public VirtualViewNavigator setSelectedOrDeselectedEntries(Set<ScopedNoteId> ids) {
		selectedOrDeselectedEntries.clear();
		selectedOrDeselectedEntries.addAll(ids);
		return this;
	}
	
	/**
	 * Checks if a view entry is selected
	 * 
	 * @param origin origin of the entry
	 * @param noteId note id of the entry
	 * @return true if selected
	 */
	public boolean isSelected(String origin, int noteId) {
		if (selectAll) {
			return !selectedOrDeselectedEntries.contains(new ScopedNoteId(origin, noteId));
		} else {
			return selectedOrDeselectedEntries.contains(new ScopedNoteId(origin, noteId));
		}
	}
	
	/**
	 * Collapse all entries by default. Use {@link #expand(String, int)} and the other expand methods to expand specific entries.
	 * 
	 * @return this navigator
	 */
	public VirtualViewNavigator collapseAll() {
		expandedOrCollapsedEntries.clear();
		expandAllByDefault = false;
		return this;
	}
	
	/**
	 * Expand all entries by default. Use {@link #collapse(String, int)} and the other collapse methods to collapse specific entries.
	 * 
	 * @return this navigator
	 */
	public VirtualViewNavigator expandAll() {
		expandedOrCollapsedEntries.clear();
		expandAllByDefault = true;
		return this;
	}
	
	/**
	 * Expand all entries up to a specific level
	 * 
	 * @param level level to expand (0 to show top level only, 1 to expand the top level)
	 * @return this navigator
	 */
	public VirtualViewNavigator expandLevel(int level) {
		this.expandLevel = level;
		return this;
	}
	
	private int[] toPositionArray(String posStr, char delimiter) {
		String[] parts = posStr.split(Pattern.quote(String.valueOf(delimiter)));
		int[] pos = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			pos[i] = Integer.parseInt(parts[i]);
		}
		return pos;
	}
	
	/**
	 * Expand a view entry at a position
	 * 
	 * @param posStr position string like "1.2.3"
	 * @return this navigator
	 */
	public VirtualViewNavigator expand(String posStr) {
		int[] pos = toPositionArray(posStr, '.');
		expand(pos);
		return this;
	}
	
	/**
	 * Collapse a view entry at a position
	 * 
	 * @param posStr position string like "1.2.3"
	 * @return this navigator
	 */
	public VirtualViewNavigator collapse(String posStr) {
		int[] pos = toPositionArray(posStr, '.');
		collapse(pos);
		return this;
	}
	
	/**
	 * Expand a view entry at a position
	 * 
	 * @param pos position, e.g. [1,2,3]
	 * @return this navigator
	 */
	public VirtualViewNavigator expand(int[] pos) {
		Optional<VirtualViewEntryData> entry = getPos(pos, false);
		if (entry.isPresent()) {
			expand(entry.get().getOrigin(), entry.get().getNoteId());
		}
		return this;
	}
	
	/**
	 * Collapse a view entry at a position
	 * 
	 * @param pos position, e.g. [1,2,3]
	 * @return this navigator
	 */
	public VirtualViewNavigator collapse(int[] pos) {
		Optional<VirtualViewEntryData> entry = getPos(pos, false);
		if (entry.isPresent()) {
			collapse(entry.get().getOrigin(), entry.get().getNoteId());
		}
		return this;
	}
	
	/**
	 * Expand an entry
	 * 
	 * @param origin origin of the entry
	 * @param noteId note id of the entry
	 * @return this navigator
	 */
	public VirtualViewNavigator expand(String origin, int noteId) {
		if (!expandAllByDefault) {
			expandedOrCollapsedEntries.add(new ScopedNoteId(origin, noteId));			
		}
		else {
			// make sure this entry is not collapsed
			expandedOrCollapsedEntries.remove(new ScopedNoteId(origin, noteId));
		}
		return this;
	}
	
	/**
	 * Collapse an entry
	 * 
	 * @param origin origin of the entry
	 * @param noteId note id of the entry
	 * @return this navigator
	 */
	public VirtualViewNavigator collapse(String origin, int noteId) {
		if (expandAllByDefault) {
			expandedOrCollapsedEntries.add(new ScopedNoteId(origin, noteId));
		}
		else {
			//make sure this entry is not expanded
			expandedOrCollapsedEntries.remove(new ScopedNoteId(origin, noteId));
		}
		return this;
	}
	
	/**
	 * Checks if an entry is expanded
	 * 
	 * @param entry entry to check
	 * @return true if expanded
	 */
	public boolean isExpanded(VirtualViewEntryData entry) {
		if (isExpanded(entry.getOrigin(), entry.getNoteId())) {
			return true;
		}
		if (expandLevel > 0 && entry.getLevel() <= expandLevel) {
			return true;
		}
		return false;
	}
	
	/**
	 * Returns a copy of the internal structure that contains the expanded or collapsed entries (depending on
	 * the current expandAll state)
	 * 
	 * @return set of expanded or collapsed entries
	 */
	public Set<ScopedNoteId> getExpandedOrCollapsedEntries() {
		return new HashSet<>(expandedOrCollapsedEntries);
	}
	
	/**
	 * Bulk function to set expanded or collapsed entries
	 * 
	 * @param ids set of expanded or collapsed entries
	 * @return this navigator
	 */
	public VirtualViewNavigator setExpandedOrCollapsedEntries(Set<ScopedNoteId> ids) {
		expandedOrCollapsedEntries.clear();
		expandedOrCollapsedEntries.addAll(ids);
		return this;
	}
	
	/**
	 * Returns the current default expand policy (e.g. expand all by default or collapse all by default)
	 * 
	 * @return true if all entries are expanded by default
	 */
	public boolean isExpandAllByDefault() {
		return expandAllByDefault;
	}
	
	/**
	 * Returns the current default collapse policy (e.g. collapse all by default or expand all by default)
	 * 
	 * @return true if all entries are collapsed by default
	 */
	public boolean isCollapseAllByDefault() {
		return !expandAllByDefault;
	}
	
	/**
	 * Returns the current expand level
	 * 
	 * @return expand level
	 */
	public int getExpandLevel() {
		return expandLevel;
	}
	
	/**
	 * Checks if an entry is expanded based on its origin/noteId (does not check if expandLevel is set)
	 * 
	 * @param origin origin of the entry
	 * @param noteId note id of the entry
	 * @return true if expanded
	 */
	public boolean isExpanded(String origin, int noteId) {
		ScopedNoteId scopedNoteId = new ScopedNoteId(origin, noteId);
		
		if (expandAllByDefault && !expandedOrCollapsedEntries.contains(scopedNoteId)) { // expandedOrCollapsedEntries is collapsed list here
			return true;
		}
		else if (!expandAllByDefault && expandedOrCollapsedEntries.contains(scopedNoteId)) { // expandedOrCollapsedEntries is expanded list here
			return true;
		}
		else if (view.getRoot().getNoteId() == noteId && VirtualView.ORIGIN_VIRTUALVIEW.equals(origin)) {
			// root is always expanded
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Utility class to navigate within the child entries of a parent entry
	 */
	private class TraversalInfo {
		private VirtualViewEntryData parentEntry;
		private boolean withCategories;
		private boolean withDocuments;
		
		private Iterator<Entry<ViewEntrySortKey,VirtualViewEntryData>> childIterator;
		private Boolean childIteratorHasDirectionDown;
		
		private ViewEntrySortKey currentChildEntrySortKey;
		private VirtualViewEntryData currentChildEntry;
		
		public TraversalInfo(VirtualViewEntryData parentEntry, boolean withCategories, boolean withDocuments) {
			this.parentEntry = parentEntry;
			this.withCategories = withCategories;
			this.withDocuments = withDocuments;
		}

		@Override
		public String toString() {
			return "TraversalInfo [parentEntry=" + parentEntry + ", entryAtCursor=" + currentChildEntry + ", iteratorDirectionDown=" + childIteratorHasDirectionDown + "]";
		}
		
		/**
		 * Returns the view entry at the cursor position
		 * 
		 * @return view entry or null if the cursor is offroad
		 */
		public VirtualViewEntryData getCurrentEntry() {
			VirtualViewEntryData entry = getCurrentEntryUnchecked();
			if (viewEntryAccessCheck.isVisible(entry)) {
				return entry;
			}
			return null;
		}
		
		private VirtualViewEntryData getCurrentEntryUnchecked() {
			return currentChildEntry;
		}
		
		public boolean gotoFirst() {
			if (gotoFirstUnchecked()) {
				VirtualViewEntryData entry = getCurrentEntry();
				if (entry != null) {
					return true;
				}
			}
			
			return gotoNextSibling();
		}
		
		private boolean gotoFirstUnchecked() {
			if (withCategories && withDocuments) {
				this.childIterator = this.parentEntry.getChildEntriesAsMap().entrySet().iterator();
			}
			else if (withCategories) {
				this.childIterator = this.parentEntry.getChildCategoriesAsMap().entrySet().iterator();
			}
			else if (withDocuments) {
				this.childIterator = this.parentEntry.getChildDocumentsAsMap().entrySet().iterator();
			}
			else {
				this.childIterator = new EmptyIterator<>();
			}
			
			childIteratorHasDirectionDown = true;
			
			if (this.childIterator.hasNext()) {
				Entry<ViewEntrySortKey,VirtualViewEntryData> currChildEntry = this.childIterator.next();
				
				currentChildEntrySortKey = currChildEntry.getKey();
				currentChildEntry = currChildEntry.getValue();
				return true;
			}
			else {
				return false;
			}
		}
		
		public boolean gotoLast() {
			if (gotoLastUnchecked()) {
				VirtualViewEntryData entry = getCurrentEntry();
				if (entry != null) {
					return true;
				}
			}
			
			return gotoPrev();
		}
		
		private boolean gotoLastUnchecked() {
			if (withCategories && withDocuments) {
				this.childIterator = this.parentEntry.getChildEntriesAsMap().descendingMap().entrySet().iterator();
			}
			else if (withCategories) {
				this.childIterator = this.parentEntry.getChildCategoriesAsMap().descendingMap().entrySet().iterator();
			}
			else if (withDocuments) {
				this.childIterator = this.parentEntry.getChildDocumentsAsMap().descendingMap().entrySet().iterator();
			}
			else {
				this.childIterator = new EmptyIterator<>();
			}
			
			childIteratorHasDirectionDown = false;
			
			if (this.childIterator.hasNext()) {
				Entry<ViewEntrySortKey,VirtualViewEntryData> currChildEntry = this.childIterator.next();
				
				currentChildEntrySortKey = currChildEntry.getKey();
				currentChildEntry = currChildEntry.getValue();

				return true;
			}
			else {
				currentChildEntrySortKey = null;
				currentChildEntry = null;
				
				return false;
			}
		}
		
		/**
		 * Moves the cursor to the next sibling in the view if possible
		 * 
		 * @return true if successful, false if no more siblings are available (then we don't change the cursor position)
		 */
		public boolean gotoNextSibling() {
			//repeat until we find an entry that we are allowed to see
			while (gotoNextSiblingUnchecked()) {
				VirtualViewEntryData entry = getCurrentEntry();
				if (entry != null) {
					return true;
				}
			}
			return false;
		}
		
		/**
		 * Moves the cursor to the next sibling in the view if possible
		 * 
		 * @return true if successful, false if no more siblings are available (then we don't change the cursor position)
		 */
		private boolean gotoNextSiblingUnchecked() {
			if (childIteratorHasDirectionDown) {
				//already moving down
				if (this.childIterator.hasNext()) {
					Entry<ViewEntrySortKey,VirtualViewEntryData> currChildEntry = this.childIterator.next();
					
					currentChildEntrySortKey = currChildEntry.getKey();
					currentChildEntry = currChildEntry.getValue();

					return true;
				} else {
					return false;
				}
			} else {
				//switch direction (move down now) and start iterator at current key
				if (this.currentChildEntrySortKey == null) {
					return false;
				}
				
				this.childIterator = this.parentEntry.getChildEntriesAsMap().tailMap(currentChildEntrySortKey, false).entrySet().iterator();
				childIteratorHasDirectionDown = true;
				if (this.childIterator.hasNext()) {
					Entry<ViewEntrySortKey,VirtualViewEntryData> currChildEntry = this.childIterator.next();
					
					currentChildEntrySortKey = currChildEntry.getKey();
					currentChildEntry = currChildEntry.getValue();

					return true;
				}
				else {
					return false;
				}
			}
		}

		public boolean gotoPrevSibling() {
			//repeat until we find an entry that we are allowed to see
			while (gotoPrevSiblingUnchecked()) {
				VirtualViewEntryData entry = getCurrentEntry();
				if (entry != null) {
					return true;
				}
			}
			return false;
		}
		
		/**
		 * Moves the cursor to the previous sibling in the view if possible
		 * 
		 * @return true if successful, false if no more siblings are available (then we don't change the cursor position)
		 */
		private boolean gotoPrevSiblingUnchecked() {
			if (!childIteratorHasDirectionDown) {
				// already moving up
				if (this.childIterator.hasNext()) {
					Entry<ViewEntrySortKey,VirtualViewEntryData> currChildEntry = this.childIterator.next();
					
					currentChildEntrySortKey = currChildEntry.getKey();
					currentChildEntry = currChildEntry.getValue();

					return true;
				} else {
					return false;
				}
			} else {
				// switch direction (move up now) and start iterator at current key
				if (this.currentChildEntrySortKey == null) {
					return false;
				}
				this.childIterator = this.parentEntry.getChildEntriesAsMap().headMap(currentChildEntrySortKey, false)
						.descendingMap().entrySet().iterator();
				childIteratorHasDirectionDown = false;
				if (this.childIterator.hasNext()) {
					Entry<ViewEntrySortKey,VirtualViewEntryData> currChildEntry = this.childIterator.next();
					
					currentChildEntrySortKey = currChildEntry.getKey();
					currentChildEntry = currChildEntry.getValue();

					return true;
				} else {
					return false;
				}
			}
		}
		
	}

}
