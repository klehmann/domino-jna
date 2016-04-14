package com.mindoo.domino.jna.internal;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import com.mindoo.domino.jna.structs.NotesBuildVersion;
import com.mindoo.domino.jna.structs.NotesCollectionPosition;
import com.mindoo.domino.jna.structs.NotesDbReplicaInfo;
import com.mindoo.domino.jna.structs.NotesFTIndexStats;
import com.mindoo.domino.jna.structs.NotesItem;
import com.mindoo.domino.jna.structs.NotesItemTable;
import com.mindoo.domino.jna.structs.NotesItemValueTable;
import com.mindoo.domino.jna.structs.NotesNumberPair;
import com.mindoo.domino.jna.structs.NotesOriginatorId;
import com.mindoo.domino.jna.structs.NotesRange;
import com.mindoo.domino.jna.structs.NotesSearchMatch32;
import com.mindoo.domino.jna.structs.NotesSearchMatch64;
import com.mindoo.domino.jna.structs.NotesTime;
import com.mindoo.domino.jna.structs.NotesTimeDate;
import com.mindoo.domino.jna.structs.NotesTimeDatePair;
import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Extract of Notes C API constants and functions converted to JNA calls
 * 
 * @author Karsten Lehmann
 */
public interface NotesCAPI extends Library {
	//computation of data type sizes for the current platform
	public final int timeDateSize = new NotesTimeDate().size();
	public final int rangeSize = new NotesRange().size();
	public final int timeSize = new NotesTime().size();
	public final int numberPairSize = new NotesNumberPair().size();
	public final int timeDatePairSize = new NotesTimeDatePair().size();
	public final int collectionPositionSize = new NotesCollectionPosition().size();
	public final int itemValueTableSize = new NotesItemValueTable().size();
	public final int itemSize = new NotesItem().size();
	public final int oidSize = new NotesOriginatorId().size();
	
	public static final short MAXALPHATIMEDATE = 80;

	public static final short ERR_MASK = 0x3fff;

	/*	Defines for Authentication flags */

	public static final short NAMES_LIST_AUTHENTICATED = 0x0001;	/* 	Set if names list has been 	*/
														/*	authenticated via Notes		*/
	public static final short NAMES_LIST_PASSWORD_AUTHENTICATED = 0x0002;	/* 	Set if names list has been 	*/
														/*	authenticated using external */
														/*	password -- Triggers "maximum */
														/*	password access allowed" feature */
	public static final short NAMES_LIST_FULL_ADMIN_ACCESS = 0x0004;	/* 	Set if user requested full admin access and it was granted */

	//	WORD LNPUBLIC OSLoadString(
	//			HMODULE  hModule,
	//			STATUS  StringCode,
	//			char far *retBuffer,
	//			WORD  BufferLength);
	public short b32_OSLoadString(int hModule, short StringCode, Memory retBuffer, short BufferLength);
	public short b64_OSLoadString(long hModule, short StringCode, Memory retBuffer, short BufferLength);

	short b32_NSFDbOpen(Memory dbName, IntBuffer dbHandle);
	short b64_NSFDbOpen(Memory dbName, LongBuffer dbHandle);
	
	short b32_NSFDbOpenExtended (Memory PathName, short Options, int hNames, NotesTimeDate ModifiedTime, IntBuffer rethDB, NotesTimeDate retDataModified, NotesTimeDate retNonDataModified);
	short b64_NSFDbOpenExtended (Memory PathName, short Options, long hNames, NotesTimeDate ModifiedTime, LongBuffer rethDB, NotesTimeDate retDataModified, NotesTimeDate retNonDataModified);
	
	short b32_NSFDbClose(int dbHandle);
	short b64_NSFDbClose(long dbHandle);

	short b32_NSFBuildNamesList(Memory UserName, int dwFlags, IntByReference rethNamesList);
	short b64_NSFBuildNamesList(Memory UserName, int dwFlags, LongByReference rethNamesList);

	short b32_NSFDbSpaceUsage(int dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes);
	short b64_NSFDbSpaceUsage(long dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes);

	short b32_NSFDbDeleteNotes(int  hDB, int  hTable, Memory retUNIDArray);
	short b64_NSFDbDeleteNotes(long hDB, long hTable, Memory retUNIDArray);

	/*  Replication flags

	NOTE:  Please note the distinction between REPLFLG_DISABLE and
	REPLFLG_NEVER_REPLICATE.  The former is used to temporarily disable
	replication.  The latter is used to indicate that this database should
	NEVER be replicated.  The former may be set and cleared by the Notes
	user interface.  The latter is intended to be set programmatically
	and SHOULD NEVER be able to be cleared by the user interface.

	The latter was invented to avoid having to set the replica ID to
	the known value of REPLICA_ID_NEVERREPLICATE.  This latter method has
	the failing that DBs that use it cannot have DocLinks to them.  */

	/*								0x0001	spare was COPY_ACL */
	/*								0x0002	spare */

	/** Disable replication */
	public short REPLFLG_DISABLE = 0x0004;
	/** Mark unread only if newer note */
	public short REPLFLG_UNREADIFFNEW = 0x0008;
	/** Don't propagate deleted notes when replicating from this database */
	public short REPLFLG_IGNORE_DELETES = 0x0010;
	/** UI does not allow perusal of Design */
	public short REPLFLG_HIDDEN_DESIGN = 0x0020;
	/** Do not list in catalog */
	public short REPLFLG_DO_NOT_CATALOG	= 0x0040;
	/** Auto-Delete documents prior to cutoff date */
	public short REPLFLG_CUTOFF_DELETE = 0x0080;
	/** DB is not to be replicated at all */
	public short REPLFLG_NEVER_REPLICATE = 0x0100;
	/** Abstract during replication */
	public short REPLFLG_ABSTRACT = 0x0200;
	/** Do not list in database add */
	public short REPLFLG_DO_NOT_BROWSE = 0x0400;
	/** Do not run chronos on database */
	public short REPLFLG_NO_CHRONOS	= 0x0800;
	/** Don't replicate deleted notes into destination database */
	public short REPLFLG_IGNORE_DEST_DELETES = 0x1000;
	/** Include in Multi Database indexing */
	public short REPLFLG_MULTIDB_INDEX = 0x2000;
	/** Low priority */
	public short REPLFLG_PRIORITY_LOW = (short) (0xC000 & 0xffff);
	/** Medium priority */
	public short REPLFLG_PRIORITY_MED = 0x0000;
	/** High priority */
	public short REPLFLG_PRIORITY_HI = 0x4000;
	/** Shift count for priority field */
	public short REPLFLG_PRIORITY_SHIFT	= 14;
	/** Mask for priority field after shifting*/
	public short REPLFLG_PRIORITY_MASK = 0x0003;
	/** Mask for clearing the field */
	public short REPLFLG_PRIORITY_INVMASK = 0x3fff;
	public short REPLFLG_USED_MASK = (short) ((0x4|0x8|0x10|0x40|0x80|0x100|0x200|0xC000|0x1000|0x2000|0x4000) & 0xffff);


	/** Reserved ReplicaID.Date. Used in ID.Date field in ReplicaID to escape
	to reserved REPLICA_ID_xxx */
	public short REPLICA_DATE_RESERVED = 0;


	/**	Number of times within cutoff interval that we purge deleted stubs.
	For example, if the cutoff interval is 90 days, we purge every 30
	days. */
	public short CUTOFF_CHANGES_DURING_INTERVAL = 3;

