package com.mindoo.domino.jna.transactions;

import com.mindoo.domino.jna.NotesDatabase;

/**
 * Callable interface for code running in a database transaction
 * 
 * @author Karsten Lehmann
 *
 * @param <T> result type
 */
public interface ITransactionCallable<T> {

	/**
	 * Add your code here that should run atomically.
	 * 
	 * @param db database
	 * @return computation result or null
	 * @throws Exception
	 */
	public T runInDbTransaction(NotesDatabase db) throws Exception;
}
