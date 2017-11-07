package com.mindoo.domino.jna.utils;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNamesList;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.utils.NotesNamingUtils.Privileges;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;

/**
 * Utility class that bridges between the C API calls provided in this project and
 * the legacy lotus.domino API.
 * 
 * @author Karsten Lehmann
 */
public class LegacyAPIUtils {
	private static boolean m_initialized;

	private static volatile Method getDocumentHandleRW;
	private static volatile Method createDocument;
	private static volatile Method createDatabase;
	private static volatile Method createXPageSession;
	private static volatile Method getDBHandle;

	private static synchronized void initialize() {
		if (m_initialized)
			return;

		m_initialized = true;

		AccessController.doPrivileged(new PrivilegedAction<Object>() {

			@Override
			public Object run() {
				try {
					//This class only works when lwpd.domino.napi.jar and lwpd.commons.jar are in the classpath
					Class<?> cClass = Class.forName("com.ibm.domino.napi.c.C");
					Method initLibrary = cClass.getMethod("initLibrary", String.class);
					initLibrary.invoke(null, "");

					try {
						Class<?> backendBridgeClass = Class.forName("com.ibm.domino.napi.c.BackendBridge");
						try {
							getDocumentHandleRW = backendBridgeClass.getMethod("getDocumentHandleRW", lotus.domino.Document.class);
						}
						catch (Exception e) {
							//
						}
						try {
							createDatabase = backendBridgeClass.getMethod("createDatabase", lotus.domino.Session.class, Long.TYPE);
						}
						catch (Exception e) {
							//
						}
						try {
							createDocument = backendBridgeClass.getMethod("createDocument", lotus.domino.Database.class, Long.TYPE);
						}
						catch (Exception e) {
							//
						}
					}
					catch (Exception e) {
						//
					}

					try {
						Class<?> xspNativeClass = Class.forName("com.ibm.domino.napi.c.xsp.XSPNative");
						try {
							createXPageSession = xspNativeClass.getMethod("createXPageSession", String.class, Long.TYPE, Boolean.TYPE, Boolean.TYPE);
						}
						catch (Exception e) {
							//
						}
						try {
							getDBHandle = xspNativeClass.getMethod("getDBHandle", Database.class);
						}
						catch (Exception e) {
							//
						}
					}
					catch (Exception e) {
						//
					}
				}
				catch (Exception t) {
					//
				}
				return null;
			}
		});
	}

	/**
	 * Reads the C handle for the specific legacy {@link Document}
	 * 
	 * @param doc document
	 * @return handle
	 */
	public static long getDocHandle(Document doc) {
		initialize();

		if (getDocumentHandleRW==null) {
			throw new NotesError(0, "Required method BackendBridge.getDocumentHandleRW(Document) class not available in this environment");
		}

		try {
			long cHandle = (Long) getDocumentHandleRW.invoke(null, doc);
			return cHandle;
		} catch (Exception e) {
			throw new NotesError(0, "Could not read document c handle", e);
		}
	}

	/**
	 * Reads the C handle for the specific legacy {@link Database}
	 * 
	 * @param db database
	 * @return handle
	 */
	public static long getDBHandle(Database db) {
		initialize();

		if (getDBHandle==null) {
			throw new NotesError(0, "Required method XSPNative.getDBHandle(Database) not available in this environment");
		}

		try {
			long cHandle = (Long) getDBHandle.invoke(null, db);
			return cHandle;
		} catch (Exception e) {
			throw new NotesError(0, "Could not get db handle", e);
		}
	}

	/**
	 * Converts a C handle to a legacy {@link Document}
	 * 
	 * @param db parent database
	 * @param handle handle
	 * @return document
	 * @deprecated does not seem to work yet, always returns null
	 */
	public static Document createDocument(Database db, long handle) {
		initialize();

		if (createDocument==null) {
			throw new NotesError(0, "Required method BackendBridge.createDocument(Database,long) not available in this environment");
		}

		try {
			Document doc = (Document) createDocument.invoke(null, db, handle);
			return doc;
		} catch (Exception e) {
			throw new NotesError(0, "Could not convert document c handle", e);
		}
	}

	/**
	 * Method to convert a handle to a legacy {@link Database}
	 * 
	 * @param session session
	 * @param handle handle
	 * @return legacy database
	 * @deprecated does not seem to work yet, always returns null
	 */
	public static Database createDatabase(Session session, long handle) {
		initialize();

		if (createDatabase==null) {
			throw new NotesError(0, "Required method BackendBridge.createDatabase(Session,long) not available in this environment");
		}

		try {
			Database db = (Database) createDatabase.invoke(null, null, handle);
			db = (Database) createDatabase.invoke(null, session, handle);
			return db;
		} catch (Exception e) {
			throw new NotesError(0, "Could not convert database c handle", e);
		}
	}

	/**
	 * Creates a legacy {@link Session} for a usernames list
	 * 
	 * @param userName Notes username, either in canonical or abbreviated format
	 * @param privileges user privileges
	 * @return Session
	 */
	public static Session createSessionAs(String userName, EnumSet<Privileges> privileges) {
		NotesNamesList namesList = NotesNamingUtils.buildNamesList(userName);
		NotesNamingUtils.setPrivileges(namesList, privileges);

		return createSessionAs(namesList);
	}

