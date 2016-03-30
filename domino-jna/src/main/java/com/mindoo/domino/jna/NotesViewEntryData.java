package com.mindoo.domino.jna;

import java.util.Map;

/**
 * Data object that contains all data read from collection entries
 * 
 * @author Karsten Lehmann
 */
public class NotesViewEntryData {
	private int[] m_pos;
	private String m_posStr;
	private int m_noteId;
	private String m_unid;
	private int m_noteClass;
	private int m_siblingCount;
	private int m_childCount;
	private int m_descendantCount;
	private boolean m_isAnyUnread;
	private short m_indentLevels;
	private short m_ftScore;
	private boolean m_isUnread;
	private Object[] m_columnValues;
	private Map<String, Object> m_summaryData;
	
	public NotesViewEntryData() {
		
	}
	
	public boolean isConflict() {
		//C API documentation regarding conflict flags in views
		//VIEW_TABLE_FLAG_CONFLICT	  -  Replication conflicts will be flagged. If TRUE, the '$Conflict' item must be SECOND-TO-LAST in the list of summary items for this view.
		return m_columnValues.length>=2 && m_columnValues[m_columnValues.length-2] != null;
	}

	public boolean isResponse() {
		//C API documentation regarding response flags in views
		//VIEW_TABLE_FLAG_FLATINDEX	  -  Do not index hierarchically If FALSE, the '$REF' item must be LAST in the list of summary items for this view.
		return m_columnValues.length>=1 && m_columnValues[m_columnValues.length-1] != null;
	}

	public int[] getPosition() {
		return m_pos;
	}

	public String getPositionStr() {
		if (m_posStr==null) {
			if (m_pos==null || m_pos.length==0) {
				m_posStr = "";
			}
			else {
				StringBuilder sb = new StringBuilder();
				for (int i=0; i<m_pos.length; i++) {
					if (i>0)
						sb.append(".");
					sb.append(m_pos[i]);
				}
				m_posStr = sb.toString();
			}
		}
		return m_posStr;
	}
	
	public void setPosition(int[] pos) {
		m_pos = pos;
	}

	public int getNoteId() {
		return m_noteId;
	}

	public void setNoteId(int noteId) {
		m_noteId = noteId;
	}

	public String getUNID() {
		return m_unid;
	}

	public void setUNID(String unid) {
		m_unid = unid;
	}

	public int getNoteClass() {
		return m_noteClass;
	}

	public void setNoteClass(int noteClass) {
		m_noteClass = noteClass;
	}

	public int getSiblingCount() {
		return m_siblingCount;
	}

	public void setSiblingCount(int siblingCount) {
		m_siblingCount = siblingCount;
	}

	public int getChildCount() {
		return m_childCount;
	}

	public void setChildCount(int childCount) {
		m_childCount = childCount;
	}

	public int getDescendantCount() {
		return m_descendantCount;
	}

	public void setDescendantCount(int descendantCount) {
		m_descendantCount = descendantCount;
	}

	public boolean isUnread() {
		return m_isUnread;
	}

	public void setUnread(boolean isUnread) {
		m_isUnread = isUnread;
	}

	public short getIndentLevels() {
		return m_indentLevels;
	}

	public void setIndentLevels(short indentLevels) {
		m_indentLevels = indentLevels;
	}

	public short getFTScore() {
		return m_ftScore;
	}

	public void setFTScore(short ftScore) {
		m_ftScore = ftScore;
	}

	public boolean isAnyUnread() {
		return m_isAnyUnread;
	}

	public void setAnyUnread(boolean isAnyUnread) {
		m_isAnyUnread = isAnyUnread;
	}

	public void setColumnValues(Object[] itemValues) {
		m_columnValues = itemValues;
	}

	public Object[] getColumnValues() {
		return m_columnValues;
	}
	
	public void setSummaryData(Map<String,Object> summaryData) {
		m_summaryData = summaryData;
	}
	
	public Map<String,Object> getSummaryData() {
		return m_summaryData;
	}
}
