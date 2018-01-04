package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.structs.NIFFindByKeyContextStruct;
import com.mindoo.domino.jna.internal.structs.NotesBlockIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesBuildVersionStruct;
import com.mindoo.domino.jna.internal.structs.NotesCollectionPositionStruct;
import com.mindoo.domino.jna.internal.structs.NotesDbReplicaInfoStruct;
import com.mindoo.domino.jna.internal.structs.NotesFTIndexStatsStruct;
import com.mindoo.domino.jna.internal.structs.NotesOriginatorIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDatePairStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCompoundStyleStruct;
import com.mindoo.domino.jna.internal.structs.html.HtmlApi_UrlComponentStruct;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Class providing C methods for 64 bit. Should be used internally by
 * the Domino JNA API methods only.
 * 
 * @author Karsten Lehmann
 */
public class NotesNativeAPI64 {
	private static volatile NotesNativeAPI64 m_instanceWithoutCrashLogging;
	private static volatile NotesNativeAPI64 m_instanceWithCrashLogging;

	/**
	 * Gets called from {@link NotesNativeAPI#initialize()}
	 * 
	 * @param instance
	 */
	static void set(NotesNativeAPI64 instance) {
		m_instanceWithoutCrashLogging = instance;
	}
	
	/**
	 * Returns the API instance used to call native Domino C API methods for 64 bit
	 * 
	 * @return API
	 */
	public static NotesNativeAPI64 get() {
		NotesGC.ensureRunningInAutoGC();
		
		if (NotesNativeAPI.m_initError!=null) {
			if (NotesNativeAPI.m_initError instanceof RuntimeException)
				throw (RuntimeException) NotesNativeAPI.m_initError;
			else
				throw new NotesError(0, "Error initializing Domino JNA API", NotesNativeAPI.m_initError);
		}
		
		if (m_instanceWithoutCrashLogging==null)
			throw new NotesError(0, "API not initialized yet. Please call DominoAPI.initialize() first.");

		if (NotesGC.isLogCrashingThreadStacktrace()) {
			if (m_instanceWithCrashLogging==null) {
				m_instanceWithCrashLogging = NotesNativeAPI.wrapWithCrashStackLogging(NotesNativeAPI64.class, m_instanceWithoutCrashLogging);
			}
			return m_instanceWithCrashLogging;
		}
		else {
			return m_instanceWithoutCrashLogging;
		}
	}
	
	public native short NSFSearch(
			long hDB,
			long hFormula,
			Memory viewTitle,
			short SearchFlags,
			short NoteClassMask,
			NotesTimeDateStruct Since,
			NotesCallbacks.b64_NsfSearchProc enumRoutine,
			Pointer EnumRoutineParameter,
			NotesTimeDateStruct retUntil);

	public native short NSFSearchExtended3 (long hDB, 
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
			NotesTimeDateStruct Since, 
			NotesCallbacks.b64_NsfSearchProc EnumRoutine,
			Pointer EnumRoutineParameter, 
			NotesTimeDateStruct retUntil, 
			long namelist);
	
	public native short NSFGetFolderSearchFilter(long hViewDB, long hDataDB, int ViewNoteID, NotesTimeDateStruct Since, int Flags, LongByReference Filter);

	public native Pointer OSLockObject(long handle);
	public native boolean OSUnlockObject(long handle);
	public native short OSMemFree(long handle);
	public native short OSMemGetSize(long handle, IntByReference retSize);
	public native int OSMemoryGetSize(long handle);
	public native void OSMemoryFree(long handle);
	public native short OSMemoryReallocate(long handle, int size);
	public native Pointer OSMemoryLock(long handle);
	public native boolean OSMemoryUnlock(long handle);
	public native short OSMemAlloc(
			short  BlkType,
			int  dwSize,
			LongByReference retHandle);

	public native short NSFItemGetText(
			long  note_handle,
			Memory item_name,
			Memory item_text,
			short text_len);

	public native short ListAllocate(
			short ListEntries,
			short TextSize,
			int fPrefixDataType,
			LongByReference rethList,
			Memory retpList,
			ShortByReference retListSize);
	
	public native short ListAddEntry(
			long hList,
			int fPrefixDataType,
			ShortByReference pListSize,
			short EntryNumber,
			Memory Text,
			short TextSize);
	
	public native short ListGetSize(
			Pointer pList,
			int fPrefixDataType);
	
