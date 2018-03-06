package com.mindoo.domino.jna.errors;

import java.text.MessageFormat;

/**
 * Subclass of {@link NotesError} that is thrown when LotusScript compilation fails
 * and that provides details about the error.
 * 
 * @author Jesse Gallagher
 * @since 0.9.16
 */
public class LotusScriptCompilationError extends NotesError {
	private static final long serialVersionUID = 1L;

	private final String m_errorText;
	private final String m_errorFile;
	private final int m_line;
	
	public LotusScriptCompilationError(int id, int line, String errorText, String errorFile) {
		super(id, toDetailedErrorMessage(id, line, errorText, errorFile));
		this.m_errorText = errorText;
		this.m_errorFile = errorFile;
		this.m_line = line;
	}
	
	private static String toDetailedErrorMessage(int id, int line, String errorText, String errorFile) {
		return MessageFormat.format("{0}: line={1}, errorText={2}, errorFile={3}", NotesErrorUtils.errToString((short)id), line, errorText, errorFile);
	}
	
	/**
	 * @return the LS file name, if applicable
	 */
	public String getErrorFile() {
		return m_errorFile;
	}
	
	/**
	 * @return description of the error
	 */
	public String getErrorText() {
		return m_errorText;
	}
	
	/**
	 * @return source line number of the error, relative to the module containing the error, if applicable
	 */
	public int getLine() {
		return m_line;
	}
}
