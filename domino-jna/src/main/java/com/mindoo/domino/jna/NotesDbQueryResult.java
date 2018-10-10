package com.mindoo.domino.jna;

/**
 * Contains the computation result for a DQL query
 * 
 * @author Karsten Lehmann
 */
public class NotesDbQueryResult {
	private NotesDatabase parentDb;
	private String query;
	private NotesIDTable idTable;
	private String explainTxt;
	private String errorTxt;
	
	NotesDbQueryResult(NotesDatabase parentDb,
			String query, NotesIDTable idTable, String explainTxt, String errorTxt) {
		this.parentDb = parentDb;
		this.query = query;
		this.idTable = idTable;
		this.explainTxt = explainTxt;
		this.errorTxt = errorTxt;
	}

	public NotesDatabase getParentDatabase() {
		return this.parentDb;
	}
	
	public String getQuery() {
		return this.query;
	}
	
	public NotesIDTable getIDTable() {
		return this.idTable;
	}
	
	public String getExplainText() {
		return this.explainTxt;
	}
	
	public String getErrorText() {
		return this.errorTxt;
	}
}
