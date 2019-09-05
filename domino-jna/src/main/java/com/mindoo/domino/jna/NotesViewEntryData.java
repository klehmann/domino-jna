package com.mindoo.domino.jna;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.utils.EmptyIterator;
import com.mindoo.domino.jna.utils.LMBCSString;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;

/**
 * Data object that contains all data read from collection entries
 * 
 * @author Karsten Lehmann
 */
public class NotesViewEntryData {
	private NotesCollection m_parentCollection;
	
	private int[] m_pos;
	private String m_posStr;
	private Integer m_noteId;
	private String m_unid;
	private long[] m_unidAsLongs;
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
	private SoftReference<Map<String, Object>> m_convertedDataRef;
	private String m_singleColumnLookupName;
	private boolean m_preferNotesTimeDates;
	
	/**
	 * Creates a new instance
	 * 
	 * @param parentCollection parent notes collection
	 */
	public NotesViewEntryData(NotesCollection parentCollection) {
		m_parentCollection = parentCollection;
	}
	
	class CacheableViewEntryData implements Serializable {
		private static final long serialVersionUID = -6919729244434994355L;
		
		private int[] m_pos;
		private String m_posStr;
		private Integer m_noteId;
		private String m_unid;
		private long[] m_unidAsLongs;
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
		private SoftReference<Map<String, Object>> m_convertedDataRef;
		private String m_singleColumnLookupName;
	}
	
	/**
	 * Method to read the cacheable and serializable data from this object
	 * 
	 * @return data
	 */
	CacheableViewEntryData getCacheableData() {
		CacheableViewEntryData data = new CacheableViewEntryData();
		data.m_pos = m_pos;
		data.m_posStr = m_posStr;
		data.m_noteId = m_noteId;
		data.m_unid = m_unid;
		data.m_unidAsLongs = m_unidAsLongs;
		data.m_noteClass = m_noteClass;
		data.m_siblingCount = m_siblingCount;
		data.m_childCount = m_childCount;
		data.m_descendantCount = m_descendantCount;
		data.m_isAnyUnread = m_isAnyUnread;
		data.m_indentLevels = m_indentLevels;
		data.m_ftScore = m_ftScore;
		data.m_isUnread = m_isUnread;
		data.m_columnValues = m_columnValues;
		data.m_columnValueSizes = m_columnValueSizes;
		data.m_summaryData = m_summaryData;
		data.m_convertedDataRef = m_convertedDataRef;
		data.m_singleColumnLookupName = m_singleColumnLookupName;
		return data;
	}
	
	/**
	 * Method to update the internal state from a cache entry
	 * 
	 * @param data cache entry data
	 */
	void updateFromCache(CacheableViewEntryData data) {
		if (m_noteId.intValue()!=data.m_noteId.intValue())
			throw new IllegalArgumentException("Note ids do not match: "+m_noteId+" != "+data.m_noteId);
		
		m_pos = data.m_pos;
		m_posStr = data.m_posStr;
		m_noteId = data.m_noteId;
		m_unid = data.m_unid;
		m_unidAsLongs = data.m_unidAsLongs;
		m_noteClass = data.m_noteClass;
		m_siblingCount = data.m_siblingCount;
		m_childCount = data.m_childCount;
		m_descendantCount = data.m_descendantCount;
		m_isAnyUnread = data.m_isAnyUnread;
		m_indentLevels = data.m_indentLevels;
		m_ftScore = data.m_ftScore;
		m_isUnread = data.m_isUnread;
		m_columnValues = data.m_columnValues;
		m_columnValueSizes = data.m_columnValueSizes;
		m_summaryData = data.m_summaryData;
		m_convertedDataRef = data.m_convertedDataRef;
		m_singleColumnLookupName = data.m_singleColumnLookupName;
	}
	
