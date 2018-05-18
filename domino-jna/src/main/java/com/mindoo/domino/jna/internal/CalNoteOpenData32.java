package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.NotesDatabase;

public class CalNoteOpenData32 implements IAdaptable {
	private NotesDatabase m_db;
	private int m_handle;
	
	public CalNoteOpenData32(NotesDatabase db, int handle) {
		m_db = db;
		m_handle = handle;
	}
	
	public NotesDatabase getDb() {
		return m_db;
	}
	
	public int getNoteHandle() {
		return m_handle;
	}
	
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (CalNoteOpenData32.class.equals(clazz)) {
			return (T) this;
		}
		return null;
	}
}