	public native short ListGetNumEntries(Pointer vList, int noteItem);

	public native short NSFItemInfo(
			long note_handle,
			Memory item_name,
			short  name_len,
			NotesBlockIdStruct retbhItem,
			ShortByReference retDataType,
			NotesBlockIdStruct retbhValue,
			IntByReference retValueLength);

	public native short NSFItemInfoNext(
			long  note_handle,
			NotesBlockIdStruct.ByValue NextItem,
			Memory item_name,
			short  name_len,
			NotesBlockIdStruct retbhItem,
			ShortByReference retDataType,
			NotesBlockIdStruct retbhValue,
			IntByReference retValueLength);

	public native short NSFItemInfoPrev(
			long  note_handle,
			NotesBlockIdStruct.ByValue  CurrItem,
			Memory item_name,
			short  name_len,
			NotesBlockIdStruct item_blockid_ptr,
			ShortByReference value_type_ptr,
			NotesBlockIdStruct value_blockid_ptr,
			IntByReference value_len_ptr);
	
	public native void NSFItemQueryEx(
			long  note_handle,
			NotesBlockIdStruct.ByValue item_bid,
			Memory item_name,
			short  return_buf_len,
			ShortByReference name_len_ptr,
			ShortByReference item_flags_ptr,
			ShortByReference value_datatype_ptr,
			NotesBlockIdStruct value_bid_ptr,
			IntByReference value_len_ptr,
			ByteByReference retSeqByte,
			ByteByReference retDupItemID);
	
	public native short NSFItemGetModifiedTimeByBLOCKID(
			long  hNote,
			NotesBlockIdStruct.ByValue bhItem,
			int  Flags,
			NotesTimeDateStruct retTime);

	public native short NSFItemGetTextListEntries(
			long note_handle,
			Memory item_name);

	public native short NSFItemGetTextListEntry(
			long note_handle,
			Memory item_name,
			short entry_position,
			Memory retEntry_text,
			short  text_len);
	public native short NSFItemGetModifiedTime(
			long hNote,
			Memory ItemName,
			short  ItemNameLength,
			int  Flags,
			NotesTimeDateStruct retTime);
	public native short NSFItemSetTextSummary(
			long  hNote,
			Memory ItemName,
			Memory ItemText,
			short TextLength,
			boolean summary);
	public native boolean NSFItemGetTime(
			long  note_handle,
			Memory td_item_name,
			NotesTimeDateStruct td_item_value);
	public native short NSFItemSetTime(
			long  note_handle,
			Memory td_item_name,
			NotesTimeDateStruct td_item_ptr);
	public native boolean NSFItemGetNumber(
			long  hNote,
			Memory ItemName,
			DoubleByReference retNumber);
	public native int NSFItemGetLong(
			long note_handle,
			Memory number_item_name,
			int number_item_default);
	public native short NSFItemSetNumber(
			long hNote,
			Memory ItemName,
			Memory Number);
	public native short NSFItemConvertToText(
			long note_handle,
			Memory item_name_ptr,
			Memory retText_buf_ptr,
			short  text_buf_len,
			char separator);
	public native short NSFItemConvertValueToText(
			short value_type,
			NotesBlockIdStruct.ByValue value_bid,
			int  value_len,
			Memory text_buf_ptr,
			short  text_buf_len,
			char separator);
	public native short NSFItemDelete(
			long note_handle,
			Memory item_name,
			short name_len);
	public native short NSFItemDeleteByBLOCKID(long note_handle, NotesBlockIdStruct.ByValue item_blockid);
	public native short NSFItemAppend(
			long note_handle,
			short  item_flags,
			Memory item_name,
			short name_len,
			short  item_type,
			Pointer item_value,
			int value_len);
	public native short NSFItemAppendByBLOCKID(
			long note_handle,
			short item_flags,
			Memory item_name,
			short name_len,
			NotesBlockIdStruct.ByValue value_bid,
			int value_len,
			NotesBlockIdStruct item_bid_ptr);
	//valuePtr value without datatype WORD
	public native short NSFItemModifyValue (long hNote, NotesBlockIdStruct.ByValue bhItem, short ItemFlags, short DataType,
			Pointer valuePtr, int valueLength);

