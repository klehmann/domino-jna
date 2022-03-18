package com.mindoo.domino.jna;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.mindoo.domino.jna.NotesIDTable.IDTableAsSet;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IAllocatedMemory;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.LMBCSStringList;
import com.mindoo.domino.jna.internal.Mem;
import com.mindoo.domino.jna.internal.Mem.LockedMemory;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPIV1200;
import com.mindoo.domino.jna.internal.NotesNativeAPIV1201;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.DHANDLE32;
import com.mindoo.domino.jna.internal.handles.DHANDLE64;
import com.mindoo.domino.jna.internal.handles.HANDLE;
import com.mindoo.domino.jna.internal.structs.NotesFieldFormulaStruct;
import com.mindoo.domino.jna.internal.structs.NotesQueryResultsHandles;
import com.mindoo.domino.jna.internal.structs.NotesResultsInfoStruct;
import com.mindoo.domino.jna.internal.structs.NotesResultsSortColumnStruct;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Aggregates, computes, sorts, and formats collections of documents across any
 * set of Domino databases.
 * 
 * @author Karsten Lehmann
 */
public class NotesQueryResultsProcessor implements IAllocatedMemory {
	public enum Categorized {
		TRUE,
		FALSE
	}

	public enum Hidden {
		TRUE,
		FALSE
	}

	/**
	 * Results processing options
	 */
	public enum QRPOptions {
		/** returns the UNID instead of noteid */
		RETURN_UNID,
		/** returns the replicaid instead of db filepath */
		RETURN_REPLICAID
	}

	public enum SortOrder {
		UNORDERED(0),
		ASCENDING(1),
		DESCENDING(2);

		private final int m_value;

		SortOrder(final int value) {
			this.m_value = value;
		}

		public int getValue() {
			return this.m_value;
		}
	}

	//since we need to manage multiple handles, we register a pseudo handle for this processor object
	//which is a unique value retrieved by incrementing qrpPseudoHandle
	private static final AtomicInteger qrpPseudoHandle = new AtomicInteger();

	private NotesDatabase m_db;
	private NotesQueryResultsHandles m_queryResultsHandles;
	private List<Reader> m_pendingJsonReader;
	//GC stuff
	private boolean m_freed;
	private int m_pseudoHandle;
	private List<Integer> m_pendingFormulaHandles = new ArrayList<>();
	private int m_totalNotesAdded = 0;
	
	/**
	 * Creates a new QueryResultsProcessor that runs in the context of the specified database
	 * 
	 * @param db database used to create QRP views
	 */
	public NotesQueryResultsProcessor(NotesDatabase db) {
		m_db = db;
		m_queryResultsHandles = NotesQueryResultsHandles.newInstance();
		m_pendingJsonReader = new ArrayList<>();

		//fetch a unique pseudo handle for GC
		m_pseudoHandle = qrpPseudoHandle.incrementAndGet();
		NotesGC.__memoryAllocated(this);
	}

	/**
	 * Checks if the database is already recycled
	 */
	private void checkHandle() {
		if (isFreed()) {
			throw new NotesError("QueryResultsProcessor already recycled");
		}

		if (m_db.isRecycled()) {
			throw new NotesError("Context database is recycled");
		}

		if (PlatformUtils.is64Bit()) {
			NotesGC.__b64_checkValidMemHandle(NotesQueryResultsProcessor.class, m_pseudoHandle);
		}
		else {
			NotesGC.__b32_checkValidMemHandle(NotesQueryResultsProcessor.class, m_pseudoHandle);
		}
	}

	@Override
	public boolean isFreed() {
		return m_freed;
	}

	@Override
	public int getHandle32() {
		return m_pseudoHandle;
	}

	@Override
	public long getHandle64() {
		return m_pseudoHandle;
	}

	@Override
	public void free() {
		if (isFreed()) {
			return;
		}

		NotesGC.__memoryBeeingFreed(this);

		synchronized (m_pendingJsonReader) {
			//make sure that all created readers have been closed
			for (Reader currReader : new ArrayList<>(m_pendingJsonReader)) { //prevent ConcurrentModificationException by creating a copy of the list
				try {
					currReader.close();
				}
				catch (IOException e) {
					//
				}
			}
			m_pendingJsonReader.clear();
		}

		if (m_queryResultsHandles!=null) {
			if (m_queryResultsHandles.hInResults!=0) {
				Mem.OSMemoryFree(m_queryResultsHandles.hInResults);
				m_queryResultsHandles.hInResults = 0;
			}

			if (m_queryResultsHandles.hOutFields!=0) {
				Mem.OSMemoryFree(m_queryResultsHandles.hOutFields);
				m_queryResultsHandles.hOutFields = 0;
			}

			if (m_queryResultsHandles.hFieldRules!=0) {
				Mem.OSMemoryFree(m_queryResultsHandles.hFieldRules);
				m_queryResultsHandles.hFieldRules = 0;
			}

			if (m_queryResultsHandles.hCombineRules!=0) {
				Mem.OSMemoryFree(m_queryResultsHandles.hCombineRules);
				m_queryResultsHandles.hCombineRules = 0;
			}
		}

		for (Integer currHandle : m_pendingFormulaHandles) {
			Mem.OSMemoryFree(currHandle);
		}
		m_pendingFormulaHandles.clear();

		m_freed = true;
	}

