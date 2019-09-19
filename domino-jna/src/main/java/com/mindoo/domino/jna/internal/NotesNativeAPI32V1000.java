package com.mindoo.domino.jna.internal;

import java.util.Map;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.INotesNativeAPI.Mode;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.ptr.IntByReference;

public class NotesNativeAPI32V1000 implements INotesNativeAPI32V1000 {
	private static volatile INotesNativeAPI32V1000 m_instanceWithoutCrashLogging;
	private static volatile INotesNativeAPI32V1000 m_instanceWithCrashLogging;

	/**
	 * Gets called from {@link NotesNativeAPI#initialize()}
	 * 
	 * @param instance
	 */
	static void set(INotesNativeAPI32V1000 instance) {
		m_instanceWithoutCrashLogging = instance;
	}

	@Override
	public native short NSFQueryDB(int hDb, Memory query, int flags, int maxDocsScanned, int maxEntriesScanned, int maxMsecs,
			IntByReference retResults, IntByReference retError, IntByReference retExplain);
	
	@Override
	public native short NSFGetSoftDeletedViewFilter(int hViewDB, int hDataDB, int viewNoteID, IntByReference hFilter);
	
	@Override
	public native short NSFDbLargeSummaryEnabled(int hDB);

	@Override
	public native short NSFDesignHarvest(int hDB, int flags);

	
	/**
	 * Returns the API instance used to call native Domino C API methods for 32 bit
	 * 
	 * @return API
	 */
	public static INotesNativeAPI32V1000 get() {
		NotesGC.ensureRunningInAutoGC();

		if (NotesNativeAPI.m_initError!=null) {
			if (NotesNativeAPI.m_initError instanceof RuntimeException)
				throw (RuntimeException) NotesNativeAPI.m_initError;
			else
				throw new NotesError(0, "Error initializing Domino JNA API", NotesNativeAPI.m_initError);
		}
		
		if (m_instanceWithoutCrashLogging==null) {
			Mode jnaMode = NotesNativeAPI.get().getActiveJNAMode();
			Map<String,Object> libraryOptions = NotesNativeAPI.get().getLibraryOptions();
			
			if (jnaMode==Mode.Direct) {
				NativeLibrary library;
				if (PlatformUtils.isWindows()) {
			        library = NativeLibrary.getInstance("nnotes", libraryOptions);
				}
				else {
			        library = NativeLibrary.getInstance("notes", libraryOptions);
				}
				
				Native.register(NotesNativeAPI32V1000.class, library);

				NotesNativeAPI32V1000 instance = new NotesNativeAPI32V1000();
				return instance;
			}
			else {
				INotesNativeAPI32V1000 api;
				if (PlatformUtils.isWindows()) {
					api = Native.loadLibrary("nnotes", INotesNativeAPI32V1000.class, libraryOptions);
				}
				else {
					api = Native.loadLibrary("notes", INotesNativeAPI32V1000.class, libraryOptions);
				}

				return api;
			}
		}
		
		if (NotesGC.isLogCrashingThreadStacktrace()) {
			if (m_instanceWithCrashLogging==null) {
				m_instanceWithCrashLogging = NotesNativeAPI.wrapWithCrashStackLogging(INotesNativeAPI32V1000.class, 
						m_instanceWithoutCrashLogging);
			}
			return m_instanceWithCrashLogging;
		}
		else {
			return m_instanceWithoutCrashLogging;
		}
	}
}
