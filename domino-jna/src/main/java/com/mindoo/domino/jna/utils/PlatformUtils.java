
package com.mindoo.domino.jna.utils;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import com.mindoo.domino.jna.constants.OSDirectory;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.sun.jna.Memory;

/**
 * Utility functions for the Domino platform and its OS platform
 */
public class PlatformUtils {
	private static boolean m_is64Bit;
	private static boolean m_isWindows;
	private static boolean m_isMac;
	private static boolean m_isLinux;

	static {
		String arch = System.getProperty("os.arch");
		m_is64Bit = "xmd64".equals(arch) || "x86_64".equals(arch) || "amd64".equals(arch);

		String osName = System.getProperty("os.name");
		String osNameLC = osName.toLowerCase();

		if (osNameLC.startsWith("windows")) {
			m_isWindows = true;
		}
		else if (osNameLC.startsWith("mac")) {
			m_isMac = true;
		}
		else if (osNameLC.startsWith("linux")) {
			m_isLinux = true;
		}
	}
	
	/**
	 * This function returns the path specification of the local Domino or Notes
	 * executable / data / temp directory.<br>
	 * <br>
	 * Author: Ulrich Krause
	 * 
	 * @param osDirectory
	 *            {@link OSDirectory}
	 * @return path
	 */
	public static String getOsDirectory(OSDirectory osDirectory) {
		Memory retPathName = new Memory(NotesConstants.MAXPATH);
		switch (osDirectory) {
		case EXECUTABLE:
			NotesNativeAPI.get().OSGetExecutableDirectory(retPathName);
			break;
		case DATA:
			NotesNativeAPI.get().OSGetDataDirectory(retPathName);
			break;
		case TEMP:
			NotesNativeAPI.get().OSGetSystemTempDirectory(retPathName, NotesConstants.MAXPATH);
			break;
		case VIEWREBUILD:
			NotesNativeAPI.get().NIFGetViewRebuildDir(retPathName, NotesConstants.MAXPATH);
			break;
// TODO commented out, not compatible with later Notes version
//		case DAOS:
//			NotesNativeAPI.get().DAOSGetBaseStoragePath(retPathName, NotesConstants.MAXPATH);
//			break;			
		default:
			throw new IllegalArgumentException("Unsupported directory type: "+osDirectory);
		}
		NotesNativeAPI.get().OSPathAddTrailingPathSep(retPathName);
		return NotesStringUtils.fromLMBCS(retPathName, -1);
	}

	/**
	 * Checks if the current JVM is running in 32 bit mode
	 * 
	 * @return true if 32 bit
	 */
	public static boolean is32Bit() {
		return !m_is64Bit;
	}

	/**
	 * Checks if the current JVM is running in 64 bit mode
	 * 
	 * @return true if 64 bit
	 */
	public static boolean is64Bit() {
		return m_is64Bit;
	}

	/**
	 * Checks if we are running in a Windows 32 bit environment
	 * 
	 * @return true if win32
	 */
	public static boolean isWin32() {
		return isWindows() && is32Bit();
	}
	
	/**
	 * Method to check if we are running in a Windows environment
	 * 
	 * @return true if Windows
	 */
	public static boolean isWindows() {
		return m_isWindows;
	}

	/**
	 * Method to check if we are running in a Mac environment
	 * 
	 * @return true if Mac
	 */
	public static boolean isMac() {
		return m_isMac;
	}

	/**
	 * Method to check if we are running in a Linux environment
	 * 
	 * @return true if Linux
	 */
	public static boolean isLinux() {
		return m_isLinux;
	}

	public static enum NSDMode { 
		RUN_ALL(NotesConstants.FR_RUN_ALL),
		RUN_CLEANUPSCRIPT_ONLY(NotesConstants.FR_RUN_CLEANUPSCRIPT_ONLY),
		RUN_NSD_ONLY(NotesConstants.FR_RUN_NSD_ONLY),
		DONT_RUN_ANYTHING(NotesConstants.FR_DONT_RUN_ANYTHING),
		SHUTDOWN_HANG(NotesConstants.FR_SHUTDOWN_HANG),
		PANIC_DIRECT(NotesConstants.FR_PANIC_DIRECT),
		RUN_QOS_NSD(NotesConstants.FR_RUN_QOS_NSD),
		NSD_AUTOMONITOR(NotesConstants.FR_NSD_AUTOMONITOR);
		
