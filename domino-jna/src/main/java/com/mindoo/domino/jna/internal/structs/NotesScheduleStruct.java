package com.mindoo.domino.jna.internal.structs;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Data structure for a schedule.
 */
public class NotesScheduleStruct extends BaseStructure implements IAdaptable {
	/** C type : DWORD[8] */
	public int[] reserved = new int[8];
	/**
	 * Users mail file replica ID<br>
	 * C type : DBID
	 */
	public NotesTimeDateStruct dbReplicaID;
	/**
	 * events etc. are in this<br>
	 * interval<br>
	 * C type : TIMEDATE_PAIR
	 */
	public NotesTimeDatePairStruct Interval;
	/**
	 * gateway error retrieving this<br>
	 * schedule
	 */
	public int dwErrGateway;
	/**
	 * error retrieving this<br>
	 * schedule<br>
	 * C type : STATUS
	 */
	public short error;
	/** unused at this time */
	public short wReserved;
	/**
	 * size of owner name<br>
	 * (includes term.)
	 */
	public short wOwnerNameSize;

	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesScheduleStruct() {
		super();
	}
	
	public static NotesScheduleStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesScheduleStruct>() {

			@Override
			public NotesScheduleStruct run() {
				return new NotesScheduleStruct();
			}
		});
	}

	protected List getFieldOrder() {
		return Arrays.asList("reserved", "dbReplicaID", "Interval", "dwErrGateway", "error", "wReserved", "wOwnerNameSize");
	}
	/**
	 * @param reserved C type : DWORD[8]<br>
	 * @param dbReplicaID Users mail file replica ID<br>
	 * C type : DBID<br>
	 * @param Interval events etc. are in this<br>
	 * interval<br>
	 * C type : TIMEDATE_PAIR<br>
	 * @param dwErrGateway gateway error retrieving this<br>
	 * schedule<br>
	 * @param error error retrieving this<br>
	 * schedule<br>
	 * C type : STATUS<br>
	 * @param wReserved unused at this time<br>
	 * @param wOwnerNameSize size of owner name<br>
	 * (includes term.)
	 * 
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesScheduleStruct(int reserved[], NotesTimeDateStruct dbReplicaID, NotesTimeDatePairStruct Interval, int dwErrGateway, short error,
			short wReserved, short wOwnerNameSize) {
		super();
		if ((reserved.length != this.reserved.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.reserved = reserved;
		this.dbReplicaID = dbReplicaID;
		this.Interval = Interval;
		this.dwErrGateway = dwErrGateway;
		this.error = error;
		this.wReserved = wReserved;
		this.wOwnerNameSize = wOwnerNameSize;
	}
	
	public static NotesScheduleStruct newInstance(final int reserved[], final NotesTimeDateStruct dbReplicaID, final NotesTimeDatePairStruct interval, final int dwErrGateway, final short error,
			final short wReserved, final short wOwnerNameSize) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesScheduleStruct>() {

			@Override
			public NotesScheduleStruct run() {
				return new NotesScheduleStruct(reserved, dbReplicaID, interval, dwErrGateway, error,
						wReserved, wOwnerNameSize);
			}
		});
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesScheduleStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesScheduleStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesScheduleStruct>() {

			@Override
			public NotesScheduleStruct run() {
				return new NotesScheduleStruct(peer);
			}
		});
	}
	
	public static class ByReference extends NotesScheduleStruct implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesScheduleStruct implements Structure.ByValue {
		
	}
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesScheduleStruct.class) {
			return (T) this;
		}
		else if (clazz == Pointer.class) {
			return (T) getPointer();
		}
		return null;
	};
}
