CREATE TABLE IF NOT EXISTS dominodocs (
	__unid text UNIQUE NOT NULL PRIMARY KEY,
	__seq integer,
	__seqtime_innard0 integer,
	__seqtime_innard1 integer,
	__seqtime_millis integer,
	__readers text,
	__form text,
	__json text,
	__binarydata BLOB
);
CREATE INDEX IF NOT EXISTS dominodocs_unid ON dominodocs (__unid);
CREATE INDEX IF NOT EXISTS dominodocs_form ON dominodocs (__form);

CREATE TABLE IF NOT EXISTS syncdatainfo (
	dbid text,
	selectionformula text
);

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
