package com.mindoo.domino.jna.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility functions that work with exceptions
 * 
 * @author Karsten Lehmann
 */
public class ExceptionUtil {

	/**
	 * The method checks whether <i>t</i> or one of the cause exceptions
	 * is of type <i>exceptionClass</i>.

	 * @param t exception
	 * @param exceptionClass exception class to look for
	 * @return true if <i>exceptionClass</i> could be found
	 */
	public static boolean isCause(Throwable t, Class<? extends Throwable> exceptionClass) {
		if (exceptionClass.isInstance(t))
			return true;
		Throwable cause=t.getCause();
		if (t==cause || cause==null)
			return false;
		else
			return isCause(cause, exceptionClass);
	}
	
	/**
	 * The method traveres the cause list and tries to find the specified exception type
	 * 
	 * @param t exception
	 * @param exceptionClass exception class to look for
	 * @return exception or null if not found
	 * 
	 * @param <T> type of exception
	 */
	public static <T extends Throwable> T findCauseOfType(Throwable t, Class<? extends Throwable> exceptionClass) {
		if (exceptionClass.isInstance(t))
			return (T) t;
		Throwable cause=t.getCause();
		if (t==cause || cause==null)
			return null;
		else
			return findCauseOfType(cause, exceptionClass);
	}
	
	
	/**
	 * Method to traverse the cause list of an exception, e.g. to search for a specific cause
	 * 
	 * @param t exception
	 * @param callback will be called with the exception and all available causes
	 */
	public static void traverseExceptionAndCauses(Throwable t, ICauseCallback callback) {
		callback.visitException(t);
		
		Throwable cause = t.getCause();
		if (cause!=null)
			traverseExceptionAndCauses(cause, callback);
	}
	
	/**
	 * Callback interface for method {@link ExceptionUtil#traverseExceptionAndCauses(Throwable, ICauseCallback)}
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface ICauseCallback {
		
		/**
		 * Method will be called with the exception and all available causes
		 * @param t exception/cause
		 */
		public void visitException(Throwable t);
		
	}

	/**
	 * Converts an exception to a string
	 * 
	 * @param e exception
	 * @return exception data as string
	 */
	public static String toString(Throwable e) {
		StringWriter sWriter = new StringWriter();
		PrintWriter pWriter = new PrintWriter(sWriter);
		e.printStackTrace(pWriter);
		return sWriter.toString();
	}
}
