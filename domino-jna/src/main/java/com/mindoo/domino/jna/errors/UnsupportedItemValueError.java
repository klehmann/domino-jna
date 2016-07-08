package com.mindoo.domino.jna.errors;

import com.mindoo.domino.jna.NotesNote;

/**
 * The exception is thrown in {@link NotesNote} when an item value is read that cannot (yet)
 * be parsed.
 * 
 * @author Karsten Lehmann
 */
public class UnsupportedItemValueError extends NotesError {
	private static final long serialVersionUID = 3855708047676206228L;

	public UnsupportedItemValueError(String msg) {
		super(0, msg);
	}
	
}
