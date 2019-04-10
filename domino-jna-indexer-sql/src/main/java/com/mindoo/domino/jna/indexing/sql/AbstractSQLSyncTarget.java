package com.mindoo.domino.jna.indexing.sql;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.json.JSONArray;

import com.mindoo.domino.jna.IItemTableData;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.sync.ISyncTarget;
import com.mindoo.domino.jna.sync.NotesOriginatorIdData;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;

/**
 * Abstract sync target class to sync Domino data into a SQL database
 * 
 * @author Karsten Lehmann
 */
public abstract class AbstractSQLSyncTarget implements ISyncTarget<AbstractSQLSyncTarget.SyncContext> {
	private static final String SQL_FLUSH_LASTSYNCDATA = "DELETE FROM syncdatainfo;";
	private static final String SQL_FLUSH_DOCS = "DELETE FROM docs;";
	private String m_jdbcUrl;
	private Connection m_conn;

	private static final String SQL_REMOVE_DOC_BY_UNID = "DELETE from docs where __unid = ?";
	private static final String SQL_INSERT_DOMINODOC = "INSERT INTO docs ("
			+ "__unid, "
			+ "__seq, "
			+ "__seqtime_innard0, "
			+ "__seqtime_innard1, "
			+ "__seqtime_millis, "
			+ "__modifiedinthisfile_millis, "
			+ "__numreaders, "
			+ "__flags, "
			+ "__form, "
			+ "__json, "
			+ "__customtext, "
			+ "__custombinary) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String SQL_INSERT_DOMINODOCREADERS = "INSERT INTO docreaders ("
			+ "__unid, "
			+ "__reader) VALUES (?, ?)";
	private static final String SQL_UPDATE_DOMINODOC = "UPDATE docs SET "
			+ "__unid = ? , "
			+ "__seq = ?, "
			+ "__seqtime_innard0 = ?, "
			+ "__seqtime_innard1 = ?, "
			+ "__seqtime_millis = ?, "
			+ "__modifiedinthisfile_millis = ?, "
			+ "__numreaders = ?, "
			+ "__flags = ?, "
			+ "__form = ?, "
			+ "__json = ?, "
			+ "__customtext = ?, "
			+ "__custombinary = ? "
			+ "WHERE __unid = ?";
	
	private static final String SQL_REMOVE_DOCREADERS_BY_UNID = "DELETE from docreaders where __unid = ?";

	private static final String SQL_INSERTORREPLACE_HISTORYENTRY = "INSERT OR REPLACE INTO synchistory ("
			+ "dbinstanceid, "
			+ "startdatetime, "
			+ "enddatetime, "
			+ "added, "
			+ "changed, "
			+ "removed, "
			+ "newcutoffdate_innard0, "
			+ "newcutoffdate_innard1, "
			+ "newcutoffdate_millis, "
			+ "json) " +
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String SQL_INSERTORREPLACE_LASTSYNCDATAINFO = "INSERT OR REPLACE INTO syncdatainfo ("
			+ "dbid, "
			+ "selectionformula, "
			+ "customdata) " +
			"VALUES (?, ?, ?)";
	private static final String SQL_FINDDOCBYUNID = "SELECT "
			+ "__unid, "
			+ "__seq, "
			+ "__seqtime_innard0, "
			+ "__seqtime_innard1 "
			+ "FROM docs where __unid=? LIMIT 1;";
	private static final String SQL_SCANDATABASE = "SELECT "
			+ "__unid, "
			+ "__seq, "
			+ "__seqtime_innard0, "
			+ "__seqtime_innard1 "
			+ "FROM docs;";
	private static final String SQL_GETLASTSYNCDBREPLICAID = "SELECT dbid "
			+ "FROM syncdatainfo LIMIT 1;";
	private static final String SQL_GETLASTSYNCSELECTIONFORMULA = "SELECT selectionformula "
			+ "FROM syncdatainfo LIMIT 1;";
	private static final String SQL_GETLASTSYNCCUSTOMDATA = "SELECT customdata "
			+ "FROM syncdatainfo LIMIT 1;";
	private static final String SQL_GETLASTSYNCENDDATE = "SELECT newcutoffdate_innard0, "
			+ "newcutoffdate_innard1 "
			+ "FROM synchistory WHERE dbinstanceid=? LIMIT 1;";
	
	/**
	 * Context class that captures all relevant data for a single sync run
	 * 
	 * @author Karsten Lehmann
	 */
	public static class SyncContext {
		private ISyncTarget<AbstractSQLSyncTarget.SyncContext> target;
		private String dbId;
		private long startDateTime;
		private long endDateTime;
		private int added;
		private int changed;
		private int removed;
		private PreparedStatement m_stmtFindDominoDocByUnid;
		private PreparedStatement m_stmtRemoveDominoDocByUnid;
		private PreparedStatement m_stmtInsertDominoDoc;
		private PreparedStatement m_stmtInsertDominoDocReaders;
		private PreparedStatement m_stmtUpdateDominoDoc;
		private PreparedStatement m_stmtDeleteAllDominoDocReaders;
		
		public SyncContext(ISyncTarget<AbstractSQLSyncTarget.SyncContext> target) {
			this.target = target;
		}

		public void dispose() {
			if (m_stmtFindDominoDocByUnid!=null) {
				try {
					m_stmtFindDominoDocByUnid.close();
				} catch (SQLException e1) {
					target.log(Level.SEVERE, "Error closing statement", e1);
				}
				m_stmtFindDominoDocByUnid = null;
			}
			
			if (m_stmtInsertDominoDoc!=null) {
				try {
					m_stmtInsertDominoDoc.close();
				} catch (SQLException e1) {
					target.log(Level.SEVERE, "Error closing statement", e1);
				}
				m_stmtInsertDominoDoc = null;
			}
			
			if (m_stmtRemoveDominoDocByUnid!=null) {
				try {
					m_stmtRemoveDominoDocByUnid.close();
				} catch (SQLException e1) {
					target.log(Level.SEVERE, "Error closing statement", e1);
				}
				m_stmtRemoveDominoDocByUnid = null;
			}

			if (m_stmtUpdateDominoDoc!=null) {
				try {
					m_stmtUpdateDominoDoc.close();
				} catch (SQLException e1) {
					target.log(Level.SEVERE, "Error closing statement", e1);
				}
				m_stmtUpdateDominoDoc = null;
			}
			
			if (m_stmtRemoveDominoDocByUnid!=null) {
				try {
					m_stmtRemoveDominoDocByUnid.close();
				} catch (SQLException e1) {
					target.log(Level.SEVERE, "Error closing statement", e1);
				}
				m_stmtRemoveDominoDocByUnid = null;
			}
			
			if (m_stmtInsertDominoDocReaders!=null) {
				try {
					m_stmtInsertDominoDocReaders.close();
				} catch (SQLException e1) {
					target.log(Level.SEVERE, "Error closing statement", e1);
				}
				m_stmtInsertDominoDocReaders = null;
			}
			
			if (m_stmtDeleteAllDominoDocReaders!=null) {
				try {
					m_stmtDeleteAllDominoDocReaders.close();
				} catch (SQLException e1) {
					target.log(Level.SEVERE, "Error closing statement", e1);
				}
				m_stmtDeleteAllDominoDocReaders = null;
			}
			this.target = null;
		}
		
		public String getDbId() {
			return dbId;
		}

		public void setDbId(String dbId) {
			this.dbId = dbId;
		}

		public long getStartDateTime() {
			return startDateTime;
		}

		public void setStartDateTime(long startDateTime) {
			this.startDateTime = startDateTime;
		}

		public long getEndDateTime() {
			return endDateTime;
		}

		public void setEndDateTime(long endDateTime) {
			this.endDateTime = endDateTime;
		}

		public int getAdded() {
			return added;
		}

		public void setAdded(int added) {
			this.added = added;
		}

		public int getChanged() {
			return changed;
		}

		public void setChanged(int changed) {
			this.changed = changed;
		}

		public int getRemoved() {
			return removed;
		}

		public void setRemoved(int removed) {
			this.removed = removed;
		}

		public PreparedStatement getStatementRemoveDominoDocByUnid() {
			return m_stmtRemoveDominoDocByUnid;
		}

		public void setStatementRemoveDominoDocByUnid(PreparedStatement stmt) {
			this.m_stmtRemoveDominoDocByUnid = stmt;
		}

		public PreparedStatement getStatementInsertDominoDoc() {
			return m_stmtInsertDominoDoc;
		}
		
		public void setStatementInsertDominoDoc(PreparedStatement stmt) {
			this.m_stmtInsertDominoDoc = stmt;
		}

		public PreparedStatement getStatementInsertDominoDocReaders() {
			return m_stmtInsertDominoDocReaders;
		}

		public void setStatementInsertDominoDocReaders(PreparedStatement stmt) {
			this.m_stmtInsertDominoDocReaders = stmt;
		}

		public PreparedStatement getStatementUpdateDominoDoc() {
			return m_stmtUpdateDominoDoc;
		}

		public void setStatementUpdateDominoDoc(PreparedStatement stmt) {
			this.m_stmtUpdateDominoDoc = stmt;
		}

		public PreparedStatement getStatementDeleteAllDocReaders() {
			return m_stmtDeleteAllDominoDocReaders;
		}
		
		public void setStatementDeleteAllDocReaders(PreparedStatement stmt) {
			this.m_stmtDeleteAllDominoDocReaders = stmt;
		}
		
		public PreparedStatement getStatementFindDominoDocByUnid() {
			return m_stmtFindDominoDocByUnid;
		}

		public void setStatementFindDominoDocByUnid(PreparedStatement stmt) {
			this.m_stmtFindDominoDocByUnid = stmt;
		}
	}

	/**
	 * Creates a new sync target for the specified JDBC url
	 * 
	 * @param jdbcUrl JDBC url
	 */
	public AbstractSQLSyncTarget(String jdbcUrl) {
		m_jdbcUrl = jdbcUrl;
	}

	protected PreparedStatement createStatementRemoveDominoDocByUnid() throws SQLException {
		return getConnection().prepareStatement(SQL_REMOVE_DOC_BY_UNID);
	}

	protected PreparedStatement createStatementInsertDominoDoc() throws SQLException {
		return getConnection().prepareStatement(SQL_INSERT_DOMINODOC);
	}

	protected PreparedStatement createStatementInsertDominoDocReaders() throws SQLException {
		return getConnection().prepareStatement(SQL_INSERT_DOMINODOCREADERS);
	}
	
	protected PreparedStatement createStatementUpdateDominoDoc() throws SQLException {
		return getConnection().prepareStatement(SQL_UPDATE_DOMINODOC);
	}

	protected PreparedStatement createStatementInsertHistoryEntry() throws SQLException {
		return getConnection().prepareStatement(SQL_INSERTORREPLACE_HISTORYENTRY);
	}

	protected PreparedStatement createStatementInsertLastSyncDataInfoEntry() throws SQLException {
		return getConnection().prepareStatement(SQL_INSERTORREPLACE_LASTSYNCDATAINFO);
	}

	protected PreparedStatement createStatementFindDocumentByUnid() throws SQLException {
		return getConnection().prepareStatement(SQL_FINDDOCBYUNID);
	}

	protected PreparedStatement createStatementScanDatabase() throws SQLException {
		return getConnection().prepareStatement(SQL_SCANDATABASE);
	}
	
	protected PreparedStatement createStatementDeleteAllDocReaders() throws SQLException {
		return getConnection().prepareStatement(SQL_REMOVE_DOCREADERS_BY_UNID);
	}
	
	protected abstract DataSource createDataSource(String jdbcUrl);

	protected String[] getDbMigrationLocations() {
		return new String[] {"classpath:db/migration"};
	}

	public Connection getConnection() throws SQLException {
		if (m_conn==null) {
			m_conn = createConnection();
		}
		return m_conn;
	}

	protected String getJdbcUrl() {
		return m_jdbcUrl;
	}
	
	protected Connection createConnection() throws SQLException {
		//use Flyway to initialize the database and migrate it between versions
		Flyway flyway;
		ClassLoader cl = getFlywayClassLoader();
		if (cl!=null) {
			flyway = new Flyway(cl);
		}
		else {
			flyway = new Flyway();
		}

		DataSource ds = createDataSource(getJdbcUrl());
		flyway.setDataSource(ds);

		//set migration file locations
		flyway.setLocations(getDbMigrationLocations());

		// Force the creation of 'schema_version' table on existing database.
		flyway.setBaselineOnMigrate(true);

		flyway.setTarget(MigrationVersion.fromVersion(getDbMainVersion()+"."+getDbSubVersion()));
		
		postInitFlyway(flyway);
		
		// migrate db schemas and data
		flyway.migrate();

		return ds.getConnection();
	}

	/**
	 * Add your own custom code here to init the flyway instance used
	 * for DB schema upgrades
	 * 
	 * @param flyway flyway
	 */
	protected void postInitFlyway(Flyway flyway) {
		//
	}
	
	/**
	 * Override this method to return a classloader that should be used by
	 * Flyway to load the DB migrations. The default implementation returns null which
	 * lets Flyway use <code>Thread.currentThread().getContextClassLoader()</code>.
	 * 
	 * @return classloader or null
	 */
	protected ClassLoader getFlywayClassLoader() {
		return null;
	}
	
	/**
	 * Returns the main version of the SQL database for the Flyway DB migration.
	 * This method is final because Domino JNA controls this part of the version so
	 * that updates to this sync target implementation class can enforce a db schema
	 * migration
	 * 
	 * @return version, currently "1"
	 */
	protected final String getDbMainVersion() {
		return "1";
	}

	/**
	 * Returns the sub version of the SQL database for the Flyway DB migration.
	 * Add your own app specific sub versioning here so that you can run your own
	 * DB migration scripts after the scripts of Domino JNA. The value 
	 * returned by {@link #getDbMainVersion()} combined with the sub version produce
	 * the version string to be passed to Flyway, e.g. "1.1", which let's Flyway
	 * run the V1__...sql migration script and an app specific V1.1__..sql as well.
	 * 
	 * @return sub version, either a value like "1" (the default) or "1.2", "1.2.3" etc.
	 */
	protected String getDbSubVersion() {
		return "1";
	}
	
	public String getLastSyncDbReplicaId() {
		PreparedStatement readDbidStmt = null;

		try {
			readDbidStmt = getConnection().prepareStatement(SQL_GETLASTSYNCDBREPLICAID);

			ResultSet rs = readDbidStmt.executeQuery();

			if (rs.next()) {
				String dbid = rs.getString("dbid");
				return dbid;
			}
			return null;
		} catch (SQLException e) {
			throw new SqlSyncException("Error reading last sync db replica id", e);
		}
		finally {
			try {
				if (readDbidStmt != null) {
					readDbidStmt.close();
				}
			} catch (SQLException ex) {
				log(Level.SEVERE, "Could not close statement to read last sync db replica id: "+SQL_GETLASTSYNCDBREPLICAID, ex);
			}
		}
	}

	@Override
	public NotesIDTable getInitialNoteIdFilter() {
		return null;
	}
	
	public String getLastSyncSelectionFormula() {
		PreparedStatement readFormulaStmt = null;

		try {
			readFormulaStmt = getConnection().prepareStatement(SQL_GETLASTSYNCSELECTIONFORMULA);

			ResultSet rs = readFormulaStmt.executeQuery();

			if (rs.next()) {
				String selectionformula = rs.getString("selectionformula");
				return selectionformula;
			}
			return null;
		} catch (SQLException e) {
			throw new SqlSyncException("Error reading last sync selectionformula", e);
		}
		finally {
			try {
				if (readFormulaStmt != null) {
					readFormulaStmt.close();
				}
			} catch (SQLException ex) {
				log(Level.SEVERE, "Could not close statement to read last sync selectionformula: "+SQL_GETLASTSYNCSELECTIONFORMULA, ex);
			}
		}
	}

	public String getLastSyncCustomData() {
		PreparedStatement readCustomDataStmt = null;

		try {
			readCustomDataStmt = getConnection().prepareStatement(SQL_GETLASTSYNCCUSTOMDATA);

			ResultSet rs = readCustomDataStmt.executeQuery();

			if (rs.next()) {
				String customData = rs.getString("customdata");
				return customData;
			}
			return null;
		} catch (SQLException e) {
			throw new SqlSyncException("Error reading last sync customdata", e);
		}
		finally {
			try {
				if (readCustomDataStmt != null) {
					readCustomDataStmt.close();
				}
			} catch (SQLException ex) {
				log(Level.SEVERE, "Could not close statement to read last sync customdata: "+SQL_GETLASTSYNCCUSTOMDATA, ex);
			}
		}
	}
	
	public NotesTimeDate getLastSyncEndDate(String dbInstanceId) {
		PreparedStatement readCutOffDateStmt = null;

		try {
			readCutOffDateStmt = getConnection().prepareStatement(SQL_GETLASTSYNCENDDATE);
			readCutOffDateStmt.setString(1, dbInstanceId);

			ResultSet rs = readCutOffDateStmt.executeQuery();

			if (rs.next()) {
				int cutoffDateInnard0 = rs.getInt("newcutoffdate_innard0");
				int cutoffDateInnard1 = rs.getInt("newcutoffdate_innard1");
				return new NotesTimeDate(new int[] {cutoffDateInnard0, cutoffDateInnard1});
			}
			return null;
		} catch (SQLException e) {
			throw new SqlSyncException("Error reading last sync end date for db instance id: "+dbInstanceId, e);
		}
		finally {
			try {
				if (readCutOffDateStmt != null) {
					readCutOffDateStmt.close();
				}
			} catch (SQLException ex) {
				log(Level.SEVERE, "Could not close statement", ex);
			}
		}
	}

	public SyncContext startingSync(String dbReplicaId) {
		SyncContext ctx = new SyncContext(this);
		ctx.setStartDateTime(System.currentTimeMillis());
		ctx.setDbId(dbReplicaId);
		
		//create reused prepared statements to document insertion/update/removal
		try {
			ctx.setStatementFindDominoDocByUnid(createStatementFindDocumentByUnid());
		}
		catch (SQLException e) {
			throw new SqlSyncException("Error creating prepared statement to find domino document by UNID", e);
		}
		try {
			ctx.setStatementInsertDominoDoc(createStatementInsertDominoDoc());
		}
		catch (SQLException e) {
			throw new SqlSyncException("Error creating prepared statement to insert domino document", e);
		}
		try {
			ctx.setStatementInsertDominoDocReaders(createStatementInsertDominoDocReaders());
		}
		catch (SQLException e) {
			throw new SqlSyncException("Error creating prepared statement to insert domino document readers", e);
		}
		try {
			ctx.setStatementRemoveDominoDocByUnid(createStatementRemoveDominoDocByUnid());
		}
		catch (SQLException e) {
			throw new SqlSyncException("Error creating prepared statement to remove domino document", e);
		}
		try {
			ctx.setStatementDeleteAllDocReaders(createStatementDeleteAllDocReaders());
		}
		catch (SQLException e) {
			throw new SqlSyncException("Error creating prepared statement to remove domino document readers", e);
		}
		try {
			ctx.setStatementUpdateDominoDoc(createStatementUpdateDominoDoc());
		}
		catch (SQLException e) {
			throw new SqlSyncException("Error creating prepared statement to update domino document", e);
		}
		
		try {
			getConnection().setAutoCommit(false);
		} catch (SQLException e) {
			throw new SqlSyncException("Error starting new sync transaction", e);
		}
		return ctx;
	}

	public void clear(SyncContext ctx) {
		Statement stmt = null;
		try {
			stmt = getConnection().createStatement();
			stmt.executeUpdate(SQL_FLUSH_DOCS);
		} catch (SQLException e) {
			throw new SqlSyncException("Error deleting content of table docs", e);
		}
		finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					throw new SqlSyncException("Error closing statement", e);
				}
			}
		}
	}

	public List<NotesOriginatorIdData> scanTargetData(SyncContext ctx) {
		List<NotesOriginatorIdData> entries = new ArrayList<NotesOriginatorIdData>();

		PreparedStatement readAllDocsStmt = null;

		try {
			readAllDocsStmt = createStatementScanDatabase();
			ResultSet rs = readAllDocsStmt.executeQuery();

			while (rs.next()) {
				String currUnid = rs.getString("__unid");
				int currSeq = rs.getInt("__seq");
				int currSeqTimeInnard0 = rs.getInt("__seqtime_innard0");
				int currSeqTimeInnard1 = rs.getInt("__seqtime_innard1");

				NotesOriginatorIdData oidData = new NotesOriginatorIdData(currUnid, currSeq,
						new int[] {currSeqTimeInnard0, currSeqTimeInnard1});
				entries.add(oidData);
			}
			return entries;
		} catch (SQLException e) {
			throw new SqlSyncException("Error scanning table docs of database "+m_jdbcUrl, e);
		}
		finally {
			try {
				if (readAllDocsStmt != null) {
					readAllDocsStmt.close();
				}
			} catch (SQLException ex) {
				log(Level.SEVERE, "Could not close statement", ex);
			}
		}

	}

	@Override
	public abstract EnumSet<DataToRead> getWhichDataToRead();

	/**
	 * Implement this method to convert summary buffer or {@link NotesNote} data
	 * to an index object.
	 * 
	 * @param oid originator id of Domino data
	 * @param summaryBufferData summary buffer if {@link #getWhichDataToRead()} returned {@link DataToRead#SummaryBufferAllItems} or {@link DataToRead#SummaryBufferSelectedItems}, null otherwise
	 * @param note note if {@link #getWhichDataToRead()} returned {@link DataToRead#NoteWithAllItems} or {@link DataToRead#NoteWithSummaryItems}, null otherwise
	 */

	public TargetResult noteChangedMatchingFormula(SyncContext ctx, NotesOriginatorIdData oid, IItemTableData summaryBufferData,
			NotesNote note) {
		NotesOriginatorIdData oidInDb = findDocumentByUnid(ctx, oid);

		List<String> readers = getReaders(oid, summaryBufferData, note);
		if (readers!=null) {
			//convert to lowercase, because reader fields are case-insensitive
			List<String> readersLC = new ArrayList<String>(readers.size());
			for (String currReader : readers) {
				readersLC.add(currReader.toLowerCase(Locale.ENGLISH));
			}
			readers = readersLC;
		}

		if (oidInDb==null) {
			try {
				PreparedStatement insertDocStmt = ctx.getStatementInsertDominoDoc();
				insertDocumentRowWithData(ctx, insertDocStmt, oid, summaryBufferData, note, readers);
			}
			catch (SQLException e) {
				throw new SqlSyncException("Error inserting note with UNID "+oid.getUNID(), e);
			}
			return TargetResult.Added;
		}
		else {
			try {
				PreparedStatement updateDocStmt = ctx.getStatementUpdateDominoDoc();
				updateDocumentRowWithData(ctx, updateDocStmt, oid, summaryBufferData, note, readers);
			}
			catch (SQLException e) {
				throw new SqlSyncException("Error updating note with UNID "+oid.getUNID(), e);
			}
			return TargetResult.Updated;
		}
	}

	/**
	 * Method to fill the parameters of an update statement that updates an
	 * existing document in the database
	 * 
	 * @param ctx sync context
	 * @param stmt update statement
	 * @param oid note originator id
	 * @param summaryBufferData summary buffer if specified in {@link #getWhichDataToRead()}
	 * @param note note  if specified in {@link #getWhichDataToRead()}
	 * @param readers readers of this note converted to lowercase or null if there are no restrictions
	 * @throws SQLException in case of SQL errors
	 */
	private void updateDocumentRowWithData(SyncContext ctx, PreparedStatement stmt,
			NotesOriginatorIdData oid, IItemTableData summaryBufferData,
			NotesNote note, List<String> readers) throws SQLException {

		String unid = oid.getUNID();
		int seq = oid.getSequence();
		int[] seqTimeInnards = oid.getSequenceTimeInnards();

		stmt.setString(1, unid);
		stmt.setInt(2, seq);
		stmt.setLong(3, seqTimeInnards[0]);
		stmt.setLong(4, seqTimeInnards[1]);
		Calendar seqTimeCal = NotesDateTimeUtils.innardsToCalendar(seqTimeInnards);
		stmt.setLong(5, seqTimeCal.getTimeInMillis());
		stmt.setLong(6, seqTimeCal.getTimeInMillis());

		int numReaders;
		if (readers==null) {
			numReaders = 0;
		}
		else {
			numReaders = readers.size();
		}
		stmt.setInt(7, numReaders);

		List<String> flags = getFlags(oid, summaryBufferData, note);
		if (flags==null)
			flags=Collections.emptyList();
		stmt.setString(8, new JSONArray(flags).toString());

		String form = null;
		if (summaryBufferData!=null) {
			form = summaryBufferData.getAsString("form", null);
		}
		if (form==null && note!=null) {
			form = note.getItemValueString("form");
		}
		if (form==null)
			form = "";
		stmt.setString(9, form);

		String jsonStr = toJson(oid, summaryBufferData, note);
		if (jsonStr==null)
			jsonStr = "{}";
		stmt.setString(10, jsonStr);

		String customTextData = getCustomTextData(oid, summaryBufferData, note);
		if (customTextData!=null) {
			stmt.setString(11, customTextData);
		}
		
		byte[] customBinaryData = getCustomBinaryData(oid, summaryBufferData, note);
		if (customBinaryData!=null) {
			stmt.setBytes(12, customBinaryData);
		}

		stmt.setString(13, unid);
		
		stmt.addBatch();
		ctx.setChanged(ctx.getChanged()+1);
		if ((ctx.getChanged() % getMaxBatchSize()) == 0) {
			executeBatchedUpdates(ctx);
		}

		//flush old readers
		PreparedStatement deleteAllDocReaders = ctx.getStatementDeleteAllDocReaders();
		deleteAllDocReaders.setString(1, unid);
		deleteAllDocReaders.executeUpdate();
		
		if (readers!=null) {
			//and write new readers
			PreparedStatement insertDocReadersStmt = ctx.getStatementInsertDominoDocReaders();
			for (String currReader : readers) {
				insertDocReadersStmt.setString(1, unid);
				insertDocReadersStmt.setString(2, currReader);
				
				insertDocReadersStmt.addBatch();
			}
			insertDocReadersStmt.executeBatch();
		}
	}
	
	/**
	 * Method to fill the parameters of an insert statement that inserts a document into the database
	 * 
	 * @param ctx sync context
	 * @param stmt insert statement
	 * @param oid note originator id
	 * @param summaryBufferData summary buffer if specified in {@link #getWhichDataToRead()}
	 * @param note note  if specified in {@link #getWhichDataToRead()}
	 * @param readers readers of this note converted to lowercase or null if there are no restrictions
	 * @throws SQLException in case of SQL errors
	 */
	private void insertDocumentRowWithData(SyncContext ctx, PreparedStatement stmt,
			NotesOriginatorIdData oid, IItemTableData summaryBufferData,
			NotesNote note, List<String> readers) throws SQLException {

		String unid = oid.getUNID();
		int seq = oid.getSequence();
		int[] seqTimeInnards = oid.getSequenceTimeInnards();
		
		stmt.setString(1, unid);
		stmt.setInt(2, seq);
		stmt.setLong(3, seqTimeInnards[0]);
		stmt.setLong(4, seqTimeInnards[1]);
		Calendar seqTimeCal = NotesDateTimeUtils.innardsToCalendar(seqTimeInnards);
		stmt.setLong(5, seqTimeCal.getTimeInMillis());
		stmt.setLong(6, seqTimeCal.getTimeInMillis());

		int numReaders;
		if (readers==null) {
			numReaders = 0;
		}
		else {
			numReaders = readers.size();
		}
		stmt.setInt(7, numReaders);

		List<String> flags = getFlags(oid, summaryBufferData, note);
		if (flags==null)
			flags=Collections.emptyList();
		stmt.setString(8, new JSONArray(flags).toString());
		
		String form = null;
		if (summaryBufferData!=null) {
			form = summaryBufferData.getAsString("form", null);
		}
		if (form==null && note!=null) {
			form = note.getItemValueString("form");
		}
		if (form==null)
			form = "";
		stmt.setString(9, form);

		String jsonStr = toJson(oid, summaryBufferData, note);
		if (jsonStr==null)
			jsonStr = "{}";
		stmt.setString(10, jsonStr);

		String customTextData = getCustomTextData(oid, summaryBufferData, note);
		if (customTextData!=null) {
			stmt.setString(11, customTextData);
		}
		
		byte[] customBinaryData = getCustomBinaryData(oid, summaryBufferData, note);
		if (customBinaryData!=null) {
			stmt.setBytes(12, customBinaryData);
		}
		
		stmt.addBatch();
		ctx.setAdded(ctx.getAdded()+1);
		if ((ctx.getAdded() % getMaxBatchSize()) == 0) {
			executeBatchedInserts(ctx);
		}

		if (numReaders>0) {
			PreparedStatement insertDocReadersStmt = ctx.getStatementInsertDominoDocReaders();
			for (String currReader : readers) {
				insertDocReadersStmt.setString(1, unid);
				insertDocReadersStmt.setString(2, currReader);
				
				insertDocReadersStmt.addBatch();
			}
			insertDocReadersStmt.executeBatch();
		}
	}

	/**
	 * Override this method to read the readers and authors from specific fields. The default
	 * implementation expects that the special item "$C1$" has been specified in {@link #getSummaryBufferItemsAndFormulas()}
	 * and that {@link #getWhichDataToRead()} returns either {@link DataToRead#SummaryBufferAllItems} or
	 * {@link DataToRead#SummaryBufferSelectedItems}.
	 * 
	 * @param oid note originator id
	 * @param summaryBufferData summary buffer if specified in {@link #getWhichDataToRead()}
	 * @param note note  if specified in {@link #getWhichDataToRead()}
	 * @return readers or null if no readers stored in note
	 */
	protected List<String> getReaders(NotesOriginatorIdData oid, IItemTableData summaryBufferData,
			NotesNote note) {
		if (summaryBufferData!=null) {
			List<String> readers = summaryBufferData.getAsStringList("$C1$", null);
			return readers;
		}
		return null;
	}

	/**
	 * Override this method to return a custom text to be stored for a document in
	 * the database
	 * 
	 * @param oid note originator id
	 * @param summaryBufferData summary buffer if specified in {@link #getWhichDataToRead()}
	 * @param note note  if specified in {@link #getWhichDataToRead()}
	 * @return custom string or null  (default)
	 */
	protected String getCustomTextData(NotesOriginatorIdData oid, IItemTableData summaryBufferData,
			NotesNote note) {
		return null;
	}
	
	/**
	 * The method currently just returns an empty list. The return value is stored in the
	 * __flags column and is reserved for future use.
	 * 
	 * @param oid note originator id
	 * @param summaryBufferData summary buffer if specified in {@link #getWhichDataToRead()}
	 * @param note note  if specified in {@link #getWhichDataToRead()}
	 * @return flags (empty list)
	 */
	protected List<String> getFlags(NotesOriginatorIdData oid, IItemTableData summaryBufferData,
			NotesNote note) {
		return Collections.emptyList();
	}
	
	/**
	 * Override this method and return binary data to be stored for a document. The
	 * default implementation returns null
	 * 
	 * @param oid note originator id
	 * @param summaryBufferData summary buffer if specified in {@link #getWhichDataToRead()}
	 * @param note note  if specified in {@link #getWhichDataToRead()}
	 * @return bytes array or null
	 */
	protected byte[] getCustomBinaryData(NotesOriginatorIdData oid, IItemTableData summaryBufferData,
			NotesNote note) {
		return null;
	}

	/**
	 * Implement this method and extract relevant data from the summary buffer or note
	 * 
	 * @param oid note originator id
	 * @param summaryBufferData summary buffer if specified in {@link #getWhichDataToRead()}
	 * @param note note  if specified in {@link #getWhichDataToRead()}
	 * @return JSON string or null if there is no JSON to store
	 */
	protected abstract String toJson(NotesOriginatorIdData oid, IItemTableData summaryBufferData,
			NotesNote note);

	public TargetResult noteChangedNotMatchingFormula(SyncContext ctx, NotesOriginatorIdData oid) {
		NotesOriginatorIdData oidInDb = findDocumentByUnid(ctx, oid);
		if (oidInDb==null)
			return TargetResult.None;

		try {
			PreparedStatement removeByUnidStmt = ctx.getStatementRemoveDominoDocByUnid();
			
			removeByUnidStmt.setString(1, oid.getUNID());
			removeByUnidStmt.addBatch();
			
			ctx.setRemoved(ctx.getRemoved()+1);
			if ((ctx.getRemoved() % getMaxBatchSize()) == 0) {
				executeBatchedRemoves(ctx);
			}
			return TargetResult.Removed;
		} catch (SQLException e) {
			throw new SqlSyncException("Error deleting document with UNID "+oid.getUNID(), e);
		}
	}

	/**
	 * Searches for a document by its UNID in the database
	 * 
	 * @param ctx sync context
	 * @param oid note originator id
	 * @return document information or null if not found
	 */
	protected NotesOriginatorIdData findDocumentByUnid(SyncContext ctx, NotesOriginatorIdData oid) {
		try {
			PreparedStatement findDocStmt = ctx.getStatementFindDominoDocByUnid();
			findDocStmt.setString(1, oid.getUNID());

			ResultSet rs = findDocStmt.executeQuery();

			if (rs.next()) {
				String currUnid = rs.getString("__unid");
				int currSeq = rs.getInt("__seq");
				int currSeqTimeInnard0 = rs.getInt("__seqtime_innard0");
				int currSeqTimeInnard1 = rs.getInt("__seqtime_innard1");

				NotesOriginatorIdData oidData = new NotesOriginatorIdData(currUnid, currSeq,
						new int[] {currSeqTimeInnard0, currSeqTimeInnard1});
				return oidData;
			}
			return null;
		} catch (SQLException e) {
			throw new SqlSyncException("Error scanning table docs of database "+m_jdbcUrl+" for document with UNID "+oid.getUNID(), e);
		}
	}

	public TargetResult noteDeleted(SyncContext ctx, NotesOriginatorIdData oid) {
		NotesOriginatorIdData oidInDb = findDocumentByUnid(ctx, oid);
		if (oidInDb==null)
			return TargetResult.None;

		try {
			PreparedStatement removeByUnidStmt = ctx.getStatementRemoveDominoDocByUnid();

			removeByUnidStmt.setString(1, oid.getUNID());
			removeByUnidStmt.addBatch();
			
			ctx.setRemoved(ctx.getRemoved()+1);
			if ((ctx.getRemoved() % getMaxBatchSize()) == 0) {
				executeBatchedRemoves(ctx);
			}
			return TargetResult.Removed;
		} catch (SQLException e) {
			throw new SqlSyncException("Error deleting document with UNID "+oid.getUNID(), e);
		}
	}
	
	public boolean isLoggable(Level level) {
		return level.intValue() >= Level.WARNING.intValue();
	}

	public void log(Level level, String msg) {
		if (!isLoggable(level))
			return;
		System.out.println(level.getLocalizedName()+": "+msg);
	}

	public void log(Level level, String msg, Throwable t) {
		if (!isLoggable(level))
			return;
		System.out.println(level.getLocalizedName()+": "+msg);
		if (t!=null) {
			StringWriter sWriter = new StringWriter();
			PrintWriter pWriter = new PrintWriter(sWriter);
			t.printStackTrace(pWriter);
			System.out.println(sWriter.toString());
		}
	}

	public void abort(SyncContext ctx, Throwable t) {
		try {
			ctx.getStatementFindDominoDocByUnid().close();
		} catch (SQLException e1) {
			log(Level.SEVERE, "Error closing statement", e1);
		}
		try {
			ctx.getStatementInsertDominoDoc().close();
		} catch (SQLException e1) {
			log(Level.SEVERE, "Error closing statement", e1);
		}
		try {
			ctx.getStatementRemoveDominoDocByUnid().close();
		} catch (SQLException e1) {
			log(Level.SEVERE, "Error closing statement", e1);
		}
		try {
			ctx.getStatementUpdateDominoDoc().close();
		} catch (SQLException e1) {
			log(Level.SEVERE, "Error closing statement", e1);
		}

		try {
			getConnection().rollback();
		} catch (SQLException e) {
			throw new SqlSyncException("Error rolling back current transaction", e);
		}
	}

	/**
	 * Method to add custom JSON data to the sync history row in subclasses. Returns "{}" by default
	 * 
	 * @param ctx sync context
	 * @param selectionFormulaForNextSync formula to be returned by {@link #getLastSyncSelectionFormula()} on the next sync run for incremental sync
	 * @param dbInstanceId db instance id for which to store the <code>startingDateForNextSync</code>
	 * @param startingDateForNextSync date to be returned by {@link #getLastSyncEndDate(String)} on the next sync run for incremental sync
	 * @return JSON string
	 */
	protected String getCustomSyncHistoryJson(SyncContext ctx, String selectionFormulaForNextSync, String dbInstanceId,
			NotesTimeDate startingDateForNextSync) {
		return "{}";
	}

	/**
	 * Returns the max number of batched operations before {@link Statement#executeBatch()}
	 * is called. The default is 1000 entries. Might improve performance for some drivers
	 * that support batching.
	 * 
	 * @return max entries
	 */
	protected int getMaxBatchSize() {
		return 1000;
	}
	
	public void endingSync(SyncContext ctx, String selectionFormulaForNextSync, String dbInstanceId,
			NotesTimeDate startingDateForNextSync) {

		ctx.setEndDateTime(System.currentTimeMillis());

		//write batched deletes
		executeBatchedRemoves(ctx);
		
		//write batched inserts
		executeBatchedInserts(ctx);
		
		//write batched updates
		executeBatchedUpdates(ctx);
		
		//write db replica id and current selection formula for next sync run
		writeDbIdAndSelectionFormula(ctx, selectionFormulaForNextSync, dbInstanceId, startingDateForNextSync);
		
		//create sync history entry for this db intance id
		writeSyncHistoryEntry(ctx, selectionFormulaForNextSync, dbInstanceId,
				startingDateForNextSync);

		try {
			getConnection().commit();
		} catch (SQLException e) {
			throw new SqlSyncException("Error committing current transaction", e);
		}
		
		ctx.dispose();
	}

	/**
	 * Executes batched domino document removal operations
	 * 
	 * @param ctx sync context
	 */
	protected void executeBatchedRemoves(SyncContext ctx) {
		try {
			PreparedStatement removeDocsStmt = ctx.getStatementRemoveDominoDocByUnid();
			removeDocsStmt.executeBatch();
		}
		catch (SQLException e) {
			throw new SqlSyncException("Error deleting documents from database", e);
		}
	}
	
	/**
	 * Executes batched domino document updates operations
	 * 
	 * @param ctx sync context
	 */
	protected void executeBatchedUpdates(SyncContext ctx) {
		try {
			PreparedStatement updateDocsStmt = ctx.getStatementUpdateDominoDoc();
			updateDocsStmt.executeBatch();
		}
		catch (SQLException e) {
			throw new SqlSyncException("Error updating documents into database", e);
		}
	}
	
	/**
	 * Executes batched domino document insertions operations
	 * 
	 * @param ctx sync context
	 */
	protected void executeBatchedInserts(SyncContext ctx) {
		try {
			PreparedStatement insertDocsStmt = ctx.getStatementInsertDominoDoc();
			insertDocsStmt.executeBatch();
		}
		catch (SQLException e) {
			throw new SqlSyncException("Error inserting documents into database", e);
		}
	}
	
	/**
	 * Override this method to store some custom data for the last successful sync run
	 * 
	 * @param ctx sync context
	 * @return custom data or null (default implementation returns null)
	 */
	protected String getCustomDataOfSync(SyncContext ctx) {
		return null;
	}
	
	private void writeDbIdAndSelectionFormula(SyncContext ctx, String selectionFormulaForNextSync, String dbInstanceId,
			NotesTimeDate startingDateForNextSync) {

		//remove old content
		Statement stmt = null;
		try {
			stmt = getConnection().createStatement();
			stmt.executeUpdate(SQL_FLUSH_LASTSYNCDATA);
		} catch (SQLException e) {
			throw new SqlSyncException("Error deleting content of table syncdatainfo", e);
		}
		finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					throw new SqlSyncException("Error closing statement", e);
				}
			}
		}

		String customData = getCustomDataOfSync(ctx);
		if (customData==null)
			customData = "";
		
		try {
			//write new content
			PreparedStatement insertLastSyncDataStmt = createStatementInsertLastSyncDataInfoEntry();
			insertLastSyncDataStmt.setString(1, ctx.getDbId());
			insertLastSyncDataStmt.setString(2, selectionFormulaForNextSync);
			insertLastSyncDataStmt.setString(3, customData);
			insertLastSyncDataStmt.executeUpdate();
		} catch (SQLException e) {
			throw new SqlSyncException("Error writing dbid/selectionformula for next sync", e);
		}

	}
	
	private void writeSyncHistoryEntry(SyncContext ctx, String selectionFormulaForNextSync, String dbInstanceId,
			NotesTimeDate startingDateForNextSync) {
		try {
			PreparedStatement insertHistoryStmt = createStatementInsertHistoryEntry();
			insertHistoryStmt.setString(1, dbInstanceId);

			insertHistoryStmt.setLong(2, ctx.getStartDateTime());
			insertHistoryStmt.setLong(3, ctx.getEndDateTime());

			insertHistoryStmt.setInt(4, ctx.getAdded());
			insertHistoryStmt.setInt(5, ctx.getChanged());
			insertHistoryStmt.setInt(6, ctx.getRemoved());

			int[] startingDateForNextSyncInnards = startingDateForNextSync.getInnards();
			long startingDateForNextSyncMillis = startingDateForNextSync.toDateInMillis();
			
			insertHistoryStmt.setInt(7, startingDateForNextSyncInnards[0]);
			insertHistoryStmt.setInt(8, startingDateForNextSyncInnards[1]);
			insertHistoryStmt.setLong(9, startingDateForNextSyncMillis);

			String customJson = getCustomSyncHistoryJson(ctx, selectionFormulaForNextSync, dbInstanceId, startingDateForNextSync);
			insertHistoryStmt.setString(10, customJson);

			insertHistoryStmt.executeUpdate();
		} catch (SQLException e1) {
			throw new SqlSyncException("Error writing sync history entry", e1);
		}
		
	}

	public void closeConnection() {
		try {
			getConnection().close();
		} catch (SQLException e) {
			throw new SqlSyncException("Could not close the SQL connection for database "+m_jdbcUrl, e);
		}
	}

}
