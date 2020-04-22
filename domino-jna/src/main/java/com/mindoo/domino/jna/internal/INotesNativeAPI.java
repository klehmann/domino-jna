package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.internal.structs.IntlFormatStruct;
import com.mindoo.domino.jna.internal.structs.NotesBlockIdStruct;
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
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

public interface INotesNativeAPI extends Library {
	public static enum Mode {Classic, Direct}
	
	public short NotesInitExtended(int argc, Memory argvPtr);
	public void NotesTerm();

	public short NotesInitThread();
	public void NotesTermThread();

	public short OSTranslate(short translateMode, Memory in, short inLength, Memory out, short outLength);
	public short OSTranslate(short translateMode, Pointer in, short inLength, Memory out, short outLength);
	public int OSTranslate32(short translateMode, Memory in, int inLength, Memory out, int outLength);
	public int OSTranslate32(short translateMode, Pointer in, int inLength, Memory out, int outLength);

	public short OSLoadString(int hModule, short StringCode, Memory retBuffer, short BufferLength);
	public short OSLoadString(long hModule, short StringCode, Memory retBuffer, short BufferLength);
	public short OSPathNetConstruct(Memory PortName,
			Memory ServerName,
			Memory FileName,
			Memory retPathName);
	public short OSPathNetParse(Memory PathName,
			Memory retPortName,
			Memory retServerName,
			Memory retFileName);
	public void OSGetExecutableDirectory(Memory retPathName);
	public void OSGetDataDirectory(Memory retPathName);
	public short OSGetSystemTempDirectory(Memory retPathName, int bufferLength);
	@UndocumentedAPI
	public void OSPathAddTrailingPathSep(Memory retPathName);
	public short OSGetEnvironmentString(Memory variableName, Memory rethValueBuffer, short bufferLength);
	public long OSGetEnvironmentLong(Memory variableName);
	@UndocumentedAPI
	public short OSGetEnvironmentTIMEDATE(Memory envVariable, NotesTimeDateStruct retTd); 
	public void OSSetEnvironmentVariable(Memory variableName, Memory Value);
	@UndocumentedAPI
	public void OSSetEnvironmentVariableExt (Memory variableName, Memory Value, short isSoft);
	public void OSSetEnvironmentInt(Memory variableName, int Value);
	@UndocumentedAPI
	public void OSSetEnvironmentTIMEDATE(Memory envVariable, NotesTimeDateStruct td);
	@UndocumentedAPI
	public short OSGetEnvironmentSeqNo();
	
	public short OSMemoryAllocate(int  dwtype, int  size, IntByReference rethandle);

	public boolean TimeLocalToGM(Memory timePtr);
	public boolean TimeLocalToGM(NotesTimeStruct timePtr);
	public boolean TimeGMToLocalZone (NotesTimeStruct timePtr);
	public boolean TimeGMToLocal (NotesTimeStruct timePtr);
	public void TimeConstant(short timeConstantType, NotesTimeDateStruct tdptr);
	public int TimeExtractTicks(Memory time);
	public int TimeExtractJulianDate(Memory time);
	public int TimeExtractDate(Memory time);

	public short ConvertTIMEDATEToText(
			IntlFormatStruct intlFormat,
			Pointer textFormat,
			NotesTimeDateStruct inputTime,
			Memory retTextBuffer,
			short textBufferLength,
			ShortByReference retTextLength);

	public short ConvertTextToTIMEDATE(
			IntlFormatStruct intlFormat,
			Pointer textFormat,
			Memory text,
			short maxLength,
			NotesTimeDateStruct retTIMEDATE);

	public short ListGetNumEntries(Pointer vList, int noteItem);

	public short ListGetText (Pointer pList,
			boolean fPrefixDataType,
			short entryNumber,
			Memory retTextPointer,
			ShortByReference retTextLength);

	public short ListGetSize(
			Pointer pList,
			int fPrefixDataType);

