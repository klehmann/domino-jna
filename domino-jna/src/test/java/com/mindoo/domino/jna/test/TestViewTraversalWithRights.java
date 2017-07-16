package com.mindoo.domino.jna.test;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesCollection.EntriesAsListCallback;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.ItemType;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.utils.IDUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils;

import lotus.domino.NotesException;
import lotus.domino.Session;

/**
 * Test case that reads view data as another user
 * 
 * @author Karsten Lehmann
 */
public class TestViewTraversalWithRights extends BaseJNATestClass {

	public NotesDatabase getFakeNamesDbAs(List<String> namesList) throws NotesException {
		NotesDatabase db = new NotesDatabase(getSession(), "", "fakenames.nsf", namesList);
		return db;
	}

	public NotesDatabase getFakeNamesDbAs(String userName) throws NotesException {
		NotesDatabase db = new NotesDatabase(getSession(), "", "fakenames.nsf", userName);
		return db;
	}

	@Test
	public void testViewTraversal_readWithRights() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbDataAsPeterTester = getFakeNamesDbAs("CN=Peter Tester/O=Mindoo");

				List<String> idUserNamesListNoReadAll = NotesNamingUtils.getUserNamesList(IDUtils.getCurrentUsername());
				idUserNamesListNoReadAll.remove("[ReadAll]");
				
				NotesDatabase dbDataAsIdUser = getFakeNamesDbAs(IDUtils.getCurrentUsername());

				final String hiddenDocUnid;
				final int hiddenDocId;

				List<String> readers = Arrays.asList("CN=Peter Tester/O=Mindoo", "[ReadAll]");
				{
					//create a new document with a reader field
					String timestamp = ((new Date())).toString();

					NotesNote hiddenDoc = dbDataAsPeterTester.createNote();
					hiddenDoc.replaceItemValue("Form", "Person");
					hiddenDoc.replaceItemValue("Type", "Person");
					hiddenDoc.replaceItemValue("Readerfield",
							EnumSet.of(ItemType.NAMES, ItemType.READERS, ItemType.SUMMARY),
							readers);
					hiddenDoc.replaceItemValue("Firstname", "Hidden1-firstname_"+timestamp);
					hiddenDoc.replaceItemValue("Lastname", "Hidden1-lastname_"+timestamp);
					hiddenDoc.update();
					hiddenDocUnid = hiddenDoc.getUNID();
					hiddenDocId = hiddenDoc.getNoteId();
					System.out.println("Created hidden doc with UNID "+hiddenDocUnid+" and note id "+hiddenDocId);
				}

				{
					//search the view to check if we find the document when we use
					//the fake user access identity
					NotesCollection colFromDbDataAsPeterTester = dbDataAsPeterTester.openCollectionByName("PeopleFlatMultiColumnSort");
					colFromDbDataAsPeterTester.update();

					colFromDbDataAsPeterTester.select(Arrays.asList(hiddenDocId), true);
					
					
					List<NotesViewEntryData> entries = colFromDbDataAsPeterTester.getAllEntries("0", 1,
							EnumSet.of(Navigate.NEXT_SELECTED), Integer.MAX_VALUE,
							EnumSet.of(ReadMask.NOTEUNID, ReadMask.SUMMARY, ReadMask.RETURN_READERSLIST), new EntriesAsListCallback(Integer.MAX_VALUE));

					System.out.println("Entries lookup #1: "+entries);
					Assert.assertTrue("Document with reader field has been found by fake user", !entries.isEmpty());
					
					List<String> noteReaders = entries.get(0).getReadersList();
					Assert.assertEquals("View lookup returned special readers column", readers, noteReaders);
					
				}
//
				{
					//now search the view as the current ID user to make sure he is not allowed
					//to see the document
					NotesCollection colFromDbDataAsIDUser = dbDataAsIdUser.openCollectionByName("PeopleFlatMultiColumnSort");
					colFromDbDataAsIDUser.update();

					colFromDbDataAsIDUser.select(Arrays.asList(hiddenDocId), true);

					List<NotesViewEntryData> entries = colFromDbDataAsIDUser.getAllEntries("0", 1,
							EnumSet.of(Navigate.NEXT_SELECTED), Integer.MAX_VALUE,
							EnumSet.of(ReadMask.NOTEUNID, ReadMask.SUMMARY), new EntriesAsListCallback(Integer.MAX_VALUE));

					System.out.println("Entries lookup #2: "+entries);
					Assert.assertTrue("Document with reader field has not been found by ID user", entries.isEmpty());
				}

				return null;
			}
		});

	}


}