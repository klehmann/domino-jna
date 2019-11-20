package com.mindoo.domino.jna.test;

import java.util.EnumSet;
import java.util.List;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesCollection.Direction;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesFTSearchResult;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.FTSearch;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.gc.NotesGC;

import lotus.domino.Session;

public class TestViewTraversalWithFTSearch extends BaseJNATestClass {

	/**
	 * This test case demonstrates how a database FT search can be restricted to
	 * specific note ids and how the FT search result can then be used to read
	 * matching view entries from a sorted view
	 */
	@Test
	public void testReadSearchResultsFromViewViaNoteIdsSelection() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting testcase testReadSearchResultsFromViewViaNoteIdsSelection:");

				NotesDatabase dbData = getFakeNamesDb();
				
				//view should be non-permuted => documents should not exist in the view multiple times, otherwise
				//we would get duplicates in the result data and there's a chance that getAllEntries runs
				//in an infinite loop, because it's working position based and the positions returned from
				//the C API are not correct for duplicate occurences (always returns the position of the
				//first doc occurence, already reported to HCL dev)
				NotesCollection companiesView = dbData.openCollectionByName("Companies");
				
				//resort view by company name
				companiesView.resortView("companyname", Direction.Ascending);
				
				//get all note ids from view index (very quick operation, because it does not check read access rights)
				NotesIDTable allNoteIdsInView = new NotesIDTable();
				companiesView.getAllIds(Navigate.NEXT, false, allNoteIdsInView);
				
				String ftQuery = "[lastname]=Hill OR [lastname]=Potter";
				
				//run database FT index search, restricted to the note ids of the view
				EnumSet<FTSearch> searchOptions = EnumSet.of(FTSearch.RET_IDTABLE);	// return an id table with search matches
																					// via NotesFTSearchResult.getMatches();
																					// we use these note ids to filter the view data
				
				//optional search options:
				searchOptions.add(FTSearch.FUZZY); // do a fuzzy search
				searchOptions.add(FTSearch.STEM_WORDS); // stem words
				searchOptions.add(FTSearch.THESAURUS_WORDS); // use thesaurus
				
				if (!dbData.isFTIndex()) {
					searchOptions.add(FTSearch.NOINDEX);		// build a temporary FT index of DB is not indexed,
															// see comments of FTSearch.NOINDEX for limits (default: max 5000 note ids)
				}
				
				NotesFTSearchResult dbSearchResult = dbData.ftSearchExt(ftQuery, 0, searchOptions, allNoteIdsInView, 0, 0);
				
				NotesIDTable dbSearchMatchesIdTable = dbSearchResult.getMatches();
				//apply selection to the view so that we can use Navigate.NEXT_SELECTED to jump between selected entries
				companiesView.select(dbSearchMatchesIdTable, true);
				
				//paging offset/count in view:
				int offset = 0;
				int count = 1000; // Integer.MAX_VALUE;
				
				// amount of entries to be read with one NIFReadEntries call, should be higher if
				// EntriesAsListCallback.isAccepted is overridden and entries are skipped in code 
				int preloadEntryCount = count;
				
				System.out.println("Reading max. "+count+" entries starting at offset "+offset);
				System.out.println();

