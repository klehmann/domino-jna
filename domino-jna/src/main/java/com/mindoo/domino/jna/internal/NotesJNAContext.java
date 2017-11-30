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
import com.sun.jna.Structure;

/**
 * Class containing a singleton instance to the JNA class wrapping the Notes C API
 * 
 * @author Karsten Lehmann
 */
public class NotesJNAContext {
	private static volatile boolean m_initialized;
	private static volatile NotesCAPI m_api;
	private static volatile Boolean m_is64Bit;
	private static volatile Integer m_platformAlignment;
	
	/**
	 * Checks if the current JVM is running in 64 bit mode
	 * 
	 * @return true if 64 bit
	 */
	public static boolean is64Bit() {
		if (m_is64Bit==null) {
			initAPI();
		}
		return m_is64Bit;
	}
	
	/**
	 * Returns the alignment to be used for the current platform
	 * @return alignment
	 */
	public static int getPlatformAlignment() {
		if (m_platformAlignment==null) {
			initAPI();
		}
		return m_platformAlignment;
	}

	/**
	 * Returns the {@link NotesCAPI} singleton instance to call C methods
	 * 
	 * @return API
	 */
	public static NotesCAPI getNotesAPI()  {
		if (m_api==null) {
			initAPI();
		}
		return m_api;
	}
	
	/**
	 * Returns the {@link NotesCAPI} singleton instance to call C methods
	 * 
	 * @return API
	 */
	@SuppressWarnings("unchecked")
	private static void initAPI()  {
		if (!m_initialized) {
			synchronized (NotesJNAContext.class) {
				if (!m_initialized) {
					try {
						 AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {

							@Override
							public Object run() throws Exception {
								//enforce using the extracted JNA .dll/.so instead of what we find on the PATH
								System.setProperty("jna.nosys", "true");
								
								Exception t = null;
								for (int i=0; i<3; i++) {
									try {
										String arch = System.getProperty("os.arch");
										m_is64Bit = "xmd64".equals(arch) || "x86_64".equals(arch) || "amd64".equals(arch);

										String osName = System.getProperty("os.name");
										
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
												m_api=(NotesCAPI) Native.loadLibrary("nnotes", WinNotesCAPI.class, options);
												if (Boolean.TRUE.equals(m_is64Bit)) {
													m_platformAlignment = Structure.ALIGN_DEFAULT;
												}
												else {
													m_platformAlignment = Structure.ALIGN_NONE;
												}
												return null;
											}
											else if (osNameLC.startsWith("mac")) {
												m_api=(NotesCAPI) Native.loadLibrary("notes", MacNotesCAPI.class, options);
												if (Boolean.TRUE.equals(m_is64Bit)) {
													m_platformAlignment = Structure.ALIGN_NONE;
												}
												else {
													m_platformAlignment = Structure.ALIGN_DEFAULT;
												}
												return null;
											}
											else if (osNameLC.startsWith("linux")) {
												m_api=(NotesCAPI) Native.loadLibrary("notes", NotesCAPI.class, options);
												m_platformAlignment = Structure.ALIGN_DEFAULT;
												return null;
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
	}
	
}