	/**
	 * Returns the parent collection
	 * 
	 * @return parent collection
	 */
	public NotesCollection getParent() {
		return m_parentCollection;
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
			if (!m_parentCollection.isConflict()) {
				return false;
			}
			else if (m_parentCollection.isHierarchical()) {
				return m_columnValues.length>=2 && m_columnValues[m_columnValues.length-2] != null;
			}
			else {
				//special case for views which have "show response hierarchy" = false:
				//here the response column value is missing
				return m_columnValues.length>=1 && m_columnValues[m_columnValues.length-1] != null;
			}
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
			if (m_parentCollection.isHierarchical()) {
				return m_columnValues.length>=1 && m_columnValues[m_columnValues.length-1] != null;
			}
			else {
				//fallback to isConflict as this is the only info we have
				return isConflict();
			}
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
			return (m_noteId.intValue() & NotesConstants.NOTEID_CATEGORY) == NotesConstants.NOTEID_CATEGORY;
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
			return (m_noteId.intValue() & NotesConstants.NOTEID_CATEGORY_TOTAL) == NotesConstants.NOTEID_CATEGORY_TOTAL;
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
		if (m_unid==null) {
			if (m_unidAsLongs!=null) {
				m_unid = NotesStringUtils.toUNID(m_unidAsLongs[0], m_unidAsLongs[1]);
			}
		}
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
	 * Sets the UNID as a long array
	 * 
	 * @param unidAsLongs long array with file / note timedates
	 */
	public void setUNID(long[] unidAsLongs) {
		m_unidAsLongs = unidAsLongs;
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
	
	public Object[] getColumnValues() {
		return m_columnValues;
	}
	
	/**
	 * Returns an iterator of all available columns for which we can read column values
	 * (e.g. does not return static column names).<br>
	 * Convenience function that simply calls {@link NotesCollection#getColumnNames()} on the parent collection
	 * 
	 * @return programmatic column names converted to lowercase
	 */
	public Iterator<String> getColumnNames() {
		if ((m_parentCollection.getNoteId() & NotesConstants.NOTE_ID_SPECIAL) == NotesConstants.NOTE_ID_SPECIAL) {
			//special collection (e.g. design collection) where we cannot read the column names from the design element
			if (m_summaryData!=null) {
				//if we have used ReadMask.SUMMARY to read the data, we can take the summary map keys
				return m_summaryData.keySet().iterator();
			}
			else {
				return new EmptyIterator<String>();
			}
		}
		else {
			return m_parentCollection.getColumnNames();
		}
	}

	/**
	 * Returns the number of columns for which we can read column data (e.g. does not count columns
	 * with static values)<br>
	 * Convenience function that simply calls {@link NotesCollection#getNumberOfColumns()} on the parent collection
	 * 
	 * @return number of columns
	 */
	public int getNumberOfColumnsWithValues() {
		return m_parentCollection.getNumberOfColumns();
	}
	
	/**
	 * Converts the column values to a map. If you are only interested in specific columns,
	 * you get way better performance calling {@link #get(String)} for those columns
	 * directly, because we lazily convert text/text list column data from LMBCS format to Java String format.<br>
	 * Calling this method converts all string columns at once.
	 * 
	 * @return map with programmatic column names as keys
	 */
	public Map<String,Object> getColumnDataAsMap() {
		Map<String,Object> data = m_convertedDataRef==null ? null : m_convertedDataRef.get();
		if (data==null) {
			data = new HashMap<String, Object>();
			
			Iterator<String> colNames = getColumnNames();
			
			while (colNames.hasNext()) {
				String currColName = colNames.next();
				Object currColValue = get(currColName);
				
				data.put(currColName, currColValue);
			}
			m_convertedDataRef = new SoftReference<Map<String,Object>>(data);
		}
		return data;
	}
	
	/**
	 * Returns a list of reader that are allowed to see this view entry.
	 * This data is only retrieved when {@link ReadMask#SUMMARY} and
	 * {@link ReadMask#RETURN_READERSLIST} are both used to read the
	 * collection data.
	 * 
	 * @return readers or null if not read
	 */
	public List<String> getReadersList() {
		Object readersList = get("$C1$");
		if (readersList instanceof List) {
			return (List<String>) readersList;
		}
		else if (readersList instanceof String) {
			return Arrays.asList((String) readersList);
		}
		else {
			return null;
		}
	}
	
	/**
	 * Method to check whether the the view entry contains a non-null value for a programmatic column
	 * name
	 * 
	 * @param columnName column name
	 * @return true if exists
	 */
	public boolean has(String columnName) {
		if (m_summaryData!=null) {
			return m_summaryData.containsKey(columnName);
		}
		else if (m_columnValues!=null) {
			int colIdx = m_parentCollection.getColumnValuesIndex(columnName);
			if (colIdx==-1) {
				return false;
			}
			else {
				return m_columnValues[colIdx] != null;
			}
		}
		else {
			return false;
		}
	}

	/**
	 * Sets whether methods like {@link #get(String)} should return {@link NotesTimeDate}
	 * instead of {@link Calendar}.
	 * 
	 * @param b true to prefer NotesTimeDate (false by default)
	 */
	public void setPreferNotesTimeDates(boolean b) {
		m_preferNotesTimeDates = b;
	}
	
	/**
	 * Returns whether methods like {@link #get(String)} should return {@link NotesTimeDate}
	 * instead of {@link Calendar}.
	 * 
	 * @return true to prefer NotesTimeDate
	 */
	public boolean isPreferNotesTimeDates() {
		return m_preferNotesTimeDates;
	}
	
	/**
	 * Returns a column value. Only returns data of either {@link ReadMask#SUMMARY} or {@link ReadMask#SUMMARYVALUES}
	 * was used to read the collection data
	 * <br>
	 * The following data types are returned for the different column data types:<br>
	 * <ul>
	 * <li>{@link NotesItem#TYPE_TEXT} - {@link String}</li>
	 * <li>{@link NotesItem#TYPE_TEXT_LIST} - {@link List} of {@link String}</li>
	 * <li>{@link NotesItem#TYPE_NUMBER} - {@link Double}</li>
	 * <li>{@link NotesItem#TYPE_NUMBER_RANGE} - {@link List} with {@link Double} values for number lists or double[] values for number ranges (not sure if Notes views really supports them)</li>
	 * <li>{@link NotesItem#TYPE_TIME} - {@link Calendar} or {@link NotesTimeDate} if {@link #setPreferNotesTimeDates(boolean)} has been called</li>
	 * <li>{@link NotesItem#TYPE_TIME_RANGE} - {@link List} with {@link Calendar} values for datetime lists or Calendar[] values for datetime ranges {@link NotesTimeDate} if {@link #setPreferNotesTimeDates(boolean)} has been called</li>
	 * </ul>
	 * 
	 * @param columnNameOrTitle programatic column name or column title
	 * @return column value or null
	 */
	public Object get(String columnNameOrTitle) {
		if (isPreferNotesTimeDates()) {
			return get(columnNameOrTitle, false);
		}
		else {
			return get(columnNameOrTitle, true);
		}
	}
	
	/**
	 * Returns a column value. Only returns data of either {@link ReadMask#SUMMARY} or {@link ReadMask#SUMMARYVALUES}
	 * was used to read the collection data
	 * <br>
	 * The following data types are returned for the different column data types:<br>
	 * <ul>
	 * <li>{@link NotesItem#TYPE_TEXT} - {@link String}</li>
	 * <li>{@link NotesItem#TYPE_TEXT_LIST} - {@link List} of {@link String}</li>
	 * <li>{@link NotesItem#TYPE_NUMBER} - {@link Double}</li>
	 * <li>{@link NotesItem#TYPE_NUMBER_RANGE} - {@link List} with {@link Double} values for number lists or double[] values for number ranges (not sure if Notes views really supports them)</li>
	 * <li>{@link NotesItem#TYPE_TIME} - {@link Calendar} or {@link NotesTimeDate} if {@link #setPreferNotesTimeDates(boolean)} has been called</li>
	 * <li>{@link NotesItem#TYPE_TIME_RANGE} - {@link List} with {@link Calendar} values for datetime lists or Calendar[] values for datetime ranges {@link NotesTimeDate} if {@link #setPreferNotesTimeDates(boolean)} has been called</li>
	 * </ul>
	 * 
	 * @param columnNameOrTitle programatic column name or column title
	 * @param convertNotesTimeDateToCalendar true to convert {@link NotesTimeDate} values to {@link Calendar}
	 * @return column value or null
	 */
	private Object get(String columnNameOrTitle, boolean convertNotesTimeDateToCalendar) {
		Object val = null;
		
		String columnNameOrTitleLC = columnNameOrTitle.toLowerCase();
		
		if (m_summaryData!=null) {
			if (m_summaryData.containsKey(columnNameOrTitleLC)) {
				val = m_summaryData.get(columnNameOrTitleLC);
			}
			else {
				//try to find the programmatic column name if columnNameOrTitle contains the column title
				int colIdx = m_parentCollection.getColumnValuesIndex(columnNameOrTitleLC);
				if (colIdx!=-1 && colIdx!=65535) {
					String progColName = m_parentCollection.getColumnName(colIdx);
					if (progColName!=null) {
						val = m_summaryData.get(progColName);
					}
				}
				
			}
		}
		else if (m_columnValues!=null) {
			int colIdx = m_parentCollection.getColumnValuesIndex(columnNameOrTitle);
			if (colIdx!=-1 && colIdx!=65535) {
				if (colIdx < m_columnValues.length) {
					val = m_columnValues[colIdx];
				}
				else {
					val = null;
				}
			}
		}
		
		if (val instanceof List) {
			List<Object> valAsList = (List<Object>) val;
			for (int i=0; i<valAsList.size(); i++) {
				Object currListValue = valAsList.get(i);
				
				if (currListValue instanceof LMBCSString) {
					valAsList.set(i, ((LMBCSString)currListValue).getValue());
				}
				else if (currListValue instanceof NotesTimeDate) {
					if (convertNotesTimeDateToCalendar) {
						valAsList.set(i, ((NotesTimeDate)currListValue).toCalendar());
					}
				}
				else if (currListValue instanceof NotesDateRange) {
					if (convertNotesTimeDateToCalendar) {
						NotesTimeDate startDateTime = ((NotesDateRange)currListValue).getStartDateTime();
						NotesTimeDate endDateTime = ((NotesDateRange)currListValue).getEndDateTime();
						
						valAsList.set(i, new Calendar[] {startDateTime.toCalendar(), endDateTime.toCalendar()});
					}
				}
			}
		}
		else if (val instanceof LMBCSString) {
			val = ((LMBCSString)val).getValue();
		}
		else if (val instanceof NotesTimeDate) {
			if (convertNotesTimeDateToCalendar) {
				val = ((NotesTimeDate)val).toCalendar();
			}
		}
		else if (val instanceof NotesDateRange) {
			if (convertNotesTimeDateToCalendar) {
				NotesTimeDate startDateTime = ((NotesDateRange)val).getStartDateTime();
				NotesTimeDate endDateTime = ((NotesDateRange)val).getEndDateTime();
				
				val = new Calendar[] {startDateTime.toCalendar(), endDateTime.toCalendar()};
			}
		}
		return val;
	}
	
	/**
	 * Convenience method to check whether there are any column values stored in this entry
	 * 
	 * @return true if we have column values
	 */
	public boolean hasAnyColumnValues() {
		if (m_summaryData!=null) {
			if (!m_summaryData.isEmpty()) {
				return true;
			}
		}
		if (m_columnValues!=null) {
			for (int i=0; i<m_columnValues.length; i++) {
				if (m_columnValues[i]!=null) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Convenience function that converts a column value to a string
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a string
	 * @return string value or null
	 */
	public String getAsString(String columnName, String defaultValue) {
		Object val = get(columnName);
		if (val instanceof String) {
			return (String) val;
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			if (valAsList.isEmpty()) {
				return defaultValue;
			}
			else {
				return valAsList.get(0).toString();
			}
		}
		return defaultValue;
	}

	/**
	 * Convenience function that converts a column value to an abbreviated name
	 * 
	 * @param columnName programatic column name or column title
	 * @return name or null
	 */
	public String getAsNameAbbreviated(String columnName) {
		String nameStr = getAsString(columnName, null);
		return nameStr==null ? null : NotesNamingUtils.toAbbreviatedName(nameStr);
	}

	/**
	 * Convenience function that converts a column value to an abbreviated name
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a string
	 * @return name or null
	 */
	public String getAsNameAbbreviated(String columnName, String defaultValue) {
		String nameStr = getAsString(columnName, defaultValue);
		return nameStr==null ? null : NotesNamingUtils.toAbbreviatedName(nameStr);
	}
	 
	/**
	 * Convenience function that converts a column value to a list of abbreviated names
	 * 
	 * @param columnName programatic column name or column title
	 * @return names or null
	 */
	public List<String> getAsNamesListAbbreviated(String columnName) {
		List<String> strList = getAsStringList(columnName, null);
		if (strList!=null) {
			List<String> namesAbbr = new ArrayList<String>(strList.size());
			for (int i=0; i<strList.size(); i++) {
				namesAbbr.add(NotesNamingUtils.toAbbreviatedName(strList.get(i)));
			}
			return namesAbbr;
		}
		else
			return null;
	}
	
	/**
	 * Convenience function that converts a column value to a list of abbreviated names
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a string
	 * @return names or null
	 */
	public List<String> getAsNamesListAbbreviated(String columnName, List<String> defaultValue) {
		List<String> strList = getAsStringList(columnName, defaultValue);
		if (strList!=null) {
			List<String> namesAbbr = new ArrayList<String>(strList.size());
			for (int i=0; i<strList.size(); i++) {
				namesAbbr.add(NotesNamingUtils.toAbbreviatedName(strList.get(i)));
			}
			return namesAbbr;
		}
		else
			return null;
	}
	
	/**
	 * Convenience function that converts a column value to a string list
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a number
	 * @return string list value or null
	 */
	public List<String> getAsStringList(String columnName, List<String> defaultValue) {
		Object val = get(columnName);
		if (val instanceof String) {
			return Arrays.asList((String) val);
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			boolean correctType=true;
			for (int i=0; i<valAsList.size(); i++) {
				if (!(valAsList.get(i) instanceof String)) {
					correctType=false;
					break;
				}
			}
			
			if (correctType) {
				return (List<String>) valAsList;
			}
			else {
				List<String> strList = new ArrayList<String>();
				for (int i=0; i<valAsList.size(); i++) {
					strList.add(valAsList.get(i).toString());
				}
				return strList;
			}
		}
		else if (val!=null) {
			return Arrays.asList(val.toString());
		}
		return defaultValue;
	}
	
	/**
	 * Convenience function that converts a column value to a {@link Calendar}
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a Calendar
	 * @return calendar value or null
	 */
	public Calendar getAsCalendar(String columnName, Calendar defaultValue) {
		Object val = get(columnName);
		if (val instanceof NotesTimeDate) {
			return ((NotesTimeDate)val).toCalendar();
		}
		else if (val instanceof Calendar) {
			return (Calendar) val;
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			if (!valAsList.isEmpty()) {
				Object firstVal = valAsList.get(0);
				if (firstVal instanceof NotesTimeDate) {
					return ((NotesTimeDate) firstVal).toCalendar();
				}
				else if (firstVal instanceof Calendar) {
					return (Calendar) firstVal;
				}
			}
		}
		return defaultValue;
	}

	/**
	 * Convenience function that converts a column value to a {@link NotesTimeDate}
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a NotesTimeDate
	 * @return calendar value or null
	 */
	public NotesTimeDate getAsTimeDate(String columnName, NotesTimeDate defaultValue) {
		Object val = get(columnName, false);
		if (val instanceof NotesTimeDate) {
			return (NotesTimeDate) val;
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			if (!valAsList.isEmpty()) {
				Object firstVal = valAsList.get(0);
				if (firstVal instanceof NotesTimeDate) {
					return (NotesTimeDate) firstVal;
				}
			}
		}
		return defaultValue;
	}
	
	/**
	 * Convenience function that converts a column value to a {@link Calendar} list
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a number
	 * @return calendar list value or null
	 */
	public List<Calendar> getAsCalendarList(String columnName, List<Calendar> defaultValue) {
		Object val = get(columnName);
		if (val instanceof NotesTimeDate) {
			return Arrays.asList(((NotesTimeDate)val).toCalendar());
		}
		else if (val instanceof Calendar) {
			return Arrays.asList((Calendar) val);
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			List<Calendar> valAsCalendarList = new ArrayList<Calendar>();
			
			for (int i=0; i<valAsList.size(); i++) {
				Object currListVal = valAsList.get(i);
				if (currListVal instanceof NotesTimeDate) {
					valAsCalendarList.add(((NotesTimeDate)currListVal).toCalendar());
				}
				else if (currListVal instanceof Calendar) {
					valAsCalendarList.add((Calendar) currListVal);
				}
				else {
					//incorrect content type
					return defaultValue;
				}
			}
			return valAsCalendarList;
		}
		return defaultValue;
	}
	
	/**
	 * Convenience function that converts a column value to a {@link Calendar} list
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a number
	 * @return calendar list value or null
	 */
	public List<NotesTimeDate> getAsTimeDateList(String columnName, List<NotesTimeDate> defaultValue) {
		Object val = get(columnName, false);
		if (val instanceof NotesTimeDate) {
			return Arrays.asList(((NotesTimeDate)val));
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			boolean correctType = true;
			
			for (Object currVal : valAsList) {
				if (!(currVal instanceof NotesTimeDate)) {
					correctType = false;
					break;
				}
			}
			
			if (correctType) {
				return (List<NotesTimeDate>) valAsList;
			}
			else {
				return defaultValue;
			}
		}
		return defaultValue;
	}
	
	/**
	 * Convenience function that converts a column value to a double
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a number
	 * @return double
	 */
	public Double getAsDouble(String columnName, Double defaultValue) {
		Object val = get(columnName);
		if (val instanceof Number) {
			return ((Number) val).doubleValue();
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			if (!valAsList.isEmpty()) {
				Object firstVal = valAsList.get(0);
				if (firstVal instanceof Number) {
					return ((Number) firstVal).doubleValue();
				}
			}
		}
		return defaultValue;
	}

	/**
	 * Convenience function that converts a column value to an integer
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a number
	 * @return integer
	 */
	public Integer getAsInteger(String columnName, Integer defaultValue) {
		Object val = get(columnName);
		if (val instanceof Number) {
			return ((Number) val).intValue();
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			if (!valAsList.isEmpty()) {
				Object firstVal = valAsList.get(0);
				if (firstVal instanceof Number) {
					return ((Number) firstVal).intValue();
				}
			}
		}
		return defaultValue;
	}

	/**
	 * Convenience function that converts a column value to a long
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a number
	 * @return long
	 */
	public Long getAsLong(String columnName, Long defaultValue) {
		Object val = get(columnName);
		if (val instanceof Number) {
			return ((Number) val).longValue();
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			if (!valAsList.isEmpty()) {
				Object firstVal = valAsList.get(0);
				if (firstVal instanceof Number) {
					return ((Number) firstVal).longValue();
				}
			}
		}
		return defaultValue;
	}
	
	/**
	 * Convenience function that converts a column value to a double list
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a number
	 * @return double list
	 */
	public List<Double> getAsDoubleList(String columnName, List<Double> defaultValue) {
		Object val = get(columnName);
		if (val instanceof Number) {
			return Arrays.asList(((Number) val).doubleValue());
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			boolean correctType=true;
			boolean numberList=true;
			
			for (int i=0; i<valAsList.size(); i++) {
				Object currObj = valAsList.get(i);
				
				if (currObj instanceof Double) {
					//ok
				}
				else if (currObj instanceof Number) {
					correctType=false;
					numberList=true;
				}
				else {
					correctType=false;
					numberList=false;
				}
			}
			
			if (correctType) {
				return (List<Double>) valAsList;
			}
			else if (numberList) {
				List<Double> dblList = new ArrayList<Double>(valAsList.size());
				for (int i=0; i<valAsList.size(); i++) {
					dblList.add(((Number)valAsList.get(i)).doubleValue());
				}
				return dblList;
			}
			else {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Convenience function that converts a column value to a integer list
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a number
	 * @return integer list
	 */
	public List<Integer> getAsIntegerList(String columnName, List<Integer> defaultValue) {
		Object val = get(columnName);
		if (val instanceof Number) {
			return Arrays.asList(((Number) val).intValue());
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			boolean correctType=true;
			boolean numberList=true;
			
			for (int i=0; i<valAsList.size(); i++) {
				Object currObj = valAsList.get(i);
				
				if (currObj instanceof Integer) {
					//ok
				}
				else if (currObj instanceof Number) {
					correctType=false;
					numberList=true;
				}
				else {
					correctType=false;
					numberList=false;
				}
			}
			
			if (correctType) {
				return (List<Integer>) valAsList;
			}
			else if (numberList) {
				List<Integer> intList = new ArrayList<Integer>(valAsList.size());
				for (int i=0; i<valAsList.size(); i++) {
					intList.add(((Number)valAsList.get(i)).intValue());
				}
				return intList;
			}
			else {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Convenience function that converts a column value to a list of longs
	 * 
	 * @param columnName programatic column name or column title
	 * @param defaultValue default value if column is empty or is not a number
	 * @return list of longs
	 */
	public List<Long> getAsLongList(String columnName, List<Long> defaultValue) {
		Object val = get(columnName);
		if (val instanceof Number) {
			return Arrays.asList(((Number) val).longValue());
		}
		else if (val instanceof List) {
			List<?> valAsList = (List<?>) val;
			boolean correctType=true;
			boolean numberList=true;
			
			for (int i=0; i<valAsList.size(); i++) {
				Object currObj = valAsList.get(i);
				
				if (currObj instanceof Long) {
					//ok
				}
				else if (currObj instanceof Number) {
					correctType=false;
					numberList=true;
				}
				else {
					correctType=false;
					numberList=false;
				}
			}
			
			if (correctType) {
				return (List<Long>) valAsList;
			}
			else if (numberList) {
				List<Long> intList = new ArrayList<Long>(valAsList.size());
				for (int i=0; i<valAsList.size(); i++) {
					intList.add(((Number)valAsList.get(i)).longValue());
				}
				return intList;
			}
			else {
				return defaultValue;
			}
		}
		return defaultValue;
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
	 * If this view entry data was received by an optimized lookup that read only one column, you
	 * can use this method to get the programmatic name of the collection column. The method
	 * is mainly used for collection data caching purposes.
	 * 
	 * @return column name or null
	 */
	public String getSingleColumnLookupName() {
		return m_singleColumnLookupName;
	}
	
	/**
	 * If this view entry data was received by an optimized lookup that read only one column,
	 * this method is used to set the programmatic name of that column.
	 * 
	 * @param colName column name or null
	 */
	public void setSingleColumnLookupName(String colName) {
		m_singleColumnLookupName = colName;
	}
	
	/**
	 * Returns the summary data, which is a map with programmatic column names as key and the column value as map value.
	 * Only returns a non-null value if {@link ReadMask#SUMMARY} is used for the lookup.<br>
	 * <br>
	 * The following values are returned for the different column data types:<br>
	 * <br>
	 * <ul>
	 * <li>{@link NotesItem#TYPE_TEXT} - {@link String}</li>
	 * <li>{@link NotesItem#TYPE_TEXT_LIST} - {@link List} of {@link String}</li>
	 * <li>{@link NotesItem#TYPE_NUMBER} - {@link Double}</li>
	 * <li>{@link NotesItem#TYPE_NUMBER_RANGE} - {@link List} with {@link Double} values for number lists or double[] values for number ranges (not sure if Notes views really supports them)</li>
	 * <li>{@link NotesItem#TYPE_TIME} - {@link Calendar}</li>
	 * <li>{@link NotesItem#TYPE_TIME_RANGE} - {@link List} with {@link Calendar} values for number lists or Calendar[] values for datetime ranges</li>
	 * </ul>
	 * 
	 * @return summary data or null
//	 */
//	public Map<String,Object> getSummaryData() {
//		return m_summaryData;
//	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (m_noteId!=null) {
			sb.append(",id="+m_noteId);
		}
		
		String unid = getUNID();
		if (unid!=null) {
			sb.append(",unid="+unid);
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
