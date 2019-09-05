package com.mindoo.domino.jna;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.mindoo.domino.jna.constants.OpenCollection;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.utils.StringTokenizerExt;

/**
 * View and folder information read from the database design collection
 * 
 * @author Karsten Lehmann
 */
public class NotesCollectionSummary {
	private NotesViewEntryData m_entry;

	private String m_title;
	private List<String> m_aliases;
	private NotesCollection m_collection;
	
	NotesCollectionSummary(NotesViewEntryData entry) {
		m_entry = entry;
	}

	public String getTitle() {
		if (m_title==null) {
			String titleAndAliases = m_entry.getAsString("$title", "");
			int iPos = titleAndAliases.indexOf("|");
			if (iPos==-1) {
				m_title = titleAndAliases;
			}
			else {
				m_title = titleAndAliases.substring(0, iPos);
			}
		}
		return m_title;
	}

	public List<String> getAliases() {
		if (m_aliases==null) {
			String titleAndAliases = m_entry.getAsString("$title", "");
			StringTokenizerExt st = new StringTokenizerExt(titleAndAliases, "|");
			st.nextToken();
			
			m_aliases = new ArrayList<String>();
			while (st.hasMoreTokens()) {
				m_aliases.add(st.nextToken());
			}
		}
		return m_aliases;
	}
	
	public boolean isFolder() {
		String flags = m_entry.getAsString(NotesConstants.DESIGN_FLAGS, "");
		return flags.contains(NotesConstants.DESIGN_FLAG_FOLDER_VIEW);
	}

	public int getNoteId() {
		return m_entry.getNoteId();
	}
	
	public String getComment() {
		return m_entry.getAsString("$comment", "");
	}
	
	public String getLanguage() {
		return m_entry.getAsString("$language", "");
	}
	
	/**
	 * Returns the raw design collection entry, in case the provided getter methods do
	 * not provide enough data.
	 * 
	 * @return entry
	 */
	public NotesViewEntryData getDesignCollectionEntry() {
		return m_entry;
	}
	
	public NotesDatabase getParent() {
		return m_entry.getParent().getParent();
	}
	
	public NotesCollection openCollection() {
		if (m_collection==null || m_collection.isRecycled()) {
			m_collection = m_entry.getParent().getParent().openCollection(m_entry.getNoteId(), (EnumSet<OpenCollection>) null);
		}
		return m_collection;
	}

	@Override
	public String toString() {
		return "NotesCollectionSummary [title="+getTitle()+", aliases="+getAliases()+", isfolder="+isFolder()+", noteid="+getNoteId()+"]";
	}
}
