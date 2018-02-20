package com.mindoo.domino.jna.indexing.sqlite;

import javax.sql.DataSource;

import org.sqlite.SQLiteDataSource;

import com.mindoo.domino.jna.indexing.sql.AbstractSQLSyncTarget;

/**
 * Abstract sync target class to sync Domino data into a Sqlite database
 * 
 * @author Karsten Lehmann
 */
public abstract class AbstractSQLiteSyncTarget extends AbstractSQLSyncTarget {
	
	public AbstractSQLiteSyncTarget(String jdbcUrl) {
		super(jdbcUrl);
	}

	@Override
	protected DataSource createDataSource(String jdbcUrl) {
		SQLiteDataSource ds = new SQLiteDataSource();
		ds.setUrl(jdbcUrl);
		return ds;
	}

}
