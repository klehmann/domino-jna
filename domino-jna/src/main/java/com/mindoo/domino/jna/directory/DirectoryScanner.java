package com.mindoo.domino.jna.directory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mindoo.domino.jna.IItemTableData;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.DatabaseOption;
import com.mindoo.domino.jna.constants.FileType;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.utils.StringTokenizerExt;

import lotus.domino.DbDirectory;

/**
 * This class scans a local or remote Domino data directory for folders or database of
 * various kinds. It has a much better scan performance compared to {@link DbDirectory}
 * in the legacy API and has more functionality, e.g. scanning subdirectories,
 * folders and more file types.
 * 
 * @author Karsten Lehmann
 */
public class DirectoryScanner {
	private String m_serverName;
	private String m_directory;
	private EnumSet<FileType> m_fileTypes;

	/**
	 * Creates a new scanner instance
	 * 
	 * @param serverName server name, either abbreviated, canonical or common name
	 * @param directory directory to scan or "" for top level
	 * @param fileTypes type of data to return e,g, {@link FileType#DBANY} or {@link FileType#DIRS}, optionally you can add a flag like {@link FileType#RECURSE}
	 */
	public DirectoryScanner(String serverName, String directory, EnumSet<FileType> fileTypes) {
		m_serverName = serverName;
		m_directory = directory==null ? "" : directory;
		m_fileTypes = fileTypes;
	}
	
	/**
	 * Starts the directory scan. During the scan, we call {@link #entryRead(SearchResultData)} with
	 * every entry we found
	 * 
	 * @return search result; override {@link #isAccepted(SearchResultData)} to apply your own filtering or {@link #entryRead(SearchResultData)} to read results while scanning
	 */
	public List<SearchResultData> scan() {
		return scan(null);
	}
	
