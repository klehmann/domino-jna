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
		if (result==0)
			return;
		
		NotesError ex = toNotesError(result);
		if (ex==null)
			return;
		else
			throw ex;
	}

	/**
	 * Converts an error code into a {@link NotesError}.
	 * 
	 * @param result error code
	 * @return exception of null if no error
	 */
	public static NotesError toNotesError(short result) {
		short status = (short) (result & NotesConstants.ERR_MASK);
		if (status==0)
			return null;
		
		boolean isRemoteError = (result & NotesConstants.STS_REMOTE) == NotesConstants.STS_REMOTE;
		
		String message;
		try {
			message = errToString(status);
		}
		catch (Throwable e) {
			return new NotesError(result, "ERR "+status);
		}
		return new NotesError((int) (status & 0xffff), message + " (error code: "+status+(isRemoteError ? ", remote server error" : "")+", raw error with all flags: "+result+ ")");
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
