package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.constants.OpenNote;
import com.mindoo.domino.jna.internal.structs.IntlFormatStruct;
import com.mindoo.domino.jna.internal.structs.LinuxNotesNamesListHeader64Struct;
import com.mindoo.domino.jna.internal.structs.MacNotesNamesListHeader64Struct;
import com.mindoo.domino.jna.internal.structs.NoteIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesCollectionPositionStruct;
import com.mindoo.domino.jna.internal.structs.NotesFileObjectStruct;
import com.mindoo.domino.jna.internal.structs.NotesItemValueTableStruct;
import com.mindoo.domino.jna.internal.structs.NotesMIMEPartStruct;
import com.mindoo.domino.jna.internal.structs.NotesNamesListHeader32Struct;
import com.mindoo.domino.jna.internal.structs.NotesNumberPairStruct;
import com.mindoo.domino.jna.internal.structs.NotesObjectDescriptorStruct;
import com.mindoo.domino.jna.internal.structs.NotesOriginatorIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesRangeStruct;
import com.mindoo.domino.jna.internal.structs.NotesReplicationHistorySummaryStruct;
import com.mindoo.domino.jna.internal.structs.NotesSchedEntryExtStruct;
import com.mindoo.domino.jna.internal.structs.NotesSchedEntryStruct;
import com.mindoo.domino.jna.internal.structs.NotesScheduleListStruct;
import com.mindoo.domino.jna.internal.structs.NotesScheduleStruct;
import com.mindoo.domino.jna.internal.structs.NotesSearchMatch32Struct;
import com.mindoo.domino.jna.internal.structs.NotesSearchMatch64Struct;
import com.mindoo.domino.jna.internal.structs.NotesTableItemStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDatePairStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.internal.structs.WinNotesNamesListHeader32Struct;
import com.mindoo.domino.jna.internal.structs.WinNotesNamesListHeader64Struct;
import com.mindoo.domino.jna.internal.structs.collation.NotesCollateDescriptorStruct;
import com.mindoo.domino.jna.internal.structs.collation.NotesCollationStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDEmbeddedCtlStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDExt2FieldStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDExtFieldStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDFieldStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDIdNameStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDPabHideStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCDResourceStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCdHotspotBeginStruct;
import com.mindoo.domino.jna.internal.structs.html.HtmlApi_UrlArgStruct;
import com.mindoo.domino.jna.internal.structs.html.HtmlApi_UrlTargetComponentStruct;
import com.mindoo.domino.jna.internal.structs.html.StringListStruct;
import com.mindoo.domino.jna.internal.structs.html.ValueUnion;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewColumnFormat2Struct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewColumnFormat3Struct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewColumnFormat4Struct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewColumnFormat5Struct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewColumnFormatStruct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewTableFormat2Struct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewTableFormat4Struct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewTableFormat5Struct;
import com.mindoo.domino.jna.internal.structs.viewformat.NotesViewTableFormatStruct;
import com.sun.jna.Pointer;

/**
 * Extract of Notes C API constants, should only be used internally by the API.
 * The plan is to wrap/provide any relevant constant as enum, like {@link OpenNote}.
 * 
 * @author Karsten Lehmann
 */
public interface NotesConstants {
	//computation of data type sizes for the current platform
	public final int timeDateSize = NotesTimeDateStruct.newInstance().size();
	public final int rangeSize = NotesRangeStruct.newInstance().size();
	public final int timeSize = NotesTimeStruct.newInstance().size();
	public final int numberPairSize = NotesNumberPairStruct.newInstance().size();
	public final int timeDatePairSize = NotesTimeDatePairStruct.newInstance().size();
	public final int collectionPositionSize = NotesCollectionPositionStruct.newInstance().size();
	public final int itemValueTableSize = NotesItemValueTableStruct.newInstance().size();
	public final int tableItemSize = NotesTableItemStruct.newInstance().size();
	public final int oidSize = NotesOriginatorIdStruct.newInstance().size();
	public final int winNamesListHeaderSize64 = WinNotesNamesListHeader64Struct.newInstance().size();
	public final int winNamesListHeaderSize32 = WinNotesNamesListHeader32Struct.newInstance().size();
	public final int namesListHeaderSize32 = NotesNamesListHeader32Struct.newInstance().size();
	public final int linuxNamesListHeaderSize64 = LinuxNotesNamesListHeader64Struct.newInstance().size();
	public final int macNamesListHeaderSize64 = MacNotesNamesListHeader64Struct.newInstance().size();
	public final int objectDescriptorSize = NotesObjectDescriptorStruct.newInstance().size();
	public final int fileObjectSize = NotesFileObjectStruct.newInstance().size();
	public final int schedListSize = NotesScheduleListStruct.newInstance().size();
	public final int schedEntrySize = NotesSchedEntryStruct.newInstance().size();
	public final int schedEntryExtSize = NotesSchedEntryExtStruct.newInstance().size();
	public final int scheduleSize = NotesScheduleStruct.newInstance().size();
	public final int notesUniversalNoteIdSize = NotesUniversalNoteIdStruct.newInstance().size();
	public final int noteIdSize = NoteIdStruct.newInstance().size();
	public final int slistStructSize = StringListStruct.newInstance().size();
	public final int valueUnionSize = Math.max(
			ValueUnion.newInstance(0).size(),
			Math.max(ValueUnion.newInstance(NotesUniversalNoteIdStruct.newInstance()).size(),
					Math.max(ValueUnion.newInstance(NoteIdStruct.newInstance()).size(), ValueUnion.newInstance(StringListStruct.newInstance()).size())));
	
	public final int htmlApiUrlTargetComponentSize = Math.max(
			HtmlApi_UrlTargetComponentStruct.newInstance(0, 0, ValueUnion.newInstance(new Pointer(0))).size(),
			Math.max(
					HtmlApi_UrlTargetComponentStruct.newInstance(0, 0, ValueUnion.newInstance(NotesUniversalNoteIdStruct.newInstance())).size(),
					HtmlApi_UrlTargetComponentStruct.newInstance(0, 0, ValueUnion.newInstance(StringListStruct.newInstance())).size()
					)
			);
	public final int htmlApiUrlArgSize = Math.max(
			HtmlApi_UrlArgStruct.newInstance(0, 0, ValueUnion.newInstance(new Pointer(0))).size(),
			Math.max(
					HtmlApi_UrlArgStruct.newInstance(0, 0, ValueUnion.newInstance(NotesUniversalNoteIdStruct.newInstance())).size(),
					HtmlApi_UrlArgStruct.newInstance(0, 0, ValueUnion.newInstance(StringListStruct.newInstance())).size()
					)
			);
	public final int htmlApiUrlComponentSize = Math.max(htmlApiUrlTargetComponentSize, htmlApiUrlArgSize);
	public final int notesCollationSize = NotesCollationStruct.newInstance().size();
	public final int notesCollateDescriptorSize = NotesCollateDescriptorStruct.newInstance().size();
	public final int notesViewTableFormatSize = NotesViewTableFormatStruct.newInstance().size();
	public final int notesViewTableFormat2Size = NotesViewTableFormat2Struct.newInstance().size();
	public final int notesViewTableFormat4Size = NotesViewTableFormat4Struct.newInstance().size();
	public final int notesViewTableFormat5Size = NotesViewTableFormat5Struct.newInstance().size();
	public final int notesViewColumnFormatSize = NotesViewColumnFormatStruct.newInstance().size();
	public final int notesViewColumnFormat2Size = NotesViewColumnFormat2Struct.newInstance().size();
	public final int notesViewColumnFormat3Size = NotesViewColumnFormat3Struct.newInstance().size();
	public final int notesViewColumnFormat4Size = NotesViewColumnFormat4Struct.newInstance().size();
	public final int notesViewColumnFormat5Size = NotesViewColumnFormat5Struct.newInstance().size();
	public final int notesSearchMatch32Size = NotesSearchMatch32Struct.newInstance().size();
	public final int notesSearchMatch64Size = NotesSearchMatch64Struct.newInstance().size();
	public final int mimePartSize = NotesMIMEPartStruct.newInstance().size();
	public final int intlFormatSize = IntlFormatStruct.newInstance().size();
	public final int notesCDFieldStructSize = NotesCDFieldStruct.newInstance().size();
	public final int notesCDExtFieldStructSize = NotesCDExtFieldStruct.newInstance().size();
	public final int notesCDExt2FieldStructSize = NotesCDExt2FieldStruct.newInstance().size();
	public final int notesCDEmbeddedCtlStructSize = NotesCDEmbeddedCtlStruct.newInstance().size();
	public final int notesCDPabhideStructSize = NotesCDPabHideStruct.newInstance().size();
	public final int notesCDHotspotBeginStructSize = NotesCdHotspotBeginStruct.newInstance().size();
	public final int notesCDIdNameStructSize = NotesCDIdNameStruct.newInstance().size();
	public final int notesCDResourceStructSize = NotesCDResourceStruct.newInstance().size();
	public final int notesReplicationHistorySummaryStructSize = NotesReplicationHistorySummaryStruct.newInstance().size();

	public static final short MAXALPHATIMEDATE = 80;

	public static final short ERR_MASK = 0x3fff;
	/** error came from remote machine */
	public static final short STS_REMOTE = 0x4000;
	
	/*	Defines for Authentication flags */

	/** Set if names list has been authenticated via Notes */
	public static final short NAMES_LIST_AUTHENTICATED = 0x0001;	
	/**	Set if names list has been authenticated using external password -- Triggers "maximum password access allowed" feature */
	public static final short NAMES_LIST_PASSWORD_AUTHENTICATED = 0x0002;
	/**	Set if user requested full admin access and it was granted */
	public static final short NAMES_LIST_FULL_ADMIN_ACCESS = 0x0004;
	
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
	/** see {@link #SEARCH_NOTIFYDELETIONS} */
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
	/** If specified, the open will check to see if this note had already been read and
	 * saved in memory.  If not, and the database is server	based, we will also check
	 * the on-disk cache.  If the note is not found, it is cached in memory and at some
	 * time in the future commited to a local on disk cache.
	 * The notes are guaranteed to be as up to date as the last time NSFValidateNoteCache was called.#
	 * Minimally, this should be called	the 1st time a database is opened prior to specifying
	 * this flag. */
	public static final int OPEN_CACHE = 0x00100000;
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
	/** do not update seq/sequence time on update */
	public static final short UPDATE_REPLICA = 0x0008;
	/** do NOT maintain revision history */
	public static final short UPDATE_NOREVISION = 0x0100;
	/** update body but leave no trace of note in file if deleted */
	public static final short UPDATE_NOSTUB = 0x0200;
	/** Compute incremental note info */
	public static final short UPDATE_INCREMENTAL = 0x4000;
	/** avoid queuing the update to the real time replicator and the streaming cluster replicator */
	public static final short UPDATE_RTR = 0x0800;
	
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
	
	/** Ghost entries do not appear in any views or searches */
	public static final short NOTE_FLAG_GHOST = 0x200;
	
	/** Note is (opened) in canonical form */
	public static final short NOTE_FLAG_CANONICAL = 0x4000;

/* 	Note structure member IDs for NSFNoteGet and SetInfo. */

	/** IDs for NSFNoteGet and SetInfo */
	public static short _NOTE_DB = 0;		
	/** (When adding new values, see the table in NTINFO.C */
	public static short _NOTE_ID = 1;
	/** Get/set the Originator ID (OID). */
	public static short _NOTE_OID = 2;
	/** Get/set the NOTE_CLASS (WORD). */
	public static short _NOTE_CLASS	= 3;
	/** Get/set the Modified in this file time/date (TIMEDATE : GMT normalized). */
	public static short _NOTE_MODIFIED = 4;
	/** For pre-V3 compatibility. Should use $Readers item */
	public static short _NOTE_PRIVILEGES = 5;
	/** Get/set the note flags (WORD). See NOTE_FLAG_xxx. */
	public static short _NOTE_FLAGS = 7;
	/** Get/set the Accessed in this file date (TIMEDATE). */
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
	/** display only views and folder; all notes &amp; web */
	public static final String DFLAGPAT_VIEWS_AND_FOLDERS_DESIGN = "-G40^";
	/** display only folders; version filtering, ignore hidden notes */
	public static final String DFLAGPAT_FOLDER_DESIGN = "(+-04*F";

	/** display only views, ignore hidden from notes */
	public static final String DFLAGPAT_VIEW_DESIGN = "-FG40^";

	/** display things that are runnable; version filtering */
	public static final String DFLAGPAT_TOOLSRUNMACRO = "-QXMBESIst5nmz{";

	/** display things that show up in agents list. No version filtering (for design) */
	public static final String DFLAGPAT_AGENTSLIST = "-QXstmz{";

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
	
    /** The view contains documents with readers fields */
	public static final int SIGNAL_VIEW_HASPRIVS = 0x0100;


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

	public static final int MAXSPRINTF = 256;
	public static final int MAXPATH = 256;
	public static final short MAXUSERNAME	= 256;			/* Maximum user name */
	
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

	public static String DESIGN_FLAGS = "$Flags";

	public static String DESIGN_FLAG_FOLDER_VIEW = "F";	/*	VIEW: This is a V4 folder view. */
	
	//saves the info in the idtable header in the dest
	public static final byte IDREPLACE_SAVEDEST = 0x01;
	
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
	
	public short TIMEDATE_MINIMUM = 0;
	public short TIMEDATE_MAXIMUM = 1;
	public short TIMEDATE_WILDCARD = 2;
	
	/*	Define flags for NSFFolderGetIDTable */
	public int DB_GETIDTABLE_VALIDATE = 0x00000001;	/*	If set, return only "validated" noteIDs */
	
	public int SIGN_NOTES_IF_MIME_PRESENT = 0x00000001;
	
	/* 	Possible validation phases for NSFNoteComputeWithForm()  */
	public short CWF_DV_FORMULA = 1;
	public short CWF_IT_FORMULA	= 2;
	public short CWF_IV_FORMULA = 3;
	public short CWF_COMPUTED_FORMULA = 4;
	public short CWF_DATATYPE_CONVERSION = 5;
	public short CWF_COMPUTED_FORMULA_LOAD = CWF_COMPUTED_FORMULA;
	public short CWF_COMPUTED_FORMULA_SAVE = 6;

	
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

	/*	Define NSF DB Classes - These all begin with 0xf000 for no good
	reason other than to ENSURE that callers of NSFDbCreate call the
	routine with valid parameters, since in earlier versions of NSF
	the argument to the call was typically 0. */

	/* The type of the database is determined by the filename extension.
	 * The extensions and their database classes are .NSX (NSFTESTFILE),
	 * .NSF (NOTEFILE), .DSK (DESKTOP), .NCF (NOTECLIPBOARD), .NTF (TEMPLATEFILE),
	 * .NSG (GIANTNOTEFILE), .NSH (HUGENOTEFILE), NTD (ONEDOCFILE),
	 * NS2 (V2NOTEFILE), NTM (ENCAPSMAILFILE). */
public short DBCLASS_BY_EXTENSION = 0;	

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
/** Database was created specifically for Domino and Notes Release Notes/Domino 10. */
public short DBCLASS_V10NOTEFILE = (short) (0xff14 & 0xffff);


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

	/**
	 * Item will not be written to disk
	 */
	public static final short ITEM_NOUPDATE = 0x0080;
	
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

	/*	Define NSF Special Note ID Indices.  The first 16 of these are reserved
	for "default notes" in each of the 16 note classes.  In order to access
	these, use SPECIAL_ID_NOTE+NOTE_CLASS_XXX.  This is generally used
	when calling NSFDbGetSpecialNoteID. NOTE: NSFNoteOpen, NSFDbReadObject
	and NSFDbWriteObject support reading special notes or objects directly
	(without calling NSFDbGetSpecialNoteID).  They use a DIFFERENT flag
	with a similar name: NOTE_ID_SPECIAL (see nsfnote.h).  Remember this
	rule:

	SPECIAL_ID_NOTE is a 16 bit mask and is used as a NoteClass argument.
	NOTE_ID_SPECIAL is a 32 bit mask and is used as a NoteID or RRV argument.
*/

	public short SPECIAL_ID_NOTE	= (short) (0x8000 & 0xffff); /* use in combination w/NOTE_CLASS when calling NSFDbGetSpecialNoteID */

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

	public int CWF_CONTINUE_ON_ERROR = 0x0001;		/*	Ignore compute errors */

	/*	EncryptFlags used in NSFNoteCopyAndEncrypt */

	public short ENCRYPT_WITH_USER_PUBLIC_KEY = 0x0001;
	public short ENCRYPT_SMIME_IF_MIME_PRESENT = 0x0002;
	public short ENCRYPT_SMIME_NO_SENDER = 0x0004;
	public short ENCRYPT_SMIME_TRUST_ALL_CERTS = 0x0008;
	
	/*	DecryptFlags used in NSFNoteDecrypt */

	public short DECRYPT_ATTACHMENTS_IN_PLACE = 0x0001;
	
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

	/* Definitions for stdout redirection types. This specifies where
		output from the LotusScript "print" statement will go */

	public static short AGENT_REDIR_NONE = 0;		/* goes to the bit bucket */
	public static short AGENT_REDIR_LOG	= 1;		/* goes to the Notes log (default) */
	public static short AGENT_REDIR_MEMORY = 2;		/* goes to a memory buffer, cleared each AgentRun */
	public static short AGENT_REDIR_MEMAPPEND = 3;		/* goes to buffer, append mode for each agent */
	
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

	/*	Define memory allocator hints, which re-use the top 2 bits of
	the BLK_ codes so that we didn't have to add a new argument to
	OSMemAlloc() */

	/** Object may be used by multiple processes */
	public short MEM_SHARE = (short) (0x8000 & 0xffff);
	/** Object may be OSMemRealloc'ed LARGER */
	public short MEM_GROWABLE = 0x4000;

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
	
	public static short MAXENVVALUE = 256;
	
	public static int MAXDWORD = 0xffffffff;
	public static short MAXWORD = (short) (0xffff & 0xffff);
	
	/** Transactions is Sub-Commited if a Sub Transaction */
	public static int NSF_TRANSACTION_BEGIN_SUB_COMMIT = 0x00000001;
	
	/** When starting a txn (not a sub tran) get an intent shared lock on the db */
	public static int NSF_TRANSACTION_BEGIN_LOCK_DB = 0x00000002;
	
	/** Don't automatically abort if Commit Processing Fails */
	public static final int TRANCOMMIT_SKIP_AUTO_ABORT = 1;
	
	/** Enable full text indexing */
	public static final int DBOPTBIT_FT_INDEX = 0;
	/** TRUE if database is being used as an object store - for garbage collection */
	public static final int DBOPTBIT_IS_OBJSTORE = 1;
	/** TRUE if database has notes which refer to an object store - for garbage collection*/
	public static final int DBOPTBIT_USES_OBJSTORE = 2;
	/** TRUE if NoteUpdate of notes in this db should never use an object store. */
	public static final int DBOPTBIT_OBJSTORE_NEVER = 3;
	/** TRUE if database is a library */
	public static final int DBOPTBIT_IS_LIBRARY = 4;
	/** TRUE if uniform access control across all replicas */
	public static final int DBOPTBIT_UNIFORM_ACCESS = 5;
	/** TRUE if NoteUpdate of notes in this db should always try to use an object store. */
	public static final int DBOPTBIT_OBJSTORE_ALWAYS = 6;
	/** TRUE if garbage collection is never to be done on this object store */
	public static final int DBOPTBIT_COLLECT_NEVER = 7;
	/** TRUE if this is a template and is considered an advanced one (for experts only.) */
	public static final int DBOPTBIT_ADV_TEMPLATE = 8;
	/** TRUE if db has no background agent */
	public static final int DBOPTBIT_NO_BGAGENT = 9;
	/** TRUE is db is out-of-service, no new opens allowed, unless DBOPEN_IGNORE_OUTOFSERVICE is specified */
	public static final int DBOPTBIT_OUT_OF_SERVICE = 10;
	/** TRUE if db is personal journal */
	public static final int DBOPTBIT_IS_PERSONALJOURNAL = 11;
	/** TRUE if db is marked for delete. no new opens allowed, cldbdir will delete the database when ref count = = 0;*/
	public static final int DBOPTBIT_MARKED_FOR_DELETE = 12;
	/** TRUE if db stores calendar events */
	public static final int DBOPTBIT_HAS_CALENDAR = 13;
	/** TRUE if db is a catalog index */
	public static final int DBOPTBIT_IS_CATALOG_INDEX = 14;
	/** TRUE if db is an address book */
	public static final int DBOPTBIT_IS_ADDRESS_BOOK = 15;
	/** TRUE if db is a "multi-db-search" repository */
	public static final int DBOPTBIT_IS_SEARCH_SCOPE = 16;
	/** TRUE if db's user activity log is confidential, only viewable by designer and manager */
	public static final int DBOPTBIT_IS_UA_CONFIDENTIAL = 17;
	/** TRUE if item names are to be treated as if the ITEM_RARELY_USED_NAME flag is set. */
	public static final int DBOPTBIT_RARELY_USED_NAMES = 18;
	/** TRUE if db is a "multi-db-site" repository */
	public static final int DBOPTBIT_IS_SITEDB = 19;

