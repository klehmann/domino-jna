package com.mindoo.domino.jna.utils;

import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.sun.jna.Memory;

/**
 * Utilities to read and write values in the Notes.ini file
 * 
 * @author Karsten Lehmann
 */
public class NotesIniUtils {

	/**
	 * Method to get the value of a Domino or Notes environment variable string.<br>
	 * <br>
	 * Domino and Notes environment variables are stored in the notes.ini file.
	 * Environment variables can be set from the Lotus Domino Server Console.<br>
	 * <br>
	 * Example:<br>
	 * <br>
	 * From Server Console:<br>
	 * <code>SET CONFIG TEST_STRING=THIS_IS_IT</code>
	 * 
	 * @param variableName variable name
	 * @return value
	 */
	public static String getEnvironmentString(String variableName) {
		NotesCAPI api = NotesJNAContext.getNotesAPI();
		
		Memory variableNameMem = NotesStringUtils.toLMBCS(variableName, true);
		Memory rethValueBuffer = new Memory(NotesCAPI.MAXENVVALUE);
		
		short result = api.OSGetEnvironmentString(variableNameMem, rethValueBuffer, NotesCAPI.MAXENVVALUE);
		if (result==1) {
			String str = NotesStringUtils.fromLMBCS(rethValueBuffer, -1);
			return str;
		}
		else {
			return "";
		}
	}

	/**
	 * This function takes the specified environment string, converts the text associated with it into type
	 * "long", and returns that value to the current context.<br>
	 * <br>
	 * Domino or Notes environment variables are stored in notes.ini, and can be set from the
	 * Lotus Domino Server via the SET CONFIG console command.<br>
	 * <br>
	 * Example (from Server Console):<br>
	 * <br>
	 * <code>SET CONFIG TEST_LONG=74000</code>
	 * 
	 * @param variableName variable name
	 * @return value as long
	 */
	public static long getEnvironmentLong(String variableName) {
		NotesCAPI api = NotesJNAContext.getNotesAPI();
		
		Memory variableNameMem = NotesStringUtils.toLMBCS(variableName, true);
		
		long longVal = api.OSGetEnvironmentLong(variableNameMem);
		return longVal;
	}

	/**
	 * This function takes the specified environment string, converts the text associated with it into type
	 * "int", and returns that value to the current context.<br>
	 * <br>
	 * Domino or Notes environment variables are stored in notes.ini, and can be set from the
	 * Lotus Domino Server via the SET CONFIG console command.<br>
	 * <br>
	 * Example (from Server Console):<br>
	 * <br>
	 * <code>SET CONFIG TEST_LONG=74000</code>
	 * 
	 * @param variableName variable name
	 * @return value as int
	 */
	public static int getEnvironmentInt(String variableName) {
		NotesCAPI api = NotesJNAContext.getNotesAPI();
		
		Memory variableNameMem = NotesStringUtils.toLMBCS(variableName, true);
		
		int intVal = api.OSGetEnvironmentInt(variableNameMem);
		return intVal;
	}

	/**
	 * Method to set a Domino or Notes environment variable to the value of the specified string.<br>
	 * The specified environment variable can be existing or new.<br>
	 * <br>
	 * Domino or Notes environment variables are stored in the file notes.ini.
	 * 
	 * @param variableName variable name
	 * @param value new value
	 */
	public static void setEnvironmentString(String variableName, String value) {
		NotesCAPI api = NotesJNAContext.getNotesAPI();
		Memory variableNameMem = NotesStringUtils.toLMBCS(variableName, true);
		Memory valueMem = NotesStringUtils.toLMBCS(value, true);
		api.OSSetEnvironmentVariable(variableNameMem, valueMem);
	}
	
	/**
	 * OSSetEnvironmentInt is used to set the value of a Domino or Notes environment variable to the
	 * specified integer.<br>
	 * The environment variable can be an existing or new one.

	 * @param variableName variable name
	 * @param value new value
	 */
	public static void setEnvironmentInt(String variableName, int value) {
		NotesCAPI api = NotesJNAContext.getNotesAPI();
		Memory variableNameMem = NotesStringUtils.toLMBCS(variableName, true);
		api.OSSetEnvironmentInt(variableNameMem, value);
	}
}
