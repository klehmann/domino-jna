package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.NamedObjectInfo;
import com.mindoo.domino.jna.NotesNote;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Session;

/**
 * Test case to see if we write the same stuff for named notes that the
 * legacy APIs do.
 * 
 * @author Karsten Lehmann
 */
public class TestNamedNotesCompatibility extends BaseJNATestClass {

	@Test
	public void testNamedNotes() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbJNA = getFakeNamesDb();
				Database dbLegacy = getFakeNamesDbLegacy();
				
				String uuid = UUID.randomUUID().toString();
				
				//should not exist:
				NotesNote note = dbJNA.openNamedNote("testname", uuid, false);
				assertNull(note);
				//set createOnFail to true to get a new doc
				NotesNote createdNote = dbJNA.openNamedNote("testname", uuid, true);
				assertTrue(createdNote.isNewNote());
				//check name/username
				assertEquals("testname", createdNote.getNamedNoteName());
				assertEquals(uuid, createdNote.getNamedNoteUsername());
				//sate note
				createdNote.update();
				assertFalse(createdNote.isNewNote());
				
				//try to find the note via search
				List<NamedObjectInfo> namedNotes = dbJNA.getNamedNoteInfos("testname");
				boolean foundNote = namedNotes
						.stream()
						.anyMatch((entry) -> {
							return entry.getNoteId() == createdNote.getNoteId();
						});
				assertTrue(foundNote);
				
				//now check
				Document docLegacy = dbLegacy.getNamedDocument("testname", uuid);
				assertNotNull(docLegacy);
				assertFalse(docLegacy.isNewNote());
				assertEquals(createdNote.getUNID(), docLegacy.getUniversalID());
				
				//delete the notes and make sure we cannot find it anymore
				createdNote.delete();

				namedNotes = dbJNA.getNamedNoteInfos("testname");
				foundNote = namedNotes
						.stream()
						.anyMatch((entry) -> {
							return entry.getNoteId() == createdNote.getNoteId();
						});
				assertFalse(foundNote);

				//check legacy API for deletion:
				docLegacy = dbLegacy.getNamedDocument("testname", uuid);
				assertTrue(docLegacy.isNewNote());
				
				
				return null;
			}
		});
	}
}
