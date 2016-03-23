package com.mindoo.domino.jna.internal;

import com.sun.jna.win32.StdCallLibrary;

/**
 * Extension of {@link NotesCAPI} implementing {@link StdCallLibrary} required
 * on Windows platforms
 * 
 * @author Karsten Lehmann
 */
public interface Win32NotesCAPI extends NotesCAPI, StdCallLibrary {

}
