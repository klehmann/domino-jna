package com.mindoo.domino.jna;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;

import com.mindoo.domino.jna.NotesDatabase.DbMode;
import com.mindoo.domino.jna.NotesSearch.SearchCallback.Action;
import com.mindoo.domino.jna.NotesSearch.SearchCallback.NoteFlags;
import com.mindoo.domino.jna.constants.FileType;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.errors.FormulaCompilationError;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesCallbacks;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.SearchMatchDecoder;
import com.mindoo.domino.jna.internal.ViewFormulaCompiler;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.sync.NotesOriginatorIdData;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * Utility class to search Notes data
 * 
 * @author Karsten Lehmann
 */
public class NotesSearch {

	/**
	 * This function scans all the notes in a database, ID table or files in a directory.<br>
	 * <br>
	 * Based on several search criteria, the function calls a user-supplied routine (an action routine)
	 * for every note or file that matches the criteria. NSFSearch is a powerful function that provides
	 * the general search mechanism for tasks that process all or some of the documents in a
	 * database or all or some of the databases in a directory.<br>
	 * <br>
	 * Specify a formula argument to improve efficiency when processing a subset of the notes in a database.<br>
	 * <br>
	 * In addition, the formula argument can be used to return computed "on-the-fly" information.<br>
	 * <br>
	 * To do this, you specify that a value returned from a formula is to be stored in a
	 * temporary field of each note.<br>
	 * <br>
	 * This temporary field and its value is then accessible in the summary buffer received by
	 * the NSFSearch action routine without having to open the note.<br>
	 * <br>
	 * For example, suppose you want the size of each note found by NSFSearch.<br>
	 * Do the following before the call to NSFSearch:<br>
	 * Call search with a formula like this:<br>
	 * "DEFAULT dLength := @DocLength; @All"<br>
	 * and specify {@link Search#SUMMARY} for the SearchFlags argument.<br>
	 * <br>
	 * In the action routine of NSFSearch, if you get a search match, look at the summary information.<br>
	 * The dLength field will be one of the items in the summary information buffer.<br>
	 * <br>
	 * Specify a note class to restrict the search to certain classes of notes.<br>
	 * Specify {@link NotesConstants#NOTE_CLASS_DOCUMENT} to find documents.<br>
	 * Specify the "since" argument to limit the search to notes created or modified
	 * in the database since a certain time/date.<br>
	 * When used to search a database, NSFSearch will search the database file sequentially
	 * if NULL is passed as the "Since" time.<br>
	 * If the search is not time-constrained (the "Since" argument is NULL or specifies
	 * the TIMEDATE_WILDCARD, ANYDAY/ALLDAY), then NSFSearch may find a given note more
	 * than once during the same search. If a non-time-constrained search passes a
	 * certain note to the action routine, and that note is subsequently updated,
	 * then NSFSearch may find that note again and pass it to the action routine a
	 * second time during the same search. This may happen if Domino or Notes relocates
	 * the updated note to a position farther down in the file. If your algorithm requires
	 * processing each note once and only once, then use time-constrained searches.<br>
	 * Save the return value of type {@link NotesTimeDate} of the present search and use
	 * that as the "Since" time on the next search.<br>
	 * <br>
	 * Alternatively, build an ID table as you search, avoid updating notes in the action
	 * routine, and process the ID table after the search completes. ID tables are
	 * guaranteed not to contain a given ID more than once.
	 * 
	 * @param db database to search in
	 * @param searchFilter optional search scope as {@link NotesIDTable} or null
	 * @param formula formula or null
	 * @param viewTitle optional view title that will be returned for "@ ViewTitle" within the formula or null
	 * @param searchFlags flags to control searching ({@link Search})
	 * @param noteClasses noteclasses to search
	 * @param since The date of the earliest modified note that is matched. The note's "Modified in this file" date is compared to this date. Specify NULL if you do not wish any filtering by date.
	 * @param callback callback to be called for every found note
	 * @return The ending (current) time/date of this search. Returned so that it can be used in a subsequent call to {@link #search(NotesDatabase, NotesIDTable, String, String, EnumSet, EnumSet, NotesTimeDate, SearchCallback)} as the "Since" argument.
	 * @throws FormulaCompilationError if formula syntax is invalid
	 */
	public static NotesTimeDate search(final NotesDatabase db, NotesIDTable searchFilter, final String formula, String viewTitle, final EnumSet<Search> searchFlags, EnumSet<NoteClass> noteClasses, NotesTimeDate since, final SearchCallback callback) throws FormulaCompilationError {
		return search(db, searchFilter, formula, null, viewTitle, searchFlags, NoteClass.toBitMaskInt(noteClasses), since, callback);
	}
	
