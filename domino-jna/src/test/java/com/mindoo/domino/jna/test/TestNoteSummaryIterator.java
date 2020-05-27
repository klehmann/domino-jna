package com.mindoo.domino.jna.test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.Test;

import com.mindoo.domino.jna.NoteSummaryIterator;
import com.mindoo.domino.jna.NoteSummaryIterator.NoteData;
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesCollection.Direction;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.constants.Find;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.gc.NotesGC;

import lotus.domino.Session;

/**
 * Test for efficient reading of note summary item data
 * 
 * @author Karsten Lehmann
 */
public class TestNoteSummaryIterator extends BaseJNATestClass {

	@Test
	public void testIteratorNoteData() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesGC.setDebugLoggingEnabled(false);
				NotesDatabase db = new NotesDatabase("", "fakenames.nsf", "");
								
				// open our example view and change its sorting to read the note ids in descending lastname order
				NotesCollection inbox = db.openCollectionByName("PeopleFlatMultiColumnSort");
				inbox.resortView("Lastname", Direction.Descending);
				
				// read all note ids of documents in view order
				long t0_readIds=System.currentTimeMillis();
				LinkedHashSet<Integer> idsSorted = inbox.getAllIds(Navigate.NEXT_NONCATEGORY);
//				LinkedHashSet<Integer> idsSorted = inbox.getAllIdsByKey(EnumSet.of(Find.CASE_INSENSITIVE, Find.EQUAL), "Abbott");
//				LinkedHashSet<Integer> idsSorted = inbox.getAllIdsInCategory("MyCategory", EnumSet.of(Navigate.NEXT_NONCATEGORY));
				
				long t1_readIds=System.currentTimeMillis();
				
				System.out.println("Reading "+idsSorted.size()+" note ids from the view took "+(t1_readIds-t0_readIds)+"ms");
				
				//select which data we would like to have computed
				Map<String,String> columnFormulas = new HashMap<>();
				columnFormulas.put("Lastname", ""); // static item
				columnFormulas.put("Firstname", "");
				columnFormulas.put("_created", "@Created"); // computed item
				
				long t0_read = System.currentTimeMillis();
				int pageSize = 50000;
				int skip = 0;
				int count = 41000;
				NoteSummaryIterator summaryIterator = new NoteSummaryIterator(db, pageSize, idsSorted.iterator(),
						skip, count, columnFormulas);
				
				int idx=1;
				
				StringBuilder sb = new StringBuilder();
				
				while (summaryIterator.hasNext()) {
					NoteData currNoteData = summaryIterator.next();
					
					//get basic note info, e.g. note ids, UNID, modified date, sequence number, sequence time
					ISearchMatch currNoteSearchMatch = currNoteData.getSearchMatch();
					int noteId = currNoteSearchMatch.getNoteId();
					String unid = currNoteSearchMatch.getUNID();
					
					//map with case-insensitive keys
					Map<String,Object> currSummaryData = currNoteData.getAllSummaryData();
					
					sb.append("#").append(idx).append("\t").append(noteId).append("\t")
					.append(unid).append("\t").append(currSummaryData).append("\n");
					
					idx++;
				}
				long t1_read = System.currentTimeMillis();
				
				System.out.println(sb.toString());
				System.out.println("Reading data for "+idx+" documents took "+(t1_read-t0_read)+"ms");
				
				return null;
			}
		});
	}

}
