package com.mindoo.domino.jna.utils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeStruct;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ShortByReference;

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
	 * @param useDayLight true to use daylight savings time
	 * @param gmtOffset GMT offset
	 * @param timeDate time date to convert
	 * @return calendar or null if timedate contains invalid innards
	 */
	public static Calendar timeDateToCalendar(boolean useDayLight, int gmtOffset, NotesTimeDate timeDate) {
		NotesTimeDateStruct struct = timeDate.getAdapter(NotesTimeDateStruct.class);
		return innardsToCalendar(useDayLight, gmtOffset, struct.Innards);
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
		int gmtOffset = getGMTOffset();
		boolean isDST = isDaylightTime();

		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+1;
		int day = cal.get(Calendar.DATE);

		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		int millis = cal.get(Calendar.MILLISECOND);

		Memory m = new Memory(NotesConstants.timeSize);
		NotesTimeStruct time = NotesTimeStruct.newInstance(m);

		time.dst=isDST ? 1 : 0;
		time.zone=-gmtOffset;

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

		int[] innards = time.GM.Innards;

		if (!hasDate) {
			innards[1] = NotesConstants.ANYDAY;
		}
		if (!hasTime) {
			innards[0] = NotesConstants.ALLDAY;
		}
		return innards;
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
	 * @param useDayLight true to use daylight savings time for the calendar object
	 * @param gmtOffset GMT offset for the calendar object
	 * @param innards array with 2 innard values
	 * @return calendar or null if invalid innards
	 */
	public static Calendar innardsToCalendar(boolean useDayLight, int gmtOffset, int[] innards) {
		if (innards==null || innards.length<2 || (innards.length>=2 && innards[0]==0 && innards[1]==0))
			return null;

		NotesTimeStruct time = NotesTimeStruct.newInstance();
		return innardsToCalendar(useDayLight, gmtOffset, innards, time);
	}
	
	/**
	 * Converts C API innard values to Java {@link Calendar}
	 * 
	 * @param useDayLight true to use daylight savings time for the calendar object
	 * @param gmtOffset GMT offset for the calendar object
	 * @param innards array with 2 innard values
	 * @param time time structure to be used for conversion; used for performance optimization to reuse the same instance
	 * @return calendar or null if invalid innards
	 */
	public static Calendar innardsToCalendar(boolean useDayLight, int gmtOffset, int[] innards, NotesTimeStruct time) {
		if (innards==null || innards.length<2 || (innards.length>=2 && innards[0]==0 && innards[1]==0))
			return null;

		boolean hasTime=(innards[0]!=NotesConstants.ALLDAY);
		boolean hasDate=(innards[1]!=NotesConstants.ANYDAY);
		time.GM.Innards[0] = innards[0];
		time.GM.Innards[1] = innards[1];

		//set desired daylight-saving time to appropriate value -> Calendar.getInstance().useDaylightTime()
		time.dst=(useDayLight) ? 1 : 0; 
		// set desired time zone to appropriate value -> Calendar.getInstance().getTimeZone().getRawOffset()
		time.zone=-gmtOffset;

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

		Calendar cal = Calendar.getInstance();

		if (hasTime && hasDate) {
			// set date and time
			cal.set((int) year,(int) month,(int) date,(int) hour,(int) minute,(int) second);
		}
		else if (!hasTime) {
			// set date only
			setAnyTime(cal);

			// set date
			cal.set((int) year,(int) month,(int) date);
		}
		else if (!hasDate) {
			// set time only
			setAnyTime(cal);

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
	
	/**
	 * Method to clear the {@link NotesTimeDate} value
	 * 
	 * @param timeDate value to be changed
	 */
	public static void setMinimum(NotesTimeDate timeDate) {
		NotesTimeDateStruct struct = timeDate.getAdapter(NotesTimeDateStruct.class);
		NotesNativeAPI.get().TimeConstant(NotesConstants.TIMEDATE_MINIMUM, struct);
		struct.read();
	}
	
	/**
	 * Method to set the {@link NotesTimeDate} value to the maximum value.
	 * 
	 * @param timeDate value to be changed
	 */
	public static void setMaximum(NotesTimeDate timeDate) {
		NotesTimeDateStruct struct = timeDate.getAdapter(NotesTimeDateStruct.class);
		NotesNativeAPI.get().TimeConstant(NotesConstants.TIMEDATE_MAXIMUM, struct);
		struct.read();
	}
	
	/**
	 * Method to set the {@link NotesTimeDate} value to ANYDAY/ALLDAY
	 * 
	 * @param timeDate value to be changed
	 */
	public static void setWildcard(NotesTimeDate timeDate) {
		NotesTimeDateStruct struct = timeDate.getAdapter(NotesTimeDateStruct.class);
		NotesNativeAPI.get().TimeConstant(NotesConstants.TIMEDATE_WILDCARD, struct);
		struct.read();
	}
	
	/**
	 * Converts a {@link NotesTimeDate} to string
	 * 
	 * @param td timedate
	 * @return string with formatted timedate
	 */
	public static String toString(NotesTimeDate td) {
		NotesTimeDateStruct struct = td.getAdapter(NotesTimeDateStruct.class);
		if (struct==null)
			throw new IllegalArgumentException("Missing native data object");
		
		if (struct.Innards==null || struct.Innards.length<2)
			return "";
		if (struct.Innards[0]==0 && struct.Innards[1]==0)
			return "MINIMUM";
		if (struct.Innards[0]==0 && struct.Innards[1]==0xffffff)
			return "MAXIMUM";
		
		DisposableMemory retTextBuffer = new DisposableMemory(100);
		
		ShortByReference retTextLength = new ShortByReference();
		short result = NotesNativeAPI.get().ConvertTIMEDATEToText(null, null, struct, retTextBuffer, (short) retTextBuffer.size(), retTextLength);
		if (result==1037) { // "Invalid Time or Date Encountered", return empty string like Notes UI does
			return "";
		}
		NotesErrorUtils.checkResult(result);

		if (retTextLength.getValue() > retTextBuffer.size()) {
			retTextBuffer.dispose();
			retTextBuffer = new DisposableMemory(retTextLength.getValue());

			result = NotesNativeAPI.get().ConvertTIMEDATEToText(null, null, struct, retTextBuffer, (short) retTextBuffer.size(), retTextLength);
			NotesErrorUtils.checkResult(result);
		}

		String txt = NotesStringUtils.fromLMBCS(retTextBuffer, retTextLength.getValue());
		retTextBuffer.dispose();
		return txt;
	}
	
	/**
	 * Parses a timedate string to a {@link NotesTimeDate}
	 * 
	 * @param dateTimeStr timedate string
	 * @return timedate
	 */
	public static NotesTimeDate fromString(String dateTimeStr) {
		Memory dateTimeStrLMBCS = NotesStringUtils.toLMBCS(dateTimeStr, true);
		//convert method expects a pointer to the date string in memory
		Memory dateTimeStrLMBCSPtr = new Memory(Pointer.SIZE);
		dateTimeStrLMBCSPtr.setPointer(0, dateTimeStrLMBCS);
		
		NotesTimeDateStruct retTimeDate = NotesTimeDateStruct.newInstance();
		Memory retTimeDateMem = new Memory(retTimeDate.size());
		retTimeDate = NotesTimeDateStruct.newInstance(retTimeDateMem);
		
		short result = NotesNativeAPI.get().ConvertTextToTIMEDATE(null, null, dateTimeStrLMBCSPtr, NotesConstants.MAXALPHATIMEDATE, retTimeDate);
		NotesErrorUtils.checkResult(result);
		return new NotesTimeDate(retTimeDate);
	}

}
