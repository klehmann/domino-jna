package com.mindoo.domino.jna.queries.condition;

import java.util.Calendar;

public class ColumnLookup extends Criteria {

	private ColumnLookup(String columnName, Relation rel, String... value) {
		
	}
	
	private ColumnLookup(String columnName, Relation rel, int... value) {
		
	}

	private ColumnLookup(String columnName, Relation rel, double... value) {
		
	}

	private ColumnLookup(String columnName, Relation rel, Calendar... value) {
		
	}

	public static Criteria column(String columnName, Relation rel, String... values) {
		return new ColumnLookup(columnName, rel, values);
	}

	public static Criteria column(String columnName, Relation rel, int... values) {
		return new ColumnLookup(columnName, rel, values);
	}

	public static Criteria column(String columnName, Relation rel, double... values) {
		return new ColumnLookup(columnName, rel, values);		
	}

	public static Criteria column(String columnName, Relation rel, Calendar... values) {
		return new ColumnLookup(columnName, rel, values);
	}

}
