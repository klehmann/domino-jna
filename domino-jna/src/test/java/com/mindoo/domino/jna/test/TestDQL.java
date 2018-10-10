package com.mindoo.domino.jna.test;

import static com.mindoo.domino.jna.dql.DQL.*;

import java.util.EnumSet;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesDbQueryResult;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.DBQuery;
import com.mindoo.domino.jna.dql.DQL.DQLTerm;

import lotus.domino.Session;

/**
 * Tests cases for DQL query engine
 * 
 * @author Karsten Lehmann
 */
public class TestDQL extends BaseJNATestClass {

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
	
	@Test
	public void testDQLSearch() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = getFakeNamesDb();
						
//				String query = "(Type = 'Person')";
//				String query = "Lastname = 'Abbott' and Firstname > 'E'";
//				String query = "Lastname = 'Abbott' and Firstname > 'B'";
//				String query = "(Lastname = 'Abbott') and (Firstname > 'B')";

				DQLTerm testTerm = and(
						item("Lastname").isEqualTo("Abbott"),
						item("Firstname").isGreaterThan("B")
						);

				NotesDbQueryResult queryResult =
						db.query(testTerm, EnumSet.of(DBQuery.EXPLAIN), 500000, 500000, 30000);
				
				System.out.println("Explain:\n"+queryResult.getExplainText());
				System.out.println("Error:\n"+queryResult.getErrorText());
				System.out.println("IDTable: "+queryResult.getIDTable());

				return null;
			}
		});
	}

}
