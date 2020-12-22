package com.mindoo.domino.jna.test;

import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesNoteCollection;
import com.mindoo.domino.jna.constants.NoteClass;

import lotus.domino.Session;

/**
 * Testcase for {@link NotesNoteCollection} class
 * 
 * @author Karsten Lehmann
 */
public class TestNoteCollection extends BaseJNATestClass {

	@Test
	public void testNoteCollectionWithData() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				withTempDb((db) -> {
					NotesNote note1 = db.createNote();
					note1.replaceItemValue("Subject", "Test");
					note1.update();
					
					NotesNote note2 = db.createNote();
					note2.replaceItemValue("Subject", "Test2");
					note2.update();
					
					NotesNoteCollection nc = db
							.createNoteCollection()
							.selectAllDataNotes()
							.withSelectionFormula("Subject=\"Test\"")
							.build();
					
					Assert.assertEquals(1, nc.getCount());
					
					int noteId = nc.getFirstId();
					Assert.assertEquals(note1.getNoteId(), noteId);
				});
				
				return null;
			}
		});
	}
	
	@Test
	public void testNoteCollectionWithDesign() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = getFakeNamesDb();
				NotesNoteCollection nc = db
						.createNoteCollection()
						.selectAllDesignElements()
						.build();
				
				boolean hasForm = false;
				boolean hasView = false;
				
				for (Integer currNoteId : nc) {
					NotesNote currNote = db.openNoteById(currNoteId);
					Assert.assertNotNull(currNote);
					EnumSet<NoteClass> currNoteClass = currNote.getNoteClass();
					if (currNoteClass.contains(NoteClass.FORM)) {
						hasForm = true;
					}
					else if (currNoteClass.contains(NoteClass.VIEW)) {
						hasView = true;
					}
					
//					String currTitle = currNote.getItemValueString("$TITLE");
//					System.out.println("noteid="+currNote.getNoteId()+"\tnoteclass="+currNoteClass+"\ttitle="+currTitle);
//					currNote.recycle();
				}
				
				Assert.assertTrue(hasForm);
				Assert.assertTrue(hasView);
				
				return null;
			}
		});
		
	}
}
