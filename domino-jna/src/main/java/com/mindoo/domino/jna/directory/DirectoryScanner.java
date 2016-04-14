package com.mindoo.domino.jna.directory;

import java.util.Calendar;
import java.util.EnumSet;
import java.util.Map;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder.ItemTableData;
import com.mindoo.domino.jna.structs.NotesTimeDate;

import lotus.domino.DbDirectory;
import lotus.domino.Session;

/**
 * This class scans a local or remote Domino data directory for folders or database of
 * various kinds. It has a much better scan performance compared to {@link DbDirectory}
 * in the legacy API and has more functionality, e.g. scanning subdirectories,
 * folders and more file types.
 * 
 * @author Karsten Lehmann
 */
public abstract class DirectoryScanner {
	private Session m_session;
	private String m_serverName;
	private String m_directory;
	private int m_fileType;

	/**
	 * Creates a new scanner instance
	 * 
	 * @param session current session
	 * @param serverName server name, either abbreviated, canonical or common name
	 * @param directory directory to scan or "" for top level
	 * @param fileType type of data to return e,g, {@link NotesCAPI#FILE_DBANY} or {@link NotesCAPI#FILE_DIRS}, optionally or'red with a flag like {@link NotesCAPI#FILE_RECURSE}
	 */
	public DirectoryScanner(Session session, String serverName, String directory, int fileType) {
		m_session = session;
		m_serverName = serverName;
		m_directory = directory==null ? "" : directory;
		m_fileType = fileType;
	}

	/**
	 * Starts the directory scan. During the scan, we call {@link #entryRead(SearchResultData)} with
	 * every entry we found
	 */
	public void scan() {
		NotesDatabase dir = new NotesDatabase(m_session, m_serverName, m_directory, "");
		try {
			dir.search(null, null, EnumSet.of(Search.FILETYPE, Search.SUMMARY), m_fileType, null, new NotesDatabase.ISearchCallback() {

				@Override
				public void noteFound(NotesDatabase parentDb, int noteId, short noteClass, NotesTimeDate created,
						NotesTimeDate modified, ItemTableData summaryBufferData) {

					Map<String,Object> dataAsMap = summaryBufferData.asMap();
					
					Object typeObj = dataAsMap.get("$type");
					if (typeObj instanceof String) {
						String typeStr = (String) typeObj;
						if ("$DIR".equals(typeStr)) {
							String folderName = null;
							Object folderObj = dataAsMap.get("$TITLE");
							if (folderObj instanceof String) {
								folderName = (String) folderObj;
							}
							
							
							String folderPath = null;
							Object folderPathObj = dataAsMap.get("$path");
							if (folderPathObj instanceof String) {
								folderPath = (String) folderObj;
							}

							FolderData folderData = new FolderData();
							folderData.setRawData(dataAsMap);
							folderData.setFolderName(folderName);
							folderData.setFolderPath(folderPath);
							
							entryRead(folderData);
							return;
						}
						else if ("$NOTEFILE".equals(typeStr)) {
							String dbTitle = null;
							
							Object infoObj = dataAsMap.get("$Info");
							if (infoObj instanceof String) {
								//the database title is the first line of the $Info value
								String infoStr = (String) infoObj;
								int iPos = infoStr.indexOf('\n');
								dbTitle = iPos==-1 ? infoStr : infoStr.substring(0, iPos);
							}
							
							Calendar dbCreated = null;
							Object createdObj = dataAsMap.get("$DBCREATED");
							if (createdObj instanceof Calendar) {
								dbCreated = (Calendar) createdObj;
							}
							
							Calendar dbModified = null;
							Object modifiedObj = dataAsMap.get("$Modified");
							if (modifiedObj instanceof Calendar) {
								dbModified = (Calendar) modifiedObj;
							}
							
							String fileName = null;
							Object fileNameObj = dataAsMap.get("$TITLE");
							if (fileNameObj instanceof String) {
								fileName = (String) fileNameObj;
							}
							
							String filePath = null;
							Object filePathObj = dataAsMap.get("$path");
							if (filePathObj instanceof String) {
								filePath = (String) filePathObj;
							}
							
							DatabaseData dbData = new DatabaseData();
							dbData.setRawData(dataAsMap);
							dbData.setTitle(dbTitle);
							dbData.setCreated(dbCreated);
							dbData.setModified(dbModified);
							dbData.setFileName(fileName);
							dbData.setFilePath(filePath);
							
							entryRead(dbData);
							return;
						}
					}
					
					//report default data object if we cannot detect the type
					SearchResultData unknownData = new SearchResultData();
					unknownData.setRawData(dataAsMap);
					entryRead(unknownData);
				}
			});
		}
		finally {
			dir.recycle();
		}

	}

