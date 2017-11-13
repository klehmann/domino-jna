package com.mindoo.domino.jna.constants;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.internal.NotesCAPI;

/**
 * Enum of the available database option bits that can be set via
 * {@link NotesDatabase#getOption(DatabaseOption)} and {@link NotesDatabase#setOption(DatabaseOption, boolean)}
 * 
 * @author Karsten Lehmann
 */
public enum DatabaseOption {
	/** Enable full text indexing */
	FT_INDEX(NotesCAPI.DBOPTBIT_FT_INDEX),
	
	/** TRUE if database is being used as an object store - for garbage collection */
	IS_OBJSTORE(NotesCAPI.DBOPTBIT_IS_OBJSTORE),
	
	/** TRUE if database has notes which refer to an object store - for garbage collection*/
	USES_OBJSTORE(NotesCAPI.DBOPTBIT_USES_OBJSTORE),
	
	/** TRUE if NoteUpdate of notes in this db should never use an object store. */
	OBJSTORE_NEVER(NotesCAPI.DBOPTBIT_OBJSTORE_NEVER),
	
	/** TRUE if database is a library */
	IS_LIBRARY(NotesCAPI.DBOPTBIT_IS_LIBRARY),
	
	/** TRUE if uniform access control across all replicas */
	UNIFORM_ACCESS(NotesCAPI.DBOPTBIT_UNIFORM_ACCESS),
	
	/** TRUE if NoteUpdate of notes in this db should always try to use an object store. */
	OBJSTORE_ALWAYS(NotesCAPI.DBOPTBIT_OBJSTORE_ALWAYS),
	
	/** TRUE if garbage collection is never to be done on this object store */
	COLLECT_NEVER(NotesCAPI.DBOPTBIT_COLLECT_NEVER),
	
	/** TRUE if this is a template and is considered an advanced one (for experts only.) */
	ADV_TEMPLATE(NotesCAPI.DBOPTBIT_ADV_TEMPLATE),
	
	/** TRUE if db has no background agent */
	NO_BGAGENT(NotesCAPI.DBOPTBIT_NO_BGAGENT),
	
	/** TRUE is db is out-of-service, no new opens allowed, unless DBOPEN_IGNORE_OUTOFSERVICE is specified */
	OUT_OF_SERVICE(NotesCAPI.DBOPTBIT_OUT_OF_SERVICE),
	
	/** TRUE if db is personal journal */
	IS_PERSONALJOURNAL(NotesCAPI.DBOPTBIT_IS_PERSONALJOURNAL),
	
	/** TRUE if db is marked for delete. no new opens allowed, cldbdir will delete the database when ref count = = 0;*/
	MARKED_FOR_DELETE(NotesCAPI.DBOPTBIT_MARKED_FOR_DELETE),
	
	/** TRUE if db stores calendar events */
	HAS_CALENDAR(NotesCAPI.DBOPTBIT_HAS_CALENDAR),
	
	/** TRUE if db is a catalog index */
	IS_CATALOG_INDEX(NotesCAPI.DBOPTBIT_IS_CATALOG_INDEX),
	
	/** TRUE if db is an address book */
	IS_ADDRESS_BOOK(NotesCAPI.DBOPTBIT_IS_ADDRESS_BOOK),
	
	/** TRUE if db is a "multi-db-search" repository */
	IS_SEARCH_SCOPE(NotesCAPI.DBOPTBIT_IS_SEARCH_SCOPE),
	
	/** TRUE if db's user activity log is confidential, only viewable by designer and manager */
	IS_UA_CONFIDENTIAL(NotesCAPI.DBOPTBIT_IS_UA_CONFIDENTIAL),
	
	/** TRUE if item names are to be treated as if the ITEM_RARELY_USED_NAME flag is set. */
	RARELY_USED_NAMES(NotesCAPI.DBOPTBIT_RARELY_USED_NAMES),
	
	/** TRUE if db is a "multi-db-site" repository */
	IS_SITEDB(NotesCAPI.DBOPTBIT_IS_SITEDB),

