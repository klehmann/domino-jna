package com.mindoo.domino.jna;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Date;
import java.util.EnumSet;

import com.mindoo.domino.jna.NotesSearch.SearchCallback.Action;
import com.mindoo.domino.jna.NotesSearch.SearchCallback.NoteFlags;
import com.mindoo.domino.jna.constants.FileType;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.constants.Search;
import com.mindoo.domino.jna.errors.FormulaCompilationError;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesCAPI.b32_NsfSearchProc;
import com.mindoo.domino.jna.internal.NotesCAPI.b64_NsfSearchProc;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder;
import com.mindoo.domino.jna.internal.NotesLookupResultBufferDecoder.ItemTableData;
import com.mindoo.domino.jna.internal.WinNotesCAPI;
import com.mindoo.domino.jna.structs.NotesItemTableStruct;
import com.mindoo.domino.jna.structs.NotesSearchMatch32Struct;
import com.mindoo.domino.jna.structs.NotesSearchMatch64Struct;
import com.mindoo.domino.jna.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

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
	 * Specify {@link NotesCAPI#NOTE_CLASS_DOCUMENT} to find documents.<br>
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
	 * @return The ending (current) time/date of this search. Returned so that it can be used in a subsequent call to {@link #search(NotesDatabase, Object, String, String, EnumSet, int, NotesTimeDate, SearchCallback)} as the "Since" argument.
	 * @throws FormulaCompilationError if formula syntax is invalid
	 */
	public static NotesTimeDate search(final NotesDatabase db, NotesIDTable searchFilter, final String formula, String viewTitle, final EnumSet<Search> searchFlags, EnumSet<NoteClass> noteClasses, NotesTimeDate since, final SearchCallback callback) throws FormulaCompilationError {
		return search(db, searchFilter, formula, viewTitle, searchFlags, NoteClass.toBitMaskInt(noteClasses), since, callback);
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
	 * Specify {@link NotesCAPI#NOTE_CLASS_DOCUMENT} to find documents.<br>
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
	 * @return The ending (current) time/date of this search. Returned so that it can be used in a subsequent call to {@link #search(NotesDatabase, Object, String, String, EnumSet, int, NotesTimeDate, SearchCallback)} as the "Since" argument.
	 * @throws FormulaCompilationError if formula syntax is invalid
	 */
	public static NotesTimeDate searchFiles(final NotesDatabase db, Object searchFilter, final String formula, String viewTitle, final EnumSet<Search> searchFlags, EnumSet<FileType> fileTypes, NotesTimeDate since, final SearchCallback callback) throws FormulaCompilationError {
		return search(db, searchFilter, formula, viewTitle, searchFlags, FileType.toBitMaskInt(fileTypes), since, callback);
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
	 * Specify {@link NotesCAPI#NOTE_CLASS_DOCUMENT} to find documents.<br>
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
	 * @param noteClassMask bitmask of {@link NoteClass} or {@link FileType} to search
	 * @param since The date of the earliest modified note that is matched. The note's "Modified in this file" date is compared to this date. Specify NULL if you do not wish any filtering by date.
	 * @param callback callback to be called for every found note
	 * @return The ending (current) time/date of this search. Returned so that it can be used in a subsequent call to {@link #search(NotesDatabase, Object, String, String, EnumSet, int, NotesTimeDate, SearchCallback)} as the "Since" argument.
	 * @throws FormulaCompilationError if formula syntax is invalid
	 */
	private static NotesTimeDate search(final NotesDatabase db, Object searchFilter, final String formula, String viewTitle, final EnumSet<Search> searchFlags, int noteClassMask, NotesTimeDate since, final SearchCallback callback) throws FormulaCompilationError {
		if (db.isRecycled()) {
			throw new NotesError(0, "Database already recycled");
		}

		if (searchFilter instanceof NotesIDTable && since==null) {
			//since must have any value to make this work in NSFSearchExtended3, so we use 1.1.1900
			since = NotesDateTimeUtils.dateToTimeDate(new Date(1900-1900, 1-1, 1, 0, 0, 0));
		}

		final NotesTimeDateStruct sinceStruct = since==null ? null : since.getAdapter(NotesTimeDateStruct.class);

		final NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		final int gmtOffset = NotesDateTimeUtils.getGMTOffset();
		final boolean isDST = NotesDateTimeUtils.isDaylightTime();

		int searchFlagsBitMask = Search.toBitMaskInt(searchFlags);
		
		
		if (NotesJNAContext.is64Bit()) {
			final b64_NsfSearchProc apiCallback;
			final Throwable invocationEx[] = new Throwable[1];

			//not sure if this is necessary, but we had to use a special library extending StdCallLibrary
			//for Windows and the documentation says this might be required for callbacks as well
			//(StdCallCallback)
			if (notesAPI instanceof WinNotesCAPI) {
				apiCallback = new WinNotesCAPI.b64_NsfSearchProcWin() {

					@Override
					public short invoke(Pointer enumRoutineParameter, NotesSearchMatch64Struct searchMatch,
							NotesItemTableStruct summaryBuffer) {

						try {
							ItemTableData itemTableData=null;
							if (searchFlags.contains(Search.SUMMARY)) {
								if (summaryBuffer!=null) {
									Pointer summaryBufferPtr = summaryBuffer.getPointer();
									boolean convertStringsLazily = true;
									itemTableData = NotesLookupResultBufferDecoder.decodeItemTable(summaryBufferPtr, gmtOffset, isDST, convertStringsLazily);
								}
							}

							short noteClass = searchMatch.NoteClass;
							int noteId = searchMatch.ID!=null ? searchMatch.ID.NoteID : 0;
							NotesOriginatorId oid = searchMatch.OriginatorID==null ? null : new NotesOriginatorId(searchMatch.OriginatorID);
							
							NotesTimeDateStruct dbCreatedStruct = searchMatch.ID!=null ? searchMatch.ID.File : null;
							NotesTimeDateStruct noteModifiedStruct = searchMatch.ID!=null ? searchMatch.ID.Note : null;

							NotesTimeDate dbCreatedWrap = dbCreatedStruct==null ? null : new NotesTimeDate(dbCreatedStruct);
							NotesTimeDate noteModifiedWrap = noteModifiedStruct==null ? null : new NotesTimeDate(noteModifiedStruct);
							
							EnumSet<NoteClass> noteClassesEnum = NoteClass.toNoteClasses(noteClass);
							EnumSet<NoteFlags> flags = toNoteFlags(searchMatch.SERetFlags);
							
							Action action;
							if (noteClassesEnum.contains(NoteClass.NOTIFYDELETION)) {
								action = callback.deletionStubFound(db, noteId, oid, noteClassesEnum, flags, dbCreatedWrap, noteModifiedWrap);
							}
							else {
								if (formula!=null && (searchMatch.SERetFlags & NotesCAPI.SE_FMATCH)==0) {
									action = callback.noteFoundNotMatchingFormula(db, noteId, oid, noteClassesEnum, flags, dbCreatedWrap, noteModifiedWrap, itemTableData);
								}
								else {
									action = callback.noteFound(db, noteId, oid, noteClassesEnum, flags, dbCreatedWrap, noteModifiedWrap, itemTableData);
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
					}
				};
			}
			else {
				apiCallback = new b64_NsfSearchProc() {

					@Override
					public short invoke(Pointer enumRoutineParameter, NotesSearchMatch64Struct searchMatch,
							NotesItemTableStruct summaryBuffer) {

						try {
							ItemTableData itemTableData=null;
							if (searchFlags.contains(Search.SUMMARY)) {
								if (summaryBuffer!=null) {
									Pointer summaryBufferPtr = summaryBuffer.getPointer();
									boolean convertStringsLazily = true;
									itemTableData = NotesLookupResultBufferDecoder.decodeItemTable(summaryBufferPtr, gmtOffset, isDST, convertStringsLazily);
								}
							}

							short noteClass = searchMatch.NoteClass;
							int noteId = searchMatch.ID!=null ? searchMatch.ID.NoteID : 0;
							NotesOriginatorId oid = searchMatch.OriginatorID==null ? null : new NotesOriginatorId(searchMatch.OriginatorID);
							
							NotesTimeDateStruct dbCreatedStruct = searchMatch.ID!=null ? searchMatch.ID.File : null;
							NotesTimeDateStruct noteModifiedStruct = searchMatch.ID!=null ? searchMatch.ID.Note : null;

							NotesTimeDate dbCreatedWrap = dbCreatedStruct==null ? null : new NotesTimeDate(dbCreatedStruct);
							NotesTimeDate noteModifiedWrap = noteModifiedStruct==null ? null : new NotesTimeDate(noteModifiedStruct);

							EnumSet<NoteClass> noteClassesEnum = NoteClass.toNoteClasses(noteClass);
							EnumSet<NoteFlags> flags = toNoteFlags(searchMatch.SERetFlags);

							Action action;
							if (noteClassesEnum.contains(NoteClass.NOTIFYDELETION)) {
								action = callback.deletionStubFound(db, noteId, oid, noteClassesEnum, flags, dbCreatedWrap, noteModifiedWrap);
							}
							else {
								if (formula!=null && (searchMatch.SERetFlags & NotesCAPI.SE_FMATCH)==0) {
									action = callback.noteFoundNotMatchingFormula(db, noteId, oid, noteClassesEnum, flags, dbCreatedWrap, noteModifiedWrap, itemTableData);
								}
								else {
									action = callback.noteFound(db, noteId, oid, noteClassesEnum, flags, dbCreatedWrap, noteModifiedWrap, itemTableData);
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
					}

				};
			}

			long hFormula = 0;
			if (!StringUtil.isEmpty(formula)) {
				//formulaName only required if formula is used for collection columns
				Memory formulaName = null;
				short formulaNameLength = 0;
				Memory formulaText = NotesStringUtils.toLMBCS(formula, false);
				short formulaTextLength = (short) formulaText.size();

				LongByReference rethFormula = new LongByReference();
				ShortByReference retFormulaLength = new ShortByReference();
				ShortByReference retCompileError = new ShortByReference();
				ShortByReference retCompileErrorLine = new ShortByReference();
				ShortByReference retCompileErrorColumn = new ShortByReference();
				ShortByReference retCompileErrorOffset = new ShortByReference();
				ShortByReference retCompileErrorLength = new ShortByReference();

				short result = notesAPI.b64_NSFFormulaCompile(formulaName, formulaNameLength, formulaText,
						formulaTextLength, rethFormula, retFormulaLength, retCompileError, retCompileErrorLine,
						retCompileErrorColumn, retCompileErrorOffset, retCompileErrorLength);

				if (result == INotesErrorConstants.ERR_FORMULA_COMPILATION) {
					String errMsg = NotesErrorUtils.errToString(result);

					throw new FormulaCompilationError(result, errMsg, formula,
							retCompileError.getValue(),
							retCompileErrorLine.getValue(),
							retCompileErrorColumn.getValue(),
							retCompileErrorOffset.getValue(),
							retCompileErrorLength.getValue());
				}
				NotesErrorUtils.checkResult(result);
				hFormula = rethFormula.getValue();
			}

			NotesIDTable tableWithHighOrderBit = null;
			boolean tableWithHighOrderBitCanBeRecycled = false;
			
			try {
				final NotesTimeDateStruct retUntil = NotesTimeDateStruct.newInstance();

				final Memory viewTitleBuf = viewTitle!=null ? NotesStringUtils.toLMBCS(viewTitle, true) : null;

				int hFilter=0;
				int filterFlags=NotesCAPI.SEARCH_FILTER_NONE;
				
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

						if (((firstId & NotesCAPI.NOTEID_RESERVED)==NotesCAPI.NOTEID_RESERVED) &&
						((lastId & NotesCAPI.NOTEID_RESERVED)==NotesCAPI.NOTEID_RESERVED)) {
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
					filterFlags = NotesCAPI.SEARCH_FILTER_NOTEID_TABLE;
				}
				else if (searchFilter instanceof NotesCollection) {
					//produces a crash:
//					NotesCollection col = (NotesCollection) searchFilter;
//					LongByReference retFilter = new LongByReference();
//					short result = notesAPI.b64_NSFGetFolderSearchFilter(db.getHandle64(), db.getHandle64(), col.getNoteId(), since, 0, retFilter);
//					NotesErrorUtils.checkResult(result);
//					hFilter = retFilter.getValue();
//					filterFlags = NotesCAPI.SEARCH_FILTER_FOLDER;
				}
				
				int searchFlags1 = 0;
				int searchFlags2 = 0;
				int searchFlags3 = 0;
				int searchFlags4 = 0;

				final long hFormulaFinal = hFormula;
				final int hFilterFinal = hFilter;
				final int filterFlagsFinal = filterFlags;
				final int searchFlagsBitMaskFinal = searchFlagsBitMask;
				final int searchFlags1Final = searchFlags1;
				final int searchFlags2Final = searchFlags2;
				final int searchFlags3Final = searchFlags3;
				final int searchFlags4Final = searchFlags4;
				final int noteClassMaskFinal = noteClassMask;

				short result;
				try {
					//AccessController call required to prevent SecurityException when running in XPages
					result = AccessController.doPrivileged(new PrivilegedExceptionAction<Short>() {

						@Override
						public Short run() throws Exception {
							return notesAPI.b64_NSFSearchExtended3(db.getHandle64(), hFormulaFinal,
									hFilterFinal, filterFlagsFinal,
									viewTitleBuf, searchFlagsBitMaskFinal, searchFlags1Final, searchFlags2Final, searchFlags3Final, searchFlags4Final,
									(short) (noteClassMaskFinal & 0xffff), sinceStruct, apiCallback, null, retUntil,
									db.m_namesList==null ? 0 : db.m_namesList.getHandle64());

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

				NotesTimeDate retUntilWrap = retUntil==null ? null : new  NotesTimeDate(retUntil);
				return retUntilWrap;
			}
			finally {
				//free handle of formula
				if (hFormula!=0) {
					notesAPI.b64_OSMemFree(hFormula);
				}
				if (tableWithHighOrderBit!=null && tableWithHighOrderBitCanBeRecycled) {
					tableWithHighOrderBit.recycle();
				}
			}

		}
		else {
			final b32_NsfSearchProc apiCallback;
			final Throwable invocationEx[] = new Throwable[1];

			if (notesAPI instanceof WinNotesCAPI) {
				apiCallback = new WinNotesCAPI.b32_NsfSearchProcWin() {
					final Throwable invocationEx[] = new Throwable[1];

					@Override
					public short invoke(Pointer enumRoutineParameter, NotesSearchMatch32Struct searchMatch,
							NotesItemTableStruct summaryBuffer) {

						try {
							ItemTableData itemTableData=null;
							if (searchFlags.contains(Search.SUMMARY)) {
								if (summaryBuffer!=null) {
									Pointer summaryBufferPtr = summaryBuffer.getPointer();
									boolean convertStringsLazily = true;
									itemTableData = NotesLookupResultBufferDecoder.decodeItemTable(summaryBufferPtr, gmtOffset, isDST, convertStringsLazily);
								}
							}

							short noteClass = searchMatch.NoteClass;
							int noteId = searchMatch.ID!=null ? searchMatch.ID.NoteID : 0;
							NotesOriginatorId oid = searchMatch.OriginatorID==null ? null : new NotesOriginatorId(searchMatch.OriginatorID);
							
							NotesTimeDateStruct dbCreatedStruct = searchMatch.ID!=null ? searchMatch.ID.File : null;
							NotesTimeDateStruct noteModifiedStruct = searchMatch.ID!=null ? searchMatch.ID.Note : null;

							NotesTimeDate dbCreatedWrap = dbCreatedStruct==null ? null : new NotesTimeDate(dbCreatedStruct);
							NotesTimeDate noteModifiedWrap = noteModifiedStruct==null ? null : new NotesTimeDate(noteModifiedStruct);

							EnumSet<NoteClass> noteClassesEnum = NoteClass.toNoteClasses(noteClass);
							EnumSet<NoteFlags> flags = toNoteFlags(searchMatch.SERetFlags);

							Action action;
							if (noteClassesEnum.contains(NoteClass.NOTIFYDELETION)) {
								action = callback.deletionStubFound(db, noteId, oid, noteClassesEnum, flags, dbCreatedWrap, noteModifiedWrap);
							}
							else {
								if (formula!=null && (searchMatch.SERetFlags & NotesCAPI.SE_FMATCH)==0) {
									action = callback.noteFoundNotMatchingFormula(db, noteId, oid, noteClassesEnum, flags, dbCreatedWrap, noteModifiedWrap, itemTableData);
								}
								else {
									action = callback.noteFound(db, noteId, oid, noteClassesEnum, flags, dbCreatedWrap, noteModifiedWrap, itemTableData);
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
					}

				};
			}
			else {
				apiCallback = new b32_NsfSearchProc() {

					@Override
					public short invoke(Pointer enumRoutineParameter, NotesSearchMatch32Struct searchMatch,
							NotesItemTableStruct summaryBuffer) {

						try {
							ItemTableData itemTableData=null;
							if (searchFlags.contains(Search.SUMMARY)) {
								if (summaryBuffer!=null) {
									Pointer summaryBufferPtr = summaryBuffer.getPointer();
									boolean convertStringsLazily = true;
									itemTableData = NotesLookupResultBufferDecoder.decodeItemTable(summaryBufferPtr, gmtOffset, isDST, convertStringsLazily);
								}
							}

							short noteClass = searchMatch.NoteClass;
							int noteId = searchMatch.ID!=null ? searchMatch.ID.NoteID : 0;
							NotesOriginatorId oid = searchMatch.OriginatorID==null ? null : new NotesOriginatorId(searchMatch.OriginatorID);
							
							NotesTimeDateStruct dbCreatedStruct = searchMatch.ID!=null ? searchMatch.ID.File : null;
							NotesTimeDateStruct noteModifiedStruct = searchMatch.ID!=null ? searchMatch.ID.Note : null;

							NotesTimeDate dbCreatedWrap = dbCreatedStruct==null ? null : new NotesTimeDate(dbCreatedStruct);
							NotesTimeDate noteModifiedWrap = noteModifiedStruct==null ? null : new NotesTimeDate(noteModifiedStruct);

							EnumSet<NoteClass> noteClassesEnum = NoteClass.toNoteClasses(noteClass);
							EnumSet<NoteFlags> flags = toNoteFlags(searchMatch.SERetFlags);

							Action action;
							if (noteClassesEnum.contains(NoteClass.NOTIFYDELETION)) {
								action = callback.deletionStubFound(db, noteId, oid, noteClassesEnum, flags, dbCreatedWrap, noteModifiedWrap);
							}
							else {
								if (formula!=null && (searchMatch.SERetFlags & NotesCAPI.SE_FMATCH)==0) {
									action = callback.noteFoundNotMatchingFormula(db, noteId, oid, noteClassesEnum, flags, dbCreatedWrap, noteModifiedWrap, itemTableData);
								}
								else {
									action = callback.noteFound(db, noteId, oid, noteClassesEnum, flags, dbCreatedWrap, noteModifiedWrap, itemTableData);
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
					}

				};

			}

			//formulaName only required of formula is used for collection columns
			int hFormula = 0;
			if (!StringUtil.isEmpty(formula)) {
				Memory formulaName = null;
				short formulaNameLength = 0;
				Memory formulaText = NotesStringUtils.toLMBCS(formula, false);
				short formulaTextLength = (short) formulaText.size();

				IntByReference rethFormula = new IntByReference();
				ShortByReference retFormulaLength = new ShortByReference();
				ShortByReference retCompileError = new ShortByReference();
				ShortByReference retCompileErrorLine = new ShortByReference();
				ShortByReference retCompileErrorColumn = new ShortByReference();
				ShortByReference retCompileErrorOffset = new ShortByReference();
				ShortByReference retCompileErrorLength = new ShortByReference();

				short result = notesAPI.b32_NSFFormulaCompile(formulaName, formulaNameLength, formulaText,
						formulaTextLength, rethFormula, retFormulaLength, retCompileError, retCompileErrorLine,
						retCompileErrorColumn, retCompileErrorOffset, retCompileErrorLength);

				if (result == INotesErrorConstants.ERR_FORMULA_COMPILATION) {
					String errMsg = NotesErrorUtils.errToString(result);

					throw new FormulaCompilationError(result, errMsg, formula,
							retCompileError.getValue(),
							retCompileErrorLine.getValue(),
							retCompileErrorColumn.getValue(),
							retCompileErrorOffset.getValue(),
							retCompileErrorLength.getValue());
				}
				NotesErrorUtils.checkResult(result);
				hFormula = rethFormula.getValue();
			}
			
			NotesIDTable tableWithHighOrderBit = null;
			boolean tableWithHighOrderBitCanBeRecycled = false;
			try {
				final NotesTimeDateStruct retUntil = NotesTimeDateStruct.newInstance();

				final Memory viewTitleBuf = viewTitle!=null ? NotesStringUtils.toLMBCS(viewTitle, false) : null;

				int hFilter=0;
				int filterFlags=NotesCAPI.SEARCH_FILTER_NONE;
				
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

						if (((firstId & NotesCAPI.NOTEID_RESERVED)==NotesCAPI.NOTEID_RESERVED) &&
						((lastId & NotesCAPI.NOTEID_RESERVED)==NotesCAPI.NOTEID_RESERVED)) {
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
					filterFlags = NotesCAPI.SEARCH_FILTER_NOTEID_TABLE;
				}
				else if (searchFilter instanceof NotesCollection) {
					//produces a crash:
//					NotesCollection col = (NotesCollection) searchFilter;
//					IntByReference retFilter = new IntByReference();
//					short result = notesAPI.b32_NSFGetFolderSearchFilter(db.getHandle32(), db.getHandle32(), col.getNoteId(), since, 0, retFilter);
//					NotesErrorUtils.checkResult(result);
//					hFilter = retFilter.getValue();
//					filterFlags = NotesCAPI.SEARCH_FILTER_FOLDER;
				}
				
				int searchFlags1 = 0;
				int searchFlags2 = 0;
				int searchFlags3 = 0;
				int searchFlags4 = 0;
				
				final int hFormulaFinal = hFormula;
				final int hFilterFinal = hFilter;
				final int filterFlagsFinal = filterFlags;
				final int searchFlagsBitMaskFinal = searchFlagsBitMask;
				final int searchFlags1Final = searchFlags1;
				final int searchFlags2Final = searchFlags2;
				final int searchFlags3Final = searchFlags3;
				final int searchFlags4Final = searchFlags4;
				final int noteClassMaskFinal = noteClassMask;
				
				short result;
				try {
					//AccessController call required to prevent SecurityException when running in XPages
					result = AccessController.doPrivileged(new PrivilegedExceptionAction<Short>() {

						@Override
						public Short run() throws Exception {
							return notesAPI.b32_NSFSearchExtended3(db.getHandle32(), hFormulaFinal, hFilterFinal, filterFlagsFinal,
									viewTitleBuf, (int) (searchFlagsBitMaskFinal & 0xffff), searchFlags1Final, searchFlags2Final, searchFlags3Final, searchFlags4Final,
									(short) (noteClassMaskFinal & 0xffff), sinceStruct, apiCallback, null, retUntil, db.m_namesList==null ? 0 : db.m_namesList.getHandle32());
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

				NotesTimeDate retUntilWrap = retUntil==null ? null : new NotesTimeDate(retUntil);
				return retUntilWrap;
			}
			finally {
				//free handle of formula
				if (hFormula!=0) {
					notesAPI.b32_OSMemFree(hFormula);
				}
				if (tableWithHighOrderBit!=null && tableWithHighOrderBitCanBeRecycled) {
					tableWithHighOrderBit.recycle();
				}
			}

		}
	}

	private static EnumSet<NoteFlags> toNoteFlags(byte flagsAsByte) {
		EnumSet<NoteFlags> flags = EnumSet.noneOf(NoteFlags.class);
		boolean isTruncated = (flagsAsByte & NotesCAPI.SE_FTRUNCATED) == NotesCAPI.SE_FTRUNCATED;
		if (isTruncated)
			flags.add(NoteFlags.Truncated);
		boolean isNoAccess = (flagsAsByte & NotesCAPI.SE_FNOACCESS) == NotesCAPI.SE_FNOACCESS;
		if (isNoAccess)
			flags.add(NoteFlags.NoAccess);
		boolean isTruncatedAttachment = (flagsAsByte & NotesCAPI.SE_FTRUNCATT) == NotesCAPI.SE_FTRUNCATT;
		if (isTruncatedAttachment)
			flags.add(NoteFlags.TruncatedAttachments);
		boolean isNoPurgeStatus = (flagsAsByte & NotesCAPI.SE_FNOPURGE) == NotesCAPI.SE_FNOPURGE;
		if (isNoPurgeStatus)
			flags.add(NoteFlags.NoPurgeStatus);
		boolean isPurged = (flagsAsByte & NotesCAPI.SE_FPURGED) == NotesCAPI.SE_FPURGED;
		if (isPurged)
			flags.add(NoteFlags.Purged);
		boolean isMatch = (flagsAsByte & NotesCAPI.SE_FMATCH) == NotesCAPI.SE_FMATCH;
		if (isMatch)
			flags.add(NoteFlags.Match);
		else
			flags.add(NoteFlags.NoMatch);
		boolean isSoftDeleted = (flagsAsByte & NotesCAPI.SE_FSOFTDELETED) == NotesCAPI.SE_FSOFTDELETED;
		if (isSoftDeleted)
			flags.add(NoteFlags.SoftDeleted);
		
		return flags;
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
		 * @param noteId note id within database
		 * @param oid note originator id containing the UNID and the sequence number/date
		 * @param noteClass class of the note
		 * @param flags note flags
		 * @param dbCreated db replica id as timedate (part of Global Instance ID received from C API)
		 * @param noteModified modified in this file (part of Global Instance ID received from C API), same as note info {@link NotesCAPI#_NOTE_MODIFIED}
		 * @param summaryBufferData gives access to the note's summary buffer if {@link Search#SUMMARY} was specified; otherwise this value is null
		 * @return either {@link Action#Continue} to go on searching or {@link Action#Stop} to stop
		 */
		public abstract Action noteFound(NotesDatabase parentDb, int noteId, NotesOriginatorId oid, EnumSet<NoteClass> noteClass, EnumSet<NoteFlags> flags, NotesTimeDate dbCreated, NotesTimeDate noteModified, ItemTableData summaryBufferData);
		
		/**
		 * Implement this method to read deletion stubs. Method
		 * is only called when a <code>since</code> date is specified.
		 * 
		 * @param parentDb parent database
		 * @param noteId note id within database
		 * @param oid note originator id containing the UNID and the sequence number/date
		 * @param noteClass class of the note
		 * @param flags note flags
		 * @param dbCreated db replica id as timedate (part of Global Instance ID received from C API)
		 * @param noteModified modified in this file (part of Global Instance ID received from C API), same as note info {@link NotesCAPI#_NOTE_MODIFIED}
		 * @return either {@link Action#Continue} to go on searching or {@link Action#Stop} to stop
		 */
		public Action deletionStubFound(NotesDatabase parentDb, int noteId, NotesOriginatorId oid, EnumSet<NoteClass> noteClass, EnumSet<NoteFlags> flags, NotesTimeDate dbCreated, NotesTimeDate noteModified) {
			return Action.Continue;
		}
		
		/**
		 * Implement this method to receive notes that do not match the selection formula. Method
		 * is only called when a <code>since</code> date is specified.
		 * 
		 * @param parentDb parent database
		 * @param noteId note id within database
		 * @param oid note originator id containing the UNID and the sequence number/date
		 * @param noteClass class of the note
		 * @param flags note flags
		 * @param dbCreated db replica id as timedate (part of Global Instance ID received from C API)
		 * @param noteModified modified in this file (part of Global Instance ID received from C API), same as note info {@link NotesCAPI#_NOTE_MODIFIED}
		 * @param summaryBufferData gives access to the note's summary buffer if {@link Search#SUMMARY} was specified; otherwise this value is null
		 * @return either {@link Action#Continue} to go on searching or {@link Action#Stop} to stop
		 */
		public Action noteFoundNotMatchingFormula(NotesDatabase parentDb, int noteId, NotesOriginatorId oid, EnumSet<NoteClass> noteClass, EnumSet<NoteFlags> flags, NotesTimeDate dbCreated, NotesTimeDate noteModified, ItemTableData summaryBufferData) {
			return Action.Continue;
		}
		
	}
}
