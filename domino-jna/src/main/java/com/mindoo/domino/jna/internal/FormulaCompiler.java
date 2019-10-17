package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.errors.FormulaCompilationError;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

public class FormulaCompiler {

	/**
	 * Compiles a formula and returns the compiled binary result.
	 * 
	 * @param formula formula to compile
	 * @return compiled formula
	 * @throws FormulaCompilationError in case of compilation errors
	 */
	public static byte[] compileFormula(String formula) throws FormulaCompilationError {
		Memory formulaName = null;
		short formulaNameLength = 0;
		Memory formulaText = NotesStringUtils.toLMBCS(formula, false, false);
		short formulaTextLength = (short) formulaText.size();

		if (PlatformUtils.is64Bit()) {
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
			long hFormula64 = rethFormula.getValue();
			
			if (hFormula64==0) {
				//should not be 0
				return new byte[0];
			}
			
			int lengthCompiledFormula = retFormulaLength.getValue() & 0xffff;
			
			if (lengthCompiledFormula==0) {
				//should not be 0
				return new byte[0];
			}
					
			Pointer ptrCompiledFormula = Mem64.OSLockObject(hFormula64);
			try {
				byte[] compiledFormula = ptrCompiledFormula.getByteArray(0, lengthCompiledFormula);
				return compiledFormula;
			}
			finally {
				Mem64.OSUnlockObject(hFormula64);
			}
		}
		else {
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
			int hFormula32 = rethFormula.getValue();
			
			if (hFormula32==0) {
				//should not be 0
				return new byte[0];
			}
			
			int lengthCompiledFormula = retFormulaLength.getValue() & 0xffff;
			
			if (lengthCompiledFormula==0) {
				//should not be 0
				return new byte[0];
			}

			Pointer ptrCompiledFormula = Mem32.OSLockObject(hFormula32);
			try {
				byte[] compiledFormula = ptrCompiledFormula.getByteArray(0, lengthCompiledFormula);
				return compiledFormula;
			}
			finally {
				Mem32.OSUnlockObject(hFormula32);
			}
		}
	
	}
}
