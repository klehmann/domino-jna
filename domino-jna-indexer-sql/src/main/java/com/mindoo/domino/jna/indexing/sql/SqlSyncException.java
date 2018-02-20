package com.mindoo.domino.jna.indexing.sql;

public class SqlSyncException extends RuntimeException {
	private static final long serialVersionUID = 5571539554515577632L;

    public SqlSyncException() {
        super();
    }

    public SqlSyncException(String message) {
        super(message);
    }

    public SqlSyncException(String message, Throwable cause) {
        super(message, cause);
    }

    public SqlSyncException(Throwable cause) {
        super(cause);
    }


}
