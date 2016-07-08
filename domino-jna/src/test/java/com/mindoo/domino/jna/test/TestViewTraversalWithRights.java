package com.mindoo.domino.jna.test;

import java.util.EnumSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesCollection.EntriesAsListCallback;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;

import lotus.domino.NotesException;
import lotus.domino.Session;

/**
 * Test case that reads view data as another user
 * 
 * @author Karsten Lehmann
 */
public class TestViewTraversalWithRights extends BaseJNATestClass {

	public NotesDatabase getFakeNamesDbAs(String userName) throws NotesException {
		NotesDatabase db = new NotesDatabase(getSession(), "", "fakenames.nsf", userName);
		return db;
	}

	@Test
	public void testViewTraversal_readWithRights() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDbAs("CN=Peter Tester/O=Mindoo");
				
				NotesCollection colFromDbData = dbData.openCollectionByName("PeopleFlatMultiColumnSort");
				colFromDbData.update();
				
				List<NotesViewEntryData> entries = colFromDbData.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT), Integer.MAX_VALUE,
						EnumSet.of(ReadMask.NOTEUNID), new EntriesAsListCallback(Integer.MAX_VALUE) {
					
					@Override
					protected boolean isAccepted(NotesViewEntryData entryData) {
						String unid = entryData.getUNID();
						if ("CC5C3F04C6E28CAE802572570066C096".equals(unid)) {
							return true;
						}
						else {
							return false;
						}
					}
					
				});
				
				Assert.assertTrue("Document with reader field has been found", !entries.isEmpty());
				
				return null;
			}
		});
	
	}


}