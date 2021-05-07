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
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * Class providing C methods for both 32 and 64 bit. Should be used internally by
 * the Domino JNA API methods only.
 * 
 * @author Karsten Lehmann
 */
public class NotesNativeAPI {
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
	
}