	/**
	 * Creates a legacy {@link Session} for a usernames list
	 * 
	 * @param userNamesList user names list with name, wildcards and groups
	 * @param privileges user privileges
	 * @return Session
	 */
	public static Session createSessionAs(List<String> userNamesList, EnumSet<Privileges> privileges) {
		final List<String> userNamesListCanonical = new ArrayList<String>();
		for (String currName : userNamesList) {
			userNamesListCanonical.add(NotesNamingUtils.toCanonicalName(currName));
		}
		NotesNamesList namesList = NotesNamingUtils.writeNewNamesList(userNamesListCanonical);
		NotesNamingUtils.setPrivileges(namesList, privileges);

		return createSessionAs(namesList);
	}

	/**
	 * Creates a legacy {@link Session} for a usernames list
	 * 
	 * @param namesList names list of user to create the session for
	 * @return Session
	 */
	public static Session createSessionAs(NotesNamesList namesList) {
		initialize();

		if (createXPageSession==null) {
			throw new NotesError(0, "Required method XSPNative.createXPageSession(String, long, boolean, boolean) not available in this environment");
		}

		try {
			long hList = NotesJNAContext.is64Bit() ? namesList.getHandle64() : namesList.getHandle32();
			final Session session = (Session) createXPageSession.invoke(null, namesList.getNames().get(0), hList, true, false);

			final long[] longHandle = new long[1];
			final int[] intHandle = new int[1];

			if (NotesJNAContext.is64Bit()) {
				Long oldHandle = (Long) NotesGC.getCustomValue("FakeSessionHandle");
				if (oldHandle==null) {
					oldHandle = Long.valueOf(0);
				}
				Long newHandle = oldHandle.longValue()+1;
				NotesGC.setCustomValue("FakeSessionHandle", newHandle.longValue());
				longHandle[0] = newHandle.longValue();

			}
			else {
				Integer oldHandle = (Integer) NotesGC.getCustomValue("FakeSessionHandle");
				if (oldHandle==null) {
					oldHandle = Integer.valueOf(0);
				}
				Integer newHandle = oldHandle.intValue()+1;
				NotesGC.setCustomValue("FakeSessionHandle", newHandle.intValue());
				intHandle[0] = newHandle.intValue();
			}

			NotesGC.__objectCreated(Session.class, new DummySessionRecyclableNotesObject(intHandle, longHandle, namesList.getNames(), session));
			return session;
		}
		catch (Exception t) {
			throw new NotesError(0, "Could not create session", t);
		}
	}

	/**
	 * Implementation of {@link IRecyclableNotesObject} that recycles a legacy {@link Session}
	 * as part of the auto GC process
	 * 
	 * @author Karsten Lehmann
	 */
	private static final class DummySessionRecyclableNotesObject implements IRecyclableNotesObject {
		private final int[] intHandle;
		private final long[] longHandle;
		private final List<String> userNamesListCanonical;
		private final Session session;

		private DummySessionRecyclableNotesObject(int[] intHandle, long[] longHandle,
				List<String> userNamesListCanonical, Session session) {
			this.intHandle = intHandle;
			this.longHandle = longHandle;
			this.userNamesListCanonical = userNamesListCanonical;
			this.session = session;
		}

		@Override
		public void recycle() {
			try {
				session.recycle();
			}
			catch (NotesException ignore) {}
		}

		@Override
		public boolean isRecycled() {
			try {
				session.isOnServer();
			}
			catch (NotesException e) {
				if (e.id==4376 || e.id==4466)
					return true;
			}
			catch (Exception t) {
				t.printStackTrace();
				return true;
			}
			return false;
		}

		@Override
		public boolean isNoRecycle() {
			return false;
		}
		
		@Override
		public long getHandle64() {
			return longHandle[0];
		}

		@Override
		public int getHandle32() {
			return intHandle[0];
		}

		@Override
		public String toString() {
			if (isRecycled()) {
				return "Session [recycled]";
			}
			else {
				return "Session [names="+userNamesListCanonical.toString()+"]";
			}
		}
	}

	/**
	 * Converts a legacy {@link Database} to a {@link NotesDatabase}
	 * 
	 * @param db legacy DB
	 * @return JNA db
	 */
	public static NotesDatabase toNotesDatabase(final Database db) {
		return new NotesDatabase(new IAdaptable() {

			@Override
			public <T> T getAdapter(Class<T> clazz) {
				if (clazz == Database.class)
					return (T) db;
				else
					return null;
			}
		});
	}
	
	/**
	 * Converts a legacy {@link Document} to a {@link NotesNote}
	 * 
	 * @param doc legacy document
	 * @return JNA note
	 */
	public static NotesNote toNotesNote(final Document doc) {
		return new NotesNote(new IAdaptable() {

			@Override
			public <T> T getAdapter(Class<T> clazz) {
				if (clazz == Document.class)
					return (T) doc;
				else
					return null;
			}
		});
	}

}
