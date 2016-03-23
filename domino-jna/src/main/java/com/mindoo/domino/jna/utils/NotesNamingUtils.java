package com.mindoo.domino.jna.utils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.NotesNamesList32;
import com.mindoo.domino.jna.structs.NotesNamesList64;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Notes name utilities to convert between various Name formats
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
		Memory templateNameMem = templateName==null ? null : NotesStringUtils.toLMBCS(templateName); //used when abbrName is only a common name
		Memory inNameMem = NotesStringUtils.toLMBCS(name);
		Memory outNameMem = new Memory(NotesCAPI.MAXUSERNAME);
		ShortByReference outLength = new ShortByReference();
		
		short result = notesAPI.DNCanonicalize(0, templateNameMem, inNameMem, outNameMem, NotesCAPI.MAXUSERNAME, outLength);
		NotesErrorUtils.checkResult(result);
		
		String sOutName = NotesStringUtils.fromLMBCS(outNameMem);
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
		Memory templateNameMem = templateName==null ? null : NotesStringUtils.toLMBCS(templateName); //used when abbrName is only a common name
		Memory inNameMem = NotesStringUtils.toLMBCS(name);
		Memory outNameMem = new Memory(NotesCAPI.MAXUSERNAME);
		ShortByReference outLength = new ShortByReference();
		
		short result = notesAPI.DNAbbreviate(0, templateNameMem, inNameMem, outNameMem, NotesCAPI.MAXUSERNAME, outLength);
		NotesErrorUtils.checkResult(result);
		
		String sOutName = NotesStringUtils.fromLMBCS(outNameMem);
		return sOutName;
	}

	/**
	 * Computes the usernames list for the specified user, which is his name, name wildcards
	 * and all his groups and nested groups
	 * 
	 * @param userName username in canonical format
	 * @return usernames list
	 */
	public static List<String> getUserNamesList(String userName) {
		//make sure that username is canonical
		userName = toCanonicalName(userName);
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory userNameLMBCS = NotesStringUtils.toLMBCS(userName);
		
		if (NotesJNAContext.is64Bit()) {
			LongByReference rethNamesList = new LongByReference();
			short result = notesAPI.b64_NSFBuildNamesList(userNameLMBCS, 0, rethNamesList);
			NotesErrorUtils.checkResult(result);
			long hUserNamesList64 = rethNamesList.getValue();
			
			Pointer namesListBufferPtr = notesAPI.b64_OSLockObject(hUserNamesList64);
			
			try {
				NotesNamesList64 namesList = new NotesNamesList64(namesListBufferPtr);
				namesList.read();
				
				List<String> names = new ArrayList<String>(namesList.NumNames);
				
				ByteArrayOutputStream bOut = new ByteArrayOutputStream();
				
				long offset = namesList.size();
				
				while (names.size() < namesList.NumNames) {
					byte b = namesListBufferPtr.getByte(offset);

					if (b == 0) {
						Memory mem = new Memory(bOut.size());
						mem.write(0, bOut.toByteArray(), 0, bOut.size());
						String currUserName = NotesStringUtils.fromLMBCS(mem);
						names.add(currUserName);
						bOut.reset();
					}
					else {
						bOut.write(b);
					}
					offset++;
				}
				
				return names;
			}
			finally {
				notesAPI.b64_OSUnlockObject(hUserNamesList64);
				notesAPI.b64_OSMemFree(hUserNamesList64);
			}
		}
		else {
			IntByReference rethNamesList = new IntByReference();
			short result = notesAPI.b32_NSFBuildNamesList(userNameLMBCS, 0, rethNamesList);
			NotesErrorUtils.checkResult(result);
			int hUserNamesList32 = rethNamesList.getValue();
			
			Pointer namesListBufferPtr = notesAPI.b32_OSLockObject(hUserNamesList32);
			
			try {
				NotesNamesList32 namesList = new NotesNamesList32(namesListBufferPtr);
				namesList.read();
				
				List<String> names = new ArrayList<String>(namesList.NumNames);
				
				ByteArrayOutputStream bOut = new ByteArrayOutputStream();
				
				long offset = namesList.size();
				
				while (names.size() < namesList.NumNames) {
					byte b = namesListBufferPtr.getByte(offset);

					if (b == 0) {
						Memory mem = new Memory(bOut.size());
						mem.write(0, bOut.toByteArray(), 0, bOut.size());
						String currUserName = NotesStringUtils.fromLMBCS(mem);
						names.add(currUserName);
						bOut.reset();
					}
					else {
						bOut.write(b);
					}
					offset++;
				}
				
				return names;
			}
			finally {
				notesAPI.b32_OSUnlockObject(hUserNamesList32);
				notesAPI.b32_OSMemFree(hUserNamesList32);
			}
		
		}

	}
}
