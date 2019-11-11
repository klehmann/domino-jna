package com.mindoo.domino.jna.test;

import java.util.EnumSet;
import java.util.List;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;

import lotus.domino.Session;

/**
 * Test cases to read visible entries of a categorized view:<br>
 * <ul>
 * <li>The first sample expands some view entries, the rest is collapsed.</li>
 * <li>The second sample collapses some view entries, the rest is expanded.</li>
 * </ul>
 * Please note that the C API does not work with index positions internally to manage
 * expanded/collapsed entries (like the classic Notes web views do by storing
 * positions of expanded entries as URL query arguments).<br>
 * <br>
 * Instead it uses note ids (both documents and categories have them), which are more
 * stable than index positions when the view index changes:<br>
 * when a view entry is inserted at the top
 * of the view, all index positions change, but the note ids are stable.<br>
 * <br>
 * Unfortunately, note ids are only valid within a database replica.
 * So if you store the expanded/collapsed
 * note ids in a web frontend that works with a Domino server cluster, you would need
 * to track if the server replica changes between requests in case of a cluster failover
 * and flush your expanded/collapsed list. Server replica change detection could be
 * done by comparing the database creation date.
 * 
 * @author Karsten Lehmann
 */
public class TestViewTraversalExpandedEntries extends BaseJNATestClass {

	@Test
	public void testAllCollapsedSomeExpanded() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {

				NotesDatabase dbData = getFakeNamesDb();
				NotesCollection companiesView = dbData.openCollectionByName("CompaniesHierarchical");

				System.out.println("Starting testcase testAllCollapsedSomeExpanded:");

				String[] expandedPositions = new String[] {
						"1",
						"2"
				};

				NotesIDTable collapsedList = companiesView.getCollapsedList();
				//invert collapsed list => it now contains note ids of expanded entries instead of collapsed ones
				collapsedList.setInverted(true);

				System.out.println("===============================");
				System.out.println("Finding note ids for positions:");
				System.out.println("===============================");

				for (String currPos : expandedPositions) {
					List<NotesViewEntryData> entries = companiesView.getAllEntries(currPos, 0, EnumSet.of(Navigate.NEXT), 1,
							EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARYVALUES, ReadMask.INDEXPOSITION), 
							new NotesCollection.EntriesAsListCallback(1));

					if (!entries.isEmpty()) {
						NotesViewEntryData entry = entries.get(0);

						System.out.println(entry.getPositionStr()+": noteid="+entry.getNoteId()+", columns="+entry.getColumnDataAsMap());
						collapsedList.addNote(entry.getNoteId());
					}
					else {
						System.out.println(currPos+" => no entry found");
					}
				}

				System.out.println("=======================================");
				System.out.println("Reading visible (expanded) view entries");
				System.out.println("=======================================");

				//paging offset/count:
				int offset = 0;
				int count = 1000; // Integer.MAX_VALUE;
				System.out.println("Reading max. "+count+" entries starting at offset "+offset);
				System.out.println();

				List<NotesViewEntryData> entries = companiesView.getAllEntries("0", 1+offset, EnumSet.of(Navigate.NEXT_EXPANDED),
						count, EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARYVALUES, ReadMask.INDEXPOSITION),
						new NotesCollection.EntriesAsListCallback(count));

				int idx=0;
				for (NotesViewEntryData currEntry : entries) {
					idx++;
					System.out.println("#"+idx+"\tpos="+currEntry.getPositionStr()+", noteid="+currEntry.getNoteId()+", columns="+currEntry.getColumnDataAsMap());
				}

				System.out.println("Done.");
				return null;
			}
		});
	}

	@Test
	public void testAllExpandedSomeCollapsed() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				NotesCollection companiesView = dbData.openCollectionByName("CompaniesHierarchical");

				System.out.println("Starting testcase testAllExpandedSomeCollapsed:");
				
				String[] collapsedPositions = new String[] {
						"1",
						"2"
				};

				NotesIDTable collapsedList = companiesView.getCollapsedList();

				System.out.println("=========================================");
				System.out.println("Finding note ids for collapsed positions:");
				System.out.println("=========================================");

				for (String currPos : collapsedPositions) {
					List<NotesViewEntryData> entries = companiesView.getAllEntries(currPos, 0, EnumSet.of(Navigate.NEXT), 1,
							EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARYVALUES, ReadMask.INDEXPOSITION), 
							new NotesCollection.EntriesAsListCallback(1));

					if (!entries.isEmpty()) {
						NotesViewEntryData entry = entries.get(0);

						System.out.println(entry.getPositionStr()+": noteid="+entry.getNoteId()+", columns="+entry.getColumnDataAsMap());
						collapsedList.addNote(entry.getNoteId());
					}
					else {
						System.out.println(currPos+" => no entry found");
					}
				}

				System.out.println("=======================================");
				System.out.println("Reading visitle (expanded) view entries");
				System.out.println("=======================================");

				//paging offset/count:
				int offset = 0;
				int count = 1000; // Integer.MAX_VALUE;
				System.out.println("Reading max. "+count+" entries starting at offset "+offset);
				System.out.println();
				
				List<NotesViewEntryData> entries = companiesView.getAllEntries("0", 1+offset, EnumSet.of(Navigate.NEXT_EXPANDED),
						count, EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARYVALUES, ReadMask.INDEXPOSITION),
						new NotesCollection.EntriesAsListCallback(count));

				int idx=0;
				for (NotesViewEntryData currEntry : entries) {
					idx++;
					System.out.println("#"+idx+"\tpos="+currEntry.getPositionStr()+", noteid="+currEntry.getNoteId()+", columns="+currEntry.getColumnDataAsMap());
				}

				System.out.println("Done.");
				return null;
			}
		});
	};


}