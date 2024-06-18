package com.mindoo.domino.jna.virtualviews.dataprovider;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.mindoo.domino.jna.IItemTableData;
import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesSearch;
import com.mindoo.domino.jna.NotesSearch.ISearchMatch;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.utils.Pair;
import com.mindoo.domino.jna.virtualviews.VirtualView;
import com.mindoo.domino.jna.virtualviews.VirtualViewDataChange;

/**
 * Data provider for a {@link VirtualView} that syncs its data with a folder in a Notes database.
 * On each {@link #update()} call, we incrementally read the added and removed note ids from the folder
 * and push those changes to the {@link VirtualView}.
 */
public class FolderVirtualViewDataProvider {
	private VirtualView view;
	private String origin;
	private NotesDatabase db;
	private int folderNoteId;
	private Map<String,String> overrideFormula;
	private NotesTimeDate since;

	/**
	 * Creates a new data provider
	 * 
	 * @param view virtual view
	 * @param origin a string that identifies the origin of the data
	 * @param db database
	 * @param folder folder to sync with (we read added/removed note ids)
	 */
	public FolderVirtualViewDataProvider(VirtualView view, String origin, NotesDatabase db, NotesCollection folder) {
		this(view, origin, db, folder, null);
	}
	
	/**
	 * Creates a new data provider
	 * 
	 * @param view virtual view
	 * @param origin a string that identifies the origin of the data
	 * @param db database
	 * @param folder folder to sync with (we read added/removed note ids)
	 * @param overrideFormula optional formula overrides for NSFSearch
	 */
	public FolderVirtualViewDataProvider(VirtualView view, String origin, NotesDatabase db, NotesCollection folder,
			Map<String,String> overrideFormula) {
		this.view = view;
		this.origin = origin;
		this.db = db;
		if (!folder.isFolder()) {
			throw new IllegalArgumentException("This is a view, not a folder");
		}
		this.folderNoteId = folder.getNoteId();
		
		this.since = new NotesTimeDate(new int[] {0,0});
	}
	
	/**
	 * Fetches the latest changes in the folder (added/removed note ids) and computes the
	 * view column values for the added notes
	 */
	public void update() {
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

		Pair<NotesIDTable,NotesIDTable> folderChanges = db.getFolderChanges(folderNoteId, since);
		NotesIDTable addedIds = folderChanges.getValue1();
		NotesIDTable removedIds = folderChanges.getValue2();
		this.since = new NotesTimeDate();
		
		removedIds.forEach(change::removeEntry);
		
		NotesIDTable idTableFilter = null;
		if (!addedIds.isEmpty()) {
			NotesTimeDate allNoteIdsSince = new NotesTimeDate();
			allNoteIdsSince.setMinimum();
			NotesIDTable allIds = db.getModifiedNoteTable(EnumSet.of(NoteClass.DATA), allNoteIdsSince, null);

			//verify that the note ids in the filter are still valid, because NSFSearch throws an
			//error if we pass invalid note ids
			idTableFilter = addedIds.intersect(allIds);
			addedIds.recycle();

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
	}
}
