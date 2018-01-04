package com.mindoo.domino.jna.gc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
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
	private static ThreadLocal<LinkedHashMap<HashKey32,IRecyclableNotesObject>> m_b32OpenHandlesDominoObjects = new ThreadLocal<LinkedHashMap<HashKey32,IRecyclableNotesObject>>();
	private static ThreadLocal<LinkedHashMap<Integer,IAllocatedMemory>> m_b32OpenHandlesMemory = new ThreadLocal<LinkedHashMap<Integer,IAllocatedMemory>>();
	private static ThreadLocal<LinkedHashMap<HashKey64, IRecyclableNotesObject>> m_b64OpenHandlesDominoObjects = new ThreadLocal<LinkedHashMap<HashKey64,IRecyclableNotesObject>>();
	private static ThreadLocal<LinkedHashMap<Long, IAllocatedMemory>> m_b64OpenHandlesMemory = new ThreadLocal<LinkedHashMap<Long,IAllocatedMemory>>();
	
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
		
		if (PlatformUtils.is64Bit()) {
			HashKey64 key = new HashKey64(clazz, obj.getHandle64());
			
			IRecyclableNotesObject oldObj = m_b64OpenHandlesDominoObjects.get().put(key, obj);
			if (oldObj!=null && oldObj!=obj) {
				throw new IllegalStateException("Duplicate handle detected. Object to store: "+obj+", object found in open handle list: "+oldObj);
			}
		}
		else {
			HashKey32 key = new HashKey32(clazz, obj.getHandle32());
			
			IRecyclableNotesObject oldObj = m_b32OpenHandlesDominoObjects.get().put(key, obj);
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
		
		
		if (PlatformUtils.is64Bit()) {
			IAllocatedMemory oldObj = m_b64OpenHandlesMemory.get().put(mem.getHandle64(), mem);
			if (oldObj!=null && oldObj!=mem) {
				throw new IllegalStateException("Duplicate handle detected. Memory to store: "+mem+", object found in open handle list: "+oldObj);
			}
		}
		else {
			IAllocatedMemory oldObj = m_b32OpenHandlesMemory.get().put(mem.getHandle32(), mem);
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
		IRecyclableNotesObject obj = m_b64OpenHandlesDominoObjects.get().get(key);
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
		
		IAllocatedMemory obj = m_b64OpenHandlesMemory.get().get(handle);
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
		IRecyclableNotesObject obj = m_b32OpenHandlesDominoObjects.get().get(key);
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
		
		IAllocatedMemory obj = m_b32OpenHandlesMemory.get().get(handle);
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

	public static Object setCustomValue(String key, Object value) {
		Map<String,Object> map = m_activeAutoGCCustomValues.get();
		if (map==null) {
			throw new IllegalStateException("No auto gc block is active");
		}
		return map.put(key, value);
	}
	
	public static Object getCustomValue(String key) {
		Map<String,Object> map = m_activeAutoGCCustomValues.get();
		if (map==null) {
			throw new IllegalStateException("No auto gc block is active");
		}
		return map.get(key);
	}
	
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
	public static <T> T runWithAutoGC(Callable<T> callable) throws Exception {
		if (Boolean.TRUE.equals(m_activeAutoGC.get())) {
			//nested call
			return callable.call();
		}
		else {
			NotesNativeAPI.initialize();

			m_activeAutoGC.set(Boolean.TRUE);
			m_activeAutoGCCustomValues.set(new HashMap<String, Object>());
			
			LinkedHashMap<HashKey32,IRecyclableNotesObject> b32HandlesDominoObjects = null;
			LinkedHashMap<HashKey64,IRecyclableNotesObject> b64HandlesDominoObjects = null;
			
			LinkedHashMap<Integer,IAllocatedMemory> b32HandlesMemory = null;
			LinkedHashMap<Long,IAllocatedMemory> b64HandlesMemory = null;
			
			try {
				if (PlatformUtils.is64Bit()) {
					b64HandlesDominoObjects = new LinkedHashMap<HashKey64,IRecyclableNotesObject>();
					m_b64OpenHandlesDominoObjects.set(b64HandlesDominoObjects);
					
					b64HandlesMemory = new LinkedHashMap<Long,IAllocatedMemory>();
					m_b64OpenHandlesMemory.set(b64HandlesMemory);
				}
				else {
					b32HandlesDominoObjects = new LinkedHashMap<HashKey32,IRecyclableNotesObject>();
					m_b32OpenHandlesDominoObjects.set(b32HandlesDominoObjects);
					
					b32HandlesMemory = new LinkedHashMap<Integer,IAllocatedMemory>();
					m_b32OpenHandlesMemory.set(b32HandlesMemory);
				}
				
				return callable.call();
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
									Entry<HashKey64,IRecyclableNotesObject> currEntry = mapEntries[i];
									IRecyclableNotesObject obj = currEntry.getValue();
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
									Entry<Long,IAllocatedMemory> currEntry = mapEntries[i];
									IAllocatedMemory obj = currEntry.getValue();
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
									Entry<HashKey32,IRecyclableNotesObject> currEntry = mapEntries[i];
									IRecyclableNotesObject obj = currEntry.getValue();
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
									Entry<Integer,IAllocatedMemory> currEntry = mapEntries[i];
									IAllocatedMemory obj = currEntry.getValue();
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
				m_activeAutoGCCustomValues.set(null);
				m_activeAutoGC.set(null);
				m_writeDebugMessages.set(Boolean.FALSE);
			}
		}
	}
}