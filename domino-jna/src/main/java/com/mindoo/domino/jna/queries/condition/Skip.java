package com.mindoo.domino.jna.queries.condition;


public class Skip {
	private Selection m_parentSelection;
	private int m_skip;
	
	public Skip(Selection parentSelection, int skip) {
		m_parentSelection = parentSelection;
		m_skip = skip;
		m_parentSelection.setSkip(skip);
	}
	
	public Count count(int entries) {
		return new Count(m_parentSelection, entries);
	}

	
}