	/**
	 * This function scans all the notes in a database, ID table or files in a directory.<br>
	 * <br>
	 * Based on several search criteria, the function calls a user-supplied routine (an action routine)
	 * for every note or file that matches the criteria. NSFSearch is a powerful function that provides
	 * the general search mechanism for tasks that process all or some of the documents in a
	 * database or all or some of the databases in a directory.<br>
	 * <br>
	 * Specify a formula argument to improve efficiency when processing a subset of the notes in a database.<br>
	 * <br>
	 * In addition, the formula argument can be used to return computed "on-the-fly" information.<br>
	 * <br>
	 * To do this, you specify that a value returned from a formula is to be stored in a
	 * temporary field of each note.<br>
	 * <br>
	 * This temporary field and its value is then accessible in the summary buffer received by
	 * the NSFSearch action routine without having to open the note.<br>
	 * <br>
	 * For example, suppose you want the size of each note found by NSFSearch.<br>
	 * Do the following before the call to NSFSearch:<br>
	 * Call search with a formula like this:<br>
	 * "DEFAULT dLength := @DocLength; @All"<br>
	 * and specify {@link Search#SUMMARY} for the SearchFlags argument.<br>
	 * <br>
	 * In the action routine of NSFSearch, if you get a search match, look at the summary information.<br>
	 * The dLength field will be one of the items in the summary information buffer.<br>
	 * <br>
	 * Specify a note class to restrict the search to certain classes of notes.<br>
	 * Specify {@link NotesConstants#NOTE_CLASS_DOCUMENT} to find documents.<br>
	 * Specify the "since" argument to limit the search to notes created or modified
	 * in the database since a certain time/date.<br>
	 * When used to search a database, NSFSearch will search the database file sequentially
	 * if NULL is passed as the "Since" time.<br>
	 * If the search is not time-constrained (the "Since" argument is NULL or specifies
	 * the TIMEDATE_WILDCARD, ANYDAY/ALLDAY), then NSFSearch may find a given note more
	 * than once during the same search. If a non-time-constrained search passes a
	 * certain note to the action routine, and that note is subsequently updated,
	 * then NSFSearch may find that note again and pass it to the action routine a
	 * second time during the same search. This may happen if Domino or Notes relocates
	 * the updated note to a position farther down in the file. If your algorithm requires
	 * processing each note once and only once, then use time-constrained searches.<br>
	 * Save the return value of type {@link NotesTimeDate} of the present search and use
	 * that as the "Since" time on the next search.<br>
	 * <br>
	 * Alternatively, build an ID table as you search, avoid updating notes in the action
	 * routine, and process the ID table after the search completes. ID tables are
	 * guaranteed not to contain a given ID more than once.
	 * 
	 * @param db database to search in
	 * @param searchFilter optional search scope as {@link NotesIDTable} or null
	 * @param formula formula or null
	 * @param columnFormulas map with programmatic column names (key) and formulas (value) with keys sorted in column order or null to output all items
	 * @param viewTitle optional view title that will be returned for "@ ViewTitle" within the formula or null
	 * @param searchFlags flags to control searching ({@link Search})
	 * @param noteClasses noteclasses to search
	 * @param since The date of the earliest modified note that is matched. The note's "Modified in this file" date is compared to this date. Specify NULL if you do not wish any filtering by date.
	 * @param callback callback to be called for every found note
	 * @return The ending (current) time/date of this search. Returned so that it can be used in a subsequent call to {@link #search(NotesDatabase, NotesIDTable, String, String, EnumSet, EnumSet, NotesTimeDate, SearchCallback)} as the "Since" argument.
	 * @throws FormulaCompilationError if formula syntax is invalid
	 */
	public static NotesTimeDate search(final NotesDatabase db, NotesIDTable searchFilter, final String formula, LinkedHashMap<String,String> columnFormulas, String viewTitle, final EnumSet<Search> searchFlags, EnumSet<NoteClass> noteClasses, NotesTimeDate since, final SearchCallback callback) throws FormulaCompilationError {
		return search(db, searchFilter, formula, columnFormulas, viewTitle, searchFlags, NoteClass.toBitMaskInt(noteClasses), since, callback);
	}
	
	/**
	 * This function scans all the notes in a database, ID table or files in a directory.<br>
	 * <br>
	 * Based on several search criteria, the function calls a user-supplied routine (an action routine)
	 * for every note or file that matches the criteria. NSFSearch is a powerful function that provides
	 * the general search mechanism for tasks that process all or some of the documents in a
	 * database or all or some of the databases in a directory.<br>
	 * <br>
	 * Specify a formula argument to improve efficiency when processing a subset of the notes in a database.<br>
	 * <br>
	 * In addition, the formula argument can be used to return computed "on-the-fly" information.<br>
	 * <br>
	 * To do this, you specify that a value returned from a formula is to be stored in a
	 * temporary field of each note.<br>
	 * <br>
	 * This temporary field and its value is then accessible in the summary buffer received by
	 * the NSFSearch action routine without having to open the note.<br>
	 * <br>
	 * For example, suppose you want the size of each note found by NSFSearch.<br>
	 * Do the following before the call to NSFSearch:<br>
	 * Call search with a formula like this:<br>
	 * "DEFAULT dLength := @DocLength; @All"<br>
	 * and specify {@link Search#SUMMARY} for the SearchFlags argument.<br>
	 * <br>
	 * In the action routine of NSFSearch, if you get a search match, look at the summary information.<br>
	 * The dLength field will be one of the items in the summary information buffer.<br>
	 * <br>
	 * Specify a note class to restrict the search to certain classes of notes.<br>
	 * Specify {@link NotesConstants#NOTE_CLASS_DOCUMENT} to find documents.<br>
	 * Specify the "since" argument to limit the search to notes created or modified
	 * in the database since a certain time/date.<br>
	 * When used to search a database, NSFSearch will search the database file sequentially
	 * if NULL is passed as the "Since" time.<br>
	 * If the search is not time-constrained (the "Since" argument is NULL or specifies
	 * the TIMEDATE_WILDCARD, ANYDAY/ALLDAY), then NSFSearch may find a given note more
	 * than once during the same search. If a non-time-constrained search passes a
	 * certain note to the action routine, and that note is subsequently updated,
	 * then NSFSearch may find that note again and pass it to the action routine a
	 * second time during the same search. This may happen if Domino or Notes relocates
	 * the updated note to a position farther down in the file. If your algorithm requires
	 * processing each note once and only once, then use time-constrained searches.<br>
	 * Save the return value of type {@link NotesTimeDate} of the present search and use
	 * that as the "Since" time on the next search.<br>
	 * <br>
	 * Alternatively, build an ID table as you search, avoid updating notes in the action
	 * routine, and process the ID table after the search completes. ID tables are
	 * guaranteed not to contain a given ID more than once.
	 * 
	 * @param db database to search in
	 * @param searchFilter optional search scope as {@link NotesIDTable} or null
	 * @param formula formula or null
	 * @param viewTitle optional view title that will be returned for "@ ViewTitle" within the formula or null
	 * @param searchFlags flags to control searching ({@link Search})
	 * @param fileTypes filetypes to search
	 * @param since The date of the earliest modified note that is matched. The note's "Modified in this file" date is compared to this date. Specify NULL if you do not wish any filtering by date.
	 * @param callback callback to be called for every found note
	 * @return The ending (current) time/date of this search. Returned so that it can be used in a subsequent call to {@link #search(NotesDatabase, NotesIDTable, String, String, EnumSet, EnumSet, NotesTimeDate, SearchCallback)} as the "Since" argument.
	 * @throws FormulaCompilationError if formula syntax is invalid
	 */
	public static NotesTimeDate searchFiles(final NotesDatabase db, Object searchFilter, final String formula, String viewTitle, final EnumSet<Search> searchFlags, EnumSet<FileType> fileTypes, NotesTimeDate since, final SearchCallback callback) throws FormulaCompilationError {
		return search(db, searchFilter, formula, null, viewTitle, searchFlags, FileType.toBitMaskInt(fileTypes), since, callback);
	}
	
