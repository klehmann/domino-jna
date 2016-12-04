package com.mindoo.domino.jna.internal;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import com.mindoo.domino.jna.structs.NotesBlockId;
import com.mindoo.domino.jna.structs.NotesBuildVersion;
import com.mindoo.domino.jna.structs.NotesCDField;
import com.mindoo.domino.jna.structs.NotesCollectionPosition;
import com.mindoo.domino.jna.structs.NotesDbReplicaInfo;
import com.mindoo.domino.jna.structs.NotesFTIndexStats;
import com.mindoo.domino.jna.structs.NotesFileObject;
import com.mindoo.domino.jna.structs.NotesItemTable;
import com.mindoo.domino.jna.structs.NotesItemValueTable;
import com.mindoo.domino.jna.structs.NotesNamesListHeader32;
import com.mindoo.domino.jna.structs.NotesNamesListHeader64;
import com.mindoo.domino.jna.structs.NotesNumberPair;
import com.mindoo.domino.jna.structs.NotesObjectDescriptor;
import com.mindoo.domino.jna.structs.NotesOriginatorId;
import com.mindoo.domino.jna.structs.NotesRange;
import com.mindoo.domino.jna.structs.NotesSearchMatch32;
import com.mindoo.domino.jna.structs.NotesSearchMatch64;
import com.mindoo.domino.jna.structs.NotesTableItem;
import com.mindoo.domino.jna.structs.NotesTime;
import com.mindoo.domino.jna.structs.NotesTimeDate;
import com.mindoo.domino.jna.structs.NotesTimeDatePair;
import com.mindoo.domino.jna.structs.NotesUniversalNoteId;
import com.mindoo.domino.jna.structs.WinNotesNamesListHeader32;
import com.mindoo.domino.jna.structs.WinNotesNamesListHeader64;
import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
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
	public final int tableItemSize = new NotesTableItem().size();
	public final int oidSize = new NotesOriginatorId().size();
	public final int winNamesListHeaderSize64 = new WinNotesNamesListHeader64().size();
	public final int winNamesListHeaderSize32 = new WinNotesNamesListHeader32().size();
	public final int namesListHeaderSize32 = new NotesNamesListHeader32().size();
	public final int namesListHeaderSize64 = new NotesNamesListHeader64().size();
	public final int objectDescriptorSize = new NotesObjectDescriptor().size();
	public final int fileObjectSize = new NotesFileObject().size();
	public final int cdFieldSize = new NotesCDField().size();
	
	public static final short MAXALPHATIMEDATE = 80;

	public static final short ERR_MASK = 0x3fff;

	/*	Defines for Authentication flags */

	/** Set if names list has been authenticated via Notes */
	public static final short NAMES_LIST_AUTHENTICATED = 0x0001;	
	/**	Set if names list has been authenticated using external password -- Triggers "maximum password access allowed" feature */
	public static final short NAMES_LIST_PASSWORD_AUTHENTICATED = 0x0002;
	/**	Set if user requested full admin access and it was granted */
	public static final short NAMES_LIST_FULL_ADMIN_ACCESS = 0x0004;

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

	short b32_NSFDbSpaceUsageScaled (int dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes, IntByReference retGranularity);
	short b64_NSFDbSpaceUsageScaled (long dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes, IntByReference retGranularity);
	
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

	public short b64_NIFReadEntriesExt(long hCollection,
			NotesCollectionPosition CollectionPos,
            short SkipNavigator, int SkipCount,
            short ReturnNavigator, int ReturnCount, int ReturnMask,
            NotesTimeDate DiffTime, long DiffIDTable, int ColumnNumber, int Flags,
            LongByReference rethBuffer, ShortByReference retBufferLength,
            IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned,
            ShortByReference retSignalFlags, NotesTimeDate retDiffTime,
            NotesTimeDate retModifiedTime, IntByReference retSequence);

	public short b32_NIFReadEntriesExt(int hCollection,
			NotesCollectionPosition CollectionPos,
			short SkipNavigator, int SkipCount,
			short ReturnNavigator, int ReturnCount, int ReturnMask,
			NotesTimeDate DiffTime, int DiffIDTable, int ColumnNumber, int Flags,
			IntByReference rethBuffer, ShortByReference retBufferLength,
			IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned,
			ShortByReference retSignalFlags, NotesTimeDate retDiffTime,
			NotesTimeDate retModifiedTime, IntByReference retSequence);

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

	short b64_NIFIsNoteInView(long hCollection, int noteID, IntByReference retIsInView);
	short b32_NIFIsNoteInView(int hCollection, int noteID, IntByReference retIsInView);

	boolean b64_NIFIsUpdateInProgress(long hCollection);
	boolean b32_NIFIsUpdateInProgress(int hCollection);
	
	public short b64_NIFGetIDTableExtended(long hCollection, short navigator, short Flags, long hIDTable);
	public short b32_NIFGetIDTableExtended(int hCollection, short navigator, short Flags, int hIDTable);

	public boolean b64_NIFCollectionUpToDate(long hCollection);
	public boolean b32_NIFCollectionUpToDate(int hCollection);
	
	/**
	 * NIFSetCollectionInfo - Special kludge for server to set collection info
	 *
	 * @param hCollection Per-user collection context handle to be validated
	 * @param SessionID pointer to SessionID of session, or 0 if you don't know or don't care
	 * @param hUnreadList Address to store unread list handle (optional)
	 * @param hCollapsedList Address to store collapsed list handle (optional)
	 * @param hSelectedList = Address to store selected list handle (optional)
	 */
	public boolean b64_NIFSetCollectionInfo (long hCollection, Pointer SessionID,
            long hUnreadList, long hCollapsedList, long hSelectedList);
            
    public boolean b32_NIFSetCollectionInfo (int hCollection, Pointer SessionID,
                    int hUnreadList, int hCollapsedList, int hSelectedList);

    public short b64_NIFUpdateFilters (long hCollection, short ModifyFlags);
    public short b32_NIFUpdateFilters (int hCollection, short ModifyFlags);
    
    public boolean b64_NIFIsTimeVariantView(long hCollection);
    public boolean b32_NIFIsTimeVariantView(int hCollection);
    
    /** UnreadList has been modified */
    public static short FILTER_UNREAD = 0x0001;
    /** CollpasedList has been modified */
    public static short FILTER_COLLAPSED = 0x0002;
    /** SelectedList has been modified */
    public static short FILTER_SELECTED = 0x0004;
    /** UNID table has been modified. */
    public static short FILTER_UNID_TABLE = 0x0008;
    /** Conditionaly do FILTER_UNREAD if current unread list indicates it - see NSFDbUpdateUnread */
    public static short FILTER_UPDATE_UNREAD = 0x0010;
    /** Mark specified ID table Read */
    public static short FILTER_MARK_READ = 0x0020;
    /** Mark specified ID table Unread */
    public static short FILTER_MARK_UNREAD = 0x0040;
    /** Mark all read */
    public static short FILTER_MARK_READ_ALL = 0x0080;
    /** Mark all unread */
    public static short FILTER_MARK_UNREAD_ALL = 0x0100;
    
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

	public short b32_OSMemGetSize(int handle, IntByReference retSize);
	public short b64_OSMemGetSize(long handle, IntByReference retSize);

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

	/** display only views and folder; version filtering */
	public static final String DFLAGPAT_VIEWS_AND_FOLDERS = "-G40n^";
	/** display things that are runnable; version filtering */
	public static final String DFLAGPAT_TOOLSRUNMACRO = "-QXMBESIst5nmz{";

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

	public boolean b32_IDScanBack (int hTable, boolean fLast, IntByReference retID);
	public boolean b64_IDScanBack (long hTable, boolean fLast, IntByReference retID);

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

	public short b64_IDEnumerate(long hTable, IdEnumerateProc Routine, Pointer Parameter);
	public short b32_IDEnumerate(int hTable, IdEnumerateProc Routine, Pointer Parameter);
	
	public interface IdEnumerateProc extends Callback { /* StdCallCallback if using __stdcall__ */
		short invoke(Pointer parameter, int noteId); 
	};

	/*        Insert a DWORD Range of IDs into an ID table
	*
	*        Inputs:
	*                hTable = Handle of ID Table
	*                IDFrom, IDTo = Range of IDs to insert (inclusive)
	*                Alignment = most typical ID repeat factor (for example, 4 if IDs are
	*                                        most typically spaced 4 apart in value)
	*                AtEnd        = if TRUE, caller GUARANTEES that the ID range does not
	*                                  overlap any IDs in the table and are the largest IDs
	*                                  in the table!
	*/
	public short b64_IDInsertRange(long hTable, int IDFrom, int IDTo, boolean AddToEnd);
	public short b32_IDInsertRange(int hTable, int IDFrom, int IDTo, boolean AddToEnd);
	
	public short b64_IDTableDifferences(long idtable1, long idtable2, LongByReference outputidtableAdds, LongByReference outputidtableDeletes, LongByReference outputidtableSame);
	public short b32_IDTableDifferences(int idtable1, int idtable2, IntByReference outputidtableAdds, IntByReference outputidtableDeletes, IntByReference outputidtableSame);
	
	public short b64_IDTableReplaceExtended(long idtableSrc, long idtableDest, byte flags);
	public short b32_IDTableReplaceExtended(int idtableSrc, int idtableDest, byte flags);
	
	//saves the info in the idtable header in the dest
	public static final byte IDREPLACE_SAVEDEST = 0x01;
	
	public short ODSLength(short type);
	
	/*
	 * ECLGetListCapabilities - WS Execution control list routine to get the
	 * capabilites for the given signer
	 *
	 * Lookup the user in the ECL and return capabilities.
	 * 
	 * Inputs: pNamesList - pointer to a NAMES_LIST that contains signer's name
	 * and all the signer's alternate names. ECLType - Type of ECL to look up.
	 *
	 * Outputs: retwCapabilites - Capabilites (ECL_FLAG_xxx | ECL_FLAG_yyy, etc)
	 * retwCapabilites2 - Extended Capabilites for Workstation ECL
	 * retfUserCanModifyECL - (optional) set to TRUE if user is allowed to
	 * modify the ECL
	 *
	 * Returns: STATUS
	 */
	public short ECLGetListCapabilities(Pointer pNamesList, short ECLType, ShortByReference retwCapabilities,
			ShortByReference retwCapabilities2, IntByReference retfUserCanModifyECL);

	public short ECL_TYPE_LOTUS_SCRIPT = 0;
	public short ECL_TYPE_JAVA_APPLET = 1;
	public short ECL_TYPE_JAVASCRIPT = 2;

	/** Access files (read/write/export/import)*/
	public short ECL_FLAG_FILES = 0x0008;
	/** Access current db's docs/db */
	public short ECL_FLAG_DOCS_DBS = 0x0010;

	/** Access environ vars (get/set) */
	public short ECL_FLAG_ENVIRON = 0x0080;
	/** Access non-notes dbs (@DB with non "","Notes" first arg) */
	public short ECL_FLAG_EXTERN_DBS = 0x0100;
	/** Access "code" in external systems (LS, DLLS, DDE) */
	public short ECL_FLAG_EXTERN_CODE = 0x0200;
	/** Access external programs (OLE/SendMsg/Launch) */
	public short ECL_FLAG_EXTERN_PROGRAMS = 0x0400;
	/** Send mail (@MailSend) */
	public short ECL_FLAG_SEND_MAIL = 0x0800;
	/** Access ECL */
	public short ECL_FLAG_ECL = 0x1000;
	/** Read access to other databases */
	public short ECL_FLAG_READ_OTHER_DBS = 0x2000;
	/** Write access to other databases */
	public short ECL_FLAG_WRITE_OTHER_DBS = 0x4000;
	/** Ability to export data (copy/print, etc) */
	public short ECL_FLAG_EXPORT_DATA = (short) (0x8000 & 0xffff);

	/* extended acl flags */
	
	/** Access network programatically */
	public short ECL_FLAG_NETWORK = 0x0001;
	/** Property Broker Get */
	public short ECL_FLAG_PROPERTY_GET = 0x0002;
	/** Property Broker Put */
	public short ECL_FLAG_PROPERTY_PUT = 0x0004;
	/** Widget configuration */
	public short ECL_FLAG_WIDGETS = 0x0008;
	/** access to load Java */
	public short ECL_FLAG_LOADJAVA = 0x0010;

	
	/*
	 * Set the agent's return http status code
	 *
	 * hAgentCtx - agent context
	 * httpStatus - http status code
	 */
	public short b64_AgentSetHttpStatusCode(long hAgentCtx, int httpStatus);
	public short b32_AgentSetHttpStatusCode(int hAgentCtx, int httpStatus);

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
	public void b32_NSFNoteSetInfo(int hNote, short type, Pointer value);
	public void b64_NSFNoteSetInfo(long hNote, short type, Pointer value);

	public short b32_NSFNoteContract(int hNote);
	public short b64_NSFNoteContract(long hNote);
	public short b32_NSFNoteExpand(int hNote);
	public short b64_NSFNoteExpand(long hNote);
	public short b32_NSFNoteClose(int hNote);
	public short b64_NSFNoteClose(long hNote);
	public short b32_NSFNoteSign(int hNote);
	public short b64_NSFNoteSign(long hNote);

	public short b32_NSFNoteUpdateExtended(int hNote, int updateFlags);
	public short b64_NSFNoteUpdateExtended(long hNote, int updateFlags);

	public short b64_NSFNoteUpdate(long note_handle, short update_flags);
	public short b32_NSFNoteUpdate(int note_handle, short update_flags);
	
	public short b32_NSFNoteCreate(int db_handle, IntByReference note_handle);
	public short b64_NSFNoteCreate(long db_handle, LongByReference note_handle);

	public short b32_NSFNoteOpen(int hDB, int noteId, short openFlags, IntByReference rethNote);
	public short b64_NSFNoteOpen(long hDB, int noteId, short openFlags, LongByReference rethNote);
	public short b32_NSFNoteOpenExt(int hDB, int noteId, int flags, IntByReference rethNote);
	public short b64_NSFNoteOpenExt(long hDB, int noteId, int flags, LongByReference rethNote);

	public short b64_NSFNoteOpenSoftDelete(long hDB, int NoteID, int Reserved, LongByReference rethNote);
	public short b32_NSFNoteOpenSoftDelete(int hDB, int NoteID, int Reserved, IntByReference rethNote);
	
	public short b64_NSFNoteHardDelete(long hDB, int NoteID, int Reserved);
	public short b32_NSFNoteHardDelete(int hDB, int NoteID, int Reserved);

	public short b32_NSFNoteDeleteExtended(int hDB, int NoteID, int UpdateFlags);
	public short b64_NSFNoteDeleteExtended(long hDB, int NoteID, int UpdateFlags);
	
	public short b64_NSFNoteDetachFile(long note_handle, NotesBlockId.ByValue item_blockid);
	public short b32_NSFNoteDetachFile(int note_handle, NotesBlockId.ByValue item_blockid);
	
	public boolean b64_NSFNoteIsSignedOrSealed(long note_handle, ByteByReference signed_flag_ptr, ByteByReference sealed_flag_ptr);
	public boolean b32_NSFNoteIsSignedOrSealed(int note_handle, ByteByReference signed_flag_ptr, ByteByReference sealed_flag_ptr);
	
	public short b32_NSFNoteOpenByUNID(
			int hDB,
			NotesUniversalNoteId pUNID,
			short  flags,
			IntByReference rethNote);
	public short b64_NSFNoteOpenByUNID(
			long  hDB,
			NotesUniversalNoteId pUNID,
			short  flags,
			LongByReference rethNote);

	public short b32_NSFNoteUnsign(int hNote);
	public short b64_NSFNoteUnsign(long hNote);

	public short b64_NSFNoteComputeWithForm(
			long  hNote,
			long  hFormNote,
			int  dwFlags,
			b64_CWFErrorProc ErrorRoutine,
			Pointer CallersContext);
	
	public short b32_NSFNoteComputeWithForm(
			int  hNote,
			int  hFormNote,
			int  dwFlags,
			b32_CWFErrorProc ErrorRoutine,
			Pointer CallersContext);
	
	/* 	Possible validation phases for NSFNoteComputeWithForm()  */
	public short CWF_DV_FORMULA = 1;
	public short CWF_IT_FORMULA	= 2;
	public short CWF_IV_FORMULA = 3;
	public short CWF_COMPUTED_FORMULA = 4;
	public short CWF_DATATYPE_CONVERSION = 5;
	public short CWF_COMPUTED_FORMULA_LOAD = CWF_COMPUTED_FORMULA;
	public short CWF_COMPUTED_FORMULA_SAVE = 6;

	public short b32_NSFItemInfo(
			int  note_handle,
			Memory item_name,
			short name_len,
			NotesBlockId retbhItem,
			ShortByReference retDataType,
			NotesBlockId retbhValue,
			IntByReference retValueLength);
	
	public short b64_NSFItemInfo(
			long note_handle,
			Memory item_name,
			short  name_len,
			NotesBlockId retbhItem,
			ShortByReference retDataType,
			NotesBlockId retbhValue,
			IntByReference retValueLength);
	
