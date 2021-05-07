package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.NotesGC;

/**
 * Class providing C methods for 32 bit. Should be used internally by
 * the Domino JNA API methods only.
 * 
 * @author Karsten Lehmann
 */
public class NotesNativeAPI32 {
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
	
}