	/**
	 * This function scans all the notes in a database, ID table or files in a directory.<br>
	 * <br>
	 * Based on several search criteria, the function calls a user-supplied routine (an action routine)
	 * for every note or file that matches the criteria. NSFSearch is a powerful function that provides
	 * the general search mechanism for tasks that process all or some of the documents in a
	 * database or all or some of the databases in a directory.<br>
	 * <br>
	 * Specify a formula argument to improve efficiency when processing a subset of the notes in a database.<br>
	 * <br>
	 * In addition, the formula argument can be used to return computed "on-the-fly" information.<br>
	 * <br>
	 * To do this, you specify that a value returned from a formula is to be stored in a
	 * temporary field of each note.<br>
	 * <br>
	 * This temporary field and its value is then accessible in the summary buffer received by
	 * the NSFSearch action routine without having to open the note.<br>
	 * <br>
	 * For example, suppose you want the size of each note found by NSFSearch.<br>
	 * Do the following before the call to NSFSearch:<br>
	 * Call search with a formula like this:<br>
	 * "DEFAULT dLength := @DocLength; @All"<br>
	 * and specify {@link Search#SUMMARY} for the SearchFlags argument.<br>
	 * <br>
	 * In the action routine of NSFSearch, if you get a search match, look at the summary information.<br>
	 * The dLength field will be one of the items in the summary information buffer.<br>
	 * <br>
	 * Specify a note class to restrict the search to certain classes of notes.<br>
	 * Specify {@link NotesConstants#NOTE_CLASS_DOCUMENT} to find documents.<br>
	 * Specify the "since" argument to limit the search to notes created or modified
	 * in the database since a certain time/date.<br>
	 * When used to search a database, NSFSearch will search the database file sequentially
	 * if NULL is passed as the "Since" time.<br>
	 * If the search is not time-constrained (the "Since" argument is NULL or specifies
	 * the TIMEDATE_WILDCARD, ANYDAY/ALLDAY), then NSFSearch may find a given note more
	 * than once during the same search. If a non-time-constrained search passes a
	 * certain note to the action routine, and that note is subsequently updated,
	 * then NSFSearch may find that note again and pass it to the action routine a
	 * second time during the same search. This may happen if Domino or Notes relocates
	 * the updated note to a position farther down in the file. If your algorithm requires
	 * processing each note once and only once, then use time-constrained searches.<br>
	 * Save the return value of type {@link NotesTimeDate} of the present search and use
	 * that as the "Since" time on the next search.<br>
	 * <br>
	 * Alternatively, build an ID table as you search, avoid updating notes in the action
	 * routine, and process the ID table after the search completes. ID tables are
	 * guaranteed not to contain a given ID more than once.
	 * 
	 * @param db database to search in
	 * @param searchFilter optional search scope as {@link NotesIDTable} or null
	 * @param formula formula or null
	 * @param columnFormulas map with programmatic column names (key) and formulas (value) with keys sorted in column order or null to output all items
	 * @param viewTitle optional view title that will be returned for "@ ViewTitle" within the formula or null
	 * @param searchFlags flags to control searching ({@link Search})
	 * @param fileTypes filetypes to search
	 * @param since The date of the earliest modified note that is matched. The note's "Modified in this file" date is compared to this date. Specify NULL if you do not wish any filtering by date.
	 * @param callback callback to be called for every found note
	 * @return The ending (current) time/date of this search. Returned so that it can be used in a subsequent call to {@link #searchFiles(NotesDatabase, Object, String, String, EnumSet, EnumSet, NotesTimeDate, SearchCallback)} as the "Since" argument.
	 * @throws FormulaCompilationError if formula syntax is invalid
	 */
	public static NotesTimeDate searchFiles(final NotesDatabase db, Object searchFilter, final String formula, LinkedHashMap<String,String> columnFormulas, String viewTitle, final EnumSet<Search> searchFlags, EnumSet<FileType> fileTypes, NotesTimeDate since, final SearchCallback callback) throws FormulaCompilationError {
		return search(db, searchFilter, formula, columnFormulas, viewTitle, searchFlags, FileType.toBitMaskInt(fileTypes), since, callback);
	}

