package com.mindoo.domino.jna;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.InnardsConverter;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ShortByReference;

/**
 * Wrapper class for the TIMEDATE C API data structure
 * 
 * @author Karsten Lehmann
 */
public class NotesTimeDate implements Comparable<NotesTimeDate> {
	private int[] m_innards = new int[2];
	private NotesTimeDateStruct m_structReused;
	
	/**
	 * Creates a new date/time object and sets it to the current date/time
	 */
	public NotesTimeDate() {
		this(NotesDateTimeUtils.calendarToInnards(Calendar.getInstance()));
	}
	
	/**
	 * Creates a new date/time object and sets it to a date/time specified as
	 * innards array
	 * 
	 * @param innards innards array
	 */
	public NotesTimeDate(int innards[]) {
		m_innards = innards.clone();
	}
	
	/**
	 * Creates a new date/time object and sets it to the specified {@link Date}
	 * 
	 * @param dt date object
	 */
	public NotesTimeDate(Date dt) {
		this(NotesDateTimeUtils.dateToInnards(dt));
	}

	/**
	 * Creates a new date/time object and sets it to the specified {@link Calendar}
	 * 
	 * @param cal calendar object
	 */
	public NotesTimeDate(Calendar cal) {
		this(NotesDateTimeUtils.calendarToInnards(cal));
	}

	/**
	 * Creates a new date/time object and sets it to the specified time in milliseconds since
	 * GMT 1/1/70
	 * 
	 * @param timeMs the milliseconds since January 1, 1970, 00:00:00 GMT
	 */
	public NotesTimeDate(long timeMs) {
		this(new Date(timeMs));
	}
	
	/**
	 * Constructs a new date/time object
	 * 
	 * @param year year
	 * @param month month, january is 1
	 * @param day day
	 * @param hour hour
	 * @param minute minute
	 * @param second second
	 * @param millis milliseconds (Notes can only store hundredth seconds)
	 * @param zone timezone
	 */
	public NotesTimeDate(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone zone) {
		this(createCalendar(year, month, day, hour, minute, second, millis, zone));
	}

