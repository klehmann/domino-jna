package com.mindoo.domino.jna.queries.condition;

import java.util.Iterator;

import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.queries.condition.internal.SelectionEvaluator;

public class Count implements Iterable<NotesViewEntryData> {
	private Selection m_parentSelection;
	private int m_count;
	
	public Count(Selection parentSelection, int count) {
		m_parentSelection = parentSelection;
		m_count = count;
		m_parentSelection.setCount(count);
	}

	@Override
	public Iterator<NotesViewEntryData> iterator() {
		return SelectionEvaluator.evaluate(m_parentSelection);
	}
	
}