	public short b32_NSFDbReplicaInfoGet(
			int  hDB,
			NotesDbReplicaInfo retReplicationInfo);

	public short b64_NSFDbReplicaInfoGet(
			long  hDB,
			NotesDbReplicaInfo retReplicationInfo);
	
	public short b32_NSFDbReplicaInfoSet(
			int  hDB,
			NotesDbReplicaInfo ReplicationInfo);

	public short b64_NSFDbReplicaInfoSet(
			long  hDB,
			NotesDbReplicaInfo ReplicationInfo);

	short b32_NIFFindDesignNoteExt(int hFile, Memory name, short noteClass, Memory pszFlagsPattern, IntBuffer retNoteID, int Options);
	short b64_NIFFindDesignNoteExt(long hFile, Memory name, short noteClass, Memory pszFlagsPattern, IntBuffer retNoteID, int Options);

	short b32_NIFOpenCollection(int hViewDB, int hDataDB, int ViewNoteID, short OpenFlags, int hUnreadList, IntByReference rethCollection, IntByReference rethViewNote, Memory retViewUNID,
			IntByReference rethCollapsedList, IntByReference rethSelectedList);
	short b64_NIFOpenCollection(long hViewDB, long hDataDB, int ViewNoteID, short OpenFlags, long hUnreadList, LongByReference rethCollection, LongByReference rethViewNote, Memory retViewUNID,
			LongByReference rethCollapsedList, LongByReference rethSelectedList);

	short b32_NIFOpenCollectionWithUserNameList (int hViewDB, int hDataDB,
			int ViewNoteID, short OpenFlags,
			int hUnreadList,
			IntByReference rethCollection,
			IntByReference rethViewNote, Memory retViewUNID,
			IntByReference rethCollapsedList,
			IntByReference rethSelectedList,
			int nameList);
	
	short b64_NIFOpenCollectionWithUserNameList (long hViewDB, long hDataDB,
			int ViewNoteID, short OpenFlags,
			long hUnreadList,
			LongByReference rethCollection,
			LongByReference rethViewNote, Memory retViewUNID,
			LongByReference rethCollapsedList,
			LongByReference rethSelectedList,
			long nameList);
	
	short b32_NIFReadEntries(int hCollection, NotesCollectionPosition IndexPos, short SkipNavigator, int SkipCount, short ReturnNavigator, int ReturnCount, int ReturnMask, IntByReference rethBuffer,
			ShortByReference retBufferLength, IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned, ShortByReference retSignalFlags);
	short b64_NIFReadEntries(long hCollection, NotesCollectionPosition IndexPos, short SkipNavigator, int SkipCount, short ReturnNavigator, int ReturnCount, int ReturnMask, LongByReference rethBuffer,
			ShortByReference retBufferLength, IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned, ShortByReference retSignalFlags);

	short b32_NIFFindByKey(int hCollection, Memory keyBuffer, short findFlags, NotesCollectionPosition retIndexPos, IntByReference retNumMatches);
	short b64_NIFFindByKey(long hCollection, Memory keyBuffer, short findFlags, NotesCollectionPosition retIndexPos, IntByReference retNumMatches);

	short b32_NIFFindByName(int hCollection, Memory name, short findFlags, NotesCollectionPosition retIndexPos, IntByReference retNumMatches);
	short b64_NIFFindByName(long hCollection, Memory name, short findFlags, NotesCollectionPosition retIndexPos, IntByReference retNumMatches);

	short b32_NIFGetCollation(int hCollection, ShortByReference retCollationNum);
	short b64_NIFGetCollation(long hCollection, ShortByReference retCollationNum);

	short b32_NIFSetCollation(int hCollection, short CollationNum);
	short b64_NIFSetCollation(long hCollection, short CollationNum);

	short b32_NIFUpdateCollection(int hCollection);
	short b64_NIFUpdateCollection(long hCollection);
	
	void b32_NIFGetLastModifiedTime(int hCollection, NotesTimeDate retLastModifiedTime);
	void b64_NIFGetLastModifiedTime(long hCollection, NotesTimeDate retLastModifiedTime);
	
	short b32_NIFFindByKeyExtended2 (int hCollection, Memory keyBuffer,
			int findFlags,
			int returnFlags,
			NotesCollectionPosition retIndexPos,
			IntByReference retNumMatches,
			ShortByReference retSignalFlags,
			IntByReference rethBuffer,
			IntByReference retSequence);

	short b64_NIFFindByKeyExtended2 (long hCollection, Memory keyBuffer,
			int findFlags,
			int returnFlags,
			NotesCollectionPosition retIndexPos,
			IntByReference retNumMatches,
			ShortByReference retSignalFlags,
			LongByReference rethBuffer,
			IntByReference retSequence);

	
	short b32_NSFDbGetModifiedNoteTable(int hDB, short NoteClassMask, NotesTimeDate Since, NotesTimeDate retUntil, IntByReference rethTable);
	short b64_NSFDbGetModifiedNoteTable(long hDB, short NoteClassMask, NotesTimeDate Since, NotesTimeDate retUntil, LongByReference rethTable);
	
	short DNCanonicalize(int Flags, Memory TemplateName, Memory InName, Memory OutName, short OutSize, ShortByReference OutLength);
	short DNAbbreviate(int Flags, Memory TemplateName, Memory InName, Memory OutName, short OutSize, ShortByReference OutLength);	
	

	short b32_NIFCloseCollection(int hCollection);
	short b64_NIFCloseCollection(long hCollection);

	Pointer b32_OSLockObject(int handle);
	Pointer b64_OSLockObject(long handle);

	boolean b32_OSUnlockObject(int handle);
	boolean b64_OSUnlockObject(long handle);

	public short b32_OSMemFree(int handle);
	public short b64_OSMemFree(long handle);

	public void ODSWriteMemory(
			Pointer ppDest,
			short  type,
			Pointer pSrc,
			short  iterations);
	
	public void ODSReadMemory(
			Pointer ppSrc,
			short  type,
			Pointer pDest,
			short iterations);
	
	public short b32_NIFLocateNote (int hCollection, NotesCollectionPosition indexPos, int noteID);
	public short b64_NIFLocateNote (long hCollection, NotesCollectionPosition indexPos, int noteID);


	/**
	 * If the following is ORed in with a note class, the resultant note ID
	 * may be passed into NSFNoteOpen and may be treated as though you first
	 * did an NSFGetSpecialNoteID followed by an NSFNoteOpen, all in a single
	 * transaction.
	 */
	public static final int NOTE_ID_SPECIAL = 0xFFFF0000;
	
	/*	Note Classifications */
	/*	If NOTE_CLASS_DEFAULT is ORed with another note class, it is in
		essence specifying that this is the default item in this class.  There
		should only be one DEFAULT note of each class that is ever updated,
		although nothing in the NSF machinery prevents the caller from adding
		more than one.  The file header contains a table of the note IDs of
		the default notes (for efficient access to them).  Whenever a note
		is updated that has the default bit set, the reference in the file
		header is updated to reflect that fact.
		WARNING: NOTE_CLASS_DOCUMENT CANNOT have a "default".  This is precluded
		by code in NSFNoteOpen to make it fast for data notes. 
	*/

