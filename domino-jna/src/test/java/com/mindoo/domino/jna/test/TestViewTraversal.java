package com.mindoo.domino.jna.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesCollection.CollationInfo;
import com.mindoo.domino.jna.NotesCollection.Direction;
import com.mindoo.domino.jna.NotesCollection.EntriesAsListCallback;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.Find;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Session;
import lotus.domino.View;

public class TestViewTraversal extends BaseJNATestClass {

	@Test
	public void testViewTraversal_readAllEntryInDataDb() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = getFakeNamesDb();
				NotesCollection col = db.openCollectionByName("People");
				long t0=System.currentTimeMillis();
				List<NotesViewEntryData> entries = col.getAllEntries("0", 1,
						EnumSet.of(Navigate.NEXT_PEER),
						Integer.MAX_VALUE,
						EnumSet.of(
								ReadMask.NOTEID,
								ReadMask.SUMMARY,
								ReadMask.INDEXPOSITION,
								ReadMask.SUMMARYVALUES,
								ReadMask.NOTECLASS,
								ReadMask.NOTEUNID
								), new EntriesAsListCallback(Integer.MAX_VALUE));
				long t1=System.currentTimeMillis();
				System.out.println("Reading data of "+entries.size()+" top level entries took "+(t1-t0)+"ms");

