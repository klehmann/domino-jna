package com.mindoo.domino.jna.gc;

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.utils.Pair;
import com.mindoo.domino.jna.utils.PlatformUtils;

/**
 * Utility class to simplify memory management with Notes handles. The class tracks
 * handle creation and disposal. By using {@link #runWithAutoGC(Callable)}, the
 * collected handles are automatically disposed when code execution is done.
 * 
 * @author Karsten Lehmann
 */
public class NotesGC {
	private static ThreadLocal<Boolean> m_activeAutoGC = new ThreadLocal<Boolean>();
	private static ThreadLocal<Map<String,Object>> m_activeAutoGCCustomValues = new ThreadLocal<Map<String,Object>>();
	
	//maps with open handles; using LinkedHashMap to keep insertion order for the keys
	private static ThreadLocal<LinkedHashMap<HashKey32,Pair<IRecyclableNotesObject, Long>>> m_b32OpenHandlesDominoObjects = new ThreadLocal<LinkedHashMap<HashKey32,Pair<IRecyclableNotesObject, Long>>>();
	private static ThreadLocal<LinkedHashMap<Integer, Pair<IAllocatedMemory, Long>>> m_b32OpenHandlesMemory = new ThreadLocal<LinkedHashMap<Integer,Pair<IAllocatedMemory, Long>>>();
	private static ThreadLocal<LinkedHashMap<HashKey64, Pair<IRecyclableNotesObject, Long>>> m_b64OpenHandlesDominoObjects = new ThreadLocal<LinkedHashMap<HashKey64,Pair<IRecyclableNotesObject, Long>>>();
	private static ThreadLocal<LinkedHashMap<Long, Pair<IAllocatedMemory, Long>>> m_b64OpenHandlesMemory = new ThreadLocal<LinkedHashMap<Long,Pair<IAllocatedMemory, Long>>>();
	private static ThreadLocal<Long> m_threadInvocationCount = new ThreadLocal<>();
	
	private static final AtomicLong m_globalInvocationCounter = new AtomicLong();
	
