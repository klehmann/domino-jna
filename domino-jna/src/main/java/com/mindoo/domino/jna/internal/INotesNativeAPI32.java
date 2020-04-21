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

public interface INotesNativeAPI32 extends Library {

	public short NSFSearch(
			int hDB,
			int hFormula,
			Memory viewTitle,
			short SearchFlags,
			short NoteClassMask,
			NotesTimeDateStruct Since,
			NotesCallbacks.NsfSearchProc enumRoutine,
			Pointer EnumRoutineParameter,
			NotesTimeDateStruct retUntil);

	@UndocumentedAPI
	public short NSFSearchExtended3 (int hDB, 
			int hFormula, 
			int hFilter, 
			int FilterFlags, 
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
			int namelist);

	@UndocumentedAPI
	public short NSFGetFolderSearchFilter(int hViewDB, int hDataDB, int ViewNoteID, NotesTimeDateStruct Since, int Flags, IntByReference Filter);

	/**
	 * @deprecated use {@link Mem32#OSLockObject(int)} instead
	 */
	@Deprecated
	public Pointer OSLockObject(int handle);
	/**
	 * @deprecated use {@link Mem32#OSUnlockObject(int)} instead
	 */
	@Deprecated
	public boolean OSUnlockObject(int handle);
	/**
	 * @deprecated use {@link Mem32#OSMemFree(int)} instead
	 */
	@Deprecated
	public short OSMemFree(int handle);
	/**
	 * @deprecated use {@link Mem32#OSMemGetSize(int, IntByReference)} instead
	 */
	@Deprecated
	public short OSMemGetSize(int handle, IntByReference retSize);
	/**
	 * @deprecated use {@link Mem32#OSMemGetSize(int, IntByReference)} instead
	 */
	@Deprecated
	public int OSMemoryGetSize(int handle);
	/**
	 * @deprecated use {@link Mem32#OSMemoryFree(int)} instead
	 */
	@Deprecated
	public void OSMemoryFree(int handle);
	/**
	 * @deprecated use {@link Mem32#OSMemoryAllocate(int, int, IntByReference)} instead
	 */
	@Deprecated
	public short OSMemoryAllocate(int dwtype, int size, IntByReference retHandle);
	/**
	 * @deprecated use {@link Mem32#OSMemoryReallocate(int, int)} instead
	 */
	@Deprecated
	public short OSMemoryReallocate(int handle, int size);
	/**
	 * @deprecated use {@link Mem32#OSMemoryLock(int)} instead
	 */
	@Deprecated
	public Pointer OSMemoryLock(int handle);
	/**
	 * @deprecated use {@link Mem32#OSMemoryUnlock(int)} instead
	 */
	@Deprecated
	public boolean OSMemoryUnlock(int handle);
	/**
	 * @deprecated use {@link Mem32#OSMemAlloc(short, int, IntByReference)} instead
	 */
	@Deprecated
	public short OSMemAlloc(
			short  BlkType,
			int  dwSize,
			IntByReference retHandle);
	
	@Deprecated
	public short OSMemGetType(int handle);

	public short NSFItemGetText(
			int  note_handle,
			Memory item_name,
			Memory item_text,
			short text_len);

	public short ListAllocate(
			short ListEntries,
			short TextSize,
			int fPrefixDataType,
			IntByReference rethList,
			Memory retpList,
			ShortByReference retListSize);
	
	public short ListAddEntry(
			int hList,
			int fPrefixDataType,
			ShortByReference pListSize,
			short EntryNumber,
			Memory Text,
			short TextSize);

	public short ListRemoveAllEntries(
			int hList,
			int fPrefixDataType,
			ShortByReference pListSize);

	public short NSFItemInfo(
			int  note_handle,
			Memory item_name,
			short name_len,
			NotesBlockIdStruct retbhItem,
			ShortByReference retDataType,
			NotesBlockIdStruct retbhValue,
			IntByReference retValueLength);

	public short NSFItemInfoNext(
			int  note_handle,
			NotesBlockIdStruct.ByValue NextItem,
			Memory item_name,
			short name_len,
			NotesBlockIdStruct retbhItem,
			ShortByReference retDataType,
			NotesBlockIdStruct retbhValue,
			IntByReference retValueLength);
	
	public short NSFItemInfoPrev(
			int  note_handle,
			NotesBlockIdStruct.ByValue  CurrItem,
			Memory item_name,
			short  name_len,
			NotesBlockIdStruct item_blockid_ptr,
			ShortByReference value_type_ptr,
			NotesBlockIdStruct value_blockid_ptr,
			IntByReference value_len_ptr);
	
	public void NSFItemQueryEx(
			int  note_handle,
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
			int  hNote,
			NotesBlockIdStruct.ByValue bhItem,
			int  Flags,
			NotesTimeDateStruct retTime);

	public short NSFItemGetTextListEntries(
			int note_handle,
			Memory item_name);

	public short NSFItemGetTextListEntry(
			int note_handle,
			Memory item_name,
			short entry_position,
			Memory retEntry_text,
			short  text_len);
	public short NSFItemGetModifiedTime(
			int hNote,
			Memory ItemName,
			short  ItemNameLength,
			int  Flags,
			NotesTimeDateStruct retTime);
	public short NSFItemSetText(
			int hNote,
			Memory ItemName,
			Memory ItemText,
			short TextLength);
	public short NSFItemSetTextSummary(
			int hNote,
			Memory ItemName,
			Memory ItemText,
			short TextLength,
			boolean summary);
	public boolean NSFItemGetTime(
			int  note_handle,
			Memory td_item_name,
			NotesTimeDateStruct td_item_value);
	public short NSFItemSetTime(
			int  note_handle,
			Memory td_item_name,
			NotesTimeDateStruct td_item_ptr);
	public boolean NSFItemGetNumber(
			int hNote,
			Memory ItemName,
			DoubleByReference retNumber);
	public int NSFItemGetLong(
			int note_handle,
			Memory number_item_name,
			int number_item_default);
	public short NSFItemSetNumber(
			int  hNote,
			Memory ItemName,
			Memory Number);
	public short NSFItemConvertToText(
			int note_handle,
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
			int note_handle,
			Memory item_name,
			short name_len);
	public short NSFItemDeleteByBLOCKID(int note_handle, NotesBlockIdStruct.ByValue item_blockid);
	public short NSFItemAppend(
			int note_handle,
			short item_flags,
			Memory item_name,
			short name_len,
			short  item_type,
			Pointer item_value,
			int value_len);
	public short NSFItemAppendByBLOCKID(
			int note_handle,
			short item_flags,
			Memory item_name,
			short name_len,
			NotesBlockIdStruct.ByValue value_bid,
			int value_len,
			NotesBlockIdStruct item_bid_ptr);

