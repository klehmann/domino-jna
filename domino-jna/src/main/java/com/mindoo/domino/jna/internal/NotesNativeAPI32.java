package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCallbacks.ABORTCHECKPROC;
import com.mindoo.domino.jna.internal.NotesCallbacks.ACLENTRYENUMFUNC;
import com.mindoo.domino.jna.internal.NotesCallbacks.OSSIGMSGPROC;
import com.mindoo.domino.jna.internal.NotesCallbacks.b32_NSFGetAllFolderChangesCallback;
import com.mindoo.domino.jna.internal.structs.NIFFindByKeyContextStruct;
import com.mindoo.domino.jna.internal.structs.NotesBlockIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesBlockIdStruct.ByValue;
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
 * Class providing C methods for 32 bit. Should be used internally by
 * the Domino JNA API methods only.
 * 
 * @author Karsten Lehmann
 */
public class NotesNativeAPI32 implements INotesNativeAPI32 {
	private static volatile INotesNativeAPI32 m_instanceWithoutCrashLogging;
	private static volatile INotesNativeAPI32 m_instanceWithCrashLogging;

	/**
	 * Gets called from {@link NotesNativeAPI#initialize()}
	 * 
	 * @param instance
	 */
	static void set(INotesNativeAPI32 instance) {
		m_instanceWithoutCrashLogging = instance;
	}
	
	/**
	 * Returns the API instance used to call native Domino C API methods for 32 bit
	 * 
	 * @return API
	 */
	public static INotesNativeAPI32 get() {
		NotesGC.ensureRunningInAutoGC();

		if (NotesNativeAPI.m_initError!=null) {
			if (NotesNativeAPI.m_initError instanceof RuntimeException)
				throw (RuntimeException) NotesNativeAPI.m_initError;
			else
				throw new NotesError(0, "Error initializing Domino JNA API", NotesNativeAPI.m_initError);
		}
		
		if (m_instanceWithoutCrashLogging==null)
			throw new NotesError(0, "API not initialized yet. Please call NotesNativeAPI.initialize() first. The easiest way to do this is by wrapping your code in a NotesGC.runWithAutoGC block");
		
		if (NotesGC.isLogCrashingThreadStacktrace()) {
			if (m_instanceWithCrashLogging==null) {
				m_instanceWithCrashLogging = NotesNativeAPI.wrapWithCrashStackLogging(INotesNativeAPI32.class, m_instanceWithoutCrashLogging);
			}
			return m_instanceWithCrashLogging;
		}
		else {
			return m_instanceWithoutCrashLogging;
		}
	}
	
	public native short NSFSearch(
			int hDB,
			int hFormula,
			Memory viewTitle,
			short SearchFlags,
			short NoteClassMask,
			NotesTimeDateStruct Since,
			NotesCallbacks.NsfSearchProc enumRoutine,
			Pointer EnumRoutineParameter,
			NotesTimeDateStruct retUntil);