	public native void NSFNoteGetInfo(long hNote, short type, Memory retValue);
	public native void NSFNoteSetInfo(long hNote, short type, Pointer value);
	public native short NSFNoteCopy(
			long note_handle_src,
			LongByReference note_handle_dst_ptr);
	public native short NSFNoteUpdateExtended(long hNote, int updateFlags);
	public native short NSFNoteCreate(long db_handle, LongByReference note_handle);
	public native short NSFNoteOpen(long hDB, int noteId, short openFlags, LongByReference rethNote);
	public native short NSFNoteOpenExt(long hDB, int noteId, int flags, LongByReference rethNote);
	public native short NSFNoteOpenByUNID(
			long  hDB,
			NotesUniversalNoteIdStruct pUNID,
			short  flags,
			LongByReference rethNote);
	public native short NSFNoteClose(long hNote);
	public native short NSFNoteVerifySignature(
			long  hNote,
			Memory SignatureItemName,
			NotesTimeDateStruct retWhenSigned,
			Memory retSigner,
			Memory retCertifier);
	public native short NSFNoteContract(long hNote);
	public native short NSFNoteExpand(long hNote);
	public native short NSFNoteSign(long hNote);
	public native short NSFNoteSignExt3(long hNote, 
			long	hKFC,
			Memory SignatureItemName,
			short ItemCount, long hItemIDs, 
			int Flags, int Reserved,
			Pointer pReserved);
	public native short NSFNoteOpenSoftDelete(long hDB, int NoteID, int Reserved, LongByReference rethNote);
	public native short NSFNoteHardDelete(long hDB, int NoteID, int Reserved);
	public native short NSFNoteDeleteExtended(long hDB, int NoteID, int UpdateFlags);
	public native short NSFNoteDetachFile(long note_handle, NotesBlockIdStruct.ByValue item_blockid);
	public native boolean NSFNoteIsSignedOrSealed(long note_handle, ByteByReference signed_flag_ptr, ByteByReference sealed_flag_ptr);
	public native short NSFNoteUnsign(long hNote);
	public native short NSFNoteComputeWithForm(
			long  hNote,
			long  hFormNote,
			int  dwFlags,
			NotesCallbacks.b64_CWFErrorProc ErrorRoutine,
			Pointer CallersContext);
	public native short NSFNoteHasComposite(long hNote);
	public native short NSFNoteHasMIME(long hNote);
	public native short NSFNoteHasMIMEPart(long hNote);
	public native short NSFNoteCipherExtractWithCallback (long hNote, NotesBlockIdStruct.ByValue bhItem,
			int ExtractFlags, int hDecryptionCipher,
			NotesCallbacks.NoteExtractCallback pNoteExtractCallback, Pointer pParam,
			int Reserved, Pointer pReserved);
	public native short NSFNoteCopyAndEncryptExt2(
			long  hSrcNote,
			long hKFC,
			short EncryptFlags,
			LongByReference rethDstNote,
			int Reserved,
			Pointer pReserved);
	public native short NSFNoteCopyAndEncrypt(
			long hSrcNote,
			short EncryptFlags,
			LongByReference rethDstNote);
	public native short NSFNoteCipherDecrypt(
			long  hNote,
			long  hKFC,
			int  DecryptFlags,
			LongByReference rethCipherForAttachments,
			int  Reserved,
			Pointer pReserved);
	public native short NSFNoteAttachFile(
			long note_handle,
			Memory item_name,
			short item_name_length,
			Memory file_name,
			Memory orig_path_name,
			short encoding_type);
	public native short NSFNoteSignHotspots(
			long hNote,
			int dwFlags,
			IntByReference retfSigned);
	
	public native short NSFItemCopy(long note_handle, NotesBlockIdStruct.ByValue item_blockid);
	public native short IDCreateTable (int alignment, LongByReference rethTable);
	public native short IDDestroyTable(long hTable);
	public native short IDInsert (long hTable, int id, IntByReference retfInserted);
	public native short IDDelete (long hTable, int id, IntByReference retfDeleted);
	public native boolean IDScan (long hTable, boolean fFirst, IntByReference retID);
	public native boolean IDScanBack (long hTable, boolean fLast, IntByReference retID);
	public native int IDEntries (long hTable);
	public native boolean IDIsPresent (long hTable, int id);
	public native int IDTableSize (long hTable);
	public native int IDTableSizeP(Pointer pIDTable);
	public native short IDTableCopy (long hTable, LongByReference rethTable);
	public native short IDTableIntersect(long hSrc1Table, long hSrc2Table, LongByReference rethDstTable);
	public native short IDDeleteAll (long hTable);
	public native boolean IDAreTablesEqual	(long hSrc1Table, long hSrc2Table);
	public native short IDDeleteTable  (long hTable, long hIDsToDelete);
	public native short IDInsertTable  (long hTable, long hIDsToAdd);
	public native short IDEnumerate(long hTable, NotesCallbacks.IdEnumerateProc Routine, Pointer Parameter);
	public native short IDInsertRange(long hTable, int IDFrom, int IDTo, boolean AddToEnd);
	public native short IDTableDifferences(long idtable1, long idtable2, LongByReference outputidtableAdds, LongByReference outputidtableDeletes, LongByReference outputidtableSame);
	public native short IDTableReplaceExtended(long idtableSrc, long idtableDest, byte flags);