	public short IDTableFlags (Pointer pIDTable);
	public void IDTableSetFlags (Pointer pIDTable, short Flags);
	public void IDTableSetTime(Pointer pIDTable, NotesTimeDateStruct Time);
	public NotesTimeDateStruct IDTableTime(Pointer pIDTable);

	public short DNCanonicalize(int Flags, Memory TemplateName, Memory InName, Memory OutName, short OutSize, ShortByReference OutLength);
	public short DNAbbreviate(int Flags, Memory TemplateName, Memory InName, Memory OutName, short OutSize, ShortByReference OutLength);	

	public short NSFGetTransLogStyle(ShortByReference LogType);
	public short NSFBeginArchivingLogs();
	public short NSFGetFirstLogToArchive(NotesUniversalNoteIdStruct LogID, IntByReference LogNumber, Memory LogPath);
	public short NSFGetNextLogToArchive(
			NotesUniversalNoteIdStruct LogID, IntByReference LogNumber, Memory LogPath);
	public short NSFDoneArchivingLog(NotesUniversalNoteIdStruct LogID, IntByReference LogSequenceNumber);
	public short NSFEndArchivingLogs();
	public short NSFTakeDatabaseOffline(Memory dbPath, int WaitTime, int options);
	public short NSFRecoverDatabases(Memory dbNames,
			NotesCallbacks.LogRestoreCallbackFunction restoreCB,
			int Flags,
			ShortByReference errDbIndex,
			NotesTimeDatePairStruct recoveryTime);
	public short NSFBringDatabaseOnline(Memory dbPath, int options);

	public short NSFItemRealloc(
			NotesBlockIdStruct.ByValue item_blockid,
			NotesBlockIdStruct value_blockid_ptr,
			int value_len);

	public short NSFDbCreateExtended(
			Memory pathName,
			short  DbClass,
			boolean  ForceCreation,
			short  Options,
			byte  EncryptStrength,
			long  MaxFileSize);
	public short NSFDbRename(Memory dbNameOld, Memory dbNameNew);
	public short NSFDbMarkInService(Memory dbPath);
	public short NSFDbMarkOutOfService(Memory dbPath);
	public short NSFDbFTSizeGet(Memory dbPath, IntByReference retFTSize);
	
	@UndocumentedAPI
	public short ECLGetListCapabilities(Pointer pNamesList, short ECLType, ShortByReference retwCapabilities,
			ShortByReference retwCapabilities2, IntByReference retfUserCanModifyECL);

	public short SECKFMChangePassword(Memory pIDFile, Memory pOldPassword, Memory pNewPassword);
	public short SECKFMGetUserName(Memory retUserName);
	public short SECKFMSwitchToIDFile(Memory pIDFileName, Memory pPassword, Memory pUserName,
			short  MaxUserNameLength, int Flags, Pointer pReserved);
	public short SECidvResetUserPassword(Memory pServer, Memory pUserName, Memory pPassword,
			short wDownloadCount, int ReservedFlags, Pointer pReserved);
	public short SECKFMGetPublicKey(
			Memory pName,
			short Function,
			short Flags,
			IntByReference rethPubKey);
	public short SECTokenValidate(
			Memory ServerName,
			Memory OrgName,
			Memory ConfigName,
			Memory TokenData,
			Memory retUsername,
			NotesTimeDateStruct retCreation,
			NotesTimeDateStruct retExpiration,
			int  dwReserved,
			Pointer vpReserved);

	public short ODSLength(short type);
	public void ODSWriteMemory(
			Pointer ppDest,
			short  type,
			Pointer pSrc,
			short  iterations);
	
	public void ODSReadMemory(
			Pointer ppSrc,
			short  type,
			Pointer pDest,
			short iterations);

