package com.mindoo.domino.jna;

import com.mindoo.domino.jna.internal.structs.NotesOriginatorIdStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * The Originator ID (OID) for a note identifies all replica copies of the same note and distinguishes
 * between different revisions of that note.<br>
 * <br>
 * The Originator ID is composed of two parts:<br>
 * (1)  the Universal Note ID (UNID) and (2) the Sequence Number and Sequence Time.<br>
 * <br>
 * The UNID (the first part of the OID) universally identifies all copies of the same note.
 * If one note in one database has the same UNID as another note in a replica of that database,
 * then the two notes are replica copies of each other. The Sequence Number and the Sequence Time,
 * taken together, distinguish different revisions of the same note from one another.<br>
 * <br>
 * The full Originator ID uniquely identifies one particular version of a note. A modified
 * version of a replica copy of a particular note will have a different OID.<br>
 * This is because Domino or Notes increments the Sequence Number when a note is edited, and also
 * sets the Sequence Time to the timedate when the Sequence Number was incremented.<br>
 * This means that when one replica copy of a note remains unchanged, but another copy is edited
 * and modified, then the UNIDs of the 2 notes will remain the same but the Sequence Number and
 * Sequence Times (hence, the OIDs) will be different.<br>
 * <br>
 * The "File" member of the OID (and UNID), contains a number derived in different ways depending
 * on the release of Domino or Notes.  Pre- 2.1 versions of Notes set the "File" member to the
 * creation timedate of the NSF file in which the note is created. Notes 2.1 sets the "File"
 * member to a user-unique identifier, derived in part from information in the ID of the user
 * creating the note, and in part from the database where the note is created. Notes 3.0 sets
 * the "File" member to a random number generated at the time the note is created.<br>
 * <br>
 * The "Note" member of the OID (and UNID), contains the date/time when the very first copy of
 * the note was stored into the first NSF (Note: date/time from $CREATED item, if exists, takes precedence).<br>
 * <br>
 * The "Sequence" member is a sequence number used to keep track of the most recent version of the
 * note. The "SequenceTime" member is a sequence number qualifier, that allows the Domino replicator
 * to determine which note is later given identical Sequence numbers.<br>
 * The sequence time qualifies the sequence number by preventing two concurrent updates from looking
 * like no update at all. The sequence time also forces all Domino systems to reach the same
 * decision as to which update is the "latest" version.<br>
 * <br>
 * The sequence time is the value that is returned to the @Modified formula and indicates when
 * the document was last edited and saved.
 */
public class NotesOriginatorId implements IAdaptable {
	private NotesOriginatorIdStruct m_struct;
	
	public NotesOriginatorId(IAdaptable adaptable) {
		NotesOriginatorIdStruct struct = adaptable.getAdapter(NotesOriginatorIdStruct.class);
		if (struct!=null) {
			m_struct = struct;
			return;
		}
		Pointer p = adaptable.getAdapter(Pointer.class);
		if (p!=null) {
			m_struct = NotesOriginatorIdStruct.newInstance(p);
			return;
		}
		throw new IllegalArgumentException("Constructor argument cannot provide a supported datatype");
	}
	
	public NotesOriginatorId(String unid, int sequence, NotesTimeDate sequenceTime) {
		m_struct = NotesOriginatorIdStruct.newInstance();
		m_struct.setUNID(unid);
		m_struct.Sequence = sequence;
		m_struct.SequenceTime = NotesTimeDateStruct.newInstance(sequenceTime.getInnards());
		m_struct.write();
	}
	
	public NotesOriginatorId(String unid, int sequence, int[] sequenceTimeInnards) {
		m_struct = NotesOriginatorIdStruct.newInstance();
		m_struct.setUNID(unid);
		m_struct.Sequence = sequence;
		m_struct.SequenceTime = NotesTimeDateStruct.newInstance(sequenceTimeInnards);
		m_struct.write();
	}
	
	public NotesTimeDate getFile() {
		return m_struct.File==null ? null : new NotesTimeDate(m_struct.File);
	}

	public NotesTimeDate getNote() {
		return m_struct.Note==null ? null : new NotesTimeDate(m_struct.Note);
	}

	public int getSequence() {
		return m_struct.Sequence;
	}
	
	public NotesTimeDate getSequenceTime() {
		return m_struct.SequenceTime==null ? null : new NotesTimeDate(m_struct.SequenceTime);
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesOriginatorIdStruct.class || clazz == Structure.class) {
			return (T) m_struct;
		}
		return null;
	}
	
	/**
	 * Extracts the {@link NotesUniversalNoteId} part from the OID data
	 * 
	 * @return UNID
	 */
	public NotesUniversalNoteId getUNID() {
		return new NotesUniversalNoteId(m_struct.getUNID());
	}
	
	/**
	 * Computes the hex UNID from the OID data
	 * 
	 * @return UNID
	 */
	public String getUNIDAsString() {
		return m_struct.getUNIDAsString();
	}
	
	@Override
	public String toString() {
		return "NotesOriginatorId [unid="+getUNIDAsString()+", seq="+getSequence()+", seqtime="+getSequenceTime()+"]";
	}
}
