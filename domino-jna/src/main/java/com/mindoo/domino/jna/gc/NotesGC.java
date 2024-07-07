package com.mindoo.domino.jna.gc;

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.DHANDLE32;
import com.mindoo.domino.jna.internal.handles.DHANDLE64;
import com.mindoo.domino.jna.utils.PlatformUtils;

/**
 * Utility class to simplify memory management with Notes handles. The class tracks
 * handle creation and disposal.<br>
 * By using {@link #runWithAutoGC(Callable)}, the
 * collected handles are automatically disposed when code execution is done.<br>
 * <br>
 * An alternative approach is to use a try-with-resources block on the {@link DominoGCContext}
 * returned by {@link #initThread()}, e.g.<br>
 * <br>
 * <code>
 * try (DominoGCContext ctx = initThread()) {<br>
 * &nbsp;&nbsp;&nbsp;// use Domino JNA classes, e.g.<br>
 * &nbsp;&nbsp;&nbsp;NotesDatabase db = new NotesDatabase("", "names.nsf", "");<br>
 * <br>
 * } catch (Exception e) {<br>
 * &nbsp;&nbsp;&nbsp;log(Level.SEVERE, "Error accessing Domino data", e);<br>
 * }<br>
 * </code>
 * 
 * @author Karsten Lehmann
 */
public class NotesGC {
	private static ThreadLocal<DominoGCContext> threadContext = new ThreadLocal<>();

	/**
	 * Returns the GC context for the current thread
	 * 
	 * @return context
	 * @throws IllegalStateException if no context is active
	 */
	private static DominoGCContext getThreadContext() {
		DominoGCContext ctx = threadContext.get();
		if (ctx==null) {
			throw new IllegalStateException("Thread is not enabled for auto GC. Either run your code via NotesGC.runWithAutoGC(Callable) or via try-with-resources on the object returned by NotesGC.initThread().");
		}
		return ctx;
	}
	
	/**
	 * Method to enable GC debug logging for the active thread's {@link DominoGCContext}
	 * 
	 * @param enabled true if enabled
	 */
	public static void setDebugLoggingEnabled(boolean enabled) {
		DominoGCContext ctx = threadContext.get();
		if (ctx!=null) {
			ctx.setDebugLoggingEnabled(enabled);
		}
	}
	
	/**
	 * Method to check if GC debug logging is enabled for the active thread's {@link DominoGCContext}
	 * is enabled.
	 * 
	 * @return true if enabled
	 */
	public static boolean isDebugLoggingEnabled() {
		DominoGCContext ctx = threadContext.get();
		if (ctx==null) {
			return false;
		}
		else {
			return ctx.isDebugLoggingEnabled();
		}
	}
	
	public static boolean isPreferNotesTimeDate() {
		DominoGCContext ctx = threadContext.get();
		if (ctx==null) {
			return false;
		}
		else {
			return ctx.isPreferNotesTimeDate();
		}
	}
	
	public static boolean isFixupLocalServerNames() {
		DominoGCContext ctx = threadContext.get();
		if (ctx==null) {
			return false;
		}
		else {
			return ctx.isFixupLocalServerNames();
		}
	}
	
	/**
	 * Activates automatic correction that when Domino JNA is running server side,
	 * we change an empty string as a server name "" into the actual server name on DB open.
	 * This is required when Domino JNA
	 * is used in a standalone application against a running Domino server so
	 * that Domino can coordinate NSF file access. Otherwise opening databases might
	 * fail with the error message that the NSF is in use.
	 * 
	 * @param b true to fix server, false by default
	 */
	public static void setFixupLocalServerNames(boolean b) {
		DominoGCContext ctx = threadContext.get();
		if (ctx!=null) {
			ctx.setFixupLocalServerNames(b);
		}
	}
	
	/**
	 * General switch to prefer {@link NotesTimeDate} as return values for various functions,
	 * e.g. formula execution and view lookups.<br>
	 * If not set, those functions will return {@link Calendar} objects because in the
	 * early days of this project our {@link NotesTimeDate} implementation was not powerful
	 * enough. If there will ever be a major rewrite of this project, we will always return
	 * {@link NotesTimeDate} objects for dates/times.
	 * 
	 * @param b true to prefer {@link NotesTimeDate}
	 */
	public static void setPreferNotesTimeDate(boolean b) {
		DominoGCContext ctx = threadContext.get();
		if (ctx!=null) {
			ctx.setPreferNotesTimeDate(b);
		}
	}
	
