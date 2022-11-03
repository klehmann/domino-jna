package com.mindoo.domino.jna;

import java.util.Calendar;
import java.util.EnumSet;

import com.mindoo.domino.jna.constants.ScheduleAttr;
import com.mindoo.domino.jna.internal.structs.NotesSchedEntryExtStruct;
import com.mindoo.domino.jna.internal.structs.NotesSchedEntryStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDatePairStruct;

/**
 * Entry of a schedule list retrieved via {@link NotesSchedule#extractScheduleList(NotesTimeDate, NotesTimeDate)}
 * 
 * @author Karsten Lehmann
 */
public class NotesScheduleEntry {
	/** UNID of the entry */
	private String m_unid;
	/** Interval of the entry */	
	private NotesTimeDate[] m_interval;
	/** {@link ScheduleAttr} attributes defined by Notes */
	private EnumSet<ScheduleAttr> m_attr;
	/** Application specific attributes */
	private byte m_userAttr;
	/** ApptUNID of the entry */
	private String apptUnid;
	/** Size of this entry (for future ease of expansion) */
	private int dwEntrySize;
	/** Longitude coordinate value */
	private double nLongitude;
	/** Latitude coordinate value */
	private double nLatitude;

	public NotesScheduleEntry(IAdaptable adaptable) {
		{
			NotesSchedEntryStruct entry = adaptable.getAdapter(NotesSchedEntryStruct.class);		
			if (entry!=null) {
				this.m_unid = entry.Unid==null ? null : entry.Unid.toString();
				NotesTimeDatePairStruct intervalTDPair = entry.Interval;
				if (intervalTDPair!=null && intervalTDPair.Lower!=null && intervalTDPair.Upper!=null) {
					this.m_interval = new NotesTimeDate[] {
							new NotesTimeDate(intervalTDPair.Lower.Innards),
							new NotesTimeDate(intervalTDPair.Upper.Innards)
					};
				}
				int attrAsInt = (int) (entry.Attr & 0xff);
				this.m_attr = EnumSet.noneOf(ScheduleAttr.class);
				for (ScheduleAttr currAttr : ScheduleAttr.values()) {
					if ((attrAsInt & currAttr.getValue())==currAttr.getValue()) {
						this.m_attr.add(currAttr);
					}
				}
				this.m_userAttr = entry.UserAttr;
			}
		}
		{
			NotesSchedEntryExtStruct entryExt = adaptable.getAdapter(NotesSchedEntryExtStruct.class);
			if (entryExt!=null) {
				this.m_unid = entryExt.Unid==null ? null : entryExt.Unid.toString();
				NotesTimeDatePairStruct intervalTDPair = entryExt.Interval;
				if (intervalTDPair!=null && intervalTDPair.Lower!=null && intervalTDPair.Upper!=null) {
					this.m_interval = new NotesTimeDate[] {
							new NotesTimeDate(intervalTDPair.Lower.Innards),
							new NotesTimeDate(intervalTDPair.Upper.Innards)
					};
				}
				int attrAsInt = (int) (entryExt.Attr & 0xff);
				this.m_attr = EnumSet.noneOf(ScheduleAttr.class);
				for (ScheduleAttr currAttr : ScheduleAttr.values()) {
					if ((attrAsInt & currAttr.getValue())==currAttr.getValue()) {
						this.m_attr.add(currAttr);
					}
				}
				this.m_userAttr = entryExt.UserAttr;
				this.apptUnid = entryExt.ApptUnid==null ? null : entryExt.ApptUnid.toString();
				this.dwEntrySize = entryExt.dwEntrySize;
				this.nLongitude = entryExt.nLongitude;
				this.nLatitude = entryExt.nLatitude;
			}
		}
	}

	public String getUnid() {
		return m_unid;
	}

	@Deprecated
	public Calendar getFrom() {
		return m_interval==null ? null : m_interval[0].toCalendar();
	}
	
	public NotesTimeDate getFromAsTimeDate() {
		return m_interval==null ? null : m_interval[0];
	}
	
	@Deprecated
	public Calendar getUntil() {
		return m_interval==null ? null : m_interval[1].toCalendar();
	}

	public NotesTimeDate getUntilAsTimeDate() {
		return m_interval==null ? null : m_interval[1];
	}
	
	public EnumSet<ScheduleAttr> getAttributes() {
		return m_attr;
	}

	public boolean hasAttribute(ScheduleAttr attr) {
		return m_attr!=null && m_attr.contains(attr);
	}

	public String getApptUnid() {
		return apptUnid;
	}

	public boolean isPenciled() {
		return m_attr!=null && m_attr.contains(ScheduleAttr.PENCILED);
	}

	public boolean isRepeatEvent() {
		return m_attr!=null && m_attr.contains(ScheduleAttr.REPEATEVENT);
	}

	public boolean isBusy() {
		return m_attr!=null && m_attr.contains(ScheduleAttr.BUSY);
	}
	
	public boolean isAppointment() {
		return m_attr!=null && m_attr.contains(ScheduleAttr.APPT);
	}
	
	public boolean isNonWork() {
		return m_attr!=null && m_attr.contains(ScheduleAttr.NONWORK);
	}
	
	@Override
	public String toString() {
		return "NotesScheduleEntry [UNID="+m_unid+", from="+(m_interval==null ? "null" : m_interval[0])+", until="+(m_interval==null ? "null" : m_interval[1])+"]";
	}
}