				for (NotesViewEntryData currEntry : entries) {
					Assert.assertTrue("Note id is not null", currEntry.getNoteId()!=0);
					Assert.assertNotNull("Summary data is not null", currEntry.getSummaryData());
					Assert.assertNotNull("Position is not null", currEntry.getPositionStr());
					Assert.assertNotNull("Summary values are not null", currEntry.getColumnValues());
					Assert.assertTrue("Note class is set", currEntry.getNoteClass()!=0);
					Assert.assertNotNull("UNID is not null", currEntry.getUNID());
				}
				return null;
			}
		});
	}

	/**
	 * Opens the fakenames.nsf and fakenames-views.nsf databases locally, which are expected to have the
	 * same template, and open the views "People". When opening the view from fakenames-views.nsf, we
	 * point it to the fakenames.nsf database so that its data come from there. The test makes sure
	 * both views have the same data.
	 */
	@Test
	public void testViewTraversal_readAllEntriesViaViewDb() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				NotesDatabase dbView = getFakeNamesViewsDb();

				EnumSet<ReadMask> returnValues = EnumSet.of(
						ReadMask.NOTEID,
								ReadMask.SUMMARY,
								ReadMask.INDEXPOSITION,
								ReadMask.SUMMARYVALUES,
								ReadMask.NOTECLASS,
								ReadMask.NOTEUNID);
				
				//flush remotely fetched index of view by opening it localy in dbView
				NotesCollection colFromDbViewWithLocalData = dbView.openCollectionByName("People");
				colFromDbViewWithLocalData.update();
				
				List<NotesViewEntryData> entriesFromDbViewLocally = colFromDbViewWithLocalData.getAllEntries("0", 1,
						EnumSet.of(Navigate.NEXT_PEER),
						Integer.MAX_VALUE,
						returnValues, new EntriesAsListCallback(Integer.MAX_VALUE));
				
				Assert.assertTrue("View opened locally in dbViews is empty", entriesFromDbViewLocally.size()==0);
				colFromDbViewWithLocalData.recycle();
				
				//now open it with reference to external db (works like a private view)
				NotesCollection colFromDbView = dbView.openCollectionByNameWithExternalData(dbData, "People");
				colFromDbView.update();
				//TODO find out why we need to to this update/recycle/reopen sequence here; without it, we get 39788 instead of 39995 entries on the first run
				colFromDbView.recycle();
				colFromDbView = dbView.openCollectionByNameWithExternalData(dbData, "People");
				colFromDbView.update();
				
				//and open the same view in the external database
				NotesCollection colFromDbData = dbView.openCollectionByName("People");
				colFromDbData.update();
				
				
				long t0=System.currentTimeMillis();
				List<NotesViewEntryData> entriesFromDbView = colFromDbView.getAllEntries("0", 1,
						EnumSet.of(Navigate.NEXT_PEER),
						Integer.MAX_VALUE,
						returnValues, new EntriesAsListCallback(Integer.MAX_VALUE));
				long t1=System.currentTimeMillis();
				System.out.println("Reading summary data and note ids of "+entriesFromDbView.size()+" top level entries took "+(t1-t0)+"ms");

				List<NotesViewEntryData> entriesFromDbData = colFromDbData.getAllEntries("0", 1,
						EnumSet.of(Navigate.NEXT_PEER),
						Integer.MAX_VALUE,
						returnValues, new EntriesAsListCallback(Integer.MAX_VALUE));

				Assert.assertEquals("Same number of view entries in view db and data db collection",
						entriesFromDbData.size(), entriesFromDbView.size());
				
				for (int i=0; i<entriesFromDbView.size(); i++) {
					NotesViewEntryData currEntryDbView = entriesFromDbView.get(i);
					NotesViewEntryData currEntryDbData = entriesFromDbData.get(i);
					
					Assert.assertEquals("Note ids #"+i+" match",
							currEntryDbView.getNoteId(),
							currEntryDbData.getNoteId());
					
					Assert.assertEquals("Position #"+i+" matches", currEntryDbView.getPositionStr(), currEntryDbData.getPositionStr());
					
					Assert.assertEquals("Summary data #"+i+" matches",
							currEntryDbView.getSummaryData(),
							currEntryDbData.getSummaryData());

					Assert.assertArrayEquals("Summary values data #"+i+" matches",
							currEntryDbView.getColumnValues(),
							currEntryDbData.getColumnValues());
					
					Assert.assertEquals("Note class #"+i+" matches", currEntryDbView.getNoteClass(), currEntryDbData.getNoteClass());
					Assert.assertEquals("UNID #"+i+" matches", currEntryDbView.getUNID(), currEntryDbData.getUNID());
				}

				return null;
			}
		});
	}
	
	/**
	 * Picks random note ids from the People collection and then populates the selectedList of the
	 * collection with those note ids. Next, we traverse only selected entries in the collection
	 * and make sure all picked ids get visited.
	 */
	@Test
	public void testViewTraversal_readSelectedEntries() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				NotesCollection colFromDbData = dbData.openCollectionByName("People");
				colFromDbData.update();

				//read all note ids from the collection
				List<NotesViewEntryData> allEntries = colFromDbData.getAllEntries("0", 1,
						EnumSet.of(Navigate.NEXT), Integer.MAX_VALUE,
						EnumSet.of(ReadMask.NOTEID), new EntriesAsListCallback(Integer.MAX_VALUE));
				
				//pick random note ids
				Set<Integer> pickedNoteIds = new HashSet<Integer>();
				while (pickedNoteIds.size() < 1000) {
					int randomIndex = (int) (Math.random() * allEntries.size());
					NotesViewEntryData randomEntry = allEntries.get(randomIndex);
					int randomNoteId = randomEntry.getNoteId();
					pickedNoteIds.add(randomNoteId);
				}
				
				//populate selected list (only works locally)
				NotesIDTable selectedList = colFromDbData.getSelectedList();
				selectedList.clear();
				
				for (Integer currNoteId : pickedNoteIds) {
					selectedList.addNote(currNoteId.intValue());
				}

				//next, traverse selected entries
				List<NotesViewEntryData> selectedEntries = colFromDbData.getAllEntries("0", 1,
						EnumSet.of(Navigate.NEXT_SELECTED), Integer.MAX_VALUE,
						EnumSet.of(ReadMask.NOTEID), new EntriesAsListCallback(Integer.MAX_VALUE));
				for (NotesViewEntryData currEntry : selectedEntries) {
					Assert.assertTrue("Entry read from view is contained in selected list", pickedNoteIds.contains(currEntry.getNoteId()));
				}
				
				for (NotesViewEntryData currEntry : selectedEntries) {
					pickedNoteIds.remove(currEntry.getNoteId());
				}
				
				Assert.assertTrue("All ids from the selected list can be found in the view", pickedNoteIds.isEmpty());
				return null;
			}
		});
	}

	/**
	 * Picks random note ids from the People collection, populates the selectedList of the
	 * collection with those note ids and inverts the selectedList.
	 * Next, we traverse only unselected entries in the collection
	 * and make sure all picked ids are skipped.
	 */
	@Test
	public void testViewTraversal_readUnselectedEntries() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				NotesCollection colFromDbData = dbData.openCollectionByName("People");
				colFromDbData.update();

				//read all note ids from the collection
				List<NotesViewEntryData> allEntries = colFromDbData.getAllEntries("0", 1,
						EnumSet.of(Navigate.NEXT), Integer.MAX_VALUE,
						EnumSet.of(ReadMask.NOTEID), new EntriesAsListCallback(Integer.MAX_VALUE));
				
				//pick random note ids
				Set<Integer> pickedNoteIds = new HashSet<Integer>();
				while (pickedNoteIds.size() < 1000) {
					int randomIndex = (int) (Math.random() * allEntries.size());
					NotesViewEntryData randomEntry = allEntries.get(randomIndex);
					int randomNoteId = randomEntry.getNoteId();
					pickedNoteIds.add(randomNoteId);
				}
				
				//populate selected list (only works locally)
				NotesIDTable selectedList = colFromDbData.getSelectedList();
				selectedList.clear();
				
				for (Integer currNoteId : pickedNoteIds) {
					selectedList.addNote(currNoteId.intValue());
				}
				selectedList.setInverted(true);
				
				//next, traverse selected entries
				List<NotesViewEntryData> selectedEntries = colFromDbData.getAllEntries("0", 1,
						EnumSet.of(Navigate.NEXT_SELECTED), Integer.MAX_VALUE,
						EnumSet.of(ReadMask.NOTEID), new EntriesAsListCallback(Integer.MAX_VALUE));
				for (NotesViewEntryData currEntry : selectedEntries) {
					Assert.assertTrue("Entry read from view is contained in selected list", !pickedNoteIds.contains(currEntry.getNoteId()));
				}
				
				return null;
			}
		});
	}
	
	@Test
	public void testViewTraversal_readAllDescendantEntriesWithMaxLevel() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				NotesCollection colFromDbData = dbData.openCollectionByName("CompaniesHierarchical");
				colFromDbData.update();

				int maxLevelFound;
				
				//read all descendants of position 1 from the collection
				List<NotesViewEntryData> allDescendantsEntries = colFromDbData.getAllEntries("1", 1,
						EnumSet.of(Navigate.ALL_DESCENDANTS),
						Integer.MAX_VALUE,
						EnumSet.of(ReadMask.INDEXPOSITION, ReadMask.NOTEID), new EntriesAsListCallback(Integer.MAX_VALUE));

				//make sure we have higher levels than 1 in the result
				maxLevelFound = 0;
				for (NotesViewEntryData currEntry : allDescendantsEntries) {
					Assert.assertEquals("Entry is descendant of 1", 1, currEntry.getPosition()[0]);
					
					int currLevel = currEntry.getLevel();
					if (currLevel > maxLevelFound) {
						maxLevelFound = currLevel;
					}
				}
				Assert.assertTrue("Entries with level 2 or higher in the result", maxLevelFound>=2);

				//read all descendants of position 1 with minlevel=0 and maxlevel=1 from the collection
				List<NotesViewEntryData> allDescendantsEntriesMaxLevel1 = colFromDbData.getAllEntries("1|0-1", 1,
						EnumSet.of(Navigate.ALL_DESCENDANTS, Navigate.MINLEVEL, Navigate.MAXLEVEL),
						Integer.MAX_VALUE,
						EnumSet.of(ReadMask.INDEXPOSITION, ReadMask.NOTEID), new EntriesAsListCallback(Integer.MAX_VALUE));

				maxLevelFound = 0;
				for (NotesViewEntryData currEntry : allDescendantsEntriesMaxLevel1) {
					
					int currLevel = currEntry.getLevel();
					if (currLevel > maxLevelFound) {
						maxLevelFound = currLevel;
					}
				}
				Assert.assertTrue("Entries with max level 1 in the result", maxLevelFound<=1);

				return null;
			}
		});
	}
	
	@Test
	public void testViewTraversal_umlauts() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				Database dbLegacyAPI = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());
				View view = dbLegacyAPI.getView("PeopleFlatMultiColumnSort");
				
				NotesCollection colFromDbData = dbData.openCollectionByName("PeopleFlatMultiColumnSort");
				colFromDbData.update();

				CollationInfo collationInfo = colFromDbData.hashCollations(view);
				short collationByLastName = collationInfo.findCollation("Lastname", Direction.Ascending);
				if (collationByLastName==-1) {
					throw new RuntimeException("Could not resort view by lastname");
				}
				colFromDbData.setCollation(collationByLastName);
				List<NotesViewEntryData> umlautEntryAsList = colFromDbData.getAllEntriesByKey(EnumSet.of(Find.PARTIAL),
						EnumSet.of(ReadMask.SUMMARY, ReadMask.NOTEID), new EntriesAsListCallback(1), "Umlaut");
				
				Assert.assertFalse("There is a person document with lastname starting with 'Umlaut'", umlautEntryAsList.isEmpty());
				
				NotesViewEntryData umlautEntry = umlautEntryAsList.get(0);
				Map<String,Object> summaryData = umlautEntry.getSummaryData();
				
				String lastNameFromView = (String) summaryData.get("lastname");
				
				Document docPerson = dbLegacyAPI.getDocumentByID(umlautEntry.getNoteIdAsHex());
				String lastNameFromDoc = docPerson.getItemValueString("Lastname");
				docPerson.recycle();
				
				Assert.assertEquals("LMBCS decoding to Java String works for Umlauts", lastNameFromDoc, lastNameFromView);
				
				return null;
			}
		});
	}
	
	@Test
	public void testViewTraversal_readDatatypes() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				NotesCollection colFromDbData = dbData.openCollectionByName("PeopleFlatMultiColumnSort");
				colFromDbData.update();

				List<NotesViewEntryData> entries = colFromDbData.getAllEntries("0", 1,
						EnumSet.of(Navigate.NEXT_NONCATEGORY),
						1000, 
						EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARY), new EntriesAsListCallback(Integer.MAX_VALUE));
				
				for (NotesViewEntryData currEntry : entries) {
					Map<String,Object> currData = currEntry.getSummaryData();
					
					//col_namevariants - stringlist
					Object nameVariants = currData.get("col_namevariants");
					Assert.assertTrue("Numberlist column contains correct datatype", nameVariants!=null && (nameVariants instanceof List && isListOfType((List<?>)nameVariants, String.class)));
					
					//col_namevariantlengths - numberlist
					Object nameVariantLengths = currData.get("col_namevariantlengths");
					Assert.assertTrue("Numberlist column contains correct datatype", nameVariantLengths!=null && (nameVariantLengths instanceof List && isListOfType((List<?>)nameVariantLengths, Double.class)));
					
					//col_createdmodified - datelist
					Object createdModified = currData.get("col_createdmodified");
					Assert.assertTrue("Datelist column contains correct datatype", createdModified!=null && (createdModified instanceof List && isListOfType((List<?>)createdModified, Calendar.class)));
					
					//lastname - string
					Object lastName = currData.get("lastname");
					Assert.assertTrue("String column contains correct datatype", lastName!=null && lastName instanceof String);
					
					//HTTPPasswordChangeDate - datetime
					Object httpPwdChangeDate = currData.get("HTTPPasswordChangeDate");
					Assert.assertTrue("Datetime column contains correct datatype", httpPwdChangeDate!=null && httpPwdChangeDate instanceof Calendar);
					
					//col_internetaddresslength - number
					Object internetAddressLength = currData.get("col_internetaddresslength");
					Assert.assertTrue("Number column contains correct datatype", internetAddressLength!=null && internetAddressLength instanceof Double);
					
					//col_createdmodifiedrange - daterange
					Object createModifiedRange = currData.get("col_createdmodifiedrange");
					Assert.assertTrue("Daterange column contains correct datatype", createModifiedRange!=null && (createModifiedRange instanceof List && isListOfType((List<?>)createModifiedRange, Calendar[].class)));
					
				}
				
				return null;
			}

		});
	}

	private boolean isListOfType(List<?> list, Class<?> classType) {
		if (list.size()==0) {
			return true;
		}
		for (int i=0; i<list.size(); i++) {
			Object currObj = list.get(i);
			if (!classType.isInstance(currObj)) {
				return false;
			}
		}
		return true;
	}

}