	/** document note */
	public static final short NOTE_CLASS_DOCUMENT = 0x0001;
	/** old name for document note */
	public static final short NOTE_CLASS_DATA = NOTE_CLASS_DOCUMENT;
	/** notefile info (help-about) note */
	public static final short NOTE_CLASS_INFO = 0x0002;
	/** form note */
	public static final short NOTE_CLASS_FORM = 0x0004;
	/** view note */
	public static final short NOTE_CLASS_VIEW = 0x0008;
	/** icon note */
	public static final short NOTE_CLASS_ICON = 0x0010;
	/** design note collection */
	public static final short NOTE_CLASS_DESIGN = 0x0020;
	/** acl note */
	public static final short NOTE_CLASS_ACL = 0x0040;
	/** Notes product help index note */
	public static final short NOTE_CLASS_HELP_INDEX = 0x0080;
	/** designer's help note */
	public static final short NOTE_CLASS_HELP = 0x0100;
	/** filter note */
	public static final short NOTE_CLASS_FILTER = 0x0200;
	/** field note */
	public static final short NOTE_CLASS_FIELD = 0x0400;
	/** replication formula */
	public static final short NOTE_CLASS_REPLFORMULA = 0x0800;
	/** Private design note, use $PrivateDesign view to locate/classify */
	public static final short NOTE_CLASS_PRIVATE = 0x1000;
	/** MODIFIER - default version of each */
	public static final short NOTE_CLASS_DEFAULT = (short) (0x8000 & 0xffff);
	/** see SEARCH_NOTIFYDELETIONS */
	public static final short NOTE_CLASS_NOTIFYDELETION	= NOTE_CLASS_DEFAULT;
	/** all note types */
	public static final short NOTE_CLASS_ALL = 0x7fff;
	/** all non-data notes */
	public static final short NOTE_CLASS_ALLNONDATA = 0x7ffe;
	/** no notes */
	public static final short NOTE_CLASS_NONE = 0x0000;
	
	/** Define symbol for those note classes that allow only one such in a file */
	public static final short NOTE_CLASS_SINGLE_INSTANCE =(
			NOTE_CLASS_DESIGN |
			NOTE_CLASS_ACL |
			NOTE_CLASS_INFO |
			NOTE_CLASS_ICON |
			NOTE_CLASS_HELP_INDEX |
			0);

/*	Note flag definitions */
	
	/** signed */
	public static final short NOTE_SIGNED = 0x0001;
	/** encrypted */
	public static final short NOTE_ENCRYPTED = 0x0002;

/*	Open Flag Definitions.  These flags are passed to NSFNoteOpen. */
	/** open only summary info */
	public static final short OPEN_SUMMARY = 0x0001;
	/** don't bother verifying default bit */
	public static final short OPEN_NOVERIFYDEFAULT = 0x0002;
	/** expand data while opening */
	public static final short OPEN_EXPAND = 0x0004;
	/** don't include any objects */
	public static final short OPEN_NOOBJECTS = 0x0008;
	/** open in a "shared" memory mode */
	public static final short OPEN_SHARE = 0x0020;
	/** Return ALL item values in canonical form */
	public static final short OPEN_CANONICAL = 0x0040;
	/** Mark unread if unread list is currently associated */
	public static final short OPEN_MARK_READ = 0x0100;
	/** Only open an abstract of large documents */
	public static final short OPEN_ABSTRACT =0x0200;
	/** Return response ID table */
	public static final short OPEN_RESPONSE_ID_TABLE = 0x1000;
	/** Include folder objects - default is not to */
	public static final int OPEN_WITH_FOLDERS	= 0x00020000;
	/** If set, leave TYPE_RFC822_TEXT items in native
	format.  Otherwise, convert to TYPE_TEXT/TYPE_TIME. */
	public static final int OPEN_RAW_RFC822_TEXT = 0x01000000;
	/** If set, leave TYPE_MIME_PART items in native
	format.  Otherwise, convert to TYPE_COMPOSITE. */
	public static final int OPEN_RAW_MIME_PART = 0x02000000;
	public static final int OPEN_RAW_MIME = (OPEN_RAW_RFC822_TEXT | OPEN_RAW_MIME_PART);

/*	Update Flag Definitions.  These flags are passed to NSFNoteUpdate and
NSFNoteDelete. See also NOTEID_xxx special definitions in nsfdata.h. */

	/** update even if ERR_CONFLICT */
	public static final short UPDATE_FORCE = 0x0001;
	/** give error if new field name defined */
	public static final short UPDATE_NAME_KEY_WARNING = 0x0002;
	/** do NOT do a database commit after update */
	public static final short UPDATE_NOCOMMIT = 0x0004;
	/** do NOT maintain revision history */
	public static final short UPDATE_NOREVISION = 0x0100;
	/** update body but leave no trace of note in file if deleted */
	public static final short UPDATE_NOSTUB = 0x0200;
	/** Compute incremental note info */
	public static final short UPDATE_INCREMENTAL = 0x4000;
	/* update body DELETED */
	public static final short UPDATE_DELETED = (short) (0x8000 & 0xffff);
	/* Obsolete; but in SDK */
	public static final short UPDATE_DUPLICATES	= 0;

/* Conflict Handler defines */
	public static final short CONFLICT_ACTION_MERGE = 1;
	public static final short CONFLICT_ACTION_HANDLED = 2;

	/** Split the second update of this note with the object store */
	public static final int UPDATE_SHARE_SECOND = 0x00200000;
	/**	Share objects only, not non-summary items, with the object store */
	public static final int UPDATE_SHARE_OBJECTS = 0x00400000;
	/** Return status of lock */
	public static final int NOTE_LOCK_STATUS 	= 0x00000008;
	/** Take out a hard note lock */
	public static final int NOTE_LOCK_HARD = 0x00000010;
	/** Take out a provisional hard note lock */
	public static final int NOTE_LOCK_PROVISIONAL = 0x00000020;
	
/*	Flags returned (beginning in V3) in the _NOTE_FLAGS */

	/** TRUE if document cannot be updated */
	public static final short NOTE_FLAG_READONLY = 0x0001;
	/** missing some data */
	public static final short NOTE_FLAG_ABSTRACTED = 0x0002;
	/** Incremental note (place holders) */
	public static final short NOTE_FLAG_INCREMENTAL	= 0x0004;
	/** Note contains linked items or linked objects */
	public static final short NOTE_FLAG_LINKED = 0x0020;
	/** Incremental type note Fully opened (NO place holders)
	This type of note is meant to retain the 
	Item sequence numbers */
	public static final short NOTE_FLAG_INCREMENTAL_FULL = 0x0040;
	/** Note is (opened) in canonical form */
	public static final short NOTE_FLAG_CANONICAL = 0x4000;

/* 	Note structure member IDs for NSFNoteGet and SetInfo. */

	/** IDs for NSFNoteGet and SetInfo */
	public static short _NOTE_DB = 0;		
	/** (When adding new values, see the table in NTINFO.C */
	public static short _NOTE_ID = 1;
	public static short _NOTE_OID = 2;
	public static short _NOTE_CLASS	= 3;
	public static short _NOTE_MODIFIED = 4;
	/** For pre-V3 compatibility. Should use $Readers item */
	public static short _NOTE_PRIVILEGES = 5;
	public static short _NOTE_FLAGS = 7;
	public static short _NOTE_ACCESSED = 8;
	/** For response hierarchy */
	public static short _NOTE_PARENT_NOTEID = 10;
	/** For response hierarchy */
	public static short _NOTE_RESPONSE_COUNT = 11;
	/** For response hierarchy */
	public static short _NOTE_RESPONSES = 12;
	/** For AddedToFile time */
	public static short _NOTE_ADDED_TO_FILE = 13;
	/** DBHANDLE of object store used by linked items */
	public static short _NOTE_OBJSTORE_DB = 14;

	
	public static final String DFLAGPAT_VIEWS_AND_FOLDERS = "-G40n^";

