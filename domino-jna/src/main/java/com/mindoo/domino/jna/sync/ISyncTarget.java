package com.mindoo.domino.jna.sync;

import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;

import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder.ItemTableData;

/**
 * Interface for a sync target that receives source database changes incrementally.
 * 
 * @author Karsten Lehmann
 */
public interface ISyncTarget {

	/**
	 * Return the replica id of the last sync run. Used to compare if it is safe
	 * to do any incremental / full sync or if the target needs to be emptied first
	 * by calling {@link #clear(Object)}.
	 * 
	 * @return replica id or null if target is still empty
	 */
	public String getLastSyncDbReplicaId();

	/**
	 * Return the selection formula of the last sync run. Used to detect that
	 * notes might need to be removed from the target if the formula has changed
	 * so that the target represents the current formula selection in the source db
	 * 
	 * @return formula or null if target is still empty
	 */
	public String getLastSyncSelectionFormula();
	
	/**
	 * Return the end date of the last sync process, which is the 
	 * start date for the next sync run, for the specified db instance id.
	 * The instance id is used to support synchronization with multiple DB replicas and
	 * have different starting points for each db instance for incremental replication.
	 * 
	 * @param dbInstanceId db instance id, computed from db server name and NSF file creation date
	 * @return date or null if first time sync with this database instance; returning null here triggers a complete content comparison based on the originator ids of source and target
	 */
	public NotesTimeDate getLastSyncEndDate(String dbInstanceId);
	
	/**
	 * The method is called when the sync process starts. It can return an
	 * optional Object that will be passed to all the methods to be called
	 * during the sync process.<br>
	 * If the underlying data store uses transactions, this is the place
	 * to start a new transaction. The sync process will either call
	 * {@link #endingSync(Object, String, String, NotesTimeDate)} or
	 * {@link #abort(Object, Throwable)}
	 * at the end of the sync process, where we can either commit
	 * or abort the transaction.
	 * 
	 * @param dbReplicaId replica id of source database, to be returned on the {@link #getLastSyncDbReplicaId()} call of the next sync run
	 * @return sync context object or null
	 */
	public Object startingSync(String dbReplicaId);
	
	/**
	 * Method to quickly wipe the whole target datastore, used in case of changing source
	 * db replica ids between sync runs.
	 * 
	 * @param ctx sync context
	 */
	public void clear(Object ctx);
	
	/**
	 * This expensive method to scan the existing target data is only used in case
	 * of a first time sync with a db instance, which means that {@link #getLastSyncEndDate(String)} returned null
	 * for the db instanceid of the current sync run.
	 * 
	 * @return list of originator ids in target (containing UNID / sequence no / sequence time of synced data) so that we can compare what is missing or outdated in the target
	 */
	public List<NotesOriginatorIdData> scanTargetData();
	
	/**
	 * Return here whether we should read just the summary buffer data for notes matching
	 * the selection formula or the whole note need to be read. Depending the returned
	 * value, one of the parameters in
	 * {@link ISyncTarget#noteChangedMatchingFormula(Object, NotesOriginatorIdData, ItemTableData, NotesNote)}
	 * (summary buffer / note) will have a value and the other will be null.
	 * 
	 * @return data to read
	 */
	public EnumSet<DataToRead> getWhichDataToRead();
	
	public enum DataToRead {
		/** read just the summary buffer returned my the NSFSearch call, which is faster than opening the note */
		SummaryBuffer,
		/** the note needs to be opened with all items, because non-summary items need to be read */
		NoteWithAllItems,
		/** the note should only be opened with summary items (faster) */
		NoteWithSummaryItems
	}

	public enum TargetResult {Added, Removed, Updated, None}
	
	/**
	 * The method is called for every note that has changed since the last sync end date and
	 * that currently matches the selection formula.
	 * 
	 * @param ctx sync context
	 * @param oid originator id containing the UNID, sequence number and sequence date ("modified initially") of the note
	 * @param summaryBufferData summary buffer if {@link #getWhichDataToRead()} returned {@link DataToRead#SummaryBuffer}, null otherwse
	 * @param note note if {@link #getWhichDataToRead()} returned {@link DataToRead#NoteWithAllItems} or {@link DataToRead#NoteWithSummaryItems}, null otherwise
	 * @return flag whether the note got added, removed or updated in the target, used for statistics
	 */
	public TargetResult noteChangedMatchingFormula(Object ctx, NotesOriginatorIdData oid, ItemTableData summaryBufferData, NotesNote note);

	/**
	 * The method is called for every note that changed since the last sync end date and
	 * that currently does not match the selection formula. Add code here to remove the note's data
	 * from the sync target if it existed there before (or an older version with the same UNID, but lower
	 * sequence number).
	 * 
	 * @param ctx sync context
	 * @param oid originator id containing the UNID, sequence number and sequence date ("modified initially") of the note
	 * @return flag whether the note got added, removed or updated in the target, used for statistics
	 */
	public TargetResult noteChangedNotMatchingFormula(Object ctx, NotesOriginatorIdData oid);

	/**
	 * The method is called for every note that got deleted since the last sync end date. Add code here
	 * to remove the note's data
	 * from the sync target if it existed there before (or an older version with the same UNID, but lower
	 * sequence number).
	 * 
	 * @param ctx sync context
	 * @param oid originator id containing the UNID, sequence number and sequence date ("modified initially") of the note
	 * @return flag whether the note got added, removed or updated in the target, used for statistics
	 */
	public TargetResult noteDeleted(Object ctx, NotesOriginatorIdData oid);

	/**
	 * Method is called during the log process in case expensive log messages are
	 * about to be produced
	 * 
	 * @param level log level
	 * @return true if loggable
	 */
	public boolean isLoggable(Level level);
	
	/**
	 * Method is called with log messages about the sync process
	 * 
	 * @param level log level
	 * @param msg log message
	 */
	public void log(Level level, String msg);
	
	/**
	 * Method is called with log messages about the sync process
	 * 
	 * @param level log level
	 * @param msg log message
	 * @param t exception
	 */
	public void log(Level level, String msg, Throwable t);
	
	/**
	 * The method is called when something went wrong in the sync process.
	 * 
	 * @param ctx sync context
	 * @param t optional exception to be logged which occurred during sync or null
	 */
	public void abort(Object ctx, Throwable t);

	/**
	 * Method is called when the whole sync process is done to commit pending writes.
	 * 
	 * @param ctx sync context
	 * @param selectionFormulaForNextSync formula to be returned by {@link #getLastSyncSelectionFormula()} on the next sync run for incremental sync
	 * @param dbInstanceId db instance id for which to store the <code>startingDateForNextSync</code>
	 * @param startingDateForNextSync date to be returned by {@link #getLastSyncEndDate(String)} on the next sync run for incremental sync
	 */
	public void endingSync(Object ctx, String selectionFormulaForNextSync, String dbInstanceId, NotesTimeDate startingDateForNextSync);
}
