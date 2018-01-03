package com.mindoo.domino.jna.test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.domino.napi.c.xsp.XSPNative;
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesCollection.Direction;
import com.mindoo.domino.jna.NotesCollection.EntriesAsListCallback;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNamesList;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.Find;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.utils.NotesNamingUtils;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.Name;
import lotus.domino.Session;

public class TestNamesListCreation extends BaseJNATestClass {

//	@Test
	public void testNamesListCreation_listComparisonWithLegacy() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				Database dbLegacyAPI = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());
				
				List<String> userNamesListJNA = NotesNamingUtils.getUserNamesList(session.getEffectiveUserName());
				Assert.assertTrue("JNA names list contains the current username", userNamesListJNA.contains(session.getEffectiveUserName()));
				
				Vector<Name> userGroupNamesList = session.getUserGroupNameList();
				for (Name currNameLegacy : userGroupNamesList) {
					Assert.assertTrue("JNA list contains "+currNameLegacy.getCanonical(), userNamesListJNA.contains(currNameLegacy.getCanonical()));
				}
				Name currUserName = session.getUserNameObject();
				Assert.assertTrue("JNA usernames list contains "+currUserName.getCanonical(), userNamesListJNA.contains(currUserName.getCanonical()));
				Assert.assertTrue("JNA usernames list contains "+currUserName.getCommon(), userNamesListJNA.contains(currUserName.getCanonical()));
				Assert.assertTrue("JNA usernames list contains *", userNamesListJNA.contains("*"));
				
				return null;
			}
		});
	}
	
	@Test
	public void testNamesListCreation_readViewDataAsFakeUser() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				int rnd = (int) (10000*Math.random());
				
				String fakeDocFirstName="Hiddendoc_Firstname_"+rnd;
				String fakeDocLastName="Hiddendoc_Lastname";

				String fakeGroup = "FakeGroup_"+rnd;
				String fakeRole = "[FakeRole_"+rnd+"_123456789012345678]";
				
				List<String> fakesUserNamesList = Arrays.asList(
						"Fake User"+rnd+"/Mindoo",
						"Fake User"+rnd,
						"*/Mindoo",
						"*",
						fakeGroup,
						fakeRole);

				NotesNamesList namesList = NotesNamingUtils.writeNewNamesList(fakesUserNamesList);
				Session sessionAsUser = XSPNative.createXPageSession("CN=Fake User"+rnd+"/O=Mindoo", namesList.getHandle64(), true, false);
				System.out.println(sessionAsUser.getEffectiveUserName());
				Database dbTest = sessionAsUser.getDatabase("", "fakenames.nsf");
				Document docTmp = dbTest.createDocument();
				Vector v = sessionAsUser.evaluate("@Usernameslist", docTmp);
				System.out.println("User names list: "+v);
				docTmp.recycle();
				sessionAsUser.recycle();
				
				NotesDatabase dbData = new NotesDatabase("", "fakenames.nsf", fakesUserNamesList);
				
				Database dbLegacyAPI = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());
				
				Document docHidden = dbLegacyAPI.createDocument();
				
				docHidden.replaceItemValue("Form", "Person");
				docHidden.replaceItemValue("Type", "Person");
				docHidden.replaceItemValue("Firstname", fakeDocFirstName);
				docHidden.replaceItemValue("Lastname", fakeDocLastName);
				
				//add admin reader field
				Item itmReadersAdmin = docHidden.replaceItemValue("readers_admin", "[ReadAll]");
				itmReadersAdmin.setNames(true);
				itmReadersAdmin.setReaders(true);
				
				//add reader field for user fake group
//				Item itmReadersFakeUser = docHidden.replaceItemValue("readers", 
//						NotesNamingUtils.toCanonicalName("Fake User"+rnd+"/Mindoo"));
				Item itmReadersFakeUser = docHidden.replaceItemValue("readers", 
						fakeRole);
				itmReadersFakeUser.setNames(true);
				itmReadersFakeUser.setReaders(true);

				final String unid = docHidden.getUniversalID();

				docHidden.save(true, false);
				docHidden.recycle();
				
				try {
					NotesCollection colFromDbData = dbData.openCollectionByName("PeopleFlatMultiColumnSort");
					try {
						colFromDbData.update();

						colFromDbData.resortView("lastname", Direction.Ascending);
						
						List<NotesViewEntryData> entries = colFromDbData.getAllEntriesByKey(EnumSet.of(Find.EQUAL),
								EnumSet.of(ReadMask.SUMMARY, ReadMask.NOTEID, ReadMask.NOTEUNID),
								new EntriesAsListCallback(Integer.MAX_VALUE) {
							@Override
							protected boolean isAccepted(NotesViewEntryData entryData) {
								return unid.equals(entryData.getUNID());
							}
						},
								fakeDocLastName);
						
						System.out.println("Lookup result as fake user: "+entries);
						Assert.assertFalse("We should be able to find the document as fake user", entries.isEmpty());
					}
					finally {
						colFromDbData.recycle();
					}
				}
				finally {
					dbData.recycle();
				}
				
				dbData = new NotesDatabase(session, "", "fakenames.nsf");
				try {
					NotesCollection colFromDbData = dbData.openCollectionByName("PeopleFlatMultiColumnSort");
					try {
						colFromDbData.update();

						colFromDbData.resortView("lastname", Direction.Ascending);
						
						List<NotesViewEntryData> entries = colFromDbData.getAllEntriesByKey(EnumSet.of(Find.EQUAL, Find.PARTIAL),
								EnumSet.of(ReadMask.SUMMARY, ReadMask.NOTEID, ReadMask.NOTEUNID),
								new EntriesAsListCallback(Integer.MAX_VALUE) {
							@Override
							protected boolean isAccepted(NotesViewEntryData entryData) {
								return unid.equals(entryData.getUNID());
							}
						},
								fakeDocLastName);
						
						System.out.println("Lookup result as normal user: "+entries);
						Assert.assertTrue("We should not be able to find the document as normal user", entries.isEmpty());
					}
					finally {
						colFromDbData.recycle();
					}
				}
				finally {
					dbData.recycle();
				}
				
				return null;
			}
		});
	
	}

}