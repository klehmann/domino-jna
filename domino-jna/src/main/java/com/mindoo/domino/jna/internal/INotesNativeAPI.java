package com.mindoo.domino.jna.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.mindoo.domino.jna.internal.NotesCallbacks.ACLENTRYENUMFUNC;
import com.mindoo.domino.jna.internal.NotesCallbacks.NSFFORMCMDSPROC;
import com.mindoo.domino.jna.internal.NotesCallbacks.NSFFORMFUNCPROC;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.HANDLE;
import com.mindoo.domino.jna.internal.structs.NotesDbOptionsStruct;
import com.mindoo.domino.jna.internal.structs.IntlFormatStruct;
import com.mindoo.domino.jna.internal.structs.KFM_PASSWORDStruct;
import com.mindoo.domino.jna.internal.structs.NotesBlockIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesSSOTokenInfoDescStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDatePairStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.internal.structs.ReplExtensionsStruct;
import com.mindoo.domino.jna.internal.structs.ReplServStatsStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCompoundStyleStruct;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

public interface INotesNativeAPI extends Library {
	@Target(value = ElementType.METHOD)
	@Retention(value = RetentionPolicy.RUNTIME)
	@interface NativeFunctionName {
	    String name() default "";
	}

	short NotesInitExtended(int argc, Memory argvPtr);
	void NotesTerm();

	short NotesInitThread();
	void NotesTermThread();

	short OSTranslate(short translateMode, Memory in, short inLength, Memory out, short outLength);
	short OSTranslate(short translateMode, Pointer in, short inLength, Memory out, short outLength);
	int OSTranslate32(short translateMode, Memory in, int inLength, Memory out, int outLength);
	int OSTranslate32(short translateMode, Pointer in, int inLength, Memory out, int outLength);

	short OSLoadString(int hModule, short StringCode, Memory retBuffer, short BufferLength);
	short OSLoadString(long hModule, short StringCode, Memory retBuffer, short BufferLength);
	short OSPathNetConstruct(Memory PortName,
			Memory ServerName,
			Memory FileName,
			Memory retPathName);
	short OSPathNetParse(Memory PathName,
			Memory retPortName,
			Memory retServerName,
			Memory retFileName);
	void OSGetExecutableDirectory(Memory retPathName);
	void OSGetDataDirectory(Memory retPathName);
	@UndocumentedAPI
	short OSGetSystemTempDirectory(Memory retPathName, int bufferLength);
	@UndocumentedAPI
	void OSPathAddTrailingPathSep(Memory retPathName);
	short OSGetEnvironmentString(Memory variableName, Memory rethValueBuffer, short bufferLength);
	long OSGetEnvironmentLong(Memory variableName);
	@UndocumentedAPI
	short OSGetEnvironmentTIMEDATE(Memory envVariable, NotesTimeDateStruct retTd); 
	void OSSetEnvironmentVariable(Memory variableName, Memory Value);
	@UndocumentedAPI
	void OSSetEnvironmentVariableExt (Memory variableName, Memory Value, short isSoft);
	void OSSetEnvironmentInt(Memory variableName, int Value);
	@UndocumentedAPI
	void OSSetEnvironmentTIMEDATE(Memory envVariable, NotesTimeDateStruct td);
	short OSGetEnvironmentSeqNo();

	short OSMemoryAllocate(int  dwtype, int  size, IntByReference rethandle);

	/**
	 * @param handle the handle to lock
	 * @return a pointer to the locked memory
	 * @deprecated use {@link Mem#OSMemoryLock(int)} instead
	 */
	@Deprecated
	Pointer OSMemoryLock(int handle);

	/**
	 * @param handle the handle to lock
	 * @return a pointer to the locked memory
	 * @deprecated use {@link Mem#OSMemoryLock(long)} instead
	 */
	@Deprecated
	Pointer OSMemoryLock(long handle);

	/**
	 * @param handle the handle to unlock
	 * @return whether unlocking was successful
	 * @deprecated use {@link LockedMemory#close} instead
	 */
	@Deprecated
	boolean OSMemoryUnlock(int handle);

	/**
	 * @param handle the handle to unlock
	 * @return whether unlocking was successful
	 * @deprecated use {@link LockedMemory#close} instead
	 */
	@Deprecated
	boolean OSMemoryUnlock(long handle);

