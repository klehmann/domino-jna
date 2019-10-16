package com.mindoo.domino.jna.test;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.constants.Find;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.gc.NotesGC;

import junit.framework.Assert;
import lotus.domino.Session;

public class TestFindByKeyExtended3 extends BaseJNATestClass {

	@Test
	public void testFindCallback() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = getFakeNamesDb();
				final NotesCollection view = db.openCollectionByName("PeopleFlatMultiColumnSort");

				for (int i=0; i<5; i++) {
					Object[] keys = new Object[] {"B"};
					EnumSet<Find> findFlags = EnumSet.of(Find.CASE_INSENSITIVE, Find.EQUAL, Find.PARTIAL);

					NotesGC.setCustomValue("collection_optimizedlookup", Boolean.FALSE);

					Set<Integer> idsWithoutOptimization = view.getAllIdsByKey(findFlags, keys);

					NotesGC.setCustomValue("collection_optimizedlookup", Boolean.TRUE);

					Set<Integer> idsWithOptimization = view.getAllIdsByKey(findFlags, keys);

					Assert.assertEquals("Lookup result is the same", idsWithoutOptimization, idsWithOptimization);
				}
				return null;
			}
		});

	}

}
