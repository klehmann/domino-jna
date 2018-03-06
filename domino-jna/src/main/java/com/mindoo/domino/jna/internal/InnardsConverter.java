package com.mindoo.domino.jna.internal;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.internal.structs.NotesTimeStruct;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;

/**
 * Utility class to convert date/time values between the Domino innard array and Java {@link Calendar}.<br>
 * The Domino date format is documented in the C API:<br>
 * <hr>
 * <b>TIMEDATE</b><br>
 * <br>
 * <br>
 * The Domino and Notes TIMEDATE structure consists of two long words that encode the time, the date,
 * the time zone, and the Daylight Savings Time settings that were in effect when the structure was initialized.<br>
 * <br>
 * The TIMEDATE structure is designed to be accessed exclusively through the time and date subroutines defined in misc.h.<br>
 * <br>
 * This structure is subject to change; the description here is provided for debugging purposes.<br>
 * <br>
 * The first DWORD, Innards[0], contains the number of hundredths of seconds since midnight, Greenwich mean time.<br>
 * <br>
 * If only the date is important, not the time, this field may be set to ALLDAY.<br>
 * <br>
 * The date and the time zone and Daylight Savings Time settings are encoded in Innards[1].<br>
 * <br>
 * The 24 low-order bits contain the Julian Day, the number of days since January 1, 4713 BC.<br>
 * <br>
 * Note that this is NOT the same as the Julian calendar!<br>
 * The Julian Day was originally devised as an aid to astronomers.<br>
 * Since only days are counted, weeks, months, and years are ignored in calculations.<br>
 * The Julian Day is defined to begin at noon;  for simplicity, Domino and Notes assume that the day begins at midnight.<br>
 * <br>
 * The high-order byte, bits 31-24, encodes the time zone and Daylight Savings Time information.<br>
 * The high-order bit, bit 31 (0x80000000), is set if Daylight Savings Time is observed.<br>
 * Bit 30 (0x40000000) is set if the time zone is east of Greenwich mean time.<br>
 * Bits 27-24 contain the number of hours difference between the time zone and Greenwich mean time, and bits
 * 29-28 contain the number of 15-minute intervals in the difference.<br>
 * <br>
 * For example, 2:49:04 P. M., Eastern Standard Time, December 10, 1996 would be stored as:<br>
 * <br>
 * Innards[0]:	0x006CDCC0	19 hours, 49 minutes, 4 seconds GMT<br>
 * Innards[1]:	0x852563FC	DST observed, zone +5, Julian Day  2,450,428<br>
 * <br>
 * If the time zone were set for Bombay, India, where Daylight Savings Time is not observed,
 * 2:49:04 P. M., December 10, 1996 would be stored as:<br>
 * <br>
 * Innards[0]:	0x0032B864	9 hours, 19 minutes, 4 seconds GMT<br>
 * Innards[1]:	0x652563FC	No DST, zone 5 1/2 hours east of GMT, Julian Day  2,450,428<br>
 * <hr>
 * Here is more documentation regarding date formats in Domino:<br>
 * <a href="http://www-01.ibm.com/support/docview.wss?uid=swg27003019">How to interpret the Hexadecimal values in a Time/Date value</a><br>
 * <br>
 * And here is an interesting technote regarding specific dates in the year 1752:<br>
 * <a href="http://www-01.ibm.com/support/docview.wss?uid=swg21098816">Error: 'Unable to interpret time or date' when entering specific dates from the year 1752</a>
 * 
 * @author Karsten Lehmann
 */
public class InnardsConverter {

	/**
	 * Converts a given date to Julian date in days
	 * 
	 * @param dt date
	 * @return days since 1/1/4713 BC
	 */
	private static long toJulianDay(Date dt) {
		// convert milliseconds since 1/1/1970 to days, add the timezone offset
		// in days
		// and add 2440587.5, which are the days between 1/1/4713 BC and
		// 1/1/1970
		return Math.round((dt.getTime() / 86400000) - (dt.getTimezoneOffset() / 1440) + 2440587.5);
	}

	private static long fromJulianDay(long julianDay, int timezoneOffset) {
		return (long) ((double) julianDay + (timezoneOffset / 1440) - 2440587.5d) * 86400000;
	}

