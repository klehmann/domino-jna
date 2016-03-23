package com.mindoo.domino.jna.internal;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import com.mindoo.domino.jna.structs.NotesCollectionPosition;
import com.mindoo.domino.jna.structs.NotesCollectionStats;
import com.mindoo.domino.jna.structs.NotesFTIndexStats;
import com.mindoo.domino.jna.structs.NotesItem;
import com.mindoo.domino.jna.structs.NotesItemValueTable;
import com.mindoo.domino.jna.structs.NotesNumberPair;
import com.mindoo.domino.jna.structs.NotesRange;
import com.mindoo.domino.jna.structs.NotesTime;
import com.mindoo.domino.jna.structs.NotesTimeDate;
import com.mindoo.domino.jna.structs.NotesTimeDatePair;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
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

	public short b32_OSPathNetConstruct(
			Memory PortName,
			Memory ServerName,
			Memory FileName,
			Memory retPathName);
	public short b64_OSPathNetConstruct(
			Memory PortName,
			Memory ServerName,
			Memory FileName,
			Memory retPathName);
	
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
	
	short b32_NSFDbGetModifiedNoteTable(int hDB, short NoteClassMask, NotesTimeDate Since, NotesTimeDate retUntil, IntByReference rethTable);
	short b64_NSFDbGetModifiedNoteTable(long hDB, short NoteClassMask, NotesTimeDate Since, NotesTimeDate retUntil, LongByReference rethTable);
	
	short DNCanonicalize(int Flags, Memory TemplateName, Memory InName, Memory OutName, short OutSize, ShortByReference OutLength);
	short DNAbbreviate(int Flags, Memory TemplateName, Memory InName, Memory OutName, short OutSize, ShortByReference OutLength);	
	

	short b32_NIFCloseCollection(int hCollection);
	short b64_NIFCloseCollection(long hCollection);

	/**
	 * Original signature : <code>void* OSLockObject(DHANDLE)</code><br>
	 * <i>native declaration : line 2701</i>
	 */
	Pointer b32_OSLockObject(int Handle);
	Pointer b64_OSLockObject(long Handle);

	/**
	 * Original signature : <code>BOOL OSUnlockObject(DHANDLE)</code><br>
	 * <i>native declaration : line 2706</i>
	 */
	boolean b32_OSUnlockObject(int Handle);
	boolean b64_OSUnlockObject(long Handle);

	public short b32_OSMemFree(int handle);
	public short b64_OSMemFree(long handle);

	public short b32_NIFLocateNote (int hCollection, NotesCollectionPosition IndexPos, int NoteID);
	public short b64_NIFLocateNote (long hCollection, NotesCollectionPosition IndexPos, int NoteID);

	
	/*	NIFOpenCollection "open" flags */

	public static short OPEN_REBUILD_INDEX = 0x0001;	/* Throw away existing index and */
											/* rebuild it from scratch */
	public static short OPEN_NOUPDATE = 0x0002;	/* Do not update index or unread */
											/* list as part of open (usually */
											/* set by server when it does it */
											/* incrementally instead). */
	public static short OPEN_DO_NOT_CREATE = 0x0004;	/* If collection object has not yet */
											/* been created, do NOT create it */
											/* automatically, but instead return */
											/* a special internal error called */
											/* ERR_COLLECTION_NOT_CREATED */
	public static short OPEN_SHARED_VIEW_NOTE = 0x0010;	/* Tells NIF to "own" the view note */
											/* (which gets read while opening the */
											/* collection) in memory, rather than */
											/* the caller "owning" the view note */
											/* by default.  If this flag is specified */
											/* on subsequent opens, and NIF currently */
											/* owns a copy of the view note, it */
											/* will just pass back the view note */
											/* handle  rather than re-reading it */
											/* from disk/network.  If specified, */
											/* the the caller does NOT have to */
											/* close the handle.  If not specified, */
											/* the caller gets a separate copy, */
											/* and has to NSFNoteClose the */
											/* handle when its done with it. */
	public static short OPEN_REOPEN_COLLECTION = 0x0020;	/* Force re-open of collection and */
											/* thus, re-read of view note. */
											/* Also implicitly prevents sharing */
											/* of collection handle, and thus */
											/* prevents any sharing of associated */
											/* structures such as unread lists, etc */

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

