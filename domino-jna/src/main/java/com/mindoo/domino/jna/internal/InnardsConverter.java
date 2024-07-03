package com.mindoo.domino.jna.internal;

import java.text.MessageFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.JulianFields;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.mindoo.domino.jna.internal.structs.NotesTimeStruct;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;

/**
 * Utility class to convert date/time values between the Domino innard array and
 * Java {@link Calendar}.<br>
 * <br>
 * This class contains both methods using the C API and methods using a manual
 * conversion.
 * The Domino date format is documented in the C API:<br>
 * <hr>
 * <b>TIMEDATE</b><br>
 * <br>
 * <br>
 * The Domino and Notes TIMEDATE structure consists of two long words that
 * encode the time, the date,
 * the time zone, and the Daylight Savings Time settings that were in effect
 * when the structure was initialized.<br>
 * <br>
 * The TIMEDATE structure is designed to be accessed exclusively through the
 * time and date subroutines defined in misc.h.<br>
 * <br>
 * This structure is subject to change; the description here is provided for
 * debugging purposes.<br>
 * <br>
 * The first DWORD, Innards[0], contains the number of hundredths of seconds
 * since midnight, Greenwich mean time.<br>
 * <br>
 * If only the date is important, not the time, this field may be set to
 * ALLDAY.<br>
 * <br>
 * The date and the time zone and Daylight Savings Time settings are encoded in
 * Innards[1].<br>
 * <br>
 * The 24 low-order bits contain the Julian Day, the number of days since
 * January 1, 4713 BC.<br>
 * <br>
 * Note that this is NOT the same as the Julian calendar!<br>
 * The Julian Day was originally devised as an aid to astronomers.<br>
 * Since only days are counted, weeks, months, and years are ignored in
 * calculations.<br>
 * The Julian Day is defined to begin at noon; for simplicity, Domino and Notes
 * assume that the day begins at midnight.<br>
 * <br>
 * The high-order byte, bits 31-24, encodes the time zone and Daylight Savings
 * Time information.<br>
 * The high-order bit, bit 31 (0x80000000), is set if Daylight Savings Time is
 * observed.<br>
 * Bit 30 (0x40000000) is set if the time zone is east of Greenwich mean
 * time.<br>
 * Bits 27-24 contain the number of hours difference between the time zone and
 * Greenwich mean time, and bits
 * 29-28 contain the number of 15-minute intervals in the difference.<br>
 * <br>
 * For example, 2:49:04 P. M., Eastern Standard Time, December 10, 1996 would be
 * stored as:<br>
 * <br>
 * Innards[0]: 0x006CDCC0 19 hours, 49 minutes, 4 seconds GMT<br>
 * Innards[1]: 0x852563FC DST observed, zone +5, Julian Day 2,450,428<br>
 * <br>
 * If the time zone were set for Bombay, India, where Daylight Savings Time is
 * not observed,
 * 2:49:04 P. M., December 10, 1996 would be stored as:<br>
 * <br>
 * Innards[0]: 0x0032B864 9 hours, 19 minutes, 4 seconds GMT<br>
 * Innards[1]: 0x652563FC No DST, zone 5 1/2 hours east of GMT, Julian Day
 * 2,450,428<br>
 * <hr>
 * Here is more documentation regarding date formats in Domino:<br>
 * <a href="http://www-01.ibm.com/support/docview.wss?uid=swg27003019">How to
 * interpret the Hexadecimal values in a Time/Date value</a><br>
 * <br>
 * And here is an interesting technote regarding specific dates in the year
 * 1752:<br>
 * <a href="http://www-01.ibm.com/support/docview.wss?uid=swg21098816">Error:
 * 'Unable to interpret time or date' when entering specific dates from the year
 * 1752</a>
 *
 * @author Karsten Lehmann
 */
public class InnardsConverter {

  /**
   * Container for all values read from an innards array
   * 
   * @author Karsten Lehmann
   */
  public static class InnardsInfo {
    private int[] innards;
    private boolean isEastOfGMT;
    private int hoursOffset;
    private int intervalsOf15Minutes;
    private long millisSinceMidnight;
    private boolean hasDate;
    private boolean hasTime;
    private boolean isDST;
    private int hour;
    private int minute;
    private int seconds;
    private int millis;
    private int day;
    private int month;
    private int year;
    private int julianDay;
    private long utcTimeFromJulianDay;
    private Date utcTimeFromJulianDayAsDate;
    private long m_epochTimeMillis;

