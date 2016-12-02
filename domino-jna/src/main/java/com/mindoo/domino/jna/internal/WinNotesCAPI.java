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

}
