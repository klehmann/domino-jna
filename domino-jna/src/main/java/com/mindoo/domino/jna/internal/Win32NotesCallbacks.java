package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.internal.NotesCallbacks.NSFFORMCMDSPROC;
import com.mindoo.domino.jna.internal.NotesCallbacks.NSFFORMFUNCPROC;
import com.mindoo.domino.jna.internal.structs.NIFFindByKeyContextStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary.StdCallCallback;

/**
 * Callback interface used on Win32 platforms. Implements 
 * {@link StdCallCallback} so that the right method calling conventions are used.
 * 
 * @author Karsten Lehmann
 */
public interface Win32NotesCallbacks {

	interface NoteExtractCallbackWin32 extends NotesCallbacks.NoteExtractCallback, StdCallCallback {
		@Override
		short invoke(Pointer data, int length, Pointer param);
	}

	interface IdEnumerateProcWin32 extends NotesCallbacks.IdEnumerateProc, StdCallCallback {
		@Override
		short invoke(Pointer parameter, int noteId);
	}

	interface MQScanCallbackWin32 extends NotesCallbacks.MQScanCallback, StdCallCallback {
		@Override
		short invoke(Pointer pBuffer, short length, short priority, Pointer ctx);
	}

	interface NSFGetNotesCallbackWin32 extends NotesCallbacks.NSFGetNotesCallback, StdCallCallback {
		@Override
		short invoke(Pointer param, int totalSizeLow, int totalSizeHigh);
	}

	interface NSFFolderAddCallbackWin32 extends NotesCallbacks.NSFFolderAddCallback, StdCallCallback {
		@Override
		short invoke(Pointer param, NotesUniversalNoteIdStruct noteUNID, int opBlock, int opBlockSize);
	}

	interface LogRestoreCallbackFunctionWin32 extends NotesCallbacks.LogRestoreCallbackFunction, StdCallCallback {
		@Override
		short invoke(NotesUniversalNoteIdStruct logID, int logNumber, Memory logSegmentPathName);
	}

	interface NIFFindByKeyProcWin32 extends NotesCallbacks.NIFFindByKeyProc, StdCallCallback {
		@Override
		short invoke(NIFFindByKeyContextStruct ctx);
	}

	interface OSSIGMSGPROCWin32 extends NotesCallbacks.OSSIGMSGPROC, StdCallCallback {
		@Override
		short invoke(Pointer message, short type);
	}

	interface OSSIGBUSYPROCWin32 extends NotesCallbacks.OSSIGBUSYPROC, StdCallCallback {
		@Override
		short invoke(short busytype);
	}

	interface OSSIGBREAKPROCWin32 extends NotesCallbacks.OSSIGBREAKPROC, StdCallCallback {
		@Override
		short invoke();
	}

	interface ABORTCHECKPROCWin32 extends NotesCallbacks.ABORTCHECKPROC, StdCallCallback {
		@Override
		short invoke();
	}
	
	interface OSSIGPROGRESSPROCWin32 extends NotesCallbacks.OSSIGPROGRESSPROC, StdCallCallback {
		@Override
		short invoke(short option, Pointer data1, Pointer data2);
	}

	interface OSSIGREPLPROCWin32 extends NotesCallbacks.OSSIGREPLPROC, StdCallCallback {
		@Override
		void invoke(short state, Pointer pText1, Pointer pText2);
	}

	interface ActionRoutinePtrWin32 extends NotesCallbacks.ActionRoutinePtr, StdCallCallback {
		@Override
		short invoke(Pointer dataPtr, short signature, int dataLength, Pointer vContext);
	}

	interface NsfSearchProcWin32 extends NotesCallbacks.NsfSearchProc, StdCallCallback {
		@Override
		short invoke(Pointer enumRoutineParameter, Pointer searchMatch, Pointer summaryBuffer);
	}

	interface CWFErrorProcWin32 extends NotesCallbacks.b32_CWFErrorProc, StdCallCallback {
		@Override
		short invoke(Pointer pCDField, short phase, short error, int hErrorText, short wErrorTextSize,
				Pointer ctx);
	}