	/** At least one of the "definition"
	 * view items ($FORMULA, $COLLATION,
	 * or $FORMULACLASS) has been modified
	 * by another user since last ReadEntries.
	 * Upon receipt, you may wish to
	 * re-read the view note if up-to-date
	 * copies of these items are needed.
	 * Upon receipt, you may also wish to
	 * re-synchronize your index position
	 * and re-read the rebuilt index.<br>
	 * <br>
	 * Signal returned only ONCE per detection */
	public static final int SIGNAL_DEFN_ITEM_MODIFIED = 0x0001;

	/** At least one of the non-"definition"
	 * view items ($TITLE,etc) has been
	 * modified since last ReadEntries.
	 * Upon receipt, you may wish to
	 * re-read the view note if up-to-date
	 * copies of these items are needed.<br>
	 * <br>
	 * Signal returned only ONCE per detection */
	public static final int SIGNAL_VIEW_ITEM_MODIFIED = 0x0002;
	
	/** Collection index has been modified
	 * by another user since last ReadEntries.
	 * Upon receipt, you may wish to
	 * re-synchronize your index position
	 * and re-read the modified index.<br>
	 * <br>
	 * Signal returned only ONCE per detection */
	public static final int SIGNAL_INDEX_MODIFIED = 0x0004;
	
	/** Unread list has been modified
	 * by another window using the same
	 * hCollection context
	 * Upon receipt, you may wish to
	 * repaint the window if the window
	 * contains the state of unread flags
	 * (This signal is never generated
	 *  by NIF - only unread list users) */
	public static final int SIGNAL_UNREADLIST_MODIFIED = 0x0008;
	
	/** Collection is not up to date */
	public static final int SIGNAL_DATABASE_MODIFIED = 0x0010;
	
	/** End of collection has not been reached
	 * due to buffer being too full.
	 * The ReadEntries should be repeated
	 * to continue reading the desired entries. */
	public static final int SIGNAL_MORE_TO_DO = 0x0020;
	
	/** The view contains a time-relative formula
	 * (e.g., @Now).  Use this flag to tell if the
	 * collection will EVER be up-to-date since
	 * time-relative views, by definition, are NEVER
	 * up-to-date. */
	public static final int SIGNAL_VIEW_TIME_RELATIVE = 0x0040;
	
	/** Returned if signal flags are not supported
	 * This is used by NIFFindByKeyExtended when it
	 * is talking to a pre-V4 server that does not
	 * support signal flags for FindByKey */
	public static final int SIGNAL_NOT_SUPPORTED = 0x0080;
	
	/**	Mask that defines all "sharing conflicts", which are cases when
	the database or collection has changed out from under the user. */
	public static final int SIGNAL_ANY_CONFLICT	= (SIGNAL_DEFN_ITEM_MODIFIED | SIGNAL_VIEW_ITEM_MODIFIED | SIGNAL_INDEX_MODIFIED | SIGNAL_UNREADLIST_MODIFIED | SIGNAL_DATABASE_MODIFIED);
	
	/**	Mask that defines all "sharing conflicts" except for SIGNAL_DATABASE_MODIFIED.
	This can be used in combination with SIGNAL_VIEW_TIME_RELATIVE to tell if
	the database or collection has truly changed out from under the user or if the
	view is a time-relative view which will NEVER be up-to-date.  SIGNAL_DATABASE_MODIFIED
	is always returned for a time-relative view to indicate that it is never up-to-date. */
	public static final int SIGNAL_ANY_NONDATA_CONFLICT	= (SIGNAL_DEFN_ITEM_MODIFIED | SIGNAL_VIEW_ITEM_MODIFIED | SIGNAL_INDEX_MODIFIED | SIGNAL_UNREADLIST_MODIFIED);

	public static final short OS_TRANSLATE_NATIVE_TO_LMBCS = 0;	/* Translate platform-specific to LMBCS */
	public static final short OS_TRANSLATE_LMBCS_TO_NATIVE = 1;	/* Translate LMBCS to platform-specific */
	public static final short OS_TRANSLATE_LOWER_TO_UPPER = 3;	/* current int'l case table */
	public static final short OS_TRANSLATE_UPPER_TO_LOWER = 4;	/* current int'l case table */
	public static final short OS_TRANSLATE_UNACCENT = 5;  	/* int'l unaccenting table */

	public static final short OS_TRANSLATE_LMBCS_TO_UNICODE = 20;
	public static final short OS_TRANSLATE_LMBCS_TO_UTF8 = 22;
	public static final short OS_TRANSLATE_UNICODE_TO_LMBCS = 23;
	public static final short OS_TRANSLATE_UTF8_TO_LMBCS = 24;

	//	WORD LNPUBLIC		OSTranslate(WORD TranslateMode, const char far *In, WORD InLength, char far *Out, WORD OutLength);

	public static final int MAXPATH = 256;
	public static final short MAXUSERNAME	= 256;			/* Maximum user name */

	public short OSTranslate(short translateMode, Memory in, short inLength, Memory out, short outLength);
	public short OSTranslate(short translateMode, Pointer in, short inLength, Memory out, short outLength);
	
	public static short IDTABLE_MODIFIED = 0x0001;	/* modified - set by Insert/Delete */
	/* and can be cleared by caller if desired */
	public static short IDTABLE_INVERTED = 0x0002;	/* sense of list inverted */
	/* (reserved for use by caller only) */

	public static long NOTEID_RESERVED = 0x80000000L;		/*	Reserved Note ID, used for categories in NIFReadEntries and for deleted notes in a lot of interfaces. */

	public static long NOTEID_GHOST_ENTRY = 0x40000000L; /* Bit 30 -> partial thread ghost collection entry */
	public static long NOTEID_CATEGORY = 0x80000000L; /* Bit 31 -> (ghost) "category entry" */
	public static long NOTEID_CATEGORY_TOTAL = 0xC0000000L; /* Bit 31+30 -> (ghost) "grand total entry" */
	public static long NOTEID_CATEGORY_INDENT = 0x3F000000L;	/* Bits 24-29 -> category indent level within this column */
	public static long NOTEID_CATEGORY_ID = 0x00FFFFFFL;	/* Low 24 bits are unique category # */

	public static long RRV_DELETED = NOTEID_RESERVED;	/* indicates a deleted note (DBTABLE.C) */

	/** Cascade can go only one level deep parent\sub */
	public static int DESIGN_LEVELS = 2; 					
	/** Maximum size of a level */
	public static int DESIGN_LEVEL_MAX = 64;

	/** Guaranteed to be the greatest of Form, View or Macro
	 * length. NOTE:  We need
	 * space for LEVELS-1 cascade
	 * characters and a NULL term.
	 * The +1 takes care of that. */
	public static int DESIGN_NAME_MAX = ((DESIGN_LEVEL_MAX+1)*DESIGN_LEVELS);

