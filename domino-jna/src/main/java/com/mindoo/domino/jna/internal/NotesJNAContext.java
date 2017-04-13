package com.mindoo.domino.jna.internal;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import com.mindoo.domino.jna.errors.UnsupportedPlatformError;
import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

/**
 * Class containing a singleton instance to the JNA class wrapping the Notes C API
 * 
 * @author Karsten Lehmann
 */
public class NotesJNAContext {
	private static volatile NotesCAPI m_api;
	private static volatile Boolean m_is64Bit;
	
	/**
	 * Checks if the current JVM is running in 64 bit mode
	 * 
	 * @return true if 64 bit
	 */
	public static boolean is64Bit() {
		if (m_is64Bit==null) {
			String arch = System.getProperty("os.arch");
			m_is64Bit = "xmd64".equals(arch) || "x86_64".equals(arch) || "amd64".equals(arch);
		}
		return m_is64Bit;
	}
	
	/**
	 * Returns the {@link NotesCAPI} singleton instance to call C methods
	 * 
	 * @return API
	 */
	@SuppressWarnings("unchecked")
	public static NotesCAPI getNotesAPI()  {
		if (m_api==null) {
			synchronized (NotesJNAContext.class) {
				if (m_api==null) {
					try {
						m_api = AccessController.doPrivileged(new PrivilegedExceptionAction<NotesCAPI>() {

							@Override
							public NotesCAPI run() throws Exception {
								Exception t = null;
								for (int i=0; i<3; i++) {
									try {
										String osName = System.getProperty("os.name");
										Native.setProtected(true);
										@SuppressWarnings("rawtypes")
										Map options = new HashMap();
										options.put(Library.OPTION_FUNCTION_MAPPER, new FunctionMapper() {
											//use different methods for 32 and 64 bit
											@Override
											public String getFunctionName(NativeLibrary library, Method method) {
												String methodName = method.getName();
												if (methodName.startsWith("b32_") || methodName.startsWith("b64_")) {
													return methodName.substring(4);
												}
												else
													return methodName;
											}
										});
										
										try {
											String osNameLC = osName.toLowerCase();
											
											if (osNameLC.startsWith("windows")) {
												return (NotesCAPI) Native.loadLibrary("nnotes", WinNotesCAPI.class, options);
											}
											else if (osNameLC.startsWith("mac")) {
												return (NotesCAPI) Native.loadLibrary("notes", MacNotesCAPI.class, options);
											}
											else if (osNameLC.startsWith("linux")) {
												return (NotesCAPI) Native.loadLibrary("notes", NotesCAPI.class, options);
											}
											else {
												throw new UnsupportedPlatformError("Platform is unknown or not supported: "+osName);
											}
										}
										catch (UnsatisfiedLinkError e) {
											System.out.println("Could not load notes native library.\nEnvironment: "+System.getenv());
											throw e;
										}
									}
									catch (Exception currError) {
										t = currError;
									}
								}
								if (t!=null)
									throw t;
								else
									throw new RuntimeException();
							}
						});
					} catch (Throwable e) {
						if (e instanceof UnsupportedPlatformError)
							throw (UnsupportedPlatformError) e;
						
						throw new RuntimeException("Could not initialize Domino JNA API", e);
					}
				}
			}
		}
		return m_api;
	}
	
}
