package com.mindoo.domino.jna;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Wrapper class for the TIMEDATE C API data structure
 * 
 * @author Karsten Lehmann
 */
public class NotesTimeDate implements IAdaptable {
	private NotesTimeDateStruct m_struct;
	
	/**
	 * Creates a new date/time object and sets it to the current date/time
	 */
	public NotesTimeDate() {
		this(NotesTimeDateStruct.newInstance(NotesDateTimeUtils.calendarToInnards(Calendar.getInstance())));
	}
	
	/**
	 * Creates a new date/time object and sets it to a date/time specified as
	 * innards array
	 * 
	 * @param innards innards array
	 */
	public NotesTimeDate(int innards[]) {
		this(NotesTimeDateStruct.newInstance(innards));
	}
	
	/**
	 * Creates a new date/time object and sets it to the specified {@link Date}
	 * 
	 * @param dt date object
	 */
	public NotesTimeDate(Date dt) {
		this(NotesTimeDateStruct.newInstance(dt));
	}

	/**
	 * Creates a new date/time object and sets it to the specified {@link Calendar}
	 * 
	 * @param cal calendar object
	 */
	public NotesTimeDate(Calendar cal) {
		this(NotesTimeDateStruct.newInstance(cal));
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
	 * Creates a new instance
	 * 
	 * @param adaptable object providing a supported data object for the time/date state
	 */
	public NotesTimeDate(IAdaptable adaptable) {
		NotesTimeDateStruct struct = adaptable.getAdapter(NotesTimeDateStruct.class);
		if (struct!=null) {
			m_struct = struct;
			return;
		}
		
		Pointer p = adaptable.getAdapter(Pointer.class);
		if (p!=null) {
			m_struct = NotesTimeDateStruct.newInstance(p);
			return;
		}
		throw new IllegalArgumentException("Constructor argument cannot provide a supported datatype");
	}
	
	private NotesTimeDate(NotesTimeDateStruct struct) {
		m_struct = struct;
	}
	
	private NotesTimeDate(Pointer peer) {
		m_struct = NotesTimeDateStruct.newInstance(peer);
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesTimeDateStruct.class || clazz == Structure.class) {
			return (T) m_struct;
		}
		return null;
	}
	
	@Override
	public String toString() {
		return NotesDateTimeUtils.toString(this);
	}
	
	/**
	 * Returns a copy of the internal Innards values
	 * 
	 * @return innards
	 */
	public int[] getInnards() {
		return Arrays.copyOf(m_struct.Innards, m_struct.Innards.length);
	}
	
	/**
	 * Checks whether the timedate has a date portion
	 * 
	 * @return true if date part exists
	 */
	public boolean hasDate() {
        boolean hasDate=(m_struct.Innards[1]!=0 && m_struct.Innards[1]!=NotesCAPI.ANYDAY);
		return hasDate;
	}
	
	/**
	 * Checks whether the timedate has a time portion
	 * 
	 * @return true if time part exists
	 */
	public boolean hasTime() {
        boolean hasDate=(m_struct.Innards[0]!=0 && m_struct.Innards[0]!=NotesCAPI.ALLDAY);
		return hasDate;
	}
	
	/**
	 * Converts the time date to a calendar
	 * 
	 * @return calendar or null if data is invalid
	 */
	public Calendar toCalendar() {
		return NotesDateTimeUtils.innardsToCalendar(
				NotesDateTimeUtils.isDaylightTime(),
				NotesDateTimeUtils.getGMTOffset(),
				m_struct.Innards);
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
	public boolean equals(Object o) {
		if (o instanceof NotesTimeDate) {
			return Arrays.equals(m_struct.Innards, ((NotesTimeDate)o).m_struct.Innards);
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
	 * @return
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
		int[] newInnards = NotesDateTimeUtils.calendarToInnards(Calendar.getInstance(), true, true);
		m_struct.Innards[0] = newInnards[0];
		m_struct.Innards[1] = newInnards[1];
		m_struct.write();
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
	 * @param cal new value
	 */
	public void setTime(Calendar cal) {
		int[] innards = NotesDateTimeUtils.calendarToInnards(cal);
		m_struct.Innards[0] = innards[0];
		m_struct.Innards[1] = innards[1];
		m_struct.write();
	}
	
	/**
	 * Sets the date part of this timedate to today and the time part to ALLDAY
	 */
	public void setToday() {
		int[] newInnards = NotesDateTimeUtils.calendarToInnards(Calendar.getInstance(), true, false);
		m_struct.Innards[0] = newInnards[0];
		m_struct.Innards[1] = newInnards[1];
		m_struct.write();
	}

	/**
	 * Sets the date part of this timedate to tomorrow and the time part to ALLDAY
	 */
	public void setTomorrow() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 1);
		int[] newInnards = NotesDateTimeUtils.calendarToInnards(cal, true, false);
		m_struct.Innards[0] = newInnards[0];
		m_struct.Innards[1] = newInnards[1];
		m_struct.write();
	}

	/**
	 * Creates a new {@link NotesTimeDate} instance with the same data as this one
	 */
	public NotesTimeDate clone() {
		NotesTimeDate clone = new NotesTimeDate();
		clone.m_struct.Innards[0] = m_struct.Innards[0];
		clone.m_struct.Innards[1] = m_struct.Innards[1];
		clone.m_struct.write();
		return clone;
	}
	
	/**
	 * Converts the internal date/time value to a {@link Date}
	 * 
	 * @return date
	 */
	public Date getTime() {
		return getTimeAsCalendar().getTime();
	}
	
	/**
	 * Converts the internal date/time value to a {@link Calendar}
	 * 
	 * @return calendar
	 */
	public Calendar getTimeAsCalendar() {
		return NotesDateTimeUtils.timeDateToCalendar(NotesDateTimeUtils.isDaylightTime(), NotesDateTimeUtils.getGMTOffset(), this);
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
		Calendar cal = NotesDateTimeUtils.innardsToCalendar(NotesDateTimeUtils.isDaylightTime(), NotesDateTimeUtils.getGMTOffset(), m_struct.Innards);
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
				int[] newInnards = NotesDateTimeUtils.calendarToInnards(cal);
				m_struct.Innards[0] = newInnards[0];
				m_struct.Innards[1] = newInnards[1];
				m_struct.write();
			}
		}
	}
}
