package com.mindoo.domino.jna;

import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
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
 * The "File" member ({@link #getFile()}) of the UNID contains a number derived in different ways depending on
 * the release of Domino or Notes.<br>
 * Pre- 2.1 versions of Notes set the "File" member to the creation timedate of the NSF file
 * in which the note is created. Notes 2.1 sets the "File" member to a user-unique identifier,
 * derived in part from information in the ID of the user creating the note, and in part
 * from the database where the note is created.  Notes 3.0 sets the "File" member to a
 * random number generated at the time the note is created.<br>
 * <br>
 * The "Note" ({@link #getNote()}) member of the UNID contains the date/time when the very first copy of the note
 * was stored into the first NSF (Note: date/time from $CREATED item, if exists, takes precedence).
 */
public class NotesUniversalNoteId implements IAdaptable {
	private NotesUniversalNoteIdStruct m_struct;
	
	public NotesUniversalNoteId(IAdaptable adaptable) {
		NotesUniversalNoteIdStruct struct = adaptable.getAdapter(NotesUniversalNoteIdStruct.class);
		if (struct!=null) {
			m_struct = struct;
			return;
		}
		Pointer p = adaptable.getAdapter(Pointer.class);
		if (p!=null) {
			m_struct = NotesUniversalNoteIdStruct.newInstance(p);
			return;
		}
		throw new IllegalArgumentException("Constructor argument cannot provide a supported datatype");
	}
	
	public NotesUniversalNoteId(String unidStr) {
		m_struct = NotesUniversalNoteIdStruct.fromString(unidStr);
	}
	
	public NotesTimeDate getFile() {
		return m_struct.File==null ? null : new NotesTimeDate(m_struct.File);
	}

	public NotesTimeDate getNote() {
		return m_struct.Note==null ? null : new NotesTimeDate(m_struct.Note);
	}

	@Override
	public String toString() {
		return m_struct==null ? "null" : m_struct.toString();
	}
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz == NotesUniversalNoteIdStruct.class || clazz == Structure.class) {
			return (T) m_struct;
		}
		else if (clazz == String.class) {
			return (T) toString();
		}
		
		return null;
	}
}
