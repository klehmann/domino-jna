package com.mindoo.domino.jna.structs;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
/**
 * The Originator ID (OID) for a note identifies all replica copies of the same note and distinguishes
 * between different revisions of that note.  The Originator ID is composed of two parts:<br>
 * <br>
 * (1)  the Universal Note ID (UNID) and (2) the Sequence Number and Sequence Time.<br>
 * <br>
 * The UNID (the first part of the OID) universally identifies all copies of the same note.
 * If one note in one database has the same UNID as another note in a replica of that database,
 * then the two notes are replica copies of each other. The Sequence Number and the Sequence Time,
 * taken together, distinguish different revisions of the same note from one another. <br>
 * <br>
 * The full Originator ID uniquely identifies one particular version of a note. A modified version
 * of a replica copy of a particular note will have a different OID. This is because Domino or
 * Notes increments the Sequence Number when a note is edited, and also sets the Sequence Time
 * to the timedate when the Sequence Number was incremented.  This means that when one replica
 * copy of a note remains unchanged, but another copy is edited and modified, then the UNIDs
 * of the 2 notes will remain the same but the Sequence Number and Sequence Times (hence,
 * the OIDs) will be different.<br>
 * <br>
 * The "File" member of the OID (and UNID), contains a number derived in different ways
 * depending on the release of Domino or Notes.  Pre- 2.1 versions of Notes set the "File"
 * member to the creation timedate of the NSF file in which the note is created. Notes 2.1
 * sets the "File" member to a user-unique identifier, derived in part from information in
 * the ID of the user creating the note, and in part from the database where the note is
 * created. Notes 3.0 sets the "File" member to a random number generated at the time the note is created.<br>
 * <br>
 * The "Note" member of the OID (and UNID), contains the date/time when the very first copy
 * of the note was stored into the first NSF (Note: date/time from $CREATED item, if exists,
 * takes precedence).<br>
 * <br>
 * The "Sequence" member is a sequence number used to keep track of the most recent version of the
 * note. The "SequenceTime" member is a sequence number qualifier, that allows the Domino
 * replicator to determine which note is later given identical Sequence numbers.<br>
 * <br>
 * The sequence time qualifies the sequence number by preventing two concurrent updates from
 * looking like no update at all. The sequence time also forces all Domino systems to reach
 * the same decision as to which update is the "latest" version.  The sequence time is the
 * value that is returned to the @Modified formula and indicates when the document was last edited and saved.<br>
 * <br>
 * API programs may obtain the Originator ID from the handle of an existing, open note by
 * specifying the _NOTE_OID member ID in the NSFNoteGetInfo function. See the example below.
 * API programs may also obtain the OID for a note given the Note ID by using the
 * NSFDbGetNoteInfo function.<br>
 * <br>
 * If you need to make an existing note appear to be a totally new note,
 * the NSFDbGenerateOID function can be used to generate a new OID.
 */
public class NotesOriginatorIdStruct extends BaseStructure implements IAdaptable {
	/** C type : DBID */
	public NotesTimeDateStruct File;
	/** C type : TIMEDATE */
	public NotesTimeDateStruct Note;
	public int Sequence;
	/** C type : TIMEDATE */
	public NotesTimeDateStruct SequenceTime;
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesOriginatorIdStruct() {
		super();
	}
	
