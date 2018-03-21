/* Generic table that will receive all domino documents.
 * 
 * The first block of columns is required for the sync (e.g. to find document
 * information like the sequence time, basically "last modified", by the doc UNID.
 * 
 * The field __modifiedinthisfile_millis is a reserved field to support a
 * later sync in opposite direction.
 * 
 * The __readers column in the third block contains a JSON array of all readers and
 * authors (if readers are present) stored in the document to check for read access.
 * Values are converted to lowercase, because reader items in Domino are case-insensitive.
 * 
 * The fourth block contains the actual document content, e.g. the "form" value for optimized
 * filtering of documents with a specific type, a JSON object with all relevant document data
 * and a BLOB that is currently not used. */
CREATE TABLE IF NOT EXISTS docs (
	__unid text UNIQUE NOT NULL PRIMARY KEY,
	__seq integer,
	__seqtime_innard0 integer,
	__seqtime_innard1 integer,
	__seqtime_millis integer,

	__modifiedinthisfile_millis integer,

	__readers text,
	
	__flags text,
	__form text,
	__json text,
	__binarydata BLOB
);

/* Create indexes to speed up UNID and FORM based searches */
CREATE INDEX IF NOT EXISTS docs_unid ON docs (__unid);
CREATE INDEX IF NOT EXISTS docs_form ON docs (__form);

CREATE TABLE IF NOT EXISTS attachments (
	__unid text NOT NULL PRIMARY KEY,
	
	__created_millis integer,
	__created_innard0 integer,
	__created_innard1 integer,
	
	__modified_millis integer,
	__modified_innard0 integer,
	__modified_innard1 integer
	
	__modifiedinthisfile_millis integer,

	__filesize integer,
	__filename text,
	
	__flags text,
	__json text,
	__binarydata BLOB
);

CREATE INDEX IF NOT EXISTS attachments_unid ON attachments (__unid);

/* This table will only contain a single line with the db replica id,
 * and the selection formula used for the last successful sync run and optional custom data */
CREATE TABLE IF NOT EXISTS syncdatainfo (
	dbid text,
	selectionformula text,
	customdata text
);

/* For each DB instance that syncs with the SQL database, we track the
 * cutoff date where to start the nexz NSF search to find added/changes/deleted
 * documents. The DB instance ID is computed from the DB replica ID, the current server
 * and the creation date of the NSF file on disk.
 * The "json" columns is currently not used.
 */
CREATE TABLE IF NOT EXISTS synchistory (
	dbinstanceid text PRIMARY KEY,
	startdatetime integer,
	enddatetime integer,
	
	added integer,
	changed integer,
	removed integer,
	
	newcutoffdate_innard0 integer,
	newcutoffdate_innard1 integer,
	newcutoffdate_millis integer,
	
	json text
);