	/**
	 * Adds note ids from a database to the query results processor
	 *
	 * @param parentDb    database that contains the documents
	 * @param noteIds     note ids, e.g. an {@link NotesIDTable}
	 * @param resultsname A unique name (to the QueryResultsProcessor instance) of
	 *                    the input. This name is used in returned entries when the
	 *                    origin of results is desired and in addFormula method
	 *                    calls to override the data used to create sorted column
	 *                    values.
	 * @return this instance
	 */
	public NotesQueryResultsProcessor addNoteIds(NotesDatabase parentDb, Collection<Integer> noteIds, String resultsname) {
		checkHandle();

		NotesResultsInfoStruct ri = NotesResultsInfoStruct.newInstance();

		String dbServer = NotesNamingUtils.toCommonName(parentDb.getServer());
		String dbPath = StringUtil.isEmpty(dbServer) ? parentDb.getRelativeFilePath() : (dbServer + "!!" + parentDb.getRelativeFilePath());
		Memory dbPathMem = NotesStringUtils.toLMBCS(dbPath, true);
		byte[] dbPathArr = dbPathMem.getByteArray(0, (int) dbPathMem.size());

		if (dbPathArr.length > ri.dbPath.length) {
			throw new IllegalArgumentException("Database path length exceeds max size in bytes");
		}
		System.arraycopy(dbPathArr, 0, ri.dbPath, 0, dbPathArr.length);

		if (resultsname!=null && resultsname.length()>0) {
			Memory resultsnameMem = NotesStringUtils.toLMBCS(resultsname, true);
			byte[] resultsnameArr = resultsnameMem.getByteArray(0, (int) resultsnameMem.size());

			if (resultsnameArr.length > ri.name.length) {
				throw new IllegalArgumentException("Result name exceeds max size in bytes");
			}
			System.arraycopy(resultsnameArr, 0, ri.name, 0, resultsnameArr.length);
		}

		NotesIDTable idTableCopy;
		if (noteIds instanceof IDTableAsSet) {
			NotesIDTable noteIdsAsTable = ((IDTableAsSet)noteIds).getIDTable();
			idTableCopy = (NotesIDTable) noteIdsAsTable.clone();
		}
		else if (noteIds instanceof Collection) {
			idTableCopy = new NotesIDTable(noteIds);
		}
		else if (noteIds==null) {
			idTableCopy = new NotesIDTable();
		}
		else {
			throw new IllegalStateException("Unexpected type of noteIds: noteIds");
		}
		
		DHANDLE idTableCopyHandle = idTableCopy.getHandle();

		if (idTableCopyHandle instanceof DHANDLE64) {
			ri.hResults = (int) (((DHANDLE64)idTableCopyHandle).hdl & 0xffffffff);
		}
		else if (idTableCopyHandle instanceof DHANDLE32) {
			ri.hResults = ((DHANDLE32)idTableCopyHandle).hdl;
		}

		ri.write();

		Pointer qrHandlesPtr = m_queryResultsHandles.getPointer();

		IntByReference hretError = new IntByReference();

		short result = NotesNativeAPIV1200.get().NSFQueryAddToResultsList(NotesConstants.QUEP_LISTTYPE.INPUT_RESULTS_LST.getValue(),
				ri.getPointer(), qrHandlesPtr /* &hInResults */, hretError);
		NotesErrorUtils.checkResult(result);

		if (result!=0 && hretError.getValue()!=0) {
			if (hretError.getValue()!=0) {
				String errorMessage = NotesErrorUtils.errToString(result);

				String errorDetails;
				try (LockedMemory memErr = Mem.OSMemoryLock(hretError.getValue(), true)) {
					errorDetails = NotesStringUtils.fromLMBCS(memErr.getPointer(), -1);
				}

				throw new NotesError(result, errorMessage + " - " + errorDetails);
			}
			else {
				NotesErrorUtils.checkResult(result);
			}
		}

		m_totalNotesAdded += noteIds.size();
		m_queryResultsHandles.read();
		return this;
	}

	/**
	 * Convenience method to add an unsorted column to the processor
	 *
	 * @param name programmatic column name
	 * @return this instance
	 */
	public NotesQueryResultsProcessor addColumn(String name) {
		return addColumn(name, "", "", SortOrder.UNORDERED, Hidden.FALSE, Categorized.FALSE);
	}

