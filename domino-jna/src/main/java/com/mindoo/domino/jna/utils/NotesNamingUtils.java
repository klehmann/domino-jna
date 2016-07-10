package com.mindoo.domino.jna.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mindoo.domino.jna.NotesNamesList;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.NotesNamesListHeader32;
import com.mindoo.domino.jna.structs.NotesNamesListHeader64;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Notes name utilities to convert between various Name formats and evaluate
 * user groups on a server.
 * 
 * @author Karsten Lehmann
 */
public class NotesNamingUtils {

	/**
	 * This function converts a distinguished name in abbreviated format to canonical format.
	 * A fully distinguished name is in canonical format - it contains all possible naming components.
	 * The abbreviated format of a distinguished name removes the labels from the naming components.
	 * 
	 * @param name name to convert
	 * @return canonical name
	 */
	public static String toCanonicalName(String name) {
		return toCanonicalName(name, null);
	}
	
	/**
	 * This function converts a distinguished name in abbreviated format to canonical format.
	 * A fully distinguished name is in canonical format - it contains all possible naming components.
	 * The abbreviated format of a distinguished name removes the labels from the naming components.
	 * 
	 * @param name name to convert
	 * @param templateName name to be used when the input name is in common name format
	 * @return canonical name
	 */
	public static String toCanonicalName(String name, String templateName) {
		if (name==null)
			return null;
		if (name.length()==0)
			return name;
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory templateNameMem = templateName==null ? null : NotesStringUtils.toLMBCS(templateName, true); //used when abbrName is only a common name
		Memory inNameMem = NotesStringUtils.toLMBCS(name, true);
		Memory outNameMem = new Memory(NotesCAPI.MAXUSERNAME);
		ShortByReference outLength = new ShortByReference();
		
		short result = notesAPI.DNCanonicalize(0, templateNameMem, inNameMem, outNameMem, NotesCAPI.MAXUSERNAME, outLength);
		NotesErrorUtils.checkResult(result);
		
		String sOutName = NotesStringUtils.fromLMBCS(outNameMem, (int) (outLength.getValue() & 0xffff));
		return sOutName;
	}
	
	/**
	 * This function converts a distinguished name in canonical format to abbreviated format.
	 * A fully distinguished name is in canonical format - it contains all possible naming components.
	 * The abbreviated format of a distinguished name removes the labels from the naming components.
	 * 
	 * @param name name to convert
	 * @return abbreviated name
	 */
	public static String toAbbreviatedName(String name) {
		return toAbbreviatedName(name, null);
	}

	/**
	 * This function converts a distinguished name in canonical format to abbreviated format.
	 * A fully distinguished name is in canonical format - it contains all possible naming components.
	 * The abbreviated format of a distinguished name removes the labels from the naming components.
	 * 
	 * @param name name to convert
	 * @param templateName name to be used when the input name is in common name format
	 * @return abbreviated name
	 */
	public static String toAbbreviatedName(String name, String templateName) {
		if (name==null)
			return null;
		if (name.length()==0)
			return name;
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory templateNameMem = templateName==null ? null : NotesStringUtils.toLMBCS(templateName, true); //used when abbrName is only a common name
		Memory inNameMem = NotesStringUtils.toLMBCS(name, true);
		Memory outNameMem = new Memory(NotesCAPI.MAXUSERNAME);
		ShortByReference outLength = new ShortByReference();
		
		short result = notesAPI.DNAbbreviate(0, templateNameMem, inNameMem, outNameMem, NotesCAPI.MAXUSERNAME, outLength);
		NotesErrorUtils.checkResult(result);
		
		String sOutName = NotesStringUtils.fromLMBCS(outNameMem, (int) (outLength.getValue() & 0xffff));
		return sOutName;
	}

	/**
	 * Writes the specified names list in null terminated LMBCS strings to a {@link ByteArrayOutputStream}
	 * 
	 * @param names names to write
	 * @param bOut target output stream
	 */
	private static void storeAsUserNamesList(List<String> names, ByteArrayOutputStream bOut) {
		//convert to canonical format
		List<String> namesCanonical = new ArrayList<String>(names.size());
		for (int i=0; i<names.size(); i++) {
			namesCanonical.add(toCanonicalName(names.get(i)));
		}
		
		for (int i=0; i<namesCanonical.size(); i++) {
			String currName = namesCanonical.get(i);
			Memory currNameLMBCS = NotesStringUtils.toLMBCS(currName, true);
			
			try {
				bOut.write(currNameLMBCS.getByteArray(0, (int) currNameLMBCS.size()));
			} catch (IOException e) {
				throw new NotesError(0, "Error writing to ByteArrayOutputStream");
			}
		}
	}
	
	/**
	 * Allocates memory in the Notes memory pool and writes a NAMES_LIST data structure with
	 * the specified names. The names are automatically converted to canonical format.
	 * 
	 * @param names names for names list, similar to result of @UserNamesList formula, e.g. usernames, wildcards, groups and roles; either abbreviated or canonical
	 * @return memory handle to NAMES_LIST
	 */
	private static int b32_writeUserNamesList(List<String> names) {
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		storeAsUserNamesList(names, bOut);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			throw new IllegalStateException("Only supported for 32 bit");
		}
		
