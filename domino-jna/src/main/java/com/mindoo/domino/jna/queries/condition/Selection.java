package com.mindoo.domino.jna.queries.condition;

import java.util.Iterator;

import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.queries.condition.internal.SelectionEvaluator;


public class Selection implements Iterable<NotesViewEntryData> {
	private String[] m_columns;
	private Filter m_whereFilter;
	private Sorting m_orderBySorting;
	private int m_skip;
	private int m_count;
	
	public Selection(String[] columns) {
		m_columns = columns;
	}
	
	public Filter where(Operator operator) {
		return new Filter(this, operator);
	}
	
	public Filter where(Criteria crit) {
		m_whereFilter = new Filter(this, crit);
		return m_whereFilter;
	}

	public Sorting orderBy(String columnName) {
		m_orderBySorting = new Sorting(this, columnName);
		return m_orderBySorting;
	}

	public Skip skip(int entries) {
		m_skip = entries;
		return new Skip(this, entries);
	}

	public Count count(int entries) {
		m_count = entries;
		return new Count(this, entries);
	}

	public Selection setSkip(int skip) {
		m_skip = skip;
		return this;
	}
	
	public Selection setCount(int count) {
		m_count = count;
		return this;
	}
	
	public Selection setFilter(Filter filter) {
		m_whereFilter = filter;
		return this;
	}
	
	public Selection setFilter(Criteria crit) {
		m_whereFilter = new Filter(this, crit);
		return this;
	}
	
	public Selection setOrderBy(String columnName) {
		m_orderBySorting = new Sorting(this, columnName);
		return this;
	}

	@Override
	public Iterator<NotesViewEntryData> iterator() {
		return SelectionEvaluator.evaluate(this);
	}
}
