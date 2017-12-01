package com.mindoo.domino.jna.formula;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.errors.FormulaCompilationError;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.errors.UnsupportedItemValueError;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.utils.NotesDateTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Utility class to execute a Domino formula on one or more {@link NotesNote} objects.
 * 
 * @author Karsten Lehmann
 */
public class FormulaExecution implements IRecyclableNotesObject {
	private String m_formula;
	
	private long m_hFormula64;
	private long m_hCompute64;
	
	private int m_hFormula32;
	private int m_hCompute32;
	
	private Pointer m_ptrCompiledFormula;
	
	/**
	 * Creates a new instance. The constructure compiles the formula and throws a {@link FormulaCompilationError},
	 * if there are any compilation errors
	 * 
	 * @param formula formula
	 * @throws FormulaCompilationError if formula has wrong syntax
	 */
	public FormulaExecution(String formula) throws FormulaCompilationError {
		m_formula = formula;
		
		final NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		Memory formulaName = null;
		short formulaNameLength = 0;
		Memory formulaText = NotesStringUtils.toLMBCS(formula, false);
		short formulaTextLength = (short) formulaText.size();

		short computeFlags = 0;
		
		if (NotesJNAContext.is64Bit()) {
			m_hFormula64 = 0;
			
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
			m_hFormula64 = rethFormula.getValue();
			
			LongByReference rethCompute = new LongByReference();
			
			m_ptrCompiledFormula = notesAPI.b64_OSLockObject(m_hFormula64);
			
			result = notesAPI.b64_NSFComputeStart(computeFlags, m_ptrCompiledFormula, rethCompute);
			NotesErrorUtils.checkResult(result);
			
			m_hCompute64 = rethCompute.getValue();
			
			NotesGC.__objectCreated(FormulaExecution.class, this);
		}
		else {
			m_hFormula32 = 0;
			
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
			m_hFormula32 = rethFormula.getValue();
			
			IntByReference rethCompute = new IntByReference();
			
			m_ptrCompiledFormula = notesAPI.b32_OSLockObject(m_hFormula32);
			
			result = notesAPI.b32_NSFComputeStart(computeFlags, m_ptrCompiledFormula, rethCompute);
			NotesErrorUtils.checkResult(result);
			
			m_hCompute32 = rethCompute.getValue();
			
			NotesGC.__objectCreated(FormulaExecution.class, this);
		}
	}
	
	public String toString() {
		if (isRecycled()) {
			return "Compiled formula [recycled, formula="+m_formula+"]";
		}
		else {
			return "Compiled formula [formula="+m_formula+"]";
		}
	}
	
	/**
	 * Convenience method to execute a formula on a single note and return the result as a string.<br>
	 * <br>
	 * <b>Please note:<br>
	 * If the same formula should be run
	 * on multiple notes, you should consider to create a shared instance of {@link FormulaExecution}
	 * and run its {@link #evaluate(NotesNote)} method. Then the formula is parsed and compiled only
	 * once, which results in better performance and optimized memory usage.</b>
	 * 
	 * @param formula formula
	 * @param note note
	 * @return computation result as string; if the formula returns a list, we pick the first value
	 * @throws FormulaCompilationError if formula has wrong syntax
	 */
	public static String evaluateAsString(String formula, NotesNote note) throws FormulaCompilationError {
		List<Object> result = evaluate(formula, note);
		if (result.isEmpty())
			return "";
		else {
			return result.get(0).toString();
		}
	}
	
	/**
	 * Convenience method to execute a formula on a single note.<br>
	 * <br>
	 * <b>Please note:<br>
	 * If the same formula should be run
	 * on multiple notes, you should consider to create a shared instance of {@link FormulaExecution}
	 * and run its {@link #evaluate(NotesNote)} method. Then the formula is parsed and compiled only
	 * once, which results in better performance and optimized memory usage.</b>
	 * 
	 * @param formula formula
	 * @param note note
	 * @return computation result
	 * @throws FormulaCompilationError if formula has wrong syntax
	 */
	public static List<Object> evaluate(String formula, NotesNote note) throws FormulaCompilationError {
		FormulaExecution instance = new FormulaExecution(formula);
		try {
			List<Object> result = instance.evaluate(note);
			return result;
		}
		finally {
			instance.recycle();
		}
	}

	/**
	 * Convenience method to execute a formula on a single note. Provides extended information.<br>
	 * <br>
	 * <b>Please note:<br>
	 * If the same formula should be run
	 * on multiple notes, you should consider to create a shared instance of {@link FormulaExecution}
	 * and run its {@link #evaluate(NotesNote)} method. Then the formula is parsed and compiled only
	 * once, which results in better performance.</b>
	 * 
	 * @param formula formula
	 * @param note note
	 * @return computation result
	 * @throws FormulaCompilationError if formula has wrong syntax
	 */
	public static FormulaExecutionResult evaluateExt(String formula, NotesNote note) throws FormulaCompilationError {
		FormulaExecution instance = new FormulaExecution(formula);
		try {
			FormulaExecutionResult result = instance.evaluateExt(note);
			return result;
		}
		finally {
			instance.recycle();
		}
	}
	
