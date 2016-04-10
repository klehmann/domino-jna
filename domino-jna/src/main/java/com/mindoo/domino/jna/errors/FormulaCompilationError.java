package com.mindoo.domino.jna.errors;

/**
 * Subclass of {@link NotesError} that is thrown when formula compilation fails
 * and that provides details about the error
 * 
 * @author Karsten Lehmann
 */
public class FormulaCompilationError extends NotesError {
	private static final long serialVersionUID = -3252229485728491910L;
	private String m_formula;
	private short m_compileError;
	private short m_compileErrorLine;
	private short m_compileErrorColumn;
	private short m_compileErrorOffset;
	private short m_compileErrorLength;
	
	/**
	 * Creates a new instance
	 * 
	 * @param id error is
	 * @param msg error message
	 * @param formula formula for which compilation failed
	 * @param compileError compile error reason status code
	 * @param compileErrorLine line number
	 * @param compileErrorColumn column number
	 * @param compileErrorOffset offset
	 * @param compileErrorLength length of error
	 */
	public FormulaCompilationError(int id, String msg, String formula,
			short compileError,
			short compileErrorLine,
			short compileErrorColumn,
			short compileErrorOffset,
			short compileErrorLength) {
		
		super(id,  toDetailedErrorMessage(msg, formula,
				compileError,
				compileErrorLine,
				compileErrorColumn,
				compileErrorOffset,
				compileErrorLength));
		
		m_formula = formula;
		m_compileError = compileError;
		m_compileErrorLine = compileErrorLine;
		m_compileErrorColumn = compileErrorColumn;
		m_compileErrorOffset = compileErrorOffset;
		m_compileErrorLength = compileErrorLength;
	}
	
	private static String toDetailedErrorMessage(String msg, String formula,
			short compileError,
			short compileErrorLine,
			short compileErrorColumn,
			short compileErrorOffset,
			short compileErrorLength) {
		
		return msg + ". "+NotesErrorUtils.errToString(compileError)+", line="+
				compileErrorLine+", column="+compileErrorColumn+", offset="+compileErrorOffset+
				", length="+compileErrorLength+", formula="+formula;
	}
	
	/**
	 * Returns the formula that raised the error
	 * 
	 * @return formula
	 */
	public String getFormula() {
		return m_formula;
	}
	
	/**
	 * Returns the compilation error message
	 * 
	 * @return error message
	 */
	public String getCompileError() {
		return NotesErrorUtils.errToString(m_compileError);
	}
	
	/**
	 * Returns the line where the error occurred
	 * 
	 * @return line
	 */
	public int getCompileErrorLine() {
		return m_compileErrorLine;
	}
	
	/**
	 * Returns the column number where the error occurred
	 * 
	 * @return column
	 */
	public int getCompileErrorColumn() {
		return m_compileErrorColumn;
	}
	
	/**
	 * Returns the offset where the error occurred
	 * 
	 * @return offset
	 */
	public int getCompileErrorOffset() {
		return m_compileErrorOffset;
	}
	
	/**
	 * Returns the lengths of the error (probably the length of the error producing code)
	 * 
	 * @return length
	 */
	public int getCompileErrorLength() {
		return m_compileErrorLength;
	}
}
