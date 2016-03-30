package com.mindoo.domino.jna.queries.condition;

import java.util.Iterator;

import com.mindoo.domino.jna.NotesViewEntryData;

public class Sorting implements Iterable<NotesViewEntryData> {
	private Selection m_parentSelection;
	private String m_columnName;
	
	public Sorting(Selection parentSelection, String columnName) {
		m_parentSelection = parentSelection;
		m_columnName = columnName;
	}

	public Skip skip(int entries) {
		return new Skip(m_parentSelection, entries);
	}
	
	public Count count(int entries) {
		return new Count(m_parentSelection, entries);
	}

	@Override
	public Iterator<NotesViewEntryData> iterator() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
