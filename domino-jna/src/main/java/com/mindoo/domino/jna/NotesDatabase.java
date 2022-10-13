package com.mindoo.domino.jna;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.mindoo.domino.jna.NotesReplicationHistorySummary.ReplicationDirection;
import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.constants.AclFlag;
import com.mindoo.domino.jna.constants.AclLevel;
import com.mindoo.domino.jna.constants.CopyDatabase;
import com.mindoo.domino.jna.constants.CreateDatabase;
import com.mindoo.domino.jna.constants.DBClass;
import com.mindoo.domino.jna.constants.DBCompact;
import com.mindoo.domino.jna.constants.DBCompact2;
import com.mindoo.domino.jna.constants.DBQuery;
import com.mindoo.domino.jna.constants.DatabaseOption;
import com.mindoo.domino.jna.constants.FTIndex;
import com.mindoo.domino.jna.constants.FTSearch;
import com.mindoo.domino.jna.constants.FileType;
import com.mindoo.domino.jna.constants.GetNotes;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.OpenCollection;
import com.mindoo.domino.jna.constants.OpenDatabase;
import com.mindoo.domino.jna.constants.OpenNote;
import com.mindoo.domino.jna.constants.ReplicateOption;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.constants.UpdateNote;
import com.mindoo.domino.jna.design.NotesForm;
import com.mindoo.domino.jna.directory.DirectoryScanner;
import com.mindoo.domino.jna.directory.DirectoryScanner.DatabaseData;
import com.mindoo.domino.jna.directory.DirectoryScanner.SearchResultData;
import com.mindoo.domino.jna.dql.DQL;
import com.mindoo.domino.jna.dql.DQL.DQLTerm;
import com.mindoo.domino.jna.dxl.DXLImporter;
import com.mindoo.domino.jna.dxl.DXLImporter.DXLImportOption;
import com.mindoo.domino.jna.errors.FormulaCompilationError;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.formula.FormulaExecution;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.FTSearchResultsDecoder;
import com.mindoo.domino.jna.internal.INotesNativeAPI32;
import com.mindoo.domino.jna.internal.INotesNativeAPI64;
import com.mindoo.domino.jna.internal.Mem;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesCallbacks;
import com.mindoo.domino.jna.internal.NotesCallbacks.ABORTCHECKPROC;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI32V1000;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.NotesNativeAPI64V1000;
import com.mindoo.domino.jna.internal.RecycleHierarchy;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks.ABORTCHECKPROCWin32;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.DHANDLE32;
import com.mindoo.domino.jna.internal.handles.DHANDLE64;
import com.mindoo.domino.jna.internal.handles.HANDLE;
import com.mindoo.domino.jna.internal.handles.HANDLE32;
import com.mindoo.domino.jna.internal.handles.HANDLE64;
import com.mindoo.domino.jna.internal.structs.NotesBuildVersionStruct;
import com.mindoo.domino.jna.internal.structs.NotesDbReplicaInfoStruct;
import com.mindoo.domino.jna.internal.structs.NotesFTIndexStatsStruct;
import com.mindoo.domino.jna.internal.structs.NotesItemDefinitionTableExt;
import com.mindoo.domino.jna.internal.structs.NotesItemDefinitionTableLock;
import com.mindoo.domino.jna.internal.structs.NotesOriginatorIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesReplicationHistorySummaryStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.internal.structs.ReplExtensionsStruct;
import com.mindoo.domino.jna.internal.structs.ReplServStatsStruct;
import com.mindoo.domino.jna.transactions.ITransactionCallable;
import com.mindoo.domino.jna.transactions.Transactions;
import com.mindoo.domino.jna.utils.ExceptionUtil;
import com.mindoo.domino.jna.utils.IDUtils;
import com.mindoo.domino.jna.utils.LegacyAPIUtils;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesIniUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils.Privileges;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.Pair;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.Ref;
import com.mindoo.domino.jna.utils.SetUtil;
import com.mindoo.domino.jna.utils.SignalHandlerUtil;
import com.mindoo.domino.jna.utils.SignalHandlerUtil.IBreakHandler;
import com.mindoo.domino.jna.utils.StringTokenizerExt;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

import lotus.domino.Database;
import lotus.domino.NotesException;
import lotus.domino.Session;

/**
 * Object wrapping a Notes database
 * 
 * @author Karsten Lehmann
 */
public class NotesDatabase implements IRecyclableNotesObject, IAdaptable {
	static final String NAMEDNOTES_APPLICATION_PREFIX = "$app_";
	
	private int m_hDB32;
	private long m_hDB64;
	private boolean m_noRecycleDb;
	private String m_asUserCanonical;
	private String m_server;
	private String[] m_paths;
	private String m_replicaID;
	private boolean m_loginAsIdOwner;
	
	NotesNamesList m_namesList;
	private List<String> m_namesStringList;
	private EnumSet<Privileges> m_namesListPrivileges;
	
	private Database m_legacyDbRef;
	private Integer m_openDatabaseId;
	private NotesACL m_acl;
	boolean m_passNamesListToDbOpen;
	private boolean m_passNamesListToViewOpen;
	private DbMode m_dbMode;
	
	private final RecycleHierarchy m_recycleHierarchy = new RecycleHierarchy();
	
	/**
	 * Opens a database either as server or on behalf of a specified user
	 * 
	 * @param session session to extract the effective username to be used to open the database
	 * @param server database server
	 * @param filePath database filepath
	 */
	public NotesDatabase(Session session, String server, String filePath) {
		this(server, filePath, getEffectiveUserName(session));
	}

	/**
	 * Opens a database either as server or on behalf of a specified user
	 * 
	 * @param session session to extract the effective username to be used to open the database
	 * @param server database server
	 * @param filePath database filepath
	 * @param openFlags flags to specify how to open the database
	 */
	public NotesDatabase(Session session, String server, String filePath, EnumSet<OpenDatabase> openFlags) {
		this(server, filePath, (List<String>) null, getEffectiveUserName(session), openFlags);
	}

	/**
	 * Opens a database either as server or on behalf of a specified user
	 * 
	 * @param server database server
	 * @param filePath database filepath
	 * @param asUserCanonical user context to open database or null/empty string to open as ID owner (e.g. server when running on the server); will be ignored if code is run locally in the Notes Client
	 * @param openFlags flags to specify how to open the database
	 */
	public NotesDatabase(String server, String filePath, String asUserCanonical, EnumSet<OpenDatabase> openFlags) {
		this(server, filePath, (List<String>) null, asUserCanonical, openFlags);
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (RecycleHierarchy.class.equals(clazz)) {
			return (T) m_recycleHierarchy;
		}
		
		return null;
	}
	
