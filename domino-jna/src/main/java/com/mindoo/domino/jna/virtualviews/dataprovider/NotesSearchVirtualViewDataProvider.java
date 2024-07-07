package com.mindoo.domino.jna.virtualviews.dataprovider;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.mindoo.domino.jna.IItemTableData;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesFTSearchResult;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesSearch;
import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.FTSearch;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.utils.StringUtil;
import com.mindoo.domino.jna.virtualviews.VirtualView;
import com.mindoo.domino.jna.virtualviews.VirtualViewDataChange;
import com.mindoo.domino.jna.virtualviews.VirtualViewDataChange.EntryData;

/**
 * Data provider for a {@link VirtualView} that fetches data from a Notes database
 * via NSF search (results are matching a selection formula) and updates the {@link VirtualView} with the latest data.
 */
public class NotesSearchVirtualViewDataProvider extends AbstractNSFVirtualViewDataProvider {
	private VirtualView view;

	//data for serialization
	private String origin;
	private String selectionFormula;
	private Set<Integer> noteIdFilter;
	private Map<String,String> overrideFormula;
	private NotesTimeDate since;
	private Set<NoteClass> noteClasses;
	private Set<Search> searchFlags;
	private String optFTQuery;
	private Set<FTSearch> optFTOptions;
	
	/**
	 * Creates a new data provider
	 * 
	 * @param origin origin string to identify the data provider
	 * @param dbServer db server
	 * @param dbFilePath db file path
	 * @param optSelectionFormula optional selection formula or null to select all docs
	 * @param optNoteClasses optional set of note classes to search for or null to search for data notes only
	 * @param searchFlags search flags or null to use default flags
	 * @param optFTQuery optional full text query to post process the found notes or null to not use full text search
	 * @param optFTOptions optional full text search options or null to use default options
	 * @param optOverrideFormula optional map of column names to formula strings to override the column formulas from the {@link VirtualView}
	 * @param optNoteIdFilter optional set of note ids to filter the search results or null to include all notes
	 */
	public NotesSearchVirtualViewDataProvider(String origin, String dbServer, String dbFilePath, String optSelectionFormula,
			Set<NoteClass> optNoteClasses, Set<Search> searchFlags,
			String optFTQuery, Set<FTSearch> optFTOptions,
			Map<String,String> optOverrideFormula, Set<Integer> optNoteIdFilter) {
		super(dbServer, dbFilePath);
		this.origin = origin;
		this.selectionFormula = optSelectionFormula;
		this.noteClasses = optNoteClasses == null ? EnumSet.of(NoteClass.DATA) : new HashSet<>(optNoteClasses);
		this.searchFlags = searchFlags == null ? EnumSet.noneOf(Search.class) : new HashSet<>(searchFlags);
		if (!StringUtil.isEmpty(optFTQuery) && NoteClass.isDesignElement(optNoteClasses)) {
			throw new IllegalArgumentException("FT search is not supported for design elements");
		}
		this.optFTQuery = optFTQuery;
		this.optFTOptions = optFTOptions == null ? EnumSet.noneOf(FTSearch.class) : new HashSet<>(optFTOptions);
		this.overrideFormula = optOverrideFormula;
		this.noteIdFilter = optNoteIdFilter;
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

		NotesDatabase db = getDatabase();
		
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
		if (since == null && !StringUtil.isEmpty(optFTQuery)) {
			//make the initial run faster by doing the FT search first and pumping the result into NSFSearch
			EnumSet<FTSearch> ftOptions = optFTOptions==null ? EnumSet.noneOf(FTSearch.class) : EnumSet.copyOf(optFTOptions);
			ftOptions.add(FTSearch.RET_IDTABLE);
			if (idTableFilter != null) {
				ftOptions.add(FTSearch.REFINE);
			}
			
			NotesFTSearchResult ftResult = db.ftSearchExt(optFTQuery, 0, ftOptions, idTableFilter, 0, 0);
			NotesIDTable ftIdTable = ftResult.getMatches();
			
			//prevent any invalid note ids in the ft search result
			NotesTimeDate allNoteIdsSince = new NotesTimeDate();
			allNoteIdsSince.setMinimum();
			NotesIDTable allIds = db.getModifiedNoteTable(EnumSet.of(NoteClass.DATA), allNoteIdsSince, null);
			idTableFilter = ftIdTable.intersect(allIds);
		}
		
		Set<Integer> removalNoteIds = new HashSet<>();
		final Map<Integer,EntryData> additionsByNoteId = new HashMap<>();
		
		NotesTimeDate newSince =
				NotesSearch.search(db, idTableFilter, StringUtil.isEmpty(selectionFormula) ? "@true" : selectionFormula,
						formulas, "-",
						searchFlags, noteClasses,
						since, new NotesSearch.SearchCallback() {
					
					@Override
					public Action deletionStubFound(NotesDatabase parentDb, ISearchMatch searchMatch,
							IItemTableData summaryBufferData) {
						removalNoteIds.add(searchMatch.getNoteId());
						return Action.Continue;
					}
					
					@Override
					public Action noteFoundNotMatchingFormula(NotesDatabase parentDb, ISearchMatch searchMatch,
							IItemTableData summaryBufferData) {
						removalNoteIds.add(searchMatch.getNoteId());
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
							EntryData entry = new EntryData(unid, values);
							additionsByNoteId.put(noteId, entry);
						}
						else {
							removalNoteIds.add(searchMatch.getNoteId());
						}
						
						return Action.Continue;
					}

		});

		if (since != null && // not on the first run (there we already did the ft search above)
				!StringUtil.isEmpty(optFTQuery)) {
			//post process the collected IDs with a full text search
			NotesIDTable idTable = new NotesIDTable(additionsByNoteId.keySet());
			EnumSet<FTSearch> ftOptions = optFTOptions==null ? EnumSet.noneOf(FTSearch.class) : EnumSet.copyOf(optFTOptions);
			ftOptions.add(FTSearch.REFINE);
			ftOptions.add(FTSearch.RET_IDTABLE);
			
			NotesFTSearchResult ftResult = db.ftSearchExt(optFTQuery, 0, ftOptions, idTable, 0, 0);
			NotesIDTable ftIdTable = ftResult.getMatches();
			
			Map<Integer,EntryData> additionsByNoteIdAfterFT = new HashMap<>();
			
			for (Entry<Integer, EntryData> currEntry : additionsByNoteId.entrySet()) {
				int noteId = currEntry.getKey();
				if (!ftIdTable.contains(noteId)) {
					removalNoteIds.add(noteId);
				}
				else {
					additionsByNoteIdAfterFT.put(noteId, currEntry.getValue());
				}
			}
			idTable.recycle();
			ftIdTable.recycle();
			
			additionsByNoteIdAfterFT.forEach((noteId, entry) -> change.addEntry(noteId, entry.getUnid(), entry.getValues()));
		}
		else {
			additionsByNoteId.forEach((noteId, entry) -> change.addEntry(noteId, entry.getUnid(), entry.getValues()));			
		}
		
		removalNoteIds.forEach(change::removeEntry);

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
