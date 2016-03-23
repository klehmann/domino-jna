package com.mindoo.domino.jna.errors;

import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;

/**
 * Utility class to work with errors coming out of C API method calls
 * 
 * @author Karsten Lehmann
 */
public class NotesErrorUtils {

	/**
	 * Checks a Notes C API result code for errors and throws a {@link NotesError} with
	 * a proper error message if the specified result code is not 0.
	 * 
	 * @param result code
	 * @throws NotesError
	 */
	public static void checkResult(short result) {
		if (result > 0) {
			short status = (short) (result & NotesCAPI.ERR_MASK);
			if (status==0)
				return;
			
			String message = errToString(status);
			throw new NotesError(result, "ERR "+result+": "+ message);
		}
	}
	
	/**
	 * Converts a C API error code to an error message
	 * 
	 * @param err error code
	 * @return error message
	 */
	public static String errToString(short err) {
		if (err==0)
			return "";
	
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		Memory retBuffer = new Memory(256);
		short outStrLength;
		if (NotesJNAContext.is64Bit()) {
			outStrLength = notesAPI.b64_OSLoadString(0, err, retBuffer, (short) 255);
		}
		else {
			outStrLength = notesAPI.b32_OSLoadString(0, err, retBuffer, (short) 255);
		}
		Memory newRetBuffer = new Memory(outStrLength);
		for (int i=0; i<outStrLength; i++) {
			newRetBuffer.setByte(i, retBuffer.getByte(i));
		}
		
		String message = NotesStringUtils.fromLMBCS(newRetBuffer);
		return message;
	}

}