	/** Forms can cascade a level */
	public static int DESIGN_FORM_MAX = DESIGN_NAME_MAX;
	/** Views can cascade a level */
	public static int DESIGN_VIEW_MAX = DESIGN_NAME_MAX;
	/** Macros can cascade a level */
	public static int DESIGN_MACRO_MAX = DESIGN_NAME_MAX;
	/** Fields cannot cascade */
	public static int DESIGN_FIELD_MAX = DESIGN_LEVEL_MAX+1;

	/** Design element comment max size. */
	public static int DESIGN_COMMENT_MAX = 256;
	/** All names, including sysnonyms */
	public static int DESIGN_ALL_NAMES_MAX = 256;
	/** Same as for views */
	public static int DESIGN_FOLDER_MAX	= DESIGN_VIEW_MAX;
	/** Same as for views */
	public static int DESIGN_FOLDER_MAX_NAME = DESIGN_LEVEL_MAX;

	public static int DESIGN_FLAGS_MAX = 32;

	public short b32_FTOpenSearch(IntByReference rethSearch);
	public short b64_FTOpenSearch(LongByReference rethSearch);
	
	public short b32_FTCloseSearch(int hSearch);
	public short b64_FTCloseSearch(long hSearch);
	
	public short b32_FTSearch(
			int hDB,
			IntByReference phSearch,
			int hColl,
			Memory query,
			int options,
			short  limit,
			int hIDTable,
			IntByReference retNumDocs,
			Memory reserved,
			IntByReference rethResults);
	public short b64_FTSearch(
			long hDB,
			LongByReference phSearch,
			long hColl,
			Memory query,
			int options,
			short  limit,
			long hIDTable,
			IntByReference retNumDocs,
			Memory reserved,
			LongByReference rethResults);
	
	public short b32_IDCreateTable (int alignment, IntByReference rethTable);
	public short b64_IDCreateTable (int alignment, LongByReference rethTable);
	
	public short b32_IDDestroyTable(int hTable);
	public short b64_IDDestroyTable(long hTable);
	
	public short b32_IDInsert (int hTable, int id, IntByReference retfInserted);
	public short b64_IDInsert (long hTable, int id, IntByReference retfInserted);
	
	public short b32_IDDelete (int hTable, int id, IntByReference retfDeleted);
	public short b64_IDDelete (long hTable, int id, IntByReference retfDeleted);
	
	public boolean b32_IDScan (int hTable, boolean fFirst, IntByReference retID);
	public boolean b64_IDScan (long hTable, boolean fFirst, IntByReference retID);
	
	public int b32_IDEntries (int hTable);
	public int b64_IDEntries (long hTable);
	
	public boolean b32_IDIsPresent (int hTable, int id);
	public boolean b64_IDIsPresent (long hTable, int id);
	
	public int b32_IDTableSize (int hTable);
	public int b64_IDTableSize (long hTable);
	
	public int b32_IDTableSizeP(Pointer pIDTable);
	public int b64_IDTableSizeP(Pointer pIDTable);
	
	public short b32_IDTableCopy (int hTable, IntByReference rethTable);
	public short b64_IDTableCopy (long hTable, LongByReference rethTable);
	
	public short b32_IDTableIntersect(int hSrc1Table, int hSrc2Table, IntByReference rethDstTable);
	public short b64_IDTableIntersect(long hSrc1Table, long hSrc2Table, LongByReference rethDstTable);
	
	public short b32_IDDeleteAll (int hTable);
	public short b64_IDDeleteAll (long hTable);
	
	public boolean b32_IDAreTablesEqual	(int hSrc1Table, int hSrc2Table);
	public boolean b64_IDAreTablesEqual	(long hSrc1Table, long hSrc2Table);
	
	public short b32_IDDeleteTable  (int hTable, int hIDsToDelete);
	public short b64_IDDeleteTable  (long hTable, long hIDsToDelete);
	
	public short b32_IDInsertTable  (int hTable, int hIDsToAdd);
	public short b64_IDInsertTable  (long hTable, long hIDsToAdd);
	
	public short IDTableFlags (ByteBuffer pIDTable);
	public void IDTableSetFlags (ByteBuffer pIDTable, short Flags);
	public void IDTableSetTime(ByteBuffer pIDTable, NotesTimeDate Time);
	public NotesTimeDate IDTableTime(ByteBuffer pIDTable);

	public short ODSLength(short type);
	
	public short ConvertTIMEDATEToText(
			ByteBuffer intlFormat,
			ByteBuffer textFormat,
			NotesTimeDate inputTime,
			Memory retTextBuffer,
			short textBufferLength,
			ShortByReference retTextLength);
	
	public short ConvertTextToTIMEDATE(
			ByteBuffer intlFormat,
			ByteBuffer textFormat,
			Memory text,
			short maxLength,
			NotesTimeDate retTIMEDATE);

	public boolean TimeGMToLocalZone (NotesTime timePtr);
	public boolean TimeGMToLocal (NotesTime timePtr);
	public boolean TimeLocalToGM(NotesTime timePtr);
	public boolean TimeLocalToGM(Memory timePtr);
	
	public short TIMEDATE_MINIMUM = 0;
	public short TIMEDATE_MAXIMUM = 1;
	public short TIMEDATE_WILDCARD = 2;
	public void TimeConstant(short timeConstantType, NotesTimeDate tdptr);

	public short ListGetText (ByteBuffer pList,
			boolean fPrefixDataType,
			short entryNumber,
			Memory retTextPointer,
			ShortByReference retTextLength);

	public short ListGetText (Pointer pList,
			boolean fPrefixDataType,
			short entryNumber,
			Memory retTextPointer,
			ShortByReference retTextLength);
	
	public int TimeExtractTicks(Memory time);

	public int TimeExtractJulianDate(Memory time);
	public int TimeExtractDate(Memory time);
	
	/*	Define flags for NSFFolderGetIDTable */
	public int DB_GETIDTABLE_VALIDATE = 0x00000001;	/*	If set, return only "validated" noteIDs */

	public short b32_NSFFolderGetIDTable(
			int  hViewDB,
			int hDataDB,
			int  viewNoteID,
			int  flags,
			IntByReference hTable);

	public short b64_NSFFolderGetIDTable(
			long  hViewDB,
			long hDataDB,
			int  viewNoteID,
			int  flags,
			LongByReference hTable);

	public short b32_FolderDocAdd(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			int  hTable,
			long  dwFlags);

	public short b64_FolderDocAdd(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  hTable,
			long  dwFlags);

	public short b32_FolderDocCount(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			long  dwFlags,
			LongByReference pdwNumDocs);

	public short b64_FolderDocCount(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  dwFlags,
			LongByReference pdwNumDocs);

	public short b32_FolderDocRemove(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			int  hTable,
			long  dwFlags);

	public short b64_FolderDocRemove(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  hTable,
			long  dwFlags);

	public short b32_FolderDocRemoveAll(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			long  dwFlags);

	public short b64_FolderDocRemoveAll(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  dwFlags);

	public short b32_FolderMove(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			int  hParentDB,
			int  ParentNoteID,
			long  dwFlags);
	
	public short b64_FolderMove(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  hParentDB,
			int  ParentNoteID,
			long  dwFlags);

	public short b32_FolderRename(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			Memory pszName,
			short  wNameLen,
			long  dwFlags);

	public short b64_FolderRename(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			Memory pszName,
			short  wNameLen,
			long  dwFlags);

