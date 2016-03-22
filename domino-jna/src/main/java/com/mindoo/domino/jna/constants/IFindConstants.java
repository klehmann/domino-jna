package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.NotesCollection;

/**
 * These flags are used by {@link NotesCollection#findByKey(short, Object...)} (NIFFindByKey)
 * and {@link NotesCollection#findByName(String, short)} (NIFFindByName) to control how the
 * view is searched for the key. The flags, FIND_PARTIAL, FIND_CASE_INSENSITIVE,
 * and FIND_ACCENT_INSENSITIVE should only be used for character data, since a
 * "partial number" or "partial date" is not well defined.<br>
 * <br>
 * The FIND_LESS_THAN and FIND_GREATER_THAN flags refer to the way the sorted
 * column keys are ordered and displayed, not the way they compare with each
 * other. FIND_LESS_THAN means "find the entry before" and FIND_GREATER_THAN
 * means "find the entry after" a desired key. The FIND_LESS_THAN and
 * FIND_GREATER_THAN flags will result in success if at least one key that
 * is less than or greater than the specified key, respectively, is found.<br>
 * <br>
 * If a FIND_FIRST_EQUAL or FIND_LAST_EQUAL comparison is specified and the
 * number of matches is requested to be returned, then the number of entries
 * which match the specified key will be returned. If a FIND_LESS_THAN or
 * FIND_GREATER_THAN comparison is specified, then the number of matching entries cannot be requested.
 * 
 * @author Karsten Lehmann
 */
public interface IFindConstants {

	/** Match only the initial characters ("T" matches "Tim", "i" does
	 * not match "Tim"). If multiple keys are used, a partial match is done on
	 * each of the keys in the order that they are specified. A partial match
	 * must be found for all specified keys in order for a particular entry
	 * to be considered a successful match. */
	public static final short FIND_PARTIAL	= 0x0001;
	/** Case insensitive ("tim" matches "Tim") */
	public static final short FIND_CASE_INSENSITIVE = 0x0002;
	/** Return up to MAXDWORD number of matching notes. If not specified,
	 * return up to MAXWORD number of matching notes. */
	public static final short FIND_RETURN_DWORD = 0x0004;
	/** Search disregards diacritical marks. */
	public static final short FIND_ACCENT_INSENSITIVE = 0x0008;
	/** If key is not found, update collection and search again */
	public static final short FIND_UPDATE_IF_NOT_FOUND = 0x0020;

	/* At most one of the following four flags should be specified */
	
	/** Find last entry less than the key value. (Specify no more than one of:
	 * FIND_LESS_THAN, FIND_FIRST_EQUAL, FIND_LAST_EQUAL, FIND_GREATER_THAN) */
	public static final short FIND_LESS_THAN = 0x0040;
	/** Find first entry equal to the key value (if more than one). This flag
	 * is the default. (Specify no more than one of: FIND_LESS_THAN, FIND_FIRST_EQUAL,
	 * FIND_LAST_EQUAL, FIND_GREATER_THAN) */
	public static final short FIND_FIRST_EQUAL = 0x0000;
	/** Find last entry equal to the key value (if more than one). (Specify no
	 * more than one of: FIND_LESS_THAN, FIND_FIRST_EQUAL, FIND_LAST_EQUAL, FIND_GREATER_THAN) */
	public static final short FIND_LAST_EQUAL = 0x0080;
	/** Find first entry greater than the key value. (Specify no more than one of:
	 * FIND_LESS_THAN, FIND_FIRST_EQUAL, FIND_LAST_EQUAL, FIND_GREATER_THAN) */
	public static final short FIND_GREATER_THAN = 0x00C0;
	/** Qualifies LESS_THAN and GREATER_THAN to mean LESS_THAN_OR_EQUAL and GREATER_THAN_OR_EQUAL */
	public static final short FIND_EQUAL = 0x0800;
	/** Bitmask of the comparison flags defined above */
	public static final short FIND_COMPARE_MASK = 0x08C0;

	/** Overlapping ranges match, and values within a range match. This symbol is
	 * valid for fields of type TYPE_TIME_RANGE and TYPE_NUMBER_RANGE. Therefore
	 * it should only be used with NIFFindByKey. */
	public static final short FIND_RANGE_OVERLAP = 0x0100;
	/** Return any entry representing an actual document
	 * (a non-category entry), instead of searching for the first
	 * (or last) entry in the category. It is unpredictable exactly
	 * which entry you will get. A count of the matched document and
	 * any subsequent documents that match is returned. This count
	 * may be less than the actual number of documents that match. */
	public static final short FIND_RETURN_ANY_NON_CATEGORY_MATCH = 0x0200;
	/** Only match non-category entries */
	public static final short FIND_NONCATEGORY_ONLY = 0x0400;
	/** Read and buffer matches NIFFindByKeyExtended2 only */
	public static final short FIND_AND_READ_MATCHES = 0x2000;

}
