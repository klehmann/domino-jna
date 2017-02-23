package com.mindoo.domino.jna.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.CollectionDataCache;
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesCollectionPosition;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.NotesIDTable.IEnumerateCallback;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.NotesViewLookupResultData;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.OpenCollection;
import com.mindoo.domino.jna.constants.ReadMask;

import lotus.domino.Session;

/**
 * Test cases that read data from views. This class tests and demonstrates some advanced view
 * reading technologies.
 * 
 * @author Karsten Lehmann
 */
public class TestExtendedViewTraversal extends BaseJNATestClass {

	/**
	 * Tests the method to read a subset of the view, all descendants of a category
	 */
//	@Test
	public void testExtViewTraversal_getCategoryDescendants() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				
				NotesCollection colFromDbData = dbData.openCollectionByName("Companies");
				colFromDbData.update();

				System.out.println("Reading descendants of category Abbas");
				
				long t0=System.currentTimeMillis();
				List<NotesViewEntryData> entries = colFromDbData.getAllEntriesInCategory("Abbas", 1,
						EnumSet.of(Navigate.NEXT),
						null, null, Integer.MAX_VALUE, EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARY), new NotesCollection.EntriesAsListCallback(Integer.MAX_VALUE));
				
				Assert.assertTrue("Category has any descendants", entries.size() > 0);
				
				for (int i=0; i<entries.size(); i++) {
					NotesViewEntryData currEntry = entries.get(i);
					System.out.println(currEntry);
				}
				long t1=System.currentTimeMillis();
				System.out.println("It took "+(t1-t0)+"ms to read "+entries.size()+" category descendants");
				
				return null;
			}
		});
	}
	
	/**
	 * Method to test an optimized lookup function that reads a single view column
	 */
//	@Test
	public void testExtViewTraversal_readSingleColumn() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				
				NotesCollection colFromDbData = dbData.openCollectionByName("Companies");
				colFromDbData.update();

				System.out.println("Reading lastname column values");
				
				long t0=System.currentTimeMillis();
				String colName = "$22";
				Set<String> lastNames = colFromDbData.getColumnValues(colName, Locale.getDefault());
				
				Assert.assertTrue("Method has read lastnames", lastNames.size() > 0);
				
				long t1=System.currentTimeMillis();
				System.out.println("It took "+(t1-t0)+"ms to read "+lastNames.size()+" unique lastnames");
				
				return null;
			}
		});
	}
	
	/**
	 * Tests the differential view read feature of NIF using low level APIs
	 */
