package com.mindoo.domino.jna.test;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.internal.NotesJNAContext;

import junit.framework.Assert;
import lotus.domino.Session;

/**
 * Tests cases for Database reopen
 * 
 * @author Karsten Lehmann
 */
public class TestDbReopen extends BaseJNATestClass {

	@Test
	public void testDbReopen() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = getFakeNamesDb();
				NotesDatabase reopenedDb = db.reopenDatabase();
				
				if (NotesJNAContext.is64Bit()) {
					Assert.assertNotSame("Reopened database has different handle", db.getHandle64(), reopenedDb.getHandle64());
				}
				else {
					Assert.assertNotSame("Reopened database has different handle", db.getHandle32(), reopenedDb.getHandle32());
				}
				
				int dbOpenDatabaseId = db.getOpenDatabaseId();
				int reopenedDbOpenDatabaseId = reopenedDb.getOpenDatabaseId();
				
				Assert.assertEquals("OpenDatabaseId is equal when db is reopened", dbOpenDatabaseId, reopenedDbOpenDatabaseId);
				reopenedDb.recycle();
				db.recycle();

				return null;
			}
		});
	}

}