	/** TRUE if docs in folders in this db have folder references */
	public static final int DBOPTBIT_FOLDER_REFERENCES = 20;

	/** TRUE if the database is a proxy for non-NSF data */
	public static final int DBOPTBIT_IS_PROXY = 21;
	/** TRUE for NNTP server add-in dbs */
	public static final int DBOPTBIT_IS_NNTP_SERVER_DB = 22;
	/** TRUE if this is a replica of an IMAP proxy, enables certain * special cases for interacting with db */
	public static final int DBOPTBIT_IS_INET_REPL = 23;
	/** TRUE if db is a Lightweight NAB */
	public static final int DBOPTBIT_IS_LIGHT_ADDRESS_BOOK = 24;
	/** TRUE if database has notes which refer to an object store - for garbage collection*/
	public static final int DBOPTBIT_ACTIVE_OBJSTORE = 25;
	/** TRUE if database is globally routed */
	public static final int DBOPTBIT_GLOBALLY_ROUTED = 26;
	/** TRUE if database has mail autoprocessing enabled */
	public static final int DBOPTBIT_CS_AUTOPROCESSING_ENABLED = 27;
	/** TRUE if database has mail filters enabled */
	public static final int DBOPTBIT_MAIL_FILTERS_ENABLED = 28;
	/** TRUE if database holds subscriptions */
	public static final int DBOPTBIT_IS_SUBSCRIPTIONDB = 29;
	/** TRUE if data base supports "check-in" "check-out" */
	public static final int DBOPTBIT_IS_LOCK_DB = 30;
	/** TRUE if editor must lock notes to edit */
	public static final int DBOPTBIT_IS_DESIGNLOCK_DB = 31;


	/* ODS26+ options */
	/** if TRUE, store all modified index blocks in lz1 compressed form */
	public static final int DBOPTBIT_COMPRESS_INDEXES = 33;
	/** if TRUE, store all modified buckets in lz1 compressed form */
	public static final int DBOPTBIT_COMPRESS_BUCKETS = 34;
	/** FALSE by default, turned on forever if DBFLAG_COMPRESS_INDEXES or DBFLAG_COMPRESS_BUCKETS are ever turned on. */
	public static final int DBOPTBIT_POSSIBLY_COMPRESSED = 35;
	/** TRUE if freed space in db is not overwritten */
	public static final int DBOPTBIT_NO_FREE_OVERWRITE = 36;
	/** DB doesn't maintain unread marks */
	public static final int DBOPTBIT_NOUNREAD = 37;
	/** TRUE if the database does not maintain note hierarchy info. */
	public static final int DBOPTBIT_NO_RESPONSE_INFO = 38;
	/** Disabling of response info will happen on next compaction */
	public static final int DBOPTBIT_DISABLE_RSP_INFO_PEND = 39;
	/** Enabling of response info will happen on next compaction */
	public static final int DBOPTBIT_ENABLE_RSP_INFO_PEND = 40;
	/** Form/Bucket bitmap optimization is enabled */
	public static final int DBOPTBIT_FORM_BUCKET_OPT = 41;
	/** Disabling of Form/Bucket bitmap opt will happen on next compaction */
	public static final int DBOPTBIT_DISABLE_FORMBKT_PEND = 42;
	/** Enabling of Form/Bucket bitmap opt will happen on next compaction */
	public static final int DBOPTBIT_ENABLE_FORMBKT_PEND = 43;
	/** If TRUE, maintain LastAccessed */
	public static final int DBOPTBIT_MAINTAIN_LAST_ACCESSED = 44;
	/** If TRUE, transaction logging is disabled for this database */
	public static final int DBOPTBIT_DISABLE_TXN_LOGGING = 45;
	/** If TRUE, monitors can't be used against this database (non-replicating) */
	public static final int DBOPTBIT_MONITORS_NOT_ALLOWED = 46;
	/** If TRUE, all transactions on this database are nested top actions */
	public static final int DBOPTBIT_NTA_ALWAYS = 47;
	/** If TRUE, objects are not to be logged */
	public static final int DBOPTBIT_DONTLOGOBJECTS = 48;
	/** If set, the default delete is soft. Can be overwritten by UPDATE_DELETE_HARD */
	public static final int DBOPTBIT_DELETES_ARE_SOFT = 49;

	/* The following bits are used by the webserver and are gotten from the icon note */
	/** if TRUE, the Db needs to be opened using SSL over HTTP */
	public static final int DBOPTBIT_HTTP_DBIS_SSL = 50;
	/** if TRUE, the Db needs to use JavaScript to render the HTML for formulas, buttons, etc */
	public static final int DBOPTBIT_HTTP_DBIS_JS = 51;
	/** if TRUE, there is a $DefaultLanguage value on the $icon note */
	public static final int DBOPTBIT_HTTP_DBIS_MULTILANG = 52;

	/* ODS37+ options */
	/** if TRUE, database is a mail.box (ODS37 and up) */
	public static final int DBOPTBIT_IS_MAILBOX = 53;
	/** if TRUE, database is allowed to have /gt;64KB UNK table */
	public static final int DBOPTBIT_LARGE_UNKTABLE = 54;
	/** If TRUE, full-text index is accent sensitive */
	public static final int DBOPTBIT_ACCENT_SENSITIVE_FT = 55;
	/** TRUE if database has NSF support for IMAP enabled */
	public static final int DBOPTBIT_IMAP_ENABLED = 56;
	/** TRUE if database is a USERless N&amp;A Book */
	public static final int DBOPTBIT_USERLESS_NAB = 57;
	/** TRUE if extended ACL's apply to this Db */
	public static final int DBOPTBIT_EXTENDED_ACL = 58;
	/** TRUE if connections to = 3;rd party DBs are allowed */
	public static final int DBOPTBIT_DECS_ENABLED = 59;
	/** TRUE if a = 1;+ referenced shared template. Sticky bit once referenced. */
	public static final int DBOPTBIT_IS_SHARED_TEMPLATE = 60;
	/** TRUE if database is a mailfile */
	public static final int DBOPTBIT_IS_MAILFILE = 61;
	/** TRUE if database is a web application */
	public static final int DBOPTBIT_IS_WEBAPPLICATION = 62;
	/** TRUE if the database should not be accessible via the standard URL syntax */
	public static final int DBOPTBIT_HIDE_FROM_WEB = 63;
	/** TRUE if database contains one or more enabled background agent */
	public static final int DBOPTBIT_ENABLED_BGAGENT = 64;
	/** database supports LZ1 compression. */
	public static final int DBOPTBIT_LZ1 = 65;
	/** TRUE if database has default language */
	public static final int DBOPTBIT_HTTP_DBHAS_DEFLANG = 66;
	/** TRUE if database design refresh is only on admin server */
	public static final int DBOPTBIT_REFRESH_DESIGN_ON_ADMIN = 67;
	/** TRUE if shared template should be actively used to merge in design. */
	public static final int DBOPTBIT_ACTIVE_SHARED_TEMPLATE = 68;
	/** TRUE to allow the use of themes when displaying the application. */
	public static final int DBOPTBIT_APPLY_THEMES = 69;
	/** TRUE if unread marks replicate */
	public static final int DBOPTBIT_UNREAD_REPLICATION = 70;
	/** TRUE if unread marks replicate out of the cluster */
	public static final int DBOPTBIT_UNREAD_REP_OUT_OF_CLUSTER = 71;
	/** TRUE, if the mail file is a migrated one from Exchange */
	public static final int DBOPTBIT_IS_MIGRATED_EXCHANGE_MAILFILE = 72;
	/** TRUE, if the mail file is a migrated one from Exchange */
	public static final int DBOPTBIT_NEED_EX_NAMEFIXUP = 73;
	/** TRUE, if out of office service is enabled in a mail file */
	public static final int DBOPTBIT_OOS_ENABLED = 74;
	/** TRUE if Support Response Threads is enabled in database */
	public static final int DBOPTBIT_SUPPORT_RESP_THREADS = 75;
	/**TRUE if the database search is disabled<br>
	 * LI = 4463;.02. give the admin a mechanism to prevent db search in scenarios
	 * where the db is very large, they don't want to create new views, and they
	 * don't want a full text index
	 */
	public static final int DBOPTBIT_NO_SIMPLE_SEARCH = 76;
	/** TRUE if the database FDO is repaired to proper coalation function. */
	public static final int DBOPTBIT_FDO_REPAIRED = 77;
	/** TRUE if the policy settings have been removed from a db with no policies */
	public static final int DBOPTBIT_POLICIES_REMOVED = 78;
	/** TRUE if Superblock is compressed. */
	public static final int DBOPTBIT_COMPRESSED_SUPERBLOCK = 79;
	/** TRUE if design note non-summary should be compressed */
	public static final int DBOPTBIT_COMPRESSED_DESIGN_NS = 80;
	/** TRUE if the db has opted in to use DAOS */
	public static final int DBOPTBIT_DAOS_ENABLED = 81;

	/** TRUE if all data documents in database should be compressed (compare with DBOPTBIT_COMPRESSED_DESIGN_NS) */
	public static final int DBOPTBIT_COMPRESSED_DATA_DOCS = 82;
	/** TRUE if views in this database should be skipped by server-side update task */
	public static final int DBOPTBIT_DISABLE_AUTO_VIEW_UPDS = 83;
	/** if TRUE, Domino can suspend T/L check for DAOS items because the dbtarget is expendable */
	public static final int DBOPTBIT_DAOS_LOGGING_NOT_REQD = 84;
	/** TRUE if exporting of view data is to be disabled */
	public static final int DBOPTBIT_DISABLE_VIEW_EXPORT = 85;
	/** TRUE if database is a NAB which contains config information, groups, and mailin databases but where users are stored externally. */
	public static final int DBOPTBIT_USERLESS2_NAB = 86;
	/** LLN2 specific, added to this codestream to reserve this value */
	public static final int DBOPTBIT_ADVANCED_PROP_OVERRIDE = 87;
	/** Turn off VerySoftDeletes for ODS51 */
	public static final int DBOPTBIT_NO_VSD = 88;
	/** NSF is to be used as a cache */
	public static final int DBOPTBIT_LOCAL_CACHE = 89;
	/** Set to force next compact to be out of place. Initially done for ODS upgrade of in use Dbs, but may have other uses down the road. The next compact will clear this bit, it is transitory. */
	public static final int DBOPTBIT_COMPACT_NO_INPLACE = 90;
	/** from LLN2 */
	public static final int DBOPTBIT_NEEDS_ZAP_LSN = 91;
	/** set to indicate this is a system db (eg NAB, mail.box, etc) so we don't rely on the db name */
	public static final int DBOPTBIT_IS_SYSTEM_DB = 92;
	/** TRUE if the db has opted in to use PIRC */
	public static final int DBOPTBIT_PIRC_ENABLED = 93;
	/** from lln2 */
	public static final int DBOPTBIT_DBMT_FORCE_FIXUP = 94;
	/** TRUE if the db has likely a complete design replication - for PIRC control */
	public static final int DBOPTBIT_DESIGN_REPLICATED = 95;
	/** on the = 1;-&gt;0 transition rename the file (for LLN2 keep in sync please) */
	public static final int DBOPTBIT_MARKED_FOR_PENDING_DELETE = 96;
	public static final int DBOPTBIT_IS_NDX_DB = 97;
	/** move NIF containers &amp; collection objects out of nsf into .ndx db */
	public static final int DBOPTBIT_SPLIT_NIF_DATA = 98;
	/** NIFNSF is off but not all containers have been moved out yet */
	public static final int DBOPTBIT_NIFNSF_OFF = 99;
	/** Inlined indexing exists for this DB */
	public static final int DBOPTBIT_INLINE_INDEX = 100;
	/** db solr search enabled */
	public static final int DBOPTBIT_SOLR_SEARCH = 101;
	/** init solr index done */
	public static final int DBOPTBIT_SOLR_SEARCH_INIT_DONE = 102;
	/** Folder sync enabled for database (sync Drafts, Sent and Trash views to IMAP folders) */
	public static final int DBOPTBIT_IMAP_FOLDERSYNC = 103;
	/** Large Summary Support (LSS) */
	public static final int DBOPTBIT_LARGE_BUCKETS_ENABLED = 104;

	/** Open with scan lock to prevent other opens with scan lock (used by replicator) */
	public static final short DBOPEN_WITH_SCAN_LOCK = 0x0001;
	/** DbPurge while opening */
	public static final short DBOPEN_PURGE = 0x0002;	
	/** No user info may be available, so don't ask for it */
	public static final short DBOPEN_NO_USERINFO	= 0x0004;
	/** Force a database fixup */
	public static final short DBOPEN_FORCE_FIXUP = 0x0008	;
	/** Scan all notes and all items (not incremental) */
	public static final short DBOPEN_FIXUP_FULL_NOTE_SCAN = 0x0010;
	/** Do not delete bad notes during note scan */
	public static final short DBOPEN_FIXUP_NO_NOTE_DELETE = 0x0020;
	/** If open fails try cluster failover */
	public static final short DBOPEN_CLUSTER_FAILOVER	 = 0x0080;
	/** Close session on error paths */
	public static final short DBOPEN_CLOSE_SESS_ON_ERROR = 0x0100	;
	/** don't log errors - used when opening log database! */
	public static final short DBOPEN_NOLOG = 0x0200;


	/** Open and read all information out of the id file */
	public static final int SECKFM_open_All = 0x00000001;
	/** Write information conatined inthe handle out to the specified ID file */
	public static final int SECKFM_close_WriteIdFile = 0x00000001;

	/** Don't set environment variable used to identify the ID file during process initialization -
	 * usually either ServerKeyFileName or KeyFileName. See SECKFMSwitchToIDFile. */
	public static final int fKFM_switchid_DontSetEnvVar	= 0x00000008;

	/*	Function codes for routine SECKFMGetPublicKey */

	public short KFM_pubkey_Primary = 0;
	public short KFM_pubkey_International = 1;
	
	public short fSECToken_EnableRenewal = 0x0001;

	public int MAXONESEGSIZE = 0xffff - 1-128;
	public int MQ_MAX_MSGSIZE = MAXONESEGSIZE - 0x50;
	public short NOPRIORITY = (short) (0xffff & 0xffff);
	public short LOWPRIORITY = (short) (0xffff & 0xffff);
	public short HIGHPRIORITY = 0;

	/*	Options to MQGet */

	public short MQ_WAIT_FOR_MSG = 0x0001;

	/* Options to MQOpen */
	
	/** Create the queue if it doesn't exist*/
	public int MQ_OPEN_CREATE = 0x00000001;
	
	/*	Public Queue Names */

	/** Prepended to "addin" task name to form task's queue name */
	public String TASK_QUEUE_PREFIX	= "MQ$";			
												
	/** DB Server */
	public String SERVER_QUEUE_NAME	= "_SERVER";			
	/** Replicator */
	public String REPL_QUEUE_NAME = TASK_QUEUE_PREFIX + "REPLICATOR";
	/** Mail Router */
	public String ROUTER_QUEUE_NAME	= TASK_QUEUE_PREFIX + "ROUTER";
	/** Index views &amp; full text process */
	public String UPDATE_QUEUE_NAME = TASK_QUEUE_PREFIX + "INDEXER";
	/** Login Process */
	public String LOGIN_QUEUE_NAME = TASK_QUEUE_PREFIX + "LOGIN";
	/** Event process */
	public String EVENT_QUEUE_NAME = TASK_QUEUE_PREFIX + "EVENT";
	/** Report process */
	public String REPORT_QUEUE_NAME = TASK_QUEUE_PREFIX + "REPORTER";
	/** Cluster Replicator */
	public String CLREPL_QUEUE_NAME = TASK_QUEUE_PREFIX + "CLREPL";
	/** Fixup */
	public String FIXUP_QUEUE_NAME = TASK_QUEUE_PREFIX + "FIXUP";
	/** Collector*/
	public String COLLECT_QUEUE_NAME = TASK_QUEUE_PREFIX + "COLLECTOR";
	/** NOI Process */
	public String NOI_QUEUE_NAME = TASK_QUEUE_PREFIX + "DIIOP";
	/** Alarms Cache daemon */
	public String ALARM_QUEUE_NAME = TASK_QUEUE_PREFIX + "ALARMS";
	/** Monitor */
	public String MONITOR_QUEUE_NAME = TASK_QUEUE_PREFIX + "MONITOR";
	/** Monitor */
	public String MONALARM_QUEUE_NAME = TASK_QUEUE_PREFIX + "MONITORALARM";
	/** Admin Panel Daemon (Request Queue) */
	public String APDAEMON_REQ_QUEUE = TASK_QUEUE_PREFIX + "APDAEMONREQ";
	/** Admin Panel Daemon (File Response Queue) */
	public String APDAEMON_FILERES_QUEUE = TASK_QUEUE_PREFIX + "APDAEMONFILERESPONSE";
	/** Admin Panel Daemon (Server Response Queue) */
	public String APDAEMON_FILEREQ_QUEUE = TASK_QUEUE_PREFIX + "APDAEMONFILEREQUEST";
	/** bktasks */
	public String BKTASKS_QUEUE_NAME = TASK_QUEUE_PREFIX + "BKTASKS";
	/** Red Zone Interface to Collector */
	public String RZINTER_QUEUE_NAME = TASK_QUEUE_PREFIX + "RZINTER";
	/** Red Zone Extra MQ */
	public String RZEXTRA_QUEUE_NAME = TASK_QUEUE_PREFIX + "RZEXTRA";
	/** Red Zone Background MQ */
	public String RZBG_QUEUE_NAME = TASK_QUEUE_PREFIX + "RZBG";
	/** Red Zone Background Extra MQ */
	public String RZBGEXTRA_QUEUE_NAME = TASK_QUEUE_PREFIX + "RZBGEXTRA";
	/** Monitor */
	public String REALTIME_STATS_QUEUE_NAME = TASK_QUEUE_PREFIX + "REALTIME";
	/** Runjava (used by ISpy) */
	public String RUNJAVA_QUEUE_NAME = TASK_QUEUE_PREFIX + "RUNJAVA";
	/** Runjava (used by ISpy) */
	public String STATS_QUEUE_NAME = TASK_QUEUE_PREFIX + "STATS";
	/** Runjava (used by ISpy) */
	public String LOG_SEARCH_QUEUE_NAME = TASK_QUEUE_PREFIX + "LOGSEARCH";
	/** Event process */
	public String DAEMON_EVENT_QUEUE_NAME = TASK_QUEUE_PREFIX + "DAEMONEVENT";
	/** Collector*/
	public String DAEMON_COLLECT_QUEUE_NAME = TASK_QUEUE_PREFIX + "DAEMONCOLLECTOR";
	/** Dircat */
	public String DIRCAT_QUEUE_NAME = TASK_QUEUE_PREFIX + "DIRCAT";
	