//	@Test
	public void testExtViewTraversal_incrementalReadLowLevelAPI() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				
				
				NotesCollection colFromDbData = dbData.openCollectionByName("PeopleFlatMultiColumnSortSingleValue");
				colFromDbData.update();

				NotesIDTable diffIDTable = new NotesIDTable();
				NotesTimeDate diffTime = null;

				final int numEntriesToRead = 5;
				
				NotesCollectionPosition startPos;
				{
					startPos = new NotesCollectionPosition("0");
					
					NotesViewLookupResultData lkResult = colFromDbData.readEntriesExt(startPos,
							EnumSet.of(Navigate.NEXT), 1, EnumSet.of(Navigate.NEXT),
							numEntriesToRead, EnumSet.of(ReadMask.SUMMARYVALUES, ReadMask.NOTEID),
							null, null, null);
					
					List<NotesViewEntryData> entries = lkResult.getEntries();
					diffTime = lkResult.getReturnedDiffTime();
					
					for (int i=0; i<entries.size(); i++) {
						NotesViewEntryData currEntry = entries.get(i);
						Map<String,Object> dataAsMap = currEntry.getColumnDataAsMap();
						System.out.println("1st run #"+Integer.toString(i+1)+" - id: "+currEntry.getNoteId()+" - "+dataAsMap);
						diffIDTable.addNote(currEntry.getNoteId());
						Assert.assertEquals("Entry read on 1st run is expected to have column values: "+currEntry, true, currEntry.hasAnyColumnValues());
					}
					
					colFromDbData.getAllIds(Navigate.NEXT, false, diffIDTable);
				}
				
				System.out.println("******Second run for incremental view read******");
				
				{
					//second run: we use DiffTime and diffIDTable to tell NIF which data we already know
					//from the view
					NotesTimeDate lastDiffTime = diffTime;
					startPos = new NotesCollectionPosition("0");
					
					NotesViewLookupResultData lkResult = colFromDbData.readEntriesExt(startPos,
							EnumSet.of(Navigate.NEXT), 1, EnumSet.of(Navigate.NEXT),
							numEntriesToRead, EnumSet.of(ReadMask.SUMMARYVALUES, ReadMask.NOTEID),
							lastDiffTime, diffIDTable, null);
					
					List<NotesViewEntryData> entries = lkResult.getEntries();
					diffTime = lkResult.getReturnedDiffTime();
					
					for (int i=0; i<entries.size(); i++) {
						NotesViewEntryData currEntry = entries.get(i);
						Map<String,Object> dataAsMap = currEntry.getColumnDataAsMap();
						System.out.println("2nd run #"+Integer.toString(i+1)+" - id: "+currEntry.getNoteId()+" - "+dataAsMap);
						Assert.assertEquals("Entry read on 2nd run is expected to have no column values: "+currEntry, false, currEntry.hasAnyColumnValues());
						diffIDTable.addNote(currEntry.getNoteId());
					}
				}
				
				return null;
			}
		});
	}
	
	/**
	 * Tests the differential view read feature of NIF using high level APIs.<br>
	 * By returning a {@link CollectionDataCache} object in
	 * method {@link NotesCollection.ViewLookupCallback#createDataCache()}, the view lookups make
	 * use of the "differential view read feature" of NIF, where we tell the NIF engine to only
	 * return view data for rows that have been modified since the last collection index update and
	 * for rows that do not exist in our cache. For other rows, we simple get back the note id and
	 * can add the missing values from our collection data cache.<br>
	 * Differential view reading speeds up the read process, because it optimizes the usage of the
	 * 64K summary buffer returned by NIFReadEntriesExt.
	 */
//	@Test
	public void testExtViewTraversal_incrementalReadHighLevelAPI() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				
				NotesCollection colFromDbData = dbData.openCollectionByName("PeopleFlatMultiColumnSortSingleValue",
						EnumSet.of(OpenCollection.NOUPDATE));
				colFromDbData.update();

				final int numEntriesToRead = 5;
				
				int maxCacheSize = Integer.MAX_VALUE;
				//create our (shared) cache instance with LRU strategy
				final CollectionDataCache collectionDataCache = new CollectionDataCache(maxCacheSize);
				
				NotesCollection.EntriesAsListCallback readCallback = new NotesCollection.EntriesAsListCallback(numEntriesToRead) {
					@Override
					public CollectionDataCache createDataCache() {
						return collectionDataCache;
					}
				};
				
				{
					List<NotesViewEntryData> entries = colFromDbData.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT), numEntriesToRead,
							EnumSet.of(ReadMask.SUMMARYVALUES, ReadMask.NOTEID), readCallback);
					
					for (int i=0; i<entries.size(); i++) {
						NotesViewEntryData currEntry = entries.get(i);
						Map<String,Object> dataAsMap = currEntry.getColumnDataAsMap();
						System.out.println("1st run #"+Integer.toString(i+1)+" - id: "+currEntry.getNoteId()+" - "+dataAsMap);
						Assert.assertEquals("Entry read on 1st run is expected to have column values: "+currEntry, true, currEntry.hasAnyColumnValues());
					}
				}
				Assert.assertEquals("Cache usage counter is 0 for first run", 0, collectionDataCache.getCacheUsageStats());
				Assert.assertEquals("Cache has been filled with collection data", Math.min(numEntriesToRead, maxCacheSize),
						collectionDataCache.size());
				
				System.out.println("******Second run for incremental view read******");
				
				{
					//second run: this run should use the data stored in the cache if view has not changed
					List<NotesViewEntryData> entries = colFromDbData.getAllEntries("0", 1, EnumSet.of(Navigate.NEXT), numEntriesToRead,
							EnumSet.of(ReadMask.SUMMARYVALUES, ReadMask.NOTEID), readCallback);
					
					
					for (int i=0; i<entries.size(); i++) {
						NotesViewEntryData currEntry = entries.get(i);
						Map<String,Object> dataAsMap = currEntry.getColumnDataAsMap();
						System.out.println("2nd run #"+Integer.toString(i+1)+" - id: "+currEntry.getNoteId()+" - "+dataAsMap);
						Assert.assertEquals("Entry read on 2nd run is expected to have column values: "+currEntry, true, currEntry.hasAnyColumnValues());
					}
				}
				
				Assert.assertTrue("Cache has been used on second lookup", collectionDataCache.getCacheUsageStats() > 0);
				
				return null;
			}
		});
	}
	
	@Test
	public void testViewTraversal_selectViaFormula() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
