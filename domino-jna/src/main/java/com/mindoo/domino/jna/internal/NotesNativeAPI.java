package com.mindoo.domino.jna.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.UnsupportedPlatformError;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCallbacks.ASYNCNOTIFYPROC;
import com.mindoo.domino.jna.internal.NotesCallbacks.STATTRAVERSEPROC;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.structs.IntlFormatStruct;
import com.mindoo.domino.jna.internal.structs.KFM_PASSWORDStruct.ByReference;
import com.mindoo.domino.jna.internal.structs.NotesBlockIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDatePairStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct.ByValue;
import com.mindoo.domino.jna.internal.structs.NotesTimeStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.internal.structs.ReplExtensionsStruct;
import com.mindoo.domino.jna.internal.structs.ReplServStatsStruct;
import com.mindoo.domino.jna.internal.structs.compoundtext.NotesCompoundStyleStruct;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * Class providing C methods for both 32 and 64 bit. Should be used internally by
 * the Domino JNA API methods only.
 * 
 * @author Karsten Lehmann
 */
public class NotesNativeAPI implements INotesNativeAPI {
	private static volatile INotesNativeAPI m_instanceWithoutCrashLogging;
	private static volatile INotesNativeAPI m_instanceWithCrashLogging;
	private static Class m_nativeClazz;
	
	private static int m_platformAlignment;
	static Throwable m_initError;

	private static Map<String, Object> m_libraryOptions;
	
	/**
	 * Returns the JNA initialization options (only public for technical reasons)
	 * 
	 * @return options, read-only
	 */
	public static Map<String, Object> getLibraryOptions() {
		return m_libraryOptions==null ? null : Collections.unmodifiableMap(m_libraryOptions);
	}

	/**
	 * Initializes the Domino API
	 */
	public static synchronized void initialize() {
		if (m_instanceWithoutCrashLogging==null && m_initError==null) {
			m_instanceWithoutCrashLogging = AccessController.doPrivileged(new PrivilegedAction<INotesNativeAPI>() {

				@Override
				public INotesNativeAPI run() {
					try {
						//keep reference to Native as described here: https://github.com/java-native-access/jna/blob/master/www/FrequentlyAskedQuestions.md#why-does-the-vm-sometimes-crash-in-my-shutdown-hook-on-windows
						m_nativeClazz = Native.class;
						
						//enforce using the extracted JNA .dll/.so file instead of what we find on the PATH
						System.setProperty("jna.nosys", "true");
						
						if (PlatformUtils.isWindows()) {
							if (PlatformUtils.is64Bit()) {
								m_platformAlignment = Structure.ALIGN_DEFAULT;
							}
							else {
								m_platformAlignment = Structure.ALIGN_NONE;
							}
						}
						else if (PlatformUtils.isMac()) {
							if (PlatformUtils.is64Bit()) {
								m_platformAlignment = Structure.ALIGN_NONE;
							}
							else {
								m_platformAlignment = Structure.ALIGN_DEFAULT;
							}
						}
						else if (PlatformUtils.isLinux()) {
							m_platformAlignment = Structure.ALIGN_DEFAULT;
						}
						else {
							String osName = System.getProperty("os.name");
							m_initError = new UnsupportedPlatformError("Platform is unknown or not supported: "+osName);
							return null;
						}

						System.out.println("Initializing Domino JNA");
						if (!"true".equals(System.getProperty("dominojna.keepprotectedmode"))) {
							//since this crashes in multithreaded and Linux environments, we
							//prefer not to use protected mode
							Native.setProtected(false);
						}
						
						if (Native.isProtected()) {
							System.out.println("WARNING: JNA protected mode should not be active on production systems!");
						}
						
						m_libraryOptions = new HashMap<String, Object>();
						m_libraryOptions.put(Library.OPTION_CLASSLOADER, NotesNativeAPI.class.getClassLoader());
						
						if (PlatformUtils.isWin32()) {
							m_libraryOptions.put(Library.OPTION_CALLING_CONVENTION, Function.ALT_CONVENTION); // set w32 stdcall convention
						}

						INotesNativeAPI api;
						if (PlatformUtils.isWindows()) {
							api = Native.loadLibrary("nnotes", INotesNativeAPI.class, m_libraryOptions);

							if (PlatformUtils.is64Bit()) {
								INotesNativeAPI64 api64 = Native.loadLibrary("nnotes", INotesNativeAPI64.class, m_libraryOptions);
								NotesNativeAPI64.set(api64);
							}
							else {
								INotesNativeAPI32 api32 = Native.loadLibrary("nnotes", INotesNativeAPI32.class, m_libraryOptions);
								NotesNativeAPI32.set(api32);
							}
						}
						else {
							api = Native.loadLibrary("notes", INotesNativeAPI.class, m_libraryOptions);
							
							if (PlatformUtils.is64Bit()) {
								INotesNativeAPI64 api64 = Native.loadLibrary("notes", INotesNativeAPI64.class, m_libraryOptions);
								NotesNativeAPI64.set(api64);
							}
							else {
								INotesNativeAPI32 api32 = Native.loadLibrary("notes", INotesNativeAPI32.class, m_libraryOptions);
								NotesNativeAPI32.set(api32);
							}
						}

						return api;
					
					}
					catch (Throwable t) {
						m_initError = t;
						return null;
					}
				}
			});
		}
	}

