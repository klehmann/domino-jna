package com.mindoo.domino.jna.test;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.NotesViewLookupResultData;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.OpenCollection;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.structs.NotesCollectionPosition;
import com.mindoo.domino.jna.structs.NotesTimeDate;

import lotus.domino.Session;

/**
 * Test cases that read data from views
 * 
 * @author Karsten Lehmann
 */
public class TestExtendedViewTraversal extends BaseJNATestClass {

	@Test
	public void testExtViewTraversal_getCategoryDescendants() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				
				NotesCollection colFromDbData = dbData.openCollectionByName("Companies");
				colFromDbData.update();

				long t0=System.currentTimeMillis();
				List<NotesViewEntryData> entries = colFromDbData.getAllEntriesInCategory("Abbas", 1,
						EnumSet.of(Navigate.NEXT_NONCATEGORY),
						null, null, Integer.MAX_VALUE, EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARY), new NotesCollection.EntriesAsListCallback(Integer.MAX_VALUE));
				for (int i=0; i<entries.size(); i++) {
					NotesViewEntryData currEntry = entries.get(i);
					System.out.println(currEntry);
				}
				long t1=System.currentTimeMillis();
				System.out.println("It took "+(t1-t0)+"ms to read "+entries.size()+" unique lastnames");
				
				return null;
			}
		});
	}
	
	@Test
	public void testExtViewTraversal_readColumn() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				
				NotesCollection colFromDbData = dbData.openCollectionByName("Companies");
				colFromDbData.update();

				long t0=System.currentTimeMillis();
				Set<String> lastNames = colFromDbData.getColumnValues("$22", Locale.getDefault());
				long t1=System.currentTimeMillis();
				System.out.println("It took "+(t1-t0)+"ms to read "+lastNames.size()+" unique lastnames");
				
				return null;
			}
		});
	}
	
	@Test
	public void testExtViewTraversal_incrementalRead() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
//				NotesDatabase dbData = new NotesDatabase(getSession(), "", "fakenames.nsf", (String) null);
				NotesDatabase dbData = getFakeNamesDb();
//				NotesDatabase dbData = new NotesDatabase(getSession(), "", "ibm/fakenames.nsf", (String) null);
//				NotesDatabase dbData = new NotesDatabase(getSession(), "", "ibm/fakenames_notworking.nsf", (String) null);
				
				
				NotesCollection colFromDbData = dbData.openCollectionByName("PeopleFlatMultiColumnSortSingleValue",
						EnumSet.of(OpenCollection.NOUPDATE));
				colFromDbData.update();

				NotesIDTable diffIDTable = new NotesIDTable();
				NotesTimeDate diffTime = null;

				final int numEntriesToRead = 5;
				
				NotesCollectionPosition startPos;
				{
					startPos = NotesCollectionPosition.toPosition("0");
					
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
					startPos = NotesCollectionPosition.toPosition("0");
					
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
}