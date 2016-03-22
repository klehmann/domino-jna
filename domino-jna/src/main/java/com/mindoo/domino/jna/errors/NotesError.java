package com.mindoo.domino.jna.errors;

/**
 * Exception class for Notes C API errors
 * 
 * @author Karsten Lehmann
 */
public class NotesError extends RuntimeException {
	private static final long serialVersionUID = 2712910384246785374L;
	private int m_id;
	
	/**
	 * Creates a new instance
	 * 
	 * @param id error code received from Notes
	 * @param message error message
	 */
	public NotesError(int id, String message) {
		super(message);
	}

	/**
	 * Created a new instance
	 * 
	 * @param id error code received from Notes
	 * @param message error message
	 * @param cause exception cause
	 */
	public NotesError(int id, String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Returns the C API error code
	 * 
	 * @return code
	 */
	public int getId() {
		return m_id;
	}
}