	/** Instructs the NSGetServerClusterMates function to not use the cluster name cache
	 * and forces a lookup on the target server instead */
	public static int CLUSTER_LOOKUP_NOCACHE = 0x00000001;
	
	/** Instructs the NSGetServerClusterMates function to only use the cluster name cache
	 * and restricts lookup to the workstation cache */
	public static int CLUSTER_LOOKUP_CACHEONLY = 0x00000002;
	
	/** Authors can't create new notes (only edit existing ones) */
	public short ACL_FLAG_AUTHOR_NOCREATE = 0x0001;
	/** Entry represents a Server (V4) */
	public short ACL_FLAG_SERVER = 0x0002;
	/** User cannot delete notes */
	public short ACL_FLAG_NODELETE = 0x0004;
	/** User can create personal agents (V4) */
	public short ACL_FLAG_CREATE_PRAGENT = 0x0008;
	/** User can create personal folders (V4) */
	public short ACL_FLAG_CREATE_PRFOLDER = 0x0010;
	/** Entry represents a Person (V4) */
	public short ACL_FLAG_PERSON = 0x0020;
	/** Entry represents a group (V4) */
	public short ACL_FLAG_GROUP = 0x0040;
	/** User can create and update shared views &amp; folders (V4)<br>
This allows an Editor to assume some Designer-level access */
	public short ACL_FLAG_CREATE_FOLDER = 0x0080;
	/** User can create LotusScript */
	public short ACL_FLAG_CREATE_LOTUSSCRIPT = 0x0100;
	/** User can read public notes */
	public short ACL_FLAG_PUBLICREADER = 0x0200;
	/** User can write public notes */
	public short ACL_FLAG_PUBLICWRITER = 0x0400;
	/** User CANNOT register monitors for this database */
	public short ACL_FLAG_MONITORS_DISALLOWED = 0x800;
	/** User cannot replicate or copy this database */
	public short ACL_FLAG_NOREPLICATE = 0x1000;
	/** Admin server can modify reader and author fields in db */
	public short ACL_FLAG_ADMIN_READERAUTHOR = 0X4000;
	/** Entry is administration server (V4) */
	public short ACL_FLAG_ADMIN_SERVER = (short) (0x8000 & 0xffff);

	/** User or Server has no access to the database. */
	public short ACL_LEVEL_NOACCESS = 0;
	/** User or Server can add new data documents to a database, but cannot examine the new document or the database. */
	public short ACL_LEVEL_DEPOSITOR = 1;
	/** User or Server can only view data documents in the database. */
	public short ACL_LEVEL_READER = 2;
	/** User or Server can create and/or edit their own data documents and examine existing ones in the database. */
	public short ACL_LEVEL_AUTHOR = 3;
	/** User or Server can create and/or edit any data document. */
	public short ACL_LEVEL_EDITOR = 4;
	/** User or Server can create and/or edit any data document and/or design document. */
	public short ACL_LEVEL_DESIGNER = 5;
	/** User or Server can create and/or maintain any type of database or document, including the ACL. */
	public short ACL_LEVEL_MANAGER = 6;

	/** Highest access level */
	public short ACL_LEVEL_HIGHEST = 6;
	/** Number of access levels */
	public short ACL_LEVEL_COUNT = 7;
	
	/** Number of privilege bits (10 bytes) */
	public int ACL_PRIVCOUNT	 = 80;
	/** Privilege name max (including null) */
	public int ACL_PRIVNAMEMAX = 16;
	/** Privilege string max  (including parentheses and null) */
	public int ACL_PRIVSTRINGMAX = (16+2);

	/** Require same ACL in ALL replicas of database */
	public int ACL_UNIFORM_ACCESS = 0x00000001;

	/* ACLUpdateEntry flags - Set flag if parameter is being modified */

	public short ACL_UPDATE_NAME = 0x01;
	public short ACL_UPDATE_LEVEL = 0x02;
	public short ACL_UPDATE_PRIVILEGES = 0x04;
	public short ACL_UPDATE_FLAGS = 0x08;
	
	/**
	 * Keys in a COLLECTIONDATA structure are divided into percentiles - divisions
	 * corresponding to one-tenth of the total range of keys - and a table of the keys
	 * marking the divisions is returned with that structure.  These constants are provided for indexing into the table.
	 */
	public static int PERCENTILE_COUNT = 11;
	public static int PERCENTILE_0 = 0;
	public static int PERCENTILE_10 = 1;
	public static int PERCENTILE_20 = 2;
	public static int PERCENTILE_30 = 3;
	public static int PERCENTILE_40 = 4;
	public static int PERCENTILE_50 = 5;
	public static int PERCENTILE_60 = 6;
	public static int PERCENTILE_70 = 7;
	public static int PERCENTILE_80 = 8;
	public static int PERCENTILE_90 = 9;
	public static int PERCENTILE_100 = 10;

	/*  Options used when calling ReplicateWithServer */

	/** Receive notes from server (pull) */
	public int REPL_OPTION_RCV_NOTES = 0x00000001;
	/** Send notes to server (push) */
	public int REPL_OPTION_SEND_NOTES = 0x00000002;
	/** Replicate all database files */
	public int REPL_OPTION_ALL_DBS = 0x00000004;
	/** Close sessions when done */
	public int REPL_OPTION_CLOSE_SESS = 0x00000040;
	/** Replicate NTFs as well */
	public int REPL_OPTION_ALL_NTFS = 0x00000400;
	/** Low, Medium &amp; High priority databases */
	public int REPL_OPTION_PRI_LOW = 0x00000000;
	/** Medium &amp; High priority databases only */
	public int REPL_OPTION_PRI_MED = 0x00004000;
	/** High priority databases only */
	public int REPL_OPTION_PRI_HI = 0x00008000;
	
	 /* Use following bits with
		ReplicateWithServerExt only */
	
/* 0x00010000-0x8000000 WILL NOT BE HONORED BY V3 SERVERS, BECAUSE V3 ONLY LOOKS AT THE FIRST 16 BITS! */
	
	/** Abstract/truncate docs to summary data and first RTF field. (~40K) */
	public int REPL_OPTION_ABSTRACT_RTF = 0x00010000;
	/** Abstract/truncate docs to summary only data. */
	public int REPL_OPTION_ABSTRACT_SMRY = 0x00020000;
	/** Replicate private documents even if not selected by default. */
	public int REPL_OPTION_PRIVATE = 0x00400000;
	
	public int REPL_OPTION_ALL_FILES = (REPL_OPTION_ALL_DBS | REPL_OPTION_ALL_NTFS);


	/** Indirect way to call NEMMessageBox */;
	public short OS_SIGNAL_MESSAGE = 3;

	/** Paint busy indicator on screen */
	public short OS_SIGNAL_BUSY = 4;

	

	/*	Definitions specific to busy signal handler */

	/** Remove the "File Activity" indicator */
	public short BUSY_SIGNAL_FILE_INACTIVE = 0;
	/** Display the "File Activity" indicator (not supported on all platforms) */
	public short BUSY_SIGNAL_FILE_ACTIVE = 1;
	/** Remove the "Network Activity" indicator. */
	public short BUSY_SIGNAL_NET_INACTIVE = 2;
	/** Display the "Network Activity" indicator. */
	public short BUSY_SIGNAL_NET_ACTIVE = 3;
	/** Display the "Poll" indicator. */
	public short BUSY_SIGNAL_POLL = 4;
	/** Display the "Wan Sending" indicator. */
	public short BUSY_SIGNAL_WAN_SENDING = 5;
	/** Display the "Wan Receiving" indicator. */
	public short BUSY_SIGNAL_WAN_RECEIVING = 6;

	/** Called from NET to see if user cancelled I/O */
	public short OS_SIGNAL_CHECK_BREAK = 5;

	/** Put up and manipulate the system wide progress indicator. */
	public short OS_SIGNAL_PROGRESS = 13;

	/**	N/A						N/A		*/
	public short PROGRESS_SIGNAL_BEGIN = 0;
	/**	N/A						N/A		*/
	public short PROGRESS_SIGNAL_END = 1;
	/**	Range					N/A		*/
	public short PROGRESS_SIGNAL_SETRANGE = 2;
	/**	pText1					pText2 - usually NULL. */
	public short PROGRESS_SIGNAL_SETTEXT = 3;
	/**	New progress pos		N/A		*/
	public short PROGRESS_SIGNAL_SETPOS = 4;
	/**	Delta of progress pos	N/A		*/
	public short PROGRESS_SIGNAL_DELTAPOS = 5;
	/**  Total Bytes */
	public short PROGRESS_SIGNAL_SETBYTERANGE = 6;
	/**	Bytes Done	*/
	public short PROGRESS_SIGNAL_SETBYTEPOS = 7;

	
	public short OS_SIGNAL_REPL = 15;

	

	/*  Definitions for replication state signal handler */
	/*	pText1		pText2. */

	/**	None					*/
	public short REPL_SIGNAL_IDLE = 0;
	/**	None					*/
	public short REPL_SIGNAL_PICKSERVER = 1;
	/**	pServer		pPort		*/
	public short REPL_SIGNAL_CONNECTING = 2;
	/**	pServer		pPort		*/
	public short REPL_SIGNAL_SEARCHING = 3;
	/**	pServerFile	pLocalFile	*/
	public short REPL_SIGNAL_SENDING = 4;
	/**	pServerFile	pLocalFile	*/
	public short REPL_SIGNAL_RECEIVING = 5;
	/**	pSrcFile				*/
	public short REPL_SIGNAL_SEARCHINGDOCS = 6;
	/**	pLocalFile	pReplFileStats */
	public short REPL_SIGNAL_DONEFILE = 7;
	/**	pServerFile pLocalFile	*/
	public short REPL_SIGNAL_REDIRECT = 8;
	/**	None				*/
	public short REPL_SIGNAL_BUILDVIEW = 9;
	/**	None				*/
	public short REPL_SIGNAL_ABORT = 10;

	public int HTMLAPI_PROP_TEXTLENGTH = 0;
	public int HTMLAPI_PROP_NUMREFS = 1;
	public int HTMLAPI_PROP_USERAGENT_LEN = 3;
	public int HTMLAPI_PROP_USERAGENT = 4;
	public int HTMLAPI_PROP_BINARYDATA = 6;
	public int HTMLAPI_PROP_MIMEMAXLINELENSEEN = 102;
	
	int CAI_Start = 0;
	int CAI_StartKey = 1;
	int CAI_Count = 2;
	int CAI_Expand = 3;
	int CAI_FullyExpand = 4;
	int CAI_ExpandView = 5;
	int CAI_Collapse = 6;
	int CAI_CollapseView = 7;
	int CAI_3PaneUI = 8;
	int CAI_TargetFrame = 9;
	int CAI_FieldElemType = 10;
	int CAI_FieldElemFormat = 11;
	int CAI_SearchQuery = 12;
	int CAI_OldSearchQuery = 13;
	int CAI_SearchMax = 14;
	int CAI_SearchWV = 15;
	int CAI_SearchOrder = 16;
	int CAI_SearchThesarus = 17;
	int CAI_ResortAscending = 18;
	int CAI_ResortDescending = 19;
	int CAI_ParentUNID = 20;
	int CAI_Click = 21;
	int CAI_UserName = 22;
	int CAI_Password = 23;
	int CAI_To = 24;
	int CAI_ISMAPx = 25;
	int CAI_ISMAPy = 26;
	int CAI_Grid = 27;
	int CAI_Date = 28;
	int CAI_TemplateType = 29;
	int CAI_TargetUNID = 30;
	int CAI_ExpandSection = 31;
	int CAI_Login = 32;
	int CAI_PickupCert = 33;
	int CAI_PickupCACert = 34;
	int CAI_SubmitCert = 35;
	int CAI_ServerRequest = 36;
	int CAI_ServerPickup = 37;
	int CAI_PickupID = 38;
	int CAI_TranslateForm = 39;
	int CAI_SpecialAction = 40;
	int CAI_AllowGetMethod = 41;
	int CAI_Seq = 42;
	int CAI_BaseTarget = 43;
	int CAI_ExpandOutline = 44;
	int CAI_StartOutline = 45;
	int CAI_Days = 46;
	int CAI_TableTab = 47;
	int CAI_MIME = 48;
	int CAI_RestrictToCategory = 49;
	int CAI_Highlight = 50;
	int CAI_Frame = 51;
	int CAI_FrameSrc = 52;
	int CAI_Navigate = 53;
	int CAI_SkipNavigate = 54;
	int CAI_SkipCount = 55;
	int CAI_EndView = 56;
	int CAI_TableRow = 57;
	int CAI_RedirectTo = 58;
	int CAI_SessionId = 59;
	int CAI_SourceFolder = 60;
	int CAI_SearchFuzzy = 61;
	int CAI_HardDelete = 62;
	int CAI_SimpleView = 63;
	int CAI_SearchEntry = 64;
	int CAI_Name = 65;
	int CAI_Id = 66;
	int CAI_RootAlias = 67;
	int CAI_Scope = 68;
	int CAI_DblClkTarget = 69;
	int CAI_Charset = 70;
	int CAI_EmptyTrash = 71;
	int CAI_EndKey = 72;
	int CAI_PreFormat = 73;
	int CAI_ImgIndex = 74;
	int CAI_AutoFramed = 75;
	int CAI_OutputFormat = 76;
	int CAI_InheritParent = 77;
	int CAI_Last = 78;
	
	int kUnknownCmdId = 0;
	int kOpenServerCmdId = 1;
	int kOpenDatabaseCmdId = 2;
	int kOpenViewCmdId = 3;
	int kOpenDocumentCmdId = 4;
	int kOpenElementCmdId = 5;
	int kOpenFormCmdId = 6;
	int kOpenAgentCmdId = 7;
	int kOpenNavigatorCmdId = 8;
	int kOpenIconCmdId = 9;
	int kOpenAboutCmdId = 10;
	int kOpenHelpCmdId = 11;
	int kCreateDocumentCmdId = 12;
	int kSaveDocumentCmdId = 13;
	int kEditDocumentCmdId = 14;
	int kDeleteDocumentCmdId = 15;
	int kSearchViewCmdId = 16;
	int kSearchSiteCmdId = 17;
	int kNavigateCmdId = 18;
	int kReadFormCmdId = 19;
	int kRequestCertCmdId = 20;
	int kReadDesignCmdId = 21;
	int kReadViewEntriesCmdId = 22;
	int kReadEntriesCmdId = 23;
	int kOpenPageCmdId = 24;
	int kOpenFrameSetCmdId = 25;
	/** OpenField command for Java applet(s) and HAPI */
	int kOpenFieldCmdId = 26;
	int kSearchDomainCmdId = 27;
	int kDeleteDocumentsCmdId = 28;
	int kLoginUserCmdId = 29;
	int kLogoutUserCmdId = 30;
	int kOpenImageResourceCmdId = 31;
	int kOpenImageCmdId = 32;
	int kCopyToFolderCmdId = 33;
	int kMoveToFolderCmdId = 34;
	int kRemoveFromFolderCmdId = 35;
	int kUndeleteDocumentsCmdId = 36;
	int kRedirectCmdId = 37;
	int kGetOrbCookieCmdId = 38;
	int kOpenCssResourceCmdId = 39;
	int kOpenFileResourceCmdId = 40;
	int kOpenJavascriptLibCmdId = 41;
	int kUnImplemented_01 = 42;
	int kChangePasswordCmdId = 43;
	int kOpenPreferencesCmdId = 44;
	int kOpenWebServiceCmdId = 45;
	int kWsdlCmdId = 46;
	int kGetImageCmdId = 47;
	int kNumberOfCmds = 48;
	/**
	 * arg value is a pointer to a nul-terminated string
	 */
	int CAVT_String = 0;
	/**
	 * arg value is an int
	 */
	int CAVT_Int = 1;
	/**
	 * arg value is a NOTEID
	 */
	int CAVT_NoteId = 2;
	/**
	 * arg value is an UNID
	 */
	int CAVT_UNID = 3;
	/**
	 * arg value is a list of null-terminated strings
	 */
	int CAVT_StringList = 4;
	
	int UAT_None = 0;
	int UAT_Server = 1;
	int UAT_Database = 2;
	int UAT_View = 3;
	int UAT_Form = 4;
	int UAT_Navigator = 5;
	int UAT_Agent = 6;
	int UAT_Document = 7;
	/** internal filename of attachment */
	int UAT_Filename = 8;
	/** external filename of attachment if different */
	int UAT_ActualFilename = 9;
	int UAT_Field = 10;
	int UAT_FieldOffset = 11;
	int UAT_FieldSuboffset = 12;
	int UAT_Page = 13;
	int UAT_FrameSet = 14;
	int UAT_ImageResource = 15;
	int UAT_CssResource = 16;
	int UAT_JavascriptLib = 17;
	int UAT_FileResource = 18;
	int UAT_About = 19;
	int UAT_Help = 20;
	int UAT_Icon = 21;
	int UAT_SearchForm = 22;
	int UAT_SearchSiteForm = 23;
	int UAT_Outline = 24;
	/** must be the last one */
	int UAT_NumberOfTypes = 25;
	
	int URT_None = 0;
	int URT_Name = 1;
	int URT_Unid = 2;
	int URT_NoteId = 3;
	int URT_Special = 4;
	int URT_RepId = 5;
	int USV_About = 0;
	int USV_Help = 1;
	int USV_Icon = 2;
	int USV_DefaultView = 3;
	int USV_DefaultForm = 4;
	int USV_DefaultNav = 5;
	int USV_SearchForm = 6;
	int USV_DefaultOutline = 7;
	int USV_First = 8;
	int USV_FileField = 9;
	int USV_NumberOfValues = 10;
	/**
	 * unknown purpose
	 */
	int HTMLAPI_REF_UNKNOWN = 0;
	/**
	 * A tag HREF= value
	 */
	int HTMLAPI_REF_HREF = 1;
	/**
	 * IMG tag SRC= value
	 */
	int HTMLAPI_REF_IMG = 2;
	/**
	 * (I)FRAME tag SRC= value
	 */
	int HTMLAPI_REF_FRAME = 3;
	/**
	 * Java applet reference
	 */
	int HTMLAPI_REF_APPLET = 4;
	/**
	 * plugin SRC= reference
	 */
	int HTMLAPI_REF_EMBED = 5;
	/**
	 * active object DATA= referendce
	 */
	int HTMLAPI_REF_OBJECT = 6;
	/**
	 * BASE tag value
	 */
	int HTMLAPI_REF_BASE = 7;
	/**
	 * BODY BACKGROUND
	 */
	int HTMLAPI_REF_BACKGROUND = 8;
	/**
	 * IMG SRC= value from MIME message
	 */
	int HTMLAPI_REF_CID = 9;
	
	/** Flag to indicate unique keys. */
	public static byte COLLATION_FLAG_UNIQUE = 0x01;
	/** Flag to indicate only build demand. */
	public static byte COLLATION_FLAG_BUILD_ON_DEMAND = 0x02;

	public static byte COLLATION_SIGNATURE = 0x44;

	/** Collate by key in summary buffer (requires key name string) */
	public static byte COLLATE_TYPE_KEY = 0;
	/** Collate by note ID */
	public static byte COLLATE_TYPE_NOTEID = 3;
	/** Collate by "tumbler" summary key (requires key name string) */
	public static byte COLLATE_TYPE_TUMBLER = 6;
	/** Collate by "category" summary key (requires key name string) */
	public static byte COLLATE_TYPE_CATEGORY = 7;
	
	public static byte COLLATE_TYPE_MAX = 7;

	/** True if descending */
	public static byte CDF_S_descending = 0;
	/** False if ascending order (default) */
	public static byte CDF_M_descending = 0x01;
	/** Obsolete - see new constant below */
	public static byte CDF_M_caseinsensitive = 0x02;
	/** If prefix list, then ignore for sorting */
	public static byte CDF_M_ignoreprefixes = 0x02;
	/** Obsolete - see new constant below */
	public static byte CDF_M_accentinsensitive = 0x04;
	/** If set, lists are permuted */
	public static byte CDF_M_permuted = 0x08;
	/** Qualifier if lists are permuted; if set, lists are pairwise permuted, otherwise lists are multiply permuted. */
	public static byte CDF_M_permuted_pairwise = 0x10;
	/** If set, treat as permuted */
	public static byte CDF_M_flat_in_v5 = 0x20;
	/** If set, text compares are case-sensitive */
	public static byte CDF_M_casesensitive_in_v5 = 0x40;
	/** If set, text compares are accent-sensitive */
	public static byte CDF_M_accentsensitive_in_v5 = (byte) (0x80 & 0xff);
	
