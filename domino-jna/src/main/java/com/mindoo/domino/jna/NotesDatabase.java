package com.mindoo.domino.jna;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;

import com.mindoo.domino.jna.NotesCollection.SearchResult;
import com.mindoo.domino.jna.NotesDatabase.ISignCallback.Action;
import com.mindoo.domino.jna.constants.FTIndex;
import com.mindoo.domino.jna.constants.FTSearch;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.OpenCollection;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.NotesBuildVersion;
import com.mindoo.domino.jna.structs.NotesCollectionPosition;
import com.mindoo.domino.jna.structs.NotesFTIndexStats;
import com.mindoo.domino.jna.structs.NotesNamesList32;
import com.mindoo.domino.jna.structs.NotesNamesList64;
import com.mindoo.domino.jna.structs.NotesTimeDate;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

import lotus.domino.Name;
import lotus.domino.NotesException;
import lotus.domino.Session;

/**
 * Object wrapping a Notes database
 * 
 * @author Karsten Lehmann
 */
public class NotesDatabase implements IRecyclableNotesObject {
	private int m_hDB32;
	private long m_hDB64;
	private boolean m_noRecycleDb;
	private String m_asUserCanonical;
	private String m_server;
	private String[] m_paths;
	private Session m_session;
	
	/**
	 * Opens a database either as server or on behalf of a specified user
	 * 
	 * @param session session to read the name of the server the API is running on
	 * @param server database server
	 * @param filePath database filepath
	 */
	public NotesDatabase(Session session, String server, String filePath) {
		this(session, server, filePath, getEffectiveUserName(session));
	}

	/**
	 * Method required to read username in constructor
	 * 
	 * @param session session
	 * @return effective username
	 */
	private static String getEffectiveUserName(Session session) {
		try {
			return session.getEffectiveUserName();
		} catch (NotesException e) {
			throw new NotesError(e.id, NotesErrorUtils.errToString((short) e.id));
		}
	}
	
	/**
	 * Opens a database either as server or on behalf of a specified user
	 * 
	 * @param session session to read the name of the server the API is running on
	 * @param server database server
	 * @param filePath database filepath
	 * @param asUserCanonical user context to open database or null to run as server; will be ignored if code is run locally in the Notes Client
	 */
	public NotesDatabase(Session session, String server, String filePath, String asUserCanonical) {
		m_session = session;
		
		try {
			//make sure server and username are in canonical format
			m_asUserCanonical = !session.isOnServer() ? "" : NotesNamingUtils.toCanonicalName(asUserCanonical);
			if (server==null)
				server = "";
			if (filePath==null)
				throw new NullPointerException("filePath is null");

			server = NotesNamingUtils.toCanonicalName(server);
			if (!"".equals(server)) {
				if (session.isOnServer()) {
					Name nameServer = session.createName(server);
					Name nameCurrServer = session.createName(session.getServerName());
					if (nameServer.getCommon().equalsIgnoreCase(nameCurrServer.getCommon())) {
						//switch to "" as servername if server points to the server the API is running on;
						//this enables advanced lookup features like using NEXT_SELECTED in view traversal
						server = "";
					}
				}
			}
		}
		catch (NotesException e) {
			throw new NotesError(e.id, NotesErrorUtils.errToString((short) e.id));
		}
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory dbServerLMBCS = NotesStringUtils.toLMBCS(server);
		Memory dbFilePathLMBCS = NotesStringUtils.toLMBCS(filePath);
		Memory retDbPathName = new Memory(NotesCAPI.MAXPATH);

		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_OSPathNetConstruct(null, dbServerLMBCS, dbFilePathLMBCS, retDbPathName);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_OSPathNetConstruct(null, dbServerLMBCS, dbFilePathLMBCS, retDbPathName);
			NotesErrorUtils.checkResult(result);
		}
		{
			//reduce length of retDbPathName
			int newLength = 0;
			for (int i=0; i<retDbPathName.size(); i++) {
				byte b = retDbPathName.getByte(i);
				if (b==0) {
					newLength = i;
					break;
				}
			}
			Memory newMem = new Memory(newLength+1);
			for (int i=0; i<newLength; i++) {
				newMem.setByte(i, retDbPathName.getByte(i));
			}
			newMem.setByte(newLength, (byte) 0);
			retDbPathName = newMem;
		}
		