	/**
	 * @param handle the handle to get the size
	 * @return the size of the handle's data in memory
	 * @deprecated use {@link Mem#OSMemoryGetSize(int)} instead
	 */
	@Deprecated int OSMemoryGetSize(int handle);
	/**
	 * @param handle the handle to get the size
	 * @return the size of the handle's data in memory
	 * @deprecated use {@link Mem#OSMemoryGetSize(long)} instead
	 */
	@Deprecated int OSMemoryGetSize(long handle);

	/**
	 * @param handle the handle to free
	 * @deprecated use {@link Mem#OSMemoryFree(int)} instead
	 */
	@Deprecated void OSMemoryFree(int handle);
	/**
	 * @param handle the handle to free
	 * @deprecated use {@link Mem#OSMemoryFree(long)} instead
	 */
	@Deprecated void OSMemoryFree(long handle);

	boolean TimeLocalToGM(Memory timePtr);
	boolean TimeLocalToGM(NotesTimeStruct timePtr);
	boolean TimeGMToLocalZone (NotesTimeStruct timePtr);
	boolean TimeGMToLocal (NotesTimeStruct timePtr);
	void TimeConstant(short timeConstantType, NotesTimeDateStruct tdptr);
	int TimeExtractTicks(Memory time);
	int TimeExtractJulianDate(Memory time);
	int TimeExtractDate(Memory time);

	short ConvertTIMEDATEToText(
			IntlFormatStruct intlFormat,
			Pointer textFormat,
			NotesTimeDateStruct inputTime,
			Memory retTextBuffer,
			short textBufferLength,
			ShortByReference retTextLength);

	short ConvertTextToTIMEDATE(
			IntlFormatStruct intlFormat,
			Pointer textFormat,
			Memory text,
			short maxLength,
			NotesTimeDateStruct retTIMEDATE);

	short ListGetNumEntries(Pointer vList, int noteItem);

	short ListGetText (Pointer pList,
			boolean fPrefixDataType,
			short entryNumber,
			Memory retTextPointer,
			ShortByReference retTextLength);

	short ListGetSize(
			Pointer pList,
			int fPrefixDataType);

	short IDTableFlags (Pointer pIDTable);
	void IDTableSetFlags (Pointer pIDTable, short Flags);
	void IDTableSetTime(Pointer pIDTable, NotesTimeDateStruct Time);
	NotesTimeDateStruct IDTableTime(Pointer pIDTable);

	short DNCanonicalize(int Flags, Memory TemplateName, Memory InName, Memory OutName, short OutSize, ShortByReference OutLength);
	short DNAbbreviate(int Flags, Memory TemplateName, Memory InName, Memory OutName, short OutSize, ShortByReference OutLength);	

	short NSFGetTransLogStyle(ShortByReference LogType);
	short NSFBeginArchivingLogs();
	short NSFGetFirstLogToArchive(NotesUniversalNoteIdStruct LogID, IntByReference LogNumber, Memory LogPath);
	short NSFGetNextLogToArchive(
			NotesUniversalNoteIdStruct LogID, IntByReference LogNumber, Memory LogPath);
	short NSFDoneArchivingLog(NotesUniversalNoteIdStruct LogID, IntByReference LogSequenceNumber);
	short NSFEndArchivingLogs();
	short NSFTakeDatabaseOffline(Memory dbPath, int WaitTime, int options);
	short NSFRecoverDatabases(Memory dbNames,
			NotesCallbacks.LogRestoreCallbackFunction restoreCB,
			int Flags,
			ShortByReference errDbIndex,
			NotesTimeDatePairStruct recoveryTime);
	short NSFBringDatabaseOnline(Memory dbPath, int options);

	short NSFItemRealloc(
			NotesBlockIdStruct.ByValue item_blockid,
			NotesBlockIdStruct value_blockid_ptr,
			int value_len);

	short NSFDbCreateExtended(
			Memory pathName,
			short  DbClass,
			boolean  ForceCreation,
			short  Options,
			byte  EncryptStrength,
			long  MaxFileSize);

