package com.mindoo.domino.jna.test;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.utils.NotesNamingUtils;

import junit.framework.Assert;
import lotus.domino.Session;

/**
 * Tests case for note signing
 * 
 * @author Karsten Lehmann
 */
public class TestSignNote extends BaseJNATestClass {

	@Test
	public void testSignDb() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = getFakeNamesDb();
				NotesNote note = db.createNote();
				String signerBefore = note.getSigner();
				
				Assert.assertEquals("Note is not signed", "", signerBefore);
				
				String userName = session.getUserName();
				note.sign();
				
				String signerAfter = note.getSigner();
				
				Assert.assertTrue("Note has been signed", NotesNamingUtils.equalNames(signerAfter, userName));
				return null;
			}
		});
	}

}