	public static byte COLLATE_DESCRIPTOR_SIGNATURE = 0x66;

	// flags1 values of VIEW_TABLE_FORMAT
	/** Default to fully collapsed */
	public static short VIEW_TABLE_FLAG_COLLAPSED = 0x0001;
	/** Do not index hierarchically. If FALSE, MUST have NSFFormulaSummaryItem($REF) as LAST item! */
	public static short VIEW_TABLE_FLAG_FLATINDEX = 0x0002;
	/** Display unread flags in margin at ALL levels */
	public static short VIEW_TABLE_FLAG_DISP_ALLUNREAD = 0x0004;
	/** Display replication conflicts. If TRUE, MUST have NSFFormulaSummaryItem($Conflict) as SECOND-TO-LAST item! */
	public static short VIEW_TABLE_FLAG_CONFLICT = 0x0008;
	/** Display unread flags in margin for documents only */
	public static short VIEW_TABLE_FLAG_DISP_UNREADDOCS = 0x0010;
	/** Position to top when view is opened. */
	public static short VIEW_TABLE_GOTO_TOP_ON_OPEN = 0x0020;	
	/** Position to bottom when view is opened. */
	public static short VIEW_TABLE_GOTO_BOTTOM_ON_OPEN = 0x0040;
	/** Color alternate rows. */
	public static short VIEW_TABLE_ALTERNATE_ROW_COLORING	 = 0x0080;
	/** Hide headings. */
	public static short VIEW_TABLE_HIDE_HEADINGS = 0x0100;
	/** Hide left margin. */
	public static short VIEW_TABLE_HIDE_LEFT_MARGIN = 0x0200;	
	/** Show simple (background color) headings. */
	public static short VIEW_TABLE_SIMPLE_HEADINGS = 0x0400;
	/** TRUE if LineCount is variable (can be reduced as needed). */
	public static short VIEW_TABLE_VARIABLE_LINE_COUNT = 0x0800;
	/*	Refresh flags.
	 * 
	 * When both flags are clear, automatic refresh of display on update notification is disabled.
	 * In this case, the refresh indicator will be displayed.
	 *
	 * When VIEW_TABLE_GOTO_TOP_ON_REFRESH is set, the view will fe refreshed from the top row of
	 * the collection (as if the user pressed F9 and Ctrl-Home).
	 *
	 * When VIEW_TABLE_GOTO_BOTTOM_ON_REFRESH is set, the view will be refreshed so the bottom row of
	 * the collection is visible (as if the user pressed F9 and Ctrl-End).
	 *
	 * When BOTH flags are set (done to avoid using another bit in the flags), the view will be
	 * refreshed from the current top row (as if the user pressed F9). */

	/** Position to top when view is refreshed. */
	public static short VIEW_TABLE_GOTO_TOP_ON_REFRESH = 0x1000;
	/** Position to bottom when view is refreshed. */
	public static short VIEW_TABLE_GOTO_BOTTOM_ON_REFRESH = 0x2000;

	/** TRUE if last column should be extended to fit the window width. */
	public static short VIEW_TABLE_EXTEND_LAST_COLUMN = 0x4000;
	/** TRUE if the View indexing should work from the Right most column */
	public static short VIEW_TABLE_RTLVIEW = (short) (0x8000 & 0xffff);

	// flags2 values of VIEW_TABLE_FORMAT
	
	/** TRUE if we should display no-borders at all on the header */
	public static short VIEW_TABLE_FLAT_HEADINGS = 0x0001	;
	/** TRUE if the icons displayed inthe view should be colorized */
	public static short VIEW_TABLE_COLORIZE_ICONS = 0x0002;
	/** TRUE if we should not display a search bar for this view */
	public static short VIEW_TABLE_HIDE_SB = 0x0004;
	/** TRUE if we should hide the calendar header */
	public static short VIEW_TABLE_HIDE_CAL_HEADER = 0x0008;
	/** TRUE if view has not been customized (i.e. not saved by Designer) */
	public static short VIEW_TABLE_NOT_CUSTOMIZED = 0x0010;
	/** TRUE if view supports display of partial thread hierarchy (Hannover v8)*/
	public static short VIEW_TABLE_SHOW_PARITAL_THREADS = 0x0020;
	/** show partial index hierarchically, if TRUE */
	public static short VIEW_TABLE_FLAG_PARTIAL_FLATINDEX = 0x0020;
	
	
	/** Value for the wSig member of the VIEW_TABLE_FORMAT2 structure. */
	public static short VALID_VIEW_FORMAT_SIG = 0x2BAD;

	/** The VIEW_COLUMN_FORMAT record begins with a WORD value for the Signature of the record.<br>
	 * This symbol specifies the signature of the VIEW_COLUMN_FORMAT record. */
	public static short VIEW_COLUMN_FORMAT_SIGNATURE = 0x4356;
	/**
	 * The VIEW_COLUMN_FORMAT2 record begins with a WORD value for the Signature of the record.<br>
	 * This symbol specifies the signature of the VIEW_COLUMN_FORMAT2 record.  
	 */
	public static short VIEW_COLUMN_FORMAT_SIGNATURE2 = 0x4357;
	/**
	 * The VIEW_COLUMN_FORMAT3 record begins with a WORD value for the Signature of the record.<br>
	 * This symbol specifies the signature of the VIEW_COLUMN_FORMAT3 record.  
	 */
	public static short VIEW_COLUMN_FORMAT_SIGNATURE3 = 0x4358;
	/**
	 * The VIEW_COLUMN_FORMAT4 record begins with a WORD value for the Signature of the record.<br>
	 * This symbol specifies the signature of the VIEW_COLUMN_FORMAT4 record.  
	 */
	public static short VIEW_COLUMN_FORMAT_SIGNATURE4 = 0x4359;
	/**
	 * The VIEW_COLUMN_FORMAT5 record begins with a WORD value for the Signature of the record.<br>
	 * This symbol specifies the signature of the VIEW_COLUMN_FORMAT5 record.  
	 */
	public static short VIEW_COLUMN_FORMAT_SIGNATURE5 = 0x4360;
	
	/*	Flags for COLOR_VALUE */
	
	/** Color space is RGB */
	public static short COLOR_VALUE_FLAGS_ISRGB = 0x0001;
	/** This object has no color */
	public static short COLOR_VALUE_FLAGS_NOCOLOR = 0x0004;
	/** Use system default color, ignore color here */
	public static short COLOR_VALUE_FLAGS_SYSTEMCOLOR = 0x0008;
	/** This color has a gradient color that follows */
	public static short COLOR_VALUE_FLAGS_HASGRADIENT = 0x0010;
	/** upper 4 bits are reserved for application specific use */
	public static short COLOR_VALUE_FLAGS_APPLICATION_MASK = (short) (0xf000 & 0xffff);

	/**  Defined for Yellow Highlighting, (not reserved). */
	public static short COLOR_VALUE_FLAGS_RESERVED1 = (short) (0x8000 & 0xffff);
	/** Defined for Pink Highlighting, (not reserved). */
	public static short COLOR_VALUE_FLAGS_RESERVED2 = 0x4000;
	/** Defined for Blue Highlighting, (not reserved). */
	public static short COLOR_VALUE_FLAGS_RESERVED3 = 0x2000;
	/** Reserved. */
	public static short COLOR_VALUE_FLAGS_RESERVED4 = 0x1000;

	public static short VCF1_M_Sort = 0x0001;
	public static short VCF1_M_SortCategorize = 0x0002;
	public static short VCF1_M_SortDescending = 0x0004;
	public static short VCF1_M_Hidden = 0x0008;
	public static short VCF1_M_Response = 0x0010;
	public static short VCF1_M_HideDetail = 0x0020;
	public static short VCF1_M_Icon = 0x0040;
	public static short VCF1_M_NoResize = 0x0080;
	public static short VCF1_M_ResortAscending = 0x0100;
	public static short VCF1_M_ResortDescending = 0x0200;
	public static short VCF1_M_Twistie = 0x0400;
	public static short VCF1_M_ResortToView = 0x0800;
	public static short VCF1_M_SecondResort = 0x1000;
	public static short VCF1_M_SecondResortDescending = 0x2000;
	/* The following 4 constants are obsolete - see new VCF3_ constants below. */
	public static short VCF1_M_CaseInsensitiveSort = 0x4000;
	public static short VCF1_M_AccentInsensitiveSort = (short) (0x8000 & 0xffff);

	public static short VCF2_M_DisplayAlignment = 0x0003;
	public static short VCF2_M_SubtotalCode = 0x003c;
	public static short VCF2_M_HeaderAlignment = 0x00c0;
	public static short VCF2_M_SortPermute = 0x0100;
	public static short VCF2_M_SecondResortUniqueSort = 0x0200;
	public static short VCF2_M_SecondResortCategorized = 0x0400;
	public static short VCF2_M_SecondResortPermute = 0x0800;
	public static short VCF2_M_SecondResortPermutePair = 0x1000;
	public static short VCF2_M_ShowValuesAsLinks = 0x2000;
	public static short VCF2_M_DisplayReadingOrder = 0x4000;
	public static short VCF2_M_HeaderReadingOrder = (short) (0x8000 & 0xffff);
	
	/* 	Definitions ---------------------------------------------------------- */
	/* The following InfoType codes are defined for REGGetIDInfo */
	/* Note that the Certifier Flag can only exist on a hierarchical ID */
	/* and that Certifier, NotesExpress, and Desktop flags are not */
	/* present in safe copies of ID files */

	public short REGIDGetUSAFlag = 1;
	/* Data structure returned is BOOL */

	public short REGIDGetHierarchicalFlag = 2;
	/* Data structure returned is BOOL */

	public short REGIDGetSafeFlag = 3;
	/* Data structure returned is BOOL */

	public short REGIDGetCertifierFlag = 4;
	/* Data structure returned is BOOL */

	public short REGIDGetNotesExpressFlag = 5;
	/* Data structure returned is BOOL */

	public short REGIDGetDesktopFlag = 6;
	/* Data structure returned is BOOL */

	public short REGIDGetName = 7;
	/* Data structure returned is char xx[MAXUSERNAME] */

	public short REGIDGetPublicKey = 8;
	/* Data structure returned is char xx[xx] */

	public short REGIDGetPrivateKey = 9;
	/* Data structure returned is char xx[xx] */

	public short REGIDGetIntlPublicKey = 10;
	/* Data structure returned is char xx[xx] */

	public short REGIDGetIntlPrivateKey = 11;
	
	/** CompoundText is derived from a file */
	public int COMP_FROM_FILE = 0x00000001;
	/** Insert a line break (0) for each line delimiter found in the input text buffer. This preserves input line breaks. */
	public int COMP_PRESERVE_LINES = 0x00000002;
	/** Create a new paragraph for each line delimiter found in the input text buffer. */
	public int COMP_PARA_LINE = 0x00000004;
	/** Create a new paragraph for each blank line found in the input text buffer.
	 * A blank line is defined as a line containing just a line delimiter (specified by the
	 * pszLineDelim parameter to CompoundTextAddTextExt). */
	public int COMP_PARA_BLANK_LINE = 0x00000008;
	/** A "hint" follows the comment for a document link. If this flag is set,
	 * the pszComment argument points to the comment string, the terminating NUL ('\0'),
	 * the hint string, and the terminating NUL. */
	public int COMP_SERVER_HINT_FOLLOWS = 0x00000010;

	/** (e.g. Times Roman family) */
	public byte FONT_FACE_ROMAN = 0;
	/** (e.g. Helv family) */
	public byte FONT_FACE_SWISS = 1;
	/** (e.g. Monotype Sans WT) */
	public byte FONT_FACE_UNICODE = 2;
	/** (e.g. Arial */
	public byte FONT_FACE_USERINTERFACE = 3;
	/** (e.g. Courier family) */
	public byte FONT_FACE_TYPEWRITER = 4;

	/**	Use this style ID in CompoundTextAddText to continue using the
	same paragraph style as the previous paragraph. */
	public int STYLE_ID_SAMEASPREV = 0xFFFFFFFF;

	/*	Standard colors -- so useful they're available by name. */

	public byte MAX_NOTES_SOLIDCOLORS = 16;

	public byte NOTES_COLOR_BLACK = 0;
	public byte NOTES_COLOR_WHITE = 1;
	public byte NOTES_COLOR_RED = 2;
	public byte NOTES_COLOR_GREEN = 3;
	public byte NOTES_COLOR_BLUE = 4;
	public byte NOTES_COLOR_MAGENTA = 5;
	public byte NOTES_COLOR_YELLOW = 6;
	public byte NOTES_COLOR_CYAN = 7;
	public byte NOTES_COLOR_DKRED = 8;
	public byte NOTES_COLOR_DKGREEN = 9;
	public byte NOTES_COLOR_DKBLUE = 10;
	public byte NOTES_COLOR_DKMAGENTA = 11;
	public byte NOTES_COLOR_DKYELLOW = 12;
	public byte NOTES_COLOR_DKCYAN = 13;
	public byte NOTES_COLOR_GRAY = 14;
	public byte NOTES_COLOR_LTGRAY = 15;

	public byte ISBOLD = 0x01;
	public byte ISITALIC = 0x02;
	public byte	ISUNDERLINE = 0x04;
	public byte ISSTRIKEOUT = 0x08;
	public byte ISSUPER = 0x10;
	public byte ISSUB = 0x20;
	public byte ISEFFECT = (byte) (0x80 & 0xff);		/* Used for implementation of special effect styles */
	public byte ISSHADOW = (byte) (0x80 & 0xff);		/* Used for implementation of special effect styles */
	public byte ISEMBOSS = (byte) (0x90 & 0xff);		/* Used for implementation of special effect styles */
	public byte ISEXTRUDE = (byte) (0xa0 & 0xff);		/* Used for implementation of special effect styles */

	/*	Paragraph justification type codes */

	/** flush left, ragged right */
	public short JUSTIFY_LEFT = 0;
	/** flush right, ragged left */
	public short JUSTIFY_RIGHT = 1;
	/** full block justification */
	public short JUSTIFY_BLOCK = 2;
	/** centered */
	public short JUSTIFY_CENTER = 3;
	/** no line wrapping AT ALL (except hard CRs) */
	public short JUSTIFY_NONE = 4;

	/*	One Inch */

	public int ONEINCH = (20*72);			/* One inch worth of TWIPS */

	/*	Paragraph Flags */

	/** start new page with this par */
	public short PABFLAG_PAGINATE_BEFORE = 0x0001;
	/** don't separate this and next par */
	public short PABFLAG_KEEP_WITH_NEXT = 0x0002;
	/** don't split lines in paragraph */
	public short PABFLAG_KEEP_TOGETHER = 0x0004;
	/** propagate even PAGINATE_BEFORE and KEEP_WITH_NEXT */
	public short PABFLAG_PROPAGATE = 0x0008;
	/** hide paragraph in R/O mode */
	public short PABFLAG_HIDE_RO = 0x0010;
	/** hide paragraph in R/W mode */
	public short PABFLAG_HIDE_RW = 0x0020;
	/** hide paragraph when printing */
	public short PABFLAG_HIDE_PR = 0x0040;
	/** in V4 and below, set if PAB.RightMargin (when nonzero)
	is to have meaning.  Turns out, is set iff para is in
	a table.  Anyway, V5+ no longer use this bit but it
	matters to V4 and below.  V5+ runs with this bit
	zeroed throughout runtime but, for backward
	compatibility, outputs it to disk at Save() time
	per whether paragraph is in a table.  */
	public short PABFLAG_DISPLAY_RM = 0x0080;
	
	/* the pab was saved in V4.	*/
	
	/**	set this bit or the Notes client will assume the pab
		was saved pre-V4 and will thus "link" these bit
		definitions (assign the right one to the left one)
		since preview did not exist pre-V4:<br>
			PABFLAG_HIDE_PV = PABFLAG_HIDE_RO<br>
			PABFLAG_HIDE_PVE = PABFLAG_HIDE_RW */
	public short PABFLAG_HIDE_UNLINK = 0x0100;
	/** hide paragraph when copying/forwarding */
	public short PABFLAG_HIDE_CO = 0x0200;
	/** display paragraph with bullet */
	public short PABFLAG_BULLET = 0x0400;
	/**  use the hide when formula even if there is one. */
	public short PABFLAG_HIDE_IF = 0x0800;
	/** display paragraph with number */
	public short PABFLAG_NUMBEREDLIST = 0x1000;
	/** hide paragraph when previewing*/
	public short PABFLAG_HIDE_PV = 0x2000;
	/** hide paragraph when editing in the preview pane. */
	public short PABFLAG_HIDE_PVE = 0x4000;
	/** hide paragraph from Notes clients */
	public short PABFLAG_HIDE_NOTES = (short) (0x8000	 & 0xffff);

	public short PABFLAG_HIDEBITS = (short) ((PABFLAG_HIDE_RO | PABFLAG_HIDE_RW | PABFLAG_HIDE_CO | PABFLAG_HIDE_PR | PABFLAG_HIDE_PV | PABFLAG_HIDE_PVE | PABFLAG_HIDE_IF | PABFLAG_HIDE_NOTES) & 0xffff);

	public short TABLE_PABFLAGS = (short) (( PABFLAG_KEEP_TOGETHER | PABFLAG_KEEP_WITH_NEXT) & 0xffff);

	/* Extra Paragraph Flags (stored in Flags2 field) */

	public short PABFLAG2_HIDE_WEB = 0x0001;
	public short PABFLAG2_CHECKEDLIST = 0x0002;
	/** PAB.LeftMargin is an offset value. */
	public short PABFLAG2_LM_OFFSET = 0x0004;
	/** PAB.LeftMargin is a percentage value. */
	public short PABFLAG2_LM_PERCENT = 0x0008;
	/** PAB.LeftMargin is an offset value. */
	public short PABFLAG2_FLLM_OFFSET = 0x0010;
	/** PAB.LeftMargin is a percentage value. */
	public short PABFLAG2_FLLM_PERCENT = 0x0020;
	/** PAB.RightMargin is an offset value.   */
	public short PABFLAG2_RM_OFFSET = 0x0040;
	/** PAB.RightMargin is a percentage value.   */
	public short PABFLAG2_RM_PERCENT = 0x0080;
	/** If to use default value instead of PAB.LeftMargin. */
	public short PABFLAG2_LM_DEFAULT = 0x0100;
	/** If to use default value instead of PAB.FirstLineLeftMargin. */
	public short PABFLAG2_FLLM_DEFAULT = 0x0200;
	/** If to use default value instead of PAB.RightMargin. */
	public short PABFLAG2_RM_DEFAULT = 0x0400;
	public short PABFLAG2_CIRCLELIST = 0x0800;	
	public short PABFLAG2_SQUARELIST = 0x1000;	
	public short PABFLAG2_UNCHECKEDLIST = 0x2000;	
	/** set if right to left reading order */
	public short PABFLAG2_BIDI_RTLREADING = 0x4000;
	/** TRUE if Pab needs to Read more Flags*/
	public short PABFLAG2_MORE_FLAGS = (short) (0x8000 & 0xffff);

	public short PABFLAG2_HIDEBITS = PABFLAG2_HIDE_WEB;

	public short PABFLAG2_CHECKLIST = (short) ((PABFLAG2_UNCHECKEDLIST | PABFLAG2_CHECKEDLIST)& 0xffff);

	public short PABFLAG2_MARGIN_DEFAULTS_MASK = (short) (( PABFLAG2_LM_DEFAULT
					| PABFLAG2_RM_DEFAULT
					| PABFLAG2_FLLM_DEFAULT	) & 0xffff);