	interface NSFNoteOpenCallbackWin32 extends NotesCallbacks.b32_NSFNoteOpenCallback, StdCallCallback {
		@Override
		short invoke(Pointer param, int hNote, int noteId, short status);
	}

	interface NSFObjectAllocCallbackWin32 extends NotesCallbacks.b32_NSFObjectAllocCallback, StdCallCallback {
		@Override
		short invoke(Pointer param, int hNote, int oldRRV, short status, int objectSize);
	}

	interface NSFObjectWriteCallbackWin32 extends NotesCallbacks.b32_NSFObjectWriteCallback, StdCallCallback {
		@Override
		short invoke(Pointer param, int hNote, int oldRRV, short status, Pointer buffer, int bufferSize);
	}

	interface LSCompilerErrorProcWin32 extends NotesCallbacks.LSCompilerErrorProc, StdCallCallback {
		@Override
		short invoke(Pointer pInfo, Pointer pCtx);
	}
	
	interface STATTRAVERSEPROCWin32 extends NotesCallbacks.STATTRAVERSEPROC, StdCallCallback {
		@Override
		short invoke(Pointer ctx, Pointer facility, Pointer statName, short valueType, Pointer value);
	};

	interface ACLENTRYENUMFUNCWin32 extends NotesCallbacks.ACLENTRYENUMFUNC, StdCallCallback {
		@Override
		void invoke(Pointer enumFuncParam, Pointer name, short accessLevel, Pointer privileges,
				short accessFlag);
	};
	
	interface XML_READ_FUNCTIONWin32 extends NotesCallbacks.XML_READ_FUNCTION, StdCallCallback {
		@Override
		int invoke(Pointer pBuffer, int length, Pointer pAction);
	};
	
	interface XML_WRITE_FUNCTIONWin32 extends NotesCallbacks.XML_WRITE_FUNCTION, StdCallCallback {
		@Override
		void invoke(Pointer bBuffer, int length, Pointer pAction);
	};
	
	interface NSFPROFILEENUMPROCWin32 extends NotesCallbacks.b32_NSFPROFILEENUMPROC, StdCallCallback {
		@Override
		short invoke(int hDB, Pointer ctx, Pointer profileName, short profileNameLength, Pointer username,
				short usernameLength, int noteId);
	};

	interface NSFDbNamedObjectEnumPROCWin32 extends NotesCallbacks.b32_NSFDbNamedObjectEnumPROC,  StdCallCallback {
		@Override
		short invoke(int hDB, Pointer param, short nameSpace, Pointer name, short nameLength,
				IntByReference objectID, NotesTimeDateStruct entryTime);
	};

	interface FPMailNoteJitEx2CallBackWin32 extends NotesCallbacks.b32_FPMailNoteJitEx2CallBack, StdCallCallback {
		@Override
		short invoke(int hdl, Pointer ptr1, Pointer ptr2);
	};

	interface DESIGN_COLL_OPENCLOSE_PROCWin32 extends NotesCallbacks.DESIGN_COLL_OPENCLOSE_PROC, StdCallCallback {
		@Override
		short invoke(int dwFlags, Pointer phColl, Pointer ctx);
	};

	interface REGSIGNALPROCWin32 extends NotesCallbacks.REGSIGNALPROC, StdCallCallback {
		@Override
		void invoke(Pointer message);
	}

	interface ASYNCNOTIFYPROCWin32 extends NotesCallbacks.ASYNCNOTIFYPROC, StdCallCallback {
		@Override
		void invoke(Pointer vactx, Pointer pvReadCtx);
	}
	
	interface DESIGNENUMPROCWin32 extends NotesCallbacks.b32_DESIGNENUMPROC, StdCallCallback {
		@Override
		short invoke(Pointer routineParameter, int hDB, int NoteID, NotesUniversalNoteIdStruct NoteUNID,
				short NoteClass, Pointer summary, int designType);
	}

	interface NSFFORMFUNCPROCWin32 extends NSFFORMFUNCPROC, StdCallCallback {
		@Override
		short invoke(Pointer ptr);
	}
	
	interface NSFFORMCMDSPROCWin32 extends NSFFORMCMDSPROC, StdCallCallback {
		@Override
		short invoke(Pointer ptr, short code, IntByReference stopFlag);
	}

}
