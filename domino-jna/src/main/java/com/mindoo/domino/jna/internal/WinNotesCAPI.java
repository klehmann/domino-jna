package com.mindoo.domino.jna.internal;

import com.sun.jna.win32.StdCallLibrary;

/**
 * Extension of {@link NotesCAPI} implementing {@link StdCallLibrary} required
 * on Windows platforms
 * 
 * @author Karsten Lehmann
 */
public interface WinNotesCAPI extends NotesCAPI, StdCallLibrary {

	public interface b32_NsfSearchProcWin extends b32_NsfSearchProc, StdCallCallback { /* StdCallCallback if using __stdcall__ */
    }
	
	public interface b64_NsfSearchProcWin extends b64_NsfSearchProc, StdCallCallback { /* StdCallCallback if using __stdcall__ */
    }

	public interface NoteExtractCallbackWin extends NoteExtractCallback, StdCallCallback { /* StdCallCallback if using __stdcall__ */
    }

	public interface NoteNsfItemScanProcWin extends NoteNsfItemScanProc, StdCallCallback { /* StdCallCallback if using __stdcall__ */
    }

	public interface b64_CWFErrorProcWin extends b64_CWFErrorProc, StdCallCallback { /* StdCallCallback if using __stdcall__ */
    }

	public interface b32_CWFErrorProcWin extends b32_CWFErrorProc, StdCallCallback { /* StdCallCallback if using __stdcall__ */
    }

	public interface IdEnumerateProcWin extends IdEnumerateProc, StdCallCallback { /* StdCallCallback if using __stdcall__ */
	};

	public interface MQScanCallbackWin extends MQScanCallback, StdCallCallback { /* StdCallCallback if using __stdcall__ */
    }

	//callbacks for NSFDbGetNotes
	
	public interface NSFGetNotesCallbackWin extends NSFGetNotesCallback, StdCallCallback {
    }

	public interface b64_NSFNoteOpenCallbackWin extends b64_NSFNoteOpenCallback, StdCallCallback {
    }

	public interface b32_NSFNoteOpenCallbackWin extends b32_NSFNoteOpenCallback, StdCallCallback {
    }

	public interface b64_NSFObjectAllocCallbackWin extends b64_NSFObjectAllocCallback, StdCallCallback {
    }

	public interface b32_NSFObjectAllocCallbackWin extends b32_NSFObjectAllocCallback, StdCallCallback {
    }

	public interface b64_NSFObjectWriteCallbackWin extends b64_NSFObjectWriteCallback, StdCallCallback {
    }

	public interface b32_NSFObjectWriteCallbackWin extends b32_NSFObjectWriteCallback, StdCallCallback {
    }

	public interface NSFFolderAddCallbackWin extends NSFFolderAddCallback, StdCallCallback {
    }
	
	public interface LogRestoreCallbackFunctionWin extends LogRestoreCallbackFunction, StdCallCallback {
	}

}