	private void checkHandle() {
		if (NotesJNAContext.is64Bit()) {
			if (m_hCompute64==0) {
				throw new NotesError(0, "Object already recycled");
			}
			if (m_hFormula64==0) {
				throw new NotesError(0, "Object already recycled");
			}
		}
		else {
			if (m_hCompute32==0) {
				throw new NotesError(0, "Object already recycled");
			}
			if (m_hFormula32==0) {
				throw new NotesError(0, "Object already recycled");
			}
		}
	}
	
	private List<Object> parseFormulaResult(Pointer valuePtr, int valueLength) {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		short dataType = valuePtr.getShort(0);
		int dataTypeAsInt = (int) (dataType & 0xffff);
		
		boolean supportedType = false;
		if (dataTypeAsInt == NotesItem.TYPE_TEXT) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TEXT_LIST) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER_RANGE) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME_RANGE) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_UNAVAILABLE) {
			supportedType = true;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_ERROR) {
			supportedType = true;
		}
		
		if (!supportedType) {
			throw new UnsupportedItemValueError("Data type is currently unsupported: "+dataTypeAsInt);
		}

		int checkDataType = valuePtr.getShort(0) & 0xffff;
		Pointer valueDataPtr = valuePtr.share(2);
		int valueDataLength = valueLength - 2;
		
		if (checkDataType!=dataTypeAsInt) {
			throw new IllegalStateException("Value data type does not meet expected date type: found "+checkDataType+", expected "+dataTypeAsInt);
		}
		if (dataTypeAsInt == NotesItem.TYPE_TEXT) {
			String txtVal = (String) ItemDecoder.decodeTextValue(notesAPI, valueDataPtr, valueDataLength, false);
			return txtVal==null ? Collections.emptyList() : Arrays.asList((Object) txtVal);
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TEXT_LIST) {
			List<Object> textList = valueDataLength==0 ? Collections.emptyList() : ItemDecoder.decodeTextListValue(notesAPI, valueDataPtr, false);
			return textList==null ? Collections.emptyList() : textList;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER) {
			double numVal = ItemDecoder.decodeNumber(notesAPI, valueDataPtr, valueDataLength);
			return Arrays.asList((Object) Double.valueOf(numVal));
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER_RANGE) {
			List<Object> numberList = ItemDecoder.decodeNumberList(notesAPI, valueDataPtr, valueDataLength);
			return numberList==null ? Collections.emptyList() : numberList;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME) {
			boolean useDayLight = NotesDateTimeUtils.isDaylightTime();
			int gmtOffset = NotesDateTimeUtils.getGMTOffset();
			
			Calendar cal = ItemDecoder.decodeTimeDate(notesAPI, valueDataPtr, valueDataLength, useDayLight, gmtOffset);
			return cal==null ? Collections.emptyList() : Arrays.asList((Object) cal);
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME_RANGE) {
			boolean useDayLight = NotesDateTimeUtils.isDaylightTime();
			int gmtOffset = NotesDateTimeUtils.getGMTOffset();
			
			List<Object> calendarValues = ItemDecoder.decodeTimeDateList(notesAPI, valueDataPtr, useDayLight, gmtOffset);
			return calendarValues==null ? Collections.emptyList() : calendarValues;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_UNAVAILABLE) {
			//e.g. returned by formula "@DeleteDocument"
			return Collections.emptyList();
		}
		else if (dataTypeAsInt == NotesItem.TYPE_ERROR) {
			//TODO find a way to parse error details
			throw new NotesError(0, "Could not evaluate formula: "+m_formula);
		}
		else {
			throw new UnsupportedItemValueError("Data is currently unsupported: "+dataTypeAsInt);
		}
	}

	/**
	 * Evaluates the formula on a note
	 * 
	 * @param note note
	 * @return formula computation result
	 */
	public List<Object> evaluate(NotesNote note) {
		return evaluateExt(note).getValue();
	}

	/**
	 * Evaluates the formula on a note. Provides extended information.
	 * 
	 * @param note note
	 * @return formula computation result with flags
	 */
	public FormulaExecutionResult evaluateExt(NotesNote note) {
		checkHandle();
		
		if (note!=null) {
			if (note.isRecycled()) {
				throw new NotesError(0, "Note is already recycled");
			}
		}
		
		final NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethResult = new LongByReference();
			ShortByReference retResultLength = new ShortByReference();
			IntByReference retNoteMatchesFormula = new IntByReference();
			IntByReference retNoteShouldBeDeleted = new IntByReference();
			IntByReference retNoteModified = new IntByReference();
			
			short result = notesAPI.b64_NSFComputeEvaluate(m_hCompute64, note.getHandle64(), rethResult, retResultLength,
					retNoteMatchesFormula, retNoteShouldBeDeleted, retNoteModified);
			NotesErrorUtils.checkResult(result);
			
			List<Object> formulaResult = null;
			
			long hResult = rethResult.getValue();
			if (hResult!=0) {
				Pointer valuePtr = notesAPI.b64_OSLockObject(hResult);
				int valueLength = retResultLength.getValue() & 0xffff;
				
				try {
					formulaResult = parseFormulaResult(valuePtr, valueLength);
				}
				finally {
					notesAPI.b64_OSUnlockObject(hResult);
					notesAPI.b64_OSMemFree(hResult);
				}
			}
			
			return new FormulaExecutionResult(formulaResult, retNoteMatchesFormula.getValue()==1,
					retNoteShouldBeDeleted.getValue()==1, retNoteModified.getValue()==1);
		}
		else {
			IntByReference rethResult = new IntByReference();
			ShortByReference retResultLength = new ShortByReference();
			IntByReference retNoteMatchesFormula = new IntByReference();
			IntByReference retNoteShouldBeDeleted = new IntByReference();
			IntByReference retNoteModified = new IntByReference();

			short result = notesAPI.b32_NSFComputeEvaluate(m_hCompute32, note.getHandle32(), rethResult, retResultLength,
					retNoteMatchesFormula, retNoteShouldBeDeleted, retNoteModified);
			NotesErrorUtils.checkResult(result);
			
			List<Object> formulaResult = null;
			
			int hResult = rethResult.getValue();
			if (hResult!=0) {
				Pointer valuePtr = notesAPI.b32_OSLockObject(hResult);
				int valueLength = retResultLength.getValue() & 0xffff;
				
				try {
					formulaResult = parseFormulaResult(valuePtr, valueLength);
				}
				finally {
					notesAPI.b32_OSUnlockObject(hResult);
					notesAPI.b32_OSMemFree(hResult);
				}
			}
			
			return new FormulaExecutionResult(formulaResult, retNoteMatchesFormula.getValue()==1,
					retNoteShouldBeDeleted.getValue()==1, retNoteModified.getValue()==1);
		}
	}
	
	/**
	 * Formula computation result
	 * 
	 * @author Karsten Lehmann
	 */
	public static class FormulaExecutionResult {
		private List<Object> m_result;
		private boolean m_matchesFormula;
		private boolean m_shouldBeDeleted;
		private boolean m_noteModified;
		
		public FormulaExecutionResult(List<Object> result, boolean matchesFormula, boolean shouldBeDeleted, boolean noteModified) {
			m_result = result;
			m_matchesFormula = matchesFormula;
			m_shouldBeDeleted = shouldBeDeleted;
			m_noteModified = noteModified;
		}
		
		public List<Object> getValue() {
			return m_result;
		}
		
		public boolean matchesFormula() {
			return m_matchesFormula;
		}
		
		public boolean shouldBeDeleted() {
			return m_shouldBeDeleted;
		}
		
		public boolean isNoteModified() {
			return m_noteModified;
		}
		
	}

	@Override
	public void recycle() {
		if (isRecycled())
			return;

		final NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		if (NotesJNAContext.is64Bit()) {
			if (m_hCompute64!=0) {
				short result = notesAPI.b64_NSFComputeStop(m_hCompute64);
				NotesErrorUtils.checkResult(result);
				m_hCompute64=0;
			}
			if (m_hFormula64!=0) {
				notesAPI.b64_OSUnlockObject(m_hFormula64);
				notesAPI.b64_OSMemFree(m_hFormula64);
				
				NotesGC.__objectBeeingBeRecycled(FormulaExecution.class, this);
				m_hFormula64 = 0;
			}
		}
		else {
			if (m_hCompute32!=0) {
				short result = notesAPI.b32_NSFComputeStop(m_hCompute32);
				NotesErrorUtils.checkResult(result);
				m_hCompute32=0;
			}
			if (m_hFormula32!=0) {
				notesAPI.b32_OSUnlockObject(m_hFormula32);
				notesAPI.b32_OSMemFree(m_hFormula32);
				NotesGC.__objectBeeingBeRecycled(FormulaExecution.class, this);
				m_hFormula32 = 0;
			}
		}
	}

	@Override
	public boolean isRecycled() {
		if (NotesJNAContext.is64Bit()) {
			if (m_hFormula64==0 && m_hCompute64==0) {
				return true;
			}
		}
		else {
			if (m_hFormula32==0 && m_hCompute32==0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isNoRecycle() {
		return false;
	}
	
	@Override
	public int getHandle32() {
		return m_hFormula32;
	}

	@Override
	public long getHandle64() {
		return m_hFormula64;
	}
}
