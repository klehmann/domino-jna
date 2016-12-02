package com.mindoo.domino.jna.test;

import java.util.Vector;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;

import lotus.domino.Database;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewEntryCollection;

/**
 * Test cases that work with the legacy API
 * 
 * @author Karsten Lehmann
 */
public class TestLegacyViewEntryCollectionIntersection extends BaseJNATestClass {

	@Test
	public void testViewTraversal_numericInequalityLookup() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();

				Database db = session.getDatabase(dbData.getServer(), dbData.getRelativeFilePath());
				View viewPeopleMultiCol = db.getView("PeopleFlatMultiColumnSort");
				viewPeopleMultiCol.resortView("Firstname", true);
				ViewEntryCollection alexEntryCol = viewPeopleMultiCol.getAllEntriesByKey("Al", false);
				
				viewPeopleMultiCol.resortView("Lastname", true);
				ViewEntryCollection prefixJCol = viewPeopleMultiCol.getAllEntriesByKey("J", false);
				
				long t0=System.currentTimeMillis();
//				prefixJCol.intersect(alexEntryCol);
				
				ViewEntry veCurrent = prefixJCol.getFirstEntry();
				while (veCurrent!=null) {
					if (veCurrent.isValid()) {
						if (alexEntryCol.contains(veCurrent)) {
							veCurrent.setPreferJavaDates(true);
							Vector<?> colValues = veCurrent.getColumnValues();
							System.out.println("ID="+veCurrent.getNoteIDAsInt() + "\tPos=" + veCurrent.getPosition('.') + "\tValues=" + colValues);
						}
					}
					
					ViewEntry veNext = prefixJCol.getNextEntry(veCurrent);
					veCurrent.recycle();
					veCurrent = veNext;
				}
				long t1=System.currentTimeMillis();
				System.out.println("Done after "+(t1-t0)+"ms");
				return null;
			}
		});
	
	}

}