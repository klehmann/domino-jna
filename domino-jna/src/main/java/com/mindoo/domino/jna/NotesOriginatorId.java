package com.mindoo.domino.jna;

import com.mindoo.domino.jna.structs.NotesOriginatorIdStruct;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

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
	
	private NotesOriginatorId(NotesOriginatorIdStruct struct) {
		m_struct = struct;
	}
	
	private NotesOriginatorId(Pointer p) {
		this(NotesOriginatorIdStruct.newInstance(p));
	}
	
	public NotesTimeDate getFile() {
		return m_struct.File==null ? null : new NotesTimeDate(m_struct.File);
	}

	public NotesTimeDate getNote() {
		return m_struct.Note==null ? null : new NotesTimeDate(m_struct.Note);
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
}