	public native short NSFDbStampNotesMultiItem(long hDB, long hTable, long hInNote);
	public native short NSFDbOpen(Memory dbName, LongByReference dbHandle);
	public native short NSFDbClose(long dbHandle);
	public native short NSFDbOpenExtended (Memory PathName, short Options, long hNames, NotesTimeDateStruct ModifiedTime, LongByReference rethDB, NotesTimeDateStruct retDataModified, NotesTimeDateStruct retNonDataModified);
	public native short NSFDbGenerateOID(long hDB, NotesOriginatorIdStruct retOID);
	public native int NSFDbGetOpenDatabaseID(long hDBU);
	public native short NSFDbReopen(long hDB, LongByReference rethDB);
	public native short NSFDbLocateByReplicaID(
			long hDB,
			NotesTimeDateStruct ReplicaID,
			Memory retPathName,
			short PathMaxLen);
	public native short NSFDbModifiedTime(
			long hDB,
			NotesTimeDateStruct retDataModified,
			NotesTimeDateStruct retNonDataModified);
	public native short NSFDbIDGet(long hDB, NotesTimeDateStruct retDbID);
	public native short NSFDbReplicaInfoGet(
			long  hDB,
			NotesDbReplicaInfoStruct retReplicationInfo);
	public native short NSFDbReplicaInfoSet(
			long  hDB,
			NotesDbReplicaInfoStruct ReplicationInfo);
	public native short NSFDbGetModifiedNoteTable(long hDB, short NoteClassMask, NotesTimeDateStruct.ByValue Since, NotesTimeDateStruct retUntil, LongByReference rethTable);
	public native short NSFDbGetNotes(
			long hDB,
			int NumNotes,
			Memory NoteID, //NOTEID array
			Memory NoteOpenFlags, // DWORD array
			Memory SinceSeqNum, // DWORD array
			int ControlFlags,
			long hObjectDB,
			Pointer CallbackParam,
			NotesCallbacks.NSFGetNotesCallback  GetNotesCallback,
			NotesCallbacks.b64_NSFNoteOpenCallback  NoteOpenCallback,
			NotesCallbacks.b64_NSFObjectAllocCallback  ObjectAllocCallback,
			NotesCallbacks.b64_NSFObjectWriteCallback  ObjectWriteCallback,
			NotesTimeDateStruct FolderSinceTime,
			NotesCallbacks.NSFFolderAddCallback  FolderAddCallback);
	public native short NSFDbGetMultNoteInfo(
			long  hDb,
			short  Count,
			short  Options,
			long  hInBuf,
			IntByReference retSize,
			LongByReference rethOutBuf);
	public native short NSFDbGetNoteInfoExt(
			long  hDB,
			int  NoteID,
			NotesOriginatorIdStruct retNoteOID,
			NotesTimeDateStruct retModified,
			ShortByReference retNoteClass,
			NotesTimeDateStruct retAddedToFile,
			ShortByReference retResponseCount,
			IntByReference retParentNoteID);
	public native short NSFDbGetMultNoteInfoByUNID(
			long hDB,
			short Count,
			short Options,
			long hInBuf,
			IntByReference retSize,
			LongByReference rethOutBuf);
	public native short NSFDbSign(long hDb, short noteclass);
	public native short NSFDbGetOptionsExt(long hDB, Memory retDbOptions);
	public native short NSFDbSetOptionsExt(long hDB, Memory dbOptions, Memory mask);
	public native void NSFDbAccessGet(long hDB, ShortByReference retAccessLevel, ShortByReference retAccessFlag);
	public native short NSFDbGetBuildVersion(long hDB, ShortByReference retVersion);
	public native short NSFDbGetMajMinVersion(long hDb, NotesBuildVersionStruct retBuildVersion);
	public native short NSFDbReadObject(
			long hDB,
			int ObjectID,
			int Offset,
			int Length,
			LongByReference rethBuffer);
	public native short NSFDbGetObjectSize(
			long hDB,
			int ObjectID,
			short ObjectType,
			IntByReference retSize,
			ShortByReference retClass,
			ShortByReference retPrivileges);
	public native short NSFDbGetSpecialNoteID(
			long hDB,
			short Index,
			IntByReference retNoteID);
	public native short NSFDbClearReplHistory(long hDb, int dwFlags);
	public native void NSFDbPathGet(
			long hDB,
			Memory retCanonicalPathName,
			Memory retExpandedPathName);
	public native short NSFDbIsRemote(long hDb);
	public native short NSFDbSpaceUsage(long dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes);
	public native short NSFDbSpaceUsageScaled (long dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes, IntByReference retGranularity);
	public native short NSFHideDesign(long hdb1, long hdb2, int param3, int param4);
	public native short NSFDbDeleteNotes(long hDB, long hTable, Memory retUNIDArray);
	public native short NSFDbIsLocallyEncrypted(long hDB, IntByReference retVal);

