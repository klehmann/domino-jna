package com.mindoo.domino.jna;

/**
 * Interface to access data of a view column
 */
public interface IViewColumn {
	public static enum ColumnSort { ASCENDING, DESCENDING, NONE }
	
	String getItemName();
	
	String getTitle();
	
	String getFormula();
	
	ColumnSort getSorting();
	
}