	/**
	 * Starts the directory scan. During the scan, we call {@link #entryRead(SearchResultData)} with
	 * every entry we found
	 * 
	 * @param formula optional search formula to filter the returned entries, see {@link SearchResultData#getRawData()} for available fields, e.g. $path="mydb.nsf" or @Word($info;@char(10);2)="db category name"
	 * @return search result; override {@link #isAccepted(SearchResultData)} to apply your own filtering or {@link #entryRead(SearchResultData)} to read results while scanning
	 */
	public List<SearchResultData> scan(String formula) {
		final List<SearchResultData> lookupResult = new ArrayList<DirectoryScanner.SearchResultData>();
		
		NotesDatabase dir = new NotesDatabase(m_serverName, m_directory, "");
		try {
			dir.searchFiles(formula, null, EnumSet.of(Search.FILETYPE, Search.SUMMARY), m_fileTypes, null, new NotesDatabase.SearchCallback() {

				@Override
				public Action noteFound(NotesDatabase parentDb, ISearchMatch searchMatch, IItemTableData summaryBufferData) {
					summaryBufferData.setPreferNotesTimeDates(true);
					Map<String,Object> dataAsMap = summaryBufferData.asMap(true);

					Object typeObj = dataAsMap.get("$type");
					if (typeObj instanceof String) {
						String typeStr = (String) typeObj;
						if ("$DIR".equals(typeStr)) {
							String folderName = null;
							Object folderNameObj = dataAsMap.get("$TITLE");
							if (folderNameObj instanceof String) {
								folderName = (String) folderNameObj;
							}

							String folderPath = null;
							Object folderPathObj = dataAsMap.get("$path");
							if (folderPathObj instanceof String) {
								folderPath = (String) folderPathObj;
							}

							FolderData folderData = new FolderData();
							folderData.setRawData(dataAsMap);
							folderData.setFolderName(folderName);
							folderData.setFolderPath(folderPath);

							if (isAccepted(folderData)) {
								lookupResult.add(folderData);
							}
							
							DirectoryScanner.Action action = entryRead(folderData);
							return action == DirectoryScanner.Action.Continue ? Action.Continue : Action.Stop;
						}
						else if ("$NOTEFILE".equals(typeStr)) {
							String dbTitle = "";
							String dbCategory = "";
							String dbTemplateName = "";
							String dbInheritTemplateName = "";
							
							Object infoObj = dataAsMap.get("$Info");
							if (infoObj instanceof String) {
								// parse weird $Info format:
								// $info=Database title\n
								// Database category\n
								// #1Database template\n
								// #2Database inherit template
								String infoStr = ((String) infoObj).replace("\r", "");
								StringTokenizerExt st = new StringTokenizerExt(infoStr, "\n");
								if (st.hasMoreTokens()) {
									dbTitle = st.nextToken();
									
									boolean secondLine = true;
									while (st.hasMoreTokens()) {
										String currLine = st.nextToken();
										
										if (secondLine) {
											secondLine = false;
											
											if (!currLine.startsWith("1#") && !currLine.startsWith("2#")) {
												dbCategory = currLine;
												continue;
											}
										}
										
										if (currLine.startsWith("#1")) {
											dbTemplateName = currLine.substring(2);
										}
										else if (currLine.startsWith("#2")) {
											dbInheritTemplateName = currLine.substring(2);
										}
									}
								}
							}

							NotesTimeDate dbCreated = null;
							Object createdObj = dataAsMap.get("$DBCREATED");
							if (createdObj instanceof NotesTimeDate) {
								dbCreated = (NotesTimeDate) createdObj;
							}

							NotesTimeDate dbModified = null;
							Object modifiedObj = dataAsMap.get("$Modified");
							if (modifiedObj instanceof NotesTimeDate) {
								dbModified = (NotesTimeDate) modifiedObj;
							}

							NotesTimeDate lastFixup = null;
							Object lastFixupObj = dataAsMap.get("$lastfixup");
							if (lastFixupObj instanceof NotesTimeDate) {
								lastFixup = (NotesTimeDate) lastFixupObj;
							}
							
							NotesTimeDate lastCompact = null;
							Object lastCompactObj = dataAsMap.get("$lastcompact");
							if (lastCompactObj instanceof NotesTimeDate) {
								lastCompact = (NotesTimeDate) lastCompactObj;
							}
							
							NotesTimeDate nonDataMod  = null;
							Object nonDataModObj = dataAsMap.get("$nondatamod");
							if (nonDataModObj instanceof NotesTimeDate) {
								nonDataMod = (NotesTimeDate) nonDataModObj;
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
							dbData.setLastFixup(lastFixup);
							dbData.setLastCompact(lastCompact);
							dbData.setDesignModifiedDate(nonDataMod);
							dbData.setFileName(fileName);
							dbData.setFilePath(filePath);
							dbData.setCategory(dbCategory);
							dbData.setTemplateName(dbTemplateName);
							dbData.setInheritTemplateName(dbInheritTemplateName);
							
							if (isAccepted(dbData)) {
								lookupResult.add(dbData);
							}

							DirectoryScanner.Action action = entryRead(dbData);
							return action == DirectoryScanner.Action.Continue ? Action.Continue : Action.Stop;
						}
					}

					//report default data object if we cannot detect the type
					SearchResultData unknownData = new SearchResultData();
					unknownData.setRawData(dataAsMap);
					DirectoryScanner.Action action = entryRead(unknownData);
					return action == DirectoryScanner.Action.Continue ? Action.Continue : Action.Stop;
				}
			});
		}
		finally {
			dir.recycle();
		}
		return lookupResult;
	}

	/**
	 * Override this method to filter the scan result. The default implementation always returns true.
	 * 
	 * @param data either {@link SearchResultData} or for known types one of its subclasses {@link FolderData} or {@link DatabaseData}
	 * @return true if accepted
	 */
	protected boolean isAccepted(SearchResultData data) {
		return true;
	}

	public static enum Action {Continue, Stop}
	
	/**
	 * Implement this method to get notified about each directory entry found and be
	 * able to cancel the scan process. The default implementation just returns {@link Action#Continue}.
	 * 
	 * @param data either {@link SearchResultData} or for known types one of its subclasses {@link FolderData} or {@link DatabaseData}
	 * @return action to continue scanning or stop
	 */
	protected Action entryRead(SearchResultData data) {
		return Action.Continue;
	}

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
		void setRawData(Map<String,Object> rawData) {
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
		private String m_folderName;
		private String m_folderPath;
		
		/**
		 * Returns the name of the folder
		 * 
		 * @return name
		 */
		public String getFolderName() {
			return m_folderName;
		}
		
		/**
		 * Sets the name of the folder
		 * 
		 * @param folderName name
		 */
		private void setFolderName(String folderName) {
			this.m_folderName = folderName;
		}
		
		/**
		 * Returns the complete relative path of the folder in the
		 * data directory
		 * 
		 * @return path
		 */
		public String getFolderPath() {
			return m_folderPath;
		}
		
		/**
		 * Sets the complete relative path of the folder in the
		 * data directory
		 * 
		 * @param folderPath path
		 */
		private void setFolderPath(String folderPath) {
			this.m_folderPath = folderPath;
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
		private NotesTimeDate m_created;
		private NotesTimeDate m_modified;
		private NotesTimeDate m_lastFixup;
		private NotesTimeDate m_lastCompact;
		private NotesTimeDate m_nonDataMod;
		private String m_category;
		private String m_templateName;
		private String m_ineritTemplateName;
		private Set<DatabaseOption> m_dbOptions;
		
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
		private void setTitle(String title) {
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
		private void setFileName(String fileName) {
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
		private void setFilePath(String filePath) {
			this.m_filePath = filePath;
		}

		/**
		 * Returns the database creation date
		 * 
		 * @return creation date
		 */
		public Calendar getCreated() {
			return m_created == null ? null : m_created.toCalendar();
		}

		/**
		 * Returns the database creation date as a {@link NotesTimeDate}
		 * 
		 * @return creation date
		 */
		public NotesTimeDate getCreatedAsTimeDate() {
			return m_created;
		}
		
		/**
		 * Sets the database creation date
		 * 
		 * @param created creation date
		 */
		private void setCreated(NotesTimeDate created) {
			this.m_created = created;
		}

		/**
		 * Returns the database modification date
		 * 
		 * @return modification date
		 */
		public Calendar getModified() {
			return m_modified == null ? null : m_modified.toCalendar();
		}

		/**
		 * Returns the database modification date as a {@link NotesTimeDate}
		 * 
		 * @return modification date
		 */
		public NotesTimeDate getModifiedAsTimeDate() {
			return m_modified;
		}
		
		/**
		 * Sets the database modification date
		 * 
		 * @param modified modification date
		 */
		private void setModified(NotesTimeDate modified) {
			this.m_modified = modified;
		}
		
		/**
		 * Returns the date of the last fixup
		 * 
		 * @return last fixup
		 */
		public Calendar getLastFixup() {
			return this.m_lastFixup == null ? null : this.m_lastFixup.toCalendar();
		}
		
		/**
		 * Returns the date of the last fixup as a {@link NotesTimeDate}
		 * 
		 * @return last fixup
		 */
		public NotesTimeDate getLastFixupAsTimeDate() {
			return this.m_lastFixup;
		}
		
		/**
		 * Sets the date of the last db fixup
		 * 
		 * @param lastFixup last fixup
		 */
		private void setLastFixup(NotesTimeDate lastFixup) {
			this.m_lastFixup = lastFixup;
		}
		
		/**
		 * Returns the date of the last compact
		 * 
		 * @return last compact
		 */
		public Calendar getLastCompact() {
			return this.m_lastCompact == null ? null : this.m_lastCompact.toCalendar();
		}
		
		/**
		 * Returns the date of the last compact as a {@link NotesTimeDate}
		 * 
		 * @return last compact
		 */
		public NotesTimeDate getLastCompactAsTimeDate() {
			return this.m_lastCompact;
		}
		
		/**
		 * Sets the date of the last db compact
		 * 
		 * @param lastCompact last compact
		 */
		private void setLastCompact(NotesTimeDate lastCompact) {
			this.m_lastCompact = lastCompact;
		}
		
		/**
		 * Returns the date of the last design change
		 * 
		 * @return design modified date
		 */
		public Calendar getDesignModifiedDate() {
			return this.m_nonDataMod == null ? null : this.m_nonDataMod.toCalendar();
		}
		
		/**
		 * Returns the date of the last design change as a {@link NotesTimeDate}
		 * 
		 * @return design modified date
		 */
		public NotesTimeDate getDesignModifiedDateASTimeDate() {
			return this.m_nonDataMod;
		}
		
		/**
		 * Sets the date of the last design change
		 * 
		 * @param nonDataMod design modified date
		 */
		private void setDesignModifiedDate(NotesTimeDate nonDataMod) {
			this.m_nonDataMod = nonDataMod;
		}
		
		/**
		 * Returns the database category
		 * 
		 * @return category or empty string
		 */
		public String getCategory() {
			return this.m_category;
		}
		
		/**
		 * Sets the database category
		 * 
		 * @param category category
		 */
		private void setCategory(String category) {
			this.m_category = category;
		}
		
		/**
		 * Returns the template name
		 * 
		 * @return template name if this database is a template, empty string otherwise
		 */
		public String getTemplateName() {
			return m_templateName;
		}
		
		private void setTemplateName(String templateName) {
			this.m_templateName = templateName;
		}
		
		/**
		 * Returns the name of the template that this database inherits its design from
		 * 
		 * @return inherit template name or empty string
		 */
		public String getInheritTemplateName() {
			return m_ineritTemplateName;
		}
		
		/**
		 * Sets the inherit template name
		 * 
		 * @param inheritTemplateName inherit template name
		 */
		private void setInheritTemplateName(String inheritTemplateName) {
			this.m_ineritTemplateName = inheritTemplateName;
		}
		
		/**
		 * Returns the {@link DatabaseOption} values for the database
		 * 
		 * @return options
		 */
		public Set<DatabaseOption> getOptions() {
			if (m_dbOptions==null) {
				Map<String, Object> rawData = getRawData();
				
				DisposableMemory dbOptionsMem = new DisposableMemory(4 * 4); //DWORD[4]
				try {
					Object opt1AsObj = rawData.get("$DBOPTIONS");
					if (opt1AsObj instanceof Number) {
						dbOptionsMem.setInt(0, ((Number)opt1AsObj).intValue());
					}
					
					Object opt2AsObj = rawData.get("$DBOPTIONS2");
					if (opt2AsObj instanceof Number) {
						dbOptionsMem.setInt(1, ((Number)opt2AsObj).intValue());
					}

					Object opt3AsObj = rawData.get("$DBOPTIONS3");
					if (opt3AsObj instanceof Number) {
						dbOptionsMem.setInt(2, ((Number)opt3AsObj).intValue());
					}

					Object opt4AsObj = rawData.get("$DBOPTIONS4");
					if (opt4AsObj instanceof Number) {
						dbOptionsMem.setInt(3, ((Number)opt4AsObj).intValue());
					}

					byte[] dbOptionsArr = dbOptionsMem.getByteArray(0, 4 * 4);

					m_dbOptions = EnumSet.noneOf(DatabaseOption.class);
					
					for (DatabaseOption currOpt : DatabaseOption.values()) {
						int optionBit = currOpt.getValue();
						int byteOffsetWithBit = optionBit / 8;
						byte byteValueWithBit = dbOptionsArr[byteOffsetWithBit];
						int bitToCheck = (int) Math.pow(2, optionBit % 8);
						
						boolean enabled = (byteValueWithBit & bitToCheck) == bitToCheck;
						if (enabled) {
							m_dbOptions.add(currOpt);
						}
					}
				}
				finally {
					dbOptionsMem.dispose();
				}
			}
			return m_dbOptions;
		}
	}
}