	public native short NSFSearchExtended3 (int hDB, 
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

	public native short NSFGetFolderSearchFilter(int hViewDB, int hDataDB, int ViewNoteID, NotesTimeDateStruct Since, int Flags, IntByReference Filter);

	public native Pointer OSLockObject(int handle);
	public native boolean OSUnlockObject(int handle);
	public native short OSMemFree(int handle);
	public native short OSMemoryAllocate(int dwtype, int size, IntByReference retHandle);
	public native short OSMemGetSize(int handle, IntByReference retSize);
	public native int OSMemoryGetSize(int handle);
	public native void OSMemoryFree(int handle);
	public native short OSMemoryReallocate(int handle, int size);
	public native Pointer OSMemoryLock(int handle);
	public native boolean OSMemoryUnlock(int handle);
	public native short OSMemAlloc(
			short  BlkType,
			int  dwSize,
			IntByReference retHandle);
	
	public native short OSMemGetType(int handle);

	public native short NSFItemGetText(
			int  note_handle,
			Memory item_name,
			Memory item_text,
			short text_len);

	public native short ListAllocate(
			short ListEntries,
			short TextSize,
			int fPrefixDataType,
			IntByReference rethList,
			Memory retpList,
			ShortByReference retListSize);
	
	public native short ListAddEntry(
			int hList,
			int fPrefixDataType,
			ShortByReference pListSize,
			short EntryNumber,
			Memory Text,
			short TextSize);

	public native short ListRemoveAllEntries(
			int hList,
			int fPrefixDataType,
			ShortByReference pListSize);

	public native short NSFItemInfo(
			int  note_handle,
			Memory item_name,
			short name_len,
			NotesBlockIdStruct retbhItem,
			ShortByReference retDataType,
			NotesBlockIdStruct retbhValue,
			IntByReference retValueLength);

	public native short NSFItemInfoNext(
			int  note_handle,
			NotesBlockIdStruct.ByValue NextItem,
			Memory item_name,
			short name_len,
			NotesBlockIdStruct retbhItem,
			ShortByReference retDataType,
			NotesBlockIdStruct retbhValue,
			IntByReference retValueLength);
	
	public native short NSFItemInfoPrev(
			int  note_handle,
			NotesBlockIdStruct.ByValue  CurrItem,
			Memory item_name,
			short  name_len,
			NotesBlockIdStruct item_blockid_ptr,
			ShortByReference value_type_ptr,
			NotesBlockIdStruct value_blockid_ptr,
			IntByReference value_len_ptr);
	
	public native void NSFItemQueryEx(
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

	public native short NSFItemGetModifiedTimeByBLOCKID(
			int  hNote,
			NotesBlockIdStruct.ByValue bhItem,
			int  Flags,
			NotesTimeDateStruct retTime);

	public native short NSFItemGetTextListEntries(
			int note_handle,
			Memory item_name);

	public native short NSFItemGetTextListEntry(
			int note_handle,
			Memory item_name,
			short entry_position,
			Memory retEntry_text,
			short  text_len);
	public native short NSFItemGetModifiedTime(
			int hNote,
			Memory ItemName,
			short  ItemNameLength,
			int  Flags,
			NotesTimeDateStruct retTime);
	public native short NSFItemSetText(
			int hNote,
			Memory ItemName,
			Memory ItemText,
			short TextLength);
	public native short NSFItemSetTextSummary(
			int hNote,
			Memory ItemName,
			Memory ItemText,
			short TextLength,
			boolean summary);
	public native boolean NSFItemGetTime(
			int  note_handle,
			Memory td_item_name,
			NotesTimeDateStruct td_item_value);
	public native short NSFItemSetTime(
			int  note_handle,
			Memory td_item_name,
			NotesTimeDateStruct td_item_ptr);
	public native boolean NSFItemGetNumber(
			int hNote,
			Memory ItemName,
			DoubleByReference retNumber);
	public native int NSFItemGetLong(
			int note_handle,
			Memory number_item_name,
			int number_item_default);
	public native short NSFItemSetNumber(
			int  hNote,
			Memory ItemName,
			Memory Number);
	public native short NSFItemConvertToText(
			int note_handle,
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
			int note_handle,
			Memory item_name,
			short name_len);
	public native short NSFItemDeleteByBLOCKID(int note_handle, NotesBlockIdStruct.ByValue item_blockid);
	public native short NSFItemAppend(
			int note_handle,
			short item_flags,
			Memory item_name,
			short name_len,
			short  item_type,
			Pointer item_value,
			int value_len);
	public native short NSFItemAppendByBLOCKID(
			int note_handle,
			short item_flags,
			Memory item_name,
			short name_len,
			NotesBlockIdStruct.ByValue value_bid,
			int value_len,
			NotesBlockIdStruct item_bid_ptr);
	//valuePtr value without datatype WORD
	public native short NSFItemModifyValue (int hNote, NotesBlockIdStruct.ByValue bhItem, short ItemFlags, short DataType,
			Pointer valuePtr, int valueLength);

	public native void NSFNoteGetInfo(int hNote, short type, Memory retValue);
	public native void NSFNoteSetInfo(int hNote, short type, Pointer value);
	public native short NSFNoteCopy(
			int note_handle_src,
			IntByReference note_handle_dst_ptr);

	public native short NSFNoteUpdateExtended(int hNote, int updateFlags);
	public native short NSFNoteCreate(int db_handle, IntByReference note_handle);
	public native short NSFNoteOpen(int hDB, int noteId, short openFlags, IntByReference rethNote);
	public native short NSFNoteOpenExt(int hDB, int noteId, int flags, IntByReference rethNote);
	public native short NSFNoteOpenByUNID(
			int hDB,
			NotesUniversalNoteIdStruct pUNID,
			short  flags,
			IntByReference rethNote);
	public native short NSFNoteOpenByUNIDExtended(int hDB, NotesUniversalNoteIdStruct pUNID, int flags, IntByReference rtn);
	public native short NSFNoteClose(int hNote);
	public native short NSFNoteVerifySignature(
			int  hNote,
			Memory SignatureItemName,
			NotesTimeDateStruct retWhenSigned,
			Memory retSigner,
			Memory retCertifier);
	public native short NSFNoteContract(int hNote);
	public native short NSFNoteExpand(int hNote);
	public native short NSFNoteSign(int hNote);
	public native short NSFNoteSignExt3(int hNote, 
			int hKFC,
			Memory SignatureItemName,
			short ItemCount, int hItemIDs, 
			int Flags, int Reserved,
			Pointer pReserved);
	public native short NSFNoteOpenSoftDelete(int hDB, int NoteID, int Reserved, IntByReference rethNote);
	public native short NSFNoteHardDelete(int hDB, int NoteID, int Reserved);
	public native short NSFNoteDeleteExtended(int hDB, int NoteID, int UpdateFlags);
	public native short NSFNoteDetachFile(int note_handle, NotesBlockIdStruct.ByValue item_blockid);
	public native boolean NSFNoteIsSignedOrSealed(int note_handle, ByteByReference signed_flag_ptr, ByteByReference sealed_flag_ptr);
	public native short NSFNoteUnsign(int hNote);
	public native short NSFNoteComputeWithForm(
			int  hNote,
			int  hFormNote,
			int  dwFlags,
			NotesCallbacks.b32_CWFErrorProc ErrorRoutine,
			Pointer CallersContext);
	public native short NSFNoteHasComposite(int hNote);
	public native short NSFNoteHasMIME(int hNote);
	public native short NSFNoteHasMIMEPart(int hNote);
	public native short NSFIsFileItemMimePart(int hNote, ByValue bhFileItem);
	public native short NSFIsMimePartInFile(int hNote, ByValue bhMIMEItem, Memory pszFileName, short wMaxFileNameLen);
	
	public native short NSFMimePartCreateStream(int hNote, Memory pchItemName, short wItemNameLen, short wPartType,
			int dwFlags, IntByReference phCtx);
	public native short NSFMimePartAppendStream(int hCtx, Memory pchData, short wDataLen);
	public native short NSFMimePartAppendFileToStream(int hCtx, Memory pszFilename);
	public native short NSFMimePartAppendObjectToStream(int hCtx, Memory pszAttachmentName);
	public native short NSFMimePartCloseStream(int hCtx, short bUpdate);
	public native short MIMEStreamOpen(
			int hNote,
			Memory pchItemName,
			short wItemNameLen,
			int dwOpenFlags,
			IntByReference rethMIMEStream);
	public native int MIMEStreamPutLine(
			Memory pszLine,
			int hMIMEStream);
	public native short MIMEStreamItemize(
			int hNote,
			Memory pchItemName,
			short wItemNameLen,
			int dwFlags,
			int hMIMEStream);
	public native int MIMEStreamWrite(Pointer pchData, int uiDataLen, int hMIMEStream);
	public native void MIMEStreamClose(
			int hMIMEStream);
	public native short MIMEConvertRFC822TextItemByBLOCKID(int hNote, ByValue bhItem, ByValue bhValue);

	public native short NSFNoteHasReadersField(int hNote, NotesBlockIdStruct bhFirstReadersItem);
	public native short NSFNoteCipherExtractWithCallback (int hNote, NotesBlockIdStruct.ByValue bhItem,
			int ExtractFlags, int hDecryptionCipher,
			NotesCallbacks.NoteExtractCallback pNoteExtractCallback, Pointer pParam,
			int Reserved, Pointer pReserved);
	public native short NSFNoteCopyAndEncryptExt2(
			int hSrcNote,
			int hKFC,
			short EncryptFlags,
			IntByReference rethDstNote,
			int  Reserved,
			Pointer pReserved);
	public native short NSFNoteCopyAndEncrypt(
			int hSrcNote,
			short EncryptFlags,
			IntByReference rethDstNote);
	public native short NSFNoteCipherDecrypt(
			int  hNote,
			int hKFC,
			int  DecryptFlags,
			LongByReference rethCipherForAttachments,
			int  Reserved,
			Pointer pReserved);
	public native short NSFNoteAttachFile(
			int note_handle,
			Memory item_name,
			short item_name_length,
			Memory file_name,
			Memory orig_path_name,
			short encoding_type);
	public native short NSFNoteSignHotspots(
			int hNote,
			int dwFlags,
			IntByReference retfSigned);
	public native short NSFNoteLSCompile(
			int hDb,
			int hNote,
			int dwFlags);
	public native short NSFNoteLSCompileExt(
			int hDb,
			int hNote,
			int dwFlags,
			NotesCallbacks.LSCompilerErrorProc pfnErrProc,
			Pointer pCtx);
	@Override public native short NSFNoteCheck(
			int hNote);

	public native short NSFDbNoteLock(
			int hDB,
			int NoteID,
			int Flags,
			Memory pLockers,
			IntByReference rethLockers,
			IntByReference retLength);
	
	public native short NSFDbNoteUnlock(
			int hDB,
			int NoteID,
			int Flags);

	public native short NSFNoteOpenWithLock(int hDB, int NoteID, int LockFlags, int OpenFlags, Memory pLockers,
			IntByReference rethLockers, IntByReference retLength, IntByReference rethNote);
	
	public native short NSFItemCopy(int note_handle, NotesBlockIdStruct.ByValue item_blockid);
	public native short NSFItemCopyAndRename(int hNote, ByValue bhItem, Memory pszNewItemName);

	public native short IDCreateTable (int alignment, IntByReference rethTable);
	public native short IDDestroyTable(int hTable);
	public native short IDInsert (int hTable, int id, IntByReference retfInserted);
	public native short IDDelete (int hTable, int id, IntByReference retfDeleted);
	public native boolean IDScan (int hTable, boolean fFirst, IntByReference retID);
	public native boolean IDScanBack (int hTable, boolean fLast, IntByReference retID);
	public native int IDEntries (int hTable);
	public native boolean IDIsPresent (int hTable, int id);
	public native int IDTableSize (int hTable);
	public native int IDTableSizeP(Pointer pIDTable);
	public native short IDTableCopy (int hTable, IntByReference rethTable);
	public native short IDTableIntersect(int hSrc1Table, int hSrc2Table, IntByReference rethDstTable);
	public native short IDDeleteAll (int hTable);
	public native boolean IDAreTablesEqual	(int hSrc1Table, int hSrc2Table);
	public native short IDDeleteTable(int hTable, int hIDsToDelete);
	public native short IDInsertTable  (int hTable, int hIDsToAdd);
	public native short IDEnumerate(int hTable, NotesCallbacks.IdEnumerateProc Routine, Pointer Parameter);
	public native short IDInsertRange(int hTable, int IDFrom, int IDTo, boolean AddToEnd);
	public native short IDTableDifferences(int idtable1, int idtable2, IntByReference outputidtableAdds, IntByReference outputidtableDeletes, IntByReference outputidtableSame);
	public native short IDTableReplaceExtended(int idtableSrc, int idtableDest, byte flags);

	public native short NSFDbStampNotesMultiItem(int hDB, int hTable, int hInNote);
	public native short NSFDbOpen(Memory dbName, IntByReference dbHandle);
	public native short NSFDbOpenExtended (Memory PathName, short Options, int hNames, NotesTimeDateStruct ModifiedTime, IntByReference rethDB, NotesTimeDateStruct retDataModified, NotesTimeDateStruct retNonDataModified);
	public native short NSFDbGenerateOID(int hDB, NotesOriginatorIdStruct retOID);
	public native short NSFDbClose(int dbHandle);
	public native int NSFDbGetOpenDatabaseID(int hDBU);
	public native short NSFDbReopen(int hDB, IntByReference rethDB);
	public native short NSFDbLocateByReplicaID(
			int  hDB,
			NotesTimeDateStruct ReplicaID,
			Memory retPathName,
			short PathMaxLen);
	public native short NSFDbModifiedTime(
			int hDB,
			NotesTimeDateStruct retDataModified,
			NotesTimeDateStruct retNonDataModified);
	public native short NSFDbIDGet(int hDB, NotesTimeDateStruct retDbID);
	public native short NSFDbReplicaInfoGet(
			int  hDB,
			NotesDbReplicaInfoStruct retReplicationInfo);
	public native short NSFDbReplicaInfoSet(
			int  hDB,
			NotesDbReplicaInfoStruct ReplicationInfo);
	public native short NSFDbGetModifiedNoteTable(int hDB, short NoteClassMask, NotesTimeDateStruct.ByValue Since, NotesTimeDateStruct retUntil, IntByReference rethTable);
	public native short NSFDbGetNotes(
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
	public native short NSFDbGetMultNoteInfo(
			int  hDb,
			short  Count,
			short  Options,
			int  hInBuf,
			IntByReference retSize,
			IntByReference rethOutBuf);
	public native short NSFDbGetNoteInfoExt(
			int  hDB,
			int  NoteID,
			NotesOriginatorIdStruct retNoteOID,
			NotesTimeDateStruct retModified,
			ShortByReference retNoteClass,
			NotesTimeDateStruct retAddedToFile,
			ShortByReference retResponseCount,
			IntByReference retParentNoteID);
	public native short NSFDbGetMultNoteInfoByUNID(
			int hDB,
			short Count,
			short Options,
			int hInBuf,
			IntByReference retSize,
			IntByReference rethOutBuf);
	public native short NSFDbSign(int hDb, short noteclass);
	public native short NSFDbGetOptionsExt(int hDB, Memory retDbOptions);
	public native short NSFDbSetOptionsExt(int hDB, Memory dbOptions, Memory mask);
	public native void NSFDbAccessGet(int hDB, ShortByReference retAccessLevel, ShortByReference retAccessFlag);
	public native short NSFDbGetBuildVersion(int hDB, ShortByReference retVersion);
	public native short NSFDbGetMajMinVersion(int hDb, NotesBuildVersionStruct retBuildVersion);
	public native short NSFDbReadObject(
			int hDB,
			int ObjectID,
			int Offset,
			int Length,
			IntByReference rethBuffer);
	
	public native short NSFDbAllocObject(int hDB, int dwSize, short Class, short Privileges, IntByReference retObjectID);
	public native short NSFDbAllocObjectExtended2(int cDB, int size, short noteClass, short privs, short type,
			IntByReference rtnRRV);
	public native short NSFDbWriteObject(int hDB, int ObjectID, int hBuffer, int Offset, int Length);
	public native short NSFDbFreeObject(int hDB, int ObjectID);
	public native short NSFDbReallocObject(int hDB, int ObjectID, int NewSize);
	
	public native short NSFDbGetObjectSize(
			int hDB,
			int ObjectID,
			short ObjectType,
			IntByReference retSize,
			ShortByReference retClass,
			ShortByReference retPrivileges);
	public native short NSFItemAppendObject(int hNote, short ItemFlags, Memory Name, short NameLength, ByValue bhValue,
			int ValueLength, int fDealloc);
	public native short NSFDbGetSpecialNoteID(
			int hDB,
			short Index,
			IntByReference retNoteID);
	public native short NSFDbClearReplHistory(int hDb, int dwFlags);
	public native void NSFDbPathGet(
			int hDB,
			Memory retCanonicalPathName,
			Memory retExpandedPathName);
	public native short NSFDbIsRemote(int hDb);
	public native short NSFDbHasFullAccess(int hDb);
	
	public native short NSFDbSpaceUsage(int dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes);
	public native short NSFDbSpaceUsageScaled (int dbHandle, IntByReference retAllocatedBytes, IntByReference retFreeBytes, IntByReference retGranularity);
	public native short NSFDbDeleteNotes(int  hDB, int  hTable, Memory retUNIDArray);
	public native short NSFDbIsLocallyEncrypted(int hDB, IntByReference retVal);
	public native short NSFDbInfoGet(
			int hDB,
			Pointer retBuffer);
	public native short NSFDbInfoSet(
			int hDB,
			Pointer Buffer);
	
	public native short NSFHideDesign(int hdb1, int hdb2, int param3, int param4);

	public native short NSFBuildNamesList(Memory UserName, int dwFlags, IntByReference rethNamesList);

	public native short CreateNamesListFromGroupNameExtend(Memory pszServerName, Memory pTarget, IntByReference rethNames);
	
	public native short CreateNamesListFromNames(short cTargets, Pointer ptrArrTargets, IntByReference rethNames);
	
	public native short CreateNamesListFromNamesExtend(Memory pszServerName, short cTargets, Pointer ptrArrTargets, IntByReference rethNames);
	
//	public native short CreateNamesListFromSessionID(Memory pszServerName, SESSIONID SessionId, IntByReference rtnhNames);
	
	public native short CreateNamesListFromSingleName(Memory pszServerName, short fDontLookupAlternateNames,
			Pointer pLookupFlags, Memory pTarget, IntByReference rethNames);
	
	public native short CreateNamesListUsingLookupName(Memory pszServerName,Pointer pLookupFlags, Memory pTarget,
			IntByReference rethNames);

	public native short NIFReadEntries(int hCollection, NotesCollectionPositionStruct IndexPos, short SkipNavigator, int SkipCount, short ReturnNavigator, int ReturnCount, int ReturnMask, IntByReference rethBuffer,
			ShortByReference retBufferLength, IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned, ShortByReference retSignalFlags);
	public native short NIFReadEntriesExt(int hCollection,
			NotesCollectionPositionStruct CollectionPos,
			short SkipNavigator, int SkipCount,
			short ReturnNavigator, int ReturnCount, int ReturnMask,
			NotesTimeDateStruct DiffTime, int DiffIDTable, int ColumnNumber, int Flags,
			IntByReference rethBuffer, ShortByReference retBufferLength,
			IntByReference retNumEntriesSkipped, IntByReference retNumEntriesReturned,
			ShortByReference retSignalFlags, NotesTimeDateStruct retDiffTime,
			NotesTimeDateStruct retModifiedTime, IntByReference retSequence);
	public native void NIFGetLastModifiedTime(int hCollection, NotesTimeDateStruct retLastModifiedTime);
	public native void NIFGetLastAccessedTime(int hCollection, NotesTimeDateStruct retLastModifiedTime);
	public native void NIFGetNextDiscardTime(int hCollection, NotesTimeDateStruct retLastModifiedTime);
	
	public native short NIFFindByKeyExtended2 (int hCollection, Memory keyBuffer,
			int findFlags,
			int returnFlags,
			NotesCollectionPositionStruct retIndexPos,
			IntByReference retNumMatches,
			ShortByReference retSignalFlags,
			IntByReference rethBuffer,
			IntByReference retSequence);
	public native short NIFFindByKeyExtended3 (int hCollection,
			Memory keyBuffer, int findFlags,
			int returnFlags,
			NotesCollectionPositionStruct retIndexPos,
			IntByReference retNumMatches, ShortByReference retSignalFlags,
			IntByReference rethBuffer, IntByReference retSequence,
			NotesCallbacks.NIFFindByKeyProc NIFFindByKeyCallback, NIFFindByKeyContextStruct Ctx);
	public native short NIFFindByKey(int hCollection, Memory keyBuffer, short findFlags, NotesCollectionPositionStruct retIndexPos, IntByReference retNumMatches);
	public native short NIFFindByName(int hCollection, Memory name, short findFlags, NotesCollectionPositionStruct retIndexPos, IntByReference retNumMatches);
	public native short NIFGetCollation(int hCollection, ShortByReference retCollationNum);
	public native short NIFSetCollation(int hCollection, short CollationNum);
	public native short NIFUpdateCollection(int hCollection);
	public native short NIFIsNoteInView(int hCollection, int noteID, IntByReference retIsInView);
	public native boolean NIFIsUpdateInProgress(int hCollection);
	public native short NIFGetIDTableExtended(int hCollection, short navigator, short Flags, int hIDTable);
	public native boolean NIFCollectionUpToDate(int hCollection);
    public native boolean NIFSetCollectionInfo (int hCollection, Pointer SessionID,
            int hUnreadList, int hCollapsedList, int hSelectedList);
    public native short NIFUpdateFilters (int hCollection, short ModifyFlags);
    public native boolean NIFIsTimeVariantView(int hCollection);
	public native short NIFCloseCollection(int hCollection);
	public native short NIFLocateNote (int hCollection, NotesCollectionPositionStruct indexPos, int noteID);
	public native short NIFFindDesignNoteExt(int hFile, Memory name, short noteClass, Memory pszFlagsPattern, IntByReference retNoteID, int Options);
	public native short NIFOpenCollection(int hViewDB, int hDataDB, int ViewNoteID, short OpenFlags, int hUnreadList, IntByReference rethCollection, IntByReference rethViewNote, Memory retViewUNID,
			IntByReference rethCollapsedList, IntByReference rethSelectedList);
	public native short NIFOpenCollectionWithUserNameList (int hViewDB, int hDataDB,
			int ViewNoteID, short OpenFlags,
			int hUnreadList,
			IntByReference rethCollection,
			IntByReference rethViewNote, Memory retViewUNID,
			IntByReference rethCollapsedList,
			IntByReference rethSelectedList,
			int nameList);
	public native short NIFGetCollectionData(
			int hCollection,
			IntByReference rethCollData);
	public native short NIFGetCollectionDocCountLW(int hCol, IntByReference pDocct);
	
	public native short NSFTransactionBegin(int hDB, int flags);
	public native short NSFTransactionCommit(int hDB, int flags);
	public native short NSFTransactionRollback(int hDB);

	//backup APIs
	public native short NSFDbGetLogInfo(int hDb, int Flags, ShortByReference LOGGED, NotesUniversalNoteIdStruct LogID,
			NotesUniversalNoteIdStruct DbIID, IntByReference LogExtent);
	public native short NSFBackupStart(int hDB, int Flags, IntByReference BackupContext, IntByReference FileSizeLow,
			IntByReference FileSizeHigh);
	public native short NSFBackupStop(int hDB, int BackupContext);
	public native short NSFBackupEnd(int hDB, int BackupContext, int Options);
	public native short NSFBackupGetChangeInfoSize(int hDB, int hBackupContext, int Flags, IntByReference InfoSizeLow,
			IntByReference InfoSizeHigh);
	public native short NSFBackupStartApplyChangeInfo(IntByReference ApplyInfoContext, Memory CopyFilePath, int Flags,
			int InfoSizeLow, int InfoSizeHigh);
	public native short NSFBackupGetNextChangeInfo(int hDB, int hBackupContext, int Flags, Memory Buffer, int BufferSize,
			IntByReference FilledSize);
	public native short NSFBackupApplyNextChangeInfo(int ApplyInfoContext, int Flags, Memory Buffer, int BufferSize);
	public native short NSFBackupEndApplyChangeInfo(int ApplyInfoContext, int Flags);

	public native short AgentDelete (int hAgent); /* delete agent */
	public native boolean IsRunAsWebUser(int hAgent);
	public native short AgentOpen (int hDB, int AgentNoteID, IntByReference rethAgent);
	public native void AgentClose (int hAgent);
	public native short AgentCreateRunContext (int hAgent,
			 Pointer pReserved,
			 int dwFlags,
			 IntByReference rethContext);
	public native short AgentCreateRunContextExt (int hAgent, Pointer pReserved, int pOldContext, int dwFlags, IntByReference rethContext);
	public native short AgentSetDocumentContext(int hAgentCtx, int hNote);
	public native short AgentSetTimeExecutionLimit(int hAgentCtx, int timeLimit);
	public native boolean AgentIsEnabled(int hAgent);
	public native void SetParamNoteID(int hAgentCtx, int noteId);
	public native short AgentSetUserName(int hAgentCtx, int hNameList);
	public native short AgentRedirectStdout(int hAgentCtx, short redirType);
	public native void AgentQueryStdoutBuffer(int hAgentCtx, IntByReference retHdl, IntByReference retSize);
	public native void AgentDestroyRunContext (int hAgentCtx);
	public native short AgentRun (int hAgent,
			int hAgentCtx,
		    int hSelection,
			int dwFlags);
	public native short AgentSetHttpStatusCode(int hAgentCtx, int httpStatus);
	public native short ClientRunServerAgent(int hdb, int nidAgent, int nidParamDoc, int bForeignServer,
			int bSuppressPrintToConsole);

	public native short FTIndex(int hDB, short options, Memory stopFile, NotesFTIndexStatsStruct retStats);
	public native short ClientFTIndexRequest(int hDB);
	public native short FTDeleteIndex(int hDB);
	public native short FTGetLastIndexTime(int hDB, NotesTimeDateStruct retTime);
	public native short FTOpenSearch(IntByReference rethSearch);
	public native short FTCloseSearch(int hSearch);
	public native short FTSearch(
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

	public native short FTSearchExt(int hDB, IntByReference phSearch, int hColl, Memory query, int options, short limit,
			int hRefineIDTable, IntByReference retNumDocs, IntByReference rethStrings, IntByReference rethResults,
			IntByReference retNumHits, int start, int count, short arg, int hNames);
	
	public native short NSFFormulaCompile(
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
	public native short NSFFormulaDecompile(
			Pointer pFormulaBuffer,
			boolean fSelectionFormula,
			IntByReference rethFormulaText,
			ShortByReference retFormulaTextLength);
	public native short NSFFormulaSummaryItem(int hFormula, Memory ItemName, short ItemNameLength);
	public native short NSFFormulaMerge(
			int hSrcFormula,
			int hDestFormula);

	public native short NSFComputeStart(
			short Flags,
			Pointer lpCompiledFormula,
			IntByReference rethCompute);
	public native short NSFComputeStop(int hCompute);
	public native short NSFComputeEvaluate(
			int  hCompute,
			int hNote,
			IntByReference rethResult,
			ShortByReference retResultLength,
			IntByReference retNoteMatchesFormula,
			IntByReference retNoteShouldBeDeleted,
			IntByReference retNoteModified);

	public native short CESCreateCTXFromNote(int hNote, IntByReference rethCESCTX);
	public native short CESGetNoSigCTX(IntByReference rethCESCTX);
	public native short CESFreeCTX(int hCESCTX);
	public native short ECLUserTrustSigner ( int hCESCtx, 
			short ECLType,
			short bSessionOnly,
			short wCapabilities,
			short wCapabilities2,
			ShortByReference retwCurrentCapabilities,
			ShortByReference retwCurrentCapabilities2);

	public native short NSFFolderGetIDTable(
			int  hViewDB,
			int hDataDB,
			int  viewNoteID,
			int  flags,
			IntByReference hTable);
	
	public native short NSFGetAllFolderChanges(int hViewDB, int hDataDB, NotesTimeDateStruct since, int flags,
			b32_NSFGetAllFolderChangesCallback Callback, Pointer Param, NotesTimeDateStruct until);

	public native short NSFGetFolderChanges(int hViewDB, int hDataDB, int viewNoteID, NotesTimeDateStruct since, int Flags,
			IntByReference addedNoteTable, IntByReference removedNoteTable);
	
	public native short FolderDocAdd(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			int  hTable,
			long  dwFlags);
	public native short FolderDocCount(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			long  dwFlags,
			LongByReference pdwNumDocs);
	public native short FolderDocRemove(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			int  hTable,
			long  dwFlags);
	public native short FolderDocRemoveAll(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			long  dwFlags);
	public native short FolderMove(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			int  hParentDB,
			int  ParentNoteID,
			long  dwFlags);
	public native short FolderRename(
			int  hDataDB,
			int  hFolderDB,
			int  FolderNoteID,
			Memory pszName,
			short  wNameLen,
			long  dwFlags);

	public native short NSFProfileOpen(
			int hDB,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength,
			short CopyProfile,
			IntByReference rethProfileNote);
	public native short NSFProfileUpdate(
			int hProfile,
			Memory ProfileName,
			short ProfileNameLength,
			Memory UserName,
			short UserNameLength);
	public native short NSFProfileSetField(
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
	public native short NSFProfileDelete(int hDB, Memory ProfileName, short ProfileNameLength, Memory UserName,
			short UserNameLength);

	public native short SECKFMOpen(IntByReference phKFC, Memory pIDFileName, Memory pPassword,
			int Flags, int Reserved, Pointer pReserved);
	public native short SECKFMClose(IntByReference phKFC, int Flags, int Reserved, Pointer pReserved);
	public native short SECidfGet(Memory pUserName, Memory pPassword, Memory pPutIDFileHere,
			IntByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved);
	public native short SECidfPut(Memory pUserName, Memory pPassword, Memory pIDFilePath,
			IntByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved);
	public native short SECidfSync( Memory pUserName, Memory pPassword, Memory pIDFilePath,
			IntByReference phKFC, Memory pServerName, int dwReservedFlags, short wReservedType,
			Pointer pReserved, IntByReference retdwFlags);
	public native short SECTokenGenerate(
			Memory ServerName,
			Memory OrgName,
			Memory ConfigName,
			Memory UserName,
			NotesTimeDateStruct Creation,
			NotesTimeDateStruct Expiration,
			IntByReference retmhToken,
			int dwReserved,
			Pointer vpReserved);
	public native void SECTokenFree(IntByReference mhToken);

	public native short SchFreeTimeSearch(
			NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate,
			short fFindFirstFit,
			int dwReserved,
			NotesTimeDatePairStruct pInterval,
			short Duration,
			Pointer pNames,
			IntByReference rethRange);

	public native short SchRetrieve(
			NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate,
			int dwOptions,
			NotesTimeDatePairStruct pInterval,
			Pointer pNames,
			IntByReference rethCntnr,
			Pointer mustBeNull1,
			Pointer mustBeNull2,
			Pointer mustBeNull3);

	public native short SchSrvRetrieveExt(Pointer pClientNames, NotesUniversalNoteIdStruct pApptUnid,
			NotesTimeDateStruct pApptOrigDate, int dwOptions, NotesTimeDatePairStruct pInterval, Pointer pNames,
			Pointer pDetails, Pointer piCalList, Memory pszProxyUserName, Memory pszProxyPassword,
			IntByReference rethCntnr);
	
	public native void SchContainer_Free(int hCntnr);
	public native short SchContainer_GetFirstSchedule(
			int hCntnr,
			IntByReference rethObj,
			Memory retpSchedule);
	public native short Schedule_Free(int hCntnr, int hSched);
	public native short SchContainer_GetNextSchedule(
			int hCntnr,
			int hCurSchedule,
			IntByReference rethNextSchedule,
			Memory retpNextSchedule);
	public native short Schedule_ExtractFreeTimeRange(
			int hCntnr,
			int hSchedObj,
			NotesUniversalNoteIdStruct punidIgnore,
			short fFindFirstFit,
			short wDuration,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethRange);
	public native short Schedule_ExtractBusyTimeRange(
			int hCntnr,
			int hSchedObj,
			NotesUniversalNoteIdStruct punidIgnore,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethRange,
			IntByReference rethMoreCtx);
	public native short Schedule_ExtractMoreBusyTimeRange(
			int hCntnr,
			int hMoreCtx,
			NotesUniversalNoteIdStruct punidIgnore,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethRange,
			IntByReference rethMore);
	public native short Schedule_ExtractSchedList(
			int hCntnr,
			int hSchedObj,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethSchedList,
			IntByReference rethMore);
	public native short Schedule_ExtractMoreSchedList(
			int hCntnr,
			int hMoreCtx,
			NotesTimeDatePairStruct pInterval,
			IntByReference retdwSize,
			IntByReference rethSchedList,
			IntByReference rethMore);
	public native short Schedule_Access(
			int hCntnr,
			int hSched,
			PointerByReference pretSched);
	public native short NSGetServerClusterMates(
			Memory pServerName,
			int dwFlags,
			IntByReference phList);
	public native short NSPingServer(
			Memory pServerName,
			IntByReference pdwIndex,
			IntByReference phList);

	public native short HTMLCreateConverter(IntByReference phHTML);
	public native short HTMLDestroyConverter(int hHTML);
	public native short HTMLSetHTMLOptions(int hHTML, StringArray optionList);
	public native short HTMLConvertItem(
			int hHTML,
			int hDB,
			int hNote,
			Memory pszItemName);
	public native short HTMLConvertNote(
			int hHTML,
			int hDB,
			int hNote,
			int NumArgs,
			HtmlApi_UrlComponentStruct pArgs);
	public native short HTMLGetProperty(
			int hHTML,
			int PropertyType,
			Pointer pProperty);
	public native short HTMLSetProperty(
			int hHTML,
			int PropertyType,
			Memory pProperty);
	public native short HTMLGetText(
			int hHTML,
			int StartingOffset,
			IntByReference pTextLength,
			Memory pText);
	public native short HTMLGetReference(
			int hHTML,
			int Index,
			IntByReference phRef);
	public native short HTMLLockAndFixupReference(
			int hRef,
			Memory ppRef);
	public native short HTMLConvertElement(
			int hHTML,
			int hDB,
			int hNote,
			Memory pszItemName,
			int ItemIndex,
			int Offset);
	
	public native short CompoundTextAddCDRecords(
			int hCompound,
			Pointer pvRecord,
			int dwRecordLength);
	public native short CompoundTextAddDocLink(
			int hCompound,
			NotesTimeDateStruct.ByValue DBReplicaID,
			NotesUniversalNoteIdStruct.ByValue ViewUNID,
			NotesUniversalNoteIdStruct.ByValue NoteUNID,
			Memory pszComment,
			int dwFlags);
	public native short CompoundTextAddParagraphExt(
			int hCompound,
			int dwStyleID,
			int FontID,
			Memory pchText,
			int dwTextLen,
			Pointer pInfo);

	public native short CompoundTextAddRenderedNote(
			int hCompound,
			int hNote,
			int hFormNote,
			int dwFlags);
	public native short CompoundTextAddTextExt(
			int hCompound,
			int dwStyleID,
			int FontID,
			Memory pchText,
			int dwTextLen,
			Memory pszLineDelim,
			int dwFlags,
			Pointer pInfo);
	public native short CompoundTextAssimilateFile(
			int hCompound,
			Memory pszFileName,
			int dwFlags);
	public native short CompoundTextAssimilateItem(
			int hCompound,
			int hNote,
			Memory pszItemName,
			int dwFlags);
	public native short CompoundTextAssimilateBuffer(int hBuffer, int bufferLength, int flags);
	public native short CompoundTextClose(
			int hCompound,
			IntByReference phReturnBuffer,
			IntByReference pdwReturnBufferSize,
			Memory pchReturnFile,
			short wReturnFileNameSize);
	public native short CompoundTextCreate(
			int hNote,
			Memory pszItemName,
			IntByReference phCompound);
	public native short CompoundTextDefineStyle(
			int hCompound,
			Memory pszStyleName,
			NotesCompoundStyleStruct pDefinition,
			IntByReference pdwStyleID);
	public native void CompoundTextDiscard(
			int hCompound);

	public native short DesignRefresh(
			Memory Server,
			int hDB,
			int dwFlags,
			ABORTCHECKPROC AbortCheck,
			OSSIGMSGPROC MessageProc);
	
	public native short NSFDbReadACL(
			int hDB,
			IntByReference rethACL);
	
	public native short ACLEnumEntries(int hACL, ACLENTRYENUMFUNC EnumFunc, Pointer EnumFuncParam);
	
	public native short ACLGetPrivName(int hACL, short PrivNum, Memory retPrivName);
	
	public native short NSFDbStoreACL(
			int hDB,
			int hACL,
			int ObjectID,
			short Method);
	
	public native short ACLLookupAccess(
			int hACL,
			Pointer pNamesList,
			ShortByReference retAccessLevel,
			Memory retPrivileges,
			ShortByReference retAccessFlags,
			IntByReference rethPrivNames);
	
	public native short ACLSetAdminServer(
			int hList,
			Memory ServerName);

	public native short ACLAddEntry(int hACL, Memory name, short AccessLevel, Memory privileges, short AccessFlags);

	public native short ACLDeleteEntry(int hACL, Memory name);
	
	public native short ACLSetFlags(
			int hACL,
			int Flags);
	
	public native short ACLGetFlags(
			int hACL,
			IntByReference retFlags);

	public native short ACLSetPrivName(int hACL, short PrivNum, Memory privName);
	
	@Override
	public native short ACLUpdateEntry(int hACL, Memory name, short updateFlags, Memory newName, short newAccessLevel,
			Memory newPrivileges, short newAccessFlags);
	
	public native short NSFSearchStartExtended(int hDB, int formula, int filter,
			int filterflags, NotesUniversalNoteIdStruct ViewUNID, Memory ViewTitle, 
			int queue, int flags, int flags1, int flags2, int flags3, int flags4, 
			short noteClass, short auxclass, short granularity, 
			NotesTimeDateStruct.ByValue since, NotesTimeDateStruct rtnuntil, 
			IntByReference rtnhandle);

	public native short QueueCreate(IntByReference qhandle);
	
	public native short QueueGet(int qhandle, IntByReference sehandle);

	public native short NSFSearchStop(int shandle);
	
	public native short QueueDelete(int qhandle);
	
	public native short NSFDbModeGet(int hDB, ShortByReference retMode);
	
	public native short NSFDbLock(int hDb);
	public native void NSFDbUnlock(int hDb, ShortByReference statusInOut);
	
	public native short CalCreateEntry(int hDB, Memory pszCalEntry, int dwFlags, IntByReference hRetUID, Pointer pCtx);
	
	public native short CalUpdateEntry(int hDB, Memory pszCalEntry, Memory pszUID, Memory pszRecurID, Memory pszComments,
			int dwFlags, Pointer pCtx);
	
	public native short CalGetUIDfromNOTEID(int hDB, int noteid, Memory pszUID, short wLen, Pointer pReserved, int dwFlags,
			Pointer pCtx);
	
	public native short CalGetUIDfromUNID(int hDB, NotesUniversalNoteIdStruct unid, Memory pszUID, short wLen,
			Pointer pReserved, int dwFlags, Pointer pCtx);
	
	public native short CalOpenNoteHandle(int hDB, Memory pszUID, Memory pszRecurID, IntByReference rethNote, int dwFlags,
			Pointer pCtx);
	
	public native short CalReadEntry(int hDB, Memory pszUID, Memory pszRecurID, IntByReference hRetCalData,
			IntByReference pdwReserved, int dwFlags, Pointer pCtx);
	
	public native short CalReadRange(int hDB, NotesTimeDateStruct.ByValue tdStart, NotesTimeDateStruct.ByValue tdEnd, int dwViewSkipCount,
			int dwMaxReturnCount, int dwReturnMask, int dwReturnMaskExt, Pointer pFilterInfo,
			IntByReference hRetCalData, ShortByReference retCalBufferLength, IntByReference hRetUIDData,
			IntByReference retNumEntriesProcessed, ShortByReference retSignalFlags, int dwFlags, Pointer pCtx);
	
	public native short CalGetUnappliedNotices(int hDB, Memory pszUID, ShortByReference pwNumNotices,
			IntByReference phRetNOTEIDs, IntByReference phRetUNIDs, Pointer pReserved, int dwFlags, Pointer pCtx);
	
	public native short CalGetNewInvitations(int hDB, NotesTimeDateStruct ptdStart, Memory pszUID,
			NotesTimeDateStruct ptdSince, NotesTimeDateStruct ptdretUntil, ShortByReference pwNumInvites,
			IntByReference phRetNOTEIDs, IntByReference phRetUNIDs, Pointer pReserved, int dwFlags, Pointer pCtx);
	
	public native short CalReadNotice(int hDB, int noteID, IntByReference hRetCalData, Pointer pReserved, int dwFlags,
			Pointer pCtx);
	
	public native short CalReadNoticeUNID(int hDB, NotesUniversalNoteIdStruct unid, IntByReference hRetCalData,
			Pointer pReserved, int dwFlags, Pointer pCtx);
	
	public native short CalNoticeAction(int hDB, int noteID, int dwAction, Memory pszComments,
			NotesCalendarActionDataStruct pExtActionInfo, int dwFlags, Pointer pCtx);
	
	public native short CalNoticeActionUNID(int hDB, NotesUniversalNoteIdStruct unid, int dwAction, Memory pszComments,
			NotesCalendarActionDataStruct pExtActionInfo, int dwFlags, Pointer pCtx);
	
	public native short CalEntryAction(int hDB, Memory pszUID, Memory pszRecurID, int dwAction, int dwRange,
			Memory pszComments, NotesCalendarActionDataStruct pExtActionInfo, int dwFlags, Pointer pCtx);
	
	public native short Schedule_GetFirstDetails(int hCntnr, int hSchedObj, IntByReference rethDetailObj,
			PointerByReference retpDetail);
	
	public native short Schedule_GetNextDetails(int hCntnr, int hDetailObj, IntByReference rethNextDetailObj,
			PointerByReference retpNextDetail);
	
	public native short LZ1Compress(Pointer sin, Pointer sout, int insize, int hCompHT, IntByReference poutsize);
	public native short LZ1Decompress(Pointer sin, Pointer SoutUncompressed, int outsize);
	
	public native short OOOStartOperation(Pointer pMailOwnerName, Pointer pHomeMailServer, int bHomeMailServer, int hMailFile,
			IntByReference hOOOContext, PointerByReference pOOOOContext);
	public native short OOOEndOperation(int hOOContext, Pointer pOOOContext);
	
	public native short NSFDbItemDefTableExt(
			int hDB,
			IntByReference retItemNameTable);
	
	public native short NSFDbGetTcpHostName(int hDB, Memory pszHostName, short wMaxHostNameLen, Memory pszDomainName,
			short wMaxDomainNameLen, Memory pszFullName, short wMaxFullNameLen);
	
	public native short NSFItemDefExtLock(
			Pointer pItemDefTable,
			NotesItemDefinitionTableLock ItemDefTableLock);

	public native short NSFItemDefExtEntries(
			NotesItemDefinitionTableLock ItemDefTableLock,
			IntByReference NumEntries);
	
	public native short NSFItemDefExtGetEntry(
			NotesItemDefinitionTableLock ItemDefTableLock,
			int ItemNum,
			ShortByReference ItemType,
			ShortByReference ItemLength,
			Pointer ItemName);
	
	public native short NSFItemDefExtUnlock(
			NotesItemDefinitionTableExt ItemDefTable,
			NotesItemDefinitionTableLock ItemDefTableLock);
	
	public native short NSFItemDefExtFree(
			NotesItemDefinitionTableExt ItemDeftable);
	
	public native short NSFRemoteConsole(Memory ServerName, Memory ConsoleCommand, IntByReference hResponseText);
	
	public native short NSFGetServerStats(Memory serverName, Memory facility, Memory statName, IntByReference rethTable,
			IntByReference retTableSize);
	
	public native short DXLImport(
			int hDXLImport,
			NotesCallbacks.XML_READ_FUNCTION pDXLReaderFunc,
			int hDB,
			Pointer pImAction);
	
	public native short DXLExportACL(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION pDXLWriteFunc,
			int hDB,
			Pointer pExAction);
	
	public native short DXLExportDatabase(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			int hDB,
			Pointer pExAction);
	
	public native short DXLExportIDTable(
			int hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			int hDB,
			int hIDTable,
			Pointer pExAction);
	
	public native short DXLExportNote(
			int  hDXLExport,
			NotesCallbacks.XML_WRITE_FUNCTION  pDXLWriteFunc,
			int hNote,
			Pointer pExAction);
}