				EnumSet<ReadMask> readMask = EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARYVALUES, ReadMask.INDEXPOSITION);
				
				// if possible do not do this to get the total count; expensive operation that needs to traverse the
				// entire view index to count selected view entries
				//
				// int totalMatches = companiesView.getEntryCount(Navigate.NEXT_SELECTED);
				// System.out.println(totalMatches+" docs in the view match the FT query");
				
				List<NotesViewEntryData> entries = companiesView.getAllEntries("0", 1+offset, EnumSet.of(Navigate.NEXT_SELECTED),
						preloadEntryCount, readMask, new NotesCollection.EntriesAsListCallback(count));
				

				System.out.println("=========================================");
				System.out.println("Reading search hits in current view order");
				System.out.println("=========================================");

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

	/**
	 * Runs a FT search and applies the result onto a NotesCollection index. When navigating in
	 * the view, the results
	 */
	@Test
	public void testReadSearchResultsFromViewViaHitNav() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesGC.setDebugLoggingEnabled(false);
				
				System.out.println("Starting testcase testReadSearchResultsFromViewViaHitNav:");

				NotesDatabase dbData = getFakeNamesDb();
				
				//view should be non-permuted => documents should not exist in the view multiple times, otherwise
				//we would get duplicates in the result data and there's a chance that getAllEntries runs
				//in an infinite loop, because it's working position based and the positions returned from
				//the C API are not correct for duplicate occurences (always returns the position of the
				//first doc occurence, already reported to HCL dev)
				NotesCollection companiesView = dbData.openCollectionByName("Companies");
				
				//resort view by company name
				companiesView.resortView("companyname", Direction.Ascending);
				
				String ftQuery = "[lastname]=Hill OR [lastname]=Potter";

				//run database FT index search, restricted to the note ids of the view
				EnumSet<FTSearch> searchOptions = EnumSet.noneOf(FTSearch.class);
				
				//optional search options:
				searchOptions.add(FTSearch.FUZZY); // do a fuzzy search
				searchOptions.add(FTSearch.STEM_WORDS); // stem words
				searchOptions.add(FTSearch.THESAURUS_WORDS); // use thesaurus
				
				// just return number of results in viewSearchResult.getNumDocs();
				// if set, navigating the view via NEXT_HIT returns no results
				// searchOptions.add(FTSearch.NUMDOCS_ONLY);
				
				// view FT search supports two sort modes, by creation date or by last modified (in this file):
				// searchOptions.add(FTSearch.SORT_DATE_CREATED);
				// searchOptions.add(FTSearch.SORT_DATE);
				// 
				// if both sort options are not set and searchOptions contains FTSearch.SCORES, the results
				// are sorted by relevance
				
				// we select to sort by last modified:
				searchOptions.add(FTSearch.SORT_DATE_MODIFIED);

				// by default, the results are sorted by creation date or last modified date in descending order;
				// use SORT_ASCEND to reverse sort order:
				searchOptions.add(FTSearch.SORT_ASCEND);

				NotesFTSearchResult viewSearchResult = companiesView.ftSearch(ftQuery, 0, searchOptions);
				int numHits = viewSearchResult.getNumHits();
				System.out.println("Number of FT results found: "+numHits);
				
				//paging offset/count in view:
				int offset = 0;
				int count = 1000; // Integer.MAX_VALUE;
				
				// amount of entries to be read with one NIFReadEntries call, should be higher if
				// EntriesAsListCallback.isAccepted is overridden and entries are skipped in code 
				int preloadEntryCount = count;
				
				System.out.println("Reading max. "+count+" entries starting at offset "+offset);
				System.out.println();

				EnumSet<ReadMask> readMask = EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARYVALUES);	// not specifying ReadMask.INDEXPOSITION here like in the other test case, because
																									// the returned data would just return increasing position numbers starting with 1

				
				// if possible do not do this to get the total count; expensive operation that needs to traverse the
				// entire view index to count selected view entries
				//
				// int totalMatches = companiesView.getEntryCount(Navigate.NEXT_SELECTED);
				// System.out.println(totalMatches+" docs in the view match the FT query");
				
				List<NotesViewEntryData> entries = companiesView.getAllEntries("0", 1+offset, EnumSet.of(Navigate.NEXT_HIT),
						preloadEntryCount, readMask, new NotesCollection.EntriesAsListCallback(count));
				

				System.out.println("====================================================");
				if (searchOptions.contains(FTSearch.SORT_DATE_MODIFIED)) {
					System.out.println("Reading search hits sorted by last modified in "+
							(searchOptions.contains(FTSearch.SORT_ASCEND) ? "ascending " : "descending ") + "order");
				}
				else if (searchOptions.contains(FTSearch.SORT_DATE_CREATED)) {
					System.out.println("Reading search hits sorted by creation date in "+
							(searchOptions.contains(FTSearch.SORT_ASCEND) ? "ascending " : "descending ") + "order");
				}
				System.out.println("====================================================");

				int idx=0;
				for (NotesViewEntryData currEntry : entries) {
					idx++;
					NotesNote note = dbData.openNoteById(currEntry.getNoteId());
					
					System.out.println("#"+idx+
							"\tcreated="+note.getCreationDateAsTimeDate()+
							", modifiedinfile="+note.getOID().getSequenceTime() +
							", noteid="+currEntry.getNoteId()+", columns="+currEntry.getColumnDataAsMap());
					note.recycle();
				}

				System.out.println("Done.");
				return null;
			}
		});
	}
}