package com.mindoo.domino.jna.internal.structs;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * This is the schedule data. The {@link NotesScheduleListStruct} is followed by
 * NumEntries of {@link NotesSchedEntryStruct} or {@link NotesSchedEntryExtStruct}.
 * 
 * @author Karsten Lehmann
 */
public class NotesScheduleListStruct extends BaseStructure implements IAdaptable {
	/** Total number of schedule entries follow */
	public int NumEntries;
	/** Application id for UserAttr interpretation */
	public short wApplicationID;
	/**
	 * Pre Notes/Domino 6: spare <br>
	 * Notes/Domino 6: This now conveys the length of a single<br>
	 * SCHED_ENTRY_xxx that follows.  Use this value<br>
	 * to skip entries that MAY be larger (ie: a later version may<br>
	 * extend SCHED_ENTRY_EXT by appending values<br>
	 * that Notes/Domino 6 does not know about so SCHED_ENTRY_xxx<br>
	 * would actually be larger than the Notes/Domino 6<br>
	 * SCHED_ENTRY_EXT
	 */
	public short Spare;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesScheduleListStruct() {
		super();
	}
	
	public static NotesScheduleListStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesScheduleListStruct>() {

			@Override
			public NotesScheduleListStruct run() {
				return new NotesScheduleListStruct();
			}
		});
	}
	
	protected List getFieldOrder() {
		return Arrays.asList("NumEntries", "wApplicationID", "Spare");
	}
	
	/**
	 * @param NumEntries Total number of schedule entries follow<br>
	 * @param wApplicationID Application id for UserAttr interpretation<br>
	 * @param Spare Pre Notes/Domino 6: spare <br>
	 * Notes/Domino 6: This now conveys the length of a single<br>
	 * SCHED_ENTRY_xxx that follows.  Use this value<br>
	 * to skip entries that MAY be larger (ie: a later version may<br>
	 * extend SCHED_ENTRY_EXT by appending values<br>
	 * that Notes/Domino 6 does not know about so SCHED_ENTRY_xxx<br>
	 * would actually be larger than the Notes/Domino 6<br>
	 * SCHED_ENTRY_EXT
	 * 
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesScheduleListStruct(int NumEntries, short wApplicationID, short Spare) {
		super();
		this.NumEntries = NumEntries;
		this.wApplicationID = wApplicationID;
		this.Spare = Spare;
	}
	
	public static NotesScheduleListStruct newInstance(final int numEntries, final short wApplicationID, final short spare) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesScheduleListStruct>() {

			@Override
			public NotesScheduleListStruct run() {
				return new NotesScheduleListStruct(numEntries, wApplicationID, spare);
			}
		});
	}
	
	public NotesScheduleListStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesScheduleListStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesScheduleListStruct>() {

			@Override
			public NotesScheduleListStruct run() {
				return new NotesScheduleListStruct(peer);
			}
		});
	}
	
	public static class ByReference extends NotesScheduleListStruct implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesScheduleListStruct implements Structure.ByValue {
		
	};
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesScheduleListStruct.class) {
			return (T) this;
		}
		else if (clazz == Pointer.class) {
			return (T) getPointer();
		}
		return null;
	}

}
