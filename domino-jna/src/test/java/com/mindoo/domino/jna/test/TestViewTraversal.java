package com.mindoo.domino.jna.test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesCollection.Direction;
import com.mindoo.domino.jna.NotesCollection.EntriesAsListCallback;
import com.mindoo.domino.jna.NotesCollection.ViewLookupCallback;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.Find;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.constants.UpdateCollectionFilters;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Session;

/**
 * Test cases that read data from views
 * 
 * @author Karsten Lehmann
 */
public class TestViewTraversal extends BaseJNATestClass {

	@Test
	public void testViewTraversal_numericInequalityLookup() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				
				NotesCollection colFromDbData = dbData.openCollectionByName("PeopleFlatMultiColumnSort");
				colFromDbData.update();

				colFromDbData.resortView("col_internetaddresslength", Direction.Ascending);
				
				double minVal = 10;
				
				List<NotesViewEntryData> entriesGreaterOrEqualMinVal = colFromDbData.getAllEntriesByKey(EnumSet.of(Find.GREATER_THAN, Find.EQUAL),
						EnumSet.of(ReadMask.SUMMARY, ReadMask.NOTEID), new EntriesAsListCallback(Integer.MAX_VALUE), Double.valueOf(minVal));
				
				Assert.assertFalse("There should be entries with lengths column values matching the search key", entriesGreaterOrEqualMinVal.isEmpty());
				
				Set<Integer> noteIdsInRange = new HashSet<Integer>();
				
				for (NotesViewEntryData currEntry : entriesGreaterOrEqualMinVal) {
					noteIdsInRange.add(currEntry.getNoteId());
					
					Number currLength = (Number) currEntry.get("col_internetaddresslength");
					
					Assert.assertTrue("Length "+currLength.doubleValue()+" of entry with note id "+currEntry.getNoteId()+" should be greater than "+minVal, currLength.doubleValue()>=minVal);
				}
				
				NotesIDTable selectedList = colFromDbData.getSelectedList();
				selectedList.clear();
				selectedList.addNotes(noteIdsInRange);
				selectedList.setInverted(true);
				
				//for remote databases, re-send modified SelectedList
				colFromDbData.updateFilters(EnumSet.of(UpdateCollectionFilters.FILTER_SELECTED));
				
