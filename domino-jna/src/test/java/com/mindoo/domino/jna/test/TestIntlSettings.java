package com.mindoo.domino.jna.test;

import org.junit.Test;

import com.mindoo.domino.jna.NotesIntlFormat;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.DateFormat;
import com.mindoo.domino.jna.constants.DateTimeStructure;
import com.mindoo.domino.jna.constants.TimeFormat;
import com.mindoo.domino.jna.constants.ZoneFormat;

import lotus.domino.Session;

/**
 * Tests case for internation settings
 * 
 * @author Karsten Lehmann
 */
public class TestIntlSettings extends BaseJNATestClass {

	@Test
	public void testIntl() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesIntlFormat intl = new NotesIntlFormat();

				System.out.println("Today str: "+intl.getTodayString());
				System.out.println("Tomorrow str: "+intl.getTomorrowString());
				
				intl.setTodayString("heute");
				intl.setTomorrowString("morgen");
				intl.setYesterdayString("gestern");
				intl.setDateSep(".");
				intl.setDateDMY(true);
				intl.setTime24Hour(true);
				intl.setDateAbbreviated(true);
				
				System.out.println("---");

				System.out.println(NotesTimeDate.today().toString(intl, DateFormat.FULL4, TimeFormat.FULL, ZoneFormat.ALWAYS, DateTimeStructure.CDATETIME));
				System.out.println(NotesTimeDate.tomorrow().toString(intl, DateFormat.FULL4, TimeFormat.FULL, ZoneFormat.ALWAYS, DateTimeStructure.CDATETIME));
				System.out.println(NotesTimeDate.yesterday().toString(intl, DateFormat.FULL4, TimeFormat.FULL, ZoneFormat.ALWAYS, DateTimeStructure.CDATETIME));
				
				System.out.println("---");

				System.out.println(NotesTimeDate.now().toString(intl, DateFormat.CPARTIAL4, TimeFormat.FULL, ZoneFormat.ALWAYS, DateTimeStructure.DATETIME));
				System.out.println(NotesTimeDate.adjustedFromNow(0, 0, 1, 0, 0, 0).toString(intl, DateFormat.CPARTIAL4, TimeFormat.FULL, ZoneFormat.ALWAYS, DateTimeStructure.DATETIME));
				System.out.println(NotesTimeDate.adjustedFromNow(0, 0, -1, 0, 0, 0).toString(intl, DateFormat.CPARTIAL4, TimeFormat.FULL, ZoneFormat.ALWAYS, DateTimeStructure.DATETIME));
				
				System.out.println("---");

				System.out.println(NotesTimeDate.now().toString(intl, DateFormat.FULL4, TimeFormat.FULL, ZoneFormat.ALWAYS, DateTimeStructure.CDATETIME));
				System.out.println(NotesTimeDate.adjustedFromNow(0, 0, 1, 0, 0, 0).toString(intl, DateFormat.FULL4, TimeFormat.FULL, ZoneFormat.ALWAYS, DateTimeStructure.CDATETIME));
				System.out.println(NotesTimeDate.adjustedFromNow(0, 0, -1, 0, 0, 0).toString(intl, DateFormat.FULL4, TimeFormat.FULL, ZoneFormat.ALWAYS, DateTimeStructure.CDATETIME));
				System.out.println(NotesTimeDate.adjustedFromNow(0, 0, 2, 0, 0, 0).toString(intl, DateFormat.FULL4, TimeFormat.FULL, ZoneFormat.ALWAYS, DateTimeStructure.CDATETIME));

				return null;
			}
		});
	}

}
