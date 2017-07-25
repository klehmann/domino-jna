package com.mindoo.domino.jna;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mindoo.domino.jna.NotesCollection.SearchResult;
import com.mindoo.domino.jna.NotesDatabase.SignCallback.Action;
import com.mindoo.domino.jna.constants.FTIndex;
import com.mindoo.domino.jna.constants.FTSearch;
import com.mindoo.domino.jna.constants.GetNotes;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.OpenCollection;
import com.mindoo.domino.jna.constants.OpenNote;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.constants.UpdateNote;
import com.mindoo.domino.jna.errors.FormulaCompilationError;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.formula.FormulaExecution;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesCAPI.NSFFolderAddCallback;
import com.mindoo.domino.jna.internal.NotesCAPI.NSFGetNotesCallback;
import com.mindoo.domino.jna.internal.NotesCAPI.b32_NSFNoteOpenCallback;
import com.mindoo.domino.jna.internal.NotesCAPI.b32_NSFObjectAllocCallback;
import com.mindoo.domino.jna.internal.NotesCAPI.b32_NSFObjectWriteCallback;
import com.mindoo.domino.jna.internal.NotesCAPI.b64_NSFNoteOpenCallback;
import com.mindoo.domino.jna.internal.NotesCAPI.b64_NSFObjectAllocCallback;
import com.mindoo.domino.jna.internal.NotesCAPI.b64_NSFObjectWriteCallback;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.internal.WinNotesCAPI;
import com.mindoo.domino.jna.structs.NotesBuildVersionStruct;
import com.mindoo.domino.jna.structs.NotesDbReplicaInfoStruct;
import com.mindoo.domino.jna.structs.NotesFTIndexStatsStruct;
import com.mindoo.domino.jna.structs.NotesOriginatorIdStruct;
import com.mindoo.domino.jna.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.utils.LegacyAPIUtils;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils.Privileges;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

