package com.mindoo.domino.jna.internal;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

public interface INotesNativeAPI64V1000 extends Library {

	@UndocumentedAPI
	public short NSFQueryDB(long hDb, Memory query, int flags,
			int maxDocsScanned, int maxEntriesScanned, int maxMsecs,
			LongByReference retResults, IntByReference retError, IntByReference retExplain);
	
	@UndocumentedAPI
	public short NSFGetSoftDeletedViewFilter(long hViewDB, long hDataDB, int viewNoteID, IntByReference hFilter);

	@UndocumentedAPI
	public short NSFDbLargeSummaryEnabled(long hDB);

	@UndocumentedAPI
	public short NSFDesignHarvest (long hDB, int flags);

}
