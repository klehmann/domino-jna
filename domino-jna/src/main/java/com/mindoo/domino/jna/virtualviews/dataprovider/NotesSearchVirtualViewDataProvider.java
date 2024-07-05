package com.mindoo.domino.jna.virtualviews.dataprovider;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mindoo.domino.jna.IItemTableData;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesSearch;
import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.utils.StringUtil;
import com.mindoo.domino.jna.virtualviews.VirtualView;
import com.mindoo.domino.jna.virtualviews.VirtualViewDataChange;

/**
 * Data provider for a {@link VirtualView} that fetches data from a Notes database
 * via NSF search (results are matching a selection formula) and updates the {@link VirtualView} with the latest data.
 */
public class NotesSearchVirtualViewDataProvider extends AbstractNSFVirtualViewDataProvider {
	private VirtualView view;
	private NotesDatabase db;

	//data for serialization
	private String origin;
	private String selectionFormula;
	private Set<Integer> noteIdFilter;
	private Map<String,String> overrideFormula;
	private NotesTimeDate since;
	private Set<NoteClass> noteClasses;
	private Set<Search> searchFlags;
	
	/**
	 * Creates a new data provider
	 * 
	 * @param origin origin string to identify the data provider
	 * @param dbServer db server
	 * @param dbFilePath db file path
	 * @param optSelectionFormula optional selection formula or null to select all docs
	 * @param optNoteClasses optional set of note classes to search for or null to search for data notes only
	 * @param searchFlags search flags or null to use default flags
	 * @param optOverrideFormula optional map of column names to formula strings to override the column formulas from the {@link VirtualView}
	 * @param optNoteIdFilter optional set of note ids to filter the search results or null to include all notes
	 */
	public NotesSearchVirtualViewDataProvider(String origin, String dbServer, String dbFilePath, String optSelectionFormula,
			Set<NoteClass> optNoteClasses, Set<Search> searchFlags,
			Map<String,String> optOverrideFormula, Set<Integer> optNoteIdFilter) {
		super(dbServer, dbFilePath);
		this.origin = origin;
		this.selectionFormula = optSelectionFormula;
		this.overrideFormula = optOverrideFormula;
		this.noteIdFilter = optNoteIdFilter;
		this.noteClasses = optNoteClasses == null ? EnumSet.of(NoteClass.DATA) : new HashSet<>(optNoteClasses);
		this.searchFlags = searchFlags == null ? EnumSet.noneOf(Search.class) : new HashSet<>(searchFlags);
	}
	
	@Override
	public void init(VirtualView view) {
		this.view = view;
	}
	
	@Override
	public String getOrigin() {
		return origin;
	}
	
	@Override
	public void update() {
		if (view == null) {
			throw new IllegalStateException("View not initialized");
		}
		
		VirtualViewDataChange change = new VirtualViewDataChange(origin);
		
		Map<String,String> formulas = new HashMap<>();
		this.view.getColumns().forEach(column -> {
			String formula = column.getFormula();
			if (formula != null) {
				formulas.put(column.getItemName(), formula);
			}			
		});
		
		if (overrideFormula != null) {
			formulas.putAll(overrideFormula);
		}
		
		//compute readers lists
		formulas.put("$C1$", "");

		NotesIDTable idTableFilter = null;
		if (since ==null && noteIdFilter != null) {
			NotesTimeDate allNoteIdsSince = new NotesTimeDate();
			allNoteIdsSince.setMinimum();
			NotesIDTable allIds = db.getModifiedNoteTable(EnumSet.of(NoteClass.DATA), allNoteIdsSince, null);

			//verify that the note ids in the filter are still valid, because NSFSearch throws an
			//error if we pass invalid note ids
			NotesIDTable noteIdFilterAsTable = new NotesIDTable(noteIdFilter);
			idTableFilter = noteIdFilterAsTable.intersect(allIds);
			noteIdFilterAsTable.recycle();
		}
		
		NotesDatabase db = getDatabase();
		
		NotesTimeDate newSince =
				NotesSearch.search(db, idTableFilter, StringUtil.isEmpty(selectionFormula) ? "@true" : selectionFormula,
						formulas, "-",
						searchFlags, noteClasses,
						since, new NotesSearch.SearchCallback() {
					
					@Override
					public Action deletionStubFound(NotesDatabase parentDb, ISearchMatch searchMatch,
							IItemTableData summaryBufferData) {
						change.removeEntry(searchMatch.getNoteId());
						return Action.Continue;
					}
					
					@Override
					public Action noteFoundNotMatchingFormula(NotesDatabase parentDb, ISearchMatch searchMatch,
							IItemTableData summaryBufferData) {
						change.removeEntry(searchMatch.getNoteId());
						return Action.Continue;
					}
					
					@Override
					public Action noteFound(NotesDatabase parentDb, ISearchMatch searchMatch,
							IItemTableData summaryBufferData) {
						
						summaryBufferData.setPreferNotesTimeDates(true);						
						int noteId = searchMatch.getNoteId();
						String unid = searchMatch.getUNID();
						Map<String,Object> values = summaryBufferData.asMap(true);
						
						boolean isAccepted = true;
						if (noteIdFilter != null && !noteIdFilter.contains(noteId)) {
							isAccepted = false;
						}
						if (isAccepted && !isAccepted(searchMatch, summaryBufferData)) {
							isAccepted = false;
						}
						
						if (isAccepted) {
							change.addEntry(noteId, unid, values);
						}
						else {
							change.removeEntry(noteId);
						}
						
						return Action.Continue;
					}

		});

		view.applyChanges(change);

	    this.since = newSince;
	}

	/**
	 * Override this method to apply additional filtering to the search results
	 * 
	 * @param searchMatch search match
	 * @param summaryBufferData summary buffer data
	 * @return true to accept the entry, false to skip it
	 */
	protected boolean isAccepted(ISearchMatch searchMatch,
							IItemTableData summaryBufferData) {
		return true;
	}
}
