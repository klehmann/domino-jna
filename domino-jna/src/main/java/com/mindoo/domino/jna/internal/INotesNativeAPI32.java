package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.internal.NotesCallbacks.ABORTCHECKPROC;
import com.mindoo.domino.jna.internal.NotesCallbacks.ACLENTRYENUMFUNC;
import com.mindoo.domino.jna.internal.NotesCallbacks.OSSIGMSGPROC;
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

public interface INotesNativeAPI32 extends Library {

	short NSFSearch(
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
	short NSFSearchExtended3 (int hDB, 
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
	short NSFGetFolderSearchFilter(int hViewDB, int hDataDB, int ViewNoteID, NotesTimeDateStruct Since, int Flags, IntByReference Filter);

	/**
	 * @deprecated use {@link Mem32#OSLockObject(int)} instead
	 */
	@Deprecated
	Pointer OSLockObject(int handle);
	/**
	 * @deprecated use {@link Mem32#OSUnlockObject(int)} instead
	 */
	@Deprecated
	boolean OSUnlockObject(int handle);
	/**
	 * @deprecated use {@link Mem32#OSMemFree(int)} instead
	 */
	@Deprecated
	short OSMemFree(int handle);
	/**
	 * @deprecated use {@link Mem32#OSMemGetSize(int, IntByReference)} instead
	 */
	@Deprecated
	short OSMemGetSize(int handle, IntByReference retSize);
	/**
	 * @deprecated use {@link Mem32#OSMemGetSize(int, IntByReference)} instead
	 */
	@Deprecated
	int OSMemoryGetSize(int handle);
	/**
	 * @deprecated use {@link Mem32#OSMemoryFree(int)} instead
	 */
	@Deprecated
	void OSMemoryFree(int handle);
	/**
	 * @deprecated use {@link Mem32#OSMemoryAllocate(int, int, IntByReference)} instead
	 */
	@Deprecated
	short OSMemoryAllocate(int dwtype, int size, IntByReference retHandle);
	/**
	 * @deprecated use {@link Mem32#OSMemoryReallocate(int, int)} instead
	 */
	@Deprecated
	short OSMemoryReallocate(int handle, int size);
	/**
	 * @deprecated use {@link Mem32#OSMemoryLock(int)} instead
	 */
	@Deprecated
	Pointer OSMemoryLock(int handle);
	/**
	 * @deprecated use {@link Mem32#OSMemoryUnlock(int)} instead
	 */
	@Deprecated
	boolean OSMemoryUnlock(int handle);
	/**
	 * @deprecated use {@link Mem32#OSMemAlloc(short, int, IntByReference)} instead
	 */
	@Deprecated
	short OSMemAlloc(
			short  BlkType,
			int  dwSize,
			IntByReference retHandle);
	
	@Deprecated
	short OSMemGetType(int handle);

	short NSFItemGetText(
			int  note_handle,
			Memory item_name,
			Memory item_text,
			short text_len);

	short ListAllocate(
			short ListEntries,
			short TextSize,
			int fPrefixDataType,
			IntByReference rethList,
			Memory retpList,
			ShortByReference retListSize);
	
	short ListAddEntry(
			int hList,
			int fPrefixDataType,
			ShortByReference pListSize,
			short EntryNumber,
			Memory Text,
			short TextSize);

	short ListRemoveAllEntries(
			int hList,
			int fPrefixDataType,
			ShortByReference pListSize);

	short NSFItemInfo(
			int  note_handle,
			Memory item_name,
			short name_len,
			NotesBlockIdStruct retbhItem,
			ShortByReference retDataType,
			NotesBlockIdStruct retbhValue,
			IntByReference retValueLength);

	short NSFItemInfoNext(
			int  note_handle,
			NotesBlockIdStruct.ByValue NextItem,
			Memory item_name,
			short name_len,
			NotesBlockIdStruct retbhItem,
			ShortByReference retDataType,
			NotesBlockIdStruct retbhValue,
			IntByReference retValueLength);
	
	short NSFItemInfoPrev(
			int  note_handle,
			NotesBlockIdStruct.ByValue  CurrItem,
			Memory item_name,
			short  name_len,
			NotesBlockIdStruct item_blockid_ptr,
			ShortByReference value_type_ptr,
			NotesBlockIdStruct value_blockid_ptr,
			IntByReference value_len_ptr);
	
	void NSFItemQueryEx(
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

	short NSFItemGetModifiedTimeByBLOCKID(
			int  hNote,
			NotesBlockIdStruct.ByValue bhItem,
			int  Flags,
			NotesTimeDateStruct retTime);

	short NSFItemGetTextListEntries(
			int note_handle,
			Memory item_name);

	short NSFItemGetTextListEntry(
			int note_handle,
			Memory item_name,
			short entry_position,
			Memory retEntry_text,
			short  text_len);
	short NSFItemGetModifiedTime(
			int hNote,
			Memory ItemName,
			short  ItemNameLength,
			int  Flags,
			NotesTimeDateStruct retTime);
	short NSFItemSetText(
			int hNote,
			Memory ItemName,
			Memory ItemText,
			short TextLength);
	short NSFItemSetTextSummary(
			int hNote,
			Memory ItemName,
			Memory ItemText,
			short TextLength,
			boolean summary);
	boolean NSFItemGetTime(
			int  note_handle,
			Memory td_item_name,
			NotesTimeDateStruct td_item_value);
	short NSFItemSetTime(
			int  note_handle,
			Memory td_item_name,
			NotesTimeDateStruct td_item_ptr);
	boolean NSFItemGetNumber(
			int hNote,
			Memory ItemName,
			DoubleByReference retNumber);
	int NSFItemGetLong(
			int note_handle,
			Memory number_item_name,
			int number_item_default);
	short NSFItemSetNumber(
			int  hNote,
			Memory ItemName,
			Memory Number);
	short NSFItemConvertToText(
			int note_handle,
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
			int note_handle,
			Memory item_name,
			short name_len);
	short NSFItemDeleteByBLOCKID(int note_handle, NotesBlockIdStruct.ByValue item_blockid);
	short NSFItemAppend(
			int note_handle,
			short item_flags,
			Memory item_name,
			short name_len,
			short  item_type,
			Pointer item_value,
			int value_len);
	short NSFItemAppendByBLOCKID(
			int note_handle,
			short item_flags,
			Memory item_name,
			short name_len,
			NotesBlockIdStruct.ByValue value_bid,
			int value_len,
			NotesBlockIdStruct item_bid_ptr);

	@UndocumentedAPI
	short NSFItemModifyValue (int hNote, NotesBlockIdStruct.ByValue bhItem, short ItemFlags, short DataType,
			Pointer valuePtr, int valueLength);

	void NSFNoteGetInfo(int hNote, short type, Pointer retValue);
	void NSFNoteSetInfo(int hNote, short type, Pointer value);
	short NSFNoteCopy(
			int note_handle_src,
			IntByReference note_handle_dst_ptr);

	short NSFNoteUpdateExtended(int hNote, int updateFlags);
	short NSFNoteCreate(int db_handle, IntByReference note_handle);
	short NSFNoteOpen(int hDB, int noteId, short openFlags, IntByReference rethNote);
	short NSFNoteOpenExt(int hDB, int noteId, int flags, IntByReference rethNote);
	short NSFNoteOpenByUNID(
			int hDB,
			NotesUniversalNoteIdStruct pUNID,
			short  flags,
			IntByReference rethNote);
	@UndocumentedAPI
	short NSFNoteOpenByUNIDExtended(int hDB, NotesUniversalNoteIdStruct pUNID, int flags, IntByReference rtn);
	short NSFNoteClose(int hNote);
	short NSFNoteVerifySignature(
			int  hNote,
			Memory SignatureItemName,
			NotesTimeDateStruct retWhenSigned,
			Memory retSigner,
			Memory retCertifier);
	short NSFNoteContract(int hNote);
	short NSFNoteExpand(int hNote);
	short NSFNoteSign(int hNote);
	short NSFNoteSignExt3(int hNote, 
			int hKFC,
			Memory SignatureItemName,
			short ItemCount, int hItemIDs, 
			int Flags, int Reserved,
			Pointer pReserved);
	short NSFNoteOpenSoftDelete(int hDB, int NoteID, int Reserved, IntByReference rethNote);
	short NSFNoteHardDelete(int hDB, int NoteID, int Reserved);
	short NSFNoteDeleteExtended(int hDB, int NoteID, int UpdateFlags);
	short NSFNoteDetachFile(int note_handle, NotesBlockIdStruct.ByValue item_blockid);
	boolean NSFNoteIsSignedOrSealed(int note_handle, ByteByReference signed_flag_ptr, ByteByReference sealed_flag_ptr);
	short NSFNoteUnsign(int hNote);
	short NSFNoteComputeWithForm(
			int  hNote,
			int  hFormNote,
			int  dwFlags,
			NotesCallbacks.b32_CWFErrorProc ErrorRoutine,
			Pointer CallersContext);
	short NSFNoteHasComposite(int hNote);
	short NSFNoteHasMIME(int hNote);
	short NSFNoteHasMIMEPart(int hNote);
	@UndocumentedAPI
	short NSFIsFileItemMimePart(int hNote, NotesBlockIdStruct.ByValue bhFileItem);
	@UndocumentedAPI
	short NSFIsMimePartInFile(int hNote, NotesBlockIdStruct.ByValue bhMIMEItem, Memory pszFileName, short wMaxFileNameLen);
	
	short NSFMimePartCreateStream(
			int hNote,
			Memory pchItemName,
			short wItemNameLen,
			short wPartType,
			int dwFlags,
			IntByReference phCtx);
	
	short NSFMimePartAppendStream(
			int hCtx,
			Memory pchData,
			short wDataLen);
	
	short NSFMimePartAppendFileToStream(
			int hCtx,
			Memory pszFilename);
	short NSFMimePartAppendObjectToStream(
			int hCtx,
			Memory pszAttachmentName);
	short NSFMimePartCloseStream(
			int hCtx,
			short  bUpdate);
//	short MIMEStreamOpen(
//			int hNote,
//			Memory pchItemName,
//			short wItemNameLen,
//			int dwOpenFlags,
//			IntByReference rethMIMEStream);
//	int MIMEStreamPutLine(
//			Memory pszLine,
//			int hMIMEStream);
//	short MIMEStreamItemize(
//			int hNote,
//			Memory pchItemName,
//			short wItemNameLen,
//			int dwFlags,
//			int hMIMEStream);
//	int MIMEStreamWrite(
//			Pointer pchData,
//			int uiDataLen,
//			int hMIMEStream);
//	void MIMEStreamClose(
//			Pointer hMIMEStream);
	short MIMEConvertRFC822TextItemByBLOCKID(
			int hNote,
			NotesBlockIdStruct.ByValue bhItem,
			NotesBlockIdStruct.ByValue bhValue);
	
	@UndocumentedAPI
	short NSFNoteHasReadersField(int hNote, NotesBlockIdStruct bhFirstReadersItem);
	short NSFNoteCipherExtractWithCallback (int hNote, NotesBlockIdStruct.ByValue bhItem,
			int ExtractFlags, int hDecryptionCipher,
			NotesCallbacks.NoteExtractCallback pNoteExtractCallback, Pointer pParam,
			int Reserved, Pointer pReserved);
	short NSFNoteCopyAndEncryptExt2(
			int hSrcNote,
			int hKFC,
			short EncryptFlags,
			IntByReference rethDstNote,
			int  Reserved,
			Pointer pReserved);
	short NSFNoteCopyAndEncrypt(
			int hSrcNote,
			short EncryptFlags,
			IntByReference rethDstNote);
	short NSFNoteCipherDecrypt(
			int  hNote,
			int hKFC,
			int  DecryptFlags,
			LongByReference rethCipherForAttachments,
			int  Reserved,
			Pointer pReserved);
	short NSFNoteAttachFile(
			int note_handle,
			Memory item_name,
			short item_name_length,
			Memory file_name,
			Memory orig_path_name,
			short encoding_type);
	short NSFNoteSignHotspots(
			int hNote,
			int dwFlags,
			IntByReference retfSigned);
	short NSFNoteLSCompile(
			int hDb,
			int hNote,
			int dwFlags);
	short NSFNoteLSCompileExt(
			int hDb,
			int hNote,
			int dwFlags,
			NotesCallbacks.LSCompilerErrorProc pfnErrProc,
			Pointer pCtx);
	short NSFNoteCheck(
			int hNote
			);

	short NSFDbNoteLock(
			int hDB,
			int NoteID,
			int Flags,
			Memory pLockers,
			IntByReference rethLockers,
			IntByReference retLength);

	short NSFDbNoteUnlock(
			int hDB,
			int NoteID,
			int Flags);
	
	short NSFNoteOpenWithLock(
			int hDB,
			int NoteID,
			int LockFlags,
			int OpenFlags,
			Memory pLockers,
			IntByReference rethLockers,
			IntByReference retLength,
			IntByReference rethNote);
	
	short NSFItemCopy(int note_handle, NotesBlockIdStruct.ByValue item_blockid);
	@UndocumentedAPI
	short NSFItemCopyAndRename (int hNote, NotesBlockIdStruct.ByValue bhItem, Memory pszNewItemName);
	
	short IDCreateTable (int alignment, IntByReference rethTable);
	short IDDestroyTable(int hTable);
	short IDInsert (int hTable, int id, IntByReference retfInserted);
	short IDDelete (int hTable, int id, IntByReference retfDeleted);
	boolean IDScan (int hTable, boolean fFirst, IntByReference retID);
	@UndocumentedAPI
	boolean IDScanBack (int hTable, boolean fLast, IntByReference retID);
	int IDEntries (int hTable);
	boolean IDIsPresent (int hTable, int id);
	int IDTableSize (int hTable);
	int IDTableSizeP(Pointer pIDTable);
	short IDTableCopy (int hTable, IntByReference rethTable);
	short IDTableIntersect(int hSrc1Table, int hSrc2Table, IntByReference rethDstTable);
	short IDDeleteAll (int hTable);
	boolean IDAreTablesEqual	(int hSrc1Table, int hSrc2Table);
	short IDDeleteTable(int hTable, int hIDsToDelete);
	short IDInsertTable  (int hTable, int hIDsToAdd);
	short IDEnumerate(int hTable, NotesCallbacks.IdEnumerateProc Routine, Pointer Parameter);
	@UndocumentedAPI
	short IDInsertRange(int hTable, int IDFrom, int IDTo, boolean AddToEnd);
	@UndocumentedAPI
	short IDTableDifferences(int idtable1, int idtable2, IntByReference outputidtableAdds, IntByReference outputidtableDeletes, IntByReference outputidtableSame);
	@UndocumentedAPI
	short IDTableReplaceExtended(int idtableSrc, int idtableDest, byte flags);

	short NSFDbStampNotesMultiItem(int hDB, int hTable, int hInNote);
	short NSFDbOpen(Memory dbName, IntByReference dbHandle);
	short NSFDbOpenExtended (Memory PathName, short Options, int hNames, NotesTimeDateStruct ModifiedTime, IntByReference rethDB, NotesTimeDateStruct retDataModified, NotesTimeDateStruct retNonDataModified);
	short NSFDbGenerateOID(int hDB, NotesOriginatorIdStruct retOID);
	short NSFDbClose(int dbHandle);
	int NSFDbGetOpenDatabaseID(int hDBU);
	short NSFDbReopen(int hDB, IntByReference rethDB);
	short NSFDbLocateByReplicaID(
			int  hDB,
			NotesTimeDateStruct ReplicaID,
			Memory retPathName,
			short PathMaxLen);
	short NSFDbModifiedTime(
			int hDB,
			NotesTimeDateStruct retDataModified,
			NotesTimeDateStruct retNonDataModified);
	short NSFDbIDGet(int hDB, NotesTimeDateStruct retDbID);
	short NSFDbReplicaInfoGet(
			int  hDB,
			NotesDbReplicaInfoStruct retReplicationInfo);
	short NSFDbReplicaInfoSet(
			int  hDB,
			NotesDbReplicaInfoStruct ReplicationInfo);
	short NSFDbGetModifiedNoteTable(int hDB, short NoteClassMask, NotesTimeDateStruct.ByValue Since, NotesTimeDateStruct retUntil, IntByReference rethTable);
	short NSFDbGetNotes(
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
	short NSFDbGetMultNoteInfo(
			int  hDb,
			short  Count,
			short  Options,
			int  hInBuf,
			IntByReference retSize,
			IntByReference rethOutBuf);
	short NSFDbGetNoteInfoExt(
			int  hDB,
			int  NoteID,
			NotesOriginatorIdStruct retNoteOID,
			NotesTimeDateStruct retModified,
			ShortByReference retNoteClass,
			NotesTimeDateStruct retAddedToFile,
			ShortByReference retResponseCount,
			IntByReference retParentNoteID);
	short NSFDbGetMultNoteInfoByUNID(
			int hDB,
			short Count,
			short Options,
			int hInBuf,
			IntByReference retSize,
			IntByReference rethOutBuf);
	short NSFDbSign(int hDb, short noteclass);
	@UndocumentedAPI
	short NSFDbGetOptionsExt(int hDB, Memory retDbOptions);
	@UndocumentedAPI
	short NSFDbSetOptionsExt(int hDB, Memory dbOptions, Memory mask);
	void NSFDbAccessGet(int hDB, ShortByReference retAccessLevel, ShortByReference retAccessFlag);
	short NSFDbGetBuildVersion(int hDB, ShortByReference retVersion);
	short NSFDbGetMajMinVersion(int hDb, NotesBuildVersionStruct retBuildVersion);
	short NSFDbReadObject(
			int hDB,
			int ObjectID,
			int Offset,
			int Length,
			IntByReference rethBuffer);
	
	short NSFDbAllocObject(
			int hDB,
			int dwSize,
			short Class,
			short Privileges,
			IntByReference retObjectID);
	
	short NSFDbAllocObjectExtended2(int cDB,
			int size, short noteClass, short privs, short type, IntByReference rtnRRV);
	
	short NSFDbWriteObject(
			int hDB,
			int ObjectID,
			int hBuffer,
			int Offset,
			int Length);
	
	short NSFDbFreeObject(
			int hDB,
			int ObjectID);
	
	short NSFDbReallocObject(
			int hDB,
			int ObjectID,
			int NewSize);

	short NSFDbGetObjectSize(
			int hDB,
			int ObjectID,
			short ObjectType,
			IntByReference retSize,
			ShortByReference retClass,
			ShortByReference retPrivileges);

	short NSFItemAppendObject(
			int hNote,
			short ItemFlags,
			Memory Name,
			short NameLength,
			NotesBlockIdStruct.ByValue bhValue,
			int ValueLength,
			int fDealloc);
	
	short NSFDbGetSpecialNoteID(
			int hDB,
			short Index,
			IntByReference retNoteID);
	short NSFDbClearReplHistory(int hDb, int dwFlags);
	short NSFDbPathGet(
			int hDB,
			Memory retCanonicalPathName,
			Memory retExpandedPathName);
	@UndocumentedAPI
	short NSFDbIsRemote(int hDb);
	@UndocumentedAPI
	short NSFDbHasFullAccess(int hDb);
	short NSFDbSpaceUsage(int dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes);
	short NSFDbSpaceUsageScaled (int dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes, IntByReference retGranularity);
	short NSFDbDeleteNotes(int  hDB, int  hTable, Memory retUNIDArray);
	short NSFDbIsLocallyEncrypted(int hDB, IntByReference retVal);
	short NSFDbInfoGet(
			int hDB,
			Pointer retBuffer);
	short NSFDbInfoSet(
			int hDB,
			Pointer Buffer);
	short NSFDbModeGet(
			int hDB,
			ShortByReference retMode);
	@UndocumentedAPI
	short NSFDbLock(int hDb);
	@UndocumentedAPI
	void NSFDbUnlock(int hDb, ShortByReference statusInOut);
	
	@UndocumentedAPI
	short NSFHideDesign(int hdb1, int hdb2, int param3, int param4);

	short NSFBuildNamesList(Memory UserName, int dwFlags, IntByReference rethNamesList);
	@UndocumentedAPI
	short CreateNamesListFromGroupNameExtend(Memory pszServerName, Memory pTarget, IntByReference rethNames);
	@UndocumentedAPI
	short CreateNamesListFromNames(short cTargets, Pointer ptrArrTargets, IntByReference rethNames);
	@UndocumentedAPI
	short CreateNamesListFromNamesExtend(Memory pszServerName, short cTargets, Pointer ptrArrTargets, IntByReference rethNames);
//	@UndocumentedAPI
//	short CreateNamesListFromSessionID(Memory pszServerName, SESSIONID SessionId, IntByReference rtnhNames);
	@UndocumentedAPI
	short CreateNamesListFromSingleName(Memory pszServerName, short fDontLookupAlternateNames,
			Pointer pLookupFlags, Memory pTarget, IntByReference rethNames);
	@UndocumentedAPI
	short CreateNamesListUsingLookupName(Memory pszServerName,Pointer pLookupFlags, Memory pTarget,
			IntByReference rethNames);
	
	short NIFReadEntries(int hCollection, NotesCollectionPositionStruct IndexPos, short SkipNavigator, int SkipCount, short ReturnNavigator, int ReturnCount, int ReturnMask, IntByReference rethBuffer,
			ShortByReference retBufferLength, IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned, ShortByReference retSignalFlags);
	@UndocumentedAPI
	short NIFReadEntriesExt(int hCollection,
			NotesCollectionPositionStruct CollectionPos,
			short SkipNavigator, int SkipCount,
			short ReturnNavigator, int ReturnCount, int ReturnMask,
			NotesTimeDateStruct DiffTime, int DiffIDTable, int ColumnNumber, int Flags,
			IntByReference rethBuffer, ShortByReference retBufferLength,
			IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned,
			ShortByReference retSignalFlags, NotesTimeDateStruct retDiffTime,
			NotesTimeDateStruct retModifiedTime, IntByReference retSequence);
	void NIFGetLastModifiedTime(int hCollection, NotesTimeDateStruct retLastModifiedTime);
	void NIFGetLastAccessedTime(int hCollection, NotesTimeDateStruct retLastModifiedTime);
	void NIFGetNextDiscardTime(int hCollection, NotesTimeDateStruct retLastModifiedTime);
	short NIFFindByKeyExtended2 (int hCollection, Memory keyBuffer,
			int findFlags,
			int returnFlags,
			NotesCollectionPositionStruct retIndexPos,
			IntByReference retNumMatches,
			ShortByReference retSignalFlags,
			IntByReference rethBuffer,
			IntByReference retSequence);
	@UndocumentedAPI
	short NIFFindByKeyExtended3 (int hCollection,
			Memory keyBuffer, int findFlags,
			int returnFlags,
			NotesCollectionPositionStruct retIndexPos,
			IntByReference retNumMatches, ShortByReference retSignalFlags,
			IntByReference rethBuffer, IntByReference retSequence,
			NotesCallbacks.NIFFindByKeyProc NIFFindByKeyCallback, NIFFindByKeyContextStruct Ctx);
	short NIFFindByKey(int hCollection, Memory keyBuffer, short findFlags, NotesCollectionPositionStruct retIndexPos, IntByReference retNumMatches);
	short NIFFindByName(int hCollection, Memory name, short findFlags, NotesCollectionPositionStruct retIndexPos, IntByReference retNumMatches);
	short NIFGetCollation(int hCollection, ShortByReference retCollationNum);
	short NIFSetCollation(int hCollection, short CollationNum);
	short NIFUpdateCollection(int hCollection);
	@UndocumentedAPI
	short NIFIsNoteInView(int hCollection, int noteID, IntByReference retIsInView);
	@UndocumentedAPI
	boolean NIFIsUpdateInProgress(int hCollection);
	@UndocumentedAPI
	short NIFGetIDTableExtended(int hCollection, short navigator, short Flags, int hIDTable);
	@UndocumentedAPI
	boolean NIFCollectionUpToDate(int hCollection);
	@UndocumentedAPI
    boolean NIFSetCollectionInfo (int hCollection, Pointer SessionID,
            int hUnreadList, int hCollapsedList, int hSelectedList);
	@UndocumentedAPI
    short NIFUpdateFilters (int hCollection, short ModifyFlags);
	@UndocumentedAPI
    boolean NIFIsTimeVariantView(int hCollection);
	short NIFCloseCollection(int hCollection);
	short NIFLocateNote (int hCollection, NotesCollectionPositionStruct indexPos, int noteID);
	@UndocumentedAPI
	short NIFFindDesignNoteExt(int hFile, Memory name, short noteClass, Memory pszFlagsPattern, IntByReference retNoteID, int Options);
	short NIFOpenCollection(int hViewDB, int hDataDB, int ViewNoteID, short OpenFlags, int hUnreadList, IntByReference rethCollection, IntByReference rethViewNote, Memory retViewUNID,
			IntByReference rethCollapsedList, IntByReference rethSelectedList);
	short NIFOpenCollectionWithUserNameList (int hViewDB, int hDataDB,
			int ViewNoteID, short OpenFlags,
			int hUnreadList,
			IntByReference rethCollection,
			IntByReference rethViewNote, Memory retViewUNID,
			IntByReference rethCollapsedList,
			IntByReference rethSelectedList,
			int nameList);
	short NIFGetCollectionData(
			int hCollection,
			IntByReference rethCollData);
	short NIFGetCollectionDocCountLW(int hCol, IntByReference pDocct);

	@UndocumentedAPI
	short NSFTransactionBegin(int hDB, int flags);
	@UndocumentedAPI
	short NSFTransactionCommit(int hDB, int flags);
	@UndocumentedAPI
	short NSFTransactionRollback(int hDB);

	//backup APIs
	short NSFDbGetLogInfo(int hDb, int Flags, ShortByReference LOGGED, NotesUniversalNoteIdStruct LogID,
			NotesUniversalNoteIdStruct DbIID, IntByReference LogExtent);
	short NSFBackupStart(int hDB, int Flags, IntByReference BackupContext, IntByReference FileSizeLow,
			IntByReference FileSizeHigh);
	short NSFBackupStop(int hDB, int BackupContext);
	short NSFBackupEnd(int hDB, int BackupContext, int Options);
	short NSFBackupGetChangeInfoSize(int hDB, int hBackupContext, int Flags, IntByReference InfoSizeLow,
			IntByReference InfoSizeHigh);
	short NSFBackupStartApplyChangeInfo(IntByReference ApplyInfoContext, Memory CopyFilePath, int Flags,
			int InfoSizeLow, int InfoSizeHigh);
	short NSFBackupGetNextChangeInfo(int hDB, int hBackupContext, int Flags, Memory Buffer, int BufferSize,
			IntByReference FilledSize);
	short NSFBackupApplyNextChangeInfo(int ApplyInfoContext, int Flags, Memory Buffer, int BufferSize);
	short NSFBackupEndApplyChangeInfo(int ApplyInfoContext, int Flags);

	short AgentDelete (int hAgent); /* delete agent */
	@UndocumentedAPI
	boolean IsRunAsWebUser(int hAgent);
	short AgentOpen (int hDB, int AgentNoteID, IntByReference rethAgent);
	void AgentClose (int hAgent);
	short AgentCreateRunContext (int hAgent,
			 Pointer pReserved,
			 int dwFlags,
			 IntByReference rethContext);
	@UndocumentedAPI
	short AgentCreateRunContextExt (int hAgent, Pointer pReserved, int pOldContext, int dwFlags, IntByReference rethContext);
	short AgentSetDocumentContext(int hAgentCtx, int hNote);
	short AgentSetTimeExecutionLimit(int hAgentCtx, int timeLimit);
	boolean AgentIsEnabled(int hAgent);
	@UndocumentedAPI
	void SetParamNoteID(int hAgentCtx, int noteId);
	@UndocumentedAPI
	short AgentSetUserName(int hAgentCtx, int hNameList);
	short AgentRedirectStdout(int hAgentCtx, short redirType);
	void AgentQueryStdoutBuffer(int hAgentCtx, IntByReference retHdl, IntByReference retSize);
	void AgentDestroyRunContext (int hAgentCtx);
	short AgentRun (int hAgent,
			int hAgentCtx,
		    int hSelection,
			int dwFlags);
	@UndocumentedAPI
	short AgentSetHttpStatusCode(int hAgentCtx, int httpStatus);
	@UndocumentedAPI
	short ClientRunServerAgent(int hdb, int nidAgent, int nidParamDoc,
			int bForeignServer, int bSuppressPrintToConsole);
	
	short FTIndex(int hDB, short options, Memory stopFile, NotesFTIndexStatsStruct retStats);
	@UndocumentedAPI
	short ClientFTIndexRequest(int hDB);
	short FTDeleteIndex(int hDB);
	short FTGetLastIndexTime(int hDB, NotesTimeDateStruct retTime);
	short FTOpenSearch(IntByReference rethSearch);
	short FTCloseSearch(int hSearch);
	short FTSearch(
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
	short FTSearchExt(
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
	
	short NSFFormulaCompile(
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
	short NSFFormulaDecompile(
			Pointer pFormulaBuffer,
			boolean fSelectionFormula,
			IntByReference rethFormulaText,
			ShortByReference retFormulaTextLength);
	short NSFFormulaSummaryItem(int hFormula, Memory ItemName, short ItemNameLength);
	short NSFFormulaMerge(
			int hSrcFormula,
			int hDestFormula);

	short NSFComputeStart(
			short Flags,
			Pointer lpCompiledFormula,
			IntByReference rethCompute);
	short NSFComputeStop(int hCompute);
	short NSFComputeEvaluate(
			int  hCompute,
			int hNote,
			IntByReference rethResult,
			ShortByReference retResultLength,
			IntByReference retNoteMatchesFormula,
			IntByReference retNoteShouldBeDeleted,
			IntByReference retNoteModified);

	@UndocumentedAPI
	short CESCreateCTXFromNote(int hNote, IntByReference rethCESCTX);
	@UndocumentedAPI
	short CESGetNoSigCTX(IntByReference rethCESCTX);
	@UndocumentedAPI
	short CESFreeCTX(int hCESCTX);
	@UndocumentedAPI
	short ECLUserTrustSigner ( int hCESCtx, 
			short ECLType,
			short bSessionOnly,
			short wCapabilities,
			short wCapabilities2,
			ShortByReference retwCurrentCapabilities,
			ShortByReference retwCurrentCapabilities2);

	short NSFFolderGetIDTable(
			int  hViewDB,
			int hDataDB,
			int  viewNoteID,
			int  flags,
			IntByReference hTable);
	
	short NSFGetAllFolderChanges(
			int hViewDB,
			int hDataDB,
			NotesTimeDateStruct since,
			int flags,
			NotesCallbacks.b32_NSFGetAllFolderChangesCallback Callback,
			Pointer Param,
			NotesTimeDateStruct until);

	short NSFGetFolderChanges(
			int hViewDB,
			int hDataDB,
			int viewNoteID,
			NotesTimeDateStruct since,
			int Flags,
			IntByReference addedNoteTable,
			IntByReference removedNoteTable);

	short FolderDocAdd(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			int  hTable,
			int  dwFlags);
	short FolderDocCount(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			int dwFlags,
			IntByReference pdwNumDocs);
	short FolderDocRemove(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			int  hTable,
			int dwFlags);
	short FolderDocRemoveAll(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			int dwFlags);

	short FolderCreate(
			int hDataDB,
			int hFolderDB,
			int FormatNoteID,
			int hFormatDB,
			Memory pszName,
			short wNameLen,
			int FolderType,
			int dwFlags,
			IntByReference pNoteID);
	
	short FolderRename(
			int hDataDB,
			int hFolderDB,
			int FolderNoteID,
			Memory pszName,
			short wNameLen,
			int dwFlags);

	short FolderDelete(
			int hDataDB,
			int hFolderDB,
			int FolderNoteID,
			int dwFlags);
	
	short FolderCopy(
			int hDataDB,
			int hFolderDB,
			int FolderNoteID,
			Memory pszName,
			short wNameLen,
			int dwFlags,
			IntByReference pNewNoteID);

	short FolderMove(
			int hDataDB,
			int hFolderDB,
			int FolderNoteID,
			int hParentDB,
			int ParentNoteID,
			int dwFlags);

	short NSFProfileOpen(
			int hDB,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength,
			short CopyProfile,
			IntByReference rethProfileNote);
	short NSFProfileUpdate(
			int hProfile,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength);
	short NSFProfileSetField(
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
	short NSFProfileDelete(
			int hDB,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength);

	short NSFProfileEnum(
			int hDB,
			Memory ProfileName,
			short ProfileNameLength,
			NotesCallbacks.b32_NSFPROFILEENUMPROC Callback,
			Pointer CallbackCtx,
			int Flags);

	short SECKFMOpen(IntByReference phKFC, Memory pIDFileName, Memory pPassword,
			int Flags, int Reserved, Pointer pReserved);
	short SECKFMClose(IntByReference phKFC, int Flags, int Reserved, Pointer pReserved);
	@UndocumentedAPI
	short SECKFMAccess(short param1, int hKFC, Pointer retUsername, Pointer param4);

	short SECidfGet(Memory pUserName, Memory pPassword, Memory pPutIDFileHere,
			IntByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved);
	short SECidfPut(Memory pUserName, Memory pPassword, Memory pIDFilePath,
			IntByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved);
	short SECidfSync( Memory pUserName, Memory pPassword, Memory pIDFilePath,
			IntByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved, IntByReference retdwFlags);
	short SECTokenGenerate(
			Memory ServerName,
			Memory OrgName,
			Memory ConfigName,
			Memory UserName,
			NotesTimeDateStruct Creation,
			NotesTimeDateStruct Expiration,
			IntByReference retmhToken,
			int dwReserved,
			Pointer vpReserved);
	void SECTokenFree(IntByReference mhToken);

	short SchFreeTimeSearch(
			NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate,
			short fFindFirstFit,
			int dwReserved,
			NotesTimeDatePairStruct pInterval,
			short Duration,
			Pointer pNames,
			IntByReference rethRange);

	short SchRetrieve(
			NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate,
			int dwOptions,
			NotesTimeDatePairStruct pInterval,
			Pointer pNames,
			IntByReference rethCntnr,
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
			IntByReference rethCntnr);

	void SchContainer_Free(int hCntnr);
	short SchContainer_GetFirstSchedule(
			int hCntnr,
			IntByReference rethObj,
			Memory retpSchedule);
	short Schedule_Free(int hCntnr, int hSched);
	short SchContainer_GetNextSchedule(
			int hCntnr,
			int hCurSchedule,
			IntByReference rethNextSchedule,
			Memory retpNextSchedule);
	short Schedule_ExtractFreeTimeRange(
			int hCntnr,
			int hSchedObj,
			NotesUniversalNoteIdStruct punidIgnore,
			short fFindFirstFit,
			short wDuration,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethRange);
	short Schedule_ExtractBusyTimeRange(
			int hCntnr,
			int hSchedObj,
			NotesUniversalNoteIdStruct punidIgnore,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethRange,
			IntByReference rethMoreCtx);
	short Schedule_ExtractMoreBusyTimeRange(
			int hCntnr,
			int hMoreCtx,
			NotesUniversalNoteIdStruct punidIgnore,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethRange,
			IntByReference rethMore);
	short Schedule_ExtractSchedList(
			int hCntnr,
			int hSchedObj,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethSchedList,
			IntByReference rethMore);
	short Schedule_ExtractMoreSchedList(
			int hCntnr,
			int hMoreCtx,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethSchedList,
			IntByReference rethMore);
	short Schedule_Access(
			int hCntnr,
			int hSched,
			PointerByReference pretSched);
	
	short Schedule_GetFirstDetails(
			int hCntnr,
			int hSchedObj,
			IntByReference rethDetailObj,
			PointerByReference retpDetail);
	
	short Schedule_GetNextDetails(
			int hCntnr,
			int hDetailObj,
			IntByReference rethNextDetailObj,
			PointerByReference retpNextDetail);
	
	short NSGetServerClusterMates(
			Memory pServerName,
			int dwFlags,
			IntByReference phList);
	short NSPingServer(
			Memory pServerName,
			IntByReference pdwIndex,
			IntByReference phList);

	short HTMLCreateConverter(IntByReference phHTML);
	short HTMLDestroyConverter(int hHTML);
	short HTMLSetHTMLOptions(int hHTML, StringArray optionList);
	short HTMLConvertItem(
			int hHTML,
			int hDB,
			int hNote,
			Memory pszItemName);
	short HTMLConvertNote(
			int hHTML,
			int hDB,
			int hNote,
			int NumArgs,
			HtmlApi_UrlComponentStruct pArgs);
	short HTMLGetProperty(
			int hHTML,
			int PropertyType,
			Pointer pProperty);
	
	@UndocumentedAPI
	short HTMLGetPropertyV (int hHTML,
			 int PropertyType, Pointer pProperty, int count);

	short HTMLSetProperty(
			int hHTML,
			int PropertyType,
			Pointer pProperty);
	short HTMLGetText(
			int hHTML,
			int StartingOffset,
			IntByReference pTextLength,
			Memory pText);
	short HTMLGetReference(
			int hHTML,
			int Index,
			IntByReference phRef);
	short HTMLLockAndFixupReference(
			int hRef,
			Memory ppRef);
	short HTMLConvertElement(
			int hHTML,
			int hDB,
			int hNote,
			Memory pszItemName,
			int ItemIndex,
			int Offset);
	
	short CompoundTextAddCDRecords(
			int hCompound,
			Pointer pvRecord,
			int dwRecordLength);
	short CompoundTextAddDocLink(
			int hCompound,
			NotesTimeDateStruct.ByValue DBReplicaID,
			NotesUniversalNoteIdStruct.ByValue ViewUNID,
			NotesUniversalNoteIdStruct.ByValue NoteUNID,
			Memory pszComment,
			int dwFlags);
	short CompoundTextAddParagraphExt(
			int hCompound,
			int dwStyleID,
			int FontID,
			Memory pchText,
			int dwTextLen,
			Pointer pInfo);

	short CompoundTextAddRenderedNote(
			int hCompound,
			int hNote,
			int hFormNote,
			int dwFlags);
	short CompoundTextAddTextExt(
			int hCompound,
			int dwStyleID,
			int FontID,
			Memory pchText,
			int dwTextLen,
			Memory pszLineDelim,
			int dwFlags,
			Pointer pInfo);
	short CompoundTextAssimilateFile(
			int hCompound,
			Memory pszFileName,
			int dwFlags);
	short CompoundTextAssimilateItem(
			int hCompound,
			int hNote,
			Memory pszItemName,
			int dwFlags);
	@UndocumentedAPI
	short CompoundTextAssimilateBuffer(int hBuffer, int bufferLength, int flags);
	short CompoundTextClose(
			int hCompound,
			IntByReference phReturnBuffer,
			IntByReference pdwReturnBufferSize,
			Memory pchReturnFile,
			short wReturnFileNameSize);
	short CompoundTextCreate(
			int hNote,
			Memory pszItemName,
			IntByReference phCompound);
	short CompoundTextDefineStyle(
			int hCompound,
			Memory pszStyleName,
			NotesCompoundStyleStruct pDefinition,
			IntByReference pdwStyleID);
	void CompoundTextDiscard(
			int hCompound);

	short DesignRefresh(
			Memory Server,
			int hDB,
			int dwFlags,
			ABORTCHECKPROC AbortCheck,
			OSSIGMSGPROC MessageProc);
	
	short NSFDbReadACL(
			int hDB,
			IntByReference rethACL);
	
	short ACLEnumEntries(
			int hACL,
			ACLENTRYENUMFUNC EnumFunc,
			Pointer EnumFuncParam);
	
	short ACLGetPrivName(
			int hACL,
			short PrivNum,
			Memory retPrivName);
	
	short NSFDbStoreACL(
			int hDB,
			int hACL,
			int ObjectID,
			short Method);
	
	short ACLLookupAccess(
			int hACL,
			Pointer pNamesList,
			ShortByReference retAccessLevel,
			Memory retPrivileges,
			ShortByReference retAccessFlags,
			IntByReference rethPrivNames);
	
	short ACLSetAdminServer(
			int hList,
			Memory ServerName);

	short ACLAddEntry(
			int hACL,
			Memory name,
			short AccessLevel,
			Memory privileges,
			short AccessFlags);
	
	short ACLDeleteEntry(
			int hACL,
			Memory name);

	short ACLSetFlags(
			int hACL,
			int Flags);
	
	short ACLGetFlags(
			int hACL,
			IntByReference retFlags);
	
	short ACLSetPrivName(
			int hACL,
			short PrivNum,
			Memory privName);

	short ACLUpdateEntry(
			int hACL,
			Memory name,
			short updateFlags,
			Memory newName,
			short newAccessLevel,
			Memory newPrivileges,
			short newAccessFlags);
	
	@UndocumentedAPI
	short NSFSearchStartExtended(int hDB, int formula, int filter,
			int filterflags, NotesUniversalNoteIdStruct ViewUNID, Memory ViewTitle, 
			int queue, int flags, int flags1, int flags2, int flags3, int flags4, 
			short noteClass, short auxclass, short granularity, 
			NotesTimeDateStruct.ByValue since, NotesTimeDateStruct rtnuntil, 
			IntByReference rtnhandle);

	@UndocumentedAPI
	short NSFSearchStop(int shandle);
	
	short CalCreateEntry(
			int hDB,
			Memory pszCalEntry,
			int dwFlags,
			IntByReference hRetUID,
			Pointer pCtx);

	short CalUpdateEntry(
			int hDB,
			Memory pszCalEntry,
			Memory pszUID,
			Memory pszRecurID,
			Memory pszComments,
			int dwFlags,
			Pointer pCtx);
	
	short CalGetUIDfromNOTEID(
			int hDB,
			int noteid,
			Memory pszUID,
			short wLen,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	short CalGetUIDfromUNID(
			int hDB,
			NotesUniversalNoteIdStruct unid,
			Memory pszUID,
			short wLen,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);

	short CalOpenNoteHandle(
			int hDB,
			Memory pszUID,
			Memory pszRecurID,
			IntByReference rethNote,
			int dwFlags,
			Pointer pCtx);

	short CalReadEntry(
			int hDB,
			Memory pszUID,
			Memory pszRecurID,
			IntByReference hRetCalData,
			IntByReference pdwReserved,
			int dwFlags,
			Pointer pCtx);
	
	short CalReadRange(
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
	
	short CalGetUnappliedNotices(
			int hDB,
			Memory pszUID,
			ShortByReference pwNumNotices,
			IntByReference phRetNOTEIDs,
			IntByReference phRetUNIDs,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);

	short CalGetNewInvitations(
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
	
	short CalReadNotice(
			int hDB,
			int noteID,
			IntByReference hRetCalData,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	short CalReadNoticeUNID(
			int hDB,
			NotesUniversalNoteIdStruct unid,
			IntByReference hRetCalData,
			Pointer pReserved,
			int dwFlags,
			Pointer pCtx);
	
	short CalNoticeAction(
			int hDB,
			int noteID,
			int dwAction,
			Memory pszComments,
			NotesCalendarActionDataStruct  pExtActionInfo,
			int dwFlags,
			Pointer pCtx);
	
	short CalNoticeActionUNID(
			int hDB,
			NotesUniversalNoteIdStruct unid,
			int dwAction,
			Memory pszComments,
			NotesCalendarActionDataStruct pExtActionInfo,
			int dwFlags,
			Pointer pCtx);
	
	short CalEntryAction(
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
	short LZ1Compress(
	        Pointer sin,
	        Pointer sout,
	        int insize,
	        int hCompHT,
	        IntByReference poutsize
	        );

	@UndocumentedAPI
	short LZ1Decompress(Pointer sin, Pointer SoutUncompressed, int outsize);
	
	short OOOStartOperation(
			Pointer pMailOwnerName,
			Pointer pHomeMailServer,
			int bHomeMailServer,
			int hMailFile,
			IntByReference hOOOContext,
			PointerByReference pOOOOContext);
	short OOOEndOperation(int hOOContext, Pointer pOOOContext);
	
	short NSFDbItemDefTableExt(
			int hDB,
			IntByReference retItemNameTable);
	
	@UndocumentedAPI
	short NSFDbGetTcpHostName(
	        int hDB,                                                        /* Database Handle           */ 
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
			IntByReference hResponseText);
	
	short NSFGetServerStats(
			Memory serverName,
			Memory facility,
			Memory statName,
			IntByReference rethTable,
			IntByReference retTableSize);

	short DXLImport(
			int hDXLImport,
			NotesCallbacks.XML_READ_FUNCTION pDXLReaderFunc,
			int hDB,
			Pointer pImAction);
	
	short DXLExportACL(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION pDXLWriteFunc,
			int hDB,
			Pointer pExAction);
	
	short DXLExportDatabase(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			int hDB,
			Pointer pExAction);
	
	short DXLExportIDTable(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			int hDB,
			int hIDTable,
			Pointer pExAction);
	
	short DXLExportNote(
			int  hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			int hNote,
			Pointer pExAction);
	
	short MIMEOpenDirectory(
			int hNote,
			IntByReference phMIMEDir);

	short MIMEFreeDirectory(
			int hMIMEDir);
	
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
			IntByReference phValue,
			IntByReference pdwValueLen);

	boolean MIMEEntityIsDiscretePart(
			Pointer pMimeEntity);

	boolean MIMEEntityIsMessagePart(
			Pointer pMIMEEntity);

	boolean MIMEEntityIsMultiPart(
			Pointer pMimeEntity);

	short MIMEFreeEntityDataObject(
			int hNote,
			Pointer pEntity);

	short MIMEGetDecodedEntityData(
			int hNote,
			Pointer pMimeEntity,
			int dwEncodedOffset,
			int dwChunkLen,
			IntByReference phData,
			IntByReference pdwDecodedDataLen,
			IntByReference pdwEncodedDataLen);

	short MIMEGetEntityData(
			int hNote,
			Pointer pME,
			short wDataType,
			int dwOffset,
			int dwRetBytes,
			IntByReference phData,
			IntByReference pdwDataLen);
	
	short MIMEGetEntityPartFlags(
			int hNote,
			Pointer pEntity,
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
			int hNote,
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
			int hNote,
			Memory pchItemName,
			short wItemNameLen,
			int dwFlags,
			Pointer hMIMEStream);

	short MIMEStreamOpen(
			int hNote,
			Memory pchItemName,
			short wItemNameLen,
			int dwOpenFlags,
			PointerByReference rethMIMEStream);

	short MIMEConvertCDParts(
			int hNote,
			boolean bCanonical,
			boolean bIsMIME,
			Pointer hCC);

	@UndocumentedAPI
	short NSFDbNamedObjectEnum(int hDB, NotesCallbacks.b32_NSFDbNamedObjectEnumPROC callback, Pointer param);

	@UndocumentedAPI
	short NSFDbGetNamedObjectID(int hDB, short NameSpace,
            Memory Name, short NameLength,
            IntByReference rtnObjectID);

	short NSFDbCreateAndCopyExtended(
			Memory srcDb,
			Memory dstDb,
			short NoteClass,
			short limit,
			int flags,
			int hNames,
			IntByReference hNewDb);
	
	short NSFDbCreateACLFromTemplate(
			long hNTF,
			long hNSF,
			Memory Manager,
			short DefaultAccess,
			IntByReference rethACL);
	
	short ACLCreate(IntByReference rethACL);

	short NSFDbCopy(
			int hSrcDB,
			int hDstDB,
			NotesTimeDateStruct.ByValue Since,
			short NoteClassMask);

	short NSFDbOpenTemplateExtended(
			Memory PathName,
			short Options,
			int hNames,
			NotesTimeDateStruct ModifiedTime,
			IntByReference rethDB,
			NotesTimeDateStruct retDataModified,
			NotesTimeDateStruct retNonDataModified);

	short NSFNoteCreateClone (int hSrcNote, IntByReference rethDstNote);

	short NSFNoteReplaceItems (int hSrcNote, int hDstNote, ShortByReference pwRetItemReplaceCount, boolean fAllowDuplicates);

	short StoredFormAddItems (int hSrcDbHandle, int hSrcNote, int hDstNote, boolean bDoSubforms, int dwFlags);

	short StoredFormRemoveItems(int hNote, int dwFlags);

	short MailNoteJitEx2(Pointer vpRunCtx, int hNote, short wMailFlags, IntByReference retdwRecipients,
			short jitflag, short wMailNoteFlags, NotesCallbacks.b32_FPMailNoteJitEx2CallBack vCallBack, Pointer vCallBackCtx);

	short MailSetSMTPMessageID(int hNote, Memory domain, Memory string, short stringLength);

	short NSFDbReopenWithFullAccess(int hDb, IntByReference hReopenedDb);

	/*	This is exactly the same as lookup name BE, except it will use the
	cache of the design collection as known to the client.  This is safe
	to use outside the client since it will map to the BE code.  Using this
	version will only be as up to date as the last time the design was
	fetched for the client.  This was added explicitly to avoid cases of
	backend code executing in their world where backend changes to design
	things was not seen immediately. */
	short DesignLookupNameFE (int hDB, short wClass, Pointer szFlagsPattern, Pointer szName,
									  	short wNameLen, int flags,
										IntByReference retNoteID, IntByReference retbIsPrivate,
										NotesCallbacks.DESIGN_COLL_OPENCLOSE_PROC OpenCloseRoutine, Pointer Ctx);

	short NSFDbGetReplHistorySummary(
			int hDb,
			int Flags,
			IntByReference rethSummary,
			IntByReference retNumEntries);

	short REGCrossCertifyID(
			int hCertCtx,
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
			IntByReference rethKfmCertCtx,
			ShortByReference retfIsHierarchical,
			ShortByReference retwFileVersion);
	
	void SECKFMFreeCertifierCtx(
			int hKfmCertCtx);

	@UndocumentedAPI
	short SECKFMMakeSafeCopy(int hKFC, short Type, short Version, Memory pFileName);

	@UndocumentedAPI
	short AssistantGetLSDataNote (int hDB, int NoteID, NotesUniversalNoteIdStruct.ByReference retUNID);

	@UndocumentedAPI
	short DesignEnum2 (int hDB,
			   short NoteClass,
			   Memory  pszFlagsPattern,
			   int dwFlags,
			   NotesCallbacks.b32_DESIGNENUMPROC proc,
			   Pointer parameters,
			   NotesCallbacks.DESIGN_COLL_OPENCLOSE_PROC openCloseRoutine,
			   Pointer ctx);

	@UndocumentedAPI
	short DesignGetNoteTable(int hDB, short NoteClass, IntByReference rethIDTable);

}
