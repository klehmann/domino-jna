package com.mindoo.domino.jna.formula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.NotesItem;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.FormulaAttributes;
import com.mindoo.domino.jna.errors.FormulaCompilationError;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.errors.UnsupportedItemValueError;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesCallbacks.NSFFORMCMDSPROC;
import com.mindoo.domino.jna.internal.NotesCallbacks.NSFFORMFUNCPROC;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks.NSFFORMCMDSPROCWin32;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks.NSFFORMFUNCPROCWin32;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.DHANDLE32;
import com.mindoo.domino.jna.internal.handles.DHANDLE64;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Utility class to execute a Domino formula on one or more {@link NotesNote} objects.<br>
 * <br>
 * The implementation supports more than the usual 64K of return value, e.g. to use
 * <code>@DBColumn</code> to read the column values in views with many entries.
 * 
 * @author Karsten Lehmann
 */
public class FormulaExecution implements IRecyclableNotesObject, IAdaptable {
	private String m_formula;
	
	private long m_hFormula64;
	private long m_hCompute64;
	
	private int m_hFormula32;
	private int m_hCompute32;
	
	private int m_compiledFormulaLength;
	
	private Pointer m_ptrCompiledFormula;
	
	private boolean m_preferNotesTimeDates;

	/**
	 * Creates a new instance. The constructure compiles the formula and throws a {@link FormulaCompilationError},
	 * if there are any compilation errors
	 * 
	 * @param formula formula
	 * @throws FormulaCompilationError if formula has wrong syntax
	 */
	public FormulaExecution(String formula) throws FormulaCompilationError {
		m_formula = formula;
		
		Memory formulaName = null;
		short formulaNameLength = 0;
		Memory formulaText = NotesStringUtils.toLMBCS(formula, false, false);
		short formulaTextLength = (short) formulaText.size();

		short computeFlags = 0;

		if (PlatformUtils.is64Bit()) {
			m_hFormula64 = 0;
			
			LongByReference rethFormula = new LongByReference();
			ShortByReference retFormulaLength = new ShortByReference();
			ShortByReference retCompileError = new ShortByReference();
			ShortByReference retCompileErrorLine = new ShortByReference();
			ShortByReference retCompileErrorColumn = new ShortByReference();
			ShortByReference retCompileErrorOffset = new ShortByReference();
			ShortByReference retCompileErrorLength = new ShortByReference();
			
			short result = NotesNativeAPI64.get().NSFFormulaCompile(formulaName, formulaNameLength, formulaText,
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
			m_compiledFormulaLength = (int) (retFormulaLength.getValue() & 0xffff);
			
			LongByReference rethCompute = new LongByReference();
			
			m_ptrCompiledFormula = Mem64.OSLockObject(m_hFormula64);
			
			result = NotesNativeAPI64.get().NSFComputeStart(computeFlags, m_ptrCompiledFormula, rethCompute);
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

			short result = NotesNativeAPI32.get().NSFFormulaCompile(formulaName, formulaNameLength, formulaText,
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
			m_compiledFormulaLength = (int) (retFormulaLength.getValue() & 0xffff);
			
			IntByReference rethCompute = new IntByReference();
			
			m_ptrCompiledFormula = Mem32.OSLockObject(m_hFormula32);
			
			result = NotesNativeAPI32.get().NSFComputeStart(computeFlags, m_ptrCompiledFormula, rethCompute);
			NotesErrorUtils.checkResult(result);
			
			m_hCompute32 = rethCompute.getValue();

			NotesGC.__objectCreated(FormulaExecution.class, this);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> clazz) {
		checkHandle();
		
		if (clazz == byte[].class) {
			//return compiled formula as byte array
			byte[] compiledFormula = m_ptrCompiledFormula.getByteArray(0, m_compiledFormulaLength);
			return (T) compiledFormula;
		}
		
		return null;
	}
	
	public String getFormula() {
		return m_formula;
	}
	
	/**
	 * Sets whether date/time values returned by the executed formulas should be
	 * returned as {@link NotesTimeDate} instead of being converted to {@link Calendar}.
	 * 
	 * @param b true to prefer NotesTimeDate (false by default)
	 */
	public void setPreferNotesTimeDates(boolean b) {
		m_preferNotesTimeDates = b;
	}
	
	/**
	 * Returns whether date/time values returned by the executed formulas should be
	 * returned as {@link NotesTimeDate} instead of being converted to {@link Calendar}.
	 * 
	 * @return true to prefer NotesTimeDate
	 */
	public boolean isPreferNotesTimeDates() {
		return m_preferNotesTimeDates;
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
		if (PlatformUtils.is64Bit()) {
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
			String txtVal = (String) ItemDecoder.decodeTextValue(valueDataPtr, valueDataLength, false);
			return txtVal==null ? Collections.emptyList() : Arrays.asList((Object) txtVal);
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TEXT_LIST) {
			List<Object> textList = valueDataLength==0 ? Collections.emptyList() : ItemDecoder.decodeTextListValue(valueDataPtr, false);
			return textList==null ? Collections.emptyList() : textList;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER) {
			double numVal = ItemDecoder.decodeNumber(valueDataPtr, valueDataLength);
			return Arrays.asList((Object) Double.valueOf(numVal));
		}
		else if (dataTypeAsInt == NotesItem.TYPE_NUMBER_RANGE) {
			List<Object> numberList = ItemDecoder.decodeNumberList(valueDataPtr, valueDataLength);
			return numberList==null ? Collections.emptyList() : numberList;
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME) {
			if (isPreferNotesTimeDates()) {
				NotesTimeDate td = ItemDecoder.decodeTimeDateAsNotesTimeDate(valueDataPtr, valueDataLength);
				return td==null ? Collections.emptyList() : Arrays.asList((Object) td);
			}
			else {
				Calendar cal = ItemDecoder.decodeTimeDate(valueDataPtr, valueDataLength);
				return cal==null ? Collections.emptyList() : Arrays.asList((Object) cal);
				
			}
		}
		else if (dataTypeAsInt == NotesItem.TYPE_TIME_RANGE) {
			if (isPreferNotesTimeDates()) {
				List<Object> tdValues = ItemDecoder.decodeTimeDateListAsNotesTimeDate(valueDataPtr);
				return tdValues==null ? Collections.emptyList() : tdValues;
			}
			else {
				List<Object> calendarValues = ItemDecoder.decodeTimeDateList(valueDataPtr);
				return calendarValues==null ? Collections.emptyList() : calendarValues;
			}
		}
		else if (dataTypeAsInt == NotesItem.TYPE_UNAVAILABLE) {
			//e.g. returned by formula "@DeleteDocument"
			return Collections.emptyList();
		}
		else if (dataTypeAsInt == NotesItem.TYPE_ERROR) {
			if (valueLength>=2) {
				short formulaErrorCode = valueDataPtr.getShort(0);
				String errMsg = NotesErrorUtils.errToString(formulaErrorCode);
				throw new NotesError(formulaErrorCode, "Could not evaluate formula "+m_formula+"\nError: "+errMsg);
			}
			else {
				throw new NotesError(0, "Could not evaluate formula "+m_formula);
			}
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
	 * @param note note to be used as additional variables available to the formula or null
	 * @return formula computation result with flags
	 */
	public FormulaExecutionResult evaluateExt(NotesNote note) {
		checkHandle();

		if (note!=null) {
			if (note.isRecycled()) {
				throw new NotesError(0, "Note is already recycled");
			}
		}

		if (PlatformUtils.is64Bit()) {
			LongByReference rethResult = new LongByReference();
			IntByReference retResultLength = new IntByReference();
			IntByReference retNoteMatchesFormula = new IntByReference();
			IntByReference retNoteShouldBeDeleted = new IntByReference();
			IntByReference retNoteModified = new IntByReference();

			//NSFComputeEvaluateExt supports more than the usual 64K of formula result data
			final int retDWordResultLength = 1;
			short result = NotesNativeAPI64.get().NSFComputeEvaluateExt(m_hCompute64, note==null ? 0 : note.getHandle64(), rethResult, retResultLength, retDWordResultLength,
					retNoteMatchesFormula, retNoteShouldBeDeleted, retNoteModified
					);
			NotesErrorUtils.checkResult(result);
			
			int valueLength = retResultLength.getValue();

			List<Object> formulaResult = null;
			
			long hResult = rethResult.getValue();
			if (hResult!=0) {
				Pointer valuePtr = Mem64.OSLockObject(hResult);
				
				try {
					formulaResult = parseFormulaResult(valuePtr, valueLength);
				}
				finally {
					Mem64.OSUnlockObject(hResult);
					result = Mem64.OSMemFree(hResult);
					NotesErrorUtils.checkResult(result);
				}
			}
			else {
				throw new IllegalStateException("got a null handle as computation result");
			}
			
			return new FormulaExecutionResult(formulaResult, retNoteMatchesFormula.getValue()==1,
					retNoteShouldBeDeleted.getValue()==1, retNoteModified.getValue()==1);
		}
		else {
			IntByReference rethResult = new IntByReference();
			IntByReference retResultLength = new IntByReference();
			IntByReference retNoteMatchesFormula = new IntByReference();
			IntByReference retNoteShouldBeDeleted = new IntByReference();
			IntByReference retNoteModified = new IntByReference();

			//NSFComputeEvaluateExt supports more than the usual 64K of formula result data
			final int retDWordResultLength = 1;
			short result = NotesNativeAPI32.get().NSFComputeEvaluateExt(m_hCompute32, note==null ? 0 : note.getHandle32(), rethResult, retResultLength, retDWordResultLength,
					retNoteMatchesFormula, retNoteShouldBeDeleted, retNoteModified
					);
			NotesErrorUtils.checkResult(result);
			
			List<Object> formulaResult = null;
			
			int hResult = rethResult.getValue();
			if (hResult!=0) {
				Pointer valuePtr = Mem32.OSLockObject(hResult);
				int valueLength = retResultLength.getValue();
				
				try {
					formulaResult = parseFormulaResult(valuePtr, valueLength);
				}
				finally {
					Mem32.OSUnlockObject(hResult);
					result = Mem32.OSMemFree(hResult);
					NotesErrorUtils.checkResult(result);
				}
			}
			else {
				throw new IllegalStateException("got a null handle as computation result");
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

		if (PlatformUtils.is64Bit()) {
			if (m_hCompute64!=0) {
				short result = NotesNativeAPI64.get().NSFComputeStop(m_hCompute64);
				NotesErrorUtils.checkResult(result);
				m_hCompute64=0;
			}
			if (m_hFormula64!=0) {
				Mem64.OSUnlockObject(m_hFormula64);
				short result = Mem64.OSMemFree(m_hFormula64);
				NotesErrorUtils.checkResult(result);
				
				NotesGC.__objectBeeingBeRecycled(FormulaExecution.class, this);
				m_hFormula64 = 0;
				m_ptrCompiledFormula=null;
			}
		}
		else {
			if (m_hCompute32!=0) {
				short result = NotesNativeAPI32.get().NSFComputeStop(m_hCompute32);
				NotesErrorUtils.checkResult(result);
				m_hCompute32=0;
			}
			if (m_hFormula32!=0) {
				Mem32.OSUnlockObject(m_hFormula32);
				short result = Mem32.OSMemFree(m_hFormula32);
				NotesErrorUtils.checkResult(result);
				
				NotesGC.__objectBeeingBeRecycled(FormulaExecution.class, this);
				m_hFormula32 = 0;
				m_ptrCompiledFormula=null;
			}
		}
	}

	@Override
	public boolean isRecycled() {
		if (PlatformUtils.is64Bit()) {
			if (m_hFormula64==0) {
				return true;
			}
		}
		else {
			if (m_hFormula32==0) {
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
	
	public DHANDLE getHandle() {
		if (PlatformUtils.is64Bit()) {
			return DHANDLE64.newInstance(m_hFormula64);
		}
		else {
			return DHANDLE32.newInstance(m_hFormula32);
		}
	}
	
	/**
	 * Returns a list of all registered (public) formula functions. Use {@link #getFunctionParameters(String)}
	 * to get a list of function parameters.
	 * 
	 * @return functions, e.g. "@Left("
	 */
	public static List<String> getAllFunctions() {
		List<String> retNames = new ArrayList<>();
		
		NSFFORMFUNCPROC callback;
		if (PlatformUtils.isWin32()) {
			callback = new NSFFORMFUNCPROCWin32() {

				@Override
				public short invoke(Pointer ptr) {
					String name = NotesStringUtils.fromLMBCS(ptr, -1);
					retNames.add(name);
					return 0;
				}
				
			};
		}
		else {
			callback = new NSFFORMFUNCPROC() {

				@Override
				public short invoke(Pointer ptr) {
					String name = NotesStringUtils.fromLMBCS(ptr, -1);
					retNames.add(name);
					return 0;
				}
				
			};
		}
		short result = NotesNativeAPI.get().NSFFormulaFunctions(callback);
		NotesErrorUtils.checkResult(result);
		
		return retNames;
	}

	/**
	 * Returns a list of all registered (public) formula commands. Use {@link #getFunctionParameters(String)}
	 * to get a list of command parameters.
	 * 
	 * @return commands, e.g. "MailSend"
	 */
	public static List<String> getAllCommands() {
		List<String> retNames = new ArrayList<>();
		
		NSFFORMCMDSPROC callback;
		if (PlatformUtils.isWin32()) {
			callback = new NSFFORMCMDSPROCWin32() {

				@Override
				public short invoke(Pointer ptr, short code, IntByReference stopFlag) {
					String name = NotesStringUtils.fromLMBCS(ptr, -1);
					retNames.add(name);
					return 0;
				}
				
			};
		}
		else {
			callback = new NSFFORMCMDSPROC() {

				@Override
				public short invoke(Pointer ptr, short code, IntByReference stopFlag) {
					String name = NotesStringUtils.fromLMBCS(ptr, -1);
					retNames.add(name);
					return 0;
				}
				
			};
		}
		short result = NotesNativeAPI.get().NSFFormulaCommands(callback);
		NotesErrorUtils.checkResult(result);
		
		return retNames;
	}
	
	public static class FormulaAnalyzeResult {
		private EnumSet<FormulaAttributes> attributes;
		
		private FormulaAnalyzeResult(EnumSet<FormulaAttributes> attributes) {
			this.attributes = attributes;
		}
		
		public Set<FormulaAttributes> getAttributes() {
			return Collections.unmodifiableSet(this.attributes);
		}

		@Override
		public String toString() {
			return "FormulaAnalyzeResult [attributes=" + attributes + "]";
		}
		
	}

	/**
	 * Scan through the function table (ftable) or keyword table (ktable)
	 * to find parameters of an @ function or an @ command.
	 * 
	 * @param formulaName name returned by {@link FormulaExecution#getAllFunctions()}, e.g. "@Left("
	 * @return function parameters, e.g. "stringToSearch; numberOfChars)stringToSearch; subString)" for the function "@Left("
	 */
	public static List<String> getFunctionParameters(String formulaName) {
		Memory formulaMem = NotesStringUtils.toLMBCS(formulaName, true);
		Pointer ptr = NotesNativeAPI.get().NSFFindFormulaParameters(formulaMem);
		if (ptr==null) {
			return Collections.emptyList();
		}
		else {
			//example format for @Middle(:
			//string; offset; numberchars)string; offset; endstring)string; startString; endstring)string; startString; numberchars)
			List<String> params = new ArrayList<>();
			String paramsConc = NotesStringUtils.fromLMBCS(ptr, -1);

			StringBuilder sb = new StringBuilder();

			for (int i=0; i<paramsConc.length(); i++) {
				char c = paramsConc.charAt(i);
				sb.append(c);

				if (c==')') {
					params.add(sb.toString());
					sb.setLength(0);
				}
			}
			return params;
		}
	}
	
	/**
	 * Analyzes the @-function
	 * 
	 * @return result with flags, e.g. whether the function returns a constant value or is time based
	 */
	public FormulaAnalyzeResult analyze() {
		checkHandle();
		
		DHANDLE hFormula = getHandle();
		
		IntByReference retAttributes = new IntByReference();
		ShortByReference retSummaryNamesOffset = new ShortByReference();
		
		short result = NotesNativeAPI.get().NSFFormulaAnalyze(hFormula.getByValue(),
				retAttributes,
				retSummaryNamesOffset);
		NotesErrorUtils.checkResult(result);
		
		EnumSet<FormulaAttributes> attributes = FormulaAttributes.toFormulaAttributes(retAttributes.getValue());
		return new FormulaAnalyzeResult(attributes);
	}
	
}
