package com.mindoo.domino.jna.errors;

import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesConstants;
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
	 * @throws NotesError if the result is not 0
	 */
	public static void checkResult(short result) {
		if (result > 0) {
			short status = (short) (result & NotesConstants.ERR_MASK);
			if (status==0)
				return;
			
			String message;
			try {
				message = errToString(status);
			}
			catch (Throwable e) {
				throw new NotesError(result, "ERR "+result);
			}
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
	
		Memory retBuffer = new Memory(256);
		short outStrLength = NotesNativeAPI.get().OSLoadString(0, err, retBuffer, (short) 255);
		if (outStrLength==0) {
			return "";
		}
		Memory newRetBuffer = new Memory(outStrLength);
		for (int i=0; i<outStrLength; i++) {
			newRetBuffer.setByte(i, retBuffer.getByte(i));
		}
		
		String message = NotesStringUtils.fromLMBCS(newRetBuffer, outStrLength);
		return message;
	}

}