	/**
	 * This function scans all the notes in a database, ID table or files in a directory.<br>
	 * <br>
	 * Based on several search criteria, the function calls a user-supplied routine (an action routine)
	 * for every note or file that matches the criteria. NSFSearch is a powerful function that provides
	 * the general search mechanism for tasks that process all or some of the documents in a
	 * database or all or some of the databases in a directory.<br>
	 * <br>
	 * Specify a formula argument to improve efficiency when processing a subset of the notes in a database.<br>
	 * <br>
	 * In addition, the formula argument can be used to return computed "on-the-fly" information.<br>
	 * <br>
	 * To do this, you specify that a value returned from a formula is to be stored in a
	 * temporary field of each note.<br>
	 * <br>
	 * This temporary field and its value is then accessible in the summary buffer received by
	 * the NSFSearch action routine without having to open the note.<br>
	 * <br>
	 * For example, suppose you want the size of each note found by NSFSearch.<br>
	 * Do the following before the call to NSFSearch:<br>
	 * Call search with a formula like this:<br>
	 * "DEFAULT dLength := @DocLength; @All"<br>
	 * and specify {@link Search#SUMMARY} for the SearchFlags argument.<br>
	 * <br>
	 * In the action routine of NSFSearch, if you get a search match, look at the summary information.<br>
	 * The dLength field will be one of the items in the summary information buffer.<br>
	 * <br>
	 * Specify a note class to restrict the search to certain classes of notes.<br>
	 * Specify {@link NotesConstants#NOTE_CLASS_DOCUMENT} to find documents.<br>
	 * Specify the "since" argument to limit the search to notes created or modified
	 * in the database since a certain time/date.<br>
	 * When used to search a database, NSFSearch will search the database file sequentially
	 * if NULL is passed as the "Since" time.<br>
	 * If the search is not time-constrained (the "Since" argument is NULL or specifies
	 * the TIMEDATE_WILDCARD, ANYDAY/ALLDAY), then NSFSearch may find a given note more
	 * than once during the same search. If a non-time-constrained search passes a
	 * certain note to the action routine, and that note is subsequently updated,
	 * then NSFSearch may find that note again and pass it to the action routine a
	 * second time during the same search. This may happen if Domino or Notes relocates
	 * the updated note to a position farther down in the file. If your algorithm requires
	 * processing each note once and only once, then use time-constrained searches.<br>
	 * Save the return value of type {@link NotesTimeDate} of the present search and use
	 * that as the "Since" time on the next search.<br>
	 * <br>
	 * Alternatively, build an ID table as you search, avoid updating notes in the action
	 * routine, and process the ID table after the search completes. ID tables are
	 * guaranteed not to contain a given ID more than once.
	 * 
	 * @param db database to search in
	 * @param searchFilter optional search scope as {@link NotesIDTable} or null
	 * @param formula formula or null
	 * @param columnFormulas map with programmatic column names (key) and formulas (value) with keys sorted in column order or null to output all items; automatically uses {@link Search#NOITEMNAMES} and {@link Search#SUMMARY} search flag
	 * @param viewTitle optional view title that will be returned for "@ ViewTitle" within the formula or null
	 * @param searchFlags flags to control searching ({@link Search})
	 * @param noteClassMask bitmask of {@link NoteClass} or {@link FileType} to search
	 * @param since The date of the earliest modified note that is matched. The note's "Modified in this file" date is compared to this date. Specify NULL if you do not wish any filtering by date.
	 * @param callback callback to be called for every found note
	 * @return The ending (current) time/date of this search. Returned so that it can be used in a subsequent call to {@link #search(NotesDatabase, Object, String, String, EnumSet, int, NotesTimeDate, SearchCallback)} as the "Since" argument.
	 * @throws FormulaCompilationError if formula syntax is invalid
	 */
	private static NotesTimeDate search(final NotesDatabase db, Object searchFilter, final String formula, LinkedHashMap<String,String> columnFormulas, String viewTitle,
			final EnumSet<Search> searchFlags, int noteClassMask, NotesTimeDate since,
			final SearchCallback callback) throws FormulaCompilationError {
		if (db.isRecycled()) {
			throw new NotesError(0, "Database already recycled");
		}

		if (searchFilter instanceof NotesIDTable) {
			if (since==null) {
				//in R9, since must have any value to make this work in NSFSearchExtended3, so we use 1.1.1900
				since = NotesDateTimeUtils.dateToTimeDate(new Date(1900-1900, 1-1, 1, 0, 0, 0));
			}
			if (StringUtil.isEmpty(viewTitle)) {
				//in R9, view title cannot be empty if filtering with IDTable
				viewTitle = "-";
			}
		}

		final NotesTimeDateStruct sinceStruct = since==null ? null : NotesTimeDateStruct.newInstance(since.getInnards());

		final EnumSet<Search> useSearchFlags = searchFlags.clone();
		if (columnFormulas!=null) {
			useSearchFlags.add(Search.SUMMARY);
			useSearchFlags.add(Search.NOITEMNAMES);
		}
		
		int searchFlagsBitMask = Search.toBitMaskStdFlagsInt(useSearchFlags);
		int search1FlagsBitMask = Search.toBitMaskSearch1Flags(useSearchFlags);
		
		final String[] columnItemNames = columnFormulas==null ? new String[0] : columnFormulas.keySet().toArray(new String[0]);
		
		DbMode mode = db.getMode();

		if (PlatformUtils.is64Bit()) {
			final Throwable invocationEx[] = new Throwable[1];

			final NotesCallbacks.NsfSearchProc apiCallback = new NotesCallbacks.NsfSearchProc() {

				@Override
				public short invoke(Pointer enumRoutineParameter, Pointer searchMatchPtr,
						Pointer summaryBufferPtr) {

					ISearchMatch searchMatch = SearchMatchDecoder.decodeSearchMatch(searchMatchPtr);
					
					IItemTableData itemTableData=null;
					try {
						boolean isMatch = formula==null || searchMatch.matchesFormula();
						
						if (isMatch && useSearchFlags.contains(Search.SUMMARY)) {
							if (summaryBufferPtr!=null && Pointer.nativeValue(summaryBufferPtr)!=0) {
								boolean convertStringsLazily = true;
								boolean convertNotesTimeDateToCalendar = false;
								
								if (useSearchFlags.contains(Search.NOITEMNAMES)) {
									//flag to just return the column values is used; so the
									//buffer contains an ITEM_VALUE_TABLE with column values
									//in the column order instead of an ITEM_TABLE with columnname/columnvalue
									//pairs
									//create an ItemTableData by adding the column names to make this invisible to callers
									itemTableData = NotesLookupResultBufferDecoder.decodeItemValueTableWithColumnNames(columnItemNames, summaryBufferPtr, convertStringsLazily, convertNotesTimeDateToCalendar, false);
								}
								else {
									itemTableData = NotesLookupResultBufferDecoder.decodeItemTable(summaryBufferPtr,
											convertStringsLazily, convertNotesTimeDateToCalendar, false);
								}
							}
						}


						Action action;
						if (searchMatch.getNoteClass().contains(NoteClass.NOTIFYDELETION)) {
							action = callback.deletionStubFound(db, searchMatch, itemTableData);
						}
						else {
							if (!isMatch) {
								action = callback.noteFoundNotMatchingFormula(db, searchMatch, itemTableData);
							}
							else {
								action = callback.noteFound(db, searchMatch, itemTableData);
							}
						}
						if (action==Action.Stop) {
							return INotesErrorConstants.ERR_CANCEL;
						}
						else {
							return 0;
						}
					}
					catch (Throwable t) {
						invocationEx[0] = t;
						return INotesErrorConstants.ERR_CANCEL;
					}
					finally {
						if (itemTableData!=null) {
							itemTableData.free();
						}
					}
				}

			};
		
			long hFormula = 0;
			if (!StringUtil.isEmpty(formula)) {
				hFormula = ViewFormulaCompiler.b64_compile(formula, columnFormulas);
			}

			NotesIDTable tableWithHighOrderBit = null;
			boolean tableWithHighOrderBitCanBeRecycled = false;
			
			try {
				final NotesTimeDateStruct retUntil = NotesTimeDateStruct.newInstance();

				final Memory viewTitleBuf = NotesStringUtils.toLMBCS(viewTitle==null ? "" : viewTitle, true);

				int hFilter=0;
				int filterFlags=NotesConstants.SEARCH_FILTER_NONE;
				
				if (searchFilter instanceof NotesIDTable) {
					//NSFSearchExtended3 required that the high order bit for each ID in the table
					//must be set; we check if a new table must be created
					NotesIDTable idTable = ((NotesIDTable)searchFilter);
					if (idTable.isEmpty()) {
						tableWithHighOrderBit = idTable;
						tableWithHighOrderBitCanBeRecycled = false;
					}
					else {
						long firstId = idTable.getFirstId();
						long lastId = idTable.getLastId();

						if (((firstId & NotesConstants.NOTEID_RESERVED)==NotesConstants.NOTEID_RESERVED) &&
						((lastId & NotesConstants.NOTEID_RESERVED)==NotesConstants.NOTEID_RESERVED)) {
							//high order bit already set for every ID
							tableWithHighOrderBit = idTable;
							tableWithHighOrderBitCanBeRecycled = false;
						}
						else {
							//create a new table
							tableWithHighOrderBit = idTable.withHighOrderBit();
							tableWithHighOrderBitCanBeRecycled = true;
						}
					}
					hFilter = (int) tableWithHighOrderBit.getHandle64();
					filterFlags = NotesConstants.SEARCH_FILTER_NOTEID_TABLE;
				}
				else if (searchFilter instanceof NotesCollection) {
					//produces a crash:
//					NotesCollection col = (NotesCollection) searchFilter;
//					LongByReference retFilter = new LongByReference();
//					short result = notesAPI.b64_NSFGetFolderSearchFilter(db.getHandle64(), db.getHandle64(), col.getNoteId(), since, 0, retFilter);
//					NotesErrorUtils.checkResult(result);
//					hFilter = retFilter.getValue();
//					filterFlags = NotesConstants.SEARCH_FILTER_FOLDER;
				}
				
				int searchFlags2 = 0;
				int searchFlags3 = 0;
				int searchFlags4 = 0;

				final long hFormulaFinal = hFormula;
				final int hFilterFinal = hFilter;
				final int filterFlagsFinal = filterFlags;
				final int searchFlagsBitMaskFinal = searchFlagsBitMask;
				final int searchFlags1Final = search1FlagsBitMask;
				final int searchFlags2Final = searchFlags2;
				final int searchFlags3Final = searchFlags3;
				final int searchFlags4Final = searchFlags4;
				final int noteClassMaskFinal = noteClassMask;

				final long hNamesList;
				if (mode == DbMode.DIRECTORY) {
					hNamesList = 0;
				}
				else {
					if (db.m_passNamesListToDbOpen && db.m_namesList!=null) {
						hNamesList = db.m_namesList.getHandle64();
					}
					else {
						hNamesList = 0;
					}
				}
				
				short result;
				try {
					//AccessController call required to prevent SecurityException when running in XPages
					result = AccessController.doPrivileged(new PrivilegedExceptionAction<Short>() {

						@Override
						public Short run() throws Exception {
							return NotesNativeAPI64.get().NSFSearchExtended3(db.getHandle64(), hFormulaFinal,
									hFilterFinal, filterFlagsFinal,
									viewTitleBuf, searchFlagsBitMaskFinal, searchFlags1Final, searchFlags2Final, searchFlags3Final, searchFlags4Final,
									(short) (noteClassMaskFinal & 0xffff), sinceStruct, apiCallback, null, retUntil,
									hNamesList);

						}
					});
				} catch (PrivilegedActionException e) {
					if (e.getCause() instanceof RuntimeException) 
						throw (RuntimeException) e.getCause();
					else
						throw new NotesError(0, "Error searching database", e);
				}


				if (invocationEx[0]!=null) {
					//special case for JUnit testcases
					if (invocationEx[0] instanceof AssertionError) {
						throw (AssertionError) invocationEx[0];
					}
					throw new NotesError(0, "Error searching database", invocationEx[0]);
				}
				
				if (result!=INotesErrorConstants.ERR_CANCEL) {
					NotesErrorUtils.checkResult(result);
				}
				else {
					return null;
				}
				NotesTimeDate retUntilWrap = retUntil==null ? null : new  NotesTimeDate(retUntil);
				return retUntilWrap;
			}
			finally {
				//free handle of formula
				if (hFormula!=0) {
					short result = Mem64.OSMemFree(hFormula);
					NotesErrorUtils.checkResult(result);
				}
				if (tableWithHighOrderBit!=null && tableWithHighOrderBitCanBeRecycled) {
					tableWithHighOrderBit.recycle();
				}
			}

		}
		else {
			final NotesCallbacks.NsfSearchProc apiCallback;
			final Throwable invocationEx[] = new Throwable[1];

			if (PlatformUtils.isWin32()) {
				apiCallback = new Win32NotesCallbacks.NsfSearchProcWin32() {
					@Override
					public short invoke(Pointer enumRoutineParameter, Pointer searchMatchPtr,
							Pointer summaryBufferPtr) {

						ISearchMatch searchMatch = SearchMatchDecoder.decodeSearchMatch(searchMatchPtr);

						IItemTableData itemTableData=null;
						try {
							boolean isMatch = formula==null || searchMatch.matchesFormula();
							
							if (isMatch && useSearchFlags.contains(Search.SUMMARY)) {
								if (summaryBufferPtr!=null && Pointer.nativeValue(summaryBufferPtr)!=0) {
									boolean convertStringsLazily = true;
									boolean convertNotesTimeDateToCalendar = false;
									
									if (useSearchFlags.contains(Search.NOITEMNAMES)) {
										//flag to just return the column values is used; so the
										//buffer contains an ITEM_VALUE_TABLE with column values
										//in the column order instead of an ITEM_TABLE with columnname/columnvalue
										//pairs
										//create an ItemTableData by adding the column names to make this invisible to callers
										itemTableData = NotesLookupResultBufferDecoder.decodeItemValueTableWithColumnNames(columnItemNames, summaryBufferPtr, convertStringsLazily, convertNotesTimeDateToCalendar, false);
									}
									else {
										itemTableData = NotesLookupResultBufferDecoder.decodeItemTable(summaryBufferPtr, 
												convertStringsLazily, convertNotesTimeDateToCalendar, false);
									}
								}
							}

							Action action;
							if (searchMatch.getNoteClass().contains(NoteClass.NOTIFYDELETION)) {
								action = callback.deletionStubFound(db, searchMatch, itemTableData);
							}
							else {
								if (!isMatch) {
									action = callback.noteFoundNotMatchingFormula(db, searchMatch, itemTableData);
								}
								else {
									action = callback.noteFound(db, searchMatch, itemTableData);
								}
							}
							if (action==Action.Stop) {
								return INotesErrorConstants.ERR_CANCEL;
							}
							else {
								return 0;
							}
						}
						catch (Throwable t) {
							invocationEx[0] = t;
							return INotesErrorConstants.ERR_CANCEL;
						}
						finally {
							if (itemTableData!=null) {
								itemTableData.free();
							}
						}
					}

				};
			}
			else {
				apiCallback = new NotesCallbacks.NsfSearchProc() {

					@Override
					public short invoke(Pointer enumRoutineParameter, Pointer searchMatchPtr,
							Pointer summaryBufferPtr) {

						ISearchMatch searchMatch = SearchMatchDecoder.decodeSearchMatch(searchMatchPtr);
						
						IItemTableData itemTableData=null;
						try {
							boolean isMatch = formula==null || searchMatch.matchesFormula();
							
							if (isMatch && useSearchFlags.contains(Search.SUMMARY)) {
								if (summaryBufferPtr!=null && Pointer.nativeValue(summaryBufferPtr)!=0) {
									boolean convertStringsLazily = true;
									boolean convertNotesTimeDateToCalendar = false;
									
									if (useSearchFlags.contains(Search.NOITEMNAMES)) {
										//flag to just return the column values is used; so the
										//buffer contains an ITEM_VALUE_TABLE with column values
										//in the column order instead of an ITEM_TABLE with columnname/columnvalue
										//pairs
										//create an ItemTableData by adding the column names to make this invisible to callers
										itemTableData = NotesLookupResultBufferDecoder.decodeItemValueTableWithColumnNames(columnItemNames, summaryBufferPtr, convertStringsLazily, convertNotesTimeDateToCalendar, false);
									}
									else {
										itemTableData = NotesLookupResultBufferDecoder.decodeItemTable(summaryBufferPtr,
												convertStringsLazily, convertNotesTimeDateToCalendar, false);
									}
								}
							}

							Action action;
							if (searchMatch.getNoteClass().contains(NoteClass.NOTIFYDELETION)) {
								action = callback.deletionStubFound(db, searchMatch, itemTableData);
							}
							else {
								if (!isMatch) {
									action = callback.noteFoundNotMatchingFormula(db, searchMatch, itemTableData);
								}
								else {
									action = callback.noteFound(db, searchMatch, itemTableData);
								}
							}
							if (action==Action.Stop) {
								return INotesErrorConstants.ERR_CANCEL;
							}
							else {
								return 0;
							}
						}
						catch (Throwable t) {
							invocationEx[0] = t;
							return INotesErrorConstants.ERR_CANCEL;
						}
						finally {
							if (itemTableData!=null) {
								itemTableData.free();
							}
						}
					}

				};

			}

			//formulaName only required of formula is used for collection columns
			int hFormula = 0;
			if (!StringUtil.isEmpty(formula)) {
				hFormula = ViewFormulaCompiler.b32_compile(formula, columnFormulas);
			}
			
			NotesIDTable tableWithHighOrderBit = null;
			boolean tableWithHighOrderBitCanBeRecycled = false;
			try {
				final NotesTimeDateStruct retUntil = NotesTimeDateStruct.newInstance();

				final Memory viewTitleBuf = viewTitle!=null ? NotesStringUtils.toLMBCS(viewTitle, false) : null;

				int hFilter=0;
				int filterFlags=NotesConstants.SEARCH_FILTER_NONE;
				
				if (searchFilter instanceof NotesIDTable) {
					//NSFSearchExtended3 required that the high order bit for each ID in the table
					//must be set; we check if a new table must be created
					NotesIDTable idTable = ((NotesIDTable)searchFilter);
					if (idTable.isEmpty()) {
						tableWithHighOrderBit = idTable;
						tableWithHighOrderBitCanBeRecycled = false;
					}
					else {
						long firstId = idTable.getFirstId();
						long lastId = idTable.getLastId();

						if (((firstId & NotesConstants.NOTEID_RESERVED)==NotesConstants.NOTEID_RESERVED) &&
						((lastId & NotesConstants.NOTEID_RESERVED)==NotesConstants.NOTEID_RESERVED)) {
							//high order bit already set for every ID
							tableWithHighOrderBit = idTable;
							tableWithHighOrderBitCanBeRecycled = false;
						}
						else {
							//create a new table
							tableWithHighOrderBit = idTable.withHighOrderBit();
							tableWithHighOrderBitCanBeRecycled = true;
						}
					}
					hFilter = (int) tableWithHighOrderBit.getHandle32();
					filterFlags = NotesConstants.SEARCH_FILTER_NOTEID_TABLE;
				}
				else if (searchFilter instanceof NotesCollection) {
					//produces a crash:
//					NotesCollection col = (NotesCollection) searchFilter;
//					IntByReference retFilter = new IntByReference();
//					short result = notesAPI.b32_NSFGetFolderSearchFilter(db.getHandle32(), db.getHandle32(), col.getNoteId(), since, 0, retFilter);
//					NotesErrorUtils.checkResult(result);
//					hFilter = retFilter.getValue();
//					filterFlags = NotesConstants.SEARCH_FILTER_FOLDER;
				}
				
				final int hFormulaFinal = hFormula;
				final int hFilterFinal = hFilter;
				final int filterFlagsFinal = filterFlags;
				final int searchFlagsBitMaskFinal = searchFlagsBitMask;
				final int searchFlags1Final = search1FlagsBitMask;
				final int searchFlags2Final = 0;
				final int searchFlags3Final = 0;
				final int searchFlags4Final = 0;
				final int noteClassMaskFinal = noteClassMask;
				
				final int hNamesList;
				if (mode == DbMode.DIRECTORY) {
					hNamesList = 0;
				}
				else {
					if (db.m_passNamesListToDbOpen && db.m_namesList!=null) {
						hNamesList = db.m_namesList.getHandle32();
					}
					else {
						hNamesList = 0;
					}
				}

				short result;
				try {
					//AccessController call required to prevent SecurityException when running in XPages
					result = AccessController.doPrivileged(new PrivilegedExceptionAction<Short>() {

						@Override
						public Short run() throws Exception {
							return NotesNativeAPI32.get().NSFSearchExtended3(db.getHandle32(), hFormulaFinal, hFilterFinal, filterFlagsFinal,
									viewTitleBuf, (int) (searchFlagsBitMaskFinal & 0xffff), searchFlags1Final, searchFlags2Final, searchFlags3Final, searchFlags4Final,
									(short) (noteClassMaskFinal & 0xffff), sinceStruct, apiCallback, null, retUntil, 
									hNamesList);
						}
					});
				} catch (PrivilegedActionException e) {
					if (e.getCause() instanceof RuntimeException) 
						throw (RuntimeException) e.getCause();
					else
						throw new NotesError(0, "Error searching database", e);
				}

				if (invocationEx[0]!=null) {
					//special case for JUnit testcases
					if (invocationEx[0] instanceof AssertionError) {
						throw (AssertionError) invocationEx[0];
					}
					throw new NotesError(0, "Error searching database", invocationEx[0]);
				}
				
				if (result!=INotesErrorConstants.ERR_CANCEL) {
					NotesErrorUtils.checkResult(result);
				}
				else {
					return null;
				}
				NotesTimeDate retUntilWrap = retUntil==null ? null : new NotesTimeDate(retUntil);
				return retUntilWrap;
			}
			finally {
				//free handle of formula
				if (hFormula!=0) {
					short result = Mem32.OSMemFree(hFormula);
					NotesErrorUtils.checkResult(result);
				}
				if (tableWithHighOrderBit!=null && tableWithHighOrderBitCanBeRecycled) {
					tableWithHighOrderBit.recycle();
				}
			}

		}
	}
	
