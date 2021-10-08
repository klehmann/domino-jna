package com.mindoo.domino.jna;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.internal.TypedItemAccess;

/**
 * Utility class that takes an {@link Iterator} of note ids and reads data
 * from the note summary buffer, returning {@link NoteData} objects.<br>
 * <br>
 * The class uses NSFSearchExtended3 internally which handles copying the
 * requested data from the notes. NSFSearchExtended3 can run on
 * a specified IDTable of note ids instead of the whole database. For
 * performance reasons, we collect pages of note ids before calling
 * NSFSearchExtended3. The page size can be set in the constructor.
 * 
 * @author Karsten Lehmann
 */
public class NoteSummaryIterator implements Iterator<NoteSummaryIterator.NoteData> {
	private PagedNoteSummaryIterator m_pagedIterator;
	private List<NoteSummaryIterator.NoteData> m_nextPage;

	/**
	 * Creates a new instance
	 * 
	 * @param db database
	 * @param pageSize number of note ids to collect internally before passing them to NSFSearchExtended3, e.g. 30000; the summary data for this amount of notes is stored in the Java heap
	 * @param noteIdIt iterator of note ids to process
	 * @param skip note ids to skip before processing them
	 * @param count number of note ids to process
	 * @param columnFormulas map with key/value pairs to be computed from the note summary items, e.g. ["_created", "@Created"] to run a formula or ["form", ""] for static fields; use ["$c1$", ""] to get a list of all readers/authors
	 */
	public NoteSummaryIterator(NotesDatabase db, int pageSize, Iterator<Integer> noteIdIt,
			int skip, int count, Map<String,String> columnFormulas) {
		this(db, pageSize, noteIdIt, skip, count, columnFormulas, EnumSet.of(NoteClass.DATA));
	}
	
	/**
	 * Creates a new instance
	 * 
	 * @param db database
	 * @param pageSize number of note ids to collect internally before passing them to NSFSearchExtended3, e.g. 500
	 * @param noteIdIt iterator of note ids to process
	 * @param skip note ids to skip before processing them
	 * @param count number of note ids to process
	 * @param columnFormulas map with key/value pairs to be computed from the note summary items, e.g. ["_created", "@Created"] to run a formula or ["form", ""] for static fields; use ["$c1$", ""] to get a list of all readers/authors
	 * @param noteClasses type of note, use {@link NoteClass#DATA} for normal documents
	 */
	public NoteSummaryIterator(NotesDatabase db, int pageSize, Iterator<Integer> noteIdIt,
			int skip, int count, Map<String,String> columnFormulas, EnumSet<NoteClass> noteClasses) {
		
		m_pagedIterator = new PagedNoteSummaryIterator(db, pageSize, noteIdIt, skip, count, columnFormulas, noteClasses);
		m_nextPage = fetchNextPage();
	}
	
	private List<NoteSummaryIterator.NoteData> fetchNextPage() {
		if (m_pagedIterator.hasNext()) {
			return m_pagedIterator.next();
		}
		else {
			return null;
		}
	}
	
	@Override
	public boolean hasNext() {
		return m_nextPage!=null;
	}
	
	@Override
	public NoteData next() {
		if (m_nextPage==null) {
			throw new NoSuchElementException();
		}
		NoteData data = m_nextPage.remove(0);
		if (m_nextPage.isEmpty()) {
			m_nextPage = fetchNextPage();
		}
		return data;
	}
	
	/**
	 * This object is returned by the iterator. It contains basic info about
	 * the note like note id, UNID, modified date, sequence number, sequence time
	 * as well as the requested data from the summary buffer.
	 */
	public static class NoteData extends TypedItemAccess {
		private NotesSearch.ISearchMatch m_searchMatch;
		private TreeMap<String,Object> m_summaryData;
		
		private NoteData(NotesSearch.ISearchMatch searchMatch, TreeMap<String,Object> summaryData) {
			m_searchMatch = searchMatch;
			m_summaryData = summaryData;
		}
		
		@Override
		public Object get(String itemName) {
			return m_summaryData.get(itemName);
		}
		
		public boolean hasItem(String itemName) {
			return m_summaryData.containsKey(itemName);
		}
		
		public NotesSearch.ISearchMatch getSearchMatch() {
			return m_searchMatch;
		}
		
		public Map<String,Object> getAllSummaryData() {
			return m_summaryData;
		}

	}