	@UndocumentedAPI
	short NSFDbCreateExtended4 (Memory pathName, short dbClass, 
			 boolean forceCreation, short options, int options2,
			 byte encryptStrength, int MaxFileSize,
			 Memory string1, Memory string2, 
			 short ReservedListLength, short ReservedListCount, 
			 NotesDbOptionsStruct.ByValue dbOptions, DHANDLE.ByValue hNamesList, DHANDLE.ByValue hReservedList);

	short NSFDbRename(Memory dbNameOld, Memory dbNameNew);
	short NSFDbMarkInService(Memory dbPath);
	short NSFDbMarkOutOfService(Memory dbPath);
	short NSFDbFTSizeGet(Memory dbPath, IntByReference retFTSize);
	
	@UndocumentedAPI
	short ECLGetListCapabilities(Pointer pNamesList, short ECLType, ShortByReference retwCapabilities,
			ShortByReference retwCapabilities2, IntByReference retfUserCanModifyECL);

	short SECKFMChangePassword(Memory pIDFile, Memory pOldPassword, Memory pNewPassword);
	short SECKFMGetUserName(Memory retUserName);
	short SECKFMSwitchToIDFile(Memory pIDFileName, Memory pPassword, Memory pUserName,
			short  MaxUserNameLength, int Flags, Pointer pReserved);
	short SECidvResetUserPassword(Memory pServer, Memory pUserName, Memory pPassword,
			short wDownloadCount, int ReservedFlags, Pointer pReserved);
	short SECKFMGetPublicKey(
			Memory pName,
			short Function,
			short Flags,
			IntByReference rethPubKey);
	short SECTokenValidate(
			Memory ServerName,
			Memory OrgName,
			Memory ConfigName,
			Memory TokenData,
			Memory retUsername,
			NotesTimeDateStruct.ByReference retCreation,
			NotesTimeDateStruct.ByReference retExpiration,
			int dwReserved,
			Pointer vpReserved);

	@UndocumentedAPI
	short SECidvIsIDInVault(Memory pServer, Memory pUserName);
	
	short ODSLength(short type);
	void ODSWriteMemory(
			Pointer ppDest,
			short  type,
			Pointer pSrc,
			short  iterations);
	
	void ODSReadMemory(
			Pointer ppSrc,
			short  type,
			Pointer pDest,
			short iterations);

	short MQCreate(Memory queueName, short quota, int options);
	short MQOpen(Memory queueName, int options, IntByReference retQueue);
	short MQClose(int queue, int options);
	short MQPut(int queue, short priority, Pointer buffer, short length, 
			int options);
	short MQGet(int queue, Pointer buffer, short bufLength,
			int options, int timeout, ShortByReference retMsgLength);
	short MQScan(int queue, Pointer buffer, short bufLength, 
			int options, NotesCallbacks.MQScanCallback actionRoutine,
			Pointer ctx, ShortByReference retMsgLength);

	void MQPutQuitMsg(int queue);
	boolean MQIsQuitPending(int queue);
	short MQGetCount(int queue);

	short ReplicateWithServerExt(
			Memory PortName,
			Memory ServerName,
			int Options,
			short NumFiles,
			Memory FileList,
			ReplExtensionsStruct ExtendedOptions,
			ReplServStatsStruct retStats);

	@NativeFunctionName(name="OSGetSignalHandler")
	public NotesCallbacks.OSSIGBREAKPROC OSGetBreakSignalHandler(short signalHandlerID);
	@NativeFunctionName(name="OSSetSignalHandler")
	public NotesCallbacks.OSSIGBREAKPROC OSSetBreakSignalHandler(short signalHandlerID, NotesCallbacks.OSSIGBREAKPROC routine);

	@NativeFunctionName(name="OSGetSignalHandler")
	public NotesCallbacks.OSSIGPROGRESSPROC OSGetProgressSignalHandler(short signalHandlerID);
	@NativeFunctionName(name="OSSetSignalHandler")
	public NotesCallbacks.OSSIGPROGRESSPROC OSSetProgressSignalHandler(short signalHandlerID, NotesCallbacks.OSSIGPROGRESSPROC routine);