//	STATUS LNPUBLIC NSFItemInfo(
//			NOTEHANDLE  note_handle,
//			const char far *item_name,
//			WORD  name_len,
//			BLOCKID far *item_blockid,
//			WORD far *value_datatype,
//			BLOCKID far *value_blockid,
//			DWORD far *value_len);
	
	public short b32_NSFItemInfoNext(
			int  note_handle,
			NotesBlockId.ByValue NextItem,
			Memory item_name,
			short name_len,
			NotesBlockId retbhItem,
			ShortByReference retDataType,
			NotesBlockId retbhValue,
			IntByReference retValueLength);
	
	public short b64_NSFItemInfoNext(
			long  note_handle,
			NotesBlockId.ByValue NextItem,
			Memory item_name,
			short  name_len,
			NotesBlockId retbhItem,
			ShortByReference retDataType,
			NotesBlockId retbhValue,
			IntByReference retValueLength);
	
	public short b32_NSFItemInfoPrev(
			int  note_handle,
			NotesBlockId.ByValue  CurrItem,
			Memory item_name,
			short  name_len,
			NotesBlockId item_blockid_ptr,
			ShortByReference value_type_ptr,
			NotesBlockId value_blockid_ptr,
			IntByReference value_len_ptr);
	
	public short b64_NSFItemInfoPrev(
			long  note_handle,
			NotesBlockId.ByValue  CurrItem,
			Memory item_name,
			short  name_len,
			NotesBlockId item_blockid_ptr,
			ShortByReference value_type_ptr,
			NotesBlockId value_blockid_ptr,
			IntByReference value_len_ptr);
	
	public short b64_NSFItemScan(
			long  note_handle,
			NoteNsfItemScanProc ActionRoutine,
			Pointer funcParam);

	public short b32_NSFItemScan(
			int  note_handle,
			NoteNsfItemScanProc ActionRoutine,
			Pointer funcParam);

	public void b32_NSFItemQueryEx(
			int  note_handle,
			NotesBlockId.ByValue item_bid,
			Memory item_name,
			short  return_buf_len,
			ShortByReference name_len_ptr,
			ShortByReference item_flags_ptr,
			ShortByReference value_datatype_ptr,
			NotesBlockId value_bid_ptr,
			IntByReference value_len_ptr,
			ByteByReference retSeqByte,
			ByteByReference retDupItemID);
	
	public void b64_NSFItemQueryEx(
			long  note_handle,
			NotesBlockId.ByValue item_bid,
			Memory item_name,
			short  return_buf_len,
			ShortByReference name_len_ptr,
			ShortByReference item_flags_ptr,
			ShortByReference value_datatype_ptr,
			NotesBlockId value_bid_ptr,
			IntByReference value_len_ptr,
			ByteByReference retSeqByte,
			ByteByReference retDupItemID);
	
	public short b32_NSFItemGetModifiedTime(
			int hNote,
			Memory ItemName,
			short  ItemNameLength,
			int  Flags,
			NotesTimeDate retTime);
	
	public short b64_NSFItemGetModifiedTime(
			long hNote,
			Memory ItemName,
			short  ItemNameLength,
			int  Flags,
			NotesTimeDate retTime);
	
	public short b32_NSFItemGetModifiedTimeByBLOCKID(
			int  hNote,
			NotesBlockId.ByValue bhItem,
			int  Flags,
			NotesTimeDate retTime);
	
	public short b64_NSFItemGetModifiedTimeByBLOCKID(
			long  hNote,
			NotesBlockId.ByValue bhItem,
			int  Flags,
			NotesTimeDate retTime);
	
	public short b32_NSFItemGetText(
			int  note_handle,
			Memory item_name,
			Memory item_text,
			short text_len);
	
	public short b64_NSFItemGetText(
			long  note_handle,
			Memory item_name,
			Memory item_text,
			short text_len);
	
	public short b64_NSFItemGetTextListEntries(
			long note_handle,
			Memory item_name);

	public short b32_NSFItemGetTextListEntries(
			int note_handle,
			Memory item_name);
	
	public short b64_NSFItemGetTextListEntry(
			long note_handle,
			Memory item_name,
			short entry_position,
			Memory retEntry_text,
			short  text_len);

	public short b32_NSFItemGetTextListEntry(
			int note_handle,
			Memory item_name,
			short entry_position,
			Memory retEntry_text,
			short  text_len);

	public short b32_NSFItemSetTextSummary(
			int hNote,
			Memory ItemName,
			Memory ItemText,
			short TextLength,
			boolean summary);
	
	public short b64_NSFItemSetTextSummary(
			long  hNote,
			Memory ItemName,
			Memory ItemText,
			short TextLength,
			boolean summary);

	public boolean b32_NSFItemGetTime(
			int  note_handle,
			Memory td_item_name,
			NotesTimeDate td_item_value);
	
	public boolean b64_NSFItemGetTime(
			long  note_handle,
			Memory td_item_name,
			NotesTimeDate td_item_value);
	
	public short b32_NSFItemSetTime(
			int  note_handle,
			Memory td_item_name,
			NotesTimeDate td_item_ptr);
	
	public short b64_NSFItemSetTime(
			long  note_handle,
			Memory td_item_name,
			NotesTimeDate td_item_ptr);

	public boolean b32_NSFItemGetNumber(
			int hNote,
			Memory ItemName,
			DoubleByReference retNumber);
	
	public boolean b64_NSFItemGetNumber(
			long  hNote,
			Memory ItemName,
			DoubleByReference retNumber);
	
	public long b32_NSFItemGetLong(
			int note_handle,
			Memory number_item_name,
			LongByReference number_item_default);

	public long b64_NSFItemGetLong(
			long note_handle,
			Memory number_item_name,
			LongByReference number_item_default);

	public short b32_NSFItemSetNumber(
			int  hNote,
			Memory ItemName,
			Memory Number);

	public short b64_NSFItemSetNumber(
			long hNote,
			Memory ItemName,
			Memory Number);

	public short b64_NSFItemConvertToText(
			long note_handle,
			Memory item_name_ptr,
			Memory retText_buf_ptr,
			short  text_buf_len,
			char separator);
	
	public short b32_NSFItemConvertToText(
			int note_handle,
			Memory item_name_ptr,
			Memory retText_buf_ptr,
			short  text_buf_len,
			char separator);

	public short b64_NSFItemConvertValueToText(
			short value_type,
			NotesBlockId.ByValue value_bid,
			int  value_len,
			Memory text_buf_ptr,
			short  text_buf_len,
			char separator);

	public short b32_NSFItemConvertValueToText(
			short value_type,
			NotesBlockId.ByValue value_bid,
			int  value_len,
			Memory text_buf_ptr,
			short  text_buf_len,
			char separator);

	public short b64_NSFItemDelete(
			long note_handle,
			Memory item_name,
			short name_len);

	public short b32_NSFItemDelete(
			int note_handle,
			Memory item_name,
			short name_len);

	public short b64_NSFItemDeleteByBLOCKID(long note_handle, NotesBlockId.ByValue item_blockid);
	public short b32_NSFItemDeleteByBLOCKID(int note_handle, NotesBlockId.ByValue item_blockid);
	
	public short b64_NSFItemCopy(long note_handle, NotesBlockId.ByValue item_blockid);
	public short b32_NSFItemCopy(int note_handle, NotesBlockId.ByValue item_blockid);

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

	public short b32_NSFDbGetNoteInfoExt(
			int  hDB,
			int  NoteID,
			NotesOriginatorId retNoteOID,
			NotesTimeDate retModified,
			ShortByReference retNoteClass,
			NotesTimeDate retAddedToFile,
			ShortByReference retResponseCount,
			IntByReference retParentNoteID);

	public short b64_NSFDbGetNoteInfoExt(
			long  hDB,
			int  NoteID,
			NotesOriginatorId retNoteOID,
			NotesTimeDate retModified,
			ShortByReference retNoteClass,
			NotesTimeDate retAddedToFile,
			ShortByReference retResponseCount,
			IntByReference retParentNoteID);

	public short b32_NSFNoteVerifySignature(
			int  hNote,
			Memory SignatureItemName,
			NotesTimeDate retWhenSigned,
			Memory retSigner,
			Memory retCertifier);
	
	public short b64_NSFNoteVerifySignature(
			long  hNote,
			Memory SignatureItemName,
			NotesTimeDate retWhenSigned,
			Memory retSigner,
			Memory retCertifier);
	
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

	/*	Item Flags */
	// These flags define the characteristics of an item (field) in a note.  The flags may be bitwise or'ed together for combined functionality.
	
	/** This item is signed. */
	public static final short ITEM_SIGN = 0x0001;
	
	/** This item is sealed. When used in NSFItemAppend, the item is encryption
	 * enabled; it can later be encrypted if edited from the Notes UI and saved
	 * in a form that specifies Encryption. */
	public static final short ITEM_SEAL = 0x0002;
	
	/** This item is stored in the note's summary buffer. Summary items may be used
	 * in view columns, selection formulas, and @-functions. Summary items may be
	 * accessed via the SEARCH_MATCH structure provided by NSFSearch or in the
	 * buffer returned by NIFReadEntries. API program may read, modify, and write
	 * items in the summary buffer without opening the note first. The maximum size
	 * of the summary buffer is 32K. Items of TYPE_COMPOSITE may not have the
	 * ITEM_SUMMARY flag set. */
	public static final short ITEM_SUMMARY	= 0x0004;
	
	/** This item is an Author Names field as indicated by the READ/WRITE-ACCESS
	 * flag. Item is TYPE_TEXT or TYPE_TEXT_LIST. Author Names fields have the
	 * ITEM_READWRITERS flag or'd with the ITEM_NAMES flag. */
	public static final short ITEM_READWRITERS = 0x0020;
	
	/** This item is a Names field. Indicated by the NAMES (distinguished names)
	 * flag. Item is TYPE_TEXT or TYPE_TEXT_LIST. */
	public static final short ITEM_NAMES = 0x0040;
	
	/** This item is a placeholder field in a form note. Item is TYPE_INVALID_OR_UNKNOWN. */
	public static final short ITEM_PLACEHOLDER = 0x0100;
	
	/** A user requires editor access to change this field. */
	public static final short ITEM_PROTECTED = 0x0200;
	
	/** This is a Reader Names field. Indicated by the READER-ACCESS flag. Item is
	 * TYPE_TEXT or TYPE_TEXT_LIST. */
	public static final short ITEM_READERS = 0x0400;
	
	/**  Item is same as on-disk. */
	public static final short ITEM_UNCHANGED = 0x1000;
	
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
	
	public short b64_NSFDbReadObject(
			long hDB,
			int ObjectID,
			int Offset,
			int Length,
			LongByReference rethBuffer);

	public short b32_NSFDbReadObject(
			int hDB,
			int ObjectID,
			int Offset,
			int Length,
			IntByReference rethBuffer);

	public short b64_NSFDbGetObjectSize(
			long hDB,
			int ObjectID,
			short ObjectType,
			IntByReference retSize,
			ShortByReference retClass,
			ShortByReference retPrivileges);
	public short b32_NSFDbGetObjectSize(
			int hDB,
			int ObjectID,
			short ObjectType,
			IntByReference retSize,
			ShortByReference retClass,
			ShortByReference retPrivileges);

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