	/**
	 * Creates a single column of values to be returned when
	 * {@link NotesQueryResultsProcessor} is executed. Column values can be generated from a field
	 * or a formula. Sorting order, categorization and hidden attributes determine the
	 * returned stream of results entries.<br>
	 * Columns span all input result sets and databases taking part in the {@link NotesQueryResultsProcessor}.
	 * Execute calls in the {@link NotesQueryResultsProcessor} require at least one column to be specified.
	 *
	 * @param name          The unique (within a QueryResultsProcessor instance)
	 *                      programmatic name of the column. If there is no override
	 *                      using the addFormula method call, the name provided will
	 *                      be treated as a field name in each database involved in
	 *                      the QueryResultsProcessor object. In JSON output, the
	 *                      name value is used the element name for each returned
	 *                      entry.<br>
	 *                      Values in the name field can specify aggregate
	 *                      functions. These functions require categorized columns
	 *                      and return computed numerical values across sets of
	 *                      results within a category. For aggregate functions
	 *                      requiring a name as an argument, the name can be
	 *                      overridden just as for a name without an aggregate
	 *                      function. For more information on aggregate functions,
	 *                      see the description of the isCategorized parameter.
	 * @param title         The display title of the column. Used only in generated
	 *                      views, the title is the UI column header.
	 * @param formula       Formula language string to serve as the default means of
	 *                      computing values for the column. If not supplied and if
	 *                      not overridden by an addFormula value, the name argument
	 *                      is treated as a field name. The precedence of supplying column values is:<br>
	 *                      <ol>
	 *                      <li>AddFormula Formula Language override</li>
	 *                      <li>Formula argument of the AddColumn method</li>
	 *                      <li>Use the name argument of the AddColumn method as a database field name</li>
	 *                      </ol>
	 *                      If supplied, the Formula Language provided is applied to the column
	 *                      across all results added using addCollection or addDominoQuery.<br>
	 *                      Formulas are not allowed on columns with aggregates.
	 * @param sortorder     A constant to indicate how the column should be sorted.
	 *                      Values are sorted case and accent insensitively by
	 *                      default. Multiple sort columns can have sort orders, and
	 *                      each order specified is sequentially applied in the
	 *                      order of addSortColumn calls. Field lists
	 *                      (multiply-occurring field values) are compared processed
	 *                      using field occurrences from first to last sequentially.
	 * @param ishidden      Sorts by a column value without returning the value. If
	 *                      true, the column cannot have a sort order of
	 *                      {@link SortOrder#UNORDERED} and cannot have an
	 *                      iscategorized value of true.
	 * @param iscategorized Categorized columns have a single entry returned for
	 *                      each unique value with entries containing that value
	 *                      nested under it. In JSON results, these nested entries
	 *                      are represented in arrays under each categorized unique
	 *                      value.<br>
	 *                      Multiply-occurring fields (i.e. lists) are not allowed
	 *                      to be categorized.<br>
	 *                      A categorized column creates values for any preceding
	 *                      uncategorized column in addition to the categorized
	 *                      column. Categorized columns can nest to create
	 *                      subcategories.
	 * @return this instance
	 */
	public NotesQueryResultsProcessor addColumn(String name, String title, String formula, SortOrder sortorder,
			Hidden ishidden, Categorized iscategorized) {
		NotesResultsSortColumnStruct rsc = NotesResultsSortColumnStruct.newInstance();
		rsc.write();

		if (StringUtil.isEmpty(name)) {
			throw new IllegalArgumentException("Column name cannot be empty");
		}

		if (title!=null && title.length()==0) {
			//title is optional
			title = null;
		}

		Memory colnameMem = NotesStringUtils.toLMBCS(name, true);
		byte[] colnameArr = colnameMem.getByteArray(0, (int) colnameMem.size());
		if (colnameArr.length > rsc.name.length) {
			throw new IllegalArgumentException("Column name exceeds max text length in bytes");
		}
		System.arraycopy(colnameArr, 0, rsc.name, 0, colnameArr.length);

		if (!StringUtil.isEmpty(title)) {
			Memory titleMem = NotesStringUtils.toLMBCS(title, true);
			byte[] titleArr = titleMem.getByteArray(0, (int) titleMem.size());
			if (titleArr.length > rsc.title.length) {
				throw new IllegalArgumentException("Title exceeds max text length in bytes");
			}
			System.arraycopy(titleArr, 0, rsc.title, 0, titleArr.length);
		}

		IntByReference rethFormulaStr = new IntByReference();

		if (!StringUtil.isEmpty(formula)) {
			Memory formulaStringMem = NotesStringUtils.toLMBCS(formula, true,  false);
			byte[] formulaStringArr = formulaStringMem.getByteArray(0, (int) formulaStringMem.size());

			Mem.OSMemoryAllocate(NotesConstants.BLK_MEM_ALLOC, formulaStringArr.length, rethFormulaStr);

			if (rethFormulaStr.getValue()==0) {
				throw new NotesError("Memory allocation for formula failed");
			}

			m_pendingFormulaHandles.add(rethFormulaStr.getValue());

			try (LockedMemory mem = Mem.OSMemoryLock(rethFormulaStr.getValue(), false)) {
				mem.getPointer().write(0, formulaStringArr, 0, formulaStringArr.length);
			}
			rsc.hColFormula = rethFormulaStr.getValue();
		}

		rsc.sortorder = sortorder.getValue();
		rsc.bHidden = ishidden == Hidden.TRUE;
		rsc.bCategorized = iscategorized == Categorized.TRUE;
		rsc.write();

		Pointer qrHandlesPtr = m_queryResultsHandles.getPointer();
		Pointer hOutFieldsPtr = qrHandlesPtr.share(4);

		IntByReference hretError = new IntByReference();

		short result = NotesNativeAPIV1200.get().NSFQueryAddToResultsList(NotesConstants.QUEP_LISTTYPE.SORT_COL_LST.getValue(),
				rsc.getPointer(), hOutFieldsPtr, hretError);

		if (result!=0 && hretError.getValue()!=0) {
			if (hretError.getValue()!=0) {
				String errorMessage = NotesErrorUtils.errToString(result);

				String errorDetails;
				try (LockedMemory memErr = Mem.OSMemoryLock(hretError.getValue(), true)) {
					errorDetails = NotesStringUtils.fromLMBCS(memErr.getPointer(), -1);
				}

				throw new NotesError(result, errorMessage + " - " + errorDetails);
			}
			else {
				NotesErrorUtils.checkResult(result);
			}
		}

		return this;
	}