	public native short NSFBuildNamesList(Memory UserName, int dwFlags, LongByReference rethNamesList);

	public native short NIFReadEntries(long hCollection, NotesCollectionPositionStruct IndexPos, short SkipNavigator, int SkipCount, short ReturnNavigator, int ReturnCount, int ReturnMask, LongByReference rethBuffer,
			ShortByReference retBufferLength, IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned, ShortByReference retSignalFlags);
	public native short NIFReadEntriesExt(long hCollection,
			NotesCollectionPositionStruct CollectionPos,
            short SkipNavigator, int SkipCount,
            short ReturnNavigator, int ReturnCount, int ReturnMask,
            NotesTimeDateStruct DiffTime, long DiffIDTable, int ColumnNumber, int Flags,
            LongByReference rethBuffer, ShortByReference retBufferLength,
            IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned,
            ShortByReference retSignalFlags, NotesTimeDateStruct retDiffTime,
            NotesTimeDateStruct retModifiedTime, IntByReference retSequence);
	public native void NIFGetLastModifiedTime(long hCollection, NotesTimeDateStruct retLastModifiedTime);
	public native short NIFFindByKeyExtended2 (long hCollection, Memory keyBuffer,
			int findFlags,
			int returnFlags,
			NotesCollectionPositionStruct retIndexPos,
			IntByReference retNumMatches,
			ShortByReference retSignalFlags,
			LongByReference rethBuffer,
			IntByReference retSequence);
	public native long NIFFindByKeyExtended3 (long hCollection,
			Memory keyBuffer, int findFlags,
			int returnFlags,
			NotesCollectionPositionStruct retIndexPos,
			IntByReference retNumMatches, ShortByReference retSignalFlags,
			LongByReference rethBuffer, IntByReference retSequence,
			NotesCallbacks.NIFFindByKeyProc NIFFindByKeyCallback, NIFFindByKeyContextStruct Ctx);
	public native short NIFFindByKey(long hCollection, Memory keyBuffer, short findFlags, NotesCollectionPositionStruct retIndexPos, IntByReference retNumMatches);
	public native short NIFFindByName(long hCollection, Memory name, short findFlags, NotesCollectionPositionStruct retIndexPos, IntByReference retNumMatches);
	public native short NIFGetCollation(long hCollection, ShortByReference retCollationNum);
	public native short NIFSetCollation(long hCollection, short CollationNum);
	public native short NIFUpdateCollection(long hCollection);
	public native short NIFIsNoteInView(long hCollection, int noteID, IntByReference retIsInView);
	public native boolean NIFIsUpdateInProgress(long hCollection);
	public native short NIFGetIDTableExtended(long hCollection, short navigator, short Flags, long hIDTable);
	public native boolean NIFCollectionUpToDate(long hCollection);
	public native boolean NIFSetCollectionInfo (long hCollection, Pointer SessionID,
            long hUnreadList, long hCollapsedList, long hSelectedList);
    public native short NIFUpdateFilters (long hCollection, short ModifyFlags);
    public native boolean NIFIsTimeVariantView(long hCollection);
	public native short NIFCloseCollection(long hCollection);
	public native short NIFLocateNote (long hCollection, NotesCollectionPositionStruct indexPos, int noteID);
	public native short NIFFindDesignNoteExt(long hFile, Memory name, short noteClass, Memory pszFlagsPattern, IntByReference retNoteID, int Options);
	public native short NIFOpenCollection(long hViewDB, long hDataDB, int ViewNoteID, short OpenFlags, long hUnreadList, LongByReference rethCollection, LongByReference rethViewNote, Memory retViewUNID,
			LongByReference rethCollapsedList, LongByReference rethSelectedList);
	public native short NIFOpenCollectionWithUserNameList (long hViewDB, long hDataDB,
			int ViewNoteID, short OpenFlags,
			long hUnreadList,
			LongByReference rethCollection,
			LongByReference rethViewNote, Memory retViewUNID,
			LongByReference rethCollapsedList,
			LongByReference rethSelectedList,
			long nameList);
	public native short NIFGetCollectionData(
			long hCollection,
			LongByReference rethCollData);