	public short b32_NSFDbClearReplHistory(int hDb, int dwFlags);
	public short b64_NSFDbClearReplHistory(long hDb, int dwFlags);

	public void b32_NSFDbPathGet(
			int hDB,
			Memory retCanonicalPathName,
			Memory retExpandedPathName);
	
	public void b64_NSFDbPathGet(
			long hDB,
			Memory retCanonicalPathName,
			Memory retExpandedPathName);
	
	public void b32_NSFNoteGetInfo(int hNote, short type, Memory retValue);
	public void b64_NSFNoteGetInfo(long hNote, short type, Memory retValue);
	public void b32_NSFNoteSetInfo(int hNote, short type, Memory value);
	public void b64_NSFNoteSetInfo(long hNote, short type, Memory value);

	public short b32_NSFNoteContract(int hNote);
	public short b64_NSFNoteContract(long hNote);
	public short b32_NSFNoteExpand(int hNote);
	public short b64_NSFNoteExpand(long hNote);
	public short b32_NSFNoteClose(int hNote);
	public short b64_NSFNoteClose(long hNote);
	public short b32_NSFNoteSign(int hNote);
	public short b64_NSFNoteSign(long hNote);

	public short b32_NSFNoteUpdate(int hNote, short updateFlags);
	public short b64_NSFNoteUpdate(long hNote, short updateFlags);

	public short b32_NSFNoteOpen(int hDB, int noteId, short openFlags, IntByReference rethNote);
	public short b64_NSFNoteOpen(long hDB, int noteId, short openFlags, LongByReference rethNote);
	public short b32_NSFNoteOpenExt(int hDB, int noteId, int flags, IntByReference rethNote);
	public short b64_NSFNoteOpenExt(long hDB, int noteId, int flags, LongByReference rethNote);


	public short b32_NSFDbGetMultNoteInfo(
			int  hDb,
			short  Count,
			short  Options,
			int  hInBuf,
			LongByReference retSize,
			IntByReference rethOutBuf);
	
	public short b64_NSFDbGetMultNoteInfo(
			long  hDb,
			short  Count,
			short  Options,
			long  hInBuf,
			LongByReference retSize,
			LongByReference rethOutBuf);

	/*	Definitions for NSFDbGetMultNoteInfo and NSFDbGetMultNoteInfoByUNID */

	/** Return NoteID */
	public static short fINFO_NOTEID = 0x0001;
	/** Return SequenceTime from OID */
	public static short fINFO_SEQTIME = 0x0002;
	/** Return Sequence number from OID */
	public static short fINFO_SEQNUM = 0x0004;
	/** Return OID (disables SeqTime &amp; number &amp; UNID) */
	public static short fINFO_OID  = 0x0008;
	/** Compress non-existent UNIDs */
	public static short fINFO_COMPRESS = 0x0040;
	/** Return UNID  */
	public static short fINFO_UNID = 0x0080;
	/** Allow the returned buffer to exceed 64k. */	
	public static short fINFO_ALLOW_HUGE = 0x0400;

	public short NSFDbCreateExtended(
			Memory pathName,
			short  DbClass,
			boolean  ForceCreation,
			short  Options,
			byte  EncryptStrength,
			long  MaxFileSize);

	/*	Define NSF DB Classes - These all begin with 0xf000 for no good
	reason other than to ENSURE that callers of NSFDbCreate call the
	routine with valid parameters, since in earlier versions of NSF
	the argument to the call was typically 0. */

	/* The type of the database is determined by the filename extension.
	 * The extensions and their database classes are .NSX (NSFTESTFILE),
	 * .NSF (NOTEFILE), .DSK (DESKTOP), .NCF (NOTECLIPBOARD), .NTF (TEMPLATEFILE),
	 * .NSG (GIANTNOTEFILE), .NSH (HUGENOTEFILE), NTD (ONEDOCFILE),
	 * NS2 (V2NOTEFILE), NTM (ENCAPSMAILFILE). */
public int DBCLASS_BY_EXTENSION = 0;	

/** A test database. */
public short DBCLASS_NSFTESTFILE = (short) (0xff00 & 0xffff);
/** A standard Domino database. */
public short DBCLASS_NOTEFILE = (short) (0xff01 & 0xffff);
/** A Notes desktop (folders, icons, etc.). */
public short DBCLASS_DESKTOP = (short) (0xff02 & 0xffff);
/** A Notes clipboard (used for cutting and pasting). */
public short DBCLASS_NOTECLIPBOARD = (short) (0xff03 & 0xffff);
/** A database that contains every type of note (forms, views, ACL, icon, etc.) except data notes. */
public short DBCLASS_TEMPLATEFILE = (short) (0xff04 & 0xffff);
/** A standard Domino database, with size up to 1 GB. This was used
 * in Notes Release 3 when the size of a previous version of a database had been limited to 200 MB.
 */
public short DBCLASS_GIANTNOTEFILE = (short) (0xff05 & 0xffff);
/**  A standard Domino database, with size up to 1 GB. This was used in Notes Release
 * 3 when the size of a previous version of a database had been limited to 300 MB.
 */
public short DBCLASS_HUGENOTEFILE	= (short) (0xff06 & 0xffff);
/** One document database with size up to 10MB. Specifically used by alternate
 * mail to create an encapsulated database. Components of the document are
 * further limited in size. It is not recommended that you use this database
 * class with NSFDbCreate. If you do, and you get an error when saving the document,
 * you will need to re-create the database using DBCLASS_NOTEFILE. */
public short DBCLASS_ONEDOCFILE = (short) (0xff07 & 0xffff);
/** Database was created as a Notes Release 2 database. */
public short DBCLASS_V2NOTEFILE = (short) (0xff08 & 0xffff);
/** One document database with size up to 5MB. Specifically used by alternate mail
 * to create an encapsulated database. Components of the document are further
 * limited in size. It is not recommended that you use this database class with
 * NSFDbCreate. If you do, and you get an error when saving the document, you will
 * need to re-create the database using DBCLASS_NOTEFILE. */
public short DBCLASS_ENCAPSMAILFILE = (short) (0xff09 & 0xffff);
/** Specifically used by alternate mail. Not recomended for use with NSFDbCreate. */
public short DBCLASS_LRGENCAPSMAILFILE = (short) (0xff0a & 0xffff);
/** Database was created as a Notes Release 3 database. */
public short DBCLASS_V3NOTEFILE = (short) (0xff0b & 0xffff);
/** Object store. */
public short DBCLASS_OBJSTORE = (short) (0xff0c & 0xffff);
/**  One document database with size up to 10MB. Specifically used by Notes Release 3
 * alternate mail to create an encapsulated database. Not recomended for use
 * with NSFDbCreate. */
public short DBCLASS_V3ONEDOCFILE	= (short) (0xff0d & 0xffff);
/** Database was created specifically for Domino and Notes Release 4. */
public short DBCLASS_V4NOTEFILE = (short) (0xff0e & 0xffff);
/** Database was created specifically for Domino and Notes Release 5. */
public short DBCLASS_V5NOTEFILE = (short) (0xff0f & 0xffff);
/** Database was created specifically for Domino and Notes Release Notes/Domino 6. */
public short DBCLASS_V6NOTEFILE = (short) (0xff10 & 0xffff);
/** Database was created specifically for Domino and Notes Release Notes/Domino 8. */
public short DBCLASS_V8NOTEFILE = (short) (0xff11 & 0xffff);
/** Database was created specifically for Domino and Notes Release Notes/Domino 8.5. */
public short DBCLASS_V85NOTEFILE = (short) (0xff12 & 0xffff);
/** Database was created specifically for Domino and Notes Release Notes/Domino 9. */
public short DBCLASS_V9NOTEFILE = (short) (0xff13 & 0xffff);


public short DBCLASS_MASK	= (0x00ff & 0xffff);
public short DBCLASS_VALID_MASK = (short) (0xff00 & 0xffff);

/* 	Option flags for NSFDbCreateExtended */

/** Create a locally encrypted database. */
public short DBCREATE_LOCALSECURITY	= 0x0001;
/** NSFNoteUpdate will not use an object store for notes in the database. */
public short DBCREATE_OBJSTORE_NEVER	= 0x0002;
/** The maximum database length is specified in bytes in NSFDbCreateExtended. */
public short DBCREATE_MAX_SPECIFIED	= 0x0004;
/** Don't support note hierarchy - ODS21 and up only */
public short DBCREATE_NORESPONSE_INFO = 0x0010;
/** Don't maintain unread lists for this DB */
public short DBCREATE_NOUNREAD = 0x0020;
/** Skip overwriting freed disk buffer space */
public short DBCREATE_NO_FREE_OVERWRITE	= 0x0200;
/** Maintain form/bucket bitmap */
public short DBCREATE_FORM_BUCKET_OPT = 0x0400;
/** Disable transaction logging for this database if specified */
public short DBCREATE_DISABLE_TXN_LOGGING = 0x0800;
/** Enable maintaining last accessed time */
public short DBCREATE_MAINTAIN_LAST_ACCESSED = 0x1000;
/** TRUE if database is a mail[n].box database */
public short DBCREATE_IS_MAILBOX = 0x4000;
/** TRUE if database should allow "large" (&lt;64K bytes) UNK table */
public short DBCREATE_LARGE_UNKTABLE = (short) (0x8000 & 0xffff);

/* Values for EncryptStrength of NSFDbCreateExtended */

public byte DBCREATE_ENCRYPT_NONE = 0x00;	
public byte DBCREATE_ENCRYPT_SIMPLE	= 0x01;	
public byte DBCREATE_ENCRYPT_MEDIUM	= 0x02;
public byte DBCREATE_ENCRYPT_STRONG	= 0x03;