	public short MQCreate(Memory queueName, short quota, int options);
	public short MQOpen(Memory queueName, int options, IntByReference retQueue);
	public short MQClose(int queue, int options);
	public short MQPut(int queue, short priority, Pointer buffer, short length, 
			int options);
	public short MQGet(int queue, Pointer buffer, short bufLength,
			int options, int timeout, ShortByReference retMsgLength);
	public short MQScan(int queue, Pointer buffer, short bufLength, 
			int options, NotesCallbacks.MQScanCallback actionRoutine,
			Pointer ctx, ShortByReference retMsgLength);

	public void MQPutQuitMsg(int queue);
	public boolean MQIsQuitPending(int queue);
	public short MQGetCount(int queue);

	public short ReplicateWithServerExt(
			Memory PortName,
			Memory ServerName,
			int Options,
			short NumFiles,
			Memory FileList,
			ReplExtensionsStruct ExtendedOptions,
			ReplServStatsStruct retStats);

	public NotesCallbacks.OSSIGPROC OSGetSignalHandler(short signalHandlerID);
	public NotesCallbacks.OSSIGPROC OSSetSignalHandler(short signalHandlerID, NotesCallbacks.OSSIGPROC routine);

	public Pointer OSGetLMBCSCLS();

	public short HTMLConvertImage(
			int hHTML,
			Memory pszImageName);
	public short REGGetIDInfo(
			Memory IDFileName,
			short InfoType,
			Memory OutBufr,
			short OutBufrLen,
			ShortByReference ActualLen);

	public void CompoundTextInitStyle(NotesCompoundStyleStruct style);
	public short EnumCompositeBuffer(
			NotesBlockIdStruct.ByValue ItemValue,
			int ItemValueLength,
			NotesCallbacks.ActionRoutinePtr  ActionRoutine,
			Pointer vContext);

	@UndocumentedAPI
	public void NIFGetViewRebuildDir(Memory retPathName, int BufferLength);
//  commented out, not compatible with later Notes version
//	@UndocumentedAPI
//	public void DAOSGetBaseStoragePath(Memory retPathName, int BufferLength);
	 
	public void NSFDbInfoParse(
			Pointer Info,
			short What,
			Pointer Buffer,
			short Length);
	public void NSFDbInfoModify(
			Pointer Info,
			short What,
			Pointer Buffer);

	public short CalGetRecurrenceID(
			NotesTimeDateStruct.ByValue tdInput,
			Memory pszRecurID,
			short wLenRecurId);
	
	public short OOOInit();
	
	public short OOOTerm();

	public short OOOEnable(
			Pointer pOOOContext,
			int bState);
	
	public short OOOGetAwayPeriod(
			Pointer pOOOContext,
			NotesTimeDateStruct tdStartAway,
			NotesTimeDateStruct tdEndAway);
	
	public short OOOGetExcludeInternet(
			Pointer pOOOContext,
			IntByReference bExcludeInternet);
	
	public short OOOGetGeneralMessage(
			Pointer pOOOContext,
			Memory pGeneralMessage,
			ShortByReference pGeneralMessageLen);
	
	public short OOOGetGeneralSubject(
			Pointer pOOOContext,
			Memory pGeneralSubject);
	
	public short OOOGetState(
			Pointer pOOOContext,
			ShortByReference retVersion,
			ShortByReference retState);

	public short OOOSetAwayPeriod(
			Pointer pOOOContext,
			NotesTimeDateStruct.ByValue tdStartAway,
			NotesTimeDateStruct.ByValue tdEndAway);
	
	public short OOOSetExcludeInternet(
			Pointer pOOOContext,
			int bExcludeInternet);

	public short OOOSetGeneralMessage(
			Pointer pOOOContext,
			Memory pGeneralMessage,
			short wGeneralMessageLen);
	
	public short OOOSetGeneralSubject(
			Pointer pOOOContext,
			Memory pGeneralSubject,
			int bDisplayReturnDate);

	public short OSGetExtIntlFormat(
			byte item,
			byte index,
			Memory buff,
			short bufSize);
	
