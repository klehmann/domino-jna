package com.mindoo.domino.jna.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesCAPI.OSSIGBREAKPROC;
import com.mindoo.domino.jna.internal.NotesCAPI.OSSIGPROGRESSPROC;
import com.mindoo.domino.jna.internal.NotesCAPI.OSSIGREPLPROC;
import com.sun.jna.Pointer;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.internal.WinNotesCAPI;

/**
 * Utility class that uses signal handlers of Domino to stop and get progress information
 * for long running operations.
 * 
 * @author Karsten Lehmann
 */
public class SignalHandlerUtil {
	private static final ConcurrentHashMap<Long, IBreakHandler> m_breakHandlerByThread = new ConcurrentHashMap<Long, IBreakHandler>();

	private static volatile boolean m_breakHandlerInstalled = false;
	private static volatile NotesCAPI.OSSIGBREAKPROC prevBreakProc = null;
	private static final WinNotesCAPI.OSSIGBREAKPROC breakProcWin = new WinNotesCAPI.OSSIGBREAKPROCWin() {

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
	private static final NotesCAPI.OSSIGBREAKPROC breakProc = new WinNotesCAPI.OSSIGBREAKPROC() {

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
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		OSSIGBREAKPROC breakProc = (OSSIGBREAKPROC) notesAPI.OSGetSignalHandler(NotesCAPI.OS_SIGNAL_CHECK_BREAK);
		return breakProc != null;
	}
	
	public static synchronized void installGlobalBreakHandler() {
		if (!m_breakHandlerInstalled) {
			NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

			if (notesAPI instanceof WinNotesCAPI) {
				prevBreakProc = (OSSIGBREAKPROC) notesAPI.OSSetSignalHandler(NotesCAPI.OS_SIGNAL_CHECK_BREAK, breakProcWin);
			}
			else {
				prevBreakProc = (OSSIGBREAKPROC) notesAPI.OSSetSignalHandler(NotesCAPI.OS_SIGNAL_CHECK_BREAK, breakProc);
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
	public static <T> T old_runInterruptable(Callable<T> callable, final IBreakHandler breakHandler) throws Exception {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		OSSIGBREAKPROC breakProc;
		final Thread callThread = Thread.currentThread();

		//store a previously registered signal handler for this thread:
		final OSSIGBREAKPROC[] prevProc = new OSSIGBREAKPROC[1];

		if (notesAPI instanceof WinNotesCAPI) {
			breakProc = new WinNotesCAPI.OSSIGBREAKPROCWin() {

				@Override
				public short invoke() {
					//check if handler got called in the right thread
					if (Thread.currentThread()==callThread) {
						if (breakHandler.shouldInterrupt()) {
							//send break signal
							return INotesErrorConstants.ERR_CANCEL;
						}
						else {
							if (prevProc[0]!=null) {
								//ask previously registered break handler
								return prevProc[0].invoke();
							}
							else {
								return 0;
							}
						}
					}
					else {
						if (prevProc[0]!=null) {
							//ask previously registered break handler
							return prevProc[0].invoke();
						}
						else {
							return 0;
						}
					}
				}
			};
		}
		else {
			breakProc = new OSSIGBREAKPROC() {

				@Override
				public short invoke() {
					//check if handler got called in the right thread
					if (Thread.currentThread()==callThread) {
						if (breakHandler.shouldInterrupt()) {
							//send break signal
							return INotesErrorConstants.ERR_CANCEL;
						}
						else {
							if (prevProc[0]!=null) {
								//ask previously registered break handler
								return prevProc[0].invoke();
							}
							else {
								return 0;
							}
						}
					}
					else {
						if (prevProc[0]!=null) {
							//ask previously registered break handler
							return prevProc[0].invoke();
						}
						else {
							return 0;
						}
					}
				}
			};
		}

		try {
			//register our signal handler and store the previous one if there is any;
			//the signal handler is thread specific so we do not get race conditions
			//between setting the new handler and restoring the old one
			prevProc[0] = (OSSIGBREAKPROC) notesAPI.OSSetSignalHandler(NotesCAPI.OS_SIGNAL_CHECK_BREAK, breakProc);
			T result = callable.call();
			return result;
		}
		finally {
			//restore original signal handler if we are still the active signal handler (should always be the case,
			//since the signal handlers are thread specific)
			OSSIGBREAKPROC currProc = (OSSIGBREAKPROC) notesAPI.OSGetSignalHandler(NotesCAPI.OS_SIGNAL_CHECK_BREAK);
			if (breakProc.equals(currProc)) {
				notesAPI.OSSetSignalHandler(NotesCAPI.OS_SIGNAL_CHECK_BREAK, prevProc[0]);
			}
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
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		OSSIGPROGRESSPROC progressProc;
		final Thread callThread = Thread.currentThread();

		//store a previously registered signal handler for this thread:
		final OSSIGPROGRESSPROC[] prevProc = new OSSIGPROGRESSPROC[1];

		if (notesAPI instanceof WinNotesCAPI) {
			progressProc = new WinNotesCAPI.OSSIGPROGRESSPROCWin() {

				@Override
				public short invoke(short option, Pointer data1, Pointer data2) {
					try {
						if (Thread.currentThread() == callThread) {
							if (option == NotesCAPI.PROGRESS_SIGNAL_BEGIN) {
								progressHandler.begin();
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_END) {
								progressHandler.end();
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_SETRANGE) {
								long range = Pointer.nativeValue(data1);
								progressHandler.setRange(range);
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_SETTEXT) {
								String str = NotesStringUtils.fromLMBCS(data1, -1);
								progressHandler.setText(str);
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_SETPOS) {
								long pos = Pointer.nativeValue(data1);
								progressHandler.setPos(pos);
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_DELTAPOS) {
								long deltapos = Pointer.nativeValue(data1);
								progressHandler.setDeltaPos(deltapos);
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_SETBYTERANGE) {
								long totalbytes = Pointer.nativeValue(data1);
								progressHandler.setByteRange(totalbytes);
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_SETBYTEPOS) {
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
			progressProc = new OSSIGPROGRESSPROC() {

				@Override
				public short invoke(short option, Pointer data1, Pointer data2) {
					try {
						if (Thread.currentThread() == callThread) {
							if (option == NotesCAPI.PROGRESS_SIGNAL_BEGIN) {
								progressHandler.begin();
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_END) {
								progressHandler.end();
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_SETRANGE) {
								long range = Pointer.nativeValue(data1);
								progressHandler.setRange(range);
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_SETTEXT) {
								String str = NotesStringUtils.fromLMBCS(data1, -1);
								progressHandler.setText(str);
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_SETPOS) {
								long pos = Pointer.nativeValue(data1);
								progressHandler.setPos(pos);
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_DELTAPOS) {
								long deltapos = Pointer.nativeValue(data1);
								progressHandler.setDeltaPos(deltapos);
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_SETBYTERANGE) {
								long totalbytes = Pointer.nativeValue(data1);
								progressHandler.setByteRange(totalbytes);
							}
							else if (option == NotesCAPI.PROGRESS_SIGNAL_SETBYTEPOS) {
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
			prevProc[0] = (OSSIGPROGRESSPROC) notesAPI.OSSetSignalHandler(NotesCAPI.OS_SIGNAL_PROGRESS, progressProc);
			T result = callable.call();
			return result;
		}
		finally {
			//restore original signal handler if we are still the active signal handler (should always be the case,
			//since the signal handlers are thread specific)
			OSSIGPROGRESSPROC currProc = (OSSIGPROGRESSPROC) notesAPI.OSGetSignalHandler(NotesCAPI.OS_SIGNAL_PROGRESS);
			if (progressProc.equals(currProc)) {
				notesAPI.OSSetSignalHandler(NotesCAPI.OS_SIGNAL_PROGRESS, prevProc[0]);
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
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		OSSIGREPLPROC replProc;
		final Thread callThread = Thread.currentThread();

		//store a previously registered signal handler for this thread:
		final OSSIGREPLPROC[] prevProc = new OSSIGREPLPROC[1];

		if (notesAPI instanceof WinNotesCAPI) {
			replProc = new WinNotesCAPI.OSSIGREPLPROCWin() {

				@Override
				public void invoke(short state, Pointer pText1, Pointer pText2) {
					try {
						if (Thread.currentThread() == callThread) {
							if (state == NotesCAPI.REPL_SIGNAL_IDLE) {
								replStateHandler.idle();
							}
							else if (state == NotesCAPI.REPL_SIGNAL_PICKSERVER) {
								replStateHandler.pickServer();
							}
							else if (state == NotesCAPI.REPL_SIGNAL_CONNECTING) {
								String server = NotesStringUtils.fromLMBCS(pText1, -1);
								String port = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.connecting(server, port);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_SEARCHING) {
								String server = NotesStringUtils.fromLMBCS(pText1, -1);
								String port = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.searching(server, port);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_SENDING) {
								String serverFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String localFile = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.sending(serverFile, localFile);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_RECEIVING) {
								String serverFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String localFile = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.receiving(serverFile, localFile);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_SEARCHINGDOCS) {
								String srcFile = NotesStringUtils.fromLMBCS(pText1, -1);
								replStateHandler.searchingDocs(srcFile);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_DONEFILE) {
								String localFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String replFileStats = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.doneFile(localFile, replFileStats);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_REDIRECT) {
								String serverFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String localFile = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.redirect(serverFile, localFile);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_BUILDVIEW) {
								replStateHandler.buildView();
							}
							else if (state == NotesCAPI.REPL_SIGNAL_ABORT) {
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
			replProc = new OSSIGREPLPROC() {

				@Override
				public void invoke(short state, Pointer pText1, Pointer pText2) {
					try {
						if (Thread.currentThread() == callThread) {
							if (state == NotesCAPI.REPL_SIGNAL_IDLE) {
								replStateHandler.idle();
							}
							else if (state == NotesCAPI.REPL_SIGNAL_PICKSERVER) {
								replStateHandler.pickServer();
							}
							else if (state == NotesCAPI.REPL_SIGNAL_CONNECTING) {
								String server = NotesStringUtils.fromLMBCS(pText1, -1);
								String port = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.connecting(server, port);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_SEARCHING) {
								String server = NotesStringUtils.fromLMBCS(pText1, -1);
								String port = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.searching(server, port);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_SENDING) {
								String serverFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String localFile = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.sending(serverFile, localFile);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_RECEIVING) {
								String serverFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String localFile = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.receiving(serverFile, localFile);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_SEARCHINGDOCS) {
								String srcFile = NotesStringUtils.fromLMBCS(pText1, -1);
								replStateHandler.searchingDocs(srcFile);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_DONEFILE) {
								String localFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String replFileStats = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.doneFile(localFile, replFileStats);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_REDIRECT) {
								String serverFile = NotesStringUtils.fromLMBCS(pText1, -1);
								String localFile = NotesStringUtils.fromLMBCS(pText2, -1);
								replStateHandler.redirect(serverFile, localFile);
							}
							else if (state == NotesCAPI.REPL_SIGNAL_BUILDVIEW) {
								replStateHandler.buildView();
							}
							else if (state == NotesCAPI.REPL_SIGNAL_ABORT) {
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
			prevProc[0] = (OSSIGREPLPROC) notesAPI.OSSetSignalHandler(NotesCAPI.OS_SIGNAL_REPL, replProc);
			T result = callable.call();
			return result;
		}
		finally {
			//restore original signal handler if we are still the active signal handler (should always be the case,
			//since the signal handlers are thread specific)
			OSSIGREPLPROC currProc = (OSSIGREPLPROC) notesAPI.OSGetSignalHandler(NotesCAPI.OS_SIGNAL_REPL);
			if (replProc.equals(currProc)) {
				notesAPI.OSSetSignalHandler(NotesCAPI.OS_SIGNAL_REPL, prevProc[0]);
			}
		}
		
	}
	
	public static interface IReplicationStateListener {
		
		public void idle();
		
		public void pickServer();
		
		public void connecting(String server, String port);
		
		public void searching(String server, String port);
		
		public void sending(String serverFile, String localFile);
		
		public void receiving(String serverFile, String localFile);
		
		public void searchingDocs(String srcFile);
		
		public void doneFile(String localFile, String replFileStats);
		
		public void redirect(String serverFile, String localFile);
		
		public void buildView();
		
		public void abort();
	}
}