	public native short NSFTransactionBegin(long hDB, int flags);
	public native short NSFTransactionCommit(long hDB, int flags);
	public native short NSFTransactionRollback(long hDB);

	//backup APIs
	public native short NSFDbGetLogInfo(long hDb, int Flags, ShortByReference LOGGED, NotesUniversalNoteIdStruct LogID,
			NotesUniversalNoteIdStruct DbIID, IntByReference LogExtent);
	public native short NSFBackupStart(long hDB, int Flags, IntByReference BackupContext, IntByReference FileSizeLow,
			IntByReference FileSizeHigh);
	public native short NSFBackupStop(long hDB, int BackupContext);
	public native short NSFBackupEnd(long hDB, int BackupContext, int Options);
	public native short NSFBackupGetChangeInfoSize( long hDB, int hBackupContext, int Flags, IntByReference InfoSizeLow,
			IntByReference InfoSizeHigh);
	public native short NSFBackupStartApplyChangeInfo(IntByReference ApplyInfoContext, Memory CopyFilePath, int Flags,
			int InfoSizeLow, int InfoSizeHigh);
	public native short NSFBackupGetNextChangeInfo(long hDB, int hBackupContext, int Flags, Memory Buffer, int BufferSize,
			IntByReference FilledSize);
	public native short NSFBackupApplyNextChangeInfo(long ApplyInfoContext, int Flags, Memory Buffer, int BufferSize);
	public native short NSFBackupEndApplyChangeInfo(int ApplyInfoContext, int Flags);

	public native short AgentDelete (long hAgent); /* delete agent */
	public native boolean IsRunAsWebUser(long hAgent);
	public native short AgentOpen (long hDB, int AgentNoteID, LongByReference rethAgent);
	public native void AgentClose (long hAgent);
	public native short AgentCreateRunContext (long hAgent,
			 Pointer pReserved,
			 int dwFlags,
			 LongByReference rethContext);
	public native short AgentCreateRunContextExt (long hAgent, Pointer pReserved, long pOldContext, int dwFlags, LongByReference rethContext);
	public native short AgentSetDocumentContext(long hAgentCtx, long hNote);
	public native short AgentSetTimeExecutionLimit(long hAgentCtx, int timeLimit);
	public native boolean AgentIsEnabled(long hAgent);
	public native void SetParamNoteID(long hAgentCtx, int noteId);
	public native short AgentSetUserName(long hAgentCtx, long hNameList);
	public native short AgentRedirectStdout(long hAgentCtx, short redirType);
	public native void AgentQueryStdoutBuffer(long hAgentCtx, LongByReference retHdl, IntByReference retSize);
	public native void AgentDestroyRunContext (long hAgentCtx);
	public native short AgentRun (long hAgent,
			long hAgentCtx,
		    int hSelection,
			int dwFlags);
	public native short AgentSetHttpStatusCode(long hAgentCtx, int httpStatus);

