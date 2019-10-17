package com.mindoo.domino.jna;

import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.constants.FTSearch;

/**
 * Container for a FT search result
 * 
 * @author Karsten Lehmann
 */
public class NotesFTSearchResult {
	private NotesIDTable m_matchesIDTable;
	private int m_numDocs;
	private int m_numHits;
	private List<String> m_highlightStrings;
	private List<NoteIdWithScore> m_noteIdsWithScore;
	private long m_searchDurationMS;
	
	public NotesFTSearchResult(NotesIDTable matchesIDTable, int numDocs, int numHits, List<String> highlightStrings,
			List<NoteIdWithScore> noteIdsWithScore, long searchDurationMS) {
		m_matchesIDTable = matchesIDTable;
		m_numDocs = numDocs;
		m_numHits = numHits;
		m_highlightStrings = highlightStrings;
		m_noteIdsWithScore = noteIdsWithScore;
		m_searchDurationMS = searchDurationMS;
	}
	
	/**
	 * Returns the duration the search took in milliseconds
	 * 
	 * @return duration
	 */
	public long getSearchDuration() {
		return m_searchDurationMS;
	}
	
	/**
	 * Returns the actual number of documents found for this search. This number may be greater than {@link #getNumDocs()}.
	 * 
	 * @return hits
	 */
	public int getNumHits() {
		return m_numHits;
	}
	
	/**
	 * Returns the number of documents returned in the results.
	 * 
	 * @return count
	 */
	public int getNumDocs() {
		return m_numDocs;
	}
	
	/**
	 * Returns an {@link NotesIDTable} of documents matching the search.
	 * 
	 * @return IDTable if ftSearch method has been used or ftSearchExt has been called with {@link FTSearch#RET_IDTABLE} option (and we have any hits), null otherwise
	 */
	public NotesIDTable getMatches() {
		return m_matchesIDTable;
	}
	
	/**
	 * Returns the sorted note ids of search matches with their search score (0-255).
	 * 
	 * @return matches with note id and search score
	 */
	public List<NoteIdWithScore> getMatchesWithScore() {
		return m_noteIdsWithScore==null ? Collections.EMPTY_LIST : m_noteIdsWithScore;
	}
	
	/**
	 * When using {@link FTSearch#EXT_RET_HL}, this method returns
	 * the search strings parsed from the FT query. E.g. for a
	 * query "(greg* or west*) and higg*", the list contains
	 * "greg*", "west*" and "higg*".
	 * 
	 * @return hightlight strings or empty list
	 */
	public List<String> getHighlightStrings() {
		return m_highlightStrings == null ? Collections.EMPTY_LIST : m_highlightStrings;
	}
	
	@Override
	public String toString() {
		return "NotesFTSearchResult [numhits="+getNumHits()+", numdocs="+getNumDocs()+", highlights="+getHighlightStrings()+
				", hasidtable="+(m_matchesIDTable!=null)+", hasmatcheswithscore="+(m_noteIdsWithScore!=null)+", duration="+m_searchDurationMS+"ms]";
	}
}