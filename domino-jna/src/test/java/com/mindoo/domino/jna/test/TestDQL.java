package com.mindoo.domino.jna.test;

//import all static builder methods on DQL class (e.g. "item" / "and" / "or" etc.)
import static com.mindoo.domino.jna.dql.DQL.*;

import java.util.EnumSet;
import java.util.List;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesCollection.Direction;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDbQueryResult;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.DBQuery;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.dql.DQL.DQLTerm;

import lotus.domino.Session;

/**
 * Tests cases for DQL query building and execution, requires Domino V10.
 * 
 * @author Karsten Lehmann
 */
public class TestDQL extends BaseJNATestClass {

	/**
	 * Some tests for the DQL query builder
	 */
	@Test
	public void testDQLFormatting() {
		
		// order_no > 146751 and order_no <= 150111
		DQLTerm term1 = and(
				item("order_no").isGreaterThan(146751),
				item("order_no").isLessThanOrEqual(150111)
				);
		System.out.println("Term 1:\n"+term1+"\n");
		
		// sales_person in (‘Chad Keighley’, ‘Jeff Chantel’, ‘Louis Cawlfield’, ‘Mariel Nathanson’)
		DQLTerm term2 = item("sales_person").in("Chad Keighley", "Jeff Chantel",
				"Louis Cawlfield", "Mariel Nathanson");
		System.out.println("Term 2:\n"+term2+"\n");
		
		// date_origin > @dt(‘20181010T100000+0500’)
		DQLTerm term3 = item("date_origin").isGreaterThan(new NotesTimeDate(2018, 10, 10, 10, 0, 0));
		System.out.println("Term 3:\n"+term3+"\n");

		// ‘Orders’.order_type = ‘Phone’
		DQLTerm term4 = view("Orders").column("order_type").isEqualTo("Phone");
		System.out.println("Term 4:\n"+term4+"\n");

		// in (‘Orders’, ’Special orders folder 1’, ‘Old_orders 2’)
		DQLTerm term5 = in("Orders", "Special orders folder 1", "old_orders 2");
		System.out.println("Term 5:\n"+term5+"\n");

		// Order_origin in (‘London’, ‘LA’, ‘Tokyo’) AND date_origin > @dt(‘20160511’) or partno = 388388
		DQLTerm term6 = or(
					and(
						item("Order_origin").in("London", "LA", "Tokyo"),
						item("date_origin").isGreaterThan(new NotesTimeDate(2016, 5, 11))
						),
					item("partno").isEqualTo(388388)
				);
		System.out.println("Term 6:\n"+term6+"\n");

		// ‘Soon to be special’.Status = ‘Shipping’ and
		//       ( order_origin = ‘LA’ or sales_person in (‘Chad Keighley’, ‘Jeff Chantel’, ‘Louis Cawlfield’, ‘Mariel Nathanson’))
		DQLTerm term7 = and (
				view("Soon to be special").column("Status").isEqualTo("Shipping"),
				or (
						item("order_origin").isEqualTo("LA"),
						item("sales_person").in("Chad Keighley", "Jeff Chantel", "Louis Cawlfield", "Mariel Nathanson")
					)
				);
		System.out.println("Term 7:\n"+term7+"\n");

		// ‘Soon to be special’.Status = ‘Inventory’ and
		//		 ( order_origin = ‘Detroit’ or NOT sales_person in (‘Harold Cunningham’, ‘Julie Leach’, ‘Gordon Smith’, ‘Terence Henry’))
		DQLTerm term8 = and(
				view("Soon to be special").column("Status").isEqualTo("Inventory"),
				or(
						item("order_origin").isEqualTo("Detroit"),
						not(
								item("sales_person").in("Harold Cunningham", "Julie Leach", "Gordon Smith", "Terence Henry")
							)
					)
				);
		System.out.println("Term 8:\n"+term8+"\n");
		
		DQLTerm term9 = all();
		System.out.println("Term 9:\n"+term9+"\n");

	}
	
	/**
	 * Demonstrates how to run a DQL query, project the result
	 * onto a view to output a sorted list of document items
	 */
	@Test
	public void testDQLSearch() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = getFakeNamesDb();

				//build query Lastname='Abbott' and Firstname>'B'
				DQLTerm dqlQuery = and(
						item("Lastname").isEqualTo("Abbott"),
						item("Firstname").isGreaterThan("B")
						);

				//execute DQL search
				System.out.println("Running DQL query: "+dqlQuery);
				
				NotesDbQueryResult queryResult;
				
				//run query without returning EXPLAIN text, use defaults for limits
				//queryResult = db.query(dqlQuery);
				
				//run query with EXPLAIN text,, use defaults for limits
				queryResult = db.query(dqlQuery, EnumSet.of(DBQuery.EXPLAIN));
				
				//run query with EXPLAIN text and custom limits
				//int maxDocsScanned = 1000;
				//int maxViewEntriesScanned = 1000;
				//int maxMsecs = 5*1000;
				//queryResult = db.query(dqlQuery, EnumSet.of(DBQuery.EXPLAIN),
				//		maxDocsScanned,
				//		maxViewEntriesScanned,
				//		maxMsecs);
				
				//output some data about the query execution, e.g. the amount
				//if seconds it took to produce the result
				System.out.println("DQL result:\n"+queryResult);
				System.out.println("Explain text:\n"+queryResult.getExplainText());
				System.out.println("IDTable with results: "+queryResult.getIDTable());

				//now let's display some data for the matching note ids
				
				//open view with sortable columns Lastname/Firstname
				NotesCollection peopleView = db.openCollectionByName("($lkPeopleGenericData1)");
				//set result sorting (column must be sortable via click)
				peopleView.resortView("Firstname", Direction.Descending);
				
				//change view selection to our DQL result
				boolean clearPrevSelection = true;
				peopleView.select(queryResult.getIDTable(), clearPrevSelection);
				
				//parameter for paging
				int offset = 0;
				int pageSize = Integer.MAX_VALUE; //load all entries in selection
				
				long t0=System.currentTimeMillis();
				
				//read all selected view entries
				List<NotesViewEntryData> entries = peopleView.getAllEntries(
						// startpos="0" means one row above first row
						"0",
						//skip = 1 means move to the first relevant row from startpos + paging offset
						1 + offset,
						//use navigation strategy NEXT_SELECTED to only return selected entries
						EnumSet.of(Navigate.NEXT_SELECTED),
						//number of entries to buffer with one read call
						//(still limited by the 64k overall buffer size)
						pageSize,
						//we want to read the note id and the column values
						EnumSet.of(ReadMask.NOTEID, ReadMask.SUMMARYVALUES),
						//generic callback that produces NotesViewEntryData objects;
						//here you can use your own subclasses of "ViewLookupCallback"
						//to produce something else, e.g. JSON objects or populate POJO's with the data
						new NotesCollection.EntriesAsListCallback(pageSize));
				
				long t1=System.currentTimeMillis();
				System.out.println(entries.size()+" view rows selected in "+(t1-t0)+"ms");
				
				//print the result
				System.out.println("Selected view data:");
				System.out.println("===================");
				System.out.println("Index\tNoteId\t\tData");
				
				for (int i=0; i<entries.size(); i++) {
					NotesViewEntryData currEntry = entries.get(i);
					
					System.out.println("#"+(i+1)+"\t"+currEntry.getNoteId()+"\t\t"+
							currEntry.get("Lastname")+", "+currEntry.getAsNameAbbreviated("Firstname"));
				}
				
				return null;
			}
		});
	}

}