	@NativeFunctionName(name="OSGetSignalHandler")
	public NotesCallbacks.OSSIGREPLPROC OSGetReplicationSignalHandler(short signalHandlerID);
	@NativeFunctionName(name="OSSetSignalHandler")
	public NotesCallbacks.OSSIGREPLPROC OSSetReplicationSignalHandler(short signalHandlerID, NotesCallbacks.OSSIGREPLPROC routine);

	Pointer OSGetLMBCSCLS();

	short HTMLConvertImage(
			int hHTML,
			Memory pszImageName);
	short REGGetIDInfo(
			Memory IDFileName,
			short InfoType,
			Memory OutBufr,
			short OutBufrLen,
			ShortByReference ActualLen);

	void CompoundTextInitStyle(NotesCompoundStyleStruct style);
	short EnumCompositeBuffer(
			NotesBlockIdStruct.ByValue ItemValue,
			int ItemValueLength,
			NotesCallbacks.ActionRoutinePtr  ActionRoutine,
			Pointer vContext);

	void NIFGetViewRebuildDir(Memory retPathName, int BufferLength);
	 
	void NSFDbInfoParse(
			Pointer Info,
			short What,
			Pointer Buffer,
			short Length);
	void NSFDbInfoModify(
			Pointer Info,
			short What,
			Pointer Buffer);

	short CalGetRecurrenceID(
			NotesTimeDateStruct.ByValue tdInput,
			Memory pszRecurID,
			short wLenRecurId);
	
	short OOOInit();
	
	short OOOTerm();

	short OOOEnable(
			Pointer pOOOContext,
			int bState);
	
	short OOOGetAwayPeriod(
			Pointer pOOOContext,
			NotesTimeDateStruct tdStartAway,
			NotesTimeDateStruct tdEndAway);
	
	short OOOGetExcludeInternet(
			Pointer pOOOContext,
			IntByReference bExcludeInternet);
	
	short OOOGetGeneralMessage(
			Pointer pOOOContext,
			Memory pGeneralMessage,
			ShortByReference pGeneralMessageLen);
	
	short OOOGetGeneralSubject(
			Pointer pOOOContext,
			Memory pGeneralSubject);
	
	short OOOGetState(
			Pointer pOOOContext,
			ShortByReference retVersion,
			ShortByReference retState);

	short OOOSetAwayPeriod(
			Pointer pOOOContext,
			NotesTimeDateStruct.ByValue tdStartAway,
			NotesTimeDateStruct.ByValue tdEndAway);
	
	short OOOSetExcludeInternet(
			Pointer pOOOContext,
			int bExcludeInternet);

	short OOOSetGeneralMessage(
			Pointer pOOOContext,
			Memory pGeneralMessage,
			short wGeneralMessageLen);
	
	short OOOSetGeneralSubject(
			Pointer pOOOContext,
			Memory pGeneralSubject,
			int bDisplayReturnDate);

	short OSGetExtIntlFormat(
			byte item,
			byte index,
			Memory buff,
			short bufSize);
	
	@UndocumentedAPI
	void DEBUGDumpHandleTable(int flags, short blkType);

	@UndocumentedAPI
	short DesignFindTemplate(Pointer designTemplateName, Pointer excludeDbPath, Pointer foundDbPath);

	@UndocumentedAPI
	short MIMEEMLExport(Memory dbName, int noteID, Memory pFileName);

	void StatTraverse(
			Memory Facility,
			Memory StatName,
			NotesCallbacks.STATTRAVERSEPROC  Routine,
			Pointer Context);
	
	void OSGetIntlSettings(
			IntlFormatStruct retIntlFormat,
			short bufferSize);
	
	@UndocumentedAPI
	short OSRunNSDExt (Memory szServerName, short flags);
	
	short DXLCreateExporter(IntByReference prethDXLExport);
	
	void DXLDeleteExporter(int hDXLExport);
	
	short DXLExportWasErrorLogged(int hDXLExport);
	
	short DXLGetExporterProperty(
			int hDXLExport,
			int prop,
			Memory retPropValue);

	short DXLSetExporterProperty(
			int hDXLExport,
			int prop,
			Memory propValue);
	
	short DXLCreateImporter(IntByReference prethDXLImport);
	
	void DXLDeleteImporter(int hDXLImport);
	
	short DXLImportWasErrorLogged(int hDXLImport);
	