	/**
	 * Callback interface to process database search results
	 * 
	 * @author Karsten Lehmann
	 */
	public static abstract class SearchCallback {
		public enum Action {Continue, Stop}
		public enum NoteFlags {
			/** does not match formula (deleted or updated) */
			NoMatch,
			/** matches formula */
			Match,
			/** document truncated */
			Truncated,
			/** note has been purged. Returned only when SEARCH_INCLUDE_PURGED is used */
			Purged,
			/** note has no purge status. Returned only when SEARCH_FULL_DATACUTOFF is used */
			NoPurgeStatus,
			/** if {@link Search#NOTIFYDELETIONS}: note is soft deleted; NoteClass &amp; {@link NoteClass#NOTIFYDELETION} also on (off for hard delete) */
			SoftDeleted,
			/** if there is reader's field at doc level this is the return value so that we could mark the replication as incomplete*/
			NoAccess,
			/** note has truncated attachments. Returned only when SEARCH1_ONLY_ABSTRACTS is used */
			TruncatedAttachments
		}
		
		/**
		 * Implement this method to receive search results
		 * 
		 * @param parentDb parent database
		 * @param searchMatch data about search match
		 * @param summaryBufferData gives access to the note's summary buffer if {@link Search#SUMMARY} was specified; otherwise this value is null
		 * @return either {@link Action#Continue} to go on searching or {@link Action#Stop} to stop
		 */
		public abstract Action noteFound(NotesDatabase parentDb, ISearchMatch searchMatch, IItemTableData summaryBufferData);
		