	@UndocumentedAPI
	public short NSFItemModifyValue (int hNote, NotesBlockIdStruct.ByValue bhItem, short ItemFlags, short DataType,
			Pointer valuePtr, int valueLength);

	public void NSFNoteGetInfo(int hNote, short type, Memory retValue);
	public void NSFNoteSetInfo(int hNote, short type, Pointer value);
	public short NSFNoteCopy(
			int note_handle_src,
			IntByReference note_handle_dst_ptr);

	public short NSFNoteUpdateExtended(int hNote, int updateFlags);
	public short NSFNoteCreate(int db_handle, IntByReference note_handle);
	public short NSFNoteOpen(int hDB, int noteId, short openFlags, IntByReference rethNote);
	public short NSFNoteOpenExt(int hDB, int noteId, int flags, IntByReference rethNote);
	public short NSFNoteOpenByUNID(
			int hDB,
			NotesUniversalNoteIdStruct pUNID,
			short  flags,
			IntByReference rethNote);
	@UndocumentedAPI
	public short NSFNoteOpenByUNIDExtended(int hDB, NotesUniversalNoteIdStruct pUNID, int flags, IntByReference rtn);
	public short NSFNoteClose(int hNote);
	public short NSFNoteVerifySignature(
			int  hNote,
			Memory SignatureItemName,
			NotesTimeDateStruct retWhenSigned,
			Memory retSigner,
			Memory retCertifier);
	public short NSFNoteContract(int hNote);
	public short NSFNoteExpand(int hNote);
	public short NSFNoteSign(int hNote);
	public short NSFNoteSignExt3(int hNote, 
			int hKFC,
			Memory SignatureItemName,
			short ItemCount, int hItemIDs, 
			int Flags, int Reserved,
			Pointer pReserved);
	public short NSFNoteOpenSoftDelete(int hDB, int NoteID, int Reserved, IntByReference rethNote);
	public short NSFNoteHardDelete(int hDB, int NoteID, int Reserved);
	public short NSFNoteDeleteExtended(int hDB, int NoteID, int UpdateFlags);
	public short NSFNoteDetachFile(int note_handle, NotesBlockIdStruct.ByValue item_blockid);
	public boolean NSFNoteIsSignedOrSealed(int note_handle, ByteByReference signed_flag_ptr, ByteByReference sealed_flag_ptr);
	public short NSFNoteUnsign(int hNote);
	public short NSFNoteComputeWithForm(
			int  hNote,
			int  hFormNote,
			int  dwFlags,
			NotesCallbacks.b32_CWFErrorProc ErrorRoutine,
			Pointer CallersContext);
	public short NSFNoteHasComposite(int hNote);
	public short NSFNoteHasMIME(int hNote);
	public short NSFNoteHasMIMEPart(int hNote);
	@UndocumentedAPI
	public short NSFIsFileItemMimePart(int hNote, NotesBlockIdStruct.ByValue bhFileItem);
	@UndocumentedAPI
	public short NSFIsMimePartInFile(int hNote, NotesBlockIdStruct.ByValue bhMIMEItem, Memory pszFileName, short wMaxFileNameLen);
	
	public short NSFMimePartCreateStream(
			int hNote,
			Memory pchItemName,
			short wItemNameLen,
			short wPartType,
			int dwFlags,
			IntByReference phCtx);
	
	public short NSFMimePartAppendStream(
			int hCtx,
			Memory pchData,
			short wDataLen);
	
	public short NSFMimePartAppendFileToStream(
			int hCtx,
			Memory pszFilename);
	public short NSFMimePartAppendObjectToStream(
			int hCtx,
			Memory pszAttachmentName);
	public short NSFMimePartCloseStream(
			int hCtx,
			short  bUpdate);
//	public short MIMEStreamOpen(
//			int hNote,
//			Memory pchItemName,
//			short wItemNameLen,
//			int dwOpenFlags,
//			IntByReference rethMIMEStream);
//	public int MIMEStreamPutLine(
//			Memory pszLine,
//			int hMIMEStream);
//	public short MIMEStreamItemize(
//			int hNote,
//			Memory pchItemName,
//			short wItemNameLen,
//			int dwFlags,
//			int hMIMEStream);
//	public int MIMEStreamWrite(
//			Pointer pchData,
//			int uiDataLen,
//			int hMIMEStream);
//	public void MIMEStreamClose(
//			Pointer hMIMEStream);
	public short MIMEConvertRFC822TextItemByBLOCKID(
			int hNote,
			NotesBlockIdStruct.ByValue bhItem,
			NotesBlockIdStruct.ByValue bhValue);
	
	@UndocumentedAPI
	public short NSFNoteHasReadersField(int hNote, NotesBlockIdStruct bhFirstReadersItem);
	public short NSFNoteCipherExtractWithCallback (int hNote, NotesBlockIdStruct.ByValue bhItem,
			int ExtractFlags, int hDecryptionCipher,
			NotesCallbacks.NoteExtractCallback pNoteExtractCallback, Pointer pParam,
			int Reserved, Pointer pReserved);
	public short NSFNoteCopyAndEncryptExt2(
			int hSrcNote,
			int hKFC,
			short EncryptFlags,
			IntByReference rethDstNote,
			int  Reserved,
			Pointer pReserved);
	public short NSFNoteCopyAndEncrypt(
			int hSrcNote,
			short EncryptFlags,
			IntByReference rethDstNote);
	public short NSFNoteCipherDecrypt(
			int  hNote,
			int hKFC,
			int  DecryptFlags,
			LongByReference rethCipherForAttachments,
			int  Reserved,
			Pointer pReserved);
	public short NSFNoteAttachFile(
			int note_handle,
			Memory item_name,
			short item_name_length,
			Memory file_name,
			Memory orig_path_name,
			short encoding_type);
	public short NSFNoteSignHotspots(
			int hNote,
			int dwFlags,
			IntByReference retfSigned);
	public short NSFNoteLSCompile(
			int hDb,
			int hNote,
			int dwFlags);
	public short NSFNoteLSCompileExt(
			int hDb,
			int hNote,
			int dwFlags,
			NotesCallbacks.LSCompilerErrorProc pfnErrProc,
			Pointer pCtx);
	public short NSFNoteCheck(
			int hNote
			);

	public short NSFDbNoteLock(
			int hDB,
			int NoteID,
			int Flags,
			Memory pLockers,
			IntByReference rethLockers,
			IntByReference retLength);

	public short NSFDbNoteUnlock(
			int hDB,
			int NoteID,
			int Flags);
	
	public short NSFNoteOpenWithLock(
			int hDB,
			int NoteID,
			int LockFlags,
			int OpenFlags,
			Memory pLockers,
			IntByReference rethLockers,
			IntByReference retLength,
			IntByReference rethNote);
	
