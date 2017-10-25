package com.mindoo.domino.jna.test;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;

import junit.framework.Assert;
import lotus.domino.Database;
import lotus.domino.Session;
import lotus.domino.View;

/**
 * Test cases that read data from views
 * 
 * @author Karsten Lehmann
 */
public class TestViewMetaData extends BaseJNATestClass {

	@Test
	public void testViewTraversal_columnTitleLookup() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				Database dbDataLegacy = getFakeNamesDbLegacy();
				
				NotesCollection colFromDbData = dbData.openCollectionByName("People");
				colFromDbData.update();

				View viewPeople = dbDataLegacy.getView("People");
				int topLevelEntriesLegacy = viewPeople.getTopLevelEntryCount();
				
				int topLevelEntries = colFromDbData.getTopLevelEntries();
				System.out.println("Top level entries: "+topLevelEntries);
				
				Assert.assertEquals("Top level entries of JNA call is equal to legacy call", topLevelEntriesLegacy, topLevelEntries);
				
				return null;
			}
		});
	}

}