	public short PABFLAG2_MARGIN_STYLES_MASK = (short) (( PABFLAG2_LM_OFFSET
				| PABFLAG2_LM_PERCENT
				| PABFLAG2_FLLM_OFFSET
				| PABFLAG2_FLLM_PERCENT
				| PABFLAG2_RM_OFFSET
				| PABFLAG2_RM_PERCENT) & 0xffff);
	
	public short PABFLAG2_MARGIN_MASK = (short) (( PABFLAG2_MARGIN_STYLES_MASK | PABFLAG2_MARGIN_DEFAULTS_MASK ) & 0xffff);

	public short PABFLAG2_ROMANUPPERLIST = (short) ((PABFLAG2_CHECKEDLIST | PABFLAG2_CIRCLELIST) & 0xffff);
	public short PABFLAG2_ROMANLOWERLIST = (short) ((PABFLAG2_CHECKEDLIST | PABFLAG2_SQUARELIST) & 0xffff);
	public short PABFLAG2_ALPHAUPPERLIST = (short) ((PABFLAG2_SQUARELIST | PABFLAG2_CIRCLELIST) & 0xffff);
	public short PABFLAG2_ALPHALOWERLIST = (short) ((PABFLAG2_CHECKEDLIST | PABFLAG2_SQUARELIST | PABFLAG2_CIRCLELIST) & 0xffff);

	/*	Table Flags */

	/* Cells grow/shrink to fill window */
	public short TABFLAG_AUTO_CELL_WIDTH = 0x0001;

	/* Cell Flags */

	/* Cell uses background color */
	public byte CELLFLAG_USE_BKGCOLOR = 0x01;

	/*	This DWORD, ExtendPabFlags, extends the PAB structure.<br>
	 * Use the ExtendedPab flags to know what to read next */

	public int EXTENDEDPABFLAGS3 = 0x00000001;	/* If True then need make another read for Flags3 */

	/* 	This DWORD extends the flags and flags 2 in the CDPABDEFINITION record */

	/** True, if Hide when embedded */
	public int PABFLAG3_HIDE_EE = 0x00000001;
	/** True, if hidden from mobile clients */
	public int PABFLAG3_HIDE_MOBILE = 0x00000002;
	/** True if boxes in a layer have set PABFLAG_DISPLAY_RM on pabs */
	public int PABFLAG3_LAYER_USES_DRM = 0x00000004;

	public short	 LONGRECORDLENGTH = 0x0000;
	public short	 WORDRECORDLENGTH = (short) (0xff00 & 0xffff);
	public short	 BYTERECORDLENGTH = 0;		/* High byte contains record length */

	/* Signatures for Composite Records in items of data type COMPOSITE */

	public short SIG_CD_SRC = (81 | WORDRECORDLENGTH);
	public short SIG_CD_IMAGEHEADER2 = (82 | BYTERECORDLENGTH );
	public short SIG_CD_PDEF_MAIN = (83 | WORDRECORDLENGTH ) /* Signatures for items used in Property Broker definitions. LI 3925.04 */;
	public short SIG_CD_PDEF_TYPE = (84 | WORDRECORDLENGTH );
	public short SIG_CD_PDEF_PROPERTY = (85 | WORDRECORDLENGTH );
	public short SIG_CD_PDEF_ACTION = (86 | WORDRECORDLENGTH );
	public short SIG_CD_TABLECELL_DATAFLAGS = (87 | BYTERECORDLENGTH);
	public short SIG_CD_EMBEDDEDCONTACTLIST = (88 | WORDRECORDLENGTH);
	public short SIG_CD_IGNORE = (89 | BYTERECORDLENGTH);
	public short SIG_CD_TABLECELL_HREF2 = (90 | WORDRECORDLENGTH);
	public short SIG_CD_HREFBORDER = (91 | WORDRECORDLENGTH);
	public short SIG_CD_TABLEDATAEXTENSION = (92 | WORDRECORDLENGTH);
	public short SIG_CD_EMBEDDEDCALCTL = (93 | WORDRECORDLENGTH);
	public short SIG_CD_ACTIONEXT = (94 | WORDRECORDLENGTH);
	public short SIG_CD_EVENT_LANGUAGE_ENTRY = (95 | WORDRECORDLENGTH);
	public short SIG_CD_FILESEGMENT = (96 | LONGRECORDLENGTH);
	public short SIG_CD_FILEHEADER = (97 | LONGRECORDLENGTH);
	public short SIG_CD_DATAFLAGS = (98 | BYTERECORDLENGTH);

	public short SIG_CD_BACKGROUNDPROPERTIES = (99 | BYTERECORDLENGTH);

	public short SIG_CD_EMBEDEXTRA_INFO = (100 | WORDRECORDLENGTH);
	public short SIG_CD_CLIENT_BLOBPART = (101 | WORDRECORDLENGTH);
	public short SIG_CD_CLIENT_EVENT = (102 | WORDRECORDLENGTH);
	public short SIG_CD_BORDERINFO_HS = (103 | WORDRECORDLENGTH);
	public short SIG_CD_LARGE_PARAGRAPH = (104 | WORDRECORDLENGTH);
	public short SIG_CD_EXT_EMBEDDEDSCHED = (105 | WORDRECORDLENGTH);
	public short SIG_CD_BOXSIZE = (106 | BYTERECORDLENGTH);
	public short SIG_CD_POSITIONING = (107 | BYTERECORDLENGTH);
	public short SIG_CD_LAYER = (108 | BYTERECORDLENGTH);
	public short SIG_CD_DECSFIELD = (109 | WORDRECORDLENGTH);
	public short SIG_CD_SPAN_END = (110 | BYTERECORDLENGTH)	/* Span End */;
	public short SIG_CD_SPAN_BEGIN = (111 | BYTERECORDLENGTH)	/* Span Begin */;
	public short SIG_CD_TEXTPROPERTIESTABLE = (112 | WORDRECORDLENGTH)	/* Text Properties Table */;
									  
	public short SIG_CD_HREF2 = (113 | WORDRECORDLENGTH);
	public short SIG_CD_BACKGROUNDCOLOR = (114 | BYTERECORDLENGTH);
	public short SIG_CD_INLINE = (115 | WORDRECORDLENGTH);
	public short SIG_CD_V6HOTSPOTBEGIN_CONTINUATION = (116 | WORDRECORDLENGTH);
	public short SIG_CD_TARGET_DBLCLK = (117 | WORDRECORDLENGTH);
	public short SIG_CD_CAPTION = (118 | WORDRECORDLENGTH);
	public short SIG_CD_LINKCOLORS = (119 | WORDRECORDLENGTH);
	public short SIG_CD_TABLECELL_HREF = (120 | WORDRECORDLENGTH);
	public short SIG_CD_ACTIONBAREXT = (121 | WORDRECORDLENGTH);
	public short SIG_CD_IDNAME = (122 | WORDRECORDLENGTH);
	public short SIG_CD_TABLECELL_IDNAME = (123 | WORDRECORDLENGTH);
	public short SIG_CD_IMAGESEGMENT = (124 | LONGRECORDLENGTH);
	public short SIG_CD_IMAGEHEADER = (125 | LONGRECORDLENGTH);
	public short SIG_CD_V5HOTSPOTBEGIN = (126 | WORDRECORDLENGTH);
	public short SIG_CD_V5HOTSPOTEND = (127 | BYTERECORDLENGTH);
	public short SIG_CD_TEXTPROPERTY = (128 | WORDRECORDLENGTH);
	public short SIG_CD_PARAGRAPH = (129 | BYTERECORDLENGTH);
	public short SIG_CD_PABDEFINITION = (130 | WORDRECORDLENGTH);
	public short SIG_CD_PABREFERENCE = (131 | BYTERECORDLENGTH);
	public short SIG_CD_TEXT = (133 | WORDRECORDLENGTH);
	public short SIG_CD_XML = (134 | WORDRECORDLENGTH);
	public short SIG_CD_HEADER = (142 | WORDRECORDLENGTH);
	public short SIG_CD_LINKEXPORT2 = (146 | WORDRECORDLENGTH);
	public short SIG_CD_BITMAPHEADER = (149 | LONGRECORDLENGTH);
	public short SIG_CD_BITMAPSEGMENT = (150 | LONGRECORDLENGTH);
	public short SIG_CD_COLORTABLE = (151 | LONGRECORDLENGTH);
	public short SIG_CD_GRAPHIC = (153 | LONGRECORDLENGTH);
	public short SIG_CD_PMMETASEG = (154 | LONGRECORDLENGTH);
	public short SIG_CD_WINMETASEG = (155 | LONGRECORDLENGTH);
	public short SIG_CD_MACMETASEG = (156 | LONGRECORDLENGTH);
	public short SIG_CD_CGMMETA = (157 | LONGRECORDLENGTH);
	public short SIG_CD_PMMETAHEADER = (158 | LONGRECORDLENGTH);
	public short SIG_CD_WINMETAHEADER = (159 | LONGRECORDLENGTH);
	public short SIG_CD_MACMETAHEADER = (160 | LONGRECORDLENGTH);
	public short SIG_CD_TABLEBEGIN = (163 | BYTERECORDLENGTH);
	public short SIG_CD_TABLECELL = (164 | BYTERECORDLENGTH);
	public short SIG_CD_TABLEEND = (165 | BYTERECORDLENGTH);
	public short SIG_CD_STYLENAME = (166 | BYTERECORDLENGTH);
	public short SIG_CD_STORAGELINK = (196 | WORDRECORDLENGTH);
	public short SIG_CD_TRANSPARENTTABLE = (197 | LONGRECORDLENGTH);
	public short SIG_CD_HORIZONTALRULE = (201 | WORDRECORDLENGTH);
	public short SIG_CD_ALTTEXT = (202 | WORDRECORDLENGTH);
	public short SIG_CD_ANCHOR = (203 | WORDRECORDLENGTH);
	public short SIG_CD_HTMLBEGIN = (204 | WORDRECORDLENGTH);
	public short SIG_CD_HTMLEND = (205 | WORDRECORDLENGTH);
	public short SIG_CD_HTMLFORMULA = (206 | WORDRECORDLENGTH);
	public short SIG_CD_NESTEDTABLEBEGIN = (207 | BYTERECORDLENGTH);
	public short SIG_CD_NESTEDTABLECELL = (208 | BYTERECORDLENGTH);
	public short SIG_CD_NESTEDTABLEEND = (209 | BYTERECORDLENGTH);
	public short SIG_CD_COLOR = (210 | BYTERECORDLENGTH);
	public short SIG_CD_TABLECELL_COLOR = (211 | BYTERECORDLENGTH);

	/* 212 thru 219 reserved for BSIG'S - don't use until we hit 255 */

	public short SIG_CD_BLOBPART = (220 | WORDRECORDLENGTH);
	public short SIG_CD_BEGIN = (221 | BYTERECORDLENGTH);
	public short SIG_CD_END = (222 | BYTERECORDLENGTH);
	public short SIG_CD_VERTICALALIGN = (223 | BYTERECORDLENGTH);
	public short SIG_CD_FLOATPOSITION = (224 | BYTERECORDLENGTH);

	public short SIG_CD_TIMERINFO = (225 | BYTERECORDLENGTH);
	public short SIG_CD_TABLEROWHEIGHT = (226 | BYTERECORDLENGTH);
	public short SIG_CD_TABLELABEL = (227 | WORDRECORDLENGTH);
	public short SIG_CD_BIDI_TEXT = (228 | WORDRECORDLENGTH);
	public short SIG_CD_BIDI_TEXTEFFECT = (229 | WORDRECORDLENGTH);
	public short SIG_CD_REGIONBEGIN = (230 | WORDRECORDLENGTH);
	public short SIG_CD_REGIONEND = (231 | WORDRECORDLENGTH);
	public short SIG_CD_TRANSITION = (232 | WORDRECORDLENGTH);
	public short SIG_CD_FIELDHINT = (233 | WORDRECORDLENGTH);
	public short SIG_CD_PLACEHOLDER = (234 | WORDRECORDLENGTH);
	public short SIG_CD_HTMLNAME = (235 | WORDRECORDLENGTH);
	public short SIG_CD_EMBEDDEDOUTLINE = (236 | WORDRECORDLENGTH);
	public short SIG_CD_EMBEDDEDVIEW = (237 | WORDRECORDLENGTH);
	public short SIG_CD_CELLBACKGROUNDDATA = (238 | WORDRECORDLENGTH);

	/* Signatures for Frameset CD records */
	public short SIG_CD_FRAMESETHEADER = (239 | WORDRECORDLENGTH);
	public short SIG_CD_FRAMESET = (240 | WORDRECORDLENGTH);
	public short SIG_CD_FRAME = (241 | WORDRECORDLENGTH);
	/* Signature for Target Frame info on a link	*/
	public short SIG_CD_TARGET = (242 | WORDRECORDLENGTH);

	public short SIG_CD_NATIVEIMAGE = (243 | LONGRECORDLENGTH);
	public short SIG_CD_MAPELEMENT = (244 | WORDRECORDLENGTH);
	public short SIG_CD_AREAELEMENT = (245 | WORDRECORDLENGTH);
	public short SIG_CD_HREF = (246 | WORDRECORDLENGTH);
	public short SIG_CD_EMBEDDEDCTL = (247 | WORDRECORDLENGTH);
	public short SIG_CD_HTML_ALTTEXT = (248 | WORDRECORDLENGTH);
	public short SIG_CD_EVENT = (249 | WORDRECORDLENGTH);
	public short SIG_CD_PRETABLEBEGIN = (251 | WORDRECORDLENGTH);
	public short SIG_CD_BORDERINFO = (252 | WORDRECORDLENGTH);
	public short SIG_CD_EMBEDDEDSCHEDCTL = (253 | WORDRECORDLENGTH);

	public short SIG_CD_EXT2_FIELD = (254 | WORDRECORDLENGTH)	/* Currency, numeric, and data/time extensions */;
	public short SIG_CD_EMBEDDEDEDITCTL = (255 | WORDRECORDLENGTH);

	/* Can not go beyond 255.  However, there may be room at the beginning of 
		the list.  Check there.   */

	/* Signatures for Composite Records that are reserved internal records, */
	/* whose format may change between releases. */

	public short SIG_CD_DOCUMENT_PRE_26 = (128 | BYTERECORDLENGTH);
	public short SIG_CD_FIELD_PRE_36 = (132 | WORDRECORDLENGTH);
	public short SIG_CD_FIELD = (138 | WORDRECORDLENGTH);
	public short SIG_CD_DOCUMENT = (134 | BYTERECORDLENGTH);
	public short SIG_CD_METAFILE = (135 | WORDRECORDLENGTH);
	public short SIG_CD_BITMAP = (136 | WORDRECORDLENGTH);
	public short SIG_CD_FONTTABLE = (139 | WORDRECORDLENGTH);
	public short SIG_CD_LINK = (140 | BYTERECORDLENGTH);
	public short SIG_CD_LINKEXPORT = (141 | BYTERECORDLENGTH);
	public short SIG_CD_KEYWORD = (143 | WORDRECORDLENGTH);
	public short SIG_CD_LINK2 = (145 | WORDRECORDLENGTH);
	public short SIG_CD_CGM = (147 | WORDRECORDLENGTH);
	public short SIG_CD_TIFF = (148 | LONGRECORDLENGTH);
	public short SIG_CD_PATTERNTABLE = (152 | LONGRECORDLENGTH);
	public short SIG_CD_DDEBEGIN = (161 | WORDRECORDLENGTH);
	public short SIG_CD_DDEEND = (162 | WORDRECORDLENGTH);
	public short SIG_CD_OLEBEGIN = (167 | WORDRECORDLENGTH);
	public short SIG_CD_OLEEND = (168 | WORDRECORDLENGTH);
	public short SIG_CD_HOTSPOTBEGIN = (169 | WORDRECORDLENGTH);
	public short SIG_CD_HOTSPOTEND = (170 | BYTERECORDLENGTH);
	public short SIG_CD_BUTTON = (171 | WORDRECORDLENGTH);
	public short SIG_CD_BAR = (172 | WORDRECORDLENGTH);
	public short SIG_CD_V4HOTSPOTBEGIN = (173 | WORDRECORDLENGTH);
	public short SIG_CD_V4HOTSPOTEND = (174 | BYTERECORDLENGTH);
	public short SIG_CD_EXT_FIELD = (176 | WORDRECORDLENGTH);
	public short SIG_CD_LSOBJECT = (177 | WORDRECORDLENGTH)/* Compiled LS code*/;
	public short SIG_CD_HTMLHEADER = (178 | WORDRECORDLENGTH) /* Raw HTML */;
	public short SIG_CD_HTMLSEGMENT = (179 | WORDRECORDLENGTH);
	public short SIG_CD_OLEOBJPH = (180 | WORDRECORDLENGTH);
	public short SIG_CD_MAPIBINARY = (181 | WORDRECORDLENGTH);
	public short SIG_CD_LAYOUT = (183 | BYTERECORDLENGTH);
	public short SIG_CD_LAYOUTTEXT = (184 | BYTERECORDLENGTH);
	public short SIG_CD_LAYOUTEND = (185 | BYTERECORDLENGTH);
	public short SIG_CD_LAYOUTFIELD = (186 | BYTERECORDLENGTH);
	public short SIG_CD_PABHIDE = (187 | WORDRECORDLENGTH);
	public short SIG_CD_PABFORMREF = (188 | BYTERECORDLENGTH);
	public short SIG_CD_ACTIONBAR = (189 | BYTERECORDLENGTH);
	public short SIG_CD_ACTION = (190 | WORDRECORDLENGTH);

	public short SIG_CD_DOCAUTOLAUNCH = (191 | WORDRECORDLENGTH);
	public short SIG_CD_LAYOUTGRAPHIC = (192 | BYTERECORDLENGTH);
	public short SIG_CD_OLEOBJINFO = (193 | WORDRECORDLENGTH);
	public short SIG_CD_LAYOUTBUTTON = (194 | BYTERECORDLENGTH);
	public short SIG_CD_TEXTEFFECT = (195 | WORDRECORDLENGTH);


	public short SIG_ACTION_HEADER = (129 | BYTERECORDLENGTH);
	public short SIG_ACTION_MODIFYFIELD = (130 | WORDRECORDLENGTH);
	public short SIG_ACTION_REPLY = (131 | WORDRECORDLENGTH);
	public short SIG_ACTION_FORMULA = (132 | WORDRECORDLENGTH);
	public short SIG_ACTION_LOTUSSCRIPT = (133 | WORDRECORDLENGTH);
	public short SIG_ACTION_SENDMAIL = (134 | WORDRECORDLENGTH);
	public short SIG_ACTION_DBCOPY = (135 | WORDRECORDLENGTH);
	public short SIG_ACTION_DELETE = (136 | BYTERECORDLENGTH);
	public short SIG_ACTION_BYFORM = (137 | WORDRECORDLENGTH);
	public short SIG_ACTION_MARKREAD = (138 | BYTERECORDLENGTH);
	public short SIG_ACTION_MARKUNREAD = (139 | BYTERECORDLENGTH);
	public short SIG_ACTION_MOVETOFOLDER = (140 | WORDRECORDLENGTH);
	public short SIG_ACTION_COPYTOFOLDER = (141 | WORDRECORDLENGTH);
	public short SIG_ACTION_REMOVEFROMFOLDER = (142 | WORDRECORDLENGTH);
	public short SIG_ACTION_NEWSLETTER = (143 | WORDRECORDLENGTH);
	public short SIG_ACTION_RUNAGENT = (144 | WORDRECORDLENGTH);
	public short SIG_ACTION_SENDDOCUMENT = (145 | BYTERECORDLENGTH);
	public short SIG_ACTION_FORMULAONLY = (146 | WORDRECORDLENGTH);
	public short SIG_ACTION_JAVAAGENT = (147 | WORDRECORDLENGTH);
	public short SIG_ACTION_JAVA = (148 | WORDRECORDLENGTH);


	/* Signatures for items of type TYPE_VIEWMAP_DATASET */

	public short SIG_VIEWMAP_DATASET = (87 | WORDRECORDLENGTH);

