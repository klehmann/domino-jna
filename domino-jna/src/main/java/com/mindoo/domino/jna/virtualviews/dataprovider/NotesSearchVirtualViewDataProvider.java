package com.mindoo.domino.jna.virtualviews.dataprovider;

import java.util.EnumSet;
import java.util.HashMap;
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
	private String dbServer;
	private String dbFilePath;

	public NotesSearchVirtualViewDataProvider(String origin, String dbServer, String dbFilePath, String selectionFormula,
			Map<String,String> overrideFormula, Set<Integer> noteIdFilter) {
		
		this.origin = origin;
		this.dbServer = dbServer;
		this.dbFilePath = dbFilePath;
		this.selectionFormula = selectionFormula;
		this.overrideFormula = overrideFormula;
		this.noteIdFilter = noteIdFilter;
	}
	
	@Override
	public void init(VirtualView view) {
		this.view = view;
	}
	
	@Override
	public String getOrigin() {
		return origin;
	}
	
	public NotesDatabase getDatabase() {
		if (db == null || db.isRecycled()) {
			db = new NotesDatabase(dbServer, dbFilePath, (String) null);
		}
		return db;
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
				NotesSearch.search(db, idTableFilter, selectionFormula, formulas, "-",
						EnumSet.of(Search.SESSION_USERNAME), EnumSet.of(NoteClass.DATA),
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
						
						change.addEntry(noteId, unid, values);

						return Action.Continue;
					}

		});

		view.applyChanges(change);

	    this.since = newSince;
	}
}
