package com.mindoo.domino.jna.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesIDTable.ComparisonResult;
import com.mindoo.domino.jna.NotesIDTable.IEnumerateCallback;
import com.mindoo.domino.jna.structs.NotesTimeDate;

import lotus.domino.Session;

/**
 * Tests cases for string utilities
 * 
 * @author Karsten Lehmann
 */
public class TestIdTable extends BaseJNATestClass {

	/**
	 * ID table insertion tests
	 */
	@Test
	public void testIDTable_insertMethods() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting id table test");
				
				NotesIDTable tableSingleInsertion = new NotesIDTable();

				final int numIdsToAdd = 40000;

				List<Integer> allAddedIds = new ArrayList<Integer>();
				{
					int currNoteId = 4;
					while (allAddedIds.size()<numIdsToAdd) {
						//skip some entries randomly in the test dataset
						if ((Math.random()*10) > 7) {
							allAddedIds.add(currNoteId);
						}

						currNoteId+=4;
					}
				}

				long t0=System.currentTimeMillis();
				for (int i=0; i<allAddedIds.size(); i++) {
					tableSingleInsertion.addNote(allAddedIds.get(i));
				}
				long t1=System.currentTimeMillis();
				System.out.println("Single insertion added "+tableSingleInsertion.getCount()+" entries after "+(t1-t0)+"ms");
				
				Assert.assertEquals("Single insertion added all ids", numIdsToAdd, tableSingleInsertion.getCount());
				
				for (int i=0; i<allAddedIds.size(); i++) {
					Assert.assertTrue("Note ID "+allAddedIds.get(i)+" has been added to the table", tableSingleInsertion.contains(allAddedIds.get(i)));
				}
				
				t0 = System.currentTimeMillis();
				NotesIDTable tableBulkInsertion = new NotesIDTable();
				//best case: ID tqble is empty; then the bulk function can use an optimized call,
				//because id can skip the test where to insert the IDs (they can just be appended)
				tableBulkInsertion.addNotes(allAddedIds);
				t1 = System.currentTimeMillis();
				System.out.println("Bulk insertion added "+tableBulkInsertion.getCount()+" entries after "+(t1-t0)+"ms");
				
				Assert.assertEquals("Bulk insertion added all ids", numIdsToAdd, tableBulkInsertion.getCount());

				for (int i=0; i<allAddedIds.size(); i++) {
					Assert.assertTrue("Note ID "+allAddedIds.get(i)+" has been added to the table", tableBulkInsertion.contains(allAddedIds.get(i)));
				}

				NotesIDTable intersectionIDTable = tableSingleInsertion.intersect(tableBulkInsertion);

				Assert.assertEquals("Insersection ID table has the right entry count", numIdsToAdd, intersectionIDTable.getCount());

				tableSingleInsertion.removeTable(tableBulkInsertion);
				
				Assert.assertTrue("Both takes contain the same IDs", tableSingleInsertion.getCount()==0);
				
				System.out.println("Done with id table test");
				return null;
			}
		});
	}

	/**
	 * ID table comparison tests
	 */
	@Test
	public void testIDTable_tableComparison() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting id table comparison");
				
				NotesIDTable table1 = new NotesIDTable(new int[] {4,8,16,48});
				NotesIDTable table2 = new NotesIDTable(new int[] {8,12,16,48});
				
				ComparisonResult compResult = table1.findDifferences(table2);
				
				int[] idsAdds = compResult.getTableAdds().toArray();
				int[] idsDeletes = compResult.getTableDeletes().toArray();
				int[] idsSame = compResult.getTableSame().toArray();
				
				Assert.assertArrayEquals("Adds are correct", new int[] {12}, idsAdds);
				Assert.assertArrayEquals("Deletes are correct", new int[] {4}, idsDeletes);
				Assert.assertArrayEquals("Same IDs are correct", new int[] {8, 16, 48}, idsSame);
				
				System.out.println("Done with id table comparison");
				return null;
			}
		});
	}
	
	/**
	 * ID Table enumeration tests
	 */
	@Test
	public void testIDTable_enumeration() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting id table comparison");
				
				NotesIDTable table = new NotesIDTable(new int[] {4,8,12,16,48});
				
				Assert.assertEquals("NotesIDTable.getFirstId() is correct", 4, table.getFirstId());
				Assert.assertEquals("NotesIDTable.getLastId() is correct", 48, table.getLastId());
				
				int[] idArr = table.toArray();
				List<Integer> idList = table.toList();
				Assert.assertEquals("ID lists have same size", idArr.length, idList.size());
				
				for (int i=0; i<idArr.length; i++) {
					Assert.assertEquals("List element #"+i+" is correct", idArr[i], idList.get(i).intValue());
				}
				
				final List<Integer> enumResultForward = new ArrayList<Integer>();
				final List<Integer> enumResultBackward = new ArrayList<Integer>();
				
				table.enumerate(new IEnumerateCallback() {

					@Override
					public Action noteVisited(int noteId) {
						enumResultForward.add(noteId);
						return Action.Continue;
					}
					
				});
				table.enumerateBackwards(new IEnumerateCallback() {

					@Override
					public Action noteVisited(int noteId) {
						enumResultBackward.add(noteId);
						return Action.Continue;
					}
				});
				Collections.reverse(enumResultBackward);
				Assert.assertArrayEquals("enumerate and enumerateBackwards are correct", enumResultForward.toArray(), enumResultBackward.toArray());
				
				System.out.println("Done with id table comparison");
				return null;
			}
		});
	}
	
	/**
	 * ID table replacement
	 */
	@Test
	public void testIDTable_tableReplace() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting id table comparison");
				
				int[] idsTable1 = new int[] {4,8,16,48};
				int[] idsTable2 = new int[] {8,12,16,48};
				
				NotesIDTable table1 = new NotesIDTable(idsTable1);
				NotesIDTable table2 = new NotesIDTable(idsTable2);

				//set a time to check if it is preserved
				NotesTimeDate timeBefore = new NotesTimeDate();
				timeBefore.setNow();
				
				table1.setTime(timeBefore);
				
				boolean saveHeader = true;
				
				int[] idsBefore = table1.toArray();
				Assert.assertArrayEquals("Table1 has the right content",idsTable1, idsBefore);
				
				table1.replaceWith(table2, saveHeader);
				
				int[] idsAfter = table1.toArray();
				Assert.assertArrayEquals("Table1 has the right content",idsTable2, idsAfter);
				
				NotesTimeDate timeAfter = table1.getTime();
				
				Assert.assertNotNull("Timedate has not been overwritten", timeAfter);
				Assert.assertArrayEquals("Timedate contains the original value", timeBefore.Innards, timeAfter.Innards);
				
				saveHeader = false;
				table1.replaceWith(table2, saveHeader);

				timeAfter = table1.getTime();
				
				Assert.assertNull("Time has been overwritten", timeAfter);
				
				System.out.println("Done with id table comparison");
				return null;
			}
		});
	}
}