    public int getDay() {
      return this.day;
    }

    public long getEpochTimeMillis() {
      return this.m_epochTimeMillis;
    }

    public int getHour() {
      return this.hour;
    }

    public int getHoursOffset() {
      return this.hoursOffset;
    }

    public int[] getInnards() {
      return this.innards;
    }

    public int getIntervalsOf15Minutes() {
      return this.intervalsOf15Minutes;
    }

    public int getJulianDay() {
      return this.julianDay;
    }

    public int getMillis() {
      return this.millis;
    }

    public long getMillisSinceMidnight() {
      return this.millisSinceMidnight;
    }

    public int getMinute() {
      return this.minute;
    }

    public int getMonth() {
      return this.month;
    }

    public int getSeconds() {
      return this.seconds;
    }

    public long getUtcTimeFromJulianDay() {
      return this.utcTimeFromJulianDay;
    }

    public Date getUtcTimeFromJulianDayAsDate() {
      return this.utcTimeFromJulianDayAsDate;
    }

    public int getYear() {
      return this.year;
    }

    public boolean isDST() {
      return this.isDST;
    }

    public boolean isEastOfGMT() {
      return this.isEastOfGMT;
    }

    public boolean isHasDate() {
      return this.hasDate;
    }

    public boolean isHasTime() {
      return this.hasTime;
    }

    public void setDay(final int day) {
      this.day = day;
    }

    public void setDST(final boolean isDST) {
      this.isDST = isDST;
    }

    public void setEastOfGMT(final boolean isEastOfGMT) {
      this.isEastOfGMT = isEastOfGMT;
    }

    public void setEpochTimeMillis(final long epochTimeMillis) {
      this.m_epochTimeMillis = epochTimeMillis;
    }

    public void setHasDate(final boolean hasDate) {
      this.hasDate = hasDate;
    }

    public void setHasTime(final boolean hasTime) {
      this.hasTime = hasTime;
    }

    public void setHour(final int hour) {
      this.hour = hour;
    }

    public void setHoursOffset(final int hoursOffset) {
      this.hoursOffset = hoursOffset;
    }

    public void setInnards(final int[] innards) {
      this.innards = innards;
    }

    public void setIntervalsOf15Minutes(final int intervalsOf15Minutes) {
      this.intervalsOf15Minutes = intervalsOf15Minutes;
    }

    public void setJulianDay(final int julianDay) {
      this.julianDay = julianDay;
    }

    public void setMillis(final int millis) {
      this.millis = millis;
    }

    public void setMillisSinceMidnight(final long millisSinceMidnight) {
      this.millisSinceMidnight = millisSinceMidnight;
    }

    public void setMinute(final int minute) {
      this.minute = minute;
    }

    public void setMonth(final int month) {
      this.month = month;
    }

    public void setSeconds(final int seconds) {
      this.seconds = seconds;
    }

    public void setUtcTimeFromJulianDay(final long utcTimeFromJulianDay) {
      this.utcTimeFromJulianDay = utcTimeFromJulianDay;
    }

    public void setUtcTimeFromJulianDayAsDate(final Date utcTimeFromJulianDayAsDate) {
      this.utcTimeFromJulianDayAsDate = utcTimeFromJulianDayAsDate;
    }

    public void setYear(final int year) {
      this.year = year;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
      return "InnardsInfo [innards=" + Arrays.toString(this.innards) + ", isEastOfGMT=" + this.isEastOfGMT
          + ", hoursOffset=" + this.hoursOffset + ", intervalsOf15Minutes=" + this.intervalsOf15Minutes
          + ", millisSinceMidnight=" + this.millisSinceMidnight
          + ", hasDate=" + this.hasDate + ", hasTime=" + this.hasTime + ", hour=" + this.hour + ", minute=" + this.minute
          + ", seconds=" + this.seconds + ", millis=" + this.millis + ", day=" + this.day + ", month=" + this.month + ", year="
          + this.year + ", isDST=" + this.isDST() + ", julianDay=" + this.getJulianDay() + ", utcFromJulianDay="
          + this.getUtcTimeFromJulianDay() +
          ", utcFromJulianDayAsDate = " + this.getUtcTimeFromJulianDayAsDate() + ", epochTimeMillis=" + this.getEpochTimeMillis()
          + "]";
    }
  }

