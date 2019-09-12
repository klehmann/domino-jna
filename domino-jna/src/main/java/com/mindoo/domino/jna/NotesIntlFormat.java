package com.mindoo.domino.jna;

import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.structs.IntlFormatStruct;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils.LineBreakConversion;
import com.sun.jna.Pointer;

/**
 * The {@link NotesIntlFormat} is used to declare a data structure with country and workstation dependant information.<br>
 * <br>
 * This structure provides information useful in parsing or composing Time, Date, and Currency strings.<br>
 * <br>
 * It is also used by {@link NotesTimeDate#toString(NotesIntlFormat, com.mindoo.domino.jna.constants.DateFormat, com.mindoo.domino.jna.constants.TimeFormat, com.mindoo.domino.jna.constants.ZoneFormat, com.mindoo.domino.jna.constants.DateTimeStructure)}
 * to indicate the expected or resulting string format, current time zone and daylight savings values.<br>
 * <br>
 * Note: The function {@link NotesTimeDate#toString(NotesIntlFormat, com.mindoo.domino.jna.constants.DateFormat, com.mindoo.domino.jna.constants.TimeFormat, com.mindoo.domino.jna.constants.ZoneFormat, com.mindoo.domino.jna.constants.DateTimeStructure)}
 * does not take the DAYLIGHT_SAVINGS ({@link #setDST(boolean)}) flag into account.

 * @author Karsten Lehmann
 */
public class NotesIntlFormat implements IAdaptable {
	private IntlFormatStruct m_struct;
	
	/**
	 * Creates a new instance
	 * 
	 * @param adaptable adaptable providing the internal state
	 */
	public NotesIntlFormat(IAdaptable adaptable) {
		IntlFormatStruct struct = adaptable.getAdapter(IntlFormatStruct.class);
		if (struct!=null) {
			m_struct = struct;
			return;
		}
		Pointer p = adaptable.getAdapter(Pointer.class);
		if (p!=null) {
			m_struct = IntlFormatStruct.newInstance(p);
			return;
		}
		throw new IllegalArgumentException("Constructor argument cannot provide a supported datatype");
	
	}