	short DXLGetImporterProperty(
			int hDXLImporter,
			int prop,
			Memory retPropValue);
	
	short DXLSetImporterProperty(
			int hDXLImport,
			int prop,
			Memory propValue);

	short XSLTTransform(
			int hXSLTransform,
			NotesCallbacks.XML_READ_FUNCTION pXSL_XMLInputFunc,
			Pointer pXSL_XMLInputAction,
			NotesCallbacks.XML_READ_FUNCTION  pXSL_StylesheetInputFunc,
			Pointer pXSL_StylesheetInputAction,
			NotesCallbacks.XML_WRITE_FUNCTION  pXSL_TransformOutputFunc,
			Pointer pXSL_TransformOutputAction);
	
	short XSLTCreateTransform(
			IntByReference prethXSLTransform);
	
	short MMCreateConvControls(
			PointerByReference phCC);

	short MMDestroyConvControls(
			Pointer hCC);
	
	void MMConvDefaults(
			Pointer hCC);

	short MMGetAttachEncoding(
			Pointer hCC);
	
	void MMSetAttachEncoding(
			Pointer hCC,
			short wAttachEncoding);
	
	void MMSetDropItems(
			Pointer hCC,
			Memory pszDropItems);
	
	Pointer MMGetDropItems(
			Pointer hCC);
	
	void MMSetKeepTabs(
			Pointer hCC,
			boolean bKeepTabs);
	
	boolean MMGetKeepTabs(
			Pointer hCC);
	
	void MMSetPointSize(
			Pointer hCC,
			short wPointSize);

	short MMGetPointSize(
			Pointer hCC);
	
	short MMGetTypeFace(
			Pointer hCC);
	
	void MMSetTypeFace(
			Pointer hCC,
			short wTypeFace);

	void MMSetAddItems(
			Pointer hCC,
			Memory pszAddItems);

	Pointer MMGetAddItems(
			Pointer hCC);
	
	void MMSetMessageContentEncoding(
			Pointer hCC,
			short wMessageContentEncoding);

	short MMGetMessageContentEncoding(
			Pointer hCC);
	
	void MMSetReadReceipt(
			Pointer hCC,
			short wReadReceipt);

	short MMGetReadReceipt(
			Pointer hCC);
	
	void MMSetSkipX(
			Pointer hCC,
			boolean bSkipX);
	
	boolean MMGetSkipX(
			Pointer hCC);
	
	int MIMEStreamPutLine(
			Memory pszLine,
			Pointer hMIMEStream);

	int MIMEStreamRead(
			Memory pchData,
			IntByReference puiDataLen,
			int uiMaxDataLen,
			Pointer hMIMEStream);	
	
	int MIMEStreamRewind(
			Pointer hMIMEStream);

	int MIMEStreamWrite(
			Memory pchData,
			int  uiDataLen,
			Pointer hMIMEStream);

	void MIMEStreamClose(
			Pointer hMIMEStream);

	@UndocumentedAPI
	short NSFProfileNameToProfileNoteName(
            Memory ProfileName, short ProfileNameLength,
            Memory UserName, short UserNameLength, boolean bLeaveCase, Memory ProfileNoteName);

	short NSFDbModifiedTimeByName(
			Memory DbName,
			NotesTimeDateStruct retDataModified,
			NotesTimeDateStruct retNonDataModified);
	
	short NSFDbDelete(
			Memory PathName
			);

	void DesignGetNameAndAlias(Memory pString, PointerByReference ppName, ShortByReference pNameLen, PointerByReference ppAlias, ShortByReference pAliasLen);

	boolean StoredFormHasSubformToken(Memory pString);

	void SECKFMCreatePassword(
			Memory pPassword,
			KFM_PASSWORDStruct.ByReference retHashedPassword);
	
	@UndocumentedAPI
	boolean CmemflagTestMultiple (Pointer s, short length, Pointer pattern);

	@UndocumentedAPI
	short QueueCreate(DHANDLE.ByReference qhandle);
	
	@UndocumentedAPI
	short QueueGet(DHANDLE.ByValue qhandle, DHANDLE.ByReference sehandle);

	@UndocumentedAPI
	short QueueDelete(DHANDLE.ByValue qhandle);