  public static Calendar decodeInnardsToCalendar(final int[] innards) {
	  Temporal t = InnardsConverter.decodeInnards(innards);
	  if (t==null) {
			//invalid innards
			Calendar nullCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			nullCal.set(Calendar.DAY_OF_MONTH, 1);
			nullCal.set(Calendar.MONTH, 1);
			nullCal.set(Calendar.YEAR, 0);
			nullCal.set(Calendar.HOUR_OF_DAY, 0);
			nullCal.set(Calendar.MINUTE, 0);
			nullCal.set(Calendar.SECOND, 0);
			nullCal.set(Calendar.MILLISECOND, 0);
			return nullCal;
		}
		else if (t instanceof OffsetDateTime) {
			OffsetDateTime offsetDateTime = (OffsetDateTime) t;
			Calendar calendar = Calendar.getInstance();
			calendar.clear();
			long epochMillis = offsetDateTime.toInstant().toEpochMilli();
			calendar.setTimeInMillis(epochMillis);
			return calendar;
		}
		else if (t instanceof LocalDate) {
			LocalDate localDate = (LocalDate) t;
			Calendar calendar = Calendar.getInstance();
			NotesDateTimeUtils.setAnyTime(calendar);
			//assuming start of day
			calendar.set(localDate.getYear(), localDate.getMonthValue()-1, localDate.getDayOfMonth());
			return calendar;
		}
		else if (t instanceof LocalTime) {
			LocalTime localTime = (LocalTime) t;
			Calendar calendar = Calendar.getInstance();
			NotesDateTimeUtils.setAnyDate(calendar);
			//assuming year/month/date information is not important
			int nano = localTime.getNano();
			int hundredth = nano / 1000000;
			int millisFromHundredth = hundredth*10;
//			calendar.set(Calendar.MILLISECOND, millisFromHundredth);
			calendar.set(0, 0, 0, localTime.getHour(), localTime.getMinute(), localTime.getSecond());
			return calendar;
		}
		else {
			throw new IllegalArgumentException(MessageFormat.format("Unexpected Temporal class: {0}", t.getClass().getName()));
		}
  }
  
