package com.mindoo.domino.jna.virtualviews;

import java.util.List;
import java.util.Objects;

/**
 * This sort key is used to sort {@link VirtualViewEntryData} objects within one level of the
 * {@link VirtualView} tree structure. It sorts category elements at the top followed by documents and
 * contains the values of the columns that are used for sorting, followed by the origin and note id.
 */
public class ViewEntrySortKey {
	
	public static ViewEntrySortKey createScanKey(boolean isCategory, List<Object> values, String origin, int noteId) {
	    return new ViewEntrySortKey(isCategory, values, origin, noteId, true);
	}

	public static ViewEntrySortKey createSortKey(boolean isCategory, List<Object> values, String origin, int noteId) {
		return new ViewEntrySortKey(isCategory, values, origin, noteId, false);
	}

	private List<Object> values;
	private boolean isCategory;
	private String origin;
	private int noteId;
	private int hashCode;
	private boolean isScanKey;
	
	public ViewEntrySortKey(boolean isCategory, List<Object> values, String origin, int noteId, boolean isScanKey) {
		this.isCategory = isCategory;
		this.values = values;
		this.origin = origin;
		this.noteId = noteId;
		this.isScanKey = isScanKey;
	}
	
	public boolean isCategory() {
		return isCategory;
	}
	
	public String getOrigin() {
		return origin;
	}
	
	public int getNoteId() {
		return noteId;
	}
	
	public List<Object> getValues() {
		return values;
	}
	
	public boolean isScanKey() {
		return isScanKey;
	}
	
	@Override
	public String toString() {
		return "ViewEntrySortKey [type=" + (isCategory ? "category" : "document") + ", values=" + values + ", origin=" + origin + ", noteId=" + noteId + "]";
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = Objects.hash(isCategory, values, noteId, origin);
		}
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
		ViewEntrySortKey other = (ViewEntrySortKey) obj;
		return Objects.equals(isCategory, other.isCategory) &&
				Objects.equals(noteId, other.noteId) &&
				Objects.equals(origin, other.origin) &&
				Objects.equals(values, other.values);
	}
	
}
