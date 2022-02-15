package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.internal.NotesCallbacks.ABORTCHECKPROC;
import com.mindoo.domino.jna.internal.NotesCallbacks.OSSIGMSGPROC;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.HANDLE;
import com.mindoo.domino.jna.internal.structs.KFM_PASSWORDStruct;
import com.mindoo.domino.jna.internal.structs.NIFFindByKeyContextStruct;
import com.mindoo.domino.jna.internal.structs.NotesBlockIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesBuildVersionStruct;
import com.mindoo.domino.jna.internal.structs.NotesCalendarActionDataStruct;
import com.mindoo.domino.jna.internal.structs.NotesCollectionPositionStruct;
import com.mindoo.domino.jna.internal.structs.NotesDbReplicaInfoStruct;
import com.mindoo.domino.jna.internal.structs.NotesFTIndexStatsStruct;
import com.mindoo.domino.jna.internal.structs.NotesItemDefinitionTableExt;
import com.mindoo.domino.jna.internal.structs.NotesItemDefinitionTableLock;
import com.mindoo.domino.jna.internal.structs.NotesOriginatorIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDatePairStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCompoundStyleStruct;
import com.mindoo.domino.jna.internal.structs.html.HtmlApi_UrlComponentStruct;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

public interface INotesNativeAPI64 extends Library {

	short NSFSearch(
			long hDB,
			long hFormula,
			Memory viewTitle,
			short SearchFlags,
			short NoteClassMask,
			NotesTimeDateStruct Since,
			NotesCallbacks.NsfSearchProc enumRoutine,
			Pointer EnumRoutineParameter,
			NotesTimeDateStruct retUntil);

	@UndocumentedAPI
	short NSFSearchExtended3 (long hDB, 
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
			NotesCallbacks.NsfSearchProc EnumRoutine,
			Pointer EnumRoutineParameter, 
			NotesTimeDateStruct retUntil, 
			long namelist);
	
	@UndocumentedAPI
	short NSFGetFolderSearchFilter(long hViewDB, long hDataDB, int ViewNoteID, NotesTimeDateStruct Since, int Flags, LongByReference Filter);

	/**
	 * @deprecated use {@link Mem64#OSLockObject(long)} instead
	 */
	@Deprecated
	Pointer OSLockObject(long handle);
	/**
	 * @deprecated use {@link Mem64#OSUnlockObject(long)} instead
	 */
	@Deprecated
	boolean OSUnlockObject(long handle);
	/**
	 * @deprecated use {@link Mem64#OSMemFree(long)} instead
	 */
	@Deprecated
	short OSMemFree(long handle);
	/**
	 * @deprecated use {@link Mem64#OSMemGetSize(long, IntByReference)} instead
	 */
	@Deprecated
	short OSMemGetSize(long handle, IntByReference retSize);
	/**
	 * @deprecated use {@link Mem64#OSMemoryGetSize(long)} instead
	 */
	@Deprecated
	int OSMemoryGetSize(long handle);
	/**
	 * @deprecated use {@link Mem64#OSMemoryAllocate(int, int, LongByReference)} instead
	 */
	@Deprecated
	short OSMemoryAllocate(int dwtype, int size, LongByReference retHandle);
	/**
	 * @deprecated use {@link Mem64#OSMemoryFree(long)} instead
	 */
	@Deprecated
	void OSMemoryFree(long handle);
	/**
	 * @deprecated use {@link Mem64#OSMemoryReallocate(long, int)} instead
	 */
	@Deprecated
	short OSMemoryReallocate(long handle, int size);
	/**
	 * @deprecated use {@link Mem64#OSMemoryLock(long)} instead
	 */
	@Deprecated
	Pointer OSMemoryLock(long handle);
	/**
	 * @deprecated use {@link Mem64#OSMemoryUnlock(long)} instead
	 */
	@Deprecated
	boolean OSMemoryUnlock(long handle);
	
	/**
	 * @deprecated use {@link Mem64#OSMemAlloc(short, int, LongByReference)} instead
	 */
	@Deprecated
	short OSMemAlloc(
			short  BlkType,
			int  dwSize,
			LongByReference retHandle);

	@Deprecated
	short OSMemGetType(long handle);
	
	short NSFItemGetText(
			long  note_handle,
			Memory item_name,
			Memory item_text,
			short text_len);

	short ListAllocate(
			short ListEntries,
			short TextSize,
			int fPrefixDataType,
			LongByReference rethList,
			Memory retpList,
			ShortByReference retListSize);
	
	short ListAddEntry(
			long hList,
			int fPrefixDataType,
			ShortByReference pListSize,
			short EntryNumber,
			Memory Text,
			short TextSize);
	
	short ListRemoveAllEntries(
			long hList,
			int fPrefixDataType,
			ShortByReference pListSize);

	short NSFItemInfo(
			long note_handle,
			Memory item_name,
			short  name_len,
			NotesBlockIdStruct retbhItem,
			ShortByReference retDataType,
			NotesBlockIdStruct retbhValue,
			IntByReference retValueLength);

	short NSFItemInfoNext(
			long  note_handle,
			NotesBlockIdStruct.ByValue NextItem,
			Memory item_name,
			short  name_len,
			NotesBlockIdStruct retbhItem,
			ShortByReference retDataType,
			NotesBlockIdStruct retbhValue,
			IntByReference retValueLength);

	short NSFItemInfoPrev(
			long  note_handle,
			NotesBlockIdStruct.ByValue  CurrItem,
			Memory item_name,
			short  name_len,
			NotesBlockIdStruct item_blockid_ptr,
			ShortByReference value_type_ptr,
			NotesBlockIdStruct value_blockid_ptr,
			IntByReference value_len_ptr);
	
