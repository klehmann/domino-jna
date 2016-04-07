package com.mindoo.domino.jna;

import java.util.Calendar;
import java.util.Map;

import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.internal.NotesCAPI;

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
	private int[] m_columnValueSizes;
	private Map<String, Object> m_summaryData;
	
	public NotesViewEntryData() {
		
	}
	
	/**
	 * Method to check whether an entry is a conflict document. Can only returns a true value
	 * if {@link ReadMask#SUMMARYVALUES} or {@link ReadMask#SUMMARY} is used for the lookup.
	 * 
	 * @return true if conflict
	 */
	public boolean isConflict() {
		//C API documentation regarding conflict flags in views
		//VIEW_TABLE_FLAG_CONFLICT	  -  Replication conflicts will be flagged. If TRUE, the '$Conflict' item must be SECOND-TO-LAST in the list of summary items for this view.
		if (m_columnValues!=null) {
			return m_columnValues.length>=2 && m_columnValues[m_columnValues.length-2] != null;
		}
		else if (m_summaryData!=null) {
			return m_summaryData.get("$Conflict") != null;
		}
		return false;
	}

	/**
	 * Method to check whether an entry is a conflict document. Can only returns a true value
	 * if {@link ReadMask#SUMMARYVALUES} or {@link ReadMask#SUMMARY} is used for the lookup.
	 * 
	 * @return true if response
	 */
	public boolean isResponse() {
		//C API documentation regarding response flags in views
		//VIEW_TABLE_FLAG_FLATINDEX	  -  Do not index hierarchically If FALSE, the '$REF' item must be LAST in the list of summary items for this view.
		if (m_columnValues!=null) {
			return m_columnValues.length>=1 && m_columnValues[m_columnValues.length-1] != null;
		}
		else if (m_summaryData!=null) {
			return m_summaryData.get("$Ref") != null;
		}
		return false;
	}

	/**
	 * Returns the entry position in the view as an int array. Only returns a non-null value if
	 * {@link ReadMask#INDEXPOSITION} is used for the lookup.
	 * 
	 * @return position or null
	 */
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
	
	/**
	 * Returns the entry position in the view as a string (e.g. 1.2.3). Only returns a non-null value if
	 * {@link ReadMask#INDEXPOSITION} is used for the lookup.
	 * 
	 * @return position string or empty string
	 */
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
	
	/**
	 * Sets the position
	 * 
	 * @param pos new position
	 */
	public void setPosition(int[] pos) {
		m_pos = pos;
	}

	/**
	 * Returns the note id of the entry. Only returns a value if {@link ReadMask#NOTEID} is used for the lookup
	 * 
	 * @return note id or 0
	 */
	public int getNoteId() {
		return m_noteId!=null ? m_noteId.intValue() : 0;
	}

	/**
	 * Returns the note id of the entry in hex format. Only returns a value if {@link ReadMask#NOTEID} is used for the lookup
	 * 
	 * @return note id as hex string or null
	 */
	public String getNoteIdAsHex() {
		return m_noteId!=null ? Integer.toString(m_noteId.intValue(), 16) : null;
	}
	
	/**
	 * Sets the note id
	 * 
	 * @param noteId note id
	 */
	public void setNoteId(int noteId) {
		m_noteId = Integer.valueOf(noteId);
	}

	/**
	 * Method to check whether the entry is a document. Only returns a value if {@link ReadMask#NOTEID}
	 * is used for the lookup
	 * 
	 * @return true if document
	 */
	public boolean isDocument() {
		return !isCategory() && !isTotal();
	}

	/**
	 * Method to check whether the entry is a category. Only returns a value if {@link ReadMask#NOTEID}
	 * is used for the lookup
	 * 
	 * @return true if category
	 */
	public boolean isCategory()  {
		if (m_noteId!=null) {
			return (m_noteId.intValue() & NotesCAPI.NOTEID_CATEGORY) == NotesCAPI.NOTEID_CATEGORY;
		}
		return false;
	}

	/**
	 * Method to check whether the entry is a total value. Only returns a value if {@link ReadMask#NOTEID}
	 * is used for the lookup
	 * 
	 * @return true if total
	 */
	public boolean isTotal() {
		if (m_noteId!=null) {
			return (m_noteId.intValue() & NotesCAPI.NOTEID_CATEGORY_TOTAL) == NotesCAPI.NOTEID_CATEGORY_TOTAL;
		}
		return false;
	}

	/**
	 * Returns the UNID of the entry. Only returns a value if {@link ReadMask#NOTEUNID}
	 * is used for the lookup
	 * 
	 * @return UNID or null
	 */
	public String getUNID() {
		return m_unid;
	}

	/**
	 * Sets the UNID
	 * 
	 * @param unid UNID
	 */
	public void setUNID(String unid) {
		m_unid = unid;
	}

	/**
	 * Returns the entry's note class. Only returns a value if {@link ReadMask#NOTECLASS}
	 * is used for the lookup
	 * 
	 * @return class
	 */
	public int getNoteClass() {
		return m_noteClass!=null ? m_noteClass.intValue() : 0;
	}

	/**
	 * Sets the note class
	 * 
	 * @param noteClass note class
	 */
	public void setNoteClass(int noteClass) {
		m_noteClass = Integer.valueOf(noteClass);
	}

	/**
	 * Returns the sibling count. Only returns a value if {@link ReadMask#INDEXSIBLINGS}
	 * is used for the lookup
	 * 
	 * @return count or 0
	 */
	public int getSiblingCount() {
		return m_siblingCount!=null ? m_siblingCount.intValue() : 0;
	}

	/**
	 * Sets the sibling count
	 * 
	 * @param siblingCount count
	 */
	public void setSiblingCount(int siblingCount) {
		m_siblingCount = Integer.valueOf(siblingCount);
	}

	/**
	 * Returns the sibling count. Only returns a value if {@link ReadMask#INDEXCHILDREN}
	 * is used for the lookup
	 * 
	 * @return count or 0
	 */
	public int getChildCount() {
		return m_childCount!=null ? m_childCount.intValue() : 0;
	}

	/**
	 * Sets the child count
	 * 
	 * @param childCount count
	 */
	public void setChildCount(int childCount) {
		m_childCount = Integer.valueOf(childCount);
	}

	/**
	 * Returns the descendant count. Only returns a value if {@link ReadMask#INDEXDESCENDANTS}
	 * is used for the lookup
	 * 
	 * @return count or 0
	 */
	public int getDescendantCount() {
		return m_descendantCount!=null ? m_descendantCount.intValue() : 0;
	}

	/**
	 * Sets the descendant count
	 * 
	 * @param descendantCount count
	 */
	public void setDescendantCount(int descendantCount) {
		m_descendantCount = Integer.valueOf(descendantCount);
	}

	/**
	 * Returns true if the entry is unread. Only returns a value if {@link ReadMask#INDEXUNREAD}
	 * is used for the lookup
	 * 
	 * @return true if unread
	 */
	public boolean isUnread() {
		return m_isUnread!=null ? m_isUnread.booleanValue() : false;
	}

	/**
	 * Sets the unread flag
	 * 
	 * @param isUnread flag
	 */
	public void setUnread(boolean isUnread) {
		m_isUnread = Boolean.valueOf(isUnread);
	}

	/**
	 * Returns the indent levels in the view. Only returns a value if {@link ReadMask#INDENTLEVELS}
	 * is used for the lookup
	 * 
	 * @return levels or 0
	 */
	public int getIndentLevels() {
		return m_indentLevels!=null ? m_indentLevels.intValue() : 0;
	}

	/**
	 * Sets the indent levels
	 * 
	 * @param indentLevels levels
	 */
	public void setIndentLevels(int indentLevels) {
		m_indentLevels = Integer.valueOf(indentLevels);
	}

	/**
	 * Returns the fulltext search score. Only returns a value if {@link ReadMask#SCORE}
	 * is used for the lookup
	 * 
	 * @return score or 0
	 */
	public int getFTScore() {
		return m_ftScore!=null ? m_ftScore.intValue() : 0;
	}

	/**
	 * Sets the fulltext search score
	 * 
	 * @param ftScore score
	 */
	public void setFTScore(int ftScore) {
		m_ftScore = Integer.valueOf(ftScore);
	}

	/**
	 * Returns a flag whether the entry or any descendents are unread. Only returns a value if {@link ReadMask#INDEXANYUNREAD}
	 * is used for the lookup
	 * 
	 * @return true if any unread
	 */
	public boolean isAnyUnread() {
		return m_isAnyUnread!=null ? m_isAnyUnread.booleanValue() : false;
	}

	/**
	 * Sets the any unread flag
	 * 
	 * @param isAnyUnread flag
	 */
	public void setAnyUnread(boolean isAnyUnread) {
		m_isAnyUnread = Boolean.valueOf(isAnyUnread);
	}

	/**
	 * Sets the collection entry column values.
	 * 
	 * @param itemValues new values
	 */
	public void setColumnValues(Object[] itemValues) {
		m_columnValues = itemValues;
	}

	/**
	 * Returns the collection entry column values. Only returns a non-null value if {@link ReadMask#SUMMARYVALUES}
	 * is used for the lookup. The column values array contains two additional columns that Domino uses to
	 * report {@link #isConflict()} and {@link #isResponse()}
	 * 
	 * @return column values or null
	 */
	public Object[] getColumnValues() {
		return m_columnValues;
	}
	
	/**
	 * Sets the sizes in bytes of the collection entry column values
	 * 
	 * @param sizes sizes
	 */
	public void setColumnValueSizesInBytes(int[] sizes) {
		m_columnValueSizes = sizes;
	}
	
	/**
	 * Returns the sizes in bytes of collection entry column values. Can be used for performance optimization
	 * to analyze which columns of a view fill the summary buffer the most. Only returns a non-null value if
	 * {@link ReadMask#SUMMARYVALUES} is used for the lookup.
	 * 
	 * @return sizes or null
	 */
	public int[] getColumnValueSizesInBytes() {
		return m_columnValueSizes;
	}
	
	/**
	 * Sets the summary map data
	 * 
	 * @param summaryData new data
	 */
	public void setSummaryData(Map<String,Object> summaryData) {
		m_summaryData = summaryData;
	}
	
	/**
	 * Returns the summary data, which is a map with programmatic column names as key and the column value as map value.
	 * Only returns a non-null value if {@link ReadMask#SUMMARY} is used for the lookup.
	 * 
	 * @return summary data or null
	 */
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
	
	/**
	 * Converts a column value to a string, used for debugging values
	 * 
	 * @param val column value
	 * @return value as string
	 */
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