	public short NSFItemCopy(int note_handle, NotesBlockIdStruct.ByValue item_blockid);
	@UndocumentedAPI
	public short NSFItemCopyAndRename (int hNote, NotesBlockIdStruct.ByValue bhItem, Memory pszNewItemName);
	
	public short IDCreateTable (int alignment, IntByReference rethTable);
	public short IDDestroyTable(int hTable);
	public short IDInsert (int hTable, int id, IntByReference retfInserted);
	public short IDDelete (int hTable, int id, IntByReference retfDeleted);
	public boolean IDScan (int hTable, boolean fFirst, IntByReference retID);
	@UndocumentedAPI
	public boolean IDScanBack (int hTable, boolean fLast, IntByReference retID);
	public int IDEntries (int hTable);
	public boolean IDIsPresent (int hTable, int id);
	public int IDTableSize (int hTable);
	public int IDTableSizeP(Pointer pIDTable);
	public short IDTableCopy (int hTable, IntByReference rethTable);
	public short IDTableIntersect(int hSrc1Table, int hSrc2Table, IntByReference rethDstTable);
	public short IDDeleteAll (int hTable);
	public boolean IDAreTablesEqual	(int hSrc1Table, int hSrc2Table);
	public short IDDeleteTable(int hTable, int hIDsToDelete);
	public short IDInsertTable  (int hTable, int hIDsToAdd);
	public short IDEnumerate(int hTable, NotesCallbacks.IdEnumerateProc Routine, Pointer Parameter);
	@UndocumentedAPI
	public short IDInsertRange(int hTable, int IDFrom, int IDTo, boolean AddToEnd);
	@UndocumentedAPI
	public short IDTableDifferences(int idtable1, int idtable2, IntByReference outputidtableAdds, IntByReference outputidtableDeletes, IntByReference outputidtableSame);
	@UndocumentedAPI
	public short IDTableReplaceExtended(int idtableSrc, int idtableDest, byte flags);

	public short NSFDbStampNotesMultiItem(int hDB, int hTable, int hInNote);
	public short NSFDbOpen(Memory dbName, IntByReference dbHandle);
	public short NSFDbOpenExtended (Memory PathName, short Options, int hNames, NotesTimeDateStruct ModifiedTime, IntByReference rethDB, NotesTimeDateStruct retDataModified, NotesTimeDateStruct retNonDataModified);
	public short NSFDbGenerateOID(int hDB, NotesOriginatorIdStruct retOID);
	public short NSFDbClose(int dbHandle);
	public int NSFDbGetOpenDatabaseID(int hDBU);
	public short NSFDbReopen(int hDB, IntByReference rethDB);
	public short NSFDbLocateByReplicaID(
			int  hDB,
			NotesTimeDateStruct ReplicaID,
			Memory retPathName,
			short PathMaxLen);
	public short NSFDbModifiedTime(
			int hDB,
			NotesTimeDateStruct retDataModified,
			NotesTimeDateStruct retNonDataModified);
	public short NSFDbIDGet(int hDB, NotesTimeDateStruct retDbID);
	public short NSFDbReplicaInfoGet(
			int  hDB,
			NotesDbReplicaInfoStruct retReplicationInfo);
	public short NSFDbReplicaInfoSet(
			int  hDB,
			NotesDbReplicaInfoStruct ReplicationInfo);
	public short NSFDbGetModifiedNoteTable(int hDB, short NoteClassMask, NotesTimeDateStruct.ByValue Since, NotesTimeDateStruct retUntil, IntByReference rethTable);
	public short NSFDbGetNotes(
			int hDB,
			int NumNotes,
			Memory NoteID, //NOTEID array
			Memory NoteOpenFlags, // DWORD array
			Memory SinceSeqNum, // DWORD array
			int ControlFlags,
			int hObjectDB,
			Pointer CallbackParam,
			NotesCallbacks.NSFGetNotesCallback  GetNotesCallback,
			NotesCallbacks.b32_NSFNoteOpenCallback  NoteOpenCallback,
			NotesCallbacks.b32_NSFObjectAllocCallback  ObjectAllocCallback,
			NotesCallbacks.b32_NSFObjectWriteCallback  ObjectWriteCallback,
			NotesTimeDateStruct FolderSinceTime,
			NotesCallbacks.NSFFolderAddCallback  FolderAddCallback);
	public short NSFDbGetMultNoteInfo(
			int  hDb,
			short  Count,
			short  Options,
			int  hInBuf,
			IntByReference retSize,
			IntByReference rethOutBuf);
	public short NSFDbGetNoteInfoExt(
			int  hDB,
			int  NoteID,
			NotesOriginatorIdStruct retNoteOID,
			NotesTimeDateStruct retModified,
			ShortByReference retNoteClass,
			NotesTimeDateStruct retAddedToFile,
			ShortByReference retResponseCount,
			IntByReference retParentNoteID);
	public short NSFDbGetMultNoteInfoByUNID(
			int hDB,
			short Count,
			short Options,
			int hInBuf,
			IntByReference retSize,
			IntByReference rethOutBuf);
	public short NSFDbSign(int hDb, short noteclass);
	@UndocumentedAPI
	public short NSFDbGetOptionsExt(int hDB, Memory retDbOptions);
	@UndocumentedAPI
	public short NSFDbSetOptionsExt(int hDB, Memory dbOptions, Memory mask);
	public void NSFDbAccessGet(int hDB, ShortByReference retAccessLevel, ShortByReference retAccessFlag);
	public short NSFDbGetBuildVersion(int hDB, ShortByReference retVersion);
	public short NSFDbGetMajMinVersion(int hDb, NotesBuildVersionStruct retBuildVersion);
	public short NSFDbReadObject(
			int hDB,
			int ObjectID,
			int Offset,
			int Length,
			IntByReference rethBuffer);
	
	public short NSFDbAllocObject(
			int hDB,
			int dwSize,
			short Class,
			short Privileges,
			IntByReference retObjectID);
	
	public short NSFDbAllocObjectExtended2(int cDB,
			int size, short noteClass, short privs, short type, IntByReference rtnRRV);
	
	public short NSFDbWriteObject(
			int hDB,
			int ObjectID,
			int hBuffer,
			int Offset,
			int Length);
	
	public short NSFDbFreeObject(
			int hDB,
			int ObjectID);
	
	public short NSFDbReallocObject(
			int hDB,
			int ObjectID,
			int NewSize);

	public short NSFDbGetObjectSize(
			int hDB,
			int ObjectID,
			short ObjectType,
			IntByReference retSize,
			ShortByReference retClass,
			ShortByReference retPrivileges);

	public short NSFItemAppendObject(
			int hNote,
			short ItemFlags,
			Memory Name,
			short NameLength,
			NotesBlockIdStruct.ByValue bhValue,
			int ValueLength,
			int fDealloc);
	