	/* Signatures for items of type TYPE_VIEWMAP */

	public short SIG_CD_VMHEADER = (175 | BYTERECORDLENGTH);
	public short SIG_CD_VMBITMAP = (176 | BYTERECORDLENGTH);
	public short SIG_CD_VMRECT = (177 | BYTERECORDLENGTH);
	public short SIG_CD_VMPOLYGON_BYTE = (178 | BYTERECORDLENGTH);
	public short SIG_CD_VMPOLYLINE_BYTE = (179 | BYTERECORDLENGTH);
	public short SIG_CD_VMREGION = (180 | BYTERECORDLENGTH);
	public short SIG_CD_VMACTION = (181 | BYTERECORDLENGTH);
	public short SIG_CD_VMELLIPSE = (182 | BYTERECORDLENGTH);
	public short SIG_CD_VMSMALLTEXTBOX = (183 | BYTERECORDLENGTH);
	public short SIG_CD_VMRNDRECT = (184 | BYTERECORDLENGTH);
	public short SIG_CD_VMBUTTON = (185 | BYTERECORDLENGTH);
	public short SIG_CD_VMACTION_2 = (186 | WORDRECORDLENGTH);
	public short SIG_CD_VMTEXTBOX = (187 | WORDRECORDLENGTH);
	public short SIG_CD_VMPOLYGON = (188 | WORDRECORDLENGTH);
	public short SIG_CD_VMPOLYLINE = (189 | WORDRECORDLENGTH);
	public short SIG_CD_VMPOLYRGN = (190 | WORDRECORDLENGTH);
	public short SIG_CD_VMCIRCLE = (191 | BYTERECORDLENGTH);
	public short SIG_CD_VMPOLYRGN_BYTE = (192 | BYTERECORDLENGTH);

	/* Signatures for alternate CD sequences*/
	public short SIG_CD_ALTERNATEBEGIN = (198 | WORDRECORDLENGTH);
	public short SIG_CD_ALTERNATEEND = (199 | BYTERECORDLENGTH);

	public short SIG_CD_OLERTMARKER = (200 | WORDRECORDLENGTH);

	public short CDIMAGETYPE_GIF = 1;
	public short CDIMAGETYPE_JPEG = 2;
	public short CDIMAGETYPE_BMP = 3;
	public short CDIMAGETYPE_PNG = 4;
	/* Images not supported in Notes rich text, but which can be useful for MIME/HTML external files */
	public short CDIMAGETYPE_SVG = 5;
	public short CDIMAGETYPE_TIF = 6;
	public short CDIMAGETYPE_PDF = 7;
	
	/* Version control of graphic header */
	public byte CDGRAPHIC_VERSION1 = 0;		/* Created by Notes version 2 */
	public byte CDGRAPHIC_VERSION2 = 1;		/* Created by Notes version 3 */
	public byte CDGRAPHIC_VERSION3 = 2;		/* Created by Notes version 4.5 */

	/*	The following flag indicates that the DestSize field contains
	pixel values instead of twips. */

	public byte CDGRAPHIC_FLAG_DESTSIZE_IS_PIXELS = 0x01;
	public byte CDGRAPHIC_FLAG_SPANSLINES = 0x02;

	/*	HOTSPOT_RUN Types */

	public short HOTSPOTREC_TYPE_POPUP = 1;
	public short HOTSPOTREC_TYPE_HOTREGION = 2;
	public short HOTSPOTREC_TYPE_BUTTON = 3;
	public short HOTSPOTREC_TYPE_FILE = 4;
	public short HOTSPOTREC_TYPE_SECTION = 7;
	public short HOTSPOTREC_TYPE_ANY = 8;
	public short HOTSPOTREC_TYPE_HOTLINK = 11;
	public short HOTSPOTREC_TYPE_BUNDLE = 12;
	public short HOTSPOTREC_TYPE_V4_SECTION = 13;
	public short HOTSPOTREC_TYPE_SUBFORM = 14;
	public short HOTSPOTREC_TYPE_ACTIVEOBJECT = 15;
	public short HOTSPOTREC_TYPE_OLERICHTEXT = 18;
	public short HOTSPOTREC_TYPE_EMBEDDEDVIEW = 19;	/* embedded view */
	public short HOTSPOTREC_TYPE_EMBEDDEDFPANE = 20;	/* embedded folder pane */
	public short HOTSPOTREC_TYPE_EMBEDDEDNAV = 21;	/* embedded navigator */
	public short HOTSPOTREC_TYPE_MOUSEOVER = 22;
	public short HOTSPOTREC_TYPE_FILEUPLOAD = 24;	/* file upload placeholder */
	public short HOTSPOTREC_TYPE_EMBEDDEDOUTLINE = 27; 	/* embedded outline */
	public short HOTSPOTREC_TYPE_EMBEDDEDCTL = 28;	/* embedded control window */
	public short HOTSPOTREC_TYPE_EMBEDDEDCALENDARCTL = 30;	/* embedded calendar control (date picker) */
	public short HOTSPOTREC_TYPE_EMBEDDEDSCHEDCTL = 31;	/* embedded scheduling control */
	public short HOTSPOTREC_TYPE_RCLINK = 32;	/* Not a new type, but renamed for V5 terms*/
	public short HOTSPOTREC_TYPE_EMBEDDEDEDITCTL = 34;	/* embedded editor control */
	public short HOTSPOTREC_TYPE_CONTACTLISTCTL = 36;	/* Embeddeble buddy list */

	public int HOTSPOTREC_RUNFLAG_BEGIN = 0x00000001;
	public int HOTSPOTREC_RUNFLAG_END = 0x00000002;
	public int HOTSPOTREC_RUNFLAG_BOX = 0x00000004;
	public int HOTSPOTREC_RUNFLAG_NOBORDER = 0x00000008;
	public int HOTSPOTREC_RUNFLAG_FORMULA = 0x00000010;	/*	Popup is a formula, not text. */
	public int HOTSPOTREC_RUNFLAG_MOVIE = 0x00000020; /*	File is a QuickTime movie. */
	public int HOTSPOTREC_RUNFLAG_IGNORE = 0x00000040; /*	Run is for backward compatibility
															(i.e. ignore the run)
														*/
	public int HOTSPOTREC_RUNFLAG_ACTION = 0x00000080;	/*	Hot region executes a canned action	*/
	public int HOTSPOTREC_RUNFLAG_SCRIPT = 0x00000100;	/*	Hot region executes a script.	*/
	public int HOTSPOTREC_RUNFLAG_INOTES = 0x00001000;
	public int HOTSPOTREC_RUNFLAG_ISMAP = 0x00002000;
	public int HOTSPOTREC_RUNFLAG_INOTES_AUTO = 0x00004000;
	public int HOTSPOTREC_RUNFLAG_ISMAP_INPUT = 0x00008000;

	public int HOTSPOTREC_RUNFLAG_SIGNED = 0x00010000;
	public int HOTSPOTREC_RUNFLAG_ANCHOR = 0x00020000;
	public int HOTSPOTREC_RUNFLAG_COMPUTED = 0x00040000;	/*	Used in conjunction
															with computed hotspots.
														*/
	public int HOTSPOTREC_RUNFLAG_TEMPLATE = 0x00080000;	/*	used in conjunction
															with embedded navigator
															panes.
														*/
	public int HOTSPOTREC_RUNFLAG_HIGHLIGHT = 0x00100000;
	public int HOTSPOTREC_RUNFLAG_EXTACTION = 0x00200000; /*  Hot region executes an extended action */
	public int HOTSPOTREC_RUNFLAG_NAMEDELEM = 0x00400000;	/*	Hot link to a named element */

	/*	Allow R6 dual action type buttons, e.g. client LotusScript, web JS */
	public int HOTSPOTREC_RUNFLAG_WEBJAVASCRIPT = 0x00800000;

	public int HOTSPOTREC_RUNFLAG_ODSMASK = 0x00FFFFFC;	/*	Mask for bits stored on disk*/

	/*	CDCAPTION - Text to display with an object (e.g., a graphic) */

	public byte CAPTION_POSITION_BELOW_CENTER = 0;	/*	Centered below object */
	public byte CAPTION_POSITION_MIDDLE_CENTER = 1;	/*	Centered on object */

	/** Force operation, even if destination "up to date" */
	public int DESIGN_FORCE = 0x00000001;
	/** Return an error if the template is not found */
	public int DESIGN_ERR_TMPL_NOT_FOUND = 0x00000008	;

	/*	NSF File Information Buffer size.  This buffer is defined to contain
	Text (host format) that is NULL-TERMINATED.  This is the ONLY null-terminated
	field in all of NSF. */

	public int NSF_INFO_SIZE = 128;

	/*	Define argument to NSFDbInfoParse/Modify to manipulate components from DbInfo */

	/** database title */
	public short INFOPARSE_TITLE = 0;
	/** database categories */
	public short INFOPARSE_CATEGORIES = 1;
	/** template name (for a design template database) */
	public short INFOPARSE_CLASS	 = 2;
	/** inherited template name (for a database that inherited its design from a design template) */
	public short INFOPARSE_DESIGN_CLASS = 3;

	/*	Define NSF DB open modes */

	/** hDB refers to a normal database file */
	public short DB_LOADED = 1;
	/** hDB refers to a "directory" and not a file */
	public short DB_DIRECTORY = 2;

	// Flags that control behavior of the calendar APIs - Used when APIS take iCalendar input to modify calendar data
	public int CAL_WRITE_COMPLETE_REPLACE = 0x00000001;		// Used when APIs modify entry data via CalUpdateEntry.
																	// This flag means that NO data is preserved from the original entry and the
																	// resulting entry is 100% a product of the iCalendar passed in.
																	// NOTE: When this flag is NOT used, some content may be preserved during an
																	// update if that particular content was not included in the iCalendar input.
																	// This includes:
																	// Body
																	// Attachments
																	// Custom data properties as specified in $CSCopyItems

	public int CAL_WRITE_DISABLE_IMPLICIT_SCHEDULING = 0x00000002;		// Used when APIs create or modify calendar entries where the organizer is the mailfile owner.
																	// When a calendar entry is modified with CAL_WRITE_DISABLE_IMPLICIT_SCHEDULING set, no notices are
																	// sent (invites, updates, reschedules, cancels, etc)
																	// Note: This is not intended for cases where you are saving a meeting as a draft (since there is currently
																	// not a capability to then send it later.  It will also not allow some notices to go out but other notices
																	// not to go out (such as, send invites to added invitees but dont send updates to existing invitees).
																	// Rather, this is targeted at callers that prefer to be responsible for sending out notices themselves through
																	// a separate mechanism

	public int CAL_WRITE_IGNORE_VERIFY_DB = 0x00000004;		// Used when APIs create or modify entries on the calendar
																	// This will allow creation/modification of calendar entries, even if the database is not a mailfile

	public int CAL_WRITE_USE_ALARM_DEFAULTS = 0x00000008;		// By default, alarms will be created on calendar entries based on VALARM content of iCalendar input.  Use of
																	// this flag will disregard VALARM information in the iCalendar and use the user's default alarm settings for
																	// created or updated entries.

	// Flags that control behavior of the calendar APIs - Used when opening a note handle for calendar data
	public int CAL_NOTEOPEN_HANDLE_NOSPLIT = 0x00000001;		// Used when getting a handle via CalOpenNoteHandle (Handy for read-only cases)
																	// When a specific instance of a recurring entry is requested, the underlying note may represent multiple
																	// instances.  Default behavior makes appropriate modifications so that the returned handle represents
																	// a single instance (but this might cause notes to be created or modified as a side effect).
																	// Using CAL_NOTEOPEN_HANDLE_NOSPLIT will bypass any note creations or modifications and return a note handle
																	// that may represent more than a single instance on the calendar.

	// Flags that control behavior of the calendar APIs that return iCalendar data for an entry or notice
	public int CAL_READ_HIDE_X_LOTUS = 0x00000001;			// Used when APIs generate iCalendar
																	// By default, some X-LOTUS properties and parameters will be included in iCalendar data
																	// returned by these APIs.  CAL_READ_HIDE_X_LOTUS causes all X-LOTUS properties and
																	// parameters to be removed from the generated iCalendar data.
																	// Note: This overrides CAL_READ_INCLUDE_X_LOTUS

	public int CAL_READ_INCLUDE_X_LOTUS = 0x00000002;			// Used when APIs generate iCalendar
																	// Include all Lotus specific properties like X-LOTUS-UPDATE-SEQ, X-LOTUS-UPDATE_WISL, etc
																	// in the generated iCalendar data.
																	// These properties are NOT included by default in any iCalendar data returned by the APIs.
																	// Caution: Unless the caller knows how to use these it can be dangerous since their presence will
																	// be honored and can cause issues if not updated properly.
																	// Ignored if CAL_READ_HIDE_X_LOTUS is also specified.

	public int CAL_READ_SKIP_RESPONSE_DATA = 0x00000004;			// RESERVED: This functionality is not currently in plan
																	// When generating ATTENDEE info in CalReadEntry, determine and populate response
																	// Status (which might be a performance hit)

	public int READ_RANGE_MASK_DTSTART = 0x00000001;
	public int READ_RANGE_MASK_DTEND = 0x00000002;
	public int READ_RANGE_MASK_DTSTAMP = 0x00000004;
	public int READ_RANGE_MASK_SUMMARY = 0x00000008;
	public int READ_RANGE_MASK_CLASS = 0x00000010;
	public int READ_RANGE_MASK_PRIORITY	= 0x00000020;
	public int READ_RANGE_MASK_RECURRENCE_ID	 = 0x00000040;
	public int READ_RANGE_MASK_SEQUENCE = 0x00000080;
	public int READ_RANGE_MASK_LOCATION = 0x00000100;
	public int READ_RANGE_MASK_TRANSP = 0x00000200;
	public int READ_RANGE_MASK_CATEGORY = 0x00000400;
	public int READ_RANGE_MASK_APPTTYPE = 0x00000800;
	public int READ_RANGE_MASK_NOTICETYPE = 0x00001000;
	public int READ_RANGE_MASK_STATUS = 0x00002000;
	public int READ_RANGE_MASK_ONLINE_URL = 0x00004000;		// Includes online meeting URL as well as any online meeting password or conf ID
	public int READ_RANGE_MASK_NOTESORGANIZER = 0x00008000;		// Note: For performance reasons, the organizer may not be stored in ORGANIZER but rather in
																// X-LOTUS-ORGANIZER to avoid lookups necessary to get the internet address.
	public int READ_RANGE_MASK_NOTESROOM = 0x00010000;		// Note: For performance reasons, the organizer may not be stored in PARTICIPANT but rather in
																// X-LOTUS-ROOM to avoid lookups necessary to get the internet address.
	public int READ_RANGE_MASK_ALARM = 0x00020000;		// Output alarm information for this entry


	/* Non-default values - only harvested if requested in dwReturnMaskExt by CalReadRange.*/
	public int READ_RANGE_MASK2_HASATTACH = 0x00000001;		// X-LOTUS-HASATTACH is set to 1 if there are any file attachments for this entry
	public int READ_RANGE_MASK2_UNID = 0x00000002;		// X-LOTUS-UNID will always be set for notices (as it is used as the identifier for
																// a notice), but setting this flag will also set X-LOTUS-UNID for calendar entries,
																// where this will be set with the UNID of the note that currently contains this
																// instance (can be used to construct a URL to open the instance in Notes, for instance)

	/* CAL_PROCESS_### values are used to define the action taken by CalNoticeAction and CalEntryAction */
	public int CAL_PROCESS_ACCEPT = 0x00000002;	/* Accept (regardless of conflicts)
														 * For Information update notices or confirm notices, this will apply the changes to the relavent
														 * calendar entry.
														 * Used by the organizer to accept a counter proposal.
														 */
	public int CAL_PROCESS_TENTATIVE = 0x00000004;	/* Tentatively accept (regardless of conflicts) */
	public int CAL_PROCESS_DECLINE = 0x00000008;	/* Decline 
														 * Can be used by the organizer to decline a counter if done from a counter notice */
	public int CAL_PROCESS_DELEGATE = 0x00000010;	/* Delegate to EXT_CALACTION_DATA::pszDelegateTo */
	public int CAL_PROCESS_COUNTER = 0x00000020;	/* Counter to a new time (requires populating EXT_CALACTION_DATA::ptdChangeTo values) */
	public int CAL_PROCESS_REQUESTINFO = 0x00000040;	/* Request updated information from the organizer for this meeting.
														 * Also used by the organizer to respond to a request for updated info.
														 */
	public int CAL_PROCESS_REMOVECANCEL = 0x00000080;	/* This will process a cancelation notice, removing the meeting from the calendar */
	public int CAL_PROCESS_DELETE = 0x00000100;	/* This will physically delete a meeting from the calendar.  This will NOT send notices out */
	public int CAL_PROCESS_SMARTREMOVE = 0x00000200;	/* This will remove the meeting or appointment from the calendar and send notices if 
														 * necessary.
														 * It is treated as a CAL_PROCESS_CANCEL if the entry is a meeting the mailfile 
														 * owner is the organizer of.  
														 * It is treated as a CAL_PROCESS_DECLINE if the entry is a meeting that the mailfile 
														 * owner is not the organizer of except when the entry is a broadcast.  In that case it
														 * is treated as a CAL_PROCESS_DELETE.
														 * It is treated as a CAL_PROCESS_DELETE if the entry is a non-meeting */
	public int CAL_PROCESS_CANCEL = 0x00000400;	/* This will cancel a meeting that the mailfile owner is the organizer of */


	public int CAL_PROCESS_UPDATEINVITEES = 0x00002000;	/* This will update the invitee lists on the specified entry (or entries) to include or remove
														 * those users specified in lists contained in the EXT_CALACTION_DATA::pAddNames and 
														 * EXT_CALACTION_DATA::pRemoveNames values */

	/* Flags that control behavior of the CalNoticeAction and CalEntryAction calendar APIs
	 * CAL_ACTION _### values are used to provide additional processing control to some actions taken by CalNoticeAction and CalEntryAction */

	public int CAL_ACTION_DO_OVERWRITE_CHECK = 0x00000001;	/* Indicates that a check should be performed when processing the action to determine 
														 		* if an overwrite of invitee changes to the entry will occur. */
	public int CAL_ACTION_UPDATE_ALL_PARTICIPANTS = 0x00000002;	/* New in 9.01 release.  Used to indicate that current entry participants should be notified of changes
																 * to the participant list in addition to those being added or removed. */

	/* Range values for actions on recurring entries */
	public int RANGE_REPEAT_CURRENT = 0;		/* Modifying just this instance */
	public int RANGE_REPEAT_ALL = 1;		/* Modifying all instances */
	public int RANGE_REPEAT_PREV = 2;		/* Modifying current + previous */
	public int RANGE_REPEAT_FUT = 3;		/* Modifying current + future */
	
	public short MIME_PART_VERSION = 2;
	
	/** Mime part has boundary. */
	public int MIME_PART_HAS_BOUNDARY = 0x00000001;
	/** Mime part has headers. */
	public int MIME_PART_HAS_HEADERS = 0x00000002;
	/** Mime part has body in database object. */
	public int MIME_PART_BODY_IN_DBOBJECT = 0x00000004;
	/** Mime part has shared database object. Used only with MIME_PART_BODY_IN_DBOBJECT. */
	public int MIME_PART_SHARED_DBOBJECT = 0x00000008;	/*	Used only with MIME_PART_BODY_IN_DBOBJECT. */
	/** Skip for conversion. */
	public int MIME_PART_SKIP_FOR_CONVERSION = 0x00000010;	/* only used during MIME->CD conversion */

	//The mime part type cPartType within the MIME_PART structure.
	
	/** Mime part type is a prolog. */
	public byte MIME_PART_PROLOG = 1;
	/** Mime part type is a body. */
	public byte MIME_PART_BODY = 2;
	/** Mime part type is a epilog. */
	public byte MIME_PART_EPILOG = 3;
	/** Mime part type is retrieve information. */
	public byte MIME_PART_RETRIEVE_INFO = 4;
	/** Mime part type is a message. */
	public byte MIME_PART_MESSAGE = 5;
	
