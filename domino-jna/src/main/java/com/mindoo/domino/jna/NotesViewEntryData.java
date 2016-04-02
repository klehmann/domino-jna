package com.mindoo.domino.jna;

import java.util.Calendar;
import java.util.Map;

import com.mindoo.domino.jna.internal.NotesCAPI;

import lotus.domino.NotesException;

/**
 * Data object that contains all data read from collection entries
 * 
 * @author Karsten Lehmann
 */
public class NotesViewEntryData {
	private int[] m_pos;
	private String m_posStr;
	private Integer m_noteId;
	private String m_unid;
	private Integer m_noteClass;
	private Integer m_siblingCount;
	private Integer m_childCount;
	private Integer m_descendantCount;
	private Boolean m_isAnyUnread;
	private Integer m_indentLevels;
	private Integer m_ftScore;
	private Boolean m_isUnread;
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

	/**
	 * Returns the level of the entry in the view (position 1 = level 0, position 1.1 = level 1)
	 * 
	 * @return level, only available when position is loaded, otherwise the method returns -1
	 */
	public int getLevel() {
		return m_pos!=null ? (m_pos.length-1) : -1;
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
		return m_noteId!=null ? m_noteId.intValue() : 0;
	}

	public void setNoteId(int noteId) {
		m_noteId = Integer.valueOf(noteId);
	}

	public boolean isDocument() {
		return !isCategory() && !isTotal();
	}

	public boolean isCategory()  {
		if (m_noteId!=null) {
			return (m_noteId.intValue() & NotesCAPI.NOTEID_CATEGORY) == NotesCAPI.NOTEID_CATEGORY;
		}
		return false;
	}

	public boolean isTotal() {
		if (m_noteId!=null) {
			return (m_noteId.intValue() & NotesCAPI.NOTEID_CATEGORY_TOTAL) == NotesCAPI.NOTEID_CATEGORY_TOTAL;
		}
		return false;
	}

	public String getUNID() {
		return m_unid;
	}

	public void setUNID(String unid) {
		m_unid = unid;
	}

	public int getNoteClass() {
		return m_noteClass!=null ? m_noteClass.intValue() : 0;
	}

	public void setNoteClass(int noteClass) {
		m_noteClass = Integer.valueOf(noteClass);
	}

	public int getSiblingCount() {
		return m_siblingCount!=null ? m_siblingCount.intValue() : null;
	}

	public void setSiblingCount(int siblingCount) {
		m_siblingCount = Integer.valueOf(siblingCount);
	}

	public int getChildCount() {
		return m_childCount!=null ? m_childCount.intValue() : 0;
	}

	public void setChildCount(int childCount) {
		m_childCount = Integer.valueOf(childCount);
	}

	public int getDescendantCount() {
		return m_descendantCount!=null ? m_descendantCount.intValue() : 0;
	}

	public void setDescendantCount(int descendantCount) {
		m_descendantCount = Integer.valueOf(descendantCount);
	}

	public boolean isUnread() {
		return m_isUnread!=null ? m_isUnread.booleanValue() : false;
	}

	public void setUnread(boolean isUnread) {
		m_isUnread = Boolean.valueOf(isUnread);
	}

	public int getIndentLevels() {
		return m_indentLevels!=null ? m_indentLevels.intValue() : 0;
	}

	public void setIndentLevels(int indentLevels) {
		m_indentLevels = Integer.valueOf(indentLevels);
	}

	public int getFTScore() {
		return m_ftScore!=null ? m_ftScore.intValue() : 0;
	}

	public void setFTScore(int ftScore) {
		m_ftScore = Integer.valueOf(ftScore);
	}

	public boolean isAnyUnread() {
		return m_isAnyUnread!=null ? m_isAnyUnread.booleanValue() : false;
	}

	public void setAnyUnread(boolean isAnyUnread) {
		m_isAnyUnread = Boolean.valueOf(isAnyUnread);
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
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (m_noteId!=null) {
			sb.append(",id="+m_noteId);
		}
		
		if (m_unid!=null) {
			sb.append("unid="+m_unid);
		}
		
		String posStr = getPositionStr();
		if (posStr!=null && posStr.length()>0) {
			sb.append(",pos="+posStr);
		}
		
		if (m_noteClass!=null) {
			sb.append(",class="+m_noteClass.intValue());
		}
		
		if (isDocument()) {
			sb.append(",type=document");
		}
		else if (isCategory()) {
			sb.append(",type=category");
		}
		else if (isTotal()) {
			sb.append(",type=total");
		}
		
		if (m_indentLevels!=null) {
			sb.append(",indentlevel="+m_indentLevels.intValue());
		}
		
		if (m_childCount!=null) {
			sb.append(",childcount="+m_childCount.intValue());
		}
		
		if (m_descendantCount!=null) {
			sb.append(",descendantcount="+m_descendantCount.intValue());
		}
		
		if (m_siblingCount!=null) {
			sb.append(",siblingcount="+m_siblingCount.intValue());
		}
		
		if (m_summaryData!=null) {
			sb.append(",summary="+m_summaryData.toString());
		}
		
		if (m_columnValues!=null) {
			sb.append(",columns=[");
			for (int i=0; i<m_columnValues.length; i++) {
				if (i>0)
					sb.append(",");
				sb.append(colValueToString(m_columnValues[i]));
			}
			sb.append("]");
		}
		
		if (m_ftScore!=null) {
			sb.append(",score="+m_ftScore.intValue());
		}
		
		if (m_isUnread!=null) {
			sb.append(",unread="+m_isUnread.booleanValue());
		}
		
		if (m_isAnyUnread!=null) {
			sb.append(",anyunread="+m_isAnyUnread.booleanValue());
		}
		
		if (sb.length()>0) {
			//remove first ","
			sb.delete(0, 1);
		}

		sb.insert(0, "ViewEntry[");
		sb.append("]");
		
		return sb.toString();
	}
	
	private String colValueToString(Object val) {
		if (val==null)
			return "null";
		
		if (val instanceof Calendar) {
			return ((Calendar)val).getTime().toString();
		}
		else {
			return val.toString();
		}
	}
}
