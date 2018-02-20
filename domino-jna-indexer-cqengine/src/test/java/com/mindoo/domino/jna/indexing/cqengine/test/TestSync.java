package com.mindoo.domino.jna.indexing.cqengine.test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.SearchCallback;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesOriginatorId;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder.ItemTableData;
import com.mindoo.domino.jna.sync.SyncResult;
import com.mindoo.domino.jna.sync.SyncUtil;

import junit.framework.Assert;
import lotus.domino.Session;

public class TestSync extends BaseJNATestClass {
	
	@Test
	public void testSyncUtil() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesGC.setDebugLoggingEnabled(false);
				NotesDatabase db = getFakeNamesDb();
				
//				final Level logLevel = Level.FINE;
				final Level logLevel = Level.WARNING;
				
				PersonSyncTarget target = new PersonSyncTarget() {
					public boolean isLoggable(Level level) {
						return level.intValue() >= logLevel.intValue();
					}
				};
				
				String selectionFormula;
				String anyUNIDToChange=null;
				
				NotesTimeDate prevSyncStart1;
				NotesTimeDate nextSyncStart1;
				final Set<String> unidsOfAllPersons = new HashSet<String>();
				{
					//use a selection formula to sync all persons
					selectionFormula = "Form=\"Person\"";
					
					//use NSFSearch to collect our expected UNIDs
					db.search(selectionFormula, "", EnumSet.of(Search.SESSION_USERNAME), EnumSet.of(NoteClass.DATA),
							null, new SearchCallback() {

						@Override
						public Action noteFound(NotesDatabase parentDb, int noteId, NotesOriginatorId oid, EnumSet<NoteClass> noteClass,
								EnumSet<NoteFlags> flags, NotesTimeDate dbCreated, NotesTimeDate noteModified,
								ItemTableData summaryBufferData) {
							unidsOfAllPersons.add(oid.getUNIDAsString());
							return Action.Continue;
						}
					});
					Assert.assertFalse("Selection contains data", unidsOfAllPersons.isEmpty());
					//pick a random UNID that we will change
					anyUNIDToChange = unidsOfAllPersons.iterator().next();
					
					System.out.println("Running sync #1 (initial sync), formula: "+selectionFormula);
					
					long t0=System.currentTimeMillis();
					
					Assert.assertTrue("Target is empty before first sync", target.getContent().isEmpty());
					
					SyncResult result1 = SyncUtil.sync(db, selectionFormula, target);
					long t1=System.currentTimeMillis();
					System.out.println("Sync result after "+(t1-t0)+"ms: "+result1);
					
					Assert.assertTrue("Entries have been added to the target", result1.getAddedToTarget() > 0);
					
					Set<String> unidsInIndex = new HashSet<String>();
					boolean anyLastNameNotStartingwithL = false;
					
					for (Person currPerson : target.getContent()) {
						unidsInIndex.add(currPerson.getUNID());
						if (!currPerson.getLastName().startsWith("L")) {
							anyLastNameNotStartingwithL = true;
						}
					}
					//make sure we have other lastnames so that we can check that our selection formula is working
					Assert.assertEquals("Our fakenames contain lastnames not starting with L", true, anyLastNameNotStartingwithL);
					
					Assert.assertEquals("Target contains the expected UNIDs", unidsOfAllPersons, unidsInIndex);
					
					prevSyncStart1 = result1.getPrevSince();
					Assert.assertNull("Prev sync start 1 is null", prevSyncStart1);
					nextSyncStart1 = result1.getNextSince();
					Assert.assertNotNull("Prev sync start 1 is null", nextSyncStart1);
				}

				NotesTimeDate prevSyncStart2;
				NotesTimeDate nextSyncStart2;
				{
					//rerun the sync without changing any document; should not sync anything new
					System.out.println("Running sync #2 (sync after nothing has changed), formula: "+selectionFormula);
					long t0=System.currentTimeMillis();
					SyncResult result2 = SyncUtil.sync(db, selectionFormula, target);
					
					Assert.assertEquals("Nothing got added", 0, result2.getAddedToTarget());
					Assert.assertEquals("Nothing got updated", 0, result2.getUpdatedInTarget());
					Assert.assertEquals("Nothing got removed", 0, result2.getRemovedFromTarget());
					Assert.assertEquals("Nothing new matching formula", 0, result2.getNoteCountMatchingFormula());
					Assert.assertEquals("Nothing new not matching formula", 0, result2.getNoteCountNotMatchingFormula());
					Assert.assertEquals("Nothing deleted", 0, result2.getNoteCountDeleted());
					
					prevSyncStart2 = result2.getPrevSince();
					nextSyncStart2 = result2.getNextSince();
					Assert.assertEquals("nextSyncStart1 is equal to prevSyncStart2", nextSyncStart1, prevSyncStart2);
					
					long t1=System.currentTimeMillis();
					System.out.println("Sync result after "+(t1-t0)+"ms: "+result2);
				}
				
