package com.mindoo.domino.jna.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.handles.DHANDLE.ByReference;
import com.mindoo.domino.jna.internal.handles.HANDLE.ByValue;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public class NotesNativeAPIV1200 implements INotesNativeAPIV1200 {
	private static volatile INotesNativeAPIV1200 m_instanceWithoutCrashLogging;
	private static volatile INotesNativeAPIV1200 m_instanceWithCrashLogging;

	/**
	 * Gets called from {@link NotesNativeAPI#initialize()}
	 * 
	 * @param instance
	 */
	static void set(INotesNativeAPIV1200 instance) {
		m_instanceWithoutCrashLogging = instance;
	}

	/**
	 * Returns the API instance used to call native Domino C API methods for 32 bit
	 * 
	 * @return API
	 */
	public static INotesNativeAPIV1200 get() {
		NotesGC.ensureRunningInAutoGC();

		if (NotesNativeAPI.m_initError!=null) {
			if (NotesNativeAPI.m_initError instanceof RuntimeException)
				throw (RuntimeException) NotesNativeAPI.m_initError;
			else
				throw new NotesError(0, "Error initializing Domino JNA API", NotesNativeAPI.m_initError);
		}
		
		if (m_instanceWithoutCrashLogging==null) {
			m_instanceWithoutCrashLogging = AccessController.doPrivileged(new PrivilegedAction<INotesNativeAPIV1200>() {

				@Override
				public INotesNativeAPIV1200 run() {
					Map<String,Object> libraryOptions = NotesNativeAPI.getLibraryOptions();
					
					INotesNativeAPIV1200 api;
					if (PlatformUtils.isWindows()) {
						api = Native.loadLibrary("nnotes", INotesNativeAPIV1200.class, libraryOptions);
					}
					else {
						api = Native.loadLibrary("notes", INotesNativeAPIV1200.class, libraryOptions);
					}

					return api;
				}
			});
		}
		
		if (NotesGC.isLogCrashingThreadStacktrace()) {
			if (m_instanceWithCrashLogging==null) {
				m_instanceWithCrashLogging = NotesNativeAPI.wrapWithCrashStackLogging(INotesNativeAPIV1200.class, 
						m_instanceWithoutCrashLogging);
			}
			return m_instanceWithCrashLogging;
		}
		else {
			return m_instanceWithoutCrashLogging;
		}
	}

	@Override
	public native short NSFProcessResults(ByValue hDb, Memory viewname, int dwFlags, int hInResults, int hOutFields,
			int hFieldRules, int hCombineRules, IntByReference hErrorText, ByReference phStreamedhQueue);

	@Override
	public native short NSFQueryAddToResultsList(int type, Pointer pInEntry, Pointer phEntryList, IntByReference phErrorText);
}
