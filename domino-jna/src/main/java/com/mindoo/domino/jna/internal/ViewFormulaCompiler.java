package com.mindoo.domino.jna.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.mindoo.domino.jna.errors.FormulaCompilationError;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Utility class to generate the $FORMULA item of view design notes
 * 
 * @author Karsten Lehmann
 */
public class ViewFormulaCompiler {
	/**
	 * Method to generate the data for the $FORMULA item of a view definition by combining
	 * the view's selection formula with the programmatic names and formulas of the columns
	 * 
	 * @param selectionFormula selection formula
	 * @param columnItemNamesAndFormulas map with programmatic column names as keys and their formula as values, will be processed in key order
	 * @return handle to combined formula for 32 bit
	 */
	public static int b32_compile(String selectionFormula, LinkedHashMap<String,String> columnItemNamesAndFormulas) {
		return (int) b64_compile(selectionFormula, columnItemNamesAndFormulas);
	}
	
	/**
	 * Method to generate the data for the $FORMULA item of a view definition by combining
	 * the view's selection formula with the programmatic names and formulas of the columns
	 * 
	 * @param selectionFormula selection formula
	 * @param columnItemNamesAndFormulas map with programmatic column names as keys and their formula as values, will be processed in key order; if null, we simply compile the selection formula
	 * @return handle to combined formula for 64 bit
	 */
	public static long b64_compile(String selectionFormula, LinkedHashMap<String,String> columnItemNamesAndFormulas) {
		Memory formulaName = null;
		short formulaNameLength = 0;
		Memory selectionFormulaMem = NotesStringUtils.toLMBCS(selectionFormula, false);
		short selectionFormulaLength = (short) (selectionFormulaMem.size() & 0xffff);

		ShortByReference retFormulaLength = new ShortByReference();
		retFormulaLength.setValue((short) 0);
		ShortByReference retCompileError = new ShortByReference();
		retCompileError.setValue((short) 0);
		ShortByReference retCompileErrorLine = new ShortByReference();
		retCompileErrorLine.setValue((short) 0);
		ShortByReference retCompileErrorColumn = new ShortByReference();
		retCompileErrorColumn.setValue((short) 0);
		ShortByReference retCompileErrorOffset = new ShortByReference();
		retCompileErrorOffset.setValue((short) 0);
		ShortByReference retCompileErrorLength = new ShortByReference();
		retCompileErrorLength.setValue((short) 0);
		
		LongByReference rethViewFormula64 = new LongByReference();
		rethViewFormula64.setValue(0);
		long hViewFormula64 = 0;
		
		IntByReference rethViewFormula32 = new IntByReference();
		rethViewFormula32.setValue(0);
		int hViewFormula32 = 0;

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().NSFFormulaCompile(formulaName, formulaNameLength, selectionFormulaMem,
					selectionFormulaLength, rethViewFormula64, retFormulaLength, retCompileError, retCompileErrorLine,
					retCompileErrorColumn, retCompileErrorOffset, retCompileErrorLength);
		}
		else {
			result = NotesNativeAPI32.get().NSFFormulaCompile(formulaName, formulaNameLength, selectionFormulaMem,
					selectionFormulaLength, rethViewFormula32, retFormulaLength, retCompileError, retCompileErrorLine,
					retCompileErrorColumn, retCompileErrorOffset, retCompileErrorLength);
		}

		if (result == INotesErrorConstants.ERR_FORMULA_COMPILATION) {
			String errMsg = NotesErrorUtils.errToString(result);

			throw new FormulaCompilationError(result, errMsg, selectionFormula,
					retCompileError.getValue(),
					retCompileErrorLine.getValue(),
					retCompileErrorColumn.getValue(),
					retCompileErrorOffset.getValue(),
					retCompileErrorLength.getValue());
		}
		NotesErrorUtils.checkResult(result);
		
		if (PlatformUtils.is64Bit()) {
			hViewFormula64 = rethViewFormula64.getValue();
			if (hViewFormula64==0)
				throw new IllegalStateException("Selection formula handle is 0 for formula: "+selectionFormula);
		}
		else {
			hViewFormula32 = rethViewFormula32.getValue();
			if (hViewFormula32==0)
				throw new IllegalStateException("Selection formula handle is 0 for formula: "+selectionFormula);
		}