	private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone zone) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month-1);
		cal.set(Calendar.DAY_OF_MONTH, day);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, second);
		cal.set(Calendar.MILLISECOND, millis);
		cal.set(Calendar.ZONE_OFFSET, zone.getRawOffset());
		return cal;
	}
	
	/**
	 * Constructs a new date/time object in the default timezone
	 * 
	 * @param year year
	 * @param month month
	 * @param day day
	 * @param hour hour
	 * @param minute minute
	 * @param second second
	 * @param millis milliseconds (Notes can only store hundredth seconds)
	 */
	public NotesTimeDate(int year, int month, int day, int hour, int minute, int second, int millis) {
		this(year, month, day, hour, minute, second, millis, TimeZone.getDefault());
	}
	
	/**
	 * Constructs a new date/time object in the default timezone
	 * 
	 * @param year year
	 * @param month month
	 * @param day day
	 * @param hour hour
	 * @param minute minute
	 * @param second second
	 */
	public NotesTimeDate(int year, int month, int day, int hour, int minute, int second) {
		this(year, month, day, hour, minute, second, 0, TimeZone.getDefault());
	}
	
	/**
	 * Constructs a new date/time object in the default timezone
	 * 
	 * @param year year
	 * @param month month
	 * @param day day
	 * @param hour hour
	 * @param minute minute
	 */
	public NotesTimeDate(int year, int month, int day, int hour, int minute) {
		this(year, month, day, hour, minute, 0, 0, TimeZone.getDefault());
	}
	
	/**
	 * Constructs a new date-only date/time object
	 * 
	 * @param year year
	 * @param month month
	 * @param day day
	 */
	public NotesTimeDate(int year, int month, int day) {
		this(year, month, day, 0, 0, 0, 0, TimeZone.getDefault());
		setAnyTime();
	}
	
	/**
	 * Creates a new instance
	 * 
	 * @param adaptable object providing a supported data object for the time/date state
	 */
	public NotesTimeDate(IAdaptable adaptable) {
		NotesTimeDateStruct struct = adaptable.getAdapter(NotesTimeDateStruct.class);
		if (struct!=null) {
			m_innards = struct.Innards.clone();
			return;
		}
		
		Pointer p = adaptable.getAdapter(Pointer.class);
		if (p!=null) {
			struct = NotesTimeDateStruct.newInstance(p);
			struct.read();
			m_innards = struct.Innards.clone();
			return;
		}
		throw new IllegalArgumentException("Constructor argument cannot provide a supported datatype");
	}
	
	private NotesTimeDate(NotesTimeDateStruct struct) {
		m_innards = struct.Innards.clone();
	}
	
	NotesTimeDate(Pointer peer) {
		m_innards = peer.getIntArray(0, 2);
	}

	private NotesTimeDateStruct lazilyCreateStruct() {
		if (m_structReused==null) {
			m_structReused = NotesTimeDateStruct.newInstance();
		}
		m_structReused.Innards = m_innards;
		m_structReused.write();
		return m_structReused;
	}
	
	/**
	 * Returns a copy of the internal Innards values
	 * 
	 * @return innards
	 */
	public int[] getInnards() {
		if (m_innards!=null) {
			return m_innards.clone();
		}
		else
			return new int[] {NotesConstants.ALLDAY,NotesConstants.ANYDAY};
	}
	
	int[] getInnardsNoClone() {
		if (m_innards!=null) {
			return m_innards;
		}
		else
			return new int[] {NotesConstants.ALLDAY,NotesConstants.ANYDAY};
	}
	
	/**
	 * Checks whether the timedate has a date portion
	 * 
	 * @return true if date part exists
	 */
	public boolean hasDate() {
		int[] innards = getInnardsNoClone();
		
        boolean hasDate=(innards[1]!=0 && innards[1]!=NotesConstants.ANYDAY);
		return hasDate;
	}
	
	/**
	 * Checks whether the timedate has a time portion
	 * 
	 * @return true if time part exists
	 */
	public boolean hasTime() {
		int[] innards = getInnardsNoClone();

        boolean hasTime=(innards[0]!=0 && innards[0]!=NotesConstants.ALLDAY);
		return hasTime;
	}
	
	/**
	 * Converts the time date to a calendar
	 * 
	 * @return calendar or null if data is invalid
	 */
	public Calendar toCalendar() {
		int[] innards = getInnardsNoClone();
		Calendar cal = InnardsConverter.decodeInnards(innards);
		
		if (cal==null) {
			//invalid innards
			Calendar nullCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			nullCal.set(Calendar.DAY_OF_MONTH, 1);
			nullCal.set(Calendar.MONTH, 1);
			nullCal.set(Calendar.YEAR, 0);
			nullCal.set(Calendar.HOUR, 0);
			nullCal.set(Calendar.MINUTE, 0);
			nullCal.set(Calendar.SECOND, 0);
			nullCal.set(Calendar.MILLISECOND, 0);
			return nullCal;
		}
		else
			return cal;
	}
	
	/**
	 * Converts the time date to a Java {@link Date}
	 * 
	 * @return date or null if data is invalid
	 */
	public Date toDate() {
		Calendar cal = toCalendar();
		return cal==null ? null : cal.getTime();
	}
	
	@Override
	public int hashCode() {
		int[] innards = getInnardsNoClone();
		return Arrays.hashCode(innards);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof NotesTimeDate) {
			return Arrays.equals(getInnardsNoClone(), ((NotesTimeDate)o).getInnardsNoClone());
		}
		return false;
	}
	
	/**
	 * Returns a new {@link NotesTimeDate} with date and time info set to "now"
	 * 
	 * @return time date
	 */
	public static NotesTimeDate now() {
		NotesTimeDate td = new NotesTimeDate();
		td.setNow();
		return td;
	}

	/**
	 * Returns a new {@link NotesTimeDate} with date only, set to today
	 * 
	 * @return time date
	 */
	public static NotesTimeDate today() {
		NotesTimeDate td = new NotesTimeDate();
		td.setToday();
		return td;
	}

	/**
	 * Returns a new {@link NotesTimeDate} with date only, set to tomorrow
	 * 
	 * @return time date
	 */
	public static NotesTimeDate tomorrow() {
		NotesTimeDate td = new NotesTimeDate();
		td.setTomorrow();
		return td;
	}

	/**
	 * Returns a new {@link NotesTimeDate} with date and time info, adjusted from the current date/time
	 * 
	 * @param year positive or negative value or 0 for no change
	 * @param month positive or negative value or 0 for no change
	 * @param day positive or negative value or 0 for no change
	 * @param hours positive or negative value or 0 for no change
	 * @param minutes positive or negative value or 0 for no change
	 * @param seconds positive or negative value or 0 for no change
	 * @return timedate
	 */
	public static NotesTimeDate adjustedFromNow(int year, int month, int day, int hours, int minutes, int seconds) {
		NotesTimeDate td = new NotesTimeDate();
		td.adjust(year, month, day, hours, minutes, seconds);
		return td;
	}
	
	/**
	 * Sets the date/time of this timedate to the current time
	 */
	public void setNow() {
		m_innards = NotesDateTimeUtils.calendarToInnards(Calendar.getInstance(), true, true);
	}

	/**
	 * Changes the internally stored date/time value
	 * 
	 * @param dt new value
	 */
	public void setTime(Date dt) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dt);
		setTime(cal);
	}
	
	/**
	 * Changes the internally stored date/time value
	 * 
	 * @param innards new value as innards array (will be copied)
	 */
	public void setTime(int[] innards) {
		if (innards.length!=2)
			throw new IllegalArgumentException("Innards array must have 2 elements ("+innards.length+"!=2");
		m_innards = innards.clone();
	}
	/**
	 * Changes the internally stored date/time value
	 * 
	 * @param cal new value
	 */
	public void setTime(Calendar cal) {
		m_innards = NotesDateTimeUtils.calendarToInnards(cal);
	}
	
	/**
	 * Sets the date part of this timedate to today and the time part to ALLDAY
	 */
	public void setToday() {
		m_innards = NotesDateTimeUtils.calendarToInnards(Calendar.getInstance(), true, false);
	}

	/**
	 * Sets the date part of this timedate to tomorrow and the time part to ALLDAY
	 */
	public void setTomorrow() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 1);
		m_innards = NotesDateTimeUtils.calendarToInnards(cal, true, false);
	}

	/**
	 * Removes the time part of this timedate
	 */
	public void setAnyTime() {
		if (m_innards!=null) {
			m_innards[0] = NotesConstants.ALLDAY;
		}
		else {
			m_innards = new int[] {NotesConstants.ALLDAY, NotesConstants.ANYDAY};
		}
	}
	
	/**
	 * Checks whether the time part of this timedate is a wildcard
	 * 
	 * @return true if there is no time
	 */
	public boolean isAnyTime() {
		int[] innards = getInnardsNoClone();
		return innards[0] == NotesConstants.ALLDAY;
	}
	
	/**
	 * Removes the date part of this timedate
	 */
	public void setAnyDate() {
		if (m_innards!=null) {
			m_innards[1] = NotesConstants.ANYDAY;
		}
		else {
			m_innards = new int[] {NotesConstants.ALLDAY, NotesConstants.ANYDAY};
		}
	}
	
	/**
	 * Checks whether the date part of this timedate is a wildcard
	 * 
	 * @return true if there is no date
	 */
	public boolean isAnyDate() {
		int[] innards = getInnardsNoClone();
		return innards[1] == NotesConstants.ANYDAY;
	}
	
	/**
	 * Creates a new {@link NotesTimeDate} instance with the same data as this one
	 */
	public NotesTimeDate clone() {
		return new NotesTimeDate(getInnardsNoClone());
	}
	
	/**
	 * Modifies the data by adding/subtracting values for year, month, day, hours, minutes and seconds
	 * 
	 * @param year positive or negative value or 0 for no change
	 * @param month positive or negative value or 0 for no change
	 * @param day positive or negative value or 0 for no change
	 * @param hours positive or negative value or 0 for no change
	 * @param minutes positive or negative value or 0 for no change
	 * @param seconds positive or negative value or 0 for no change
	 */
	public void adjust(int year, int month, int day, int hours, int minutes, int seconds) {
		int[] innards = getInnardsNoClone();
		Calendar cal = NotesDateTimeUtils.innardsToCalendar(innards);
		if (cal!=null) {
			boolean modified = false;
			
			if (NotesDateTimeUtils.hasDate(cal)) {
				if (year!=0) {
					cal.add(Calendar.YEAR, year);
					modified=true;
				}
				if (month!=0) {
					cal.add(Calendar.MONTH, month);
					modified=true;
				}
				if (day!=0) {
					cal.add(Calendar.DATE, day);
					modified=true;
				}
			}
			if (NotesDateTimeUtils.hasTime(cal)) {
				if (hours!=0) {
					cal.add(Calendar.HOUR, hours);
					modified=true;
				}
				if (minutes!=0) {
					cal.add(Calendar.MINUTE, minutes);
					modified=true;
				}
				if (seconds!=0) {
					cal.add(Calendar.SECOND, seconds);
					modified=true;
				}
			}
			
			if (modified) {
				m_innards = NotesDateTimeUtils.calendarToInnards(cal);
			}
		}
	}

	/**
	 * Converts the time date to the number of milliseconds since 1/1/70.
	 * 
	 * @return milliseconds since January 1, 1970, 00:00:00 GMT
	 */
	public long toDateInMillis() {
		return toCalendar().getTimeInMillis();
	}

	public boolean isBefore(NotesTimeDate o) {
		return toDateInMillis() < o.toDateInMillis();
	}
	
	public boolean isAfter(NotesTimeDate o) {
		return toDateInMillis() > o.toDateInMillis();
	}
	
	@Override
	public int compareTo(NotesTimeDate o) {
		long thisTimeInMillis = toDateInMillis();
		long otherTimeInMillis = o.toDateInMillis();
		
		if (thisTimeInMillis < otherTimeInMillis) {
			return -1;
		}
		else if (thisTimeInMillis > otherTimeInMillis) {
			return 1;
		}
		else {
			return 0;
		}
	}
	
	/**
	 * Method to clear the {@link NotesTimeDate} value
	 */
	public void setMinimum() {
		NotesTimeDateStruct struct = lazilyCreateStruct();
		NotesNativeAPI.get().TimeConstant(NotesConstants.TIMEDATE_MINIMUM, struct);
		struct.read();
		m_innards = struct.Innards.clone();
	}
	
	/**
	 * Method to set the {@link NotesTimeDate} value to the maximum value.
	 */
	public void setMaximum() {
		NotesTimeDateStruct struct = lazilyCreateStruct();
		NotesNativeAPI.get().TimeConstant(NotesConstants.TIMEDATE_MAXIMUM, struct);
		struct.read();
		m_innards = struct.Innards.clone();
	}
	
	/**
	 * Method to set the {@link NotesTimeDate} value to ANYDAY/ALLDAY
	 */
	public void setWildcard() {
		NotesTimeDateStruct struct = lazilyCreateStruct();
		NotesNativeAPI.get().TimeConstant(NotesConstants.TIMEDATE_WILDCARD, struct);
		struct.read();
		m_innards = struct.Innards.clone();
	}
	
	/**
	 * Converts a {@link NotesTimeDate} to string
	 * 
	 * @return string with formatted timedate
	 */
	public String toString() {
		NotesTimeDateStruct struct = lazilyCreateStruct();
		
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
		
		DisposableMemory retTimeDateMem = new DisposableMemory(NotesConstants.timeDateSize);
		NotesTimeDateStruct retTimeDate = NotesTimeDateStruct.newInstance(retTimeDateMem);
		
		short result = NotesNativeAPI.get().ConvertTextToTIMEDATE(null, null, dateTimeStrLMBCSPtr, NotesConstants.MAXALPHATIMEDATE, retTimeDate);
		NotesErrorUtils.checkResult(result);
		retTimeDate.read();
		int[] innards = retTimeDate.Innards;
		NotesTimeDate td = new NotesTimeDate(innards);
		retTimeDateMem.dispose();
		return td;
	}
}
