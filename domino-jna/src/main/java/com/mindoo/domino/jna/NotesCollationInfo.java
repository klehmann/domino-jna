package com.mindoo.domino.jna;

import java.util.List;

import com.mindoo.domino.jna.internal.NotesCAPI;

/**
 * Container object that provides access to the available sortings for a {@link NotesCollection}
 * 
 * @author Karsten Lehmann
 */
public class NotesCollationInfo {
	public byte m_flags;
	private List<NotesCollateDescriptor> m_collateDescriptors;
	
	public NotesCollationInfo(byte flags, List<NotesCollateDescriptor> descriptors) {
		m_flags = flags;
		m_collateDescriptors = descriptors;
	}
	
	/**
	 * Indicates unique keys. Used for ODBC Access: Generate unique keys in index.
	 * 
	 * @return true for unique keys
	 */
	public boolean isUnique() {
		return (m_flags & NotesCAPI.COLLATION_FLAG_UNIQUE) == NotesCAPI.COLLATION_FLAG_UNIQUE;
	}

	/**
	 * Flag to indicate only build on demand.
	 * 
	 * @return true for build on demand
	 */
	public boolean isBuildOnDemand() {
		return (m_flags & NotesCAPI.COLLATION_FLAG_BUILD_ON_DEMAND) == NotesCAPI.COLLATION_FLAG_BUILD_ON_DEMAND;
	}

	/**
	 * Returns the collate descriptors with the sortings used for this collation
	 * 
	 * @return descriptors
	 */
	public List<NotesCollateDescriptor> getDescriptors() {
		return m_collateDescriptors;
	}
}
