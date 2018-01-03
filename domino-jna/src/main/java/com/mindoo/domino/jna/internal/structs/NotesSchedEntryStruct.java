package com.mindoo.domino.jna.internal.structs;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * This is the data structure that describes an individual schedule entry.
 */
public class NotesSchedEntryStruct extends BaseStructure implements IAdaptable {
	/** C type : UNID */
	public NotesUniversalNoteIdStruct Unid;
	/** C type : TIMEDATE_PAIR */
	public NotesTimeDatePairStruct Interval;
	public byte Attr;
	public byte UserAttr;
	/** C type : BYTE[2] */
	public byte[] spare = new byte[2];
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesSchedEntryStruct() {
		super();
	}
	
	public static NotesSchedEntryStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesSchedEntryStruct>() {

			@Override
			public NotesSchedEntryStruct run() {
				return new NotesSchedEntryStruct();
			}
		});
	}
	
	protected List getFieldOrder() {
		return Arrays.asList("Unid", "Interval", "Attr", "UserAttr", "spare");
	}
	
	/**
	 * @param Unid C type : UNID<br>
	 * @param Interval C type : TIMEDATE_PAIR<br>
	 * @param spare C type : BYTE[2]
	 * @param Attr attributes
	 * @param UserAttr user defined attributes
	 * @param spare unused bytes
	 * 
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesSchedEntryStruct(NotesUniversalNoteIdStruct Unid, NotesTimeDatePairStruct Interval, byte Attr, byte UserAttr, byte spare[]) {
		super();
		this.Unid = Unid;
		this.Interval = Interval;
		this.Attr = Attr;
		this.UserAttr = UserAttr;
		if ((spare.length != this.spare.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.spare = spare;
	}
	
	public static NotesSchedEntryStruct newInstance(final NotesUniversalNoteIdStruct Unid, final NotesTimeDatePairStruct Interval, final byte Attr, final byte UserAttr, final byte[] spare) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesSchedEntryStruct>() {

			@Override
			public NotesSchedEntryStruct run() {
				return new NotesSchedEntryStruct(Unid, Interval, Attr, UserAttr, spare);
			}
		});
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesSchedEntryStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesSchedEntryStruct newInstance(final Pointer peer) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesSchedEntryStruct>() {

			@Override
			public NotesSchedEntryStruct run() {
				return new NotesSchedEntryStruct(peer);
			}
		});
	}
	
	public static class ByReference extends NotesSchedEntryStruct implements Structure.ByReference {
		
	};
	public static class ByValue extends NotesSchedEntryStruct implements Structure.ByValue {
		
	};
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesSchedEntryStruct.class) {
			return (T) this;
		}
		else if (clazz == Pointer.class) {
			return (T) getPointer();
		}
		return null;
	}
	
}
