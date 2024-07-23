package com.mindoo.domino.jna.utils;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.Mem;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ShortByReference;

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
		Memory variableNameMem = NotesStringUtils.toLMBCS(variableName, true);
		Memory rethValueBuffer = new Memory(NotesConstants.MAXENVVALUE);
		
		short result = NotesNativeAPI.get().OSGetEnvironmentString(variableNameMem, rethValueBuffer, NotesConstants.MAXENVVALUE);
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
		Memory variableNameMem = NotesStringUtils.toLMBCS(variableName, true);
		long longVal = NotesNativeAPI.get().OSGetEnvironmentLong(variableNameMem);
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
		String value = getEnvironmentString(variableName);
		if (StringUtil.isEmpty(value))
			return 0;
		
		try {
			int iVal = (int) Double.parseDouble(value);
			return iVal;
		}
		catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Method to get the value of a Domino or Notes environment variable string as a {@link NotesTimeDate}.<br>
	 * 
	 * @param variableName variable name
	 * @return value as {@link NotesTimeDate} or null if not found or value cannot be converted
	 */
	public static NotesTimeDate getEnvironmentTimeDate(String variableName) {
		Memory variableNameMem = NotesStringUtils.toLMBCS(variableName, true);
		NotesTimeDateStruct tdStruct = NotesTimeDateStruct.newInstance();
		
		short result = NotesNativeAPI.get().OSGetEnvironmentTIMEDATE(variableNameMem, tdStruct);
		if (result==1) {
			return new NotesTimeDate(tdStruct);
		}
		else {
			return null;
		}
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
		Memory variableNameMem = NotesStringUtils.toLMBCS(variableName, true);
		Memory valueMem = NotesStringUtils.toLMBCS(value, true);
		NotesNativeAPI.get().OSSetEnvironmentVariable(variableNameMem, valueMem);
	}
	
	/**
	 * Method to set a Domino or Notes environment variable to the value of the specified string.<br>
	 * The specified environment variable can be existing or new.<br>
	 * <br>
	 * Domino or Notes environment variables are stored in the file notes.ini.
	 * 
	 * @param variableName variable name
	 * @param value new value
	 * @param isSoft only check hard list of variables that are not settable as we are using the "soft" level 
	 */
	public static void setEnvironmentString(String variableName, String value, boolean isSoft) {
		Memory variableNameMem = NotesStringUtils.toLMBCS(variableName, true);
		Memory valueMem = NotesStringUtils.toLMBCS(value, true);
		NotesNativeAPI.get().OSSetEnvironmentVariableExt(variableNameMem, valueMem, isSoft ? (short) 1 : (short) 0);
	}
	
	/**
	 * OSSetEnvironmentInt is used to set the value of a Domino or Notes environment variable to the
	 * specified integer.<br>
	 * The environment variable can be an existing or new one.

	 * @param variableName variable name
	 * @param value new value
	 */
	public static void setEnvironmentInt(String variableName, int value) {
		Memory variableNameMem = NotesStringUtils.toLMBCS(variableName, true);
		NotesNativeAPI.get().OSSetEnvironmentInt(variableNameMem, value);
	}
	
	public static void setEnvironmentTimeDate(String variableName, NotesTimeDate td) {
		Memory variableNameMem = NotesStringUtils.toLMBCS(variableName, true);
		NotesTimeDateStruct tdStruct = td.getAdapter(NotesTimeDateStruct.class);
		NotesNativeAPI.get().OSSetEnvironmentTIMEDATE(variableNameMem, tdStruct);
	}
	
	/**
	 * Returns a word indicating the notes.ini vars changed
	 * 
	 * @return Notes.ini sequence number
	 */
	public static short getEnvironmentSeqNo() {
		return NotesNativeAPI.get().OSGetEnvironmentSeqNo();
	}
	
	/**
	 * Scans the Notes.ini for variables matching a search pattern
	 * 
	 * @param searchKeyword search pattern, either the name of a variable, a wildcard (e.g. "dir*") or "*" to get all entries
	 * @return case-insensitive map of Notes.ini variables and values
	 */
	public static Map<String,String> findEnvironmentEntries(String searchKeyword) {
		Memory sectionNameMem = NotesStringUtils.toLMBCS("Notes", true);
		Memory searchKeywordMem = NotesStringUtils.toLMBCS(searchKeyword == null ? "" : searchKeyword, true);
		
		DHANDLE.ByReference rethVarsList = DHANDLE.newInstanceByReference();
		
		short result = NotesNativeAPI.get().OSGetEnvAllConfigStrings(sectionNameMem, searchKeywordMem, rethVarsList);
		NotesErrorUtils.checkResult(result);
		
		if (rethVarsList.isNull()) {
			return Collections.emptyMap();
		}
		
		Map<String, String> entries = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		Pointer pEnvData = Mem.OSLockObject(rethVarsList);
		ShortByReference retTextLength = new ShortByReference();
		Memory retTextPointer = new Memory(Native.POINTER_SIZE);
		try {
			int numEntriesAsInt = (int) (NotesNativeAPI.get().ListGetNumEntries(pEnvData, 0) & 0xffff);
			for (int i=0; i<numEntriesAsInt; i+=2) {
				result = NotesNativeAPI.get().ListGetText(pEnvData, false, (char) i, retTextPointer, retTextLength);
				NotesErrorUtils.checkResult(result);
				
				String currKey = NotesStringUtils.fromLMBCS(retTextPointer.getPointer(0), -1);

				result = NotesNativeAPI.get().ListGetText(pEnvData, false, (char) (i+1), retTextPointer, retTextLength);
				NotesErrorUtils.checkResult(result);
				
				String currValue = NotesStringUtils.fromLMBCS(retTextPointer.getPointer(0), -1);

				entries.put(currKey, currValue);
			}
		}
		finally {
			Mem.OSUnlockObject(rethVarsList);
			Mem.OSMemFree(rethVarsList.getByValue());
		}
		
		return entries;
	}
}