	/*	Data Type Definitions. */


	/*	Class definitions.  Classes are defined to be the
		"generic" classes of data type that the internal formula computation
		mechanism recognizes when doing recalcs. */

	public static final int CLASS_NOCOMPUTE = (int)(0 << 8);
	public static final int CLASS_ERROR = (int)(1 << 8);
	public static final int CLASS_UNAVAILABLE = (int)(2 << 8);
	public static final int CLASS_NUMBER = (int)(3 << 8);
	public static final int CLASS_TIME = (int)(4 << 8);
	public static final int CLASS_TEXT = (int)(5 << 8);
	public static final int CLASS_FORMULA = (int)(6 << 8);
	public static final int CLASS_USERID = (int)(7 << 8);

	public static final int CLASS_MASK = (int)0xff00;

	/*  All datatypes below are passed to NSF in either host (machine-specific
		byte ordering and padding) or canonical form (Intel 86 packed form).
		The format of each datatype, as it is passed to and from NSF functions,
		is listed below in the comment field next to each of the data types.
		(This host/canonical issue is NOT applicable to Intel86 machines,
		because on that machine, they are the same and no conversion is required).
		On all other machines, use the ODS subroutine package to perform
		conversions of those datatypes in canonical format before they can
		be interpreted. */

	/*	"Computable" Data Types */

	public static final int TYPE_ERROR = (int)(0 + (1 << 8));
	public static final int TYPE_UNAVAILABLE = (int)(0 + (2 << 8));
	public static final int TYPE_TEXT = (int)(0 + (5 << 8));
	public static final int TYPE_TEXT_LIST = (int)(1 + (5 << 8));
	public static final int TYPE_NUMBER = (int)(0 + (3 << 8));
	public static final int TYPE_NUMBER_RANGE = (int)(1 + (3 << 8));
	public static final int TYPE_TIME = (int)(0 + (4 << 8));
	public static final int TYPE_TIME_RANGE = (int)(1 + (4 << 8));
	public static final int TYPE_FORMULA = (int)(0 + (6 << 8));
	public static final int TYPE_USERID = (int)(0 + (7 << 8));

	/*	"Non-Computable" Data Types */

	public static final int TYPE_SIGNATURE = (int)(8 + (0 << 8));
	public static final int TYPE_ACTION = (int)(16 + (0 << 8));
	public static final int TYPE_WORKSHEET_DATA = (int)(13 + (0 << 8));
	public static final int TYPE_VIEWMAP_LAYOUT = (int)(19 + (0 << 8));
	public static final int TYPE_SEAL2 = (int)(31 + (0 << 8));
	public static final int TYPE_LSOBJECT = (int)(20 + (0 << 8));
	public static final int TYPE_ICON = (int)(6 + (0 << 8));
	public static final int TYPE_VIEW_FORMAT = (int)(5 + (0 << 8));
	public static final int TYPE_SCHED_LIST = (int)(22 + (0 << 8));
	public static final int TYPE_VIEWMAP_DATASET = (int)(18 + (0 << 8));
	public static final int TYPE_SEAL = (int)(9 + (0 << 8));
	public static final int TYPE_MIME_PART = (int)(25 + (0 << 8));
	public static final int TYPE_SEALDATA = (int)(10 + (0 << 8));
	public static final int TYPE_NOTELINK_LIST = (int)(7 + (0 << 8));
	public static final int TYPE_COLLATION = (int)(2 + (0 << 8));
	public static final int TYPE_RFC822_TEXT = (int)(2 + (5 << 8));
	public static final int TYPE_COMPOSITE = (int)(1 + (0 << 8));
	public static final int TYPE_OBJECT = (int)(3 + (0 << 8));
	public static final int TYPE_HTML = (int)(21 + (0 << 8));
	public static final int TYPE_ASSISTANT_INFO = (int)(17 + (0 << 8));
	public static final int TYPE_HIGHLIGHTS = (int)(12 + (0 << 8));
	public static final int TYPE_NOTEREF_LIST = (int)(4 + (0 << 8));
	public static final int TYPE_QUERY = (int)(15 + (0 << 8));
	public static final int TYPE_USERDATA = (int)(14 + (0 << 8));
	public static final int TYPE_INVALID_OR_UNKNOWN = (int)(0 + (0 << 8));
	public static final int TYPE_SEAL_LIST = (int)(11 + (0 << 8));
	public static final int TYPE_CALENDAR_FORMAT = (int)(24 + (0 << 8));

	public static final int ALLDAY = 0xffffffff;
	public static final int ANYDAY = 0xffffffff;