  /**
   * Converts C API innard values to Java {@link Temporal} implementations. This
   * implementation
   * uses pure Java functions for the conversion, which is faster than using JNA.
   * 
   * @param innards array with 2 innard values
   * @return {@link OffsetDateTime}, {@link LocalDate}, {@link LocalTime}, or null
   *         if invalid innards
   */
  public static Temporal decodeInnards(final int[] innards) {
    if (innards == null || innards.length < 2 || innards.length >= 2 && innards[0] == 0 && innards[1] == 0) {
      return null;
    }

    // The Domino and Notes TIMEDATE structure consists of two long words that
    // encode the time, the date,
    // the time zone, and the Daylight Savings Time settings that were in effect
    // when the structure was initialized.
    // The TIMEDATE structure is designed to be accessed exclusively through the
    // time and date subroutines
    // defined in misc.h. This structure is subject to change; the description here
    // is provided for debugging purposes.

    final boolean hasTime = innards[0] != NotesConstants.ALLDAY;
    final boolean hasDate = innards[1] != NotesConstants.ANYDAY;

    if (!hasDate && !hasTime) {
      return null;
    }

    // The first DWORD, Innards[0], contains the number of hundredths of seconds
    // since midnight,
    // Greenwich mean time. If only the date is important, not the time, this field
    // may be set to ALLDAY.

    final long timeInnard = Integer.toUnsignedLong(innards[0]);
    final long hundredSecondsSinceMidnight = timeInnard;
    long milliSecondsSinceMidnight;
    if (hasTime) {
      milliSecondsSinceMidnight = hundredSecondsSinceMidnight * 10;
    } else {
      milliSecondsSinceMidnight = 0;
    }

    LocalTime utcTime;
    try {
      utcTime = LocalTime.ofNanoOfDay(milliSecondsSinceMidnight * 1000 * 1000);
    } catch (final DateTimeException e) {
      // Observed when the stored data is not representable (e.g. from a randomly-set
      // UNID)
      return null;
    }
    if (!hasDate) {
      return utcTime;
    }

    // The date and the time zone and Daylight Savings Time settings are encoded in
    // Innards[1].
    //
    // The 24 low-order bits contain the Julian Day, the number of days since
    // January 1, 4713 BC.
    //
    // Note that this is NOT the same as the Julian calendar! The Julian Day was
    // originally devised as an aid
    // to astronomers. Since only days are counted, weeks, months, and years are
    // ignored in calculations.
    // The Julian Day is defined to begin at noon; for simplicity, Domino and Notes
    // assume that the day
    // begins at midnight. The high-order byte, bits 31-24, encodes the time zone
    // and Daylight Savings
    // Time information.
    // The high-order bit, bit 31 (0x80000000), is set if Daylight Savings Time is
    // observed.
    // Bit 30 (0x40000000) is set if the time zone is east of Greenwich mean time.
    // Bits 27-24 contain the number of hours difference between the time zone and
    // Greenwich mean
    // time, and bits 29-28 contain the number of 15-minute intervals in the
    // difference.

    final int dateInnard = innards[1];

    final long julianDay = dateInnard & 0x7FFFFF;

    //using 1970-01-01 for performance reasons, earlier implementation used LocalDate.now() which was slower
    final LocalDate utcDate = LocalDate.of(1970,1,1).with(JulianFields.JULIAN_DAY, julianDay);

    if (hasTime) {
      final OffsetDateTime utc = OffsetDateTime.of(utcDate, utcTime, ZoneOffset.UTC);

      // Determines whether the zone does DST at all
      final boolean dst = (dateInnard & 0x80000000) != 0;              // bit 31

      // Figure out the time zone
      final boolean eastOfGmt = (dateInnard & 0x40000000) != 0;        // bit 30
      // Non-daylight offset from GMT (e.g. -5h in US Eastern regardless of day of
      // year)
      final int hourOffset = (dateInnard & 0xF000000) >> 24;            // bits 27-24
      final int intervalCount = (dateInnard & 0x30000000) >> 28;        // bits 29-28

      final int offsetSeconds = (eastOfGmt ? 1 : -1) * (hourOffset * 60 * 60 + intervalCount * 15 * 60);

      // Since time zone information is stored only as "normal offset" + "do they do
      // daylight savings at all?",
      // we it's unsafe to try to map to a real time zone. Instead, just return an
      // OffsetDateTime that matches
      // how it was stored
      if (offsetSeconds != 0) {
        // Then just make a generic offset
        final ZoneOffset offset = ZoneOffset.ofTotalSeconds(offsetSeconds);
        return OffsetDateTime.ofInstant(utc.toInstant(), offset);
      } else {
        return utc;
      }
    } else {
      return utcDate;
    }
  }

  public static int[] encodeInnards(final LocalDate localDate) {
    // The 24 low-order bits contain the Julian Day, the number of days since
    // January 1, 4713 BC
    return new int[] {
        NotesConstants.ALLDAY,
        (int) localDate.getLong(JulianFields.JULIAN_DAY)
    };
  }

  public static int[] encodeInnards(final LocalTime localTime) {
    // The first DWORD, Innards[0], contains the number of hundredths of seconds
    // since midnight
    return new int[] {
        (int) (localTime.toNanoOfDay() / 1000 / 1000 / 10),
        NotesConstants.ANYDAY
    };
  }

  public static int[] encodeInnards(final OffsetDateTime offsetDateTime, final ZoneId zoneId) {
    final int[] innards = new int[2];

    // The first DWORD, Innards[0], contains the number of hundredths of seconds
    // since midnight, GMT
    innards[0] = (int) (offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalTime().toNanoOfDay() / 1000 / 1000 / 10);

    // The 24 low-order bits contain the Julian Day, the number of days since
    // January 1, 4713 BC
    final int julianDay = (int) offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).getLong(JulianFields.JULIAN_DAY);

    int zoneMask = 0;

    // Figure out the offset during non-summer time
    int tzOffsetSeconds;
    if (zoneId != null) {
      tzOffsetSeconds = zoneId.getRules().getOffset(offsetDateTime.toInstant()).getTotalSeconds();
    } else {
      tzOffsetSeconds = offsetDateTime.getOffset().getTotalSeconds();
    }

    // Bit 30 (0x40000000) is set if the time zone is east of Greenwich mean time.
    if (tzOffsetSeconds > 0) {
      zoneMask |= 0x40000000;
    }