import lotus.domino.Database;
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
	private String m_replicaID;
	private boolean m_isOnServer;
	private boolean m_authenticateUser;
	private boolean m_loginAsIdOwner;
	NotesNamesList m_namesList;
	private Database m_legacyDbRef;
	
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
	 * @param asUserCanonical user context to open database or null to run as server
	 */
	public NotesDatabase(Session session, String server, String filePath, String asUserCanonical) {
		this(session, server, filePath, (List<String>) null, asUserCanonical);
	}
	
	/**
	 * Opens a database either as server or on behalf of a specified user
	 * 
	 * @param session session to read the name of the server the API is running on
	 * @param server database server
	 * @param filePath database filepath
	 * @param namesForNamesList optional names list for the user to open the database; same content as @Usernameslist, but can be any combination of names, groups or roles (does not have to exist in the directory)
	 */
	public NotesDatabase(Session session, String server, String filePath, List<String> namesForNamesList) {
		this(session, server, filePath, namesForNamesList, null);
	}

	/**
	 * Opens a database either as server or on behalf of a specified user
	 * 
	 * @param session session to read the name of the server the API is running on
	 * @param server database server
	 * @param filePath database filepath
	 * @param namesForNamesList optional names list
	 * @param asUserCanonical user context to open database or null to run as server; will be ignored if code is run locally in the Notes Client
	 */
	private NotesDatabase(Session session, String server, String filePath, List<String> namesForNamesList, String asUserCanonical) {
		m_session = session;
		
		try {
			//make sure server and username are in canonical format
			m_asUserCanonical = NotesNamingUtils.toCanonicalName(asUserCanonical);
			//TODO fix this
			m_isOnServer = session.isOnServer();
			
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
						//switch to "" as servername if server points to the server the API is running on
						server = "";
					}
				}
			}
			
			if (namesForNamesList==null && asUserCanonical!=null && NotesNamingUtils.equalNames(asUserCanonical, session.getUserName())) {
				m_loginAsIdOwner = true;
			}
			else {
				m_loginAsIdOwner = false;
			}
			
			if ("".equals(server)) {
				m_authenticateUser = true;
			}
			else if (session.isOnServer() && (namesForNamesList!=null || !StringUtil.isEmpty(asUserCanonical))) {
				m_authenticateUser = true;
			}
		}
		catch (NotesException e) {
			throw new NotesError(e.id, NotesErrorUtils.errToString((short) e.id));
		}
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory dbServerLMBCS = NotesStringUtils.toLMBCS(server, true);
		Memory dbFilePathLMBCS = NotesStringUtils.toLMBCS(filePath, true);
		Memory retFullNetPath = new Memory(NotesCAPI.MAXPATH);

		short result = notesAPI.OSPathNetConstruct(null, dbServerLMBCS, dbFilePathLMBCS, retFullNetPath);
		NotesErrorUtils.checkResult(result);
		
		{
			//reduce length of retDbPathName
			int newLength = 0;
			for (int i=0; i<retFullNetPath.size(); i++) {
				byte b = retFullNetPath.getByte(i);
				if (b==0) {
					newLength = i;
					break;
				}
			}
			Memory newMem = new Memory(newLength+1);
			for (int i=0; i<newLength; i++) {
				newMem.setByte(i, retFullNetPath.getByte(i));
			}
			newMem.setByte(newLength, (byte) 0);
			retFullNetPath = newMem;
		}
		
		if (NotesJNAContext.is64Bit()) {
			LongBuffer hDB = LongBuffer.allocate(1);
			if ( m_loginAsIdOwner || (StringUtil.isEmpty(m_asUserCanonical) && namesForNamesList==null)) {
				//open database as id owner, not providing a NAMES_LIST
				result = notesAPI.b64_NSFDbOpen(retFullNetPath, hDB);
				NotesErrorUtils.checkResult(result);
			}
			else {
				//open database as user
				
				//first build usernames list
				if (namesForNamesList!=null) {
					m_namesList = NotesNamingUtils.writeNewNamesList(namesForNamesList);
				}
				else {
					m_namesList = NotesNamingUtils.buildNamesList(m_asUserCanonical);
				}
				
				if (m_authenticateUser) {
					//setting authenticated flag for the user is required when running on the server
					NotesNamingUtils.setPrivileges(m_namesList, EnumSet.of(Privileges.Authenticated, Privileges.PasswordAuthenticated));
				}
				
				//now try to open the database as this user
				short openOptions = 0;
				NotesTimeDateStruct modifiedTime = null;
				NotesTimeDateStruct retDataModified = NotesTimeDateStruct.newInstance();
				NotesTimeDateStruct retNonDataModified = NotesTimeDateStruct.newInstance();
				
				int retries = 5;
				do {
					//try opening the database multiple times; we had issues here when opening
					//many dbs remotely that could be solved by retrying
					result = notesAPI.b64_NSFDbOpenExtended(retFullNetPath, openOptions, m_namesList.getHandle64(), modifiedTime, hDB, retDataModified, retNonDataModified);
					retries--;
					if (result!=0) {
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				while (retries>0 && result!=0);
				
				NotesErrorUtils.checkResult(result);
			}

			m_hDB64 = hDB.get(0);
		}
		else {
			IntBuffer hDB = IntBuffer.allocate(1);
			if (m_loginAsIdOwner || (StringUtil.isEmpty(m_asUserCanonical) && namesForNamesList==null)) {
				//open database as id owner, not providing a NAMES_LIST
				result = notesAPI.b32_NSFDbOpen(retFullNetPath, hDB);
				NotesErrorUtils.checkResult(result);
			}
			else {
				//open database as user
				
				//first build usernames list
				if (namesForNamesList!=null) {
					m_namesList = NotesNamingUtils.writeNewNamesList(namesForNamesList);
				}
				else {
					m_namesList = NotesNamingUtils.buildNamesList(m_asUserCanonical);
				}
				
				if (m_authenticateUser) {
					//setting authenticated flag for the user is required when running on the server
					NotesNamingUtils.setPrivileges(m_namesList, EnumSet.of(Privileges.Authenticated, Privileges.PasswordAuthenticated));
				}
				
				//now try to open the database as this user
				short openOptions = 0;
				NotesTimeDateStruct modifiedTime = null;
				NotesTimeDateStruct retDataModified = NotesTimeDateStruct.newInstance();
				NotesTimeDateStruct retNonDataModified = NotesTimeDateStruct.newInstance();
				
				int retries = 5;
				do {
					//try opening the database multiple times; we had issues here when opening
					//many dbs remotely that could be solved by retrying
					result = notesAPI.b32_NSFDbOpenExtended(retFullNetPath, openOptions, m_namesList.getHandle32(), modifiedTime, hDB, retDataModified, retNonDataModified);
					retries--;
					if (result!=0) {
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				while (retries>0 && result!=0);

				NotesErrorUtils.checkResult(result);
			}
			
			m_hDB32 = hDB.get(0);
		}
		NotesGC.__objectCreated(NotesDatabase.class, this);
	}

	/**
	 * Creates a new NotesDatabase
	 * 
	 * @param adaptable adaptable providing enough information to create the database
	 */
	public NotesDatabase(IAdaptable adaptable) {
		Database legacyDB = adaptable.getAdapter(Database.class);
		if (legacyDB!=null) {
			if (isRecycled(legacyDB))
				throw new NotesError(0, "Legacy database already recycled");
			
			long dbHandle = LegacyAPIUtils.getDBHandle(legacyDB);
			if (dbHandle==0)
				throw new NotesError(0, "Could not read db handle");
			
			if (NotesJNAContext.is64Bit()) {
				m_hDB64 = dbHandle;
			}
			else {
				m_hDB32 = (int) dbHandle;
			}
			NotesGC.__objectCreated(NotesDatabase.class, this);
			setNoRecycleDb();
			m_legacyDbRef = legacyDB;
			
			//compute usernames list used
			NotesNote note = createNote();
			List userNamesList = FormulaExecution.evaluate("@UserNamesList", note);
			note.recycle();
			
			m_namesList = NotesNamingUtils.writeNewNamesList(userNamesList);
		}
		else {
			throw new NotesError(0, "Unsupported adaptable parameter");
		}
	}
	
	/** Available encryption strengths for database creation */
	public static enum Encryption {None, Simple, Medium, Strong};
	
	/**
	 * This function creates a new Domino database.
	 * 
	 * @param session current session
	 * @param serverName server name, either canonical, abbreviated or common name
	 * @param filePath filepath to database
	 * @param dbClass specifies the class of the database created. See DB_CLASS for classes that may be specified.
	 * @param forceCreation controls whether the call will overwrite an existing database of the same name. Set to TRUE to overwrite, set to FALSE not to overwrite.
	 * @param options database creation option flags.  See DBCREATE_xxx
	 * @param encryption encryption strength
	 * @param maxFileSize optional.  Maximum file size of the database, in bytes.  In order to specify a maximum file size, use the database class, DBCLASS_BY_EXTENSION and use the option, DBCREATE_MAX_SPECIFIED.
	 */
	public static void createDatabase(Session session, String serverName, String filePath, short dbClass, boolean forceCreation, short options, Encryption encryption, long maxFileSize) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		String fullPath = NotesStringUtils.osPathNetConstruct(null, serverName, filePath);
		Memory fullPathMem = NotesStringUtils.toLMBCS(fullPath, true);
		
		byte encryptStrengthByte;
		switch (encryption) {
		case None:
			encryptStrengthByte = NotesCAPI.DBCREATE_ENCRYPT_NONE;
			break;
		case Simple:
			encryptStrengthByte = NotesCAPI.DBCREATE_ENCRYPT_SIMPLE;
			break;
		case Medium:
			encryptStrengthByte = NotesCAPI.DBCREATE_ENCRYPT_MEDIUM;
			break;
		case Strong:
			encryptStrengthByte = NotesCAPI.DBCREATE_ENCRYPT_STRONG;
			break;
			default:
				encryptStrengthByte = NotesCAPI.DBCREATE_ENCRYPT_NONE;
		}
		
		short result = notesAPI.NSFDbCreateExtended(fullPathMem, dbClass, forceCreation, options, encryptStrengthByte, maxFileSize);
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Internal method to read the session used to create the database
	 * 
	 * @return session
	 * @deprecated will be removed if {@link NotesCollection} can decode the collation info without falling back to the legacy API
	 */
	@Deprecated
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
	 * This function gets the given database's {@link NotesDbReplicaInfo} structure.<br>
	 * <br>
	 * This structure contains information that tells the Domino Replicator how to treat the database.<br>
	 * The ".ID" member enables the Replicator to identify "replicas" of databases.<br>
	 * <br>
	 * The ".CutoffInterval" is the age in days at which deleted document identifiers are purged.<br>
	 * Domino divides this interval into thirds, and for each third of the interval carries
	 * out what amounts to an incremental purge.<br>
	 * <br>
	 * These deleted document identifiers are sometimes called deletion stubs.<br>
	 * <br>
	 * The ".Cutoff" member is a {@link NotesTimeDate} value that is calculated by
	 * subtracting the Cutoff Interval (also called Purge Interval) from today's date.<br>
	 * <br>
	 * It prevents notes that are older than that date from being replicated at all.<br>
	 * <br>
	 * The ".Flags" member is a bit-wise encoded short that stores miscellaneous Replicator flags.<br>
	 * See REPLFLG_xxx for further information on Replicator flags.

	 * @return replica info
	 */
	public NotesDbReplicaInfo getReplicaInfo() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		NotesDbReplicaInfoStruct retReplicationInfo = NotesDbReplicaInfoStruct.newInstance();
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFDbReplicaInfoGet(m_hDB64, retReplicationInfo);
		}
		else {
			result = notesAPI.b32_NSFDbReplicaInfoGet(m_hDB32, retReplicationInfo);
		}
		NotesErrorUtils.checkResult(result);
		return new NotesDbReplicaInfo(retReplicationInfo);
	}
	
	/**
	 * This function sets the given database's {@link NotesDbReplicaInfo} structure.<br>
	 * <br>
	 * Use this function to set specific values, such as the replica ID, in the header
	 * data of a database.<br>
	 * To create a new replica copy of a given database, for example, you must first
	 * create the new database using the NSFDbCreate, then get the replica ID of the
	 * source database via {@link #getReplicaInfo()}, then set this replica ID into
	 * the new database via {@link #setReplicaInfo(NotesDbReplicaInfo)}.<br>
	 * <br>
	 * You may also use {@link #setReplicaInfo(NotesDbReplicaInfo)} to set values
	 * such as the replication flags in the header of the database.<br>
	 * <br>
	 * See the symbolic value REPLFLG_xxx for specific replication settings.

	 * @param replInfo new replica info
	 */
	public void setReplicaInfo(NotesDbReplicaInfo replInfo) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFDbReplicaInfoSet(m_hDB64, replInfo.getAdapter(NotesDbReplicaInfoStruct.class));
		}
		else {
			result = notesAPI.b32_NSFDbReplicaInfoSet(m_hDB32, replInfo.getAdapter(NotesDbReplicaInfoStruct.class));
		}
		NotesErrorUtils.checkResult(result);
		//reset cached replicaId
		m_replicaID = null;
	}
	
	/**
	 * Returns the hex encoded replica id of the database (16 character hex string)
	 * 
	 * @return replica id
	 */
	public String getReplicaID() {
		if (m_replicaID==null) {
			NotesDbReplicaInfo replInfo = getReplicaInfo();
			m_replicaID = replInfo.getReplicaID();
		}
		return m_replicaID;
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
	
	@Override
	public String toString() {
		if (isRecycled()) {
			return "NotesDatabase [recycled]";
		}
		else {
			return "NotesDatabase [handle="+(NotesJNAContext.is64Bit() ? m_hDB64 : m_hDB32)+", server="+getServer()+", filepath="+getRelativeFilePath()+"]";
		}
	}
	
	/**
	 * Loads the path information from Notes
	 */
	private void loadPaths() {
		if (m_paths==null) {
			checkHandle();
			
			NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
			Memory retCanonicalPathName = new Memory(NotesCAPI.MAXPATH);
			Memory retExpandedPathName = new Memory(NotesCAPI.MAXPATH);
			
			if (NotesJNAContext.is64Bit()) {
				notesAPI.b64_NSFDbPathGet(m_hDB64, retCanonicalPathName, retExpandedPathName);
			}
			else {
				notesAPI.b32_NSFDbPathGet(m_hDB32, retCanonicalPathName, retExpandedPathName);
			}

			String canonicalPathName = NotesStringUtils.fromLMBCS(retCanonicalPathName, NotesStringUtils.getNullTerminatedLength(retCanonicalPathName));
			String expandedPathName = NotesStringUtils.fromLMBCS(retExpandedPathName, NotesStringUtils.getNullTerminatedLength(retExpandedPathName));
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
	
	@Override
	public int getHandle32() {
		return m_hDB32;
	}

	@Override
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
	@Override
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
	@Override
	public void recycle() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (!m_noRecycleDb) {
			if (NotesJNAContext.is64Bit()) {
				if (m_hDB64!=0) {
					short result = notesAPI.b64_NSFDbClose(m_hDB64);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectBeeingBeRecycled(NotesDatabase.class, this);
					m_hDB64=0;
				}
			}
			else {
				if (m_hDB32!=0) {
					short result = notesAPI.b32_NSFDbClose(m_hDB32);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectBeeingBeRecycled(NotesDatabase.class, this);
					m_hDB32=0;
				}
			}
			
			if (m_namesList!=null) {
				if (!m_namesList.isFreed()) {
					m_namesList.free();
					m_namesList = null;
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
	
	private boolean isRecycled(Database db) {
		try {
			//call any method to check recycled state
			db.isInService();
		}
		catch (NotesException e) {
			if (e.id==4376 || e.id==4466)
				return true;
		}
		return false;
	}
	
	/**
	 * Checks if the database is already recycled
	 */
	private void checkHandle() {
		if (m_legacyDbRef!=null && isRecycled(m_legacyDbRef))
			throw new NotesError(0, "Wrapped legacy database already recycled");
		
		if (NotesJNAContext.is64Bit()) {
			if (m_hDB64==0)
				throw new NotesError(0, "Database already recycled");
			NotesGC.__b64_checkValidObjectHandle(NotesDatabase.class, m_hDB64);
		}
		else {
			if (m_hDB32==0)
				throw new NotesError(0, "Database already recycled");
			NotesGC.__b32_checkValidObjectHandle(NotesDatabase.class, m_hDB32);
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
	 * Converts bytes in memory to a UNID
	 * 
	 * @param buf memory
	 * @return unid
	 */
	private static String toUNID(Memory buf) {
		Formatter formatter = new Formatter();
		ByteBuffer data = buf.getByteBuffer(0, buf.size()).order(ByteOrder.LITTLE_ENDIAN);
		formatter.format("%16x", data.getLong());
		formatter.format("%16x", data.getLong());
		String unid = formatter.toString().toUpperCase();
		formatter.close();
		return unid;
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
		
		//always enforce reopening; funny things can happen on a Domino server
		//without this flag like sharing collections between users resulting in
		//users seeing the wrong data *sometimes*...
		EnumSet<OpenCollection> openFlagSetClone = openFlagSet==null ? EnumSet.noneOf(OpenCollection.class) : openFlagSet.clone();
		openFlagSetClone.add(OpenCollection.OPEN_REOPEN_COLLECTION);
		
		short openFlags = OpenCollection.toBitMask(openFlagSetClone); //NotesCAPI.OPEN_NOUPDATE;

		short result;
		NotesCollection newCol;
		if (NotesJNAContext.is64Bit()) {
			LongByReference hCollection = new LongByReference();
			LongByReference collapsedList = new LongByReference();
			collapsedList.setValue(0);
			LongByReference selectedList = new LongByReference();
			selectedList.setValue(0);
			
			if (m_namesList==null) {
				//open view as server
				result = notesAPI.b64_NIFOpenCollection(m_hDB64, dataDb.m_hDB64, viewNoteId, (short) openFlags, unreadTable.getHandle64(), hCollection, null, viewUNID, collapsedList, selectedList);
				NotesErrorUtils.checkResult(result);
			}
			else {
				//now try to open collection as this user
				int retries = 5;
				do {
					//try opening the database multiple times; we had issues here when opening
					//many dbs remotely that could be solved by retrying
					result = notesAPI.b64_NIFOpenCollectionWithUserNameList(m_hDB64, dataDb.m_hDB64, viewNoteId, (short) openFlags, unreadTable.getHandle64(), hCollection, null, viewUNID, collapsedList, selectedList, m_namesList.getHandle64());
					retries--;
					if (result!=0) {
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				while (retries>0 && result!=0);

				NotesErrorUtils.checkResult(result);
			}
			
			String sViewUNID = toUNID(viewUNID);
			newCol = new NotesCollection(this, hCollection.getValue(), name, viewNoteId, sViewUNID, new NotesIDTable(collapsedList.getValue(), true), new NotesIDTable(selectedList.getValue(), true), unreadTable, m_asUserCanonical);
		}
		else {
			IntByReference hCollection = new IntByReference();
			IntByReference collapsedList = new IntByReference();
			collapsedList.setValue(0);
			IntByReference selectedList = new IntByReference();
			selectedList.setValue(0);
			
			if (m_namesList==null) {
				result = notesAPI.b32_NIFOpenCollection(m_hDB32, dataDb.m_hDB32, viewNoteId, (short) openFlags, unreadTable.getHandle32(), hCollection, null, viewUNID, collapsedList, selectedList);
				NotesErrorUtils.checkResult(result);
			}
			else {
				//now try to open collection as this user
				int retries = 5;
				do {
					//try opening the database multiple times; we had issues here when opening
					//many dbs remotely that could be solved by retrying
					result = notesAPI.b32_NIFOpenCollectionWithUserNameList(m_hDB32, dataDb.m_hDB32, viewNoteId, (short) openFlags, unreadTable.getHandle32(), hCollection, null, viewUNID, collapsedList, selectedList, m_namesList.getHandle32());
					retries--;
					if (result!=0) {
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				while (retries>0 && result!=0);
				
				NotesErrorUtils.checkResult(result);
			}
			
			String sViewUNID = toUNID(viewUNID);
			newCol = new NotesCollection(this, hCollection.getValue(), name, viewNoteId, sViewUNID, new NotesIDTable(collapsedList.getValue(), true), new NotesIDTable(selectedList.getValue(), true), unreadTable, m_asUserCanonical);
		}
		
		NotesGC.__objectCreated(NotesCollection.class, newCol);
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
		
		Memory viewNameLMBCS = NotesStringUtils.toLMBCS(collectionName, true);

		IntBuffer viewNoteID = IntBuffer.allocate(1);
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NIFFindDesignNoteExt(m_hDB64, viewNameLMBCS, NotesCAPI.NOTE_CLASS_VIEW, NotesStringUtils.toLMBCS(NotesCAPI.DFLAGPAT_VIEWS_AND_FOLDERS, true), viewNoteID, 0);
		}
		else {
			result = notesAPI.b32_NIFFindDesignNoteExt(m_hDB32, viewNameLMBCS, NotesCAPI.NOTE_CLASS_VIEW, NotesStringUtils.toLMBCS(NotesCAPI.DFLAGPAT_VIEWS_AND_FOLDERS, true), viewNoteID, 0);
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
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		EnumSet<FTSearch> searchOptions = EnumSet.of(FTSearch.RET_IDTABLE);
		int searchOptionsBitMask = FTSearch.toBitMask(searchOptions);
		
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethSearch = new LongByReference();
			
			short result = notesAPI.b64_FTOpenSearch(rethSearch);
			NotesErrorUtils.checkResult(result);

			Memory queryLMBCS = NotesStringUtils.toLMBCS(query, true);
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
			
			return new SearchResult(rethResults.getValue()==0 ? null : new NotesIDTable(rethResults.getValue(), false), retNumDocs.getValue());
		}
		else {
			IntByReference rethSearch = new IntByReference();
			
			short result = notesAPI.b32_FTOpenSearch(rethSearch);
			NotesErrorUtils.checkResult(result);

			Memory queryLMBCS = NotesStringUtils.toLMBCS(query, true);
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

			result = notesAPI.b32_FTCloseSearch(rethSearch.getValue());
			NotesErrorUtils.checkResult(result);
			
			return new SearchResult(rethResults.getValue()==0 ? null : new NotesIDTable(rethResults.getValue(), false), retNumDocs.getValue());
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
		checkHandle();

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
	 * This function deletes a note from this database with default flags (0).<br>
	 * <br>
	 * This function allows using extended 32-bit DWORD update options, as described in the entry {@link UpdateNote}.<br>
	 * <br>
	 * It deletes the specified note by updating it with a nil body, and marking the note as a deletion stub.<br>
	 * The deletion stub identifies the deleted note to other replica copies of the database.<br>
	 * This allows the replicator to delete copies of the note from replica databases.
	 * <br>
	 * The deleted note may be of any NOTE_CLASS_xxx.  The active user ID must have sufficient user access
	 * in the databases's Access Control List (ACL) to carry out a deletion on the note or the function
	 * will return an error code.
	 * 
	 * @param noteId note id of note to be deleted
	 */
	public void deleteNote(int noteId) {
		deleteNote(noteId, EnumSet.noneOf(UpdateNote.class));
	}
	
	/**
	 * This function deletes a note from this database.<br>
	 * <br>
	 * This function allows using extended 32-bit DWORD update options, as described in the entry {@link UpdateNote}.<br>
	 * <br>
	 * It deletes the specified note by updating it with a nil body, and marking the note as a deletion stub.<br>
	 * The deletion stub identifies the deleted note to other replica copies of the database.<br>
	 * This allows the replicator to delete copies of the note from replica databases.
	 * <br>
	 * The deleted note may be of any NOTE_CLASS_xxx.  The active user ID must have sufficient user access
	 * in the databases's Access Control List (ACL) to carry out a deletion on the note or the function
	 * will return an error code.
	 * 
	 * @param noteId note id of note to be deleted
	 * @param flags flags
	 */
	public void deleteNote(int noteId, EnumSet<UpdateNote> flags) {
		checkHandle();
		
		int flagsAsInt = UpdateNote.toBitMaskForUpdateExt(flags);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFNoteDeleteExtended(m_hDB64, noteId, flagsAsInt);
		}
		else {
			result = notesAPI.b64_NSFNoteDeleteExtended(m_hDB32, noteId, flagsAsInt);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * This function clears out the replication history information of the specified database replica.
	 * This can also be done using the Notes user interface via the File/Replication/History menu item selection.
	 */
	public void clearReplicationHistory() {
		checkHandle();

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
		checkHandle();

		//make sure retUntil is not null
		if (retUntil==null)
			retUntil = new NotesTimeDate();
		
		NotesTimeDateStruct sinceStruct = since.getAdapter(NotesTimeDateStruct.class);
		NotesTimeDateStruct retUntilStruct = retUntil.getAdapter(NotesTimeDateStruct.class);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		NotesIDTable modifiedNoteTable;
		
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethTable = new LongByReference();
			short result = notesAPI.b64_NSFDbGetModifiedNoteTable(m_hDB64, noteClassMask, sinceStruct, retUntilStruct, rethTable);
			if (result == INotesErrorConstants.ERR_NO_MODIFIED_NOTES) {
				return new NotesIDTable();
			}
			NotesErrorUtils.checkResult(result);
			return new NotesIDTable(rethTable.getValue(), false);
		}
		else {
			IntByReference rethTable = new IntByReference();
			short result = notesAPI.b32_NSFDbGetModifiedNoteTable(m_hDB32, noteClassMask, sinceStruct, retUntilStruct, rethTable);
			if (result == INotesErrorConstants.ERR_NO_MODIFIED_NOTES) {
				return new NotesIDTable();
			}
			NotesErrorUtils.checkResult(result);
			return new NotesIDTable(rethTable.getValue(), false);
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
	public static abstract class SignCallback {
		/** Values to control sign process */
		public enum Action {Stop, Continue}

		/**
		 * Override this method to get the full summary data in the callback
		 * {@link #shouldSign(NotesViewEntryData, String)}. For performance reasons,
		 * the default implementation returns false.
		 * 
		 * @return true to retrieve summary data for design elements
		 */
		public boolean shouldReadSummaryDataFromDesignCollection() {
			return false;
		}
		
		/**
		 * Method to skip signing for specific notes
		 * 
		 * @param noteData note data from design collection
		 * @param currentSigner current design element signer
		 * @return true to sign
		 */
		public abstract boolean shouldSign(NotesViewEntryData noteData, String currentSigner);
		
		/**
		 * Method is called after signing a note
		 * 
		 * @param noteData note data from design collection
		 * @return return value to stop signing
		 */
		public abstract Action noteSigned(NotesViewEntryData noteData);
	}

	/**
	 * Opens an agent in the databaser
	 * 
	 * @param agentName agent name
	 * @return agent or null if not found
	 */
	public NotesAgent getAgent(String agentName) {
		checkHandle();

		Memory agentNameLMBCS = NotesStringUtils.toLMBCS(agentName, true);

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		IntBuffer retAgentNoteID = IntBuffer.allocate(1);
		retAgentNoteID.clear();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NIFFindDesignNoteExt(m_hDB64, agentNameLMBCS, NotesCAPI.NOTE_CLASS_FILTER, NotesStringUtils.toLMBCS(NotesCAPI.DFLAGPAT_TOOLSRUNMACRO, true), retAgentNoteID, 0);
		}
		else {
			result = notesAPI.b32_NIFFindDesignNoteExt(m_hDB32, agentNameLMBCS, NotesCAPI.NOTE_CLASS_FILTER, NotesStringUtils.toLMBCS(NotesCAPI.DFLAGPAT_TOOLSRUNMACRO, true), retAgentNoteID, 0);
		}
		if (result==1028) {
			//Entry not found in index
			return null;
		}
		
		//throws an error if agent cannot be found:
		NotesErrorUtils.checkResult(result);
		
		int agentNoteId = retAgentNoteID.get(0);
		if (agentNoteId==0) {
			throw new NotesError(0, "Agent not found in database: "+agentName);
		}
		
		NotesAgent agent;
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethAgent = new LongByReference();
			
			result = notesAPI.b64_AgentOpen(m_hDB64, agentNoteId, rethAgent);
			NotesErrorUtils.checkResult(result);
			
			agent = new NotesAgent(this, agentNoteId, rethAgent.getValue());
		}
		else {
			IntByReference rethAgent = new IntByReference();
			
			result = notesAPI.b32_AgentOpen(m_hDB32, agentNoteId, rethAgent);
			NotesErrorUtils.checkResult(result);
			
			agent = new NotesAgent(this, agentNoteId, rethAgent.getValue());
		}
		NotesGC.__objectCreated(NotesAgent.class, agent);
		
		return agent;
	}
	
	/**
	 * Sign all documents of a specified note class (see NOTE_CLASS_* in {@link NotesCAPI}.
	 * 
	 * @param noteClasses bitmask of note classes to sign
	 * @param callback optional callback to get notified about signed notes or null
	 */
	public void signAll(int noteClasses, SignCallback callback) {
		checkHandle();

		//TODO improve performance by checking first if the current signer has already signed the documents (like in the legacy API)
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		String signer;
		try {
			signer = m_session.getUserName();
		} catch (NotesException e) {
			throw new NotesError(e.id, NotesErrorUtils.errToString((short) e.id));
		}
		
		NotesCollection col = openDesignCollection();
		try {
			NotesCollectionPosition pos = new NotesCollectionPosition("0");
			boolean moreToDo = true;
			boolean isFirstRun = true;
			while (moreToDo) {
				boolean shouldReadSummaryDataFromDesignCollection = callback!=null ? callback.shouldReadSummaryDataFromDesignCollection() : false;
				EnumSet<ReadMask> readMask = EnumSet.of(ReadMask.NOTEID, ReadMask.NOTECLASS);
				if (shouldReadSummaryDataFromDesignCollection) {
					readMask.add(ReadMask.SUMMARY);
				}
				NotesViewLookupResultData data = col.readEntries(pos, isFirstRun ? EnumSet.of(Navigate.NEXT) : EnumSet.of(Navigate.CURRENT), isFirstRun ? 1 : 0, EnumSet.of(Navigate.NEXT), Integer.MAX_VALUE, readMask);
				moreToDo = data.hasMoreToDo();
				isFirstRun=false;
				
				List<NotesViewEntryData> entries = data.getEntries();
				for (NotesViewEntryData currEntry : entries) {
					int currNoteClass = currEntry.getNoteClass();
					if ((currNoteClass & noteClasses)!=0) {
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
								NotesTimeDateStruct retWhenSigned = NotesTimeDateStruct.newInstance();
								Memory retSigner = new Memory(NotesCAPI.MAXUSERNAME);
								Memory retCertifier = new Memory(NotesCAPI.MAXUSERNAME);
								
								result = notesAPI.b64_NSFNoteVerifySignature(rethNote.getValue(), null, retWhenSigned, retSigner, retCertifier);
								
								boolean signRequired = false;
								String currNoteSigner;
								if (result != 0) {
									signRequired = true;
									currNoteSigner = "";
								}
								else {
									currNoteSigner = NotesStringUtils.fromLMBCS(retSigner, NotesStringUtils.getNullTerminatedLength(retSigner));
									if (signer.equalsIgnoreCase(currNoteSigner)) {
										//already signed by current user
										continue;
									}
								}
								
								if (callback!=null && !callback.shouldSign(currEntry, currNoteSigner)) {
									signRequired = false;
								}
								
								if (signRequired) {

									result = notesAPI.b64_NSFNoteSign(rethNote.getValue());
									NotesErrorUtils.checkResult(result);

									if (expandNote) {
										result = notesAPI.b64_NSFNoteContract(rethNote.getValue());
										NotesErrorUtils.checkResult(result);
									}
									
									result = notesAPI.b64_NSFNoteUpdateExtended(rethNote.getValue(), 0);
									NotesErrorUtils.checkResult(result);
								}
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
								NotesTimeDateStruct retWhenSigned = NotesTimeDateStruct.newInstance();
								Memory retSigner = new Memory(NotesCAPI.MAXUSERNAME);
								Memory retCertifier = new Memory(NotesCAPI.MAXUSERNAME);
								
								result = notesAPI.b32_NSFNoteVerifySignature(rethNote.getValue(), null, retWhenSigned, retSigner, retCertifier);
								
								boolean signRequired = false;
								String currNoteSigner;
								if (result != 0) {
									signRequired = true;
									currNoteSigner = "";
								}
								else {
									currNoteSigner = NotesStringUtils.fromLMBCS(retSigner, NotesStringUtils.getNullTerminatedLength(retSigner));
									if (signer.equalsIgnoreCase(currNoteSigner)) {
										//already signed by current user
										continue;
									}
								}
								
								if (callback!=null && !callback.shouldSign(currEntry, currNoteSigner)) {
									signRequired = false;
								}

								if (signRequired) {
									result = notesAPI.b32_NSFNoteSign(rethNote.getValue());
									NotesErrorUtils.checkResult(result);

									if (expandNote) {
										result = notesAPI.b32_NSFNoteContract(rethNote.getValue());
										NotesErrorUtils.checkResult(result);
									}
									
									result = notesAPI.b32_NSFNoteUpdateExtended(rethNote.getValue(), 0);
									NotesErrorUtils.checkResult(result);
								}
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
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short optionsBitMask = FTIndex.toBitMask(options);
		
		NotesFTIndexStatsStruct retStats = NotesFTIndexStatsStruct.newInstance();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_FTIndex(m_hDB64, optionsBitMask, null, retStats);
		}
		else {
			result = notesAPI.b32_FTIndex(m_hDB32, optionsBitMask, null, retStats);
		}
		NotesErrorUtils.checkResult(result);
		retStats.read();
		
		return new NotesFTIndexStats(retStats.DocsAdded, retStats.DocsUpdated, retStats.DocsDeleted, retStats.BytesIndexed);
	}
	
	/**
	 * This function deletes a full text index for a database.<br>
	 * <br>
	 * This function does not disable full text indexing for a database.
	 * In order to disable full text indexing for a database, use 
	 * NSFDbSetOption(hDb, 0, DBOPTION_FT_INDEX);
	 */
	public void FTDeleteIndex() {
		checkHandle();

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
	 * Checks whether a database is located on a remote server
	 * 
	 * @return true if remote
	 */
	public boolean isRemote() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short isRemote;
		if (NotesJNAContext.is64Bit()) {
			isRemote = notesAPI.b64_NSFDbIsRemote(m_hDB64);
		}
		else {
			isRemote = notesAPI.b32_NSFDbIsRemote(m_hDB32);
		}
		return isRemote==1;
	}
	
	/**
	 * This routine returns the last time a database was full text indexed.
	 * It can also be used to determine if a database is full text indexed.
	 * If the database is not full text indexed, null is returned.
	 * 
	 * @return last index time or null if not indexed
	 */
	public Calendar getFTLastIndexTime() {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
        int gmtOffset = NotesDateTimeUtils.getGMTOffset();
        boolean useDayLight = NotesDateTimeUtils.isDaylightTime();

		if (NotesJNAContext.is64Bit()) {
			NotesTimeDateStruct retTime = NotesTimeDateStruct.newInstance();
			short result = notesAPI.b64_FTGetLastIndexTime(m_hDB64, retTime);
			if (result == INotesErrorConstants.ERR_FT_NOT_INDEXED) {
				return null;
			}
			NotesErrorUtils.checkResult(result);
			retTime.read();
			
			NotesTimeDate retTimeWrap = new NotesTimeDate(retTime);
			return NotesDateTimeUtils.timeDateToCalendar(useDayLight, gmtOffset, retTimeWrap);
		}
		else {
			NotesTimeDateStruct retTime = NotesTimeDateStruct.newInstance();
			short result = notesAPI.b32_FTGetLastIndexTime(m_hDB32, retTime);
			if (result == INotesErrorConstants.ERR_FT_NOT_INDEXED) {
				return null;
			}
			NotesErrorUtils.checkResult(result);
			retTime.read();
			
			NotesTimeDate retTimeWrap = new NotesTimeDate(retTime);
			return NotesDateTimeUtils.timeDateToCalendar(useDayLight, gmtOffset, retTimeWrap);
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
		checkHandle();

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
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		NotesBuildVersionStruct retVersion = NotesBuildVersionStruct.newInstance();
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFDbGetMajMinVersion(m_hDB64, retVersion);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFDbGetMajMinVersion(m_hDB32, retVersion);
			NotesErrorUtils.checkResult(result);
		}
		retVersion.read();
		return new NotesBuildVersion(retVersion.MajorVersion,
				retVersion.MinorVersion, retVersion.QMRNumber, retVersion.QMUNumber,
				retVersion.HotfixNumber, retVersion.Flags, retVersion.FixpackNumber, retVersion.Spare);
	}

	public static interface ISearchCallback extends NotesSearch.ISearchCallback {
		
	}
	
	/**
	 * This function scans all the notes in a database or files in a directory.<br>
	 * <br>
	 * Based on several search criteria, the function calls a user-supplied routine (an action routine)
	 * for every note or file that matches the criteria. NSFSearch is a powerful function that provides
	 * the general search mechanism for tasks that process all or some of the documents in a
	 * database or all or some of the databases in a directory.<br>
	 * <br>
	 * Specify a formula argument to improve efficiency when processing a subset of the notes in a database.<br>
	 * <br>
	 * In addition, the formula argument can be used to return computed "on-the-fly" information.<br>
	 * <br>
	 * To do this, you specify that a value returned from a formula is to be stored in a
	 * temporary field of each note.<br>
	 * <br>
	 * This temporary field and its value is then accessible in the summary buffer received by
	 * the NSFSearch action routine without having to open the note.<br>
	 * <br>
	 * For example, suppose you want the size of each note found by NSFSearch.<br>
	 * Do the following before the call to NSFSearch:<br>
	 * Call search with a formula like this:<br>
	 * "DEFAULT dLength := @DocLength; @All"<br>
	 * and specify {@link Search#SUMMARY} for the SearchFlags argument.<br>
	 * <br>
	 * In the action routine of NSFSearch, if you get a search match, look at the summary information.<br>
	 * The dLength field will be one of the items in the summary information buffer.<br>
	 * <br>
	 * Specify a note class to restrict the search to certain classes of notes.<br>
	 * Specify {@link NotesCAPI#NOTE_CLASS_DOCUMENT} to find documents.<br>
	 * Specify the "since" argument to limit the search to notes created or modified
	 * in the database since a certain time/date.<br>
	 * When used to search a database, NSFSearch will search the database file sequentially
	 * if NULL is passed as the "Since" time.<br>
	 * If the search is not time-constrained (the "Since" argument is NULL or specifies
	 * the TIMEDATE_WILDCARD, ANYDAY/ALLDAY), then NSFSearch may find a given note more
	 * than once during the same search. If a non-time-constrained search passes a
	 * certain note to the action routine, and that note is subsequently updated,
	 * then NSFSearch may find that note again and pass it to the action routine a
	 * second time during the same search. This may happen if Domino or Notes relocates
	 * the updated note to a position farther down in the file. If your algorithm requires
	 * processing each note once and only once, then use time-constrained searches.<br>
	 * Save the return value of type {@link NotesTimeDate} of the present search and use
	 * that as the "Since" time on the next search.<br>
	 * <br>
	 * Alternatively, build an ID table as you search, avoid updating notes in the action
	 * routine, and process the ID table after the search completes. ID tables are
	 * guaranteed not to contain a given ID more than once.
	 * 
	 * @param formula formula or null
	 * @param viewTitle optional view title that will be returned for "@ ViewTitle" within the formula or null
	 * @param searchFlags flags to control searching ({@link Search})
	 * @param noteClassMask bitmask of noteclasses to search
	 * @param since The date of the earliest modified note that is matched. The note's "Modified in this file" date is compared to this date. Specify NULL if you do not wish any filtering by date.
	 * @param callback callback to be called for every found note
	 * @return The ending (current) time/date of this search. Returned so that it can be used in a subsequent call to {@link #search(String, String, EnumSet, int, NotesTimeDate, ISearchCallback)} as the "Since" argument.
	 * @throws FormulaCompilationError if formula syntax is invalid
	 */
	public NotesTimeDate search(final String formula, String viewTitle, final EnumSet<Search> searchFlags, int noteClassMask, NotesTimeDate since, final ISearchCallback callback) throws FormulaCompilationError {
		NotesTimeDate endTimeDate = NotesSearch.search(this, null, formula, viewTitle, searchFlags, noteClassMask, since, callback);
		return endTimeDate;
	}
	
	/**
	 * Data container that stores the lookup result for note info
	 * 
	 * @author Karsten Lehmann
	 */
	public static class NoteInfo {
		private int m_noteId;
		private NotesOriginatorIdStruct m_oid;
		private NotesOriginatorId m_oidWrap;
		private boolean m_isDeleted;
		private boolean m_notPresent;
		
		private NoteInfo(int noteId, NotesOriginatorIdStruct oid, boolean isDeleted, boolean notPresent) {
			m_noteId = noteId;
			m_oid = oid;
			m_isDeleted = isDeleted;
			m_notPresent = notPresent;
		}
		
		/**
		 * Returns the note id used to look up this data
		 * 
		 * @return note id
		 */
		public int getNoteId() {
			return m_noteId;
		}
		
		/**
		 * Returns the raw {@link NotesOriginatorId} object containing the
		 * data we also provide via direct methods
		 * 
		 * @return OID
		 */
		public NotesOriginatorId getOID() {
			if (m_oidWrap==null) {
				m_oidWrap = m_oid==null ? null : new NotesOriginatorId(m_oid);
			}
			return m_oidWrap;
		}
		
		/**
		 * Returns the sequence number
		 * 
		 * @return sequence number
		 */
		public int getSequence() {
			return m_oid==null ? 0 : m_oid.Sequence;
		}
		
		/**
		 * Returns the sequence time ( = "Modified (initially)")
		 * 
		 * @return sequence time
		 */
		public NotesTimeDate getSequenceTime() {
			NotesOriginatorId oidWrap = getOID();
			if (oidWrap!=null) {
				return oidWrap.getSequenceTime();
			}
			return null;
		}
		
		/**
		 * Returns the UNID as hex string
		 * 
		 * @return UNID or null if the note could not be found
		 */
		public String getUnid() {
			return m_oid!=null ? m_oid.getUNIDAsString() : null;
		}
		
		/**
		 * Returns true if the note has already been deleted
		 * 
		 * @return true if deleted
		 */
		public boolean isDeleted() {
			return m_isDeleted;
		}
		
		/**
		 * Returns true if the note currently exists in the database
		 * 
		 * @return true if note exists
		 */
		public boolean exists() {
			return !m_notPresent;
		}
	}

	/**
	 * Extension of {@link NoteInfo} with additional note lookup data
	 * 
	 * @author Karsten Lehmann
	 */
	public static class NoteInfoExt extends NoteInfo {
		private NotesTimeDateStruct m_modified;
		private short m_noteClass;
		private NotesTimeDateStruct m_addedToFile;
		private short m_responseCount;
		private int m_parentNoteId;
		
		private NoteInfoExt(int noteId, NotesOriginatorIdStruct oid, boolean isDeleted, boolean notPresent,
				NotesTimeDateStruct modified, short noteClass, NotesTimeDateStruct addedToFile, short responseCount,
				int parentNoteId) {
			
			super(noteId, oid, isDeleted, notPresent);
			m_modified = modified;
			m_noteClass = noteClass;
			m_addedToFile = addedToFile;
			m_responseCount = responseCount;
			m_parentNoteId = parentNoteId;
		}
		
		/**
		 * Returns the value for "Modified in this file"
		 * 
		 * @return date
		 */
		public NotesTimeDate getModified() {
			return m_modified==null ? null : new NotesTimeDate(m_modified);
		}
		
		/**
		 * Returns the note class
		 * 
		 * @return class
		 */
		public short getNoteClass() {
			return m_noteClass;
		}
		
		/**
		 * Returns the value for "Added in this file"
		 * 
		 * @return date
		 */
		public NotesTimeDate getAddedToFile() {
			return m_addedToFile==null ? null : new NotesTimeDate(m_addedToFile);
		}
		
		/**
		 * Returns the number of responses
		 * 
		 * @return response count
		 */
		public short getResponseCount() {
			return m_responseCount;
		}
		
		/**
		 * Returns the note id of the parent note or 0
		 * 
		 * @return parent note id
		 */
		public int getParentNoteId() {
			return m_parentNoteId;
		}
	}
	
	/**
	 * Convenience to convert note ids to UNIDs.
	 * The method internally calls {@link NotesDatabase#getMultiNoteInfo(int[])}.
	 * 
	 * 
	 * @param noteIds note ids to look up
	 * @param retUnidsByNoteId map is populated with found UNIDs
	 * @param retNoteIdsNotFound set is populated with any note id that could not be found
	 */
	public void toUnids(int[] noteIds, Map<Integer,String> retUnidsByNoteId, Set<Integer> retNoteIdsNotFound) {
		NoteInfo[] infoArr = getMultiNoteInfo(noteIds);
		for (NoteInfo currInfo : infoArr) {
			if (currInfo.exists()) {
				retUnidsByNoteId.put(currInfo.getNoteId(), currInfo.getUnid());
			}
			else {
				retNoteIdsNotFound.add(currInfo.getNoteId());
			}
		}
	}
	
	/**
	 * Get the note's the Originator ID (OID) structure, the time and date the note was last
	 * modified, the NOTE_CLASS_xxx, the time and date it was added to the database,
	 * the number of response documents and its parent's NoteID.
	 * 
	 * @param noteId note id
	 * @return info object with data
	 */
	public NoteInfoExt getNoteInfoExt(int noteId) {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		NotesOriginatorIdStruct retNoteOID = NotesOriginatorIdStruct.newInstance();
		NotesTimeDateStruct retModified = NotesTimeDateStruct.newInstance();
		ShortByReference retNoteClass = new ShortByReference();
		NotesTimeDateStruct retAddedToFile = NotesTimeDateStruct.newInstance();
		ShortByReference retResponseCount = new ShortByReference();
		IntByReference retParentNoteID = new IntByReference();
		boolean isDeleted = false;
		//not sure if we can check this via error code:
		boolean notPresent = false;
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFDbGetNoteInfoExt(m_hDB64, noteId, retNoteOID, retModified, retNoteClass, retAddedToFile, retResponseCount, retParentNoteID);
			if (result==INotesErrorConstants.ERR_NOTE_DELETED) {
				isDeleted = true;
			}
			else if (result==INotesErrorConstants.ERR_INVALID_NOTE) {
				notPresent = true;
			}
			else {
				NotesErrorUtils.checkResult(result);
			}
		}
		else {
			short result = notesAPI.b32_NSFDbGetNoteInfoExt(m_hDB32, noteId, retNoteOID, retModified, retNoteClass, retAddedToFile, retResponseCount, retParentNoteID);
			if (result==INotesErrorConstants.ERR_NOTE_DELETED) {
				isDeleted = true;
			}
			else if (result==INotesErrorConstants.ERR_INVALID_NOTE) {
				notPresent = true;
			}
			else {
				NotesErrorUtils.checkResult(result);
			}
		}
		
		NoteInfoExt info = new NoteInfoExt(noteId, retNoteOID, isDeleted, notPresent, retModified,
				retNoteClass.getValue(), retAddedToFile, retResponseCount.getValue(), retParentNoteID.getValue());
		
		return info;
	}
	
	/**
	 * This method can be used to get information for a number documents in a
	 * database from their note ids in a single call.<br>
	 * the data returned by this method is the {@link NotesOriginatorId}, which contains
	 * the UNID of the document, the sequence number and the sequence time ("Modified initially" time).<br>
	 * <br>
	 * In addition, the method checks whether a document exists or has been deleted.<br>
	 * <br>
	 * Please note that the method can only handle max. 65535 note ids, because it's
	 * using a WORD / short datatype for the count internally to call the C API.
	 * 
	 * @param noteIds array of note ids
	 * @return lookup results, same size and order as <code>noteIds</code> array
	 * @throws IllegalArgumentException if note id array has too many entries (more than 65535)
	 */
	public NoteInfo[] getMultiNoteInfo(int[] noteIds) {
		checkHandle();

		if (noteIds.length ==0) {
			return new NoteInfo[0];
		}
		
		if (noteIds.length > 65535) {
			throw new IllegalArgumentException("Max 65535 note ids are supported");
		}
		
		NoteInfo[] retNoteInfo;
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			LongByReference retHandle = new LongByReference();
			short result = notesAPI.b64_OSMemAlloc((short) 0, noteIds.length * 4, retHandle);
			NotesErrorUtils.checkResult(result);

			long retHandleLong = retHandle.getValue();
			try {
				Pointer inBufPtr = notesAPI.b64_OSLockObject(retHandleLong);
				
				Pointer currInBufPtr = inBufPtr;
				int offset = 0;
				
				for (int i=0; i<noteIds.length; i++) {
					currInBufPtr.setInt(0, noteIds[i]);
					offset += 4;
					currInBufPtr = inBufPtr.share(offset);
				}
				
				notesAPI.b64_OSUnlockObject(retHandleLong);

				LongByReference retSize = new LongByReference();
				LongByReference rethOutBuf = new LongByReference();
				short options = NotesCAPI.fINFO_OID | NotesCAPI.fINFO_ALLOW_HUGE | NotesCAPI.fINFO_NOTEID;
				
				result = notesAPI.b64_NSFDbGetMultNoteInfo(m_hDB64, (short) (noteIds.length & 0xffff), options, retHandleLong, retSize, rethOutBuf);
				NotesErrorUtils.checkResult(result);

				long rethOutBufLong = rethOutBuf.getValue();
				
				//decode return buffer
				int entrySize = 4 /* note id */ + NotesCAPI.oidSize;
				long retSizeLong = retSize.getValue();
				if (retSizeLong != noteIds.length*entrySize) {
					throw new IllegalStateException("Unexpected size of return data. Expected "+noteIds.length*entrySize+" bytes for data of "+noteIds.length+" ids, got "+retSizeLong+" bytes");
				}
				
				Pointer outBufPtr = notesAPI.b64_OSLockObject(rethOutBuf.getValue());
				try {
					retNoteInfo = decodeMultiNoteLookupData(noteIds, outBufPtr);
					
				}
				finally {
					notesAPI.b64_OSUnlockObject(rethOutBufLong);
					notesAPI.b64_OSMemFree(rethOutBufLong);
				}
			}
			finally {
				notesAPI.b64_OSMemFree(retHandleLong);
			}
		}
		else {
			IntByReference retHandle = new IntByReference();
			short result = notesAPI.b32_OSMemAlloc((short) 0, noteIds.length * 4, retHandle);
			NotesErrorUtils.checkResult(result);

			int retHandleInt = retHandle.getValue();
			try {
				Pointer inBufPtr = notesAPI.b32_OSLockObject(retHandleInt);
				
				Pointer currInBufPtr = inBufPtr;
				int offset = 0;
				
				for (int i=0; i<noteIds.length; i++) {
					currInBufPtr.setInt(0, noteIds[i]);
					offset += 4;
					currInBufPtr = inBufPtr.share(offset);
				}
				
				notesAPI.b32_OSUnlockObject(retHandleInt);

				LongByReference retSize = new LongByReference();
				IntByReference rethOutBuf = new IntByReference();
				short options = NotesCAPI.fINFO_OID | NotesCAPI.fINFO_ALLOW_HUGE | NotesCAPI.fINFO_NOTEID;
				
				result = notesAPI.b32_NSFDbGetMultNoteInfo(m_hDB32, (short) (noteIds.length & 0xffff), options, retHandleInt, retSize, rethOutBuf);
				NotesErrorUtils.checkResult(result);

				int rethOutBufInt = rethOutBuf.getValue();
				
				//decode return buffer
				int entrySize = 4 /* note id */ + NotesCAPI.oidSize;
				long retSizeLong = retSize.getValue();
				if (retSizeLong != noteIds.length*entrySize) {
					throw new IllegalStateException("Unexpected size of return data. Expected "+noteIds.length*entrySize+" bytes for data of "+noteIds.length+" ids, got "+retSizeLong+" bytes");
				}
				
				Pointer outBufPtr = notesAPI.b32_OSLockObject(rethOutBuf.getValue());
				try {
					retNoteInfo = decodeMultiNoteLookupData(noteIds, outBufPtr);
				}
				finally {
					notesAPI.b32_OSUnlockObject(rethOutBufInt);
					notesAPI.b32_OSMemFree(rethOutBufInt);
				}
			}
			finally {
				notesAPI.b32_OSMemFree(retHandleInt);
			}
		}

		return retNoteInfo;
	}
	
	/**
	 * Helper method to extract the return data of method {@link #getMultNoteInfo(int[])}
	 * 
	 * @param noteIds note ids used for lookup
	 * @param outBufPtr buffer pointer
	 * @return array of note info objects
	 */
	private NoteInfo[] decodeMultiNoteLookupData(int[] noteIds, Pointer outBufPtr) {
		NoteInfo[] retNoteInfo = new NoteInfo[noteIds.length];
		
		Pointer entryBufPtr = outBufPtr;
		
		for (int i=0; i<noteIds.length; i++) {
			int offsetInEntry = 0;
			
			int currNoteId = noteIds[i];
			int returnedNoteId = entryBufPtr.getInt(0);

			offsetInEntry += 4;

			Pointer fileTimeDatePtr = entryBufPtr.share(offsetInEntry);
			NotesTimeDateStruct fileTimeDate = NotesTimeDateStruct.newInstance(fileTimeDatePtr);
			fileTimeDate.read();
			
			offsetInEntry += 8;
			
			Pointer noteTimeDatePtr = entryBufPtr.share(offsetInEntry);
			NotesTimeDateStruct noteTimeDate = NotesTimeDateStruct.newInstance(noteTimeDatePtr);
			noteTimeDate.read();
			
			offsetInEntry += 8;
			
			int sequence = entryBufPtr.getInt(offsetInEntry);

			offsetInEntry += 4;
			
			Pointer sequenceTimePtr = entryBufPtr.share(offsetInEntry);
			NotesTimeDateStruct sequenceTimeDate = NotesTimeDateStruct.newInstance(sequenceTimePtr);
			sequenceTimeDate.read();
			
			offsetInEntry += 8;

			NotesOriginatorIdStruct oid = NotesOriginatorIdStruct.newInstance();
			oid.File = fileTimeDate;
			oid.Note = noteTimeDate;
			oid.Sequence = sequence;
			oid.SequenceTime = sequenceTimeDate;
			
			
			entryBufPtr = entryBufPtr.share(offsetInEntry);
			
			boolean isDeleted = (returnedNoteId & NotesCAPI.NOTEID_RESERVED) == NotesCAPI.NOTEID_RESERVED;
			boolean isNotPresent = returnedNoteId==0;
			retNoteInfo[i] = new NoteInfo(currNoteId, oid, isDeleted, isNotPresent);
		}
		return retNoteInfo;
	}
	
	/**
	 * This function reads a note into memory and returns a handle to the in-memory copy.<br>
	 * 
	 * @param noteIdStr The note ID as hex string of the note that you want to open.
	 * @return note
	 */
	public NotesNote openNoteById(String noteIdStr) {
		return openNoteById(noteIdStr, EnumSet.noneOf(OpenNote.class));
	}
	
	/**
	 * This function reads a note into memory and returns a handle to the in-memory copy.<br>
	 * <br>
	 * If the note is marked as unread, by default this function does not change the unread mark.<br>
	 * You can use the {@link OpenNote#MARK_READ} flag to change an unread mark to read for remote databases.
	 * 
	 * @param noteIdStr The note ID as hex string of the note that you want to open.
	 * @param openFlags Flags that control the manner in which the note is opened. This, in turn, controls what information about the note is available to you and how it is structured. The flags are defined in {@link OpenNote}.
	 * @return note
	 */
	public NotesNote openNoteById(String noteIdStr, EnumSet<OpenNote> openFlags) {
		return openNoteById(Integer.parseInt(noteIdStr, 16), openFlags);
	}
	
	/**
	 * This function permanently deletes the specified "soft deleted" note from
	 * the specified database.<br>
	 * The deleted note may be of any NOTE_CLASS_xxx. The active user ID must have
	 * sufficient user access in the databases's Access Control List (ACL) to carry
	 * out a deletion on the note or the function will throw an error.

	 * @param softDelNoteId The ID of the note that you want to delete.
	 */
	public void hardDeleteNote(int softDelNoteId) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFNoteHardDelete(m_hDB64, softDelNoteId, 0);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFNoteHardDelete(m_hDB32, softDelNoteId, 0);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function reads a "soft deleted" note into memory.<br>
	 * Its input is a database handle and a note ID within that database.<br>
	 * Use {@link NotesNote#update(EnumSet)} to restore this "soft deleted" note.

	 * @param noteId The ID of the "soft deleted" note to open
	 * @return note
	 */
	public NotesNote openSoftDeletedNoteById(int noteId) {
		checkHandle();
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethNote = new LongByReference();
			short result = notesAPI.b64_NSFNoteOpenSoftDelete(m_hDB64, noteId, 0, rethNote);
			NotesErrorUtils.checkResult(result);
			
			long hNote = rethNote.getValue();
			NotesNote note = new NotesNote(this, hNote);
			NotesGC.__objectCreated(NotesNote.class, note);
			
			return note;
		}
		else {
			IntByReference rethNote = new IntByReference();
			short result = notesAPI.b32_NSFNoteOpenSoftDelete(m_hDB32, noteId, 0, rethNote);
			NotesErrorUtils.checkResult(result);
			
			int hNote = rethNote.getValue();
			NotesNote note = new NotesNote(this, hNote);
			NotesGC.__objectCreated(NotesNote.class, note);
			
			return note;
		}
	}
	
	/**
	 * This function reads a note into memory and returns a handle to the in-memory copy.<br>
	 * 
	 * @param noteId The note ID of the note that you want to open.
	 * @return note
	 */
	public NotesNote openNoteById(int noteId) {
		return openNoteById(noteId, EnumSet.noneOf(OpenNote.class));
	}
	
	/**
	 * This function reads a note into memory and returns a handle to the in-memory copy.<br>
	 * <br>
	 * If the note is marked as unread, by default this function does not change the unread mark.<br>
	 * You can use the {@link OpenNote#MARK_READ} flag to change an unread mark to read for remote databases.
	 * 
	 * @param noteId The note ID of the note that you want to open.
	 * @param openFlags Flags that control the manner in which the note is opened. This, in turn, controls what information about the note is available to you and how it is structured. The flags are defined in {@link OpenNote}.
	 * @return note
	 */
	public NotesNote openNoteById(int noteId, EnumSet<OpenNote> openFlags) {
		checkHandle();

		int openFlagsBitmask = OpenNote.toBitMaskForOpenExt(openFlags);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethNote = new LongByReference();
			short result = notesAPI.b64_NSFNoteOpenExt(m_hDB64, noteId, openFlagsBitmask, rethNote);
			NotesErrorUtils.checkResult(result);
			
			long hNote = rethNote.getValue();
			NotesNote note = new NotesNote(this, hNote);
			NotesGC.__objectCreated(NotesNote.class, note);
			
			return note;
		}
		else {
			IntByReference rethNote = new IntByReference();
			short result = notesAPI.b32_NSFNoteOpenExt(m_hDB32, noteId, openFlagsBitmask, rethNote);
			NotesErrorUtils.checkResult(result);
			
			int hNote = rethNote.getValue();
			NotesNote note = new NotesNote(this, hNote);
			NotesGC.__objectCreated(NotesNote.class, note);
			
			return note;
		}
	}
	
	/**
	 * This function takes the Universal Note ID and reads the note into memory and returns
	 * a handle to the in-memory copy.
	 * @param unid UNID
	 * @return note
	 */
	public NotesNote openNoteByUnid(String unid) {
		return openNoteByUnid(unid, EnumSet.noneOf(OpenNote.class));
	}
	
	/**
	 * This function takes the Universal Note ID and reads the note into memory and returns
	 * a handle to the in-memory copy.<br>
	 * This function only supports the set of 16-bit WORD options described in the entry {@link OpenNote}.

	 * @param unid UNID
	 * @param openFlags open flags
	 * @return note
	 */
	public NotesNote openNoteByUnid(String unid, EnumSet<OpenNote> openFlags) {
		checkHandle();

		short openFlagsBitmask = OpenNote.toBitMaskForOpen(openFlags);
		NotesUniversalNoteIdStruct unidObj = NotesUniversalNoteIdStruct.fromString(unid);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethNote = new LongByReference();
			short result = notesAPI.b64_NSFNoteOpenByUNID(m_hDB64, unidObj, openFlagsBitmask, rethNote);
			NotesErrorUtils.checkResult(result);
			
			long hNote = rethNote.getValue();
			NotesNote note = new NotesNote(this, hNote);
			NotesGC.__objectCreated(NotesNote.class, note);
			
			return note;
		}
		else {
			IntByReference rethNote = new IntByReference();
			short result = notesAPI.b32_NSFNoteOpenByUNID(m_hDB32, unidObj, openFlagsBitmask, rethNote);
			NotesErrorUtils.checkResult(result);
			
			int hNote = rethNote.getValue();
			NotesNote note = new NotesNote(this, hNote);
			NotesGC.__objectCreated(NotesNote.class, note);
			
			return note;
		}
	}
	
	/**
	 * Creates a new in-memory note
	 * 
	 * @return note
	 */
	public NotesNote createNote() {
		checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			LongByReference retNoteHandle = new LongByReference();
			short result = notesAPI.b64_NSFNoteCreate(m_hDB64, retNoteHandle);
			NotesErrorUtils.checkResult(result);
			NotesNote note = new NotesNote(this, retNoteHandle.getValue());
			NotesGC.__objectCreated(NotesNote.class, note);
			return note;
		}
		else {
			IntByReference retNoteHandle = new IntByReference();
			short result = notesAPI.b32_NSFNoteCreate(m_hDB32, retNoteHandle);
			NotesErrorUtils.checkResult(result);
			NotesNote note = new NotesNote(this, retNoteHandle.getValue());
			NotesGC.__objectCreated(NotesNote.class, note);
			return note;
		}
	}

	/**
	 * Rename a local database or template file name. Allows to 'move' a huge
	 * database blazingly fast. If you move the application to another directory,
	 * you have to check if the directory exists and create the target
	 * directory prior to calling this method<br>
	 * <br>
	 * Author: Ulrich Krause
	 * 
	 * @param dbNameOld
	 *            The old file name of the local database or template
	 * @param dbNameNew
	 *            The new file name of the local database or template.
	 */
	public static void renameDatabase(String dbNameOld, String dbNameNew) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		Memory dbNameOldLMBCS = NotesStringUtils.toLMBCS(dbNameOld, true);
		Memory dbNameNewLMBCS = NotesStringUtils.toLMBCS(dbNameNew, true);

		short result = notesAPI.NSFDbRename(dbNameOldLMBCS, dbNameNewLMBCS);

		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Gets the value of a database option
	 * 
	 * @param optionBit see DBOPTBIT_XXX constants in {@link NotesCAPI}
	 * @return true if the option is enabled, false if the option is disabled
	 */
	public boolean getOption(int optionBit) {
		checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory retDbOptions = new Memory(4 * 4); //DWORD[4]
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFDbGetOptionsExt(m_hDB64, retDbOptions);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFDbGetOptionsExt(m_hDB32, retDbOptions);
			NotesErrorUtils.checkResult(result);
		}
		byte[] dbOptionsArr = retDbOptions.getByteArray(0, 4 * 4);

		int byteOffsetWithBit = optionBit / 8;
		byte byteValueWithBit = dbOptionsArr[byteOffsetWithBit];
		int bitToCheck = (int) Math.pow(2, optionBit % 8);
		
		boolean enabled = (byteValueWithBit & bitToCheck) == bitToCheck;
		return enabled;
	}
	
	/**
	 * Sets the value of a database option
	 * 
	 * @param optionBit see DBOPTBIT_XXX constants in {@link NotesCAPI}
	 * @param flag true to set the option
	 */
	public void setOption(int optionBit, boolean flag) {
		checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		int byteOffsetWithBit = optionBit / 8;
		int bitToCheck = (int) Math.pow(2, optionBit % 8);

		byte[] optionsWithBitSetArr = new byte[4*4];
		optionsWithBitSetArr[byteOffsetWithBit] = (byte) (bitToCheck & 0xff);
		
		Memory dbOptionsWithBitSetMem = new Memory(4 * 4);
		dbOptionsWithBitSetMem.write(0, optionsWithBitSetArr, 0, 4*4);
		
		if (NotesJNAContext.is64Bit()) {
			short result;
			if (flag) {
				//use dbOptionsMem both for the new value and for the bitmask, since the new value is 1
				result = notesAPI.b64_NSFDbSetOptionsExt(m_hDB64, dbOptionsWithBitSetMem, dbOptionsWithBitSetMem);
			}
			else {
				Memory nullBytesMem = new Memory(4 * 4);
				nullBytesMem.clear();
				result = notesAPI.b64_NSFDbSetOptionsExt(m_hDB64, nullBytesMem, dbOptionsWithBitSetMem);
			}
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result;
			if (flag) {
				result = notesAPI.b32_NSFDbSetOptionsExt(m_hDB32, dbOptionsWithBitSetMem, dbOptionsWithBitSetMem);
			}
			else {
				Memory nullBytesMem = new Memory(4 * 4);
				nullBytesMem.clear();
				result = notesAPI.b32_NSFDbSetOptionsExt(m_hDB32, nullBytesMem, dbOptionsWithBitSetMem);
			}
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * Hides the design of this database
	 */
	public void hideDesign() {
		checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFHideDesign(m_hDB64, m_hDB64, 0, 0);
		}
		else {
			result = notesAPI.b32_NSFHideDesign(m_hDB32, m_hDB32, 0, 0);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Called once before any others but only if going to a server that is R6 or greater.
	 * If {@link GetNotes#ORDER_BY_SIZE} is specified in options the two DWORD parameters, TotalSizeLow and TotalSizeHigh, provide the approximate total size of the bytes to be returned in the notes and objects. These values are intended to be used for progress indication
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IGetNotesCallback {
		
		public void gettingNotes(int totalSizeLow, int totalSizeHigh);
		
	}
	
	/**
	 * This function is called for each note retrieved. If non-NULL, this is called for each note
	 * after all objects have been retrieved (if {@link GetNotes#SEND_OBJECTS} is specified)
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface INoteOpenCallback {
		
		public void noteOpened(NotesNote note, int noteId, short status);
		
	}
	
	/**
	 * If {@link GetNotes#SEND_OBJECTS} is specified and <code>objectDb</code> is not NULL,
	 * this function is called exactly once for each object to provide the caller with information
	 * about the object's size and ObjectID. The intent is to allow for the physical allocation
	 * for the object if need be. It is called before the {@link INoteOpenCallback} for the corresponding note
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IObjectAllocCallback {
		
		public void objectAllocated(NotesNote note, int oldRRV, short status, int objectSize);
		
	}
	
	/**
	 * This function is called for each "chunk" of each object if {@link GetNotes#SEND_OBJECTS}
	 * is specified and <code>objectDb</code> is not NULL. For each object this will be
	 * called one or more times
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IObjectWriteCallback {
		
		public void objectChunkWritten(NotesNote note, int oldRRV, short status, ByteBuffer buffer);
		
	}
	
	/**
	 * {@link GetNotes#GET_FOLDER_ADDS} is specified but {@link GetNotes#APPLY_FOLDER_ADDS} is not, this function is called for each note after the {@link INoteOpenCallback} function is called
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IFolderAddCallback {
		
		public void addedToFolder(String unid);
		
	}
	
	/**
	 * This function will return a stream of notes to the caller through several callback functions.<br>
	 * <br>
	 * It can be used to quickly and incrementally read a large number of notes from a database,
	 * skipping the transfer of item values where the item's sequence number is lower or equal a specified value
	 * (see <code>sinceSeqNum</code> parameter).
	 * 
	 * @param noteIds note ID(s) of note(s) to be retrieved
	 * @param noteOpenFlags flags that control the manner in which the note is opened. This, in turn, controls what information about the note is available to you and how it is structured. The flags are defined in {@link OpenNote} and may be or'ed together to combine functionality.
	 * @param sinceSeqNum since sequence number; controls which fields are accessible in the returned notes; e.g. if you specify a very high value, items with lower or equal sequence number have the type {@link NotesItem#TYPE_UNAVAILABLE}
	 * @param controlFlags  Flags that control the actions of the function during note retrieval. The flags are defined in {@link GetNotes}.
	 * @param objectDb If objects are being retrieved {@link GetNotes#SEND_OBJECTS} and this value is not NULL, objects will be stored in this database and attached to the incoming notes prior to {@link INoteOpenCallback} being called.  
	 * @param getNotesCallback Called once before any others but only if going to a server that is R6 or greater. If {@link GetNotes#ORDER_BY_SIZE} is specified in options the two DWORD parameters, TotalSizeLow and TotalSizeHigh, provide the approximate total size of the bytes to be returned in the notes and objects. These values are intended to be used for progress indication
	 * @param noteOpenCallback This function is called for each note retrieved. If non-NULL, this is called for each note after all objects have been retrieved (if {@link GetNotes#SEND_OBJECTS} is specified)
	 * @param objectAllocCallback If {@link GetNotes#SEND_OBJECTS} is specified and <code>objectDb</code> is not NULL, this function is called exactly once for each object to provide the caller with information about the object's size and ObjectID. The intent is to allow for the physical allocation for the object if need be. It is called before the {@link INoteOpenCallback} for the corresponding note
	 * @param objectWriteCallback This function is called for each "chunk" of each object if {@link GetNotes#SEND_OBJECTS} is specified and <code>objectDb</code> is not NULL. For each object this will be called one or more times
	 * @param folderSinceTime {@link NotesTimeDate} containing a time/date value specifying the earliest time to retrieve notes from the folder. If {@link GetNotes#GET_FOLDER_ADDS} is specified this is the time folder operations should be retrieved from
	 * @param folderAddCallback If {@link GetNotes#GET_FOLDER_ADDS} is specified but {@link GetNotes#APPLY_FOLDER_ADDS} is not, this function is called for each note after the {@link INoteOpenCallback} function is called
	 */
	public void getNotes(int[] noteIds, EnumSet<OpenNote>[] noteOpenFlags, int[] sinceSeqNum,
			EnumSet<GetNotes> controlFlags, NotesDatabase objectDb,
			final IGetNotesCallback getNotesCallback, final INoteOpenCallback noteOpenCallback,
			final IObjectAllocCallback objectAllocCallback, final IObjectWriteCallback objectWriteCallback,
			NotesTimeDate folderSinceTime, final IFolderAddCallback folderAddCallback) {
		
		checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		if (noteIds.length==0)
			return;
		
		if (noteIds.length != noteOpenFlags.length) {
			throw new NotesError(0, "Size of note open flags array does not match note ids array ("+noteOpenFlags.length+"!="+noteIds.length+")");
		}
		if (noteIds.length != sinceSeqNum.length) {
			throw new NotesError(0, "Size of sinceSeqNum array does not match note ids array ("+sinceSeqNum.length+"!="+noteIds.length+")");
		}
		
		NotesTimeDateStruct folderSinceTimeStruct = folderSinceTime==null ? null : folderSinceTime.getAdapter(NotesTimeDateStruct.class);
		
		Memory arrNoteIdsMem = new Memory(4 * noteIds.length);
		for (int i=0; i<noteIds.length; i++) {
			arrNoteIdsMem.setInt(4*i, noteIds[i]);
		}
		Memory arrNoteOpenFlagsMem = new Memory(4 * noteOpenFlags.length);
		for (int i=0; i<noteOpenFlags.length; i++) {
			arrNoteOpenFlagsMem.setInt(4*i, OpenNote.toBitMaskForOpenExt(noteOpenFlags[i]));
		}
		Memory arrSinceSeqNumMem = new Memory(4 * sinceSeqNum.length);
		for (int i=0; i<sinceSeqNum.length; i++) {
			arrSinceSeqNumMem.setInt(4*i, sinceSeqNum[i]);
		}
		
		final Throwable[] exception = new Throwable[1];
		NSFGetNotesCallback cGetNotesCallback = null;
		
		if (getNotesCallback!=null) {
			if (notesAPI instanceof WinNotesCAPI) {
				cGetNotesCallback = new WinNotesCAPI.NSFGetNotesCallbackWin() {

					@Override
					public short invoke(Pointer param, int totalSizeLow, int totalSizeHigh) {
						try {
							getNotesCallback.gettingNotes(totalSizeLow, totalSizeHigh);
							return 0;
						}
						catch (RuntimeException e) {
							exception[0] = e;
							return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
						}
						catch (Throwable t) {
							exception[0] = t;
							return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
						}
					}};
			}
			else {
				cGetNotesCallback = new NSFGetNotesCallback() {

					@Override
					public short invoke(Pointer param, int totalSizeLow, int totalSizeHigh) {
						try {
							getNotesCallback.gettingNotes(totalSizeLow, totalSizeHigh);
							return 0;
						}
						catch (RuntimeException e) {
							exception[0] = e;
							return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
						}
						catch (Throwable t) {
							exception[0] = t;
							return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
						}
					}};
			}
		}
		
		NSFFolderAddCallback cFolderAddCallback = null;
		
		if (folderAddCallback!=null) {
			if (notesAPI instanceof WinNotesCAPI) {
				cFolderAddCallback = new WinNotesCAPI.NSFFolderAddCallbackWin() {

					@Override
					public short invoke(Pointer param, NotesUniversalNoteIdStruct noteUNID, int opBlock, int opBlockSize) {
						try {
							folderAddCallback.addedToFolder(noteUNID==null ? null : noteUNID.toString());
							return 0;
						}
						catch (RuntimeException e) {
							exception[0] = e;
							return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
						}
						catch (Throwable t) {
							exception[0] = t;
							return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
						}
					}
				};
			}
			else {
				cFolderAddCallback = new NSFFolderAddCallback() {

					@Override
					public short invoke(Pointer param, NotesUniversalNoteIdStruct noteUNID, int opBlock, int opBlockSize) {
						try {
							folderAddCallback.addedToFolder(noteUNID==null ? null : noteUNID.toString());
							return 0;
						}
						catch (RuntimeException e) {
							exception[0] = e;
							return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
						}
						catch (Throwable t) {
							exception[0] = t;
							return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
						}
					}
				};
			}
		}
		
		if (NotesJNAContext.is64Bit()) {
			b64_NSFNoteOpenCallback cNoteOpenCallback = null;
			b64_NSFObjectAllocCallback cObjectAllocCallback = null;
			b64_NSFObjectWriteCallback cObjectWriteCallback = null;
			
			if (noteOpenCallback!=null) {
				if (notesAPI instanceof WinNotesCAPI) {
					cNoteOpenCallback = new WinNotesCAPI.b64_NSFNoteOpenCallbackWin() {

						@Override
						public short invoke(Pointer param, long hNote, int noteId, short status) {
							NotesNote note = new NotesNote(NotesDatabase.this, hNote);
							note.setNoRecycle();
							
							try {
								NotesGC.__objectCreated(NotesNote.class, note);
								noteOpenCallback.noteOpened(note, noteId, status);
								return 0;
							}
							catch (RuntimeException e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
					};
				}
				else {
					cNoteOpenCallback = new b64_NSFNoteOpenCallback() {

						@Override
						public short invoke(Pointer param, long hNote, int noteId, short status) {
							NotesNote note = new NotesNote(NotesDatabase.this, hNote);
							note.setNoRecycle();

							try {
								NotesGC.__objectCreated(NotesNote.class, note);
								noteOpenCallback.noteOpened(note, noteId, status);
								return 0;
							}
							catch (RuntimeException e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
						
					};					
				}
			}
			
			if (objectAllocCallback!=null) {
				if (notesAPI instanceof WinNotesCAPI) {
					cObjectAllocCallback = new WinNotesCAPI.b64_NSFObjectAllocCallbackWin() {

						@Override
						public short invoke(Pointer param, long hNote, int oldRRV, short status, int objectSize) {
							NotesNote note = new NotesNote(NotesDatabase.this, hNote);
							note.setNoRecycle();

							try {
								NotesGC.__objectCreated(NotesNote.class, note);
								objectAllocCallback.objectAllocated(note, oldRRV, status, objectSize);
								return 0;
							}
							catch (RuntimeException e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
					};
				}
				else {
					cObjectAllocCallback = new b64_NSFObjectAllocCallback() {

						@Override
						public short invoke(Pointer param, long hNote, int oldRRV, short status, int objectSize) {
							NotesNote note = new NotesNote(NotesDatabase.this, hNote);
							note.setNoRecycle();

							try {
								NotesGC.__objectCreated(NotesNote.class, note);
								objectAllocCallback.objectAllocated(note, oldRRV, status, objectSize);
								return 0;
							}
							catch (RuntimeException e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
					};
				}
			}
			
			if (objectWriteCallback!=null) {
				if (notesAPI instanceof WinNotesCAPI) {
					cObjectWriteCallback = new WinNotesCAPI.b64_NSFObjectWriteCallbackWin() {

						@Override
						public short invoke(Pointer param, long hNote, int oldRRV, short status, Pointer buffer,
								int bufferSize) {
							NotesNote note = new NotesNote(NotesDatabase.this, hNote);
							note.setNoRecycle();

							ByteBuffer byteBuf = buffer.getByteBuffer(0, bufferSize);
							
							try {
								NotesGC.__objectCreated(NotesNote.class, note);
								objectWriteCallback.objectChunkWritten(note, oldRRV, status, byteBuf);
								return 0;
							}
							catch (RuntimeException e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
						
					};
				}
				else {
					cObjectWriteCallback = new b64_NSFObjectWriteCallback() {

						@Override
						public short invoke(Pointer param, long hNote, int oldRRV, short status, Pointer buffer,
								int bufferSize) {
							NotesNote note = new NotesNote(NotesDatabase.this, hNote);
							note.setNoRecycle();

							ByteBuffer byteBuf = buffer.getByteBuffer(0, bufferSize);
							
							try {
								NotesGC.__objectCreated(NotesNote.class, note);
								objectWriteCallback.objectChunkWritten(note, oldRRV, status, byteBuf);
								return 0;
							}
							catch (RuntimeException e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
						
					};
				}
			}
			
			short result = notesAPI.b64_NSFDbGetNotes(m_hDB64, noteIds.length, arrNoteIdsMem, arrNoteOpenFlagsMem, arrSinceSeqNumMem, GetNotes.toBitMask(controlFlags), objectDb==null ? 0 : objectDb.getHandle64(), null, cGetNotesCallback, cNoteOpenCallback, cObjectAllocCallback, cObjectWriteCallback, folderSinceTimeStruct, cFolderAddCallback);
			if (exception[0]!=null) {
				throw new NotesError(0, "Error reading notes", exception[0]);
			}
			NotesErrorUtils.checkResult(result);
		}
		else {
			b32_NSFNoteOpenCallback cNoteOpenCallback = null;
			b32_NSFObjectAllocCallback cObjectAllocCallback = null;
			b32_NSFObjectWriteCallback cObjectWriteCallback = null;
			
			if (noteOpenCallback!=null) {
				if (notesAPI instanceof WinNotesCAPI) {
					cNoteOpenCallback = new WinNotesCAPI.b32_NSFNoteOpenCallbackWin() {

						@Override
						public short invoke(Pointer param, int hNote, int noteId, short status) {
							NotesNote note = new NotesNote(NotesDatabase.this, hNote);
							note.setNoRecycle();

							try {
								NotesGC.__objectCreated(NotesNote.class, note);
								noteOpenCallback.noteOpened(note, noteId, status);
								return 0;
							}
							catch (RuntimeException e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
						
					};
				}
				else {
					cNoteOpenCallback = new b32_NSFNoteOpenCallback() {

						@Override
						public short invoke(Pointer param, int hNote, int noteId, short status) {
							NotesNote note = new NotesNote(NotesDatabase.this, hNote);
							note.setNoRecycle();

							try {
								NotesGC.__objectCreated(NotesNote.class, note);
								noteOpenCallback.noteOpened(note, noteId, status);
								return 0;
							}
							catch (RuntimeException e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
						
					};
				}
			}
			
			if (objectAllocCallback!=null) {
				if (notesAPI instanceof WinNotesCAPI) {
					cObjectAllocCallback = new WinNotesCAPI.b32_NSFObjectAllocCallbackWin() {

						@Override
						public short invoke(Pointer param, int hNote, int oldRRV, short status, int objectSize) {
							NotesNote note = new NotesNote(NotesDatabase.this, hNote);
							note.setNoRecycle();

							try {
								NotesGC.__objectCreated(NotesNote.class, note);
								objectAllocCallback.objectAllocated(note, oldRRV, status, objectSize);
								return 0;
							}
							catch (RuntimeException e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
					};
				}
				else {
					cObjectAllocCallback = new b32_NSFObjectAllocCallback() {

						@Override
						public short invoke(Pointer param, int hNote, int oldRRV, short status, int objectSize) {
							NotesNote note = new NotesNote(NotesDatabase.this, hNote);
							note.setNoRecycle();

							try {
								NotesGC.__objectCreated(NotesNote.class, note);
								objectAllocCallback.objectAllocated(note, oldRRV, status, objectSize);
								return 0;
							}
							catch (RuntimeException e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
					};
				}
			}
			
			if (objectWriteCallback!=null) {
				if (notesAPI instanceof WinNotesCAPI) {
					cObjectWriteCallback = new WinNotesCAPI.b32_NSFObjectWriteCallbackWin() {

						@Override
						public short invoke(Pointer param, int hNote, int oldRRV, short status, Pointer buffer,
								int bufferSize) {
							NotesNote note = new NotesNote(NotesDatabase.this, hNote);
							note.setNoRecycle();

							ByteBuffer byteBuf = buffer.getByteBuffer(0, bufferSize);
							
							try {
								NotesGC.__objectCreated(NotesNote.class, note);
								objectWriteCallback.objectChunkWritten(note, oldRRV, status, byteBuf);
								return 0;
							}
							catch (RuntimeException e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
					};
				}
				else {
					cObjectWriteCallback = new b32_NSFObjectWriteCallback() {

						@Override
						public short invoke(Pointer param, int hNote, int oldRRV, short status, Pointer buffer,
								int bufferSize) {
							NotesNote note = new NotesNote(NotesDatabase.this, hNote);
							note.setNoRecycle();

							ByteBuffer byteBuf = buffer.getByteBuffer(0, bufferSize);
							
							try {
								NotesGC.__objectCreated(NotesNote.class, note);
								objectWriteCallback.objectChunkWritten(note, oldRRV, status, byteBuf);
								return 0;
							}
							catch (RuntimeException e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_NSF_COMPUTE_ECL_ABORT;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
					};
				}
			}
			
			short result = notesAPI.b32_NSFDbGetNotes(m_hDB32, noteIds.length, arrNoteIdsMem, arrNoteOpenFlagsMem, arrSinceSeqNumMem, GetNotes.toBitMask(controlFlags), objectDb==null ? 0 : objectDb.getHandle32(), null, cGetNotesCallback, cNoteOpenCallback, cObjectAllocCallback, cObjectWriteCallback, folderSinceTimeStruct, cFolderAddCallback);
			if (exception[0]!=null) {
				throw new NotesError(0, "Error reading notes", exception[0]);
			}
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function generates a new Originator ID (OID) used to uniquely identify a note.<br>
	 * <br>
	 * Use this function when you already have a note open and wish to create a totally new note
	 * with the same items as the open note.<br>
	 * This function is commonly used after NSFNoteCopy, because the copy created by NSFNoteCopy
	 * has the same OID as the source note.<br>
	 * <br>
	 * You do not need this method when creating a new note from scratch using {@link #createNote()},
	 * because the internally used NSFNoteCreate performs this function for you.<br>
	 * <br>
	 * If the database resides on a remote Lotus Domino Server, the current user must to have
	 * the appropriate level of access to carry out this operation.
	 * 
	 * @return new OID
	 */
	public NotesOriginatorId generateOID() {
		checkHandle();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		NotesOriginatorIdStruct retOIDStruct = NotesOriginatorIdStruct.newInstance();
		
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFDbGenerateOID(m_hDB64, retOIDStruct);
		}
		else {
			result = notesAPI.b32_NSFDbGenerateOID(m_hDB32, retOIDStruct);
		}
		NotesErrorUtils.checkResult(result);

		retOIDStruct.read();
		
		return new NotesOriginatorId(retOIDStruct);
	}
}