		if (columnItemNamesAndFormulas!=null) {
			boolean errorCompilingColumns = true;
			
			//keep track of what to dispose when compiling errors occur
			List<Long> columnFormulaHandlesToDisposeOnError64 = new ArrayList<Long>();
			List<Integer> columnFormulaHandlesToDisposeOnError32 = new ArrayList<Integer>();
			
			try {
				//compile each column and merge them with the view formula
				for (Entry<String,String> currEntry : columnItemNamesAndFormulas.entrySet()) {
					String columnItemName = currEntry.getKey();

					Memory columnItemNameMem = NotesStringUtils.toLMBCS(columnItemName, false);
					short columnItemNameLength = (short) (columnItemNameMem.size() & 0xffff);

					//add summary item definition for column
					if (PlatformUtils.is64Bit()) {
						result = NotesNativeAPI64.get().NSFFormulaSummaryItem(hViewFormula64, columnItemNameMem, columnItemNameLength);
						NotesErrorUtils.checkResult(result);
					}
					else {
						result = NotesNativeAPI32.get().NSFFormulaSummaryItem(hViewFormula32, columnItemNameMem, columnItemNameLength);
						NotesErrorUtils.checkResult(result);
					}

					String columnFormula = currEntry.getValue().trim();
					
					if (!StringUtil.isEmpty(columnFormula)) {
						//if we have a column formula, compile it and add it to the view formula
						Memory columnFormulaMem = NotesStringUtils.toLMBCS(columnFormula, false);
						short columnFormulaLength = (short) (columnFormulaMem.size() & 0xffff);
						
						ShortByReference retColumnFormulaLength = new ShortByReference();
						retColumnFormulaLength.setValue((short) 0);
						ShortByReference retColumnCompileError = new ShortByReference();
						retColumnCompileError.setValue((short) 0);
						ShortByReference retColumnCompileErrorLine = new ShortByReference();
						retColumnCompileErrorLine.setValue((short) 0);
						ShortByReference retColumnCompileErrorColumn = new ShortByReference();
						retColumnCompileErrorColumn.setValue((short) 0);
						ShortByReference retColumnCompileErrorOffset = new ShortByReference();
						retColumnCompileErrorOffset.setValue((short) 0);
						ShortByReference retColumnCompileErrorLength = new ShortByReference();
						retColumnCompileErrorLength.setValue((short) 0);
						
						LongByReference rethColumnFormula64 = new LongByReference();
						rethColumnFormula64.setValue(0);
						long hColumnFormula64 = 0;
						
						IntByReference rethColumnFormula32 = new IntByReference();
						rethColumnFormula32.setValue(0);
						int hColumnFormula32 = 0;
					
						if (PlatformUtils.is64Bit()) {
							result = NotesNativeAPI64.get().NSFFormulaCompile(columnItemNameMem, columnItemNameLength, columnFormulaMem,
									columnFormulaLength, rethColumnFormula64, retColumnFormulaLength, retColumnCompileError, retColumnCompileErrorLine,
									retColumnCompileErrorColumn, retColumnCompileErrorOffset, retColumnCompileErrorLength);
						}
						else {
							result = NotesNativeAPI32.get().NSFFormulaCompile(columnItemNameMem, columnItemNameLength, columnFormulaMem,
									columnFormulaLength, rethColumnFormula32, retColumnFormulaLength, retColumnCompileError, retColumnCompileErrorLine,
									retColumnCompileErrorColumn, retColumnCompileErrorOffset, retColumnCompileErrorLength);
						}

						if (result == INotesErrorConstants.ERR_FORMULA_COMPILATION) {
							String errMsg = NotesErrorUtils.errToString(result);

							throw new FormulaCompilationError(result, errMsg, columnFormula,
									retColumnCompileError.getValue(),
									retColumnCompileErrorLine.getValue(),
									retColumnCompileErrorColumn.getValue(),
									retColumnCompileErrorOffset.getValue(),
									retColumnCompileErrorLength.getValue());
						}
						NotesErrorUtils.checkResult(result);

						if (PlatformUtils.is64Bit()) {
							hColumnFormula64 = rethColumnFormula64.getValue();
							if (hColumnFormula64==0)
								throw new IllegalStateException("Column formula handle is 0 for formula: "+columnFormula);
							
							columnFormulaHandlesToDisposeOnError64.add(hColumnFormula64);
							
							//merge formulas
							result = NotesNativeAPI64.get().NSFFormulaMerge(hColumnFormula64, hViewFormula64);
							NotesErrorUtils.checkResult(result);
						}
						else {
							hColumnFormula32 = rethColumnFormula32.getValue();
							if (hColumnFormula32==0)
								throw new IllegalStateException("Column formula handle is 0 for formula: "+columnFormula);
							
							columnFormulaHandlesToDisposeOnError32.add(hColumnFormula32);
							
							//merge formulas
							result = NotesNativeAPI32.get().NSFFormulaMerge(hColumnFormula32, hViewFormula32);
							NotesErrorUtils.checkResult(result);
						}
					}
				}
				//all ok!
				errorCompilingColumns = false;
			}
			finally {
				//in any case free the compiled column memory
				if (PlatformUtils.is64Bit()) {
					for (Long currColumnFormulaHandle : columnFormulaHandlesToDisposeOnError64) {
						result = Mem64.OSMemFree(currColumnFormulaHandle.longValue());
						NotesErrorUtils.checkResult(result);
					}
				}
				else {
					for (Integer currColumnFormulaHandle : columnFormulaHandlesToDisposeOnError32) {
						result = Mem32.OSMemFree(currColumnFormulaHandle.intValue());
						NotesErrorUtils.checkResult(result);
					}
				}
				
				//and if errors occurred compiling the columns, free the view formula memory as well
				if (errorCompilingColumns) {
					if (PlatformUtils.is64Bit()) {
						result = Mem64.OSMemFree(hViewFormula64);
						NotesErrorUtils.checkResult(result);
					}
					else {
						result = Mem32.OSMemFree(hViewFormula32);
						NotesErrorUtils.checkResult(result);
					}
				}
			}
			
			if (errorCompilingColumns) {
				//should not happen; just avoiding to return a null handle in case of programming error
				throw new IllegalStateException("Unexpected state. There were unreported errors compiling the column formulas");
			}
		}
		
		if (PlatformUtils.is64Bit()) {
			if (hViewFormula64==0)
				throw new IllegalStateException("Unexpected state. Formula handle to be returned is null");
			return hViewFormula64;
		}
		else {
			if (hViewFormula32==0)
				throw new IllegalStateException("Unexpected state. Formula handle to be returned is null");
			return hViewFormula32;
		}
	}

}
