package com.mindoo.domino.jna.utils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.internal.InnardsConverter;
import com.mindoo.domino.jna.internal.NotesConstants;

/**
 * DateTime conversion utilities between Java and the Notes C API
 * 
 * @author Karsten Lehmann
 */
public class NotesDateTimeUtils {

	/**
	 * Returns whether the current timezone is in daylight savings time
	 * 
	 * @return true if DST
	 */
	public static boolean isDaylightTime() {
		TimeZone tz = TimeZone.getDefault();
		
	    return tz.useDaylightTime();
	}

	/**
	 * Returns the current timezone's GMT offset
	 * 
	 * @return offset
	 */
	public static int getGMTOffset() {
		TimeZone tz = TimeZone.getDefault();
		
		return (int)(tz.getRawOffset() / 3600000);
	}

	/**
	 * Method to convert a {@link NotesTimeDate} object to a Java {@link Calendar}
	 * 
	 * @param timeDate time date to convert
	 * @return calendar or null if timedate contains invalid innards
	 */
	public static Calendar timeDateToCalendar(NotesTimeDate timeDate) {
		return timeDate.toCalendar();
	}

	/**
	 * Method to check whether year, month and date fields are set
	 * 
	 * @param cal calendar to check
	 * @return true if we have a date
	 */
	public static boolean hasDate(Calendar cal) {
		boolean hasDate = cal.isSet(Calendar.YEAR) && cal.isSet(Calendar.MONTH) && cal.isSet(Calendar.DATE);
		return hasDate;
	}
	
	/**
	 * Method to check whether hour, minute, second and millisecond fields are set
	 * 
	 * @param cal calendar to check
	 * @return true if we have a time
	 */
	public static boolean hasTime(Calendar cal) {
		boolean hasTime = cal.isSet(Calendar.HOUR_OF_DAY) && cal.isSet(Calendar.MINUTE) &&
				cal.isSet(Calendar.SECOND) && cal.isSet(Calendar.MILLISECOND);
		return hasTime;
	}
	
	/**
	 * Method to convert a {@link Calendar} to a {@link NotesTimeDate}
	 * 
	 * @param cal calendar
	 * @return timedate
	 */
	public static NotesTimeDate calendarToTimeDate(Calendar cal) {
		boolean hasDate = hasDate(cal);
		boolean hasTime = hasTime(cal);
		
		return calendarToTimeDate(cal, hasDate, hasTime);
	}

	/**
	 * Clears the hour, minute, second and millisecond fields of a {@link Calendar} object
	 * 
	 * @param cal calendar
	 */
	public static void setAnyTime(Calendar cal) {
		// set date only
		// clear time fields
		// clear hour of the day
		cal.clear(Calendar.HOUR_OF_DAY);

		// clear minute
		cal.clear(Calendar.MINUTE);

		// clear second
		cal.clear(Calendar.SECOND);

		// clear millisecond
		cal.clear(Calendar.MILLISECOND);
	}

	/**
	 * Clears the year, month and date fields of a {@link Calendar} object
	 * 
	 * @param cal calendar
	 */
	public static void setAnyDate(Calendar cal) {
		// clear date fields
		// clear year
		cal.clear(Calendar.YEAR);

		// clear month
		cal.clear(Calendar.MONTH);

		// clear day
		cal.clear(Calendar.DATE);
	}
	
	/**
	 * Method to convert a {@link Calendar} to a {@link NotesTimeDate}
	 * 
	 * @param cal calendar
	 * @param hasDate true to convert the date
	 * @param hasTime true to convert the time
	 * @return timedate
	 */
	public static NotesTimeDate calendarToTimeDate(Calendar cal, boolean hasDate, boolean hasTime) {
		int[] innards = calendarToInnards(cal, hasDate, hasTime);
		return new NotesTimeDate(new int[] {innards[0], innards[1]});
	}
	
