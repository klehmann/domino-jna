package com.mindoo.domino.jna.queries.condition;

import java.util.Iterator;

import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.queries.condition.internal.SelectionEvaluator;

public class Filter implements Iterable<NotesViewEntryData> {
	private Selection m_parentSelection;
	private Criteria m_crit;
	
	public Filter(Selection parentSelection, Criteria crit) {
		m_parentSelection = parentSelection;
		m_crit = crit;
		parentSelection.setFilter(this);
	}
	
	public Sorting orderBy(String columnName) {
		return new Sorting(m_parentSelection, columnName);
	}

	public Skip skip(int entries) {
		return new Skip(m_parentSelection, entries);
	}

	@Override
	public Iterator<NotesViewEntryData> iterator() {
		return SelectionEvaluator.evaluate(m_parentSelection);
	}
}
