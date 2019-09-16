package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.internal.structs.NIFFindByKeyContextStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.sun.jna.Callback;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * Callback interface used on non-Windows platforms
 * 
 * @author Karsten Lehmann
 */
public interface NotesCallbacks {

	/**
	 * Callback used by EnumCompositeBuffer
	 */
	interface ActionRoutinePtr extends Callback {
		short invoke(Pointer dataPtr, short signature, int dataLength, Pointer vContext);
	}

	/**
	 * Callback used by NSFDbGetNotes
	 */
	interface NSFGetNotesCallback extends Callback {
		short invoke(Pointer param, int totalSizeLow, int totalSizeHigh);
	}

	/**
	 * Callback used by NSFDbGetNotes
	 */
	interface NSFFolderAddCallback extends Callback {
		short invoke(Pointer param, NotesUniversalNoteIdStruct noteUNID, int opBlock, int opBlockSize);
	}

	/**
	 * Callback used by IDEnumerate
	 */
	interface IdEnumerateProc extends Callback {
		short invoke(Pointer parameter, int noteId);
	}

	/**
	 * Callback used by NSFNoteCipherExtractWithCallback
	 */
	interface NoteExtractCallback extends Callback {
		short invoke(Pointer data, int length, Pointer param);
	}

	/**
	 * Callback used by NSFRecoverDatabases
	 */
	interface LogRestoreCallbackFunction extends Callback {
		short invoke(NotesUniversalNoteIdStruct logID, int logNumber, Memory logSegmentPathName);
	}

	/**
	 * Callback used by NIFFindByKeyExtended3
	 */
	interface NIFFindByKeyProc extends Callback {
		short invoke(NIFFindByKeyContextStruct ctx);
	}

	/**
	 * Callback used by MQScan
	 */
	interface MQScanCallback extends Callback {
		short invoke(Pointer pBuffer, short length, short priority, Pointer ctx);
	}

	/**
	 * Base callback used by OSGetSignalHandler / OSSetSignalHandler
	 */
	interface OSSIGPROC extends Callback {
	}

	/**
	 * Callback used by message signal handlers
	 */
	interface OSSIGMSGPROC extends OSSIGPROC {
		public short invoke(Pointer message, short type);
	}

	/**
	 * Callback used by busy signal handlers
	 */
	interface OSSIGBUSYPROC extends OSSIGPROC {
		public short invoke(short busytype);
	}

	/**
	 * Callback used by break signal handlers
	 */
	interface OSSIGBREAKPROC extends OSSIGPROC {
		short invoke();
	}

	/**
	 * Callback used to abort design refresh
	 */
	interface ABORTCHECKPROC extends Callback {
		short invoke();
	}
	
	/**
	 * Callback used by progress signal handlers
	 */
	interface OSSIGPROGRESSPROC extends OSSIGPROC {
		public short invoke(short option, Pointer data1, Pointer data2);
	}

	/**
	 * Callback used by replication signal handlers
	 */
	interface OSSIGREPLPROC extends OSSIGPROC {
		public void invoke(short state, Pointer pText1, Pointer pText2);
	}

	/**
	 * Callback used by NSFDbGetNotes
	 */
	interface b32_NSFNoteOpenCallback extends Callback {
		short invoke(Pointer param, int hNote, int noteId, short status);
	}

	/**
	 * Callback used by NSFDbGetNotes
	 */
	interface b32_NSFObjectAllocCallback extends Callback {
		short invoke(Pointer param, int hNote, int oldRRV, short status, int objectSize);
	}

	/**
	 * Callback used by NSFDbGetNotes
	 */
	interface b32_NSFObjectWriteCallback extends Callback {
		short invoke(Pointer param, int hNote, int oldRRV, short status, Pointer buffer, int bufferSize);
	}

	/**
	 * Callback used by NSFNoteComputeWithForm
	 */
	interface b32_CWFErrorProc extends Callback {
		short invoke(Pointer pCDField, short phase, short error, int hErrorText, short wErrorTextSize, Pointer ctx);
	}

	interface b32_NSFGetAllFolderChangesCallback extends Callback {
		short invoke(Pointer param, NotesUniversalNoteIdStruct noteUnid, int hAddedNoteTable, int removedNoteTable);
	}

	/**
	 * Callback used by NSFDbGetNotes
	 */
	interface b64_NSFNoteOpenCallback extends Callback {
		short invoke(Pointer param, long hNote, int noteId, short status);
	}

	/**
	 * Callback used by NSFDbGetNotes
	 */
	interface b64_NSFObjectAllocCallback extends Callback {
		short invoke(Pointer param, long hNote, int oldRRV, short status, int objectSize);
	}

	/**
	 * Callback used by NSFDbGetNotes
	 */
	interface b64_NSFObjectWriteCallback extends Callback {
		short invoke(Pointer param, long hNote, int oldRRV, short status, Pointer buffer, int bufferSize);
	}

	/**
	 * Callback used by NSFSearchExtended3
	 */
	interface NsfSearchProc extends Callback {
		short invoke(Pointer enumRoutineParameter, Pointer searchMatch,
				Pointer summaryBuffer);
	}

	/**
	 * Callback used by NSFNoteComputeWithForm
	 */
	interface b64_CWFErrorProc extends Callback {
		short invoke(Pointer pCDField, short phase, short error, long hErrorText, short wErrorTextSize, Pointer ctx);
	}
	
	interface LSCompilerErrorProc extends Callback {
		short invoke(Pointer pInfo, Pointer pCtx);
	}

	interface b64_NSFGetAllFolderChangesCallback extends Callback {
		short invoke(Pointer param, NotesUniversalNoteIdStruct noteUnid, long hAddedNoteTable, long removedNoteTable);
	}

	interface STATTRAVERSEPROC extends Callback {
		short invoke(Pointer ctx, Pointer facility, Pointer statName, short valueType, Pointer value);
	}
	
	interface ACLENTRYENUMFUNC extends Callback {
		void invoke(Pointer enumFuncParam, Pointer name, short accessLevel, Pointer privileges, short accessFlag);
	}
}