	void NSFItemQueryEx(
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
	
	short NSFItemGetModifiedTimeByBLOCKID(
			long  hNote,
			NotesBlockIdStruct.ByValue bhItem,
			int  Flags,
			NotesTimeDateStruct retTime);

	short NSFItemGetTextListEntries(
			long note_handle,
			Memory item_name);

	short NSFItemGetTextListEntry(
			long note_handle,
			Memory item_name,
			short entry_position,
			Memory retEntry_text,
			short  text_len);
	short NSFItemGetModifiedTime(
			long hNote,
			Memory ItemName,
			short  ItemNameLength,
			int  Flags,
			NotesTimeDateStruct retTime);
	short NSFItemSetText(
			long hNote,
			Memory ItemName,
			Memory ItemText,
			short TextLength);
	short NSFItemSetTextSummary(
			long  hNote,
			Memory ItemName,
			Memory ItemText,
			short TextLength,
			boolean summary);
	boolean NSFItemGetTime(
			long  note_handle,
			Memory td_item_name,
			NotesTimeDateStruct td_item_value);
	short NSFItemSetTime(
			long  note_handle,
			Memory td_item_name,
			NotesTimeDateStruct td_item_ptr);
	boolean NSFItemGetNumber(
			long  hNote,
			Memory ItemName,
			DoubleByReference retNumber);
	int NSFItemGetLong(
			long note_handle,
			Memory number_item_name,
			int number_item_default);
	short NSFItemSetNumber(
			long hNote,
			Memory ItemName,
			Memory Number);
	short NSFItemConvertToText(
			long note_handle,
			Memory item_name_ptr,
			Memory retText_buf_ptr,
			short  text_buf_len,
			char separator);
	short NSFItemConvertValueToText(
			short value_type,
			NotesBlockIdStruct.ByValue value_bid,
			int  value_len,
			Memory text_buf_ptr,
			short  text_buf_len,
			char separator);
	short NSFItemDelete(
			long note_handle,
			Memory item_name,
			short name_len);
	short NSFItemDeleteByBLOCKID(long note_handle, NotesBlockIdStruct.ByValue item_blockid);
	short NSFItemAppend(
			long note_handle,
			short  item_flags,
			Memory item_name,
			short name_len,
			short  item_type,
			Pointer item_value,
			int value_len);
	short NSFItemAppendByBLOCKID(
			long note_handle,
			short item_flags,
			Memory item_name,
			short name_len,
			NotesBlockIdStruct.ByValue value_bid,
			int value_len,
			NotesBlockIdStruct item_bid_ptr);

	void NSFNoteGetInfo(long hNote, short type, Pointer retValue);
	void NSFNoteSetInfo(long hNote, short type, Pointer value);
	short NSFNoteCopy(
			long note_handle_src,
			LongByReference note_handle_dst_ptr);
	short NSFNoteUpdateExtended(long hNote, int updateFlags);
	short NSFNoteCreate(long db_handle, LongByReference note_handle);
	short NSFNoteOpen(long hDB, int noteId, short openFlags, LongByReference rethNote);
	short NSFNoteOpenExt(long hDB, int noteId, int flags, LongByReference rethNote);
	short NSFNoteOpenByUNID(
			long  hDB,
			NotesUniversalNoteIdStruct pUNID,
			short  flags,
			LongByReference rethNote);
	short NSFNoteOpenByUNIDExtended(long hDB, NotesUniversalNoteIdStruct pUNID, int flags, LongByReference rtn); 
	short NSFNoteClose(long hNote);
	short NSFNoteVerifySignature(
			long  hNote,
			Memory SignatureItemName,
			NotesTimeDateStruct retWhenSigned,
			Memory retSigner,
			Memory retCertifier);
	short NSFNoteContract(long hNote);
	@UndocumentedAPI
	short NSFNoteExpand(long hNote);
	short NSFNoteSign(long hNote);
	short NSFNoteSignExt3(long hNote, 
			long	hKFC,
			Memory SignatureItemName,
			short ItemCount, long hItemIDs, 
			int Flags, int Reserved,
			Pointer pReserved);
	short NSFNoteOpenSoftDelete(long hDB, int NoteID, int Reserved, LongByReference rethNote);
	short NSFNoteHardDelete(long hDB, int NoteID, int Reserved);
	short NSFNoteDeleteExtended(long hDB, int NoteID, int UpdateFlags);
	short NSFNoteDetachFile(long note_handle, NotesBlockIdStruct.ByValue item_blockid);
	boolean NSFNoteIsSignedOrSealed(long note_handle, ByteByReference signed_flag_ptr, ByteByReference sealed_flag_ptr);
	short NSFNoteUnsign(long hNote);
	short NSFNoteComputeWithForm(
			long  hNote,
			long  hFormNote,
			int  dwFlags,
			NotesCallbacks.b64_CWFErrorProc ErrorRoutine,
			Pointer CallersContext);
	short NSFNoteHasComposite(long hNote);
	short NSFNoteHasMIME(long hNote);
	short NSFNoteHasMIMEPart(long hNote);
	@UndocumentedAPI
	short NSFIsFileItemMimePart(long hNote, NotesBlockIdStruct.ByValue bhFileItem);
	@UndocumentedAPI
	short NSFIsMimePartInFile(long hNote, NotesBlockIdStruct.ByValue bhMIMEItem, Memory pszFileName, short wMaxFileNameLen);

	short NSFMimePartCreateStream(
			long hNote,
			Memory pchItemName,
			short wItemNameLen,
			short wPartType,
			int dwFlags,
			LongByReference phCtx);
	
	short NSFMimePartAppendStream(
			long hCtx,
			Memory pchData,
			short wDataLen);
	
	short NSFMimePartAppendFileToStream(
			long hCtx,
			Memory pszFilename);
	short NSFMimePartAppendObjectToStream(
			long hCtx,
			Memory pszAttachmentName);
	short NSFMimePartCloseStream(
			long hCtx,
			short bUpdate);
//	short MIMEStreamOpen(
//			long hNote,
//			Memory pchItemName,
//			short wItemNameLen,
//			int dwOpenFlags,
//			LongByReference rethMIMEStream);
//	int MIMEStreamPutLine(
//			Memory pszLine,
//			long hMIMEStream);
//	short MIMEStreamItemize(
//			long hNote,
//			Memory pchItemName,
//			short wItemNameLen,
//			int dwFlags,
//			long hMIMEStream);
//	int MIMEStreamWrite(
//			Pointer pchData,
//			int uiDataLen,
//			long hMIMEStream);
//	void MIMEStreamClose(
//			int hMIMEStream);
	short MIMEConvertRFC822TextItemByBLOCKID(
			long hNote,
			NotesBlockIdStruct.ByValue bhItem,
			NotesBlockIdStruct.ByValue bhValue);

	short NSFNoteHasReadersField(long hNote, NotesBlockIdStruct bhFirstReadersItem);
	short NSFNoteCipherExtractWithCallback (long hNote, NotesBlockIdStruct.ByValue bhItem,
			int ExtractFlags, int hDecryptionCipher,
			NotesCallbacks.NoteExtractCallback pNoteExtractCallback, Pointer pParam,
			int Reserved, Pointer pReserved);
	short NSFNoteCopyAndEncryptExt2(
			long  hSrcNote,
			long hKFC,
			short EncryptFlags,
			LongByReference rethDstNote,
			int Reserved,
			Pointer pReserved);
	short NSFNoteCopyAndEncrypt(
			long hSrcNote,
			short EncryptFlags,
			LongByReference rethDstNote);
	short NSFNoteCipherDecrypt(
			long  hNote,
			long  hKFC,
			int  DecryptFlags,
			LongByReference rethCipherForAttachments,
			int  Reserved,
			Pointer pReserved);
	short NSFNoteAttachFile(
			long note_handle,
			Memory item_name,
			short item_name_length,
			Memory file_name,
			Memory orig_path_name,
			short encoding_type);
	short NSFNoteSignHotspots(
			long hNote,
			int dwFlags,
			IntByReference retfSigned);
	short NSFNoteLSCompile(
			long hDb,
			long hNote,
			int dwFlags);
	short NSFNoteLSCompileExt(
			long hDb,
			long hNote,
			int dwFlags,
			NotesCallbacks.LSCompilerErrorProc pfnErrProc,
			Pointer pCtx);
	short NSFNoteCheck(
			long hNote
			);

	short NSFDbNoteLock(
			long hDB,
			int NoteID,
			int Flags,
			Memory pLockers,
			LongByReference rethLockers,
			IntByReference retLength);
	
	short NSFDbNoteUnlock(
			long hDB,
			int NoteID,
			int Flags);

	short NSFNoteOpenWithLock(
			long hDB,
			int NoteID,
			int LockFlags,
			int OpenFlags,
			Memory pLockers,
			LongByReference rethLockers,
			IntByReference retLength,
			LongByReference rethNote);

	short NSFItemCopy(long note_handle, NotesBlockIdStruct.ByValue item_blockid);
	short NSFItemCopyAndRename (long hNote, NotesBlockIdStruct.ByValue bhItem, Memory pszNewItemName);

	short IDCreateTable (int alignment, LongByReference rethTable);
	short IDDestroyTable(long hTable);
	short IDInsert (long hTable, int id, IntByReference retfInserted);
	short IDDelete (long hTable, int id, IntByReference retfDeleted);
	boolean IDScan (long hTable, boolean fFirst, IntByReference retID);
	boolean IDScanBack (long hTable, boolean fLast, IntByReference retID);
	int IDEntries (long hTable);
	boolean IDIsPresent (long hTable, int id);
	int IDTableSize (long hTable);
	int IDTableSizeP(Pointer pIDTable);
	short IDTableCopy (long hTable, LongByReference rethTable);
	short IDTableIntersect(long hSrc1Table, long hSrc2Table, LongByReference rethDstTable);
	short IDDeleteAll (long hTable);
	boolean IDAreTablesEqual	(long hSrc1Table, long hSrc2Table);
	short IDDeleteTable  (long hTable, long hIDsToDelete);
	short IDInsertTable  (long hTable, long hIDsToAdd);
	short IDEnumerate(long hTable, NotesCallbacks.IdEnumerateProc Routine, Pointer Parameter);
	@UndocumentedAPI
	short IDInsertRange(long hTable, int IDFrom, int IDTo, boolean AddToEnd);
	@UndocumentedAPI
	short IDTableDifferences(long idtable1, long idtable2, LongByReference outputidtableAdds, LongByReference outputidtableDeletes, LongByReference outputidtableSame);
	@UndocumentedAPI
	short IDTableReplaceExtended(long idtableSrc, long idtableDest, byte flags);

	short NSFDbStampNotesMultiItem(long hDB, long hTable, long hInNote);
	short NSFDbOpen(Memory dbName, LongByReference dbHandle);

	short NSFDbClose(long dbHandle);
	short NSFDbOpenExtended (Memory PathName, short Options, long hNames, NotesTimeDateStruct ModifiedTime, LongByReference rethDB, NotesTimeDateStruct retDataModified, NotesTimeDateStruct retNonDataModified);
	short NSFDbGenerateOID(long hDB, NotesOriginatorIdStruct retOID);
	int NSFDbGetOpenDatabaseID(long hDBU);
	short NSFDbReopen(long hDB, LongByReference rethDB);
	short NSFDbLocateByReplicaID(
			long hDB,
			NotesTimeDateStruct ReplicaID,
			Memory retPathName,
			short PathMaxLen);
	short NSFDbModifiedTime(
			long hDB,
			NotesTimeDateStruct retDataModified,
			NotesTimeDateStruct retNonDataModified);
	short NSFDbIDGet(long hDB, NotesTimeDateStruct retDbID);
	short NSFDbReplicaInfoGet(
			long  hDB,
			NotesDbReplicaInfoStruct retReplicationInfo);
	short NSFDbReplicaInfoSet(
			long  hDB,
			NotesDbReplicaInfoStruct ReplicationInfo);
	short NSFDbGetModifiedNoteTable(long hDB, short NoteClassMask, NotesTimeDateStruct.ByValue Since, NotesTimeDateStruct retUntil, LongByReference rethTable);
	short NSFDbGetNotes(
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
	short NSFDbGetMultNoteInfo(
			long  hDb,
			short  Count,
			short  Options,
			long  hInBuf,
			IntByReference retSize,
			LongByReference rethOutBuf);
	short NSFDbGetNoteInfoExt(
			long  hDB,
			int  NoteID,
			NotesOriginatorIdStruct retNoteOID,
			NotesTimeDateStruct retModified,
			ShortByReference retNoteClass,
			NotesTimeDateStruct retAddedToFile,
			ShortByReference retResponseCount,
			IntByReference retParentNoteID);
	short NSFDbGetMultNoteInfoByUNID(
			long hDB,
			short Count,
			short Options,
			long hInBuf,
			IntByReference retSize,
			LongByReference rethOutBuf);
	short NSFDbSign(long hDb, short noteclass);
	@UndocumentedAPI
	short NSFDbGetOptionsExt(long hDB, Memory retDbOptions);
	@UndocumentedAPI
	short NSFDbSetOptionsExt(long hDB, Memory dbOptions, Memory mask);
	void NSFDbAccessGet(long hDB, ShortByReference retAccessLevel, ShortByReference retAccessFlag);
	short NSFDbGetBuildVersion(long hDB, ShortByReference retVersion);
	short NSFDbGetMajMinVersion(long hDb, NotesBuildVersionStruct retBuildVersion);
	
	short NSFItemAppendObject(
			long hNote,
			short ItemFlags,
			Memory Name,
			short NameLength,
			NotesBlockIdStruct.ByValue bhValue,
			int ValueLength,
			int fDealloc);
	
	short NSFDbGetObjectSize(
			long hDB,
			int ObjectID,
			short ObjectType,
			IntByReference retSize,
			ShortByReference retClass,
			ShortByReference retPrivileges);
	short NSFDbGetSpecialNoteID(
			long hDB,
			short Index,
			IntByReference retNoteID);
	short NSFDbClearReplHistory(long hDb, int dwFlags);
	short NSFDbPathGet(
			long hDB,
			Memory retCanonicalPathName,
			Memory retExpandedPathName);
	short NSFDbIsRemote(long hDb);
	short NSFDbHasFullAccess(long hDb);
	short NSFDbSpaceUsage(long dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes);
	short NSFDbSpaceUsageScaled (long dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes, IntByReference retGranularity);
	@UndocumentedAPI
	short NSFHideDesign(long hdb1, long hdb2, int param3, int param4);
	short NSFDbDeleteNotes(long hDB, long hTable, Memory retUNIDArray);
	short NSFDbIsLocallyEncrypted(long hDB, IntByReference retVal);
	short NSFDbInfoGet(
			long hDB,
			Pointer retBuffer);
	short NSFDbInfoSet(
			long hDB,
			Pointer Buffer);
	short NSFDbModeGet(
			long  hDB,
			ShortByReference retMode);
	@UndocumentedAPI
	short NSFDbLock(long hDb);
	@UndocumentedAPI
	void NSFDbUnlock(long hDb, ShortByReference statusInOut);

	short NSFBuildNamesList(Memory UserName, int dwFlags, LongByReference rethNamesList);
	@UndocumentedAPI
	short CreateNamesListFromGroupNameExtend(Memory pszServerName, Memory pTarget, LongByReference rethNames);
	@UndocumentedAPI
	short CreateNamesListFromNames(short cTargets, Pointer ptrArrTargets, LongByReference rethNames);
	@UndocumentedAPI
	short CreateNamesListFromNamesExtend(Memory pszServerName, short cTargets, Pointer ptrArrTargets, LongByReference rethNames);
	@UndocumentedAPI
	short CreateNamesListFromSingleName(Memory pszServerName, short fDontLookupAlternateNames,
			Pointer pLookupFlags, Memory pTarget, LongByReference rethNames);
	@UndocumentedAPI
	short CreateNamesListUsingLookupName(Memory pszServerName,Pointer pLookupFlags, Memory pTarget,
			LongByReference rethNames);

	short NIFReadEntries(long hCollection, NotesCollectionPositionStruct IndexPos, short SkipNavigator, int SkipCount, short ReturnNavigator, int ReturnCount, int ReturnMask, LongByReference rethBuffer,
			ShortByReference retBufferLength, IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned, ShortByReference retSignalFlags);
	short NIFReadEntriesExt(long hCollection,
			NotesCollectionPositionStruct CollectionPos,
            short SkipNavigator, int SkipCount,
            short ReturnNavigator, int ReturnCount, int ReturnMask,
            NotesTimeDateStruct DiffTime, long DiffIDTable, int ColumnNumber, int Flags,
            LongByReference rethBuffer, ShortByReference retBufferLength,
            IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned,
            ShortByReference retSignalFlags, NotesTimeDateStruct retDiffTime,
            NotesTimeDateStruct retModifiedTime, IntByReference retSequence);
	void NIFGetLastModifiedTime(long hCollection, NotesTimeDateStruct retLastModifiedTime);
	@UndocumentedAPI
	void NIFGetLastAccessedTime(long hCollection, NotesTimeDateStruct retLastModifiedTime);
	@UndocumentedAPI
	void NIFGetNextDiscardTime(long hCollection, NotesTimeDateStruct retLastModifiedTime);
	short NIFFindByKeyExtended2 (long hCollection, Memory keyBuffer,
			int findFlags,
			int returnFlags,
			NotesCollectionPositionStruct retIndexPos,
			IntByReference retNumMatches,
			ShortByReference retSignalFlags,
			LongByReference rethBuffer,
			IntByReference retSequence);
	@UndocumentedAPI
	short NIFFindByKeyExtended3 (long hCollection,
			Memory keyBuffer, int findFlags,
			int returnFlags,
			NotesCollectionPositionStruct retIndexPos,
			IntByReference retNumMatches, ShortByReference retSignalFlags,
			LongByReference rethBuffer, IntByReference retSequence,
			NotesCallbacks.NIFFindByKeyProc NIFFindByKeyCallback, NIFFindByKeyContextStruct Ctx);
	
//	STATUS far PASCAL NIFFindByKeyExtended3 (HCOLLECTION hCollection,
//			void *KeyBuffer, DWORD FindFlags,
//			DWORD ReturnFlags, 
//			COLLECTIONPOSITION *retIndexPos,
//			DWORD *retNumMatches, WORD *retSignalFlags,
//			DHANDLE *rethBuffer, DWORD *retSequence,
//			NIFFINDBYKEYPROC NIFFindByKeyCallback, NIFFINDBYKEYCTX *Ctx); 
	
	short NIFFindByKey(long hCollection, Memory keyBuffer, short findFlags, NotesCollectionPositionStruct retIndexPos, IntByReference retNumMatches);
	short NIFFindByName(long hCollection, Memory name, short findFlags, NotesCollectionPositionStruct retIndexPos, IntByReference retNumMatches);
	short NIFGetCollation(long hCollection, ShortByReference retCollationNum);
	short NIFSetCollation(long hCollection, short CollationNum);
	short NIFUpdateCollection(long hCollection);
	@UndocumentedAPI
	short NIFIsNoteInView(long hCollection, int noteID, IntByReference retIsInView);
	@UndocumentedAPI
	boolean NIFIsUpdateInProgress(long hCollection);
	@UndocumentedAPI
	short NIFGetIDTableExtended(long hCollection, short navigator, short Flags, long hIDTable);
	@UndocumentedAPI
	boolean NIFCollectionUpToDate(long hCollection);
	@UndocumentedAPI
	boolean NIFSetCollectionInfo (long hCollection, Pointer SessionID,
            long hUnreadList, long hCollapsedList, long hSelectedList);
	@UndocumentedAPI
    short NIFUpdateFilters (long hCollection, short ModifyFlags);
	@UndocumentedAPI
    boolean NIFIsTimeVariantView(long hCollection);
	short NIFCloseCollection(long hCollection);
	short NIFLocateNote (long hCollection, NotesCollectionPositionStruct indexPos, int noteID);
	@UndocumentedAPI
	short NIFFindDesignNoteExt(long hFile, Memory name, short noteClass, Memory pszFlagsPattern, IntByReference retNoteID, int Options);
	short NIFOpenCollection(long hViewDB, long hDataDB, int ViewNoteID, short OpenFlags, long hUnreadList, LongByReference rethCollection, LongByReference rethViewNote, Memory retViewUNID,
			LongByReference rethCollapsedList, LongByReference rethSelectedList);
	short NIFOpenCollectionWithUserNameList (long hViewDB, long hDataDB,
			int ViewNoteID, short OpenFlags,
			long hUnreadList,
			LongByReference rethCollection,
			LongByReference rethViewNote, Memory retViewUNID,
			LongByReference rethCollapsedList,
			LongByReference rethSelectedList,
			long nameList);
	short NIFGetCollectionData(
			long hCollection,
			LongByReference rethCollData);
	@UndocumentedAPI
	short NIFGetCollectionDocCountLW(long hCol, IntByReference pDocct);

	@UndocumentedAPI
	short NSFTransactionBegin(long hDB, int flags);
	@UndocumentedAPI
	short NSFTransactionCommit(long hDB, int flags);
	@UndocumentedAPI
	short NSFTransactionRollback(long hDB);

	//backup APIs
	short NSFDbGetLogInfo(long hDb, int Flags, ShortByReference LOGGED, NotesUniversalNoteIdStruct LogID,
			NotesUniversalNoteIdStruct DbIID, IntByReference LogExtent);
	short NSFBackupStart(long hDB, int Flags, IntByReference BackupContext, IntByReference FileSizeLow,
			IntByReference FileSizeHigh);
	short NSFBackupStop(long hDB, int BackupContext);
	short NSFBackupEnd(long hDB, int BackupContext, int Options);
	short NSFBackupGetChangeInfoSize( long hDB, int hBackupContext, int Flags, IntByReference InfoSizeLow,
			IntByReference InfoSizeHigh);
	short NSFBackupStartApplyChangeInfo(IntByReference ApplyInfoContext, Memory CopyFilePath, int Flags,
			int InfoSizeLow, int InfoSizeHigh);
	short NSFBackupGetNextChangeInfo(long hDB, int hBackupContext, int Flags, Memory Buffer, int BufferSize,
			IntByReference FilledSize);
	short NSFBackupApplyNextChangeInfo(long ApplyInfoContext, int Flags, Memory Buffer, int BufferSize);
	short NSFBackupEndApplyChangeInfo(int ApplyInfoContext, int Flags);

	short AgentDelete (long hAgent); /* delete agent */
	@UndocumentedAPI
	boolean IsRunAsWebUser(long hAgent);
	short AgentOpen (long hDB, int AgentNoteID, LongByReference rethAgent);
	void AgentClose (long hAgent);
	short AgentCreateRunContext (long hAgent,
			 Pointer pReserved,
			 int dwFlags,
			 LongByReference rethContext);
	@UndocumentedAPI
	short AgentCreateRunContextExt (long hAgent, Pointer pReserved, long pOldContext, int dwFlags, LongByReference rethContext);
	short AgentSetDocumentContext(long hAgentCtx, long hNote);
	short AgentSetTimeExecutionLimit(long hAgentCtx, int timeLimit);
	boolean AgentIsEnabled(long hAgent);
	void SetParamNoteID(long hAgentCtx, int noteId);
	@UndocumentedAPI
	short AgentSetUserName(long hAgentCtx, long hNameList);
	short AgentRedirectStdout(long hAgentCtx, short redirType);
	void AgentQueryStdoutBuffer(long hAgentCtx, LongByReference retHdl, IntByReference retSize);
	void AgentDestroyRunContext (long hAgentCtx);
	short AgentRun (long hAgent,
			long hAgentCtx,
		    int hSelection,
			int dwFlags);
	short AgentSetHttpStatusCode(long hAgentCtx, int httpStatus);
	short ClientRunServerAgent(long hdb, int nidAgent, int nidParamDoc,
			int bForeignServer, int bSuppressPrintToConsole);

	short FTIndex(long hDB, short options, Memory stopFile, NotesFTIndexStatsStruct retStats);
	@UndocumentedAPI
	short ClientFTIndexRequest(long hDB);
	short FTDeleteIndex(long hDB);
	short FTGetLastIndexTime(long hDB, NotesTimeDateStruct retTime);
	short FTOpenSearch(LongByReference rethSearch);
	short FTCloseSearch(long hSearch);
	short FTSearch(
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

	@UndocumentedAPI
	short FTSearchExt(
			long hDB,
			LongByReference phSearch,
			long hColl,
			Memory query,
			int options,
			short limit,
			long hRefineIDTable,
			IntByReference retNumDocs,
			LongByReference rethStrings,
			LongByReference rethResults,
			IntByReference retNumHits,
			int start,
			int count,
			short arg,
			long hNames
			);
	
	short NSFFormulaCompile(
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
	short NSFFormulaDecompile(
			Pointer pFormulaBuffer,
			boolean fSelectionFormula,
			LongByReference rethFormulaText,
			ShortByReference retFormulaTextLength);
	short NSFFormulaSummaryItem(long hFormula, Memory ItemName, short ItemNameLength);
	short NSFFormulaMerge(
			long hSrcFormula,
			long hDestFormula);
	short NSFComputeStart(
			short Flags,
			Pointer lpCompiledFormula,
			LongByReference rethCompute);
	short NSFComputeStop(long hCompute);
	short NSFComputeEvaluate(
			long  hCompute,
			long hNote,
			LongByReference rethResult,
			ShortByReference retResultLength,
			IntByReference retNoteMatchesFormula,
			IntByReference retNoteShouldBeDeleted,
			IntByReference retNoteModified);

	@UndocumentedAPI
	short CESCreateCTXFromNote(int hNote, LongByReference rethCESCTX);
	@UndocumentedAPI
	short CESGetNoSigCTX(LongByReference rethCESCTX);
	@UndocumentedAPI
	void CESFreeCTX(long hCESCTX);
	@UndocumentedAPI
	short ECLUserTrustSigner ( long hCESCtx, 
			short ECLType,
			short bSessionOnly,
			short wCapabilities,
			short wCapabilities2,
			ShortByReference retwCurrentCapabilities,
			ShortByReference retwCurrentCapabilities2);

	short NSFFolderGetIDTable(
			long  hViewDB,
			long hDataDB,
			int  viewNoteID,
			int  flags,
			LongByReference hTable);
	
	short NSFGetAllFolderChanges(
			long hViewDB,
			long hDataDB,
			NotesTimeDateStruct since,
			int flags,
			NotesCallbacks.b64_NSFGetAllFolderChangesCallback Callback,
			Pointer Param,
			NotesTimeDateStruct until);

	short NSFGetFolderChanges(
			long hViewDB,
			long hDataDB,
			int viewNoteID,
			NotesTimeDateStruct since,
			int Flags,
			LongByReference addedNoteTable,
			LongByReference removedNoteTable);
	
	short FolderDocAdd(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  hTable,
			int  dwFlags);
	short FolderDocCount(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			int dwFlags,
			IntByReference pdwNumDocs);
	short FolderDocRemove(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  hTable,
			int dwFlags);
	short FolderDocRemoveAll(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			int dwFlags);

	short FolderCreate(
			long hDataDB,
			long hFolderDB,
			int FormatNoteID,
			long hFormatDB,
			Memory pszName,
			short wNameLen,
			int FolderType,
			int dwFlags,
			IntByReference pNoteID);
	
	short FolderRename(
			long hDataDB,
			long hFolderDB,
			int FolderNoteID,
			Memory pszName,
			short wNameLen,
			int dwFlags);

	short FolderDelete(
			long hDataDB,
			long hFolderDB,
			int FolderNoteID,
			int dwFlags);
	
	short FolderCopy(
			long hDataDB,
			long hFolderDB,
			int FolderNoteID,
			Memory pszName,
			short wNameLen,
			int dwFlags,
			IntByReference pNewNoteID);

	short FolderMove(
			long hDataDB,
			long hFolderDB,
			int FolderNoteID,
			long hParentDB,
			int ParentNoteID,
			int dwFlags);
	
	short NSFProfileOpen(
			long hDB,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength,
			short CopyProfile,
			LongByReference rethProfileNote);
	short NSFProfileUpdate(
			long hProfile,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength);
	short NSFProfileSetField(
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
	short NSFProfileDelete(
			long hDB,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength);
	
	short NSFProfileEnum(
			long hDB,
			Memory ProfileName,
			short ProfileNameLength,
			NotesCallbacks.b64_NSFPROFILEENUMPROC Callback,
			Pointer CallbackCtx,
			int Flags);

	short SECKFMOpen(LongByReference phKFC, Memory pIDFileName, Memory pPassword,
			int Flags, int Reserved, Pointer pReserved);

	short SECKFMClose(LongByReference phKFC, int Flags, int Reserved, Pointer pReserved);
	@UndocumentedAPI
	short SECKFMAccess(short param1, long hKFC, Pointer retUsername, Pointer param4);

	short SECidfGet(Memory pUserName, Memory pPassword, Memory pPutIDFileHere,
			LongByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved);
	short SECidfPut(Memory pUserName, Memory pPassword, Memory pIDFilePath,
			LongByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved);
	short SECidfSync( Memory pUserName, Memory pPassword, Memory pIDFilePath,
			LongByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved, IntByReference retdwFlags);
	short SECTokenGenerate(
			Memory ServerName,
			Memory OrgName,
			Memory ConfigName,
			Memory UserName,
			NotesTimeDateStruct Creation,
			NotesTimeDateStruct Expiration,
			LongByReference retmhToken,
			int dwReserved,
			Pointer vpReserved);
	void SECTokenFree(LongByReference mhToken);

	short SchFreeTimeSearch(
			NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate,
			short fFindFirstFit,
			int dwReserved,
			NotesTimeDatePairStruct pInterval,
			short Duration,
			Pointer pNames,
			LongByReference rethRange);

	short SchRetrieve(
			NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate,
			int dwOptions,
			NotesTimeDatePairStruct pInterval,
			Pointer pNames,
			LongByReference rethCntnr,
			Pointer mustBeNull1,
			Pointer mustBeNull2,
			Pointer mustBeNull3);

	short SchSrvRetrieveExt(
			Pointer pClientNames,
			NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate,
			int dwOptions,
			NotesTimeDatePairStruct pInterval,
			Pointer pNames,
			Pointer pDetails,
			Pointer piCalList,
			Memory pszProxyUserName,
			Memory pszProxyPassword,
			LongByReference rethCntnr);
	
	void SchContainer_Free(long hCntnr);
	short SchContainer_GetFirstSchedule(
			long hCntnr,
			IntByReference rethObj,
			Memory retpSchedule);
	short Schedule_Free(long hCntnr, int hSched);
	short SchContainer_GetNextSchedule(
			long hCntnr,
			int hCurSchedule,
			IntByReference rethNextSchedule,
			Memory retpNextSchedule);
	short Schedule_ExtractFreeTimeRange(
			long hCntnr,
			int hSchedObj,
			NotesUniversalNoteIdStruct punidIgnore,
			short fFindFirstFit,
			short wDuration,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethRange);
	short Schedule_ExtractBusyTimeRange(
			long hCntnr,
			int hSchedObj,
			NotesUniversalNoteIdStruct punidIgnore,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethRange,
			IntByReference rethMoreCtx);
	short Schedule_ExtractMoreBusyTimeRange(
			long hCntnr,
			int hMoreCtx,
			NotesUniversalNoteIdStruct punidIgnore,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethRange,
			IntByReference rethMore);
	short Schedule_ExtractSchedList(
			long hCntnr,
			int hSchedObj,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethSchedList,
			IntByReference rethMore);
	short Schedule_ExtractMoreSchedList(
			long hCntnr,
			int hMoreCtx,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethSchedList,
			IntByReference rethMore);
	short Schedule_Access(
			long hCntnr,
			int hSched,
			PointerByReference pretSched);
	short Schedule_GetFirstDetails(
			long hCntnr,
			int hSchedObj,
			IntByReference rethDetailObj,
			PointerByReference retpDetail);
	
	short Schedule_GetNextDetails(
			long hCntnr,
			int hDetailObj,
			IntByReference rethNextDetailObj,
			PointerByReference retpNextDetail);
	short NSGetServerClusterMates(
			Memory pServerName,
			int dwFlags,
			LongByReference phList);
	short NSPingServer(
			Memory pServerName,
			IntByReference pdwIndex,
			LongByReference phList);

	short HTMLCreateConverter(LongByReference phHTML);
	short HTMLDestroyConverter(long hHTML);
	short HTMLSetHTMLOptions(long hHTML, StringArray optionList);
	short HTMLConvertItem(
			long hHTML,
			long hDB,
			long hNote,
			Memory pszItemName);
	short HTMLConvertNote(
			long hHTML,
			long hDB,
			long hNote,
			int NumArgs,
			HtmlApi_UrlComponentStruct pArgs);
	short HTMLGetProperty(
			long hHTML,
			long PropertyType,
			Pointer pProperty);
	
	@UndocumentedAPI
	short HTMLGetPropertyV (long hHTML,
			 long PropertyType, Pointer pProperty, int count);

	short HTMLSetProperty(
			long hHTML,
			long PropertyType,
			Pointer pProperty);
	short HTMLGetText(
			long hHTML,
			int startingOffset,
			IntByReference pTextLength,
			Memory pText);
	short HTMLGetReference(
			long hHTML,
			int Index,
			LongByReference phRef);
	short HTMLLockAndFixupReference(
			long hRef,
			Memory ppRef);
	short HTMLConvertElement(
			long hHTML,
			long hDB,
			long hNote,
			Memory pszItemName,
			int ItemIndex,
			int Offset);

	short CompoundTextAddCDRecords(
			long hCompound,
			Pointer pvRecord,
			int dwRecordLength);

	short CompoundTextAddDocLink(
			long hCompound,
			NotesTimeDateStruct.ByValue DBReplicaID,
			NotesUniversalNoteIdStruct.ByValue ViewUNID,
			NotesUniversalNoteIdStruct.ByValue NoteUNID,
			Memory pszComment,
			int dwFlags);
	short CompoundTextAddParagraphExt(
			long hCompound,
			int dwStyleID,
			int FontID,
			Memory pchText,
			int dwTextLen,
			Pointer pInfo);
	short CompoundTextAddRenderedNote(
			long hCompound,
			long hNote,
			long hFormNote,
			int dwFlags);
	short CompoundTextAddTextExt(
			long hCompound,
			int dwStyleID,
			int FontID,
			Memory pchText,
			int dwTextLen,
			Memory pszLineDelim,
			int dwFlags,
			Pointer pInfo);
	short CompoundTextAssimilateFile(
			long hCompound,
			Memory pszFileName,
			int dwFlags);
	short CompoundTextAssimilateItem(
			long hCompound,
			long hNote,
			Memory pszItemName,
			int dwFlags);
	@UndocumentedAPI
	short CompoundTextAssimilateBuffer(long hBuffer, int bufferLength, int flags);
	short CompoundTextClose(
			long hCompound,
			LongByReference phReturnBuffer,
			IntByReference pdwReturnBufferSize,
			Memory pchReturnFile,
			short wReturnFileNameSize);
	short CompoundTextCreate(
			long hNote,
			Memory pszItemName,
			LongByReference phCompound);
	short CompoundTextDefineStyle(
			long hCompound,
			Memory pszStyleName,
			NotesCompoundStyleStruct pDefinition,
			IntByReference pdwStyleID);
	void CompoundTextDiscard(
			long hCompound);

	short DesignRefresh(
			Memory Server,
			long hDB,
			int dwFlags,
			ABORTCHECKPROC AbortCheck,
			OSSIGMSGPROC MessageProc);

	@UndocumentedAPI
	short NSFSearchStartExtended(long hDB, long formula, long filter,
			int filterflags, NotesUniversalNoteIdStruct ViewUNID, Memory ViewTitle, 
			long queue, int flags, int flags1, int flags2, int flags3, int flags4, 
			short noteClass, short auxclass, short granularity, 
			NotesTimeDateStruct.ByValue since, NotesTimeDateStruct rtnuntil, 
			LongByReference rtnhandle);

	@UndocumentedAPI
	short NSFSearchStop(long shandle);
	
	short CalCreateEntry(
			long hDB,
			Memory pszCalEntry,
			int dwFlags,
			LongByReference hRetUID,
			Pointer pCtx);

	short CalUpdateEntry(
			long hDB,
			Memory pszCalEntry,
			Memory pszUID,
			Memory pszRecurID,
			Memory pszComments,
			int dwFlags,
			Pointer pCtx);
	
	short CalGetUIDfromNOTEID(
			long hDB,
			int noteid,
			Memory pszUID,
			short wLen,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	short CalGetUIDfromUNID(
			long hDB,
			NotesUniversalNoteIdStruct unid,
			Memory pszUID,
			short wLen,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);

	short CalOpenNoteHandle(
			long hDB,
			Memory pszUID,
			Memory pszRecurID,
			LongByReference rethNote,
			int dwFlags,
			Pointer pCtx);

	short CalReadEntry(
			long hDB,
			Memory pszUID,
			Memory pszRecurID,
			LongByReference hRetCalData,
			IntByReference pdwReserved,
			int dwFlags,
			Pointer pCtx);

	short CalReadRange(
			long hDB,
			NotesTimeDateStruct.ByValue tdStart,
			NotesTimeDateStruct.ByValue  tdEnd,
			int dwViewSkipCount,
			int dwMaxReturnCount,
			int dwReturnMask,
			int dwReturnMaskExt,
			Pointer pFilterInfo,
			LongByReference hRetCalData,
			ShortByReference retCalBufferLength,
			LongByReference hRetUIDData,
			IntByReference retNumEntriesProcessed,
			ShortByReference retSignalFlags,
			int dwFlags,
			Pointer pCtx);
	
	short CalGetUnappliedNotices(
			long hDB,
			Memory pszUID,
			ShortByReference pwNumNotices,
			LongByReference phRetNOTEIDs,
			LongByReference phRetUNIDs,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	short CalGetNewInvitations(
			long hDB,
			NotesTimeDateStruct ptdStart,
			Memory pszUID,
			NotesTimeDateStruct ptdSince,
			NotesTimeDateStruct ptdretUntil,
			ShortByReference pwNumInvites,
			LongByReference phRetNOTEIDs,
			LongByReference phRetUNIDs,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	short CalReadNotice(
			long hDB,
			int noteID,
			LongByReference hRetCalData,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	short CalReadNoticeUNID(
			long hDB,
			NotesUniversalNoteIdStruct unid,
			LongByReference hRetCalData,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	short CalNoticeAction(
			long hDB,
			int noteID,
			int dwAction,
			Memory pszComments,
			NotesCalendarActionDataStruct pExtActionInfo,
			int dwFlags,
			Pointer pCtx);

	short CalNoticeActionUNID(
			long hDB,
			NotesUniversalNoteIdStruct unid,
			int dwAction,
			Memory pszComments,
			NotesCalendarActionDataStruct pExtActionInfo,
			int dwFlags,
			Pointer pCtx);
	
	short CalEntryAction(
			long hDB,
			Memory pszUID,
			Memory pszRecurID,
			int dwAction,
			int dwRange,
			Memory pszComments,
			NotesCalendarActionDataStruct pExtActionInfo,
			int dwFlags,
			Pointer pCtx);

	@UndocumentedAPI
	short LZ1Compress(
	        Pointer sin,
	        Pointer sout,
	        int insize,
	        long hCompHT,
	        IntByReference poutsize
	        );

	@UndocumentedAPI
	short LZ1Decompress(Pointer sin, Pointer SoutUncompressed, int outsize);

	short OOOStartOperation(
			Pointer pMailOwnerName,
			Pointer pHomeMailServer,
			int bHomeMailServer,
			long hMailFile,
			LongByReference hOOOContext,
			PointerByReference pOOOOContext);

	short OOOEndOperation(long hOOContext, Pointer pOOOContext);

	short NSFDbItemDefTableExt(
			long hDB,
			LongByReference retItemNameTable);
	
	@UndocumentedAPI
	short NSFDbGetTcpHostName(
	        long hDB,                                                        /* Database Handle           */ 
	        Memory pszHostName,                                        /* Return TCP Host Name      */ 
	        short wMaxHostNameLen,                                /* Size of Host Name Buffer  */ 
	        Memory pszDomainName,                                        /* Return TCP Domain Name    */ 
	        short wMaxDomainNameLen,                                /* Size of Domain Buffer     */ 
	        Memory pszFullName,                                        /* Return Full TCP Name      */ 
	        short wMaxFullNameLen);                            /* Size of Full Name Buffer  */

	short NSFItemDefExtLock(
			Pointer pItemDefTable,
			NotesItemDefinitionTableLock ItemDefTableLock);

	short NSFItemDefExtEntries(
			NotesItemDefinitionTableLock ItemDefTableLock,
			IntByReference NumEntries);
	
	short NSFItemDefExtGetEntry(
			NotesItemDefinitionTableLock ItemDefTableLock,
			int ItemNum,
			ShortByReference ItemType,
			ShortByReference ItemLength,
			Pointer ItemName);
	
	short NSFItemDefExtUnlock(
			NotesItemDefinitionTableExt ItemDefTable,
			NotesItemDefinitionTableLock ItemDefTableLock);
	
	short NSFItemDefExtFree(
			NotesItemDefinitionTableExt ItemDeftable);
	
	short NSFRemoteConsole(
			Memory ServerName,
			Memory ConsoleCommand,
			LongByReference hResponseText);
	
	short NSFGetServerStats(
			Memory serverName,
			Memory facility,
			Memory statName,
			LongByReference rethTable,
			IntByReference retTableSize);
	
	short DXLImport(
			int hDXLImport,
			NotesCallbacks.XML_READ_FUNCTION pDXLReaderFunc,
			long hDB,
			Pointer pImAction);
	
	short DXLExportACL(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION pDXLWriteFunc,
			long hDB,
			Pointer pExAction);
	
	short DXLExportDatabase(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			long hDB,
			Pointer pExAction);
	
	short DXLExportIDTable(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			long hDB,
			long hIDTable,
			Pointer pExAction);
	
	short DXLExportNote(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			long hNote,
			Pointer pExAction);
	
	short MIMEOpenDirectory(
			long hNote,
			IntByReference phMIMEDir);
	
	short MIMEFreeDirectory(
			long hMIMEDir);
	
	Pointer MIMEEntityContentID(
			Pointer pMIMEEntity);

	Pointer MIMEEntityContentLocation(
			Pointer pMIMEEntity);
	
	int MIMEEntityContentSubtype(
			Pointer pMIMEEntity);

	int MIMEEntityContentType(
			Pointer pMIMEEntity);
	
	Pointer MIMEEntityGetHeader(
			Pointer pMIMEEntity,
			int symKey);

	short MIMEEntityGetTypeParam(
			Pointer pMIMEEntity,
			int symParam,
			LongByReference phValue,
			IntByReference pdwValueLen);
	
	boolean MIMEEntityIsDiscretePart(
			Pointer pMimeEntity);

	boolean MIMEEntityIsMessagePart(
			Pointer pMIMEEntity);
	
	boolean MIMEEntityIsMultiPart(
			Pointer pMimeEntity);

	short MIMEFreeEntityDataObject(
			long hNote,
			Pointer pEntity);

	short MIMEGetDecodedEntityData(
			long hNote,
			Pointer pMimeEntity,
			int dwEncodedOffset,
			int dwChunkLen,
			LongByReference phData,
			IntByReference pdwDecodedDataLen,
			IntByReference pdwEncodedDataLen);

	short MIMEGetEntityData(
			long hNote,
			Pointer pME,
			short wDataType,
			int dwOffset,
			int dwRetBytes,
			LongByReference phData,
			IntByReference pdwDataLen);

	short MIMEGetEntityPartFlags(
			long hNote,
			Pointer  pEntity,
			IntByReference pdwFlags);

	void  MimeGetExtFromTypeInfo(
			Memory pszType,
			Memory pszSubtype,
			Memory pszExtBuf,
			short wExtBufLen,
			Memory pszDescrBuf,
			short wDescrBufLen);

	void MIMEGetFirstSubpart(
			int  hMIMEDir,
			Pointer  pMIMEEntity,
			PointerByReference retpMIMEEntity);
	
	short MIMEGetNextSibling(
			int hMIMEDir,
			Pointer pMIMEEntity,
			PointerByReference retpMIMEEntity);

	short MIMEGetParent(
			int  hMIMEDir,
			Pointer  pMIMEEntity,
			PointerByReference retpMIMEEntity);
	
	short MIMEGetPrevSibling(
			int  hMIMEDir,
			Pointer  pMIMEEntity,
			PointerByReference retpMIMEEntity);

	short MIMEGetRootEntity(
			int  hMIMEDir,
			PointerByReference retpMIMEEntity);	
	
	short MIMEGetText(
			long hNote,
			Memory pchItemName,
			short wItemNameLen,
			boolean bNotesEOL,
			Memory pchBuf,
			int dwMaxBufLen,
			IntByReference pdwBufLen);

	void MimeGetTypeInfoFromExt(
			Memory pszExt,
			Memory pszTypeBuf,
			short wTypeBufLen,
			Memory pszSubtypeBuf,
			short wSubtypeBufLen,
			Memory pszDescrBuf,
			short wDescrBufLen);
	
	short MIMEHeaderNameToItemName(
			short wMessageType,
			Memory pszHeaderName,
			Memory pszHeaderBody,
			Memory retszItemName,
			short wItemNameSize,
			ShortByReference retwHeaderType,
			ShortByReference retwItemFlags,
			ShortByReference retwDataType);

	short MIMEItemNameToHeaderName(
			short wMessageType,
			Memory pszItemName,
			Memory retszHeaderName,
			short wHeaderNameSize,
			ShortByReference retwHeaderType);

	short MIMEIterateNext(
			int  hMIMEDir,
			Pointer pTopMIMEEntity,
			Pointer pPrevMIMEEntity,
			PointerByReference retpMIMEEntity);

	int MIMEStreamGetLine(
			Memory pszLine,
			int uiMaxLineSize,
			Pointer hMIMEStream);

	short MIMEStreamItemize(
			long hNote,
			Memory pchItemName,
			short wItemNameLen,
			int dwFlags,
			Pointer hMIMEStream);
	
	short MIMEStreamOpen(
			long hNote,
			Memory pchItemName,
			short wItemNameLen,
			int dwOpenFlags,
			PointerByReference rethMIMEStream);

	short MIMEConvertCDParts(
			long hNote,
			boolean bCanonical,
			boolean bIsMIME,
			Pointer hCC);
	
	@UndocumentedAPI
	short NSFDbNamedObjectEnum(long hDB, NotesCallbacks.b64_NSFDbNamedObjectEnumPROC callback, Pointer param);

	@UndocumentedAPI
	short NSFDbGetNamedObjectID(long hDB, short NameSpace,
            Memory Name, short NameLength,
            IntByReference rtnObjectID);

	short NSFDbCreateAndCopyExtended(
			Memory srcDb,
			Memory dstDb,
			short NoteClass,
			short limit,
			int flags,
			long hNames,
			LongByReference hNewDb);
	
	short NSFDbCreateACLFromTemplate(
			long hNTF,
			long hNSF,
			Memory Manager,
			short DefaultAccess,
			LongByReference rethACL);
	
	short NSFDbCopy(
			long hSrcDB,
			long hDstDB,
			NotesTimeDateStruct.ByValue Since,
			short NoteClassMask);

	short NSFDbOpenTemplateExtended(
			Memory PathName,
			short Options,
			long hNames,
			NotesTimeDateStruct ModifiedTime,
			LongByReference rethDB,
			NotesTimeDateStruct retDataModified,
			NotesTimeDateStruct retNonDataModified);

	short NSFNoteCreateClone (long hSrcNote, LongByReference rethDstNote);

	short NSFNoteReplaceItems (long hSrcNote, long hDstNote, ShortByReference pwRetItemReplaceCount, boolean fAllowDuplicates);

	short StoredFormAddItems (long hSrcDbHandle, long hSrcNote, long hDstNote, boolean bDoSubforms, int dwFlags);

	short StoredFormRemoveItems(long hNote, int dwFlags);

	short MailNoteJitEx2(Pointer vpRunCtx, long hNote, short wMailFlags, IntByReference retdwRecipients,
			short jitflag, short wMailNoteFlags, NotesCallbacks.b64_FPMailNoteJitEx2CallBack vCallBack, Pointer vCallBackCtx);

	short MailSetSMTPMessageID(long hNote, Memory domain, Memory string, short stringLength);

	/*	This is exactly the same as lookup name BE, except it will use the
	cache of the design collection as known to the client.  This is safe
	to use outside the client since it will map to the BE code.  Using this
	version will only be as up to date as the last time the design was
	fetched for the client.  This was added explicitly to avoid cases of
	backend code executing in their world where backend changes to design
	things was not seen immediately. */
	short DesignLookupNameFE (long hDB, short wClass, Pointer szFlagsPattern, Pointer szName,
									  	short wNameLen, int flags,
										IntByReference retNoteID, IntByReference retbIsPrivate,
										NotesCallbacks.DESIGN_COLL_OPENCLOSE_PROC OpenCloseRoutine, Pointer Ctx);

	short NSFDbGetReplHistorySummary(
			long hDb,
			int Flags,
			LongByReference rethSummary,
			IntByReference retNumEntries);

	short REGCrossCertifyID(
			long hCertCtx,
			short spare1,
			Memory regServer,
			Memory idFileName,
			Memory location,
			Memory comment,
			Memory forwardAddress,
			short spare2,
			NotesCallbacks.REGSIGNALPROC  pStatusFunc,
			Memory errorPathName);
	
	short SECKFMGetCertifierCtx(
			Memory pCertFile,
			KFM_PASSWORDStruct.ByReference pKfmPW,
			Memory pLogFile,
			NotesTimeDateStruct.ByReference pExpDate,
			Memory retCertName,
			LongByReference rethKfmCertCtx,
			ShortByReference retfIsHierarchical,
			ShortByReference retwFileVersion);
	
	void SECKFMFreeCertifierCtx(
			long hKfmCertCtx);

	@UndocumentedAPI
	short SECKFMMakeSafeCopy(long hKFC, short Type, short Version, Memory pFileName);

	@UndocumentedAPI
	short AssistantGetLSDataNote (long hDB, int NoteID, NotesUniversalNoteIdStruct.ByReference retUNID);

	@UndocumentedAPI
	short DesignEnum2 (long hDB,
			   short NoteClass,
			   Memory  pszFlagsPattern,
			   int dwFlags,
			   NotesCallbacks.b64_DESIGNENUMPROC proc,
			   Pointer parameters,
			   NotesCallbacks.DESIGN_COLL_OPENCLOSE_PROC openCloseRoutine,
			   Pointer ctx);

	@UndocumentedAPI
	short DesignGetNoteTable(long hDB, short NoteClass, LongByReference rethIDTable);

	@UndocumentedAPI
	short NSFDbGetModifiedNoteTableExt (HANDLE.ByValue hDB, short NoteClassMask, 
			short Option, NotesTimeDateStruct.ByValue Since,
			NotesTimeDateStruct.ByReference retUntil,
			DHANDLE.ByReference rethTable);

}
