package com.mindoo.domino.jna.test;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.CalendarRead;
import com.mindoo.domino.jna.constants.CalendarReadRange;
import com.mindoo.domino.jna.utils.NotesCalendarUtils;
import com.mindoo.domino.jna.utils.NotesIniUtils;

import lotus.domino.Session;

/**
 * Tests cases for calendaring and scheduling API
 * 
 * @author Karsten Lehmann
 */
public class TestCalendarAPIs extends BaseJNATestClass {

	@Test
	public void testFreeTimeSearch() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				System.out.println("Starting calendar lookup");

				NotesTimeDate start = NotesTimeDate.now();
				start.adjust(0, -6, 0, 0, 0, 0);
				NotesTimeDate end = NotesTimeDate.now();

				String mailServerName = NotesIniUtils.getEnvironmentString("MailServer");
				String mailFile = NotesIniUtils.getEnvironmentString("MailFile");
				
				NotesDatabase dbMail = new NotesDatabase(mailServerName, mailFile, (String) null);
				StringWriter retICal = new StringWriter();
				List<String> retUIDs = new ArrayList<String>();
				
				int entriesToRead = 5;
				
				//read all available fields
//				EnumSet<CalendarReadRange> dataToRead = EnumSet.allOf(CalendarReadRange.class);
				
				//read some only a few fields
				EnumSet<CalendarReadRange> dataToRead = EnumSet.of(
						CalendarReadRange.DTSTART,
						CalendarReadRange.DTEND,
						CalendarReadRange.SUMMARY);
				
				NotesCalendarUtils.readRange(dbMail, start, end, 0, entriesToRead, dataToRead,
						retICal, retUIDs);
				
				System.out.println("Result of range lookup:");
				System.out.println("=======================");
				System.out.println("UIDs:");
				System.out.println(retUIDs);
				System.out.println();
				System.out.println("ICal:");
				System.out.println(retICal.toString());
				
				System.out.println("Now opening every calendar entry to read the full data");
				System.out.println("======================================================");
				for (String currUID : retUIDs) {
					System.out.println("UID: "+currUID);
					System.out.println("-----");
					String iCal = NotesCalendarUtils.readCalendarEntry(dbMail, currUID, null, EnumSet.of(CalendarRead.INCLUDE_X_LOTUS));
					System.out.println(iCal);
				}
				
				System.out.println("Done with calendar lookup");
				return null;
			}
		});
	}
	
}