	public static NotesOriginatorIdStruct newInstance() {
		return AccessController.doPrivileged(new PrivilegedAction<NotesOriginatorIdStruct>() {

			@Override
			public NotesOriginatorIdStruct run() {
				return new NotesOriginatorIdStruct();
			}
		});
	}
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesOriginatorIdStruct.class) {
			return (T) this;
		}
		else if (clazz == Pointer.class) {
			return (T) getPointer();
		}
		return null;
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("File", "Note", "Sequence", "SequenceTime");
	}
	
	/**
	 * @param File C type : DBID
	 * @param Note C type : TIMEDATE
	 * @param Sequence : int
	 * @param SequenceTime C type : TIMEDATE
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesOriginatorIdStruct(NotesTimeDateStruct File, NotesTimeDateStruct Note, int Sequence, NotesTimeDateStruct SequenceTime) {
		super();
		this.File = File;
		this.Note = Note;
		this.Sequence = Sequence;
		this.SequenceTime = SequenceTime;
	}
	
	public static NotesOriginatorIdStruct newInstance(final NotesTimeDateStruct File, final NotesTimeDateStruct Note, final int Sequence, final NotesTimeDateStruct SequenceTime) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesOriginatorIdStruct>() {

			@Override
			public NotesOriginatorIdStruct run() {
				return new NotesOriginatorIdStruct(File, Note, Sequence, SequenceTime);
			}
		});
	}

	
	/**
	 * @param unid C type : UNID / UNIVERSALNOTEID
	 * @param Sequence : int
	 * @param SequenceTime C type : TIMEDATE
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 */
	public NotesOriginatorIdStruct(NotesUniversalNoteIdStruct unid, int Sequence, NotesTimeDateStruct SequenceTime) {
		this.File = unid.File;
		this.Note = unid.Note;
		this.Sequence = Sequence;
		this.SequenceTime = SequenceTime;
	}

	public static NotesOriginatorIdStruct newInstance(final NotesUniversalNoteIdStruct unid, final int Sequence, final NotesTimeDateStruct SequenceTime) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesOriginatorIdStruct>() {

			@Override
			public NotesOriginatorIdStruct run() {
				return new NotesOriginatorIdStruct(unid, Sequence, SequenceTime);
			}
		});
	}
	
	/**
	 * @deprecated only public to be used by JNA; use static newInstance method instead to run in AccessController.doPrivileged block
	 * 
	 * @param peer pointer
	 */
	public NotesOriginatorIdStruct(Pointer peer) {
		super(peer);
	}
	
	public static NotesOriginatorIdStruct newInstance(final Pointer p) {
		return AccessController.doPrivileged(new PrivilegedAction<NotesOriginatorIdStruct>() {

			@Override
			public NotesOriginatorIdStruct run() {
				return new NotesOriginatorIdStruct(p);
			}
		});
	}
	
	public static class ByReference extends NotesOriginatorIdStruct implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesOriginatorIdStruct implements Structure.ByValue {
		
	};
	
	/**
	 * Extracts the {@link NotesUniversalNoteIdStruct} part from the OID data
	 * 
	 * @return UNID
	 */
	public NotesUniversalNoteIdStruct getUNID() {
		return new NotesUniversalNoteIdStruct(this.File, this.Note);
	}
	
	/**
	 * Computes the hex UNID from the OID data
	 * 
	 * @return UNID
	 */
	public String getUNIDAsString() {
		write();
		Pointer oidPtr = getPointer();
		
		Formatter formatter = new Formatter();
		ByteBuffer data = oidPtr.getByteBuffer(0, 16).order(ByteOrder.LITTLE_ENDIAN);
		formatter.format("%16x", data.getLong());
		formatter.format("%16x", data.getLong());
		String unid = formatter.toString().replace(" ", "0").toUpperCase();
		formatter.close();
		return unid;
	}
	
	/**
	 * Sets a new universal id stored by this OID
	 * 
	 * @param unid new universal id
	 */
	public void setUNID(String unid) {
		if (unid.length()!=32) {
			throw new IllegalArgumentException("Invalid unid: "+unid);
		}
		
		for (int i=0; i<32; i++) {
			char c = unid.charAt(i);
			if ((c>='0' && c<='9') || (c>='A' && c<='F') || (c>='a' && c<='f')) {
				
			}
			else {
				throw new IllegalArgumentException("Invalid unid: "+unid);
			}
		}
		
		write();
		
		Pointer oidPtr = getPointer();
		ByteBuffer data = oidPtr.getByteBuffer(0, 16).order(ByteOrder.LITTLE_ENDIAN);
		LongBuffer longBuffer = data.asLongBuffer();
		
		String firstPart = unid.substring(0, 16);
		long firstPartAsLong = new BigInteger(firstPart, 16).longValue();
		longBuffer.put(0, firstPartAsLong);
		
		String secondPart = unid.substring(16);
		long secondPartAsLong = new BigInteger(secondPart, 16).longValue();
		longBuffer.put(1, secondPartAsLong);
		
		read();
		
		String newWrittenUnid = getUNIDAsString();
		if (!unid.equalsIgnoreCase(newWrittenUnid)) {
			//should not happen ;-)
			throw new IllegalStateException("Error setting new UNID in OID structure. Probably wrong memory alignment. Please contact dev.");
		}
	}
}