	private static String toBinary(long l) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < Long.numberOfLeadingZeros((long)l); i++) {
		      sb.append('0');
		}
		sb.append(Long.toBinaryString((long)l));
		return sb.toString();
	}

	/**
	 * Method to convert a {@link Calendar} object to an innard array. This implementation
	 * uses the C API.
	 * 
	 * @param cal calendar
	 * @return innard array
	 */
	public static int[] encodeInnardsWithCAPI(Calendar cal) {
		boolean hasDate = NotesDateTimeUtils.hasDate(cal);
		boolean hasTime = NotesDateTimeUtils.hasTime(cal);
		return encodeInnardsWithCAPI(cal, hasDate, hasTime);
	}
	
	/**
	 * Method to convert a {@link Calendar} object to an innard array. This implementation
	 * uses the C API.
	 * 
	 * @param cal calendar
	 * @param hasDate true to create an innard with a date part
	 * @param hasTime true to create an innard with a time part
	 * @return innard array
	 */
	public static int[] encodeInnardsWithCAPI(Calendar cal, boolean hasDate, boolean hasTime) {
		Calendar calUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calUTC.setTimeInMillis(cal.getTimeInMillis());
		
		int year = calUTC.get(Calendar.YEAR);
		int month = calUTC.get(Calendar.MONTH)+1;
		int day = calUTC.get(Calendar.DATE);

		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		int millis = cal.get(Calendar.MILLISECOND);

		hour = calUTC.get(Calendar.HOUR_OF_DAY);
		minute = calUTC.get(Calendar.MINUTE);
		second = calUTC.get(Calendar.SECOND);
		millis = calUTC.get(Calendar.MILLISECOND);

		DisposableMemory m = new DisposableMemory(NotesConstants.timeSize);
		NotesTimeStruct time = NotesTimeStruct.newInstance(m);

		time.dst=0;//isDST ? 1 : 0;
		time.zone=0; //-gmtOffset;

		time.hour = hour;
		time.minute = minute;
		time.second = second;
		time.hundredth = (short) (millis / 10);

		time.year = year;
		time.month = month;
		time.day = day;
		time.write();

		//convert day, month, year etc. to GM NotesTimeDate
		boolean convRet = NotesNativeAPI.get().TimeLocalToGM(m);
		if (convRet) {
			String msg = "Error converting calendar value to GM: "+cal.getTime();
			throw new NotesError(0, msg);
		}
		time.read();

		int[] innards = time.GM.Innards.clone();
		m.dispose();
		
		if (!hasDate) {
			innards[1] = NotesConstants.ANYDAY;
		}
		if (!hasTime) {
			innards[0] = NotesConstants.ALLDAY;
		}
		return innards;
	}

	/**
	 * Method to convert a {@link Calendar} object to an innard array. This implementation
	 * uses pure Java functions for the conversion, which is faster than using JNA.
	 * 
	 * @param cal calendar
	 * @return innard array
	 */
	public static int[] encodeInnards(Calendar cal) {
		boolean hasDate = NotesDateTimeUtils.hasDate(cal);
		boolean hasTime = NotesDateTimeUtils.hasTime(cal);
		return encodeInnards(cal, hasDate, hasTime);
	}
	
	/**
	 * Method to convert a {@link Calendar} object to an innard array. This implementation
	 * uses pure Java functions for the conversion, which is faster than using JNA.
	 * 
	 * @param cal calendar
	 * @param hasDate true to create an innard with a date part
	 * @param hasTime true to create an innard with a time part
	 * @return innard array
	 */
	public static int[] encodeInnards(Calendar cal, boolean hasDate, boolean hasTime) {
		TimeZone tz = Calendar.getInstance().getTimeZone();

		int[] innards = new int[2];

		//The first DWORD, Innards[0], contains the number of hundredths of seconds since midnight,
		long dtTimeMillisSince1970 = cal.getTimeInMillis();
		long dtTimeDaysSince1970 = dtTimeMillisSince1970 / (24*60*60*1000);
		long dtTimeMillisSince1970StartOfDay = dtTimeDaysSince1970 * 24*60*60*1000;
		if (hasTime) {
			innards[0] = (int) (((dtTimeMillisSince1970-dtTimeMillisSince1970StartOfDay) / 10) & 0xffffffff);
		}
		else {
			innards[0] = NotesConstants.ALLDAY;
		}

		//The 24 low-order bits contain the Julian Day, the number of days since January 1, 4713 BC
		Date dtTime = cal.getTime();
		long julianDay = toJulianDay(dtTime);

		long zoneMask = 0;

//		The high-order bit, bit 31 (0x80000000), is set if Daylight Savings Time is observed
		if (tz.useDaylightTime()) {
			zoneMask |= 1l << 31;
		}
		
//		Bit 30 (0x40000000) is set if the time zone is east of Greenwich mean time.
		if (tz.getRawOffset()>0) {
			zoneMask |= 1l << 30;
		}
		
		int tzOffset = dtTime.getTimezoneOffset();
		int tzOffsetHours = Math.abs(tzOffset / 60);
		
		//Bits 27-24 contain the number of hours difference between the time zone and Greenwich mean time
		zoneMask |= ((long)tzOffsetHours) << 24;

		//bits 29-28 contain the number of 15-minute intervals in the difference
		
		int tzOffsetFractionMinutes = tzOffset % 60;
		int tzOffsetFraction15MinuteIntervalls = tzOffsetFractionMinutes / 15;
		zoneMask |= ((long)tzOffsetFraction15MinuteIntervalls) << 28;

		long resultLong = julianDay | zoneMask;
		
		if (hasDate) {
			innards[1] = (int) (resultLong & 0xffffffff);
		}
		else {
			innards[1] = NotesConstants.ANYDAY;
		}
		return innards;
	}

	/**
	 * Converts C API innard values to Java {@link Calendar}. This implementation
	 * uses the C API internally.
	 * 
	 * @param innards array with 2 innard values
	 * @return calendar or null if invalid innards
	 */
	public static Calendar decodeInnardsWithCAPI(int[] innards) {
		if (innards==null || innards.length<2 || (innards.length>=2 && innards[0]==0 && innards[1]==0))
			return null;

		boolean hasTime=(innards[0]!=NotesConstants.ALLDAY);
		boolean hasDate=(innards[1]!=NotesConstants.ANYDAY);
		
		if (!hasDate && !hasTime)
			return null;
		
		NotesTimeStruct time = NotesTimeStruct.newInstance();
		time.GM.Innards[0] = innards[0];
		time.GM.Innards[1] = innards[1];

		//set desired daylight-saving time to appropriate value, here UTC
		time.dst=0;
		// set desired time zone to appropriate value, here UTC
		time.zone=0;

		boolean convRet;
		if (hasDate && hasTime) {
			convRet = NotesNativeAPI.get().TimeGMToLocalZone(time);
		}
		else {
			convRet = NotesNativeAPI.get().TimeGMToLocal(time);
		}

		if (convRet) {
			//C API doc says:
			//Returns FALSE if successful and TRUE if not.
			
			//not throwing an exception here anymore, but returning null, because legacy Domino API
			//also ignores the value silently when reading these innards from a document
			return null;
		}

		int year = time.year;
		int month = time.month-1;
		int date = time.day;
		int weekday = time.weekday;
		int hour = time.hour;
		int minute = time.minute;
		int second = time.second;
		int millisecond = time.hundredth * 10;

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

		if (hasTime && hasDate) {
			// set date and time
			cal.set((int) year,(int) month,(int) date,(int) hour,(int) minute,(int) second);
		}
		else if (!hasTime) {
			// set date only
			NotesDateTimeUtils.setAnyTime(cal);

			// set date
			cal.set((int) year,(int) month,(int) date);
		}
		else if (!hasDate) {
			// set time only
			NotesDateTimeUtils.setAnyTime(cal);

			// set hour of the day
			cal.set(Calendar.HOUR, (int) hour);

			// set minute
			cal.set(Calendar.MINUTE, (int) minute);

			// set second
			cal.set(Calendar.SECOND, (int) second);
		}

		if (hasTime) {
			// set milliseconds
			cal.set(Calendar.MILLISECOND, (int) millisecond);
		}                

		return cal;
	}
	
	public static boolean isDST(int[] innards) {
		int dateInnard = innards[1];

		//The high-order bit, bit 31 (0x80000000), is set if Daylight Savings Time is observed
		if (((dateInnard >> 31) & 1) == 1) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public static int getTimeZoneOffset(int[] innards) {
		int dateInnard = innards[1];
		
		//Bit 30 (0x40000000) is set if the time zone is east of Greenwich mean time
		int factor;
		if (((dateInnard >> 30) & 1) == 1) {
			factor = 1;
		}
		else {
			factor = -1;
		}
		
		//Bits 27-24 contain the number of hours difference between the time zone and Greenwich mean
		//time, and bits 29-28 contain the number of 15-minute intervals in the difference.
		int hours = ((dateInnard >> 24) & 15);
		int intervals15min = ((dateInnard >> 28) & 3);
		return factor * (hours*60 + intervals15min*15);
	}
	
	/**
	 * Converts C API innard values to Java {@link Calendar}. This implementation
	 * uses pure Java functions for the conversion, which is faster than using JNA.
	 * 
	 * @param innards array with 2 innard values
	 * @return calendar or null if invalid innards
	 */
	public static Calendar decodeInnards(int[] innards) {
		if (innards==null || innards.length<2 || (innards.length>=2 && innards[0]==0 && innards[1]==0))
			return null;
		
		boolean hasTime=(innards[0]!=NotesConstants.ALLDAY);
		boolean hasDate=(innards[1]!=NotesConstants.ANYDAY);

		if (!hasDate && !hasTime)
			return null;

		//The Domino and Notes TIMEDATE structure consists of two long words that encode the time, the date,
		//the time zone, and the Daylight Savings Time settings that were in effect when the structure was initialized.
		//The TIMEDATE structure is designed to be accessed exclusively through the time and date subroutines
		//defined in misc.h.  This structure is subject to change;  the description here is provided for debugging purposes.
		//
		//The first DWORD, Innards[0], contains the number of hundredths of seconds since midnight,
		//Greenwich mean time.  If only the date is important, not the time, this field may be set to ALLDAY.
		//
		//The date and the time zone and Daylight Savings Time settings are encoded in Innards[1].
		//		
		//The 24 low-order bits contain the Julian Day, the number of days since January 1, 4713 BC.
		//		
		//Note that this is NOT the same as the Julian calendar!  The Julian Day was originally devised as an aid
		//to astronomers.  Since only days are counted, weeks, months, and years are ignored in calculations.
		//The Julian Day is defined to begin at noon;  for simplicity, Domino and Notes assume that the day
		//begins at midnight.  The high-order byte, bits 31-24, encodes the time zone and Daylight Savings
		//Time information.
		//The high-order bit, bit 31 (0x80000000), is set if Daylight Savings Time is observed.
		//Bit 30 (0x40000000) is set if the time zone is east of Greenwich mean time.
		//Bits 27-24 contain the number of hours difference between the time zone and Greenwich mean
		//time, and bits 29-28 contain the number of 15-minute intervals in the difference.

		int timeInnard = innards[0];
		int dateInnard = innards[1];
		long hundredSecondsSinceMidnight = timeInnard;
		long milliSecondsSinceMidnight;
		if (hasTime) {
			milliSecondsSinceMidnight = hundredSecondsSinceMidnight * 10;
		}
		else {
			milliSecondsSinceMidnight = 0;
		}
		
		long julianDayLong = dateInnard & 16777215;
		int julianDay = (int) (julianDayLong & 0xffffffff);
		long baseTime;
		if (hasDate) {
			baseTime = fromJulianDay(julianDay, 0);
		}
		else {
			baseTime = System.currentTimeMillis();
			baseTime = baseTime - (baseTime % (24*60*60*1000));
		}
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTimeInMillis(baseTime + milliSecondsSinceMidnight);
		
		if (hasTime) {
			return cal;
		}
		
		Calendar resultCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		
		// set date only
		NotesDateTimeUtils.setAnyTime(resultCal);

		// set date
		resultCal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));

		return resultCal;
	}
}