	public int OOOPROF_MAX_BODY_SIZE = 32767;		 // Buffers passed into OOOGetGeneralSubject should be this size
	
	/* Item values to pass into OSGetExtIntlFormat(..)  */
	public byte EXT_AM_STRING = 1;	/* Request for AM String */
	public byte EXT_PM_STRING = 2;	/* Request for PM String */
	public byte EXT_CURRENCY_STRING = 3;	/* Request for Currency String */
	public byte MONTH_NAMES = 4;	/* Request for Month Names */
	public byte ABBR_MONTH_NAMES = 5;	/* Request for abbreviated month names */
	public byte WEEKDAY_NAMES = 6;	/* Request for weekday names */
	public byte ABBR_WEEKDAY_NAMES = 7;	/* Request for abbreviated weekday names */
	public byte CALENDARTYPE	 = 8;	/* Request for Calendar Type, see CALENDAR_XXX types below */
	public byte ERANAME = 9;	/* Request for Asian Native Calendar Name */
	public byte ABBRERANAME = 10;  /* Request for abbreviated Asian Native Calendar Name*/

	/* CalendarType */
	public byte CALENDAR_NONE = 0;
	public byte CALENDAR_JAPAN = 1;
	public byte CALENDAR_TAIWAN = 2;
	public byte CALENDAR_THAI = 3;
	public byte CALENDAR_KOREA = 4;

	public static int MAX_ITEMDEF_SEGMENTS = 25;
	
	/** Open the Stream for Read  */
	public int MIME_STREAM_OPEN_READ = 0x00000001;
	/** Open the Stream for Write */
	public int MIME_STREAM_OPEN_WRITE = 0x00000002;

	/** Include MIME Headers */
	public int MIME_STREAM_MIME_INCLUDE_HEADERS = 0x00000010;
	/** Include RFC822 Headers */
	public int MIME_STREAM_RFC2822_INCLUDE_HEADERS = 0x00000020;

	/** Include RFC822, MIME Headers      */
	public int MIME_STREAM_INCLUDE_HEADERS = (MIME_STREAM_MIME_INCLUDE_HEADERS|MIME_STREAM_RFC2822_INCLUDE_HEADERS);

	public int MIME_STREAM_SUCCESS = 0;
	public int MIME_STREAM_EOS = 1;
	public int MIME_STREAM_IO = 2;

	/*	Define the MIME stream itemize options. */
	public int MIME_STREAM_NO_DELETE_ATTACHMENTS	 = 0x00000001;
	public int MIME_STREAM_ITEMIZE_HEADERS = 0x00000002;
	public int MIME_STREAM_ITEMIZE_BODY = 0x00000004;

	public int MIME_STREAM_ITEMIZE_FULL = (MIME_STREAM_ITEMIZE_HEADERS|MIME_STREAM_ITEMIZE_BODY);

	/*	Value type constants */

	public short VT_LONG	 = 0;
	public short VT_TEXT	 = 1;
	public short VT_TIMEDATE = 2;
	public short VT_NUMBER = 3;

	/*	Flags for StatUpdate */

	/** Statistic is unique */
	public short ST_UNIQUE = 0x0001;
	/** Add to VT_LONG statistic, don't replace */
	public short ST_ADDITIVE	 = 0x0002;
	/** Statistic is resetable to 0 */
	public short ST_RESETABLE = 0x0003;

	/*	Define search results data structure */

	/** Array of scores follows */
	public short FT_RESULTS_SCORES = 0x0001;
	/** Search results are series of FT_SEARCH_RESULT_ENTRY structures */
	public short FT_RESULTS_EXPANDED = 0x0002;
	/** Url expanded format returned by FTSearchExt only */
	public short FT_RESULTS_URL = 0x0004;
	
	/* year, month, and day */
	public byte TDFMT_FULL = 0;
	/* month and day, year if not this year */
	public byte TDFMT_CPARTIAL = 1;
	/* month and day */
	public byte	TDFMT_PARTIAL = 2;
	/* year and month */
	public byte TDFMT_DPARTIAL = 3;
	/* year(4digit), month, and day */
	public byte TDFMT_FULL4 = 4;
	/* month and day, year(4digit) if not this year */
	public byte TDFMT_CPARTIAL4 = 5;
	/* year(4digit) and month */
	public byte TDFMT_DPARTIAL4 = 6;
	/* hour, minute, and second */
	public byte TTFMT_FULL = 0;
	/* hour and minute */
	public byte TTFMT_PARTIAL = 1;
	/* hour */
	public byte TTFMT_HOUR = 2;
	/* hour, minute, second, hundredths (max resolution). This currently works only for time-to-text conversion! */
	public byte TTFMT_FULL_MAX = 3;

	/* all times converted to THIS zone*/ 
	public byte TZFMT_NEVER = 0;
	/* show only when outside this zone */
	public byte TZFMT_SOMETIMES = 1;
	/* show on all times, regardless */
	public byte TZFMT_ALWAYS = 2;

	/* DATE */
	public byte TSFMT_DATE = 0;		
	/* TIME */
	public byte TSFMT_TIME = 1;		
	/* DATE TIME */
	public byte TSFMT_DATETIME = 2;	
	/* DATE TIME or TIME Today or TIME Yesterday */
	public byte TSFMT_CDATETIME = 3;
	/* DATE, Today or Yesterday */ 
	public byte TSFMT_CDATE = 4;

	//lengths of strings in INTLFORMAT
	
	public int ISTRMAX = 5;
	public int YTSTRMAX = 32;

	/*	International Environment Parameter Definitions */

	public short CURRENCY_SUFFIX = 0x0001;
	public short CURRENCY_SPACE = 0x0002;
	public short NUMBER_LEADING_ZERO = 0x0004;
	public short CLOCK_24_HOUR = 0x0008;
	public short DAYLIGHT_SAVINGS = 0x0010;
	public short DATE_MDY = 0x0020;
	public short DATE_DMY = 0x0040;
	public short DATE_YMD = 0x0080;
	public short DATE_4DIGIT_YEAR = 0x0100;
	public short TIME_AMPM_PREFIX = 0x0400;
	public short DATE_ABBREV	 = 0x0800;
	
	/* edit control styles */
	public short EC_STYLE_EDITMULTILINE = 0x0001;
	public short EC_STYLE_EDITVSCROLL = 0x0002;
	public short EC_STYLE_EDITPASSWORD = 0x0004;
	/* combobox styles */
	public short EC_STYLE_EDITCOMBO = 0x0001;
	/* list box styles */
	public short EC_STYLE_LISTMULTISEL = 0x0001;
	/* time control styles */
	public short EC_STYLE_CALENDAR = 0x0001;
	public short EC_STYLE_TIME = 0x0002;
	public short EC_STYLE_DURATION = 0x0004;
	public short EC_STYLE_TIMEZONE = 0x0008;
	/* control style is valid */
	public int EC_STYLE_VALID = 0x80000000;

	/** other control flags */
	public short EC_FLAG_UNITS = 0x000F;
	/** Width/Height are in dialog units, not twips */
	public short EC_FLAG_DIALOGUNITS = 0x0001;
	/** Width/Height should be adjusted to fit contents */
	public short EC_FLAG_FITTOCONTENTS = 0x0002;
	/** this control is active regardless of docs R/W status */
	public short EC_FLAG_ALWAYSACTIVE = 0x0010;
	/** let placeholder automatically fit to window */
	public short EC_FLAG_FITTOWINDOW = 0x0020;
	/** position control to top of paragraph */
	public short EC_FLAG_POSITION_TOP = 0x0040;
	/** position control to bottom of paragraph */
	public short EC_FLAG_POSITION_BOTTOM = 0x0080;
	/** position control to ascent of paragraph */
	public short EC_FLAG_POSITION_ASCENT = 0x0100;
	/** position control to height of paragraph */
	public short EC_FLAG_POSITION_HEIGHT = 0x0200;

	public short FR_RUN_ALL = 0x1000;
	public short FR_RUN_CLEANUPSCRIPT_ONLY = 0x1; 
	public short FR_RUN_NSD_ONLY = 0x2; 
	public short FR_DONT_RUN_ANYTHING = 0x4; 
	public short FR_SHUTDOWN_HANG = 0x8; 
	public short FR_PANIC_DIRECT = 0x10; 
	public short FR_RUN_QOS_NSD = 0x20; 
	public short FR_NSD_AUTOMONITOR = 0x40;
	
	/** maximum number of stops in tables */
	public static int MAXTABS = 20;

	/* ignore the action.  don't log anything and just continue */
	public short DXLLOGOPTION_IGNORE=1;
	/* log the problem as a warning */
	public short DXLLOGOPTION_WARNING=2;
	/* log the problem as an error */
	public short DXLLOGOPTION_ERROR=3;
	/* log the problem as a fatal error */
	public short DXLLOGOPTION_FATALERROR=4;
	
	//	DXL_EXPORT_PROPERTY default values are set as follows:
	//		 
	//		Note:	(i) = can input new value into the exporter.
	//		 	(o) = can get current value out of exporter.
	//		 	(io) = can do both. 
	//		 
	//		 	eDxlExportResultLog		= (o)	NULLMEMHANDLE
	//			eDefaultDoctypeSYSTEM	= (o)	default filename of dtd keyed to current version of DXL exporter."
	//			eDoctypeSYSTEM		= (io)	filename of dtd keyed to current version of DXL exporter."
	//		 	eDXLBannerComments		= (io)	NULLMEMHANDLE
	//		 	eDxlExportCharset		= (io)	eDxlExportUtf8
	//		 	eDxlRichtextOption		= (io)	eRichtextAsDxl
	//			eDxlExportResultLogComment	= (io)	NULLMEMHANDLE
	//		 	eForceNoteFormat		= (io)	FALSE
	//		 	eExitOnFirstFatalError		= (io)	TRUE
	//		 	eOutputRootAttrs		= (io)	TRUE
	//		 	eOutputXmlDecl		= (io)	TRUE
	//		 	eOutputDOCTYPE		= (io)	TRUE
	//			eConvertNotesbitmapToGIF	= (io) 	FALSE
	//			eDxlValidationStyle		= (io)	eDxlExportValidationStyle_DTD"
	//			eDxlDefaultSchemaLocation	= (o)	URI's of schema keyed to current version of DLX exporter."
	//			eDxlSchemaLocation		= (io)	filename of XML Schema keyed to current version of DXL exporter."

			
	/* non-boolean export properties */

	/** MEMHANDLE,				Readonly - the result log from the last export. */
	public int eDxlExportResultLog=1;
	/**MEMHANDLE,				Readonly - filename of dtd/schema keyed to current version of exporter */
	public int eDefaultDoctypeSYSTEM=2;
	/** char*(i)/MEMHANDLE(o),	What to use for the DOCTYPE SYSTEM value (if emitted)<br>
	 * NULL or "" 	= DOCTYPE should contain no SYSTEM info<br>
	 * "filename"	= filename of dtd or schema used as DOCTYPE SYSTEM value */
	public int eDoctypeSYSTEM=3;
	/** char*(i)/MEMHANDLE(o),	One or more XML comments to output at top of the DXL<br>
	 * NULL or ""	= no dxl banner comments<br>
	 * "whatever"	= zero or more nul-terminated strings capped by extra empty string */
	public int eDXLBannerComments=4;
	/** DXL_EXPORT_CHARSET,		Specifies output charset. */
	public int eDxlExportCharset=5;
	/** DXL_RICHTEXT_OPTION,		Specifies rule for exporting richtext. */
	public int eDxlRichtextOption=6;
	/** char*(i)/MEMHANDLE(o),	LMBCS string to be added as comment to top of result log */
	public int eDxlExportResultLogComment=7;
	/** DXL_EXPORT_VALIDATION_STYLE,	Specifies style of validation info emitted by exporter<br>
	 * Can override other settings, eg - eOutputDOCTYPE */
	public int eDxlValidationStyle=8;
	/** MEMHANDLE,				Readonly - default xsi:SchemaLocation attribute value for current DXL version */
	public int eDxlDefaultSchemaLocation=9;
	/** char*(i)/MEMHANDLE(o),	LMBCS value of xsi:SchemaLocation attribute put into DXL root element */
	public int eDxlSchemaLocation=10;
	/** DXL_MIME_OPTION,			Specifies rule for exporting native MIME. */
	public int eDxlMimeOption=11;
	/** char*(i)/MEMHANDLE(o),	Text to insert within richtext where an attachmentref<br>
	 * was omitted; may contain XML markup but must be valid<br>
	 * DXL richtext content */
	public int eAttachmentOmittedText=12;
	/** char*(i)/MEMHANDLE(o),	Text to insert within richtext where an objectref<br>
	 * was omitted; may contain XML markup but must be valid<br>
	 * DXL richtext content */
	public int eOLEObjectOmittedText=13;
	/** char*(i)/MEMHANDLE(o),	Text to insert within richtext where a picture<br>
	 * was omitted; may contain XML markup but must be valid<br>
	 * DXL richtext content */
	public int ePictureOmittedText=14;	
	/** HANDLE of list			List of item names to omit from DXL.  Use Listxxx<br>
	 * functions to build list (use fPrefixDataType=FALSE)<br>
	 * (i)API makes a copy of list thus does not adopt HANDLE<br>
	 * (o)API returns copy of list thus caller must free */
	public int eOmitItemNames=15;
	/** HANDLE of list			List of item names; only items with one of these names will be included in the output DXL.<br>
	 *   Use Listxxx<br>
	 *   functions to build list (use fPrefixDataType=FALSE)<br>
	 *   (i)API makes a copy of list thus does not adopt HANDLE<br>
	 *   (o)API returns copy of list thus caller must free */
	public int eRestrictToItemNames=16;

	/* boolean properties (gap allows for future definitions of other non-boolean export properties) */

	/** BOOL, TRUE = Export data as notes containing items, FALSE = export using a high level of abstraction, */
	public int eForceNoteFormat=30;
	/** BOOL, TRUE = Abort on first fatal error, FALSE = try to continue to export */
	public int eExitOnFirstFatalError=31;
	/** BOOL, TRUE = Root needs xmlns, version, and other common root attrs */
	public int eOutputRootAttrs=32;
	/** BOOL, TRUE = Emit a leading xml declaration statement (&lt;?xml ...?&gt;) */
	public int eOutputXmlDecl=33;
	/** BOOL, TRUE = Emit a DOCTYPE statement (can be overridden by dDxlValidationStyle) */
	public int eOutputDOCTYPE=34;
	/** BOOL, TRUE = Convert Notesbitmaps embedded in richtext to GIFs, FALSE = blob the Notesbitmap CD records */
	public int eConvertNotesbitmapsToGIF=35;
	/** BOOL, TRUE = omit attachments within documents: both the attachmentref
	 * within richtext and corresponding items that contain file objects */
	public int eOmitRichtextAttachments=36;
	/** BOOL, TRUE = omit OLE objects within documents: both the objectref
	 * within richtext and corresponding items that contain file objects */
	public int eOmitOLEObjects=37;	
	/** BOOL, TRUE = omit items within documents that are not normal attachments
	 * (named $FILE) and that contain file objects */
	public int eOmitMiscFileObjects=38;
	/** BOOL, TRUE = omit pictures that occur directly within document richtext and
	 * contain gif, jpeg, notesbitmap, or cgm--does not include picture within
	 * attachmentref or imagemap */
	public int eOmitPictures=39;
	/** BOOL, TRUE = uncompress attachments */
	public int eUncompressAttachments=40;

	/*
	 * DXL export charsets
	 */

	/** (default) "encoding =" attribute is set to utf8 and output charset is utf8 */
	public int DXL_EXPORT_CHARSET_eDxlExportUtf8 = 0;
	
	/** "encoding =" attribute is set to utf16 and charset is utf16 */
	public int DXL_EXPORT_CHARSET_eDxlExportUtf16 = 1;

	public int DXL_EXPORT_VALIDATION_STYLE_eDxlExportValidationStyle_None = 0;
	public int DXL_EXPORT_VALIDATION_STYLE_eDxlExportValidationStyle_DTD = 1;
	public int DXL_EXPORT_VALIDATION_STYLE_eDxlExportValidationStyle_XMLSchema = 2;
	
	/** (default) output native MIME within &lt;mime&gt; element in DXL */
	public int DXL_MIME_OPTION_eMimeAsDxl = 0;
	/** output MIME as uninterpretted (base64'ed) item data */
	public int DXL_MIME_OPTION_eMimeAsItemdata = 1;
	
	/** (default) output richtext as dxl with warning 
	   comments if uninterpretable CD records */
	public int DXL_RICHTEXT_OPTION_eRichtextAsDxl = 0;
	/** output richtext as uninterpretted (base64'ed) item data */
	public int DXL_RICHTEXT_OPTION_eRichtextAsItemdata = 1;
	
//	DXL_IMPORT_PROPERTY default values are set as follows:
//		 
//		 Note:	(i) = can input new value into the importer.
//		 	(o) = can get current value out of importer.
//		 	(io) = can do both. 
//		 
//		 	iACLImportOption			= (io) DXLIMPORTOPTION_IGNORE
//		 	iDesignImportOption			= (io) DXLIMPORTOPTION_IGNORE
//		 	iDocumentsImportOption		= (io) DXLIMPOROPTION_CREATE
//		 	iCreateFullTextIndex			= (io) FALSE
//
//				note:	To create a Full Text Index on a database, the iCreateFullTextIndex must be set to TRUE,
//				          	the iReplaceDbProperties must be set to TRUE and a schema element named &lt;fulltextsettings&gt;
//				        	 must be defined.
//
//		 	iReplaceDbProperties			= (io) FALSE
//		 	iInputValidationOption			= (io) Xml_Validate_Auto
//		 	iReplicaRequiredForReplaceOrUpdate	= (io) TRUE
//		 	iExitOnFirstFatalError			= (io) TRUE
//		 	iUnknownTokenLogOption		= (io) DXLLOGOPTION_FATALERROR
//		 	iResultLogComment			= (io) NULLMEMHANDLE
//		 	iResultLog				= (o)  NULLMEMHANDLE
//			iImportedNoteList			= (o)  NULLHANDLE

	/** WORD, Assign to value defined in DXLIMPORTOPTION */
	public short iACLImportOption=1;
	/** WORD, Assign to value defined in DXLIMPORTOPTION */
	public short iDesignImportOption=2;
	/** WORD, Assign to value defined in DXLIMPORTOPTION */
	public short iDocumentsImportOption=3;
	/** BOOL, TRUE = create full text index, FALSE Do NOT create full text index<br>
	 * In order to create FullTextIndex ReplaceDbProperties needs to be True<br>
	 * element 6lt;fulltextsettings&gt; needs to exist in the Dxl */
	public short iCreateFullTextIndex=4;
	/** BOOL, TRUE = replace database properties, FALSE Do NOT replace database properties */
	public short iReplaceDbProperties=5;
	/** Xml_Validation_Option, Values defined in Xml_Validation_Option, ...Validate_Never, ...Validate_Always, ...Validate_Auto */
	public short iInputValidationOption=6;
	/** BOOL, TRUE = skip replace/update ops if target DB and import DXL do not have same replicaid's<br>
	 * ... FALSE = allow replace/update ops even if target DB and import DXL do not have same replicaid's */
	public short iReplicaRequiredForReplaceOrUpdate=7;
	/** BOOL, TRUE = importer exits on first fatal error, FALSE = importer continues even if fatal error found */
	public short iExitOnFirstFatalError=8;
	/** WORD, Assign to value defined in DXLLOGOPTION. Specifies what to do if DXL contains an unknown element or attribute */
	public short iUnknownTokenLogOption=9;
	/** char*(i)/MEMHANDLE(o)  LMBCS string to be added as comment to top of result log */ 
	public short iResultLogComment=10;
	/** MEMHANDLE, (readonly) The result log from the last import */
	public short iResultLog=11;
	/** DHANDLE, (readonly) An IDTABLE listing the notes imported by the last import operation */
	public short iImportedNoteList=12;
	/** BOOL, TRUE = all imported LotusScript code is compiled, FALSE = no compilation */
	public short iCompileLotusScript=13;

