package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

/**
 * Schedule Query APIs
 * 
 * @author Karsten Lehmann
 */
public enum ScheduleOptions {
	
	/** Return composite sched */
	COMPOSITE(0x0001),
	
	/** Return each person's sched */
	EACHPERSON(0x0002),
	
	/** Do only local lookup */
	LOCAL(0x0004),
	
	/** force remote even if you are using workstation based email */
	FORCEREMOTE(0x0020);
	
	private int m_val;
	
	ScheduleOptions(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static short toBitMask(EnumSet<ScheduleOptions> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (ScheduleOptions currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return (short) (result & 0xffff);
	}
	
	public static int toBitMaskInt(EnumSet<ScheduleOptions> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (ScheduleOptions currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