	/**
	 * Method to write a stacktrace to disk right before each native method invocation. Consumes
	 * much performance and is therefore disabled by default and just here to track down
	 * handle panics.<br>
	 * Stacktraces are written as files domino-jna-stack-&lt;threadid&gt;.txt in the temp directory.<br>
	 * <br>
	 * Logging is enabled for the active thread's {@link DominoGCContext}.
	 * 
	 * @param log true to log
	 */
	public static void setLogCrashingThreadStacktrace(boolean log) {
		DominoGCContext ctx = threadContext.get();
		if (ctx!=null) {
			ctx.setLogCrashingThreadStackTrace(log);
		}
	}
	
	/**
	 * Checks whether stacktraces for each native method invocation should be written to disk. Consumes
	 * much performance and is therefore disabled by default and just here to track down
	 * handle panics.<br>
	 * Stacktraces are written as files domino-jna-stack-&lt;threadid&gt;.txt in the temp directory.<br>
	 * <br>
	 * Logging is enabled for the active thread's {@link DominoGCContext}.
	 * 
	 * @return true to log
	 */
	public static boolean isLogCrashingThreadStacktrace() {
		DominoGCContext ctx = threadContext.get();
		if (ctx==null) {
			return false;
		}
		else {
			return ctx.isLogCrashingThreadStackTrace();
		}
	}
	
	/**
	 * Method to get the current count of open Domino object handles
	 * 
	 * @return handle count
	 */
	public static int getNumberOfOpenObjectHandles() {
		DominoGCContext ctx = getThreadContext();

		if (PlatformUtils.is64Bit()) {
			return ctx.getOpenHandlesDominoObjects64().size();
		}
		else {
			return ctx.getOpenHandlesDominoObjects32().size();
		}
	}

	/**
	 * Method to get the current count of open Domino memory handles
	 * 
	 * @return handle count
	 */
	public static int getNumberOfOpenMemoryHandles() {
		DominoGCContext ctx = getThreadContext();

		if (PlatformUtils.is64Bit()) {
			return ctx.getOpenHandlesMemory64().size();
		}
		else {
			return ctx.getOpenHandlesMemory32().size();
		}
	}

	public static class HashKey64 {
		private Class<?> m_clazz;
		private long m_handle;
		
		public HashKey64(Class<?> clazz, long handle) {
			m_clazz = clazz;
			m_handle = handle;
		}
		
		public long getHandle() {
			return m_handle;
		}
		