	public short NSFDbGetSpecialNoteID(
			int hDB,
			short Index,
			IntByReference retNoteID);
	public short NSFDbClearReplHistory(int hDb, int dwFlags);
	public void NSFDbPathGet(
			int hDB,
			Memory retCanonicalPathName,
			Memory retExpandedPathName);
	@UndocumentedAPI
	public short NSFDbIsRemote(int hDb);
	@UndocumentedAPI
	public short NSFDbHasFullAccess(int hDb);
	public short NSFDbSpaceUsage(int dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes);
	public short NSFDbSpaceUsageScaled (int dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes, IntByReference retGranularity);
	public short NSFDbDeleteNotes(int  hDB, int  hTable, Memory retUNIDArray);
	public short NSFDbIsLocallyEncrypted(int hDB, IntByReference retVal);
	public short NSFDbInfoGet(
			int hDB,
			Pointer retBuffer);
	public short NSFDbInfoSet(
			int hDB,
			Pointer Buffer);
	public short NSFDbModeGet(
			int hDB,
			ShortByReference retMode);
	@UndocumentedAPI
	public short NSFDbLock(int hDb);
	@UndocumentedAPI
	public void NSFDbUnlock(int hDb, ShortByReference statusInOut);
	
	@UndocumentedAPI
	public short NSFHideDesign(int hdb1, int hdb2, int param3, int param4);

	public short NSFBuildNamesList(Memory UserName, int dwFlags, IntByReference rethNamesList);
	@UndocumentedAPI
	public short CreateNamesListFromGroupNameExtend(Memory pszServerName, Memory pTarget, IntByReference rethNames);
	@UndocumentedAPI
	public short CreateNamesListFromNames(short cTargets, Pointer ptrArrTargets, IntByReference rethNames);
	@UndocumentedAPI
	public short CreateNamesListFromNamesExtend(Memory pszServerName, short cTargets, Pointer ptrArrTargets, IntByReference rethNames);
//	@UndocumentedAPI
//	public short CreateNamesListFromSessionID(Memory pszServerName, SESSIONID SessionId, IntByReference rtnhNames);
	@UndocumentedAPI
	public short CreateNamesListFromSingleName(Memory pszServerName, short fDontLookupAlternateNames,
			Pointer pLookupFlags, Memory pTarget, IntByReference rethNames);
	@UndocumentedAPI
	public short CreateNamesListUsingLookupName(Memory pszServerName,Pointer pLookupFlags, Memory pTarget,
			IntByReference rethNames);
	
	public short NIFReadEntries(int hCollection, NotesCollectionPositionStruct IndexPos, short SkipNavigator, int SkipCount, short ReturnNavigator, int ReturnCount, int ReturnMask, IntByReference rethBuffer,
			ShortByReference retBufferLength, IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned, ShortByReference retSignalFlags);
	@UndocumentedAPI
	public short NIFReadEntriesExt(int hCollection,
			NotesCollectionPositionStruct CollectionPos,
			short SkipNavigator, int SkipCount,
			short ReturnNavigator, int ReturnCount, int ReturnMask,
			NotesTimeDateStruct DiffTime, int DiffIDTable, int ColumnNumber, int Flags,
			IntByReference rethBuffer, ShortByReference retBufferLength,
			IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned,
			ShortByReference retSignalFlags, NotesTimeDateStruct retDiffTime,
			NotesTimeDateStruct retModifiedTime, IntByReference retSequence);
	public void NIFGetLastModifiedTime(int hCollection, NotesTimeDateStruct retLastModifiedTime);
	public void NIFGetLastAccessedTime(int hCollection, NotesTimeDateStruct retLastModifiedTime);
	public void NIFGetNextDiscardTime(int hCollection, NotesTimeDateStruct retLastModifiedTime);
	public short NIFFindByKeyExtended2 (int hCollection, Memory keyBuffer,
			int findFlags,
			int returnFlags,
			NotesCollectionPositionStruct retIndexPos,
			IntByReference retNumMatches,
			ShortByReference retSignalFlags,
			IntByReference rethBuffer,
			IntByReference retSequence);
	@UndocumentedAPI
	public short NIFFindByKeyExtended3 (int hCollection,
			Memory keyBuffer, int findFlags,
			int returnFlags,
			NotesCollectionPositionStruct retIndexPos,
			IntByReference retNumMatches, ShortByReference retSignalFlags,
			IntByReference rethBuffer, IntByReference retSequence,
			NotesCallbacks.NIFFindByKeyProc NIFFindByKeyCallback, NIFFindByKeyContextStruct Ctx);
	public short NIFFindByKey(int hCollection, Memory keyBuffer, short findFlags, NotesCollectionPositionStruct retIndexPos, IntByReference retNumMatches);
	public short NIFFindByName(int hCollection, Memory name, short findFlags, NotesCollectionPositionStruct retIndexPos, IntByReference retNumMatches);
	public short NIFGetCollation(int hCollection, ShortByReference retCollationNum);
	public short NIFSetCollation(int hCollection, short CollationNum);
	public short NIFUpdateCollection(int hCollection);
	@UndocumentedAPI
	public short NIFIsNoteInView(int hCollection, int noteID, IntByReference retIsInView);
	@UndocumentedAPI
	public boolean NIFIsUpdateInProgress(int hCollection);
	@UndocumentedAPI
	public short NIFGetIDTableExtended(int hCollection, short navigator, short Flags, int hIDTable);
	@UndocumentedAPI
	public boolean NIFCollectionUpToDate(int hCollection);
	@UndocumentedAPI
    public boolean NIFSetCollectionInfo (int hCollection, Pointer SessionID,
            int hUnreadList, int hCollapsedList, int hSelectedList);
	@UndocumentedAPI
    public short NIFUpdateFilters (int hCollection, short ModifyFlags);
	@UndocumentedAPI
    public boolean NIFIsTimeVariantView(int hCollection);
	public short NIFCloseCollection(int hCollection);
	public short NIFLocateNote (int hCollection, NotesCollectionPositionStruct indexPos, int noteID);
	@UndocumentedAPI
	public short NIFFindDesignNoteExt(int hFile, Memory name, short noteClass, Memory pszFlagsPattern, IntByReference retNoteID, int Options);
	public short NIFOpenCollection(int hViewDB, int hDataDB, int ViewNoteID, short OpenFlags, int hUnreadList, IntByReference rethCollection, IntByReference rethViewNote, Memory retViewUNID,
			IntByReference rethCollapsedList, IntByReference rethSelectedList);
	public short NIFOpenCollectionWithUserNameList (int hViewDB, int hDataDB,
			int ViewNoteID, short OpenFlags,
			int hUnreadList,
			IntByReference rethCollection,
			IntByReference rethViewNote, Memory retViewUNID,
			IntByReference rethCollapsedList,
			IntByReference rethSelectedList,
			int nameList);
	public short NIFGetCollectionData(
			int hCollection,
			IntByReference rethCollData);
	public short NIFGetCollectionDocCountLW(int hCol, IntByReference pDocct);

