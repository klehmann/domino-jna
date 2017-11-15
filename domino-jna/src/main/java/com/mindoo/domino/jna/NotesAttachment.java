package com.mindoo.domino.jna;

import java.util.Calendar;

import com.mindoo.domino.jna.NotesAttachment.IDataCallback.Action;
import com.mindoo.domino.jna.constants.Compression;
import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesCAPI.NoteExtractCallback;
import com.mindoo.domino.jna.structs.NotesBlockIdStruct;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.internal.WinNotesCAPI;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

/**
 * Data container to access metadata and binary data of a note attachment
 * 
 * @author Karsten Lehmann
 */
public class NotesAttachment {
	private String m_fileName;
	private Compression m_compression;
	private short m_fileFlags;
	private int m_fileSize;
	private NotesTimeDate m_fileCreated;
	private NotesTimeDate m_fileModified;
	private NotesNote m_parentNote;
	private NotesBlockIdStruct m_itemBlockId;
	private int m_rrv;
	
	public NotesAttachment(String fileName, Compression compression, short fileFlags, int fileSize,
			NotesTimeDate fileCreated, NotesTimeDate fileModified, NotesNote parentNote,
			NotesBlockIdStruct itemBlockId, int rrv) {
		m_fileName = fileName;
		m_compression = compression;
		m_fileFlags = fileFlags;
		m_fileSize = fileSize;
		m_fileCreated = fileCreated;
		m_fileModified = fileModified;
		m_parentNote = parentNote;
		m_itemBlockId = itemBlockId;
		m_rrv = rrv;
	}

	/**
	 * Returns the RRV ID that identifies the object in the database
	 * 
	 * @return RRV
	 */
	public int getRRV() {
		return m_rrv;
	}
	
	/**
	 * Returns the filename of the attachment
	 * 
	 * @return filename
	 */
	public String getFileName() {
		return m_fileName;
	}
	
	/**
	 * Returns the compression type
	 * 
	 * @return compression
	 */
	public Compression getCompression() {
		return m_compression;
	}
	
	/**
	 * Returns file flags, e.g. {@link NotesCAPI#FILEFLAG_SIGN}
	 * 
	 * @return flags
	 */
	public short getFileFlags() {
		return m_fileFlags;
	}
	
	/**
	 * Returns the file size
	 * 
	 * @return file size
	 */
	public int getFileSize() {
		return m_fileSize;
	}
	
	/**
	 * Returns the creation date
	 * 
	 * @return creation date
	 */
	public Calendar getFileCreated() {
		return m_fileCreated.toCalendar();
	}
	
	/**
	 * Returns the last modified date
	 * 
	 * @return date
	 */
	public Calendar getFileModified() {
		return m_fileModified.toCalendar();
	}

	/**
	 * Returns the parent note of the attachment
	 * 
	 * @return note
	 */
	public NotesNote getParentNote() {
		return m_parentNote;
	}

	/**
	 * Method to access the binary attachment data beginning at an offset in the file.
	 * The method is only supported when the attachment has no compression. Otherwise
	 * we will throw an {@link UnsupportedOperationException}.

	 * @param callback callback is called with streamed data
	 * @param offset offset to start reading
	 */
	public void readData(final IDataCallback callback, int offset) {
		readData(callback, offset, 65535);
	}
	
