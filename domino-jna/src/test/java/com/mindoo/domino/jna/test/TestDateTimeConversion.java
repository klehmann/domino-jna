package com.mindoo.domino.jna.test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;

import junit.framework.Assert;
import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.NotesException;
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
				
				Assert.assertNotNull("Conversion should produce a value", cal);
				
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
	
	@Test
	public void testInvalidInnards() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = getFakeNamesDb();
				int[] invalidInnards = new int[] {5767168, 83886086};
				NotesTimeDate invalidTimeDate = new NotesTimeDate(invalidInnards);
				Calendar invalidTimeDateAsCal = invalidTimeDate.toCalendar();

				Assert.assertEquals("Hour is 0", 0, invalidTimeDateAsCal.get(Calendar.HOUR));
				Assert.assertEquals("Minute is 0", 0, invalidTimeDateAsCal.get(Calendar.MINUTE));
				Assert.assertEquals("Second is 0", 0, invalidTimeDateAsCal.get(Calendar.SECOND));
				Assert.assertEquals("Day of month is 1", 1, invalidTimeDateAsCal.get(Calendar.DAY_OF_MONTH));
				Assert.assertEquals("Month is 1", 1, invalidTimeDateAsCal.get(Calendar.MONTH));
				Assert.assertEquals("Year is 1", 1, invalidTimeDateAsCal.get(Calendar.YEAR));

				Calendar cal = NotesDateTimeUtils.innardsToCalendar(false, 0, invalidInnards);
				Assert.assertNull("Conversion of invalid values should return a null value", cal);
				
				Assert.assertEquals("Return empty string for NotesTimeDate.toString() of invalid timedate", "", invalidTimeDate.toString());
				
				NotesNote doc = db.createNote();
				doc.replaceItemValue("Form", "Person");
				doc.replaceItemValue("Type", "Person");
				doc.replaceItemValue("Lastname", "1. Invalid date");
				doc.replaceItemValue("Firstname", "Test");
				doc.replaceItemValue("DateValue", invalidTimeDate);
				
				List<Object> rereadInvalidValue = doc.getItemValue("DateValue");
				Assert.assertTrue("Item has a Calendar value", rereadInvalidValue.size()==1 && rereadInvalidValue.get(0) instanceof Calendar);
				Calendar rereadCal = (Calendar) rereadInvalidValue.get(0);
				
				Assert.assertEquals("Hour is 0", 0, rereadCal.get(Calendar.HOUR));
				Assert.assertEquals("Minute is 0", 0, rereadCal.get(Calendar.MINUTE));
				Assert.assertEquals("Second is 0", 0, rereadCal.get(Calendar.SECOND));
				Assert.assertEquals("Day of month is 1", 1, rereadCal.get(Calendar.DAY_OF_MONTH));
				Assert.assertEquals("Month is 1", 1, rereadCal.get(Calendar.MONTH));
				Assert.assertEquals("Year is 1", 1, rereadCal.get(Calendar.YEAR));

				doc.update();
				
				String unid = doc.getUNID();
				doc.recycle();
				
				Database dbLegacy = getFakeNamesDbLegacy();
				Document docLegacy = dbLegacy.getDocumentByUNID(unid);
				Vector invalidValues = docLegacy.getItemValue("DateValue");
				
				Assert.assertEquals("Legacy API returns Vector with one value", 1, invalidValues.size());
				
				DateTime invalidValue = (DateTime) invalidValues.get(0);
				Assert.assertEquals("Legacy API returns empty string for DateTime.toString() of invalid value", "", invalidValue.toString());

				int err = 0;
				try {
					Date dtVal = invalidValue.toJavaDate();
				}
				catch (NotesException e) {
					err = e.id;
				}
				Assert.assertEquals("Legacy API also returns error code 4458 when converting invalid dates", 4458, err);
				
				return null;
			}
		});
	}
}
