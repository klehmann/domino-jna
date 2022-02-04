package com.mindoo.domino.jna.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Native;

public class NotesNativeAPI32V1000 {
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
			m_instanceWithoutCrashLogging = AccessController.doPrivileged(new PrivilegedAction<INotesNativeAPI32V1000>() {

				@Override
				public INotesNativeAPI32V1000 run() {
					Map<String,Object> libraryOptions = NotesNativeAPI.getLibraryOptions();
					
					INotesNativeAPI32V1000 api;
					if (PlatformUtils.isWindows()) {
						api = Native.loadLibrary("nnotes", INotesNativeAPI32V1000.class, libraryOptions);
					}
					else {
						api = Native.loadLibrary("notes", INotesNativeAPI32V1000.class, libraryOptions);
					}

					return api;
				}
			});
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