/* 	Note structure member IDs for NSFNoteGet&SetInfo. */

	/** IDs for NSFNoteGet&SetInfo */
	public static short _NOTE_DB = 0;		
	/** (When adding new values, see the */
	public static short _NOTE_ID = 1;
	/**  table in NTINFO.C */
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


	public static final int SIGNAL_DEFN_ITEM_MODIFIED = 0x0001;
	public static final int SIGNAL_VIEW_ITEM_MODIFIED = 0x0002;
	public static final int SIGNAL_INDEX_MODIFIED = 0x0004;
	public static final int SIGNAL_UNREADLIST_MODIFIED = 0x0008;
	public static final int SIGNAL_DATABASE_MODIFIED = 0x0010;
	public static final int SIGNAL_MORE_TO_DO = 0x0020;
	public static final int SIGNAL_VIEW_TIME_RELATIVE = 0x0040;
	public static final int SIGNAL_NOT_SUPPORTED = 0x0080;
	public static final int SIGNAL_ANY_CONFLICT	= (SIGNAL_DEFN_ITEM_MODIFIED | SIGNAL_VIEW_ITEM_MODIFIED | SIGNAL_INDEX_MODIFIED | SIGNAL_UNREADLIST_MODIFIED | SIGNAL_DATABASE_MODIFIED);
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
	
	public short b32_FTOpenSearch(IntByReference rethSearch);
	public short b64_FTOpenSearch(LongByReference rethSearch);
	
	public short b32_FTCloseSearch(int hSearch);
	public short b64_FTCloseSearch(long hSearch);
	
	public short b32_FTSearch(
			int hDB,
			IntByReference phSearch,
			int hColl,
			Memory Query,
			int Options,
			short  Limit,
			int hIDTable,
			IntByReference retNumDocs,
			Memory Reserved,
			IntByReference rethResults);
	public short b64_FTSearch(
			long hDB,
			LongByReference phSearch,
			long hColl,
			Memory Query,
			int Options,
			short  Limit,
			long hIDTable,
			IntByReference retNumDocs,
			Memory Reserved,
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
	
	public short b32_IDTableCopy (int hTable, IntByReference rethTable);
	public short b64_IDTableCopy (long hTable, LongByReference rethTable);
	
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

	public short ConvertTIMEDATEToText(
			ByteBuffer IntlFormat,
			ByteBuffer TextFormat,
			NotesTimeDate InputTime,
			Memory retTextBuffer,
			short TextBufferLength,
			ShortByReference retTextLength);
	
	public short ConvertTextToTIMEDATE(
			ByteBuffer IntlFormat,
			ByteBuffer TextFormat,
			Memory Text,
			short MaxLength,
			NotesTimeDate retTIMEDATE);

	public boolean TimeGMToLocalZone (NotesTime timePtr);
	public boolean TimeGMToLocal (NotesTime timePtr);
	public boolean TimeLocalToGM(NotesTime timePtr);
	public boolean TimeLocalToGM(Memory timePtr);
	
	public short TIMEDATE_MINIMUM = 0;
	public short TIMEDATE_MAXIMUM = 1;
	public short TIMEDATE_WILDCARD = 2;
	public void TimeConstant(short TimeConstantType, NotesTimeDate tdptr);

	public short ListGetText (ByteBuffer pList,
			boolean fPrefixDataType,
			short EntryNumber,
			Memory retTextPointer,
			ShortByReference retTextLength);

	public short ListGetText (Pointer pList,
			boolean fPrefixDataType,
			short EntryNumber,
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
			int  ViewNoteID,
			int  Flags,
			IntByReference hTable);

	public short b64_NSFFolderGetIDTable(
			long  hViewDB,
			long hDataDB,
			int  ViewNoteID,
			int  Flags,
			LongByReference hTable);

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
	
}