	/**
	 * Method to access the binary attachment data beginning at an offset in the file.
	 * The method is only supported when the attachment has no compression. Otherwise
	 * we will throw an {@link UnsupportedOperationException}.
	 * 
	 * @param callback callback is called with streamed data
	 * @param offset offset to start reading
	 * @param bufferSize max size of the buffer to be returned in the callback
	 */
	public void readData(final IDataCallback callback, int offset, int bufferSize) {
		m_parentNote.checkHandle();

		if (getCompression() != Compression.NONE) {
			throw new UnsupportedOperationException("This operation is only supported on attachments without compression.");
		}
		if (bufferSize<=0)
			throw new IllegalArgumentException("Buffer size must be a positive number");
		
		int currOffset = offset;
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			while (true) {
				int bytesToRead;
				if ((currOffset+bufferSize) < m_fileSize) {
					bytesToRead = bufferSize;
				}
				else {
					bytesToRead = m_fileSize - currOffset;
				}
				if (bytesToRead<=0) {
					//we're done
					break;
				}
				
				LongByReference rethBuffer = new LongByReference();
				
				short result = notesAPI.b64_NSFDbReadObject(m_parentNote.getParent().getHandle64(), m_rrv, currOffset, bytesToRead, rethBuffer);
				NotesErrorUtils.checkResult(result);
				
				Pointer ptr = notesAPI.b64_OSLockObject(rethBuffer.getValue());
				try {
					byte[] buffer = ptr.getByteArray(0, bytesToRead);
					Action action = callback.read(buffer);
					if (action==Action.Stop) {
						break;
					}
				}
				finally {
					notesAPI.b64_OSUnlockObject(rethBuffer.getValue());
					notesAPI.b64_OSMemFree(rethBuffer.getValue());
				}
				
				currOffset += bytesToRead;
			}
		}
		else {
			while (true) {
				int bytesToRead;
				if ((offset+bufferSize) < m_fileSize) {
					bytesToRead = bufferSize;
				}
				else {
					bytesToRead = m_fileSize - currOffset;
				}
				
				if (bytesToRead<=0) {
					//we're done
					break;
				}
				
				IntByReference rethBuffer = new IntByReference();
				
				short result = notesAPI.b32_NSFDbReadObject(m_parentNote.getParent().getHandle32(), m_rrv, currOffset, bytesToRead, rethBuffer);
				NotesErrorUtils.checkResult(result);
				
				Pointer ptr = notesAPI.b32_OSLockObject(rethBuffer.getValue());
				try {
					byte[] buffer = ptr.getByteArray(0, bytesToRead);
					Action action = callback.read(buffer);
					if (action==Action.Stop) {
						break;
					}
				}
				finally {
					notesAPI.b32_OSUnlockObject(rethBuffer.getValue());
					notesAPI.b32_OSMemFree(rethBuffer.getValue());
				}
				
				currOffset += bytesToRead;
			}
		}
	}
	
	/**
	 * Method to access the binary attachment data
	 * 
	 * @param callback callback is called with streamed data
	 */
	public void readData(final IDataCallback callback) {
		m_parentNote.checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		NotesBlockIdStruct.ByValue itemBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		itemBlockIdByVal.pool = m_itemBlockId.pool;
		itemBlockIdByVal.block = m_itemBlockId.block;
		
		int extractFlags = 0;
		int hDecryptionCipher = 0;
		
		NoteExtractCallback extractCallback;
		final Throwable[] extractError = new Throwable[1];
		
		if (notesAPI instanceof WinNotesCAPI) {
			extractCallback = new WinNotesCAPI.NoteExtractCallbackWin() {
				
				@Override
				public short invoke(Pointer data, int length, Pointer param) {
					if (length==0)
						return 0;
					
					try {
						byte[] dataArr = data.getByteArray(0, length);
						Action action = callback.read(dataArr);
						if (action==Action.Continue) {
							return 0;
						}
						else {
							throw new InterruptedException();
						}
					}
					catch (Throwable t) {
						extractError[0] = t;
						return INotesErrorConstants.ERR_NSF_INTERRUPT;
					}
				}
			};
		}
		else {
			extractCallback = new NoteExtractCallback() {

				@Override
				public short invoke(Pointer data, int length, Pointer param) {
					if (length==0)
						return 0;
					
					try {
						byte[] dataArr = data.getByteArray(0, length);
						Action action = callback.read(dataArr);
						if (action==Action.Continue) {
							return 0;
						}
						else {
							throw new InterruptedException();
						}
					}
					catch (Throwable t) {
						extractError[0] = t;
						return INotesErrorConstants.ERR_NSF_INTERRUPT;
					}
				}
			};
		}
		
		short result;
		if (NotesJNAContext.is64Bit()) {
			result = notesAPI.b64_NSFNoteCipherExtractWithCallback(m_parentNote.getHandle64(), 
					itemBlockIdByVal, extractFlags, hDecryptionCipher, 
					extractCallback, null, 0, null);
		}
		else {
			result = notesAPI.b32_NSFNoteCipherExtractWithCallback(m_parentNote.getHandle32(), 
					itemBlockIdByVal, extractFlags, hDecryptionCipher, 
					extractCallback, null, 0, null);
		}
		
		if (extractError[0] != null) {
			throw new NotesError(0, "Extraction interrupted", extractError[0]);
		}
		
		NotesErrorUtils.checkResult(result);
	}
	
	/**
	 * Deletes an attached file item from a note and also deallocates the disk space
	 * used to store the attached file in the database.
	 */
	public void deleteFromNote() {
		m_parentNote.checkHandle();
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		
		NotesBlockIdStruct.ByValue itemBlockIdByVal = NotesBlockIdStruct.ByValue.newInstance();
		itemBlockIdByVal.pool = m_itemBlockId.pool;
		itemBlockIdByVal.block = m_itemBlockId.block;

		if (NotesJNAContext.is64Bit()) {
			short result = notesAPI.b64_NSFNoteDetachFile(m_parentNote.getHandle64(), itemBlockIdByVal);
			NotesErrorUtils.checkResult(result);
		}
		else {
			short result = notesAPI.b32_NSFNoteDetachFile(m_parentNote.getHandle32(), itemBlockIdByVal);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * Callback class to read the streamed attachment data
	 * 
	 * @author Karsten Lehmann
	 */
	public static interface IDataCallback {
		public static enum Action {Continue, Stop};
		
		/**
		 * Implement this method to receive attachment data
		 * 
		 * @param data data
		 * @return action, either Continue or Stop
		 */
		public Action read(byte[] data);
	}
}
