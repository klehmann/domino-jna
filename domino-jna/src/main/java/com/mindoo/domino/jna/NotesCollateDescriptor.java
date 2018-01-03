package com.mindoo.domino.jna;

import com.mindoo.domino.jna.NotesCollection.Direction;
import com.mindoo.domino.jna.constants.CollateType;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * Data container for a single sort column of a {@link NotesCollationInfo}
 * 
 * @author Karsten Lehmann
 */
public class NotesCollateDescriptor {
	private NotesCollationInfo m_parentCollateInfo;
	private String m_name;
	private CollateType m_keyType;
	private byte m_flags;
	
	public NotesCollateDescriptor(NotesCollationInfo parentCollateInfo, String name, CollateType type, byte flags) {
		m_parentCollateInfo = parentCollateInfo;
		m_name = name;
		m_keyType = type;
		m_flags = flags;
	}
	
	/**
	 * Returns the collation that this descriptor is part of
	 * 
	 * @return collation
	 */
	public NotesCollationInfo getParent() {
		return m_parentCollateInfo;
	}
	
	/**
	 * Returns the sort direction
	 * 
	 * @return sorting
	 */
	public Direction getDirection() {
		return (m_flags & NotesConstants.CDF_M_descending) == NotesConstants.CDF_M_descending ? Direction.Descending : Direction.Ascending;
	}
	
	/**
	 * Returns the collate type
	 * 
	 * @return type
	 */
	public CollateType getType() {
		return m_keyType;
	}
	
	/**
	 * Returns the item name of the column to be sorted
	 * 
	 * @return item name
	 */
	public String getName() {
		return m_name;
	}
	
	/**
	 * If prefix list, then ignore for sorting
	 * 
	 * @return true to ignore
	 */
	public boolean isIgnorePrefixes() {
		return (m_flags & NotesConstants.CDF_M_ignoreprefixes) == NotesConstants.CDF_M_ignoreprefixes;
	}

	/**
	 * If set, text compares are case-sensitive
	 * 
	 * @return true for case-sensitive
	 */
	public boolean isCaseSensitiveSort() {
		return (m_flags & NotesConstants.CDF_M_casesensitive_in_v5) == NotesConstants.CDF_M_casesensitive_in_v5;
	}
	
	/**
	 * If set, text compares are accent-sensitive
	 * 
	 * @return true for accent-sensitive
	 */
	public boolean isAccentSensitiveSort() {
		return (m_flags & NotesConstants.CDF_M_accentsensitive_in_v5) == NotesConstants.CDF_M_accentsensitive_in_v5;
	}
	
	/**
	 * If set, lists are permuted
	 * 
	 * @return true if permuted
	 */
	public boolean isPermuted() {
		return (m_flags & NotesConstants.CDF_M_permuted) == NotesConstants.CDF_M_permuted;
	}
	
	/**
	 * Qualifier if lists are permuted; if set, lists are pairwise permuted, otherwise lists are multiply permuted
	 * 
	 * @return true if pairwise permuted
	 */
	public boolean isPermutedPairwise() {
		return (m_flags & NotesConstants.CDF_M_permuted_pairwise) == NotesConstants.CDF_M_permuted_pairwise;
	}
	
	/**
	 * If set, treat as permuted
	 * 
	 * @return true if permuted
	 */
	public boolean isFlat() {
		return (m_flags & NotesConstants.CDF_M_flat_in_v5) == NotesConstants.CDF_M_flat_in_v5;
	}
}
