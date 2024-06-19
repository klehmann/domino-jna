package com.mindoo.domino.jna.virtualviews;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.mindoo.domino.jna.utils.EmptyIterator;
import com.mindoo.domino.jna.virtualviews.VirtualView.ScopedNoteId;
import com.mindoo.domino.jna.virtualviews.security.ViewEntryAccessCheck;

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
	private VirtualView view;
	private boolean withCategories;
	private boolean withDocuments;
	private ViewEntryAccessCheck viewEntryAccessCheck;
	
	/** if selectAll==true, this set is treated as deselected list */
	private Set<ScopedNoteId> selectedOrDeselectedEntries = ConcurrentHashMap.newKeySet();
	private boolean selectAll = false;
	
	/** if expandAll==true, this set is treated as collapsed list */
	private Set<ScopedNoteId> expandedOrCollapsedEntries = ConcurrentHashMap.newKeySet();
	private boolean expandAll = false;

	private Stack<TraversalInfo> currentEntryStack;
	
	/**
	 * Creates a new view navigator
	 * 
	 * @param view view
	 * @param cats whether to include category entries
	 * @param docs whether to include document entries
	 * @param userNamesListByOrigin map with user names, groups and roles for each origin (each {@link VirtualViewEntry} is checked against the ACL of the origin database)
	 */
	public VirtualViewNavigator(VirtualView view, WithCategories cats, WithDocuments docs,
			ViewEntryAccessCheck viewEntryAccessCheck) {
		this.view = view;
		this.withCategories = cats == WithCategories.YES;
		this.withDocuments = docs == WithDocuments.YES;
		if (!withCategories && !withDocuments) {
			throw new IllegalArgumentException("The view navigator must contain categories, documents or both");
		}
		this.viewEntryAccessCheck = viewEntryAccessCheck;
		TraversalInfo traversalInfo = new TraversalInfo(view.getRoot(), withCategories, withDocuments);
		this.currentEntryStack = new Stack<>();
		this.currentEntryStack.push(traversalInfo);
	}
	
	/**
	 * Moves the cursor to a position in the view
	 * 
	 * @param posStr position string e.g. "1.2.3"
	 * @param delimiter delimiter in position string
	 * @return true if position could be found
	 */
	public boolean gotoPos(String posStr, char delimiter) {
		int[] pos = toPositionArray(posStr, delimiter);
		return gotoPos(pos);
	}

	/**
	 * Moves the cursor to a position in the view
	 * 
	 * @param pos new position
	 * @return true if position could be found
	 */
	public boolean gotoPos(int[] pos) {
		Optional<VirtualViewEntry> entry = getPos(pos, true);
		return entry.isPresent();
	}
	
	/**
	 * Returns the view entry at the specified position
	 * 
	 * @param posStr position string e.g. "1.2.3"
	 * @param delimiter delimiter in position string
	 * @return entry if found or empty
	 */
	public Optional<VirtualViewEntry> getPos(String posStr, char delimiter) {
		int[] pos = toPositionArray(posStr, delimiter);
		return getPos(pos);
	}
	
	/**
	 * Returns the view entry at the specified position
	 * 
	 * @param pos new position, e.g. [1,2,3]
	 * @return entry if found or empty
	 */
	public Optional<VirtualViewEntry> getPos(int[] pos) {
		return getPos(pos, false);
	}
	
	/**
	 * Moves the cursor to a position in the view
	 * 
	 * @param pos new position
	 * @param moveCursor true to move the cursor to the new position
	 * @return entry if found
	 */
	private Optional<VirtualViewEntry> getPos(int[] pos, boolean moveCursor) {
		Stack<TraversalInfo> newCurrentEntryStack = new Stack<>();
		
		VirtualViewEntry parentEntry = view.getRoot();
		TraversalInfo traversalInfo = new TraversalInfo(parentEntry, withCategories, withDocuments);
		newCurrentEntryStack.push(traversalInfo);
		
		for (int i=0; i<pos.length; i++) {						
			if (traversalInfo.gotoFirst()) {
				VirtualViewEntry matchingEntry = null;
				do {
					VirtualViewEntry currSearchEntry = traversalInfo.getCurrentEntry();
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
		VirtualViewEntry currEntry = getCurrentEntry();
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
		VirtualViewEntry currEntry = getCurrentEntry();
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
		if (!view.getRoot().equals(currentEntryStack.peek().parentEntry)) {
			currentEntryStack.pop();
			return true;
		}
		return false;
	}
	
	/**
	 * Navigates to the next selected entry in the view, taking the expand states into account
	 * 
	 * @return true if successful
	 * @see #select(String, int, boolean)
	 */
	public boolean gotoNextSelected() {
		while (gotoNext()) {
			VirtualViewEntry currEntry = getCurrentEntry();

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
			VirtualViewEntry currEntry = getCurrentEntry();

			if (isSelected(currEntry.getOrigin(), currEntry.getNoteId())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Navigates to the next entry in the view, taking the expand states into account
	 * 
	 * @return true if successful
	 */
	public boolean gotoNext() {
		VirtualViewEntry currEntry = getCurrentEntry();
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
		
		if (gotoParent()) {
			// go up one level, go to next sibling
			if (gotoNextSibling()) {
				return true;
			}
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
		VirtualViewEntry currEntry = getCurrentEntry();
		if (currEntry == null) {
			return false;
		}
		
		if (gotoPrevSibling()) {
			//navigate to the deepest expanded descendant entry of the prev sibling
			VirtualViewEntry prevSiblingEntry = getCurrentEntry();
			gotoDeepestExpandedDescendant(prevSiblingEntry);
			return true;
		}

		if (gotoParent()) {
			//navigate to parent
			VirtualViewEntry parentEntry = getCurrentEntry();
			gotoDeepestExpandedDescendant(parentEntry);
			return true;
		}
		
		return false;
	}

	/**
	 * Navigates to the deepest expanded descendant of a parent entry
	 * 
	 * @param entry parent entry
	 */
	private void gotoDeepestExpandedDescendant(VirtualViewEntry entry) {
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
		//back to top level
		while (currentEntryStack.size() > 1) {
			currentEntryStack.pop();
		}
		
		if (currentEntryStack.peek().gotoLast()) {
			VirtualViewEntry lastTopLevelEntry = getCurrentEntry();
			gotoDeepestExpandedDescendant(lastTopLevelEntry);

			return true;
		}
		
		return false;
	}
	
	/**
	 * Returns the view entry at the cursor position
	 * 
	 * @return view entry or null if the cursor is offroad
	 */
	public VirtualViewEntry getCurrentEntry() {
		if (currentEntryStack.isEmpty()) {
			return null;
		}
		else {
			VirtualViewEntry currEntry = currentEntryStack.peek().getCurrentEntry();
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
	 */
	public void select(String origin, int noteId, boolean selectParentCategories) {
		VirtualViewEntry rootEntry = view.getRoot();
		
		ScopedNoteId scopedNoteId = new ScopedNoteId(origin, noteId);
		if (selectAll) {
			//make sure this entry is not deselected
			selectedOrDeselectedEntries.remove(scopedNoteId);			
		}
		else {
			selectedOrDeselectedEntries.add(scopedNoteId);
		}
		
		if (selectParentCategories) {
			List<VirtualViewEntry> entries = view.findEntries(origin, noteId);
			if (entries != null) {
				for (VirtualViewEntry currEntry : entries) {
					VirtualViewEntry parent = currEntry.getParent();
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
	}
	
	/**
	 * Selects all view entries. Use {@link #deselect(String, int)} to remove
	 * entries from the selection
	 */
	public void selectAll() {
		selectedOrDeselectedEntries.clear();
		selectAll = true;
	}
	
	/**
	 * Clears the set of selection entries
	 */
	public void deselectAll() {
		selectedOrDeselectedEntries.clear();
		selectAll = false;	
	}
	
	/**
	 * Deselects a view entry
	 * 
	 * @param origin origin of the entry
	 * @param noteId note id of the entry
	 */
	public void deselect(String origin, int noteId) {
		if (selectAll) {
			selectedOrDeselectedEntries.add(new ScopedNoteId(origin, noteId));			
		}
		else {
			selectedOrDeselectedEntries.remove(new ScopedNoteId(origin, noteId));
		}
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
	 * Collapse all entries
	 */
	public void collapseAll() {
		expandedOrCollapsedEntries.clear();
		expandAll = false;
	}
	
	/**
	 * Expand all entries
	 */
	public void expandAll() {
		expandedOrCollapsedEntries.clear();
		expandAll = true;
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
	 * @param delimiter delimiter used in the position string like '.'
	 */
	public void expand(String posStr, char delimiter) {
		int[] pos = toPositionArray(posStr, delimiter);
		expand(pos);
	}
	
	/**
	 * Collapse a view entry at a position
	 * 
	 * @param posStr position string like "1.2.3"
	 * @param delimiter delimiter used in the position string like '.'
	 */
	public void collapse(String posStr, char delimiter) {
		int[] pos = toPositionArray(posStr, delimiter);
		collapse(pos);
	}
	
	/**
	 * Expand a view entry at a position
	 * 
	 * @param pos position, e.g. [1,2,3]
	 */
	public void expand(int[] pos) {
		Optional<VirtualViewEntry> entry = getPos(pos, false);
		if (entry.isPresent()) {
			expand(entry.get().getOrigin(), entry.get().getNoteId());
		}
	}
	
	/**
	 * Collapse a view entry at a position
	 * 
	 * @param pos position, e.g. [1,2,3]
	 */
	public void collapse(int[] pos) {
		Optional<VirtualViewEntry> entry = getPos(pos, false);
		if (entry.isPresent()) {
			collapse(entry.get().getOrigin(), entry.get().getNoteId());
		}
	}
	
	/**
	 * Expand an entry
	 * 
	 * @param origin origin of the entry
	 * @param noteId note id of the entry
	 */
	public void expand(String origin, int noteId) {
		if (!expandAll) {
			expandedOrCollapsedEntries.add(new ScopedNoteId(origin, noteId));			
		}
		else {
			// make sure this entry is not collapsed
			expandedOrCollapsedEntries.remove(new ScopedNoteId(origin, noteId));
		}
	}
	
	/**
	 * Collapse an entry
	 * 
	 * @param origin origin of the entry
	 * @param noteId note id of the entry
	 */
	public void collapse(String origin, int noteId) {
		if (expandAll) {
			expandedOrCollapsedEntries.add(new ScopedNoteId(origin, noteId));
		}
		else {
			//make sure this entry is not expanded
			expandedOrCollapsedEntries.remove(new ScopedNoteId(origin, noteId));
		}
	}
	
	/**
	 * Checks if an entry is expanded
	 * 
	 * @param entry entry to check
	 * @return true if expanded
	 */
	public boolean isExpanded(VirtualViewEntry entry) {
		return isExpanded(entry.getOrigin(), entry.getNoteId());
	}
	
	/**
	 * Checks if an entry is expanded
	 * 
	 * @param origin origin of the entry
	 * @param noteId note id of the entry
	 * @return true if expanded
	 */
	public boolean isExpanded(String origin, int noteId) {
		ScopedNoteId scopedNoteId = new ScopedNoteId(origin, noteId);
		
		if (expandAll && !expandedOrCollapsedEntries.contains(scopedNoteId)) { // expandedOrCollapsedEntries is collapsed list here
			return true;
		}
		else if (!expandAll && expandedOrCollapsedEntries.contains(scopedNoteId)) { // expandedOrCollapsedEntries is expanded list here
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
		private VirtualViewEntry parentEntry;
		private boolean withCategories;
		private boolean withDocuments;
		
		private Iterator<Entry<ViewEntrySortKey,VirtualViewEntry>> childIterator;
		private Boolean childIteratorHasDirectionDown;
		
		private ViewEntrySortKey currentChildEntrySortKey;
		private VirtualViewEntry currentChildEntry;
		
		public TraversalInfo(VirtualViewEntry parentEntry, boolean withCategories, boolean withDocuments) {
			this.parentEntry = parentEntry;
			this.withCategories = withCategories;
			this.withDocuments = withDocuments;
		}

		/**
		 * Returns the view entry at the cursor position
		 * 
		 * @return view entry or null if the cursor is offroad
		 */
		public VirtualViewEntry getCurrentEntry() {
			VirtualViewEntry entry = getCurrentEntryUnchecked();
			if (viewEntryAccessCheck.isVisible(entry)) {
				return entry;
			}
			return null;
		}
		
		private VirtualViewEntry getCurrentEntryUnchecked() {
			return currentChildEntry;
		}
		
		public boolean gotoFirst() {
			if (gotoFirstUnchecked()) {
				VirtualViewEntry entry = getCurrentEntry();
				if (entry != null) {
					return true;
				}
			}
			
			return gotoNextSibling();
		}
		
		private boolean gotoFirstUnchecked() {
			if (withCategories && withDocuments) {
				this.childIterator = this.parentEntry.getChildEntries().entrySet().iterator();
			}
			else if (withCategories) {
				this.childIterator = this.parentEntry.getCategories().entrySet().iterator();
			}
			else if (withDocuments) {
				this.childIterator = this.parentEntry.getDocuments().entrySet().iterator();
			}
			else {
				this.childIterator = new EmptyIterator<>();
			}
			
			childIteratorHasDirectionDown = true;
			
			if (this.childIterator.hasNext()) {
				Entry<ViewEntrySortKey,VirtualViewEntry> currChildEntry = this.childIterator.next();
				
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
				VirtualViewEntry entry = getCurrentEntry();
				if (entry != null) {
					return true;
				}
			}
			
			return gotoPrev();
		}
		
		private boolean gotoLastUnchecked() {
			if (withCategories && withDocuments) {
				this.childIterator = this.parentEntry.getChildEntries().descendingMap().entrySet().iterator();
			}
			else if (withCategories) {
				this.childIterator = this.parentEntry.getCategories().descendingMap().entrySet().iterator();
			}
			else if (withDocuments) {
				this.childIterator = this.parentEntry.getDocuments().descendingMap().entrySet().iterator();
			}
			else {
				this.childIterator = new EmptyIterator<>();
			}
			
			childIteratorHasDirectionDown = false;
			
			if (this.childIterator.hasNext()) {
				Entry<ViewEntrySortKey,VirtualViewEntry> currChildEntry = this.childIterator.next();
				
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
				VirtualViewEntry entry = getCurrentEntry();
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
					Entry<ViewEntrySortKey,VirtualViewEntry> currChildEntry = this.childIterator.next();
					
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
				
				this.childIterator = this.parentEntry.getChildEntries().tailMap(currentChildEntrySortKey, false).entrySet().iterator();
				childIteratorHasDirectionDown = true;
				if (this.childIterator.hasNext()) {
					Entry<ViewEntrySortKey,VirtualViewEntry> currChildEntry = this.childIterator.next();
					
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
				VirtualViewEntry entry = getCurrentEntry();
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
					Entry<ViewEntrySortKey,VirtualViewEntry> currChildEntry = this.childIterator.next();
					
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
				this.childIterator = this.parentEntry.getChildEntries().headMap(currentChildEntrySortKey, false)
						.descendingMap().entrySet().iterator();
				childIteratorHasDirectionDown = false;
				if (this.childIterator.hasNext()) {
					Entry<ViewEntrySortKey,VirtualViewEntry> currChildEntry = this.childIterator.next();
					
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
