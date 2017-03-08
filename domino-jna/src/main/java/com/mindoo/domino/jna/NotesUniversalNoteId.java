package com.mindoo.domino.jna;

import com.mindoo.domino.jna.structs.NotesUniversalNoteIdStruct;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

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
	
	private NotesUniversalNoteId(NotesUniversalNoteIdStruct struct) {
		m_struct = struct;
	}
	
	private NotesUniversalNoteId(Pointer p) {
		this(NotesUniversalNoteIdStruct.newInstance(p));
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
