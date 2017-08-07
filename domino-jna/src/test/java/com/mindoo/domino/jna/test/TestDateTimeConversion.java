package com.mindoo.domino.jna.test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;

import junit.framework.Assert;
import lotus.domino.Session;

public class TestDateTimeConversion extends BaseJNATestClass {

	@Test
	public void testBrokenAllDayAnyDayCheck() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				//Thu Aug 10 00:00:00 GMT:
				int[] testInnards = new int[] {0, -1054506632};
				NotesTimeDate timeDate = new NotesTimeDate(testInnards);
				Calendar cal = NotesDateTimeUtils.innardsToCalendar(false, 0, testInnards);
				
				int hour = cal.get(Calendar.HOUR_OF_DAY);
				int minute = cal.get(Calendar.MINUTE);
				int second = cal.get(Calendar.SECOND);
				int millis = cal.get(Calendar.MILLISECOND);
				
				Assert.assertEquals("Hour is 0", 0, hour);
				Assert.assertEquals("Minute is 0", 0, minute);
				Assert.assertEquals("Second is 0", 0, second);
				Assert.assertEquals("Millisecond is 0", 0, millis);
				
				return null;
			}
		});
	}
	
//	@Test
}