	@UndocumentedAPI
	public short NSFTransactionBegin(int hDB, int flags);
	@UndocumentedAPI
	public short NSFTransactionCommit(int hDB, int flags);
	@UndocumentedAPI
	public short NSFTransactionRollback(int hDB);

	//backup APIs
	public short NSFDbGetLogInfo(int hDb, int Flags, ShortByReference LOGGED, NotesUniversalNoteIdStruct LogID,
			NotesUniversalNoteIdStruct DbIID, IntByReference LogExtent);
	public short NSFBackupStart(int hDB, int Flags, IntByReference BackupContext, IntByReference FileSizeLow,
			IntByReference FileSizeHigh);
	public short NSFBackupStop(int hDB, int BackupContext);
	public short NSFBackupEnd(int hDB, int BackupContext, int Options);
	public short NSFBackupGetChangeInfoSize(int hDB, int hBackupContext, int Flags, IntByReference InfoSizeLow,
			IntByReference InfoSizeHigh);
	public short NSFBackupStartApplyChangeInfo(IntByReference ApplyInfoContext, Memory CopyFilePath, int Flags,
			int InfoSizeLow, int InfoSizeHigh);
	public short NSFBackupGetNextChangeInfo(int hDB, int hBackupContext, int Flags, Memory Buffer, int BufferSize,
			IntByReference FilledSize);
	public short NSFBackupApplyNextChangeInfo(int ApplyInfoContext, int Flags, Memory Buffer, int BufferSize);
	public short NSFBackupEndApplyChangeInfo(int ApplyInfoContext, int Flags);

	public short AgentDelete (int hAgent); /* delete agent */
	@UndocumentedAPI
	public boolean IsRunAsWebUser(int hAgent);
	public short AgentOpen (int hDB, int AgentNoteID, IntByReference rethAgent);
	public void AgentClose (int hAgent);
	public short AgentCreateRunContext (int hAgent,
			 Pointer pReserved,
			 int dwFlags,
			 IntByReference rethContext);
	@UndocumentedAPI
	public short AgentCreateRunContextExt (int hAgent, Pointer pReserved, int pOldContext, int dwFlags, IntByReference rethContext);
	public short AgentSetDocumentContext(int hAgentCtx, int hNote);
	public short AgentSetTimeExecutionLimit(int hAgentCtx, int timeLimit);
	public boolean AgentIsEnabled(int hAgent);
	@UndocumentedAPI
	public void SetParamNoteID(int hAgentCtx, int noteId);
	@UndocumentedAPI
	public short AgentSetUserName(int hAgentCtx, int hNameList);
	public short AgentRedirectStdout(int hAgentCtx, short redirType);
	public void AgentQueryStdoutBuffer(int hAgentCtx, IntByReference retHdl, IntByReference retSize);
	public void AgentDestroyRunContext (int hAgentCtx);
	public short AgentRun (int hAgent,
			int hAgentCtx,
		    int hSelection,
			int dwFlags);
	@UndocumentedAPI
	public short AgentSetHttpStatusCode(int hAgentCtx, int httpStatus);
	@UndocumentedAPI
	public short ClientRunServerAgent(int hdb, int nidAgent, int nidParamDoc,
			int bForeignServer, int bSuppressPrintToConsole);
	
	public short FTIndex(int hDB, short options, Memory stopFile, NotesFTIndexStatsStruct retStats);
	@UndocumentedAPI
	public short ClientFTIndexRequest(int hDB);
	public short FTDeleteIndex(int hDB);
	public short FTGetLastIndexTime(int hDB, NotesTimeDateStruct retTime);
	public short FTOpenSearch(IntByReference rethSearch);
	public short FTCloseSearch(int hSearch);
	public short FTSearch(
			int hDB,
			IntByReference phSearch,
			int hColl,
			Memory query,
			int options,
			short  limit,
			int hIDTable,
			IntByReference retNumDocs,
			Memory reserved,
			IntByReference rethResults);

	@UndocumentedAPI
	public short FTSearchExt(
			int hDB,
			IntByReference phSearch,
			int hColl,
			Memory query,
			int options,
			short limit,
			int hRefineIDTable,
			IntByReference retNumDocs,
			IntByReference rethStrings,
			IntByReference rethResults,
			IntByReference retNumHits,
			int start,
			int count,
			short arg,
			int hNames
			);
	
	public short NSFFormulaCompile(
			Memory FormulaName,
			short FormulaNameLength,
			Memory FormulaText,
			short  FormulaTextLength,
			IntByReference rethFormula,
			ShortByReference retFormulaLength,
			ShortByReference retCompileError,
			ShortByReference retCompileErrorLine,
			ShortByReference retCompileErrorColumn,
			ShortByReference retCompileErrorOffset,
			ShortByReference retCompileErrorLength);
	public short NSFFormulaDecompile(
			Pointer pFormulaBuffer,
			boolean fSelectionFormula,
			IntByReference rethFormulaText,
			ShortByReference retFormulaTextLength);
	public short NSFFormulaSummaryItem(int hFormula, Memory ItemName, short ItemNameLength);
	public short NSFFormulaMerge(
			int hSrcFormula,
			int hDestFormula);

	public short NSFComputeStart(
			short Flags,
			Pointer lpCompiledFormula,
			IntByReference rethCompute);
	public short NSFComputeStop(int hCompute);
	public short NSFComputeEvaluate(
			int  hCompute,
			int hNote,
			IntByReference rethResult,
			ShortByReference retResultLength,
			IntByReference retNoteMatchesFormula,
			IntByReference retNoteShouldBeDeleted,
			IntByReference retNoteModified);

	@UndocumentedAPI
	public short CESCreateCTXFromNote(int hNote, IntByReference rethCESCTX);
	@UndocumentedAPI
	public short CESGetNoSigCTX(IntByReference rethCESCTX);
	@UndocumentedAPI
	public short CESFreeCTX(int hCESCTX);
	@UndocumentedAPI
	public short ECLUserTrustSigner ( int hCESCtx, 
			short ECLType,
			short bSessionOnly,
			short wCapabilities,
			short wCapabilities2,
			ShortByReference retwCurrentCapabilities,
			ShortByReference retwCurrentCapabilities2);

	public short NSFFolderGetIDTable(
			int  hViewDB,
			int hDataDB,
			int  viewNoteID,
			int  flags,
			IntByReference hTable);
	