	/**
	 * Provides Domino formula language to override the data used to generate values
	 * for a
	 * particular sort column and an input collection or set of collections. Since
	 * input collections
	 * can be created from different databases, design differences can be adjusted
	 * using addFormula
	 * to have homogenous values in the output.
	 *
	 * @param formula     Formula language string to be evaluated in order to supply
	 *                    the values for a sort column
	 * @param columnname  String value responding the programmatic name of the sort
	 *                    column whose values are to be generated using the formula
	 *                    language supplied
	 * @param resultsname Used to specify the input collection names to which will
	 *                    use the formula language to generate sort column values.
	 *                    Wildcards may be specified to map to multiple input
	 *                    collection names.
	 * @return this instance
	 */
	public NotesQueryResultsProcessor addFormula(String formula, String columnname, String resultsname) {
		checkHandle();

		if (StringUtil.isEmpty(formula)) {
			throw new IllegalArgumentException("Formula cannot be empty");
		}

		if (StringUtil.isEmpty(columnname)) {
			throw new IllegalArgumentException("Column name cannot be empty");
		}

		if (StringUtil.isEmpty(resultsname)) {
			throw new IllegalArgumentException("Results name cannot be empty");
		}

		Memory resultsnameMem = NotesStringUtils.toLMBCS(resultsname, true);
		byte[] resultsnameArr = resultsnameMem.getByteArray(0, (int) resultsnameMem.size());
		if (resultsnameArr.length > NotesConstants.MAX_CMD_VALLEN) {
			throw new IllegalArgumentException("Results name exceeds max size in bytes");
		}

		Memory columnnameMem = NotesStringUtils.toLMBCS(columnname, true);
		byte[] columnnameArr = columnnameMem.getByteArray(0, (int) columnnameMem.size());
		if (columnnameArr.length > NotesConstants.MAX_CMD_VALLEN) {
			throw new IllegalArgumentException("Column name exceeds max size in bytes");
		}

		Memory formulaMem = NotesStringUtils.toLMBCS(formula, true, false);
		byte[] formulaStringArr = formulaMem.getByteArray(0, (int) formulaMem.size());

		NotesFieldFormulaStruct ffStruct = NotesFieldFormulaStruct.newInstance();
		ffStruct.write();

		//write result set name
		System.arraycopy(resultsnameArr, 0, ffStruct.resultsname, 0, resultsnameArr.length);

		//write column name
		System.arraycopy(columnnameArr, 0, ffStruct.columnname, 0, columnnameArr.length);

		//write formula string
		{
			IntByReference rethFormula = new IntByReference();
			Mem.OSMemoryAllocate(NotesConstants.BLK_MEM_ALLOC, formulaStringArr.length, rethFormula);

			if (rethFormula.getValue()==0) {
				throw new NotesError("Memory allocation for formula failed");
			}

			try (LockedMemory mem = Mem.OSMemoryLock(rethFormula.getValue());) {
				mem.getPointer().write(0, formulaStringArr, 0, formulaStringArr.length);
			}

			ffStruct.hFormula = rethFormula.getValue();

			//dispose this later
			m_pendingFormulaHandles.add(ffStruct.hFormula);
		}

		ffStruct.write();
		Pointer ptrFF = ffStruct.getPointer();

		Pointer qrHandlesPtr = m_queryResultsHandles.getPointer();
		Pointer hFieldRulesPtr = qrHandlesPtr.share(4+4);

		IntByReference hretError = new IntByReference();

		short result = NotesNativeAPIV1200.get().NSFQueryAddToResultsList(NotesConstants.QUEP_LISTTYPE.FIELD_FORMULA_LST.getValue(),
				ptrFF, hFieldRulesPtr, hretError);

		if (result!=0 && hretError.getValue()!=0) {
			if (hretError.getValue()!=0) {
				String errorMessage = NotesErrorUtils.errToString(result);

				String errorDetails;
				try (LockedMemory memErr = Mem.OSMemoryLock(hretError.getValue(), true)) {
					errorDetails = NotesStringUtils.fromLMBCS(memErr.getPointer(), -1);
				}

				throw new NotesError(result, errorMessage + " - " + errorDetails);
			}
			else {
				NotesErrorUtils.checkResult(result);
			}
		}

		return this;
	}

