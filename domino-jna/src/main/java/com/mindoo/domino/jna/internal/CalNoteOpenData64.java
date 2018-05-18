package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.NotesDatabase;

public class CalNoteOpenData64 implements IAdaptable {
	private NotesDatabase m_db;
	private long m_handle;
	
	public CalNoteOpenData64(NotesDatabase db, long handle) {
		m_db = db;
		m_handle = handle;
	}
	
	public NotesDatabase getDb() {
		return m_db;
	}
	
	public long getNoteHandle() {
		return m_handle;
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (CalNoteOpenData64.class.equals(clazz)) {
			return (T) this;
		}
		return null;
	}
}
