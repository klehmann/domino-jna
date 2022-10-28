package com.mindoo.domino.jna.test;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.gc.NotesGC;

import junit.framework.Assert;
import lotus.domino.Session;

public class TestNotePrimaryKey extends BaseJNATestClass {
	//switch to change the used lookup mode
	private final String propEnforceRemoteNamedObjectSearch = NotesDatabase.class.getName()+".namedobjects.enforceremote";

	@Test
	public void testPrimaryKeyLookup() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				//it is required to use a local database here to test all code paths.
				//
				//the C API methods that return data from the named object table are
				//only available for local DBs, so we had to build a (slower) workaround
				//for remote database based on NSF search
				NotesDatabase dbJNA = new NotesDatabase("", "fakenames.nsf", "");

				String pkCategory = "testcategory";
				String pkObjectId = "configuration_"+System.currentTimeMillis();

				NotesNote note = dbJNA.createNote();
				assertFalse(note.isGhost());
				
				//this adds a $name item to the note with a format similar to profile documents,
				//e.g. $app_012testcategory_myobjectid (where 012 is the length of the following
				//category name)
				//NSF has an internal table (named object table) that will automatically index
				//normal notes and ghost notes (special notes created via NotesDatabase.createGhostNote())
				//that do not show up in views/searches) with a $name item. We can use efficient
				//lookup methods to find these notes without the need to create lookup views
				note.setPrimaryKey(pkCategory, pkObjectId);
				note.replaceItemValue("Type", "Person");
				note.replaceItemValue("Lastname", "1. Primary Key test");
				note.replaceItemValue("Firstname", "1. Primary Key test");
				note.update();

				{
					NotesNote noteFoundViaPK = dbJNA.openNoteByPrimaryKey(pkCategory, pkObjectId);
					Assert.assertNotNull("note could be found via primary key", noteFoundViaPK);
					Assert.assertEquals("note found via primary key has correct UNID", note.getUNID(), noteFoundViaPK.getUNID());
				}
				{
					Map<String,Map<String,Integer>> notesByCategoryAndPK = dbJNA.getAllNotesByPrimaryKey();
					Assert.assertTrue("object id in map", notesByCategoryAndPK.containsKey(pkCategory));
					
					Map<String,Integer> notesByPK = notesByCategoryAndPK.get(pkCategory);
					Assert.assertTrue("object id in map", notesByPK.containsKey(pkObjectId));
					Assert.assertTrue("value has the right note id", notesByPK.get(pkObjectId) == note.getNoteId());
				}
				{
					Map<String,Integer> notesByPK = dbJNA.getAllNotesByPrimaryKey(pkCategory);
					Assert.assertTrue("object id in map", notesByPK.containsKey(pkObjectId));
					Assert.assertTrue("value has the right note id", notesByPK.get(pkObjectId) == note.getNoteId());
				}

				// switch to remote mode, using NSFSearchExtended3
				NotesGC.setCustomValue(propEnforceRemoteNamedObjectSearch, Boolean.TRUE);

				{
					NotesNote noteFoundViaPK = dbJNA.openNoteByPrimaryKey(pkCategory, pkObjectId);
					Assert.assertNotNull("Note could be found via primary key", noteFoundViaPK);
					Assert.assertEquals("note found via primary key has correct UNID", note.getUNID(), noteFoundViaPK.getUNID());
				}
				{
					Map<String,Map<String,Integer>> notesByCategoryAndPK = dbJNA.getAllNotesByPrimaryKey();
					Assert.assertTrue("object id in map", notesByCategoryAndPK.containsKey(pkCategory));
					
					Map<String,Integer> notesByPK = notesByCategoryAndPK.get(pkCategory);
					Assert.assertTrue("object id in map", notesByPK.containsKey(pkObjectId));
					Assert.assertTrue("value has the right note id", notesByPK.get(pkObjectId) == note.getNoteId());
				}
				{
					Map<String,Integer> notesByPK = dbJNA.getAllNotesByPrimaryKey(pkCategory);
					Assert.assertTrue("object id in map", notesByPK.containsKey(pkObjectId));
					Assert.assertTrue("value has the right note id", notesByPK.get(pkObjectId) == note.getNoteId());
				}

				//now delete the note and see if the index has been updated
				note.delete();

				// switch to local mode
				NotesGC.setCustomValue(propEnforceRemoteNamedObjectSearch, Boolean.FALSE);

				{
					NotesNote noteFoundViaPK = dbJNA.openNoteByPrimaryKey(pkCategory, pkObjectId);
					Assert.assertNull("deleted note could not be found via primary key", noteFoundViaPK);
				}
				{
					Map<String,Integer> notesByPK = dbJNA.getAllNotesByPrimaryKey(pkCategory);
					Assert.assertTrue("deleted object not in map", !notesByPK.containsKey(pkObjectId));
				}

				// switch to remote mode, using NSFSearchExtended3
				NotesGC.setCustomValue(propEnforceRemoteNamedObjectSearch, Boolean.TRUE);
				
				{
					NotesNote noteFoundViaPK = dbJNA.openNoteByPrimaryKey(pkCategory, pkObjectId);
					Assert.assertNull("deleted note could not be found via primary key", noteFoundViaPK);
				}
				{
					Map<String,Integer> notesByPK = dbJNA.getAllNotesByPrimaryKey(pkCategory);
					Assert.assertTrue("deleted object not in map", !notesByPK.containsKey(pkObjectId));
				}