//				NotesDatabase db = getFakeNamesDb();
				NotesDatabase db = new NotesDatabase(getSession(), "", "fakenames.nsf", (String)null);

				NotesCollection col = db.openCollectionByName("People");
				String formula = "@Begins(LastName;\"L\")";
//				col.select(formula, true);
				

				NotesIDTable idTable = new NotesIDTable();

				
				col.getAllIds(Navigate.NEXT_NONCATEGORY, false, idTable);
				
				long t0=System.currentTimeMillis();
				int[] idsScan = idTable.toArray();
				long t1=System.currentTimeMillis();
				System.out.println("IDScan: "+(t1-t0)+"ms, "+idsScan.length+" entries");
				
				t0=System.currentTimeMillis();
				final List<Integer> idList = new ArrayList<Integer>();
				idTable.enumerate(new IEnumerateCallback() {

					@Override
					public Action noteVisited(int noteId) {
						idList.add(noteId);
						return Action.Continue;
					}
				});
				t1=System.currentTimeMillis();
				System.out.println("IDEnumerate: "+(t1-t0)+"ms, "+idList.size()+" entries");

				t0=System.currentTimeMillis();
				idList.clear();
				idTable.enumerateBackwards(new IEnumerateCallback() {

					@Override
					public Action noteVisited(int noteId) {
						idList.add(noteId);
						return Action.Continue;
					}
				});
				t1=System.currentTimeMillis();
				System.out.println("IDScanBack: "+(t1-t0)+"ms, "+idList.size()+" entries");

				int[] unfilteredIds = idTable.toArray();
				System.out.println("unfilteredIds.length = "+unfilteredIds.length);
				
				t0=System.currentTimeMillis();
				NotesIDTable filteredTable = idTable.filter(db, formula);
				t1=System.currentTimeMillis();
				System.out.println("Filtering "+idTable.getCount()+" down to "+filteredTable.getCount()+" took "+(t1-t0)+"ms");
				
				int[] filteredIds = filteredTable.toArray();
				System.out.println("filteredIds.length = "+filteredIds.length);
				System.out.println("unfilteredIds: "+Arrays.toString(filteredIds));
				
//				col.select("SELECT Firstname=\"Ellis\"", true);
//				
//				long t0=System.currentTimeMillis();
//				List<NotesViewEntryData> entries = col.getAllEntries("0", 1,
//						EnumSet.of(Navigate.NEXT_SELECTED),
//						Integer.MAX_VALUE,
//						EnumSet.of(
//								ReadMask.NOTEID,
//								ReadMask.SUMMARYVALUES
//								), new EntriesAsListCallback(Integer.MAX_VALUE));
//				long t1=System.currentTimeMillis();
//				System.out.println("Reading data of "+entries.size()+" top level entries took "+(t1-t0)+"ms");
//
//				for (NotesViewEntryData currEntry : entries) {
//					System.out.println(currEntry.getNoteId()+"\t"+currEntry.getColumnDataAsMap());
//				}
				return null;
			}
		});
	}
}