    final int tzOffsetHours = Math.abs(tzOffsetSeconds / (60 * 60));

    // Bits 27-24 contain the number of hours difference between the time zone and
    // Greenwich mean time
    zoneMask |= (long) tzOffsetHours << 24;

    // bits 29-28 contain the number of 15-minute intervals in the difference

    final int tzOffsetFractionSeconds = Math.abs(tzOffsetSeconds % (60 * 60));
    final int tzOffsetFractionMinutes = tzOffsetFractionSeconds / 60;
    final int tzOffsetFraction15MinuteIntervals = tzOffsetFractionMinutes / 15;

    // Check to make sure we can even express this
    final int expressableOffset = tzOffsetHours * 60 * 60 + tzOffsetFraction15MinuteIntervals * 15 * 60;
    if (expressableOffset != Math.abs(tzOffsetSeconds)) {
      throw new IllegalArgumentException(
          MessageFormat.format("Zone offset of {0} seconds cannot be expressed as a Domino date/time", tzOffsetSeconds));
    }
    zoneMask |= tzOffsetFraction15MinuteIntervals << 28;

    innards[1] = julianDay | zoneMask;

    return innards;
  }

  /**
   * Method to convert a {@link ZonedDateTime} object to an innard array. This
   * implementation
   * uses pure Java functions for the conversion, which is faster than using JNA.
   * 
   * @param zonedDateTime the zoned date time object to convert
   * @return innard array
   */
  public static int[] encodeInnards(final ZonedDateTime zonedDateTime) {
    int[] innards;
    // The high-order bit, bit 31 (0x80000000), is set if Daylight Savings Time is
    // ever observed in the zone
    if (InnardsConverter.hasDst(zonedDateTime.getZone(), zonedDateTime.toInstant())) {
      innards = InnardsConverter.encodeInnards(zonedDateTime.toOffsetDateTime(), zonedDateTime.getZone());
      innards[1] |= 0x80000000;
    } else {
      innards = InnardsConverter.encodeInnards(zonedDateTime.toOffsetDateTime(), null);
    }

    return innards;
  }

  private static long fromJulianDay(final long julianDay, final int timezoneOffset) {
    return (long) ((double) julianDay + timezoneOffset / 1440 - 2440587.5d) * 86400000;
  }

  public static int getTimeZoneOffset(final int[] innards) {
    final int dateInnard = innards[1];

    // Bit 30 (0x40000000) is set if the time zone is east of Greenwich mean time
    int factor;
    if ((dateInnard >> 30 & 1) == 1) {
      factor = 1;
    } else {
      factor = -1;
    }

    // Bits 27-24 contain the number of hours difference between the time zone and
    // Greenwich mean
    // time, and bits 29-28 contain the number of 15-minute intervals in the
    // difference.
    final int hours = dateInnard >> 24 & 15;
    final int intervals15min = dateInnard >> 28 & 3;
    return factor * (hours * 60 + intervals15min * 15);
  }

  private static boolean hasDst(final ZoneId zone, final Instant instant) {
    return zone.getRules().nextTransition(instant) != null && zone.getRules().previousTransition(instant) != null;
  }

  public static boolean isAnyDate(final int[] innards) {
    return innards[1] == NotesConstants.ANYDAY;
  }

  public static boolean isAnyTime(final int[] innards) {
    return innards[0] == NotesConstants.ALLDAY;
  }

  public static boolean isDST(final int[] innards) {
    final int dateInnard = innards[1];

    // The high-order bit, bit 31 (0x80000000), is set if Daylight Savings Time is
    // observed
    if ((dateInnard >> 31 & 1) == 1) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Parses the content of an innard array
   * 
   * @param innards innards
   * @return parse result
   */
  public static InnardsInfo parseInnards(final int[] innards) {
    if (innards == null || innards.length < 2 || innards.length >= 2 && innards[0] == 0 && innards[1] == 0) {
      return null;
    }

    final InnardsInfo info = new InnardsInfo();
    info.setInnards(innards.clone());

    final boolean hasTime = innards[0] != NotesConstants.ALLDAY;
    info.setHasTime(hasTime);

    final boolean hasDate = innards[1] != NotesConstants.ANYDAY;
    info.setHasDate(hasDate);

    // The 24 low-order bits contain the Julian Day, the number of days since
    // January 1, 4713 BC.
    final int julianDay = innards[1] & 16777215 & 0xffffffff;
    info.setJulianDay(julianDay);

    final long utcTimeFromJulianDay = InnardsConverter.fromJulianDay(julianDay, 0);
    info.setUtcTimeFromJulianDay(utcTimeFromJulianDay);
    info.setUtcTimeFromJulianDayAsDate(new Date(utcTimeFromJulianDay));

    // The high-order bit, bit 31 (0x80000000), is set if Daylight Savings Time is
    // observed
    if ((innards[1] >> 31 & 1) == 1) {
      info.setDST(true);
    } else {
      info.setDST(false);
    }

    // Bit 30 (0x40000000) is set if the time zone is east of Greenwich mean time
    if ((innards[1] >> 30 & 1) == 1) {
      info.setEastOfGMT(true);
    } else {
      info.setEastOfGMT(false);
    }

    // Bits 27-24 contain the number of hours difference between the time zone and
    // Greenwich mean
    // time, and bits 29-28 contain the number of 15-minute intervals in the
    // difference.
    final int hours = innards[1] >> 24 & 15;
    info.setHoursOffset(hours);
    final int intervals15min = innards[1] >> 28 & 3;
    info.setIntervalsOf15Minutes(intervals15min);

    if (!hasDate && !hasTime) {
      return info;
    }

    // The Domino and Notes TIMEDATE structure consists of two long words that
    // encode the time, the date,
    // the time zone, and the Daylight Savings Time settings that were in effect
    // when the structure was initialized.
    // The TIMEDATE structure is designed to be accessed exclusively through the
    // time and date subroutines
    // defined in misc.h. This structure is subject to change; the description here
    // is provided for debugging purposes.
    //
    // The first DWORD, Innards[0], contains the number of hundredths of seconds
    // since midnight,
    // Greenwich mean time. If only the date is important, not the time, this field
    // may be set to ALLDAY.
    //
    // The date and the time zone and Daylight Savings Time settings are encoded in
    // Innards[1].
    //
    // The 24 low-order bits contain the Julian Day, the number of days since
    // January 1, 4713 BC.
    //
    // Note that this is NOT the same as the Julian calendar! The Julian Day was
    // originally devised as an aid
    // to astronomers. Since only days are counted, weeks, months, and years are
    // ignored in calculations.
    // The Julian Day is defined to begin at noon; for simplicity, Domino and Notes
    // assume that the day
    // begins at midnight. The high-order byte, bits 31-24, encodes the time zone
    // and Daylight Savings
    // Time information.
    // The high-order bit, bit 31 (0x80000000), is set if Daylight Savings Time is
    // observed.
    // Bit 30 (0x40000000) is set if the time zone is east of Greenwich mean time.
    // Bits 27-24 contain the number of hours difference between the time zone and
    // Greenwich mean
    // time, and bits 29-28 contain the number of 15-minute intervals in the
    // difference.

    final long hundredSecondsSinceMidnight = innards[0];
    long milliSecondsSinceMidnight;
    if (hasTime) {
      milliSecondsSinceMidnight = hundredSecondsSinceMidnight * 10;
    } else {
      milliSecondsSinceMidnight = 0;
    }
    info.setMillisSinceMidnight(milliSecondsSinceMidnight);

    if (!hasDate) {
      final int hour = (int) (milliSecondsSinceMidnight / (60 * 60 * 1000));
      final int minute = (int) ((milliSecondsSinceMidnight - hour * 60 * 60 * 1000) / (60 * 1000));
      final int seconds = (int) ((milliSecondsSinceMidnight - hour * 60 * 60 * 1000 - minute * 60 * 1000) / 1000);
      final int millis = (int) (milliSecondsSinceMidnight - hour * 60 * 60 * 1000 - minute * 60 * 1000 - seconds * 1000);

      info.setHour(hour);
      info.setMinute(minute);
      info.setSeconds(seconds);
      info.setMillis(millis);

      return info;
    }

    final long baseTime = InnardsConverter.fromJulianDay(julianDay, 0);

    final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$

    final long epochTimeMillis = baseTime + milliSecondsSinceMidnight;
    cal.setTimeInMillis(epochTimeMillis);
    info.setEpochTimeMillis(epochTimeMillis);

    final int day = cal.get(Calendar.DATE);
    final int month = cal.get(Calendar.MONTH);
    final int year = cal.get(Calendar.YEAR);

    info.setDay(day);
    info.setMonth(month);
    info.setYear(year);

    return info;
  }

  /**
   * Converts the provided 10-millisecond "ticks" value to a {@link LocalTime}.
   * 
   * @param ticks a positive integer of 10-millisecond "ticks" since midnight
   * @return a {@link LocalTime} object representing the value
   * @since 1.0.24
   */
  public static LocalTime ticksToLocalTime(final long ticks) {
    if (ticks < 0) {
      throw new IllegalArgumentException("ticks must be non-negative");
    }
    final long nano = ticks * 10 * 1000 * 1000;
    return LocalTime.ofNanoOfDay(nano);
  }

  /**
   * Converts the innards array to a Java {@link LocalDate}
   * 
   * @param innards innards
   * @return local date, returns null if innards do not contain a date
   */
  public static LocalDate toJavaDate(final int[] innards) {
    if (InnardsConverter.isAnyDate(innards)) {
      return null;
    }

    final InnardsInfo innardsInfo = InnardsConverter.parseInnards(innards);
    return LocalDate.of(innardsInfo.getYear(), innardsInfo.getMonth(), innardsInfo.getDay());
  }

  /**
   * Converts the innards array to a Java {@link OffsetDateTime}.
   * 
   * @param innards innards
   * @return offset date time, if innards have no date and time, we return null,
   *         for time only values we use today's date, for date only values, we
   *         use 00:00:00 as time
   */
  public static OffsetDateTime toJavaDateTime(final int[] innards) {
    if (InnardsConverter.isAnyDate(innards) && InnardsConverter.isAnyTime(innards)) {
      return null;
    }
    final InnardsInfo innardsInfo = InnardsConverter.parseInnards(innards);
    final long epochTimeMillis = innardsInfo.getEpochTimeMillis();
    final ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(innardsInfo.getHoursOffset(),
        innardsInfo.getIntervalsOf15Minutes() * 15);
    final OffsetDateTime dateTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochTimeMillis),
        ZoneId.ofOffset("UTC", zoneOffset)); //$NON-NLS-1$

    return dateTime;
  }

  /**
   * Converts the innards array to a Java {@link LocalTime}
   * 
   * @param innards innards array
   * @return local time, returns null if innards do not contain a time
   */
  public static LocalTime toJavaTime(final int[] innards) {
    if (InnardsConverter.isAnyTime(innards)) {
      return null;
    }

    final InnardsInfo innardsInfo = InnardsConverter.parseInnards(innards);
    return LocalTime.of(innardsInfo.getHour(), innardsInfo.getMinute(), innardsInfo.getSeconds(),
        innardsInfo.getMillis() * 1000000);
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

		if (!hasDate) {
			//set desired daylight-saving time to appropriate value
			time.dst=0;//NotesDateTimeUtils.isDaylightTime() ? 1 : 0;
			// set desired time zone to appropriate value
			time.zone=0; //NotesDateTimeUtils.getGMTOffset();
		}
		else {
			if (hasTime) {
				//set desired daylight-saving time to appropriate value, here UTC
				time.dst=0;
				// set desired time zone to appropriate value, here UTC
				time.zone=0;
			}
			else {
				TimeZone tz = TimeZone.getDefault();
				time.dst = tz.useDaylightTime() ? 1 : 0;
				int tzRawOffset = tz.getRawOffset();
				int tzOffsetHours = (int)(tzRawOffset / 3600000);
				time.zone = -1*tzOffsetHours;
			}
		}

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

		Calendar cal;

		if (hasTime && hasDate) {
			// set date and time
			cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			cal.set((int) year,(int) month,(int) date,(int) hour,(int) minute,(int) second);
			cal.set(Calendar.MILLISECOND, (int) millisecond);
		}
		else if (!hasTime) {
			// set date only
			cal = Calendar.getInstance();
			NotesDateTimeUtils.setAnyTime(cal);

			// set date
			cal.set((int) year,(int) month,(int) date);
		}
		else {
			// set time only
			cal = Calendar.getInstance();
			
			cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), hour, minute, second);
			cal.set(Calendar.MILLISECOND, (int) millisecond);

			NotesDateTimeUtils.setAnyDate(cal);
		}

		return cal;
	}  
}