	/**
	 * Returns the API instance used to call native Domino C API methods for 32 and 64 bit
	 * 
	 * @return API
	 */
	public static INotesNativeAPI get() {
		//check if this failed the last time
		if (m_initError!=null) {
			if (m_initError instanceof RuntimeException)
				throw (RuntimeException) m_initError;
			else
				throw new NotesError(0, "Error initializing Domino JNA API", m_initError);
		}
		
		if (m_instanceWithoutCrashLogging==null) {
			initialize();
			
			if (m_initError!=null) {
				if (m_initError instanceof RuntimeException)
					throw (RuntimeException) m_initError;
				else
					throw new NotesError(0, "Error initializing Domino JNA API", m_initError);
			}
		}

		if (NotesGC.isLogCrashingThreadStacktrace()) {
			if (m_instanceWithCrashLogging==null) {
				m_instanceWithCrashLogging = wrapWithCrashStackLogging(INotesNativeAPI.class, m_instanceWithoutCrashLogging);
			}
			return m_instanceWithCrashLogging;
		}
		else {
			return m_instanceWithoutCrashLogging;
		}
	}

	/**
	 * Returns the alignment to be used for the current platform
	 * @return alignment
	 */
	public static int getPlatformAlignment() {
		return m_platformAlignment;
	}


	/**
	 * {@link MethodInterceptor} that writes the caller stacktrace to a file before invoking
	 * the wrapped methods in order to improve crash cause detection.
	 * 
	 * @author Karsten Lehmann
	 *
	 * @param <T> class of wrapped API
	 */
	private static class MethodInterceptorWithStacktraceLogging<T> implements MethodInterceptor {
		private final T original;
		private boolean loggedFileLocation;
		
		public MethodInterceptorWithStacktraceLogging(T original) {
			this.original = original;
		}

		public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			Exception e = new Exception();
			e.fillInStackTrace();

			File stFile = createStackTraceFile(e);
			try {
				return method.invoke(original, args);
			}
			finally {
				if (stFile!=null)
					deleteStackTraceFile(stFile);
			}
		}

		private void deleteStackTraceFile(final File stFile) {
			AccessController.doPrivileged(new PrivilegedAction<Object>() {

				@Override
				public Object run() {
					if (stFile.exists() && !stFile.delete()) {
						stFile.deleteOnExit();
					}
					return null;
				}
			});
		}