	public native short FTIndex(long hDB, short options, Memory stopFile, NotesFTIndexStatsStruct retStats);
	public native short FTDeleteIndex(long hDB);
	public native short FTGetLastIndexTime(long hDB, NotesTimeDateStruct retTime);
	public native short FTOpenSearch(LongByReference rethSearch);
	public native short FTCloseSearch(long hSearch);
	public native short FTSearch(
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

	public native short NSFFormulaCompile(
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
	public native short NSFFormulaDecompile(
			Pointer pFormulaBuffer,
			boolean fSelectionFormula,
			LongByReference rethFormulaText,
			ShortByReference retFormulaTextLength);
	public native short NSFComputeStart(
			short Flags,
			Pointer lpCompiledFormula,
			LongByReference rethCompute);
	public native short NSFComputeStop(long hCompute);
	public native short NSFComputeEvaluate(
			long  hCompute,
			long hNote,
			LongByReference rethResult,
			ShortByReference retResultLength,
			IntByReference retNoteMatchesFormula,
			IntByReference retNoteShouldBeDeleted,
			IntByReference retNoteModified);

	public native short CESCreateCTXFromNote(int hNote, LongByReference rethCESCTX);
	public native short CESGetNoSigCTX(LongByReference rethCESCTX);
	public native short CESFreeCTX(long hCESCTX);
	public native short ECLUserTrustSigner ( long hCESCtx, 
			short ECLType,
			short bSessionOnly,
			short wCapabilities,
			short wCapabilities2,
			ShortByReference retwCurrentCapabilities,
			ShortByReference retwCurrentCapabilities2);

	public native short NSFFolderGetIDTable(
			long  hViewDB,
			long hDataDB,
			int  viewNoteID,
			int  flags,
			LongByReference hTable);
	public native short FolderDocAdd(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  hTable,
			long  dwFlags);
	public native short FolderDocCount(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  dwFlags,
			LongByReference pdwNumDocs);
	public native short FolderDocRemove(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  hTable,
			long  dwFlags);
	public native short FolderDocRemoveAll(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  dwFlags);
	public native short FolderMove(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  hParentDB,
			int  ParentNoteID,
			long  dwFlags);
	public native short FolderRename(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			Memory pszName,
			short  wNameLen,
			long  dwFlags);

	public native short NSFProfileOpen(
			long hDB,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength,
			short CopyProfile,
			LongByReference rethProfileNote);
	public native short NSFProfileUpdate(
			long hProfile,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength);
	public native short NSFProfileSetField(
			long hDB,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength,
			Memory FieldName,
			short FieldNameLength,
			short Datatype,
			Pointer Value,
			int ValueLength);

	public native short SECKFMOpen(LongByReference phKFC, Memory pIDFileName, Memory pPassword,
			int Flags, int Reserved, Pointer pReserved);

	public native short SECKFMClose(LongByReference phKFC, int Flags, int Reserved, Pointer pReserved);
	public native short SECidfGet(Memory pUserName, Memory pPassword, Memory pPutIDFileHere,
			LongByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved);
	public native short SECidfPut(Memory pUserName, Memory pPassword, Memory pIDFilePath,
			LongByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved);
	public native short SECidfSync( Memory pUserName, Memory pPassword, Memory pIDFilePath,
			LongByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved, IntByReference retdwFlags);
	public native short SECTokenGenerate(
			Memory ServerName,
			Memory OrgName,
			Memory ConfigName,
			Memory UserName,
			NotesTimeDateStruct Creation,
			NotesTimeDateStruct Expiration,
			LongByReference retmhToken,
			int dwReserved,
			Pointer vpReserved);
	public native void SECTokenFree(LongByReference mhToken);

	public native short SchFreeTimeSearch(
			NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate,
			short fFindFirstFit,
			int dwReserved,
			NotesTimeDatePairStruct pInterval,
			short Duration,
			Pointer pNames,
			LongByReference rethRange);

	public native short SchRetrieve(
			NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate,
			int dwOptions,
			NotesTimeDatePairStruct pInterval,
			Pointer pNames,
			LongByReference rethCntnr,
			Pointer mustBeNull1,
			Pointer mustBeNull2,
			Pointer mustBeNull3);

	public native void SchContainer_Free(long hCntnr);
	public native short SchContainer_GetFirstSchedule(
			long hCntnr,
			IntByReference rethObj,
			Memory retpSchedule);
	public native short Schedule_Free(long hCntnr, int hSched);
	public native short SchContainer_GetNextSchedule(
			long hCntnr,
			int hCurSchedule,
			IntByReference rethNextSchedule,
			Memory retpNextSchedule);
	public native short Schedule_ExtractFreeTimeRange(
			long hCntnr,
			int hSchedObj,
			NotesUniversalNoteIdStruct punidIgnore,
			short fFindFirstFit,
			short wDuration,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethRange);
	public native short Schedule_ExtractBusyTimeRange(
			long hCntnr,
			int hSchedObj,
			NotesUniversalNoteIdStruct punidIgnore,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethRange,
			IntByReference rethMoreCtx);
	public native short Schedule_ExtractMoreBusyTimeRange(
			long hCntnr,
			int hMoreCtx,
			NotesUniversalNoteIdStruct punidIgnore,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethRange,
			IntByReference rethMore);
	public native short Schedule_ExtractSchedList(
			long hCntnr,
			int hSchedObj,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethSchedList,
			IntByReference rethMore);
	public native short Schedule_ExtractMoreSchedList(
			long hCntnr,
			int hMoreCtx,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethSchedList,
			IntByReference rethMore);
	public native short Schedule_Access(
			long hCntnr,
			int hSched,
			PointerByReference pretSched);

	public native short NSGetServerClusterMates(
			Memory pServerName,
			int dwFlags,
			LongByReference phList);
	public native short NSPingServer(
			Memory pServerName,
			IntByReference pdwIndex,
			LongByReference phList);

	public native short HTMLCreateConverter(LongByReference phHTML);
	public native short HTMLDestroyConverter(long hHTML);
	public native short HTMLSetHTMLOptions(long hHTML, StringArray optionList);
	public native short HTMLConvertItem(
			long hHTML,
			long hDB,
			long hNote,
			Memory pszItemName);
	public native short HTMLConvertNote(
			long hHTML,
			long hDB,
			long hNote,
			int NumArgs,
			HtmlApi_UrlComponentStruct pArgs);
	public native short HTMLGetProperty(
			long hHTML,
			long PropertyType,
			Pointer pProperty);
	public native short HTMLSetProperty(
			int hHTML,
			long PropertyType,
			Memory pProperty);
	public native short HTMLGetText(
			long hHTML,
			int startingOffset,
			IntByReference pTextLength,
			Memory pText);
	public native short HTMLGetReference(
			long hHTML,
			int Index,
			LongByReference phRef);
	public native short HTMLLockAndFixupReference(
			long hRef,
			Memory ppRef);
	public native short HTMLConvertElement(
			long hHTML,
			long hDB,
			long hNote,
			Memory pszItemName,
			int ItemIndex,
			int Offset);

	public native short CompoundTextAddCDRecords(
			long hCompound,
			Pointer pvRecord,
			int dwRecordLength);

	public native short CompoundTextAddDocLink(
			long hCompound,
			NotesTimeDateStruct.ByValue DBReplicaID,
			NotesUniversalNoteIdStruct.ByValue ViewUNID,
			NotesUniversalNoteIdStruct.ByValue NoteUNID,
			Memory pszComment,
			int dwFlags);
	public native short CompoundTextAddParagraphExt(
			long hCompound,
			int dwStyleID,
			int FontID,
			Memory pchText,
			int dwTextLen,
			Pointer pInfo);
	public native short CompoundTextAddRenderedNote(
			long hCompound,
			long hNote,
			long hFormNote,
			int dwFlags);
	public native short CompoundTextAddTextExt(
			long hCompound,
			int dwStyleID,
			int FontID,
			Memory pchText,
			int dwTextLen,
			Memory pszLineDelim,
			int dwFlags,
			Pointer pInfo);
	public native short CompoundTextAssimilateFile(
			long hCompound,
			Memory pszFileName,
			int dwFlags);
	public native short CompoundTextAssimilateItem(
			long hCompound,
			long hNote,
			Memory pszItemName,
			int dwFlags);
	public native short CompoundTextAssimilateBuffer(long hBuffer, int bufferLength, int flags);
	public native short CompoundTextClose(
			long hCompound,
			LongByReference phReturnBuffer,
			IntByReference pdwReturnBufferSize,
			Memory pchReturnFile,
			short wReturnFileNameSize);
	public native short CompoundTextCreate(
			long hNote,
			Memory pszItemName,
			LongByReference phCompound);
	public native short CompoundTextDefineStyle(
			long hCompound,
			Memory pszStyleName,
			NotesCompoundStyleStruct pDefinition,
			IntByReference pdwStyleID);
	public native void CompoundTextDiscard(
			long hCompound);

}
