package com.mindoo.domino.jna.sync;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesOriginatorId;
import com.mindoo.domino.jna.NotesSearch;
import com.mindoo.domino.jna.NotesSearch.SearchCallback;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.OpenNote;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder.ItemTableData;
import com.mindoo.domino.jna.sync.ISyncTarget.DataToRead;
import com.mindoo.domino.jna.sync.ISyncTarget.TargetResult;
import com.mindoo.domino.jna.utils.NotesNamingUtils;

/**
 * Generic data sync utility that incrementally synchronizes Domino data with external
 * data stores, e.g. to build a custom view indexer or migrate data.<br>
 * <br>
 * The algorithm supports synchonizing mulitple db replicas with the same target and
 * uses a separate sync starting point for each source database.<br>
 * <br>
 * We also handle changing the selection formula. In that case, we do a fast comparison
 * which data needs to be purged from the target and which data needs to be
 * transferred from source to target.<br>
 * <br>
 * In case of a source db replica id change, we clear the target data and restart
 * the whole sync process from the beginning.
 * 
 * @author Karsten Lehmann
 */
public class SyncUtil {

	/**
	 * Synchronizes a subset of a Domino database with a {@link ISyncTarget}.
	 * 
	 * @param dbSource source database
	 * @param selectionFormula selection formula for content
	 * @param target sync target
	 * @return result statistics
	 */
	public static SyncResult sync(final NotesDatabase dbSource, String selectionFormula, final ISyncTarget target) {
		long t0=System.currentTimeMillis();
		
		String dbReplicaId = dbSource.getReplicaID();
		String lastDbReplicaId = target.getLastSyncDbReplicaId();
		//check if replica has changed; in this case, all existing target data needs to be removed
		boolean isWipeReqired = lastDbReplicaId!=null && !dbReplicaId.equals(lastDbReplicaId);
		
		String lastSelectionFormula = target.getLastSyncSelectionFormula();
		//check if selection formula has changed; in that case we might need to remove data from the target
		//that no longer matches the formula
		boolean selectionFormulaHasChanged = lastSelectionFormula!=null && !selectionFormula.equals(lastSelectionFormula);
		
		String dbServerAbbr = NotesNamingUtils.toAbbreviatedName(dbSource.getServer());
		if (dbServerAbbr==null)
			dbServerAbbr="";
		String dbFilePath = dbSource.getRelativeFilePath();
		String dbInstanceId = dbServerAbbr + "_" + dbFilePath + "_" + dbSource.getCreated().toDateInMillis();

		NotesTimeDate lastSyncEndDate = target.getLastSyncEndDate(dbInstanceId);
		final Object ctx = target.startingSync(dbReplicaId);
		
		NotesIDTable searchFilter = null;
		
		try {
			if (isWipeReqired) {
				//db replica id has changed, tell the target to clear its content and any stored last sync dates
				target.clear(ctx);
				lastSyncEndDate = null;
			}
			
			NotesTimeDate sinceDateForSearch;
			
			NotesTimeDate startDateForNextSync = null;
			
			Map<String,NotesOriginatorIdData> purgeInTarget = new HashMap<String,NotesOriginatorIdData>();

			boolean skipSearchAndCopy = false;
			
			if (selectionFormulaHasChanged || lastSyncEndDate==null) {
				sinceDateForSearch = null;
				
				//no last sync date, so we need to do a one-time comparison of source and target content
				List<NotesOriginatorIdData> targetOIDs = target.scanTargetData();
				if (!targetOIDs.isEmpty()) {
					Map<String,NotesOriginatorIdData> targetOIDsByUNID = new HashMap<String,NotesOriginatorIdData>();
					for (NotesOriginatorIdData currOID : targetOIDs) {
						targetOIDsByUNID.put(currOID.getUNID(), currOID);
					}
					
					final Map<String,NotesOriginatorIdData> sourceOIDsByUNID = new HashMap<String,NotesOriginatorIdData>();
					
					NotesTimeDate sourceOIDSearchEndDate = NotesSearch.search(dbSource, null, selectionFormula, "-", EnumSet.of(Search.SESSION_USERNAME),
							EnumSet.of(NoteClass.DOCUMENT), null, new SearchCallback() {

								@Override
								public Action noteFound(NotesDatabase parentDb, int noteId, NotesOriginatorId oid,
										EnumSet<NoteClass> noteClass, EnumSet<NoteFlags> flags, NotesTimeDate dbCreated,
										NotesTimeDate noteModified, ItemTableData summaryBufferData) {
									
									NotesOriginatorIdData oidData = new NotesOriginatorIdData(oid);
									
									sourceOIDsByUNID.put(oidData.getUNID(), oidData);
									return Action.Continue;
								}
					});
					
					//find out which data we need to transfer
					Map<String, NotesOriginatorIdData> missingUNIDsInTarget = new HashMap<String, NotesOriginatorIdData>();
					Map<String, NotesOriginatorIdData> additionalUNIDsInTarget = new HashMap<String, NotesOriginatorIdData>();
					Map<String, NotesOriginatorIdData> equalInSourceAndTarget = new HashMap<String, NotesOriginatorIdData>();
					
					Map<String, NotesOriginatorIdData[]> outdatedUNIDsInTarget = new HashMap<String, NotesOriginatorIdData[]>();
					Map<String, NotesOriginatorIdData[]> newerUNIDsInTarget = new HashMap<String, NotesOriginatorIdData[]>();
					Map<String, NotesOriginatorIdData[]> conflicts = new HashMap<String, NotesOriginatorIdData[]>();
					
					for (Entry<String,NotesOriginatorIdData> currEntry : sourceOIDsByUNID.entrySet()) {
						String currUNID = currEntry.getKey();
						NotesOriginatorIdData currSourceOID = currEntry.getValue();
						
						NotesOriginatorIdData matchingOIDInTarget = targetOIDsByUNID.get(currUNID);
						if (matchingOIDInTarget==null) {
							missingUNIDsInTarget.put(currUNID, currSourceOID);
						}
						else if (currSourceOID.getSequence()==matchingOIDInTarget.getSequence()) {
							//se	quence time is expected to be the same, otherwise we have a conflict
							if (currSourceOID.getSequenceTime().equals(matchingOIDInTarget.getSequenceTime())) {
								equalInSourceAndTarget.put(currUNID, currSourceOID);
							}
							else {
								conflicts.put(currUNID, new NotesOriginatorIdData[] {currSourceOID, matchingOIDInTarget});
							}
						}
						else if (currSourceOID.getSequence()<matchingOIDInTarget.getSequence()) {
							//target contains newer content that came from another replica
							newerUNIDsInTarget.put(currUNID, new NotesOriginatorIdData[] {currSourceOID, matchingOIDInTarget});
						}
						else if (currSourceOID.getSequence()<matchingOIDInTarget.getSequence()) {
							//target contains oldeer content
							outdatedUNIDsInTarget.put(currUNID, new NotesOriginatorIdData[] {currSourceOID, matchingOIDInTarget});
						}
					}

					for (Entry<String,NotesOriginatorIdData> currEntry : targetOIDsByUNID.entrySet()) {
						String currUNID = currEntry.getKey();
						NotesOriginatorIdData currTargetOID = currEntry.getValue();
						
						NotesOriginatorIdData matchingOIDInSource = sourceOIDsByUNID.get(currUNID);
						if (matchingOIDInSource==null) {
							if (selectionFormulaHasChanged) {
								//we purge this entry because it no longer matches the changed selection formula
								purgeInTarget.put(currUNID, currTargetOID);
							}
							else {
								//looks like another NSF replica has more data matching
								//the selection formula then the current one has
								additionalUNIDsInTarget.put(currUNID, currTargetOID);
							}
						}
					}
					
					Set<String> unidsToTransfer = new HashSet<String>();
					unidsToTransfer.addAll(missingUNIDsInTarget.keySet());
					unidsToTransfer.addAll(outdatedUNIDsInTarget.keySet());
					
					//for conflicts, let the newer win for now; happens if the sync with two
					//NSFs that have unresolved conflicts for documents
					for (Entry<String,NotesOriginatorIdData[]> currEntry : conflicts.entrySet()) {
						String currUNID = currEntry.getKey();
						NotesOriginatorIdData[] sourceTargetOIDs = currEntry.getValue();
						if (sourceTargetOIDs[0].getSequenceTime().isAfter(sourceTargetOIDs[1].getSequenceTime())) {
							unidsToTransfer.add(currUNID);
						}
					}
					
					if (unidsToTransfer.isEmpty() && purgeInTarget.isEmpty()) {
						//nothing to do
						target.endingSync(ctx, selectionFormula, dbInstanceId, sourceOIDSearchEndDate);
						long t1=System.currentTimeMillis();
						return new SyncResult((int) (t1-t0), isWipeReqired, selectionFormulaHasChanged,
								null, sourceOIDSearchEndDate,
								0, 0, 0,
								sourceOIDsByUNID.size(), 0, 0);
					}
					
					if (!unidsToTransfer.isEmpty()) {
						//use a fast bulk conversion method to convert the UNIDs to note ids, because
						//we need a NotesIDTable later to restrict our search+copy operation
						//(filter parameter of NSFSearchExtended3)
						
						Map<String,Integer> retNoteIdsByUnid = new HashMap<String,Integer>();
						//retNoteUnidsNotFound -> documents might have gotten deleted since our search,
						//which is not important because our next incremental search will detect this
						Set<String> retNoteUnidsNotFound = new HashSet<String>();

						//since the C API mostly deals with note ids, we need to convert our unids
						String[] unidsArr = unidsToTransfer.toArray(new String[unidsToTransfer.size()]);
						
						dbSource.toNoteIds(unidsArr, retNoteIdsByUnid, retNoteUnidsNotFound);

						//limit our next copy process to these UNIDs / note ids, speeding up the search
						searchFilter = new NotesIDTable(retNoteIdsByUnid.values());
					}
					else {
						skipSearchAndCopy = true;
					}
				}
				else {
					//all ok, target is empty
				}
			}
			else {
				//do an incremental sync
				sinceDateForSearch = lastSyncEndDate;
			}
			
			EnumSet<Search> searchFlags = EnumSet.of(Search.SESSION_USERNAME);
			if (sinceDateForSearch!=null) {
				//incremental sync, we need non-matching and deleted notes as well
				searchFlags.add(Search.ALL_VERSIONS);
				searchFlags.add(Search.NOTIFYDELETIONS);
			}
			
			final EnumSet<DataToRead> dataToRead = target.getWhichDataToRead();
			if (dataToRead.contains(DataToRead.SummaryBufferAllItems) || dataToRead.contains(DataToRead.SummaryBufferSelectedItems)) {
				searchFlags.add(Search.SUMMARY);
			}
			if (dataToRead.contains(DataToRead.SummaryBufferSelectedItems)) {
				searchFlags.add(Search.NOITEMNAMES);
			}
			searchFlags.add(Search.NONREPLICATABLE);
			
			Map<String,String> additionalComputedSummaryBufferEntries = target.getSummaryBufferItemsAndFormulas();
			final LinkedHashMap<String,String> additionalComputedSummaryBufferEntriesSorted = additionalComputedSummaryBufferEntries==null ? null : new LinkedHashMap<String, String>(additionalComputedSummaryBufferEntries);
			
			final int[] addedToTarget = new int[1];
			final int[] updatedInTarget = new int[1];
			final int[] removedFromTarget = new int[1];
			
			final int[] notesMatchingFormula = new int[1];
			final int[] notesNotMatchingFormula = new int[1];
			final int[] notesDeleted = new int[1];
			
			if (!purgeInTarget.isEmpty()) {
				//purge entries from target, when they no longer match the changed formula
				for (Entry<String,NotesOriginatorIdData> currEntry : purgeInTarget.entrySet()) {
					TargetResult tResult = target.noteChangedNotMatchingFormula(ctx, currEntry.getValue());
					if (tResult==TargetResult.Added)
						addedToTarget[0]++;
					else if (tResult==TargetResult.Removed)
						removedFromTarget[0]++;
					else if (tResult==TargetResult.Updated)
						updatedInTarget[0]++;
				}
			}
			
			//the actual lookup and copy operation
			if (!skipSearchAndCopy && (searchFilter==null || !searchFilter.isEmpty())) {
				NotesTimeDate copyOpEndDate = NotesSearch.search(dbSource, searchFilter, selectionFormula, additionalComputedSummaryBufferEntriesSorted, "-", searchFlags, EnumSet.of(NoteClass.DOCUMENT),
						sinceDateForSearch, new SearchCallback() {

					@Override
					public Action noteFound(NotesDatabase parentDb, int noteId, NotesOriginatorId oid,
							EnumSet<NoteClass> noteClass, EnumSet<NoteFlags> flags, NotesTimeDate dbCreated,
							NotesTimeDate noteModified, ItemTableData summaryBufferData) {

						NotesOriginatorIdData oidData = new NotesOriginatorIdData(oid);
						
						NotesNote note = null;
						if (dataToRead.contains(DataToRead.NoteWithAllItems)) {
							try {
								note = dbSource.openNoteById(noteId);
							}
							catch (Exception e) {
								target.log(Level.WARNING, "Error loading document with note id "+noteId+" and UNID "+oid.getUNIDAsString()+". Seems to have been deleted in the meantime and gets ignored.", e);
								return Action.Continue;
							}
						}
						else if (dataToRead.contains(DataToRead.NoteWithSummaryItems)) {
							try {
								note = dbSource.openNoteById(noteId, EnumSet.of(OpenNote.SUMMARY));
							}
							catch (Exception e) {
								target.log(Level.WARNING, "Error loading document with note id "+noteId+" and UNID "+oid.getUNIDAsString()+". Seems to have been deleted in the meantime and gets ignored.", e);
								return Action.Continue;
							}
						}
						
						TargetResult tResult = target.noteChangedMatchingFormula(ctx, oidData, summaryBufferData, note);
						if (tResult==TargetResult.Added)
							addedToTarget[0]++;
						else if (tResult==TargetResult.Removed)
							removedFromTarget[0]++;
						else if (tResult==TargetResult.Updated)
							updatedInTarget[0]++;
						
						notesMatchingFormula[0]++;
						
						if (note!=null) {
							note.recycle();
						}

						return Action.Continue;
					}

					@Override
					public Action noteFoundNotMatchingFormula(NotesDatabase parentDb, int noteId,
							NotesOriginatorId oid, EnumSet<NoteClass> noteClass, EnumSet<NoteFlags> flags,
							NotesTimeDate dbCreated, NotesTimeDate noteModified, ItemTableData summaryBufferData) {
						
						NotesOriginatorIdData oidData = new NotesOriginatorIdData(oid);
						
						TargetResult tResult = target.noteChangedNotMatchingFormula(ctx, oidData);
						if (tResult==TargetResult.Added)
							addedToTarget[0]++;
						else if (tResult==TargetResult.Removed)
							removedFromTarget[0]++;
						else if (tResult==TargetResult.Updated)
							updatedInTarget[0]++;
						
						notesNotMatchingFormula[0]++;
						return Action.Continue;
					}

					@Override
					public Action deletionStubFound(NotesDatabase parentDb, int noteId, NotesOriginatorId oid,
							EnumSet<NoteClass> noteClass, EnumSet<NoteFlags> flags, NotesTimeDate dbCreated,
							NotesTimeDate noteModified) {
						
						NotesOriginatorIdData oidData = new NotesOriginatorIdData(oid);
						
						TargetResult tResult = target.noteDeleted(ctx, oidData);
						if (tResult==TargetResult.Added)
							addedToTarget[0]++;
						else if (tResult==TargetResult.Removed)
							removedFromTarget[0]++;
						else if (tResult==TargetResult.Updated)
							updatedInTarget[0]++;
						
						notesDeleted[0]++;
						return Action.Continue;
					}
				});
				if (startDateForNextSync==null) {
					startDateForNextSync = copyOpEndDate;
				}
			}
			
			target.endingSync(ctx, selectionFormula, dbInstanceId, startDateForNextSync);
			
			long t1=System.currentTimeMillis();
			return new SyncResult((int) (t1-t0), isWipeReqired, selectionFormulaHasChanged, sinceDateForSearch, startDateForNextSync,
					addedToTarget[0], updatedInTarget[0], removedFromTarget[0],
					notesMatchingFormula[0], notesNotMatchingFormula[0], notesDeleted[0]);
		}
		catch (Throwable t) {
			target.log(Level.SEVERE, "Exception occurred during sync operation", t);
			target.abort(ctx, t);
			throw new NotesError(0, "Exception occurred during sync operation", t);
		}
		finally {
			if (searchFilter!=null) {
				searchFilter.recycle();
			}
		}
	}
	
}