	@UndocumentedAPI
	short NSFRemoteConsoleAsync (
			Memory serverName, Memory ConsoleCommand, int Flags,
			DHANDLE.ByReference phConsoleText, DHANDLE.ByReference phTasksText, DHANDLE.ByReference phUsersText,
			ShortByReference pSignals, IntByReference pConsoleBufferID, DHANDLE.ByValue hQueue,
			NotesCallbacks.ASYNCNOTIFYPROC Proc,Pointer param, PointerByReference retactx);

	short NSFRemoteConsole(
			Memory ServerName,
			Memory ConsoleCommand,
			DHANDLE.ByReference hResponseText);

	void NSFAsyncNotifyPoll(Pointer actx, IntByReference retMySessions, ShortByReference retFirstError);
	void NSFUpdateAsyncIOStatus(Pointer actx);
	void NSFCancelAsyncIO (Pointer actx);

	void SECTokenValidate2(
			Memory ServerName,
			Memory OrgName,
			Memory ConfigName,
			Memory TokenData,
			int AssumedType,
			int dwRequestedInfoFlags,
			NotesSSOTokenInfoDescStruct.ByReference retpInfo,
			int dwReserved,
			Pointer vpReserved);
	
	void SECTokenFreeInfo(
			NotesSSOTokenInfoDescStruct.ByReference pInfo,
			boolean bFreeAll,
			int dwRequestedInfoFlags);
	
	short NSFDbAllocObject(
			HANDLE.ByValue hDB,
			int dwSize,
			short Class,
			short Privileges,
			IntByReference retObjectID);

	@UndocumentedAPI
	short NSFDbAllocObjectExtended2(HANDLE.ByValue cDB,
			int size, short noteClass, short privs, short type, IntByReference rtnRRV);

	short NSFDbReadObject(
			HANDLE.ByValue hDB,
			int ObjectID,
			int Offset,
			int Length,
			DHANDLE.ByReference rethBuffer);

	short NSFDbWriteObject(
			HANDLE.ByValue hDB,
			int ObjectID,
			DHANDLE.ByValue hBuffer,
			int Offset,
			int Length);
	
	short NSFDbFreeObject(
			HANDLE.ByValue hDB,
			int ObjectID);

	short NSFDbReallocObject(
			HANDLE.ByValue hDB,
			int objectID,
			int newSize);
	
	short NSFDbGetObjectSize(
			HANDLE.ByValue hDB,
			int ObjectID,
			short ObjectType,
			IntByReference retSize,
			ShortByReference retClass,
			ShortByReference retPrivileges);

	@UndocumentedAPI
	short NSFDbLocalSecInfoGetLocal(HANDLE.ByValue hDb, IntByReference state, IntByReference strength);

	@UndocumentedAPI
	short NSFDbLocalSecInfoSet(HANDLE.ByValue hDB, short Option, byte EncryptStrength, Memory Username);

	short NSFDbCompactExtendedExt2(Memory pathname, int options, int options2, DoubleByReference originalSize, DoubleByReference compactedSize);

	@UndocumentedAPI
	Pointer NSFFindFormulaParameters(Memory pszString);
	
	@UndocumentedAPI
	short NSFFormulaFunctions(NSFFORMFUNCPROC callback);

	@UndocumentedAPI
	short NSFFormulaCommands(NSFFORMCMDSPROC callback);
	
	@UndocumentedAPI
	short NSFFormulaAnalyze (DHANDLE.ByValue hFormula,
			IntByReference retAttributes,
			ShortByReference retSummaryNamesOffset);

	/**
	 * @param handle the handle to lock
	 * @return a pointer to the locked value
	 * @deprecated use {@link Mem#OSLockObject(DHANDLE.ByValue)} instead
	 */
	@Deprecated Pointer OSLockObject(DHANDLE.ByValue handle);
	/**
	 * @param handle the handle to unlock
	 * @return whether unlocking was successful
	 * @deprecated use {@link Mem#OSUnlockObject(NotesBlockIdStruct)} instead
	 */
	@Deprecated boolean OSUnlockObject(DHANDLE.ByValue handle);

	short NSFItemModifyValue (DHANDLE.ByValue hNote, NotesBlockIdStruct.ByValue bhItem, 
		      short itemFlags, short dataType, 
		      Pointer value, int valueLength);