	public short NSFGetAllFolderChanges(
			int hViewDB,
			int hDataDB,
			NotesTimeDateStruct since,
			int flags,
			NotesCallbacks.b32_NSFGetAllFolderChangesCallback Callback,
			Pointer Param,
			NotesTimeDateStruct until);

	public short NSFGetFolderChanges(
			int hViewDB,
			int hDataDB,
			int viewNoteID,
			NotesTimeDateStruct since,
			int Flags,
			IntByReference addedNoteTable,
			IntByReference removedNoteTable);

	public short FolderDocAdd(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			int  hTable,
			long  dwFlags);
	public short FolderDocCount(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			long  dwFlags,
			LongByReference pdwNumDocs);
	public short FolderDocRemove(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			int  hTable,
			long  dwFlags);
	public short FolderDocRemoveAll(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			long  dwFlags);
	public short FolderMove(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			int  hParentDB,
			int  ParentNoteID,
			long  dwFlags);
	public short FolderRename(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			Memory pszName,
			short  wNameLen,
			long  dwFlags);

	public short NSFProfileOpen(
			int hDB,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength,
			short CopyProfile,
			IntByReference rethProfileNote);
	public short NSFProfileUpdate(
			int hProfile,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength);
	public short NSFProfileSetField(
			int hDB,
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
			int hDB,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength);

	public short SECKFMOpen(IntByReference phKFC, Memory pIDFileName, Memory pPassword,
			int Flags, int Reserved, Pointer pReserved);
	public short SECKFMClose(IntByReference phKFC, int Flags, int Reserved, Pointer pReserved);
	@UndocumentedAPI
	public short SECKFMAccess(short param1, int hKFC, Pointer retUsername, Pointer param4);

	public short SECidfGet(Memory pUserName, Memory pPassword, Memory pPutIDFileHere,
			IntByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved);
	public short SECidfPut(Memory pUserName, Memory pPassword, Memory pIDFilePath,
			IntByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved);
	public short SECidfSync( Memory pUserName, Memory pPassword, Memory pIDFilePath,
			IntByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved, IntByReference retdwFlags);
	public short SECTokenGenerate(
			Memory ServerName,
			Memory OrgName,
			Memory ConfigName,
			Memory UserName,
			NotesTimeDateStruct Creation,
			NotesTimeDateStruct Expiration,
			IntByReference retmhToken,
			int dwReserved,
			Pointer vpReserved);
	public void SECTokenFree(IntByReference mhToken);

	public short SchFreeTimeSearch(
			NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate,
			short fFindFirstFit,
			int dwReserved,
			NotesTimeDatePairStruct pInterval,
			short Duration,
			Pointer pNames,
			IntByReference rethRange);

	public short SchRetrieve(
			NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate,
			int dwOptions,
			NotesTimeDatePairStruct pInterval,
			Pointer pNames,
			IntByReference rethCntnr,
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
			IntByReference rethCntnr);

	public void SchContainer_Free(int hCntnr);
	public short SchContainer_GetFirstSchedule(
			int hCntnr,
			IntByReference rethObj,
			Memory retpSchedule);
	public short Schedule_Free(int hCntnr, int hSched);
	public short SchContainer_GetNextSchedule(
			int hCntnr,
			int hCurSchedule,
			IntByReference rethNextSchedule,
			Memory retpNextSchedule);
	public short Schedule_ExtractFreeTimeRange(
			int hCntnr,
			int hSchedObj,
			NotesUniversalNoteIdStruct punidIgnore,
			short fFindFirstFit,
			short wDuration,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethRange);
	public short Schedule_ExtractBusyTimeRange(
			int hCntnr,
			int hSchedObj,
			NotesUniversalNoteIdStruct punidIgnore,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethRange,
			IntByReference rethMoreCtx);
	public short Schedule_ExtractMoreBusyTimeRange(
			int hCntnr,
			int hMoreCtx,
			NotesUniversalNoteIdStruct punidIgnore,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethRange,
			IntByReference rethMore);
	public short Schedule_ExtractSchedList(
			int hCntnr,
			int hSchedObj,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethSchedList,
			IntByReference rethMore);
	public short Schedule_ExtractMoreSchedList(
			int hCntnr,
			int hMoreCtx,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethSchedList,
			IntByReference rethMore);
	public short Schedule_Access(
			int hCntnr,
			int hSched,
			PointerByReference pretSched);
	
	public short Schedule_GetFirstDetails(
			int hCntnr,
			int hSchedObj,
			IntByReference rethDetailObj,
			PointerByReference retpDetail);
	
	public short Schedule_GetNextDetails(
			int hCntnr,
			int hDetailObj,
			IntByReference rethNextDetailObj,
			PointerByReference retpNextDetail);
	
	public short NSGetServerClusterMates(
			Memory pServerName,
			int dwFlags,
			IntByReference phList);
	public short NSPingServer(
			Memory pServerName,
			IntByReference pdwIndex,
			IntByReference phList);

	public short HTMLCreateConverter(IntByReference phHTML);
	public short HTMLDestroyConverter(int hHTML);
	public short HTMLSetHTMLOptions(int hHTML, StringArray optionList);
	public short HTMLConvertItem(
			int hHTML,
			int hDB,
			int hNote,
			Memory pszItemName);
	public short HTMLConvertNote(
			int hHTML,
			int hDB,
			int hNote,
			int NumArgs,
			HtmlApi_UrlComponentStruct pArgs);
	public short HTMLGetProperty(
			int hHTML,
			int PropertyType,
			Pointer pProperty);
	public short HTMLSetProperty(
			int hHTML,
			int PropertyType,
			Memory pProperty);
	public short HTMLGetText(
			int hHTML,
			int StartingOffset,
			IntByReference pTextLength,
			Memory pText);
	public short HTMLGetReference(
			int hHTML,
			int Index,
			IntByReference phRef);
	public short HTMLLockAndFixupReference(
			int hRef,
			Memory ppRef);
	public short HTMLConvertElement(
			int hHTML,
			int hDB,
			int hNote,
			Memory pszItemName,
			int ItemIndex,
			int Offset);
	
	public short CompoundTextAddCDRecords(
			int hCompound,
			Pointer pvRecord,
			int dwRecordLength);
	public short CompoundTextAddDocLink(
			int hCompound,
			NotesTimeDateStruct.ByValue DBReplicaID,
			NotesUniversalNoteIdStruct.ByValue ViewUNID,
			NotesUniversalNoteIdStruct.ByValue NoteUNID,
			Memory pszComment,
			int dwFlags);
	public short CompoundTextAddParagraphExt(
			int hCompound,
			int dwStyleID,
			int FontID,
			Memory pchText,
			int dwTextLen,
			Pointer pInfo);