	/**
	 * Processes the input collections in the manner specified by the Sort Columns,
	 * overriding field values with formulas specified via addFormula calls, and returns
	 * JSON output.<br>
	 * <br>
	 * The JSON syntax produced by {@link NotesQueryResultsProcessor} execution conforms
	 * to JSON RFC 8259.<br>
	 * All results are output under the <code>“StreamedResults”</code> top element key. For
	 * categorized results,
	 * all nested details are output under the “Documents” key.<br>
	 * Special keys <code>“@nid”</code> for NoteID and <code>“@DbPath”</code> are output so results can be
	 * acted upon on a document basis.<br>
	 * Fields that are lists on documents (multiply-occurring) are output as JSON
	 * arrays of like type.
	 *
	 * @param appendable the execution result is written in JSON format into this
	 *                   {@link Appendable} in small chunks (e.g. a {@link Writer} or {@link StringBuilder}
	 * @param options    options to tweak the JSON output or null/empty for default
	 *                   format
	 */
	public void executeToJSON(Appendable appendable, Set<QRPOptions> options) {
		if (appendable==null) {
			throw new IllegalArgumentException("Appendable is null");
		}
		
		checkHandle();
		
		if (m_totalNotesAdded==0) {
			//workaround for 12.0.0 / 12.0.1 issue where the produced json string is invalid
			try {
				appendable.append("{\"StreamResults\" :[]} ");
			} catch (IOException e) {
				throw new NotesError(0, "Error writing data to Appendable", e);
			}
		}

		m_queryResultsHandles.read();

		if (m_queryResultsHandles.hOutFields == 0) {
			throw new NotesError("No column has been defined");
		}

		if (m_db.isRecycled()) {
			throw new NotesError("Context database is recycled");
		}

		IntByReference hErrorText = new IntByReference();
		hErrorText.setValue(0);
		DHANDLE.ByReference hqueue = DHANDLE.newInstanceByReference();

		int dwFlags = NotesConstants.PROCRES_JSON_OUTPUT;
		if (options!=null) {
			if (options.contains(QRPOptions.RETURN_UNID)) {
				dwFlags |= NotesConstants.PROCRES_RETURN_UNID;
			}
			if (options.contains(QRPOptions.RETURN_REPLICAID)) {
				dwFlags |= NotesConstants.PROCRES_RETURN_REPLICAID;
			}
		}

		short result = NotesNativeAPIV1200.get().NSFProcessResults(m_db.getHandle().getByValue(),
				null, // viewname
				dwFlags,
				m_queryResultsHandles.hInResults,
				m_queryResultsHandles.hOutFields,
				m_queryResultsHandles.hFieldRules,
				m_queryResultsHandles.hCombineRules,
				hErrorText,
				hqueue);

		if (result == 0) {
			AtomicReference<DHANDLE.ByReference> hHoldQueueEntry = new AtomicReference<>();
			DHANDLE.ByReference hQueueEntry = DHANDLE.newInstanceByReference();

			try {
				short loopError = 0;

				while (loopError == 0) {
					loopError = NotesNativeAPI.get().QueueGet(hqueue.getByValue(), hQueueEntry);

					/* hqueueentry is reused for each segment. last time it is NULLHANDLE,
					so remember the handle for release */
					if (hHoldQueueEntry.get() == null) {
						hHoldQueueEntry.set(hQueueEntry);
					}

					if (loopError == 0 & !hQueueEntry.isNull()) {
						Pointer pinbuf = Mem.OSLockObject(hQueueEntry.getByValue());
						try {
							//skip header
							pinbuf = pinbuf.share(NotesConstants.queueEntryHeaderSize);
							pinbuf = pinbuf.share(NotesConstants.resultsStreamBufferHeaderSize);

							String dataStr = NotesStringUtils.fromLMBCS(pinbuf, -1);
							appendable.append(dataStr);
						} catch (IOException e) {
							throw new NotesError(0, "Error writing data to Appendable", e);
						}
						finally {
							Mem.OSUnlockObject(hQueueEntry.getByValue());	/* note: do not free until done with queue */
						}
					}

					/* not an error condition */
					if (loopError == INotesErrorConstants.ERR_QUEUE_EMPTY) {
						loopError = 0;
						break;
					}
				}

				result = loopError;
			}
			finally {
				if (hqueue!=null && !hqueue.isNull()) {
					short resultQueueDelete = NotesNativeAPI.get().QueueDelete(hqueue.getByValue());
					NotesErrorUtils.checkResult(resultQueueDelete);
				}

				if (hHoldQueueEntry.get()!=null && !hHoldQueueEntry.get().isNull()) {
					short resultMemFree = Mem.OSMemFree(hHoldQueueEntry.get().getByValue());
					NotesErrorUtils.checkResult(resultMemFree);
				}
			}
		}

		if (result!=0) {
			if (hErrorText.getValue()!=0) {
				try (LockedMemory errMsgMem = Mem.OSMemoryLock(hErrorText.getValue(), true);) {
					Pointer errMsgPtr = errMsgMem.getPointer();
					String errMsg = NotesStringUtils.fromLMBCS(errMsgPtr, -1);
					throw new NotesError(result, errMsg);
				}
			}
			else {
				NotesErrorUtils.checkResult(result);
			}
		}
	}