	@UndocumentedAPI
	short DesignOpenCollection(HANDLE.ByValue hDB,
            boolean bPrivate,
            short OpenFlags,
            DHANDLE.ByReference rethCollection,
            IntByReference retCollectionNoteID);

	short NIFCloseCollection(DHANDLE.ByValue hCollection);

	@UndocumentedAPI
	short NLS_goto_prev_whole_char (
	    PointerByReference ppString, 
	    Pointer pStrStart, 
	    Pointer pInfo);

	/**
	 * @param handle the handle to free
	 * @return the result status
	 * @deprecated use {@link Mem#OSMemFree(DHANDLE.ByValue)} instead
	 */
	@Deprecated short OSMemFree(DHANDLE.ByValue handle);

	/**
	 * @param handle the handle for which to get the size
	 * @param retSize the size return value
	 * @return the result status
	 * @deprecated use {@link Mem#OSMemGetSize(DHANDLE.ByValue, IntByReference)} instead
	 */
	@Deprecated short OSMemGetSize(DHANDLE.ByValue handle, IntByReference retSize);

	/**
	 * @param handle the handle of the memory to realloc
	 * @param newSize new size of memory
	 * @return status
	 * @deprecated use {@link Mem#OSMemRealloc(com.hcl.domino.jna.internal.gc.handles.DHANDLE.ByValue, int)} instead
	 */
	@Deprecated short OSMemRealloc(
			DHANDLE.ByValue handle,
			int newSize);

	short NSFDbReadACL(
			HANDLE.ByValue hDB,
			DHANDLE.ByReference rethACL);
	
	short ACLSetAdminServer(
			DHANDLE.ByValue hList,
			Memory ServerName);

	short ACLGetAdminServer(
			DHANDLE.ByValue hList,
			Memory ServerName);
	
	@UndocumentedAPI
	short ACLCopy(DHANDLE.ByValue hList, DHANDLE.ByReference hNewList);

	short ACLLookupAccess(
			DHANDLE.ByValue hACL,
			Pointer pNamesList,
			ShortByReference retAccessLevel,
			Memory retPrivileges,
			ShortByReference retAccessFlags,
			LongByReference rethPrivNames);
	
	short ACLAddEntry(
			DHANDLE.ByValue hACL,
			Memory name,
			short AccessLevel,
			Memory privileges,
			short AccessFlags);

	short ACLDeleteEntry(
			DHANDLE.ByValue hACL,
			Memory name);
	
	short ACLSetFlags(
			DHANDLE.ByValue hACL,
			int Flags);
	
	short ACLGetFlags(
			DHANDLE.ByValue hACL,
			IntByReference retFlags);

	short ACLSetPrivName(
			DHANDLE.ByValue hACL,
			short PrivNum,
			Memory privName);

	short ACLUpdateEntry(
			DHANDLE.ByValue hACL,
			Memory name,
			short updateFlags,
			Memory newName,
			short newAccessLevel,
			Memory newPrivileges,
			short newAccessFlags);

	short ACLEnumEntries(
			DHANDLE.ByValue hACL,
			ACLENTRYENUMFUNC EnumFunc,
			Pointer EnumFuncParam);

	short ACLGetPrivName(
			DHANDLE.ByValue hACL,
			short PrivNum,
			Memory retPrivName);

	short NSFDbStoreACL(
			HANDLE.ByValue hDB,
			DHANDLE.ByValue hACL,
			int ObjectID,
			short Method);
	
	short ACLCreate(DHANDLE.ByReference rethACL);

	short NSFDbGetSpecialNoteID(
			HANDLE.ByValue hDB,
			short Index,
			IntByReference retNoteID);

	short NSFDbClassGet(
			HANDLE.ByValue hDB,
			ShortByReference retClass);
	
	short IDTableIntersect(DHANDLE.ByValue hSrc1Table, DHANDLE.ByValue hSrc2Table, DHANDLE.ByReference rethDstTable);
	int IDEntries (DHANDLE.ByValue hTable);
	short IDDestroyTable(DHANDLE.ByValue hTable);
	short IDDeleteTable  (DHANDLE.ByValue hTable, DHANDLE.ByValue hIDsToDelete);

}
