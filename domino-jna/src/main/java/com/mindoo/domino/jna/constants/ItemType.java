package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

/**
 * These flags define the characteristics of an item (field) in a note.<br>
 * The flags may be or'ed together for combined functionality.
 * 
 * @author Karsten Lehmann
 */
public enum ItemType {
	
	/** This field will be signed if requested */
	SIGN(0x0001),
	
	/** This item is sealed. When used in NSFItemAppend, the item is encryption enabled;
	 * it can later be encrypted if edited from the Notes UI and saved in a form that specifies Encryption. */
	SEAL(0x0002),
	
	/** This item is stored in the note's summary buffer. Summary items may be used in view columns,
	 * selection formulas, and @functions. Summary items may be accessed via the SEARCH_MATCH
	 * structure provided by NSFSearch or in the buffer returned by NIFReadEntries.
	 * API program may read, modify, and write items in the summary buffer without opening
	 * the note first. The maximum size of the summary buffer is 32K.<br>
	 * Items of TYPE_COMPOSITE may not have the ITEM_SUMMARY flag set. */
	SUMMARY(0x0004),
	
	/** This item is an Author Names field as indicated by the READ/WRITE-ACCESS flag.
	 * Item is TYPE_TEXT or TYPE_TEXT_LIST. Author Names fields have the ITEM_READWRITERS flag
	 * or'd with the ITEM_NAMES flag.
	 */
	READWRITERS(0x0020),
	
	/** This item is a Names field. Indicated by the NAMES (distinguished names) flag.
	 * Item is TYPE_TEXT or TYPE_TEXT_LIST. */
	NAMES(0x0040),
	
	/**
	 * Item will not be written to disk
	 */
	NOUPDATE(0x0080),
	
	/** This item is a placeholder field in a form note. Item is TYPE_INVALID_OR_UNKNOWN. */
	PLACEHOLDER(0x100),
	
	/** A user requires editor access to change this field. */
	PROTECTED(0x0200),
	
	/** This is a Reader Names field. Indicated by the READER-ACCESS flag. Item is TYPE_TEXT or TYPE_TEXT_LIST. */
	READERS(0x400),
	
	/** Item is same as on-disk  */
	UNCHANGED(0x1000),
	
	/** Special flag to keep \n in string item values instead of replacing them with \0 
	 * (e.g. required when writing $$FormScript in design elements) */
	KEEPLINEBREAKS(0x8000000);
	
	private int m_val;
	
	ItemType(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static short toBitMask(EnumSet<ItemType> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (ItemType currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}
	
	public static int toBitMaskInt(EnumSet<ItemType> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (ItemType currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