	/**
	 * Method to convert a {@link Date} to a {@link NotesTimeDate}
	 * 
	 * @param dt date
	 * @param hasDate true to convert the date
	 * @param hasTime true to convert the time
	 * @return timedate
	 */
	public static NotesTimeDate dateToTimeDate(Date dt, boolean hasDate, boolean hasTime) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dt);
		
		int[] innards = calendarToInnards(cal, hasDate, hasTime);
		return new NotesTimeDate(new int[] {innards[0], innards[1]});
	}
	
	/**
	 * Method to convert a {@link Date} to a {@link NotesTimeDate}
	 * 
	 * @param dt date
	 * @return timedate
	 */
	public static NotesTimeDate dateToTimeDate(Date dt) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dt);
		
		int[] innards = calendarToInnards(cal, true, true);
		return new NotesTimeDate(new int[] {innards[0], innards[1]});
	}
	
	/**
	 * Method to convert a {@link Date} object to an innard array
	 * 
	 * @param dt date
	 * @return innard array
	 */
	public static int[] dateToInnards(Date dt) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dt);
		return calendarToInnards(cal);
	}
	
	/**
	 * Method to convert a {@link Date} object to an innard array
	 * 
	 * @param dt date
	 * @param hasDate true to convert the date
	 * @param hasTime true to convert the time
	 * @return innard array
	 */
	public static int[] dateToInnards(Date dt, boolean hasDate, boolean hasTime) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dt);
		return calendarToInnards(cal, hasDate, hasTime);
	}
	
	/**
	 * Method to convert a {@link Calendar} object to an innard array
	 * 
	 * @param cal calendar
	 * @return innard array
	 */
	public static int[] calendarToInnards(Calendar cal) {
		boolean hasDate = hasDate(cal);
		boolean hasTime = hasTime(cal);
	
		return calendarToInnards(cal, hasDate, hasTime);
	}
	
	/**
	 * Method to convert a {@link Calendar} object to an innard array
	 * 
	 * @param cal calendar
	 * @param hasDate true to convert the date
	 * @param hasTime true to convert the time
	 * @return innard array
	 */
	public static int[] calendarToInnards(Calendar cal, boolean hasDate, boolean hasTime) {
		return InnardsConverter.encodeInnards(cal, hasDate, hasTime);
	}
	
	/**
	 * Method to compare two date/time values and check whether the first is after the second
	 * 
	 * @param innards1 first date/time
	 * @param innards2 second date/time
	 * @return true if after
	 */
	public static boolean isAfter(int[] innards1, int[] innards2) {
		return compareInnards(innards1, innards2) > 0;
	}

	/**
	 * Method to compare two date/time values and check whether the first is before the second
	 * 
	 * @param innards1 first date/time
	 * @param innards2 second date/time
	 * @return true if before
	 */
	public static boolean isBefore(int[] innards1, int[] innards2) {
		return compareInnards(innards1, innards2) < 0;
	}

	/**
	 * Method to compare two date/time values and check whether both are equal
	 * 
	 * @param innards1 first date/time
	 * @param innards2 second date/time
	 * @return true if equal
	 */
	public static boolean isEqual(int[] innards1, int[] innards2) {
		return compareInnards(innards1, innards2) == 0;
	}

	/**
	 * Compares two date/time values and returns -1, if the first value is before
	 * the second, 1 if the first value is after the second and 0 if both values are
	 * equal.
	 * 
	 * @param innards1 first date/time
	 * @param innards2 second date/time
	 * @return compare result
	 */
	public static int compareInnards(int[] innards1, int[] innards2) {
		if (!hasDate(innards1)) {
			throw new IllegalArgumentException("Innard array #1 does not have a date part: "+Arrays.toString(innards1));
		}
		if (!hasDate(innards2)) {
			throw new IllegalArgumentException("Innard array #1 does not have a date part: "+Arrays.toString(innards2));
		}
		if (!hasTime(innards1)) {
			throw new IllegalArgumentException("Innard array #1 does not have a time part: "+Arrays.toString(innards1));
		}
		if (!hasTime(innards2)) {
			throw new IllegalArgumentException("Innard array #1 does not have a time part: "+Arrays.toString(innards2));
		}
		
		//compare date part
		if (innards1[1] > innards2[1]) {
			return 1;
		}
		else if (innards1[1] < innards2[1]) {
			return -1;
		}
		else {
			//compare time part
			if (innards1[0] > innards2[0]) {
				return 1;
			}
			else if (innards1[0] < innards2[0]){
				return -1;
			}
			else {
				return 0;
			}
		}
	}
	
	/**
	 * Method to check whether a date/time represented as an innard array has
	 * a time part
	 * 
	 * @param innards innards
	 * @return true if it has time
	 */
	public static boolean hasTime(int[] innards) {
		if (innards.length!=2)
			throw new IllegalArgumentException("Invalid innard size: "+innards.length+", expected 2");
		return (innards[0]!=NotesConstants.ALLDAY);
	}
	
	/**
	 * Method to check whether a date/time represented as an innard array has
	 * a date part
	 * 
	 * @param innards innards
	 * @return true if it has date
	 */
	public static boolean hasDate(int[] innards) {
		if (innards.length!=2)
			throw new IllegalArgumentException("Invalid innard size: "+innards.length+", expected 2");
		return (innards[1]!=NotesConstants.ANYDAY);
	}
	
	/**
	 * Converts C API innard values to Java {@link Calendar}
	 * 
	 * @param innards array with 2 innard values
	 * @return calendar or null if invalid innards
	 */
	public static Calendar innardsToCalendar(int[] innards) {
		return InnardsConverter.decodeInnards(innards);
	}

}
