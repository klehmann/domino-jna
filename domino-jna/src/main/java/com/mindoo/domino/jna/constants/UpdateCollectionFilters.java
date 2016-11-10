package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.NotesCollection;

/**
 * Constants to be used for {@link NotesCollection#updateFilters(EnumSet)}.<br>
 * <br>
 * Specifies which information needs to be resend to a remote server, because it
 * has been changed locally.
 * 
 * @author Karsten Lehmann
 */
public enum UpdateCollectionFilters {
	
	/** UnreadList has been modified */
	FILTER_UNREAD(0x0001),

	/** CollpasedList has been modified */
	FILTER_COLLAPSED(0x0002),

	/** SelectedList has been modified */
	FILTER_SELECTED(0x0004),

	/** UNID table has been modified. */
	FILTER_UNID_TABLE(0x0008),

	/** Conditionaly do FILTER_UNREAD if current unread list indicates it - see NSFDbUpdateUnread */
	FILTER_UPDATE_UNREAD(0x0010),

	/** Mark specified ID table Read */
	FILTER_MARK_READ(0x0020),

	/** Mark specified ID table Unread */
	FILTER_MARK_UNREAD(0x0040),

	/** Mark all read */
	FILTER_MARK_READ_ALL(0x0080),

	/** Mark all unread */
	FILTER_MARK_UNREAD_ALL(0x0100);

	private int m_val;

	UpdateCollectionFilters(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}

	public static short toBitMask(EnumSet<UpdateCollectionFilters> openFlagSet) {
		int result = 0;
		if (openFlagSet != null) {
			for (UpdateCollectionFilters currFlag : values()) {
				if (openFlagSet.contains(currFlag)) {
					result = result | currFlag.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}
}
