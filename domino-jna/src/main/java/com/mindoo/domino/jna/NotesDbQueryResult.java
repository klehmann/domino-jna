package com.mindoo.domino.jna;

import com.mindoo.domino.jna.constants.DBQuery;
import com.mindoo.domino.jna.constants.Navigate;

/**
 * Contains the computation result for a DQL query
 * 
 * @author Karsten Lehmann
 */
public class NotesDbQueryResult {
	private NotesDatabase parentDb;
	private String query;
	private NotesIDTable idTable;
	private int idTableCountSaved;
	private String explainTxt;
	private long durationInMillis;
	
	NotesDbQueryResult(NotesDatabase parentDb,
			String query, NotesIDTable idTable, String explainTxt, long durationInMillis) {
		this.parentDb = parentDb;
		this.query = query;
		this.idTable = idTable;
		this.idTableCountSaved = idTable==null ? 0 : idTable.getCount();
		this.explainTxt = explainTxt;
		this.durationInMillis = durationInMillis;
	}

	/**
	 * Returns the {@link NotesDatabase} that was used to run the query
	 * 
	 * @return database
	 */
	public NotesDatabase getParentDatabase() {
		return this.parentDb;
	}
	
	/**
	 * Returns the DQL query string
	 * 
	 * @return DQL query
	 */
	public String getQuery() {
		return this.query;
	}
	
	/**
	 * Returns an {@link NotesIDTable} with the note ids of documents matching the
	 * query. Use it for example to call {@link NotesCollection#select(NotesIDTable, boolean)}
	 * and change the selected note ids in a view, followed by calling to
	 * {@link NotesCollection#getAllEntries(String, int, java.util.EnumSet, int, java.util.EnumSet, com.mindoo.domino.jna.NotesCollection.ViewLookupCallback)}
	 * with the navigation strategy {@link Navigate#NEXT_SELECTED} to only
	 * return selected view rows in the current view sorting (use {@link NotesCollection#resortView(String, com.mindoo.domino.jna.NotesCollection.Direction)}
	 * to change sorting).
	 * 
	 * @return IDTable with note ids of matching documents
	 */
	public NotesIDTable getIDTable() {
		return this.idTable;
	}
	
	/**
	 * Returns the explain text if {@link DBQuery#EXPLAIN} was specified as
	 * query option
	 * 
	 * @return explain text or empty string
	 */
	public String getExplainText() {
		return this.explainTxt;
	}

	/**
	 * Returns the number of milliseconds it took to compute the result
	 * 
	 * @return duration
	 */
	public long getDurationInMillis() {
		return this.durationInMillis;
	}
	
	@Override
	public String toString() {
		if (this.idTable!=null && this.idTable.isRecycled()) {
			return "NotesDbQueryResult [duration="+this.durationInMillis+", count="+this.idTableCountSaved+", IDTable recycled, query="+this.query+"]";
		}
		else {
			return "NotesDbQueryResult [duration="+this.durationInMillis+", count="+(this.idTable==null ? "0" : this.idTable.getCount())+", query="+this.query+"]";
		}
	}

}
