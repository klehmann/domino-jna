package com.mindoo.domino.jna.test;

import java.util.NavigableMap;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesItem;

import lotus.domino.Session;

/**
 * Tests cases for item definition table
 * 
 * @author Karsten Lehmann
 */
public class TestItemDefinitionTable extends BaseJNATestClass {

	@Test
	public void testFreeTimeSearch() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = getFakeNamesDb();
				NavigableMap<String,Integer> itemDefTable = db.getItemDefinitionTable();
				System.out.println("Number of items in db: "+itemDefTable.size());
				System.out.println(itemDefTable);
				Assert.assertTrue("Item firstname exists in item def table and map comparison is case insensitive", itemDefTable.containsKey("fIrsTname"));
				Assert.assertEquals("Item type for firstname is TYPE_TEXT", itemDefTable.get("fIrsTname").intValue(), NotesItem.TYPE_TEXT);
				
				return null;
			}
		});
	}
	
}
