package com.mindoo.domino.jna.internal;

import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;

public interface INotesNativeAPI32V1000 {

	@UndocumentedAPI
	public short NSFQueryDB(int hDb, Memory query, int flags,
			int maxDocsScanned, int maxEntriesScanned, int maxMsecs,
			IntByReference retResults, IntByReference retError, IntByReference retExplain);

	@UndocumentedAPI
	public short NSFGetSoftDeletedViewFilter(int hViewDB, int hDataDB, int viewNoteID, IntByReference hFilter);

}