	/** TRUE if docs in folders in this db have folder references */
	FOLDER_REFERENCES(NotesCAPI.DBOPTBIT_FOLDER_REFERENCES),

	/** TRUE if the database is a proxy for non-NSF data */
	IS_PROXY(NotesCAPI.DBOPTBIT_IS_PROXY),
	
	/** TRUE for NNTP server add-in dbs */
	IS_NNTP_SERVER_DB(NotesCAPI.DBOPTBIT_IS_NNTP_SERVER_DB),
	
	/** TRUE if this is a replica of an IMAP proxy, enables certain * special cases for interacting with db */
	IS_INET_REPL(NotesCAPI.DBOPTBIT_IS_INET_REPL),
	
	/** TRUE if db is a Lightweight NAB */
	IS_LIGHT_ADDRESS_BOOK(NotesCAPI.DBOPTBIT_IS_LIGHT_ADDRESS_BOOK),
	
	/** TRUE if database has notes which refer to an object store - for garbage collection*/
	ACTIVE_OBJSTORE(NotesCAPI.DBOPTBIT_ACTIVE_OBJSTORE),
	
	/** TRUE if database is globally routed */
	GLOBALLY_ROUTED(NotesCAPI.DBOPTBIT_GLOBALLY_ROUTED),
	
	/** TRUE if database has mail autoprocessing enabled */
	CS_AUTOPROCESSING_ENABLED(NotesCAPI.DBOPTBIT_CS_AUTOPROCESSING_ENABLED),
	
	/** TRUE if database has mail filters enabled */
	MAIL_FILTERS_ENABLED(NotesCAPI.DBOPTBIT_MAIL_FILTERS_ENABLED),
	
	/** TRUE if database holds subscriptions */
	IS_SUBSCRIPTIONDB(NotesCAPI.DBOPTBIT_IS_SUBSCRIPTIONDB),
	
	/** TRUE if data base supports "check-in" "check-out" */
	IS_LOCK_DB(NotesCAPI.DBOPTBIT_IS_LOCK_DB),
	
	/** TRUE if editor must lock notes to edit */
	IS_DESIGNLOCK_DB(NotesCAPI.DBOPTBIT_IS_DESIGNLOCK_DB),

	/* ODS26+ options */
	
	/** if TRUE, store all modified index blocks in lz1 compressed form */
	COMPRESS_INDEXES(NotesCAPI.DBOPTBIT_COMPRESS_INDEXES),
	/** if TRUE, store all modified buckets in lz1 compressed form */
	COMPRESS_BUCKETS(NotesCAPI.DBOPTBIT_COMPRESS_BUCKETS),
	/** FALSE by default, turned on forever if DBFLAG_COMPRESS_INDEXES or DBFLAG_COMPRESS_BUCKETS are ever turned on. */
	POSSIBLY_COMPRESSED(NotesCAPI.DBOPTBIT_POSSIBLY_COMPRESSED),
	/** TRUE if freed space in db is not overwritten */
	NO_FREE_OVERWRITE(NotesCAPI.DBOPTBIT_NO_FREE_OVERWRITE),
	/** DB doesn't maintain unread marks */
	NOUNREAD(NotesCAPI.DBOPTBIT_NOUNREAD),
	/** TRUE if the database does not maintain note hierarchy info. */
	NO_RESPONSE_INFO(NotesCAPI.DBOPTBIT_NO_RESPONSE_INFO),
	/** Disabling of response info will happen on next compaction */
	DISABLE_RSP_INFO_PEND(NotesCAPI.DBOPTBIT_DISABLE_RSP_INFO_PEND),
	/** Enabling of response info will happen on next compaction */
	ENABLE_RSP_INFO_PEND(NotesCAPI.DBOPTBIT_ENABLE_RSP_INFO_PEND),
	/** Form/Bucket bitmap optimization is enabled */
	FORM_BUCKET_OPT(NotesCAPI.DBOPTBIT_FORM_BUCKET_OPT),
	/** Disabling of Form/Bucket bitmap opt will happen on next compaction */
	DISABLE_FORMBKT_PEND(NotesCAPI.DBOPTBIT_DISABLE_FORMBKT_PEND),
	/** Enabling of Form/Bucket bitmap opt will happen on next compaction */
	ENABLE_FORMBKT_PEND(NotesCAPI.DBOPTBIT_ENABLE_FORMBKT_PEND),
	/** If TRUE, maintain LastAccessed */
	MAINTAIN_LAST_ACCESSED(NotesCAPI.DBOPTBIT_MAINTAIN_LAST_ACCESSED),
	/** If TRUE, transaction logging is disabled for this database */
	DISABLE_TXN_LOGGING(NotesCAPI.DBOPTBIT_DISABLE_TXN_LOGGING),
	/** If TRUE, monitors can't be used against this database (non-replicating) */
	MONITORS_NOT_ALLOWED(NotesCAPI.DBOPTBIT_MONITORS_NOT_ALLOWED),
	/** If TRUE, all transactions on this database are nested top actions */
	NTA_ALWAYS(NotesCAPI.DBOPTBIT_NTA_ALWAYS),
	/** If TRUE, objects are not to be logged */
	DONTLOGOBJECTS(NotesCAPI.DBOPTBIT_DONTLOGOBJECTS),
	/** If set, the default delete is soft. Can be overwritten by UPDATE_DELETE_HARD */
	DELETES_ARE_SOFT(NotesCAPI.DBOPTBIT_DELETES_ARE_SOFT),

