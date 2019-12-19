package com.mindoo.domino.jna.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.INotesNativeAPI.Mode;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

public class NotesNativeAPIV901 implements INotesNativeAPIV901 {
	private static volatile INotesNativeAPIV901 m_instanceWithoutCrashLogging;
	private static volatile INotesNativeAPIV901 m_instanceWithCrashLogging;

	/**
	 * Gets called from {@link NotesNativeAPI#initialize()}
	 * 
	 * @param instance
	 */
	static void set(INotesNativeAPIV901 instance) {
		m_instanceWithoutCrashLogging = instance;
	}

	@Override
	public native short CalGetApptunidFromUID(Memory pszUID, Memory pszApptunid, int dwFlags, Pointer pCtx);

	/**
	 * Returns the API instance used to call native Domino C API methods for 32 bit
	 * 
	 * @return API
	 */
	public static INotesNativeAPIV901 get() {
		NotesGC.ensureRunningInAutoGC();

		if (NotesNativeAPI.m_initError!=null) {
			if (NotesNativeAPI.m_initError instanceof RuntimeException)
				throw (RuntimeException) NotesNativeAPI.m_initError;
			else
				throw new NotesError(0, "Error initializing Domino JNA API", NotesNativeAPI.m_initError);
		}
		
		if (m_instanceWithoutCrashLogging==null) {
			m_instanceWithoutCrashLogging = AccessController.doPrivileged(new PrivilegedAction<INotesNativeAPIV901>() {

				@Override
				public INotesNativeAPIV901 run() {
					Mode jnaMode = NotesNativeAPI.getActiveJNAMode();
					Map<String,Object> libraryOptions = NotesNativeAPI.getLibraryOptions();
					
					if (jnaMode==Mode.Direct) {
						NativeLibrary library;
						if (PlatformUtils.isWindows()) {
					        library = NativeLibrary.getInstance("nnotes", libraryOptions);
						}
						else {
					        library = NativeLibrary.getInstance("notes", libraryOptions);
						}
						
						Native.register(NotesNativeAPIV901.class, library);

						NotesNativeAPIV901 instance = new NotesNativeAPIV901();
						return instance;
					}
					else {
						INotesNativeAPIV901 api;
						if (PlatformUtils.isWindows()) {
							api = Native.loadLibrary("nnotes", INotesNativeAPIV901.class, libraryOptions);
						}
						else {
							api = Native.loadLibrary("notes", INotesNativeAPIV901.class, libraryOptions);
						}

						return api;
					}
				}
			});
		}
		
		if (NotesGC.isLogCrashingThreadStacktrace()) {
			if (m_instanceWithCrashLogging==null) {
				m_instanceWithCrashLogging = NotesNativeAPI.wrapWithCrashStackLogging(INotesNativeAPIV901.class, 
						m_instanceWithoutCrashLogging);
			}
			return m_instanceWithCrashLogging;
		}
		else {
			return m_instanceWithoutCrashLogging;
		}
	}
}
