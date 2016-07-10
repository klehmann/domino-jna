package com.mindoo.domino.jna.test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.SignCallback;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.internal.NotesCAPI;

import lotus.domino.Session;

/**
 * Tests cases for database searches
 * 
 * @author Karsten Lehmann
 */
public class TestSignDb extends BaseJNATestClass {

	//commented out; requires native reading of DESIGN view collation structure
	//@Test
	public void testSignDb() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = new NotesDatabase(session, "", "test/signtest.nsf");
				db.signAll(NotesCAPI.NOTE_CLASS_ALLNONDATA, new SignCallback() {

					@Override
					public boolean shouldSign(NotesViewEntryData noteData, String currSigner) {
						return true;
					}

					@Override
					public Action noteSigned(NotesViewEntryData noteData) {
						System.out.println("Note signed: ID="+noteData.getNoteId()+", data="+noteData.getColumnDataAsMap());
						return Action.Continue;
					}
					
				});
				
				return null;
			}
		});
	}

}
