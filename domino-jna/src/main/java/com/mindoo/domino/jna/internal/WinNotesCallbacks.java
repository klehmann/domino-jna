package com.mindoo.domino.jna.internal;

import com.sun.jna.win32.StdCallLibrary.StdCallCallback;

/**
 * Callback interface used on Windows platforms. Implements 
 * {@link StdCallCallback} so that the right method calling conventions are used.
 * 
 * @author Karsten Lehmann
 */
public interface WinNotesCallbacks {

	interface NoteExtractCallbackWin extends NotesCallbacks.NoteExtractCallback, StdCallCallback {}

	interface IdEnumerateProcWin extends NotesCallbacks.IdEnumerateProc, StdCallCallback {}

	interface MQScanCallbackWin extends NotesCallbacks.MQScanCallback, StdCallCallback {}

	interface NSFGetNotesCallbackWin extends NotesCallbacks.NSFGetNotesCallback, StdCallCallback {}

	interface NSFFolderAddCallbackWin extends NotesCallbacks.NSFFolderAddCallback, StdCallCallback {}

	interface LogRestoreCallbackFunctionWin extends NotesCallbacks.LogRestoreCallbackFunction, StdCallCallback {}

	interface NIFFindByKeyProcWin extends NotesCallbacks.NIFFindByKeyProc, StdCallCallback {}

	interface OSSIGPROCWin extends NotesCallbacks.OSSIGPROC, StdCallCallback {}

	interface OSSIGMSGPROCWin extends NotesCallbacks.OSSIGMSGPROC, StdCallCallback {}

	interface OSSIGBUSYPROCWin extends NotesCallbacks.OSSIGBUSYPROC, StdCallCallback {}

	interface OSSIGBREAKPROCWin extends NotesCallbacks.OSSIGBREAKPROC, StdCallCallback {}

	interface ABORTCHECKPROCWin extends NotesCallbacks.ABORTCHECKPROC {}
	
	interface OSSIGPROGRESSPROCWin extends NotesCallbacks.OSSIGPROGRESSPROC, StdCallCallback {}

	interface OSSIGREPLPROCWin extends NotesCallbacks.OSSIGREPLPROC, StdCallCallback {}

	interface ActionRoutinePtrWin extends NotesCallbacks.ActionRoutinePtr {}

	interface b32_NsfSearchProcWin extends NotesCallbacks.b32_NsfSearchProc, StdCallCallback {}

	interface b32_CWFErrorProcWin extends NotesCallbacks.b32_CWFErrorProc, StdCallCallback {}

	interface b32_NSFNoteOpenCallbackWin extends NotesCallbacks.b32_NSFNoteOpenCallback, StdCallCallback {}

	interface b32_NSFObjectAllocCallbackWin extends NotesCallbacks.b32_NSFObjectAllocCallback, StdCallCallback {}

	interface b32_NSFObjectWriteCallbackWin extends NotesCallbacks.b32_NSFObjectWriteCallback, StdCallCallback {}

	interface b64_NsfSearchProcWin extends NotesCallbacks.b64_NsfSearchProc, StdCallCallback {}

	interface b64_CWFErrorProcWin extends NotesCallbacks.b64_CWFErrorProc, StdCallCallback {}

	interface b64_NSFNoteOpenCallbackWin extends NotesCallbacks.b64_NSFNoteOpenCallback, StdCallCallback {}

	interface b64_NSFObjectAllocCallbackWin extends NotesCallbacks.b64_NSFObjectAllocCallback, StdCallCallback {}

	interface b64_NSFObjectWriteCallbackWin extends NotesCallbacks.b64_NSFObjectWriteCallback, StdCallCallback {}

}