		private File createStackTraceFile(final Exception e) {
			return AccessController.doPrivileged(new PrivilegedAction<File>() {

				@Override
				public File run() {
					String outDirPath = System.getProperty("dominojna.dumpdir");
					if (StringUtil.isEmpty(outDirPath)) {
						outDirPath = System.getProperty("java.io.tmpdir");
					}
					File outDir = new File(outDirPath);
					if (!outDir.exists())
						outDir.mkdirs();
					
					if (!loggedFileLocation) {
						System.out.println("Writing stacktrace files in directory "+outDir.getAbsolutePath());
						loggedFileLocation = true;
					}
					
					File stFile = new File(outDir, "domino-jna-stack-"+Thread.currentThread().getId()+".txt");
					if (stFile.exists()) {
						if (!stFile.delete()) {
							stFile.deleteOnExit();
							return null;
						}
					}
					FileOutputStream fOut=null;
					Writer fWriter=null;
					PrintWriter pWriter=null;
					try {
						fOut = new FileOutputStream(stFile);
						fWriter = new OutputStreamWriter(fOut, Charset.forName("UTF-8"));
						pWriter = new PrintWriter(fWriter);
						e.printStackTrace(pWriter);
						pWriter.flush();
						FileChannel channel = fOut.getChannel();
						channel.force(true);
						return stFile;
					} catch (IOException e1) {
						e.printStackTrace();
						return null;
					}
					finally {
						if (fOut!=null) {
							try {
								fOut.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						if (pWriter!=null) {
							pWriter.close();
						}
						if (fWriter!=null) {
							try {
								fWriter.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			});
		}
	}

	/**
	 * Wraps the specified API object to dump caller stacktraces right before invoking
	 * native methods
	 * 
	 * @param api API
	 * @return wrapped API
	 */
	static <T> T wrapWithCrashStackLogging(final Class<T> apiClazz, final T api) {

		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<T>() {

				@Override
				public T run() throws Exception {
					MethodInterceptor handler = new MethodInterceptorWithStacktraceLogging<T>(api);
					T wrapperWithStacktraceLogging = (T) Enhancer.create(apiClazz, handler);
					return wrapperWithStacktraceLogging;
				}
			});
		} catch (PrivilegedActionException e) {
			e.printStackTrace();
			return api;
		}
	}
	
	public native short NotesInitExtended(int argc, Memory argvPtr);
	public native void NotesTerm();

	public native short NotesInitThread();
	public native void NotesTermThread();

	public native short OSTranslate(short translateMode, Memory in, short inLength, Memory out, short outLength);
	public native short OSTranslate(short translateMode, Pointer in, short inLength, Memory out, short outLength);
	@UndocumentedAPI
	public native int OSTranslate32(short translateMode, Memory in, int inLength, Memory out, int outLength);
	@UndocumentedAPI
	public native int OSTranslate32(short translateMode, Pointer in, int inLength, Memory out, int outLength);

	public native short OSLoadString(int hModule, short StringCode, Memory retBuffer, short BufferLength);
	public native short OSLoadString(long hModule, short StringCode, Memory retBuffer, short BufferLength);
	public native short OSPathNetConstruct(Memory PortName,
			Memory ServerName,
			Memory FileName,
			Memory retPathName);
	public native short OSPathNetParse(Memory PathName,
			Memory retPortName,
			Memory retServerName,
			Memory retFileName);
	public native void OSGetExecutableDirectory(Memory retPathName);
	public native void OSGetDataDirectory(Memory retPathName);
	public native short OSGetSystemTempDirectory(Memory retPathName, int bufferLength);
	public native void OSPathAddTrailingPathSep(Memory retPathName);
	public native short OSGetEnvironmentString(Memory variableName, Memory rethValueBuffer, short bufferLength);
	public native long OSGetEnvironmentLong(Memory variableName);
	public native short OSGetEnvironmentTIMEDATE(Memory envVariable, NotesTimeDateStruct retTd);
	public native void OSSetEnvironmentVariable(Memory variableName, Memory Value);
	public native void OSSetEnvironmentVariableExt(Memory variableName, Memory Value, short isSoft);
	public native void OSSetEnvironmentInt(Memory variableName, int Value);
	public native void OSSetEnvironmentTIMEDATE(Memory envVariable, NotesTimeDateStruct td);
	public native short OSGetEnvironmentSeqNo();
	
	public native short OSMemoryAllocate(int  dwtype, int  size, IntByReference rethandle);

	public native boolean TimeLocalToGM(Memory timePtr);
	public native boolean TimeLocalToGM(NotesTimeStruct timePtr);
	public native boolean TimeGMToLocalZone (NotesTimeStruct timePtr);
	public native boolean TimeGMToLocal (NotesTimeStruct timePtr);
	public native void TimeConstant(short timeConstantType, NotesTimeDateStruct tdptr);
	public native int TimeExtractTicks(Memory time);
	public native int TimeExtractJulianDate(Memory time);
	public native int TimeExtractDate(Memory time);

	public native short ConvertTIMEDATEToText(
			IntlFormatStruct intlFormat,
			Pointer textFormat,
			NotesTimeDateStruct inputTime,
			Memory retTextBuffer,
			short textBufferLength,
			ShortByReference retTextLength);

	public native short ConvertTextToTIMEDATE(
			IntlFormatStruct intlFormat,
			Pointer textFormat,
			Memory text,
			short maxLength,
			NotesTimeDateStruct retTIMEDATE);

	public native short ListGetNumEntries(Pointer vList, int noteItem);

	public native short ListGetText (Pointer pList,
			boolean fPrefixDataType,
			short entryNumber,
			Memory retTextPointer,
			ShortByReference retTextLength);

	public native short ListGetSize(
			Pointer pList,
			int fPrefixDataType);

	public native short IDTableFlags (Pointer pIDTable);
	public native void IDTableSetFlags (Pointer pIDTable, short Flags);
	public native void IDTableSetTime(Pointer pIDTable, NotesTimeDateStruct Time);
	public native NotesTimeDateStruct IDTableTime(Pointer pIDTable);

	public native short DNCanonicalize(int Flags, Memory TemplateName, Memory InName, Memory OutName, short OutSize, ShortByReference OutLength);
	public native short DNAbbreviate(int Flags, Memory TemplateName, Memory InName, Memory OutName, short OutSize, ShortByReference OutLength);	

	public native short NSFGetTransLogStyle(ShortByReference LogType);
	public native short NSFBeginArchivingLogs();
	public native short NSFGetFirstLogToArchive(NotesUniversalNoteIdStruct LogID, IntByReference LogNumber, Memory LogPath);
	public native short NSFGetNextLogToArchive(
			NotesUniversalNoteIdStruct LogID, IntByReference LogNumber, Memory LogPath);
	public native short NSFDoneArchivingLog(NotesUniversalNoteIdStruct LogID, IntByReference LogSequenceNumber);
	public native short NSFEndArchivingLogs();
	public native short NSFTakeDatabaseOffline(Memory dbPath, int WaitTime, int options);
	public native short NSFRecoverDatabases(Memory dbNames,
			NotesCallbacks.LogRestoreCallbackFunction restoreCB,
			int Flags,
			ShortByReference errDbIndex,
			NotesTimeDatePairStruct recoveryTime);
	public native short NSFBringDatabaseOnline(Memory dbPath, int options);

	public native short NSFItemRealloc(
			NotesBlockIdStruct.ByValue item_blockid,
			NotesBlockIdStruct value_blockid_ptr,
			int value_len);

	public native short NSFDbCreateExtended(
			Memory pathName,
			short  DbClass,
			boolean  ForceCreation,
			short  Options,
			byte  EncryptStrength,
			long  MaxFileSize);
	public native short NSFDbRename(Memory dbNameOld, Memory dbNameNew);
	public native short NSFDbMarkInService(Memory dbPath);
	public native short NSFDbMarkOutOfService(Memory dbPath);
	public native short NSFDbFTSizeGet(Memory dbPath, IntByReference retFTSize);
	
	public native short ECLGetListCapabilities(Pointer pNamesList, short ECLType, ShortByReference retwCapabilities,
			ShortByReference retwCapabilities2, IntByReference retfUserCanModifyECL);

	public native short SECKFMChangePassword(Memory pIDFile, Memory pOldPassword, Memory pNewPassword);
	public native short SECKFMGetUserName(Memory retUserName);
	public native short SECKFMSwitchToIDFile(Memory pIDFileName, Memory pPassword, Memory pUserName,
			short  MaxUserNameLength, int Flags, Pointer pReserved);
	public native short SECidvResetUserPassword(Memory pServer, Memory pUserName, Memory pPassword,
			short wDownloadCount, int ReservedFlags, Pointer pReserved);
	public native short SECKFMGetPublicKey(
			Memory pName,
			short Function,
			short Flags,
			IntByReference rethPubKey);
	public native short SECTokenValidate(
			Memory ServerName,
			Memory OrgName,
			Memory ConfigName,
			Memory TokenData,
			Memory retUsername,
			NotesTimeDateStruct retCreation,
			NotesTimeDateStruct retExpiration,
			int  dwReserved,
			Pointer vpReserved);

	public native short SECidvIsIDInVault(Memory pServer, Memory pUserName);
	
	public native short ODSLength(short type);
	public native void ODSWriteMemory(
			Pointer ppDest,
			short  type,
			Pointer pSrc,
			short  iterations);
	
	public native void ODSReadMemory(
			Pointer ppSrc,
			short  type,
			Pointer pDest,
			short iterations);

	public native short MQCreate(Memory queueName, short quota, int options);
	public native short MQOpen(Memory queueName, int options, IntByReference retQueue);
	public native short MQClose(int queue, int options);
	public native short MQPut(int queue, short priority, Pointer buffer, short length, 
			int options);
	public native short MQGet(int queue, Pointer buffer, short bufLength,
			int options, int timeout, ShortByReference retMsgLength);
	public native short MQScan(int queue, Pointer buffer, short bufLength, 
			int options, NotesCallbacks.MQScanCallback actionRoutine,
			Pointer ctx, ShortByReference retMsgLength);

	public native void MQPutQuitMsg(int queue);
	public native boolean MQIsQuitPending(int queue);
	public native short MQGetCount(int queue);

	public native short ReplicateWithServerExt(
			Memory PortName,
			Memory ServerName,
			int Options,
			short NumFiles,
			Memory FileList,
			ReplExtensionsStruct ExtendedOptions,
			ReplServStatsStruct retStats);

	public native NotesCallbacks.OSSIGPROC OSGetSignalHandler(short signalHandlerID);
	public native NotesCallbacks.OSSIGPROC OSSetSignalHandler(short signalHandlerID, NotesCallbacks.OSSIGPROC routine);

	public native Pointer OSGetLMBCSCLS();

	public native short HTMLConvertImage(
			int hHTML,
			Memory pszImageName);
	public native short REGGetIDInfo(
			Memory IDFileName,
			short InfoType,
			Memory OutBufr,
			short OutBufrLen,
			ShortByReference ActualLen);

	public native void CompoundTextInitStyle(NotesCompoundStyleStruct style);
	public native short EnumCompositeBuffer(
			NotesBlockIdStruct.ByValue ItemValue,
			int ItemValueLength,
			NotesCallbacks.ActionRoutinePtr  ActionRoutine,
			Pointer vContext);

	public native void NIFGetViewRebuildDir(Memory retPathName, int BufferLength);
//  commented out, not compatible with later Notes version
//	public native void DAOSGetBaseStoragePath(Memory retPathName, int BufferLength);
	 
	public native void NSFDbInfoParse(
			Pointer Info,
			short What,
			Pointer Buffer,
			short Length);
	public native void NSFDbInfoModify(
			Pointer Info,
			short What,
			Pointer Buffer);

	public native short CalGetRecurrenceID(ByValue tdInput, Memory pszRecurID, short wLenRecurId);
	
	public native short OOOInit();
	public native short OOOTerm();
	public native short OOOGetAwayPeriod(Pointer pOOOContext, NotesTimeDateStruct tdStartAway, NotesTimeDateStruct tdEndAway);
	public native short OOOGetExcludeInternet(Pointer pOOOContext, IntByReference bExcludeInternet);
	public native short OOOGetGeneralSubject(Pointer pOOOContext, Memory pGeneralSubject);
	public native short OOOGetGeneralMessage(Pointer pOOOContext, Memory pGeneralMessage,
			ShortByReference pGeneralMessageLen);
	public native short OOOGetState(Pointer pOOOContext, ShortByReference retVersion, ShortByReference retState);
	public native short OOOSetAwayPeriod(Pointer pOOOContext, ByValue tdStartAway, ByValue tdEndAway);
	public native short OOOSetExcludeInternet(Pointer pOOOContext, int bExcludeInternet);
	public native short OOOSetGeneralMessage(Pointer pOOOContext, Memory pGeneralMessage, short wGeneralMessageLen);
	public native short OOOSetGeneralSubject(Pointer pOOOContext, Memory pGeneralSubject, int bDisplayReturnDate);
	public native short OOOEnable(Pointer pOOOContext, int bState);
	
	public native short OSGetExtIntlFormat(byte item, byte index, Memory buff, short bufSize);
	
	@Override
	public native void DEBUGDumpHandleTable(int flags, short blkType);

	public native short DesignFindTemplate(Pointer designTemplateName, Pointer excludeDbPath, Pointer foundDbPath);
	
	public native short MIMEEMLExport(Memory dbName, int noteID, Memory pFileName);

	public native void StatTraverse(Memory Facility, Memory StatName, STATTRAVERSEPROC Routine, Pointer Context);

	public native void OSGetIntlSettings(IntlFormatStruct retIntlFormat, short bufferSize);
	
	public native short OSRunNSDExt(Memory szServerName, short flags);
	
	public native short DXLCreateExporter(IntByReference prethDXLExport);
	
	public native void DXLDeleteExporter(int hDXLExport);
	
	public native short DXLExportWasErrorLogged(int hDXLExport);

	public native short DXLGetExporterProperty(
			int hDXLExport,
			int prop,
			Memory retPropValue);

	public native short DXLSetExporterProperty(
			int hDXLExport,
			int prop,
			Memory propValue);

	public native short DXLCreateImporter(IntByReference prethDXLImport);
	
	public native void DXLDeleteImporter(int hDXLImport);
	
	public native short DXLImportWasErrorLogged(int hDXLImport);

	public native short DXLGetImporterProperty(
			int hDXLImporter,
			int prop,
			Memory retPropValue);
	
	public native short DXLSetImporterProperty(
			int hDXLImport,
			int prop,
			Memory propValue);
	
	public native short XSLTTransform(
			int hXSLTransform,
			NotesCallbacks.XML_READ_FUNCTION pXSL_XMLInputFunc,
			Pointer pXSL_XMLInputAction,
			NotesCallbacks.XML_READ_FUNCTION  pXSL_StylesheetInputFunc,
			Pointer pXSL_StylesheetInputAction,
			NotesCallbacks.XML_WRITE_FUNCTION  pXSL_TransformOutputFunc,
			Pointer pXSL_TransformOutputAction);
	
	public native short XSLTCreateTransform(IntByReference prethXSLTransform);
	
	public native short MMCreateConvControls(
			PointerByReference phCC);

	public native short MMDestroyConvControls(
			Pointer hCC);
	
	public native void MMConvDefaults(
			Pointer hCC);

	public native short MMGetAttachEncoding(
			Pointer hCC);
	
	public native void MMSetAttachEncoding(
			Pointer hCC,
			short wAttachEncoding);
	
	public native void MMSetDropItems(
			Pointer hCC,
			Memory pszDropItems);
	
	public native Pointer MMGetDropItems(
			Pointer hCC);
	
	public native void MMSetKeepTabs(
			Pointer hCC,
			boolean bKeepTabs);
	
	public native boolean MMGetKeepTabs(
			Pointer hCC);
	
	public native void MMSetPointSize(
			Pointer hCC,
			short wPointSize);

	public native short MMGetPointSize(
			Pointer hCC);
	
	public native short MMGetTypeFace(
			Pointer hCC);
	
	public native void MMSetTypeFace(
			Pointer hCC,
			short wTypeFace);

	public native void MMSetAddItems(
			Pointer hCC,
			Memory pszAddItems);

	public native Pointer MMGetAddItems(
			Pointer hCC);
	
	public native void MMSetMessageContentEncoding(
			Pointer hCC,
			short wMessageContentEncoding);

	public native short MMGetMessageContentEncoding(
			Pointer hCC);
	
	public native void MMSetReadReceipt(
			Pointer hCC,
			short wReadReceipt);

	public native short MMGetReadReceipt(
			Pointer hCC);
	
	public native void MMSetSkipX(
			Pointer hCC,
			boolean bSkipX);
	
	public native boolean MMGetSkipX(
			Pointer hCC);

	public native int MIMEStreamPutLine(
			Memory pszLine,
			Pointer hMIMEStream);

	public native int MIMEStreamRead(
			Memory pchData,
			IntByReference puiDataLen,
			int uiMaxDataLen,
			Pointer hMIMEStream);	
	
	public native int MIMEStreamRewind(
			Pointer hMIMEStream);

	public native int MIMEStreamWrite(
			Memory pchData,
			int  uiDataLen,
			Pointer hMIMEStream);

	public native void MIMEStreamClose(
			Pointer hMIMEStream);

	public native short NSFProfileNameToProfileNoteName(
            Memory ProfileName, short ProfileNameLength,
            Memory UserName, short UserNameLength, boolean bLeaveCase, Memory ProfileNoteName);

	public native short NSFDbModifiedTimeByName(
			Memory DbName,
			NotesTimeDateStruct retDataModified,
			NotesTimeDateStruct retNonDataModified);

	public native short NSFDbDelete(
			Memory PathName
			);

	public native void DesignGetNameAndAlias(Memory pString, PointerByReference ppName, ShortByReference pNameLen, PointerByReference ppAlias, ShortByReference pAliasLen);

	@Override
	public native boolean StoredFormHasSubformToken(Memory pString);

	public native void SECKFMCreatePassword(Memory pPassword, ByReference retHashedPassword);

	@Override
	public native boolean CmemflagTestMultiple (Pointer s, short length, Pointer pattern);

	@Override public native short QueueCreate(com.mindoo.domino.jna.internal.handles.DHANDLE.ByReference qhandle);

	@Override public native short QueueGet(com.mindoo.domino.jna.internal.handles.DHANDLE.ByValue qhandle, DHANDLE.ByReference sehandle);

	@Override public native short QueueDelete(DHANDLE.ByValue qhandle);

	@Override public native short NSFRemoteConsoleAsync(Memory serverName, Memory ConsoleCommand, int Flags,
			com.mindoo.domino.jna.internal.handles.DHANDLE.ByReference phConsoleText,
			com.mindoo.domino.jna.internal.handles.DHANDLE.ByReference phTasksText,
			com.mindoo.domino.jna.internal.handles.DHANDLE.ByReference phUsersText, ShortByReference pSignals,
			IntByReference pConsoleBufferID, com.mindoo.domino.jna.internal.handles.DHANDLE.ByValue hQueue,
			ASYNCNOTIFYPROC Proc, Pointer param, PointerByReference retactx);

	@Override public native short NSFRemoteConsole(Memory ServerName, Memory ConsoleCommand,
			DHANDLE.ByReference hResponseText);

	@Override public native void NSFAsyncNotifyPoll(Pointer actx, IntByReference retMySessions, ShortByReference retFirstError);

	@Override public native void NSFUpdateAsyncIOStatus(Pointer actx);

	@Override public native void NSFCancelAsyncIO(Pointer actx);

}