	/**
	 * Creates a new instance with the current machines default internationalization
	 * settings. The settings can then be changed by calling the available setters.
	 */
	public NotesIntlFormat() {
		m_struct = IntlFormatStruct.newInstance();
		NotesNativeAPI.get().OSGetIntlSettings(m_struct, (short) (NotesConstants.intlFormatSize & 0xffff));
		m_struct.read();
	}
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (IntlFormatStruct.class.equals(clazz)) {
			return (T) m_struct;
		}
		return null;
	}
	
	public boolean isCurrencySuffix() {
		return (m_struct.Flags & NotesConstants.CURRENCY_SUFFIX) == NotesConstants.CURRENCY_SUFFIX;
	}
	
	public void setCurrencySuffix(boolean b) {
		if (isCurrencySuffix() == b) {
			return;
		}
		
		if (b) {
			m_struct.Flags = (short) ((m_struct.Flags | NotesConstants.CURRENCY_SUFFIX) & 0xffff);
		}
		else {
			m_struct.Flags =~ NotesConstants.CURRENCY_SUFFIX;
		}
		m_struct.write();
	}
	
	public boolean isCurrencySpace() {
		return (m_struct.Flags & NotesConstants.CURRENCY_SPACE) == NotesConstants.CURRENCY_SPACE;
	}

	public void setCurrencySpace(boolean b) {
		if (isCurrencySpace() == b) {
			return;
		}
		
		if (b) {
			m_struct.Flags = (short) ((m_struct.Flags | NotesConstants.CURRENCY_SPACE) & 0xffff);
		}
		else {
			m_struct.Flags =~ NotesConstants.CURRENCY_SPACE;
		}
		m_struct.write();
	}

	public boolean isNumberLeadingZero() {
		return (m_struct.Flags & NotesConstants.NUMBER_LEADING_ZERO) == NotesConstants.NUMBER_LEADING_ZERO;
	}

	public void setNumberLeadingZero(boolean b) {
		if (isNumberLeadingZero() == b) {
			return;
		}
		
		if (b) {
			m_struct.Flags = (short) ((m_struct.Flags | NotesConstants.NUMBER_LEADING_ZERO) & 0xffff);
		}
		else {
			m_struct.Flags =~ NotesConstants.NUMBER_LEADING_ZERO;
		}
		m_struct.write();
	}

	public boolean isTime24Hour() {
		return (m_struct.Flags & NotesConstants.CLOCK_24_HOUR) == NotesConstants.CLOCK_24_HOUR;
	}
	
	public void setTime24Hour(boolean b) {
		if (isTime24Hour() == b) {
			return;
		}
		
		if (b) {
			m_struct.Flags = (short) ((m_struct.Flags | NotesConstants.CLOCK_24_HOUR) & 0xffff);
		}
		else {
			m_struct.Flags =~ NotesConstants.CLOCK_24_HOUR;
		}
		m_struct.write();
	}
	
	public boolean isDST() {
		return (m_struct.Flags & NotesConstants.DAYLIGHT_SAVINGS) == NotesConstants.DAYLIGHT_SAVINGS;
	}
	
	public void setDST(boolean b) {
		if (isDST() == b) {
			return;
		}
		
		if (b) {
			m_struct.Flags = (short) ((m_struct.Flags | NotesConstants.DAYLIGHT_SAVINGS) & 0xffff);
		}
		else {
			m_struct.Flags =~ NotesConstants.DAYLIGHT_SAVINGS;
		}
		m_struct.write();
	}
	
	public boolean isDateMDY() {
		return (m_struct.Flags & NotesConstants.DATE_MDY) == NotesConstants.DATE_MDY;
	}
	
	public void setDateMDY(boolean b) {
		if (isDateMDY() == b) {
			return;
		}
		
		if (b) {
			m_struct.Flags = (short) ((m_struct.Flags | NotesConstants.DATE_MDY) & 0xffff);
		}
		else {
			m_struct.Flags =~ NotesConstants.DATE_MDY;
		}
		m_struct.write();
	}
	
	public boolean isDateDMY() {
		return (m_struct.Flags & NotesConstants.DATE_DMY) == NotesConstants.DATE_DMY;
	}
	
	public void setDateDMY(boolean b) {
		if (isDateDMY() == b) {
			return;
		}
		if (b) {
			m_struct.Flags = (short) ((m_struct.Flags | NotesConstants.DATE_DMY) & 0xffff);
		}
		else {
			m_struct.Flags =~ NotesConstants.DATE_DMY;
		}
		m_struct.write();
	}
	
	public boolean isDateYMD() {
		return (m_struct.Flags & NotesConstants.DATE_YMD) == NotesConstants.DATE_YMD;
	}
	
	public void setDateYMD(boolean b) {
		if (isDateYMD() == b) {
			return;
		}
		
		if (b) {
			m_struct.Flags = (short) ((m_struct.Flags | NotesConstants.DATE_YMD) & 0xffff);
		}
		else {
			m_struct.Flags =~ NotesConstants.DATE_YMD;
		}
		m_struct.write();
	}

	public boolean is4DigitYear() {
		return (m_struct.Flags & NotesConstants.DATE_4DIGIT_YEAR) == NotesConstants.DATE_4DIGIT_YEAR;
	}
	
	public void set4DigitYear(boolean b) {
		if (is4DigitYear() == b) {
			return;
		}
		
		if (b) {
			m_struct.Flags = (short) ((m_struct.Flags | NotesConstants.DATE_4DIGIT_YEAR) & 0xffff);
		}
		else {
			m_struct.Flags =~ NotesConstants.DATE_4DIGIT_YEAR;
		}
		m_struct.write();
	}
	
	public boolean isAMPMPrefix() {
		return (m_struct.Flags & NotesConstants.TIME_AMPM_PREFIX) == NotesConstants.TIME_AMPM_PREFIX;
	}
	
	public void setAMPMPrefix(boolean b) {
		if (isAMPMPrefix() == b) {
			return;
		}
		
		if (b) {
			m_struct.Flags = (short) ((m_struct.Flags | NotesConstants.TIME_AMPM_PREFIX) & 0xffff);
		}
		else {
			m_struct.Flags =~ NotesConstants.TIME_AMPM_PREFIX;
		}
		m_struct.write();
	}
	
	public boolean isDateAbbreviated() {
		return (m_struct.Flags & NotesConstants.DATE_ABBREV) == NotesConstants.DATE_ABBREV;
	}
	
	public void setDateAbbreviated(boolean b) {
		if (isDateAbbreviated() == b) {
			return;
		}
		
		if (b) {
			m_struct.Flags = (short) ((m_struct.Flags | NotesConstants.DATE_ABBREV) & 0xffff);
		}
		else {
			m_struct.Flags =~ NotesConstants.DATE_ABBREV;
		}
		m_struct.write();
	}
	
	/**
	 * Number of decimal digits in fractional monetary amounts
	 * 
	 * @return digits
	 */
	public int getCurrencyDigits() {
		return (int) (m_struct.CurrencyDigits & 0xff);
	}
	
	public void setCurrencyDigits(int digits) {
		if (getCurrencyDigits() == digits) {
			return;
		}
		
		if (digits<0 || digits>255) {
			throw new IllegalArgumentException("Digits must be between 0 and 255");
		}
		m_struct.CurrencyDigits = (byte) (digits & 0xff);
		m_struct.write();
	}
	
	/**
	 * number of hours added to the time to get Greenwich Mean Time. May be positive or negative.
	 * 
	 * @return timezone
	 */
	public int getTimeZone() {
		return m_struct.TimeZone;
	}
	
	public void setTimeZone(int timezone) {
		m_struct.TimeZone = timezone;
		m_struct.write();
	}
	
	private void fitLMBCSEncodedStringIntoByteArray(String str, byte[] targetArray) {
		for (int i=str.length(); i>=0; i--) {
			String currSubStr = i==str.length() ? str : str.substring(0, i);
			DisposableMemory mem = NotesStringUtils.toLMBCSNoCache(currSubStr, true, LineBreakConversion.ORIGINAL);
			try {
				if (mem.size() <= targetArray.length) {
					//size ok
					mem.read(0, targetArray, 0,  (int) mem.size());
					m_struct.write();
					return;
				}
			}
			finally {
				mem.dispose();
			}
		}
		//should not happen
		targetArray[0] = 0;
		
		m_struct.write();
	}
	
	/**
	 * Returns AM/am string used in countries with 12 hour time format
	 * 
	 * @return AM string
	 */
	public String getAMString() {
		byte[] arr = m_struct.AMString;
		
		int length = Math.min(NotesStringUtils.getNullTerminatedLength(arr), arr.length-1);
		DisposableMemory mem = new DisposableMemory(length);
		mem.write(0, arr, 0, length);
		String str = NotesStringUtils.fromLMBCS(mem, length);
		mem.dispose();
		return str;
	}

	public void setAMString(String str) {
		fitLMBCSEncodedStringIntoByteArray(str, m_struct.AMString);
	}

	/**
	 * Returns PM/pm string used in countries with 12 hour time format
	 * 
	 * @return PM string
	 */
	public String getPMString() {
		byte[] arr = m_struct.PMString;
		
		int length = Math.min(NotesStringUtils.getNullTerminatedLength(arr), arr.length-1);
		DisposableMemory mem = new DisposableMemory(length);
		mem.write(0, arr, 0, length);
		String str = NotesStringUtils.fromLMBCS(mem, length);
		mem.dispose();
		return str;
	}

	public void setPMString(String str) {
		fitLMBCSEncodedStringIntoByteArray(str, m_struct.PMString);
	}

	/**
	 * Symbol for currency: $, Fr, SEK, etc.
	 * 
	 * @return PM string
	 */
	public String getCurrencySymbol() {
		byte[] arr = m_struct.CurrencyString;
		
		int length = Math.min(NotesStringUtils.getNullTerminatedLength(arr), arr.length-1);
		DisposableMemory mem = new DisposableMemory(length);
		mem.write(0, arr, 0, length);
		String str = NotesStringUtils.fromLMBCS(mem, length);
		mem.dispose();
		return str;
	}

	public void setCurrencySymbol(String str) {
		fitLMBCSEncodedStringIntoByteArray(str, m_struct.CurrencyString);
	}
	
	/**
	 * Symbol formatting monetary amounts in thousands
	 * 
	 * @return thousand string
	 */
	public String getThousandString() {
		byte[] arr = m_struct.ThousandString;
		
		int length = Math.min(NotesStringUtils.getNullTerminatedLength(arr), arr.length-1);
		DisposableMemory mem = new DisposableMemory(length);
		mem.write(0, arr, 0, length);
		String str = NotesStringUtils.fromLMBCS(mem, length);
		mem.dispose();
		return str;
	}

	public void setThousandString(String str) {
		fitLMBCSEncodedStringIntoByteArray(str, m_struct.ThousandString);
	}

	/**
	 * Symbol denoting decimal fraction of monetary amounts<br>
	 * 
	 * @return decimal string
	 */
	public String getDecimalString() {
		byte[] arr = m_struct.DecimalString;
		
		int length = Math.min(NotesStringUtils.getNullTerminatedLength(arr), arr.length-1);
		DisposableMemory mem = new DisposableMemory(length);
		mem.write(0, arr, 0, length);
		String str = NotesStringUtils.fromLMBCS(mem, length);
		mem.dispose();
		return str;
	}

	public void setDecimalString(String str) {
		fitLMBCSEncodedStringIntoByteArray(str, m_struct.DecimalString);
	}
	
	/**
	 * Character(s) separating components of date string
	 * 
	 * @return date string
	 */
	public String getDateSep() {
		byte[] arr = m_struct.DateString;
		
		int length = Math.min(NotesStringUtils.getNullTerminatedLength(arr), arr.length-1);
		DisposableMemory mem = new DisposableMemory(length);
		mem.write(0, arr, 0, length);
		String str = NotesStringUtils.fromLMBCS(mem, length);
		mem.dispose();
		return str;
	}

	public void setDateSep(String str) {
		fitLMBCSEncodedStringIntoByteArray(str, m_struct.DateString);
	}
	
	/**
	 * Character(s) separating components of time string
	 * 
	 * @return time string
	 */
	public String getTimeSep() {
		byte[] arr = m_struct.TimeString;
		
		int length = Math.min(NotesStringUtils.getNullTerminatedLength(arr), arr.length-1);
		DisposableMemory mem = new DisposableMemory(length);
		mem.write(0, arr, 0, length);
		String str = NotesStringUtils.fromLMBCS(mem, length);
		mem.dispose();
		return str;
	}

	public void setTimeSep(String str) {
		fitLMBCSEncodedStringIntoByteArray(str, m_struct.TimeString);
	}
	
	/**
	 * String denoting previous day
	 * 
	 * @return yesterday string
	 */
	public String getYesterdayString() {
		byte[] arr = m_struct.YesterdayString;
		
		int length = Math.min(NotesStringUtils.getNullTerminatedLength(arr), arr.length-1);
		DisposableMemory mem = new DisposableMemory(length);
		mem.write(0, arr, 0, length);
		String str = NotesStringUtils.fromLMBCS(mem, length);
		mem.dispose();
		return str;
	}

	public void setYesterdayString(String str) {
		fitLMBCSEncodedStringIntoByteArray(str, m_struct.YesterdayString);
	}
	
	/**
	 * String denoting current day
	 * 
	 * @return yesterday string
	 */
	public String getTodayString() {
		byte[] arr = m_struct.TodayString;
		
		int length = Math.min(NotesStringUtils.getNullTerminatedLength(arr), arr.length-1);
		DisposableMemory mem = new DisposableMemory(length);
		mem.write(0, arr, 0, length);
		String str = NotesStringUtils.fromLMBCS(mem, length);
		mem.dispose();
		return str;
	}

	public void setTodayString(String str) {
		fitLMBCSEncodedStringIntoByteArray(str, m_struct.TodayString);
	}
	
	/**
	 * String denoting next day
	 * 
	 * @return yesterday string
	 */
	public String getTomorrowString() {
		byte[] arr = m_struct.TomorrowString;
		
		int length = Math.min(NotesStringUtils.getNullTerminatedLength(arr), arr.length-1);
		DisposableMemory mem = new DisposableMemory(length);
		mem.write(0, arr, 0, length);
		String str = NotesStringUtils.fromLMBCS(mem, length);
		mem.dispose();
		return str;
	}

	public void setTomorrowString(String str) {
		fitLMBCSEncodedStringIntoByteArray(str, m_struct.TomorrowString);
	}

}