	/**
	 * Processes the input collections in the manner specified by the Sort Columns,
	 * overriding field values with formulas specified via addFormula calls, and
	 * returns JSON output.<br>
	 * <br>
	 * The JSON syntax produced by {@link NotesQueryResultsProcessor} execution conforms
	 * to JSON RFC 8259.<br>
	 * All results are output under the <code>“StreamedResults”</code> top element key. For
	 * categorized results,
	 * all nested details are output under the “Documents” key.<br>
	 * Special keys <code>“@nid”</code> for NoteID and <code>“@DbPath”</code> are output so results can be
	 * acted upon on a document basis.<br>
	 * Fields that are lists on documents (multiply-occurring) are output as JSON
	 * arrays of like type.
	 *
	 * @return reader to receive the the JSON data
	 * @param options options to tweak the JSON output or null/empty for default
	 *                format
	 */
	public Reader executeToJSON(Set<QRPOptions> options) {
		checkHandle();

		if (m_totalNotesAdded==0) {
			//workaround for 12.0.0 / 12.0.1 issue where the produced json string is invalid
			return new StringReader("{\"StreamResults\" :[]} ");
		}
		
		m_queryResultsHandles.read();

		if (m_queryResultsHandles.hOutFields == 0) {
			throw new NotesError("No column has been defined");
		}

		if (m_db.isRecycled()) {
			throw new NotesError("Database is recycled");
		}

		Memory viewNameMem = null;
		IntByReference hErrorText = new IntByReference();
		hErrorText.setValue(0);
		DHANDLE.ByReference hqueue = DHANDLE.newInstanceByReference();

		int dwFlags = NotesConstants.PROCRES_JSON_OUTPUT;
		if (options!=null) {
			if (options.contains(QRPOptions.RETURN_UNID)) {
				dwFlags |= NotesConstants.PROCRES_RETURN_UNID;
			}
			if (options.contains(QRPOptions.RETURN_REPLICAID)) {
				dwFlags |= NotesConstants.PROCRES_RETURN_REPLICAID;
			}
		}
		final int fDwFlags = dwFlags;

		HANDLE.ByValue dbHandleByVal = m_db.getHandle().getByValue();

		short result = NotesNativeAPIV1200.get().NSFProcessResults(dbHandleByVal, viewNameMem,
				fDwFlags, m_queryResultsHandles.hInResults, m_queryResultsHandles.hOutFields,
				m_queryResultsHandles.hFieldRules, m_queryResultsHandles.hCombineRules, hErrorText, hqueue);

		if (result!=0) {
			if (hErrorText.getValue()!=0) {
				try (LockedMemory errMsgMem = Mem.OSMemoryLock(hErrorText.getValue(), true);) {
					Pointer errMsgPtr = errMsgMem.getPointer();
					String errMsg = NotesStringUtils.fromLMBCS(errMsgPtr, -1);
					throw new NotesError(result, errMsg);
				}
			}
			else {
				NotesErrorUtils.checkResult(result);
			}
		}

		if (hqueue.isNull()) {
			return new StringReader("");
		}

		QueryResultsJSONReader reader = new QueryResultsJSONReader(this, hqueue, hErrorText);
		//register this reader to ensure it is closed on GC
		registerReaderForClose(reader);
		return reader;
	}

	/**
	 * Reader implementation to receive the JSON data from the queue
	 */
	private static class QueryResultsJSONReader extends Reader {
		private NotesQueryResultsProcessor m_processor;
		private DHANDLE.ByReference m_hqueue;
		private DHANDLE.ByReference m_hHoldQueueEntry;

		//the last read chunk of JSON data
		private String m_jsonChunk;
		//position of character to return next
		private int m_jsonChunkPos;

		private boolean m_eof;
		private boolean m_isClosed;
		private IntByReference m_hErrorText;

		public QueryResultsJSONReader(NotesQueryResultsProcessor processor, DHANDLE.ByReference hqueue,
				IntByReference hErrorText) {
			super();
			m_processor = processor;
			m_hqueue = hqueue;
			m_hErrorText = hErrorText;
		}

