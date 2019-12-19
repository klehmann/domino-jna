package com.mindoo.domino.jna.utils;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.NotesCallbacks;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.Win32NotesCallbacks;
import com.sun.jna.Pointer;

/**
 * Utility class that uses signal handlers of Domino to stop and get progress information
 * for long running operations.
 * 
 * @author Karsten Lehmann
 */
public class SignalHandlerUtil {
	private static final ConcurrentHashMap<Long, IBreakHandler> m_breakHandlerByThread = new ConcurrentHashMap<Long, IBreakHandler>();

	private static volatile boolean m_breakHandlerInstalled = false;
	private static volatile NotesCallbacks.OSSIGBREAKPROC prevBreakProc = null;
	private static final NotesCallbacks.OSSIGBREAKPROC breakProcWin = new Win32NotesCallbacks.OSSIGBREAKPROCWin32() {

		@Override
		public short invoke() {
			long threadId = Thread.currentThread().getId();
			IBreakHandler breakHandler = m_breakHandlerByThread.get(threadId);
			if (breakHandler!=null) {
				if (breakHandler.shouldInterrupt()) {
					return INotesErrorConstants.ERR_CANCEL;
				}
			}

			if (prevBreakProc!=null) {
				return prevBreakProc.invoke();
			}
			else {
				return 0;
			}
		}
		
	};
	private static final NotesCallbacks.OSSIGBREAKPROC breakProc = new NotesCallbacks.OSSIGBREAKPROC() {

		@Override
		public short invoke() {
			long threadId = Thread.currentThread().getId();
			IBreakHandler breakHandler = m_breakHandlerByThread.get(threadId);
			if (breakHandler!=null) {
				if (breakHandler.shouldInterrupt()) {
					return INotesErrorConstants.ERR_CANCEL;
				}
			}

			if (prevBreakProc!=null) {
				return prevBreakProc.invoke();
			}
			else {
				return 0;
			}
		}
		
	};
	
	/**
	 * Method to check whether the current threads has an active break handler
	 * 
	 * @return true if there is an active break handler
	 */
	public static boolean hasActiveBreakHandler() {
		NotesCallbacks.OSSIGBREAKPROC breakProc = (NotesCallbacks.OSSIGBREAKPROC) NotesNativeAPI.get().OSGetSignalHandler(NotesConstants.OS_SIGNAL_CHECK_BREAK);
		return breakProc != null;
	}
	
	public static synchronized void installGlobalBreakHandler() {
		if (!m_breakHandlerInstalled) {
			try {
				//AccessController call required to prevent SecurityException when running in XPages
				prevBreakProc = AccessController.doPrivileged(new PrivilegedExceptionAction<NotesCallbacks.OSSIGBREAKPROC>() {

					@Override
					public NotesCallbacks.OSSIGBREAKPROC run() throws Exception {
						if (PlatformUtils.isWin32()) {
							return (NotesCallbacks.OSSIGBREAKPROC) NotesNativeAPI.get().OSSetSignalHandler(NotesConstants.OS_SIGNAL_CHECK_BREAK, breakProcWin);
						}
						else {
							return (NotesCallbacks.OSSIGBREAKPROC) NotesNativeAPI.get().OSSetSignalHandler(NotesConstants.OS_SIGNAL_CHECK_BREAK, breakProc);
						}
					}
				});
			} catch (PrivilegedActionException e) {
				if (e.getCause() instanceof RuntimeException) 
					throw (RuntimeException) e.getCause();
				else
					throw new NotesError(0, "Error installing break handler", e);
			}
			m_breakHandlerInstalled = true;
		}
	}

	/**
	 * The method registers a break signal handler for the execution time of the specified
	 * {@link Callable}. The break signal handler can be used to send a break signal to Domino
	 * so that the current (probably long running) operation, e.g. a fulltext on a remote
	 * database, can be interrupted.
	 * 
	 * @param callable callable to execute
	 * @param breakHandler break handler to interrupt the current operation
	 * @return optional result
	 * @throws Exception of callable throws an error
	 * 
	 * @param <T> result type
	 */
	public static <T> T runInterruptable(Callable<T> callable, final IBreakHandler breakHandler) throws Exception {
		installGlobalBreakHandler();
		
		long threadId = Thread.currentThread().getId();
		m_breakHandlerByThread.put(threadId, breakHandler);
		try {
			T result = callable.call();
			return result;
		}
		finally {
			m_breakHandlerByThread.remove(threadId);
		}
	}
	
