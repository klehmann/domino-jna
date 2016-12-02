package com.mindoo.domino.jna.utils;

import java.lang.reflect.Method;

import com.mindoo.domino.jna.errors.NotesError;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Session;

/**
 * Utility class that bridges between the C API calls provided in this project and
 * the legacy lotus.domino API.
 * 
 * @author Karsten Lehmann
 */
public class LegacyAPIUtils {
	private static boolean m_initialized;
	
	private static Method getDocumentHandleRW;
	private static Method createDocument;
	private static Method createDatabase;
	
	private static void initialize() {
		if (m_initialized)
			return;
		
		m_initialized = true;
		
		try {
			//This class only works when lwpd.domino.napi.jar and lwpd.commons.jar are in the classpath
			Class<?> cClass = Class.forName("com.ibm.domino.napi.c.C");
			Method initLibrary = cClass.getMethod("initLibrary", String.class);
			initLibrary.invoke(null, "");
			
			Class<?> backendBridgeClass = Class.forName("com.ibm.domino.napi.c.BackendBridge");
			getDocumentHandleRW = backendBridgeClass.getMethod("getDocumentHandleRW", lotus.domino.Document.class);
			createDatabase = backendBridgeClass.getMethod("createDatabase", lotus.domino.Session.class, Long.TYPE);
			createDocument = backendBridgeClass.getMethod("createDocument", lotus.domino.Database.class, Long.TYPE);
		}
		catch (Throwable t) {
			//API does not seem to be available, e.g. in Basic Client
		}
	}
	
	/**
	 * Reads the C handle for the specific legacy {@link Document}
	 * 
	 * @param doc document
	 * @return handle
	 */
	public static long getHandle(Document doc) {
		initialize();
		
		if (getDocumentHandleRW==null) {
			throw new NotesError(0, "Required BackendBridge class not available in this environment");
		}
		
		try {
			long cHandle = (Long) getDocumentHandleRW.invoke(null, doc);
			return cHandle;
		} catch (Throwable e) {
			throw new NotesError(0, "Could not read document c handle", e);
		}
	}
	
	/**
	 * Converts a C handle to a legacy {@link Document}
	 * 
	 * @param db parent database
	 * @param handle handle
	 * @return document
	 */
	public static Document createDocument(Database db, long handle) {
		initialize();
		
		if (createDocument==null) {
			throw new NotesError(0, "Required BackendBridge class not available in this environment");
		}
		
		try {
			Document doc = (Document) createDocument.invoke(null, db, handle);
			return doc;
		} catch (Throwable e) {
			throw new NotesError(0, "Could not convert document c handle", e);
		}
	}
	
	public static Database createDatabase(Session session, long handle) {
		initialize();
		
		if (createDatabase==null) {
			throw new NotesError(0, "Required BackendBridge class not available in this environment");
		}
		
		try {
			Database db = (Database) createDatabase.invoke(null, session, handle);
			return db;
		} catch (Throwable e) {
			throw new NotesError(0, "Could not convert database c handle", e);
		}
	}
}
