package com.mindoo.domino.jna.test;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;

import org.junit.Test;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesCollection.EntriesAsListCallback;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.Find;
import com.mindoo.domino.jna.constants.ReadMask;

import lotus.domino.Database;
import lotus.domino.DateRange;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewEntryCollection;

public class TestRangeLookup extends BaseJNATestClass {
	@Test
	public void doTestLookup() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase dbData = getFakeNamesDb();
				Database db = getFakeNamesDbLegacy();
				
				View viewRangeTest = db.getView("$DateRangeLookup2");
				Vector<Object> lkKeys = new Vector<Object>();
				Date startDate = new Date(2015 - 1900, 7 - 1, 1, 0, 0, 0);
				Date endDate = new Date(2018 - 1900, 8 - 1, 31, 23, 59, 59);
				
				DateRange range = session.createDateRange(startDate, endDate);
				lkKeys.add("Bennett");
				lkKeys.add(range);
				
				ViewEntryCollection vecEntries = viewRangeTest.getAllEntriesByKey(lkKeys, false);
				System.out.println("Number of entries found: "+vecEntries.getCount());
				ViewEntry veCurrent = vecEntries.getFirstEntry();
				while (veCurrent!=null) {
					if (veCurrent.isValid()) {
						veCurrent.setPreferJavaDates(true);
						Vector colValues = veCurrent.getColumnValues();
						System.out.println(colValues);
					}
					ViewEntry veNext = vecEntries.getNextEntry();
					veCurrent.recycle();
					veCurrent = veNext;
				}
				
				System.out.println("=========================");
				NotesCollection col = dbData.openCollectionByName("$DateRangeLookup");
				col.update();
				
				Calendar startDateCal = Calendar.getInstance();
				startDateCal.setTime(startDate);
				Calendar endDateCal = Calendar.getInstance();
				endDateCal.setTime(endDate);
				
				List<NotesViewEntryData> jnaEntries = col.getAllEntriesByKey(EnumSet.of(Find.CASE_INSENSITIVE, Find.RANGE_OVERLAP),
						EnumSet.of(ReadMask.SUMMARY), new EntriesAsListCallback(Integer.MAX_VALUE),
						"Bennett", new Calendar[] {startDateCal, endDateCal});
				
				for (NotesViewEntryData currEntry : jnaEntries) {
					System.out.println(currEntry);
				}
				return null;
			}
		});
	}
}
