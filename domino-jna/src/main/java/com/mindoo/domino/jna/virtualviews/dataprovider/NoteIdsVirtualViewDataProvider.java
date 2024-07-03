package com.mindoo.domino.jna.virtualviews.dataprovider;

import java.util.Collection;
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
import com.mindoo.domino.jna.virtualviews.VirtualView;
import com.mindoo.domino.jna.virtualviews.VirtualViewDataChange;

/**
 * Data provider for a {@link VirtualView} that adds an arbitrary list of note ids to the {@link VirtualView}.
 */
public class NoteIdsVirtualViewDataProvider extends AbstractNSFVirtualViewDataProvider {
	private VirtualView view;
	private NotesDatabase db;
	
	private String dbServer;
	private String dbFilePath;
	private String origin;
	private Map<String,String> overrideFormula;

	private Set<Integer> addedSinceLastUpdate;
	private Set<Integer> removedSinceLastUpdate;
	
	/**
	 * Creates a new data provider
	 * 
	 * @param origin a string that identifies the origin of the data
	 * @param db database
	 * @param overrideFormula optional formula overrides for NSFSearch
	 */
	public NoteIdsVirtualViewDataProvider(String origin, String dbServer, String dbFilePath, Map<String,String> overrideFormula) {
		this.origin = origin;
		this.dbServer = dbServer;
		this.dbFilePath = dbFilePath;
		this.overrideFormula = overrideFormula;
		this.addedSinceLastUpdate = new HashSet<>();
		this.removedSinceLastUpdate = new HashSet<>();
	}
	
	@Override
	public void init(VirtualView view) {
		this.view = view;
	}

	@Override
	public String getOrigin() {
		return origin;
	}
	
	/**
	 * Adds note ids to the list of notes to be processed in the next {@link #update()}
	 * 
	 * @param noteIds note ids
	 */
	public void addNoteIds(Collection<Integer> noteIds) {
		addedSinceLastUpdate.addAll(noteIds);
	}
	
	/**
	 * Removes note ids from the list of notes to be processed in the next {@link #update()}
	 * 
	 * @param noteIds note ids
	 */
	public void removeNoteIds(Collection<Integer> noteIds) {
		removedSinceLastUpdate.addAll(noteIds);
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

		if (addedSinceLastUpdate.isEmpty() && removedSinceLastUpdate.isEmpty()) {
			return;
		}
		
		VirtualViewDataChange change = new VirtualViewDataChange(origin);

		Map<String,String> formulas = new HashMap<>();
		this.view.getColumns().forEach(column -> {
			String formula = column.getValueFormula();
			if (formula != null) {
				formulas.put(column.getItemName(), formula);
			}			
		});
		
		if (overrideFormula != null) {
			formulas.putAll(overrideFormula);
		}
		
		//compute readers lists
		formulas.put("$C1$", "");
		
		removedSinceLastUpdate.forEach(change::removeEntry);
		
		NotesDatabase db = getDatabase();
		
		NotesIDTable idTableFilter = null;		
		if (!addedSinceLastUpdate.isEmpty()) {
			NotesIDTable addedSinceLastUpdateAsTable = new NotesIDTable(addedSinceLastUpdate);
			
			NotesTimeDate allNoteIdsSince = new NotesTimeDate();
			allNoteIdsSince.setMinimum();
			NotesIDTable allIds = db.getModifiedNoteTable(EnumSet.of(NoteClass.DATA), allNoteIdsSince, null);

			//verify that the note ids in the filter are still valid, because NSFSearch throws an
			//error if we pass invalid note ids
			idTableFilter = addedSinceLastUpdateAsTable.intersect(allIds);
			addedSinceLastUpdateAsTable.recycle();

			NotesSearch.search(db, idTableFilter, "SELECT @TRUE", formulas, "-",
					EnumSet.of(Search.SESSION_USERNAME), EnumSet.of(NoteClass.DATA),
					null, new NotesSearch.SearchCallback() {

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
		}

		view.applyChanges(change);
		
		this.addedSinceLastUpdate.clear();
		this.removedSinceLastUpdate.clear();
	}
}
