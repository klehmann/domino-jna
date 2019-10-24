package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

public class FormulaDecompiler {

	/**
	 * Decompiles a compiled formula
	 * 
	 * @param compiledFormula compiled formula as byte array
	 * @return formula
	 */
	public static String decompileFormula(byte[] compiledFormula) {
		DisposableMemory mem = new DisposableMemory(compiledFormula.length);
		try {
			mem.write(0, compiledFormula, 0, compiledFormula.length);
			return decompileFormula(mem);
		}
		finally {
			mem.dispose();
		}
	}
	
	/**
	 * Decompiles a compiled formula
	 * 
	 * @param ptr pointer to compiled formula
	 * @return formula
	 */
	public static String decompileFormula(Pointer ptr) {
		if (PlatformUtils.is64Bit()) {
			LongByReference rethFormulaText = new LongByReference();
			ShortByReference retFormulaTextLength = new ShortByReference();
			short result = NotesNativeAPI64.get().NSFFormulaDecompile(ptr, false, rethFormulaText, retFormulaTextLength);
			NotesErrorUtils.checkResult(result);

			Pointer formulaPtr = Mem64.OSLockObject(rethFormulaText.getValue());
			try {
				int textLen = (int) (retFormulaTextLength.getValue() & 0xffff);
				String formula = NotesStringUtils.fromLMBCS(formulaPtr, textLen);
				return formula;
			}
			finally {
				Mem64.OSUnlockObject(rethFormulaText.getValue());
				result = Mem64.OSMemFree(rethFormulaText.getValue());
				NotesErrorUtils.checkResult(result);
			}
		}
		else {
			IntByReference rethFormulaText = new IntByReference();
			ShortByReference retFormulaTextLength = new ShortByReference();
			short result = NotesNativeAPI32.get().NSFFormulaDecompile(ptr, false, rethFormulaText, retFormulaTextLength);
			NotesErrorUtils.checkResult(result);
			
			Pointer formulaPtr = Mem32.OSLockObject(rethFormulaText.getValue());
			try {
				int textLen = (int) (retFormulaTextLength.getValue() & 0xffff);
				String formula = NotesStringUtils.fromLMBCS(formulaPtr, textLen);
				return formula;
			}
			finally {
				Mem32.OSUnlockObject(rethFormulaText.getValue());
				result = Mem32.OSMemFree(rethFormulaText.getValue());
				NotesErrorUtils.checkResult(result);
			}
		}	
	
	}
}
