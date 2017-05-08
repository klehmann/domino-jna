package com.mindoo.domino.jna.structs;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * TThis is the extended data structure that describes an individual schedule entry.
 */
public class NotesSchedEntryExtStruct extends BaseStructure implements IAdaptable {
	/** C type : UNID */
	public NotesUniversalNoteIdStruct Unid;
	/** C type : TIMEDATE_PAIR */
	public NotesTimeDatePairStruct Interval;
	public byte Attr;
	public byte UserAttr;
	/** C type : BYTE[2] */
	public byte[] spare = new byte[2];

	/* Everything above this point is the same as NotesSchedEntryStruct for preR6 clients!
	 * Everything from here on down is R6 (or later) only! */

	public NotesUniversalNoteIdStruct ApptUnid;   /* ApptUNID of the entry */
	public int dwEntrySize; /* Size of this entry (for future ease of expansion) */
	public double nLongitude;     /* Longitude coordinate value */
	public double nLatitude;      /* Latitude coordinate value */
	    
	/**
	 * Creates a new entry
	 * 
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesSchedEntryExtStruct() {
		super();
	}
	
	public static NotesSchedEntryExtStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesSchedEntryExtStruct>() {

			@Override
			public NotesSchedEntryExtStruct run() {
				return new NotesSchedEntryExtStruct();
			}
		});
	}
	
	protected List getFieldOrder() {
		return Arrays.asList("Unid", "Interval", "Attr", "UserAttr", "spare", "ApptUnid", "dwEntrySize",
				"nLongitude", "nLatitude");
	}
	
	/**
	 * Creates a new entry
	 * 
	 * @param Unid C type : UNID<br>
	 * @param Interval C type : TIMEDATE_PAIR<br>
	 * @param Attr SCHED_ATTR_xxx attributes defined by Notes
	 * @param UserAttr Application specific attributes
	 * @param spare C type : BYTE[2]
	 * @param ApptUnid ApptUNID of the appointment note
	 * @param dwEntrySize Size of this entry (for future ease of expansion)
	 * @param nLongitude Geographical coordinates of the entry: longitude
	 * @param nLatitude Geographical coordinates of the entry: latitude
	 * 
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesSchedEntryExtStruct(NotesUniversalNoteIdStruct Unid, NotesTimeDatePairStruct Interval,
			byte Attr, byte UserAttr, byte spare[],
			NotesUniversalNoteIdStruct ApptUnid, int dwEntrySize, double nLongitude, double nLatitude) {
		super();
		this.Unid = Unid;
		this.Interval = Interval;
		this.Attr = Attr;
		this.UserAttr = UserAttr;
		if ((spare.length != this.spare.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.spare = spare;
		this.ApptUnid = ApptUnid;
		this.dwEntrySize = dwEntrySize;
		this.nLongitude = nLongitude;
		this.nLatitude = nLatitude;
	}
	
	public static NotesSchedEntryExtStruct newInstance(final NotesUniversalNoteIdStruct Unid,
			final NotesTimeDatePairStruct Interval, final byte Attr, final byte UserAttr, final byte[] spare,
			final NotesUniversalNoteIdStruct ApptUnid, final int dwEntrySize, final double nLongitude, final double nLatitude) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesSchedEntryExtStruct>() {

			@Override
			public NotesSchedEntryExtStruct run() {
				return new NotesSchedEntryExtStruct(Unid, Interval, Attr, UserAttr, spare, ApptUnid,
						dwEntrySize, nLongitude, nLatitude);
			}
		});
	}
	
	/**
	 * Creates a new entry
	 * 
	 * @param peer pointer
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesSchedEntryExtStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesSchedEntryExtStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesSchedEntryExtStruct>() {

			@Override
			public NotesSchedEntryExtStruct run() {
				return new NotesSchedEntryExtStruct(peer);
			}
		});
	}
	
	public static class ByReference extends NotesSchedEntryExtStruct implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesSchedEntryExtStruct implements Structure.ByValue {
		
	};
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesSchedEntryExtStruct.class) {
			return (T) this;
		}
		else if (clazz == Pointer.class) {
			return (T) getPointer();
		}
		return null;
	}
	
}
