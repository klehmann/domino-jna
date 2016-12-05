package com.mindoo.domino.jna.transactions;

/**
 * This exception is thrown by {@link Transactions#runInDbTransaction(com.mindoo.domino.jna.NotesDatabase, ITransactionCallable)}
 * when the transaction has been rolled back.
 * 
 * @author Karsten Lehmann
 */
public class RollbackException extends RuntimeException {
	private static final long serialVersionUID = -8053240674051185168L;

	public RollbackException() {
		super();
	}

	public RollbackException(String message) {
		super(message);
	}

	public RollbackException(String message, Throwable cause) {
		super(message, cause);
	}

	public RollbackException(Throwable cause) {
		super(cause);
	}

}
