package com.mindoo.domino.jna.internal;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * Interfaces with methods new in 9.0.1
 * 
 * @author Karsten Lehmann
 */
public interface INotesNativeAPIV901 extends Library {

	public short CalGetApptunidFromUID(
			Memory pszUID,
			Memory pszApptunid,
			int dwFlags,
			Pointer pCtx);

}
