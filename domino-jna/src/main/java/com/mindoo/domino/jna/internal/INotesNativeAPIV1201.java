package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.HANDLE;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;

public interface INotesNativeAPIV1201 extends Library {

	  @UndocumentedAPI
	  short NABLookupBasicAuthentication(Memory userName, Memory password, int dwFlags,
	      int nMaxFullNameLen,
	      Memory fullUserName);

	  @UndocumentedAPI
	  short NSFProcessResultsExt(HANDLE.ByValue hDb,
	      Memory resultsname,
	      int dwFlags,
	      int hInResults,
	      int hOutFields,
	      int hFieldRules,
	      int hCombineRules,
	      DHANDLE.ByValue hReaders,
	      int dwHoursTillExpire,
	      IntByReference phErrorText,
	      DHANDLE.ByReference phStreamedhQueue,
	      ShortByReference phViewOpened,
	      IntByReference pViewNoteID,
	      int dwQRPTimeLimit, 
	      int dwQRPEntriesLimit,  
	      int dwQRPTimeCheckInterval);  

}
