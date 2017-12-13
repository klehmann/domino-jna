package com.mindoo.domino.jna.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import com.mindoo.domino.jna.errors.UnsupportedPlatformError;
import com.mindoo.domino.jna.gc.NotesGC;
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
	private static volatile NotesCAPI m_apiWithCrashStackLogging;
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
		if (NotesGC.isLogCrashingThreadStacktrace()) {
			if (m_apiWithCrashStackLogging==null) {
				initAPI();
				m_apiWithCrashStackLogging = wrapWithCrashStackLogging(m_api);
			}
			return m_apiWithCrashStackLogging;

		}
		else {
			if (m_api==null) {
				initAPI();
			}
			return m_api;
		}
	}
	
	/**
	 * Wraps the specified API object to dump caller stacktraces right before invoking
	 * native methods
	 * 
	 * @param api API
	 * @return wrapped API
	 */
	private static NotesCAPI wrapWithCrashStackLogging(final NotesCAPI api) {
		final Class<?>[] interfaces;
		if (api instanceof WinNotesCAPI) {
			interfaces = new Class[] {NotesCAPI.class, WinNotesCAPI.class};
		}
		else if (api instanceof MacNotesCAPI) {
			interfaces = new Class[] {NotesCAPI.class, MacNotesCAPI.class};
		}
		else {
			interfaces = new Class[] {NotesCAPI.class};
		}
		
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<NotesCAPI>() {

				@Override
				public NotesCAPI run() throws Exception {
					return (NotesCAPI) Proxy.newProxyInstance(api.getClass().getClassLoader(), interfaces, new InvocationHandler() {
						
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							Exception e = new Exception();
							e.fillInStackTrace();
							
							File stFile = createStackTraceFile(e);
							try {
								return method.invoke(api, args);
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
									String tmpDirPath = System.getProperty("java.io.tmpdir");
									File tmpDir = new File(tmpDirPath);
									File stFile = new File(tmpDir, "domino-jna-stack-"+Thread.currentThread().getId()+".txt");
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
					});
				}
			});
		} catch (PrivilegedActionException e) {
			e.printStackTrace();
			return api;
		}
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
