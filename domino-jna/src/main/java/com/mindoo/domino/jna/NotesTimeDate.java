package com.mindoo.domino.jna;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import com.mindoo.domino.jna.constants.DateFormat;
import com.mindoo.domino.jna.constants.DateTimeStructure;
import com.mindoo.domino.jna.constants.TimeFormat;
import com.mindoo.domino.jna.constants.ZoneFormat;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.InnardsConverter;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.structs.IntlFormatStruct;
import com.mindoo.domino.jna.internal.structs.NotesTFMTStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ShortByReference;

/**
 * Wrapper class for the TIMEDATE C API data structure
 * 
 * @author Karsten Lehmann
 */
public class NotesTimeDate implements Comparable<NotesTimeDate>, Temporal, IAdaptable {
	private int[] m_innards = new int[2];
	private NotesTimeDateStruct m_structReused;
	
	private TimeZone m_guessedTimezone;
	
	/**
	 * Creates a new date/time object and sets it to the current date/time
	 */
	public NotesTimeDate() {
	    this(OffsetDateTime.now());
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
	 * Creates a new date/time object and sets it to the specified
	 * {@link ZonedDateTime}
	 * 
	 * @param dt zoned date/time value
	 */
	public NotesTimeDate(TemporalAccessor dt) {
	    int[] innards;
	    if (dt instanceof ZonedDateTime) {
	      innards = InnardsConverter.encodeInnards((ZonedDateTime) dt);
	    } else if (dt instanceof OffsetDateTime) {
	      innards = InnardsConverter.encodeInnards((OffsetDateTime) dt, null);
	    } else if (dt instanceof LocalDate) {
	      innards = InnardsConverter.encodeInnards((LocalDate) dt);
	    } else if (dt instanceof LocalTime) {
	      innards = InnardsConverter.encodeInnards((LocalTime) dt);
	    } else if (dt instanceof Instant) {
	      innards = InnardsConverter.encodeInnards(OffsetDateTime.ofInstant((Instant) dt, ZoneId.of("UTC")), null); //$NON-NLS-1$
	    } else if (dt instanceof NotesTimeDate) {
	      innards = ((NotesTimeDate) dt).getInnards();
	    } else {
	      final Instant instant = Instant.from(dt);
	      innards = InnardsConverter.encodeInnards(OffsetDateTime.ofInstant(instant, ZoneId.of("UTC")), null); //$NON-NLS-1$
	    }
	    this.m_innards = innards;
	}

	/**
	 * Creates a new date/time object and sets it to the specified {@link Date}
	 * 
	 * @deprecated use constructor for Java 8 Date/Time API
	 * @param dt date object
	 */
	@Deprecated
	public NotesTimeDate(Date dt) {
		this(NotesDateTimeUtils.dateToInnards(dt));
	}

	/**
	 * Creates a new date/time object and sets it to the specified {@link Calendar}
	 * 
	 * @deprecated use constructor for Java 8 Date/Time API
	 * @param cal calendar object
	 */
	@Deprecated
	public NotesTimeDate(Calendar cal) {
		this(NotesDateTimeUtils.calendarToInnards(cal));
	}


	/**
	 * Creates a new date/time object and sets it to the specified time in
	 * milliseconds since GMT 1/1/70
	 * 
	 * @param timeMs the milliseconds since January 1, 1970, 00:00:00 GMT
	 */
	public NotesTimeDate(long timeMs) {
		this(InnardsConverter.encodeInnards(ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeMs), ZoneId.of("UTC")))); //$NON-NLS-1$
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