	public static final int DT_SHOWDATE = (int)0x0008;
	public static final int DT_SHOWABBREV = (int)0x0800;
	public static final int DT_STYLE_MDY = (int)2;
	public static final int DT_USE_TFMT = (int)0x0001;
	public static final int SECS_IN_WEEK = (int)604800;
	public static final int DT_STYLE_MSK = (int)0x000f0000;
	public static final int DT_STYLE_YMD = (int)1;
	public static final int DT_STYLE_DMY = (int)3;
	public static final int DT_SHOWTIME = (int)0x0004;
	public static final int TICKS_IN_MINUTE = (int)6000;
	public static final int DT_4DIGITYEAR = (int)0x0001;
	public static final int DT_24HOUR = (int)0x0040;
	public static final int SECS_IN_MONTH = (int)2592000;
	public static final int DT_ALPHAMONTH = (int)0x0002;
	public static final int TICKS_IN_DAY = (int)8640000;
	public static final int SECS_IN_DAY = (int)86400;
	public static final int TICKS_IN_SECOND = (int)100;
	public static final int TICKS_IN_HOUR = (int)360000;
	public static final int DT_VALID = (int)0x8000;

	public short b32_FTIndex(int hDB, short options, Memory stopFile, NotesFTIndexStats retStats);
	public short b64_FTIndex(long hDB, short options, Memory stopFile, NotesFTIndexStats retStats);

	public short b32_FTDeleteIndex(int hDB);
	public short b64_FTDeleteIndex(long hDB);
	
	public short b32_FTGetLastIndexTime(int hDB, NotesTimeDate retTime);
	public short b64_FTGetLastIndexTime(long hDB, NotesTimeDate retTime);
	
	public short b32_NSFDbGetBuildVersion(int hDB, ShortByReference retVersion);
	public short b64_NSFDbGetBuildVersion(long hDB, ShortByReference retVersion);
	
	public short b32_NSFDbGetMajMinVersion(int hDb, NotesBuildVersion retBuildVersion);
	public short b64_NSFDbGetMajMinVersion(long hDb, NotesBuildVersion retBuildVersion);
	
	public short NotesInitExtended(int  argc, StringArray argvPtr);
	public void NotesTerm();

	public short b32_NSFSearch(
			int hDB,
			int hFormula,
			Memory viewTitle,
			short SearchFlags,
			short NoteClassMask,
			NotesTimeDate Since,
			b32_NsfSearchProc enumRoutine,
			Pointer EnumRoutineParameter,
			NotesTimeDate retUntil);

	public short b64_NSFSearch(
			long hDB,
			long hFormula,
			Memory viewTitle,
			short SearchFlags,
			short NoteClassMask,
			NotesTimeDate Since,
			b64_NsfSearchProc enumRoutine,
			Pointer EnumRoutineParameter,
			NotesTimeDate retUntil);

	public interface b32_NsfSearchProc extends Callback { /* StdCallCallback if using __stdcall__ */
        void invoke(Pointer enumRoutineParameter, NotesSearchMatch32 searchMatch, NotesItemTable summaryBuffer); 
    }
	
	public interface b64_NsfSearchProc extends Callback { /* StdCallCallback if using __stdcall__ */
        void invoke(Pointer enumRoutineParameter, NotesSearchMatch64 searchMatch, NotesItemTable summaryBuffer); 
    }
	
	public short b64_NSFFormulaCompile(
			Memory FormulaName,
			short FormulaNameLength,
			Memory FormulaText,
			short  FormulaTextLength,
			LongByReference rethFormula,
			ShortByReference retFormulaLength,
			ShortByReference retCompileError,
			ShortByReference retCompileErrorLine,
			ShortByReference retCompileErrorColumn,
			ShortByReference retCompileErrorOffset,
			ShortByReference retCompileErrorLength);

	public short b32_NSFFormulaCompile(
			Memory FormulaName,
			short FormulaNameLength,
			Memory FormulaText,
			short  FormulaTextLength,
			IntByReference rethFormula,
			ShortByReference retFormulaLength,
			ShortByReference retCompileError,
			ShortByReference retCompileErrorLine,
			ShortByReference retCompileErrorColumn,
			ShortByReference retCompileErrorOffset,
			ShortByReference retCompileErrorLength);

	/** does not match formula (deleted or updated) */
	public static byte SE_FNOMATCH = 0x00;
	/** matches formula */
	public static byte SE_FMATCH = 0x01;
	/** document truncated */
	public static byte SE_FTRUNCATED = 0x02;
	/** note has been purged. Returned only when SEARCH_INCLUDE_PURGED is used */
	public static byte SE_FPURGED = 0x04;
	/** note has no purge status. Returned only when SEARCH_FULL_DATACUTOFF is used */
	public static byte SE_FNOPURGE = 0x08;
	/** if SEARCH_NOTIFYDELETIONS: note is soft deleted; NoteClass &amp; NOTE_CLASS_NOTIFYDELETION also on (off for hard delete) */
	public static byte SE_FSOFTDELETED = 0x10;
	/** if there is reader's field at doc level this is the return value so that we could mark the replication as incomplete*/
	public static byte SE_FNOACCESS = 0x20;
	/** note has truncated attachments. Returned only when SEARCH1_ONLY_ABSTRACTS is used */
	public static byte SE_FTRUNCATT	= 0x40;

	/*	File type flags (used with NSFSearch directory searching). */

	/** Any file type */
	public static int FILE_ANY = 0;
	/** Starting in V3, any DB that is a candidate for replication */
	public static int FILE_DBREPL = 1;
	/** Databases that can be templates */
	public static int FILE_DBDESIGN = 2;
	/** BOX - Any .BOX (Mail.BOX, SMTP.Box...) */
	public static int FILE_MAILBOX = 3;
							 
	/** NS?, any NSF version */
	public static int FILE_DBANY = 4;
	/** NT?, any NTF version */
	public static int FILE_FTANY = 5;
	/** MDM - modem command file */
	public static int FILE_MDMTYPE = 6;
	/** directories only */
	public static int FILE_DIRSONLY = 7;
	/** VPC - virtual port command file */
	public static int FILE_VPCTYPE = 8;
	/** SCR - comm port script files */
	public static int FILE_SCRTYPE	= 9;
	/** ANY Notes database (.NS?, .NT?, .BOX)	*/
	public static int FILE_ANYNOTEFILE = 10;
	/** DTF - Any .DTF. Used for container and sort temp files to give them a more
	   unique name than .TMP so we can delete *.DTF from the temp directory and
	   hopefully not blow away other application's temp files. */
	public static int FILE_UNIQUETEMP = 11;
	/** CLN - Any .cln file...multi user cleanup files*/
	public static int FILE_MULTICLN = 12;
	/** any smarticon file *.smi */
	public static int FILE_SMARTI = 13;

	/** File type mask (for FILE_xxx codes above) */
	public static int FILE_TYPEMASK = 0x00ff;
	/** List subdirectories as well as normal files */
	public static int FILE_DIRS = 0x8000;
	/** Do NOT return ..'s */
	public static int FILE_NOUPDIRS = 0x4000;
	/** Recurse into subdirectories */
	public static int FILE_RECURSE = 0x2000;
	/** All directories, linked files &amp; directories */
	public static int FILE_LINKSONLY = 0x1000;

	public short OSPathNetConstruct(Memory PortName,
											Memory ServerName,
											Memory FileName,
											Memory retPathName);
	
	public short OSPathNetParse(Memory PathName,
											Memory retPortName,
											Memory retServerName,
											Memory retFileName);

	public short b32_OSMemAlloc(
			short  BlkType,
			long  dwSize,
			IntByReference retHandle);
	
	public short b64_OSMemAlloc(
			short  BlkType,
			long  dwSize,
			LongByReference retHandle);

}