//	STATUS far PASCAL NSFSearchExtended3 (DBHANDLE hDB,
//            FORMULAHANDLE hFormula,
//            DHANDLE hFilter,
//            DWORD FilterFlags,
//            char *ViewTitle,
//            DWORD SearchFlags,
//            DWORD SearchFlags1,
//            DWORD SearchFlags2,
//            DWORD SearchFlags3,
//            DWORD SearchFlags4,
//            WORD NoteClassMask,
//            TIMEDATE *Since,
//            STATUS (far PASCAL *EnumRoutine)
//                                    (void *EnumRoutineParameter,
//                                    SEARCH_MATCH *SearchMatch,
//                                    ITEM_TABLE *SummaryBuffer),
//            void *EnumRoutineParameter,
//            TIMEDATE *retUntil,
//            DHANDLE namelist) 
//	
	public short b64_NSFSearchExtended3 (long hDB, 
            long hFormula, 
            long hFilter, 
            int filterFlags, 
            Memory ViewTitle, 
            int SearchFlags, 
            int SearchFlags1, 
            int SearchFlags2, 
            int SearchFlags3, 
            int SearchFlags4, 
            short NoteClassMask, 
            NotesTimeDate Since, 
            b64_NsfSearchProc  EnumRoutine,
            Pointer EnumRoutineParameter, 
            NotesTimeDate retUntil, 
            long namelist);

	public short b32_NSFSearchExtended3 (int hDB, 
            int hFormula, 
            int hFilter, 
            int FilterFlags, 
            Memory ViewTitle, 
            int SearchFlags, 
            int SearchFlags1, 
            int SearchFlags2, 
            int SearchFlags3, 
            int SearchFlags4, 
            short NoteClassMask, 
            NotesTimeDate Since, 
            b32_NsfSearchProc  EnumRoutine,
            Pointer EnumRoutineParameter, 
            NotesTimeDate retUntil, 
            int namelist);

	//Get filter information needed to do a NSFSearchStart via a FOLDER search
	public short b64_NSFGetFolderSearchFilter(long hViewDB, long hDataDB, int ViewNoteID, NotesTimeDate Since, int Flags, LongByReference Filter);
	public short b32_NSFGetFolderSearchFilter(int hViewDB, int hDataDB, int ViewNoteID, NotesTimeDate Since, int Flags, IntByReference Filter);
	
	public short b64_NSFSearchWithUserNameList(
			long hDB,
			long hFormula,
			Memory ViewTitle,
			short  SearchFlags,
			short  NoteClassMask,
			NotesTimeDate Since,
			b64_NsfSearchProc  EnumRoutine,
			Pointer EnumRoutineParameter,
			NotesTimeDate retUntil,
			long  nameList);

	public short b32_NSFSearchWithUserNameList(
			int hDB,
			int hFormula,
			Memory ViewTitle,
			short  SearchFlags,
			short  NoteClassMask,
			NotesTimeDate Since,
			b32_NsfSearchProc  EnumRoutine,
			Pointer EnumRoutineParameter,
			NotesTimeDate retUntil,
			int nameList);

	/** No filter specified (hFilter ignored). */
	public int SEARCH_FILTER_NONE = 0x00000000;
	/** hFilter is a Note ID table. */
	public int SEARCH_FILTER_NOTEID_TABLE = 0x00000001;
	/** hFilter is a View note handle */
	public int SEARCH_FILTER_FOLDER = 0x00000002;
	/** Filter on particular Properties. */
	public int SEARCH_FILTER_DBDIR_PROPERTY = 0x00000004;
	/** Filter on Database Options (bits set). */
	public int SEARCH_FILTER_DBOPTIONS = 0x00000010;
	/** Filter on Database Options (bits clear). */
	public int SEARCH_FILTER_DBOPTIONS_CLEAR = 0x00000020;
	/** Filter based on a set of form names */
	public int SEARCH_FILTER_FORMSKIMMED = 0x00000040;
	/** Don't try to filter on form names, we know it won't work */
	public int SEARCH_FILTER_NOFORMSKIMMED = 0x00000080;
	/** Filter on Query View SQL */
	public int SEARCH_FILTER_QUERY_VIEW = 0x00000100;
	/** Filter on item revision times */
	public int SEARCH_FILTER_ITEM_TIME = 0x00000200;
	/** Filter on time range input */
	public int SEARCH_FILTER_RANGE = 0x00000400;
	/** Filter out .ndx files */
	public int SEARCH_FILTER_NO_NDX = 0x00000800;
	/** Search for databases with inline indexing */
	public int SEARCH_FILTER_INLINE_INDEX = 0x00001000;

	/**
	 * Include deleted and non-matching notes in search (ALWAYS "ON" in partial
	 * searches!)
	 */
	public int SEARCH_ALL_VERSIONS = 0x0001;

	/** obsolete synonym */
	public int SEARCH_INCLUDE_DELETED = SEARCH_ALL_VERSIONS;

	/** TRUE to return summary buffer with each match */
	public int SEARCH_SUMMARY = 0x0002;
	/**
	 * For directory mode file type filtering. If set, "NoteClassMask" is
	 * treated as a FILE_xxx mask for directory filtering
	 */
	public int SEARCH_FILETYPE = 0x0004;

	/** special caching for dir scan */
	public int SEARCH_SERVERCACHE = 0x0008;

	/** Set NOTE_CLASS_NOTIFYDELETION bit of NoteClass for deleted notes */
	public int SEARCH_NOTIFYDELETIONS = 0x0010;

	/** do not put item names into summary info */
	public int SEARCH_NOITEMNAMES = 0x0020;
	/** return error if we don't have full privileges */
	public int SEARCH_ALLPRIVS = 0x0040;

	/** for dir scans, only return files needing fixup */
	public int SEARCH_FILEFIXUP = 0x0080;
	/** Formula buffer is hashed UNID table */
	public int SEARCH_UNID_TABLE = 0x0100;
	/** Return buffer in canonical form */
	public int SEARCH_CANONICAL = 0x0200;
	/** Use current session's user name, not server's */
	public int SEARCH_SESSION_USERNAME = 0x0400;
	/** Allow search to return id's only, i.e. no summary buffer */
	public int SEARCH_NOPRIVCHECK = 0x0800;
	/** Filter out "Truncated" documents */
	public int SEARCH_NOABSTRACTS = 0x1000;
	/** Perform unread flag sync */
	public int SEARCH_SYNC = 0x2000;

	/** Search formula applies only to data notes, i.e., others match */
	public int SEARCH_DATAONLY_FORMULA = 0x4000;
	/** INCLUDE notes with non-replicatable OID flag */
	public int SEARCH_NONREPLICATABLE = 0x8000;
	/**
	 * SEARCH_MATCH is V4 style. That is MatchesFormula is now a bit field where
	 * the lower bit indicates whether the document matches. If it does, the
	 * other bits provide additional information regarding the note.
	 */
	public int SEARCH_V4INFO = 0x00010000;
	/** Search includes all children of matching documents. */
	public int SEARCH_ALLCHILDREN = 0x00020000;
	/** Search includes all descendants of matching documents. */
	public int SEARCH_ALLDESCENDANTS = 0x00040000;
	/** First pass in a multipass hierarchical search. */
	public int SEARCH_FIRSTPASS = 0x00080000;
	/** Descendants were added on this pass. */
	public int SEARCH_DESCENDANTSADDED = 0x00100000;
	/** Formula is an Array of Formulas. */
	public int SEARCH_MULTI_FORMULA = 0x00200000;
	/** Return purged note ids as deleted notes. */
	public int SEARCH_INCLUDE_PURGED = 0x00400000;
	/** Only return templates without the "advanced" bit set */
	public int SEARCH_NO_ADV_TEMPLATES = 0x00800000;
	/** Only Private Views or Agents */
	public int SEARCH_PRIVATE_ONLY = 0x01000000;
	/**
	 * Full search (as if Since was "1") but exclude DATA notes prior to
	 * passed-in Since time
	 */
	public int SEARCH_FULL_DATACUTOFF = 0x02000000;

	/**
	 * If specified, the progress field in the SEARCH_ENTRY structure will be
	 * filled in. This avoids performing the calculation if it was not wanted.
	 */
	public int SEARCH_CALC_PROGRESS = 0x04000000;
	/**
	 * Include *** ALL *** named ghost notes in the search (profile docs,
	 * xACL's, etc). Note: use SEARCH1_PROFILE_DOCS, etc., introduced in R6, for
	 * finer control
	 */
	public int SEARCH_NAMED_GHOSTS = 0x08000000;
	/** Perform optimized unread sync */
	public int SEARCH_SYNC_OPTIMIZED = 0x10000000;
	/**
	 * Return only docs with protection fields (BS_PROTECTED set in note header)
	 */
	public int SEARCH_ONLYPROTECTED = 0x20000000;
	/** Return soft deleted documents */
	public int SEARCH_SOFTDELETIONS = 0x40000000;

	/** for setting/verifying that bits 28-31 of search 1 flags are 1000 */
	public int SEARCH1_SIGNATURE = 0x80000000;

	public int SEARCH1_SELECT_NAMED_GHOSTS = (0x00000001 | SEARCH1_SIGNATURE);

	/**
	 * Include profile documents (a specific type of named ghost note) in the
	 * search Note: set SEARCH1_SELECT_NAMED_GHOSTS, too, if you want the
	 * selection formula to be applied to the profile docs (so as not to get
	 * them all back as matches).
	 */
	public int SEARCH1_PROFILE_DOCS = (0X00000002 | SEARCH1_SIGNATURE);

	/**
	 * Skim off notes whose summary buffer can't be generated because its size
	 * is too big.
	 */
	public int SEARCH1_SKIM_SUMMARY_BUFFER_TOO_BIG = (0x00000004 | SEARCH1_SIGNATURE);
	public int SEARCH1_RETURN_THREAD_UNID_ARRAY = (0x00000008 | SEARCH1_SIGNATURE);
	public int SEARCH1_RETURN_TUA = SEARCH1_RETURN_THREAD_UNID_ARRAY;

	/**
	 * flag for reporting noaccess in case of reader's field at the doc level
	 */
	public int SEARCH1_REPORT_NOACCESS = (0x000000010 | SEARCH1_SIGNATURE);
	/** Search "Truncated" documents */
	public int SEARCH1_ONLY_ABSTRACTS = (0x000000020 | SEARCH1_SIGNATURE);
	/**
	 * Search documents fixup purged. This distinct and mutually exlusive from
	 * SEARCH_INCLUDE_PURGED which is used for view processing by NIF etc to
	 * remove purged notes from views. This is used for replication restoring
	 * corrupt documents.
	 */
	public int SEARCH1_FIXUP_PURGED = (0x000000040 | SEARCH1_SIGNATURE);

	public interface b32_NsfSearchProc extends Callback { /* StdCallCallback if using __stdcall__ */
        void invoke(Pointer enumRoutineParameter, NotesSearchMatch32 searchMatch, NotesItemTable summaryBuffer); 
    }
	
	public interface b64_NsfSearchProc extends Callback { /* StdCallCallback if using __stdcall__ */
        void invoke(Pointer enumRoutineParameter, NotesSearchMatch64 searchMatch, NotesItemTable summaryBuffer); 
    }

	public interface NoteExtractCallback extends Callback { /* StdCallCallback if using __stdcall__ */
        short invoke(Pointer data, int length, Pointer param); 
    }

	public interface NoteNsfItemScanProc extends Callback { /* StdCallCallback if using __stdcall__ */
        void invoke(short spare, short itemFlags, Pointer name, short nameLength, Pointer value, int valueLength, Pointer routineParameter); 
    }

	public interface b64_CWFErrorProc extends Callback { /* StdCallCallback if using __stdcall__ */
		short invoke(Pointer pCDField, short phase, short error, long hErrorText, short wErrorTextSize, Pointer ctx); 
    }

	public interface b32_CWFErrorProc extends Callback { /* StdCallCallback if using __stdcall__ */
       short invoke(Pointer pCDField, short phase, short error, int hErrorText, short wErrorTextSize, Pointer ctx); 
    }

	public int CWF_CONTINUE_ON_ERROR = 0x0001;		/*	Ignore compute errors */

	public short b64_NSFNoteCipherExtractWithCallback (long hNote, NotesBlockId.ByValue bhItem,
			int ExtractFlags, int hDecryptionCipher,
			NoteExtractCallback pNoteExtractCallback, Pointer pParam,
			int Reserved, Pointer pReserved);

	public short b32_NSFNoteCipherExtractWithCallback (int hNote, NotesBlockId.ByValue bhItem,
			int ExtractFlags, int hDecryptionCipher,
			NoteExtractCallback pNoteExtractCallback, Pointer pParam,
			int Reserved, Pointer pReserved);

	public short b64_NSFNoteAttachFile(
			long note_handle,
			Memory item_name,
			short item_name_length,
			Memory file_name,
			Memory orig_path_name,
			short encoding_type);
	
	public short b32_NSFNoteAttachFile(
			int note_handle,
			Memory item_name,
			short item_name_length,
			Memory file_name,
			Memory orig_path_name,
			short encoding_type);
	
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

	public short b64_NSFComputeStart(
			short Flags,
			Pointer lpCompiledFormula,
			LongByReference rethCompute);

	public short b32_NSFComputeStart(
			short Flags,
			Pointer lpCompiledFormula,
			IntByReference rethCompute);

	public short b64_NSFComputeStop(long hCompute);
	public short b32_NSFComputeStop(int hCompute);

	public short b64_NSFComputeEvaluate(
			long  hCompute,
			long hNote,
			LongByReference rethResult,
			ShortByReference retResultLength,
			IntByReference retNoteMatchesFormula,
			IntByReference retNoteShouldBeDeleted,
			IntByReference retNoteModified);
	
	public short b32_NSFComputeEvaluate(
			int  hCompute,
			int hNote,
			IntByReference rethResult,
			ShortByReference retResultLength,
			IntByReference retNoteMatchesFormula,
			IntByReference retNoteShouldBeDeleted,
			IntByReference retNoteModified);

	public short b32_AgentOpen (int hDB, int AgentNoteID, IntByReference rethAgent);
	public short b64_AgentOpen (long hDB, int AgentNoteID, LongByReference rethAgent);
	
	public void b32_AgentClose (int hAgent);
	public void b64_AgentClose (long hAgent);
	
	/* Use this flag to tell the run context that when it runs an
		agent, you want it to check the privileges of the signer of
		that agent and apply them. For example, if the signer of the
		agent has "restricted" agent privileges, then the agent will
		be restricted. If you don't set this flag, all agents run as
		unrestricted.
		
		List of security checks enabled by this flag:
			Restricted/unrestricted agent
			Can create databases
			Is agent targeted to this machine
			Is user allowed to access this machine
			Can user run personal agents
	*/

	public static final int AGENT_SECURITY_OFF = 0x00;		/* CreateRunContext */
	public static final int AGENT_SECURITY_ON = 0x01;		/* CreateRunContext */
	public static final int AGENT_REOPEN_DB = 0x10;		/* AgentRun */

	public short b32_AgentCreateRunContext (int hAgent,
											 Pointer pReserved,
											 int dwFlags,
											 IntByReference rethContext);
	
	public short b64_AgentCreateRunContext (long hAgent,
			 Pointer pReserved,
			 int dwFlags,
			 LongByReference rethContext);
	
	public short b64_AgentCreateRunContextExt (long hAgent, Pointer pReserved, long pOldContext, int dwFlags, LongByReference rethContext);
	public short b32_AgentCreateRunContextExt (int hAgent, Pointer pReserved, int pOldContext, int dwFlags, IntByReference rethContext);
	
	public short b32_AgentSetDocumentContext(int hAgentCtx, int hNote);
	public short b64_AgentSetDocumentContext(long hAgentCtx, long hNote);
	
	/* allow api users to set time execution limit. if not set, default is 0 which means no limit */
	public short b32_AgentSetTimeExecutionLimit(int hAgentCtx, int timeLimit);
	public short b64_AgentSetTimeExecutionLimit(long hAgentCtx, int timeLimit);

	/* allow api users to find out if the agent is enabled */
	public boolean b32_AgentIsEnabled(int hAgent);
	public boolean b64_AgentIsEnabled(long hAgent);

	public void b64_SetParamNoteID(long hAgentCtx, int noteId);
	public void b32_SetParamNoteID(int hAgentCtx, int noteId);

	public short b64_AgentSetUserName(long hAgentCtx, long hNameList);
	public short b32_AgentSetUserName(int hAgentCtx, int hNameList);

	/* Definitions for stdout redirection types. This specifies where
		output from the LotusScript "print" statement will go */

	public static short AGENT_REDIR_NONE = 0;		/* goes to the bit bucket */
	public static short AGENT_REDIR_LOG	= 1;		/* goes to the Notes log (default) */
	public static short AGENT_REDIR_MEMORY = 2;		/* goes to a memory buffer, cleared each AgentRun */
	public static short AGENT_REDIR_MEMAPPEND = 3;		/* goes to buffer, append mode for each agent */

	public short b32_AgentRedirectStdout(int hAgentCtx, short redirType);
	public short b64_AgentRedirectStdout(long hAgentCtx, short redirType);
	
	public void b32_AgentQueryStdoutBuffer(int hAgentCtx, IntByReference retHdl, IntByReference retSize);
	public void b64_AgentQueryStdoutBuffer(long hAgentCtx, LongByReference retHdl, IntByReference retSize);
	
	public void b32_AgentDestroyRunContext (int hAgentCtx);
	public void b64_AgentDestroyRunContext (long hAgentCtx);
	
	public short AgentDelete (int hAgent); /* delete agent */
	public short AgentDelete (long hAgent); /* delete agent */
	
	public boolean b64_IsRunAsWebUser(long hAgent);
	public boolean b32_IsRunAsWebUser(int hAgent);



	/* If AGENT_REOPEN_DB is set, the AgentRun call will reopen
		the agent's database with the privileges of the signer of
		the agent. If the flag is not set, the agent's "context"
		database will be open with the privileges of the current
		user (meaning, the current Notes id, or the current Domino
		web user). */

	public short b32_AgentRun (int hAgent,
								int hAgentCtx,
							    int hSelection,
								int dwFlags);
	public short b64_AgentRun (long hAgent,
			long hAgentCtx,
		    int hSelection,
			int dwFlags);

	
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
			int  dwSize,
			IntByReference retHandle);
	
	public short b64_OSMemAlloc(
			short  BlkType,
			int  dwSize,
			LongByReference retHandle);

	public static final int _TIMEDATE = 10;
	public static final int _TIMEDATE_PAIR = 11;
	public static final int _ALIGNED_NUMBER_PAIR = 12;
	public static final int _LIST = 13;
	public static final int _RANGE = 14;
	public static final int _DBID = 15;
	public static final int _ITEM = 17;
	public static final int _ITEM_TABLE = 18;
	public static final int _SEARCH_MATCH = 24;
	public static final int _ORIGINATORID = 26;
	public static final int _OID = _ORIGINATORID;
	public static final int _OBJECT_DESCRIPTOR = 27;
	public static final int _UNIVERSALNOTEID = 28;
	public static final int _UNID = _UNIVERSALNOTEID;
	public static final int _VIEW_TABLE_FORMAT = 29;
	public static final int _VIEW_COLUMN_FORMAT = 30;
	public static final int _NOTELINK = 33;
	public static final int _LICENSEID = 34;
	public static final int _VIEW_FORMAT_HEADER = 42;
	public static final int _VIEW_TABLE_FORMAT2 = 43;
	public static final int _DBREPLICAINFO = 56;
	public static final int _FILEOBJECT = 58;
	public static final int _COLLATION = 59;
	public static final int _COLLATE_DESCRIPTOR = 60;
	public static final int _CDKEYWORD = 68;
	public static final int _CDLINK2 = 72;
	public static final int _CDLINKEXPORT2 = 97;
	public static final int _CDPARAGRAPH = 109;
	public static final int _CDPABDEFINITION = 110;
	public static final int _CDPABREFERENCE = 111;
	public static final int _CDFIELD_PRE_36 = 112;
	public static final int _CDTEXT = 113;
	public static final int _CDDOCUMENT = 114;
	public static final int _CDMETAFILE = 115;
	public static final int _CDBITMAP = 116;
	public static final int _CDHEADER = 117;
	public static final int _CDFIELD = 118;
	public static final int _CDFONTTABLE = 119;
	public static final int _CDFACE = 120;
	public static final int _CDCGM = 156;
	public static final int _CDTIFF = 159;
	public static final int _CDBITMAPHEADER = 162;
	public static final int _CDBITMAPSEGMENT = 163;
	public static final int _CDCOLORTABLE = 164;
	public static final int _CDPATTERNTABLE = 165;
	public static final int _CDGRAPHIC = 166;
	public static final int _CDPMMETAHEADER = 167;
	public static final int _CDWINMETAHEADER = 168;
	public static final int _CDMACMETAHEADER = 169;
	public static final int _CDCGMMETA = 170;
	public static final int _CDPMMETASEG = 171;
	public static final int _CDWINMETASEG = 172;
	public static final int _CDMACMETASEG = 173;
	public static final int _CDDDEBEGIN = 174;
	public static final int _CDDDEEND = 175;
	public static final int _CDTABLEBEGIN = 176;
	public static final int _CDTABLECELL = 177;
	public static final int _CDTABLEEND = 178;
	public static final int _CDSTYLENAME = 188;
	public static final int _FILEOBJECT_MACEXT = 192;
	public static final int _FILEOBJECT_HPFSEXT = 193;
	public static final int _CDOLEBEGIN = 218;
	public static final int _CDOLEEND = 219;
	public static final int _CDHOTSPOTBEGIN = 230;
	public static final int _CDHOTSPOTEND = 231;
	public static final int _CDBUTTON = 237;
	public static final int _CDBAR = 308;
	public static final int _CDQUERYHEADER = 314;
	public static final int _CDQUERYTEXTTERM = 315;
	public static final int _CDACTIONHEADER = 316;
	public static final int _CDACTIONMODIFYFIELD = 317;
	public static final int _ODS_ASSISTSTRUCT = 318;
	public static final int _VIEWMAP_HEADER_RECORD = 319;
	public static final int _VIEWMAP_RECT_RECORD = 320;
	public static final int _VIEWMAP_BITMAP_RECORD = 321;
	public static final int _VIEWMAP_REGION_RECORD = 322;
	public static final int _VIEWMAP_POLYGON_RECORD_BYTE = 323;
	public static final int _VIEWMAP_POLYLINE_RECORD_BYTE = 324;
	public static final int _VIEWMAP_ACTION_RECORD = 325;
	public static final int _ODS_ASSISTRUNINFO = 326;
	public static final int _CDACTIONREPLY = 327;
	public static final int _CDACTIONFORMULA = 332;
	public static final int _CDACTIONLOTUSSCRIPT = 333;
	public static final int _CDQUERYBYFIELD = 334;
	public static final int _CDACTIONSENDMAIL = 335;
	public static final int _CDACTIONDBCOPY = 336;
	public static final int _CDACTIONDELETE = 337;
	public static final int _CDACTIONBYFORM = 338;
	public static final int _ODS_ASSISTFIELDSTRUCT = 339;
	public static final int _CDACTION = 340;
	public static final int _CDACTIONREADMARKS = 341;
	public static final int _CDEXTFIELD = 342;
	public static final int _CDLAYOUT = 343;
	public static final int _CDLAYOUTTEXT = 344;
	public static final int _CDLAYOUTEND = 345;
	public static final int _CDLAYOUTFIELD = 346;
	public static final int _VIEWMAP_DATASET_RECORD = 347;
	public static final int _CDDOCAUTOLAUNCH = 350;
	public static final int _CDPABHIDE = 358;
	public static final int _CDPABFORMULAREF = 359;
	public static final int _CDACTIONBAR = 360;
	public static final int _CDACTIONFOLDER = 361;
	public static final int _CDACTIONNEWSLETTER = 362;
	public static final int _CDACTIONRUNAGENT = 363;
	public static final int _CDACTIONSENDDOCUMENT = 364;
	public static final int _CDQUERYFORMULA = 365;
	public static final int _CDQUERYBYFORM = 373;
	public static final int _ODS_ASSISTRUNOBJECTHEADER = 374;
	public static final int _ODS_ASSISTRUNOBJECTENTRY=375;
	public static final int _CDOLEOBJ_INFO=379;
	public static final int _CDLAYOUTGRAPHIC=407;
	public static final int _CDQUERYBYFOLDER=413;
	public static final int _CDQUERYUSESFORM=423;
	public static final int _VIEW_COLUMN_FORMAT2=428;
	public static final int _VIEWMAP_TEXT_RECORD=464;
	public static final int _CDLAYOUTBUTTON=466;
	public static final int _CDQUERYTOPIC=471;
	public static final int _CDLSOBJECT=482;
	public static final int _CDHTMLHEADER=492;
	public static final int _CDHTMLSEGMENT=493;
	public static final int _SCHED_LIST=502;
	public static final int  _SCHED_LIST_OBJ = _SCHED_LIST;
	public static final int _SCHED_ENTRY=503;
	public static final int _SCHEDULE=504;
	public static final int _CDTEXTEFFECT=508;
	public static final int _VIEW_CALENDAR_FORMAT=513;
	public static final int _CDSTORAGELINK=515;
	public static final int _ACTIVEOBJECT=516;
	public static final int _ACTIVEOBJECTPARAM=517;
	public static final int _ACTIVEOBJECTSTORAGELINK=518;
	public static final int _CDTRANSPARENTTABLE=541;
	/* modified viewmap records, changed CD record from byte to word */
	public static final int _VIEWMAP_POLYGON_RECORD=551;
	public static final int _VIEWMAP_POLYLINE_RECORD=552;
	public static final int _SCHED_DETAIL_LIST=553;
	public static final int _CDALTERNATEBEGIN=554;
	public static final int _CDALTERNATEEND=555;
	public static final int _CDOLERTMARKER=556;
	public static final int _HSOLERICHTEXT=557;
	public static final int _CDANCHOR=559;
	public static final int _CDHRULE=560;
	public static final int _CDALTTEXT=561;
	public static final int _CDACTIONJAVAAGENT=562;
	public static final int _CDHTMLBEGIN=564;
	public static final int _CDHTMLEND=565;
	public static final int _CDHTMLFORMULA=566;
	public static final int _CDBEGINRECORD=577;
	public static final int _CDENDRECORD=578;
	public static final int _CDVERTICALALIGN=579;
	public static final int _CDFLOAT=580;
	public static final int _CDTIMERINFO=581;
	public static final int _CDTABLEROWHEIGHT=582;
	public static final int _CDTABLELABEL=583;
	public static final int _CDTRANSITION=610;
	public static final int _CDPLACEHOLDER=611;
	public static final int _CDEMBEDDEDVIEW=615;
	public static final int _CDEMBEDDEDOUTLINE=620;
	public static final int _CDREGIONBEGIN=621;
	public static final int _CDREGIONEND=622;
	public static final int _CDCELLBACKGROUNDDATA=623;
	public static final int _FRAMESETLENGTH=625;
	public static final int _CDFRAMESETHEADER=626;
	public static final int _CDFRAMESET=627;
	public static final int _CDFRAME=628;
	public static final int _CDTARGET=629;
	public static final int _CDRESOURCE=631;
	public static final int _CDMAPELEMENT=632;
	public static final int _CDAREAELEMENT=633;
	public static final int _CDRECT=634;
	public static final int _CDPOINT=635;
	public static final int _CDEMBEDDEDCTL=636;
	public static final int _CDEVENT=637;
	public static final int _MIME_PART=639;
	public static final int _CDPRETABLEBEGIN=640;
	public static final int _CDCOLOR=645;
	public static final int _CDBORDERINFO=646;
	public static final int _CDEXT2FIELD=672;
	public static final int _CDEMBEDDEDSCHEDCTL=674;
	public static final int _RFC822ITEMDESC=675;
	public static final int _COLOR_VALUE=690;
	public static final int _CDBLOBPART=695;
	public static final int _CDIMAGEHEADER=705;
	public static final int _CDIMAGESEGMENT=706;
	public static final int _VIEW_TABLE_FORMAT3=707;
	public static final int _CDIDNAME=708;
	public static final int _CDACTIONBAREXT=719;
	public static final int _CDLINKCOLORS=722;
	public static final int _CDCAPTION=728;
	public static final int _CDFIELDHINT=742;
	public static final int _CDLSOBJECT_R6=744;
	public static final int _CDINLINE=756;
	public static final int _CDTEXTPROPERTIESTABLE=765;
	public static final int _CDSPANRECORD=766;
	public static final int _CDDECSFIELD=767;
	public static final int _CDLAYER=808;
	public static final int _CDPOSITIONING=809;
	public static final int _CDBOXSIZE=810;
	public static final int _CDEMBEDDEDEDITCTL=816;
	public static final int _CDEMBEDDEDSCHEDCTLEXTRA=818;
	public static final int _LOG_SEARCHR6_REQ=821;
	public static final int _CDBACKGROUNDPROPERTIES=822;
	public static final int _CDTEXTPROPERTY=833;
	public static final int _CDDATAFLAGS=834;
	public static final int _CDFILEHEADER=835;
	public static final int _CDFILESEGMENT=836;
	public static final int _CDEVENTENTRY=847;
	public static final int _CDACTIONEXT=848;
	public static final int _CDEMBEDDEDCALCTL=849;
	public static final int _CDTABLEDATAEXTENSION=857;
	public static final int _CDLARGEPARAGRAPH=909;
	public static final int _CDIGNORE=912;
	public static final int _VIEW_COLUMN_FORMAT5=914;
	public static final int _CDEMBEDEXTRAINFO=934;
	public static final int _CDEMBEDDEDCONTACTLIST=935;
	public static final int _NOTE_SEAL2_HDR=1031;
	public static final int _NOTE_SEAL2=1032;
	public static final int _NOTE_RECORD_DESC=1033;
	
	
	/*	These must be OR-ed into the ObjectType below in order to get the
	desired behavior.  Note that OBJECT_COLLECTION implicitly has
	both of these bits implied, because that was the desired behavior
	before these bits were invented. */

	/** do not copy object when updating to new note or database */
	public static final int OBJECT_NO_COPY = 0x8000;
	/** keep object around even if hNote doesn't have it when NoteUpdating */
	public static final int OBJECT_PRESERVE	= 0x4000;
	/** Public access object being allocated. */
	public static final int OBJECT_PUBLIC = 0x2000;

	/*	Object Types, a sub-category of TYPE_OBJECT */

	/** File Attachment */
	public static final short OBJECT_FILE = 0;
	/** IDTable of "done" docs attached to filter */
	public static final short OBJECT_FILTER_LEFTTODO = 3;
	/** Assistant run data object */
	public static final short OBJECT_ASSIST_RUNDATA = 8;
	/** Used as input to NSFDbGetObjectSize */
	public static final short OBJECT_UNKNOWN = (short) (0xffff & 0xffff);

	/** file object has object digest appended */
	public static final short FILEFLAG_SIGN = 0x0001;
	/** file is represented by an editor run in the document */
	public static final short FILEFLAG_INDOC = 0x0002;
	/** file object has mime data appended */
	public static final short FILEFLAG_MIME	= 0x0004;
	/** file is a folder automaticly compressed by Notes */
	public static final short FILEFLAG_AUTOCOMPRESSED = 0x0080;

	public short NSFDbRename(Memory dbNameOld, Memory dbNameNew);
	
	public void OSGetExecutableDirectory(Memory retPathName);

	public void OSGetDataDirectory(Memory retPathName);

	public short OSGetSystemTempDirectory(Memory retPathName, int bufferLength);

	public void OSPathAddTrailingPathSep(Memory retPathName);
	
	public static int MAXDWORD = 0xffffffff;
	
}