	public short CompoundTextAddRenderedNote(
			int hCompound,
			int hNote,
			int hFormNote,
			int dwFlags);
	public short CompoundTextAddTextExt(
			int hCompound,
			int dwStyleID,
			int FontID,
			Memory pchText,
			int dwTextLen,
			Memory pszLineDelim,
			int dwFlags,
			Pointer pInfo);
	public short CompoundTextAssimilateFile(
			int hCompound,
			Memory pszFileName,
			int dwFlags);
	public short CompoundTextAssimilateItem(
			int hCompound,
			int hNote,
			Memory pszItemName,
			int dwFlags);
	@UndocumentedAPI
	public short CompoundTextAssimilateBuffer(int hBuffer, int bufferLength, int flags);
	public short CompoundTextClose(
			int hCompound,
			IntByReference phReturnBuffer,
			IntByReference pdwReturnBufferSize,
			Memory pchReturnFile,
			short wReturnFileNameSize);
	public short CompoundTextCreate(
			int hNote,
			Memory pszItemName,
			IntByReference phCompound);
	public short CompoundTextDefineStyle(
			int hCompound,
			Memory pszStyleName,
			NotesCompoundStyleStruct pDefinition,
			IntByReference pdwStyleID);
	public void CompoundTextDiscard(
			int hCompound);

	public short DesignRefresh(
			Memory Server,
			int hDB,
			int dwFlags,
			ABORTCHECKPROC AbortCheck,
			OSSIGMSGPROC MessageProc);
	
	public short NSFDbReadACL(
			int hDB,
			IntByReference rethACL);
	
	public short ACLEnumEntries(
			int hACL,
			ACLENTRYENUMFUNC EnumFunc,
			Pointer EnumFuncParam);
	
	public short ACLGetPrivName(
			int hACL,
			short PrivNum,
			Memory retPrivName);
	
	public short NSFDbStoreACL(
			int hDB,
			int hACL,
			int ObjectID,
			short Method);
	
	public short ACLLookupAccess(
			int hACL,
			Pointer pNamesList,
			ShortByReference retAccessLevel,
			Memory retPrivileges,
			ShortByReference retAccessFlags,
			IntByReference rethPrivNames);
	
	public short ACLSetAdminServer(
			int hList,
			Memory ServerName);

	public short ACLAddEntry(
			int hACL,
			Memory name,
			short AccessLevel,
			Memory privileges,
			short AccessFlags);
	
	public short ACLDeleteEntry(
			int hACL,
			Memory name);

	public short ACLSetFlags(
			int hACL,
			int Flags);
	
	public short ACLGetFlags(
			int hACL,
			IntByReference retFlags);
	
	public short ACLSetPrivName(
			int hACL,
			short PrivNum,
			Memory privName);

	public short ACLUpdateEntry(
			int hACL,
			Memory name,
			short updateFlags,
			Memory newName,
			short newAccessLevel,
			Memory newPrivileges,
			short newAccessFlags);
	
	@UndocumentedAPI
	public short NSFSearchStartExtended(int hDB, int formula, int filter,
			int filterflags, NotesUniversalNoteIdStruct ViewUNID, Memory ViewTitle, 
			int queue, int flags, int flags1, int flags2, int flags3, int flags4, 
			short noteClass, short auxclass, short granularity, 
			NotesTimeDateStruct.ByValue since, NotesTimeDateStruct rtnuntil, 
			IntByReference rtnhandle);

	@UndocumentedAPI
	public short QueueCreate(IntByReference qhandle);
	
	@UndocumentedAPI
	public short QueueGet(int qhandle, IntByReference sehandle);

	@UndocumentedAPI
	public short NSFSearchStop(int shandle);
	
	@UndocumentedAPI
	public short QueueDelete(int qhandle);
	
	public short CalCreateEntry(
			int hDB,
			Memory pszCalEntry,
			int dwFlags,
			IntByReference hRetUID,
			Pointer pCtx);

	public short CalUpdateEntry(
			int hDB,
			Memory pszCalEntry,
			Memory pszUID,
			Memory pszRecurID,
			Memory pszComments,
			int dwFlags,
			Pointer pCtx);
	