		@Override
		public void close() throws IOException {
			if (m_isClosed) {
				return;
			}

			if (m_hqueue!=null && !m_hqueue.isNull()) {
				DHANDLE.ByValue hqueueByVal = m_hqueue.getByValue();
				short resultQueueDelete = NotesNativeAPI.get().QueueDelete(hqueueByVal);
				NotesErrorUtils.checkResult(resultQueueDelete);
				m_hqueue.clear();
			}

			if (m_hHoldQueueEntry!=null && !m_hHoldQueueEntry.isNull()) {
				DHANDLE.ByValue hholdqueueentryByVal = m_hHoldQueueEntry.getByValue();
				short resultMemFree = Mem.OSMemFree(hholdqueueentryByVal);
				NotesErrorUtils.checkResult(resultMemFree);
				m_hHoldQueueEntry.clear();
			}

//			m_processor.checkHandle();
			m_processor.unregisterReaderForClose(this);

			m_isClosed = true;
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			if (cbuf.length==0) {
				return 0;
			}

			int numCharsCopied = 0;
			boolean isEOF = false;

			for (int i=off; i<(off+len); i++) {
				//read next character
				int currChar = read();
				if (currChar==-1) {
					//no more data
					isEOF = true;
					break;
				}
				cbuf[i] = (char) currChar;
				numCharsCopied++;
			}

			if (numCharsCopied==0 && isEOF) {
				return -1;
			}

			return numCharsCopied;
		}

		@Override
		public int read(char[] cbuf) throws IOException {
			return read(cbuf, 0, cbuf.length);
		}

		@Override
		public int read() throws IOException {
			if (m_jsonChunk==null || m_jsonChunkPos>=m_jsonChunk.length()) {
				m_jsonChunk = readNextChunk();
				m_jsonChunkPos = 0;
			}

			if (m_jsonChunkPos>=m_jsonChunk.length()) {
				return -1;
			}

			return m_jsonChunk.charAt(m_jsonChunkPos++);
		}

		private String readNextChunk() {
			m_processor.checkHandle();

			if (m_eof) {
				return "";
			}

			DHANDLE.ByValue m_hqueueByVal = m_hqueue.getByValue();

			DHANDLE.ByReference hQueueEntry = DHANDLE.newInstanceByReference();

			short readQueueError = NotesNativeAPI.get().QueueGet(m_hqueueByVal, hQueueEntry);

			/* hqueueentry is reused for each segment. last time it is NULLHANDLE,
			so remember the handle for release */
			if (m_hHoldQueueEntry == null) {
				m_hHoldQueueEntry = hQueueEntry;
			}

			String dataStr = "";
			if (readQueueError == 0 & !hQueueEntry.isNull()) {
				DHANDLE.ByValue hqueueentryByVal = hQueueEntry.getByValue();

				Pointer pinbuf = Mem.OSLockObject(hqueueentryByVal);
				try {
					//skip header
					pinbuf = pinbuf.share(NotesConstants.queueEntryHeaderSize);
					pinbuf = pinbuf.share(NotesConstants.resultsStreamBufferHeaderSize);

					dataStr = NotesStringUtils.fromLMBCS(pinbuf, -1);
				}
				finally {
					Mem.OSUnlockObject(hqueueentryByVal);	/* note: do not free until done with queue */
				}
			}

			/* not an error condition */
			if (readQueueError == INotesErrorConstants.ERR_QUEUE_EMPTY) {
				readQueueError = 0;
			}

			if (readQueueError!=0) {
				if (m_hErrorText.getValue()!=0) {
					try (LockedMemory errMsgMem = Mem.OSMemoryLock(m_hErrorText.getValue(), true);) {
						Pointer errMsgPtr = errMsgMem.getPointer();
						String errMsg = NotesStringUtils.fromLMBCS(errMsgPtr, -1);
						throw new NotesError(readQueueError, errMsg);
					}
				}
				else {
					NotesErrorUtils.checkResult(readQueueError);
				}
			}

			if (dataStr.length()==0) {
				m_eof = true;
			}

			return dataStr;
		}
	}

