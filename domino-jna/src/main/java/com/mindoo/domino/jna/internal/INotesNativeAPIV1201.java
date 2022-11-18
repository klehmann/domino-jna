package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.HANDLE;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
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

	  @UndocumentedAPI
	  short ListAllocate2Ext (short ListEntries,
			  int TextSize,
			  boolean fPrefixDataType,
			  IntByReference rethList,
			  PointerByReference retpList,
			  IntByReference retListSize,
			  boolean bAllowLarge);

	  @UndocumentedAPI
	  short ListAddEntry2Ext(int mhList,
			  boolean fPrefixDataType,
			  IntByReference pListSize,
			  short EntryNumber,
			  Memory Text,
			  char TextSize,
			  boolean bAllowLarge);

	  @UndocumentedAPI
	  /* to issue  design command */
	  short NSFDesignCommand(HANDLE.ByValue hDb, int cmd, int dwFlags, 
			  Memory pObjectName, IntByReference phReturnVal, IntByReference phErrorText, int hDsgnCmd);

	  @UndocumentedAPI
	  /* to prep for design command - add fields for create index */
	  short NSFDesignCommandAddComponent(Memory name, int attr,
			  IntByReference phDsgnCmd);

}