				NotesTimeDate prevSyncStart3;
				NotesTimeDate nextSyncStart3;
				{
					//now change one document
					NotesNote note = db.openNoteByUnid(anyUNIDToChange);
					note.replaceItemValue("xyz", "123");
					note.update();
					note.recycle();
					
					//and rerun the sync to see if this gets detected
					System.out.println("Running sync #3 (sync after note was changed), formula: "+selectionFormula);
					long t0=System.currentTimeMillis();
					SyncResult result3 = SyncUtil.sync(db, selectionFormula, target);
					
					Assert.assertEquals("Nothing got added", 0, result3.getAddedToTarget());
					Assert.assertEquals("One note got updated", 1, result3.getUpdatedInTarget());
					Assert.assertEquals("Nothing got removed", 0, result3.getRemovedFromTarget());
					Assert.assertEquals("One new matching formula", 1, result3.getNoteCountMatchingFormula());
					Assert.assertEquals("Nothing new not matching formula", 0, result3.getNoteCountNotMatchingFormula());
					Assert.assertEquals("Nothing deleted", 0, result3.getNoteCountDeleted());
					
					prevSyncStart3 = result3.getPrevSince();
					nextSyncStart3 = result3.getNextSince();
					Assert.assertEquals("nextSyncStart2 is equal to prevSyncStart3", nextSyncStart2, prevSyncStart3);
					
					long t1=System.currentTimeMillis();
					System.out.println("Sync result after "+(t1-t0)+"ms: "+result3);
				}
				
				final Set<String> unidsOfLPersons = new HashSet<String>();
				{
					//change the selection formula to cause a purge operation, where we compare source and
					//target and remove everything not matching the formula
					selectionFormula = "Form=\"Person\" & @Begins(Lastname;\"L\")";
					System.out.println("Running sync #4 (changed formula), formula: "+selectionFormula);
					
					//use NSFSearch to collect our expected UNIDs
					db.search(selectionFormula, "", EnumSet.of(Search.SESSION_USERNAME), EnumSet.of(NoteClass.DATA),
							null, new SearchCallback() {

						@Override
						public Action noteFound(NotesDatabase parentDb, int noteId, NotesOriginatorId oid, EnumSet<NoteClass> noteClass,
								EnumSet<NoteFlags> flags, NotesTimeDate dbCreated, NotesTimeDate noteModified,
								ItemTableData summaryBufferData) {
							unidsOfLPersons.add(oid.getUNIDAsString());
							return Action.Continue;
						}
					});
					Assert.assertFalse("Selection contains any data", unidsOfLPersons.isEmpty());
					Assert.assertTrue("Refined selection returns less data", unidsOfLPersons.size() < unidsOfAllPersons.size());
					
					int expectedRemovalsFromIndex = unidsOfAllPersons.size() - unidsOfLPersons.size();
					
					long t0=System.currentTimeMillis();
					SyncResult result4 = SyncUtil.sync(db, selectionFormula, target);
					long t1=System.currentTimeMillis();
					System.out.println("Sync result after "+(t1-t0)+"ms: "+result4);
					
					Assert.assertEquals("Nothing got added", 0, result4.getAddedToTarget());
					Assert.assertEquals("Nothing got updated", 0, result4.getUpdatedInTarget());
					Assert.assertEquals(expectedRemovalsFromIndex+ "entries got removed", expectedRemovalsFromIndex, result4.getRemovedFromTarget());
					Assert.assertEquals("Nothing new matching formula", 0, result4.getNoteCountMatchingFormula());
					Assert.assertEquals("Nothing new not matching formula", 0, result4.getNoteCountNotMatchingFormula());
					Assert.assertEquals("Nothing deleted", 0, result4.getNoteCountDeleted());
					
					Set<String> unidsInIndex = new HashSet<String>();
					for (Person currPerson : target.getContent()) {
						Assert.assertTrue("Person with UNID "+currPerson.getUNID()+" was also found via NSFSearch", unidsOfLPersons.contains(currPerson.getUNID()));
						Assert.assertTrue("Lastname "+currPerson.getLastName()+" starts with L", currPerson.getLastName().startsWith("L"));
						unidsInIndex.add(currPerson.getUNID());
					}
					Assert.assertEquals("Index contains the expected UNIDs", unidsOfLPersons, unidsInIndex);
				}

				{
					selectionFormula = "Form=\"Person\"";
					System.out.println("Running sync #5 (formula back to original), formula: "+selectionFormula);
					
					int expectedAdditionsToIndex = unidsOfAllPersons.size() - unidsOfLPersons.size();
					
					long t0=System.currentTimeMillis();
					SyncResult result5 = SyncUtil.sync(db, selectionFormula, target);
					long t1=System.currentTimeMillis();
					System.out.println("Sync result after "+(t1-t0)+"ms: "+result5);
					
					Assert.assertEquals(expectedAdditionsToIndex+ " entries got added", expectedAdditionsToIndex, result5.getAddedToTarget());
					Assert.assertEquals("Nothing got updated", 0, result5.getUpdatedInTarget());
					Assert.assertEquals("Nothing got removed", 0, result5.getRemovedFromTarget());
					Assert.assertEquals(expectedAdditionsToIndex+" entries matching formula", expectedAdditionsToIndex, result5.getNoteCountMatchingFormula());
					Assert.assertEquals("Nothing new not matching formula", 0, result5.getNoteCountNotMatchingFormula());
					Assert.assertEquals("Nothing deleted", 0, result5.getNoteCountDeleted());
					
					Set<String> unidsInIndex = new HashSet<String>();
					boolean anyLastNameNotStartingwithL = false;
					
					for (Person currPerson : target.getContent()) {
						unidsInIndex.add(currPerson.getUNID());
						if (!currPerson.getLastName().startsWith("L")) {
							anyLastNameNotStartingwithL = true;
						}
					}
					//make sure we have other lastnames so that we can check that our selection formula is working
					Assert.assertEquals("Our fakenames contain lastnames not starting with L", true, anyLastNameNotStartingwithL);
					
					Assert.assertEquals("Target contains the expected UNIDs", unidsOfAllPersons, unidsInIndex);
				}
				
				return null;
			}
		});
	
	}
}