				System.out.println("Done testing named objects");
				return null;
			}
		});
	}

	@Test
	public void testPrimaryKeyLookupWithGhost() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				//it is required to use a local database here to test all code paths.
				//
				//the C API methods that return data from the named object table are
				//only available for local DBs, so we had to build a (slower) workaround
				//for remote database based on NSF search
				NotesDatabase dbJNA = new NotesDatabase("", "fakenames.nsf", "");

				String pkCategory = "testcategory";
				String pkObjectId = "configuration_"+System.currentTimeMillis();

				NotesNote note = dbJNA.createGhostNote();
				assertTrue(note.isGhost());
				
				//this adds a $name item to the note with a format similar to profile documents,
				//e.g. $app_012testcategory_myobjectid (where 012 is the length of the following
				//category name)
				//NSF has an internal table (named object table) that will automatically index
				//normal notes and ghost notes (special notes created via NotesDatabase.createGhostNote())
				//that do not show up in views/searches) with a $name item. We can use efficient
				//lookup methods to find these notes without the need to create lookup views
				note.setPrimaryKey(pkCategory, pkObjectId);
				note.replaceItemValue("Type", "Person");
				note.replaceItemValue("Lastname", "1. Primary Key test");
				note.replaceItemValue("Firstname", "1. Primary Key test");
				note.update();

				{
					NotesNote noteFoundViaPK = dbJNA.openNoteByPrimaryKey(pkCategory, pkObjectId);
					Assert.assertNotNull("note could be found via primary key", noteFoundViaPK);
					Assert.assertEquals("note found via primary key has correct UNID", note.getUNID(), noteFoundViaPK.getUNID());
				}
				{
					Map<String,Map<String,Integer>> notesByCategoryAndPK = dbJNA.getAllNotesByPrimaryKey();
					Assert.assertTrue("object id in map", notesByCategoryAndPK.containsKey(pkCategory));
					
					Map<String,Integer> notesByPK = notesByCategoryAndPK.get(pkCategory);
					Assert.assertTrue("object id in map", notesByPK.containsKey(pkObjectId));
					Assert.assertTrue("value has the right note id", notesByPK.get(pkObjectId) == note.getNoteId());
				}
				{
					Map<String,Integer> notesByPK = dbJNA.getAllNotesByPrimaryKey(pkCategory);
					Assert.assertTrue("object id in map", notesByPK.containsKey(pkObjectId));
					Assert.assertTrue("value has the right note id", notesByPK.get(pkObjectId) == note.getNoteId());
				}

				// switch to remote mode, using NSFSearchExtended3
				NotesGC.setCustomValue(propEnforceRemoteNamedObjectSearch, Boolean.TRUE);

				{
					NotesNote noteFoundViaPK = dbJNA.openNoteByPrimaryKey(pkCategory, pkObjectId);
					Assert.assertNotNull("Note could be found via primary key", noteFoundViaPK);
					Assert.assertEquals("note found via primary key has correct UNID", note.getUNID(), noteFoundViaPK.getUNID());
				}
				{
					Map<String,Map<String,Integer>> notesByCategoryAndPK = dbJNA.getAllNotesByPrimaryKey();
					Assert.assertTrue("object id in map", notesByCategoryAndPK.containsKey(pkCategory));
					
					Map<String,Integer> notesByPK = notesByCategoryAndPK.get(pkCategory);
					Assert.assertTrue("object id in map", notesByPK.containsKey(pkObjectId));
					Assert.assertTrue("value has the right note id", notesByPK.get(pkObjectId) == note.getNoteId());
				}
				{
					Map<String,Integer> notesByPK = dbJNA.getAllNotesByPrimaryKey(pkCategory);
					Assert.assertTrue("object id in map", notesByPK.containsKey(pkObjectId));
					Assert.assertTrue("value has the right note id", notesByPK.get(pkObjectId) == note.getNoteId());
				}

				//now delete the note and see if the index has been updated
				note.delete();

				// switch to local mode
				NotesGC.setCustomValue(propEnforceRemoteNamedObjectSearch, Boolean.FALSE);

				{
					NotesNote noteFoundViaPK = dbJNA.openNoteByPrimaryKey(pkCategory, pkObjectId);
					Assert.assertNull("deleted note could not be found via primary key", noteFoundViaPK);
				}
				{
					Map<String,Integer> notesByPK = dbJNA.getAllNotesByPrimaryKey(pkCategory);
					Assert.assertTrue("deleted object not in map", !notesByPK.containsKey(pkObjectId));
				}

				// switch to remote mode, using NSFSearchExtended3
				NotesGC.setCustomValue(propEnforceRemoteNamedObjectSearch, Boolean.TRUE);
				
				{
					NotesNote noteFoundViaPK = dbJNA.openNoteByPrimaryKey(pkCategory, pkObjectId);
					Assert.assertNull("deleted note could not be found via primary key", noteFoundViaPK);
				}
				{
					Map<String,Integer> notesByPK = dbJNA.getAllNotesByPrimaryKey(pkCategory);
					Assert.assertTrue("deleted object not in map", !notesByPK.containsKey(pkObjectId));
				}

				System.out.println("Done testing named objects");
				return null;
			}
		});
	}
}
