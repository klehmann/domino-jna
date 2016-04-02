package com.mindoo.domino.jna.utils;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.sun.jna.StringArray;

public class NotesInitUtils {
	public static void notesInitExtended(String[] argvArr) {
		NotesCAPI api = NotesJNAContext.getNotesAPI();

		try {
			short result = api.NotesInitExtended(argvArr.length, new StringArray(argvArr));
			NotesErrorUtils.checkResult(result);
		}
		catch (Throwable t) {
			throw new RuntimeException("Could not initialize Notes API", t);
		}
	}

	public static void notesTerm() {
		NotesCAPI api = NotesJNAContext.getNotesAPI();
		api.NotesTerm();
	}

}
