package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.HANDLE;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public interface INotesNativeAPIV1200 extends Library {

	  @UndocumentedAPI
		public short NSFProcessResults(HANDLE.ByValue hDb,
				Memory viewname,
				int dwFlags,
				int hInResults,
				int hOutFields,
				int hFieldRules,
				int hCombineRules,
				IntByReference hErrorText,
				DHANDLE.ByReference phStreamedhQueue);  

	  @UndocumentedAPI
		short NSFQueryAddToResultsList(int /* QUEP_LISTTYPE */ type,
				Pointer pInEntry, Pointer phEntryList,
				IntByReference phErrorText);

}
