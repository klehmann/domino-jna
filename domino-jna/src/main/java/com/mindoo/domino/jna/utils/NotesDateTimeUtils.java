package com.mindoo.domino.jna.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.NotesTime;
import com.mindoo.domino.jna.structs.NotesTimeDate;
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
		TimeZone tz = Calendar.getInstance().getTimeZone();
		
	    return tz.useDaylightTime();
	}

	/**
	 * Returns the current timezone's GMT offset
	 * 
	 * @return offset
	 */
	public static int getGMTOffset() {
		TimeZone tz = Calendar.getInstance().getTimeZone();
		
		return (int)(tz.getRawOffset() / 3600000);
	}

	/**
	 * Method to convert a {@link NotesTimeDate} object to a Java {@link Calendar}
	 * 
	 * @param useDayLight true to use daylight savings time
	 * @param gmtOffset GMT offset
	 * @param timeDate time date to convert
	 * @return calendar
	 */
	public static Calendar timeDateToCalendar(boolean useDayLight, int gmtOffset, NotesTimeDate timeDate) {
		return innardsToCalendar(useDayLight, gmtOffset, timeDate.Innards);
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
		NotesTimeDate td = new NotesTimeDate();
		td.Innards[0] = innards[0];
		td.Innards[1] = innards[1];
		
		return td;
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
		NotesTimeDate td = new NotesTimeDate();
		td.Innards[0] = innards[0];
		td.Innards[1] = innards[1];
		
		return td;
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
		NotesTimeDate td = new NotesTimeDate();
		td.Innards[0] = innards[0];
		td.Innards[1] = innards[1];
		
		return td;
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
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		int gmtOffset = getGMTOffset();
		boolean isDST = isDaylightTime();
		
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+1;
		int day = cal.get(Calendar.DATE);
		
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		int millis = cal.get(Calendar.MILLISECOND);
		
		Memory m = new Memory(NotesCAPI.timeSize);
		NotesTime time = new NotesTime(m);
		
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
		boolean convRet = notesAPI.TimeLocalToGM(m);
        if (convRet) {
        	String msg = "Error converting calendar value to GM: "+cal.getTime();
        	throw new NotesError(0, msg);
        }
		time.read();

        int[] innards = time.GM.Innards;
        
        if (!hasDate) {
        	innards[1] = NotesCAPI.ANYDAY;
        }
        if (!hasTime) {
        	innards[0] = NotesCAPI.ALLDAY;
        }
        return innards;
	}
	
	/**
	 * Converts C API innard values to Java {@link Calendar}
	 * 
	 * @param useDayLight true to use daylight savings time for the calendar object
	 * @param gmtOffset GMT offset for the calendar object
	 * @param innards array with 2 innard values
	 * @return calendar
	 */
	public static Calendar innardsToCalendar(boolean useDayLight, int gmtOffset, int[] innards) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (innards==null || innards.length<2 || (innards.length>=2 && innards[0]==0 && innards[1]==0))
			return null;
		
        boolean hasTime=(innards[0]!=0 && innards[0]!=NotesCAPI.ALLDAY);
        boolean hasDate=(innards[1]!=0 && innards[1]!=NotesCAPI.ANYDAY);

		NotesTime time = new NotesTime();
		time.GM.Innards[0] = innards[0];
		time.GM.Innards[1] = innards[1];
		
		//set desired daylight-saving time to appropriate value -> Calendar.getInstance().useDaylightTime()
		time.dst=(useDayLight) ? 1 : 0; 
        // set desired time zone to appropriate value -> Calendar.getInstance().getTimeZone().getRawOffset()
        time.zone=-gmtOffset;

        boolean convRet;
        if (hasDate && hasTime) {
        	convRet = notesAPI.TimeGMToLocalZone(time);
        }
        else {
        	convRet = notesAPI.TimeGMToLocal(time);
        }
        
        if (convRet) {
        	String msg = "Error converting date/time value from GMT to local zone: ";
    		msg+="[";
    		for (int i=0; i<innards.length; i++) {
    			if (i>0)
    				msg+=", ";
    			msg+=innards[i];
    		}
    		msg+="]";
        	throw new NotesError(0, msg);
        }
        
		int year = time.year;
		int month = time.month-1;
		int date = time.day;
		int weekday = time.weekday;
		int hour = time.hour;
		int minute = time.minute;
		int second = time.second;
		int millisecond = (short) (time.hundredth * 10);

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
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		notesAPI.TimeConstant(NotesCAPI.TIMEDATE_MINIMUM, timeDate);
//		timeDate.read();
	}
	
	/**
	 * Method to set the {@link NotesTimeDate} value to the maximum value.
	 * 
	 * @param timeDate value to be changed
	 */
	public static void setMaximum(NotesTimeDate timeDate) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		notesAPI.TimeConstant(NotesCAPI.TIMEDATE_MAXIMUM, timeDate);
//		timeDate.read();
	}
	
	/**
	 * Method to set the {@link NotesTimeDate} value to ANYDAY/ALLDAY
	 * 
	 * @param timeDate value to be changed
	 */
	public static void setWildcard(NotesTimeDate timeDate) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		notesAPI.TimeConstant(NotesCAPI.TIMEDATE_WILDCARD, timeDate);
//		timeDate.read();
	}
	
	/**
	 * Converts a {@link NotesTimeDate} to string
	 * 
	 * @param td timedate
	 * @return string with formatted timedate
	 */
	public static String toString(NotesTimeDate td) {
		if (td.Innards==null || td.Innards.length<2 || (td.Innards.length>=2 && td.Innards[0]==0 && td.Innards[1]==0))
			return null;
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory retTextBuffer = new Memory(100);

		ShortByReference retTextLength = new ShortByReference();
		short result = notesAPI.ConvertTIMEDATEToText(null, null, td, retTextBuffer, (short) retTextBuffer.size(), retTextLength);
		NotesErrorUtils.checkResult(result);

		if (retTextLength.getValue() > retTextBuffer.size()) {
			retTextBuffer = new Memory(retTextLength.getValue());

			result = notesAPI.ConvertTIMEDATEToText(null, null, td, retTextBuffer, (short) retTextBuffer.size(), retTextLength);
			NotesErrorUtils.checkResult(result);
		}

		return NotesStringUtils.fromLMBCS(retTextBuffer, retTextLength.getValue());
	}
	
	/**
	 * Parses a timedate string to a {@link NotesTimeDate}
	 * 
	 * @param dateTimeStr timedate string
	 * @return timedate
	 */
	public static NotesTimeDate fromString(String dateTimeStr) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory dateTimeStrLMBCS = NotesStringUtils.toLMBCS(dateTimeStr, true);
		//convert method expects a pointer to the date string in memory
		Memory dateTimeStrLMBCSPtr = new Memory(Pointer.SIZE);
		dateTimeStrLMBCSPtr.setPointer(0, dateTimeStrLMBCS);
		
		NotesTimeDate retTimeDate = new NotesTimeDate();
		Memory retTimeDateMem = new Memory(retTimeDate.size());
		retTimeDate = new NotesTimeDate(retTimeDateMem);
		
		short result = notesAPI.ConvertTextToTIMEDATE(null, null, dateTimeStrLMBCSPtr, NotesCAPI.MAXALPHATIMEDATE, retTimeDate);
		NotesErrorUtils.checkResult(result);
		return retTimeDate;
	}

}