	private static ThreadLocal<Boolean> m_writeDebugMessages = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {
			return Boolean.FALSE;
		};
	};
	private static ThreadLocal<Boolean> m_logCrashingThreadStackTrace = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {
			return Boolean.FALSE;
		};
	};
	
	/**
	 * Method to enable GC debug logging for the current {@link #runWithAutoGC(Callable)} call
	 * 
	 * @param enabled true if enabled
	 */
	public static void setDebugLoggingEnabled(boolean enabled) {
		m_writeDebugMessages.set(Boolean.valueOf(enabled));
	}
	
	/**
	 * Method to write a stacktrace to disk right before each native method invocation. Consumes
	 * much performance and is therefore disabled by default and just here to track down
	 * handle panics.<br>
	 * Stacktraces are written as files domino-jna-stack-&lt;threadid&gt;.txt in the temp directory.
	 * 
	 * @param log true to log
	 */
	public static void setLogCrashingThreadStacktrace(boolean log) {
		m_logCrashingThreadStackTrace.set(Boolean.valueOf(log));
	}
	
	/**
	 * Checks whether stacktraces for each native method invocation should be written to disk. Consumes
	 * much performance and is therefore disabled by default and just here to track down
	 * handle panics.<br>
	 * Stacktraces are written as files domino-jna-stack-&lt;threadid&gt;.txt in the temp directory.
	 * 
	 * @return true to log
	 */
	public static boolean isLogCrashingThreadStacktrace() {
		return Boolean.TRUE.equals(m_logCrashingThreadStackTrace.get());
	}
	
	/**
	 * Method to get the current count of open Domino object handles
	 * 
	 * @return handle count
	 */
	public static int getNumberOfOpenObjectHandles() {
		if (!Boolean.TRUE.equals(m_activeAutoGC.get()))
			throw new IllegalStateException("Auto GC is not active");
		
		if (PlatformUtils.is64Bit()) {
			return m_b64OpenHandlesDominoObjects.get().size();
		}
		else {
			return m_b32OpenHandlesDominoObjects.get().size();
		}
	}

	/**
	 * Method to get the current count of open Domino memory handles
	 * 
	 * @return handle count
	 */
	public static int getNumberOfOpenMemoryHandles() {
		if (!Boolean.TRUE.equals(m_activeAutoGC.get()))
			throw new IllegalStateException("Auto GC is not active");
		
		if (PlatformUtils.is64Bit()) {
			return m_b64OpenHandlesMemory.get().size();
		}
		else {
			return m_b32OpenHandlesMemory.get().size();
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
		if (!Boolean.TRUE.equals(m_activeAutoGC.get()))
			throw new IllegalStateException("Auto GC is not active");
		
		if (obj.isRecycled())
			throw new NotesError(0, "Object is already recycled");
		
		long currInvCnt = m_threadInvocationCount.get();
		
		if (PlatformUtils.is64Bit()) {
			HashKey64 key = new HashKey64(clazz, obj.getHandle64());
			
			Pair<IRecyclableNotesObject,Long> oldObjWithInvCnt = m_b64OpenHandlesDominoObjects.get().put(key, new Pair(obj, currInvCnt));
			IRecyclableNotesObject oldObj = oldObjWithInvCnt==null ? null : oldObjWithInvCnt.getValue1();
			if (oldObj!=null && oldObj!=obj) {
				throw new IllegalStateException("Duplicate handle detected. Object to store: "+obj+", object found in open handle list: "+oldObj);
			}
		}
		else {
			HashKey32 key = new HashKey32(clazz, obj.getHandle32());
			
			Pair<IRecyclableNotesObject,Long> oldObjWithInvCnt = m_b32OpenHandlesDominoObjects.get().put(key, new Pair(obj, currInvCnt));
			IRecyclableNotesObject oldObj = oldObjWithInvCnt==null ? null : oldObjWithInvCnt.getValue1();
			if (oldObj!=null && oldObj!=obj) {
				throw new IllegalStateException("Duplicate handle detected. Object to store: "+obj+", object found in open handle list: "+oldObj);
			}
		}
		
		if (Boolean.TRUE.equals(m_writeDebugMessages.get())) {
			System.out.println("AutoGC - Added object: "+obj);
		}
	}

	/**
	 * Internal method to register a created Notes object that needs to be recycled
	 * 
	 * @param mem Notes object
	 */
	public static void __memoryAllocated(IAllocatedMemory mem) {
		if (!Boolean.TRUE.equals(m_activeAutoGC.get()))
			throw new IllegalStateException("Auto GC is not active");
		
		if (mem.isFreed())
			throw new NotesError(0, "Memory is already freed");
		
		long currInvCnt = m_threadInvocationCount.get();

		if (PlatformUtils.is64Bit()) {
			Pair<IAllocatedMemory,Long> oldObjWithInvCnt = m_b64OpenHandlesMemory.get().put(mem.getHandle64(), new Pair(mem, currInvCnt));
			IAllocatedMemory oldObj = oldObjWithInvCnt==null ? null : oldObjWithInvCnt.getValue1();
			if (oldObj!=null && oldObj!=mem) {
				throw new IllegalStateException("Duplicate handle detected. Memory to store: "+mem+", object found in open handle list: "+oldObj);
			}
		}
		else {
			Pair<IAllocatedMemory,Long> oldObjWithInvCnt = m_b32OpenHandlesMemory.get().put(mem.getHandle32(), new Pair(mem, currInvCnt));
			IAllocatedMemory oldObj = oldObjWithInvCnt==null ? null : oldObjWithInvCnt.getValue1();
			if (oldObj!=null && oldObj!=mem) {
				throw new IllegalStateException("Duplicate handle detected. Memory to store: "+mem+", object found in open handle list: "+oldObj);
			}
		}
		
		if (Boolean.TRUE.equals(m_writeDebugMessages.get())) {
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
		if (!Boolean.TRUE.equals(m_activeAutoGC.get()))
			throw new IllegalStateException("Auto GC is not active");
		
		HashKey64 key = new HashKey64(objClazz, handle);
		Pair<IRecyclableNotesObject,Long> objWithInvCnt = m_b64OpenHandlesDominoObjects.get().get(key);
		if (objWithInvCnt!=null) {
			long currInvCnt = m_threadInvocationCount.get();
			if (currInvCnt != objWithInvCnt.getValue2()) {
				throw new NotesError(0, "Object was created in a different thread or auto-gc block!");
			}
		}

		IRecyclableNotesObject obj = objWithInvCnt==null ? null : objWithInvCnt.getValue1();
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
	public static void __b64_checkValidMemHandle(Class<? extends IAllocatedMemory> memClazz, long handle) {
		if (!Boolean.TRUE.equals(m_activeAutoGC.get()))
			throw new IllegalStateException("Auto GC is not active");
		
		Pair<IAllocatedMemory,Long> objWithInvCnt = m_b64OpenHandlesMemory.get().get(handle);
		if (objWithInvCnt!=null) {
			long currInvCnt = m_threadInvocationCount.get();
			if (currInvCnt != objWithInvCnt.getValue2()) {
				throw new NotesError(0, "Memory was allocated in a different thread or auto-gc block!");
			}
		}

		IAllocatedMemory obj = objWithInvCnt==null ? null : objWithInvCnt.getValue1();
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
		if (!Boolean.TRUE.equals(m_activeAutoGC.get()))
			throw new IllegalStateException("Auto GC is not active");
		
		HashKey32 key = new HashKey32(objClazz, handle);
		Pair<IRecyclableNotesObject,Long> objWithInvCnt = m_b32OpenHandlesDominoObjects.get().get(key);
		if (objWithInvCnt!=null) {
			long currInvCnt = m_threadInvocationCount.get();
			if (currInvCnt != objWithInvCnt.getValue2()) {
				throw new NotesError(0, "Object was created in a different thread or auto-gc block!");
			}
		}

		IRecyclableNotesObject obj = objWithInvCnt==null ? null : objWithInvCnt.getValue1();
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
		if (!Boolean.TRUE.equals(m_activeAutoGC.get()))
			throw new IllegalStateException("Auto GC is not active");
		
		Pair<IAllocatedMemory,Long> objWithInvCnt = m_b32OpenHandlesMemory.get().get(handle);
		if (objWithInvCnt!=null) {
			long currInvCnt = m_threadInvocationCount.get();
			if (currInvCnt != objWithInvCnt.getValue2()) {
				throw new NotesError(0, "Memory was allocated in a different thread or auto-gc block!");
			}
		}
		IAllocatedMemory obj = objWithInvCnt==null ? null : objWithInvCnt.getValue1();
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
		if (!Boolean.TRUE.equals(m_activeAutoGC.get()))
			throw new IllegalStateException("Auto GC is not active");
		
		if (obj.isRecycled())
			throw new NotesError(0, "Object is already recycled");

		if (Boolean.TRUE.equals(m_writeDebugMessages.get())) {
			System.out.println("AutoGC - Removing object: "+obj.getClass()+" with handle="+(PlatformUtils.is64Bit() ? obj.getHandle64() : obj.getHandle32()));
		}
		
		if (PlatformUtils.is64Bit()) {
			HashKey64 key = new HashKey64(clazz, obj.getHandle64());
			m_b64OpenHandlesDominoObjects.get().remove(key);
		}
		else {
			HashKey32 key = new HashKey32(clazz, obj.getHandle32());
			m_b32OpenHandlesDominoObjects.get().remove(key);
		}
	}

	/**
	 * Internal method to unregister a created Notes object that was recycled
	 * 
	 * @param mem Notes object
	 */
	public static void __memoryBeeingFreed(IAllocatedMemory mem) {
		if (!Boolean.TRUE.equals(m_activeAutoGC.get()))
			throw new IllegalStateException("Auto GC is not active");
		
		if (mem.isFreed())
			throw new NotesError(0, "Memory has already been freed");

		if (Boolean.TRUE.equals(m_writeDebugMessages.get())) {
			System.out.println("AutoGC - Removing memory: "+mem.getClass()+" with handle="+(PlatformUtils.is64Bit() ? mem.getHandle64() : mem.getHandle32()));
		}
		
		if (PlatformUtils.is64Bit()) {
			m_b64OpenHandlesMemory.get().remove(mem.getHandle64());
		}
		else {
			m_b32OpenHandlesMemory.get().remove(mem.getHandle32());
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
		Map<String,Object> map = m_activeAutoGCCustomValues.get();
		if (map==null) {
			throw new IllegalStateException("No auto gc block is active");
		}
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
		Map<String,Object> map = m_activeAutoGCCustomValues.get();
		if (map==null) {
			throw new IllegalStateException("No auto gc block is active");
		}
		return map.get(key);
	}
	
	/**
	 * Tests if a custom value has been set via {@link #setCustomValue(String, Object)}.
	 * 
	 * @param key key
	 * @return true if value is set
	 */
	public boolean hasCustomValue(String key) {
		Map<String,Object> map = m_activeAutoGCCustomValues.get();
		if (map==null) {
			throw new IllegalStateException("No auto gc block is active");
		}
		return map.containsKey(key);
	}
	
	/**
	 * Throws an exception when the code is currently not running in a runWithAutoGC block
	 */
	public static void ensureRunningInAutoGC() {
		if (!Boolean.TRUE.equals(m_activeAutoGC.get())) {
			throw new NotesError(0, "Please wrap code accessing the JNA API in a NotesGC.runWithAutoGC(Callable) block");
		}
	}
	
	/**
	 * Method to check whether the current thread is already running in
	 * an auto GC block
	 * 
	 * @return true if in auto GC
	 */
	public static boolean isAutoGCActive() {
		return Boolean.TRUE.equals(m_activeAutoGC.get());
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
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> T runWithAutoGC(final Callable<T> callable) throws Exception {
		if (Boolean.TRUE.equals(m_activeAutoGC.get())) {
			//nested call
			return callable.call();
		}
		else {
			long incCnt = m_globalInvocationCounter.getAndIncrement();
			m_threadInvocationCount.set(incCnt);
			
			NotesNativeAPI.initialize();

			m_activeAutoGC.set(Boolean.TRUE);
			m_activeAutoGCCustomValues.set(new HashMap<String, Object>());
			
			LinkedHashMap<HashKey32,Pair<IRecyclableNotesObject,Long>> b32HandlesDominoObjects = null;
			LinkedHashMap<HashKey64,Pair<IRecyclableNotesObject,Long>> b64HandlesDominoObjects = null;
			
			LinkedHashMap<Integer,Pair<IAllocatedMemory,Long>> b32HandlesMemory = null;
			LinkedHashMap<Long,Pair<IAllocatedMemory,Long>> b64HandlesMemory = null;
			
			try {
				if (PlatformUtils.is64Bit()) {
					b64HandlesDominoObjects = new LinkedHashMap<HashKey64,Pair<IRecyclableNotesObject,Long>>();
					m_b64OpenHandlesDominoObjects.set(b64HandlesDominoObjects);
					
					b64HandlesMemory = new LinkedHashMap<Long,Pair<IAllocatedMemory,Long>>();
					m_b64OpenHandlesMemory.set(b64HandlesMemory);
				}
				else {
					b32HandlesDominoObjects = new LinkedHashMap<HashKey32,Pair<IRecyclableNotesObject,Long>>();
					m_b32OpenHandlesDominoObjects.set(b32HandlesDominoObjects);
					
					b32HandlesMemory = new LinkedHashMap<Integer,Pair<IAllocatedMemory,Long>>();
					m_b32OpenHandlesMemory.set(b32HandlesMemory);
				}
				
				return AccessController.doPrivileged(new PrivilegedAction<T>() {

					@Override
					public T run() {
						try {
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
			finally {
				boolean writeDebugMsg = Boolean.TRUE.equals(m_writeDebugMessages.get());
				
				if (PlatformUtils.is64Bit()) {
					{
						//recycle created Domino objects
						if (!b64HandlesDominoObjects.isEmpty()) {
							Entry[] mapEntries = b64HandlesDominoObjects.entrySet().toArray(new Entry[b64HandlesDominoObjects.size()]);
							if (mapEntries.length>0) {
								if (writeDebugMsg) {
									System.out.println("AutoGC - Auto-recycling "+mapEntries.length+" Domino objects:");
								}
								
								for (int i=mapEntries.length-1; i>=0; i--) {
									Entry<HashKey64,Pair<IRecyclableNotesObject,Long>> currEntry = mapEntries[i];
									IRecyclableNotesObject obj = currEntry.getValue().getValue1();
									try {
										if (!obj.isRecycled()) {
											if (writeDebugMsg) {
												System.out.println("AutoGC - Auto-recycling "+obj);
											}
											obj.recycle();
										}
									}
									catch (Throwable e) {
										e.printStackTrace();
									}
									b64HandlesDominoObjects.remove(currEntry.getKey());
								}
								
								if (writeDebugMsg) {
									System.out.println("AutoGC - Done auto-recycling "+mapEntries.length+" Domino objects");
								}
								
								b64HandlesDominoObjects.clear();
								m_b64OpenHandlesDominoObjects.set(null);
							}
						}
					}
					{
						//dispose allocated memory
						if (!b64HandlesMemory.isEmpty()) {
							Entry[] mapEntries = b64HandlesMemory.entrySet().toArray(new Entry[b64HandlesMemory.size()]);
							if (mapEntries.length>0) {
								if (writeDebugMsg) {
									System.out.println("AutoGC - Freeing "+mapEntries.length+" memory handles");
								}

								for (int i=mapEntries.length-1; i>=0; i--) {
									Entry<Long,Pair<IAllocatedMemory,Long>> currEntry = mapEntries[i];
									IAllocatedMemory obj = currEntry.getValue().getValue1();
									try {
										if (!obj.isFreed()) {
											if (writeDebugMsg) {
												System.out.println("AutoGC - Freeing "+obj);
											}
											obj.free();
										}
									}
									catch (Throwable e) {
										e.printStackTrace();
									}
									b64HandlesMemory.remove(currEntry.getKey());
								}

								if (writeDebugMsg) {
									System.out.println("AutoGC - Done freeing "+mapEntries.length+" memory handles");
								}

								b64HandlesMemory.clear();
								m_b64OpenHandlesMemory.set(null);
							}
						}
					}
				}
				else {
					{
						if (!b32HandlesDominoObjects.isEmpty()) {
							//recycle created Domino objects
							Entry[] mapEntries = b32HandlesDominoObjects.entrySet().toArray(new Entry[b32HandlesDominoObjects.size()]);
							if (mapEntries.length>0) {
								if (writeDebugMsg) {
									System.out.println("AutoGC - Recycling "+mapEntries.length+" Domino objects:");
								}

								for (int i=mapEntries.length-1; i>=0; i--) {
									Entry<HashKey32,Pair<IRecyclableNotesObject,Long>> currEntry = mapEntries[i];
									IRecyclableNotesObject obj = currEntry.getValue().getValue1();
									try {
										if (!obj.isRecycled()) {
											if (writeDebugMsg) {
												System.out.println("AutoGC - Recycling "+obj);
											}
											obj.recycle();
										}
									}
									catch (Throwable e) {
										e.printStackTrace();
									}
									b32HandlesDominoObjects.remove(currEntry.getKey());
								}
								if (writeDebugMsg) {
									System.out.println("AutoGC - Done recycling "+mapEntries.length+" memory handles");
								}

								b32HandlesDominoObjects.clear();
								m_b32OpenHandlesDominoObjects.set(null);
							}
						}
					}
					{
						if (!b32HandlesMemory.isEmpty()) {
							//dispose allocated memory
							Entry[] mapEntries = b32HandlesMemory.entrySet().toArray(new Entry[b32HandlesMemory.size()]);
							if (mapEntries.length>0) {
								if (writeDebugMsg) {
									System.out.println("AutoGC - Freeing "+mapEntries.length+" memory handles");
								}

								for (int i=mapEntries.length-1; i>=0; i--) {
									Entry<Integer,Pair<IAllocatedMemory,Long>> currEntry = mapEntries[i];
									IAllocatedMemory obj = currEntry.getValue().getValue1();
									try {
										if (!obj.isFreed()) {
											if (writeDebugMsg) {
												System.out.println("AutoGC - Freeing "+obj);
											}
											obj.free();
										}
									}
									catch (Throwable e) {
										e.printStackTrace();
									}
									b32HandlesMemory.remove(currEntry.getKey());
								}
								if (writeDebugMsg) {
									System.out.println("AutoGC - Done freeing "+mapEntries.length+" memory handles");
								}

								b32HandlesMemory.clear();
								m_b32OpenHandlesMemory.set(null);
							}
						}
					}
				}
				
				Map<String,Object> customValues = m_activeAutoGCCustomValues.get();
				if (customValues!=null) {
					cleanupCustomValues(customValues);
					customValues.clear();
				}
				m_activeAutoGCCustomValues.set(null);
				m_activeAutoGC.set(null);
				m_writeDebugMessages.set(Boolean.FALSE);
				m_threadInvocationCount.set(null);
			}
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

}