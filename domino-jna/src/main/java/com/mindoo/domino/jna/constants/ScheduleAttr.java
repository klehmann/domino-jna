package com.mindoo.domino.jna.constants;

import java.util.EnumSet;

import com.mindoo.domino.jna.NotesScheduleEntry;

/**
 * Available attributes of {@link NotesScheduleEntry} objects
 * 
 * @author Karsten Lehmann
 */
public enum ScheduleAttr {
	/** Used by gateways to return foreign UNIDs */
	FOREIGNUNID(0x10),
	/** Used by V5 C&amp;S to identify new repeating meetings */
	REPEATEVENT(0x20),

	/* these are the entry type bits */
	
	/** Entry types that block off busy time. */
	BUSY(0x08),

	/** Entry types that don't block off busy time */
	PENCILED(0x01),

	/* Entry types that block off busy time */
	
	APPT(0x08 + 0x00),
	NONWORK(0x08 + 0x01);
	
	private int m_val;
	
	ScheduleAttr(int val) {
		m_val = val;
	}
	
	public int getValue() {
		return m_val;
	}
	
	public static byte toBitMask(EnumSet<ScheduleAttr> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (ScheduleAttr currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return (byte) (result & 0xff);
	}
	
	public static int toBitMaskInt(EnumSet<ScheduleAttr> findSet) {
		int result = 0;
		if (findSet!=null) {
			for (ScheduleAttr currFind : values()) {
				if (findSet.contains(currFind)) {
					result = result | currFind.getValue();
				}
			}
		}
		return result;
	}

}