		/**
		 * Implement this method to read deletion stubs. Method
		 * is only called when a <code>since</code> date is specified.
		 * 
		 * @param parentDb parent database
		 * @param searchMatch data about search match
		 * @param summaryBufferData gives access to the note's summary buffer if {@link Search#SUMMARY} was specified; otherwise this value is null
		 * @return either {@link Action#Continue} to go on searching or {@link Action#Stop} to stop
		 */
		public Action deletionStubFound(NotesDatabase parentDb, ISearchMatch searchMatch, IItemTableData summaryBufferData) {
			return Action.Continue;
		}
		
		/**
		 * Implement this method to receive notes that do not match the selection formula. Method
		 * is only called when a <code>since</code> date is specified.
		 * 
		 * @param parentDb parent database
		 * @param searchMatch data about search match
		 * @param summaryBufferData gives access to the note's summary buffer if {@link Search#SUMMARY} was specified; otherwise this value is null
		 * @return either {@link Action#Continue} to go on searching or {@link Action#Stop} to stop
		 */
		public Action noteFoundNotMatchingFormula(NotesDatabase parentDb, ISearchMatch searchMatch, IItemTableData summaryBufferData) {
			return Action.Continue;
		}
		
	}
	
	/**
	 * Interface to access the summary buffer, either item by item or to decode the whole buffer
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface ISummaryBufferAccess {
		
		public Iterator<String> getItemNames();
		
		public Object getItemValue(String itemName);
		
		public int getItemType(String itemName);
		
		public boolean hasItem(String itemName);
		
		public IItemTableData decodeWholeBuffer();
		

		/**
		 * Frees the memory, if not already done
		 */
		public void free();
		