				//now do an inverted lookup to find every entry not in range
				List<NotesViewEntryData> entriesNotGreaterOrEqualMinVal = colFromDbData.getAllEntries("0", 1,
						EnumSet.of(Navigate.NEXT_SELECTED), Integer.MAX_VALUE, EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARY), new EntriesAsListCallback(Integer.MAX_VALUE));
				
				Assert.assertFalse("There should be entries with lengths column values not matching the search key", entriesNotGreaterOrEqualMinVal.isEmpty());

				for (NotesViewEntryData currEntry : entriesNotGreaterOrEqualMinVal) {
					Assert.assertFalse("Inverted flag in selected list works", noteIdsInRange.contains(currEntry.getNoteId()));
					
					Number currLength = (Number) currEntry.get("col_internetaddresslength");
					
					Assert.assertFalse("Length "+currLength.doubleValue()+" of entry with note id "+currEntry.getNoteId()+" should not be greater than "+minVal, currLength.doubleValue()>=minVal);
				}
				
				return null;
			}
		});
	
	}

	@Test
	public void testViewTraversal_stringInequalityLookup() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				
				NotesCollection colFromDbData = dbData.openCollectionByName("PeopleFlatMultiColumnSort");
				colFromDbData.update();

				colFromDbData.resortView("lastname", Direction.Ascending);
				
				String minVal = "D";
				
				List<NotesViewEntryData> entriesGreaterOrEqualMinVal = colFromDbData.getAllEntriesByKey(EnumSet.of(Find.GREATER_THAN, Find.EQUAL),
						EnumSet.of(ReadMask.SUMMARY, ReadMask.NOTEID), new EntriesAsListCallback(Integer.MAX_VALUE),
						minVal);
				
				Assert.assertFalse("There should be entries with lastname column values matching the search key", entriesGreaterOrEqualMinVal.isEmpty());
				
				Set<Integer> noteIdsInRange = new HashSet<Integer>();
				
				for (NotesViewEntryData currEntry : entriesGreaterOrEqualMinVal) {
					noteIdsInRange.add(currEntry.getNoteId());
					
					String currLastname = (String) currEntry.get("lastname");
					
					Assert.assertTrue("Lastname "+currLastname+" of entry with note id "+currEntry.getNoteId()+" should be greater or equal "+minVal, currLastname.compareToIgnoreCase(minVal) >=0);
				}
				
				NotesIDTable selectedList = colFromDbData.getSelectedList();
				selectedList.clear();
				selectedList.addNotes(noteIdsInRange);
				selectedList.setInverted(true);
				
				//for remote databases, re-send modified SelectedList
				colFromDbData.updateFilters(EnumSet.of(UpdateCollectionFilters.FILTER_SELECTED));

				//now do an inverted lookup to find every entry not in range
				List<NotesViewEntryData> entriesNotGreaterOrEqualMinVal = colFromDbData.getAllEntries("0", 1,
						EnumSet.of(Navigate.NEXT_SELECTED), Integer.MAX_VALUE, EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARY), new EntriesAsListCallback(Integer.MAX_VALUE));
				
				Assert.assertFalse("There should be entries with lastname column values not matching the search key", entriesNotGreaterOrEqualMinVal.isEmpty());

				for (NotesViewEntryData currEntry : entriesNotGreaterOrEqualMinVal) {
					Assert.assertFalse("Inverted flag in selected list works", noteIdsInRange.contains(currEntry.getNoteId()));
					
					String currLastname = (String) currEntry.get("lastname");
					
					Assert.assertTrue("Lastname "+currLastname+" of entry with note id "+currEntry.getNoteId()+" should not be greater or equal "+minVal, currLastname.compareToIgnoreCase(minVal)<0);
				}
				
				return null;
			}
		});
	
	}

	@Test
	public void testViewTraversal_readAllEntryDataInDataDb() {
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
//								ReadMask.SUMMARYVALUES,
								ReadMask.NOTECLASS,
								ReadMask.NOTEUNID
								), new EntriesAsListCallback(Integer.MAX_VALUE));
				long t1=System.currentTimeMillis();
				System.out.println("Reading data of "+entries.size()+" top level entries took "+(t1-t0)+"ms");

				for (NotesViewEntryData currEntry : entries) {
					Assert.assertTrue("Note id is not null", currEntry.getNoteId()!=0);
					Assert.assertNotNull("Position is not null", currEntry.getPositionStr());
					Assert.assertNotNull("Column values are not null", currEntry.get("$17"));
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
				NotesCollection colFromDbData = dbData.openCollectionByName("People");
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

				Assert.assertTrue("View opened with remote database is not empty", entriesFromDbView.size()!=0);
				Assert.assertTrue("View opened in data db is not empty", entriesFromDbData.size()!=0);

				Assert.assertEquals("Same number of view entries in view db and data db collection",
						entriesFromDbData.size(), entriesFromDbView.size());
				
				for (int i=0; i<entriesFromDbView.size(); i++) {
					NotesViewEntryData currEntryDbView = entriesFromDbView.get(i);
					NotesViewEntryData currEntryDbData = entriesFromDbData.get(i);
					
					Assert.assertEquals("Note ids #"+i+" match",
							currEntryDbView.getNoteId(),
							currEntryDbData.getNoteId());
					
					Assert.assertEquals("Position #"+i+" matches", currEntryDbView.getPositionStr(), currEntryDbData.getPositionStr());
					
					Assert.assertEquals("Column data #"+i+" matches",
							currEntryDbView.getColumnDataAsMap(),
							currEntryDbData.getColumnDataAsMap());
					
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
				
				//open People view
				NotesCollection colFromDbData = dbData.openCollectionByName("People");
				colFromDbData.update();

				//read all note ids from the collection
				LinkedHashSet<Integer> allIds = colFromDbData.getAllIds(Navigate.NEXT_NONCATEGORY);
				Integer[] allIdsArr = allIds.toArray(new Integer[allIds.size()]);
				
				//pick random note ids
				Set<Integer> pickedNoteIds = new HashSet<Integer>();
				while (pickedNoteIds.size() < 1000) {
					int randomIndex = (int) (Math.random() * allIdsArr.length);
					int randomNoteId = allIdsArr[randomIndex];
					pickedNoteIds.add(randomNoteId);
				}
				
				//populate selected list (only works locally)
				NotesIDTable selectedList = colFromDbData.getSelectedList();
				selectedList.clear();
				selectedList.addNotes(pickedNoteIds);

				//for remote databases, re-send modified SelectedList
				colFromDbData.updateFilters(EnumSet.of(UpdateCollectionFilters.FILTER_SELECTED));

				//next, traverse selected entries only
				List<NotesViewEntryData> selectedEntries = colFromDbData.getAllEntries("0", 1,
						EnumSet.of(Navigate.NEXT_SELECTED), Integer.MAX_VALUE,
						EnumSet.of(ReadMask.NOTEID), new EntriesAsListCallback(Integer.MAX_VALUE));
				
				//check if we really read entries from our selection
				for (NotesViewEntryData currEntry : selectedEntries) {
					Assert.assertTrue("Entry read from view is contained in selected list", pickedNoteIds.contains(currEntry.getNoteId()));
				}
				
				//now remove all read ids from pickedNoteIds and make sure that we did not miss anything
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
				
				//for remote databases, re-send modified SelectedList
				colFromDbData.updateFilters(EnumSet.of(UpdateCollectionFilters.FILTER_SELECTED));

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
				
				NotesCollection colFromDbData = dbData.openCollectionByName("PeopleFlatMultiColumnSort");
				colFromDbData.update();

				colFromDbData.resortView("Lastname", Direction.Ascending);
				
				List<NotesViewEntryData> umlautEntryAsList = colFromDbData.getAllEntriesByKey(EnumSet.of(Find.PARTIAL),
						EnumSet.of(ReadMask.SUMMARY, ReadMask.NOTEID), new EntriesAsListCallback(1), "Umlaut");
				
				Assert.assertFalse("There is a person document with lastname starting with 'Umlaut'", umlautEntryAsList.isEmpty());
				
				NotesViewEntryData umlautEntry = umlautEntryAsList.get(0);
				
				String lastNameFromView = (String) umlautEntry.get("lastname");
				
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
						EnumSet.of(ReadMask.NOTEID, ReadMask.NOTEUNID, ReadMask.SUMMARY), new EntriesAsListCallback(Integer.MAX_VALUE));
				
				for (NotesViewEntryData currEntry : entries) {
					
					//col_namevariants - stringlist
					Object nameVariants = currEntry.get("col_namevariants");
					Assert.assertTrue("Numberlist column contains correct datatype", nameVariants!=null && (nameVariants instanceof List && isListOfType((List<?>)nameVariants, String.class)));
					
					//col_namevariantlengths - numberlist
					Object nameVariantLengths = currEntry.get("col_namevariantlengths");
					Assert.assertTrue("Numberlist column contains correct datatype", nameVariantLengths!=null && (nameVariantLengths instanceof List && isListOfType((List<?>)nameVariantLengths, Double.class)));
					
					//col_createdmodified - datelist
					Object createdModified = currEntry.get("col_createdmodified");
					Assert.assertTrue("Datelist column contains correct datatype", createdModified!=null && (createdModified instanceof List && isListOfType((List<?>)createdModified, Calendar.class)));
					
					//lastname - string
					Object lastName = currEntry.get("lastname");
					Assert.assertTrue("String column contains correct datatype", lastName!=null && lastName instanceof String);
					
					//HTTPPasswordChangeDate - datetime
					Object httpPwdChangeDate = currEntry.get("HTTPPasswordChangeDate");
					Assert.assertTrue("Datetime column contains correct datatype", httpPwdChangeDate!=null && httpPwdChangeDate instanceof Calendar);
					
					//col_internetaddresslength - number
					Object internetAddressLength = currEntry.get("col_internetaddresslength");
					Assert.assertTrue("Number column contains correct datatype", internetAddressLength!=null && internetAddressLength instanceof Double);
					
					//col_createdmodifiedrange - daterange
					Object createModifiedRange = currEntry.get("col_createdmodifiedrange");
					
					Assert.assertTrue("Daterange column of note unid "+currEntry.getUNID()+" contains correct datatype", createModifiedRange!=null && (createModifiedRange instanceof List && isListOfType((List<?>)createModifiedRange, Calendar[].class)));
				}
				
				return null;
			}

		});
	}
	
	/**
	 * The test compares reading view data with limited and unlimited preload buffer; boths
	 * are expected to return the same result, which shows that continuing reading view
	 * data after the first preload buffer has been processed restarts reading at the right
	 * position
	 */
	@Test
	public void testViewTraversal_allEntriesLimitedPreloadBuffer() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting limited preload buffer test");
				
				NotesDatabase dbData = getFakeNamesDb();
				
				final int nrOfNotesToRead = 20; // no more than we have in the view
				//only read 10 entries per readEntries call
				int preloadBufferEntries = 10;
				
				//grab some note ids from the People view
				NotesCollection col = dbData.openCollectionByName("People");
				int[] someNoteIdsReadWithLimitedPreloadBuffer = col.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT_NONCATEGORY), preloadBufferEntries, EnumSet.of(ReadMask.NOTEID), new ViewLookupCallback<int[]>() {
					
					int m_idx = 0;
					
					@Override
					public int[] startingLookup() {
						return new int[nrOfNotesToRead];
					}

					@Override
					public com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action entryRead(int[] result,
							NotesViewEntryData entryData) {
						if (m_idx < result.length) {
							result[m_idx] = entryData.getNoteId();
							m_idx++;
							return Action.Continue;
						}
						else {
							return Action.Stop;
						}
					}

					@Override
					public int[] lookupDone(int[] result) {
						return result;
					}
				});
				
				int cntNonNullIds = 0;
				for (int currId : someNoteIdsReadWithLimitedPreloadBuffer) {
					if (currId!=0) {
						cntNonNullIds++;
					}
				}
				
				Assert.assertEquals("All ids have been read", someNoteIdsReadWithLimitedPreloadBuffer.length, cntNonNullIds);

				//now read with unlimited buffer, which effectively reads 64K of data, but only
				//takes the first "nrOfNotesToRead" entries of them
				int[] someNoteIdsReadWithUnlimitedPreloadBuffer = col.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT_NONCATEGORY), Integer.MAX_VALUE, EnumSet.of(ReadMask.NOTEID), new ViewLookupCallback<int[]>() {
					
					int m_idx = 0;
					
					@Override
					public int[] startingLookup() {
						return new int[nrOfNotesToRead];
					}

					@Override
					public com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action entryRead(int[] result,
							NotesViewEntryData entryData) {
						if (m_idx < result.length) {
							result[m_idx] = entryData.getNoteId();
							m_idx++;
							return Action.Continue;
						}
						else {
							return Action.Stop;
						}
					}

					@Override
					public int[] lookupDone(int[] result) {
						return result;
					}
				});
				
				Assert.assertTrue("Reading with limited and unlimited preload buffer produces the same result", Arrays.equals(someNoteIdsReadWithLimitedPreloadBuffer, someNoteIdsReadWithUnlimitedPreloadBuffer));
				
				System.out.println("Done with limited preload buffer test");
				return null;
			}
		});
	}

	@Test
	public void testViewTraversal_idScan() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				//compare the result of two scan functions, one using NIFGetIDTableExtended and
				//the other one using NIFReadEntries
				NotesDatabase dbData = getFakeNamesDb();
				NotesCollection colFromDbData = dbData.openCollectionByName("CompaniesHierarchical");
				colFromDbData.update();

				long t0=System.currentTimeMillis();
				NotesIDTable idTableUnsorted = new NotesIDTable();
				colFromDbData.getAllIds(Navigate.NEXT_NONCATEGORY, false, idTableUnsorted);
				long t1=System.currentTimeMillis();
				System.out.println("Read "+idTableUnsorted.getCount()+" entries unsorted in "+(t1-t0)+"ms");
				
				t0=System.currentTimeMillis();
				LinkedHashSet<Integer> idsSorted = colFromDbData.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT_NONCATEGORY), Integer.MAX_VALUE, EnumSet.of(ReadMask.NOTEID), new ViewLookupCallback<LinkedHashSet<Integer>>() {

					@Override
					public LinkedHashSet<Integer> startingLookup() {
						return new LinkedHashSet<Integer>();
					}

					@Override
					public com.mindoo.domino.jna.NotesCollection.ViewLookupCallback.Action entryRead(
							LinkedHashSet<Integer> ctx, NotesViewEntryData entryData) {
						
						ctx.add(entryData.getNoteId());
						return Action.Continue;
					}
					
					@Override
					public LinkedHashSet<Integer> lookupDone(LinkedHashSet<Integer> result) {
						return result;
					}
				});
				t1=System.currentTimeMillis();
				System.out.println("Read "+idsSorted.size()+" entries sorted in "+(t1-t0)+"ms");
				
				//compare both lists
				Assert.assertEquals("Both id lists have the same size", idTableUnsorted.getCount(), idsSorted.size());
				for (Integer currNoteId : idsSorted) {
					Assert.assertTrue("ID table contains note id "+currNoteId, idTableUnsorted.contains(currNoteId));
				}
				for (Integer currNoteId : idsSorted) {
					idTableUnsorted.removeNote(currNoteId);
				}
				Assert.assertTrue("ID table is empty after removing all entries from sorted list", idTableUnsorted.isEmpty());
				
				//test isNoteInView function
				for (Integer currNoteId : idsSorted) {
					Assert.assertTrue("Note with note ID "+currNoteId+" is in view", colFromDbData.isNoteInView(currNoteId));
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