		Memory namesListMem = new Memory(NotesCAPI.namesListHeaderSize32);
		NotesNamesListHeader32 namesListHeader = new NotesNamesListHeader32(namesListMem);
		namesListHeader.NumNames = (short) (names.size() & 0xffff);
		namesListHeader.write();
		
		IntByReference retHandle = new IntByReference();
		short result = notesAPI.b32_OSMemAlloc((short) 0, NotesCAPI.namesListHeaderSize32 + bOut.size(), retHandle);
		NotesErrorUtils.checkResult(result);
		
		final int retHandleAsInt = retHandle.getValue();
		
		//write the data
		Pointer ptr = notesAPI.b32_OSLockObject(retHandleAsInt);
		try {
			byte[] namesListByteArr = namesListMem.getByteArray(0, (int) namesListMem.size());
			ptr.write(0, namesListByteArr, 0, namesListByteArr.length);
			
			byte[] namesDataArr = bOut.toByteArray();
			ptr.write(namesListByteArr.length, namesDataArr, 0, namesDataArr.length);
		}
		finally {
			notesAPI.b32_OSUnlockObject(retHandleAsInt);
		}

		return retHandleAsInt;
	}

	/**
	 * Allocates memory in the Notes memory pool and writes a NAMES_LIST data structure with
	 * the specified names. The names are automatically converted to canonical format.
	 * 
	 * @param names names for names list, similar to result of @UserNamesList formula, e.g. usernames, wildcards, groups and roles; either abbreviated or canonical
	 * @return memory handle to NAMES_LIST
	 */
	private static long b64_writeUserNamesList(List<String> names) {
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		storeAsUserNamesList(names, bOut);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (!NotesJNAContext.is64Bit()) {
			throw new IllegalStateException("Only supported for 64 bit");
		}
		
		Memory namesListMem = new Memory(NotesCAPI.namesListHeaderSize64);
		NotesNamesListHeader64 namesListHeader = new NotesNamesListHeader64(namesListMem);
		namesListHeader.NumNames = (short) (names.size() & 0xffff);
		namesListHeader.write();

		LongByReference retHandle = new LongByReference();
		short result = notesAPI.b64_OSMemAlloc((short) 0, NotesCAPI.namesListHeaderSize64 + bOut.size(), retHandle);
		NotesErrorUtils.checkResult(result);
		
		long retHandleAsLong = retHandle.getValue();
		
		//write the data
		Pointer ptr = notesAPI.b64_OSLockObject(retHandleAsLong);
		try {
			byte[] namesListByteArr = namesListMem.getByteArray(0, (int) namesListMem.size());
			ptr.write(0, namesListByteArr, 0, namesListByteArr.length);
			
			byte[] namesDataArr = bOut.toByteArray();
			ptr.write(namesListByteArr.length, namesDataArr, 0, namesDataArr.length);
		}
		finally {
			notesAPI.b64_OSUnlockObject(retHandleAsLong);
		}
		
		return retHandleAsLong;
	}

	/**
	 * Programatically creates a {@link NotesNamesList}
	 * 
	 * @param names names for names list, similar to result of @UserNamesList formula, e.g. usernames, wildcards, groups and roles; either abbreviated or canonical
	 * @return names list
	 */
	public static NotesNamesList writeNewNamesList(List<String> names) {
		if (NotesJNAContext.is64Bit()) {
			long handle64 = b64_writeUserNamesList(names);
			NotesNamesList namesList = new NotesNamesList(handle64);
			NotesGC.__objectCreated(namesList);
			return namesList;
		}
		else {
			int handle32 = b32_writeUserNamesList(names);
			NotesNamesList namesList = new NotesNamesList(handle32);
			NotesGC.__objectCreated(namesList);
			return namesList;
		}
	}
	
	/**
	 * Computes a {@link NotesNamesList} structure with all name variants, wildcards and groups for
	 * the specified user
	 * 
	 * @param userName username, either abbreviated or canonical
	 * @return names list
	 */
	public static NotesNamesList buildNamesList(String userName) {
		//make sure that username is canonical
		userName = toCanonicalName(userName);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory userNameLMBCS = NotesStringUtils.toLMBCS(userName, true);
		
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethNamesList = new LongByReference();
			short result = notesAPI.b64_NSFBuildNamesList(userNameLMBCS, 0, rethNamesList);
			NotesErrorUtils.checkResult(result);
			long hUserNamesList64 = rethNamesList.getValue();
			
			NotesNamesList newList =  new NotesNamesList(hUserNamesList64);
			NotesGC.__objectCreated(newList);
			return newList;
		}
		else {
			IntByReference rethNamesList = new IntByReference();
			short result = notesAPI.b32_NSFBuildNamesList(userNameLMBCS, 0, rethNamesList);
			NotesErrorUtils.checkResult(result);
			int hUserNamesList32 = rethNamesList.getValue();
			
			NotesNamesList newList = new NotesNamesList(hUserNamesList32);
			NotesGC.__objectCreated(newList);
			return newList;
		}
	}
	
	/**
	 * Computes the usernames list for the specified user, which is his name, name wildcards
	 * and all his groups and nested groups
	 * 
	 * @param userName username in canonical format
	 * @return usernames list
	 */
	public static List<String> getUserNamesList(String userName) {
		NotesNamesList namesList = buildNamesList(userName);
		List<String> names = namesList.getNames();
		namesList.recycle();
		
		return names;
	}
	
}
