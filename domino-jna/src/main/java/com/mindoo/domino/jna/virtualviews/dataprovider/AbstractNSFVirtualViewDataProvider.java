package com.mindoo.domino.jna.virtualviews.dataprovider;

import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.gc.NotesGC;

/**
 * Base class for data providers that fetch data from a Notes database
 */
public abstract class AbstractNSFVirtualViewDataProvider implements IVirtualViewDataProvider {
	protected String dbServer;
	protected String dbFilePath;
	
	public AbstractNSFVirtualViewDataProvider(String dbServer, String dbFilePath) {
		this.dbServer = dbServer;
		this.dbFilePath = dbFilePath;
	}

	public String getDbServer() {
		return dbServer;
	}
	
	public String getDbFilePath() {
		return dbFilePath;
	}
	
	public final NotesDatabase getDatabase() {
		//reuse the same db instance across multiple NSF data providers
		String cacheKey = "NSFVirtualViewProvider_"+dbServer+"!!"+dbFilePath;
		NotesDatabase db = (NotesDatabase) NotesGC.getCustomValue(cacheKey);
		if (db == null || db.isRecycled()) {
			db = new NotesDatabase(dbServer, dbFilePath, (String) null);
			NotesGC.setCustomValue(cacheKey, db);
		}
		return db;
	}
	
}