		private short m_modeAsShort;
		
		private NSDMode(short modeAsShort) {
			m_modeAsShort = modeAsShort;
		}
		
		public short getValue() {
			return m_modeAsShort;
		}
		
		public static short toBitMask(EnumSet<NSDMode> mode) {
			int result = 0;
			if (mode!=null) {
				for (NSDMode currMode : values()) {
					if (mode.contains(currMode)) {
						result = result | currMode.getValue();
					}
				}
			}
			return (short) (result & 0xffff);
		}

	}

	/**
	 * Runs an NSD locally.
	 * 
	 * @param mode NSD mode
	 */
	public static void runNSD(EnumSet<NSDMode> mode) {
		runNSD("", mode);
	}
	
	/**
	 * Runs an NSD. Please note that we could not yet get this method to work with a remote server name.
	 * That's why the method is private. We always goto an error message
	 * "%p Author %,lu %,lu %,lu %s %s %p %s" back when we called the method with a server name
	 * other than "".
	 * 
	 * @param serverName server, either abbreviated or canonical, use "" for the local environment
	 * @param mode NSD mode
	 */
	private static void runNSD(String serverName, EnumSet<NSDMode> mode) {
		short modeAsShort = NSDMode.toBitMask(mode);

		Memory serverNameMem = NotesStringUtils.toLMBCS(NotesNamingUtils.toCanonicalName(serverName), true);
		
		short result = NotesNativeAPI.get().OSRunNSDExt(serverNameMem, modeAsShort);
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Runs a block of code with suppressed {@link SecurityManager}
	 * 
	 * @param <T> result type
	 * @param action action to run
	 * @return result
	 */
	public static <T> T runPrivileged(PrivilegedAction<T> action) {
		return AccessController.doPrivileged(action);
	}

	/**
	 * Runs a block of code with suppressed {@link SecurityManager}
	 * 
	 * @param <T> result type
	 * @param action action to run
	 * @return result
	 * @throws PrivilegedActionException
	 */
	public static <T> T runPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
		return AccessController.doPrivileged(action);
	}

	/**
	 * Calls {@link System#getProperties()} in an {@link AccessController#doPrivileged(PrivilegedAction)}
	 * block.
	 * 
	 * @return system properties
	 */
	public static Properties getSystemProperties() {
		return AccessController.doPrivileged(new PrivilegedAction<Properties>() {

			@Override
			public Properties run() {
				return System.getProperties();
			}
		});
	}
	
	/**
	 * Calls {@link System#getenv()} in an {@link AccessController#doPrivileged(PrivilegedAction)}
	 * block.
	 * 
	 * @return system environment
	 */
	public static Map<String,String> getSystemEmv() {
		return AccessController.doPrivileged(new PrivilegedAction<Map<String,String>>() {

			@Override
			public Map<String,String> run() {
				return System.getenv();
			}
		});
	}

	/**
	 * Invokes the {@link ServiceLoader} within an {@link AccessController#doPrivileged(PrivilegedAction)}
	 * block to find service implementations.
	 * 
	 * @param <T> service type
	 * @param clazz class of service
	 * @return iterator with implementations
	 */
	public static <T> Iterator<T> getService(Class<T> clazz) {
		return AccessController.doPrivileged(new PrivilegedAction<Iterator<T>>() {

			@Override
			public Iterator<T> run() {
				Iterator<T> wrappedIt = ServiceLoader.load(clazz).iterator();
				return new Iterator<T>() {

					@Override
					public boolean hasNext() {
						return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

							@Override
							public Boolean run() {
								return wrappedIt.hasNext();
							}
						});
					}

					@Override
					public T next() {
						return AccessController.doPrivileged(new PrivilegedAction<T>() {

							@Override
							public T run() {
								return wrappedIt.next();
							}
						});
					}
					
				};
			}
		});
	}
}