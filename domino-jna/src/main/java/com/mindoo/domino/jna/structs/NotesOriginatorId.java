package com.mindoo.domino.jna.structs;

import java.util.Arrays;
import java.util.List;

import com.mindoo.domino.jna.utils.NotesStringUtils;
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
public class NotesOriginatorId extends Structure {
	/** C type : DBID */
	public NotesTimeDate File;
	/** C type : TIMEDATE */
	public NotesTimeDate Note;
	public int Sequence;
	/** C type : TIMEDATE */
	public NotesTimeDate SequenceTime;
	
	public NotesOriginatorId() {
		super();
	}
	
	protected List<? > getFieldOrder() {
		return Arrays.asList("File", "Note", "Sequence", "SequenceTime");
	}
	
	/**
	 * @param File C type : DBID<br>
	 * @param Note C type : TIMEDATE<br>
	 * @param SequenceTime C type : TIMEDATE
	 */
	public NotesOriginatorId(NotesTimeDate File, NotesTimeDate Note, int Sequence, NotesTimeDate SequenceTime) {
		super();
		this.File = File;
		this.Note = Note;
		this.Sequence = Sequence;
		this.SequenceTime = SequenceTime;
	}
	
	public NotesOriginatorId(Pointer peer) {
		super(peer);
	}
	
	public static class ByReference extends NotesOriginatorId implements Structure.ByReference {
		
	};
	
	public static class ByValue extends NotesOriginatorId implements Structure.ByValue {
		
	};
	
	/**
	 * Computes the hex UNID from the OID data
	 * 
	 * @return UNID
	 */
	public String getUNID() {
		return NotesStringUtils.extractUNID(this);
	}
}