	/**
	 * Implement this method to send break signals to long running operations
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IBreakHandler {

		/**
		 * Make sure that this method does not do any heavy computation as it is called a
		 * lot by Domino.
		 * 
		 * @return true to send break signal
		 */
		public boolean shouldInterrupt();

	}
	

	/**
	 * The method registers a progress signal handler for the execution time of the specified
	 * {@link Callable}. The progress signal handler can be used to get notified about the
	 * progress of method execution, e.g. replication or copy operations.
	 * 
	 * @param callable callable to execute
	 * @param progressHandler progress handler to get notified about progress changes
	 * @return optional result
	 * @throws Exception of callable throws an error
	 * 
	 * @param <T> result type
	 */
	public static <T> T runWithProgress(Callable<T> callable, final IProgressListener progressHandler) throws Exception {
		final NotesCallbacks.OSSIGPROGRESSPROC progressProc;
		final Thread callThread = Thread.currentThread();

		//store a previously registered signal handler for this thread:
		final NotesCallbacks.OSSIGPROGRESSPROC[] prevProc = new NotesCallbacks.OSSIGPROGRESSPROC[1];

		if (PlatformUtils.isWin32()) {
			progressProc = new Win32NotesCallbacks.OSSIGPROGRESSPROCWin32() {

				@Override
				public short invoke(short option, Pointer data1, Pointer data2) {
					try {
						if (Thread.currentThread() == callThread) {
							if (option == NotesConstants.PROGRESS_SIGNAL_BEGIN) {
								progressHandler.begin();
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_END) {
								progressHandler.end();
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_SETRANGE) {
								long range = Pointer.nativeValue(data1);
								progressHandler.setRange(range);
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_SETTEXT) {
								String str = NotesStringUtils.fromLMBCS(data1, -1);
								progressHandler.setText(str);
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_SETPOS) {
								long pos = Pointer.nativeValue(data1);
								progressHandler.setPos(pos);
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_DELTAPOS) {
								long deltapos = Pointer.nativeValue(data1);
								progressHandler.setDeltaPos(deltapos);
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_SETBYTERANGE) {
								long totalbytes = Pointer.nativeValue(data1);
								progressHandler.setByteRange(totalbytes);
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_SETBYTEPOS) {
								long bytesDone = Pointer.nativeValue(data1);
								progressHandler.setBytePos(bytesDone);
							}
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					if (prevProc[0]!=null) {
						return prevProc[0].invoke(option, data1, data2);
					}
					else
						return 0;
				}
			};
		}
		else {
			progressProc = new NotesCallbacks.OSSIGPROGRESSPROC() {

				@Override
				public short invoke(short option, Pointer data1, Pointer data2) {
					try {
						if (Thread.currentThread() == callThread) {
							if (option == NotesConstants.PROGRESS_SIGNAL_BEGIN) {
								progressHandler.begin();
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_END) {
								progressHandler.end();
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_SETRANGE) {
								long range = Pointer.nativeValue(data1);
								progressHandler.setRange(range);
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_SETTEXT) {
								String str = NotesStringUtils.fromLMBCS(data1, -1);
								progressHandler.setText(str);
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_SETPOS) {
								long pos = Pointer.nativeValue(data1);
								progressHandler.setPos(pos);
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_DELTAPOS) {
								long deltapos = Pointer.nativeValue(data1);
								progressHandler.setDeltaPos(deltapos);
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_SETBYTERANGE) {
								long totalbytes = Pointer.nativeValue(data1);
								progressHandler.setByteRange(totalbytes);
							}
							else if (option == NotesConstants.PROGRESS_SIGNAL_SETBYTEPOS) {
								long bytesDone = Pointer.nativeValue(data1);
								progressHandler.setBytePos(bytesDone);
							}
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					if (prevProc[0]!=null) {
						return prevProc[0].invoke(option, data1, data2);
					}
					else
						return 0;
				}
			};
		}

		try {
			//register our signal handler and store the previous one if there is any;
			//the signal handler is thread specific so we do not get race conditions
			//between setting the new handler and restoring the old one
			
			//AccessController call required to prevent SecurityException when running in XPages
			prevProc[0] = AccessController.doPrivileged(new PrivilegedExceptionAction<NotesCallbacks.OSSIGPROGRESSPROC>() {

				@Override
				public NotesCallbacks.OSSIGPROGRESSPROC run() throws Exception {
					return (NotesCallbacks.OSSIGPROGRESSPROC) NotesNativeAPI.get().OSSetSignalHandler(NotesConstants.OS_SIGNAL_PROGRESS, progressProc);
				}
			});
			
			T result = callable.call();
			return result;
		}
		finally {
			//restore original signal handler if we are still the active signal handler (should always be the case,
			//since the signal handlers are thread specific)
			
			//AccessController call required to prevent SecurityException when running in XPages
			NotesCallbacks.OSSIGPROGRESSPROC currProc = AccessController.doPrivileged(new PrivilegedExceptionAction<NotesCallbacks.OSSIGPROGRESSPROC>() {

				@Override
				public NotesCallbacks.OSSIGPROGRESSPROC run() throws Exception {
					return (NotesCallbacks.OSSIGPROGRESSPROC) NotesNativeAPI.get().OSGetSignalHandler(NotesConstants.OS_SIGNAL_PROGRESS);
				}
			});
			if (progressProc.equals(currProc)) {
				NotesNativeAPI.get().OSSetSignalHandler(NotesConstants.OS_SIGNAL_PROGRESS, prevProc[0]);
			}
		}
	}
	
	/**
	 * Definition of function that will handle the progress bar signal.<br>
	 * <br>
	 * The progress signal handler displays a progress bar.<br>
	 * The progress position will generally start at 0 and end at Range.<br>
	 * The current progress supplied is either absolute ({@link #setPos(long)}) or a delta from the
	 * previous progress state ({@link #setDeltaPos(long)}).<br>
	 * As the operation which is supplying progress information is peformed, the range may change.<br>
	 * <br>
	 * If it does, an additional {@link #setRange(long)} will be signaled.
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IProgressListener {
		
		public void begin();
		
		public void end();
		
		public void setRange(long range);
		
		public void setText(String str);
		
		public void setPos(long pos);
		
		public void setDeltaPos(long pos);
		
		public void setByteRange(long range);
		
		public void setBytePos(long pos);
		
	}
	

	/**
	 * The method registers a replication state signal handler for the execution time of the specified
	 * {@link Callable}. The signal handler can be used to get notified about the
	 * replication progress.
	 * 
	 * @param callable callable to execute
	 * @param replStateHandler replication state handler to get notified about replication state changes
	 * @return optional result
	 * @throws Exception of callable throws an error
	 * 
	 * @param <T> result type
	 */
	public static <T> T runWithReplicationStateTracking(Callable<T> callable,
			final IReplicationStateListener replStateHandler) throws Exception {
		final NotesCallbacks.OSSIGREPLPROC replProc;
		final Thread callThread = Thread.currentThread();

		//store a previously registered signal handler for this thread:
		final NotesCallbacks.OSSIGREPLPROC[] prevProc = new NotesCallbacks.OSSIGREPLPROC[1];

		if (PlatformUtils.isWin32()) {
			replProc = new Win32NotesCallbacks.OSSIGREPLPROCWin32() {

				@Override
				public void invoke(short state, Pointer pText1, Pointer pText2) {
					try {
						if (Thread.currentThread() == callThread) {
							if (state == NotesConstants.REPL_SIGNAL_IDLE) {
								replStateHandler.idle();
							}
							else if (state == NotesConstants.REPL_SIGNAL_PICKSERVER) {
								replStateHandler.pickServer();
							}
							else if (state == NotesConstants.REPL_SIGNAL_CONNECTING) {
								String server = NotesStringUtils.fromLMBCS(pText1, -1);
								String port = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.connecting(server, port);
							}
							else if (state == NotesConstants.REPL_SIGNAL_SEARCHING) {
								String server = NotesStringUtils.fromLMBCS(pText1, -1);
								String port = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.searching(server, port);
							}
							else if (state == NotesConstants.REPL_SIGNAL_SENDING) {
								String serverFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String localFile = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.sending(serverFile, localFile);
							}
							else if (state == NotesConstants.REPL_SIGNAL_RECEIVING) {
								String serverFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String localFile = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.receiving(serverFile, localFile);
							}
							else if (state == NotesConstants.REPL_SIGNAL_SEARCHINGDOCS) {
								String srcFile = NotesStringUtils.fromLMBCS(pText1, -1);
								replStateHandler.searchingDocs(srcFile);
							}
							else if (state == NotesConstants.REPL_SIGNAL_DONEFILE) {
								String localFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String replFileStats = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.doneFile(localFile, replFileStats);
							}
							else if (state == NotesConstants.REPL_SIGNAL_REDIRECT) {
								String serverFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String localFile = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.redirect(serverFile, localFile);
							}
							else if (state == NotesConstants.REPL_SIGNAL_BUILDVIEW) {
								replStateHandler.buildView();
							}
							else if (state == NotesConstants.REPL_SIGNAL_ABORT) {
								replStateHandler.abort();
							}
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					if (prevProc[0]!=null) {
						prevProc[0].invoke(state, pText1, pText2);
					}
				}
			};
		}
		else {
			replProc = new NotesCallbacks.OSSIGREPLPROC() {

				@Override
				public void invoke(short state, Pointer pText1, Pointer pText2) {
					try {
						if (Thread.currentThread() == callThread) {
							if (state == NotesConstants.REPL_SIGNAL_IDLE) {
								replStateHandler.idle();
							}
							else if (state == NotesConstants.REPL_SIGNAL_PICKSERVER) {
								replStateHandler.pickServer();
							}
							else if (state == NotesConstants.REPL_SIGNAL_CONNECTING) {
								String server = NotesStringUtils.fromLMBCS(pText1, -1);
								String port = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.connecting(server, port);
							}
							else if (state == NotesConstants.REPL_SIGNAL_SEARCHING) {
								String server = NotesStringUtils.fromLMBCS(pText1, -1);
								String port = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.searching(server, port);
							}
							else if (state == NotesConstants.REPL_SIGNAL_SENDING) {
								String serverFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String localFile = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.sending(serverFile, localFile);
							}
							else if (state == NotesConstants.REPL_SIGNAL_RECEIVING) {
								String serverFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String localFile = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.receiving(serverFile, localFile);
							}
							else if (state == NotesConstants.REPL_SIGNAL_SEARCHINGDOCS) {
								String srcFile = NotesStringUtils.fromLMBCS(pText1, -1);
								replStateHandler.searchingDocs(srcFile);
							}
							else if (state == NotesConstants.REPL_SIGNAL_DONEFILE) {
								String localFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String replFileStats = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.doneFile(localFile, replFileStats);
							}
							else if (state == NotesConstants.REPL_SIGNAL_REDIRECT) {
								String serverFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String localFile = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.redirect(serverFile, localFile);
							}
							else if (state == NotesConstants.REPL_SIGNAL_BUILDVIEW) {
								replStateHandler.buildView();
							}
							else if (state == NotesConstants.REPL_SIGNAL_ABORT) {
								replStateHandler.abort();
							}
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					if (prevProc[0]!=null) {
						prevProc[0].invoke(state, pText1, pText2);
					}
				}
			};
		}

		try {
			//register our signal handler and store the previous one if there is any;
			//the signal handler is thread specific so we do not get race conditions
			//between setting the new handler and restoring the old one
			
			//AccessController call required to prevent SecurityException when running in XPages
			prevProc[0] = AccessController.doPrivileged(new PrivilegedExceptionAction<NotesCallbacks.OSSIGREPLPROC>() {

				@Override
				public NotesCallbacks.OSSIGREPLPROC run() throws Exception {
					return (NotesCallbacks.OSSIGREPLPROC) NotesNativeAPI.get().OSSetSignalHandler(NotesConstants.OS_SIGNAL_REPL, replProc);
				}
			});
			T result = callable.call();
			return result;
		}
		finally {
			//restore original signal handler if we are still the active signal handler (should always be the case,
			//since the signal handlers are thread specific)
			
			//AccessController call required to prevent SecurityException when running in XPages
			NotesCallbacks.OSSIGREPLPROC currProc = AccessController.doPrivileged(new PrivilegedExceptionAction<NotesCallbacks.OSSIGREPLPROC>() {

				@Override
				public NotesCallbacks.OSSIGREPLPROC run() throws Exception {
					return (NotesCallbacks.OSSIGREPLPROC) NotesNativeAPI.get().OSGetSignalHandler(NotesConstants.OS_SIGNAL_REPL);
				}
			});
			if (replProc.equals(currProc)) {
				NotesNativeAPI.get().OSSetSignalHandler(NotesConstants.OS_SIGNAL_REPL, prevProc[0]);
			}
		}
		
	}
	
	public static interface IReplicationStateListener {
		
		/**
		 * Indicating the connection is done.
		 */
		public void idle();
		
		/**
		 * Display that it is trying to select a server.
		 */
		public void pickServer();
		
		/**
		 *  Starting the connection.
		 *  
		 * @param server remove server
		 * @param port port
		 */
		public void connecting(String server, String port);

		/**
		 * Searching for matching replica on the server
		 * 
		 * @param server remove server
		 * @param port port
		 */
		public void searching(String server, String port);
		
		/**
		 * A "push" replication.
		 * 
		 * @param serverFile filepath on server
		 * @param localFile local filepath
		 */
		public void sending(String serverFile, String localFile);
		
		/**
		 * A "pull" replication.
		 * 
		 * @param serverFile filepath on server
		 * @param localFile local filepath
		 */
		public void receiving(String serverFile, String localFile);
		
		/**
		 * Replicator is in the searching phase.
		 * 
		 * @param srcFile source db filepath
		 */
		public void searchingDocs(String srcFile);
		
		/**
		 * Signal the file is done.
		 * 
		 * @param localFile local filepath
		 * @param replFileStats stats
		 */
		public void doneFile(String localFile, String replFileStats);
		
		/**
		 * Signal found a redirect.
		 * 
		 * @param serverFile server filepath
		 * @param localFile local filepath
		 */
		public void redirect(String serverFile, String localFile);
		
		/**
		 *  Signal view is building.
		 */
		public void buildView();
		
		/**
		 * Replication aborted
		 */
		public void abort();
	}
}