	private class PagedNoteSummaryIterator implements Iterator<List<NoteSummaryIterator.NoteData>> {
		private NotesDatabase m_db;
		private int m_pageSize;
		private Iterator<Integer> m_noteIdIt;
		private int m_skip;
		private int m_count;
		private int m_skipped;
		private int m_processed;
		private boolean m_done;
		private Map<String,String> m_columnFormulas;
		private EnumSet<NoteClass> m_noteClasses;
		
		private List<NoteData> m_nextPage;
		
		public PagedNoteSummaryIterator(NotesDatabase db, int pageSize, Iterator<Integer> noteIdIt,
				int skip, int count,
				Map<String,String> columnFormulas, EnumSet<NoteClass> documentClasses) {
			
			m_db = db;
			m_pageSize = pageSize;
			m_noteIdIt = noteIdIt;
			m_skip = skip;
			m_count = count;
			m_columnFormulas = columnFormulas;
			m_noteClasses = documentClasses;
			
			m_nextPage = produceNextPage();
		}
		
		@Override
		public boolean hasNext() {
			return m_nextPage!=null;
		}
		
		@Override
		public List<NoteSummaryIterator.NoteData> next() {
			if (m_nextPage==null) {
				throw new NoSuchElementException();
			}
			
			List<NoteSummaryIterator.NoteData> page = m_nextPage;
			m_nextPage = produceNextPage();
			return page;
		}
		
		private List<NoteSummaryIterator.NoteData> produceNextPage() {
			if (!m_noteIdIt.hasNext() || m_done) {
				return null;
			}
			
			NotesIDTable idTable = new NotesIDTable();
			
			List<Integer> noteIdsInPage = new ArrayList<>();
			
			while (m_skipped < m_skip && m_noteIdIt.hasNext()) {
				m_noteIdIt.next();
				m_skipped++;
			}

			//collect ids for next page
			for (int i=0; i<m_pageSize; i++) {
				if (m_noteIdIt.hasNext()) {
					if (m_processed < m_count) {
						Integer noteId = m_noteIdIt.next();
						noteIdsInPage.add(noteId);
						m_processed++;
					}
					else {
						m_done = true;
						break;
					}
				}
				else {
					break;
				}
			}
			idTable.addNotes(noteIdsInPage);
			
			LinkedHashMap<Integer,NoteData> dataByNoteId = new LinkedHashMap<>();
			
			//read summary data for note ids to produce next page
			
			NotesSearch.search(m_db, idTable, "@true", m_columnFormulas, "-", EnumSet.of(Search.SUMMARY,
					Search.SESSION_USERNAME),
					m_noteClasses, null, new NotesSearch.SearchCallback() {
						
				private TreeMap<String,Object> getSummaryData(IItemTableData summaryBufferData) {
					TreeMap<String,Object> data = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
					summaryBufferData.setPreferNotesTimeDates(true);
					
					if (summaryBufferData!=null) {
						for (String currItemName : m_columnFormulas.keySet()) {
							Object currItemValue = summaryBufferData.get(currItemName);
							data.put(currItemName, currItemValue);
						}
					}
					
					return data;
				}

				@Override
				public Action noteFound(NotesDatabase parentDb, ISearchMatch searchMatch, IItemTableData summaryBufferData) {
					TreeMap<String,Object> summaryData = getSummaryData(summaryBufferData);

					NoteData docInfo = new NoteData(searchMatch, summaryData);
					dataByNoteId.put(searchMatch.getNoteId(), docInfo);

					return Action.Continue;
				}

				@Override
				public Action deletionStubFound(NotesDatabase parentDb, ISearchMatch searchMatch,
						IItemTableData summaryBufferData) {

					return Action.Continue;
				}

				@Override
				public Action noteFoundNotMatchingFormula(NotesDatabase parentDb, ISearchMatch searchMatch,
						IItemTableData summaryBufferData) {

					return Action.Continue;
				}
			}

					);
			idTable.recycle();
			
			List<NoteData> page = new ArrayList<NoteData>();
			
			for (Integer currNoteId : noteIdsInPage) {
				NoteData currNoteData = dataByNoteId.get(currNoteId);
				if (currNoteData!=null) {
					page.add(currNoteData);
				}
			}

			//repeat search if we could not find any data
			while (page!=null && page.isEmpty()) {
				List<NoteData> nextPage = produceNextPage();
				if (nextPage==null) {
					return null;
				}
				else {
					page.clear();
					page.addAll(nextPage);
				}
			}
			
			return page;
		}

	}
}
