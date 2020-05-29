package com.mindoo.domino.jna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.mindoo.domino.jna.constants.OpenCollection;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.utils.StringTokenizerExt;

/**
 * View and folder information read from the database design
 * 
 * @author Karsten Lehmann
 */
public class NotesCollectionSummary {
	private String m_title;
	private List<String> m_aliases;
	private String m_flags;
	private NotesDatabase m_parentDb;
	private int m_noteId;
	private String m_comment;
	private String m_language;

	private NotesCollection m_collection;
	
	NotesCollectionSummary(NotesDatabase parentdb) {
		m_parentDb = parentdb;
	}

	void initFromDesignCollectionEntry(NotesViewEntryData entry) {
		String titleAndAliases = entry.getAsString("$title", "");
		StringTokenizerExt st = new StringTokenizerExt(titleAndAliases, "|");
		m_title = st.nextToken();
		
		m_aliases = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			m_aliases.add(st.nextToken());
		}
		
		m_flags = entry.getAsString(NotesConstants.DESIGN_FLAGS, "");
		m_noteId = entry.getNoteId();
		m_comment = entry.getAsString("$comment", "");
		m_language = entry.getAsString("$language", "");
	}

	public void setTitle(String title) {
		m_title = title;
	}
	
	public String getTitle() {
		return m_title==null ? "" : m_title;
	}

	public void setAliases(List<String> aliases) {
		m_aliases = aliases;
	}
	
	public List<String> getAliases() {
		return m_aliases==null ? Collections.emptyList() : m_aliases;
	}
	
	public void setFlags(String flags) {
		m_flags = flags;
	}
	
	public boolean isFolder() {
		if (m_flags!=null) {
			return m_flags.contains(NotesConstants.DESIGN_FLAG_FOLDER_VIEW);
		}
		return false;
	}

	public void setNoteId(int noteId) {
		m_noteId = noteId;
	}
	
	public int getNoteId() {
		return m_noteId;
	}
	
	public void setComment(String comment) {
		m_comment = comment;
	}
	
	public String getComment() {
		return m_comment==null ? "" : m_comment;
	}
	
	public void setLanguage(String language) {
		m_language = language;
	}
	
	public String getLanguage() {
		return m_language==null ? "" : m_language;
	}
	
	public NotesDatabase getParent() {
		return m_parentDb;
	}
	
	public NotesCollection openCollection() {
		if (m_collection==null || m_collection.isRecycled()) {
			m_collection = m_parentDb.openCollection(getNoteId(), (EnumSet<OpenCollection>) null);
		}
		return m_collection;
	}

	@Override
	public String toString() {
		return "NotesCollectionSummary [title="+getTitle()+", aliases="+getAliases()+", isfolder="+isFolder()+", noteid="+getNoteId()+"]";
	}
}
