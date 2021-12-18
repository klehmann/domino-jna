package com.mindoo.domino.jna.test;

import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.internal.InnardsConverter;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;

public class TestDateTimeConversion extends BaseJNATestClass {

	private String m_timeFormatStr;
	private String m_dateFormatStr;

	@Test
	public void testInvalidInnards() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				NotesDatabase db = getFakeNamesDb();
				int[] invalidInnards = new int[] {5767168, 83886086};
				NotesTimeDate invalidTimeDate = new NotesTimeDate(invalidInnards);
				Assert.assertEquals("Return empty string for NotesTimeDate.toString() of invalid timedate", "", invalidTimeDate.toString());

				Calendar calCAPI = InnardsConverter.decodeInnardsWithCAPI(invalidInnards);
				Assert.assertNull("Domino returns an error converting the innards", calCAPI);
				
				Calendar calManual = InnardsConverter.decodeInnardsToCalendar(invalidInnards);
				Assert.assertNotNull("Manual code is able to convert the innards", calManual); // conversion returns Mon Jan 08 17:01:11 CET 4713

				NotesNote note = db.createNote();
				note.replaceItemValue("Form", "Person");
				note.replaceItemValue("Type", "Person");
				note.replaceItemValue("Lastname", "1. Invalid date");
				note.replaceItemValue("Firstname", "Test");
				note.replaceItemValue("DateValue", invalidTimeDate);
				note.update();

				String unid = note.getUNID();
				note.recycle();

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

	//commented out, should be run manually, since it's taking forever to complete
//	@Test
	public void testManualInnardEncodingAndDecoding() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				session.setTrackMillisecInJavaDates(true);

				NotesDatabase db = getFakeNamesDb();
				Database dbLegacy = getFakeNamesDbLegacy();

				TimeZone defaultTimeZone = TimeZone.getDefault();
				DateTimeZone defaultDateTimeZone = DateTimeZone.getDefault();
				
