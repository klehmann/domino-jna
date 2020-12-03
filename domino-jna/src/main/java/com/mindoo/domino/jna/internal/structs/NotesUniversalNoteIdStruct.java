package com.mindoo.domino.jna.internal.structs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
/**
 * The Universal Note ID (UNID) identifies all copies of the same note in different replicas of the same
 * database universally (across all servers).<br>
 * <br>
 * If one note in one database has the same UNID as another note in a replica database, then the
 * two notes are replicas of each other.<br>
 * <br>
 * The UNID is used to reference a specific note from another note. Specifically, the FIELD_LINK ($REF)
 * field of a response note contains the UNID of it's parent.<br>
 * <br>
 * Similarly, Doc Links (see NOTELINK) contains the UNID of the linked-to note plus the database ID
 * where the linked-to note can be found. The important characteristic of the UNID is that it
 * continues to reference a specific note even if the note being referenced is updated.<br>
 * <br>
 * The Domino replicator uses the Universal Note ID to match the notes in one database with
 * their respective copies in replica databases. For example, if database A is a replica copy
 * of database B, database A contains a note with a particular UNID, and database B contains
 * a note with the same UNID, then the replicator concludes that these two notes are replica
 * copies of one another. On the other hand, if database A contains a note with a particular
 * UNID but database B does not, then the replicator will create a copy of that note and
 * add it to database B.<br>
 * <br>
 * One database must never contain two notes with the same UNID. If the replicator finds two
 * notes with the same UNID in the same database, it generates an error message in the log
 * and does not replicate the document.<br>
 * <br>
 * The "File" member of the UNID contains a number derived in different ways depending on
 * the release of Domino or Notes.<br>
 * Pre- 2.1 versions of Notes set the "File" member to the creation timedate of the NSF file
 * in which the note is created. Notes 2.1 sets the "File" member to a user-unique identifier,
 * derived in part from information in the ID of the user creating the note, and in part
 * from the database where the note is created.  Notes 3.0 sets the "File" member to a
 * random number generated at the time the note is created.<br>
 * <br>
 * The "Note" member of the UNID contains the date/time when the very first copy of the note
 * was stored into the first NSF (Note: date/time from $CREATED item, if exists, takes precedence).
 */
public class NotesUniversalNoteIdStruct extends BaseStructure implements IAdaptable {
	/** C type : DBID */
	public NotesTimeDateStruct File;
	/** C type : TIMEDATE */
	public NotesTimeDateStruct Note;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesUniversalNoteIdStruct() {
		super();
	}
	
	public static NotesUniversalNoteIdStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesUniversalNoteIdStruct>() {

			@Override
			public NotesUniversalNoteIdStruct run() {
				return new NotesUniversalNoteIdStruct();
			}
		});
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("File", "Note");
	}
	
	/**
	 * @param File C type : DBID
	 * @param Note C type : TIMEDATE
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesUniversalNoteIdStruct(NotesTimeDateStruct File, NotesTimeDateStruct Note) {
		super();
		this.File = File;
		this.Note = Note;
	}

	public static NotesUniversalNoteIdStruct newInstance(final NotesTimeDateStruct File, final NotesTimeDateStruct Note) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesUniversalNoteIdStruct>() {

			@Override
			public NotesUniversalNoteIdStruct run() {
				return new NotesUniversalNoteIdStruct(File, Note);
			}
		});
	}

	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesUniversalNoteIdStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesUniversalNoteIdStruct newInstance(final Pointer p) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesUniversalNoteIdStruct>() {

			@Override
			public NotesUniversalNoteIdStruct run() {
				return new NotesUniversalNoteIdStruct(p);
			}
		});
	}
	
	public static NotesUniversalNoteIdStruct.ByReference newInstanceByReference() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesUniversalNoteIdStruct.ByReference>() {

			@Override
			public NotesUniversalNoteIdStruct.ByReference run() {
				return new NotesUniversalNoteIdStruct.ByReference();
			}
		});
	}
	
	public static class ByReference extends NotesUniversalNoteIdStruct implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesUniversalNoteIdStruct implements Structure.ByValue {
		public static NotesUniversalNoteIdStruct.ByValue newInstance() {
			return AccessController.doPrivileged(new PrivilegedAction<NotesUniversalNoteIdStruct.ByValue>() {

				@Override
				public NotesUniversalNoteIdStruct.ByValue run() {
					return new NotesUniversalNoteIdStruct.ByValue();
				}
			});
		}
	};
	
	/**
	 * Computes the hex UNID from the OID data
	 * 
	 * @return UNID
	 */
	@Override
	public String toString() {
		write();
		Pointer oidPtr = getPointer();
		
		Formatter formatter = new Formatter();
		ByteBuffer data = oidPtr.getByteBuffer(0, 16).order(ByteOrder.LITTLE_ENDIAN);
		formatter.format("%016x", data.getLong());
		formatter.format("%016x", data.getLong());
		String unidStr = formatter.toString().toUpperCase();
		formatter.close();
		return unidStr;
	}
	
	/**
	 * Changes the internal value to a UNID formatted as string
	 * 
	 * @param unidStr UNID string
	 */
	public void setUnid(String unidStr) {
		if (unidStr.length() != 32) {
			throw new IllegalArgumentException("UNID is expected to have 32 characters");
		}
		
		int fileInnards1 = (int) (Long.parseLong(unidStr.substring(0,8), 16) & 0xffffffff);
		int fileInnards0 = (int) (Long.parseLong(unidStr.substring(8,16), 16) & 0xffffffff);

		int noteInnards1 = (int) (Long.parseLong(unidStr.substring(16,24), 16) & 0xffffffff);
		int noteInnards0 = (int) (Long.parseLong(unidStr.substring(24,32), 16) & 0xffffffff);

		NotesTimeDateStruct file = NotesTimeDateStruct.newInstance(new int[] {fileInnards0, fileInnards1});
		NotesTimeDateStruct note = NotesTimeDateStruct.newInstance(new int[] {noteInnards0, noteInnards1});

		this.File = file;
		this.Note = note;
		write();
	}
	
	/**
	 * Converts a hex encoded UNID to a {@link NotesUniversalNoteIdStruct} object
	 * 
	 * @param unidStr UNID string
	 * @return UNID object
	 */
	public static NotesUniversalNoteIdStruct fromString(String unidStr) {
		NotesUniversalNoteIdStruct unid = NotesUniversalNoteIdStruct.newInstance();
		unid.setUnid(unidStr);
		return unid;
	}
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesUniversalNoteIdStruct.class) {
			return (T) this;
		}
		else if (clazz == Pointer.class) {
			return (T) getPointer();
		}
		return null;
	}
}
