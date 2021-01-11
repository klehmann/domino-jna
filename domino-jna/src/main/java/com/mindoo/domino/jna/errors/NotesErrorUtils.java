package com.mindoo.domino.jna.errors;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;

import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.utils.NotesStringUtils;

/**
 * Utility class to work with errors coming out of C API method calls
 * 
 * @author Karsten Lehmann
 */
public class NotesErrorUtils {
	private static volatile Map<Short,String> fallbackErrorTexts;
	
	private static synchronized Map<Short,String> getFallbackErrorTexts() {
		if (fallbackErrorTexts==null) {
			fallbackErrorTexts = AccessController.doPrivileged(new PrivilegedAction<Map<Short,String>>() {

				@Override
				public Map<Short,String> run() {
					ServiceLoader<IErrorTextProvider> loader =  ServiceLoader.load(IErrorTextProvider.class);
					
					List<IErrorTextProvider> providersSorted = new ArrayList<>();
					loader.forEach((currProvider) -> {
						providersSorted.add(currProvider);
					});
					providersSorted.sort(new Comparator<IErrorTextProvider>() {

						@Override
						public int compare(IErrorTextProvider o1, IErrorTextProvider o2) {
							int prio1 = o1.getPriority();
							int prio2 = o2.getPriority();
							
							if (prio1 < prio2) {
								return -1;
							}
							else if (prio1 > prio2) {
								return 1;
							}
							
							int hash1 = System.identityHashCode(o1);
							int hash2 = System.identityHashCode(o1);
							
							if (hash1 < hash2) {
								return -1;
							}
							else if (hash1 > hash2) {
								return 1;
							}
							
							return 0;
						}
					});
					
					Map<Short,String> allErrorTexts = new HashMap<>();
					
					for (IErrorTextProvider currProvider : providersSorted) {
						Map<Short,String> currTexts = currProvider.getErrorTexts();
						if (currTexts!=null) {
							for (Entry<Short,String> currEntry : currTexts.entrySet()) {
								short currCode = currEntry.getKey();
								String currTxt = currEntry.getValue();
								
								if (!allErrorTexts.containsKey(currCode)) {
									allErrorTexts.put(currCode, currTxt);
								}
							}
						}
					}
					
					return allErrorTexts;
				}
			});
		}
	
		return fallbackErrorTexts;
	}
	
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
	public static String errToString(int err) {
		return errToString((short) (err & 0xffff));
	}
	
	/**
	 * Converts a C API error code to an error message
	 * 
	 * @param err error code
	 * @return error message
	 */
	public static String errToString(short err) {
		short status = (short) (err & NotesConstants.ERR_MASK);
		
		if (status==0) {
			return "";
		}
	
		DisposableMemory retBuffer = new DisposableMemory(256);
		try {
			retBuffer.clear();
			
			short outStrLength = NotesNativeAPI.get().OSLoadString(0, status, retBuffer, (short) 255);
			if (outStrLength!=0) {
				String message = NotesStringUtils.fromLMBCS(retBuffer, outStrLength);
				return message;
			}
		}
		finally {
			retBuffer.dispose();
		}
		
		//there are some error codes (e.g. ERR_HTMLAPI_HTMLOPTION) for which OSLoadString did not return
		//a string; that's why we added the available error texts from the R9 C API to the project
		Map<Short,String> fallbackTexts = getFallbackErrorTexts();
		String txt = fallbackTexts.get(status);
		return txt==null ? "" : txt;
	}

}
