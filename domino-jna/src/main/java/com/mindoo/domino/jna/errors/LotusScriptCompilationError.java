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
	
	public LotusScriptCompilationError(int id, String errorText, String errorFile) {
		super(id, toDetailedErrorMessage(id, errorText, errorFile));
		this.m_errorText = errorText;
		this.m_errorFile = errorFile;
	}
	
	private static String toDetailedErrorMessage(int id, String errorText, String errorFile) {
		return MessageFormat.format("{0}: errorText={1}, errorFile={2}", NotesErrorUtils.errToString((short)id), errorText, errorFile);
	}
	
	public String getErrorFile() {
		return m_errorFile;
	}
	
	public String getErrorText() {
		return m_errorText;
	}
}