	/*	CDRESOURCE Flags */
	
	/** the type's data is a formula, valid for _TYPE_URL and _TYPE_NAMEDELEMENT
	*/
	public int CDRESOURCE_FLAGS_FORMULA = 0x00000001;

	/** the notelink variable length data
	* contains the notelink itself not
 	* an index into a $Links items
	*/
	public int CDRESOURCE_FLAGS_NOTELINKINLINE = 0x00000002;
	
	/** If specified, the link
	is to an absolute 
	database or thing.
	Used to make a hard
	link to a specific DB. */
	public int CDRESOURCE_FLAGS_ABSOLUTE = 0x00000004;
	
	/** If specified, the server
	and file hint are filled
	in and should be 
	attempted before trying
	other copies. */
	public int CDRESOURCE_FLAGS_USEHINTFIRST = 0x00000008;

	/** the type's data is a canned image file (data/domino/icons/[*].gif)
	*  valid for _TYPE_URL and _CLASS_IMAGE only
	*/
	public int CDRESOURCE_FLAGS_CANNEDIMAGE = 0x00000010;
	
	/*	NOTE: _PRIVATE_DATABASE and _PRIVATE_DESKTOP are mutually exclusive. */
	
	/** the object is private in its database */
	public int CDRESOURCE_FLAGS_PRIVATE_DATABASE = 0x00000020;
	/** the object is private in the desktop database */
	public int CDRESOURCE_FLAGS_PRIVATE_DESKTOP = 0x00000040;

	/** the replica in the CD resource needs to be obtained via RLGetReplicaID
	to handle special replica IDs like 'current' mail file. */
	public int CDRESOURCE_FLAGS_REPLICA_WILDCARD = 0x00000080;
	
	/** used with class view and folder to mean "Simple View" */
	public int CDRESOURCE_FLAGS_SIMPLE = 0x00000100;
	/** open this up in design mode */
	public int CDRESOURCE_FLAGS_DESIGN_MODE = 0x00000200;
	/** open this up in prevew mode, if supported.   Not saved to disk */
	public int CDRESOURCE_FLAGS_PREVIEW = 0x00000400;
	/** we will be doing a search after link opened.   Not saved to disk */
	public int CDRESOURCE_FLAGS_SEARCH = 0x00000800;

	/** An UNID is added to the end of the hResource that means 
	something to that type  - currently used in named element type*/
	public int CDRESOURCE_FLAGS_UNIDADDED = 0x00001000;
	/** document should be in edit mode */
	public int CDRESOURCE_FLAGS_EDIT_MODE = 0x00002000;


	/** reserved meaning for each resource link class */
	public int CDRESOURCE_FLAGS_RESERVED1 = 0x10000000;
	/** reserved meaning for each resource link class */
	public int CDRESOURCE_FLAGS_RESERVED2 = 0x20000000;
	/** reserved meaning for each resource link class */
	public int CDRESOURCE_FLAGS_RESERVED3 = 0x40000000;
	/** reserved meaning for each resource link class */
	public int CDRESOURCE_FLAGS_RESERVED4 = 0x80000000;


	/* Types of CDRESOURCE
	*/
	public short CDRESOURCE_TYPE_EMPTY = 0;
	public short CDRESOURCE_TYPE_URL = 1;
	public short CDRESOURCE_TYPE_NOTELINK = 2;
	public short CDRESOURCE_TYPE_NAMEDELEMENT = 3;
	/** Currently not written to disk only used in RESOURCELINK */
	public short CDRESOURCE_TYPE_NOTEIDLINK = 4;
	/** This would be used in conjunction with the formula flag. The formula
	is an @-Command that would perform some action, typically it would also switch to a 
	Notes UI element. This will be used to reference the replicator page and other UI elements. */
	public short CDRESOURCE_TYPE_ACTION = 5;
	/** Currently not written to disk only used in RESOURCELINK */
	public short CDRESOURCE_TYPE_NAMEDITEMELEMENT = 6;

	/** And above...  See comment below. */
	public short CDRESOURCE_TYPE_RESERVERS = 32000;

	/*	Sitemaps/Outlines use the same type identifiers as resource links.
		However, there are some types that are special to an outline, and
			we want to reserve an upper range for thos special types.

		For now, reserve the entire upper range 32,000 and up for them.
		The IDs are started at MAXWORD and work their way down.   
	*/


	/* Classes of resource linked to by CDRESOURCE
	*/
	
	public short CDRESOURCE_CLASS_UNKNOWN = 0;
	public short CDRESOURCE_CLASS_DOCUMENT = 1;
	public short CDRESOURCE_CLASS_VIEW = 2;
	public short CDRESOURCE_CLASS_FORM = 3;
	public short CDRESOURCE_CLASS_NAVIGATOR = 4;
	public short CDRESOURCE_CLASS_DATABASE = 5;
	public short CDRESOURCE_CLASS_FRAMESET = 6;
	public short CDRESOURCE_CLASS_PAGE = 7;
	public short CDRESOURCE_CLASS_IMAGE = 8;
	public short CDRESOURCE_CLASS_ICON = 9;
	public short CDRESOURCE_CLASS_HELPABOUT = 10;
	public short CDRESOURCE_CLASS_HELPUSING = 11;
	public short CDRESOURCE_CLASS_SERVER = 12;
	public short CDRESOURCE_CLASS_APPLET = 13;
	/** A compiled formula someplace */
	public short CDRESOURCE_CLASS_FORMULA = 14;
	public short CDRESOURCE_CLASS_AGENT = 15;
	/** a file on disk (file:) */
	public short CDRESOURCE_CLASS_FILE = 16;
	/** A file attached to a note */
	public short CDRESOURCE_CLASS_FILEATTACHMENT = 17;
	public short CDRESOURCE_CLASS_OLEEMBEDDING = 18;
	/** A shared image resource */
	public short CDRESOURCE_CLASS_SHAREDIMAGE = 19;
	public short CDRESOURCE_CLASS_FOLDER = 20;
	/** An old (4.6) or new style portfolio. Which gets incorporated into the bookmark bar as a tab, rather
	 * than getting opened as a database. */
	public short CDRESOURCE_CLASS_PORTFOLIO = 21;
	public short CDRESOURCE_CLASS_OUTLINE = 22;

	public short KFM_access_GetIDFHFlags = 51;

	/** File is password protected */
	public short fIDFH_Password = 0x0001;
	/** File password is required. */
	public short fIDFH_Password_Required = 0x0002; 
	/** Update 'MinPasswordSize' field of the target ID file when merging this ID file with the target ID file. */ 
	public short fIDFH_UpdateMinPWOnMerge = 0x0004;
	/** Password may be shared by all processes */
	public short fIDFH_PWShareable = 0x0008;
	/** ID file may be used for the limited version of Notes */
	public short fIDFH_ForLimitedOnly = 0x0010; 
	                                                   
	/** THIS IS OBSOLETE, DO NOT USE ID may not be used on work stations. */
	public short fIDFH_ForServersOnly = 0x0020;
	/** ID file may be used for the "Desktop" version of Notes Not yet supported */ 
	public short fIDFH_ForDesktopOnly = 0x0040;
	/** Client license, but client cannot run the "desktop"  program. */
	public short fIDFH_ForServiceOnly = 0x0080;

	/** ID has been modified such that a backup should be performed */
	public short fIDFH_BackupNeeded = 0x0100;
	/** ID file has an extra that descibes special password features (eg, 128 bit key) */
	public short fIDFH_PWExtra = 0x0200; 
	/** Must prompt user before automatically accepting a name change */ 
	public short fIDFH_ChangeNamePrompt = 0x0400;

	/** For mailed in requests to certifier usually using a "safe-copy".<br>
	 * This flags says that the requestor does not need a response via Mail -- usually because the response
	 * will be detected during authentication with a server whose Address Book has been
	 * updated with a new certificate for the requestor. */ 
	public short f1IDFH_DontReplyViaMail = 0x0001;

	/** ID file's UDOs contain more more than one LICENSE, most likely due to an ID merge. */ 
	public short f1IDFH_MultipleLicenses = 0x0002;

	/** Admin has locked down the value of this field. See fIDFH_PWShareable */ 
	public short f1IDFH_PWShareableLockdown = 0x0004;

	/** SI not converted to new fmt */ 
	public short f1IDFH_SINotConverted = (short) (0x8000 & 0xffff);

	public short NONS_NOASSIGN = (short) (0x8000 & 0xffff);
	
	/** Named Object "User Unread ID Table" Name Space */
	public short NONS_USER_UNREAD = 0;
	/** Named Note */
	public short NONS_NAMED_NOTE = 1;
	/** Named Object "User NameList" name space */
	public short NONS_USER_NAMELIST = 2;
	/** Named object - Folder Directory Object */
	public short NONS_FDO = 3;
	/** Named object - Execution Control List object */
	public short NONS_ECL_OBJECT = 4;
	/** Named object - design note (exists in ODS37+). */
	public short NONS_DESIGN_NOTE = 5;
	/** Named object - IMAP visable folders (exists in build 166+) */
	public short NONS_IMAP_FOLDERS = 6;
	/** Named object - activity log for unread marks */
	public short NONS_USER_UNREAD_ACTIVITY_LOG = 7;
	/** Named object - DAOS pending object delete */
	public short NONS_DAOS_DELETED = 8;
	/** Named object - DAOS pending object delete */
	public short NONS_DAOS_OBJECT = 9;

	/* Option flags for NSFDbCreateAndCopy */

	public int DBCOPY_REPLICA = 0x00000001;
	public int DBCOPY_SUBCLASS_TEMPLATE = 0x00000002;
	public int DBCOPY_DBINFO2 = 0x00000004;
	public int DBCOPY_SPECIAL_OBJECTS = 0x00000008;
	public int DBCOPY_NO_ACL	 = 0x00000010;
	public int DBCOPY_NO_FULLTEXT = 0x00000020;
	public int DBCOPY_ENCRYPT_SIMPLE = 0x00000040;
	public int DBCOPY_ENCRYPT_MEDIUM = 0x00000080;
	public int DBCOPY_ENCRYPT_STRONG = 0x00000100;
	public int DBCOPY_KEEP_NOTE_MODTIME = 0x00000200;
	public int DBCOPY_REPLICA_NAMELIST = 0x01000000;	/* Copy the NameList (applicable only when DBCOPY_REPLICA is specified) */
	public int DBCOPY_DEST_IS_NSF = 0x02000000;	/* Destination is NSF-backed database */
	public int DBCOPY_DEST_IS_DB2 = 0x04000000;	/* Destination is DB2-backed database */
	public int DBCOPY_OVERRIDE_DEST = 0x08000000;	/* Destination should override default if able to */
	public int DBCOPY_DBCLASS_HIGHEST_NOTEFILE = 0x10000000; /* Create Db using the latest ODS, regardless of INI settings */
	public int DBCOPY_COMPACT_REPLICA = 0x20000000; /* Create Db for copy style compaction */

	/**  If present, is ID table representing the contents of the folder */
	public String VIEW_FOLDER_IDTABLE = "$FolderIDTable"	;
	
	/**  If present, is ODS version of FOHEADER and 
	set of entries, see dbfolder.h.  The view
	may have additional items with suffixes
	on this item name, e.g., $FolderObject1 */
	public String VIEW_FOLDER_OBJECT = "$FolderObject";

	/* Note is shared (always located in the database) */
	public int DESIGN_TYPE_SHARED = 0;
	/* Note is private and is located in the database */
	public int DESIGN_TYPE_PRIVATE_DATABASE = 1;

	/* wMailNoteFlags -- for extended control via MailNoteJitEx */
	
	/* mail the note if there is at least one recipient, i.e. To, CC, or BCC */
	public short MAILNOTE_ANYRECIPIENT = 0x0001;
	/* enabled logging */
	public short MAIL_JIT_LOG = 0x0002;

	/* wMailNoteFlags -- for extended control via MailNoteJitEx2 */
	
	/* caller is enforcing OSGetEnvironmentInt(SECUREMAIL) ... don't force MSN_SIGN OR MSN_SEAL */
	public short MAILNOTE_NO_SECUREMAIL_MODE = 0x0004;
	/* use local mail.box, regardless of LOCINFO_REALMAILSERVER or LOCINFO_MAILSERVER setting. */
	public short MAILNOTE_USELOCALMAILBOX = 0x0008;
	/* don't use $$LNABHAS* in place of actual cert  */
	public short MAILNOTE_NO_LNAB_ENTRIES = 0x0010;
	/* don't have NAMELookup use local directories */
	public short MAILNOTE_NO_SEARCH_LOCAL_DIRECTORIES = 0x0020;
	/* if recips only have Notes certs, do Notes encryption of MIME message */
	public short MAILNOTE_NOTES_ENCRYPT_MIME = 0x0040;
	/* message is in mime format */
	public short MAILNOTE_MIMEBODY = 0x0080;
	/* use X509 cert if it's the only one found */
	public short MAILNOTE_CANUSEX509 = 0x0100;
	public short MAILNOTE_SKIP_LOOKUP = 0x0200;

	/* mail note that is not a jit canidate*/
	public short MAIL_NO_JIT = 0;
	/* mail a JIT canidate note*/
	public short MAIL_JIT = 1;
	/* mail a miem message that is not a jit canidate*/
	public short MAIL_MIME_NO_JIT = 2;
	/* mail a note that is not a JIT, but caller has set recipient's field */
	public short MAIL_NO_JIT_RECIPIENTS_DONE = 3;

	/* Query for Sign/Seal */
	public short MSN_QUERY = 0x0001;
	/* Sign */
	public short MSN_SIGN = 0x0002;
	/* Seal */
	public short MSN_SEAL = 0x0004;
	/* Use results of previous query */
	public short MSN_PREVQUERY = 0x0008;
	/* Must send to North American */
	public short MSN_AMERICAN_ONLY = 0x0010;
	
	/* license holders. */
	
	/* Recipient must have valid */
	public short MSN_PUBKEY_ONLY = 0x0020;
	/* Sending a receit message. */
	public short MSN_RECEIPT = 0x0040;
	/* The dialog is for a DDE request */
	public short MSN_DDEDIALOG = 0x0080;
	/* Don't allow mail encryption */
	public short MSN_NOSEAL = 0x0100;
	/* Used by alternate mail */
	public short MSN_FWDASATT = 0x0200;
	
	/* The mailer should process the */
	/* note, skip recipient work */
	/* and prompt the user for */
	/* addressing. */ 
	
	/* Disregard all other flags */
	public short MSN_ADDRESS_ONLY = 0x0400;
	
	/* and refresh the mail addresses */
	/* via lookup */
	
	/* Don't lookup the supplied names */
	public short MSN_NOLOOKUP = 0x0800;
	/*  MailForwardNoteNoEdit only */
	
	/* Used by alternate mail */
	public short MSN_FWDASTEXT = 0x1000; 
	/* The mailer should process the */
	/* note, skip recipient work */
	/* and prompt the user for */
	/* addressing. */
	
	/* Indicates that the From field has */
	/* already been set up and that the */
	/* mailer should not slam it with the */
	/* user name. This is used by agents */
	/* running on the server */
	public short MSN_FROMOVERRIDE = 0x2000;
	/* deposit in smtp.box instead of mail.box */
	public short MSN_SMTP_MAILBOX = 0X4000;

	/* 2 pass mailer */
	public short MSN_2PASSMAILER = (short) (0x8000 & 0xffff);

	public String DESIGN_CLASS = "$Class";
	
	public String FIELD_UPDATED_BY = "$UpdatedBy";
	public String FIELD_FORM = "Form";

	/* form item to hold form CD */
	public String ITEM_NAME_TEMPLATE = "$Body";

	/* SendTo item name */
	public String MAIL_SENDTO_ITEM = "SendTo";
	/* CopyTo item name */
	public String MAIL_COPYTO_ITEM = "CopyTo";
	/* Blind copy to item name */
	public String MAIL_BLINDCOPYTO_ITEM = "BlindCopyTo";

	public short DGN_SKIPBLANK = 0x0001;
	/** only match with the main name of the design element. */
	public short DGN_SKIPSYNONYMS = 0x0002;
	
	/** remove underlines from the names (they indicate a hotkey) */
	public short DGN_STRIPUNDERS = 0x0004;

	/** convert underscore to a character that identifies the next character as a hotkey (&amp; on windows) */
	public short DGN_CONVUNDERS = 0x0008;
	public short DGN_CASEINSENSITIVE = 0x0010;
	public short DGN_STRIPBACKS = 0x0020;

	public short DGN_ONLYSHARED = 0x0040;
	public short DGN_ONLYPRIVATE = 0x0080;

	public short DGN_FILTERPRIVATE1STUSE = 0x0100;
	public short DGN_ALLPRIVATE = 0x0200;
	public short DGN_HASUNID = 0x0400;
	public short DGN_NOCHECKACCESS = 0x0800;
	public short DGN_LISTBOX = (DGN_STRIPUNDERS|DGN_SKIPSYNONYMS|DGN_SKIPBLANK);
	public short DGN_MENU = (DGN_SKIPSYNONYMS|DGN_SKIPBLANK);
	
	/** tells enumeration functions to only enumerate if readily available not if NIFReadEntries necessary.
	 * Used to allow Desk cache to work or fallback to NSFDbFindDesignNote direct lookup
	 */
	public short DGN_ONLYIFFAST = 0x1000;
	/** return alias only if it has, don't return display name as alias */
	public short DGN_ONLYALIAS = 0x2000;
	/** only match if the name or alias is exactly the same as the name supplied */
	public short DGN_EXACTNAME = 0x4000;

	/* display things editable with dialog box; no version filtering (for design) */
	public String DFLAGPAT_VIEWFORM_ALL_VERSIONS = "-FQMUGXWy#i:|@K;g~%z^}";

	/*	If this field exists in a mail note, it means that */
	/*	mail message was created by an agent. */
	public String ASSIST_MAIL_ITEM = "$AssistMail";

	/* indicates if message was auto generated */
	public String MAIL_ITEM_AUTOSUBMITTED = "Auto-submitted";
	/* value for 	MAIL_ITEM_AUTOSUBMITTED */
	public String MAIL_AUTOGENERATED = "auto-generated";

	/* From item name */
	public String MAIL_FROM_ITEM = "From";

	/* Posted date item name */
	public String MAIL_POSTEDDATE_ITEM = "PostedDate";

	/* Unique ID of this message */
	public String MAIL_ID_ITEM = "$MessageID";

	public String ITEM_IS_NATIVE_MIME = "$NoteHasNativeMIME";

	/* Body item name */
	public String MAIL_BODY_ITEM = "Body";

	public String FORM_SCRIPT_ITEM_NAME = "$$FormScript";
	public String DOC_SCRIPT_ITEM = "$Script";
	public String DOC_SCRIPT_NAME = "$$ScriptName";
	public String DOC_ACTION_ITEM = "$$FormAction";

	public String ITEM_NAME_NOTE_SIGNATURE = "$Signature";
	/* stored form signature */
	public String ITEM_NAME_NOTE_STOREDFORM_SIG = "$SIG$Form";
	/* stored form and subform signature prefix - followed by either $FORM or the subform name*/
	public String ITEM_NAME_NOTE_STOREDFORM_SIG_PREFIX = "$SIG";

	public String FIELD_TITLE = "$TITLE";
	/* document header info */
	public String ITEM_NAME_DOCUMENT = "$Info";
	public String SUBFORM_ITEM_NAME = "$SubForms";

	/* only subforms; no version filtering */
	public String DFLAGPAT_SUBFORM_ALL_VERSIONS = "+U";

	/*	Define function codes for SECKFMMakeSafeCopy */

	/** Create a safe-copy containing the "active" RSA keys. */
	public short KFM_safecopy_Standard = 0;
	
	/** Create a safe-copy containing the "pending" RSA keys */
	public short KFM_safecopy_NewPubKey = 1;
	
	/** Create a safe-copy containing the "pending" RSA keys if any, else use the "active" RSA keys. */
	public short KFM_safecopy_NewestKey = 2;

}