	/**
	 * Checks if a database exists
	 * 
	 * @param server database server
	 * @param filePath database filepath
	 * @return true if DB exists
	 */
	public static boolean exists(String server, String filePath) {
		boolean isOnServer = IDUtils.isOnServer();
		
		String idUserName = IDUtils.getIdUsername();
		
		if (!"".equals(server)) {
			if (isOnServer) {
				String serverCN = NotesNamingUtils.toCommonName(server);
				String currServerCN = NotesNamingUtils.toCommonName(idUserName);
				if (serverCN.equalsIgnoreCase(currServerCN)) {
					//switch to "" as servername if server points to the server the API is running on
					server = "";
				}
			}
		}

		Memory retFullNetPath = constructNetPath(server, filePath);

		NotesTimeDateStruct retDataModified = NotesTimeDateStruct.newInstance();
		NotesTimeDateStruct retNonDataModified = NotesTimeDateStruct.newInstance();
		
		short result = NotesNativeAPI.get().NSFDbModifiedTimeByName(retFullNetPath, retDataModified, retNonDataModified);
		if (result == 259) { // File does not exist
			return false;
		}
		NotesErrorUtils.checkResult(result);
		
		return true;
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
			throw new NotesError(e.id, e.getLocalizedMessage());
		}
	}

	/**
	 * Opens a database either as server or on behalf of a specified user
	 * 
	 * @param server database server
	 * @param filePath database filepath
	 * @param asUserCanonical user context to open database or null/empty string to open as ID owner (e.g. server when running on the server); will be ignored if code is run locally in the Notes Client
	 */
	public NotesDatabase(String server, String filePath, String asUserCanonical) {
		this(server, filePath, (List<String>) null, asUserCanonical);
	}
	
	/**
	 * Opens a database either as server or on behalf of a specified user
	 * 
	 * @param server database server
	 * @param filePath database filepath
	 * @param namesForNamesList optional names list for the user to open the database; same content as @Usernameslist, but can be any combination of names, groups or roles (does not have to exist in the directory)
	 */
	public NotesDatabase(String server, String filePath, List<String> namesForNamesList) {
		this(server, filePath, namesForNamesList, null);
	}

	/**
	 * Opens a database either as server or on behalf of a specified user
	 * 
	 * @param server database server
	 * @param filePath database filepath
	 * @param namesForNamesList optional names list
	 * @param asUserCanonical user context to open database or null/empty string to open as ID owner (e.g. server when running on the server); will be ignored if code is run locally in the Notes Client
	 */
	private NotesDatabase(String server, String filePath, List<String> namesForNamesList, String asUserCanonical) {
		this(server, filePath, namesForNamesList, asUserCanonical, (EnumSet<OpenDatabase>) null);
	}
	
	private NotesDatabase(long handle, String asUserCanonical, NotesNamesList namesList) {
		if (!PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		
		m_hDB64 = handle;
		m_asUserCanonical = asUserCanonical;
		
		m_namesList = namesList;
		m_namesStringList = m_namesList.getNames();
		m_namesListPrivileges = NotesNamingUtils.getPrivileges(namesList);
	}

	private NotesDatabase(int handle, String asUserCanonical, NotesNamesList namesList) {
		if (PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		
		m_hDB32 = handle;
		m_asUserCanonical = asUserCanonical;
		
		m_namesList = namesList;
		m_namesStringList = m_namesList.getNames();
		m_namesListPrivileges = NotesNamingUtils.getPrivileges(namesList);
	}

	/**
	 * Opens a database either as server or on behalf of a specified user
	 * 
	 * @param server database server
	 * @param filePath database filepath
	 * @param namesForNamesList optional names list
	 * @param asUserCanonical user context to open database or null/empty string to open as ID owner (e.g. server when running on the server); will be ignored if code is run locally in the Notes Client
	 * @param openFlags flags to specify how to open the database
	 */
	private NotesDatabase(String server, String filePath, List<String> namesForNamesList, String asUserCanonical, EnumSet<OpenDatabase> openFlags) {
		String idUserName = IDUtils.getIdUsername();
		if (StringUtil.isEmpty(asUserCanonical)) {
			asUserCanonical = idUserName;
		}
		//make sure server and username are in canonical format
		m_asUserCanonical = NotesNamingUtils.toCanonicalName(asUserCanonical);
		
		if (server==null)
			server = "";
		if (filePath==null)
			throw new NullPointerException("filePath is null");

		if (NotesGC.isFixupLocalServerNames() && StringUtil.isEmpty(server) && IDUtils.isOnServer()) {
			//switch to full server name, prevents "database is in use" errors when running side-by-side with Domino
			server = IDUtils.getIdUsername();
		}
		else {
			server = NotesNamingUtils.toCanonicalName(server);
		}
		
		boolean isOnServer = IDUtils.isOnServer();
		
		if (namesForNamesList==null && (StringUtil.isEmpty(m_asUserCanonical) || (m_asUserCanonical!=null && NotesNamingUtils.equalNames(m_asUserCanonical, idUserName)))) {
			m_loginAsIdOwner = true;
		}
		else {
			m_loginAsIdOwner = false;
		}

		Memory retFullNetPath = constructNetPath(server, filePath);
		short result;
		
		short openOptions = openFlags==null ? 0 : OpenDatabase.toBitMaskForOpen(openFlags);

		if (namesForNamesList==null) {
			if (m_asUserCanonical==null) {
				m_asUserCanonical = IDUtils.getIdUsername();
			}
		}
		
		//first build usernames list
		if (namesForNamesList!=null) {
			m_namesList = NotesNamingUtils.writeNewNamesList(namesForNamesList);
		}
		else {
			m_namesList = NotesNamingUtils.buildNamesList(server, m_asUserCanonical);
		}
		m_namesStringList = m_namesList.getNames();
		
		//setting authenticated flag for the user is required when running on the server
		if (openFlags!=null && openFlags.contains(OpenDatabase.FULL_ACCESS)) {
			NotesNamingUtils.setPrivileges(m_namesList, EnumSet.of(Privileges.FullAdminAccess,
					Privileges.Authenticated));
		}
		else {
			NotesNamingUtils.setPrivileges(m_namesList, EnumSet.of(Privileges.Authenticated));
		}
		
		m_namesListPrivileges = NotesNamingUtils.getPrivileges(m_namesList);

		m_passNamesListToViewOpen = false;
		m_passNamesListToDbOpen = false;
		if (m_namesList!=null) {
			if (!m_loginAsIdOwner) {
				//if we should be opening the DB as another user, we need to pass the names ist;
				//this might produce ERR 582: You are not listed as a trusted server
				m_passNamesListToDbOpen = true;
				m_passNamesListToViewOpen = true;
			}
		}
		
		if (PlatformUtils.is64Bit()) {
			LongByReference hDB = new LongByReference();

			//now try to open the database as this user
			NotesTimeDateStruct modifiedTime = null;
			NotesTimeDateStruct retDataModified = NotesTimeDateStruct.newInstance();
			NotesTimeDateStruct retNonDataModified = NotesTimeDateStruct.newInstance();
			
			int retries = 5;
			do {
				//try opening the database multiple times; we had issues here when opening
				//many dbs remotely that could be solved by retrying
				if (m_passNamesListToDbOpen) {
					result = NotesNativeAPI64.get().NSFDbOpenExtended(retFullNetPath, openOptions, m_namesList.getHandle64(),
							modifiedTime, hDB, retDataModified, retNonDataModified);
					
					if (result==582 && StringUtil.isEmpty(server) && !isOnServer && m_loginAsIdOwner) {
						result = NotesNativeAPI64.get().NSFDbOpenExtended(retFullNetPath, openOptions, 0, modifiedTime, hDB,
								retDataModified, retNonDataModified);
					}
				}
				else {
					result = NotesNativeAPI64.get().NSFDbOpenExtended(retFullNetPath, openOptions, 0, modifiedTime, hDB,
							retDataModified, retNonDataModified);
				}
			
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

			if ((result & NotesConstants.ERR_MASK) == 259) { // File does not exist
				//try to find this database in the folder configured via SharedDataDirectory
				//in the Notes.ini

				if (m_passNamesListToDbOpen) {
					result = NotesNativeAPI64.get().NSFDbOpenTemplateExtended(retFullNetPath, openOptions,
							m_namesList.getHandle64(),
							null, hDB, retDataModified, retNonDataModified);
				}
				else {
					result = NotesNativeAPI64.get().NSFDbOpenTemplateExtended(retFullNetPath, openOptions,
							0,
							null, hDB, retDataModified, retNonDataModified);
				}
			}
			
			if ((result & NotesConstants.ERR_MASK) == 259) {
				throw new NotesError(result, "No database found on server '"+server+"' with filepath "+filePath);
			}
			
			NotesErrorUtils.checkResult(result);

			m_hDB64 = hDB.getValue();
		}
		else {
			IntByReference hDB = new IntByReference();

			//now try to open the database as this user
			NotesTimeDateStruct modifiedTime = null;
			NotesTimeDateStruct retDataModified = NotesTimeDateStruct.newInstance();
			NotesTimeDateStruct retNonDataModified = NotesTimeDateStruct.newInstance();

			int retries = 5;
			do {
				//try opening the database multiple times; we had issues here when opening
				//many dbs remotely that could be solved by retrying
				if (m_passNamesListToDbOpen) {
					result = NotesNativeAPI32.get().NSFDbOpenExtended(retFullNetPath, openOptions, m_namesList.getHandle32(), modifiedTime, hDB, retDataModified, retNonDataModified);
					
					if (result==582 && StringUtil.isEmpty(server) && !isOnServer && m_loginAsIdOwner) {
						result = NotesNativeAPI32.get().NSFDbOpenExtended(retFullNetPath, openOptions, 0, modifiedTime, hDB, retDataModified, retNonDataModified);
					}
				}
				else {
					result = NotesNativeAPI32.get().NSFDbOpenExtended(retFullNetPath, openOptions, 0, modifiedTime, hDB, retDataModified, retNonDataModified);
				}
			
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

			if ((result & NotesConstants.ERR_MASK) == 259) { // File does not exist
				//try to find this database in the folder configured via SharedDataDirectory
				//in the Notes.ini
				
				if (m_passNamesListToDbOpen) {
					result = NotesNativeAPI32.get().NSFDbOpenTemplateExtended(retFullNetPath, openOptions,
							m_namesList.getHandle32(),
							null, hDB, retDataModified, retNonDataModified);
				}
				else {
					result = NotesNativeAPI32.get().NSFDbOpenTemplateExtended(retFullNetPath, openOptions,
							0,
							null, hDB, retDataModified, retNonDataModified);
				}
			}
			
			if ((result & NotesConstants.ERR_MASK) == 259) {
				throw new NotesError(result, "No database found on server '"+server+"' with filepath "+filePath);
			}

			NotesErrorUtils.checkResult(result);

			m_hDB32 = hDB.getValue();
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
			
			if (PlatformUtils.is64Bit()) {
				m_hDB64 = dbHandle;
			}
			else {
				m_hDB32 = (int) dbHandle;
			}
			NotesGC.__objectCreated(NotesDatabase.class, this, true); //true -> do not check for duplicate handle since it's a shared handle
			setNoRecycleDb();
			m_legacyDbRef = legacyDB;

			Session session;
			try {
				session = legacyDB.getParent();
				String effUserName = session.getEffectiveUserName();
				m_asUserCanonical = effUserName;
				
				List<String> names = NotesNamingUtils.getUserNamesList(legacyDB.getServer(), m_asUserCanonical);
				m_namesList = NotesNamingUtils.writeNewNamesList(names);

				//setting authenticated flag for the user is required when running on the server
				NotesNamingUtils.setPrivileges(m_namesList, EnumSet.of(Privileges.Authenticated));

				m_namesListPrivileges = NotesNamingUtils.getPrivileges(m_namesList);
				
			} catch (NotesException e) {
				throw new NotesError(e.id, e.getLocalizedMessage());
			}
		}
		else {
			throw new NotesError(0, "Unsupported adaptable parameter");
		}
	}

	/**
	 * Convenience method to look up the roles for a user from the database {@link NotesACL}.<br>
	 * Use {@link NotesACL#lookupAccess(NotesNamesList)} if you need other access information
	 * for the same user as well.
	 * 
	 * @param userName username, either canonical or abbreviated
	 * @return list of roles, not null
	 */
	public List<String> queryAccessRoles(String userName) {
		NotesNamesList namesList = null;
		try {
			namesList = NotesNamingUtils.buildNamesList(getServer(), userName);
			List<String> roles = getACL().lookupAccess(namesList).getRoles();
			return roles;
		}
		catch (NotesError e) {
			throw new NotesError(e.getId(), "Error computing roles for "+userName+" on server \""+m_server+"\"", e);
		}
		finally {
			if (namesList!=null) {
				namesList.free();
			}
		}
	}
	
	/**
	 * Convenience method to look up the access level for a user from the database {@link NotesACL}<br>
	 * Use {@link NotesACL#lookupAccess(NotesNamesList)} if you need other access information
	 * for the same user as well.
	 * 
	 * @param userName username, either canonical or abbreviated
	 * @return access level
	 */
	public AclLevel queryAccess(String userName) {
		NotesNamesList namesList = NotesNamingUtils.buildNamesList(getServer(), userName);
		try {
			AclLevel level = getACL().lookupAccess(namesList).getAclLevel();
			return level;
		}
		finally {
			namesList.free();
		}
	}
	
	/**
	 * Convenience method to look up the access flags for a user from the database {@link NotesACL}<br>
	 * Use {@link NotesACL#lookupAccess(NotesNamesList)} if you need other access information
	 * for the same user as well.
	 * 
	 * @param userName username, either canonical or abbreviated
	 * @return flags
	 */
	public EnumSet<AclFlag> queryAccessFlags(String userName) {
		NotesNamesList namesList = NotesNamingUtils.buildNamesList(getServer(), userName);
		try {
			EnumSet<AclFlag> flags = getACL().lookupAccess(namesList).getAclFlags();
			return flags;
		}
		finally {
			namesList.free();
		}
	}
	
	/**
	 * This function reads the access control list of the database.<br>
	 * If you modify this copy of the access control list, use {link {@link NotesACL#save()}}
	 * to store it in the database.
	 * 
	 * @return acl
	 */
	public NotesACL getACL() {
		checkHandle();
		HANDLE hDb = getHandle();
		
		if (m_acl==null || m_acl.isFreed()) {
			DHANDLE.ByReference rethACL = DHANDLE.newInstanceByReference();
			short result = NotesNativeAPI.get().NSFDbReadACL(hDb.getByValue(), rethACL);
			NotesErrorUtils.checkResult(result);
			m_acl = new NotesACL(this, rethACL);
			NotesGC.__memoryAllocated(m_acl);
		}
		return m_acl;
	}

	/**
	 * Locate and return name of template for a given template name.
	 * 
	 * @param templateName name of template to find
	 * @return path to DB which is the template for this template name or empty string if not found
	 */
	public static String findDatabaseByTemplateName(String templateName) {
		return findDatabaseByTemplateName(templateName, (String) null);
	}
	
	/**
	 * Locate and return name of template for a given template name.
	 * 
	 * @param templateName name of template to find
	 * @param excludeDbPath optional db path to exclude during search
	 * @return path to DB which is the template for this template name or empty string if not found
	 */
	public static String findDatabaseByTemplateName(String templateName, String excludeDbPath) {
		Memory templateNameMem = NotesStringUtils.toLMBCS(templateName, true);
		Memory excludeDbPathMem = NotesStringUtils.toLMBCS(excludeDbPath==null ? "" : excludeDbPath, true);
		DisposableMemory retDbPath = new DisposableMemory(256);
		try {
			short result = NotesNativeAPI.get().DesignFindTemplate(templateNameMem,
					excludeDbPathMem, retDbPath);
			if ((result & NotesConstants.ERR_MASK)==1028) //entry not found in index
				return "";
			NotesErrorUtils.checkResult(result);
			
			return NotesStringUtils.fromLMBCS(retDbPath, -1);
		}
		finally {
			retDbPath.dispose();
		}
	}
	
	/**
	 * Convenience function that uses the {@link DirectoryScanner} to find all databases
	 * with the specified template name
	 * 
	 * @param serverName server to scan
	 * @param directory top directory for search
	 * @param templateName template name
	 * @return list of filepaths
	 */
	public static List<String> findAllDatabasesByTemplateName(String serverName, String directory,
			String templateName) {
		
		//make sure the template name does not contain invalid characters
		templateName = templateName.replace("\"", "");
		String templateNameLC = templateName.toLowerCase(Locale.ENGLISH);
		
//		$Info=Database title
//				#2OpenEclipseUpdateSite
				
		DirectoryScanner scanner = new DirectoryScanner(serverName, directory, EnumSet.of(FileType.ANY));
		//use formula to decode this crazy format
		String formula = "@contains(@LowerCase($info);\"#1"+templateNameLC+"\"+@Char(10)) |" +
				"@contains(@LowerCase($info);\"#1"+templateNameLC+"\"+@Char(13)) |" +
				" @Ends(@LowerCase($info);\"#1" + templateNameLC + "\")";
		
		List<SearchResultData> results = scanner.scan(formula);

		List<String> filePaths = results
				.stream()
				.map((data) -> {
					if (data instanceof DatabaseData) {
						return ((DatabaseData)data).getFilePath();
					}
					else {
						return "";
					}
				})
				.filter((filePath) -> {
					return !StringUtil.isEmpty(filePath);
				})
				.collect(Collectors.toList());
		
		return filePaths;
	}

	/**
	 * Searches for a database by its replica id in the data directory (and subdirectories) specified by this
	 * scanner instance. The method only uses the server specified for this scanner instance, not the directory.
	 * It always searches the whole directory.
	 * 
	 * @param server server to search db replica
	 * @param replicaId replica id to search for
	 * @return path to database matching this id or empty string if not found
	 */
	public static String findDatabaseByReplicaId(String server, String replicaId) {
		NotesDatabase dir = new NotesDatabase(server, "", "");
		try {
			int[] innards = NotesStringUtils.replicaIdToInnards(replicaId);
			NotesTimeDateStruct replicaIdStruct = NotesTimeDateStruct.newInstance(innards);

			Memory retPathNameMem = new Memory(NotesConstants.MAXPATH);
			short result;
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().NSFDbLocateByReplicaID(dir.getHandle64(), replicaIdStruct, retPathNameMem, (short) (NotesConstants.MAXPATH & 0xffff));
			}
			else {
				result = NotesNativeAPI32.get().NSFDbLocateByReplicaID(dir.getHandle32(), replicaIdStruct, retPathNameMem, (short) (NotesConstants.MAXPATH & 0xffff));
			}
			if (result == 259) // File does not exist
				return "";
			
			NotesErrorUtils.checkResult(result);

			String retPathName = NotesStringUtils.fromLMBCS(retPathNameMem, -1);
			if (retPathName==null || retPathName.length()==0) {
				return "";
			}
			else {
				return retPathName;
			}
		}
		finally {
			dir.recycle();
		}
	}

	/**
	 * Available encryption strengths for database creation
	 */
	public enum Encryption {
		None(NotesConstants.DBCREATE_ENCRYPT_NONE),
		Simple(NotesConstants.DBCREATE_ENCRYPT_SIMPLE),
		Medium(NotesConstants.DBCREATE_ENCRYPT_MEDIUM),
		Strong(NotesConstants.DBCREATE_ENCRYPT_STRONG),
		/** added in 12.0.1 */
		AES128(NotesConstants.DBCREATE_ENCRYPT_AES128),
		/** added in 12.0.2 */
		AES256(NotesConstants.DBCREATE_ENCRYPT_AES256);

		private final int value;

		Encryption(final int value) {
			this.value = value;
		}

		Encryption(final byte value) {
			this.value = Byte.toUnsignedInt(value);
		}

		public int getValue() {
			return value;
		}
		
		public static Encryption valueOf(int val) {
			for (Encryption currEnc : values()) {
				if (val == currEnc.getValue()) {
					return currEnc;
				}
			}
			throw new IllegalArgumentException(MessageFormat.format("Unknown encryption value: {0}", val));
		}
	};

	/**
	 * Encryption states
	 */
	public enum EncryptionState {
		UNENCRYPTED(0), ENCRYPTED(1), PENDING_ENCRYPTION(2), PENDING_DECRYPTION(3);

		private final int value;

		EncryptionState(final int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
		
		public static EncryptionState valueOf(int val) {
			for (EncryptionState currState : values()) {
				if (val == currState.getValue()) {
					return currState;
				}
			}
			throw new IllegalArgumentException(MessageFormat.format("Unknown state value: {0}", val));
		}
	}

	/**
	 * Convenience method that calls {@link #createDatabase(String, String, DBClass, boolean, EnumSet, Encryption, long, String, AclLevel, String, boolean)}
	 * with some defaults, e.g. large UNK table, "Optimize document table map", no quota.
	 * 
	 * @param serverName server name, either canonical, abbreviated or common name
	 * @param filePath filepath to database
	 * @param encryption encryption strength
	 * @param dbTitle title
	 * @param defaultAccessLevel access level for the "-Default-" ACL entry
	 * @param manager user to add to the ACL with manager access, if empty/null, wen use the user returned by {@link IDUtils#getIdUsername()}
	 * @param initDbDesign true to add a view, add the icon note and create the design collection so that the DB can be opened in the Notes Client; if false, you need to do this on your own, e.g. by importing the design as DXL
	 */
	public static void createDatabase(String serverName, String filePath, Encryption encryption, String dbTitle,
			AclLevel defaultAccessLevel, String manager,
			boolean initDbDesign) {
		
		createDatabase(serverName, filePath, DBClass.BY_EXTENSION, false,
				EnumSet.noneOf(CreateDatabase.class), encryption,
				0, dbTitle, defaultAccessLevel, manager, initDbDesign);
	}
	
	/**
	 * Creates a new database and initializes the ACL with default access level and an entry
	 * with manager rights. The created database is not fully initialized unless you set
	 * <code>initDbDesign</code> to true. This triggers a DXL import which creates a view,
	 * adds the default database icon and creates the design collection. If you plan to run
	 * your own DXL import with the DB design, you might want to set <code>initDbDesign</code>
	 * to false.
	 * 
	 * @param serverName server name, either canonical, abbreviated or common name
	 * @param filePath filepath to database
	 * @param dbClass specifies the class of the database created. See {@link DBClass} for classes that may be specified.
	 * @param forceCreation controls whether the call will overwrite an existing database of the same name. Set to TRUE to overwrite, set to FALSE not to overwrite.
	 * @param options database creation option flags.  See {@link CreateDatabase}
	 * @param encryption encryption strength
	 * @param maxFileSize optional.  Maximum file size of the database, in bytes.  In order to specify a maximum file size, use the database class, {@link DBClass#BY_EXTENSION} and use the option, {@link CreateDatabase#MAX_SPECIFIED}.
	 * @param dbTitle title
	 * @param defaultAccessLevel access level for the "-Default-" ACL entry
	 * @param manager user to add to the ACL with manager access, if empty/null, wen use the user returned by {@link IDUtils#getIdUsername()}
	 * @param initDbDesign true to add a view, add the icon note and create the design collection so that the DB can be opened in the Notes Client; if false, you need to do this on your own, e.g. by importing the design as DXL
	 */
	public static void createDatabase(String serverName, String filePath, DBClass dbClass, boolean forceCreation,
			EnumSet<CreateDatabase> options, Encryption encryption, long maxFileSize,
			String dbTitle,
			AclLevel defaultAccessLevel, String manager,
			boolean initDbDesign) {

		String fullPathTarget = NotesStringUtils.osPathNetConstruct(null, serverName, filePath);
		Memory fullPathTargetMem = NotesStringUtils.toLMBCS(fullPathTarget, true);

		byte encryptStrengthByte = (byte) (encryption.getValue() & 0xff);

		short dbClassShort = dbClass.getValue();
		short optionsShort = CreateDatabase.toBitMask(options);
		if (encryptStrengthByte!=0) {
			optionsShort |= NotesConstants.DBCREATE_LOCALSECURITY;
		}
		
		short result = NotesNativeAPI.get().NSFDbCreateExtended(fullPathTargetMem, dbClassShort, forceCreation, optionsShort, encryptStrengthByte, maxFileSize);
		NotesErrorUtils.checkResult(result);

		NotesDatabase db = new NotesDatabase(serverName, filePath, "");
		
		//create ACL
		
		DHANDLE.ByReference rethACL = DHANDLE.newInstanceByReference();
		result = NotesNativeAPI.get().ACLCreate(rethACL);
		NotesErrorUtils.checkResult(result);

		result = NotesNativeAPI.get().NSFDbStoreACL(db.getHandle().getByValue(), rethACL.getByValue(), 0, (short) 1);
		NotesErrorUtils.checkResult(result);

		if (initDbDesign) {
			InputStream in = null;
			
			try {
				//use DXL importer to create basic structures like the icon note
				//and the design collection
				in = NotesNativeAPI.class.getResourceAsStream("blank_dxl.xml");
				if (in==null) {
					throw new NotesError("File blank_dxl.xml not found");
				}
				DXLImporter importer = new DXLImporter();
				importer.setDesignImportOption(DXLImportOption.REPLACE_ELSE_CREATE);
				importer.setReplaceDbProperties(true);
				importer.importDxl(in, db);
				importer.free();
			} catch (IOException e) {
				throw new NotesError(0, "Error initializing new database design", e);
			}
			finally {
				if (in!=null) {
					try {
						in.close();
					} catch (IOException e) {
						//should not happen
						e.printStackTrace();
					}
				}
			}
		}
		
		db.setTitle(dbTitle);
		
		//write default acl entries, might lock us out
		NotesACL acl = db.getACL();

		acl.updateEntry("-Default-", "-Default-", defaultAccessLevel,  Collections.emptyList(), EnumSet.noneOf(AclFlag.class));
		acl.updateEntry("OtherDomainServers", null, AclLevel.NOACCESS, Collections.emptyList(), EnumSet.of(AclFlag.GROUP, AclFlag.SERVER));
		acl.updateEntry(manager, null, AclLevel.MANAGER, Collections.emptyList(), EnumSet.noneOf(AclFlag.class));

		if (db.isRemote()) {
			acl.updateEntry(db.getServer(), null, AclLevel.MANAGER, Collections.emptyList(), EnumSet.of(AclFlag.SERVER, AclFlag.ADMIN_SERVER));
			acl.setAdminServer(db.getServer());
		}

		acl.updateEntry("LocalDomainServers", null, AclLevel.MANAGER, Collections.emptyList(), EnumSet.of(AclFlag.GROUP, AclFlag.SERVER));

		acl.save();

		db.recycle();
	}

	/**
	 * Calls {@link #createAndCopyDatabase(String, String, String, String, EnumSet, long, Set, NotesNamesList)}
	 * with parameters/flags to create a replica copy for a database.
	 * 
	 * @param sourceDbServer server of database to copy
	 * @param sourceDbFilePath filepath of database to copy
	 * @param targetDbServerName server name of new database to be created
	 * @param targetDbFilePath filepath of new database to be created
	 * @return replica DB
	 */
	public static NotesDatabase createDbReplica(String sourceDbServer, String sourceDbFilePath,
			String targetDbServerName, String targetDbFilePath) {
		return createAndCopyDatabase(sourceDbServer, sourceDbFilePath, targetDbServerName, targetDbFilePath, null, 0, EnumSet.of(
				CopyDatabase.REPLICA,
				CopyDatabase.REPLICA_NAMELIST
				), null);
	}
	
	/**
	 * This convenience methods calls
	 * {@link #createAndCopyDatabase(String, String, String, String, EnumSet, long, Set, NotesNamesList)}
	 * with parameters to create a new database from the specified template database.<br>
	 * <br>
	 * It copies all data/design notes, creates an ACL derived from the template DB acl (e.g. "[Group1]"
	 * entry becomes a "Group1" entry) and sets the inherited template name to the
	 * template name of the template DB.<br>
	 * <br>
	 * You can use {@link #findDatabaseByTemplateName(String)} or {@link #findAllDatabasesByTemplateName(String, String, String)}
	 * to find the template database filepath for a given template name.
	 * 
	 * @param templateDbServer server of template database
	 * @param templateDbFilePath filepath of template database
	 * @param serverName server name of new database to be created
	 * @param filePath filepath of new database to be created
	 * @return new database
	 */
	public static NotesDatabase createDatabaseFromTemplate(String templateDbServer, String templateDbFilePath,
			String serverName, String filePath) {
		NotesDatabase newDb = createAndCopyDatabase(templateDbServer, templateDbFilePath, serverName, filePath, null, 0,
				(Set<CopyDatabase>) null, null);
		return newDb;
	}
	
	/**
	 * <b>Please note:<br>
	 * There are two convenience functions {@link #createDbReplica(String, String, String, String)}
	 * and {@link #createDatabaseFromTemplate(String, String, String, String)} that both use this powerful
	 * copy function. Both are much easier to use than this method, so it's recommended to
	 * use them instead of this one.</b><br>
	 * <br>
	 * This function creates a new copy of a Domino database based on the one supplied in <code>templateDb</code>
	 * and allows for a {@link NotesNamesList} structure UserName to provide authentication for trusted servers.<br>
	 * <br>
	 * The database class of the new database is based on the file extension specified by <code>templateDb</code>.<br>
	 * <br>
	 * Specifically, the new copy will contain the replication settings, database options, Access Control List,
	 * Full Text Index (if any),  as well as data and non-data notes (dependent on the NoteClass argument) of
	 * the original database.<br>
	 * <br>
	 * You may specify the types of notes that you want copied to the new database with the
	 * <code>noteClassesToCopy</code> argument.<br>
	 * <br>
	 * You may also specify the maximum size (database quota) that the database can grow to with the
	 * <code>maxFileSize</code> argument.<br>
	 * <br>
	 * Additionally, you may specify that the new database is to be a replica copy of the original database,
	 * meaning that it will share the same replica ID.<br>
	 * 
	 * @param sourceDbServer server of database to copy
	 * @param sourceDbFilePath filepath of database to copy
	 * @param targetServerName server name of new database to be created
	 * @param targetFilePath filepath of new database to be created
	 * @param noteClassesToCopy type of notes to copy or <code>null</code> to copy all content
	 * @param maxFileSize Size limit for new database in bytes, will be rounded to full megabytes. This argument will control how large the new copy can grow to.  Specify a value of zero if you do not wish to place a size limit on the newly copied database.
	 * @param copyFlags Option flags determining type of copy. Currently, the only supported flags are {@link CopyDatabase#REPLICA}, {@link CopyDatabase#ENCRYPT_SIMPLE}, {@link CopyDatabase#ENCRYPT_MEDIUM}, {@link CopyDatabase#ENCRYPT_STRONG}, {@link CopyDatabase#REPLICA_NAMELIST}, {@link CopyDatabase#OVERRIDE_DEST}.
	 * @param namesList may be null or a UserName that is used to provide authentication for trusted servers.  This causes the UserName's ACL permissions in the database to be enforced.  Please see {@link NotesNamingUtils#buildNamesList(String)} NSFBuildNamesList for more information on building a NAMES_LIST structure.
	 * @return database copy
	 */
	public static NotesDatabase createAndCopyDatabase(String sourceDbServer, String sourceDbFilePath,
			String targetServerName, String targetFilePath,
			EnumSet<NoteClass> noteClassesToCopy,
			long maxFileSize, Set<CopyDatabase> copyFlags,
			NotesNamesList namesList) {
		
		String fullPathTarget = NotesStringUtils.osPathNetConstruct(null, targetServerName, targetFilePath);
		Memory fullPathTargetMem = NotesStringUtils.toLMBCS(fullPathTarget, true);

		String fullPathSource = NotesStringUtils.osPathNetConstruct(null, sourceDbServer, sourceDbFilePath);
		Memory fullPathSourceMem = NotesStringUtils.toLMBCS(fullPathSource, true);

		short noteClassToCopy = noteClassesToCopy==null ? NotesConstants.NOTE_CLASS_ALL : NoteClass.toBitMask(noteClassesToCopy);

		int createCopyFlags = CopyDatabase.toBitMask(copyFlags);
		createCopyFlags |= NotesConstants.DBCOPY_DEST_IS_NSF;
		
		short result;
		
		NotesDatabase dbNew;
		
		NotesNamesList namesListForDbCreate;
		if (namesList==null) {
			namesListForDbCreate = NotesNamingUtils.buildNamesList(targetServerName, IDUtils.getIdUsername());
			NotesNamingUtils.setPrivileges(namesListForDbCreate, EnumSet.of(Privileges.Authenticated));
		}
		else {
			namesListForDbCreate = namesList;
		}

		short maxFileSizeInMB = (short) ((maxFileSize/(1024*1024)) & 0xffff);
		if (PlatformUtils.is64Bit()) {
			LongByReference rethNewDb = new LongByReference();
			
			result = NotesNativeAPI64.get().NSFDbCreateAndCopyExtended(fullPathSourceMem, fullPathTargetMem,
					noteClassToCopy, maxFileSizeInMB, createCopyFlags, namesList==null ? 0 : namesList.getHandle64(), rethNewDb);
			NotesErrorUtils.checkResult(result);
			
			dbNew = new NotesDatabase(rethNewDb.getValue(), namesListForDbCreate.getNames().get(0), namesListForDbCreate);
		}
		else {
			IntByReference rethNewDb = new IntByReference();
			
			result = NotesNativeAPI32.get().NSFDbCreateAndCopyExtended(fullPathSourceMem, fullPathTargetMem,
					noteClassToCopy, maxFileSizeInMB, createCopyFlags, namesList==null ? 0 : namesList.getHandle32(), rethNewDb);
			NotesErrorUtils.checkResult(result);
			
			dbNew = new NotesDatabase(rethNewDb.getValue(), namesListForDbCreate.getNames().get(0), namesListForDbCreate);
		}
		NotesGC.__objectCreated(NotesDatabase.class, dbNew);
		
		return dbNew;
	}
	
	/**
	 * Deletes a database
	 * 
	 * @param server server of database
	 * @param filePath filepath of database
	 */
	public static void deleteDatabase(String server, String filePath) {
		String fullPath = NotesStringUtils.osPathNetConstruct(null, server, filePath);
		Memory fullPathMem = NotesStringUtils.toLMBCS(fullPath, true);

		NotesErrorUtils.checkResult(NotesNativeAPI.get().NSFDbDelete(fullPathMem));
	}
	
	/**
	 * Reads the mail file location from the Notes Client Notes.ini and opens the mail database
	 * with {@link OpenDatabase#CLUSTER_FAILOVER} flag.
	 * 
	 * @return mail database or null when running on server or there is no mail file info in Notes.ini ("MailServer" / "MailFile" lines)
	 */
	public static NotesDatabase openMailDatabase() {
		if (IDUtils.isOnServer()) {
			return null;
		}
		
		String mailServer = NotesIniUtils.getEnvironmentString("MailServer");
		String mailFile = NotesIniUtils.getEnvironmentString("MailFile");
		
		if (StringUtil.isEmpty(mailFile)) {
			return null;
		}
		
		NotesDatabase dbMail = new NotesDatabase(mailServer, mailFile, (String) null, EnumSet.of(OpenDatabase.CLUSTER_FAILOVER));
		return dbMail;
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
	 * Returns the creation date of this database file
	 * 
	 * @return creation date
	 */
	public NotesTimeDate getCreated() {
		checkHandle();
		NotesTimeDateStruct createdStruct = NotesTimeDateStruct.newInstance();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbIDGet(m_hDB64, createdStruct);
		}
		else {
			result = NotesNativeAPI32.get().NSFDbIDGet(m_hDB32, createdStruct);
		}
		NotesErrorUtils.checkResult(result);
		return new NotesTimeDate(createdStruct);
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
		checkHandle();
		short result;
		NotesDbReplicaInfoStruct retReplicationInfo = NotesDbReplicaInfoStruct.newInstance();
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbReplicaInfoGet(m_hDB64, retReplicationInfo);
		}
		else {
			result = NotesNativeAPI32.get().NSFDbReplicaInfoGet(m_hDB32, retReplicationInfo);
		}
		NotesErrorUtils.checkResult(result);
		return new NotesDbReplicaInfo(retReplicationInfo);
	}
	
	/**
	 * Optional flags that can be used with {@link NotesDatabase#getReplicationHistory(Set)}.
	 */
	public enum ReplicationHistoryFlags {
		/** Don't copy wild card entries */
		REMOVE_WILDCARDS(0x00000001),
		
		SORT_BY_DATE(0x00000002),
		
		ONLY_COMPLETE(0x00000004);
		
		private int m_value;
		
		private ReplicationHistoryFlags(int value) {
			m_value = value;
		}

		public int getValue() {
			return m_value;
		}
	}
	
	/**
	 * Reads the replication history of the database
	 * 
	 * @param flags Optional history summary flags enabling you to specify that wildcard entries are not to be returned and/or that sorting is to be done by date rather than by the default, server name
	 * @return replication history or empty list
	 */
	public List<NotesReplicationHistorySummary> getReplicationHistory(Set<ReplicationHistoryFlags> flags) {
		checkHandle();
		
		int flagsAsInt = 0;
		if (flags!=null) {
			for (ReplicationHistoryFlags currFlag : flags) {
				flagsAsInt |= currFlag.getValue();
			}
		}
		
		IntByReference retNumEntries = new IntByReference();
		
		List<NotesReplicationHistorySummary> history = new ArrayList<>();
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethSummary = new LongByReference();
			short result = NotesNativeAPI64.get().NSFDbGetReplHistorySummary(m_hDB64, flagsAsInt, rethSummary, retNumEntries);
			NotesErrorUtils.checkResult(result);

			int numEntries = retNumEntries.getValue();
			long hSummary = rethSummary.getValue();
			
			if (numEntries>0 && rethSummary.getValue()!=0) {
				Pointer ptr = Mem64.OSLockObject(hSummary);
				Pointer currPosPtr = ptr;
				try {
					for (int i=0; i<numEntries; i++) {
						NotesReplicationHistorySummaryStruct struct = NotesReplicationHistorySummaryStruct.newInstance(currPosPtr);
						struct.read();
						
						NotesTimeDate replicationTime = struct.ReplicationTime==null ? null : new NotesTimeDate(struct.ReplicationTime);
						AclLevel aclLevel = AclLevel.toLevel((int) (struct.AccessLevel & 0xffff));
						
						Set<AclFlag> accessFlags = AclFlag.valuesOf((int) (struct.AccessFlags & 0xffff));
						ReplicationDirection direction;
						if (struct.Direction == ReplicationDirection.SEND.getValue()) {
							direction = ReplicationDirection.SEND;
						}
						else if (struct.Direction == ReplicationDirection.RECEIVE.getValue()) {
							direction = ReplicationDirection.RECEIVE;
						}
						else {
							direction = ReplicationDirection.NEVER;
						}
						
						String serverFilePath = NotesStringUtils.fromLMBCS(ptr.share(struct.ServerNameOffset), -1);
						int iPos = serverFilePath.indexOf("!!");
						String server;
						String filePath;
						if (iPos==-1) {
							server = "";
							filePath = "";
						}
						else {
							server = serverFilePath.substring(0, iPos);
							filePath = serverFilePath.substring(iPos+2);
						}
						
						NotesReplicationHistorySummary entry = new NotesReplicationHistorySummary(replicationTime, 
								aclLevel, accessFlags, direction, server, filePath);
						history.add(entry);
						
						currPosPtr = currPosPtr.share(NotesConstants.notesReplicationHistorySummaryStructSize);
					}
				}
				finally {
					Mem64.OSUnlockObject(hSummary);
					Mem64.OSMemFree(hSummary);
				}
			}
		}
		else {
			IntByReference rethSummary = new IntByReference();
			short result = NotesNativeAPI32.get().NSFDbGetReplHistorySummary(m_hDB32, flagsAsInt, rethSummary, retNumEntries);
			NotesErrorUtils.checkResult(result);
			
			int numEntries = retNumEntries.getValue();
			int hSummary = rethSummary.getValue();

			if (numEntries>0 && rethSummary.getValue()!=0) {
				Pointer ptr = Mem32.OSLockObject(rethSummary.getValue());
				Pointer currPosPtr = ptr;
				try {
					for (int i=0; i<numEntries; i++) {

						NotesReplicationHistorySummaryStruct struct = NotesReplicationHistorySummaryStruct.newInstance(currPosPtr);
						struct.read();
						
						NotesTimeDate replicationTime = struct.ReplicationTime==null ? null : new NotesTimeDate(struct.ReplicationTime);
						AclLevel aclLevel = AclLevel.toLevel((int) (struct.AccessLevel & 0xffff));
						
						Set<AclFlag> accessFlags = AclFlag.valuesOf((int) (struct.AccessFlags & 0xffff));
						ReplicationDirection direction;
						if (struct.Direction == ReplicationDirection.SEND.getValue()) {
							direction = ReplicationDirection.SEND;
						}
						else if (struct.Direction == ReplicationDirection.RECEIVE.getValue()) {
							direction = ReplicationDirection.RECEIVE;
						}
						else {
							direction = ReplicationDirection.NEVER;
						}
						
						String serverFilePath = NotesStringUtils.fromLMBCS(ptr.share(struct.ServerNameOffset), -1);
						int iPos = serverFilePath.indexOf("!!");
						String server;
						String filePath;
						if (iPos==-1) {
							server = "";
							filePath = "";
						}
						else {
							server = serverFilePath.substring(0, iPos);
							filePath = serverFilePath.substring(iPos+2);
						}
						
						NotesReplicationHistorySummary entry = new NotesReplicationHistorySummary(replicationTime, 
								aclLevel, accessFlags, direction, server, filePath);
						history.add(entry);
						
						currPosPtr = currPosPtr.share(NotesConstants.notesReplicationHistorySummaryStructSize);
					}
				}
				finally {
					Mem32.OSUnlockObject(hSummary);
					Mem32.OSMemFree(hSummary);
				}
			}
		}
		
		return history;
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
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbReplicaInfoSet(m_hDB64, replInfo.getAdapter(NotesDbReplicaInfoStruct.class));
		}
		else {
			result = NotesNativeAPI32.get().NSFDbReplicaInfoSet(m_hDB32, replInfo.getAdapter(NotesDbReplicaInfoStruct.class));
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
			return "NotesDatabase [handle="+(PlatformUtils.is64Bit() ? m_hDB64 : m_hDB32)+", server="+getServer()+", filepath="+getRelativeFilePath()+"]";
		}
	}
	
	/**
	 * Loads the path information from Notes
	 */
	private void loadPaths() {
		if (m_paths==null) {
			checkHandle();
			
			Memory retCanonicalPathName = new Memory(NotesConstants.MAXPATH);
			Memory retExpandedPathName = new Memory(NotesConstants.MAXPATH);
			
			short result;
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().NSFDbPathGet(m_hDB64, retCanonicalPathName, retExpandedPathName);
			}
			else {
				result = NotesNativeAPI32.get().NSFDbPathGet(m_hDB32, retCanonicalPathName, retExpandedPathName);
			}
			NotesErrorUtils.checkResult(result);
			
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

	public HANDLE getHandle() {
		if (PlatformUtils.is64Bit()) {
			return HANDLE64.newInstance(m_hDB64);
		}
		else {
			return HANDLE32.newInstance(m_hDB32);
		}
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
		if (PlatformUtils.is64Bit()) {
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
		if (!m_noRecycleDb) {
			if (PlatformUtils.is64Bit()) {
				if (m_hDB64!=0) {
					m_recycleHierarchy.recycleChildren();
					
					short result = NotesNativeAPI64.get().NSFDbClose(m_hDB64);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectBeeingBeRecycled(NotesDatabase.class, this);
					m_hDB64=0;
					if (m_namesList!=null && !m_namesList.isFreed()) {
						m_namesList.free();
					}
				}
			}
			else {
				if (m_hDB32!=0) {
					m_recycleHierarchy.recycleChildren();
					
					short result = NotesNativeAPI32.get().NSFDbClose(m_hDB32);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectBeeingBeRecycled(NotesDatabase.class, this);
					m_hDB32=0;
					if (m_namesList!=null && !m_namesList.isFreed()) {
						m_namesList.free();
					}
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
	
	@Override
	public boolean isNoRecycle() {
		return m_noRecycleDb;
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
		
		if (PlatformUtils.is64Bit()) {
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
	 * @return collection or null if not found
	 */
	public NotesCollection openCollectionByName(String viewName, EnumSet<OpenCollection> openFlagSet) {
		checkHandle();
		
		int viewNoteId = findCollection(viewName);
		if (viewNoteId==0) {
			return null;
		}
		return openCollection(viewNoteId, openFlagSet);
	}

	/**
	 * Open a collection by its design document UNID
	 * 
	 * @param unid UNID
	 * @return collection or null if not found
	 */
	public NotesCollection openCollectionByUNID(String unid) {
		return openCollectionByUNID(unid, (EnumSet<OpenCollection>) null);
	}

	/**
	 * Open a collection by its design document UNID
	 * 
	 * @param unid UNID
	 * @param openFlagSet open flags
	 * @return collection or null if not found
	 */
	public NotesCollection openCollectionByUNID(String unid, EnumSet<OpenCollection> openFlagSet) {
		int viewNoteId = toNoteId(unid);
		if (viewNoteId==0) {
			return null;
		}
		return openCollection(viewNoteId, openFlagSet);
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
	 * @return collection or null if not found
	 */
	public NotesCollection openCollectionByNameWithExternalData(NotesDatabase dbData, String viewName, EnumSet<OpenCollection> openFlagSet) {
		checkHandle();
		
		int viewNoteId = findCollection(viewName);
		if (viewNoteId==0) {
			return null;
		}
		return openCollectionWithExternalData(dbData, viewNoteId, openFlagSet);
	}

	/**
	 * Opens a collection by its view note id
	 * 
	 * @param viewNoteId view/collection note id
	 * @param openFlagSet open flags, see {@link OpenCollection}
	 * @return collection
	 */
	public NotesCollection openCollection(int viewNoteId, EnumSet<OpenCollection> openFlagSet)  {
		return openCollectionWithExternalData(this, viewNoteId, openFlagSet);
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
		formatter.format("%016x", data.getLong());
		formatter.format("%016x", data.getLong());
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
	 * @param viewNoteId view/collection note id
	 * @param openFlagSet open flags, see {@link OpenCollection}
	 * @return collection
	 */
	public NotesCollection openCollectionWithExternalData(NotesDatabase dataDb, int viewNoteId, EnumSet<OpenCollection> openFlagSet)  {
		checkHandle();
		
		Memory viewUNID = new Memory(16);
		NotesIDTable unreadTable = new NotesIDTable();
		
		//always enforce reopening; funny things can happen on a Domino server
		//without this flag like sharing collections between users resulting in
		//users seeing the wrong data *sometimes*...
		EnumSet<OpenCollection> openFlagSetClone = openFlagSet==null ? EnumSet.noneOf(OpenCollection.class) : openFlagSet.clone();
		openFlagSetClone.add(OpenCollection.OPEN_REOPEN_COLLECTION);
		
		short openFlags = OpenCollection.toBitMask(openFlagSetClone); //NotesConstants.OPEN_NOUPDATE;

		short result;
		NotesCollection newCol;
		
		if (m_namesList!=null && m_namesList.isFreed()) {
			throw new NotesError(0, "Unexpected state: internal names list has already been recycled");
		}
		
		if (PlatformUtils.is64Bit()) {
			LongByReference hCollection = new LongByReference();
			LongByReference collapsedList = new LongByReference();
			collapsedList.setValue(0);
			LongByReference selectedList = new LongByReference();
			selectedList.setValue(0);
			
			if (!m_passNamesListToViewOpen) {
				//open view as server
				result = NotesNativeAPI64.get().NIFOpenCollection(m_hDB64, dataDb.m_hDB64, viewNoteId, (short) openFlags, unreadTable.getHandle64(), hCollection, null, viewUNID, collapsedList, selectedList);
				NotesErrorUtils.checkResult(result);
			}
			else {
				//now try to open collection as this user
				int retries = 5;
				do {
					//try opening the database multiple times; we had issues here when opening
					//many dbs remotely that could be solved by retrying
					result = NotesNativeAPI64.get().NIFOpenCollectionWithUserNameList(m_hDB64, dataDb.m_hDB64, viewNoteId,
							(short) openFlags, unreadTable.getHandle64(), hCollection, null, viewUNID, collapsedList, selectedList, m_namesList.getHandle64());
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
			newCol = new NotesCollection(this, hCollection.getValue(), viewNoteId, sViewUNID, new NotesIDTable(collapsedList.getValue(), true), new NotesIDTable(selectedList.getValue(), true), unreadTable, m_asUserCanonical);
		}
		else {
			IntByReference hCollection = new IntByReference();
			IntByReference collapsedList = new IntByReference();
			collapsedList.setValue(0);
			IntByReference selectedList = new IntByReference();
			selectedList.setValue(0);
			
			if (!m_passNamesListToViewOpen) {
				result = NotesNativeAPI32.get().NIFOpenCollection(m_hDB32, dataDb.m_hDB32, viewNoteId, (short) openFlags, unreadTable.getHandle32(), hCollection, null, viewUNID, collapsedList, selectedList);
				NotesErrorUtils.checkResult(result);
			}
			else {
				//now try to open collection as this user
				int retries = 5;
				do {
					//try opening the database multiple times; we had issues here when opening
					//many dbs remotely that could be solved by retrying
					result = NotesNativeAPI32.get().NIFOpenCollectionWithUserNameList(m_hDB32, dataDb.m_hDB32, viewNoteId,
							(short) openFlags, unreadTable.getHandle32(), hCollection, null, viewUNID, collapsedList, selectedList, m_namesList.getHandle32());
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
			newCol = new NotesCollection(this, hCollection.getValue(), viewNoteId, sViewUNID, new NotesIDTable(collapsedList.getValue(), true), new NotesIDTable(selectedList.getValue(), true), unreadTable, m_asUserCanonical);
		}
		
		NotesGC.__objectCreated(NotesCollection.class, newCol);
		return newCol;
	}

	/**
	 * Lookup method to find a view
	 * 
	 * @param viewName view name
	 * @return note id of view or 0 if not found
	 */
	public int findView(String viewName) {
		checkHandle();
		
		Memory viewNameLMBCS = NotesStringUtils.toLMBCS(viewName, true);

		IntByReference retViewNoteID = new IntByReference();
		retViewNoteID.setValue(0);

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NIFFindDesignNoteExt(m_hDB64, viewNameLMBCS, NotesConstants.NOTE_CLASS_VIEW, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_VIEW_DESIGN, true), retViewNoteID, 0);
		}
		else {
			result = NotesNativeAPI32.get().NIFFindDesignNoteExt(m_hDB32, viewNameLMBCS, NotesConstants.NOTE_CLASS_VIEW, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_VIEW_DESIGN, true), retViewNoteID, 0);
		}
		
		if ((result & NotesConstants.ERR_MASK)==1028) { //view not found
			return 0;
		}
		
		//throws an error if view cannot be found:
		NotesErrorUtils.checkResult(result);

		return retViewNoteID.getValue();
	}
	
	/**
	 * Lookup method to find a collection (view or folder)
	 * 
	 * @param collectionName collection name
	 * @return note id of collection or 0 if not found
	 */
	public int findCollection(String collectionName) {
		checkHandle();
		
		Memory collectionNameLMBCS = NotesStringUtils.toLMBCS(collectionName, true);

		IntByReference retCollectionNoteID = new IntByReference();
		retCollectionNoteID.setValue(0);

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NIFFindDesignNoteExt(m_hDB64, collectionNameLMBCS, NotesConstants.NOTE_CLASS_VIEW, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_VIEWS_AND_FOLDERS_DESIGN, true), retCollectionNoteID, 0);
		}
		else {
			result = NotesNativeAPI32.get().NIFFindDesignNoteExt(m_hDB32, collectionNameLMBCS, NotesConstants.NOTE_CLASS_VIEW, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_VIEWS_AND_FOLDERS_DESIGN, true), retCollectionNoteID, 0);
		}
		
		if ((result & NotesConstants.ERR_MASK)==1028) { //view not found
			return 0;
		}
		
		//throws an error if view cannot be found:
		NotesErrorUtils.checkResult(result);

		return retCollectionNoteID.getValue();
	}

	/**
	 * Lookup method to find a folder
	 * 
	 * @param folderName folder name
	 * @return note id of folder or 0 if not found
	 */
	public int findFolder(String folderName) {
		checkHandle();
		
		Memory folderNameLMBCS = NotesStringUtils.toLMBCS(folderName, true);

		IntByReference retFolderNoteID = new IntByReference();
		retFolderNoteID.setValue(0);

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NIFFindDesignNoteExt(m_hDB64, folderNameLMBCS, NotesConstants.NOTE_CLASS_VIEW, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_FOLDER_DESIGN, true), retFolderNoteID, 0);
		}
		else {
			result = NotesNativeAPI32.get().NIFFindDesignNoteExt(m_hDB32, folderNameLMBCS, NotesConstants.NOTE_CLASS_VIEW, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_FOLDER_DESIGN, true), retFolderNoteID, 0);
		}
		
		if ((result & NotesConstants.ERR_MASK)==1028) { //view not found
			return 0;
		}
		
		//throws an error if view cannot be found:
		NotesErrorUtils.checkResult(result);

		return retFolderNoteID.getValue();
	}
	
	/**
	 * Performs a fulltext search in the database
	 * 
	 * @param query fulltext query
	 * @param limit Maximum number of documents to return.  Use 0 to return the maximum number of results for the search
	 * @param filterIDTable optional ID table to further refine the search.  Use null if this is not required.
	 * @return search result
	 */
	public NotesFTSearchResult ftSearch(String query, int limit, NotesIDTable filterIDTable) {
		//always return IDTable for database wide searches
		EnumSet<FTSearch> searchOptions = EnumSet.of(FTSearch.RET_IDTABLE);
		return ftSearchExt(query, limit, searchOptions, filterIDTable, 0, 0);
	}
	
	/**
	 * Performs a fulltext search in the database with advanced options.<br>
	 * FTSearchExt is a superset of {@link #ftSearch(String, int, NotesIDTable)}.
	 * 
	 * @param query fulltext query
	 * @param limit Maximum number of documents to return (max. 65535). Use 0 to return the maximum number of results for the search
	 * @param options search options
	 * @param filterIDTable optional ID table to further refine the search.  Use null if this is not required.
	 * @param start the starting document number for the paged result. For the non-paged result, set this item to 0. For the paged result, set this item to a non-zero number.
	 * @param count number of documents to return for the paged result, set to 0 to return all results
	 * @return search result
	 */
	public NotesFTSearchResult ftSearchExt(String query, int limit, EnumSet<FTSearch> options, NotesIDTable filterIDTable, int start, int count) {
		checkHandle();

		if (limit<0 || limit>65535)
			throw new IllegalArgumentException("Limit must be between 0 and 65535 (WORD datatype in C API)");

		EnumSet<FTSearch> searchOptionsToUse = options.clone();
		if (filterIDTable!=null) {
			//automatically set refine option if id table is not null
			searchOptionsToUse.add(FTSearch.REFINE);
		}
		int searchOptionsBitMask = FTSearch.toBitMask(searchOptionsToUse);
		
		short limitAsShort = limit>65535 ? (short) 0xffff : ((short) (limit & 0xffff));
		
		List<String> highlightStrings = null;
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethSearch = new LongByReference();
			
			short result = NotesNativeAPI64.get().FTOpenSearch(rethSearch);
			NotesErrorUtils.checkResult(result);

			Memory queryLMBCS = NotesStringUtils.toLMBCS(query, true);
			IntByReference retNumDocs = new IntByReference();
			LongByReference rethResults = new LongByReference();
			
			LongByReference rethStrings = new LongByReference();
			IntByReference retNumHits = new IntByReference();
			short arg = 0;
			long hNames = 0;
			if (m_passNamesListToDbOpen && m_namesList!=null) {
				hNames = m_namesList.getHandle64();
			}
			
			long t0=System.currentTimeMillis();
			
			result = NotesNativeAPI64.get().FTSearchExt(m_hDB64, 
					rethSearch, 0,
					queryLMBCS, searchOptionsBitMask,
					limitAsShort,
					filterIDTable==null ? 0 : filterIDTable.getHandle64(),
							retNumDocs, rethStrings, rethResults, retNumHits, start, count, arg, hNames);
			
			long t1=System.currentTimeMillis();

			if (result==3874) { //no documents found
				result = NotesNativeAPI64.get().FTCloseSearch(rethSearch.getValue());
				NotesErrorUtils.checkResult(result);
				return new NotesFTSearchResult(new NotesIDTable(), 0, 0, null, null, t1-t0);
			}
			NotesErrorUtils.checkResult(result);
			
			if (searchOptionsToUse.contains(FTSearch.EXT_RET_HL)) {
				//decode highlights
				long hStrings = rethStrings.getValue();
				if (hStrings!=0) {
					Pointer ptr = Mem64.OSLockObject(hStrings);
					try {
						short varLength = ptr.getShort(0);
						ptr = ptr.share(2);
						short flags = ptr.getShort(0);
						ptr = ptr.share(2);
						
						String strHighlights = NotesStringUtils.fromLMBCS(ptr, (int) (varLength & 0xffff));
						
						highlightStrings = new ArrayList<String>();
						StringTokenizerExt st = new StringTokenizerExt(strHighlights, "\n");
						while (st.hasMoreTokens()) {
							String currToken = st.nextToken();
							if (!StringUtil.isEmpty(currToken)) {
								highlightStrings.add(currToken);
							}
						}
					}
					finally {
						Mem64.OSUnlockObject(hStrings);
						Mem64.OSMemFree(hStrings);
					}
				}
			}
			
			NotesIDTable resultsIdTable = null;
			List<NoteIdWithScore> matchesWithScore = null;
			
			if (searchOptionsToUse.contains(FTSearch.RET_IDTABLE)) {
				long hResults = rethResults.getValue();
				if (hResults!=0) {
					resultsIdTable = new NotesIDTable(rethResults.getValue(), false);
				}
				if (resultsIdTable==null) {
					resultsIdTable = new NotesIDTable();
				}
			}
			else {
				long hResults = rethResults.getValue();
				if (hResults!=0) {
					Pointer ptr = Mem64.OSLockObject(hResults);
					try {
						matchesWithScore = FTSearchResultsDecoder.decodeNoteIdsWithStoreSearchResult(ptr, searchOptionsToUse);
					}
					finally {
						Mem64.OSUnlockObject(hResults);
						Mem64.OSMemFree(hResults);
					}
				}
			}
			
			result = NotesNativeAPI64.get().FTCloseSearch(rethSearch.getValue());
			NotesErrorUtils.checkResult(result);

			return new NotesFTSearchResult(resultsIdTable, retNumDocs.getValue(), retNumHits.getValue(), highlightStrings,
					matchesWithScore, t1-t0);
		}
		else {
			IntByReference rethSearch = new IntByReference();
			
			short result = NotesNativeAPI32.get().FTOpenSearch(rethSearch);
			NotesErrorUtils.checkResult(result);

			Memory queryLMBCS = NotesStringUtils.toLMBCS(query, true);
			IntByReference retNumDocs = new IntByReference();
			IntByReference rethResults = new IntByReference();

			IntByReference rethStrings = new IntByReference();
			IntByReference retNumHits = new IntByReference();
			short arg = 0;
			int hNames = 0;
			if (m_passNamesListToDbOpen && m_namesList!=null) {
				hNames = m_namesList.getHandle32();
			}

			long t0=System.currentTimeMillis();
			result = NotesNativeAPI32.get().FTSearchExt(m_hDB32, 
					rethSearch, 0,
					queryLMBCS, searchOptionsBitMask,
					limitAsShort,
					filterIDTable==null ? 0 : filterIDTable.getHandle32(),
							retNumDocs, rethStrings, rethResults, retNumHits, start, count, arg, hNames);
			long t1=System.currentTimeMillis();
			
			if (result==3874) { //no documents found
				result = NotesNativeAPI32.get().FTCloseSearch(rethSearch.getValue());
				NotesErrorUtils.checkResult(result);
				return new NotesFTSearchResult(new NotesIDTable(), 0, 0, null, null, t1-t0);
			}
			NotesErrorUtils.checkResult(result);
			
			if (searchOptionsToUse.contains(FTSearch.EXT_RET_HL)) {
				//decode highlights
				int hStrings = rethStrings.getValue();
				if (hStrings!=0) {
					Pointer ptr = Mem32.OSLockObject(hStrings);
					try {
						short varLength = ptr.getShort(0);
						ptr = ptr.share(2);
						short flags = ptr.getShort(0);
						ptr = ptr.share(2);
						
						String strHighlights = NotesStringUtils.fromLMBCS(ptr, (int) (varLength & 0xffff));
						
						highlightStrings = new ArrayList<String>();
						StringTokenizerExt st = new StringTokenizerExt(strHighlights, "\n");
						while (st.hasMoreTokens()) {
							String currToken = st.nextToken();
							if (!StringUtil.isEmpty(currToken)) {
								highlightStrings.add(currToken);
							}
						}
					}
					finally {
						Mem32.OSUnlockObject(hStrings);
						Mem32.OSMemFree(hStrings);
					}
				}
			}
			
			if (result==3874) { //no documents found
				result = NotesNativeAPI32.get().FTCloseSearch(rethSearch.getValue());
				NotesErrorUtils.checkResult(result);
				return new NotesFTSearchResult(new NotesIDTable(), 0, 0, null, null, t1-t0);
			}
			NotesErrorUtils.checkResult(result);

			NotesIDTable resultsIdTable = null;
			List<NoteIdWithScore> matchesWithScore = null;

			if (searchOptionsToUse.contains(FTSearch.RET_IDTABLE)) {
				int hResults = rethResults.getValue();
				if (hResults!=0) {
					resultsIdTable = new NotesIDTable(rethResults.getValue(), false);
				}
				if (resultsIdTable==null) {
					resultsIdTable = new NotesIDTable();
				}
			}
			else {
				int hResults = rethResults.getValue();
				if (hResults!=0) {
					Pointer ptr = Mem32.OSLockObject(hResults);
					try {
						matchesWithScore = FTSearchResultsDecoder.decodeNoteIdsWithStoreSearchResult(ptr, searchOptionsToUse);
					}
					finally {
						Mem32.OSUnlockObject(hResults);
						Mem32.OSMemFree(hResults);
					}
				}
			}
			
			result = NotesNativeAPI32.get().FTCloseSearch(rethSearch.getValue());
			NotesErrorUtils.checkResult(result);


			return new NotesFTSearchResult(resultsIdTable, retNumDocs.getValue(), retNumHits.getValue(), highlightStrings,
					matchesWithScore, t1-t0);
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
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFDbDeleteNotes(m_hDB64, idTable.getHandle64(), null);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFDbDeleteNotes(m_hDB32, idTable.getHandle32(), null);
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
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFNoteDeleteExtended(m_hDB64, noteId, flagsAsInt);
		}
		else {
			result = NotesNativeAPI32.get().NSFNoteDeleteExtended(m_hDB32, noteId, flagsAsInt);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * This function clears out the replication history information of the specified database replica.
	 * This can also be done using the Notes user interface via the File/Replication/History menu item selection.
	 */
	public void clearReplicationHistory() {
		checkHandle();
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFDbClearReplHistory(m_hDB64, 0);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFDbClearReplHistory(m_hDB32, 0);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function obtains the time/date of the last modified data and non-data notes in the specified database.
	 * 
	 * @return array with last modified date/time for data and non-date
	 */
	public NotesTimeDate[] getLastModifiedTimes() {
		checkHandle();
		
		NotesTimeDateStruct retDataModifiedStruct = NotesTimeDateStruct.newInstance();
		NotesTimeDateStruct retNonDataModifiedStruct = NotesTimeDateStruct.newInstance();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbModifiedTime(m_hDB64, retDataModifiedStruct, retNonDataModifiedStruct);
		}
		else {
			result = NotesNativeAPI32.get().NSFDbModifiedTime(m_hDB32, retDataModifiedStruct, retNonDataModifiedStruct);
		}
		NotesErrorUtils.checkResult(result);
		
		return new NotesTimeDate[] {
				new NotesTimeDate(retDataModifiedStruct),
				new NotesTimeDate(retNonDataModifiedStruct)
		};
	}
	
	/**
	 * This function returns an ID Table of Note IDs of notes which have been modified in some way
	 * from the given starting time until "now".  The ending time/date is returned, so that this
	 * function can be performed incrementally.<br>
	 * Except when TIMEDATE_MINIMUM is specified, the IDs of notes deleted during the time span will
	 * also be returned in the ID Table, and the IDs of these deleted notes have been ORed with
	 * {@link NotesConstants#RRV_DELETED} before being added to the table.  You must check the
	 * {@link NotesConstants#RRV_DELETED} flag when using the resulting table.<br>
	 * <br>
	 * Note: If there are NO modified or deleted notes in the database since the specified time,
	 * the Notes C API returns an error ERR_NO_MODIFIED_NOTES. In our wrapper code, we check for
	 * this error and return an empty {@link NotesIDTable} instead.<br>
	 * <br>
	 * Note: You program is responsible for freeing up the returned id table handle.
	 * 
	 * @param noteClassMaskEnum the appropriate {@link NoteClass} mask for the documents you wish to select. Symbols can be OR'ed to obtain the desired Note classes in the resulting ID Table.  
	 * @param since A TIMEDATE structure containing the starting date used when selecting notes to be added to the ID Table built by this function. To include ALL notes (including those deleted during the time span) of a given note class, use {@link NotesTimeDate#setWildcard()}.  To include ALL notes of a given note class, but excluding those notes deleted during the time span, use {@link NotesTimeDate#setMinimum()}.
	 * @param retUntil A pointer to a {@link NotesTimeDate} structure into which the ending time of this search will be returned.  This can subsequently be used as the starting time in a later search.
	 * @return newly allocated ID Table, you are responsible for freeing the storage when you are done with it using {@link NotesIDTable#recycle()}
	 */
	public NotesIDTable getModifiedNoteTable(EnumSet<NoteClass> noteClassMaskEnum, NotesTimeDate since, NotesTimeDate retUntil) {
		short noteClassMask = NoteClass.toBitMask(noteClassMaskEnum);

		return getModifiedNoteTable(noteClassMask, since, retUntil);
	}
	
	/**
	 * This function returns an ID Table of Note IDs of notes which have been modified in some way
	 * from the given starting time until "now".  The ending time/date is returned, so that this
	 * function can be performed incrementally.<br>
	 * Except when TIMEDATE_MINIMUM is specified, the IDs of notes deleted during the time span will
	 * also be returned in the ID Table, and the IDs of these deleted notes have been ORed with
	 * {@link NotesConstants#RRV_DELETED} before being added to the table.  You must check the
	 * {@link NotesConstants#RRV_DELETED} flag when using the resulting table.<br>
	 * <br>
	 * Note: If there are NO modified or deleted notes in the database since the specified time,
	 * the Notes C API returns an error ERR_NO_MODIFIED_NOTES. In our wrapper code, we check for
	 * this error and return an empty {@link NotesIDTable} instead.<br>
	 * <br>
	 * Note: You program is responsible for freeing up the returned id table handle.
	 * 
	 * @param noteClassMask {@link NoteClass} mask as short.  
	 * @param since A TIMEDATE structure containing the starting date used when selecting notes to be added to the ID Table built by this function. To include ALL notes (including those deleted during the time span) of a given note class, use {@link NotesTimeDate#setWildcard()}.  To include ALL notes of a given note class, but excluding those notes deleted during the time span, use {@link NotesTimeDate#setMinimum()}.
	 * @param retUntil A pointer to a {@link NotesTimeDate} structure into which the ending time of this search will be returned.  This can subsequently be used as the starting time in a later search.
	 * @return newly allocated ID Table, you are responsible for freeing the storage when you are done with it using {@link NotesIDTable#recycle()}
	 */
	private NotesIDTable getModifiedNoteTable(short noteClassMask, NotesTimeDate since, NotesTimeDate retUntil) {
		checkHandle();

		
		//make sure retUntil is not null
		if (retUntil==null)
			retUntil = new NotesTimeDate();
		
		NotesTimeDateStruct sinceStruct = NotesTimeDateStruct.newInstance(since.getInnards());
		NotesTimeDateStruct.ByValue sinceStructByVal = NotesTimeDateStruct.ByValue.newInstance();
		sinceStructByVal.Innards[0] = sinceStruct.Innards[0];
		sinceStructByVal.Innards[1] = sinceStruct.Innards[1];
		sinceStructByVal.write();
		NotesTimeDateStruct.ByReference retUntilStruct = NotesTimeDateStruct.newInstanceByReference();
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethTable = new LongByReference();
			short result = NotesNativeAPI64.get().NSFDbGetModifiedNoteTable(m_hDB64, noteClassMask, sinceStructByVal, retUntilStruct, rethTable);
			if (result == INotesErrorConstants.ERR_NO_MODIFIED_NOTES) {
				return new NotesIDTable();
			}
			NotesErrorUtils.checkResult(result);
			
			retUntil.getInnardsNoClone()[0] = retUntilStruct.Innards[0];
			retUntil.getInnardsNoClone()[1] = retUntilStruct.Innards[1];

			return new NotesIDTable(rethTable.getValue(), false);
		}
		else {
			IntByReference rethTable = new IntByReference();
			short result = NotesNativeAPI32.get().NSFDbGetModifiedNoteTable(m_hDB32, noteClassMask, sinceStructByVal, retUntilStruct, rethTable);
			if (result == INotesErrorConstants.ERR_NO_MODIFIED_NOTES) {
				return new NotesIDTable();
			}
			NotesErrorUtils.checkResult(result);
			
			retUntil.getInnardsNoClone()[0] = retUntilStruct.Innards[0];
			retUntil.getInnardsNoClone()[1] = retUntilStruct.Innards[1];

			return new NotesIDTable(rethTable.getValue(), false);
		}
	}
	
	/**
	 * Opens and returns the design collection
	 * 
	 * @return design collection
	 */
	public NotesCollection openDesignCollection() {
		try {
			NotesCollection col = openCollection(NotesConstants.NOTE_ID_SPECIAL | NotesConstants.NOTE_CLASS_DESIGN, null);
			if (col!=null) {
				return col;
			}
		}
		catch (NotesError e) {
			//ignore, we call DesignOpenCollection next which creates the design collection
		}
		
		HANDLE hDb = getHandle();
		
		DHANDLE.ByReference rethCollection = DHANDLE.newInstanceByReference();
		IntByReference retCollectionNoteID = new IntByReference();
		short openResult = NotesNativeAPI.get().DesignOpenCollection(hDb.getByValue(),
				false, (short) 0, rethCollection, retCollectionNoteID);
		
		if (openResult==0) {
			NotesErrorUtils.checkResult(NotesNativeAPI.get().NIFCloseCollection(rethCollection.getByValue()));
		}
		
		//try again:
		NotesCollection col = openCollection(NotesConstants.NOTE_ID_SPECIAL | NotesConstants.NOTE_CLASS_DESIGN, null);
		return col;
	}
	
	/**
	 * Returns basic information about all views in the database, read from
	 * the design collection.
	 * 
	 * @return view info
	 */
	public List<NotesCollectionSummary> getAllCollections() {
		LinkedHashMap<String,String> columnFormulas = new LinkedHashMap<String, String>();
		columnFormulas.put("$title", "");
		columnFormulas.put(NotesConstants.DESIGN_FLAGS, "");
		columnFormulas.put("$comment", "");
		columnFormulas.put("$language", "");

		List<NotesCollectionSummary> collections = new ArrayList<>();

		NotesSearch.search(this, null, "@True", columnFormulas, "-", EnumSet.of(Search.SUMMARY),
				EnumSet.of(NoteClass.VIEW), null, new NotesSearch.SearchCallback() {

			@Override
			public NotesSearch.SearchCallback.Action noteFound(NotesDatabase parentDb,
					ISearchMatch searchMatch, IItemTableData summaryBufferData) {

				NotesCollectionSummary newInfo = new NotesCollectionSummary(NotesDatabase.this);
				collections.add(newInfo);

				newInfo.setNoteId(searchMatch.getNoteId());

				String titleAndAliases = summaryBufferData.getAsString("$title", "");
				if (titleAndAliases.contains("|")) {
					StringTokenizerExt st = new StringTokenizerExt(titleAndAliases, "|");
					String title = st.nextToken();
					newInfo.setTitle(title);

					List<String> aliases = new ArrayList<String>();
					while (st.hasMoreTokens()) {
						aliases.add(st.nextToken());
					}
					newInfo.setAliases(aliases);
				}
				else {
					newInfo.setTitle(titleAndAliases);
					newInfo.setAliases(Collections.emptyList());
				}

				String flags = summaryBufferData.getAsString(NotesConstants.DESIGN_FLAGS, "");
				newInfo.setFlags(flags);

				String comment = summaryBufferData.getAsString("$comment", "");
				newInfo.setComment(comment);

				String language = summaryBufferData.getAsString("$language", "");
				newInfo.setLanguage(language);

				return SearchCallback.Action.Continue;
			}
		}
				);

		return collections;
	}
	
	/**
	 * Opens the default collection for the database
	 * 
	 * @return default collection or null if not found
	 */
	public NotesCollection openDefaultCollection() {
		checkHandle();
		
		short result;
		IntByReference retNoteID = new IntByReference();
		
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbGetSpecialNoteID(m_hDB64, (short) ((NotesConstants.SPECIAL_ID_NOTE | NotesConstants.NOTE_CLASS_VIEW) & 0xffff), retNoteID);
		}
		else {
			result = NotesNativeAPI32.get().NSFDbGetSpecialNoteID(m_hDB32, (short) ((NotesConstants.SPECIAL_ID_NOTE | NotesConstants.NOTE_CLASS_VIEW) & 0xffff), retNoteID);
		}
		if ((result & NotesConstants.ERR_MASK)==1028) { //not found
			return null;
		}
		NotesErrorUtils.checkResult(result);
		int noteId = retNoteID.getValue();
		if (noteId==0) {
			return null;
		}
		NotesCollection col = openCollection(noteId, null);
		return col;
	}
	
	/**
	 * Returns the icon note
	 * 
	 * @return icon note or null if not found
	 */
	public NotesNote openIconNote() {
		checkHandle();

		short result;
		IntByReference retNoteID = new IntByReference();
		
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbGetSpecialNoteID(m_hDB64, (short) ((NotesConstants.SPECIAL_ID_NOTE | NotesConstants.NOTE_CLASS_ICON) & 0xffff), retNoteID);
		}
		else {
			result = NotesNativeAPI32.get().NSFDbGetSpecialNoteID(m_hDB32, (short) ((NotesConstants.SPECIAL_ID_NOTE | NotesConstants.NOTE_CLASS_ICON) & 0xffff), retNoteID);
		}
		if ((result & NotesConstants.ERR_MASK)==1028) { //not found
			return null;
		}
		NotesErrorUtils.checkResult(result);
		int noteId = retNoteID.getValue();
		if (noteId==0) {
			return null;
		}
		return openNoteById(noteId);
	}
	
	/**
	 * Returns the new true color icon note (form design document with $TITLE="$DBIcon")
	 * 
	 * @return icon note or null if not found
	 */
	public NotesNote openTrueColorIconNote() {
		checkHandle();
		AtomicInteger retNoteId = new AtomicInteger();

		NotesSearch.search(this, null, "$TITLE=\"$DBIcon\"", "-",
				EnumSet.noneOf(Search.class),
				EnumSet.of(NoteClass.FORM),
				null, new NotesSearch.SearchCallback() {

			@Override
			public Action noteFound(NotesDatabase parentDb, ISearchMatch searchMatch,
					IItemTableData summaryBufferData) {
				retNoteId.set(searchMatch.getNoteId());
				return Action.Stop;
			}

		});

		if (retNoteId.get()==0) {
			return null;
		}
		else {
			return openNoteById(retNoteId.get());
		}
	}
	
	/**
	 * Returns the note of the default form
	 * 
	 * @return default form note or null if not found
	 */
	public NotesNote openDefaultFormNote() {
		checkHandle();

		short result;
		IntByReference retNoteID = new IntByReference();
		
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbGetSpecialNoteID(m_hDB64, (short) ((NotesConstants.SPECIAL_ID_NOTE | NotesConstants.NOTE_CLASS_FORM) & 0xffff), retNoteID);
		}
		else {
			result = NotesNativeAPI32.get().NSFDbGetSpecialNoteID(m_hDB32, (short) ((NotesConstants.SPECIAL_ID_NOTE | NotesConstants.NOTE_CLASS_FORM) & 0xffff), retNoteID);
		}
		if ((result & NotesConstants.ERR_MASK)==1028) { //not found
			return null;
		}
		NotesErrorUtils.checkResult(result);
		int noteId = retNoteID.getValue();
		if (noteId==0) {
			return null;
		}
		return openNoteById(noteId);
	}
	
	/**
	 * Returns the database info note
	 * 
	 * @return info note or null if not found
	 */
	public NotesNote openDatabaseInfoNote() {
		checkHandle();

		short result;
		IntByReference retNoteID = new IntByReference();
		
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbGetSpecialNoteID(m_hDB64, (short) ((NotesConstants.SPECIAL_ID_NOTE | NotesConstants.NOTE_CLASS_INFO) & 0xffff), retNoteID);
		}
		else {
			result = NotesNativeAPI32.get().NSFDbGetSpecialNoteID(m_hDB32, (short) ((NotesConstants.SPECIAL_ID_NOTE | NotesConstants.NOTE_CLASS_INFO) & 0xffff), retNoteID);
		}
		if ((result & NotesConstants.ERR_MASK)==1028) { //not found
			return null;
		}
		NotesErrorUtils.checkResult(result);
		int noteId = retNoteID.getValue();
		if (noteId==0) {
			return null;
		}
		return openNoteById(noteId);
	}
	
	/**
	 * Returns the database help note
	 * 
	 * @return help note or null if not found
	 */
	public NotesNote openDatabaseHelpNote() {
		checkHandle();

		short result;
		IntByReference retNoteID = new IntByReference();
		
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbGetSpecialNoteID(m_hDB64, (short) ((NotesConstants.SPECIAL_ID_NOTE | NotesConstants.NOTE_CLASS_HELP) & 0xffff), retNoteID);
		}
		else {
			result = NotesNativeAPI32.get().NSFDbGetSpecialNoteID(m_hDB32, (short) ((NotesConstants.SPECIAL_ID_NOTE | NotesConstants.NOTE_CLASS_HELP) & 0xffff), retNoteID);
		}
		if ((result & NotesConstants.ERR_MASK)==1028) { //not found
			return null;
		}
		NotesErrorUtils.checkResult(result);
		int noteId = retNoteID.getValue();
		if (noteId==0) {
			return null;
		}
		return openNoteById(noteId);
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
		 * Method to skip signing for specific notes
		 * 
		 * @param designElement design element
		 * @param currentSigner current design element signer
		 * @return true to sign
		 */
		public abstract boolean shouldSign(DesignElement designElement, String currentSigner);
		
		/**
		 * Method is called after signing a note
		 * 
		 * @param designElement design element
		 * @return return value to stop signing
		 */
		public abstract Action noteSigned(DesignElement designElement);
	}

	/**
	 * Looks up a design note by its name
	 * 
	 * @param name name
	 * @param noteType type of design note
	 * @return note, null if not found
	 */
	public NotesNote findDesignNote(String name, NoteClass noteType) {
		int noteId = findDesignNoteId(name, noteType);
		if (noteId==0) {
			return null;
		}
		return openNoteById(noteId);
	}
	
	/**
	 * Looks up a design note by its name and returns the note id
	 * 
	 * @param name name
	 * @param noteType type of design note
	 * @return note id or 0 if not found
	 */
	public int findDesignNoteId(String name, NoteClass noteType) {
		if (StringUtil.isEmpty(name)) {
			return 0;
		}
		
		checkHandle();
		IntByReference retNoteID = new IntByReference();
		retNoteID.setValue(0);
		
		Memory nameMem = NotesStringUtils.toLMBCS(name, true);
		short noteTypeShort = (short) (noteType.getValue() & 0xffff);
		
		Memory flagsPatternMem = null;
		if (noteType == NoteClass.VIEW) {
			flagsPatternMem = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_VIEWS_AND_FOLDERS_DESIGN, true);
		}
		else if (noteType == NoteClass.FILTER) {
			flagsPatternMem = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_AGENTSLIST, true);
		}
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NIFFindDesignNoteExt(m_hDB64, nameMem, noteTypeShort, flagsPatternMem, retNoteID, 0);
		}
		else {
			result = NotesNativeAPI32.get().NIFFindDesignNoteExt(m_hDB32, nameMem, noteTypeShort, flagsPatternMem, retNoteID, 0);
		}

		if (noteType==NoteClass.FORM) {
			if (((result & NotesConstants.ERR_MASK)==INotesErrorConstants.ERR_NOT_FOUND) ||
					((result & NotesConstants.ERR_MASK)==INotesErrorConstants.ERR_INVALID_NOTE) ||
					retNoteID.getValue()==0) {
				
				IntByReference retbIsPrivate = new IntByReference();
				
				if (PlatformUtils.is64Bit()) {
					result = NotesNativeAPI64.get().DesignLookupNameFE(m_hDB64, noteTypeShort,
							null, nameMem, (short) ((nameMem.size()-1) & 0xffff),
							NotesConstants.DGN_STRIPUNDERS | NotesConstants.DGN_SKIPSYNONYMS | NotesConstants.DGN_ONLYSHARED,
							retNoteID, retbIsPrivate, null, null);
				}
				else {
					result = NotesNativeAPI32.get().DesignLookupNameFE(m_hDB32, noteTypeShort,
							null, nameMem, (short) ((nameMem.size()-1) & 0xffff),
							NotesConstants.DGN_STRIPUNDERS | NotesConstants.DGN_SKIPSYNONYMS | NotesConstants.DGN_ONLYSHARED,
							retNoteID, retbIsPrivate, null, null);
				}
			}
		}

		if (((result & NotesConstants.ERR_MASK)==INotesErrorConstants.ERR_NOT_FOUND) ||
				((result & NotesConstants.ERR_MASK)==INotesErrorConstants.ERR_INVALID_NOTE)) {
			return 0;
		}
		
		NotesErrorUtils.checkResult(result);
		
		int noteId = retNoteID.getValue();
		return noteId;
	}
	
	/**
	 * Opens an agent in the database
	 * 
	 * @param agentName agent name
	 * @return agent or null if not found
	 */
	public NotesAgent getAgent(String agentName) {
		checkHandle();

		Memory agentNameLMBCS = NotesStringUtils.toLMBCS(agentName, true);

		IntByReference retAgentNoteID = new IntByReference();
		retAgentNoteID.setValue(0);
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NIFFindDesignNoteExt(m_hDB64, agentNameLMBCS, NotesConstants.NOTE_CLASS_FILTER, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_AGENTSLIST, true), retAgentNoteID, NotesConstants.DGN_STRIPUNDERS);
		}
		else {
			result = NotesNativeAPI32.get().NIFFindDesignNoteExt(m_hDB32, agentNameLMBCS, NotesConstants.NOTE_CLASS_FILTER, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_AGENTSLIST, true), retAgentNoteID, NotesConstants.DGN_STRIPUNDERS);
		}
		if ((result & NotesConstants.ERR_MASK)==1028) {
			//Entry not found in index
			return null;
		}
		
		//throws an error if agent cannot be found:
		NotesErrorUtils.checkResult(result);
		
		int agentNoteId = retAgentNoteID.getValue();
		if (agentNoteId==0) {
			throw new NotesError(0, "Agent not found in database: "+agentName);
		}
		
		NotesAgent agent;
		if (PlatformUtils.is64Bit()) {
			LongByReference rethAgent = new LongByReference();
			
			result = NotesNativeAPI64.get().AgentOpen(m_hDB64, agentNoteId, rethAgent);
			NotesErrorUtils.checkResult(result);
			
			agent = new NotesAgent(this, agentNoteId, rethAgent.getValue());
		}
		else {
			IntByReference rethAgent = new IntByReference();
			
			result = NotesNativeAPI32.get().AgentOpen(m_hDB32, agentNoteId, rethAgent);
			NotesErrorUtils.checkResult(result);
			
			agent = new NotesAgent(this, agentNoteId, rethAgent.getValue());
		}
		NotesGC.__objectCreated(NotesAgent.class, agent);
		
		return agent;
	}

	/**
	 * The returned document is created when you save an agent, and it is stored in
	 * the same database as the agent.<br>
	 * The document replicates, but is not displayed in views.<br>
	 * Each time you edit and re-save an agent, its saved data document is deleted
	 * and a new, blank one is created. When you delete an agent, its saved data document is deleted.
	 * 
	 * @param agentName agent name
	 * @return document or null if agent could not be found
	 */
	public NotesNote getAgentSavedData(String agentName) {
		checkHandle();

		Memory agentNameLMBCS = NotesStringUtils.toLMBCS(agentName, true);

		IntByReference retAgentNoteID = new IntByReference();
		retAgentNoteID.setValue(0);
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NIFFindDesignNoteExt(m_hDB64, agentNameLMBCS, NotesConstants.NOTE_CLASS_FILTER, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_AGENTSLIST, true), retAgentNoteID, NotesConstants.DGN_STRIPUNDERS);
		}
		else {
			result = NotesNativeAPI32.get().NIFFindDesignNoteExt(m_hDB32, agentNameLMBCS, NotesConstants.NOTE_CLASS_FILTER, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_AGENTSLIST, true), retAgentNoteID, NotesConstants.DGN_STRIPUNDERS);
		}
		if ((result & NotesConstants.ERR_MASK)==1028) {
			//Entry not found in index
			return null;
		}
		
		//throws an error if agent cannot be found:
		NotesErrorUtils.checkResult(result);
		
		int agentNoteId = retAgentNoteID.getValue();
		if (agentNoteId==0) {
			throw new NotesError(0, "Agent not found in database: "+agentName);
		}

		NotesUniversalNoteIdStruct.ByReference retUNID = NotesUniversalNoteIdStruct.newInstanceByReference();
		
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().AssistantGetLSDataNote(m_hDB64, agentNoteId, retUNID);
		}
		else {
			result = NotesNativeAPI32.get().AssistantGetLSDataNote(m_hDB32, agentNoteId, retUNID);
		}
		NotesErrorUtils.checkResult(result);
		
		String unid = retUNID.toString();
		if (StringUtil.isEmpty(unid) || "00000000000000000000000000000000".equals(unid)) {
			return null;
		}
		else {
			return openNoteByUnid(unid);
		}
	}
	
	/**
	 * Sign all documents of a specified note class (see NOTE_CLASS_* in {@link NotesConstants}.
	 * 
	 * @param noteClassesEnum bitmask of note classes to sign
	 * @param callback optional callback to get notified about signed notes or null
	 */
	public void signAll(EnumSet<NoteClass> noteClassesEnum, SignCallback callback) {
		signAll(null, noteClassesEnum, false, callback);
	}
	
	public static class DesignElement {
		private String unid;
		private int noteId;
		private EnumSet<NoteClass> noteClass;
		private String title;
		private String flags;
		private NotesTimeDate lastModified;
		private NotesTimeDate sequenceTime;
		private int sequenceNumber;
		
		private DesignElement() {
		}

		public String getUNID() {
			return unid;
		}
		
		private void setUNID(String unid) {
			this.unid = unid;
		}
		
		public int getNoteId() {
			return noteId;
		}

		private void setNoteId(int noteId) {
			this.noteId = noteId;
		}

		public EnumSet<NoteClass> getNoteClass() {
			return noteClass;
		}

		private void setNoteClass(EnumSet<NoteClass> noteClass) {
			this.noteClass = noteClass;
		}

		public String getTitle() {
			return title;
		}

		private void setTitle(String title) {
			this.title = title;
		}
		
		public String getFlags() {
			return this.flags;
		}
		
		private void setFlags(String flags) {
			this.flags = flags;
		}

		public NotesTimeDate getLastModified() {
			return lastModified;
		}

		private void setLastModified(NotesTimeDate lastModified) {
			this.lastModified = lastModified;
		}

		public NotesTimeDate getSequenceTime() {
			return sequenceTime;
		}

		private void setSequenceTime(NotesTimeDate sequenceTime) {
			this.sequenceTime = sequenceTime;
		}

		public int getSequenceNumber() {
			return sequenceNumber;
		}

		private void setSequenceNumber(int sequenceNumber) {
			this.sequenceNumber = sequenceNumber;
		}

		@Override
		public String toString() {
			return "DesignElement [unid=" + unid + ", noteId=" + noteId + ", noteClass=" + noteClass + ", title="
					+ title + ", flags=" + flags + ", lastModified=" + lastModified + ", sequenceTime=" + sequenceTime
					+ ", sequenceNumber=" + sequenceNumber + "]";
		}
	}
	
	/**
	 * Performs a database search and returns basic information about design elements of
	 * the specified types
	 * 
	 * @param noteClassesEnum note classes
	 * @return design elements
	 */
	public List<DesignElement> getDesignElements(EnumSet<NoteClass> noteClassesEnum) {
		checkHandle();
		
		LinkedHashMap<String, String> columnFormulas = new LinkedHashMap<>();
		columnFormulas.put("$title", "");
		columnFormulas.put("$flags", "");
		
		List<DesignElement> designElements = new ArrayList<>();

		NotesSearch.search(this, null, "@True", columnFormulas, "-", EnumSet.of(Search.SUMMARY),
				noteClassesEnum, null, new NotesSearch.SearchCallback() {

					@Override
					public Action noteFound(NotesDatabase parentDb, ISearchMatch searchMatch,
							IItemTableData summaryBufferData) {
						
						String title = summaryBufferData.getAsString("$title", "");
						int noteId = searchMatch.getNoteId();
						EnumSet<NoteClass> noteClass = searchMatch.getNoteClass();
						String flags = summaryBufferData.getAsString("$flags", "");
						
						DesignElement de = new DesignElement();
						de.setNoteId(noteId);
						de.setNoteClass(noteClass);
						de.setTitle(title);
						de.setFlags(flags);
						de.setUNID(searchMatch.getUNID());
						de.setLastModified(searchMatch.getNoteModified());
						de.setSequenceNumber(searchMatch.getSeq());
						de.setSequenceTime(searchMatch.getSeqTime());
						
						designElements.add(de);
						
						return Action.Continue;
					}
		});
		
		return designElements;
	}
	
	/**
	 * Sign all documents of a specified note class (see NOTE_CLASS_* in {@link NotesConstants}.
	 * 
	 * @param userId ID to be used for signing, use null for the active Notes ID
	 * @param noteClassesEnum bitmask of note classes to sign
	 * @param signNotesIfMimePresent If the note has MIME parts and this flag is true it will be SMIME signed, if not set it will be Notes signed.
	 * @param callback optional callback to get notified about signed notes or null
	 */
	public void signAll(NotesUserId userId, EnumSet<NoteClass> noteClassesEnum, boolean signNotesIfMimePresent,
			SignCallback callback) {
		
		checkHandle();
		
		String idUser = userId==null ? IDUtils.getIdUsername() : userId.getUsername();
		List<DesignElement> designElements = getDesignElements(noteClassesEnum);
		
		for (DesignElement currDE : designElements) {
			int currNoteId = currDE.getNoteId();

			EnumSet<NoteClass> currNoteClass = currDE.getNoteClass();
			
			boolean expandNote = false;
			if (currNoteClass.contains(NoteClass.FORM) || currNoteClass.contains(NoteClass.INFO) ||
					currNoteClass.contains(NoteClass.HELP) || currNoteClass.contains(NoteClass.FIELD)) {
				expandNote = true;
			}

			if (PlatformUtils.is64Bit()) {
				LongByReference rethNote = new LongByReference();
				
				short result = NotesNativeAPI64.get().NSFNoteOpen(m_hDB64, currNoteId, expandNote ? NotesConstants.OPEN_EXPAND : 0, rethNote);
				NotesErrorUtils.checkResult(result);
				try {
					NotesTimeDateStruct retWhenSigned = NotesTimeDateStruct.newInstance();
					Memory retSigner = new Memory(NotesConstants.MAXUSERNAME);
					Memory retCertifier = new Memory(NotesConstants.MAXUSERNAME);
					
					result = NotesNativeAPI64.get().NSFNoteVerifySignature(rethNote.getValue(), null, retWhenSigned, retSigner, retCertifier);
					
					boolean signRequired = false;
					String currNoteSigner;
					if (result != 0) {
						signRequired = true;
						currNoteSigner = "";
					}
					else {
						currNoteSigner = NotesStringUtils.fromLMBCS(retSigner, NotesStringUtils.getNullTerminatedLength(retSigner));
						if (userId==null && NotesNamingUtils.equalNames(idUser, currNoteSigner)) {
							//we have no user id to be used for signing and the note is already signed by current user
							continue;
						}
						else {
							signRequired = true;
						}
					}
					
					if (callback!=null && !callback.shouldSign(currDE, currNoteSigner)) {
						signRequired = false;
					}
					
					if (signRequired) {
						if (userId!=null) {
							result = NotesNativeAPI64.get().NSFNoteSignExt3(rethNote.getValue(), userId==null ? 0 : userId.getHandle64(), null, NotesConstants.MAXWORD, 0, signNotesIfMimePresent ? NotesConstants.SIGN_NOTES_IF_MIME_PRESENT : 0, 0, null);
						}
						else {
							result = NotesNativeAPI64.get().NSFNoteSign(rethNote.getValue());
						}
						NotesErrorUtils.checkResult(result);

						if (expandNote) {
							result = NotesNativeAPI64.get().NSFNoteContract(rethNote.getValue());
							NotesErrorUtils.checkResult(result);
						}
						
						result = NotesNativeAPI64.get().NSFNoteUpdateExtended(rethNote.getValue(), 0);
						NotesErrorUtils.checkResult(result);
					}
				}
				finally {
					result = NotesNativeAPI64.get().NSFNoteClose(rethNote.getValue());
					NotesErrorUtils.checkResult(result);
				}
			}
			else {
				IntByReference rethNote = new IntByReference();
				short result = NotesNativeAPI32.get().NSFNoteOpen(m_hDB32, currNoteId, expandNote ? NotesConstants.OPEN_EXPAND : 0, rethNote);
				NotesErrorUtils.checkResult(result);
				try {
					NotesTimeDateStruct retWhenSigned = NotesTimeDateStruct.newInstance();
					Memory retSigner = new Memory(NotesConstants.MAXUSERNAME);
					Memory retCertifier = new Memory(NotesConstants.MAXUSERNAME);
					
					result = NotesNativeAPI32.get().NSFNoteVerifySignature(rethNote.getValue(), null, retWhenSigned, retSigner, retCertifier);
					
					boolean signRequired = false;
					String currNoteSigner;
					if (result != 0) {
						signRequired = true;
						currNoteSigner = "";
					}
					else {
						currNoteSigner = NotesStringUtils.fromLMBCS(retSigner, NotesStringUtils.getNullTerminatedLength(retSigner));
						if (userId==null && NotesNamingUtils.equalNames(idUser, currNoteSigner)) {
							//we have no user id to be used for signing and the note is already signed by current user
							continue;
						}
						else {
							signRequired = true;
						}
					}
					
					if (callback!=null && !callback.shouldSign(currDE, currNoteSigner)) {
						signRequired = false;
					}

					if (signRequired) {
						if (userId!=null) {
							result = NotesNativeAPI32.get().NSFNoteSignExt3(rethNote.getValue(), userId==null ? 0 : userId.getHandle32(), null, NotesConstants.MAXWORD, 0, signNotesIfMimePresent ? NotesConstants.SIGN_NOTES_IF_MIME_PRESENT : 0, 0, null);
						}
						else {
							result = NotesNativeAPI32.get().NSFNoteSign(rethNote.getValue());
						}
						NotesErrorUtils.checkResult(result);

						if (expandNote) {
							result = NotesNativeAPI32.get().NSFNoteContract(rethNote.getValue());
							NotesErrorUtils.checkResult(result);
						}
						
						result = NotesNativeAPI32.get().NSFNoteUpdateExtended(rethNote.getValue(), 0);
						NotesErrorUtils.checkResult(result);
					}
				}
				finally {
					result = NotesNativeAPI32.get().NSFNoteClose(rethNote.getValue());
					NotesErrorUtils.checkResult(result);
				}
			}
			
			if (callback!=null) {
				com.mindoo.domino.jna.NotesDatabase.SignCallback.Action action = callback.noteSigned(currDE);
				if (action==com.mindoo.domino.jna.NotesDatabase.SignCallback.Action.Stop) {
					return;
				}
			}
		}
	}

	/**
	 * This function creates a new full text index for a local database.<br>
	 * <br>
	 * Synchronous full text indexing of a remote database is not supported in the C API.
	 * Use {@link #ftIndexRequest()} to request an index update of a remote database.
	 * 
	 * @param options Indexing options. See {@link FTIndex}
	 * @return indexing statistics
	 */
	public NotesFTIndexStats FTIndex(EnumSet<FTIndex> options) {
		checkHandle();
		
		short optionsBitMask = FTIndex.toBitMask(options);
		
		NotesFTIndexStatsStruct retStats = NotesFTIndexStatsStruct.newInstance();
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().FTIndex(m_hDB64, optionsBitMask, null, retStats);
		}
		else {
			result = NotesNativeAPI32.get().FTIndex(m_hDB32, optionsBitMask, null, retStats);
		}
		NotesErrorUtils.checkResult(result);
		retStats.read();
		
		return new NotesFTIndexStats(retStats.DocsAdded, retStats.DocsUpdated, retStats.DocsDeleted, retStats.BytesIndexed);
	}
	
	/**
	 * Requests an asynchronous update of the full text index
	 */
	public void ftIndexRequest() {
		checkHandle();
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().ClientFTIndexRequest(m_hDB64);
		}
		else {
			result = NotesNativeAPI32.get().ClientFTIndexRequest(m_hDB32);
		}
		NotesErrorUtils.checkResult(result);
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
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().FTDeleteIndex(m_hDB64);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().FTDeleteIndex(m_hDB32);
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
		short isRemote;
		if (PlatformUtils.is64Bit()) {
			isRemote = NotesNativeAPI64.get().NSFDbIsRemote(m_hDB64);
		}
		else {
			isRemote = NotesNativeAPI32.get().NSFDbIsRemote(m_hDB32);
		}
		return isRemote==1;
	}
	
	/**
	 * Checks whether the database has been opened with full access {@link OpenDatabase#FULL_ACCESS}
	 * 
	 * @return true if full access
	 */
	public boolean hasFullAccess() {
		short hasFullAccess;
		if (PlatformUtils.is64Bit()) {
			hasFullAccess = NotesNativeAPI64.get().NSFDbHasFullAccess(m_hDB64);
		}
		else {
			hasFullAccess = NotesNativeAPI32.get().NSFDbHasFullAccess(m_hDB32);
		}
		return hasFullAccess==1; 
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
		
		if (PlatformUtils.is64Bit()) {
			NotesTimeDateStruct retTime = NotesTimeDateStruct.newInstance();
			short result = NotesNativeAPI64.get().FTGetLastIndexTime(m_hDB64, retTime);
			if (result == INotesErrorConstants.ERR_FT_NOT_INDEXED) {
				return null;
			}
			NotesErrorUtils.checkResult(result);
			retTime.read();
			
			NotesTimeDate retTimeWrap = new NotesTimeDate(retTime);
			return NotesDateTimeUtils.timeDateToCalendar(retTimeWrap);
		}
		else {
			NotesTimeDateStruct retTime = NotesTimeDateStruct.newInstance();
			short result = NotesNativeAPI32.get().FTGetLastIndexTime(m_hDB32, retTime);
			if (result == INotesErrorConstants.ERR_FT_NOT_INDEXED) {
				return null;
			}
			NotesErrorUtils.checkResult(result);
			retTime.read();
			
			NotesTimeDate retTimeWrap = new NotesTimeDate(retTime);
			return NotesDateTimeUtils.timeDateToCalendar(retTimeWrap);
		}
	}

	/**
	 * This routine returns the last time a database was full text indexed.
	 * It can also be used to determine if a database is full text indexed.
	 * If the database is not full text indexed, null is returned.
	 * 
	 * @return last index time or null if not indexed
	 */
	public NotesTimeDate getFTLastIndexTimeAsNotesTimeDate() {
		checkHandle();
		
		if (PlatformUtils.is64Bit()) {
			NotesTimeDateStruct retTime = NotesTimeDateStruct.newInstance();
			short result = NotesNativeAPI64.get().FTGetLastIndexTime(m_hDB64, retTime);
			if (result == INotesErrorConstants.ERR_FT_NOT_INDEXED) {
				return null;
			}
			NotesErrorUtils.checkResult(result);
			retTime.read();
			
			NotesTimeDate retTimeWrap = new NotesTimeDate(retTime);
			return retTimeWrap;
		}
		else {
			NotesTimeDateStruct retTime = NotesTimeDateStruct.newInstance();
			short result = NotesNativeAPI32.get().FTGetLastIndexTime(m_hDB32, retTime);
			if (result == INotesErrorConstants.ERR_FT_NOT_INDEXED) {
				return null;
			}
			NotesErrorUtils.checkResult(result);
			retTime.read();
			
			NotesTimeDate retTimeWrap = new NotesTimeDate(retTime);
			return retTimeWrap;
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
		
		ShortByReference retVersion = new ShortByReference();
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFDbGetBuildVersion(m_hDB64, retVersion);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFDbGetBuildVersion(m_hDB32, retVersion);
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

		NotesBuildVersionStruct retVersion = NotesBuildVersionStruct.newInstance();
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFDbGetMajMinVersion(m_hDB64, retVersion);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFDbGetMajMinVersion(m_hDB32, retVersion);
			NotesErrorUtils.checkResult(result);
		}
		retVersion.read();
		return new NotesBuildVersion(retVersion.MajorVersion,
				retVersion.MinorVersion, retVersion.QMRNumber, retVersion.QMUNumber,
				retVersion.HotfixNumber, retVersion.Flags, retVersion.FixpackNumber);
	}

	public static abstract class SearchCallback extends NotesSearch.SearchCallback {
		
	}
	
	/**
	 * This function scans all the notes in a database.<br>
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
	 * Specify {@link NotesConstants#NOTE_CLASS_DOCUMENT} to find documents.<br>
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
	 * @param noteClassMaskEnum bitmask of noteclasses to search
	 * @param since The date of the earliest modified note that is matched. The note's "Modified in this file" date is compared to this date. Specify NULL if you do not wish any filtering by date.
	 * @param callback callback to be called for every found note
	 * @return The ending (current) time/date of this search. Returned so that it can be used in a subsequent call to {@link #search(String, String, EnumSet, EnumSet, NotesTimeDate, SearchCallback)} as the "Since" argument.
	 * @throws FormulaCompilationError if formula syntax is invalid
	 */
	public NotesTimeDate search(final String formula, String viewTitle, final EnumSet<Search> searchFlags,
			EnumSet<NoteClass> noteClassMaskEnum, NotesTimeDate since, final SearchCallback callback) throws FormulaCompilationError {
		NotesTimeDate endTimeDate = NotesSearch.search(this, null, formula, viewTitle, searchFlags, noteClassMaskEnum, since, callback);
		return endTimeDate;
	}
	
	/**
	 * This function scans all the notes in a database or ID table.<br>
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
	 * Specify {@link NotesConstants#NOTE_CLASS_DOCUMENT} to find documents.<br>
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
	 * @param searchFilter optional search scope as {@link NotesIDTable} or null
	 * @param formula formula or null
	 * @param columnFormulas map with programmatic column names (key) and formulas (value) with keys sorted in column order or null to output all items; automatically uses {@link Search#NOITEMNAMES} and {@link Search#SUMMARY} search flag
	 * @param viewTitle optional view title that will be returned for "@ ViewTitle" within the formula or null
	 * @param searchFlags flags to control searching ({@link Search})
	 * @param noteClasses noteclasses to search
	 * @param since The date of the earliest modified note that is matched. The note's "Modified in this file" date is compared to this date. Specify NULL if you do not wish any filtering by date.
	 * @param callback callback to be called for every found note
	 * @return The ending (current) time/date of this search. Returned so that it can be used in a subsequent call to {@link #search(NotesIDTable, String, LinkedHashMap, String, EnumSet, EnumSet, NotesTimeDate, SearchCallback)} as the "Since" argument.
	 * @throws FormulaCompilationError if formula syntax is invalid
	 */
	public NotesTimeDate search(NotesIDTable searchFilter, final String formula,
			LinkedHashMap<String,String> columnFormulas, String viewTitle,
			final EnumSet<Search> searchFlags, EnumSet<NoteClass> noteClasses, NotesTimeDate since,
			final SearchCallback callback) throws FormulaCompilationError {
		
		NotesTimeDate endTimeDate = NotesSearch.search(this, searchFilter, formula, columnFormulas,
				viewTitle, searchFlags, noteClasses, since, callback);
		return endTimeDate;
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
	 * Specify {@link NotesConstants#NOTE_CLASS_DOCUMENT} to find documents.<br>
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
	 * @param fileTypeEnum filetypes to search
	 * @param since The date of the earliest modified note that is matched. The note's "Modified in this file" date is compared to this date. Specify NULL if you do not wish any filtering by date.
	 * @param callback callback to be called for every found note
	 * @return The ending (current) time/date of this search. Returned so that it can be used in a subsequent call to {@link #search(String, String, EnumSet, EnumSet, NotesTimeDate, SearchCallback)} as the "Since" argument.
	 * @throws FormulaCompilationError if formula syntax is invalid
	 */
	public NotesTimeDate searchFiles(final String formula, String viewTitle, final EnumSet<Search> searchFlags, EnumSet<FileType> fileTypeEnum, NotesTimeDate since, final SearchCallback callback) throws FormulaCompilationError {
		NotesTimeDate endTimeDate = NotesSearch.searchFiles(this, null, formula, viewTitle, searchFlags, fileTypeEnum, since, callback);
		return endTimeDate;
	}

	/**
	 * Data container that stores the lookup result for note info
	 * 
	 * @author Karsten Lehmann
	 */
	public static class NoteInfo {
		private int m_noteId;
		private int m_sequence;
		private NotesTimeDate m_sequenceTime;
		private String m_unid;
		private boolean m_isDeleted;
		private boolean m_notPresent;
		
		private NoteInfo(int noteId, String unid, NotesTimeDate sequenceTime, int sequence,
				boolean isDeleted, boolean notPresent) {
			
			m_noteId = noteId;
			m_unid = unid;
			m_sequenceTime = sequenceTime;
			m_sequence = sequence;
			m_isDeleted = isDeleted;
			m_notPresent = notPresent;
		}
		
		/**
		 * Returns the note id
		 * 
		 * @return note id or 0 if the note could not be found
		 */
		public int getNoteId() {
			return m_noteId;
		}
		
		/**
		 * Returns the sequence number
		 * 
		 * @return sequence number or 0 if the note could not be found
		 */
		public int getSequence() {
			return m_sequence;
		}
		
		/**
		 * Returns the sequence time ( = "Modified (initially)")
		 * 
		 * @return sequence time or null if the note could not be found
		 */
		public NotesTimeDate getSequenceTime() {
			return m_sequenceTime;
		}
		
		/**
		 * Returns the UNID as hex string
		 * 
		 * @return UNID or null if the note could not be found
		 */
		public String getUnid() {
			return m_unid;
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

		@Override
		public String toString() {
			return "NoteInfo [noteId=" + m_noteId + ", sequence=" + m_sequence + ", sequenceTime="
					+ m_sequenceTime + ", unid=" + m_unid + ", isDeleted=" + m_isDeleted + ", notPresent="
					+ m_notPresent + "]";
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
		
		private NoteInfoExt(int noteId, String unid, NotesTimeDate sequenceTime, int sequence,
				boolean isDeleted, boolean notPresent,
				NotesTimeDateStruct modified, short noteClass, NotesTimeDateStruct addedToFile, short responseCount,
				int parentNoteId) {
			
			super(noteId, unid, sequenceTime, sequence, isDeleted, notPresent);
			
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
		public NoteClass getNoteClass() {
			return NoteClass.toNoteClass((int) (m_noteClass & 0xffff));
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

		@Override
		public String toString() {
			return "NoteInfoExt [modified=" + m_modified + ", noteClass=" + m_noteClass + ", addedToFile="
					+ m_addedToFile + ", responseCount=" + m_responseCount + ", parentNoteId=" + m_parentNoteId
					+ "]";
		}
		
		
	}
	
	/**
	 * Convenience method to convert a single note unid to a note id. Currently only calls
	 * the bulk function {@link NotesDatabase#toNoteIds(String[], Map, Set)}.
	 * 
	 * @param unid UNID
	 * @return note id or 0 if not found
	 */
	public int toNoteId(String unid) {
		Map<String,Integer> retNoteIdsByUnid = new HashMap<String, Integer>(1);
		Set<String> retNoteUnidsNotFound = new HashSet<String>(1);
		toNoteIds(new String[] {unid}, retNoteIdsByUnid, retNoteUnidsNotFound);
		
		Integer noteId = retNoteIdsByUnid.get(unid);
		return noteId!=null ? noteId.intValue() : 0;
	}
	
	/**
	 * Convenience method to convert note unids to note ids.
	 * The method internally calls {@link NotesDatabase#getMultiNoteInfo(String[])}.
	 * 
	 * @param noteUnids note unids to look up
	 * @param retNoteIdsByUnid map is populated with found note ids
	 * @param retNoteUnidsNotFound set is populated with any note unid that could not be found; can be null
	 */
	public void toNoteIds(String[] noteUnids, Map<String,Integer> retNoteIdsByUnid, Set<String> retNoteUnidsNotFound) {
		NoteInfo[] infoArr = getMultiNoteInfo(noteUnids);
		for (int i=0; i<noteUnids.length; i++) {
			NoteInfo currInfo = infoArr[i];
			if (currInfo.exists()) {
				retNoteIdsByUnid.put(noteUnids[i], currInfo.getNoteId());
			}
			else {
				if (retNoteUnidsNotFound!=null)
					retNoteUnidsNotFound.add(noteUnids[i]);
			}
		}
	}
	
	/**
	 * Convenience method to convert a single note id to a UNID. Currently
	 * only calls the bulk function {@link NotesDatabase#toUnids(int[], Map, Set)}.
	 * 
	 * @param noteId note id to look up
	 * @return resolved UNID or null if not found
	 */
	public String toUnid(int noteId) {
		Map<Integer,String> retUnidsByNoteId = new HashMap<Integer, String>(1);
		Set<Integer> retNoteIdsNotFound = new HashSet<Integer>(1);
		toUnids(new int[] {noteId}, retUnidsByNoteId, retNoteIdsNotFound);
		return retUnidsByNoteId.get(noteId);
	}
	
	/**
	 * Convenience method to convert note ids to UNIDs.
	 * The method internally calls {@link NotesDatabase#getMultiNoteInfo(int[])}.
	 * 
	 * @param noteIds note ids to look up
	 * @param retUnidsByNoteId map is populated with found UNIDs
	 * @param retNoteIdsNotFound set is populated with any note id that could not be found
	 */
	public void toUnids(Collection<Integer> noteIds, Map<Integer,String> retUnidsByNoteId, Set<Integer> retNoteIdsNotFound) {
		toUnids(SetUtil.toPrimitiveArray(noteIds), retUnidsByNoteId, retNoteIdsNotFound);
	}

	/**
	 * Convenience method to convert note ids to UNIDs.
	 * The method internally calls {@link NotesDatabase#getMultiNoteInfo(int[])}.
	 * 
	 * @param noteIds note ids to look up
	 * @param retUnidsByNoteId map is populated with found UNIDs
	 * @param retNoteIdsNotFound set is populated with any note id that could not be found
	 */
	public void toUnids(int[] noteIds, Map<Integer,String> retUnidsByNoteId, Set<Integer> retNoteIdsNotFound) {
		NoteInfo[] infoArr = getMultiNoteInfo(noteIds);
		for (int i=0; i<noteIds.length; i++) {
			NoteInfo currInfo = infoArr[i];
			if (currInfo.exists()) {
				retUnidsByNoteId.put(noteIds[i], currInfo.getUnid());
			}
			else {
				retNoteIdsNotFound.add(noteIds[i]);
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

		NotesOriginatorIdStruct retNoteOID = NotesOriginatorIdStruct.newInstance();
		NotesTimeDateStruct retModified = NotesTimeDateStruct.newInstance();
		ShortByReference retNoteClass = new ShortByReference();
		NotesTimeDateStruct retAddedToFile = NotesTimeDateStruct.newInstance();
		ShortByReference retResponseCount = new ShortByReference();
		IntByReference retParentNoteID = new IntByReference();
		boolean isDeleted = false;
		//not sure if we can check this via error code:
		boolean notPresent = false;
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFDbGetNoteInfoExt(m_hDB64, noteId, retNoteOID, retModified, retNoteClass, retAddedToFile, retResponseCount, retParentNoteID);
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
			short result = NotesNativeAPI32.get().NSFDbGetNoteInfoExt(m_hDB32, noteId, retNoteOID, retModified, retNoteClass, retAddedToFile, retResponseCount, retParentNoteID);
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
		
		String unid = retNoteOID.getUNIDAsString();
		NotesTimeDate sequenceTime = new NotesTimeDate(retNoteOID.SequenceTime.Innards);
		int sequence = retNoteOID.Sequence;
		
		NoteInfoExt info = new NoteInfoExt(noteId, unid, sequenceTime, sequence,
				isDeleted, notPresent, retModified,
				retNoteClass.getValue(), retAddedToFile, retResponseCount.getValue(), retParentNoteID.getValue());
		
		return info;
	}
	
	/**
	 * This method can be used to get information for a number documents in a
	 * database from their note ids in a single call.<br>
	 * The data returned by this method is the note id, {@link NotesOriginatorId}, which contains
	 * the UNID of the document, the sequence number and the sequence time ("Modified initially" time).<br>
	 * <br>
	 * In addition, the method checks whether a document exists or has been deleted.
	 * 
	 * @param noteIds array of note ids
	 * @return lookup results, same size and order as <code>noteIds</code> array
	 * @throws IllegalArgumentException if note id array has too many entries (more than 65535)
	 */
	public NoteInfo[] getMultiNoteInfo(int[] noteIds) {
		checkHandle();
		
		int entrySize = 4 /* note id */ + NotesConstants.oidSize;
		//not more than 32767 entries and output buffer cannot exceed 64k
		final int ENTRIESBYCALL = Math.min(65535, 64000 / entrySize);

		if (noteIds.length < ENTRIESBYCALL)
			return _getMultiNoteInfo(noteIds);
		
		//work around C API limit of max 65535 entries per call
		NoteInfo[] noteInfos = new NoteInfo[noteIds.length];
		
		int startOffset = 0;
		
		while (startOffset < noteIds.length) {
			int endOffsetExclusive = Math.min(noteIds.length, startOffset + ENTRIESBYCALL);
			int[] currNoteIds = new int[endOffsetExclusive - startOffset];
			System.arraycopy(noteIds, startOffset, currNoteIds, 0, endOffsetExclusive - startOffset);
			
			NoteInfo[] currNoteInfos = _getMultiNoteInfo(currNoteIds);
			System.arraycopy(currNoteInfos, 0, noteInfos, startOffset, currNoteInfos.length);
			startOffset += ENTRIESBYCALL;
		}
		
		return noteInfos;
	}
	
	/**
	 * This method can be used to get information for a number documents in a
	 * database from their note ids in a single call.<br>
	 * The data returned by this method is the note id, {@link NotesOriginatorId}, which contains
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
	private NoteInfo[] _getMultiNoteInfo(int[] noteIds) {

		if (noteIds.length ==0) {
			return new NoteInfo[0];
		}
		
		if (noteIds.length > 65535) {
			throw new IllegalArgumentException("Max 65535 note ids are supported");
		}
		
		NoteInfo[] retNoteInfo;
		
		if (PlatformUtils.is64Bit()) {
			LongByReference retHandle = new LongByReference();
			short result = Mem64.OSMemAlloc((short) 0, noteIds.length * 4, retHandle);
			NotesErrorUtils.checkResult(result);

			boolean inMemHandleLocked = false;
			
			long retHandleLong = retHandle.getValue();
			try {
				Pointer inBufPtr = Mem64.OSLockObject(retHandleLong);
				inMemHandleLocked = true;
				
				Pointer currInBufPtr = inBufPtr;
				int offset = 0;
				
				for (int i=0; i<noteIds.length; i++) {
					currInBufPtr.setInt(0, noteIds[i]);
					offset += 4;
					currInBufPtr = inBufPtr.share(offset);
				}
				
				Mem64.OSUnlockObject(retHandleLong);
				inMemHandleLocked = false;
				
				IntByReference retSize = new IntByReference();
				LongByReference rethOutBuf = new LongByReference();
				short options = NotesConstants.fINFO_OID | NotesConstants.fINFO_ALLOW_HUGE | NotesConstants.fINFO_NOTEID;
				
				result = NotesNativeAPI64.get().NSFDbGetMultNoteInfo(m_hDB64, (short) (noteIds.length & 0xffff), options, retHandleLong, retSize, rethOutBuf);
				NotesErrorUtils.checkResult(result);

				long rethOutBufLong = rethOutBuf.getValue();
				if (rethOutBufLong==0)
					throw new IllegalStateException("Returned result handle is 0");
				
				//decode return buffer
				int entrySize = 4 /* note id */ + NotesConstants.oidSize;
				long retSizeLong = retSize.getValue();
				if (retSizeLong != noteIds.length*entrySize) {
					throw new IllegalStateException("Unexpected size of return data. Expected "+noteIds.length*entrySize+" bytes for data of "+noteIds.length+" ids, got "+retSizeLong+" bytes");
				}
				
				Pointer outBufPtr = Mem64.OSLockObject(rethOutBuf.getValue());
				try {
					retNoteInfo = decodeMultiNoteLookupData(noteIds.length, outBufPtr);
				}
				finally {
					Mem64.OSUnlockObject(rethOutBufLong);
					result = Mem64.OSMemFree(rethOutBufLong);
					NotesErrorUtils.checkResult(result);
				}
			}
			finally {
				if (inMemHandleLocked) {
					Mem64.OSUnlockObject(retHandleLong);
				}
				result = Mem64.OSMemFree(retHandleLong);
				NotesErrorUtils.checkResult(result);
			}
		}
		else {
			IntByReference retHandle = new IntByReference();
			short result = Mem32.OSMemAlloc((short) 0, noteIds.length * 4, retHandle);
			NotesErrorUtils.checkResult(result);

			boolean inMemHandleLocked = false;

			int retHandleInt = retHandle.getValue();
			try {
				Pointer inBufPtr = Mem32.OSLockObject(retHandleInt);
				inMemHandleLocked = true;
				
				Pointer currInBufPtr = inBufPtr;
				int offset = 0;
				
				for (int i=0; i<noteIds.length; i++) {
					currInBufPtr.setInt(0, noteIds[i]);
					offset += 4;
					currInBufPtr = inBufPtr.share(offset);
				}
				
				Mem32.OSUnlockObject(retHandleInt);
				inMemHandleLocked = false;
				
				IntByReference retSize = new IntByReference();
				IntByReference rethOutBuf = new IntByReference();
				short options = NotesConstants.fINFO_OID | NotesConstants.fINFO_ALLOW_HUGE | NotesConstants.fINFO_NOTEID;
				
				result = NotesNativeAPI32.get().NSFDbGetMultNoteInfo(m_hDB32, (short) (noteIds.length & 0xffff), options, retHandleInt, retSize, rethOutBuf);
				NotesErrorUtils.checkResult(result);

				int rethOutBufInt = rethOutBuf.getValue();
				if (rethOutBufInt==0)
					throw new IllegalStateException("Returned result handle is 0");

				//decode return buffer
				int entrySize = 4 /* note id */ + NotesConstants.oidSize;
				long retSizeLong = retSize.getValue();
				if (retSizeLong != noteIds.length*entrySize) {
					throw new IllegalStateException("Unexpected size of return data. Expected "+noteIds.length*entrySize+" bytes for data of "+noteIds.length+" ids, got "+retSizeLong+" bytes");
				}
				
				Pointer outBufPtr = Mem32.OSLockObject(rethOutBuf.getValue());
				try {
					retNoteInfo = decodeMultiNoteLookupData(noteIds.length, outBufPtr);
				}
				finally {
					Mem32.OSUnlockObject(rethOutBufInt);
					result = Mem32.OSMemFree(rethOutBufInt);
					NotesErrorUtils.checkResult(result);
				}
			}
			finally {
				if (inMemHandleLocked) {
					Mem32.OSUnlockObject(retHandleInt);
				}
				result = Mem32.OSMemFree(retHandleInt);
				NotesErrorUtils.checkResult(result);
			}
		}

		return retNoteInfo;
	}
	
	/**
	 * This method can be used to get information for a number documents in a
	 * database from their note unids in a single call.<br>
	 * The data returned by this method is the note id, {@link NotesOriginatorId}, which contains
	 * the UNID of the document, the sequence number and the sequence time ("Modified initially" time).<br>
	 * <br>
	 * In addition, the method checks whether a document exists or has been deleted.<br>
	 * 
	 * @param noteUNIDs array of note unids
	 * @return lookup results, same size and order as <code>noteUNIDs</code> array
	 * @throws IllegalArgumentException if note unid array has too many entries (more than 32767)
	 */
	public NoteInfo[] getMultiNoteInfo(String[] noteUNIDs) {
		checkHandle();

		int entrySize = 4 /* note id */ + NotesConstants.oidSize;
		//not more than 32767 entries and output buffer cannot exceed 64k
		final int ENTRIESBYCALL = Math.min(32767, 64000 / entrySize);
		
		if (noteUNIDs.length < ENTRIESBYCALL)
			return _getMultiNoteInfo(noteUNIDs);
		
		//work around C API limit of max 32767 entries per call
		NoteInfo[] noteInfos = new NoteInfo[noteUNIDs.length];
		
		int startOffset = 0;
		
		while (startOffset < noteUNIDs.length) {
			int endOffsetExclusive = Math.min(noteUNIDs.length, startOffset + ENTRIESBYCALL);
			String[] currNoteUNIDs = new String[endOffsetExclusive - startOffset];
			System.arraycopy(noteUNIDs, startOffset, currNoteUNIDs, 0, endOffsetExclusive - startOffset);
			
			NoteInfo[] currNoteInfos = _getMultiNoteInfo(currNoteUNIDs);
			System.arraycopy(currNoteInfos, 0, noteInfos, startOffset, currNoteInfos.length);
			startOffset += ENTRIESBYCALL;
		}
		
		return noteInfos;
	}
	
	/**
	 * This method can be used to get information for a number documents in a
	 * database from their note unids in a single call.<br>
	 * The data returned by this method is the note id, {@link NotesOriginatorId}, which contains
	 * the UNID of the document, the sequence number and the sequence time ("Modified initially" time).<br>
	 * <br>
	 * In addition, the method checks whether a document exists or has been deleted.<br>
	 * <br>
	 * Please note that the method can only handle max. 32767 note ids in one call.
	 * 
	 * @param noteUNIDs array of note unids
	 * @return lookup results, same size and order as <code>noteUNIDs</code> array
	 * @throws IllegalArgumentException if note unid array has too many entries (more than 32767)
	 */
	private NoteInfo[] _getMultiNoteInfo(String[] noteUNIDs) {
		if (noteUNIDs.length ==0) {
			return new NoteInfo[0];
		}
		
		if (noteUNIDs.length > 32767) {
			throw new IllegalArgumentException("Max 32767 note ids are supported");
		}
		
		NoteInfo[] retNoteInfo;
		
		if (PlatformUtils.is64Bit()) {
			LongByReference retHandle = new LongByReference();
			short result = Mem64.OSMemAlloc((short) 0, noteUNIDs.length * 16, retHandle);
			NotesErrorUtils.checkResult(result);

			boolean inMemHandleLocked = false;
			
			long retHandleLong = retHandle.getValue();
			try {
				Pointer inBufPtr = Mem64.OSLockObject(retHandleLong);
				inMemHandleLocked = true;
				
				Pointer currInBufPtr = inBufPtr;
				int offset = 0;
				
				for (int i=0; i<noteUNIDs.length; i++) {
					NotesStringUtils.unidToPointer(noteUNIDs[i], currInBufPtr);
					offset += 16;
					currInBufPtr = inBufPtr.share(offset);
				}
				
				Mem64.OSUnlockObject(retHandleLong);
				inMemHandleLocked = false;
				
				IntByReference retSize = new IntByReference();
				LongByReference rethOutBuf = new LongByReference();
				short options = NotesConstants.fINFO_OID | NotesConstants.fINFO_ALLOW_HUGE | NotesConstants.fINFO_NOTEID;
				
				result = NotesNativeAPI64.get().NSFDbGetMultNoteInfoByUNID(m_hDB64, (short) (noteUNIDs.length & 0xffff),
						options, retHandleLong, retSize, rethOutBuf);

				NotesErrorUtils.checkResult(result);

				long rethOutBufLong = rethOutBuf.getValue();
				
				//decode return buffer
				int entrySize = 4 /* note id */ + NotesConstants.oidSize;
				long retSizeLong = retSize.getValue();
				if (retSizeLong != noteUNIDs.length*entrySize) {
					throw new IllegalStateException("Unexpected size of return data. Expected "+noteUNIDs.length*entrySize+" bytes for data of "+noteUNIDs.length+" ids, got "+retSizeLong+" bytes");
				}
				
				Pointer outBufPtr = Mem64.OSLockObject(rethOutBuf.getValue());
				try {
					retNoteInfo = decodeMultiNoteLookupData(noteUNIDs.length, outBufPtr);
				}
				finally {
					Mem64.OSUnlockObject(rethOutBufLong);
					result = Mem64.OSMemFree(rethOutBufLong);
					NotesErrorUtils.checkResult(result);
				}
			}
			finally {
				if (inMemHandleLocked) {
					Mem64.OSUnlockObject(retHandleLong);
				}
				result = Mem64.OSMemFree(retHandleLong);
				NotesErrorUtils.checkResult(result);
			}
		}
		else {
			IntByReference retHandle = new IntByReference();
			short result = Mem32.OSMemAlloc((short) 0, noteUNIDs.length * 16, retHandle);
			NotesErrorUtils.checkResult(result);

			boolean inMemHandleLocked = false;

			int retHandleInt = retHandle.getValue();
			try {
				Pointer inBufPtr = Mem32.OSLockObject(retHandleInt);
				inMemHandleLocked = true;
				
				Pointer currInBufPtr = inBufPtr;
				int offset = 0;
				
				for (int i=0; i<noteUNIDs.length; i++) {
					NotesStringUtils.unidToPointer(noteUNIDs[i], currInBufPtr);
					offset += 16;
					currInBufPtr = inBufPtr.share(offset);
				}
				
				Mem32.OSUnlockObject(retHandleInt);
				inMemHandleLocked = false;
				
				IntByReference retSize = new IntByReference();
				IntByReference rethOutBuf = new IntByReference();
				short options = NotesConstants.fINFO_OID | NotesConstants.fINFO_ALLOW_HUGE | NotesConstants.fINFO_NOTEID;
				
				result = NotesNativeAPI32.get().NSFDbGetMultNoteInfoByUNID(m_hDB32, (short) (noteUNIDs.length & 0xffff),
						options, retHandleInt, retSize, rethOutBuf);
				NotesErrorUtils.checkResult(result);

				int rethOutBufInt = rethOutBuf.getValue();
				
				//decode return buffer
				int entrySize = 4 /* note id */ + NotesConstants.oidSize;
				long retSizeLong = retSize.getValue();
				if (retSizeLong != noteUNIDs.length*entrySize) {
					throw new IllegalStateException("Unexpected size of return data. Expected "+noteUNIDs.length*entrySize+" bytes for data of "+noteUNIDs.length+" ids, got "+retSizeLong+" bytes");
				}
				
				Pointer outBufPtr = Mem32.OSLockObject(rethOutBuf.getValue());
				try {
					retNoteInfo = decodeMultiNoteLookupData(noteUNIDs.length, outBufPtr);
				}
				finally {
					Mem32.OSUnlockObject(rethOutBufInt);
					result = Mem32.OSMemFree(rethOutBufInt);
					NotesErrorUtils.checkResult(result);
				}
			}
			finally {
				if (inMemHandleLocked) {
					Mem32.OSUnlockObject(retHandleInt);
				}
				result = Mem32.OSMemFree(retHandleInt);
				NotesErrorUtils.checkResult(result);
			}
		}

		return retNoteInfo;
	}

	/**
	 * Helper method to extract the return data of method {@link #getMultiNoteInfo(int[])} or {@link #getMultiNoteInfo(String[])}
	 * 
	 * @param nrOfElements number of list elements
	 * @param outBufPtr buffer pointer
	 * @return array of note info objects
	 */
	private NoteInfo[] decodeMultiNoteLookupData(int nrOfElements, Pointer outBufPtr) {
		NoteInfo[] retNoteInfo = new NoteInfo[nrOfElements];
		
		Pointer entryBufPtr = outBufPtr;
		
		for (int i=0; i<nrOfElements; i++) {
			int offsetInEntry = 0;
			
			int currNoteId = entryBufPtr.getInt(0);

			offsetInEntry += 4;

			Pointer fileTimeDatePtr = entryBufPtr.share(offsetInEntry);
			
			String unid = NotesStringUtils.pointerToUnid(fileTimeDatePtr);
			
			offsetInEntry += 8; //skip "file" field
			offsetInEntry += 8; // skip "note" field
			
			int sequence = entryBufPtr.getInt(offsetInEntry);

			offsetInEntry += 4;
			
			Pointer sequenceTimePtr = entryBufPtr.share(offsetInEntry);
			int[] sequenceTimeInnards = sequenceTimePtr.getIntArray(0, 2);
			NotesTimeDate sequenceTime = new NotesTimeDate(sequenceTimeInnards);
			
			offsetInEntry += 8;
			
			entryBufPtr = entryBufPtr.share(offsetInEntry);
			
			boolean isDeleted = (currNoteId & NotesConstants.NOTEID_RESERVED) == NotesConstants.NOTEID_RESERVED;
			boolean isNotPresent = currNoteId==0;
			retNoteInfo[i] = new NoteInfo(currNoteId, unid, sequenceTime, sequence, isDeleted, isNotPresent);
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
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFNoteHardDelete(m_hDB64, softDelNoteId, 0);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFNoteHardDelete(m_hDB32, softDelNoteId, 0);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function reads a "soft deleted" note into memory.<br>
	 * Its input is a database handle and a note ID within that database.<br>
	 * Use {@link NotesNote#update(Set)} to restore this "soft deleted" note.

	 * @param noteId The ID of the "soft deleted" note to open
	 * @return note
	 */
	public NotesNote openSoftDeletedNoteById(int noteId) {
		checkHandle();
		if (PlatformUtils.is64Bit()) {
			LongByReference rethNote = new LongByReference();
			short result = NotesNativeAPI64.get().NSFNoteOpenSoftDelete(m_hDB64, noteId, 0, rethNote);
			if ((result & NotesConstants.ERR_MASK)==1028) {
				return null;
			}
			NotesErrorUtils.checkResult(result);
			
			long hNote = rethNote.getValue();
			NotesNote note = new NotesNote(this, hNote);
			NotesGC.__objectCreated(NotesNote.class, note);
			
			return note;
		}
		else {
			IntByReference rethNote = new IntByReference();
			short result = NotesNativeAPI32.get().NSFNoteOpenSoftDelete(m_hDB32, noteId, 0, rethNote);
			if ((result & NotesConstants.ERR_MASK)==1028) {
				return null;
			}
			NotesErrorUtils.checkResult(result);
			
			int hNote = rethNote.getValue();
			NotesNote note = new NotesNote(this, hNote);
			NotesGC.__objectCreated(NotesNote.class, note);
			
			return note;
		}
	}

	private int toNoteOpenOptions(Set<OpenNote> flags) {
		int options = 0;
		if (flags.contains(OpenNote.SUMMARY)) {
			options = options | NotesConstants.OPEN_SUMMARY;
		}

		if (flags.contains(OpenNote.NOVERIFYDEFAULT)) {
			options = options | NotesConstants.OPEN_NOVERIFYDEFAULT;
		}

		if (flags.contains(OpenNote.EXPAND)) {
			options = options | NotesConstants.OPEN_EXPAND;
		}

		if (flags.contains(OpenNote.NOOBJECTS)) {
			options = options | NotesConstants.OPEN_NOOBJECTS;
		}

		if (flags.contains(OpenNote.SHARE)) {
			options = options | NotesConstants.OPEN_SHARE;
		}

		if (flags.contains(OpenNote.CANONICAL)) {
			options = options | NotesConstants.OPEN_CANONICAL;
		}
		
		if (flags.contains(OpenNote.MARK_READ)) {
			options = options | NotesConstants.OPEN_MARK_READ;
		}

		if (flags.contains(OpenNote.ABSTRACT)) {
			options = options | NotesConstants.OPEN_ABSTRACT;
		}
		
		if (flags.contains(OpenNote.RESPONSE_ID_TABLE)) {
			options = options | NotesConstants.OPEN_RESPONSE_ID_TABLE;
		}

		if (flags.contains(OpenNote.WITH_FOLDERS)) {
			options = options | NotesConstants.OPEN_WITH_FOLDERS;
		}

		if (flags.contains(OpenNote.CACHE)) {
			options = options | NotesConstants.OPEN_CACHE;
		}

		// we negated the following two OPEN_XXX constants, so we keep
		// the items in their native format if conversion is not explicitly requested
		
		if (!flags.contains(OpenNote.CONVERT_RFC822_TO_TEXT_AND_TIME)) {
			options = options | NotesConstants.OPEN_RAW_RFC822_TEXT;
		}

		if (!flags.contains(OpenNote.CONVERT_MIME_TO_RICHTEXT)) {
			options = options | NotesConstants.OPEN_RAW_MIME_PART;
		}

		return options;
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
	 * @return note or null if not found
	 */
	public NotesNote openNoteById(int noteId, Set<OpenNote> openFlags) {
		checkHandle();

		int openFlagsBitmask = toNoteOpenOptions(openFlags);
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethNote = new LongByReference();
			short result = NotesNativeAPI64.get().NSFNoteOpenExt(m_hDB64, noteId, openFlagsBitmask, rethNote);
			if ((result & NotesConstants.ERR_MASK)==1028) {
				return null;
			}
			NotesErrorUtils.checkResult(result);
			
			long hNote = rethNote.getValue();
			NotesNote note = new NotesNote(this, hNote);
			NotesGC.__objectCreated(NotesNote.class, note);
			
			return note;
		}
		else {
			IntByReference rethNote = new IntByReference();
			short result = NotesNativeAPI32.get().NSFNoteOpenExt(m_hDB32, noteId, openFlagsBitmask, rethNote);
			if ((result & NotesConstants.ERR_MASK)==1028) {
				return null;
			}
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
	 * a handle to the in-memory copy.

	 * @param unid UNID
	 * @param openFlags open flags
	 * @return note or null if not found
	 */
	public NotesNote openNoteByUnid(String unid, EnumSet<OpenNote> openFlags) {
		checkHandle();

		int openFlagsBitmask = toNoteOpenOptions(openFlags);
		NotesUniversalNoteIdStruct unidObj = NotesUniversalNoteIdStruct.fromString(unid);
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethNote = new LongByReference();
			short result = NotesNativeAPI64.get().NSFNoteOpenByUNIDExtended(m_hDB64, unidObj, openFlagsBitmask, rethNote);
			if ((result & NotesConstants.ERR_MASK)==1028) {
				return null;
			}
			NotesErrorUtils.checkResult(result);
			
			long hNote = rethNote.getValue();
			NotesNote note = new NotesNote(this, hNote);
			NotesGC.__objectCreated(NotesNote.class, note);
			
			return note;
		}
		else {
			IntByReference rethNote = new IntByReference();
			short result = NotesNativeAPI32.get().NSFNoteOpenByUNIDExtended(m_hDB32, unidObj, openFlagsBitmask, rethNote);
			if ((result & NotesConstants.ERR_MASK)==1028) {
				return null;
			}
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
		
		if (PlatformUtils.is64Bit()) {
			LongByReference retNoteHandle = new LongByReference();
			short result = NotesNativeAPI64.get().NSFNoteCreate(m_hDB64, retNoteHandle);
			NotesErrorUtils.checkResult(result);
			NotesNote note = new NotesNote(this, retNoteHandle.getValue());
			NotesGC.__objectCreated(NotesNote.class, note);
			return note;
		}
		else {
			IntByReference retNoteHandle = new IntByReference();
			short result = NotesNativeAPI32.get().NSFNoteCreate(m_hDB32, retNoteHandle);
			NotesErrorUtils.checkResult(result);
			NotesNote note = new NotesNote(this, retNoteHandle.getValue());
			NotesGC.__objectCreated(NotesNote.class, note);
			return note;
		}
	}

	/**
	 * Creates a new in-memory ghost note. A ghost note does not appear in
	 * any view or search.
	 * 
	 * @return ghost note
	 */
	public NotesNote createGhostNote() {
		NotesNote note = createNote();
		note.setGhost(true);
		return note;
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
		Memory dbNameOldLMBCS = NotesStringUtils.toLMBCS(dbNameOld, true);
		Memory dbNameNewLMBCS = NotesStringUtils.toLMBCS(dbNameNew, true);

		short result = NotesNativeAPI.get().NSFDbRename(dbNameOldLMBCS, dbNameNewLMBCS);

		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Gets the value of a database option
	 * 
	 * @param option set {@link DatabaseOption}
	 * @return true if the option is enabled, false if the option is disabled
	 */
	public boolean getOption(DatabaseOption option) {
		checkHandle();
		
		Memory retDbOptions = new Memory(4 * 4); //DWORD[4]
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFDbGetOptionsExt(m_hDB64, retDbOptions);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFDbGetOptionsExt(m_hDB32, retDbOptions);
			NotesErrorUtils.checkResult(result);
		}
		byte[] dbOptionsArr = retDbOptions.getByteArray(0, 4 * 4);

		int optionBit = option.getValue();
		int byteOffsetWithBit = optionBit / 8;
		byte byteValueWithBit = dbOptionsArr[byteOffsetWithBit];
		int bitToCheck = (int) Math.pow(2, optionBit % 8);
		
		boolean enabled = (byteValueWithBit & bitToCheck) == bitToCheck;
		return enabled;
	}
	
	/**
	 * Returns the {@link DatabaseOption} values for the database
	 * 
	 * @return options
	 */
	public Set<DatabaseOption> getOptions() {
		checkHandle();
		
		DisposableMemory retDbOptions = new DisposableMemory(4 * 4); //DWORD[4]
		try {
			if (PlatformUtils.is64Bit()) {
				short result = NotesNativeAPI64.get().NSFDbGetOptionsExt(m_hDB64, retDbOptions);
				NotesErrorUtils.checkResult(result);
			}
			else {
				short result = NotesNativeAPI32.get().NSFDbGetOptionsExt(m_hDB32, retDbOptions);
				NotesErrorUtils.checkResult(result);
			}
			byte[] dbOptionsArr = retDbOptions.getByteArray(0, 4 * 4);

			Set<DatabaseOption> dbOptions = EnumSet.noneOf(DatabaseOption.class);
			
			for (DatabaseOption currOpt : DatabaseOption.values()) {
				int optionBit = currOpt.getValue();
				int byteOffsetWithBit = optionBit / 8;
				byte byteValueWithBit = dbOptionsArr[byteOffsetWithBit];
				int bitToCheck = (int) Math.pow(2, optionBit % 8);
				
				boolean enabled = (byteValueWithBit & bitToCheck) == bitToCheck;
				if (enabled) {
					dbOptions.add(currOpt);
				}
			}
			
			return dbOptions;
		}
		finally {
			retDbOptions.dispose();
		}
	}
	
	/**
	 * Sets the value of a database option
	 * 
	 * @param option see {@link DatabaseOption}
	 * @param flag true to set the option
	 */
	public void setOption(DatabaseOption option, boolean flag) {
		checkHandle();
		
		int optionBit = option.getValue();
		int byteOffsetWithBit = optionBit / 8;
		int bitToCheck = (int) Math.pow(2, optionBit % 8);

		byte[] optionsWithBitSetArr = new byte[4*4];
		optionsWithBitSetArr[byteOffsetWithBit] = (byte) (bitToCheck & 0xff);
		
		Memory dbOptionsWithBitSetMem = new Memory(4 * 4);
		dbOptionsWithBitSetMem.write(0, optionsWithBitSetArr, 0, 4*4);
		
		if (PlatformUtils.is64Bit()) {
			short result;
			if (flag) {
				//use dbOptionsMem both for the new value and for the bitmask, since the new value is 1
				result = NotesNativeAPI64.get().NSFDbSetOptionsExt(m_hDB64, dbOptionsWithBitSetMem, dbOptionsWithBitSetMem);
			}
			else {
				Memory nullBytesMem = new Memory(4 * 4);
				nullBytesMem.clear();
				result = NotesNativeAPI64.get().NSFDbSetOptionsExt(m_hDB64, nullBytesMem, dbOptionsWithBitSetMem);
			}
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result;
			if (flag) {
				result = NotesNativeAPI32.get().NSFDbSetOptionsExt(m_hDB32, dbOptionsWithBitSetMem, dbOptionsWithBitSetMem);
			}
			else {
				Memory nullBytesMem = new Memory(4 * 4);
				nullBytesMem.clear();
				result = NotesNativeAPI32.get().NSFDbSetOptionsExt(m_hDB32, nullBytesMem, dbOptionsWithBitSetMem);
			}
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * Hides the design of this database
	 */
	public void hideDesign() {
		checkHandle();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFHideDesign(m_hDB64, m_hDB64, 0, 0);
		}
		else {
			result = NotesNativeAPI32.get().NSFHideDesign(m_hDB32, m_hDB32, 0, 0);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Checks if the database design is hidden
	 * 
	 * @return true if hidden
	 */
	public boolean isDesignHidden() {
		return (getReplicaInfo().getFlags() & 0x0020) == 0x0020;
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
	public void getNotes(final int[] noteIds, EnumSet<OpenNote>[] noteOpenFlags, int[] sinceSeqNum,
			final EnumSet<GetNotes> controlFlags, final NotesDatabase objectDb,
			final IGetNotesCallback getNotesCallback, final INoteOpenCallback noteOpenCallback,
			final IObjectAllocCallback objectAllocCallback, final IObjectWriteCallback objectWriteCallback,
			NotesTimeDate folderSinceTime, final IFolderAddCallback folderAddCallback) {
		
		checkHandle();

		if (noteIds.length==0)
			return;
		
		if (noteIds.length != noteOpenFlags.length) {
			throw new NotesError(0, "Size of note open flags array does not match note ids array ("+noteOpenFlags.length+"!="+noteIds.length+")");
		}
		if (noteIds.length != sinceSeqNum.length) {
			throw new NotesError(0, "Size of sinceSeqNum array does not match note ids array ("+sinceSeqNum.length+"!="+noteIds.length+")");
		}
		
		final NotesTimeDateStruct folderSinceTimeStruct = folderSinceTime==null ? null : NotesTimeDateStruct.newInstance(folderSinceTime.getInnards());
		
		final Memory arrNoteIdsMem = new Memory(4 * noteIds.length);
		for (int i=0; i<noteIds.length; i++) {
			arrNoteIdsMem.setInt(4*i, noteIds[i]);
		}
		final Memory arrNoteOpenFlagsMem = new Memory(4 * noteOpenFlags.length);
		for (int i=0; i<noteOpenFlags.length; i++) {
			arrNoteOpenFlagsMem.setInt(4*i, toNoteOpenOptions(noteOpenFlags[i]));
		}
		final Memory arrSinceSeqNumMem = new Memory(4 * sinceSeqNum.length);
		for (int i=0; i<sinceSeqNum.length; i++) {
			arrSinceSeqNumMem.setInt(4*i, sinceSeqNum[i]);
		}
		
		final Throwable[] exception = new Throwable[1];
		final NotesCallbacks.NSFGetNotesCallback cGetNotesCallback;
		
		if (getNotesCallback!=null) {
			if (PlatformUtils.isWin32()) {
				cGetNotesCallback = new Win32NotesCallbacks.NSFGetNotesCallbackWin32() {

					@Override
					public short invoke(Pointer param, int totalSizeLow, int totalSizeHigh) {
						try {
							getNotesCallback.gettingNotes(totalSizeLow, totalSizeHigh);
							return 0;
						}
						catch (RuntimeException e) {
							exception[0] = e;
							return INotesErrorConstants.ERR_CANCEL;
						}
						catch (Throwable t) {
							exception[0] = t;
							return INotesErrorConstants.ERR_CANCEL;
						}
					}};
			}
			else {
				cGetNotesCallback = new NotesCallbacks.NSFGetNotesCallback() {

					@Override
					public short invoke(Pointer param, int totalSizeLow, int totalSizeHigh) {
						try {
							getNotesCallback.gettingNotes(totalSizeLow, totalSizeHigh);
							return 0;
						}
						catch (RuntimeException e) {
							exception[0] = e;
							return INotesErrorConstants.ERR_CANCEL;
						}
						catch (Throwable t) {
							exception[0] = t;
							return INotesErrorConstants.ERR_CANCEL;
						}
					}};
			}
		}
		else {
			cGetNotesCallback=null;
		}
		
		final NotesCallbacks.NSFFolderAddCallback cFolderAddCallback;
		
		if (folderAddCallback!=null) {
			if (PlatformUtils.isWin32()) {
				cFolderAddCallback = new Win32NotesCallbacks.NSFFolderAddCallbackWin32() {

					@Override
					public short invoke(Pointer param, NotesUniversalNoteIdStruct noteUNID, int opBlock, int opBlockSize) {
						try {
							folderAddCallback.addedToFolder(noteUNID==null ? null : noteUNID.toString());
							return 0;
						}
						catch (RuntimeException e) {
							exception[0] = e;
							return INotesErrorConstants.ERR_CANCEL;
						}
						catch (Throwable t) {
							exception[0] = t;
							return INotesErrorConstants.ERR_CANCEL;
						}
					}
				};
			}
			else {
				cFolderAddCallback = new NotesCallbacks.NSFFolderAddCallback() {

					@Override
					public short invoke(Pointer param, NotesUniversalNoteIdStruct noteUNID, int opBlock, int opBlockSize) {
						try {
							folderAddCallback.addedToFolder(noteUNID==null ? null : noteUNID.toString());
							return 0;
						}
						catch (RuntimeException e) {
							exception[0] = e;
							return INotesErrorConstants.ERR_CANCEL;
						}
						catch (Throwable t) {
							exception[0] = t;
							return INotesErrorConstants.ERR_CANCEL;
						}
					}
				};
			}
		}
		else {
			cFolderAddCallback=null;
		}
		
		if (PlatformUtils.is64Bit()) {
			final NotesCallbacks.b64_NSFNoteOpenCallback cNoteOpenCallback;
			final NotesCallbacks.b64_NSFObjectAllocCallback cObjectAllocCallback;
			final NotesCallbacks.b64_NSFObjectWriteCallback cObjectWriteCallback;
			
			if (noteOpenCallback!=null) {
				cNoteOpenCallback = new NotesCallbacks.b64_NSFNoteOpenCallback() {

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
							return INotesErrorConstants.ERR_CANCEL;
						}
						catch (Exception e) {
							exception[0] = e;
							return INotesErrorConstants.ERR_CANCEL;
						}
						finally {
							NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
						}
					}
				};					
			}
			else {
				cNoteOpenCallback=null;
			}
			
			if (objectAllocCallback!=null) {
				cObjectAllocCallback = new NotesCallbacks.b64_NSFObjectAllocCallback() {

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
							return INotesErrorConstants.ERR_CANCEL;
						}
						catch (Exception e) {
							exception[0] = e;
							return INotesErrorConstants.ERR_CANCEL;
						}
						finally {
							NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
						}
					}
				};
			}
			else {
				cObjectAllocCallback=null;
			}
			
			if (objectWriteCallback!=null) {
				cObjectWriteCallback = new NotesCallbacks.b64_NSFObjectWriteCallback() {

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
							return INotesErrorConstants.ERR_CANCEL;
						}
						catch (Exception e) {
							exception[0] = e;
							return INotesErrorConstants.ERR_CANCEL;
						}
						finally {
							NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
						}
					}
				};
			}
			else {
				cObjectWriteCallback=null;
			}
			
			short result;
			try {
				//AccessController call required to prevent SecurityException when running in XPages
				result = AccessController.doPrivileged(new PrivilegedExceptionAction<Short>() {

					@Override
					public Short run() throws Exception {
						return NotesNativeAPI64.get().NSFDbGetNotes(m_hDB64, noteIds.length, arrNoteIdsMem, arrNoteOpenFlagsMem,
								arrSinceSeqNumMem, GetNotes.toBitMask(controlFlags), objectDb==null ? 0 : objectDb.getHandle64(),
										null, cGetNotesCallback, cNoteOpenCallback, cObjectAllocCallback, cObjectWriteCallback,
										folderSinceTimeStruct, cFolderAddCallback);
					}
				});
			} catch (PrivilegedActionException e) {
				if (e.getCause() instanceof RuntimeException) 
					throw (RuntimeException) e.getCause();
				else
					throw new NotesError(0, "Error getting notes from database", e);
			}
			
			if (exception[0]!=null) {
				throw new NotesError(0, "Error reading notes", exception[0]);
			}
			NotesErrorUtils.checkResult(result);
		}
		else {
			final NotesCallbacks.b32_NSFNoteOpenCallback cNoteOpenCallback;
			final NotesCallbacks.b32_NSFObjectAllocCallback cObjectAllocCallback;
			final NotesCallbacks.b32_NSFObjectWriteCallback cObjectWriteCallback;
			
			if (noteOpenCallback!=null) {
				if (PlatformUtils.isWin32()) {
					cNoteOpenCallback = new Win32NotesCallbacks.NSFNoteOpenCallbackWin32() {

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
								return INotesErrorConstants.ERR_CANCEL;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_CANCEL;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
						
					};
				}
				else {
					cNoteOpenCallback = new NotesCallbacks.b32_NSFNoteOpenCallback() {

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
								return INotesErrorConstants.ERR_CANCEL;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_CANCEL;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
						
					};
				}
			}
			else {
				cNoteOpenCallback=null;
			}
			
			if (objectAllocCallback!=null) {
				if (PlatformUtils.isWin32()) {
					cObjectAllocCallback = new Win32NotesCallbacks.NSFObjectAllocCallbackWin32() {

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
								return INotesErrorConstants.ERR_CANCEL;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_CANCEL;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
					};
				}
				else {
					cObjectAllocCallback = new NotesCallbacks.b32_NSFObjectAllocCallback() {

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
								return INotesErrorConstants.ERR_CANCEL;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_CANCEL;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
					};
				}
			}
			else {
				cObjectAllocCallback=null;
			}
			
			if (objectWriteCallback!=null) {
				if (PlatformUtils.isWin32()) {
					cObjectWriteCallback = new Win32NotesCallbacks.NSFObjectWriteCallbackWin32() {

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
								return INotesErrorConstants.ERR_CANCEL;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_CANCEL;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
					};
				}
				else {
					cObjectWriteCallback = new NotesCallbacks.b32_NSFObjectWriteCallback() {

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
								return INotesErrorConstants.ERR_CANCEL;
							}
							catch (Exception e) {
								exception[0] = e;
								return INotesErrorConstants.ERR_CANCEL;
							}
							finally {
								NotesGC.__objectBeeingBeRecycled(NotesNote.class, note);
							}
						}
					};
				}
			}
			else {
				cObjectWriteCallback=null;
			}
			
			short result;
			try {
				//AccessController call required to prevent SecurityException when running in XPages
				result = AccessController.doPrivileged(new PrivilegedExceptionAction<Short>() {

					@Override
					public Short run() throws Exception {
						return NotesNativeAPI32.get().NSFDbGetNotes(m_hDB32, noteIds.length, arrNoteIdsMem, arrNoteOpenFlagsMem,
								arrSinceSeqNumMem, GetNotes.toBitMask(controlFlags), objectDb==null ? 0 : objectDb.getHandle32(),
										null, cGetNotesCallback, cNoteOpenCallback, cObjectAllocCallback, cObjectWriteCallback,
										folderSinceTimeStruct, cFolderAddCallback);
					}
				});
			} catch (PrivilegedActionException e) {
				if (e.getCause() instanceof RuntimeException) 
					throw (RuntimeException) e.getCause();
				else
					throw new NotesError(0, "Error getting notes from database", e);
			}
			if (exception[0]!=null) {
				throw new NotesError(0, "Error reading notes", exception[0]);
			}
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * Convenience method that calls {@link #generateOID()} and returns the UNID part
	 * of the generated {@link NotesOriginatorId}
	 * 
	 * @return new UNID
	 */
	public String generateUNID() {
		return generateOID().getUNIDAsString();
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

		NotesOriginatorIdStruct retOIDStruct = NotesOriginatorIdStruct.newInstance();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbGenerateOID(m_hDB64, retOIDStruct);
		}
		else {
			result = NotesNativeAPI32.get().NSFDbGenerateOID(m_hDB32, retOIDStruct);
		}
		NotesErrorUtils.checkResult(result);

		retOIDStruct.read();
		
		return new NotesOriginatorId(retOIDStruct);
	}
	
	/**
	 * This function gets the level of database access granted to the username that opened the database.
	 * 
	 * @return access level and flags
	 */
	public AccessInfoAndFlags getAccessInfoAndFlags() {
		checkHandle();
		
		ShortByReference retAccessLevel = new ShortByReference();
		ShortByReference retAccessFlag = new ShortByReference();
		
		if (PlatformUtils.is64Bit()) {
			NotesNativeAPI64.get().NSFDbAccessGet(m_hDB64, retAccessLevel, retAccessFlag);
		}
		else {
			NotesNativeAPI32.get().NSFDbAccessGet(m_hDB32, retAccessLevel, retAccessFlag);
		}
		
		int iAccessLevel = retAccessLevel.getValue();
		AclLevel retLevel = AclLevel.toLevel(iAccessLevel);
		
		int iAccessFlag = (int) (retAccessFlag.getValue() & 0xffff);
		EnumSet<AclFlag> retFlags = EnumSet.noneOf(AclFlag.class);
		for (AclFlag currFlag : AclFlag.values()) {
			if ((iAccessFlag & currFlag.getValue()) == currFlag.getValue()) {
				retFlags.add(currFlag);
			}
		}
		
		return new AccessInfoAndFlags(retLevel, retFlags);
	}
	
	/**
	 * Container class for the current user's access level and flags to this database
	 * 
	 * @author Karsten Lehmann
	 */
	public static class AccessInfoAndFlags {
		private AclLevel m_aclLevel;
		private EnumSet<AclFlag> m_aclFlags;
		
		private AccessInfoAndFlags(AclLevel aclLevel, EnumSet<AclFlag> aclFlags) {
			m_aclLevel = aclLevel;
			m_aclFlags = aclFlags;
		}
		
		public AclLevel getAclLevel() {
			return m_aclLevel;
		}
		
		public EnumSet<AclFlag> getAclFlags() {
			return m_aclFlags;
		}
	}
	
	/**
	 * Replicates this Domino database with a specified server.<br>
	 * <br>
	 * Replication can be performed in either direction or both directions (push, pull, or both).<br>
	 * <br>
	 * <b>Please note:<br>
	 * Run this method inside {@link SignalHandlerUtil#runInterruptable(java.util.concurrent.Callable, com.mindoo.domino.jna.utils.SignalHandlerUtil.IBreakHandler)}
	 * to be able to cancel the process and inside {@link SignalHandlerUtil#runWithProgress(java.util.concurrent.Callable, com.mindoo.domino.jna.utils.SignalHandlerUtil.IProgressListener)}
	 * to get progress info.</b>
	 * 
	 * @param serverName destination server (either abbreviated or canonical format)
	 * @param options replication options
	 * @param timeLimitMin If non-zero, number of minutes replication is allowed to execute before cancellation. If not specified, no limit is imposed
	 * @return replication stats
	 */
	public NotesReplicationStats replicateWithServer(String serverName, EnumSet<ReplicateOption> options, int timeLimitMin) {
		String dbPathWithServer;
		
		String server = getServer();
		if (!StringUtil.isEmpty(server)) {
			dbPathWithServer = server + "!!" + getRelativeFilePath();
		}
		else {
			dbPathWithServer = getAbsoluteFilePathOnLocal();
		}
		return replicateDbsWithServer(serverName, options, Arrays.asList(dbPathWithServer), timeLimitMin);
	}
	
	/**
	 * This routine replicates Domino database files on the local system with a specified server.<br>
	 * <br>
	 * Either all common files can be replicated or a specified list of files can be replicated.<br>
	 * <br>
	 * Replication can be performed in either direction or both directions (push, pull, or both).<br>
	 * <br>
	 * <b>Please note:<br>
	 * Run this method inside {@link SignalHandlerUtil#runInterruptable(java.util.concurrent.Callable, com.mindoo.domino.jna.utils.SignalHandlerUtil.IBreakHandler)}
	 * to be able to cancel the process and inside {@link SignalHandlerUtil#runWithProgress(java.util.concurrent.Callable, com.mindoo.domino.jna.utils.SignalHandlerUtil.IProgressListener)}
	 * to get progress info.</b>
	 * 
	 * @param serverName destination server (either abbreviated or canonical format)
	 * @param options replication options
	 * @param fileList list of files to replicate, use server!!filepath format to specify databases on other servers
	 * @param timeLimitMin If non-zero, number of minutes replication is allowed to execute before cancellation. If not specified, no limit is imposed
	 * @return replication stats
	 */
	public static NotesReplicationStats replicateDbsWithServer(String serverName, EnumSet<ReplicateOption> options, List<String> fileList,
			int timeLimitMin) {
		
		Memory serverNameMem = NotesStringUtils.toLMBCS(NotesNamingUtils.toAbbreviatedName(serverName), true);

		ReplServStatsStruct retStatsStruct = ReplServStatsStruct.newInstance();
		ReplExtensionsStruct extendedOptions = ReplExtensionsStruct.newInstance();
		extendedOptions.Size = 2 + 2;
		extendedOptions.TimeLimit = (short) (timeLimitMin & 0xffff);
		extendedOptions.write();
		
		short numFiles = 0;
		Memory fileListMem = null;
		if (fileList!=null && !fileList.isEmpty()) {
			if (fileList.size() > 65535)
				throw new IllegalArgumentException("Number of files exceeds max size (65535)");
			numFiles = (short) (fileList.size() & 0xffff);
			ByteArrayOutputStream bOut = new ByteArrayOutputStream();
			for (String currFileName : fileList) {
				if (currFileName.length() > 0) {
					Memory currFileNameMem = NotesStringUtils.toLMBCS(currFileName, true);
					try {
						bOut.write(currFileNameMem.getByteArray(0, (int) currFileNameMem.size()));
					} catch (IOException e) {
						throw new NotesError(0, "Error writing file list to memory", e);
					}
				}
			}
			fileListMem = new Memory(bOut.size());
			byte[] bOutArr = bOut.toByteArray();
			fileListMem.write(0, bOutArr, 0, (int) bOutArr.length); 
		}
		
		int optionsInt = ReplicateOption.toBitMaskInt(options);
		short result = NotesNativeAPI.get().ReplicateWithServerExt(null, serverNameMem, optionsInt, numFiles,
				fileListMem, extendedOptions, retStatsStruct);
		NotesErrorUtils.checkResult(result);
		
		retStatsStruct.read();
		NotesReplicationStats retStats = new NotesReplicationStats(retStatsStruct);
		return retStats;
	}

	/**
	 * This function reopens this database again, return a database that exists in the caller's address space.<br>
	 * <br>
	 * This function allows a task (for instance, an API program that is an OLE server) to access a database that
	 * was opened by another task (for instance, Notes working as an OLE client).<br>
	 * <br>
	 * Also, this function allows one thread of a multithreaded API program to access a database that was
	 * already opened by a different thread.<br>
	 * <br>
	 * To avoid memory errors, programs should not use database handles from outside the program's address
	 * space for database I/O.
	 * 
	 * @return reopened database
	 */
	public NotesDatabase reopenDatabase() {
		if (PlatformUtils.is64Bit()) {
			if (m_hDB64==0) {
				throw new NotesError(0, "Database already recycled");
			}
		}
		else {
			if (m_hDB32==0) {
				throw new NotesError(0, "Database already recycled");
			}
		}

		NotesNamesList namesListForClone = NotesNamingUtils.writeNewNamesList(m_namesStringList);
		NotesNamingUtils.setPrivileges(namesListForClone, m_namesListPrivileges);
		
		NotesDatabase dbNew;
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference retDbHandle = new LongByReference();
			result = NotesNativeAPI64.get().NSFDbReopen(m_hDB64, retDbHandle);
			NotesErrorUtils.checkResult(result);
			
			long newDbHandle = retDbHandle.getValue();
			dbNew = new NotesDatabase(newDbHandle, m_asUserCanonical, namesListForClone);
		}
		else {
			IntByReference retDbHandle = new IntByReference();
			result = NotesNativeAPI32.get().NSFDbReopen(m_hDB32, retDbHandle);
			NotesErrorUtils.checkResult(result);
			
			int newDbHandle = retDbHandle.getValue();
			dbNew = new NotesDatabase(newDbHandle, m_asUserCanonical, namesListForClone);
		}
		NotesGC.__objectCreated(NotesDatabase.class, dbNew);
		return dbNew;
	}
	
	/**
	 * Returns a unique 32-bit identifier for a database that is valid as long as any handle
	 * to the database remains open.<br>
	 * The same identifier will be returned for all handles that refer to the same database.<br>
	 * In particular, if {@link #reopenDatabase()} is called, a new handle will be created for the database,
	 * but this identifer will remain the same, providing a simple and efficient way to
	 * determine whether or not two database handles refer to the same database.<br>
	 * <br>
	 * After all handles to the database have been closed and the database is opened,
	 * this function may or may not return a different database identifier.
	 * 
	 * @return id
	 */
	public int getOpenDatabaseId() {
		if (m_openDatabaseId==null) {
			checkHandle();
			
			if (PlatformUtils.is64Bit()) {
				m_openDatabaseId = NotesNativeAPI64.get().NSFDbGetOpenDatabaseID(m_hDB64);
			}
			else {
				m_openDatabaseId = NotesNativeAPI32.get().NSFDbGetOpenDatabaseID(m_hDB32);
			}
		}
		return m_openDatabaseId;
	}

	/**
	 * Constructs a network path of a database (server!!path with proper encoding)
	 * 
	 * @param server server or null
	 * @param filePath filepath
	 * @return LMBCS encoded path
	 */
	private static Memory constructNetPath(String server, String filePath) {
		if (server==null)
			server = "";
		if (filePath==null)
			throw new NullPointerException("filePath is null");

		server = NotesNamingUtils.toCanonicalName(server);
		
		Memory dbServerLMBCS = NotesStringUtils.toLMBCS(server, true);
		Memory dbFilePathLMBCS = NotesStringUtils.toLMBCS(filePath, true);
		Memory retFullNetPath = new Memory(NotesConstants.MAXPATH);

		short result = NotesNativeAPI.get().OSPathNetConstruct(null, dbServerLMBCS, dbFilePathLMBCS, retFullNetPath);
		NotesErrorUtils.checkResult(result);

		//reduce length of retDbPathName
		int newLength = 0;
		for (int i=0; i<retFullNetPath.size(); i++) {
			byte b = retFullNetPath.getByte(i);
			if (b==0) {
				newLength = i;
				break;
			}
		}
		byte[] retFullNetPathArr = retFullNetPath.getByteArray(0, newLength);
		
		Memory reducedFullNetPathMem = new Memory(newLength+1);
		reducedFullNetPathMem.write(0, retFullNetPathArr, 0, retFullNetPathArr.length);
		reducedFullNetPathMem.setByte(newLength, (byte) 0);
		return reducedFullNetPathMem;
	}
	
	/**
	 * This function marks a cluster database in service by clearing the database option flag
	 * {@link DatabaseOption#OUT_OF_SERVICE}, if set.<br>
	 * <br>
	 * When a call to {@link #markInService(String, String)} is successful, the Cluster Manager enables
	 * users to access the database again by removing the "out of service" access restriction.<br>
	 * <br>
	 * Traditional Domino database access control list (ACL) privileges apply under all circumstances.
	 * In order to use {@link #markInService(String, String)} on a database in a cluster, the remote Notes
	 * user must have at least designer access privileges for the specified database.
	 * If a user does not have the proper privileges, a database access error is returned.<br>
	 * <br>
	 * The {@link #markInService(String, String)} function only affects databases within a Lotus Domino Server cluster.<br>
	 * <br>
	 * For more information, see the Domino  Administration Help database.

	 * @param server db server
	 * @param filePath db filepath
	 */
	public static void markInService(String server, String filePath) {
		Memory dbPathMem = constructNetPath(server, filePath);
		short result = NotesNativeAPI.get().NSFDbMarkInService(dbPathMem);
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * This function marks a cluster database in service by clearing the database option flag
	 * {@link DatabaseOption#OUT_OF_SERVICE}, if set.<br>
	 * <br>
	 * When a call to {@link #markInService(String, String)} is successful, the Cluster Manager enables
	 * users to access the database again by removing the "out of service" access restriction.<br>
	 * <br>
	 * Traditional Domino database access control list (ACL) privileges apply under all circumstances.
	 * In order to use {@link #markInService(String, String)} on a database in a cluster, the remote Notes
	 * user must have at least designer access privileges for the specified database.
	 * If a user does not have the proper privileges, a database access error is returned.<br>
	 * <br>
	 * The {@link #markInService(String, String)} function only affects databases within a Lotus Domino Server cluster.<br>
	 * <br>
	 * For more information, see the Domino Administration Help database.<br>
	 * <br>
	 * This is a convenience function that just calls {@link #markInService(String, String)} with
	 * server/filepath of the current database.
	 */
	public void markInService() {
		markInService(getServer(), getRelativeFilePath());
	}
	
	/**
	 * This function marks a cluster database out of service for remote user sessions by modifying
	 * the database option flags to include {@link DatabaseOption#OUT_OF_SERVICE}.<br>
	 * <br>
	 * When this operation is successful, the Cluster Manager denies any new user sessions for this database.<br>
	 * This restriction is in addition to any restrictions set forth in the database access control list (ACL).<br>
	 * The purpose of this function is allow the system administrator to perform maintenance on a database
	 * without requiring a server shutdown or having to use the database ACL to restrict access.<br>
	 * <br>
	 * In order to use {@link #markOutOfService(String, String)} with a database on a clustered server, the remote
	 * Notes user must have at least designer access privileges.<br>
	 * <br>
	 * If a user's privilege level is insufficient, a database access error is returned.<br>
	 * The {@link #markOutOfService(String, String)} function affects only databases that reside on
	 * Domino clusters.<br>
	 * You can mark a database back in service by calling the {@link #markInService(String, String)} function.<br>
	 * <br>
	 * For more information, see the Domino Administration Help database.
	 * 
	 * @param server db server
	 * @param filePath db filepath
	 */
	public static void markOutOfService(String server, String filePath) {
		Memory dbPathMem = constructNetPath(server, filePath);
		short result = NotesNativeAPI.get().NSFDbMarkOutOfService(dbPathMem);
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * This function marks a cluster database out of service for remote user sessions by modifying
	 * the database option flags to include {@link DatabaseOption#OUT_OF_SERVICE}.<br>
	 * <br>
	 * When this operation is successful, the Cluster Manager denies any new user sessions for this database.<br>
	 * This restriction is in addition to any restrictions set forth in the database access control list (ACL).<br>
	 * The purpose of this function is allow the system administrator to perform maintenance on a database
	 * without requiring a server shutdown or having to use the database ACL to restrict access.<br>
	 * <br>
	 * In order to use {@link #markOutOfService(String, String)} with a database on a clustered server, the remote
	 * Notes user must have at least designer access privileges.<br>
	 * <br>
	 * If a user's privilege level is insufficient, a database access error is returned.<br>
	 * The {@link #markOutOfService(String, String)} function affects only databases that reside on
	 * Domino clusters.<br>
	 * You can mark a database back in service by calling the {@link #markInService(String, String)} function.<br>
	 * <br>
	 * For more information, see the Domino Administration Help database.<br>
	 * <br>
	 * This is a convenience function that just calls {@link #markOutOfService(String, String)} with
	 * server/filepath of the current database.
	 */
	public void markOutOfService() {
		markOutOfService(getServer(), getRelativeFilePath());
	}
	
	/**
	 * Check to see if database is locally encrypted.
	 * 
	 * @return true if encrypted
	 */
	public boolean isLocallyEncrypted() {
		checkHandle();
		
		short result = 0;
		IntByReference retVal = new IntByReference();
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbIsLocallyEncrypted(m_hDB64, retVal);
			NotesErrorUtils.checkResult(result);
		} else {
			result = NotesNativeAPI32.get().NSFDbIsLocallyEncrypted(m_hDB32, retVal);
			NotesErrorUtils.checkResult(result);
		}

		return retVal.getValue() == 1;
	}

	/**
	 * This function returns the number of bytes allocated and the number of bytes
	 * free in the database.<br>
	 * <br>
	 * The total size of the number of bytes allocated plus the number of bytes free will
	 * differ from the file system size of the database.<br>
	 * This is due to internal rounding of the size up to the next 256K boundary.<br>
	 * <br>
	 * Used and unused space is also calculated in "chunks" of allocation granularity while
	 * the file system size is determined in actual bytes.<br>
	 * <br>
	 * The percent of the database that is in use is the result of the number of bytes allocated
	 * divided by the size of the database, multiplied by 100.
	 * 
	 * @return array of [allocatedbytes, freebytes]
	 */
	public int[] getSpaceUsage() {
		checkHandle();
		
		IntByReference retAllocatedBytes = new IntByReference();
		IntByReference retFreeBytes = new IntByReference();

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbSpaceUsage(m_hDB64, retAllocatedBytes, retFreeBytes);
		}
		else {
			result = NotesNativeAPI32.get().NSFDbSpaceUsage(m_hDB32, retAllocatedBytes, retFreeBytes);
		}
		NotesErrorUtils.checkResult(result);
		
		int allocatedBytes = retAllocatedBytes.getValue();
		int freeBytes = retFreeBytes.getValue();
		
		return new int[] {allocatedBytes, freeBytes};
	}
	
	/**
	 * When a database is full text indexed, an index folder is created in the DATA directory.<br>
	 * <br>
	 * This function retrieves the size of this folder and its contents.<br>
	 * <br>
	 * If the database is not full text indexed, the size will be set to zero.
	 * 
	 * @param server db server
	 * @param filePath db filepath
	 * @return FT index size
	 */
	public static int getFTSize(String server, String filePath) {
		Memory dbPathMem = constructNetPath(server, filePath);
		IntByReference retFTSize = new IntByReference();
		
		short result = NotesNativeAPI.get().NSFDbFTSizeGet(dbPathMem, retFTSize);
		NotesErrorUtils.checkResult(result);

		return retFTSize.getValue();
	}
	
	/**
	 * When a database is full text indexed, an index folder is created in the DATA directory.<br>
	 * <br>
	 * This function retrieves the size of this folder and its contents. It is a convenience
	 * function that just calls {@link #getFTSize(String, String)} with server/filepath of the current database.
	 * <br>
	 * If the database is not full text indexed, the size will be set to zero.<br>
	 * 
	 * @return FT index size
	 */
	public int getFTSize() {
		return getFTSize(getServer(), getRelativeFilePath());
	}

	/**
	 * This function will refresh the database design, as allowed by the database/design properties and
	 * access control of Domino, from a server's templates.<br>
	 * <br>
	 * The refreshed database, if open in Domino or Notes at the time of refresh, must be closed and
	 * reopened to view any changes.<br>
	 * <br>
	 * Convenience function that calls {@link #refreshDesign(String, boolean, boolean, IBreakHandler)} with
	 * force and errIfTemplateNotFound set to true and without break handler.
	 * 
	 * @param server name of the Lotus Domino Server on which the database template resides,  If you want to specify "no server" (the local machine), use ""
	 */
	public void refreshDesign(String server) {
		refreshDesign(server, true, true, null);
	}
	
	/**
	 * This function will refresh the database design, as allowed by the database/design properties and
	 * access control of Domino, from a server's templates.<br>
	 * <br>
	 * The refreshed database, if open in Domino or Notes at the time of refresh, must be closed and
	 * reopened to view any changes.
	 * 
	 * @param server name of the Lotus Domino Server on which the database template resides,  If you want to specify "no server" (the local machine), use ""
	 * @param force true to force operation, even if destination "up to date"
	 * @param errIfTemplateNotFound true to return an error if the template is not found
	 * @param abortHandler optional break handler to abort the operation or null
	 */
	public void refreshDesign(String server, boolean force, boolean errIfTemplateNotFound, final IBreakHandler abortHandler) {
		checkHandle();
		
		Memory serverMem = NotesStringUtils.toLMBCS(server, true);
		
		ABORTCHECKPROC abortProc = null;
		if (abortHandler!=null) {
			if (PlatformUtils.isWin32()) {
				abortProc = new ABORTCHECKPROCWin32() {

					@Override
					public short invoke() {
						if (abortHandler.shouldInterrupt())
							return INotesErrorConstants.ERR_CANCEL;
						return 0;
					}};
			}
			else {
				abortProc = new ABORTCHECKPROC() {

					@Override
					public short invoke() {
						if (abortHandler.shouldInterrupt())
							return INotesErrorConstants.ERR_CANCEL;
						return 0;
					}};
			}
		}
		int dwFlags = 0;
		if (force) {
			dwFlags |= NotesConstants.DESIGN_FORCE;
		}
		if (errIfTemplateNotFound) {
			dwFlags |= NotesConstants.DESIGN_ERR_TMPL_NOT_FOUND;
		}
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().DesignRefresh(serverMem, m_hDB64, dwFlags, abortProc, null);
		}
		else {
			result = NotesNativeAPI32.get().DesignRefresh(serverMem, m_hDB32, dwFlags, abortProc, null);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * This function gets the database information buffer of a Domino database.<br>
	 * <br>
	 * The information buffer is a NULL terminated string and consists of one or more of the
	 * following pieces of information:<br>
	 * <ul>
	 * <li>database title</li>
	 * <li>categories</li>
	 * <li>class</li>
	 * <li>and design class</li>
	 * </ul>
	 * <br>
	 * Use NSFDbInfoParse to retrieve any one piece of information from the buffer.<br>
	 * <br>
	 * Database information appears in the Notes UI, in the File, Database, Properties InfoBox.<br>
	 * Clicking the Basics tab displays the Title field with the database title.<br>
	 * <br>
	 * Selecting the Design tab opens the Design tabbed page. The database class is displayed in the
	 * Database is a template/Template Name field and the database design class is displayed in the
	 * Inherit design from template/Template Name field. The Categories field displays the database
	 * categories.<br>
	 * <br>
	 * Database categories are different than view categories.<br>
	 * Database categories are keywords specified for the database.<br>
	 * Each server's database catalog (CATALOG.NSF) contains a view, called Databases by Category,
	 * which lists only the categorized databases.<br>
	 * <br>
	 * The database title also appears on the Notes Desktop below each database icon.
	 * 
	 * @return buffer
	 */
	private Memory getDbInfoBuffer() {
		checkHandle();
		
		Memory infoBuf = new Memory(NotesConstants.NSF_INFO_SIZE);
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbInfoGet(m_hDB64, infoBuf);
		}
		else {
			result = NotesNativeAPI32.get().NSFDbInfoGet(m_hDB32, infoBuf);
		}
		NotesErrorUtils.checkResult(result);
		
		return infoBuf;
	}
	
	/**
	 * Writes the modified db info buffer
	 * 
	 * @param infoBuf info buffer
	 */
	private void writeDbInfoBuffer(Memory infoBuf) {
		checkHandle();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbInfoSet(m_hDB64, infoBuf);
		}
		else {
			result = NotesNativeAPI32.get().NSFDbInfoSet(m_hDB32, infoBuf);
		}
		NotesErrorUtils.checkResult(result);
		
		//as documented in NSFDbInfoSet, we need to update the icon note as well
		try {
			NotesNote iconNote = openIconNote();
			if (iconNote.hasItem("$TITLE")) {
				if (PlatformUtils.is64Bit()) {
					result = NotesNativeAPI64.get().NSFItemSetText(iconNote.getHandle64(), NotesStringUtils.toLMBCS("$TITLE",  true), infoBuf, NotesConstants.MAXWORD);
				}
				else {
					result = NotesNativeAPI32.get().NSFItemSetText(iconNote.getHandle32(), NotesStringUtils.toLMBCS("$TITLE",  true), infoBuf, NotesConstants.MAXWORD);
				}
				NotesErrorUtils.checkResult(result);

				iconNote.update();
			}
			iconNote.recycle();
		}
		catch (NotesError e) {
			if (e.getId() != 578) // Special database object cannot be located
				throw e;
		}
	}
	
	/**
	 * Returns the database title
	 * 
	 * @return title
	 */
	public String getTitle() {
		checkHandle();
		
		Memory infoBuf = getDbInfoBuffer();
		Memory titleMem = new Memory(NotesConstants.NSF_INFO_SIZE - 1);
		
		NotesNativeAPI.get().NSFDbInfoParse(infoBuf, NotesConstants.INFOPARSE_TITLE, titleMem, (short) (titleMem.size() & 0xffff));
		return NotesStringUtils.fromLMBCS(titleMem, -1);
	}
	
	/**
	 * Changes the database title
	 * 
	 * @param newTitle new title
	 */
	public void setTitle(String newTitle) {
		checkHandle();
		Memory infoBuf = getDbInfoBuffer();
		Memory newTitleMem = NotesStringUtils.toLMBCS(newTitle, true);
		
		NotesNativeAPI.get().NSFDbInfoModify(infoBuf, NotesConstants.INFOPARSE_TITLE, newTitleMem);
		
		writeDbInfoBuffer(infoBuf);
	}

	/**
	 * Returns the database categories
	 * 
	 * @return categories
	 */
	public String getCategories() {
		checkHandle();
		
		Memory infoBuf = getDbInfoBuffer();
		Memory categoriesMem = new Memory(NotesConstants.NSF_INFO_SIZE - 1);
		
		NotesNativeAPI.get().NSFDbInfoParse(infoBuf, NotesConstants.INFOPARSE_CATEGORIES, categoriesMem, (short) (categoriesMem.size() & 0xffff));
		return NotesStringUtils.fromLMBCS(categoriesMem, -1);
	}
	
	/**
	 * Changes the database categories
	 * 
	 * @param newCategories new categories
	 */
	public void setCategories(String newCategories) {
		checkHandle();
		Memory infoBuf = getDbInfoBuffer();
		Memory newCategoriesMem = NotesStringUtils.toLMBCS(newCategories, true);
		
		NotesNativeAPI.get().NSFDbInfoModify(infoBuf, NotesConstants.INFOPARSE_CATEGORIES, newCategoriesMem);
		
		writeDbInfoBuffer(infoBuf);
	}
	
	/**
	 * The template name of a database, if the database is a template. If the database is not a template, returns an empty string.
	 * 
	 * @return template name or "" if no template
	 */
	public String getTemplateName() {
		checkHandle();
		
		Memory infoBuf = getDbInfoBuffer();
		Memory templateNameMem = new Memory(NotesConstants.NSF_INFO_SIZE - 1);
		
		NotesNativeAPI.get().NSFDbInfoParse(infoBuf, NotesConstants.INFOPARSE_CLASS, templateNameMem, (short) (templateNameMem.size() & 0xffff));
		return NotesStringUtils.fromLMBCS(templateNameMem, -1);
	}
	
	/**
	 * Changes the template name of a template database.
	 * 
	 * @param newTemplateName new template name
	 */
	public void setTemplateName(String newTemplateName) {
		checkHandle();
		Memory infoBuf = getDbInfoBuffer();
		Memory newTemplateNameMem = NotesStringUtils.toLMBCS(newTemplateName, true);
		
		NotesNativeAPI.get().NSFDbInfoModify(infoBuf, NotesConstants.INFOPARSE_CLASS, newTemplateNameMem);
		
		writeDbInfoBuffer(infoBuf);
	}
	
	/**
	 *  The name of the design template from which a database inherits its design.<br>
	 *  If the database does not inherit its design from a design template, it returns an empty string ("").
	 * 
	 * @return template name or ""
	 */
	public String getDesignTemplateName() {
		checkHandle();
		
		Memory infoBuf = getDbInfoBuffer();
		Memory designTemplateNameMem = new Memory(NotesConstants.NSF_INFO_SIZE - 1);
		
		NotesNativeAPI.get().NSFDbInfoParse(infoBuf, NotesConstants.INFOPARSE_DESIGN_CLASS, designTemplateNameMem, (short) (designTemplateNameMem.size() & 0xffff));
		return NotesStringUtils.fromLMBCS(designTemplateNameMem, -1);
	}
	
	/**
	 * Changes the name of the design template from which a database inherits its design
	 * 
	 * @param newDesignTemplateName new design template name
	 */
	public void setDesignTemplateName(String newDesignTemplateName) {
		checkHandle();
		Memory infoBuf = getDbInfoBuffer();
		Memory newDesignTemplateNameMem = NotesStringUtils.toLMBCS(newDesignTemplateName, true);
		
		NotesNativeAPI.get().NSFDbInfoModify(infoBuf, NotesConstants.INFOPARSE_DESIGN_CLASS, newDesignTemplateNameMem);
		
		writeDbInfoBuffer(infoBuf);
	}

	public static enum DbMode {
		/** internal db handle refers to a "directory" and not a file */
		DIRECTORY,
		/** internal db handle refers to a normal database file */
		DATABASE
		}

	/**
	 * Use this function to find out whether the {@link NotesDatabase} is a database or a directory.
	 * (The C API uses the db handle also to scan directory contents)
	 * 
	 * @return mode
	 */
	public DbMode getMode() {
		if (m_dbMode==null) {
			checkHandle();

			ShortByReference retMode = new ShortByReference();
			short result;

			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().NSFDbModeGet(m_hDB64, retMode);
			}
			else {
				result = NotesNativeAPI32.get().NSFDbModeGet(m_hDB32, retMode);
			}
			NotesErrorUtils.checkResult(result);
			
			if (retMode.getValue() == NotesConstants.DB_LOADED) {
				m_dbMode = DbMode.DATABASE;
			}
			else {
				m_dbMode = DbMode.DIRECTORY;
			}
		}
		return m_dbMode;
	}
	
	/**
	 * Indicates whether document locking is enabled for a database.
	 *  
	 * @return true if enabled
	 */
	public boolean isDocumentLockingEnabled() {
		return getOption(DatabaseOption.IS_LOCK_DB);
	}
	
	/**
	 * Indicates whether document locking is enabled for a database.
	 * 
	 * @param b true to enable document locking
	 */
	public void setDocumentLockingEnabled(boolean b) {
		setOption(DatabaseOption.IS_LOCK_DB, b);
	}
	
	private static final Pattern[] dbFilenamePatterns = new Pattern[] {
			//old NSF versions
			Pattern.compile("^.+\\.ns\\d$", Pattern.CASE_INSENSITIVE),
			Pattern.compile("^.+\\.nt\\\\d$", Pattern.CASE_INSENSITIVE),
			//standard db and template name
			Pattern.compile("^.+\\.nsf$", Pattern.CASE_INSENSITIVE),
			Pattern.compile("^.+\\.ntf$", Pattern.CASE_INSENSITIVE),
			//not sure what this is, coming from osfile.h
			Pattern.compile("^.+\\.nsh$", Pattern.CASE_INSENSITIVE),
			Pattern.compile("^.+\\.nsg$", Pattern.CASE_INSENSITIVE),
			//cache.ndk and other internal databases
			Pattern.compile("^.+\\.ndk$", Pattern.CASE_INSENSITIVE),
			//mailbox db
			Pattern.compile("^.+\\.box$", Pattern.CASE_INSENSITIVE)
	};
	
	/**
	 * The method compares a path against a list of known file extensions
	 * for NSF databases like .ns9, .nsf, .ntf or .box
	 * 
	 * @param path filepath
	 * @return true of NSF database format
	 */
	public static boolean isDatabasePath(String path) {
		for (Pattern currPattern : dbFilenamePatterns) {
			Matcher matcher = currPattern.matcher(path);
			if (matcher.matches()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Runs a DQL query against the documents in the database.<br>
	 * 
	 * @param query Domino query (DQL) generated via {@link DQL} factory class
	 * @return query result
	 * @since V10
	 */
	public NotesDbQueryResult query(DQLTerm query) {
		return query(query.toString(), null, 0, 0,
				0);
	}

	/**
	 * Runs a DQL query against the documents in the database.<br>
	 * 
	 * @param query Domino query (DQL) generated via {@link DQL} factory class
	 * @param flags controlling execution, see {@link DBQuery}
	 * @return query result
	 * @since V10
	 */
	public NotesDbQueryResult query(DQLTerm query, EnumSet<DBQuery> flags) {
		return query(query, flags, 0, 0, 0);
	}
	
	/**
	 * Runs a DQL query against the documents in the database.<br>
	 * 
	 * @param query Domino query (DQL) generated via {@link DQL} factory class
	 * @param flags controlling execution, see {@link DBQuery}
	 * @param maxDocsScanned maximum number of document scans allowed
	 * @param maxEntriesScanned maximum number of view entries processed allows
	 * @param maxMsecs max milliseconds of executiion allow 
	 * @return query result
	 * @since V10
	 */
	public NotesDbQueryResult query(DQLTerm query, EnumSet<DBQuery> flags,
			int maxDocsScanned, int maxEntriesScanned, int maxMsecs) {
		
		return query(query.toString(), flags, maxDocsScanned, maxEntriesScanned,
				maxMsecs);
	}
	
	/**
	 * Runs a DQL query against the documents in the database.<br>
	 * 
	 * @param query Domino query (DQL) as a single string (max 64K in length) 
	 * @return query result
	 * @since V10
	 */
	public NotesDbQueryResult query(String query) {
		return query(query, null, 0, 0, 0);
	}
	
	/**
	 * Runs a DQL query against the documents in the database.<br>
	 * 
	 * @param query Domino query (DQL) as a single string (max 64K in length) 
	 * @param flags controlling execution, see {@link DBQuery}
	 * @return query result
	 * @since V10
	 */
	public NotesDbQueryResult query(String query, EnumSet<DBQuery> flags) {
		return query(query, flags, 0, 0, 0);
	}

	/**
	 * Runs a DQL query against the documents in the database.<br>
	 * 
	 * @param query Domino query (DQL) as a single string (max 64K in length) 
	 * @param flags controlling execution, see {@link DBQuery}
	 * @param maxDocsScanned maximum number of document scans allowed
	 * @param maxEntriesScanned maximum number of view entries processed allows
	 * @param maxMsecs max milliseconds of executiion allow 
	 * @return query result
	 * @since V10
	 */
	public NotesDbQueryResult query(String query, EnumSet<DBQuery> flags,
			int maxDocsScanned, int maxEntriesScanned, int maxMsecs) {
		checkHandle();
		
		Memory queryMem = NotesStringUtils.toLMBCS(query, true);
		int flagsAsInt = flags==null ? 0 : DBQuery.toBitMask(flags);
		
		NotesIDTable idTable = null;
		String errorTxt = "";
		String explainTxt = "";
		
		IntByReference retError = new IntByReference();
		retError.setValue(0);
		IntByReference retExplain = new IntByReference();
		retExplain.setValue(0);
		
		long t0=System.currentTimeMillis();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference retResults = new LongByReference();
			retResults.setValue(0);
			
			result = NotesNativeAPI64V1000.get().NSFQueryDB(m_hDB64, queryMem,
					flagsAsInt, maxDocsScanned, maxEntriesScanned, maxMsecs, retResults, 
					retError, retExplain);
			
			if (retError.getValue()!=0) {
				Pointer ptr = Mem64.OSMemoryLock(retError.getValue());
				try {
					errorTxt = NotesStringUtils.fromLMBCS(ptr, -1);
				}
				finally {
					Mem64.OSMemoryUnlock(retError.getValue());
					Mem64.OSMemoryFree(retError.getValue());
				}
			}

			if (result!=0) {
				if (!StringUtil.isEmpty(errorTxt)) {
					throw new NotesError(result, errorTxt, NotesErrorUtils.toNotesError(result));
				}
				else {
					NotesErrorUtils.checkResult(result);
				}
			}
			
			if (retResults.getValue()!=0) {
				idTable = (NotesIDTable) new NotesIDTable(retResults.getValue(), false);
			}
			
			if (retExplain.getValue()!=0) {
				Pointer ptr = Mem64.OSMemoryLock(retExplain.getValue());
				try {
					explainTxt = NotesStringUtils.fromLMBCS(ptr, -1);
				}
				finally {
					Mem64.OSMemoryUnlock(retExplain.getValue());
					Mem64.OSMemoryFree(retExplain.getValue());
				}
			}
		}
		else {
			IntByReference retResults = new IntByReference();
			retResults.setValue(0);
			
			result = NotesNativeAPI32V1000.get().NSFQueryDB(m_hDB32, queryMem,
					flagsAsInt, maxDocsScanned, maxEntriesScanned, maxMsecs, retResults, 
					retError, retExplain);
			
			if (retError.getValue()!=0) {
				Pointer ptr = Mem32.OSMemoryLock(retError.getValue());
				try {
					errorTxt = NotesStringUtils.fromLMBCS(ptr, -1);
				}
				finally {
					Mem32.OSMemoryUnlock(retError.getValue());
					Mem32.OSMemoryFree(retError.getValue());
				}
			}

			if (result!=0) {
				if (!StringUtil.isEmpty(errorTxt)) {
					throw new NotesError(result, errorTxt, NotesErrorUtils.toNotesError(result));
				}
				else {
					NotesErrorUtils.checkResult(result);
				}
			}
			
			if (retResults.getValue()!=0) {
				idTable = (NotesIDTable) new NotesIDTable(retResults.getValue(), false);
			}
			
			if (retExplain.getValue()!=0) {
				Pointer ptr = Mem32.OSMemoryLock(retExplain.getValue());
				try {
					explainTxt = NotesStringUtils.fromLMBCS(ptr, -1);
				}
				finally {
					Mem32.OSMemoryUnlock(retExplain.getValue());
					Mem32.OSMemoryFree(retExplain.getValue());
				}
			}
		}
		long t1=System.currentTimeMillis();
		
		return new NotesDbQueryResult(this, query, idTable, explainTxt, t1-t0);
	}
	
	/**
	 * The extended version of the Item Definition Table for a database contains
	 * the number of items, name and type of all the items defined in that database.<br>
	 * <br>
	 * Examples are field names, form names, design names, and formula labels.<br>
	 * Applications can obtain a copy of the extended version of the Item Definition
	 * Table by calling this method.
	 * 
	 * @return item definition table with case-insensitive key access
	 */
	public NavigableMap<String,Integer> getItemDefinitionTable() {
		checkHandle();
		
		NavigableMap<String,Integer> table = new TreeMap<String,Integer>(String.CASE_INSENSITIVE_ORDER);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			INotesNativeAPI64 api = NotesNativeAPI64.get();
			
			LongByReference retItemNameTable = new LongByReference();
			result = api.NSFDbItemDefTableExt(m_hDB64, retItemNameTable);
			NotesErrorUtils.checkResult(result);
			
			long retItemNameTableAsLong = retItemNameTable.getValue();
			Pointer pItemDefExt = Mem64.OSLockObject(retItemNameTableAsLong);
			try {
				NotesItemDefinitionTableExt itemDefTableExt = NotesItemDefinitionTableExt.newInstance(pItemDefExt);
				itemDefTableExt.read();
				
				NotesItemDefinitionTableLock itemDefTableLock = NotesItemDefinitionTableLock.newInstance();
				
				result = api.NSFItemDefExtLock(pItemDefExt, itemDefTableLock);
				NotesErrorUtils.checkResult(result);
				try {
					IntByReference retNumEntries = new IntByReference();
					result = api.NSFItemDefExtEntries(itemDefTableLock, retNumEntries);
					NotesErrorUtils.checkResult(result);
					
					int iNumEntries = retNumEntries.getValue();
					
					ShortByReference retItemType = new ShortByReference();
					ShortByReference retItemNameLength = new ShortByReference();
					//implemented this like the sample code for NSFDbItemDefTableExt
					//provided in the C API documentation, but it looks like the following
					//128 byte buffer is never used, instead the NSFItemDefExtGetEntry
					//call redirects the pointer stored in retItemNamePtr to
					//the item name in memory
					DisposableMemory retItemName = new DisposableMemory(128);
					DisposableMemory retItemNamePtr = new DisposableMemory(Native.POINTER_SIZE);
					retItemNamePtr.setPointer(0, retItemName);
					
					try {
						for (int i=0; i<iNumEntries; i++) {
							retItemName.clear();
							
							result = api.NSFItemDefExtGetEntry(itemDefTableLock, i, retItemType,
									retItemNameLength, retItemNamePtr);
							NotesErrorUtils.checkResult(result);
							
							//grab the current pointer stored in retItemNamePtr;
							//using retItemName here did not work, because the
							//value of retItemNamePtr got redirected
							String currItemName = NotesStringUtils.fromLMBCS(
									new Pointer(retItemNamePtr.getLong(0)),
									(int) (retItemNameLength.getValue() & 0xffff));
							int itemTypeAsInt = (int) (retItemType.getValue() & 0xffff);
							
							table.put(currItemName, Integer.valueOf(itemTypeAsInt));
						}
					}
					finally {
						retItemName.dispose();
						retItemNamePtr.dispose();
					}
				}
				finally {
					result = api.NSFItemDefExtUnlock(itemDefTableExt, itemDefTableLock);
				}
			}
			finally {
				Mem64.OSUnlockObject(retItemNameTable.getValue());
				Mem64.OSMemFree(retItemNameTable.getValue());
			}
		}
		else {
			INotesNativeAPI32 api = NotesNativeAPI32.get();
			
			IntByReference retItemNameTable = new IntByReference();
			result = api.NSFDbItemDefTableExt(m_hDB32, retItemNameTable);
			NotesErrorUtils.checkResult(result);
			
			Pointer pItemDefExt = Mem32.OSLockObject(retItemNameTable.getValue());
			try {
				NotesItemDefinitionTableExt itemDefTableExt = NotesItemDefinitionTableExt.newInstance(pItemDefExt);
				itemDefTableExt.read();
				NotesItemDefinitionTableLock itemDefTableLock = NotesItemDefinitionTableLock.newInstance();
				
				result = api.NSFItemDefExtLock(pItemDefExt, itemDefTableLock);
				NotesErrorUtils.checkResult(result);
				try {
					IntByReference retNumEntries = new IntByReference();
					result = api.NSFItemDefExtEntries(itemDefTableLock, retNumEntries);
					NotesErrorUtils.checkResult(result);
					
					int iNumEntries = retNumEntries.getValue();
					
					ShortByReference retItemType = new ShortByReference();
					ShortByReference retItemNameLength = new ShortByReference();
					//implemented this like the sample code for NSFDbItemDefTableExt
					//provided in the C API documentation, but it looks like the following
					//128 byte buffer is never used, instead the NSFItemDefExtGetEntry
					//call redirects the pointer stored in retItemNamePtr to
					//the item name in memory
					DisposableMemory retItemName = new DisposableMemory(128);
					DisposableMemory retItemNamePtr = new DisposableMemory(Native.POINTER_SIZE);
					retItemNamePtr.setPointer(0, retItemName);
					
					try {
						for (int i=0; i<iNumEntries; i++) {
							result = api.NSFItemDefExtGetEntry(itemDefTableLock, i, retItemType,
									retItemNameLength, retItemNamePtr);
							NotesErrorUtils.checkResult(result);
							
							//grab the current pointer stored in retItemNamePtr;
							//using retItemName here did not work, because the
							//value of retItemNamePtr got redirected
							String currItemName = NotesStringUtils.fromLMBCS(
									new Pointer(retItemNamePtr.getLong(0)),
									(int) (retItemNameLength.getValue() & 0xffff));
							int itemTypeAsInt = (int) (retItemType.getValue() & 0xffff);
							
							table.put(currItemName, Integer.valueOf(itemTypeAsInt));
						}
					}
					finally {
						retItemName.dispose();
						retItemNamePtr.dispose();
					}
				}
				finally {
					result = api.NSFItemDefExtUnlock(itemDefTableExt, itemDefTableLock);
				}
			}
			finally {
				Mem32.OSUnlockObject(retItemNameTable.getValue());
				Mem32.OSMemFree(retItemNameTable.getValue());
			}
		}
		return table;
	}
	
	/**
	 * Open a profile document.<br>
	 * <br>
	 * If a profile for the specified profile name/user pair does not exist,
	 * one is created.<br>
	 * <br>
	 * Author access is the minimum access required to create a profile document.
	 * 
	 * @param profileName Name of the profile
	 * @return profile note
	 */
	public NotesNote getProfileNote(String profileName) {
		return getProfileNote(profileName, (String) null);
	}
	
	/**
	 * Open a profile document.<br>
	 * <br>
	 * If a profile for the specified profile name/user pair does not exist,
	 * one is created.<br>
	 * <br>
	 * Author access is the minimum access required to create a profile document.
	 * 
	 * @param profileName Name of the profile
	 * @param userName Name of the user of this profile.  Optional - may be NULL or empty string
	 * @return profile note
	 */
	public NotesNote getProfileNote(String profileName, String userName) {
		checkHandle();
		
		Memory profileNameMem = NotesStringUtils.toLMBCS(profileName, false);
		Memory userNameMem = StringUtil.isEmpty(userName) ? null : NotesStringUtils.toLMBCS(userName, false);
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethProfileNote = new LongByReference();
			
			short result = NotesNativeAPI64.get().NSFProfileOpen(m_hDB64, profileNameMem,
					(short) (profileNameMem.size() & 0xffff), userNameMem,
					(short) (userNameMem==null ? 0 : (userNameMem.size() & 0xffff)), (short) 1, rethProfileNote);
			NotesErrorUtils.checkResult(result);

			long hNote = rethProfileNote.getValue();
			if (hNote==0) {
				return null;
			}
			NotesNote profileNote = new NotesNote(this, hNote);
			NotesGC.__objectCreated(NotesNote.class, profileNote);
			return profileNote;
		}
		else {
			IntByReference rethProfileNote = new IntByReference();
			
			short result = NotesNativeAPI32.get().NSFProfileOpen(m_hDB32, profileNameMem,
					(short) (profileNameMem.size() & 0xffff), userNameMem,
					(short) (userNameMem==null ? 0 : (userNameMem.size() & 0xffff)), (short) 1, rethProfileNote);
			NotesErrorUtils.checkResult(result);

			int hNote = rethProfileNote.getValue();
			if (hNote==0) {
				return null;
			}
			NotesNote profileNote = new NotesNote(this, hNote);
			NotesGC.__objectCreated(NotesNote.class, profileNote);
			return profileNote;
		}
	}
	
	/**
	 * Delete a profile document from the database. 
	 * 
	 * @param profileName Name of the profile.
	 */
	public void removeProfileNote(String profileName) {
		removeProfileNote(profileName, (String) null);
	}
	
	/**
	 * Delete a profile document from the database. 
	 * 
	 * @param profileName Name of the profile.
	 * @param userName  Name of the user of this profile.  Optional - may be NULL or empty string
	 */
	public void removeProfileNote(String profileName, String userName) {
		checkHandle();
		
		Memory profileNameMem = NotesStringUtils.toLMBCS(profileName, false);
		Memory userNameMem = StringUtil.isEmpty(userName) ? null : NotesStringUtils.toLMBCS(userName, false);

		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().NSFProfileDelete(m_hDB64, profileNameMem,
					(short) (profileNameMem.size() & 0xffff),
					userNameMem, (short) (userNameMem==null ? 0 : (userNameMem.size() & 0xffff)));
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().NSFProfileDelete(m_hDB32, profileNameMem,
					(short) (profileNameMem.size() & 0xffff),
					userNameMem, (short) (userNameMem==null ? 0 : (userNameMem.size() & 0xffff)));
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * Method to check if large summary buffer support has been enabled
	 * for this database
	 * 
	 * @return true if enabled
	 * @since V10
	 */
	public boolean isLargeSummaryEnabled() {
		checkHandle();
		
		if (PlatformUtils.is64Bit()) {
			return NotesNativeAPI64V1000.get().NSFDbLargeSummaryEnabled(m_hDB64) == 1;
		}
		else {
			return NotesNativeAPI32V1000.get().NSFDbLargeSummaryEnabled(m_hDB32) == 1;
		}
	}
	
	/**
	 * This method locks the database against concurrent updaters
	 * (e.g. other code calling this method at the same time) and
	 * executes the specified {@link ITransactionCallable} within an active
	 * DB transaction. If errors occur during execution, the transaction is rolled back.<br>
	 * See {@link Transactions} for details about the NSF transaction concept NTA - Nested Top Actions.<br>
	 * <br>
	 * Readers can still access the database while is it locked. But
	 * if you modify notes during callable execution, any concurrent code reading
	 * the notes is blocked until the transaction is either committed or aborted.
	 * 
	 * @param callable callable to execute
	 * @return computation result
	 * 
	 * @param <T> type of computation result
	 */
	public <T> T runWithDbLock(ITransactionCallable<T> callable) {
		checkHandle();
		
		short result;
		
		ShortByReference statusInOut = new ShortByReference();
		statusInOut.setValue((short) 0);
		
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFDbLock(m_hDB64);
			NotesErrorUtils.checkResult(result);
			
			Exception executionEx = null;
			T val = null;
			try {
				val = callable.runInDbTransaction(this);
			}
			catch (Exception e) {
				executionEx = e;
				
				NotesError nEx = ExceptionUtil.findCauseOfType(e, NotesError.class);
				if (nEx!=null) {
					statusInOut.setValue((short) (nEx.getId() & 0xffff));
				}
				else {
					statusInOut.setValue(INotesErrorConstants.ERR_CANCEL);
				}
			}
			finally {
				//rolls back if statusInOut!=0 and writes its own status code in statusInOut if
				//there are errors unlocking
				NotesNativeAPI64.get().NSFDbUnlock(m_hDB64, statusInOut);
			}
			
			if (executionEx instanceof RuntimeException) {
				throw (RuntimeException) executionEx;
			}
			else if (executionEx!=null) {
				throw new NotesError(0, "Error executing code with active DB lock", executionEx);
			}
			
			NotesErrorUtils.checkResult(statusInOut.getValue());
			
			return val;
		}
		else {
			result = NotesNativeAPI32.get().NSFDbLock(m_hDB32);
			NotesErrorUtils.checkResult(result);
			
			Exception executionEx = null;
			T val = null;
			try {
				val = callable.runInDbTransaction(this);
			}
			catch (Exception e) {
				executionEx = e;
				
				NotesError nEx = ExceptionUtil.findCauseOfType(e, NotesError.class);
				if (nEx!=null) {
					statusInOut.setValue((short) (nEx.getId() & 0xffff));
				}
				else {
					statusInOut.setValue(INotesErrorConstants.ERR_CANCEL);
				}
			}
			finally {
				//rolls back if statusInOut!=0 and writes its own status code in statusInOut if
				//there are errors unlocking
				NotesNativeAPI32.get().NSFDbUnlock(m_hDB32, statusInOut);
			}
			
			if (executionEx instanceof RuntimeException) {
				throw (RuntimeException) executionEx;
			}
			else if (executionEx!=null) {
				throw new NotesError(0, "Error executing code with active DB lock", executionEx);
			}
			
			NotesErrorUtils.checkResult(statusInOut.getValue());
			
			return val;
		}
	}
	
	public enum HarvestMode { UPDATE, ADD }
	
	/**
	 * Harvest view design elements for optimized DQL performance.
	 * 
	 * @param mode harvest mode
	 * @since V10
	 */
	public void harvestDesign(HarvestMode mode) {
		checkHandle();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64V1000.get().NSFDesignHarvest(m_hDB64, mode==HarvestMode.ADD ? 1 : 0);
		}
		else {
			result = NotesNativeAPI32V1000.get().NSFDesignHarvest(m_hDB32, mode==HarvestMode.ADD ? 1 : 0);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	public List<String> getNamesList() {
		NotesNote tmpNote = createNote();
		try {
			List<Object> userNamesList = FormulaExecution.evaluate("@Usernameslist", tmpNote);
			List<String> namesListWithoutRoles = new ArrayList<String>();
			for (int i=0; i<userNamesList.size(); i++) {
				String currName = userNamesList.get(i).toString();
				if (!currName.startsWith("[") && !currName.endsWith("]")) {
					namesListWithoutRoles.add(currName);
				}
			}
			return namesListWithoutRoles;
		}
		finally {
			tmpNote.recycle();
		}
	}

	/**
	 * Returns an array of TCP [hostname, domainname, fullname] for the server of this database.
	 * 
	 * @return hostname/domainname/fullname
	 */
	public String[] getTcpHostName() {
		checkHandle();
		
		DisposableMemory retHostName = new DisposableMemory(NotesConstants.MAXPATH);
		DisposableMemory retDomainName = new DisposableMemory(NotesConstants.MAXPATH);
		DisposableMemory retFullName = new DisposableMemory(NotesConstants.MAXPATH);

		try {
			short result;
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().NSFDbGetTcpHostName(m_hDB64, retHostName, (short) ((NotesConstants.MAXPATH-1) & 0xffff),
						retDomainName, (short) ((NotesConstants.MAXPATH-1) & 0xffff),
						retFullName, (short) ((NotesConstants.MAXPATH-1) & 0xffff));
			}
			else {
				result = NotesNativeAPI32.get().NSFDbGetTcpHostName(m_hDB32, retHostName, (short) ((NotesConstants.MAXPATH-1) & 0xffff),
						retDomainName, (short) ((NotesConstants.MAXPATH-1) & 0xffff),
						retFullName, (short) ((NotesConstants.MAXPATH-1) & 0xffff));
			}
			NotesErrorUtils.checkResult(result);
			
			String hostName = NotesStringUtils.fromLMBCS(retHostName, -1);
			String domainName = NotesStringUtils.fromLMBCS(retDomainName, -1);
			String fullName = NotesStringUtils.fromLMBCS(retFullName, -1);
			
			return new String[] {hostName, domainName, fullName};
		}
		finally {
			retHostName.dispose();
			retDomainName.dispose();
			retFullName.dispose();
		}
	}
	
	public static class ProfileNoteInfo {
		private String m_profileName;
		private String m_username;
		private int m_noteId;
		
		private ProfileNoteInfo(String profileName, String username, int noteId) {
			m_profileName = profileName;
			m_username = username;
			m_noteId = noteId;
		}
		
		public int getNoteId() {
			return m_noteId;
		}
		
		public String getProfileName() {
			return m_profileName;
		}
		
		public String getUserName() {
			return m_username;
		}

		@Override
		public String toString() {
			return "ProfileNoteInfo [profileName=" + m_profileName + ", username=" + m_username + ", noteId="
					+ m_noteId + "]";
		}
	}
	
	/**
	 * Returns infos about all profile notes in the database
	 * 
	 * @return list of  profile note infos
	 */
	public List<ProfileNoteInfo> getProfileNoteInfos() {
		return getProfileNoteInfos(null);
	}
	
	/**
	 * Returns infos about the profile notes with the specified name in the database
	 * 
	 * @param profileName Name of the profile. To enumerate all profile documents within a database, use null
	 * @return list of  profile note infos
	 */
	public List<ProfileNoteInfo> getProfileNoteInfos(String profileName) {
		checkHandle();
		
		List<ProfileNoteInfo> retNoteInfos = new ArrayList<>();
		
		Memory profileNameMem = StringUtil.isEmpty(profileName) ? null : NotesStringUtils.toLMBCS(profileName, false);
		
		if (PlatformUtils.is64Bit()) {
			NotesCallbacks.b64_NSFPROFILEENUMPROC callback = new NotesCallbacks.b64_NSFPROFILEENUMPROC() {

				@Override
				public short invoke(long hDB, Pointer ctx, Pointer profileNameMem, short profileNameLength,
						Pointer usernameMem, short usernameLength, int noteId) {
					
					String profileName="";
					if (profileName!=null) {
						profileName = NotesStringUtils.fromLMBCS(profileNameMem, (int) ((profileNameLength & 0xffff)));
					}
					String userName="";
					if (usernameMem!=null) {
						userName = NotesStringUtils.fromLMBCS(usernameMem, (int) ((usernameLength & 0xffff)));
					}
					
					retNoteInfos.add(new ProfileNoteInfo(profileName, userName, noteId));
					return 0;
				}
			};
			
			short result = NotesNativeAPI64.get().NSFProfileEnum(m_hDB64,
					profileNameMem, profileNameMem==null ? (short) 0 : (short) (profileNameMem.size() & 0xffff),
					callback, null, 0);
			NotesErrorUtils.checkResult(result);
		}
		else {
			NotesCallbacks.b32_NSFPROFILEENUMPROC callback;
			
			if (PlatformUtils.isWin32()) {
				callback = new Win32NotesCallbacks.NSFPROFILEENUMPROCWin32() {

					@Override
					public short invoke(int hDB, Pointer ctx, Pointer profileNameMem, short profileNameLength,
							Pointer usernameMem, short usernameLength, int noteId) {
						
						String profileName="";
						if (profileName!=null) {
							profileName = NotesStringUtils.fromLMBCS(profileNameMem, (int) ((profileNameLength & 0xffff)));
						}
						String userName="";
						if (usernameMem!=null) {
							userName = NotesStringUtils.fromLMBCS(usernameMem, (int) ((profileNameLength & 0xffff)));
						}
						
						retNoteInfos.add(new ProfileNoteInfo(profileName, userName, noteId));
						return 0;
					}
				};
			}
			else {
				callback = new Win32NotesCallbacks.NSFPROFILEENUMPROCWin32() {

					@Override
					public short invoke(int hDB, Pointer ctx, Pointer profileNameMem, short profileNameLength,
							Pointer usernameMem, short usernameLength, int noteId) {
						
						String profileName="";
						if (profileName!=null) {
							profileName = NotesStringUtils.fromLMBCS(profileNameMem, (int) ((profileNameLength & 0xffff)));
						}
						String userName="";
						if (usernameMem!=null) {
							userName = NotesStringUtils.fromLMBCS(usernameMem, (int) ((profileNameLength & 0xffff)));
						}
						
						retNoteInfos.add(new ProfileNoteInfo(profileName, userName, noteId));
						return 0;
					}
				};
			}
			
			short result = NotesNativeAPI32.get().NSFProfileEnum(m_hDB32,
					profileNameMem, profileNameMem==null ? (short) 0 : (short) (profileNameMem.size() & 0xffff),
					callback, null, 0);
			NotesErrorUtils.checkResult(result);
		}
		
		return retNoteInfos;
	}
	
	/**
	 * Converts a profile name and profile username to the name of a named object
	 * 
	 * @param profileName profile name
	 * @param username profile username or null/empty string
	 * @param leaveCase true to keep the case
	 * @return name of named object
	 */
	private String toProfileNoteName(String profileName, String username, boolean leaveCase) {
		if (StringUtil.isEmpty(profileName)) {
			throw new IllegalArgumentException("Profile name cannot be empty");
		}
		
		Memory profileNameMem = NotesStringUtils.toLMBCS(profileName, false);
		Memory usernameMem = StringUtil.isEmpty(username) ? null : NotesStringUtils.toLMBCS(username, false);
		
		DisposableMemory retStrMem = new DisposableMemory(NotesConstants.MAXSPRINTF);
		
		try {
			short result = NotesNativeAPI.get().NSFProfileNameToProfileNoteName(profileNameMem, (short) (profileNameMem.size() & 0xffff),
					usernameMem, (short) ((usernameMem==null ? 0 : usernameMem.size()) & 0xffff), leaveCase, retStrMem);
			NotesErrorUtils.checkResult(result);
			
			return NotesStringUtils.fromLMBCS(retStrMem, -1);
		}
		finally {
			retStrMem.dispose();
		}

	}

	public enum Action {
		Continue, Stop
	}
	

	/**
	 * Callback interface to receive the named objects in a database
	 */
	private static interface NamedObjectEnumCallback {
		/**
		 * Method is called for every named object in the database
		 * 
		 * @param nameSpace namespace
		 * @param name name of named object
		 * @param rrv RRV to be used with {@link NotesDatabase#openNoteById(int)}
		 * @param entryTime sequence time of the note or null if not provided
		 * @return action, either {@link Action#Continue} to continue scanning and {@link Action#Stop} to stop the search
		 */
		public Action objectFound(String name, int rrv, NotesTimeDate entryTime);
	}

	private final String propEnforceLocalNamedObjectSearch = NotesDatabase.class.getName()+".namedobjects.enforcelocal";
	private final String propEnforceRemoteNamedObjectSearch = NotesDatabase.class.getName()+".namedobjects.enforceremote";
	
	/**
	 * Enumerates all named objects in the database. As of Notes/Domino R10/R11, this
	 * method only returns data in a local database
	 * 
	 * @param callback enumeration callback
	 */
	private void getNamedObjects(NamedObjectEnumCallback callback) {
		boolean useLocalSearch;
		
		if (isRemote()) {
			//NSFDbNamedObjectEnum cannot be used in remote databases as of R11
			useLocalSearch = false;

			//enforce flag can be used to override this behavior if R12 supports remote searches
			if (Boolean.TRUE.equals(NotesGC.getCustomValue(propEnforceLocalNamedObjectSearch))) {
				useLocalSearch = true;
			}
		}
		else {
			useLocalSearch = true;
			
			//enforce flag can be used to select an NSFSearchExtended3 based locally
			//(mostly to compare both ways for testcases)
			if (Boolean.TRUE.equals(NotesGC.getCustomValue(propEnforceRemoteNamedObjectSearch))) {
				useLocalSearch = false;
			}
		}
		
		if (useLocalSearch) {
			_getNamedObjectInfosLocal(callback);
		}
		else {
			_getNamedObjectInfosRemote(callback);
		}
	}
	
	/**
	 * Cache object for named object table entries
	 */
	private static class NamedObjectCacheEntry {
		private String name;
		private int noteId;
		private NotesTimeDate sequenceTime;
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public int getNoteId() {
			return noteId;
		}
		
		public void setNoteId(int noteId) {
			this.noteId = noteId;
		}

		public NotesTimeDate getSequenceTime() {
			return sequenceTime;
		}

		public void setSequenceTime(NotesTimeDate sequenceTime) {
			this.sequenceTime = sequenceTime;
		}
	}
	
	private Map<Integer,NamedObjectCacheEntry> remoteDbNamedObjectLookupCache = new HashMap<>();
	private NotesTimeDate remoteNamedObjectLookupCutOffDate_DataNotes = null;
	
	/**
	 * Enumerates all named objects in the database, implementation for remote database
	 * where NSFDbNamedObjectEnum is not yet available.
	 * 
	 * @param callback enumeration callback
	 */
	private void _getNamedObjectInfosRemote(NamedObjectEnumCallback callback) {
		LinkedHashMap<String, String> items = new LinkedHashMap<>();
		items.put("$name", "");

		//use incremental NSFSearch to improve performance on the second call
		NotesSearch.SearchCallback searchCallback = new NotesSearch.SearchCallback() {

			@Override
			public Action noteFound(NotesDatabase parentDb, ISearchMatch searchMatch,
					IItemTableData summaryBufferData) {

				int noteId = searchMatch.getNoteId();
				NotesTimeDate entryTime = searchMatch.getSeqTime();
				String name = summaryBufferData.getAsString("$name", "");

				NamedObjectCacheEntry info = new NamedObjectCacheEntry();
				info.setName(name);
				info.setNoteId(noteId);
				info.setSequenceTime(entryTime);

				EnumSet<NoteClass> noteClass = searchMatch.getNoteClass();

				if (noteClass.contains(NoteClass.DATA)) {
					remoteDbNamedObjectLookupCache.put(noteId, info);
				}
				
				return Action.Continue;
			}

			@Override
			public Action deletionStubFound(NotesDatabase parentDb, ISearchMatch searchMatch,
					IItemTableData summaryBufferData) {

				remoteDbNamedObjectLookupCache.remove(searchMatch.getNoteId());
				return Action.Continue;
			}

			@Override
			public Action noteFoundNotMatchingFormula(NotesDatabase parentDb, ISearchMatch searchMatch,
					IItemTableData summaryBufferData) {

				remoteDbNamedObjectLookupCache.remove(searchMatch.getNoteId());
				return Action.Continue;
			}

		};

		//although this search is very fast, using a local database is really recommended to make use of the named object table C methods
		remoteNamedObjectLookupCutOffDate_DataNotes = NotesSearch.search(this, null, "@IsAvailable($name)", items, "-",
				EnumSet.of(Search.NAMED_GHOSTS, Search.SELECT_NAMED_GHOSTS), EnumSet.of(NoteClass.DATA),
				remoteNamedObjectLookupCutOffDate_DataNotes, searchCallback);
		
		for (NamedObjectCacheEntry currInfo : remoteDbNamedObjectLookupCache.values()) {
			Action action = callback.objectFound(currInfo.getName(), currInfo.getNoteId(), currInfo.getSequenceTime());
			if (action == Action.Stop) {
				break;
			}
		}
	}
	
	/**
	 * Enumerates all named objects in the database. As of Notes/Domino R10/R11, this
	 * method only returns data in a local database
	 * 
	 * @param callback enumeration callback
	 */
	private void _getNamedObjectInfosLocal(NamedObjectEnumCallback callback) {
		checkHandle();

		if (isRemote()) {
			throw new IllegalStateException("This method cannot yet be called on remote databases");
		}

		if (callback==null) {
			throw new IllegalArgumentException("Callback cannot be null");
		}

		Exception[] ex = new Exception[1];

		if (PlatformUtils.is64Bit()) {
			NotesCallbacks.b64_NSFDbNamedObjectEnumPROC cCallback = new NotesCallbacks.b64_NSFDbNamedObjectEnumPROC() {

				@Override
				public short invoke(long hDB, Pointer param, short nameSpaceShort, Pointer nameMem, short nameLength,
						IntByReference objectID, NotesTimeDateStruct entryTimeStruct) {

					if (objectID==null) {
						//skip some entries in the named object table because they are used internally
						//as duplicate lookup keys and cannot be queried via NSFDbGetNamedObjectID
						return 0;
					}
					if (nameSpaceShort!=NotesConstants.NONS_NAMED_NOTE) {
						//skip internal NSF structures
						return 0;
					}

					String name = NotesStringUtils.fromLMBCS(nameMem, (int) (nameLength & 0xffff));
					NotesTimeDate entryTimeDate = entryTimeStruct==null ? null : new NotesTimeDate(entryTimeStruct.Innards);

					try {
						Action action = callback.objectFound(name, objectID==null ? 0 : objectID.getValue(), entryTimeDate);
						if (action==Action.Stop) {
							return INotesErrorConstants.ERR_CANCEL;
						}
						else {
							return 0;
						}
					}
					catch (Exception e) {
						ex[0] = e;
						return INotesErrorConstants.ERR_CANCEL;
					}
				}
			};

			AccessController.doPrivileged(new PrivilegedAction<Object>() {
				@Override
				public Object run() {
					short result = NotesNativeAPI64.get().NSFDbNamedObjectEnum(m_hDB64, cCallback, null);
					if (result == INotesErrorConstants.ERR_CANCEL) {
						if (ex[0] != null) {
							throw new NotesError(0, "Error enumerating named objects", ex[0]);
						}
						return null;
					}
					NotesErrorUtils.checkResult(result);

					return null;
				}
			});
		}
		else {
			NotesCallbacks.b32_NSFDbNamedObjectEnumPROC cCallback;

			if (Platform.isWindows()) {
				cCallback = new Win32NotesCallbacks.NSFDbNamedObjectEnumPROCWin32() {

					@Override
					public short invoke(int hDB, Pointer param, short nameSpaceShort, Pointer nameMem, short nameLength,
							IntByReference objectID, NotesTimeDateStruct entryTimeStruct) {

						if (objectID==null) {
							//skip some entries in the named object table because they are used internally
							//as duplicate lookup keys and cannot be queried via NSFDbGetNamedObjectID
							return 0;
						}

						if (nameSpaceShort!=NotesConstants.NONS_NAMED_NOTE) {
							//skip internal NSF structures
							return 0;
						}

						String name = NotesStringUtils.fromLMBCS(nameMem, (int) (nameLength & 0xffff));
						NotesTimeDate entryTimeDate = entryTimeStruct==null ? null : new NotesTimeDate(entryTimeStruct.Innards);

						try {
							Action action = callback.objectFound(name, objectID==null ? 0 : objectID.getValue(), entryTimeDate);
							if (action==Action.Stop) {
								return INotesErrorConstants.ERR_CANCEL;
							}
							else {
								return 0;
							}
						}
						catch (Exception e) {
							ex[0] = e;
							return INotesErrorConstants.ERR_CANCEL;
						}

					}};
			}
			else {
				cCallback = new NotesCallbacks.b32_NSFDbNamedObjectEnumPROC() {

					@Override
					public short invoke(int hDB, Pointer param, short nameSpaceShort, Pointer nameMem, short nameLength,
							IntByReference objectID, NotesTimeDateStruct entryTimeStruct) {

						if (objectID==null) {
							//skip some entries in the named object table because they are used internally
							//as duplicate lookup keys and cannot be queried via NSFDbGetNamedObjectID
							return 0;
						}

						if (nameSpaceShort!=NotesConstants.NONS_NAMED_NOTE) {
							//skip internal NSF structures
							return 0;
						}

						String name = NotesStringUtils.fromLMBCS(nameMem, (int) (nameLength & 0xffff));
						NotesTimeDate entryTimeDate = entryTimeStruct==null ? null : new NotesTimeDate(entryTimeStruct.Innards);

						try {
							Action action = callback.objectFound(name, objectID==null ? 0 : objectID.getValue(), entryTimeDate);
							if (action==Action.Stop) {
								return INotesErrorConstants.ERR_CANCEL;
							}
							else {
								return 0;
							}
						}
						catch (Exception e) {
							ex[0] = e;
							return INotesErrorConstants.ERR_CANCEL;
						}

					}};
			}

			AccessController.doPrivileged(new PrivilegedAction<Object>() {
				@Override
				public Object run() {
					short result = NotesNativeAPI32.get().NSFDbNamedObjectEnum(m_hDB32, cCallback, null);
					if (result == INotesErrorConstants.ERR_CANCEL) {
						if (ex[0] != null) {
							throw new NotesError(0, "Error enumerating named objects", ex[0]);
						}
						return null;
					}
					NotesErrorUtils.checkResult(result);
					return null;
				}
			});

		}
	}

	/**
	 * Returns the RRV of a named object in the database
	 * 
	 * @param name name to look for
	 * @return rrv or 0 if not found
	 */
	private int getNamedObjectRRV(String name) {
		checkHandle();
		
		if (StringUtil.isEmpty(name)) {
			throw new IllegalArgumentException("Name cannot be empty");
		}

		boolean useLocalSearch;
		
		if (isRemote()) {
			//NSFDbNamedObjectEnum cannot be used in remote databases as of R11
			useLocalSearch = false;

			//enforce flag can be used to override this behavior if R12 supports remote searches
			if (Boolean.TRUE.equals(NotesGC.getCustomValue(propEnforceLocalNamedObjectSearch))) {
				useLocalSearch = true;
			}
		}
		else {
			useLocalSearch = true;
			
			//enforce flag can be used to select an NSFSearchExtended3 based locally
			//(mostly to compare both ways for testcases)
			if (Boolean.TRUE.equals(NotesGC.getCustomValue(propEnforceRemoteNamedObjectSearch))) {
				useLocalSearch = false;
			}
		}
		
		if (useLocalSearch) {
			Memory nameMem = NotesStringUtils.toLMBCS(name, false);
			
			IntByReference rtnObjectID = new IntByReference();
			
			short nsAsShort = NotesConstants.NONS_NAMED_NOTE | NotesConstants.NONS_NOASSIGN;
			
			short result;
			if (PlatformUtils.is64Bit()) {
				result = NotesNativeAPI64.get().NSFDbGetNamedObjectID(m_hDB64, nsAsShort, nameMem,
						(short) (nameMem.size() & 0xffff), rtnObjectID);
				if ((result & NotesConstants.ERR_MASK)==578) //special database object cannot be located
					return 0;
				NotesErrorUtils.checkResult(result);
			}
			else {
				result = NotesNativeAPI32.get().NSFDbGetNamedObjectID(m_hDB32, nsAsShort, nameMem,
						(short) (nameMem.size() & 0xffff), rtnObjectID);
				if ((result & NotesConstants.ERR_MASK)==578) //special database object cannot be located
					return 0;
				NotesErrorUtils.checkResult(result);
			}

			return rtnObjectID.getValue();
		}
		else {
			AtomicInteger result = new AtomicInteger();

			//use NSF search based approach for remote database, because
			//NSFDbGetNamedObjectID only works locally and there no exported
			//function for remote databases in nnotes.dll yet
			_getNamedObjectInfosRemote(new NamedObjectEnumCallback() {

				@Override
				public Action objectFound(String currName, int currObjectId,
						NotesTimeDate currEntryTime) {
					if (name.equalsIgnoreCase(currName)) {
						result.set(currObjectId);
						return Action.Stop;
					}
					else {
						return Action.Continue;
					}
				}
			});
			
			return result.get();
		}
	}
	
	/**
	 * Computes a $name value from category/objectkey similar to the name
	 * of profile notes, e.g. "$app_015calcolorprofile_" or "$app_015calcolorprofile_myobjectname"
	 * 
	 * @param category category part of primary key
	 * @param objectKey object key part of primary key
	 * @return note name
	 */
	static String getApplicationNoteName(String category, String objectKey) {
		// use a format similar to profile docs $name value,
		// e.g. "$app_015calcolorprofile_" or "$app_015calcolorprofile_myobjectname"
		String fullNodeName = (NAMEDNOTES_APPLICATION_PREFIX +
				StringUtil.pad(Integer.toString(category.length()), 3, '0', false) + category + "_" +
				objectKey)
				.toLowerCase(Locale.ENGLISH);
		
		return fullNodeName;
	}

	/**
	 * Uses an efficient NSF lookup mechanism to find a document that 
	 * matches the primary key specified with <code>category</code> and
	 * <code>objectKey</code>.
	 * 
	 * @param category category part of primary key
	 * @param objectId object id part of primary key
	 * @return note or null if not found
	 */
	public NotesNote openNoteByPrimaryKey(String category, String objectId) {
		String fullNodeName = getApplicationNoteName(category, objectId);
		int rrv = getNamedObjectRRV(fullNodeName);
		return rrv==0 ? null : openNoteById(rrv);
	}
	
	/**
	 * Parses a string like "$app_015calcolorprofile_myobjectname" into
	 * category and object id
	 * 
	 * @param name name
	 * @return array of [category,objectid] or null if unsupported format
	 */
	static String[] parseApplicationNamedNoteName(String name) {
		if (!name.startsWith(NAMEDNOTES_APPLICATION_PREFIX)) {
			return null;
		}
		
		String remainder = name.substring(5); //"$app_".length()
		if (remainder.length()<3) {
			return null;
		}
		
		String categoryNameLengthStr = remainder.substring(0, 3);
		int categoryNameLength = Integer.parseInt(categoryNameLengthStr);
		
		remainder = remainder.substring(3);
		String category = remainder.substring(0, categoryNameLength);
		
		remainder = remainder.substring(categoryNameLength+1);
		
		String objectKey = remainder;
		
		return new String[] {category, objectKey};
	}
	
	/**
	 * Returns the note id of all notes with assigned primary key
	 * (via {@link NotesNote#setPrimaryKey(String, String)}, hashed by their category value
	 * and object id
	 * 
	 * @return case insensitive lookup result, outer map with category as hash key, inner map with [objectid,noteid] entries
	 */
	public Map<String,Map<String,Integer>> getAllNotesByPrimaryKey() {
		Map<String,Map<String,Integer>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		getNamedObjects(new NamedObjectEnumCallback() {
			
			@Override
			public Action objectFound(String name, int objectId, NotesTimeDate entryTime) {
				//only read entries starting with $app_ , that way we skip internal NSF stuff
				if (objectId!=0 && StringUtil.startsWithIgnoreCase(name, NAMEDNOTES_APPLICATION_PREFIX)) {
					String[] parsedNamedNoteInfos = parseApplicationNamedNoteName(name);
					if (parsedNamedNoteInfos!=null) {
						String category = parsedNamedNoteInfos[0];
						String objectKey = parsedNamedNoteInfos[1];
						
						Map<String,Integer> entriesForCategory = result.get(category);
						if (entriesForCategory==null) {
							entriesForCategory = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
							result.put(category, entriesForCategory);
						}
						
						entriesForCategory.put(objectKey, objectId);
					}
				}
				
				return Action.Continue;
			}
		});
		
		return result;
	}
	
	/**
	 * Returns the note id of all notes with <code>category</code> in the assigned primary key
	 * (via {@link NotesNote#setPrimaryKey(String, String)}, hashed by their category value
	 * and object id
	 * 
	 * @param category category
	 * @return case insensitive result map with [objectid, noteid] entries
	 */
	public Map<String,Integer> getAllNotesByPrimaryKey(String category) {
		if (StringUtil.isEmpty(category)) {
			throw new IllegalArgumentException("Category cannot be empty");
		}
		String prefix = getApplicationNoteName(category, "");
		
		Map<String,Integer> result = new HashMap<>();
		
		getNamedObjects(new NamedObjectEnumCallback() {
			
			@Override
			public Action objectFound(String name, int rrv, NotesTimeDate entryTime) {
				if (rrv!=0 && StringUtil.startsWithIgnoreCase(name, prefix)) {
					String objectKey = name.substring(prefix.length()).toLowerCase(Locale.ENGLISH);
					result.put(objectKey, rrv);
				}
				
				return Action.Continue;
			}
		});
		
		return result;
	}

	/**
	 * This convenience function creates a new shared folder in the database with the default design.

	 * @param newFolderName Name of new folder.  Individual folder names are limited to DESIGN_FOLDER_MAX_NAME bytes (64).  If this is to be a cascading folder (subfolder), use a backslash character to separate the folder names, for example, "Parent Folder\\New Folder".  In this example, New Folder will be a subfolder of Parent Folder.  If Parent Folder does not exist, it will be created.
	 * @return note id of new folder
	 * @throws NotesError with status code 1144 if view already exists
	 */
	public int createFolder(String newFolderName) throws NotesError {
		return createFolder(this, 0, newFolderName);
	}
	
	/**
	 * This function creates a new shared folder in the database.

	 * @param formatNoteId Folder/view note with which to base the new folder's design.  Specify 0 to use the default design. The default design is specified in an existing folder's design. If there is no default design specified in an existing folder's design, then the default view note is used.
	 * @param newFolderName Name of new folder.  Individual folder names are limited to DESIGN_FOLDER_MAX_NAME bytes (64).  If this is to be a cascading folder (subfolder), use a backslash character to separate the folder names, for example, "Parent Folder\\New Folder".  In this example, New Folder will be a subfolder of Parent Folder.  If Parent Folder does not exist, it will be created.
	 * @return note id of new folder
	 * @throws NotesError with status code 1144 if view already exists
	 */
	public int createFolder(int formatNoteId, String newFolderName) throws NotesError {
		return createFolder(this, formatNoteId, newFolderName);
	}
	
	/**
	 * This function creates a new shared folder in the database.

	 * @param formatFolderName name of folder/view with which to base the new folder's design.  Specify ""/null to use the default design. The default design is specified in an existing folder's design. If there is no default design specified in an existing folder's design, then the default view note is used.
	 * @param newFolderName Name of new folder. Individual folder names are limited to DESIGN_FOLDER_MAX_NAME bytes (64).  If this is to be a cascading folder (subfolder), use a backslash character to separate the folder names, for example, "Parent Folder\\New Folder".  In this example, New Folder will be a subfolder of Parent Folder.  If Parent Folder does not exist, it will be created.
	 * @return note id of new folder
	 * @throws NotesError with status code 1144 if view already exists
	 */
	public int createFolder(String formatFolderName, String newFolderName) throws NotesError {
		return createFolder(null, formatFolderName, newFolderName);
	}
	
	/**
	 * This function creates a new shared folder in the database.

	 * @param formatDb database which contains the folder/view note with which to base the new folder's design. You may specify <code>null</code> if this is the same as this database.
	 * @param formatFolderName name of folder/view with which to base the new folder's design.  Specify ""/null to use the default design. The default design is specified in an existing folder's design. If there is no default design specified in an existing folder's design, then the default view note is used.
	 * @param newFolderName Name of new folder. Individual folder names are limited to DESIGN_FOLDER_MAX_NAME bytes (64).  If this is to be a cascading folder (subfolder), use a backslash character to separate the folder names, for example, "Parent Folder\\New Folder".  In this example, New Folder will be a subfolder of Parent Folder.  If Parent Folder does not exist, it will be created.
	 * @return note id of new folder
	 * @throws NotesError with status code 1144 if view already exists
	 */
	public int createFolder(NotesDatabase formatDb, String formatFolderName, String newFolderName) throws NotesError {
		if (StringUtil.isEmpty(formatFolderName)) {
			return createFolder(formatDb, 0, newFolderName);
		}
		
		int formatNoteId = formatDb==null ? findFolder(formatFolderName) : formatDb.findFolder(formatFolderName);
		if (formatNoteId==0) {
			formatNoteId = formatDb==null ? findView(formatFolderName) : formatDb.findView(formatFolderName);
		}
		if (formatNoteId==0) {
			throw new NotesError(1028, "No format view/folder found with name "+formatFolderName);
		}
		
		return createFolder(formatDb, formatNoteId, newFolderName);
	}
	
	/**
	 * This function creates a new shared folder in the database.

	 * @param formatDb database which contains the folder/view note with which to base the new folder's design. You may specify <code>null</code> if this is the same as this database.
	 * @param formatNoteId Folder/view note with which to base the new folder's design.  Specify 0 to use the default design. The default design is specified in an existing folder's design. If there is no default design specified in an existing folder's design, then the default view note is used.
	 * @param newFolderName Name of new folder.  Individual folder names are limited to DESIGN_FOLDER_MAX_NAME bytes (64).  If this is to be a cascading folder (subfolder), use a backslash character to separate the folder names, for example, "Parent Folder\\New Folder".  In this example, New Folder will be a subfolder of Parent Folder.  If Parent Folder does not exist, it will be created.
	 * @return note id of new folder
	 * @throws NotesError with status code 1144 if view already exists
	 */
	public int createFolder(NotesDatabase formatDb, int formatNoteId, String newFolderName) throws NotesError {
		checkHandle();
		
		if (formatDb!=null && formatDb.isRecycled()) {
			throw new NotesError("Folder design DB is recycled");
		}
		
		Memory newFolderNameMem = NotesStringUtils.toLMBCS(newFolderName, false);
		if (newFolderNameMem.size() > NotesConstants.DESIGN_FOLDER_MAX_NAME) {
			throw new IllegalArgumentException("Folder name too long (max "+NotesConstants.DESIGN_FOLDER_MAX_NAME+" bytes, found "+newFolderNameMem.size()+" bytes)");
		}

		short newFolderNameLength = (short) (newFolderNameMem.size() & 0xffff);
		
		IntByReference retNoteId = new IntByReference();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().FolderCreate(getHandle64(), getHandle64(), formatNoteId,
					formatDb==null ? 0 : formatDb.getHandle64(), newFolderNameMem, newFolderNameLength,
							NotesConstants.DESIGN_TYPE_SHARED, 0, retNoteId);
		}
		else {
			result = NotesNativeAPI32.get().FolderCreate(getHandle32(), getHandle32(), formatNoteId,
					formatDb==null ? 0 : formatDb.getHandle32(), newFolderNameMem, newFolderNameLength,
							NotesConstants.DESIGN_TYPE_SHARED, 0, retNoteId);
		}
		NotesErrorUtils.checkResult(result);

		return retNoteId.getValue();
	}
	
	/**
	 * This function deletes the given folder. Subfolders within this folder are also deleted.
	 * 
	 * @param folderName name of folder to be deleted.
	 */
	public void deleteFolder(String folderName) {
		int folderNoteId = findFolder(folderName);
		if (folderNoteId==0) {
			throw new NotesError(1028, "No source folder found with name "+folderName);
		}
		deleteFolder(folderNoteId);
	}
	
	/**
	 * This function deletes the given folder. Subfolders within this folder are also deleted.
	 * 
	 * @param folderNoteId note id of folder to be deleted.
	 */
	public void deleteFolder(int folderNoteId) {
		if (folderNoteId==0) {
			throw new IllegalArgumentException("Folder note id cannot be 0");
		}
		
		checkHandle();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().FolderDelete(m_hDB64, m_hDB64, folderNoteId, 0);
		}
		else {
			result = NotesNativeAPI32.get().FolderDelete(m_hDB32, m_hDB32, folderNoteId, 0);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * This function creates a new folder and copies the contents of the source folder to it.<br>
	 * Any subfolders are also copied.
	 * 
	 * @param sourceFolderName name of source folder.
	 * @param newFolderName Name for the new copy of the folder. Individual folder names are limited to DESIGN_FOLDER_MAX_NAME bytes (64). If this is to be a cascading folder (a subfolder), use a backslash character to separate the folder names, for example, "Parent Folder\\New Copy"  In this example, New Copy will be a subfolder of Parent Folder.  If Parent Folder does not exist, it will be created.
	 * @return note id of new folder
	 */
	public int copyFolder(String sourceFolderName, String newFolderName) {
		int sourceFolderNoteId = findFolder(sourceFolderName);
		if (sourceFolderNoteId==0) {
			throw new NotesError(1028, "No source folder found with name "+sourceFolderNoteId);
		}
		return copyFolder(sourceFolderNoteId, newFolderName);
	}
	
	/**
	 * This function creates a new folder and copies the contents of the source folder to it.<br>
	 * Any subfolders are also copied.
	 * 
	 * @param sourceFolderNoteId note id of source folder.
	 * @param newFolderName Name for the new copy of the folder. Individual folder names are limited to DESIGN_FOLDER_MAX_NAME bytes (64). If this is to be a cascading folder (a subfolder), use a backslash character to separate the folder names, for example, "Parent Folder\\New Copy"  In this example, New Copy will be a subfolder of Parent Folder.  If Parent Folder does not exist, it will be created.
	 * @return note id of new folder
	 */
	public int copyFolder(int sourceFolderNoteId, String newFolderName) {
		if (sourceFolderNoteId==0) {
			throw new IllegalArgumentException("Source folder note id cannot be 0");
		}
		
		checkHandle();
		
		Memory newFolderNameMem = NotesStringUtils.toLMBCS(newFolderName, false);
		if (newFolderNameMem.size() > NotesConstants.DESIGN_FOLDER_MAX_NAME) {
			throw new IllegalArgumentException("Folder name too long (max "+NotesConstants.DESIGN_FOLDER_MAX_NAME+" bytes, found "+newFolderNameMem.size()+" bytes)");
		}

		short newFolderNameLength = (short) (newFolderNameMem.size() & 0xffff);

		IntByReference retNewNoteId = new IntByReference();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().FolderCopy(m_hDB64, getHandle64(), sourceFolderNoteId,
					newFolderNameMem, newFolderNameLength, 0, retNewNoteId);
		}
		else {
			result = NotesNativeAPI32.get().FolderCopy(m_hDB32, getHandle32(), sourceFolderNoteId,
					newFolderNameMem, newFolderNameLength, 0, retNewNoteId);
		}
		NotesErrorUtils.checkResult(result);
		
		return retNewNoteId.getValue();
	}
	
	/**
	 * This function moves the specified folder under a given parent folder.<br>
	 * <br>
	 * If the parent folder is a shared folder, then the child folder must be a shared folder.<br>
	 * If the parent folder is a private folder, then the child folder must be a private folder.
	 * 
	 * @param folderName name of folder to be moved.
	 * @param targetParentFolderName name of the new parent folder.
	 */
	public void moveFolder(String folderName, String targetParentFolderName) {
		int folderNoteId = findFolder(folderName);
		if (folderNoteId==0) {
			throw new NotesError(1028, "No folder found with name "+folderName);
		}

		int targetParentFolderNoteId = findFolder(targetParentFolderName);
		if (targetParentFolderNoteId==0) {
			throw new NotesError(1028, "No folder found with name "+targetParentFolderName);
		}

		moveFolder(folderNoteId, targetParentFolderNoteId);
	}
	
	/**
	 * This function moves the specified folder under a given parent folder.<br>
	 * <br>
	 * If the parent folder is a shared folder, then the child folder must be a shared folder.<br>
	 * If the parent folder is a private folder, then the child folder must be a private folder.
	 * 
	 * @param folderNoteId note id of folder to be moved.
	 * @param targetParentFolderNoteId note id of the new parent folder.
	 */
	public void moveFolder(int folderNoteId, int targetParentFolderNoteId) {
		if (folderNoteId==0) {
			throw new IllegalArgumentException("Folder note id cannot be 0");
		}
		if (targetParentFolderNoteId==0) {
			throw new IllegalArgumentException("Target folder note id cannot be 0");
		}

		checkHandle();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().FolderMove(m_hDB64, 0, folderNoteId, 0,
					targetParentFolderNoteId, 0);
		}
		else {
			result = NotesNativeAPI32.get().FolderMove(m_hDB32, 0, folderNoteId, 0,
					targetParentFolderNoteId, 0);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * This function renames the specified folder and its subfolders.
	 * 
	 * @param oldFolderName name of folder to rename
	 * @param name new folder name
	 */
	public void renameFolder(String oldFolderName, String name) {
		int folderNoteId = findFolder(oldFolderName);
		if (folderNoteId==0) {
			throw new NotesError(1028, "No folder found with name "+oldFolderName);
		}
		
		renameFolder(folderNoteId, name);
	}
	
	/**
	 * This function renames the specified folder and its subfolders.
	 * 
	 * @param oldFolderNoteId note id of folder to rename
	 * @param name new folder name
	 */
	public void renameFolder(int oldFolderNoteId, String name) {
		if (oldFolderNoteId==0) {
			throw new IllegalArgumentException("Folder note id cannot be 0");
		}
		
		checkHandle();
		
		Memory pszName = NotesStringUtils.toLMBCS(name, false);
		if (pszName.size() > NotesConstants.DESIGN_FOLDER_MAX_NAME) {
			throw new IllegalArgumentException("Folder name too long (max "+NotesConstants.DESIGN_FOLDER_MAX_NAME+" bytes, found "+pszName.size()+" bytes)");
		}
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().FolderRename(m_hDB64, 0, oldFolderNoteId, pszName, (short) pszName.size(), 0);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().FolderRename(m_hDB32, 0, oldFolderNoteId, pszName, (short) pszName.size(), 0);
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * This function adds the document(s) specified in an ID Table to this folder.
	 * Throws an error if {@link NotesCollection#isFolder()} is <code>false</code>.
	 * 
	 * @param folderNoteId note id of folder
	 * @param idTable id table
	 * @throws NotesError with id 947 (Attempt to perform folder operation on non-folder note) if not a folder
	 */
	public void addToFolder(int folderNoteId, NotesIDTable idTable) {
		checkHandle();
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().FolderDocAdd(m_hDB64, 0, folderNoteId, idTable.getHandle64(), 0);
		}
		else {
			result = NotesNativeAPI32.get().FolderDocAdd(m_hDB32, 0, folderNoteId, idTable.getHandle32(), 0);
		}
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * This function adds the document(s) specified as note id set to this folder.
	 * Throws an error if {@link NotesCollection#isFolder()} is <code>false</code>.
	 * 
	 * @param folderNoteId note id of folder
	 * @param noteIds ids of notes to add
	 * @throws NotesError with id 947 (Attempt to perform folder operation on non-folder note) if not a folder
	 */
	public void addToFolder(int folderNoteId, Collection<Integer> noteIds) {
		checkHandle();
		
		NotesIDTable idTable = new NotesIDTable();
		try {
			idTable.addNotes(noteIds);
			addToFolder(folderNoteId, idTable);
		}
		finally {
			idTable.recycle();
		}
	}

	/**
	 * This function removes the document(s) specified in an ID Table from a folder.
	 * 
	 * @param folderNoteId note id of folder
	 * @param idTable id table
	 */
	public void removeFromFolder(int folderNoteId, NotesIDTable idTable) {
		checkHandle();
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().FolderDocRemove(m_hDB64, 0, folderNoteId, idTable.getHandle64(), 0);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().FolderDocRemove(m_hDB32, 0, folderNoteId, idTable.getHandle32(), 0);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function removes the document(s) specified as note id set from a folder.
	 * 
	 * @param folderNoteId note id of folder
	 * @param noteIds ids of notes to remove
	 */
	public void removeFromFolder(int folderNoteId, Set<Integer> noteIds) {
		checkHandle();
		
		NotesIDTable idTable = new NotesIDTable();
		try {
			idTable.addNotes(noteIds);
			removeFromFolder(folderNoteId, idTable);
		}
		finally {
			idTable.recycle();
		}
	}

	/**
	 * This function removes all documents from a specified folder.<br>
	 * <br>
	 * Subfolders and documents within the subfolders are not removed.
	 * 
	 * @param folderNoteId note id of folder
	 */
	public void removeAllFromFolder(int folderNoteId) {
		checkHandle();
		
		if (PlatformUtils.is64Bit()) {
			short result = NotesNativeAPI64.get().FolderDocRemoveAll(m_hDB64, 0, folderNoteId, 0);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = NotesNativeAPI32.get().FolderDocRemoveAll(m_hDB32, 0, folderNoteId, 0);
			NotesErrorUtils.checkResult(result);
		}
	}

	/**
	 * This function returns the number of entries in the specified folder's index.<br>
	 * <br>
	 * This is the number of documents plus the number of cateogories (if any) in the folder.<br>
	 * <br>
	 * Subfolders and documents in subfolders are not included in the count.
	 * 
	 * @param folderNoteId note id of folder
	 * @return count
	 */
	public int getFolderDocCount(int folderNoteId) {
		checkHandle();
		
		if (PlatformUtils.is64Bit()) {
			IntByReference pdwNumDocs = new IntByReference();
			short result = NotesNativeAPI64.get().FolderDocCount(m_hDB64, 0, folderNoteId, 0, pdwNumDocs);
			NotesErrorUtils.checkResult(result);
			return pdwNumDocs.getValue();
		}
		else {
			IntByReference pdwNumDocs = new IntByReference();
			short result = NotesNativeAPI32.get().FolderDocCount(m_hDB32, 0, folderNoteId, 0, pdwNumDocs);
			NotesErrorUtils.checkResult(result);
			return pdwNumDocs.getValue();
		}
	}

	/**
	 * Returns an id table of the folder content
	 * 
	 * @param folderNoteId note id of folder
	 * @param validateIds If set, return only "validated" noteIDs
	 * @return id table
	 */
	public NotesIDTable getIDTableForFolder(int folderNoteId, boolean validateIds) {
		checkHandle();
		
		if (PlatformUtils.is64Bit()) {
			LongByReference hTable = new LongByReference();
			short result = NotesNativeAPI64.get().NSFFolderGetIDTable(m_hDB64, m_hDB64, folderNoteId, validateIds ? NotesConstants.DB_GETIDTABLE_VALIDATE : 0, hTable);
			NotesErrorUtils.checkResult(result);
			return new NotesIDTable(hTable.getValue(), false);
		}
		else {
			IntByReference hTable = new IntByReference();
			short result = NotesNativeAPI32.get().NSFFolderGetIDTable(m_hDB32, m_hDB32, folderNoteId, validateIds ? NotesConstants.DB_GETIDTABLE_VALIDATE : 0, hTable);
			NotesErrorUtils.checkResult(result);
			return new NotesIDTable(hTable.getValue(), false);
		}
	}

	/**
	 * Creates a new empty/unselected {@link NotesNoteCollection} instance
	 * 
	 * @return note collection
	 */
	public NotesNoteCollection createNoteCollection() {
		return new NotesNoteCollection(this);
	}
	
	private NotesIDTable getAllDesignElements(NoteClass noteClass) {
		checkHandle();

		Ref<NotesIDTable> idTable = new Ref<>(new NotesIDTable());

		boolean designEnumSuccess = false;

		short result = 0;

		NotesCallbacks.b64_DESIGNENUMPROC callback64;
		NotesCallbacks.b32_DESIGNENUMPROC callback32;

		if (PlatformUtils.is64Bit()) {
			callback32 = null;
			
			callback64 = new NotesCallbacks.b64_DESIGNENUMPROC() {

				@Override
				public short invoke(Pointer routineParameter, long hDB, int noteID,
						NotesUniversalNoteIdStruct noteUNID, short noteClass, Pointer summary, int designType) {

					idTable.get().addNote(noteID);
					return 0;
				}
			};
		}
		else {
			callback64 = null;
			
			if (PlatformUtils.isWin32()) {
				callback32 = new Win32NotesCallbacks.DESIGNENUMPROCWin32() {

					@Override
					public short invoke(Pointer routineParameter, int hDB, int noteID,
							NotesUniversalNoteIdStruct noteUNID, short noteClass, Pointer summary, int designType) {

						idTable.get().addNote(noteID);
						return 0;
					}

				};
			}
			else {
				callback32 = new NotesCallbacks.b32_DESIGNENUMPROC() {

					@Override
					public short invoke(Pointer routineParameter, int hDB, int noteID,
							NotesUniversalNoteIdStruct noteUNID, short noteClass, Pointer summary, int designType) {

						idTable.get().addNote(noteID);
						return 0;
					}
				};
			}
		}

		if (noteClass == NoteClass.VIEW || noteClass == NoteClass.FILTER ||
				noteClass == NoteClass.FORM) {

			String designFlags;
			if (noteClass == NoteClass.VIEW) {
				designFlags = NotesConstants.DFLAGPAT_VIEWS_AND_FOLDERS_DESIGN;
			}
			else if (noteClass == NoteClass.FILTER) {
				designFlags = NotesConstants.DFLAGPAT_AGENTSLIST;
			}
			else {
				designFlags = NotesConstants.DFLAGPAT_VIEWFORM_ALL_VERSIONS;
			}
			Memory designFlagsMem = NotesStringUtils.toLMBCS(designFlags, true);

			if (PlatformUtils.is64Bit()) {
				result = AccessController.doPrivileged((PrivilegedAction<Short>) () -> {
					return NotesNativeAPI64.get().DesignEnum2(m_hDB64, (short) (noteClass.getValue() &0xffff),
							designFlagsMem,
							NotesConstants.DGN_ONLYSHARED, callback64, null, null, null);
				});

				if (result==0 && noteClass == NoteClass.FORM) {
					//add subforms
					Memory subformDesignFlagsMem = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_SUBFORM_ALL_VERSIONS, true);

					result = AccessController.doPrivileged((PrivilegedAction<Short>) () -> {
						return NotesNativeAPI64.get().DesignEnum2(m_hDB64, (short) (noteClass.getValue() &0xffff),
								subformDesignFlagsMem,
								NotesConstants.DGN_ONLYSHARED, callback64, null, null, null);
					});
				}

				if (result == 0) {
					designEnumSuccess = true;
				}
			}
			else {
				result = AccessController.doPrivileged((PrivilegedAction<Short>) () -> {
					return NotesNativeAPI32.get().DesignEnum2(m_hDB32, (short) (noteClass.getValue() &0xffff),
							designFlagsMem,
							NotesConstants.DGN_ONLYSHARED, callback32, null, null, null);
				});
				NotesErrorUtils.checkResult(result);

				if (result==0 && noteClass == NoteClass.FORM) {
					//add subforms
					Memory subformDesignFlagsMem = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_SUBFORM_ALL_VERSIONS, true);

					result = AccessController.doPrivileged((PrivilegedAction<Short>) () -> {
						return NotesNativeAPI32.get().DesignEnum2(m_hDB32, (short) (noteClass.getValue() &0xffff),
								subformDesignFlagsMem,
								NotesConstants.DGN_ONLYSHARED, callback32, null, null, null);
					});
					NotesErrorUtils.checkResult(result);
				}

				if (result == 0) {
					designEnumSuccess = true;
				}
			}

			if (result != INotesErrorConstants.ERR_NOACCESS) {
				NotesErrorUtils.checkResult(result);
			}
		}

		if (!designEnumSuccess) {
			idTable.get().recycle();

			// non-views, and v3 servers
			// first get all the public design notes
			DHANDLE hIDTable;
			if (PlatformUtils.is64Bit()) {
				LongByReference rethIDTable = new LongByReference();
				result = NotesNativeAPI64.get().DesignGetNoteTable(m_hDB64,
						(short) (noteClass.getValue() & 0xffff), rethIDTable);
				NotesErrorUtils.checkResult(result);
				hIDTable = DHANDLE64.newInstance(rethIDTable.getValue());
			}
			else {
				IntByReference rethIDTable = new IntByReference();
				result = NotesNativeAPI32.get().DesignGetNoteTable(m_hDB32,
						(short) (noteClass.getValue() & 0xffff), rethIDTable);
				NotesErrorUtils.checkResult(result);
				hIDTable = DHANDLE32.newInstance(rethIDTable.getValue());
			}

			idTable.set(new NotesIDTable(hIDTable, false));

			// if we're getting views, remove any navigator notes;
			// if we're doing agents, remove script libs

			if (noteClass == NoteClass.VIEW || noteClass == NoteClass.FILTER ||
					noteClass == NoteClass.FORM) {

				boolean isView = noteClass == NoteClass.VIEW;
				boolean isForm = noteClass == NoteClass.FORM;

				Set<Integer> noteIdsToRemove = new HashSet<>();

				for (Integer currNoteId : idTable.get()) {
					NotesNote currNote = null;
					try {
						currNote = openNoteById(currNoteId, EnumSet.of(OpenNote.SUMMARY));
					}
					catch (NotesError e) {
						//ignore
					}

					if (currNote!=null) {
						String flags = currNote.getItemValueString(NotesConstants.DESIGN_FLAGS);

						if (isView && flags.contains(NotesConstants.DESIGN_FLAG_VIEWMAP)) {
							noteIdsToRemove.add(currNoteId);
						}
						else if (isForm && (
								flags.contains(NotesConstants.DESIGN_FLAG_IMAGE_RESOURCE) ||
								flags.contains(NotesConstants.DESIGN_FLAG_WEBPAGE) ||
								flags.contains(NotesConstants.DESIGN_FLAG_JAVA_RESOURCE) ||
								flags.contains(NotesConstants.DESIGN_FLAG_FRAMESET) ||
								flags.contains(NotesConstants.DESIGN_FLAG_HTMLFILE) ||
								flags.contains(NotesConstants.DESIGN_FLAG_JSP) ||
								flags.contains(NotesConstants.DESIGN_FLAG_SACTIONS) ||
								flags.contains(NotesConstants.DESIGN_FLAG_STYLESHEET_RESOURCE)
								)) {
							noteIdsToRemove.add(currNoteId);
						}
						else if (!isView && !isForm && (
								flags.contains(NotesConstants.DESIGN_FLAG_SCRIPTLIB) ||
								flags.contains(NotesConstants.DESIGN_FLAG_DATABASESCRIPT) ||
								flags.contains(NotesConstants.DESIGN_FLAG_SITEMAP)
								)) {
							noteIdsToRemove.add(currNoteId);
						}
						currNote.recycle();
					}
				}
			}
		}

		// Now use a lower level one to get private notes

		String designFlags;

		/* this call might fail if the db is on a V3 server. Ignore ERR_NOACCESS return code */
		if (noteClass == NoteClass.VIEW) {
			designFlags = NotesConstants.DFLAGPAT_VIEWS_AND_FOLDERS;
		}
		else if (noteClass == NoteClass.FILTER) {
			designFlags = NotesConstants.DFLAGPAT_AGENTSLIST;
		}
		else {
			designFlags = null;
		}
		Memory designFlagsMem = designFlags==null ? null : NotesStringUtils.toLMBCS(designFlags, true);

		if (PlatformUtils.is64Bit()) {
			result = AccessController.doPrivileged((PrivilegedAction<Short>) ()->{
				return NotesNativeAPI64.get().DesignEnum2(m_hDB64, (short) (noteClass.getValue() & 0xffff),
						designFlagsMem, NotesConstants.DGN_ONLYPRIVATE | NotesConstants.DGN_ALLPRIVATE,
						callback64, null, null, null);
			});
		}
		else {
			result = AccessController.doPrivileged((PrivilegedAction<Short>) ()->{
				return NotesNativeAPI32.get().DesignEnum2(m_hDB32, (short) (noteClass.getValue() & 0xffff),
						designFlagsMem, NotesConstants.DGN_ONLYPRIVATE | NotesConstants.DGN_ALLPRIVATE,
						callback32, null, null, null);
			});
		}

		if (result==0 || result==INotesErrorConstants.ERR_NOACCESS) {
			return idTable.get();
		}
		NotesErrorUtils.checkResult(result);
		return new NotesIDTable(); //should not be reached, because checkResult throws an Exception
	}

	/**
	 * Returns the note ids of all forms and subforms
	 * 
	 * @return note ids
	 */
	public NotesIDTable getFormNoteIds() {
		return getAllDesignElements(NoteClass.FORM);
	}
	
	/**
	 * Returns a stream of database forms (lazily loaded)
	 * 
	 * @return forms
	 */
	public Stream<NotesForm> getForms() {
		NotesIDTable idTable = getFormNoteIds();
		return StreamSupport
				.stream(idTable.spliterator(), false)
				.map((noteId) -> {
					return openNoteById(noteId);
				})
				.filter((note) -> {
					return note!=null;
				})
				.map((note) -> {
					return new NotesForm(note);
				});
	}

	/**
	 * Finds a form or subform in a database given the form name
	 * 
	 * @param formName form name
	 * @return form or null if not found
	 */
	public NotesForm getForm(String formName) {
		if (StringUtil.isEmpty(formName)) {
			return null;
		}
		
		int formNoteId = findDesignNoteId(formName, NoteClass.FORM);
		if (formNoteId!=0) {
			NotesNote formNote = openNoteById(formNoteId);
			if (formNote!=null) {
				return new NotesForm(formNote);
			}
		}
		
		return null;
	}
	
	public interface EncryptionInfo {
		
		EncryptionState getState();
		
		Encryption getStrength();
		
	}
	
	/**
	 * Returns information about the current encryption strength and state of
	 * this database.
	 * 
	 * @return encryption info
	 */
	public EncryptionInfo getLocalEncryptionInfo() {
		checkHandle();
		HANDLE hDb = getHandle();
		
		IntByReference retState = new IntByReference();
		IntByReference retStrength = new IntByReference();
		
		short result = NotesNativeAPI.get().NSFDbLocalSecInfoGetLocal(hDb.getByValue(), retState, retStrength);
		NotesErrorUtils.checkResult(result);
		
		EncryptionState state = EncryptionState.valueOf(retState.getValue());
		Encryption strength = Encryption.valueOf(retStrength.getValue());
		
		return new EncryptionInfo() {

			@Override
			public EncryptionState getState() {
				return state;
			}

			@Override
			public Encryption getStrength() {
				return strength;
			}

			@Override
			public String toString() {
				return "EncryptionInfo [state="+getState()+", strength="+getStrength()+"]";
			}
			
		};
	}
	
	/**
	 * Changes the local encryption level/strength
	 * 
	 * @param encryption new encryption
	 * @param userName user to encrypt the database for; null/empty for current ID user (should be used in the Notes Client and in most cases on the server side as well)
	 */
	public void setLocalEncryptionInfo(Encryption encryption, String userName) {
		checkHandle();
		
		HANDLE hDb = getHandle();

		if (userName==null) {
			userName = "";
		}
		String userNameCanonical = NotesNamingUtils.toCanonicalName(userName);
		Memory userNameCanonicalMem = NotesStringUtils.toLMBCS(userNameCanonical, true);

		short option = NotesConstants.LSECINFOSET_MODIFY;
		if (encryption == Encryption.None) {
			option = NotesConstants.LSECINFOSET_CLEAR;
		}
		
		byte strengthAsByte = (byte) (encryption.getValue() & 0xff);
		
		short result = NotesNativeAPI.get().NSFDbLocalSecInfoSet(hDb.getByValue(), option, strengthAsByte, userNameCanonicalMem);
		short status = (short) (result & NotesConstants.ERR_MASK);
		if (status == INotesErrorConstants.ERR_LOCALSEC_NEEDCOMPACT) {
			return;
		}
		NotesErrorUtils.checkResult(result);
	}

	/**
	 * This function compresses a local database to remove the space left by deleting documents, freeing up disk space.
	 * Deletion stubs however, are left intact in the database.<br>
	 * <br>
	 * Calls {@link #compact(String, Set, Set)} with null values for the options internally.
	 * 
	 * @param pathName pathname of a Domino database. The directory part of the path may be omitted if the database is in the data directory.  The filename extension may be omitted if it is ".NSF".
	 * @return the original and compacted size of the NSF
	 */
	public static Pair<Double,Double> compact(String pathName) {
		return compact(pathName, null, null);
	}
	
	/**
	 * This function compresses a local database to remove the space left by deleting documents, freeing up disk space.
	 * Deletion stubs however, are left intact in the database.
	 * 
	 * @param pathName pathname of a Domino database. The directory part of the path may be omitted if the database is in the data directory.  The filename extension may be omitted if it is ".NSF".
	 * @param options See {@link DBCompact}. If no options are desired, this parameter may be set to null
	 * @param options2 See {@link DBCompact2}. If no options are desired, this parameter may be set to null
	 * @return the original and compacted size of the NSF
	 */
	public static Pair<Double,Double> compact(String pathName, Set<DBCompact> options, Set<DBCompact2> options2) {
		Memory pathNameMem = NotesStringUtils.toLMBCS(pathName, true);
		int optionsAsInt = DBCompact.toBitMask(options);
		int options2AsInt = DBCompact2.toBitMask(options2);
		
		DoubleByReference retOriginalSize = new DoubleByReference();
		DoubleByReference retCompactedSize = new DoubleByReference();
		
		short result = NotesNativeAPI.get().NSFDbCompactExtendedExt2(pathNameMem, optionsAsInt, options2AsInt,
				retOriginalSize, retCompactedSize);
		NotesErrorUtils.checkResult(result);
		
		return new Pair<>(retOriginalSize.getValue(), retCompactedSize.getValue());
	}

	/**
	 * This function gets the database creation class specified when the database was created.<br>
	 * <br>
	 * This function is useful for those API programs that need to determine whether the database was
	 * created specifically for alternate mail (in this case, {@link DBClass#ENCAPSMAILFILE} will be returned).<br>
	 * <br>
	 * However, for all other types of databases, there is no guarantee that the database matches the database
	 * creation class description.
	 * 
	 * @return database class
	 */
	public DBClass getDbClass() {
		checkHandle();
		
		HANDLE.ByValue hDb = getHandle().getByValue();
		ShortByReference retClass = new ShortByReference();
		short result = NotesNativeAPI.get().NSFDbClassGet(hDb, retClass);
		NotesErrorUtils.checkResult(result);

		return DBClass.toType(retClass.getValue() & 0xffff);
	}

	private String m_cachedUnreadTableUsernameCanonical;
	private NotesIDTable m_cachedUnreadTable;

	/**
	 * Checks if a NotesNote is in the unread table for the specified user.<br>
	 * <br>
	 * For performance reasons we internally cache the unread table and store the
	 * username.
	 * This cached table is reused if the username on subsequent calls is the same
	 * and recycled if it is different.
	 *
	 * @param userNameParam name if user in abbreviated or canonical format; if null, we
	 *                 use the {@link NotesDatabase} opener
	 * @param noteId   note id of document
	 * @return true if unread
	 */
	public boolean isNoteUnread(String userNameParam, int noteId) {
		String userName = StringUtil.isEmpty(userNameParam) ? getNamesList().get(0) : userNameParam;
		String userNameCanonical = NotesNamingUtils.toCanonicalName(userName);

		if (
				StringUtil.isEmpty(m_cachedUnreadTableUsernameCanonical) ||
				m_cachedUnreadTable==null ||
				m_cachedUnreadTable.isRecycled() ||
				!NotesNamingUtils.equalNames(userNameCanonical, m_cachedUnreadTableUsernameCanonical)) {

			if (m_cachedUnreadTable!=null) {
				m_cachedUnreadTable.recycle();
				m_cachedUnreadTable = null;
			}

			m_cachedUnreadTable = getUnreadNoteTable(userNameCanonical, true, true).orElse(null);
			m_cachedUnreadTableUsernameCanonical = userNameCanonical;
		}

		return m_cachedUnreadTable != null && m_cachedUnreadTable.contains(noteId);
	}

	/**
	 * An ID Table is created containing the list of unread notes in the
	 * database for the specified user.<br>
	 * <br>
	 * The argument {@code createIfNotAvailable} controls what action is to be
	 * performed
	 * if there is no list of unread notes for the specified user in the
	 * database.<br>
	 * <br>
	 * If no list is found and this flag is set to {@code false}, the method will
	 * return an empty {@link Optional}.<br>
	 * If this flag is set to {code true}, the list of unread notes will be
	 * created and all
	 * notes in the database will be added to the list.<br>
	 * <br>
	 * No coordination is performed between different users of the same
	 * database.<br>
	 * <br>
	 * If an application obtains a list of unread notes while another user is
	 * modifying the
	 * list, the changes made may not be visible to the application.<br>
	 * <br>
	 * Unread marks for each user are stored in the client desktop.dsk file and in
	 * the database.<br>
	 * <br>
	 * When a user closes a database (either through the Notes user interface or
	 * through an API program),
	 * the unread marks in the desktop.dsk file and in the database are synchronized
	 * so that
	 * they match.<br>
	 * Unread marks are not replicated when a database is replicated.<br>
	 * <br>
	 * Instead, when a user opens a replica of a database, the unread marks from the
	 * desktop.dsk
	 * file propagates to the replica database.<br>
	 *
	 * @param userNameParam             user for which to check unread marks (abbreviated
	 *                             or canonical format); use {@code null} for
	 *                             current {@link NotesDatabase} opener
	 * @param createIfNotAvailable {code true}: If the unread list for this user
	 *                             cannot be found on disk, return all note IDs.
	 *                             {@code false}: If the list cannot be found,
	 *                             return an empty {@link Optional}
	 * @param updateUnread         {@code true} to update unread marks,
	 *                             {@code false} to not update unread marks.
	 * @return an {@link Optional} describing the table of unread documents, or an
	 *         empty one if there is no table
	 */
	public Optional<NotesIDTable> getUnreadNoteTable(String userNameParam, boolean createIfNotAvailable, boolean updateUnread) {
		checkHandle();

		String userName = StringUtil.isEmpty(userNameParam) ? getNamesList().get(0) : userNameParam;
		String userNameCanonical = NotesNamingUtils.toCanonicalName(userName);

		Memory userNameCanonicalMem = NotesStringUtils.toLMBCS(userNameCanonical, false);
		if (userNameCanonicalMem.size() > 65535) {
			throw new IllegalArgumentException("Username exceeds max length of 65535 bytes");
		}
		short userNameLength = (short) (userNameCanonicalMem.size() & 0xffff);

		DHANDLE.ByReference rethUnreadList = DHANDLE.newInstanceByReference();

		HANDLE hDb = getHandle();

		short result = NotesNativeAPI.get().NSFDbGetUnreadNoteTable2(hDb.getByValue(), userNameCanonicalMem, userNameLength,
				createIfNotAvailable, updateUnread, rethUnreadList);

		NotesErrorUtils.checkResult(result);

		if (rethUnreadList.isNull()) {
			return Optional.empty();
		}
		else {
			//make the cached ID table reflect the latest DB changes
			result = NotesNativeAPI.get().NSFDbUpdateUnread(hDb.getByValue(), rethUnreadList.getByValue());
			NotesErrorUtils.checkResult(result);

			return Optional.of(new NotesIDTable(rethUnreadList, false));
		}
	}

	/**
	 * Method to apply changes to the unread note table
	 *
	 * @param userNameParam       user for which to update unread marks (abbreviated
	 *                            or canonical format); use {@code null} for current
	 *                            {@link NotesDatabase} opener
	 * @param noteIdToMarkRead    note ids to mark read (=remove from the unread
	 *                            table)
	 * @param noteIdsToMarkUnread note ids to mark unread (=add to the unread table)
	 */
	public void updateUnreadNoteTable(String userNameParam, Set<Integer> noteIdToMarkRead,
			Set<Integer> noteIdsToMarkUnread) {

		checkHandle();

		String userName = StringUtil.isEmpty(userNameParam) ? getNamesList().get(0) : userNameParam;
		String userNameCanonical = NotesNamingUtils.toCanonicalName(userName);

		Memory userNameCanonicalMem = NotesStringUtils.toLMBCS(userNameCanonical, false);
		if (userNameCanonicalMem.size() > 65535) {
			throw new IllegalArgumentException("Username exceeds max length of 65535 bytes");
		}
		short userNameLength = (short) (userNameCanonicalMem.size() & 0xffff);

		NotesIDTable unreadTable = getUnreadNoteTable(userNameCanonical, true, true).orElse(null);
		NotesIDTable unreadTableOrig;

		HANDLE hDb = getHandle();

		if (unreadTable != null) {
			//make the cached ID table reflect the latest DB changes
			short result = NotesNativeAPI.get().NSFDbUpdateUnread(hDb.getByValue(), unreadTable.getHandle().getByValue());
			NotesErrorUtils.checkResult(result);

			unreadTableOrig = (NotesIDTable) unreadTable.clone();
		}
		else {
			unreadTable = new NotesIDTable();

			unreadTableOrig = new NotesIDTable();
		}

		if (noteIdToMarkRead != null && !noteIdToMarkRead.isEmpty()) {
			unreadTable.asSet().removeAll(noteIdToMarkRead);
		}

		if (noteIdsToMarkUnread != null && !noteIdsToMarkUnread.isEmpty()) {
			unreadTable.asSet().addAll(noteIdsToMarkUnread);
		}

		short result = NotesNativeAPI.get().NSFDbSetUnreadNoteTable(hDb.getByValue(), userNameCanonicalMem, userNameLength,
				true, unreadTableOrig.getHandle().getByValue(), unreadTable.getHandle().getByValue());
		NotesErrorUtils.checkResult(result);

		unreadTable.recycle();
		unreadTableOrig.recycle();

		//remove cached unread table for this user that is used by isNoteUnread
		if (m_cachedUnreadTable!=null &&
				NotesNamingUtils.equalNames(userNameCanonical, m_cachedUnreadTableUsernameCanonical)) {
			m_cachedUnreadTable.recycle();
			m_cachedUnreadTable = null;
			m_cachedUnreadTableUsernameCanonical = null;
		}

	}
	
	/**
	 * This function gets the list of Address books in use locally or Domino Directories on a server.<br>
	 * <br>
	 * If a server is specified, and that server is configured to have a Directory Assistance database 
	 * (formerly referred to as the Master Address Book), then this function gets the list of Domino
	 * Directories (Server Address books)  from this database.<br>
	 * <br>
	 * In Domino and Notes Releases 4.6.x, it only includes Domino Name &amp; Address books and not any of
	 * the LDAP directories in the list.<br>
	 * <br>
	 * If no server is specified or if no Directory Assistance database is configured on the specified server,
	 * then this function uses the NAMES variable in the notes.ini file to construct the list of Address books.<br>
	 * <br>
	 * The NAMES variable defines the list of Domino Directories and Address books in use by Domino and Notes.<br>
	 * If the NAMES variable is missing, the default is "NAMES.NSF".<br>
	 * <br>
	 * For each name in the NAMES list, the used C API method checks that the database exists.
	 * If the databases exists, it adds the database path to the return list. If no named databases exists,
	 * we return an empty list.<br>
	 * 
	 * @param serverName Name of server. Specify NULL to get the list of Address books used on the local system.
	 * @return paths
	 */
	public static Set<String> getAddressBookPaths(String serverName) {
		Memory server = NotesStringUtils.toLMBCS(serverName, true);
		ShortByReference returnCount = new ShortByReference();
		ShortByReference returnLength = new ShortByReference();
		
		DHANDLE.ByReference hReturn = DHANDLE.newInstanceByReference();
		short result = NotesNativeAPI.get().NAMEGetAddressBooks(
			server,
			(short)0,
			returnCount,
			returnLength,
			hReturn
		);
		NotesErrorUtils.checkResult(result);
		
		Pointer ptr = Mem.OSLockObject(hReturn);
		try {
			int count = returnCount.getValue();
			Set<String> retList = new LinkedHashSet<>(count);
			
			Pointer strPtr = ptr.share(0);
			for(int i = 0; i < count; i++) {
				int strlen = NotesStringUtils.getNullTerminatedLength(strPtr);
				String path = NotesStringUtils.fromLMBCS(strPtr, strlen);
				if(StringUtil.isNotEmpty(path)) {
					retList.add(path);
				}
				
				strPtr = strPtr.share(strlen);
			}
			
			return retList;
		}
		finally {
			Mem.OSUnlockObject(hReturn);
			Mem.OSMemFree(hReturn.getByValue());
		}
	}
}