	/**
	 * Implement this method to get notified about each directory entry found
	 * 
	 * @param data, either {@link SearchResultData} or for known types one of its subclasses {@link FolderData} or {@link DatabaseData}
	 */
	protected abstract void entryRead(SearchResultData data);

	/**
	 * Base class for directory scan search results
	 * 
	 * @author Karsten Lehmann
	 */
	public static class SearchResultData {
		private Map<String,Object> m_rawData;

		/**
		 * Returns the raw data of the search result entry
		 * 
		 * @return data
		 */
		public Map<String,Object> getRawData() {
			return m_rawData;
		}

		/**
		 * Sets the raw data of the search result entry
		 * 
		 * @param rawData data
		 */
		public void setRawData(Map<String,Object> rawData) {
			this.m_rawData = rawData;
		}
		
	}
	
	/**
	 * Subclass of {@link SearchResultData} that is used to return
	 * parsed data of folders.
	 * 
	 * @author Karsten Lehmann
	 */
	public static class FolderData extends SearchResultData {
		private String m_fileName;
		private String m_filePath;
		
		/**
		 * Returns the name of the folder
		 * 
		 * @return name
		 */
		public String getFolderName() {
			return m_fileName;
		}
		
		/**
		 * Sets the name of the folder
		 * 
		 * @param folderName name
		 */
		public void setFolderName(String folderName) {
			this.m_fileName = folderName;
		}
		
		/**
		 * Returns the complete relative path of the folder in the
		 * data directory
		 * 
		 * @return path
		 */
		public String getFolderPath() {
			return m_filePath;
		}
		
		/**
		 * Sets the complete relative path of the folder in the
		 * data directory
		 * 
		 * @param folderPath path
		 */
		public void setFolderPath(String folderPath) {
			this.m_filePath = folderPath;
		}
	}

	/**
	 * Subclass of {@link SearchResultData} that is used to return
	 * parsed data of databases.
	 * 
	 * @author Karsten Lehmann
	 */
	public static class DatabaseData extends SearchResultData {
		private String m_title;
		private String m_fileName;
		private String m_filePath;
		private Calendar m_created;
		private Calendar m_modified;

		/**
		 * Returns the database title
		 * 
		 * @return title
		 */
		public String getTitle() {
			return m_title;
		}
		
		/**
		 * Sets the database title
		 * 
		 * @param title title
		 */
		public void setTitle(String title) {
			this.m_title = title;
		}

		/**
		 * Returns the filename of the database
		 * 
		 * @return filename
		 */
		public String getFileName() {
			return m_fileName;
		}

		/**
		 * Sets the filename of the database
		 * 
		 * @param fileName filename
		 */
		public void setFileName(String fileName) {
			this.m_fileName = fileName;
		}

		/**
		 * Returns the complete relative path of the database in the data directory
		 * 
		 * @return path
		 */
		public String getFilePath() {
			return m_filePath;
		}

		/**
		 * Sets the complete relative path of the database in the data directory
		 * 
		 * @param filePath path
		 */
		public void setFilePath(String filePath) {
			this.m_filePath = filePath;
		}

		/**
		 * Returns the database creation date
		 * 
		 * @return creation date
		 */
		public Calendar getCreated() {
			return m_created;
		}

		/**
		 * Sets the database creation date
		 * 
		 * @param created creation date
		 */
		public void setCreated(Calendar created) {
			this.m_created = created;
		}

		/**
		 * Returns the database modification date
		 * 
		 * @return modification date
		 */
		public Calendar getModified() {
			return m_modified;
		}

		/**
		 * Sets the database modification date
		 * 
		 * @param modified modification date
		 */
		public void setModified(Calendar modified) {
			this.m_modified = modified;
		}
		
	}
}