	/**
	 * Saves sorted QueryResultsProcessor results to a "results view" in a database.<br>
	 * Processes the input collections in the manner specified by the Sort Columns,
	 * overriding field values with formulas specified via addFormula calls.<br>
	 * Creates a results view in a host database and returns note id of the View.<br>
	 * <br>
	 * Results views created using the ExecuteToView method have the following distinctive
	 * characteristics.<br>
	 * <br>
	 * To open and manipulate results views using the HCL Notes® client or to write application
	 * code that utilizes it, it's important to understand these characteristics.<br>
	 * <br>
	 * Results views are created and persist in a database that you choose. Using a separate,
	 * non-production database is recommended. Doing so avoids unnecessary, routine database
	 * processing and also avoids user confusion over the views, which are not standard views.<br>
	 * <br>
	 * Results views are generated programmatically, so they are designed to be discarded after use. Therefore:<br>
	 * <ul>
	 * <li>They do not refresh automatically. If you want more recent data, you need to delete the old view using a method to remove in the View class or by running updall with the -Tx option, and then recreate and repopulate the view.</li>
	 * <li>They are automatically deleted during updall and dbmt task maintenance after their expiration time elapses.</li>
	 * </ul>
	 * Results views contain unique NoteIDs that cannot be referenced. Therefore:<br>
	 * <ul>
	 * <li>They do not generate document properties data in the Notes client.</li>
	 * <li>You can't open them using normal mouse gestures in the Notes client.</li>
	 * <li>You can't use full text indexing to search them; they are the results of such searches.</li>
	 * <li>You can use API calls that use those NoteIDs only within the context of the results views.</li>
	 * <li>They include hidden columns that contain the database path and the true NoteID for each originating document. You can access this information using view column processing.</li>
	 * </ul>
	 * Security for results views is implemented at the view level:<br>
	 * <ul>
	 * <li>By default, only the person or server creating the view can read the view data.</li>
	 * <li>You can use the Readers parameter to define a reader list.</li>
	 * <li>A person or server with access to the view gets access to all document details and aggregate values; there is no mechanism to restrict this access.</li>
	 * </ul>
	 * Domino processing of results views is otherwise typical.<br>
	 * <br>
	 * You can use Domino Designer to edit results views, with the exception of selection
	 * criteria and view formulas, which are specified when the views are created.
	 * 
	 * @param viewName The name of the results view to create and populate.
	 * @param hoursUntilExpire The time, in hours, for the view to be left in the host database. If not specified, it expires in 24 hours. You can extend the expiration time using the updall or dbmt tasks.
	 * @param readers These define the allowed Readers for the documents in the View (usernames and groups). Will be converted to canonical format
	 * @return view note id
	 */
	public int executeToView(String viewName, int hoursUntilExpire, Collection<String> readers) {
		checkHandle();

		m_queryResultsHandles.read();

		if (m_queryResultsHandles.hOutFields == 0) {
			throw new NotesError("No column has been defined");
		}

		if (m_db.isRecycled()) {
			throw new NotesError("Database is recycled");
		}

		Memory viewNameMem = NotesStringUtils.toLMBCS(viewName, true);
		List<String> readersCanonical = NotesNamingUtils.toCanonicalNames(readers);

		LMBCSStringList readersList = new LMBCSStringList(readersCanonical, false);

		IntByReference hErrorText = new IntByReference();
		hErrorText.setValue(0);
		DHANDLE.ByReference hqueue = DHANDLE.newInstanceByReference();

		final int timeoutsec = getTimeoutSec();
		final int maxEntries = getMaxEntries();

		ShortByReference hviewcoll = new ShortByReference();
		IntByReference viewnid = new IntByReference();

		short result = NotesNativeAPIV1201.get().NSFProcessResultsExt(
				m_db.getHandle().getByValue(),
				viewNameMem,
				NotesConstants.PROCRES_CREATE_VIEW,
				m_queryResultsHandles.hInResults,
				m_queryResultsHandles.hOutFields,
				m_queryResultsHandles.hFieldRules,
				m_queryResultsHandles.hCombineRules,
				readersList.getHandle().getByValue(),
				hoursUntilExpire,
				hErrorText,
				hqueue,
				hviewcoll,
				viewnid,
				timeoutsec*1000,
				maxEntries,
				0
				);

		if (result==0) {
			return viewnid.getValue();
		}
		else {
			if (hErrorText.getValue()!=0) {
				try (LockedMemory errMsgMem = Mem.OSMemoryLock(hErrorText.getValue(), true);) {
					Pointer errMsgPtr = errMsgMem.getPointer();
					String errMsg = NotesStringUtils.fromLMBCS(errMsgPtr, -1);
					throw new NotesError(result, errMsg);
				}
			}
			else {
				NotesErrorUtils.checkResult(result);
				//unreachable
				return 0;
			}
		}
	}

	/**
	 * Registers a reader to be closed when this allocations object is disposed
	 * 
	 * @param reader reader
	 */
	private void registerReaderForClose(Reader reader) {
		synchronized (m_pendingJsonReader) {
			m_pendingJsonReader.add(reader);
		}
	}

	/**
	 * Unregisters a reader from the auto close list. Used if it has been closed
	 * manually.
	 * 
	 * @param reader reader
	 */
	private void unregisterReaderForClose(Reader reader) {
		synchronized (m_pendingJsonReader) {
			m_pendingJsonReader.remove(reader);
		}
	}

	//might make sense to override these in subclasses; 0 means using defaults (or values read from Notes.ini)

	protected int getTimeoutSec() {
		return 0;
	}

	protected int getMaxEntries() {
		return 0;
	}

	@Override
	public String toString() {
		if (isFreed()) {
			return "NotesQueryResultsProcessor [freed]";
		}
		else if (m_db.isRecycled()) {
			return "NotesQueryResultsProcessor [handle="+m_pseudoHandle+", db=recycled]";
		}
		else {
			return "NotesQueryResultsProcessor [handle="+m_pseudoHandle+", server="+m_db.getServer()+
					", filepath="+m_db.getRelativeFilePath()+"]";
		}
	}


}
