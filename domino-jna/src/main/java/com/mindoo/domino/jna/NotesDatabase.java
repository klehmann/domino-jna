package com.mindoo.domino.jna;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mindoo.domino.jna.NotesCollection.SearchResult;
import com.mindoo.domino.jna.NotesDatabase.SignCallback.Action;
import com.mindoo.domino.jna.constants.AclFlag;
import com.mindoo.domino.jna.constants.AclLevel;
import com.mindoo.domino.jna.constants.CreateDatabase;
import com.mindoo.domino.jna.constants.DBClass;
import com.mindoo.domino.jna.constants.DBQuery;
import com.mindoo.domino.jna.constants.DatabaseOption;
import com.mindoo.domino.jna.constants.FTIndex;
import com.mindoo.domino.jna.constants.FTSearch;
import com.mindoo.domino.jna.constants.FileType;
import com.mindoo.domino.jna.constants.GetNotes;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.OpenCollection;
import com.mindoo.domino.jna.constants.OpenDatabase;
import com.mindoo.domino.jna.constants.OpenNote;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.constants.ReplicateOption;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.constants.UpdateNote;
import com.mindoo.domino.jna.dql.DQL;
import com.mindoo.domino.jna.dql.DQL.DQLTerm;
import com.mindoo.domino.jna.errors.FormulaCompilationError;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.INotesNativeAPI32;
import com.mindoo.domino.jna.internal.INotesNativeAPI64;
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
import com.mindoo.domino.jna.internal.Win32NotesCallbacks;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks.ABORTCHECKPROCWin32;
import com.mindoo.domino.jna.internal.structs.NotesBuildVersionStruct;
import com.mindoo.domino.jna.internal.structs.NotesDbReplicaInfoStruct;
import com.mindoo.domino.jna.internal.structs.NotesFTIndexStatsStruct;
import com.mindoo.domino.jna.internal.structs.NotesItemDefinitionTableExt;
import com.mindoo.domino.jna.internal.structs.NotesItemDefinitionTableLock;
import com.mindoo.domino.jna.internal.structs.NotesOriginatorIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.internal.structs.ReplExtensionsStruct;
import com.mindoo.domino.jna.internal.structs.ReplServStatsStruct;
import com.mindoo.domino.jna.transactions.ITransactionCallable;
import com.mindoo.domino.jna.transactions.Transactions;
import com.mindoo.domino.jna.utils.CaseInsensitiveStringComparator;
import com.mindoo.domino.jna.utils.ExceptionUtil;
import com.mindoo.domino.jna.utils.IDUtils;
import com.mindoo.domino.jna.utils.LegacyAPIUtils;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesIniUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils.Privileges;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.SignalHandlerUtil;
import com.mindoo.domino.jna.utils.SignalHandlerUtil.IBreakHandler;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
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
public class NotesDatabase implements IRecyclableNotesObject {
	private int m_hDB32;
	private long m_hDB64;
	private boolean m_noRecycleDb;
	private String m_asUserCanonical;
	private String m_server;
	private String[] m_paths;
	private String m_replicaID;
	private boolean m_loginAsIdOwner;
	NotesNamesList m_namesList;
	private Database m_legacyDbRef;
	private Integer m_openDatabaseId;
	private NotesACL m_acl;
	boolean m_passNamesListToDbOpen;
	private boolean m_passNamesListToViewOpen;
	private DbMode m_dbMode;
	
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
	}

	private NotesDatabase(int handle, String asUserCanonical, NotesNamesList namesList) {
		if (PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		
		m_hDB32 = handle;
		m_asUserCanonical = asUserCanonical;
		m_namesList = namesList;
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

		server = NotesNamingUtils.toCanonicalName(server);
		
		boolean isOnServer = IDUtils.isOnServer();
		
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
			m_namesList = NotesNamingUtils.buildNamesList(m_server, m_asUserCanonical);
		}
		
		//setting authenticated flag for the user is required when running on the server
		if (openFlags!=null && openFlags.contains(OpenDatabase.FULL_ACCESS)) {
			NotesNamingUtils.setPrivileges(m_namesList, EnumSet.of(Privileges.FullAdminAccess,
					Privileges.Authenticated));
		}
		else {
			NotesNamingUtils.setPrivileges(m_namesList, EnumSet.of(Privileges.Authenticated));
		}
		
		m_passNamesListToViewOpen = false;
		m_passNamesListToDbOpen = false;
		if (m_namesList!=null) {
			if ("".equals(server)) {
				//locally, we can open the DB as any user/group/role
				m_passNamesListToDbOpen = true;
			}
			else if (!m_loginAsIdOwner) {
				//if we should be opening the DB as another user, we need to pass the names ist;
				//this might produce ERR 22507: You are not listed as a trusted server
				m_passNamesListToDbOpen = true;
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
			NotesGC.__objectCreated(NotesDatabase.class, this);
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
			namesList = NotesNamingUtils.buildNamesList(m_server, userName);
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
		NotesNamesList namesList = NotesNamingUtils.buildNamesList(m_server, userName);
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
		NotesNamesList namesList = NotesNamingUtils.buildNamesList(m_server, userName);
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
		if (m_acl==null || m_acl.isFreed()) {
			short result;
			if (PlatformUtils.is64Bit()) {
				LongByReference rethACL = new LongByReference();
				result = NotesNativeAPI64.get().NSFDbReadACL(m_hDB64, rethACL);
				NotesErrorUtils.checkResult(result);
				m_acl = new NotesACL(this, rethACL.getValue());
				NotesGC.__memoryAllocated(m_acl);
			}
			else {
				IntByReference rethACL = new IntByReference();
				result = NotesNativeAPI32.get().NSFDbReadACL(m_hDB32, rethACL);
				NotesErrorUtils.checkResult(result);
				m_acl = new NotesACL(this, rethACL.getValue());
				NotesGC.__memoryAllocated(m_acl);
			}
		}
		return m_acl;
	}

	/**
	 * Locate and return name of template for a given template name.
	 * 
	 * @param templateName name of template to find
	 * @return path to DB which is the template for this template name
	 */
	public static String findDatabaseByTemplateName(String templateName) {
		return findDatabaseByTemplateName(templateName, (String) null);
	}
	
	/**
	 * Locate and return name of template for a given template name.
	 * 
	 * @param templateName name of template to find
	 * @param excludeDbPath optional db path to exclude during search
	 * @return path to DB which is the template for this template name or null if not found
	 */
	public static String findDatabaseByTemplateName(String templateName, String excludeDbPath) {
		Memory templateNameMem = NotesStringUtils.toLMBCS(templateName, true);
		Memory excludeDbPathMem = NotesStringUtils.toLMBCS(excludeDbPath==null ? "" : excludeDbPath, true);
		DisposableMemory retDbPath = new DisposableMemory(256);
		try {
			short result = NotesNativeAPI.get().DesignFindTemplate(templateNameMem,
					excludeDbPathMem, retDbPath);
			if ((result & NotesConstants.ERR_MASK)==1028) //entry not found in index
				return null;
			NotesErrorUtils.checkResult(result);
			
			return NotesStringUtils.fromLMBCS(retDbPath, -1);
		}
		finally {
			retDbPath.dispose();
		}
	}
	
	/**
	 * Searches for a database by its replica id in the data directory (and subdirectories) specified by this
	 * scanner instance. The method only uses the server specified for this scanner instance, not the directory.
	 * It always searches the whole directory.
	 * 
	 * @param server server to search db replica
	 * @param replicaId replica id to search for
	 * @return path to database matching this id or null if not found
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
				return null;
			
			NotesErrorUtils.checkResult(result);

			String retPathName = NotesStringUtils.fromLMBCS(retPathNameMem, -1);
			if (retPathName==null || retPathName.length()==0) {
				return null;
			}
			else {
				return retPathName;
			}
		}
		finally {
			dir.recycle();
		}
	}
	
	/** Available encryption strengths for database creation */
	public static enum Encryption {None, Simple, Medium, Strong};
	
	/**
	 * This function creates a new Domino database.
	 * 
	 * @param serverName server name, either canonical, abbreviated or common name
	 * @param filePath filepath to database
	 * @param dbClass specifies the class of the database created. See {@link DBClass} for classes that may be specified.
	 * @param forceCreation controls whether the call will overwrite an existing database of the same name. Set to TRUE to overwrite, set to FALSE not to overwrite.
	 * @param options database creation option flags.  See {@link CreateDatabase}
	 * @param encryption encryption strength
	 * @param maxFileSize optional.  Maximum file size of the database, in bytes.  In order to specify a maximum file size, use the database class, {@link DBClass#BY_EXTENSION} and use the option, {@link CreateDatabase#MAX_SPECIFIED}.
	 */
	public static void createDatabase(String serverName, String filePath, DBClass dbClass, boolean forceCreation, EnumSet<CreateDatabase> options, Encryption encryption, long maxFileSize) {
		String fullPath = NotesStringUtils.osPathNetConstruct(null, serverName, filePath);
		Memory fullPathMem = NotesStringUtils.toLMBCS(fullPath, true);
		
		byte encryptStrengthByte;
		switch (encryption) {
		case None:
			encryptStrengthByte = NotesConstants.DBCREATE_ENCRYPT_NONE;
			break;
		case Simple:
			encryptStrengthByte = NotesConstants.DBCREATE_ENCRYPT_SIMPLE;
			break;
		case Medium:
			encryptStrengthByte = NotesConstants.DBCREATE_ENCRYPT_MEDIUM;
			break;
		case Strong:
			encryptStrengthByte = NotesConstants.DBCREATE_ENCRYPT_STRONG;
			break;
			default:
				encryptStrengthByte = NotesConstants.DBCREATE_ENCRYPT_NONE;
		}
		
		short dbClassShort = dbClass.getValue();
		short optionsShort = CreateDatabase.toBitMask(options);
		short result = NotesNativeAPI.get().NSFDbCreateExtended(fullPathMem, dbClassShort, forceCreation, optionsShort, encryptStrengthByte, maxFileSize);
		NotesErrorUtils.checkResult(result);
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
			
			if (PlatformUtils.is64Bit()) {
				NotesNativeAPI64.get().NSFDbPathGet(m_hDB64, retCanonicalPathName, retExpandedPathName);
			}
			else {
				NotesNativeAPI32.get().NSFDbPathGet(m_hDB32, retCanonicalPathName, retExpandedPathName);
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
					short result = NotesNativeAPI32.get().NSFDbClose(m_hDB32);
					NotesErrorUtils.checkResult(result);
					NotesGC.__objectBeeingBeRecycled(NotesDatabase.class, this);
					m_hDB32=0;
					if (m_namesList!=null && !m_namesList.isFreed()) {
						m_namesList.free();
					}
				}
			}
			
			if (m_acl!=null && !m_acl.isFreed()) {
				m_acl.free();
				m_acl = null;
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
	NotesCollection openCollection(int viewNoteId, EnumSet<OpenCollection> openFlagSet)  {
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
	 * Lookup method to find a collection
	 * 
	 * @param collectionName collection name
	 * @return note id of collection or 0 if not found
	 */
	public int findCollection(String collectionName) {
		checkHandle();
		
		Memory viewNameLMBCS = NotesStringUtils.toLMBCS(collectionName, true);

		IntByReference viewNoteID = new IntByReference();
		viewNoteID.setValue(0);

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NIFFindDesignNoteExt(m_hDB64, viewNameLMBCS, NotesConstants.NOTE_CLASS_VIEW, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_VIEWS_AND_FOLDERS_DESIGN, true), viewNoteID, 0);
		}
		else {
			result = NotesNativeAPI32.get().NIFFindDesignNoteExt(m_hDB32, viewNameLMBCS, NotesConstants.NOTE_CLASS_VIEW, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_VIEWS_AND_FOLDERS_DESIGN, true), viewNoteID, 0);
		}
		
		if ((result & NotesConstants.ERR_MASK)==1028) { //view not found
			return 0;
		}
		
		//throws an error if view cannot be found:
		NotesErrorUtils.checkResult(result);

		return viewNoteID.getValue();
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
		
		EnumSet<FTSearch> searchOptions = EnumSet.of(FTSearch.RET_IDTABLE);
		if (filterIDTable!=null) {
			searchOptions.add(FTSearch.REFINE);
		}
		int searchOptionsBitMask = FTSearch.toBitMask(searchOptions);
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethSearch = new LongByReference();
			
			short result = NotesNativeAPI64.get().FTOpenSearch(rethSearch);
			NotesErrorUtils.checkResult(result);

			Memory queryLMBCS = NotesStringUtils.toLMBCS(query, true);
			IntByReference retNumDocs = new IntByReference();
			LongByReference rethResults = new LongByReference();
			
			result = NotesNativeAPI64.get().FTSearch(
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
			if (result==3874) { //no documents found
				result = NotesNativeAPI64.get().FTCloseSearch(rethSearch.getValue());
				NotesErrorUtils.checkResult(result);
				return new SearchResult(new NotesIDTable(), 0);
			}
			NotesErrorUtils.checkResult(result);

			result = NotesNativeAPI64.get().FTCloseSearch(rethSearch.getValue());
			NotesErrorUtils.checkResult(result);
			
			return new SearchResult(rethResults.getValue()==0 ? null : new NotesIDTable(rethResults.getValue(), false), retNumDocs.getValue());
		}
		else {
			IntByReference rethSearch = new IntByReference();
			
			short result = NotesNativeAPI32.get().FTOpenSearch(rethSearch);
			NotesErrorUtils.checkResult(result);

			Memory queryLMBCS = NotesStringUtils.toLMBCS(query, true);
			IntByReference retNumDocs = new IntByReference();
			IntByReference rethResults = new IntByReference();
			
			result = NotesNativeAPI32.get().FTSearch(
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
			if (result==3874) { //no documents found
				result = NotesNativeAPI32.get().FTCloseSearch(rethSearch.getValue());
				NotesErrorUtils.checkResult(result);
				return new SearchResult(new NotesIDTable(), 0);
			}
			NotesErrorUtils.checkResult(result);

			result = NotesNativeAPI32.get().FTCloseSearch(rethSearch.getValue());
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
		checkHandle();

		short noteClassMask = NoteClass.toBitMask(noteClassMaskEnum);
		
		//make sure retUntil is not null
		if (retUntil==null)
			retUntil = new NotesTimeDate();
		
		NotesTimeDateStruct sinceStruct = NotesTimeDateStruct.newInstance(since.getInnards());
		NotesTimeDateStruct.ByValue sinceStructByVal = NotesTimeDateStruct.ByValue.newInstance();
		sinceStructByVal.Innards[0] = sinceStruct.Innards[0];
		sinceStructByVal.Innards[1] = sinceStruct.Innards[1];
		sinceStructByVal.write();
		NotesTimeDateStruct retUntilStruct = NotesTimeDateStruct.newInstance(retUntil.getInnards());
		
		if (PlatformUtils.is64Bit()) {
			LongByReference rethTable = new LongByReference();
			short result = NotesNativeAPI64.get().NSFDbGetModifiedNoteTable(m_hDB64, noteClassMask, sinceStructByVal, retUntilStruct, rethTable);
			if (result == INotesErrorConstants.ERR_NO_MODIFIED_NOTES) {
				return new NotesIDTable();
			}
			NotesErrorUtils.checkResult(result);
			return new NotesIDTable(rethTable.getValue(), false);
		}
		else {
			IntByReference rethTable = new IntByReference();
			short result = NotesNativeAPI32.get().NSFDbGetModifiedNoteTable(m_hDB32, noteClassMask, sinceStructByVal, retUntilStruct, rethTable);
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
		NotesCollection designCol = openDesignCollection();

		List<NotesCollectionSummary> collections = designCol.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT),
				Integer.MAX_VALUE, EnumSet.of(ReadMask.SUMMARY, ReadMask.NOTEID, ReadMask.NOTECLASS),
				new NotesCollection.ViewLookupCallback<List<NotesCollectionSummary>>() {

			@Override
			public List<NotesCollectionSummary> startingLookup() {
				return new ArrayList<NotesCollectionSummary>();
			}

			@Override
			public Action entryRead(List<NotesCollectionSummary> result, NotesViewEntryData entryData) {
				if (entryData.getNoteClass() == 8) { // NoteClass.VIEW
					result.add(new NotesCollectionSummary(entryData));
				}
				return Action.Continue;
			}

			@Override
			public List<NotesCollectionSummary> lookupDone(List<NotesCollectionSummary> result) {
				return result;
			}

		});
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
			flagsPatternMem = NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_TOOLSRUNMACRO, true);
		}
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NIFFindDesignNoteExt(m_hDB64, nameMem, noteTypeShort, flagsPatternMem, retNoteID, 0);
		}
		else {
			result = NotesNativeAPI32.get().NIFFindDesignNoteExt(m_hDB32, nameMem, noteTypeShort, flagsPatternMem, retNoteID, 0);
		}
		
		if ((result & NotesConstants.ERR_MASK)==1028) {
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
			result = NotesNativeAPI64.get().NIFFindDesignNoteExt(m_hDB64, agentNameLMBCS, NotesConstants.NOTE_CLASS_FILTER, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_TOOLSRUNMACRO, true), retAgentNoteID, 0);
		}
		else {
			result = NotesNativeAPI32.get().NIFFindDesignNoteExt(m_hDB32, agentNameLMBCS, NotesConstants.NOTE_CLASS_FILTER, NotesStringUtils.toLMBCS(NotesConstants.DFLAGPAT_TOOLSRUNMACRO, true), retAgentNoteID, 0);
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
	 * Sign all documents of a specified note class (see NOTE_CLASS_* in {@link NotesConstants}.
	 * 
	 * @param noteClassesEnum bitmask of note classes to sign
	 * @param callback optional callback to get notified about signed notes or null
	 */
	public void signAll(EnumSet<NoteClass> noteClassesEnum, SignCallback callback) {
		checkHandle();

		int noteClasses = NoteClass.toBitMaskInt(noteClassesEnum);
		
		String signer = IDUtils.getIdUsername();
		
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
						if ( ((currNoteClass & NotesConstants.NOTE_CLASS_FORM)==NotesConstants.NOTE_CLASS_FORM) || 
								((currNoteClass & NotesConstants.NOTE_CLASS_INFO)==NotesConstants.NOTE_CLASS_INFO) ||
								((currNoteClass & NotesConstants.NOTE_CLASS_HELP)==NotesConstants.NOTE_CLASS_HELP) ||
								((currNoteClass & NotesConstants.NOTE_CLASS_FIELD)==NotesConstants.NOTE_CLASS_FIELD)) {
							
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
									if (NotesNamingUtils.equalNames(signer, currNoteSigner)) {
										//already signed by current user
										continue;
									}
									else {
										signRequired = true;
									}
								}
								
								if (callback!=null && !callback.shouldSign(currEntry, currNoteSigner)) {
									signRequired = false;
								}
								
								if (signRequired) {
									result = NotesNativeAPI64.get().NSFNoteSign(rethNote.getValue());
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
									if (signer.equalsIgnoreCase(currNoteSigner)) {
										//already signed by current user
										continue;
									}
								}
								
								if (callback!=null && !callback.shouldSign(currEntry, currNoteSigner)) {
									signRequired = false;
								}

								if (signRequired) {
									result = NotesNativeAPI32.get().NSFNoteSign(rethNote.getValue());
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
				retVersion.HotfixNumber, retVersion.Flags, retVersion.FixpackNumber, retVersion.Spare);
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
	 * Use {@link NotesNote#update(EnumSet)} to restore this "soft deleted" note.

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
	public NotesNote openNoteById(int noteId, EnumSet<OpenNote> openFlags) {
		checkHandle();

		int openFlagsBitmask = OpenNote.toBitMaskForOpenExt(openFlags);
		
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

		int openFlagsBitmask = OpenNote.toBitMaskForOpenExt(openFlags);
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
			arrNoteOpenFlagsMem.setInt(4*i, OpenNote.toBitMaskForOpenExt(noteOpenFlags[i]));
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

		ReplServStatsStruct retStatsStruct = new ReplServStatsStruct();
		ReplExtensionsStruct extendedOptions = new ReplExtensionsStruct();
		extendedOptions.Size = 2 + 2;
		extendedOptions.TimeLimit = (short) (timeLimitMin & 0xffff);
		
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
		checkHandle();
		
		NotesNamesList namesList = null;
		if (m_namesList!=null) {
			List<String> namesListEntries = m_namesList.getNames();
			namesList = NotesNamingUtils.writeNewNamesList(namesListEntries);
		}

		NotesDatabase dbNew;
		short result;
		if (PlatformUtils.is64Bit()) {
			LongByReference retDbHandle = new LongByReference();
			result = NotesNativeAPI64.get().NSFDbReopen(m_hDB64, retDbHandle);
			NotesErrorUtils.checkResult(result);
			
			long newDbHandle = retDbHandle.getValue();
			dbNew = new NotesDatabase(newDbHandle, m_asUserCanonical, namesList);
		}
		else {
			IntByReference retDbHandle = new IntByReference();
			result = NotesNativeAPI32.get().NSFDbReopen(m_hDB32, retDbHandle);
			NotesErrorUtils.checkResult(result);
			
			int newDbHandle = retDbHandle.getValue();
			dbNew = new NotesDatabase(newDbHandle, m_asUserCanonical, namesList);
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
		
		String idUserName = IDUtils.getIdUsername();
		boolean isOnServer = IDUtils.isOnServer();
		
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
		NavigableMap<String,Integer> table = new TreeMap<String,Integer>(new CaseInsensitiveStringComparator());
		
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
					DisposableMemory retItemNamePtr = new DisposableMemory(Pointer.SIZE);
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
					DisposableMemory retItemNamePtr = new DisposableMemory(Pointer.SIZE);
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
	public NotesProfileNote getProfileNote(String profileName) {
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
	public NotesProfileNote getProfileNote(String profileName, String userName) {
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
			NotesProfileNote profileNote = new NotesProfileNote(this, hNote);
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
			NotesProfileNote profileNote = new NotesProfileNote(this, hNote);
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
	
}