	/* The following bits are used by the webserver and are gotten from the icon note */
	
	/** if TRUE, the Db needs to be opened using SSL over HTTP */
	HTTP_DBIS_SSL(NotesCAPI.DBOPTBIT_HTTP_DBIS_SSL),
	/** if TRUE, the Db needs to use JavaScript to render the HTML for formulas, buttons, etc */
	HTTP_DBIS_JS(NotesCAPI.DBOPTBIT_HTTP_DBIS_JS),
	/** if TRUE, there is a $DefaultLanguage value on the $icon note */
	HTTP_DBIS_MULTILANG(NotesCAPI.DBOPTBIT_HTTP_DBIS_MULTILANG),

	/* ODS37+ options */
	
	/** if TRUE, database is a mail.box (ODS37 and up) */
	IS_MAILBOX(NotesCAPI.DBOPTBIT_IS_MAILBOX),
	/** if TRUE, database is allowed to have /gt;64KB UNK table */
	LARGE_UNKTABLE(NotesCAPI.DBOPTBIT_LARGE_UNKTABLE),
	/** If TRUE, full-text index is accent sensitive */
	ACCENT_SENSITIVE_FT(NotesCAPI.DBOPTBIT_ACCENT_SENSITIVE_FT),
	/** TRUE if database has NSF support for IMAP enabled */
	IMAP_ENABLED(NotesCAPI.DBOPTBIT_IMAP_ENABLED),
	/** TRUE if database is a USERless N&amp;A Book */
	USERLESS_NAB(NotesCAPI.DBOPTBIT_USERLESS_NAB),
	/** TRUE if extended ACL's apply to this Db */
	EXTENDED_ACL(NotesCAPI.DBOPTBIT_EXTENDED_ACL),
	/** TRUE if connections to = 3;rd party DBs are allowed */
	DECS_ENABLED(NotesCAPI.DBOPTBIT_DECS_ENABLED),
	/** TRUE if a = 1;+ referenced shared template. Sticky bit once referenced. */
	IS_SHARED_TEMPLATE(NotesCAPI.DBOPTBIT_IS_SHARED_TEMPLATE),
	/** TRUE if database is a mailfile */
	IS_MAILFILE(NotesCAPI.DBOPTBIT_IS_MAILFILE),
	/** TRUE if database is a web application */
	IS_WEBAPPLICATION(NotesCAPI.DBOPTBIT_IS_WEBAPPLICATION),
	/** TRUE if the database should not be accessible via the standard URL syntax */
	HIDE_FROM_WEB(NotesCAPI.DBOPTBIT_HIDE_FROM_WEB),
	/** TRUE if database contains one or more enabled background agent */
	ENABLED_BGAGENT(NotesCAPI.DBOPTBIT_ENABLED_BGAGENT),
	/** database supports LZ1 compression. */
	LZ1(NotesCAPI.DBOPTBIT_LZ1),
	/** TRUE if database has default language */
	HTTP_DBHAS_DEFLANG(NotesCAPI.DBOPTBIT_HTTP_DBHAS_DEFLANG),
	/** TRUE if database design refresh is only on admin server */
	REFRESH_DESIGN_ON_ADMIN(NotesCAPI.DBOPTBIT_REFRESH_DESIGN_ON_ADMIN),
	/** TRUE if shared template should be actively used to merge in design. */
	ACTIVE_SHARED_TEMPLATE(NotesCAPI.DBOPTBIT_ACTIVE_SHARED_TEMPLATE),
	/** TRUE to allow the use of themes when displaying the application. */
	APPLY_THEMES(NotesCAPI.DBOPTBIT_APPLY_THEMES),
	/** TRUE if unread marks replicate */
	UNREAD_REPLICATION(NotesCAPI.DBOPTBIT_UNREAD_REPLICATION),
	
