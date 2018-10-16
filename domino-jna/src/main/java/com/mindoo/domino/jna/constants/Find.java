package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.NotesCollection;

/**
 * These flags are used by {@link NotesCollection#findByKey(EnumSet, Object...)} (NIFFindByKey)
 * and {@link NotesCollection#findByName(String, EnumSet)} (NIFFindByName) to control how the
 * view is searched for the key. The flags, {@link #PARTIAL}, {@link #CASE_INSENSITIVE},
 * and {@link #ACCENT_INSENSITIVE} should only be used for character data, since a
 * "partial number" or "partial date" is not well defined.<br>
 * <br>
 * The {@link #LESS_THAN} and {@link #GREATER_THAN} flags refer to the way the sorted
 * column keys are ordered and displayed, not the way they compare with each
 * other. {@link #LESS_THAN} means "find the entry before" and {@link #GREATER_THAN} 
 * means "find the entry after" a desired key. The {@link #LESS_THAN} and
 * {@link #GREATER_THAN} flags will result in success if at least one key that
 * is less than or greater than the specified key, respectively, is found.<br>
 * <br>
 * If a {@link #FIRST_EQUAL} or {@link #LAST_EQUAL} comparison is specified and the
 * number of matches is requested to be returned, then the number of entries
 * which match the specified key will be returned. If a {@link #LESS_THAN} or
 * {@link #GREATER_THAN} comparison is specified, then the number of matching 
 * entries cannot be requested.
 * 
 * @author Karsten Lehmann
 */
public enum Find {
	/** Match only the initial characters ("T" matches "Tim", "i" does
	 * not match "Tim"). If multiple keys are used, a partial match is done on
	 * each of the keys in the order that they are specified. A partial match
	 * must be found for all specified keys in order for a particular entry
	 * to be considered a successful match. */
	PARTIAL(0x0001),
	
	/** Case insensitive ("tim" matches "Tim") */
	CASE_INSENSITIVE(0x0002),
	
	/** Return up to MAXDWORD number of matching notes. If not specified,
	 * return up to MAXWORD number of matching notes. */
	RETURN_DWORD(0x0004),
	
	/** Search disregards diacritical marks. */
	ACCENT_INSENSITIVE(0x0008),
	
	/** If key is not found, update collection and search again */
	UPDATE_IF_NOT_FOUND(0x0020),

	/* At most one of the following four flags should be specified */
	
	/** Find last entry less than the key value. (Specify no more than one of:
	 * LESS_THAN, FIRST_EQUAL, LAST_EQUAL, GREATER_THAN) */
	LESS_THAN(0x0040),
	
	/** Find first entry equal to the key value (if more than one). This flag
	 * is the default. (Specify no more than one of: LESS_THAN, FIRST_EQUAL,
	 * LAST_EQUAL, GREATER_THAN) */
	FIRST_EQUAL(0x0000),
	/** Find last entry equal to the key value (if more than one). (Specify no
	 * more than one of: LESS_THAN, FIRST_EQUAL, LAST_EQUAL, GREATER_THAN) */
	LAST_EQUAL(0x0080),
	
	/** Find first entry greater than the key value. (Specify no more than one of:
	 * LESS_THAN, FIRST_EQUAL, LAST_EQUAL, GREATER_THAN) */
	GREATER_THAN(0x00C0),
	
	/** Qualifies LESS_THAN and GREATER_THAN to mean LESS_THAN_OR_EQUAL and GREATER_THAN_OR_EQUAL */
	EQUAL(0x0800),
	
	/** Server will not count entries if set (ONLY used in client/server transaction) */ 
	NOCOUNT_ENTRIES(0x0010),
	
	/** Overlapping ranges match, and values within a range match. This symbol is
	 * valid for fields of type TYPE_TIME_RANGE and TYPE_NUMBER_RANGE. Therefore
	 * it should only be used with NIFFindByKey. */
	RANGE_OVERLAP(0x0100),
	
	/** Return any entry representing an actual document
	 * (a non-category entry), instead of searching for the first
	 * (or last) entry in the category. It is unpredictable exactly
	 * which entry you will get. A count of the matched document and
	 * any subsequent documents that match is returned. This count
	 * may be less than the actual number of documents that match. */
	RETURN_ANY_NON_CATEGORY_MATCH(0x0200),
	
	/** Only match non-category entries */
	NONCATEGORY_ONLY(0x0400),
	
	/** User COLLECTIONPOSITION-&gt;MaxLevel as BottomLevel. This can be setup prior to call */
	SET_BOTTOM_LEVEL(0x1000),        
    
	/** Do not return multiple matches for
	the same NOTEID in standard
	permuted views */
	UNIQUE_NOTEIDS_IN_FLAT_VIEW(0x4000),
	
	/** Return groups, ie looking in the $SERVER_ACCESS view */
	RETURN_SERVER_ACCESS(0x8000),

	//NIFFindByKeyExtended2 flags
	
	/** Read and buffer matches NIFFindByKeyExtended2 only */
	AND_READ_MATCHES(0x2000),
	
	/** Use the case and accent sensitivity flags specified in the view */
	VIEW_SENSITIVE(0x00010000),
    
	/** Find first category match */
	CATEGORY_MATCH(0x00020000),
	
	/** Limit number of matches returned */
	LIMIT_MATCHES(0x00040000),
	
	/** Search key is a category with leaf entry
    so match the leaf as well (eg. ibm\atlanta\joe blow */
	MATCH_CATEGORYANDLEAF(0x00080000),
	
	/** Search key is a category only and if no match then match on the leaf. */
	MATCH_CATEGORYORLEAF(0x00100000),
	
	/** Return only entries which hNames would disallow (requires full access set) */
	MATCH_PRIVATE_ONLY(0x00200000),
	
	/** Limit number of matches returned to the number in retNumMatches as an input parameter */
	SPECIFIC_MATCHES(0x00400000),
	
	/** If the collection needs it, call NIFUpdateColletion prior to find (NIFFindByKeyExtended2 only */
	REFRESH_FIRST(0x00800000);

	private int m_val;
	
	Find(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static short toBitMask(EnumSet<Find> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (Find currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}
	
	public static int toBitMaskInt(EnumSet<Find> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (Find currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}
	
	/** Bitmask of the comparison flags defined above */
	public static short FIND_COMPARE_MASK = 0x08C0;

}
