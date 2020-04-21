package com.mindoo.domino.jna.mime;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.EnumSet;

import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.constants.MimeStreamItemizeOptions;
import com.mindoo.domino.jna.constants.MimeStreamOpenOptions;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * Utility class to read a {@link NotesNote} in MIME format or populate (itemize) a
 * document with data from a MIME document.<br>
 * 
 * @author Karsten Lehmann
 */
public class MIMEStream implements IRecyclableNotesObject {
	private NotesNote m_note;
	private String m_itemName;
	private Pointer m_hMIMEStream;
	private boolean m_recycled;

	/**
	 *  Creates a MIME stream, serializes the named items into it, and returns the MIME stream.<br>
	 *  Returns just the MIME content for the specified item, no named / RF822 items of
	 *  the note.
	 * 
	 * @param note note note
	 * @param itemName item name
	 * @return MIME stream
	 */
	public static MIMEStream openForRead(NotesNote note, String itemName) {
		return openForRead(note, itemName, EnumSet.noneOf(MimeStreamOpenOptions.class));
	}
	
	/**
	 *  Creates a MIME stream, serializes the named items into it, and returns the MIME stream.<br>
	 *  Specify the flag {@link MimeStreamOpenOptions#RFC2822_INCLUDE_HEADERS}
	 *  to flush all RFC2822 headers to the output MIME stream; i.e.,
	 *  the message's initial headers -- To, From, Subject, Date, etc.<br>
	 *  <br>
	 *  Also specify the flag {@link MimeStreamOpenOptions#MIME_INCLUDE_HEADERS}
	 *  to flush all MIME entity headers to the output MIME stream.
	 * 
	 * @param note note note
	 * @param itemName item name
	 * @param flags open flags
	 * @return MIME stream
	 */
	public static MIMEStream openForRead(NotesNote note, String itemName, EnumSet<MimeStreamOpenOptions> flags) {
		EnumSet<MimeStreamOpenOptions> flagsClone = flags.clone();
		
		int dwOpenFlags = MimeStreamOpenOptions.toBitMaskInt(flagsClone);
		dwOpenFlags = dwOpenFlags | NotesConstants.MIME_STREAM_OPEN_READ;
		return new MIMEStream(note, itemName, dwOpenFlags);
	}

	/**
	 * Creates a MIME stream. Use {@link #putLine(String)}
	 * or {@link #write(Reader)} to specify the MIME data to be written, then
	 * call {@link #itemize(EnumSet)} to write the data to the document.
	 * 
	 * @param note note
	 * @param itemName mime item to write content
	 * @return MIME stream
	 */
	public static MIMEStream openForWrite(NotesNote note, String itemName) {
		return openForWrite(note, itemName, EnumSet.noneOf(MimeStreamOpenOptions.class));
	}

	/**
	 * Creates a MIME stream. Use {@link #putLine(String)}
	 * or {@link #write(Reader)} to specify the MIME data to be written, then
	 * call {@link #itemize(EnumSet)} to write the data to the document.
	 * 
	 * @param note note
	 * @param itemName mime item to write content
	 * @param flags open flags
	 * @return MIME stream
	 */
	public static MIMEStream openForWrite(NotesNote note, String itemName, EnumSet<MimeStreamOpenOptions> flags) {
		EnumSet<MimeStreamOpenOptions> flagsClone = flags.clone();

		int dwOpenFlags = MimeStreamOpenOptions.toBitMaskInt(flagsClone);
		dwOpenFlags = dwOpenFlags | NotesConstants.MIME_STREAM_OPEN_WRITE;
		return new MIMEStream(note, itemName, dwOpenFlags);
	}