	@UndocumentedAPI
	public void DEBUGDumpHandleTable(int flags, short blkType);

	@UndocumentedAPI
	public short DesignFindTemplate(Pointer designTemplateName, Pointer excludeDbPath, Pointer foundDbPath);

	@UndocumentedAPI
	public short MIMEEMLExport(Memory dbName, int noteID, Memory pFileName);

	public void StatTraverse(
			Memory Facility,
			Memory StatName,
			NotesCallbacks.STATTRAVERSEPROC  Routine,
			Pointer Context);
	
	public void OSGetIntlSettings(
			IntlFormatStruct retIntlFormat,
			short bufferSize);
	
	@UndocumentedAPI
	public short OSRunNSDExt (Memory szServerName, short flags);
	
	public short DXLCreateExporter(IntByReference prethDXLExport);
	
	public void DXLDeleteExporter(int hDXLExport);
	
	public short DXLExportWasErrorLogged(int hDXLExport);
	
	public short DXLGetExporterProperty(
			int hDXLExport,
			int prop,
			Memory retPropValue);

	public short DXLSetExporterProperty(
			int hDXLExport,
			int prop,
			Memory propValue);
	
	public short DXLCreateImporter(IntByReference prethDXLImport);
	
	public void DXLDeleteImporter(int hDXLImport);
	
	public short DXLImportWasErrorLogged(int hDXLImport);
	
	public short DXLGetImporterProperty(
			int hDXLImporter,
			int prop,
			Memory retPropValue);
	
	public short DXLSetImporterProperty(
			int hDXLImport,
			int prop,
			Memory propValue);

	public short XSLTTransform(
			int hXSLTransform,
			NotesCallbacks.XML_READ_FUNCTION pXSL_XMLInputFunc,
			Pointer pXSL_XMLInputAction,
			NotesCallbacks.XML_READ_FUNCTION  pXSL_StylesheetInputFunc,
			Pointer pXSL_StylesheetInputAction,
			NotesCallbacks.XML_WRITE_FUNCTION  pXSL_TransformOutputFunc,
			Pointer pXSL_TransformOutputAction);
	
	public short XSLTCreateTransform(
			IntByReference prethXSLTransform);
	
	public short MMCreateConvControls(
			PointerByReference phCC);

	public short MMDestroyConvControls(
			Pointer hCC);
	
	public void MMConvDefaults(
			Pointer hCC);

	public short MMGetAttachEncoding(
			Pointer hCC);
	
	public void MMSetAttachEncoding(
			Pointer hCC,
			short wAttachEncoding);
	
	public void MMSetDropItems(
			Pointer hCC,
			Memory pszDropItems);
	
	public Pointer MMGetDropItems(
			Pointer hCC);
	
	public void MMSetKeepTabs(
			Pointer hCC,
			boolean bKeepTabs);
	
	public boolean MMGetKeepTabs(
			Pointer hCC);
	
	public void MMSetPointSize(
			Pointer hCC,
			short wPointSize);

	public short MMGetPointSize(
			Pointer hCC);
	
	public short MMGetTypeFace(
			Pointer hCC);
	
	public void MMSetTypeFace(
			Pointer hCC,
			short wTypeFace);

	public void MMSetAddItems(
			Pointer hCC,
			Memory pszAddItems);

	public Pointer MMGetAddItems(
			Pointer hCC);
	
	public void MMSetMessageContentEncoding(
			Pointer hCC,
			short wMessageContentEncoding);

	public short MMGetMessageContentEncoding(
			Pointer hCC);
	
	public void MMSetReadReceipt(
			Pointer hCC,
			short wReadReceipt);

	public short MMGetReadReceipt(
			Pointer hCC);
	
	public void MMSetSkipX(
			Pointer hCC,
			boolean bSkipX);
	
	public boolean MMGetSkipX(
			Pointer hCC);
	
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

}
