package com.mindoo.domino.jna;

import java.util.Calendar;
import java.util.Date;

/**
 * A {@link NotesDateRange} represents a range of dates and times.
 * 
 * @author Karsten Lehmann
 */
public class NotesDateRange {
	private NotesTimeDate m_startDateTime;
	private NotesTimeDate m_endDateTime;
	
	/**
	 * Creates a new date range
	 * 
	 * @param startDateTime start date time, not null
	 * @param endDateTime end date time, not null
	 */
	public NotesDateRange(Calendar startDateTime, Calendar endDateTime) {
		if (startDateTime==null)
			throw new NullPointerException("startDateTime cannot be null");
		if (endDateTime==null)
			throw new NullPointerException("endDateTime cannot be null");
		m_startDateTime = new NotesTimeDate(startDateTime);
		m_endDateTime = new NotesTimeDate(endDateTime);
	}
	
	/**
	 * Creates a new date range
	 * 
	 * @param startDateTime start date time, not null
	 * @param endDateTime end date time, not null
	 */
	public NotesDateRange(Date startDateTime, Date endDateTime) {
		if (startDateTime==null)
			throw new NullPointerException("startDateTime cannot be null");
		if (endDateTime==null)
			throw new NullPointerException("endDateTime cannot be null");
		m_startDateTime = new NotesTimeDate(startDateTime);
		m_endDateTime = new NotesTimeDate(endDateTime);
	}

	/**
	 * Creates a new date range
	 * 
	 * @param startDateTime start date time, not null
	 * @param endDateTime end date time, not null
	 */
	public NotesDateRange(NotesTimeDate startDateTime, NotesTimeDate endDateTime) {
		if (startDateTime==null)
			throw new NullPointerException("startDateTime cannot be null");
		if (endDateTime==null)
			throw new NullPointerException("endDateTime cannot be null");
		m_startDateTime = startDateTime;
		m_endDateTime = endDateTime;
	}
	
	public NotesTimeDate getStartDateTime() {
		return m_startDateTime;
	}
	
	public NotesTimeDate getEndDateTime() {
		return m_endDateTime;
	}
}
