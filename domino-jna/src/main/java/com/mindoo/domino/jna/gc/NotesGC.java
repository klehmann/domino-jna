package com.mindoo.domino.jna.gc;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.internal.NotesJNAContext;

/**
 * Utility class to simplify memory management with Notes handles. The class tracks
 * handle creation and disposal. By using {@link #runWithAutoGC(Callable)}, the
 * collected handles are automatically disposed when code execution is done.
 * 
 * @author Karsten Lehmann
 */
public class NotesGC {
	private static ThreadLocal<Boolean> m_activeAutoGC = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return Boolean.FALSE;
		}
	};
	//maps with open handles; using LinkedHashMap to keep insertion order for the keys
	private static ThreadLocal<LinkedHashMap<Integer,IRecyclableNotesObject>> m_b32OpenHandles = new ThreadLocal<LinkedHashMap<Integer,IRecyclableNotesObject>>();
	private static ThreadLocal<LinkedHashMap<Long, IRecyclableNotesObject>> m_b64OpenHandles = new ThreadLocal<LinkedHashMap<Long,IRecyclableNotesObject>>();
	
	/**
	 * Internal method to register a created Notes object that needs to be recycled
	 * 
	 * @param obj Notes object
	 */
	public static void __objectCreated(IRecyclableNotesObject obj) {
		if (!m_activeAutoGC.get().booleanValue())
			throw new IllegalStateException("Auto GC is not active");
		
		if (obj.isRecycled())
			throw new NotesError(0, "Object is already recycled");
		
		if (NotesJNAContext.is64Bit()) {
			m_b64OpenHandles.get().put(obj.getHandle64(), obj);
			
		}
		else {
			m_b32OpenHandles.get().put(obj.getHandle32(), obj);
		}
	}
	
	/**
	 * Internal method to check whether a 64 bit handle exists
	 * 
	 * @param handle handle
	 * @throws NotesError if handle does not exist
	 */
	public static void __b64_checkValidHandle(long handle) {
		IRecyclableNotesObject obj = m_b64OpenHandles.get().get(handle);
		if (obj==null) {
			throw new NotesError(0, "The provided C handle does not seem to exist (anymore).");
		}
	}

	/**
	 * Internal method to check whether a 32 bit handle exists
	 * 
	 * @param handle handle
	 * @throws NotesError if handle does not exist
	 */
	public static void __b32_checkValidHandle(int handle) {
		IRecyclableNotesObject obj = m_b32OpenHandles.get().get(handle);
		if (obj==null) {
			throw new NotesError(0, "The provided C handle does not seem to exist (anymore).");
		}
	}

	/**
	 * Internal method to unregister a created Notes object that was recycled
	 * 
	 * @param obj Notes object
	 */
	public static void __objectRecycled(IRecyclableNotesObject obj) {
		if (!m_activeAutoGC.get().booleanValue())
			throw new IllegalStateException("Auto GC is not active");
		
		if (obj.isRecycled())
			throw new NotesError(0, "Object is already recycled");

		if (NotesJNAContext.is64Bit()) {
			m_b64OpenHandles.get().remove(obj.getHandle64());
		}
		else {
			m_b32OpenHandles.get().remove(obj.getHandle32());
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
		if (m_activeAutoGC.get().booleanValue()) {
			//nested call
			return callable.call();
		}
		else {
			m_activeAutoGC.set(Boolean.TRUE);
			LinkedHashMap<Integer,IRecyclableNotesObject> b32Handles = null;
			LinkedHashMap<Long,IRecyclableNotesObject> b64Handles = null;
			
			try {
				if (NotesJNAContext.is64Bit()) {
					b64Handles = new LinkedHashMap<Long,IRecyclableNotesObject>();
					m_b64OpenHandles.set(b64Handles);
				}
				else {
					b32Handles = new LinkedHashMap<Integer,IRecyclableNotesObject>();
					m_b32OpenHandles.set(b32Handles);
				}
				
				return callable.call();
			}
			finally {
				if (NotesJNAContext.is64Bit()) {
					Entry[] mapEntries = b64Handles.entrySet().toArray(new Entry[b64Handles.size()]);
					
					for (int i=mapEntries.length-1; i>=0; i--) {
						Entry<Long,IRecyclableNotesObject> currEntry = mapEntries[i];
						IRecyclableNotesObject obj = currEntry.getValue();
						obj.recycle();
					}
				}
				else {
					Entry[] mapEntries = b32Handles.entrySet().toArray(new Entry[b32Handles.size()]);
					
					for (int i=mapEntries.length-1; i>=0; i--) {
						Entry<Integer,IRecyclableNotesObject> currEntry = mapEntries[i];
						IRecyclableNotesObject obj = currEntry.getValue();
						obj.recycle();
					}
					
				}
				m_activeAutoGC.set(Boolean.FALSE);
			}
		}
	}
}