		short result;
		if (NotesJNAContext.is64Bit()) {
			LongBuffer hDB = LongBuffer.allocate(1);
			if (StringUtil.isEmpty(m_asUserCanonical)) {
				//open database as server
				result = notesAPI.b64_NSFDbOpen(retDbPathName, hDB);
				NotesErrorUtils.checkResult(result);
			}
			else {
				//open database as user
				
				//first build usernames list
				Memory userNameLMBCS = NotesStringUtils.toLMBCS(m_asUserCanonical);
				LongByReference rethNamesList = new LongByReference();
				result = notesAPI.b64_NSFBuildNamesList(userNameLMBCS, 0, rethNamesList);
				NotesErrorUtils.checkResult(result);
				long hUserNamesList64 = rethNamesList.getValue();
				
				Pointer namesListBufferPtr = notesAPI.b64_OSLockObject(hUserNamesList64);
				
				try {
					NotesNamesList64 namesList = new NotesNamesList64(namesListBufferPtr);
					namesList.read();

					//setting authenticated flag for the user is required
					namesList.Authenticated = NotesCAPI.NAMES_LIST_AUTHENTICATED | NotesCAPI.NAMES_LIST_PASSWORD_AUTHENTICATED;
					namesList.write();
					namesList.read();
					
					//now try to open the database as this user
					short openOptions = 0;
					NotesTimeDate modifiedTime = null;
					NotesTimeDate retDataModified = new NotesTimeDate();
					NotesTimeDate retNonDataModified = new NotesTimeDate();
					result = notesAPI.b64_NSFDbOpenExtended(retDbPathName, openOptions, 0, modifiedTime, hDB, retDataModified, retNonDataModified);
					NotesErrorUtils.checkResult(result);
				}
				finally {
					notesAPI.b64_OSUnlockObject(hUserNamesList64);
					notesAPI.b64_OSMemFree(hUserNamesList64);
				}
			}

			m_hDB64 = hDB.get(0);
		}
		else {
			IntBuffer hDB = IntBuffer.allocate(1);
			if (StringUtil.isEmpty(m_asUserCanonical)) {
				result = notesAPI.b32_NSFDbOpen(retDbPathName, hDB);
				NotesErrorUtils.checkResult(result);
			}
			else {
				//open database as user
				
				//first build usernames list
				Memory userNameLMBCS = NotesStringUtils.toLMBCS(m_asUserCanonical);
				IntByReference rethNamesList = new IntByReference();
				result = notesAPI.b32_NSFBuildNamesList(userNameLMBCS, 0, rethNamesList);
				NotesErrorUtils.checkResult(result);
				int hUserNamesList32 = rethNamesList.getValue();
				
				Pointer namesListBufferPtr = notesAPI.b32_OSLockObject(hUserNamesList32);
				
				try {
					NotesNamesList32 namesList = new NotesNamesList32(namesListBufferPtr);
					namesList.read();
					//setting authenticated flag for the user is required
					namesList.Authenticated = NotesCAPI.NAMES_LIST_AUTHENTICATED | NotesCAPI.NAMES_LIST_PASSWORD_AUTHENTICATED;
					namesList.write();
					namesList.read();

					//now try to open the database as this user
					short openOptions = 0;
					NotesTimeDate modifiedTime = null;
					NotesTimeDate retDataModified = new NotesTimeDate();
					NotesTimeDate retNonDataModified = new NotesTimeDate();
					result = notesAPI.b32_NSFDbOpenExtended(retDbPathName, openOptions, hUserNamesList32, modifiedTime, hDB, retDataModified, retNonDataModified);
					NotesErrorUtils.checkResult(result);
				}
				finally {
					notesAPI.b32_OSUnlockObject(hUserNamesList32);
					notesAPI.b32_OSMemFree(hUserNamesList32);
				}
			}
			
			m_hDB32 = hDB.get(0);
		}
		NotesGC.__objectCreated(this);
	}

	/**
	 * Internal method to read the session used to create the database
	 * 
	 * @return session
	 * @deprecated will be removed if {@link NotesCollection} can decode the collation info without falling back to the legacy API
	 */
	Session getSession() {
		return m_session;
	}
	
	/**
	 * Returns the server of the database
	 * 
	 * @return server
	 */
	public String getServer() {
		loadPaths();
		return m_server;
	}
	
	/**
	 * Returns the filepath of the database relative to the data directory
	 * 
	 * @return filepath
	 */
	public String getRelativeFilePath() {
		loadPaths();
		return m_paths[0];
	}
	
	/**
	 * Returns the absolute filepath of the database
	 * 
	 * @return filepath
	 */
	public String getAbsoluteFilePathOnLocal() {
		loadPaths();
		return m_paths[1];
	}
	
	/**
	 * Loads the path information from Notes
	 */
	private void loadPaths() {
		if (m_paths==null) {
			NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
			Memory retCanonicalPathName = new Memory(NotesCAPI.MAXPATH);
			Memory retExpandedPathName = new Memory(NotesCAPI.MAXPATH);
			
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_NSFDbPathGet(m_hDB64, retCanonicalPathName, retExpandedPathName);
			}
			else {
				notesAPI.b32_NSFDbPathGet(m_hDB32, retCanonicalPathName, retExpandedPathName);
			}

			String canonicalPathName = NotesStringUtils.fromLMBCS(retCanonicalPathName);
			String expandedPathName = NotesStringUtils.fromLMBCS(retExpandedPathName);
			String relDbPath;
			String absDbPath;
			
			int iPos = canonicalPathName.indexOf("!!");
			if (iPos==-1) {
				//local db
				m_server = "";
				relDbPath = canonicalPathName;
			}
			else {
				m_server = canonicalPathName.substring(0, iPos);
				relDbPath = canonicalPathName.substring(iPos+2);
			}
			iPos = expandedPathName.indexOf("!!");
			if (iPos==-1) {
				absDbPath = expandedPathName;
			}
			else {
				absDbPath = expandedPathName.substring(iPos+2);
			}
			m_paths = new String[] {relDbPath, absDbPath};
		}
	}
	
	public int getHandle32() {
		return m_hDB32;
	}

	public long getHandle64() {
		return m_hDB64;
	}

	/**
	 * Returns the username for this we opened the database
	 * 
	 * @return username in canonical format or null if running as server
	 */
	public String getContextUser() {
		return m_asUserCanonical;
	}

	/**
	 * Check if this object is recycled
	 * 
	 * @return true if recycled
	 */
	public boolean isRecycled() {
		if (NotesJNAContext.is64Bit()) {
			return m_hDB64==0;
		}
		else {
			return m_hDB32==0;
		}
	}

	/**
	 * Recycle this object, if not already recycled
	 */
	public void recycle() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (!m_noRecycleDb) {
			if (NotesJNAContext.is64Bit()) {
				if (m_hDB64!=0) {
					short result = notesAPI.b64_NSFDbClose(m_hDB64);
					NotesErrorUtils.checkResult(result);
					m_hDB64=0;
				}
			}
			else {
				if (m_hDB32!=0) {
					short result = notesAPI.b32_NSFDbClose(m_hDB32);
					NotesErrorUtils.checkResult(result);
					m_hDB32=0;
				}
			}
		}
	}

	/**
	 * Prevent recycling.
	 * 
	 * @deprecated internal framework method, do only use it if you know what you are doing
	 */
	public void setNoRecycleDb() {
		m_noRecycleDb=true;
	}
	
	/**
	 * Checks if the database is already recycled
	 */
	private void checkHandle() {
		if (NotesJNAContext.is64Bit()) {
			if (m_hDB64==0)
				throw new NotesError(0, "Database already recycled");
			NotesGC.__b64_checkValidHandle(m_hDB64);
		}
		else {
			if (m_hDB32==0)
				throw new NotesError(0, "Database already recycled");
			NotesGC.__b32_checkValidHandle(m_hDB32);
		}
	}

	/**
	 * Locates a collection by its name and opens it
	 * 
	 * @param viewName name of the view/collection
	 * @return collection
	 */
	public NotesCollection openCollectionByName(String viewName) {
		return openCollectionByName(viewName, (EnumSet<OpenCollection>) null);
	}
	
	/**
	 * Locates a collection by its name and opens it
	 * 
	 * @param viewName name of the view/collection
	 * @param openFlagSet open flags, see {@link OpenCollection}
	 * @return collection
	 */
	public NotesCollection openCollectionByName(String viewName, EnumSet<OpenCollection> openFlagSet) {
		checkHandle();
		
		int viewNoteId = findCollection(viewName);
		return openCollection(viewName, viewNoteId, openFlagSet);
	}

	/**
	 * Locates a collection by its name and opens it. This method lets you store
	 * the view in a separate database than the one containing the actual data,
	 * which can be useful to reduce database size (by externalizing view indices) and
	 * to let one Domino server index data of another one.
	 * 
	 * @param dbData database containing the data to populate the collection
	 * @param viewName name of the view/collection
	 * @return collection
	 */
	public NotesCollection openCollectionByNameWithExternalData(NotesDatabase dbData, String viewName) {
		return openCollectionByNameWithExternalData(dbData, viewName, (EnumSet<OpenCollection>) null);
	}
	
	/**
	 * Locates a collection by its name and opens it. This method lets you store
	 * the view in a separate database than the one containing the actual data,
	 * which can be useful to reduce database size (by externalizing view indices) and
	 * to let one Domino server index data of another one.
	 * 
	 * @param dbData database containing the data to populate the collection
	 * @param viewName name of the view/collection
	 * @param openFlagSet open flags, see {@link OpenCollection}
	 * @return collection
	 */
	public NotesCollection openCollectionByNameWithExternalData(NotesDatabase dbData, String viewName, EnumSet<OpenCollection> openFlagSet) {
		checkHandle();
		
		int viewNoteId = findCollection(viewName);
		return openCollectionWithExternalData(dbData, viewName, viewNoteId, openFlagSet);
	}

	/**
	 * Opens a collection by its view note id
	 * 
	 * @param name view/collection name
	 * @param viewNoteId view/collection note id
	 * @param openFlagSet open flags, see {@link OpenCollection}
	 * @return collection
	 */
	NotesCollection openCollection(String name, int viewNoteId, EnumSet<OpenCollection> openFlagSet)  {
		return openCollectionWithExternalData(this, name, viewNoteId, openFlagSet);
	}

	/**
	 * Opens a collection by its view note id. This method lets you store
	 * the view in a separate database than the one containing the actual data,
	 * which can be useful to reduce database size (by externalizing view indices) and
	 * to let one Domino server index data of another one.
	 * 
	 * @param dataDb database containing the data to populate the collection
	 * @param name view/collection name
	 * @param viewNoteId view/collection note id
	 * @param openFlagSet open flags, see {@link OpenCollection}
	 * @return collection
	 */
	NotesCollection openCollectionWithExternalData(NotesDatabase dataDb, String name, int viewNoteId, EnumSet<OpenCollection> openFlagSet)  {
		checkHandle();
		
		Memory viewUNID = new Memory(16);
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		NotesIDTable unreadTable = new NotesIDTable();
		
		short openFlags = OpenCollection.toBitMask(openFlagSet); //NotesCAPI.OPEN_NOUPDATE;

		short result;
		NotesCollection newCol;
		if (NotesJNAContext.is64Bit()) {
			LongByReference hCollection = new LongByReference();
			LongByReference collapsedList = new LongByReference();
			collapsedList.setValue(0);
			LongByReference selectedList = new LongByReference();
			selectedList.setValue(0);
			
			if (StringUtil.isEmpty(m_asUserCanonical)) {
				//open view as server
				result = notesAPI.b64_NIFOpenCollection(m_hDB64, dataDb.m_hDB64, viewNoteId, (short) openFlags, unreadTable.getHandle64(), hCollection, null, viewUNID, collapsedList, selectedList);
				NotesErrorUtils.checkResult(result);
			}
			else {
				//open view as user
				
				//first build usernames list
				Memory userNameLMBCS = NotesStringUtils.toLMBCS(m_asUserCanonical);
				LongByReference rethNamesList = new LongByReference();
				result = notesAPI.b64_NSFBuildNamesList(userNameLMBCS, 0, rethNamesList);
				NotesErrorUtils.checkResult(result);
				long hUserNamesList64 = rethNamesList.getValue();
				
				Pointer namesListBufferPtr = notesAPI.b64_OSLockObject(hUserNamesList64);
				
				try {
					NotesNamesList64 namesList = new NotesNamesList64(namesListBufferPtr);
					namesList.read();
					//setting authenticated flag for the user is required
					namesList.Authenticated = NotesCAPI.NAMES_LIST_AUTHENTICATED | NotesCAPI.NAMES_LIST_PASSWORD_AUTHENTICATED;
					namesList.write();
					namesList.read();

					//now try to open collection as this user
					result = notesAPI.b64_NIFOpenCollectionWithUserNameList(m_hDB64, dataDb.m_hDB64, viewNoteId, (short) openFlags, unreadTable.getHandle64(), hCollection, null, viewUNID, collapsedList, selectedList, hUserNamesList64);
					NotesErrorUtils.checkResult(result);
				}
				finally {
					notesAPI.b64_OSUnlockObject(hUserNamesList64);
					notesAPI.b64_OSMemFree(hUserNamesList64);
				}
			}
			
			String sViewUNID = NotesStringUtils.toUNID(viewUNID);
			newCol = new NotesCollection(this, hCollection.getValue(), name, viewNoteId, sViewUNID, new NotesIDTable(collapsedList.getValue()), new NotesIDTable(selectedList.getValue()), unreadTable, m_asUserCanonical);
		}
		else {
			IntByReference hCollection = new IntByReference();
			IntByReference collapsedList = new IntByReference();
			collapsedList.setValue(0);
			IntByReference selectedList = new IntByReference();
			selectedList.setValue(0);
			
			if (StringUtil.isEmpty(m_asUserCanonical)) {
				result = notesAPI.b32_NIFOpenCollection(m_hDB32, dataDb.m_hDB32, viewNoteId, (short) openFlags, unreadTable.getHandle32(), hCollection, null, viewUNID, collapsedList, selectedList);
				NotesErrorUtils.checkResult(result);
			}
			else {
				//open view as user
				
				//first build usernames list
				Memory userNameLMBCS = NotesStringUtils.toLMBCS(m_asUserCanonical);
				IntByReference rethNamesList = new IntByReference();
				result = notesAPI.b32_NSFBuildNamesList(userNameLMBCS, 0, rethNamesList);
				NotesErrorUtils.checkResult(result);
				int hUserNamesList32 = rethNamesList.getValue();
				
				Pointer namesListBufferPtr = notesAPI.b32_OSLockObject(hUserNamesList32);
				
				try {
					NotesNamesList32 namesList = new NotesNamesList32(namesListBufferPtr);
					namesList.read();
					//setting authenticated flag for the user is required
					namesList.Authenticated = NotesCAPI.NAMES_LIST_AUTHENTICATED | NotesCAPI.NAMES_LIST_PASSWORD_AUTHENTICATED;
					namesList.write();
					namesList.read();

					//now try to open collection as this user
					result = notesAPI.b32_NIFOpenCollectionWithUserNameList(m_hDB32, dataDb.m_hDB32, viewNoteId, (short) openFlags, unreadTable.getHandle32(), hCollection, null, viewUNID, collapsedList, selectedList, hUserNamesList32);
					NotesErrorUtils.checkResult(result);
				}
				finally {
					notesAPI.b32_OSUnlockObject(hUserNamesList32);
					notesAPI.b32_OSMemFree(hUserNamesList32);
				}
			}
			
			String sViewUNID = NotesStringUtils.toUNID(viewUNID);
			newCol = new NotesCollection(this, hCollection.getValue(), name, viewNoteId, sViewUNID, new NotesIDTable(collapsedList.getValue()), new NotesIDTable(selectedList.getValue()), unreadTable, m_asUserCanonical);
		}
		
		NotesGC.__objectCreated(newCol);
		return newCol;
	}
	
	/**
	 * Lookup method to find a collection
	 * 
	 * @param collectionName collection name
	 * @return note id of collection
	 */
	public int findCollection(String collectionName) {
		checkHandle();
		
		Memory viewNameLMBCS = NotesStringUtils.toLMBCS(collectionName);

		IntBuffer viewNoteID = IntBuffer.allocate(1);
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NIFFindDesignNoteExt(m_hDB64, viewNameLMBCS, NotesCAPI.NOTE_CLASS_VIEW, NotesStringUtils.toLMBCS(NotesCAPI.DFLAGPAT_VIEWS_AND_FOLDERS), viewNoteID, 0);
		}
		else {
			result = notesAPI.b32_NIFFindDesignNoteExt(m_hDB32, viewNameLMBCS, NotesCAPI.NOTE_CLASS_VIEW, NotesStringUtils.toLMBCS(NotesCAPI.DFLAGPAT_VIEWS_AND_FOLDERS), viewNoteID, 0);
		}
		//throws an error if view cannot be found:
		NotesErrorUtils.checkResult(result);

		return viewNoteID.get(0);
	}

	/**
	 * Performance a fulltext search in the database
	 * 
	 * @param query fulltext query
	 * @param limit Maximum number of documents to return.  Use 0 to return the maximum number of results for the search
	 * @param filterIDTable optional ID table to further refine the search.  Use null if this is not required.
	 * @return search result
	 */
	public SearchResult ftSearch(String query, short limit, NotesIDTable filterIDTable) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		EnumSet<FTSearch> searchOptions = EnumSet.of(FTSearch.RET_IDTABLE);
		int searchOptionsBitMask = FTSearch.toBitMask(searchOptions);
		
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethSearch = new LongByReference();
			
			short result = notesAPI.b64_FTOpenSearch(rethSearch);
			NotesErrorUtils.checkResult(result);

			Memory queryLMBCS = NotesStringUtils.toLMBCS(query);
			IntByReference retNumDocs = new IntByReference();
			LongByReference rethResults = new LongByReference();
			
			result = notesAPI.b64_FTSearch(
					m_hDB64,
					rethSearch,
					0,
					queryLMBCS,
					searchOptionsBitMask,
					limit,
					filterIDTable==null ? 0 : filterIDTable.getHandle64(),
					retNumDocs,
					new Memory(Pointer.SIZE), // Reserved field
					rethResults);
			NotesErrorUtils.checkResult(result);

			result = notesAPI.b64_FTCloseSearch(rethSearch.getValue());
			NotesErrorUtils.checkResult(result);
			
			return new SearchResult(rethResults.getValue()==0 ? null : new NotesIDTable(rethResults.getValue()), retNumDocs.getValue());
		}
		else {
			IntByReference rethSearch = new IntByReference();
			
			short result = notesAPI.b32_FTOpenSearch(rethSearch);
			NotesErrorUtils.checkResult(result);

			Memory queryLMBCS = NotesStringUtils.toLMBCS(query);
			IntByReference retNumDocs = new IntByReference();
			IntByReference rethResults = new IntByReference();
			
			result = notesAPI.b32_FTSearch(
					m_hDB32,
					rethSearch,
					0,
					queryLMBCS,
					searchOptionsBitMask,
					limit,
					filterIDTable==null ? 0 : filterIDTable.getHandle32(),
					retNumDocs,
					new Memory(Pointer.SIZE), // Reserved field
					rethResults);
			NotesErrorUtils.checkResult(result);

			result = notesAPI.b64_FTCloseSearch(rethSearch.getValue());
			NotesErrorUtils.checkResult(result);
			
			return new SearchResult(rethResults.getValue()==0 ? null : new NotesIDTable(rethResults.getValue()), retNumDocs.getValue());
		}
	}

	/**
	 * This function deletes all the notes specified in the ID table.
	 * 
	 * This function is useful when deleting a large number of notes in a remote database,
	 * because it minimizes the network traffic by sending only one request to the Lotus Domino Server.<br>
	 * <br>
	 * Note: This function will return an error if the ID table contains View notes or Design notes.
	 * 
	 * @param idTable ID table of Notes to be deleted
	 */
	public void deleteNotes(NotesIDTable idTable) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFDbDeleteNotes(m_hDB64, idTable.getHandle64(), null);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFDbDeleteNotes(m_hDB32, idTable.getHandle32(), null);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function clears out the replication history information of the specified database replica.
	 * This can also be done using the Notes user interface via the File/Replication/History menu item selection.
	 */
	public void clearReplicationHistory() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFDbClearReplHistory(m_hDB64, 0);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFDbClearReplHistory(m_hDB32, 0);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function returns an ID Table of Note IDs of notes which have been modified in some way
	 * from the given starting time until "now".  The ending time/date is returned, so that this
	 * function can be performed incrementally.<br>
	 * Except when TIMEDATE_MINIMUM is specified, the IDs of notes deleted during the time span will
	 * also be returned in the ID Table, and the IDs of these deleted notes have been ORed with
	 * {@link NotesCAPI#RRV_DELETED} before being added to the table.  You must check the
	 * {@link NotesCAPI#RRV_DELETED} flag when using the resulting table.<br>
	 * <br>
	 * Note: If there are NO modified or deleted notes in the database since the specified time,
	 * the Notes C API returns an error ERR_NO_MODIFIED_NOTES. In our wrapper code, we check for
	 * this error and return an empty {@link NotesIDTable} instead.<br>
	 * <br>
	 * Note: You program is responsible for freeing up the returned id table handle.
	 * 
	 * @param noteClassMask the appropriate NOTE_CLASS_xxx mask for the documents you wish to select. Symbols can be OR'ed to obtain the desired Note classes in the resulting ID Table.  
	 * @param since A TIMEDATE structure containing the starting date used when selecting notes to be added to the ID Table built by this function. To include ALL notes (including those deleted during the time span) of a given note class, use {@link NotesDateTimeUtils#setWildcard(NotesTimeDate)}.  To include ALL notes of a given note class, but excluding those notes deleted during the time span, use {@link NotesDateTimeUtils#setMinimum(NotesTimeDate)}.
	 * @param retUntil A pointer to a {@link NotesTimeDate} structure into which the ending time of this search will be returned.  This can subsequently be used as the starting time in a later search.
	 * @return newly allocated ID Table, you are responsible for freeing the storage when you are done with it using {@link NotesIDTable#recycle()}
	 */
	public NotesIDTable getModifiedNoteTable(short noteClassMask, NotesTimeDate since, NotesTimeDate retUntil) {
		//make sure retUntil is not null
		if (retUntil==null)
			retUntil = new NotesTimeDate();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethTable = new LongByReference();
			short result = notesAPI.b64_NSFDbGetModifiedNoteTable(m_hDB64, noteClassMask, since, retUntil, rethTable);
			if (result == INotesErrorConstants.ERR_NO_MODIFIED_NOTES) {
				return new NotesIDTable();
			}
			NotesErrorUtils.checkResult(result);
			return new NotesIDTable(rethTable.getValue());
		}
		else {
			IntByReference rethTable = new IntByReference();
			short result = notesAPI.b32_NSFDbGetModifiedNoteTable(m_hDB32, noteClassMask, since, retUntil, rethTable);
			if (result == INotesErrorConstants.ERR_NO_MODIFIED_NOTES) {
				return new NotesIDTable();
			}
			NotesErrorUtils.checkResult(result);
			return new NotesIDTable(rethTable.getValue());
		}
	}
	
	/**
	 * Opens and returns the design collection
	 * 
	 * @return design collection
	 */
	public NotesCollection openDesignCollection() {
		NotesCollection col = openCollection("DESIGN", NotesCAPI.NOTE_ID_SPECIAL | NotesCAPI.NOTE_CLASS_DESIGN, null);
		return col;
	}
	
	/**
	 * Callback interface to get notified about progress when signing
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface ISignCallback {
		/** Values to control sign process */
		public enum Action {Stop, Continue}

		/**
		 * Method to skip signing for specific notes
		 * 
		 * @param noteData note data from design collection
		 * @return true to sign
		 */
		public boolean shouldSign(NotesViewEntryData noteData);
		
		/**
		 * Method is called after signing a note
		 * 
		 * @param noteData note data from design collection
		 * @return return value to stop signing
		 */
		public Action noteSigned(NotesViewEntryData noteData);
	}

	/**
	 * Sign all documents of a specified note class (see NOTE_CLASS_* in {@link NotesCAPI}.
	 * 
	 * @param noteClasses bitmask of note classes to sign
	 * @param callback optional callback to get notified about signed notes or null
	 */
	public void signAll(int noteClasses, ISignCallback callback) {
		//TODO improve performance by checking first if the current signer has already signed the documents (like in the legacy API)
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		NotesCollection col = openDesignCollection();
		try {
			NotesCollectionPosition pos = NotesCollectionPosition.toPosition("0");
			boolean moreToDo = true;
			boolean isFirstRun = true;
			while (moreToDo) {
				NotesViewLookupResultData data = col.readEntries(pos, isFirstRun ? EnumSet.of(Navigate.NEXT) : EnumSet.of(Navigate.CURRENT), isFirstRun ? 1 : 0, EnumSet.of(Navigate.NEXT), Integer.MAX_VALUE, EnumSet.of(ReadMask.NOTEID, ReadMask.NOTECLASS, ReadMask.SUMMARY));
				moreToDo = data.hasMoreToDo();
				isFirstRun=false;
				
				List<NotesViewEntryData> entries = data.getEntries();
				for (NotesViewEntryData currEntry : entries) {
					int currNoteClass = currEntry.getNoteClass();
					if ((currNoteClass & noteClasses)!=0) {
						if (callback!=null && !callback.shouldSign(currEntry)) {
							continue;
						}
						
						int currNoteId = currEntry.getNoteId();
						
						boolean expandNote = false;
						if ( ((currNoteClass & NotesCAPI.NOTE_CLASS_FORM)==NotesCAPI.NOTE_CLASS_FORM) || 
								((currNoteClass & NotesCAPI.NOTE_CLASS_INFO)==NotesCAPI.NOTE_CLASS_INFO) ||
								((currNoteClass & NotesCAPI.NOTE_CLASS_HELP)==NotesCAPI.NOTE_CLASS_HELP) ||
								((currNoteClass & NotesCAPI.NOTE_CLASS_FIELD)==NotesCAPI.NOTE_CLASS_FIELD)) {
							
							expandNote = true;
						}
						
						if (NotesJNAContext.is64Bit()) {
							LongByReference rethNote = new LongByReference();
							
							short result = notesAPI.b64_NSFNoteOpen(m_hDB64, currNoteId, expandNote ? NotesCAPI.OPEN_EXPAND : 0, rethNote);
							NotesErrorUtils.checkResult(result);
							try {
								result = notesAPI.b64_NSFNoteSign(rethNote.getValue());
								NotesErrorUtils.checkResult(result);

								if (expandNote) {
									result = notesAPI.b64_NSFNoteContract(rethNote.getValue());
									NotesErrorUtils.checkResult(result);
								}
								
								result = notesAPI.b64_NSFNoteUpdate(rethNote.getValue(), (short) 0);
								NotesErrorUtils.checkResult(result);
							}
							finally {
								result = notesAPI.b64_NSFNoteClose(rethNote.getValue());
								NotesErrorUtils.checkResult(result);
							}
						}
						else {
							IntByReference rethNote = new IntByReference();
							short result = notesAPI.b32_NSFNoteOpen(m_hDB32, currNoteId, expandNote ? NotesCAPI.OPEN_EXPAND : 0, rethNote);
							NotesErrorUtils.checkResult(result);
							try {
								result = notesAPI.b32_NSFNoteSign(rethNote.getValue());
								NotesErrorUtils.checkResult(result);

								if (expandNote) {
									result = notesAPI.b32_NSFNoteContract(rethNote.getValue());
									NotesErrorUtils.checkResult(result);
								}
								
								result = notesAPI.b32_NSFNoteUpdate(rethNote.getValue(), (short) 0);
								NotesErrorUtils.checkResult(result);
							}
							finally {
								result = notesAPI.b32_NSFNoteClose(rethNote.getValue());
								NotesErrorUtils.checkResult(result);
							}
						}
						
						if (callback!=null) {
							Action action = callback.noteSigned(currEntry);
							if (action==Action.Stop) {
								return;
							}
						}
					}
				}
			}
		}
		finally {
			if (col!=null) {
				col.recycle();
			}
		}
	}

	/**
	 * This function creates a new full text index for a local database.<br>
	 * <br>
	 * Full text indexing of a remote database is not supported in the C API.
	 * 
	 * @param options Indexing options. See {@link FTIndex}
	 * @return indexing statistics
	 */
	public NotesFTIndexStats FTIndex(EnumSet<FTIndex> options) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short optionsBitMask = FTIndex.toBitMask(options);
		
		NotesFTIndexStats retStats = new NotesFTIndexStats();
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_FTIndex(m_hDB64, optionsBitMask, null, retStats);
			NotesErrorUtils.checkResult(result);
			retStats.read();
			return retStats;
		}
		else {
			short result = notesAPI.b32_FTIndex(m_hDB32, optionsBitMask, null, retStats);
			NotesErrorUtils.checkResult(result);
			retStats.read();
			return retStats;
		}
	}
	
	/**
	 * This function deletes a full text index for a database.<br>
	 * <br>
	 * This function does not disable full text indexing for a database.
	 * In order to disable full text indexing for a database, use 
	 * NSFDbSetOption(hDb, 0, DBOPTION_FT_INDEX);
	 */
	public void FTDeleteIndex() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_FTDeleteIndex(m_hDB64);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_FTDeleteIndex(m_hDB32);
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * Convenience method to check whether the database is fulltext indexed.
	 * Internally calls {@link #getFTLastIndexTime()} and checks for null
	 * return value.
	 * 
	 * @return true if indexed
	 */
	public boolean isFTIndex() {
		return getFTLastIndexTime() != null;
	}
	
	/**
	 * This routine returns the last time a database was full text indexed.
	 * It can also be used to determine if a database is full text indexed.
	 * If the database is not full text indexed, null is returned.
	 * 
	 * @return last index time or null if not indexed
	 */
	public Calendar getFTLastIndexTime() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
        int gmtOffset = NotesDateTimeUtils.getGMTOffset();
        boolean useDayLight = NotesDateTimeUtils.isDaylightTime();

		if (NotesJNAContext.is64Bit()) {
			NotesTimeDate retTime = new NotesTimeDate();
			short result = notesAPI.b64_FTGetLastIndexTime(m_hDB64, retTime);
			if (result == INotesErrorConstants.ERR_FT_NOT_INDEXED) {
				return null;
			}
			NotesErrorUtils.checkResult(result);
			retTime.read();
			return NotesDateTimeUtils.timeDateToCalendar(useDayLight, gmtOffset, retTime);
		}
		else {
			NotesTimeDate retTime = new NotesTimeDate();
			short result = notesAPI.b32_FTGetLastIndexTime(m_hDB32, retTime);
			if (result == INotesErrorConstants.ERR_FT_NOT_INDEXED) {
				return null;
			}
			NotesErrorUtils.checkResult(result);
			retTime.read();
			return NotesDateTimeUtils.timeDateToCalendar(useDayLight, gmtOffset, retTime);
		}
	}

	/**
	 * This function returns the "major" portion of the build number of the Domino or
	 * Notes executable running on the system where the specified database resides.
	 * Use this information to determine what Domino or Notes release is running on a given system.
	 * The database handle input may represent a local database, or a database that resides
	 * on a Lotus Domino Server.<br>
	 * <br>
	 * Domino or Notes Release 1.0 (all preliminary and final versions) are build numbers 1 to 81.<br>
	 * Domino or Notes Release 2.0 (all preliminary and final versions) are build numbers 82 to 93.<br>
	 * Domino or Notes Release 3.0 (all preliminary and final versions) are build numbers 94 to 118.<br>
	 * Domino or Notes Release 4.0 (all preliminary and final versions) are build numbers 119 to 136.<br>
	 * Domino or Notes Release 4.1 (all preliminary and final versions) are build number 138.<br>
	 * Domino or Notes Release 4.5 (all preliminary and final versions) are build number 140 - 145.<br>
	 * Domino or Notes Release 4.6 (all preliminary and final versions) are build number 147.<br>
	 * Domino or Notes Release 5.0 Beta 1 is build number 161.<br>
	 * Domino or Notes Release 5.0 Beta 2 is build number 163.<br>
	 * Domino or Notes Releases 5.0 - 5.0.11 are build number 166.<br>
	 * Domino or Notes Release Rnext Beta 1 is build number 173.<br>
	 * Domino or Notes Release Rnext Beta 2 is build number 176.<br>
	 * Domino or Notes Release Rnext Beta 3 is build number 178.<br>
	 * Domino or Notes Release Rnext Beta 4 is build number 179.<br>
	 * Domino or Notes 6  Pre-release 1 is build number 183.<br>
	 * Domino or Notes 6  Pre-release 2 is build number 185.<br>
	 * Domino or Notes 6  Release Candidate is build number 190.<br>
	 * Domino or Notes 6 - 6.0.2 are build number 190.<br>
	 * Domino or Notes 6.0.3 - 6.5 are build numbers 191 to 194.<br>
	 * Domino or Notes 7.0 Beta 2 is build number 254.<br>
	 * Domino or Notes 9.0 is build number 400.<br>
	 * Domino or Notes 9.0.1 is build number 405.<br>
	 * 
	 * @return build version
	 */
	public short getParentServerBuildVersion() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		ShortByReference retVersion = new ShortByReference();
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFDbGetBuildVersion(m_hDB64, retVersion);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFDbGetBuildVersion(m_hDB32, retVersion);
			NotesErrorUtils.checkResult(result);
		}
		return retVersion.getValue();
	}
	
	/**
	 * This function returns a BUILDVERSION structure which contains all types of
	 * information about the level of code running on a machine.<br>
	 * <br>
	 * See {@link NotesBuildVersion} for more information.
	 * 
	 * @return version
	 */
	public NotesBuildVersion getParentServerMajMinVersion() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		NotesBuildVersion retVersion = new NotesBuildVersion();
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFDbGetMajMinVersion(m_hDB64, retVersion);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFDbGetMajMinVersion(m_hDB32, retVersion);
			NotesErrorUtils.checkResult(result);
		}
		return retVersion;
	}

}