	/**
	 * Constructs a new date/time by merging the date and time part of two other {@link NotesTimeDate} objects
	 * 
	 * @param date date part
	 * @param time time part
	 */
	public NotesTimeDate(NotesTimeDate date, NotesTimeDate time) {
		m_innards[0] = time.getInnardsNoClone()[0]; // time part
		m_innards[1] = date.getInnardsNoClone()[1]; // date part
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
		Calendar cal = createCalendar(year, month, day, 0, 0, 0, 0, TimeZone.getDefault());
		NotesDateTimeUtils.setAnyTime(cal);
		m_innards = NotesDateTimeUtils.calendarToInnards(cal);
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

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (NotesTimeDateStruct.class.equals(clazz)) {
			return (T) lazilyCreateStruct();
		}
		return null;
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
	 * Converts the {@link NotesTimeDate} to a {@link Temporal} value.
	 * 
	 * @return {@link OffsetDateTime}, {@link LocalDate}, {@link LocalTime}, or null if invalid innards
	 */
	public Optional<Temporal> toTemporal() {
		int[] innards = getInnardsNoClone();
	    return Optional.ofNullable(InnardsConverter.decodeInnards(innards));
	}
	
	/**
	 * Converts the time date to a calendar
	 * 
	 * @deprecated use {@link #toTemporal()} for Java 8 Date/Time API
	 * @return calendar or null if data is invalid
	 */
	@Deprecated
	public Calendar toCalendar() {
		int[] innards = getInnardsNoClone();
		return InnardsConverter.decodeInnardsToCalendar(innards);
	}
	
	/**
	 * Converts the time date to a Java {@link Date}
	 * 
	 * @deprecated use {@link #toTemporal()} for Java 8 Date/Time API
	 * @return date or null if data is invalid
	 */
	@Deprecated
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
	 * Returns a new {@link NotesTimeDate} with date only, set to yesterday
	 * 
	 * @return time date
	 */
	public static NotesTimeDate yesterday() {
		NotesTimeDate td = new NotesTimeDate();
		td.setYesterday();
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
		m_guessedTimezone = null;
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
		m_guessedTimezone = null;
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
		m_guessedTimezone = null;
	}
	/**
	 * Changes the internally stored date/time value
	 * 
	 * @param cal new value
	 */
	public void setTime(Calendar cal) {
		m_innards = NotesDateTimeUtils.calendarToInnards(cal);
		m_guessedTimezone = null;
	}
	
	public TimeZone getTimeZone() {
		if (m_guessedTimezone==null) {
			if (m_innards[1] == NotesConstants.ANYDAY) {
				return null;
			}

			long innard1Long = m_innards[1];

			int tzSign;
			if (((innard1Long >> 30) & 1 ) == 0) {
				tzSign = -1;
			}
			else {
				tzSign = 1;
			}

			//The high-order bit, bit 31 (0x80000000), is set if Daylight Savings Time is observed
			boolean useDST;
			if (((innard1Long >> 31) & 1 ) == 0) {
				useDST = false;
			}
			else {
				useDST = true;
			}

			int tzOffsetHours = (int) (innard1Long >> 24) & 0xF;
			int tzOffsetFraction15MinuteIntervalls = (int) (innard1Long >> 28) & 0x3;

			long rawOffsetMillis = tzSign * 1000 * (tzOffsetHours * 60 * 60 + 15*60*tzOffsetFraction15MinuteIntervalls);

			if (rawOffsetMillis==0) {
				m_guessedTimezone = TimeZone.getTimeZone("GMT");
			}
			else {
				//not great, go through the JDK locales to find a matching one by comparing the 
				//raw offset; grep its short id and try to load a TimeZone for it
				//the purpose is to return short ids like "CET" instead of "Africa/Ceuta"
				String[] timezonesWithOffset = TimeZone.getAvailableIDs((int) rawOffsetMillis);
				for (String currTZID : timezonesWithOffset) {
					TimeZone currTZ = TimeZone.getTimeZone(currTZID);
					if (useDST==currTZ.useDaylightTime()) {
						String tzShortId = currTZ.getDisplayName(false, TimeZone.SHORT, Locale.ENGLISH);
						m_guessedTimezone = TimeZone.getTimeZone(tzShortId);
						if ("GMT".equals(m_guessedTimezone.getID())) {
							//parse failed
							m_guessedTimezone = currTZ;
						}
						break;
					}
				}

				if (m_guessedTimezone==null) {
					String tzString = "GMT" + (tzSign < 0 ? "-" : "+") +
							StringUtil.pad(Integer.toString(tzOffsetHours + (useDST ? 1 : 0)), 2, '0', false) + ":" +
							StringUtil.pad(Integer.toString(15 * tzOffsetFraction15MinuteIntervalls), 2, '0', false);

					m_guessedTimezone = TimeZone.getTimeZone(tzString);
				}
			}
		}
		return m_guessedTimezone;
	}
	
	/**
	 * Changes the timezone of this {@link NotesTimeDate} while keeping the current date/time
	 * 
	 * @param tz new timezone
	 */
	public void setTimeZone(TimeZone tz) {
		long zoneMask = 0;

		//The high-order bit, bit 31 (0x80000000), is set if Daylight Savings Time is observed
		if (tz.useDaylightTime()) {
			zoneMask |= 1l << 31;
		}
		
		//Bit 30 (0x40000000) is set if the time zone is east of Greenwich mean time.
		int tzOffsetSeconds = (int)(tz.getRawOffset() / 1000);
		
		if (tzOffsetSeconds>0) {
			zoneMask |= 1l << 30;
		}
		int tzOffsetHours = Math.abs(tzOffsetSeconds / (60*60));
		
		//Bits 27-24 contain the number of hours difference between the time zone and Greenwich mean time
		zoneMask |= ((long)tzOffsetHours) << 24;

		//bits 29-28 contain the number of 15-minute intervals in the difference
		
		int tzOffsetFractionSeconds = tzOffsetSeconds - tzOffsetHours*60*60; //  tzOffset % 60;
		int tzOffsetFractionMinutes = tzOffsetFractionSeconds % 60;
		
		int tzOffsetFraction15MinuteIntervalls = tzOffsetFractionMinutes / 15;
		zoneMask |= ((long)tzOffsetFraction15MinuteIntervalls) << 28;

		m_innards[1] = m_innards[1] & 0xFFFFFF;
		long newInnard1AsLong = m_innards[1];
		newInnard1AsLong = (newInnard1AsLong & 0xFFFFFF) | zoneMask;
		
		m_innards[1] = (int) (newInnard1AsLong & 0xffffffff);
		
		m_guessedTimezone = tz;
	}
	
	/**
	 * Sets the date part of this timedate to today and the time part to ALLDAY
	 */
	public void setToday() {
		m_innards = NotesDateTimeUtils.calendarToInnards(Calendar.getInstance(), true, false);
		m_guessedTimezone = null;
	}

	/**
	 * Sets the date part of this timedate to tomorrow and the time part to ALLDAY
	 */
	public void setTomorrow() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 1);
		m_innards = NotesDateTimeUtils.calendarToInnards(cal, true, false);
		m_guessedTimezone = null;
	}

	/**
	 * Sets the date part of this timedate to yesterday and the time part to ALLDAY
	 */
	public void setYesterday() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		m_innards = NotesDateTimeUtils.calendarToInnards(cal, true, false);
		m_guessedTimezone = null;
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
		m_guessedTimezone = null;
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
		m_guessedTimezone = null;
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
	 * @deprecated use {@link #toTemporal()} for Java 8 Date/Time API
	 * @return milliseconds since January 1, 1970, 00:00:00 GMT
	 */
	@Deprecated
	public long toDateInMillis() {
		return toCalendar().getTimeInMillis();
	}

	public boolean isBefore(NotesTimeDate o) {
		return compareTo(o) < 0;
	}
	
	public boolean isAfter(NotesTimeDate o) {
		return compareTo(o) > 0;
	}

	public int compareTo(final NotesTimeDate o) {
		if (this.hasDate() && o.hasDate()) {
			if (this.hasTime() && o.hasTime()) {
				return this.toOffsetDateTime().compareTo(o.toOffsetDateTime());
			} else {
				return this.toLocalDate().compareTo(o.toLocalDate());
			}
		} else if (this.hasTime() && o.hasTime()) {
			return this.toLocalTime().compareTo(o.toLocalTime());
		} else {
			// Incompatible operands
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
		m_guessedTimezone = null;
	}
	
	/**
	 * Method to set the {@link NotesTimeDate} value to the maximum value.
	 */
	public void setMaximum() {
		NotesTimeDateStruct struct = lazilyCreateStruct();
		NotesNativeAPI.get().TimeConstant(NotesConstants.TIMEDATE_MAXIMUM, struct);
		struct.read();
		m_innards = struct.Innards.clone();
		m_guessedTimezone = null;
	}
	
	/**
	 * Method to set the {@link NotesTimeDate} value to ANYDAY/ALLDAY
	 */
	public void setWildcard() {
		NotesTimeDateStruct struct = lazilyCreateStruct();
		NotesNativeAPI.get().TimeConstant(NotesConstants.TIMEDATE_WILDCARD, struct);
		struct.read();
		m_innards = struct.Innards.clone();
		m_guessedTimezone = null;
	}
	
	/**
	 * Converts a {@link NotesTimeDate} to string
	 * 
	 * @return string with formatted timedate
	 */
	public String toString() {
		return toString(DateFormat.FULL, TimeFormat.FULL, ZoneFormat.ALWAYS, DateTimeStructure.DATETIME);
	}
	
	/**
	 * Converts a {@link NotesTimeDate} to string with formatting options.
	 * 
	 * @param dFormat how to format the date part
	 * @param tFormat how to format the time part
	 * @param zFormat how to format the timezone
	 * @param dtStructure overall structure of the result, e.g. {@link DateTimeStructure} for date only
	 * @return string with formatted timedate
	 */
	public String toString(DateFormat dFormat, TimeFormat tFormat, ZoneFormat zFormat, DateTimeStructure dtStructure) {
		return toString((NotesIntlFormat) null, dFormat, tFormat, zFormat, dtStructure);
	}
	
	/**
	 * Converts a {@link NotesTimeDate} to string with formatting options.
	 * 
	 * @param intl the internationalization settings in effect. Can be <code>null</code>, in which case this function works with the client/server default settings for the duration of the call.
	 * @param dFormat how to format the date part
	 * @param tFormat how to format the time part
	 * @param zFormat how to format the timezone
	 * @param dtStructure overall structure of the result, e.g. {@link DateTimeStructure} for date only
	 * @return string with formatted timedate
	 */
	public String toString(NotesIntlFormat intl, DateFormat dFormat, TimeFormat tFormat, ZoneFormat zFormat, DateTimeStructure dtStructure) {
		NotesTimeDateStruct struct = lazilyCreateStruct();
		
		if (struct.Innards==null || struct.Innards.length<2)
			return "";
		if (struct.Innards[0]==0 && struct.Innards[1]==0)
			return "MINIMUM";
		if (struct.Innards[0]==0 && struct.Innards[1]==0xffffff)
			return "MAXIMUM";
		
		
		IntlFormatStruct intlStruct = intl==null ? null : intl.getAdapter(IntlFormatStruct.class);
		NotesTFMTStruct tfmtStruct = NotesTFMTStruct.newInstance();
		tfmtStruct.Date = dFormat==null ? NotesConstants.TDFMT_FULL : dFormat.getValue();
		tfmtStruct.Time = tFormat==null ? NotesConstants.TTFMT_FULL : tFormat.getValue();
		tfmtStruct.Zone = zFormat==null ? NotesConstants.TZFMT_ALWAYS : zFormat.getValue();
		tfmtStruct.Structure = dtStructure==null ? NotesConstants.TSFMT_DATETIME : dtStructure.getValue();
		tfmtStruct.write();
		
		String txt;
		int outBufLength = 40;
		DisposableMemory retTextBuffer = new DisposableMemory(outBufLength);
		while (true) {
			ShortByReference retTextLength = new ShortByReference();
			short result = NotesNativeAPI.get().ConvertTIMEDATEToText(intlStruct, tfmtStruct.getPointer(), struct, retTextBuffer, (short) retTextBuffer.size(), retTextLength);
			if (result==1037) { // "Invalid Time or Date Encountered", return empty string like Notes UI does
				return "";
			}
			if (result!=1033) { // "Output Buffer Overflow"
				NotesErrorUtils.checkResult(result);
			}

			if (result==1033 || (retTextLength.getValue() >= retTextBuffer.size())) {
				retTextBuffer.dispose();
				outBufLength = outBufLength * 2;
				retTextBuffer = new DisposableMemory(outBufLength);

				continue;
			}
			else {
				txt = NotesStringUtils.fromLMBCS(retTextBuffer, retTextLength.getValue());
				break;
			}
		}

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
		return fromString((NotesIntlFormat) null, dateTimeStr);
	}
	
	/**
	 * Parses a timedate string to a {@link NotesTimeDate}
	 * 
	 * @param intl international settings to be used for parsing
	 * @param dateTimeStr timedate string
	 * @return timedate
	 */
	public static NotesTimeDate fromString(NotesIntlFormat intl, String dateTimeStr) {
		Memory dateTimeStrLMBCS = NotesStringUtils.toLMBCS(dateTimeStr, true);
		//convert method expects a pointer to the date string in memory
		Memory dateTimeStrLMBCSPtr = new Memory(Native.POINTER_SIZE);
		dateTimeStrLMBCSPtr.setPointer(0, dateTimeStrLMBCS);
		
		IntlFormatStruct intlStruct = intl==null ? null : intl.getAdapter(IntlFormatStruct.class);
		
		DisposableMemory retTimeDateMem = new DisposableMemory(NotesConstants.timeDateSize);
		NotesTimeDateStruct retTimeDate = NotesTimeDateStruct.newInstance(retTimeDateMem);
		
		short result = NotesNativeAPI.get().ConvertTextToTIMEDATE(intlStruct, null, dateTimeStrLMBCSPtr, NotesConstants.MAXALPHATIMEDATE, retTimeDate);
		NotesErrorUtils.checkResult(result);
		retTimeDate.read();
		int[] innards = retTimeDate.Innards;
		NotesTimeDate td = new NotesTimeDate(innards);
		retTimeDateMem.dispose();
		return td;
	}

	public LocalDate toLocalDate() {
		final Temporal temporal = this.toTemporal().orElse(null);
		if (temporal instanceof OffsetDateTime) {
			return ((OffsetDateTime) temporal).toLocalDate();
		} else if (temporal instanceof ZonedDateTime) {
			return ((ZonedDateTime) temporal).toLocalDate();
		} else if (temporal instanceof LocalDate) {
			return (LocalDate) temporal;
		} else {
			throw new NotesError(MessageFormat.format("Cannot create an LocalDate from a {0}", temporal.getClass().getName()));
		}
	}

	public LocalTime toLocalTime() {
		final Temporal temporal = this.toTemporal().orElse(null);
		if (temporal instanceof OffsetDateTime) {
			return ((OffsetDateTime) temporal).toLocalTime();
		} else if (temporal instanceof ZonedDateTime) {
			return ((ZonedDateTime) temporal).toLocalTime();
		} else if (temporal instanceof LocalTime) {
			return (LocalTime) temporal;
		} else {
			throw new NotesError(MessageFormat.format("Cannot create an LocalTime from a {0}", temporal.getClass().getName()));
		}
	}

	public OffsetDateTime toOffsetDateTime() {
		final Temporal temporal = this.toTemporal().orElse(null);
		if (temporal instanceof OffsetDateTime) {
			return (OffsetDateTime) temporal;
		} else if (temporal instanceof ZonedDateTime) {
			return ((ZonedDateTime) temporal).toOffsetDateTime();
		} else if (temporal != null) {
			throw new NotesError(
					MessageFormat.format("Cannot create an OffsetDateTime from a {0}", temporal.getClass().getName()));
		} else {
			throw new NotesError("Cannot create an OffsetDateTime: Date object is invalid");
		}
	}

	public boolean isValid() {
		return this.toTemporal().isPresent();
	}

	@Override
	public long until(final Temporal endExclusive, final TemporalUnit unit) {
		final Temporal temporal = this.toTemporal().get();
		return temporal.until(endExclusive, unit);
	}

	@Override
	public Temporal with(final TemporalField field, final long newValue) {
		Temporal temporal = this.toTemporal().get();
		temporal = temporal.with(field, newValue);
		return new NotesTimeDate(temporal);
	}


	@Override
	public boolean isSupported(final TemporalField field) {
		if (ChronoField.YEAR.equals(field) ||
				ChronoField.MONTH_OF_YEAR.equals(field) ||
				ChronoField.DAY_OF_MONTH.equals(field) ||
				ChronoField.HOUR_OF_DAY.equals(field) ||
				ChronoField.MINUTE_OF_HOUR.equals(field) ||
				ChronoField.SECOND_OF_MINUTE.equals(field) ||
				ChronoField.MILLI_OF_SECOND.equals(field) ||
				ChronoField.NANO_OF_SECOND.equals(field)) {

			return true;
		}
		return false;
	}

	@Override
	public boolean isSupported(final TemporalUnit unit) {
		if (ChronoUnit.YEARS.equals(unit) ||
				ChronoUnit.MONTHS.equals(unit) ||
				ChronoUnit.DAYS.equals(unit) ||
				ChronoUnit.HOURS.equals(unit) ||
				ChronoUnit.MINUTES.equals(unit) ||
				ChronoUnit.SECONDS.equals(unit) ||
				ChronoUnit.MILLIS.equals(unit)) {

			return true;
		}
		return false;
	}

	@Override
	public long getLong(final TemporalField field) {
		// TODO modify to check without conversion for compatible types
		return this.toTemporal().get().getLong(field);
	}

	@Override
	public Temporal plus(final long amountToAdd, final TemporalUnit unit) {
		Temporal temporal = this.toTemporal().get();
		temporal = temporal.plus(amountToAdd, unit);
		return new NotesTimeDate(temporal);
	}

	@Override
	public <R> R query(final TemporalQuery<R> query) {
		return this.toTemporal().get().query(query);
	}

}
