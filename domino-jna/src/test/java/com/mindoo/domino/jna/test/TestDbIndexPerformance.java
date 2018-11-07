package com.mindoo.domino.jna.test;

import java.util.Calendar;
import java.util.EnumSet;
import java.util.LinkedHashMap;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.IItemTableData;
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDatabase.SearchCallback;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesSearch;
import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import lotus.domino.Session;

/**
 * Tests cases for database searches
 * 
 * @author Karsten Lehmann
 */
public class TestDbIndexPerformance extends BaseJNATestClass {

	@Test
	public void testDbSearch_searchSelectedNoteIds() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				
				//limit formula search to all documents in the people view:
				NotesCollection peopleView = dbData.openCollectionByName("People");
				
				NotesIDTable retFilterIds = new NotesIDTable();
				boolean filterTable = false; // no read rights check required, handled by NSF search
				peopleView.getAllIds(Navigate.NEXT, filterTable, retFilterIds);
				
				System.out.println("Using filter ID table with "+retFilterIds.getCount()+" entries for NSF search");
				
				//run the loop multiple times to get measurements when the data comes
				//from the OS disk cache)
				
				for (int i=0; i<3; i++) {
					
					//field values / computed values to be returned by the search
					
					//if columnFormulas==null, the whole summary buffer is returned,
					//which is much slower; using EnumSet.noneOf(Search.class) instead
					//of EnumSet.of(Search.SUMMARY) would be the fastest, if we only
					//need the matching note ids and no note data (summaryBufferData
					//would then be returned as null)
					LinkedHashMap<String, String> columnFormulas = new LinkedHashMap<String, String>();
					columnFormulas.put("firstname", "");
					columnFormulas.put("lastname", "");
					columnFormulas.put("companyname", "");
					columnFormulas.put("fullname", "");
					columnFormulas.put("created", "@Created");

					long t0=System.currentTimeMillis();

					final AtomicInteger cnt = new AtomicInteger();

					String viewTitle = "-";
					NotesTimeDate since = null;
					
					NotesSearch.search(dbData, retFilterIds, "Type=\"Person\" & @Begins(Firstname;\"A\")",
							columnFormulas, viewTitle,
							EnumSet.of(Search.SUMMARY), EnumSet.of(NoteClass.DOCUMENT), since,
							new SearchCallback() {

						@Override
						public Action noteFound(NotesDatabase parentDb, ISearchMatch searchMatch,
								IItemTableData summaryBufferData) {

							String firstName = summaryBufferData.getAsString("firstname", "");
							Assert.assertTrue("Firstname not empty", firstName!=null && firstName.length()>0);
							String lastName = summaryBufferData.getAsString("lastname", "");
							Assert.assertTrue("Lastname not empty", lastName!=null && lastName.length()>0);
							Calendar created = summaryBufferData.getAsCalendar("created", null);
							Assert.assertNotNull("Creation date is not null", created);

							cnt.incrementAndGet();
							return null;
						}

					});

					long t1=System.currentTimeMillis();
					System.out.println("Read "+cnt.get()+" documents in "+(t1-t0)+"ms");
					
					Assert.assertTrue("Search returned any entries", cnt.get() > 0);
				}
				return null;
			}
		});
	
	}
	
}
