package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.internal.NotesCallbacks.ABORTCHECKPROC;
import com.mindoo.domino.jna.internal.NotesCallbacks.ACLENTRYENUMFUNC;
import com.mindoo.domino.jna.internal.NotesCallbacks.OSSIGMSGPROC;
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

	public short NSFSearch(
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
	public short NSFSearchExtended3 (long hDB, 
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
	public short NSFGetFolderSearchFilter(long hViewDB, long hDataDB, int ViewNoteID, NotesTimeDateStruct Since, int Flags, LongByReference Filter);

	/**
	 * @deprecated use {@link Mem64#OSLockObject(long)} instead
	 */
	@Deprecated
	public Pointer OSLockObject(long handle);
	/**
	 * @deprecated use {@link Mem64#OSUnlockObject(long)} instead
	 */
	@Deprecated
	public boolean OSUnlockObject(long handle);
	/**
	 * @deprecated use {@link Mem64#OSMemFree(long)} instead
	 */
	@Deprecated
	public short OSMemFree(long handle);
	/**
	 * @deprecated use {@link Mem64#OSMemGetSize(long, IntByReference)} instead
	 */
	@Deprecated
	public short OSMemGetSize(long handle, IntByReference retSize);
	/**
	 * @deprecated use {@link Mem64#OSMemoryGetSize(long)} instead
	 */
	@Deprecated
	public int OSMemoryGetSize(long handle);
	/**
	 * @deprecated use {@link Mem64#OSMemoryAllocate(int, int, LongByReference)} instead
	 */
	@Deprecated
	public short OSMemoryAllocate(int dwtype, int size, LongByReference retHandle);
	/**
	 * @deprecated use {@link Mem64#OSMemoryFree(long)} instead
	 */
	@Deprecated
	public void OSMemoryFree(long handle);
	/**
	 * @deprecated use {@link Mem64#OSMemoryReallocate(long, int)} instead
	 */
	@Deprecated
	public short OSMemoryReallocate(long handle, int size);
	/**
	 * @deprecated use {@link Mem64#OSMemoryLock(long)} instead
	 */
	@Deprecated
	public Pointer OSMemoryLock(long handle);
	/**
	 * @deprecated use {@link Mem64#OSMemoryUnlock(long)} instead
	 */
	@Deprecated
	public boolean OSMemoryUnlock(long handle);
	
	/**
	 * @deprecated use {@link Mem64#OSMemAlloc(short, int, LongByReference)} instead
	 */
	@Deprecated
	public short OSMemAlloc(
			short  BlkType,
			int  dwSize,
			LongByReference retHandle);

	@Deprecated
	public short OSMemGetType(long handle);
	
	public short NSFItemGetText(
			long  note_handle,
			Memory item_name,
			Memory item_text,
			short text_len);

	public short ListAllocate(
			short ListEntries,
			short TextSize,
			int fPrefixDataType,
			LongByReference rethList,
			Memory retpList,
			ShortByReference retListSize);
	
	public short ListAddEntry(
			long hList,
			int fPrefixDataType,
			ShortByReference pListSize,
			short EntryNumber,
			Memory Text,
			short TextSize);
	
	public short ListRemoveAllEntries(
			long hList,
			int fPrefixDataType,
			ShortByReference pListSize);

	public short NSFItemInfo(
			long note_handle,
			Memory item_name,
			short  name_len,
			NotesBlockIdStruct retbhItem,
			ShortByReference retDataType,
			NotesBlockIdStruct retbhValue,
			IntByReference retValueLength);

	public short NSFItemInfoNext(
			long  note_handle,
			NotesBlockIdStruct.ByValue NextItem,
			Memory item_name,
			short  name_len,
			NotesBlockIdStruct retbhItem,
			ShortByReference retDataType,
			NotesBlockIdStruct retbhValue,
			IntByReference retValueLength);

	public short NSFItemInfoPrev(
			long  note_handle,
			NotesBlockIdStruct.ByValue  CurrItem,
			Memory item_name,
			short  name_len,
			NotesBlockIdStruct item_blockid_ptr,
			ShortByReference value_type_ptr,
			NotesBlockIdStruct value_blockid_ptr,
			IntByReference value_len_ptr);
	
	public void NSFItemQueryEx(
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
	
	public short NSFItemGetModifiedTimeByBLOCKID(
			long  hNote,
			NotesBlockIdStruct.ByValue bhItem,
			int  Flags,
			NotesTimeDateStruct retTime);

	public short NSFItemGetTextListEntries(
			long note_handle,
			Memory item_name);

	public short NSFItemGetTextListEntry(
			long note_handle,
			Memory item_name,
			short entry_position,
			Memory retEntry_text,
			short  text_len);
	public short NSFItemGetModifiedTime(
			long hNote,
			Memory ItemName,
			short  ItemNameLength,
			int  Flags,
			NotesTimeDateStruct retTime);
	public short NSFItemSetText(
			long hNote,
			Memory ItemName,
			Memory ItemText,
			short TextLength);
	public short NSFItemSetTextSummary(
			long  hNote,
			Memory ItemName,
			Memory ItemText,
			short TextLength,
			boolean summary);
	public boolean NSFItemGetTime(
			long  note_handle,
			Memory td_item_name,
			NotesTimeDateStruct td_item_value);
	public short NSFItemSetTime(
			long  note_handle,
			Memory td_item_name,
			NotesTimeDateStruct td_item_ptr);
	public boolean NSFItemGetNumber(
			long  hNote,
			Memory ItemName,
			DoubleByReference retNumber);
	public int NSFItemGetLong(
			long note_handle,
			Memory number_item_name,
			int number_item_default);
	public short NSFItemSetNumber(
			long hNote,
			Memory ItemName,
			Memory Number);
	public short NSFItemConvertToText(
			long note_handle,
			Memory item_name_ptr,
			Memory retText_buf_ptr,
			short  text_buf_len,
			char separator);
	public short NSFItemConvertValueToText(
			short value_type,
			NotesBlockIdStruct.ByValue value_bid,
			int  value_len,
			Memory text_buf_ptr,
			short  text_buf_len,
			char separator);
	public short NSFItemDelete(
			long note_handle,
			Memory item_name,
			short name_len);
	public short NSFItemDeleteByBLOCKID(long note_handle, NotesBlockIdStruct.ByValue item_blockid);
	public short NSFItemAppend(
			long note_handle,
			short  item_flags,
			Memory item_name,
			short name_len,
			short  item_type,
			Pointer item_value,
			int value_len);
	public short NSFItemAppendByBLOCKID(
			long note_handle,
			short item_flags,
			Memory item_name,
			short name_len,
			NotesBlockIdStruct.ByValue value_bid,
			int value_len,
			NotesBlockIdStruct item_bid_ptr);

	@UndocumentedAPI
	public short NSFItemModifyValue (long hNote, NotesBlockIdStruct.ByValue bhItem, short ItemFlags, short DataType,
			Pointer valuePtr, int valueLength);

	public void NSFNoteGetInfo(long hNote, short type, Memory retValue);
	public void NSFNoteSetInfo(long hNote, short type, Pointer value);
	public short NSFNoteCopy(
			long note_handle_src,
			LongByReference note_handle_dst_ptr);
	public short NSFNoteUpdateExtended(long hNote, int updateFlags);
	public short NSFNoteCreate(long db_handle, LongByReference note_handle);
	public short NSFNoteOpen(long hDB, int noteId, short openFlags, LongByReference rethNote);
	public short NSFNoteOpenExt(long hDB, int noteId, int flags, LongByReference rethNote);
	public short NSFNoteOpenByUNID(
			long  hDB,
			NotesUniversalNoteIdStruct pUNID,
			short  flags,
			LongByReference rethNote);
	@UndocumentedAPI
	public short NSFNoteOpenByUNIDExtended(long hDB, NotesUniversalNoteIdStruct pUNID, int flags, LongByReference rtn); 
	public short NSFNoteClose(long hNote);
	public short NSFNoteVerifySignature(
			long  hNote,
			Memory SignatureItemName,
			NotesTimeDateStruct retWhenSigned,
			Memory retSigner,
			Memory retCertifier);
	public short NSFNoteContract(long hNote);
	public short NSFNoteExpand(long hNote);
	public short NSFNoteSign(long hNote);
	public short NSFNoteSignExt3(long hNote, 
			long	hKFC,
			Memory SignatureItemName,
			short ItemCount, long hItemIDs, 
			int Flags, int Reserved,
			Pointer pReserved);
	public short NSFNoteOpenSoftDelete(long hDB, int NoteID, int Reserved, LongByReference rethNote);
	public short NSFNoteHardDelete(long hDB, int NoteID, int Reserved);
	public short NSFNoteDeleteExtended(long hDB, int NoteID, int UpdateFlags);
	public short NSFNoteDetachFile(long note_handle, NotesBlockIdStruct.ByValue item_blockid);
	public boolean NSFNoteIsSignedOrSealed(long note_handle, ByteByReference signed_flag_ptr, ByteByReference sealed_flag_ptr);
	public short NSFNoteUnsign(long hNote);
	public short NSFNoteComputeWithForm(
			long  hNote,
			long  hFormNote,
			int  dwFlags,
			NotesCallbacks.b64_CWFErrorProc ErrorRoutine,
			Pointer CallersContext);
	public short NSFNoteHasComposite(long hNote);
	public short NSFNoteHasMIME(long hNote);
	public short NSFNoteHasMIMEPart(long hNote);
	@UndocumentedAPI
	public short NSFIsFileItemMimePart(long hNote, NotesBlockIdStruct.ByValue bhFileItem);
	@UndocumentedAPI
	public short NSFIsMimePartInFile(long hNote, NotesBlockIdStruct.ByValue bhMIMEItem, Memory pszFileName, short wMaxFileNameLen);

	public short NSFMimePartCreateStream(
			long hNote,
			Memory pchItemName,
			short wItemNameLen,
			short wPartType,
			int dwFlags,
			LongByReference phCtx);
	
	public short NSFMimePartAppendStream(
			long hCtx,
			Memory pchData,
			short wDataLen);
	
	public short NSFMimePartAppendFileToStream(
			long hCtx,
			Memory pszFilename);
	public short NSFMimePartAppendObjectToStream(
			long hCtx,
			Memory pszAttachmentName);
	public short NSFMimePartCloseStream(
			long hCtx,
			short bUpdate);
	public short MIMEStreamOpen(
			long hNote,
			Memory pchItemName,
			short wItemNameLen,
			int dwOpenFlags,
			LongByReference rethMIMEStream);
	public int MIMEStreamPutLine(
			Memory pszLine,
			long hMIMEStream);
	public short MIMEStreamItemize(
			long hNote,
			Memory pchItemName,
			short wItemNameLen,
			int dwFlags,
			long hMIMEStream);
	public int MIMEStreamWrite(
			Pointer pchData,
			int uiDataLen,
			long hMIMEStream);
	public void MIMEStreamClose(
			long hMIMEStream);
	public short MIMEConvertRFC822TextItemByBLOCKID(
			long hNote,
			NotesBlockIdStruct.ByValue bhItem,
			NotesBlockIdStruct.ByValue bhValue);

	@UndocumentedAPI
	public short NSFNoteHasReadersField(long hNote, NotesBlockIdStruct bhFirstReadersItem);
	public short NSFNoteCipherExtractWithCallback (long hNote, NotesBlockIdStruct.ByValue bhItem,
			int ExtractFlags, int hDecryptionCipher,
			NotesCallbacks.NoteExtractCallback pNoteExtractCallback, Pointer pParam,
			int Reserved, Pointer pReserved);
	public short NSFNoteCopyAndEncryptExt2(
			long  hSrcNote,
			long hKFC,
			short EncryptFlags,
			LongByReference rethDstNote,
			int Reserved,
			Pointer pReserved);
	public short NSFNoteCopyAndEncrypt(
			long hSrcNote,
			short EncryptFlags,
			LongByReference rethDstNote);
	public short NSFNoteCipherDecrypt(
			long  hNote,
			long  hKFC,
			int  DecryptFlags,
			LongByReference rethCipherForAttachments,
			int  Reserved,
			Pointer pReserved);
	public short NSFNoteAttachFile(
			long note_handle,
			Memory item_name,
			short item_name_length,
			Memory file_name,
			Memory orig_path_name,
			short encoding_type);
	public short NSFNoteSignHotspots(
			long hNote,
			int dwFlags,
			IntByReference retfSigned);
	public short NSFNoteLSCompile(
			long hDb,
			long hNote,
			int dwFlags);
	public short NSFNoteLSCompileExt(
			long hDb,
			long hNote,
			int dwFlags,
			NotesCallbacks.LSCompilerErrorProc pfnErrProc,
			Pointer pCtx);
	public short NSFNoteCheck(
			long hNote
			);

	public short NSFDbNoteLock(
			long hDB,
			int NoteID,
			int Flags,
			Memory pLockers,
			LongByReference rethLockers,
			IntByReference retLength);
	
	public short NSFDbNoteUnlock(
			long hDB,
			int NoteID,
			int Flags);

	public short NSFNoteOpenWithLock(
			long hDB,
			int NoteID,
			int LockFlags,
			int OpenFlags,
			Memory pLockers,
			LongByReference rethLockers,
			IntByReference retLength,
			LongByReference rethNote);

	public short NSFItemCopy(long note_handle, NotesBlockIdStruct.ByValue item_blockid);
	@UndocumentedAPI
	public short NSFItemCopyAndRename (long hNote, NotesBlockIdStruct.ByValue bhItem, Memory pszNewItemName);

	public short IDCreateTable (int alignment, LongByReference rethTable);
	public short IDDestroyTable(long hTable);
	public short IDInsert (long hTable, int id, IntByReference retfInserted);
	public short IDDelete (long hTable, int id, IntByReference retfDeleted);
	public boolean IDScan (long hTable, boolean fFirst, IntByReference retID);
	@UndocumentedAPI
	public boolean IDScanBack (long hTable, boolean fLast, IntByReference retID);
	public int IDEntries (long hTable);
	public boolean IDIsPresent (long hTable, int id);
	public int IDTableSize (long hTable);
	public int IDTableSizeP(Pointer pIDTable);
	public short IDTableCopy (long hTable, LongByReference rethTable);
	public short IDTableIntersect(long hSrc1Table, long hSrc2Table, LongByReference rethDstTable);
	public short IDDeleteAll (long hTable);
	public boolean IDAreTablesEqual	(long hSrc1Table, long hSrc2Table);
	public short IDDeleteTable  (long hTable, long hIDsToDelete);
	public short IDInsertTable  (long hTable, long hIDsToAdd);
	public short IDEnumerate(long hTable, NotesCallbacks.IdEnumerateProc Routine, Pointer Parameter);
	@UndocumentedAPI
	public short IDInsertRange(long hTable, int IDFrom, int IDTo, boolean AddToEnd);
	@UndocumentedAPI
	public short IDTableDifferences(long idtable1, long idtable2, LongByReference outputidtableAdds, LongByReference outputidtableDeletes, LongByReference outputidtableSame);
	@UndocumentedAPI
	public short IDTableReplaceExtended(long idtableSrc, long idtableDest, byte flags);

	public short NSFDbStampNotesMultiItem(long hDB, long hTable, long hInNote);
	public short NSFDbOpen(Memory dbName, LongByReference dbHandle);
	public short NSFDbClose(long dbHandle);
	public short NSFDbOpenExtended (Memory PathName, short Options, long hNames, NotesTimeDateStruct ModifiedTime, LongByReference rethDB, NotesTimeDateStruct retDataModified, NotesTimeDateStruct retNonDataModified);
	public short NSFDbGenerateOID(long hDB, NotesOriginatorIdStruct retOID);
	public int NSFDbGetOpenDatabaseID(long hDBU);
	public short NSFDbReopen(long hDB, LongByReference rethDB);
	public short NSFDbLocateByReplicaID(
			long hDB,
			NotesTimeDateStruct ReplicaID,
			Memory retPathName,
			short PathMaxLen);
	public short NSFDbModifiedTime(
			long hDB,
			NotesTimeDateStruct retDataModified,
			NotesTimeDateStruct retNonDataModified);
	public short NSFDbIDGet(long hDB, NotesTimeDateStruct retDbID);
	public short NSFDbReplicaInfoGet(
			long  hDB,
			NotesDbReplicaInfoStruct retReplicationInfo);
	public short NSFDbReplicaInfoSet(
			long  hDB,
			NotesDbReplicaInfoStruct ReplicationInfo);
	public short NSFDbGetModifiedNoteTable(long hDB, short NoteClassMask, NotesTimeDateStruct.ByValue Since, NotesTimeDateStruct retUntil, LongByReference rethTable);
	public short NSFDbGetNotes(
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
	public short NSFDbGetMultNoteInfo(
			long  hDb,
			short  Count,
			short  Options,
			long  hInBuf,
			IntByReference retSize,
			LongByReference rethOutBuf);
	public short NSFDbGetNoteInfoExt(
			long  hDB,
			int  NoteID,
			NotesOriginatorIdStruct retNoteOID,
			NotesTimeDateStruct retModified,
			ShortByReference retNoteClass,
			NotesTimeDateStruct retAddedToFile,
			ShortByReference retResponseCount,
			IntByReference retParentNoteID);
	public short NSFDbGetMultNoteInfoByUNID(
			long hDB,
			short Count,
			short Options,
			long hInBuf,
			IntByReference retSize,
			LongByReference rethOutBuf);
	public short NSFDbSign(long hDb, short noteclass);
	@UndocumentedAPI
	public short NSFDbGetOptionsExt(long hDB, Memory retDbOptions);
	@UndocumentedAPI
	public short NSFDbSetOptionsExt(long hDB, Memory dbOptions, Memory mask);
	public void NSFDbAccessGet(long hDB, ShortByReference retAccessLevel, ShortByReference retAccessFlag);
	public short NSFDbGetBuildVersion(long hDB, ShortByReference retVersion);
	public short NSFDbGetMajMinVersion(long hDb, NotesBuildVersionStruct retBuildVersion);
	public short NSFDbReadObject(
			long hDB,
			int ObjectID,
			int Offset,
			int Length,
			LongByReference rethBuffer);

	public short NSFDbAllocObject(
			long hDB,
			int dwSize,
			short Class,
			short Privileges,
			IntByReference retObjectID);
	
	public short NSFDbAllocObjectExtended2(long cDB,
			int size, short noteClass, short privs, short type, IntByReference rtnRRV);

	public short NSFDbWriteObject(
			long hDB,
			int ObjectID,
			long hBuffer,
			int Offset,
			int Length);
	
	public short NSFDbFreeObject(
			long hDB,
			int ObjectID);
	
	public short NSFDbReallocObject(
			long hDB,
			int ObjectID,
			int NewSize);

	public short NSFItemAppendObject(
			long hNote,
			short ItemFlags,
			Memory Name,
			short NameLength,
			NotesBlockIdStruct.ByValue bhValue,
			int ValueLength,
			int fDealloc);
	
	public short NSFDbGetObjectSize(
			long hDB,
			int ObjectID,
			short ObjectType,
			IntByReference retSize,
			ShortByReference retClass,
			ShortByReference retPrivileges);
	public short NSFDbGetSpecialNoteID(
			long hDB,
			short Index,
			IntByReference retNoteID);
	public short NSFDbClearReplHistory(long hDb, int dwFlags);
	public void NSFDbPathGet(
			long hDB,
			Memory retCanonicalPathName,
			Memory retExpandedPathName);
	@UndocumentedAPI
	public short NSFDbIsRemote(long hDb);
	@UndocumentedAPI
	public short NSFDbHasFullAccess(long hDb);
	public short NSFDbSpaceUsage(long dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes);
	public short NSFDbSpaceUsageScaled (long dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes, IntByReference retGranularity);
	@UndocumentedAPI
	public short NSFHideDesign(long hdb1, long hdb2, int param3, int param4);
	public short NSFDbDeleteNotes(long hDB, long hTable, Memory retUNIDArray);
	public short NSFDbIsLocallyEncrypted(long hDB, IntByReference retVal);
	public short NSFDbInfoGet(
			long hDB,
			Pointer retBuffer);
	public short NSFDbInfoSet(
			long hDB,
			Pointer Buffer);
	public short NSFDbModeGet(
			long  hDB,
			ShortByReference retMode);
	@UndocumentedAPI
	public short NSFDbLock(long hDb);
	@UndocumentedAPI
	public void NSFDbUnlock(long hDb, ShortByReference statusInOut);

	public short NSFBuildNamesList(Memory UserName, int dwFlags, LongByReference rethNamesList);
	@UndocumentedAPI
	public short CreateNamesListFromGroupNameExtend(Memory pszServerName, Memory pTarget, LongByReference rethNames);
	@UndocumentedAPI
	public short CreateNamesListFromNames(short cTargets, Pointer ptrArrTargets, LongByReference rethNames);
	@UndocumentedAPI
	public short CreateNamesListFromNamesExtend(Memory pszServerName, short cTargets, Pointer ptrArrTargets, LongByReference rethNames);
//	@UndocumentedAPI
//	public short CreateNamesListFromSessionID(Memory pszServerName, SESSIONID SessionId, LongByReference rtnhNames);
	@UndocumentedAPI
	public short CreateNamesListFromSingleName(Memory pszServerName, short fDontLookupAlternateNames,
			Pointer pLookupFlags, Memory pTarget, LongByReference rethNames);
	@UndocumentedAPI
	public short CreateNamesListUsingLookupName(Memory pszServerName,Pointer pLookupFlags, Memory pTarget,
			LongByReference rethNames);

	public short NIFReadEntries(long hCollection, NotesCollectionPositionStruct IndexPos, short SkipNavigator, int SkipCount, short ReturnNavigator, int ReturnCount, int ReturnMask, LongByReference rethBuffer,
			ShortByReference retBufferLength, IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned, ShortByReference retSignalFlags);
	@UndocumentedAPI
	public short NIFReadEntriesExt(long hCollection,
			NotesCollectionPositionStruct CollectionPos,
            short SkipNavigator, int SkipCount,
            short ReturnNavigator, int ReturnCount, int ReturnMask,
            NotesTimeDateStruct DiffTime, long DiffIDTable, int ColumnNumber, int Flags,
            LongByReference rethBuffer, ShortByReference retBufferLength,
            IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned,
            ShortByReference retSignalFlags, NotesTimeDateStruct retDiffTime,
            NotesTimeDateStruct retModifiedTime, IntByReference retSequence);
	public void NIFGetLastModifiedTime(long hCollection, NotesTimeDateStruct retLastModifiedTime);
	public void NIFGetLastAccessedTime(long hCollection, NotesTimeDateStruct retLastModifiedTime);
	public void NIFGetNextDiscardTime(long hCollection, NotesTimeDateStruct retLastModifiedTime);
	public short NIFFindByKeyExtended2 (long hCollection, Memory keyBuffer,
			int findFlags,
			int returnFlags,
			NotesCollectionPositionStruct retIndexPos,
			IntByReference retNumMatches,
			ShortByReference retSignalFlags,
			LongByReference rethBuffer,
			IntByReference retSequence);
	@UndocumentedAPI
	public short NIFFindByKeyExtended3 (long hCollection,
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
	
	public short NIFFindByKey(long hCollection, Memory keyBuffer, short findFlags, NotesCollectionPositionStruct retIndexPos, IntByReference retNumMatches);
	public short NIFFindByName(long hCollection, Memory name, short findFlags, NotesCollectionPositionStruct retIndexPos, IntByReference retNumMatches);
	public short NIFGetCollation(long hCollection, ShortByReference retCollationNum);
	public short NIFSetCollation(long hCollection, short CollationNum);
	public short NIFUpdateCollection(long hCollection);
	@UndocumentedAPI
	public short NIFIsNoteInView(long hCollection, int noteID, IntByReference retIsInView);
	@UndocumentedAPI
	public boolean NIFIsUpdateInProgress(long hCollection);
	@UndocumentedAPI
	public short NIFGetIDTableExtended(long hCollection, short navigator, short Flags, long hIDTable);
	@UndocumentedAPI
	public boolean NIFCollectionUpToDate(long hCollection);
	@UndocumentedAPI
	public boolean NIFSetCollectionInfo (long hCollection, Pointer SessionID,
            long hUnreadList, long hCollapsedList, long hSelectedList);
	@UndocumentedAPI
    public short NIFUpdateFilters (long hCollection, short ModifyFlags);
	@UndocumentedAPI
    public boolean NIFIsTimeVariantView(long hCollection);
	public short NIFCloseCollection(long hCollection);
	public short NIFLocateNote (long hCollection, NotesCollectionPositionStruct indexPos, int noteID);
	@UndocumentedAPI
	public short NIFFindDesignNoteExt(long hFile, Memory name, short noteClass, Memory pszFlagsPattern, IntByReference retNoteID, int Options);
	public short NIFOpenCollection(long hViewDB, long hDataDB, int ViewNoteID, short OpenFlags, long hUnreadList, LongByReference rethCollection, LongByReference rethViewNote, Memory retViewUNID,
			LongByReference rethCollapsedList, LongByReference rethSelectedList);
	public short NIFOpenCollectionWithUserNameList (long hViewDB, long hDataDB,
			int ViewNoteID, short OpenFlags,
			long hUnreadList,
			LongByReference rethCollection,
			LongByReference rethViewNote, Memory retViewUNID,
			LongByReference rethCollapsedList,
			LongByReference rethSelectedList,
			long nameList);
	public short NIFGetCollectionData(
			long hCollection,
			LongByReference rethCollData);
	public short NIFGetCollectionDocCountLW(long hCol, IntByReference pDocct);

	@UndocumentedAPI
	public short NSFTransactionBegin(long hDB, int flags);
	@UndocumentedAPI
	public short NSFTransactionCommit(long hDB, int flags);
	@UndocumentedAPI
	public short NSFTransactionRollback(long hDB);

	//backup APIs
	public short NSFDbGetLogInfo(long hDb, int Flags, ShortByReference LOGGED, NotesUniversalNoteIdStruct LogID,
			NotesUniversalNoteIdStruct DbIID, IntByReference LogExtent);
	public short NSFBackupStart(long hDB, int Flags, IntByReference BackupContext, IntByReference FileSizeLow,
			IntByReference FileSizeHigh);
	public short NSFBackupStop(long hDB, int BackupContext);
	public short NSFBackupEnd(long hDB, int BackupContext, int Options);
	public short NSFBackupGetChangeInfoSize( long hDB, int hBackupContext, int Flags, IntByReference InfoSizeLow,
			IntByReference InfoSizeHigh);
	public short NSFBackupStartApplyChangeInfo(IntByReference ApplyInfoContext, Memory CopyFilePath, int Flags,
			int InfoSizeLow, int InfoSizeHigh);
	public short NSFBackupGetNextChangeInfo(long hDB, int hBackupContext, int Flags, Memory Buffer, int BufferSize,
			IntByReference FilledSize);
	public short NSFBackupApplyNextChangeInfo(long ApplyInfoContext, int Flags, Memory Buffer, int BufferSize);
	public short NSFBackupEndApplyChangeInfo(int ApplyInfoContext, int Flags);

	public short AgentDelete (long hAgent); /* delete agent */
	@UndocumentedAPI
	public boolean IsRunAsWebUser(long hAgent);
	public short AgentOpen (long hDB, int AgentNoteID, LongByReference rethAgent);
	public void AgentClose (long hAgent);
	public short AgentCreateRunContext (long hAgent,
			 Pointer pReserved,
			 int dwFlags,
			 LongByReference rethContext);
	@UndocumentedAPI
	public short AgentCreateRunContextExt (long hAgent, Pointer pReserved, long pOldContext, int dwFlags, LongByReference rethContext);
	public short AgentSetDocumentContext(long hAgentCtx, long hNote);
	public short AgentSetTimeExecutionLimit(long hAgentCtx, int timeLimit);
	public boolean AgentIsEnabled(long hAgent);
	@UndocumentedAPI
	public void SetParamNoteID(long hAgentCtx, int noteId);
	@UndocumentedAPI
	public short AgentSetUserName(long hAgentCtx, long hNameList);
	public short AgentRedirectStdout(long hAgentCtx, short redirType);
	public void AgentQueryStdoutBuffer(long hAgentCtx, LongByReference retHdl, IntByReference retSize);
	public void AgentDestroyRunContext (long hAgentCtx);
	public short AgentRun (long hAgent,
			long hAgentCtx,
		    int hSelection,
			int dwFlags);
	@UndocumentedAPI
	public short AgentSetHttpStatusCode(long hAgentCtx, int httpStatus);
	@UndocumentedAPI
	public short ClientRunServerAgent(long hdb, int nidAgent, int nidParamDoc,
			int bForeignServer, int bSuppressPrintToConsole);

	public short FTIndex(long hDB, short options, Memory stopFile, NotesFTIndexStatsStruct retStats);
	@UndocumentedAPI
	public short ClientFTIndexRequest(long hDB);
	public short FTDeleteIndex(long hDB);
	public short FTGetLastIndexTime(long hDB, NotesTimeDateStruct retTime);
	public short FTOpenSearch(LongByReference rethSearch);
	public short FTCloseSearch(long hSearch);
	public short FTSearch(
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
	public short FTSearchExt(
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
	
	public short NSFFormulaCompile(
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
	public short NSFFormulaDecompile(
			Pointer pFormulaBuffer,
			boolean fSelectionFormula,
			LongByReference rethFormulaText,
			ShortByReference retFormulaTextLength);
	public short NSFFormulaSummaryItem(long hFormula, Memory ItemName, short ItemNameLength);
	public short NSFFormulaMerge(
			long hSrcFormula,
			long hDestFormula);
	public short NSFComputeStart(
			short Flags,
			Pointer lpCompiledFormula,
			LongByReference rethCompute);
	public short NSFComputeStop(long hCompute);
	public short NSFComputeEvaluate(
			long  hCompute,
			long hNote,
			LongByReference rethResult,
			ShortByReference retResultLength,
			IntByReference retNoteMatchesFormula,
			IntByReference retNoteShouldBeDeleted,
			IntByReference retNoteModified);

	@UndocumentedAPI
	public short CESCreateCTXFromNote(int hNote, LongByReference rethCESCTX);
	@UndocumentedAPI
	public short CESGetNoSigCTX(LongByReference rethCESCTX);
	@UndocumentedAPI
	public short CESFreeCTX(long hCESCTX);
	@UndocumentedAPI
	public short ECLUserTrustSigner ( long hCESCtx, 
			short ECLType,
			short bSessionOnly,
			short wCapabilities,
			short wCapabilities2,
			ShortByReference retwCurrentCapabilities,
			ShortByReference retwCurrentCapabilities2);

	public short NSFFolderGetIDTable(
			long  hViewDB,
			long hDataDB,
			int  viewNoteID,
			int  flags,
			LongByReference hTable);
	
	public short NSFGetAllFolderChanges(
			long hViewDB,
			long hDataDB,
			NotesTimeDateStruct since,
			int flags,
			NotesCallbacks.b64_NSFGetAllFolderChangesCallback Callback,
			Pointer Param,
			NotesTimeDateStruct until);

	public short NSFGetFolderChanges(
			long hViewDB,
			long hDataDB,
			int viewNoteID,
			NotesTimeDateStruct since,
			int Flags,
			LongByReference addedNoteTable,
			LongByReference removedNoteTable);
	
	public short FolderDocAdd(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  hTable,
			long  dwFlags);
	public short FolderDocCount(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  dwFlags,
			LongByReference pdwNumDocs);
	public short FolderDocRemove(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  hTable,
			long  dwFlags);
	public short FolderDocRemoveAll(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  dwFlags);
	public short FolderMove(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			long  hParentDB,
			int  ParentNoteID,
			long  dwFlags);
	public short FolderRename(
			long  hDataDB,
			long  hFolderDB,
			int  FolderNoteID,
			Memory pszName,
			short  wNameLen,
			long  dwFlags);

	public short NSFProfileOpen(
			long hDB,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength,
			short CopyProfile,
			LongByReference rethProfileNote);
	public short NSFProfileUpdate(
			long hProfile,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength);
	public short NSFProfileSetField(
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
	public short NSFProfileDelete(
			long hDB,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength);
	
	public short SECKFMOpen(LongByReference phKFC, Memory pIDFileName, Memory pPassword,
			int Flags, int Reserved, Pointer pReserved);

	public short SECKFMClose(LongByReference phKFC, int Flags, int Reserved, Pointer pReserved);
	public short SECidfGet(Memory pUserName, Memory pPassword, Memory pPutIDFileHere,
			LongByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved);
	public short SECidfPut(Memory pUserName, Memory pPassword, Memory pIDFilePath,
			LongByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved);
	public short SECidfSync( Memory pUserName, Memory pPassword, Memory pIDFilePath,
			LongByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved, IntByReference retdwFlags);
	public short SECTokenGenerate(
			Memory ServerName,
			Memory OrgName,
			Memory ConfigName,
			Memory UserName,
			NotesTimeDateStruct Creation,
			NotesTimeDateStruct Expiration,
			LongByReference retmhToken,
			int dwReserved,
			Pointer vpReserved);
	public void SECTokenFree(LongByReference mhToken);

	public short SchFreeTimeSearch(
			NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate,
			short fFindFirstFit,
			int dwReserved,
			NotesTimeDatePairStruct pInterval,
			short Duration,
			Pointer pNames,
			LongByReference rethRange);

	public short SchRetrieve(
			NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate,
			int dwOptions,
			NotesTimeDatePairStruct pInterval,
			Pointer pNames,
			LongByReference rethCntnr,
			Pointer mustBeNull1,
			Pointer mustBeNull2,
			Pointer mustBeNull3);

	public short SchSrvRetrieveExt(
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
	
	public void SchContainer_Free(long hCntnr);
	public short SchContainer_GetFirstSchedule(
			long hCntnr,
			IntByReference rethObj,
			Memory retpSchedule);
	public short Schedule_Free(long hCntnr, int hSched);
	public short SchContainer_GetNextSchedule(
			long hCntnr,
			int hCurSchedule,
			IntByReference rethNextSchedule,
			Memory retpNextSchedule);
	public short Schedule_ExtractFreeTimeRange(
			long hCntnr,
			int hSchedObj,
			NotesUniversalNoteIdStruct punidIgnore,
			short fFindFirstFit,
			short wDuration,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethRange);
	public short Schedule_ExtractBusyTimeRange(
			long hCntnr,
			int hSchedObj,
			NotesUniversalNoteIdStruct punidIgnore,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethRange,
			IntByReference rethMoreCtx);
	public short Schedule_ExtractMoreBusyTimeRange(
			long hCntnr,
			int hMoreCtx,
			NotesUniversalNoteIdStruct punidIgnore,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethRange,
			IntByReference rethMore);
	public short Schedule_ExtractSchedList(
			long hCntnr,
			int hSchedObj,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethSchedList,
			IntByReference rethMore);
	public short Schedule_ExtractMoreSchedList(
			long hCntnr,
			int hMoreCtx,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			LongByReference rethSchedList,
			IntByReference rethMore);
	public short Schedule_Access(
			long hCntnr,
			int hSched,
			PointerByReference pretSched);
	public short Schedule_GetFirstDetails(
			long hCntnr,
			int hSchedObj,
			IntByReference rethDetailObj,
			PointerByReference retpDetail);
	
	public short Schedule_GetNextDetails(
			long hCntnr,
			int hDetailObj,
			IntByReference rethNextDetailObj,
			PointerByReference retpNextDetail);
	public short NSGetServerClusterMates(
			Memory pServerName,
			int dwFlags,
			LongByReference phList);
	public short NSPingServer(
			Memory pServerName,
			IntByReference pdwIndex,
			LongByReference phList);

	public short HTMLCreateConverter(LongByReference phHTML);
	public short HTMLDestroyConverter(long hHTML);
	public short HTMLSetHTMLOptions(long hHTML, StringArray optionList);
	public short HTMLConvertItem(
			long hHTML,
			long hDB,
			long hNote,
			Memory pszItemName);
	public short HTMLConvertNote(
			long hHTML,
			long hDB,
			long hNote,
			int NumArgs,
			HtmlApi_UrlComponentStruct pArgs);
	public short HTMLGetProperty(
			long hHTML,
			long PropertyType,
			Pointer pProperty);
	public short HTMLSetProperty(
			int hHTML,
			long PropertyType,
			Memory pProperty);
	public short HTMLGetText(
			long hHTML,
			int startingOffset,
			IntByReference pTextLength,
			Memory pText);
	public short HTMLGetReference(
			long hHTML,
			int Index,
			LongByReference phRef);
	public short HTMLLockAndFixupReference(
			long hRef,
			Memory ppRef);
	public short HTMLConvertElement(
			long hHTML,
			long hDB,
			long hNote,
			Memory pszItemName,
			int ItemIndex,
			int Offset);

	public short CompoundTextAddCDRecords(
			long hCompound,
			Pointer pvRecord,
			int dwRecordLength);

	public short CompoundTextAddDocLink(
			long hCompound,
			NotesTimeDateStruct.ByValue DBReplicaID,
			NotesUniversalNoteIdStruct.ByValue ViewUNID,
			NotesUniversalNoteIdStruct.ByValue NoteUNID,
			Memory pszComment,
			int dwFlags);
	public short CompoundTextAddParagraphExt(
			long hCompound,
			int dwStyleID,
			int FontID,
			Memory pchText,
			int dwTextLen,
			Pointer pInfo);
	public short CompoundTextAddRenderedNote(
			long hCompound,
			long hNote,
			long hFormNote,
			int dwFlags);
	public short CompoundTextAddTextExt(
			long hCompound,
			int dwStyleID,
			int FontID,
			Memory pchText,
			int dwTextLen,
			Memory pszLineDelim,
			int dwFlags,
			Pointer pInfo);
	public short CompoundTextAssimilateFile(
			long hCompound,
			Memory pszFileName,
			int dwFlags);
	public short CompoundTextAssimilateItem(
			long hCompound,
			long hNote,
			Memory pszItemName,
			int dwFlags);
	@UndocumentedAPI
	public short CompoundTextAssimilateBuffer(long hBuffer, int bufferLength, int flags);
	public short CompoundTextClose(
			long hCompound,
			LongByReference phReturnBuffer,
			IntByReference pdwReturnBufferSize,
			Memory pchReturnFile,
			short wReturnFileNameSize);
	public short CompoundTextCreate(
			long hNote,
			Memory pszItemName,
			LongByReference phCompound);
	public short CompoundTextDefineStyle(
			long hCompound,
			Memory pszStyleName,
			NotesCompoundStyleStruct pDefinition,
			IntByReference pdwStyleID);
	public void CompoundTextDiscard(
			long hCompound);

	public short DesignRefresh(
			Memory Server,
			long hDB,
			int dwFlags,
			ABORTCHECKPROC AbortCheck,
			OSSIGMSGPROC MessageProc);

	public short NSFDbReadACL(
			long hDB,
			LongByReference rethACL);
	
	public short ACLEnumEntries(
			long hACL,
			ACLENTRYENUMFUNC EnumFunc,
			Pointer EnumFuncParam);

	public short ACLGetPrivName(
			long hACL,
			short PrivNum,
			Memory retPrivName);

	public short NSFDbStoreACL(
			long hDB,
			long hACL,
			int ObjectID,
			short Method);
	
	public short ACLLookupAccess(
			long hACL,
			Pointer pNamesList,
			ShortByReference retAccessLevel,
			Memory retPrivileges,
			ShortByReference retAccessFlags,
			LongByReference rethPrivNames);
	
	public short ACLSetAdminServer(
			long hList,
			Memory ServerName);
	
	public short ACLAddEntry(
			long hACL,
			Memory name,
			short AccessLevel,
			Memory privileges,
			short AccessFlags);

	public short ACLDeleteEntry(
			long hACL,
			Memory name);
	
	public short ACLSetFlags(
			long hACL,
			int Flags);
	
	public short ACLGetFlags(
			long hACL,
			IntByReference retFlags);

	public short ACLSetPrivName(
			long hACL,
			short PrivNum,
			Memory privName);

	public short ACLUpdateEntry(
			long hACL,
			Memory name,
			short updateFlags,
			Memory newName,
			short newAccessLevel,
			Memory newPrivileges,
			short newAccessFlags);

	@UndocumentedAPI
	public short NSFSearchStartExtended(long hDB, long formula, long filter,
			int filterflags, NotesUniversalNoteIdStruct ViewUNID, Memory ViewTitle, 
			long queue, int flags, int flags1, int flags2, int flags3, int flags4, 
			short noteClass, short auxclass, short granularity, 
			NotesTimeDateStruct.ByValue since, NotesTimeDateStruct rtnuntil, 
			LongByReference rtnhandle);

	@UndocumentedAPI
	public short QueueCreate(LongByReference qhandle);

	@UndocumentedAPI
	public short QueueGet(long qhandle, LongByReference sehandle);
	
	@UndocumentedAPI
	public short NSFSearchStop(long shandle);

	@UndocumentedAPI
	public short QueueDelete(long qhandle);
	
	public short CalCreateEntry(
			long hDB,
			Memory pszCalEntry,
			int dwFlags,
			LongByReference hRetUID,
			Pointer pCtx);

	public short CalUpdateEntry(
			long hDB,
			Memory pszCalEntry,
			Memory pszUID,
			Memory pszRecurID,
			Memory pszComments,
			int dwFlags,
			Pointer pCtx);
	
	public short CalGetUIDfromNOTEID(
			long hDB,
			int noteid,
			Memory pszUID,
			short wLen,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	public short CalGetUIDfromUNID(
			long hDB,
			NotesUniversalNoteIdStruct unid,
			Memory pszUID,
			short wLen,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);

	public short CalOpenNoteHandle(
			long hDB,
			Memory pszUID,
			Memory pszRecurID,
			LongByReference rethNote,
			int dwFlags,
			Pointer pCtx);

	public short CalReadEntry(
			long hDB,
			Memory pszUID,
			Memory pszRecurID,
			LongByReference hRetCalData,
			IntByReference pdwReserved,
			int dwFlags,
			Pointer pCtx);

	public short CalReadRange(
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
	
	public short CalGetUnappliedNotices(
			long hDB,
			Memory pszUID,
			ShortByReference pwNumNotices,
			LongByReference phRetNOTEIDs,
			LongByReference phRetUNIDs,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	public short CalGetNewInvitations(
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
	
	public short CalReadNotice(
			long hDB,
			int noteID,
			LongByReference hRetCalData,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	public short CalReadNoticeUNID(
			long hDB,
			NotesUniversalNoteIdStruct unid,
			LongByReference hRetCalData,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	public short CalNoticeAction(
			long hDB,
			int noteID,
			int dwAction,
			Memory pszComments,
			NotesCalendarActionDataStruct pExtActionInfo,
			int dwFlags,
			Pointer pCtx);

	public short CalNoticeActionUNID(
			long hDB,
			NotesUniversalNoteIdStruct unid,
			int dwAction,
			Memory pszComments,
			NotesCalendarActionDataStruct pExtActionInfo,
			int dwFlags,
			Pointer pCtx);
	
	public short CalEntryAction(
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
	public short LZ1Compress(
	        Pointer sin,
	        Pointer sout,
	        int insize,
	        long hCompHT,
	        IntByReference poutsize
	        );

	@UndocumentedAPI
	public short LZ1Decompress(Pointer sin, Pointer SoutUncompressed, int outsize);

	public short OOOStartOperation(
			Pointer pMailOwnerName,
			Pointer pHomeMailServer,
			int bHomeMailServer,
			long hMailFile,
			LongByReference hOOOContext,
			PointerByReference pOOOOContext);

	public short OOOEndOperation(long hOOContext, Pointer pOOOContext);

	public short NSFDbItemDefTableExt(
			long hDB,
			LongByReference retItemNameTable);
	
	@UndocumentedAPI
	public short NSFDbGetTcpHostName(
	        long hDB,                                                        /* Database Handle           */ 
	        Memory pszHostName,                                        /* Return TCP Host Name      */ 
	        short wMaxHostNameLen,                                /* Size of Host Name Buffer  */ 
	        Memory pszDomainName,                                        /* Return TCP Domain Name    */ 
	        short wMaxDomainNameLen,                                /* Size of Domain Buffer     */ 
	        Memory pszFullName,                                        /* Return Full TCP Name      */ 
	        short wMaxFullNameLen);                            /* Size of Full Name Buffer  */

	public short NSFItemDefExtLock(
			Pointer pItemDefTable,
			NotesItemDefinitionTableLock ItemDefTableLock);

	public short NSFItemDefExtEntries(
			NotesItemDefinitionTableLock ItemDefTableLock,
			IntByReference NumEntries);
	
	public short NSFItemDefExtGetEntry(
			NotesItemDefinitionTableLock ItemDefTableLock,
			int ItemNum,
			ShortByReference ItemType,
			ShortByReference ItemLength,
			Pointer ItemName);
	
	public short NSFItemDefExtUnlock(
			NotesItemDefinitionTableExt ItemDefTable,
			NotesItemDefinitionTableLock ItemDefTableLock);
	
	public short NSFItemDefExtFree(
			NotesItemDefinitionTableExt ItemDeftable);
	
	public short NSFRemoteConsole(
			Memory ServerName,
			Memory ConsoleCommand,
			LongByReference hResponseText);
	
	public short NSFGetServerStats(
			Memory serverName,
			Memory facility,
			Memory statName,
			LongByReference rethTable,
			IntByReference retTableSize);
	
	public short DXLImport(
			int hDXLImport,
			NotesCallbacks.XML_READ_FUNCTION pDXLReaderFunc,
			long hDB,
			Pointer pImAction);
	
	public short DXLExportACL(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION pDXLWriteFunc,
			long hDB,
			Pointer pExAction);
	
	public short DXLExportDatabase(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			long hDB,
			Pointer pExAction);
	
	public short DXLExportIDTable(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			long hDB,
			long hIDTable,
			Pointer pExAction);
	
	public short DXLExportNote(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			long hNote,
			Pointer pExAction);
}