	private MIMEStream(NotesNote note, String itemName, int dwOpenFlags) {
		if (note.isRecycled()) {
			throw new NotesError(0, "Note is recycled");
		}
		m_note = note;
		
		Memory itemNameMem = NotesStringUtils.toLMBCS(itemName, false);

		PointerByReference rethMIMEStream = new PointerByReference();
		rethMIMEStream.setValue(null);

		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().MIMEStreamOpen(note.getHandle64(),
					itemNameMem, (short) (itemNameMem.size() & 0xffff), dwOpenFlags, rethMIMEStream);
			NotesErrorUtils.checkResult(result);
			m_hMIMEStream = rethMIMEStream.getValue();
		}
		else {
			result = NotesNativeAPI32.get().MIMEStreamOpen(note.getHandle32(),
					itemNameMem, (short) (itemNameMem.size() & 0xffff), dwOpenFlags, rethMIMEStream);
			NotesErrorUtils.checkResult(result);
			m_hMIMEStream = rethMIMEStream.getValue();
		}
		NotesGC.__objectCreated(MIMEStream.class, this);
	}

	private void checkRecycled() {
		if (isRecycled()) {
			throw new NotesError(0, "Stream already recycled");
		}
		else if (m_note.isRecycled()) {
			throw new NotesError(0, "Nnote is recycled");
		}
	}
	
	@Override
	public boolean isRecycled() {
		return m_recycled;
	}
	
	@Override
	public boolean isNoRecycle() {
		return false;
	}

	@Override
	public int getHandle32() {
		return 0;
	}

	@Override
	public long getHandle64() {
		return 0;
	}
	
	@Override
	public void recycle() {
		if (isRecycled()) {
			return;
		}
		
		if (PlatformUtils.is64Bit()) {
			NotesNativeAPI64.get().MIMEStreamClose(m_hMIMEStream);
			m_recycled = true;
		}
		else {
			NotesNativeAPI32.get().MIMEStreamClose(m_hMIMEStream);
			m_recycled = true;			
		}
	}
	
	public void close() {
		recycle();
	}
	
	public enum MimeStreamResult {
		/** successful MIME stream I/O. */
		SUCCESS,
		/** End Of Stream -- no more data in the MIME stream. */
		EOS
	}

	/**
	 * Writes a single line into the stream
	 * 
	 * @param line line
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public void putLine(String line) throws IOException {
		checkRecycled();
		
		Memory lineMem = NotesStringUtils.toLMBCS(line, true, false);

		if (PlatformUtils.is64Bit()) {
			toStreamResult(NotesNativeAPI64.get().MIMEStreamPutLine(lineMem, m_hMIMEStream));
		}
		else {
			toStreamResult(NotesNativeAPI32.get().MIMEStreamPutLine(lineMem, m_hMIMEStream));
		}
	}

	private MimeStreamResult toStreamResult(int resultAsInt) throws IOException {
		if (resultAsInt == NotesConstants.MIME_STREAM_IO) {
			throw new IOException("I/O error received during MIME stream operation");
		}
		else if (resultAsInt == NotesConstants.MIME_STREAM_EOS) {
			return MimeStreamResult.EOS;
		}
		else {
			return MimeStreamResult.SUCCESS;
		}
	}

	/**
	 * Reads a single line from the input MIME stream.
	 * 
	 * @param target {@link Appendable} to receive the line
	 * @return true if End Of Stream, false otherwise
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public MimeStreamResult readLine(Appendable target) throws IOException {
		checkRecycled();

		DisposableMemory retLine = new DisposableMemory(400);
		try {
			if (PlatformUtils.is64Bit()) {
				int resultAsInt = NotesNativeAPI64.get().MIMEStreamGetLine(retLine, (int) retLine.size(), m_hMIMEStream);
				MimeStreamResult result = toStreamResult(resultAsInt);
				
				String line = NotesStringUtils.fromLMBCS(retLine, -1);
				target.append(line);
				
				return result;
			}
			else {
				int resultAsInt = NotesNativeAPI32.get().MIMEStreamGetLine(retLine, (int) retLine.size(), m_hMIMEStream);
				MimeStreamResult result = toStreamResult(resultAsInt);
				
				String line = NotesStringUtils.fromLMBCS(retLine, -1);
				target.append(line);
				
				return result;
			}
		}
		finally {
			retLine.dispose();
		}
	}
	
	/**
	 * This function copies the MIME stream content into a {@link Writer}.
	 * 
	 * @param writer writer to receive the MIME stream data
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public void read(Writer writer) throws IOException {
		checkRecycled();
		
		int MAX_BUFFER_SIZE = 60000;
		DisposableMemory pchData = new DisposableMemory(MAX_BUFFER_SIZE);
		try {
			IntByReference puiDataLen = new IntByReference();

			MimeStreamResult result = null;
			while (result != MimeStreamResult.EOS) {
				if (PlatformUtils.is64Bit()) {
					result = toStreamResult(NotesNativeAPI64.get().MIMEStreamRead(pchData,
							puiDataLen, MAX_BUFFER_SIZE, m_hMIMEStream));
					int len = puiDataLen.getValue();
					if (len > 0) {
						String txt = NotesStringUtils.fromLMBCS(pchData, len);
						writer.write(txt);
					}
					else {
						return;
					}
				}
				else {
					result = toStreamResult(NotesNativeAPI32.get().MIMEStreamRead(pchData,
							puiDataLen, MAX_BUFFER_SIZE, m_hMIMEStream));
					int len = puiDataLen.getValue();
					if (len > 0) {
						String txt = NotesStringUtils.fromLMBCS(pchData, len);
						writer.write(txt);
					}
					else {
						return;
					}
				}
			}
		}
		finally {
			pchData.dispose();
		}
	}

	/**
	 * This function parses the MIME stream content to create a Notes/Domino MIME format document.<br>
	 * <br>
	 * With {@link MimeStreamItemizeOptions#ITEMIZE_HEADERS}, we only parse and itemize the
	 * MIME streams initial RFC822 headers into Notes/Domino items, e.g. the Internet message header
	 * 'To:' is itemized to the Notes item 'SendTo'.<br>
	 * 
	 * We itemize the headers and the body parts of the input MIME stream if both
	 * {@link MimeStreamItemizeOptions#ITEMIZE_HEADERS} and {@link MimeStreamItemizeOptions#ITEMIZE_BODY}
	 * are set.
	 * 
	 * @param flags flags to control the itemize operation
	 */
	public void itemize(EnumSet<MimeStreamItemizeOptions> flags) {
		checkRecycled();
		
		Memory targetItemNameMem = NotesStringUtils.toLMBCS(m_itemName, false);
		
		int dwFlags = MimeStreamItemizeOptions.toBitMaskInt(flags);
		
		short result;
		if (PlatformUtils.is64Bit()) {
			result = NotesNativeAPI64.get().MIMEStreamItemize(m_note.getHandle64(),
					targetItemNameMem, (short) (targetItemNameMem.size() & 0xffff), dwFlags, m_hMIMEStream);
			NotesErrorUtils.checkResult(result);
		}
		else {
			result = NotesNativeAPI64.get().MIMEStreamItemize(m_note.getHandle64(),
					targetItemNameMem, (short) (targetItemNameMem.size() & 0xffff), dwFlags, m_hMIMEStream);
			NotesErrorUtils.checkResult(result);
		}
	}
	
	/**
	 * This function rewinds a MIME stream to the beginning.
	 * 
	 * @throws IOException in case of MIME stream I/O errors
	 */
	public void rewind() throws IOException {
		checkRecycled();
		
		if (PlatformUtils.is64Bit()) {
			toStreamResult(NotesNativeAPI64.get().MIMEStreamRewind(m_hMIMEStream)); 
		}
		else {
			toStreamResult(NotesNativeAPI32.get().MIMEStreamRewind(m_hMIMEStream)); 
		}
	}

	/**
	 * This function writes a buffer to the input MIME stream.
	 * 
	 * @param reader reader to get the MIME stream data
	 * @throws IOException in case of 
	 */
	public void write(Reader reader) throws IOException {
		char[] buffer = new char[60000];
		int len;
		
		while ((len=reader.read(buffer))>0) {
			String txt = new String(buffer, 0, len);
			Memory txtMem = NotesStringUtils.toLMBCS(txt, false, false);
			
			if (PlatformUtils.is64Bit()) {
				toStreamResult(NotesNativeAPI64.get().MIMEStreamWrite(txtMem, (short) (txtMem.size() & 0xffff), m_hMIMEStream));
			}
			else {
				toStreamResult(NotesNativeAPI32.get().MIMEStreamWrite(txtMem, (short) (txtMem.size() & 0xffff), m_hMIMEStream));
			}
		}
	}
}