	public short CalGetUIDfromNOTEID(
			int hDB,
			int noteid,
			Memory pszUID,
			short wLen,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	public short CalGetUIDfromUNID(
			int hDB,
			NotesUniversalNoteIdStruct unid,
			Memory pszUID,
			short wLen,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);

	public short CalOpenNoteHandle(
			int hDB,
			Memory pszUID,
			Memory pszRecurID,
			IntByReference rethNote,
			int dwFlags,
			Pointer pCtx);

	public short CalReadEntry(
			int hDB,
			Memory pszUID,
			Memory pszRecurID,
			IntByReference hRetCalData,
			IntByReference pdwReserved,
			int dwFlags,
			Pointer pCtx);
	
	public short CalReadRange(
			int hDB,
			NotesTimeDateStruct.ByValue tdStart,
			NotesTimeDateStruct.ByValue  tdEnd,
			int dwViewSkipCount,
			int dwMaxReturnCount,
			int dwReturnMask,
			int dwReturnMaskExt,
			Pointer pFilterInfo,
			IntByReference hRetCalData,
			ShortByReference retCalBufferLength,
			IntByReference hRetUIDData,
			IntByReference retNumEntriesProcessed,
			ShortByReference retSignalFlags,
			int dwFlags,
			Pointer pCtx);
	
	public short CalGetUnappliedNotices(
			int hDB,
			Memory pszUID,
			ShortByReference pwNumNotices,
			IntByReference phRetNOTEIDs,
			IntByReference phRetUNIDs,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);

	public short CalGetNewInvitations(
			int hDB,
			NotesTimeDateStruct ptdStart,
			Memory pszUID,
			NotesTimeDateStruct ptdSince,
			NotesTimeDateStruct ptdretUntil,
			ShortByReference pwNumInvites,
			IntByReference phRetNOTEIDs,
			IntByReference phRetUNIDs,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	public short CalReadNotice(
			int hDB,
			int noteID,
			IntByReference hRetCalData,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	public short CalReadNoticeUNID(
			int hDB,
			NotesUniversalNoteIdStruct unid,
			IntByReference hRetCalData,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	public short CalNoticeAction(
			int hDB,
			int noteID,
			int dwAction,
			Memory pszComments,
			NotesCalendarActionDataStruct  pExtActionInfo,
			int dwFlags,
			Pointer pCtx);
	
	public short CalNoticeActionUNID(
			int hDB,
			NotesUniversalNoteIdStruct unid,
			int dwAction,
			Memory pszComments,
			NotesCalendarActionDataStruct pExtActionInfo,
			int dwFlags,
			Pointer pCtx);
	
	public short CalEntryAction(
			int hDB,
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
	        int hCompHT,
	        IntByReference poutsize
	        );

	@UndocumentedAPI
	public short LZ1Decompress(Pointer sin, Pointer SoutUncompressed, int outsize);
	
	public short OOOStartOperation(
			Pointer pMailOwnerName,
			Pointer pHomeMailServer,
			int bHomeMailServer,
			int hMailFile,
			IntByReference hOOOContext,
			PointerByReference pOOOOContext);
	public short OOOEndOperation(int hOOContext, Pointer pOOOContext);
	
	public short NSFDbItemDefTableExt(
			int hDB,
			IntByReference retItemNameTable);
	
	@UndocumentedAPI
	public short NSFDbGetTcpHostName(
	        int hDB,                                                        /* Database Handle           */ 
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
			IntByReference hResponseText);
	
	public short NSFGetServerStats(
			Memory serverName,
			Memory facility,
			Memory statName,
			IntByReference rethTable,
			IntByReference retTableSize);

	public short DXLImport(
			int hDXLImport,
			NotesCallbacks.XML_READ_FUNCTION pDXLReaderFunc,
			int hDB,
			Pointer pImAction);
	
	public short DXLExportACL(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION pDXLWriteFunc,
			int hDB,
			Pointer pExAction);
	
	public short DXLExportDatabase(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			int hDB,
			Pointer pExAction);
	
	public short DXLExportIDTable(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			int hDB,
			int hIDTable,
			Pointer pExAction);
	
	public short DXLExportNote(
			int  hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			int hNote,
			Pointer pExAction);
	
	public short MIMEOpenDirectory(
			int hNote,
			IntByReference phMIMEDir);

	public short MIMEFreeDirectory(
			int hMIMEDir);
	
	public Pointer MIMEEntityContentID(
			Pointer pMIMEEntity);
	
	public Pointer MIMEEntityContentLocation(
			Pointer pMIMEEntity);

	public int MIMEEntityContentSubtype(
			Pointer pMIMEEntity);
	
	public int MIMEEntityContentType(
			Pointer pMIMEEntity);

	public Pointer MIMEEntityGetHeader(
			Pointer pMIMEEntity,
			int symKey);

	public short MIMEEntityGetTypeParam(
			Pointer pMIMEEntity,
			int symParam,
			IntByReference phValue,
			IntByReference pdwValueLen);

	public boolean MIMEEntityIsDiscretePart(
			Pointer pMimeEntity);

	public boolean MIMEEntityIsMessagePart(
			Pointer pMIMEEntity);

	public boolean MIMEEntityIsMultiPart(
			Pointer pMimeEntity);

	public short MIMEFreeEntityDataObject(
			int hNote,
			Pointer pEntity);

	public short MIMEGetDecodedEntityData(
			int hNote,
			Pointer pMimeEntity,
			int dwEncodedOffset,
			int dwChunkLen,
			IntByReference phData,
			IntByReference pdwDecodedDataLen,
			IntByReference pdwEncodedDataLen);

	public short MIMEGetEntityData(
			int hNote,
			Pointer pME,
			short wDataType,
			int dwOffset,
			int dwRetBytes,
			IntByReference phData,
			IntByReference pdwDataLen);
	
	public short MIMEGetEntityDataSize(
			int  hNote,
			Pointer pME,
			short wDataType,
			IntByReference pdwDataLen);
	
	public short MIMEGetEntityPartFlags(
			int hNote,
			Pointer pEntity,
			IntByReference pdwFlags);

	public void  MimeGetExtFromTypeInfo(
			Memory pszType,
			Memory pszSubtype,
			Memory pszExtBuf,
			short wExtBufLen,
			Memory pszDescrBuf,
			short wDescrBufLen);
	
	public void MIMEGetFirstSubpart(
			int  hMIMEDir,
			Pointer  pMIMEEntity,
			PointerByReference retpMIMEEntity);

	public short MIMEGetNextSibling(
			int hMIMEDir,
			Pointer pMIMEEntity,
			PointerByReference retpMIMEEntity);
	
	public short MIMEGetParent(
			int  hMIMEDir,
			Pointer  pMIMEEntity,
			PointerByReference retpMIMEEntity);

	public short MIMEGetPrevSibling(
			int  hMIMEDir,
			Pointer  pMIMEEntity,
			PointerByReference retpMIMEEntity);
	
	public short MIMEGetRootEntity(
			int  hMIMEDir,
			PointerByReference retpMIMEEntity);	

	public short MIMEGetText(
			int hNote,
			Memory pchItemName,
			short wItemNameLen,
			boolean bNotesEOL,
			Memory pchBuf,
			int dwMaxBufLen,
			IntByReference pdwBufLen);
	
	public void MimeGetTypeInfoFromExt(
			Memory pszExt,
			Memory pszTypeBuf,
			short wTypeBufLen,
			Memory pszSubtypeBuf,
			short wSubtypeBufLen,
			Memory pszDescrBuf,
			short wDescrBufLen);

	public short MIMEHeaderNameToItemName(
			short wMessageType,
			Memory pszHeaderName,
			Memory pszHeaderBody,
			Memory retszItemName,
			short wItemNameSize,
			ShortByReference retwHeaderType,
			ShortByReference retwItemFlags,
			ShortByReference retwDataType);
	
	public short MIMEItemNameToHeaderName(
			short wMessageType,
			Memory pszItemName,
			Memory retszHeaderName,
			short wHeaderNameSize,
			ShortByReference retwHeaderType);

	public short MIMEIterateNext(
			int  hMIMEDir,
			Pointer pTopMIMEEntity,
			Pointer pPrevMIMEEntity,
			PointerByReference retpMIMEEntity);

	public int MIMEStreamGetLine(
			Memory pszLine,
			int uiMaxLineSize,
			Pointer hMIMEStream);
	
	public short MIMEStreamItemize(
			int hNote,
			Memory pchItemName,
			short wItemNameLen,
			int dwFlags,
			Pointer hMIMEStream);

	public short MIMEStreamOpen(
			int hNote,
			Memory pchItemName,
			short wItemNameLen,
			int dwOpenFlags,
			PointerByReference rethMIMEStream);

	public int MIMEStreamPutLine(
			Memory pszLine,
			Pointer hMIMEStream);

	public int MIMEStreamRead(
			Memory pchData,
			IntByReference puiDataLen,
			int uiMaxDataLen,
			Pointer hMIMEStream);	
	
	public int MIMEStreamRewind(
			Pointer hMIMEStream);

	public int MIMEStreamWrite(
			Memory pchData,
			int  uiDataLen,
			Pointer hMIMEStream);

	public void MIMEStreamClose(
			Pointer hMIMEStream);

	public short MIMEConvertCDParts(
			int hNote,
			boolean bCanonical,
			boolean bIsMIME,
			Pointer hCC);
	
}
