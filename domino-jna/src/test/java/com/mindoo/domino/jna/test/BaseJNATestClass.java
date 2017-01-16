package com.mindoo.domino.jna.test;

import java.util.concurrent.Callable;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.utils.NotesInitUtils;
import com.sun.jna.Native;

import lotus.domino.Database;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;
import lotus.domino.Session;

import org.junit.BeforeClass;
import org.junit.AfterClass;

public class BaseJNATestClass {
	public static final String DBPATH_FAKENAMES_VIEWS_NSF = "fakenames-views.nsf";
	public static final String DBPATH_FAKENAMES_NSF = "fakenames.nsf";
	private ThreadLocal<Session> m_threadSession = new ThreadLocal<Session>();
	private static boolean m_notesInitExtendedCalled = false;
	
	@BeforeClass
	public static void initNotes() {
		String notesProgramDir = System.getenv("Notes_ExecDirectory");
		String notesIniPath = System.getenv("NotesINI");
		
		if (notesProgramDir!=null && notesProgramDir.length()>0 && notesIniPath!=null && notesIniPath.length()>0) {
			NotesInitUtils.notesInitExtended(new String[] {
					notesProgramDir,
					"="+notesIniPath
			});
			m_notesInitExtendedCalled = true;
		}
		NotesThread.sinitThread();
		Native.setProtected(true);
	}
	
	@AfterClass
	public static void termNotes() {
		if (m_notesInitExtendedCalled) {
			NotesInitUtils.notesTerm();
		}
		NotesThread.stermThread();
	}
	
	public NotesDatabase getFakeNamesDb() throws NotesException {
		NotesDatabase db = new NotesDatabase(getSession(), "", DBPATH_FAKENAMES_NSF);
		return db;
	}

	public NotesDatabase getFakeNamesViewsDb() throws NotesException {
		NotesDatabase db = new NotesDatabase(getSession(), "", DBPATH_FAKENAMES_VIEWS_NSF);
		return db;
	}

	public Database getFakeNamesDbLegacy() throws NotesException {
		Database db = getSession().getDatabase("", DBPATH_FAKENAMES_NSF);
		return db;
	}

	public Database getFakeNamesViewsDbLegacy() throws NotesException {
		Database db = getSession().getDatabase("", DBPATH_FAKENAMES_VIEWS_NSF);
		return db;
	}

	public Session getSession() {
		return m_threadSession.get();
	}
	
	public <T> T runWithSession(final IDominoCallable<T> callable) {
		final Session[] session = new Session[1];
		try {
			session[0] = NotesFactory.createSession();
			session[0].setTrackMillisecInJavaDates(true);
			m_threadSession.set(session[0]);
			
			T result = NotesGC.runWithAutoGC(new Callable<T>() {

				@Override
				public T call() throws Exception {
					NotesGC.setDebugLoggingEnabled(true);
					
					T result = callable.call(session[0]);
					return result;
				}
			});
			return result;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		finally {
			if (session[0]!=null) {
				try {
					session[0].recycle();
				} catch (NotesException e) {
					e.printStackTrace();
				}
			}
			m_threadSession.set(null);
		}
	}
	
	public static interface IDominoCallable<T> {
		
		public T call(Session session) throws Exception;
		
	}
}