	/** TRUE if unread marks replicate out of the cluster */
	UNREAD_REP_OUT_OF_CLUSTER(NotesCAPI.DBOPTBIT_UNREAD_REP_OUT_OF_CLUSTER),
	
	/** TRUE, if the mail file is a migrated one from Exchange */
	IS_MIGRATED_EXCHANGE_MAILFILE(NotesCAPI.DBOPTBIT_IS_MIGRATED_EXCHANGE_MAILFILE),
	
	/** TRUE, if the mail file is a migrated one from Exchange */
	NEED_EX_NAMEFIXUP(NotesCAPI.DBOPTBIT_NEED_EX_NAMEFIXUP),
	
	/** TRUE, if out of office service is enabled in a mail file */
	OOS_ENABLED(NotesCAPI.DBOPTBIT_OOS_ENABLED),
	
	/** TRUE if Support Response Threads is enabled in database */
	SUPPORT_RESP_THREADS(NotesCAPI.DBOPTBIT_SUPPORT_RESP_THREADS),
	
	/**TRUE if the database search is disabled. Give the admin a mechanism to prevent db search in scenarios
	 * where the db is very large, they don't want to create new views, and they
	 * don't want a full text index
	 */
	NO_SIMPLE_SEARCH(NotesCAPI.DBOPTBIT_NO_SIMPLE_SEARCH),
	
	/** TRUE if the database FDO is repaired to proper coalation function. */
	FDO_REPAIRED(NotesCAPI.DBOPTBIT_FDO_REPAIRED),
	
	/** TRUE if the policy settings have been removed from a db with no policies */
	POLICIES_REMOVED(NotesCAPI.DBOPTBIT_POLICIES_REMOVED),
	
	/** TRUE if Superblock is compressed. */
	COMPRESSED_SUPERBLOCK(NotesCAPI.DBOPTBIT_COMPRESSED_SUPERBLOCK),
	
	/** TRUE if design note non-summary should be compressed */
	COMPRESSED_DESIGN_NS(NotesCAPI.DBOPTBIT_COMPRESSED_DESIGN_NS),
	
	/** TRUE if the db has opted in to use DAOS */
	DAOS_ENABLED(NotesCAPI.DBOPTBIT_DAOS_ENABLED),

	/** TRUE if all data documents in database should be compressed (compare with DBOPTBIT_COMPRESSED_DESIGN_NS) */
	COMPRESSED_DATA_DOCS(NotesCAPI.DBOPTBIT_COMPRESSED_DATA_DOCS),
	
	/** TRUE if views in this database should be skipped by server-side update task */
	DISABLE_AUTO_VIEW_UPDS(NotesCAPI.DBOPTBIT_DISABLE_AUTO_VIEW_UPDS),
	
	/** if TRUE, Domino can suspend T/L check for DAOS items because the dbtarget is expendable */
	DAOS_LOGGING_NOT_REQD(NotesCAPI.DBOPTBIT_DAOS_LOGGING_NOT_REQD),
	
