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
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.utils.Pair;
import com.mindoo.domino.jna.virtualviews.VirtualView;
import com.mindoo.domino.jna.virtualviews.VirtualViewDataChange;

/**
 * Data provider for a {@link VirtualView} that syncs its data with a folder in a Notes database.
 * On each {@link #update()} call, we incrementally read the added and removed note ids from the folder
 * and push those changes to the {@link VirtualView}.
 */
public class FolderVirtualViewDataProvider extends AbstractNSFVirtualViewDataProvider {
	private VirtualView view;
	private int folderNoteId;

	//data for serialization
	private String origin;
	private String folderName;
	private Map<String,String> overrideFormula;
	private NotesTimeDate since;

	/**
	 * Creates a new data provider
	 * 
	 * @param origin a string that identifies the origin of the data
	 * @param dbServer server name
	 * @param dbFilePath database file path
	 * @param folderName folder to sync with (we read added/removed note ids)
	 * @param overrideFormula optional formula overrides for NSFSearch
	 */
	public FolderVirtualViewDataProvider(String origin, String dbServer, String dbFilePath, String folderName,
			Map<String,String> overrideFormula) {
		super(dbServer, dbFilePath);
		this.origin = origin;
		this.overrideFormula = overrideFormula;
		
		this.since = new NotesTimeDate(new int[] {0,0});
	}
	
	@Override
	public void init(VirtualView view) {
		this.view = view;
	}
	
	@Override
	public String getOrigin() {
		return origin;
	}
		
	private int getFolderNoteId() {
		if (folderNoteId == 0) {
			NotesDatabase db = getDatabase();
			NotesCollection col = db.openCollectionByName(folderName);
			if (col == null) {
				throw new NotesError("Folder " + folderName + " not found in database "+dbServer+"!!"+dbFilePath);
			}
			if (!col.isFolder()) {
				throw new NotesError("The view " + folderName + " is not a folder in database "+dbServer+"!!"+dbFilePath);
			}
			this.folderNoteId = col.getNoteId();
			col.recycle();
		}
		return folderNoteId;
	}
	/**
	 * Fetches the latest changes in the folder (added/removed note ids) and computes the
	 * view column values for the added notes
	 */
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
		int folderNoteId = getFolderNoteId();
		
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