		/**
		 * Checks if this memory has already been freed
		 * 
		 * @return true if freed
		 */
		public boolean isFreed();
		
	}
	
	/**
	 * Container with information about each note received for an NSF search,
	 * containing the global instance id (GID), originator id (OID) and
	 * information about the note class and flags.
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface ISearchMatch {

		//global instance id properties
		
		/**
		 * Gives raw access to the global instance id's file timedate data
		 * 
		 * @return file innards
		 */
		public int[] getGIDFileInnards();
		
		/**
		 * Gives raw access to the global instance id's note timedate data
		 * 
		 * @return note innards
		 */
		public int[] getGIDNoteInnards();
		
		/**
		 * Returns the note id
		 * 
		 * @return note id
		 */
		public int getNoteId();
		
		//originator id properties
		
		/**
		 * Gives raw access to the originator id's file timedate data
		 * 
		 * @return file innards
		 */
		public int[] getOIDFileInnards();
		
		/**
		 * Gives raw access to the originator id's note timedate data
		 * 
		 * @return note innards
		 */
		public int[] getOIDNoteInnards();
		
		/**
		 * Returns the note's sequence number
		 * 
		 * @return sequence number
		 */
		public int getSeq();
		
		/**
		 * Gives raw access to the note's sequence time data
		 * 
		 * @return sequence time innards
		 */
		public int[] getSeqTimeInnards();
		
		//other data
		
		/**
		 * Returns information about the note's class
		 * 
		 * @return class info
		 */
		public EnumSet<NoteClass> getNoteClass();
		
		/**
		 * Returns information about note flags
		 * 
		 * @return flags
		 */
		public EnumSet<NoteFlags> getFlags();
	
		/**
		 * Convenience function that checks whether the result of {@link #getFlags()}
		 * contains {@link NoteFlags#Match}. When a formula and a date is specified for an NSF
		 * search, the search not only returns notes matching the formula, but also
		 * deleted notes and notes not matching the formula.
		 * 
		 * @return true if matches formula
		 */
		public boolean matchesFormula();
		
		/**
		 * Returns the length of the returned summary buffer
		 * 
		 * @return summary buffer
		 */
		public int getSummaryLength();
	
		//methods with the same content but different return types
		
		/**
		 * Returns all the data of the originator id
		 * 
		 * @return originator id data
		 */
		public NotesOriginatorIdData getOIDData();
		
		/**
		 * Returns the UNID of the note
		 * 
		 * @return UNID
		 */
		public String getUNID();
		
		/**
		 * Returns the "file" part of the global instance id as a {@link NotesTimeDate}.
		 * This is the creation date of the database.
		 * 
		 * @return db creation date
		 */
		public NotesTimeDate getDbCreated();
		
		/**
		 * Returns the modified date of the note as an {@link NotesTimeDate}
		 * 
		 * @return modified date
		 */
		public NotesTimeDate getNoteModified();
		
		/**
		 * Returns the sequence time of the note as a {@link NotesTimeDate}.
		 * 
		 * @return sequence time
		 */
		public NotesTimeDate getSeqTime();
		
	}
}