	/** TRUE if exporting of view data is to be disabled */
	DISABLE_VIEW_EXPORT(NotesCAPI.DBOPTBIT_DISABLE_VIEW_EXPORT),
	
	/** TRUE if database is a NAB which contains config information, groups, and mailin databases but where users are stored externally. */
	USERLESS2_NAB(NotesCAPI.DBOPTBIT_USERLESS2_NAB),
	
	/** LLN2 specific, added to this codestream to reserve this value */
	ADVANCED_PROP_OVERRIDE(NotesCAPI.DBOPTBIT_ADVANCED_PROP_OVERRIDE),
	
	/** Turn off VerySoftDeletes for ODS51 */
	NO_VSD(NotesCAPI.DBOPTBIT_NO_VSD),
	
	/** NSF is to be used as a cache */
	LOCAL_CACHE(NotesCAPI.DBOPTBIT_LOCAL_CACHE),
	
	/** Set to force next compact to be out of place. Initially done for ODS upgrade of in use Dbs, but may have other uses down the road. The next compact will clear this bit, it is transitory. */
	COMPACT_NO_INPLACE(NotesCAPI.DBOPTBIT_COMPACT_NO_INPLACE),
	/** from LLN2 */
	NEEDS_ZAP_LSN(NotesCAPI.DBOPTBIT_NEEDS_ZAP_LSN),
	
	/** set to indicate this is a system db (eg NAB, mail.box, etc) so we don't rely on the db name */
	IS_SYSTEM_DB(NotesCAPI.DBOPTBIT_IS_SYSTEM_DB),
	
	/** TRUE if the db has opted in to use PIRC */
	PIRC_ENABLED(NotesCAPI.DBOPTBIT_PIRC_ENABLED),
	
	/** from lln2 */
	DBMT_FORCE_FIXUP(NotesCAPI.DBOPTBIT_DBMT_FORCE_FIXUP),
	
	/** TRUE if the db has likely a complete design replication - for PIRC control */
	DESIGN_REPLICATED(NotesCAPI.DBOPTBIT_DESIGN_REPLICATED),
	
	/** on the = 1;-&gt;0 transition rename the file (for LLN2 keep in sync please) */
	MARKED_FOR_PENDING_DELETE(NotesCAPI.DBOPTBIT_MARKED_FOR_PENDING_DELETE),
	
	IS_NDX_DB(NotesCAPI.DBOPTBIT_IS_NDX_DB),
	
	/** move NIF containers &amp; collection objects out of nsf into .ndx db */
	SPLIT_NIF_DATA(NotesCAPI.DBOPTBIT_SPLIT_NIF_DATA),
	
	/** NIFNSF is off but not all containers have been moved out yet */
	NIFNSF_OFF(NotesCAPI.DBOPTBIT_NIFNSF_OFF),
	
	/** Inlined indexing exists for this DB */
	INLINE_INDEX(NotesCAPI.DBOPTBIT_INLINE_INDEX),
	
	/** db solr search enabled */
	SOLR_SEARCH(NotesCAPI.DBOPTBIT_SOLR_SEARCH),
	
	/** init solr index done */
	SOLR_SEARCH_INIT_DONE(NotesCAPI.DBOPTBIT_SOLR_SEARCH_INIT_DONE),
	
	/** Folder sync enabled for database (sync Drafts, Sent and Trash views to IMAP folders) */
	IMAP_FOLDERSYNC(NotesCAPI.DBOPTBIT_IMAP_FOLDERSYNC),
	
	/** Large Summary Support (LSS) */
	LARGE_BUCKETS_ENABLED(NotesCAPI.DBOPTBIT_LARGE_BUCKETS_ENABLED);

	private int m_val;

	DatabaseOption(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}

	/**
	 * Converts a numeric constant to a db option bit
	 * 
	 * @param value constant
	 * @return db option bit
	 */
	public static DatabaseOption toType(int value) {
		for (DatabaseOption currOption : values()) {
			if (value == currOption.getValue())
				return currOption;
		}
		throw new IllegalArgumentException("Unknown constant: "+value);
	}
}