				String[] tzIds = TimeZone.getAvailableIDs(); //these are a lot, 617 in my test
				try {
					for (int currYear=2018; currYear<=2018; currYear++) {
						for (int currMonth=1; currMonth < 2; currMonth++) {
							for (int currDay=1; currDay<=30; currDay+=11) {
								for (int tzIdxEnd=tzIds.length-1; tzIdxEnd>=0; tzIdxEnd--) {
									String currEncodingTimeZoneId = tzIds[tzIdxEnd];

									for (int mode=0; mode<3; mode++) {
										boolean hasDate;
										boolean hasTime;

										switch (mode) {
										case 0:
											hasDate = true;
											hasTime = true;
											break;
										case 1:
											hasDate = true;
											hasTime = false;
											break;
										case 2:
											hasDate = false;
											hasTime = true;
											break;
										default:
											throw new IllegalStateException();
										}

										for (int dtIdx=0; dtIdx<4; dtIdx++) {
											int currHour;
											int currMinute;
											int currSecond;
											int currMillis=0;

											if (dtIdx==0) {
												currHour = 8;
												currMinute = 10;
												currSecond = 0;
											}
											else if (dtIdx==1) {
												currHour = 12;
												currMinute = 23;
												currSecond = 50;
											}
											else if (dtIdx==2) {
												currHour = 21;
												currMinute = 35;
												currSecond = 0;
											}
											else {
												currHour = 23;
												currMinute = 59;
												currSecond = 59;
											}

											//switch to a different time zone for innard encoding so that
											//the innard[1] contains this timezone info
											TimeZone currEncodingTZ = TimeZone.getTimeZone(currEncodingTimeZoneId);

											DateTimeZone currEncodingDTZ;
											try {
												currEncodingDTZ = DateTimeZone.forTimeZone(currEncodingTZ);
											}
											catch (IllegalArgumentException e) {
												continue;
											}
											
											DateTimeZone.setDefault(currEncodingDTZ);
											TimeZone.setDefault(currEncodingTZ);
											
											org.joda.time.DateTime jdtExpectedDateTime = new org.joda.time.DateTime(currYear,
													currMonth, currDay, currHour, currMinute, currSecond, currEncodingDTZ);
											long expectedDateInMillis = jdtExpectedDateTime.getMillis();
											
											String expectedDateAsString = jdtExpectedDateTime.toString();
											
											LocalDate expectedLocalDate = jdtExpectedDateTime.toLocalDate();
											String expectedLocalDateAsString = expectedLocalDate.toString();
											LocalTime expectedLocalTime = jdtExpectedDateTime.toLocalTime();
											String expectedLocalTimeAsString = expectedLocalTime.toString();
											

											//encode date with the current encoding timezone
											Calendar calExpected = Calendar.getInstance(currEncodingTZ);
											calExpected.setTimeInMillis(expectedDateInMillis);
											int[] innardsManual = NotesDateTimeUtils.calendarToInnards(calExpected, hasDate, hasTime);

											for (int tzIdxDec=0; tzIdxDec<tzIds.length; tzIdxDec++) {
												String currDecodeTimeZoneId = tzIds[tzIdxDec];
												TimeZone currDecodingTZ = TimeZone.getTimeZone(currDecodeTimeZoneId);
												
												DateTimeZone currDecodingDTZ;
												try {
													currDecodingDTZ = DateTimeZone.forTimeZone(currDecodingTZ);
												}
												catch (IllegalArgumentException e) {
													continue;
												}
												
//												System.out.println("Encoding TZ: "+currEncodingTZ.getID()+", "+currEncodingTZ.getDisplayName()+", decoding TZ: "+currDecodingTZ.getID()+", "+currDecodingTZ.getDisplayName());
//												System.out.println("currYear="+currYear+", currMonth="+currMonth+", currDay="+currDay+", currHour="+currHour+", currMinute="+currMinute+", currSecond="+currSecond);
//												System.out.println("hasDate="+hasDate+", hasTime="+hasTime);
												
												//try decoding the timezones with all
												TimeZone.setDefault(currDecodingTZ);
												DateTimeZone.setDefault(currDecodingDTZ);

												//now check writing the innards to Domino documents using JNA
												//produces the right data when read via legacy API
												if (hasDate) {
													if (hasTime) {
														Date dtLegacy = getDateAndTimeViaLegacyAPI(db, dbLegacy, innardsManual);
														try {
															Assert.assertEquals("Legacy API roundtrip is ok for date/time innards "+
																	Arrays.toString(innardsManual)+", original date "+expectedDateAsString+
																	" === "+dtLegacy+", Encoding TZ: "+currEncodingTimeZoneId+
																	", decoding TZ: "+currDecodeTimeZoneId, jdtExpectedDateTime.getMillis(), dtLegacy.getTime());
														}
														catch (AssertionFailedError e) {
															e.printStackTrace();
															throw e;
														}
														//try to decode innards; should produce the original datetime
														Calendar checkDtManual = InnardsConverter.decodeInnardsToCalendar(innardsManual);
														Assert.assertEquals("Manual Date-Innard-Date roundtrip is ok for innards "+Arrays.toString(innardsManual)+", original date "+expectedDateAsString+
																" === "+checkDtManual.getTime()+", Encoding TZ: "+currEncodingTimeZoneId+", decoding TZ: "+currDecodeTimeZoneId, expectedDateInMillis, checkDtManual.getTimeInMillis());

													}
													else {
														LocalDate localDateLegacy = getDateViaLegacyAPI(db, dbLegacy, innardsManual);
														try {
															Assert.assertEquals("Legacy API roundtrip is ok for date only innards "+
																	Arrays.toString(innardsManual)+", original date "+expectedDateAsString+
																	" === "+localDateLegacy+
																	", Encoding TZ: "+currEncodingTimeZoneId+
																	", decoding TZ: "+currDecodeTimeZoneId, expectedLocalDateAsString, localDateLegacy.toString());
														}
														catch (AssertionFailedError e) {
															e.printStackTrace();
															throw e;
														}
													}
												}
												else {
													if (hasTime) {
														LocalTime localTimeLegacy = getTimeViaLegacyAPI(db, dbLegacy, innardsManual);
														try {
															Assert.assertEquals("Legacy API roundtrip is ok for time only innards "+
																	Arrays.toString(innardsManual)+", original date "+expectedDateAsString+
																	" === "+localTimeLegacy+", Encoding TZ: "+currEncodingTimeZoneId+
																	", decoding TZ: "+currDecodeTimeZoneId, expectedLocalTimeAsString, localTimeLegacy.toString());
														}
														catch (AssertionFailedError e) {
															e.printStackTrace();
															LocalDate localDateLegacy = getDateViaLegacyAPI(db, dbLegacy, innardsManual);
															throw e;
														}
													}
													else {
														//case does not exist
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				finally {
					//restore original timezone
					TimeZone.setDefault(defaultTimeZone);
					DateTimeZone.setDefault(defaultDateTimeZone);
				}
				return null;
			}


			private int[] getInnardsViaLegacyAPI(NotesDatabase db, Database dbLegacy, Calendar cal, boolean hasDate, boolean hasTime) {
				try {
					lotus.domino.DateTime ndt = dbLegacy.getParent().createDateTime(cal);
					if (!hasDate) {
						ndt.setAnyDate();
					}
					if (!hasTime) {
						ndt.setAnyTime();
					}
					
					Document doc = dbLegacy.createDocument();
					doc.replaceItemValue("Form", "Person");
					doc.replaceItemValue("Type", "Person");
					doc.replaceItemValue("Firstname", "1. Datetime conversion date-innards");
					doc.replaceItemValue("Lastname", "1. Datetime conversion date-innards");
					doc.replaceItemValue("Date", ndt);
					doc.save();
					String unid = doc.getUniversalID();
					doc.recycle();
					NotesNote note = db.openNoteByUnid(unid);
					NotesTimeDate td = note.getItemValueAsTimeDate("Date");
					int[] innards = td.getInnards();
					note.delete();
					return innards;
				} catch (NotesException e) {
					throw new NotesError(e.id, e.getLocalizedMessage(), e);
				}
			}

			private int[] getInnardsViaLegacyAPI(NotesDatabase db, Database dbLegacy, LocalDate date) {
				try {
					lotus.domino.DateTime ndt = dbLegacy.getParent().createDateTime(date.toDateTimeAtStartOfDay().toDate());
					ndt.setAnyTime();
					Document doc = dbLegacy.createDocument();
					doc.replaceItemValue("Form", "Person");
					doc.replaceItemValue("Type", "Person");
					doc.replaceItemValue("Firstname", "1. Datetime conversion date-innards");
					doc.replaceItemValue("Lastname", "1. Datetime conversion date-innards");
					doc.replaceItemValue("Date", ndt);
					doc.save();
					String unid = doc.getUniversalID();
					doc.recycle();
					NotesNote note = db.openNoteByUnid(unid);
					NotesTimeDate td = note.getItemValueAsTimeDate("Date");
					int[] innards = td.getInnards();
					note.delete();
					return innards;
				} catch (NotesException e) {
					throw new NotesError(e.id, e.getLocalizedMessage(), e);
				}
			}
			
			private int[] getInnardsViaLegacyAPI(NotesDatabase db, Database dbLegacy, LocalTime time) {
				try {
					lotus.domino.DateTime ndt = dbLegacy.getParent().createDateTime(time.toDateTimeToday().toDate());
					ndt.setAnyDate();
					Document doc = dbLegacy.createDocument();
					doc.replaceItemValue("Form", "Person");
					doc.replaceItemValue("Type", "Person");
					doc.replaceItemValue("Firstname", "1. Datetime conversion date-innards");
					doc.replaceItemValue("Lastname", "1. Datetime conversion date-innards");
					doc.replaceItemValue("Date", ndt);
					doc.save();
					String unid = doc.getUniversalID();
					doc.recycle();
					NotesNote note = db.openNoteByUnid(unid);
					NotesTimeDate td = note.getItemValueAsTimeDate("Date");
					int[] innards = td.getInnards();
					note.delete();
					return innards;
				} catch (NotesException e) {
					throw new NotesError(e.id, e.getLocalizedMessage(), e);
				}
			}
			
			private LocalDate getDateViaLegacyAPI(NotesDatabase db, Database dbLegacy, int[] innards) {
				NotesNote note = db.createNote();
				note.replaceItemValue("Form", "Person");
				note.replaceItemValue("Type", "Person");
				note.replaceItemValue("Firstname", "1. Datetime conversion - innards-date");
				note.replaceItemValue("Lastname", "1. Datetime conversion - innards-date");
				note.replaceItemValue("Date", new NotesTimeDate(innards));
				note.update();
				String unid = note.getUNID();
				note.recycle();

				try {
					Document doc = dbLegacy.getDocumentByUNID(unid);
					Vector<?> values = doc.getItemValue("Date");
					doc.remove(true);
					doc.recycle();

					lotus.domino.DateTime dtLegacy = (DateTime) values.get(0);
					String dateOnly = dtLegacy.getDateOnly();
					LocalDate localDate = parseDateOnly(dbLegacy.getParent(), dateOnly);
					return localDate;
				} catch (NotesException e) {
					throw new NotesError(e.id, e.getLocalizedMessage(), e);
				}
			}

			private Date getDateAndTimeViaLegacyAPI(NotesDatabase db, Database dbLegacy, int[] innards) {
				NotesNote note = db.createNote();
				note.replaceItemValue("Form", "Person");
				note.replaceItemValue("Type", "Person");
				note.replaceItemValue("Firstname", "1. Datetime conversion");
				note.replaceItemValue("Lastname", "1. Datetime conversion");
				note.replaceItemValue("Date", new NotesTimeDate(innards));
				note.update();
				String unid = note.getUNID();
				note.recycle();

				try {
					Document doc = dbLegacy.getDocumentByUNID(unid);
					Vector<?> values = doc.getItemValue("Date");
					doc.remove(true);
					doc.recycle();

					lotus.domino.DateTime dtLegacy = (DateTime) values.get(0);
					Date jdtLegacy = dtLegacy.toJavaDate();
					return jdtLegacy;
				} catch (NotesException e) {
					throw new NotesError(e.id, e.getLocalizedMessage(), e);
				}
			}

			
		});
	}
	

//	@Test
	public void testConv() {

		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				TimeZone currEncodingTZ = TimeZone.getTimeZone("Pacific/Tongatapu");
				DateTimeZone currEncodingDTZ = DateTimeZone.forTimeZone(currEncodingTZ);

				//try decoding the timezones with all
				TimeZone.setDefault(currEncodingTZ);
				DateTimeZone.setDefault(currEncodingDTZ);

				int currHour = 8;
				int currMinute = 10;
				int currSecond = 0;
				int currDay = 1;
				int currMonth = 1;
				int currYear = 2018;
				
				org.joda.time.DateTime jdtExpectedDateTime = new org.joda.time.DateTime(currYear,
						currMonth, currDay, currHour, currMinute, currSecond, currEncodingDTZ);
				long expectedDateInMillis = jdtExpectedDateTime.getMillis();
				Date dt = new Date(expectedDateInMillis);
				
				String expectedDateAsString = jdtExpectedDateTime.toString();
				
				LocalDate expectedLocalDate = jdtExpectedDateTime.toLocalDate();
				String expectedLocalDateAsString = expectedLocalDate.toString();
				LocalTime expectedLocalTime = jdtExpectedDateTime.toLocalTime();
				String expectedLocalTimeAsString = expectedLocalTime.toString();
				

				//encode date with the current encoding timezone
				Calendar calExpected = Calendar.getInstance(currEncodingTZ);
				calExpected.setTimeInMillis(expectedDateInMillis);
				int[] innardsManual = NotesDateTimeUtils.calendarToInnards(calExpected, false, true);

				Calendar calExpected2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				calExpected2.setTimeInMillis(expectedDateInMillis);
				int[] innardsManual2 = NotesDateTimeUtils.calendarToInnards(calExpected2, false, true);

				
				TimeZone currDecodingTZ = TimeZone.getTimeZone("Africa/Abidjan");
				DateTimeZone currDecodingDTZ = DateTimeZone.forTimeZone(currDecodingTZ);

				TimeZone.setDefault(currDecodingTZ);
				DateTimeZone.setDefault(currDecodingDTZ);

				Temporal t = InnardsConverter.decodeInnards(innardsManual);

				Calendar checkCal = NotesDateTimeUtils.innardsToCalendar(innardsManual);
				
				NotesDatabase db = getFakeNamesDb();
				Database dbLegacy = getFakeNamesDbLegacy();
				
				LocalTime time = getTimeViaLegacyAPI(db, dbLegacy, innardsManual);
				System.out.println(time);
//				
//				
////				int[] innards = [143000, -1]
//				NotesDatabase dbData = new NotesDatabase("", "fakenames2018.nsf", "");
////				NotesCollection view = dbData.openCollectionByName("Contacts");
//				NotesNote note = dbData.openNoteByUnid("3E525A7962A51887C12582E800711758");
//				NotesTimeDate td = note.getItemValueAsTimeDate("Birthday");
//				int[] innards = td.getInnards();
//				System.out.println("Innards: "+Arrays.toString(innards));
//				
//				System.out.println("Formatted value:" + td.toString());
//				
//				Calendar cal = td.toCalendar();
//				System.out.println("Calendar.toString(): "+cal);
//				
//				int month = cal.get(Calendar.MONTH);
//				int day = cal.get(Calendar.DAY_OF_MONTH);
//				int year = cal.get(Calendar.YEAR);
//				System.out.println("day: "+day+", month: "+month+", year: "+year);
//				
//				Date date = td.toDate();
//				System.out.println(date);
				
				return null;
			}
		});
	
	}
	
	private LocalTime getTimeViaLegacyAPI(NotesDatabase db, Database dbLegacy, int[] innards) {
		NotesNote note = db.createNote();
		note.replaceItemValue("Form", "Person");
		note.replaceItemValue("Type", "Person");
		note.replaceItemValue("Firstname", "1. Datetime conversion");
		note.replaceItemValue("Lastname", "1. Datetime conversion");
		note.replaceItemValue("Date", new NotesTimeDate(innards));
		note.update();
		String unid = note.getUNID();
		note.recycle();

		try {
			Document doc = dbLegacy.getDocumentByUNID(unid);
			Vector<?> values = doc.getItemValue("Date");
			doc.remove(true);
			doc.recycle();

			lotus.domino.DateTime dtLegacy = (DateTime) values.get(0);
			Date jdtLegacy = dtLegacy.toJavaDate();
			String timeOnly = dtLegacy.getTimeOnly();
			int millis = (int) (jdtLegacy.getTime() - 1000*(jdtLegacy.getTime() / 1000));

			LocalTime localTime = parseTimeOnly(dbLegacy.getParent(), timeOnly).withMillisOfSecond(millis);
			return localTime;
		} catch (NotesException e) {
			throw new NotesError(e.id, e.getLocalizedMessage(), e);
		}
	}
	
	private LocalDate parseDateOnly(Session session, String dateOnly) {
		String formatStr = getDateFormatString(session);
		DateTimeFormatter formatter = DateTimeFormat.forPattern(formatStr);
		LocalDate localDate = formatter.parseLocalDate(dateOnly);
		return localDate;
	}

	private LocalTime parseTimeOnly(Session session, String timeOnly) {
		String formatStr = getTimeFormatString(session);
		DateTimeFormatter formatter = DateTimeFormat.forPattern(formatStr);
		LocalTime localTime = formatter.parseLocalTime(timeOnly);
		return localTime;
	}

	private String getTimeFormatString(Session session) {
		if (m_timeFormatStr==null) {
			try {
				Vector<?> tmp = session.evaluate("@Text(@Time(11;22;33))");
				String timeFormatted = tmp.get(0).toString();
				if (timeFormatted.contains("AM") || timeFormatted.contains("PM")) {
					m_timeFormatStr = timeFormatted.replace("AM", "a").replace("PM", "a").replace("11", "hh"); // 1-12 AM/PM
				}
				else {
					m_timeFormatStr = timeFormatted.replace("11", "HH"); // 24 hours
				}
				m_timeFormatStr = m_timeFormatStr.replace("22", "mm").replace("33", "ss");
			} catch (NotesException e) {
				throw new NotesError(e.id, e.getLocalizedMessage());
			}
		}
		return m_timeFormatStr;
	}

	private String getDateFormatString(Session session) {
		if (m_dateFormatStr==null) {
			try {
				Vector<?> tmp = session.evaluate("@Text(@Date(2006;4;5))");
				m_dateFormatStr = tmp.get(0).toString()
						.replace("2006", "YYYY")
						.replace("06", "YY")
						.replace("04", "MM")
						.replace("4", "M")
						.replace("05", "dd")
						.replace("5", "d");
			} catch (NotesException e) {
				throw new NotesError(e.id, e.getLocalizedMessage());
			}
		}
		return m_dateFormatStr;
	}

}
