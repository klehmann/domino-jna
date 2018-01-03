package com.mindoo.domino.jna.utils;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.sun.jna.StringArray;

public class NotesInitUtils {
	/**
	 * This routine initializes the Notes runtime system for all environments.<br>
	 * <br>
	 * This routine can also be used for standalone programs running with Domino.<br>
	 * <br>
	 * After completing all C API actions and before the program exits, call {@link #notesTerm()}.<br>
	 * <br>
	 * NotesInitExtended uses argv[0] to attempt to locate the Domino or Notes executable directory.<br>
	 * If the Domino or Notes executable directory is not in argv[0], then NotesInitExtended will
	 * use the path to determine the Domino or Notes executable directory.
	 * Then it performs the same initialization that NotesInit() performs.<br>
	 * <br>
	 * As of release 5 of Domino and Notes, you can specify an ini file to NotesInitExtended,
	 * by passing an argument preceded by an "=" sign. This argument can be any argument
	 * except the first argument.<br>
	 * <br>
	 * The name of the ini file may include the full path specification.<br>
	 * <br>
	 * If the path is not included then the specified ini file must reside in the Domino or Notes
	 * executable directory.<br>
	 * <br>
	 * Use the following syntax followed by the file name in the argv list: example "=new.ini".<br>
	 * <br>
	 * Applications may use more than one thread of execution accessing Domino or Notes.<br>
	 * <br>
	 * Each new thread created by an application must call {@link #notesInitThread()} before
	 * making calls to Domino or Notes and {@link #notesTermThread()} before the thread terminates.<br>
	 * <br>
	 * Please refer to the reference entries for these functions for more information.<br>
	 * 
	 * @param argvArr arguments
	 */
	public static void notesInitExtended(String[] argvArr) {
		StringArray strArr = new StringArray(argvArr);

		short result = NotesNativeAPI.get().NotesInitExtended(argvArr.length, strArr);
		if (result!=0)
			throw new NotesError(result, "Error initializing Notes connection");
	}

	/**
	 *	A call to this routine will shut down the Domino or Notes runtime system.<br>
	 * <br>
	 * Other Lotus C API functions for Domino and Notes cannot be used after calling this routine.<br>
	 * <br>
	 * It is strongly suggested that applications terminate immediately after calling {@link #notesTerm()}.<br>
	 * Failing to do so, applications can hold onto resources which cannot be easily cleaned up in
	 * the event of a Notes/Domino crash.
	 */
	public static void notesTerm() {
		NotesNativeAPI.get().NotesTerm();
	}

	/**
	 * Initialize the Domino or Notes runtime system to support a new thread.<br>
	 * <br>
	 * Applications may make C API calls from more than one thread.<br>
	 * <br>
	 * Each new thread must call {@link #notesInitThread()} before making any other C API calls.<br>
	 * <br>
	 * The first thread calls {@link #notesInitExtended(String[])} which does the process
	 * initialization as well as the the initialization for that thread.<br>
	 * <br>
	 * Each subsequent thread must call {@link #notesInitThread()} and, before terminating,
	 * each subsequent thread must call {@link #notesTermThread()}.<br>
	 * <br>
	 * The last thread out must call {@link #notesTerm()}.<br>
	 * <br>
	 * However, the thread that calls {@link #notesInitExtended(String[])} does not have to
	 * be the thread that calls {@link #notesTerm()}.
	 */
	public static void notesInitThread() {
		short result = NotesNativeAPI.get().NotesInitThread();
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Each thread that calls {@link #notesInitExtended(String[])} must also call
	 * {@link #notesTermThread()} before the thread terminates.<br>
	 * <br>
	 * {@link #notesTermThread()} will free resources allocated by Domino or Notes for that thread.
	 */
	public static void notesTermThread() {
		NotesNativeAPI.get().NotesTermThread();
	}
}