		public Class<?> getType() {
			return m_clazz;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((m_clazz == null) ? 0 : m_clazz.hashCode());
			result = prime * result + (int) (m_handle ^ (m_handle >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			HashKey64 other = (HashKey64) obj;
			if (m_clazz == null) {
				if (other.m_clazz != null)
					return false;
			} else if (!m_clazz.equals(other.m_clazz))
				return false;
			if (m_handle != other.m_handle)
				return false;
			return true;
		}
	}

	public static class HashKey32 {
		private Class<?> m_clazz;
		private int m_handle;

		public HashKey32(Class<?> clazz, int handle) {
			m_clazz = clazz;
			m_handle = handle;
		}
		
		public int getHandle() {
			return m_handle;
		}
		
		public Class<?> getType() {
			return m_clazz;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((m_clazz == null) ? 0 : m_clazz.hashCode());
			result = prime * result + m_handle;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			HashKey32 other = (HashKey32) obj;
			if (m_clazz == null) {
				if (other.m_clazz != null)
					return false;
			} else if (!m_clazz.equals(other.m_clazz))
				return false;
			if (m_handle != other.m_handle)
				return false;
			return true;
		}
	}

	/**
	 * Internal method to register a created Notes object that needs to be recycled
	 * 
	 * @param clazz class of hash pool
	 * @param obj Notes object
	 */
	public static void __objectCreated(Class<?> clazz, IRecyclableNotesObject obj) {
		__objectCreated(clazz, obj, false);
	}
	
	/**
	 * Internal method to register a created Notes object that needs to be recycled
	 * 
	 * @param clazz class of hash pool
	 * @param skipDuplicateHandleCheck to skip checking for a duplicate handle registration, used for shared handles (e.g. coming from Notes.jar objects)
	 * @param obj Notes object
	 */
	public static void __objectCreated(Class<?> clazz, IRecyclableNotesObject obj, boolean skipDuplicateHandleCheck) {
		DominoGCContext ctx = getThreadContext();
		
		if (obj.isRecycled())
			throw new NotesError(0, "Object is already recycled");
		
		if (PlatformUtils.is64Bit()) {
			HashKey64 key = new HashKey64(clazz, obj.getHandle64());
			
			LinkedHashMap<HashKey64, IRecyclableNotesObject> openHandles = ctx.getOpenHandlesDominoObjects64();
			IRecyclableNotesObject oldObj = openHandles.put(key, obj);
			if (!skipDuplicateHandleCheck && oldObj!=null && oldObj!=obj) {
				throw new IllegalStateException("Duplicate handle detected. Object to store: "+obj+", object found in open handle list: "+oldObj);
			}
		}
		else {
			HashKey32 key = new HashKey32(clazz, obj.getHandle32());
			
			LinkedHashMap<HashKey32, IRecyclableNotesObject> openHandles = ctx.getOpenHandlesDominoObjects32();
			IRecyclableNotesObject oldObj = openHandles.put(key, obj);
			if (!skipDuplicateHandleCheck && oldObj!=null && oldObj!=obj) {
				throw new IllegalStateException("Duplicate handle detected. Object to store: "+obj+", object found in open handle list: "+oldObj);
			}
		}
		
		if (ctx.isDebugLoggingEnabled()) {
			System.out.println("AutoGC - Added object: "+obj);
		}
	}

	/**
	 * Internal method to register a created Notes object that needs to be recycled
	 * 
	 * @param mem Notes object
	 */
	public static void __memoryAllocated(IAllocatedMemory mem) {
		DominoGCContext ctx = getThreadContext();
		
		if (mem.isFreed())
			throw new NotesError(0, "Memory is already freed");
		
		if (PlatformUtils.is64Bit()) {
			LinkedHashMap<Long, IAllocatedMemory> openHandles = ctx.getOpenHandlesMemory64();
			IAllocatedMemory oldObj = openHandles.put(mem.getHandle64(), mem);
			if (oldObj!=null && oldObj!=mem) {
				throw new IllegalStateException("Duplicate handle detected. Memory to store: "+mem+", object found in open handle list: "+oldObj);
			}
		}
		else {
			LinkedHashMap<Integer, IAllocatedMemory> openHandles = ctx.getOpenHandlesMemory32();
			IAllocatedMemory oldObj = openHandles.put(mem.getHandle32(), mem);
			if (oldObj!=null && oldObj!=mem) {
				throw new IllegalStateException("Duplicate handle detected. Memory to store: "+mem+", object found in open handle list: "+oldObj);
			}
		}
		
		if (ctx.isDebugLoggingEnabled()) {
			System.out.println("AutoGC - Added memory: "+mem);
		}
	}

	/**
	 * Internal method to check whether a 64 bit handle exists
	 * 
	 * @param objClazz class of Notes object
	 * @param handle handle
	 * @return Notes object
	 * @throws NotesError if handle does not exist
	 */
	public static IRecyclableNotesObject __b64_checkValidObjectHandle(Class<? extends IRecyclableNotesObject> objClazz, long handle) {
		DominoGCContext ctx = getThreadContext();
		
		HashKey64 key = new HashKey64(objClazz, handle);
		LinkedHashMap<HashKey64, IRecyclableNotesObject> openHandles = ctx.getOpenHandlesDominoObjects64();

		IRecyclableNotesObject obj = openHandles.get(key);
		if (obj==null) {
			throw new NotesError(0, "The provided C handle "+handle+" of object with class "+objClazz.getName()+" does not seem to exist (anymore).");
		}
		else {
			return obj;
		}
	}

	/**
	 * Internal method to check whether a 64 bit handle exists
	 * 
	 * @param memClazz class of Notes object
	 * @param handle handle
	 * @throws NotesError if handle does not exist
	 */
	public static void __checkValidMemHandle(Class<? extends IAllocatedMemory> memClazz, DHANDLE handle) {
		if (PlatformUtils.is64Bit()) {
			__b64_checkValidMemHandle(memClazz, ((DHANDLE64)handle).hdl);
		}
		else {
			__b32_checkValidMemHandle(memClazz, ((DHANDLE32)handle).hdl);
		}
	}
	
	/**
	 * Internal method to check whether a 64 bit handle exists
	 * 
	 * @param memClazz class of Notes object
	 * @param handle handle
	 * @throws NotesError if handle does not exist
	 */
	public static void __b64_checkValidMemHandle(Class<? extends IAllocatedMemory> memClazz, long handle) {
		DominoGCContext ctx = getThreadContext();
		
		LinkedHashMap<Long, IAllocatedMemory> openHandles = ctx.getOpenHandlesMemory64();

		IAllocatedMemory obj = openHandles.get(handle);
		if (obj==null) {
			throw new NotesError(0, "The provided C handle "+handle+" of memory with class "+memClazz.getName()+" does not seem to exist (anymore).");
		}
	}

	/**
	 * Internal method to check whether a 32 bit handle exists
	 * 
	 * @param objClazz class of Notes object
	 * @param handle handle
	 * @return Notes object
	 * @throws NotesError if handle does not exist
	 */
	public static IRecyclableNotesObject __b32_checkValidObjectHandle(Class<? extends IRecyclableNotesObject> objClazz, int handle) {
		DominoGCContext ctx = getThreadContext();
		
		LinkedHashMap<HashKey32,IRecyclableNotesObject> openHandles = ctx.getOpenHandlesDominoObjects32();
		
		HashKey32 key = new HashKey32(objClazz, handle);

		IRecyclableNotesObject obj = openHandles.get(key);
		if (obj==null) {
			throw new NotesError(0, "The provided C handle "+handle+" of object with class "+objClazz.getName()+" does not seem to exist (anymore).");
		}
		else
			return obj;
	}

	/**
	 * Internal method to check whether a 32 bit handle exists
	 * 
	 * @param objClazz class of Notes object
	 * @param handle handle
	 * @throws NotesError if handle does not exist
	 */
	public static void __b32_checkValidMemHandle(Class<? extends IAllocatedMemory> objClazz, int handle) {
		DominoGCContext ctx = getThreadContext();

		LinkedHashMap<Integer, IAllocatedMemory> openHandles = ctx.getOpenHandlesMemory32();
		
		IAllocatedMemory obj = openHandles.get(handle);
		if (obj==null) {
			throw new NotesError(0, "The provided C handle "+handle+" of memory with class "+objClazz.getName()+" does not seem to exist (anymore).");
		}
	}

	/**
	 * Internal method to unregister a created Notes object that was recycled
	 * 
	 * @param clazz class of hash pool
	 * @param obj Notes object
	 */
	public static void __objectBeeingBeRecycled(Class<? extends IRecyclableNotesObject> clazz, IRecyclableNotesObject obj) {
		DominoGCContext ctx = getThreadContext();
		
		if (obj.isRecycled())
			throw new NotesError(0, "Object is already recycled");

		if (ctx.isDebugLoggingEnabled()) {
			System.out.println("AutoGC - Removing object: "+obj.getClass()+" with handle="+(PlatformUtils.is64Bit() ? obj.getHandle64() : obj.getHandle32()));
		}
		
		if (PlatformUtils.is64Bit()) {
			HashKey64 key = new HashKey64(clazz, obj.getHandle64());
			LinkedHashMap<HashKey64, IRecyclableNotesObject> openHandles = ctx.getOpenHandlesDominoObjects64();
			openHandles.remove(key);
		}
		else {
			HashKey32 key = new HashKey32(clazz, obj.getHandle32());
			LinkedHashMap<HashKey32, IRecyclableNotesObject> openHandles = ctx.getOpenHandlesDominoObjects32();
			openHandles.remove(key);
		}
	}

	/**
	 * Internal method to unregister a created Notes object that was recycled
	 * 
	 * @param mem Notes object
	 */
	public static void __memoryBeeingFreed(IAllocatedMemory mem) {
		DominoGCContext ctx = getThreadContext();
		
		if (mem.isFreed())
			throw new NotesError(0, "Memory has already been freed");

		if (ctx.isDebugLoggingEnabled()) {
			System.out.println("AutoGC - Removing memory: "+mem.getClass()+" with handle="+(PlatformUtils.is64Bit() ? mem.getHandle64() : mem.getHandle32()));
		}
		
		if (PlatformUtils.is64Bit()) {
			LinkedHashMap<Long, IAllocatedMemory> openHandles = ctx.getOpenHandlesMemory64();
			openHandles.remove(mem.getHandle64());
		}
		else {
			LinkedHashMap<Integer, IAllocatedMemory> openHandles = ctx.getOpenHandlesMemory32();
			openHandles.remove(mem.getHandle32());
		}
	}

	/**
	 * Use this method to store your own custom values for the duration of the
	 * current {@link NotesGC#runWithAutoGC(Callable)} execution block.
	 * 
	 * @param key key
	 * @param value value, implement interface {@link IDisposableCustomValue} to get called for disposal
	 * @return previous value
	 */
	public static Object setCustomValue(String key, Object value) {
		DominoGCContext ctx = getThreadContext();

		Map<String,Object> map = ctx.getCustomValues();
		return map.put(key, value);
	}
	
	/**
	 * Reads a custom value stored via {@link #setCustomValue(String, Object)}
	 * for the duration of the current {@link #runWithAutoGC(Callable)}
	 * execution block.
	 * 
	 * @param key
	 * @return value or null if not set
	 */
	public static Object getCustomValue(String key) {
		DominoGCContext ctx = getThreadContext();
		
		Map<String,Object> map = ctx.getCustomValues();
		return map.get(key);
	}
	
	/**
	 * Tests if a custom value has been set via {@link #setCustomValue(String, Object)}.
	 * 
	 * @param key key
	 * @return true if value is set
	 */
	public boolean hasCustomValue(String key) {
		DominoGCContext ctx = getThreadContext();

		Map<String,Object> map = ctx.getCustomValues();
		return map.containsKey(key);
	}
	
	/**
	 * Throws an exception when the code is currently not running in a runWithAutoGC block
	 */
	public static void ensureRunningInAutoGC() {
		DominoGCContext ctx = threadContext.get();
		if (ctx==null) {
			throw new IllegalStateException("Thread is not enabled for auto GC. Either run your code via runWithAutoGC(Callable) or via try-with-resources on the object returned by initThread().");
		}
	}
	
	/**
	 * Method to check whether the current thread is already running in
	 * an auto GC block
	 * 
	 * @return true if in auto GC
	 */
	public static boolean isAutoGCActive() {
		return threadContext.get() != null;
	}

	/**
	 * When using {@link NotesGC#setCustomValue(String, Object)} to store your own
	 * values, use this
	 * interface for your value to get called for disposal when the {@link NotesGC#runWithAutoGC(Callable)}
	 * block is finished. Otherwise the value is just removed from the intermap map.
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IDisposableCustomValue {
		public void dispose();
	}

	/**
	 * Runs a piece of code and automatically disposes any allocated Notes objects at the end.
	 * The method supported nested calls.
	 * 
	 * @param callable code to execute
	 * @return computation result
	 * @throws Exception in case of errors
	 * 
	 * @param <T> return value type of code to be run
	 */
	public static <T> T runWithAutoGC(final Callable<T> callable) throws Exception {
		try (DominoGCContext ctx = initThread()) {
			return AccessController.doPrivileged(new PrivilegedAction<T>() {

				@Override
				public T run() {
					try {
						if ("true".equals(System.getProperty("dominojna.fixuplocalservername"))) {
							NotesGC.setFixupLocalServerNames(true);
						}
						if ("true".equals(System.getProperty("dominojna.prefernotestimedate"))) {
							NotesGC.setPreferNotesTimeDate(true);
						}
						
						return callable.call();
					} catch (Exception e) {
						if (e instanceof RuntimeException) {
							throw (RuntimeException) e;
						}
						else {
							throw new NotesError(0, "Error during code execution", e);
						}
					}
				}
			});
		}
	}
	
	/**
	 * Initializes the current thread for Domino JNA C and memory resource
	 * tracking.<br>
	 * <br>
	 * <b>The returned {@link DominoGCContext} must be closed to free up all allocated resources.
	 * Otherwise the client/server will run out of handles sooner or later!</b><br>
	 * <br>
	 * It is recommented to use a try-with-resources block to ensure calling the close() method
	 * even in case of execution errors, e.g.<br>
	 * <br>
	 * <code>
	 * try (DominoGCContext ctx = initThread()) {<br>
	 * &nbsp;&nbsp;&nbsp;// use Domino JNA classes, e.g.<br>
	 * &nbsp;&nbsp;&nbsp;NotesDatabase db = new NotesDatabase("", "names.nsf", "");<br>
	 * <br>
	 * } catch (Exception e) {<br>
	 * &nbsp;&nbsp;&nbsp;log(Level.SEVERE, "Error accessing Domino data", e);<br>
	 * }<br>
	 * </code>
	 * <br>
	 * Nested invocation of this method is supported. The current implementation only
	 * frees up resources when calling {@link DominoGCContext#close()} on the outer most
	 * context. Other calls are ignored.
	 * 
	 * @return garbage collection context
	 */
	public static DominoGCContext initThread() {
		DominoGCContext ctx = threadContext.get();
		if (ctx==null) {
			NotesNativeAPI.initialize();
			ctx = new DominoGCContext(null);
			threadContext.set(ctx);
			return ctx;
		}
		else {
			return new DominoGCContext(ctx);
		}
	}

	/**
	 * Domino handle collection context to collect all allocated C object
	 * and memory handles for the current thread.
	 * 
	 * @author Karsten Lehmann
	 */
	public static class DominoGCContext implements AutoCloseable {
		private DominoGCContext m_parentCtx;
		private Thread m_parentThread;
		private Map<String,Object> m_activeAutoGCCustomValues;
		
		//maps with open handles; using LinkedHashMap to keep insertion order for the keys and disposed in reverse order
		private LinkedHashMap<HashKey32,IRecyclableNotesObject> m_b32OpenHandlesDominoObjects;
		private LinkedHashMap<Integer, IAllocatedMemory> m_b32OpenHandlesMemory;
		private LinkedHashMap<HashKey64, IRecyclableNotesObject> m_b64OpenHandlesDominoObjects;
		private LinkedHashMap<Long, IAllocatedMemory> m_b64OpenHandlesMemory;
		private boolean m_debugLoggingEnabled;
		private boolean m_logCrashingThreadStackTrace;
		private boolean m_preferNotesTimeDate;
		private boolean m_fixupLocalServerNames;
		
		private DominoGCContext(DominoGCContext parentCtx) {
			m_parentCtx = parentCtx;
			m_parentThread = Thread.currentThread();
		}
		
		/**
		 * Returns the parent GC context if nested calls on {@link NotesGC#initThread()}
		 * are used.
		 * 
		 * @return parent context or null for top context
		 */
		public DominoGCContext getParentContext() {
			return m_parentCtx;
		}
		
		/**
		 * Returns true if this GC context is the first created for the current
		 * thread and false if it's a nested context.
		 * 
		 * @return true if top context
		 */
		public boolean isTopContext() {
			return m_parentCtx==null;
		}
		
		private void checkValidThread() {
			if (!m_parentThread.equals(Thread.currentThread())) {
				throw new IllegalStateException("This context cannot be used across threads");
			}
		}
		
		public boolean isDebugLoggingEnabled() {
			if (m_parentCtx!=null) {
				return m_parentCtx.isDebugLoggingEnabled();
			}
			return m_debugLoggingEnabled;
		}
		
		public void setDebugLoggingEnabled(boolean b) {
			if (m_parentCtx!=null) {
				m_parentCtx.setDebugLoggingEnabled(b);
				return;
			}
			m_debugLoggingEnabled = b;
		}
		
		public boolean isPreferNotesTimeDate() {
			return m_preferNotesTimeDate;
		}
		
		public void setPreferNotesTimeDate(boolean b) {
			if (m_parentCtx!=null) {
				m_parentCtx.setPreferNotesTimeDate(b);
				return;
			}
			m_preferNotesTimeDate = b;
		}
		
		public boolean isFixupLocalServerNames() {
			return m_fixupLocalServerNames;
		}
		
		public void setFixupLocalServerNames(boolean b) {
			if (m_parentCtx!=null) {
				m_parentCtx.setFixupLocalServerNames(b);
				return;
			}
			m_fixupLocalServerNames = b;
		}
		
		public boolean isLogCrashingThreadStackTrace() {
			if (m_parentCtx!=null) {
				return m_parentCtx.isLogCrashingThreadStackTrace();
			}
			return m_logCrashingThreadStackTrace;
		}
		
		public void setLogCrashingThreadStackTrace(boolean b) {
			if (m_parentCtx!=null) {
				m_parentCtx.setLogCrashingThreadStackTrace(b);
				return;
			}
			m_logCrashingThreadStackTrace = b;
		}
		
		private Map<String,Object> getCustomValues() {
			checkValidThread();
			if (m_parentCtx!=null) {
				return m_parentCtx.getCustomValues();
			}
			if (m_activeAutoGCCustomValues==null) {
				m_activeAutoGCCustomValues = new HashMap<>();
			}
			return m_activeAutoGCCustomValues;
		}
		
		private LinkedHashMap<HashKey32,IRecyclableNotesObject> getOpenHandlesDominoObjects32() {
			checkValidThread();
			if (m_parentCtx!=null) {
				return m_parentCtx.getOpenHandlesDominoObjects32();
			}
			if (m_b32OpenHandlesDominoObjects==null) {
				m_b32OpenHandlesDominoObjects = new LinkedHashMap<>();
			}
			return m_b32OpenHandlesDominoObjects;
		}
		
		private LinkedHashMap<Integer, IAllocatedMemory> getOpenHandlesMemory32() {
			checkValidThread();
			if (m_parentCtx!=null) {
				return m_parentCtx.getOpenHandlesMemory32();
			}
			if (m_b32OpenHandlesMemory==null) {
				m_b32OpenHandlesMemory = new LinkedHashMap<>();
			}
			return m_b32OpenHandlesMemory;
		}
		
		private LinkedHashMap<HashKey64, IRecyclableNotesObject> getOpenHandlesDominoObjects64() {
			checkValidThread();
			if (m_parentCtx!=null) {
				return m_parentCtx.getOpenHandlesDominoObjects64();
			}
			if (m_b64OpenHandlesDominoObjects==null) {
				m_b64OpenHandlesDominoObjects = new LinkedHashMap<>();
			}
			return m_b64OpenHandlesDominoObjects;
		}
		
		private LinkedHashMap<Long, IAllocatedMemory> getOpenHandlesMemory64() {
			checkValidThread();
			if (m_parentCtx!=null) {
				return m_parentCtx.getOpenHandlesMemory64();
			}
			if (m_b64OpenHandlesMemory==null) {
				m_b64OpenHandlesMemory = new LinkedHashMap<>();
			}
			return m_b64OpenHandlesMemory;
		}
		
		@Override
		public void close() throws Exception {
			checkValidThread();
			
			if (!isTopContext()) {
				//don't free up resources in nested calls on NotesGC.initThread()
				return;
			}

			if (PlatformUtils.is64Bit()) {
				{
					//recycle created Domino objects
					if (m_b64OpenHandlesDominoObjects!=null && !m_b64OpenHandlesDominoObjects.isEmpty()) {
						Entry[] mapEntries = m_b64OpenHandlesDominoObjects.entrySet().toArray(new Entry[m_b64OpenHandlesDominoObjects.size()]);
						if (mapEntries.length>0) {
							if (m_debugLoggingEnabled) {
								System.out.println("AutoGC - Auto-recycling "+mapEntries.length+" Domino objects:");
							}
							
							for (int i=mapEntries.length-1; i>=0; i--) {
								Entry<HashKey64,IRecyclableNotesObject> currEntry = mapEntries[i];
								IRecyclableNotesObject obj = currEntry.getValue();
								try {
									if (!obj.isRecycled()) {
										if (m_debugLoggingEnabled) {
											System.out.println("AutoGC - Auto-recycling "+obj);
										}
										obj.recycle();
									}
								}
								catch (Throwable e) {
									e.printStackTrace();
								}
								m_b64OpenHandlesDominoObjects.remove(currEntry.getKey());
							}
							
							if (m_debugLoggingEnabled) {
								System.out.println("AutoGC - Done auto-recycling "+mapEntries.length+" Domino objects");
							}
							
							m_b64OpenHandlesDominoObjects.clear();
							m_b64OpenHandlesDominoObjects = null;
						}
					}
				}
				{
					//dispose allocated memory
					if (m_b64OpenHandlesMemory!=null && !m_b64OpenHandlesMemory.isEmpty()) {
						Entry[] mapEntries = m_b64OpenHandlesMemory.entrySet().toArray(new Entry[m_b64OpenHandlesMemory.size()]);
						if (mapEntries.length>0) {
							if (m_debugLoggingEnabled) {
								System.out.println("AutoGC - Freeing "+mapEntries.length+" memory handles");
							}

							for (int i=mapEntries.length-1; i>=0; i--) {
								Entry<Long,IAllocatedMemory> currEntry = mapEntries[i];
								IAllocatedMemory obj = currEntry.getValue();
								try {
									if (!obj.isFreed()) {
										if (m_debugLoggingEnabled) {
											System.out.println("AutoGC - Freeing "+obj);
										}
										obj.free();
									}
								}
								catch (Throwable e) {
									e.printStackTrace();
								}
								m_b64OpenHandlesMemory.remove(currEntry.getKey());
							}

							if (m_debugLoggingEnabled) {
								System.out.println("AutoGC - Done freeing "+mapEntries.length+" memory handles");
							}

							m_b64OpenHandlesMemory.clear();
							m_b64OpenHandlesMemory = null;
						}
					}
				}
			}
			else {
				{
					if (m_b32OpenHandlesDominoObjects!=null && !m_b32OpenHandlesDominoObjects.isEmpty()) {
						//recycle created Domino objects
						Entry[] mapEntries = m_b32OpenHandlesDominoObjects.entrySet().toArray(new Entry[m_b32OpenHandlesDominoObjects.size()]);
						if (mapEntries.length>0) {
							if (m_debugLoggingEnabled) {
								System.out.println("AutoGC - Recycling "+mapEntries.length+" Domino objects:");
							}

							for (int i=mapEntries.length-1; i>=0; i--) {
								Entry<HashKey32,IRecyclableNotesObject> currEntry = mapEntries[i];
								IRecyclableNotesObject obj = currEntry.getValue();
								try {
									if (!obj.isRecycled()) {
										if (m_debugLoggingEnabled) {
											System.out.println("AutoGC - Recycling "+obj);
										}
										obj.recycle();
									}
								}
								catch (Throwable e) {
									e.printStackTrace();
								}
								m_b32OpenHandlesDominoObjects.remove(currEntry.getKey());
							}
							if (m_debugLoggingEnabled) {
								System.out.println("AutoGC - Done recycling "+mapEntries.length+" memory handles");
							}

							m_b32OpenHandlesDominoObjects.clear();
							m_b32OpenHandlesDominoObjects = null;
						}
					}
				}
				{
					if (m_b32OpenHandlesMemory!=null && !m_b32OpenHandlesMemory.isEmpty()) {
						//dispose allocated memory
						Entry[] mapEntries = m_b32OpenHandlesMemory.entrySet().toArray(new Entry[m_b32OpenHandlesMemory.size()]);
						if (mapEntries.length>0) {
							if (m_debugLoggingEnabled) {
								System.out.println("AutoGC - Freeing "+mapEntries.length+" memory handles");
							}

							for (int i=mapEntries.length-1; i>=0; i--) {
								Entry<Integer,IAllocatedMemory> currEntry = mapEntries[i];
								IAllocatedMemory obj = currEntry.getValue();
								try {
									if (!obj.isFreed()) {
										if (m_debugLoggingEnabled) {
											System.out.println("AutoGC - Freeing "+obj);
										}
										obj.free();
									}
								}
								catch (Throwable e) {
									e.printStackTrace();
								}
								m_b32OpenHandlesMemory.remove(currEntry.getKey());
							}
							if (m_debugLoggingEnabled) {
								System.out.println("AutoGC - Done freeing "+mapEntries.length+" memory handles");
							}

							m_b32OpenHandlesMemory.clear();
							m_b32OpenHandlesMemory = null;
						}
					}
				}
			}
			
			if (m_activeAutoGCCustomValues!=null) {
				cleanupCustomValues(m_activeAutoGCCustomValues);
				m_activeAutoGCCustomValues.clear();
				m_activeAutoGCCustomValues = null;
			}
			
			threadContext.set(null);
		}
	}
	
	private static void cleanupCustomValues(Map<String, Object> customValues) {
		for (Entry<String,Object> currEntry : customValues.entrySet()) {
			Object currVal = currEntry.getValue();
			if (currVal instanceof IDisposableCustomValue) {
				try {
					((IDisposableCustomValue)currVal).dispose();
				}
				catch (Exception e) {
					//give access to this exception via special (optional) PrintWriter,
					//but continue with the loop
					Object out = customValues.get("NotesGC.CustomValueDisposeOut");
					if (out instanceof PrintWriter) {
						e.printStackTrace((PrintWriter) out);
					}
				}
			}
		}
	}
}