package com.mindoo.domino.jna.test;

import java.util.LinkedHashSet;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.constants.Navigate;

import junit.framework.Assert;
import lotus.domino.Session;

/**
 * Tests cases for note id reading in views
 * 
 * @author Karsten Lehmann
 */
public class TestReadNoteIdsFromView extends BaseJNATestClass {

	@Test
	public void testReadNoteIds() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting to read note ids from view");
				
				NotesDatabase dbData = getFakeNamesDb();
				NotesCollection colFromDbData = dbData.openCollectionByName("People");
				
				long t0,t1;
				
				NotesIDTable idTableReadWithOptimizedCall = new NotesIDTable();
				
				{
					t0=System.currentTimeMillis();
					//method uses NIFGetIDTableExtended internally to quickly populate an ID table
					//with all ids found in a view
					colFromDbData.getAllIds(Navigate.NEXT_NONCATEGORY, true, idTableReadWithOptimizedCall);
					t1=System.currentTimeMillis();
					System.out.println("Reading "+idTableReadWithOptimizedCall.getCount()+" note ids via NIFGetIDTableExtended WITHOUT enumeration took "+(t1-t0)+"ms");

					//enumerate ids manually; calling NotesIDTable.toArray() does the same;
					//we just want to verify that the enumeration returns the right data
					final int[] allIds = new int[idTableReadWithOptimizedCall.getCount()];
					final int[] idx = new int[1];
					
					idTableReadWithOptimizedCall.enumerate(new NotesIDTable.IEnumerateCallback() {

						@Override
						public Action noteVisited(int noteId) {
							allIds[idx[0]] = noteId;
							idx[0]++;

							return Action.Continue;
						}});
					Assert.assertEquals("Callback is called for each id in the table", idx[0], idTableReadWithOptimizedCall.getCount());
					
					t1=System.currentTimeMillis();
					System.out.println("Reading "+idTableReadWithOptimizedCall.getCount()+" note ids via NIFGetIDTableExtended WITH enumeration took "+(t1-t0)+"ms");
					
				}
				
				{
					t0=System.currentTimeMillis();
					//read ids via view traversal; returns ids in view order, but for databases on another server,
					//this method is expected to be slower than using NIFGetIDTableExtended
					LinkedHashSet<Integer> idsReadWithViewTraversal = colFromDbData.getAllIds(Navigate.NEXT_NONCATEGORY);
					Assert.assertEquals("View traversal returned the same count as ID scan via NIFGetIDTableExtended", idTableReadWithOptimizedCall.getCount(), idsReadWithViewTraversal.size());
					
					t1=System.currentTimeMillis();
					System.out.println("Reading "+idsReadWithViewTraversal.size()+" note ids with via traversal took "+(t1-t0)+"ms");
					
					
					//compare both results
					NotesIDTable idTableReadWithViewTraversal = new NotesIDTable(idsReadWithViewTraversal);
					Assert.assertEquals("Both id tables have the same size", idTableReadWithOptimizedCall.getCount(), idTableReadWithViewTraversal.getCount());
					
					Assert.assertTrue("Both id tables contain the same data", idTableReadWithOptimizedCall.equalsTable(idTableReadWithViewTraversal));
					
					NotesIDTable clonedOptimizedIDTable = (NotesIDTable) idTableReadWithOptimizedCall.clone();
					clonedOptimizedIDTable.removeTable(idTableReadWithViewTraversal);
					Assert.assertTrue("Intersection of both tables is empty: "+clonedOptimizedIDTable.toList(), clonedOptimizedIDTable.isEmpty());
				}
				
				System.out.println("Done reading note ids from view");
				return null;
			}
		});
	}
}
