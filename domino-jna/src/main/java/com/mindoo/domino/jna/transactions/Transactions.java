package com.mindoo.domino.jna.transactions;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;

/**
 * Utility class that gives actions to NSF transactions. NSF transactions cover
 * normal document operations like add/update/delete, but in R9 they have some
 * caveats like folder operations (e.g. moving docs between folders) that may not
 * be covered properly according to IBM dev. Transactions currently only work
 * in local databases.<br>
 * <br>
 * The concept behind NSF transactions is called nested top actions. In short, you can
 * open nested transactions in your running transaction, which can be committed, although
 * the surrounding transaction may be rolled back.<br>
 * <br>
 * Here is a short description of the concept:<br>
 * <a href="http://www.cse.iitb.ac.in/infolab/Data/Courses/CS632/1999/aries/node23.html">Nested Top Actions</a>.
 * <br>
 * Running code via {@link #runInDbTransaction(NotesDatabase, ITransactionCallable)}
 * does not automatically lock out all other threads.
 * Instead, only an Intend Shared lock is allocated, all users
 * can still read and write. Blocking other threads from writing starts when the
 * first write operations is being made (e.g. via NSFNoteUpdate). In addition, you
 * should not take too much time for your transaction operation, because it gets
 * aborted after a certain amount of time and when many other threads are waiting in the queue.
 * 
 * @author Karsten Lehmann
 */
public class Transactions {
	private static final ThreadLocal<Integer> activeTransactionDepthForCurrentThread = new ThreadLocal<Integer>() {
		protected Integer initialValue() {
			return 0;
		};
	};
	private static Boolean transactionsAvailable;
	
	/**
	 * Method to check whether transactions are available for the specified database.
	 * 
	 * @param anyLocalDb any database 
	 * @return true if available
	 */
	public static boolean areTransactionsSupported(NotesDatabase anyLocalDb) {
		if (transactionsAvailable==null) {
			NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

			if (NotesJNAContext.is64Bit()) {
				short result = notesAPI.b64_NSFTransactionBegin(anyLocalDb.getHandle64(), NotesCAPI.NSF_TRANSACTION_BEGIN_SUB_COMMIT);
				if (result==0) {
					transactionsAvailable = true;
					notesAPI.b64_NSFTransactionRollback(anyLocalDb.getHandle64());
				}
				else {
					transactionsAvailable = false;
				}
			}
			else {
				short result = notesAPI.b32_NSFTransactionBegin(anyLocalDb.getHandle32(), NotesCAPI.NSF_TRANSACTION_BEGIN_SUB_COMMIT);
				if (result==0) {
					transactionsAvailable = true;
					notesAPI.b32_NSFTransactionRollback(anyLocalDb.getHandle32());
				}
				else {
					transactionsAvailable = false;
				}
			}
		}
		return transactionsAvailable.booleanValue();
	}
	
	/**
	 * Method to check if the current thread is running in a transaction ({@link #getTransactionLevelForCurrentThread()}
	 * is 1 or higher).
	 * 
	 * @return true if in transaction
	 */
	public static boolean isTransactionActiveForCurrentThread() {
		return activeTransactionDepthForCurrentThread.get() > 0;
	}
	
	/**
	 * Method to read the current transaction depth. If the returned value is 0, the code is not running
	 * in a transaction, for 1 we are running in a main transaction, for higher values the code is
	 * running in a nested transaction.
	 * 
	 * @return level
	 */
	public static int getTransactionLevelForCurrentThread() {
		return activeTransactionDepthForCurrentThread.get();
	}
	
	/**
	 * Executes a {@link ITransactionCallable} atomically in a database. If
	 * {@link #getTransactionLevelForCurrentThread()} is 0, the method is
	 * waiting for an exclusive lock on the database, for 1 or higher it
	 * opens a sub transaction. The transaction is automatically committed
	 * after execution of the {@link ITransactionCallable} if no exception
	 * is thrown.
	 * 
	 * @param db database
	 * @param callable callable
	 * @return optional return value
	 */
	public static <T> T runInDbTransaction(NotesDatabase db, ITransactionCallable<T> callable) throws RollbackException {
		if (db.isRecycled()) {
			throw new NotesError(0, "Database is already recycled");
		}
		
		int transactionLevel = activeTransactionDepthForCurrentThread.get().intValue();

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();

		int flags;
		if (transactionLevel==0) {
			flags = NotesCAPI.NSF_TRANSACTION_BEGIN_LOCK_DB;
		}
		else {
			flags = NotesCAPI.NSF_TRANSACTION_BEGIN_SUB_COMMIT;
		}
		
		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFTransactionBegin(db.getHandle64(), flags);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFTransactionBegin(db.getHandle32(), flags);
			NotesErrorUtils.checkResult(result);
		}
		
		activeTransactionDepthForCurrentThread.set(transactionLevel+1);
		try {
			T retValue = callable.runInDbTransaction(db);
			
			if (NotesJNAContext.is64Bit()) {
				short result = notesAPI.b64_NSFTransactionCommit(db.getHandle64(), 0);
				NotesErrorUtils.checkResult(result);
			}
			else {
				short result = notesAPI.b64_NSFTransactionCommit(db.getHandle32(), 0);
				NotesErrorUtils.checkResult(result);
			}
			return retValue;
		}
		catch (Throwable t) {
			if (NotesJNAContext.is64Bit()) {
				short result = notesAPI.b64_NSFTransactionRollback(db.getHandle64());
				NotesErrorUtils.checkResult(result);
			}
			else {
				short result = notesAPI.b64_NSFTransactionRollback(db.getHandle32());
				NotesErrorUtils.checkResult(result);
			}
			if (t instanceof RollbackException) {
				throw (RollbackException) t;
			}
			else {
				throw new RollbackException(t);
			}
		}
		finally {
			activeTransactionDepthForCurrentThread.set(transactionLevel);
		}
	}
}
